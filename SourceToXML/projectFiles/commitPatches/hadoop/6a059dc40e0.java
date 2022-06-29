From 6a059dc40e0020acc64d8049bf7fe6c7c0aad358 Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Tue, 16 Oct 2012 18:00:01 +0000
Subject: [PATCH] HADOOP-8922. Provide alternate JSONP output for
 JMXJsonServlet to allow javascript in browser dashboard (Damien Hardy via
 bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1398904 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 +++
 .../org/apache/hadoop/jmx/JMXJsonServlet.java | 26 +++++++++++++++++--
 .../apache/hadoop/jmx/TestJMXJsonServlet.java | 24 +++++++++++++++++
 3 files changed, 51 insertions(+), 2 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index df0585cff7e..3df1039730d 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -320,6 +320,9 @@ Release 2.0.3-alpha - Unreleased
 
     HADOOP-8929. Add toString, other improvements for SampleQuantiles (todd)
 
    HADOOP-8922. Provide alternate JSONP output for JMXJsonServlet to allow
    javascript in browser dashboard (Damien Hardy via bobby)

   OPTIMIZATIONS
 
     HADOOP-8866. SampleQuantiles#query is O(N^2) instead of O(N). (Andrew Wang
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java
index 84dc15c882f..076ffb4c231 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java
@@ -113,12 +113,17 @@
  *  All other objects will be converted to a string and output as such.
  *  
  *  The bean's name and modelerType will be returned for all beans.
 *
 *  Optional paramater "callback" should be used to deliver JSONP response.
 *  
  */
 public class JMXJsonServlet extends HttpServlet {
   private static final Log LOG = LogFactory.getLog(JMXJsonServlet.class);
 
   private static final long serialVersionUID = 1L;
 
  private static final String CALLBACK_PARAM = "callback";

   /**
    * MBean server.
    */
@@ -154,11 +159,22 @@ public void doGet(HttpServletRequest request, HttpServletResponse response) {
         return;
       }
       JsonGenerator jg = null;
      String jsonpcb = null;
      PrintWriter writer = null;
       try {
        response.setContentType("application/json; charset=utf8");
        writer = response.getWriter();
 
        // "callback" parameter implies JSONP outpout
        jsonpcb = request.getParameter(CALLBACK_PARAM);
        if (jsonpcb != null) {
          response.setContentType("application/javascript; charset=utf8");
          writer.write(jsonpcb + "(");
        } else {
          response.setContentType("application/json; charset=utf8");
        }
 
        PrintWriter writer = response.getWriter();
         jg = jsonFactory.createJsonGenerator(writer);
        jg.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
         jg.useDefaultPrettyPrinter();
         jg.writeStartObject();
 
@@ -188,6 +204,12 @@ public void doGet(HttpServletRequest request, HttpServletResponse response) {
         if (jg != null) {
           jg.close();
         }
        if (jsonpcb != null) {
           writer.write(");");
        }
        if (writer != null) {
          writer.close();
        }
       }
     } catch (IOException e) {
       LOG.error("Caught an exception while processing JMX request", e);
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java
index ea2f76bf10a..41303f41d91 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java
@@ -78,5 +78,29 @@ public static void assertReFind(String re, String value) {
         "/jmx?get=java.lang:type=Memory::"));
     LOG.info("/jmx RESULT: "+result);
     assertReFind("\"ERROR\"", result);

    // test to get JSONP result
    result = readOutput(new URL(baseUrl, "/jmx?qry=java.lang:type=Memory&callback=mycallback1"));
    LOG.info("/jmx?qry=java.lang:type=Memory&callback=mycallback RESULT: "+result);
    assertReFind("^mycallback1\\(\\{", result);
    assertReFind("\\}\\);$", result);

    // negative test to get an attribute of a mbean as JSONP
    result = readOutput(new URL(baseUrl,
        "/jmx?get=java.lang:type=Memory::&callback=mycallback2"));
    LOG.info("/jmx RESULT: "+result);
    assertReFind("^mycallback2\\(\\{", result);
    assertReFind("\"ERROR\"", result);
    assertReFind("\\}\\);$", result);

    // test to get an attribute of a mbean as JSONP
    result = readOutput(new URL(baseUrl,
        "/jmx?get=java.lang:type=Memory::HeapMemoryUsage&callback=mycallback3"));
    LOG.info("/jmx RESULT: "+result);
    assertReFind("^mycallback3\\(\\{", result);
    assertReFind("\"name\"\\s*:\\s*\"java.lang:type=Memory\"", result);
    assertReFind("\"committed\"\\s*:", result);
    assertReFind("\\}\\);$", result);

   }
 }
- 
2.19.1.windows.1

