/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ListLooper<T> {
    private final Collection<T> backingCollection;
    private Iterator<T> iterator;
    private boolean listLooped = false;

    public ListLooper(Collection<T> _collection) {
        backingCollection = _collection;
        iterator = _collection.iterator();
    }

    public T next() {
        if (iterator.hasNext()) {
            listLooped = false;
        } else {
            iterator = backingCollection.iterator();
            listLooped = true;
        }
        return iterator.next();
    }

    public boolean listLooped() {
        return listLooped;
    }

    public List<T> getAll() {
        return new ArrayList<>(backingCollection);
    }
}
