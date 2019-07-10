/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.gpio.GPIOMapping;

public class GpioSetMappingRequest extends JsonRequest {

    public static final String METHOD_NAME = "gpio_set_mapping";

    public GPIOMapping params = new GPIOMapping();

    // keep default for Jackson mapper
    public GpioSetMappingRequest() {
        method = METHOD_NAME;
    }

}
