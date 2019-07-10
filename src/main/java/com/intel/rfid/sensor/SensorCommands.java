/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.data.DeviceAlertType;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.api.sensor.AlertSeverity;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.api.sensor.GeoRegion;
import com.intel.rfid.api.sensor.LEDState;
import com.intel.rfid.behavior.BehaviorCompleter;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.console.AnyStringCompleter;
import com.intel.rfid.console.ArgumentIterator;
import com.intel.rfid.console.BetterEnumCompleter;
import com.intel.rfid.console.BooleanCompleter;
import com.intel.rfid.console.CLICommander;
import com.intel.rfid.console.SyntaxException;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.helpers.DateTimeHelper;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class SensorCommands implements CLICommander.Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private SensorManager sensorMgr;


    public SensorCommands(SensorManager _sensorMgr) {
        sensorMgr = _sensorMgr;
    }

    private ObjectMapper mapper = Jackson.getMapper();

    public static final String CMD_ID = "sensor";

    public static final String SHOW = "show";
    public static final String SET_BEHAVIOR = "set.behavior";
    public static final String START_READING = "start.reading";
    public static final String STOP_READING = "stop.reading";
    public static final String FORCE_ALL_DISCONNECT = "force.all.disconnect";
    public static final String RESET = "reset";
    public static final String REBOOT = "reboot";
    public static final String SHUTDOWN = "shutdown";
    public static final String REMOVE = "remove";
    public static final String GET_BIST = "get.bist.results";
    public static final String GET_STATE = "get.state";
    public static final String GET_SW_VERS = "get.versions";
    public static final String SET_LED = "set.led";
    public static final String GET_LAST_COMMS = "get.last.comms";
    public static final String SET_MOTION_EVENT = "set.motion";
    public static final String SET_FACILITY = "set.facility";
    public static final String SET_MIN_RSSI = "set.min.rssi";
    public static final String SET_PERSONALITY = "set.personality";
    public static final String CLEAR_PERSONALITY = "clear.personality";
    public static final String SET_ALIAS = "set.alias";
    public static final String GET_ALIASES = "get.aliases";
    public static final String SEND_EVENTS = "send.events";
    public static final String CAPTURE_IMG = "capture.images";
    public static final String SET_ALERT_THRESHOLD = "set.alert.threshold";
    public static final String ACK_ALERT = "ack.alert";
    public static final String MUTE_ALERT = "mute.alert";
    public static final String STATS = "stats";
    public static final String TOKENS = "tokens";
    public static final String GET_GEO_REGION = "get.geo.region";
    public static final String SET_GEO_REGION = "set.geo.region";
    public static final String SOFTWARE_UPDATE = "software.update";

    @Override
    public String getCommandId() {
        return CMD_ID;
    }


    @Override
    public void getCompleters(List<Completer> _comps) {

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(START_READING, STOP_READING,
                                     RESET, REBOOT, SHUTDOWN, REMOVE,
                                     CLEAR_PERSONALITY,
                                     GET_BIST,
                                     GET_LAST_COMMS,
                                     GET_STATE,
                                     GET_SW_VERS,
                                     TOKENS,
                                     GET_GEO_REGION,
                                     SOFTWARE_UPDATE),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(FORCE_ALL_DISCONNECT),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_BEHAVIOR),
                new BehaviorCompleter(),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SHOW, STATS),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_LED),
                new BetterEnumCompleter(LEDState.class),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_ALERT_THRESHOLD),
                new BetterEnumCompleter(DeviceAlertType.class),
                new BetterEnumCompleter(AlertSeverity.class),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_FACILITY),
                new AnyStringCompleter(),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_MIN_RSSI),
                new AnyStringCompleter(),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_PERSONALITY),
                new BetterEnumCompleter(Personality.class),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_ALIAS),
                new AnyStringCompleter(),
                new BetterEnumCompleter(AntennaPort.class),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_ALIAS),
                new StringsCompleter(SensorPlatform.ALIAS_KEY_DEFAULT, SensorPlatform.ALIAS_KEY_DEVICE_ID),
                new BetterEnumCompleter(AntennaPort.class),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));


        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(GET_ALIASES),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(ACK_ALERT, MUTE_ALERT),
                new BetterEnumCompleter(DeviceAlertType.class),
                new BooleanCompleter(),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_MOTION_EVENT),
                new StringsCompleter(SEND_EVENTS),
                new BooleanCompleter(),
                new StringsCompleter(CAPTURE_IMG),
                new BooleanCompleter(),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_GEO_REGION),
                new BetterEnumCompleter(GeoRegion.class),
                new SensorIdCompleter(sensorMgr),
                new NullCompleter()
        ));


    }

    @Override
    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW + " [ regex ]");
        _out.indent(1, "Displays a list of all known sensors");
        _out.indent(1, "Optional regex will match against the sensor id");
        _out.indent(1, "A sensor is known to the gateway by the following:");
        _out.indent(2, "Listed in facility group configuration");
        _out.indent(2, "Listed in personality group configuration");
        _out.indent(2, "Listed in schedule group configuration");
        _out.indent(2, "Actively connects to the gateway regardless of facility, personality, or schedule inclusion");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + START_READING + " <device_id>...");
        _out.indent(1, "Start reading using the currently configured behavior(s)");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + STOP_READING + " <device_id>...");
        _out.indent(1, "Stop reading");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + FORCE_ALL_DISCONNECT);
        _out.indent(1, "forces a disconnect of ALL sensors (sends gateway shutting down message)");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + RESET + " <device_id>...");
        _out.indent(1, "Command device to perform a reset");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + REBOOT + " <device_id>...");
        _out.indent(1, "Command device to perform a reboot");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHUTDOWN + " <device_id>...");
        _out.indent(1, "Command device to perform an OS shutdown");
        _out.indent(1, "CAUTION: Do this ONLY prior to physically removing the device!");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + REMOVE + " <device_id>...");
        _out.indent(1, "Removes device from ALL configurations (faciility, personality, schedule)");
        _out.indent(1, "The device must be in the DISCONNECTED state");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + GET_BIST + " <device_id>...");
        _out.indent(1, "Retrieve Built-In-Self-Test results");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + GET_LAST_COMMS + " <device_id>...");
        _out.indent(1, "Display last heard time");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + GET_STATE + " <device_id>...");
        _out.indent(1, "Retrieve device capabilities");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + GET_SW_VERS + " <device_id>...");
        _out.indent(1, "Retrieve device software version");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_ALERT_THRESHOLD + " <type> <severity> <device_id>...");
        _out.indent(1, "Change a Device Alert Threshold");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_BEHAVIOR + " <behavior> <device_id>...");
        _out.indent(1, "Apply a predefined RFID behavior");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_MIN_RSSI + " <dBm10X> <device_id>...");
        _out.indent(1, "Apply a minimum acceptable RSSI level");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_FACILITY + " <facility_id> <device_id>...");
        _out.indent(1, "Assign the Facility ID");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_PERSONALITY + " <personality> <device_id>...");
        _out.indent(1, "Add a personality");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + CLEAR_PERSONALITY + " <device_id>...");
        _out.indent(1, "Clears a personality");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_ALIAS + " <alias> <antenna-port> <device_id>...");
        _out.indent(1, "Add an alias to a device:antenna-port combination");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + GET_ALIASES + " <antenna-port> <device_id>...");
        _out.indent(1, "Retrieve the alias assigned to a device:antenna-port combination");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_LED + " <led_state> <device_id>...");
        _out.indent(1, "Apply a visual indicator behavior");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_MOTION_EVENT + " " +
                SEND_EVENTS + BooleanCompleter.asOptions() +
                CAPTURE_IMG + BooleanCompleter.asOptions() +
                " <device_id>...");
        _out.indent(1, "Enable or disable the sending of Motion Events");
        _out.indent(1, "Enable or disable the capture of .jpg images");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + ACK_ALERT + " <alert_type>" + BooleanCompleter
                .asOptions() + " <device_id>...");
        _out.indent(1, "Acknowledge a Device Alert");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + MUTE_ALERT + " <alert_type>" + BooleanCompleter
                .asOptions() + " <device_id>...");
        _out.indent(1, "Disable a particular Device Alert");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + TOKENS + " <device_id>...");
        _out.indent(1, "Show the token associated with the device");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + GET_GEO_REGION + " <device_id>...");
        _out.indent(1, "Retrieves the OEM configured geographic region");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_GEO_REGION + " <region> <device_id>...");
        _out.indent(1, "!!! WARNING  WARNING WARNING !!!");
        _out.indent(1, "Changes the OEM configuration for the rfid module frequency configuration");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SOFTWARE_UPDATE + " <device_id>...");
        _out.indent(1,
                    "Triggers the software update script to run on the device that checks for software updates from the package repo");
    }

    @Override
    public void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out)
            throws GatewayException, IOException {

        long timeoutMillis = ResponseHandler.DEFAULT_WAIT_TIMEOUT_MILLIS;
        RSPCommandCallback rcc = null;

        switch (_action) {

            case GET_LAST_COMMS:
                printLastComms(_argIter, _out);
                break;

            case SHOW:
                doShow(_argIter, _out);
                break;

            case STATS:
                doStats(_argIter, _out);
                break;

            case SET_BEHAVIOR:
                String bid = _argIter.next();
                Behavior behavior = BehaviorConfig.getBehavior(bid);
                for (SensorPlatform rsp : getRSPs(_argIter, _out)) {
                    rsp.setBehavior(behavior);
                    showCmdResult(rsp, SET_BEHAVIOR, true, _out);
                }
                break;

            case START_READING:
                rcc = new RSPCommandCallback(START_READING) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.startReading();
                    }
                };
                break;

            case STOP_READING:
                rcc = new RSPCommandCallback(STOP_READING) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.stopReading();
                    }
                };
                break;

            case FORCE_ALL_DISCONNECT:
                _out.line("Forcing sensors to disconnect: ");
                sensorMgr.disconnectAll();
                break;

            case RESET:
                // Give reset command extra time to respond
                timeoutMillis = ResponseHandler.SENSOR_RESET_TIMEOUT_MILLIS;
                rcc = new RSPCommandCallback(RESET) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.reset();
                    }
                };
                break;

            case REBOOT:
                rcc = new RSPCommandCallback(REBOOT) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.reboot();
                    }
                };
                break;

            case SHUTDOWN:
                rcc = new RSPCommandCallback(SHUTDOWN) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.shutdown();
                    }
                };
                break;

            case TOKENS:
                doTokens(_argIter, _out);
                break;

            case REMOVE:
                doRemove(_argIter, _out);
                break;

            case GET_BIST:
                rcc = new RSPCommandCallback(GET_BIST) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.getBISTResults();
                    }
                };
                break;

            case GET_STATE:
                rcc = new RSPCommandCallback(GET_STATE) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.getState();
                    }
                };
                break;

            case GET_SW_VERS:
                rcc = new RSPCommandCallback(GET_SW_VERS) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.getSoftwareVersion();
                    }
                };
                break;

            case SET_LED:
                final LEDState state = LEDState.valueOf(_argIter.next());
                rcc = new RSPCommandCallback(SET_LED) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.setLED(state);
                    }
                };
                break;

            case SET_ALERT_THRESHOLD:
                final DeviceAlertType t1 = DeviceAlertType.valueOf(_argIter.next());
                final AlertSeverity severity = AlertSeverity.valueOf(_argIter.next());
                final Integer threshold;
                try {
                    threshold = NumberFormat.getInstance().parse(_argIter.next()).intValue();
                } catch (ParseException pe) {
                    throw new SyntaxException(pe.getMessage());
                }
                rcc = new RSPCommandCallback(SET_ALERT_THRESHOLD) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.setAlertThreshold(t1.id, severity, threshold);
                    }
                };
                break;
            case ACK_ALERT:
                final DeviceAlertType t2 = DeviceAlertType.valueOf(_argIter.next());
                final boolean ack = Boolean.parseBoolean(_argIter.next());
                rcc = new RSPCommandCallback(ACK_ALERT) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.acknowledgeAlert(t2.id, ack);
                    }
                };
                break;
            case MUTE_ALERT:
                final DeviceAlertType t3 = DeviceAlertType.valueOf(_argIter.next());
                final boolean mute = Boolean.parseBoolean(_argIter.next());
                rcc = new RSPCommandCallback(MUTE_ALERT) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.muteAlert(t3.id, mute);
                    }
                };
                break;

            case SET_MOTION_EVENT:
                boolean b = true;
                if (SEND_EVENTS.equals(_argIter.next())) { b = Boolean.parseBoolean(_argIter.next()); }
                final boolean sendEvents = b;
                if (CAPTURE_IMG.equals(_argIter.next())) { b = Boolean.parseBoolean(_argIter.next()); }
                final boolean captureImages = b;

                rcc = new RSPCommandCallback(SET_MOTION_EVENT) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.setMotion(sendEvents, captureImages);
                    }
                };
                break;

            case SET_MIN_RSSI: {
                try {
                    final int minRssiDbm10x = Integer.parseInt(_argIter.next());
                    for (SensorPlatform rsp : getRSPs(_argIter, _out)) {
                        rsp.setMinRssiDbm10X(minRssiDbm10x);
                        _out.line(SET_MIN_RSSI + " : OK");
                    }
                } catch (NumberFormatException nfe) {
                    _out.line(SET_MIN_RSSI + " requires an Integer ideally in the range -1000 to -100");
                }
            }
            break;

            case SET_FACILITY: {
                final String facilityId = _argIter.next();
                rcc = new RSPCommandCallback(SET_FACILITY) {
                    public ResponseHandler callCommand(SensorPlatform _rsp) {
                        return _rsp.setFacilityId(facilityId);
                    }
                };
            }
            break;

            case SET_PERSONALITY: {
                final Personality p = Personality.valueOf(_argIter.next());
                for (SensorPlatform rsp : getRSPs(_argIter, _out)) {
                    rsp.setPersonality(p);
                    showCmdResult(rsp, SET_PERSONALITY, true, _out);
                }
            }
            break;

            case CLEAR_PERSONALITY: {
                for (SensorPlatform sensor : getRSPs(_argIter, _out)) {
                    sensor.clearPersonality();
                    showCmdResult(sensor, CLEAR_PERSONALITY, true, _out);
                }
            }
            break;

            case SET_ALIAS: {
                doSetAlias(_argIter, _out);
            }
            break;

            case GET_ALIASES: {
                final AntennaPort port = AntennaPort.valueOf(_argIter.next());
                for (SensorPlatform rsp : getRSPs(_argIter, _out)) {
                    showCmdResult(rsp, rsp.getAliasesAsString(), true, _out);
                }
            }
            break;

            case GET_GEO_REGION: {
                for (SensorPlatform sensor : getRSPs(_argIter, _out)) {
                    ResponseHandler rh = sensor.getGeoRegion();
                    try {
                        rh.waitForResponse(10, TimeUnit.SECONDS);
                        mapper.writeValue(_out, rh.getResult());
                    } catch (InterruptedException _e) {
                        _out.line(SET_GEO_REGION + " Interrupted waiting for response");
                        Thread.currentThread().interrupt();
                    }
                }
            }
            break;

            case SET_GEO_REGION: {
                final GeoRegion region = GeoRegion.valueOf(_argIter.next());
                for (SensorPlatform sensor : getRSPs(_argIter, _out)) {
                    ResponseHandler rh = sensor.setGeoRegion(region);
                    try {
                        _out.line("sending command to " + sensor.getDeviceId() + " and waiting up to 5 minutes");
                        rh.waitForResponse(5, TimeUnit.MINUTES);
                    } catch (InterruptedException _e) {
                        _out.line(SET_GEO_REGION + " Interrupted waiting for response");
                        Thread.currentThread().interrupt();
                    }
                    showCmdResult(sensor, SET_GEO_REGION, !rh.isError(), _out);
                }
            }
            break;

            case SOFTWARE_UPDATE: {
                for (SensorPlatform sensor : getRSPs(_argIter, _out)) {
                    ResponseHandler rh = sensor.softwareUpdate();
                    try {
                        rh.waitForResponse(10, TimeUnit.SECONDS);
                        mapper.writeValue(_out, rh.getResult());
                    } catch (InterruptedException _e) {
                        _out.line(SET_GEO_REGION + " Interrupted waiting for response");
                        Thread.currentThread().interrupt();
                    }
                }
            }
            break;

            default:
                usage(_out);
        }

        if (rcc != null) {
            doRSPCommand(_argIter, rcc, _out, timeoutMillis);
        }

    }

    public void doSetAlias(ArgumentIterator _argIter, PrettyPrinter _out) throws GatewayException {

        String alias = _argIter.next();
        AntennaPort port = AntennaPort.valueOf(_argIter.next());

        ArrayList<Integer> portIndexes = new ArrayList<>();

        switch (port) {
            case PORT_0:
                portIndexes.add(0);
                break;
            case PORT_1:
                portIndexes.add(1);
                break;
            case PORT_2:
                portIndexes.add(2);
                break;
            case PORT_3:
                portIndexes.add(3);
                break;
            case PORTS_0_1:
                portIndexes.add(0);
                portIndexes.add(1);
                break;
            case PORTS_2_3:
                portIndexes.add(2);
                portIndexes.add(3);
                break;
            case ALL_PORTS:
            default:
                for (int j = 0; j < SensorPlatform.NUM_ALIASES; j++) {
                    portIndexes.add(j);
                }
                break;
        }

        for (SensorPlatform rsp : getRSPs(_argIter, _out)) {
            for (int portIndex : portIndexes) {
                rsp.setAlias(portIndex, alias);
                showCmdResult(rsp, SET_ALIAS, true, _out);
            }
        }
    }

    public void doShow(ArgumentIterator _argIter, PrettyPrinter _out) throws GatewayException {

        TreeSet<SensorPlatform> sensors = new TreeSet<>(getRSPs(_argIter, _out));
        if (sensors.isEmpty()) { return; }

        _out.println(SensorPlatform.HDR);
        _out.println();
        for (SensorPlatform rsp : sensors) {
            _out.println(rsp);
        }
    }

    public void doStats(ArgumentIterator _argIter, PrettyPrinter _out) throws GatewayException {

        Collection<SensorPlatform> rsps = getRSPs(_argIter, _out);
        if (rsps.isEmpty()) { return; }

        StringBuilder sb = new StringBuilder();

        sb.append(tableRowIdSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append("\n");

        sb.append(tableRowId(""));
        sb.append(tableColLabel("prev10Min"));
        sb.append(tableColLabel("prevHour"));
        sb.append(tableColLabel("prevDay"));
        sb.append(tableColLabel("prevWeek"));
        sb.append("\n");

        sb.append(tableRowId("Device Id"));
        sb.append(tableColHeader());
        sb.append(tableColHeader());
        sb.append(tableColHeader());
        sb.append(tableColHeader());
        sb.append("\n");

        sb.append(tableRowIdSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append("\n");

        for (SensorPlatform rsp : rsps) {
            SensorStats stats = rsp.getSensorStats();
            sb.append(tableRowId(rsp.getDeviceId()));
            sb.append(tableCol(stats.prev10Minutes()));
            sb.append(tableCol(stats.prevHour()));
            sb.append(tableCol(stats.prevDay()));
            sb.append(tableCol(stats.prevWeek()));
            sb.append("\n");
        }

        sb.append(tableRowIdSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append(tableColSep());
        sb.append("\n");

        _out.line(sb.toString());
    }

    public void doTokens(ArgumentIterator _argIter, PrettyPrinter _out) throws GatewayException {

        TreeSet<SensorPlatform> sensors = new TreeSet<>(getRSPs(_argIter, _out));
        if (sensors.isEmpty()) { return; }

        _out.println("device      token");
        _out.println();
        for (SensorPlatform rsp : sensors) {
            _out.println(rsp.getDeviceId() + "  " + rsp.getProvisionToken());
        }
    }

    public void showCmdResult(SensorPlatform _rsp,
                              String _cmd,
                              boolean _success,
                              PrettyPrinter _out) {
        String result = (_success ? "OK" : "FAILED");
        _out.line(_cmd + " : " + result);
        _out.line(_rsp.toString());
    }

    public void doRemove(ArgumentIterator _argIter, PrettyPrinter _out) throws GatewayException {
        for (SensorPlatform rsp : getRSPs(_argIter, _out)) {
            SensorManager.RemoveResult result = sensorMgr.remove(rsp);
            _out.line("Removing " + rsp.getDeviceId() + ": " + result.message);
        }
    }

    public void printLastComms(ArgumentIterator _argIter, PrettyPrinter _out) throws SyntaxException {
        for (SensorPlatform rsp : getRSPs(_argIter, _out)) {
            long last = rsp.getLastCommsMillis();
            String readable;
            if (last == 0L) {
                readable = "out of comms";
            } else {
                readable = DateTimeHelper.toUserLocal(new Date(last));
            }
            _out.line(rsp.getDeviceId() + " - " + readable);
        }
    }

    private Collection<SensorPlatform> getRSPs(ArgumentIterator _argIter, PrettyPrinter _out)
            throws SyntaxException {

        HashSet<SensorPlatform> rsps = new HashSet<>();

        if (_argIter == null || !_argIter.hasNext()) {
            sensorMgr.getSensors(rsps);
            return rsps;
        }

        while (_argIter.hasNext()) {
            String id = _argIter.next();
            if (SensorManager.ALL_SENSORS.equals(id)) {
                sensorMgr.getSensors(rsps);
                if (rsps.size() == 0) {
                    _out.error("No sensors available");
                }
            } else if (id.contains("*")) {
                rsps.addAll(sensorMgr.findRSPs(id));
                if (rsps.size() == 0) {
                    _out.error("No sensors available");
                }
            } else {
                SensorPlatform rsp = sensorMgr.getSensor(id);
                if (rsp != null) {
                    rsps.add(rsp);
                } else {
                    _out.error("No sensor found with id " + id);
                }
            }
        }
        return rsps;
    }

    private abstract class RSPCommandCallback {

        public final String commandLabel;

        public RSPCommandCallback(String _commandLabel) {
            commandLabel = _commandLabel;
        }

        public abstract ResponseHandler callCommand(SensorPlatform _rsp);
    }

    private void doRSPCommand(ArgumentIterator _argIter, RSPCommandCallback _callback, PrettyPrinter _out,
                              long _timeoutMillis)
            throws SyntaxException {

        TreeMap<String, ResponseHandler> handlers = new TreeMap<>();
        for (SensorPlatform rsp : getRSPs(_argIter, _out)) {

            ResponseHandler handler = _callback.callCommand(rsp);
            if (!handler.isError()) {
                handlers.put(rsp.getDeviceId(), handler);
            } else {
                handler.show(mapper, _out);
            }
        }

        for (String deviceId : handlers.keySet()) {

            ResponseHandler handler = handlers.get(deviceId);
            try {
                if (handler.waitForResponse(_timeoutMillis, TimeUnit.MILLISECONDS)) {
                    handler.show(mapper, _out);
                } else {
                    _out.line(deviceId + " - TIMED OUT");
                }
            } catch (InterruptedException e) {
                _out.line(deviceId + " - INTERRUPTED");
                Thread.currentThread().interrupt();
            }
        }
    }

    public static String tableRowIdSep() {
        return "--------------";
    }

    public static String tableRowId(String _s) {
        return String.format("| %10s |", _s);
    }

    public static String tableColHeader() {
        return "|   #Tags   RSSI Ut% |";
    }

    public static String tableColSep() {
        return "----------------------";
    }

    public static String tableColLabel(String _label) {
        return String.format("| %19s|", _label);
    }

    public static String tableCol(SensorStats.Data _d) {
        if (_d != null) {
            return String.format("| %7.0f %6.0f %3.0f |",
                                 _d.avgTagCount, _d.avgRssiDbm, (_d.avgUtilization * 100));
        } else {
            return "|     N/A    N/A N/A |";
        }
    }

    public enum AntennaPort {
        PORT_0,
        PORT_1,
        PORT_2,
        PORT_3,
        PORTS_0_1,
        PORTS_2_3,
        ALL_PORTS
    }
}
