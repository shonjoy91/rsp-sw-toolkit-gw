/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gpio;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.intel.rfid.api.data.GPIOMapping;
import com.intel.rfid.api.data.GPIOPinFunction;
import com.intel.rfid.api.data.GPIOState;
import com.intel.rfid.api.data.GPIOInfo;
import com.intel.rfid.api.data.GPIOInputEvent;
import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.api.downstream.GPIODeviceConnectResponse;
import com.intel.rfid.api.downstream.GPIOInputNotification;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import com.intel.rfid.api.data.ReadStateEvent;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GPIOManager implements SensorManager.ReadStateListener {

    public static final String ALL_DEVICES = "ALL";
    public static final int HEARTBEAT_CHECK_SECONDS = 30;

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected Map<String, GPIODevice> gpioDevices = new HashMap<>();
    protected List<GPIOMapping> gpioMappings = new ArrayList<>();

    private Object executorLock = new Object();
    private ExecutorService eventExecutor = ExecutorUtils.newEventExecutor(log, 5);
    private ScheduledExecutorService scheduleExecutor = Executors.newScheduledThreadPool(2);
    private DownstreamManager downstreamMgr;
    private SensorManager sensorMgr;

    private final Publisher<GPIOInputEventListener> gpioInputEventPublisher = new Publisher<>(executorLock,
                                                                                                eventExecutor);
    public interface GPIOInputEventListener {
        void onGPIOInputEvent(GPIOInputEvent _gie);
    }

    public GPIOManager(SensorManager _sensorMgr) {
        sensorMgr = _sensorMgr;
        scheduleTasks(scheduleExecutor);
    }

    public void setDownstreamMgr(DownstreamManager _downstreamMgr) {
        downstreamMgr = _downstreamMgr;
    }

    private void scheduleTasks(ScheduledExecutorService _scheduler) {
        _scheduler.scheduleAtFixedRate(this::checkLostHeartbeats,
                                       HEARTBEAT_CHECK_SECONDS,
                                       HEARTBEAT_CHECK_SECONDS,
                                       TimeUnit.SECONDS);
    }

    public boolean start() {
        synchronized (executorLock) {
            eventExecutor = ExecutorUtils.ensureValidSequential(eventExecutor);
            gpioInputEventPublisher.replaceExecutor(eventExecutor);
        }
        sensorMgr.addReadStateListener(this);

        restore();
        log.info("GPIO Manager started");
        return true;
    }


    public boolean stop() {
        synchronized (executorLock) {
            gpioInputEventPublisher.clearSubscribers();
            try {
                ExecutorUtils.shutdownExecutors(log, scheduleExecutor, eventExecutor);
            } catch (InterruptedException _e) {
                Thread.currentThread().interrupt();
            }
        }
        sensorMgr.removeReadStateListener(this);

        persist();
        log.info("GPIO manager stopped");
        return true;
    }

    private void checkLostHeartbeats() {
        synchronized (gpioDevices) {
            for (GPIODevice device : gpioDevices.values()) {
                device.checkLostHeartbeatAndReset();
            }
        }
    }

    public void onReadStateChange(ReadStateEvent _rse) {

        // Loop through the mappings to see if there is anything assigned
        // to this particular sensor for SENSOR_TRANSMITTING
        for (GPIOMapping mapping : gpioMappings) {
            if ((mapping.sensor_id.equals(_rse.rsp.getDeviceId())) &&
                (mapping.function == GPIOPinFunction.SENSOR_TRANSMITTING)) {

                GPIODevice device = gpioDevices.get(mapping.device_id);
                if (device != null) {
                    switch (_rse.current) {
                        case STARTED:
                            device.setGPIOState(mapping.gpio_info.index, GPIOState.ASSERTED);
                            break;
                        case STOPPED:
                            device.setGPIOState(mapping.gpio_info.index, GPIOState.DEASSERTED);
                            break;
                        default:
                            // Do nothing in this case
                            break;
                    }
                } else {
                    log.warn("No GPIO device found matching {}", mapping.device_id);
                }
            }
        }
    }

    /**
     * Returns a GPIODevice instance matching the given deviceId, assuming it is managing
     * a GPIODevice with that ID. If not, then this returns null.
     *
     * @param _deviceId the GPIODevice's device_id.
     * @return A GPIODevice instance managed by this manager, or null.
     */
    public GPIODevice getGPIODevice(String _deviceId) {
        synchronized (gpioDevices) {
            return gpioDevices.get(_deviceId);
        }
    }

    /**
     * Creates and returns an GPIODevice instance managed by this sensor manager.
     * If a GPIODevice with this ID is already managed by this manager, it is returned,
     * and no new instance is created.
     *
     * @param _deviceId the GPIODevice ID.
     * @return A GPIODevice instance managed by this manager.
     */
    public GPIODevice establishGPIODevice(String _deviceId) {
        GPIODevice gpioDevice;
        synchronized (gpioDevices) {
            gpioDevice = gpioDevices.computeIfAbsent(_deviceId, k -> new GPIODevice(_deviceId, this));
        }
        return gpioDevice;
    }

    public void sendGPIOConnectResponse(String _responseId, String _deviceId)
        throws IOException, GatewayException {

        if (downstreamMgr == null) {
            throw new GatewayException("missing downstream manager reference");
        }

        GPIODeviceConnectResponse rsp = new GPIODeviceConnectResponse(_responseId, System.currentTimeMillis());

        downstreamMgr.sendGPIODevceConnectRsp(_deviceId, rsp);
    }

    public void sendGPIOCommand(String _deviceId, JsonRequest _req)
        throws IOException, GatewayException {

        if (downstreamMgr == null) {
            throw new GatewayException("missing downstream manager reference");
        }
        downstreamMgr.sendGPIOCommand(_deviceId, _req);
    }

    public void handleGPIOInput(String _deviceId, GPIOInputNotification _gin)
        throws IOException, GatewayException {

        log.info("handleGPIOInput from {} and index {}", _deviceId, _gin.params.gpio_info.index);
        // Loop through the mappings to see if there is anything assigned
        // to this particular device and pin
        for (GPIOMapping mapping : gpioMappings) {
            if ((mapping.device_id.equals(_deviceId)) &&
                (mapping.gpio_info.index == _gin.params.gpio_info.index)) {

                SensorPlatform sensor = sensorMgr.getRSP(mapping.sensor_id);
                if (sensor != null) {
                    switch (_gin.params.gpio_info.state) {
                        case ASSERTED:
                            sensor.startReading();
                            break;
                        case DEASSERTED:
                            sensor.stopReading();
                            break;
                        default:
                            // Do nothing in this case
                            break;
                    }
                    GPIOInputEvent event = new GPIOInputEvent(mapping, _gin.params.gpio_info.state);
                    notifyGPIOInputEvent(event);
                } else {
                    log.warn("No Sensor found matching {}", mapping.sensor_id);
                }
            }
        }
    }

    /**
     * Removes the given RSP from the manager, setting its facility to Unknown and
     * clearing its personalities. If the RSP was not managed by this manager, it
     * will still have it's facility and personalities cleared.
     *
     * @param _rsp the RSP instance to remove from the manager.
     */
    public void remove(GPIODevice _device) {
        synchronized (gpioDevices) {
            gpioDevices.remove(_device.getDeviceId());
        }
    }

    public void addGPIOInputEventListener(GPIOInputEventListener _listener) {
        gpioInputEventPublisher.subscribe(_listener);
    }

    public void removeGPIOInputEventListener(GPIOInputEventListener _listener) {
        gpioInputEventPublisher.unsubscribe(_listener);
    }

    void notifyGPIOInputEvent(GPIOInputEvent _event) {
        log.info("Publishing GPIO input event {}", _event);
        gpioInputEventPublisher.notifyListeners(listener -> listener.onGPIOInputEvent(_event));
    }

    public boolean addMapping(GPIOMapping _map) {
        GPIODevice device = getGPIODevice(_map.device_id);
        SensorPlatform sensor = sensorMgr.getRSP(_map.sensor_id);
        if ((device != null) && (sensor != null)) {
            // Get the rest of the information to fill out the mapping
            GPIOInfo gpio_info = device.deviceInfo.gpio_info.get(_map.gpio_info.index);
            _map.gpio_info = gpio_info;
            gpioMappings.add(_map);
            return true;
        } else {
            return false;
        }
    }

    public void clearMappings() {
        gpioMappings.clear();
    }

    public static final Path CACHE_PATH = Env.resolveCache("gpio_mappings.json");
    protected static final ObjectMapper mapper = Jackson.getMapper();

    public static class Cache {

        public List<GPIOMapping> gpioMappings = new ArrayList<>();

    }

    protected void persist() {

        if (gpioMappings.isEmpty()) { return; }

        try (FileWriter fw = new FileWriter(CACHE_PATH.toAbsolutePath().toString())) {
            GPIOManager.Cache cache = new GPIOManager.Cache();
            cache.gpioMappings = gpioMappings;
            mapper.writerWithDefaultPrettyPrinter().writeValue(fw, cache);
            log.info("wrote {}", CACHE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("failed persisting mappings {}", e.getMessage());
        }
    }

    protected void restore() {
        if (!Files.exists(CACHE_PATH)) { return; }

        try (InputStream fis = Files.newInputStream(CACHE_PATH)) {
            GPIOManager.Cache cache = mapper.readValue(fis, GPIOManager.Cache.class);
            gpioMappings = cache.gpioMappings;
            log.info("Restored {}", CACHE_PATH);

        } catch (IOException e) {
            log.error("Failed to restore {}", CACHE_PATH, e);
        }

    }

}