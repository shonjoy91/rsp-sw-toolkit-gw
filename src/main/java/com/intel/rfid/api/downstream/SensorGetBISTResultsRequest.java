/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;

public class SensorGetBISTResultsRequest extends JsonRequest {

    public static final String METHOD_NAME = "get_bist_results";

    public SensorGetBISTResultsRequest() { method = METHOD_NAME; }

}
