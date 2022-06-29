From 72ee25896a5dd8c12fdbbda0f7e3d6857685ed23 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Mon, 31 Oct 2016 12:59:57 -0700
Subject: [PATCH] OOZIE-2536 Hadoop's cleanup of local directory in uber mode
 causing failures (satishsaley via rohini)

--
 release-log.txt                                    |  1 +
 .../hadoop/OozieLauncherOutputCommitter.java       | 14 ++++++++++++++
 2 files changed, 15 insertions(+)

diff --git a/release-log.txt b/release-log.txt
index 77cc5adee..8cb548e91 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.4.0 release (trunk - unreleased)
 
OOZIE-2536 Hadoop's cleanup of local directory in uber mode causing failures (satishsaley via rohini)
 OOZIE-1986 Add FindBugs report to pre-commit build (andras.piros via rkanter)
 OOZIE-2634 Queue dump command message is confusing when the queue is empty (andras.piros via rkanter)
 
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java
index 153019b5d..84c09bba3 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java
@@ -19,6 +19,7 @@
 
 package org.apache.oozie.action.hadoop;
 
import java.io.File;
 import java.io.IOException;
 
 import org.apache.hadoop.mapred.JobContext;
@@ -27,6 +28,19 @@ import org.apache.hadoop.mapred.TaskAttemptContext;
 
 public class OozieLauncherOutputCommitter extends OutputCommitter {
 
    public OozieLauncherOutputCommitter() {
        File propConf = new File(LauncherMapper.PROPAGATION_CONF_XML);
        if (!propConf.exists()) {
            try {
                propConf.createNewFile();
            }
            catch (IOException e) {
                System.out.println("Failed to create " + LauncherMapper.PROPAGATION_CONF_XML);
                e.printStackTrace(System.err);
            }
        }
    }

     @Override
     public void setupJob(JobContext jobContext) throws IOException {
     }
- 
2.19.1.windows.1

