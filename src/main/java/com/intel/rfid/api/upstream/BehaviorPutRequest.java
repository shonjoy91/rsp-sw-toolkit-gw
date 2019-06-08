/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.sensor.Behavior;

public class BehaviorPutRequest extends JsonRequest {

    public static final String METHOD_NAME = "behavior_put";
    
    public Behavior params;

    public BehaviorPutRequest() {
        method = METHOD_NAME;
    }

    public BehaviorPutRequest(Behavior _behavior) {
        this();
        params = _behavior;
    }
}
