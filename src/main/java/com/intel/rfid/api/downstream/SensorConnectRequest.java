/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.data.SensorInfo;

public class SensorConnectRequest extends JsonRequest {

    public static final String METHOD_NAME = "connect";

    public SensorConnectRequest() {
        method = METHOD_NAME;
    }

    public SensorInfo params = new SensorInfo();

}
