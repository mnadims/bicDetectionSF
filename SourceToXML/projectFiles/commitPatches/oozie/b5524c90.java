From b5524c901b9a4b0607647f4047b0e7ff8a07a3d7 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Wed, 29 Jul 2015 09:25:03 -0700
Subject: [PATCH] OOZIE-2320
 TestZKXLogStreamingService.testStreamingWithMultipleOozieServers_coordActionList
 is failing (rkanter)

--
 .../org/apache/oozie/service/DummyLogStreamingServlet.java   | 5 +++--
 release-log.txt                                              | 1 +
 2 files changed, 4 insertions(+), 2 deletions(-)

diff --git a/core/src/test/java/org/apache/oozie/service/DummyLogStreamingServlet.java b/core/src/test/java/org/apache/oozie/service/DummyLogStreamingServlet.java
index 455caa132..bbc816ed9 100644
-- a/core/src/test/java/org/apache/oozie/service/DummyLogStreamingServlet.java
++ b/core/src/test/java/org/apache/oozie/service/DummyLogStreamingServlet.java
@@ -20,13 +20,14 @@ package org.apache.oozie.service;
 
 import java.io.IOException;
 import java.io.Writer;
import java.net.URLDecoder;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 /**
 * Used by {@link TestZKXLogStreamingService#testStreamingWithMultipleOozieServers() } to stream logs from another Oozie "server".
 * Used by {@link TestZKXLogStreamingService} to stream logs from another Oozie "server".
  * Due to the way the servlet gets loaded, it has to be its own class instead of an inner class.
  */
 public class DummyLogStreamingServlet extends HttpServlet {
@@ -36,7 +37,7 @@ public class DummyLogStreamingServlet extends HttpServlet {
 
     @Override
     protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        lastQueryString = request.getQueryString();
        lastQueryString = URLDecoder.decode(request.getQueryString(), "UTF-8");
         response.setStatus(HttpServletResponse.SC_OK);
         Writer writer = response.getWriter();
         writer.append(logs);
diff --git a/release-log.txt b/release-log.txt
index 41467b0f0..2e1586cf2 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2320 TestZKXLogStreamingService.testStreamingWithMultipleOozieServers_coordActionList is failing (rkanter)
 OOZIE-2293 Oozie 4.0.1 build failed while building Catalog (raviprak via shwethags)
 OOZIE-2308 Add support for bundle:conf() function (kailongs via rohini)
 OOZIE-2315 TestOozieCLI.testshareLibUpdate_withSecurity fails with Hadoop 2 (rkanter)
- 
2.19.1.windows.1

