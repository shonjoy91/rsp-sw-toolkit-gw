/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.intel.rfid.api.Personality;
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
    public void testPersistRestore() {

        String dev01 = "RSP-000001";
        String facility01 = "facility-01";
        String token01 = "token-01";

        Personality personality = Personality.EXIT;

        ClusterManager clusterMgr = new ClusterManager();

        SensorManager sensorMgr = new SensorManager(clusterMgr);
        SensorPlatform sensor;
        //Files.delete(SensorManager.CACHE_PATH);
        // try restoring first to check for any errors from nothing
        sensorMgr.restore();
        assert (sensorMgr.getRSPsCopy().size() == 0);

        sensor = sensorMgr.establishRSP(dev01);
        sensor.setProvisionToken(token01);
        sensor.setFacilityId(facility01);
        sensor.setPersonality(personality);

        sensorMgr.establishRSP("UnitTestRSP-112233");

        sensorMgr.establishRSP("UnitTestRSP-DDEEFF");

        sensorMgr.persist();

        assertThat(Files.exists(SensorManager.CACHE_PATH)).isTrue();

        sensorMgr = new SensorManager(clusterMgr);
        assertThat(sensorMgr.getRSPsCopy()).isEmpty();

        sensorMgr.restore();
        assertThat(sensorMgr.getRSPsCopy()).hasSize(3);

        List<SensorPlatform> sensors = new ArrayList<>();
        sensors.addAll(sensorMgr.findRSPs(dev01));

        assertThat(sensors).hasSize(1);
        sensor = sensors.get(0);
        assertThat(sensor.getDeviceId()).isEqualTo(dev01);
        assertThat(sensor.getProvisionToken()).isEqualTo(token01);
        assertThat(sensor.getFacilityId()).isEqualTo(facility01);

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
