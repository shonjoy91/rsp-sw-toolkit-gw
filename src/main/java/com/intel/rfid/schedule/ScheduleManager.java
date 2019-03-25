/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.SchedulerSummary;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.exception.ConfigException;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.intel.rfid.schedule.ScheduleManager.RunState.ALL_SEQUENCED;
import static com.intel.rfid.schedule.ScheduleManager.RunState.INACTIVE;

public class ScheduleManager
    implements SensorManager.ConnectionStateListener {

    public enum RunState {INACTIVE, ALL_ON, ALL_SEQUENCED, FROM_CONFIG}

    public static final RunState DEFAULT_RUN_STATE = ALL_SEQUENCED;

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected volatile RunState runState = INACTIVE;

    protected final Object executorLock = new Object();
    protected ExecutorService runStatePubExec = ExecutorUtils.newSingleThreadedEventExecutor(log);
    protected Publisher<RunStateListener> runStatePublisher = new Publisher<>(executorLock, runStatePubExec);

    public static final String BEHAVIOR_ID_ALL_ON = "ClusterAllOn_PORTS_1";
    public static final String BEHAVIOR_ID_ALL_SEQ = "ClusterAllSeq_PORTS_1";

    protected final List<ScheduleCluster> clusters = new ArrayList<>();
    protected final ExecutorService clusterExec = Executors.newCachedThreadPool();
    protected final Collection<ClusterRunner> clusterRunners = new ArrayList<>();
    protected final Collection<Future<?>> clusterFutures = new ArrayList<>();
    protected final ClusterManager clusterMgr;


    public static class CacheState {
        public RunState runState;
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

    public ScheduleManager(ClusterManager _clusterMgr) {
        clusterMgr = _clusterMgr;
    }


    public boolean start() {

        if (runState != INACTIVE) {
            log.warn("already started");
            return true;
        }

        synchronized (executorLock) {
            runStatePubExec = ExecutorUtils.ensureValidParallel(runStatePubExec);
            runStatePublisher.replaceExecutor(runStatePubExec);
        }

        RunState startupRunState = DEFAULT_RUN_STATE;
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

        } catch (FileNotFoundException _e) {
            // this is a don't care really, but most code quality tools
            // gripe about empty catch blocks
            log.debug("cache file does not yet exist");
        } catch (IOException _e) {
            log.error("Failed to restore {}", CACHE_PATH, _e);
        }

        changeRunState(startupRunState);
        log.info("ScheduleManager started");
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
        changeRunState(INACTIVE);
    }

    public void activate(RunState _runState) throws IOException, ConfigException {
        changeRunState(_runState);
    }

    @Override
    public void onConnectionStateChange(ConnectionStateEvent _cse) {

        // don't care
        if (_cse.current != ConnectionState.CONNECTED) { return; }

        // don't care
        if (runState == INACTIVE) { return; }

        // already included
        synchronized (clusters) {
            for (ScheduleCluster sc : clusters) {
                if (sc.contains(_cse.rsp)) {
                    return;
                }
            }
        }

        // This has to be on a different thread
        clusterExec.submit(() -> changeRunState(runState));
    }

    public SchedulerSummary getSummary() {
        SchedulerSummary summary = new SchedulerSummary();
        summary.run_state = runState;
        synchronized (clusters) {
            for (ScheduleCluster sc : clusters) {
                summary.clusters.add(sc.asCluster());
            }
        }
        return summary;
    }

    public void show(PrettyPrinter _out) {
        List<ScheduleCluster> clustersCopy = new ArrayList<>();
        synchronized (clusters) {
            clustersCopy.addAll(clusters);
        }

        _out.line("runState: " + runState);
        _out.divider();
        _out.line("clusters:");
        for (ScheduleCluster cluster : clustersCopy) {
            _out.line("      id: " + cluster.id);
            _out.line("behavior: " + cluster.behavior.id);
            for (SensorGroup group : cluster.sensorGroups) {
                _out.chunk("sensors: [");
                for (SensorPlatform sensor : group.sensors) {
                    _out.chunk(sensor.getDeviceId() + " ");
                }
                _out.endln("]");
            }
            _out.endln();
            _out.divider();
        }
        _out.blank();
    }

    protected synchronized void changeRunState(RunState _next)  {
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
        synchronized (clusters) {
            clusters.clear();
            switch (_next) {
                case ALL_ON:
                    clusterMgr.generateClusterPerSensor(clusters, BEHAVIOR_ID_ALL_ON);
                    break;
                case ALL_SEQUENCED:
                    clusterMgr.generateSequenceCluster(clusters, BEHAVIOR_ID_ALL_SEQ);
                    break;
                case FROM_CONFIG:
                    clusterMgr.generateFromConfig(clusters);
                    break;
            }
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
            runStatePublisher.notifyListeners(l -> l.onScheduleRunState(runState));
        }
    }

    protected void startClusters() {
        List<ScheduleCluster> clustersCopy = new ArrayList<>();
        synchronized (clusters) {
            clustersCopy.addAll(clusters);
        }

        synchronized (clusterRunners) {
            if (!clusterRunners.isEmpty()) {
                log.warn("refusing to start clusters due to improper state -- " +
                         "cluster futures have not been cleared");
                return;
            }

            log.info("Starting {} clusters", clusters.size());

            for (ScheduleCluster cluster : clustersCopy) {
                ClusterRunner cr = new ClusterRunner(cluster);
                clusterRunners.add(cr);
                clusterFutures.add(clusterExec.submit(cr));
            }
        }
    }

    protected void stopClusters() {
        synchronized (clusterRunners) {
            log.info("Stopping {} clusters", clusterRunners.size());
            // cannot just cancel the thread without giving it time to finish
            // otherwise, it will keep running and state ??
            clusterFutures.forEach(f -> f.cancel(true));
            clusterFutures.clear();
            clusterRunners.forEach(ClusterRunner::awaitFinish);
            clusterRunners.clear();
        }
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
