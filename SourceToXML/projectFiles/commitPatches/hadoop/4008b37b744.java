From 4008b37b744ebb01f807e7608959194f36615245 Mon Sep 17 00:00:00 2001
From: Aaron Myers <atm@apache.org>
Date: Thu, 14 Mar 2013 20:11:49 +0000
Subject: [PATCH] HADOOP-9405. TestGridmixSummary#testExecutionSummarizer is
 broken. Contributed by Andrew Wang.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1456639 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt             | 3 +++
 .../apache/hadoop/mapred/gridmix/TestGridmixSummary.java    | 6 +++---
 2 files changed, 6 insertions(+), 3 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 70004ff25f4..c9d6251b7b8 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -350,6 +350,9 @@ Trunk (Unreleased)
 
     HADOOP-9397. Incremental dist tar build fails (Chris Nauroth via jlowe)
 
    HADOOP-9405. TestGridmixSummary#testExecutionSummarizer is broken. (Andrew
    Wang via atm)

   OPTIMIZATIONS
 
     HADOOP-7761. Improve the performance of raw comparisons. (todd)
diff --git a/hadoop-tools/hadoop-gridmix/src/test/java/org/apache/hadoop/mapred/gridmix/TestGridmixSummary.java b/hadoop-tools/hadoop-gridmix/src/test/java/org/apache/hadoop/mapred/gridmix/TestGridmixSummary.java
index f49617fd5cf..61e5ea05777 100644
-- a/hadoop-tools/hadoop-gridmix/src/test/java/org/apache/hadoop/mapred/gridmix/TestGridmixSummary.java
++ b/hadoop-tools/hadoop-gridmix/src/test/java/org/apache/hadoop/mapred/gridmix/TestGridmixSummary.java
@@ -257,7 +257,7 @@ public void testExecutionSummarizer() throws IOException {
                  qPath.toString(), es.getInputTraceLocation());
     // test expected data size
     assertEquals("Mismatch in expected data size", 
                 "1.0k", es.getExpectedDataSize());
                 "1 K", es.getExpectedDataSize());
     // test input data statistics
     assertEquals("Mismatch in input data statistics", 
                  ExecutionSummarizer.stringifyDataStatistics(dataStats), 
@@ -272,7 +272,7 @@ public void testExecutionSummarizer() throws IOException {
     es.finalize(factory, testTraceFile.toString(), 1024*1024*1024*10L, resolver,
                 dataStats, conf);
     assertEquals("Mismatch in expected data size", 
                 "10.0g", es.getExpectedDataSize());
                 "10 G", es.getExpectedDataSize());
     
     // test trace signature uniqueness
     //  touch the trace file
@@ -389,4 +389,4 @@ public void testClusterSummarizer() throws IOException {
     assertEquals("Cluster summary test failed!", 0, 
                  cs.getNumBlacklistedTrackers());
   }
}
\ No newline at end of file
}
- 
2.19.1.windows.1

