From 43c2b2320dcf344c42086ceb782e0fc53c439952 Mon Sep 17 00:00:00 2001
From: Scott Blum <dragonsinth@gmail.com>
Date: Mon, 17 Apr 2017 18:27:12 -0400
Subject: [PATCH] SOLR-10420: fix watcher leak in DistributedQueue

--
 .../apache/solr/cloud/DistributedQueue.java   | 56 ++++++++++++-------
 .../solr/cloud/DistributedQueueTest.java      | 50 ++++++++++++++++-
 2 files changed, 84 insertions(+), 22 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java b/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
index e7ac5e5fd16..6c28cc69c3f 100644
-- a/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
++ b/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
@@ -86,10 +86,9 @@ public class DistributedQueue {
    */
   private final Condition changed = updateLock.newCondition();
 
  /**
   * If non-null, the last watcher to listen for child changes.  If null, the in-memory contents are dirty.
   */
  private ChildWatcher lastWatcher = null;
  private boolean isDirty = true;

  private int watcherCount = 0;
 
   public DistributedQueue(SolrZkClient zookeeper, String dir) {
     this(zookeeper, dir, new Overseer.Stats());
@@ -238,10 +237,10 @@ public class DistributedQueue {
     try {
       while (true) {
         try {
          // We don't need to explicitly set isDirty here; if there is a watcher, it will
          // see the update and set the bit itself; if there is no watcher we can defer
          // the update anyway.
          // Explicitly set isDirty here so that synchronous same-thread calls behave as expected.
          // This will get set again when the watcher actually fires, but that's ok.
           zookeeper.create(dir + "/" + PREFIX, data, CreateMode.PERSISTENT_SEQUENTIAL, true);
          isDirty = true;
           return;
         } catch (KeeperException.NoNodeException e) {
           try {
@@ -269,15 +268,25 @@ public class DistributedQueue {
   private String firstChild(boolean remove) throws KeeperException, InterruptedException {
     updateLock.lockInterruptibly();
     try {
      // If we're not in a dirty state, and we have in-memory children, return from in-memory.
      if (lastWatcher != null && !knownChildren.isEmpty()) {
        return remove ? knownChildren.pollFirst() : knownChildren.first();
      if (!isDirty) {
        // If we're not in a dirty state...
        if (!knownChildren.isEmpty()) {
          // and we have in-memory children, return from in-memory.
          return remove ? knownChildren.pollFirst() : knownChildren.first();
        } else {
          // otherwise there's nothing to return
          return null;
        }
       }
 
      // Try to fetch an updated list of children from ZK.
      ChildWatcher newWatcher = new ChildWatcher();
      // Dirty, try to fetch an updated list of children from ZK.
      // Only set a new watcher if there isn't already a watcher.
      ChildWatcher newWatcher = (watcherCount == 0) ? new ChildWatcher() : null;
       knownChildren = fetchZkChildren(newWatcher);
      lastWatcher = newWatcher; // only set after fetchZkChildren returns successfully
      if (newWatcher != null) {
        watcherCount++; // watcher was successfully set
      }
      isDirty = false;
       if (knownChildren.isEmpty()) {
         return null;
       }
@@ -422,16 +431,25 @@ public class DistributedQueue {
     }
   }
 
  @VisibleForTesting boolean hasWatcher() throws InterruptedException {
  @VisibleForTesting int watcherCount() throws InterruptedException {
     updateLock.lockInterruptibly();
     try {
      return lastWatcher != null;
      return watcherCount;
     } finally {
       updateLock.unlock();
     }
   }
 
  private class ChildWatcher implements Watcher {
  @VisibleForTesting boolean isDirty() throws InterruptedException {
    updateLock.lockInterruptibly();
    try {
      return isDirty;
    } finally {
      updateLock.unlock();
    }
  }

  @VisibleForTesting class ChildWatcher implements Watcher {
 
     @Override
     public void process(WatchedEvent event) {
@@ -441,10 +459,8 @@ public class DistributedQueue {
       }
       updateLock.lock();
       try {
        // this watcher is automatically cleared when fired
        if (lastWatcher == this) {
          lastWatcher = null;
        }
        isDirty = true;
        watcherCount--;
         // optimistically signal any waiters that the queue may not be empty now, so they can wake up and retry
         changed.signalAll();
       } finally {
diff --git a/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java b/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
index b6754c71bf6..d2d6a16f335 100644
-- a/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
@@ -113,13 +113,15 @@ public class DistributedQueueTest extends SolrTestCaseJ4 {
 
     // After draining the queue, a watcher should be set.
     assertNull(dq.peek(100));
    assertTrue(dq.hasWatcher());
    assertFalse(dq.isDirty());
    assertEquals(1, dq.watcherCount());
 
     forceSessionExpire();
 
     // Session expiry should have fired the watcher.
     Thread.sleep(100);
    assertFalse(dq.hasWatcher());
    assertTrue(dq.isDirty());
    assertEquals(0, dq.watcherCount());
 
     // Rerun the earlier test make sure updates are still seen, post reconnection.
     future = executor.submit(() -> new String(dq.peek(true), UTF8));
@@ -137,6 +139,50 @@ public class DistributedQueueTest extends SolrTestCaseJ4 {
     assertNull(dq.poll());
   }
 
  @Test
  public void testLeakChildWatcher() throws Exception {
    String dqZNode = "/distqueue/test";
    DistributedQueue dq = makeDistributedQueue(dqZNode);
    assertTrue(dq.peekElements(1, 1, s1 -> true).isEmpty());
    assertEquals(1, dq.watcherCount());
    assertFalse(dq.isDirty());
    assertTrue(dq.peekElements(1, 1, s1 -> true).isEmpty());
    assertEquals(1, dq.watcherCount());
    assertFalse(dq.isDirty());
    assertNull(dq.peek());
    assertEquals(1, dq.watcherCount());
    assertFalse(dq.isDirty());
    assertNull(dq.peek(10));
    assertEquals(1, dq.watcherCount());
    assertFalse(dq.isDirty());

    dq.offer("hello world".getBytes(UTF8));
    assertNotNull(dq.peek()); // synchronously available
    // dirty and watcher state indeterminate here, race with watcher
    Thread.sleep(100); // watcher should have fired now
    assertNotNull(dq.peek());
    assertEquals(1, dq.watcherCount());
    assertFalse(dq.isDirty());
    assertFalse(dq.peekElements(1, 1, s -> true).isEmpty());
    assertEquals(1, dq.watcherCount());
    assertFalse(dq.isDirty());
  }

  @Test
  public void testLocallyOffer() throws Exception {
    String dqZNode = "/distqueue/test";
    DistributedQueue dq = makeDistributedQueue(dqZNode);
    dq.peekElements(1, 1, s -> true);
    for (int i = 0; i < 100; i++) {
      byte[] data = String.valueOf(i).getBytes(UTF8);
      dq.offer(data);
      assertNotNull(dq.peek());
      dq.poll();
      dq.peekElements(1, 1, s -> true);
    }
  }


   @Test
   public void testPeekElements() throws Exception {
     String dqZNode = "/distqueue/test";
- 
2.19.1.windows.1

