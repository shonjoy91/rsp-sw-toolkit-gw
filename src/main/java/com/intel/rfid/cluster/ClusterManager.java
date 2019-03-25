package com.intel.rfid.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.Behavior;
import com.intel.rfid.api.ProvisionToken;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.schedule.ScheduleCluster;
import com.intel.rfid.sensor.SensorGroup;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClusterManager {

    public static final Path CACHE_PATH = Env.resolveCache("cluster_manager.json");

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper mapper = Jackson.getMapper();

    protected SensorManager sensorMgr;
    protected final Object clusterCfgLock = new Object();
    protected ClusterConfig clusterCfg;
    protected Map<String, ProvisionToken> tokens = new HashMap<>();

    public void setSensorManager(SensorManager _sensorMgr) {
        sensorMgr = _sensorMgr;
    }

    public boolean isTokenValid(String _token) {
        synchronized (clusterCfgLock) {
            ProvisionToken pt = tokens.get(_token);
            return pt != null && !isExpired(pt);
        }
    }
    
    public static boolean isExpired(ProvisionToken _pt) {
        return _pt.expirationTimestamp != ProvisionToken.NEVER_EXPIRES &&
               _pt.expirationTimestamp > 0 &&
               _pt.expirationTimestamp < System.currentTimeMillis();
    }

    public List<ProvisionToken> getProvionTokens() {
        synchronized (clusterCfgLock) {
          return new ArrayList<>(tokens.values());
        }
    }

    public void alignSensor(SensorPlatform _sensor) {
        Cluster cluster = findClusterByToken(_sensor.getProvisionToken());
        if (cluster == null) {
            cluster = findClusterByDeviceId(_sensor.getDeviceId());
        }
        if (cluster == null) {
            return;
        }
        // look up by groups
        if (cluster.facility_id != null && !cluster.facility_id.equals(_sensor.getFacilityId())) {
            _sensor.setFacilityId(cluster.facility_id);
        }
        if (cluster.personality != _sensor.getPersonality()) {
            _sensor.setPersonality(cluster.personality);
        }
    }

    public Cluster getCluster(String _clusterId) {
        synchronized (clusterCfgLock) {
            if (_clusterId == null || clusterCfg == null) {
                return null;
            }
            for (Cluster c : clusterCfg.clusters) {
                if(_clusterId.equals(c.id)) {
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
                    for (String id : groups) {

                        if (id.equals(_deviceId)) {
                            return c;
                        }
                    }
                }
            }
            return null;
        }
    }


    public void generateFromConfig(List<ScheduleCluster> _clusters) {
        synchronized (clusterCfgLock) {

            if (sensorMgr == null || clusterCfg == null) { 
                log.error("improper state: sensorMgr or clusterCfg is null");
                return; 
            }

            for (Cluster cluster : clusterCfg.clusters) {
                Behavior behavior = getBehavior(cluster.behavior_id);
                if (behavior == null) {
                    log.warn("cluster: {} unknown behavior: {} ", cluster.id, cluster.behavior_id);
                    continue;
                }
                List<SensorGroup> groups = getSensorGroupsForCluster(cluster);
                if (groups == null) { continue; }

                _clusters.add(new ScheduleCluster(cluster.id, behavior, groups));
            }
        }
    }

    private List<SensorGroup> getSensorGroupsForCluster(Cluster _cluster) {
        List<SensorGroup> _groups = new ArrayList<>();

        for (ProvisionToken pt : _cluster.tokens) {
            _groups.add(sensorMgr.groupForProvisioningToken(pt.token));
        }

        for (Collection<String> devices : _cluster.sensor_groups) {
            _groups.add(sensorMgr.groupForIds(devices));
        }
        return _groups;
    }

    private Behavior getBehavior(String _id) {
        try {
            return BehaviorConfig.getBehavior(_id);
        } catch (IOException _e) {
            log.error("Unable to locate behavior {}", _id);
        }
        return null;
    }


    public void generateClusterPerSensor(List<ScheduleCluster> _clusters, String _behaviorId) {
        // this should not happen
        if (sensorMgr == null) { return; }
        Behavior behavior = getBehavior(_behaviorId);
        if (behavior == null) { return; }

        for (SensorPlatform sensor : sensorMgr.getRSPsCopy()) {
            SensorGroup sensorGroup = new SensorGroup();
            sensorGroup.sensors.add(sensor);
            List<SensorGroup> sensorGroups = new ArrayList<>();
            sensorGroups.add(sensorGroup);
            ScheduleCluster sc = new ScheduleCluster(sensor.getDeviceId(),
                                                     behavior,
                                                     sensorGroups);
            _clusters.add(sc);
        }
    }

    public void generateSequenceCluster(List<ScheduleCluster> _clusters, String _behaviorId) {
        // this should not happen
        if (sensorMgr == null) { return; }
        Behavior behavior = getBehavior(_behaviorId);
        if (behavior == null) { return; }

        List<SensorGroup> sensorGroups = new ArrayList<>();
        for (SensorPlatform sensor : sensorMgr.getRSPsCopy()) {
            // sensor should be in the correct state.
            SensorGroup sensorGroup = new SensorGroup();
            sensorGroup.sensors.add(sensor);
            sensorGroups.add(sensorGroup);
        }
        _clusters.add(new ScheduleCluster("Sequence",
                                          behavior,
                                          sensorGroups));
    }


    public void start() {
        try {
            restore(CACHE_PATH);
        } catch(IOException _e) {
            log.warn("failed restoring {} {}", CACHE_PATH, _e.getMessage());
        }
        log.info("started");
    }

    public void stop() {
        persist();
    }

    public void loadConfig(Path _clusterCfgPath) throws IOException {
        restore(_clusterCfgPath);
        persist();

        if (sensorMgr == null) { return; }

        for (SensorPlatform sensor : sensorMgr.getRSPsCopy()) {
            alignSensor(sensor);
        }
    }

    private void restore(Path _path) throws IOException {
        synchronized (clusterCfgLock) {
            if (Files.exists(_path)) {
                try (InputStream fis = Files.newInputStream(_path)) {
                    clusterCfg = mapper.readValue(fis, ClusterConfig.class);
                    tokens.clear();
                    for (Cluster c : clusterCfg.clusters) {
                        for (ProvisionToken pt : c.tokens) {
                            tokens.put(pt.token, pt);
                        }
                    }
                }
            }
        }
    }


    private void persist() {
        synchronized (clusterCfgLock) {
            if (clusterCfg == null) { return; }

            try (OutputStream os = Files.newOutputStream(CACHE_PATH)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(os, clusterCfg);
                log.info("wrote {}", CACHE_PATH);
            } catch (IOException e) {
                log.error("failed persisting schedule manager {}", e.getMessage());
            }
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
