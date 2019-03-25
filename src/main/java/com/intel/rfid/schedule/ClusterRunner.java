/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.intel.rfid.api.Behavior;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.helpers.ListLooper;
import com.intel.rfid.sensor.SensorGroup;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ClusterRunner implements Runnable {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected ScheduleCluster cluster;

    // A runner MUST be allowed to finish cleanly to ensure
    // that a sensor actually stops reading.
    protected final CountDownLatch completedLatch;

    public ClusterRunner(ScheduleCluster _cluster) {
        cluster = _cluster;
        completedLatch = new CountDownLatch(1);
    }

    private void pause() {
        try {
            log.debug("pausing cluster {} 2 seconds", cluster.id);
            Thread.sleep(2000);
        } catch (InterruptedException _e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void awaitFinish() {
        try {
            completedLatch.await();
        } catch(InterruptedException _e) {
            log.debug("Cluster[{}]: interrupted while awaitFinish", cluster.id);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {

        if (cluster.sensorGroups.size() == 0) {
            log.info("Cluster[{}]: no sensors available", cluster.id);
            completedLatch.countDown();
            return;
        }

        ListLooper<SensorGroup> groupLooper = new ListLooper<>(cluster.sensorGroups);
        boolean anySensorStarted = false;

        while (!Thread.currentThread().isInterrupted()) {

            if(groupLooper.listLooped()) {
                anySensorStarted = false;
            }

            SensorGroup sensorGroup = groupLooper.next();

            log.debug("Cluster[{}]: starting readers", cluster.id);
            List<CompletableFuture<Boolean>> readerFutures = startSensors(sensorGroup.sensors,
                                                                          cluster.behavior);

            if(readerFutures.isEmpty()) {
                if(groupLooper.listLooped() && !anySensorStarted) {
                    pause();
                }
                continue;
            } else {
                anySensorStarted = true;
            }

            log.debug("Cluster[{}]: awaiting readers completion", cluster.id);
            boolean readInterrupted = !waitForComplete(readerFutures,
                                                       BehaviorConfig.getWaitTimeout(cluster.behavior),
                                                       TimeUnit.MILLISECONDS);
            
            log.debug("Cluster[{}]: stopping laggards", cluster.id);
            stopLaggards(sensorGroup.sensors);

            log.debug("Cluster[{}]: awaiting laggards completion", cluster.id);
            boolean stopInterrupted = !waitForComplete(readerFutures,
                                                       5000,
                                                       TimeUnit.MILLISECONDS);

            log.debug("Cluster[{}]: canceling all futures", cluster.id);
            for (CompletableFuture<?> readerFuture : readerFutures) {
                readerFuture.cancel(false);
            }

            if (readInterrupted || stopInterrupted) {
                log.debug("Cluster[{}]: passing on thread interrupt", cluster.id);
                Thread.currentThread().interrupt();
            }
        }

        log.debug("Cluster[{}]: finished", cluster.id);
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


    private boolean waitForComplete(Collection<CompletableFuture<Boolean>> _readerFutures,
                                    long _waitTime, TimeUnit _timeUnit) {
        try {

            CompletableFuture
                .allOf(_readerFutures.toArray(new CompletableFuture[_readerFutures.size()]))
                .get(_waitTime, _timeUnit);

            // check results to see if a reader actually started
            log.debug("Cluster[{}]: ...done", cluster.id);
        } catch (TimeoutException e) {
            log.warn("Cluster[{}]: ...timed out waiting for readers to complete", cluster.id);
        } catch (CancellationException e) {
            log.warn("Cluster[{}]: ...sensor reads canceled while waiting", cluster.id);
        } catch (ExecutionException e) {
            log.warn("Cluster[{}]: ...exception while waiting on sensors", cluster.id, e);
        } catch (InterruptedException e) {
            log.warn("Cluster[{}]: ...interrupted while waiting on sensors", cluster.id);
            return false;
        }
        return true;
    }
    
}
