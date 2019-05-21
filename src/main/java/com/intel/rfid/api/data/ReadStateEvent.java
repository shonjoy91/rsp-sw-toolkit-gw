/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import com.intel.rfid.sensor.SensorPlatform;

public class ReadStateEvent {
    
    public final SensorPlatform rsp;
    public final ReadState previous;
    public final ReadState current;


    public ReadStateEvent(SensorPlatform _rsp,
                          ReadState _prev,
                          ReadState _current) {
        rsp = _rsp;
        previous = _prev;
        current = _current;
    }

}
