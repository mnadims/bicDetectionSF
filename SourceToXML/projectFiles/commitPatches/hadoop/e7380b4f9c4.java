From e7380b4f9c4f29bf5f0c07b95ddba88b0ef3765d Mon Sep 17 00:00:00 2001
From: Aaron Myers <atm@apache.org>
Date: Thu, 31 Jan 2013 07:29:46 +0000
Subject: [PATCH] HDFS-4428. FsDatasetImpl should disclose what the error is
 when a rename fails. Contributed by Colin Patrick McCabe.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1440865 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/hadoop/io/nativeio/Errno.java  |  3 ++
 .../apache/hadoop/io/nativeio/NativeIO.java   | 32 +++++++++++++++
 .../org/apache/hadoop/io/nativeio/NativeIO.c  | 23 ++++++++++-
 .../apache/hadoop/io/nativeio/errno_enum.c    |  3 ++
 .../hadoop/io/nativeio/TestNativeIO.java      | 39 +++++++++++++++++++
 hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt   |  3 ++
 .../fsdataset/impl/FsDatasetImpl.java         | 37 ++++++++++++------
 7 files changed, 127 insertions(+), 13 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/Errno.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/Errno.java
index b48f76da18e..f823978bd71 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/Errno.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/Errno.java
@@ -55,6 +55,9 @@
   EPIPE,
   EDOM,
   ERANGE,
  ELOOP,
  ENAMETOOLONG,
  ENOTEMPTY,
 
   UNKNOWN;
 }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java
index 94ff5f6057f..c8649c6a2e9 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java
@@ -17,6 +17,7 @@
  */
 package org.apache.hadoop.io.nativeio;
 
import java.io.File;
 import java.io.FileDescriptor;
 import java.io.IOException;
 import java.util.Map;
@@ -293,4 +294,35 @@ public static Stat getFstat(FileDescriptor fd) throws IOException {
     stat.group = getName(IdCache.GROUP, stat.groupId);
     return stat;
   }
  
  /**
   * A version of renameTo that throws a descriptive exception when it fails.
   *
   * @param src                  The source path
   * @param dst                  The destination path
   * 
   * @throws NativeIOException   On failure.
   */
  public static void renameTo(File src, File dst)
      throws IOException {
    if (!nativeLoaded) {
      if (!src.renameTo(dst)) {
        throw new IOException("renameTo(src=" + src + ", dst=" +
          dst + ") failed.");
      }
    } else {
      renameTo0(src.getAbsolutePath(), dst.getAbsolutePath());
    }
  }

  /**
   * A version of renameTo that throws a descriptive exception when it fails.
   *
   * @param src                  The source path
   * @param dst                  The destination path
   * 
   * @throws NativeIOException   On failure.
   */
  private static native void renameTo0(String src, String dst)
      throws NativeIOException;
 }
diff --git a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
index 7e82152c1fe..be957b447ad 100644
-- a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
++ b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
@@ -24,11 +24,12 @@
 #include <grp.h>
 #include <jni.h>
 #include <pwd.h>
#include <stdio.h>
 #include <stdlib.h>
 #include <string.h>
 #include <sys/stat.h>
#include <sys/types.h>
 #include <sys/syscall.h>
#include <sys/types.h>
 #include <unistd.h>
 
 #include "config.h"
@@ -502,6 +503,26 @@ ssize_t get_pw_buflen() {
   #endif
   return (ret > 512) ? ret : 512;
 }

JNIEXPORT void JNICALL 
Java_org_apache_hadoop_io_nativeio_NativeIO_renameTo0(JNIEnv *env, 
jclass clazz, jstring jsrc, jstring jdst)
{
  const char *src = NULL, *dst = NULL;
  
  src = (*env)->GetStringUTFChars(env, jsrc, NULL);
  if (!src) goto done; // exception was thrown
  dst = (*env)->GetStringUTFChars(env, jdst, NULL);
  if (!dst) goto done; // exception was thrown
  if (rename(src, dst)) {
    throw_ioe(env, errno);
  }

done:
  if (src) (*env)->ReleaseStringUTFChars(env, jsrc, src);
  if (dst) (*env)->ReleaseStringUTFChars(env, jdst, dst);
}

 /**
  * vim: sw=2: ts=2: et:
  */
