/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;


public class InventoryData extends JsonNotification {

    public static final String METHOD_NAME = "inventory_data";

    public InventoryData() {
        method = METHOD_NAME;
    }

    public EpcRead params = new EpcRead();

}
