/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.sensor.Behavior;

import java.util.List;

public class BehaviorResponse extends JsonResponseOK {

    public BehaviorResponse(String _id, List<Behavior> _behaviors) {
        super(_id, Boolean.TRUE);
        result = _behaviors;
    }

}
