/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public enum SelectedState {
    Any((byte) 0x00),
    Deasserted((byte) 0x02),
    Asserted((byte) 0x03),
    // to prevent having to deal with nulls
    UNKNOWN((byte) 0xFF);

    public final byte val;

    SelectedState(byte _val) { val = _val; }

}

