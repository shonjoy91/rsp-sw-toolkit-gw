/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.inventory.MobilityProfile;

public class MobilityProfilePutRequest extends JsonRequest {

    public static final String METHOD_NAME = "mobility_profile_put";

    public MobilityProfile params;

    public MobilityProfilePutRequest() {
        method = METHOD_NAME;
    }

    public MobilityProfilePutRequest(MobilityProfile _mobilityProfile) {
        this();
        params = _mobilityProfile;
    }
}
