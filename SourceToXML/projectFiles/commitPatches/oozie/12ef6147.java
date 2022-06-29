From 12ef61470e12aa9885220de1e453dec1da05b28c Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Wed, 23 Jul 2014 23:27:01 -0700
Subject: [PATCH] OOZIE-1906 Service to periodically remove ZK lock (puru via
 rohini)

--
 .../apache/oozie/service/ZKLocksService.java  | 42 +++++++++++++++++--
 core/src/main/resources/oozie-default.xml     | 18 ++++++++
 .../oozie/service/TestZKLocksService.java     | 21 ++++++++++
 pom.xml                                       |  7 ++--
 release-log.txt                               |  1 +
 5 files changed, 83 insertions(+), 6 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
index d03a89999..3c642db2f 100644
-- a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
++ b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
@@ -17,10 +17,16 @@
  */
 package org.apache.oozie.service;
 
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.framework.recipes.locks.ChildReaper;
import org.apache.curator.framework.recipes.locks.Reaper;
 import org.apache.curator.framework.recipes.locks.InterProcessMutex;
 import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.utils.ThreadUtils;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.util.Instrumentable;
 import org.apache.oozie.util.Instrumentation;
@@ -28,18 +34,30 @@ import org.apache.oozie.lock.LockToken;
 import org.apache.oozie.util.XLog;
 import org.apache.oozie.util.ZKUtils;
 
import com.google.common.annotations.VisibleForTesting;

 /**
  * Service that provides distributed locks via ZooKeeper.  Requires that a ZooKeeper ensemble is available.  The locks will be
  * located under a ZNode named "locks" under the namespace (see {@link ZKUtils}).  For example, with default settings, if the
  * resource we're locking is called "foo", then the ZNode backing the lock will be at /oozie/locks/foo.
 * <p>
 * ChildReaper is used for deleting unused locks. Only one childreaper will be active in cluster.
 * ZK Path /oozie.zookeeper.namespace/services/locksChildReaperLeaderPath is used for leader selection.
  */

 public class ZKLocksService extends MemoryLocksService implements Service, Instrumentable {
 
     private ZKUtils zk;
     private static XLog LOG = XLog.getLog(ZKLocksService.class);
    private static final String LOCKS_NODE = "/locks/";
    public static final String LOCKS_NODE = "/locks";
     private final AtomicLong lockCount = new AtomicLong();
 
    private static final String REAPING_LEADER_PATH = ZKUtils.ZK_BASE_SERVICES_PATH + "/locksChildReaperLeaderPath";
    public static final int DEFAULT_REAPING_THRESHOLD = 300; // In sec
    public static final String REAPING_THRESHOLD = CONF_PREFIX + "ZKLocksService.locks.reaper.threshold";
    public static final String REAPING_THREADS = CONF_PREFIX + "ZKLocksService.locks.reaper.threads";
    private ChildReaper reaper = null;

     /**
      * Initialize the zookeeper locks service
      *
@@ -50,6 +68,9 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
         super.init(services);
         try {
             zk = ZKUtils.register(this);
            reaper = new ChildReaper(zk.getClient(), LOCKS_NODE, Reaper.Mode.REAP_INDEFINITELY, getExecutorService(),
                    services.getConf().getInt(REAPING_THRESHOLD, DEFAULT_REAPING_THRESHOLD) * 1000, REAPING_LEADER_PATH);
            reaper.start();
         }
         catch (Exception ex) {
             throw new ServiceException(ErrorCode.E1700, ex.getMessage(), ex);
@@ -62,6 +83,15 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
      */
     @Override
     public void destroy() {
        if (reaper != null) {
            try {
                reaper.close();
            }
            catch (IOException e) {
                LOG.error("Error closing childReaper", e);
            }
        }

         if (zk != null) {
             zk.unregister(this);
         }
@@ -95,7 +125,7 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
      */
     @Override
     public LockToken getReadLock(String resource, long wait) throws InterruptedException {
        InterProcessReadWriteLock lock = new InterProcessReadWriteLock(zk.getClient(), LOCKS_NODE + resource);
        InterProcessReadWriteLock lock = new InterProcessReadWriteLock(zk.getClient(), LOCKS_NODE + "/" + resource);
         InterProcessMutex readLock = lock.readLock();
         return acquireLock(wait, readLock);
     }
@@ -110,7 +140,7 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
      */
     @Override
     public LockToken getWriteLock(String resource, long wait) throws InterruptedException {
        InterProcessReadWriteLock lock = new InterProcessReadWriteLock(zk.getClient(), LOCKS_NODE + resource);
        InterProcessReadWriteLock lock = new InterProcessReadWriteLock(zk.getClient(), LOCKS_NODE + "/" + resource);
         InterProcessMutex writeLock = lock.writeLock();
         return acquireLock(wait, writeLock);
     }
@@ -157,4 +187,10 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
             }
         }
     }

    private static ScheduledExecutorService getExecutorService() {
        return ThreadUtils.newFixedThreadScheduledPool(Services.get().getConf().getInt(REAPING_THREADS, 2),
                "ZKLocksChildReaper");
    }

 }
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 4a58e9b2a..ebceaa7d3 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -2166,4 +2166,22 @@
         </description>
     </property>
 
    <property>
        <name>oozie.service.ZKLocksService.locks.reaper.threshold</name>
        <value>300</value>
        <description>
            The frequency at which the ChildReaper will run.
            Duration should be in sec. Default is 5 min.
        </description>
    </property>

    <property>
        <name>oozie.service.ZKLocksService.locks.reaper.threads</name>
        <value>2</value>
        <description>
            Number of fixed threads used by ChildReaper to
            delete empty locks.
        </description>
    </property>

 </configuration>
