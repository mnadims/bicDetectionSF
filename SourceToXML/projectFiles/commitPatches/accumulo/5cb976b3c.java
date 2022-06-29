From 5cb976b3ca0b4ecaac27b7963622c1c2f5664251 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Fri, 24 Oct 2014 22:11:19 -0400
Subject: [PATCH] ACCUMULO-3258 Consolidate "tserver" into application variable

--
 .../main/java/org/apache/accumulo/tserver/TabletServer.java | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index 23baf43f9..03fe06982 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -3662,14 +3662,14 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     try {
       SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
       ServerOpts opts = new ServerOpts();
      opts.parseArgs("tserver", args);
      String hostname = opts.getAddress();
       final String app = "tserver";
      opts.parseArgs(app, args);
      String hostname = opts.getAddress();
       Accumulo.setupLogging(app);
       final Instance instance = HdfsZooInstance.getInstance();
       ServerConfiguration conf = new ServerConfiguration(instance);
       VolumeManager fs = VolumeManagerImpl.get();
      Accumulo.init(fs, conf, "tserver");
      Accumulo.init(fs, conf, app);
       TabletServer server = new TabletServer(conf, fs);
       server.config(hostname);
       Accumulo.enableTracing(hostname, app);
- 
2.19.1.windows.1

