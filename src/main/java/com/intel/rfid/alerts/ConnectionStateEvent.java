/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.alerts;

import com.intel.rfid.api.data.Connection;
import com.intel.rfid.sensor.SensorPlatform;

public class ConnectionStateEvent {

    public final SensorPlatform rsp;
    public final Connection.State previous;
    public final Connection.State current;
    public final Connection.Cause cause;


    public ConnectionStateEvent(SensorPlatform _rsp,
                                Connection.State _prev,
                                Connection.State _current,
                                Connection.Cause _cause) {
        rsp = _rsp;
        previous = _prev;
        current = _current;
        cause = _cause;
    }

}
