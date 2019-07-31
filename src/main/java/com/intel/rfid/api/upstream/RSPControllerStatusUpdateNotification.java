/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.controller.RSPControllerStatus;

public class RSPControllerStatusUpdateNotification extends JsonNotification {

    public static final String METHOD_NAME = "rsp_controller_status_update";

    public Params params = new Params();

    public RSPControllerStatusUpdateNotification() {
        method = METHOD_NAME;
    }

    public RSPControllerStatusUpdateNotification(String _deviceId, RSPControllerStatus _status) {
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
