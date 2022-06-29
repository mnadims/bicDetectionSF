From 70a5ffe4b029896df81aa49cd08bbaf9b0355a36 Mon Sep 17 00:00:00 2001
From: Purshotam Shah <purushah@yahoo-inc.com>
Date: Tue, 26 Jan 2016 10:10:01 -0800
Subject: [PATCH] OOZIE-1922 MemoryLocksService fails if lock is acquired
 multiple times in same thread and released

--
 .../org/apache/oozie/lock/MemoryLocks.java    |  12 +-
 .../oozie/service/MemoryLocksService.java     |   7 ++
 .../apache/oozie/service/ZKLocksService.java  |  21 +++-
 .../apache/oozie/lock/TestMemoryLocks.java    | 116 ++++++++++++++++++
 .../oozie/service/TestZKLocksService.java     |  75 ++++++-----
 release-log.txt                               |   1 +
 6 files changed, 182 insertions(+), 50 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java b/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java
index ee564b3de..7d65ac0e2 100644
-- a/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java
++ b/core/src/main/java/org/apache/oozie/lock/MemoryLocks.java
@@ -52,13 +52,17 @@ public class MemoryLocks {
          */
         @Override
         public void release() {
            int val = rwLock.getQueueLength();
            if (val == 0) {
            lock.unlock();
            if (!isLockHeld()) {
                 synchronized (locks) {
                    locks.remove(resource);
                    if (!isLockHeld()) {
                        locks.remove(resource);
                    }
                 }
             }
            lock.unlock();
        }
        private boolean isLockHeld(){
            return rwLock.hasQueuedThreads() || rwLock.isWriteLocked() || rwLock.getReadLockCount() > 0;
         }
     }
 
diff --git a/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java b/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java
index e3eccdb3f..d7c6a89fd 100644
-- a/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java
++ b/core/src/main/java/org/apache/oozie/service/MemoryLocksService.java
@@ -23,6 +23,8 @@ import org.apache.oozie.util.Instrumentation;
 import org.apache.oozie.lock.LockToken;
 import org.apache.oozie.lock.MemoryLocks;
 
import com.google.common.annotations.VisibleForTesting;

 /**
  * Service that provides in-memory locks.  Assumes no other Oozie servers are using the database.
  */
@@ -95,4 +97,9 @@ public class MemoryLocksService implements Service, Instrumentable {
     public LockToken getWriteLock(String resource, long wait) throws InterruptedException {
         return locks.getWriteLock(resource, wait);
     }

    @VisibleForTesting
    public MemoryLocks getMemoryLocks() {
        return locks;
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
index e3a6bcf04..35fc8a659 100644
-- a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
++ b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
@@ -197,13 +197,16 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
         public void release() {
             try {
                 lock.release();
                int val = lock.getParticipantNodes().size();
                //TODO this might break, when count is zero and before we remove lock, same thread may ask for same lock.
                // Hashmap will return the lock, but eventually release will remove it from hashmap and a immediate getlock will
                //create a new instance. Will fix this as part of OOZIE-1922
                if (val == 0) {
                if (zkLocks.get(resource) == null) {
                    return;
                }
                if (!isLockHeld()) {
                     synchronized (zkLocks) {
                        zkLocks.remove(resource);
                        if (zkLocks.get(resource) != null) {
                            if (!isLockHeld()) {
                                zkLocks.remove(resource);
                            }
                        }
                     }
                 }
             }
@@ -212,6 +215,12 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
             }
 
         }

        private boolean isLockHeld() {
            return zkLocks.get(resource).readLock().isAcquiredInThisProcess()
                    || zkLocks.get(resource).writeLock().isAcquiredInThisProcess();
        }

     }
 
     @VisibleForTesting
diff --git a/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java b/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java
index 0efe31033..61fec19b3 100644
-- a/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java
++ b/core/src/test/java/org/apache/oozie/lock/TestMemoryLocks.java
@@ -18,6 +18,10 @@
 
 package org.apache.oozie.lock;
 
import java.util.UUID;
import org.apache.oozie.service.MemoryLocksService;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
 import org.apache.oozie.test.XTestCase;
 import org.apache.oozie.util.XLog;
 
@@ -219,4 +223,116 @@ public class TestMemoryLocks extends XTestCase {
         assertEquals("a:1-L a:1-U a:2-L a:2-U", sb.toString().trim());
     }
 
    public class SameThreadWriteLocker implements Runnable {
        protected String name;
        private String nameIndex;
        private StringBuffer sb;
        protected long timeout;

        public SameThreadWriteLocker(String name, int nameIndex, long timeout, StringBuffer buffer) {
            this.name = name;
            this.nameIndex = name + ":" + nameIndex;
            this.sb = buffer;
            this.timeout = timeout;
        }

        public void run() {
            try {
                log.info("Getting lock [{0}]", nameIndex);
                MemoryLocks.MemoryLockToken token = getLock();
                MemoryLocks.MemoryLockToken token2 = getLock();

                if (token != null) {
                    log.info("Got lock [{0}]", nameIndex);
                    sb.append(nameIndex + "-L1 ");
                    if (token2 != null) {
                        sb.append(nameIndex + "-L2 ");
                    }
                    sb.append(nameIndex + "-U1 ");
                    token.release();
                    synchronized (this) {
                        wait();
                    }
                    sb.append(nameIndex + "-U2 ");
                    token2.release();
                    log.info("Release lock [{0}]", nameIndex);
                }
                else {
                    sb.append(nameIndex + "-N ");
                    log.info("Did not get lock [{0}]", nameIndex);
                }
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public void finish() {
            synchronized (this) {
                notify();
            }
        }

        protected MemoryLocks.MemoryLockToken getLock() throws InterruptedException {
            return locks.getWriteLock(name, timeout);
        }

    }

    public void testWriteLockSameThreadNoWait() throws Exception {
        StringBuffer sb = new StringBuffer("");
        SameThreadWriteLocker l1 = new SameThreadWriteLocker("a", 1, 0, sb);
        Locker l2 = new WriteLocker("a", 2, 0, sb);

        new Thread(l1).start();
        Thread.sleep(500);
        new Thread(l2).start();
        Thread.sleep(500);
        l1.finish();
        Thread.sleep(500);
        l2.finish();
        Thread.sleep(500);
        assertEquals("a:1-L1 a:1-L2 a:1-U1 a:2-N a:1-U2", sb.toString().trim());
    }

    public void testWriteLockSameThreadWait() throws Exception {
        StringBuffer sb = new StringBuffer("");
        SameThreadWriteLocker l1 = new SameThreadWriteLocker("a", 1, 0, sb);
        Locker l2 = new WriteLocker("a", 2, 1000, sb);

        new Thread(l1).start();
        Thread.sleep(500);
        new Thread(l2).start();
        Thread.sleep(500);
        l1.finish();
        Thread.sleep(500);
        l2.finish();
        Thread.sleep(500);
        assertEquals("a:1-L1 a:1-L2 a:1-U1 a:1-U2 a:2-L a:2-U", sb.toString().trim());
    }

    public void testLockReentrant() throws ServiceException, InterruptedException {
        final String path = UUID.randomUUID().toString();
        MemoryLocksService lockService = new MemoryLocksService();
        try {
            lockService.init(Services.get());
            LockToken lock = lockService.getWriteLock(path, 5000);
            lock = (LockToken) lockService.getWriteLock(path, 5000);
            lock = (LockToken) lockService.getWriteLock(path, 5000);
            assertEquals(lockService.getMemoryLocks().size(), 1);
            lock.release();
            assertEquals(lockService.getMemoryLocks().size(), 1);
            lock.release();
            assertEquals(lockService.getMemoryLocks().size(), 1);
            lock.release();
            assertEquals(lockService.getMemoryLocks().size(), 0);
        }
        catch (Exception e) {
            fail("Reentrant property, it should have acquired lock");
        }
        finally {
            lockService.destroy();
        }
    }

 }
diff --git a/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java b/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
index 02cc1372d..70aa4d7d5 100644
-- a/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
++ b/core/src/test/java/org/apache/oozie/service/TestZKLocksService.java
@@ -464,49 +464,16 @@ public class TestZKLocksService extends ZKXTestCase {
     public void testReentrantMultipleThread() throws ServiceException, InterruptedException {
         final String path = UUID.randomUUID().toString();
         final ZKLocksService zkls = new ZKLocksService();
        final LockToken[] locks = new LockToken[2];

        zkls.init(Services.get());
         try {
            zkls.init(Services.get());
            Thread t1 = new Thread() {
                public void run() {
                    try {
                        locks[0] = zkls.getWriteLock(path, 5000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread t2 = new Thread() {
                public void run() {
                    try {
                        locks[1] = zkls.getWriteLock(path, 5000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            ThreadLock t1 = new ThreadLock(zkls, path);
            ThreadLock t2 = new ThreadLock(zkls, path);
             t1.start();
            t2.start();
             t1.join();
            assertFalse(zkls.getLocks().containsKey(path));
            t2.start();
             t2.join();

            if (locks[0] != null) {
                assertNull(locks[1]);
            }
            if (locks[1] != null) {
                assertNull(locks[0]);
            }

            if (locks[0] != null) {
                locks[0].release();
            }
            if (locks[1] != null) {
                locks[1].release();
            }
            assertTrue(zkls.getLocks().containsKey(path));
            assertFalse(zkls.getLocks().containsKey(path));
         }
         finally {
             zkls.destroy();
@@ -514,8 +481,9 @@ public class TestZKLocksService extends ZKXTestCase {
     }
 
     public void testLockReaper() throws Exception {
        Services.get().getConf().set(ZKLocksService.REAPING_THRESHOLD, "1");
        ConfigurationService.set(ZKLocksService.REAPING_THRESHOLD, "1");
         ZKLocksService zkls = new ZKLocksService();

         try {
             zkls.init(Services.get());
             for (int i = 0; i < 10; ++i) {
@@ -531,4 +499,31 @@ public class TestZKLocksService extends ZKXTestCase {
             zkls.destroy();
         }
     }

    static class ThreadLock extends Thread {
        ZKLocksService zkls;
        String path;
        LockToken lock = null;

        public ThreadLock(ZKLocksService zkls, String path) {
            this.zkls = zkls;
            this.path = path;

        }

        public void run() {
            try {
                lock = zkls.getWriteLock(path, 5000);
                if (lock != null) {
                    lock = zkls.getWriteLock(path, 5000);
                    Thread.sleep(1000);
                    lock.release();
                    Thread.sleep(1000);
                    lock.release();
                }
            }
            catch (InterruptedException e) {
            }
        }
    }
 }
\ No newline at end of file
diff --git a/release-log.txt b/release-log.txt
index 1b675bb48..6dac28b83 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-1922 MemoryLocksService fails if lock is acquired multiple times in same thread and released (puru)
 OOZIE-2432 TestPurgeXCommand fails (fdenes via rkanter)
 OOZIE-2434 inconsistent coord action status and workflow job status (satishsaley via puru)
 OOZIE-2438 Oozie client "jobs -filter" diagnostic message clarification (satishsaley via puru)
- 
2.19.1.windows.1

