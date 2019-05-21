/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.downstream;

import com.intel.rfid.api.common.JsonRequest;

public class SensorSetFacilityIdRequest extends JsonRequest {

    public static final String METHOD_NAME = "set_facility_id";

    public String params;

    // keep default for Jackson mapper
    public SensorSetFacilityIdRequest() { method = METHOD_NAME; }

    public SensorSetFacilityIdRequest(String _facilityId) {
        this();
        params = _facilityId;
    }

}
