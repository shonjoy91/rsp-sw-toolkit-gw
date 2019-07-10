/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.helpers.SysStats;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.intel.rfid.console.CLICommander.INFO;
import static com.intel.rfid.console.CLICommander.Support;

public class SystemCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static final String CMD_ID = "system";

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
        _out.indent(1, "Displays processor and memory information");
    }

    @Override
    public synchronized void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out) {


        switch (_action) {
            case INFO:
            default:
                SysStats.MemoryInfo memInfo = SysStats.getMemoryInfo();
                SysStats.CPUInfo cpuInfo = SysStats.getCPUInfo();
                _out.indent(0, "Java Heap Used........ " + memInfo.strHeapUsed);
                _out.indent(0, "Java Heap Free........ " + memInfo.strHeapFree);
                _out.indent(0, "Java Heap Total....... " + memInfo.strHeapTotal);
                _out.indent(0, "Java Heap Max......... " + memInfo.strHeapMax);
                _out.indent(0, "System Total Memory... " + memInfo.strSystemPhysical);
                _out.indent(0, "Process Threads....... " + cpuInfo.strProcessThreads);
                _out.indent(0, "Process Load.......... " + cpuInfo.strProcessLoad);
                _out.indent(0, "System Load........... " + cpuInfo.strSystemLoad);
                break;
        }
    }
}
