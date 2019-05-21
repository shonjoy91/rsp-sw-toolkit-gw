/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.common;

public class GetSoftwareVersionRequest extends JsonRequest {

    public static final String METHOD_NAME = "get_sw_version";

    public GetSoftwareVersionRequest() { method = METHOD_NAME; }

}
