/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public enum ChannelType {
    SingleIndex((byte) 0x00),
    SingleFrequency((byte) 0x01),
    HoppingByPlan((byte) 0x02),
    // to prevent having to deal with nulls
    UNKNOWN((byte) 0xFF);

    public final byte val;

    ChannelType(byte _val) { val = _val; }

}

