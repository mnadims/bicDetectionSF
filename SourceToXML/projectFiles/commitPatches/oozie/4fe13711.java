From 4fe137114451450109e32c8c6e30b2f5c898b0bf Mon Sep 17 00:00:00 2001
From: rkanter <rkanter@unknown>
Date: Fri, 6 Dec 2013 17:46:51 +0000
Subject: [PATCH] OOZIE-1634 TestJavaActionExecutor#testUpdateConfForUberMode
 fails against Hadoop 2 (rkanter)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1548612 13f79535-47bb-0310-9956-ffa450edef68
--
 .../oozie/action/hadoop/TestJavaActionExecutor.java   | 11 +++++++++--
 release-log.txt                                       |  1 +
 2 files changed, 10 insertions(+), 2 deletions(-)

diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
index 369aac0d1..88af9b5be 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
@@ -1588,8 +1588,15 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         assertEquals("2560", launcherConf.get(JavaActionExecutor.YARN_AM_RESOURCE_MB));
         // heap size in child.opts (2048 + 512)
         int heapSize = ae.extractHeapSizeMB(launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS));
        assertEquals("-Xmx2048m -Djava.net.preferIPv4Stack=true -Xmx2560m",
                launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS).trim());
        // There's an extra parameter (-Xmx1024m) in here when using YARN that's not here when using MR1
        if (createJobConf().get("yarn.resourcemanager.address") != null) {
            assertEquals("-Xmx1024m -Xmx2048m -Djava.net.preferIPv4Stack=true -Xmx2560m",
                    launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS).trim());
        }
        else {
            assertEquals("-Xmx2048m -Djava.net.preferIPv4Stack=true -Xmx2560m",
                    launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS).trim());
        }
 
         // env
         assertEquals("A=foo", launcherConf.get(JavaActionExecutor.YARN_AM_ENV));
diff --git a/release-log.txt b/release-log.txt
index fc617d86b..f3a5d19ac 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1634 TestJavaActionExecutor#testUpdateConfForUberMode fails against Hadoop 2 (rkanter)
 OOZIE-1633 Test failures related to sharelib when running against Hadoop 2 (rkanter)
 OOZIE-1598 enable html email in email action (puru via ryota)
 OOZIE-1631 Tools module should have a direct dependency on mockito (rkanter)
- 
2.19.1.windows.1

