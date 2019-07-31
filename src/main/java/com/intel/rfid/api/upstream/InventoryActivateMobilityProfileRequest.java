/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class InventoryActivateMobilityProfileRequest extends JsonRequest {

    public static final String METHOD_NAME = "inventory_activate_mobility_profile";

    public InventoryActivateMobilityProfileRequest() {
        method = METHOD_NAME;
    }
    
    public InventoryActivateMobilityProfileRequest(String _mobilityProfileId) {
        this();
        params.mobility_profile_id = _mobilityProfileId;
    }

    public class Params {
        public String mobility_profile_id;
    }

    public Params params = new Params();


}
