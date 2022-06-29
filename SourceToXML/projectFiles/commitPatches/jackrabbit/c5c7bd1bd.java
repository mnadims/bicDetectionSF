From c5c7bd1bde42c364b59df276e20d92300067b6e5 Mon Sep 17 00:00:00 2001
From: Dominique Pfister <dpfister@apache.org>
Date: Tue, 29 Nov 2011 16:41:20 +0000
Subject: [PATCH] JCR-3138 - Skip sync delay when changes are found - made
 behaviour subclass overridable - added test case

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1207957 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/cluster/ClusterNode.java  |  23 +-
 .../core/journal/AbstractJournal.java         |  70 +++---
 .../core/cluster/ClusterSyncTest.java         | 207 ++++++++++++++++++
 .../jackrabbit/core/cluster/TestAll.java      |   1 +
 4 files changed, 271 insertions(+), 30 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/ClusterSyncTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cluster/ClusterNode.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cluster/ClusterNode.java
index 3fe804214..ae351602f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cluster/ClusterNode.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cluster/ClusterNode.java
@@ -187,6 +187,11 @@ public class ClusterNode implements Runnable,
      * Record deserializer.
      */
     private ClusterRecordDeserializer deserializer = new ClusterRecordDeserializer();
    
    /**
     * Flag indicating whether sync is manual.
     */
    private boolean disableAutoSync;
 
     /**
      * Initialize this cluster node.
@@ -243,6 +248,13 @@ public class ClusterNode implements Runnable,
     public long getStopDelay() {
         return stopDelay;
     }
    
    /**
     * Disable periodic background synchronization. Used for testing purposes, only.
     */
    protected void disableAutoSync() {
        disableAutoSync = true;
    }
 
     /**
      * Starts this cluster node.
@@ -253,11 +265,12 @@ public class ClusterNode implements Runnable,
         if (status == NONE) {
             sync();
 
            Thread t = new Thread(this, "ClusterNode-" + clusterNodeId);
            t.setDaemon(true);
            t.start();
            syncThread = t;

            if (!disableAutoSync) {
                Thread t = new Thread(this, "ClusterNode-" + clusterNodeId);
                t.setDaemon(true);
                t.start();
                syncThread = t;
            }
             status = STARTED;
         }
     }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java
index 00d4a96bc..72d193c8b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/AbstractJournal.java
@@ -217,39 +217,59 @@ public abstract class AbstractJournal implements Journal {
      * @throws JournalException if an error occurs
      */
     protected void doSync(long startRevision) throws JournalException {
        RecordIterator iterator = getRecords(startRevision);
        long stopRevision = Long.MIN_VALUE;

        try {
            while (iterator.hasNext()) {
                Record record = iterator.nextRecord();
                if (record.getJournalId().equals(id)) {
                    log.info("Record with revision '" + record.getRevision()
                            + "' created by this journal, skipped.");
                } else {
                    RecordConsumer consumer = getConsumer(record.getProducerId());
                    if (consumer != null) {
                        try {
                            consumer.consume(record);
                        } catch (IllegalStateException e) {
                            log.error("Could not synchronize to revision: " + record.getRevision() + " due illegal state of RecordConsumer.");
                            return;
        for (;;) {
            RecordIterator iterator = getRecords(startRevision);
            long stopRevision = Long.MIN_VALUE;
    
            try {
                while (iterator.hasNext()) {
                    Record record = iterator.nextRecord();
                    if (record.getJournalId().equals(id)) {
                        log.info("Record with revision '" + record.getRevision()
                                + "' created by this journal, skipped.");
                    } else {
                        RecordConsumer consumer = getConsumer(record.getProducerId());
                        if (consumer != null) {
                            try {
                                consumer.consume(record);
                            } catch (IllegalStateException e) {
                                log.error("Could not synchronize to revision: " + record.getRevision() + " due illegal state of RecordConsumer.");
                                return;
                            }
                         }
                     }
                    stopRevision = record.getRevision();
                 }
                stopRevision = record.getRevision();
            } finally {
                iterator.close();
             }
        } finally {
            iterator.close();
        }
    
            if (stopRevision > 0) {
                for (RecordConsumer consumer : consumers.values()) {
                    consumer.setRevision(stopRevision);
                }
                log.info("Synchronized to revision: " + stopRevision);
 
        if (stopRevision > 0) {
            for (RecordConsumer consumer : consumers.values()) {
                consumer.setRevision(stopRevision);
                if (syncAgainOnNewRecords()) {
                    // changes detected, sync again
                    startRevision = stopRevision;
                    continue;
                }
             }
            log.info("Synchronized to revision: " + stopRevision);
            break;
         }
     }
    
    /**
     * Return a flag indicating whether synchronization should continue
     * in a loop until no more new records are found. Subclass overridable.
     * 
     * @return <code>true</code> if synchronization should continue;
     *         <code>false</code> otherwise
     */
    protected boolean syncAgainOnNewRecords() {
        return false;
    }
 
     /**
      * Lock the journal revision, disallowing changes from other sources until
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/ClusterSyncTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/ClusterSyncTest.java
new file mode 100644
index 000000000..83fbfa8ad
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/ClusterSyncTest.java
@@ -0,0 +1,207 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.cluster;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.SimpleEventListener.LockEvent;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalFactory;
import org.apache.jackrabbit.core.journal.MemoryJournal;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordConsumer;
import org.apache.jackrabbit.core.journal.MemoryJournal.MemoryRecord;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.test.JUnitTest;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * Test cases for cluster synchronization.
 */
public class ClusterSyncTest extends JUnitTest {

    /** Defaut workspace name. */
    private static final String DEFAULT_WORKSPACE = "default";

    /** Default sync delay: 5 seconds. */
    private static final long SYNC_DELAY = 5000;

    /** Master node. */
    private ClusterNode master;

    /** Slave node. */
    /* avoid synthetic accessor */ ClusterNode slave;

    /** Records shared among multiple memory journals. */
    private final ArrayList<MemoryRecord> records = new ArrayList<MemoryRecord>();
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        master = createClusterNode("master", false);
        master.start();

        slave = createClusterNode("slave", true);
        slave.start();

        super.setUp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void tearDown() throws Exception {
        if (slave != null) {
            slave.stop();
        }
        if (master != null) {
            master.stop();
        }
        super.tearDown();
    }

    /**
     * Verify that sync() on a cluster node will continue fetching results until no more
     * changes are detected. 
     * 
     * @throws Exception
     */
    public void testSyncAllChanges() throws Exception {
        // create channel on master and slave
        LockEventChannel channel = master.createLockChannel(DEFAULT_WORKSPACE);
        slave.createLockChannel(DEFAULT_WORKSPACE).setListener(new SimpleEventListener());
        
        // add blocking consumer to slave, this will block on the first non-empty sync()
        BlockingConsumer consumer = new BlockingConsumer();
        slave.getJournal().register(consumer);
        
        // add first entry
        LockEvent event = new LockEvent(NodeId.randomId(), true, "admin");
        channel.create(event.getNodeId(), event.isDeep(), event.getUserId()).ended(true);
        
        // start a manual sync on the slave and ...
        Thread syncOnce = new Thread(new Runnable() {
            public void run() {
                try {
                    slave.sync();
                } catch (ClusterException e) {
                    /* ignore */
                }
            }
        });
        syncOnce.start();

        // ... wait until it blocks
        consumer.waitUntilBlocked();
        
        // add second entry
        event = new LockEvent(NodeId.randomId(), true, "admin");
        channel.create(event.getNodeId(), event.isDeep(), event.getUserId()).ended(true);
        
        // now unblock slave
        consumer.unblock();

        // wait for the sync to finish
        syncOnce.join();
        
        assertEquals(master.getRevision(), slave.getRevision());
    }

    /**
     * Create a cluster node, with a memory journal referencing a list of records.
     *
     * @param id cluster node id
     * @param records memory journal's list of records
     * @param disableAutoSync if <code>true</code> background synchronization is disabled
     */
    private ClusterNode createClusterNode(String id, boolean disableAutoSync) throws Exception {
        final MemoryJournal journal = new MemoryJournal() {
            protected boolean syncAgainOnNewRecords() {
                return true;
            }
        };
        JournalFactory jf = new JournalFactory() {
            public Journal getJournal(NamespaceResolver resolver)
                    throws RepositoryException {
                return journal;
            }
        };
        ClusterConfig cc = new ClusterConfig(id, SYNC_DELAY, jf);
        SimpleClusterContext context = new SimpleClusterContext(cc);

        journal.setRepositoryHome(context.getRepositoryHome());
        journal.init(id, context.getNamespaceResolver());
        journal.setRecords(records);
        
        ClusterNode clusterNode = new ClusterNode();
        clusterNode.init(context);
        if (disableAutoSync) {
            clusterNode.disableAutoSync();
        }
        return clusterNode;
    }
    
    /**
     * Custom consumer that will block inside the journal's sync() method
     * until it is unblocked.
     */
    static class BlockingConsumer implements RecordConsumer {

        private final Latch blockLatch = new Latch();
        private final Latch unblockLatch = new Latch();
        private long revision;
        
        public String getId() {
            return "CUSTOM";
        }

        public long getRevision() {
            return revision;
        }

        public void consume(Record record) {
            /* nothing to be done here */
        }

        public void setRevision(long revision) {
            blockLatch.release();
            
            try {
                unblockLatch.acquire();
            } catch (InterruptedException e) {
                /* ignore */
            }
            this.revision = revision;
        }
        
        public void waitUntilBlocked() throws InterruptedException {
            blockLatch.acquire();
        }
        
        public void unblock() {
            unblockLatch.release();
        }
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java
index da45f3719..2034dba58 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java
@@ -38,6 +38,7 @@ public class TestAll extends TestCase {
         TestSuite suite = new TestSuite();
 
         suite.addTestSuite(ClusterRecordTest.class);
        suite.addTestSuite(ClusterSyncTest.class);
         suite.addTestSuite(DbClusterTest.class);
 
         return suite;
- 
2.19.1.windows.1

