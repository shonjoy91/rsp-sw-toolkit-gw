/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.SensorConnectionStateInfo;

public class SensorConnectionStateNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_connection_state_notification";

    public SensorConnectionStateInfo params;

    public SensorConnectionStateNotification() {
        method = METHOD_NAME;
    }
    
    public SensorConnectionStateNotification(SensorConnectionStateInfo _info) {
        this();
        params = _info;
    }
    
}
