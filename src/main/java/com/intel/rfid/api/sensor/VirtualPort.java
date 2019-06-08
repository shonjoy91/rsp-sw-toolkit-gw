/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public class VirtualPort {

    // Maximum values
    public static final float MAX_POWER_LEVEL = 31.5f;
    public static final int MAX_VIRTUAL_PORTS = 8;

    // Antenna Configuration Parameters
    public int index = 0;
    public EnabledState state = EnabledState.Disabled;
    public float power_level = 30.5f;
    public int dwell_time = 2000;
    public int inv_cycles = 0;
    public int physical_port = 0;


    public VirtualPort() {

    }

    public int getPowerLevel10x() {
        return (int) (power_level * 10);
    }

    public boolean setPowerLevel10x(int _level) {
        float level = ((float) _level) / 10.0f;
        boolean success = false;
        if (level <= MAX_POWER_LEVEL) {
            power_level = level;
            success = true;
        }
        return success;
    }

}
