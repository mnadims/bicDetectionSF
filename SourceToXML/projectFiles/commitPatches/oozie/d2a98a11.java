From d2a98a119bb8e1842462bc36fce29a1e63b2c9af Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Thu, 9 Feb 2017 18:16:50 -0800
Subject: [PATCH] OOZIE-2788 Fix jobs API servlet mapping for
 EmbeddedOozieServer (abhishekbafna via rkanter)

--
 release-log.txt                               |  1 +
 .../apache/oozie/server/ServletMapper.java    | 19 +++++++++++++------
 2 files changed, 14 insertions(+), 6 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index 955771b40..32e3648c0 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.4.0 release (trunk - unreleased)
 
OOZIE-2788 Fix jobs API servlet mapping for EmbeddedOozieServer (abhishekbafna via rkanter)
 OOZIE-2778 Copy only jetty.version related server dependencies to distro (asasvari via abhishekbafna)
 OOZIE-2786 Pass Oozie workflow ID and settings to Spark application configuration (zhengxb2005 via rkanter)
 OOZIE-2790 log4j configuration is not passed to spark executors (satishsaley)
diff --git a/server/src/main/java/org/apache/oozie/server/ServletMapper.java b/server/src/main/java/org/apache/oozie/server/ServletMapper.java
index ae27ac3d5..c6d0b5b8c 100644
-- a/server/src/main/java/org/apache/oozie/server/ServletMapper.java
++ b/server/src/main/java/org/apache/oozie/server/ServletMapper.java
@@ -27,6 +27,7 @@ import org.apache.oozie.servlet.V0JobServlet;
 import org.apache.oozie.servlet.V0JobsServlet;
 import org.apache.oozie.servlet.V1AdminServlet;
 import org.apache.oozie.servlet.V1JobServlet;
import org.apache.oozie.servlet.V1JobsServlet;
 import org.apache.oozie.servlet.V2AdminServlet;
 import org.apache.oozie.servlet.V2JobServlet;
 import org.apache.oozie.servlet.V2SLAServlet;
@@ -36,12 +37,15 @@ import org.eclipse.jetty.servlet.ServletHandler;
 import org.eclipse.jetty.servlet.ServletHolder;
 import org.eclipse.jetty.servlet.ServletMapping;
 import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 import javax.servlet.Servlet;
 
 
 public class ServletMapper {
     private final WebAppContext servletContextHandler;
    private static final Logger LOG = LoggerFactory.getLogger(ServletMapper.class);
 
     @Inject
     public ServletMapper(final WebAppContext servletContextHandler) {
@@ -59,19 +63,22 @@ public class ServletMapper {
         mapServlet(CallbackServlet.class, "/callback/*");
 
         ServletHandler servletHandler = servletContextHandler.getServletHandler();
        String voJobservletName = V0JobsServlet.class.getSimpleName();
        servletHandler.addServlet(new ServletHolder(voJobservletName, new V0JobsServlet()));
        String v0JobsServletName = V0JobsServlet.class.getSimpleName();
        servletHandler.addServlet(new ServletHolder(v0JobsServletName, new V0JobsServlet()));
         ServletMapping jobServletMappingV0 = new ServletMapping();
         jobServletMappingV0.setPathSpec("/v0/jobs");
        jobServletMappingV0.setServletName(voJobservletName);
        jobServletMappingV0.setServletName(v0JobsServletName);
 
        String v1JobsServletName = V1JobsServlet.class.getSimpleName();
        servletHandler.addServlet(new ServletHolder(v1JobsServletName, new V1JobsServlet()));
         ServletMapping jobServletMappingV1 = new ServletMapping();
         jobServletMappingV1.setPathSpec("/v1/jobs");
        jobServletMappingV1.setServletName(voJobservletName);
        jobServletMappingV1.setServletName(v1JobsServletName);
 
        // v1 and v2 version for the jobs API are same.
         ServletMapping jobServletMappingV2 = new ServletMapping();
         jobServletMappingV2.setPathSpec("/v2/jobs");
        jobServletMappingV2.setServletName(voJobservletName);
        jobServletMappingV2.setServletName(v1JobsServletName);
 
         servletHandler.addServletMapping(jobServletMappingV0);
         servletHandler.addServletMapping(jobServletMappingV1);
@@ -89,7 +96,7 @@ public class ServletMapper {
         try {
             servletContextHandler.addServlet(new ServletHolder(servletClass.newInstance()), servletPath);
         } catch (final InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
         }
     }
 }
- 
2.19.1.windows.1

