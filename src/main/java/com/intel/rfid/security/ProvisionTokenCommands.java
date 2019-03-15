/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.rfid.api.ProvisionToken;
import com.intel.rfid.console.ArgumentIterator;
import com.intel.rfid.console.SyntaxException;
import com.intel.rfid.exception.GatewayException;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.helpers.DateTimeHelper;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.PrettyPrinter;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.intel.rfid.console.CLICommander.Support;

public final class ProvisionTokenCommands implements Support {

    protected Logger log = LoggerFactory.getLogger(getClass());

    static final String CMD_ID = "tokens";
    static final String SHOW = "show";
    static final String SHOW_DURATION = "show.duration";
    static final String SET_DURATION_INFINITE = "set.duration.infinite";
    static final String SET_DURATION_DAYS = "set.duration.days";
    static final String SET_DURATION_HOURS = "set.duration.hours";
    static final String GENERATE = "generate";
    static final String EXPORT = "export";
    static final String DELETE = "delete";

    // use these to parse output in test case
    static final String LABEL_TOTAL_TOKENS = "total tokens: ";
    static final String LABEL_TOKENS = "token: ";
    static final String LABEL_EXPIRES = "  exp: ";
    static final String LABEL_EXPORT_PATH = "provision token exported to: ";

    long durationMillis = ProvisionTokenManager.DEFAULT_VAILIDITY_MILLIS;

    ObjectMapper mapper = Jackson.getMapper();

    @Override
    public String getCommandId() {
        return CMD_ID;
    }

    @Override
    public void getCompleters(List<Completer> _completers) {
        _completers.add(
            new AggregateCompleter(
                new ArgumentCompleter(
                    new StringsCompleter(CMD_ID),
                    new StringsCompleter(SHOW, GENERATE,
                                         SHOW_DURATION,
                                         SET_DURATION_INFINITE,
                                         SET_DURATION_HOURS,
                                         SET_DURATION_DAYS),
                    new NullCompleter()
                ),
                new ArgumentCompleter(
                    new StringsCompleter(CMD_ID),
                    new StringsCompleter(DELETE, EXPORT),
                    new ProvisionTokenCompleter(),
                    new NullCompleter()
                )
            )
                       );
    }

    @Override
    public void usage(PrettyPrinter _out) {
        _out.indent(0, "USAGE");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW);
        _out.indent(1, "Show existing provisioning tokens");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SHOW_DURATION);
        _out.indent(1, "Show the valid duration used when generating new provisioning tokens");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_DURATION_INFINITE);
        _out.indent(1, "Set the valid duration of new provisioning tokens to never expire");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_DURATION_DAYS + " <number>");
        _out.indent(1, "Set the valid duration of new provisioning tokens in days (must be > 0)");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + SET_DURATION_HOURS + " <number>");
        _out.indent(1, "Set the valid duration of new provisioning tokens in hours (must be > 0)");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + GENERATE);
        _out.indent(1, "Generate a new provisioning token");
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + EXPORT);
        _out.indent(1, "Exports all tokens to " + Env.getExportTokenPath().toAbsolutePath().toString());
        _out.blank();
        _out.indent(0, "> " + CMD_ID + " " + DELETE + " <token>");
        _out.indent(1, "Delete an existing provisioning token");
        _out.indent(1, "WARNING: TOKEN IS NOT RECOVERABLE. Use with caution");
        _out.blank();
    }

    @Override
    public void doAction(String _action, ArgumentIterator _argIter, PrettyPrinter _out)
        throws GatewayException, IOException {

        switch (_action) {
            case SHOW:
                doShowTokens(_out);
                break;
            case SHOW_DURATION:
                doShowDuration(_out);
                break;
            case SET_DURATION_INFINITE:
                durationMillis = ProvisionTokenManager.INFINITE_VALIDITY;
                doShowDuration(_out);
                break;
            case SET_DURATION_DAYS:
                doSetDuration(Long.parseLong(_argIter.next()), TimeUnit.DAYS);
                doShowDuration(_out);
                break;
            case SET_DURATION_HOURS:
                doSetDuration(Long.parseLong(_argIter.next()), TimeUnit.HOURS);
                doShowDuration(_out);
                break;
            case GENERATE:
                doGenerateToken(_out);
                break;
            case EXPORT:
                doExportTokens(_out);
                break;
            case DELETE:
                doDeleteToken(_argIter, _out);
                break;
            default:
                usage(_out);
        }
    }

    private void doShowTokens(PrettyPrinter _out) throws IOException {
        Collection<ProvisionToken> allPT = SecurityContext.instance().getProvisionTokenMgr().getAll();
        if (allPT.size() <= 0) {
            _out.line("No tokens");
            return;
        }

        _out.println(LABEL_TOTAL_TOKENS + allPT.size());
        for (ProvisionToken pt : allPT) {
            _out.divider();
            _out.println(LABEL_TOKENS + pt.token);
            String expires;
            if (pt.expirationTimestamp == ProvisionTokenManager.INFINITE_VALIDITY) {
                expires = "infinite";
            } else {
                expires = DateTimeHelper.toUserLocal(new Date(pt.expirationTimestamp));
            }
            _out.println(LABEL_EXPIRES + expires);
        }
    }

    private void doShowDuration(PrettyPrinter _out) {
        _out.print("newly generated tokens will be valid for: ");
        if (durationMillis == ProvisionTokenManager.INFINITE_VALIDITY) {
            _out.print("inifinite");
        } else {
            _out.print(TimeUnit.MILLISECONDS.toHours(durationMillis) + " hours == " +
                       TimeUnit.MILLISECONDS.toMinutes(durationMillis) + " minutes == " +
                       durationMillis + " milliseconds");
        }
        _out.println();
    }

    private void doSetDuration(long _duration, TimeUnit _fromTimeUnit) throws SyntaxException {
        if (_duration <= 0) {
            throw new SyntaxException("value must be greater than 0");
        }
        durationMillis = _fromTimeUnit.toMillis(_duration);
    }

    private void doGenerateToken(PrettyPrinter _out) throws IOException {
        ProvisionToken pt = SecurityContext.instance().getProvisionTokenMgr().generate(durationMillis);
        exportForUser(pt, _out);
        mapper.writerWithDefaultPrettyPrinter().writeValue(_out, pt);
        _out.println();
    }

    private void exportForUser(ProvisionToken _pt, PrettyPrinter _out) {

        try {
            String fileName = "token_" + DateTimeHelper.toFilelNameLocal(new Date(_pt.generatedTimestamp)) + ".json";
            Path outPath = Env.resolveTokenPath(fileName);
            try (OutputStream os = Files.newOutputStream(outPath)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(os, _pt);
                _out.println(LABEL_EXPORT_PATH + outPath.toAbsolutePath().toString());
            }

        } catch (IOException e) {
            _out.error("error: " + e.getMessage());
        }

    }

    private void doExportTokens(PrettyPrinter _out) throws IOException, SyntaxException {

        Collection<ProvisionToken> allPT = SecurityContext.instance().getProvisionTokenMgr().getAll();
        if (allPT.size() <= 0) {
            _out.line("No tokens");
            return;
        }

        for (ProvisionToken pt : allPT) {
            exportForUser(pt, _out);
        }
    }


    private void doDeleteToken(ArgumentIterator _argIter, PrettyPrinter _out) throws IOException, SyntaxException {

        String token = _argIter.next();
        if (SecurityContext.instance().getProvisionTokenMgr().removeToken(token)) {
            _out.println("Token deleted");
            log.info("Token deleted: " + token);

        } else {
            _out.println("Token not found");
        }
    }

}
