/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

public class GPIOInputEvent {

    public final GPIOMapping mapping;
    public final GPIOState state;


    public GPIOInputEvent(GPIOMapping _mapping, GPIOState _state) {
        mapping = _mapping;
        state = _state;
    }

}
