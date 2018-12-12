/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import java.util.ArrayList;
import java.util.List;

public class SensorGroup {

    public List<SensorPlatform> sensors = new ArrayList<>();

    public SensorGroup() { }

    public SensorGroup(List<String> _sensorIds, SensorManager _sensorMgr) {
        for (String devId : _sensorIds) {
            sensors.add(_sensorMgr.establishRSP(devId));
        }
    }

}
