/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.JsonRpcError;
import com.intel.rfid.api.data.Connection;
import com.intel.rfid.api.data.DeviceAlertDetails;
import com.intel.rfid.api.data.Personality;
import com.intel.rfid.api.data.ReadState;
import com.intel.rfid.api.data.SensorBasicInfo;
import com.intel.rfid.api.data.SensorConfigInfo;
import com.intel.rfid.api.sensor.AckAlertRequest;
import com.intel.rfid.api.sensor.AlertSeverity;
import com.intel.rfid.api.sensor.ApplyBehaviorRequest;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.api.sensor.ConnectRequest;
import com.intel.rfid.api.sensor.DeviceAlertNotification;
import com.intel.rfid.api.sensor.GeoRegion;
import com.intel.rfid.api.sensor.GetBISTResultsRequest;
import com.intel.rfid.api.sensor.GetGeoRegionRequest;
import com.intel.rfid.api.sensor.GetSoftwareVersionRequest;
import com.intel.rfid.api.sensor.GetStateRequest;
import com.intel.rfid.api.sensor.InventoryCompleteNotification;
import com.intel.rfid.api.sensor.InventoryDataNotification;
import com.intel.rfid.api.sensor.LEDState;
import com.intel.rfid.api.sensor.MotionEventNotification;
import com.intel.rfid.api.sensor.OemCfgUpdateNotification;
import com.intel.rfid.api.sensor.Platform;
import com.intel.rfid.api.sensor.RebootRequest;
import com.intel.rfid.api.sensor.ResetRequest;
import com.intel.rfid.api.sensor.RspInfo;
import com.intel.rfid.api.sensor.SensorHeartbeatNotification;
import com.intel.rfid.api.sensor.SetAlertThresholdRequest;
import com.intel.rfid.api.sensor.SetFacilityIdRequest;
import com.intel.rfid.api.sensor.SetGeoRegionRequest;
import com.intel.rfid.api.sensor.SetLEDRequest;
import com.intel.rfid.api.sensor.SetMotionEventRequest;
import com.intel.rfid.api.sensor.ShutdownRequest;
import com.intel.rfid.api.sensor.SoftwareUpdateRequest;
import com.intel.rfid.api.sensor.StatusUpdateNotification;
import com.intel.rfid.exception.RspControllerException;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.schedule.AtomicTimeMillis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intel.rfid.api.sensor.ApplyBehaviorRequest.Action.START;

