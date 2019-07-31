/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.controller.Env;
import com.intel.rfid.helpers.Jackson;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MobilityProfileConfig implements Completer {

    protected static final Logger log = LoggerFactory.getLogger(MobilityProfileConfig.class);
    protected static ObjectMapper mapper = Jackson.getMapper();

    public static MobilityProfile getProfile(String _id) throws IOException {
        return available().get(_id);
    }

    public static Map<String, MobilityProfile> available() throws IOException {

        // look for any mobility profiles under the conffiguration mobiliity directory
        File dirFile = Env.resolveConfig("mobility").toFile();

        if (!dirFile.exists()) {
            throw new FileNotFoundException("missing mobility dir: " + dirFile.getAbsolutePath());
        }

        if (!dirFile.isDirectory()) {
            throw new FileNotFoundException("mobility dir location is not a directory: " + dirFile.getAbsolutePath());
        }

        File[] files = dirFile.listFiles();
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("mobility dir is empty: " + dirFile.getAbsolutePath());
        }

        Map<String, MobilityProfile> map = new TreeMap<>();

        for (File f : files) {
            try {
                MobilityProfile mp = mapper.readValue(f, MobilityProfile.class);
                if (map.containsKey(mp.getId())) {
                    log.error("mobiility profile id map duplicate: {} in file {}",
                              mp.getId(), f.getAbsolutePath());
                } else {
                    map.put(mp.getId(), mp);
                }
            } catch (Exception e) {
                log.error("error getting mobility profile from file: {}", f.getAbsolutePath());
            }
        }
        return map;
    }

    public int complete(String buffer, final int cursor, final List<CharSequence> candidates) {

        StringsCompleter sc = new StringsCompleter();
        try {
            Map<String, MobilityProfile> map = available();
            if (!map.isEmpty()) {
                sc.getStrings().addAll(map.keySet());
            }
        } catch (IOException e) {
            log.error("error:", e);
        }
        return sc.complete(buffer, cursor, candidates);
    }

}
