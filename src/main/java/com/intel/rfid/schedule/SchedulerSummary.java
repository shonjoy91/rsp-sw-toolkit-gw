/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.intel.rfid.cluster.Cluster;
import com.intel.rfid.schedule.ScheduleManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulerSummary {
    public ScheduleManager.RunState run_state;
    public List<ScheduleManager.RunState> available_states = Arrays.asList(ScheduleManager.RunState.values());
    public List<Cluster> clusters = new ArrayList<>();
    
    public void copyFrom(SchedulerSummary _other) {
        run_state = _other.run_state;
        clusters.addAll(_other.clusters);
    }
    
    
}
