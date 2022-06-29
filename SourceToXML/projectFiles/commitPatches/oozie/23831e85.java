From 23831e855aef98f7e97791d00adaf984b1b5311b Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Wed, 30 Sep 2015 14:23:04 -0700
Subject: [PATCH] OOZIE-2377 Hive2 Action should not propagate oozie.hive2.*
 properties to Beeline (rkanter)

--
 release-log.txt                                               | 1 +
 .../main/java/org/apache/oozie/action/hadoop/Hive2Main.java   | 4 ++--
 2 files changed, 3 insertions(+), 2 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index d2eee5738..1ccd43339 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2377 Hive2 Action should not propagate oozie.hive2.* properties to Beeline (rkanter)
 OOZIE-2376 Default action configs not honored if no <configuration> section in workflow (rkanter)
 OOZIE-2365 oozie fail to start when smtp password not set (rohini)
 OOZIE-2360 Spark Action fails due to missing mesos jar (rkanter)
diff --git a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
index 56f5451ca..e122608b3 100644
-- a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
++ b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
@@ -216,8 +216,8 @@ public class Hive2Main extends LauncherMain {
             arguments.add("--hiveconf");
             arguments.add("mapreduce.job.tags=" + actionConf.get(LauncherMain.MAPREDUCE_JOB_TAGS));
         }
        // Propagate "oozie.*" configs
        for (Map.Entry<String, String> oozieConfig : actionConf.getValByRegex("^oozie\\.(?!launcher).+").entrySet()) {
        // Propagate "oozie.*" configs (but not "oozie.launcher.*" nor "oozie.hive2.*")
        for (Map.Entry<String, String> oozieConfig : actionConf.getValByRegex("^oozie\\.(?!launcher|hive2).+").entrySet()) {
             arguments.add("--hiveconf");
             arguments.add(oozieConfig.getKey() + "=" + oozieConfig.getValue());
         }
- 
2.19.1.windows.1

