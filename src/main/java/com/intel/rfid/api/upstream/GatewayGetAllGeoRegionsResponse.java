/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;

public class GatewayGetAllGeoRegionsResponse extends JsonResponseOK {

    public GatewayGetAllGeoRegionsResponse(String _id, String[] _regions) {
        super(_id, Boolean.TRUE);
        result = _regions;
    }

}
