/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.data.ScheduleRunState;

public class SchedulerSetRunStateRequest extends JsonRequest {

    public static final String METHOD_NAME = "scheduler_set_run_state";

    public static class Params {
        public ScheduleRunState run_state;
    }

    public Params params = new Params();

    // keep default for Jackson mapper
    public SchedulerSetRunStateRequest() {
        method = METHOD_NAME;
    }

    public SchedulerSetRunStateRequest(ScheduleRunState _runState) {
        this();
        params.run_state = _runState;
    }
}
