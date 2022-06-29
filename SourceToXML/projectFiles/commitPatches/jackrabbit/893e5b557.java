From 893e5b5574a8bad9052be285e3590fee486f636e Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Mon, 26 Sep 2011 18:36:36 +0000
Subject: [PATCH] JCR-3063 NullPointerException in ItemManager

Clean up output from the test case



git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1175988 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/core/NPEandCMETest.java | 26 +++++++++----------
 1 file changed, 13 insertions(+), 13 deletions(-)

diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java
index 2a404ab4a..db55de6ce 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NPEandCMETest.java
@@ -27,9 +27,15 @@ import javax.jcr.Session;
 
 import org.apache.jackrabbit.core.state.NoSuchItemStateException;
 import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 public class NPEandCMETest extends AbstractJCRTest {
 
    /** Logger instance */
    private static final Logger log =
            LoggerFactory.getLogger(NPEandCMETest.class);

     private final static int NUM_THREADS = 10;
     private final static boolean SHOW_STACKTRACE = true;
     
@@ -71,8 +77,8 @@ public class NPEandCMETest extends AbstractJCRTest {
             npes += tasks[i].npes;
             cmes += tasks[i].cmes;
         }
        System.err.println("Total NPEs: " + npes);
        System.err.println("Total CMEs: " + cmes);
        assertEquals("Total NPEs > 0", 0, npes);
        assertEquals("Total CMEs > 0", 0, cmes);
     }
     
     private static class TestTask implements Runnable {
@@ -121,24 +127,18 @@ public class NPEandCMETest extends AbstractJCRTest {
                 // ignorable
             }
             catch (RepositoryException e) {
                if (e.getCause() == null || !(e.getCause() instanceof NoSuchItemStateException)) {
                    System.err.println("thread" + id + ":" + e);
                    e.printStackTrace();
                Throwable cause = e.getCause();
                if (!(cause instanceof NoSuchItemStateException)) {
                    log.warn("Unexpected RepositoryException caught", e);
                 }
                 // else ignorable
             }
             catch (NullPointerException e) {
                System.err.println("====> " + e);
                if (SHOW_STACKTRACE) {
                    e.printStackTrace();
                }
                log.error("NPE caught", e);
                 npes++;
             }
             catch (ConcurrentModificationException e) {
                System.err.println("====> " + e);
                if (SHOW_STACKTRACE) {
                    e.printStackTrace();
                }
                log.error("CME caught", e);
                 cmes++;
             }
         }
- 
2.19.1.windows.1

