/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetGeoRegionRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_geo_region";

    public class Params {
        public GeoRegion region;
    }

    public Params params;

    // keep default for Jackson mapper
    public SetGeoRegionRequest() {
        method = METHOD_NAME;
        params = new Params();
    }

    public SetGeoRegionRequest(GeoRegion _region) {
        this();
        params.region = _region;
    }
}
