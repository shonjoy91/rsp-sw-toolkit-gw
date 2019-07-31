/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class InventoryGetActiveMobilityProfileIdRequest extends JsonRequest {

    public static final String METHOD_NAME = "inventory_get_active_mobility_profile_id";

    public InventoryGetActiveMobilityProfileIdRequest() {
        method = METHOD_NAME;
    }

}
