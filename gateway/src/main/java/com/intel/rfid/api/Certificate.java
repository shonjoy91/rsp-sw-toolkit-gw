/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class Certificate {
    public String one_line_pem;

    public Certificate() {
        // for JSON deserialization
    }

    public Certificate(String _one_line_pem) {
        one_line_pem = _one_line_pem;
    }
}
