/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonRequest;

public class SetFacilityIdRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_facility_id";

    public String params = "UNKNOWN";

    // keep default for Jackson mapper
    public SetFacilityIdRequest() { method = METHOD_NAME; }

    public SetFacilityIdRequest(String _facilityId) {
        this();
        params = _facilityId;
    }

}
