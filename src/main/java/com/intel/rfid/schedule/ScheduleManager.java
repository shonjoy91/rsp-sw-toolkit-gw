/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.Behavior;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.helpers.Publisher;
import com.intel.rfid.sensor.ConnectionState;
import com.intel.rfid.sensor.ConnectionStateEvent;
import com.intel.rfid.sensor.SensorGroup;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.intel.rfid.schedule.ScheduleManager.RunState.ALL_SEQUENCED;
import static com.intel.rfid.schedule.ScheduleManager.RunState.FROM_CONFIG;
import static com.intel.rfid.schedule.ScheduleManager.RunState.INACTIVE;

public class ScheduleManager
    implements SensorManager.ConnectionStateListener {

    public enum RunState {INACTIVE, ALL_ON, ALL_SEQUENCED, FROM_CONFIG}

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected final Object stateLock = new Object();
    protected RunState runState = INACTIVE;

    protected final Object executorLock = new Object();
    protected ExecutorService runStatePubExec = ExecutorUtils.newSingleThreadedEventExecutor(log);
    protected Publisher<RunStateListener> runStatePublisher = new Publisher<>(executorLock, runStatePubExec);

    public static final String BEHAVIOR_ID_ALL_ON = "DefaultAllOn";
    public static final String BEHAVIOR_ID_ALL_SEQ = "DefaultAllSeq";
    protected Behavior behaviorAllOn = new Behavior();
    protected Behavior behaviorAllSeq = new Behavior();

    protected SensorManager sensorMgr;
    protected ScheduleConfiguration scheduleCfg;

    protected final Object clusterLock = new Object();
    protected final List<ScheduleCluster> clusters = new ArrayList<>();
    protected final ExecutorService clusterExec = Executors.newCachedThreadPool();
    protected final Collection<ClusterRunner> clusterRunners = new HashSet<>();
    protected final Collection<Future<?>> clusterFutures = new ArrayList<>();


    public static class CacheState {
        public RunState runState;
        public ScheduleConfiguration scheduleCfg;
    }

    public interface RunStateListener {
        void onScheduleRunState(RunState _current);
    }

    public void addRunStateListener(RunStateListener _l) {
        runStatePublisher.subscribe(_l);
    }

    public void removeRunStateListener(RunStateListener _l) {
        runStatePublisher.unsubscribe(_l);
    }

    public ScheduleManager(SensorManager _sensorMgr) {
        sensorMgr = _sensorMgr;
    }


    public boolean start() {

        synchronized (stateLock) {
            if (runState != INACTIVE) {
                log.warn("already started");
                return true;
            }
        }

        synchronized (executorLock) {
            runStatePubExec = ExecutorUtils.ensureValidParallel(runStatePubExec);
            runStatePublisher.replaceExecutor(runStatePubExec);
        }

        try {
            behaviorAllOn = BehaviorConfig.getBehavior(BEHAVIOR_ID_ALL_ON);
        } catch (IOException _e) {
            log.warn("unable to get behavior {}", BEHAVIOR_ID_ALL_ON);
        }

        try {
            behaviorAllSeq = BehaviorConfig.getBehavior(BEHAVIOR_ID_ALL_SEQ);
        } catch (IOException _e) {
            log.warn("unable to get behavior {}", BEHAVIOR_ID_ALL_SEQ);
        }


        try (InputStream fis = Files.newInputStream(CACHE_PATH)) {

            CacheState cacheState = mapper.readValue(fis, CacheState.class);
            log.info("Restored {}", CACHE_PATH);
            if (cacheState.runState == null) {
                log.info("cached runState is null");
                return true;
            }
            if (cacheState.runState == INACTIVE) {
                log.info("cached runState is INACTIVE");
                return true;
            }

            if (cacheState.runState == FROM_CONFIG) {
                realizeScheduleClusters(cacheState.scheduleCfg);
            }
            scheduleCfg = cacheState.scheduleCfg;
            tryChangeState(cacheState.runState);
            log.info("ScheduleManager started");
            return true;

        } catch (ConfigException _e) {
            log.error("Failed to restore {}", CACHE_PATH, _e);
        } catch (FileNotFoundException _e) {
            // this is a don't care really, but most code quality tools
            // grip about empty catch blocks
            log.info("cache file does not yet exist");
        } catch (IOException _e) {
            log.error("Failed to restore {}", CACHE_PATH, _e);
        }

        return true;
    }

    public boolean stop() {
        // save the state before stopping so it will be able to restart
        // correctly if gateway is going down (and back up)
        persistState();
        deactivate();
        synchronized (executorLock) {
            runStatePublisher.clearSubscribers();
            try {
                ExecutorUtils.shutdownExecutor(log, runStatePubExec);
            } catch (InterruptedException _e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("ScheduleManager stopped");
        return true;
    }

    public void deactivate() {
        tryChangeState(INACTIVE);
    }

    public void activate(RunState _runState, Path _scheduleConfigPath) throws IOException, ConfigException {

        if (_runState == FROM_CONFIG) {
            try (InputStream fis = Files.newInputStream(_scheduleConfigPath)) {
                ScheduleConfiguration tmpSchedCfg = mapper.readValue(fis, ScheduleConfiguration.class);
                realizeScheduleClusters(tmpSchedCfg);
                // and can be applied to the instance variables
                scheduleCfg = tmpSchedCfg;
            }
        }
        tryChangeState(_runState);
    }

    protected void realizeScheduleClusters(ScheduleConfiguration _scheduleCfg) throws ConfigException {
        List<ScheduleCluster> tmpClusters = new ArrayList<>();
        for (ScheduleConfiguration.Cluster cfgCluster : _scheduleCfg.clusters) {
            tmpClusters.add(new ScheduleCluster(cfgCluster, sensorMgr));
        }
        // at this point, if no exception has been thrown, then the configuration is good
        synchronized (clusterLock) {
            clusters.clear();
            clusters.addAll(tmpClusters);
            clusters.forEach(ScheduleCluster::alignSensorConfiguration);
        }
    }

    @Override
    public void onConnectionStateChange(ConnectionStateEvent _cse) {

        // don't care
        if (_cse.current != ConnectionState.CONNECTED) { return; }

        // don't care
        synchronized (stateLock) {
            if (runState == INACTIVE || runState == FROM_CONFIG) { return; }
        }

        // already included
        synchronized (clusterLock) {
            for (ScheduleCluster sc : clusters) {
                if (sc.contains(_cse.rsp)) {
                    return;
                }
            }
        }

        // trigger a reconfiguration
        tryChangeState(runState);
    }

    public void show(PrettyPrinter _out) {
        synchronized (stateLock) {
            _out.line("runState: " + runState);
            if (runState == FROM_CONFIG && scheduleCfg != null) {
                _out.divider();
                try {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(_out, scheduleCfg);
                } catch (IOException _e) {
                    _out.line("!!! error printing the schedule configuration !!!");
                    _out.line(_e.toString());
                }

                _out.divider();
            }
            _out.blank();
        }
    }

    protected void tryChangeState(RunState _runState) {
        try {
            changeRunState(_runState);
        } catch (GatewayException _gwe) {
            log.error(_gwe.getLocalizedMessage());
        }
    }

    protected void changeRunState(RunState _next) throws GatewayException {
        // calling this method with a mode that is currently running will trigger a "reset" of that mode
        boolean actuallyChanged = false;

        synchronized (stateLock) {

            log.info("changing RunState from {} to {}...", runState, _next);

            // handle exiting the current state
            switch (runState) {
                case ALL_ON:
                case ALL_SEQUENCED:
                case FROM_CONFIG:
                    stopClusters();
                    break;
                default:
            }

            // generate clusters as needed
            switch (_next) {
                case ALL_ON:
                    generateAllOnClusters();
                    break;
                case ALL_SEQUENCED:
                    generateAllSequencedClusters();
                    break;
                // NOTE: the FROM_CONFIG isn't handled here
                // because generating the clusters is useful for
                // validating configuration so the clusters are 
                // already available
            }

            // state entry
            switch (_next) {
                case ALL_ON:
                case ALL_SEQUENCED:
                case FROM_CONFIG:
                    startClusters();
                    break;
            }

            if (runState != _next) {
                runState = _next;
                actuallyChanged = true;
            }
        }

        if (actuallyChanged) {
            // notify listeners only on a change from one state to another
            runStatePublisher.notifyListeners(l -> l.onScheduleRunState(runState));
        }
    }

    /**
     * generates one cluster per sensor
     */
    public void generateAllOnClusters() {

        synchronized (clusterLock) {
            clusters.clear();

            for (SensorPlatform sensor : sensorMgr.getRSPsCopy()) {
                SensorGroup sensorGroup = new SensorGroup();
                sensorGroup.sensors.add(sensor);
                List<SensorGroup> sensorGroups = new ArrayList<>();
                sensorGroups.add(sensorGroup);
                ScheduleCluster sc = new ScheduleCluster(sensor.getDeviceId(),
                                                         null,
                                                         behaviorAllOn,
                                                         sensorGroups);
                clusters.add(sc);
            }
        }
    }

    public void generateAllSequencedClusters() {

        synchronized (clusterLock) {
            clusters.clear();

            List<SensorGroup> sensorGroups = new ArrayList<>();
            for (SensorPlatform sensor : sensorMgr.getRSPsCopy()) {
                // sensor should be in the correct state.
                SensorGroup sensorGroup = new SensorGroup();
                sensorGroup.sensors.add(sensor);
                sensorGroups.add(sensorGroup);
            }
            ScheduleCluster sc = new ScheduleCluster(ALL_SEQUENCED.toString(),
                                                     null,
                                                     behaviorAllSeq,
                                                     sensorGroups);
            clusters.add(sc);
        }
    }

    protected void startClusters() {
        synchronized (clusterLock) {

            log.info("Starting {} clusters", clusters.size());

            if (!clusterFutures.isEmpty()) {
                log.warn("refusing to start clusters due to improper state -- " +
                         "cluster futures have not been cleared");
                return;
            }

            if (!clusterRunners.isEmpty()) {
                log.warn("refusing to start clusters due to improper state -- " +
                         "previously running clusters have not been cleared");
                return;
            }


            for (ScheduleCluster cluster : clusters) {
                ClusterRunner runner = new ClusterRunner(cluster);
                clusterRunners.add(runner);
                clusterFutures.add(clusterExec.submit(runner));
            }
        }
    }

    protected void stopClusters() {
        
        boolean interrupted = false;
        synchronized (clusterLock) {

            log.info("Stopping {} clusters", clusterFutures.size());

            clusterFutures.forEach(f -> f.cancel(true));
            clusterFutures.clear();
            Thread.yield();

            // wait until all sensors have stopped reading
            boolean keepTryingToStop = true;
            while (keepTryingToStop) {
                try {
                    keepTryingToStop = !awaitClustersFinished();
                } catch (InterruptedException _e) {
                    interrupted = true;
                    keepTryingToStop = false;
                }
            }
            clusterRunners.clear();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
    
    private boolean awaitClustersFinished() throws InterruptedException {
        // wait until all sensors have stopped reading
        boolean allCompleted = true;
        for (ClusterRunner runner : clusterRunners) {
                if (!runner.checkIfFinished(250)) {
                    allCompleted = false;
                }
        }
        return allCompleted;
    }

    public static final Path CACHE_PATH = Env.resolveCache("schedule_manager.json");
    protected ObjectMapper mapper = Jackson.getMapper();

    private void persistState() {
        CacheState cacheState = new CacheState();
        synchronized (stateLock) {
            cacheState.runState = runState;
            cacheState.scheduleCfg = scheduleCfg;
        }
        try (OutputStream os = Files.newOutputStream(CACHE_PATH)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(os, cacheState);
            log.info("wrote {}", CACHE_PATH);
        } catch (IOException e) {
            log.error("failed persisting schedule manager {}", e.getMessage());
        }
    }

}
