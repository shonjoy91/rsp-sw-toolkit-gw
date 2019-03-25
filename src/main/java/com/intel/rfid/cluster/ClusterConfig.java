/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.cluster;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a POJO to support JSON serialization / deserialization
 */
public class ClusterConfig {

    public String id;
    public List<Cluster> clusters = new ArrayList<>();

}
