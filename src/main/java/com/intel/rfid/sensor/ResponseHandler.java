/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intel.rfid.api.common.JsonRPCError;
import com.intel.rfid.api.common.JsonRequest;
import com.intel.rfid.helpers.PrettyPrinter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ResponseHandler {

    public static final long DEFAULT_WAIT_TIMEOUT_MILLIS = 3500;
    // Sensor resets take longer to complete, so give 15 seconds before timing out
    public static final long SENSOR_RESET_TIMEOUT_MILLIS = 15000;

    private static final String REQ_ID_NONE = "none";
    protected String deviceId;
    private JsonNode resultNode;
    private JsonNode errorNode;
    private final CountDownLatch latch = new CountDownLatch(1);
    private JsonRequest request;

    private String requestId;


    public ResponseHandler(String _deviceId, String _trxId) {
        deviceId = _deviceId;
        requestId = _trxId;
    }

    // use these constructors to create an "instant" response handler
    public ResponseHandler(String _deviceId, JsonRPCError.Type _errType, String _msg) {
        this(_deviceId, REQ_ID_NONE, _errType, _msg);
    }

    public ResponseHandler(String _deviceId, String _trxId, JsonRPCError.Type _errType, String _msg) {
        this(_deviceId, _trxId);
        if (JsonRPCError.Type.NO_ERROR.equals(_errType)) {
            resultNode = JsonNodeFactory.instance.textNode(_msg);
        } else {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("code", _errType.code);
            error.put("message", _errType.name());
            error.put("data", _msg);

            errorNode = error;
        }
        latch.countDown();
    }

    public void setRequest(JsonRequest request) {
        this.request = request;
    }

    public JsonRequest getRequest() {
        return request;
    }

    public JsonNode getResult() {
        return resultNode;
    }

    public JsonNode getError() {
        return errorNode;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getErrorDescription() {
        try {
            return String.format("%s : %s", errorNode.path("message").asText(), errorNode.path("data").asText());
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public boolean isError() {
        return errorNode != null;
    }

    // available to override if additional functionality is needed
    public void onResult(JsonNode _result) {
    }

    public void onError(JsonNode _error) {
    }

    public String getDeviceId() {
        return deviceId;
    }

    // DO NOT CALL THIS FROM AN MQTT Message Handler Thread !!!
    // returns the result of waiting on the latch count to go to 0
    // true - if latch reached 0, false means a timeout
    public boolean waitForResponse() throws InterruptedException {
        return waitForResponse(DEFAULT_WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public boolean waitForResponse(long l, TimeUnit timeUnit) throws InterruptedException {
        return latch.await(l, timeUnit);
    }

    public void handleResponse(JsonNode _rootNode) {
        resultNode = _rootNode.get("result");
        errorNode = _rootNode.get("error");

        // hit the callback for non-blockers that care about the result
        if (resultNode != null) {
            onResult(resultNode);
        } else if (errorNode != null) {
            onError(errorNode);
        }

        // unblock others (if any) that are waiting
        latch.countDown();
    }

    public void show(ObjectMapper mapper, PrettyPrinter _out) {
        _out.chunk(getDeviceId());
        _out.chunk(" - ");

        if (resultNode != null) {
            // this means it was OK, which just has boolean true for a response
            // for the CLI, convert this to "OK"
            try {
                _out.chunk(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultNode));
            } catch (JsonProcessingException e) {
                _out.chunk(e.getMessage());
            }
            _out.endln();
            return;
        }

        if (errorNode != null) {
            _out.chunk("ERROR");
            _out.chunk(" : ");
            _out.chunk(errorNode.path("message").asText());
            _out.chunk(" : ");
            try {
                _out.chunk(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorNode.get("data")));
            } catch (JsonProcessingException e) {
                _out.chunk(e.getMessage());
            }
            _out.endln();
        }
    }
}
