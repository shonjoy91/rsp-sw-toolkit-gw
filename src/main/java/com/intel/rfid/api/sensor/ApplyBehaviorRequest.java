/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class ApplyBehaviorRequest extends JsonRequest {

    public static final String METHOD_NAME = "apply_behavior";

    public ApplyBehaviorRequest() {
        method = METHOD_NAME;
    }

    public ApplyBehaviorRequest(Action _action, Behavior _behavior) {
        this();
        params.behavior = _behavior;
        params.action = _action;
        params.action_time = 0;
    }

    public Params params = new Params();

    public enum Action {START, STOP}

    public static class Params {
        public Action action;
        public long action_time;
        public Behavior behavior;
    }

}
