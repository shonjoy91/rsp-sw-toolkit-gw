/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.behavior;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import java.util.List;

public class BehaviorCompleter implements Completer {

    public int complete(String buffer, final int cursor, final List<CharSequence> candidates) {
        StringsCompleter sc = new StringsCompleter();
        sc.getStrings().addAll(BehaviorConfig.available().keySet());
        return sc.complete(buffer, cursor, candidates);
    }

}
