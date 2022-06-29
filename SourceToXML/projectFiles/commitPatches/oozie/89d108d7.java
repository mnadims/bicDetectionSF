From 89d108d7acbcd527e84e0f41401e800d45e20ea1 Mon Sep 17 00:00:00 2001
From: rohini <rohini@unknown>
Date: Thu, 25 Jul 2013 13:00:34 +0000
Subject: [PATCH] OOZIE-1466 current EL should not check for less than or equal
 to zero (rohini)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1506948 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/oozie/coord/CoordELFunctions.java  |  2 -
 .../TestCoordActionInputCheckXCommand.java    | 43 ++++++++-----------
 release-log.txt                               |  1 +
 3 files changed, 20 insertions(+), 26 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
index 192c53032..050dcbdbe 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
@@ -524,8 +524,6 @@ public class CoordELFunctions {
      * @throws Exception
      */
     public static String ph2_coord_currentRange(int start, int end) throws Exception {
        ParamChecker.checkLEZero(start, "current:n");
        ParamChecker.checkLEZero(end, "current:n");
         if (isSyncDataSet()) { // For Sync Dataset
             return coord_currentRange_sync(start, end);
         }
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
index 290378700..7b8549243 100644
-- a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
@@ -154,10 +154,18 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
         Date endTime = DateUtils.parseDateOozieTZ("2009-02-02T23:59" + TZ);
         CoordinatorJobBean job = addRecordToCoordJobTable(jobId, startTime, endTime);
         new CoordMaterializeTransitionXCommand(job.getId(), 3600).call();
        createDir(getTestCaseDir() + "/2009/01/29/");
        createDir(getTestCaseDir() + "/2009/02/05/");
         createDir(getTestCaseDir() + "/2009/01/15/");
         new CoordActionInputCheckXCommand(job.getId() + "@1", job.getId()).call();
        checkCoordAction(job.getId() + "@1");
        JPAService jpaService = Services.get().get(JPAService.class);
        CoordinatorActionBean action = jpaService.execute(new CoordActionGetJPAExecutor(job.getId() + "@1"));
        System.out.println("missingDeps " + action.getMissingDependencies() + " Xml " + action.getActionXml());
        if (action.getMissingDependencies().indexOf("/2009/02/05/") >= 0) {
            fail("directory should be resolved :" + action.getMissingDependencies());
        }
        if (action.getMissingDependencies().indexOf("/2009/01/15/") < 0) {
            fail("directory should NOT be resolved :" + action.getMissingDependencies());
        }
     }
 
     /**
@@ -172,8 +180,8 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
         new CoordMaterializeTransitionXCommand(job.getId(), 3600).call();
 
         // providing some of the dataset dirs required as per coordinator
        // specification - /2009/02/12, /2009/02/05, /2009/01/29, /2009/01/22
        createDir(getTestCaseDir() + "/2009/02/12/");
        // specification - /2009/02/19, /2009/02/12, /2009/02/05, /2009/01/29, /2009/01/22
        createDir(getTestCaseDir() + "/2009/02/19/");
         createDir(getTestCaseDir() + "/2009/01/29/");
 
         new CoordActionInputCheckXCommand(job.getId() + "@1", job.getId()).call();
@@ -188,9 +196,9 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
 
         // Missing dependencies recorded by the coordinator action after input check
         String missDepsOrder = action.getMissingDependencies();
        // Expected missing dependencies are /2009/02/05, /2009/01/29, and /2009/01/22.
        // Expected missing dependencies are /2009/02/12, /2009/02/05, /2009/01/29, and /2009/01/22.
 
        int index = missDepsOrder.indexOf("/2009/02/12");
        int index = missDepsOrder.indexOf("/2009/02/19");
         if( index >= 0) {
             fail("Dependency should be available! current list: " + missDepsOrder);
         }
@@ -836,10 +844,14 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
             appXml += "<start-instance>${coord:" + dataInType + "(0,5)}</start-instance>";
             appXml += "<end-instance>${coord:" + dataInType + "(3,5)}</end-instance>";
         }
        else {
        else if (dataInType.equals("latest")) {
             appXml += "<start-instance>${coord:" + dataInType + "(-3)}</start-instance>";
             appXml += "<end-instance>${coord:" + dataInType + "(0)}</end-instance>";
         }
        else if (dataInType.equals("current")) {
            appXml += "<start-instance>${coord:" + dataInType + "(-3)}</start-instance>";
            appXml += "<end-instance>${coord:" + dataInType + "(1)}</end-instance>";
        }
         appXml += "</data-in>";
         appXml += "</input-events>";
         appXml += "<output-events>";
@@ -888,23 +900,6 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
         return coordJob;
     }
 
    private void checkCoordAction(String actionId) {
        try {
            JPAService jpaService = Services.get().get(JPAService.class);
            CoordinatorActionBean action = jpaService.execute(new CoordActionGetJPAExecutor(actionId));
            System.out.println("missingDeps " + action.getMissingDependencies() + " Xml " + action.getActionXml());
            if (action.getMissingDependencies().indexOf("/2009/01/29/") >= 0) {
                fail("directory should be resolved :" + action.getMissingDependencies());
            }
            if (action.getMissingDependencies().indexOf("/2009/01/15/") < 0) {
                fail("directory should NOT be resolved :" + action.getMissingDependencies());
            }
        }
        catch (JPAExecutorException se) {
            fail("Action ID " + actionId + " was not stored properly in db");
        }
    }

     private CoordinatorActionBean checkCoordAction(String actionId, String expDeps, CoordinatorAction.Status stat)
             throws Exception {
         try {
diff --git a/release-log.txt b/release-log.txt
index 32a014524..2ebef2415 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -7,6 +7,7 @@ OOZIE-1440 Build fails in certain environments due to xerces OpenJPA issue (mack
 
 -- Oozie 4.0.0 release
 
OOZIE-1466 current EL should not check for less than or equal to zero (rohini)
 OOZIE-1465 Making constants in CoordELFunctions public for el extensions (shwethags via rohini)
 OOZIE-1450 Duplicate Coord_Action events on Waiting -> Timeout, and Coord Materialize not removing actions on Failure (mona)
 OOZIE-1451 CoordActionInputCheckX does a redundant eagerLoadState (rohini)
- 
2.19.1.windows.1

