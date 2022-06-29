From 72156b82ecc05aa6dc072ab8d5dce5f328b140c8 Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <eric.newton@gmail.com>
Date: Mon, 29 Sep 2014 09:36:11 -0400
Subject: [PATCH] ACCUMULO-2480 make the tserver give up and die if openning
 the WAL experiences 5 errors in 10 seconds

--
 .../impl/MiniAccumuloClusterImpl.java         |  4 +
 .../accumulo/tserver/log/DfsLogger.java       | 15 ++--
 .../tserver/log/TabletServerLogger.java       | 17 +++++
 .../accumulo/test/TabletServerGivesUpIT.java  | 73 +++++++++++++++++++
 4 files changed, 101 insertions(+), 8 deletions(-)
 create mode 100644 test/src/main/java/org/apache/accumulo/test/TabletServerGivesUpIT.java

diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java
index c9031eb72..1fb590101 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java
@@ -778,4 +778,8 @@ public class MiniAccumuloClusterImpl implements AccumuloCluster {
     }
     return stats;
   }

  public MiniDFSCluster getMiniDfs() {
    return this.miniDFS;
  }
 }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java
index 6260ec7a4..8de2b2587 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java
@@ -16,12 +16,6 @@
  */
 package org.apache.accumulo.tserver.log;
 
import static org.apache.accumulo.tserver.logger.LogEvents.COMPACTION_FINISH;
import static org.apache.accumulo.tserver.logger.LogEvents.COMPACTION_START;
import static org.apache.accumulo.tserver.logger.LogEvents.DEFINE_TABLET;
import static org.apache.accumulo.tserver.logger.LogEvents.MANY_MUTATIONS;
import static org.apache.accumulo.tserver.logger.LogEvents.OPEN;

 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
@@ -41,6 +35,7 @@ import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.LinkedBlockingQueue;
 import java.util.concurrent.atomic.AtomicLong;
 
import com.google.common.base.Joiner;
 import org.apache.accumulo.core.client.Durability;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
@@ -65,7 +60,11 @@ import org.apache.hadoop.fs.FSDataOutputStream;
 import org.apache.hadoop.fs.Path;
 import org.apache.log4j.Logger;
 
import com.google.common.base.Joiner;
import static org.apache.accumulo.tserver.logger.LogEvents.COMPACTION_FINISH;
import static org.apache.accumulo.tserver.logger.LogEvents.COMPACTION_START;
import static org.apache.accumulo.tserver.logger.LogEvents.DEFINE_TABLET;
import static org.apache.accumulo.tserver.logger.LogEvents.MANY_MUTATIONS;
import static org.apache.accumulo.tserver.logger.LogEvents.OPEN;
 
 /**
  * Wrap a connection to a logger.
@@ -130,7 +129,7 @@ public class DfsLogger {
   private static final LogFileValue EMPTY = new LogFileValue();
 
   private boolean closed = false;

  
   private class LogSyncingTask implements Runnable {
 
     @Override
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
index 243b4051f..86ae596c0 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/TabletServerLogger.java
@@ -26,11 +26,16 @@ import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.concurrent.locks.ReadWriteLock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 
import org.apache.accumulo.server.util.Halt;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
 import org.apache.accumulo.core.client.Durability;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
@@ -86,6 +91,14 @@ public class TabletServerLogger {
 
   private final AtomicLong syncCounter;
   private final AtomicLong flushCounter;
  
  private final static int HALT_AFTER_ERROR_COUNT = 5;
  private final Cache<Long, Object> walErrors;
  {
    // Die if we get 5 WAL creation errors in 10 seconds
    walErrors = CacheBuilder.newBuilder().maximumSize(HALT_AFTER_ERROR_COUNT).expireAfterWrite(10, TimeUnit.SECONDS).build();
  }

 
   static private abstract class TestCallWithWriteLock {
     abstract boolean test();
@@ -194,6 +207,10 @@ public class TabletServerLogger {
       logSetId.incrementAndGet();
       return;
     } catch (Exception t) {
      walErrors.put(System.currentTimeMillis(), "");
      if (walErrors.size() >= HALT_AFTER_ERROR_COUNT) {
        Halt.halt("Experienced too many errors creating WALs, giving up");
      }
       throw new RuntimeException(t);
     }
   }
diff --git a/test/src/main/java/org/apache/accumulo/test/TabletServerGivesUpIT.java b/test/src/main/java/org/apache/accumulo/test/TabletServerGivesUpIT.java
new file mode 100644
index 000000000..e2e5ac983
-- /dev/null
++ b/test/src/main/java/org/apache/accumulo/test/TabletServerGivesUpIT.java
@@ -0,0 +1,73 @@
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
package org.apache.accumulo.test;

import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
import org.apache.accumulo.test.functional.ConfigurableMacIT;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// ACCUMULO-2480
public class TabletServerGivesUpIT extends ConfigurableMacIT {
  
  @Override
  public void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.useMiniDFS(true);
    cfg.setNumTservers(1);
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "5s");
  }

  @Test(timeout = 30 * 1000)
  public void test() throws Exception {
    final Connector conn = this.getConnector();
    // Yes, there's a tabletserver
    assertEquals(1, conn.instanceOperations().getTabletServers().size());
    final String tableName = getUniqueNames(1)[0];
    conn.tableOperations().create(tableName);
    // Kill dfs
    cluster.getMiniDfs().shutdown();
    // ask the tserver to do something
    final AtomicReference<Exception> ex = new AtomicReference<>(); 
    Thread splitter = new Thread() {
      public void run() {
        try {
          TreeSet<Text> splits = new TreeSet<>();
          splits.add(new Text("X"));
          conn.tableOperations().addSplits(tableName, splits);
        } catch (Exception e) {
          ex.set(e);
        }
      }
    };
    splitter.start();
    // wait for the tserver to give up on writing to the WAL
    while (conn.instanceOperations().getTabletServers().size() == 1) {
      UtilWaitThread.sleep(1000);
    }
  }
  
}
- 
2.19.1.windows.1

