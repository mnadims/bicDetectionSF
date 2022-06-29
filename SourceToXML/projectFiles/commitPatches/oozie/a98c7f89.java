From a98c7f89a4515c4c86c291929b735f582a9a7a23 Mon Sep 17 00:00:00 2001
From: rkanter <rkanter@unknown>
Date: Wed, 10 Jul 2013 23:37:01 +0000
Subject: [PATCH] OOZIE-1447 Sqoop actions that don't launch a map reduce job
 fail with an IllegalArgumentException (jarcec via rkanter)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1502059 13f79535-47bb-0310-9956-ffa450edef68
--
 .../action/hadoop/SqoopActionExecutor.java    | 33 +++++-----
 release-log.txt                               |  1 +
 .../hadoop/TestSqoopActionExecutor.java       | 63 +++++++++++++++++++
 3 files changed, 82 insertions(+), 15 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java
index a82aa9165..24f20cfd3 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java
@@ -142,26 +142,29 @@ public class SqoopActionExecutor extends JavaActionExecutor {
                 // Cumulative counters for all Sqoop mapreduce jobs
                 Counters counters = null;
 
                // Sqoop do not have to create mapreduce job each time
                 String externalIds = action.getExternalChildIDs();
                String []jobIds = externalIds.split(",");
                if (externalIds != null && !externalIds.trim().isEmpty()) {
                    String []jobIds = externalIds.split(",");
 
                for(String jobId : jobIds) {
                    RunningJob runningJob = jobClient.getJob(JobID.forName(jobId));
                    if (runningJob == null) {
                      throw new ActionExecutorException(ActionExecutorException.ErrorType.FAILED, "SQOOP001",
                        "Unknown hadoop job [{0}] associated with action [{1}].  Failing this action!", action
                        .getExternalId(), action.getId());
                    }
                    for(String jobId : jobIds) {
                        RunningJob runningJob = jobClient.getJob(JobID.forName(jobId));
                        if (runningJob == null) {
                          throw new ActionExecutorException(ActionExecutorException.ErrorType.FAILED, "SQOOP001",
                            "Unknown hadoop job [{0}] associated with action [{1}].  Failing this action!", action
                            .getExternalId(), action.getId());
                        }
 
                    Counters taskCounters = runningJob.getCounters();
                    if(taskCounters != null) {
                        if(counters == null) {
                          counters = taskCounters;
                        Counters taskCounters = runningJob.getCounters();
                        if(taskCounters != null) {
                            if(counters == null) {
                              counters = taskCounters;
                            } else {
                              counters.incrAllCounters(taskCounters);
                            }
                         } else {
                          counters.incrAllCounters(taskCounters);
                          XLog.getLog(getClass()).warn("Could not find Hadoop Counters for job: [{0}]", jobId);
                         }
                    } else {
                      XLog.getLog(getClass()).warn("Could not find Hadoop Counters for job: [{0}]", jobId);
                     }
                 }
 
diff --git a/release-log.txt b/release-log.txt
index dc915641a..0bb204b94 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1447 Sqoop actions that don't launch a map reduce job fail with an IllegalArgumentException (jarcec via rkanter)
 OOZIE-1440 Build fails in certain environments due to xerces OpenJPA issue (mackrorysd via rkanter)
 
 -- Oozie 4.0.0 release
diff --git a/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java b/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
index 1f7e62561..c790e0af2 100644
-- a/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
++ b/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
@@ -95,6 +95,27 @@ public class TestSqoopActionExecutor extends ActionExecutorTestCase {
             "<arg>I</arg>" +
             "</sqoop>";
 
    private static final String SQOOP_ACTION_EVAL_XML =
            "<sqoop xmlns=\"uri:oozie:sqoop-action:0.1\">" +
            "<job-tracker>{0}</job-tracker>" +
            "<name-node>{1}</name-node>" +
            "<configuration>" +
            "<property>" +
            "<name>oozie.sqoop.log.level</name>" +
            "<value>INFO</value>" +
            "</property>" +
            "</configuration>" +
            "<arg>eval</arg>" +
            "<arg>--connect</arg>" +
            "<arg>{2}</arg>" +
            "<arg>--username</arg>" +
            "<arg>sa</arg>" +
            "<arg>--password</arg>" +
            "<arg></arg>" +
            "<arg>--verbose</arg>" +
            "<arg>--query</arg>" +
            "<arg>{3}</arg>" +
            "</sqoop>";
 
     @Override
     protected void setSystemProps() throws Exception {
@@ -168,6 +189,12 @@ public class TestSqoopActionExecutor extends ActionExecutorTestCase {
                                     "dummy", "dummyValue", command);
     }
 
    private String getActionXmlEval() {
      String query = "select TT.I, TT.S from TT";
      return MessageFormat.format(SQOOP_ACTION_EVAL_XML, getJobTrackerUri(), getNameNodeUri(),
        getActionJdbcUri(), query);
    }

     private String getActionXmlFreeFromQuery() {
         String query = "select TT.I, TT.S from TT where $CONDITIONS";
         return MessageFormat.format(SQOOP_ACTION_ARGS_XML, getJobTrackerUri(), getNameNodeUri(),
@@ -233,6 +260,42 @@ public class TestSqoopActionExecutor extends ActionExecutorTestCase {
         assertTrue(outputData.getProperty(LauncherMain.HADOOP_JOBS).trim().length() > 0);
     }
 
    public void testSqoopEval() throws Exception {
        createDB();

        Context context = createContext(getActionXmlEval());
        final RunningJob launcherJob = submitAction(context);
        String launcherId = context.getAction().getExternalId();
        waitFor(120 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                return launcherJob.isComplete();
            }
        });
        assertTrue(launcherJob.isSuccessful());

        assertFalse(LauncherMapperHelper.hasIdSwap(launcherJob));

        SqoopActionExecutor ae = new SqoopActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
        assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
        assertNotNull(context.getAction().getData());
        assertNotNull(context.getAction().getExternalChildIDs());
        assertEquals(0, context.getAction().getExternalChildIDs().length());
        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        String hadoopCounters = context.getVar(MapReduceActionExecutor.HADOOP_COUNTERS);
        assertNotNull(hadoopCounters);
        assertTrue(hadoopCounters.isEmpty());

        assertNotNull(context.getAction().getData());
        Properties outputData = new Properties();
        outputData.load(new StringReader(context.getAction().getData()));
        assertTrue(outputData.containsKey(LauncherMain.HADOOP_JOBS));
        assertEquals(0, outputData.getProperty(LauncherMain.HADOOP_JOBS).trim().length());
    }

     public void testSqoopActionFreeFormQuery() throws Exception {
         createDB();
 
- 
2.19.1.windows.1

