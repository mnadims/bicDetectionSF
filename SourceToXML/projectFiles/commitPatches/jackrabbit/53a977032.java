From 53a97703223095343c727e89f9d97b203268ddca Mon Sep 17 00:00:00 2001
From: =?UTF-8?q?Dominique=20J=C3=A4ggi?= <dj@apache.org>
Date: Wed, 17 Aug 2016 09:55:14 +0000
Subject: [PATCH] JCR-4002 : CSRF in Jackrabbit-Webdav using empty content-type

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1756604 13f79535-47bb-0310-9956-ffa450edef68
--
 .../webdav/server/AbstractWebdavServlet.java  |   3 +-
 .../jackrabbit/webdav/util/CSRFUtil.java      |  34 ++----
 .../jackrabbit/webdav/server/PostTest.java    |  71 ++++++++++++
 .../webdav/server/WebdavServerTests.java      |   1 +
 .../jackrabbit/webdav/util/CSRFUtilTest.java  | 107 ++++++------------
 5 files changed, 117 insertions(+), 99 deletions(-)
 create mode 100644 jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/PostTest.java

diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/server/AbstractWebdavServlet.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/server/AbstractWebdavServlet.java
index 5f402b3d0..c4c2ad63d 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/server/AbstractWebdavServlet.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/server/AbstractWebdavServlet.java
@@ -596,7 +596,7 @@ abstract public class AbstractWebdavServlet extends HttpServlet implements DavCo
      */
     protected void doPost(WebdavRequest request, WebdavResponse response,
                           DavResource resource) throws IOException, DavException {
        doPut(request, response, resource);
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
     }
 
     /**
@@ -1384,7 +1384,6 @@ abstract public class AbstractWebdavServlet extends HttpServlet implements DavCo
      * @param out
      * @return
      * @see #doPut(WebdavRequest, WebdavResponse, DavResource)
     * @see #doPost(WebdavRequest, WebdavResponse, DavResource)
      * @see #doMkCol(WebdavRequest, WebdavResponse, DavResource)
      */
     protected OutputContext getOutputContext(DavServletResponse response, OutputStream out) {
diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
index 07b2c0172..4d431eba4 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
@@ -16,14 +16,12 @@
  */
 package org.apache.jackrabbit.webdav.util;
 
import org.apache.jackrabbit.webdav.DavMethods;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.servlet.http.HttpServletRequest;
 import java.net.MalformedURLException;
 import java.net.URL;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Set;
@@ -38,20 +36,6 @@ public class CSRFUtil {
      */
     public static final String DISABLED = "disabled";
 
    /**
     * Request content types for CSRF checking, see JCR-3909
     */
    public static final Set<String> CONTENT_TYPES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(
                    new String[] {
                            "application/x-www-form-urlencoded",
                            "multipart/form-data",
                            "text/plain",
                            null // no content type included in request
                    }
            )
    ));

     /**
      * logger instance
      */
@@ -109,21 +93,19 @@ public class CSRFUtil {
     }
 
     public boolean isValidRequest(HttpServletRequest request) throws MalformedURLException {
        int methodCode = DavMethods.getMethodCode(request.getMethod());
        if (disabled || DavMethods.DAV_POST != methodCode || !CONTENT_TYPES.contains(request.getContentType())) {
        if (disabled) {
             return true;
         } else {

             String refHeader = request.getHeader("Referer");
            // empty referrer headers are not allowed for POST + relevant content types (see JCR-3909)
             if (refHeader == null) {
                return false;
                // empty referrer is always allowed
                return true;
            } else {
                String host = new URL(refHeader).getHost();
                // test referrer-host equelst server or
                // if it is contained in the set of explicitly allowed host names
                return host.equals(request.getServerName()) || allowedReferrerHosts.contains(host);
             }

            String host = new URL(refHeader).getHost();
            // test referrer-host equals server or
            // if it is contained in the set of explicitly allowed host names
            return host.equals(request.getServerName()) || allowedReferrerHosts.contains(host);
         }
     }
 }
