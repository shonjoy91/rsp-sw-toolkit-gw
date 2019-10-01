/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.alerts.ConnectionStateEvent;
import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.data.BooleanResult;
import com.intel.rfid.api.data.Cluster;
import com.intel.rfid.api.data.Connection;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.api.data.ReadState;
import com.intel.rfid.api.data.SensorConfigInfo;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.api.sensor.ConnectResponse;
import com.intel.rfid.api.sensor.DeviceAlertNotification;
import com.intel.rfid.api.sensor.OemCfgUpdateNotification;
import com.intel.rfid.api.upstream.RspControllerStatusUpdateNotification;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.controller.ConfigManager;
import com.intel.rfid.controller.Env;
import com.intel.rfid.controller.RspControllerStatus;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.ExpiredTokenException;
import com.intel.rfid.exception.InvalidTokenException;
import com.intel.rfid.exception.RspControllerException;
import com.intel.rfid.helpers.DateTimeHelper;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.Publisher;
import com.intel.rfid.helpers.StringHelper;
import com.intel.rfid.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SensorManager {

    public static final String ALL_SENSORS = "ALL";

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final int HEARTBEAT_CHECK_SECONDS = 30;
    private static final Pattern CONVERT_TO_CHAR_LITERALS = Pattern.compile(".");
    private static final Pattern ALLOW_STAR_PATTERNS = Pattern.compile("[*]", Pattern.LITERAL);

    private static final String STATS_FILE_PREFIX = "sensor_reads";
    private static final String STATS_FILE_EXTENSION = ".csv";
    private static final Path STATS_PATH = Env.resolveStats(STATS_FILE_PREFIX + STATS_FILE_EXTENSION);

    final Map<String, SensorPlatform> deviceIdToRSP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    protected ClusterManager clusterMgr;

    private final Object executorLock = new Object();
    private ExecutorService eventExecutor = ExecutorUtils.newEventExecutor(log, 5);
    private ScheduledExecutorService scheduleExecutor = Executors.newScheduledThreadPool(2);
    private DownstreamManager downstreamMgr;

    private final Publisher<ConnectionStateListener> connectionStatePublisher = new Publisher<>(executorLock,
                                                                                                eventExecutor);
    private final Publisher<ReadStateListener> readStatePublisher = new Publisher<>(executorLock,
                                                                                    eventExecutor);
    private final Publisher<SensorDeviceAlertListener> alertPublisher = new Publisher<>(executorLock,
                                                                                        eventExecutor);
    private final Publisher<OemCfgUpdateListener> oemCfgUpdatePublisher = new Publisher<>(executorLock,
                                                                                          eventExecutor);
    private final Publisher<ConfigUpdateListener> configUpdatePublisher = new Publisher<>(executorLock,
                                                                                          eventExecutor);

    public interface ConnectionStateListener {
        void onConnectionStateChange(ConnectionStateEvent _cse);
    }

    public interface ReadStateListener {
        void onReadStateEvent(ReadStateEvent _cse);
    }

    public interface SensorDeviceAlertListener {
        void onSensorDeviceAlert(DeviceAlertNotification _alert);
    }

    public interface OemCfgUpdateListener {
        void onOemCfgUpdate(OemCfgUpdateNotification _notification);
    }

    public interface ConfigUpdateListener {
        void onConfigUpdate(SensorConfigInfo _configInfo);
    }

    public SensorManager(ClusterManager _clusterMgr) {
        clusterMgr = _clusterMgr;
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

        _scheduler.scheduleAtFixedRate(this::processStats,
                                       SensorStats.SAMPLE_RATE_MINUTES,
                                       SensorStats.SAMPLE_RATE_MINUTES,
                                       TimeUnit.MINUTES);
    }

    public boolean start() {
        // TODO: this is not really idempotent at this point
        // TODO: not sure why it goes from pooled to sequential? and need to include others here? only if
        // sensor manager gets stopped and restarted without a new();
        restore();
        synchronized (executorLock) {
            scheduleExecutor = ExecutorUtils.ensureValidScheduler(scheduleExecutor, this::scheduleTasks);
            eventExecutor = ExecutorUtils.ensureValidSequential(eventExecutor);
            connectionStatePublisher.replaceExecutor(eventExecutor);
            alertPublisher.replaceExecutor(eventExecutor);
        }

        log.info("Sensor Manager started");
        return true;
    }


    public boolean stop() {
        synchronized (executorLock) {
            connectionStatePublisher.clearSubscribers();
            alertPublisher.clearSubscribers();

            try {
                ExecutorUtils.shutdownExecutors(log, scheduleExecutor, eventExecutor);
            } catch (InterruptedException _e) {
                Thread.currentThread().interrupt();
            }
        }

        // preserve a snapshot of the log file by
        // changing the filename to have a date included.
        rollStatsFile();

        persist();

        log.info("sensor manager stopped");
        return true;
    }

    public SensorStateSummary getSummary() {
        SensorStateSummary summary = new SensorStateSummary();

        synchronized (deviceIdToRSP) {
            for (SensorPlatform sensor : deviceIdToRSP.values()) {

                if (sensor.getConnectionState() == Connection.State.CONNECTED) {
                    summary.connected++;
                } else {
                    summary.disconnected++;
                }

                if (sensor.isReading()) { summary.reading++; }
            }
        }
        return summary;
    }

    public void groupForIds(Collection<String> _sensorIds, List<SensorPlatform> _sensors) {
        for (String id : _sensorIds) {
            _sensors.add(establish(id));
        }
    }

    public void groupForProvisioningToken(String _token, List<SensorPlatform> _sensors) {
        synchronized (deviceIdToRSP) {
            for (SensorPlatform sensor : deviceIdToRSP.values()) {
                if (_token.equals(sensor.getProvisionToken())) {
                    _sensors.add(sensor);
                }
            }
        }
    }

    public void registerSensor(String _deviceId, String _provisioningToken)
            throws ExpiredTokenException, InvalidTokenException {

        if (_deviceId == null || _deviceId.isEmpty()) {
            throw new InvalidTokenException("bad device id");
        }

        if (ConfigManager.instance.getProvisionSensorTokenRequired()) {
            clusterMgr.validateToken(_provisioningToken);
        }

        SensorPlatform sensor = establish(_deviceId);
        sensor.setProvisionToken(_provisioningToken);
        clusterMgr.alignSensor(sensor);
    }

    public void getSensors(Collection<SensorPlatform> _collection) {
        synchronized (deviceIdToRSP) {
            _collection.addAll(deviceIdToRSP.values());
        }
    }

    public void getDeviceIds(Collection<String> _collection) {
        synchronized (deviceIdToRSP) {
            _collection.addAll(deviceIdToRSP.keySet());
        }
    }

    public void getFacilities(Collection<String> _collection) {
        Set<String> uniqueFacilities = new TreeSet<>();
        synchronized (deviceIdToRSP) {
            for (SensorPlatform sensor : deviceIdToRSP.values()) {
                uniqueFacilities.add(sensor.getFacilityId());
            }
        }
        _collection.addAll(uniqueFacilities);
    }

    public Collection<SensorPlatform> findRSPs(String _pattern) {
        String literals = CONVERT_TO_CHAR_LITERALS.matcher(_pattern).replaceAll("[$0]");
        String regex = ALLOW_STAR_PATTERNS.matcher(literals).replaceAll(Matcher.quoteReplacement(".*"));

        Collection<SensorPlatform> matchingRSPs;
        synchronized (deviceIdToRSP) {
            matchingRSPs = deviceIdToRSP
                    .entrySet()
                    .stream()
                    .filter(idRSP -> idRSP.getKey().matches(regex))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
        }

        return matchingRSPs;
    }

    /**
     * Returns an RSP instance matching the given deviceId, assuming it is managing
     * an RSP with that ID. If not, then this returns null.
     *
     * @param _deviceId the RSP's device ID.
     * @return A SensorPlatform instance managed by this manager, or null.
     */
    public SensorPlatform getSensor(String _deviceId) {
        synchronized (deviceIdToRSP) {
            return deviceIdToRSP.get(_deviceId);
        }
    }

    /**
     * Creates and returns an RSP instance managed by this sensor manager.
     * If an RSP with this ID is already managed by this manager, it is returned,
     * and no new instance is created.
     *
     * Support Mixed Case Sensitivity for device ID: 
     * The sensors by default have a device ID with lower case characters 
     * in the mac address portion rather than upper case characters which
     * are used in the device labeling.
     * It is important that the code base defaults are the lower case so it matches
     * the mqtt topics that the sensor subscribes to.
     *
     * @param _deviceId the RSP's device ID.
     * @return A SensorPlatform instance managed by this manager.
     */
    public SensorPlatform establish(String _deviceId) {
        SensorPlatform sensor;
        synchronized (deviceIdToRSP) {
            sensor = deviceIdToRSP.get(_deviceId);
            if(sensor == null) {
                String convertedId = StringHelper.convertCaseRSPId(_deviceId);
                sensor = new SensorPlatform(convertedId, this);
                deviceIdToRSP.put(convertedId, sensor);
            }
        }
        return sensor;
    }
    
    public void sendConnectResponse(String _responseId, String _deviceId, String _facilityId)
            throws IOException, RspControllerException {

        if (downstreamMgr == null) {
            throw new RspControllerException("missing sensor manager reference");
        }
        ConfigManager cm = ConfigManager.instance;

        ConnectResponse rsp = new ConnectResponse(_responseId,
                                                  _facilityId,
                                                  System.currentTimeMillis(),
                                                  cm.getLocalHost("ntp.server.host"),
                                                  cm.getSensorSoftwareRepos(),
                                                  SecurityContext.instance().getKeyMgr().getSshEncodedPublicKey());

        downstreamMgr.sendConnectRsp(_deviceId, rsp);
    }

    public void sendSensorCommand(String _deviceId, JsonRequest _req)
            throws IOException, RspControllerException {

        if (downstreamMgr == null) {
            throw new RspControllerException("missing sensor manager reference");
        }
        downstreamMgr.sendCommand(_deviceId, _req);
    }

    // This is a workaround for the API which does not have an explicit message
    // to cause a sensor to reconnect. The main use case for this is a runtime
    // provision token configuration change requiring sensors to re-authenticate 
    public void disconnectAll() {
        if (downstreamMgr != null) {
            RspControllerStatusUpdateNotification gsu = new RspControllerStatusUpdateNotification(ConfigManager.instance
                                                                                                          .getRspControllerDeviceId(),
                                                                                                  RspControllerStatus.RSP_CONTROLLER_SHUTTING_DOWN);
            downstreamMgr.send(gsu);
        }

        // mark all sensors as disconnected
        // assumption here is that all sensors will process the shutting down message
        // and will disconnect without any upstream indication.
        // if they are not marked here as disconnected, then sensor status reports and
        // reconnect sequence is incorrect.
        synchronized (deviceIdToRSP) {
            for (SensorPlatform rsp : deviceIdToRSP.values()) {
                rsp.changeConnectionState(Connection.State.DISCONNECTED,
                                          Connection.Cause.FORCED_DISCONNECT);
            }
        }
    }

    public BooleanResult remove(SensorPlatform _rsp) {
        if (_rsp.connectionState != Connection.State.DISCONNECTED) {
            return new BooleanResult(false,
                                     "refusing to remove sensor " + _rsp.getDeviceId() + " until it is disconnected");
        }

        if (clusterMgr != null) {
            Cluster cluster = clusterMgr.findClusterByDeviceId(_rsp.getDeviceId());
            if (cluster != null) {
                return new BooleanResult(false,
                                         "refusing to remove sensor " + _rsp.getDeviceId() + " used in cluster " + cluster.id);
            }
        }

        synchronized (deviceIdToRSP) {
            _rsp.setFacilityId(SensorPlatform.UNKNOWN_FACILITY_ID);
            if (downstreamMgr != null) {
                downstreamMgr.sensorRemoved(_rsp.getDeviceId());
            }
            deviceIdToRSP.remove(_rsp.getDeviceId());
            notifyConnectionStateChange(_rsp,
                                        _rsp.connectionState,
                                        Connection.State.DISCONNECTED,
                                        Connection.Cause.REMOVED);
        }
        return new BooleanResult(true, "OK");
    }

    public void addConnectionStateListener(ConnectionStateListener _listener) {
        connectionStatePublisher.subscribe(_listener);
    }

    public void removeConnectionStateListener(ConnectionStateListener _listener) {
        connectionStatePublisher.unsubscribe(_listener);
    }

    void notifyConnectionStateChange(final SensorPlatform _rsp,
                                     Connection.State _prevState,
                                     Connection.State _current,
                                     Connection.Cause _cause) {

        // todo: on connected, align with cluster
        connectionStatePublisher.notifyListeners(listener -> listener.onConnectionStateChange(
                new ConnectionStateEvent(_rsp, _prevState, _current, _cause)));
    }

    public void addReadStateListener(ReadStateListener _listener) {
        readStatePublisher.subscribe(_listener);
    }

    public void removeReadStateListener(ReadStateListener _listener) {
        readStatePublisher.unsubscribe(_listener);
    }

    void notifyReadStateChange(SensorPlatform _sensor,
                               ReadState _previous,
                               ReadState _current,
                               Behavior _behavior) {

        final String behaviorId = (_behavior != null) ? _behavior.id : null;
        // todo: on connected, align with cluster
        readStatePublisher.notifyListeners(listener -> listener.onReadStateEvent(
                new ReadStateEvent(_sensor.getDeviceId(), _previous, _current, behaviorId)));
    }

    public void addDeviceAlertListener(SensorDeviceAlertListener _listeners) {
        alertPublisher.subscribe(_listeners);
    }

    public void removeDeviceAlertListener(SensorDeviceAlertListener _listeners) {
        alertPublisher.unsubscribe(_listeners);
    }

    void notifyDeviceAlert(DeviceAlertNotification _alert) {
        alertPublisher.notifyListeners(listener -> listener.onSensorDeviceAlert(_alert));
    }

    public void addOemCfgUpdateListener(OemCfgUpdateListener _listeners) {
        oemCfgUpdatePublisher.subscribe(_listeners);
    }

    public void removeOemCfgUpdateListener(OemCfgUpdateListener _listeners) {
        oemCfgUpdatePublisher.unsubscribe(_listeners);
    }

    void notifyOemCfgUpdate(OemCfgUpdateNotification _notification) {
        oemCfgUpdatePublisher.notifyListeners(listener -> listener.onOemCfgUpdate(_notification));
    }

    public void addConfigUpdateListener(ConfigUpdateListener _listeners) {
        configUpdatePublisher.subscribe(_listeners);
    }

    public void removeConfigUpdateListener(ConfigUpdateListener _listeners) {
        configUpdatePublisher.unsubscribe(_listeners);
    }

    void notifyConfigUpdate(SensorPlatform _sensor) {
        final SensorConfigInfo info = _sensor.getConfigInfo();
        configUpdatePublisher.notifyListeners(listener -> listener.onConfigUpdate(info));
        // persist the new config to disk
        persist();
    }

    private void checkLostHeartbeats() {
        synchronized (deviceIdToRSP) {
            for (SensorPlatform rsp : deviceIdToRSP.values()) {
                rsp.checkLostHeartbeatAndReset();
            }
        }
    }

    private void processStats() {
        Map<String, SensorStats> statsMap = new TreeMap<>();
        // don't lock the map for very long
        synchronized (deviceIdToRSP) {
            for (SensorPlatform rsp : deviceIdToRSP.values()) {
                statsMap.put(rsp.getDeviceId(), rsp.getSensorStats());
            }
        }

        for (SensorStats stats : statsMap.values()) {
            stats.sample();
        }

        // just clobber the file every time
        try (BufferedWriter bw = Files.newBufferedWriter(STATS_PATH)) {

            bw.write("");
            bw.write(", prev10Min, prev10Min, prev10Min");
            bw.write(", prevHour, prevHour, prevHour");
            bw.write(", prevDay, prevDay, prevDay");
            bw.write(", prevWeek, prevWeek, prevWeek");
            bw.newLine();

            bw.write("sensor");
            bw.write(", avgTags, avgRssi, avgUtil");
            bw.write(", avgTags, avgRssi, avgUtil");
            bw.write(", avgTags, avgRssi, avgUtil");
            bw.write(", avgTags, avgRssi, avgUtil");
            bw.newLine();

            for (Map.Entry<String, SensorStats> stringSensorStatsEntry : statsMap.entrySet()) {
                SensorStats stats = stringSensorStatsEntry.getValue();
                bw.write(stringSensorStatsEntry.getKey());
                bw.write(",");
                bw.write(SensorStats.csv(stats.prev10Minutes()));
                bw.write(SensorStats.csv(stats.prevHour()));
                bw.write(SensorStats.csv(stats.prevDay()));
                bw.write(SensorStats.csv(stats.prevWeek()));
                bw.newLine();
            }

            // add a reference timestamp after the data is done
            bw.newLine();
            bw.write("Created,");
            bw.write(DateTimeHelper.toUserLocal(new Date()));
            bw.newLine();

        } catch (IOException _e) {
            log.error("error: {}", _e.getMessage());
        }
    }

    private void rollStatsFile() {
        try {
            if (Files.exists(STATS_PATH)) {
                Path to = STATS_PATH.resolveSibling(STATS_FILE_PREFIX +
                                                            "_" + DateTimeHelper.toFilelNameLocal(new Date()) +
                                                            STATS_FILE_EXTENSION);
                Files.move(STATS_PATH, to);
            }
        } catch (IOException _e) {
            log.error("error: {}", _e.getMessage());
        }
    }

    public static final Path CACHE_PATH = Env.resolveCache("sensors.json");
    protected static final ObjectMapper mapper = Jackson.getMapper();

    public static class Cache {
        public List<Cache.Sensor> sensors = new ArrayList<>();

        public static class Sensor {
            public String deviceId;
            public String faciiltyId;
            public Personality personality;
            public String provisionToken;
            public List<String> aliases = new ArrayList<>();

            public Sensor() { }

            public Sensor(String _deviceId, String _faciiltyId, Personality _personality, String _provisionToken,
                          List<String> _aliases) {
                deviceId = _deviceId;
                faciiltyId = _faciiltyId;
                personality = _personality;
                provisionToken = _provisionToken;
                aliases.addAll(_aliases);
            }
        }

    }

    protected void persist() {

        Cache cache = new Cache();
        synchronized (deviceIdToRSP) {
            for (SensorPlatform rsp : deviceIdToRSP.values()) {
                cache.sensors.add(new Cache.Sensor(rsp.getDeviceId(),
                                                   rsp.getFacilityId(),
                                                   rsp.getPersonality(),
                                                   rsp.getProvisionToken(),
                                                   rsp.getAliases()));
            }
        }

        if (cache.sensors.isEmpty()) { return; }
        try (FileWriter fw = new FileWriter(CACHE_PATH.toAbsolutePath().toString())) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fw, cache);
            log.info("wrote {}", CACHE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("failed persisting sensors {}", e.getMessage());
        }
    }

    protected void restore() {
        if (!Files.exists(CACHE_PATH)) { return; }

        Cache cache = null;

        try (InputStream fis = Files.newInputStream(CACHE_PATH)) {

            cache = mapper.readValue(fis, Cache.class);
            log.info("Restored {}", CACHE_PATH);

        } catch (IOException e) {
            log.error("Failed to restore {}", CACHE_PATH, e);
        }

        if (cache == null) {
            return;
        }

        SensorPlatform realSensor;
        synchronized (deviceIdToRSP) {
            for (Cache.Sensor cachedSensor : cache.sensors) {
                realSensor = deviceIdToRSP.computeIfAbsent(cachedSensor.deviceId,
                                                           k -> new SensorPlatform(cachedSensor.deviceId, this));
                realSensor.setPersonality(cachedSensor.personality);
                realSensor.setFacilityId(cachedSensor.faciiltyId);
                realSensor.setProvisionToken(cachedSensor.provisionToken);
                realSensor.setAliases(cachedSensor.aliases);
            }
        }

    }

}