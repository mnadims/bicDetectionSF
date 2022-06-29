From f1c19b9365cbac88e45a9eed516fbfc6c9aa9947 Mon Sep 17 00:00:00 2001
From: Vinod Kumar Vavilapalli <vinodkv@apache.org>
Date: Mon, 5 Oct 2015 10:56:55 -0700
Subject: [PATCH] HADOOP-12441. Fixed shell-kill command behaviour to work
 correctly on some Linux distributions after HADOOP-12317. Contributed by
 Wangda Tan.

--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../java/org/apache/hadoop/util/Shell.java    | 46 +++++++++++++++----
 .../org/apache/hadoop/util/TestShell.java     | 13 +++---
 .../yarn/server/nodemanager/NodeManager.java  | 12 +++++
 4 files changed, 57 insertions(+), 17 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index e7252722d77..ce038ae5296 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1185,6 +1185,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12442. Display help if the command option to 'hdfs dfs' is not valid
     (nijel via vinayakumarb)
 
    HADOOP-12441. Fixed shell-kill command behaviour to work correctly on some
    Linux distributions after HADOOP-12317. (Wangda Tan via vinodkv)

 Release 2.7.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
index e4269558c30..ca70ef30fd6 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
@@ -210,20 +210,26 @@ static private OSType getOSType() {
 
   /** Return a command for determining if process with specified pid is alive. */
   public static String[] getCheckProcessIsAliveCommand(String pid) {
    return Shell.WINDOWS ?
      new String[] { Shell.WINUTILS, "task", "isAlive", pid } :
      isSetsidAvailable ?
        new String[] { "kill", "-0", "--", "-" + pid } :
        new String[] { "kill", "-0", pid };
    return getSignalKillCommand(0, pid);
   }
 
   /** Return a command to send a signal to a given pid */
   public static String[] getSignalKillCommand(int code, String pid) {
    return Shell.WINDOWS ?
      new String[] { Shell.WINUTILS, "task", "kill", pid } :
      isSetsidAvailable ?
        new String[] { "kill", "-" + code, "--", "-" + pid } :
        new String[] { "kill", "-" + code, pid };
    // Code == 0 means check alive
    if (Shell.WINDOWS) {
      if (0 == code) {
        return new String[] { Shell.WINUTILS, "task", "isAlive", pid };
      } else {
        return new String[] { Shell.WINUTILS, "task", "kill", pid };
      }
    }

    if (isSetsidAvailable) {
      // Use the shell-builtin as it support "--" in all Hadoop supported OSes
      return new String[] { "bash", "-c", "kill -" + code + " -- -" + pid };
    } else {
      return new String[] { "bash", "-c", "kill -" + code + " " + pid };
    }
   }
 
   public static final String ENV_NAME_REGEX = "[A-Za-z_][A-Za-z0-9_]*";
@@ -386,6 +392,26 @@ public static final String getWinUtilsPath() {
     return winUtilsPath;
   }
 
  public static final boolean isBashSupported = checkIsBashSupported();
  private static boolean checkIsBashSupported() {
    if (Shell.WINDOWS) {
      return false;
    }

    ShellCommandExecutor shexec;
    boolean supported = true;
    try {
      String[] args = {"bash", "-c", "echo 1000"};
      shexec = new ShellCommandExecutor(args);
      shexec.execute();
    } catch (IOException ioe) {
      LOG.warn("Bash is not supported by the OS", ioe);
      supported = false;
    }

    return supported;
  }

   public static final boolean isSetsidAvailable = isSetsidSupported();
   private static boolean isSetsidSupported() {
     if (Shell.WINDOWS) {
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
index a96a0c82cc9..fc202da6cf7 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
@@ -163,9 +163,9 @@ public void testGetCheckProcessIsAliveCommand() throws Exception {
       expectedCommand =
           new String[]{ Shell.WINUTILS, "task", "isAlive", anyPid };
     } else if (Shell.isSetsidAvailable) {
      expectedCommand = new String[]{ "kill", "-0", "--", "-" + anyPid };
      expectedCommand = new String[] { "bash", "-c", "kill -0 -- -" + anyPid };
     } else {
      expectedCommand = new String[]{"kill", "-0", anyPid};
      expectedCommand = new String[]{ "bash", "-c", "kill -0 " + anyPid };
     }
     Assert.assertArrayEquals(expectedCommand, checkProcessAliveCommand);
   }
@@ -177,15 +177,14 @@ public void testGetSignalKillCommand() throws Exception {
         anyPid);
 
     String[] expectedCommand;

     if (Shell.WINDOWS) {
       expectedCommand =
          new String[]{ Shell.WINUTILS, "task", "kill", anyPid };
          new String[]{ Shell.WINUTILS, "task", "isAlive", anyPid };
     } else if (Shell.isSetsidAvailable) {
      expectedCommand =
          new String[]{ "kill", "-" + anySignal, "--", "-" + anyPid };
      expectedCommand = new String[] { "bash", "-c", "kill -9 -- -" + anyPid };
     } else {
      expectedCommand =
          new String[]{ "kill", "-" + anySignal, anyPid };
      expectedCommand = new String[]{ "bash", "-c", "kill -9 " + anyPid };
     }
     Assert.assertArrayEquals(expectedCommand, checkProcessAliveCommand);
   }
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/NodeManager.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/NodeManager.java
index 184f4891309..a0f73309280 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/NodeManager.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/NodeManager.java
@@ -43,6 +43,7 @@
 import org.apache.hadoop.util.JvmPauseMonitor;
 import org.apache.hadoop.util.NodeHealthScriptRunner;
 import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.Shell;
 import org.apache.hadoop.util.ShutdownHookManager;
 import org.apache.hadoop.util.StringUtils;
 import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
@@ -595,6 +596,17 @@ public NodeHealthCheckerService getNodeHealthChecker() {
 
   private void initAndStartNodeManager(Configuration conf, boolean hasToReboot) {
     try {
      // Failed to start if we're a Unix based system but we don't have bash.
      // Bash is necessary to launch containers under Unix-based systems.
      if (!Shell.WINDOWS) {
        if (!Shell.isBashSupported) {
          String message =
              "Failing NodeManager start since we're on a "
                  + "Unix-based system but bash doesn't seem to be available.";
          LOG.fatal(message);
          throw new YarnRuntimeException(message);
        }
      }
 
       // Remove the old hook if we are rebooting.
       if (hasToReboot && null != nodeManagerShutdownHook) {
- 
2.19.1.windows.1

