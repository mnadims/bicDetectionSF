From 776eb119acc9c79deb9ca2f76712a84470d2fac9 Mon Sep 17 00:00:00 2001
From: Suresh Srinivas <suresh@apache.org>
Date: Fri, 3 May 2013 03:36:51 +0000
Subject: [PATCH] HADOOP-9523. Provide a generic IBM java vendor flag in
 PlatformName.java to support non-Sun JREs. Contributed by Tian Hong Wang.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1478634 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                  |  3 +++
 .../apache/hadoop/io/compress/GzipCodec.java   |  4 ++--
 .../hadoop/security/UserGroupInformation.java  | 14 +++++++-------
 .../apache/hadoop/security/ssl/SSLFactory.java |  5 ++---
 .../org/apache/hadoop/util/PlatformName.java   | 18 +++++++++++-------
 .../apache/hadoop/conf/TestConfiguration.java  |  4 ++--
 6 files changed, 27 insertions(+), 21 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 04b862e4e61..5d92c600d00 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -594,6 +594,9 @@ Release 2.0.5-beta - UNRELEASED
     HADOOP-9322. LdapGroupsMapping doesn't seem to set a timeout for
     its directory search. (harsh)
 
    HADOOP-9523. Provide a generic IBM java vendor flag in PlatformName.java
    to support non-Sun JREs. (Tian Hong Wang via suresh)

   OPTIMIZATIONS
 
     HADOOP-9150. Avoid unnecessary DNS resolution attempts for logical URIs
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/compress/GzipCodec.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/compress/GzipCodec.java
index 6ac692c14e7..293dabe7c0b 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/compress/GzipCodec.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/compress/GzipCodec.java
@@ -25,6 +25,7 @@
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.io.compress.DefaultCodec;
 import org.apache.hadoop.io.compress.zlib.*;
