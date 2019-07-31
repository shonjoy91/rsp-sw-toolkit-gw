/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.sensor.DeviceAlertNotification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSPControllerDeviceAlertNotification extends JsonNotification {

    public static final String METHOD_NAME = "device_alert";

    public Params params = new Params();

    public RSPControllerDeviceAlertNotification() {
        method = METHOD_NAME;
    }

    public RSPControllerDeviceAlertNotification(DeviceAlertNotification _deviceAlert) {
        this();
        params.sent_on = _deviceAlert.params.sent_on;
        params.device_id = _deviceAlert.params.device_id;
        if (_deviceAlert.params.facility_id != null) {
            params.facilities.add(_deviceAlert.params.facility_id);
        }
        params.alert_number = _deviceAlert.params.alert_number;
        params.alert_description = _deviceAlert.params.alert_description;
        params.severity = _deviceAlert.params.severity;
        if (_deviceAlert.params.optional != null) {
            params.optional.putAll(_deviceAlert.params.optional);
        }
    }

    public static class Params {
        public long sent_on;
        public String device_id;
        public String controller_id;
        public List<String> facilities = new ArrayList<>();
        public int alert_number;
        public String alert_description;
        public String severity;
        public Map<String, Object> optional = new HashMap<>();
    }

}
