/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.JsonResponse;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.gateway.JsonRpcController;
import com.intel.rfid.gpio.GPIOManager;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.upstream.UpstreamManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminWebSocketServlet
        extends WebSocketServlet {

    protected static Logger log = LoggerFactory.getLogger(AdminWebSocketServlet.class);

    protected ClusterManager clusterMgr;
    protected SensorManager sensorMgr;
    protected GPIOManager gpioMgr;
    protected InventoryManager inventoryMgr;
    protected UpstreamManager upstreamMgr;
    protected DownstreamManager downstreamMgr;
    protected ScheduleManager scheduleMgr;

    public AdminWebSocketServlet(ClusterManager _clusterMgr, 
                                 SensorManager _sensorMgr,
                                 GPIOManager _gpioMgr,
                                 InventoryManager _inventoryMgr,
                                 UpstreamManager _upstreamMgr,
                                 DownstreamManager _downstreamMgr,
                                 ScheduleManager _scheduleMgr) {
        clusterMgr = _clusterMgr;
        sensorMgr = _sensorMgr;
        gpioMgr = _gpioMgr;
        inventoryMgr = _inventoryMgr;
        upstreamMgr = _upstreamMgr;
        downstreamMgr = _downstreamMgr;
        scheduleMgr = _scheduleMgr;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator((req, resp) -> new SocketAdapter());
    }

    public class SocketAdapter
            extends WebSocketAdapter
            implements JsonRpcController.Callback {

        protected ObjectMapper mapper = Jackson.getMapper();
        protected JsonRpcController controller;

        public SocketAdapter() {
            controller = new JsonRpcController(this,
                                               clusterMgr,
                                               sensorMgr,
                                               gpioMgr,
                                               inventoryMgr,
                                               upstreamMgr,
                                               downstreamMgr,
                                               scheduleMgr);
        }

        @Override
        public void onWebSocketConnect(Session _session) {
            super.onWebSocketConnect(_session);
            controller.start();
            log.info("Socket Connected: {}", _session.getRemoteAddress());
        }

        @Override
        public void onWebSocketText(String _msg) {
            if (isConnected()) {
                controller.inbound(_msg);
            }
        }

        @Override
        public void onWebSocketClose(int _statusCode, String _reason) {
            controller.stop();
            super.onWebSocketClose(_statusCode, _reason);
            log.info("Socket Closed: [{}] {}", _statusCode, _reason);
        }

        @Override
        public void onWebSocketError(Throwable _cause) {
            super.onWebSocketError(_cause);
            log.error("Socket Error ", _cause);
        }

        @Override
        public void sendJsonResponse(JsonResponse _response) {
            send(_response);
        }

        @Override
        public void sendJsonNotification(JsonNotification _notification) {
            send(_notification);
        }

        protected void send(Object _obj) {
            if (getSession() == null || getRemote() == null) {
                log.warn("socketAdapter is unusable");
                return;
            }

            if (getSession().isOpen()) {
                try {
                    getRemote().sendStringByFuture(mapper.writeValueAsString(_obj));
                } catch (JsonProcessingException _e) {
                    log.warn("error processing notification: {}", _e.getMessage());
                }
            }
        }
    }


}
