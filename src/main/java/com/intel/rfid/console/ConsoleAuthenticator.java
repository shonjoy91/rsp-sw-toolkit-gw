/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import com.intel.rfid.controller.ConfigManager;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleAuthenticator implements PasswordAuthenticator {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean authenticate(String _userId, String _password, ServerSession _session) {
        ConfigManager.Credentials crd = ConfigManager.instance.getConsoleCredentials();
        if (crd.userId == null || crd.password == null) {
            log.warn("Requested authentication for {}:{} but userId and/or password are not configured",
                     _userId, _password);
            return false;
        }
        return (_userId != null && _userId.equals(crd.userId)) &&
                (_password != null && _password.equals(crd.password));
    }

}
