/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.upstream;

import com.intel.rfid.api.sensor.GetStateResponse;
import com.intel.rfid.api.sensor.RSPInfo;

public class SensorGetStateResponse extends GetStateResponse {
    public SensorGetStateResponse(String _id, RSPInfo _info) {
        super(_id, _info);
    }
}
