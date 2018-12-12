/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class ApplyBehavior extends JsonRequest {

    public static final String METHOD_NAME = "apply_behavior";

    public ApplyBehavior() {
        method = METHOD_NAME;
    }

    public ApplyBehavior(Action _action, Behavior _behavior) {
        this();
        params.action = _action;
        if (_behavior != null) {
            params.behavior = _behavior;
            params.auto_repeat = _behavior.auto_repeat;
        }
    }

    public Params params = new Params();

    public enum Action {START, STOP}

    public static class Params {
        // default all of these to start
        // that will be the case most of the time
        public Action action;
        // action time is not yet supported as of RSP version 1.0.2
        // breaks functionality
        //public long action_time;
        public boolean auto_repeat = true;
        public Behavior behavior;
    }

}
