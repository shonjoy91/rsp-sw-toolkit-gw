/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.InventoryEventInfo;

public class InventoryEventNotification extends JsonNotification {

    public static final String METHOD_NAME = "inventory_event";

    public InventoryEventInfo params;

    public InventoryEventNotification() {
        method = METHOD_NAME;
    }

    public InventoryEventNotification(InventoryEventInfo _info) {
        this();
        params = _info;
    }
}
