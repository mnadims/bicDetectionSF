From 2088309d66541d74f2abde4e28bbf301aad7c0be Mon Sep 17 00:00:00 2001
From: Ivan Mitic <ivanmi@apache.org>
Date: Fri, 30 Aug 2013 01:04:35 +0000
Subject: [PATCH] HADOOP-9774. RawLocalFileSystem.listStatus() return absolute
 paths when input path is relative on Windows. Contributed by Shanyu Zhao.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1518865 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../main/java/org/apache/hadoop/fs/Path.java  | 12 ++++++
 .../apache/hadoop/fs/RawLocalFileSystem.java  |  6 ++-
 .../apache/hadoop/fs/TestLocalFileSystem.java | 15 ++++++++
 .../java/org/apache/hadoop/fs/TestPath.java   | 38 ++++++++++++++++++-
 5 files changed, 71 insertions(+), 3 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 7f88052df20..7e12e7f288b 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -438,6 +438,9 @@ Release 2.1.1-beta - UNRELEASED
     HADOOP-9894.  Race condition in Shell leads to logged error stream handling
     exceptions (Arpit Agarwal)
 
    HADOOP-9774. RawLocalFileSystem.listStatus() return absolute paths when
    input path is relative on Windows. (Shanyu Zhao via ivanmi)

 Release 2.1.0-beta - 2013-08-22
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Path.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Path.java
index 4b50882eae8..2d3acd0f8bb 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Path.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Path.java
@@ -182,6 +182,18 @@ public Path(URI aUri) {
   /** Construct a Path from components. */
   public Path(String scheme, String authority, String path) {
     checkPathArg( path );

    // add a slash in front of paths with Windows drive letters
    if (hasWindowsDrive(path) && path.charAt(0) != '/') {
      path = "/" + path;
    }

    // add "./" in front of Linux relative paths so that a path containing
    // a colon e.q. "a:b" will not be interpreted as scheme "a".
    if (!WINDOWS && path.charAt(0) != '/') {
      path = "./" + path;
    }

     initialize(scheme, authority, path, null);
   }
 
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
index 42f77fc3508..c2e2458fe0c 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
@@ -393,7 +393,7 @@ public boolean delete(Path p, boolean recursive) throws IOException {
         new DeprecatedRawLocalFileStatus(localf, getDefaultBlockSize(f), this)};
     }
 
    File[] names = localf.listFiles();
    String[] names = localf.list();
     if (names == null) {
       return null;
     }
@@ -401,7 +401,9 @@ public boolean delete(Path p, boolean recursive) throws IOException {
     int j = 0;
     for (int i = 0; i < names.length; i++) {
       try {
        results[j] = getFileStatus(new Path(names[i].getAbsolutePath()));
        // Assemble the path using the Path 3 arg constructor to make sure
        // paths with colon are properly resolved on Linux
        results[j] = getFileStatus(new Path(f, new Path(null, null, names[i])));
         j++;
       } catch (FileNotFoundException e) {
         // ignore the files not found since the dir list may have have changed
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
index dacb2c9b82f..8f427500c86 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
@@ -280,6 +280,21 @@ public void testListStatusWithColons() throws IOException {
         stats[0].getPath().toUri().getPath());
   }
   
  @Test
  public void testListStatusReturnConsistentPathOnWindows() throws IOException {
    assumeTrue(Shell.WINDOWS);
    String dirNoDriveSpec = TEST_ROOT_DIR;
    if (dirNoDriveSpec.charAt(1) == ':')
    	dirNoDriveSpec = dirNoDriveSpec.substring(2);
    
    File file = new File(dirNoDriveSpec, "foo");
    file.mkdirs();
    FileStatus[] stats = fileSys.listStatus(new Path(dirNoDriveSpec));
    assertEquals("Unexpected number of stats", 1, stats.length);
    assertEquals("Bad path from stat", new Path(file.getPath()).toUri().getPath(),
        stats[0].getPath().toUri().getPath());
  }
  
   @Test(timeout = 10000)
   public void testReportChecksumFailure() throws IOException {
     base.mkdirs();
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java
index 0f6bf71bded..f0a457b4127 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestPath.java
@@ -158,7 +158,43 @@ public void testChild() {
       assertEquals(new Path("c:/foo"), new Path("d:/bar", "c:/foo"));
     }
   }
  

  @Test (timeout = 30000)
  public void testPathThreeArgContructor() {
    assertEquals(new Path("foo"), new Path(null, null, "foo"));
    assertEquals(new Path("scheme:///foo"), new Path("scheme", null, "/foo"));
    assertEquals(
        new Path("scheme://authority/foo"),
        new Path("scheme", "authority", "/foo"));

    if (Path.WINDOWS) {
      assertEquals(new Path("c:/foo/bar"), new Path(null, null, "c:/foo/bar"));
      assertEquals(new Path("c:/foo/bar"), new Path(null, null, "/c:/foo/bar"));
    } else {
      assertEquals(new Path("./a:b"), new Path(null, null, "a:b"));
    }

    // Resolution tests
    if (Path.WINDOWS) {
      assertEquals(
          new Path("c:/foo/bar"),
          new Path("/fou", new Path(null, null, "c:/foo/bar")));
      assertEquals(
          new Path("c:/foo/bar"),
          new Path("/fou", new Path(null, null, "/c:/foo/bar")));
      assertEquals(
          new Path("/foo/bar"),
          new Path("/foo", new Path(null, null, "bar")));
    } else {
      assertEquals(
          new Path("/foo/bar/a:b"),
          new Path("/foo/bar", new Path(null, null, "a:b")));
      assertEquals(
          new Path("/a:b"),
          new Path("/foo/bar", new Path(null, null, "/a:b")));
    }
  }

   @Test (timeout = 30000)
   public void testEquals() {
     assertFalse(new Path("/").equals(new Path("/foo")));
- 
2.19.1.windows.1

