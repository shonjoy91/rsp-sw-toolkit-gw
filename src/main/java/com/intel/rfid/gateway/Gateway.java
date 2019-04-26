/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gateway;

import com.intel.rfid.alerts.AlertManager;
import com.intel.rfid.api.GatewayStatusUpdate;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.console.CLICommandBuilder;
import com.intel.rfid.console.CLICommander;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.rest.EndPointManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.upstream.UpstreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Gateway implements CLICommandBuilder {

    protected Logger log = LoggerFactory.getLogger(getClass());

    // Even though there is only one gateway object used in a running
    // project, this reather elaborate construction scheme is in
    // place to support unit testing by swapping in effective test
    // objects i.e. a SensorManager that uses simulated sensor software
    public static Gateway build() {
        Gateway gw = new Gateway();
        gw.init();
        return gw;
    }

    protected ClusterManager clusterMgr;
    protected SensorManager sensorMgr;
    protected ScheduleManager scheduleMgr;
    protected InventoryManager inventoryMgr;
    protected DownstreamManager downstreamMgr;
    protected UpstreamManager upstreamMgr;
    protected EndPointManager endPointMgr;

    protected AlertManager alertMgr;

    protected Gateway() { }

    protected void init() {
        log.info("Gateway {} software initializing", Version.asString());

        if (clusterMgr == null) {
            clusterMgr = new ClusterManager();
        }

        if (sensorMgr == null) {
            sensorMgr = new SensorManager(clusterMgr);
        }
        if (inventoryMgr == null) {
            inventoryMgr = new InventoryManager();
        }
        if (downstreamMgr == null) {
            downstreamMgr = new DownstreamManager(sensorMgr);
        }
        if (scheduleMgr == null) {
            scheduleMgr = new ScheduleManager(clusterMgr);
        }
        if (upstreamMgr == null) {
            upstreamMgr = new UpstreamManager();
        }
        if (endPointMgr == null) {
            endPointMgr = new EndPointManager(sensorMgr, 
                                              inventoryMgr,
                                              upstreamMgr,
                                              downstreamMgr,
                                              scheduleMgr);
        }
        if (alertMgr == null) {
            alertMgr = new AlertManager(upstreamMgr);
        }

        // additional object relationships
        clusterMgr.setSensorManager(sensorMgr);
        clusterMgr.setScheduleManager(scheduleMgr);
        clusterMgr.setDownstreamManager(downstreamMgr);

        // hook up all the listeners

        // to apply the correct behavior to an RSP that comes online
        sensorMgr.addConnectionStateListener(scheduleMgr);

        // to check for unconfigured facilities for new RSPs coming online
        sensorMgr.addConnectionStateListener(alertMgr);

        // to forward these alerts upwards
        sensorMgr.addDeviceAlertListener(alertMgr);

        // to connect tag reads from down to up stream
        downstreamMgr.addInventoryDataListener(inventoryMgr);
        inventoryMgr.addUpstreamEventListener(upstreamMgr);

        // to support exiting behavior
        scheduleMgr.addRunStateListener(inventoryMgr);

        log.info("Gateway initialized");
    }

    public void start() {
        log.info("Gateway {} starting", Version.asString());

        clusterMgr.start();
        alertMgr.start();
        sensorMgr.start();
        inventoryMgr.start();
        upstreamMgr.start();
        downstreamMgr.start();
        scheduleMgr.start();
        endPointMgr.start();

        if (oneAtaTimeExec.isShutdown() || oneAtaTimeExec.isTerminated()) {
            oneAtaTimeExec = Executors.newSingleThreadExecutor();
        }
        log.info("Gateway started");
    }

    public void stop() {
        // Stop in opposite order of starting
        log.info("Gateway stopping");
        downstreamMgr.sendGWStatus(GatewayStatusUpdate.SHUTTING_DOWN);

        endPointMgr.stop();
        scheduleMgr.stop();
        downstreamMgr.stop();
        upstreamMgr.stop();
        inventoryMgr.stop();
        sensorMgr.stop();
        alertMgr.stop();
        clusterMgr.stop();

        scheduleMgr.removeRunStateListener(inventoryMgr);
        inventoryMgr.removeUpstreamEventListener(upstreamMgr);
        downstreamMgr.removeInventoryDataListener(inventoryMgr);
        sensorMgr.removeConnectionStateListener(alertMgr);
        sensorMgr.removeDeviceAlertListener(alertMgr);
        sensorMgr.removeConnectionStateListener(scheduleMgr);

        try {
            oneAtaTimeExec.shutdown();
            if (!oneAtaTimeExec.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                oneAtaTimeExec.shutdownNow();
                if (!oneAtaTimeExec.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    log.error("timeout waiting for executor to finish");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("interrupted waiting for executor to shut down");
        }
        log.info("Gateway stopped");
    }

    @Override
    public CLICommander buildCommander(PrettyPrinter _prettyPrinter) {
        CLICommander commander = new CLICommander(_prettyPrinter);
        commander.enable(clusterMgr);
        commander.enable(sensorMgr);
        commander.enable(scheduleMgr);
        commander.enable(inventoryMgr);
        commander.enable(upstreamMgr);
        commander.enable(downstreamMgr);
        return commander;
    }

    protected static ExecutorService oneAtaTimeExec = Executors.newSingleThreadExecutor();

}
