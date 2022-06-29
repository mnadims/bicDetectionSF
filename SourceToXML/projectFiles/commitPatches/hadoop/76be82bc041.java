From 76be82bc0419affbe0103bc4f45c90926f08d0cc Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Mon, 18 Jul 2016 14:38:35 +0100
Subject: [PATCH] HADOOP-13073 RawLocalFileSystem does not react on changing
 umask. Contributed by Andras Bokor

--
 .../apache/hadoop/fs/RawLocalFileSystem.java  |   7 +-
 .../fs/TestLocalFileSystemPermission.java     | 102 +++++++++++++-----
 2 files changed, 78 insertions(+), 31 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
index cc41f4a41e8..0fcddcf0986 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
@@ -64,8 +64,6 @@
   // Temporary workaround for HADOOP-9652.
   private static boolean useDeprecatedFileStatus = true;
 
  private FsPermission umask;

   @VisibleForTesting
   public static void useStatIfAvailable() {
     useDeprecatedFileStatus = !Stat.isAvailable();
@@ -99,7 +97,6 @@ public File pathToFile(Path path) {
   public void initialize(URI uri, Configuration conf) throws IOException {
     super.initialize(uri, conf);
     setConf(conf);
    umask = FsPermission.getUMask(conf);
   }
   
   /*******************************************************
@@ -233,7 +230,7 @@ private LocalFSFileOutputStream(Path f, boolean append,
       if (permission == null) {
         this.fos = new FileOutputStream(file, append);
       } else {
        permission = permission.applyUMask(umask);
        permission = permission.applyUMask(FsPermission.getUMask(getConf()));
         if (Shell.WINDOWS && NativeIO.isAvailable()) {
           this.fos = NativeIO.Windows.createFileOutputStreamWithMode(file,
               append, permission.toShort());
@@ -510,7 +507,7 @@ protected boolean mkOneDirWithMode(Path p, File p2f, FsPermission permission)
     if (permission == null) {
       permission = FsPermission.getDirDefault();
     }
    permission = permission.applyUMask(umask);
    permission = permission.applyUMask(FsPermission.getUMask(getConf()));
     if (Shell.WINDOWS && NativeIO.isAvailable()) {
       try {
         NativeIO.Windows.createDirectoryWithMode(p2f, permission.toShort());
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java
index e37de1957bb..11e94a78c8b 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystemPermission.java
@@ -20,7 +20,6 @@
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.permission.*;
 import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.util.StringUtils;
 import org.apache.log4j.Level;
 import org.apache.hadoop.util.Shell;
 
@@ -28,11 +27,21 @@
 import java.util.*;
 
 import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
 
 /**
  * This class tests the local file system via the FileSystem abstraction.
  */
 public class TestLocalFileSystemPermission extends TestCase {

  public static final Logger LOGGER =
      LoggerFactory.getLogger(TestFcLocalFsPermission.class);

   static final String TEST_PATH_PREFIX = GenericTestUtils.getTempPath(
       TestLocalFileSystemPermission.class.getSimpleName());
 
@@ -64,12 +73,12 @@ private void cleanup(FileSystem fs, Path name) throws IOException {
 
   public void testLocalFSDirsetPermission() throws IOException {
     if (Path.WINDOWS) {
      System.out.println("Cannot run test for Windows");
      LOGGER.info("Cannot run test for Windows");
       return;
     }
    Configuration conf = new Configuration();
    LocalFileSystem localfs = FileSystem.getLocal(new Configuration());
    Configuration conf = localfs.getConf();
     conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "044");
    LocalFileSystem localfs = FileSystem.getLocal(conf);
     Path dir = new Path(TEST_PATH_PREFIX + "dir");
     localfs.mkdirs(dir);
     try {
@@ -78,8 +87,7 @@ public void testLocalFSDirsetPermission() throws IOException {
           FsPermission.getDirDefault().applyUMask(FsPermission.getUMask(conf)),
           initialPermission);
     } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      LOGGER.error("Cannot run test", e);
       return;
     }
 
@@ -90,8 +98,7 @@ public void testLocalFSDirsetPermission() throws IOException {
       FsPermission initialPermission = getPermission(localfs, dir1);
       assertEquals(perm.applyUMask(FsPermission.getUMask(conf)), initialPermission);
     } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      LOGGER.error("Cannot run test", e);
       return;
     }
 
@@ -105,8 +112,7 @@ public void testLocalFSDirsetPermission() throws IOException {
       assertEquals(copyPermission, initialPermission);
       dir2 = copyPath;
     } catch (Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      LOGGER.error("Cannot run test", e);
       return;
     } finally {
       cleanup(localfs, dir);
@@ -120,7 +126,7 @@ public void testLocalFSDirsetPermission() throws IOException {
   /** Test LocalFileSystem.setPermission */
   public void testLocalFSsetPermission() throws IOException {
     if (Path.WINDOWS) {
      System.out.println("Cannot run test for Windows");
      LOGGER.info("Cannot run test for Windows");
       return;
     }
     Configuration conf = new Configuration();
@@ -134,8 +140,7 @@ public void testLocalFSsetPermission() throws IOException {
           FsPermission.getFileDefault().applyUMask(FsPermission.getUMask(conf)),
           initialPermission);
     } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      LOGGER.error("Cannot run test", e);
       return;
     }
 
@@ -147,8 +152,7 @@ public void testLocalFSsetPermission() throws IOException {
       assertEquals(
           perm.applyUMask(FsPermission.getUMask(conf)), initialPermission);
     } catch(Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      LOGGER.error("Cannot run test", e);
       return;
     }
 
@@ -162,8 +166,7 @@ public void testLocalFSsetPermission() throws IOException {
       assertEquals(copyPermission, initialPermission);
       f2 = copyPath;
     } catch (Exception e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      LOGGER.error("Cannot run test", e);
       return;
     }
 
@@ -191,10 +194,10 @@ FsPermission getPermission(LocalFileSystem fs, Path p) throws IOException {
     return fs.getFileStatus(p).getPermission();
   }
 
  /** Test LocalFileSystem.setOwner */
  /** Test LocalFileSystem.setOwner. */
   public void testLocalFSsetOwner() throws IOException {
     if (Path.WINDOWS) {
      System.out.println("Cannot run test for Windows");
      LOGGER.info("Cannot run test for Windows");
       return;
     }
 
@@ -206,16 +209,15 @@ public void testLocalFSsetOwner() throws IOException {
     List<String> groups = null;
     try {
       groups = getGroups();
      System.out.println(filename + ": " + getPermission(localfs, f));
      LOGGER.info("{}: {}", filename, getPermission(localfs, f));
     }
     catch(IOException e) {
      System.out.println(StringUtils.stringifyException(e));
      System.out.println("Cannot run test");
      LOGGER.error("Cannot run test", e);
       return;
     }
     if (groups == null || groups.size() < 1) {
      System.out.println("Cannot run test: need at least one group.  groups="
                         + groups);
      LOGGER.error("Cannot run test: need at least one group. groups={}",
          groups);
       return;
     }
 
@@ -230,13 +232,61 @@ public void testLocalFSsetOwner() throws IOException {
         localfs.setOwner(f, null, g1);
         assertEquals(g1, getGroup(localfs, f));
       } else {
        System.out.println("Not testing changing the group since user " +
                           "belongs to only one group.");
        LOGGER.info("Not testing changing the group since user " +
            "belongs to only one group.");
       }
     } 
     finally {cleanup(localfs, f);}
   }
 
  /**
   * Steps:
   * 1. Create a directory with default permissions: 777 with umask 022
   * 2. Check the directory has good permissions: 755
   * 3. Set the umask to 062.
   * 4. Create a new directory with default permissions.
   * 5. For this directory we expect 715 as permission not 755
   * @throws Exception we can throw away all the exception.
   */
  public void testSetUmaskInRealTime() throws Exception {
    if (Path.WINDOWS) {
      LOGGER.info("Cannot run test for Windows");
      return;
    }

    LocalFileSystem localfs = FileSystem.getLocal(new Configuration());
    Configuration conf = localfs.getConf();
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "022");
    LOGGER.info("Current umask is {}",
        conf.get(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY));
    Path dir = new Path(TEST_PATH_PREFIX + "dir");
    Path dir2 = new Path(TEST_PATH_PREFIX + "dir2");
    try {
      assertTrue(localfs.mkdirs(dir));
      FsPermission initialPermission = getPermission(localfs, dir);
      assertEquals(
          "With umask 022 permission should be 755 since the default " +
              "permission is 777", new FsPermission("755"), initialPermission);

      // Modify umask and create a new directory
      // and check if new umask is applied
      conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "062");
      assertTrue(localfs.mkdirs(dir2));
      FsPermission finalPermission = localfs.getFileStatus(dir2)
          .getPermission();
      assertThat("With umask 062 permission should not be 755 since the " +
          "default permission is 777", new FsPermission("755"),
          is(not(finalPermission)));
      assertEquals(
          "With umask 062 we expect 715 since the default permission is 777",
          new FsPermission("715"), finalPermission);
    } finally {
      conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "022");
      cleanup(localfs, dir);
      cleanup(localfs, dir2);
    }
  }

   static List<String> getGroups() throws IOException {
     List<String> a = new ArrayList<String>();
     String s = Shell.execCommand(Shell.getGroupsCommand());
- 
2.19.1.windows.1

