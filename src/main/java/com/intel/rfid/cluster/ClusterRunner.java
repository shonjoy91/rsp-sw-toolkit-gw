/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.cluster;

import com.intel.rfid.api.data.Behavior;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.helpers.ListLooper;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ClusterRunner implements Runnable {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected final ClusterManager clusterMgr;
    protected final String clusterId;
    protected final Behavior behavior;
    public final List<List<SensorPlatform>> sensorGroups;
    protected final AtomicBoolean updateSensorsFlag;

    // A runner MUST be allowed to finish cleanly to ensure
    // that a sensor actually stops reading.
    protected final CountDownLatch completedLatch;

    // public ClusterRunner(ScheduleCluster _cluster) {
    //     cluster = _cluster;
    //     completedLatch = new CountDownLatch(1);
    // }

    public ClusterRunner(ClusterManager _clusterMgr,
                         String _clusterId,
                         Behavior _behavior) {

        completedLatch = new CountDownLatch(1);

        updateSensorsFlag = new AtomicBoolean(true);

        clusterMgr = _clusterMgr;
        clusterId = _clusterId;
        behavior = _behavior;
        sensorGroups = new ArrayList<>();
    }

    // constructor to support easily creating a single group of a single sensor
    public ClusterRunner(ClusterManager _clusterMgr,
                         SensorPlatform _sensor,
                         Behavior _behavior) {
        this(_clusterMgr, _sensor.getDeviceId(), _behavior);
        List<SensorPlatform> sensors = new ArrayList<>(1);
        sensors.add(_sensor);
        sensorGroups.add(sensors);
        updateSensorsFlag.set(false);
    }

    public String getClusterId() {
        return clusterId;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public boolean knowsOf(SensorPlatform _sensor) {
        synchronized (sensorGroups) {
            for (List<SensorPlatform> sensors : sensorGroups) {
                if (sensors.contains(_sensor)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void notifyNewSensor() {
        updateSensorsFlag.set(true);
    }

    protected void updateSensorGroups() {
        log.debug("updating sensor groups for cluster {}", clusterId);
        synchronized (sensorGroups) {
            updateSensorsFlag.set(false);
            sensorGroups.clear();
            clusterMgr.getSensorGroups(clusterId, sensorGroups);
        }
    }

    public void awaitFinish() {
        try {
            completedLatch.await();
        } catch (InterruptedException _e) {
            log.debug("Cluster[{}]: interrupted while awaitFinish", clusterId);
            Thread.currentThread().interrupt();
        }
    }

    private void pause() {
        try {
            log.debug("pausing cluster {} 2 seconds", clusterId);
            Thread.sleep(2000);
        } catch (InterruptedException _e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {

        // the looper will initally be empty by design, and trigger a sensor alignment
        // but any sensor alignment might result in an empty set of sensors, so the run loop
        // always has to handle the potential for an empty group looper anywya
        ListLooper<List<SensorPlatform>> groupLooper = new ListLooper<>(sensorGroups);

        boolean anySensorStarted = false;
        int behaviorWaitTimetout = BehaviorConfig.getWaitTimeout(behavior);
        AtomicBoolean waitInterrupted = new AtomicBoolean(false);

        while (!Thread.currentThread().isInterrupted()) {

            if (groupLooper.isEmpty() || updateSensorsFlag.get()) {
                pause();
                updateSensorGroups();
                groupLooper = new ListLooper<>(sensorGroups);
                // no guarantee that alignSensors will actually return sensors
                // especially for cluster configuration with provisioning tokens
                continue;
            }

            if (groupLooper.listLooped()) {
                anySensorStarted = false;
            }

            List<SensorPlatform> sensors = groupLooper.next();

            log.debug("Cluster[{}]: starting readers", clusterId);
            List<CompletableFuture<Boolean>> readerFutures = startSensors(sensors, behavior);

            if (readerFutures.isEmpty()) {
                if (groupLooper.listLooped() && !anySensorStarted) {
                    pause();
                }
                continue;
            } else {
                anySensorStarted = true;
            }

            waitInterrupted.set(false);
            log.debug("Cluster[{}]: awaiting readers completion", clusterId);
            waitForComplete(readerFutures,
                            behaviorWaitTimetout,
                            TimeUnit.MILLISECONDS,
                            waitInterrupted);

            log.debug("Cluster[{}]: stopping laggards", clusterId);
            stopLaggards(sensors);

            log.debug("Cluster[{}]: awaiting laggards completion", clusterId);
            waitForComplete(readerFutures,
                            5000,
                            TimeUnit.MILLISECONDS,
                            waitInterrupted);

            log.debug("Cluster[{}]: canceling all futures", clusterId);
            for (CompletableFuture<?> readerFuture : readerFutures) {
                readerFuture.cancel(false);
            }

            if (waitInterrupted.get()) {
                log.debug("Cluster[{}]: passing on thread interrupt", clusterId);
                Thread.currentThread().interrupt();
            }
        }

        log.debug("Cluster[{}]: finished", clusterId);
        completedLatch.countDown();

    }

    private List<CompletableFuture<Boolean>> startSensors(Collection<SensorPlatform> _sensors,
                                                          Behavior _behavior) {
        return _sensors.stream()
                       .filter(SensorPlatform::isConnected)
                       .map(rsp -> rsp.startScanAsync(_behavior))
                       .collect(Collectors.toList());
    }

    private void stopLaggards(Collection<SensorPlatform> _sensors) {
        _sensors.stream()
                .filter(SensorPlatform::isPotentiallyReading)
                .forEach(SensorPlatform::stopReading);
    }


    private void waitForComplete(Collection<CompletableFuture<Boolean>> _readerFutures,
                                 long _waitTime,
                                 TimeUnit _timeUnit,
                                 AtomicBoolean _waitInterrupted) {
        try {

            CompletableFuture
                .allOf(_readerFutures.toArray(new CompletableFuture[_readerFutures.size()]))
                .get(_waitTime, _timeUnit);

            // check results to see if a reader actually started
            log.debug("Cluster[{}]: ...done", clusterId);
        } catch (TimeoutException e) {
            log.warn("Cluster[{}]: ...timed out waiting for readers to complete", clusterId);
        } catch (CancellationException e) {
            log.warn("Cluster[{}]: ...sensor reads canceled while waiting", clusterId);
        } catch (ExecutionException e) {
            log.warn("Cluster[{}]: ...exception while waiting on sensors", clusterId, e);
        } catch (InterruptedException e) {
            log.warn("Cluster[{}]: ...interrupted while waiting on sensors", clusterId);
            _waitInterrupted.set(true);
        }
    }

}
