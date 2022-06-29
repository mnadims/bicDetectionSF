From dc56aaeed6a56dff58e7ff1328ab36da8cae427a Mon Sep 17 00:00:00 2001
From: virag <virag@unknown>
Date: Fri, 12 Oct 2012 00:55:53 +0000
Subject: [PATCH] OOZIE-1012 Sqoop jobs are unable to utilize Hadoop Counters
 (jarcec via virag)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1397401 13f79535-47bb-0310-9956-ffa450edef68
--
 .../action/hadoop/SqoopActionExecutor.java    | 143 ++++++++++++++++++
 .../hadoop/TestSqoopActionExecutor.java       |  12 ++
 .../site/twiki/DG_SqoopActionExtension.twiki  |   8 +
 .../site/twiki/WorkflowFunctionalSpec.twiki   |   1 +
 release-log.txt                               |   1 +
 5 files changed, 165 insertions(+)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java
index 7bb2b430c..05c32d914 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/SqoopActionExecutor.java
@@ -19,22 +19,33 @@ package org.apache.oozie.action.hadoop;
 
 import java.io.IOException;
 import java.io.StringReader;
import java.net.URISyntaxException;
 import java.util.ArrayList;
 import java.util.List;
import java.util.Properties;
 import java.util.StringTokenizer;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.service.HadoopAccessorException;
 import org.apache.oozie.util.XConfiguration;
 import org.apache.oozie.util.XmlUtils;
import org.apache.oozie.util.XLog;
 import org.jdom.Element;
 import org.jdom.JDOMException;
 import org.jdom.Namespace;
 
 public class SqoopActionExecutor extends JavaActionExecutor {
 
  public static final String OOZIE_ACTION_EXTERNAL_STATS_WRITE = "oozie.action.external.stats.write";
 
     public SqoopActionExecutor() {
         super("sqoop");
@@ -107,6 +118,138 @@ public class SqoopActionExecutor extends JavaActionExecutor {
         return actionConf;
     }
 
    /**
     * We will gather counters from all executed action Hadoop jobs (e.g. jobs
     * that moved data, not the launcher itself) and merge them together. There
     * will be only one job most of the time. The only exception is
     * import-all-table option that will execute one job per one exported table.
     *
     * @param context Action context
     * @param action Workflow action
     * @throws ActionExecutorException
     */
    @Override
    public void end(Context context, WorkflowAction action) throws ActionExecutorException {
        super.end(context, action);
        JobClient jobClient = null;

        boolean exception = false;
        try {
            if (action.getStatus() == WorkflowAction.Status.OK) {
                Element actionXml = XmlUtils.parseXml(action.getConf());
                JobConf jobConf = createBaseHadoopConf(context, actionXml);
                jobClient = createJobClient(context, jobConf);

                // Cumulative counters for all Sqoop mapreduce jobs
                Counters counters = null;

                String externalIds = action.getExternalChildIDs();
                String []jobIds = externalIds.split(",");

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
                        } else {
                          counters.incrAllCounters(taskCounters);
                        }
                    } else {
                      XLog.getLog(getClass()).warn("Could not find Hadoop Counters for job: [{0}]", jobId);
                    }
                }

                if (counters != null) {
                    ActionStats stats = new MRStats(counters);
                    String statsJsonString = stats.toJSON();
                    context.setVar(MapReduceActionExecutor.HADOOP_COUNTERS, statsJsonString);

                    // If action stats write property is set to false by user or
                    // size of stats is greater than the maximum allowed size,
                    // do not store the action stats
                    if (Boolean.parseBoolean(evaluateConfigurationProperty(actionXml,
                            OOZIE_ACTION_EXTERNAL_STATS_WRITE, "true"))
                            && (statsJsonString.getBytes().length <= getMaxExternalStatsSize())) {
                        context.setExecutionStats(statsJsonString);
                        log.debug(
                          "Printing stats for sqoop action as a JSON string : [{0}]" + statsJsonString);
                    }
                } else {
                    context.setVar(MapReduceActionExecutor.HADOOP_COUNTERS, "");
                    XLog.getLog(getClass()).warn("Can't find any associated Hadoop job counters");
                }
            }
        }
        catch (Exception ex) {
            exception = true;
            throw convertException(ex);
        }
        finally {
            if (jobClient != null) {
                try {
                    jobClient.close();
                }
                catch (Exception e) {
                    if (exception) {
                        log.error("JobClient error: ", e);
                    }
                    else {
                        throw convertException(e);
                    }
                }
            }
        }
    }

    // Return the value of the specified configuration property
    private String evaluateConfigurationProperty(Element actionConf, String key, String defaultValue)
            throws ActionExecutorException {
        try {
            if (actionConf != null) {
                Namespace ns = actionConf.getNamespace();
                Element e = actionConf.getChild("configuration", ns);

                if(e != null) {
                  String strConf = XmlUtils.prettyPrint(e).toString();
                  XConfiguration inlineConf = new XConfiguration(new StringReader(strConf));
                  return inlineConf.get(key, defaultValue);
                }
            }
            return defaultValue;
        }
        catch (IOException ex) {
            throw convertException(ex);
        }
    }

    /**
     * Get the stats and external child IDs
     *
     * @param actionFs the FileSystem object
     * @param runningJob the runningJob
     * @param action the Workflow action
     * @param context executor context
     *
     */
    @Override
    protected void getActionData(FileSystem actionFs, RunningJob runningJob, WorkflowAction action, Context context)
            throws HadoopAccessorException, JDOMException, IOException, URISyntaxException{
        super.getActionData(actionFs, runningJob, action, context);

        // Load stored Hadoop jobs ids and promote them as external child ids
        action.getData();
        Properties props = new Properties();
        props.load(new StringReader(action.getData()));
        context.setExternalChildIDs((String)props.get(LauncherMain.HADOOP_JOBS));
    }

     @Override
     protected boolean getCaptureOutput(WorkflowAction action) throws JDOMException {
         return true;
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
index c064d95df..9f160b01c 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
@@ -175,8 +175,14 @@ public class TestSqoopActionExecutor extends ActionExecutorTestCase {
         assertTrue(launcherId.equals(context.getAction().getExternalId()));
         assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
         assertNotNull(context.getAction().getData());
        assertNotNull(context.getAction().getExternalChildIDs());
         ae.end(context, context.getAction());
         assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        String hadoopCounters = context.getVar(MapReduceActionExecutor.HADOOP_COUNTERS);
        assertNotNull(hadoopCounters);
        assertFalse(hadoopCounters.isEmpty());

         FileSystem fs = getFileSystem();
         BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(new Path(getSqoopOutputDir(), "part-m-00000"))));
         int count = 0;
