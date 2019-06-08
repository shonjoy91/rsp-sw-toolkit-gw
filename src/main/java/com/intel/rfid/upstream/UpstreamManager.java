/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.JsonResponse;
import com.intel.rfid.api.data.MqttStatus;
import com.intel.rfid.api.upstream.GatewayDeviceAlertNotification;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.gateway.JsonRpcController;
import com.intel.rfid.gpio.GPIOManager;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.SensorManager;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class UpstreamManager
    implements InventoryManager.UpstreamEventListener, 
               MqttUpstream.Dispatch,
               JsonRpcController.Callback {

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
        deviceId = cm.getGatewayDeviceId();
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
    public void onUpstreamEvent(UpstreamInventoryEventInfo _uie) {
        send(_uie);
    }

    @Override
    public void onMessage(String _topic, MqttMessage _msg) {
    
        rpcController.inbound(_msg.getPayload());
    }

    public void send(GatewayDeviceAlertNotification _alert) {
        try {
            _alert.params.gateway_id = deviceId;
            mqttUpstream.publish(_alert);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public void send(UpstreamInventoryEventInfo _uie) {
        try {
            _uie.gateway_id = deviceId;
            mqttUpstream.publish(_uie);
        } catch (Exception e) {
            log.error("error: ", e);
        }
    }
    
    public MqttStatus getMqttStatus() {
        return mqttUpstream.getSummary();
    }

    public void show(PrettyPrinter _out) {
        _out.chunk("MQTT Upstream: ");
        mqttUpstream.status(_out);
    }

    @Override
    public void sendJsonResponse(JsonResponse _rsp) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(_rsp);
            mqttUpstream.publishResponse(bytes);
            log.info("sent msgid[{}] {}",
                     _rsp.id, mapper.writeValueAsString(_rsp));
        } catch(IOException | GatewayException _e) {
            log.error("error {}", _e.getMessage());
        }
    }

    @Override
    public void sendJsonNotification(JsonNotification _notification) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(_notification);
            mqttUpstream.publishResponse(bytes);
            log.info("sent msgid[{}] {}",
                     _notification.getMethod(), mapper.writeValueAsString(_notification));
        } catch(IOException | GatewayException _e) {
            log.error("error {}", _e.getMessage());
        }
    }
}
