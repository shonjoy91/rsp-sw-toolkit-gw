/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;

public class ClearGPIOMappingsRequest extends JsonRequest {

    public static final String METHOD_NAME = "clear_gpio_mappings";

    // keep default for Jackson mapper
    public ClearGPIOMappingsRequest() {
        method = METHOD_NAME;
    }

}
