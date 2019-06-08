/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.schedule.SchedulerSummary;

public class SchedulerRunStateNotification extends JsonNotification {

    public static final String METHOD_NAME = "scheduler_run_state";

    public SchedulerSummary params = new SchedulerSummary();

    public SchedulerRunStateNotification() {
        method = METHOD_NAME;
    }

    public SchedulerRunStateNotification(SchedulerSummary _summary) {
        this();
        params.copyFrom(_summary);
    }

}
