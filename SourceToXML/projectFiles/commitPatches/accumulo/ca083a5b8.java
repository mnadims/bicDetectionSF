From ca083a5b864b57650073204344bd5461d2a3d78e Mon Sep 17 00:00:00 2001
From: Jenna Huston <jenna.huston22@gmail.com>
Date: Thu, 9 Oct 2014 14:07:33 -0400
Subject: [PATCH] ACCUMULO-3181 VolumeChooser usage doesn't always comply with
 implied API contract

Signed-off-by: Sean Busbey <busbey@cloudera.com>
--
 .../org/apache/accumulo/server/ServerConstants.java |  4 ----
 .../org/apache/accumulo/server/fs/VolumeUtil.java   |  3 ++-
 .../org/apache/accumulo/server/init/Initialize.java | 13 ++++++++-----
 .../org/apache/accumulo/server/util/FileUtil.java   |  4 ++--
 .../accumulo/server/util/MetadataTableUtil.java     |  3 ++-
 .../accumulo/server/util/TabletOperations.java      |  2 +-
 .../java/org/apache/accumulo/master/Master.java     |  2 +-
 .../apache/accumulo/master/TabletGroupWatcher.java  |  6 +++---
 .../accumulo/master/tableOps/CreateTable.java       |  3 ++-
 .../org/apache/accumulo/tserver/log/DfsLogger.java  |  3 ++-
 10 files changed, 23 insertions(+), 20 deletions(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java b/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java
index 10a864c5e..880e2db72 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java
++ b/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java
@@ -143,10 +143,6 @@ public class ServerConstants {
     return VolumeConfiguration.prefix(getTablesDirs(), MetadataTable.ID);
   }
 
  public static String[] getTemporaryDirs() {
    return VolumeConfiguration.prefix(getBaseUris(), "tmp");
  }

   public static synchronized List<Pair<Path,Path>> getVolumeReplacements() {
 
     if (replacementsList == null) {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeUtil.java b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeUtil.java
index c87074b88..ea69f80e7 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeUtil.java
++ b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeUtil.java
@@ -248,7 +248,8 @@ public class VolumeUtil {
       throw new IllegalArgumentException("Unexpected table dir " + dir);
     }
 
    Path newDir = new Path(vm.choose(ServerConstants.getTablesDirs()) + "/" + dir.getParent().getName() + "/" + dir.getName());
    Path newDir = new Path(vm.choose(ServerConstants.getBaseUris()) + Path.SEPARATOR + ServerConstants.TABLE_DIR + Path.SEPARATOR + dir.getParent().getName()
        + Path.SEPARATOR + dir.getName());
 
     log.info("Updating directory for " + extent + " from " + dir + " to " + newDir);
     if (extent.isRootTablet()) {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java b/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
index fcecc37b4..14d5e98b1 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
++ b/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
@@ -224,9 +224,10 @@ public class Initialize {
 
     UUID uuid = UUID.randomUUID();
     // the actual disk locations of the root table and tablets
    String[] configuredTableDirs = VolumeConfiguration.prefix(VolumeConfiguration.getVolumeUris(ServerConfiguration.getSiteConfiguration()),
        ServerConstants.TABLE_DIR);
    final Path rootTablet = new Path(fs.choose(configuredTableDirs) + "/" + RootTable.ID + RootTable.ROOT_TABLET_LOCATION);
    String[] configuredVolumes = VolumeConfiguration.getVolumeUris(ServerConfiguration.getSiteConfiguration());
    final Path rootTablet = new Path(fs.choose(configuredVolumes) + Path.SEPARATOR + ServerConstants.TABLE_DIR + Path.SEPARATOR + RootTable.ID
        + RootTable.ROOT_TABLET_LOCATION);

     try {
       initZooKeeper(opts, uuid.toString(), instanceNamePath, rootTablet);
     } catch (Exception e) {
@@ -305,8 +306,10 @@ public class Initialize {
     // the actual disk locations of the metadata table and tablets
     final Path[] metadataTableDirs = paths(ServerConstants.getMetadataTableDirs());
 
    String tableMetadataTabletDir = fs.choose(VolumeConfiguration.prefix(ServerConstants.getMetadataTableDirs(), TABLE_TABLETS_TABLET_DIR));
    String defaultMetadataTabletDir = fs.choose(VolumeConfiguration.prefix(ServerConstants.getMetadataTableDirs(), Constants.DEFAULT_TABLET_LOCATION));
    String tableMetadataTabletDir = fs.choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + MetadataTable.ID
        + TABLE_TABLETS_TABLET_DIR;
    String defaultMetadataTabletDir = fs.choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + MetadataTable.ID
        + Constants.DEFAULT_TABLET_LOCATION;
 
     // initialize initial metadata config in zookeeper
     initMetadataConfig();
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/FileUtil.java b/server/base/src/main/java/org/apache/accumulo/server/util/FileUtil.java
index 0f7ac22cb..aa37e350b 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/FileUtil.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/FileUtil.java
@@ -79,11 +79,11 @@ public class FileUtil {
   private static final Logger log = Logger.getLogger(FileUtil.class);
   
   private static Path createTmpDir(AccumuloConfiguration acuConf, VolumeManager fs) throws IOException {
    String accumuloDir = fs.choose(ServerConstants.getTemporaryDirs());
    String accumuloDir = fs.choose(ServerConstants.getBaseUris());
     
     Path result = null;
     while (result == null) {
      result = new Path(accumuloDir + "/tmp/idxReduce_" + String.format("%09d", new Random().nextInt(Integer.MAX_VALUE)));
      result = new Path(accumuloDir + Path.SEPARATOR + "tmp/idxReduce_" + String.format("%09d", new Random().nextInt(Integer.MAX_VALUE)));
       
       try {
         fs.getFileStatus(result);
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java b/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
index f908d25f7..03635cc8c 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
@@ -880,9 +880,10 @@ public class MetadataTableUtil {
       Key k = entry.getKey();
       Mutation m = new Mutation(k.getRow());
       m.putDelete(k.getColumnFamily(), k.getColumnQualifier());
      String dir = volumeManager.choose(ServerConstants.getTablesDirs()) + "/" + tableId
      String dir = volumeManager.choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + tableId
           + new String(FastFormat.toZeroPaddedString(dirCount++, 8, 16, Constants.CLONE_PREFIX_BYTES));
       TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.put(m, new Value(dir.getBytes(Constants.UTF8)));

       bw.addMutation(m);
     }
 
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/TabletOperations.java b/server/base/src/main/java/org/apache/accumulo/server/util/TabletOperations.java
index b8e7113ff..2c9fe9c3e 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/TabletOperations.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/TabletOperations.java
@@ -38,7 +38,7 @@ public class TabletOperations {
     String lowDirectory;
     
     UniqueNameAllocator namer = UniqueNameAllocator.getInstance();
    String volume = fs.choose(ServerConstants.getTablesDirs());
    String volume = fs.choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR;
     
     while (true) {
       try {
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index 52f116fbf..ce4bc4165 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -236,7 +236,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
     if (!zoo.exists(dirZPath)) {
       Path oldPath = fs.getFullPath(FileType.TABLE, "/" + MetadataTable.ID + "/root_tablet");
       if (fs.exists(oldPath)) {
        String newPath = fs.choose(ServerConstants.getTablesDirs()) + "/" + RootTable.ID;
        String newPath = fs.choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + RootTable.ID;
         fs.mkdirs(new Path(newPath));
         if (!fs.rename(oldPath, new Path(newPath))) {
           throw new IOException("Failed to move root tablet from " + oldPath + " to " + newPath);
diff --git a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
index 0a3d1d0b1..f5ed9412f 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
++ b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
@@ -550,9 +550,9 @@ class TabletGroupWatcher extends Daemon {
       } else {
         // Recreate the default tablet to hold the end of the table
         Master.log.debug("Recreating the last tablet to point to " + extent.getPrevEndRow());
        String tdir = master.getFileSystem().choose(ServerConstants.getTablesDirs()) + "/" + extent.getTableId() + Constants.DEFAULT_TABLET_LOCATION;
        MetadataTableUtil.addTablet(new KeyExtent(extent.getTableId(), null, extent.getPrevEndRow()), tdir,
            SystemCredentials.get(), timeType, this.master.masterLock);
        String tdir = master.getFileSystem().choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + extent.getTableId()
            + Constants.DEFAULT_TABLET_LOCATION;
        MetadataTableUtil.addTablet(new KeyExtent(extent.getTableId(), null, extent.getPrevEndRow()), tdir, SystemCredentials.get(), timeType, this.master.masterLock);
       }
     } catch (Exception ex) {
       throw new AccumuloException(ex);
diff --git a/server/master/src/main/java/org/apache/accumulo/master/tableOps/CreateTable.java b/server/master/src/main/java/org/apache/accumulo/master/tableOps/CreateTable.java
index 5b64053c7..45164e107 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/tableOps/CreateTable.java
++ b/server/master/src/main/java/org/apache/accumulo/master/tableOps/CreateTable.java
@@ -176,7 +176,8 @@ class ChooseDir extends MasterRepo {
   @Override
   public Repo<Master> call(long tid, Master master) throws Exception {
     // Constants.DEFAULT_TABLET_LOCATION has a leading slash prepended to it so we don't need to add one here
    tableInfo.dir = master.getFileSystem().choose(ServerConstants.getTablesDirs()) + "/" + tableInfo.tableId + Constants.DEFAULT_TABLET_LOCATION;
    tableInfo.dir = master.getFileSystem().choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + tableInfo.tableId
        + Constants.DEFAULT_TABLET_LOCATION;
     return new CreateDir(tableInfo);
   }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java
index 2bd0b473a..0e02f05c0 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/log/DfsLogger.java
@@ -351,7 +351,8 @@ public class DfsLogger {
     log.debug("DfsLogger.open() begin");
     VolumeManager fs = conf.getFileSystem();
 
    logPath = fs.choose(ServerConstants.getWalDirs()) + "/" + logger + "/" + filename;
    logPath = fs.choose(ServerConstants.getBaseUris()) + Path.SEPARATOR + ServerConstants.WAL_DIR + Path.SEPARATOR + logger + Path.SEPARATOR + filename;

     metaReference = toString();
     try {
       short replication = (short) conf.getConfiguration().getCount(Property.TSERV_WAL_REPLICATION);
- 
2.19.1.windows.1

