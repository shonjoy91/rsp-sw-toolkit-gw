/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.inventory.TagReadSummary;
import com.intel.rfid.inventory.TagStateSummary;

public class InventorySummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "inventory_summary";

    public Params params = new Params();

    public InventorySummaryNotification() {
        method = METHOD_NAME;
    }

    public InventorySummaryNotification(TagStateSummary _tagStateSummary, TagReadSummary _tagReadSummary) {
        this();
        params.tag_state_summary.copyFrom(_tagStateSummary);
        params.tag_read_summary.copyFrom(_tagReadSummary);
    }
    
    public class Params {
        public TagStateSummary tag_state_summary = new TagStateSummary();
        public TagReadSummary tag_read_summary = new TagReadSummary();
    }

}
