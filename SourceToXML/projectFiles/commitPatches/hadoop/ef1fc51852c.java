From ef1fc51852cd2914accde4d80c2f496cd1ca042f Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Thu, 18 Dec 2014 11:33:09 -0800
Subject: [PATCH] HADOOP-11385. Prevent cross site scripting attack on
 JMXJSONServlet. Contributed by Haohui Mai.

--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../org/apache/hadoop/jmx/JMXJsonServlet.java | 46 +++++++-----------
 .../apache/hadoop/jmx/TestJMXJsonServlet.java | 48 +++++--------------
 3 files changed, 34 insertions(+), 63 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 167bf384c8e..7cbac149f63 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -614,6 +614,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11125. Remove redundant tests in TestOsSecureRandom.
     (Masanori Oyama via wheat9)
 
    HADOOP-11385. Prevent cross site scripting attack on JMXJSONServlet.
    (wheat9)

 Release 2.6.0 - 2014-11-18
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java
index f775dd71beb..9ade62f27ab 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/jmx/JMXJsonServlet.java
@@ -17,12 +17,11 @@
 
 package org.apache.hadoop.jmx;
 
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.http.HttpServer2;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
 
 import javax.management.AttributeNotFoundException;
 import javax.management.InstanceNotFoundException;
@@ -43,12 +42,12 @@
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.http.HttpServer2;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Set;
 
 /*
  * This servlet is based off of the JMXProxyServlet from Tomcat 7.0.14. It has
@@ -114,16 +113,16 @@
  *  
  *  The bean's name and modelerType will be returned for all beans.
  *
 *  Optional paramater "callback" should be used to deliver JSONP response.
 *  
  */
 public class JMXJsonServlet extends HttpServlet {
   private static final Log LOG = LogFactory.getLog(JMXJsonServlet.class);
  static final String ACCESS_CONTROL_ALLOW_METHODS =
      "Access-Control-Allow-Methods";
  static final String ACCESS_CONTROL_ALLOW_ORIGIN =
      "Access-Control-Allow-Origin";
 
   private static final long serialVersionUID = 1L;
 
  private static final String CALLBACK_PARAM = "callback";

   /**
    * MBean server.
    */
@@ -164,19 +163,13 @@ public void doGet(HttpServletRequest request, HttpServletResponse response) {
         return;
       }
       JsonGenerator jg = null;
      String jsonpcb = null;
       PrintWriter writer = null;
       try {
         writer = response.getWriter();
  
        // "callback" parameter implies JSONP outpout
        jsonpcb = request.getParameter(CALLBACK_PARAM);
        if (jsonpcb != null) {
          response.setContentType("application/javascript; charset=utf8");
          writer.write(jsonpcb + "(");
        } else {
          response.setContentType("application/json; charset=utf8");
        }
        response.setContentType("application/json; charset=utf8");
        response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET");
        response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
 
         jg = jsonFactory.createJsonGenerator(writer);
         jg.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
@@ -209,9 +202,6 @@ public void doGet(HttpServletRequest request, HttpServletResponse response) {
         if (jg != null) {
           jg.close();
         }
        if (jsonpcb != null) {
           writer.write(");");
        }
         if (writer != null) {
           writer.close();
         }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java
index a119f2c29ce..cf7014ddc6b 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/jmx/TestJMXJsonServlet.java
@@ -18,20 +18,21 @@
 package org.apache.hadoop.jmx;
 
 
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.http.HttpServer2;
 import org.apache.hadoop.http.HttpServerFunctionalTest;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.hadoop.jmx.JMXJsonServlet.ACCESS_CONTROL_ALLOW_METHODS;
import static org.apache.hadoop.jmx.JMXJsonServlet.ACCESS_CONTROL_ALLOW_ORIGIN;

 public class TestJMXJsonServlet extends HttpServerFunctionalTest {
  private   static final Log LOG = LogFactory.getLog(TestJMXJsonServlet.class);
   private static HttpServer2 server;
   private static URL baseUrl;
 
@@ -53,54 +54,31 @@ public static void assertReFind(String re, String value) {
   
   @Test public void testQuery() throws Exception {
     String result = readOutput(new URL(baseUrl, "/jmx?qry=java.lang:type=Runtime"));
    LOG.info("/jmx?qry=java.lang:type=Runtime RESULT: "+result);
     assertReFind("\"name\"\\s*:\\s*\"java.lang:type=Runtime\"", result);
     assertReFind("\"modelerType\"", result);
     
     result = readOutput(new URL(baseUrl, "/jmx?qry=java.lang:type=Memory"));
    LOG.info("/jmx?qry=java.lang:type=Memory RESULT: "+result);
     assertReFind("\"name\"\\s*:\\s*\"java.lang:type=Memory\"", result);
     assertReFind("\"modelerType\"", result);
     
     result = readOutput(new URL(baseUrl, "/jmx"));
    LOG.info("/jmx RESULT: "+result);
     assertReFind("\"name\"\\s*:\\s*\"java.lang:type=Memory\"", result);
     
     // test to get an attribute of a mbean
     result = readOutput(new URL(baseUrl, 
         "/jmx?get=java.lang:type=Memory::HeapMemoryUsage"));
    LOG.info("/jmx RESULT: "+result);
     assertReFind("\"name\"\\s*:\\s*\"java.lang:type=Memory\"", result);
     assertReFind("\"committed\"\\s*:", result);
     
     // negative test to get an attribute of a mbean
     result = readOutput(new URL(baseUrl, 
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
 
    // test to CORS headers
    HttpURLConnection conn = (HttpURLConnection)
        new URL(baseUrl, "/jmx?qry=java.lang:type=Memory").openConnection();
    assertEquals("GET", conn.getHeaderField(ACCESS_CONTROL_ALLOW_METHODS));
    assertNotNull(conn.getHeaderField(ACCESS_CONTROL_ALLOW_ORIGIN));
   }
 }
- 
2.19.1.windows.1

