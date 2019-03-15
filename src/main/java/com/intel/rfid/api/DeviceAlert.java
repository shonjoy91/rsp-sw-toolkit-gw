/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.Map;

public class DeviceAlert extends JsonNotification {

    public static final String METHOD_NAME = "device_alert";

    public enum Type {
        Unknown(0),
        RfModuleError(100),
        HighAmbientTemp(101),
        HighCpuTemp(102),
        HighCpuUsage(103),
        HighMemoryUsage(104),
        LowVoltageError(105),
        DeviceMoved(151);

        public final int id;

        Type(int _id) { id = _id; }

    }

    public enum Severity {info, warning, urgent, critical}

    public Params params = new Params();

    public DeviceAlert() {
        method = METHOD_NAME;
    }

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
        public int alert_number;
        public String alert_description;
        public String severity;
        public Map<String, Object> optional;
    }

}
