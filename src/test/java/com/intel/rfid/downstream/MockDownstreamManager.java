/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.downstream;

import com.intel.rfid.alerts.ConnectionStateEvent;
import com.intel.rfid.api.sensor.ConnectRequest;
import com.intel.rfid.api.sensor.StatusUpdateNotification;
import com.intel.rfid.gpio.GPIOManager;
import com.intel.rfid.jmdns.MockJmDNSService;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

public class MockDownstreamManager extends DownstreamManager implements SensorManager.ConnectionStateListener {

    public MockDownstreamManager(SensorManager _sensorManager, GPIOManager _gpioMgr) {

        super(_sensorManager, _gpioMgr);
        // swap out
        jmDNSService = new MockJmDNSService();
        mqttDownstream = new MockMqttDownstream(this);
        mqttDownstream.start();

    }

    @Override
    public void startJmDNSService() {
        log.info("mock not actually starting JmDNSService");
    }

    @Override
    public void stopJmDNSService() {
        log.info("mock not actually stopping JmDNSService");
    }

    private final Object conectLock = new Object();

    public void connectSequence(SensorPlatform _sensor) throws IOException, InterruptedException {

        synchronized (conectLock) {
            ConnectRequest req = new ConnectRequest();
            req.params.hostname = _sensor.getDeviceId();
            onMessage(MqttDownstream.CONNECT_TOPIC, new MqttMessage(mapper.writeValueAsBytes(req)));
            StatusUpdateNotification readyMsg = new StatusUpdateNotification();
            readyMsg.params.device_id = _sensor.getDeviceId();
            readyMsg.params.status = StatusUpdateNotification.Status.ready;
            onMessage(MqttDownstream.RSP_STATUS_TOPIC + "/" + _sensor.getDeviceId(),
                      new MqttMessage(mapper.writeValueAsBytes(readyMsg)));
            conectLock.wait(1000);
        }
    }

    @Override
    public void onConnectionStateChange(ConnectionStateEvent _cse) {
        conectLock.notify();
    }

    public boolean handlerExistsFor(SensorPlatform _sensor) {
        synchronized (rspMsgHandlers) {
            return rspMsgHandlers.containsKey(_sensor.getDeviceId());
        }
    }
    //@Override
    //public void sendConnectRsp(String _deviceId, ConnectResponse _rsp)
    //        throws JsonProcessingException, GatewayException {
    //
    //    // do nothing for now, eventually hook into sensor simulation
    //}
    //
    //public void sendCommand(String _deviceId, JsonRequest _req)
    //        throws JsonProcessingException, GatewayException {
    //
    //}

    //public void sendConnectRsp(String _deviceId, ConnectResponse _rsp)
    //        throws JsonProcessingException, GatewayException {
    //
    //    byte[] bytes = mapper.writeValueAsBytes(_rsp);
    //    mqttGateway.publishConnectResponse(_deviceId, bytes);
    //    log.info("{} msgid[{}] ConnectResponse {}",
    //             _deviceId, _rsp.id,
    //             mapper.writeValueAsString(_rsp.result));
    //}

    //public void sendGWStatus(String _status) {
    //    GatewayStatusUpdate gsu = new GatewayStatusUpdate(ConfigManager.instance.getGatewayDeviceId(), _status);
    //    try {
    //        mqttGateway.publishGWStatus(mapper.writeValueAsBytes(gsu));
    //        log.info("Published GatewayStatusUpdate {}", _status);
    //    } catch (GatewayException | JsonProcessingException _e) {
    //        log.error("failed to send gateway status update {} {}",
    //                  _status, _e.getMessage());
    //    }
    //}

    //public void onData(InventoryDataNotification _data, SensorPlatform _rsp) {
    //  synchronized (inventoryDataListeners) {
    //    for (InventoryDataListener l : inventoryDataListeners) {
    //      try {
    //        l.onInventoryData(_data, _rsp);
    //      } catch (Throwable t) {
    //        log.error("error:", t);
    //      }
    //    }
    //  }
    //}

}
