From f5ef2db5003f3f75a236f9b3ff2d1d4469ca073e Mon Sep 17 00:00:00 2001
From: Suresh Srinivas <suresh@apache.org>
Date: Sat, 27 Apr 2013 15:44:33 +0000
Subject: [PATCH] HADOOP-9500. TestUserGroupInformation#testGetServerSideGroups
 fails on Windows due to failure to find winutils.exe. Contributed by Chris
 Nauroth.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1476606 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt               | 3 +++
 .../org/apache/hadoop/security/TestUserGroupInformation.java  | 4 +++-
 2 files changed, 6 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index c1611a29dd7..dfc30e4b577 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -522,6 +522,9 @@ Trunk (Unreleased)
 
     HADOOP-9290. Some tests cannot load native library on windows.
     (Chris Nauroth via suresh)

    HADOOP-9500. TestUserGroupInformation#testGetServerSideGroups fails on 
    Windows due to failure to find winutils.exe. (Chris Nauroth via suresh)
     
 Release 2.0.5-beta - UNRELEASED
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
index fd23e965536..aa40cf48bd9 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
@@ -76,7 +76,9 @@ public static void setup() {
     javax.security.auth.login.Configuration.setConfiguration(
         new DummyLoginConfiguration());
     // doesn't matter what it is, but getGroups needs it set...
    System.setProperty("hadoop.home.dir", "/tmp");
    // use HADOOP_HOME environment variable to prevent interfering with logic
    // that finds winutils.exe
    System.setProperty("hadoop.home.dir", System.getenv("HADOOP_HOME"));
     // fake the realm is kerberos is enabled
     System.setProperty("java.security.krb5.kdc", "");
     System.setProperty("java.security.krb5.realm", "DEFAULT.REALM");
- 
2.19.1.windows.1

