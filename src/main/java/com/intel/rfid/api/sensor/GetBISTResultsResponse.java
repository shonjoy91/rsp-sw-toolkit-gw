/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.rfid.api.sensor;

import com.intel.rfid.api.JsonResponseOK;

public class GetBISTResultsResponse extends JsonResponseOK {

    public BISTResults result;

    public GetBISTResultsResponse(String _id, BISTResults _result) {

        super(_id, Boolean.TRUE);
        result = _result;

    }

}
