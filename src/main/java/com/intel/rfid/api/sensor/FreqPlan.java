/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public enum FreqPlan {
    // UHF RFID Frequency Plans
    FCC((byte) 0x00),
    ETSI((byte) 0x01),
    Japan((byte) 0x02),
    Custom((byte) 0x0A),
    UNKNOWN((byte) 0xFF);

    public final byte val;

    FreqPlan(byte _val) { val = _val; }

}
