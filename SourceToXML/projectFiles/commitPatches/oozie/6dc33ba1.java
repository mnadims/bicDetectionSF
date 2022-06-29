From 6dc33ba18b20ae23016129616d95e95cb29dd454 Mon Sep 17 00:00:00 2001
From: mona <mona@unknown>
Date: Thu, 31 Jan 2013 06:36:46 +0000
Subject: [PATCH] OOZIE-1179

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1440859 13f79535-47bb-0310-9956-ffa450edef68
--
 .../oozie/command/coord/CoordActionInputCheckXCommand.java       | 1 -
 release-log.txt                                                  | 1 +
 2 files changed, 1 insertion(+), 1 deletion(-)

diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
index 7d06ba780..7b6bbcefc 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
@@ -144,7 +144,6 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
                 isChangeInDependency = true;
                 coordAction.setMissingDependencies(nonExistListStr);
             }
			coordAction.setMissingDependencies(nonExistList.toString());
             if (status == true) {
                 coordAction.setStatus(CoordinatorAction.Status.READY);
                 // pass jobID to the CoordActionReadyXCommand
diff --git a/release-log.txt b/release-log.txt
index 4bf4f7198..0351a99db 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.4.0 release (trunk - unreleased)
 
OOZIE-1179 coord action in WAITING when no definition of dataset in coord job xml (mona)
 OOZIE-1194 test-patch shouldn't run the testHive profile because it not longer exists (rkanter)
 OOZIE-1193 upgrade jython to 2.5.3 for Pig in Oozie due to jython 2.5.0 legal issues (bowenzhangusa via rkanter)
 OOZIE-1172 Add documentation on how to get Java actions to authenticate properly on Kerberos-enabled clusters (rkanter)
- 
2.19.1.windows.1

