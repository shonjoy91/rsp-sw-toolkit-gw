/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class GpioClearMappingsRequest extends JsonRequest {

    public static final String METHOD_NAME = "gpio_clear_mappings";

    // keep default for Jackson mapper
    public GpioClearMappingsRequest() {
        method = METHOD_NAME;
    }

}
