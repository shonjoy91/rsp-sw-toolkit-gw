/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.security;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import java.util.List;

public class ProvisionTokenCompleter implements Completer {

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        StringsCompleter strCompleter = new StringsCompleter();
        strCompleter.getStrings().addAll(SecurityContext.instance().getProvisionTokenMgr().getTokensOnly());
        return strCompleter.complete(buffer, cursor, candidates);
    }
}
