/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.downstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.helpers.Jackson;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public abstract class MqttMsgHandler extends Thread {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static class Inbound {

        public Inbound(String _deviceId, MqttMessage _mqttMessage) {
            deviceId = _deviceId;
            mqttMessage = _mqttMessage;
        }

        public String deviceId;
        public MqttMessage mqttMessage;
    }

    private boolean keepGoing = true;

    private LinkedBlockingQueue<Inbound> msgQueue = new LinkedBlockingQueue<>(100);

    public void shutdown() {
        keepGoing = false;

        interrupt();
        try {
            join(2000);
        } catch (InterruptedException e) {
            log.warn("interrupted shutting down");
            Thread.currentThread().interrupt();
        }

    }

    private boolean loggedOnce = false;

    public void queue(Inbound _msg) {
        if (!msgQueue.offer(_msg)) {

            if (!loggedOnce) {
                log.warn("data handler queue is full. " +
                         "messages will be dropped until space is available");
                loggedOnce = true;
            }
        } else {
            // clear the state to re-enable the message
            loggedOnce = false;
        }
    }


    @Override
    public void run() {

        keepGoing = true;
        while (keepGoing) {
            try {
                Inbound msg = msgQueue.take();
                handleMessage(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected final ObjectMapper mapper = Jackson.getMapper();

    protected abstract void handleMessage(Inbound _msg);

}
