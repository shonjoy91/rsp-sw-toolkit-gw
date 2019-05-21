/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.sensor.SensorManagerSummary;

public class SensorManagerSummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_manager_summary";

    public SensorManagerSummary params = new SensorManagerSummary();

    public SensorManagerSummaryNotification() {
        method = METHOD_NAME;
    }

    public SensorManagerSummaryNotification(SensorManagerSummary _summary) {
        this();
        params.copyFrom(_summary);
    }

}