public class SensorPlatform
        implements Comparable<SensorPlatform> {

    public static final long LOST_COMMS_THRESHOLD = 90000;
    // Threshold used to determine whether the behavior is DEEP_SCAN
    public static final int DEEP_SCAN_DWELL_TIME_THRESHOLD = 10000;
    public static final String UNKNOWN_FACILITY_ID = "UNKNOWN";
    public static final String DEFAULT_FACILITY_ID = "DEFAULT_FACILITY";

    public static final long START_RATE_LIMIT = TimeUnit.SECONDS.toMillis(2);

    protected final ObjectMapper mapper = Jackson.getMapper();

    protected String facilityId = DEFAULT_FACILITY_ID;
    protected Personality personality;
    protected final String deviceId;
    protected int minRssiDbm10X = Integer.MIN_VALUE;

    public static final int NUM_ALIASES = 4;
    protected final List<String> aliases = new ArrayList<>(NUM_ALIASES);

    protected final Logger logRSP; // created on construction using the deviceId
    protected final Logger logAlert = LoggerFactory.getLogger("rsp.alert");
    protected final Logger logHeartbeat = LoggerFactory.getLogger("rsp.heartbeat");
    protected final Logger logInventory = LoggerFactory.getLogger("rsp.inventory");
    protected final Logger logMotion = LoggerFactory.getLogger("rsp.motion");
    protected final Logger logStatus = LoggerFactory.getLogger("rsp.status");
    protected final Logger logConnect = LoggerFactory.getLogger("rsp.connect");

    protected final Object msgHandleLock = new Object();
    protected final Map<String, ResponseHandler> responseHandlers = new HashMap<>();

    protected final SensorStats sensorStats = new SensorStats();
    protected final SensorManager sensorMgr;

    protected final Object latchLock = new Object();
    protected CountDownLatch inventoryLatch = new CountDownLatch(0);

    private final ExecutorService readExecutor;

    protected Behavior currentBehavior = new Behavior();
    protected Connection.State connectionState = Connection.State.DISCONNECTED;
    protected ReadState readState = ReadState.STOPPED;
    protected final AtomicTimeMillis lastCommsMillis = new AtomicTimeMillis();
    private final AtomicTimeMillis lastStartFailure = new AtomicTimeMillis();
    protected boolean inDeepScan = false;
    protected String provisionToken;
    protected RspInfo rspInfo;

    public SensorPlatform(String _deviceId,
                          SensorManager _sensorMgr) {
        deviceId = _deviceId;
        sensorMgr = _sensorMgr;
        logRSP = LoggerFactory.getLogger(
                String.format("%s.%s", getClass().getSimpleName(), deviceId));
        // only have a single thread because reading commands should never be in parallel
        readExecutor = Executors.newFixedThreadPool(1,
                                                    new ExecutorUtils.NamedThreadFactory(deviceId));

        for (int i = 0; i < NUM_ALIASES; i++) {
            aliases.add(getDefaultAlias(i));
        }
    }

    /**
     * Valid transition: DISCONNECTED <==> CONNECTING <==> CONNECTED
     */
    private static boolean isStateTransitionValid(Connection.State _from, Connection.State _to) {
        boolean valid = false;
        switch (_from) {
            case DISCONNECTED:
                if (_to == Connection.State.CONNECTING) {
                    valid = true;
                }
                break;
            case CONNECTING:
                if (_to != Connection.State.CONNECTING) {
                    valid = true;
                }
                break;
            case CONNECTED:
                if (_to == Connection.State.DISCONNECTED) {
                    valid = true;
                }
                break;
        }
        return valid;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getMinRssiDbm10X() {
        return minRssiDbm10X;
    }

    public void setMinRssiDbm10X(int _minRssi) {
        minRssiDbm10X = _minRssi;
    }

    public SensorStats getSensorStats() {
        return sensorStats;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public boolean isFacilityValid() {
        return !UNKNOWN_FACILITY_ID.equals(facilityId);
    }

    public ResponseHandler setFacilityId(String _id) {
        // don't allow nulls EVER!
        if (_id == null) {
            return new ResponseHandler(deviceId,
                                       JsonRpcError.Type.INVALID_PARAMETER,
                                       "facility id cannot be NULL");
        }

        if (facilityId.equals(_id)) {
            return new ResponseHandler(deviceId,
                                       JsonRpcError.Type.WRONG_STATE,
                                       "facility id already set to " + _id);
        }

        // if not connected, this is all that needs to be done
        if (!isConnected()) {
            changeFacilityId(_id);
            return new ResponseHandler(deviceId,
                                       JsonRpcError.Type.NO_ERROR,
                                       "NOTE!! facility id is now set, but RSP is not connected");
        }

        // stop the RSP first if needed before changing facility id
        if (_id.equals(UNKNOWN_FACILITY_ID) && readState == ReadState.STARTED) {
            execute(new ApplyBehaviorRequest(ApplyBehaviorRequest.Action.STOP, currentBehavior));
        }

        return execute(new SetFacilityIdRequest(_id));
    }

    private void changeFacilityId(String _id) {
        facilityId = _id;
        sensorMgr.notifyConfigUpdate(this);
    }

    public void clearPersonality() {
        personality = null;
        sensorMgr.notifyConfigUpdate(this);
    }

    public void setPersonality(Personality _personality) {
        personality = _personality;
        sensorMgr.notifyConfigUpdate(this);
    }

    public Personality getPersonality() { return personality; }

    public boolean hasPersonality(Personality _personality) {
        return personality != null && personality == _personality;
    }

    public static final String ALIAS_KEY_DEFAULT = "DEFAULT";
    public static final String ALIAS_KEY_DEVICE_ID = "DEVICE_ID";

    public void setAliases(List<String> _aliases) {
        // NOTE: not
        if (_aliases == null || _aliases.isEmpty()) {
            resetAliasesInternal();
        } else {
            // these need interpreting so have to loop;
            for (int i = 0; i < _aliases.size(); i++) {
                setAliasInternal(i, _aliases.get(i));
            }
        }
        sensorMgr.notifyConfigUpdate(this);
    }

    /**
     * NOTE: this method should only be used externally.
     * Pay attention to the notifyConfigUpdate calls.
     */
    public void setAlias(int _portIndex, String _alias) {
        setAliasInternal(_portIndex, _alias);
        sensorMgr.notifyConfigUpdate(this);
    }

    /**
     * aliases have been added primarily to support H1000, but that makes other models report
     * tag reads per antenna port which is non-intuitive.
     * this method is a means to overcome that usage (this base class without explicitly knowing
     * the platform during construction needs to support all ports ... H1000 usage model)
     */
    public void checkAliasesOnConnect() {
        if (rspInfo == null || rspInfo.platform == null || rspInfo.platform == Platform.H1000) { return; }

        boolean updated = false;
        for (int port = 0; port < NUM_ALIASES; port++) {
            if (getAlias(port).equals(getDefaultAlias(port))) {
                logRSP.info("resetting port alias[{}} from {} to {}",
                            port, getAlias(port), getDefaultAlias(port));
                setAliasInternal(port, ALIAS_KEY_DEVICE_ID);
                updated = true;
            }
        }

        if (updated) {
            sensorMgr.notifyConfigUpdate(this);
        }
    }

    protected void resetAliasesInternal() {
        String key = ALIAS_KEY_DEFAULT;
        if (rspInfo != null && rspInfo.platform != null && rspInfo.platform != Platform.H1000) {
            key = ALIAS_KEY_DEVICE_ID;
        }
        for (int port = 0; port < NUM_ALIASES; port++) {
            setAliasInternal(port, key);
        }
    }

    // need a common setter to prevent double notifications
    protected void setAliasInternal(int _portIndex, String _alias) {
        if (_portIndex < 0 || _portIndex >= NUM_ALIASES) {
            logRSP.debug("alias index out of bounds {}", _portIndex);
            return;
        }

        // figure out the intended alias
        String interpretedAlias;
        if (_alias == null || _alias.isEmpty() || ALIAS_KEY_DEFAULT.equals(_alias)) {
            interpretedAlias = getDefaultAlias(_portIndex);
        } else if (ALIAS_KEY_DEVICE_ID.equals(_alias)) {
            interpretedAlias = deviceId;
        } else {
            interpretedAlias = _alias;
        }

        aliases.set(_portIndex, interpretedAlias);
    }

    public String getAlias(int _portIndex) {
        if (_portIndex < 0 || _portIndex >= aliases.size()) {
            return getDefaultAlias(_portIndex);
        }
        return aliases.get(_portIndex);
    }

    public List<String> getAliases() {
        return new ArrayList<>(aliases);
    }

    public String getAliasesAsString() {
        return Arrays.toString(aliases.toArray());
    }

    public String getDefaultAlias(int _portIndex) {
        return deviceId + "-" + _portIndex;
    }

    public String getProvisionToken() {
        return provisionToken;
    }

    public void setProvisionToken(String _token) {
        provisionToken = _token;
    }

    public void handleMessage(byte[] _msg) {
        synchronized (msgHandleLock) {
            // this means the reader is alive
            updateLastComms();

            try {

                JsonNode rootNode = mapper.readTree(_msg);

                // check for method object and get out early if missing
                JsonNode idNode = rootNode.get("id");
                JsonNode methodNode = rootNode.get("method");

                // both requests and notifications can follow a similar
                // processing path. The onXXX methods are inherently mapped
                // to whether this is a request or notification
                if (methodNode != null) {
                    handleMethod(methodNode.asText(), rootNode);
                } else if (idNode != null) {
                    String id = idNode.asText();
                    ResponseHandler rh = responseHandlers.get(id);
                    if (rh != null) {
                        rh.handleResponse(rootNode);
                    }
                    responseHandlers.remove(id);
                } else {
                    logRSP.warn("{} unhandled json msg: {}",
                                deviceId, mapper.writeValueAsString(rootNode));
                }

            } catch (Exception e) {
                logRSP.error("{} Error handling message:", deviceId, e);
            }
        }
    }

    private void handleMethod(String _method, JsonNode _rootNode) {

        try {

            switch (_method) {

                case ConnectRequest.METHOD_NAME:
                    ConnectRequest conReq = mapper.treeToValue(_rootNode, ConnectRequest.class);
                    onConnect(conReq);
                    break;

                case DeviceAlertNotification.METHOD_NAME:
                    DeviceAlertNotification alert = mapper.treeToValue(_rootNode, DeviceAlertNotification.class);
                    onDeviceAlert(alert);
                    break;

                case MotionEventNotification.METHOD_NAME:
                    MotionEventNotification event = mapper.treeToValue(_rootNode, MotionEventNotification.class);
                    onMotionEvent(event);
                    break;

                case SensorHeartbeatNotification.METHOD_NAME:
                    SensorHeartbeatNotification hb = mapper.treeToValue(_rootNode, SensorHeartbeatNotification.class);
                    onHeartbeat(hb);
                    break;

                case StatusUpdateNotification.METHOD_NAME:
                    StatusUpdateNotification update = mapper.treeToValue(_rootNode, StatusUpdateNotification.class);
                    onStatusUpdate(update);
                    break;

                case InventoryCompleteNotification.METHOD_NAME:
                    InventoryCompleteNotification ic = mapper.treeToValue(_rootNode,
                                                                          InventoryCompleteNotification.class);
                    onInventoryComplete(ic);
                    break;

                case OemCfgUpdateNotification.METHOD_NAME:
                    OemCfgUpdateNotification updateNotification = mapper.treeToValue(_rootNode,
                                                                                     OemCfgUpdateNotification.class);
                    sensorMgr.notifyOemCfgUpdate(updateNotification);
                    break;

                default:
                    logRSP.warn("{} unhandled method: {}", deviceId, _method);

            }

        } catch (JsonProcessingException e) {
            logRSP.error("{} Error inbound JsonRPC message:", deviceId, e);
        } catch (Exception e) {
            logRSP.error("{} Error handling message:", deviceId, e);
        }

    }

    protected final List<DeviceAlertDetails> alerts = new ArrayList<>();

    private void onDeviceAlert(DeviceAlertNotification _msg) {
        logInboundJson(logAlert, _msg.getMethod(), _msg.params);
        synchronized (alerts) {
            alerts.add(_msg.params);
        }
        sensorMgr.notifyDeviceAlert(_msg);
        // be aware to not block on the ResultHandler returned by
        // this call in this current thread, deadlock
        acknowledgeAlert(_msg.params.alert_number, true);
    }

    public List<DeviceAlertDetails> getAlerts() {
        List<DeviceAlertDetails> l;
        synchronized (alerts) {
            l = new ArrayList<>(alerts);
        }
        return l;
    }

    private void onHeartbeat(SensorHeartbeatNotification _msg) {
        if (connectionState != Connection.State.CONNECTED) {
            changeConnectionState(Connection.State.CONNECTED, Connection.Cause.RESYNC);
        }
        logInboundJson(logHeartbeat, _msg.getMethod(), _msg.params);
    }

    private void onInventoryComplete(InventoryCompleteNotification _msg) {
        logInboundJson(logInventory, _msg.getMethod(), _msg.params);
        // don't change if PEND_START as this inventory complete might be
        // from a previous transaction start/finish cycle
        // don't change if PEND_STOP because that transition happens
        // via the response handler of the stop request
        if (readState == ReadState.STARTED) {
            setReadState(ReadState.STOPPED);
        }

        synchronized (latchLock) {
            if (inventoryLatch != null) {
                inventoryLatch.countDown();
            }
        }
    }

    public void onInventoryData(InventoryDataNotification _invData) {
        sensorStats.onInventoryData(_invData);
    }

    private void onMotionEvent(MotionEventNotification _msg) {
        logInboundJson(logMotion, _msg.getMethod(), _msg.params);
    }

    private void onStatusUpdate(StatusUpdateNotification _msg) {
        logInboundJson(logStatus, _msg.getMethod(), _msg.params);

        StatusUpdateNotification.Status status = _msg.params.status;

        switch (status) {
            // shutting down is a clean exit by the reader
            // lost comes from MQTT broker as last will from RSP
            // fallthrough on purpose, in current implementation
            // the lost message will come in after the sensor lost comms
            // timer has already caused us to go disconnected
            case shutting_down:
                if (connectionState != Connection.State.DISCONNECTED) {
                    changeConnectionState(Connection.State.DISCONNECTED, Connection.Cause.SHUTTING_DOWN);
                }
                break;
            case lost:
                if (connectionState != Connection.State.DISCONNECTED) {
                    changeConnectionState(Connection.State.DISCONNECTED,
                                          Connection.Cause.LOST_DOWNSTREAM_COMMS);
                }
                break;
            case ready:
                // this one is sent in two situations
                // after a chip reset (in_reset)
                // to confirm the connection request and response
                if (connectionState == Connection.State.CONNECTING) {
                    changeConnectionState(Connection.State.CONNECTED, Connection.Cause.READY);
                }
                break;
            case in_reset:
                setReadState(ReadState.STOPPED);
                break;
            case firmware_update:
            case unknown:
            default:
                break;
        }
    }

    protected void onConnect(ConnectRequest _msg) {
        logInboundJson(logConnect, _msg.getMethod(), _msg.params);
        try {
            sensorMgr.sendConnectResponse(_msg.getId(), deviceId, facilityId);
            rspInfo = new RspInfo(_msg.params);
            checkAliasesOnConnect();
            changeConnectionState(Connection.State.CONNECTING, null);
        } catch (IOException | RspControllerException _e) {
            logRSP.error("error sending connect response", _e);
        }
    }

    public boolean isConnected() {
        return connectionState == Connection.State.CONNECTED;
    }

    public Connection.State getConnectionState() {
        return connectionState;
    }

    protected synchronized void changeConnectionState(Connection.State _next, Connection.Cause _cause) {
        Connection.State prevState = connectionState;

        if (isStateTransitionValid(connectionState, _next)) {
            logRSP.info("{} changing connection from {} to {} caused by {}",
                        deviceId, connectionState, _next, _cause);

            // check and log missing facility
            if (_next == Connection.State.CONNECTED && UNKNOWN_FACILITY_ID.equals(facilityId)) {
                logRSP.warn("Detected unconfigured facility for device: " + deviceId);
            }
        } else {
            logRSP.warn("{} Unexpected connection transition from {} to {} caused by {}",
                        deviceId, connectionState, _next, _cause);
        }

        // change the state
        connectionState = _next;

        // post transition actions
        if (connectionState == Connection.State.DISCONNECTED) {
            if (readState != ReadState.STOPPED) {
                logRSP.info("Resetting read state to STOPPED on disconnect");
                setReadState(ReadState.STOPPED);
            }
            sensorStats.disconnected();
        }

        sensorMgr.notifyConnectionStateChange(this, prevState, connectionState, _cause);
    }

    public long getLastCommsMillis() {
        return lastCommsMillis.get();
    }

    public void updateLastComms() {
        lastCommsMillis.mark();
    }

    private boolean hasLostComms() {
        return !lastCommsMillis.isWithin(LOST_COMMS_THRESHOLD);
    }

    void checkLostHeartbeatAndReset() {
        if (hasLostComms()) {
            if (connectionState != Connection.State.DISCONNECTED) {
                changeConnectionState(Connection.State.DISCONNECTED, Connection.Cause.LOST_HEARTBEAT);
            }
            lastCommsMillis.set(0);
        }
    }

    public void setBehavior(Behavior _behavior) {
        if (_behavior == null) { return; }
        currentBehavior = _behavior;
        if (isConnected() && readState == ReadState.STARTED) {
            execute(new ApplyBehaviorRequest(ApplyBehaviorRequest.Action.STOP, currentBehavior));
        }
    }

    public SensorConfigInfo getConfigInfo() {
        return new SensorConfigInfo(deviceId, facilityId, personality, new ArrayList<>(aliases));
    }

    public SensorBasicInfo getBasicInfo() {
        return new SensorBasicInfo(deviceId,
                                   connectionState,
                                   readState,
                                   currentBehavior.id,
                                   facilityId,
                                   personality,
                                   new ArrayList<>(aliases),
                                   new ArrayList<>(alerts));

    }

    private final AtomicBoolean scanCompletedSuccessfully = new AtomicBoolean(false);

    public CompletableFuture<Boolean> startScanAsync(final Behavior _behavior) {
        CountDownLatch latch = new CountDownLatch(1);
        synchronized (latchLock) {
            inventoryLatch = latch;
        }

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                // An RSP with a current module error condition will fail to start
                // only to be immediately rescheduled, rate limit this loop condition
                if (lastStartFailure.isWithin(START_RATE_LIMIT)) {
                    Thread.sleep(START_RATE_LIMIT);
                }

                ResponseHandler responseHandler = startReading(_behavior);
                if (!responseHandler.waitForResponse() || responseHandler.isError()) {
                    lastStartFailure.mark();
                    return false;
                }

                // We don't specify a timeout; instead, if the caller wishes to, they
                // can cancel the future, resulting in the "whenComplete" method below
                // counting down the latch (and future.get() throwing a CancellationException).
                latch.await();
                return scanCompletedSuccessfully.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }, readExecutor);

        future.exceptionally(throwable -> false)
              .whenComplete((success, throwable) -> {
                  if (future.isCancelled()) {
                      latch.countDown();
                  }
              });

        return future;
    }

    public ResponseHandler startReading(Behavior _behavior) {
        if (_behavior == null) {
            logRSP.error("Cannot start reading with a NULL behavior");
            return new ResponseHandler(deviceId,
                                       JsonRpcError.Type.INVALID_PARAMETER,
                                       "behavior cannot be NULL");
        }
        currentBehavior = _behavior;
        return startReading();
    }

    public ResponseHandler startReading() {
        ResponseHandler rh;

        // this shouldn't happen, but you never know
        if (isReading()) {
            rh = stopReading();
            if (rh.isError()) {
                logRSP.error("startReading: unable to stop reader {}", deviceId);
                return rh;
            }
        }

        // NOTE: This is done in this one spot because all other "startReading"
        // methods get piped through to this one.
        ReadState previousState = readState;
        setReadState(ReadState.PEND_START);
        setInDeepScan();

        rh = requestReadStateChange(START);
        if (rh.isError()) {
            setReadState(previousState);
        }

        scanCompletedSuccessfully.set(true);
        return rh;
    }

    public ResponseHandler stopReading() {
        scanCompletedSuccessfully.set(false);
        ReadState previousState = readState;
        setReadState(ReadState.PEND_STOP);
        ResponseHandler rh = requestReadStateChange(ApplyBehaviorRequest.Action.STOP);
        if (rh.isError()) {
            setReadState(previousState);
        }
        return rh;
    }

    public boolean isPotentiallyReading() {
        return readState == ReadState.PEND_START || readState == ReadState.STARTED;
    }

    public boolean isReading() {
        return readState == ReadState.STARTED;
    }

    private void setReadState(ReadState _next) {
        ReadState prev = readState;
        readState = _next;
        sensorMgr.notifyReadStateChange(this, prev, readState, currentBehavior);
        if (readState == ReadState.STARTED) {
            sensorStats.startedReading();
        } else if (readState == ReadState.STOPPED) {
            sensorStats.stoppedReading();
        }
    }

    private ResponseHandler requestReadStateChange(ApplyBehaviorRequest.Action _action) {
        ResponseHandler rh;
        // connectivity is checked in execute method
        if (_action == START && !isFacilityValid()) {
            rh = new ResponseHandler(deviceId,
                                     JsonRpcError.Type.WRONG_STATE,
                                     "facility id is not configured");
        } else {
            rh = execute(new ApplyBehaviorRequest(_action, currentBehavior));
        }

        return rh;
    }

    public boolean isInDeepScan() {
        return inDeepScan;
    }

    private void setInDeepScan() {
        // check the behavior id to see if "DeepScan" is in the name
        inDeepScan = (currentBehavior.id.toLowerCase().contains("deepscan") ||
                      currentBehavior.id.toLowerCase().contains("deep_scan"));
    }

    public ResponseHandler reset() {
        return execute(new ResetRequest());
    }

    public ResponseHandler reboot() {
        return execute(new RebootRequest());
    }

    public ResponseHandler shutdown() {
        return execute(new ShutdownRequest());
    }

    public ResponseHandler softwareUpdate() {
        return execute(new SoftwareUpdateRequest());
    }

    public ResponseHandler getBISTResults() {
        return execute(new GetBISTResultsRequest());
    }

    public ResponseHandler getState() {
        return execute(new GetStateRequest());
    }

    public ResponseHandler getSoftwareVersion() {
        return execute(new GetSoftwareVersionRequest());
    }

    public ResponseHandler setLED(LEDState _state) {
        return execute(new SetLEDRequest(_state));
    }

    public ResponseHandler setMotion(boolean _sendEvents, boolean _captureImages) {
        return execute(new SetMotionEventRequest(_sendEvents, _captureImages));
    }

    public ResponseHandler getGeoRegion() {
        return execute(new GetGeoRegionRequest());
    }

    public ResponseHandler setGeoRegion(GeoRegion _region) {
        return execute(new SetGeoRegionRequest(_region));
    }

    public ResponseHandler setAlertThreshold(int _alertNumber,
                                             AlertSeverity _severity,
                                             Integer _threshold) {
        return execute(new SetAlertThresholdRequest(_alertNumber, _severity, _threshold));
    }

    public ResponseHandler acknowledgeAlert(int _alertNumber, boolean _ack) {
        return execute(new AckAlertRequest(_alertNumber, _ack, false));
    }

    public ResponseHandler muteAlert(int _alertNumber, boolean _ack) {
        return execute(new AckAlertRequest(_alertNumber, false, _ack));
    }

    private ResponseHandler execute(JsonRequest _req) {
        if (connectionState != Connection.State.CONNECTED) {
            String errMsg = "no connection to RSP";
            logRSP.info("Cannot execute {}: {} - {}", deviceId, _req.getMethod(), errMsg);
            return new ResponseHandler(deviceId, JsonRpcError.Type.WRONG_STATE, errMsg);
        }

        ResponseHandler rh;

        if (_req instanceof ApplyBehaviorRequest) {
            rh = new StartStopHandler(deviceId, _req.getId(),
                                      ((ApplyBehaviorRequest) _req).params.action);
        } else if (_req instanceof SetFacilityIdRequest) {
            rh = new SetFacilityHandler(deviceId, _req.getId(),
                                        ((SetFacilityIdRequest) _req).params);
        } else {
            rh = new ResponseHandler(deviceId, _req.getId());
        }

        // be sure to put the handler in before sending the message
        // risk of the message and response coming back before the handler
        // can get to it.
        synchronized (msgHandleLock) {
            responseHandlers.put(_req.getId(), rh);
        }

        try {
            rh.setRequest(_req);
            sensorMgr.sendSensorCommand(deviceId, _req);
        } catch (Exception e) {
            synchronized (msgHandleLock) {
                responseHandlers.remove(_req.getId());
            }
            rh = new ResponseHandler(deviceId, _req.getId(),
                                     JsonRpcError.Type.INTERNAL_ERROR, e.getMessage());
            rh.setRequest(_req);
            logRSP.error("{} error sending command:", deviceId, e);
        }

        return rh;
    }

    @Override
    public boolean equals(Object _o) {
        if (this == _o) {
            return true;
        }
        if (_o == null || getClass() != _o.getClass()) {
            return false;
        }
        SensorPlatform rsp = (SensorPlatform) _o;
        return Objects.equals(deviceId, rsp.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId);
    }

    private static final String FMT = "%-10s %-12s %-10s %-25s %-18s %-12s %s";
    public static final String HDR = String
            .format(FMT, "device", "connect", "reading", "behavior", "facility", "personality", "aliases");

    @Override
    public String toString() {
        return String.format(FMT,
                             deviceId,
                             connectionState,
                             readState,
                             currentBehavior.id,
                             facilityId,
                             personality == null ? "" : personality,
                             getAliasesAsString());
    }

    private void logInboundJson(Logger _log, String _prefix, Object _msg) {
        try {
            _log.info("{} RECEIVED {} {}", deviceId, _prefix, mapper.writeValueAsString(_msg));
        } catch (JsonProcessingException e) {
            _log.error("{} ERROR: {}", deviceId, e);
        }
    }

    @Override
    public int compareTo(SensorPlatform _other) {
        return deviceId.compareTo(_other.deviceId);
    }

    private class StartStopHandler extends ResponseHandler {
        ApplyBehaviorRequest.Action handlerAction;

        StartStopHandler(String _deviceId, String _trxId, ApplyBehaviorRequest.Action _action) {
            super(_deviceId, _trxId);
            handlerAction = _action;
        }

        @Override
        public void onResult(JsonNode _result) {
            switch (handlerAction) {
                case START:
                    setReadState(ReadState.STARTED);
                    break;
                case STOP:
                    setReadState(ReadState.STOPPED);
                    break;
            }
        }

        @Override
        public void onError(JsonNode _error) {
            setReadState(ReadState.STOPPED);
        }
    }

    /**
     * This handler is used to confirm a facility change request
     * that has been sent to a sensor
     */
    private class SetFacilityHandler extends ResponseHandler {
        private final String newFacilityId;

        SetFacilityHandler(String _deviceId, String _trxId, String _facilityId) {
            super(_deviceId, _trxId);
            newFacilityId = _facilityId;
        }

        @Override
        public void onResult(JsonNode _result) {
            // this means its good
            changeFacilityId(newFacilityId);
        }
    }
}
