/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

public class ConnectionStateEvent {

    public enum Cause {
        SHUTTING_DOWN,
        LOST_DOWNSTREAM_COMMS,
        LOST_HEARTBEAT,
        READY,
        IN_RESET,
        RESYNC
    }

    public final SensorPlatform rsp;
    public final ConnectionState previous;
    public final ConnectionState current;
    public final Cause cause;


    public ConnectionStateEvent(SensorPlatform _rsp,
                                ConnectionState _prev,
                                ConnectionState _current,
                                Cause _cause) {
        rsp = _rsp;
        previous = _prev;
        current = _current;
        cause = _cause;
    }

}
