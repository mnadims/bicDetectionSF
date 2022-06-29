From 0ef7ff47d5d031783ce61e93d36dc30703b5b28b Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Wed, 23 Sep 2015 19:33:55 -0700
Subject: [PATCH] HADOOP-12438. Reset
 RawLocalFileSystem.useDeprecatedFileStatus in TestLocalFileSystem.
 Contributed by Chris Nauroth.

--
 hadoop-common-project/hadoop-common/CHANGES.txt                | 3 +++
 .../test/java/org/apache/hadoop/fs/TestLocalFileSystem.java    | 1 +
 2 files changed, 4 insertions(+)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index acc2120a966..73e56b37bfe 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1144,6 +1144,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12417. TestWebDelegationToken failing with port in use.
     (Mingliang Liu via wheat9)
 
    HADOOP-12438. Reset RawLocalFileSystem.useDeprecatedFileStatus in
    TestLocalFileSystem. (Chris Nauroth via wheat9)

 Release 2.7.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
index 13499efec15..912c4f43e02 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
@@ -72,6 +72,7 @@ public void after() throws IOException {
     FileUtil.setWritable(base, true);
     FileUtil.fullyDelete(base);
     assertTrue(!base.exists());
    RawLocalFileSystem.useStatIfAvailable();
   }
 
   /**
- 
2.19.1.windows.1

