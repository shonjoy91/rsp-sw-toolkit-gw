/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.data.TagInfo;

import java.util.List;

public class InventoryGetTagInfoResponse extends JsonResponseOK {

    public InventoryGetTagInfoResponse(String _id, List<TagInfo> _infoList) {
        super(_id, Boolean.TRUE);
        result = _infoList;
    }


}
