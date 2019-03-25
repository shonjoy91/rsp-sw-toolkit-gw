/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.cluster;

import com.intel.rfid.api.Personality;
import com.intel.rfid.api.ProvisionToken;
import com.intel.rfid.helpers.EnvHelper;
import com.intel.rfid.schedule.ScheduleCluster;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterTest {

    @BeforeClass
    public static void beforeClass() throws IOException, GeneralSecurityException {
        EnvHelper.beforeTests();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        EnvHelper.afterTests();
    }

    public static final String FAKE = "fakityfakefake";


    ClusterTestHelper cth = new ClusterTestHelper();

    @Test
    public void testConfigurationParsing() {

        ClusterManager clusterMgr = new ClusterManager();
        Cluster cluster;

        cluster = clusterMgr.getCluster("SalesFloorExit");
        assertThat(cluster).isNull();

        cth.loadConfig(clusterMgr, "clusters/sample_sensor_cluster_config.json");
        cluster = clusterMgr.getCluster("SalesFloorExit");
        assertThat(cluster).isNotNull();
        assertThat(cluster.tokens).isEmpty();
        assertThat(cluster.sensor_groups).hasSize(1);

        cluster = clusterMgr.getCluster("SalesFloorMobility");
        assertThat(cluster).isNotNull();
        assertThat(cluster.tokens).isEmpty();
        assertThat(cluster.sensor_groups).hasSize(2);

        cth.loadConfig(clusterMgr, "clusters/sample_provisioning_cluster_config.json");
        cluster = clusterMgr.getCluster("SalesFloorExit");
        assertThat(cluster).isNotNull();
        assertThat(cluster.tokens).hasSize(1);
        assertThat(cluster.sensor_groups).isEmpty();

        cluster = clusterMgr.getCluster("SalesFloorMobility");
        assertThat(cluster).isNotNull();
        assertThat(cluster.tokens).hasSize(2);
        assertThat(cluster.sensor_groups).isEmpty();

    }

    @Test
    public void testExpiredToken() {

        ClusterManager clusterMgr = new ClusterManager();
        ProvisionToken pt = new ProvisionToken("test", "ABCDEF");
        pt.expirationTimestamp = 300;
        clusterMgr.tokens.put(pt.token, pt);

        assertThat(clusterMgr.isTokenValid(pt.token)).isFalse();

        pt = new ProvisionToken("test", "ABCDEF");
        pt.expirationTimestamp = System.currentTimeMillis() + 5000;
        clusterMgr.tokens.put(pt.token, pt);
        assertThat(clusterMgr.isTokenValid(pt.token)).isTrue();
    }

    @Test
    public void testTokenConnection() {
        List<ScheduleCluster> schedClusters = new ArrayList<>();
        ClusterManager clusterMgr = new ClusterManager();

        clusterMgr.generateFromConfig(schedClusters);
        assertThat(schedClusters).isEmpty();

        SensorManager sensorMgr = new SensorManager(clusterMgr);
        clusterMgr.generateFromConfig(schedClusters);
        assertThat(schedClusters).isEmpty();

        cth.loadConfig(clusterMgr, "clusters/sample_provisioning_cluster_config.json");

        Cluster clusterSFExit = clusterMgr.getCluster("SalesFloorExit");
        assertThat(clusterSFExit).isNotNull();
        assertThat(clusterSFExit.tokens).isNotEmpty();
        assertThat(clusterMgr.getProvionTokens()).isNotEmpty();

        String rspId150008 = "RSP-150008";

        // Bad tokens do not establish new sensors
        assertThat(sensorMgr.registerSensor(rspId150008, FAKE)).isFalse();
        Collection<SensorPlatform> sensors = sensorMgr.getRSPsCopy();
        assertThat(sensors).isEmpty();

        // Good token will establish a sensor and associate a provisioning token
        assertThat(sensorMgr.registerSensor(rspId150008, clusterSFExit.tokens.get(0).token)).isTrue();
        sensors = sensorMgr.getRSPsCopy();
        assertThat(sensors).isNotEmpty();

        SensorPlatform rspE00100 = sensorMgr.getRSP(rspId150008);
        assertThat(rspE00100).isNotNull();

        assertThat(rspE00100.getFacilityId()).isEqualTo(clusterSFExit.facility_id);
        assertThat(rspE00100.getProvisionToken()).isEqualTo(clusterSFExit.tokens.get(0).token);

        // Cluster configuration will clobber existing facility and personality
        // when new token comes in
        SensorPlatform rsp01 = sensorMgr.establishRSP("RSP-TEST01");
        rsp01.setFacilityId(FAKE);
        rsp01.setPersonality(Personality.FITTING_ROOM);

        assertThat(sensorMgr.registerSensor(rsp01.getDeviceId(), clusterSFExit.tokens.get(0).token)).isTrue();
        assertThat(rsp01.getFacilityId()).isEqualTo(clusterSFExit.facility_id);
        assertThat(rsp01.hasPersonality(Personality.EXIT)).isTrue();
        assertThat(rsp01.hasPersonality(Personality.FITTING_ROOM)).isFalse();
        assertThat(rsp01.hasPersonality(Personality.POS)).isFalse();

        // exercise some error paths for align sensor
        SensorPlatform rspNotInConfig = sensorMgr.establishRSP("RSP-noncfg");
        rspNotInConfig.setFacilityId(FAKE);
        clusterMgr.alignSensor(rspNotInConfig);
        assertThat(rspNotInConfig.getFacilityId()).isEqualTo(FAKE);

        schedClusters.clear();
        clusterMgr.generateFromConfig(schedClusters);
        for(ScheduleCluster sc : schedClusters) {
            System.out.println("sched cluster sensors");
            for(SensorPlatform rsp : sc.getAllSensors()) {
                System.out.println(rsp.getDeviceId());
            }
            System.out.println();
        }
        assertThat(schedClusters).isNotEmpty();

        // check that loading a new configuration will realign existing sensors
        rspE00100.setFacilityId(FAKE);
        rspE00100.setPersonality(Personality.FITTING_ROOM);
        cth.loadConfig(clusterMgr, "clusters/sample_sensor_cluster_config.json");
        assertThat(rsp01.getFacilityId()).isEqualTo(clusterSFExit.facility_id);
        assertThat(rsp01.hasPersonality(Personality.EXIT)).isTrue();
        assertThat(rsp01.hasPersonality(Personality.FITTING_ROOM)).isFalse();
        assertThat(rsp01.hasPersonality(Personality.POS)).isFalse();

    }


    @Test
    public void test_findClusterByDeviceId() {

        ClusterManager clusterMgr = new ClusterManager();
        Cluster cluster;

        cluster = clusterMgr.findClusterByDeviceId(null);
        assertThat(cluster).isNull();
        cluster = clusterMgr.findClusterByDeviceId(FAKE);
        assertThat(cluster).isNull();

        cth.loadConfig(clusterMgr, "clusters/sample_sensor_cluster_config.json");

        cluster = clusterMgr.findClusterByDeviceId(FAKE);
        assertThat(cluster).isNull();

        cluster = clusterMgr.findClusterByDeviceId("RSP-150009");
        assertThat(cluster).isNotNull();
        assertThat(cluster.id).isEqualTo("SalesFloorPOS");

        cluster = clusterMgr.findClusterByDeviceId("RSP-150005");
        assertThat(cluster).isNotNull();
        assertThat(cluster.id).isEqualTo("SalesFloorMobility");
    }

    @Test
    public void test_findClusterByToken() {

        ClusterManager clusterMgr = new ClusterManager();
        Cluster cluster;

        cluster = clusterMgr.findClusterByToken(null);
        assertThat(cluster).isNull();
        cluster = clusterMgr.findClusterByToken(FAKE);
        assertThat(cluster).isNull();

        cth.loadConfig(clusterMgr, "clusters/sample_provisioning_cluster_config.json");

        cluster = clusterMgr.findClusterByToken(FAKE);
        assertThat(cluster).isNull();

        cluster = clusterMgr.findClusterByToken("SalesFloor01Exit001SUUWWYYBBDDFFE3DA5098692CE27945AA367189B52C58");
        assertThat(cluster).isNotNull();
        assertThat(cluster.id).isEqualTo("SalesFloorExit");

        cluster = clusterMgr.findClusterByToken("BackStockDeepScan001UUWWYYBBDDFFE3DA5098692CE27945AA367189B52C58");
        assertThat(cluster).isNotNull();
        assertThat(cluster.id).isEqualTo("BackStock");
    }
}