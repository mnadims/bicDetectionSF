From 4911f855d9f8eea94e5b0ae848cc37fcef355252 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Fri, 10 Jul 2015 18:41:12 -0400
Subject: [PATCH] ACCUMULO-3937 Allow configuration in killing a tserver due to
 HDFS failures

--
 .../java/org/apache/accumulo/core/conf/Property.java |  5 +++++
 .../org/apache/accumulo/tserver/TabletServer.java    |  8 +++++---
 .../accumulo/tserver/log/TabletServerLogger.java     | 12 +++++++-----
 3 files changed, 17 insertions(+), 8 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/conf/Property.java b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
index b0ade7a20..3e2b2e7b0 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/Property.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
@@ -245,6 +245,11 @@ public enum Property {
           + "must be made, which is slower. However opening too many files at once can cause problems."),
   TSERV_WALOG_MAX_SIZE("tserver.walog.max.size", "1G", PropertyType.MEMORY,
       "The maximum size for each write-ahead log. See comment for property tserver.memory.maps.max"),
  TSERV_WALOG_TOLERATED_CREATION_FAILURES("tserver.walog.tolerated.creation.failures", "15", PropertyType.COUNT,
      "The maximum number of failures tolerated when creating a new WAL file within the period specified by tserver.walog.failures.period."
          + " Exceeding this number of failures in the period causes the TabletServer to exit."),
  TSERV_WALOG_TOLERATED_CREATION_FAILURES_PERIOD("tserver.walog.tolerated.creation.failures.period", "10s", PropertyType.TIMEDURATION,
      "The period in which the number of failures to create a WAL file in HDFS causes the TabletServer to exit."),
   TSERV_MAJC_DELAY("tserver.compaction.major.delay", "30s", PropertyType.TIMEDURATION,
       "Time a tablet server will sleep between checking which tablets need compaction."),
   TSERV_MAJC_THREAD_MAXOPEN("tserver.compaction.major.thread.files.open.max", "10", PropertyType.COUNT,
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index 8b6ad303f..4be001a2a 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -343,12 +343,14 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
       }
     }, 5000, 5000);
 
    long walogMaxSize = getConfiguration().getMemoryInBytes(Property.TSERV_WALOG_MAX_SIZE);
    long minBlockSize = CachedConfiguration.getInstance().getLong("dfs.namenode.fs-limits.min-block-size", 0);
    final long walogMaxSize = getConfiguration().getMemoryInBytes(Property.TSERV_WALOG_MAX_SIZE);
    final long minBlockSize = CachedConfiguration.getInstance().getLong("dfs.namenode.fs-limits.min-block-size", 0);
     if (minBlockSize != 0 && minBlockSize > walogMaxSize)
       throw new RuntimeException("Unable to start TabletServer. Logger is set to use blocksize " + walogMaxSize + " but hdfs minimum block size is "
           + minBlockSize + ". Either increase the " + Property.TSERV_WALOG_MAX_SIZE + " or decrease dfs.namenode.fs-limits.min-block-size in hdfs-site.xml.");
    logger = new TabletServerLogger(this, walogMaxSize, syncCounter, flushCounter);
    final long toleratedWalCreationFailures = getConfiguration().getCount(Property.TSERV_WALOG_TOLERATED_CREATION_FAILURES);
    final long toleratedWalCreationFailuresPeriod = getConfiguration().getTimeInMillis(Property.TSERV_WALOG_TOLERATED_CREATION_FAILURES_PERIOD);
    logger = new TabletServerLogger(this, walogMaxSize, syncCounter, flushCounter, toleratedWalCreationFailures, toleratedWalCreationFailuresPeriod);
     this.resourceManager = new TabletServerResourceManager(this, fs);
     this.security = AuditedSecurityOperation.getInstance(this);
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
index 1d385d919..a1921c2c2 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
@@ -92,9 +92,8 @@ public class TabletServerLogger {
   private final AtomicLong syncCounter;
   private final AtomicLong flushCounter;
 
  private final static int HALT_AFTER_ERROR_COUNT = 5;
  // Die if we get 5 WAL creation errors in 10 seconds
  private final Cache<Long,Object> walErrors = CacheBuilder.newBuilder().maximumSize(HALT_AFTER_ERROR_COUNT).expireAfterWrite(10, TimeUnit.SECONDS).build();
  private final long toleratedFailures;
  private final Cache<Long,Object> walErrors;
 
   static private abstract class TestCallWithWriteLock {
     abstract boolean test();
@@ -139,11 +138,14 @@ public class TabletServerLogger {
     }
   }
 
  public TabletServerLogger(TabletServer tserver, long maxSize, AtomicLong syncCounter, AtomicLong flushCounter) {
  public TabletServerLogger(TabletServer tserver, long maxSize, AtomicLong syncCounter, AtomicLong flushCounter, long toleratedWalCreationFailures,
      long toleratedFailuresPeriodMillis) {
     this.tserver = tserver;
     this.maxSize = maxSize;
     this.syncCounter = syncCounter;
     this.flushCounter = flushCounter;
    this.toleratedFailures = toleratedWalCreationFailures;
    this.walErrors = CacheBuilder.newBuilder().maximumSize(toleratedFailures).expireAfterWrite(toleratedFailuresPeriodMillis, TimeUnit.MILLISECONDS).build();
   }
 
   private int initializeLoggers(final List<DfsLogger> copy) throws IOException {
@@ -204,7 +206,7 @@ public class TabletServerLogger {
       return;
     } catch (Exception t) {
       walErrors.put(System.currentTimeMillis(), "");
      if (walErrors.size() >= HALT_AFTER_ERROR_COUNT) {
      if (walErrors.size() > toleratedFailures) {
         Halt.halt("Experienced too many errors creating WALs, giving up");
       }
       throw new RuntimeException(t);
- 
2.19.1.windows.1

