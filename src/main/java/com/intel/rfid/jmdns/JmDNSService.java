/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.jmdns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.controller.ConfigManager;
import com.intel.rfid.helpers.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class JmDNSService {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static class ServiceAnnouncement {

        public final boolean sensor_token_required;
        public final String mqtt_credentials_url;
        public final String root_cert_url;
        public final String ntp_host;

        public ServiceAnnouncement() {
            ConfigManager cm = ConfigManager.instance;
            ntp_host = cm.getLocalHost("ntp.server.host");
            sensor_token_required = cm.getProvisionSensorTokenRequired();
            root_cert_url = cm.getProvisionUrlCACert();
            mqtt_credentials_url = cm.getProvisionUrlSensorCredentials();
        }
    }

    protected Map<String, String> buildProperTxtRecordMap() {
        Map<String, String> map = new HashMap<>();
        ConfigManager cm = ConfigManager.instance;
        map.put("mqtt_credentials_url", cm.getProvisionUrlSensorCredentials());
        map.put("ntp_host", cm.getLocalHost("ntp.server.host"));
        map.put("root_cert_url", cm.getProvisionUrlCACert());
        map.put("sensor_token_required", Boolean.toString(cm.getProvisionSensorTokenRequired()));
        return map;
    }

    private ObjectMapper mapper = Jackson.getMapper();
    private boolean started = false;

    public boolean start() {
        if (!started) {
            try {
                JmmDNS jmmDNS = JmmDNS.Factory.getInstance();
                jmmDNS.addNetworkTopologyListener(new NetworkChangeListener());
                for (JmDNS jmdns : jmmDNS.getDNS()) {
                    if (isValidRegisterAddress(jmdns.getInetAddress())) {
                        registerService(jmdns);
                        started = true;
                    }
                }
                log.info(getClass().getSimpleName() + " started");
            } catch (IOException _e) {
                log.warn("IOException starting: {}", _e.getMessage());
            }
        }
        return started;
    }

    public boolean stop() {

        if (started) {
            try {
                JmmDNS jmmDNS = JmmDNS.Factory.getInstance();
                for (JmDNS jmdns : jmmDNS.getDNS()) {
                    if (isValidRegisterAddress(jmdns.getInetAddress())) {
                        log.info("Un-registering JmDNS services {}", info.getName());
                        jmdns.unregisterService(info);
                        log.info("Un-registering JmDNS services {}", info2.getName());
                        jmdns.unregisterService(info2);
                    }
                }
                jmmDNS.close();
                started = false;
            } catch (IOException _e) {
                log.error("IOException stopping: {}", _e.getMessage());
            }
        }
        log.info(getClass().getSimpleName() + " stopped");
        return !started;
    }

    public boolean isStarted() {
        return started;
    }

    public class NetworkChangeListener implements NetworkTopologyListener {
        @Override
        public void inetAddressAdded(NetworkTopologyEvent event) {
            try {
                InetAddress addr = event.getDNS().getInetAddress();
                log.debug("InetAddress Added: {}", addr.getHostAddress());
                if (isValidRegisterAddress(addr)) {
                    log.info("InetAddress {} was added, we will register jmdns", addr.getHostAddress());
                    registerService(event.getDNS());
                }
            } catch (IOException ioe) {
                log.warn("Unable to register jmdns.", ioe);
            }
        }

        @Override
        public void inetAddressRemoved(NetworkTopologyEvent event) {
            try {
                InetAddress addr = event.getDNS().getInetAddress();
                log.debug("InetAddress Removed: {}", addr.getHostAddress());
                if (isValidRegisterAddress(addr)) {
                    log.warn("InetAddress {} was removed, we will un-register the jmdns", addr.getHostAddress());
                    event.getDNS().unregisterAllServices();
                }
            } catch (IOException ioe) {
                log.warn("Unable to un-register jmdns.", ioe);
            }
        }
    }

    // Abstract out this function since it is used in multiple places
    //  so we can prevent someone from forgetting to change it everywhere
    private static boolean isValidRegisterAddress(InetAddress addr) {
        return addr instanceof Inet4Address && !addr.isLoopbackAddress();
    }

    ServiceInfo info, info2;
    private synchronized void registerService(JmDNS jmdns) {

        // need to handle the deprecated name for Gateway and the new name to support
        // sensor software versions
        try {
            String hostName = jmdns.getHostName();
            String hostAddress = jmdns.getInetAddress().getHostAddress();
            String deviceId = ConfigManager.instance.getRspControllerDeviceId();

            info = ServiceInfo.create(
                    "_rfid._tcp.local.",
                    "RFID-Gateway-" + deviceId,
                    "",
                    0, 0, 0,
                    false,
                    mapper.writeValueAsBytes(new ServiceAnnouncement()));

            jmdns.registerService(info);

            log.info("registered mdns: {} on {} ({}) with {} ",
                     info.getName(),
                     hostName, hostAddress,
                     info.getNiceTextString());

            info2 = ServiceInfo.create(
                    "_rsp-controller._tcp.local.",
                    deviceId,
                    "",
                    0, 0, 0,
                    false,
                    buildProperTxtRecordMap());

            jmdns.registerService(info2);

            log.info("registered mdns: {} on {} ({}) with {} ",
                     info2.getName(),
                     hostName, hostAddress,
                     parseTxtRecord(info2.getTextBytes()));


        } catch (IOException ioe) {
            log.warn("Unable to register jmdns.", ioe);
        }
    }
    
    public String parseTxtRecord(byte[] _bytes) {
        final StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < _bytes.length) {
            sb.append(System.lineSeparator());
            int size = _bytes[i];
            i++;
            int j = i;
            i += size;
            while(j < i) {
                sb.append((char) _bytes[j]);
                j++;
            }
        }
        return sb.toString();
    }
}

