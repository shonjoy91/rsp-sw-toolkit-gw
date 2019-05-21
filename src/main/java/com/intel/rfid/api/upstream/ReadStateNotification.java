/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.ReadState;
import com.intel.rfid.api.data.ReadStateEvent;

public class ReadStateNotification extends JsonNotification {

    public static final String METHOD_NAME = "read_state_notification";

    public Params params = new Params();

    public ReadStateNotification() {
        method = METHOD_NAME;
    }

    public ReadStateNotification(ReadStateEvent _event) {
        this();
        params.device_id = _event.rsp.getDeviceId();
        params.previous = _event.previous;
        params.current = _event.current;
    }

    public class Params {
        public String device_id;
        public ReadState previous;
        public ReadState current;
    }
}
