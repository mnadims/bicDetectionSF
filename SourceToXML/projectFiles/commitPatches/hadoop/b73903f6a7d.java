From b73903f6a7d0f7cc89c94d1c8e4e04fa60aeaf13 Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Tue, 23 Oct 2012 15:47:26 +0000
Subject: [PATCH] HADOOP-8962. RawLocalFileSystem.listStatus fails when a child
 filename contains a colon (jlowe via bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1401325 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt    |  3 +++
 .../org/apache/hadoop/fs/RawLocalFileSystem.java   |  4 ++--
 .../org/apache/hadoop/fs/TestLocalFileSystem.java  | 14 ++++++++++++++
 3 files changed, 19 insertions(+), 2 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 3e0c21116aa..9c50f2c4ea1 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1097,6 +1097,9 @@ Release 0.23.5 - UNRELEASED
     HADOOP-8811. Compile hadoop native library in FreeBSD (Radim Kolar via
     bobby)
 
    HADOOP-8962. RawLocalFileSystem.listStatus fails when a child filename
    contains a colon (jlowe via bobby)

 Release 0.23.4 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
index 267510d364d..4c089f1a299 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
@@ -350,7 +350,7 @@ public boolean delete(Path p, boolean recursive) throws IOException {
         new RawLocalFileStatus(localf, getDefaultBlockSize(f), this) };
     }
 
    String[] names = localf.list();
    File[] names = localf.listFiles();
     if (names == null) {
       return null;
     }
@@ -358,7 +358,7 @@ public boolean delete(Path p, boolean recursive) throws IOException {
     int j = 0;
     for (int i = 0; i < names.length; i++) {
       try {
        results[j] = getFileStatus(new Path(f, names[i]));
        results[j] = getFileStatus(new Path(names[i].getAbsolutePath()));
         j++;
       } catch (FileNotFoundException e) {
         // ignore the files not found since the dir list may have have changed
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
index e411314b85e..eb3d33df377 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
@@ -249,6 +249,7 @@ public void testStatistics() throws Exception {
     assertEquals(1, fileSchemeCount);
   }
 
  @Test
   public void testHasFileDescriptor() throws IOException {
     Configuration conf = new Configuration();
     LocalFileSystem fs = FileSystem.getLocal(conf);
@@ -258,4 +259,17 @@ public void testHasFileDescriptor() throws IOException {
         new RawLocalFileSystem().new LocalFSFileInputStream(path), 1024);
     assertNotNull(bis.getFileDescriptor());
   }

  @Test
  public void testListStatusWithColons() throws IOException {
    Configuration conf = new Configuration();
    LocalFileSystem fs = FileSystem.getLocal(conf);
    File colonFile = new File(TEST_ROOT_DIR, "foo:bar");
    colonFile.mkdirs();
    colonFile.createNewFile();
    FileStatus[] stats = fs.listStatus(new Path(TEST_ROOT_DIR));
    assertEquals("Unexpected number of stats", 1, stats.length);
    assertEquals("Bad path from stat", colonFile.getAbsolutePath(),
        stats[0].getPath().toUri().getPath());
  }
 }
- 
2.19.1.windows.1

