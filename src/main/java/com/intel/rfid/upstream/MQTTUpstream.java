/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.upstream.GatewayDeviceAlertNotification;
import com.intel.rfid.api.data.MQTTSummary;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.mqtt.MQTT;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTUpstream extends MQTT {

    protected Dispatch dispatch;
    protected static final ObjectMapper mapper = Jackson.getMapper();

    public static final String TOPIC_PREFIX = "rfid/gw";
    public static final String ALERTS_TOPIC = TOPIC_PREFIX + "/alerts";
    public static final String EVENTS_TOPIC = TOPIC_PREFIX + "/events";
    public static final String COMMAND_TOPIC = TOPIC_PREFIX + "/command";
    public static final String RESPONSE_TOPIC = TOPIC_PREFIX + "/response";

    public MQTTUpstream(Dispatch _dispatch) {
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

    public void publish(GatewayDeviceAlertNotification _alert) {
        try {
            log.info("publishing alert {}", mapper.writeValueAsString(_alert));
            publish(ALERTS_TOPIC, mapper.writeValueAsBytes(_alert), DEFAULT_QOS);
        } catch (GatewayException | JsonProcessingException e) {
            log.error("error", e);
        }
    }

    public void publish(UpstreamInventoryEvent _uie) {
        try {
            log.info("publishing {} events", _uie.params.data.size());
            publish(EVENTS_TOPIC, mapper.writeValueAsBytes(_uie), DEFAULT_QOS);
        } catch (Exception e) {
            log.error("error: ", e);
        }
    }

    public void publishResponse(byte[] _msg) throws GatewayException {
        String topic = RESPONSE_TOPIC;
        publish(topic, _msg, DEFAULT_QOS);
    }

    // TOOD: remove this in favor of getSummary()
    public void status(PrettyPrinter _out) {
        super.status(_out);
        _out.line("pub: " + ALERTS_TOPIC);
        _out.line("pub: " + EVENTS_TOPIC);
    }

    public MQTTSummary getSummary() {
        MQTTSummary summary = new MQTTSummary();
        summary.run_state = runState;
        summary.broker_uri = brokerURI;
        summary.subscribes.addAll(subscriptions);
        summary.publishes.add(ALERTS_TOPIC);
        summary.publishes.add(EVENTS_TOPIC);
        return summary;
    }


}
