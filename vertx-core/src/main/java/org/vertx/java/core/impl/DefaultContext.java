/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package org.vertx.java.core.impl;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import org.slf4j.MDC;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.core.file.impl.PathResolver;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class DefaultContext implements Context {
  public static boolean isMDCAware = Boolean.parseBoolean(System.getProperty("MDC_AWARE", "true"));
  private static final Logger log = LoggerFactory.getLogger(DefaultContext.class);

  protected final VertxInternal vertx;
  private DeploymentHandle deploymentContext;
  private PathResolver pathResolver;
  private Set<Closeable> closeHooks;
  private final ClassLoader tccl;
  private boolean closed;
  private final EventLoop eventLoop;
  protected final Executor orderedBgExec;

  protected DefaultContext(VertxInternal vertx, Executor orderedBgExec) {
    this.vertx = vertx;
    this.orderedBgExec = orderedBgExec;
    EventLoopGroup group = vertx.getEventLoopGroup();
    if (group != null) {
      this.eventLoop = group.next();
      this.tccl = Thread.currentThread().getContextClassLoader();
    } else {
      this.eventLoop = null;
      this.tccl = null;
    }
  }

  public void setTCCL() {
    Thread.currentThread().setContextClassLoader(tccl);
  }

  public void setDeploymentHandle(DeploymentHandle deploymentHandle) {
    this.deploymentContext = deploymentHandle;
  }

  public DeploymentHandle getDeploymentHandle() {
    return deploymentContext;
  }

  public PathResolver getPathResolver() {
    return pathResolver;
  }

  public void setPathResolver(PathResolver pathResolver) {
    this.pathResolver = pathResolver;
  }

  public void reportException(Throwable t) {
    if (deploymentContext != null) {
      deploymentContext.reportException(t);
    } else {
      log.error("Unhandled exception", t);
    }
  }

  public void addCloseHook(Closeable hook) {
    if (closeHooks == null) {
      closeHooks = new HashSet<>();
    }
    closeHooks.add(hook);
  }

  public void removeCloseHook(Closeable hook) {
    if (closeHooks != null) {
      closeHooks.remove(hook);
    }
  }

  public void runCloseHooks(Handler<AsyncResult<Void>> doneHandler) {
    if (closeHooks != null) {
      final CountingCompletionHandler<Void> aggHandler = new CountingCompletionHandler<>(vertx, closeHooks.size());
      aggHandler.setHandler(doneHandler);
      // Copy to avoid ConcurrentModificationException
      for (Closeable hook : new HashSet<>(closeHooks)) {
        try {
          hook.close(new AsyncResultHandler<Void>() {
            @Override
            public void handle(AsyncResult<Void> asyncResult) {
              if (asyncResult.failed()) {
                aggHandler.failed(asyncResult.cause());
              } else {
                aggHandler.complete();
              }
            }
          });
        } catch (Throwable t) {
          reportException(t);
        }
      }
    } else {
      doneHandler.handle(new DefaultFutureResult<>((Void) null));
    }
  }

  public abstract void execute(Runnable handler);

  public abstract boolean isOnCorrectWorker(EventLoop worker);

  public void execute(EventLoop worker, final Runnable handler) {
    if (isOnCorrectWorker(worker)) {
      wrapTask(handler).run();
    } else {
      execute(new MDCAwareRunnable() {
        @Override
        void invoke() {
          handler.run();
        }
      });
    }
  }

  public void runOnContext(final Handler<Void> task) {
    execute(new MDCAwareRunnable() {
      public void invoke() {
        task.handle(null);
      }
    });
  }

  public EventLoop getEventLoop() {
    return eventLoop;
  }

  // This executes the task in the worker pool using the ordered executor of the context
  // It's used e.g. from BlockingActions
  protected void executeOnOrderedWorkerExec(final Runnable task) {
    orderedBgExec.execute(wrapTask(task));
  }

  public void close() {
    unsetContext();
    closed = true;
  }

  private void unsetContext() {
    vertx.setContext(null);
  }

  protected Runnable wrapTask(final Runnable task) {
    return new MDCAwareRunnable() {
      public void invoke() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        try {
          vertx.setContext(DefaultContext.this);
          task.run();
        } catch (Throwable t) {
          reportException(t);
        } finally {
          if (!threadName.equals(currentThread.getName())) {
            currentThread.setName(threadName);
          }
        }
        if (closed) {
          // We allow tasks to be run after the context is closed but we make sure we unset the context afterwards
          // to avoid any leaks
          unsetContext();
        }
      }
    };
  }

  private static abstract class MDCAwareRunnable implements Runnable {
    private Map<String, String> values;

    public MDCAwareRunnable() {
      if (isMDCAware) {
        values = MDC.getCopyOfContextMap();
      }
    }

    public void run() {
      if (!isMDCAware) {
        invoke();
        return;
      }
      Map<String, String> save = MDC.getCopyOfContextMap();
      try {
        MDC.clear();
        if (values != null && !values.isEmpty()) {
          MDC.setContextMap(values);
        }
        invoke();
      } finally {
        MDC.clear();
        if (save != null && !save.isEmpty()) {
          MDC.setContextMap(save);
        }
      }
    }

    abstract void invoke();
  }

}
