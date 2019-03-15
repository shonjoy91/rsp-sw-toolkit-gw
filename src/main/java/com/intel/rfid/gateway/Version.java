/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gateway;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;

public class Version {

    protected static final String ID;

    static {
        String id = "unavailable";

        try {
            String jarUrl = Version.class.getProtectionDomain()
                                         .getCodeSource()
                                         .getLocation()
                                         .getFile();

            Attributes attributes = new JarFile(new File(jarUrl)).getManifest().getMainAttributes();
            id = attributes.getValue(IMPLEMENTATION_VERSION);

            if (id == null || id.length() == 0) {
                id = "unavailable";
            }

        } catch (IOException _e) {
            System.err.println(_e.getMessage());
        }

        ID = id;
    }

    public static String asString() {
        return ID;
    }

}
