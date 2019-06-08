/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public enum SessionFlag {
    S0((byte) 0x00),
    S1((byte) 0x01),
    S2((byte) 0x02),
    S3((byte) 0x03),
    SL((byte) 0x04),
    // to prevent having to deal with nulls
    UNKNOWN((byte) 0xFF);

    public final byte val;

    SessionFlag(byte _val) { val = _val; }

}

