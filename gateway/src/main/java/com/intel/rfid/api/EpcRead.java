/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.ArrayList;

public class EpcRead {

    public long sent_on = 0;
    public int period = 500;
    public String device_id = null;
    public String facility_id = null;
    public boolean motion_detected = false;
    public ArrayList<Data> data = new ArrayList<>();

    public static class Data {

        public String epc;
        public String tid;
        public int antenna_id = 0;
        public long last_read_on = 0;
        public int rssi = 0;
        public int phase = 0;
        public int frequency = 0;

    }
}
