/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;


public class StatusUpdate extends JsonNotification {

    public enum Status {
        unknown,
        ready,
        in_reset,
        shutting_down,
        firmware_update,
        lost
    }

    public static final String METHOD_NAME = "status_update";

    public StatusUpdate() {
        method = METHOD_NAME;
    }

    public StatusUpdate(String _deviceId, Status _status) {
        this();
        params.device_id = _deviceId;
        params.sent_on = System.currentTimeMillis();
        params.status = _status;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
        public Status status;

    }
}
