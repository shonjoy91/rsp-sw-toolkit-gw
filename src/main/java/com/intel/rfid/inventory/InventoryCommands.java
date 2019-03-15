/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.intel.rfid.console.ArgumentIterator;
import com.intel.rfid.console.SyntaxException;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.exception.GatewayException;
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
import java.util.Map;

import static com.intel.rfid.console.CLICommander.INFO;
import static com.intel.rfid.console.CLICommander.SET;
import static com.intel.rfid.console.CLICommander.SHOW;
import static com.intel.rfid.console.CLICommander.Support;
import static com.intel.rfid.console.CLICommander.UNLOAD;

public class InventoryCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private InventoryManager mgr;

    public InventoryCommands(InventoryManager _mgr) {
        mgr = _mgr;
    }

    public static final String CMD_ID = "inventory";

    public static final String SNAPSHOT = "snapshot";
    public static final String SUMMARY = "summary";
    public static final String DETAIL = "detail";
    public static final String EXITING = "exiting";
    public static final String MOBILITY = "mobility.profile";
    public static final String WAYPOINTS = "waypoints";
    public static final String STATS = "stats";
    public static final String SET_REGEX = "set.regex";
    public static final String CHECK_REGEX = "check.regex";
    public static final String START_RECORDING = "start.recording";
    public static final String STOP_RECORDING = "stop.recording";

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
                    new StringsCompleter(SUMMARY, DETAIL, EXITING,
                                         UNLOAD, SNAPSHOT),
                    new NullCompleter()
                )
            )
                  );

        _comps.add(
            new AggregateCompleter(
                new ArgumentCompleter(
                    new StringsCompleter(CMD_ID),
                    new StringsCompleter(WAYPOINTS),
                    new StringsCompleter(SHOW, SNAPSHOT),
                    new NullCompleter()
                )
            )
                  );

        _comps.add(
            new AggregateCompleter(
                new ArgumentCompleter(
                    new StringsCompleter(CMD_ID),
                    new StringsCompleter(MOBILITY),
                    new StringsCompleter(SHOW, INFO),
                    new NullCompleter()
                )
            )
                  );

        _comps.add(
            new AggregateCompleter(
                new ArgumentCompleter(
                    new StringsCompleter(CMD_ID),
                    new StringsCompleter(MOBILITY),
                    new StringsCompleter(SET),
                    new MobilityProfileConfig(),
                    new NullCompleter()
                )
            )
                  );

        _comps.add(
            new AggregateCompleter(
                new ArgumentCompleter(
                    new StringsCompleter(CMD_ID),
                    new StringsCompleter(STATS),
                    new StringsCompleter(SHOW, SNAPSHOT, SET_REGEX, CHECK_REGEX,
                                         START_RECORDING, STOP_RECORDING),
                    new NullCompleter()
                )
            )
                  );

    }

    @Override
    public void usage(PrettyPrinter _out) {
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SUMMARY);
        _out.indent(1, "Shows the tags in summarized groupings");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + DETAIL + " [ regex ]");
        _out.indent(1, "Shows details of each tag in the database");
        _out.indent(1, "Optional regex will match against the EPC");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + EXITING);
        _out.indent(1, "Shows the tags in the exiting table, potential departeds");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SNAPSHOT);
        _out.indent(1, "Writes a snapshot of the current inventory to file");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + UNLOAD);
        _out.indent(1, "!! WARNING !! WARNING !! WARNING !! WARNING !! WARNING !! WARNING !! WARNING !! ");
        _out.indent(1, "Clears all tag reads from the gateway including the cached file. No recovery available");
        _out.indent(1, "This will cause new arrival events to be generated for all tags");
        _out.blank();
        usageStats(_out);
        _out.blank();
        usageMobility(_out);
        _out.blank();
        usageWaypoints(_out);
    }

    public void usageStats(PrettyPrinter _out) {
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + STATS + " " + SHOW + " [ regex ]");
        _out.indent(1, "Shows read statistics of each tag in the database");
        _out.indent(1, "Optional regex will match against the EPC");

        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + STATS + " " + SNAPSHOT + " [ regex ]");
        _out.indent(1, "Writes a snapshot of tag read statistics to file");
        _out.indent(1, "Optional regex will match against the EPC");

        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + STATS + " " + SET_REGEX);
        _out.indent(1, "Sets a regular expression used for filtering stats during recording");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + STATS + " " + CHECK_REGEX);
        _out.indent(1, "Displays the stats the current regex will return and also shows the regex pattern");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + STATS + " " + START_RECORDING);
        _out.indent(1, "Starts taking stats sanpshots at a regular interval (5 seconds)");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + STATS + " " + STOP_RECORDING);
    }

    public void usageMobility(PrettyPrinter _out) {
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + MOBILITY + " " + SHOW);
        _out.indent(1, "Shows the mobility profile settings used in the moved event algorithm");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + MOBILITY + " " + SET);
        _out.indent(1, "Sets the mobility profile used in the moved event algorithm");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + MOBILITY + " " + INFO);
        _out.indent(1, "Provides a description of the " + MOBILITY + " parameters");
    }

    public void usageWaypoints(PrettyPrinter _out) {
        _out.indent(0, "> " + CMD_ID + " " + WAYPOINTS + " " + SHOW);
        _out.indent(1, "Shows the tags waypoint history");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + WAYPOINTS + " " + SNAPSHOT);
        _out.indent(1, "Writes a snapshot of tag waypoints to file");
    }

    @Override
    public void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out)
        throws GatewayException, IOException {

        switch (_action) {
            case SUMMARY:
                mgr.showSummary(_out);
                break;
            case DETAIL:
                mgr.showDetail(nextOrNull(_argIter), _out);
                break;
            case EXITING:
                mgr.showExiting(nextOrNull(_argIter), _out);
                break;
            case SNAPSHOT:
                String snapPath = mgr.snapshot();
                _out.println(SNAPSHOT + ": " + snapPath);
                break;
            case UNLOAD:
                mgr.unload();
                _out.line("unload complete");
                break;
            case STATS:
                doStats(_argIter, _out);
                break;
            case MOBILITY:
                doMobility(_argIter, _out);
                break;
            case WAYPOINTS:
                doWaypoints(_argIter, _out);
                break;
            default:
                usage(_out);
        }
    }

    private void doStats(ArgumentIterator _argIter, PrettyPrinter _out)
        throws GatewayException, IOException {

        if (!_argIter.hasNext()) {
            usageStats(_out);
            return;
        }

        String action = _argIter.next();
        switch (action) {
            case SHOW:
                mgr.showStats(nextOrNull(_argIter), _out);
                break;
            case SNAPSHOT:
                String path = mgr.snapshotStats(nextOrNull(_argIter));
                _out.line(STATS + " " + SNAPSHOT + ": " + path);
                break;
            case SET_REGEX:
                mgr.setInventoryRegex(nextOrNull(_argIter));
                mgr.checkInventoryRegex(_out);
                break;
            case CHECK_REGEX:
                mgr.checkInventoryRegex(_out);
                break;
            case START_RECORDING:
                _out.line(mgr.startRecordingStats());
                break;
            case STOP_RECORDING:
                _out.line(mgr.stopRecordingStats());
                break;
            default:
                usageStats(_out);
                break;
        }

    }

    private void doMobility(ArgumentIterator _argIter, PrettyPrinter _out)
        throws GatewayException, IOException {
        if (!_argIter.hasNext()) {
            usageMobility(_out);
            return;
        }

        RssiAdjuster adj = mgr.getRssiAdjuster();
        if (adj == null) {
            throw new ConfigException("Internal error, no rssi adjuster available");
        }

        String action = _argIter.next();
        switch (action) {
            case SET:
                doMobilitySet(_argIter, adj);
                doMobilityShow(_out, adj);
                break;
            case SHOW:
                doMobilityShow(_out, adj);
                break;
            default:
                usageMobility(_out);
                break;
        }

    }

    private void doMobilitySet(ArgumentIterator _argIter, RssiAdjuster adj)
        throws GatewayException, IOException {
        String id = _argIter.next();
        MobilityProfile mp = MobilityProfileConfig.getProfile(id);
        if (mp == null) {
            throw new ConfigException("no mobility profile found for id " + id);
        }
        adj.set(mp);
    }

    private void doMobilityShow(PrettyPrinter _out, RssiAdjuster adj)
        throws GatewayException, IOException {
        _out.line("ACTIVE:");
        adj.showMobilityProfile(_out);
        _out.blank();
        _out.line("AVAILABLE:");
        Map<String, MobilityProfile> map = MobilityProfileConfig.available();
        for (MobilityProfile mp : map.values()) {
            _out.line(mp.toString());
        }
    }


    private void doWaypoints(ArgumentIterator _argIter, PrettyPrinter _out)
        throws GatewayException, IOException {

        if (!_argIter.hasNext()) {
            usageWaypoints(_out);
            return;
        }

        String action = _argIter.next();
        switch (action) {
            case SHOW: {
                mgr.showWaypoints(nextOrNull(_argIter), _out);
                break;
            }
            case SNAPSHOT:
                String path = mgr.snapshotWaypoints(nextOrNull(_argIter));
                _out.line(WAYPOINTS + " " + SNAPSHOT + ": " + path);
                break;
            default:
                usageWaypoints(_out);
                break;
        }
    }

    private String nextOrNull(ArgumentIterator _argIter) throws SyntaxException {
        if (_argIter.hasNext()) {
            return _argIter.next();
        }
        return null;
    }

}
