/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.intel.rfid.api.data.Personality;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.helpers.EnvHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SensorManagerTest {


    @BeforeClass
    public static void beforeClass() throws IOException {
        EnvHelper.beforeTests();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        //EnvHelper.afterTests();
    }
    
    @Test
    public void testAliasing() {

        ClusterManager clusterMgr = new ClusterManager();
        SensorManager sensorMgr = new SensorManager(clusterMgr);

        SensorPlatform rsp01 = sensorMgr.establishRSP("RSP-TEST01");

        // Check constructed with defaults
        for (int i = 0; i < SensorPlatform.NUM_ALIASES; i++) {
            assertThat(rsp01.getAlias(i)).isEqualTo(rsp01.getDefaultAlias(i));
        }

        // Check ways to set a default
        rsp01.setAlias(0, null);
        rsp01.setAlias(1, "");
        rsp01.setAlias(3, SensorPlatform.ALIAS_KEY_DEFAULT);

        for (int i = 0; i < SensorPlatform.NUM_ALIASES; i++) {
            assertThat(rsp01.getAlias(i)).isEqualTo(rsp01.getDefaultAlias(i));
        }

        for (int i = 0; i < SensorPlatform.NUM_ALIASES; i++) {
            rsp01.setAlias(i, SensorPlatform.ALIAS_KEY_DEVICE_ID);
            assertThat(rsp01.getAlias(i)).isEqualTo(rsp01.getDeviceId());
        }

        assertThat(rsp01.getAlias(0)).isEqualTo(rsp01.getAlias(1));
        assertThat(rsp01.getAlias(2)).isEqualTo(rsp01.getAlias(1));
        assertThat(rsp01.getAlias(2)).isEqualTo(rsp01.getAlias(3));

        rsp01.setAlias(0, "alias-0");
        assertThat(rsp01.getAlias(0)).isEqualTo("alias-0");
        rsp01.setAlias(1, "alias-1");
        assertThat(rsp01.getAlias(1)).isEqualTo("alias-1");
        rsp01.setAlias(2, "alias-2");
        assertThat(rsp01.getAlias(2)).isEqualTo("alias-2");
        rsp01.setAlias(3, "alias-3");
        assertThat(rsp01.getAlias(3)).isEqualTo("alias-3");

    }

    @Test
    public void testPersistRestore() {

        String dev01 = "RSP-000001";
        String facility01 = "facility-01";
        String token01 = "token-01";
        String alias01 = "alias-01";
        String alias03 = "alias-03";

        Personality personality = Personality.EXIT;

        ClusterManager clusterMgr = new ClusterManager();

        SensorManager sensorMgr = new SensorManager(clusterMgr);
        List<SensorPlatform> sensors = new ArrayList<>();
        SensorPlatform sensor;
        //Files.delete(SensorManager.CACHE_PATH);
        // try restoring first to check for any errors from nothing
        sensorMgr.restore();
        sensorMgr.getSensors(sensors);
        assertThat(sensors).isEmpty();

        sensor = sensorMgr.establishRSP(dev01);
        sensor.setProvisionToken(token01);
        sensor.setFacilityId(facility01);
        sensor.setPersonality(personality);
        sensor.setAlias(1, alias01);
        sensor.setAlias(3, alias03);

        sensorMgr.establishRSP("UnitTestRSP-112233");

        sensorMgr.establishRSP("UnitTestRSP-DDEEFF");

        sensorMgr.persist();

        assertThat(Files.exists(SensorManager.CACHE_PATH)).isTrue();

        sensorMgr = new SensorManager(clusterMgr);
        sensors.clear();
        sensorMgr.getSensors(sensors);
        assertThat(sensors).isEmpty();

        sensorMgr.restore();
        sensors.clear();
        sensorMgr.getSensors(sensors);
        assertThat(sensors).hasSize(3);

        sensors.clear();
        sensors.addAll(sensorMgr.findRSPs(dev01));

        assertThat(sensors).hasSize(1);
        sensor = sensors.get(0);
        assertThat(sensor.getDeviceId()).isEqualTo(dev01);
        assertThat(sensor.getProvisionToken()).isEqualTo(token01);
        assertThat(sensor.getFacilityId()).isEqualTo(facility01);
        assertThat(sensor.getAlias(1)).isEqualTo(alias01);
        assertThat(sensor.getAlias(3)).isEqualTo(alias03);
        assertThat(sensor.getAlias(0)).isEqualTo(sensor.getDefaultAlias(0));
        assertThat(sensor.getAlias(2)).isEqualTo(sensor.getDefaultAlias(2));
        assertThat(sensor.getAlias(4)).isEqualTo(sensor.getDefaultAlias(4));

        assertThat(sensor.getPersonality()).isEqualTo(personality);
    }

    // String dev01 = "RSP-000001";
    // String facility01 = "facility-01";
    // String token01 = "token-01";
    // Personality personality01 = Personality.EXIT;
    //
    // @Test
    // public void testPersistRestore() {
    //     ClusterManager clusterMgr = new ClusterManager();
    //
    //     SensorManager sensorMgr = new SensorManager(clusterMgr);
    //     SensorPlatform sensor;
    //     //Files.delete(SensorManager.CACHE_PATH);
    //     // try restoring first to check for any errors from nothing
    //     sensorMgr.restore();
    //     assert (sensorMgr.getRSPsCopy().size() == 0);
    //
    //     sensor = sensorMgr.establishRSP(dev01);
    //     sensor.setProvisionToken(token01);
    //     sensor.setFacilityId(facility01);
    //     sensor.setPersonality(personality01);
    //
    //     sensorMgr.establishRSP("UnitTestRSP-112233");
    //
    //     sensorMgr.establishRSP("UnitTestRSP-DDEEFF");
    //
    //     sensorMgr.persist();
    //
    //     assert (Files.exists(SensorManager.CACHE_PATH));
    //
    //     sensorMgr = new SensorManager(clusterMgr);
    //     assert (sensorMgr.getRSPsCopy().size() == 0);
    //
    //     sensorMgr.restore();
    //     assert (sensorMgr.getRSPsCopy().size() == 3);
    //
    //     List<SensorPlatform> sensors = new ArrayList<>();
    //     sensors.addAll(sensorMgr.findRSPs(dev01));
    //    
    //     assertTrue(sensors.size() == 1);
    //     sensor = sensors.get(0);
    //     assertTrue(sensor.getDeviceId().equals(dev01));
    //     assertTrue(sensor.getProvisionToken().equals(token01));
    //     assertTrue(sensor.getFacilityId().equals(facility01));
    //     assertTrue(sensor.getPersonality() == personality01);
    // }

}
