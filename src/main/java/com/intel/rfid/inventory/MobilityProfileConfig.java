/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.controller.Env;
import com.intel.rfid.helpers.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class MobilityProfileConfig {

    protected static final Logger log = LoggerFactory.getLogger(MobilityProfileConfig.class);
    protected static ObjectMapper mapper = Jackson.getMapper();
    public static final Path BASE_DIR_PATH = Env.getConfigPath().resolve("mobility");

    public static Map<String, MobilityProfile> available() {
        Map<String, MobilityProfile> map = new TreeMap<>();
        MobilityProfile defaultMobilityProfile = new MobilityProfile();
        map.put(defaultMobilityProfile.getId(), defaultMobilityProfile);
        File[] files = BASE_DIR_PATH.toFile().listFiles();

        if (files == null || files.length == 0) {
            return map;
        }

        for (File f : files) {

            String fileName = f.toPath().getFileName().toString();
            // get rid of the .json extension (or any extension)
            int i = fileName.lastIndexOf(".json");
            if (i > 0) {
                fileName = fileName.substring(0, i);
            }

            try {
                MobilityProfile b = mapper.readValue(f, MobilityProfile.class);
                if (map.containsKey(b.getId())) {
                    log.warn("mobility profile file {} has duplicate id: {}", f.getAbsoluteFile(), b.getId());
                }
                map.put(fileName, b);
            } catch (IOException e) {
                log.error("error processing mobility profile: {}", f.getAbsolutePath());
            }
        }
        return map;
    }

    public static void put(MobilityProfile _mobilityProfile) throws IOException {
        Path p = BASE_DIR_PATH.resolve(_mobilityProfile.getId() + ".json");
        try (OutputStream os = Files.newOutputStream(p)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(os, _mobilityProfile);
            log.info("wrote {}", p);
        } catch (IOException e) {
            log.error("failed persisting {}", e.getMessage());
            throw e;
        }


    }

    public static MobilityProfile deleteMobilityProfile(String _mobilityProfileId) throws IOException {
        MobilityProfile mobilityProfile = getMobilityProfile(_mobilityProfileId);
        Path p = BASE_DIR_PATH.resolve(_mobilityProfileId + ".json");
        if (Files.deleteIfExists(p)) {
            return mobilityProfile;
        } else {
            throw new IOException("mobilityProfile " + _mobilityProfileId + " not found on disk");
        }
    }

    public static MobilityProfile getMobilityProfile(String _mobilityProfileId) throws IOException {
        Map<String, MobilityProfile> avail = available();
        for (MobilityProfile b : avail.values()) {
            if (b.getId().equals(_mobilityProfileId)) {
                return b;
            }
        }
        throw new IOException("unable to locate " + _mobilityProfileId);
    }
    
}
