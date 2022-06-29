From 3276633f3002ad7e9096c4ee5b6c329f5a708248 Mon Sep 17 00:00:00 2001
From: jvishwakarma <jvishwakarma@walmartlabs.com>
Date: Wed, 3 Aug 2016 15:36:31 +0530
Subject: [PATCH] OOZIE-2244 Oozie should mask passwords in the logs when
 logging command arguments (venkatnrangan via jaydeepvishwakarma)

--
 release-log.txt                               |  1 +
 .../apache/oozie/action/hadoop/JavaMain.java  |  5 +---
 .../oozie/action/hadoop/LauncherMapper.java   | 26 ++++++++++++++++---
 .../apache/oozie/action/hadoop/SqoopMain.java |  6 +----
 4 files changed, 25 insertions(+), 13 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index df4b1e87e..32bd26830 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2244 Oozie should mask passwords in the logs when logging command arguments (venkatnrangan via jaydeepvishwakarma)
 OOZIE-2516 Update web service documentation for jobs API (abhishekbafna via rkanter)
 OOZIE-2497 Some tests fail on windows due to hard coded URIs (abhishekbafna via rkanter)
 OOZIE-2349 Method getCoordJobInfo(String jobId, String filter, int offset, int length, boolean desc) is not present in LocalOozieClientCoord (nperiwal via rkanter)
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java
index 10a1b12f5..e4f4b4389 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/JavaMain.java
@@ -46,10 +46,7 @@ public class JavaMain extends LauncherMain {
 
         Class<?> klass = actionConf.getClass(JAVA_MAIN_CLASS, Object.class);
         System.out.println("Main class        : " + klass.getName());
        System.out.println("Arguments         :");
        for (String arg : args) {
            System.out.println("                    " + arg);
        }
        LauncherMapper.printArgs("Arguments         :", args);
         System.out.println();
         Method mainMethod = klass.getMethod("main", String[].class);
         try {
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
index 17ba97dab..727148664 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
@@ -210,10 +210,7 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
                     System.out.println("Maximum output    : "
                             + getJobConf().getInt(CONF_OOZIE_ACTION_MAX_OUTPUT_DATA, 2 * 1024));
                     System.out.println();
                    System.out.println("Arguments         :");
                    for (String arg : args) {
                        System.out.println("                    " + arg);
                    }
                    printArgs("Arguments         :", args);
 
                     System.out.println();
                     System.out.println("Java System Properties:");
@@ -613,6 +610,27 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
         }
     }
 
    /**
     * Print arguments to standard output stream. Mask out argument values to option with name 'password' in them.
     * @param banner source banner
     * @param args arguments to be printed
     */
    public static void printArgs(String banner, String[] args) {
        System.out.println(banner);
        boolean maskNextArg = false;
        for (String arg : args) {
            if (maskNextArg) {
                System.out.println("             " + "********");
                maskNextArg = false;
            }
            else {
                System.out.println("             " + arg);
                if (arg.toLowerCase().contains("password")) {
                    maskNextArg = true;
                }
            }
        }
    }
 }
 
 class LauncherSecurityManager extends SecurityManager {
diff --git a/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java b/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java
index b0c7635c5..623fd2e64 100644
-- a/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java
++ b/sharelib/sqoop/src/main/java/org/apache/oozie/action/hadoop/SqoopMain.java
@@ -163,11 +163,7 @@ public class SqoopMain extends LauncherMain {
             throw new RuntimeException("Action Configuration does not have [" + SqoopActionExecutor.SQOOP_ARGS + "] property");
         }
 
        System.out.println("Sqoop command arguments :");
        for (String arg : sqoopArgs) {
            System.out.println("             " + arg);
        }

        LauncherMapper.printArgs("Sqoop command arguments :", sqoopArgs);
         LauncherMainHadoopUtils.killChildYarnJobs(sqoopConf);
 
         System.out.println("=================================================================");
- 
2.19.1.windows.1

