/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;


import java.util.ArrayList;
import java.util.List;

public class RSPInventoryEvent extends JsonNotification {

    public static final String METHOD_NAME = "inventory_event";

    public RSPInventoryEvent() {
        method = METHOD_NAME;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
        public boolean motion_detected = false;
        public List<Item> data = new ArrayList<>();

        @Override
        public String toString() {
            return "Params{" + "sent_on=" + sent_on +
                   ", device_id='" + device_id + '\'' +
                   ", facility_id='" + facility_id + '\'' +
                   ", data=" + data +
                   '}';
        }
    }

    public static class Item {
        public String epc;
        public String tid;
        public String event_type;
        public int antenna_id;
        public long event_date;
        public TagDirection relative_motion;
        public int distance;
    }

    @Override
    public String toString() {
        return "{" + "sent_on=" + params.sent_on +
               ", device_id='" + params.device_id + '\'' +
               ", facility_id='" + params.facility_id + '\'' +
               ", data.size=" + params.data.size() +
               '}';
    }

}
