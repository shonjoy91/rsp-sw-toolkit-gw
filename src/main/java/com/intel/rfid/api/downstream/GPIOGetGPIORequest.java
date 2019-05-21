/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;

public class GPIOGetGPIORequest extends JsonRequest {

    public static final String METHOD_NAME = "gpio_get_gpio";

    public GPIOGetGPIORequest() { method = METHOD_NAME; }

}
