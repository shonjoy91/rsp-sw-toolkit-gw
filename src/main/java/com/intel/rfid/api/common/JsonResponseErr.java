/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.common;

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

    // write this out explicitly to avoid having to catch mapping exceptions
    // when serializing.
    // public String toJson() {
    //     StringBuffer sb = new StringBuffer();
    //     sb.append("{");
    //     sb.append("\"id\": \"").append(id).append("\",");
    //     sb.append("\"error\": {");
    //    
    //     sb.append("\"code\": ").append(error.code).append(",");
    //     sb.append("\"message\": \"").append(error.message).append("\",");
    //     sb.append("\"data\": \"");
    //     if(error.data != null) {
    //         sb.append(error.data.toString());
    //     }
    //     sb.append("\"");
    //     sb.append("}");
    //     sb.append("}");
    //    
    //     return sb.toString();
    // }
}
