/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class JsonResponseOK extends JsonResponse {

    public Object result;

    public JsonResponseOK() { }

    public JsonResponseOK(String _id, Object _result) {
        id = _id;
        result = _result;
    }
}
