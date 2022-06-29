From 995080c44c2bf296eb58764cab6f536ce5a808a1 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Sat, 8 Nov 2014 12:20:19 -0500
Subject: [PATCH] ACCUMULO-3304 Track assignment execution duration and warn
 when they take longer than some duration.

Introduce a new configuration value, defaults to 10min, which controls
the duration an assignment must be running for a warning to be
printed. The period of checking threads is always half of the value.
--
 .../apache/accumulo/core/conf/Property.java   |  2 +
 server/tserver/pom.xml                        |  9 +--
 .../tserver/ActiveAssignmentRunnable.java     | 78 +++++++++++++++++++
 .../accumulo/tserver/RunnableStartedAt.java   | 51 ++++++++++++
 .../apache/accumulo/tserver/TabletServer.java | 13 ++--
 .../tserver/TabletServerResourceManager.java  | 75 ++++++++++++++++--
 6 files changed, 210 insertions(+), 18 deletions(-)
 create mode 100644 server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java
 create mode 100644 server/tserver/src/main/java/org/apache/accumulo/tserver/RunnableStartedAt.java

diff --git a/core/src/main/java/org/apache/accumulo/core/conf/Property.java b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
index a03b210c9..aec7af5f7 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/Property.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
@@ -278,6 +278,8 @@ public enum Property {
       "resiliency in the face of unexpected power outages, at the cost of speed. If method is not available, the legacy 'sync' method " +
       "will be used to ensure backwards compatibility with older Hadoop versions. A value of 'hflush' is the alternative to the default value " +
       "of 'hsync' which will result in faster writes, but with less durability"),
  TSERV_ASSIGNMENT_DURATION_WARNING("tserver.assignment.duration.warning", "10m", PropertyType.TIMEDURATION, "The amount of time an assignment can run "
      + " before the server will print a warning along with the current stack trace. Meant to help debug stuck assignments"),
 
   // properties that are specific to logger server behavior
   LOGGER_PREFIX("logger.", null, PropertyType.PREFIX, "Properties in this category affect the behavior of the write-ahead logger servers"),
diff --git a/server/tserver/pom.xml b/server/tserver/pom.xml
index 3ea50ad6a..1e0850430 100644
-- a/server/tserver/pom.xml
++ b/server/tserver/pom.xml
@@ -87,6 +87,10 @@
       <groupId>org.apache.zookeeper</groupId>
       <artifactId>zookeeper</artifactId>
     </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
    </dependency>
     <dependency>
       <groupId>junit</groupId>
       <artifactId>junit</artifactId>
@@ -97,11 +101,6 @@
       <artifactId>easymock</artifactId>
       <scope>test</scope>
     </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <scope>test</scope>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-log4j12</artifactId>
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java
new file mode 100644
index 000000000..dcbdae756
-- /dev/null
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java
@@ -0,0 +1,78 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.tserver;

import java.util.concurrent.ConcurrentHashMap;

import jline.internal.Preconditions;

import org.apache.accumulo.core.data.KeyExtent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ActiveAssignmentRunnable implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(ActiveAssignmentRunnable.class);

  private final ConcurrentHashMap<KeyExtent,RunnableStartedAt> activeAssignments;
  private final KeyExtent extent;
  private final Runnable delegate;

  // Make sure that the other thread calling getException will see the assignment by the thread calling run()
  private volatile Thread executingThread;

  public ActiveAssignmentRunnable(ConcurrentHashMap<KeyExtent,RunnableStartedAt> activeAssignments, KeyExtent extent, Runnable delegate) {
    Preconditions.checkNotNull(activeAssignments);
    Preconditions.checkNotNull(extent);
    Preconditions.checkNotNull(delegate);
    this.activeAssignments = activeAssignments;
    this.extent = extent;
    this.delegate = delegate;
  }

  @Override
  public void run() {
    if (activeAssignments.containsKey(extent)) {
      throw new IllegalStateException("Active assignment already exists for " + extent);
    }

    executingThread = Thread.currentThread();

    try {
      RunnableStartedAt runnableWithStartTime = new RunnableStartedAt(this, System.currentTimeMillis());
      log.trace("Started assignment for {} at {}", extent, runnableWithStartTime.getStartTime());
      activeAssignments.put(extent, runnableWithStartTime);
      delegate.run();
    } finally {
      if (log.isTraceEnabled()) {
        // Avoid the call to currentTimeMillis if we'd just throw it away anyways
        log.trace("Finished assignment for {} at {}", extent, System.currentTimeMillis());
      }
      activeAssignments.remove(extent);
    }
  }

  public Exception getException() {
    final Exception e = new Exception("Assignment of " + extent);
    if (null != executingThread) {
      e.setStackTrace(executingThread.getStackTrace());
    }
    return e;
  }
}
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/RunnableStartedAt.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/RunnableStartedAt.java
new file mode 100644
index 000000000..6513091fc
-- /dev/null
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/RunnableStartedAt.java
@@ -0,0 +1,51 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.tserver;

