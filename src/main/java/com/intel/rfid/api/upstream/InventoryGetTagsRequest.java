/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;

public class InventoryGetTagsRequest extends JsonRequest {

    public static final String METHOD_NAME = "get_tags";

    public String params = null;

    // keep default for Jackson mapper
    public InventoryGetTagsRequest() {
        method = METHOD_NAME;
    }

}
