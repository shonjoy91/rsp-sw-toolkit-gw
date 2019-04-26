/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import ch.qos.logback.classic.Level;
import com.intel.rfid.gateway.Env;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class EnvHelper {

    private static Path configResourceDirPath = null;
    private static Path testResourceDirPath = null;
    private static Path testRunPath = null;

    public static void beforeTests() throws IOException {
        beforeBasicTests();
    }

    public static void beforeBasicTests() throws IOException {
        establishTestDir();
        copyConfig();
        adjustLogs();
    }

    public static void afterTests() {
        if (testRunPath != null) {
            deleteAll(testRunPath.toFile());
        }
        testRunPath = null;
    }

    // will copy the config directory contents into the test environment
    public static void copyConfig() throws IOException {
        FileHelper.copy(configResourceDirPath, testRunPath.resolve("config"));
    }

    // this will make the console and test output much quieter
    // by changing scheduler and jetty default log levels
    public static void adjustLogs() {
        try {
            ch.qos.logback.classic.Logger logger;
            String[] logClasses = {
                "org.eclipse.jetty",
                "javax.jmdns.impl",
                "org.quartz"
            };

            for (String s : logClasses) {
                logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(s);
                logger.setLevel(Level.WARN);
            }

        } catch (Exception e) {
            System.err.println("error setting log levels " + e.getMessage());
        }

    }

    public static void clearCache() {
        File[] files = Env.getCachePath().toFile().listFiles();
        if (files == null) { return; }
        for (File c : files) { deleteAll(c); }

    }

    protected static boolean deleteAll(File _f) {

        if (_f == null || !_f.exists()) { return true; }

        if (_f.isDirectory()) {
            File[] files = _f.listFiles();
            if (files == null) { return true; }
            for (File c : files) { deleteAll(c); }
        }
        return _f.delete();
    }

    public static void establishTestDir() throws IOException {

        // gradle sets current directory to the build dir
        // Intellij sets it to the project dir so do a bit
        // of manipulation to get to the right place
        String runDir = "unit_test_run";
        Path usrDir = Paths.get(System.getProperty("user.dir"));
        System.out.println("usrDir: " + usrDir.getFileName().toString());
        if (Files.exists(usrDir.resolve("config"))) {
            configResourceDirPath = usrDir.resolve("config");
            testResourceDirPath = usrDir.resolve("src")
                                        .resolve("test")
                                        .resolve("resources");
            testRunPath = usrDir.resolve("build").resolve(runDir);
        } else if (usrDir.endsWith("build")) {
            configResourceDirPath = usrDir.resolveSibling("config");
            testResourceDirPath = usrDir.resolveSibling("src")
                                        .resolve("test")
                                        .resolve("resources");
            testRunPath = usrDir.resolve(runDir);
        }

        assertTrue("unable to locate source config directory: " + configResourceDirPath.toAbsolutePath(),
                   configResourceDirPath.toFile().isDirectory());
        assertTrue("unable to locate test resouces directory: " + testResourceDirPath.toAbsolutePath(),
                   testResourceDirPath.toFile().isDirectory());

        // be sure to set the system property before referencing Env class
        System.setProperty(Env.SYS_PROP_GW_HOME, testRunPath.toString());

        // this section actually creates the config and cache directories
        if (testRunPath.toFile().exists()) {
            deleteAll(testRunPath.toFile());
        }
        Env.ensurePath(testRunPath);
        Env.ensurePath(Env.getCachePath());
        Env.ensurePath(Env.getConfigPath());

        File f;
        f = Env.getCachePath().toFile();
        System.out.println("testing cache path:" + f.getAbsolutePath());
        assertTrue(f.exists());
        assertTrue(f.isDirectory());

        f = Env.getConfigPath().toFile();
        System.out.println("testing config path:" + f.getAbsolutePath());
        assertTrue(f.exists());
        assertTrue(f.isDirectory());

    }

    public static Path resolveTestResource(String _path) {
        return testResourceDirPath.resolve(_path);
    }

}
