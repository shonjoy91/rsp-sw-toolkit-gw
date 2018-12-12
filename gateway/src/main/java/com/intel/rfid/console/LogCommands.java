/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.intel.rfid.console.CLICommander.SET;
import static com.intel.rfid.console.CLICommander.SHOW;
import static com.intel.rfid.console.CLICommander.Support;

public class LogCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static final String CMD_ID = "log";

    @Override
    public String getCommandId() {
        return CMD_ID;
    }


    @Override
    public void getCompleters(List<Completer> _comps) {

        _comps.add(
            new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SHOW),
                new NullCompleter())
                  );

        _comps.add(
            new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET),
                new LoggerCompleter(),
                new LogLevelCompleter(),
                new NullCompleter())
                  );

    }

    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW);
        _out.indent(1, "Shows the current log level settings for all active loggers");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET + " <log id> <level>");
        _out.indent(1, "Sets the level of the logger associated with the log_id");
    }

    @Override
    public void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out)
        throws SyntaxException, IOException {

        switch (_action) {
            case SHOW:
                showLogLevels(_out);
                break;
            case SET:
                setLogLevel(_argIter, _out);
                break;
            default:
                usage(_out);
        }
    }

    public void setLogLevel(ArgumentIterator _argIter, PrettyPrinter _out) throws SyntaxException {
        String logId = _argIter.next();
        String levelId = _argIter.next();

        if (logId.isEmpty() ||
            logId.equals("*") ||
            Logger.ROOT_LOGGER_NAME.equals(logId)) {

            logId = Logger.ROOT_LOGGER_NAME;
        }

        Level logLevel = null;
        if (!"PARENT".equals(levelId)) {
            logLevel = Level.toLevel(levelId, Level.OFF);
            if (logLevel.equals(Level.OFF) &&
                !levelId.equalsIgnoreCase("OFF")) {
                _out.line("Error, invalid log level specified!");
                return;
            }
        }

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logId);
        logger.setLevel(logLevel);
        _out.line(logger.getName() + " : " + logger.getLevel());
    }


    public void showLogLevels(PrettyPrinter _out) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        _out.line(String.format("%-7s %-9s %s",
                                "level", "effective", "name"));
        for (ch.qos.logback.classic.Logger log : loggerContext.getLoggerList()) {
            _out.line(String.format("%-7s %-9s %s",
                                    log.getLevel(),
                                    log.getEffectiveLevel(),
                                    log.getName()));
        }
    }

}
