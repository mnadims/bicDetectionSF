From e4b8d9e72d54d4725bf2a902452459b6b243b2e9 Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Wed, 25 Mar 2015 11:12:27 -0700
Subject: [PATCH] HADOOP-10670. Allow AuthenticationFilters to load secret from
 signature secret files. Contributed by Kai Zheng.

--
 .../server/AuthenticationFilter.java          | 18 +++++---
 .../server/TestAuthenticationFilter.java      | 38 +++++++++++++++-
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../AuthenticationFilterInitializer.java      | 26 -----------
 .../security/TestAuthenticationFilter.java    | 12 ------
 ...melineAuthenticationFilterInitializer.java | 43 ++-----------------
 .../RMAuthenticationFilterInitializer.java    | 31 -------------
 7 files changed, 55 insertions(+), 116 deletions(-)

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
index e891ed2623d..43bb4b0af92 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
@@ -18,12 +18,7 @@
 import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
 import org.apache.hadoop.security.authentication.client.AuthenticationException;
 import org.apache.hadoop.security.authentication.client.KerberosAuthenticator;
import org.apache.hadoop.security.authentication.util.Signer;
import org.apache.hadoop.security.authentication.util.SignerException;
import org.apache.hadoop.security.authentication.util.RandomSignerSecretProvider;
import org.apache.hadoop.security.authentication.util.SignerSecretProvider;
import org.apache.hadoop.security.authentication.util.StringSignerSecretProvider;
import org.apache.hadoop.security.authentication.util.ZKSignerSecretProvider;
import org.apache.hadoop.security.authentication.util.*;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -147,6 +142,8 @@
    */
   public static final String SIGNATURE_SECRET = "signature.secret";
 
  public static final String SIGNATURE_SECRET_FILE = SIGNATURE_SECRET + ".file";

   /**
    * Constant for the configuration property that indicates the validity of the generated token.
    */
