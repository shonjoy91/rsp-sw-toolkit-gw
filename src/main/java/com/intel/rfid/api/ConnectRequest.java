/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;


public class ConnectRequest extends JsonRequest {

    public static final String METHOD_NAME = "connect";

    public ConnectRequest() {
        method = METHOD_NAME;
    }

    public RSPInfo params = new RSPInfo();

}
