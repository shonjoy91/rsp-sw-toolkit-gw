/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jline.console.completer.StringsCompleter;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LoggerCompleter extends StringsCompleter {

    public LoggerCompleter() {
        // Get all possible logger names for completion
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<String> loggerNames = new ArrayList<>();
        loggerNames.add("*");
        for (Logger log : loggerContext.getLoggerList()) {
            loggerNames.add(log.getName());
        }

        this.getStrings().addAll(loggerNames);
    }
}
