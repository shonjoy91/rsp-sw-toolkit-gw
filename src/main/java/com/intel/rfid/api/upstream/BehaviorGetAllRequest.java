/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class BehaviorGetAllRequest extends JsonRequest {

    public static final String METHOD_NAME = "behavior_get_all";

    public BehaviorGetAllRequest() {
        method = METHOD_NAME;
    }

}
