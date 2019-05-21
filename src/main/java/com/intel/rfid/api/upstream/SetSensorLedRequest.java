/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.data.LEDState;

public class SetSensorLedRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_sensor_led";

    public Params params = new Params();

    public SetSensorLedRequest() {
        method = METHOD_NAME;
    }

    public static class Params {
        public LEDState led_state;
    }
}
