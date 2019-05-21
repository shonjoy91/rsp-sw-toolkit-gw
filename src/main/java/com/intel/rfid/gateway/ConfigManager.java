/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.gateway;

import com.intel.rfid.api.downstream.SensorCredentials;
import com.intel.rfid.api.data.RepoVersions;
import com.intel.rfid.downstream.MQTTDownstream;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.helpers.ProcessHelper;
import com.intel.rfid.helpers.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigManager {

    public static final ConfigManager instance = new ConfigManager();

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected Properties properties;

    ConfigManager() {
        properties = new Properties();
        // Default config is in lib folder, as it is meant to be immutable
        Path p = Env.resolveConfig("gateway.cfg");
        try (InputStream is = Files.newInputStream(p)) {
            properties.load(is);
            log.info("loaded default configuration from: " + p);
        } catch (Exception e) {
            log.error("Unable to load default configuration {}", e.getMessage());
        }

    }

    public SensorCredentials getSensorCredentials() {
        // the password cannot be null, must be an empty string
        return new SensorCredentials(getMQTTDownstreamURI(),
                                     getOptString("mqtt.downstream.topic.prefix", MQTTDownstream.TOPIC_PREFIX),
                                     getOptString("mqtt.downstream.password", ""));
    }

    public String get(String property) {
        return (String) properties.get(property);
    }

    public String getReqString(String _key) throws ConfigException {
        String val = (String) properties.get(_key);
        if (val == null || val.isEmpty()) {
            throw new ConfigException("missing value for required property " + _key);
        }
        return val;
    }

    public int getReqInt(String _property) throws ConfigException {
        String s = get(_property);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new ConfigException(_property + " bad integer format for value: " + s);
        }
    }

    public long getReqLong(String _property) throws ConfigException {
        String s = get(_property);
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new ConfigException(_property + " bad long format for value: " + s);
        }
    }

    public boolean getReqBool(String _property) throws ConfigException {
        String s = get(_property);
        if (s != null) {
            s = s.trim();
            if (s.equalsIgnoreCase("true")) {
                return true;
            } else if (s.equalsIgnoreCase("false")) {
                return false;
            }
        }
        throw new ConfigException(_property + " bad boolean format for value: " + s);
    }


    public String getOptString(String property, String _default) {
        String val = (String) properties.get(property);
        return val != null ? val : _default;
    }

    public int getOptInt(String _property, int _default) {
        try {
            return getReqInt(_property);
        } catch (ConfigException _ce) {
            log.info("returning default value {} for property {} ", _default, _property);
            return _default;
        }
    }

    public long getOptLong(String _property, long _default) {
        try {
            return getReqLong(_property);
        } catch (ConfigException _ce) {
            return _default;
        }
    }

    public boolean getOptBool(String _property, boolean _default) {
        try {
            return getReqBool(_property);
        } catch (ConfigException _ce) {
            return _default;
        }
    }


    private String gwDeviceId = null;

    public String getGatewayDeviceId() {
        if (gwDeviceId == null) {
            try {
                gwDeviceId = (String) properties.get("gateway.device_id");
                if (gwDeviceId == null || gwDeviceId.length() == 0) {
                    gwDeviceId = getHostname();
                }
            } catch (Exception e) {
                log.error("Problems getting gateway device id", e);
            }
        }
        return gwDeviceId;
    }

    /**
     * This facilitates defaulting configuration values to the hostname of
     * the system the code is running on, but allowing overriding those hosts
     * in the config file if needed. Current design is that gateway host also
     * is where the mqtt broker and the ntp server is running
     */
    public String getHost(String _key) {
        String s = null;
        try {
            s = (String) properties.get(_key);
            if (s == null || s.length() == 0) {
                s = getHostname() + ".local";
            }
        } catch (Exception e) {
            log.error("Problems getting host for key {}: ", _key, e);
        }
        return s;
    }

    public String getLocalHost(String _key) {
        String s = null;
        try {
            s = (String) properties.get(_key);
            if (s == null || s.length() == 0) {
                s = getHostname() + ".local";
            }
        } catch (Exception e) {
            log.error("Problems getting host for key {}: ", _key, e);
        }
        return s;
    }

    public String getLocalHost() {
        return getLocalHost("gateway.device_id");
    }

    String getHostname() throws UnknownHostException {
        String hostname;

        // Linux env var
        // Query the env var first to allow it to be overridden easier within docker
        hostname = System.getenv().get("HOSTNAME");
        if (!StringHelper.isNullOrEmpty(hostname)) {
            return hostname;
        }

        // Linux hostname command
        ProcessHelper ph = new ProcessHelper("hostname");
        ph.run();
        if (ph.exitCode == 0 && !ph.outLines.isEmpty()) {
            hostname = ph.outLines.get(0);
            if (!StringHelper.isNullOrEmpty(hostname)) {
                return hostname;
            }
        }

        // Windows hostname command
        ph = new ProcessHelper("hostname.exe");
        ph.run();
        if (ph.exitCode == 0 && !ph.outLines.isEmpty()) {
            hostname = ph.outLines.get(0);
            if (!StringHelper.isNullOrEmpty(hostname)) {
                return hostname;
            }
        }

        // Windows env var
        hostname = System.getenv().get("COMPUTERNAME");
        if (!StringHelper.isNullOrEmpty(hostname)) {
            return hostname;
        }

        // Java fallback
        return InetAddress.getLocalHost().getHostName();
    }

    public String getSensorRepoBase() {
        return Env.getHomePath().relativize(Env.getSensorSoftwareRepoPath()).toString();
    }
    
    public List<String> getSensorSoftwareRepos() {
        List<String> urls = new ArrayList<>();
        String proto = getOptString("repo.rsp.protocol", "http");

        String repoHost = getURI(proto,
                                 getLocalHost("repo.rsp.host"),
                                 getOptInt("repo.rsp.port", getProvisionPortForProtocol(proto)));

        List<String> archs = new ArrayList<>();
        RepoVersions versions = new RepoVersions();
        getRepoInfo(archs, versions);
        for (String arch : archs) {
            urls.add(repoHost + "/" + getSensorRepoBase() + "/" + arch);
        }

        if (urls.isEmpty()) {
            log.error("configuration for repo.rsp.archs does not contain any valid arch names");
        }

        return urls;
    }

    public void getRepoInfo(List<String> _architectures, RepoVersions _repoVersions) {

        // the architectures are the top level directory names
        // in the software repo
        File[] dirs = Env.getSensorSoftwareRepoPath().toFile().listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                String archName = dir.getName();
                _architectures.add(dir.getName());
                String suffix = "_" + archName + ".ipk";

                File[] pkgs = dir.listFiles((File pathname) -> pathname.getName().endsWith(suffix));
                if(pkgs == null) { continue; }
                for(File f : pkgs) {
                    String pkg = f.getName();
                    String pkgName = pkg.substring(0, pkg.indexOf("_"));
                    String pkgVer = pkg.substring(pkgName.length() + 1, pkg.indexOf(suffix));
                    switch (pkgName) {
                        case "rfid-rsp":
                            _repoVersions.app_version = pkgVer;
                            break;
                        case "platform-support":
                            _repoVersions.platform_support_version = pkgVer;
                            break;
                        case "pkg-manifest":
                            _repoVersions.pkg_manifest_version = pkgVer;
                            break;
                    }
                }
            }
        }
    }
    
    public static class Credentials {
        public String userId;
        public String password;
    }

    public int getConsolePort() {
        return getOptInt("console.port", 5222);
    }

    public Credentials getConsoleCredentials() {
        Credentials credentials = new Credentials();
        credentials.userId = getOptString("console.userid", "gwconsole");
        credentials.password = getOptString("console.password", "gwconsole");
        return credentials;
    }

    // tcp, ssl, mqtt(?), mqtts(?)
    public static final String DEFAULT_MQTT_PROTOCOL = "tcp";
    public static final int DEFAULT_MQTT_PORT = 1883;


    public String getMQTTUpstreamURI() {
        return getURI(getOptString("mqtt.upstream.protocol", DEFAULT_MQTT_PROTOCOL),
                      getHost("mqtt.upstream.host"),
                      getOptInt("mqtt.upstream.port", DEFAULT_MQTT_PORT));
    }

    public Credentials getMQTTUpstreamCredentials() {
        Credentials c = new Credentials();
        c.userId = getOptString("mqtt.upstream.username", "RFID-GW-UPSTREAM");
        c.password = get("mqtt.upstream.password");
        return c;
    }

    public String getMQTTDownstreamURI() {
        return getURI(getOptString("mqtt.downstream.protocol", DEFAULT_MQTT_PROTOCOL),
                      getHost("mqtt.downstream.host"),
                      getOptInt("mqtt.downstream.port", DEFAULT_MQTT_PORT));
    }

    public Credentials getMQTTDownstreamCredentials() {
        Credentials c = new Credentials();
        c.userId = getOptString("mqtt.downstream.username", "RFID-GW-DOWNSTREAM");
        c.password = get("mqtt.downstream.password");
        return c;
    }

    public String getURI(String _protocol, String _host, int _port) {
        return String.format("%s://%s:%s", _protocol, _host, _port);
    }


    // REST Configuration
    // Because this information needs to be shared between JmDNS and 
    // the servlets and endpoints, all the configuration is centralized
    // here in the configuaration manager
    public static final String PROVISION_ROOT_CA_PATH = "/provision/root-ca-cert";
    public static final String PROVISION_SENSOR_CREDENTIALS_PATH = "/provision/sensor-credentials";

    public String getProvisionCACertProtocol() {
        return getOptString("provision.ca.cert.protocol", "http");
    }

    public String getProvisionSensorCredentialsProtocol() {
        return getOptString("provision.sensor.credentials.protocol", "https");
    }

    public int getProvisionPortForProtocol(String _protocol) {
        return "http".equals(_protocol) ? getProvisionHttpPort() : getProvisionTlsPort();
    }

    public int getProvisionHttpPort() {
        return getOptInt("provision.http.port", 8080);
    }

    public int getProvisionTlsPort() {
        return getOptInt("provision.tls.port", 8443);
    }

    public String getProvisionUrlCACert() {
        String proto = getProvisionCACertProtocol();
        return getURI(proto,
                      getLocalHost(),
                      getProvisionPortForProtocol(proto)) +
               PROVISION_ROOT_CA_PATH;
    }

    public String getProvisionUrlSensorCredentials() {
        String proto = getProvisionSensorCredentialsProtocol();
        return getURI(proto,
                      getLocalHost(),
                      getProvisionPortForProtocol(proto)) +
               PROVISION_SENSOR_CREDENTIALS_PATH;
    }

    public boolean getProvisionSensorTokenRequired() {
        return getOptBool("provision.sensor.token.required", false);
    }

}
