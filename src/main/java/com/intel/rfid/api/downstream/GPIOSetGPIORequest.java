/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.data.GPIOInfo;

public class GPIOSetGPIORequest extends JsonRequest {

    public static final String METHOD_NAME = "gpio_set_gpio";

    public GPIOInfo params = new GPIOInfo();

    // keep default for Jackson mapper
    public GPIOSetGPIORequest() { method = METHOD_NAME; }

    public GPIOSetGPIORequest(GPIOInfo _gpio_info) {
        this();
        params = _gpio_info;
    }

}
