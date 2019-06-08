/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class RemoveDeviceRequest extends JsonRequest {

    public static final String METHOD_NAME = "remove_device";

    public static class Params {
        public String device_id;
    }

    public Params params = new Params();

    public RemoveDeviceRequest() { method = METHOD_NAME; }
    
    public RemoveDeviceRequest(String _deviceId) {
        this();
        params.device_id = _deviceId;
    }

}
