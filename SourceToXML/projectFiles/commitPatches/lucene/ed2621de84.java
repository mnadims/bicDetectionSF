From ed2621de8402154440e3e1e1979eb620bd23b926 Mon Sep 17 00:00:00 2001
From: Scott Blum <dragonsinth@gmail.com>
Date: Tue, 7 Jun 2016 01:52:16 -0400
Subject: [PATCH] SOLR-9191: OverseerTaskQueue.peekTopN() fatally flawed

--
 solr/CHANGES.txt                              |  6 +-
 .../apache/solr/cloud/DistributedQueue.java   | 95 ++++++++++++-------
 .../solr/cloud/OverseerTaskProcessor.java     |  6 +-
 .../apache/solr/cloud/OverseerTaskQueue.java  | 48 +++-------
 .../solr/cloud/DistributedQueueTest.java      | 44 ++++++++-
 5 files changed, 127 insertions(+), 72 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index d73c61fd517..c8e7c1f6d59 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -46,7 +46,11 @@ Optimizations
   (Ryan Zezeski, Mark Miller, Shawn Heisey, Steve Davids)
 
 ==================  6.2.0 ==================
(No Changes)

Bug Fixes
----------------------

* SOLR-9191: OverseerTaskQueue.peekTopN() fatally flawed (Scott Blum, Noble Paul)
 
 ==================  6.1.0 ==================
 
diff --git a/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java b/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
index e424b7e89e6..afed6f13fef 100644
-- a/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
++ b/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
@@ -17,14 +17,15 @@
 package org.apache.solr.cloud;
 
 import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
 import java.util.List;
 import java.util.NoSuchElementException;
import java.util.SortedSet;
 import java.util.TreeSet;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Preconditions;
@@ -32,6 +33,7 @@ import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.cloud.SolrZkClient;
 import org.apache.solr.common.cloud.ZkCmdExecutor;
import org.apache.solr.common.util.Pair;
 import org.apache.solr.util.stats.TimerContext;
 import org.apache.zookeeper.CreateMode;
 import org.apache.zookeeper.KeeperException;
