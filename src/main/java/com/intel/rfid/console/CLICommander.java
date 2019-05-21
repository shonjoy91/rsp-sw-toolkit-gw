/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.cluster.ClusterManagerCommands;
import com.intel.rfid.downstream.DownstreamCommands;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gpio.GPIOCommands;
import com.intel.rfid.gpio.GPIOManager;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.inventory.InventoryCommands;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.schedule.ScheduleManagerCommands;
import com.intel.rfid.sensor.SensorCommands;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.upstream.UpstreamCommands;
import com.intel.rfid.upstream.UpstreamManager;
import jline.console.completer.Completer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CLICommander {

    public interface Support {
        String getCommandId();

        void getCompleters(List<Completer> _comps);

        void doAction(String _action,
                      ArgumentIterator _argIter,
                      PrettyPrinter _out) throws GatewayException, IOException;

        void usage(PrettyPrinter _out);
    }

    public static final String LOAD_FILE = "load.file";
    public static final String UNLOAD = "unload";
    public static final String SET = "set";
    public static final String SHOW = "show";
    public static final String INFO = "info";


    protected Logger log = LoggerFactory.getLogger(getClass());
    private PrettyPrinter out;
    Map<String, Support> supporters = new HashMap<>();

    public CLICommander(PrettyPrinter _out) {

        out = _out;

        Support supporter;
        supporter = new LogCommands();
        supporters.put(supporter.getCommandId(), supporter);

        supporter = new SystemCommands();
        supporters.put(supporter.getCommandId(), supporter);

        supporter = new VersionCommands();
        supporters.put(supporter.getCommandId(), supporter);

    }

    public Support getSupporter(String commandId) {
        return supporters.get(commandId);
    }

    public void set(PrettyPrinter _out) {
        out = _out;
    }

    public void enable(ClusterManager _clusterMgr) {
        Support supporter = new ClusterManagerCommands(_clusterMgr);
        supporters.put(supporter.getCommandId(), supporter);
    }

    public void enable(SensorManager _sensorMgr) {
        Support supporter = new SensorCommands(_sensorMgr);
        supporters.put(supporter.getCommandId(), supporter);
    }

    public void enable(GPIOManager _gpioMgr, SensorManager _sensorMgr) {
        Support supporter = new GPIOCommands(_gpioMgr, _sensorMgr);
        supporters.put(supporter.getCommandId(), supporter);
    }

    public void enable(ScheduleManager _mgr) {
        Support supporter = new ScheduleManagerCommands(_mgr);
        supporters.put(supporter.getCommandId(), supporter);
    }

    public void enable(InventoryManager _mgr) {
        Support supporter = new InventoryCommands(_mgr);
        supporters.put(supporter.getCommandId(), supporter);
    }

    public void enable(UpstreamManager _mgr) {
        Support supporter = new UpstreamCommands(_mgr);
        supporters.put(supporter.getCommandId(), supporter);
    }

    public void enable(DownstreamManager _mgr) {
        Support supporter = new DownstreamCommands(_mgr);
        supporters.put(supporter.getCommandId(), supporter);
    }

    public void getCompleters(List<Completer> _comps) {
        for (Support support : supporters.values()) {
            support.getCompleters(_comps);
        }
    }


    public void execute(String _cmdLine) {
        if (_cmdLine == null || _cmdLine.length() == 0) { return; }

        out.divider();

        try {
            ArgumentIterator argIter = new ArgumentIterator(_cmdLine);
            String commandId = argIter.next();
            Support support = supporters.get(commandId);
            if (support != null) {
                try {
                    if (!argIter.hasNext()) {
                        support.usage(out);
                    } else {
                        String action = argIter.next();
                        support.doAction(action, argIter, out);
                    }

                } catch (SyntaxException se) {
                    out.error(se.getMessage());
                    support.usage(out);
                }

            } else {
                out.error("no command support found for " + commandId);
            }
        } catch (Exception e) {
            log.error("error executing {} ", _cmdLine, e);
            out.error("\"" + _cmdLine + "\" : " + e.getMessage());
        }
        out.divider();
    }

}
