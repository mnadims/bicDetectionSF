From a9a8297cad4122961b34265c0a31d87134a4a028 Mon Sep 17 00:00:00 2001
From: Jing Zhao <jing9@apache.org>
Date: Mon, 16 May 2016 15:23:36 -0700
Subject: [PATCH] HADOOP-13146. Refactor RetryInvocationHandler. Contributed by
 Tsz Wo Nicholas Sze.

--
 .../io/retry/FailoverProxyProvider.java       |  11 +
 .../io/retry/RetryInvocationHandler.java      | 350 +++++++++---------
 .../hadoop/io/retry/TestRetryProxy.java       |  67 +---
 3 files changed, 211 insertions(+), 217 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/FailoverProxyProvider.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/FailoverProxyProvider.java
index 5acb936aad5..c73e0837721 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/FailoverProxyProvider.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/FailoverProxyProvider.java
@@ -37,10 +37,21 @@
      * provides information for debugging purposes.
      */
     public final String proxyInfo;

     public ProxyInfo(T proxy, String proxyInfo) {
       this.proxy = proxy;
       this.proxyInfo = proxyInfo;
     }

    public String getString(String methodName) {
      return proxy.getClass().getSimpleName() + "." + methodName
          + " over " + proxyInfo;
    }

    @Override
    public String toString() {
      return proxy.getClass().getSimpleName() + " over " + proxyInfo;
    }
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java
index a67c84f058b..300d0c2ab5b 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java
@@ -17,48 +17,137 @@
  */
 package org.apache.hadoop.io.retry;
 
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.io.retry.FailoverProxyProvider.ProxyInfo;
 import org.apache.hadoop.io.retry.RetryPolicy.RetryAction;
import org.apache.hadoop.ipc.Client;
import org.apache.hadoop.ipc.*;
 import org.apache.hadoop.ipc.Client.ConnectionId;
