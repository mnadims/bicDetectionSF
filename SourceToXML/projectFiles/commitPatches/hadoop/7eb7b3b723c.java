From 7eb7b3b723c524ece8ef2247943eb631fefcfe41 Mon Sep 17 00:00:00 2001
From: Daryn Sharp <daryn@apache.org>
Date: Mon, 18 Mar 2013 13:46:52 +0000
Subject: [PATCH] HADOOP-9299.  kerberos name resolution is kicking in even
 when kerberos is not configured (daryn)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1457763 13f79535-47bb-0310-9956-ffa450edef68
--
 .../authentication/util/KerberosName.java     |  18 ++-
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../hadoop/security/HadoopKerberosName.java   |  30 ++--
 .../hadoop/security/UserGroupInformation.java |  46 +++---
 .../security/TestUserGroupInformation.java    | 147 ++++++++++++++++--
 5 files changed, 191 insertions(+), 53 deletions(-)

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
index 6ff30f78909..6c511869c0f 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/KerberosName.java
@@ -383,9 +383,25 @@ public String getShortName() throws IOException {
    * @param ruleString the rules string.
    */
   public static void setRules(String ruleString) {
    rules = parseRules(ruleString);
    rules = (ruleString != null) ? parseRules(ruleString) : null;
   }
 
  /**
   * Get the rules.
   * @return String of configured rules, or null if not yet configured
   */
  public static String getRules() {
    String ruleString = null;
    if (rules != null) {
      StringBuilder sb = new StringBuilder();
      for (Rule rule : rules) {
        sb.append(rule.toString()).append("\n");
      }
      ruleString = sb.toString().trim();
    }
    return ruleString;
  }
  
   /**
    * Indicates if the name rules have been set.
    * 
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 1cb8ecbd3b8..85540ccdfde 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -571,6 +571,9 @@ Release 2.0.5-beta - UNRELEASED
     HADOOP-9407. commons-daemon 1.0.3 dependency has bad group id causing
     build issues. (Sangjin Lee via suresh)
 
    HADOOP-9299.  kerberos name resolution is kicking in even when kerberos
    is not configured (daryn)

 Release 2.0.4-alpha - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java
index 00ef5d7a357..55b27868571 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/HadoopKerberosName.java
@@ -18,6 +18,8 @@
 
 package org.apache.hadoop.security;
 
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTH_TO_LOCAL;

 import java.io.IOException;
 
 import org.apache.hadoop.classification.InterfaceAudience;
@@ -25,7 +27,6 @@
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.security.authentication.util.KerberosName;
 import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
 /**
  * This class implements parsing and handling of Kerberos principal names. In 
  * particular, it splits them apart and translates them down into local
@@ -36,15 +37,6 @@
 @InterfaceStability.Evolving
 public class HadoopKerberosName extends KerberosName {
 
  static {
    try {
      KerberosUtil.getDefaultRealm();
    } catch (Exception ke) {
      if(UserGroupInformation.isSecurityEnabled())
        throw new IllegalArgumentException("Can't get Kerberos configuration",ke);
    }
  }

   /**
    * Create a name from the full Kerberos principal name.
    * @param name
@@ -63,7 +55,23 @@ public HadoopKerberosName(String name) {
    * @throws IOException
    */
   public static void setConfiguration(Configuration conf) throws IOException {
    String ruleString = conf.get(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTH_TO_LOCAL, "DEFAULT");
    final String defaultRule;
    switch (SecurityUtil.getAuthenticationMethod(conf)) {
      case KERBEROS:
      case KERBEROS_SSL:
        try {
          KerberosUtil.getDefaultRealm();
        } catch (Exception ke) {
          throw new IllegalArgumentException("Can't get Kerberos realm", ke);
        }
        defaultRule = "DEFAULT";
        break;
      default:
        // just extract the simple user name
        defaultRule = "RULE:[1:$1] RULE:[2:$1]";
        break; 
    }
    String ruleString = conf.get(HADOOP_SECURITY_AUTH_TO_LOCAL, defaultRule);
     setRules(ruleString);
   }
 
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
index f2c74d8f654..8a22a6f8b08 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -53,14 +53,12 @@
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.metrics2.annotation.Metric;
 import org.apache.hadoop.metrics2.annotation.Metrics;
 import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
 import org.apache.hadoop.metrics2.lib.MutableRate;
 import org.apache.hadoop.security.SaslRpcServer.AuthMethod;
import org.apache.hadoop.security.authentication.util.KerberosName;
 import org.apache.hadoop.security.authentication.util.KerberosUtil;
 import org.apache.hadoop.security.token.Token;
 import org.apache.hadoop.security.token.TokenIdentifier;
@@ -192,8 +190,6 @@ public boolean logout() throws LoginException {
 
   /** Metrics to track UGI activity */
   static UgiMetrics metrics = UgiMetrics.create();
  /** Are the static variables that depend on configuration initialized? */
  private static boolean isInitialized = false;
   /** The auth method to use */
   private static AuthenticationMethod authenticationMethod;
   /** Server-side groups fetching service */
@@ -213,8 +209,8 @@ public boolean logout() throws LoginException {
    * Must be called before useKerberos or groups is used.
    */
   private static synchronized void ensureInitialized() {
    if (!isInitialized) {
        initialize(new Configuration(), KerberosName.hasRulesBeenSet());
    if (conf == null) {
      initialize(new Configuration(), false);
     }
   }
 
@@ -222,25 +218,17 @@ private static synchronized void ensureInitialized() {
    * Initialize UGI and related classes.
    * @param conf the configuration to use
    */
  private static synchronized void initialize(Configuration conf, boolean skipRulesSetting) {
    initUGI(conf);
    // give the configuration on how to translate Kerberos names
    try {
      if (!skipRulesSetting) {
  private static synchronized void initialize(Configuration conf,
                                              boolean overrideNameRules) {
    authenticationMethod = SecurityUtil.getAuthenticationMethod(conf);
    if (overrideNameRules || !HadoopKerberosName.hasRulesBeenSet()) {
      try {
         HadoopKerberosName.setConfiguration(conf);
      } catch (IOException ioe) {
        throw new RuntimeException(
            "Problem with Kerberos auth_to_local name configuration", ioe);
       }
    } catch (IOException ioe) {
      throw new RuntimeException("Problem with Kerberos auth_to_local name " +
          "configuration", ioe);
     }
  }
  
  /**
   * Set the configuration values for UGI.
   * @param conf the configuration to use
   */
  private static synchronized void initUGI(Configuration conf) {
    authenticationMethod = SecurityUtil.getAuthenticationMethod(conf);
     try {
         kerberosMinSecondsBeforeRelogin = 1000L * conf.getLong(
                 HADOOP_KERBEROS_MIN_SECONDS_BEFORE_RELOGIN,
@@ -255,7 +243,6 @@ private static synchronized void initUGI(Configuration conf) {
     if (!(groups instanceof TestingGroups)) {
       groups = Groups.getUserToGroupsMappingService(conf);
     }
    isInitialized = true;
     UserGroupInformation.conf = conf;
   }
 
@@ -268,7 +255,18 @@ private static synchronized void initUGI(Configuration conf) {
   @InterfaceAudience.Public
   @InterfaceStability.Evolving
   public static void setConfiguration(Configuration conf) {
    initialize(conf, false);
    initialize(conf, true);
  }
  
  @InterfaceAudience.Private
  @VisibleForTesting
  static void reset() {
    authenticationMethod = null;
    conf = null;
    groups = null;
    kerberosMinSecondsBeforeRelogin = 0;
    setLoginUser(null);
    HadoopKerberosName.setRules(null);
   }
   
   /**
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
index 12f4b313ecd..fd23e965536 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
@@ -38,10 +38,12 @@
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.metrics2.MetricsRecordBuilder;
 import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
import org.apache.hadoop.security.authentication.util.KerberosName;
 import org.apache.hadoop.security.token.Token;
 import org.apache.hadoop.security.token.TokenIdentifier;
 import static org.apache.hadoop.test.MetricsAsserts.*;
 import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTH_TO_LOCAL;
 import org.apache.hadoop.util.Shell;
 
 public class TestUserGroupInformation {
@@ -73,17 +75,18 @@
   public static void setup() {
     javax.security.auth.login.Configuration.setConfiguration(
         new DummyLoginConfiguration());
    // doesn't matter what it is, but getGroups needs it set...
    System.setProperty("hadoop.home.dir", "/tmp");
    // fake the realm is kerberos is enabled
    System.setProperty("java.security.krb5.kdc", "");
    System.setProperty("java.security.krb5.realm", "DEFAULT.REALM");
   }
   
   @Before
   public void setupUgi() {
     conf = new Configuration();
    conf.set(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTH_TO_LOCAL,
        "RULE:[2:$1@$0](.*@HADOOP.APACHE.ORG)s/@.*//" +
        "RULE:[1:$1@$0](.*@HADOOP.APACHE.ORG)s/@.*//"
        + "DEFAULT");
    UserGroupInformation.reset();
     UserGroupInformation.setConfiguration(conf);
    UserGroupInformation.setLoginUser(null);
   }
   
   @After
@@ -230,28 +233,138 @@ public Object run() throws IOException {
   /** test constructor */
   @Test (timeout = 30000)
   public void testConstructor() throws Exception {
    UserGroupInformation ugi = 
      UserGroupInformation.createUserForTesting("user2/cron@HADOOP.APACHE.ORG", 
                                                GROUP_NAMES);
    // make sure the short and full user names are correct
    assertEquals("user2/cron@HADOOP.APACHE.ORG", ugi.getUserName());
    assertEquals("user2", ugi.getShortUserName());
    ugi = UserGroupInformation.createUserForTesting(USER_NAME, GROUP_NAMES);
    assertEquals("user1", ugi.getShortUserName());
    // security off, so default should just return simple name
    testConstructorSuccess("user1", "user1");
    testConstructorSuccess("user2@DEFAULT.REALM", "user2");
    testConstructorSuccess("user3/cron@DEFAULT.REALM", "user3");    
    testConstructorSuccess("user4@OTHER.REALM", "user4");
    testConstructorSuccess("user5/cron@OTHER.REALM", "user5");
    // failure test
    testConstructorFailures(null);
    testConstructorFailures("");
  }
  
  /** test constructor */
  @Test (timeout = 30000)
  public void testConstructorWithRules() throws Exception {
    // security off, but use rules if explicitly set
    conf.set(HADOOP_SECURITY_AUTH_TO_LOCAL,
        "RULE:[1:$1@$0](.*@OTHER.REALM)s/(.*)@.*/other-$1/");
    UserGroupInformation.setConfiguration(conf);
    testConstructorSuccess("user1", "user1");
    testConstructorSuccess("user4@OTHER.REALM", "other-user4");
    // failure test
    testConstructorFailures("user2@DEFAULT.REALM");
    testConstructorFailures("user3/cron@DEFAULT.REALM");
    testConstructorFailures("user5/cron@OTHER.REALM");
    testConstructorFailures(null);
    testConstructorFailures("");
  }
  
  /** test constructor */
  @Test (timeout = 30000)
  public void testConstructorWithKerberos() throws Exception {
    // security on, default is remove default realm
    SecurityUtil.setAuthenticationMethod(AuthenticationMethod.KERBEROS, conf);
    UserGroupInformation.setConfiguration(conf);

    testConstructorSuccess("user1", "user1");
    testConstructorSuccess("user2@DEFAULT.REALM", "user2");
    testConstructorSuccess("user3/cron@DEFAULT.REALM", "user3");    
    // failure test
    testConstructorFailures("user4@OTHER.REALM");
    testConstructorFailures("user5/cron@OTHER.REALM");
    testConstructorFailures(null);
    testConstructorFailures("");
  }

  /** test constructor */
  @Test (timeout = 30000)
  public void testConstructorWithKerberosRules() throws Exception {
    // security on, explicit rules
    SecurityUtil.setAuthenticationMethod(AuthenticationMethod.KERBEROS, conf);
    conf.set(HADOOP_SECURITY_AUTH_TO_LOCAL,
        "RULE:[2:$1@$0](.*@OTHER.REALM)s/(.*)@.*/other-$1/" +
        "RULE:[1:$1@$0](.*@OTHER.REALM)s/(.*)@.*/other-$1/" +
        "DEFAULT");
    UserGroupInformation.setConfiguration(conf);
     
    testConstructorSuccess("user1", "user1");
    testConstructorSuccess("user2@DEFAULT.REALM", "user2");
    testConstructorSuccess("user3/cron@DEFAULT.REALM", "user3");    
    testConstructorSuccess("user4@OTHER.REALM", "other-user4");
    testConstructorSuccess("user5/cron@OTHER.REALM", "other-user5");
     // failure test
     testConstructorFailures(null);
     testConstructorFailures("");
   }
 
  private void testConstructorSuccess(String principal, String shortName) {
    UserGroupInformation ugi = 
        UserGroupInformation.createUserForTesting(principal, GROUP_NAMES);
    // make sure the short and full user names are correct
    assertEquals(principal, ugi.getUserName());
    assertEquals(shortName, ugi.getShortUserName());
  }
  
   private void testConstructorFailures(String userName) {
    boolean gotException = false;
     try {
       UserGroupInformation.createRemoteUser(userName);
    } catch (Exception e) {
      gotException = true;
      fail("user:"+userName+" wasn't invalid");
    } catch (IllegalArgumentException e) {
      String expect = (userName == null || userName.isEmpty())
          ? "Null user" : "Illegal principal name "+userName;
      assertEquals(expect, e.getMessage());
     }
    assertTrue(gotException);
  }

  @Test (timeout = 30000)
  public void testSetConfigWithRules() {
    String[] rules = { "RULE:[1:TEST1]", "RULE:[1:TEST2]", "RULE:[1:TEST3]" };

    // explicitly set a rule
    UserGroupInformation.reset();
    assertFalse(KerberosName.hasRulesBeenSet());
    KerberosName.setRules(rules[0]);
    assertTrue(KerberosName.hasRulesBeenSet());
    assertEquals(rules[0], KerberosName.getRules());

    // implicit init should honor rules already being set
    UserGroupInformation.createUserForTesting("someone", new String[0]);
    assertEquals(rules[0], KerberosName.getRules());

    // set conf, should override
    conf.set(HADOOP_SECURITY_AUTH_TO_LOCAL, rules[1]);
    UserGroupInformation.setConfiguration(conf);
    assertEquals(rules[1], KerberosName.getRules());

    // set conf, should again override
    conf.set(HADOOP_SECURITY_AUTH_TO_LOCAL, rules[2]);
    UserGroupInformation.setConfiguration(conf);
    assertEquals(rules[2], KerberosName.getRules());
    
    // implicit init should honor rules already being set
    UserGroupInformation.createUserForTesting("someone", new String[0]);
    assertEquals(rules[2], KerberosName.getRules());
  }

  @Test (timeout = 30000)
  public void testEnsureInitWithRules() throws IOException {
    String rules = "RULE:[1:RULE1]";

    // trigger implicit init, rules should init
    UserGroupInformation.reset();
    assertFalse(KerberosName.hasRulesBeenSet());
    UserGroupInformation.createUserForTesting("someone", new String[0]);
    assertTrue(KerberosName.hasRulesBeenSet());
    
    // set a rule, trigger implicit init, rule should not change 
    UserGroupInformation.reset();
    KerberosName.setRules(rules);
    assertTrue(KerberosName.hasRulesBeenSet());
    assertEquals(rules, KerberosName.getRules());
    UserGroupInformation.createUserForTesting("someone", new String[0]);
    assertEquals(rules, KerberosName.getRules());
   }
 
   @Test (timeout = 30000)
- 
2.19.1.windows.1

