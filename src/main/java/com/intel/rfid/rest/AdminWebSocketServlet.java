package com.intel.rfid.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intel.rfid.api.DownstreamManagerSummaryNotification;
import com.intel.rfid.api.InventoryListNotification;
import com.intel.rfid.api.InventorySummaryNotification;
import com.intel.rfid.api.JsonRPCError;
import com.intel.rfid.api.JsonResponseErr;
import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.SchedulerSummaryNotification;
import com.intel.rfid.api.SensorManagerSummaryNotification;
import com.intel.rfid.api.SensorShowInfoNotification;
import com.intel.rfid.api.TagReadSummary;
import com.intel.rfid.api.TagReadSummaryNotification;
import com.intel.rfid.api.TagStateSummary;
import com.intel.rfid.api.TagStateSummaryNotification;
import com.intel.rfid.api.TagStatsUpdateNotification;
import com.intel.rfid.api.UpstreamManagerSummaryNotification;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.ConnectionStateEvent;
import com.intel.rfid.sensor.ReadStateEvent;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminWebSocketServlet
    extends WebSocketServlet
    implements SensorManager.ConnectionStateListener, SensorManager.ReadStateListener {

    public static final String SUBSCRIBE = "subscribe";

    public static final String CMD_SCHEDULER_ACTIVATE = "scheduler_activate";

    public static final String CMD_SENSOR_GET_BIST_RESULTS = "sensor_get_bist_results";
    public static final String CMD_SENSOR_GET_SW_VERSION = "sensor_get_sw_version";
    public static final String CMD_SENSOR_GET_STATE = "sensor_get_state";

    protected static Logger log = LoggerFactory.getLogger(AdminWebSocketServlet.class);

    protected SensorManager sensorMgr;
    protected InventoryManager inventoryMgr;
    protected UpstreamManager upstreamMgr;
    protected DownstreamManager downstreamMgr;
    protected ScheduleManager scheduleMgr;
    protected SocketAdapter socketAdapter;
    protected ObjectMapper mapper;

    private ScheduledExecutorService scheduler;


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
        socketAdapter = new SocketAdapter();
        mapper = Jackson.getMapper();
        scheduler = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator((req, resp) -> socketAdapter);
    }

    @Override
    public void onConnectionStateChange(ConnectionStateEvent _cse) {

    }

    @Override
    public void onReadStateChange(ReadStateEvent _cse) {

    }

    // TODO: consider spawning workers to deal with each connections separately and not bog down Jetty server?

    public class SocketAdapter extends WebSocketAdapter {

        @Override
        public void onWebSocketConnect(Session _session) {
            super.onWebSocketConnect(_session);

            synchronized (subscribedTopics) {
                subscribedTopics.clear();
            }
            if (scheduler.isShutdown() || scheduler.isTerminated()) {
                scheduler = Executors.newScheduledThreadPool(1);
            }
            scheduler.scheduleAtFixedRate(
                AdminWebSocketServlet.this::doSubscriptions, 
                1, 3, TimeUnit.SECONDS);
            

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
            super.onWebSocketClose(_statusCode, _reason);
            synchronized (subscribedTopics) {
                subscribedTopics.clear();
            }
            try {
                ExecutorUtils.shutdownExecutor(log, scheduler);
            } catch (InterruptedException _e) {
                Thread.currentThread().interrupt();
            }
            log.info("Socket Closed: [{}] {}", _statusCode, _reason);
        }

        @Override
        public void onWebSocketError(Throwable _cause) {
            super.onWebSocketError(_cause);
            log.error("Socket Error: {}", _cause);
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
                        onSensorCommand(rootNode, idNode, methodNode);
                        break;
                    default:
                        log.warn("unhandled websocket method: {}", methodNode.asText());
                        break;
                }

            } catch (InterruptedException _e) {
                log.error("interrupted servicing request: {}", _e);
                Thread.currentThread().interrupt();
            } catch (IOException _e) {
                log.error("bad json: {}", _e);
                send(new JsonResponseErr("x", JsonRPCError.Type.PARSE_ERROR, null));
            }
        }
    }

    protected void onSensorCommand(JsonNode _rootNode, JsonNode _idNode, JsonNode _methodNode)
        throws IOException, InterruptedException {

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
        }

        if (handler == null) {
            log.warn("onSensorCommand bad command: {}", _methodNode.asText());
            send(new JsonResponseErr(_idNode.asText(), JsonRPCError.Type.FUNCTION_NOT_SUPPORTED, null));
            return;
        }

        ObjectNode resultNode = mapper.createObjectNode();
        resultNode.put("jsonrpc", "2.0");
        resultNode.put("id", _idNode.asText());
        resultNode.put("method", _methodNode.asText());

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

    protected void onSchedulerActivate(JsonNode _rootNode, JsonNode _idNode) throws IOException {
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
        } catch (IllegalArgumentException | ConfigException _e) {
            log.error("error activating scheduler: {}", _e);
        }

    }

    public enum Topic {
        scheduler_summary,
        inventory_list,
        inventory_summary,
        tag_read_summary,
        tag_state_summary,
        tag_statistics,
        sensor_summary,
        sensor_list,
        sensor_connection_state,
        sensor_read_state,
        sensor_stats, // read rate ? others ?
        sensor_alerts,
        upstream_summary,
        downstream_summary,
    }

    protected void handleSubscription(JsonNode _rootNode, JsonNode _idNode) throws IOException {

        if (_rootNode.get("params") == null || _rootNode.get("params") == null) {
            log.warn("handleSubscription bad request {}", mapper.writeValueAsString(_rootNode));
            return;
        }

        JsonNode topics = _rootNode.get("params");
        if(!topics.isArray()) {
            log.warn("handleSubscription bad topics {}", mapper.writeValueAsString(topics));
            return;
        }

        send(new JsonResponseOK(_idNode.asText(), null));

        for(JsonNode topicNode : topics) {
            Topic topic = Topic.valueOf(topicNode.asText());
            synchronized (subscribedTopics) {
                subscribedTopics.add(topic);
            }
            sendTopic(topic);
        }
    }

    protected final Set<Topic> subscribedTopics = new HashSet<>();

    protected void doSubscriptions() {
        synchronized (subscribedTopics) {
            for(Topic topic :subscribedTopics) {
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
                send(new InventoryListNotification(inventoryMgr.getTags(null)));
                break;
            case inventory_summary:
            case tag_read_summary:
            case tag_state_summary:
                TagStateSummary tagStateSummary = new TagStateSummary();
                TagReadSummary tagReadSummary = new TagReadSummary();
                inventoryMgr.getSummary(tagStateSummary, tagReadSummary);
                if(_topic == Topic.inventory_summary) {
                    send(new InventorySummaryNotification(tagStateSummary, tagReadSummary));
                } else if(_topic == Topic.tag_read_summary) {
                    send(new TagReadSummaryNotification(tagReadSummary));
                } else {
                    send(new TagStateSummaryNotification(tagStateSummary));
                }
                break;
            case tag_statistics:
                send(new TagStatsUpdateNotification(inventoryMgr.getStatsUpdate(null)));
                break;
            case sensor_summary:
                send(new SensorManagerSummaryNotification(sensorMgr.getSummary()));
                break;
            case sensor_list:
                send(new SensorShowInfoNotification(sensorMgr.getBasicInfo()));
                break;
            case upstream_summary:
                send(new UpstreamManagerSummaryNotification(upstreamMgr.getSummary()));
                break;
            case downstream_summary:
                send(new DownstreamManagerSummaryNotification(downstreamMgr.getSummary()));
                break;
            case sensor_alerts:
            case sensor_connection_state:
            case sensor_read_state:
            case sensor_stats:
            default:
                log.warn("unhandled topic {}", _topic);
                break;
        }
    }

    protected void send(Object _obj) {
        if (socketAdapter.getSession() == null || socketAdapter.getRemote() == null) {
            log.warn("socketAdapter is unusable");
            return;
        }

        if (socketAdapter.getSession().isOpen()) {
            try {
                socketAdapter.getRemote().sendStringByFuture(mapper.writeValueAsString(_obj));
            } catch (JsonProcessingException _e) {
                log.warn("error processing notification: {}", _e.getMessage());
            }
        }
    }


}