import org.apache.hadoop.ipc.ProtocolTranslator;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RpcConstants;
import org.apache.hadoop.ipc.RpcInvocationHandler;
 
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
 
 /**
 * This class implements RpcInvocationHandler and supports retry on the client 
 * side.
 * A {@link RpcInvocationHandler} which supports client side retry .
  */
 @InterfaceAudience.Private
 public class RetryInvocationHandler<T> implements RpcInvocationHandler {
   public static final Log LOG = LogFactory.getLog(RetryInvocationHandler.class);
  private final FailoverProxyProvider<T> proxyProvider;
 
  /**
   * The number of times the associated proxyProvider has ever been failed over.
   */
  private long proxyProviderFailoverCount = 0;
  private static class Counters {
    /** Counter for retries. */
    private int retries;
    /** Counter for method invocation has been failed over. */
    private int failovers;
  }

  private static class ProxyDescriptor<T> {
    private final FailoverProxyProvider<T> fpp;
    /** Count the associated proxy provider has ever been failed over. */
    private long failoverCount = 0;

    private ProxyInfo<T> proxyInfo;

    ProxyDescriptor(FailoverProxyProvider<T> fpp) {
      this.fpp = fpp;
      this.proxyInfo = fpp.getProxy();
    }

    synchronized ProxyInfo<T> getProxyInfo() {
      return proxyInfo;
    }

    synchronized T getProxy() {
      return proxyInfo.proxy;
    }

    synchronized long getFailoverCount() {
      return failoverCount;
    }

    synchronized void failover(long expectedFailoverCount, Method method) {
      // Make sure that concurrent failed invocations only cause a single
      // actual failover.
      if (failoverCount == expectedFailoverCount) {
        fpp.performFailover(proxyInfo.proxy);
        failoverCount++;
      } else {
        LOG.warn("A failover has occurred since the start of "
            + proxyInfo.getString(method.getName()));
      }
      proxyInfo = fpp.getProxy();
    }

    boolean idempotentOrAtMostOnce(Method method) throws NoSuchMethodException {
      final Method m = fpp.getInterface()
          .getMethod(method.getName(), method.getParameterTypes());
      return m.isAnnotationPresent(Idempotent.class)
          || m.isAnnotationPresent(AtMostOnce.class);
    }

    void close() throws IOException {
      fpp.close();
    }
  }

  private static class RetryInfo {
    private final long delay;
    private final RetryAction failover;
    private final RetryAction fail;

    RetryInfo(long delay, RetryAction failover, RetryAction fail) {
      this.delay = delay;
      this.failover = failover;
      this.fail = fail;
    }

    static RetryInfo newRetryInfo(RetryPolicy policy, Exception e,
        Counters counters, boolean idempotentOrAtMostOnce) throws Exception {
      long maxRetryDelay = 0;
      RetryAction failover = null;
      RetryAction retry = null;
      RetryAction fail = null;

      final Iterable<Exception> exceptions = e instanceof MultiException ?
          ((MultiException) e).getExceptions().values()
          : Collections.singletonList(e);
      for (Exception exception : exceptions) {
        final RetryAction a = policy.shouldRetry(exception,
            counters.retries, counters.failovers, idempotentOrAtMostOnce);
        if (a.action == RetryAction.RetryDecision.FAIL) {
          fail = a;
        } else {
          // must be a retry or failover
          if (a.action == RetryAction.RetryDecision.FAILOVER_AND_RETRY) {
            failover = a;
          } else {
            retry = a;
          }
          if (a.delayMillis > maxRetryDelay) {
            maxRetryDelay = a.delayMillis;
          }
        }
      }

      return new RetryInfo(maxRetryDelay, failover,
          failover == null && retry == null? fail: null);
    }
  }

  private final ProxyDescriptor<T> proxyDescriptor;

   private volatile boolean hasMadeASuccessfulCall = false;
   
   private final RetryPolicy defaultPolicy;
   private final Map<String,RetryPolicy> methodNameToPolicyMap;
  private ProxyInfo<T> currentProxy;
 
   protected RetryInvocationHandler(FailoverProxyProvider<T> proxyProvider,
       RetryPolicy retryPolicy) {
@@ -68,39 +157,40 @@ protected RetryInvocationHandler(FailoverProxyProvider<T> proxyProvider,
   protected RetryInvocationHandler(FailoverProxyProvider<T> proxyProvider,
       RetryPolicy defaultPolicy,
       Map<String, RetryPolicy> methodNameToPolicyMap) {
    this.proxyProvider = proxyProvider;
    this.proxyDescriptor = new ProxyDescriptor<>(proxyProvider);
     this.defaultPolicy = defaultPolicy;
     this.methodNameToPolicyMap = methodNameToPolicyMap;
    this.currentProxy = proxyProvider.getProxy();
  }

  private RetryPolicy getRetryPolicy(Method method) {
    final RetryPolicy policy = methodNameToPolicyMap.get(method.getName());
    return policy != null? policy: defaultPolicy;
   }
 
   @Override
   public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable {
    RetryPolicy policy = methodNameToPolicyMap.get(method.getName());
    if (policy == null) {
      policy = defaultPolicy;
    }
    
    // The number of times this method invocation has been failed over.
    int invocationFailoverCount = 0;
    final boolean isRpc = isRpcInvocation(currentProxy.proxy);
      throws Throwable {
    final boolean isRpc = isRpcInvocation(proxyDescriptor.getProxy());
     final int callId = isRpc? Client.nextCallId(): RpcConstants.INVALID_CALL_ID;
    int retries = 0;
    return invoke(method, args, isRpc, callId, new Counters());
  }

  private Object invoke(final Method method, final Object[] args,
      final boolean isRpc, final int callId, final Counters counters)
      throws Throwable {
    final RetryPolicy policy = getRetryPolicy(method);

     while (true) {
       // The number of times this invocation handler has ever been failed over,
       // before this method invocation attempt. Used to prevent concurrent
       // failed method invocations from triggering multiple failover attempts.
      long invocationAttemptFailoverCount;
      synchronized (proxyProvider) {
        invocationAttemptFailoverCount = proxyProviderFailoverCount;
      }
      final long failoverCount = proxyDescriptor.getFailoverCount();
 
       if (isRpc) {
        Client.setCallIdAndRetryCount(callId, retries);
        Client.setCallIdAndRetryCount(callId, counters.retries);
       }
       try {
        Object ret = invokeMethod(method, args);
        final Object ret = invokeMethod(method, args);
         hasMadeASuccessfulCall = true;
         return ret;
       } catch (Exception ex) {
@@ -108,153 +198,74 @@ public Object invoke(Object proxy, Method method, Object[] args)
           // If interrupted, do not retry.
           throw ex;
         }
        boolean isIdempotentOrAtMostOnce = proxyProvider.getInterface()
            .getMethod(method.getName(), method.getParameterTypes())
            .isAnnotationPresent(Idempotent.class);
        if (!isIdempotentOrAtMostOnce) {
          isIdempotentOrAtMostOnce = proxyProvider.getInterface()
              .getMethod(method.getName(), method.getParameterTypes())
              .isAnnotationPresent(AtMostOnce.class);
        }
        List<RetryAction> actions = extractActions(policy, ex, retries++,
                invocationFailoverCount, isIdempotentOrAtMostOnce);
        RetryAction failAction = getFailAction(actions);
        if (failAction != null) {
          // fail.
          if (failAction.reason != null) {
            LOG.warn("Exception while invoking " + currentProxy.proxy.getClass()
                + "." + method.getName() + " over " + currentProxy.proxyInfo
                + ". Not retrying because " + failAction.reason, ex);
          }
          throw ex;
        } else { // retry or failover
          // avoid logging the failover if this is the first call on this
          // proxy object, and we successfully achieve the failover without
          // any flip-flopping
          boolean worthLogging = 
            !(invocationFailoverCount == 0 && !hasMadeASuccessfulCall);
          worthLogging |= LOG.isDebugEnabled();
          RetryAction failOverAction = getFailOverAction(actions);
          long delay = getDelayMillis(actions);

          if (worthLogging) {
            String msg = "Exception while invoking " + method.getName()
                + " of class " + currentProxy.proxy.getClass().getSimpleName()
                + " over " + currentProxy.proxyInfo;

            if (invocationFailoverCount > 0) {
              msg += " after " + invocationFailoverCount + " fail over attempts"; 
            }

            if (failOverAction != null) {
              // failover
              msg += ". Trying to fail over " + formatSleepMessage(delay);
            } else {
              // retry
              msg += ". Retrying " + formatSleepMessage(delay);
            }
            LOG.info(msg, ex);
          }

          if (delay > 0) {
            Thread.sleep(delay);
          }

          if (failOverAction != null) {
            // Make sure that concurrent failed method invocations only cause a
            // single actual fail over.
            synchronized (proxyProvider) {
              if (invocationAttemptFailoverCount == proxyProviderFailoverCount) {
                proxyProvider.performFailover(currentProxy.proxy);
                proxyProviderFailoverCount++;
              } else {
                LOG.warn("A failover has occurred since the start of this method"
                    + " invocation attempt.");
              }
              currentProxy = proxyProvider.getProxy();
            }
            invocationFailoverCount++;
          }
        }
        handleException(method, policy, failoverCount, counters, ex);
       }
     }
   }
 
  /**
   * Obtain a retry delay from list of RetryActions.
   */
  private long getDelayMillis(List<RetryAction> actions) {
    long retVal = 0;
    for (RetryAction action : actions) {
      if (action.action == RetryAction.RetryDecision.FAILOVER_AND_RETRY ||
              action.action == RetryAction.RetryDecision.RETRY) {
        if (action.delayMillis > retVal) {
          retVal = action.delayMillis;
        }
  private void handleException(final Method method, final RetryPolicy policy,
      final long expectedFailoverCount, final Counters counters,
      final Exception ex) throws Exception {
    final RetryInfo retryInfo = RetryInfo.newRetryInfo(policy, ex, counters,
        proxyDescriptor.idempotentOrAtMostOnce(method));
    counters.retries++;

    if (retryInfo.fail != null) {
      // fail.
      if (retryInfo.fail.reason != null) {
        LOG.warn("Exception while invoking "
            + proxyDescriptor.getProxyInfo().getString(method.getName())
            + ". Not retrying because " + retryInfo.fail.reason, ex);
       }
      throw ex;
     }
    return retVal;
  }
 
  /**
   * Return the first FAILOVER_AND_RETRY action.
   */
  private RetryAction getFailOverAction(List<RetryAction> actions) {
    for (RetryAction action : actions) {
      if (action.action == RetryAction.RetryDecision.FAILOVER_AND_RETRY) {
        return action;
      }
    // retry
    final boolean isFailover = retryInfo.failover != null;

    log(method, isFailover, counters.failovers, retryInfo.delay, ex);

    if (retryInfo.delay > 0) {
      Thread.sleep(retryInfo.delay);
     }
    return null;
  }
 
  /**
   * Return the last FAIL action.. only if there are no RETRY actions.
   */
  private RetryAction getFailAction(List<RetryAction> actions) {
    RetryAction fAction = null;
    for (RetryAction action : actions) {
      if (action.action == RetryAction.RetryDecision.FAIL) {
        fAction = action;
      } else {
        // Atleast 1 RETRY
        return null;
      }
    if (isFailover) {
      proxyDescriptor.failover(expectedFailoverCount, method);
      counters.failovers++;
     }
    return fAction;
   }
 
  private List<RetryAction> extractActions(RetryPolicy policy, Exception ex,
                                           int i, int invocationFailoverCount,
                                           boolean isIdempotentOrAtMostOnce)
          throws Exception {
    List<RetryAction> actions = new LinkedList<>();
    if (ex instanceof MultiException) {
      for (Exception th : ((MultiException) ex).getExceptions().values()) {
        actions.add(policy.shouldRetry(th, i, invocationFailoverCount,
                isIdempotentOrAtMostOnce));
      }
    } else {
      actions.add(policy.shouldRetry(ex, i,
              invocationFailoverCount, isIdempotentOrAtMostOnce));
  private void log(final Method method, final boolean isFailover,
      final int failovers, final long delay, final Exception ex) {
    // log info if this has made some successful calls or
    // this is not the first failover
    final boolean info = hasMadeASuccessfulCall || failovers != 0;
    if (!info && !LOG.isDebugEnabled()) {
      return;
     }
    return actions;
  }
 
  private static String formatSleepMessage(long millis) {
    if (millis > 0) {
      return "after sleeping for " + millis + "ms.";
    final StringBuilder b = new StringBuilder()
        .append("Exception while invoking ")
        .append(proxyDescriptor.getProxyInfo().getString(method.getName()));
    if (failovers > 0) {
      b.append(" after ").append(failovers).append(" failover attempts");
    }
    b.append(isFailover? ". Trying to failover ": ". Retrying ");
    b.append(delay > 0? "after sleeping for " + delay + "ms.": "immediately.");

    if (info) {
      LOG.info(b.toString(), ex);
     } else {
      return "immediately.";
      LOG.debug(b.toString(), ex);
     }
   }
  

   protected Object invokeMethod(Method method, Object[] args) throws Throwable {
     try {
       if (!method.isAccessible()) {
         method.setAccessible(true);
       }
      return method.invoke(currentProxy.proxy, args);
      return method.invoke(proxyDescriptor.getProxy(), args);
     } catch (InvocationTargetException e) {
       throw e.getCause();
     }
@@ -274,12 +285,11 @@ static boolean isRpcInvocation(Object proxy) {
 
   @Override
   public void close() throws IOException {
    proxyProvider.close();
    proxyDescriptor.close();
   }
 
   @Override //RpcInvocationHandler
   public ConnectionId getConnectionId() {
    return RPC.getConnectionIdForProxy(currentProxy.proxy);
    return RPC.getConnectionIdForProxy(proxyDescriptor.getProxy());
   }

 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/retry/TestRetryProxy.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/retry/TestRetryProxy.java
index 4137daec54c..41c1be49104 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/retry/TestRetryProxy.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/retry/TestRetryProxy.java
@@ -18,55 +18,32 @@
 
 package org.apache.hadoop.io.retry;
 
import static org.apache.hadoop.io.retry.RetryPolicies.RETRY_FOREVER;
import static org.apache.hadoop.io.retry.RetryPolicies.TRY_ONCE_THEN_FAIL;
import static org.apache.hadoop.io.retry.RetryPolicies.retryByException;
import static org.apache.hadoop.io.retry.RetryPolicies.retryByRemoteException;
import static org.apache.hadoop.io.retry.RetryPolicies.retryOtherThanRemoteException;
import static org.apache.hadoop.io.retry.RetryPolicies.retryUpToMaximumCountWithFixedSleep;
import static org.apache.hadoop.io.retry.RetryPolicies.retryUpToMaximumCountWithProportionalSleep;
import static org.apache.hadoop.io.retry.RetryPolicies.retryUpToMaximumTimeWithFixedSleep;
import static org.apache.hadoop.io.retry.RetryPolicies.retryForeverWithFixedSleep;
import static org.apache.hadoop.io.retry.RetryPolicies.exponentialBackoffRetry;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hadoop.io.retry.RetryPolicies.*;
 import org.apache.hadoop.io.retry.RetryPolicy.RetryAction;
 import org.apache.hadoop.io.retry.RetryPolicy.RetryAction.RetryDecision;
import org.apache.hadoop.io.retry.RetryPolicies.RetryUpToMaximumCountWithFixedSleep;
import org.apache.hadoop.io.retry.RetryPolicies.RetryUpToMaximumTimeWithFixedSleep;
import org.apache.hadoop.io.retry.RetryPolicies.TryOnceThenFail;
 import org.apache.hadoop.io.retry.UnreliableInterface.FatalException;
 import org.apache.hadoop.io.retry.UnreliableInterface.UnreliableException;
 import org.apache.hadoop.ipc.ProtocolTranslator;
 import org.apache.hadoop.ipc.RemoteException;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

 import org.junit.Before;
 import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
 
import java.io.IOException;
 import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.hadoop.io.retry.RetryPolicies.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
 
 public class TestRetryProxy {
   
@@ -131,25 +108,21 @@ public void testRpcInvocation() throws Exception {
     final UnreliableInterface unreliable = (UnreliableInterface)
       RetryProxy.create(UnreliableInterface.class, unreliableImpl, RETRY_FOREVER);
     assertTrue(RetryInvocationHandler.isRpcInvocation(unreliable));
    

    final AtomicInteger count = new AtomicInteger();
     // Embed the proxy in ProtocolTranslator
     ProtocolTranslator xlator = new ProtocolTranslator() {
      int count = 0;
       @Override
       public Object getUnderlyingProxyObject() {
        count++;
        count.getAndIncrement();
         return unreliable;
       }
      @Override
      public String toString() {
        return "" + count;
      }
     };
     
     // For a proxy wrapped in ProtocolTranslator method should return true
     assertTrue(RetryInvocationHandler.isRpcInvocation(xlator));
     // Ensure underlying proxy was looked at
    assertEquals(xlator.toString(), "1");
    assertEquals(1, count.get());
     
     // For non-proxy the method must return false
     assertFalse(RetryInvocationHandler.isRpcInvocation(new Object()));
- 
2.19.1.windows.1

