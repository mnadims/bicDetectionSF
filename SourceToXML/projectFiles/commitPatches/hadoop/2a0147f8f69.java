From 2a0147f8f698f22e61281c06691107e24a2f139c Mon Sep 17 00:00:00 2001
From: Devaraj Das <ddas@apache.org>
Date: Fri, 16 Mar 2012 01:45:12 +0000
Subject: [PATCH] HADOOP-6941. Adds support for building Hadoop with IBM's JDK.
 Contributed by Stephen Watt, Eli Collins and Devaraj Das.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1301308 13f79535-47bb-0310-9956-ffa450edef68
--
 .../client/KerberosAuthenticator.java         | 16 +++--
 .../server/KerberosAuthenticationHandler.java |  4 +-
 .../authentication/util/KerberosName.java     |  9 +--
 .../authentication/util/KerberosUtil.java     | 53 +++++++++++++++++
 .../authentication/KerberosTestUtils.java     |  6 +-
 .../TestKerberosAuthenticationHandler.java    | 13 +++--
 .../hadoop-common/CHANGES.txt                 |  3 +
 .../hadoop/security/HadoopKerberosName.java   |  8 +--
 .../apache/hadoop/security/SecurityUtil.java  | 55 ++++++++++++++----
 .../hadoop/security/UserGroupInformation.java | 58 ++++++++++++++-----
 10 files changed, 175 insertions(+), 50 deletions(-)
 create mode 100644 hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java
index 7338cda2195..48b6cbec6e3 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/client/KerberosAuthenticator.java
@@ -13,12 +13,12 @@
  */
 package org.apache.hadoop.security.authentication.client;
 
import com.sun.security.auth.module.Krb5LoginModule;
 import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
 import org.ietf.jgss.GSSContext;
 import org.ietf.jgss.GSSManager;
 import org.ietf.jgss.GSSName;
import sun.security.jgss.GSSUtil;
import org.ietf.jgss.Oid;
 
 import javax.security.auth.Subject;
 import javax.security.auth.login.AppConfigurationEntry;
@@ -26,6 +26,7 @@
 import javax.security.auth.login.LoginContext;
 import javax.security.auth.login.LoginException;
 import java.io.IOException;
import java.lang.reflect.Field;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.security.AccessControlContext;
@@ -97,7 +98,7 @@
     }
 
     private static final AppConfigurationEntry USER_KERBEROS_LOGIN =
      new AppConfigurationEntry(Krb5LoginModule.class.getName(),
      new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(),
                                 AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL,
                                 USER_KERBEROS_OPTIONS);
 
@@ -109,7 +110,7 @@
       return USER_KERBEROS_CONF;
     }
   }

  
   private URL url;
   private HttpURLConnection conn;
   private Base64 base64;
