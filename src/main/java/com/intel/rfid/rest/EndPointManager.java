/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.rest;

import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.security.SecurityContext;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.upstream.UpstreamManager;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndPointManager {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected Server server;
    protected SensorManager sensorMgr;
    protected InventoryManager inventoryMgr;
    protected UpstreamManager upstreamMgr;
    protected DownstreamManager downstreamMgr;
    protected ScheduleManager scheduleMgr;

    public EndPointManager(SensorManager _sensorMgr,
                           InventoryManager _inventoryMgr,
                           UpstreamManager _upstreamMgr,
                           DownstreamManager _downstreamMgr,
                           ScheduleManager _scheduleMgr) {

        sensorMgr = _sensorMgr;
        inventoryMgr = _inventoryMgr;
        upstreamMgr = _upstreamMgr;
        downstreamMgr = _downstreamMgr;
        scheduleMgr = _scheduleMgr;
    }

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
            addErrorHandlers();

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
        ServletContextHandler context;
        context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);

        ServletHolder holder;

        holder = new ServletHolder(new RootCACertEndPoint());
        context.addServlet(holder, ConfigManager.PROVISION_ROOT_CA_PATH);
        log.info("adding {} on {}", 
                 RootCACertEndPoint.class.getSimpleName(), 
                 ConfigManager.PROVISION_ROOT_CA_PATH);

        holder = new ServletHolder(new SensorCredentialsEndPoint(sensorMgr));
        context.addServlet(holder, ConfigManager.PROVISION_SENSOR_CREDENTIALS_PATH);
        log.info("adding {} on {}", 
                 SensorCredentialsEndPoint.class.getSimpleName(), 
                 ConfigManager.PROVISION_SENSOR_CREDENTIALS_PATH);

        holder = new ServletHolder(new AdminWebSocketServlet(sensorMgr, 
                                                             inventoryMgr,
                                                             upstreamMgr,
                                                             downstreamMgr,
                                                             scheduleMgr));
        context.addServlet(holder, "/web-admin-socket");
        log.info("adding {} on {}",
                 AdminWebSocketServlet.class.getSimpleName(),
                 "/web-admin-socket");

        context.setWelcomeFiles(new String[] {"dashboard.html"});
        holder = new ServletHolder("web-admin-files-servlet", DefaultServlet.class);
        holder.setInitParameter("resourceBase", Env.getWebAdminResourcePath().toString());
        holder.setInitParameter("pathInfoOnly","true");
        context.addServlet(holder, "/" + Env.getWebAdminResourcePath().getFileName().toString() + "/*");
        log.info("adding web-admin-servlet on {}", holder.getInitParameter("resourceBase"));


        holder = new ServletHolder("repo-servlet", DefaultServlet.class);
        holder.setInitParameter("resourceBase", Env.getSensorSoftwareRepoPath().toString());
        holder.setInitParameter("pathInfoOnly","true");
        context.addServlet(holder, "/" + ConfigManager.instance.getSensorRepoBase() + "/*");
        log.info("adding DefaultServlet on {}", holder.getInitParameter("resourceBase"));
    }
    
    protected void addErrorHandlers() {
        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setShowStacks(false);
        server.addBean(errorHandler);
    }
}
