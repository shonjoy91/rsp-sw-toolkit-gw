/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.sensor;

public enum MemoryBank {
    // Platform Configuration
    RESERVED((byte) 0x00),
    EPC((byte) 0x01),
    TID((byte) 0x02),
    USER((byte) 0x03),
    // to prevent having to deal with nulls
    UNKNOWN((byte) 0xFF);

    public final byte val;

    MemoryBank(byte _val) { val = _val; }
}
