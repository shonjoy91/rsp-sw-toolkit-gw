/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class SetLED extends JsonRequest {

    public static final String METHOD_NAME = "set_led";

    public LEDState params;

    // keep default for Jackson mapper
    public SetLED() { method = METHOD_NAME; }

    public SetLED(LEDState _state) {
        this();
        params = _state;
    }

}
