From fc9aeb13ae2d5c4d62f0a1de3fee0f040ea3f343 Mon Sep 17 00:00:00 2001
From: Purshotam Shah <purushah@yahoo-inc.com>
Date: Fri, 3 Jun 2016 22:50:05 -0700
Subject: [PATCH] OOZIE-2503 show ChildJobURLs to spark action

--
 .../action/hadoop/SparkActionExecutor.java    | 24 +++++--
 release-log.txt                               |  1 +
 .../apache/oozie/action/hadoop/SparkMain.java | 69 ++++++++++++++++++-
 .../oozie/action/hadoop/TestSparkMain.java    |  7 ++
 webapp/src/main/webapp/oozie-console.js       |  3 +-
 5 files changed, 97 insertions(+), 7 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/SparkActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/SparkActionExecutor.java
index 6d37105b3..97355fde0 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/SparkActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/SparkActionExecutor.java
@@ -18,21 +18,26 @@
 
 package org.apache.oozie.action.hadoop;
 
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.service.HadoopAccessorException;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.SparkConfigurationService;
 import org.jdom.Element;
import org.jdom.JDOMException;
 import org.jdom.Namespace;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

 public class SparkActionExecutor extends JavaActionExecutor {
     public static final String SPARK_MAIN_CLASS_NAME = "org.apache.oozie.action.hadoop.SparkMain";
     public static final String TASK_USER_PRECEDENCE = "mapreduce.task.classpath.user.precedence"; // hadoop-2
@@ -155,4 +160,15 @@ public class SparkActionExecutor extends JavaActionExecutor {
         return launcherConf.get(LauncherMapper.CONF_OOZIE_ACTION_MAIN_CLASS, SPARK_MAIN_CLASS_NAME);
     }
 
    @Override
    protected void getActionData(FileSystem actionFs, RunningJob runningJob, WorkflowAction action, Context context)
            throws HadoopAccessorException, JDOMException, IOException, URISyntaxException {
        super.getActionData(actionFs, runningJob, action, context);
        readExternalChildIDs(action, context);
    }

    @Override
    protected boolean getCaptureOutput(WorkflowAction action) throws JDOMException {
        return true;
    }
 }
diff --git a/release-log.txt b/release-log.txt
index 1f13c2635..189ca2195 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2503 show ChildJobURLs to spark action (satishsaley via puru)
 OOZIE-2551 Feature request: epoch timestamp generation (jtolar via puru)
 OOZIE-2542 Option to disable OpenJPA BrokerImpl finalization (puru)
 OOZIE-2447 Illegal character 0x0 oozie client (satishsaley via puru)
diff --git a/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java b/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java
index 604f28733..0e6e2716f 100644
-- a/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java
++ b/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java
@@ -21,13 +21,18 @@ package org.apache.oozie.action.hadoop;
 import org.apache.commons.io.FileUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.PropertyConfigurator;
 import org.apache.spark.deploy.SparkSubmit;
 
 import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
