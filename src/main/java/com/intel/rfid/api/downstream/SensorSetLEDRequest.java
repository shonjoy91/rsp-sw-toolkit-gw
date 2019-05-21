/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.data.LEDState;

public class SensorSetLEDRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_led";

    public LEDState params;

    // keep default for Jackson mapper
    public SensorSetLEDRequest() { method = METHOD_NAME; }

    public SensorSetLEDRequest(LEDState _state) {
        this();
        params = _state;
    }

}