@@ -216,8 +222,14 @@ public class TestSqoopActionExecutor extends ActionExecutorTestCase {
         assertTrue(launcherId.equals(context.getAction().getExternalId()));
         assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
         assertNotNull(context.getAction().getData());
        assertNotNull(context.getAction().getExternalChildIDs());
         ae.end(context, context.getAction());
         assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        String hadoopCounters = context.getVar(MapReduceActionExecutor.HADOOP_COUNTERS);
        assertNotNull(hadoopCounters);
        assertFalse(hadoopCounters.isEmpty());

         FileSystem fs = getFileSystem();
         FileStatus[] parts = fs.listStatus(new Path(getSqoopOutputDir()), new PathFilter() {
             @Override
diff --git a/docs/src/site/twiki/DG_SqoopActionExtension.twiki b/docs/src/site/twiki/DG_SqoopActionExtension.twiki
index eccafac22..b256529b8 100644
-- a/docs/src/site/twiki/DG_SqoopActionExtension.twiki
++ b/docs/src/site/twiki/DG_SqoopActionExtension.twiki
@@ -170,6 +170,14 @@ The same Sqoop action using =arg= elements:
 NOTE: The =arg= elements syntax, while more verbose, allows to have spaces in a single argument, something useful when
 using free from queries.
 
---+++ Sqoop Action Counters

The counters of the map-reduce job run by the Sqoop action are available to be used in the workflow via the
[[WorkflowFunctionalSpec#HadoopCountersEL][hadoop:counters() EL function]].

If the Sqoop action run an import all command, the =hadoop:counters()= EL will return the aggregated counters
of all map-reduce jobs run by the Sqoop import all command.

 ---+++ Sqoop Action Logging
 
 Sqoop action logs are redirected to the Oozie Launcher map-reduce job task STDOUT/STDERR that runs Sqoop.
diff --git a/docs/src/site/twiki/WorkflowFunctionalSpec.twiki b/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
index cb34fd8ba..690a7f95a 100644
-- a/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
++ b/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
@@ -1688,6 +1688,7 @@ not completed yet.
 
 ---++++ 4.2.5 Hadoop EL Functions
 
#HadoopCountersEL
 *Map < String, Map < String, Long > > hadoop:counters(String node)*
 
 It returns the counters for a job submitted by a Hadoop action node. It returns =0= if the if the Hadoop job has not
diff --git a/release-log.txt b/release-log.txt
index a9beaeb63..11ec8257d 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.4.0 release (trunk - unreleased)
 
OOZIE-1012 Sqoop jobs are unable to utilize Hadoop Counters (jarcec via virag)
 OOZIE-986 Oozie client shell script should use consistent naming for java options (stevenwillis via tucu)
 OOZIE-1018 Display coord job start time, end time, pause time, concurrency in job -info (mona via tucu)
 OOZIE-1016 Tests that use junit assert or fail in a new thread report success when they are actually failing (rkanter via tucu)
- 
2.19.1.windows.1

