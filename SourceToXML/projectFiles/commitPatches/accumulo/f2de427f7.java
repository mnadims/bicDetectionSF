From f2de427f74a8a6ffa05c6209e7161e7349458241 Mon Sep 17 00:00:00 2001
From: Eric Newton <eric.newton@gmail.com>
Date: Tue, 25 Feb 2014 11:18:34 -0500
Subject: [PATCH] ACCUMULO-2401 removed many calls to deprecated
 AccumuloConfiguration.getSiteConfiguration() fixed hacky crypto tests that
 messed with the site configuration

--
 .../apache/accumulo/core/cli/ClientOpts.java  |   2 +-
 .../core/conf/DefaultConfiguration.java       |   2 +-
 .../blockfile/impl/CachableBlockFile.java     |  45 ++++----
 .../accumulo/core/file/rfile/PrintInfo.java   |   5 +-
 .../core/file/rfile/RFileOperations.java      |   6 +-
 .../accumulo/core/file/rfile/SplitLarge.java  |  10 +-
 .../core/file/rfile/bcfile/BCFile.java        |  15 +--
 .../core/file/rfile/bcfile/PrintInfo.java     |   9 +-
 .../accumulo/core/security/SecurityUtil.java  |   4 +-
 .../accumulo/core/util/shell/Shell.java       |   2 +-
 .../accumulo/core/zookeeper/ZooUtil.java      |   5 +-
 .../mapred/AccumuloFileOutputFormatTest.java  |   3 +
 .../accumulo/core/conf/PropertyTest.java      |   2 +-
 .../core/file/rfile/CreateCompatTestFile.java |   3 +-
 .../core/file/rfile/MultiLevelIndexTest.java  |   6 +-
 .../accumulo/core/file/rfile/RFileTest.java   |   8 +-
 .../core/security/crypto/CryptoTest.java      | 100 +++++-------------
 .../core/util/shell/ShellSetInstanceTest.java |   2 +-
 .../accumulo/server/ServerConstants.java      |   2 +-
 .../server/client/HdfsZooInstance.java        |   2 +-
 .../server/conf/ZooConfiguration.java         |   2 +-
 .../accumulo/server/init/Initialize.java      |   7 +-
 .../accumulo/gc/SimpleGarbageCollector.java   |   2 +-
 .../org/apache/accumulo/master/Master.java    |   2 +-
 .../accumulo/master/state/SetGoalState.java   |   3 +-
 .../org/apache/accumulo/monitor/Monitor.java  |   2 +-
 .../apache/accumulo/tracer/TraceServer.java   |   2 +-
 .../apache/accumulo/tserver/TabletServer.java |   4 +-
 .../accumulo/tserver/logger/LogReader.java    |   5 +-
 .../tserver/log/TestUpgradePathForWALogs.java |   8 +-
 .../randomwalk/concurrent/BulkImport.java     |   4 +-
 31 files changed, 118 insertions(+), 156 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
index ca126e3d7..7573f694b 100644
-- a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
++ b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
@@ -258,7 +258,7 @@ public class ClientOpts extends Help {
       };
       this.zookeepers = config.get(Property.INSTANCE_ZK_HOST);
       Path instanceDir = new Path(config.get(Property.INSTANCE_DFS_DIR), "instance_id");
      String instanceIDFromFile = ZooUtil.getInstanceIDFromHdfs(instanceDir);
      String instanceIDFromFile = ZooUtil.getInstanceIDFromHdfs(instanceDir, config);
       if (config.getBoolean(Property.INSTANCE_RPC_SSL_ENABLED))
         clientConfig.setProperty(ClientProperty.INSTANCE_RPC_SSL_ENABLED, "true");
       return cachedClientConfig = clientConfig.withInstance(UUID.fromString(instanceIDFromFile)).withZkHosts(zookeepers);
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/DefaultConfiguration.java b/core/src/main/java/org/apache/accumulo/core/conf/DefaultConfiguration.java
index 6162bc5ef..cfd2f3539 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/DefaultConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/DefaultConfiguration.java
@@ -53,7 +53,7 @@ public class DefaultConfiguration extends AccumuloConfiguration {
       // the following loop is super slow, it takes a few milliseconds, so cache it
       resolvedProps = new HashMap<String,String>();
       for (Property prop : Property.values())
        if (!prop.isExperimental() && !prop.getType().equals(PropertyType.PREFIX))
        if (!prop.getType().equals(PropertyType.PREFIX))
           resolvedProps.put(prop.getKey(), prop.getDefaultValue());
     }
     return resolvedProps;
diff --git a/core/src/main/java/org/apache/accumulo/core/file/blockfile/impl/CachableBlockFile.java b/core/src/main/java/org/apache/accumulo/core/file/blockfile/impl/CachableBlockFile.java
index 5e462159a..095a21838 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/blockfile/impl/CachableBlockFile.java
++ b/core/src/main/java/org/apache/accumulo/core/file/blockfile/impl/CachableBlockFile.java
@@ -23,6 +23,7 @@ import java.io.IOException;
 import java.io.InputStream;
 import java.lang.ref.SoftReference;
 
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.file.blockfile.ABlockReader;
 import org.apache.accumulo.core.file.blockfile.ABlockWriter;
 import org.apache.accumulo.core.file.blockfile.BlockFileReader;
