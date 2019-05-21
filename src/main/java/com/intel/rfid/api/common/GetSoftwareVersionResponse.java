/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api.common;

public class GetSoftwareVersionResponse extends JsonResponseOK {

    public GetSoftwareVersionResponse(String _id, String _version) {
        super(_id, Boolean.TRUE);

        result = new Result(_version);
    }

    public static class Result {
        public String version;

        public Result(String _version) {

            version = _version;
        }
    }

}
