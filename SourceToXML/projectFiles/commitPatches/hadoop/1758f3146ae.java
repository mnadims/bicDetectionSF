From 1758f3146ae582493ca02be9babfaf24fb612613 Mon Sep 17 00:00:00 2001
From: Jason Darrell Lowe <jlowe@apache.org>
Date: Wed, 11 Jun 2014 22:05:04 +0000
Subject: [PATCH] HADOOP-10622. Shell.runCommand can deadlock. Contributed by
 Gera Shegalov

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1602033 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  2 ++
 .../java/org/apache/hadoop/util/Shell.java    | 31 +++++++++++--------
 2 files changed, 20 insertions(+), 13 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index b0104677646..8e524a487bd 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -532,6 +532,8 @@ Release 2.5.0 - UNRELEASED
     HADOOP-10656. The password keystore file is not picked by LDAP group mapping
     (brandonli)
 
    HADOOP-10622. Shell.runCommand can deadlock (Gera Shegalov via jlowe)

   BREAKDOWN OF HADOOP-10514 SUBTASKS AND RELATED JIRAS
 
     HADOOP-10520. Extended attributes definition and FileSystem APIs for
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
index 0117fe5f117..e6f24a87764 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
@@ -526,12 +526,8 @@ public void run() {
       }
       // wait for the process to finish and check the exit code
       exitCode  = process.waitFor();
      try {
        // make sure that the error thread exits
        errThread.join();
      } catch (InterruptedException ie) {
        LOG.warn("Interrupted while reading the error stream", ie);
      }
      // make sure that the error thread exits
      joinThread(errThread);
       completed.set(true);
       //the timeout thread handling
       //taken care in finally block
@@ -560,13 +556,9 @@ public void run() {
       } catch (IOException ioe) {
         LOG.warn("Error while closing the input stream", ioe);
       }
      try {
        if (!completed.get()) {
          errThread.interrupt();
          errThread.join();
        }
      } catch (InterruptedException ie) {
        LOG.warn("Interrupted while joining errThread");
      if (!completed.get()) {
        errThread.interrupt();
        joinThread(errThread);
       }
       try {
         InputStream stderr = process.getErrorStream();
@@ -581,6 +573,19 @@ public void run() {
     }
   }
 
  private static void joinThread(Thread t) {
    while (t.isAlive()) {
      try {
        t.join();
      } catch (InterruptedException ie) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Interrupted while joining on: " + t, ie);
        }
        t.interrupt(); // propagate interrupt
      }
    }
  }

   /** return an array containing the command name & its parameters */ 
   protected abstract String[] getExecString();
   
- 
2.19.1.windows.1

