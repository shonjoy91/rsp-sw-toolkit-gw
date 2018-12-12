/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.rest;

import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.security.SecurityContext;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndPointManager {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected Server server;

    public boolean start() {

        if (server != null) {
            log.error("previous server was not stopped correctly");
            return false;
        }

        try {

            server = new Server();
            server.setStopAtShutdown(true);
            server.setStopTimeout(3000);

            addHttpConnector();
            addTlsConnector();
            addEndPoints();

            server.start();

            log.info(getClass().getSimpleName() + " started");
            return true;
        } catch (Exception _e) {
            log.error("failed to start: {}", _e.getMessage());
        }
        return false;
    }

    public boolean stop() {
        if (server != null) {
            try {
                server.stop();
                server.join();
            } catch (Exception e) {
                log.error("Failed to stop end point server: ", e);
            } finally {
                server.destroy();
                server = null;
            }
        }
        log.info(getClass().getSimpleName() + " stopped");
        return true;
    }

    protected void addHttpConnector() {
        // plain ol' http
        ServerConnector httpConnector = new ServerConnector(server);
        httpConnector.setPort(ConfigManager.instance.getProvisionHttpPort());
        server.addConnector(httpConnector);
    }


    protected void addTlsConnector() {
        try {
            // TLS connection
            HttpConfiguration httpCfg = new HttpConfiguration();
            httpCfg.addCustomizer(new SecureRequestCustomizer());
            HttpConnectionFactory httpCnxFactory = new HttpConnectionFactory(httpCfg);
            SslConnectionFactory sslCnxFactory =
                new SslConnectionFactory(SecurityContext.instance().getJettySslContextFactory(),
                                         "http/1.1");
            ServerConnector sslConnector = new ServerConnector(server, sslCnxFactory, httpCnxFactory);
            sslConnector.setPort(ConfigManager.instance.getProvisionTlsPort());
            server.addConnector(sslConnector);

        } catch (ConfigException _e) {
            log.error("Unable to add TLS connector: {}", _e.getMessage());
        }
    }

    protected void addEndPoints() {
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.addServlet(RootCACertEndPoint.class, ConfigManager.PROVISION_ROOT_CA_PATH);
        ctx.addServlet(SensorCredentialsEndPoint.class, ConfigManager.PROVISION_SENSOR_CREDENTIALS_PATH);
        server.setHandler(ctx);
    }
}
