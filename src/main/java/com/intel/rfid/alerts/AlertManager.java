/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.alerts;

import com.intel.rfid.api.data.Connection;
import com.intel.rfid.api.sensor.AlertSeverity;
import com.intel.rfid.api.sensor.DeviceAlertNotification;
import com.intel.rfid.api.upstream.GatewayDeviceAlertNotification;
import com.intel.rfid.gateway.GatewayStatus;
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

        if (_cse.current == Connection.State.CONNECTED &&
            _cse.previous != Connection.State.CONNECTED) {

            upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                   GatewayStatus.RSP_CONNECTED,
                                                   AlertSeverity.info));

        } else if (_cse.current == Connection.State.DISCONNECTED &&
                   _cse.previous != Connection.State.DISCONNECTED) {


            if (_cse.cause == Connection.Cause.LOST_HEARTBEAT) {

                upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                       GatewayStatus.RSP_LOST_HEARTBEAT,
                                                       AlertSeverity.warning));

            } else if (_cse.cause == Connection.Cause.LOST_DOWNSTREAM_COMMS) {

                upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                       GatewayStatus.RSP_LAST_WILL_AND_TESTAMENT,
                                                       AlertSeverity.warning));

            } else if (_cse.cause == Connection.Cause.SHUTTING_DOWN) {

                upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                       GatewayStatus.RSP_SHUTTING_DOWN,
                                                       AlertSeverity.warning));

            } else if (_cse.cause == Connection.Cause.FORCED_DISCONNECT) {

                upstreamMgr.send(new SensorStatusAlert(_cse.rsp,
                                                       GatewayStatus.GATEWAY_TRIGGERED_RSP_DISCONNECT,
                                                       AlertSeverity.warning));

            }
        }
    }

    @Override
    public void onSensorDeviceAlert(DeviceAlertNotification _alert) {
        upstreamMgr.send(new GatewayDeviceAlertNotification(_alert));
    }

}
