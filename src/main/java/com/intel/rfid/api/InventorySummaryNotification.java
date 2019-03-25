/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

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
