/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.gateway.Env;
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

public class BehaviorConfig {

    protected static final Logger log = LoggerFactory.getLogger(BehaviorConfig.class);
    protected static ObjectMapper mapper = Jackson.getMapper();
    public static final Path BASE_DIR_PATH = Env.getConfigPath().resolve("behaviors");

    public static Map<String, Behavior> available() {
        Map<String, Behavior> map = new TreeMap<>();
        Behavior defaultBehavior = new Behavior();
        map.put(defaultBehavior.id, defaultBehavior);
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
                Behavior b = mapper.readValue(f, Behavior.class);
                if (map.containsKey(b.id)) {
                    log.warn("behavior file {} has duplicate id: {}", f.getAbsoluteFile(), b.id);
                }
                map.put(fileName, b);
            } catch (IOException e) {
                log.error("error processing behavior: {}", f.getAbsolutePath());
            }
        }
        return map;
    }

    public static void put(Behavior _behavior) throws IOException {
        Path p = BASE_DIR_PATH.resolve(_behavior.id + ".json");
        try (OutputStream os = Files.newOutputStream(p)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(os, _behavior);
            log.info("wrote {}", p);
        } catch (IOException e) {
            log.error("failed persisting {}", e.getMessage());
            throw e;
        }


    }

    public static Behavior deleteBehavior(String _behaviorId) throws IOException {
        Behavior behavior = getBehavior(_behaviorId);
        Path p = BASE_DIR_PATH.resolve(_behaviorId + ".json");
        if (Files.deleteIfExists(p)) {
            return behavior;
        } else {
            throw new IOException("behavior " + _behaviorId + " not found on disk");
        }
    }

    public static Behavior getBehavior(String _behaviorId) throws IOException {
        Map<String, Behavior> avail = available();
        for (Behavior b : avail.values()) {
            if (b.id.equals(_behaviorId)) {
                return b;
            }
        }
        throw new IOException("unable to locate " + _behaviorId);
    }

    public static int getWaitTimeout(Behavior _b) {
        int i = 10000;

        // num_inv cycles ~1 minute each
        int dwellTime = _b.getDwell_time();
        int numInvCycles = _b.getInv_cycles();
        int numRounds = (_b.auto_repeat ? 4 : 1);

        if (numInvCycles > 0) {
            if (dwellTime > 0) {
                i = dwellTime * numRounds;
            } else {
                // allow 1 minute for each inv_cycle
                i = (60 * 1000 * numInvCycles) * numRounds;
            }
        } else if (dwellTime > 0) {
            i = dwellTime * numRounds;
        }
        // give the RSPs a couple seconds to get
        // their inventory complete messages sent and received
        i += 2000;

        return i;
    }

}
