/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.cluster;

import com.intel.rfid.api.data.Cluster;
import com.intel.rfid.api.data.ClusterConfig;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.exception.ExpiredTokenException;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.exception.InvalidTokenException;
import com.intel.rfid.helpers.EnvHelper;
import com.intel.rfid.security.ProvisionToken;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import org.assertj.core.api.ThrowableAssert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

public class ClusterTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        EnvHelper.beforeTests();
    }

    @AfterClass
    public static void afterClass() {
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
        final ProvisionToken pt1 = new ProvisionToken("test", "ABCDEF");
        pt1.expirationTimestamp = 300;
        clusterMgr.tokens.put(pt1.token, pt1);

        assertThatThrownBy(() -> clusterMgr.validateToken(pt1.token)).isInstanceOf(ExpiredTokenException.class);

        final ProvisionToken pt2 = new ProvisionToken("test", "ABCDEF");
        pt2.expirationTimestamp = System.currentTimeMillis() + 5000;
        clusterMgr.tokens.put(pt2.token, pt2);
        try {
            clusterMgr.validateToken(pt2.token);
        } catch (GatewayException _e) {
            fail("unexpected token validation error {}", _e.getMessage());
        }
    }

    @Test
    public void testAliasConfiguration() {

        ClusterManager clusterMgr = new ClusterManager();
        SensorManager sensorMgr = new SensorManager(clusterMgr);
        clusterMgr.setSensorManager(sensorMgr);

        SensorPlatform rsp00 = sensorMgr.establishRSP("RSP-150000");
        SensorPlatform rsp01 = sensorMgr.establishRSP("RSP-150001");
        SensorPlatform rsp02 = sensorMgr.establishRSP("RSP-150002");
        SensorPlatform rsp03 = sensorMgr.establishRSP("RSP-150003");
        SensorPlatform rsp04 = sensorMgr.establishRSP("RSP-150004");
        SensorPlatform rsp05 = sensorMgr.establishRSP("RSP-150005");

        cth.loadConfig(clusterMgr, "clusters/sample_alias_cluster_config.json");

        for (int i = 0; i < SensorPlatform.NUM_ALIASES; i++) {
            assertThat(rsp00.getAlias(i)).isEqualTo(rsp00.getDefaultAlias(i));
            assertThat(rsp01.getAlias(i)).isEqualTo(rsp01.getDefaultAlias(i));
        }

        SensorPlatform[] sensors = {rsp02, rsp03, rsp04};
        for (SensorPlatform sensor : sensors) {
            for (int i = 0; i < 2; i++) {
                assertThat(sensor.getAlias(i)).isEqualTo(sensor.getDeviceId());
            }
        }

        assertThat(rsp05.getAlias(0)).isEqualTo(rsp05.getDefaultAlias(0));
        assertThat(rsp05.getAlias(1)).isEqualTo(rsp05.getDefaultAlias(1));
        assertThat(rsp05.getAlias(2)).isEqualTo("freezer");
        assertThat(rsp05.getAlias(3)).isEqualTo("cooler");

    }

    @Test
    public void testTokenConnection() throws InvalidTokenException, ExpiredTokenException {
        List<ClusterRunner> runners = new ArrayList<>();
        ClusterManager clusterMgr = new ClusterManager();

        clusterMgr.generateFromConfig(runners);
        assertThat(runners).isEmpty();

        SensorManager sensorMgr = new SensorManager(clusterMgr);
        clusterMgr.setSensorManager(sensorMgr);

        clusterMgr.generateFromConfig(runners);
        assertThat(runners).isEmpty();

        cth.loadConfig(clusterMgr, "clusters/sample_provisioning_cluster_config.json");

        Cluster clusterSFExit = clusterMgr.getCluster("SalesFloorExit");
        assertThat(clusterSFExit).isNotNull();
        assertThat(clusterSFExit.tokens).isNotEmpty();
        assertThat(clusterMgr.getProvionTokens()).isNotEmpty();

        String rspId150008 = "RSP-150008";

        Collection<SensorPlatform> sensors = new ArrayList<>();
        sensorMgr.getSensors(sensors);
        assertThat(sensors).isEmpty();

        // Good token will establish a sensor and associate a provisioning token
        sensorMgr.registerSensor(rspId150008, clusterSFExit.tokens.get(0).token);
        sensors.clear();
        sensorMgr.getSensors(sensors);
        assertThat(sensors).isNotEmpty();

        SensorPlatform rspE00100 = sensorMgr.getSensor(rspId150008);
        assertThat(rspE00100).isNotNull();

        assertThat(rspE00100.getFacilityId()).isEqualTo(clusterSFExit.facility_id);
        assertThat(rspE00100.getProvisionToken()).isEqualTo(clusterSFExit.tokens.get(0).token);

        //"ExitLeft", "ExitOverhead", "ExitRight", null
        assertThat(rspE00100.getAlias(0)).isEqualTo("ExitLeft");
        assertThat(rspE00100.getAlias(1)).isEqualTo("ExitOverhead");
        assertThat(rspE00100.getAlias(2)).isEqualTo("ExitRight");
        assertThat(rspE00100.getAlias(3)).isEqualTo(rspE00100.getDefaultAlias(3));
        assertThat(rspE00100.getAlias(3)).isEqualTo(rspE00100.getDeviceId() + "-3");

        // Cluster configuration will clobber existing facility and personality
        // when new token comes in
        SensorPlatform rsp01 = sensorMgr.establishRSP("RSP-TEST01");
        rsp01.setFacilityId(FAKE);
        rsp01.setPersonality(Personality.FITTING_ROOM);
        rsp01.setAlias(0, FAKE);
        rsp01.setAlias(2, FAKE);

        sensorMgr.registerSensor(rsp01.getDeviceId(), clusterSFExit.tokens.get(0).token);
        assertThat(rsp01.getFacilityId()).isEqualTo(clusterSFExit.facility_id);
        assertThat(rsp01.hasPersonality(Personality.EXIT)).isTrue();
        assertThat(rsp01.hasPersonality(Personality.FITTING_ROOM)).isFalse();
        assertThat(rsp01.hasPersonality(Personality.POS)).isFalse();

        // exercise some error paths for align sensor
        SensorPlatform rspNotInConfig = sensorMgr.establishRSP("RSP-noncfg");
        rspNotInConfig.setFacilityId(FAKE);
        clusterMgr.alignSensor(rspNotInConfig);
        assertThat(rspNotInConfig.getFacilityId()).isEqualTo(FAKE);

        runners.clear();
        clusterMgr.generateFromConfig(runners);
        assertThat(runners).isNotEmpty();

        // check that loading a new configuration will realign existing sensors
        rspE00100.setFacilityId(FAKE);
        rspE00100.setPersonality(Personality.FITTING_ROOM);
        rspE00100.setAlias(1, FAKE);
        rspE00100.setAlias(3, FAKE);
        cth.loadConfig(clusterMgr, "clusters/sample_sensor_cluster_config.json");
        assertThat(rspE00100.getFacilityId()).isEqualTo(clusterSFExit.facility_id);
        assertThat(rspE00100.hasPersonality(Personality.EXIT)).isTrue();
        assertThat(rspE00100.hasPersonality(Personality.FITTING_ROOM)).isFalse();
        assertThat(rspE00100.hasPersonality(Personality.POS)).isFalse();

    }


    @Test
    public void testValidate() {
        assertThatThrownBy(() -> ClusterManager.validate(null))
                .isInstanceOf(ConfigException.class)
                .withFailMessage(ClusterManager.VAL_ERR_NULL_CFG);

        final ClusterConfig cfg = new ClusterConfig();
        final ThrowableAssert.ThrowingCallable throwingCallable = () -> ClusterManager.validate(cfg);
        assertThatThrownBy(throwingCallable)
                .isInstanceOf(ConfigException.class)
                .withFailMessage(ClusterManager.VAL_ERR_MISSING_CLUSTERS);

        Cluster cluster = new Cluster();
        cfg.clusters.add(cluster);
        assertThatThrownBy(throwingCallable).isInstanceOf(ConfigException.class);

        cluster.behavior_id = "ClusterMobility_PORTS_1";
        try {
            ClusterManager.validate(cfg);
        } catch (Exception _e) {
            fail("Unexpected exception: " + _e.getClass() + " with " + _e.getMessage());
        }

        cluster = new Cluster();
        cluster.behavior_id = "bad_behavior_id";
        cfg.clusters.add(cluster);

        cluster = new Cluster();
        cluster.behavior_id = "and_another_bad_behavior_id";
        cfg.clusters.add(cluster);
        assertThatThrownBy(throwingCallable).isInstanceOf(ConfigException.class);

        cfg.clusters = null;
        assertThatThrownBy(throwingCallable).isInstanceOf(ConfigException.class);
    }

    @Test
    public void testFindClusterByDeviceId() {

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
    public void testFindClusterByToken() {

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