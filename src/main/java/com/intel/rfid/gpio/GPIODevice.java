/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gpio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.JsonRpcError;
import com.intel.rfid.api.data.Connection;
import com.intel.rfid.api.gpio.GPIO;
import com.intel.rfid.api.gpio.GPIOConnectRequest;
import com.intel.rfid.api.gpio.GPIODeviceInfo;
import com.intel.rfid.api.gpio.GPIOInfo;
import com.intel.rfid.api.gpio.GPIOInputNotification;
import com.intel.rfid.api.gpio.GPIOSetStateRequest;
import com.intel.rfid.api.sensor.SensorHeartbeatNotification;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.schedule.AtomicTimeMillis;
import com.intel.rfid.sensor.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GPIODevice {

    protected GPIODeviceInfo deviceInfo;
    protected GPIOManager manager;
    protected int inputs = 0;
    protected int outputs = 0;

    protected final Logger logGPIO; // created on construction using the deviceId
    protected final Logger logAlert = LoggerFactory.getLogger("gpio.alert");
    protected final Logger logHeartbeat = LoggerFactory.getLogger("gpio.heartbeat");
    protected final Logger logInputEvent = LoggerFactory.getLogger("gpio.input");
    protected final Logger logConnect = LoggerFactory.getLogger("rsp.connect");

    protected Connection.State connectionState = Connection.State.DISCONNECTED;
    protected final AtomicTimeMillis lastCommsMillis = new AtomicTimeMillis();

    public static final long LOST_COMMS_THRESHOLD = 90000;
    protected static final ObjectMapper MAPPER = Jackson.getMapper();
    protected final Object msgHandleLock = new Object();
    protected final Map<String, ResponseHandler> responseHandlers = new HashMap<>();

    public GPIODevice(String _deviceId, GPIOManager _manager) {
        deviceInfo = new GPIODeviceInfo();
        deviceInfo.device_id = _deviceId;
        manager = _manager;
        logGPIO = LoggerFactory.getLogger(String.format("%s.%s", getClass().getSimpleName(), deviceInfo.device_id));
    }

    public void setGPIODeviceInfo(GPIODeviceInfo _info) {
        if (_info.device_id.equals(deviceInfo.device_id)) {
            deviceInfo = _info;
            inputs = 0;
            outputs = 0;
            for (GPIOInfo info : deviceInfo.gpio_info) {
                if (info.direction == GPIO.Direction.INPUT) {
                    inputs++;
                } else {
                    outputs++;
                }
            }
        } else {
            logGPIO.warn("Cannot set GPIODeviceInfo, {} does not match {}", deviceInfo.device_id, _info.device_id);
        }
    }

    public String getDeviceId() {
        return deviceInfo.device_id;
    }

    public ResponseHandler setGPIOState(int _index, GPIO.State _state) {
        // if not connected, this is all that needs to be done
        if (!isConnected()) {
            return new ResponseHandler(getDeviceId(),
                                       JsonRpcError.Type.NO_ERROR,
                                       "GPIODevice is not connected");
        }
        if (!(_index < deviceInfo.gpio_info.size())) {
            return new ResponseHandler(getDeviceId(),
                                       JsonRpcError.Type.INVALID_PARAMETER,
                                       "Index is out of range");
        }
        GPIOInfo info = deviceInfo.gpio_info.get(_index);
        info.state = _state;
        logGPIO.info("{} {} {}", getDeviceId(), _state, info.name);
        deviceInfo.gpio_info.set(_index, info);
        return execute(new GPIOSetStateRequest(info));
    }

    private ResponseHandler execute(JsonRequest _req) {
        if (connectionState != Connection.State.CONNECTED) {
            String errMsg = "no connection to device";
            logGPIO.info("Cannot execute {}: {} - {}", getDeviceId(), _req.getMethod(), errMsg);
            return new ResponseHandler(getDeviceId(), JsonRpcError.Type.WRONG_STATE, errMsg);
        }

        ResponseHandler rh;

        rh = new ResponseHandler(getDeviceId(), _req.getId());

        // be sure to put the handler in before sending the message
        // risk of the message and response coming back before the handler
        // can get to it.
        synchronized (msgHandleLock) {
            responseHandlers.put(_req.getId(), rh);
        }

        try {
            rh.setRequest(_req);
            manager.sendGPIOCommand(getDeviceId(), _req);
        } catch (Exception e) {
            synchronized (msgHandleLock) {
                responseHandlers.remove(_req.getId());
            }
            rh = new ResponseHandler(getDeviceId(), _req.getId(),
                                     JsonRpcError.Type.INTERNAL_ERROR, e.getMessage());
            rh.setRequest(_req);
            logGPIO.error("{} error sending command:", getDeviceId(), e);
        }

        return rh;
    }

    public void handleMessage(byte[] _msg) {
        synchronized (msgHandleLock) {
            updateLastComms();
            try {

                JsonNode rootNode = MAPPER.readTree(_msg);

                // check for method object and get out early if missing
                JsonNode idNode = rootNode.get("id");
                JsonNode methodNode = rootNode.get("method");

                // both requests and notifications can follow a similar
                // processing path. The onXXX methods are inherently mapped
                // to whether this is a request or notification
                if (methodNode != null) {
                    handleMethod(methodNode.asText(), rootNode);
                } else if (idNode != null) {
                    String id = idNode.asText();
                    ResponseHandler rh = responseHandlers.get(id);
                    if (rh != null) {
                        rh.handleResponse(rootNode);
                    }
                    responseHandlers.remove(id);
                } else {
                    logGPIO.warn("{} unhandled json msg: {}",
                                 deviceInfo.device_id, MAPPER.writeValueAsString(rootNode));
                }

            } catch (Exception e) {
                logGPIO.error("{} Error handling message:", deviceInfo.device_id, e);
            }
        }
    }

    private void handleMethod(String _method, JsonNode _rootNode) {

        try {

            switch (_method) {

                case GPIOConnectRequest.METHOD_NAME:
                    GPIOConnectRequest conReq = MAPPER.treeToValue(_rootNode, GPIOConnectRequest.class);
                    onConnect(conReq);
                    break;

                case GPIOInputNotification.METHOD_NAME:
                    GPIOInputNotification inputNotification = MAPPER.treeToValue(_rootNode,
                                                                                 GPIOInputNotification.class);
                    onGPIOInputNotification(inputNotification);
                    break;

                case SensorHeartbeatNotification.METHOD_NAME:
                    SensorHeartbeatNotification hb = MAPPER.treeToValue(_rootNode, SensorHeartbeatNotification.class);
                    onHeartbeat(hb);
                    break;

                default:
                    logGPIO.warn("{} unhandled method: {}", deviceInfo.device_id, _method);

            }

        } catch (JsonProcessingException e) {
            logGPIO.error("{} Error inbound JsonRPC message:", deviceInfo.device_id, e);
        } catch (Exception e) {
            logGPIO.error("{} Error handling message:", deviceInfo.device_id, e);
        }

    }

    private void onConnect(GPIOConnectRequest _msg) {
        logInboundJson(logConnect, _msg.getMethod(), _msg.params);
        if (connectionState != Connection.State.CONNECTED) {
            changeConnectionState(Connection.State.CONNECTED, Connection.Cause.READY);
        }
        setGPIODeviceInfo(_msg.params);
        try {
            manager.sendGPIOConnectResponse(_msg.getId(), deviceInfo.device_id);
        } catch (IOException | GatewayException _e) {
            logGPIO.error("error sending connect response", _e);
        }
    }

    public boolean isConnected() {
        return connectionState == Connection.State.CONNECTED;
    }

    public Connection.State getConnectionState() {
        return connectionState;
    }

    private void onGPIOInputNotification(GPIOInputNotification _notification) {
        logInboundJson(logConnect, _notification.getMethod(), _notification.params);
        try {
            manager.handleGPIOInput(deviceInfo.device_id, _notification);
            GPIOInfo info = _notification.params.gpio_info;
            deviceInfo.gpio_info.set(info.index, info);
        } catch (IOException | GatewayException _e) {
            logGPIO.error("error handling GPIO Input", _e);
        }
    }

    private void onHeartbeat(SensorHeartbeatNotification _msg) {
        logInboundJson(logHeartbeat, _msg.getMethod(), _msg.params);
        if (connectionState != Connection.State.CONNECTED) {
            changeConnectionState(Connection.State.CONNECTED, Connection.Cause.RESYNC);
        }
    }

    protected synchronized void changeConnectionState(Connection.State _next, Connection.Cause _cause) {

        connectionState = _next;
    }

    public long getLastCommsMillis() {
        return lastCommsMillis.get();
    }

    public void updateLastComms() {
        lastCommsMillis.mark();
    }

    private boolean hasLostComms() {
        return !lastCommsMillis.isWithin(LOST_COMMS_THRESHOLD);
    }

    void checkLostHeartbeatAndReset() {
        if (hasLostComms()) {
            if (connectionState != Connection.State.DISCONNECTED) {
                changeConnectionState(Connection.State.DISCONNECTED, Connection.Cause.LOST_HEARTBEAT);
            }
            lastCommsMillis.set(0);
        }
    }

    private static final String FMT = "%-12s %-12s %-12s %-12s";
    public static final String HDR = String
            .format(FMT, "device", "state", "inputs", "outputs");

    @Override
    public String toString() {
        return String.format(FMT,
                             deviceInfo.device_id,
                             connectionState,
                             inputs,
                             outputs);
    }

    private void logInboundJson(Logger _log, String _prefix, Object _msg) {
        try {
            _log.info("{} RECEIVED {} {}", deviceInfo.device_id, _prefix, MAPPER.writeValueAsString(_msg));
        } catch (JsonProcessingException e) {
            _log.error("{} ERROR: {}", deviceInfo.device_id, e);
        }
    }

}
