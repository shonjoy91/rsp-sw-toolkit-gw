/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

public class SensorStateSummary {
    public int reading;
    public int connected;
    public int disconnected;

    public void copyFrom(SensorStateSummary _other) {
        reading = _other.reading;
        connected = _other.connected;
        disconnected = _other.disconnected;
    }
}
