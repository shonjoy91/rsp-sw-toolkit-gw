/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.sensor.RSPControllerVersions;

public class RSPControllerGetVersionsResponse extends JsonResponseOK {

    public RSPControllerGetVersionsResponse(String _id, RSPControllerVersions _versions) {
        super(_id, Boolean.TRUE);
        result = _versions;
    }

}
