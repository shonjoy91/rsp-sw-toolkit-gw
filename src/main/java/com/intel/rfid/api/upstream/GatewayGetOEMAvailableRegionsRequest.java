/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class GatewayGetOEMAvailableRegionsRequest extends JsonRequest {

    public static final String METHOD_NAME = "gateway_get_oem_available_regions";

    public class Params {
        public String device_id;
    }

    public Params params = new Params();

    public GatewayGetOEMAvailableRegionsRequest() {
        method = METHOD_NAME;
    }

}
