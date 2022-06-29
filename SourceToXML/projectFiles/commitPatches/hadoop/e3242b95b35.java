From e3242b95b35844a0877a83032d3a7e3d5e9bd9c2 Mon Sep 17 00:00:00 2001
From: Devaraj Das <ddas@apache.org>
Date: Thu, 3 May 2012 17:16:44 +0000
Subject: [PATCH] HADOOP-8346. Makes oid changes to make SPNEGO work. Was
 broken due to fixes introduced by the IBM JDK compatibility patch.
 Contributed by Devaraj Das.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1333557 13f79535-47bb-0310-9956-ffa450edef68
--
 .../authentication/client/KerberosAuthenticator.java      | 8 +++-----
 .../hadoop/security/authentication/util/KerberosUtil.java | 6 ++----
 .../server/TestKerberosAuthenticationHandler.java         | 6 +++---
 hadoop-common-project/hadoop-common/CHANGES.txt           | 3 +++
 4 files changed, 11 insertions(+), 12 deletions(-)

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java
index 48b6cbec6e3..4227d084385 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java
@@ -26,7 +26,6 @@
 import javax.security.auth.login.LoginContext;
 import javax.security.auth.login.LoginException;
 import java.io.IOException;
import java.lang.reflect.Field;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.security.AccessControlContext;
@@ -196,11 +195,10 @@ public Void run() throws Exception {
           try {
             GSSManager gssManager = GSSManager.getInstance();
             String servicePrincipal = "HTTP/" + KerberosAuthenticator.this.url.getHost();
            
            Oid oid = KerberosUtil.getOidInstance("NT_GSS_KRB5_PRINCIPAL");
             GSSName serviceName = gssManager.createName(servicePrincipal,
                                                        GSSName.NT_HOSTBASED_SERVICE);
            Oid oid = KerberosUtil.getOidClassInstance(servicePrincipal, 
                gssManager);
                                                        oid);
            oid = KerberosUtil.getOidInstance("GSS_KRB5_MECH_OID");
             gssContext = gssManager.createContext(serviceName, oid, null,
                                                   GSSContext.DEFAULT_LIFETIME);
             gssContext.requestCredDeleg(true);
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java
index df8319c6643..5688e600f77 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java
@@ -22,7 +22,6 @@
 import java.lang.reflect.Method;
 
 import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
 import org.ietf.jgss.Oid;
 
 public class KerberosUtil {
@@ -34,8 +33,7 @@ public static String getKrb5LoginModuleName() {
       : "com.sun.security.auth.module.Krb5LoginModule";
   }
   
  public static Oid getOidClassInstance(String servicePrincipal,
      GSSManager gssManager) 
  public static Oid getOidInstance(String oidName) 
       throws ClassNotFoundException, GSSException, NoSuchFieldException,
       IllegalAccessException {
     Class<?> oidClass;
@@ -44,7 +42,7 @@ public static Oid getOidClassInstance(String servicePrincipal,
     } else {
       oidClass = Class.forName("sun.security.jgss.GSSUtil");
     }
    Field oidField = oidClass.getDeclaredField("GSS_KRB5_MECH_OID");
    Field oidField = oidClass.getDeclaredField(oidName);
     return (Oid)oidField.get(oidClass);
   }
 
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java
index e6e7c9cca00..692ceab92da 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java
@@ -145,10 +145,10 @@ public String call() throws Exception {
         GSSContext gssContext = null;
         try {
           String servicePrincipal = KerberosTestUtils.getServerPrincipal();
          Oid oid = KerberosUtil.getOidInstance("NT_GSS_KRB5_PRINCIPAL");
           GSSName serviceName = gssManager.createName(servicePrincipal,
              GSSName.NT_HOSTBASED_SERVICE);
          Oid oid = KerberosUtil.getOidClassInstance(servicePrincipal, 
              gssManager);
              oid);
          oid = KerberosUtil.getOidInstance("GSS_KRB5_MECH_OID");
           gssContext = gssManager.createContext(serviceName, oid, null,
                                                   GSSContext.DEFAULT_LIFETIME);
           gssContext.requestCredDeleg(true);
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index f8435a41298..d0d134bb306 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -409,6 +409,9 @@ Release 2.0.0 - UNRELEASED
     HADOOP-8342. HDFS command fails with exception following merge of 
     HADOOP-8325 (tucu)
 
    HADOOP-8346. Makes oid changes to make SPNEGO work. Was broken due
    to fixes introduced by the IBM JDK compatibility patch. (ddas)

   BREAKDOWN OF HADOOP-7454 SUBTASKS
 
     HADOOP-7455. HA: Introduce HA Service Protocol Interface. (suresh)
- 
2.19.1.windows.1

