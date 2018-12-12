/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.ArrayList;
import java.util.List;

public class InventoryEvent extends JsonNotification {

    public static final String METHOD_NAME = "inventory_event";

    public InventoryEvent() {
        method = METHOD_NAME;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on = System.currentTimeMillis();
        public String gateway_id;
        public List<Item> data = new ArrayList<>();

        @Override
        public String toString() {
            return "sent_on=" + sent_on +
                   ", data.size=" + data.size();
        }
    }

    public static class Item {
        public String facility_id;
        public String epc_code;
        public String tid;
        public String epc_encode_format = "tbd";
        public String event_type;
        public long timestamp;
        public String location;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + params.toString();
    }

}
