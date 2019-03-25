/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ConsoleSession implements Runnable {

    public static final String QUIT_CMD = "quit";
    public static final String EXIT_CMD = "exit";

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected InputStream in;
    protected OutputStream out;
    protected OutputStream err;
    protected CLICommandBuilder cmdBuilder;

    public ConsoleSession(InputStream _in,
                          OutputStream _out,
                          OutputStream _err,
                          CLICommandBuilder _cmdBuilder) {
        in = _in;
        out = _out;
        err = _err;
        cmdBuilder = _cmdBuilder;
    }

    private boolean keepGoing;

    public void end() {
        keepGoing = false;
    }

    @Override
    public void run() {
        keepGoing = true;

        try (
            FilterOutputStream fos = new FilterOutputStream(out) {
                final int cr = System.getProperty("line.separator").charAt(0);

                @Override
                public void write(final int i) throws IOException {
                    super.write(i);
                    if (i == cr) {
                        super.write(ConsoleReader.RESET_LINE);
                    }
                }
            }

        ) {

            ConsoleReader console = new ConsoleReader(in, fos);
            console.setPrompt("rfid-gw> ");

            PrettyPrinter pp = new PrettyPrinter(console.getOutput());
            CLICommander commander = cmdBuilder.buildCommander(pp);
            List<Completer> comps = new ArrayList<>();

            comps.add(new ArgumentCompleter(
                new StringsCompleter(QUIT_CMD, EXIT_CMD),
                new NullCompleter()));

            commander.getCompleters(comps);

            console.addCompleter(new AggregateCompleter(comps));

            pp.blank();
            pp.line("RFID Gateway console session");
            pp.blank();
            pp.line("<tab> to view available commands");
            pp.line("'clear' to clear the screen/console");
            pp.line("'quit' to end");
            pp.blank();
            pp.flush();

            String line;
            while (keepGoing && (line = console.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase(QUIT_CMD) || line.equalsIgnoreCase(EXIT_CMD)) {
                    keepGoing = false;
                } else if (line.equalsIgnoreCase("clear")) {
                    console.clearScreen();
                } else {
                    commander.execute(line);
                }
            }
            pp.line("Stopping console session...");
            pp.flush();

        } catch (InterruptedIOException _e) {
            log.info("Stopping console: Interrupted");
        } catch (Exception e) {
            log.error("error ", e);
        }
    }
}

