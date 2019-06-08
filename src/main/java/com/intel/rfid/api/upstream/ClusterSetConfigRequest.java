/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.data.ClusterConfig;

public class ClusterSetConfigRequest extends JsonRequest {

    public static final String METHOD_NAME = "cluster_set_config";

    public ClusterConfig params;

    public ClusterSetConfigRequest() {
        method = METHOD_NAME;
    }

    public ClusterSetConfigRequest(ClusterConfig _clusterConfig) {
        this();
        params = _clusterConfig;
    }
}
