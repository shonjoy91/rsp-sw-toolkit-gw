/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.gpio;

public class GPIOInputEvent {

    public final GPIOMapping mapping;
    public final GPIO.State state;


    public GPIOInputEvent(GPIOMapping _mapping, GPIO.State _state) {
        mapping = _mapping;
        state = _state;
    }

}
