From e4dbe897cfbd710549a0368e156a49181409a671 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Mon, 27 Oct 2014 14:57:10 -0400
Subject: [PATCH] ACCUMULO-3264 Only reset logging configuration when we're
 certain that we have configuration files

The original change broke logging for all of the ITs because
the logging configuration was reset without having any means
to add back the original log4j configuration from maven.

This shouldn't have been a problem because MAC should really
be setting up logging in the same way that a real cluster does
instead of faking it.
--
 .../org/apache/accumulo/server/Accumulo.java  |  5 +++--
 .../server/watcher/Log4jConfiguration.java    | 19 +++++++++++++------
 2 files changed, 16 insertions(+), 8 deletions(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
index 0dc76b222..1e7658a20 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
++ b/server/base/src/main/java/org/apache/accumulo/server/Accumulo.java
@@ -161,11 +161,12 @@ public class Accumulo {
     // Read the auditing config
     String auditConfig = String.format("%s/auditLog.xml", System.getenv("ACCUMULO_CONF_DIR"));
 
    DOMConfigurator.configureAndWatch(auditConfig, 5000);

     // Set up local file-based logging right away
     Log4jConfiguration logConf = new Log4jConfiguration(logConfigFile);
     logConf.resetLogger();

    // Watch the auditLog.xml for the future updates
    DOMConfigurator.configureAndWatch(auditConfig, 5000);
   }
 
   public static void init(VolumeManager fs, ServerConfiguration serverConfig, String application) throws IOException {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java b/server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java
index 7dea7a323..0cac730d6 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java
++ b/server/base/src/main/java/org/apache/accumulo/server/watcher/Log4jConfiguration.java
@@ -16,6 +16,8 @@
  */
 package org.apache.accumulo.server.watcher;
 
import java.io.File;

 import org.apache.log4j.LogManager;
 import org.apache.log4j.PropertyConfigurator;
 import org.apache.log4j.xml.DOMConfigurator;
@@ -27,10 +29,12 @@ public class Log4jConfiguration {
 
   private final boolean usingProperties;
   private final String filename;
  private final File log4jFile;
 
   public Log4jConfiguration(String filename) {
     usingProperties = (filename != null && filename.endsWith(".properties"));
     this.filename = filename;
    log4jFile = new File(filename);
   }
 
   public boolean isUsingProperties() {
@@ -38,12 +42,15 @@ public class Log4jConfiguration {
   }
 
   public void resetLogger() {
    // Force a reset on the logger's configuration
    LogManager.resetConfiguration();
    if (usingProperties) {
      new PropertyConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    } else {
      new DOMConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
    // Force a reset on the logger's configuration, but only if the configured log4j file actually exists
    // If we reset the configuration blindly, the ITs will not get any logging as they don't set it up on their own
    if (log4jFile.exists() && log4jFile.isFile() && log4jFile.canRead()) {
      LogManager.resetConfiguration();
      if (usingProperties) {
        new PropertyConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
      } else {
        new DOMConfigurator().doConfigure(filename, LogManager.getLoggerRepository());
      }
     }
   }
 }
- 
2.19.1.windows.1

