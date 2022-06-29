From 89f68fbb4603bb92ca1e27f6334f57577a568fa0 Mon Sep 17 00:00:00 2001
From: virag <virag@unknown>
Date: Sat, 25 Aug 2012 00:27:22 +0000
Subject: [PATCH] OOZIE-969 Unit tests in TestStatusTransitService failing due
 to change in CoordKillX (mona via virag)

git-svn-id: https://svn.apache.org/repos/asf/incubator/oozie/trunk@1377162 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/oozie/command/coord/CoordKillXCommand.java     | 4 ++--
 .../org/apache/oozie/service/TestStatusTransitService.java    | 4 ++--
 release-log.txt                                               | 1 +
 3 files changed, 5 insertions(+), 4 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordKillXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordKillXCommand.java
index 7ce4dfcd6..e39666077 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordKillXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordKillXCommand.java
@@ -28,7 +28,7 @@ import org.apache.oozie.command.wf.KillXCommand;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.KillTransitionXCommand;
 import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.executor.jpa.BulkUpdateInsertJPAExecutor;
import org.apache.oozie.executor.jpa.BulkUpdateInsertForCoordActionStatusJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetActionsNotCompletedJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
@@ -160,7 +160,7 @@ public class CoordKillXCommand extends KillTransitionXCommand {
     @Override
     public void performWrites() throws CommandException {
         try {
            jpaService.execute(new BulkUpdateInsertJPAExecutor(updateList, null));
            jpaService.execute(new BulkUpdateInsertForCoordActionStatusJPAExecutor(updateList, null));
         }
         catch (JPAExecutorException e) {
             throw new CommandException(e);
diff --git a/core/src/test/java/org/apache/oozie/service/TestStatusTransitService.java b/core/src/test/java/org/apache/oozie/service/TestStatusTransitService.java
index 65de2d151..cd68acf7e 100644
-- a/core/src/test/java/org/apache/oozie/service/TestStatusTransitService.java
++ b/core/src/test/java/org/apache/oozie/service/TestStatusTransitService.java
@@ -198,7 +198,7 @@ public class TestStatusTransitService extends XDataTestCase {
         assertEquals(CoordinatorJob.Status.KILLED, coordJob.getStatus());
         assertEquals(CoordinatorAction.Status.KILLED, coordAction.getStatus());
         assertEquals(WorkflowJob.Status.KILLED, wfJob.getStatus());
        //assertEquals(false, coordAction.isPending());
        assertEquals(false, coordAction.isPending());
 
         Runnable runnable = new StatusTransitRunnable();
         runnable.run();
@@ -215,7 +215,7 @@ public class TestStatusTransitService extends XDataTestCase {
         });
 
         coordJob = jpaService.execute(coordJobGetCmd);
        //assertEquals(false, coordJob.isPending());
        assertEquals(false, coordJob.isPending());
     }
 
     /**
diff --git a/release-log.txt b/release-log.txt
index ee5078696..7fed4d206 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.3.0 release (trunk - unreleased)
 
OOZIE-969 Unit tests in TestStatusTransitService failing due to change in CoordKillX (mona via virag)
 OOZIE-965 Allow the timezone attribute in coordinator jobs to use a format like GMT-#### (rkanter via tucu)
 OOZIE-848 Bulk Monitoring API - Consolidated view of jobs (mona via virag)
 OOZIE-934 Exception reporting during Services startup is inadequate (mona via virag)
- 
2.19.1.windows.1