diff --git a/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java b/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
index a77346961..5ce8ecb00 100644
-- a/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
++ b/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
@@ -20,6 +20,7 @@ package org.apache.oozie.service;
 import org.apache.oozie.lock.LockToken;
 import org.apache.oozie.util.*;
 import org.apache.oozie.test.ZKXTestCase;
import org.apache.zookeeper.data.Stat;
 
 public class TestZKLocksService extends ZKXTestCase {
     private XLog log = XLog.getLog(getClass());
@@ -415,4 +416,24 @@ public class TestZKLocksService extends ZKXTestCase {
         sleep(1000);
         assertEquals("a:1-L a:1-U a:2-L a:2-U", sb.toString().trim());
     }

    public void testLockReaper() throws Exception {
        Services.get().getConf().set(ZKLocksService.REAPING_THRESHOLD, "1");
        ZKLocksService zkls = new ZKLocksService();
        try {
            zkls.init(Services.get());
            for (int i = 0; i < 10; ++i) {
                LockToken l = zkls.getReadLock(String.valueOf(i), 1);
                l.release();

            }
            sleep(2000);
            Stat stat = getClient().checkExists().forPath(ZKLocksService.LOCKS_NODE);
            assertEquals(stat.getNumChildren(), 0);
        }
        finally {
            zkls.destroy();
        }
    }

 }
diff --git a/pom.xml b/pom.xml
index 7fb57e556..190bad717 100644
-- a/pom.xml
++ b/pom.xml
@@ -98,6 +98,7 @@
 
          <openjpa.version>2.2.2</openjpa.version>
          <xerces.version>2.10.0</xerces.version>
         <curator.version>2.5.0</curator.version>
     </properties>
 
     <modules>
@@ -702,19 +703,19 @@
             <dependency>
                 <groupId>org.apache.curator</groupId>
                 <artifactId>curator-recipes</artifactId>
                <version>2.4.0</version>
                <version>${curator.version}</version>
             </dependency>
 
             <dependency>
                 <groupId>org.apache.curator</groupId>
                 <artifactId>curator-x-discovery</artifactId>
                <version>2.4.0</version>
                <version>${curator.version}</version>
             </dependency>
 
             <dependency>
                 <groupId>org.apache.curator</groupId>
                 <artifactId>curator-test</artifactId>
                <version>2.4.0</version>
                <version>${curator.version}</version>
             </dependency>
 
             <!-- examples -->
diff --git a/release-log.txt b/release-log.txt
index 8650116ea..996693360 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -4,6 +4,7 @@ OOZIE-1943 Bump up trunk to 4.2.0-SNAPSHOT (bzhang)
 
 -- Oozie 4.1.0 release (4.1 - unreleased)
 
OOZIE-1906 Service to periodically remove ZK lock (puru via rohini)
 OOZIE-1812 Bulk API with bundle Id should relax regex check for Id (puru via rohini)
 OOZIE-1915 Move system properties to conf properties (puru via rohini)
 OOZIE-1934 coordinator action repeatedly picked up by cachePurgeWorker of PartitionDependencyManagerService (ryota)
- 
2.19.1.windows.1

