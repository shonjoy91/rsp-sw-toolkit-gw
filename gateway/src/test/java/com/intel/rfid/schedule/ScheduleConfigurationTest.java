/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.helpers.Jackson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ScheduleConfigurationTest {

    private static ObjectMapper mapper = Jackson.getMapper();

    @BeforeClass
    public static void beforeClass() throws IOException {
        // EnvHelper.beforeTests();
        mapper = Jackson.getMapper();

    }

    @AfterClass
    public static void afterClass()  throws IOException {
        // EnvHelper.afterTests();
    }

    @Test
    public void testParseSampleSchedule() {

        ClassLoader loader = getClass().getClassLoader();
        String fileName = "schedules/sample_schedule_config.json";
        URL url = loader.getResource(fileName);
        assertNotNull(url);

        try (InputStream is = url.openStream()) {

            ScheduleConfiguration schedCfg = mapper.readValue(is, ScheduleConfiguration.class);

            System.out.println("parsed ScheduleConfiguration: " + schedCfg.id +
                               " with clusters: " + schedCfg.clusters.size());
            System.out.println(mapper.writeValueAsString(schedCfg));

        } catch (Exception e) {
            fail(fileName + " parsing threw Exception: " + e.getMessage());
        }


    }

    // @Test
    // public void testStartStopDefaultBehavior() {
    //
    //   ClassLoader loader = getClass().getClassLoader();
    //   URL url;
    //   Schedule sched;
    //   String fileName;
    //
    //   fileName = "json-test/get_schedule_ALL_START_STOP.json";
    //   url = loader.getResource(fileName);
    //   assertNotNull(url);
    //   try (InputStream is = url.openStream()) {
    //
    //     sched = ScheduleFactory.fromJSON(is);
    //     assertNotNull(sched);
    //     // check JSON serialize, deserialize
    //     ScheduleFactory.toCache(sched);
    //     sched = ScheduleFactory.fromCache();
    //     assertNotNull(sched);
    //     // clean up afterward
    //     ScheduleFactory.toCache(null);
    //
    //   } catch (Exception e) {
    //     fail(fileName + " parsing threw Exception: " + e.getMessage());
    //   }
    // }
    //
    // @Test
    // public void testBadJsonFormat() {
    //
    //   ClassLoader loader = getClass().getClassLoader();
    //   URL url;
    //   Schedule sched;
    //   String fileName;
    //
    //   fileName = "json-test/get_schedule_rsp_MISSING_JOB.json";
    //   url = loader.getResource(fileName);
    //   assertNotNull(url);
    //   try (InputStream is = url.openStream()) {
    //     sched = ScheduleFactory.fromJSON(is);
    //     assertNull(sched);
    //     fail(fileName + " should have thrown ValidationException");
    //   } catch (ValidationException e) {
    //     System.out.println("expected exception: " + e);
    //   } catch (Exception e) {
    //     fail(fileName + " should have thrown Schedule.FormatException");
    //   }
    //
    //   fileName = "json-test/get_schedule_rsp_MISSING_BEHAVIOR.json";
    //   url = loader.getResource(fileName);
    //   assertNotNull(url);
    //   try (InputStream is = url.openStream()) {
    //     sched = ScheduleFactory.fromJSON(is);
    //     assertNull(sched);
    //     fail(fileName + " should have thrown ValidationException");
    //   } catch (ValidationException e) {
    //     System.out.println("expected exception: " + e);
    //   } catch (Exception e) {
    //     fail(fileName + " should have thrown Schedule.FormatException");
    //   }
    // }
}