diff --git a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/errno_enum.c b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/errno_enum.c
index 76d1ff17252..4d07c31394a 100644
-- a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/errno_enum.c
++ b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/errno_enum.c
@@ -63,6 +63,9 @@ static errno_mapping_t ERRNO_MAPPINGS[] = {
   MAPPING(EPIPE),
   MAPPING(EDOM),
   MAPPING(ERANGE),
  MAPPING(ELOOP),
  MAPPING(ENAMETOOLONG),
  MAPPING(ENOTEMPTY),
   {-1, NULL}
 };
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
index 9ee1516863b..f77e7288d62 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
@@ -25,11 +25,14 @@
 import java.util.concurrent.atomic.AtomicReference;
 import java.util.ArrayList;
 import java.util.List;

import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Test;
 import static org.junit.Assume.*;
 import static org.junit.Assert.*;
 
import org.apache.commons.io.FileUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.conf.Configuration;
@@ -293,4 +296,40 @@ public void testGetGroupName() throws IOException {
     assertFalse(NativeIO.getGroupName(0).isEmpty());
   }
 
  @Test
  public void testRenameTo() throws Exception {
    final File TEST_DIR = new File(new File(
        System.getProperty("test.build.data","build/test/data")), "renameTest");
    assumeTrue(TEST_DIR.mkdirs());
    File nonExistentFile = new File(TEST_DIR, "nonexistent");
    File targetFile = new File(TEST_DIR, "target");
    // Test attempting to rename a nonexistent file.
    try {
      NativeIO.renameTo(nonExistentFile, targetFile);
      Assert.fail();
    } catch (NativeIOException e) {
      Assert.assertEquals(e.getErrno(), Errno.ENOENT);
    }
    
    // Test renaming a file to itself.  It should succeed and do nothing.
    File sourceFile = new File(TEST_DIR, "source");
    Assert.assertTrue(sourceFile.createNewFile());
    NativeIO.renameTo(sourceFile, sourceFile);

    // Test renaming a source to a destination.
    NativeIO.renameTo(sourceFile, targetFile);

    // Test renaming a source to a path which uses a file as a directory.
    sourceFile = new File(TEST_DIR, "source");
    Assert.assertTrue(sourceFile.createNewFile());
    File badTarget = new File(targetFile, "subdir");
    try {
      NativeIO.renameTo(sourceFile, badTarget);
      Assert.fail();
    } catch (NativeIOException e) {
      Assert.assertEquals(e.getErrno(), Errno.ENOTDIR);
    }

    FileUtils.deleteQuietly(TEST_DIR);
  }
 }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt b/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
index 9512f26e7cb..a314d0f7e75 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
++ b/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
@@ -735,6 +735,9 @@ Release 2.0.3-alpha - Unreleased
     HDFS-4444. Add space between total transaction time and number of
     transactions in FSEditLog#printStatistics. (Stephen Chu via suresh)
 
    HDFS-4428. FsDatasetImpl should disclose what the error is when a rename
    fails. (Colin Patrick McCabe via atm)

   BREAKDOWN OF HDFS-3077 SUBTASKS
 
     HDFS-3077. Quorum-based protocol for reading and writing edit logs.
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/fsdataset/impl/FsDatasetImpl.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/fsdataset/impl/FsDatasetImpl.java
index c65e5faa405..caf970de6a4 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/fsdataset/impl/FsDatasetImpl.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/fsdataset/impl/FsDatasetImpl.java
@@ -75,6 +75,7 @@
 import org.apache.hadoop.hdfs.server.datanode.metrics.FSDatasetMBean;
 import org.apache.hadoop.hdfs.server.protocol.BlockRecoveryCommand.RecoveringBlock;
 import org.apache.hadoop.hdfs.server.protocol.ReplicaRecoveryInfo;