@@ -55,18 +56,18 @@ public class CachableBlockFile {
     private BlockWrite _bw;
     private FSDataOutputStream fsout = null;
     
    public Writer(FileSystem fs, Path fName, String compressAlgor, Configuration conf) throws IOException {
    public Writer(FileSystem fs, Path fName, String compressAlgor, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
       this.fsout = fs.create(fName);
      init(fsout, compressAlgor, conf);
      init(fsout, compressAlgor, conf, accumuloConfiguration);
     }
     
    public Writer(FSDataOutputStream fsout, String compressAlgor, Configuration conf) throws IOException {
    public Writer(FSDataOutputStream fsout, String compressAlgor, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
       this.fsout = fsout;
      init(fsout, compressAlgor, conf);
      init(fsout, compressAlgor, conf, accumuloConfiguration);
     }
     
    private void init(FSDataOutputStream fsout, String compressAlgor, Configuration conf) throws IOException {
      _bc = new BCFile.Writer(fsout, compressAlgor, conf, false);
    private void init(FSDataOutputStream fsout, String compressAlgor, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
      _bc = new BCFile.Writer(fsout, compressAlgor, conf, false, accumuloConfiguration);
     }
     
     public ABlockWriter prepareMetaBlock(String name) throws IOException {
@@ -149,6 +150,7 @@ public class CachableBlockFile {
     private FileSystem fs;
     private Configuration conf;
     private boolean closed = false;
    private AccumuloConfiguration accumuloConfiguration = null;
     
     private interface BlockLoader {
       BlockReader get() throws IOException;
@@ -166,7 +168,7 @@ public class CachableBlockFile {
       
       @Override
       public BlockReader get() throws IOException {
        return getBCFile().getDataBlock(blockIndex);
        return getBCFile(accumuloConfiguration).getDataBlock(blockIndex);
       }
       
       @Override
@@ -190,7 +192,7 @@ public class CachableBlockFile {
       
       @Override
       public BlockReader get() throws IOException {
        return getBCFile().getDataBlock(offset, compressedSize, rawSize);
        return getBCFile(accumuloConfiguration).getDataBlock(offset, compressedSize, rawSize);
       }
       
       @Override
@@ -202,14 +204,16 @@ public class CachableBlockFile {
     private class MetaBlockLoader implements BlockLoader {
       
       private String name;
      private AccumuloConfiguration accumuloConfiguration;
       
      MetaBlockLoader(String name) {
      MetaBlockLoader(String name, AccumuloConfiguration accumuloConfiguration) {
         this.name = name;
        this.accumuloConfiguration = accumuloConfiguration;
       }
       
       @Override
       public BlockReader get() throws IOException {
        return getBCFile().getMetaBlock(name);
        return getBCFile(accumuloConfiguration).getMetaBlock(name);
       }
       
       @Override
@@ -218,7 +222,7 @@ public class CachableBlockFile {
       }
     }
     
    public Reader(FileSystem fs, Path dataFile, Configuration conf, BlockCache data, BlockCache index) throws IOException {
    public Reader(FileSystem fs, Path dataFile, Configuration conf, BlockCache data, BlockCache index, AccumuloConfiguration accumuloConfiguration) throws IOException {
       
       /*
        * Grab path create input stream grab len create file
@@ -229,24 +233,25 @@ public class CachableBlockFile {
       this._iCache = index;
       this.fs = fs;
       this.conf = conf;
      this.accumuloConfiguration = accumuloConfiguration;
     }
     
    public Reader(FSDataInputStream fsin, long len, Configuration conf, BlockCache data, BlockCache index) throws IOException {
    public Reader(FSDataInputStream fsin, long len, Configuration conf, BlockCache data, BlockCache index, AccumuloConfiguration accumuloConfiguration) throws IOException {
       this._dCache = data;
       this._iCache = index;
      init(fsin, len, conf);
      init(fsin, len, conf, accumuloConfiguration);
     }
 
    public Reader(FSDataInputStream fsin, long len, Configuration conf) throws IOException {
    public Reader(FSDataInputStream fsin, long len, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
       // this.fin = fsin;
      init(fsin, len, conf);
      init(fsin, len, conf, accumuloConfiguration);
     }
     
    private void init(FSDataInputStream fsin, long len, Configuration conf) throws IOException {
      this._bc = new BCFile.Reader(this, fsin, len, conf);
    private void init(FSDataInputStream fsin, long len, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
      this._bc = new BCFile.Reader(this, fsin, len, conf, accumuloConfiguration);
     }
     
    private synchronized BCFile.Reader getBCFile() throws IOException {
    private synchronized BCFile.Reader getBCFile(AccumuloConfiguration accumuloConfiguration) throws IOException {
       if (closed)
         throw new IllegalStateException("File " + fileName + " is closed");
       
@@ -254,7 +259,7 @@ public class CachableBlockFile {
         // lazily open file if needed
         Path path = new Path(fileName);
         fin = fs.open(path);
        init(fin, fs.getFileStatus(path).getLen(), conf);
        init(fin, fs.getFileStatus(path).getLen(), conf, accumuloConfiguration);
       }
       
       return _bc;
@@ -364,7 +369,7 @@ public class CachableBlockFile {
      */
     public BlockRead getMetaBlock(String blockName) throws IOException {
       String _lookup = this.fileName + "M" + blockName;
      return getBlock(_lookup, _iCache, new MetaBlockLoader(blockName));
      return getBlock(_lookup, _iCache, new MetaBlockLoader(blockName, accumuloConfiguration));
     }
     
     @Override
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java
index 899a5cd86..7cee0f94f 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java
@@ -49,7 +49,8 @@ public class PrintInfo {
     Configuration conf = new Configuration();
 
     @SuppressWarnings("deprecation")
    FileSystem hadoopFs = FileUtil.getFileSystem(conf, AccumuloConfiguration.getSiteConfiguration());
    AccumuloConfiguration aconf = AccumuloConfiguration.getSiteConfiguration();
    FileSystem hadoopFs = FileUtil.getFileSystem(conf, aconf);
     FileSystem localFs  = FileSystem.getLocal(conf);
     Opts opts = new Opts();
     opts.parseArgs(PrintInfo.class.getName(), args);
@@ -70,7 +71,7 @@ public class PrintInfo {
       else
         fs = hadoopFs.exists(path) ? hadoopFs : localFs; // fall back to local
       
      CachableBlockFile.Reader _rdr = new CachableBlockFile.Reader(fs, path, conf, null, null);
      CachableBlockFile.Reader _rdr = new CachableBlockFile.Reader(fs, path, conf, null, null, aconf);
       Reader iter = new RFile.Reader(_rdr);
       
       iter.printInfo();
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
index 5374332a4..b36141347 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
@@ -59,7 +59,7 @@ public class RFileOperations extends FileOperations {
     // long len = fs.getFileStatus(path).getLen();
     // FSDataInputStream in = fs.open(path);
     // Reader reader = new RFile.Reader(in, len , conf);
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(fs, path, conf, dataCache, indexCache);
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(fs, path, conf, dataCache, indexCache, acuconf);
     final Reader reader = new RFile.Reader(_cbr);
     
     return reader.getIndex();
@@ -75,7 +75,7 @@ public class RFileOperations extends FileOperations {
       BlockCache dataCache, BlockCache indexCache) throws IOException {
     Path path = new Path(file);
     
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(fs, path, conf, dataCache, indexCache);
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(fs, path, conf, dataCache, indexCache, acuconf);
     Reader iter = new RFile.Reader(_cbr);
     
     if (seekToBeginning) {
@@ -121,7 +121,7 @@ public class RFileOperations extends FileOperations {
     
     String compression = acuconf.get(Property.TABLE_FILE_COMPRESSION_TYPE);
     
    CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(fs.create(new Path(file), false, bufferSize, (short) rep, block), compression, conf);
    CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(fs.create(new Path(file), false, bufferSize, (short) rep, block), compression, conf, acuconf);
     Writer writer = new RFile.Writer(_cbw, (int) blockSize, (int) indexBlockSize);
     return writer;
   }
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/SplitLarge.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/SplitLarge.java
index e19e5f32b..53e4aaa84 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/SplitLarge.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/SplitLarge.java
@@ -20,6 +20,7 @@ import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ByteSequence;
@@ -58,8 +59,9 @@ public class SplitLarge {
     opts.parseArgs(SplitLarge.class.getName(), args);
     
     for (String file : opts.files) {
      AccumuloConfiguration aconf = DefaultConfiguration.getDefaultConfiguration(); 
       Path path = new Path(file);
      CachableBlockFile.Reader rdr = new CachableBlockFile.Reader(fs, path, conf, null, null);
      CachableBlockFile.Reader rdr = new CachableBlockFile.Reader(fs, path, conf, null, null, aconf);
       Reader iter = new RFile.Reader(rdr);
       
       if (!file.endsWith(".rf")) {
@@ -68,10 +70,10 @@ public class SplitLarge {
       String smallName = file.substring(0, file.length() - 3) + "_small.rf";
       String largeName = file.substring(0, file.length() - 3) + "_large.rf";
       
      int blockSize = (int) DefaultConfiguration.getDefaultConfiguration().getMemoryInBytes(Property.TABLE_FILE_BLOCK_SIZE);
      Writer small = new RFile.Writer(new CachableBlockFile.Writer(fs, new Path(smallName), "gz", conf), blockSize);
      int blockSize = (int) aconf.getMemoryInBytes(Property.TABLE_FILE_BLOCK_SIZE);
      Writer small = new RFile.Writer(new CachableBlockFile.Writer(fs, new Path(smallName), "gz", conf, aconf), blockSize);
       small.startDefaultLocalityGroup();
      Writer large = new RFile.Writer(new CachableBlockFile.Writer(fs, new Path(largeName), "gz", conf), blockSize);
      Writer large = new RFile.Writer(new CachableBlockFile.Writer(fs, new Path(largeName), "gz", conf, aconf), blockSize);
       large.startDefaultLocalityGroup();
 
       iter.seek(new Range(), new ArrayList<ByteSequence>(), false);
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/BCFile.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/BCFile.java
index e8f45ccf0..6c3ea0d0a 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/BCFile.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/BCFile.java
@@ -346,7 +346,7 @@ public final class BCFile {
      * @throws IOException
      * @see Compression#getSupportedAlgorithms
      */
    public Writer(FSDataOutputStream fout, String compressionName, Configuration conf, boolean trackDataBlocks) throws IOException {
    public Writer(FSDataOutputStream fout, String compressionName, Configuration conf, boolean trackDataBlocks, AccumuloConfiguration accumuloConfiguration) throws IOException {
       if (fout.getPos() != 0) {
         throw new IOException("Output file not at zero offset.");
       }
@@ -360,9 +360,6 @@ public final class BCFile {
 
       // Set up crypto-related detail, including secret key generation and encryption
 
      @SuppressWarnings("deprecation")
      AccumuloConfiguration accumuloConfiguration = AccumuloConfiguration.getSiteConfiguration();

       this.cryptoModule = CryptoModuleFactory.getCryptoModule(accumuloConfiguration);
       this.cryptoParams = new BCFileCryptoModuleParameters();
       CryptoModuleFactory.fillParamsObjectFromConfiguration(cryptoParams, accumuloConfiguration);
@@ -739,7 +736,7 @@ public final class BCFile {
      *          Length of the corresponding file
      * @throws IOException
      */
    public Reader(FSDataInputStream fin, long fileLength, Configuration conf) throws IOException {
    public Reader(FSDataInputStream fin, long fileLength, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
 
       this.in = fin;
       this.conf = conf;
@@ -775,9 +772,6 @@ public final class BCFile {
       // If they exist, read the crypto parameters
       if (!version.equals(BCFile.API_VERSION_1)) {
 
        @SuppressWarnings("deprecation")
        AccumuloConfiguration accumuloConfiguration = AccumuloConfiguration.getSiteConfiguration();

         // read crypto parameters
         fin.seek(offsetCryptoParameters);
         cryptoParams = new BCFileCryptoModuleParameters();
@@ -822,7 +816,7 @@ public final class BCFile {
       }
     }
 
    public Reader(CachableBlockFile.Reader cache, FSDataInputStream fin, long fileLength, Configuration conf) throws IOException {
    public Reader(CachableBlockFile.Reader cache, FSDataInputStream fin, long fileLength, Configuration conf, AccumuloConfiguration accumuloConfiguration) throws IOException {
       this.in = fin;
       this.conf = conf;
 
@@ -864,9 +858,6 @@ public final class BCFile {
         // If they exist, read the crypto parameters
         if (!version.equals(BCFile.API_VERSION_1) && cachedCryptoParams == null) {
 
          @SuppressWarnings("deprecation")
          AccumuloConfiguration accumuloConfiguration = AccumuloConfiguration.getSiteConfiguration();

           // read crypto parameters
           fin.seek(offsetCryptoParameters);
           cryptoParams = new BCFileCryptoModuleParameters();
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/PrintInfo.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/PrintInfo.java
index a7464c634..4809d8072 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/PrintInfo.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/bcfile/PrintInfo.java
@@ -30,11 +30,11 @@ import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 
 public class PrintInfo {
  public static void printMetaBlockInfo(Configuration conf, FileSystem fs, Path path) throws IOException {
  public static void printMetaBlockInfo(Configuration conf, FileSystem fs, Path path, AccumuloConfiguration accumuloConfiguration) throws IOException {
     FSDataInputStream fsin = fs.open(path);
     BCFile.Reader bcfr = null;
     try {
      bcfr = new BCFile.Reader(fsin, fs.getFileStatus(path).getLen(), conf);
      bcfr = new BCFile.Reader(fsin, fs.getFileStatus(path).getLen(), conf, accumuloConfiguration);
       
       Set<Entry<String,MetaIndexEntry>> es = bcfr.metaIndex.index.entrySet();
       
@@ -56,7 +56,8 @@ public class PrintInfo {
   public static void main(String[] args) throws Exception {
     Configuration conf = new Configuration();
     @SuppressWarnings("deprecation")
    FileSystem hadoopFs = FileUtil.getFileSystem(conf, AccumuloConfiguration.getSiteConfiguration());
    AccumuloConfiguration siteConf = AccumuloConfiguration.getSiteConfiguration();
    FileSystem hadoopFs = FileUtil.getFileSystem(conf, siteConf);
     FileSystem localFs = FileSystem.getLocal(conf);
     Path path = new Path(args[0]);
     FileSystem fs;
@@ -64,6 +65,6 @@ public class PrintInfo {
       fs = path.getFileSystem(conf);
     else
       fs = hadoopFs.exists(path) ? hadoopFs : localFs; // fall back to local
    printMetaBlockInfo(conf, fs, path);
    printMetaBlockInfo(conf, fs, path, siteConf);
   }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java b/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java
index 59c19749d..43a2d4626 100644
-- a/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java
@@ -35,9 +35,7 @@ public class SecurityUtil {
    * This method is for logging a server in kerberos. If this is used in client code, it will fail unless run as the accumulo keytab's owner. Instead, use
    * {@link #login(String, String)}
    */
  public static void serverLogin() {
    @SuppressWarnings("deprecation")
    AccumuloConfiguration acuConf = AccumuloConfiguration.getSiteConfiguration();
  public static void serverLogin(AccumuloConfiguration acuConf) {
     String keyTab = acuConf.getPath(Property.GENERAL_KERBEROS_KEYTAB);
     if (keyTab == null || keyTab.length() == 0)
       return;
diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
index 7f1b8dc49..faded5633 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
@@ -434,7 +434,7 @@ public class Shell extends ShellOptions {
       AccumuloConfiguration conf = SiteConfiguration.getInstance(ServerConfigurationUtil.convertClientConfig(DefaultConfiguration.getInstance(), clientConfig));
       if (instanceName == null) {
         Path instanceDir = new Path(conf.get(Property.INSTANCE_DFS_DIR), "instance_id");
        instanceId = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(instanceDir));
        instanceId = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(instanceDir, conf));
       }
       if (keepers == null) {
         keepers = conf.get(Property.INSTANCE_ZK_HOST);
diff --git a/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooUtil.java b/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooUtil.java
index fa0bdf630..a062602fc 100644
-- a/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooUtil.java
@@ -46,11 +46,10 @@ public class ZooUtil extends org.apache.accumulo.fate.zookeeper.ZooUtil {
    * Utility to support certain client side utilities to minimize command-line options.
    */
 
  public static String getInstanceIDFromHdfs(Path instanceDirectory) {
  public static String getInstanceIDFromHdfs(Path instanceDirectory, AccumuloConfiguration conf) {
     try {
 
      @SuppressWarnings("deprecation")
      FileSystem fs = FileUtil.getFileSystem(instanceDirectory.toString(), CachedConfiguration.getInstance(), AccumuloConfiguration.getSiteConfiguration());
      FileSystem fs = FileUtil.getFileSystem(instanceDirectory.toString(), CachedConfiguration.getInstance(), conf);
       FileStatus[] files = null;
       try {
         files = fs.listStatus(instanceDirectory);
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
index 565b61889..aad544b85 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
@@ -46,6 +46,7 @@ import org.apache.hadoop.mapred.Reporter;
 import org.apache.hadoop.mapred.lib.IdentityMapper;
 import org.apache.hadoop.util.Tool;
 import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
 import org.junit.BeforeClass;
 import org.junit.Rule;
 import org.junit.Test;
@@ -108,6 +109,7 @@ public class AccumuloFileOutputFormatTest {
             if (index == 2)
               fail();
           } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e, e);
             assertEquals(2, index);
           }
         } catch (AssertionError e) {
@@ -191,6 +193,7 @@ public class AccumuloFileOutputFormatTest {
     File f = folder.newFile("writeBadVisibility");
     f.delete();
     MRTester.main(new String[] {"root", "", BAD_TABLE, f.getAbsolutePath()});
    Logger.getLogger(this.getClass()).error(e1, e1);
     assertNull(e1);
     assertNull(e2);
   }
diff --git a/core/src/test/java/org/apache/accumulo/core/conf/PropertyTest.java b/core/src/test/java/org/apache/accumulo/core/conf/PropertyTest.java
index 9609e02cb..600caab83 100644
-- a/core/src/test/java/org/apache/accumulo/core/conf/PropertyTest.java
++ b/core/src/test/java/org/apache/accumulo/core/conf/PropertyTest.java
@@ -148,7 +148,7 @@ public class PropertyTest {
     TreeSet<String> expected = new TreeSet<String>();
     for (Entry<String,String> entry : conf) {
       String key = entry.getKey();
      if (key.equals(Property.INSTANCE_SECRET.getKey()) || key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")
      if (key.equals(Property.INSTANCE_SECRET.getKey()) || key.toLowerCase().contains("password") || key.toLowerCase().endsWith("secret")
           || key.startsWith(Property.TRACE_TOKEN_PROPERTY_PREFIX.getKey()))
         expected.add(key);
     }
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/CreateCompatTestFile.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/CreateCompatTestFile.java
index e99b3f29a..46c2f0e63 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/CreateCompatTestFile.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/CreateCompatTestFile.java
@@ -19,6 +19,7 @@ package org.apache.accumulo.core.file.rfile;
 import java.util.HashSet;
 import java.util.Set;
 
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
@@ -55,7 +56,7 @@ public class CreateCompatTestFile {
   public static void main(String[] args) throws Exception {
     Configuration conf = new Configuration();
     FileSystem fs = FileSystem.get(conf);
    CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(fs, new Path(args[0]), "gz", conf);
    CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(fs, new Path(args[0]), "gz", conf, AccumuloConfiguration.getDefaultConfiguration());
     RFile.Writer writer = new RFile.Writer(_cbw, 1000);
     
     writer.startNewLocalityGroup("lg1", ncfs(nf("cf_", 1), nf("cf_", 2)));
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java
index 00405d1af..04874955f 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java
@@ -22,6 +22,7 @@ import java.util.Random;
 
 import junit.framework.TestCase;
 
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.file.blockfile.ABlockWriter;
 import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
@@ -51,9 +52,10 @@ public class MultiLevelIndexTest extends TestCase {
   }
   
   private void runTest(int maxBlockSize, int num) throws IOException {
    AccumuloConfiguration aconf = AccumuloConfiguration.getDefaultConfiguration();
     ByteArrayOutputStream baos = new ByteArrayOutputStream();
     FSDataOutputStream dos = new FSDataOutputStream(baos, new FileSystem.Statistics("a"));
    CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(dos, "gz", CachedConfiguration.getInstance());
    CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(dos, "gz", CachedConfiguration.getInstance(), aconf);
     
     BufferedWriter mliw = new BufferedWriter(new Writer(_cbw, maxBlockSize));
     
@@ -73,7 +75,7 @@ public class MultiLevelIndexTest extends TestCase {
     byte[] data = baos.toByteArray();
     SeekableByteArrayInputStream bais = new SeekableByteArrayInputStream(data);
     FSDataInputStream in = new FSDataInputStream(bais);
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in, data.length, CachedConfiguration.getInstance());
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in, data.length, CachedConfiguration.getInstance(), aconf);
     
     Reader reader = new Reader(_cbr, RFile.RINDEX_VER_7);
     BlockRead rootIn = _cbr.getMetaBlock("root");
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
index cffe2cb08..fb9658ce0 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
@@ -178,7 +178,7 @@ public class RFileTest {
 
       baos = new ByteArrayOutputStream();
       dos = new FSDataOutputStream(baos, new FileSystem.Statistics("a"));
      CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(dos, "gz", conf);
      CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(dos, "gz", conf, AccumuloConfiguration.getDefaultConfiguration());
       writer = new RFile.Writer(_cbw, 1000, 1000);
 
       if (startDLG)
@@ -211,7 +211,7 @@ public class RFileTest {
       LruBlockCache indexCache = new LruBlockCache(100000000, 100000);
       LruBlockCache dataCache = new LruBlockCache(100000000, 100000);
 
      CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in, fileLength, conf, dataCache, indexCache);
      CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in, fileLength, conf, dataCache, indexCache, AccumuloConfiguration.getDefaultConfiguration());
       reader = new RFile.Reader(_cbr);
       iter = new ColumnFamilySkippingIterator(reader);
 
@@ -1546,7 +1546,9 @@ public class RFileTest {
     byte data[] = baos.toByteArray();
     SeekableByteArrayInputStream bais = new SeekableByteArrayInputStream(data);
     FSDataInputStream in2 = new FSDataInputStream(bais);
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in2, data.length, CachedConfiguration.getInstance());
    @SuppressWarnings("deprecation")
    AccumuloConfiguration aconf = AccumuloConfiguration.getSiteConfiguration();
    CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in2, data.length, CachedConfiguration.getInstance(), aconf);
     Reader reader = new RFile.Reader(_cbr);
     checkIndex(reader);
 
diff --git a/core/src/test/java/org/apache/accumulo/core/security/crypto/CryptoTest.java b/core/src/test/java/org/apache/accumulo/core/security/crypto/CryptoTest.java
index 963f41ce1..605e43ae4 100644
-- a/core/src/test/java/org/apache/accumulo/core/security/crypto/CryptoTest.java
++ b/core/src/test/java/org/apache/accumulo/core/security/crypto/CryptoTest.java
@@ -34,6 +34,7 @@ import java.security.NoSuchAlgorithmException;
 import java.security.NoSuchProviderException;
 import java.security.SecureRandom;
 import java.util.Arrays;
import java.util.Map.Entry;
 
 import javax.crypto.BadPaddingException;
 import javax.crypto.Cipher;
@@ -42,7 +43,8 @@ import javax.crypto.NoSuchPaddingException;
 import javax.crypto.spec.SecretKeySpec;
 
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.hadoop.conf.Configuration;
 import org.junit.Rule;
 import org.junit.Test;
 import org.junit.rules.ExpectedException;
@@ -61,7 +63,6 @@ public class CryptoTest {
   
   @Test
   public void testNoCryptoStream() throws IOException {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_OFF_CONF);    
     
     CryptoModuleParameters params = CryptoModuleFactory.createParamsObjectFromAccumuloConfiguration(conf);
@@ -82,14 +83,10 @@ public class CryptoTest {
     params = cryptoModule.getEncryptingOutputStream(params);
     assertNotNull(params.getEncryptedOutputStream());
     assertEquals(out, params.getEncryptedOutputStream());
    

    restoreOldConfiguration(oldSiteConfigProperty, conf);
   }
   
   @Test
   public void testCryptoModuleParamsParsing() {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_ON_CONF);    
 
     CryptoModuleParameters params = CryptoModuleFactory.createParamsObjectFromAccumuloConfiguration(conf);
@@ -102,48 +99,32 @@ public class CryptoTest {
     assertEquals("SHA1PRNG", params.getRandomNumberGenerator());
     assertEquals("SUN", params.getRandomNumberGeneratorProvider());
     assertEquals("org.apache.accumulo.core.security.crypto.CachingHDFSSecretKeyEncryptionStrategy", params.getKeyEncryptionStrategyClass());
    
    restoreOldConfiguration(oldSiteConfigProperty, conf);    
   }
   
   @Test
   public void testCryptoModuleParamsValidation1() throws IOException {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_ON_CONF);    
   
    try {
      
      CryptoModuleParameters params = CryptoModuleFactory.createParamsObjectFromAccumuloConfiguration(conf);
      CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);
      
      assertTrue(cryptoModule instanceof DefaultCryptoModule);
      
      exception.expect(RuntimeException.class);
      cryptoModule.getEncryptingOutputStream(params);
      
      
    } finally {
      restoreOldConfiguration(oldSiteConfigProperty, conf);             
    }

    CryptoModuleParameters params = CryptoModuleFactory.createParamsObjectFromAccumuloConfiguration(conf);
    CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);

    assertTrue(cryptoModule instanceof DefaultCryptoModule);

    exception.expect(RuntimeException.class);
    cryptoModule.getEncryptingOutputStream(params);
   }
 
   @Test
   public void testCryptoModuleParamsValidation2() throws IOException {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_ON_CONF);    
    
    try {
      
      CryptoModuleParameters params = CryptoModuleFactory.createParamsObjectFromAccumuloConfiguration(conf);
      CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);
      
      assertTrue(cryptoModule instanceof DefaultCryptoModule);
      
      exception.expect(RuntimeException.class);
      cryptoModule.getDecryptingInputStream(params);
    } finally {
      restoreOldConfiguration(oldSiteConfigProperty, conf);             
    }
    CryptoModuleParameters params = CryptoModuleFactory.createParamsObjectFromAccumuloConfiguration(conf);
    CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);

    assertTrue(cryptoModule instanceof DefaultCryptoModule);

    exception.expect(RuntimeException.class);
    cryptoModule.getDecryptingInputStream(params);
   }
   
   private String getStringifiedBytes(String s) throws IOException {
@@ -170,14 +151,12 @@ public class CryptoTest {
 
   @Test
   public void testCryptoModuleBasicReadWrite() throws IOException {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_ON_KEK_OFF_CONF);    
   
     CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);
     CryptoModuleParameters params = CryptoModuleFactory.createParamsObjectFromAccumuloConfiguration(conf);
     
     assertTrue(cryptoModule instanceof DefaultCryptoModule);
    assertTrue(params.getKeyEncryptionStrategyClass() == null || params.getKeyEncryptionStrategyClass().equals(""));
     
     byte[] resultingBytes = setUpSampleEncryptedBytes(cryptoModule, params);
     
@@ -199,8 +178,6 @@ public class CryptoTest {
     
     assertEquals(MARKER_STRING, markerString);
     assertEquals(MARKER_INT, markerInt);
    
    restoreOldConfiguration(oldSiteConfigProperty, conf);
   }
 
   private byte[] setUpSampleEncryptedBytes(CryptoModule cryptoModule, CryptoModuleParameters params) throws IOException {
@@ -233,7 +210,6 @@ public class CryptoTest {
   
   @Test
   public void testKeyEncryptionAndCheckThatFileCannotBeReadWithoutKEK() throws IOException {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_ON_CONF);    
   
     CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);
@@ -259,19 +235,13 @@ public class CryptoTest {
     assertNotNull(params.getPlaintextInputStream());
     DataInputStream dataIn = new DataInputStream(params.getPlaintextInputStream());
     // We expect the following operation to fail and throw an exception
    try {
      exception.expect(IOException.class);
      @SuppressWarnings("unused")
      String markerString = dataIn.readUTF();
    }
    finally {
      restoreOldConfiguration(oldSiteConfigProperty, conf);      
    }
    exception.expect(IOException.class);
    @SuppressWarnings("unused")
    String markerString = dataIn.readUTF();
  }
 
   @Test
   public void testKeyEncryptionNormalPath() throws IOException {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_ON_CONF);    
 
     CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);
@@ -299,13 +269,10 @@ public class CryptoTest {
     
     assertEquals(MARKER_STRING, markerString);
     assertEquals(MARKER_INT, markerInt);

    restoreOldConfiguration(oldSiteConfigProperty, conf);
   }
   
   @Test
   public void testChangingCryptoParamsAndCanStillDecryptPreviouslyEncryptedFiles() throws IOException {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
     AccumuloConfiguration conf = setAndGetAccumuloConfig(CRYPTO_ON_CONF);    
 
     CryptoModule cryptoModule = CryptoModuleFactory.getCryptoModule(conf);
@@ -336,27 +303,16 @@ public class CryptoTest {
     
     assertEquals(MARKER_STRING, markerString);
     assertEquals(MARKER_INT, markerInt);

    restoreOldConfiguration(oldSiteConfigProperty, conf);   
   }
   
  private void restoreOldConfiguration(String oldSiteConfigProperty, AccumuloConfiguration conf) {
    if (oldSiteConfigProperty != null) {
      System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, oldSiteConfigProperty);
    } else {
      System.clearProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
  private AccumuloConfiguration setAndGetAccumuloConfig(String cryptoConfSetting) {
    ConfigurationCopy result = new ConfigurationCopy(AccumuloConfiguration.getDefaultConfiguration());
    Configuration conf = new Configuration(false);
    conf.addResource(cryptoConfSetting);
    for (Entry<String,String> e : conf) {
      result.set(e.getKey(), e.getValue());
     }
    ((SiteConfiguration)conf).clearAndNull();
  }



  private AccumuloConfiguration setAndGetAccumuloConfig(String cryptoConfSetting) {  
    @SuppressWarnings("deprecation")
    AccumuloConfiguration conf = AccumuloConfiguration.getSiteConfiguration();
    System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, cryptoConfSetting);
    ((SiteConfiguration)conf).clearAndNull();
    return conf;
    return result;
   }
   
   @Test
diff --git a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
index 40b5cf076..af810ad10 100644
-- a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
++ b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
@@ -179,7 +179,7 @@ public class ShellSetInstanceTest {
     if (!onlyInstance) {
       mockStatic(ZooUtil.class);
       randomUUID = UUID.randomUUID();
      expect(ZooUtil.getInstanceIDFromHdfs(anyObject(Path.class)))
      expect(ZooUtil.getInstanceIDFromHdfs(anyObject(Path.class), anyObject(AccumuloConfiguration.class)))
         .andReturn(randomUUID.toString());
       replay(ZooUtil.class);
       expect(clientConf.withInstance(randomUUID)).andReturn(clientConf);
diff --git a/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java b/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java
index eb928ba68..ca5783b8f 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java
++ b/server/base/src/main/java/org/apache/accumulo/server/ServerConstants.java
@@ -136,7 +136,7 @@ public class ServerConstants {
       String currentIid;
       Integer currentVersion;
       try {
        currentIid = ZooUtil.getInstanceIDFromHdfs(new Path(baseDir, INSTANCE_ID_DIR));
        currentIid = ZooUtil.getInstanceIDFromHdfs(new Path(baseDir, INSTANCE_ID_DIR), ServerConfiguration.getSiteConfiguration());
         Path vpath = new Path(baseDir, VERSION_DIR);
         currentVersion = Accumulo.getAccumuloPersistentVersion(vpath.getFileSystem(CachedConfiguration.getInstance()), vpath);
       } catch (Exception e) {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java b/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
index 54fb7e352..6993a0a46 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
++ b/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
@@ -122,7 +122,7 @@ public class HdfsZooInstance implements Instance {
 
   private static synchronized void _getInstanceID() {
     if (instanceId == null) {
      String instanceIdFromFile = ZooUtil.getInstanceIDFromHdfs(ServerConstants.getInstanceIdLocation());
      String instanceIdFromFile = ZooUtil.getInstanceIDFromHdfs(ServerConstants.getInstanceIdLocation(), ServerConfiguration.getSiteConfiguration());
       instanceId = instanceIdFromFile;
     }
   }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/conf/ZooConfiguration.java b/server/base/src/main/java/org/apache/accumulo/server/conf/ZooConfiguration.java
index e790d0d42..32f6126d4 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/conf/ZooConfiguration.java
++ b/server/base/src/main/java/org/apache/accumulo/server/conf/ZooConfiguration.java
@@ -57,7 +57,7 @@ public class ZooConfiguration extends AccumuloConfiguration {
     if (instance == null) {
       propCache = new ZooCache(parent.get(Property.INSTANCE_ZK_HOST), (int) parent.getTimeInMillis(Property.INSTANCE_ZK_TIMEOUT));
       instance = new ZooConfiguration(parent);
      String deprecatedInstanceIdFromHdfs = ZooUtil.getInstanceIDFromHdfs(ServerConstants.getInstanceIdLocation());
      String deprecatedInstanceIdFromHdfs = ZooUtil.getInstanceIDFromHdfs(ServerConstants.getInstanceIdLocation(), parent);
       instanceId = deprecatedInstanceIdFromHdfs;
     }
     return instance;
diff --git a/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java b/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
index a7e858ca8..8533484b4 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
++ b/server/base/src/main/java/org/apache/accumulo/server/init/Initialize.java
@@ -573,7 +573,7 @@ public class Initialize {
     Path iidPath = new Path(aBasePath, ServerConstants.INSTANCE_ID_DIR);
     Path versionPath = new Path(aBasePath, ServerConstants.VERSION_DIR);
 
    UUID uuid = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(iidPath));
    UUID uuid = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(iidPath, ServerConfiguration.getSiteConfiguration()));
 
     if (ServerConstants.DATA_VERSION != Accumulo.getAccumuloPersistentVersion(versionPath.getFileSystem(CachedConfiguration.getInstance()), versionPath)) {
       throw new IOException("Accumulo " + Constants.VERSION + " cannot initialize data version " + Accumulo.getAccumuloPersistentVersion(fs));
@@ -602,10 +602,11 @@ public class Initialize {
     opts.parseArgs(Initialize.class.getName(), args);
 
     try {
      SecurityUtil.serverLogin();
      AccumuloConfiguration acuConf = ServerConfiguration.getSiteConfiguration();
      SecurityUtil.serverLogin(acuConf);
       Configuration conf = CachedConfiguration.getInstance();
 
      VolumeManager fs = VolumeManagerImpl.get(ServerConfiguration.getSiteConfiguration());
      VolumeManager fs = VolumeManagerImpl.get(acuConf);
 
       if (opts.resetSecurity) {
         if (isInitialized(fs)) {
diff --git a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
index e6c8265fb..89925b46a 100644
-- a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
++ b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
@@ -134,7 +134,7 @@ public class SimpleGarbageCollector implements Iface {
   private Instance instance;
 
   public static void main(String[] args) throws UnknownHostException, IOException {
    SecurityUtil.serverLogin();
    SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
     Instance instance = HdfsZooInstance.getInstance();
     ServerConfiguration serverConf = new ServerConfiguration(instance);
     final VolumeManager fs = VolumeManagerImpl.get();
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index 31469f113..a06337786 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -1058,7 +1058,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
 
   public static void main(String[] args) throws Exception {
     try {
      SecurityUtil.serverLogin();
      SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
 
       VolumeManager fs = VolumeManagerImpl.get();
       ServerOpts opts = new ServerOpts();
diff --git a/server/master/src/main/java/org/apache/accumulo/master/state/SetGoalState.java b/server/master/src/main/java/org/apache/accumulo/master/state/SetGoalState.java
index e28dcad05..f981bae8f 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/state/SetGoalState.java
++ b/server/master/src/main/java/org/apache/accumulo/master/state/SetGoalState.java
@@ -23,6 +23,7 @@ import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
 import org.apache.accumulo.server.Accumulo;
 import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
@@ -37,7 +38,7 @@ public class SetGoalState {
       System.err.println("Usage: accumulo " + SetGoalState.class.getName() + " [NORMAL|SAFE_MODE|CLEAN_STOP]");
       System.exit(-1);
     }
    SecurityUtil.serverLogin();
    SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
 
     VolumeManager fs = VolumeManagerImpl.get();
     Accumulo.waitForZookeeperAndHdfs(fs);
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
index 90b65fef0..0e6b37f82 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/Monitor.java
@@ -390,7 +390,7 @@ public class Monitor {
   }
 
   public static void main(String[] args) throws Exception {
    SecurityUtil.serverLogin();
    SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
     
     VolumeManager fs = VolumeManagerImpl.get();
     ServerOpts opts = new ServerOpts();
diff --git a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
index 0f4cd3a15..30f1ae768 100644
-- a/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
++ b/server/tracer/src/main/java/org/apache/accumulo/tracer/TraceServer.java
@@ -283,7 +283,7 @@ public class TraceServer implements Watcher {
   }
   
   public static void main(String[] args) throws Exception {
    SecurityUtil.serverLogin();
    SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
     ServerOpts opts = new ServerOpts();
     opts.parseArgs("tracer", args);
     Instance instance = HdfsZooInstance.getInstance();
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index 4efcab1c3..1cce5655c 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -3153,7 +3153,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
 
   // main loop listens for client requests
   public void run() {
    SecurityUtil.serverLogin();
    SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
 
     try {
       clientAddress = startTabletClientService();
@@ -3646,7 +3646,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
 
   public static void main(String[] args) throws IOException {
     try {
      SecurityUtil.serverLogin();
      SecurityUtil.serverLogin(ServerConfiguration.getSiteConfiguration());
       VolumeManager fs = VolumeManagerImpl.get();
       ServerOpts opts = new ServerOpts();
       opts.parseArgs("tserver", args);
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/logger/LogReader.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/logger/LogReader.java
index b32ace9be..25b804308 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/logger/LogReader.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/logger/LogReader.java
@@ -28,9 +28,9 @@ import java.util.regex.Pattern;
 
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.conf.SiteConfiguration;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.accumulo.tserver.log.DfsLogger;
@@ -97,8 +97,7 @@ public class LogReader {
 
       if (fs.isFile(path)) {
         // read log entries from a simple hdfs file
        @SuppressWarnings("deprecation")
        DFSLoggerInputStreams streams = DfsLogger.readHeaderAndReturnStream(fs, path, SiteConfiguration.getSiteConfiguration());
        DFSLoggerInputStreams streams = DfsLogger.readHeaderAndReturnStream(fs, path, ServerConfiguration.getSiteConfiguration());
         DataInputStream input = streams.getDecryptingInputStream();
 
         try {
diff --git a/server/tserver/src/test/java/org/apache/accumulo/tserver/log/TestUpgradePathForWALogs.java b/server/tserver/src/test/java/org/apache/accumulo/tserver/log/TestUpgradePathForWALogs.java
index f1ceb3b7c..af149faf2 100644
-- a/server/tserver/src/test/java/org/apache/accumulo/tserver/log/TestUpgradePathForWALogs.java
++ b/server/tserver/src/test/java/org/apache/accumulo/tserver/log/TestUpgradePathForWALogs.java
@@ -23,7 +23,7 @@ import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.commons.io.FileUtils;
@@ -84,8 +84,7 @@ public class TestUpgradePathForWALogs {
       walogInHDFStream.close();
       walogInHDFStream = null;
 
      @SuppressWarnings("deprecation")
      LogSorter logSorter = new LogSorter(null, fs, DefaultConfiguration.getSiteConfiguration());
      LogSorter logSorter = new LogSorter(null, fs, ServerConfiguration.getSiteConfiguration());
       LogSorter.LogProcessor logProcessor = logSorter.new LogProcessor();
 
       logProcessor.sort(WALOG_FROM_15, new Path("file://" + root.getRoot().getAbsolutePath() + WALOG_FROM_15), "file://" + root.getRoot().getAbsolutePath()
@@ -119,8 +118,7 @@ public class TestUpgradePathForWALogs {
       walogInHDFStream.close();
       walogInHDFStream = null;
 
      @SuppressWarnings("deprecation")
      LogSorter logSorter = new LogSorter(null, fs, DefaultConfiguration.getSiteConfiguration());
      LogSorter logSorter = new LogSorter(null, fs, ServerConfiguration.getSiteConfiguration());
       LogSorter.LogProcessor logProcessor = logSorter.new LogProcessor();
 
       logProcessor.sort(walogToTest, new Path("file://" + root.getRoot().getAbsolutePath() + walogToTest), "file://" + root.getRoot().getAbsolutePath()
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/BulkImport.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/BulkImport.java
index d4d6838b1..4a4d5c802 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/BulkImport.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/BulkImport.java
@@ -28,6 +28,7 @@ import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
@@ -48,8 +49,9 @@ public class BulkImport extends Test {
     RFile.Writer writer;
     
     public RFileBatchWriter(Configuration conf, FileSystem fs, String file) throws IOException {
      AccumuloConfiguration aconf = AccumuloConfiguration.getDefaultConfiguration();
       CachableBlockFile.Writer cbw = new CachableBlockFile.Writer(fs.create(new Path(file), false, conf.getInt("io.file.buffer.size", 4096),
          (short) conf.getInt("dfs.replication", 3), conf.getLong("dfs.block.size", 1 << 26)), "gz", conf);
          (short) conf.getInt("dfs.replication", 3), conf.getLong("dfs.block.size", 1 << 26)), "gz", conf, aconf);
       writer = new RFile.Writer(cbw, 100000);
       writer.startDefaultLocalityGroup();
     }
- 
2.19.1.windows.1

