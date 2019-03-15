/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a simplification of the set_device_alert json API
 * which extracts the desired action and associated params for this
 * particular action.
 * <p>
 * acknowledge an alert
 * muting an alert
 * setting an alert threshold
 */
public class SetAlertThreshold extends JsonRequest {

    public static final String METHOD_NAME = "set_device_alert";

    public SetAlertThreshold() { method = METHOD_NAME; }

    public SetAlertThreshold(int _alertNumber,
                             DeviceAlert.Severity _severity,
                             Number _threshold) {
        this();

        Param t = new Param();
        t.alert_number = _alertNumber;
        t.severity = _severity;
        t.threshold = _threshold;

        params.add(t);
    }

    public List<Param> params = new ArrayList<>();

    public static class Param {
        public int alert_number;
        public DeviceAlert.Severity severity = DeviceAlert.Severity.info;
        public Number threshold;
    }
}
