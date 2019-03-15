/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.exception;

@SuppressWarnings({"serial"})
public class ConfigException extends GatewayException {

    public ConfigException(String _msg) {
        super(_msg);
    }

    public ConfigException(String _msg, Throwable t) {
        super(_msg, t);
    }
}
