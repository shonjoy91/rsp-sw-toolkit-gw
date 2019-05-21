/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.sensor.SensorPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class RssiAdjuster {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected MobilityProfile mobilityProfile;

    public RssiAdjuster() {
        restoreMobilityProfile();
    }

    public void set(MobilityProfile _profile) {
        mobilityProfile = _profile;
        persistMobilityProfile();
    }

    public double getWeight(long _lastReadMillis, SensorPlatform _rsp) {

        if (_rsp.isInDeepScan()) {
            return mobilityProfile.getT();
        }

        double w;
        double T = mobilityProfile.getT();
        double M = mobilityProfile.getM();
        double B = mobilityProfile.getB();

        w = (M * (System.currentTimeMillis() - _lastReadMillis)) + B;

        // check if weight needs to be capped at threshold ceiling
        if (w > T) { w = T; }

        return w;
    }

    public void showMobilityProfile(PrettyPrinter _out) {
        _out.line(mobilityProfile.toString());
    }

    // need to match up the cache and the configuration
    public static final Path MOBILITY_PROFILE_PATH = Env.getCachePath().resolve("mobility_profile.json");
    protected static ObjectMapper mapper = Jackson.getMapper();

    private void persistMobilityProfile() {
        try (OutputStream os = Files.newOutputStream(MOBILITY_PROFILE_PATH)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(os, mobilityProfile);
            log.info("wrote {}", MOBILITY_PROFILE_PATH);
        } catch (IOException e) {
            log.error("failed writing {}", MOBILITY_PROFILE_PATH, e);
        }

    }

    private void restoreMobilityProfile() {
        if (!Files.exists(MOBILITY_PROFILE_PATH)) {
            log.info("no mobility sensitivity has been established, using the default");
            mobilityProfile = new MobilityProfile();
            persistMobilityProfile();
            return;
        }

        try (InputStream is = Files.newInputStream(MOBILITY_PROFILE_PATH)) {

            mobilityProfile = mapper.readValue(is, MobilityProfile.class);
            log.info("Restored {}", MOBILITY_PROFILE_PATH);

        } catch (IOException e) {
            log.error("Failed to restore {}", MOBILITY_PROFILE_PATH, e);
        }

    }
}
