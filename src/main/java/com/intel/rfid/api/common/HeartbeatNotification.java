/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.common;


public class HeartbeatNotification extends JsonNotification {

    public static final String METHOD_NAME = "heartbeat";

    public HeartbeatNotification() {
        method = METHOD_NAME;
        params.sent_on = System.currentTimeMillis();
    }

    public HeartbeatNotification(String _deviceId) {
        this();
        params.device_id = _deviceId;
    }


    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
        public String video_url;
    }

}
