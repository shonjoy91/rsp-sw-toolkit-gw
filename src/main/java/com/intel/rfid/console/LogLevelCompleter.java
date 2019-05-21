/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import ch.qos.logback.classic.Level;
import jline.console.completer.StringsCompleter;

public class LogLevelCompleter extends StringsCompleter {

    public LogLevelCompleter() {

        this.getStrings().add(Level.ALL.levelStr);
        this.getStrings().add(Level.TRACE.levelStr);
        this.getStrings().add(Level.INFO.levelStr);
        this.getStrings().add(Level.DEBUG.levelStr);
        this.getStrings().add(Level.WARN.levelStr);
        this.getStrings().add(Level.ERROR.levelStr);
        this.getStrings().add(Level.OFF.levelStr);

        this.getStrings().add("PARENT");
    }
}
