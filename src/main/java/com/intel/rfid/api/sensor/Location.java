/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public class Location {
    public double latitude;
    public double longitude;
    public double altitude;

    public void copyFrom(Location _loc) {
        latitude = _loc.latitude;
        longitude = _loc.longitude;
        altitude = _loc.altitude;
    }
}
