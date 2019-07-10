/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.gpio;

public class GPIOInfo {

    public int index;
    public String name;
    public GPIO.State state;
    public GPIO.Direction direction;

    private static final String FMT = "%-12s %-12s %-12s %-12s";
    public static final String HDR = String
            .format(FMT, "index", "name", "state", "direction");

    @Override
    public String toString() {
        return String.format(FMT,
                             index,
                             name,
                             state,
                             direction);
    }

}
