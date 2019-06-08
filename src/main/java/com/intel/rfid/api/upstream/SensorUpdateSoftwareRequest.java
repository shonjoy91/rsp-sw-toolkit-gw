/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class SensorUpdateSoftwareRequest extends JsonRequest {

    public static final String METHOD_NAME = "sensor_update_software";

    public class Params {
        public String device_id;
    }

    public Params params = new Params();

    public SensorUpdateSoftwareRequest() {
        method = METHOD_NAME;
    }

}
