/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import com.intel.rfid.gateway.Version;
import com.intel.rfid.api.upstream.*;
import com.intel.rfid.api.common.*;
import com.intel.rfid.api.data.MQTTSummary;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.cluster.ClusterManager;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.schedule.ScheduleManager;
import com.intel.rfid.sensor.SensorManager;
import com.intel.rfid.sensor.SensorPlatform;
import com.intel.rfid.gpio.GPIODevice;
import com.intel.rfid.gpio.GPIOManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class UpstreamManager
    implements InventoryManager.UpstreamEventListener, MQTTUpstream.Dispatch {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected String deviceId;
    protected MQTTUpstream mqttUpstream;

    protected ClusterManager clusterMgr;
    protected SensorManager sensorMgr;
    protected GPIOManager gpioMgr;
    protected ScheduleManager scheduleMgr;
    protected InventoryManager inventoryMgr;
    protected DownstreamManager downstreamMgr;

    protected ObjectMapper mapper = Jackson.getMapper();

    public UpstreamManager(ClusterManager _clusterMgr,
                           SensorManager _sensorMgr,
                           GPIOManager _gpioMgr,
                           ScheduleManager _scheduleMgr,
                           InventoryManager _inventoryMgr,
                           DownstreamManager _downstreamMgr) {

        clusterMgr = _clusterMgr;
        sensorMgr = _sensorMgr;
        gpioMgr = _gpioMgr;
        scheduleMgr = _scheduleMgr;
        inventoryMgr = _inventoryMgr;
        downstreamMgr = _downstreamMgr;

        ConfigManager cm = ConfigManager.instance;
        deviceId = cm.getGatewayDeviceId();
        mqttUpstream = new MQTTUpstream(this);
    }

    public boolean start() {
        mqttUpstream.start();
        log.info(getClass().getSimpleName() + " started");
        return true;
    }

    public boolean stop() {
        mqttUpstream.stop();
        log.info(getClass().getSimpleName() + " stopped");
        return true;
    }

    @Override
    public void onUpstreamEvent(UpstreamInventoryEvent _uie) {
        send(_uie);
    }

    @Override
    public void onMessage(String _topic, MqttMessage _msg) {
    
        try {
            JsonNode rootNode = mapper.readTree(_msg.getPayload());
            JsonNode idNode = rootNode.get("id");
            JsonNode methodNode = rootNode.get("method");

            if ((rootNode != null) && (idNode != null) && (methodNode != null)) {
                JsonResponse rsp = handleRequest(methodNode.asText(), idNode.asText(), rootNode);
                sendResponse(rsp);
            } else {
                log.warn("unhandled json msg: {}", _msg.toString());
            }
        } catch (Exception e) {
            log.error("error handling message", e);
        }
    }

    protected JsonResponse handleRequest(String _method, String _id, JsonNode _rootNode) {
        JsonResponse rsp = null;

        try {
            switch (_method) {

                case GetDownstreamSummaryRequest.METHOD_NAME: {
                    rsp = new GetDownstreamSummaryResponse(_id, downstreamMgr.getSummary());
                    break;
                }
                case GetUpstreamSummaryRequest.METHOD_NAME: {
                    rsp = new GetUpstreamSummaryResponse(_id, getSummary());
                    break;
                }
                case SetGPIOMappingRequest.METHOD_NAME: {
                    SetGPIOMappingRequest req = mapper.treeToValue(_rootNode, SetGPIOMappingRequest.class);
                    if (gpioMgr.addMapping(req.params)) {
                        rsp = new JsonResponseOK(_id, Boolean.TRUE);
                    } else {
                        rsp = new JsonResponseErr(_id, JsonRPCError.Type.INTERNAL_ERROR, "Failed to add mapping");
                    }
                    break;
                }
                case ClearGPIOMappingsRequest.METHOD_NAME: {
                    gpioMgr.clearMappings();
                    rsp = new JsonResponseOK(_id, Boolean.TRUE);
                    break;
                }
                case GetSensorInfoRequest.METHOD_NAME: {
                    rsp = new GetSensorInfoResponse(_id, sensorMgr.getBasicInfo());
                    break;
                }
                case GetSoftwareVersionRequest.METHOD_NAME: {
                    rsp = new GetSoftwareVersionResponse(_id, Version.asString());
                    break;
                }
                case RemoveDeviceRequest.METHOD_NAME: {
                    RemoveDeviceRequest req = mapper.treeToValue(_rootNode, RemoveDeviceRequest.class);
                    for (String device_id : req.params.devices) {
                        SensorPlatform sensor = sensorMgr.getRSP(device_id);
                        if (sensor != null) {
                            sensorMgr.remove(sensor);
                        }
                        GPIODevice gpio = gpioMgr.getGPIODevice(device_id);
                        if (gpio != null) {
                            gpioMgr.remove(gpio);
                        }
                    }
                    break;
                }
                case SchedulerActivateRequest.METHOD_NAME: {
                    SchedulerActivateRequest req = mapper.treeToValue(_rootNode, SchedulerActivateRequest.class);
                    scheduleMgr.activate(req.params);
                    rsp = new JsonResponseOK(_id, Boolean.TRUE);
                    break;
                }
                case SchedulerDeactivateRequest.METHOD_NAME: {
                    scheduleMgr.deactivate();
                    rsp = new JsonResponseOK(_id, Boolean.TRUE);
                    break;
                }
                case InventoryGetTagsRequest.METHOD_NAME: {
                    InventoryGetTagsRequest req = mapper.treeToValue(_rootNode, InventoryGetTagsRequest.class);
                    rsp = new InventoryGetTagsResponse(_id, inventoryMgr.getTags(req.params));
                    break;
                }
                case SetClusterConfigRequest.METHOD_NAME: {
                    SetClusterConfigRequest req = mapper.treeToValue(_rootNode, SetClusterConfigRequest.class);
                    clusterMgr.loadConfig(req.params.cluster_config);
                    rsp = new JsonResponseOK(_id, Boolean.TRUE);
                    break;
                }
                case SetSensorLedRequest.METHOD_NAME: {
                    SetSensorLedRequest req = mapper.treeToValue(_rootNode, SetSensorLedRequest.class);
                    sensorMgr.groupSetLED(req.params.led_state);
                    rsp = new JsonResponseOK(_id, Boolean.TRUE);
                    break;
                }
                default:
                    rsp = new JsonResponseErr(_id, JsonRPCError.Type.METHOD_NOT_FOUND, _method);
                    break;
            }
        } catch (JsonProcessingException jpe) {
            log.error("{}", jpe.getMessage());
            rsp = new JsonResponseErr(_id, JsonRPCError.Type.INVALID_PARAMETER, jpe.getMessage());
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            rsp = new JsonResponseErr(_id, JsonRPCError.Type.INTERNAL_ERROR, e.getMessage());
        }
        return rsp;
    }

    public JsonResponse onSchedulerActivate(String _id, JsonNode _rootNode) {
        JsonResponse rsp = null;
        return rsp;
    }

    public JsonResponse onSchedulerDeactivate(String _id) {
        JsonResponse rsp = null;
        return rsp;
    }

    public JsonResponse onSendCycleCount(String _id) {
        JsonResponse rsp = null;
        return rsp;
    }

    public JsonResponse onSetClusterConfig(String _id, JsonNode _rootNode) {
        JsonResponse rsp = null;
        return rsp;
    }

    public void send(GatewayDeviceAlertNotification _alert) {
        try {
            _alert.params.gateway_id = deviceId;
            mqttUpstream.publish(_alert);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public void send(UpstreamInventoryEvent _uie) {
        try {
            _uie.params.gateway_id = deviceId;
            mqttUpstream.publish(_uie);
        } catch (Exception e) {
            log.error("error: ", e);
        }
    }

    public void sendResponse(JsonResponse _rsp)
        throws JsonProcessingException, GatewayException {

        byte[] bytes = mapper.writeValueAsBytes(_rsp);
        mqttUpstream.publishResponse(bytes);
        log.info("sent msgid[{}] {}",
                 _rsp.id, mapper.writeValueAsString(_rsp));
    }

    public MQTTSummary getSummary() {
        return mqttUpstream.getSummary();
    }

    public void show(PrettyPrinter _out) {
        _out.chunk("MQTT Upstream: ");
        mqttUpstream.status(_out);
    }

}
