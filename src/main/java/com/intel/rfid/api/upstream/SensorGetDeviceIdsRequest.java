/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class SensorGetDeviceIdsRequest extends JsonRequest {

    public static final String METHOD_NAME = "sensor_get_device_ids";

    public SensorGetDeviceIdsRequest() {
        method = METHOD_NAME;
    }

}
