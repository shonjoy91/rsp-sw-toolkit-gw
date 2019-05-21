/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.MQTTSummary;

public class UpstreamManagerSummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "upstream_manager_summary";

    public MQTTSummary params = new MQTTSummary();

    public UpstreamManagerSummaryNotification() {
        method = METHOD_NAME;
    }

    public UpstreamManagerSummaryNotification(MQTTSummary _summary) {
        this();
        params.copyFrom(_summary);
    }

}
