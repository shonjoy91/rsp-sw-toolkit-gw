/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class BehaviorGetRequest extends JsonRequest {

    public static final String METHOD_NAME = "behavior_get";

    public class Params {
        public String behavior_id;
    }

    public Params params = new Params();

    public BehaviorGetRequest() {
        method = METHOD_NAME;
    }

    public BehaviorGetRequest(String _behaviorId) {
        this();
        params.behavior_id = _behaviorId;
    }
}
