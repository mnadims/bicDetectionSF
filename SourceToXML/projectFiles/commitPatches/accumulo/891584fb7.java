From 891584fb7dff4e6d7895f2341b9052559144ea70 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Fri, 10 Oct 2014 19:19:20 -0400
Subject: [PATCH] ACCUMULO-3229 Format long as decimal integer, not
 floating-point

--
 .../main/java/org/apache/accumulo/core/util/shell/Shell.java    | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
index bb3c06e95..fa0f5d45a 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
@@ -493,7 +493,7 @@ public class Shell extends ShellOptions {
     if (disableAuthTimeout)
       sb.append("- Authorization timeout: disabled\n");
     else
      sb.append("- Authorization timeout: ").append(String.format("%.2fs%n", TimeUnit.NANOSECONDS.toSeconds(authTimeout)));
      sb.append("- Authorization timeout: ").append(String.format("%ds%n", TimeUnit.NANOSECONDS.toSeconds(authTimeout)));
     sb.append("- Debug: ").append(isDebuggingEnabled() ? "on" : "off").append("\n");
     if (!scanIteratorOptions.isEmpty()) {
       for (Entry<String,List<IteratorSetting>> entry : scanIteratorOptions.entrySet()) {
- 
2.19.1.windows.1

