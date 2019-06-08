/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class DownstreamGetMqttStatusRequest extends JsonRequest {

    public static final String METHOD_NAME = "downstream_get_mqtt_status";

    public DownstreamGetMqttStatusRequest() {
        method = METHOD_NAME;
    }
}
