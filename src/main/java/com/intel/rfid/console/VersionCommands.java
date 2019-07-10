/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import com.intel.rfid.gateway.Version;
import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.intel.rfid.console.CLICommander.INFO;
import static com.intel.rfid.console.CLICommander.Support;

public class VersionCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static final String CMD_ID = "version";

    @Override
    public String getCommandId() {
        return CMD_ID;
    }

    @Override
    public void getCompleters(List<Completer> _comps) {
        _comps.add(
                new ArgumentCompleter(
                        new StringsCompleter(CMD_ID),
                        new StringsCompleter(INFO),
                        new NullCompleter())
        );
    }

    @Override
    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + INFO);
        _out.indent(1, "Displays the software version information");
    }

    @Override
    public synchronized void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out) {

        switch (_action) {
            case INFO:
            default:
                _out.line(Version.asString());
                break;
        }
    }

}
