/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Env {

    // support configuration via environment variable
    public static final String ENV_VAR_GW_HOME = "RSP_GATEWAY_HOME";

    // support configuration via command line changes
    // (overrides environment variables)
    public static final String SYS_PROP_GW_HOME = "rsp.gateway.home";

    protected static final Logger log = LoggerFactory.getLogger(Env.class);
    private static Path homePath;
    private static Path cachePath;
    private static Path configPath;
    private static Path snapshotPath;
    private static Path statsPath;
    private static Path tagReadPath;
    private static Path exportTokenPath;
    private static Path sensorSoftwareRepoPath;
    private static Path webAdminResourcePath;

    // command line system property takes precedence
    // then any environment variables
    // then use the current working directory OR default value
    static {
        String s;

        // GATEWAY PATHS
        s = System.getProperty(SYS_PROP_GW_HOME);
        if (s == null) {
            s = System.getenv(ENV_VAR_GW_HOME);
        }
        if (s == null) {
            s = System.getProperty("user.dir");
        }
        homePath = Paths.get(s);
        cachePath = homePath.resolve("cache");
        configPath = homePath.resolve("config");
        snapshotPath = homePath.resolve("snapshot");
        statsPath = homePath.resolve("stats");
        tagReadPath = homePath.resolve("tagread");
        exportTokenPath = homePath.resolve("exported-tokens");
        sensorSoftwareRepoPath = homePath.resolve("sensor-sw-repo");
        webAdminResourcePath = homePath.resolve("web-admin");

        Path[] paths = {cachePath, configPath, snapshotPath, statsPath,
                        tagReadPath, exportTokenPath, sensorSoftwareRepoPath, webAdminResourcePath};

        for (Path curPath : paths) {
            try {
                ensurePath(curPath);
            } catch (IOException e) {
                log.error("error creating paths: {}", e);
            }
        }
    }

    public static Path getHomePath() {
        return homePath.toAbsolutePath();
    }

    public static Path getCachePath() {
        return cachePath.toAbsolutePath();
    }

    public static Path resolveCache(String _file) {
        return cachePath.resolve(_file).toAbsolutePath();
    }

    public static Path getConfigPath() {
        return configPath.toAbsolutePath();
    }

    public static Path resolveConfig(String _file) {
        return configPath.resolve(_file);
    }

    public static Path getSnapshotPath() {
        return snapshotPath.toAbsolutePath();
    }

    public static Path resolveSnapshotPath(String _file) {
        return snapshotPath.resolve(_file);
    }

    public static Path resolveStats(String _file) {
        return statsPath.resolve(_file);
    }

    public static Path resolveTagRead(String _file) {
        return tagReadPath.resolve(_file);
    }

    public static Path getExportTokenPath() {
        return exportTokenPath.toAbsolutePath();
    }

    public static Path resolveTokenPath(String _file) {
        return exportTokenPath.resolve(_file);
    }

    public static Path getSensorSoftwareRepoPath() {
        return sensorSoftwareRepoPath.toAbsolutePath();
    }

    public static Path getWebAdminResourcePath() {
        return webAdminResourcePath.toAbsolutePath();
    }

    public static void ensurePath(Path p) throws IOException {
        if (Files.notExists(p)) {
            log.info("creating path: {}", p.toString());
            Files.createDirectories(p);
        }
    }

}
