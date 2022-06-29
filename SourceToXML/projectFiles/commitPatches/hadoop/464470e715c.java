From 464470e715c95fc3b832e93800d551fdb44333f1 Mon Sep 17 00:00:00 2001
From: Colin McCabe <cmccabe@apache.org>
Date: Wed, 25 Sep 2013 20:51:09 +0000
Subject: [PATCH] HADOOP-9981. globStatus should minimize its listStatus and
 getFileStatus calls.  (Contributed by Colin Patrick McCabe)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1526297 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 +
 .../java/org/apache/hadoop/fs/Globber.java    | 86 ++++++++++++++-----
 .../org/apache/hadoop/fs/TestGlobPaths.java   | 49 ++++++++---
 3 files changed, 105 insertions(+), 33 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 90ae9e91013..e196b74927a 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -369,6 +369,9 @@ Release 2.3.0 - UNRELEASED
     HADOOP-9791. Add a test case covering long paths for new FileUtil access
     check methods (ivanmi)
 
    HADOOP-9981. globStatus should minimize its listStatus and getFileStatus
    calls.  (Contributed by Colin Patrick McCabe)

 Release 2.2.0 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
index a23649fe482..d00c387f9b3 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
@@ -83,6 +83,15 @@ private Path fixRelativePart(Path path) {
     }
   }
 
  /**
   * Convert a path component that contains backslash ecape sequences to a
   * literal string.  This is necessary when you want to explicitly refer to a
   * path that contains globber metacharacters.
   */
  private static String unescapePathComponent(String name) {
    return name.replaceAll("\\\\(.)", "$1");
  }

   /**
    * Translate an absolute path into a list of path components.
    * We merge double slashes into a single slash here.
@@ -166,37 +175,72 @@ private String authorityFromPath(Path path) throws IOException {
             new Path(scheme, authority, Path.SEPARATOR)));
       }
       
      for (String component : components) {
      for (int componentIdx = 0; componentIdx < components.size();
          componentIdx++) {
         ArrayList<FileStatus> newCandidates =
             new ArrayList<FileStatus>(candidates.size());
        GlobFilter globFilter = new GlobFilter(component);
        GlobFilter globFilter = new GlobFilter(components.get(componentIdx));
        String component = unescapePathComponent(components.get(componentIdx));
         if (globFilter.hasPattern()) {
           sawWildcard = true;
         }
         if (candidates.isEmpty() && sawWildcard) {
          // Optimization: if there are no more candidates left, stop examining 
          // the path components.  We can only do this if we've already seen
          // a wildcard component-- otherwise, we still need to visit all path 
          // components in case one of them is a wildcard.
           break;
         }
        for (FileStatus candidate : candidates) {
          FileStatus resolvedCandidate = candidate;
          if (candidate.isSymlink()) {
            // We have to resolve symlinks, because otherwise we don't know
            // whether they are directories.
            resolvedCandidate = getFileStatus(candidate.getPath());
        if ((componentIdx < components.size() - 1) &&
            (!globFilter.hasPattern())) {
          // Optimization: if this is not the terminal path component, and we 
          // are not matching against a glob, assume that it exists.  If it 
          // doesn't exist, we'll find out later when resolving a later glob
          // or the terminal path component.
          for (FileStatus candidate : candidates) {
            candidate.setPath(new Path(candidate.getPath(), component));
           }
          if (resolvedCandidate == null ||
              resolvedCandidate.isDirectory() == false) {
            continue;
          }
          FileStatus[] children = listStatus(candidate.getPath());
          for (FileStatus child : children) {
            // Set the child path based on the parent path.
            // This keeps the symlinks in our path.
            child.setPath(new Path(candidate.getPath(),
                    child.getPath().getName()));
            if (globFilter.accept(child.getPath())) {
              newCandidates.add(child);
          continue;
        }
        for (FileStatus candidate : candidates) {
          if (globFilter.hasPattern()) {
            FileStatus[] children = listStatus(candidate.getPath());
            if (children.length == 1) {
              // If we get back only one result, this could be either a listing
              // of a directory with one entry, or it could reflect the fact
              // that what we listed resolved to a file.
              //
              // Unfortunately, we can't just compare the returned paths to
              // figure this out.  Consider the case where you have /a/b, where
              // b is a symlink to "..".  In that case, listing /a/b will give
              // back "/a/b" again.  If we just went by returned pathname, we'd
              // incorrectly conclude that /a/b was a file and should not match
              // /a/*/*.  So we use getFileStatus of the path we just listed to
              // disambiguate.
              if (!getFileStatus(candidate.getPath()).isDirectory()) {
                continue;
              }
             }
          }
            for (FileStatus child : children) {
              // Set the child path based on the parent path.
              child.setPath(new Path(candidate.getPath(),
                      child.getPath().getName()));
              if (globFilter.accept(child.getPath())) {
                newCandidates.add(child);
              }
            }
          } else {
            // When dealing with non-glob components, use getFileStatus 
            // instead of listStatus.  This is an optimization, but it also
            // is necessary for correctness in HDFS, since there are some
            // special HDFS directories like .reserved and .snapshot that are
            // not visible to listStatus, but which do exist.  (See HADOOP-9877)
            FileStatus childStatus = getFileStatus(
                new Path(candidate.getPath(), component));
            if (childStatus != null) {
              newCandidates.add(childStatus);
             }
           }
         }
         candidates = newCandidates;
       }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
