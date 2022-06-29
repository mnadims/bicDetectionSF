From c6c0f4eb25e511944915bc869e741197f7a277e0 Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Sat, 17 Jan 2015 18:26:03 +0000
Subject: [PATCH] HADOOP-10542 Potential null pointer dereference in
 Jets3tFileSystemStore retrieveBlock(). (Ted Yu via stevel)

--
 hadoop-common-project/hadoop-common/CHANGES.txt                | 3 +++
 .../java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java    | 2 +-
 2 files changed, 4 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 6896fe24de4..b2c1af3e310 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -716,6 +716,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11350. The size of header buffer of HttpServer is too small when
     HTTPS is enabled. (Benoy Antony via wheat9)
 
    HADOOP-10542 Potential null pointer dereference in Jets3tFileSystemStore
    retrieveBlock(). (Ted Yu via stevel)	

 Release 2.6.0 - 2014-11-18
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java b/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java
index 241ec0f3277..5f46aea805d 100644
-- a/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java
++ b/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java
@@ -173,7 +173,7 @@ private InputStream get(String key, boolean checkMetadata)
       return object.getDataInputStream();
     } catch (S3ServiceException e) {
       if ("NoSuchKey".equals(e.getS3ErrorCode())) {
        return null;
        throw new IOException(key + " doesn't exist");
       }
       if (e.getCause() instanceof IOException) {
         throw (IOException) e.getCause();
- 
2.19.1.windows.1

