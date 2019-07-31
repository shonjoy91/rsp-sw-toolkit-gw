/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;

public class RspControllerHeartbeatNotification extends JsonNotification {

    public static final String METHOD_NAME = "controller_heartbeat";

    public RspControllerHeartbeatNotification() {
        method = METHOD_NAME;
    }

    public RspControllerHeartbeatNotification(String _deviceId) {
        this();
        params.sent_on = System.currentTimeMillis();
        params.device_id = _deviceId;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
    }

}
