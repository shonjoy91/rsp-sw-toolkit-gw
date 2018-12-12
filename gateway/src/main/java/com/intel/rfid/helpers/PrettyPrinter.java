/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

public class PrettyPrinter extends PrintWriter {
    public static final String INDENT_SPACE = "    ";
    public static final int NUM_COLUMNS = 92;
    public static final String DIV;

    static {
        StringBuilder sb = new StringBuilder(NUM_COLUMNS);
        for (int i = 0; i < NUM_COLUMNS; i++) {
            sb.append("-");
        }
        DIV = sb.toString();
    }

    public PrettyPrinter(Writer _writer) {
        super(_writer);
    }


    public PrettyPrinter indent(int _level, String _s) {
        for (int x = 0; x < _level; x++) {
            print(INDENT_SPACE);
        }
        println(_s);
        return this;
    }

    public PrettyPrinter chunk(String _s) {
        print(_s);
        return this;
    }

    @Override
    public PrettyPrinter format(String format, Object... args) {
        super.format(format, args);
        return this;
    }

    public PrettyPrinter endln() {
        return endln("");
    }

    public PrettyPrinter endln(String _s) {
        println(_s);
        return this;
    }

    public PrettyPrinter line(String _line) {
        println(_line);
        return this;
    }

    public PrettyPrinter rightPad(String _content, char _c) {
        if (NUM_COLUMNS <= _content.length()) {
            println(_content);
        } else {
            char[] padChars = new char[NUM_COLUMNS - _content.length()];
            Arrays.fill(padChars, _c);
            println(_content + new String(padChars));
        }
        return this;
    }

    public PrettyPrinter blank() {
        println();
        return this;
    }

    public void divider() {
        println(DIV);
    }

    public void error(String _msg) {
        print("ERROR ");
        print(_msg);
        println();
    }

}
