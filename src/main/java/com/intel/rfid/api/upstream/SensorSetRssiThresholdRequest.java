/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class SensorSetRssiThresholdRequest extends JsonRequest {

    public static final String METHOD_NAME = "sensor_set_min_rssi";

    public class Params {
        public String device_id;
        public int threshold;
    }

    public Params params = new Params();

    public SensorSetRssiThresholdRequest() {
        method = METHOD_NAME;
    }

    public SensorSetRssiThresholdRequest(String _device_id, int _threshold) {
        this();
        params.device_id = _device_id;
        params.threshold = _threshold;
    }
}
