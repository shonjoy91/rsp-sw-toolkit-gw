/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.sensor.LEDState;

public class SensorSetLedRequest extends JsonRequest {

    public static final String METHOD_NAME = "sensor_set_led";

    public class Params {
        public String device_id;
        public LEDState led_state;
    }

    public Params params = new Params();
    
    public SensorSetLedRequest() {
        method = METHOD_NAME;
    }

    public SensorSetLedRequest(String _deviceId, LEDState _ledState) {
        this();
        params.device_id = _deviceId;
        params.led_state = _ledState;
    }
}
