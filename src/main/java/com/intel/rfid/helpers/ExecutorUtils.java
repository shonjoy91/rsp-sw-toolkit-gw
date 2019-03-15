/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.rfid.helpers;

import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExecutorUtils {
    public static final long TERMINATION_AWAIT_MILLIS = TimeUnit.SECONDS.toMillis(2);

    /**
     * Attempts to shutdown all of given the scheduler services using a pre-configured
     * timeout. If one or more fails to shutdown within the time limit or is interrupted,
     * an error is logged, but this method still attempts to shutdown the remaining
     * executors.
     */
    public static void shutdownExecutors(Logger _log, ExecutorService... _services) throws InterruptedException {
        InterruptedException ie = null;
        for (ExecutorService executor : _services) {

            if (ie != null) {
                _log.info("although interrupted, shutting down additional executor");
            }

            try {
                shutdownExecutor(_log, executor);
            } catch (InterruptedException _e) {
                _log.warn("interrupted waiting for executor to shut down");
                ie = _e;
            }
        }

        if (ie != null) {
            throw ie;
        }
    }

    /**
     * Attempts to shutdown the ExecutorService.
     * Logs an error if it fails.
     */
    public static void shutdownExecutor(Logger _log, ExecutorService _service) throws InterruptedException {
        if (_service == null) { return; }
        _service.shutdown();
        if (!_service.awaitTermination(TERMINATION_AWAIT_MILLIS, TimeUnit.MILLISECONDS)) {
            _service.shutdownNow();
            if (!_service.awaitTermination(TERMINATION_AWAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                _log.error("shutting down executor service timed out after calling shutdown and shutdownNow");
            }
        }
    }

    /**
     * If the service is null, shutdown, or terminated, returns a new executor
     * using the given supplier; otherwise, simply returns the passed in executor.
     */
    public static <T extends ExecutorService> T ensureValidExecutor(T _service,
                                                                    Supplier<T> _executorSupplier,
                                                                    Consumer<T> _ifRecreated) {
        // Java docs say "isTerminated is never true unless isShutdown or shutdownNow was called",
        // so I'm pretty sure it's redundant with isShutdown.
        if (_service == null || _service.isShutdown() || _service.isTerminated()) {
            T newService = _executorSupplier.get();
            if (_ifRecreated != null) {
                _ifRecreated.accept(newService);
            }
            return newService;
        }
        return _service;
    }

    // TODO: not sure this always cleans up, cancels, shutdowns existing??
    public static ExecutorService ensureValidParallel(ExecutorService _service) {
        return ensureValidExecutor(_service, Executors::newCachedThreadPool, null);
    }


    /**
     * If the service is null, shutdown, or terminated, returns a new single-threaded
     * executor; otherwise, simply returns the passed in executor.
     */
    public static ExecutorService ensureValidSequential(ExecutorService _service) {
        return ensureValidExecutor(_service, Executors::newSingleThreadExecutor, null);
    }

    /**
     * If the service is null, shutdown, or terminated, returns a new scheduled
     * thread pool executor. Otherwise, simply returns the passed in executor.
     */
    public static ScheduledExecutorService ensureValidScheduler(ScheduledExecutorService _service,
                                                                Consumer<ScheduledExecutorService> _ifRecreated) {
        return ensureValidExecutor(_service, Executors::newSingleThreadScheduledExecutor, _ifRecreated);
    }

    /**
     * A ThreadPoolExecutor that uses the given logger to logs exceptions
     * resulting from tasks running on the executor, whether they were
     * started using <code>submit</code> or <code>invoke</code>.
     * <p>
     * Normally, Runnables submitted to an executor need to wrap their
     * execution in try...catch to log messages, including interruptions,
     * which causes a lot of boilerplate for lambda expressions.
     * <p>
     * The expected use of this is to simplify event notification code,
     * which can execute an event notifier without concern as to whether
     * the event receiver throws an exception or interrupts the thread.
     * <p>
     * If the thread throws an InterruptedException, this will log it and
     * call <code>Thread.currentThread().interrupt()</code>.
     */
    public static class ExceptionLoggingExecutor extends ThreadPoolExecutor {
        private final Logger logger;

        ExceptionLoggingExecutor(Logger _logger, int _corePoolSize, int _maximumPoolSize,
                                 long _keepAliveTime, TimeUnit _unit, BlockingQueue<Runnable> _workQueue) {
            super(_corePoolSize, _maximumPoolSize, _keepAliveTime, _unit, _workQueue,
                  new NamedThreadFactory(_logger.getName()));
            logger = _logger;
        }

        @Override
        public void afterExecute(Runnable _runnable, Throwable _throwable) {
            super.afterExecute(_runnable, _throwable);
            // for submit() instances, which use a Future
            if (_throwable == null && _runnable instanceof Future<?>) {
                try {
                    // won't block, since this is afterExecute
                    ((Future<?>) _runnable).get();
                } catch (CancellationException | ExecutionException e) {
                    logger.error("Uncaught exception while executing thread", e);
                } catch (InterruptedException e) {
                    logger.error("Thread interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * An Event Executor is a single-threaded executor that logs exceptions
     * on the given logger.
     * <p>
     * This maintains the same semantics as those in the Executors class.
     *
     * @return an ExceptionLoggingExecutor with the parameters used by
     * <code>Executors::newSingleThreadExecutor</code>
     */
    public static ExecutorService newSingleThreadedEventExecutor(Logger _logger) {
        return new ExceptionLoggingExecutor(_logger, 1, 1, 0L,
                                            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public static ExecutorService newEventExecutor(Logger _logger, int _poolSize) {
        return new ExceptionLoggingExecutor(_logger, _poolSize, _poolSize, 0L,
                                            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public static class NamedThreadFactory implements ThreadFactory {
        ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        private final String namePrefix;

        public NamedThreadFactory(String _prefix) {
            namePrefix = _prefix + "-";
        }

        @Override
        public Thread newThread(Runnable _runnable) {
            Thread thread = defaultThreadFactory.newThread(_runnable);
            thread.setName(namePrefix + thread.getName());
            return thread;
        }
    }
}
