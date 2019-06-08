/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.MqttStatus;

public class DownstreamMqttStatusNotification extends JsonNotification {

    public static final String METHOD_NAME = "downstream_mqtt_status";

    public MqttStatus params;

    public DownstreamMqttStatusNotification() {
        method = METHOD_NAME;
    }

    public DownstreamMqttStatusNotification(MqttStatus _status) {
        this();
        params = _status;
    }

}
