/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.sensor;

public class TagDataEntry {

    public String epc;
    public String tid;
    public int ant_port;
    public int avg_rssi;
    public long first_read;
    public long last_read;
    public long seen_count;

    public TagDataEntry(TagRead _read) {
        epc = _read.epc;
        tid = _read.tid;
        ant_port = _read.antenna_id;
        avg_rssi = _read.rssi;
        first_read = System.currentTimeMillis();
        last_read = first_read;
        seen_count = 1;
    }

    @Override
    public String toString() {
        return "epc ='" + epc + '\'' +
                ", tid ='" + tid + '\'' +
                ", ant_port = " + ant_port +
                ", avg_rssi = " + avg_rssi +
                ", first_read = " + first_read +
                ", last_read = " + last_read +
                ", seen_count = " + seen_count;
    }

}
