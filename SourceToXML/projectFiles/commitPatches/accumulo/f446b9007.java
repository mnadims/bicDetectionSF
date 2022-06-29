From f446b9007ce8a4f0820e89c9e8e41a866ee8d548 Mon Sep 17 00:00:00 2001
From: Dave Marion <dlmarion@apache.org>
Date: Wed, 2 Mar 2016 15:08:40 -0500
Subject: [PATCH] ACCUMULO-1755: Removed synchronization of binning mutations
 in TabletServerBatchWriter

The TabletServerBatchWriter will attempt to bin mutations in a background thread. If that
thread is busy then the binning will occur in the client thread. Previously, if binning were
to occur in one client thread, it would block all client threads from adding mutations.
--
 .../client/impl/TabletServerBatchWriter.java  | 135 ++++++++++++------
 .../accumulo/core/util/SimpleThreadPool.java  |   6 +
 .../test/functional/BatchWriterFlushIT.java   |  88 +++++++++++-
 3 files changed, 185 insertions(+), 44 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
index 404b4947f..491bcc15c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
@@ -32,7 +32,10 @@ import java.util.Set;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
 import org.apache.accumulo.core.client.AccumuloException;
@@ -137,13 +140,13 @@ public class TabletServerBatchWriter {
   private long initialCompileTimes;
   private double initialSystemLoad;
 
  private int tabletServersBatchSum = 0;
  private int tabletBatchSum = 0;
  private int numBatches = 0;
  private int maxTabletBatch = Integer.MIN_VALUE;
  private int minTabletBatch = Integer.MAX_VALUE;
  private int minTabletServersBatch = Integer.MAX_VALUE;
  private int maxTabletServersBatch = Integer.MIN_VALUE;
  private AtomicInteger tabletServersBatchSum = new AtomicInteger(0);
  private AtomicInteger tabletBatchSum = new AtomicInteger(0);
  private AtomicInteger numBatches = new AtomicInteger(0);
  private AtomicInteger maxTabletBatch = new AtomicInteger(Integer.MIN_VALUE);
  private AtomicInteger minTabletBatch = new AtomicInteger(Integer.MAX_VALUE);
  private AtomicInteger minTabletServersBatch = new AtomicInteger(Integer.MAX_VALUE);
  private AtomicInteger maxTabletServersBatch = new AtomicInteger(Integer.MIN_VALUE);
 
   private Throwable lastUnknownError = null;
 
@@ -230,7 +233,12 @@ public class TabletServerBatchWriter {
     if (mutations.getMemoryUsed() == 0)
       return;
     lastProcessingStartTime = System.currentTimeMillis();
    writer.addMutations(mutations);
    try {
      writer.queueMutations(mutations);
    } catch (InterruptedException e) {
      log.warn("Mutations rejected from binning thread, retrying...");
      failedMutations.add(mutations);
    }
     mutations = new MutationSet();
   }
 
@@ -354,6 +362,7 @@ public class TabletServerBatchWriter {
       checkForFailures();
     } finally {
       // make a best effort to release these resources
      writer.binningThreadPool.shutdownNow();
       writer.sendThreadPool.shutdownNow();
       jtimer.cancel();
       span.stop();
@@ -361,26 +370,26 @@ public class TabletServerBatchWriter {
   }
 
   private void logStats() {
    long finishTime = System.currentTimeMillis();
    if (log.isTraceEnabled()) {
      long finishTime = System.currentTimeMillis();
 
    long finalGCTimes = 0;
    List<GarbageCollectorMXBean> gcmBeans = ManagementFactory.getGarbageCollectorMXBeans();
    for (GarbageCollectorMXBean garbageCollectorMXBean : gcmBeans) {
      finalGCTimes += garbageCollectorMXBean.getCollectionTime();
    }
      long finalGCTimes = 0;
      List<GarbageCollectorMXBean> gcmBeans = ManagementFactory.getGarbageCollectorMXBeans();
      for (GarbageCollectorMXBean garbageCollectorMXBean : gcmBeans) {
        finalGCTimes += garbageCollectorMXBean.getCollectionTime();
      }
 
    CompilationMXBean compMxBean = ManagementFactory.getCompilationMXBean();
    long finalCompileTimes = 0;
    if (compMxBean.isCompilationTimeMonitoringSupported()) {
      finalCompileTimes = compMxBean.getTotalCompilationTime();
    }
      CompilationMXBean compMxBean = ManagementFactory.getCompilationMXBean();
      long finalCompileTimes = 0;
      if (compMxBean.isCompilationTimeMonitoringSupported()) {
        finalCompileTimes = compMxBean.getTotalCompilationTime();
      }
 
    double averageRate = totalSent.get() / (totalSendTime.get() / 1000.0);
    double overallRate = totalAdded / ((finishTime - startTime) / 1000.0);
      double averageRate = totalSent.get() / (totalSendTime.get() / 1000.0);
      double overallRate = totalAdded / ((finishTime - startTime) / 1000.0);
 
    double finalSystemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
      double finalSystemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
 
    if (log.isTraceEnabled()) {
       log.trace("");
       log.trace("TABLET SERVER BATCH WRITER STATISTICS");
       log.trace(String.format("Added                : %,10d mutations", totalAdded));
@@ -397,9 +406,10 @@ public class TabletServerBatchWriter {
       log.trace(String.format("Total bin time       : %,10.2f secs %6.2f%s", totalBinTime.get() / 1000.0,
           100.0 * totalBinTime.get() / (finishTime - startTime), "%"));
       log.trace(String.format("Average bin rate     : %,10.2f mutations/sec", totalBinned.get() / (totalBinTime.get() / 1000.0)));
      log.trace(String.format("tservers per batch   : %,8.2f avg  %,6d min %,6d max", tabletServersBatchSum / (double) numBatches, minTabletServersBatch,
          maxTabletServersBatch));
      log.trace(String.format("tablets per batch    : %,8.2f avg  %,6d min %,6d max", tabletBatchSum / (double) numBatches, minTabletBatch, maxTabletBatch));
      log.trace(String.format("tservers per batch   : %,8.2f avg  %,6d min %,6d max", (float) (tabletServersBatchSum.get() / numBatches.get()),
          minTabletServersBatch.get(), maxTabletServersBatch.get()));
      log.trace(String.format("tablets per batch    : %,8.2f avg  %,6d min %,6d max", (float) (tabletBatchSum.get() / numBatches.get()), minTabletBatch.get(),
          maxTabletBatch.get()));
       log.trace("");
       log.trace("SYSTEM STATISTICS");
       log.trace(String.format("JVM GC Time          : %,10.2f secs", ((finalGCTimes - initialGCTimes) / 1000.0)));
@@ -416,16 +426,32 @@ public class TabletServerBatchWriter {
   }
 
   public void updateBinningStats(int count, long time, Map<String,TabletServerMutations<Mutation>> binnedMutations) {
    totalBinTime.addAndGet(time);
    totalBinned.addAndGet(count);
    updateBatchStats(binnedMutations);
    if (log.isTraceEnabled()) {
      totalBinTime.addAndGet(time);
      totalBinned.addAndGet(count);
      updateBatchStats(binnedMutations);
    }
   }
 
  private synchronized void updateBatchStats(Map<String,TabletServerMutations<Mutation>> binnedMutations) {
    tabletServersBatchSum += binnedMutations.size();
  private static void computeMin(AtomicInteger stat, int update) {
    int old = stat.get();
    while (!stat.compareAndSet(old, Math.min(old, update))) {
      old = stat.get();
    }
  }

  private static void computeMax(AtomicInteger stat, int update) {
    int old = stat.get();
    while (!stat.compareAndSet(old, Math.max(old, update))) {
      old = stat.get();
    }
  }
 
    minTabletServersBatch = Math.min(minTabletServersBatch, binnedMutations.size());
    maxTabletServersBatch = Math.max(maxTabletServersBatch, binnedMutations.size());
  private void updateBatchStats(Map<String,TabletServerMutations<Mutation>> binnedMutations) {
    tabletServersBatchSum.addAndGet(binnedMutations.size());

    computeMin(minTabletServersBatch, binnedMutations.size());
    computeMax(maxTabletServersBatch, binnedMutations.size());
 
     int numTablets = 0;
 
@@ -434,12 +460,12 @@ public class TabletServerBatchWriter {
       numTablets += tsm.getMutations().size();
     }
 
    tabletBatchSum += numTablets;
    tabletBatchSum.addAndGet(numTablets);
 
    minTabletBatch = Math.min(minTabletBatch, numTablets);
    maxTabletBatch = Math.max(maxTabletBatch, numTablets);
    computeMin(minTabletBatch, numTablets);
    computeMax(maxTabletBatch, numTablets);
 
    numBatches++;
    numBatches.incrementAndGet();
   }
 
   private void waitRTE() {
@@ -616,19 +642,22 @@ public class TabletServerBatchWriter {
   private class MutationWriter {
 
     private static final int MUTATION_BATCH_SIZE = 1 << 17;
    private ExecutorService sendThreadPool;
    private Map<String,TabletServerMutations<Mutation>> serversMutations;
    private Set<String> queued;
    private Map<String,TabletLocator> locators;
    private final ExecutorService sendThreadPool;
    private final SimpleThreadPool binningThreadPool;
    private final Map<String,TabletServerMutations<Mutation>> serversMutations;
    private final Set<String> queued;
    private final Map<String,TabletLocator> locators;
 
     public MutationWriter(int numSendThreads) {
       serversMutations = new HashMap<String,TabletServerMutations<Mutation>>();
       queued = new HashSet<String>();
       sendThreadPool = new SimpleThreadPool(numSendThreads, this.getClass().getName());
       locators = new HashMap<String,TabletLocator>();
      binningThreadPool = new SimpleThreadPool(1, "BinMutations", new SynchronousQueue<Runnable>());
      binningThreadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
     }
 
    private TabletLocator getLocator(String tableId) {
    private synchronized TabletLocator getLocator(String tableId) {
       TabletLocator ret = locators.get(tableId);
       if (ret == null) {
         ret = TabletLocator.getLocator(instance, new Text(tableId));
@@ -686,7 +715,27 @@ public class TabletServerBatchWriter {
 
     }
 
    void addMutations(MutationSet mutationsToSend) {
    void queueMutations(final MutationSet mutationsToSend) throws InterruptedException {
      if (null == mutationsToSend)
        return;
      binningThreadPool.execute(new Runnable() {

        @Override
        public void run() {
          if (null != mutationsToSend) {
            try {
              if (log.isTraceEnabled())
                log.trace(Thread.currentThread().getName() + " - binning " + mutationsToSend.size() + " mutations");
              addMutations(mutationsToSend);
            } catch (Exception e) {
              updateUnknownErrors("Error processing mutation set", e);
            }
          }
        }
      });
    }

    private void addMutations(MutationSet mutationsToSend) {
       Map<String,TabletServerMutations<Mutation>> binnedMutations = new HashMap<String,TabletServerMutations<Mutation>>();
       Span span = Trace.start("binMutations");
       try {
diff --git a/core/src/main/java/org/apache/accumulo/core/util/SimpleThreadPool.java b/core/src/main/java/org/apache/accumulo/core/util/SimpleThreadPool.java
index a406233c0..899199126 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/SimpleThreadPool.java
++ b/core/src/main/java/org/apache/accumulo/core/util/SimpleThreadPool.java
@@ -16,6 +16,7 @@
  */
 package org.apache.accumulo.core.util;
 
import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.LinkedBlockingQueue;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
@@ -30,4 +31,9 @@ public class SimpleThreadPool extends ThreadPoolExecutor {
     allowCoreThreadTimeOut(true);
   }
 
  public SimpleThreadPool(int max, final String name, BlockingQueue<Runnable> queue) {
    super(max, max, 4l, TimeUnit.SECONDS, queue, new NamingThreadFactory(name));
    allowCoreThreadTimeOut(true);
  }

 }
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/BatchWriterFlushIT.java b/test/src/test/java/org/apache/accumulo/test/functional/BatchWriterFlushIT.java
index 52d9c9370..e2277a37c 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/BatchWriterFlushIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/BatchWriterFlushIT.java
@@ -18,9 +18,16 @@ package org.apache.accumulo.test.functional;
 
 import static com.google.common.base.Charsets.UTF_8;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
 import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
 import java.util.Map.Entry;
 import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.AccumuloException;
@@ -36,14 +43,17 @@ import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.SimpleThreadPool;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.accumulo.harness.AccumuloClusterIT;
 import org.apache.hadoop.io.Text;
import org.junit.Assert;
 import org.junit.Test;
 
 public class BatchWriterFlushIT extends AccumuloClusterIT {
 
   private static final int NUM_TO_FLUSH = 100000;
  private static final int NUM_THREADS = 3;
 
   @Override
   protected int defaultTimeoutSeconds() {
@@ -60,7 +70,6 @@ public class BatchWriterFlushIT extends AccumuloClusterIT {
     c.tableOperations().create(bwlt);
     runFlushTest(bwft);
     runLatencyTest(bwlt);

   }
 
   private void runLatencyTest(String tableName) throws Exception {
@@ -170,6 +179,83 @@ public class BatchWriterFlushIT extends AccumuloClusterIT {
     }
   }
 
  @Test
  public void runMultiThreadedBinningTest() throws Exception {
    Connector c = getConnector();
    String[] tableNames = getUniqueNames(1);
    String tableName = tableNames[0];
    c.tableOperations().create(tableName);
    for (int x = 0; x < NUM_THREADS; x++) {
      c.tableOperations().addSplits(tableName, new TreeSet<Text>(Collections.singleton(new Text(Integer.toString(x * NUM_TO_FLUSH)))));
    }

    // Logger.getLogger(TabletServerBatchWriter.class).setLevel(Level.TRACE);
    final List<Set<Mutation>> allMuts = new LinkedList<Set<Mutation>>();
    List<Mutation> data = new ArrayList<Mutation>();
    for (int i = 0; i < NUM_THREADS; i++) {
      final int thread = i;
      for (int j = 0; j < NUM_TO_FLUSH; j++) {
        int row = thread * NUM_TO_FLUSH + j;
        Mutation m = new Mutation(new Text(String.format("%10d", row)));
        m.put(new Text("cf" + thread), new Text("cq"), new Value(("" + row).getBytes()));
        data.add(m);
      }
    }
    Assert.assertEquals(NUM_THREADS * NUM_TO_FLUSH, data.size());
    Collections.shuffle(data);
    for (int n = 0; n < (NUM_THREADS * NUM_TO_FLUSH); n += NUM_TO_FLUSH) {
      Set<Mutation> muts = new HashSet<Mutation>(data.subList(n, n + NUM_TO_FLUSH));
      allMuts.add(muts);
    }

    SimpleThreadPool threads = new SimpleThreadPool(NUM_THREADS, "ClientThreads");
    threads.allowCoreThreadTimeOut(false);
    threads.prestartAllCoreThreads();

    BatchWriterConfig cfg = new BatchWriterConfig();
    cfg.setMaxLatency(10, TimeUnit.SECONDS);
    cfg.setMaxMemory(1 * 1024 * 1024);
    cfg.setMaxWriteThreads(NUM_THREADS);
    final BatchWriter bw = getConnector().createBatchWriter(tableName, cfg);

    for (int k = 0; k < NUM_THREADS; k++) {
      final int idx = k;
      threads.execute(new Runnable() {
        @Override
        public void run() {
          try {
            bw.addMutations(allMuts.get(idx));
            bw.flush();
          } catch (MutationsRejectedException e) {
            Assert.fail("Error adding mutations to batch writer");
          }
        }
      });
    }
    threads.shutdown();
    threads.awaitTermination(3, TimeUnit.MINUTES);
    bw.close();
    Scanner scanner = getConnector().createScanner(tableName, Authorizations.EMPTY);
    for (Entry<Key,Value> e : scanner) {
      Mutation m = new Mutation(e.getKey().getRow());
      m.put(e.getKey().getColumnFamily(), e.getKey().getColumnQualifier(), e.getValue());
      boolean found = false;
      for (int l = 0; l < NUM_THREADS; l++) {
        if (allMuts.get(l).contains(m)) {
          found = true;
          allMuts.get(l).remove(m);
          break;
        }
      }
      Assert.assertTrue("Mutation not found: " + m.toString(), found);
    }

    for (int m = 0; m < NUM_THREADS; m++) {
      Assert.assertEquals(0, allMuts.get(m).size());
    }

  }

   private void verifyEntry(int row, Entry<Key,Value> entry) throws Exception {
     if (!entry.getKey().getRow().toString().equals(String.format("r_%10d", row))) {
       throw new Exception("Unexpected key returned, expected " + row + " got " + entry.getKey());
- 
2.19.1.windows.1

