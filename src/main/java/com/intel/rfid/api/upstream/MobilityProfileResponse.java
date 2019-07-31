/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.inventory.MobilityProfile;

import java.util.List;

public class MobilityProfileResponse extends JsonResponseOK {

    public MobilityProfileResponse(String _id, List<MobilityProfile> _profiles) {
        super(_id, Boolean.TRUE);
        result = _profiles;
    }

}
