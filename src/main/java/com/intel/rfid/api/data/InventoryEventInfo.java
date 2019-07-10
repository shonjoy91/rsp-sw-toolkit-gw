/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.data;

import java.util.ArrayList;
import java.util.List;

public class InventoryEventInfo {
    public long sent_on = System.currentTimeMillis();
    public String gateway_id;
    public List<InventoryEventItem> data = new ArrayList<>();

    @Override
    public String toString() {
        return "sent_on=" + sent_on +
                ", data.size=" + data.size();
    }
}
