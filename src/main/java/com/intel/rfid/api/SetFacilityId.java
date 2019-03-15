/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class SetFacilityId extends JsonRequest {

    public static final String METHOD_NAME = "set_facility_id";

    public String params;

    // keep default for Jackson mapper
    public SetFacilityId() { method = METHOD_NAME; }

    public SetFacilityId(String _facilityId) {
        this();
        params = _facilityId;
    }

}
