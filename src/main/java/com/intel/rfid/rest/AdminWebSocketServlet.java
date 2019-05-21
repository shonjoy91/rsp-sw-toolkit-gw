/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intel.rfid.api.upstream.EpcFilterPatternNotification;
import com.intel.rfid.api.data.GeoRegion;
import com.intel.rfid.api.downstream.OemCfgUpdateNotification;
import com.intel.rfid.api.upstream.ReadStateNotification;
import com.intel.rfid.api.data.RepoVersions;
import com.intel.rfid.api.upstream.SensorBasicInfoNotification;
import com.intel.rfid.api.upstream.SensorConfigNotification;
import com.intel.rfid.api.upstream.SensorConnectionStateNotification;
import com.intel.rfid.api.common.DeviceAlertNotification;
import com.intel.rfid.api.common.JsonRPCError;
import com.intel.rfid.api.common.JsonResponseErr;
import com.intel.rfid.api.common.JsonResponseOK;
import com.intel.rfid.api.downstream.DownstreamManagerSummaryNotification;
import com.intel.rfid.api.upstream.InventoryListNotification;
import com.intel.rfid.api.upstream.InventorySummaryNotification;
import com.intel.rfid.api.upstream.SchedulerSummaryNotification;
import com.intel.rfid.api.upstream.SensorManagerSummaryNotification;
import com.intel.rfid.api.upstream.TagReadSummaryNotification;
import com.intel.rfid.api.upstream.TagStateSummaryNotification;
import com.intel.rfid.api.upstream.TagStatsUpdateNotification;
import com.intel.rfid.api.upstream.UpstreamManagerSummaryNotification;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.inventory.TagReadSummary;
import com.intel.rfid.inventory.TagStateSummary;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.api.data.ConnectionStateEvent;
import com.intel.rfid.api.data.ReadStateEvent;
import com.intel.rfid.sensor.ResponseHandler;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import com.intel.rfid.upstream.UpstreamManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminWebSocketServlet
        extends WebSocketServlet {

    public static final String CMD_SCHEDULER_ACTIVATE = "scheduler_activate";

    public static final String CMD_SENSOR_GET_BIST_RESULTS = "sensor_get_bist_results";
    public static final String CMD_SENSOR_GET_SW_VERSION = "sensor_get_sw_version";
    public static final String CMD_SENSOR_GET_STATE = "sensor_get_state";
    public static final String CMD_SENSOR_GET_GEO_REGION = "sensor_get_geo_region";
    public static final String CMD_SENSOR_SET_GEO_REGION = "sensor_set_geo_region";
    public static final String CMD_SENSOR_SOFTWARE_UPDATE = "sensor_software_update";

    public static final String CMD_OEM_GET_AVAILABLE_REGIONS = "oem_get_available_regions";
    public static final String CMD_REPO_GET_VERSIONS = "repo_get_versions";

    public static final String CMD_SET_EPC_FILTER_PATTERN = "set_epc_filter_pattern";
    public static final String CMD_GET_EPC_FILTER_PATTERN = "get_epc_filter_pattern";
    protected String epcFilterPattern;

    public static final String SUBSCRIBE = "subscribe";

    public enum Topic {
        downstream_manager_summary,
        inventory_list,
        inventory_summary,
        oem_cfg_update_status,
        scheduler_summary,
        sensor_alerts,
        sensor_basic_info,
        sensor_config,
        sensor_connection_state,
        sensor_manager_summary,
        sensor_read_state,
        sensor_stats, // read rate ? others ?
        tag_read_summary,
        tag_state_summary,
        tag_statistics,
        upstream_manager_summary,
        ;

        protected boolean isEventTopic() {
            boolean b = false;
            switch (this) {
                case sensor_alerts:
                case sensor_config:
                case sensor_connection_state:
                case oem_cfg_update_status:
                case sensor_read_state:
                    b = true;
            }
            return b;
        }

        protected boolean isPeriodicTopic() {
            boolean b = false;
            switch (this) {
                case downstream_manager_summary:
                case inventory_list:
                case inventory_summary:
                case scheduler_summary:
                case sensor_manager_summary:
                case sensor_stats:
                case tag_read_summary:
                case tag_state_summary:
                case tag_statistics:
                case upstream_manager_summary:
                    b = true;
                    break;
            }
            return b;
        }
    }

    protected static Logger log = LoggerFactory.getLogger(AdminWebSocketServlet.class);

    protected SensorManager sensorMgr;
    protected InventoryManager inventoryMgr;
    protected UpstreamManager upstreamMgr;
    protected DownstreamManager downstreamMgr;
    protected ScheduleManager scheduleMgr;

    public AdminWebSocketServlet(SensorManager _sensorMgr,
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

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator((req, resp) -> new SocketAdapter());
    }

    public class SocketAdapter
            extends WebSocketAdapter
            implements SensorManager.ConnectionStateListener,
                       SensorManager.ReadStateListener,
                       SensorManager.OemCfgUpdateListener,
                       SensorManager.SensorDeviceAlertListener,
                       SensorManager.ConfigUpdateListener {

        protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ;
        protected ObjectMapper mapper = Jackson.getMapper();
        protected final Set<Topic> periodicTopics = new HashSet<>();

        @Override
        public void onWebSocketConnect(Session _session) {
            super.onWebSocketConnect(_session);
            if (scheduler.isShutdown() || scheduler.isTerminated()) {
                scheduler = Executors.newScheduledThreadPool(1);
            }
            scheduler.scheduleAtFixedRate(this::doPeriodicSubscriptions, 1, 3, TimeUnit.SECONDS);
            log.info("Socket Connected: {}", _session.getRemoteAddress());
        }

        @Override
        public void onWebSocketText(String _msg) {
            if (isConnected()) {
                onInboundText(_msg);
            }
        }

        @Override
        public void onWebSocketClose(int _statusCode, String _reason) {
            unsubscribeAll();
            try {
                ExecutorUtils.shutdownExecutor(log, scheduler);
            } catch (InterruptedException _e) {
                Thread.currentThread().interrupt();
            }
            super.onWebSocketClose(_statusCode, _reason);
            log.info("Socket Closed: [{}] {}", _statusCode, _reason);
        }

        @Override
        public void onWebSocketError(Throwable _cause) {
            super.onWebSocketError(_cause);
            log.error("Socket Error ", _cause);
        }

        protected void onInboundText(String _text) {
            try {

                log.info("onInboundText: {}", _text);

                JsonNode rootNode = mapper.readTree(_text);
                JsonNode idNode = rootNode.get("id");
                JsonNode methodNode = rootNode.get("method");

                if (idNode == null || methodNode == null) {
                    log.warn("input is not JSON RPC request: {}", _text);
                    return;
                }

                switch (methodNode.asText()) {
                    case SUBSCRIBE:
                        handleSubscription(rootNode, idNode);
                        break;
                    case CMD_SCHEDULER_ACTIVATE:
                        onSchedulerActivate(rootNode, idNode);
                        break;
                    case CMD_SENSOR_GET_BIST_RESULTS:
                    case CMD_SENSOR_GET_STATE:
                    case CMD_SENSOR_GET_SW_VERSION:
                    case CMD_SENSOR_SOFTWARE_UPDATE:
                    case CMD_SENSOR_GET_GEO_REGION:
                    case CMD_SENSOR_SET_GEO_REGION:
                        onSensorCommand(rootNode, idNode, methodNode);
                        break;
                    case CMD_OEM_GET_AVAILABLE_REGIONS:
                        send(new JsonResponseOK(idNode.asText(), GeoRegion.values()));
                        break;
                    case CMD_REPO_GET_VERSIONS:
                        onRepoGetVersions(idNode);
                        break;
                    case CMD_SET_EPC_FILTER_PATTERN:
                        epcFilterPattern = rootNode.get("params").get("epc_filter_pattern").asText();
                    case CMD_GET_EPC_FILTER_PATTERN:
                        send(new JsonResponseOK(idNode.asText(), new EpcFilterPatternNotification(epcFilterPattern)));
                        break;
                    default:
                        log.warn("unhandled websocket method: {}", methodNode.asText());
                        break;
                }

            } catch (InterruptedException _e) {
                log.error("interrupted servicing request", _e);
                Thread.currentThread().interrupt();
            } catch (IOException _e) {
                log.error("bad json", _e);
                send(new JsonResponseErr("x", JsonRPCError.Type.PARSE_ERROR, null));
            }
        }

        protected void onSensorCommand(JsonNode _rootNode, JsonNode _idNode, JsonNode _methodNode)
                throws InterruptedException {

            if (_rootNode.get("params") == null || _rootNode.get("params").get("sensor_id") == null) {
                log.warn("onSensorCommand incorrect request");
                return;
            }

            String sensorId = _rootNode.get("params").get("sensor_id").asText();
            SensorPlatform sensor = sensorMgr.getRSP(sensorId);
            if (sensor == null) {
                log.warn("onSensorCommand bad sensor id: {}", sensorId);
                send(new JsonResponseErr(_idNode.asText(), JsonRPCError.Type.INVALID_PARAMETER, null));
                return;
            }

            ResponseHandler handler = null;
            switch (_methodNode.asText()) {
                case CMD_SENSOR_GET_BIST_RESULTS:
                    handler = sensor.getBISTResults();
                    break;
                case CMD_SENSOR_GET_STATE:
                    handler = sensor.getState();
                    break;
                case CMD_SENSOR_GET_SW_VERSION:
                    handler = sensor.getSoftwareVersion();
                    break;
                case CMD_SENSOR_SOFTWARE_UPDATE:
                    handler = sensor.softwareUpdate();
                    break;
                case CMD_SENSOR_GET_GEO_REGION:
                    handler = sensor.getGeoRegion();
                    break;
                case CMD_SENSOR_SET_GEO_REGION:
                    try {
                        String region = _rootNode.get("params").get("region").asText();
                        handler = sensor.setGeoRegion(GeoRegion.valueOf(region));
                    } catch (Exception _e) {
                        log.error("error:", _e);
                    }
                    break;
            }
            if (handler == null) {
                log.warn("onSensorCommand bad command: {}", _methodNode.asText());
                send(new JsonResponseErr(_idNode.asText(), JsonRPCError.Type.FUNCTION_NOT_SUPPORTED, null));
                return;
            }

            ObjectNode resultNode = mapper.createObjectNode();
            resultNode.put("jsonrpc", "2.0");
            resultNode.put("id", _idNode.asText());

            handler.waitForResponse(5, TimeUnit.SECONDS);
            if (handler.getResult() != null) {
                resultNode.set("result", handler.getResult());
                send(resultNode);
            } else if (handler.getError() != null) {
                resultNode.set("error", handler.getError());
                send(resultNode);
            } else {
                send(new JsonResponseErr(_idNode.asText(), JsonRPCError.Type.INTERNAL_ERROR, null));
            }

        }

        protected void onSchedulerActivate(JsonNode _rootNode, JsonNode _idNode) {
            if (_rootNode.get("params") == null || _rootNode.get("params").get("run_state") == null) {
                log.warn("onSchedulerActivate incorrect request");
                return;
            }

            try {
                String newRunState = _rootNode.get("params").get("run_state").asText();
                ScheduleManager.RunState runState = ScheduleManager.RunState.valueOf(newRunState);
                scheduleMgr.activate(runState);
                send(new JsonResponseOK(_idNode.asText(), null));
                send(new SchedulerSummaryNotification(scheduleMgr.getSummary()));
            } catch (IllegalArgumentException _e) {
                log.error("error activating scheduler", _e);
            }

        }

        protected void onRepoGetVersions(JsonNode _idNode) {
            List<String> archs = new ArrayList<>();
            RepoVersions versions = new RepoVersions();
            ConfigManager.instance.getRepoInfo(archs, versions);
            try {
                log.info("{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(versions));
            } catch (Exception e) {
                log.error("{}", e.getMessage());
            }
            send(new JsonResponseOK(_idNode.asText(), versions));
        }

        protected void handleSubscription(JsonNode _rootNode, JsonNode _idNode) throws IOException {

            if (_rootNode.get("params") == null || _rootNode.get("params") == null) {
                log.warn("handleSubscription bad request {}", mapper.writeValueAsString(_rootNode));
                return;
            }

            JsonNode topics = _rootNode.get("params");
            if (!topics.isArray()) {
                log.warn("handleSubscription bad topics {}", mapper.writeValueAsString(topics));
                return;
            }

            send(new JsonResponseOK(_idNode.asText(), null));

            for (JsonNode topicNode : topics) {

                Topic topic;
                try {
                    topic = Topic.valueOf(topicNode.asText());
                } catch (IllegalArgumentException _e) {
                    log.info("unknown subscription topic {}", topicNode.asText());
                    continue;
                }

                if (topic == Topic.inventory_list || topic == Topic.tag_statistics) {
                    send(new EpcFilterPatternNotification(epcFilterPattern));
                }

                if (topic.isEventTopic()) {
                    subscribeEventTopic(topic);
                } else {
                    sendTopic(topic);
                    if (topic.isPeriodicTopic()) {
                        synchronized (periodicTopics) {
                            periodicTopics.add(topic);
                        }
                    }
                }
            }
        }

        protected void subscribeEventTopic(Topic _topic) {
            switch (_topic) {
                case sensor_alerts:
                    sensorMgr.addDeviceAlertListener(this);
                    break;
                case sensor_config:
                    sensorMgr.addConfigUpdateListener(this);
                    break;
                case sensor_connection_state:
                    sensorMgr.addConnectionStateListener(this);
                    break;
                case oem_cfg_update_status:
                    sensorMgr.addOemCfgUpdateListener(this);
                    break;
                case sensor_read_state:
                    sensorMgr.addReadStateListener(this);
                    break;
            }

        }

        protected void unsubscribeAll() {
            sensorMgr.removeDeviceAlertListener(this);
            sensorMgr.removeConfigUpdateListener(this);
            sensorMgr.removeConnectionStateListener(this);
            sensorMgr.removeOemCfgUpdateListener(this);
            sensorMgr.removeReadStateListener(this);
        }

        protected void doPeriodicSubscriptions() {
            synchronized (periodicTopics) {
                for (Topic topic : periodicTopics) {
                    sendTopic(topic);
                }
            }
        }

        protected void sendTopic(Topic _topic) {

            switch (_topic) {
                case scheduler_summary:
                    send(new SchedulerSummaryNotification(scheduleMgr.getSummary()));
                    break;
                case inventory_list:
                    send(new InventoryListNotification(inventoryMgr.getTags(epcFilterPattern)));
                    break;
                case inventory_summary:
                case tag_read_summary:
                case tag_state_summary:
                    TagStateSummary tagStateSummary = new TagStateSummary();
                    TagReadSummary tagReadSummary = new TagReadSummary();
                    inventoryMgr.getSummary(tagStateSummary, tagReadSummary);
                    if (_topic == Topic.inventory_summary) {
                        send(new InventorySummaryNotification(tagStateSummary, tagReadSummary));
                    } else if (_topic == Topic.tag_read_summary) {
                        send(new TagReadSummaryNotification(tagReadSummary));
                    } else {
                        send(new TagStateSummaryNotification(tagStateSummary));
                    }
                    break;
                case tag_statistics:
                    send(new TagStatsUpdateNotification(inventoryMgr.getStatsUpdate(epcFilterPattern)));
                    break;
                case sensor_manager_summary:
                    send(new SensorManagerSummaryNotification(sensorMgr.getSummary()));
                    break;
                case sensor_basic_info:
                    send(new SensorBasicInfoNotification(sensorMgr.getBasicInfo()));
                    break;
                case upstream_manager_summary:
                    send(new UpstreamManagerSummaryNotification(upstreamMgr.getSummary()));
                    break;
                case downstream_manager_summary:
                    send(new DownstreamManagerSummaryNotification(downstreamMgr.getSummary()));
                    break;
            }
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

        @Override
        public void onConnectionStateChange(ConnectionStateEvent _cse) {
            send(new SensorConnectionStateNotification(_cse));
        }

        @Override
        public void onReadStateChange(ReadStateEvent _cse) {
            send(new ReadStateNotification(_cse));
        }

        @Override
        public void onSensorDeviceAlert(DeviceAlertNotification _alert) {
            send(_alert);
        }

        @Override
        public void onOemCfgUpdate(OemCfgUpdateNotification _notification) {
            send(_notification);
        }

        @Override
        public void onConfigUpdate(SensorConfigNotification _notification) {
            send(_notification);
        }
    }


}
