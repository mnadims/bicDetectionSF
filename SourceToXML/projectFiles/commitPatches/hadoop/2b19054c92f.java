From 2b19054c92f9e93e3e2515ddb568fe501d7fba68 Mon Sep 17 00:00:00 2001
From: Suresh Srinivas <suresh@apache.org>
Date: Wed, 10 Apr 2013 00:45:47 +0000
Subject: [PATCH] HADOOP-9437. TestNativeIO#testRenameTo fails on Windows due
 to assumption that POSIX errno is embedded in NativeIOException. Contributed
 by Chris Nauroth.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1466306 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                  |  4 ++++
 .../org/apache/hadoop/io/nativeio/NativeIO.c   | 18 ++++++++++++++++++
 .../hadoop/io/nativeio/TestNativeIO.java       | 16 ++++++++++++++--
 3 files changed, 36 insertions(+), 2 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 2f755073d5e..29026bdf4a5 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -505,6 +505,10 @@ Trunk (Unreleased)
 
     HADOOP-9353. Activate native-win maven profile by default on Windows.
     (Arpit Agarwal via szetszwo)

    HADOOP-9437. TestNativeIO#testRenameTo fails on Windows due to assumption
    that POSIX errno is embedded in NativeIOException. (Chris Nauroth via
    suresh)
     
 Release 2.0.5-beta - UNRELEASED
 
diff --git a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
index 47f8dc1c9df..cd9b2a4d8b3 100644
-- a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
++ b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
@@ -816,6 +816,7 @@ JNIEXPORT void JNICALL
 Java_org_apache_hadoop_io_nativeio_NativeIO_renameTo0(JNIEnv *env, 
 jclass clazz, jstring jsrc, jstring jdst)
 {
#ifdef UNIX
   const char *src = NULL, *dst = NULL;
   
   src = (*env)->GetStringUTFChars(env, jsrc, NULL);
@@ -829,6 +830,23 @@ jclass clazz, jstring jsrc, jstring jdst)
 done:
   if (src) (*env)->ReleaseStringUTFChars(env, jsrc, src);
   if (dst) (*env)->ReleaseStringUTFChars(env, jdst, dst);
#endif

#ifdef WINDOWS
  LPCWSTR src = NULL, dst = NULL;

  src = (LPCWSTR) (*env)->GetStringChars(env, jsrc, NULL);
  if (!src) goto done; // exception was thrown
  dst = (LPCWSTR) (*env)->GetStringChars(env, jdst, NULL);
  if (!dst) goto done; // exception was thrown
  if (!MoveFile(src, dst)) {
    throw_ioe(env, GetLastError());
  }

done:
  if (src) (*env)->ReleaseStringChars(env, jsrc, src);
  if (dst) (*env)->ReleaseStringChars(env, jdst, dst);
#endif
 }
 
 /**
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
index 0602d302720..f5fc49dbde6 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
@@ -446,7 +446,13 @@ public void testRenameTo() throws Exception {
       NativeIO.renameTo(nonExistentFile, targetFile);
       Assert.fail();
     } catch (NativeIOException e) {
      Assert.assertEquals(e.getErrno(), Errno.ENOENT);
      if (Path.WINDOWS) {
        Assert.assertEquals(
          String.format("The system cannot find the file specified.%n"),
          e.getMessage());
      } else {
        Assert.assertEquals(Errno.ENOENT, e.getErrno());
      }
     }
     
     // Test renaming a file to itself.  It should succeed and do nothing.
@@ -465,7 +471,13 @@ public void testRenameTo() throws Exception {
       NativeIO.renameTo(sourceFile, badTarget);
       Assert.fail();
     } catch (NativeIOException e) {
      Assert.assertEquals(e.getErrno(), Errno.ENOTDIR);
      if (Path.WINDOWS) {
        Assert.assertEquals(
          String.format("The parameter is incorrect.%n"),
          e.getMessage());
      } else {
        Assert.assertEquals(Errno.ENOTDIR, e.getErrno());
      }
     }
 
     FileUtils.deleteQuietly(TEST_DIR);
- 
2.19.1.windows.1

