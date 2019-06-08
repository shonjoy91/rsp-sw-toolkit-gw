/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class GetBISTResultsRequest extends JsonRequest {

    public static final String METHOD_NAME = "get_bist_results";

    public GetBISTResultsRequest() { method = METHOD_NAME; }

}
