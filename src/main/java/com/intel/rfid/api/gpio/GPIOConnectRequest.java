/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.gpio;

import com.intel.rfid.api.JsonRequest;

public class GPIOConnectRequest extends JsonRequest {

    public static final String METHOD_NAME = "gpio_connect";

    public GPIOConnectRequest() {
        method = METHOD_NAME;
    }

    public GPIODeviceInfo params = new GPIODeviceInfo();

}
