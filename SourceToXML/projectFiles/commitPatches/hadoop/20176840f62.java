From 20176840f6287fb426090820d5a3319c7e120bea Mon Sep 17 00:00:00 2001
From: Jason Darrell Lowe <jlowe@apache.org>
Date: Fri, 24 Jan 2014 15:45:28 +0000
Subject: [PATCH] Addendum patch for HADOOP-9652 to fix performance problems.
 Contributed by Andrew Wang

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1561038 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt       |  6 +++---
 .../java/org/apache/hadoop/fs/RawLocalFileSystem.java | 11 ++++++++++-
 .../java/org/apache/hadoop/fs/TestSymlinkLocalFS.java |  5 +++++
 3 files changed, 18 insertions(+), 4 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 4d18a94c4a1..7595563d4e0 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -424,6 +424,9 @@ Release 2.4.0 - UNRELEASED
     HADOOP-10143 replace WritableFactories's hashmap with ConcurrentHashMap
     (Liang Xie via stack)
 
    HADOOP-9652. Allow RawLocalFs#getFileLinkStatus to fill in the link owner
    and mode if requested. (Andrew Wang via Colin Patrick McCabe)

   OPTIMIZATIONS
 
     HADOOP-9748. Reduce blocking on UGI.ensureInitialized (daryn)
@@ -450,9 +453,6 @@ Release 2.4.0 - UNRELEASED
     HADOOP-9817. FileSystem#globStatus and FileContext#globStatus need to work
     with symlinks. (Colin Patrick McCabe via Andrew Wang)
 
    HADOOP-9652.  RawLocalFs#getFileLinkStatus does not fill in the link owner
    and mode.  (Andrew Wang via Colin Patrick McCabe)

     HADOOP-9875.  TestDoAsEffectiveUser can fail on JDK 7.  (Aaron T. Myers via
     Colin Patrick McCabe)
 
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
index 7d70ada73b4..bb5d8aada3e 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
@@ -16,8 +16,11 @@
  * limitations under the License.
  */
 

 package org.apache.hadoop.fs;
 
import com.google.common.annotations.VisibleForTesting;

 import java.io.BufferedOutputStream;
 import java.io.DataOutput;
 import java.io.File;
@@ -51,7 +54,13 @@
 public class RawLocalFileSystem extends FileSystem {
   static final URI NAME = URI.create("file:///");
   private Path workingDir;
  private static final boolean useDeprecatedFileStatus = !Stat.isAvailable();
  // Temporary workaround for HADOOP-9652.
  private static boolean useDeprecatedFileStatus = true;

  @VisibleForTesting
  public static void useStatIfAvailable() {
    useDeprecatedFileStatus = !Stat.isAvailable();
  }
   
   public RawLocalFileSystem() {
     workingDir = getInitialWorkingDirectory();
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
index c82dcc8a124..64e34af64bb 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
@@ -38,6 +38,11 @@
  * Test symbolic links using LocalFs.
  */
 abstract public class TestSymlinkLocalFS extends SymlinkBaseTest {

  // Workaround for HADOOP-9652
  static {
    RawLocalFileSystem.useStatIfAvailable();
  }
   
   @Override
   protected String getScheme() {
- 
2.19.1.windows.1

