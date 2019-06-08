/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.MqttStatus;

public class UpstreamMqttStatusNotification extends JsonNotification {

    public static final String METHOD_NAME = "upstream_mqtt_status";

    public MqttStatus params;

    public UpstreamMqttStatusNotification() {
        method = METHOD_NAME;
    }

    public UpstreamMqttStatusNotification(MqttStatus _status) {
        this();
        params = _status;
    }

}
