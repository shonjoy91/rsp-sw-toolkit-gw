/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.data.InventorySummary;
import com.intel.rfid.schedule.SchedulerSummary;
import com.intel.rfid.sensor.SensorStateSummary;

import java.util.ArrayList;
import java.util.List;

public class GatewayHeartbeatNotification extends JsonNotification {

    public static final String METHOD_NAME = "gateway_heartbeat";

    public GatewayHeartbeatNotification() {
        method = METHOD_NAME;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
    }

}
