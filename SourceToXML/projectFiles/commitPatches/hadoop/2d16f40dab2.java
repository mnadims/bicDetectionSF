From 2d16f40dab291a29b3fc005221b12fd587615d4e Mon Sep 17 00:00:00 2001
From: Ravi Prakash <raviprak@apache.org>
Date: Tue, 5 Jan 2016 23:26:03 -0800
Subject: [PATCH] HADOOP-12689. S3 filesystem operations stopped working
 correctly

--
 hadoop-common-project/hadoop-common/CHANGES.txt           | 3 +++
 .../org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java    | 8 ++++++--
 2 files changed, 9 insertions(+), 2 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 863d04729eb..1b867f0d16b 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1550,6 +1550,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12682. Fix TestKMS#testKMSRestart* failure.
     (Wei-Chiu Chuang via xyao)
 
    HADOOP-12689. S3 filesystem operations stopped working correctly
    (Matt Paduano via raviprak)

 Release 2.7.3 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java b/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java
index 5f46aea805d..a186c14ccc4 100644
-- a/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java
++ b/hadoop-tools/hadoop-aws/src/main/java/org/apache/hadoop/fs/s3/Jets3tFileSystemStore.java
@@ -173,7 +173,7 @@ private InputStream get(String key, boolean checkMetadata)
       return object.getDataInputStream();
     } catch (S3ServiceException e) {
       if ("NoSuchKey".equals(e.getS3ErrorCode())) {
        throw new IOException(key + " doesn't exist");
        return null;
       }
       if (e.getCause() instanceof IOException) {
         throw (IOException) e.getCause();
@@ -241,7 +241,11 @@ public File retrieveBlock(Block block, long byteRangeStart)
     OutputStream out = null;
     try {
       fileBlock = newBackupFile();
      in = get(blockToKey(block), byteRangeStart);
      String blockId = blockToKey(block);
      in = get(blockId, byteRangeStart);
      if (in == null) {
        throw new IOException("Block missing from S3 store: " + blockId);
      }
       out = new BufferedOutputStream(new FileOutputStream(fileBlock));
       byte[] buf = new byte[bufferSize];
       int numRead;
- 
2.19.1.windows.1

