/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetAlertThresholdRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_alert_threshold";

    public Params params = new Params();

    public SetAlertThresholdRequest() { method = METHOD_NAME; }

    public SetAlertThresholdRequest(int _alertNumber, AlertSeverity _severity, Integer _threshold) {
        this();

        params.alert_number = _alertNumber;
        params.severity = _severity;
        params.threshold = _threshold;
    }

    public static class Params {
        public int alert_number;
        public AlertSeverity severity;
        public Integer threshold;
    }
}
