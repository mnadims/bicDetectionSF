From 10549ef294b0e08a3802eb58d7c9ef5f1afe15ba Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Fri, 26 Jun 2015 13:47:04 -0700
Subject: [PATCH] OOZIE-2262 Fix log streaming from other server with start/end
 filter (kailongs via rohini)

--
 core/src/main/java/org/apache/oozie/util/AuthUrlClient.java | 6 ++++--
 release-log.txt                                             | 3 ++-
 2 files changed, 6 insertions(+), 3 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/util/AuthUrlClient.java b/core/src/main/java/org/apache/oozie/util/AuthUrlClient.java
index 5de847138..b45a96acc 100644
-- a/core/src/main/java/org/apache/oozie/util/AuthUrlClient.java
++ b/core/src/main/java/org/apache/oozie/util/AuthUrlClient.java
@@ -23,8 +23,10 @@ import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
 import java.net.HttpURLConnection;
 import java.net.URL;
import java.net.URLEncoder;
 import java.security.PrivilegedExceptionAction;
 import java.util.Map;
 
@@ -142,7 +144,7 @@ public class AuthUrlClient {
         return reader;
     }
 
    public static String getQueryParamString(Map<String, String[]> params) {
    public static String getQueryParamString(Map<String, String[]> params) throws UnsupportedEncodingException {
         StringBuilder stringBuilder = new StringBuilder();
         if (params == null || params.isEmpty()) {
             return "";
@@ -153,7 +155,7 @@ public class AuthUrlClient {
                 String value = params.get(key)[0]; // We don't support multi value.
                 stringBuilder.append(key);
                 stringBuilder.append("=");
                stringBuilder.append(value);
                stringBuilder.append(URLEncoder.encode(value,"UTF-8"));
             }
         }
         return stringBuilder.toString();
diff --git a/release-log.txt b/release-log.txt
index fe8d1fcf1..a53fcb629 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,9 +1,10 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2262 Fix log streaming from other server with start/end filter (kailongs via rohini)
 OOZIE-2159 'oozie validate' command should be moved server-side (seoeun25 via rkanter)
 OOZIE-2271 Upgrade Tomcat to 6.0.44 (rkanter)
 OOZIE-2266 Fix 'total' actions returned in coordinator job info (sai-krish via rkanter)
OOZIE-2264 Fix coord:offset(n,"DAY") to resolve correct data set(kailongs via puru)
OOZIE-2264 Fix coord:offset(n,"DAY") to resolve correct data set (kailongs via puru)
 OOZIE-2178 fix javadoc to compile on JDK8 (rkanter)
 OOZIE-2268 Update ActiveMQ version for security and other fixes (rkanter)
 OOZIE-2215 Support glob in FS EL function (ryota)
- 
2.19.1.windows.1

