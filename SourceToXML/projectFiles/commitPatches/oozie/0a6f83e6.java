From 0a6f83e62126c60f9f9f2648368ed8bcf6293876 Mon Sep 17 00:00:00 2001
From: satishsaley <satishsaley@apache.org>
Date: Mon, 9 Oct 2017 15:01:30 -0700
Subject: [PATCH] OOZIE-3031 Coord job with only unresolved dependencies
 doesn't timeout (puru via satishsaley)

--
 .../coord/CoordActionInputCheckXCommand.java    |  3 +--
 .../TestCoordActionInputCheckXCommand.java      | 17 +++++++++++++++++
 release-log.txt                                 |  1 +
 3 files changed, 19 insertions(+), 2 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
index 401b2c7ce..179cc450e 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
@@ -172,7 +172,6 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
 
 
             boolean status = checkResolvedInput(actionXml, existList, nonExistList, actionConf);
            String nonExistListStr = nonExistList.toString();
             boolean isPushDependenciesMet = coordPushInputDependency.isDependencyMet();
             if (status && nonResolvedList.length() > 0) {
                 status = (isPushDependenciesMet) ? checkUnResolvedInput(actionXml, actionConf) : false;
@@ -196,7 +195,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
                 updateCoordAction(coordAction, isChangeInDependency);
             }
             else {
                if (!nonExistListStr.isEmpty() && isPushDependenciesMet) {
                if (isPushDependenciesMet) {
                     queue(new CoordActionTimeOutXCommand(coordAction, coordJob.getUser(), coordJob.getAppName()));
                 }
                 else {
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
index 9f2094290..96ac19533 100644
-- a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
@@ -754,6 +754,23 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
         checkCoordAction(actionId, missingDeps, CoordinatorAction.Status.TIMEDOUT);
     }
 
    @Test
    public void testTimeoutWithUnResolved() throws Exception {
        String jobId = "0000000-" + new Date().getTime() + "-TestCoordActionInputCheckXCommand-C";
        Date startTime = DateUtils.parseDateOozieTZ("2009-02-15T23:59" + TZ);
        Date endTime = DateUtils.parseDateOozieTZ("2009-02-16T23:59" + TZ);
        CoordinatorJobBean job = addRecordToCoordJobTable(jobId, startTime, endTime, "latest");
        new CoordMaterializeTransitionXCommand(job.getId(), 3600).call();
        CoordinatorActionBean action = CoordActionQueryExecutor.getInstance()
                .get(CoordActionQuery.GET_COORD_ACTION, job.getId() + "@1");
        assertEquals(CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR + "${coord:latestRange(-3,0)}",
                action.getMissingDependencies());
        long timeOutCreationTime = System.currentTimeMillis() - (13 * 60 * 1000);
        setCoordActionCreationTime(action.getId(), timeOutCreationTime);
        new CoordActionInputCheckXCommand(action.getId(), action.getJobId()).call();
        checkCoordActionStatus(action.getId(),  CoordinatorAction.Status.TIMEDOUT);
    }

     @Test
     public void testTimeoutWithException() throws Exception {
         String missingDeps = "nofs:///dirx/filex";
diff --git a/release-log.txt b/release-log.txt
index 3358149bc..f126e64e9 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 5.0.0 release (trunk - unreleased)
 
OOZIE-3031 Coord job with only unresolved dependencies doesn't timeout (puru via satishsaley)
 OOZIE-3079 Filtering coordinators returns bundle id as null (satishsaley)
 OOZIE-3078 PasswordMasker throws NPE with null arguments (asasvari)
 OOZIE-3075 Follow-up on OOZIE-3054: create the lib directory if it doesn't exist (pbacsko)
- 
2.19.1.windows.1

