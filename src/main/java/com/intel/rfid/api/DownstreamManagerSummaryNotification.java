/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class DownstreamManagerSummaryNotification extends JsonNotification {

    public static final String METHOD_NAME = "downstream_manager_summary";

    public MQTTSummary params = new MQTTSummary();

    public DownstreamManagerSummaryNotification() {
        method = METHOD_NAME;
    }

    public DownstreamManagerSummaryNotification(MQTTSummary _summary) {
        this();
        params.copyFrom(_summary);
    }

}
