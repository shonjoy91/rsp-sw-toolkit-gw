/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.intel.rfid.api.data.ScheduleRunState;
import com.intel.rfid.api.data.Cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulerSummary {
    public ScheduleRunState run_state;
    public List<ScheduleRunState> available_states = Arrays.asList(ScheduleRunState.values());
    public List<Cluster> clusters = new ArrayList<>();
    
    public void copyFrom(SchedulerSummary _other) {
        run_state = _other.run_state;
        clusters.addAll(_other.clusters);
    }
    
    
}
