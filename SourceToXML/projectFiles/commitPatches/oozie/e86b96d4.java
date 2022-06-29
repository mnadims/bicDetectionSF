From e86b96d45cc7af706a0e0f5bd1bf36d760266c8e Mon Sep 17 00:00:00 2001
From: Shwetha GS <sshivalingamurthy@hortonworks.com>
Date: Mon, 27 Apr 2015 15:10:06 +0530
Subject: [PATCH] OOZIE-2129 Duplicate child jobs per instance - fixed job
 failure

--
 .../org/apache/oozie/action/hadoop/LauncherMapperHelper.java  | 2 +-
 .../apache/oozie/action/hadoop/LauncherMainHadoopUtils.java   | 3 +++
 .../apache/oozie/action/hadoop/LauncherMainHadoopUtils.java   | 3 +++
 .../apache/oozie/action/hadoop/LauncherMainHadoopUtils.java   | 4 +++-
 .../apache/oozie/action/hadoop/LauncherMainHadoopUtils.java   | 1 +
 .../java/org/apache/oozie/action/hadoop/LauncherMapper.java   | 3 +++
 6 files changed, 14 insertions(+), 2 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java b/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
index 6a93232b5..e22329de5 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
@@ -164,7 +164,7 @@ public class LauncherMapperHelper {
 
     public static void setupYarnRestartHandling(JobConf launcherJobConf, Configuration actionConf, String launcherTag)
             throws NoSuchAlgorithmException {
        launcherJobConf.setLong("oozie.job.launch.time", System.currentTimeMillis());
        launcherJobConf.setLong(LauncherMainHadoopUtils.OOZIE_JOB_LAUNCH_TIME, System.currentTimeMillis());
         // Tags are limited to 100 chars so we need to hash them to make sure (the actionId otherwise doesn't have a max length)
         String tag = getTag(launcherTag);
         // keeping the oozie.child.mapreduce.job.tags instead of mapreduce.job.tags to avoid killing launcher itself.
diff --git a/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index 9e34d0bb2..dca7820c9 100644
-- a/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-0.23/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -23,6 +23,9 @@ import org.apache.hadoop.conf.Configuration;
 
 public class LauncherMainHadoopUtils {
 
    public static final String CHILD_MAPREDUCE_JOB_TAGS = "oozie.child.mapreduce.job.tags";
    public static final String OOZIE_JOB_LAUNCH_TIME = "oozie.job.launch.time";

     private LauncherMainHadoopUtils() {
     }
 
diff --git a/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index 9e34d0bb2..dca7820c9 100644
-- a/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-1/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -23,6 +23,9 @@ import org.apache.hadoop.conf.Configuration;
 
 public class LauncherMainHadoopUtils {
 
    public static final String CHILD_MAPREDUCE_JOB_TAGS = "oozie.child.mapreduce.job.tags";
    public static final String OOZIE_JOB_LAUNCH_TIME = "oozie.job.launch.time";

     private LauncherMainHadoopUtils() {
     }
 
diff --git a/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index 9331c130d..f6eda73d9 100644
-- a/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-2/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -19,6 +19,7 @@
 package org.apache.oozie.action.hadoop;
 
 import java.io.IOException;
import java.lang.String;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
@@ -39,6 +40,7 @@ import org.apache.hadoop.yarn.exceptions.YarnException;
 public class LauncherMainHadoopUtils {
 
     public static final String CHILD_MAPREDUCE_JOB_TAGS = "oozie.child.mapreduce.job.tags";
    public static final String OOZIE_JOB_LAUNCH_TIME = "oozie.job.launch.time";
 
     private LauncherMainHadoopUtils() {
     }
@@ -47,7 +49,7 @@ public class LauncherMainHadoopUtils {
         System.out.println("Fetching child yarn jobs");
         long startTime = 0L;
         try {
            startTime = Long.parseLong((System.getProperty("oozie.job.launch.time")));
            startTime = Long.parseLong(System.getProperty(OOZIE_JOB_LAUNCH_TIME));
         } catch(NumberFormatException nfe) {
             throw new RuntimeException("Could not find Oozie job launch time", nfe);
         }
diff --git a/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java b/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
index 211ba09fe..102a6c915 100644
-- a/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
++ b/hadooplibs/hadoop-utils-3/src/main/java/org/apache/oozie/action/hadoop/LauncherMainHadoopUtils.java
@@ -37,6 +37,7 @@ import org.apache.hadoop.yarn.exceptions.YarnException;
 public class LauncherMainHadoopUtils {
 
     public static final String CHILD_MAPREDUCE_JOB_TAGS = "oozie.child.mapreduce.job.tags";
    public static final String OOZIE_JOB_LAUNCH_TIME = "oozie.job.launch.time";
 
     private LauncherMainHadoopUtils() {
     }
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
index fe3897635..7c4d48d86 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
@@ -469,6 +469,9 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
         System.setProperty(ACTION_PREFIX + ACTION_DATA_NEW_ID, new File(ACTION_DATA_NEW_ID).getAbsolutePath());
         System.setProperty(ACTION_PREFIX + ACTION_DATA_OUTPUT_PROPS, new File(ACTION_DATA_OUTPUT_PROPS).getAbsolutePath());
         System.setProperty(ACTION_PREFIX + ACTION_DATA_ERROR_PROPS, new File(ACTION_DATA_ERROR_PROPS).getAbsolutePath());
        System.setProperty(LauncherMainHadoopUtils.OOZIE_JOB_LAUNCH_TIME,
                getJobConf().get(LauncherMainHadoopUtils.OOZIE_JOB_LAUNCH_TIME));

         String actionConfigClass = getJobConf().get(OOZIE_ACTION_CONFIG_CLASS);
         if (actionConfigClass != null) {
             System.setProperty(OOZIE_ACTION_CONFIG_CLASS, actionConfigClass);
- 
2.19.1.windows.1

