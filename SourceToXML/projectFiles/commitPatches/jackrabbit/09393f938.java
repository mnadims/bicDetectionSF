From 09393f93862923e4c8a2f8c7d1236e1a5d3373b5 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Wed, 31 Aug 2016 13:15:24 +0000
Subject: [PATCH] JCR-4009: CSRF in Jackrabbit-Webdav

CSRFUtil: properly parse content types (handle params, normalize, handle case differences also multiple field instances), handle missing content type header field, handle partial-URI in referer, DEBUG logging

WebDAV servlet: disable bogus POST support

Davex: include Referer header field in POST requests used for davex remoting

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1758600 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/spi2davex/PostMethod.java      |  1 +
 .../apache/jackrabbit/webdav/DavResource.java |  2 +-
 .../webdav/server/AbstractWebdavServlet.java  |  3 +-
 .../jackrabbit/webdav/util/CSRFUtil.java      | 72 ++++++++++++++-----
 .../jackrabbit/webdav/util/CSRFUtilTest.java  | 64 +++++++++++------
 5 files changed, 101 insertions(+), 41 deletions(-)

diff --git a/jackrabbit-spi2dav/src/main/java/org/apache/jackrabbit/spi2davex/PostMethod.java b/jackrabbit-spi2dav/src/main/java/org/apache/jackrabbit/spi2davex/PostMethod.java
index 5355a72e9..f6e243ca2 100644
-- a/jackrabbit-spi2dav/src/main/java/org/apache/jackrabbit/spi2davex/PostMethod.java
++ b/jackrabbit-spi2dav/src/main/java/org/apache/jackrabbit/spi2davex/PostMethod.java
@@ -47,6 +47,7 @@ class PostMethod extends DavMethodBase {
 
     public PostMethod(String uri) {
         super(uri);
        super.setRequestHeader("Referer", uri);
         HttpMethodParams params = getParams();
         params.setContentCharset("UTF-8");
     }
diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/DavResource.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/DavResource.java
index c99b5cd21..6e70a4206 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/DavResource.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/DavResource.java
@@ -40,7 +40,7 @@ public interface DavResource {
     /**
      * String constant representing the WebDAV 1 and 2 method set.
      */
    public static final String METHODS = "OPTIONS, GET, HEAD, POST, TRACE, PROPFIND, PROPPATCH, MKCOL, COPY, PUT, DELETE, MOVE, LOCK, UNLOCK";
    public static final String METHODS = "OPTIONS, GET, HEAD, TRACE, PROPFIND, PROPPATCH, MKCOL, COPY, PUT, DELETE, MOVE, LOCK, UNLOCK";
 
     /**
      * Returns a comma separated list of all compliance classes the given
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
index bc1de76ff..b22b7ca11 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
@@ -16,18 +16,20 @@
  */
 package org.apache.jackrabbit.webdav.util;
 
import org.apache.jackrabbit.webdav.DavMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
 import java.util.Arrays;
 import java.util.Collections;
import java.util.Enumeration;
 import java.util.HashSet;
import java.util.Locale;
 import java.util.Set;
 
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 /**
  * <code>CSRFUtil</code>...
  */
@@ -39,7 +41,7 @@ public class CSRFUtil {
     public static final String DISABLED = "disabled";
 
     /**
     * Request content types for CSRF checking, see JCR-3909
     * Request content types for CSRF checking, see JCR-3909, JCR-4002, and JCR-4009
      */
     public static final Set<String> CONTENT_TYPES = Collections.unmodifiableSet(new HashSet<String>(
             Arrays.asList(
@@ -92,6 +94,7 @@ public class CSRFUtil {
         if (config == null || config.length() == 0) {
             disabled = false;
             allowedReferrerHosts = Collections.emptySet();
            log.debug("CSRF protection disabled");
         } else {
             if (DISABLED.equalsIgnoreCase(config.trim())) {
                 disabled = true;
@@ -104,25 +107,62 @@ public class CSRFUtil {
                     allowedReferrerHosts.add(entry.trim());
                 }
             }
            log.debug("CSRF protection enabled, allowed referrers: " + allowedReferrerHosts);
         }
     }
 
    public boolean isValidRequest(HttpServletRequest request) throws MalformedURLException {
        int methodCode = DavMethods.getMethodCode(request.getMethod());
        if (disabled || DavMethods.DAV_POST != methodCode || !CONTENT_TYPES.contains(request.getContentType())) {
    public boolean isValidRequest(HttpServletRequest request) {

        if (disabled) {
            return true;
        } else if (!"POST".equals(request.getMethod())) {
            // protection only needed for POST
             return true;
         } else {
            Enumeration<String> cts = (Enumeration<String>) request.getHeaders("Content-Type");
            String ct = null;
            if (cts != null && cts.hasMoreElements()) {
                String t = cts.nextElement();
                // prune parameters
                int semicolon = t.indexOf(';');
                if (semicolon >= 0) {
                    t = t.substring(0, semicolon);
                }
                ct = t.trim().toLowerCase(Locale.ENGLISH);
            }
            if (cts != null && cts.hasMoreElements()) {
                // reject if there are more header field instances
                log.debug("request blocked because there were multiple content-type header fields");
                return false;
            }
            if (ct != null && !CONTENT_TYPES.contains(ct)) {
                // type present and not in blacklist
                return true;
            }
 
             String refHeader = request.getHeader("Referer");
            // empty referrer headers are not allowed for POST + relevant content types (see JCR-3909)
            // empty referrer headers are not allowed for POST + relevant
            // content types (see JCR-3909)
             if (refHeader == null) {
                log.debug("POST with content type" + ct + " blocked due to missing referer header field");
                 return false;
             }
 
            String host = new URL(refHeader).getHost();
            // test referrer-host equals server or
            // if it is contained in the set of explicitly allowed host names
            return host.equals(request.getServerName()) || allowedReferrerHosts.contains(host);
            try {
                String host = new URI(refHeader).getHost();
                // test referrer-host equals server or
                // if it is contained in the set of explicitly allowed host
                // names
                boolean ok = host == null || host.equals(request.getServerName()) || allowedReferrerHosts.contains(host);
                if (!ok) {
                    log.debug("POST with content type" + ct + " blocked due to referer header field being: " + refHeader);
                }
                return ok;
            } catch (URISyntaxException ex) {
                // referrer malformed -> block access
                log.debug("POST with content type" + ct + " blocked due to malformed referer header field: " + refHeader);
                return false;
            }
         }
     }
 }
\ No newline at end of file
diff --git a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
index 08f1450db..8c1804529 100644
-- a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
++ b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
@@ -16,17 +16,9 @@
  */
 package org.apache.jackrabbit.webdav.util;
 
import junit.framework.TestCase;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
 import java.security.Principal;
 import java.util.ArrayList;
 import java.util.Arrays;
@@ -37,6 +29,15 @@ import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;
 
 /**
  * <code>CSRFUtilTest</code>...
@@ -55,16 +56,20 @@ public class CSRFUtilTest extends TestCase {
         validURLs.add("http://localhost:4503/jackrabbit/server");
         validURLs.add("https://localhost:4503/jackrabbit/server");
         validURLs.add("https://localhost/jackrabbit/server");
        validURLs.add("//localhost/jackrabbit/server");
        validURLs.add("/jackrabbit/server");
 
         invalidURLs.add("http://invalidHost/test");
         invalidURLs.add("http://host1:8080/test");
         invalidURLs.add("http://user:pw@host2/test");
     }
 
    private static void testValid(CSRFUtil util, Collection<String> validURLs, String method, Set<String> contentTypes) throws MalformedURLException {
    static String[] noContentType = new String[0];

    private static void testValid(CSRFUtil util, Collection<String> validURLs, String method, Set<String> contentTypes) {
         if (null == contentTypes) {
             for (String url : validURLs) {
                assertTrue(url, util.isValidRequest(createRequest(url, method, null)));
                assertTrue(url, util.isValidRequest(createRequest(url, method, noContentType)));
             }
         } else {
             for (String contentType : contentTypes) {
@@ -75,10 +80,10 @@ public class CSRFUtilTest extends TestCase {
         }
     }
 
    private static void testInvalid(CSRFUtil util, Collection<String> invalidURLs, String method, Set<String> contentTypes) throws MalformedURLException {
    private static void testInvalid(CSRFUtil util, Collection<String> invalidURLs, String method, Set<String> contentTypes) {
         if (null == contentTypes) {
             for (String url : validURLs) {
                assertFalse(url, util.isValidRequest(createRequest(url, method, null)));
                assertFalse(url, util.isValidRequest(createRequest(url, method, noContentType)));
             }
         } else {
             for (String contentType : contentTypes) {
@@ -89,8 +94,12 @@ public class CSRFUtilTest extends TestCase {
         }
     }
 
    private static HttpServletRequest createRequest(String url, String method, String[] contentTypes) {
        return new DummyRequest(url, SERVER_NAME, method, contentTypes);
    }

     private static HttpServletRequest createRequest(String url, String method, String contentType) {
        return new DummyRequest(url, SERVER_NAME, method, contentType);
        return new DummyRequest(url, SERVER_NAME, method, new String[] { contentType });
     }
 
     public void testNullConfig() throws Exception {
@@ -109,6 +118,10 @@ public class CSRFUtilTest extends TestCase {
         CSRFUtil util = new CSRFUtil("");
         testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
         assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, "text/plain")));
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, noContentType)));
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, "TEXT/PLAIN; foo=bar")));
        assertTrue("no referrer", util.isValidRequest(createRequest(null, POST, "application/json")));
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, new String[] { "application/json", "foo/bar" })));
     }
 
     public void testDisabledConfig() throws Exception {
@@ -155,13 +168,13 @@ public class CSRFUtilTest extends TestCase {
         private final String referer;
         private final String serverName;
         private final String method;
        private final String contentType;
        private final String[] contentTypes;
 
        private DummyRequest(String referer, String serverName, String method, String contentType) {
        private DummyRequest(String referer, String serverName, String method, String[] contentTypes) {
             this.referer = referer;
             this.serverName = serverName;
             this.method = method;
            this.contentType = contentType;
            this.contentTypes = contentTypes;
         }
 
         //---------------------------------------------< HttpServletRequest >---
@@ -178,6 +191,19 @@ public class CSRFUtilTest extends TestCase {
             return serverName;
         }
 
        public String getContentType() {
            return contentTypes.length == 0 ? null : contentTypes[0];
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Enumeration getHeaders(String name) {
            if (name != null && contentTypes.length > 0 && name.toLowerCase(Locale.ENGLISH).equals("content-type")) {
                return new Vector(Arrays.asList(contentTypes)).elements();
            } else {
                return null;
            }
        }

         //---------------------------------------------------------< unused >---
         public String getAuthType() {
             return null;
@@ -188,9 +214,6 @@ public class CSRFUtilTest extends TestCase {
         public long getDateHeader(String name) {
             return 0;
         }
        public Enumeration getHeaders(String name) {
            return null;
        }
         public Enumeration getHeaderNames() {
             return null;
         }
@@ -266,9 +289,6 @@ public class CSRFUtilTest extends TestCase {
         public int getContentLength() {
             return 0;
         }
        public String getContentType() {
            return contentType;
        }
         public ServletInputStream getInputStream() throws IOException {
             return null;
         }
- 
2.19.1.windows.1

