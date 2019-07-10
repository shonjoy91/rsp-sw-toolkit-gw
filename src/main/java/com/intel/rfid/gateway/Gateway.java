/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gateway;

import com.intel.rfid.api.data.InventorySummary;
import com.intel.rfid.api.upstream.GatewayHeartbeatNotification;
import com.intel.rfid.api.upstream.GatewayStatusUpdateNotification;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.console.CLICommandBuilder;
import com.intel.rfid.console.CLICommander;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.gpio.GPIOManager;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.rest.EndPointManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.upstream.UpstreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    protected GPIOManager gpioMgr;
    protected ScheduleManager scheduleMgr;
    protected InventoryManager inventoryMgr;
    protected DownstreamManager downstreamMgr;
    protected UpstreamManager upstreamMgr;
    protected EndPointManager endPointMgr;

    protected Gateway() { }

    protected void init() {
        log.info("Gateway {} software initializing", Version.asString());

        if (clusterMgr == null) {
            clusterMgr = new ClusterManager();
        }
        if (sensorMgr == null) {
            sensorMgr = new SensorManager(clusterMgr);
        }
        if (gpioMgr == null) {
            gpioMgr = new GPIOManager(sensorMgr);
        }
        if (inventoryMgr == null) {
            inventoryMgr = new InventoryManager();
        }
        if (downstreamMgr == null) {
            downstreamMgr = new DownstreamManager(sensorMgr, gpioMgr);
        }
        if (scheduleMgr == null) {
            scheduleMgr = new ScheduleManager(clusterMgr);
        }
        if (upstreamMgr == null) {
            upstreamMgr = new UpstreamManager(clusterMgr,
                                              sensorMgr,
                                              gpioMgr,
                                              scheduleMgr,
                                              inventoryMgr,
                                              downstreamMgr);
        }
        if (endPointMgr == null) {
            endPointMgr = new EndPointManager(clusterMgr,
                                              sensorMgr,
                                              gpioMgr,
                                              inventoryMgr,
                                              upstreamMgr,
                                              downstreamMgr,
                                              scheduleMgr);
        }

        // additional object relationships
        clusterMgr.setSensorManager(sensorMgr);
        clusterMgr.setScheduleManager(scheduleMgr);
        clusterMgr.setDownstreamManager(downstreamMgr);

        // hook up all the listeners

        // to apply the correct behavior to an RSP that comes online
        sensorMgr.addConnectionStateListener(scheduleMgr);

        // to check for unconfigured facilities for new RSPs coming online
        sensorMgr.addConnectionStateListener(upstreamMgr);

        // to forward these alerts upwards
        sensorMgr.addDeviceAlertListener(upstreamMgr);

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
        sensorMgr.start();
        gpioMgr.start();
        inventoryMgr.start();
        upstreamMgr.start();
        downstreamMgr.start();
        scheduleMgr.start();
        endPointMgr.start();

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
        }
        scheduler.scheduleAtFixedRate(this::heartbeatTask, 30, 30, TimeUnit.SECONDS);

        log.info("Gateway started");
    }

    public void stop() {
        // Stop in opposite order of starting
        log.info("Gateway stopping");

        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    log.error("timeout waiting for scheduler to finish");
                }
            }
        } catch (InterruptedException e) {
            log.warn("interrupted waiting for scheduler to shut down");
            Thread.currentThread().interrupt();
        }

        downstreamMgr.sendGWStatus(GatewayStatusUpdateNotification.SHUTTING_DOWN);

        endPointMgr.stop();
        scheduleMgr.stop();
        downstreamMgr.stop();
        upstreamMgr.stop();
        inventoryMgr.stop();
        gpioMgr.stop();
        sensorMgr.stop();
        clusterMgr.stop();

        scheduleMgr.removeRunStateListener(inventoryMgr);
        inventoryMgr.removeUpstreamEventListener(upstreamMgr);
        downstreamMgr.removeInventoryDataListener(inventoryMgr);
        sensorMgr.removeConnectionStateListener(upstreamMgr);
        sensorMgr.removeDeviceAlertListener(upstreamMgr);
        sensorMgr.removeConnectionStateListener(scheduleMgr);

        log.info("Gateway stopped");
    }

    @Override
    public CLICommander buildCommander(PrettyPrinter _prettyPrinter) {
        CLICommander commander = new CLICommander(_prettyPrinter);
        commander.enable(clusterMgr);
        commander.enable(sensorMgr);
        commander.enable(gpioMgr, sensorMgr);
        commander.enable(scheduleMgr);
        commander.enable(inventoryMgr);
        commander.enable(upstreamMgr);
        commander.enable(downstreamMgr);
        return commander;
    }

    protected ScheduledExecutorService scheduler;


    private void heartbeatTask() {
        GatewayHeartbeatNotification hb = new GatewayHeartbeatNotification();
        ConfigManager cfgMgr = ConfigManager.instance;
        hb.params.sent_on = System.currentTimeMillis();
        hb.params.device_id = cfgMgr.getGatewayDeviceId();
        upstreamMgr.send(hb);
    }

}
