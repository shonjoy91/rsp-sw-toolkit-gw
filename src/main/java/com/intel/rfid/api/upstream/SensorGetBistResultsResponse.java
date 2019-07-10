/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.sensor.BISTResults;

public class SensorGetBistResultsResponse extends JsonResponseOK {

    public SensorGetBistResultsResponse(String _id, BISTResults _results) {
        super(_id, Boolean.TRUE);
        result = _results;
    }

}
