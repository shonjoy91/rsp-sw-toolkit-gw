/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public enum TargetState {
    A((byte) 0x00),
    B((byte) 0x01),
    // to prevent having to deal with nulls
    UNKNOWN((byte) 0xFF);

    private static final Map<Byte, TargetState> lut = new HashMap<>();

    static { for (TargetState v : TargetState.values()) { lut.put(v.val, v); } }

    public final byte val;

    TargetState(byte _val) { val = _val; }

    public void to(DataOutput _out) throws IOException {
        _out.writeByte(val);
    }

    public static TargetState from(DataInput _in) throws IOException {
        return from(_in.readByte());
    }

    public static TargetState from(byte _byte) {
        TargetState state = lut.get(_byte);
        if (state == null) {
            state = TargetState.UNKNOWN;
        }
        return state;
    }

    public static TargetState from(ByteBuffer _buf) {
        return from(_buf.get());
    }

}

