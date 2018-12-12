/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import java.util.List;
import java.util.TreeSet;

public class SensorIdCompleter implements Completer {

    private SensorManager sensorMgr;

    public SensorIdCompleter(SensorManager _sensorMgr) {
        sensorMgr = _sensorMgr;
    }

    public int complete(String buffer, final int cursor, final List<CharSequence> candidates) {

        StringsCompleter sc = new StringsCompleter();
        TreeSet<String> ids = new TreeSet<>();
        ids.add(SensorManager.ALL_SENSORS);
        for (SensorPlatform rsp : sensorMgr.getRSPsCopy()) {
            ids.add(rsp.getDeviceId());
        }
        sc.getStrings().addAll(ids);

        return sc.complete(buffer, cursor, candidates);
    }

}
