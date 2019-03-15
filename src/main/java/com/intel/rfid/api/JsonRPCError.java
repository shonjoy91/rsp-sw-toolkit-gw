/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class JsonRPCError {
    public enum Type {
        NO_ERROR(0),
        WRONG_STATE(-32001),
        METHOD_NOT_FOUND(-32601),
        INVALID_PARAMETER(-32602),
        INTERNAL_ERROR(-32603),
        FUNCTION_NOT_SUPPORTED(-32604),
        PARSE_ERROR(-32700);

        public final int code;

        Type(int _code) { code = _code; }
    }

    public int code;
    public String message;
    public Object data;
}
