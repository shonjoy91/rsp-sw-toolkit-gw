/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import com.intel.rfid.helpers.PrettyPrinter;

public interface CLICommandBuilder {
    CLICommander buildCommander(PrettyPrinter _prettyPrinter);
}
