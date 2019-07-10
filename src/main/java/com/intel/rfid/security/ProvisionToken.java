/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.security;

public class ProvisionToken {

    public static final int NEVER_EXPIRES = -1;
    public String username = "";
    public String token = "";
    public long generatedTimestamp = 0;
    public long expirationTimestamp = 0;

    public ProvisionToken() {
        // for JSON de-serialization
    }

    public ProvisionToken(String _username, String _token) {
        username = _username;
        token = _token;
        generatedTimestamp = 0;
        expirationTimestamp = NEVER_EXPIRES;
    }

    @Override
    public String toString() {
        return "ProvisionToken{" +
                "username='" + username + '\'' +
                ", token='" + token + '\'' +
                ", generatedTimestamp=" + generatedTimestamp +
                ", expirationTimestamp=" + expirationTimestamp +
                '}';
    }
}
