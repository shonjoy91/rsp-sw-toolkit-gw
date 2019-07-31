/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gpio;

import com.intel.rfid.api.gpio.GPIO;
import com.intel.rfid.api.gpio.GPIOInfo;
import com.intel.rfid.api.gpio.GPIOMapping;
import com.intel.rfid.console.AnyStringCompleter;
import com.intel.rfid.console.ArgumentIterator;
import com.intel.rfid.console.BetterEnumCompleter;
import com.intel.rfid.console.SyntaxException;
import com.intel.rfid.exception.RSPControllerException;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.sensor.SensorIdCompleter;
import com.intel.rfid.sensor.SensorManager;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.intel.rfid.console.CLICommander.Support;

public class GPIOCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private GPIOManager gpioMgr;
    private SensorManager sensorMgr;

    public GPIOCommands(GPIOManager _gpioMgr, SensorManager _sensorMgr) {
        gpioMgr = _gpioMgr;
        sensorMgr = _sensorMgr;
    }

    public static final String CMD_ID = "gpio";

    public static final String SHOW_DEVICES = "show.devices";
    public static final String SHOW_MAPPING = "show.mapping";
    public static final String CLEAR_MAPPING = "clear.mapping";
    public static final String SHOW_DEVICE_INFO = "show.device.info";
    public static final String MAP_GPIO = "map.gpio";
    public static final String SET_STATE = "set.state";


    @Override
    public String getCommandId() {
        return CMD_ID;
    }


    @Override
    public void getCompleters(List<Completer> _comps) {

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SHOW_DEVICES,
                                     SHOW_MAPPING,
                                     CLEAR_MAPPING),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SHOW_DEVICE_INFO),
                new GPIODeviceIdCompleter(gpioMgr),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(MAP_GPIO),
                new SensorIdCompleter(sensorMgr),
                new GPIODeviceIdCompleter(gpioMgr),
                new AnyStringCompleter(),
                new BetterEnumCompleter(GPIO.PinFunction.class),
                new NullCompleter()
        ));

        _comps.add(new ArgumentCompleter(
                new StringsCompleter(CMD_ID),
                new StringsCompleter(SET_STATE),
                new GPIODeviceIdCompleter(gpioMgr),
                new AnyStringCompleter(),
                new BetterEnumCompleter(GPIO.State.class),
                new NullCompleter()
        ));
    }

    @Override
    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW_DEVICES);
        _out.indent(1, "Displays the connected GPIO devices");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW_MAPPING);
        _out.indent(1, "Displays the current GPIO mappings");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + CLEAR_MAPPING);
        _out.indent(1, "Removes all GPIO mappings");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW_DEVICE_INFO + " <gpio_id>");
        _out.indent(1, "Displays the available GPIO info for the specified device");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + MAP_GPIO + " <sensor_device_id> <gpio_id> <index> <function>");
        _out.indent(1, "Maps a particular GPIO pin to Sensor Function");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_STATE + " <gpio_id> <index> <state>");
        _out.indent(1, "Forces the state of a particular output GPIO");
        _out.blank();
    }

    @Override
    public void doAction(String action, ArgumentIterator _argIter, PrettyPrinter _out)
            throws SyntaxException, RSPControllerException, IOException {

        switch (action) {
            case SHOW_DEVICES:
                doShowDevices(_out);
                break;
            case SHOW_MAPPING:
                doShowMapping(_out);
                break;
            case CLEAR_MAPPING:
                doClearMapping(_out);
                break;
            case SHOW_DEVICE_INFO:
                doShowDeviceInfo(_argIter, _out);
                break;
            case MAP_GPIO:
                doMapGPIO(_argIter, _out);
                break;
            case SET_STATE:
                doSetState(_argIter, _out);
                break;
            default:
                usage(_out);
        }
    }

    public void doShowDevices(PrettyPrinter _out) throws RSPControllerException {

        if (gpioMgr.gpioDevices.isEmpty()) {
            _out.println("No GPIO Devices Connected");
            return;
        }

        _out.println(GPIODevice.HDR);
        _out.println();
        for (String device_id : gpioMgr.gpioDevices.keySet()) {
            GPIODevice device = gpioMgr.getGPIODevice(device_id);
            _out.println(device);
        }
    }

    public void doShowMapping(PrettyPrinter _out) throws RSPControllerException {

        _out.println(GPIOMapping.HDR);
        _out.println();
        for (GPIOMapping mapping : gpioMgr.gpioMappings) {
            _out.println(mapping);
        }
    }

    public void doClearMapping(PrettyPrinter _out) throws RSPControllerException {

        gpioMgr.clearMappings();
        _out.println("OK");
    }

    public void doShowDeviceInfo(ArgumentIterator _argIter, PrettyPrinter _out) throws RSPControllerException {

        if (gpioMgr.gpioDevices.isEmpty()) {
            _out.println("No GPIO Devices Connected");
            return;
        }

        final String device_id = String.valueOf(_argIter.next());
        final GPIODevice device = gpioMgr.getGPIODevice(device_id);

        _out.println(GPIOInfo.HDR);
        _out.println();
        for (GPIOInfo info : device.deviceInfo.gpio_info) {
            _out.println(info);
        }
    }

    public void doMapGPIO(ArgumentIterator _argIter, PrettyPrinter _out) throws RSPControllerException {

        try {
            GPIOMapping mapping = new GPIOMapping();
            mapping.sensor_device_id = String.valueOf(_argIter.next());
            mapping.gpio_device_id = String.valueOf(_argIter.next());
            mapping.gpio_info.index = Integer.valueOf(_argIter.next());
            mapping.function = GPIO.PinFunction.valueOf(_argIter.next());

            GPIODevice device = gpioMgr.getGPIODevice(mapping.gpio_device_id);
            if (device != null) {
                // Range check the index
                if (mapping.gpio_info.index < device.deviceInfo.gpio_info.size()) {
                    GPIOInfo info = device.deviceInfo.gpio_info.get(mapping.gpio_info.index);
                    // Only certain combinations of mappings are valid
                    // so check for them here
                    switch (info.direction) {
                        case INPUT:
                            switch (mapping.function) {
                                case START_READING:
                                case STOP_READING:
                                    gpioMgr.addMapping(mapping);
                                    _out.println("OK");
                                    break;
                                default:
                                    _out.println("Not a valid GPIO mapping for this device");
                                    break;
                            }
                            break;
                        case OUTPUT:
                            switch (mapping.function) {
                                case SENSOR_CONNECTED:
                                case SENSOR_DISCONNECTED:
                                case SENSOR_READING_TAGS:
                                case SENSOR_TRANSMITTING:
                                    gpioMgr.addMapping(mapping);
                                    _out.println("OK");
                                    break;
                                default:
                                    _out.println("Not a valid GPIO mapping for this device");
                                    break;
                            }
                            break;
                    }
                } else {
                    _out.line("Index " + mapping.gpio_info.index + " out of range.");
                }
            }
        } catch (Exception e) {
            _out.println(e.toString());
        }
    }

    public void doSetState(ArgumentIterator _argIter, PrettyPrinter _out) throws RSPControllerException {

        if (gpioMgr.gpioDevices.isEmpty()) {
            _out.println("No GPIO Devices Connected");
            return;
        }

        final String device_id = String.valueOf(_argIter.next());
        final int index = Integer.valueOf(_argIter.next());
        final GPIO.State state = GPIO.State.valueOf(_argIter.next());
        GPIODevice device = gpioMgr.gpioDevices.get(device_id);
        if (device != null) {
            device.setGPIOState(index, state);
            _out.println("OK");
        } else {
            _out.println("FAILED");
        }
    }
}
