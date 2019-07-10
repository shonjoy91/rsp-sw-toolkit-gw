/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class SchedulerGetRunStateRequest extends JsonRequest {

    public static final String METHOD_NAME = "scheduler_get_run_state";

    // keep default for Jackson mapper
    public SchedulerGetRunStateRequest() {
        method = METHOD_NAME;
    }

}
