/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.console;

import java.util.ArrayList;

public class ArgumentIterator {

    private int position = 0;
    private String[] argIter;
    private String cmdLine;

    public ArgumentIterator(String _cmdLine) {
        cmdLine = _cmdLine;
        argIter = _cmdLine.split("\\s+");
    }

    public boolean hasNext() {
        return position != argIter.length;
    }

    public String next() throws SyntaxException {
        if (!hasNext()) {
            throw new SyntaxException(cmdLine + " > expecting more arguments");
        }
        position += 1;
        return argIter[position - 1];
    }

    private ArrayList<Integer> positionStack;

    /**
     * Save the current iterator position so a future call
     * to <code>reset</code> will reset the iterator to the
     * saved position. Multiple calls to save/reset may be
     * nested; reset will return the iterator to the saved
     * positions in the opposite order they were saved.
     */
    public void save() {
        if (positionStack == null) {
            positionStack = new ArrayList<>();
        }
        positionStack.add(position);
    }

    /**
     * Reset the current iterator position to the last value
     * saved, assuming a previous call to <code>save</code>
     * was made. If not, then this call has no effect.
     *
     * @return false if there is not a position to return to;
     * true if the position was reset.
     */
    public boolean reset() {
        if (positionStack == null || positionStack.isEmpty()) {
            return false;
        }
        position = positionStack.remove(positionStack.size() - 1);
        return true;
    }
}
