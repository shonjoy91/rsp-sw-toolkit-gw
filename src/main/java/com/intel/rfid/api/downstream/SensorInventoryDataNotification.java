/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.EpcRead;

public class SensorInventoryDataNotification extends JsonNotification {

    public static final String METHOD_NAME = "inventory_data";

    public SensorInventoryDataNotification() {
        method = METHOD_NAME;
    }

    public EpcRead params = new EpcRead();

}
