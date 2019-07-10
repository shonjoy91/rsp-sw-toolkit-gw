/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

public class Connection {

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public enum Cause {
        SHUTTING_DOWN,
        LOST_DOWNSTREAM_COMMS,
        LOST_HEARTBEAT,
        READY,
        IN_RESET,
        RESYNC,
        FORCED_DISCONNECT,
        REMOVED
    }
}
