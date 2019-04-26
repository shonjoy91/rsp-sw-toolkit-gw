/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.downstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.ConnectRequest;
import com.intel.rfid.api.ConnectResponse;
import com.intel.rfid.api.GatewayStatusUpdate;
import com.intel.rfid.api.InventoryData;
import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.MQTTSummary;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.jmdns.JmDNSService;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class DownstreamManager implements MQTTDownstream.Dispatch {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected MQTTDownstream mqttDownstream;
    protected JmDNSService jmDNSService;
    protected SensorManager sensorMgr;

    protected DataMsgHandler dataMsgHandler;
    protected Map<String, RSPMsgHandler> rspMsgHandlers;

    protected ObjectMapper mapper = Jackson.getMapper();

    public DownstreamManager(SensorManager _sensorMgr) {
        sensorMgr = _sensorMgr;
        sensorMgr.setDownstreamMgr(this);
        mqttDownstream = new MQTTDownstream(this);
        jmDNSService = new JmDNSService();
        dataMsgHandler = new DataMsgHandler();
        rspMsgHandlers = new HashMap<>();
    }

    public boolean start() {
        dataMsgHandler.start();
        mqttDownstream.start();
        jmDNSService.start();
        log.info(getClass().getSimpleName() + " started");
        return true;
    }

    public boolean stop() {
        jmDNSService.stop();
        mqttDownstream.stop();
        for (RSPMsgHandler rmh : rspMsgHandlers.values()) {
            rmh.shutdown();
            log.info("Stopped message handler {}", rmh.getDeviceId());
        }
        rspMsgHandlers.clear();
        dataMsgHandler.shutdown();
        log.info(getClass().getSimpleName() + " stopped");
        return true;
    }

    public void startJmDNSService() {
        jmDNSService.start();
    }

    public void stopJmDNSService() {
        jmDNSService.stop();
    }

    public void sendCommand(String _deviceId, JsonRequest _req)
        throws JsonProcessingException, GatewayException {

        byte[] bytes = mapper.writeValueAsBytes(_req);
        mqttDownstream.publishCommand(_deviceId, bytes);
        log.info("{} msgid[{}] {} {}",
                 _deviceId, _req.getId(), _req.getMethod(),
                 mapper.writeValueAsString(_req));
    }

    public void sendConnectRsp(String _deviceId, ConnectResponse _rsp)
        throws JsonProcessingException, GatewayException {

        byte[] bytes = mapper.writeValueAsBytes(_rsp);
        mqttDownstream.publishConnectResponse(_deviceId, bytes);
        log.info("{} msgid[{}] ConnectResponse {}",
                 _deviceId, _rsp.id,
                 mapper.writeValueAsString(_rsp.result));
    }

    public void sendGWStatus(String _status) {
        GatewayStatusUpdate gsu = new GatewayStatusUpdate(ConfigManager.instance.getGatewayDeviceId(), _status);
        try {
            mqttDownstream.publishGWStatus(mapper.writeValueAsBytes(gsu));
            log.info("Published GatewayStatusUpdate {}", _status);
        } catch (GatewayException | JsonProcessingException _e) {
            log.error("failed to send gateway status update {} {}",
                      _status, _e.getMessage());
        }
    }

    public MQTTSummary getSummary() {
        return mqttDownstream.getSummary();
    }
    
    @Override
    public void onMessage(String _topic, MqttMessage _msg) {
        // Don't blow up!!
        try {
            // Execute only if the schema validation of the mqtt message passes
            String deviceId;

            if (_topic.equals(MQTTDownstream.CONNECT_TOPIC)) {
                // connects need special handling to find the device id,
                // and the device id is needed to get the message handler
                // they don't occur very often so a bit of double processing
                // shouldn't be such a big deal
                ConnectRequest cr = mapper.readValue(_msg.getPayload(), ConnectRequest.class);
                deviceId = cr.params.hostname;
            } else {
                deviceId = _topic.substring(_topic.lastIndexOf('/') + 1);
            }

            MQTTMsgHandler.Inbound inbound = new MQTTMsgHandler.Inbound(deviceId, _msg);

            if (_topic.startsWith(MQTTDownstream.DATA_TOPIC)) {
                dataMsgHandler.queue(inbound);
            } else {
                RSPMsgHandler rspMsgHandler = rspMsgHandlers.get(deviceId);
                if (rspMsgHandler == null) {
                    SensorPlatform rsp = sensorMgr.establishRSP(deviceId);
                    rspMsgHandler = new RSPMsgHandler(rsp);
                    rspMsgHandlers.put(deviceId, rspMsgHandler);
                    rspMsgHandler.start();
                }

                rspMsgHandler.queue(inbound);
            }
        } catch (Exception e) {
            log.error("Exception for topic {} msg {}",
                      _topic, new String(_msg.getPayload()), e);
        }
    }

    public interface InventoryDataListener {
        void onInventoryData(InventoryData _data, SensorPlatform _rsp);
    }

    protected final HashSet<InventoryDataListener> inventoryDataListeners = new HashSet<>();

    public void addInventoryDataListener(InventoryDataListener _l) {
        synchronized (inventoryDataListeners) {
            inventoryDataListeners.add(_l);
        }
    }

    public void removeInventoryDataListener(InventoryDataListener _l) {
        synchronized (inventoryDataListeners) {
            inventoryDataListeners.remove(_l);
        }
    }

    public class DataMsgHandler extends MQTTMsgHandler {

        protected Logger readlog = LoggerFactory.getLogger("tag.read");

        protected void handleMessage(Inbound _msg) {

            SensorPlatform rsp = sensorMgr.getRSP(_msg.deviceId);
            // don't mess with data coming from unknown sensors
            if (rsp == null) {
                return;
            }

            rsp.updateLastComms();

            try {
                InventoryData data = mapper.readValue(_msg.mqttMessage.getPayload(), InventoryData.class);

                rsp.onInventoryData(data);
                synchronized (inventoryDataListeners) {
                    for (InventoryDataListener l : inventoryDataListeners) {
                        try {
                            l.onInventoryData(data, rsp);
                        } catch (Throwable t) {
                            log.error("error:", t);
                        }
                    }
                }

                if (readlog.isInfoEnabled()) {
                    // MUST call new String() or data will be garbled, toString() does not function properly
                    readlog.info(new String(_msg.mqttMessage.getPayload()));
                }

            } catch (Exception e) {
                log.error("error: ", e);
            }
        }
    }

    public static class RSPMsgHandler extends MQTTMsgHandler {

        protected SensorPlatform rsp;

        public RSPMsgHandler(SensorPlatform _rsp) {
            rsp = _rsp;
            setName("msg-handler " + rsp.getDeviceId());
        }

        public String getDeviceId() {
            return rsp.getDeviceId();
        }

        protected void handleMessage(Inbound _msg) {
            if (_msg.mqttMessage != null) {
                rsp.handleMessage(_msg.mqttMessage.getPayload());
            }
        }

    }

    public void show(PrettyPrinter _out) {

        _out.line("JmDNS Service started: " + jmDNSService.isStarted());
        _out.blank();

        _out.chunk("MQTT Downstream: ");
        mqttDownstream.status(_out);
        _out.blank();

    }

}
