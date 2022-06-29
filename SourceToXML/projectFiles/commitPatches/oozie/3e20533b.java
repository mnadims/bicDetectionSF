From 3e20533b0fc75ee91ed7e6bad9eff07a63dba35c Mon Sep 17 00:00:00 2001
From: Shwetha GS <sshivalingamurthy@hortonworks.com>
Date: Mon, 27 Apr 2015 10:50:45 +0530
Subject: [PATCH] OOZIE-2129 Duplicate child jobs per instance
 (jaydeepvishwakarma via shwethags)

--
 .../action/hadoop/JavaActionExecutor.java     | 11 ++++-
 .../action/hadoop/LauncherMapperHelper.java   | 14 +++----
 .../oozie/SubWorkflowActionExecutor.java      |  8 ++++
 .../oozie/command/wf/ActionStartXCommand.java | 16 ++++++++
 .../hadoop/LauncherMainHadoopUtils.java       |  4 --
 .../hadoop/LauncherMainHadoopUtils.java       |  4 --
 .../hadoop/LauncherMainHadoopUtils.java       | 34 ++++++++--------
 .../hadoop/LauncherMainHadoopUtils.java       | 34 +++++++---------
 release-log.txt                               |  1 +
 .../apache/oozie/action/hadoop/HiveMain.java  |  2 +
 .../apache/oozie/action/hadoop/Hive2Main.java |  6 +++
 sharelib/oozie/pom.xml                        |  1 -
 .../apache/oozie/action/hadoop/JavaMain.java  |  5 +--
 .../oozie/action/hadoop/LauncherMain.java     | 15 ++++++-
 .../oozie/action/hadoop/LauncherMapper.java   | 40 +++++++++++++++++--
 .../oozie/action/hadoop/MapReduceMain.java    | 28 +++++--------
 .../apache/oozie/action/hadoop/PigMain.java   |  1 +
 sharelib/spark/pom.xml                        |  6 ++-
 .../SparkMain.java                            |  3 +-
 .../apache/oozie/action/hadoop/SqoopMain.java |  1 +
 20 files changed, 151 insertions(+), 83 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index eb2dbdbf9..695853ebc 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -57,6 +57,7 @@ import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.command.wf.ActionStartXCommand;
 import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.HadoopAccessorException;
 import org.apache.oozie.service.HadoopAccessorService;
