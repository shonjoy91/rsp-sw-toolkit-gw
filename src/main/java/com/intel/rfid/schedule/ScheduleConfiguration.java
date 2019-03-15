/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a POJO to support JSON serialization / deserialization
 */
public class ScheduleConfiguration {

    public String id;
    public List<Cluster> clusters = new ArrayList<>();

    public static class Cluster {
        public String personality;
        public String facility_id;
        public String behavior_id;
        public List<List<String>> sensor_groups = new ArrayList<>();
    }
}
