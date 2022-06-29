From 872db60c8da0d03900e057923ae41f043ffc6b59 Mon Sep 17 00:00:00 2001
From: Virag Kothari <virag@yahoo-inc.com>
Date: Mon, 24 Feb 2014 10:30:27 -0800
Subject: [PATCH] OOZIE-1699 Some of the commands submitted to Oozie internal
 queue are never executed (sriksun via virag)

--
 .../oozie/service/CallableQueueService.java   | 35 ++++++-----
 .../util/PollablePriorityDelayQueue.java      |  2 +-
 .../apache/oozie/util/PriorityDelayQueue.java | 32 ++++++----
 .../service/TestCallableQueueService.java     | 59 ++++++++++++++++---
 release-log.txt                               |  1 +
 5 files changed, 92 insertions(+), 37 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/service/CallableQueueService.java b/core/src/main/java/org/apache/oozie/service/CallableQueueService.java
index ab81b091c..093eb08dc 100644
-- a/core/src/main/java/org/apache/oozie/service/CallableQueueService.java
++ b/core/src/main/java/org/apache/oozie/service/CallableQueueService.java
@@ -29,7 +29,6 @@ import java.util.Set;
 import java.util.Map.Entry;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
@@ -153,16 +152,17 @@ public class CallableQueueService implements Service, Instrumentable {
         }
 
         public void run() {
            if (Services.get().getSystemMode() == SYSTEM_MODE.SAFEMODE) {
                log.info("Oozie is in SAFEMODE, requeuing callable [{0}] with [{1}]ms delay", getElement().getType(),
                        SAFE_MODE_DELAY);
                setDelay(SAFE_MODE_DELAY, TimeUnit.MILLISECONDS);
                removeFromUniqueCallables();
                queue(this, true);
                return;
            }
            XCallable<?> callable = getElement();
            XCallable<?> callable = null;
             try {
                removeFromUniqueCallables();
                if (Services.get().getSystemMode() == SYSTEM_MODE.SAFEMODE) {
                    log.info("Oozie is in SAFEMODE, requeuing callable [{0}] with [{1}]ms delay", getElement().getType(),
                            SAFE_MODE_DELAY);
                    setDelay(SAFE_MODE_DELAY, TimeUnit.MILLISECONDS);
                    queue(this, true);
                    return;
                }
                callable = getElement();
                 if (callableBegin(callable)) {
                     cron.stop();
                     addInQueueCron(cron);
@@ -170,7 +170,6 @@ public class CallableQueueService implements Service, Instrumentable {
                     XLog log = XLog.getLog(getClass());
                     log.trace("executing callable [{0}]", callable.getName());
 
                    removeFromUniqueCallables();
                     try {
                         callable.call();
                         incrCounter(INSTR_EXECUTED_COUNTER, 1);
@@ -188,13 +187,19 @@ public class CallableQueueService implements Service, Instrumentable {
                     log.warn("max concurrency for callable [{0}] exceeded, requeueing with [{1}]ms delay", callable
                             .getType(), CONCURRENCY_DELAY);
                     setDelay(CONCURRENCY_DELAY, TimeUnit.MILLISECONDS);
                    removeFromUniqueCallables();
                     queue(this, true);
                     incrCounter(callable.getType() + "#exceeded.concurrency", 1);
                 }
             }
            catch (Throwable t) {
                incrCounter(INSTR_FAILED_COUNTER, 1);
                log.warn("exception callable [{0}], {1}", callable == null ? "N/A" : callable.getName(),
                        t.getMessage(), t);
            }
             finally {
                callableEnd(callable);
                if (callable != null) {
                    callableEnd(callable);
                }
             }
         }
 
@@ -558,9 +563,9 @@ public class CallableQueueService implements Service, Instrumentable {
                 try {
                     executor.execute(wrapper);
                 }
                catch (RejectedExecutionException ree) {
                catch (Throwable ree) {
                     wrapper.removeFromUniqueCallables();
                    throw ree;
                    throw new RuntimeException(ree);
                 }
             }
         }
diff --git a/core/src/main/java/org/apache/oozie/util/PollablePriorityDelayQueue.java b/core/src/main/java/org/apache/oozie/util/PollablePriorityDelayQueue.java
index 664554439..6d692e31e 100644
-- a/core/src/main/java/org/apache/oozie/util/PollablePriorityDelayQueue.java
++ b/core/src/main/java/org/apache/oozie/util/PollablePriorityDelayQueue.java
@@ -39,8 +39,8 @@ public class PollablePriorityDelayQueue<E> extends PriorityDelayQueue<E> {
      */
     @Override
     public QueueElement<E> poll() {
        lock.lock();
         try {
            lock.lock();
             antiStarvation();
             QueueElement<E> e = null;
             int i = priorities;
diff --git a/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java b/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java
index 8b4e0fff6..a3f214840 100644
-- a/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java
++ b/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java
@@ -258,8 +258,8 @@ public class PriorityDelayQueue<E> extends AbstractQueue<PriorityDelayQueue.Queu
     @SuppressWarnings("unchecked")
     public Iterator<QueueElement<E>> iterator() {
         QueueElement[][] queueElements = new QueueElement[queues.length][];
        lock.lock();
         try {
            lock.lock();
             for (int i = 0; i < queues.length; i++) {
                 queueElements[i] = queues[i].toArray(new QueueElement[0]);
             }
@@ -340,23 +340,29 @@ public class PriorityDelayQueue<E> extends AbstractQueue<PriorityDelayQueue.Queu
         if (queueElement == null) {
             throw new NullPointerException("queueElement is NULL");
         }
        if (queueElement.getPriority() < 0 && queueElement.getPriority() >= priorities) {
            throw new IllegalArgumentException("priority out of range");
        if (queueElement.getPriority() < 0 || queueElement.getPriority() >= priorities) {
            throw new IllegalArgumentException("priority out of range: " + queueElement);
         }
         if (queueElement.inQueue) {
            throw new IllegalStateException("queueElement already in a queue");
            throw new IllegalStateException("queueElement already in a queue: " + queueElement);
         }
         if (!ignoreSize && currentSize != null && currentSize.get() >= maxSize) {
             return false;
         }
        boolean accepted = queues[queueElement.getPriority()].offer(queueElement);
        debug("offer([{0}]), to P[{1}] delay[{2}ms] accepted[{3}]", queueElement.getElement().toString(),
              queueElement.getPriority(), queueElement.getDelay(TimeUnit.MILLISECONDS), accepted);
        if (accepted) {
            if (currentSize != null) {
                currentSize.incrementAndGet();
        boolean accepted;
        lock.lock();
        try {
            accepted = queues[queueElement.getPriority()].offer(queueElement);
            debug("offer([{0}]), to P[{1}] delay[{2}ms] accepted[{3}]", queueElement.getElement().toString(),
                  queueElement.getPriority(), queueElement.getDelay(TimeUnit.MILLISECONDS), accepted);
            if (accepted) {
                if (currentSize != null) {
                    currentSize.incrementAndGet();
                }
                queueElement.inQueue = true;
             }
            queueElement.inQueue = true;
        } finally {
            lock.unlock();
         }
         return accepted;
     }
@@ -390,8 +396,8 @@ public class PriorityDelayQueue<E> extends AbstractQueue<PriorityDelayQueue.Queu
      */
     @Override
     public QueueElement<E> poll() {
        lock.lock();
         try {
            lock.lock();
             antiStarvation();
             QueueElement<E> e = null;
             int i = priorities;
@@ -421,8 +427,8 @@ public class PriorityDelayQueue<E> extends AbstractQueue<PriorityDelayQueue.Queu
      */
     @Override
     public QueueElement<E> peek() {
        lock.lock();
         try {
            lock.lock();
             antiStarvation();
             QueueElement<E> e = null;
 
diff --git a/core/src/test/java/org/apache/oozie/service/TestCallableQueueService.java b/core/src/test/java/org/apache/oozie/service/TestCallableQueueService.java
index 903866df1..fefb44893 100644
-- a/core/src/test/java/org/apache/oozie/service/TestCallableQueueService.java
++ b/core/src/test/java/org/apache/oozie/service/TestCallableQueueService.java
@@ -17,20 +17,21 @@
  */
 package org.apache.oozie.service;
 
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.command.XCommand;
 import org.apache.oozie.test.XTestCase;
 import org.apache.oozie.util.XCallable;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

 public class TestCallableQueueService extends XTestCase {
     static AtomicLong EXEC_ORDER = new AtomicLong();
 
@@ -872,4 +873,46 @@ public class TestCallableQueueService extends XTestCase {
         assertTrue(intCallable.executed > callables.get(5).executed);
     }
 
    public void testRemoveUniqueCallables() throws Exception {
        XCommand command = new XCommand("Test", "type", 100) {
            @Override
            protected boolean isLockRequired() {
                return false;
            }

            @Override
            public String getEntityKey() {
                return "TEST";
            }

            @Override
            protected void loadState() throws CommandException {
            }

            @Override
            protected void verifyPrecondition() throws CommandException, PreconditionException {
            }

            @Override
            protected Object execute() throws CommandException {
                return null;
            }
        };
        Services.get().destroy();
        setSystemProperty(CallableQueueService.CONF_THREADS, "1");
        new Services().init();

        CallableQueueService queueservice = Services.get().get(CallableQueueService.class);
        List<String> uniquesBefore = queueservice.getUniqueDump();
        try {
            queueservice.queue(command);
            fail("Expected illegal argument exception: priority = 100");
        }
        catch (Exception e) {
            assertTrue(e.getCause() != null && e.getCause() instanceof IllegalArgumentException);
        }
        List<String> uniquesAfter = queueservice.getUniqueDump();
        uniquesAfter.removeAll(uniquesBefore);
        assertTrue(uniquesAfter.toString(), uniquesAfter.isEmpty());
    }
 }
diff --git a/release-log.txt b/release-log.txt
index 3f8b9bb8f..fa0540fae 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1699 Some of the commands submitted to Oozie internal queue are never executed (sriksun via virag)
 OOZIE-1671 add an option to limit # of coordinator actions for log retrieval (ryota)
 OOZIE-1629 EL function in <timeout> is not evaluated properly (ryota)
 OOZIE-1618 dryrun should check variable substitution in workflow.xml (bowenzhangusa via rkanter)
- 
2.19.1.windows.1

