From f5e0bd30fde654ed48fe73e5c0523030365385a4 Mon Sep 17 00:00:00 2001
From: Xiaoyu Yao <xyao@apache.org>
Date: Wed, 14 Dec 2016 13:41:40 -0800
Subject: [PATCH] HADOOP-13890. Maintain HTTP/host as SPNEGO SPN support and
 fix KerberosName parsing. Contributed by Xiaoyu Yao.

--
 .../server/KerberosAuthenticationHandler.java | 19 ++++++++--------
 .../authentication/util/KerberosName.java     |  4 ++--
 .../authentication/util/TestKerberosName.java | 22 +++++++++++++++++++
 .../web/TestWebDelegationToken.java           |  4 ++++
 4 files changed, 38 insertions(+), 11 deletions(-)

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
index f51bbd68f7c..e0ee227c614 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
@@ -73,7 +73,7 @@
  * </ul>
  */
 public class KerberosAuthenticationHandler implements AuthenticationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(
  public static final Logger LOG = LoggerFactory.getLogger(
       KerberosAuthenticationHandler.class);
 
   /**
@@ -274,14 +274,14 @@ public void init(Properties config) throws ServletException {
         loginContexts.add(loginContext);
         KerberosName kerbName = new KerberosName(spnegoPrincipal);
         if (kerbName.getHostName() != null
            && kerbName.getRealm() != null
             && kerbName.getServiceName() != null
             && kerbName.getServiceName().equals("HTTP")) {
          LOG.trace("Map server: {} to principal: {}", kerbName.getHostName(),
          boolean added = serverPrincipalMap.put(kerbName.getHostName(),
               spnegoPrincipal);
          serverPrincipalMap.put(kerbName.getHostName(), spnegoPrincipal);
          LOG.info("Map server: {} to principal: [{}], added = {}",
              kerbName.getHostName(), spnegoPrincipal, added);
         } else {
          LOG.warn("HTTP principal: {} is invalid for SPNEGO!",
          LOG.warn("HTTP principal: [{}] is invalid for SPNEGO!",
               spnegoPrincipal);
         }
       }
@@ -419,8 +419,8 @@ public AuthenticationToken authenticate(HttpServletRequest request,
               @Override
               public AuthenticationToken run() throws Exception {
                 if (LOG.isTraceEnabled()) {
                  LOG.trace("SPNEGO with principals: {}",
                      serverPrincipals.toString());
                  LOG.trace("SPNEGO with server principals: {} for {}",
                      serverPrincipals.toString(), serverName);
                 }
                 AuthenticationToken token = null;
                 Exception lastException = null;
@@ -464,7 +464,7 @@ private AuthenticationToken runWithPrincipal(String serverPrincipal,
     GSSCredential gssCreds = null;
     AuthenticationToken token = null;
     try {
      LOG.trace("SPNEGO initiated with principal {}", serverPrincipal);
      LOG.trace("SPNEGO initiated with server principal [{}]", serverPrincipal);
       gssCreds = this.gssManager.createCredential(
           this.gssManager.createName(serverPrincipal,
               KerberosUtil.getOidInstance("NT_GSS_KRB5_PRINCIPAL")),
@@ -491,7 +491,8 @@ private AuthenticationToken runWithPrincipal(String serverPrincipal,
         String userName = kerberosName.getShortName();
         token = new AuthenticationToken(userName, clientPrincipal, getType());
         response.setStatus(HttpServletResponse.SC_OK);
        LOG.trace("SPNEGO completed for principal [{}]", clientPrincipal);
        LOG.trace("SPNEGO completed for client principal [{}]",
            clientPrincipal);
       }
     } finally {
       if (gssContext != null) {
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
index 0b668f1e237..6d15b6bea5c 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
@@ -54,7 +54,7 @@
    * A pattern that matches a Kerberos name with at most 2 components.
    */
   private static final Pattern nameParser =
    Pattern.compile("([^/@]*)(/([^/@]*))?@([^/@]*)");
      Pattern.compile("([^/@]+)(/([^/@]+))?(@([^/@]+))?");
 
   /**
    * A pattern that matches a string with out '$' and then a single
@@ -109,7 +109,7 @@ public KerberosName(String name) {
     } else {
       serviceName = match.group(1);
       hostName = match.group(3);
      realm = match.group(4);
      realm = match.group(5);
     }
   }
 
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestKerberosName.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestKerberosName.java
index f85b3e11d5a..a375bc95036 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestKerberosName.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/util/TestKerberosName.java
@@ -82,6 +82,28 @@ public void testAntiPatterns() throws Exception {
     checkTranslation("root/joe@FOO.COM", "root/joe@FOO.COM");
   }
 
  @Test
  public void testParsing() throws Exception {
    final String principalNameFull = "HTTP/abc.com@EXAMPLE.COM";
    final String principalNameWoRealm = "HTTP/abc.com";
    final String principalNameWoHost = "HTTP@EXAMPLE.COM";

    final KerberosName kerbNameFull = new KerberosName(principalNameFull);
    Assert.assertEquals("HTTP", kerbNameFull.getServiceName());
    Assert.assertEquals("abc.com", kerbNameFull.getHostName());
    Assert.assertEquals("EXAMPLE.COM", kerbNameFull.getRealm());

    final KerberosName kerbNamewoRealm = new KerberosName(principalNameWoRealm);
    Assert.assertEquals("HTTP", kerbNamewoRealm.getServiceName());
    Assert.assertEquals("abc.com", kerbNamewoRealm.getHostName());
    Assert.assertEquals(null, kerbNamewoRealm.getRealm());

    final KerberosName kerbNameWoHost = new KerberosName(principalNameWoHost);
    Assert.assertEquals("HTTP", kerbNameWoHost.getServiceName());
    Assert.assertEquals(null, kerbNameWoHost.getHostName());
    Assert.assertEquals("EXAMPLE.COM", kerbNameWoHost.getRealm());
  }

   @Test
   public void testToLowerCase() throws Exception {
     String rules =
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/token/delegation/web/TestWebDelegationToken.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/token/delegation/web/TestWebDelegationToken.java
index 89f15da891c..7319e4ca2bc 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/token/delegation/web/TestWebDelegationToken.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/token/delegation/web/TestWebDelegationToken.java
@@ -31,6 +31,8 @@
 import org.apache.hadoop.security.authentication.server.PseudoAuthenticationHandler;
 import org.apache.hadoop.security.authentication.util.KerberosUtil;
 import org.apache.hadoop.security.token.delegation.AbstractDelegationTokenSecretManager;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.log4j.Level;
 import org.eclipse.jetty.server.Server;
 import org.eclipse.jetty.server.ServerConnector;
 import org.eclipse.jetty.servlet.ServletContextHandler;
@@ -197,6 +199,8 @@ public void setUp() throws Exception {
     UserGroupInformation.setConfiguration(conf);
 
     jetty = createJettyServer();
    GenericTestUtils.setLogLevel(KerberosAuthenticationHandler.LOG,
        Level.TRACE);
   }
 
   @After
- 
2.19.1.windows.1

