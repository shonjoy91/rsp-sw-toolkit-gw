/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class ClusterGetConfigRequest extends JsonRequest {

    public static final String METHOD_NAME = "cluster_get_config";

    public ClusterGetConfigRequest() {
        method = METHOD_NAME;
    }
}
