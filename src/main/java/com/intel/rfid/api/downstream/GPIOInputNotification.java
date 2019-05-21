/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.GPIOInfo;

public class GPIOInputNotification extends JsonNotification {

    public static final String METHOD_NAME = "gpio_input";

    public GPIOInputNotification() { method = METHOD_NAME; }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public GPIOInfo gpio_info;
    }
}