@@ -195,9 +196,12 @@ public Void run() throws Exception {
           try {
             GSSManager gssManager = GSSManager.getInstance();
             String servicePrincipal = "HTTP/" + KerberosAuthenticator.this.url.getHost();
            
             GSSName serviceName = gssManager.createName(servicePrincipal,
                                                        GSSUtil.NT_GSS_KRB5_PRINCIPAL);
            gssContext = gssManager.createContext(serviceName, GSSUtil.GSS_KRB5_MECH_OID, null,
                                                        GSSName.NT_HOSTBASED_SERVICE);
            Oid oid = KerberosUtil.getOidClassInstance(servicePrincipal, 
                gssManager);
            gssContext = gssManager.createContext(serviceName, oid, null,
                                                   GSSContext.DEFAULT_LIFETIME);
             gssContext.requestCredDeleg(true);
             gssContext.requestMutualAuth(true);
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
index 79bff01d75a..45297851d73 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/server/KerberosAuthenticationHandler.java
@@ -15,9 +15,9 @@
 
 import org.apache.hadoop.security.authentication.client.AuthenticationException;
 import org.apache.hadoop.security.authentication.client.KerberosAuthenticator;
import com.sun.security.auth.module.Krb5LoginModule;
 import org.apache.commons.codec.binary.Base64;
 import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
 import org.ietf.jgss.GSSContext;
 import org.ietf.jgss.GSSCredential;
 import org.ietf.jgss.GSSManager;
@@ -95,7 +95,7 @@ public KerberosConfiguration(String keytab, String principal) {
       }
 
       return new AppConfigurationEntry[]{
        new AppConfigurationEntry(Krb5LoginModule.class.getName(),
          new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(),
                                   AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                                   options),};
     }
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
index ad4741a6886..63958670713 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
@@ -23,12 +23,11 @@
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
import java.lang.reflect.Method;
 
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 
import sun.security.krb5.Config;
import sun.security.krb5.KrbException;
 
 /**
  * This class implements parsing and handling of Kerberos principal names. In
@@ -77,13 +76,11 @@
   private static List<Rule> rules;
 
   private static String defaultRealm;
  private static Config kerbConf;
 
   static {
     try {
      kerbConf = Config.getInstance();
      defaultRealm = kerbConf.getDefaultRealm();
    } catch (KrbException ke) {
      defaultRealm = KerberosUtil.getDefaultRealm();
    } catch (Exception ke) {
         defaultRealm="";
     }
   }
diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java
new file mode 100644
index 00000000000..1db8b665802
-- /dev/null
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosUtil.java
@@ -0,0 +1,53 @@
package org.apache.hadoop.security.authentication.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;

public class KerberosUtil {

  /* Return the Kerberos login module name */
  public static String getKrb5LoginModuleName() {
    return System.getProperty("java.vendor").contains("IBM")
      ? "com.ibm.security.auth.module.Krb5LoginModule"
      : "com.sun.security.auth.module.Krb5LoginModule";
  }
  
  public static Oid getOidClassInstance(String servicePrincipal,
      GSSManager gssManager) 
      throws ClassNotFoundException, GSSException, NoSuchFieldException,
      IllegalAccessException {
    Class<?> oidClass;
    if (System.getProperty("java.vendor").contains("IBM")) {
      oidClass = Class.forName("com.ibm.security.jgss.GSSUtil");
    } else {
      oidClass = Class.forName("sun.security.jgss.GSSUtil");
    }
    Field oidField = oidClass.getDeclaredField("GSS_KRB5_MECH_OID");
    return (Oid)oidField.get(oidClass);
  }

  public static String getDefaultRealm() 
      throws ClassNotFoundException, NoSuchMethodException, 
      IllegalArgumentException, IllegalAccessException, 
      InvocationTargetException {
    Object kerbConf;
    Class<?> classRef;
    Method getInstanceMethod;
    Method getDefaultRealmMethod;
    if (System.getProperty("java.vendor").contains("IBM")) {
      classRef = Class.forName("com.ibm.security.krb5.internal.Config");
    } else {
      classRef = Class.forName("sun.security.krb5.Config");
    }
    getInstanceMethod = classRef.getMethod("getInstance", new Class[0]);
    kerbConf = getInstanceMethod.invoke(classRef, new Object[0]);
    getDefaultRealmMethod = classRef.getDeclaredMethod("getDefaultRealm",
         new Class[0]);
    return (String)getDefaultRealmMethod.invoke(kerbConf, new Object[0]);
  }
}
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/KerberosTestUtils.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/KerberosTestUtils.java
index 92e1de5a261..ea0f17f04cf 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/KerberosTestUtils.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/KerberosTestUtils.java
@@ -13,13 +13,15 @@
  */
 package org.apache.hadoop.security.authentication;
 
import com.sun.security.auth.module.Krb5LoginModule;
 
 import javax.security.auth.Subject;
 import javax.security.auth.kerberos.KerberosPrincipal;
 import javax.security.auth.login.AppConfigurationEntry;
 import javax.security.auth.login.Configuration;
 import javax.security.auth.login.LoginContext;

import org.apache.hadoop.security.authentication.util.KerberosUtil;

 import java.io.File;
 import java.security.Principal;
 import java.security.PrivilegedActionException;
@@ -88,7 +90,7 @@ public KerberosConfiguration(String principal) {
       options.put("debug", "true");
 
       return new AppConfigurationEntry[]{
        new AppConfigurationEntry(Krb5LoginModule.class.getName(),
        new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(),
                                   AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                                   options),};
     }
diff --git a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java
index 161839ddcd8..e6e7c9cca00 100644
-- a/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java
++ b/hadoop-common-project/hadoop-auth/src/test/java/org/apache/hadoop/security/authentication/server/TestKerberosAuthenticationHandler.java
@@ -19,14 +19,16 @@
 import junit.framework.TestCase;
 import org.apache.commons.codec.binary.Base64;
 import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
 import org.ietf.jgss.GSSContext;
 import org.ietf.jgss.GSSManager;
 import org.ietf.jgss.GSSName;
 import org.mockito.Mockito;
import sun.security.jgss.GSSUtil;
import org.ietf.jgss.Oid;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
 import java.util.Properties;
 import java.util.concurrent.Callable;
 
