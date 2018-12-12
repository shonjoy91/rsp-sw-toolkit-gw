/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

public class RfUtils {
    public static double rssiToMilliwatts(double _rssi) {
        return Math.pow(10, _rssi / 10.0);
    }

    public static double milliwattsToRssi(double _mw) {
        return Math.log10(_mw) * 10.0;
    }
}
