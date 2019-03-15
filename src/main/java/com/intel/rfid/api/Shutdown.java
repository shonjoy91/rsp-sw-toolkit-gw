/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

public class Shutdown extends JsonRequest {

    public static final String METHOD_NAME = "shutdown";

    public Shutdown() { method = METHOD_NAME; }

}
