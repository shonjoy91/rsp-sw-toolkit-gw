/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.sensor.GeoRegion;

public class SensorSetGeoRegionRequest extends JsonRequest {

    public static final String METHOD_NAME = "sensor_set_geo_region";

    public class Params {
        public String devicd_id;
        public GeoRegion region;
    }

    public Params params = new Params();

    public SensorSetGeoRegionRequest() {
        method = METHOD_NAME;
    }

    public SensorSetGeoRegionRequest(String _deviceId, GeoRegion _region) {
        this();
        params.devicd_id = _deviceId;
        params.region = _region;
    }

}
