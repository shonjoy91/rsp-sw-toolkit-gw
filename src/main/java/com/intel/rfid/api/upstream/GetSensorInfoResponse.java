/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.data.SensorBasicInfo;
import com.intel.rfid.api.common.JsonResponseOK;

import java.util.ArrayList;
import java.util.List;

public class GetSensorInfoResponse extends JsonResponseOK {

    public GetSensorInfoResponse(String _id, List<SensorBasicInfo> _sensorInfoList) {
        super(_id, Boolean.TRUE);
        result = new ArrayList<>(_sensorInfoList);
    }
    
}
