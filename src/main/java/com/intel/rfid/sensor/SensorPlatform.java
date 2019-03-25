/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.sensor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.AckAlert;
import com.intel.rfid.api.ApplyBehavior;
import com.intel.rfid.api.Behavior;
import com.intel.rfid.api.ConnectRequest;
import com.intel.rfid.api.ConnectResponse;
import com.intel.rfid.api.DeviceAlert;
import com.intel.rfid.api.DeviceAlertDetails;
import com.intel.rfid.api.GetBISTResults;
import com.intel.rfid.api.GetSoftwareVersion;
import com.intel.rfid.api.GetState;
import com.intel.rfid.api.InventoryComplete;
import com.intel.rfid.api.InventoryData;
import com.intel.rfid.api.JsonRPCError;
import com.intel.rfid.api.JsonRequest;
import com.intel.rfid.api.LEDState;
import com.intel.rfid.api.MotionEvent;
import com.intel.rfid.api.Personality;
import com.intel.rfid.api.RSPHeartbeat;
import com.intel.rfid.api.RSPInventoryEvent;
import com.intel.rfid.api.Reboot;
import com.intel.rfid.api.Reset;
import com.intel.rfid.api.SetAlertThreshold;
import com.intel.rfid.api.SetFacilityId;
import com.intel.rfid.api.SetLED;
import com.intel.rfid.api.SetMotionEvent;
import com.intel.rfid.api.Shutdown;
import com.intel.rfid.api.StatusUpdate;
import com.intel.rfid.downstream.DownstreamManager;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.ExecutorUtils;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.schedule.AtomicTimeMillis;
import com.intel.rfid.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
import java.util.concurrent.atomic.AtomicInteger;

import static com.intel.rfid.api.ApplyBehavior.Action.START;

