/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.alerts;

import com.intel.rfid.api.sensor.AlertSeverity;
import com.intel.rfid.api.upstream.RspControllerDeviceAlertNotification;
import com.intel.rfid.controller.ConfigManager;
import com.intel.rfid.controller.RspControllerStatus;
import com.intel.rfid.sensor.SensorPlatform;

public class SensorStatusAlert extends RspControllerDeviceAlertNotification {

    public SensorStatusAlert(SensorPlatform _rsp, RspControllerStatus _status, AlertSeverity _severity) {
        params.sent_on = System.currentTimeMillis();
        params.device_id = _rsp.getDeviceId();
        params.controller_id = ConfigManager.instance.getRspControllerDeviceId();
        params.alert_number = _status.id;
        params.alert_description = _status.toString();
        params.severity = _severity.toString();
        params.facilities.add(_rsp.getFacilityId());
    }
}
