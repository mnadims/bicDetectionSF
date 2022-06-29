From 19014354dfd373ad5d05221d824bb3105e9271ac Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Wed, 19 Oct 2016 17:35:10 -0700
Subject: [PATCH] OOZIE-2705 Oozie Spark action ignores
 spark.executor.extraJavaOptions and spark.driver.extraJavaOptions (gezapeti
 via rkanter)

--
 release-log.txt                               |  1 +
 .../apache/oozie/action/hadoop/SparkMain.java | 31 ++++++++++++++++---
 2 files changed, 27 insertions(+), 5 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index e1d3d2f13..bcf2f3a6a 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -6,6 +6,7 @@ OOZIE-2634 Queue dump command message is confusing when the queue is empty (andr
 
 -- Oozie 4.3.0 release
 
OOZIE-2705 Oozie Spark action ignores spark.executor.extraJavaOptions and spark.driver.extraJavaOptions (gezapeti via rkanter)
 OOZIE-2621 Use hive-exec-<version>-core instead of hive-exec in oozie-core (gezapeti via rkanter)
 OOZIE-2613 Upgrade hive version from 0.13.1 to 1.2.0 (abhishekbafna via rkanter)
 OOZIE-2658 --driver-class-path can overwrite the classpath in SparkMain (gezapeti via rkanter)
diff --git a/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java b/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java
index 19a39a99e..0da74d4ac 100644
-- a/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java
++ b/sharelib/spark/src/main/java/org/apache/oozie/action/hadoop/SparkMain.java
@@ -52,6 +52,9 @@ public class SparkMain extends LauncherMain {
     private static final String DRIVER_CLASSPATH_OPTION = "--driver-class-path";
     private static final String EXECUTOR_CLASSPATH = "spark.executor.extraClassPath=";
     private static final String DRIVER_CLASSPATH = "spark.driver.extraClassPath=";
    private static final String EXECUTOR_EXTRA_JAVA_OPTIONS = "spark.executor.extraJavaOptions=";
    private static final String DRIVER_EXTRA_JAVA_OPTIONS = "spark.driver.extraJavaOptions=";
    private static final String LOG4J_CONFIGURATION_JAVA_OPTION = "-Dlog4j.configuration=";
     private static final String HIVE_SECURITY_TOKEN = "spark.yarn.security.tokens.hive.enabled";
     private static final String HBASE_SECURITY_TOKEN = "spark.yarn.security.tokens.hbase.enabled";
     private static final String CONF_OOZIE_SPARK_SETUP_HADOOP_CONF_DIR = "oozie.action.spark.setup.hadoop.conf.dir";
@@ -119,6 +122,8 @@ public class SparkMain extends LauncherMain {
         }
         boolean addedHiveSecurityToken = false;
         boolean addedHBaseSecurityToken = false;
        boolean addedLog4jDriverSettings = false;
        boolean addedLog4jExecutorSettings = false;
         StringBuilder driverClassPath = new StringBuilder();
         StringBuilder executorClassPath = new StringBuilder();
         String sparkOpts = actionConf.get(SparkActionExecutor.SPARK_OPTS);
@@ -150,6 +155,19 @@ public class SparkMain extends LauncherMain {
                 if (opt.startsWith(HBASE_SECURITY_TOKEN)) {
                     addedHBaseSecurityToken = true;
                 }
                if (opt.startsWith(EXECUTOR_EXTRA_JAVA_OPTIONS) || opt.startsWith(DRIVER_EXTRA_JAVA_OPTIONS)) {
                    if(!opt.contains(LOG4J_CONFIGURATION_JAVA_OPTION)) {
                        opt += " " + LOG4J_CONFIGURATION_JAVA_OPTION + SPARK_LOG4J_PROPS;
                    }else{
                        System.out.println("Warning: Spark Log4J settings are overwritten." +
                                " Child job IDs may not be available");
                    }
                    if(opt.startsWith(EXECUTOR_EXTRA_JAVA_OPTIONS)) {
                        addedLog4jExecutorSettings = true;
                    }else{
                        addedLog4jDriverSettings = true;
                    }
                }
                 if(addToSparkArgs) {
                     sparkArgs.add(opt);
                 }
@@ -169,11 +187,6 @@ public class SparkMain extends LauncherMain {
             sparkArgs.add("--conf");
             sparkArgs.add(DRIVER_CLASSPATH + driverClassPath.toString());
         }
        sparkArgs.add("--conf");
        sparkArgs.add("spark.executor.extraJavaOptions=-Dlog4j.configuration=" + SPARK_LOG4J_PROPS);

        sparkArgs.add("--conf");
        sparkArgs.add("spark.driver.extraJavaOptions=-Dlog4j.configuration=" + SPARK_LOG4J_PROPS);
 
         if (actionConf.get(MAPREDUCE_JOB_TAGS) != null) {
             sparkArgs.add("--conf");
@@ -188,6 +201,14 @@ public class SparkMain extends LauncherMain {
             sparkArgs.add("--conf");
             sparkArgs.add(HBASE_SECURITY_TOKEN + "=false");
         }
        if(!addedLog4jExecutorSettings) {
            sparkArgs.add("--conf");
            sparkArgs.add(EXECUTOR_EXTRA_JAVA_OPTIONS + LOG4J_CONFIGURATION_JAVA_OPTION + SPARK_LOG4J_PROPS);
        }
        if(!addedLog4jDriverSettings) {
            sparkArgs.add("--conf");
            sparkArgs.add(DRIVER_EXTRA_JAVA_OPTIONS + LOG4J_CONFIGURATION_JAVA_OPTION + SPARK_LOG4J_PROPS);
        }
         File defaultConfFile = getMatchingFile(SPARK_DEFAULTS_FILE_PATTERN);
         if (defaultConfFile != null) {
             sparkArgs.add("--properties-file");
- 
2.19.1.windows.1

