/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;


import com.intel.rfid.api.JsonRequest;

public class ConnectRequest extends JsonRequest {

    public static final String METHOD_NAME = "connect";

    public RSPInfo params = new RSPInfo();

    public ConnectRequest() { 
        method = METHOD_NAME;
    }

    public ConnectRequest(RSPInfo _info) {
        this();
        params = _info;
    }

}
