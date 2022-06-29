From c9ace69cff4a626c9e1c21181cd2b957ae2a3fb0 Mon Sep 17 00:00:00 2001
From: Suresh Srinivas <suresh@apache.org>
Date: Wed, 15 May 2013 07:42:06 +0000
Subject: [PATCH] HADOOP-9563. Fix incompatibility introduced by HADOOP-9523.
 Contributed by Tian Hong Wang.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1482709 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt              | 3 +++
 .../src/main/java/org/apache/hadoop/util/PlatformName.java   | 5 ++---
 2 files changed, 5 insertions(+), 3 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 805258f566d..79df6e07245 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -723,6 +723,9 @@ Release 2.0.5-beta - UNRELEASED
     HADOOP-9220. Unnecessary transition to standby in ActiveStandbyElector.
     (tom and todd via todd)
 
    HADOOP-9563. Fix incompatibility introduced by HADOOP-9523.
    (Tian Hong Wang via suresh)

 Release 2.0.4-alpha - 2013-04-25 
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java
index 819a9216bd9..24846f849a5 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PlatformName.java
@@ -25,7 +25,7 @@
  * A helper class for getting build-info of the java-vm. 
  * 
  */
@InterfaceAudience.Private
@InterfaceAudience.LimitedPrivate({"HBase"})
 @InterfaceStability.Unstable
 public class PlatformName {
   /**
@@ -49,7 +49,6 @@
   public static final boolean IBM_JAVA = JAVA_VENDOR_NAME.contains("IBM");
   
   public static void main(String[] args) {
    System.out.println("platform name: " + PLATFORM_NAME);
    System.out.println("java vendor name: " + JAVA_VENDOR_NAME);
    System.out.println(PLATFORM_NAME);
   }
 }
- 
2.19.1.windows.1

