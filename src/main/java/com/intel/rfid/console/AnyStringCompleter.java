/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import jline.console.completer.Completer;

import java.util.List;

public class AnyStringCompleter
    implements Completer {

    public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {

        if (buffer == null || buffer.trim().length() == 0) {
            return -1;
        }

        candidates.add(buffer);

        return candidates.isEmpty() ? -1 : 0;
    }
}
