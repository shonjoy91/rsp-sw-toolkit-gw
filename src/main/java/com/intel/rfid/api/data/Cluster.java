/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import com.intel.rfid.security.ProvisionToken;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    public String id;
    public Personality personality;
    public String facility_id;
    public List<String> aliases = new ArrayList<>();
    public String behavior_id;
    public List<List<String>> sensor_groups = new ArrayList<>();
    public List<ProvisionToken> tokens = new ArrayList<>();
}
