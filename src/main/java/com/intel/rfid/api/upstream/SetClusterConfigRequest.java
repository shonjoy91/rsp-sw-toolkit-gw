/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.cluster.ClusterConfig;

public class SetClusterConfigRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_cluster_config";

    public Params params = new Params();

    public SetClusterConfigRequest() {
        method = METHOD_NAME;
    }

    public static class Params {
        public ClusterConfig cluster_config;
    }
}
