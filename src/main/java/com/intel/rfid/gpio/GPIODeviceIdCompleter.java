/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gpio;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import java.util.List;
import java.util.TreeSet;

public class GPIODeviceIdCompleter implements Completer {

    private GPIOManager gpioManager;

    public GPIODeviceIdCompleter(GPIOManager _gpioManager) {
        gpioManager = _gpioManager;
    }

    public int complete(String buffer, final int cursor, final List<CharSequence> candidates) {

        StringsCompleter sc = new StringsCompleter();
        TreeSet<String> ids = new TreeSet<>();
        ids.add(GPIOManager.ALL_DEVICES);
        for (String device : gpioManager.gpioDevices.keySet()) {
            ids.add(device);
        }
        sc.getStrings().addAll(ids);

        return sc.complete(buffer, cursor, candidates);
    }

}
