/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.downstream;

import com.intel.rfid.console.ArgumentIterator;
import com.intel.rfid.console.SyntaxException;
import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.intel.rfid.console.CLICommander.SHOW;
import static com.intel.rfid.console.CLICommander.Support;

public class DownstreamCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private DownstreamManager mgr;

    public DownstreamCommands(DownstreamManager _mgr) {
        mgr = _mgr;
    }

    public static final String CMD_ID = "gpio";

    public static final String ENABLE_JMDNS = "enable.jmdns";
    public static final String DISABLE_JMDNS = "disable.jmdns";


    @Override
    public String getCommandId() {
        return CMD_ID;
    }


    @Override
    public void getCompleters(List<Completer> _comps) {

        _comps.add(
                new AggregateCompleter(
                        new ArgumentCompleter(
                                new StringsCompleter(CMD_ID),
                                new StringsCompleter(SHOW, ENABLE_JMDNS, DISABLE_JMDNS),
                                new NullCompleter()
                        )
                )
        );

    }

    @Override
    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + DISABLE_JMDNS);
        _out.indent(1, "Disable the sending of JmDNS announcements");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + ENABLE_JMDNS);
        _out.indent(1, "Enable the sending of JmDNS announcements");
        _out.blank();
    }

    @Override
    public void doAction(String action, ArgumentIterator _argIter, PrettyPrinter _out)
            throws SyntaxException, IOException {

        switch (action) {
            case SHOW:
                mgr.show(_out);
                break;
            case ENABLE_JMDNS:
                mgr.startJmDNSService();
                mgr.show(_out);
                break;
            case DISABLE_JMDNS:
                mgr.stopJmDNSService();
                mgr.show(_out);
                break;
            default:
                usage(_out);
        }
    }

}
