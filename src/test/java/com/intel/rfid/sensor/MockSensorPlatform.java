/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

public class MockSensorPlatform extends SensorPlatform {

    public MockSensorPlatform(String _deviceId, MockSensorManager _sensorMgr) {
        super(_deviceId, _sensorMgr);
    }

    public String asLocation() {
        return getDeviceId() + "-0";
    }

}
