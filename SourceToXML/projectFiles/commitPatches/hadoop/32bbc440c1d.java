From 32bbc440c1d7a8c7b261b4778af30f7e1a2ccead Mon Sep 17 00:00:00 2001
From: Jing Zhao <jing9@apache.org>
Date: Wed, 9 Apr 2014 04:51:16 +0000
Subject: [PATCH] HADOOP-10475. ConcurrentModificationException in
 AbstractDelegationTokenSelector.selectToken(). Contributed by Jing Zhao.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1585888 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt       |  3 +++
 .../java/org/apache/hadoop/security/Credentials.java  |  1 -
 .../apache/hadoop/security/UserGroupInformation.java  | 11 ++++++-----
 3 files changed, 9 insertions(+), 6 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 0ccadbcfad3..128964650d6 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -352,6 +352,9 @@ Release 2.5.0 - UNRELEASED
     HADOOP-10468. TestMetricsSystemImpl.testMultiThreadedPublish fails
     intermediately. (wheat9)
 
    HADOOP-10475. ConcurrentModificationException in
    AbstractDelegationTokenSelector.selectToken(). (jing9)

 Release 2.4.1 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
index b796743eaa1..b81e810f191 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/Credentials.java
@@ -31,7 +31,6 @@
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
import java.util.Map.Entry;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
index cd553decfc9..cee4e117508 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -17,9 +17,10 @@
  */
 package org.apache.hadoop.security;
 
import static org.apache.hadoop.fs.CommonConfigurationKeys.HADOOP_KERBEROS_MIN_SECONDS_BEFORE_RELOGIN;
import static org.apache.hadoop.fs.CommonConfigurationKeys.HADOOP_KERBEROS_MIN_SECONDS_BEFORE_RELOGIN_DEFAULT;
 import static org.apache.hadoop.fs.CommonConfigurationKeys.HADOOP_USER_GROUP_METRICS_PERCENTILES_INTERVALS;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_KERBEROS_MIN_SECONDS_BEFORE_RELOGIN;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_KERBEROS_MIN_SECONDS_BEFORE_RELOGIN_DEFAULT;
import static org.apache.hadoop.util.PlatformName.IBM_JAVA;
 
 import java.io.File;
 import java.io.IOException;
@@ -30,6 +31,7 @@
 import java.security.PrivilegedAction;
 import java.security.PrivilegedActionException;
 import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
@@ -45,9 +47,9 @@
 import javax.security.auth.kerberos.KerberosPrincipal;
 import javax.security.auth.kerberos.KerberosTicket;
 import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
 import javax.security.auth.login.LoginContext;
 import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
 import javax.security.auth.spi.LoginModule;
 
 import org.apache.commons.logging.Log;
@@ -68,7 +70,6 @@
 import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.util.Shell;
 import org.apache.hadoop.util.Time;
import static org.apache.hadoop.util.PlatformName.IBM_JAVA;
 
 import com.google.common.annotations.VisibleForTesting;
 
@@ -1415,7 +1416,7 @@ public synchronized boolean addToken(Text alias,
   public synchronized
   Collection<Token<? extends TokenIdentifier>> getTokens() {
     return Collections.unmodifiableCollection(
        getCredentialsInternal().getAllTokens());
        new ArrayList<Token<?>>(getCredentialsInternal().getAllTokens()));
   }
 
   /**
- 
2.19.1.windows.1

