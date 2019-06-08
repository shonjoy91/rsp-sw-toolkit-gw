/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonNotification;

public class InventoryCompleteNotification extends JsonNotification {

    public static final String METHOD_NAME = "inventory_complete";

    public InventoryCompleteNotification() {
        method = METHOD_NAME;
    }

    public InventoryCompleteNotification(String _deviceId, String _facilityId) {
        this();
        params.sent_on = System.currentTimeMillis();
        params.device_id = _deviceId;
        params.facility_id = _facilityId;
    }

    public Params params = new Params();

    public static class Params {
        public long sent_on;
        public String device_id;
        public String facility_id;
    }

}
