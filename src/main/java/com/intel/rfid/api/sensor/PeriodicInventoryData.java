/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import java.util.ArrayList;

public class PeriodicInventoryData {
    public long sent_on = 0;
    public int period = 500;
    public String device_id = "";
    public Location location = new Location();
    public String facility_id = "";
    public boolean motion_detected = false;
    public ArrayList<TagRead> data = new ArrayList<>();
}