import static org.apache.hadoop.util.PlatformName.IBM_JAVA;
 
 /**
  * This class creates gzip compressors/decompressors. 
@@ -41,10 +42,9 @@
 
     private static class ResetableGZIPOutputStream extends GZIPOutputStream {
       private static final int TRAILER_SIZE = 8;
      public static final String JVMVendor= System.getProperty("java.vendor");
       public static final String JVMVersion= System.getProperty("java.version");
       private static final boolean HAS_BROKEN_FINISH =
          (JVMVendor.contains("IBM") && JVMVersion.contains("1.6.0"));
          (IBM_JAVA && JVMVersion.contains("1.6.0"));
 
       public ResetableGZIPOutputStream(OutputStream out) throws IOException {
         super(out);
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
index 8a22a6f8b08..990b31c9300 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -64,6 +64,7 @@
 import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.util.Shell;
 import org.apache.hadoop.util.Time;
import static org.apache.hadoop.util.PlatformName.IBM_JAVA;
 
 import com.google.common.annotations.VisibleForTesting;
 
@@ -306,12 +307,11 @@ private static boolean isAuthenticationMethodEnabled(AuthenticationMethod method
       System.getProperty("os.name").startsWith("Windows");
   private static final boolean is64Bit =
       System.getProperty("os.arch").contains("64");
  private static final boolean ibmJava = System.getProperty("java.vendor").contains("IBM");
   private static final boolean aix = System.getProperty("os.name").equals("AIX");
 
   /* Return the OS login module class name */
   private static String getOSLoginModuleName() {
    if (ibmJava) {
    if (IBM_JAVA) {
       if (windows) {
         return is64Bit ? "com.ibm.security.auth.module.Win64LoginModule"
             : "com.ibm.security.auth.module.NTLoginModule";
@@ -333,7 +333,7 @@ private static String getOSLoginModuleName() {
     ClassLoader cl = ClassLoader.getSystemClassLoader();
     try {
       String principalClass = null;
      if (ibmJava) {
      if (IBM_JAVA) {
         if (is64Bit) {
           principalClass = "com.ibm.security.auth.UsernamePrincipal";
         } else {
@@ -430,7 +430,7 @@ public String toString() {
     private static final Map<String,String> USER_KERBEROS_OPTIONS = 
       new HashMap<String,String>();
     static {
      if (ibmJava) {
      if (IBM_JAVA) {
         USER_KERBEROS_OPTIONS.put("useDefaultCcache", "true");
       } else {
         USER_KERBEROS_OPTIONS.put("doNotPrompt", "true");
@@ -439,7 +439,7 @@ public String toString() {
       }
       String ticketCache = System.getenv("KRB5CCNAME");
       if (ticketCache != null) {
        if (ibmJava) {
        if (IBM_JAVA) {
           // The first value searched when "useDefaultCcache" is used.
           System.setProperty("KRB5CCNAME", ticketCache);
         } else {
@@ -455,7 +455,7 @@ public String toString() {
     private static final Map<String,String> KEYTAB_KERBEROS_OPTIONS = 
       new HashMap<String,String>();
     static {
      if (ibmJava) {
      if (IBM_JAVA) {
         KEYTAB_KERBEROS_OPTIONS.put("credsType", "both");
       } else {
         KEYTAB_KERBEROS_OPTIONS.put("doNotPrompt", "true");
@@ -487,7 +487,7 @@ public String toString() {
       } else if (USER_KERBEROS_CONFIG_NAME.equals(appName)) {
         return USER_KERBEROS_CONF;
       } else if (KEYTAB_KERBEROS_CONFIG_NAME.equals(appName)) {
        if (ibmJava) {
        if (IBM_JAVA) {
           KEYTAB_KERBEROS_OPTIONS.put("useKeytab",
               prependFileAuthority(keytabFile));
         } else {
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/ssl/SSLFactory.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/ssl/SSLFactory.java
index 7f82f6aab3c..c118948c82d 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/ssl/SSLFactory.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/ssl/SSLFactory.java
@@ -22,6 +22,7 @@
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.security.authentication.client.ConnectionConfigurator;
 import org.apache.hadoop.util.ReflectionUtils;
import static org.apache.hadoop.util.PlatformName.IBM_JAVA;
 
 import javax.net.ssl.HostnameVerifier;
 import javax.net.ssl.HttpsURLConnection;
@@ -58,9 +59,7 @@
     "hadoop.ssl.client.conf";
   public static final String SSL_SERVER_CONF_KEY =
     "hadoop.ssl.server.conf";
  private static final boolean IBMJAVA = 
      System.getProperty("java.vendor").contains("IBM");
  public static final String SSLCERTIFICATE = IBMJAVA?"ibmX509":"SunX509"; 
  public static final String SSLCERTIFICATE = IBM_JAVA?"ibmX509":"SunX509"; 
 
   public static final boolean DEFAULT_SSL_REQUIRE_CLIENT_CERT = false;
 
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java
index 43a5e8970a6..819a9216bd9 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java
@@ -32,20 +32,24 @@
    * The complete platform 'name' to identify the platform as 
    * per the java-vm.
    */
  private static final String platformName =
  public static final String PLATFORM_NAME =
       (Shell.WINDOWS ? System.getenv("os") : System.getProperty("os.name"))
       + "-" + System.getProperty("os.arch")
       + "-" + System.getProperty("sun.arch.data.model");
   
   /**
   * Get the complete platform as per the java-vm.
   * @return returns the complete platform as per the java-vm.
   * The java vendor name used in this platform. 
    */
  public static String getPlatformName() {
    return platformName;
  }
  public static final String JAVA_VENDOR_NAME = System.getProperty("java.vendor");

  /**
   * A public static variable to indicate the current java vendor is 
   * IBM java or not. 
   */
  public static final boolean IBM_JAVA = JAVA_VENDOR_NAME.contains("IBM");
   
   public static void main(String[] args) {
    System.out.println(platformName);
    System.out.println("platform name: " + PLATFORM_NAME);
    System.out.println("java vendor name: " + JAVA_VENDOR_NAME);
   }
 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
index cc5903ffea2..ee0c9de7732 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
@@ -44,6 +44,7 @@
 import org.apache.hadoop.conf.Configuration.IntegerRanges;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.net.NetUtils;
import static org.apache.hadoop.util.PlatformName.IBM_JAVA;
 import org.codehaus.jackson.map.ObjectMapper; 
 
 public class TestConfiguration extends TestCase {
@@ -52,9 +53,8 @@
   final static String CONFIG = new File("./test-config.xml").getAbsolutePath();
   final static String CONFIG2 = new File("./test-config2.xml").getAbsolutePath();
   final static Random RAN = new Random();
  final static boolean IBMJAVA = System.getProperty("java.vendor").contains("IBM"); 
   final static String XMLHEADER = 
            IBMJAVA?"<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration>":
            IBM_JAVA?"<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration>":
   "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><configuration>";
 
   @Override
- 
2.19.1.windows.1

