/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.gpio;

import com.intel.rfid.api.JsonResponseOK;

public class GPIOConnectResponse extends JsonResponseOK {

    public GPIOConnectResponse(String _id, long _currTime) {
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
