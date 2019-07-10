/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Thread-safe class on which subscribers can subscribe for or unsubscribe
 * from messages, and publishers can notify subscribers.
 * <p>
 * Subscribing and unsubscribing is synchronized on the private, internal
 * listeners collection, whereas notification is locked on the provided
 * lock object the class was created with so that event messaging may be
 * coordinated across Publisher instances, if desired.
 * <p>
 * The listeners collection is implemented as a WeakHashMap so that classes
 * that forget to unsubscribe will still be cleaned up by the Garbage Collector.
 *
 * @param <T> the listener interface subscribers should implement.
 */
public class Publisher<T> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    // NOTE: it might be worth making this a ConcurrentHashMap and instead
    // either manually using WeakReferences, or working to ensure that all
    // subscribers play nice an unsubscribe. This would be most useful for
    // the notifiers that are called often, since read contention is far
    // less likely.
    // It might also make sense to use this class in a Map keyed on topics,
    // located in a central place (the gateway?) or passed to the manager 
    // instances, so they can fire-and-forget messages
    // that other service need to act upon.
    private final Set<T> listeners = Collections.newSetFromMap(new WeakHashMap<>());
    private ExecutorService executorService;
    private final Object executorLock;

    public Publisher(Object _executorLock, ExecutorService _executor) {
        executorLock = _executorLock;
        executorService = _executor;
        log.debug("Created new publisher with its own executor");
    }


    /**
     * Replaces the executor service.
     */
    public void replaceExecutor(ExecutorService _executor) {
        Objects.requireNonNull(_executor, "_executor");
        log.debug("Replacing executor");
        synchronized (executorLock) {
            executorService = _executor;
        }
    }

    public boolean subscribe(T _listener) {
        log.debug("Add subscriber");
        synchronized (listeners) {
            return listeners.add(_listener);
        }
    }

    public boolean unsubscribe(T _listener) {
        log.debug("Removing subscriber");
        synchronized (listeners) {
            return listeners.remove(_listener);
        }
    }

    public void clearSubscribers() {
        log.debug("Clearing all subscribers");
        synchronized (listeners) {
            listeners.clear();
        }
    }

    /**
     * Locks the listener collection long enough to generate event runnables using
     * the _notifyHow Consumer, then locks the executor and submits the runnables.
     */
    public Set<Future<?>> notifyListeners(Consumer<T> _notifyHow) {
        log.debug("Notifying listeners");

        Collection<Runnable> runnables;

        log.debug("Generating runnables");
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                log.debug("No listeners to notify");
                return Collections.emptySet();
            }

            runnables = listeners
                    .stream()
                    .map(listener -> (Runnable) () -> _notifyHow.accept(listener))
                    .collect(Collectors.toList());
        }
        log.debug("Done");

        log.debug("Scheduling Execution");
        Set<Future<?>> futures;
        synchronized (executorLock) {
            futures = runnables.stream().map(executorService::submit).collect(Collectors.toSet());
        }
        log.debug("Done");
        return futures;
    }
}
