/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.schedule.SchedulerSummary;

public class SchedulerRunStateResponse extends JsonResponseOK {

    public SchedulerRunStateResponse(String _id, SchedulerSummary _summary) {
        super(_id, Boolean.TRUE);
        result = _summary;
    }
    
}
