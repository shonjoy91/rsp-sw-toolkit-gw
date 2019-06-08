/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.SensorReadStateInfo;

public class SensorReadStateNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_read_state_notification";

    public SensorReadStateInfo params;

    public SensorReadStateNotification() {
        method = METHOD_NAME;
    }

    public SensorReadStateNotification(SensorReadStateInfo _info) {
        this();
        params = _info;
    }

}
