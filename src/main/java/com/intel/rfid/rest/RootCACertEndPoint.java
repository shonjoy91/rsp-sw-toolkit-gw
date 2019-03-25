/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.Certificate;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.security.KeyManager;
import com.intel.rfid.security.SecurityContext;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("serial")
public class RootCACertEndPoint extends DefaultServlet {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private static final ObjectMapper mapper = Jackson.getMapper();

    @Override
    protected void doGet(HttpServletRequest _req, HttpServletResponse _rsp) {

        try {
            KeyManager keyMgr = SecurityContext.instance().getKeyMgr();
            Certificate cert = new Certificate(keyMgr.getCACertificateAsPem());
            String s = mapper.writeValueAsString(cert);
            _rsp.getWriter().println(s);
            _rsp.setStatus(HttpStatus.OK_200);
        } catch (IOException e) {
            log.error("Error doGet: {}", e.getMessage());
            _rsp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }
}
