/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.controller;

public enum RSPControllerStatus {

    UNKNOWN(0, "unknown"),
    OK(1, "ok"), // included so this enum can return status from method calls
    // A couple of RSP codes that originate at the RSP Controller
    // (check the RSP alert description and values for additional info)
    RSP_CONNECTED(195, "rsp_connected"),
    RSP_SHUTTING_DOWN(196, "rsp_shutting_down"),
    RSP_FACILITY_NOT_CONFIGURED(197, "rsp_facilitiy_not_configured"),
    RSP_LOST_HEARTBEAT(198, "rsp_lost_heartbeat"),
    RSP_LAST_WILL_AND_TESTAMENT(199, "rsp_last_will_and_testament"),

    // RSP Controller Json RPC error codes start at 200
    // the descriptions must be simple to maintain backward compatibility
    RSP_CONTROLLER_STARTED(240, "controller_ready"),
    RSP_CONTROLLER_SHUTTING_DOWN(241, "controller_shutting_down"),
    RSP_CONTROLLER_TRIGGERED_RSP_DISCONNECT(242, "lost"),
    ;

    public final int id;
    public final String label;

    RSPControllerStatus(int _id, String _label) {
        id = _id;
        label = _label;
    }

}
