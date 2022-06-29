From 6d4a9d0ea41ee7135ca4f6bc5ad30e73a712a58a Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Mon, 16 Jun 2014 22:18:46 -0700
Subject: [PATCH] Amendment to OOZIE-1879 to increase the test timeout to not
 fail against Hadoop 2

--
 .../java/org/apache/oozie/command/wf/TestReRunXCommand.java   | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
index 5bae614af..efd2a7d23 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
@@ -144,7 +144,7 @@ public class TestReRunXCommand extends XDataTestCase {
 
         final String jobId1 = wfClient.submit(conf);
         wfClient.start(jobId1);
        waitFor(40 * 1000, new Predicate() {
        waitFor(200 * 1000, new Predicate() {
             @Override
             public boolean evaluate() throws Exception {
                 return wfClient.getJobInfo(jobId1).getStatus() == WorkflowJob.Status.KILLED;
@@ -163,7 +163,7 @@ public class TestReRunXCommand extends XDataTestCase {
         conf.setProperty("cmd3", "echo");      // expected to succeed
 
         wfClient.reRun(jobId1, conf);
        waitFor(40 * 1000, new Predicate() {
        waitFor(200 * 1000, new Predicate() {
             @Override
             public boolean evaluate() throws Exception {
                 return wfClient.getJobInfo(jobId1).getStatus() == WorkflowJob.Status.SUCCEEDED;
- 
2.19.1.windows.1

