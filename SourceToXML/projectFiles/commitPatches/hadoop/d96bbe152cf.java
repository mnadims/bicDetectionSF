From d96bbe152cf536304208f2e8f35deb3b2aa91d2b Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Thu, 16 Jul 2015 15:21:53 -0700
Subject: [PATCH] HDFS-8767. RawLocalFileSystem.listStatus() returns null for
 UNIX pipefile. Contributed by kanaka kumar avvaru.

--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../apache/hadoop/fs/RawLocalFileSystem.java  | 53 ++++++++++---------
 .../apache/hadoop/fs/TestLocalFileSystem.java | 22 ++++++++
 3 files changed, 53 insertions(+), 25 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index cf79bab0c50..b54688fe90d 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -987,6 +987,9 @@ Release 2.7.2 - UNRELEASED
     HADOOP-12191. Bzip2Factory is not thread safe. (Brahma Reddy Battula
     via ozawa)
 
    HDFS-8767. RawLocalFileSystem.listStatus() returns null for UNIX pipefile.
    (kanaka kumar avvaru via wheat9)

 Release 2.7.1 - 2015-07-06 
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
index ac65b6221f3..4728dbe4f20 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
@@ -459,35 +459,38 @@ public boolean delete(Path p, boolean recursive) throws IOException {
     if (!localf.exists()) {
       throw new FileNotFoundException("File " + f + " does not exist");
     }
    if (localf.isFile()) {
      if (!useDeprecatedFileStatus) {
        return new FileStatus[] { getFileStatus(f) };
      }
      return new FileStatus[] {
        new DeprecatedRawLocalFileStatus(localf, getDefaultBlockSize(f), this)};
    }
 
    String[] names = localf.list();
    if (names == null) {
      return null;
    }
    results = new FileStatus[names.length];
    int j = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // Assemble the path using the Path 3 arg constructor to make sure
        // paths with colon are properly resolved on Linux
        results[j] = getFileStatus(new Path(f, new Path(null, null, names[i])));
        j++;
      } catch (FileNotFoundException e) {
        // ignore the files not found since the dir list may have have changed
        // since the names[] list was generated.
    if (localf.isDirectory()) {
      String[] names = localf.list();
      if (names == null) {
        return null;
       }
      results = new FileStatus[names.length];
      int j = 0;
      for (int i = 0; i < names.length; i++) {
        try {
          // Assemble the path using the Path 3 arg constructor to make sure
          // paths with colon are properly resolved on Linux
          results[j] = getFileStatus(new Path(f, new Path(null, null,
                                                          names[i])));
          j++;
        } catch (FileNotFoundException e) {
          // ignore the files not found since the dir list may have have
          // changed since the names[] list was generated.
        }
      }
      if (j == names.length) {
        return results;
      }
      return Arrays.copyOf(results, j);
     }
    if (j == names.length) {
      return results;

    if (!useDeprecatedFileStatus) {
      return new FileStatus[] { getFileStatus(f) };
     }
    return Arrays.copyOf(results, j);
    return new FileStatus[] {
        new DeprecatedRawLocalFileStatus(localf,
        getDefaultBlockSize(f), this) };
   }
   
   protected boolean mkOneDir(File p2f) throws IOException {
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
index f641f041d29..13499efec15 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
@@ -32,11 +32,14 @@
 
 import static org.junit.Assert.*;
 import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;
 
 import org.junit.After;
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

 
 /**
  * This class tests the local file system via the FileSystem abstraction.
@@ -612,4 +615,23 @@ public void testAppendSetsPosCorrectly() throws Exception {
     }
   }
 
  @Test
  public void testFileStatusPipeFile() throws Exception {
    RawLocalFileSystem origFs = new RawLocalFileSystem();
    RawLocalFileSystem fs = spy(origFs);
    Configuration conf = mock(Configuration.class);
    fs.setConf(conf);
    Whitebox.setInternalState(fs, "useDeprecatedFileStatus", false);
    Path path = new Path("/foo");
    File pipe = mock(File.class);
    when(pipe.isFile()).thenReturn(false);
    when(pipe.isDirectory()).thenReturn(false);
    when(pipe.exists()).thenReturn(true);

    FileStatus stat = mock(FileStatus.class);
    doReturn(pipe).when(fs).pathToFile(path);
    doReturn(stat).when(fs).getFileStatus(path);
    FileStatus[] stats = fs.listStatus(path);
    assertTrue(stats != null && stats.length == 1 && stats[0] == stat);
  }
 }
- 
2.19.1.windows.1

