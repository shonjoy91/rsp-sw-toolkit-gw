/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SensorGroup {

    private final List<SensorPlatform> sensors = new ArrayList<>();

    public synchronized Collection<SensorPlatform> getSensors() {
        return new ArrayList<>(sensors);
    }

    public synchronized void setSensors(Collection<SensorPlatform> _sensors) {
        sensors.addAll(_sensors);
    }

    public synchronized void add(SensorPlatform _sensor) {
        sensors.add(_sensor);
    }

}
