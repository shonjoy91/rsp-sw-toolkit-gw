/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonNotification;
import com.intel.rfid.api.data.SensorBasicInfo;

import java.util.ArrayList;
import java.util.List;

public class SensorBasicInfoNotification extends JsonNotification {

    public static final String METHOD_NAME = "sensor_basic_info";

    public List<SensorBasicInfo> params = new ArrayList<>();

    public SensorBasicInfoNotification() {
        method = METHOD_NAME;
    }

    public SensorBasicInfoNotification(List<SensorBasicInfo> _list) {
        this();
        params.addAll(_list);
    }

}
