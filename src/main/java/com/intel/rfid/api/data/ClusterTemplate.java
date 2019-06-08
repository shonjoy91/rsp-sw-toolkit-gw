/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import java.util.ArrayList;
import java.util.List;

public class ClusterTemplate {
    public List<Personality> personalities = new ArrayList<>();
    public List<String> behavior_ids = new ArrayList<>();
    public List<String> sensor_device_ids = new ArrayList<>();
}
