/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.upstream;

import com.intel.rfid.api.GatewayDeviceAlert;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.PrettyPrinter;
import com.intel.rfid.inventory.InventoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpstreamManager
    implements InventoryManager.UpstreamEventListener {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected String deviceId;
    protected MQTTUpstream mqttUpstream;

    public UpstreamManager() {
        ConfigManager cm = ConfigManager.instance;
        deviceId = cm.getGatewayDeviceId();
        mqttUpstream = new MQTTUpstream();
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

    public void send(GatewayDeviceAlert _alert) {
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

    public void show(PrettyPrinter _out) {
        _out.chunk("MQTT Upstream: ");
        mqttUpstream.status(_out);
    }

}
