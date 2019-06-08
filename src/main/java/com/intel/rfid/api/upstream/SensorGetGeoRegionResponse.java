/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.sensor.GeoRegion;
import com.intel.rfid.api.sensor.GetGeoRegionResponse;

public class SensorGetGeoRegionResponse extends GetGeoRegionResponse {

    public SensorGetGeoRegionResponse(String _id, GeoRegion _region) {
        super(_id, _region);
    }
    
}
