From 7e86ba8c7327f99ca8708494b6d402af4cd0b4ec Mon Sep 17 00:00:00 2001
From: Scott Blum <dragonsinth@apache.org>
Date: Thu, 9 Jun 2016 13:57:11 -0400
Subject: [PATCH] SOLR-9191: use a Predicate instead of a Function

--
 .../java/org/apache/solr/cloud/DistributedQueue.java   |  4 ++--
 .../org/apache/solr/cloud/DistributedQueueTest.java    | 10 +++++++---
 2 files changed, 9 insertions(+), 5 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java b/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
index afed6f13fef..7576ae54e3c 100644
-- a/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
++ b/solr/core/src/java/org/apache/solr/cloud/DistributedQueue.java
@@ -25,10 +25,10 @@ import java.util.TreeSet;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.cloud.SolrZkClient;
@@ -320,7 +320,7 @@ public class DistributedQueue {
    * <p/>
    * Package-private to support {@link OverseerTaskQueue} specifically.
    */
  Collection<Pair<String, byte[]>> peekElements(int max, long waitMillis, Function<String, Boolean> acceptFilter) throws KeeperException, InterruptedException {
  Collection<Pair<String, byte[]>> peekElements(int max, long waitMillis, Predicate<String> acceptFilter) throws KeeperException, InterruptedException {
     List<String> foundChildren = new ArrayList<>();
     long waitNanos = TimeUnit.MILLISECONDS.toNanos(waitMillis);
     while (true) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java b/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
index f42f1014cc8..66a7ed8deb7 100644
-- a/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/DistributedQueueTest.java
@@ -23,6 +23,7 @@ import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 
import com.google.common.base.Predicates;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.cloud.SolrZkClient;
 import org.apache.solr.common.util.ExecutorUtil;
@@ -31,6 +32,9 @@ import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
import static com.google.common.base.Predicates.alwaysFalse;
import static com.google.common.base.Predicates.alwaysTrue;

 public class DistributedQueueTest extends SolrTestCaseJ4 {
 
   private static final Charset UTF8 = Charset.forName("UTF-8");
@@ -150,15 +154,15 @@ public class DistributedQueueTest extends SolrTestCaseJ4 {
 
     // Should be able to get 0, 1, 2, or 3 instantly
     for (int i = 0; i <= 3; ++i) {
      assertEquals(i, dq.peekElements(i, 0, child -> true).size());
      assertEquals(i, dq.peekElements(i, 0, alwaysTrue()).size());
     }
 
     // Asking for more should return only 3.
    assertEquals(3, dq.peekElements(4, 0, child -> true).size());
    assertEquals(3, dq.peekElements(4, 0, alwaysTrue()).size());
 
     // If we filter everything out, we should block for the full time.
     long start = System.nanoTime();
    assertEquals(0, dq.peekElements(4, 1000, child -> false).size());
    assertEquals(0, dq.peekElements(4, 1000, alwaysFalse()).size());
     assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(500));
 
     // If someone adds a new matching element while we're waiting, we should return immediately.
- 
2.19.1.windows.1

