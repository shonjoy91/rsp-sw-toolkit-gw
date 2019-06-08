/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.data.FilterPattern;

public class InventoryGetTagInfoRequest extends JsonRequest {

    public static final String METHOD_NAME = "inventory_get_tag_info";

    public FilterPattern params;

    // keep default for Jackson mapper
    public InventoryGetTagInfoRequest() {
        method = METHOD_NAME;
    }

    public InventoryGetTagInfoRequest(FilterPattern _pattern) {
        this();
        params = _pattern;
    }
}
