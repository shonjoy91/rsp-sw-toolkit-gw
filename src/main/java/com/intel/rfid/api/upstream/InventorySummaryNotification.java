/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.InventorySummary;

public class InventorySummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "inventory_summary";

    public InventorySummary params;

    public InventorySummaryNotification() {
        method = METHOD_NAME;
    }

    public InventorySummaryNotification(InventorySummary _inventorySummary) {
        this();
        params = _inventorySummary;
    }
}
