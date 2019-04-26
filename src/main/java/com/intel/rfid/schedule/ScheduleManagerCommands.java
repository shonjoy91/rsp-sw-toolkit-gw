/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.intel.rfid.api.SchedulerSummary;
import com.intel.rfid.cluster.Cluster;
import com.intel.rfid.console.ArgumentIterator;
import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.intel.rfid.console.CLICommander.INFO;
import static com.intel.rfid.console.CLICommander.SHOW;
import static com.intel.rfid.console.CLICommander.Support;

public class ScheduleManagerCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private ScheduleManager scheduleMgr;

    public ScheduleManagerCommands(ScheduleManager _scheduleMgr) {
        scheduleMgr = _scheduleMgr;
    }

    public static final String CMD_ID = "scheduler";

    public static final String ACTIVATE_ALL_ON = "activate.all.on";
    public static final String ACTIVATE_ALL_SEQ = "activate.all.sequenced";
    public static final String ACTIVATE_FROM_CFG = "activate.from.config";
    public static final String DEACTIVATE = "deactivate";

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
                    new StringsCompleter(SHOW,
                                         ACTIVATE_ALL_ON,
                                         ACTIVATE_ALL_SEQ,
                                         ACTIVATE_FROM_CFG,
                                         DEACTIVATE),
                    new NullCompleter()
                )
            )
                  );

    }

    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + INFO + " <topic>");
        _out.indent(1, "Displays info about the topic");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW);
        _out.indent(1, "Displays info about current state of the schedule manager");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + ACTIVATE_ALL_ON);
        _out.indent(1, "Transitions to all sensors reading tags at the same time");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + ACTIVATE_ALL_SEQ);
        _out.indent(1, "Transitions to each sensor reading tags one at a time");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + ACTIVATE_FROM_CFG);
        _out.indent(1, "Transitions to running per the existing cluster configuration");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + DEACTIVATE);
        _out.indent(1, "Deactivates any scheduling activities and causes the sensors to stop reading");
        _out.blank();
    }

    @Override
    public void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out) {

        switch (_action) {

            case ACTIVATE_ALL_ON:
            case ACTIVATE_ALL_SEQ:
            case ACTIVATE_FROM_CFG:
                doActivate(_action, _out);
                break;
            case DEACTIVATE:
                doDeactivate(_out);
                break;
            case SHOW:
                doShow(_out);
                break;
            default:
                usage(_out);
        }
    }

    private void doActivate(String _action, PrettyPrinter _out) {

        switch (_action) {
            case ACTIVATE_FROM_CFG:
                scheduleMgr.activate(ScheduleManager.RunState.FROM_CONFIG);
                break;
            case ACTIVATE_ALL_SEQ:
                scheduleMgr.activate(ScheduleManager.RunState.ALL_SEQUENCED);
                break;
            case ACTIVATE_ALL_ON:
                scheduleMgr.activate(ScheduleManager.RunState.ALL_ON);
                break;
            default:
                _out.line("unhandled action " + _action);
                return;
        }
        _out.line("completed");
    }

    private void doDeactivate(PrettyPrinter _out) {
        scheduleMgr.deactivate();
        _out.line("completed");
    }
    
    private void doShow(PrettyPrinter _out) {

        SchedulerSummary summary = scheduleMgr.getSummary();

        _out.line("runState: " + summary.run_state);
        _out.divider();
        _out.line("clusters:");
        for (Cluster cluster : summary.clusters) {
            _out.line("      id: " + cluster.id);
            _out.line("behavior: " + cluster.behavior_id);
            for (List<String> sensors : cluster.sensor_groups) {
                _out.chunk("sensors: [");
                for (String sensorId : sensors) {
                    _out.chunk(sensorId + " ");
                }
                _out.endln("]");
            }
            _out.endln();
            _out.divider();
        }
        _out.blank();

    }


}
