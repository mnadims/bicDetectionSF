From a2a5cb60b09491cb672978ba9442f02373392c67 Mon Sep 17 00:00:00 2001
From: Konstantin V Shvachko <shv@apache.org>
Date: Thu, 16 Jun 2016 18:20:49 -0700
Subject: [PATCH] HADOOP-13189. FairCallQueue makes callQueue larger than the
 configured capacity. Contributed by Vinitha Gankidi.

--
 .../apache/hadoop/ipc/CallQueueManager.java   |  4 +--
 .../org/apache/hadoop/ipc/FairCallQueue.java  | 19 ++++++++-----
 .../hadoop/ipc/TestCallQueueManager.java      |  4 +--
 .../apache/hadoop/ipc/TestFairCallQueue.java  | 27 ++++++++++++++-----
 4 files changed, 37 insertions(+), 17 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/CallQueueManager.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/CallQueueManager.java
index 7a19217bebc..cbf8ebd320e 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/CallQueueManager.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/CallQueueManager.java
@@ -72,8 +72,8 @@ public CallQueueManager(Class<? extends BlockingQueue<E>> backingClass,
     this.clientBackOffEnabled = clientBackOffEnabled;
     this.putRef = new AtomicReference<BlockingQueue<E>>(bq);
     this.takeRef = new AtomicReference<BlockingQueue<E>>(bq);
    LOG.info("Using callQueue: " + backingClass + " scheduler: " +
        schedulerClass);
    LOG.info("Using callQueue: " + backingClass + " queueCapacity: " +
        maxQueueSize + " scheduler: " + schedulerClass);
   }
 
   private static <T extends RpcScheduler> T createScheduler(
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java
index 435c454176b..38b196dc808 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/FairCallQueue.java
@@ -75,11 +75,12 @@ private void signalNotEmpty() {
 
   /**
    * Create a FairCallQueue.
   * @param capacity the maximum size of each sub-queue
   * @param capacity the total size of all sub-queues
    * @param ns the prefix to use for configuration
    * @param conf the configuration to read from
   * Notes: the FairCallQueue has no fixed capacity. Rather, it has a minimum
   * capacity of `capacity` and a maximum capacity of `capacity * number_queues`
   * Notes: Each sub-queue has a capacity of `capacity / numSubqueues`.
   * The first or the highest priority sub-queue has an excess capacity
   * of `capacity % numSubqueues`
    */
   public FairCallQueue(int priorityLevels, int capacity, String ns,
       Configuration conf) {
@@ -88,13 +89,19 @@ public FairCallQueue(int priorityLevels, int capacity, String ns,
           "at least 1");
     }
     int numQueues = priorityLevels;
    LOG.info("FairCallQueue is in use with " + numQueues + " queues.");
    LOG.info("FairCallQueue is in use with " + numQueues +
        " queues with total capacity of " + capacity);
 
     this.queues = new ArrayList<BlockingQueue<E>>(numQueues);
     this.overflowedCalls = new ArrayList<AtomicLong>(numQueues);

    int queueCapacity = capacity / numQueues;
    int capacityForFirstQueue = queueCapacity + (capacity % numQueues);
     for(int i=0; i < numQueues; i++) {
      this.queues.add(new LinkedBlockingQueue<E>(capacity));
      if (i == 0) {
        this.queues.add(new LinkedBlockingQueue<E>(capacityForFirstQueue));
      } else {
        this.queues.add(new LinkedBlockingQueue<E>(queueCapacity));
      }
       this.overflowedCalls.add(new AtomicLong(0));
     }
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestCallQueueManager.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestCallQueueManager.java
index af9ce1b08bc..121165785cd 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestCallQueueManager.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestCallQueueManager.java
@@ -214,9 +214,9 @@ public void testFcqBackwardCompatibility() throws InterruptedException {
     assertTrue(queue.getCanonicalName().equals(queueClassName));
 
     manager = new CallQueueManager<FakeCall>(queue, scheduler, false,
        2, "", conf);
        8, "", conf);
 
    // Default FCQ has 4 levels and the max capacity is 2 x 4
    // Default FCQ has 4 levels and the max capacity is 8
     assertCanPut(manager, 3, 3);
   }
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java
index 4a8ad3b9271..8c96c2e500a 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestFairCallQueue.java
@@ -18,12 +18,6 @@
 
 package org.apache.hadoop.ipc;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;
 
@@ -68,7 +62,26 @@ public void setUp() {
     Configuration conf = new Configuration();
     conf.setInt("ns." + FairCallQueue.IPC_CALLQUEUE_PRIORITY_LEVELS_KEY, 2);
 
    fcq = new FairCallQueue<Schedulable>(2, 5, "ns", conf);
    fcq = new FairCallQueue<Schedulable>(2, 10, "ns", conf);
  }

  // Validate that the total capacity of all subqueues equals
  // the maxQueueSize for different values of maxQueueSize
  public void testTotalCapacityOfSubQueues() {
    Configuration conf = new Configuration();
    FairCallQueue<Schedulable> fairCallQueue;
    fairCallQueue = new FairCallQueue<Schedulable>(1, 1000, "ns", conf);
    assertEquals(fairCallQueue.remainingCapacity(), 1000);
    fairCallQueue = new FairCallQueue<Schedulable>(4, 1000, "ns", conf);
    assertEquals(fairCallQueue.remainingCapacity(), 1000);
    fairCallQueue = new FairCallQueue<Schedulable>(7, 1000, "ns", conf);
    assertEquals(fairCallQueue.remainingCapacity(), 1000);
    fairCallQueue = new FairCallQueue<Schedulable>(1, 1025, "ns", conf);
    assertEquals(fairCallQueue.remainingCapacity(), 1025);
    fairCallQueue = new FairCallQueue<Schedulable>(4, 1025, "ns", conf);
    assertEquals(fairCallQueue.remainingCapacity(), 1025);
    fairCallQueue = new FairCallQueue<Schedulable>(7, 1025, "ns", conf);
    assertEquals(fairCallQueue.remainingCapacity(), 1025);
   }
 
   //
- 
2.19.1.windows.1