import java.util.AbstractMap;
import java.util.Map.Entry;

/**
 * Encapsulation of a task and the time it began execution.
 */
public class RunnableStartedAt extends AbstractMap.SimpleEntry<ActiveAssignmentRunnable,Long> {

  private static final long serialVersionUID = 1L;

  public RunnableStartedAt(ActiveAssignmentRunnable task, Long startedAtMillis) {
    super(task, startedAtMillis);
  }

  public RunnableStartedAt(Entry<? extends ActiveAssignmentRunnable,? extends Long> entry) {
    super(entry);
  }

  /**
   * @return The task being executed
   */
  public ActiveAssignmentRunnable getTask() {
    return getKey();
  }

  /**
   * @return The time, in millis, that the runnable was submitted at
   */
  public Long getStartTime() {
    return getValue();
  }

}
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index 94be0bb8d..8ef44da55 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -2282,7 +2282,8 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
       // add the assignment job to the appropriate queue
       log.info("Loading tablet " + extent);
 
      final Runnable ah = new LoggingRunnable(log, new AssignmentHandler(extent));
      final AssignmentHandler ah = new AssignmentHandler(extent);
      // final Runnable ah = new LoggingRunnable(log, );
       // Root tablet assignment must take place immediately
       if (extent.isRootTablet()) {
         new Daemon("Root Tablet Assignment") {
@@ -2299,9 +2300,9 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
         }.start();
       } else {
         if (extent.isMeta()) {
          resourceManager.addMetaDataAssignment(ah);
          resourceManager.addMetaDataAssignment(extent, log, ah);
         } else {
          resourceManager.addAssignment(ah);
          resourceManager.addAssignment(extent, log, ah);
         }
       }
     }
@@ -2824,7 +2825,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     }
   }
 
  private class AssignmentHandler implements Runnable {
  protected class AssignmentHandler implements Runnable {
     private KeyExtent extent;
     private int retryAttempt = 0;
 
@@ -2979,10 +2980,10 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
               if (extent.isRootTablet()) {
                 new Daemon(new LoggingRunnable(log, handler), "Root tablet assignment retry").start();
               } else {
                resourceManager.addMetaDataAssignment(handler);
                resourceManager.addMetaDataAssignment(extent, log, handler);
               }
             } else {
              resourceManager.addAssignment(handler);
              resourceManager.addAssignment(extent, log, handler);
             }
           }
         }, reschedule);
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java
index 935ffebd2..7c0eedc44 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java
@@ -26,6 +26,7 @@ import java.util.Map.Entry;
 import java.util.SortedMap;
 import java.util.TreeMap;
 import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.LinkedBlockingQueue;
@@ -54,6 +55,7 @@ import org.apache.accumulo.server.util.time.SimpleTimer;
 import org.apache.accumulo.trace.instrument.TraceExecutorService;
 import org.apache.accumulo.tserver.FileManager.ScanFileManager;
 import org.apache.accumulo.tserver.Tablet.MinorCompactionReason;
import org.apache.accumulo.tserver.TabletServer.AssignmentHandler;
 import org.apache.accumulo.tserver.compaction.CompactionStrategy;
 import org.apache.accumulo.tserver.compaction.DefaultCompactionStrategy;
 import org.apache.accumulo.tserver.compaction.MajorCompactionReason;
