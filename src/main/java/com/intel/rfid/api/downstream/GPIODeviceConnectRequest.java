/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.data.GPIODeviceInfo;

public class GPIODeviceConnectRequest extends JsonRequest {

    public static final String METHOD_NAME = "gpio_connect";

    public GPIODeviceConnectRequest() {
        method = METHOD_NAME;
    }

    public GPIODeviceInfo params = new GPIODeviceInfo();

}
