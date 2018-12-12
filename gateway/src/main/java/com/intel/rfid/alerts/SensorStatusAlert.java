/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.alerts;

import com.intel.rfid.api.DeviceAlert;
import com.intel.rfid.api.GatewayDeviceAlert;
import com.intel.rfid.api.GatewayStatus;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.sensor.SensorPlatform;

public class SensorStatusAlert extends GatewayDeviceAlert {

    public SensorStatusAlert(SensorPlatform _rsp, GatewayStatus _status, DeviceAlert.Severity _severity) {
        params.sent_on = System.currentTimeMillis();
        params.device_id = _rsp.getDeviceId();
        params.gateway_id = ConfigManager.instance.getGatewayDeviceId();
        params.alert_number = _status.id;
        params.alert_description = _status.toString();
        params.severity = _severity.toString();
        params.facilities.add(_rsp.getFacilityId());
    }
}
