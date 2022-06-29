From 1e06299df82b98795124fe8a33578c111e744ff4 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@apache.org>
Date: Wed, 19 Aug 2015 19:00:51 -0700
Subject: [PATCH] HADOOP-12317. Applications fail on NM restart on some linux
 distro because NM container recovery declares AM container as LOST (adhoot
 via rkanter)

--
 .../hadoop-common/CHANGES.txt                 |  4 ++
 .../java/org/apache/hadoop/util/Shell.java    | 11 ++++--
 .../org/apache/hadoop/util/TestShell.java     | 39 +++++++++++++++++++
 3 files changed, 51 insertions(+), 3 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index d07adcb7b35..943dbac731c 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1063,6 +1063,10 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12322. typos in rpcmetrics.java. (Anu Engineer via
     Arpit Agarwal)
 
    HADOOP-12317. Applications fail on NM restart on some linux distro
    because NM container recovery declares AM container as LOST
    (adhoot via rkanter)

 Release 2.7.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
index ed83e8d1e5e..e4269558c30 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
@@ -212,13 +212,18 @@ static private OSType getOSType() {
   public static String[] getCheckProcessIsAliveCommand(String pid) {
     return Shell.WINDOWS ?
       new String[] { Shell.WINUTILS, "task", "isAlive", pid } :
      new String[] { "kill", "-0", isSetsidAvailable ? "-" + pid : pid };
      isSetsidAvailable ?
        new String[] { "kill", "-0", "--", "-" + pid } :
        new String[] { "kill", "-0", pid };
   }
 
   /** Return a command to send a signal to a given pid */
   public static String[] getSignalKillCommand(int code, String pid) {
    return Shell.WINDOWS ? new String[] { Shell.WINUTILS, "task", "kill", pid } :
      new String[] { "kill", "-" + code, isSetsidAvailable ? "-" + pid : pid };
    return Shell.WINDOWS ?
      new String[] { Shell.WINUTILS, "task", "kill", pid } :
      isSetsidAvailable ?
        new String[] { "kill", "-" + code, "--", "-" + pid } :
        new String[] { "kill", "-" + code, pid };
   }
 
   public static final String ENV_NAME_REGEX = "[A-Za-z_][A-Za-z0-9_]*";
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
index d9dc9ef5fe6..a96a0c82cc9 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
@@ -18,6 +18,7 @@
 package org.apache.hadoop.util;
 
 import junit.framework.TestCase;
import org.junit.Assert;
 
 import java.io.BufferedReader;
 import java.io.File;
@@ -150,6 +151,44 @@ public void testShellCommandTimerLeak() throws Exception {
     System.err.println("after: " + timersAfter);
     assertEquals(timersBefore, timersAfter);
   }

  public void testGetCheckProcessIsAliveCommand() throws Exception {
    String anyPid = "9999";
    String[] checkProcessAliveCommand = Shell.getCheckProcessIsAliveCommand(
        anyPid);

    String[] expectedCommand;

    if (Shell.WINDOWS) {
      expectedCommand =
          new String[]{ Shell.WINUTILS, "task", "isAlive", anyPid };
    } else if (Shell.isSetsidAvailable) {
      expectedCommand = new String[]{ "kill", "-0", "--", "-" + anyPid };
    } else {
      expectedCommand = new String[]{"kill", "-0", anyPid};
    }
    Assert.assertArrayEquals(expectedCommand, checkProcessAliveCommand);
  }

  public void testGetSignalKillCommand() throws Exception {
    String anyPid = "9999";
    int anySignal = 9;
    String[] checkProcessAliveCommand = Shell.getSignalKillCommand(anySignal,
        anyPid);

    String[] expectedCommand;
    if (Shell.WINDOWS) {
      expectedCommand =
          new String[]{ Shell.WINUTILS, "task", "kill", anyPid };
    } else if (Shell.isSetsidAvailable) {
      expectedCommand =
          new String[]{ "kill", "-" + anySignal, "--", "-" + anyPid };
    } else {
      expectedCommand =
          new String[]{ "kill", "-" + anySignal, anyPid };
    }
    Assert.assertArrayEquals(expectedCommand, checkProcessAliveCommand);
  }
   
 
   private void testInterval(long interval) throws IOException {
- 
2.19.1.windows.1

