/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.SensorConfigInfo;

public class SensorConfigNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_config_notification";

    public SensorConfigInfo params;

    public SensorConfigNotification() {
        method = METHOD_NAME;
    }
    
    public SensorConfigNotification(SensorConfigInfo _sensorConfigInfo) {
        this();
        params = _sensorConfigInfo;
    }


}
