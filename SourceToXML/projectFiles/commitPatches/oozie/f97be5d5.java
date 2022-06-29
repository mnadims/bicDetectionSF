From f97be5d55cf8c25da0456b0345eae0aa4330c68b Mon Sep 17 00:00:00 2001
From: rohini <rohini@unknown>
Date: Mon, 19 Aug 2013 23:36:16 +0000
Subject: [PATCH] OOZIE-1501 Mapreduce action counters are picked up from
 launcher job instead of mapreduce job (rohini)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1515666 13f79535-47bb-0310-9956-ffa450edef68
--
 .../oozie/action/hadoop/MapReduceActionExecutor.java   | 10 +++++-----
 release-log.txt                                        |  1 +
 .../action/hadoop/TestMapReduceActionExecutor.java     |  2 ++
 3 files changed, 8 insertions(+), 5 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
index eab14b7c7..5e2592ac6 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
@@ -29,7 +29,6 @@ import org.apache.hadoop.mapred.JobClient;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.JobID;
 import org.apache.hadoop.mapred.RunningJob;
import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.service.Services;
@@ -181,11 +180,11 @@ public class MapReduceActionExecutor extends JavaActionExecutor {
                 Element actionXml = XmlUtils.parseXml(action.getConf());
                 JobConf jobConf = createBaseHadoopConf(context, actionXml);
                 jobClient = createJobClient(context, jobConf);
                RunningJob runningJob = jobClient.getJob(JobID.forName(action.getExternalId()));
                RunningJob runningJob = jobClient.getJob(JobID.forName(action.getExternalChildIDs()));
                 if (runningJob == null) {
                     throw new ActionExecutorException(ActionExecutorException.ErrorType.FAILED, "MR002",
                                                      "Unknown hadoop job [{0}] associated with action [{1}].  Failing this action!", action
                            .getExternalId(), action.getId());
                            "Unknown hadoop job [{0}] associated with action [{1}].  Failing this action!",
                            action.getExternalChildIDs(), action.getId());
                 }
 
                 Counters counters = runningJob.getCounters();
@@ -207,7 +206,8 @@ public class MapReduceActionExecutor extends JavaActionExecutor {
                 }
                 else {
                     context.setVar(HADOOP_COUNTERS, "");
                    XLog.getLog(getClass()).warn("Could not find Hadoop Counters for: [{0}]", action.getExternalId());
                    XLog.getLog(getClass()).warn("Could not find Hadoop Counters for: [{0}]",
                            action.getExternalChildIDs());
                 }
             }
         }
diff --git a/release-log.txt b/release-log.txt
index 0511f3fa5..d01591224 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -16,6 +16,7 @@ OOZIE-1440 Build fails in certain environments due to xerces OpenJPA issue (mack
 
 -- Oozie 4.0.0 release
 
OOZIE-1501 Mapreduce action counters are picked up from launcher job instead of mapreduce job (rohini)
 OOZIE-1405 Fix flakey SLA tests (mona)
 OOZIE-1480 Web-console Workflow Job Info popup should display parent-id field and no empty Nominal time field (mona)
 OOZIE-1481 Getting a coordinator job info with len=0 should return 0 actions (rohini)
diff --git a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
index 844a6449f..bf2a78fb4 100644
-- a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
++ b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
@@ -54,6 +54,7 @@ import java.util.Scanner;
 import java.util.jar.JarOutputStream;
 import java.util.regex.Pattern;
 import java.util.zip.ZipEntry;

 import org.apache.hadoop.fs.FileStatus;
 import org.apache.oozie.action.ActionExecutorException;
 
@@ -373,6 +374,7 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         assertNotNull(context.getVar("hadoop.counters"));
         String counters = context.getVar("hadoop.counters");
         assertTrue(counters.contains("Counter"));
        assertTrue(counters.contains("\"MAP_OUTPUT_RECORDS\":2"));
 
         //External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
         assertNotNull(context.getExternalChildIDs());
- 
2.19.1.windows.1

