/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.data.FilterPattern;

public class InventoryGetTagStatsInfoRequest extends JsonRequest {

    public static final String METHOD_NAME = "inventory_get_tag_stats_info";

    public FilterPattern params;

    // keep default for Jackson mapper
    public InventoryGetTagStatsInfoRequest() {
        method = METHOD_NAME;
    }

    public InventoryGetTagStatsInfoRequest(FilterPattern _pattern) {
        this();
        params = _pattern;
    }
}
