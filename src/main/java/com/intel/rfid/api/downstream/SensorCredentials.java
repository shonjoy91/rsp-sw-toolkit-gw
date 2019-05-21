/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

public class SensorCredentials {
    public String mqtt_uri;
    public String mqtt_topic_prefix;
    public String mqtt_password;

    public SensorCredentials() {
        // for JSON deserialization
    }

    public SensorCredentials(String _mqtt_uri, String _mqtt_topic_prefix, String _mqtt_password) {
        mqtt_uri = _mqtt_uri;
        mqtt_topic_prefix = _mqtt_topic_prefix;
        mqtt_password = _mqtt_password;
    }
}
