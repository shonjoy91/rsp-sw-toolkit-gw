/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;

import java.util.Date;

public class GatewayStatusUpdateNotification extends JsonNotification {

    public static final String METHOD_NAME = "gw_status_update";

    public static final String READY = "ready";
    public static final String SHUTTING_DOWN = "shutting_down";
    public static final String LOST = "lost";

    public Params params = new Params();

    public GatewayStatusUpdateNotification() {
        method = METHOD_NAME;
    }

    public GatewayStatusUpdateNotification(String _deviceId, String _status) {
        this();
        params.device_id = _deviceId;
        params.status = _status;
    }

    public static class Params {
        public long sent_on = new Date().getTime();
        public String device_id;
        public String status;
    }

}
