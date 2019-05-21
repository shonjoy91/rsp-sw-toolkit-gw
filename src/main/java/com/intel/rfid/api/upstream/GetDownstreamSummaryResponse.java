/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonResponseOK;
import com.intel.rfid.api.data.MQTTSummary;

public class GetDownstreamSummaryResponse extends JsonResponseOK {

    public GetDownstreamSummaryResponse(String _id, MQTTSummary _summary) {
        super(_id, Boolean.TRUE);

        result = new Result(_summary);
    }

    public static class Result {
        public MQTTSummary summary;

        public Result(MQTTSummary _summary) {

            summary = _summary;
        }
    }

}
