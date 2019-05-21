/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;

public class GetSensorInfoRequest extends JsonRequest {

    public static final String METHOD_NAME = "get_sensor_info";

    public GetSensorInfoRequest() {
        method = METHOD_NAME;
    }

}
