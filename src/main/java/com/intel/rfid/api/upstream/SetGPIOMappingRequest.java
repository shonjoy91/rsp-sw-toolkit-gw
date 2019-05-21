/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.data.GPIOMapping;

public class SetGPIOMappingRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_gpio_mapping";

    public GPIOMapping params = new GPIOMapping();

    // keep default for Jackson mapper
    public SetGPIOMappingRequest() {
        method = METHOD_NAME;
    }

}
