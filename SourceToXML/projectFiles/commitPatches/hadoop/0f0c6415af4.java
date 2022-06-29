From 0f0c6415af409d213e7a132390a850c1251b92ef Mon Sep 17 00:00:00 2001
From: Akira Ajisaka <aajisaka@apache.org>
Date: Tue, 10 May 2016 10:02:46 -0700
Subject: [PATCH] HADOOP-13118. Fix IOUtils#cleanup and IOUtils#closeStream
 javadoc. Contributed by Wei-Chiu Chuang.

--
 .../src/main/java/org/apache/hadoop/io/IOUtils.java           | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java
index 2588bf1f465..e6749b76bfa 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java
@@ -234,7 +234,7 @@ public static void skipFully(InputStream in, long len) throws IOException {
   }
   
   /**
   * Close the Closeable objects and <b>ignore</b> any {@link IOException} or 
   * Close the Closeable objects and <b>ignore</b> any {@link Throwable} or
    * null pointers. Must only be used for cleanup in exception handlers.
    *
    * @param log the log to record problems to at debug level. Can be null.
@@ -255,7 +255,7 @@ public static void cleanup(Log log, java.io.Closeable... closeables) {
   }
 
   /**
   * Closes the stream ignoring {@link IOException}.
   * Closes the stream ignoring {@link Throwable}.
    * Must only be called in cleaning up from exception handlers.
    *
    * @param stream the Stream to close
- 
2.19.1.windows.1