@@ -884,8 +885,16 @@ public class JavaActionExecutor extends ActionExecutor {
             launcherJobConf.setBoolean("mapreduce.job.complete.cancel.delegation.tokens", true);
             setupLauncherConf(launcherJobConf, actionXml, appPathRoot, context);
 
            String launcherTag = null;
            // Extracting tag and appending action name to maintain the uniqueness.
            if (context.getVar(ActionStartXCommand.OOZIE_ACTION_YARN_TAG) != null) {
                launcherTag = context.getVar(ActionStartXCommand.OOZIE_ACTION_YARN_TAG);
            } else { //Keeping it to maintain backward compatibly with test cases.
                launcherTag = action.getId();
            }

             // Properties for when a launcher job's AM gets restarted
            LauncherMapperHelper.setupYarnRestartHandling(launcherJobConf, actionConf, action.getId());
            LauncherMapperHelper.setupYarnRestartHandling(launcherJobConf, actionConf, launcherTag);
 
             String actionShareLibProperty = actionConf.get(ACTION_SHARELIB_FOR + getType());
             if (actionShareLibProperty != null) {
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java b/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
index 069a734c2..6a93232b5 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
@@ -23,8 +23,6 @@ import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
 import java.math.BigInteger;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
@@ -164,17 +162,19 @@ public class LauncherMapperHelper {
         launcherConf.set("mapred.output.dir", new Path(actionDir, "output").toString());
     }
 
    public static void setupYarnRestartHandling(JobConf launcherJobConf, Configuration actionConf, String actionId)
    public static void setupYarnRestartHandling(JobConf launcherJobConf, Configuration actionConf, String launcherTag)
             throws NoSuchAlgorithmException {
         launcherJobConf.setLong("oozie.job.launch.time", System.currentTimeMillis());
         // Tags are limited to 100 chars so we need to hash them to make sure (the actionId otherwise doesn't have a max length)
        String tag = getTag(actionId);
        actionConf.set("mapreduce.job.tags", tag);
        String tag = getTag(launcherTag);
        // keeping the oozie.child.mapreduce.job.tags instead of mapreduce.job.tags to avoid killing launcher itself.
        // mapreduce.job.tags should only go to child job launch by launcher.
        actionConf.set(LauncherMainHadoopUtils.CHILD_MAPREDUCE_JOB_TAGS, tag);
     }
 
    private static String getTag(String actionId) throws NoSuchAlgorithmException {
    private static String getTag(String launcherTag) throws NoSuchAlgorithmException {
         MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(actionId.getBytes(), 0, actionId.length());
        digest.update(launcherTag.getBytes(), 0, launcherTag.length());
         String md5 = "oozie-" + new BigInteger(1, digest.digest()).toString(16);
         return md5;
     }
diff --git a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
index 527a5e274..854d62132 100644
-- a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
@@ -24,6 +24,7 @@ import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.DagEngine;
 import org.apache.oozie.LocalOozieClient;
 import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.command.wf.ActionStartXCommand;
 import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.DagEngineService;
 import org.apache.oozie.client.WorkflowAction;
@@ -181,6 +182,13 @@ public class SubWorkflowActionExecutor extends ActionExecutor {
                 //TODO: this has to be refactored later to be done in a single place for REST calls and this
                 JobUtils.normalizeAppPath(context.getWorkflow().getUser(), context.getWorkflow().getGroup(),
                                           subWorkflowConf);

                // pushing the tag to conf for using by Launcher.
                if(context.getVar(ActionStartXCommand.OOZIE_ACTION_YARN_TAG) != null) {
                    subWorkflowConf.set(ActionStartXCommand.OOZIE_ACTION_YARN_TAG,
                            context.getVar(ActionStartXCommand.OOZIE_ACTION_YARN_TAG));
                }

                 // if the rerun failed node option is provided during the time of rerun command, old subworkflow will
                 // rerun again.
                 if(action.getExternalId() != null && parentConf.getBoolean(OozieClient.RERUN_FAIL_NODES, false)) {
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
index d4048a18f..e06649cf9 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
@@ -68,6 +68,7 @@ public class ActionStartXCommand extends ActionXCommand<Void> {
     public static final String COULD_NOT_START = "COULD_NOT_START";
     public static final String START_DATA_MISSING = "START_DATA_MISSING";
     public static final String EXEC_DATA_MISSING = "EXEC_DATA_MISSING";
    public static final String OOZIE_ACTION_YARN_TAG = "oozie.action.yarn.tag";
 
     private String jobId = null;
     private String actionId = null;
@@ -231,6 +232,21 @@ public class ActionStartXCommand extends ActionXCommand<Void> {
                 Instrumentation.Cron cron = new Instrumentation.Cron();
                 cron.start();
                 context.setStartTime();
                /*
                Creating and forwarding the tag, It will be useful during repeat attempts of Launcher, to ensure only
                one child job is running. Tag is formed as follows:
                For workflow job, tag = action-id
                For Coord job, tag = coord-action-id@action-name (if not part of sub flow), else
                coord-action-id@subflow-action-name@action-name.
                 */
                if (conf.get(OOZIE_ACTION_YARN_TAG) != null) {
                    context.setVar(OOZIE_ACTION_YARN_TAG, conf.get(OOZIE_ACTION_YARN_TAG) + "@" + wfAction.getName());
                } else if (wfJob.getParentId() != null) {
                    context.setVar(OOZIE_ACTION_YARN_TAG, wfJob.getParentId() + "@" + wfAction.getName());
                } else {
                    context.setVar(OOZIE_ACTION_YARN_TAG, wfAction.getId());
                }

                 executor.start(context, wfAction);
                 cron.stop();
                 FaultInjection.activate("org.apache.oozie.command.SkipCommitFaultInjection");
diff --git a/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index 46c2fbd81..9e34d0bb2 100644
-- a/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -26,10 +26,6 @@ public class LauncherMainHadoopUtils {
     private LauncherMainHadoopUtils() {
     }
 
    public static String getYarnJobForMapReduceAction(Configuration actionConf) {
        return null;
    }

     public static void killChildYarnJobs(Configuration actionConf) {
         // no-op
     }
diff --git a/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index 46c2fbd81..9e34d0bb2 100644
-- a/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -26,10 +26,6 @@ public class LauncherMainHadoopUtils {
     private LauncherMainHadoopUtils() {
     }
 
    public static String getYarnJobForMapReduceAction(Configuration actionConf) {
        return null;
    }

     public static void killChildYarnJobs(Configuration actionConf) {
         // no-op
     }
diff --git a/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index f6bb6a44f..9331c130d 100644
-- a/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -23,6 +23,8 @@ import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;

import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
 import org.apache.hadoop.yarn.api.protocolrecords.ApplicationsRequestScope;
@@ -33,29 +35,35 @@ import org.apache.hadoop.yarn.api.records.ApplicationReport;
 import org.apache.hadoop.yarn.client.ClientRMProxy;
 import org.apache.hadoop.yarn.client.api.YarnClient;
 import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.mapreduce.TypeConverter;
 
 public class LauncherMainHadoopUtils {
 
    public static final String CHILD_MAPREDUCE_JOB_TAGS = "oozie.child.mapreduce.job.tags";

     private LauncherMainHadoopUtils() {
     }
 
     private static Set<ApplicationId> getChildYarnJobs(Configuration actionConf) {
        Set<ApplicationId> childYarnJobs = new HashSet<ApplicationId>();
        System.out.println("Fetching child yarn jobs");
         long startTime = 0L;
         try {
             startTime = Long.parseLong((System.getProperty("oozie.job.launch.time")));
         } catch(NumberFormatException nfe) {
             throw new RuntimeException("Could not find Oozie job launch time", nfe);
         }
        String tag = actionConf.get("mapreduce.job.tags");
        if (tag == null) {
            throw new RuntimeException("Could not find Yarn tags property (mapreduce.job.tags)");

        Set<ApplicationId> childYarnJobs = new HashSet<ApplicationId>();
        if (actionConf.get(CHILD_MAPREDUCE_JOB_TAGS) == null) {
            System.out.print("Could not find Yarn tags property " + CHILD_MAPREDUCE_JOB_TAGS);
            return childYarnJobs;
         }

        String tag = actionConf.get(CHILD_MAPREDUCE_JOB_TAGS);
        System.out.println("tag id : " + tag);
         GetApplicationsRequest gar = GetApplicationsRequest.newInstance();
         gar.setScope(ApplicationsRequestScope.OWN);
        gar.setStartRange(startTime, System.currentTimeMillis());
         gar.setApplicationTags(Collections.singleton(tag));
        gar.setStartRange(startTime, System.currentTimeMillis());
         try {
             ApplicationClientProtocol proxy = ClientRMProxy.createRMProxy(actionConf, ApplicationClientProtocol.class);
             GetApplicationsResponse apps = proxy.getApplications(gar);
@@ -68,19 +76,9 @@ public class LauncherMainHadoopUtils {
         } catch (YarnException ye) {
             throw new RuntimeException("Exception occurred while finding child jobs", ye);
         }
        return childYarnJobs;
    }
 
    public static String getYarnJobForMapReduceAction(Configuration actionConf) {
        Set<ApplicationId> childYarnJobs = getChildYarnJobs(actionConf);
        String childJobId = null;
        if (!childYarnJobs.isEmpty()) {
            ApplicationId childJobYarnId = childYarnJobs.iterator().next();
            System.out.println("Found Map-Reduce job [" + childJobYarnId + "] already running");
            // Need the JobID version for Oozie
            childJobId = TypeConverter.fromYarn(childJobYarnId).toString();
        }
        return childJobId;
        System.out.println("Child yarn jobs are found - " + StringUtils.join(childYarnJobs, ","));
        return childYarnJobs;
     }
 
     public static void killChildYarnJobs(Configuration actionConf) {
diff --git a/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index f6bb6a44f..211ba09fe 100644
-- a/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -33,29 +33,35 @@ import org.apache.hadoop.yarn.api.records.ApplicationReport;
 import org.apache.hadoop.yarn.client.ClientRMProxy;
 import org.apache.hadoop.yarn.client.api.YarnClient;
 import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.mapreduce.TypeConverter;
 
 public class LauncherMainHadoopUtils {
 
    public static final String CHILD_MAPREDUCE_JOB_TAGS = "oozie.child.mapreduce.job.tags";

     private LauncherMainHadoopUtils() {
     }
 
     private static Set<ApplicationId> getChildYarnJobs(Configuration actionConf) {
        Set<ApplicationId> childYarnJobs = new HashSet<ApplicationId>();
        System.out.println("Fetching child yarn jobs");
         long startTime = 0L;
         try {
             startTime = Long.parseLong((System.getProperty("oozie.job.launch.time")));
         } catch(NumberFormatException nfe) {
             throw new RuntimeException("Could not find Oozie job launch time", nfe);
         }
        String tag = actionConf.get("mapreduce.job.tags");
        if (tag == null) {
            throw new RuntimeException("Could not find Yarn tags property (mapreduce.job.tags)");

        Set<ApplicationId> childYarnJobs = new HashSet<ApplicationId>();
        if (actionConf.get(CHILD_MAPREDUCE_JOB_TAGS) == null) {
            System.out.print("Could not find Yarn tags property " + CHILD_MAPREDUCE_JOB_TAGS);
            return childYarnJobs;
         }

        String tag = actionConf.get(CHILD_MAPREDUCE_JOB_TAGS);
        System.out.println("tag id : " + tag);
         GetApplicationsRequest gar = GetApplicationsRequest.newInstance();
         gar.setScope(ApplicationsRequestScope.OWN);
        gar.setStartRange(startTime, System.currentTimeMillis());
         gar.setApplicationTags(Collections.singleton(tag));
        gar.setStartRange(startTime, System.currentTimeMillis());
         try {
             ApplicationClientProtocol proxy = ClientRMProxy.createRMProxy(actionConf, ApplicationClientProtocol.class);
             GetApplicationsResponse apps = proxy.getApplications(gar);
@@ -68,19 +74,9 @@ public class LauncherMainHadoopUtils {
         } catch (YarnException ye) {
             throw new RuntimeException("Exception occurred while finding child jobs", ye);
         }
        return childYarnJobs;
    }
 
    public static String getYarnJobForMapReduceAction(Configuration actionConf) {
        Set<ApplicationId> childYarnJobs = getChildYarnJobs(actionConf);
        String childJobId = null;
        if (!childYarnJobs.isEmpty()) {
            ApplicationId childJobYarnId = childYarnJobs.iterator().next();
            System.out.println("Found Map-Reduce job [" + childJobYarnId + "] already running");
            // Need the JobID version for Oozie
            childJobId = TypeConverter.fromYarn(childJobYarnId).toString();
        }
        return childJobId;
        System.out.println("Child yarn jobs are found - " + StringUtils.join(childYarnJobs, ","));
        return childYarnJobs;
     }
 
     public static void killChildYarnJobs(Configuration actionConf) {
@@ -106,4 +102,4 @@ public class LauncherMainHadoopUtils {
             throw new RuntimeException("Exception occurred while killing child job(s)", ioe);
         }
     }
}
}
\ No newline at end of file
diff --git a/release-log.txt b/release-log.txt
index 1b5cccda7..a0c455712 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-2129 Duplicate child jobs per instance (jaydeepvishwakarma via shwethags)
 OOZIE-2214 fix test case TestCoordRerunXCommand.testCoordRerunDateNeg (ryota)
 OOZIE-2213 oozie-setup.ps1 should use "start-process" rather than "cmd /c" to invoke OozieSharelibCLI or OozieDBCLI commands (bzhang)
 OOZIE-2210 Update extjs 2.2 link (bzhang)
diff --git a/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java b/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java
index 5ea4e1a7c..84bdb7900 100644
-- a/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java
++ b/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java
@@ -85,6 +85,8 @@ public class HiveMain extends LauncherMain {
 
         hiveConf.addResource(new Path("file:///", actionXml));
 
        setYarnTag(hiveConf);

         // Propagate delegation related props from launcher job to Hive job
         String delegationToken = getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION");
         if (delegationToken != null) {
diff --git a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
index 304e3913e..557969e5d 100644
-- a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
++ b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
@@ -72,6 +72,7 @@ public class Hive2Main extends LauncherMain {
         }
 
         actionConf.addResource(new Path("file:///", actionXml));
        setYarnTag(actionConf);
 
         // Propagate delegation related props from launcher job to Hive job
         String delegationToken = getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION");
@@ -199,6 +200,11 @@ public class Hive2Main extends LauncherMain {
             arguments.add(beelineArg);
         }
 
        if (actionConf.get(LauncherMain.MAPREDUCE_JOB_TAGS) != null ) {
            arguments.add("--hiveconf");
            arguments.add("mapreduce.job.tags=" + actionConf.get(LauncherMain.MAPREDUCE_JOB_TAGS));
        }

         System.out.println("Beeline command arguments :");
         for (String arg : arguments) {
             System.out.println("             " + arg);
diff --git a/sharelib/oozie/pom.xml b/sharelib/oozie/pom.xml
index 087b6ded2..484fb45b1 100644
-- a/sharelib/oozie/pom.xml
++ b/sharelib/oozie/pom.xml
@@ -139,6 +139,5 @@
             </plugin>
         </plugins>
     </build>

 </project>
 
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java
index f58ff1d08..10a1b12f5 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java
@@ -20,10 +20,7 @@
 package org.apache.oozie.action.hadoop;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
 
import java.io.File;
import java.io.IOException;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 
@@ -43,6 +40,8 @@ public class JavaMain extends LauncherMain {
 
         Configuration actionConf = loadActionConf();
 
        setYarnTag(actionConf);

         LauncherMainHadoopUtils.killChildYarnJobs(actionConf);
 
         Class<?> klass = actionConf.getClass(JAVA_MAIN_CLASS, Object.class);
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java
index 0860484d9..2288ed08c 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java
@@ -40,6 +40,7 @@ import org.apache.hadoop.mapred.JobConf;
 public abstract class LauncherMain {
 
     public static final String HADOOP_JOBS = "hadoopJobs";
    public static final String MAPREDUCE_JOB_TAGS = "mapreduce.job.tags";
 
     protected static void run(Class<? extends LauncherMain> klass, String[] args) throws Exception {
         LauncherMain main = klass.newInstance();
@@ -181,7 +182,7 @@ public abstract class LauncherMain {
      * @return action  Configuration
      * @throws IOException
      */
    protected Configuration loadActionConf() throws IOException {
    public static Configuration loadActionConf() throws IOException {
         // loading action conf prepared by Oozie
         Configuration actionConf = new Configuration(false);
 
@@ -197,6 +198,18 @@ public abstract class LauncherMain {
         actionConf.addResource(new Path("file:///", actionXml));
         return actionConf;
     }

    protected static void setYarnTag(Configuration actionConf) {
        if(actionConf.get(LauncherMainHadoopUtils.CHILD_MAPREDUCE_JOB_TAGS) != null) {
            // in case the user set their own tags, appending the launcher tag.
            if(actionConf.get(MAPREDUCE_JOB_TAGS) != null) {
                actionConf.set(MAPREDUCE_JOB_TAGS, actionConf.get(MAPREDUCE_JOB_TAGS) + ","
                        + actionConf.get(LauncherMainHadoopUtils.CHILD_MAPREDUCE_JOB_TAGS));
            } else {
                actionConf.set(MAPREDUCE_JOB_TAGS, actionConf.get(LauncherMainHadoopUtils.CHILD_MAPREDUCE_JOB_TAGS));
            }
        }
    }
 }
 
 class LauncherMainException extends Exception {
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
index 9c3128f80..fe3897635 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
@@ -21,12 +21,15 @@ package org.apache.oozie.action.hadoop;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.io.StringWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.security.Permission;
@@ -78,6 +81,8 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
     static final String ACTION_DATA_NEW_ID = "newId";
     static final String ACTION_DATA_ERROR_PROPS = "error.properties";
     public static final String HADOOP2_WORKAROUND_DISTRIBUTED_CACHE = "oozie.hadoop-2.0.2-alpha.workaround.for.distributed.cache";
    public static final String PROPAGATION_CONF_XML = "propagation-conf.xml";
    public static final String OOZIE_LAUNCHER_JOB_ID = "oozie.launcher.job.id";
 
     private void setRecoveryId(Configuration launcherConf, Path actionDir, String recoveryId) throws LauncherException {
         try {
@@ -171,6 +176,9 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
 
                     setupMainConfiguration();
 
                    // Propagating the conf to use by child job.
                    propagateToHadoopConf();

                     try {
                         System.out.println("Starting the execution of prepare actions");
                         executePrepare();
@@ -322,6 +330,34 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
         System.out.println();
     }
 
    /**
     * Pushing all important conf to hadoop conf for the action
     */
    private void propagateToHadoopConf() throws IOException {
        Configuration propagationConf = new Configuration(false);
        if (System.getProperty(OOZIE_ACTION_ID) != null) {
            propagationConf.set(OOZIE_ACTION_ID, System.getProperty(OOZIE_ACTION_ID));
        }
        if (System.getProperty(OOZIE_JOB_ID) != null) {
            propagationConf.set(OOZIE_JOB_ID, System.getProperty(OOZIE_JOB_ID));
        }
        if(System.getProperty(OOZIE_LAUNCHER_JOB_ID) != null) {
            propagationConf.set(OOZIE_LAUNCHER_JOB_ID, System.getProperty(OOZIE_LAUNCHER_JOB_ID));
        }

        // loading action conf prepared by Oozie
        Configuration actionConf = LauncherMain.loadActionConf();

        if(actionConf.get(LauncherMainHadoopUtils.CHILD_MAPREDUCE_JOB_TAGS) != null) {
            propagationConf.set(LauncherMain.MAPREDUCE_JOB_TAGS,
                    actionConf.get(LauncherMainHadoopUtils.CHILD_MAPREDUCE_JOB_TAGS));
        }

        propagationConf.writeXml(new FileWriter(PROPAGATION_CONF_XML));
        Configuration.dumpConfiguration(propagationConf, new OutputStreamWriter(System.out));
        Configuration.addDefaultResource(PROPAGATION_CONF_XML);
    }

     protected JobConf getJobConf() {
         return jobConf;
     }
@@ -421,8 +457,7 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
         Path pathNew = new Path(new Path(actionDir, ACTION_CONF_XML),
                 new Path(new File(ACTION_CONF_XML).getAbsolutePath()));
         FileSystem fs = FileSystem.get(pathNew.toUri(), getJobConf());
        fs.copyToLocalFile(new Path(actionDir, ACTION_CONF_XML),
                new Path(new File(ACTION_CONF_XML).getAbsolutePath()));
        fs.copyToLocalFile(new Path(actionDir, ACTION_CONF_XML), new Path(new File(ACTION_CONF_XML).getAbsolutePath()));
 
         System.setProperty("oozie.launcher.job.id", getJobConf().get("mapred.job.id"));
         System.setProperty(OOZIE_JOB_ID, getJobConf().get(OOZIE_JOB_ID));
@@ -434,7 +469,6 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
         System.setProperty(ACTION_PREFIX + ACTION_DATA_NEW_ID, new File(ACTION_DATA_NEW_ID).getAbsolutePath());
         System.setProperty(ACTION_PREFIX + ACTION_DATA_OUTPUT_PROPS, new File(ACTION_DATA_OUTPUT_PROPS).getAbsolutePath());
         System.setProperty(ACTION_PREFIX + ACTION_DATA_ERROR_PROPS, new File(ACTION_DATA_ERROR_PROPS).getAbsolutePath());
        System.setProperty("oozie.job.launch.time", getJobConf().get("oozie.job.launch.time"));
         String actionConfigClass = getJobConf().get(OOZIE_ACTION_CONFIG_CLASS);
         if (actionConfigClass != null) {
             System.setProperty(OOZIE_ACTION_CONFIG_CLASS, actionConfigClass);
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java
index 61cec7ec8..23447cf87 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java
@@ -46,35 +46,25 @@ public class MapReduceMain extends LauncherMain {
         // loading action conf prepared by Oozie
         Configuration actionConf = new Configuration(false);
         actionConf.addResource(new Path("file:///", System.getProperty("oozie.action.conf.xml")));
        setYarnTag(actionConf);
 
         JobConf jobConf = new JobConf();
         addActionConf(jobConf, actionConf);
        LauncherMainHadoopUtils.killChildYarnJobs(jobConf);
 
         // Run a config class if given to update the job conf
         runConfigClass(jobConf);
 
         logMasking("Map-Reduce job configuration:", new HashSet<String>(), jobConf);
 
        String jobId = LauncherMainHadoopUtils.getYarnJobForMapReduceAction(jobConf);
         File idFile = new File(System.getProperty(LauncherMapper.ACTION_PREFIX + LauncherMapper.ACTION_DATA_NEW_ID));
        if (jobId != null) {
            if (!idFile.exists()) {
                System.out.print("JobId file is mising: writing now... ");
                writeJobIdFile(idFile, jobId);
                System.out.print("Done");
            }
            System.out.println("Exiting launcher");
            System.out.println();
        }
        else {
            System.out.println("Submitting Oozie action Map-Reduce job");
            System.out.println();
            // submitting job
            RunningJob runningJob = submitJob(jobConf);

            jobId = runningJob.getID().toString();
            writeJobIdFile(idFile, jobId);
        }
        System.out.println("Submitting Oozie action Map-Reduce job");
        System.out.println();
        // submitting job
        RunningJob runningJob = submitJob(jobConf);

        String jobId = runningJob.getID().toString();
        writeJobIdFile(idFile, jobId);
 
         System.out.println("=======================");
         System.out.println();
diff --git a/sharelib/pig/src/main/java/org/apache/oozie/action/hadoop/PigMain.java b/sharelib/pig/src/main/java/org/apache/oozie/action/hadoop/PigMain.java
index 129022a17..8228e88d4 100644
-- a/sharelib/pig/src/main/java/org/apache/oozie/action/hadoop/PigMain.java
++ b/sharelib/pig/src/main/java/org/apache/oozie/action/hadoop/PigMain.java
@@ -95,6 +95,7 @@ public class PigMain extends LauncherMain {
         }
 
         actionConf.addResource(new Path("file:///", actionXml));
        setYarnTag(actionConf);
 
         Properties pigProperties = new Properties();
         for (Map.Entry<String, String> entry : actionConf) {
diff --git a/sharelib/spark/pom.xml b/sharelib/spark/pom.xml
index c53253271..51a4251a4 100644
-- a/sharelib/spark/pom.xml
++ b/sharelib/spark/pom.xml
@@ -49,7 +49,11 @@
             <artifactId>commons-lang</artifactId>
             <scope>compile</scope>
         </dependency>

        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-sharelib-oozie</artifactId>
            <scope>provided</scope>
        </dependency>
         <dependency>
             <groupId>org.apache.spark</groupId>
             <artifactId>spark-core_2.10</artifactId>
diff --git a/sharelib/spark/src/main/java/org.apache.oozie.action.hadoop/SparkMain.java b/sharelib/spark/src/main/java/org.apache.oozie.action.hadoop/SparkMain.java
index dcf3868cf..b18a0b935 100644
-- a/sharelib/spark/src/main/java/org.apache.oozie.action.hadoop/SparkMain.java
++ b/sharelib/spark/src/main/java/org.apache.oozie.action.hadoop/SparkMain.java
@@ -22,8 +22,6 @@ import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.spark.deploy.SparkSubmit;
 
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.List;
 
@@ -43,6 +41,7 @@ public class SparkMain extends LauncherMain {
     @Override
     protected void run(String[] args) throws Exception {
         Configuration actionConf = loadActionConf();
        setYarnTag(actionConf);
         LauncherMainHadoopUtils.killChildYarnJobs(actionConf);
 
         List<String> sparkArgs = new ArrayList<String>();
diff --git a/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java b/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java
index 1ffaf10da..6ba72383e 100644
-- a/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java
++ b/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java
@@ -60,6 +60,7 @@ public class SqoopMain extends LauncherMain {
         }
 
         sqoopConf.addResource(new Path("file:///", actionXml));
        setYarnTag(sqoopConf);
 
         String delegationToken = getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION");
         if (delegationToken != null) {
- 
2.19.1.windows.1

