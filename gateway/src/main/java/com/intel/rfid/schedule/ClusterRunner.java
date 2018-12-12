/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.helpers.ListLooper;
import com.intel.rfid.sensor.ResponseHandler;
import com.intel.rfid.sensor.SensorGroup;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClusterRunner implements Runnable {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected ScheduleCluster cluster;
    protected final CountDownLatch completedLatch = new CountDownLatch(1);

    public ClusterRunner(ScheduleCluster _cluster) {
        cluster = _cluster;
    }

    protected final Map<SensorPlatform, CountDownLatch> latches = new HashMap<>();

    public boolean checkIfFinished(long _waitMillis) throws InterruptedException {
        return completedLatch.await(_waitMillis, TimeUnit.MILLISECONDS);
    }

    private void pause() {
        try {
            log.info("pausing cluster 2 seconds");
            Thread.sleep(2000);
        } catch (InterruptedException _e) {
            Thread.currentThread().interrupt();
        }
    }

    public void run() {

        if (cluster.sensorGroups.size() == 0) {
            pause();
            completedLatch.countDown();
            return;
        }

        ListLooper<SensorGroup> groupLooper = new ListLooper<>(cluster.sensorGroups);

        boolean anySensorStarted = false;
        while (!Thread.currentThread().isInterrupted()) {

            SensorGroup sensorGroup = groupLooper.next();
            startSensors(sensorGroup);

            if (latches.isEmpty()) {

                if (groupLooper.listLooped() && !anySensorStarted) {
                    pause();
                }
                continue;

            } else {
                anySensorStarted = true;
            }

            waitOnSensors();

            if (groupLooper.listLooped()) { anySensorStarted = false; }

        }

        completedLatch.countDown();

    }

    protected void startSensors(SensorGroup _sensorGroup) {
        latches.clear();

        // get the RSPs started with the behavior
        for (SensorPlatform sensor : _sensorGroup.sensors) {
            CountDownLatch latch = new CountDownLatch(1);
            ResponseHandler handler = sensor.startReading(cluster.behavior, latch);
            if (!handler.isError()) {
                latches.put(sensor, latch);
            } else {
                // this will end up getting logged often if an RSP goes offline
                // probably can be removed once the flow is debugged
                log.debug("{} {}", sensor.getDeviceId(), handler.getError());
            }
        }
    }

    protected void waitOnSensors() {

        // manage the interrupted state for both loops
        boolean wasInterrupted = false;

        // for those that started, wait for inventory complete (with timeout)
        // but accomodate interruptions
        int waitTime = BehaviorConfig.getWaitTimeout(cluster.behavior);
        log.debug("waitTime: " + waitTime);

        Iterator<Map.Entry<SensorPlatform, CountDownLatch>> iter;

        try {
            iter = latches.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<SensorPlatform, CountDownLatch> entry = iter.next();
                CountDownLatch latch = entry.getValue();
                if (latch.await(waitTime, TimeUnit.MILLISECONDS)) {
                    iter.remove();
                } else {
                    // this is a timeout condition
                    // the others should be done already
                    waitTime = 0;
                }
            }
        } catch (InterruptedException e) {
            log.info("interrupted waiting on inventory complete");
            wasInterrupted = true;
        }

        // explicitly stop any laggards
        for (SensorPlatform rsp : latches.keySet()) {
            log.debug("{} explicitly stopping", rsp.getDeviceId());
            rsp.stopReading();
        }

        // wait short time to confirm the stops (inventory_complete)
        iter = latches.entrySet().iterator();
        waitTime = 5000;
        try {
            while (iter.hasNext()) {
                Map.Entry<SensorPlatform, CountDownLatch> entry = iter.next();
                SensorPlatform rsp = entry.getKey();
                CountDownLatch latch = entry.getValue();
                log.debug("{} waiting on explicit stop", rsp.getDeviceId());
                if (latch.await(waitTime, TimeUnit.MILLISECONDS)) {
                    log.debug("{} confirmed explicit stop", rsp.getDeviceId());
                    iter.remove();
                } else {
                    log.warn("{} timed out waiting for explicit stop", rsp.getDeviceId());
                    waitTime = 0;
                }
            }
        } catch (InterruptedException e) {
            log.info("interrupted waiting on forced stop inventory complete");
            wasInterrupted = true;
        }

        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }


}
