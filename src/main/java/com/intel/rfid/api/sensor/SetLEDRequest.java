/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetLEDRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_led";

    public LEDState params;

    // keep default for Jackson mapper
    public SetLEDRequest() { method = METHOD_NAME; }

    public SetLEDRequest(LEDState _state) {
        this();
        params = _state;
    }

}
