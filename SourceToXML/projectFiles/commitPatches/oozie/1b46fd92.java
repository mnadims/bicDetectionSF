From 1b46fd92f10c561d14839c5a53da12d3afb46bb8 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Thu, 18 Aug 2016 11:51:24 -0700
Subject: [PATCH] OOZIE-2589 CompletedActionXCommand is hardcoded to wrong
 priority (tm_linfly via rkanter)

--
 .../org/apache/oozie/command/wf/CompletedActionXCommand.java    | 2 +-
 release-log.txt                                                 | 1 +
 2 files changed, 2 insertions(+), 1 deletion(-)

diff --git a/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
index bc39bce14..26397e0c9 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
@@ -51,7 +51,7 @@ public class CompletedActionXCommand extends WorkflowXCommand<Void> {
     }
 
     public CompletedActionXCommand(String actionId, String externalStatus, Properties actionData, int priority) {
        this(actionId, externalStatus, actionData, 1, 0);
        this(actionId, externalStatus, actionData, priority, 0);
     }
 
     public CompletedActionXCommand(String actionId, String externalStatus, Properties actionData) {
diff --git a/release-log.txt b/release-log.txt
index b222fc053..fc3ad1c78 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2589 CompletedActionXCommand is hardcoded to wrong priority (tm_linfly via rkanter)
 OOZIE-2081 WorkflowJob notification to include coordinator action id (seoeun25 via rkanter)
 OOZIE-2036 Drop support for Java 1.6 (gezapeti via jaydeepvishwakarma)
 OOZIE-2512 ShareLibservice returns incorrect path for jar (satishsaley via puru)
- 
2.19.1.windows.1

