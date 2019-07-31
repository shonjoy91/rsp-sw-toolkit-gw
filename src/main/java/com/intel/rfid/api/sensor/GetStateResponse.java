/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonResponseOK;

public class GetStateResponse extends JsonResponseOK {

    public GetStateResponse(String _id, RspInfo _info) {

        super(_id, Boolean.TRUE);

        result = _info;
    }

    public RspInfo result = new RspInfo();

}
