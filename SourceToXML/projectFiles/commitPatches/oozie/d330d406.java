From d330d40665a3b42744db20dfc5d9a80ad5f9b439 Mon Sep 17 00:00:00 2001
From: Purshotam Shah <purushah@yahoo-inc.com>
Date: Tue, 27 Sep 2016 12:21:26 -0700
Subject: [PATCH] OOZIE-2501 ZK reentrant lock doesn't work for few cases

--
 .../oozie/command/wf/ActionStartXCommand.java |  10 ++
 .../org/apache/oozie/lock/MemoryLocks.java    |  82 +++++--------
 .../oozie/service/MemoryLocksService.java     |   9 +-
 .../apache/oozie/service/ZKLocksService.java  |  85 +++++--------
 .../apache/oozie/lock/TestMemoryLocks.java    |  60 +++++++--
 .../oozie/service/TestZKLocksService.java     | 115 +++++++++++++-----
 release-log.txt                               |   1 +
 7 files changed, 218 insertions(+), 144 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
index 41f4430f6..edfac4844 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
@@ -21,6 +21,7 @@ package org.apache.oozie.command.wf;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;

 import javax.servlet.jsp.el.ELException;
 
 import org.apache.hadoop.conf.Configuration;
@@ -41,6 +42,7 @@ import org.apache.oozie.client.SLAEvent.Status;
 import org.apache.oozie.client.rest.JsonBean;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.command.XCommand;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
