/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.ConnectionState;
import com.intel.rfid.api.data.ConnectionStateEvent;

public class SensorConnectionStateNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_connection_state";

    public Params params = new Params();

    public SensorConnectionStateNotification() {
        method = METHOD_NAME;
    }
    
    public SensorConnectionStateNotification(ConnectionStateEvent _event) {
        this();
        params.device_id = _event.rsp.getDeviceId();
        params.previous = _event.previous;
        params.current = _event.current;
        params.cause = _event.cause;
    }

    public class Params{
        public String device_id;
        public ConnectionState previous;
        public ConnectionState current;
        public ConnectionStateEvent.Cause cause;
    }

}
