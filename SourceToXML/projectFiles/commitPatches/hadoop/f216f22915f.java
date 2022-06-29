From f216f22915f9620c086e361e6eb759a75bed199a Mon Sep 17 00:00:00 2001
From: Colin McCabe <cmccabe@apache.org>
Date: Fri, 2 Aug 2013 21:42:38 +0000
Subject: [PATCH] HADOOP-9761.  ViewFileSystem#rename fails when using
 DistributedFileSystem (Andrew Wang via Colin Patrick McCabe)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1509874 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../hadoop/fs/FileSystemLinkResolver.java     |  4 ++-
 .../hadoop/hdfs/DistributedFileSystem.java    | 30 ++++++-------------
 .../fs/viewfs/TestViewFileSystemHdfs.java     |  8 ++++-
 4 files changed, 22 insertions(+), 23 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 3c0737ca94b..52f53f326f8 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -312,6 +312,9 @@ Release 2.3.0 - UNRELEASED
     HADOOP-9582. Non-existent file to "hadoop fs -conf" doesn't throw error
     (Ashwin Shankar via jlowe)
 
    HADOOP-9761.  ViewFileSystem#rename fails when using DistributedFileSystem.
    (Andrew Wang via Colin Patrick McCabe)

 Release 2.1.1-beta - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystemLinkResolver.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystemLinkResolver.java
index fce2891750c..c01d41fa9a4 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystemLinkResolver.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystemLinkResolver.java
@@ -73,7 +73,9 @@ public T resolve(final FileSystem filesys, final Path path)
     int count = 0;
     T in = null;
     Path p = path;
    FileSystem fs = FileSystem.getFSofPath(p, filesys.getConf());
    // Assumes path belongs to this FileSystem.
    // Callers validate this by passing paths through FileSystem#checkPath
    FileSystem fs = filesys;
     for (boolean isLink = true; isLink;) {
       try {
         in = doCall(p);
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java
index 8127689713d..6cb84741ab1 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java
@@ -170,12 +170,11 @@ public Path getHomeDirectory() {
   }
 
   /**
   * Checks that the passed URI belongs to this filesystem, resolves the path
   * component against the current working directory if relative, and finally
   * returns the absolute path component.
   * Checks that the passed URI belongs to this filesystem and returns
   * just the path component. Expects a URI with an absolute path.
    * 
   * @param file URI to check and resolve
   * @return resolved absolute path component of {file}
   * @param file URI with absolute path
   * @return path component of {file}
    * @throws IllegalArgumentException if URI does not belong to this DFS
    */
   private String getPathName(Path file) {
@@ -514,15 +513,10 @@ public void concat(Path trg, Path [] psrcs) throws IOException {
   @Override
   public boolean rename(Path src, Path dst) throws IOException {
     statistics.incrementWriteOps(1);
    // Both Paths have to belong to this DFS

     final Path absSrc = fixRelativePart(src);
     final Path absDst = fixRelativePart(dst);
    FileSystem srcFS = getFSofPath(absSrc, getConf());
    FileSystem dstFS = getFSofPath(absDst, getConf());
    if (!srcFS.getUri().equals(getUri()) ||
        !dstFS.getUri().equals(getUri())) {
      throw new IOException("Renames across FileSystems not supported");
    }

     // Try the rename without resolving first
     try {
       return dfs.rename(getPathName(absSrc), getPathName(absDst));
@@ -539,7 +533,8 @@ public Boolean doCall(final Path p)
         @Override
         public Boolean next(final FileSystem fs, final Path p)
             throws IOException {
          return fs.rename(source, p);
          // Should just throw an error in FileSystem#checkPath
          return doCall(p);
         }
       }.resolve(this, absDst);
     }
@@ -553,15 +548,8 @@ public Boolean next(final FileSystem fs, final Path p)
   public void rename(Path src, Path dst, final Options.Rename... options)
       throws IOException {
     statistics.incrementWriteOps(1);
    // Both Paths have to belong to this DFS
     final Path absSrc = fixRelativePart(src);
     final Path absDst = fixRelativePart(dst);
    FileSystem srcFS = getFSofPath(absSrc, getConf());
    FileSystem dstFS = getFSofPath(absDst, getConf());
    if (!srcFS.getUri().equals(getUri()) ||
        !dstFS.getUri().equals(getUri())) {
      throw new IOException("Renames across FileSystems not supported");
    }
     // Try the rename without resolving first
     try {
       dfs.rename(getPathName(absSrc), getPathName(absDst), options);
@@ -579,7 +567,7 @@ public Void doCall(final Path p)
         @Override
         public Void next(final FileSystem fs, final Path p)
             throws IOException {
          // Since we know it's this DFS for both, can just call doCall again
          // Should just throw an error in FileSystem#checkPath
           return doCall(p);
         }
       }.resolve(this, absDst);
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/viewfs/TestViewFileSystemHdfs.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/viewfs/TestViewFileSystemHdfs.java
index 1426d76e2d4..013d8a6e1a3 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/viewfs/TestViewFileSystemHdfs.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/viewfs/TestViewFileSystemHdfs.java
@@ -24,8 +24,10 @@
 import javax.security.auth.login.LoginException;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.FileSystemTestHelper;
import org.apache.hadoop.fs.FsConstants;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.hdfs.DFSConfigKeys;
 import org.apache.hadoop.hdfs.MiniDFSCluster;
@@ -69,7 +71,11 @@ public static void clusterSetupAtBegining() throws IOException,
     
     fHdfs = cluster.getFileSystem(0);
     fHdfs2 = cluster.getFileSystem(1);
    
    fHdfs.getConf().set(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY,
        FsConstants.VIEWFS_URI.toString());
    fHdfs2.getConf().set(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY,
        FsConstants.VIEWFS_URI.toString());

     defaultWorkingDirectory = fHdfs.makeQualified( new Path("/user/" + 
         UserGroupInformation.getCurrentUser().getShortUserName()));
     defaultWorkingDirectory2 = fHdfs2.makeQualified( new Path("/user/" + 
- 
2.19.1.windows.1

