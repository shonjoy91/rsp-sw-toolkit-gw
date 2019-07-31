/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.upstream;

import com.intel.rfid.api.JsonRequest;

public class MobilityProfileDeleteRequest extends JsonRequest {

    public static final String METHOD_NAME = "mobility_profile_delete";

    public class Params {
        public String mobility_profile_id;
    }

    public Params params = new Params();

    public MobilityProfileDeleteRequest() {
        method = METHOD_NAME;
    }

    public MobilityProfileDeleteRequest(String _behaviorId) {
        this();
        params.mobility_profile_id = _behaviorId;
    }
}
