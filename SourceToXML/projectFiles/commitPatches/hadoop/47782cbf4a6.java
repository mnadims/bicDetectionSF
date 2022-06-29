From 47782cbf4a66d49064fd3dd6d1d1a19cc42157fc Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Thu, 26 Mar 2015 16:29:36 -0700
Subject: [PATCH] HADOOP-11748. The secrets of auth cookies should not be
 specified in configuration in clear text. Contributed by Li Lu and Haohui
 Mai.

--
 .../server/AuthenticationFilter.java          |   7 +-
 .../server/TestAuthenticationFilter.java      | 173 ++++++------------
 .../util/StringSignerSecretProvider.java      |   6 +-
 .../StringSignerSecretProviderCreator.java    |  33 ++++
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../hadoop-hdfs-httpfs/pom.xml                |   6 +
 .../fs/http/server/TestHttpFSServer.java      |   6 +-
 7 files changed, 106 insertions(+), 128 deletions(-)
 rename hadoop-common-project/hadoop-auth/src/{main => test}/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java (92%)
 create mode 100644 hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProviderCreator.java

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
index 43bb4b0af92..5c22fce0245 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/AuthenticationFilter.java
@@ -279,14 +279,11 @@ protected void initializeSecretProvider(FilterConfig filterConfig)
             = config.getProperty(SIGNER_SECRET_PROVIDER, null);
     // fallback to old behavior
     if (signerSecretProviderName == null) {
      String signatureSecret = config.getProperty(SIGNATURE_SECRET, null);
       String signatureSecretFile = config.getProperty(
           SIGNATURE_SECRET_FILE, null);
      // The precedence from high to low : file, inline string, random
      // The precedence from high to low : file, random
       if (signatureSecretFile != null) {
         providerClassName = FileSignerSecretProvider.class.getName();
      } else if (signatureSecret != null) {
        providerClassName = StringSignerSecretProvider.class.getName();
       } else {
         providerClassName = RandomSignerSecretProvider.class.getName();
         randomSecret = true;
@@ -295,8 +292,6 @@ protected void initializeSecretProvider(FilterConfig filterConfig)
       if ("random".equals(signerSecretProviderName)) {
         providerClassName = RandomSignerSecretProvider.class.getName();
         randomSecret = true;
      } else if ("string".equals(signerSecretProviderName)) {
        providerClassName = StringSignerSecretProvider.class.getName();
       } else if ("file".equals(signerSecretProviderName)) {
         providerClassName = FileSignerSecretProvider.class.getName();
       } else if ("zookeeper".equals(signerSecretProviderName)) {
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
index a03894b0757..26c10a9d31a 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestAuthenticationFilter.java
@@ -38,7 +38,7 @@
 import org.apache.hadoop.security.authentication.client.AuthenticationException;
 import org.apache.hadoop.security.authentication.util.Signer;
 import org.apache.hadoop.security.authentication.util.SignerSecretProvider;
import org.apache.hadoop.security.authentication.util.StringSignerSecretProvider;
import org.apache.hadoop.security.authentication.util.StringSignerSecretProviderCreator;
 import org.junit.Assert;
 import org.junit.Test;
 import org.mockito.Mockito;
@@ -158,15 +158,15 @@ public void testInit() throws Exception {
     try {
       FilterConfig config = Mockito.mock(FilterConfig.class);
       Mockito.when(config.getInitParameter(AuthenticationFilter.AUTH_TYPE)).thenReturn("simple");
      Mockito.when(config.getInitParameter(AuthenticationFilter.AUTH_TOKEN_VALIDITY)).thenReturn(
      Mockito.when(config.getInitParameter(
          AuthenticationFilter.AUTH_TOKEN_VALIDITY)).thenReturn(
           (new Long(TOKEN_VALIDITY_SEC)).toString());
       Mockito.when(config.getInitParameterNames()).thenReturn(
        new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                                 AuthenticationFilter.AUTH_TOKEN_VALIDITY)).elements());
          new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                                           AuthenticationFilter.AUTH_TOKEN_VALIDITY)).elements());
       ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(context.getAttribute(AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
          .thenReturn(null);
       Mockito.when(config.getServletContext()).thenReturn(context);
       filter.init(config);
       Assert.assertEquals(PseudoAuthenticationHandler.class, filter.getAuthenticationHandler().getClass());
@@ -179,27 +179,6 @@ public void testInit() throws Exception {
       filter.destroy();
     }
 
    // string secret
    filter = new AuthenticationFilter();
    try {
      FilterConfig config = Mockito.mock(FilterConfig.class);
      Mockito.when(config.getInitParameter(AuthenticationFilter.AUTH_TYPE)).thenReturn("simple");
      Mockito.when(config.getInitParameter(AuthenticationFilter.SIGNATURE_SECRET)).thenReturn("secret");
      Mockito.when(config.getInitParameterNames()).thenReturn(
        new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                                 AuthenticationFilter.SIGNATURE_SECRET)).elements());
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

     // custom secret as inline
     filter = new AuthenticationFilter();
     try {
@@ -278,11 +257,7 @@ public void init(Properties config, ServletContext servletContext,
         new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                                  AuthenticationFilter.COOKIE_DOMAIN,
                                  AuthenticationFilter.COOKIE_PATH)).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
       Assert.assertEquals(".foo.com", filter.getCookieDomain());
       Assert.assertEquals("/bar", filter.getCookiePath());
@@ -303,11 +278,7 @@ public void init(Properties config, ServletContext servletContext,
         new Vector<String>(
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
       Assert.assertTrue(DummyAuthenticationHandler.init);
     } finally {
@@ -345,11 +316,7 @@ public void testInitCaseSensitivity() throws Exception {
       Mockito.when(config.getInitParameterNames()).thenReturn(
           new Vector<String>(Arrays.asList(AuthenticationFilter.AUTH_TYPE,
               AuthenticationFilter.AUTH_TOKEN_VALIDITY)).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
 
       filter.init(config);
       Assert.assertEquals(PseudoAuthenticationHandler.class, 
@@ -372,11 +339,7 @@ public void testGetRequestURL() throws Exception {
         new Vector<String>(
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
@@ -392,6 +355,7 @@ public void testGetRequestURL() throws Exception {
   @Test
   public void testGetToken() throws Exception {
     AuthenticationFilter filter = new AuthenticationFilter();

     try {
       FilterConfig config = Mockito.mock(FilterConfig.class);
       Mockito.when(config.getInitParameter("management.operation.return")).
@@ -404,21 +368,13 @@ public void testGetToken() throws Exception {
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         AuthenticationFilter.SIGNATURE_SECRET,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      SignerSecretProvider secretProvider =
          getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       AuthenticationToken token = new AuthenticationToken("u", "p", DummyAuthenticationHandler.TYPE);
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      Properties secretProviderProps = new Properties();
      secretProviderProps.setProperty(
              AuthenticationFilter.SIGNATURE_SECRET, "secret");
      secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);

       Signer signer = new Signer(secretProvider);
       String tokenSigned = signer.sign(token.toString());
 
@@ -448,18 +404,14 @@ public void testGetTokenExpired() throws Exception {
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         AuthenticationFilter.SIGNATURE_SECRET,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       AuthenticationToken token =
           new AuthenticationToken("u", "p", DummyAuthenticationHandler.TYPE);
       token.setExpires(System.currentTimeMillis() - TOKEN_VALIDITY_SEC);
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      SignerSecretProvider secretProvider =
          StringSignerSecretProviderCreator.newStringSignerSecretProvider();
       Properties secretProviderProps = new Properties();
       secretProviderProps.setProperty(
               AuthenticationFilter.SIGNATURE_SECRET, "secret");
@@ -500,17 +452,13 @@ public void testGetTokenInvalidType() throws Exception {
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         AuthenticationFilter.SIGNATURE_SECRET,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "invalidtype");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      SignerSecretProvider secretProvider =
          StringSignerSecretProviderCreator.newStringSignerSecretProvider();
       Properties secretProviderProps = new Properties();
       secretProviderProps.setProperty(
               AuthenticationFilter.SIGNATURE_SECRET, "secret");
@@ -536,6 +484,23 @@ public void testGetTokenInvalidType() throws Exception {
     }
   }
 
  private static SignerSecretProvider getMockedServletContextWithStringSigner(
      FilterConfig config) throws Exception {
    Properties secretProviderProps = new Properties();
    secretProviderProps.setProperty(AuthenticationFilter.SIGNATURE_SECRET,
                                    "secret");
    SignerSecretProvider secretProvider =
        StringSignerSecretProviderCreator.newStringSignerSecretProvider();
    secretProvider.init(secretProviderProps, null, TOKEN_VALIDITY_SEC);

    ServletContext context = Mockito.mock(ServletContext.class);
    Mockito.when(context.getAttribute(
            AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
            .thenReturn(secretProvider);
    Mockito.when(config.getServletContext()).thenReturn(context);
    return secretProvider;
  }

   @Test
   public void testDoFilterNotAuthenticated() throws Exception {
     AuthenticationFilter filter = new AuthenticationFilter();
@@ -549,11 +514,7 @@ public void testDoFilterNotAuthenticated() throws Exception {
         new Vector<String>(
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
@@ -603,11 +564,7 @@ private void _testDoFilterAuthentication(boolean withDomainPath,
             AuthenticationFilter.AUTH_TOKEN_VALIDITY,
             AuthenticationFilter.SIGNATURE_SECRET, "management.operation" +
             ".return", "expired.token")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
    getMockedServletContextWithStringSigner(config);
 
     if (withDomainPath) {
       Mockito.when(config.getInitParameter(AuthenticationFilter
@@ -661,8 +618,8 @@ public Object answer(InvocationOnMock invocation) throws Throwable {
         Mockito.verify(chain).doFilter(Mockito.any(ServletRequest.class),
                 Mockito.any(ServletResponse.class));
 
        StringSignerSecretProvider secretProvider
                = new StringSignerSecretProvider();
        SignerSecretProvider secretProvider =
            StringSignerSecretProviderCreator.newStringSignerSecretProvider();
         Properties secretProviderProps = new Properties();
         secretProviderProps.setProperty(
                 AuthenticationFilter.SIGNATURE_SECRET, "secret");
@@ -734,11 +691,7 @@ public void testDoFilterAuthenticated() throws Exception {
         new Vector<String>(
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
@@ -746,8 +699,8 @@ public void testDoFilterAuthenticated() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "t");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      SignerSecretProvider secretProvider =
          StringSignerSecretProviderCreator.newStringSignerSecretProvider();
       Properties secretProviderProps = new Properties();
       secretProviderProps.setProperty(
               AuthenticationFilter.SIGNATURE_SECRET, "secret");
@@ -795,11 +748,7 @@ public void testDoFilterAuthenticationFailure() throws Exception {
         new Vector<String>(
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
@@ -863,11 +812,7 @@ public void testDoFilterAuthenticatedExpired() throws Exception {
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         AuthenticationFilter.SIGNATURE_SECRET,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
@@ -875,8 +820,8 @@ public void testDoFilterAuthenticatedExpired() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", DummyAuthenticationHandler.TYPE);
       token.setExpires(System.currentTimeMillis() - TOKEN_VALIDITY_SEC);
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      SignerSecretProvider secretProvider =
          StringSignerSecretProviderCreator.newStringSignerSecretProvider();
       Properties secretProviderProps = new Properties();
       secretProviderProps.setProperty(
               AuthenticationFilter.SIGNATURE_SECRET, secret);
@@ -942,11 +887,7 @@ public void testDoFilterAuthenticatedInvalidType() throws Exception {
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         AuthenticationFilter.SIGNATURE_SECRET,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
@@ -954,8 +895,8 @@ public void testDoFilterAuthenticatedInvalidType() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "invalidtype");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      SignerSecretProvider secretProvider =
          StringSignerSecretProviderCreator.newStringSignerSecretProvider();
       Properties secretProviderProps = new Properties();
       secretProviderProps.setProperty(
               AuthenticationFilter.SIGNATURE_SECRET, secret);
@@ -989,11 +930,7 @@ public void testManagementOperation() throws Exception {
         new Vector<String>(
           Arrays.asList(AuthenticationFilter.AUTH_TYPE,
                         "management.operation.return")).elements());
      ServletContext context = Mockito.mock(ServletContext.class);
      Mockito.when(context.getAttribute(
              AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE))
              .thenReturn(null);
      Mockito.when(config.getServletContext()).thenReturn(context);
      getMockedServletContextWithStringSigner(config);
       filter.init(config);
 
       HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
@@ -1013,8 +950,8 @@ public void testManagementOperation() throws Exception {
 
       AuthenticationToken token = new AuthenticationToken("u", "p", "t");
       token.setExpires(System.currentTimeMillis() + TOKEN_VALIDITY_SEC);
      StringSignerSecretProvider secretProvider
              = new StringSignerSecretProvider();
      SignerSecretProvider secretProvider =
          StringSignerSecretProviderCreator.newStringSignerSecretProvider();
       Properties secretProviderProps = new Properties();
       secretProviderProps.setProperty(
               AuthenticationFilter.SIGNATURE_SECRET, "secret");
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
similarity index 92%
rename from hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
rename to hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
index 57ddd372fe4..7e5b10e6418 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProvider.java
@@ -16,6 +16,8 @@
 import java.nio.charset.Charset;
 import java.util.Properties;
 import javax.servlet.ServletContext;

import com.google.common.annotations.VisibleForTesting;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
@@ -24,8 +26,8 @@
  * A SignerSecretProvider that simply creates a secret based on a given String.
  */
 @InterfaceStability.Unstable
@InterfaceAudience.Private
public class StringSignerSecretProvider extends SignerSecretProvider {
@VisibleForTesting
class StringSignerSecretProvider extends SignerSecretProvider {
 
   private byte[] secret;
   private byte[][] secrets;
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProviderCreator.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProviderCreator.java
new file mode 100644
index 00000000000..e567e7bfbaf
-- /dev/null
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/StringSignerSecretProviderCreator.java
@@ -0,0 +1,33 @@
/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */
package org.apache.hadoop.security.authentication.util;

import com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * Helper class for creating StringSignerSecretProviders in unit tests
 */
@InterfaceStability.Unstable
@VisibleForTesting
public class StringSignerSecretProviderCreator {
  /**
   * @return a new StringSignerSecretProvider
   * @throws Exception
   */
  public static StringSignerSecretProvider newStringSignerSecretProvider()
      throws Exception {
    return new StringSignerSecretProvider();
  }
}
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 40b4f84033d..e739a8ff457 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1163,6 +1163,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11738. Fix a link of Protocol Buffers 2.5 for download in BUILDING.txt.
     (ozawa)
 
    HADOOP-11748. The secrets of auth cookies should not be specified in
    configuration in clear text. (Li Lu and Haohui Mai via wheat9)

 Release 2.6.1 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-hdfs-project/hadoop-hdfs-httpfs/pom.xml b/hadoop-hdfs-project/hadoop-hdfs-httpfs/pom.xml
index ddc60339669..520e30fb325 100644
-- a/hadoop-hdfs-project/hadoop-hdfs-httpfs/pom.xml
++ b/hadoop-hdfs-project/hadoop-hdfs-httpfs/pom.xml
@@ -195,6 +195,12 @@
       <scope>test</scope>
       <type>test-jar</type>
     </dependency>
    <dependency>
      <groupId>org.apache.hadoop</groupId>
      <artifactId>hadoop-auth</artifactId>
      <scope>test</scope>
      <type>test-jar</type>
    </dependency>
     <dependency>
       <groupId>log4j</groupId>
       <artifactId>log4j</artifactId>
diff --git a/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java b/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java
index 763d168d198..14b7a43654d 100644
-- a/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java
++ b/hadoop-hdfs-project/hadoop-hdfs-httpfs/src/test/java/org/apache/hadoop/fs/http/server/TestHttpFSServer.java
@@ -18,6 +18,8 @@
 package org.apache.hadoop.fs.http.server;
 
 import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.security.authentication.util.SignerSecretProvider;
import org.apache.hadoop.security.authentication.util.StringSignerSecretProviderCreator;
 import org.apache.hadoop.security.token.delegation.web.DelegationTokenAuthenticator;
 import org.apache.hadoop.security.token.delegation.web.KerberosDelegationTokenAuthenticationHandler;
 import org.json.simple.JSONArray;
@@ -68,7 +70,6 @@
 import com.google.common.collect.Maps;
 import java.util.Properties;
 import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
import org.apache.hadoop.security.authentication.util.StringSignerSecretProvider;
 
 public class TestHttpFSServer extends HFSTestCase {
 
@@ -687,7 +688,8 @@ public void testDelegationTokenOperations() throws Exception {
       new AuthenticationToken("u", "p",
           new KerberosDelegationTokenAuthenticationHandler().getType());
     token.setExpires(System.currentTimeMillis() + 100000000);
    StringSignerSecretProvider secretProvider = new StringSignerSecretProvider();
    SignerSecretProvider secretProvider =
        StringSignerSecretProviderCreator.newStringSignerSecretProvider();
     Properties secretProviderProps = new Properties();
     secretProviderProps.setProperty(AuthenticationFilter.SIGNATURE_SECRET, "secret");
     secretProvider.init(secretProviderProps, null, -1);
- 
2.19.1.windows.1