import org.apache.hadoop.io.nativeio.NativeIO;
 import org.apache.hadoop.metrics2.util.MBeans;
 import org.apache.hadoop.util.DataChecksum;
 import org.apache.hadoop.util.DiskChecker.DiskErrorException;
@@ -398,13 +399,17 @@ static File moveBlockFiles(Block b, File srcfile, File destdir
     final File dstfile = new File(destdir, b.getBlockName());
     final File srcmeta = FsDatasetUtil.getMetaFile(srcfile, b.getGenerationStamp());
     final File dstmeta = FsDatasetUtil.getMetaFile(dstfile, b.getGenerationStamp());
    if (!srcmeta.renameTo(dstmeta)) {
    try {
      NativeIO.renameTo(srcmeta, dstmeta);
    } catch (IOException e) {
       throw new IOException("Failed to move meta file for " + b
          + " from " + srcmeta + " to " + dstmeta);
          + " from " + srcmeta + " to " + dstmeta, e);
     }
    if (!srcfile.renameTo(dstfile)) {
    try {
      NativeIO.renameTo(srcfile, dstfile);
    } catch (IOException e) {
       throw new IOException("Failed to move block file for " + b
          + " from " + srcfile + " to " + dstfile.getAbsolutePath());
          + " from " + srcfile + " to " + dstfile.getAbsolutePath(), e);
     }
     if (LOG.isDebugEnabled()) {
       LOG.debug("addBlock: Moved " + srcmeta + " to " + dstmeta
@@ -531,10 +536,12 @@ private synchronized ReplicaBeingWritten append(String bpid,
     if (LOG.isDebugEnabled()) {
       LOG.debug("Renaming " + oldmeta + " to " + newmeta);
     }
    if (!oldmeta.renameTo(newmeta)) {
    try {
      NativeIO.renameTo(oldmeta, newmeta);
    } catch (IOException e) {
       throw new IOException("Block " + replicaInfo + " reopen failed. " +
                             " Unable to move meta file  " + oldmeta +
                            " to rbw dir " + newmeta);
                            " to rbw dir " + newmeta, e);
     }
 
     // rename block file to rbw directory
@@ -542,14 +549,18 @@ private synchronized ReplicaBeingWritten append(String bpid,
       LOG.debug("Renaming " + blkfile + " to " + newBlkFile
           + ", file length=" + blkfile.length());
     }
    if (!blkfile.renameTo(newBlkFile)) {
      if (!newmeta.renameTo(oldmeta)) {  // restore the meta file
    try {
      NativeIO.renameTo(blkfile, newBlkFile);
    } catch (IOException e) {
      try {
        NativeIO.renameTo(newmeta, oldmeta);
      } catch (IOException ex) {
         LOG.warn("Cannot move meta file " + newmeta + 
            "back to the finalized directory " + oldmeta);
            "back to the finalized directory " + oldmeta, ex);
       }
       throw new IOException("Block " + replicaInfo + " reopen failed. " +
                               " Unable to move block file " + blkfile +
                              " to rbw dir " + newBlkFile);
                              " to rbw dir " + newBlkFile, e);
     }
     
     // Replace finalized replica by a RBW replica in replicas map
@@ -656,11 +667,13 @@ private void bumpReplicaGS(ReplicaInfo replicaInfo,
     if (LOG.isDebugEnabled()) {
       LOG.debug("Renaming " + oldmeta + " to " + newmeta);
     }
    if (!oldmeta.renameTo(newmeta)) {
    try {
      NativeIO.renameTo(oldmeta, newmeta);
    } catch (IOException e) {
       replicaInfo.setGenerationStamp(oldGS); // restore old GS
       throw new IOException("Block " + replicaInfo + " reopen failed. " +
                             " Unable to move meta file  " + oldmeta +
                            " to " + newmeta);
                            " to " + newmeta, e);
     }
   }
 
- 
2.19.1.windows.1

