/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.alerts;

import com.intel.rfid.api.DeviceAlert;
import com.intel.rfid.api.GatewayDeviceAlert;
import com.intel.rfid.api.GatewayStatus;
import com.intel.rfid.sensor.ConnectionState;
import com.intel.rfid.sensor.ConnectionStateEvent;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.upstream.UpstreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertManager
    implements SensorManager.SensorDeviceAlertListener,
               SensorManager.ConnectionStateListener {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected UpstreamManager upstreamMgr;

    public AlertManager(UpstreamManager _upstreamMgr) {
        upstreamMgr = _upstreamMgr;
    }

    public boolean start() {
        log.info(getClass().getSimpleName() + " started");
        return true;
    }

    public boolean stop() {
        log.info(getClass().getSimpleName() + " stopped");
        return true;
    }

    @Override
    public void onConnectionStateChange(ConnectionStateEvent _cse) {

        if (_cse.current == ConnectionState.CONNECTED &&
            _cse.previous != ConnectionState.CONNECTED) {

            upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                   GatewayStatus.RSP_CONNECTED,
                                                   DeviceAlert.Severity.info));

        } else if (_cse.current == ConnectionState.DISCONNECTED &&
                   _cse.previous != ConnectionState.DISCONNECTED) {


            if (_cse.cause == ConnectionStateEvent.Cause.LOST_HEARTBEAT) {

                upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                       GatewayStatus.RSP_LOST_HEARTBEAT,
                                                       DeviceAlert.Severity.warning));

            } else if (_cse.cause == ConnectionStateEvent.Cause.LOST_DOWNSTREAM_COMMS) {

                upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                       GatewayStatus.RSP_LAST_WILL_AND_TESTAMENT,
                                                       DeviceAlert.Severity.warning));

            } else if (_cse.cause == ConnectionStateEvent.Cause.SHUTTING_DOWN) {

                upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                       GatewayStatus.RSP_SHUTTING_DOWN,
                                                       DeviceAlert.Severity.warning));

            }
        }
    }

    @Override
    public void onSensorDeviceAlert(DeviceAlert _alert) {
        upstreamMgr.send(new GatewayDeviceAlert(_alert));
    }

}
