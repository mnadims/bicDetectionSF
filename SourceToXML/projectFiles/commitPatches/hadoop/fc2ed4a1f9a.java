From fc2ed4a1f9a19d61f5e3cb4fd843604f0c7fe95f Mon Sep 17 00:00:00 2001
From: Colin Patrick Mccabe <cmccabe@cloudera.com>
Date: Mon, 8 Jun 2015 17:49:31 -0700
Subject: [PATCH] HADOOP-11347. RawLocalFileSystem#mkdir and create should
 honor umask (Varun Saxena via Colin P. McCabe)

--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../apache/hadoop/fs/RawLocalFileSystem.java  |  45 ++++---
 .../fs/TestLocalFileSystemPermission.java     | 111 +++++++++++++++++-
 3 files changed, 134 insertions(+), 25 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index fa6e4b75e53..ce8baeeaad7 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -482,6 +482,9 @@ Trunk (Unreleased)
 
     HADOOP-9905. remove dependency of zookeeper for hadoop-client (vinayakumarb)
 
    HADOOP-11347. RawLocalFileSystem#mkdir and create should honor umask (Varun
    Saxena via Colin P. McCabe)

   OPTIMIZATIONS
 
     HADOOP-7761. Improve the performance of raw comparisons. (todd)
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
index 56dd7adaf7e..b94d9d9ce8c 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
@@ -59,6 +59,8 @@
   // Temporary workaround for HADOOP-9652.
   private static boolean useDeprecatedFileStatus = true;
 
  private FsPermission umask;

   @VisibleForTesting
   public static void useStatIfAvailable() {
     useDeprecatedFileStatus = !Stat.isAvailable();
@@ -92,6 +94,7 @@ public File pathToFile(Path path) {
   public void initialize(URI uri, Configuration conf) throws IOException {
     super.initialize(uri, conf);
     setConf(conf);
    umask = FsPermission.getUMask(conf);
   }
   
   /*******************************************************
@@ -211,9 +214,13 @@ public FSDataInputStream open(Path f, int bufferSize) throws IOException {
     private LocalFSFileOutputStream(Path f, boolean append,
         FsPermission permission) throws IOException {
       File file = pathToFile(f);
      if (!append && permission == null) {
        permission = FsPermission.getFileDefault();
      }
       if (permission == null) {
         this.fos = new FileOutputStream(file, append);
       } else {
        permission = permission.applyUMask(umask);
         if (Shell.WINDOWS && NativeIO.isAvailable()) {
           this.fos = NativeIO.Windows.createFileOutputStreamWithMode(file,
               append, permission.toShort());
@@ -484,27 +491,27 @@ protected boolean mkOneDir(File p2f) throws IOException {
   protected boolean mkOneDirWithMode(Path p, File p2f, FsPermission permission)
       throws IOException {
     if (permission == null) {
      return p2f.mkdir();
    } else {
      if (Shell.WINDOWS && NativeIO.isAvailable()) {
        try {
          NativeIO.Windows.createDirectoryWithMode(p2f, permission.toShort());
          return true;
        } catch (IOException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(
                "NativeIO.createDirectoryWithMode error, path = %s, mode = %o",
                p2f, permission.toShort()), e);
          }
          return false;
        }
      } else {
        boolean b = p2f.mkdir();
        if (b) {
          setPermission(p, permission);
      permission = FsPermission.getDirDefault();
    }
    permission = permission.applyUMask(umask);
    if (Shell.WINDOWS && NativeIO.isAvailable()) {
      try {
        NativeIO.Windows.createDirectoryWithMode(p2f, permission.toShort());
        return true;
      } catch (IOException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format(
              "NativeIO.createDirectoryWithMode error, path = %s, mode = %o",
              p2f, permission.toShort()), e);
         }
        return b;
        return false;
      }
    } else {
      boolean b = p2f.mkdir();
      if (b) {
        setPermission(p, permission);
       }
      return b;
     }
   }
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java
index 5e985737d3c..148cf3e1036 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java
@@ -54,12 +54,75 @@ private Path writeFile(FileSystem fs, String name) throws IOException {
     return f;
   }
 
  private void cleanupFile(FileSystem fs, Path name) throws IOException {
  private Path writeFile(FileSystem fs, String name, FsPermission perm) throws IOException {
    Path f = new Path(TEST_PATH_PREFIX + name);
    FSDataOutputStream stm = fs.create(f, perm, true, 2048, (short)1, 32 * 1024 * 1024, null);
    stm.writeBytes("42\n");
    stm.close();
    return f;
  }

  private void cleanup(FileSystem fs, Path name) throws IOException {
     assertTrue(fs.exists(name));
     fs.delete(name, true);
     assertTrue(!fs.exists(name));
   }
 
  public void testLocalFSDirsetPermission() throws IOException {
    if (Path.WINDOWS) {
      System.out.println("Cannot run test for Windows");
      return;
    }
    Configuration conf = new Configuration();
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "044");
    LocalFileSystem localfs = FileSystem.getLocal(conf);
    Path dir = new Path(TEST_PATH_PREFIX + "dir");
    localfs.mkdirs(dir);
    try {
      FsPermission initialPermission = getPermission(localfs, dir);
      assertEquals(
          FsPermission.getDirDefault().applyUMask(FsPermission.getUMask(conf)),
          initialPermission);
    } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      return;
    }

    FsPermission perm = new FsPermission((short)0755);
    Path dir1 = new Path(TEST_PATH_PREFIX + "dir1");
    localfs.mkdirs(dir1, perm);
    try {
      FsPermission initialPermission = getPermission(localfs, dir1);
      assertEquals(perm.applyUMask(FsPermission.getUMask(conf)), initialPermission);
    } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      return;
    }

    Path dir2 = new Path(TEST_PATH_PREFIX + "dir2");
    localfs.mkdirs(dir2);
    try {
      FsPermission initialPermission = getPermission(localfs, dir2);
      Path copyPath = new Path(TEST_PATH_PREFIX + "dir_copy");
      localfs.rename(dir2, copyPath);
      FsPermission copyPermission = getPermission(localfs, copyPath);
      assertEquals(copyPermission, initialPermission);
      dir2 = copyPath;
    } catch (Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      return;
    } finally {
      cleanup(localfs, dir);
      cleanup(localfs, dir1);
      if (localfs.exists(dir2)) {
        localfs.delete(dir2, true);
      }
    }
  }

   /** Test LocalFileSystem.setPermission */
   public void testLocalFSsetPermission() throws IOException {
     if (Path.WINDOWS) {
@@ -67,15 +130,44 @@ public void testLocalFSsetPermission() throws IOException {
       return;
     }
     Configuration conf = new Configuration();
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "044");
     LocalFileSystem localfs = FileSystem.getLocal(conf);
     String filename = "foo";
     Path f = writeFile(localfs, filename);
     try {
       FsPermission initialPermission = getPermission(localfs, f);
      System.out.println(filename + ": " + initialPermission);
      assertEquals(FsPermission.getFileDefault().applyUMask(FsPermission.getUMask(conf)), initialPermission);
      assertEquals(
          FsPermission.getFileDefault().applyUMask(FsPermission.getUMask(conf)),
          initialPermission);
    } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      return;
     }
    catch(Exception e) {

    String filename1 = "foo1";
    FsPermission perm = new FsPermission((short)0755);
    Path f1 = writeFile(localfs, filename1, perm);
    try {
      FsPermission initialPermission = getPermission(localfs, f1);
      assertEquals(
          perm.applyUMask(FsPermission.getUMask(conf)), initialPermission);
    } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      return;
    }

    String filename2 = "foo2";
    Path f2 = writeFile(localfs, filename2);
    try {
      FsPermission initialPermission = getPermission(localfs, f2);
      Path copyPath = new Path(TEST_PATH_PREFIX + "/foo_copy");
      localfs.rename(f2, copyPath);
      FsPermission copyPermission = getPermission(localfs, copyPath);
      assertEquals(copyPermission, initialPermission);
      f2 = copyPath;
    } catch (Exception e) {
       System.out.println(StringUtils.stringifyException(e));
       System.out.println("Cannot run test");
       return;
@@ -92,7 +184,13 @@ public void testLocalFSsetPermission() throws IOException {
       localfs.setPermission(f, all);
       assertEquals(all, getPermission(localfs, f));
     }
    finally {cleanupFile(localfs, f);}
    finally {
      cleanup(localfs, f);
      cleanup(localfs, f1);
      if (localfs.exists(f2)) {
        localfs.delete(f2, true);
      }
    }
   }
 
   FsPermission getPermission(LocalFileSystem fs, Path p) throws IOException {
@@ -107,6 +205,7 @@ public void testLocalFSsetOwner() throws IOException {
     }
 
     Configuration conf = new Configuration();
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "044");
     LocalFileSystem localfs = FileSystem.getLocal(conf);
     String filename = "bar";
     Path f = writeFile(localfs, filename);
@@ -141,7 +240,7 @@ public void testLocalFSsetOwner() throws IOException {
                            "belongs to only one group.");
       }
     } 
    finally {cleanupFile(localfs, f);}
    finally {cleanup(localfs, f);}
   }
 
   static List<String> getGroups() throws IOException {
- 
2.19.1.windows.1

