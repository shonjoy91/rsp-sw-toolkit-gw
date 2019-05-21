/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonResponseOK;

public class GPIODeviceConnectResponse extends JsonResponseOK {

    public GPIODeviceConnectResponse(String _id, long _currTime) {
        super(_id, Boolean.TRUE);
        result = new Result(_currTime);
    }

    public static class Result {
        public long sent_on;

        public Result(long _currTime) {
            sent_on = _currTime;
        }
    }

}
