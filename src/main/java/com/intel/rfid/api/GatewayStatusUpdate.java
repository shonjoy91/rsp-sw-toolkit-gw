/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.Date;

public class GatewayStatusUpdate extends JsonNotification {

    public static final String METHOD_NAME = "gw_status_update";

    public static final String READY = "ready";
    public static final String SHUTTING_DOWN = "shutting_down";
    public static final String LOST = "lost";

    public Params params = new Params();

    public GatewayStatusUpdate() {
        method = METHOD_NAME;
    }

    public GatewayStatusUpdate(String _deviceId, String _status) {
        method = METHOD_NAME;
        params.device_id = _deviceId;
        params.status = _status;
    }

    public static class Params {
        public long sent_on = new Date().getTime();
        public String device_id;
        public String status;
    }

}
