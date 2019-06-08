/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.sensor;

public enum SelectAction {
    // Specifies the action that will be applied
    // to the tag population based on the select
    // mask, the meaning is "MATCH_NOMATCH".
    A_B((byte) 0x00),
    A_NONE((byte) 0x01),
    NONE_B((byte) 0x02),
    INVERT_NONE((byte) 0x03),
    B_A((byte) 0x04),
    B_NONE((byte) 0x05),
    NONE_A((byte) 0x06),
    NONE_INVERT((byte) 0x07),
    UNKNOWN((byte) 0xFF);

    public final byte val;

    SelectAction(byte _val) { val = _val; }

}
