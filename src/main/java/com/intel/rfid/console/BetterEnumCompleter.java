/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import jline.console.completer.StringsCompleter;

import static jline.internal.Preconditions.checkNotNull;

public class BetterEnumCompleter extends StringsCompleter {
    public BetterEnumCompleter(Class<? extends Enum<?>> source) {
        checkNotNull(source);

        for (Enum<?> e : source.getEnumConstants()) {
            this.getStrings().add(e.name());
        }
    }
}
