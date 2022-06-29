From 0bdd263d82a4510f16df49238d57c9f78ac28ae7 Mon Sep 17 00:00:00 2001
From: Masatake Iwasaki <iwasakims@apache.org>
Date: Thu, 27 Oct 2016 15:44:49 +0900
Subject: [PATCH] HADOOP-13017. Implementations of InputStream.read(buffer,
 offset, bytes) to exit 0 if bytes==0. Contributed by Steve Loughran.

--
 .../src/main/java/org/apache/hadoop/fs/HarFileSystem.java    | 3 +++
 .../java/org/apache/hadoop/security/SaslInputStream.java     | 5 ++++-
 .../main/java/org/apache/hadoop/security/SaslRpcClient.java  | 3 +++
 .../main/java/org/apache/hadoop/util/LimitInputStream.java   | 3 +++
 .../java/org/apache/hadoop/hdfs/web/WebHdfsFileSystem.java   | 3 +++
 .../org/apache/hadoop/tools/util/ThrottledInputStream.java   | 3 +++
 .../hadoop/fs/swift/http/HttpInputStreamWithRelease.java     | 3 +++
 .../hadoop/fs/swift/snative/SwiftNativeInputStream.java      | 3 +++
 8 files changed, 25 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HarFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HarFileSystem.java
index 5f6ae486b81..ce1cf4556e4 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HarFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HarFileSystem.java
@@ -968,6 +968,9 @@ public synchronized int read(byte[] b) throws IOException {
       @Override
       public synchronized int read(byte[] b, int offset, int len) 
         throws IOException {
        if (len == 0) {
          return 0;
        }
         int newlen = len;
         int ret = -1;
         if (position + len > end) {
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslInputStream.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslInputStream.java
index 7ee452316a1..a3d66b977c2 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslInputStream.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslInputStream.java
@@ -246,6 +246,9 @@ public int read(byte[] b) throws IOException {
    */
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
    if (len == 0) {
      return 0;
    }
     if (!useWrap) {
       return inStream.read(b, off, len);
     }
@@ -378,4 +381,4 @@ public int read(ByteBuffer dst) throws IOException {
     }
     return bytesRead;
   }
}
\ No newline at end of file
}
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
index cd942b7a41e..388f1b298ad 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslRpcClient.java
@@ -569,6 +569,9 @@ public int read(byte b[]) throws IOException {
 
     @Override
     public synchronized int read(byte[] buf, int off, int len) throws IOException {
      if (len == 0) {
        return 0;
      }
       // fill the buffer with the next RPC message
       if (unwrappedRpcBuffer.remaining() == 0) {
         readNextRpcPacket();
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/LimitInputStream.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/LimitInputStream.java
index c94a51745d7..bd646e0bcb6 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/LimitInputStream.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/LimitInputStream.java
@@ -74,6 +74,9 @@ public int read() throws IOException {
 
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
    if (len == 0) {
      return 0;
    }
     if (left == 0) {
       return -1;
     }
diff --git a/hadoop-hdfs-project/hadoop-hdfs-client/src/main/java/org/apache/hadoop/hdfs/web/WebHdfsFileSystem.java b/hadoop-hdfs-project/hadoop-hdfs-client/src/main/java/org/apache/hadoop/hdfs/web/WebHdfsFileSystem.java
index af43d56d70b..782f113d6d4 100644
-- a/hadoop-hdfs-project/hadoop-hdfs-client/src/main/java/org/apache/hadoop/hdfs/web/WebHdfsFileSystem.java
++ b/hadoop-hdfs-project/hadoop-hdfs-client/src/main/java/org/apache/hadoop/hdfs/web/WebHdfsFileSystem.java
@@ -1832,6 +1832,9 @@ int read(byte[] b, int off, int len) throws IOException {
       if (runnerState == RunnerState.CLOSED) {
         throw new IOException("Stream closed");
       }
      if (len == 0) {
        return 0;
      }
 
       // Before the first read, pos and fileLength will be 0 and readBuffer
       // will all be null. They will be initialized once the first connection
diff --git a/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/util/ThrottledInputStream.java b/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/util/ThrottledInputStream.java
index 70355274add..2d2f10c90bd 100644
-- a/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/util/ThrottledInputStream.java
++ b/hadoop-tools/hadoop-distcp/src/main/java/org/apache/hadoop/tools/util/ThrottledInputStream.java
@@ -84,6 +84,9 @@ public int read(byte[] b) throws IOException {
   /** {@inheritDoc} */
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
    if (len == 0) {
      return 0;
    }
     throttle();
     int readLen = rawStream.read(b, off, len);
     if (readLen != -1) {
diff --git a/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/http/HttpInputStreamWithRelease.java b/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/http/HttpInputStreamWithRelease.java
index c75759e96b8..627792cb583 100644
-- a/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/http/HttpInputStreamWithRelease.java
++ b/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/http/HttpInputStreamWithRelease.java
@@ -187,6 +187,9 @@ public int read() throws IOException {
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
     SwiftUtils.validateReadArgs(b, off, len);
    if (len == 0) {
      return 0;
    }
     //if the stream is already closed, then report an exception.
     assumeNotReleased();
     //now read in a buffer, reacting differently to different operations
diff --git a/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/snative/SwiftNativeInputStream.java b/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/snative/SwiftNativeInputStream.java
index 3fd370227fd..23d8c09f031 100644
-- a/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/snative/SwiftNativeInputStream.java
++ b/hadoop-tools/hadoop-openstack/src/main/java/org/apache/hadoop/fs/swift/snative/SwiftNativeInputStream.java
@@ -161,6 +161,9 @@ public synchronized int read() throws IOException {
   public synchronized int read(byte[] b, int off, int len) throws IOException {
     SwiftUtils.debug(LOG, "read(buffer, %d, %d)", off, len);
     SwiftUtils.validateReadArgs(b, off, len);
    if (len == 0) {
      return 0;
    }
     int result = -1;
     try {
       verifyOpen();
- 
2.19.1.windows.1

