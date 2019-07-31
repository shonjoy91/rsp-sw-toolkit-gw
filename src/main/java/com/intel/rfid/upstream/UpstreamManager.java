/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.alerts.ConnectionStateEvent;
import com.intel.rfid.alerts.SensorStatusAlert;
import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.JsonResponse;
import com.intel.rfid.api.data.Connection;
import com.intel.rfid.api.data.MqttStatus;
import com.intel.rfid.api.sensor.AlertSeverity;
import com.intel.rfid.api.sensor.DeviceAlertNotification;
import com.intel.rfid.api.upstream.InventoryEventNotification;
import com.intel.rfid.api.upstream.RspControllerDeviceAlertNotification;
import com.intel.rfid.api.upstream.RspControllerHeartbeatNotification;
import com.intel.rfid.api.upstream.RspControllerStatusUpdateNotification;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.controller.ConfigManager;
import com.intel.rfid.controller.JsonRpcController;
import com.intel.rfid.controller.RspControllerStatus;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.gpio.GPIOManager;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.SensorManager;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.intel.rfid.api.sensor.AlertSeverity.info;
import static com.intel.rfid.api.sensor.AlertSeverity.warning;
import static com.intel.rfid.controller.RspControllerStatus.RSP_CONNECTED;
import static com.intel.rfid.controller.RspControllerStatus.RSP_CONTROLLER_TRIGGERED_RSP_DISCONNECT;
import static com.intel.rfid.controller.RspControllerStatus.RSP_LAST_WILL_AND_TESTAMENT;
import static com.intel.rfid.controller.RspControllerStatus.RSP_LOST_HEARTBEAT;
import static com.intel.rfid.controller.RspControllerStatus.RSP_SHUTTING_DOWN;

public class UpstreamManager
        implements InventoryManager.UpstreamEventListener,
                   MqttUpstream.Dispatch,
                   JsonRpcController.Callback,
                   SensorManager.SensorDeviceAlertListener,
                   SensorManager.ConnectionStateListener {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected String deviceId;
    protected MqttUpstream mqttUpstream;

    protected ClusterManager clusterMgr;
    protected SensorManager sensorMgr;
    protected GPIOManager gpioMgr;
    protected ScheduleManager scheduleMgr;
    protected InventoryManager inventoryMgr;
    protected DownstreamManager downstreamMgr;

    protected JsonRpcController rpcController;
    protected ObjectMapper mapper = Jackson.getMapper();

    public UpstreamManager(ClusterManager _clusterMgr,
                           SensorManager _sensorMgr,
                           GPIOManager _gpioMgr,
                           ScheduleManager _scheduleMgr,
                           InventoryManager _inventoryMgr,
                           DownstreamManager _downstreamMgr) {

        clusterMgr = _clusterMgr;
        sensorMgr = _sensorMgr;
        gpioMgr = _gpioMgr;
        scheduleMgr = _scheduleMgr;
        inventoryMgr = _inventoryMgr;
        downstreamMgr = _downstreamMgr;

        ConfigManager cm = ConfigManager.instance;
        deviceId = cm.getRspControllerDeviceId();
        mqttUpstream = new MqttUpstream(this);

        rpcController = new JsonRpcController(this,
                                              clusterMgr,
                                              sensorMgr,
                                              gpioMgr,
                                              inventoryMgr,
                                              this,
                                              downstreamMgr,
                                              scheduleMgr);
    }

    public boolean start() {
        rpcController.start();
        mqttUpstream.start();

        log.info(getClass().getSimpleName() + " started");
        return true;
    }

    public boolean stop() {
        mqttUpstream.stop();
        rpcController.stop();
        log.info(getClass().getSimpleName() + " stopped");
        return true;
    }

    @Override
    public void onMessage(String _topic, MqttMessage _msg) {
        rpcController.inbound(_msg.getPayload());
    }

    @Override
    public void onUpstreamEvent(UpstreamInventoryEventInfo _uie) {
        _uie.device_id = deviceId;
        mqttUpstream.publishEvent(new InventoryEventNotification(_uie));
    }

    public void send(RspControllerDeviceAlertNotification _alert) {
        mqttUpstream.publishAlert(_alert);
    }

    public void send(RspControllerHeartbeatNotification _hb) {
        mqttUpstream.publishEvent(_hb);
    }

    public void send(RspControllerStatusUpdateNotification _not) {
        mqttUpstream.publishAlert(_not);
    }

    @Override
    public void sendJsonResponse(JsonResponse _rsp) {
        mqttUpstream.publishResponse(_rsp);
    }

    @Override
    public void sendJsonNotification(JsonNotification _not) {
        mqttUpstream.publishNotification(_not);
    }

    @Override
    public void onConnectionStateChange(ConnectionStateEvent _cse) {

        // map to the correct status and severity
        RspControllerStatus status = null;
        AlertSeverity severity = null;

        if (_cse.current == Connection.State.CONNECTED &&
                _cse.previous != Connection.State.CONNECTED) {

            severity = info;
            status = RSP_CONNECTED;

        } else if (_cse.current == Connection.State.DISCONNECTED &&
                _cse.previous != Connection.State.DISCONNECTED) {

            severity = warning;
            switch (_cse.cause) {
                case LOST_HEARTBEAT:
                    status = RSP_LOST_HEARTBEAT;
                    break;
                case LOST_DOWNSTREAM_COMMS:
                    status = RSP_LAST_WILL_AND_TESTAMENT;
                    break;
                case SHUTTING_DOWN:
                    status = RSP_SHUTTING_DOWN;
                    break;
                case FORCED_DISCONNECT:
                    status = RSP_CONTROLLER_TRIGGERED_RSP_DISCONNECT;
                    break;
            }
        }

        if (status != null) {
            send(new SensorStatusAlert(_cse.rsp, status, severity));
        }

    }

    @Override
    public void onSensorDeviceAlert(DeviceAlertNotification _alert) {
        send(new RspControllerDeviceAlertNotification(_alert));
    }

    public MqttStatus getMqttStatus() {
        return mqttUpstream.getSummary();
    }

    @Deprecated
    public void show(PrettyPrinter _out) {
        _out.chunk("MQTT Upstream: ");
        mqttUpstream.status(_out);
    }

}
