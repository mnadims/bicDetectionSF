From cefb2db2df9e4e07baa383e5350b325f8c75735c Mon Sep 17 00:00:00 2001
From: Stefan Guggisberg <stefan@apache.org>
Date: Fri, 23 Sep 2011 14:57:52 +0000
Subject: [PATCH] JCR-3063 NullPointerException in ItemManager

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1174822 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/state/SharedItemStateManager.java    |   7 +-
 .../apache/jackrabbit/core/NPEandCMETest.java | 131 ++++++++++++++++++
 .../org/apache/jackrabbit/core/TestAll.java   |   2 +
 3 files changed, 136 insertions(+), 4 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index e733bfea9..5d966ebd1 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -771,14 +771,13 @@ public class SharedItemStateManager
 
             ISMLocking.ReadLock readLock = null;
             try {
                // Let the shared item listeners know about the change
                shared.persisted();

                 // downgrade to read lock
                 readLock = writeLock.downgrade();
                 writeLock = null;
 
                // Let the shared item listeners know about the change
                // JCR-2171: This must happen after downgrading the lock!
                shared.persisted();

                 /* notify virtual providers about node references */
                 for (int i = 0; i < virtualNodeReferences.length; i++) {
                     ChangeLog virtualRefs = virtualNodeReferences[i];
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java
new file mode 100644
index 000000000..b97fdc26f
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java
@@ -0,0 +1,131 @@
package org.apache.jackrabbit.core;

import java.util.ConcurrentModificationException;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.test.AbstractJCRTest;

public class NPEandCMETest extends AbstractJCRTest {

    private final static int NUM_THREADS = 10;
    private final static boolean SHOW_STACKTRACE = true;
    
    protected void setUp() throws Exception {
        super.setUp();
        Session session = getHelper().getSuperuserSession();
        session.getRootNode().addNode("test");
        session.save();
    }
    
    protected void tearDown() throws Exception {
        try {
            Session session = getHelper().getSuperuserSession();
            if (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
                session.save();
            }
        } finally {
            super.tearDown();
        }
    }
    
    public void testDo() throws Exception {
        Thread[] threads = new Thread[NUM_THREADS];
        TestTask[] tasks = new TestTask[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            Session session = getHelper().getSuperuserSession();
            tasks[i] = new TestTask(i, session);
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(tasks[i]);
            threads[i].start();
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].join();
        }
        int npes = 0, cmes = 0;
        for(int i = 0; i < NUM_THREADS; i++) {
            npes += tasks[i].npes;
            cmes += tasks[i].cmes;
        }
        System.err.println("Total NPEs: " + npes);
        System.err.println("Total CMEs: " + cmes);
    }
    
    private static class TestTask implements Runnable {

        private final Session session;
        private final int id;
        private final Node test;
        
        private int npes = 0;
        private int cmes = 0;
        
        private TestTask(int id, Session session) throws RepositoryException {
            this.id = id;
            this.session = session;
            test = this.session.getRootNode().getNode("test");
        }
        
        public void run() {
            try {
                for (int i = 0; i < 500; i++) {
                    NodeIterator nodes = test.getNodes();
                    if (nodes.getSize() > 100) {
                        long count = nodes.getSize() - 100;
                        while (nodes.hasNext() && count-- > 0) {
                            Node node = nodes.nextNode();
                            if (node != null) {
                                try {
                                    node.remove();
                                }
                                catch (ItemNotFoundException e) {
                                    // item was already removed
                                }
                                catch (InvalidItemStateException e) {
                                    // ignorable
                                }
                            }
                        }
                        session.save();
                    }
                    test.addNode("test-" + id + "-" + i);
                    session.save();
                }
                
            }
            catch (InvalidItemStateException e) {
                // ignorable
            }
            catch (RepositoryException e) {
                if (e.getCause() == null || !(e.getCause() instanceof NoSuchItemStateException)) {
                    System.err.println("thread" + id + ":" + e);
                    e.printStackTrace();
                }
                // else ignorable
            }
            catch (NullPointerException e) {
                System.err.println("====> " + e);
                if (SHOW_STACKTRACE) {
                    e.printStackTrace();
                }
                npes++;
            }
            catch (ConcurrentModificationException e) {
                System.err.println("====> " + e);
                if (SHOW_STACKTRACE) {
                    e.printStackTrace();
                }
                cmes++;
            }
        }
        
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
index 7bb211a1f..94e6e3205 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
@@ -80,6 +80,8 @@ public class TestAll extends TestCase {
 
         suite.addTestSuite(OverlappingNodeAddTest.class);
 
        suite.addTestSuite(NPEandCMETest.class);

         return suite;
     }
 }
- 
2.19.1.windows.1

