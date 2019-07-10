/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

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

public class UpstreamCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private UpstreamManager mgr;

    public UpstreamCommands(UpstreamManager _mgr) {
        mgr = _mgr;
    }

    public static final String CMD_ID = "upstream";

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
                                new StringsCompleter(SHOW),
                                new NullCompleter()
                        )
                )
        );

    }

    @Override
    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW);
        _out.indent(1, "shows information about Upstream");
        _out.blank();
    }

    @Override
    public void doAction(String action, ArgumentIterator _argIter, PrettyPrinter _out)
            throws SyntaxException, IOException {

        switch (action) {
            case SHOW:
                mgr.show(_out);
                break;
            default:
                usage(_out);
        }
    }

}
