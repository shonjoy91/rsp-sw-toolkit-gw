/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.alerts.ConnectionStateEvent;
import com.intel.rfid.api.JsonNotification;
import com.intel.rfid.api.JsonResponse;
import com.intel.rfid.api.JsonResponseErr;
import com.intel.rfid.api.JsonResponseOK;
import com.intel.rfid.api.JsonRpcError;
import com.intel.rfid.api.data.BooleanResult;
import com.intel.rfid.api.data.InventorySummary;
import com.intel.rfid.api.data.ScheduleRunState;
import com.intel.rfid.api.data.SensorConfigInfo;
import com.intel.rfid.api.data.SensorConnectionStateInfo;
import com.intel.rfid.api.data.SensorReadStateInfo;
import com.intel.rfid.api.data.SensorSoftwareRepoVersions;
import com.intel.rfid.api.data.TagInfo;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.api.sensor.DeviceAlertNotification;
import com.intel.rfid.api.sensor.GeoRegion;
import com.intel.rfid.api.sensor.LEDState;
import com.intel.rfid.api.sensor.OemCfgUpdateNotification;
import com.intel.rfid.api.sensor.RspControllerVersions;
import com.intel.rfid.api.upstream.BehaviorDeleteRequest;
import com.intel.rfid.api.upstream.BehaviorGetAllRequest;
import com.intel.rfid.api.upstream.BehaviorGetRequest;
import com.intel.rfid.api.upstream.BehaviorPutRequest;
import com.intel.rfid.api.upstream.BehaviorResponse;
import com.intel.rfid.api.upstream.ClusterConfigResponse;
import com.intel.rfid.api.upstream.ClusterDeleteConfigRequest;
import com.intel.rfid.api.upstream.ClusterGetConfigRequest;
import com.intel.rfid.api.upstream.ClusterGetTemplateRequest;
import com.intel.rfid.api.upstream.ClusterGetTemplateResponse;
import com.intel.rfid.api.upstream.ClusterSetConfigRequest;
import com.intel.rfid.api.upstream.DownstreamGetMqttStatusRequest;
import com.intel.rfid.api.upstream.DownstreamGetMqttStatusResponse;
import com.intel.rfid.api.upstream.DownstreamMqttStatusNotification;
import com.intel.rfid.api.upstream.GpioClearMappingsRequest;
import com.intel.rfid.api.upstream.GpioSetMappingRequest;
import com.intel.rfid.api.upstream.InventoryActivateMobilityProfileRequest;
import com.intel.rfid.api.upstream.InventoryGetActiveMobilityProfileIdRequest;
import com.intel.rfid.api.upstream.InventoryGetActiveMobilityProfileIdResponse;
import com.intel.rfid.api.upstream.InventoryGetTagInfoRequest;
import com.intel.rfid.api.upstream.InventoryGetTagInfoResponse;
import com.intel.rfid.api.upstream.InventoryGetTagStatsInfoRequest;
import com.intel.rfid.api.upstream.InventoryGetTagStatsInfoResponse;
import com.intel.rfid.api.upstream.InventorySummaryNotification;
import com.intel.rfid.api.upstream.InventoryUnloadRequest;
import com.intel.rfid.api.upstream.MobilityProfileDeleteRequest;
import com.intel.rfid.api.upstream.MobilityProfileGetAllRequest;
import com.intel.rfid.api.upstream.MobilityProfileGetRequest;
import com.intel.rfid.api.upstream.MobilityProfilePutRequest;
import com.intel.rfid.api.upstream.MobilityProfileResponse;
import com.intel.rfid.api.upstream.RspControllerGetAllGeoRegionsRequest;
import com.intel.rfid.api.upstream.RspControllerGetAllGeoRegionsResponse;
import com.intel.rfid.api.upstream.RspControllerGetSensorSWRepoVersionsRequest;
import com.intel.rfid.api.upstream.RspControllerGetSensorSwRepoVersionsResponse;
import com.intel.rfid.api.upstream.RspControllerGetVersionsRequest;
import com.intel.rfid.api.upstream.RspControllerGetVersionsResponse;
import com.intel.rfid.api.upstream.SchedulerGetRunStateRequest;
import com.intel.rfid.api.upstream.SchedulerRunStateNotification;
import com.intel.rfid.api.upstream.SchedulerRunStateResponse;
import com.intel.rfid.api.upstream.SchedulerSetRunStateRequest;
import com.intel.rfid.api.upstream.SensorConfigNotification;
import com.intel.rfid.api.upstream.SensorConnectionStateNotification;
import com.intel.rfid.api.upstream.SensorForceAllDisconnectRequest;
import com.intel.rfid.api.upstream.SensorGetBasicInfoRequest;
import com.intel.rfid.api.upstream.SensorGetBasicInfoResponse;
import com.intel.rfid.api.upstream.SensorGetBistResultsRequest;
import com.intel.rfid.api.upstream.SensorGetDeviceIdsRequest;
import com.intel.rfid.api.upstream.SensorGetDeviceIdsResponse;
import com.intel.rfid.api.upstream.SensorGetGeoRegionRequest;
import com.intel.rfid.api.upstream.SensorGetStateRequest;
import com.intel.rfid.api.upstream.SensorGetVersionsRequest;
import com.intel.rfid.api.upstream.SensorReadStateNotification;
import com.intel.rfid.api.upstream.SensorRebootRequest;
import com.intel.rfid.api.upstream.SensorRemoveRequest;
import com.intel.rfid.api.upstream.SensorResetRequest;
import com.intel.rfid.api.upstream.SensorSetGeoRegionRequest;
import com.intel.rfid.api.upstream.SensorSetLedRequest;
import com.intel.rfid.api.upstream.SensorStateSummaryNotification;
import com.intel.rfid.api.upstream.SensorUpdateSoftwareRequest;
import com.intel.rfid.api.upstream.UpstreamGetMqttStatusRequest;
import com.intel.rfid.api.upstream.UpstreamGetMqttStatusResponse;
import com.intel.rfid.api.upstream.UpstreamMqttStatusNotification;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.gpio.GPIOManager;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.inventory.MobilityProfile;
import com.intel.rfid.inventory.MobilityProfileConfig;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.schedule.SchedulerSummary;
import com.intel.rfid.sensor.ReadStateEvent;
import com.intel.rfid.sensor.ResponseHandler;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import com.intel.rfid.tag.Tag;
import com.intel.rfid.upstream.UpstreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JsonRpcController
        implements SensorManager.ConnectionStateListener,
                   SensorManager.ReadStateListener,
                   SensorManager.OemCfgUpdateListener,
                   SensorManager.SensorDeviceAlertListener,
                   SensorManager.ConfigUpdateListener,
                   ScheduleManager.RunStateListener {

    public interface Callback {
        void sendJsonResponse(JsonResponse _response);

        void sendJsonNotification(JsonNotification _notification);
    }

    public static final String SUBSCRIBE = "subscribe";

    public enum Topic {
        downstream_mqtt_status,
        inventory_summary,
        oem_cfg_update_status,
        scheduler_run_state,
        sensor_alerts,
        sensor_config_notification,
        sensor_connection_state_notification,
        sensor_state_summary,
        sensor_read_state_notification,
        sensor_stats, // read rate ? others ?
        upstream_mqtt_status,
        ;

        protected boolean isEventTopic() {
            boolean b = false;
            switch (this) {
                case scheduler_run_state:
                case sensor_alerts:
                case sensor_config_notification:
                case sensor_connection_state_notification:
                case oem_cfg_update_status:
                case sensor_read_state_notification:
                    b = true;
            }
            return b;
        }

        protected boolean isPeriodicTopic() {
            boolean b = false;
            switch (this) {
                case downstream_mqtt_status:
                case inventory_summary:
                case sensor_state_summary:
                case sensor_stats:
                case upstream_mqtt_status:
                    b = true;
                    break;
            }
            return b;
        }
    }

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    protected ObjectMapper mapper = Jackson.getMapper();
    protected final Set<Topic> periodicTopics = new HashSet<>();
    protected final Callback callback;

    protected ClusterManager clusterMgr;
    protected SensorManager sensorMgr;
    protected GPIOManager gpioMgr;
    protected InventoryManager inventoryMgr;
    protected UpstreamManager upstreamMgr;
    protected DownstreamManager downstreamMgr;
    protected ScheduleManager scheduleMgr;

    public JsonRpcController(Callback _callback,
                             ClusterManager _clusterMgr,
                             SensorManager _sensorMgr,
                             GPIOManager _gpioMgr,
                             InventoryManager _inventoryMgr,
                             UpstreamManager _upstreamMgr,
                             DownstreamManager _downstreamMgr,
                             ScheduleManager _scheduleMgr) {

        callback = _callback;
        clusterMgr = _clusterMgr;
        sensorMgr = _sensorMgr;
        gpioMgr = _gpioMgr;
        inventoryMgr = _inventoryMgr;
        upstreamMgr = _upstreamMgr;
        downstreamMgr = _downstreamMgr;
        scheduleMgr = _scheduleMgr;
    }

    public void start() {
        if (scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler = Executors.newScheduledThreadPool(1);
        }
        scheduler.scheduleAtFixedRate(this::doPeriodicSubscriptions, 1, 3, TimeUnit.SECONDS);
        log.info("{}:{} started",
                 callback.getClass().getSimpleName(),
                 getClass().getSimpleName());
    }

    public void stop() {
        unsubscribeAll();
        try {
            ExecutorUtils.shutdownExecutor(log, scheduler);
        } catch (InterruptedException _e) {
            Thread.currentThread().interrupt();
        }
        log.info("{}:{} stopped",
                 callback.getClass().getSimpleName(),
                 getClass().getSimpleName());
    }

    protected void sendOK(String _id, Object _result) {
        callback.sendJsonResponse(new JsonResponseOK(_id, _result));
    }

    protected void sendResponse(JsonResponse _response) {
        callback.sendJsonResponse(_response);
    }

    protected void sendErr(String _id, JsonRpcError.Type _errType, Object _errData) {
        callback.sendJsonResponse(new JsonResponseErr(_id, _errType, _errData));
    }

    protected void sendNotification(JsonNotification _notification) {
        callback.sendJsonNotification(_notification);
    }

    @Override
    public void onScheduleRunState(ScheduleRunState _current, SchedulerSummary _summary) {
        sendNotification(new SchedulerRunStateNotification(_summary));
    }

    @Override
    public void onConnectionStateChange(ConnectionStateEvent _cse) {
        SensorConnectionStateInfo info = new SensorConnectionStateInfo(_cse.rsp.getDeviceId(),
                                                                       _cse.current);
        sendNotification(new SensorConnectionStateNotification(info));
    }

    @Override
    public void onReadStateEvent(ReadStateEvent _cse) {
        SensorReadStateInfo rsi = new SensorReadStateInfo(_cse.deviceId,
                                                          _cse.previous,
                                                          _cse.current,
                                                          _cse.behaviorId);
        sendNotification(new SensorReadStateNotification(rsi));
    }

    @Override
    public void onSensorDeviceAlert(DeviceAlertNotification _alert) {
        sendNotification(_alert);
    }

    @Override
    public void onOemCfgUpdate(OemCfgUpdateNotification _notification) {
        sendNotification(_notification);
    }

    @Override
    public void onConfigUpdate(SensorConfigInfo _info) {
        sendNotification(new SensorConfigNotification(_info));
    }

    public void inbound(String _request) {
        handleRequest(_request, null);
    }

    public void inbound(byte[] _request) {
        handleRequest(null, _request);
    }

    protected void handleRequest(String _msgString, byte[] _msgBytes) {

        String reqId = "UNKNOWN";
        String reqMethod;

        try {

            JsonNode rootNode;

            if (_msgString != null) {
                rootNode = mapper.readTree(_msgString);
            } else if (_msgBytes != null) {
                rootNode = mapper.readTree(_msgBytes);
            } else {
                log.error("missing data");
                return;
            }

            JsonNode idNode = rootNode.get("id");
            JsonNode methodNode = rootNode.get("method");

            if (idNode == null || methodNode == null) {
                log.warn("input is not JSON RPC request");
                return;
            }
            reqId = idNode.asText();
            reqMethod = methodNode.asText();

            switch (reqMethod) {


                case RspControllerGetAllGeoRegionsRequest.METHOD_NAME:
                case RspControllerGetSensorSWRepoVersionsRequest.METHOD_NAME:
                case RspControllerGetVersionsRequest.METHOD_NAME:
                    handleRspControllerCommand(reqId, reqMethod);
                    break;

                case BehaviorGetRequest.METHOD_NAME:
                case BehaviorGetAllRequest.METHOD_NAME:
                case BehaviorPutRequest.METHOD_NAME:
                case BehaviorDeleteRequest.METHOD_NAME:
                    handleBehaviorCommand(rootNode, reqId, reqMethod);
                    break;

                case ClusterSetConfigRequest.METHOD_NAME:
                case ClusterGetConfigRequest.METHOD_NAME:
                case ClusterGetTemplateRequest.METHOD_NAME:
                case ClusterDeleteConfigRequest.METHOD_NAME:
                    handleClusterCommand(rootNode, reqId, reqMethod);
                    break;

                case GpioClearMappingsRequest.METHOD_NAME:
                case GpioSetMappingRequest.METHOD_NAME:
                    handleGPIOCommand(rootNode, reqId, reqMethod);
                    break;

                case InventoryGetTagStatsInfoRequest.METHOD_NAME:
                case InventoryGetTagInfoRequest.METHOD_NAME:
                case InventoryUnloadRequest.METHOD_NAME:
                case InventoryGetActiveMobilityProfileIdRequest.METHOD_NAME:
                case InventoryActivateMobilityProfileRequest.METHOD_NAME:
                    handleInventoryCommand(rootNode, reqId, reqMethod);
                    break;

                case DownstreamGetMqttStatusRequest.METHOD_NAME: {
                    sendResponse(new DownstreamGetMqttStatusResponse(reqId, downstreamMgr.getMqttStatus()));
                    break;
                }
                case UpstreamGetMqttStatusRequest.METHOD_NAME: {
                    sendResponse(new UpstreamGetMqttStatusResponse(reqId, upstreamMgr.getMqttStatus()));
                    break;
                }

                case MobilityProfileGetRequest.METHOD_NAME:
                case MobilityProfileGetAllRequest.METHOD_NAME:
                case MobilityProfilePutRequest.METHOD_NAME:
                case MobilityProfileDeleteRequest.METHOD_NAME:
                    handleMobilityProfileCommand(rootNode, reqId, reqMethod);
                    break;


                case SchedulerGetRunStateRequest.METHOD_NAME:
                    sendResponse(new SchedulerRunStateResponse(reqId, scheduleMgr.getSummary()));
                    break;
                case SchedulerSetRunStateRequest.METHOD_NAME:
                    handleSchedulerSetRunState(rootNode, reqId);
                    break;


                case SensorGetDeviceIdsRequest.METHOD_NAME: {
                    List<String> sensorDeviceIds = new ArrayList<>();
                    sensorMgr.getDeviceIds(sensorDeviceIds);
                    Collections.sort(sensorDeviceIds);
                    sendResponse(new SensorGetDeviceIdsResponse(reqId, sensorDeviceIds));
                    break;
                }
                case SensorForceAllDisconnectRequest.METHOD_NAME: {
                    sensorMgr.disconnectAll();
                    sendOK(reqId, "OK");
                    break;
                }

                case SensorGetBasicInfoRequest.METHOD_NAME:
                case SensorRemoveRequest.METHOD_NAME:
                    handleSensorLocalCommand(rootNode, reqId, reqMethod);
                    break;

                case SensorGetBistResultsRequest.METHOD_NAME:
                case SensorGetStateRequest.METHOD_NAME:
                case SensorGetVersionsRequest.METHOD_NAME:
                case SensorUpdateSoftwareRequest.METHOD_NAME:
                case SensorGetGeoRegionRequest.METHOD_NAME:
                case SensorSetGeoRegionRequest.METHOD_NAME:
                case SensorSetLedRequest.METHOD_NAME:
                case SensorResetRequest.METHOD_NAME:
                case SensorRebootRequest.METHOD_NAME:
                    handleSensorRoundTripCommand(rootNode, reqId, reqMethod);
                    break;

                case SUBSCRIBE:
                    handleSubscription(rootNode, reqId);
                    break;

                default:
                    log.warn("unhandled JsonRPC method: {}", reqMethod);
                    sendErr(reqId, JsonRpcError.Type.FUNCTION_NOT_SUPPORTED, reqMethod);
                    break;
            }

        } catch (InterruptedException _e) {
            log.error("interrupted servicing request", _e);
            Thread.currentThread().interrupt();
            sendErr(reqId, JsonRpcError.Type.INTERNAL_ERROR, _e.getMessage());
        } catch (IOException | ConfigException _e) {
            log.error("bad request", _e);
            sendErr(reqId, JsonRpcError.Type.PARSE_ERROR, _e.getMessage());
        }
    }

    protected void handleBehaviorCommand(JsonNode _rootNode, String _reqId, String _reqMethod)
            throws IOException {

        List<Behavior> behaviors = new ArrayList<>();

        switch (_reqMethod) {
            case BehaviorPutRequest.METHOD_NAME: {
                BehaviorPutRequest req = mapper.treeToValue(_rootNode, BehaviorPutRequest.class);
                BehaviorConfig.put(req.params);
                behaviors.add(BehaviorConfig.getBehavior(req.params.id));
                break;
            }
            case BehaviorDeleteRequest.METHOD_NAME: {
                BehaviorDeleteRequest req = mapper.treeToValue(_rootNode, BehaviorDeleteRequest.class);
                behaviors.add(BehaviorConfig.deleteBehavior(req.params.behavior_id));
                break;
            }
            case BehaviorGetRequest.METHOD_NAME: {
                BehaviorGetRequest req = mapper.treeToValue(_rootNode, BehaviorGetRequest.class);
                behaviors.add(BehaviorConfig.getBehavior(req.params.behavior_id));
                break;
            }
            case BehaviorGetAllRequest.METHOD_NAME: {
                behaviors.addAll(BehaviorConfig.available().values());
                break;
            }
        }
        // error conditions must throw exception to generate an actual error response
        sendResponse(new BehaviorResponse(_reqId, behaviors));
    }


    protected void handleClusterCommand(JsonNode _rootNode, String _reqId, String _reqMethod)
            throws IOException, ConfigException {

        switch (_reqMethod) {
            case ClusterSetConfigRequest.METHOD_NAME:
                ClusterSetConfigRequest cscr = mapper.treeToValue(_rootNode, ClusterSetConfigRequest.class);
                clusterMgr.loadConfig(cscr.params);
                // fallthrough
            case ClusterGetConfigRequest.METHOD_NAME:
                sendResponse(new ClusterConfigResponse(_reqId, clusterMgr.getConfig()));
                break;
            case ClusterGetTemplateRequest.METHOD_NAME:
                sendResponse(new ClusterGetTemplateResponse(_reqId, clusterMgr.getTemplate()));
                break;
            case ClusterDeleteConfigRequest.METHOD_NAME:
                sendResponse(new ClusterConfigResponse(_reqId, clusterMgr.deleteConfig()));
                break;
        }
    }

    protected void handleRspControllerCommand(String _reqId, String _reqMethod) {

        switch (_reqMethod) {
            case RspControllerGetAllGeoRegionsRequest.METHOD_NAME: {
                sendResponse(new RspControllerGetAllGeoRegionsResponse(_reqId, GeoRegion.asStrings()));
                break;
            }
            case RspControllerGetSensorSWRepoVersionsRequest.METHOD_NAME: {
                List<String> archs = new ArrayList<>();
                SensorSoftwareRepoVersions versions = new SensorSoftwareRepoVersions();
                ConfigManager.instance.getRepoInfo(archs, versions);
                sendResponse(new RspControllerGetSensorSwRepoVersionsResponse(_reqId, versions));
                break;
            }
            case RspControllerGetVersionsRequest.METHOD_NAME: {
                RspControllerVersions versions = new RspControllerVersions();
                versions.software_version = Version.asString();
                sendResponse(new RspControllerGetVersionsResponse(_reqId, versions));
                break;
            }
        }
    }

    protected void handleGPIOCommand(JsonNode _rootNode, String _reqId, String _reqMethod)
            throws JsonProcessingException {

        switch (_reqMethod) {
            case GpioSetMappingRequest.METHOD_NAME:
                GpioSetMappingRequest gcmr = mapper.treeToValue(_rootNode, GpioSetMappingRequest.class);
                if (gpioMgr.addMapping(gcmr.params)) {
                    sendResponse(new JsonResponseOK(_reqId, "OK"));
                } else {
                    sendErr(_reqId, JsonRpcError.Type.INVALID_PARAMETER, _reqMethod);
                }
                break;
            case GpioClearMappingsRequest.METHOD_NAME:
                sendResponse(new JsonResponseOK(_reqId, "OK"));
                break;
        }
    }

    protected void handleInventoryCommand(JsonNode _rootNode, String _reqId, String _reqMethod)
            throws JsonProcessingException {

        switch (_reqMethod) {

            case InventoryGetTagInfoRequest.METHOD_NAME: {
                InventoryGetTagInfoRequest req = mapper.treeToValue(_rootNode, InventoryGetTagInfoRequest.class);
                List<TagInfo> infoList = new ArrayList<>();
                Collection<Tag> tags = inventoryMgr.getTags(req.params.filter_pattern);
                for (Tag t : tags) {
                    infoList.add(new TagInfo(t.getEPC(),
                                             t.getTID(),
                                             t.getState(),
                                             t.getLocation(),
                                             t.getLastRead(),
                                             t.getFacility()));
                }
                sendResponse(new InventoryGetTagInfoResponse(_reqId, infoList));
                break;
            }
            case InventoryGetTagStatsInfoRequest.METHOD_NAME: {
                InventoryGetTagStatsInfoRequest req = mapper.treeToValue(_rootNode,
                                                                         InventoryGetTagStatsInfoRequest.class);
                sendResponse(new InventoryGetTagStatsInfoResponse(_reqId,
                                                                  inventoryMgr.getStatsInfo(req.params.filter_pattern)));
                break;
            }
            case InventoryUnloadRequest.METHOD_NAME: {
                inventoryMgr.unload();
                sendResponse(new JsonResponseOK(_reqId, null));
                break;
            }
            case InventoryGetActiveMobilityProfileIdRequest.METHOD_NAME: {
                sendResponse(new InventoryGetActiveMobilityProfileIdResponse(_reqId, 
                                                                             inventoryMgr.getActiveMobilityProfileId()));
                break;
            }
            case InventoryActivateMobilityProfileRequest.METHOD_NAME: {
                InventoryActivateMobilityProfileRequest req = mapper.treeToValue(_rootNode, InventoryActivateMobilityProfileRequest.class);
                BooleanResult br = inventoryMgr.activateMobilityProfile(req.params.mobility_profile_id);
                if(br.success) {
                    sendOK(_reqId, req.params.mobility_profile_id);
                } else {
                    sendErr(_reqId, JsonRpcError.Type.INVALID_PARAMETER, br);
                }
            }
        }

    }

    protected void handleMobilityProfileCommand(JsonNode _rootNode, String _reqId, String _reqMethod)
            throws IOException {

        BooleanResult boolResult;
        List<MobilityProfile> profiles = new ArrayList<>();

        switch (_reqMethod) {
            case MobilityProfilePutRequest.METHOD_NAME: {
                MobilityProfilePutRequest req = mapper.treeToValue(_rootNode, MobilityProfilePutRequest.class);
                MobilityProfileConfig.put(req.params);
                profiles.add(MobilityProfileConfig.getMobilityProfile(req.params.getId()));
                if(inventoryMgr.getActiveMobilityProfileId().equals(req.params.getId())) {
                    boolResult = inventoryMgr.activateMobilityProfile(req.params.getId());
                    if(!boolResult.success) {
                        log.error("error activating {} mobility profile {}", req.params.getId(), boolResult.message);
                    }
                }
                break;
            }
            case MobilityProfileDeleteRequest.METHOD_NAME: {
                MobilityProfileDeleteRequest req = mapper.treeToValue(_rootNode, MobilityProfileDeleteRequest.class);
                profiles.add(MobilityProfileConfig.deleteMobilityProfile(req.params.mobility_profile_id));
                if(inventoryMgr.getActiveMobilityProfileId().equals(req.params.mobility_profile_id)) {
                    boolResult = inventoryMgr.activateMobilityProfile(MobilityProfile.DEFAULT_ID);
                    if(!boolResult.success) {
                        log.error("error activating default mobility profile {}", boolResult.message);
                    }
                }
                break;
            }
            case MobilityProfileGetRequest.METHOD_NAME: {
                MobilityProfileGetRequest req = mapper.treeToValue(_rootNode, MobilityProfileGetRequest.class);
                profiles.add(MobilityProfileConfig.getMobilityProfile(req.params.mobility_profile_id));
                break;
            }
            case MobilityProfileGetAllRequest.METHOD_NAME: {
                profiles.addAll(MobilityProfileConfig.available().values());
                break;
            }
        }
        // error conditions must throw exception to generate an actual error response
        sendResponse(new MobilityProfileResponse(_reqId, profiles));
    }



    protected void handleSensorLocalCommand(JsonNode _rootNode, String _reqId, String _reqMethod) {

        if (_rootNode.get("params") == null || _rootNode.get("params").get("device_id") == null) {
            log.warn("handleSensorLocalCommand incorrect request");
            return;
        }

        String deviceId = _rootNode.get("params").get("device_id").asText();
        SensorPlatform sensor = sensorMgr.getSensor(deviceId);
        if (sensor == null) {
            log.warn("handleSensorLocalCommand bad device id: {}", deviceId);
            sendErr(_reqId, JsonRpcError.Type.INVALID_PARAMETER, null);
            return;
        }

        switch (_reqMethod) {
            case SensorGetBasicInfoRequest.METHOD_NAME: {
                sendResponse(new SensorGetBasicInfoResponse(_reqId, sensor.getBasicInfo()));
                break;
            }
            case SensorRemoveRequest.METHOD_NAME: {
                BooleanResult result = sensorMgr.remove(sensor);
                if (!result.success) {
                    sendErr(_reqId, JsonRpcError.Type.WRONG_STATE, result.message);
                }
                sendOK(_reqId, result.message);
                break;
            }
        }
    }

    protected void handleSensorRoundTripCommand(JsonNode _rootNode, String _reqId, String _reqMethod)
            throws InterruptedException {

        if (_rootNode.get("params") == null || _rootNode.get("params").get("device_id") == null) {
            log.warn("handleSensorRoundTripCommand incorrect request");
            return;
        }

        String deviceId = _rootNode.get("params").get("device_id").asText();
        SensorPlatform sensor = sensorMgr.getSensor(deviceId);
        if (sensor == null) {
            log.warn("handleSensorRoundTripCommand bad device id: {}", deviceId);
            sendErr(_reqId, JsonRpcError.Type.INVALID_PARAMETER, null);
            return;
        }

        ResponseHandler handler = null;
        switch (_reqMethod) {
            case SensorGetBistResultsRequest.METHOD_NAME:
                handler = sensor.getBISTResults();
                break;
            case SensorGetStateRequest.METHOD_NAME:
                handler = sensor.getState();
                break;
            case SensorGetVersionsRequest.METHOD_NAME:
                handler = sensor.getSoftwareVersion();
                break;
            case SensorUpdateSoftwareRequest.METHOD_NAME:
                handler = sensor.softwareUpdate();
                break;
            case SensorGetGeoRegionRequest.METHOD_NAME:
                handler = sensor.getGeoRegion();
                break;
            case SensorResetRequest.METHOD_NAME:
                handler = sensor.reset();
                break;
            case SensorRebootRequest.METHOD_NAME:
                handler = sensor.reboot();
                break;
            case SensorSetLedRequest.METHOD_NAME:
                try {
                    String ledState = _rootNode.get("params").get("led_state").asText();
                    handler = sensor.setLED(LEDState.valueOf(ledState));
                } catch (Exception _e) {
                    log.error("error:", _e);
                }
                break;
            case SensorSetGeoRegionRequest.METHOD_NAME:
                try {
                    String region = _rootNode.get("params").get("region").asText();
                    handler = sensor.setGeoRegion(GeoRegion.valueOf(region));
                } catch (Exception _e) {
                    log.error("error:", _e);
                }
                break;
        }

        if (handler == null) {
            log.warn("handleSensorRoundTripCommand bad command: {}", _reqMethod);
            sendErr(_reqId, JsonRpcError.Type.FUNCTION_NOT_SUPPORTED, null);
            return;
        }

        handler.waitForResponse(5, TimeUnit.SECONDS);
        if (handler.getResult() != null) {
            sendOK(_reqId, handler.getResult());
        } else {
            sendErr(_reqId, JsonRpcError.Type.INTERNAL_ERROR, handler.getError());
        }

    }

    protected void handleSchedulerSetRunState(JsonNode _rootNode, String _reqId)
            throws IOException, IllegalArgumentException {
        SchedulerSetRunStateRequest req = mapper.treeToValue(_rootNode, SchedulerSetRunStateRequest.class);
        scheduleMgr.setRunState(req.params.run_state);
        ScheduleRunState actualRunState = scheduleMgr.getRunState();
        if (req.params.run_state == actualRunState) {
            sendResponse(new SchedulerRunStateResponse(_reqId, scheduleMgr.getSummary()));
        } else {
            sendErr(_reqId,
                    JsonRpcError.Type.INTERNAL_ERROR,
                    "chaging run state failed, scheduler run state: " + actualRunState.toString());
        }

    }

    protected void handleSubscription(JsonNode _rootNode, String _reqId) throws IOException {

        if (_rootNode.get("params") == null || _rootNode.get("params") == null) {
            log.warn("handleSubscription bad request {}", mapper.writeValueAsString(_rootNode));
            return;
        }

        JsonNode topics = _rootNode.get("params");
        if (!topics.isArray()) {
            log.warn("handleSubscription bad topics {}", mapper.writeValueAsString(topics));
            return;
        }

        sendOK(_reqId, null);

        for (JsonNode topicNode : topics) {

            Topic topic;
            try {
                topic = Topic.valueOf(topicNode.asText());
            } catch (IllegalArgumentException _e) {
                log.info("unknown subscription topic {}", topicNode.asText());
                continue;
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
            case scheduler_run_state:
                scheduleMgr.addRunStateListener(this);
                break;
            case sensor_alerts:
                sensorMgr.addDeviceAlertListener(this);
                break;
            case sensor_config_notification:
                sensorMgr.addConfigUpdateListener(this);
                break;
            case sensor_connection_state_notification:
                sensorMgr.addConnectionStateListener(this);
                break;
            case oem_cfg_update_status:
                sensorMgr.addOemCfgUpdateListener(this);
                break;
            case sensor_read_state_notification:
                sensorMgr.addReadStateListener(this);
                break;
        }

    }

    protected void unsubscribeAll() {
        scheduleMgr.removeRunStateListener(this);
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
            case scheduler_run_state:
                sendNotification(new SchedulerRunStateNotification(scheduleMgr.getSummary()));
                break;
            case inventory_summary:
                InventorySummary inventorySummary = new InventorySummary();
                inventoryMgr.getSummary(inventorySummary);
                sendNotification(new InventorySummaryNotification(inventorySummary));
                break;
            case sensor_state_summary:
                sendNotification(new SensorStateSummaryNotification(sensorMgr.getSummary()));
                break;
            case upstream_mqtt_status:
                sendNotification(new UpstreamMqttStatusNotification(upstreamMgr.getMqttStatus()));
                break;
            case downstream_mqtt_status:
                sendNotification(new DownstreamMqttStatusNotification(downstreamMgr.getMqttStatus()));
                break;
        }
    }

}