import java.util.Properties;
 import java.util.regex.Pattern;
 
 public class SparkMain extends LauncherMain {
@@ -48,6 +53,9 @@ public class SparkMain extends LauncherMain {
     private String sparkJars = null;
     private String sparkClasspath = null;
 
    private static final String SPARK_LOG4J_PROPS = "spark-log4j.properties";
    private static final Pattern[] SPARK_JOB_IDS_PATTERNS = {
            Pattern.compile("Submitted application (application[0-9_]*)") };
     public static void main(String[] args) throws Exception {
         run(SparkMain.class, args);
     }
@@ -58,7 +66,7 @@ public class SparkMain extends LauncherMain {
         Configuration actionConf = loadActionConf();
         setYarnTag(actionConf);
         LauncherMainHadoopUtils.killChildYarnJobs(actionConf);

        String logFile = setUpSparkLog4J(actionConf);
         List<String> sparkArgs = new ArrayList<String>();
 
         sparkArgs.add(MASTER_OPTION);
@@ -175,6 +183,13 @@ public class SparkMain extends LauncherMain {
             sparkArgs.add("--conf");
             sparkArgs.add(DIST_FILES + sparkJars);
         }

        sparkArgs.add("--conf");
        sparkArgs.add("spark.executor.extraJavaOptions=-Dlog4j.configuration=" + SPARK_LOG4J_PROPS);

        sparkArgs.add("--conf");
        sparkArgs.add("spark.driver.extraJavaOptions=-Dlog4j.configuration=" + SPARK_LOG4J_PROPS);

         if (!addedHiveSecurityToken) {
             sparkArgs.add("--conf");
             sparkArgs.add(HIVE_SECURITY_TOKEN + "=false");
@@ -204,7 +219,12 @@ public class SparkMain extends LauncherMain {
             System.out.println("                    " + arg);
         }
         System.out.println();
        runSpark(sparkArgs.toArray(new String[sparkArgs.size()]));
        try {
            runSpark(sparkArgs.toArray(new String[sparkArgs.size()]));
        }
        finally {
            writeExternalChildIDs(logFile, SPARK_JOB_IDS_PATTERNS, "Spark");
        }
     }
 
     /**
@@ -331,4 +351,49 @@ public class SparkMain extends LauncherMain {
         }
         return result;
     }

    public static String setUpSparkLog4J(Configuration distcpConf) throws IOException {
        // Logfile to capture job IDs
        String hadoopJobId = System.getProperty("oozie.launcher.job.id");
        if (hadoopJobId == null) {
            throw new RuntimeException("Launcher Hadoop Job ID system,property not set");
        }
        String logFile = new File("spark-oozie-" + hadoopJobId + ".log").getAbsolutePath();
        Properties hadoopProps = new Properties();

        // Preparing log4j configuration
        URL log4jFile = Thread.currentThread().getContextClassLoader().getResource("log4j.properties");
        if (log4jFile != null) {
            // getting hadoop log4j configuration
            hadoopProps.load(log4jFile.openStream());
        }

        String logLevel = distcpConf.get("oozie.spark.log.level", "INFO");
        String rootLogLevel = distcpConf.get("oozie.action." + LauncherMapper.ROOT_LOGGER_LEVEL, "INFO");

        hadoopProps.setProperty("log4j.rootLogger", rootLogLevel + ", A");
        hadoopProps.setProperty("log4j.logger.org.apache.spark", logLevel + ", A, jobid");
        hadoopProps.setProperty("log4j.additivity.org.apache.spark", "false");
        hadoopProps.setProperty("log4j.appender.A", "org.apache.log4j.ConsoleAppender");
        hadoopProps.setProperty("log4j.appender.A.layout", "org.apache.log4j.PatternLayout");
        hadoopProps.setProperty("log4j.appender.A.layout.ConversionPattern", "%d [%t] %-5p %c %x - %m%n");
        hadoopProps.setProperty("log4j.appender.jobid", "org.apache.log4j.FileAppender");
        hadoopProps.setProperty("log4j.appender.jobid.file", logFile);
        hadoopProps.setProperty("log4j.appender.jobid.layout", "org.apache.log4j.PatternLayout");
        hadoopProps.setProperty("log4j.appender.jobid.layout.ConversionPattern", "%d [%t] %-5p %c %x - %m%n");
        hadoopProps.setProperty("log4j.logger.org.apache.hadoop.mapred", "INFO, jobid");
        hadoopProps.setProperty("log4j.logger.org.apache.hadoop.mapreduce.Job", "INFO, jobid");
        hadoopProps.setProperty("log4j.logger.org.apache.hadoop.yarn.client.api.impl.YarnClientImpl", "INFO, jobid");

        String localProps = new File(SPARK_LOG4J_PROPS).getAbsolutePath();
        OutputStream os1 = new FileOutputStream(localProps);
        try {
            hadoopProps.store(os1, "");
        }
        finally {
            os1.close();
        }
        PropertyConfigurator.configure(SPARK_LOG4J_PROPS);
        return logFile;
    }
 }
diff --git a/sharelib/spark/src/test/java/org/apache/oozie/action/hadoop/TestSparkMain.java b/sharelib/spark/src/test/java/org/apache/oozie/action/hadoop/TestSparkMain.java
index f3ec89954..5ef464920 100644
-- a/sharelib/spark/src/test/java/org/apache/oozie/action/hadoop/TestSparkMain.java
++ b/sharelib/spark/src/test/java/org/apache/oozie/action/hadoop/TestSparkMain.java
@@ -53,6 +53,9 @@ public class TestSparkMain extends MainTestCase {
 
         jobConf.set("mapreduce.job.tags", "" + System.currentTimeMillis());
         setSystemProperty("oozie.job.launch.time", "" + System.currentTimeMillis());
        File statsDataFile = new File(getTestCaseDir(), "statsdata.properties");
        File hadoopIdsFile = new File(getTestCaseDir(), "hadoopIds");
        File outputDataFile = new File(getTestCaseDir(), "outputdata.properties");
 
         jobConf.set(SparkActionExecutor.SPARK_MASTER, "local[*]");
         jobConf.set(SparkActionExecutor.SPARK_MODE, "client");
@@ -70,6 +73,10 @@ public class TestSparkMain extends MainTestCase {
         os.close();
 
         System.setProperty("oozie.action.conf.xml", actionXml.getAbsolutePath());
        setSystemProperty("oozie.launcher.job.id", "" + System.currentTimeMillis());
        setSystemProperty("oozie.action.stats.properties", statsDataFile.getAbsolutePath());
        setSystemProperty("oozie.action.externalChildIDs", hadoopIdsFile.getAbsolutePath());
        setSystemProperty("oozie.action.output.properties", outputDataFile.getAbsolutePath());
 
         File jarFile = IOUtils.createJar(new File(getTestCaseDir()), "test.jar", LauncherMainTester.class);
         InputStream is = new FileInputStream(jarFile);
diff --git a/webapp/src/main/webapp/oozie-console.js b/webapp/src/main/webapp/oozie-console.js
index 99dc6ce02..b35170439 100644
-- a/webapp/src/main/webapp/oozie-console.js
++ b/webapp/src/main/webapp/oozie-console.js
@@ -765,7 +765,8 @@ function jobDetailsPopup(response, request) {
                 items : urlUnit
             };
             if (actionStatus.type == "pig" || actionStatus.type == "hive" || actionStatus.type == "map-reduce"
                    || actionStatus.type == "hive2" || actionStatus.type == "sqoop" || actionStatus.type == "distcp") {
                    || actionStatus.type == "hive2" || actionStatus.type == "sqoop" || actionStatus.type == "distcp"
                    || actionStatus.type == "spark") {
                 var tabPanel = win.items.get(0);
                 tabPanel.add(childJobsItem);
             }
- 
2.19.1.windows.1

