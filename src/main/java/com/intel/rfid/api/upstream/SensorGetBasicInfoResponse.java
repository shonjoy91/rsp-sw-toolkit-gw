/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.data.SensorBasicInfo;

public class SensorGetBasicInfoResponse extends JsonResponseOK {

    public SensorGetBasicInfoResponse(String _id, SensorBasicInfo _info) {
        super(_id, Boolean.TRUE);
        result = _info;
    }

}
