From 085c5b793b057b8a28bd2bfcd96ebf03b3ecfc21 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Fri, 10 Oct 2014 17:53:44 -0400
Subject: [PATCH] ACCUMULO-3224 Use nanoTime in the shell's auth timeout

--
 .../org/apache/accumulo/core/util/shell/Shell.java   | 12 +++++++-----
 1 file changed, 7 insertions(+), 5 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
index a2834ff1c..bb3c06e95 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
@@ -36,6 +36,7 @@ import java.util.Map.Entry;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.UUID;
import java.util.concurrent.TimeUnit;
 
 import jline.ConsoleReader;
 import jline.History;
@@ -196,7 +197,7 @@ public class Shell extends ShellOptions {
   private boolean tabCompletion;
   private boolean disableAuthTimeout;
   private long authTimeout;
  private long lastUserActivity = System.currentTimeMillis();
  private long lastUserActivity = System.nanoTime();
   private boolean logErrorsToConsole = false;
   private PrintWriter writer = null;
   private boolean masking = false;
@@ -229,7 +230,7 @@ public class Shell extends ShellOptions {
       }
       
       setDebugging(cl.hasOption(debugOption.getLongOpt()));
      authTimeout = Integer.parseInt(cl.getOptionValue(authTimeoutOpt.getLongOpt(), DEFAULT_AUTH_TIMEOUT)) * 60 * 1000l;
      authTimeout = TimeUnit.MINUTES.toNanos(Integer.parseInt(cl.getOptionValue(authTimeoutOpt.getLongOpt(), DEFAULT_AUTH_TIMEOUT)));
       disableAuthTimeout = cl.hasOption(disableAuthTimeoutOpt.getLongOpt());
       
       if (cl.hasOption(zooKeeperInstance.getOpt()) && cl.getOptionValues(zooKeeperInstance.getOpt()).length != 2)
@@ -492,7 +493,7 @@ public class Shell extends ShellOptions {
     if (disableAuthTimeout)
       sb.append("- Authorization timeout: disabled\n");
     else
      sb.append("- Authorization timeout: ").append(String.format("%.2fs%n", authTimeout / 1000.0));
      sb.append("- Authorization timeout: ").append(String.format("%.2fs%n", TimeUnit.NANOSECONDS.toSeconds(authTimeout)));
     sb.append("- Debug: ").append(isDebuggingEnabled() ? "on" : "off").append("\n");
     if (!scanIteratorOptions.isEmpty()) {
       for (Entry<String,List<IteratorSetting>> entry : scanIteratorOptions.entrySet()) {
@@ -548,7 +549,8 @@ public class Shell extends ShellOptions {
           return;
         }
         
        if (!(sc instanceof ExitCommand) && !ignoreAuthTimeout && System.currentTimeMillis() - lastUserActivity > authTimeout) {
        long duration = System.nanoTime() - lastUserActivity;
        if (!(sc instanceof ExitCommand) && !ignoreAuthTimeout && (duration < 0 || duration > authTimeout)) {
           reader.printString("Shell has been idle for too long. Please re-authenticate.\n");
           boolean authFailed = true;
           do {
@@ -568,7 +570,7 @@ public class Shell extends ShellOptions {
             if (authFailed)
               reader.printString("Invalid password. ");
           } while (authFailed);
          lastUserActivity = System.currentTimeMillis();
          lastUserActivity = System.nanoTime();
         }
         
         // Get the options from the command on how to parse the string
- 
2.19.1.windows.1

