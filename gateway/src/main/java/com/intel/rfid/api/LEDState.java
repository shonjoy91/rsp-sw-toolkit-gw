/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum LEDState {
    Normal, Beacon, Test, Disabled;


    public static Collection<String> getNames() {
        List<String> list = new ArrayList<>();
        for (LEDState value : values()) {
            list.add(value.name());
        }
        return list;
    }

}
