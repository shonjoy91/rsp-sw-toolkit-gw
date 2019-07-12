/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class GatewayGetAllGeoRegionsRequest extends JsonRequest {

    public static final String METHOD_NAME = "gateway_get_all_geo_regions";

    public class Params {
        public String device_id;
    }

    public Params params = new Params();

    public GatewayGetAllGeoRegionsRequest() {
        method = METHOD_NAME;
    }

}
