/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.jmdns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.gateway.ConfigManager;
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

public class JmDNSService {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static class ServiceAnnouncement {
        public boolean sensor_token_required = false;
        public String mqtt_credentials_url = "";
        public String root_cert_url = "";
        public String ntp_host = "";

        public void update() {
            ConfigManager cm = ConfigManager.instance;
            ntp_host = cm.getLocalHost("ntp.server.host");
            sensor_token_required = cm.getProvisionSensorTokenRequired();
            root_cert_url = cm.getProvisionUrlCACert();
            mqtt_credentials_url = cm.getProvisionUrlSensorCredentials();
        }
    }

    private static ObjectMapper mapper = Jackson.getMapper();

    private static String REGISTER_TYPE = "_rfid._tcp.local.";
    private static String REGISTER_NAME_PREFIX = "RFID-Gateway";
    private ServiceAnnouncement serviceAnnouncement;

    private boolean started = false;

    public JmDNSService() {
        REGISTER_TYPE = ConfigManager.instance.getOptString(
            "jmdns.register.type", "_rfid._tcp.local.");
        REGISTER_NAME_PREFIX = ConfigManager.instance.getOptString(
            "jmdns.register.name.prefix", "RFID-Gateway");

        serviceAnnouncement = new ServiceAnnouncement();
        serviceAnnouncement.update();
    }

    public boolean start() {
        if (!started) {
            try {

                JmmDNS.Factory.getInstance().addNetworkTopologyListener(new NetworkChangeListener());
                for (JmDNS jmdns : JmmDNS.Factory.getInstance().getDNS()) {
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
                log.info("Un-registering JmDNS services...");
                JmmDNS.Factory.getInstance().close();
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

    private synchronized void registerService(JmDNS jmdns) {
        try {
            String fullName = REGISTER_NAME_PREFIX + "-" + ConfigManager.instance.getGatewayDeviceId();

            log.info("attempting to register mdns: {} on {} ({})...",
                     fullName,
                     jmdns.getHostName(),
                     jmdns.getInetAddress().getHostAddress());

            final ServiceInfo info = ServiceInfo.create(
                REGISTER_TYPE, fullName, "", 0, 0, 0, false, mapper.writeValueAsBytes(serviceAnnouncement));

            jmdns.registerService(info);

            log.info("registered mdns: {} on {} ({}) with {} ",
                     fullName,
                     jmdns.getHostName(),
                     jmdns.getInetAddress().getHostAddress(),
                     new String(info.getTextBytes()));
        } catch (IOException ioe) {
            log.warn("Unable to register jmdns.", ioe);
        }
    }

}
