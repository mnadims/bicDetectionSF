From be646246ba8add6bacf025c448f1e9511e475c2c Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <ecn@apache.org>
Date: Wed, 20 Mar 2013 14:12:33 +0000
Subject: [PATCH] ACCUMULO-1066 applying patch from Tim Reardon to display
 auths in sorted order

git-svn-id: https://svn.apache.org/repos/asf/accumulo/trunk@1458842 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/util/shell/commands/GetAuthsCommand.java    | 12 +++++++++++-
 .../apache/accumulo/core/util/shell/ShellTest.java   |  6 +++---
 2 files changed, 14 insertions(+), 4 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java b/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java
index 088670750..67146ada3 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java
@@ -17,14 +17,18 @@
 package org.apache.accumulo.core.util.shell.commands;
 
 import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.shell.Shell;
 import org.apache.accumulo.core.util.shell.Shell.Command;
 import org.apache.commons.cli.CommandLine;
 import org.apache.commons.cli.Option;
 import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
 
 public class GetAuthsCommand extends Command {
   private Option userOpt;
@@ -32,7 +36,13 @@ public class GetAuthsCommand extends Command {
   @Override
   public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws AccumuloException, AccumuloSecurityException, IOException {
     final String user = cl.getOptionValue(userOpt.getOpt(), shellState.getConnector().whoami());
    shellState.getReader().printString(shellState.getConnector().securityOperations().getUserAuthorizations(user) + "\n");
    // Sort authorizations
    Authorizations auths = shellState.getConnector().securityOperations().getUserAuthorizations(user);
    SortedSet<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    for (byte[] auth : auths) {
      set.add(new String(auth));
    }
    shellState.getReader().printString(StringUtils.join(set, ',') + "\n");
     return 0;
   }
   
diff --git a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellTest.java b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellTest.java
index 19286a5c7..93ccc8ed7 100644
-- a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellTest.java
++ b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellTest.java
@@ -149,13 +149,13 @@ public class ShellTest {
     Shell.log.debug("Starting auths test --------------------------");
     exec("setauths x,y,z", false, "Missing required option");
     exec("setauths -s x,y,z -u notauser", false, "user does not exist");
    exec("setauths -s x,y,z", true);
    exec("setauths -s y,z,x", true);
     exec("getauths -u notauser", false,"user does not exist");
    exec("getauths", true,"y,z,x");
    exec("getauths", true,"x,y,z");
     exec("addauths -u notauser", false,"Missing required option");
     exec("addauths -u notauser -s foo", false,"user does not exist");
     exec("addauths -s a", true);
    exec("getauths", true, "y,z,a,x");
    exec("getauths", true, "a,x,y,z");
     exec("setauths -c", true);
   }
   
- 
2.19.1.windows.1

