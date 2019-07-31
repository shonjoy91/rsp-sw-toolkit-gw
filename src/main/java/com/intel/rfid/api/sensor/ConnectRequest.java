/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;


import com.intel.rfid.api.JsonRequest;

public class ConnectRequest extends JsonRequest {

    public static final String METHOD_NAME = "connect";

    public RspInfo params = new RspInfo();

    public ConnectRequest() {
        method = METHOD_NAME;
    }

    public ConnectRequest(RspInfo _info) {
        this();
        params = _info;
    }

}
