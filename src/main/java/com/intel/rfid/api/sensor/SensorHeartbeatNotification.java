/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;


import com.intel.rfid.api.JsonNotification;

public class SensorHeartbeatNotification extends JsonNotification {

    public static final String METHOD_NAME = "heartbeat";

    public SensorHeartbeatNotification() {
        method = METHOD_NAME;
        params.sent_on = System.currentTimeMillis();
    }

    public SensorHeartbeatNotification(String _deviceId) {
        this();
        params.device_id = _deviceId;
    }


    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
        public Location location;
        public String video_url;
    }

}
