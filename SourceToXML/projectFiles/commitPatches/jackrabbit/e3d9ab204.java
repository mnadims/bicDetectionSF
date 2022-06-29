From e3d9ab2042dfd0c59dd53263da307427223f8f1a Mon Sep 17 00:00:00 2001
From: =?UTF-8?q?Dominique=20J=C3=A4ggi?= <dj@apache.org>
Date: Fri, 12 Aug 2016 12:04:37 +0000
Subject: [PATCH] JCR-4002 : CSRF in Jackrabbit-Webdav using empty content-type

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1756173 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/jackrabbit/webdav/util/CSRFUtil.java  | 3 ++-
 .../org/apache/jackrabbit/webdav/util/CSRFUtilTest.java   | 8 ++++++++
 2 files changed, 10 insertions(+), 1 deletion(-)

diff --git a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
index bc1de76ff..07b2c0172 100644
-- a/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
++ b/jackrabbit-webdav/src/main/java/org/apache/jackrabbit/webdav/util/CSRFUtil.java
@@ -46,7 +46,8 @@ public class CSRFUtil {
                     new String[] {
                             "application/x-www-form-urlencoded",
                             "multipart/form-data",
                            "text/plain"
                            "text/plain",
                            null // no content type included in request
                     }
             )
     ));
diff --git a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
index 08f1450db..23bf7145a 100644
-- a/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
++ b/jackrabbit-webdav/src/test/java/org/apache/jackrabbit/webdav/util/CSRFUtilTest.java
@@ -48,6 +48,8 @@ public class CSRFUtilTest extends TestCase {
     private static final String GET = "GET";
     private static final String POST = "POST";
 
    private static final Set<String> EMPTY_REFERER = new HashSet<String>() {{ add(null); }};

     private static final List<String> validURLs = new ArrayList<String>();
     private static final List<String> invalidURLs = new ArrayList<String>();
 
@@ -111,6 +113,12 @@ public class CSRFUtilTest extends TestCase {
         assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, "text/plain")));
     }
 
    public void testNoContentType() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, validURLs, POST, EMPTY_REFERER);
        testInvalid(util, invalidURLs, POST, EMPTY_REFERER);
    }

     public void testDisabledConfig() throws Exception {
         CSRFUtil util = new CSRFUtil(CSRFUtil.DISABLED);
         testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
- 
2.19.1.windows.1