@@ -80,21 +82,15 @@ public class DistributedQueue {
   private TreeSet<String> knownChildren = new TreeSet<>();
 
   /**
   * Used to wait on a non-empty queue; you must hold {@link #updateLock} and verify that
   * {@link #knownChildren} is empty before waiting on this condition.
   * Used to wait on ZK changes to the child list; you must hold {@link #updateLock} before waiting on this condition.
    */
  private final Condition notEmpty = updateLock.newCondition();
  private final Condition changed = updateLock.newCondition();
 
   /**
   * If non-null, the last watcher to listen for child changes.
   * If non-null, the last watcher to listen for child changes.  If null, the in-memory contents are dirty.
    */
   private ChildWatcher lastWatcher = null;
 
  /**
   * If true, ZK's child list probably doesn't match what's in memory.
   */
  private boolean isDirty = true;

   public DistributedQueue(SolrZkClient zookeeper, String dir) {
     this(zookeeper, dir, new Overseer.Stats());
   }
@@ -165,7 +161,7 @@ public class DistributedQueue {
         if (result != null) {
           return result;
         }
        waitNanos = notEmpty.awaitNanos(waitNanos);
        waitNanos = changed.awaitNanos(waitNanos);
       }
       return null;
     } finally {
@@ -222,7 +218,7 @@ public class DistributedQueue {
         if (result != null) {
           return result;
         }
        notEmpty.await();
        changed.await();
       }
     } finally {
       updateLock.unlock();
@@ -273,25 +269,19 @@ public class DistributedQueue {
   private String firstChild(boolean remove) throws KeeperException, InterruptedException {
     updateLock.lockInterruptibly();
     try {
      // Try to fetch the first in-memory child.
      if (!knownChildren.isEmpty()) {
      // If we're not in a dirty state, and we have in-memory children, return from in-memory.
      if (lastWatcher != null && !knownChildren.isEmpty()) {
         return remove ? knownChildren.pollFirst() : knownChildren.first();
       }
 
      if (lastWatcher != null && !isDirty) {
        // No children, no known updates, and a watcher is already set; nothing we can do.
        return null;
      }

       // Try to fetch an updated list of children from ZK.
       ChildWatcher newWatcher = new ChildWatcher();
       knownChildren = fetchZkChildren(newWatcher);
       lastWatcher = newWatcher; // only set after fetchZkChildren returns successfully
      isDirty = false;
       if (knownChildren.isEmpty()) {
         return null;
       }
      notEmpty.signalAll();
      changed.signalAll();
       return remove ? knownChildren.pollFirst() : knownChildren.first();
     } finally {
       updateLock.unlock();
@@ -325,26 +315,63 @@ public class DistributedQueue {
   }
 
   /**
   * Return the currently-known set of children from memory. If there are no children,
   * waits up to {@code waitMillis} for at least one child to become available. May
   * update the set of known children.
   * Return the currently-known set of elements, using child names from memory. If no children are found, or no
   * children pass {@code acceptFilter}, waits up to {@code waitMillis} for at least one child to become available.
   * <p/>
   * Package-private to support {@link OverseerTaskQueue} specifically.
    */
  SortedSet<String> getChildren(long waitMillis) throws KeeperException, InterruptedException {
  Collection<Pair<String, byte[]>> peekElements(int max, long waitMillis, Function<String, Boolean> acceptFilter) throws KeeperException, InterruptedException {
    List<String> foundChildren = new ArrayList<>();
     long waitNanos = TimeUnit.MILLISECONDS.toNanos(waitMillis);
    while (waitNanos > 0) {
    while (true) {
       // Trigger a fetch if needed.
      firstElement();
      firstChild(false);

       updateLock.lockInterruptibly();
       try {
        if (!knownChildren.isEmpty()) {
          return new TreeSet<>(knownChildren);
        for (String child : knownChildren) {
          if (acceptFilter.apply(child)) {
            foundChildren.add(child);
          }
         }
        waitNanos = notEmpty.awaitNanos(waitNanos);
        if (!foundChildren.isEmpty()) {
          break;
        }
        if (waitNanos <= 0) {
          break;
        }
        waitNanos = changed.awaitNanos(waitNanos);
       } finally {
         updateLock.unlock();
       }

      if (!foundChildren.isEmpty()) {
        break;
      }
    }

    // Technically we could restart the method if we fail to actually obtain any valid children
    // from ZK, but this is a super rare case, and the latency of the ZK fetches would require
    // much more sophisticated waitNanos tracking.
    List<Pair<String, byte[]>> result = new ArrayList<>();
    for (String child : foundChildren) {
      if (result.size() >= max) {
        break;
      }
      try {
        byte[] data = zookeeper.getData(dir + "/" + child, null, null, true);
        result.add(new Pair<>(child, data));
      } catch (KeeperException.NoNodeException e) {
        // Another client deleted the node first, remove the in-memory and continue.
        updateLock.lockInterruptibly();
        try {
          knownChildren.remove(child);
        } finally {
          updateLock.unlock();
        }
      }
     }
    return Collections.emptySortedSet();
    return result;
   }
 
   /**
@@ -418,10 +445,8 @@ public class DistributedQueue {
         if (lastWatcher == this) {
           lastWatcher = null;
         }
        // Do no updates in this thread, just signal state back to client threads.
        isDirty = true;
         // optimistically signal any waiters that the queue may not be empty now, so they can wake up and retry
        notEmpty.signalAll();
        changed.signalAll();
       } finally {
         updateLock.unlock();
       }
diff --git a/solr/core/src/java/org/apache/solr/cloud/OverseerTaskProcessor.java b/solr/core/src/java/org/apache/solr/cloud/OverseerTaskProcessor.java
index 93a7e6fee1b..092ed97cc3a 100644
-- a/solr/core/src/java/org/apache/solr/cloud/OverseerTaskProcessor.java
++ b/solr/core/src/java/org/apache/solr/cloud/OverseerTaskProcessor.java
@@ -190,9 +190,9 @@ public class OverseerTaskProcessor implements Runnable, Closeable {
             cleanUpWorkQueue();
 
           List<QueueEvent> heads = workQueue.peekTopN(MAX_PARALLEL_TASKS, runningZKTasks, 2000L);

          if (heads == null)
          if (heads.isEmpty()) {
             continue;
          }
 
           log.debug("Got {} tasks from work-queue : [{}]", heads.size(), heads.toString());
 
@@ -466,6 +466,8 @@ public class OverseerTaskProcessor implements Runnable, Closeable {
           log.warn("Could not find and remove async call [" + asyncId + "] from the running map.");
         }
       }

      workQueue.remove(head);
     }
 
     private void resetTaskWithException(OverseerMessageHandler messageHandler, String id, String asyncId, String taskKey, ZkNodeProps message) {
diff --git a/solr/core/src/java/org/apache/solr/cloud/OverseerTaskQueue.java b/solr/core/src/java/org/apache/solr/cloud/OverseerTaskQueue.java
index 4cee814e38c..aae7df22069 100644
-- a/solr/core/src/java/org/apache/solr/cloud/OverseerTaskQueue.java
++ b/solr/core/src/java/org/apache/solr/cloud/OverseerTaskQueue.java
@@ -17,7 +17,6 @@
 package org.apache.solr.cloud;
 
 import java.lang.invoke.MethodHandles;

 import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;
@@ -25,6 +24,7 @@ import java.util.TreeSet;
 
 import org.apache.solr.common.cloud.SolrZkClient;
 import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.util.Pair;
 import org.apache.solr.util.stats.TimerContext;
 import org.apache.zookeeper.CreateMode;
 import org.apache.zookeeper.KeeperException;
@@ -82,9 +82,8 @@ public class OverseerTaskQueue extends DistributedQueue {
 
   /**
    * Remove the event and save the response into the other path.
   * 
    */
  public byte[] remove(QueueEvent event) throws KeeperException,
  public void remove(QueueEvent event) throws KeeperException,
       InterruptedException {
     TimerContext time = stats.time(dir + "_remove_event");
     try {
@@ -97,9 +96,10 @@ public class OverseerTaskQueue extends DistributedQueue {
         LOG.info("Response ZK path: " + responsePath + " doesn't exist."
             + "  Requestor may have disconnected from ZooKeeper");
       }
      byte[] data = zookeeper.getData(path, null, null, true);
      zookeeper.delete(path, -1, true);
      return data;
      try {
        zookeeper.delete(path, -1, true);
      } catch (KeeperException.NoNodeException ignored) {
      }
     } finally {
       time.stop();
     }
@@ -227,44 +227,26 @@ public class OverseerTaskQueue extends DistributedQueue {
     ArrayList<QueueEvent> topN = new ArrayList<>();
 
     LOG.debug("Peeking for top {} elements. ExcludeSet: {}", n, excludeSet);
    TimerContext time = null;
    TimerContext time;
     if (waitMillis == Long.MAX_VALUE) time = stats.time(dir + "_peekTopN_wait_forever");
     else time = stats.time(dir + "_peekTopN_wait" + waitMillis);
 
     try {
      for (String headNode : getChildren(waitMillis)) {
        if (topN.size() < n) {
          try {
            String id = dir + "/" + headNode;
            if (excludeSet.contains(id)) continue;
            QueueEvent queueEvent = new QueueEvent(id,
                zookeeper.getData(dir + "/" + headNode, null, null, true), null);
            topN.add(queueEvent);
          } catch (KeeperException.NoNodeException e) {
            // Another client removed the node first, try next
          }
        } else {
          if (topN.size() >= 1) {
            printQueueEventsListElementIds(topN);
            return topN;
          }
        }
      }

      if (topN.size() > 0 ) {
        printQueueEventsListElementIds(topN);
        return topN;
      for (Pair<String, byte[]> element : peekElements(n, waitMillis, child -> !excludeSet.contains(dir + "/" + child))) {
        topN.add(new QueueEvent(dir + "/" + element.first(),
            element.second(), null));
       }
      return null;
      printQueueEventsListElementIds(topN);
      return topN;
     } finally {
       time.stop();
     }
   }
 
   private static void printQueueEventsListElementIds(ArrayList<QueueEvent> topN) {
    if(LOG.isDebugEnabled()) {
      StringBuffer sb = new StringBuffer("[");
      for(QueueEvent queueEvent: topN) {
    if (LOG.isDebugEnabled() && !topN.isEmpty()) {
      StringBuilder sb = new StringBuilder("[");
      for (QueueEvent queueEvent : topN) {
         sb.append(queueEvent.getId()).append(", ");
       }
       sb.append("]");
diff --git a/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java b/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
index 840e7e51f7f..f42f1014cc8 100644
-- a/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
@@ -19,7 +19,6 @@ package org.apache.solr.cloud;
 import java.nio.charset.Charset;
 import java.util.NoSuchElementException;
 import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
@@ -137,6 +136,49 @@ public class DistributedQueueTest extends SolrTestCaseJ4 {
     assertNull(dq.poll());
   }
 
  @Test
  public void testPeekElements() throws Exception {
    String dqZNode = "/distqueue/test";
    byte[] data = "hello world".getBytes(UTF8);

    DistributedQueue dq = makeDistributedQueue(dqZNode);

    // Populate with data.
    dq.offer(data);
    dq.offer(data);
    dq.offer(data);

    // Should be able to get 0, 1, 2, or 3 instantly
    for (int i = 0; i <= 3; ++i) {
      assertEquals(i, dq.peekElements(i, 0, child -> true).size());
    }

    // Asking for more should return only 3.
    assertEquals(3, dq.peekElements(4, 0, child -> true).size());

    // If we filter everything out, we should block for the full time.
    long start = System.nanoTime();
    assertEquals(0, dq.peekElements(4, 1000, child -> false).size());
    assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(500));

    // If someone adds a new matching element while we're waiting, we should return immediately.
    executor.submit(() -> {
      try {
        Thread.sleep(500);
        dq.offer(data);
      } catch (Exception e) {
        // ignore
      }
    });
    start = System.nanoTime();
    assertEquals(1, dq.peekElements(4, 2000, child -> {
      // The 4th element in the queue will end with a "3".
      return child.endsWith("3");
    }).size());
    assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(1000));
    assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(250));
  }

   private void forceSessionExpire() throws InterruptedException, TimeoutException {
     long sessionId = zkClient.getSolrZooKeeper().getSessionId();
     zkServer.expire(sessionId);
- 
2.19.1.windows.1

