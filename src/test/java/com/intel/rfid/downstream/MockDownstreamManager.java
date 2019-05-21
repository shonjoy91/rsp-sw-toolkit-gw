/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.downstream;


import com.intel.rfid.api.downstream.SensorInventoryDataNotification;
import com.intel.rfid.jmdns.MockJmDNSService;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import com.intel.rfid.gpio.GPIOManager;

public class MockDownstreamManager extends DownstreamManager {

  public MockDownstreamManager(SensorManager _sensorManager, GPIOManager _gpioMgr) {

    super(_sensorManager, _gpioMgr);
    // swap out
    jmDNSService = new MockJmDNSService();
    mqttDownstream = new MQTTDownstream(this);

  }

  @Override
  public void startJmDNSService() {
    log.info("mock not actually starting JmDNSService");
  }

  @Override
  public void stopJmDNSService() {
    log.info("mock not actually stopping JmDNSService");
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


  public void onData(SensorInventoryDataNotification _data, SensorPlatform _rsp) {
    synchronized (inventoryDataListeners) {
      for (InventoryDataListener l : inventoryDataListeners) {
        try {
          l.onInventoryData(_data, _rsp);
        } catch (Throwable t) {
          log.error("error:", t);
        }
      }
    }

  }
}