@@ -399,4 +401,12 @@ public class ActionStartXCommand extends ActionXCommand<org.apache.oozie.command
         queue(new ActionStartXCommand(wfAction.getId(), wfAction.getType()), retryDelayMillis);
     }
 
    protected void queue(XCommand<?> command, long msDelay) {
        // ActionStartXCommand is synchronously called from SignalXCommand passing wfJob so that it doesn't have to
        //reload wfJob again. We need set wfJob to null, so that it get reloaded when the requeued command executes.
        if (command instanceof ActionStartXCommand) {
            ((ActionStartXCommand)command).wfJob = null;
        }
        super.queue(command, msDelay);
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java b/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java
index 7d65ac0e2..1ef1e413f 100644
-- a/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java
++ b/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java
@@ -18,33 +18,32 @@
 
 package org.apache.oozie.lock;
 
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 import java.util.concurrent.locks.Lock;
import org.apache.oozie.service.MemoryLocksService.Type;

import com.google.common.collect.MapMaker;
 
 /**
  * In memory resource locking that provides READ/WRITE lock capabilities.
  */
 public class MemoryLocks {
    final private HashMap<String, ReentrantReadWriteLock> locks = new HashMap<String, ReentrantReadWriteLock>();
 
    private static enum Type {
        READ, WRITE
    }
    final private ConcurrentMap<String, ReentrantReadWriteLock> locks = new MapMaker().weakValues().makeMap();
 
     /**
      * Implementation of {@link LockToken} for in memory locks.
      */
     class MemoryLockToken implements LockToken {
        private final ReentrantReadWriteLock rwLock;
        private final java.util.concurrent.locks.Lock lock;
        private final String resource;
        private final ReentrantReadWriteLock lockEntry;
        private final Type type;

        public MemoryLockToken(ReentrantReadWriteLock lockEntry, Type type) {
            this.lockEntry = lockEntry;
            this.type = type;
 
        private MemoryLockToken(ReentrantReadWriteLock rwLock, java.util.concurrent.locks.Lock lock, String resource) {
            this.rwLock = rwLock;
            this.lock = lock;
            this.resource = resource;
         }
 
         /**
@@ -52,18 +51,15 @@ public class MemoryLocks {
          */
         @Override
         public void release() {
            lock.unlock();
            if (!isLockHeld()) {
                synchronized (locks) {
                    if (!isLockHeld()) {
                        locks.remove(resource);
                    }
                }
            switch (type) {
                case WRITE:
                    lockEntry.writeLock().unlock();
                    break;
                case READ:
                    lockEntry.readLock().unlock();
                    break;
             }
         }
        private boolean isLockHeld(){
            return rwLock.hasQueuedThreads() || rwLock.isWriteLocked() || rwLock.getReadLockCount() > 0;
        }
     }
 
     /**
@@ -76,41 +72,23 @@ public class MemoryLocks {
     }
 
     /**
     * Obtain a READ lock for a source.
     * Obtain a lock for a source.
      *
      * @param resource resource name.
     * @param type lock type.
      * @param wait time out in milliseconds to wait for the lock, -1 means no timeout and 0 no wait.
      * @return the lock token for the resource, or <code>null</code> if the lock could not be obtained.
      * @throws InterruptedException thrown if the thread was interrupted while waiting.
      */
    public MemoryLockToken getReadLock(String resource, long wait) throws InterruptedException {
        return getLock(resource, Type.READ, wait);
    }

    /**
     * Obtain a WRITE lock for a source.
     *
     * @param resource resource name.
     * @param wait time out in milliseconds to wait for the lock, -1 means no timeout and 0 no wait.
     * @return the lock token for the resource, or <code>null</code> if the lock could not be obtained.
     * @throws InterruptedException thrown if the thread was interrupted while waiting.
     */
    public MemoryLockToken getWriteLock(String resource, long wait) throws InterruptedException {
        return getLock(resource, Type.WRITE, wait);
    }

    private MemoryLockToken getLock(String resource, Type type, long wait) throws InterruptedException {
        ReentrantReadWriteLock lockEntry;
        synchronized (locks) {
            if (locks.containsKey(resource)) {
                lockEntry = locks.get(resource);
            }
            else {
                lockEntry = new ReentrantReadWriteLock(true);
                locks.put(resource, lockEntry);
    public MemoryLockToken getLock(final String resource, Type type, long wait) throws InterruptedException {
        ReentrantReadWriteLock lockEntry = locks.get(resource);
        if (lockEntry == null) {
            ReentrantReadWriteLock newLock = new ReentrantReadWriteLock(true);
            lockEntry = locks.putIfAbsent(resource, newLock);
            if (lockEntry == null) {
                lockEntry = newLock;
             }
         }

         Lock lock = (type.equals(Type.READ)) ? lockEntry.readLock() : lockEntry.writeLock();
 
         if (wait == -1) {
@@ -133,6 +111,10 @@ public class MemoryLocks {
                 locks.put(resource, lockEntry);
             }
         }
        return new MemoryLockToken(lockEntry, lock, resource);
        return new MemoryLockToken(lockEntry, type);
    }

    public ConcurrentMap<String, ReentrantReadWriteLock> getLockMap(){
        return locks;
     }
 }
diff --git a/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java b/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java
index d7c6a89fd..2ab2abc34 100644
-- a/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java
++ b/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java
@@ -29,6 +29,11 @@ import com.google.common.annotations.VisibleForTesting;
  * Service that provides in-memory locks.  Assumes no other Oozie servers are using the database.
  */
 public class MemoryLocksService implements Service, Instrumentable {

    public static enum Type {
        READ, WRITE
    }

     protected static final String INSTRUMENTATION_GROUP = "locks";
     private MemoryLocks locks;
 
@@ -83,7 +88,7 @@ public class MemoryLocksService implements Service, Instrumentable {
      * @throws InterruptedException thrown if the thread was interrupted while waiting.
      */
     public LockToken getReadLock(String resource, long wait) throws InterruptedException {
        return locks.getReadLock(resource, wait);
        return locks.getLock(resource, Type.READ, wait);
     }
 
     /**
@@ -95,7 +100,7 @@ public class MemoryLocksService implements Service, Instrumentable {
      * @throws InterruptedException thrown if the thread was interrupted while waiting.
      */
     public LockToken getWriteLock(String resource, long wait) throws InterruptedException {
        return locks.getWriteLock(resource, wait);
        return locks.getLock(resource, Type.WRITE, wait);
     }
 
     @VisibleForTesting
diff --git a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
index 952b90d5d..8acbad9bb 100644
-- a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
++ b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
@@ -17,7 +17,7 @@
  */
 package org.apache.oozie.service;
 
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.curator.framework.recipes.locks.InterProcessMutex;
@@ -39,6 +39,7 @@ import org.apache.curator.framework.state.ConnectionState;
 import org.apache.curator.utils.ThreadUtils;
 
 import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MapMaker;
 
 /**
  * Service that provides distributed locks via ZooKeeper.  Requires that a ZooKeeper ensemble is available.  The locks will be
@@ -51,7 +52,8 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
     private static XLog LOG = XLog.getLog(ZKLocksService.class);
     public static final String LOCKS_NODE = "/locks";
 
    final private HashMap<String, InterProcessReadWriteLock> zkLocks = new HashMap<String, InterProcessReadWriteLock>();
    private ConcurrentMap<String, InterProcessReadWriteLock> zkLocks = new MapMaker().weakValues().makeMap();

 
     private static final String REAPING_LEADER_PATH = ZKUtils.ZK_BASE_SERVICES_PATH + "/locksChildReaperLeaderPath";
     public static final String REAPING_THRESHOLD = CONF_PREFIX + "ZKLocksService.locks.reaper.threshold";
@@ -123,18 +125,7 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
      */
     @Override
     public LockToken getReadLock(String resource, long wait) throws InterruptedException {
        InterProcessReadWriteLock lockEntry;
        synchronized (zkLocks) {
            if (zkLocks.containsKey(resource)) {
                lockEntry = zkLocks.get(resource);
            }
            else {
                lockEntry = new InterProcessReadWriteLock(zk.getClient(), LOCKS_NODE + "/" + resource);
                zkLocks.put(resource, lockEntry);
            }
        }
        InterProcessMutex readLock = lockEntry.readLock();
        return acquireLock(wait, readLock, resource);
        return acquireLock(resource, Type.READ, wait);
     }
 
     /**
@@ -147,29 +138,27 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
      */
     @Override
     public LockToken getWriteLock(String resource, long wait) throws InterruptedException {
        InterProcessReadWriteLock lockEntry;
        synchronized (zkLocks) {
            if (zkLocks.containsKey(resource)) {
                lockEntry = zkLocks.get(resource);
            }
            else {
                lockEntry = new InterProcessReadWriteLock(zk.getClient(), LOCKS_NODE + "/" + resource);
                zkLocks.put(resource, lockEntry);
            }
        }
        InterProcessMutex writeLock = lockEntry.writeLock();
        return acquireLock(wait, writeLock, resource);
        return acquireLock(resource, Type.WRITE, wait);
     }
 
    private LockToken acquireLock(long wait, InterProcessMutex lock, String resource) {
    private LockToken acquireLock(final String resource, Type type, long wait) throws InterruptedException {
        InterProcessReadWriteLock lockEntry = zkLocks.get(resource);
        if (lockEntry == null) {
            InterProcessReadWriteLock newLock = new InterProcessReadWriteLock(zk.getClient(), LOCKS_NODE + "/" + resource);
            lockEntry = zkLocks.putIfAbsent(resource, newLock);
            if (lockEntry == null) {
                lockEntry = newLock;
            }
        }
        InterProcessMutex lock = (type.equals(Type.READ)) ? lockEntry.readLock() : lockEntry.writeLock();
         ZKLockToken token = null;
         try {
             if (wait == -1) {
                 lock.acquire();
                token = new ZKLockToken(lock, resource);
                token = new ZKLockToken(lockEntry, type);
             }
             else if (lock.acquire(wait, TimeUnit.MILLISECONDS)) {
                token = new ZKLockToken(lock, resource);
                token = new ZKLockToken(lockEntry, type);
             }
         }
         catch (Exception ex) {
@@ -183,12 +172,12 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
      * Implementation of {@link LockToken} for zookeeper locks.
      */
     class ZKLockToken implements LockToken {
        private final InterProcessMutex lock;
        private final String resource;
        private final InterProcessReadWriteLock lockEntry;
        private final Type type;
 
        private ZKLockToken(InterProcessMutex lock, String resource) {
            this.lock = lock;
            this.resource = resource;
        private ZKLockToken(InterProcessReadWriteLock lockEntry, Type type) {
            this.lockEntry = lockEntry;
            this.type = type;
         }
 
         /**
@@ -197,35 +186,23 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
         @Override
         public void release() {
             try {
                lock.release();
                if (zkLocks.get(resource) == null) {
                    return;
                }
                if (!isLockHeld()) {
                    synchronized (zkLocks) {
                        if (zkLocks.get(resource) != null) {
                            if (!isLockHeld()) {
                                zkLocks.remove(resource);
                            }
                        }
                    }
                switch (type) {
                    case WRITE:
                        lockEntry.writeLock().release();
                        break;
                    case READ:
                        lockEntry.readLock().release();
                        break;
                 }
             }
             catch (Exception ex) {
                 LOG.warn("Could not release lock: " + ex.getMessage(), ex);
             }

         }

        private boolean isLockHeld() {
            return zkLocks.get(resource).readLock().isAcquiredInThisProcess()
                    || zkLocks.get(resource).writeLock().isAcquiredInThisProcess();
        }

     }
 
     @VisibleForTesting
    public HashMap<String, InterProcessReadWriteLock> getLocks(){
    public ConcurrentMap<String, InterProcessReadWriteLock> getLocks(){
         return zkLocks;
     }
 
diff --git a/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java b/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java
index f0a87e541..8c7b58eec 100644
-- a/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java
++ b/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java
@@ -23,6 +23,7 @@ import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.oozie.service.MemoryLocksService;
import org.apache.oozie.service.MemoryLocksService.Type;
 import org.apache.oozie.service.ServiceException;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.test.XTestCase;
@@ -31,6 +32,7 @@ import org.apache.oozie.util.XLog;
 public class TestMemoryLocks extends XTestCase {
     private static final int LATCH_TIMEOUT = 10;
     private XLog log = XLog.getLog(getClass());
    public static final int DEFAULT_LOCK_TIMEOUT = 5 * 1000;
 
     private MemoryLocks locks;
 
@@ -118,7 +120,7 @@ public class TestMemoryLocks extends XTestCase {
         }
 
         protected MemoryLocks.MemoryLockToken getLock() throws InterruptedException {
            return locks.getReadLock(name, timeout);
            return locks.getLock(name, Type.READ, timeout);
         }
     }
 
@@ -129,7 +131,7 @@ public class TestMemoryLocks extends XTestCase {
         }
 
         protected MemoryLocks.MemoryLockToken getLock() throws InterruptedException {
            return locks.getWriteLock(name, timeout);
            return locks.getLock(name, Type.WRITE, timeout);
         }
     }
 
@@ -323,7 +325,7 @@ public class TestMemoryLocks extends XTestCase {
         }
 
         protected MemoryLocks.MemoryLockToken getLock() throws InterruptedException {
            return locks.getWriteLock(name, timeout);
            return locks.getLock(name, Type.WRITE, timeout);
         }
     }
 
@@ -372,16 +374,16 @@ public class TestMemoryLocks extends XTestCase {
         MemoryLocksService lockService = new MemoryLocksService();
         try {
             lockService.init(Services.get());
            LockToken lock = lockService.getWriteLock(path, 5000);
            lock = (LockToken) lockService.getWriteLock(path, 5000);
            lock = (LockToken) lockService.getWriteLock(path, 5000);
            LockToken lock = lockService.getWriteLock(path, DEFAULT_LOCK_TIMEOUT);
            lock = (LockToken) lockService.getWriteLock(path, DEFAULT_LOCK_TIMEOUT);
            lock = (LockToken) lockService.getWriteLock(path, DEFAULT_LOCK_TIMEOUT);
             assertEquals(lockService.getMemoryLocks().size(), 1);
             lock.release();
             assertEquals(lockService.getMemoryLocks().size(), 1);
             lock.release();
             assertEquals(lockService.getMemoryLocks().size(), 1);
             lock.release();
            assertEquals(lockService.getMemoryLocks().size(), 0);
            checkLockRelease(path, lockService);
         }
         catch (Exception e) {
             fail("Reentrant property, it should have acquired lock");
@@ -391,4 +393,48 @@ public class TestMemoryLocks extends XTestCase {
         }
     }
 
    public void testLocksAreGarbageCollected() throws ServiceException, InterruptedException {
        String path = new String("a");
        String path1 = new String("a");
        MemoryLocksService lockService = new MemoryLocksService();
        lockService.init(Services.get());
        LockToken lock = lockService.getWriteLock(path, DEFAULT_LOCK_TIMEOUT);
        int oldHash = lockService.getMemoryLocks().getLockMap().get(path).hashCode();
        lock.release();
        lock = lockService.getWriteLock(path1, DEFAULT_LOCK_TIMEOUT);
        int newHash = lockService.getMemoryLocks().getLockMap().get(path1).hashCode();
        assertTrue(oldHash == newHash);
        lock.release();
        lock = null;
        System.gc();
        path = "a";
        lock = lockService.getWriteLock(path, DEFAULT_LOCK_TIMEOUT);
        newHash = lockService.getMemoryLocks().getLockMap().get(path).hashCode();
        assertFalse(oldHash == newHash);

    }

    public void testLocksAreReused() throws ServiceException, InterruptedException {
        String path = "a";
        MemoryLocksService lockService = new MemoryLocksService();
        lockService.init(Services.get());
        LockToken lock = lockService.getWriteLock(path, DEFAULT_LOCK_TIMEOUT);
        int oldHash = System.identityHashCode(lockService.getMemoryLocks().getLockMap().get(path));
        System.gc();
        lock.release();
        lock = lockService.getWriteLock(path, DEFAULT_LOCK_TIMEOUT);
        assertEquals(lockService.getMemoryLocks().size(), 1);
        int newHash = System.identityHashCode(lockService.getMemoryLocks().getLockMap().get(path));
        assertTrue(oldHash == newHash);
    }

    private void checkLockRelease(String path, MemoryLocksService lockService) {
        if (lockService.getMemoryLocks().getLockMap().get(path) == null) {
            // good lock is removed from memory after gc.
        }
        else {
            assertFalse(lockService.getMemoryLocks().getLockMap().get(path).isWriteLocked());
        }
    }

 }
diff --git a/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java b/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
index d1acadfff..d04f04e80 100644
-- a/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
++ b/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
@@ -21,6 +21,7 @@ package org.apache.oozie.service;
 import java.util.UUID;
 
 import org.apache.oozie.lock.LockToken;
import org.apache.oozie.lock.TestMemoryLocks;
 import org.apache.oozie.service.ZKLocksService.ZKLockToken;
 import org.apache.oozie.test.ZKXTestCase;
 import org.apache.oozie.util.XLog;
@@ -132,7 +133,7 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            _testWaitWriteLock(zkls, zkls);
            checkWaitWriteLock(zkls, zkls);
         }
         finally {
             zkls.destroy();
@@ -146,7 +147,7 @@ public class TestZKLocksService extends ZKXTestCase {
         try {
             zkls1.init(Services.get());
             zkls2.init(Services.get());
            _testWaitWriteLock(zkls1, zkls2);
            checkWaitWriteLock(zkls1, zkls2);
         }
         finally {
             zkls1.destroy();
@@ -154,7 +155,7 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void _testWaitWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
    public void checkWaitWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
         StringBuffer sb = new StringBuffer("");
         Locker l1 = new WriteLocker("a", 1, -1, sb, zkls1);
         Locker l2 = new WriteLocker("a", 2, -1, sb, zkls2);
@@ -174,7 +175,7 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            _testNoWaitWriteLock(zkls, zkls);
            checkNoWaitWriteLock(zkls, zkls);
         }
         finally {
             zkls.destroy();
@@ -188,7 +189,7 @@ public class TestZKLocksService extends ZKXTestCase {
         try {
             zkls1.init(Services.get());
             zkls2.init(Services.get());
            _testNoWaitWriteLock(zkls1, zkls2);
            checkNoWaitWriteLock(zkls1, zkls2);
         }
         finally {
             zkls1.destroy();
@@ -196,7 +197,7 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void _testNoWaitWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
    public void checkNoWaitWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
         StringBuffer sb = new StringBuffer("");
         Locker l1 = new WriteLocker("a", 1, 0, sb, zkls1);
         Locker l2 = new WriteLocker("a", 2, 0, sb, zkls2);
@@ -216,7 +217,7 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            _testTimeoutWaitingWriteLock(zkls, zkls);
            checkTimeoutWaitingWriteLock(zkls, zkls);
         }
         finally {
             zkls.destroy();
@@ -230,7 +231,7 @@ public class TestZKLocksService extends ZKXTestCase {
         try {
             zkls1.init(Services.get());
             zkls2.init(Services.get());
            _testTimeoutWaitingWriteLock(zkls1, zkls2);
            checkTimeoutWaitingWriteLock(zkls1, zkls2);
         }
         finally {
             zkls1.destroy();
@@ -238,7 +239,7 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void _testTimeoutWaitingWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
    public void checkTimeoutWaitingWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
         StringBuffer sb = new StringBuffer("");
         Locker l1 = new WriteLocker("a", 1, 0, sb, zkls1);
         Locker l2 = new WriteLocker("a", 2, (long) (WAITFOR_RATIO * 2000), sb, zkls2);
@@ -258,7 +259,7 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            _testTimeoutTimingOutWriteLock(zkls, zkls);
            checkTimeoutTimingOutWriteLock(zkls, zkls);
         }
         finally {
             zkls.destroy();
@@ -272,7 +273,7 @@ public class TestZKLocksService extends ZKXTestCase {
         try {
             zkls1.init(Services.get());
             zkls2.init(Services.get());
            _testTimeoutTimingOutWriteLock(zkls1, zkls2);
            checkTimeoutTimingOutWriteLock(zkls1, zkls2);
         }
         finally {
             zkls1.destroy();
@@ -280,7 +281,7 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void _testTimeoutTimingOutWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
    public void checkTimeoutTimingOutWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
         StringBuffer sb = new StringBuffer("");
         Locker l1 = new WriteLocker("a", 1, 0, sb, zkls1);
         Locker l2 = new WriteLocker("a", 2, 50, sb, zkls2);
@@ -300,7 +301,7 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            _testReadLock(zkls, zkls);
            checkReadLock(zkls, zkls);
         }
         finally {
             zkls.destroy();
@@ -314,7 +315,7 @@ public class TestZKLocksService extends ZKXTestCase {
         try {
             zkls1.init(Services.get());
             zkls2.init(Services.get());
            _testReadLock(zkls1, zkls2);
            checkReadLock(zkls1, zkls2);
         }
         finally {
             zkls1.destroy();
@@ -322,7 +323,7 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void _testReadLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
    public void checkReadLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
         StringBuffer sb = new StringBuffer("");
         Locker l1 = new ReadLocker("a", 1, -1, sb, zkls1);
         Locker l2 = new ReadLocker("a", 2, -1, sb, zkls2);
@@ -342,7 +343,7 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            _testReadWriteLock(zkls, zkls);
            checkReadWriteLock(zkls, zkls);
         }
         finally {
             zkls.destroy();
@@ -356,7 +357,7 @@ public class TestZKLocksService extends ZKXTestCase {
         try {
             zkls1.init(Services.get());
             zkls2.init(Services.get());
            _testReadWriteLock(zkls1, zkls2);
            checkReadWriteLock(zkls1, zkls2);
         }
         finally {
             zkls1.destroy();
@@ -364,7 +365,7 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void _testReadWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
    public void checkReadWriteLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
         StringBuffer sb = new StringBuffer("");
         Locker l1 = new ReadLocker("a", 1, -1, sb, zkls1);
         Locker l2 = new WriteLocker("a", 2, -1, sb, zkls2);
@@ -384,7 +385,7 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            _testWriteReadLock(zkls, zkls);
            checkWriteReadLock(zkls, zkls);
         }
         finally {
             zkls.destroy();
@@ -398,7 +399,7 @@ public class TestZKLocksService extends ZKXTestCase {
         try {
             zkls1.init(Services.get());
             zkls2.init(Services.get());
            _testWriteReadLock(zkls1, zkls2);
            checkWriteReadLock(zkls1, zkls2);
         }
         finally {
             zkls1.destroy();
@@ -406,7 +407,7 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void _testWriteReadLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
    public void checkWriteReadLock(ZKLocksService zkls1, ZKLocksService zkls2) throws Exception {
         StringBuffer sb = new StringBuffer("");
         Locker l1 = new WriteLocker("a", 1, -1, sb, zkls1);
         Locker l2 = new ReadLocker("a", 2, -1, sb, zkls2);
@@ -427,10 +428,10 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            ZKLockToken lock = (ZKLockToken) zkls.getWriteLock(path, 5000);
            ZKLockToken lock = (ZKLockToken) zkls.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
             assertTrue(zkls.getLocks().containsKey(path));
             lock.release();
            assertFalse(zkls.getLocks().containsKey(path));
            checkLockRelease(path, zkls);
         }
         finally {
             zkls.destroy();
@@ -442,16 +443,16 @@ public class TestZKLocksService extends ZKXTestCase {
         ZKLocksService zkls = new ZKLocksService();
         try {
             zkls.init(Services.get());
            ZKLockToken lock = (ZKLockToken) zkls.getWriteLock(path, 5000);
            lock = (ZKLockToken) zkls.getWriteLock(path, 5000);
            lock = (ZKLockToken) zkls.getWriteLock(path, 5000);
            ZKLockToken lock = (ZKLockToken) zkls.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
            lock = (ZKLockToken) zkls.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
            lock = (ZKLockToken) zkls.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
             assertTrue(zkls.getLocks().containsKey(path));
             lock.release();
             assertTrue(zkls.getLocks().containsKey(path));
             lock.release();
             assertTrue(zkls.getLocks().containsKey(path));
             lock.release();
            assertFalse(zkls.getLocks().containsKey(path));
            checkLockRelease(path, zkls);
         }
         catch (Exception e) {
             fail("Reentrant property, it should have acquired lock");
@@ -470,10 +471,10 @@ public class TestZKLocksService extends ZKXTestCase {
             ThreadLock t2 = new ThreadLock(zkls, path);
             t1.start();
             t1.join();
            assertFalse(zkls.getLocks().containsKey(path));
            checkLockRelease(path, zkls);
             t2.start();
             t2.join();
            assertFalse(zkls.getLocks().containsKey(path));
            checkLockRelease(path, zkls);
         }
         finally {
             zkls.destroy();
@@ -507,6 +508,58 @@ public class TestZKLocksService extends ZKXTestCase {
         }
     }
 
    public void testLocksAreGarbageCollected() throws ServiceException, InterruptedException {
        String path = new String("a");
        String path1 = new String("a");
        ZKLocksService lockService = new ZKLocksService();
        try {
            lockService.init(Services.get());
            LockToken lock = lockService.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
            lock.release();
            assertEquals(lockService.getLocks().size(), 1);
            int oldHash = lockService.getLocks().get(path).hashCode();
            lock = lockService.getWriteLock(path1, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
            int newHash = lockService.getLocks().get(path1).hashCode();
            assertTrue(oldHash == newHash);
            lock = null;
            System.gc();
            lock = lockService.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
            newHash = lockService.getLocks().get(path).hashCode();
            assertFalse(oldHash == newHash);
        }
        finally {
            lockService.destroy();
        }
    }

    public void testLocksAreReused() throws ServiceException, InterruptedException {
        String path = "a";
        ZKLocksService lockService = new ZKLocksService();
        try {
            lockService.init(Services.get());
            LockToken lock = lockService.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
            int oldHash = System.identityHashCode(lockService.getLocks().get(path));
            System.gc();
            lock.release();
            lock = lockService.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
            assertEquals(lockService.getLocks().size(), 1);
            int newHash = System.identityHashCode(lockService.getLocks().get(path));
            assertTrue(oldHash == newHash);
        }
        finally {
            lockService.destroy();
        }
    }

    private void checkLockRelease(String path, ZKLocksService zkls) {
        if (zkls.getLocks().get(path) == null) {
            // good, lock is removed from memory after gc.
        }
        else {
            assertFalse(zkls.getLocks().get(path).writeLock().isAcquiredInThisProcess());
        }
    }

     static class ThreadLock extends Thread {
         ZKLocksService zkls;
         String path;
@@ -520,9 +573,9 @@ public class TestZKLocksService extends ZKXTestCase {
 
         public void run() {
             try {
                lock = zkls.getWriteLock(path, 5000);
                lock = zkls.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
                 if (lock != null) {
                    lock = zkls.getWriteLock(path, 5000);
                    lock = zkls.getWriteLock(path, TestMemoryLocks.DEFAULT_LOCK_TIMEOUT);
                     Thread.sleep(1000);
                     lock.release();
                     Thread.sleep(1000);
diff --git a/release-log.txt b/release-log.txt
index 10a183a66..b03a61a14 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -3,6 +3,7 @@
 
 -- Oozie 4.3.0 release
 
OOZIE-2501 ZK reentrant lock doesn't work for few cases (puru)
 OOZIE-2582 Populating external child Ids for action failures (abhishekbafna via rohini)
 OOZIE-2678 Oozie job -kill doesn't work with tez jobs (abhishekbafna via rohini)
 OOZIE-2676 Make hadoop-2 as the default profile (gezapeti via rkanter)
- 
2.19.1.windows.1

