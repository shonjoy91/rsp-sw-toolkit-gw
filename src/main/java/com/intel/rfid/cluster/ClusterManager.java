/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.data.Cluster;
import com.intel.rfid.api.data.ClusterConfig;
import com.intel.rfid.api.data.ClusterTemplate;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.api.data.ScheduleRunState;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.controller.Env;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.exception.ExpiredTokenException;
import com.intel.rfid.exception.InvalidTokenException;
import com.intel.rfid.exception.RspControllerException;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.security.ProvisionToken;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterManager {

    public static final Path CACHE_PATH = Env.resolveCache("cluster_manager.json");

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper mapper = Jackson.getMapper();

    protected SensorManager sensorMgr;
    protected ScheduleManager schedMgr;
    protected DownstreamManager downstreamMgr;
    protected final Object clusterCfgLock = new Object();
    protected ClusterConfig clusterCfg;
    protected Map<String, ProvisionToken> tokens = new HashMap<>();


    public void start() {
        try {
            restore();
        } catch (IOException | RspControllerException _e) {
            log.warn("failed restoring {} {}", CACHE_PATH, _e.getMessage());
        }
        log.info("started");
    }

    public void stop() {
        persist();
    }

    public void loadConfig(Path _clusterCfgPath) throws IOException, ConfigException {
        ClusterConfig newConfig = fromFile(_clusterCfgPath);
        loadConfig(newConfig);
    }

    public void loadConfig(ClusterConfig newConfig) throws ConfigException {
        validate(newConfig);

        synchronized (clusterCfgLock) {
            clusterCfg = newConfig;
            persist();
            updateTokenMap();
        }

        align();
    }

    public ClusterConfig deleteConfig() {
        ClusterConfig cc;
        synchronized (clusterCfgLock) {
            cc = clusterCfg;
            clusterCfg = null;
            persist();
            updateTokenMap();
        }
        align();
        return cc;
    }

    private void align() {
        if (sensorMgr == null) {
            log.warn("missing reference to sensor manager");
            return;
        }

        boolean restartScheduler = false;
        if (schedMgr != null && schedMgr.getRunState() == ScheduleRunState.FROM_CONFIG) {
            log.info("deactivating scheduler to trigger load new cluster configuration");
            restartScheduler = true;
            schedMgr.setRunState(ScheduleRunState.INACTIVE);
        }

        if (!tokens.isEmpty()) {
            // need to kick all sensors off so they will reconnect and establish their credentials
            // but then the align might not work because the disconnect sequencing / messaging is asynchronous
            // mostly need to kick off if using provisioning tokens (what if they have changed in new file??)
            sensorMgr.disconnectAll();
        } else if (clusterCfg != null) {
            final Set<String> sensorIDsInConfig = new HashSet<>();

            for (Cluster cluster : clusterCfg.clusters) {
                for (List<String> sensorGroup : cluster.sensor_groups) {
                    sensorIDsInConfig.addAll(sensorGroup);
                }
            }

            for (String id : sensorIDsInConfig) {
                SensorPlatform sensor = sensorMgr.establish(id);
                alignSensor(sensor);
            }
        }

        if (restartScheduler) {
            schedMgr.setRunState(ScheduleRunState.FROM_CONFIG);
        }
    }

    public ClusterConfig getConfig() {
        ClusterConfig cc = null;
        synchronized (clusterCfgLock) {
            if (clusterCfg != null) {
                try {
                    cc = mapper.readValue(mapper.writeValueAsBytes(clusterCfg), ClusterConfig.class);
                } catch (IOException _e) {
                    log.warn("error: {}", _e.getMessage());
                }
            }
        }
        return cc;
    }

    public ClusterTemplate getTemplate() {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.personalities = Arrays.asList(Personality.values());
        for (Behavior b : BehaviorConfig.available().values()) {
            clusterTemplate.behavior_ids.add(b.id);
        }
        if (sensorMgr != null) {
            sensorMgr.getDeviceIds(clusterTemplate.sensor_device_ids);
        }
        return clusterTemplate;
    }

    public void setSensorManager(SensorManager _sensorMgr) {
        sensorMgr = _sensorMgr;
    }

    public void setScheduleManager(ScheduleManager _schedMgr) {
        schedMgr = _schedMgr;
    }

    public void setDownstreamManager(DownstreamManager _downstreamMgr) {
        downstreamMgr = _downstreamMgr;
    }

    public void validateToken(String _token) throws InvalidTokenException, ExpiredTokenException {
        if (_token == null) {
            throw new InvalidTokenException("null tokens do not work");
        }
        synchronized (clusterCfgLock) {
            ProvisionToken pt = tokens.get(_token);
            if (pt == null) {
                throw new InvalidTokenException("unknown token");
            }
            if (isExpired(pt)) {
                throw new ExpiredTokenException("token expired");
            }
        }
    }

    public static boolean isExpired(ProvisionToken _pt) {
        return _pt.expirationTimestamp != ProvisionToken.NEVER_EXPIRES &&
                _pt.expirationTimestamp < System.currentTimeMillis();
    }

    public List<ProvisionToken> getProvionTokens() {
        synchronized (clusterCfgLock) {
            return new ArrayList<>(tokens.values());
        }
    }

    public Cluster lookup(SensorPlatform _sensor) {
        Cluster cluster = findClusterByToken(_sensor.getProvisionToken());
        if (cluster == null) {
            cluster = findClusterByDeviceId(_sensor.getDeviceId());
        }
        return cluster;
    }

    public void alignSensor(SensorPlatform _sensor) {
        log.debug("aligning {}", _sensor.getDeviceId());
        Cluster cluster = lookup(_sensor);
        if (cluster == null) {
            log.debug("no cluster found for {}", _sensor.getDeviceId());
            return;
        }
        // look up by groups
        if (cluster.facility_id != null && !cluster.facility_id.equals(_sensor.getFacilityId())) {
            _sensor.setFacilityId(cluster.facility_id);
        }
        if (cluster.personality != _sensor.getPersonality()) {
            _sensor.setPersonality(cluster.personality);
        }
        if (cluster.aliases != null) {
            _sensor.setAliases(cluster.aliases);
        }
    }

    public Cluster getCluster(String _clusterId) {
        synchronized (clusterCfgLock) {
            if (_clusterId == null || clusterCfg == null) {
                return null;
            }
            for (Cluster c : clusterCfg.clusters) {
                if (_clusterId.equals(c.id)) {
                    return c;
                }
            }
            return null;
        }
    }

    public Cluster findClusterByToken(String _token) {
        synchronized (clusterCfgLock) {
            if (_token == null || clusterCfg == null) {
                return null;
            }
            for (Cluster c : clusterCfg.clusters) {
                for (ProvisionToken pt : c.tokens) {
                    if (pt.token.equals(_token)) {
                        return c;
                    }
                }
            }
            return null;
        }
    }

    public Cluster findClusterByDeviceId(String _deviceId) {
        synchronized (clusterCfgLock) {
            if (_deviceId == null || clusterCfg == null) {
                return null;
            }
            for (Cluster c : clusterCfg.clusters) {
                for (List<String> groups : c.sensor_groups) {
                    for (String sensorId : groups) {
                        log.debug("sensorId {} deviceId {}", sensorId, _deviceId);

                        if (sensorId.equalsIgnoreCase(_deviceId)) {
                            return c;
                        }
                    }
                }
            }
            return null;
        }
    }


    public void generateFromConfig(Collection<ClusterRunner> _runners) {
        if (sensorMgr == null) {
            log.warn("missing sensor manager");
            return;
        }
        synchronized (clusterCfgLock) {

            if (clusterCfg == null) {
                log.warn("missing cluster configuration");
                return;
            }

            for (Cluster cluster : clusterCfg.clusters) {
                Behavior behavior = getBehavior(cluster.behavior_id);
                if (behavior == null) {
                    log.warn("cluster: {} unknown behavior: {} ", cluster.id, cluster.behavior_id);
                    continue;
                }
                _runners.add(new ClusterRunner(this, cluster.id, behavior));
            }
        }
    }

    protected void getSensorGroupsForCluster(Cluster _cluster, List<List<SensorPlatform>> _groups) {

        for (ProvisionToken pt : _cluster.tokens) {
            ArrayList<SensorPlatform> sensors = new ArrayList<>();
            sensorMgr.groupForProvisioningToken(pt.token, sensors);
            _groups.add(sensors);
        }

        for (Collection<String> devices : _cluster.sensor_groups) {
            ArrayList<SensorPlatform> sensors = new ArrayList<>();
            sensorMgr.groupForIds(devices, sensors);
            _groups.add(sensors);
        }
    }

    private Behavior getBehavior(String _id) {
        try {
            return BehaviorConfig.getBehavior(_id);
        } catch (IOException _e) {
            log.error("Unable to locate behavior {}", _id);
        }
        return null;
    }

    public static final String CLUSTER_ID_ALL_SEQ = "cluster_all_seq";

    public void getSensorGroups(String _clusterId, List<List<SensorPlatform>> _sensorGroups) {

        if (CLUSTER_ID_ALL_SEQ.equals(_clusterId)) {
            List<SensorPlatform> sensors = new ArrayList<>();
            sensorMgr.getSensors(sensors);
            for (SensorPlatform sensor : sensors) {
                ArrayList<SensorPlatform> sensorGroup = new ArrayList<>();
                sensorGroup.add(sensor);
                _sensorGroups.add(sensorGroup);
            }
        } else {
            synchronized (clusterCfgLock) {
                if (clusterCfg == null) {
                    log.warn("missing cluster configuration");
                    return;
                }

                for (Cluster cluster : clusterCfg.clusters) {
                    if (cluster.id.equals(_clusterId)) {
                        getSensorGroupsForCluster(cluster, _sensorGroups);
                        break;
                    }
                }
            }
        }
    }

    public void generateClusterPerSensor(List<ClusterRunner> _runners, String _behaviorId) {
        // this should not happen
        if (sensorMgr == null) { return; }
        Behavior behavior = getBehavior(_behaviorId);
        if (behavior == null) { return; }
        List<SensorPlatform> sensors = new ArrayList<>();
        sensorMgr.getSensors(sensors);
        for (SensorPlatform sensor : sensors) {
            _runners.add(new ClusterRunner(this, sensor, behavior));
        }
    }

    public void generateSequenceCluster(List<ClusterRunner> _runners, String _behaviorId) {
        // this should not happen
        if (sensorMgr == null) { return; }
        Behavior behavior = getBehavior(_behaviorId);
        if (behavior == null) { return; }

        _runners.add(new ClusterRunner(this,
                                       CLUSTER_ID_ALL_SEQ,
                                       behavior));
    }


    private void restore() throws IOException, ConfigException {
        synchronized (clusterCfgLock) {
            if (Files.exists(CACHE_PATH)) {
                try (InputStream fis = Files.newInputStream(CACHE_PATH)) {
                    clusterCfg = mapper.readValue(fis, ClusterConfig.class);
                    validate(clusterCfg);
                    updateTokenMap();
                }
            }
        }
    }

    // assumption that appropriate synchronization occurs before calling
    private void updateTokenMap() {
        tokens.clear();
        if (clusterCfg == null) { return; }
        for (Cluster c : clusterCfg.clusters) {
            for (ProvisionToken pt : c.tokens) {
                tokens.put(pt.token, pt);
            }
        }
    }

    private void persist() {
        synchronized (clusterCfgLock) {
            if (clusterCfg == null) {
                try {
                    Files.deleteIfExists(CACHE_PATH);
                } catch (IOException _e) {
                    log.error("failed deleting: {} : ", CACHE_PATH.toAbsolutePath(), _e);
                }
                return;
            }

            try (OutputStream os = Files.newOutputStream(CACHE_PATH)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(os, clusterCfg);
                log.info("wrote {}", CACHE_PATH);
            } catch (IOException e) {
                log.error("failed persisting {}", e.getMessage());
            }
        }
    }

    private ClusterConfig fromFile(Path _path) throws IOException {
        try (InputStream fis = Files.newInputStream(_path)) {
            return mapper.readValue(fis, ClusterConfig.class);
        }
    }

    public static final String VAL_ERR_NULL_CFG = "null cluster configuration";
    public static final String VAL_ERR_MISSING_CLUSTERS = "missing clusters";

    public static void validate(ClusterConfig _cfg) throws ConfigException {
        if (_cfg == null) {
            throw new ConfigException(VAL_ERR_NULL_CFG);
        }

        if (_cfg.clusters == null || _cfg.clusters.isEmpty()) {
            throw new ConfigException(VAL_ERR_MISSING_CLUSTERS);
        }

        List<String> badBehaviors = new ArrayList<>();
        for (Cluster c : _cfg.clusters) {
            try {
                BehaviorConfig.getBehavior(c.behavior_id);
            } catch (IOException _e) {
                badBehaviors.add(c.behavior_id);
            }
        }

        if (!badBehaviors.isEmpty()) {
            throw new ConfigException("Bad cluster behaviors " + Arrays.toString(badBehaviors.toArray()));
        }
    }


    public void show(PrettyPrinter _out) {
        synchronized (clusterCfgLock) {
            if (clusterCfg == null) {
                _out.line("missing cluster configuration");
                return;
            }
            try {
                _out.line("clusterCfg:");
                mapper.writerWithDefaultPrettyPrinter().writeValue(_out, clusterCfg);
            } catch (IOException _e) {
                log.error("err {}", _e.getLocalizedMessage());
            }
        }
        _out.blank();
    }

    public void showTokens(PrettyPrinter _out) {
        _out.line("tokens:");
        synchronized (clusterCfgLock) {
            try {
                for (ProvisionToken pt : tokens.values()) {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(_out, pt);
                    _out.endln();
                }
            } catch (IOException _e) {
                log.error("err {}", _e.getLocalizedMessage());
            }
        }
        _out.blank();
    }

    public void exportTokens(PrettyPrinter _out) {
        synchronized (clusterCfgLock) {
            // create a unique file name for each token, the pt.username is not guaranteed to be unique
            for (ProvisionToken pt : tokens.values()) {
                try {
                    String fileName = "token_" + pt.username + ".json";
                    Path outPath = Env.resolveTokenPath(fileName);
                    try (OutputStream os = Files.newOutputStream(outPath)) {
                        mapper.writerWithDefaultPrettyPrinter().writeValue(os, pt);
                        _out.println("exported: " + outPath.toAbsolutePath().toString());
                    }
                } catch (IOException e) {
                    _out.error("error: " + e.getMessage());
                }
            }
        }
    }
}
