/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;

import java.util.ArrayList;
import java.util.List;

public class SensorGetDeviceIdsResponse extends JsonResponseOK {

    public SensorGetDeviceIdsResponse(String _id, List<String> _deviceIds) {
        super(_id, Boolean.TRUE);
        result = new ArrayList<>(_deviceIds);
    }

}
