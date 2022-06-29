From 91e832e7b47ff5088ca2bb54aa25f6f166d6c8d5 Mon Sep 17 00:00:00 2001
From: Eli Collins <eli@apache.org>
Date: Wed, 5 Sep 2012 19:42:09 +0000
Subject: [PATCH] HADOOP-8770. NN should not RPC to self to find trash
 defaults. Contributed by Eli Collins

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1381319 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  2 +
 .../main/java/org/apache/hadoop/fs/Trash.java | 22 ++++-
 .../apache/hadoop/fs/TrashPolicyDefault.java  | 21 +----
 .../java/org/apache/hadoop/fs/TestTrash.java  |  7 +-
 .../org/apache/hadoop/hdfs/TestDFSShell.java  | 93 +++++++++++++++++++
 .../org/apache/hadoop/hdfs/TestHDFSTrash.java | 54 -----------
 6 files changed, 122 insertions(+), 77 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 4c24291d75b..93ad985375a 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -465,6 +465,8 @@ Branch-2 ( Unreleased changes )
 
     HADOOP-8764. CMake: HADOOP-8737 broke ARM build. (Trevor Robinson via eli)
 
    HADOOP-8770. NN should not RPC to self to find trash defaults. (eli)

   BREAKDOWN OF HDFS-3042 SUBTASKS
 
     HADOOP-8220. ZKFailoverController doesn't handle failure to become active
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Trash.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Trash.java
index 56ccac3b9c8..2d5f540e2d7 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Trash.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Trash.java
@@ -68,8 +68,26 @@ public Trash(FileSystem fs, Configuration conf) throws IOException {
   public static boolean moveToAppropriateTrash(FileSystem fs, Path p,
       Configuration conf) throws IOException {
     Path fullyResolvedPath = fs.resolvePath(p);
    Trash trash = new Trash(FileSystem.get(fullyResolvedPath.toUri(), conf), conf);
    boolean success =  trash.moveToTrash(fullyResolvedPath);
    FileSystem fullyResolvedFs =
        FileSystem.get(fullyResolvedPath.toUri(), conf);
    // If the trash interval is configured server side then clobber this
    // configuration so that we always respect the server configuration.
    try {
      long trashInterval = fullyResolvedFs.getServerDefaults(
          fullyResolvedPath).getTrashInterval();
      if (0 != trashInterval) {
        Configuration confCopy = new Configuration(conf);
        confCopy.setLong(CommonConfigurationKeysPublic.FS_TRASH_INTERVAL_KEY,
            trashInterval);
        conf = confCopy;
      }
    } catch (Exception e) {
      // If we can not determine that trash is enabled server side then
      // bail rather than potentially deleting a file when trash is enabled.
      throw new IOException("Failed to get server trash configuration", e);
    }
    Trash trash = new Trash(fullyResolvedFs, conf);
    boolean success = trash.moveToTrash(fullyResolvedPath);
     if (success) {
       System.out.println("Moved: '" + p + "' to trash at: " +
           trash.getCurrentTrashDir() );
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java
index 1820c6619e2..05e629752c8 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java
@@ -79,24 +79,9 @@ public void initialize(Configuration conf, FileSystem fs, Path home) {
     this.trash = new Path(home, TRASH);
     this.homesParent = home.getParent();
     this.current = new Path(trash, CURRENT);
    long trashInterval = 0;
    try {
      trashInterval = fs.getServerDefaults(home).getTrashInterval();
    } catch (IOException ioe) {
      LOG.warn("Unable to get server defaults", ioe);
    }
    // If the trash interval is not configured or is disabled on the
    // server side then check the config which may be client side.
    if (0 == trashInterval) {
      this.deletionInterval = (long)(conf.getFloat(
          FS_TRASH_INTERVAL_KEY, FS_TRASH_INTERVAL_DEFAULT)
          * MSECS_PER_MINUTE);
    } else {
      this.deletionInterval = trashInterval * MSECS_PER_MINUTE;
    }
    // For the checkpoint interval use the given config instead of
    // checking the server as it's OK if a client starts an emptier
    // with a different interval than the server.
    this.deletionInterval = (long)(conf.getFloat(
        FS_TRASH_INTERVAL_KEY, FS_TRASH_INTERVAL_DEFAULT)
        * MSECS_PER_MINUTE);
     this.emptierInterval = (long)(conf.getFloat(
         FS_TRASH_CHECKPOINT_INTERVAL_KEY, FS_TRASH_CHECKPOINT_INTERVAL_DEFAULT)
         * MSECS_PER_MINUTE);
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java
index 70bd62fa000..fa79d5959e7 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java
@@ -99,7 +99,6 @@ public static void trashShell(final FileSystem fs, final Path base)
   }
 
   /**
   * 
    * Test trash for the shell's delete command for the default file system
    * specified in the paramter conf
    * @param conf 
@@ -429,8 +428,10 @@ public static void trashShell(final Configuration conf, final Path base,
       String output = byteStream.toString();
       System.setOut(stdout);
       System.setErr(stderr);
      assertTrue("skipTrash wasn't suggested as remedy to failed rm command",
        output.indexOf(("Consider using -skipTrash option")) != -1 );
      assertTrue("skipTrash wasn't suggested as remedy to failed rm command" +
          " or we deleted / even though we could not get server defaults",
          output.indexOf("Consider using -skipTrash option") != -1 ||
          output.indexOf("Failed to determine server trash configuration") != -1);
     }
 
   }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
index 1ebef6db51b..426d8e70152 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
@@ -57,6 +57,8 @@
 import org.apache.hadoop.util.ToolRunner;
 import org.junit.Test;
 
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_TRASH_INTERVAL_KEY;

 /**
  * This class tests commands from DFSShell.
  */
@@ -1480,4 +1482,95 @@ public void testCopyCommandsWithForceOption() throws Exception {
 
   }
 
  /**
   * Delete a file optionally configuring trash on the server and client.
   */
  private void deleteFileUsingTrash(
      boolean serverTrash, boolean clientTrash) throws Exception {
    // Run a cluster, optionally with trash enabled on the server
    Configuration serverConf = new HdfsConfiguration();
    if (serverTrash) {
      serverConf.setLong(FS_TRASH_INTERVAL_KEY, 1);
    }

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(serverConf)
      .numDataNodes(1).format(true).build();
    Configuration clientConf = new Configuration(serverConf);

    // Create a client, optionally with trash enabled
    if (clientTrash) {
      clientConf.setLong(FS_TRASH_INTERVAL_KEY, 1);
    } else {
      clientConf.setLong(FS_TRASH_INTERVAL_KEY, 0);
    }

    FsShell shell = new FsShell(clientConf);
    FileSystem fs = null;

    try {
      // Create and delete a file
      fs = cluster.getFileSystem();
      writeFile(fs, new Path(TEST_ROOT_DIR, "foo"));
      final String testFile = TEST_ROOT_DIR + "/foo";
      final String trashFile = shell.getCurrentTrashDir() + "/" + testFile;
      String[] argv = new String[] { "-rm", testFile };
      int res = ToolRunner.run(shell, argv);
      assertEquals("rm failed", 0, res);

      if (serverTrash) {
        // If the server config was set we should use it unconditionally
        assertTrue("File not in trash", fs.exists(new Path(trashFile)));
      } else if (clientTrash) {
        // If the server config was not set but the client config was
        // set then we should use it
        assertTrue("File not in trashed", fs.exists(new Path(trashFile)));
      } else {
        // If neither was set then we should not have trashed the file
        assertFalse("File was not removed", fs.exists(new Path(testFile)));
        assertFalse("File was trashed", fs.exists(new Path(trashFile)));
      }
    } finally {
      if (fs != null) {
        fs.close();
      }
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Test that the server trash configuration is respected when
   * the client configuration is not set.
   */
  @Test
  public void testServerConfigRespected() throws Exception {
    deleteFileUsingTrash(true, false);
  }

  /**
   * Test that server trash configuration is respected even when the
   * client configuration is set.
   */
  @Test
  public void testServerConfigRespectedWithClient() throws Exception {
    deleteFileUsingTrash(true, true);
  }

  /**
   * Test that the client trash configuration is respected when
   * the server configuration is not set.
   */
  @Test
  public void testClientConfigRespected() throws Exception {
    deleteFileUsingTrash(false, true);
  }

  /**
   * Test that trash is disabled by default.
   */
  @Test
  public void testNoTrashConfig() throws Exception {
    deleteFileUsingTrash(false, false);
  }
 }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java
index b10cab01e04..ad4d600f51d 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java
@@ -23,11 +23,6 @@
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.TestTrash;
import org.apache.hadoop.fs.Trash;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_TRASH_INTERVAL_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
 
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
@@ -62,53 +57,4 @@ public void testNonDefaultFS() throws IOException {
     conf.set(DFSConfigKeys.FS_DEFAULT_NAME_KEY, fs.getUri().toString());
     TestTrash.trashNonDefaultFS(conf);
   }

  /** Clients should always use trash if enabled server side */
  @Test
  public void testTrashEnabledServerSide() throws IOException {
    Configuration serverConf = new HdfsConfiguration();
    Configuration clientConf = new Configuration();

    // Enable trash on the server and client
    serverConf.setLong(FS_TRASH_INTERVAL_KEY, 1);
    clientConf.setLong(FS_TRASH_INTERVAL_KEY, 1);

    MiniDFSCluster cluster2 = null;
    try {
      cluster2 = new MiniDFSCluster.Builder(serverConf).numDataNodes(1).build();
      FileSystem fs = cluster2.getFileSystem();
      assertTrue(new Trash(fs, clientConf).isEnabled());

      // Disabling trash on the client is ignored
      clientConf.setLong(FS_TRASH_INTERVAL_KEY, 0);
      assertTrue(new Trash(fs, clientConf).isEnabled());
    } finally {
      if (cluster2 != null) cluster2.shutdown();
    }
  }

  /** Clients should always use trash if enabled client side */
  @Test
  public void testTrashEnabledClientSide() throws IOException {
    Configuration serverConf = new HdfsConfiguration();
    Configuration clientConf = new Configuration();
    
    // Disable server side
    serverConf.setLong(FS_TRASH_INTERVAL_KEY, 0);

    MiniDFSCluster cluster2 = null;
    try {
      cluster2 = new MiniDFSCluster.Builder(serverConf).numDataNodes(1).build();

      // Client side is disabled by default
      FileSystem fs = cluster2.getFileSystem();
      assertFalse(new Trash(fs, clientConf).isEnabled());

      // Enabling on the client works even though its disabled on the server
      clientConf.setLong(FS_TRASH_INTERVAL_KEY, 1);
      assertTrue(new Trash(fs, clientConf).isEnabled());
    } finally {
      if (cluster2 != null) cluster2.shutdown();
    }
  }
 }
- 
2.19.1.windows.1

