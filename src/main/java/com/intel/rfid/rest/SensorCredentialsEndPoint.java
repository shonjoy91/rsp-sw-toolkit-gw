/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.ProvisionToken;
import com.intel.rfid.api.SensorCredentials;
import com.intel.rfid.gateway.ConfigManager;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.security.SecurityContext;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class SensorCredentialsEndPoint extends DefaultServlet {

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected static final ObjectMapper mapper = Jackson.getMapper();

    @Override
    protected void doGet(HttpServletRequest _req, HttpServletResponse _rsp) throws IOException, ServletException {
        _rsp.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
        _rsp.getWriter().println("Please use POST instead");
    }

    @Override
    protected void doPost(HttpServletRequest _req, HttpServletResponse _rsp) throws IOException, ServletException {

        // nest the method to keep indents reasonable
        // grab all exceptions even though method signature indicates otherwise
        // TODO: unify error handing and let exceptions propogate as designed, write custom exception handler if needed
        try {
            processRequest(_req, _rsp);
        } catch (Exception e) {
            log.error("Internal servlet error- ", e);
            _rsp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

    private void processRequest(HttpServletRequest _req, HttpServletResponse _rsp)
        throws IOException, ServletException {

        ConfigManager cm = ConfigManager.instance;

        if (isBadRequest(_req, _rsp)) {
            return;
        }

        String request;
        request = _req.getReader().lines().collect(Collectors.joining());

        // Execute only if the schema validation of the http request passes
        ProvisionToken pToken = mapper.readValue(request, ProvisionToken.class);
        log.info(pToken.toString());

        // check for user name, can't proceed without it
        if (pToken.username.isEmpty()) {
            setErr(_rsp, HttpStatus.UNAUTHORIZED_401, "Invalid username.");
            return;
        }

        if (cm.getProvisionSensorTokenRequired()) {

            boolean valid = false;

            if (pToken.token != null && !pToken.token.isEmpty()) {
                log.info("authenticating LOCAL");
                valid = SecurityContext.instance().getProvisionTokenMgr().isTokenValid(pToken.token);
            }

            if (!valid) {
                setErr(_rsp, HttpStatus.UNAUTHORIZED_401, "Invalid token.");
                return;
            }

        } else {
            log.info("skipping token authentication, not required");
        }

        SensorCredentials credentials = cm.getSensorCredentials();
        _rsp.setStatus(HttpStatus.OK_200);
        _rsp.getWriter().println(mapper.writeValueAsString(credentials));
    }

    static final String JSON_CONTENT_TYPE = "application/json";

    private boolean isBadRequest(HttpServletRequest _req, HttpServletResponse _rsp) throws IOException {

        // I don't think this is possible the way the server is configured
        // It is not testable from JUnit.
        if (!_req.isSecure()) {
            setErr(_rsp,
                   HttpStatus.FORBIDDEN_403,
                   "Secure scheme required for credential delivery.");
            return true;
        }

        String contentType = _req.getContentType();
        if (contentType == null ||
            contentType.length() == 0 ||
            !contentType.toLowerCase().contains(JSON_CONTENT_TYPE)) {

            setErr(_rsp,
                   HttpStatus.NOT_ACCEPTABLE_406,
                   "Unacceptable content type. Required: " + JSON_CONTENT_TYPE);
            return true;
        }

        if (_req.getContentLength() <= 0) {
            setErr(_rsp,
                   HttpStatus.LENGTH_REQUIRED_411,
                   "Content-Length required. ");
            return true;
        }
        
        return false;
    }

    private void setErr(HttpServletResponse _rsp, int _code, String _msg) throws IOException {
        _rsp.setStatus(_code);
        log.info(_msg);
        _rsp.getWriter().println(_msg);
    }

}
