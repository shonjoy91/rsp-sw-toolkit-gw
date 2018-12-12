/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.exception;

@SuppressWarnings({"serial"})
public class NotConnectedException extends GatewayException {

    public NotConnectedException(String _connectionId) {
        super(_connectionId);
    }
}
