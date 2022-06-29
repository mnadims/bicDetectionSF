From 22ff9e68d1d9f27d62455c15fb1242538551aca9 Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Thu, 19 May 2016 14:44:44 +0100
Subject: [PATCH] HADOOP-12767. Update apache httpclient version to 4.5.2;
 httpcore to 4.4.4. Artem Aliev via stevel.

--
 .../web/DelegationTokenAuthenticationFilter.java      |  7 +++++--
 .../security/token/delegation/web/ServletUtils.java   |  7 +++++--
 hadoop-project/pom.xml                                |  4 ++--
 .../yarn/server/webproxy/WebAppProxyServlet.java      | 11 +++++++----
 4 files changed, 19 insertions(+), 10 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/DelegationTokenAuthenticationFilter.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/DelegationTokenAuthenticationFilter.java
index af66ee56776..fb6817e4daa 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/DelegationTokenAuthenticationFilter.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/DelegationTokenAuthenticationFilter.java
@@ -216,8 +216,11 @@ protected void setHandlerAuthMethod(SaslRpcServer.AuthMethod authMethod) {
 
   @VisibleForTesting
   static String getDoAs(HttpServletRequest request) {
    List<NameValuePair> list = URLEncodedUtils.parse(request.getQueryString(),
        UTF8_CHARSET);
    String queryString = request.getQueryString();
    if (queryString == null) {
      return null;
    }
    List<NameValuePair> list = URLEncodedUtils.parse(queryString, UTF8_CHARSET);
     if (list != null) {
       for (NameValuePair nv : list) {
         if (DelegationTokenAuthenticatedURL.DO_AS.
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/ServletUtils.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/ServletUtils.java
index 16137accc80..078dfa44bdd 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/ServletUtils.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/token/delegation/web/ServletUtils.java
@@ -45,8 +45,11 @@
    */
   public static String getParameter(HttpServletRequest request, String name)
       throws IOException {
    List<NameValuePair> list = URLEncodedUtils.parse(request.getQueryString(),
        UTF8_CHARSET);
    String queryString = request.getQueryString();
    if (queryString == null) {
      return null;
    }
    List<NameValuePair> list = URLEncodedUtils.parse(queryString, UTF8_CHARSET);
     if (list != null) {
       for (NameValuePair nv : list) {
         if (name.equals(nv.getName())) {
diff --git a/hadoop-project/pom.xml b/hadoop-project/pom.xml
index 46ecb231b65..3a2f9d90642 100644
-- a/hadoop-project/pom.xml
++ b/hadoop-project/pom.xml
@@ -462,12 +462,12 @@
       <dependency>
         <groupId>org.apache.httpcomponents</groupId>
         <artifactId>httpclient</artifactId>
        <version>4.2.5</version>
        <version>4.5.2</version>
       </dependency>
       <dependency>
         <groupId>org.apache.httpcomponents</groupId>
         <artifactId>httpcore</artifactId>
        <version>4.2.5</version>
        <version>4.4.4</version>
       </dependency>
       <dependency>
         <groupId>commons-codec</groupId>
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-web-proxy/src/main/java/org/apache/hadoop/yarn/server/webproxy/WebAppProxyServlet.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-web-proxy/src/main/java/org/apache/hadoop/yarn/server/webproxy/WebAppProxyServlet.java
index 9d64667b4c9..0b621aa182a 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-web-proxy/src/main/java/org/apache/hadoop/yarn/server/webproxy/WebAppProxyServlet.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-web-proxy/src/main/java/org/apache/hadoop/yarn/server/webproxy/WebAppProxyServlet.java
@@ -423,11 +423,14 @@ private void methodAction(final HttpServletRequest req,
 
       // Append the user-provided path and query parameter to the original
       // tracking url.
      List<NameValuePair> queryPairs =
          URLEncodedUtils.parse(req.getQueryString(), null);
       UriBuilder builder = UriBuilder.fromUri(trackingUri);
      for (NameValuePair pair : queryPairs) {
        builder.queryParam(pair.getName(), pair.getValue());
      String queryString = req.getQueryString();
      if (queryString != null) {
        List<NameValuePair> queryPairs =
            URLEncodedUtils.parse(queryString, null);
        for (NameValuePair pair : queryPairs) {
          builder.queryParam(pair.getName(), pair.getValue());
        }
       }
       URI toFetch = builder.path(rest).build();
 
- 
2.19.1.windows.1

