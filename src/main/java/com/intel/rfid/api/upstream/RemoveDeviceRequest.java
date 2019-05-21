/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;

import java.util.List;

public class RemoveDeviceRequest extends JsonRequest {

    public static final String METHOD_NAME = "remove_device";

    public Params params = new Params();

    public RemoveDeviceRequest() { method = METHOD_NAME; }

    public RemoveDeviceRequest(List<String> _devices) {
        this();

        params.devices = _devices;
    }

    public static class Params {
        public List<String> devices;
    }

}
