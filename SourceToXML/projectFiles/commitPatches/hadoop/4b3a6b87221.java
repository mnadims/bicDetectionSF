From 4b3a6b87221076a6b5df2bf4243575018e5f1793 Mon Sep 17 00:00:00 2001
From: Arpit Agarwal <arp@apache.org>
Date: Fri, 22 Aug 2014 22:16:15 +0000
Subject: [PATCH] HADOOP-10282. Create a FairCallQueue: a multi-level call
 queue which schedules incoming calls and multiplexes outgoing calls.
 (Contributed by Chris Li)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1619938 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |   4 +
 .../org/apache/hadoop/ipc/FairCallQueue.java  | 449 ++++++++++++++++++
 .../hadoop/ipc/FairCallQueueMXBean.java       |  27 ++
 .../org/apache/hadoop/ipc/RpcMultiplexer.java |  32 ++
 .../ipc/WeightedRoundRobinMultiplexer.java    |   2 +-
 .../apache/hadoop/ipc/TestFairCallQueue.java  | 392 +++++++++++++++
 6 files changed, 905 insertions(+), 1 deletion(-)
 create mode 100644 hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java
 create mode 100644 hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueueMXBean.java
 create mode 100644 hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/RpcMultiplexer.java
 create mode 100644 hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 50a6b82afd0..0291c758e03 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -560,6 +560,10 @@ Release 2.6.0 - UNRELEASED
     HADOOP-10224. JavaKeyStoreProvider has to protect against corrupting
     underlying store. (asuresh via tucu)
 
    HADOOP-10282. Create a FairCallQueue: a multi-level call queue which
    schedules incoming calls and multiplexes outgoing calls. (Chris Li via
    Arpit Agarwal)

   BUG FIXES
 
     HADOOP-10781. Unportable getgrouplist() usage breaks FreeBSD (Dmitry
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java
new file mode 100644
index 00000000000..0b56243db58
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java
@@ -0,0 +1,449 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ipc;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.util.MBeans;

/**
 * A queue with multiple levels for each priority.
 */
public class FairCallQueue<E extends Schedulable> extends AbstractQueue<E>
  implements BlockingQueue<E> {
  // Configuration Keys
  public static final int    IPC_CALLQUEUE_PRIORITY_LEVELS_DEFAULT = 4;
  public static final String IPC_CALLQUEUE_PRIORITY_LEVELS_KEY =
    "faircallqueue.priority-levels";

  public static final Log LOG = LogFactory.getLog(FairCallQueue.class);

  /* The queues */
  private final ArrayList<BlockingQueue<E>> queues;

  /* Read locks */
  private final ReentrantLock takeLock = new ReentrantLock();
  private final Condition notEmpty = takeLock.newCondition();
  private void signalNotEmpty() {
    takeLock.lock();
    try {
      notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
  }

  /* Scheduler picks which queue to place in */
  private RpcScheduler scheduler;

  /* Multiplexer picks which queue to draw from */
  private RpcMultiplexer multiplexer;

  /* Statistic tracking */
  private final ArrayList<AtomicLong> overflowedCalls;

  /**
   * Create a FairCallQueue.
   * @param capacity the maximum size of each sub-queue
   * @param ns the prefix to use for configuration
   * @param conf the configuration to read from
   * Notes: the FairCallQueue has no fixed capacity. Rather, it has a minimum
   * capacity of `capacity` and a maximum capacity of `capacity * number_queues`
   */
  public FairCallQueue(int capacity, String ns, Configuration conf) {
    int numQueues = parseNumQueues(ns, conf);
    LOG.info("FairCallQueue is in use with " + numQueues + " queues.");

    this.queues = new ArrayList<BlockingQueue<E>>(numQueues);
    this.overflowedCalls = new ArrayList<AtomicLong>(numQueues);

    for(int i=0; i < numQueues; i++) {
      this.queues.add(new LinkedBlockingQueue<E>(capacity));
      this.overflowedCalls.add(new AtomicLong(0));
    }

    this.scheduler = new DecayRpcScheduler(numQueues, ns, conf);
    this.multiplexer = new WeightedRoundRobinMultiplexer(numQueues, ns, conf);

    // Make this the active source of metrics
    MetricsProxy mp = MetricsProxy.getInstance(ns);
    mp.setDelegate(this);
  }

  /**
   * Read the number of queues from the configuration.
   * This will affect the FairCallQueue's overall capacity.
   * @throws IllegalArgumentException on invalid queue count
   */
  private static int parseNumQueues(String ns, Configuration conf) {
    int retval = conf.getInt(ns + "." + IPC_CALLQUEUE_PRIORITY_LEVELS_KEY,
      IPC_CALLQUEUE_PRIORITY_LEVELS_DEFAULT);
    if(retval < 1) {
      throw new IllegalArgumentException("numQueues must be at least 1");
    }
    return retval;
  }

  /**
   * Returns the first non-empty queue with equal or lesser priority
   * than <i>startIdx</i>. Wraps around, searching a maximum of N
   * queues, where N is this.queues.size().
   *
   * @param startIdx the queue number to start searching at
   * @return the first non-empty queue with less priority, or null if
   * everything was empty
   */
  private BlockingQueue<E> getFirstNonEmptyQueue(int startIdx) {
    final int numQueues = this.queues.size();
    for(int i=0; i < numQueues; i++) {
      int idx = (i + startIdx) % numQueues; // offset and wrap around
      BlockingQueue<E> queue = this.queues.get(idx);
      if (queue.size() != 0) {
        return queue;
      }
    }

    // All queues were empty
    return null;
  }

  /* AbstractQueue and BlockingQueue methods */

  /**
   * Put and offer follow the same pattern:
   * 1. Get a priorityLevel from the scheduler
   * 2. Get the nth sub-queue matching this priorityLevel
   * 3. delegate the call to this sub-queue.
   *
   * But differ in how they handle overflow:
   * - Put will move on to the next queue until it lands on the last queue
   * - Offer does not attempt other queues on overflow
   */
  @Override
  public void put(E e) throws InterruptedException {
    int priorityLevel = scheduler.getPriorityLevel(e);

    final int numLevels = this.queues.size();
    while (true) {
      BlockingQueue<E> q = this.queues.get(priorityLevel);
      boolean res = q.offer(e);
      if (!res) {
        // Update stats
        this.overflowedCalls.get(priorityLevel).getAndIncrement();

        // If we failed to insert, try again on the next level
        priorityLevel++;

        if (priorityLevel == numLevels) {
          // That was the last one, we will block on put in the last queue
          // Delete this line to drop the call
          this.queues.get(priorityLevel-1).put(e);
          break;
        }
      } else {
        break;
      }
    }


    signalNotEmpty();
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit)
      throws InterruptedException {
    int priorityLevel = scheduler.getPriorityLevel(e);
    BlockingQueue<E> q = this.queues.get(priorityLevel);
    boolean ret = q.offer(e, timeout, unit);

    signalNotEmpty();

    return ret;
  }

  @Override
  public boolean offer(E e) {
    int priorityLevel = scheduler.getPriorityLevel(e);
    BlockingQueue<E> q = this.queues.get(priorityLevel);
    boolean ret = q.offer(e);

    signalNotEmpty();

    return ret;
  }

  @Override
  public E take() throws InterruptedException {
    int startIdx = this.multiplexer.getAndAdvanceCurrentIndex();

    takeLock.lockInterruptibly();
    try {
      // Wait while queue is empty
      for (;;) {
        BlockingQueue<E> q = this.getFirstNonEmptyQueue(startIdx);
        if (q != null) {
          // Got queue, so return if we can poll out an object
          E e = q.poll();
          if (e != null) {
            return e;
          }
        }

        notEmpty.await();
      }
    } finally {
      takeLock.unlock();
    }
  }

  @Override
  public E poll(long timeout, TimeUnit unit)
      throws InterruptedException {

    int startIdx = this.multiplexer.getAndAdvanceCurrentIndex();

    long nanos = unit.toNanos(timeout);
    takeLock.lockInterruptibly();
    try {
      for (;;) {
        BlockingQueue<E> q = this.getFirstNonEmptyQueue(startIdx);
        if (q != null) {
          E e = q.poll();
          if (e != null) {
            // Escape condition: there might be something available
            return e;
          }
        }

        if (nanos <= 0) {
          // Wait has elapsed
          return null;
        }

        try {
          // Now wait on the condition for a bit. If we get
          // spuriously awoken we'll re-loop
          nanos = notEmpty.awaitNanos(nanos);
        } catch (InterruptedException ie) {
          notEmpty.signal(); // propagate to a non-interrupted thread
          throw ie;
        }
      }
    } finally {
      takeLock.unlock();
    }
  }

  /**
   * poll() provides no strict consistency: it is possible for poll to return
   * null even though an element is in the queue.
   */
  @Override
  public E poll() {
    int startIdx = this.multiplexer.getAndAdvanceCurrentIndex();

    BlockingQueue<E> q = this.getFirstNonEmptyQueue(startIdx);
    if (q == null) {
      return null; // everything is empty
    }

    // Delegate to the sub-queue's poll, which could still return null
    return q.poll();
  }

  /**
   * Peek, like poll, provides no strict consistency.
   */
  @Override
  public E peek() {
    BlockingQueue<E> q = this.getFirstNonEmptyQueue(0);
    if (q == null) {
      return null;
    } else {
      return q.peek();
    }
  }

  /**
   * Size returns the sum of all sub-queue sizes, so it may be greater than
   * capacity.
   * Note: size provides no strict consistency, and should not be used to
   * control queue IO.
   */
  @Override
  public int size() {
    int size = 0;
    for (BlockingQueue q : this.queues) {
      size += q.size();
    }
    return size;
  }

  /**
   * Iterator is not implemented, as it is not needed.
   */
  @Override
  public Iterator<E> iterator() {
    throw new NotImplementedException();
  }

  /**
   * drainTo defers to each sub-queue. Note that draining from a FairCallQueue
   * to another FairCallQueue will likely fail, since the incoming calls
   * may be scheduled differently in the new FairCallQueue. Nonetheless this
   * method is provided for completeness.
   */
  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    int sum = 0;
    for (BlockingQueue<E> q : this.queues) {
      sum += q.drainTo(c, maxElements);
    }
    return sum;
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    int sum = 0;
    for (BlockingQueue<E> q : this.queues) {
      sum += q.drainTo(c);
    }
    return sum;
  }

  /**
   * Returns maximum remaining capacity. This does not reflect how much you can
   * ideally fit in this FairCallQueue, as that would depend on the scheduler's
   * decisions.
   */
  @Override
  public int remainingCapacity() {
    int sum = 0;
    for (BlockingQueue q : this.queues) {
      sum += q.remainingCapacity();
    }
    return sum;
  }

  /**
   * MetricsProxy is a singleton because we may init multiple
   * FairCallQueues, but the metrics system cannot unregister beans cleanly.
   */
  private static final class MetricsProxy implements FairCallQueueMXBean {
    // One singleton per namespace
    private static final HashMap<String, MetricsProxy> INSTANCES =
      new HashMap<String, MetricsProxy>();

    // Weakref for delegate, so we don't retain it forever if it can be GC'd
    private WeakReference<FairCallQueue> delegate;

    // Keep track of how many objects we registered
    private int revisionNumber = 0;

    private MetricsProxy(String namespace) {
      MBeans.register(namespace, "FairCallQueue", this);
    }

    public static synchronized MetricsProxy getInstance(String namespace) {
      MetricsProxy mp = INSTANCES.get(namespace);
      if (mp == null) {
        // We must create one
        mp = new MetricsProxy(namespace);
        INSTANCES.put(namespace, mp);
      }
      return mp;
    }

    public void setDelegate(FairCallQueue obj) {
      this.delegate = new WeakReference<FairCallQueue>(obj);
      this.revisionNumber++;
    }

    @Override
    public int[] getQueueSizes() {
      FairCallQueue obj = this.delegate.get();
      if (obj == null) {
        return new int[]{};
      }

      return obj.getQueueSizes();
    }

    @Override
    public long[] getOverflowedCalls() {
      FairCallQueue obj = this.delegate.get();
      if (obj == null) {
        return new long[]{};
      }

      return obj.getOverflowedCalls();
    }

    @Override public int getRevision() {
      return revisionNumber;
    }
  }

  // FairCallQueueMXBean
  public int[] getQueueSizes() {
    int numQueues = queues.size();
    int[] sizes = new int[numQueues];
    for (int i=0; i < numQueues; i++) {
      sizes[i] = queues.get(i).size();
    }
    return sizes;
  }

  public long[] getOverflowedCalls() {
    int numQueues = queues.size();
    long[] calls = new long[numQueues];
    for (int i=0; i < numQueues; i++) {
      calls[i] = overflowedCalls.get(i).get();
    }
    return calls;
  }

  // For testing
  @VisibleForTesting
  public void setScheduler(RpcScheduler newScheduler) {
    this.scheduler = newScheduler;
  }

  @VisibleForTesting
  public void setMultiplexer(RpcMultiplexer newMux) {
    this.multiplexer = newMux;
  }
}
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueueMXBean.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueueMXBean.java
new file mode 100644
index 00000000000..bd68ecb1ad3
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueueMXBean.java
@@ -0,0 +1,27 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ipc;

public interface FairCallQueueMXBean {
  // Get the size of each subqueue, the index corrosponding to the priority
  // level.
  int[] getQueueSizes();
  long[] getOverflowedCalls();
  int getRevision();
}
\ No newline at end of file
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/RpcMultiplexer.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/RpcMultiplexer.java
new file mode 100644
index 00000000000..01eecc55cfa
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/RpcMultiplexer.java
@@ -0,0 +1,32 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ipc;

/**
 * Implement this interface to make a pluggable multiplexer in the
 * FairCallQueue.
 */
public interface RpcMultiplexer {
  /**
   * Should get current index and optionally perform whatever is needed
   * to prepare the next index.
   * @return current index
   */
  int getAndAdvanceCurrentIndex();
}
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/WeightedRoundRobinMultiplexer.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/WeightedRoundRobinMultiplexer.java
index 497ca757461..cfda94734cf 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/WeightedRoundRobinMultiplexer.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/WeightedRoundRobinMultiplexer.java
@@ -38,7 +38,7 @@
  * There may be more reads than the minimum due to race conditions. This is
  * allowed by design for performance reasons.
  */
public class WeightedRoundRobinMultiplexer {
public class WeightedRoundRobinMultiplexer implements RpcMultiplexer {
   // Config keys
   public static final String IPC_CALLQUEUE_WRRMUX_WEIGHTS_KEY =
     "faircallqueue.multiplexer.weights";
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java
new file mode 100644
index 00000000000..acbedc50f9f
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java
@@ -0,0 +1,392 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.conf.Configuration;
import org.mockito.Matchers;

import static org.apache.hadoop.ipc.FairCallQueue.IPC_CALLQUEUE_PRIORITY_LEVELS_KEY;

public class TestFairCallQueue extends TestCase {
  private FairCallQueue<Schedulable> fcq;

  private Schedulable mockCall(String id) {
    Schedulable mockCall = mock(Schedulable.class);
    UserGroupInformation ugi = mock(UserGroupInformation.class);

    when(ugi.getUserName()).thenReturn(id);
    when(mockCall.getUserGroupInformation()).thenReturn(ugi);

    return mockCall;
  }

  // A scheduler which always schedules into priority zero
  private RpcScheduler alwaysZeroScheduler;
  {
    RpcScheduler sched = mock(RpcScheduler.class);
    when(sched.getPriorityLevel(Matchers.<Schedulable>any())).thenReturn(0); // always queue 0
    alwaysZeroScheduler = sched;
  }

  public void setUp() {
    Configuration conf = new Configuration();
    conf.setInt("ns." + IPC_CALLQUEUE_PRIORITY_LEVELS_KEY, 2);

    fcq = new FairCallQueue<Schedulable>(5, "ns", conf);
  }

  //
  // Ensure that FairCallQueue properly implements BlockingQueue
  //
  public void testPollReturnsNullWhenEmpty() {
    assertNull(fcq.poll());
  }

  public void testPollReturnsTopCallWhenNotEmpty() {
    Schedulable call = mockCall("c");
    assertTrue(fcq.offer(call));

    assertEquals(call, fcq.poll());

    // Poll took it out so the fcq is empty
    assertEquals(0, fcq.size());
  }

  public void testOfferSucceeds() {
    fcq.setScheduler(alwaysZeroScheduler);

    for (int i = 0; i < 5; i++) {
      // We can fit 10 calls
      assertTrue(fcq.offer(mockCall("c")));
    }

    assertEquals(5, fcq.size());
  }

  public void testOfferFailsWhenFull() {
    fcq.setScheduler(alwaysZeroScheduler);
    for (int i = 0; i < 5; i++) { assertTrue(fcq.offer(mockCall("c"))); }

    assertFalse(fcq.offer(mockCall("c"))); // It's full

    assertEquals(5, fcq.size());
  }

  public void testOfferSucceedsWhenScheduledLowPriority() {
    // Scheduler will schedule into queue 0 x 5, then queue 1
    RpcScheduler sched = mock(RpcScheduler.class);
    when(sched.getPriorityLevel(Matchers.<Schedulable>any())).thenReturn(0, 0, 0, 0, 0, 1, 0);
    fcq.setScheduler(sched);
    for (int i = 0; i < 5; i++) { assertTrue(fcq.offer(mockCall("c"))); }

    assertTrue(fcq.offer(mockCall("c")));

    assertEquals(6, fcq.size());
  }

  public void testPeekNullWhenEmpty() {
    assertNull(fcq.peek());
  }

  public void testPeekNonDestructive() {
    Schedulable call = mockCall("c");
    assertTrue(fcq.offer(call));

    assertEquals(call, fcq.peek());
    assertEquals(call, fcq.peek()); // Non-destructive
    assertEquals(1, fcq.size());
  }

  public void testPeekPointsAtHead() {
    Schedulable call = mockCall("c");
    Schedulable next = mockCall("b");
    fcq.offer(call);
    fcq.offer(next);

    assertEquals(call, fcq.peek()); // Peek points at the head
  }

  public void testPollTimeout() throws InterruptedException {
    fcq.setScheduler(alwaysZeroScheduler);

    assertNull(fcq.poll(10, TimeUnit.MILLISECONDS));
  }

  public void testPollSuccess() throws InterruptedException {
    fcq.setScheduler(alwaysZeroScheduler);

    Schedulable call = mockCall("c");
    assertTrue(fcq.offer(call));

    assertEquals(call, fcq.poll(10, TimeUnit.MILLISECONDS));

    assertEquals(0, fcq.size());
  }

  public void testOfferTimeout() throws InterruptedException {
    fcq.setScheduler(alwaysZeroScheduler);
    for (int i = 0; i < 5; i++) {
      assertTrue(fcq.offer(mockCall("c"), 10, TimeUnit.MILLISECONDS));
    }

    assertFalse(fcq.offer(mockCall("e"), 10, TimeUnit.MILLISECONDS)); // It's full

    assertEquals(5, fcq.size());
  }

  public void testDrainTo() {
    Configuration conf = new Configuration();
    conf.setInt("ns." + IPC_CALLQUEUE_PRIORITY_LEVELS_KEY, 2);
    FairCallQueue<Schedulable> fcq2 = new FairCallQueue<Schedulable>(10, "ns", conf);

    fcq.setScheduler(alwaysZeroScheduler);
    fcq2.setScheduler(alwaysZeroScheduler);

    // Start with 3 in fcq, to be drained
    for (int i = 0; i < 3; i++) {
      fcq.offer(mockCall("c"));
    }

    fcq.drainTo(fcq2);

    assertEquals(0, fcq.size());
    assertEquals(3, fcq2.size());
  }

  public void testDrainToWithLimit() {
    Configuration conf = new Configuration();
    conf.setInt("ns." + IPC_CALLQUEUE_PRIORITY_LEVELS_KEY, 2);
    FairCallQueue<Schedulable> fcq2 = new FairCallQueue<Schedulable>(10, "ns", conf);

    fcq.setScheduler(alwaysZeroScheduler);
    fcq2.setScheduler(alwaysZeroScheduler);

    // Start with 3 in fcq, to be drained
    for (int i = 0; i < 3; i++) {
      fcq.offer(mockCall("c"));
    }

    fcq.drainTo(fcq2, 2);

    assertEquals(1, fcq.size());
    assertEquals(2, fcq2.size());
  }

  public void testInitialRemainingCapacity() {
    assertEquals(10, fcq.remainingCapacity());
  }

  public void testFirstQueueFullRemainingCapacity() {
    fcq.setScheduler(alwaysZeroScheduler);
    while (fcq.offer(mockCall("c"))) ; // Queue 0 will fill up first, then queue 1

    assertEquals(5, fcq.remainingCapacity());
  }

  public void testAllQueuesFullRemainingCapacity() {
    RpcScheduler sched = mock(RpcScheduler.class);
    when(sched.getPriorityLevel(Matchers.<Schedulable>any())).thenReturn(0, 0, 0, 0, 0, 1, 1, 1, 1, 1);
    fcq.setScheduler(sched);
    while (fcq.offer(mockCall("c"))) ;

    assertEquals(0, fcq.remainingCapacity());
    assertEquals(10, fcq.size());
  }

  public void testQueuesPartialFilledRemainingCapacity() {
    RpcScheduler sched = mock(RpcScheduler.class);
    when(sched.getPriorityLevel(Matchers.<Schedulable>any())).thenReturn(0, 1, 0, 1, 0);
    fcq.setScheduler(sched);
    for (int i = 0; i < 5; i++) { fcq.offer(mockCall("c")); }

    assertEquals(5, fcq.remainingCapacity());
    assertEquals(5, fcq.size());
  }

  /**
   * Putter produces FakeCalls
   */
  public class Putter implements Runnable {
    private final BlockingQueue<Schedulable> cq;

    public final String tag;
    public volatile int callsAdded = 0; // How many calls we added, accurate unless interrupted
    private final int maxCalls;

    public Putter(BlockingQueue<Schedulable> aCq, int maxCalls, String tag) {
      this.maxCalls = maxCalls;
      this.cq = aCq;
      this.tag = tag;
    }

    private String getTag() {
      if (this.tag != null) return this.tag;
      return "";
    }

    @Override
    public void run() {
      try {
        // Fill up to max (which is infinite if maxCalls < 0)
        while (callsAdded < maxCalls || maxCalls < 0) {
          cq.put(mockCall(getTag()));
          callsAdded++;
        }
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  /**
   * Taker consumes FakeCalls
   */
  public class Taker implements Runnable {
    private final BlockingQueue<Schedulable> cq;

    public final String tag; // if >= 0 means we will only take the matching tag, and put back
                          // anything else
    public volatile int callsTaken = 0; // total calls taken, accurate if we aren't interrupted
    public volatile Schedulable lastResult = null; // the last thing we took
    private final int maxCalls; // maximum calls to take

    private IdentityProvider uip;

    public Taker(BlockingQueue<Schedulable> aCq, int maxCalls, String tag) {
      this.maxCalls = maxCalls;
      this.cq = aCq;
      this.tag = tag;
      this.uip = new UserIdentityProvider();
    }

    @Override
    public void run() {
      try {
        // Take while we don't exceed maxCalls, or if maxCalls is undefined (< 0)
        while (callsTaken < maxCalls || maxCalls < 0) {
          Schedulable res = cq.take();
          String identity = uip.makeIdentity(res);

          if (tag != null && this.tag.equals(identity)) {
            // This call does not match our tag, we should put it back and try again
            cq.put(res);
          } else {
            callsTaken++;
            lastResult = res;
          }
        }
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  // Assert we can take exactly the numberOfTakes
  public void assertCanTake(BlockingQueue<Schedulable> cq, int numberOfTakes,
    int takeAttempts) throws InterruptedException {

    Taker taker = new Taker(cq, takeAttempts, "default");
    Thread t = new Thread(taker);
    t.start();
    t.join(100);

    assertEquals(numberOfTakes, taker.callsTaken);
    t.interrupt();
  }

  // Assert we can put exactly the numberOfPuts
  public void assertCanPut(BlockingQueue<Schedulable> cq, int numberOfPuts,
    int putAttempts) throws InterruptedException {

    Putter putter = new Putter(cq, putAttempts, null);
    Thread t = new Thread(putter);
    t.start();
    t.join(100);

    assertEquals(numberOfPuts, putter.callsAdded);
    t.interrupt();
  }

  // Make sure put will overflow into lower queues when the top is full
  public void testPutOverflows() throws InterruptedException {
    fcq.setScheduler(alwaysZeroScheduler);

    // We can fit more than 5, even though the scheduler suggests the top queue
    assertCanPut(fcq, 8, 8);
    assertEquals(8, fcq.size());
  }

  public void testPutBlocksWhenAllFull() throws InterruptedException {
    fcq.setScheduler(alwaysZeroScheduler);

    assertCanPut(fcq, 10, 10); // Fill up
    assertEquals(10, fcq.size());

    // Put more which causes overflow
    assertCanPut(fcq, 0, 1); // Will block
  }

  public void testTakeBlocksWhenEmpty() throws InterruptedException {
    fcq.setScheduler(alwaysZeroScheduler);
    assertCanTake(fcq, 0, 1);
  }

  public void testTakeRemovesCall() throws InterruptedException {
    fcq.setScheduler(alwaysZeroScheduler);
    Schedulable call = mockCall("c");
    fcq.offer(call);

    assertEquals(call, fcq.take());
    assertEquals(0, fcq.size());
  }

  public void testTakeTriesNextQueue() throws InterruptedException {
    // Make a FCQ filled with calls in q 1 but empty in q 0
    RpcScheduler q1Scheduler = mock(RpcScheduler.class);
    when(q1Scheduler.getPriorityLevel(Matchers.<Schedulable>any())).thenReturn(1);
    fcq.setScheduler(q1Scheduler);

    // A mux which only draws from q 0
    RpcMultiplexer q0mux = mock(RpcMultiplexer.class);
    when(q0mux.getAndAdvanceCurrentIndex()).thenReturn(0);
    fcq.setMultiplexer(q0mux);

    Schedulable call = mockCall("c");
    fcq.put(call);

    // Take from q1 even though mux said q0, since q0 empty
    assertEquals(call, fcq.take());
    assertEquals(0, fcq.size());
  }
}
\ No newline at end of file
- 
2.19.1.windows.1

