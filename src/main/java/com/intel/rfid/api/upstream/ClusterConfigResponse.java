/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.data.ClusterConfig;

public class ClusterConfigResponse extends JsonResponseOK {

    public ClusterConfigResponse(String _id, ClusterConfig _clusterConfig) {
        super(_id, Boolean.TRUE);
        result = _clusterConfig;
    }

}