@@ -62,9 +64,9 @@ import org.apache.log4j.Logger;
 
 /**
  * ResourceManager is responsible for managing the resources of all tablets within a tablet server.
 * 
 * 
 * 
 *
 *
 *
  */
 public class TabletServerResourceManager {
 
@@ -82,6 +84,8 @@ public class TabletServerResourceManager {
   private ExecutorService defaultReadAheadThreadPool;
   private Map<String,ExecutorService> threadPools = new TreeMap<String,ExecutorService>();
 
  private final ConcurrentHashMap<KeyExtent,RunnableStartedAt> activeAssignments;

   private HashSet<TabletResourceManager> tabletResources;
 
   private final VolumeManager fs;
@@ -196,6 +200,8 @@ public class TabletServerResourceManager {
 
     assignMetaDataPool = createEs(0, 1, 60, "metadata tablet assignment");
 
    activeAssignments = new ConcurrentHashMap<KeyExtent,RunnableStartedAt>();

     readAheadThreadPool = createEs(Property.TSERV_READ_AHEAD_MAXCONCURRENT, "tablet read ahead");
     defaultReadAheadThreadPool = createEs(Property.TSERV_METADATA_READ_AHEAD_MAXCONCURRENT, "metadata tablets read ahead");
 
@@ -209,6 +215,61 @@ public class TabletServerResourceManager {
     memoryManager.init(conf);
     memMgmt = new MemoryManagementFramework();
     memMgmt.startThreads();

    SimpleTimer timer = SimpleTimer.getInstance();

    // We can use the same map for both metadata and normal assignments since the keyspace (extent)
    // is guaranteed to be unique. Schedule the task once, the task will reschedule itself.
    timer.schedule(new AssignmentWatcher(acuConf, activeAssignments, timer), 5000);
  }

  /**
   * Accepts some map which is tracking active assignment task(s) (running) and monitors them to ensure that the time the assignment(s) have been running don't
   * exceed a threshold. If the time is exceeded a warning is printed and a stack trace is logged for the running assignment.
   */
  protected static class AssignmentWatcher implements Runnable {
    private static final Logger log = Logger.getLogger(AssignmentWatcher.class);

    private final Map<KeyExtent,RunnableStartedAt> activeAssignments;
    private final AccumuloConfiguration conf;
    private final SimpleTimer timer;

    public AssignmentWatcher(AccumuloConfiguration conf, Map<KeyExtent,RunnableStartedAt> activeAssignments, SimpleTimer timer) {
      this.conf = conf;
      this.activeAssignments = activeAssignments;
      this.timer = timer;
    }

    @Override
    public void run() {
      final long millisBeforeWarning = conf.getTimeInMillis(Property.TSERV_ASSIGNMENT_DURATION_WARNING);
      try {
        long now = System.currentTimeMillis();
        KeyExtent extent;
        RunnableStartedAt runnable;
        for (Entry<KeyExtent,RunnableStartedAt> entry : activeAssignments.entrySet()) {
          extent = entry.getKey();
          runnable = entry.getValue();
          final long duration = now - runnable.getStartTime();

          // Print a warning if an assignment has been running for over the configured time length
          if (duration > millisBeforeWarning) {
            log.warn("Assignment for " + extent + " has been running for at least " + duration + "ms", runnable.getTask().getException());
          } else if (log.isTraceEnabled()) {
            log.trace("Assignment for " + extent + " only running for " + duration + "ms");
          }
        }
      } catch (Exception e) {
        log.warn("Caught exception checking active assignments", e);
      } finally {
        // Don't run more often than every 5s
        long delay = Math.max((long) (millisBeforeWarning * 0.5), 5000l);
        if (log.isTraceEnabled()) {
          log.trace("Rescheduling assignment watcher to run in " + delay + "ms");
        }
        timer.schedule(this, delay);
      }
    }
   }
 
   private static class TabletStateImpl implements TabletState, Cloneable {
@@ -647,12 +708,12 @@ public class TabletServerResourceManager {
     }
   }
 
  public void addAssignment(Runnable assignmentHandler) {
    assignmentPool.execute(assignmentHandler);
  public void addAssignment(KeyExtent extent, Logger log, AssignmentHandler assignmentHandler) {
    assignmentPool.execute(new ActiveAssignmentRunnable(activeAssignments, extent, new LoggingRunnable(log, assignmentHandler)));
   }
 
  public void addMetaDataAssignment(Runnable assignmentHandler) {
    assignMetaDataPool.execute(assignmentHandler);
  public void addMetaDataAssignment(KeyExtent extent, Logger log, AssignmentHandler assignmentHandler) {
    assignMetaDataPool.execute(new ActiveAssignmentRunnable(activeAssignments, extent, new LoggingRunnable(log, assignmentHandler)));
   }
 
   public void addMigration(KeyExtent tablet, Runnable migrationHandler) {
- 
2.19.1.windows.1