index 8e8124747c3..8eb9847ebb5 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestGlobPaths.java
@@ -28,6 +28,7 @@
 import org.apache.hadoop.fs.permission.FsPermission;
 import org.apache.hadoop.hdfs.HdfsConfiguration;
 import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.namenode.INodeId;
 import org.apache.hadoop.security.AccessControlException;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.junit.*;
@@ -361,17 +362,6 @@ public void testMultiGlob() throws IOException {
     status = fs.globStatus(new Path(USER_DIR+"{/dir*}"));
     checkStatus(status, d1, d2, d3, d4);
 
    /* 
     * true filter
     */

    PathFilter trueFilter = new PathFilter() {
      @Override
      public boolean accept(Path path) {
        return true;
      }
    };

     status = fs.globStatus(new Path(Path.SEPARATOR), trueFilter);
     checkStatus(status, new Path(Path.SEPARATOR));
     
@@ -843,6 +833,8 @@ public boolean accept(Path path) {
     }
   }
 
  private static final PathFilter trueFilter = new AcceptAllPathFilter();

   /**
    * Accept only paths ending in Z.
    */
@@ -893,11 +885,13 @@ public void run(FSTestWrapper wrap, FSTestWrapper unprivilegedWrap,
     }
   }
 
  @Ignore
   @Test
   public void testGlobWithSymlinksOnFS() throws Exception {
     testOnFileSystem(new TestGlobWithSymlinks());
   }
 
  @Ignore
   @Test
   public void testGlobWithSymlinksOnFC() throws Exception {
     testOnFileContext(new TestGlobWithSymlinks());
@@ -970,11 +964,13 @@ public void run(FSTestWrapper wrap, FSTestWrapper unprivilegedWrap,
     }
   }
 
  @Ignore
   @Test
   public void testGlobWithSymlinksToSymlinksOnFS() throws Exception {
     testOnFileSystem(new TestGlobWithSymlinksToSymlinks());
   }
 
  @Ignore
   @Test
   public void testGlobWithSymlinksToSymlinksOnFC() throws Exception {
     testOnFileContext(new TestGlobWithSymlinksToSymlinks());
@@ -1019,11 +1015,13 @@ public void run(FSTestWrapper wrap, FSTestWrapper unprivilegedWrap,
     }
   }
 
  @Ignore
   @Test
   public void testGlobSymlinksWithCustomPathFilterOnFS() throws Exception {
     testOnFileSystem(new TestGlobSymlinksWithCustomPathFilter());
   }
 
  @Ignore
   @Test
   public void testGlobSymlinksWithCustomPathFilterOnFC() throws Exception {
     testOnFileContext(new TestGlobSymlinksWithCustomPathFilter());
@@ -1044,7 +1042,7 @@ public void run(FSTestWrapper wrap, FSTestWrapper unprivilegedWrap,
           new Path(USER_DIR + "/alphaLink"), new AcceptAllPathFilter());
       Assert.assertEquals(1, statuses.length);
       Path path = statuses[0].getPath();
      Assert.assertEquals(USER_DIR + "/alphaLink", path.toUri().getPath());
      Assert.assertEquals(USER_DIR + "/alpha", path.toUri().getPath());
       Assert.assertEquals("hdfs", path.toUri().getScheme());
       if (fc != null) {
         // If we're using FileContext, then we can list a file:/// URI.
@@ -1150,4 +1148,31 @@ public void testGlobAccessDeniedOnFS() throws Exception {
   public void testGlobAccessDeniedOnFC() throws Exception {
     testOnFileContext(new TestGlobAccessDenied());
   }

  /**
   * Test that trying to list a reserved path on HDFS via the globber works.
   **/
  private static class TestReservedHdfsPaths implements FSTestWrapperGlobTest {
    public void run(FSTestWrapper wrap, FSTestWrapper unprivilegedWrap,
        FileSystem fs, FileContext fc) throws Exception {
      String reservedRoot = "/.reserved/.inodes/" + INodeId.ROOT_INODE_ID;
      Assert.assertEquals(reservedRoot,
        TestPath.mergeStatuses(unprivilegedWrap.
            globStatus(new Path(reservedRoot), new AcceptAllPathFilter())));
      // These inodes don't show up via listStatus.
      Assert.assertEquals("",
        TestPath.mergeStatuses(unprivilegedWrap.
            globStatus(new Path("/.reserved/*"), new AcceptAllPathFilter())));
    }
  }

  @Test
  public void testReservedHdfsPathsOnFS() throws Exception {
    testOnFileSystem(new TestReservedHdfsPaths());
  }

  @Test
  public void testReservedHdfsPathsOnFC() throws Exception {
    testOnFileContext(new TestReservedHdfsPaths());
  }
 }
- 
2.19.1.windows.1

