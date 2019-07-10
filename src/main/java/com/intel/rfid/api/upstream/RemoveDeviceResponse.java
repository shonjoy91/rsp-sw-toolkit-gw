/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonResponseOK;

public class RemoveDeviceResponse extends JsonResponseOK {

    public static class Result {
        public String device_id;

        public Result(String _deviceId) {
            device_id = _deviceId;
        }
    }

    public RemoveDeviceResponse(String _id, String _deviceId) {
        super(_id, Boolean.TRUE);
        result = new Result(_deviceId);
    }

}
