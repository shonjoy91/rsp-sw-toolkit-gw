/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import com.intel.rfid.exception.RSPControllerException;

@SuppressWarnings({"serial"})
public class SyntaxException extends RSPControllerException {

    public SyntaxException(String _s) { super(_s); }

}
