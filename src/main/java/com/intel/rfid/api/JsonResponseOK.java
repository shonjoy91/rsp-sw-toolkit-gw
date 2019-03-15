/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.List;

public class JsonResponseOK extends JsonResponse {

    public Object result;

    public JsonResponseOK() { }

    public JsonResponseOK(String _id, Object _result) {
        id = _id;
        result = _result;
    }

    // convenience class for some result messages
    public static class StatusResult {
        public static final String OK = "OK";
        public static final String WARN = "WARN";
        public String status = OK;
        public List<String> messages;

        public StatusResult(List<String> _msgs) {
            if (_msgs != null && _msgs.size() > 0) {
                status = JsonResponseOK.StatusResult.WARN;
                messages = _msgs;
            }
        }

        public StatusResult(String status, List<String> messages) {
            this.status = status;
            this.messages = messages;
        }
    }

    public static class KeyPairResult {
        public String public_key_hex = "";

        public KeyPairResult(String _hex) {
            public_key_hex = _hex;
        }
    }

}
