/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.gpio;

public class GPIOMapping {

    public String sensor_device_id = null;
    public String gpio_device_id = null;
    public GPIOInfo gpio_info = new GPIOInfo();
    public GPIO.PinFunction function = GPIO.PinFunction.NOT_ASSIGNED;

    private static final String FMT = "%-12s %-12s %-12s %-24s";
    public static final String HDR = String
            .format(FMT, "sensor id", "gpio id", "gpio index", "pin function");

    @Override
    public String toString() {
        return String.format(FMT,
                             sensor_device_id,
                             gpio_device_id,
                             gpio_info.index,
                             function);
    }

}
