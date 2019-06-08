/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class BehaviorDeleteRequest extends JsonRequest {

    public static final String METHOD_NAME = "behavior_delete";

    public class Params {
        public String behavior_id;
    }

    public Params params = new Params();

    public BehaviorDeleteRequest() {
        method = METHOD_NAME;
    }

    public BehaviorDeleteRequest(String _behaviorId) {
        this();
        params.behavior_id = _behaviorId;
    }
}
