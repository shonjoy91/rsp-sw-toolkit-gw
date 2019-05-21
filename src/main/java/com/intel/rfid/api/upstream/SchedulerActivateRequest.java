/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.schedule.ScheduleManager;

public class SchedulerActivateRequest extends JsonRequest {

    public static final String METHOD_NAME = "scheduler_activate";

    public ScheduleManager.RunState params = ScheduleManager.RunState.INACTIVE;

    // keep default for Jackson mapper
    public SchedulerActivateRequest() {
        method = METHOD_NAME;
    }

}
