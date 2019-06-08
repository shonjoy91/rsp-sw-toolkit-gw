/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.gpio;

import com.intel.rfid.api.JsonRequest;

public class GPIOSetStateRequest extends JsonRequest {

    public static final String METHOD_NAME = "gpio_set_state";

    public GPIOInfo params = new GPIOInfo();

    // keep default for Jackson mapper
    public GPIOSetStateRequest() { method = METHOD_NAME; }

    public GPIOSetStateRequest(GPIOInfo _gpio_info) {
        this();
        params = _gpio_info;
    }

}
