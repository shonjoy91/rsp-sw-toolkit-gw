/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.schedule.SchedulerSummary;

public class SchedulerSummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "scheduler_summary";

    public SchedulerSummary params = new SchedulerSummary();

    public SchedulerSummaryNotification() {
        method = METHOD_NAME;
    }

    public SchedulerSummaryNotification(SchedulerSummary _summary) {
        this();
        params.copyFrom(_summary);
    }

}
