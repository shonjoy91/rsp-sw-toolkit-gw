/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.gateway.GatewayStatus;

import java.util.Date;

public class GatewayStatusUpdateNotification extends JsonNotification {

    public static final String METHOD_NAME = "gw_status_update";

    public Params params = new Params();

    public GatewayStatusUpdateNotification() {
        method = METHOD_NAME;
    }

    public GatewayStatusUpdateNotification(String _deviceId, GatewayStatus _status) {
        this();
        params.device_id = _deviceId;
        params.status = _status.label;
    }

    public static class Params {
        public long sent_on = System.currentTimeMillis();
        public String device_id;
        public String status;
    }

}
