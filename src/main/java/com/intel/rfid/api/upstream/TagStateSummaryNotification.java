/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.inventory.TagStateSummary;

public class TagStateSummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "tag_state_summary";

    public TagStateSummary params = new TagStateSummary();

    public TagStateSummaryNotification() {
        method = METHOD_NAME;
    }

    public TagStateSummaryNotification(TagStateSummary _summary) {
        this();
        params.copyFrom(_summary);
    }

}
