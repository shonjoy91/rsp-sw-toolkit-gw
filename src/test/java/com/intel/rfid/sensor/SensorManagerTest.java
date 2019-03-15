/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.intel.rfid.api.Personality;
import com.intel.rfid.helpers.EnvHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

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

        SensorManager sensorMgr = new SensorManager();
        SensorPlatform sensor;
        //Files.delete(SensorManager.CACHE_PATH);
        // try restoring first to check for any errors from nothing
        sensorMgr.restore();
        assert (sensorMgr.getRSPsCopy().size() == 0);

        sensor = sensorMgr.establishRSP("UnitTestRSP-AABBCC");
        sensor.setFacilityId("test01");
        sensor.setPersonality(Personality.POS);
        sensorMgr.establishRSP("UnitTestRSP-112233");
        sensorMgr.establishRSP("UnitTestRSP-DDEEFF");
        sensorMgr.persist();

        assert (Files.exists(SensorManager.CACHE_PATH));

        sensorMgr.restore();
        assert (sensorMgr.getRSPsCopy().size() == 3);
        
    }

}
