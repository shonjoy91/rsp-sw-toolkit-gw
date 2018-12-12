/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.GatewayDeviceAlert;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.mqtt.MQTT;

public class MQTTUpstream extends MQTT {

    protected static final ObjectMapper mapper = Jackson.getMapper();

    public static final String TOPIC_PREFIX = "rfid/gw";
    public static final String ALERTS_TOPIC = TOPIC_PREFIX + "/alerts";
    public static final String EVENTS_TOPIC = TOPIC_PREFIX + "/events";

    public MQTTUpstream() {
        ConfigManager cm = ConfigManager.instance;
        credentials = cm.getMQTTUpstreamCredentials();
        brokerURI = cm.getMQTTUpstreamURI();
    }

    public void publish(GatewayDeviceAlert _alert) {
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

    public void status(PrettyPrinter _out) {
        super.status(_out);
        _out.line("pub: " + ALERTS_TOPIC);
        _out.line("pub: " + EVENTS_TOPIC);
    }

}
