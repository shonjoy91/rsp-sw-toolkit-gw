/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.common.JsonRequest;

public class GetDownstreamSummaryRequest extends JsonRequest {

    public static final String METHOD_NAME = "get_downstream_summary";

    public GetDownstreamSummaryRequest() {
        method = METHOD_NAME;
    }

}