\ No newline at end of file
diff --git a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/PostTest.java b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/PostTest.java
new file mode 100644
index 000000000..839fbb40c
-- /dev/null
++ b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/PostTest.java
@@ -0,0 +1,71 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.server;

import junit.framework.TestCase;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.net.URI;

/**
 * Test cases for the WebDAV servlet not executing the POST method (CSRF protection).
 * <p>
 * Required system properties:
 * <ul>
 * <li>webdav.test.url</li>
 * <li>webdav.test.username</li>
 * <li>webdav.test.password</li>
 * </ul>
 */

public class PostTest extends TestCase {

    private String root;
    private URI uri;
    private String username, password;
    private HttpClient client;

    protected void setUp() throws Exception {
        this.uri = URI.create(System.getProperty("webdav.test.url"));
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }
        this.username = System.getProperty(("webdav.test.username"), "");
        this.password = System.getProperty(("webdav.test.password"), "");
        this.client = new HttpClient();
        this.client.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPostDenied() throws HttpException, IOException {
        PostMethod options = new PostMethod(this.uri.toASCIIString());
        int status = this.client.executeMethod(options);
        assertEquals(405, status);
    }
}
diff --git a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/WebdavServerTests.java b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/WebdavServerTests.java
index ea3a60df2..7be4b84c5 100644
-- a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/WebdavServerTests.java
++ b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/server/WebdavServerTests.java
@@ -26,6 +26,7 @@ public class WebdavServerTests extends TestCase {
         TestSuite suite = new TestSuite("WebDAV Server Tests");
 
         suite.addTestSuite(BindTest.class);
        suite.addTestSuite(PostTest.class);
         suite.addTestSuite(RFC4918DestinationHeaderTest.class);
         suite.addTestSuite(RFC4918IfHeaderTest.class);
         suite.addTestSuite(RFC4918PropfindTest.class);
diff --git a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
index 23bf7145a..ea85face7 100644
-- a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
++ b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
@@ -29,14 +29,11 @@ import java.io.UnsupportedEncodingException;
 import java.net.MalformedURLException;
 import java.security.Principal;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collection;
 import java.util.Enumeration;
import java.util.HashSet;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
import java.util.Set;
 
 /**
  * <code>CSRFUtilTest</code>...
@@ -45,85 +42,64 @@ public class CSRFUtilTest extends TestCase {
 
     private static final String SERVER_NAME = "localhost";
 
    private static final String GET = "GET";
    private static final String POST = "POST";

    private static final Set<String> EMPTY_REFERER = new HashSet<String>() {{ add(null); }};

     private static final List<String> validURLs = new ArrayList<String>();
    private static final List<String> invalidURLs = new ArrayList<String>();
 
     static {
        validURLs.add(null);
         validURLs.add("http://localhost:4503/jackrabbit/server");
         validURLs.add("https://localhost:4503/jackrabbit/server");
         validURLs.add("https://localhost/jackrabbit/server");

        invalidURLs.add("http://invalidHost/test");
        invalidURLs.add("http://host1:8080/test");
        invalidURLs.add("http://user:pw@host2/test");
     }
 
    private static void testValid(CSRFUtil util, Collection<String> validURLs, String method, Set<String> contentTypes) throws MalformedURLException {
        if (null == contentTypes) {
            for (String url : validURLs) {
                assertTrue(url, util.isValidRequest(createRequest(url, method, null)));
            }
        } else {
            for (String contentType : contentTypes) {
                for (String url : validURLs) {
                    assertTrue(url, util.isValidRequest(createRequest(url, method, contentType)));
                }
            }
    private static void testValid(CSRFUtil util, Collection<String> validURLs) throws MalformedURLException {
        for (String url : validURLs) {
            assertTrue(url, util.isValidRequest(createRequest(url)));
         }
     }
 
    private static void testInvalid(CSRFUtil util, Collection<String> invalidURLs, String method, Set<String> contentTypes) throws MalformedURLException {
        if (null == contentTypes) {
            for (String url : validURLs) {
                assertFalse(url, util.isValidRequest(createRequest(url, method, null)));
            }
        } else {
            for (String contentType : contentTypes) {
                for (String url : invalidURLs) {
                    assertFalse(url, util.isValidRequest(createRequest(url, method, contentType)));
                }
            }
    private static void testInvalid(CSRFUtil util, Collection<String> invalidURLs) throws MalformedURLException {
        for (String url : invalidURLs) {
            assertFalse(url, util.isValidRequest(createRequest(url)));
         }
     }
 
    private static HttpServletRequest createRequest(String url, String method, String contentType) {
        return new DummyRequest(url, SERVER_NAME, method, contentType);
    private static HttpServletRequest createRequest(String url) {
        return new DummyRequest(url, SERVER_NAME);
     }
 
     public void testNullConfig() throws Exception {
         CSRFUtil util = new CSRFUtil(null);
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
    }
 
    public void testEmptyConfig() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
    }
        testValid(util, validURLs);
 
    public void testNoReferrer() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, "text/plain")));
        List<String> invalidURLs = new ArrayList<String>();
        invalidURLs.add("http://invalidHost/test");
        invalidURLs.add("http://host1:8080/test");
        invalidURLs.add("http://user:pw@host2/test");
        testInvalid(util, invalidURLs);
     }
 
    public void testNoContentType() throws Exception {
    public void testEmptyConfig() throws Exception {
         CSRFUtil util = new CSRFUtil("");
        testValid(util, validURLs, POST, EMPTY_REFERER);
        testInvalid(util, invalidURLs, POST, EMPTY_REFERER);
        testValid(util, validURLs);

        List<String> invalidURLs = new ArrayList<String>();
        invalidURLs.add("http://invalidHost/test");
        invalidURLs.add("http://host1:8080/test");
        invalidURLs.add("http://user:pw@host2/test");
        testInvalid(util, invalidURLs);
     }
 
     public void testDisabledConfig() throws Exception {
         CSRFUtil util = new CSRFUtil(CSRFUtil.DISABLED);
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        testValid(util, validURLs);

         // since test is disabled any other referer host must be allowed
        testValid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
        List<String> otherHosts = new ArrayList<String>();
        otherHosts.add("http://validHost:80/test");
        otherHosts.add("http://host1/test");
        otherHosts.add("https://user:pw@host2/test");
        testValid(util, otherHosts);
     }
 
     public void testConfig() throws Exception {
@@ -145,31 +121,20 @@ public class CSRFUtilTest extends TestCase {
 
         for (String config : configs) {
             CSRFUtil util = new CSRFUtil(config);
            testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
            testValid(util, otherHosts, POST, CSRFUtil.CONTENT_TYPES);
            testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
            testValid(util, validURLs);
            testValid(util, otherHosts);
            testInvalid(util, invalidURLs);
         }
     }
 
    public void testMethodsAndMediaType() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, invalidURLs, GET, CSRFUtil.CONTENT_TYPES);
        testValid(util, invalidURLs, POST, new HashSet<String>(Arrays.asList(new String[] {"application/json"})));
        testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
    }

     private static final class DummyRequest implements HttpServletRequest {
 
         private final String referer;
         private final String serverName;
        private final String method;
        private final String contentType;
 
        private DummyRequest(String referer, String serverName, String method, String contentType) {
        private DummyRequest(String referer, String serverName) {
             this.referer = referer;
             this.serverName = serverName;
            this.method = method;
            this.contentType = contentType;
         }
 
         //---------------------------------------------< HttpServletRequest >---
@@ -206,7 +171,7 @@ public class CSRFUtilTest extends TestCase {
             return 0;
         }
         public String getMethod() {
            return method;
            return null;
         }
         public String getPathInfo() {
             return null;
@@ -275,7 +240,7 @@ public class CSRFUtilTest extends TestCase {
             return 0;
         }
         public String getContentType() {
            return contentType;
            return null;
         }
         public ServletInputStream getInputStream() throws IOException {
             return null;
- 
2.19.1.windows.1

