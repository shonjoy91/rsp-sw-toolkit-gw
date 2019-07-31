/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.exception;

@SuppressWarnings({"serial"})
public class FailedException extends RspControllerException {

    public FailedException(String _msg) {
        super(_msg);
    }
}