@@ -143,9 +145,12 @@ public String call() throws Exception {
         GSSContext gssContext = null;
         try {
           String servicePrincipal = KerberosTestUtils.getServerPrincipal();
          GSSName serviceName = gssManager.createName(servicePrincipal, GSSUtil.NT_GSS_KRB5_PRINCIPAL);
          gssContext = gssManager.createContext(serviceName, GSSUtil.GSS_KRB5_MECH_OID, null,
                                                GSSContext.DEFAULT_LIFETIME);
          GSSName serviceName = gssManager.createName(servicePrincipal,
              GSSName.NT_HOSTBASED_SERVICE);
          Oid oid = KerberosUtil.getOidClassInstance(servicePrincipal, 
              gssManager);
          gssContext = gssManager.createContext(serviceName, oid, null,
                                                  GSSContext.DEFAULT_LIFETIME);
           gssContext.requestCredDeleg(true);
           gssContext.requestMutualAuth(true);
 
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 80a0df2ffc0..acf3a3db9f6 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -190,6 +190,9 @@ Release 0.23.3 - UNRELEASED
 
     HADOOP-7806. Support binding to sub-interfaces (eli)
 
    HADOOP-6941. Adds support for building Hadoop with IBM's JDK 
    (Stephen Watt, Eli and ddas)

   OPTIMIZATIONS
 
   BUG FIXES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java
index 2166acc7a31..c5eb7dd420a 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java
@@ -24,9 +24,7 @@
 import org.apache.hadoop.classification.InterfaceStability;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.security.authentication.util.KerberosName;

import sun.security.krb5.Config;
import sun.security.krb5.KrbException;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
 
 /**
  * This class implements parsing and handling of Kerberos principal names. In 
@@ -40,8 +38,8 @@
 
   static {
     try {
      Config.getInstance().getDefaultRealm();
    } catch (KrbException ke) {
      KerberosUtil.getDefaultRealm();
    } catch (Exception ke) {
       if(UserGroupInformation.isSecurityEnabled())
         throw new IllegalArgumentException("Can't get Kerberos configuration",ke);
     }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
index 13ea2e971ad..ad982bc56d3 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
@@ -17,6 +17,10 @@
 package org.apache.hadoop.security;
 
 import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.URI;
@@ -49,9 +53,6 @@
 //this will need to be replaced someday when there is a suitable replacement
 import sun.net.dns.ResolverConfiguration;
 import sun.net.util.IPAddressUtil;
import sun.security.jgss.krb5.Krb5Util;
import sun.security.krb5.Credentials;
import sun.security.krb5.PrincipalName;
 
 @InterfaceAudience.LimitedPrivate({"HDFS", "MapReduce"})
 @InterfaceStability.Evolving
@@ -155,12 +156,41 @@ public static void fetchServiceTicket(URL remoteHost) throws IOException {
     String serviceName = "host/" + remoteHost.getHost();
     if (LOG.isDebugEnabled())
       LOG.debug("Fetching service ticket for host at: " + serviceName);
    Credentials serviceCred = null;
    Object serviceCred = null;
    Method credsToTicketMeth;
    Class<?> krb5utilClass;
     try {
      PrincipalName principal = new PrincipalName(serviceName,
          PrincipalName.KRB_NT_SRV_HST);
      serviceCred = Credentials.acquireServiceCreds(principal
          .toString(), Krb5Util.ticketToCreds(getTgtFromSubject()));
      Class<?> principalClass;
      Class<?> credentialsClass;
      
      if (System.getProperty("java.vendor").contains("IBM")) {
        principalClass = Class.forName("com.ibm.security.krb5.PrincipalName");
        
        credentialsClass = Class.forName("com.ibm.security.krb5.Credentials");
        krb5utilClass = Class.forName("com.ibm.security.jgss.mech.krb5");
      } else {
        principalClass = Class.forName("sun.security.krb5.PrincipalName");
        credentialsClass = Class.forName("sun.security.krb5.Credentials");
        krb5utilClass = Class.forName("sun.security.jgss.krb5");
      }
      @SuppressWarnings("rawtypes")
      Constructor principalConstructor = principalClass.getConstructor(String.class, 
          int.class);
      Field KRB_NT_SRV_HST = principalClass.getDeclaredField("KRB_NT_SRV_HST");
      Method acquireServiceCredsMeth = 
          credentialsClass.getDeclaredMethod("acquireServiceCreds", 
              String.class, credentialsClass);
      Method ticketToCredsMeth = krb5utilClass.getDeclaredMethod("ticketToCreds", 
          KerberosTicket.class);
      credsToTicketMeth = krb5utilClass.getDeclaredMethod("credsToTicket", 
          credentialsClass);
      
      Object principal = principalConstructor.newInstance(serviceName,
          KRB_NT_SRV_HST.get(principalClass));
      
      serviceCred = acquireServiceCredsMeth.invoke(credentialsClass, 
          principal.toString(), 
          ticketToCredsMeth.invoke(krb5utilClass, getTgtFromSubject()));
     } catch (Exception e) {
       throw new IOException("Can't get service ticket for: "
           + serviceName, e);
@@ -168,8 +198,13 @@ public static void fetchServiceTicket(URL remoteHost) throws IOException {
     if (serviceCred == null) {
       throw new IOException("Can't get service ticket for " + serviceName);
     }
    Subject.getSubject(AccessController.getContext()).getPrivateCredentials()
        .add(Krb5Util.credsToTicket(serviceCred));
    try {
      Subject.getSubject(AccessController.getContext()).getPrivateCredentials()
          .add(credsToTicketMeth.invoke(krb5utilClass, serviceCred));
    } catch (Exception e) {
      throw new IOException("Can't get service ticket for: "
          + serviceName, e);
    }
   }
   
   /**
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
index d595b5a8027..8285334739d 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -58,14 +58,11 @@
 import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
 import org.apache.hadoop.metrics2.lib.MutableRate;
 import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
 import org.apache.hadoop.security.token.Token;
 import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.util.Shell;
 
import com.sun.security.auth.NTUserPrincipal;
import com.sun.security.auth.UnixPrincipal;
import com.sun.security.auth.module.Krb5LoginModule;

 /**
  * User and group information for Hadoop.
  * This class wraps around a JAAS Subject and provides methods to determine the
@@ -289,20 +286,51 @@ public static boolean isSecurityEnabled() {
   private final boolean isKeytab;
   private final boolean isKrbTkt;
   
  private static final String OS_LOGIN_MODULE_NAME;
  private static final Class<? extends Principal> OS_PRINCIPAL_CLASS;
  private static String OS_LOGIN_MODULE_NAME;
  private static Class<? extends Principal> OS_PRINCIPAL_CLASS;
   private static final boolean windows = 
                            System.getProperty("os.name").startsWith("Windows");
  static {
    if (windows) {
      OS_LOGIN_MODULE_NAME = "com.sun.security.auth.module.NTLoginModule";
      OS_PRINCIPAL_CLASS = NTUserPrincipal.class;
  /* Return the OS login module class name */
  private static String getOSLoginModuleName() {
    if (System.getProperty("java.vendor").contains("IBM")) {
      return windows ? "com.ibm.security.auth.module.NTLoginModule"
       : "com.ibm.security.auth.module.LinuxLoginModule";
     } else {
      OS_LOGIN_MODULE_NAME = "com.sun.security.auth.module.UnixLoginModule";
      OS_PRINCIPAL_CLASS = UnixPrincipal.class;
      return windows ? "com.sun.security.auth.module.NTLoginModule"
        : "com.sun.security.auth.module.UnixLoginModule";
     }
   }
  

  /* Return the OS principal class */
  @SuppressWarnings("unchecked")
  private static Class<? extends Principal> getOsPrincipalClass() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    try {
      if (System.getProperty("java.vendor").contains("IBM")) {
        if (windows) {
          return (Class<? extends Principal>)
            cl.loadClass("com.ibm.security.auth.UsernamePrincipal");
        } else {
          return (Class<? extends Principal>)
            (System.getProperty("os.arch").contains("64")
             ? cl.loadClass("com.ibm.security.auth.UsernamePrincipal")
             : cl.loadClass("com.ibm.security.auth.LinuxPrincipal"));
        }
      } else {
        return (Class<? extends Principal>) (windows
           ? cl.loadClass("com.sun.security.auth.NTUserPrincipal")
           : cl.loadClass("com.sun.security.auth.UnixPrincipal"));
      }
    } catch (ClassNotFoundException e) {
      LOG.error("Unable to find JAAS classes:" + e.getMessage());
    }
    return null;
  }
  static {
    OS_LOGIN_MODULE_NAME = getOSLoginModuleName();
    OS_PRINCIPAL_CLASS = getOsPrincipalClass();
  }

   private static class RealUser implements Principal {
     private final UserGroupInformation realUser;
     
@@ -382,7 +410,7 @@ public String toString() {
       USER_KERBEROS_OPTIONS.putAll(BASIC_JAAS_OPTIONS);
     }
     private static final AppConfigurationEntry USER_KERBEROS_LOGIN =
      new AppConfigurationEntry(Krb5LoginModule.class.getName(),
      new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(),
                                 LoginModuleControlFlag.OPTIONAL,
                                 USER_KERBEROS_OPTIONS);
     private static final Map<String,String> KEYTAB_KERBEROS_OPTIONS = 
@@ -395,7 +423,7 @@ public String toString() {
       KEYTAB_KERBEROS_OPTIONS.putAll(BASIC_JAAS_OPTIONS);      
     }
     private static final AppConfigurationEntry KEYTAB_KERBEROS_LOGIN =
      new AppConfigurationEntry(Krb5LoginModule.class.getName(),
      new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(),
                                 LoginModuleControlFlag.REQUIRED,
                                 KEYTAB_KERBEROS_OPTIONS);
     
- 
2.19.1.windows.1

