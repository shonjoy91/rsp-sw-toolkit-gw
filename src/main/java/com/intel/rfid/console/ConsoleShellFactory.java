/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

public class ConsoleShellFactory implements Factory<Command> {

    public Command create() {
        return new ConsoleShell(cmdBuilder);
    }

    private final CLICommandBuilder cmdBuilder;

    public ConsoleShellFactory(CLICommandBuilder _cmdBuilder) {
        cmdBuilder = _cmdBuilder;
    }

    private static class ConsoleShell implements Command, Runnable {

        protected final Logger log = LoggerFactory.getLogger(getClass());

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private Thread thread;
        private ConsoleSession session;
        private final CLICommandBuilder cmdBuilder;

        public ConsoleShell(CLICommandBuilder _cmdBuilder) {
            cmdBuilder = _cmdBuilder;
        }

        public void setInputStream(InputStream _in) { in = _in; }

        public void setOutputStream(OutputStream _out) { out = _out; }

        public void setErrorStream(OutputStream _err) { err = _err; }

        public void setExitCallback(ExitCallback _callback) { callback = _callback; }

        public void start(Environment _env) {
            session = new ConsoleSession(in, out, err, cmdBuilder);
            thread = new Thread(this);
            thread.start();
        }

        public void destroy() {
            if (session != null) {
                session.end();
            }
            thread.interrupt();
        }

        @Override
        public void run() {

            try {
                session.run();
            } catch (Exception e) {
                log.error("Error executing JLineShell...", e);
            } finally {
                callback.onExit(0);
            }
        }
    }
}
