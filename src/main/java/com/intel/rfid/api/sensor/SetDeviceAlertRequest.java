/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

import java.util.ArrayList;
import java.util.List;

public class SetDeviceAlertRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_device_alert";

    public SetDeviceAlertRequest() { method = METHOD_NAME; }

    public SetDeviceAlertRequest(List<Param> _params) {
        this();

        params.addAll(_params);
    }

    public List<Param> params = new ArrayList<>();

    public static class Param {
        public int alert_number;
        public AlertSeverity severity;
        public Integer threshold;
        public boolean acknowledge;
        public boolean mute;
    }

}
