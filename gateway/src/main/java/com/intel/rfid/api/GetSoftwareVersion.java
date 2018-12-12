/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class GetSoftwareVersion extends JsonRequest {

    public static final String METHOD_NAME = "get_sw_version";

    public GetSoftwareVersion() { method = METHOD_NAME; }

}
