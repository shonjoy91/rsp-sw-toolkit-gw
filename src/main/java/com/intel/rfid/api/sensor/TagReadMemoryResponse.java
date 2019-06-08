/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonResponseOK;

public class TagReadMemoryResponse extends JsonResponseOK {

    public TagReadMemoryResponse(String _id, String _data) {
        super(_id, new Result(_data));
    }

    public static class Result {
        public long sent_on = 0;
        public String data = "";

        public Result() {}

        public Result(String _data) {
            sent_on = System.currentTimeMillis();
            data = _data;
        }
    }

    public void setData(String _data) {
        result = new Result(_data);
    }
}
