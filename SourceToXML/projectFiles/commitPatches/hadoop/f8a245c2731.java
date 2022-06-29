From f8a245c2731ab09b31d9024496d0f475597504ab Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Tue, 31 Jan 2012 22:40:25 +0000
Subject: [PATCH] HADOOP-8006  TestFSInputChecker is failing in trunk. (Daryn
 Sharp via bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1238841 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../apache/hadoop/fs/ChecksumFileSystem.java  | 32 ++++++++++++++++---
 2 files changed, 31 insertions(+), 4 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 8a53524d906..5dd6716f5a8 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -80,6 +80,9 @@ Trunk (unreleased changes)
     kerberos. (jitendra)
 
   BUGS
    HADOOP-8006  TestFSInputChecker is failing in trunk.
                 (Daryn Sharp via bobby)

     HADOOP-7998. CheckFileSystem does not correctly honor setVerifyChecksum
                  (Daryn Sharp via bobby)
 
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java
index de1178930f7..040f59dbb8c 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java
@@ -119,7 +119,6 @@ private int getSumBufferSize(int bytesPerSum, int bufferSize) {
     private static final int HEADER_LENGTH = 8;
     
     private int bytesPerSum = 1;
    private long fileLen = -1L;
     
     public ChecksumFSInputChecker(ChecksumFileSystem fs, Path file)
       throws IOException {
@@ -244,6 +243,24 @@ protected int readChunk(long pos, byte[] buf, int offset, int len,
       }
       return nread;
     }
  }
  
  private static class FSDataBoundedInputStream extends FSDataInputStream {
    private FileSystem fs;
    private Path file;
    private long fileLen = -1L;

    FSDataBoundedInputStream(FileSystem fs, Path file, InputStream in)
        throws IOException {
      super(in);
      this.fs = fs;
      this.file = file;
    }
    
    @Override
    public boolean markSupported() {
      return false;
    }
     
     /* Return the file length */
     private long getFileLength() throws IOException {
@@ -304,9 +321,16 @@ public synchronized void seek(long pos) throws IOException {
    */
   @Override
   public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    return verifyChecksum
      ? new FSDataInputStream(new ChecksumFSInputChecker(this, f, bufferSize))
      : getRawFileSystem().open(f, bufferSize);
    FileSystem fs;
    InputStream in;
    if (verifyChecksum) {
      fs = this;
      in = new ChecksumFSInputChecker(this, f, bufferSize);
    } else {
      fs = getRawFileSystem();
      in = fs.open(f, bufferSize);
    }
    return new FSDataBoundedInputStream(fs, f, in);
   }
 
   /** {@inheritDoc} */
- 
2.19.1.windows.1

