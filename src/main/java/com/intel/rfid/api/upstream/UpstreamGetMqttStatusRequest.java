/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class UpstreamGetMqttStatusRequest extends JsonRequest {

    public static final String METHOD_NAME = "upstream_get_mqtt_status";

    public UpstreamGetMqttStatusRequest() {
        method = METHOD_NAME;
    }
}
