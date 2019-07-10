/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessHelper {

    protected Logger log = LoggerFactory.getLogger(getClass());

    public static final int STANDARD_FAIL_CODE = -1;
    String[] cmdLine;
    Process proc;
    Thread outThread;
    Thread errThread;
    public List<String> outLines = new ArrayList<>();
    public List<String> errLines = new ArrayList<>();
    public int exitCode = STANDARD_FAIL_CODE;
    public boolean success = false;

    public ProcessHelper(String... _cmdLine) {
        cmdLine = _cmdLine;
    }

    // BLOCKING METHOD
    public void run() {
        try {
            // fresh output, allows re-running the command
            if (!outLines.isEmpty()) {
                outLines = new ArrayList<>();
            }
            if (!errLines.isEmpty()) {
                errLines = new ArrayList<>();
            }
            proc = new ProcessBuilder(cmdLine).start();
            outThread = new Thread(new Stringer(proc.getInputStream(), outLines));
            errThread = new Thread(new Stringer(proc.getErrorStream(), errLines));
            outThread.start();
            errThread.start();
            if (proc.waitFor(10, TimeUnit.SECONDS)) {
                success = true;
                outThread.join(2000);
                errThread.join(2000);
                exitCode = proc.exitValue();
            }
        } catch (IOException e) {
            errLines.add(e.getMessage());
        } catch (InterruptedException e) {
            cancel();
            Thread.currentThread().interrupt();
        }

    }

    public void cancel() {
        try {
            if (proc != null) {
                proc.destroyForcibly();
            }
            if (outThread != null) {
                outThread.interrupt();
                outThread.join(2000);
            }
            if (errThread != null) {
                errThread.interrupt();
                errThread.join(2000);
            }
        } catch (InterruptedException _e) {
            Thread.currentThread().interrupt();
        }
    }

    public class Stringer implements Runnable {

        private InputStream is;
        private List<String> out;

        public Stringer(InputStream _is, List<String> _out) {
            is = _is;
            out = _out;
        }

        @Override
        public void run() {
            try (BufferedReader br =
                         new BufferedReader(
                                 new InputStreamReader(is))) {

                String curLine;
                while ((curLine = br.readLine()) != null) {
                    out.add(curLine);
                }
            } catch (IOException e) {
                log.error("error: " + e.getMessage());
            }
        }
    }
}
