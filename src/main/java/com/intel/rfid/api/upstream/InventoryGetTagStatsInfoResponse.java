/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.data.TagStatsInfo;

public class InventoryGetTagStatsInfoResponse extends JsonResponseOK {

    public InventoryGetTagStatsInfoResponse(String _id, TagStatsInfo _tagStatsInfo) {
        super(_id, Boolean.TRUE);
        result = _tagStatsInfo;
    }


}
