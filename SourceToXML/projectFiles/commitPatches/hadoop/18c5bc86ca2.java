From 18c5bc86ca256beb9d4ccd6588c0b0ebe9dfcbd0 Mon Sep 17 00:00:00 2001
From: Eli Collins <eli@apache.org>
Date: Fri, 17 Aug 2012 23:22:17 +0000
Subject: [PATCH] HADOOP-8689. Make trash a server side configuration option.
 Contributed by Eli Collins

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1374472 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  2 +
 .../java/org/apache/hadoop/fs/FileSystem.java |  4 +-
 .../apache/hadoop/fs/FsServerDefaults.java    |  8 ++-
 .../apache/hadoop/fs/TrashPolicyDefault.java  | 45 +++++++++-----
 .../apache/hadoop/fs/ftp/FtpConfigKeys.java   |  4 +-
 .../hadoop/fs/local/LocalConfigKeys.java      |  4 +-
 .../src/main/resources/core-default.xml       | 11 +++-
 .../java/org/apache/hadoop/fs/TestTrash.java  |  8 +--
 .../hadoop/hdfs/protocolPB/PBHelper.java      |  6 +-
 .../hdfs/server/namenode/FSNamesystem.java    |  6 +-
 .../hadoop/hdfs/server/namenode/NameNode.java |  4 +-
 .../hadoop-hdfs/src/main/proto/hdfs.proto     |  1 +
 .../org/apache/hadoop/hdfs/TestHDFSTrash.java | 59 +++++++++++++++++--
 13 files changed, 127 insertions(+), 35 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index c4ec4a783bb..e0fb6e87e0d 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -198,6 +198,8 @@ Branch-2 ( Unreleased changes )
     HADOOP-8388. Remove unused BlockLocation serialization.
     (Colin Patrick McCabe via eli)
 
    HADOOP-8689. Make trash a server side configuration option. (eli)

   NEW FEATURES
  
     HDFS-3042. Automatic failover support for NameNode HA (todd)
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
index c28e25340b2..13881c776f0 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
@@ -661,7 +661,9 @@ public FsServerDefaults getServerDefaults() throws IOException {
         64 * 1024, 
         getDefaultReplication(),
         conf.getInt("io.file.buffer.size", 4096),
        false);
        false,
        // NB: ignoring the client trash configuration
        CommonConfigurationKeysPublic.FS_TRASH_INTERVAL_DEFAULT);
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FsServerDefaults.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FsServerDefaults.java
index f019593a107..274311e6682 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FsServerDefaults.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FsServerDefaults.java
@@ -49,19 +49,21 @@ public Writable newInstance() {
   private short replication;
   private int fileBufferSize;
   private boolean encryptDataTransfer;
  private long trashInterval;
 
   public FsServerDefaults() {
   }
 
   public FsServerDefaults(long blockSize, int bytesPerChecksum,
       int writePacketSize, short replication, int fileBufferSize,
      boolean encryptDataTransfer) {
      boolean encryptDataTransfer, long trashInterval) {
     this.blockSize = blockSize;
     this.bytesPerChecksum = bytesPerChecksum;
     this.writePacketSize = writePacketSize;
     this.replication = replication;
     this.fileBufferSize = fileBufferSize;
     this.encryptDataTransfer = encryptDataTransfer;
    this.trashInterval = trashInterval;
   }
 
   public long getBlockSize() {
@@ -88,6 +90,10 @@ public boolean getEncryptDataTransfer() {
     return encryptDataTransfer;
   }
 
  public long getTrashInterval() {
    return trashInterval;
  }

   // /////////////////////////////////////////
   // Writable
   // /////////////////////////////////////////
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java
index b6e9e880a1c..07870df1a62 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/TrashPolicyDefault.java
@@ -34,7 +34,6 @@
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
 import org.apache.hadoop.fs.Options.Rename;
 import org.apache.hadoop.fs.permission.FsAction;
 import org.apache.hadoop.fs.permission.FsPermission;
@@ -66,6 +65,7 @@
 
   private Path current;
   private Path homesParent;
  private long emptierInterval;
 
   public TrashPolicyDefault() { }
 
@@ -79,8 +79,27 @@ public void initialize(Configuration conf, FileSystem fs, Path home) {
     this.trash = new Path(home, TRASH);
     this.homesParent = home.getParent();
     this.current = new Path(trash, CURRENT);
    this.deletionInterval = (long) (conf.getFloat(FS_TRASH_INTERVAL_KEY,
                                    FS_TRASH_INTERVAL_DEFAULT) *  MSECS_PER_MINUTE);
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
    this.emptierInterval = (long)(conf.getFloat(
        FS_TRASH_CHECKPOINT_INTERVAL_KEY, FS_TRASH_CHECKPOINT_INTERVAL_DEFAULT)
        * MSECS_PER_MINUTE);
   }
   
   private Path makeTrashRelativePath(Path basePath, Path rmFilePath) {
@@ -89,7 +108,7 @@ private Path makeTrashRelativePath(Path basePath, Path rmFilePath) {
 
   @Override
   public boolean isEnabled() {
    return (deletionInterval != 0);
    return deletionInterval != 0;
   }
 
   @Override
@@ -223,7 +242,7 @@ public Path getCurrentTrashDir() {
 
   @Override
   public Runnable getEmptier() throws IOException {
    return new Emptier(getConf());
    return new Emptier(getConf(), emptierInterval);
   }
 
   private class Emptier implements Runnable {
@@ -231,16 +250,14 @@ public Runnable getEmptier() throws IOException {
     private Configuration conf;
     private long emptierInterval;
 
    Emptier(Configuration conf) throws IOException {
    Emptier(Configuration conf, long emptierInterval) throws IOException {
       this.conf = conf;
      this.emptierInterval = (long) (conf.getFloat(FS_TRASH_CHECKPOINT_INTERVAL_KEY,
                                     FS_TRASH_CHECKPOINT_INTERVAL_DEFAULT) *
                                     MSECS_PER_MINUTE);
      if (this.emptierInterval > deletionInterval ||
          this.emptierInterval == 0) {
        LOG.warn("The configured interval for checkpoint is " +
                 this.emptierInterval + " minutes." +
                 " Using interval of " + deletionInterval +
      this.emptierInterval = emptierInterval;
      if (emptierInterval > deletionInterval || emptierInterval == 0) {
        LOG.info("The configured checkpoint interval is " +
                 (emptierInterval / MSECS_PER_MINUTE) + " minutes." +
                 " Using an interval of " +
                 (deletionInterval / MSECS_PER_MINUTE) +
                  " minutes that is used for deletion instead");
         this.emptierInterval = deletionInterval;
       }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ftp/FtpConfigKeys.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ftp/FtpConfigKeys.java
index b646dcaf2c1..0bb5de7faee 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ftp/FtpConfigKeys.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/ftp/FtpConfigKeys.java
@@ -45,6 +45,7 @@
                                                 "ftp.client-write-packet-size";
   public static final int     CLIENT_WRITE_PACKET_SIZE_DEFAULT = 64*1024;
   public static final boolean ENCRYPT_DATA_TRANSFER_DEFAULT = false;
  public static final long    FS_TRASH_INTERVAL_DEFAULT = 0;
   
   protected static FsServerDefaults getServerDefaults() throws IOException {
     return new FsServerDefaults(
@@ -53,7 +54,8 @@ protected static FsServerDefaults getServerDefaults() throws IOException {
         CLIENT_WRITE_PACKET_SIZE_DEFAULT,
         REPLICATION_DEFAULT,
         STREAM_BUFFER_SIZE_DEFAULT,
        ENCRYPT_DATA_TRANSFER_DEFAULT);
        ENCRYPT_DATA_TRANSFER_DEFAULT,
        FS_TRASH_INTERVAL_DEFAULT);
   }
 }
   
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/LocalConfigKeys.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/LocalConfigKeys.java
index da767d29dc3..76626c3aa03 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/LocalConfigKeys.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/LocalConfigKeys.java
@@ -44,6 +44,7 @@
                                                 "file.client-write-packet-size";
   public static final int CLIENT_WRITE_PACKET_SIZE_DEFAULT = 64*1024;
   public static final boolean ENCRYPT_DATA_TRANSFER_DEFAULT = false;
  public static final long FS_TRASH_INTERVAL_DEFAULT = 0;
 
   public static FsServerDefaults getServerDefaults() throws IOException {
     return new FsServerDefaults(
@@ -52,7 +53,8 @@ public static FsServerDefaults getServerDefaults() throws IOException {
         CLIENT_WRITE_PACKET_SIZE_DEFAULT,
         REPLICATION_DEFAULT,
         STREAM_BUFFER_SIZE_DEFAULT,
        ENCRYPT_DATA_TRANSFER_DEFAULT);
        ENCRYPT_DATA_TRANSFER_DEFAULT,
        FS_TRASH_INTERVAL_DEFAULT);
   }
 }
   
diff --git a/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml b/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml
index 25d5798de99..ca9210b6191 100644
-- a/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml
++ b/hadoop-common-project/hadoop-common/src/main/resources/core-default.xml
@@ -351,8 +351,12 @@
   <name>fs.trash.interval</name>
   <value>0</value>
   <description>Number of minutes after which the checkpoint
  gets deleted.
  If zero, the trash feature is disabled.
  gets deleted.  If zero, the trash feature is disabled.
  This option may be configured both on the server and the
  client. If trash is disabled server side then the client
  side configuration is checked. If trash is enabled on the
  server side then the value configured on the server is
  used and the client configuration value is ignored.
   </description>
 </property>
 
@@ -360,7 +364,8 @@
   <name>fs.trash.checkpoint.interval</name>
   <value>0</value>
   <description>Number of minutes between trash checkpoints.
  Should be smaller or equal to fs.trash.interval.
  Should be smaller or equal to fs.trash.interval. If zero,
  the value is set to the value of fs.trash.interval.
   Every time the checkpointer runs it creates a new checkpoint 
   out of current and removes checkpoints created more than 
   fs.trash.interval minutes ago.
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java
index d85b60a9936..8bfa7185b02 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestTrash.java
@@ -111,10 +111,10 @@ public static void trashShell(final Configuration conf, final Path base,
       throws IOException {
     FileSystem fs = FileSystem.get(conf);
 
    conf.set(FS_TRASH_INTERVAL_KEY, "0"); // disabled
    conf.setLong(FS_TRASH_INTERVAL_KEY, 0); // disabled
     assertFalse(new Trash(conf).isEnabled());
 
    conf.set(FS_TRASH_INTERVAL_KEY, "10"); // 10 minute
    conf.setLong(FS_TRASH_INTERVAL_KEY, 10); // 10 minute
     assertTrue(new Trash(conf).isEnabled());
 
     FsShell shell = new FsShell();
@@ -435,7 +435,7 @@ public static void trashShell(final Configuration conf, final Path base,
   }
 
   public static void trashNonDefaultFS(Configuration conf) throws IOException {
    conf.set(FS_TRASH_INTERVAL_KEY, "10"); // 10 minute
    conf.setLong(FS_TRASH_INTERVAL_KEY, 10); // 10 minute
     // attempt non-default FileSystem trash
     {
       final FileSystem lfs = FileSystem.getLocal(conf);
@@ -580,7 +580,7 @@ public static void performanceTestDeleteSameFile() throws IOException{
     FileSystem fs = FileSystem.getLocal(conf);
     
     conf.set("fs.defaultFS", fs.getUri().toString());
    conf.set(FS_TRASH_INTERVAL_KEY, "10"); //minutes..
    conf.setLong(FS_TRASH_INTERVAL_KEY, 10); //minutes..
     FsShell shell = new FsShell();
     shell.setConf(conf);
     //Path trashRoot = null;
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocolPB/PBHelper.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocolPB/PBHelper.java
index 44863b2b289..1361c47afc2 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocolPB/PBHelper.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocolPB/PBHelper.java
@@ -1002,7 +1002,8 @@ public static FsServerDefaults convert(FsServerDefaultsProto fs) {
         fs.getBlockSize(), fs.getBytesPerChecksum(), 
         fs.getWritePacketSize(), (short) fs.getReplication(),
         fs.getFileBufferSize(),
        fs.getEncryptDataTransfer());
        fs.getEncryptDataTransfer(),
        fs.getTrashInterval());
   }
   
   public static FsServerDefaultsProto convert(FsServerDefaults fs) {
@@ -1013,7 +1014,8 @@ public static FsServerDefaultsProto convert(FsServerDefaults fs) {
       setWritePacketSize(fs.getWritePacketSize())
       .setReplication(fs.getReplication())
       .setFileBufferSize(fs.getFileBufferSize())
      .setEncryptDataTransfer(fs.getEncryptDataTransfer()).build();
      .setEncryptDataTransfer(fs.getEncryptDataTransfer())
      .setTrashInterval(fs.getTrashInterval()).build();
   }
   
   public static FsPermissionProto convert(FsPermission p) {
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/FSNamesystem.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/FSNamesystem.java
index 7887eafe374..14f4e0b114b 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/FSNamesystem.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/FSNamesystem.java
@@ -19,6 +19,8 @@
 
 import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.IO_FILE_BUFFER_SIZE_DEFAULT;
 import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.IO_FILE_BUFFER_SIZE_KEY;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_TRASH_INTERVAL_KEY;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_TRASH_INTERVAL_DEFAULT;
 import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BLOCK_SIZE_DEFAULT;
 import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BLOCK_SIZE_KEY;
 import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_DEFAULT;
@@ -104,6 +106,7 @@
 import org.apache.hadoop.HadoopIllegalArgumentException;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
 import org.apache.hadoop.fs.ContentSummary;
 import org.apache.hadoop.fs.CreateFlag;
 import org.apache.hadoop.fs.FileAlreadyExistsException;
@@ -479,7 +482,8 @@ public static FSNamesystem loadFromDisk(Configuration conf,
           conf.getInt(DFS_CLIENT_WRITE_PACKET_SIZE_KEY, DFS_CLIENT_WRITE_PACKET_SIZE_DEFAULT),
           (short) conf.getInt(DFS_REPLICATION_KEY, DFS_REPLICATION_DEFAULT),
           conf.getInt(IO_FILE_BUFFER_SIZE_KEY, IO_FILE_BUFFER_SIZE_DEFAULT),
          conf.getBoolean(DFS_ENCRYPT_DATA_TRANSFER_KEY, DFS_ENCRYPT_DATA_TRANSFER_DEFAULT));
          conf.getBoolean(DFS_ENCRYPT_DATA_TRANSFER_KEY, DFS_ENCRYPT_DATA_TRANSFER_DEFAULT),
          conf.getLong(FS_TRASH_INTERVAL_KEY, FS_TRASH_INTERVAL_DEFAULT));
       
       this.maxFsObjects = conf.getLong(DFS_NAMENODE_MAX_OBJECTS_KEY, 
                                        DFS_NAMENODE_MAX_OBJECTS_DEFAULT);
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NameNode.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NameNode.java
index 2df693b3c4e..083b16be689 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NameNode.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NameNode.java
@@ -511,9 +511,7 @@ private void stopCommonServices() {
   }
   
   private void startTrashEmptier(Configuration conf) throws IOException {
    long trashInterval = conf.getLong(
        CommonConfigurationKeys.FS_TRASH_INTERVAL_KEY,
        CommonConfigurationKeys.FS_TRASH_INTERVAL_DEFAULT);
    long trashInterval = namesystem.getServerDefaults().getTrashInterval();  
     if (trashInterval == 0) {
       return;
     } else if (trashInterval < 0) {
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/proto/hdfs.proto b/hadoop-hdfs-project/hadoop-hdfs/src/main/proto/hdfs.proto
index a640ddaf49d..019fb58558e 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/proto/hdfs.proto
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/proto/hdfs.proto
@@ -188,6 +188,7 @@ message FsServerDefaultsProto {
   required uint32 replication = 4; // Actually a short - only 16 bits used
   required uint32 fileBufferSize = 5;
   optional bool encryptDataTransfer = 6 [default = false];
  optional uint64 trashInterval = 7 [default = 0];
 }
 
 
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java
index e4124e75c72..b10cab01e04 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestHDFSTrash.java
@@ -23,12 +23,18 @@
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.TestTrash;
import org.apache.hadoop.fs.Trash;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_TRASH_INTERVAL_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 /**
 * This class tests commands from Trash.
 * Test trash using HDFS
  */
 public class TestHDFSTrash {
   private static MiniDFSCluster cluster = null;
@@ -44,9 +50,6 @@ public static void tearDown() {
     if (cluster != null) { cluster.shutdown(); }
   }
 
  /**
   * Tests Trash on HDFS
   */
   @Test
   public void testTrash() throws IOException {
     TestTrash.trashShell(cluster.getFileSystem(), new Path("/"));
@@ -60,4 +63,52 @@ public void testNonDefaultFS() throws IOException {
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

