/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.intel.rfid.tag.TagState;
import com.intel.rfid.tag.TagDirection;
import com.intel.rfid.tag.Tag;

public class PreviousTag {

    public final String location;
    public final String deviceLocation;
    public final String facility;
    public final long lastRead;
    public final long lastDeparted;
    public final long lastArrived;
    public final TagState state;
    public final TagDirection direction;

    public PreviousTag(Tag _tag) {
        location = _tag.getLocation();
        deviceLocation = _tag.getDeviceLocation();
        facility = _tag.getFacility();
        lastRead = _tag.getLastRead();
        lastDeparted = _tag.getLastDeparted();
        lastArrived = _tag.getLastArrived();
        state = _tag.getState();
        direction = _tag.getDirection();
    }
}
