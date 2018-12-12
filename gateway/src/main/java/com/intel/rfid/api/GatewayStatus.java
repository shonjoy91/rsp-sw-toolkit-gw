/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public enum GatewayStatus {

    UNKNOWN(0),
    OK(1), // included so this enum can return status from method calls
    // A couple of RSP codes that originate at the Gateway
    RSP_CONNECTED(195),
    RSP_SHUTTING_DOWN(196),
    RSP_FACILITY_NOT_CONFIGURED(197),
    RSP_LOST_HEARTBEAT(198),
    RSP_LAST_WILL_AND_TESTAMENT(199),

    // Gateway Json RPC error codes start at 200
    GATEWAY_STARTED(240),
    GATEWAY_SHUTTING_DOWN(241),;

    public final int id;

    GatewayStatus(int _id) { id = _id; }

}
