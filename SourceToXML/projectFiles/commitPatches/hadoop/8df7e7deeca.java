From 8df7e7deecad2b8131d67a1916b1ec4c9f7bc633 Mon Sep 17 00:00:00 2001
From: Colin McCabe <cmccabe@apache.org>
Date: Thu, 15 Aug 2013 23:05:41 +0000
Subject: [PATCH] HADOOP-9865.  FileContext#globStatus has a regression with
 respect to relative path.  (Contributed by Chaun Lin)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1514531 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 +
 .../java/org/apache/hadoop/fs/Globber.java    |  8 +--
 .../org/apache/hadoop/fs/TestGlobPaths.java   | 55 ++++++++++++++-----
 3 files changed, 47 insertions(+), 19 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 9518bf278af..e18d4584299 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -337,6 +337,9 @@ Release 2.3.0 - UNRELEASED
     HADOOP-9875.  TestDoAsEffectiveUser can fail on JDK 7.  (Aaron T. Myers via
     Colin Patrick McCabe)
 
    HADOOP-9865.  FileContext#globStatus has a regression with respect to
    relative path.  (Chuan Lin via Colin Patrick McCabe)

 
 Release 2.1.1-beta - UNRELEASED
 
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
index ad28478aeb8..378311a71a2 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
@@ -99,24 +99,24 @@ private Path fixRelativePart(Path path) {
   }
 
   private String schemeFromPath(Path path) throws IOException {
    String scheme = pathPattern.toUri().getScheme();
    String scheme = path.toUri().getScheme();
     if (scheme == null) {
       if (fs != null) {
         scheme = fs.getUri().getScheme();
       } else {
        scheme = fc.getFSofPath(path).getUri().getScheme();
        scheme = fc.getDefaultFileSystem().getUri().getScheme();
       }
     }
     return scheme;
   }
 
   private String authorityFromPath(Path path) throws IOException {
    String authority = pathPattern.toUri().getAuthority();
    String authority = path.toUri().getAuthority();
     if (authority == null) {
       if (fs != null) {
         authority = fs.getUri().getAuthority();
       } else {
        authority = fc.getFSofPath(path).getUri().getAuthority();
        authority = fc.getDefaultFileSystem().getUri().getAuthority();
       }
     }
     return authority ;
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
index b712be10f0f..820b00bb0b0 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
@@ -622,21 +622,7 @@ public void pTestCombination() throws IOException {
       cleanupDFS();
     }
   }
  
  @Test
  public void pTestRelativePath() throws IOException {
    try {
      String [] files = new String[] {"a", "abc", "abc.p", "bacd"};
      Path[] matchedPath = prepareTesting("a*", files);
      assertEquals(matchedPath.length, 3);
      assertEquals(matchedPath[0], new Path(USER_DIR, path[0]));
      assertEquals(matchedPath[1], new Path(USER_DIR, path[1]));
      assertEquals(matchedPath[2], new Path(USER_DIR, path[2]));
    } finally {
      cleanupDFS();
    }
  }
  

   /* Test {xx,yy} */
   @Test
   public void pTestCurlyBracket() throws IOException {
@@ -1061,4 +1047,43 @@ public void testGlobFillsInSchemeOnFS() throws Exception {
   public void testGlobFillsInSchemeOnFC() throws Exception {
     testOnFileContext(new TestGlobFillsInScheme());
   }

  /**
   * Test that globStatus works with relative paths.
   **/
  private static class TestRelativePath implements FSTestWrapperGlobTest {
    public void run(FSTestWrapper wrap, FileSystem fs, FileContext fc)
      throws Exception {
      String[] files = new String[] { "a", "abc", "abc.p", "bacd" };

      Path[] path = new Path[files.length];
      for(int i=0; i <  files.length; i++) {
        path[i] = wrap.makeQualified(new Path(files[i]));
        wrap.mkdir(path[i], FsPermission.getDirDefault(), true);
      }

      Path patternPath = new Path("a*");
      Path[] globResults = FileUtil.stat2Paths(wrap.globStatus(patternPath,
            new AcceptAllPathFilter()),
          patternPath);

      for(int i=0; i < globResults.length; i++) {
        globResults[i] = wrap.makeQualified(globResults[i]);
      }

      assertEquals(globResults.length, 3);
      assertEquals(USER_DIR + "/a;" + USER_DIR + "/abc;" + USER_DIR + "/abc.p",
                    TestPath.mergeStatuses(globResults));
    }
  }

  @Test
  public void testRelativePathOnFS() throws Exception {
    testOnFileSystem(new TestRelativePath());
  }

  @Test
  public void testRelativePathOnFC() throws Exception {
    testOnFileContext(new TestRelativePath());
  }
 }
- 
2.19.1.windows.1

