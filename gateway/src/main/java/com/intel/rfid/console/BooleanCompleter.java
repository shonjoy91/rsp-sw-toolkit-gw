/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import jline.console.completer.StringsCompleter;

public class BooleanCompleter extends StringsCompleter {

    public static String asOptions() {
        return " <" + Boolean.TRUE.toString() + "|" + Boolean.FALSE.toString() + ">";
    }

    public BooleanCompleter() {
        this.getStrings().add(Boolean.TRUE.toString());
        this.getStrings().add(Boolean.FALSE.toString());
    }
}
