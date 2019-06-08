/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonResponseOK;

public class GetGeoRegionResponse extends JsonResponseOK {

    public GetGeoRegionResponse(String _responseId,
                                GeoRegion _region) {

        super(_responseId, Boolean.TRUE);
        result = new Result(_region);
    }

    public static class Result {

        public GeoRegion region;

        public Result(GeoRegion _region) {
            region = _region;
        }
    }

}
