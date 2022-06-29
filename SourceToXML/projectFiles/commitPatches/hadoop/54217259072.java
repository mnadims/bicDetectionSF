From 5421725907267d88609442c220aed3f32ccf6ad1 Mon Sep 17 00:00:00 2001
From: Daryn Sharp <daryn@apache.org>
Date: Thu, 16 Jan 2014 18:54:52 +0000
Subject: [PATCH] HADOOP-10146. Workaround JDK7 Process fd close bug (daryn)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1558883 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                  |  2 ++
 .../java/org/apache/hadoop/util/Shell.java     | 18 ++++++++++++++++--
 2 files changed, 18 insertions(+), 2 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 1b098a1dc34..2dee49b1bb5 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -526,6 +526,8 @@ Release 2.4.0 - UNRELEASED
     HADOOP-10236. Fix typo in o.a.h.ipc.Client#checkResponse. (Akira Ajisaka
     via suresh)
 
    HADOOP-10146. Workaround JDK7 Process fd close bug (daryn)

 Release 2.3.0 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
index 8013f22b97a..59c64c63583 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
@@ -21,6 +21,7 @@
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
import java.io.InputStream;
 import java.util.Arrays;
 import java.util.Map;
 import java.util.Timer;
@@ -511,7 +512,17 @@ public void run() {
       }
       // close the input stream
       try {
        inReader.close();
        // JDK 7 tries to automatically drain the input streams for us
        // when the process exits, but since close is not synchronized,
        // it creates a race if we close the stream first and the same
        // fd is recycled.  the stream draining thread will attempt to
        // drain that fd!!  it may block, OOM, or cause bizarre behavior
        // see: https://bugs.openjdk.java.net/browse/JDK-8024521
        //      issue is fixed in build 7u60
        InputStream stdout = process.getInputStream();
        synchronized (stdout) {
          inReader.close();
        }
       } catch (IOException ioe) {
         LOG.warn("Error while closing the input stream", ioe);
       }
@@ -524,7 +535,10 @@ public void run() {
         LOG.warn("Interrupted while joining errThread");
       }
       try {
        errReader.close();
        InputStream stderr = process.getErrorStream();
        synchronized (stderr) {
          errReader.close();
        }
       } catch (IOException ioe) {
         LOG.warn("Error while closing the error stream", ioe);
       }
- 
2.19.1.windows.1

