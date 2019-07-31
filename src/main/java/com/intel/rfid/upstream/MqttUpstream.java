/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.data.MqttStatus;
import com.intel.rfid.api.upstream.RSPControllerStatusUpdateNotification;
import com.intel.rfid.controller.ConfigManager;
import com.intel.rfid.controller.RSPControllerStatus;
import com.intel.rfid.exception.RSPControllerException;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.mqtt.Mqtt;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

public class MqttUpstream extends Mqtt {

    protected Dispatch dispatch;
    protected static final ObjectMapper mapper = Jackson.getMapper();

    public static final String TOPIC_PREFIX = "rfid/controller";
    public static final String ALERTS_TOPIC = TOPIC_PREFIX + "/alerts";
    public static final String EVENTS_TOPIC = TOPIC_PREFIX + "/events";
    public static final String COMMAND_TOPIC = TOPIC_PREFIX + "/command";
    public static final String RESPONSE_TOPIC = TOPIC_PREFIX + "/response";
    public static final String NOTIFICATION_TOPIC = TOPIC_PREFIX + "/notification";

    public MqttUpstream(Dispatch _dispatch) {
        ConfigManager cm = ConfigManager.instance;
        credentials = cm.getMQTTUpstreamCredentials();
        brokerURI = cm.getMQTTUpstreamURI();
        dispatch = _dispatch;
    }

    @Override
    public void start() {
        subscribe(COMMAND_TOPIC);

        super.start();
    }

    public interface Dispatch {
        void onMessage(final String _topic, final MqttMessage _msg);
    }

    protected void onConnect() {
        super.onConnect();
        try {
            String deviceId = ConfigManager.instance.getRSPControllerDeviceId();
            RSPControllerStatusUpdateNotification gsu = new RSPControllerStatusUpdateNotification(deviceId,
                                                                                                  RSPControllerStatus.RSP_CONTROLLER_STARTED);
            publishAlert(mapper.writeValueAsBytes(gsu));
            log.info("Published {}", RSPControllerStatus.RSP_CONTROLLER_STARTED);
        } catch (Exception e) {
            log.warn("Error publishing {}", RSPControllerStatus.RSP_CONTROLLER_STARTED.label, e);
        }
    }


    @Override
    public void messageArrived(final String _topic, final MqttMessage _msg) throws Exception {

        if (_msg.isRetained()) {
            log.info("Discarding retained message");
            return;
        }
        if (_msg.isDuplicate()) {
            log.info("Discarding duplicate message");
            return;
        }

        dispatch.onMessage(_topic, _msg);
    }

    public void publishAlert(Object _msg) {
        publish(ALERTS_TOPIC, _msg);
    }

    public void publishEvent(Object _msg) {
        publish(EVENTS_TOPIC, _msg);
    }

    public void publishResponse(Object _msg) {
        publish(RESPONSE_TOPIC, _msg);
    }

    public void publishNotification(Object _msg) {
        publish(NOTIFICATION_TOPIC, _msg);
    }

    private void publish(String _topic, Object _msg) {
        try {
            publish(_topic, mapper.writeValueAsBytes(_msg), DEFAULT_QOS);
        } catch (IOException | RSPControllerException _e) {
            log.error("error {}", _e.getMessage());
        }

    }


    // TOOD: remove this in favor of getMqttStatus()
    @Deprecated
    public void status(PrettyPrinter _out) {
        super.status(_out);
        _out.line("pub: " + ALERTS_TOPIC);
        _out.line("pub: " + EVENTS_TOPIC);
    }

    public MqttStatus getSummary() {
        MqttStatus summary = super.getSummary();
        summary.publishes.add(ALERTS_TOPIC);
        summary.publishes.add(EVENTS_TOPIC);
        summary.publishes.add(RESPONSE_TOPIC);
        summary.publishes.add(NOTIFICATION_TOPIC);
        return summary;
    }


}
