From cc8a2a29f18a6b54e182c320fa356751a494d499 Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Fri, 27 Jan 2012 22:42:47 +0000
Subject: [PATCH] HADOOP-7998 CheckFileSystem does not correctly honor
 setVerifyChecksum (Daryn Sharp via bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1236911 13f79535-47bb-0310-9956-ffa450edef68
--
 .../dev-support/test-patch.properties         |  2 +-
 .../hadoop-common/CHANGES.txt                 |  2 +
 .../apache/hadoop/fs/ChecksumFileSystem.java  |  5 +-
 .../hadoop/fs/TestChecksumFileSystem.java     | 93 ++++++++++++++-----
 4 files changed, 76 insertions(+), 26 deletions(-)

diff --git a/hadoop-common-project/dev-support/test-patch.properties b/hadoop-common-project/dev-support/test-patch.properties
index 15b54bfcf0d..c33b2a9440b 100644
-- a/hadoop-common-project/dev-support/test-patch.properties
++ b/hadoop-common-project/dev-support/test-patch.properties
@@ -18,4 +18,4 @@
 
 OK_RELEASEAUDIT_WARNINGS=0
 OK_FINDBUGS_WARNINGS=0
OK_JAVADOC_WARNINGS=6
OK_JAVADOC_WARNINGS=13
diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index fb7eb2a2e18..444224bc758 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -77,6 +77,8 @@ Trunk (unreleased changes)
     HADOOP-7965. Support for protocol version and signature in PB. (jitendra)
 
   BUGS
    HADOOP-7998. CheckFileSystem does not correctly honor setVerifyChecksum
                 (Daryn Sharp via bobby)
 
     HADOOP-7851. Configuration.getClasses() never returns the default value. 
                  (Uma Maheswara Rao G via amarrk)
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java
index f24c3924caf..de1178930f7 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ChecksumFileSystem.java
@@ -304,8 +304,9 @@ public synchronized void seek(long pos) throws IOException {
    */
   @Override
   public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    return new FSDataInputStream(
        new ChecksumFSInputChecker(this, f, bufferSize));
    return verifyChecksum
      ? new FSDataInputStream(new ChecksumFSInputChecker(this, f, bufferSize))
      : getRawFileSystem().open(f, bufferSize);
   }
 
   /** {@inheritDoc} */
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestChecksumFileSystem.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestChecksumFileSystem.java
index 373bdf12d5a..80347a72b45 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestChecksumFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestChecksumFileSystem.java
@@ -22,12 +22,22 @@
 import org.apache.hadoop.fs.FSDataOutputStream;
 import static org.apache.hadoop.fs.FileSystemTestHelper.*;
 import org.apache.hadoop.conf.Configuration;
import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
 
public class TestChecksumFileSystem extends TestCase {
public class TestChecksumFileSystem {
   static final String TEST_ROOT_DIR
     = System.getProperty("test.build.data","build/test/data/work-dir/localfs");
 
  static LocalFileSystem localFs;

  @Before
  public void resetLocalFs() throws Exception {
    localFs = FileSystem.getLocal(new Configuration());
    localFs.setVerifyChecksum(true);
  }

  @Test
   public void testgetChecksumLength() throws Exception {
     assertEquals(8, ChecksumFileSystem.getChecksumLength(0L, 512));
     assertEquals(12, ChecksumFileSystem.getChecksumLength(1L, 512));
@@ -40,9 +50,8 @@ public void testgetChecksumLength() throws Exception {
                  ChecksumFileSystem.getChecksumLength(10000000000000L, 10));    
   } 
   
  @Test
   public void testVerifyChecksum() throws Exception {    
    Configuration conf = new Configuration();
    LocalFileSystem localFs = FileSystem.getLocal(conf);
     Path testPath = new Path(TEST_ROOT_DIR, "testPath");
     Path testPath11 = new Path(TEST_ROOT_DIR, "testPath11");
     FSDataOutputStream fout = localFs.create(testPath);
@@ -68,7 +77,7 @@ public void testVerifyChecksum() throws Exception {
     
     //copying the wrong checksum file
     FileUtil.copy(localFs, localFs.getChecksumFile(testPath11), localFs, 
        localFs.getChecksumFile(testPath),false,true,conf);
        localFs.getChecksumFile(testPath),false,true,localFs.getConf());
     assertTrue("checksum exists", localFs.exists(localFs.getChecksumFile(testPath)));
     
     boolean errorRead = false;
@@ -80,20 +89,13 @@ public void testVerifyChecksum() throws Exception {
     assertTrue("error reading", errorRead);
     
     //now setting verify false, the read should succeed
    try {
      localFs.setVerifyChecksum(false);
      String str = readFile(localFs, testPath, 1024).toString();
      assertTrue("read", "testing".equals(str));
    } finally {
      // reset for other tests
      localFs.setVerifyChecksum(true);
    }
    
    localFs.setVerifyChecksum(false);
    String str = readFile(localFs, testPath, 1024).toString();
    assertTrue("read", "testing".equals(str));
   }
 
  @Test
   public void testMultiChunkFile() throws Exception {
    Configuration conf = new Configuration();
    LocalFileSystem localFs = FileSystem.getLocal(conf);
     Path testPath = new Path(TEST_ROOT_DIR, "testMultiChunk");
     FSDataOutputStream fout = localFs.create(testPath);
     for (int i = 0; i < 1000; i++) {
@@ -116,9 +118,8 @@ public void testMultiChunkFile() throws Exception {
    * Test to ensure that if the checksum file is truncated, a
    * ChecksumException is thrown
    */
  @Test
   public void testTruncatedChecksum() throws Exception { 
    Configuration conf = new Configuration();
    LocalFileSystem localFs = FileSystem.getLocal(conf);
     Path testPath = new Path(TEST_ROOT_DIR, "testtruncatedcrc");
     FSDataOutputStream fout = localFs.create(testPath);
     fout.write("testing truncation".getBytes());
@@ -146,14 +147,60 @@ public void testTruncatedChecksum() throws Exception {
     }
 
     // telling it not to verify checksums, should avoid issue.
    localFs.setVerifyChecksum(false);
    String str = readFile(localFs, testPath, 1024).toString();
    assertTrue("read", "testing truncation".equals(str));
  }
  
  @Test
  public void testStreamType() throws Exception {
    Path testPath = new Path(TEST_ROOT_DIR, "testStreamType");
    localFs.create(testPath).close();    
    FSDataInputStream in = null;
    
    localFs.setVerifyChecksum(true);
    in = localFs.open(testPath);
    assertTrue("stream is input checker",
        in.getWrappedStream() instanceof FSInputChecker);
    
    localFs.setVerifyChecksum(false);
    in = localFs.open(testPath);
    assertFalse("stream is not input checker",
        in.getWrappedStream() instanceof FSInputChecker);
  }
  
  @Test
  public void testCorruptedChecksum() throws Exception {
    Path testPath = new Path(TEST_ROOT_DIR, "testCorruptChecksum");
    Path checksumPath = localFs.getChecksumFile(testPath);

    // write a file to generate checksum
    FSDataOutputStream out = localFs.create(testPath, true);
    out.write("testing 1 2 3".getBytes());
    out.close();
    assertTrue(localFs.exists(checksumPath));
    FileStatus stat = localFs.getFileStatus(checksumPath);
    
    // alter file directly so checksum is invalid
    out = localFs.getRawFileSystem().create(testPath, true);
    out.write("testing stale checksum".getBytes());
    out.close();
    assertTrue(localFs.exists(checksumPath));
    // checksum didn't change on disk
    assertEquals(stat, localFs.getFileStatus(checksumPath));

    Exception e = null;
     try {
      localFs.setVerifyChecksum(false);
      String str = readFile(localFs, testPath, 1024).toString();
      assertTrue("read", "testing truncation".equals(str));
    } finally {
      // reset for other tests
       localFs.setVerifyChecksum(true);
      readFile(localFs, testPath, 1024);
    } catch (ChecksumException ce) {
      e = ce;
    } finally {
      assertNotNull("got checksum error", e);
     }
 
    localFs.setVerifyChecksum(false);
    String str = readFile(localFs, testPath, 1024);
    assertEquals("testing stale checksum", str);
   }
 }
- 
2.19.1.windows.1

