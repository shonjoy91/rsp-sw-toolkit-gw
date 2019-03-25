/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class SensorShowInfoNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_show_info";

    public SensorShowInfo params = new SensorShowInfo();

    public SensorShowInfoNotification() {
        method = METHOD_NAME;
    }

    public SensorShowInfoNotification(SensorShowInfo _summary) {
        this();
        params.copyFrom(_summary);
    }

}
