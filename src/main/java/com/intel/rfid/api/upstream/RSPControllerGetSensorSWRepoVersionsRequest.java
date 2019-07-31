/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class RSPControllerGetSensorSWRepoVersionsRequest extends JsonRequest {

    public static final String METHOD_NAME = "controller_get_sensor_sw_repo_versions";

    public class Params {
        public String device_id;
    }

    public Params params = new Params();

    public RSPControllerGetSensorSWRepoVersionsRequest() {
        method = METHOD_NAME;
    }

}
