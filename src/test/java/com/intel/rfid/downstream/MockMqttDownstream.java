/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.downstream;

import com.intel.rfid.exception.GatewayException;

import java.util.concurrent.Executors;

public class MockMqttDownstream extends MqttDownstream {

    public MockMqttDownstream(Dispatch _dispatch) {
        super(_dispatch);
    }

    public void start() {
        exec = Executors.newSingleThreadExecutor();
        exec.submit(getOutboundTask());
        log.info(getClass().getSimpleName() + " started");
    }

    public void publish(String _topic, byte[] _msg, QOS _qos) throws GatewayException {
        // just log, but don't do anything
        log.info("{} not actually published", _topic);
    }

}
