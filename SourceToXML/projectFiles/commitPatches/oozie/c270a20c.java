From c270a20c3819a04cfa368f2dc34e0a98484f2c9c Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Fri, 5 Sep 2014 16:34:09 -0700
Subject: [PATCH] OOZIE-1989 NPE during a rerun with forks (rkanter)

--
 .../apache/oozie/workflow/lite/LiteWorkflowInstance.java    | 6 ++++--
 release-log.txt                                             | 1 +
 2 files changed, 5 insertions(+), 2 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java
index fb823e2b3..919c95a00 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java
@@ -727,8 +727,8 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
 
         @Override
         public int compare(String node1, String node2) {
            Date date1 = FAR_INTO_THE_FUTURE;
            Date date2 = FAR_INTO_THE_FUTURE;
            Date date1 = null;
            Date date2 = null;
             NodeInstance node1Instance = executionPaths.get(node1);
             if (node1Instance != null) {
                 date1 = this.actionEndTimes.get(node1Instance.nodeName);
@@ -737,6 +737,8 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
             if (node2Instance != null) {
                 date2 = this.actionEndTimes.get(node2Instance.nodeName);
             }
            date1 = (date1 == null) ? FAR_INTO_THE_FUTURE : date1;
            date2 = (date2 == null) ? FAR_INTO_THE_FUTURE : date2;
             return date1.compareTo(date2);
         }
 
diff --git a/release-log.txt b/release-log.txt
index 0435249a1..bace76f28 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -15,6 +15,7 @@ OOZIE-1943 Bump up trunk to 4.2.0-SNAPSHOT (bzhang)
 
 -- Oozie 4.1.0 release (4.1 - unreleased)
 
OOZIE-1989 NPE during a rerun with forks (rkanter)
 OOZIE-1945 NPE in JaveActionExecutor#check() (sree2k via rkanter)
 OOZIE-1984 SLACalculator in HA mode performs duplicate operations on records with completed jobs (mona)
 OOZIE-1958 address duplication of env variables in oozie.launcher.yarn.app.mapreduce.am.env when running with uber mode (ryota)
- 
2.19.1.windows.1

