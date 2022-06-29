From 9097e2efe4c92d83c8fab88dc11be84505a6cab5 Mon Sep 17 00:00:00 2001
From: Xiaoyu Yao <xyao@apache.org>
Date: Thu, 13 Oct 2016 10:52:13 -0700
Subject: [PATCH] HADOOP-13565. KerberosAuthenticationHandler#authenticate
 should not rebuild SPN based on client request. Contributed by Xiaoyu Yao.

--
 .../server/KerberosAuthenticationHandler.java              | 7 +------
 1 file changed, 1 insertion(+), 6 deletions(-)

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
index c6d188170c6..07c2a312363 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
@@ -343,8 +343,6 @@ public AuthenticationToken authenticate(HttpServletRequest request, final HttpSe
       authorization = authorization.substring(KerberosAuthenticator.NEGOTIATE.length()).trim();
       final Base64 base64 = new Base64(0);
       final byte[] clientToken = base64.decode(authorization);
      final String serverName = InetAddress.getByName(request.getServerName())
                                           .getCanonicalHostName();
       try {
         token = Subject.doAs(serverSubject, new PrivilegedExceptionAction<AuthenticationToken>() {
 
@@ -354,10 +352,7 @@ public AuthenticationToken run() throws Exception {
             GSSContext gssContext = null;
             GSSCredential gssCreds = null;
             try {
              gssCreds = gssManager.createCredential(
                  gssManager.createName(
                      KerberosUtil.getServicePrincipal("HTTP", serverName),
                      KerberosUtil.getOidInstance("NT_GSS_KRB5_PRINCIPAL")),
              gssCreds = gssManager.createCredential(null,
                   GSSCredential.INDEFINITE_LIFETIME,
                   new Oid[]{
                     KerberosUtil.getOidInstance("GSS_SPNEGO_MECH_OID"),
- 
2.19.1.windows.1

