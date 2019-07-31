/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.cluster;

import com.intel.rfid.console.ArgumentIterator;
import com.intel.rfid.exception.RspControllerException;
import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.intel.rfid.console.CLICommander.LOAD_FILE;
import static com.intel.rfid.console.CLICommander.SHOW;
import static com.intel.rfid.console.CLICommander.Support;

public class ClusterManagerCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private ClusterManager clusterMgr;

    public ClusterManagerCommands(ClusterManager _clusterMgr) {
        clusterMgr = _clusterMgr;
    }

    public static final String CMD_ID = "clusters";

    public static final String SHOW_TOKENS = "show.tokens";
    public static final String EXPORT_TOKENS = "export.tokens";

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
                                new StringsCompleter(SHOW, SHOW_TOKENS, EXPORT_TOKENS),
                                new NullCompleter()
                        ),
                        new ArgumentCompleter(
                                new StringsCompleter(CMD_ID),
                                new StringsCompleter(LOAD_FILE),
                                new FileNameCompleter(),
                                new NullCompleter()
                        )
                )
        );

    }

    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW);
        _out.indent(1, "Displays info about current state of the schedule manager");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + LOAD_FILE);
        _out.indent(1, "Load a cluster configuration from the specified JSON file path");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW_TOKENS);
        _out.indent(1, "Displays the tokens included in the configuration");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + EXPORT_TOKENS);
        _out.indent(1, "Exports the tokens included in the configuration as separate json files");
        _out.blank();
    }

    @Override
    public void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out)
            throws IOException, RspControllerException {

        switch (_action) {

            case LOAD_FILE:
                doLoadFile(_argIter, _out);
                break;
            case SHOW:
                clusterMgr.show(_out);
                break;
            case SHOW_TOKENS:
                clusterMgr.showTokens(_out);
                break;
            case EXPORT_TOKENS:
                clusterMgr.exportTokens(_out);
                break;
            default:
                usage(_out);
        }
    }

    private void doLoadFile(ArgumentIterator _argIter, PrettyPrinter _out)
            throws IOException, RspControllerException {


        Path p = Paths.get(new File(_argIter.next()).getCanonicalPath());

        clusterMgr.loadConfig(p);
        _out.line("completed");
    }

}