public class SensorPlatform 
    implements Comparable<SensorPlatform> {

    public static final long LOST_COMMS_THRESHOLD = 90000;
    // Threshold used to determine whether the behavior is DEEP_SCAN
    public static final int DEEP_SCAN_DWELL_TIME_THRESHOLD = 10000;
    public static final String UNKNOWN_FACILITY_ID = "UNKNOWN";
    public static final String DEFAULT_FACILITY_ID = "DEFAULT_FACILITY";

    public static final long START_RATE_LIMIT = TimeUnit.SECONDS.toMillis(2);

    protected static final ObjectMapper MAPPER = Jackson.getMapper();

    protected String facilityId = DEFAULT_FACILITY_ID;
    protected Personality personality;
    protected final String deviceId;
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
    protected ConnectionState connectionState = ConnectionState.DISCONNECTED;
    protected ReadState readState = ReadState.STOPPED;
    protected final AtomicTimeMillis lastCommsMillis = new AtomicTimeMillis();
    private final AtomicTimeMillis lastStartFailure = new AtomicTimeMillis();
    private final AtomicInteger startReadingErrors = new AtomicInteger(0);
    protected boolean inDeepScan = false;
    protected String provisionToken;

    protected final Object downstreamLock = new Object();
    protected DownstreamManager downstream;

    public SensorPlatform(String _deviceId,
                          SensorManager _sensorMgr) {
        deviceId = _deviceId;
        sensorMgr = _sensorMgr;
        logRSP = LoggerFactory.getLogger(
            String.format("%s.%s", getClass().getSimpleName(), deviceId));
        // only have a single thread because reading commands should never be in parallel
        readExecutor = Executors.newFixedThreadPool(1,
                                                    new ExecutorUtils.NamedThreadFactory(deviceId));
    }

    /**
     * Valid transition: DISCONNECTED <==> CONNECTING <==> CONNECTED
     */
    private static boolean isStateTransitionValid(ConnectionState _from, ConnectionState _to) {
        boolean valid = false;
        switch (_from) {
            case DISCONNECTED:
                if (_to == ConnectionState.CONNECTING) {
                    valid = true;
                }
                break;
            case CONNECTING:
                if (_to != ConnectionState.CONNECTING) {
                    valid = true;
                }
                break;
            case CONNECTED:
                if (_to == ConnectionState.DISCONNECTED) {
                    valid = true;
                }
                break;
        }
        return valid;
    }

    public void setDownstream(DownstreamManager _downstream) {
        synchronized (downstreamLock) {
            downstream = _downstream;
        }
    }

    public String getDeviceId() {
        return deviceId;
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
                                       JsonRPCError.Type.INVALID_PARAMETER,
                                       "facility id cannot be NULL");
        }

        if (facilityId.equals(_id)) {
            return new ResponseHandler(deviceId,
                                       JsonRPCError.Type.WRONG_STATE,
                                       "facility id already set to " + _id);
        }

        // if not connected, this is all that needs to be done
        if (!isConnected()) {
            changeFacilityId(_id);
            return new ResponseHandler(deviceId,
                                       JsonRPCError.Type.NO_ERROR,
                                       "NOTE!! facility id is now set, but RSP is not connected");
        }

        // stop the RSP first if needed before changing facility id
        if (_id.equals(UNKNOWN_FACILITY_ID) && readState == ReadState.STARTED) {
            execute(new ApplyBehavior(ApplyBehavior.Action.STOP, currentBehavior));
        }

        return execute(new SetFacilityId(_id));
    }

    private void changeFacilityId(String _id) {
        facilityId = _id;
    }

    public void clearPersonality() {
        personality = null;
    }

    public void setPersonality(Personality _personality) {
        personality = _personality;
    }

    public Personality getPersonality() { return personality; }

    public boolean hasPersonality(Personality _personality) {
        return personality != null && personality == _personality;
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

                JsonNode rootNode = MAPPER.readTree(_msg);

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
                                deviceId, MAPPER.writeValueAsString(rootNode));
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
                    ConnectRequest conReq = MAPPER.treeToValue(_rootNode, ConnectRequest.class);
                    onConnect(conReq);
                    break;

                case DeviceAlert.METHOD_NAME:
                    DeviceAlert alert = MAPPER.treeToValue(_rootNode, DeviceAlert.class);
                    onDeviceAlert(alert);
                    break;

                case MotionEvent.METHOD_NAME:
                    MotionEvent event = MAPPER.treeToValue(_rootNode, MotionEvent.class);
                    onMotionEvent(event);
                    break;

                case RSPHeartbeat.METHOD_NAME:
                    RSPHeartbeat hb = MAPPER.treeToValue(_rootNode, RSPHeartbeat.class);
                    onHeartbeat(hb);
                    break;

                case StatusUpdate.METHOD_NAME:
                    StatusUpdate update = MAPPER.treeToValue(_rootNode, StatusUpdate.class);
                    onStatusUpdate(update);
                    break;

                case InventoryComplete.METHOD_NAME:
                    InventoryComplete ic = MAPPER.treeToValue(_rootNode, InventoryComplete.class);
                    onInventoryComplete(ic);
                    break;

                case RSPInventoryEvent.METHOD_NAME:
                    RSPInventoryEvent rie = MAPPER.treeToValue(_rootNode, RSPInventoryEvent.class);
                    onInventoryEvent(rie);
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

    private void onDeviceAlert(DeviceAlert _msg) {
        logInboundJson(logAlert, _msg.getMethod(), _msg.params);
        synchronized (alerts) {
            alerts.add(_msg.params);
        }
        sensorMgr.notifyDeviceAlert(_msg);
        // be aware to not block on the ResultHandler returned by
        // this call in this current thread, deadlock
        acknowledgeAlert(_msg.params.alert_number, true);
    }
    
    protected List<DeviceAlertDetails> getAlerts() {
        List<DeviceAlertDetails> l = new ArrayList<>();
        synchronized (alerts) {
            l.addAll(alerts);
        }
        return l;
    }

    private void onHeartbeat(RSPHeartbeat _msg) {
        if (connectionState != ConnectionState.CONNECTED) {
            changeConnectionState(ConnectionState.CONNECTED, ConnectionStateEvent.Cause.RESYNC);
        }
        logInboundJson(logHeartbeat, _msg.getMethod(), _msg.params);
    }

    private void onInventoryComplete(InventoryComplete _msg) {
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

    public void onInventoryData(InventoryData _invData) {
        sensorStats.onInventoryData(_invData);
    }

    private void onInventoryEvent(RSPInventoryEvent _msg) {
        logInboundJson(logInventory, _msg.getMethod(), _msg.params);
    }

    private void onMotionEvent(MotionEvent _msg) {
        logInboundJson(logMotion, _msg.getMethod(), _msg.params);
    }

    private void onStatusUpdate(StatusUpdate _msg) {
        logInboundJson(logStatus, _msg.getMethod(), _msg.params);

        StatusUpdate.Status status = _msg.params.status;

        switch (status) {
            // shutting down is a clean exit by the reader
            // lost comes from MQTT broker as last will from RSP
            // fallthrough on purpose, in current implementation
            // the lost message will come in after the sensor lost comms
            // timer has already caused us to go disconnected
            case shutting_down:
                if (connectionState != ConnectionState.DISCONNECTED) {
                    changeConnectionState(ConnectionState.DISCONNECTED, ConnectionStateEvent.Cause.SHUTTING_DOWN);
                }
                break;
            case lost:
                if (connectionState != ConnectionState.DISCONNECTED) {
                    changeConnectionState(ConnectionState.DISCONNECTED,
                                          ConnectionStateEvent.Cause.LOST_DOWNSTREAM_COMMS);
                }
                break;
            case ready:
                // this one is sent in two situations
                // after a chip reset (in_reset)
                // to confirm the connection request and response
                if (connectionState == ConnectionState.CONNECTING) {
                    changeConnectionState(ConnectionState.CONNECTED, ConnectionStateEvent.Cause.READY);
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

    private void onConnect(ConnectRequest _msg) {
        logInboundJson(logConnect, _msg.getMethod(), _msg.params);
        if (sendConnectResponse(_msg)) {
            changeConnectionState(ConnectionState.CONNECTING, null);
        }
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    protected synchronized void changeConnectionState(ConnectionState _next, ConnectionStateEvent.Cause _cause) {
        ConnectionState prevState = connectionState;

        if (isStateTransitionValid(connectionState, _next)) {
            logRSP.info("{} changing connection from {} to {} caused by {}",
                        deviceId, connectionState, _next, _cause);

            // check and log missing facility
            if (_next == ConnectionState.CONNECTED && UNKNOWN_FACILITY_ID.equals(facilityId)) {
                logRSP.warn("Detected unconfigured facility for device: " + deviceId);
            }
        } else {
            logRSP.warn("{} Unexpected connection transition from {} to {} caused by {}",
                        deviceId, connectionState, _next, _cause);
        }

        // change the state
        connectionState = _next;

        // post transition actions
        if (connectionState == ConnectionState.DISCONNECTED) {
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
            if (connectionState != ConnectionState.DISCONNECTED) {
                changeConnectionState(ConnectionState.DISCONNECTED, ConnectionStateEvent.Cause.LOST_HEARTBEAT);
            }
            lastCommsMillis.set(0);
        }
    }

    private boolean sendConnectResponse(ConnectRequest _msg) {
        ConfigManager cm = ConfigManager.instance;
        try {
            ConnectResponse rsp = new ConnectResponse(_msg.getId(),
                                                      facilityId,
                                                      System.currentTimeMillis(),
                                                      cm.getLocalHost("ntp.server.host"),
                                                      cm.getRSPSoftwareRepos(),
                                                      SecurityContext.instance().getKeyMgr().getSshEncodedPublicKey());

            synchronized (downstreamLock) {
                if (downstream == null) {
                    logRSP.error("No downstream manager for {}", deviceId);
                    return false;
                }
                downstream.sendConnectRsp(deviceId, rsp);
            }
        } catch (Exception e) {
            logRSP.error("{} error sending connect response:", deviceId, e);
            return false;
        }
        return true;
    }

    public ResponseHandler setBehavior(Behavior _behavior) {
        if (_behavior == null) {
            return new ResponseHandler(deviceId,
                                       JsonRPCError.Type.INVALID_PARAMETER,
                                       "behavior cannot be NULL");
        }

        currentBehavior = _behavior;

        if (isConnected() && readState == ReadState.STARTED) {
            return execute(new ApplyBehavior(ApplyBehavior.Action.STOP, currentBehavior));
        } else {
            return new ResponseHandler(deviceId, JsonRPCError.Type.NO_ERROR,
                                       "behavior is now " + currentBehavior.id);
        }
    }
    
    public String getBehaviorId() {
        String id = null;
        if(currentBehavior != null) {
            id = currentBehavior.id;
        }
        return id;
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
                                       JsonRPCError.Type.INVALID_PARAMETER,
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
        ResponseHandler rh = requestReadStateChange(ApplyBehavior.Action.STOP);
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

    public ReadState getReadState() {
        return readState;
    }

    private void setReadState(ReadState _next) {
        ReadState prev = readState;
        readState = _next;
        sensorMgr.notifyReadStateChange(this, prev, readState);
        if (readState == ReadState.STARTED) {
            sensorStats.startedReading();
        } else if (readState == ReadState.STOPPED) {
            sensorStats.stoppedReading();
        }
    }

    private ResponseHandler requestReadStateChange(ApplyBehavior.Action _action) {
        ResponseHandler rh;
        // connectivity is checked in execute method
        if (_action == START && !isFacilityValid()) {
            rh = new ResponseHandler(deviceId,
                                     JsonRPCError.Type.WRONG_STATE,
                                     "facility id is not configured");
        } else {
            rh = execute(new ApplyBehavior(_action, currentBehavior));
        }

        return rh;
    }

    public boolean isInDeepScan() {
        return inDeepScan;
    }

    private void setInDeepScan() {
        // check the behavior parameters to determine
        // if this behavior provides a duty cycle that is
        // often enough that tag reads should use the weighting decay algorithm
        inDeepScan = (!currentBehavior.toggle_target_flag)
                     || (currentBehavior.repeat_until_no_tags)
                     || (currentBehavior.getInv_cycles() == 0
                         && currentBehavior.getDwell_time() > DEEP_SCAN_DWELL_TIME_THRESHOLD);
    }

    public ResponseHandler reset() {
        return execute(new Reset());
    }

    public ResponseHandler reboot() {
        return execute(new Reboot());
    }

    public ResponseHandler shutdown() {
        return execute(new Shutdown());
    }

    public ResponseHandler getBISTResults() {
        return execute(new GetBISTResults());
    }

    public ResponseHandler getState() {
        return execute(new GetState());
    }

    public ResponseHandler getSoftwareVersion() {
        return execute(new GetSoftwareVersion());
    }

    public ResponseHandler setLED(LEDState _state) {
        return execute(new SetLED(_state));
    }

    public ResponseHandler setMotion(boolean _sendEvents, boolean _captureImages) {
        return execute(new SetMotionEvent(_sendEvents, _captureImages));
    }

    public ResponseHandler setAlertThreshold(int _alertNumber,
                                             DeviceAlert.Severity _severity,
                                             Number _threshold) {
        return execute(new SetAlertThreshold(_alertNumber, _severity, _threshold));
    }

    public ResponseHandler acknowledgeAlert(int _alertNumber, boolean _ack) {
        return execute(new AckAlert(_alertNumber, _ack, false));
    }

    public ResponseHandler muteAlert(int _alertNumber, boolean _ack) {
        return execute(new AckAlert(_alertNumber, false, _ack));
    }

    private ResponseHandler execute(JsonRequest _req) {
        if (connectionState != ConnectionState.CONNECTED) {
            String errMsg = "no connection to RSP";
            logRSP.info("Cannot execute {}: {} - {}", deviceId, _req.getMethod(), errMsg);
            return new ResponseHandler(deviceId, JsonRPCError.Type.WRONG_STATE, errMsg);
        }

        // if it weren't possible to set the downstream manager, this wouldn't be necessary
        DownstreamManager downstreamManager;
        synchronized (downstreamLock) {
            if (downstream == null) {
                String errMsg = "missing mqttDownstream reference";
                logRSP.info("Cannot execute {}: {} - {}", deviceId, _req.getMethod(), errMsg);
                return new ResponseHandler(deviceId, JsonRPCError.Type.WRONG_STATE, errMsg);
            }
            downstreamManager = downstream;
        }

        _req.generateId();
        ResponseHandler rh;

        if (_req instanceof ApplyBehavior) {
            rh = new StartStopHandler(deviceId, _req.getId(),
                                      ((ApplyBehavior) _req).params.action);
        } else if (_req instanceof SetFacilityId) {
            rh = new SetFacilityHandler(deviceId, _req.getId(),
                                        ((SetFacilityId) _req).params);
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
            downstreamManager.sendCommand(deviceId, _req);
        } catch (Exception e) {
            synchronized (msgHandleLock) {
                responseHandlers.remove(_req.getId());
            }
            rh = new ResponseHandler(deviceId, _req.getId(),
                                     JsonRPCError.Type.INTERNAL_ERROR, e.getMessage());
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

    private static final String FMT = "%-10s %-12s %-10s %-25s %-18s %s";
    public static final String HDR = String
        .format(FMT, "device", "connect", "reading", "behavior", "facility", "personality");

    @Override
    public String toString() {
        return String.format(FMT,
                             deviceId,
                             connectionState,
                             readState,
                             currentBehavior.id,
                             facilityId,
                             personality == null ? "" : personality);
    }

    private void logInboundJson(Logger _log, String _prefix, Object _msg) {
        try {
            _log.info("{} RECEIVED {} {}", deviceId, _prefix, MAPPER.writeValueAsString(_msg));
        } catch (JsonProcessingException e) {
            _log.error("{} ERROR: {}", deviceId, e);
        }
    }

    @Override
    public int compareTo(SensorPlatform _other) {
        return deviceId.compareTo(_other.deviceId);
    }

    private class StartStopHandler extends ResponseHandler {
        ApplyBehavior.Action handlerAction;

        StartStopHandler(String _deviceId, String _trxId, ApplyBehavior.Action _action) {
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