@@ -283,7 +280,12 @@ protected void initializeSecretProvider(FilterConfig filterConfig)
     // fallback to old behavior
     if (signerSecretProviderName == null) {
       String signatureSecret = config.getProperty(SIGNATURE_SECRET, null);
      if (signatureSecret != null) {
      String signatureSecretFile = config.getProperty(
          SIGNATURE_SECRET_FILE, null);
      // The precedence from high to low : file, inline string, random
      if (signatureSecretFile != null) {
        providerClassName = FileSignerSecretProvider.class.getName();
      } else if (signatureSecret != null) {
         providerClassName = StringSignerSecretProvider.class.getName();
       } else {
         providerClassName = RandomSignerSecretProvider.class.getName();
@@ -295,6 +297,8 @@ protected void initializeSecretProvider(FilterConfig filterConfig)
         randomSecret = true;
       } else if ("string".equals(signerSecretProviderName)) {
         providerClassName = StringSignerSecretProvider.class.getName();
      } else if ("file".equals(signerSecretProviderName)) {
        providerClassName = FileSignerSecretProvider.class.getName();
       } else if ("zookeeper".equals(signerSecretProviderName)) {
         providerClassName = ZKSignerSecretProvider.class.getName();
       } else {
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
index c01c182db15..a03894b0757 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
@@ -13,7 +13,10 @@
  */
 package org.apache.hadoop.security.authentication.server;
 
import java.io.File;
import java.io.FileWriter;
 import java.io.IOException;
import java.io.Writer;
 import java.net.HttpCookie;
 import java.util.Arrays;
 import java.util.HashMap;
@@ -197,7 +200,7 @@ public void testInit() throws Exception {
       filter.destroy();
     }
 
    // custom secret
    // custom secret as inline
     filter = new AuthenticationFilter();
     try {
       FilterConfig config = Mockito.mock(FilterConfig.class);
@@ -231,6 +234,39 @@ public void init(Properties config, ServletContext servletContext,
       filter.destroy();
     }
 
    // custom secret by file
    File testDir = new File(System.getProperty("test.build.data",
        "target/test-dir"));
    testDir.mkdirs();
    String secretValue = "hadoop";
    File secretFile = new File(testDir, "http-secret.txt");
    Writer writer = new FileWriter(secretFile);
    writer.write(secretValue);
    writer.close();

    filter = new AuthenticationFilter();
    try {
      FilterConfig config = Mockito.mock(FilterConfig.class);
      Mockito.when(config.getInitParameter(
          AuthenticationFilter.AUTH_TYPE)).thenReturn("simple");
      Mockito.when(config.getInitParameter(
          AuthenticationFilter.SIGNATURE_SECRET_FILE))
          .thenReturn(secretFile.getAbsolutePath());
      Mockito.when(config.getInitParameterNames()).thenReturn(
          new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
              AuthenticationFilter.SIGNATURE_SECRET_FILE)).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
          AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
          .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      filter.init(config);
      Assert.assertFalse(filter.isRandomSecret());
      Assert.assertFalse(filter.isCustomSignerSecretProvider());
    } finally {
      filter.destroy();
    }

     // custom cookie domain and cookie path
     filter = new AuthenticationFilter();
     try {
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index a01a201206b..46dfee4d456 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -710,6 +710,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-9329. document native build dependencies in BUILDING.txt (Vijay Bhat
     via Colin P. McCabe)
 
    HADOOP-10670. Allow AuthenticationFilters to load secret from signature
    secret files. (Kai Zheng via wheat9)

   OPTIMIZATIONS
 
     HADOOP-11323. WritableComparator#compare keeps reference to byte array.
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java
index 43d1b66d44f..cb3830d3ea2 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/AuthenticationFilterInitializer.java
@@ -17,7 +17,6 @@
  */
 package org.apache.hadoop.security;
 
import com.google.common.base.Charsets;
 import org.apache.hadoop.http.HttpServer2;
 import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
 import org.apache.hadoop.conf.Configuration;
@@ -25,11 +24,7 @@
 import org.apache.hadoop.http.FilterInitializer;
 import org.apache.hadoop.security.authentication.server.KerberosAuthenticationHandler;
 
import java.io.FileInputStream;
import java.io.FileReader;
 import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
 import java.util.HashMap;
 import java.util.Map;
 
@@ -50,8 +45,6 @@
 
   static final String PREFIX = "hadoop.http.authentication.";
 
  static final String SIGNATURE_SECRET_FILE = AuthenticationFilter.SIGNATURE_SECRET + ".file";

   /**
    * Initializes hadoop-auth AuthenticationFilter.
    * <p/>
@@ -77,25 +70,6 @@ public void initFilter(FilterContainer container, Configuration conf) {
       }
     }
 
    String signatureSecretFile = filterConfig.get(SIGNATURE_SECRET_FILE);
    if (signatureSecretFile == null) {
      throw new RuntimeException("Undefined property: " + SIGNATURE_SECRET_FILE);      
    }

    StringBuilder secret = new StringBuilder();
    try (Reader reader = new InputStreamReader(
        new FileInputStream(signatureSecretFile), Charsets.UTF_8)) {
      int c = reader.read();
      while (c > -1) {
        secret.append((char)c);
        c = reader.read();
      }
      reader.close();
      filterConfig.put(AuthenticationFilter.SIGNATURE_SECRET, secret.toString());
    } catch (IOException ex) {
      throw new RuntimeException("Could not read HTTP signature secret file: " + signatureSecretFile);            
    }

     //Resolve _HOST into bind address
     String bindAddress = conf.get(HttpServer2.BIND_ADDRESS);
     String principal = filterConfig.get(KerberosAuthenticationHandler.PRINCIPAL);
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestAuthenticationFilter.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestAuthenticationFilter.java
index b6aae0eb637..c8179e2edc0 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestAuthenticationFilter.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestAuthenticationFilter.java
@@ -37,17 +37,6 @@
   public void testConfiguration() throws Exception {
     Configuration conf = new Configuration();
     conf.set("hadoop.http.authentication.foo", "bar");
    
    File testDir = new File(System.getProperty("test.build.data", 
                                               "target/test-dir"));
    testDir.mkdirs();
    File secretFile = new File(testDir, "http-secret.txt");
    Writer writer = new FileWriter(new File(testDir, "http-secret.txt"));
    writer.write("hadoop");
    writer.close();
    conf.set(AuthenticationFilterInitializer.PREFIX + 
             AuthenticationFilterInitializer.SIGNATURE_SECRET_FILE, 
             secretFile.getAbsolutePath());
 
     conf.set(HttpServer2.BIND_ADDRESS, "barhost");
     
@@ -68,7 +57,6 @@ public Object answer(InvocationOnMock invocationOnMock)
 
           assertEquals("simple", conf.get("type"));
           assertEquals("36000", conf.get("token.validity"));
          assertEquals("hadoop", conf.get("signature.secret"));
           assertNull(conf.get("cookie.domain"));
           assertEquals("true", conf.get("simple.anonymous.allowed"));
           assertEquals("HTTP/barhost@LOCALHOST",
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-applicationhistoryservice/src/main/java/org/apache/hadoop/yarn/server/timeline/security/TimelineAuthenticationFilterInitializer.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-applicationhistoryservice/src/main/java/org/apache/hadoop/yarn/server/timeline/security/TimelineAuthenticationFilterInitializer.java
index 1ee818145e6..a3c136c214f 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-applicationhistoryservice/src/main/java/org/apache/hadoop/yarn/server/timeline/security/TimelineAuthenticationFilterInitializer.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-applicationhistoryservice/src/main/java/org/apache/hadoop/yarn/server/timeline/security/TimelineAuthenticationFilterInitializer.java
@@ -18,20 +18,11 @@
 
 package org.apache.hadoop.yarn.server.timeline.security;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.http.FilterContainer;
 import org.apache.hadoop.http.FilterInitializer;
 import org.apache.hadoop.http.HttpServer2;
import org.apache.hadoop.io.IOUtils;
 import org.apache.hadoop.security.SecurityUtil;
 import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
 import org.apache.hadoop.security.authentication.server.KerberosAuthenticationHandler;
@@ -42,7 +33,9 @@
 import org.apache.hadoop.security.token.delegation.web.PseudoDelegationTokenAuthenticationHandler;
 import org.apache.hadoop.yarn.security.client.TimelineDelegationTokenIdentifier;
 
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
 
 /**
  * Initializes {@link TimelineAuthenticationFilter} which provides support for
@@ -62,9 +55,6 @@
    */
   public static final String PREFIX = "yarn.timeline-service.http-authentication.";
 
  private static final String SIGNATURE_SECRET_FILE =
      TimelineAuthenticationFilter.SIGNATURE_SECRET + ".file";

   @VisibleForTesting
   Map<String, String> filterConfig;
 
@@ -106,31 +96,6 @@ public void initFilter(FilterContainer container, Configuration conf) {
       }
     }
 
    String signatureSecretFile = filterConfig.get(SIGNATURE_SECRET_FILE);
    if (signatureSecretFile != null) {
      Reader reader = null;
      try {
        StringBuilder secret = new StringBuilder();
        reader = new InputStreamReader(new FileInputStream(new File(signatureSecretFile)),
                                      Charset.forName("UTF-8"));

        int c = reader.read();
        while (c > -1) {
          secret.append((char) c);
          c = reader.read();
        }
        filterConfig
            .put(TimelineAuthenticationFilter.SIGNATURE_SECRET,
                secret.toString());
      } catch (IOException ex) {
        throw new RuntimeException(
            "Could not read HTTP signature secret file: "
                + signatureSecretFile);
      } finally {
        IOUtils.closeStream(reader);
      }
    }

     String authType = filterConfig.get(AuthenticationFilter.AUTH_TYPE);
     if (authType.equals(PseudoAuthenticationHandler.TYPE)) {
       filterConfig.put(AuthenticationFilter.AUTH_TYPE,
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-common/src/main/java/org/apache/hadoop/yarn/server/security/http/RMAuthenticationFilterInitializer.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-common/src/main/java/org/apache/hadoop/yarn/server/security/http/RMAuthenticationFilterInitializer.java
index a62cda39a30..9fc13348c13 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-common/src/main/java/org/apache/hadoop/yarn/server/security/http/RMAuthenticationFilterInitializer.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-common/src/main/java/org/apache/hadoop/yarn/server/security/http/RMAuthenticationFilterInitializer.java
@@ -43,14 +43,11 @@
 public class RMAuthenticationFilterInitializer extends FilterInitializer {
 
   String configPrefix;
  String signatureSecretFileProperty;
   String kerberosPrincipalProperty;
   String cookiePath;
 
   public RMAuthenticationFilterInitializer() {
     this.configPrefix = "hadoop.http.authentication.";
    this.signatureSecretFileProperty =
        AuthenticationFilter.SIGNATURE_SECRET + ".file";
     this.kerberosPrincipalProperty = KerberosAuthenticationHandler.PRINCIPAL;
     this.cookiePath = "/";
   }
@@ -77,34 +74,6 @@ public RMAuthenticationFilterInitializer() {
       }
     }
 
    String signatureSecretFile = filterConfig.get(signatureSecretFileProperty);
    if (signatureSecretFile != null) {
      Reader reader = null;
      try {
        StringBuilder secret = new StringBuilder();
        reader =
            new InputStreamReader(new FileInputStream(signatureSecretFile),
              "UTF-8");
        int c = reader.read();
        while (c > -1) {
          secret.append((char) c);
          c = reader.read();
        }
        filterConfig.put(AuthenticationFilter.SIGNATURE_SECRET,
          secret.toString());
      } catch (IOException ex) {
        // if running in non-secure mode, this filter only gets added
        // because the user has not setup his own filter so just generate
        // a random secret. in secure mode, the user needs to setup security
        if (UserGroupInformation.isSecurityEnabled()) {
          throw new RuntimeException(
            "Could not read HTTP signature secret file: " + signatureSecretFile);
        }
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }

     // Resolve _HOST into bind address
     String bindAddress = conf.get(HttpServer2.BIND_ADDRESS);
     String principal = filterConfig.get(kerberosPrincipalProperty);
- 
2.19.1.windows.1

