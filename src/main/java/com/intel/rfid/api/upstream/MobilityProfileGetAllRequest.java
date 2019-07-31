/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class MobilityProfileGetAllRequest extends JsonRequest {

    public static final String METHOD_NAME = "mobility_profile_get_all";

    public MobilityProfileGetAllRequest() {
        method = METHOD_NAME;
    }

}
