/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;


public class InventoryComplete extends JsonNotification {

    public static final String METHOD_NAME = "inventory_complete";

    public InventoryComplete() {
        method = METHOD_NAME;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
    }

}
