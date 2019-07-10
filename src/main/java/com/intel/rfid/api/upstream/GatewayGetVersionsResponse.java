/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.sensor.GatewayVersions;

public class GatewayGetVersionsResponse extends JsonResponseOK {

    public GatewayGetVersionsResponse(String _id, GatewayVersions _versions) {
        super(_id, Boolean.TRUE);
        result = _versions;
    }

}
