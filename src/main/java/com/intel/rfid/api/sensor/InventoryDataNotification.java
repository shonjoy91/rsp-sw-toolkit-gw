/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;


import com.intel.rfid.api.JsonNotification;

public class InventoryDataNotification extends JsonNotification {

    public static final String METHOD_NAME = "inventory_data";

    public InventoryDataNotification() {
        method = METHOD_NAME;
        params.sent_on = System.currentTimeMillis();
    }

    public PeriodicInventoryData params = new PeriodicInventoryData();

}
