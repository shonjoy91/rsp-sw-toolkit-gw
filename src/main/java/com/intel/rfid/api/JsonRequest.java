/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.api;

import java.util.concurrent.atomic.AtomicInteger;

public class JsonRequest {

    protected String jsonrpc = "2.0";
    protected String id = IdGen.nextId();
    protected String method;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public String getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public void generateId() {
        id = IdGen.nextId();
    }

    private static class IdGen {
        private static final AtomicInteger id = new AtomicInteger(0);

        private static synchronized String nextId() {
            int next = id.getAndIncrement();
            if (next < 0) {
                next = 1;
                id.set(next);
            }
            return String.valueOf(next);
        }
    }
}
