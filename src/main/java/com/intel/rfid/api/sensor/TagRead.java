/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

public class TagRead {

    public String epc;
    public String tid;
    public int antenna_id = 0;
    public long last_read_on = 0;
    public int rssi = 0;
    public int phase = 0;
    public int frequency = 0;

    @Override
    public String toString() {
        return "epc ='" + epc + '\'' +
                ", tid ='" + tid + '\'' +
                ", antenna_id =" + antenna_id +
                ", last_read_on =" + last_read_on +
                ", rssi =" + rssi +
                ", phase =" + phase +
                ", frequency =" + frequency;
    }
}
