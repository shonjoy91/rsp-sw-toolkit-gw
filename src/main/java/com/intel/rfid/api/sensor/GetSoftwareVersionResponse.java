/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonResponseOK;

public class GetSoftwareVersionResponse extends JsonResponseOK {

    public GetSoftwareVersionResponse() {
        result = new SensorSoftwareVersions();
    }

}
