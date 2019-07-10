/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.alerts.ConnectionStateEvent;
import com.intel.rfid.api.data.Cluster;
import com.intel.rfid.api.data.Connection;
import com.intel.rfid.api.data.ScheduleRunState;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.cluster.ClusterRunner;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.Publisher;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.intel.rfid.api.data.ScheduleRunState.ALL_ON;
import static com.intel.rfid.api.data.ScheduleRunState.ALL_SEQUENCED;
import static com.intel.rfid.api.data.ScheduleRunState.INACTIVE;

public class ScheduleManager
        implements SensorManager.ConnectionStateListener {

    public static final ScheduleRunState DEFAULT_RUN_STATE = ALL_SEQUENCED;

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected volatile ScheduleRunState runState = INACTIVE;

    protected final Object publisherLock = new Object();
    protected ExecutorService runStatePubExec = ExecutorUtils.newSingleThreadedEventExecutor(log);
    protected Publisher<RunStateListener> runStatePublisher = new Publisher<>(publisherLock, runStatePubExec);

    public static final String BEHAVIOR_ID_ALL_ON = "ClusterAllOn_PORTS_1";
    public static final String BEHAVIOR_ID_ALL_SEQ = "ClusterAllSeq_PORTS_1";

    protected final ExecutorService clusterExec = Executors.newCachedThreadPool();
    protected final List<ClusterRunner> clusterRunners = new ArrayList<>();
    protected final Collection<Future<?>> clusterFutures = new ArrayList<>();
    protected final ClusterManager clusterMgr;

    public static class CacheState {
        public ScheduleRunState runState;
    }

    public interface RunStateListener {
        void onScheduleRunState(ScheduleRunState _current, SchedulerSummary _summary);
    }

    public synchronized void addRunStateListener(RunStateListener _l) {
        runStatePublisher.subscribe(_l);
    }

    public synchronized void removeRunStateListener(RunStateListener _l) {
        runStatePublisher.unsubscribe(_l);
    }

    public ScheduleManager(ClusterManager _clusterMgr) {
        clusterMgr = _clusterMgr;
    }

    public synchronized boolean start() {

        if (runState != INACTIVE) {
            log.warn("already started");
            return true;
        }

        synchronized (publisherLock) {
            runStatePubExec = ExecutorUtils.ensureValidParallel(runStatePubExec);
            runStatePublisher.replaceExecutor(runStatePubExec);
        }

        ScheduleRunState startupRunState = DEFAULT_RUN_STATE;
        try (InputStream fis = Files.newInputStream(CACHE_PATH)) {

            CacheState cacheState = mapper.readValue(fis, CacheState.class);
            log.info("Restored {}", CACHE_PATH);
            if (cacheState.runState != null) {
                if (cacheState.runState == INACTIVE) {
                    log.info("cached runState is INACTIVE");
                    return true;
                }
                startupRunState = cacheState.runState;
            }

        } catch (FileNotFoundException | NoSuchFileException _e) {
            // this is a don't care really, but most code quality tools
            // gripe about empty catch blocks
            log.debug("cache file does not yet exist");
        } catch (IOException _e) {
            log.error("Failed to restore {} {}", CACHE_PATH, _e.getClass());
        }

        changeRunState(startupRunState);
        log.info("ScheduleManager started");
        return true;

    }

    public synchronized boolean stop() {
        // save the state before stopping so it will be able to restart
        // correctly if gateway is going down (and back up)
        persistState();
        changeRunState(INACTIVE);
        runStatePublisher.clearSubscribers();
        try {
            ExecutorUtils.shutdownExecutor(log, runStatePubExec);
        } catch (InterruptedException _e) {
            Thread.currentThread().interrupt();
        }
        log.info("ScheduleManager stopped");
        return true;
    }

    public synchronized void setRunState(ScheduleRunState _runState) {
        changeRunState(_runState);
    }

    public synchronized ScheduleRunState getRunState() {
        return runState;
    }

    @Override
    public synchronized void onConnectionStateChange(ConnectionStateEvent _cse) {

        // don't care
        if (runState == INACTIVE) { return; }

        if (_cse.current == Connection.State.CONNECTING) { return; }

        if (_cse.current == Connection.State.DISCONNECTED && _cse.cause == Connection.Cause.REMOVED) {
            changeRunState(runState);
            return;
        }

        // already handled at least once
        for (ClusterRunner cr : clusterRunners) {
            if (cr.knowsOf(_cse.rsp)) {
                return;
            }
        }

        // if sensor isn't included already ...
        if (runState == ALL_ON) {
            try {
                Behavior behavior = BehaviorConfig.getBehavior(BEHAVIOR_ID_ALL_ON);
                ClusterRunner runner = new ClusterRunner(clusterMgr,
                                                         _cse.rsp,
                                                         behavior);
                clusterFutures.add(clusterExec.submit(runner));
                clusterRunners.add(runner);
            } catch (IOException _e) {
                log.error("unable to build cluster runner {}", _e.getMessage());
            }
        } else if (runState == ALL_SEQUENCED) {
            // there should only be a single cluster in here, 
            // but it is not this method's responsibility to enforce that
            for (ClusterRunner runner : clusterRunners) {
                runner.notifySensorUpdate();
            }
        } else {
            Cluster cluster = clusterMgr.lookup(_cse.rsp);
            if (cluster == null) { return; }
            for (ClusterRunner runner : clusterRunners) {
                if (runner.getClusterId().equals(cluster.id)) {
                    runner.notifySensorUpdate();
                    break;
                }
            }
        }

    }

    public synchronized SchedulerSummary getSummary() {
        SchedulerSummary summary = new SchedulerSummary();
        summary.run_state = runState;
        for (ClusterRunner runner : clusterRunners) {
            Cluster c = new Cluster();
            summary.clusters.add(c);
            c.id = runner.getClusterId();
            c.behavior_id = runner.getBehavior().id;
            for (List<SensorPlatform> sensors : runner.sensorGroups) {
                List<String> sensorIds = new ArrayList<>();
                for (SensorPlatform sensor : sensors) {
                    sensorIds.add(sensor.getDeviceId());
                }
                c.sensor_groups.add(sensorIds);
            }
        }
        return summary;
    }

    protected void changeRunState(ScheduleRunState _next) {
        // calling this method with a mode that is currently running will trigger a "reset" of that mode
        boolean actuallyChanged = false;


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
        if (!clusterRunners.isEmpty() && !clusterFutures.isEmpty()) {
            log.warn("bad threading is occuring with cluster runners {} and futures {}",
                     clusterRunners.size(), clusterFutures.size());
            clusterRunners.clear();
            clusterFutures.clear();
        }

        switch (_next) {
            case ALL_ON:
                clusterMgr.generateClusterPerSensor(clusterRunners, BEHAVIOR_ID_ALL_ON);
                break;
            case ALL_SEQUENCED:
                clusterMgr.generateSequenceCluster(clusterRunners, BEHAVIOR_ID_ALL_SEQ);
                break;
            case FROM_CONFIG:
                clusterMgr.generateFromConfig(clusterRunners);
                break;
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

        if (actuallyChanged) {
            // notify listeners only on a change from one state to another
            final SchedulerSummary summary = getSummary();
            runStatePublisher.notifyListeners(l -> l.onScheduleRunState(runState, summary));
        }
    }

    protected void startClusters() {
        log.info("Starting {} clusters", clusterRunners.size());
        for (ClusterRunner runner : clusterRunners) {
            clusterFutures.add(clusterExec.submit(runner));
        }
    }

    protected void stopClusters() {
        log.info("Stopping {} clusters", clusterRunners.size());
        clusterFutures.forEach(f -> f.cancel(true));
        clusterFutures.clear();
        // cannot just cancel the thread without giving it time to finish
        // otherwise, it will keep running and state ??
        clusterRunners.forEach(ClusterRunner::awaitFinish);
        clusterRunners.clear();
    }

    public static final Path CACHE_PATH = Env.resolveCache("schedule_manager.json");
    protected ObjectMapper mapper = Jackson.getMapper();

    private void persistState() {
        CacheState cacheState = new CacheState();
        cacheState.runState = runState;
        try (OutputStream os = Files.newOutputStream(CACHE_PATH)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(os, cacheState);
            log.info("wrote {}", CACHE_PATH);
        } catch (IOException e) {
            log.error("failed persisting schedule manager {}", e.getMessage());
        }
    }

}
