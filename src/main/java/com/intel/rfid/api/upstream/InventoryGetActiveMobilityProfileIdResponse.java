/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.data.SensorSoftwareRepoVersions;

public class InventoryGetActiveMobilityProfileIdResponse extends JsonResponseOK {

    public InventoryGetActiveMobilityProfileIdResponse(String _id, String _mobilityProfileId) {
        super(_id, Boolean.TRUE);
        result = _mobilityProfileId;
    }

}
