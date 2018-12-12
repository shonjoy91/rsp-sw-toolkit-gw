/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class JsonResponseErr extends JsonResponse {

    public JsonRPCError error = new JsonRPCError();

    public JsonResponseErr() { }

    public JsonResponseErr(String _id, int _errCode, String _errMsg, Object _errData) {
        id = _id;
        error.code = _errCode;
        error.message = _errMsg;
        error.data = _errData;
    }

    public JsonResponseErr(String _id, JsonRPCError.Type _errType, Object _errData) {
        this(_id, _errType.code, _errType.toString(), _errData);
    }

}
