/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.sensor.SensorStateSummary;

public class SensorStateSummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_state_summary";

    public SensorStateSummary params = new SensorStateSummary();

    public SensorStateSummaryNotification() {
        method = METHOD_NAME;
    }

    public SensorStateSummaryNotification(SensorStateSummary _summary) {
        this();
        params.copyFrom(_summary);
    }

}
