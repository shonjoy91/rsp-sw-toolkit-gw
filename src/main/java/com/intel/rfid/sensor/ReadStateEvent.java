/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.intel.rfid.api.data.ReadState;

public class ReadStateEvent {
    
    public String deviceId;
    public ReadState previous;
    public ReadState current;
    public String behaviorId;
    
    public ReadStateEvent(String _deviceId,
                          ReadState _previous,
                          ReadState _current,
                          String _behaviorId) {

        deviceId = _deviceId;
        previous = _previous;
        current = _current;
        behaviorId = _behaviorId;
        
    }

    
}
