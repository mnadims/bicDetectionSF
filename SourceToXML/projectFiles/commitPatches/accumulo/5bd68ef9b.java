From 5bd68ef9b751848a441cb56ca82a7e2aafdf1461 Mon Sep 17 00:00:00 2001
From: Keith Turner <kturner@apache.org>
Date: Thu, 21 Nov 2013 15:03:03 -0500
Subject: [PATCH] ACCUMULO-1009 moved ClientConfiguration into public API and
 removed its usage of AccumuloConfiguration

--
 .../apache/accumulo/core/cli/ClientOpts.java  |  4 +-
 .../{conf => client}/ClientConfiguration.java | 60 ++-----------------
 .../core/client/ZooKeeperInstance.java        | 19 +++---
 .../client/admin/InstanceOperationsImpl.java  |  8 +--
 .../client/admin/TableOperationsImpl.java     |  6 +-
 .../client/impl/ConditionalWriterImpl.java    |  6 +-
 .../core/client/impl/MasterClient.java        |  2 +-
 .../core/client/impl/OfflineScanner.java      |  4 +-
 .../core/client/impl/ScannerIterator.java     |  2 +-
 .../core/client/impl/ServerClient.java        |  4 +-
 ...tory.java => ServerConfigurationUtil.java} | 35 ++++++++++-
 .../impl/TabletServerBatchReaderIterator.java |  2 +-
 .../client/impl/TabletServerBatchWriter.java  |  6 +-
 .../accumulo/core/client/impl/Writer.java     |  2 +-
 .../client/mapred/AbstractInputFormat.java    |  2 +-
 .../client/mapred/AccumuloInputFormat.java    |  2 +-
 .../mapred/AccumuloMultiTableInputFormat.java |  2 +-
 .../client/mapred/AccumuloOutputFormat.java   |  2 +-
 .../client/mapred/AccumuloRowInputFormat.java |  2 +-
 .../client/mapreduce/AbstractInputFormat.java |  2 +-
 .../client/mapreduce/AccumuloInputFormat.java |  2 +-
 .../AccumuloMultiTableInputFormat.java        |  2 +-
 .../mapreduce/AccumuloOutputFormat.java       |  2 +-
 .../mapreduce/AccumuloRowInputFormat.java     |  2 +-
 .../mapreduce/lib/util/ConfiguratorBase.java  |  2 +-
 .../metadata/MetadataLocationObtainer.java    |  8 +--
 .../accumulo/core/util/shell/Shell.java       | 11 ++--
 .../core/util/shell/ShellOptionsJC.java       |  4 +-
 .../lib/util/ConfiguratorBaseTest.java        |  4 +-
 .../core/conf/ClientConfigurationTest.java    |  3 +-
 .../core/util/shell/ShellSetInstanceTest.java | 17 +++---
 .../simple/filedata/FileDataQuery.java        |  2 +-
 .../simple/mapreduce/TokenFileWordCount.java  |  2 +-
 .../examples/simple/reservations/ARS.java     |  2 +-
 .../minicluster/MiniAccumuloCluster.java      |  2 +-
 .../minicluster/MiniAccumuloInstance.java     |  2 +-
 .../apache/accumulo/proxy/ProxyServer.java    |  2 +-
 .../accumulo/server/client/BulkImporter.java  | 10 ++--
 .../accumulo/utils/metanalysis/IndexMeta.java |  2 +-
 .../gc/GarbageCollectWriteAheadLogs.java      |  4 +-
 .../accumulo/gc/SimpleGarbageCollector.java   | 14 ++---
 .../org/apache/accumulo/master/Master.java    |  4 +-
 .../apache/accumulo/test/IMMLGBenchmark.java  |  2 +-
 .../metadata/MetadataBatchScanTest.java       |  2 +-
 .../test/performance/thrift/NullTserver.java  |  2 +-
 .../accumulo/test/randomwalk/State.java       |  2 +-
 .../test/randomwalk/multitable/CopyTool.java  |  2 +-
 .../sequential/MapRedVerifyTool.java          |  2 +-
 .../accumulo/test/scalability/ScaleTest.java  |  2 +-
 .../test/MultiTableBatchWriterTest.java       |  2 +-
 50 files changed, 138 insertions(+), 153 deletions(-)
 rename core/src/main/java/org/apache/accumulo/core/{conf => client}/ClientConfiguration.java (83%)
 rename core/src/main/java/org/apache/accumulo/core/client/impl/{ServerConfigurationFactory.java => ServerConfigurationUtil.java} (54%)

diff --git a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
index 1d26a00a9..c43b1218c 100644
-- a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
++ b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
@@ -24,9 +24,11 @@ import java.util.UUID;
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
@@ -35,8 +37,6 @@ import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.Properties;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/ClientConfiguration.java b/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
similarity index 83%
rename from core/src/main/java/org/apache/accumulo/core/conf/ClientConfiguration.java
rename to core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
index 5bb95aeb3..aa1c6fc16 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/ClientConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ClientConfiguration.java
@@ -14,31 +14,29 @@
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.apache.accumulo.core.conf;
package org.apache.accumulo.core.client;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.StringReader;
 import java.io.StringWriter;
 import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
 import java.util.UUID;
 
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.PropertyType;
 import org.apache.accumulo.core.util.ArgumentChecker;
 import org.apache.commons.configuration.CompositeConfiguration;
 import org.apache.commons.configuration.Configuration;
 import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
 import org.apache.commons.configuration.PropertiesConfiguration;
 
 /**
  * Contains a list of property keys recognized by the Accumulo client and convenience methods for setting them.
 * 
 * @since 1.6.0
  */
 public class ClientConfiguration extends CompositeConfiguration {
   public static final String USER_ACCUMULO_DIR_NAME = ".accumulo";
@@ -259,52 +257,4 @@ public class ClientConfiguration extends CompositeConfiguration {
       setProperty(ClientProperty.RPC_SSL_KEYSTORE_TYPE, type);
     return this;
   }

  public AccumuloConfiguration getAccumuloConfiguration() {
    final AccumuloConfiguration defaultConf = AccumuloConfiguration.getDefaultConfiguration();
    return new AccumuloConfiguration() {

      @Override
      public Iterator<Entry<String,String>> iterator() {
        TreeMap<String,String> entries = new TreeMap<String,String>();

        for (Entry<String,String> parentEntry : defaultConf)
          entries.put(parentEntry.getKey(), parentEntry.getValue());

        @SuppressWarnings("unchecked")
        Iterator<String> keyIter = getKeys();
        while (keyIter.hasNext()) {
          String key = keyIter.next();
          entries.put(key, getString(key));
        }

        return entries.entrySet().iterator();
      }

      @Override
      public String get(Property property) {
        if (containsKey(property.getKey()))
          return getString(property.getKey());
        else
          return defaultConf.get(property);
      }

      @Override
      public void getProperties(Map<String,String> props, PropertyFilter filter) {
        for (Entry<String,String> entry : this)
          if (filter.accept(entry.getKey()))
            props.put(entry.getKey(), entry.getValue());
      }
    };
  }

  public static ClientConfiguration fromAccumuloConfiguration(AccumuloConfiguration accumuloConf) {
    Map<String,String> props = new HashMap<String,String>();
    for (ClientProperty prop : ClientProperty.values()) {
      if (prop.accumuloProperty == null)
        continue;
      props.put(prop.getKey(), accumuloConf.get(prop.accumuloProperty));
    }
    return new ClientConfiguration(new MapConfiguration(props));
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java b/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
index fb4ab79a6..caf68647a 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
@@ -23,12 +23,13 @@ import java.util.UUID;
 import java.util.concurrent.atomic.AtomicInteger;
 
 import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.impl.ConnectorImpl;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.metadata.RootTable;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.util.ArgumentChecker;
@@ -81,7 +82,7 @@ public class ZooKeeperInstance implements Instance {
    *          The name of specific accumulo instance. This is set at initialization time.
    * @param zooKeepers
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(Configuration)} instead.
    */
   @Deprecated
   public ZooKeeperInstance(String instanceName, String zooKeepers) {
@@ -96,7 +97,7 @@ public class ZooKeeperInstance implements Instance {
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
    * @param sessionTimeout
    *          zoo keeper session time out in milliseconds.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(Configuration)} instead.
    */
   @Deprecated
   public ZooKeeperInstance(String instanceName, String zooKeepers, int sessionTimeout) {
@@ -109,7 +110,7 @@ public class ZooKeeperInstance implements Instance {
    *          The UUID that identifies the accumulo instance you want to connect to.
    * @param zooKeepers
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(Configuration)} instead.
    */
   @Deprecated
   public ZooKeeperInstance(UUID instanceId, String zooKeepers) {
@@ -124,7 +125,7 @@ public class ZooKeeperInstance implements Instance {
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
    * @param sessionTimeout
    *          zoo keeper session time out in milliseconds.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(Configuration)} instead.
    */
   @Deprecated
   public ZooKeeperInstance(UUID instanceId, String zooKeepers, int sessionTimeout) {
@@ -264,11 +265,9 @@ public class ZooKeeperInstance implements Instance {
   }
 
   @Override
  @Deprecated
   public AccumuloConfiguration getConfiguration() {
    if (accumuloConf == null) {
      accumuloConf = clientConf.getAccumuloConfiguration();
    }
    return accumuloConf;
    return ServerConfigurationUtil.convertClientConfig(accumuloConf == null ? DefaultConfiguration.getInstance() : accumuloConf, clientConf);
   }
 
   @Override
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/InstanceOperationsImpl.java b/core/src/main/java/org/apache/accumulo/core/client/admin/InstanceOperationsImpl.java
index 333201ee2..85bc1a3e9 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/InstanceOperationsImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/InstanceOperationsImpl.java
@@ -30,7 +30,7 @@ import org.apache.accumulo.core.client.impl.ClientExec;
 import org.apache.accumulo.core.client.impl.ClientExecReturn;
 import org.apache.accumulo.core.client.impl.MasterClient;
 import org.apache.accumulo.core.client.impl.ServerClient;
import org.apache.accumulo.core.client.impl.ServerConfigurationFactory;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.impl.thrift.ClientService;
 import org.apache.accumulo.core.client.impl.thrift.ConfigurationType;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
@@ -132,7 +132,7 @@ public class InstanceOperationsImpl implements InstanceOperations {
   public List<ActiveScan> getActiveScans(String tserver) throws AccumuloException, AccumuloSecurityException {
     Client client = null;
     try {
      client = ThriftUtil.getTServerClient(tserver, ServerConfigurationFactory.getConfiguration(instance));
      client = ThriftUtil.getTServerClient(tserver, ServerConfigurationUtil.getConfiguration(instance));
       
       List<ActiveScan> as = new ArrayList<ActiveScan>();
       for (org.apache.accumulo.core.tabletserver.thrift.ActiveScan activeScan : client.getActiveScans(Tracer.traceInfo(), credentials.toThrift(instance))) {
@@ -169,7 +169,7 @@ public class InstanceOperationsImpl implements InstanceOperations {
   public List<ActiveCompaction> getActiveCompactions(String tserver) throws AccumuloException, AccumuloSecurityException {
     Client client = null;
     try {
      client = ThriftUtil.getTServerClient(tserver, ServerConfigurationFactory.getConfiguration(instance));
      client = ThriftUtil.getTServerClient(tserver, ServerConfigurationUtil.getConfiguration(instance));
       
       List<ActiveCompaction> as = new ArrayList<ActiveCompaction>();
       for (org.apache.accumulo.core.tabletserver.thrift.ActiveCompaction activeCompaction : client.getActiveCompactions(Tracer.traceInfo(),
@@ -193,7 +193,7 @@ public class InstanceOperationsImpl implements InstanceOperations {
   public void ping(String tserver) throws AccumuloException {
     TTransport transport = null;
     try {
      transport = ThriftUtil.createTransport(AddressUtil.parseAddress(tserver), ServerConfigurationFactory.getConfiguration(instance));
      transport = ThriftUtil.createTransport(AddressUtil.parseAddress(tserver), ServerConfigurationUtil.getConfiguration(instance));
       TabletClientService.Client client = ThriftUtil.createClient(new TabletClientService.Client.Factory(), transport);
       client.getTabletServerStatus(Tracer.traceInfo(), credentials.toThrift(instance));
     } catch (TTransportException e) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperationsImpl.java b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperationsImpl.java
index a85772d66..a779ae41a 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperationsImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperationsImpl.java
@@ -61,7 +61,7 @@ import org.apache.accumulo.core.client.impl.ClientExec;
 import org.apache.accumulo.core.client.impl.ClientExecReturn;
 import org.apache.accumulo.core.client.impl.MasterClient;
 import org.apache.accumulo.core.client.impl.ServerClient;
import org.apache.accumulo.core.client.impl.ServerConfigurationFactory;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
@@ -470,7 +470,7 @@ public class TableOperationsImpl extends TableOperationsHelper {
         }
         
         try {
          TabletClientService.Client client = ThriftUtil.getTServerClient(tl.tablet_location, ServerConfigurationFactory.getConfiguration(instance));
          TabletClientService.Client client = ThriftUtil.getTServerClient(tl.tablet_location, ServerConfigurationUtil.getConfiguration(instance));
           try {
             OpTimer opTimer = null;
             if (log.isTraceEnabled())
@@ -1113,7 +1113,7 @@ public class TableOperationsImpl extends TableOperationsHelper {
       ret = new Path(dir);
       fs = ret.getFileSystem(CachedConfiguration.getInstance());
     } else {
      fs = FileUtil.getFileSystem(CachedConfiguration.getInstance(), ServerConfigurationFactory.getConfiguration(instance));
      fs = FileUtil.getFileSystem(CachedConfiguration.getInstance(), ServerConfigurationUtil.getConfiguration(instance));
       ret = fs.makeQualified(new Path(dir));
     }
 
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java
index 1d35af4a5..cd89adbf5 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java
@@ -536,10 +536,10 @@ class ConditionalWriterImpl implements ConditionalWriter {
   
   private TabletClientService.Iface getClient(String location) throws TTransportException {
     TabletClientService.Iface client;
    if (timeout < ServerConfigurationFactory.getConfiguration(instance).getTimeInMillis(Property.GENERAL_RPC_TIMEOUT))
      client = ThriftUtil.getTServerClient(location, ServerConfigurationFactory.getConfiguration(instance), timeout);
    if (timeout < ServerConfigurationUtil.getConfiguration(instance).getTimeInMillis(Property.GENERAL_RPC_TIMEOUT))
      client = ThriftUtil.getTServerClient(location, ServerConfigurationUtil.getConfiguration(instance), timeout);
     else
      client = ThriftUtil.getTServerClient(location, ServerConfigurationFactory.getConfiguration(instance));
      client = ThriftUtil.getTServerClient(location, ServerConfigurationUtil.getConfiguration(instance));
     return client;
   }
   
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java b/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java
index 4cf6e06e5..6bef3a7e3 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java
@@ -61,7 +61,7 @@ public class MasterClient {
     
     try {
       // Master requests can take a long time: don't ever time out
      MasterClientService.Client client = ThriftUtil.getClientNoTimeout(new MasterClientService.Client.Factory(), master, ServerConfigurationFactory.getConfiguration(instance));
      MasterClientService.Client client = ThriftUtil.getClientNoTimeout(new MasterClientService.Client.Factory(), master, ServerConfigurationUtil.getConfiguration(instance));
       return client;
     } catch (TTransportException tte) {
       if (tte.getCause().getClass().equals(UnknownHostException.class)) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineScanner.java b/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineScanner.java
index 5e92d8bdf..6e0871096 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineScanner.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineScanner.java
@@ -227,7 +227,7 @@ class OfflineIterator implements Iterator<Entry<Key,Value>> {
     if (currentExtent != null && !extent.isPreviousExtent(currentExtent))
       throw new AccumuloException(" " + currentExtent + " is not previous extent " + extent);
     
    String tablesDir = ServerConfigurationFactory.getConfiguration(instance).get(Property.INSTANCE_DFS_DIR) + "/tables";
    String tablesDir = ServerConfigurationUtil.getConfiguration(instance).get(Property.INSTANCE_DFS_DIR) + "/tables";
 
     List<String> absFiles = new ArrayList<String>();
     for (String relPath : relFiles) {
@@ -296,7 +296,7 @@ class OfflineIterator implements Iterator<Entry<Key,Value>> {
     
     Configuration conf = CachedConfiguration.getInstance();
     
    FileSystem defaultFs = FileUtil.getFileSystem(conf, ServerConfigurationFactory.getConfiguration(instance));
    FileSystem defaultFs = FileUtil.getFileSystem(conf, ServerConfigurationUtil.getConfiguration(instance));
     
     for (SortedKeyValueIterator<Key,Value> reader : readers) {
       ((FileSKVIterator) reader).close();
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java
index 677a751d7..5ea36629d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java
@@ -81,7 +81,7 @@ public class ScannerIterator implements Iterator<Entry<Key,Value>> {
       
       try {
         while (true) {
          List<KeyValue> currentBatch = ThriftScanner.scan(instance, credentials, scanState, timeOut, ServerConfigurationFactory.getConfiguration(instance));
          List<KeyValue> currentBatch = ThriftScanner.scan(instance, credentials, scanState, timeOut, ServerConfigurationUtil.getConfiguration(instance));
           
           if (currentBatch == null) {
             synchQ.add(EMPTY_LIST);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java
index 89956db00..4eb845ded 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java
@@ -125,7 +125,7 @@ public class ServerClient {
   }
   
   public static Pair<String,ClientService.Client> getConnection(Instance instance, boolean preferCachedConnections) throws TTransportException {
    AccumuloConfiguration conf = ServerConfigurationFactory.getConfiguration(instance);
    AccumuloConfiguration conf = ServerConfigurationUtil.getConfiguration(instance);
     return getConnection(instance, preferCachedConnections, conf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT));
   }
   
@@ -142,7 +142,7 @@ public class ServerClient {
       if (data != null && !new String(data).equals("master"))
         servers.add(new ThriftTransportKey(
           new ServerServices(new String(data)).getAddressString(Service.TSERV_CLIENT),
          rpcTimeout, SslConnectionParams.forClient(ServerConfigurationFactory.getConfiguration(instance))));
          rpcTimeout, SslConnectionParams.forClient(ServerConfigurationUtil.getConfiguration(instance))));
     }
     
     boolean opened = false;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ServerConfigurationFactory.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ServerConfigurationUtil.java
similarity index 54%
rename from core/src/main/java/org/apache/accumulo/core/client/impl/ServerConfigurationFactory.java
rename to core/src/main/java/org/apache/accumulo/core/client/impl/ServerConfigurationUtil.java
index 2c3427afa..8021f764a 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ServerConfigurationFactory.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ServerConfigurationUtil.java
@@ -16,15 +16,48 @@
  */
 package org.apache.accumulo.core.client.impl;
 
import java.util.Iterator;
import java.util.Map;

 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.commons.configuration.Configuration;
 
 /**
  * All client side code that needs a server side configuration object should obtain it from here.
  */
public class ServerConfigurationFactory {
public class ServerConfigurationUtil {
   @SuppressWarnings("deprecation")
   public static AccumuloConfiguration getConfiguration(Instance instance) {
     return instance.getConfiguration();
   }
  
  public static AccumuloConfiguration convertClientConfig(final AccumuloConfiguration base, final Configuration config) {

    return new AccumuloConfiguration() {
      @Override
      public String get(Property property) {
        if (config.containsKey(property.getKey()))
          return config.getString(property.getKey());
        else
          return base.get(property);
      }

      @Override
      public void getProperties(Map<String,String> props, PropertyFilter filter) {

        base.getProperties(props, filter);

        @SuppressWarnings("unchecked")
        Iterator<String> keyIter = config.getKeys();
        while (keyIter.hasNext()) {
          String key = keyIter.next();
          if (filter.accept(key))
            props.put(key, config.getString(key));
        }
      }
    };

  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
index 9961f8f24..d82056bec 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
@@ -362,7 +362,7 @@ public class TabletServerBatchReaderIterator implements Iterator<Entry<Key,Value
           timeoutTrackers.put(tsLocation, timeoutTracker);
         }
         doLookup(instance, credentials, tsLocation, tabletsRanges, tsFailures, unscanned, receiver, columns, options, authorizations,
            ServerConfigurationFactory.getConfiguration(instance), timeoutTracker);
            ServerConfigurationUtil.getConfiguration(instance), timeoutTracker);
         if (tsFailures.size() > 0) {
           locator.invalidateCache(tsFailures.keySet());
           synchronized (failures) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
index b79ae39e0..8a51657c3 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
@@ -855,10 +855,10 @@ public class TabletServerBatchWriter {
       try {
         TabletClientService.Iface client;
         
        if (timeoutTracker.getTimeOut() < ServerConfigurationFactory.getConfiguration(instance).getTimeInMillis(Property.GENERAL_RPC_TIMEOUT))
          client = ThriftUtil.getTServerClient(location, ServerConfigurationFactory.getConfiguration(instance), timeoutTracker.getTimeOut());
        if (timeoutTracker.getTimeOut() < ServerConfigurationUtil.getConfiguration(instance).getTimeInMillis(Property.GENERAL_RPC_TIMEOUT))
          client = ThriftUtil.getTServerClient(location, ServerConfigurationUtil.getConfiguration(instance), timeoutTracker.getTimeOut());
         else
          client = ThriftUtil.getTServerClient(location, ServerConfigurationFactory.getConfiguration(instance));
          client = ThriftUtil.getTServerClient(location, ServerConfigurationUtil.getConfiguration(instance));
         
         try {
           MutationSet allFailures = new MutationSet();
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/Writer.java b/core/src/main/java/org/apache/accumulo/core/client/impl/Writer.java
index e253024f5..72a050a24 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/Writer.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/Writer.java
@@ -93,7 +93,7 @@ public class Writer {
       }
       
       try {
        updateServer(instance, m, tabLoc.tablet_extent, tabLoc.tablet_location, credentials, ServerConfigurationFactory.getConfiguration(instance));
        updateServer(instance, m, tabLoc.tablet_extent, tabLoc.tablet_location, credentials, ServerConfigurationUtil.getConfiguration(instance));
         return;
       } catch (NotServingTabletException e) {
         log.trace("Not serving tablet, server = " + tabLoc.tablet_location);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
index 53ac4a14c..c0ef0b5a0 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
@@ -27,6 +27,7 @@ import java.util.Map;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientSideIteratorScanner;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
@@ -43,7 +44,6 @@ import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
 import org.apache.accumulo.core.client.mapreduce.lib.util.InputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Range;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java
index ffd74a50b..917b71da0 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java
@@ -19,8 +19,8 @@ package org.apache.accumulo.core.client.mapred;
 import java.io.IOException;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java
index f6eb294bd..2ef993161 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java
@@ -19,9 +19,9 @@ package org.apache.accumulo.core.client.mapred;
 import java.io.IOException;
 import java.util.Map;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
 import org.apache.accumulo.core.client.mapreduce.lib.util.InputConfigurator;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.util.format.DefaultFormatter;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
index eae6780a3..02512a480 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
@@ -26,6 +26,7 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.MultiTableBatchWriter;
@@ -38,7 +39,6 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java
index 35ce7c772..673c5b879 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java
@@ -19,9 +19,9 @@ package org.apache.accumulo.core.client.mapred;
 import java.io.IOException;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
index 9d8024eda..5c2777d08 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
@@ -31,6 +31,7 @@ import java.util.Map;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientSideIteratorScanner;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
@@ -46,7 +47,6 @@ import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.lib.util.InputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
index 0539c9345..9a339be34 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
@@ -19,8 +19,8 @@ package org.apache.accumulo.core.client.mapreduce;
 import java.io.IOException;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloMultiTableInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloMultiTableInputFormat.java
index e59abae43..357bf38a3 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloMultiTableInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloMultiTableInputFormat.java
@@ -22,11 +22,11 @@ import java.io.IOException;
 import java.util.List;
 import java.util.Map;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.mapreduce.lib.util.InputConfigurator;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
index afbedcaa3..0c924b18f 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
@@ -26,6 +26,7 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.MultiTableBatchWriter;
@@ -38,7 +39,6 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java
index 4734eda81..37caf15f3 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java
@@ -19,9 +19,9 @@ package org.apache.accumulo.core.client.mapreduce;
 import java.io.IOException;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java
index 91891508a..0fbba987d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java
@@ -22,12 +22,12 @@ import java.net.URISyntaxException;
 
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.util.ArgumentChecker;
 import org.apache.commons.codec.binary.Base64;
diff --git a/core/src/main/java/org/apache/accumulo/core/metadata/MetadataLocationObtainer.java b/core/src/main/java/org/apache/accumulo/core/metadata/MetadataLocationObtainer.java
index cb86b7743..25007f568 100644
-- a/core/src/main/java/org/apache/accumulo/core/metadata/MetadataLocationObtainer.java
++ b/core/src/main/java/org/apache/accumulo/core/metadata/MetadataLocationObtainer.java
@@ -34,7 +34,7 @@ import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.impl.AccumuloServerException;
 import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.impl.ServerConfigurationFactory;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocations;
@@ -98,7 +98,7 @@ public class MetadataLocationObtainer implements TabletLocationObtainer {
       Map<String,Map<String,String>> serverSideIteratorOptions = Collections.emptyMap();
       
       boolean more = ThriftScanner.getBatchFromServer(instance, credentials, range, src.tablet_extent, src.tablet_location, encodedResults, locCols,
          serverSideIteratorList, serverSideIteratorOptions, Constants.SCAN_BATCH_SIZE, Authorizations.EMPTY, false, ServerConfigurationFactory.getConfiguration(instance));
          serverSideIteratorList, serverSideIteratorOptions, Constants.SCAN_BATCH_SIZE, Authorizations.EMPTY, false, ServerConfigurationUtil.getConfiguration(instance));
       
       decodeRows(encodedResults, results);
       
@@ -106,7 +106,7 @@ public class MetadataLocationObtainer implements TabletLocationObtainer {
         range = new Range(results.lastKey().followingKey(PartialKey.ROW_COLFAM_COLQUAL_COLVIS_TIME), true, new Key(stopRow).followingKey(PartialKey.ROW), false);
         encodedResults.clear();
         more = ThriftScanner.getBatchFromServer(instance, credentials, range, src.tablet_extent, src.tablet_location, encodedResults, locCols,
            serverSideIteratorList, serverSideIteratorOptions, Constants.SCAN_BATCH_SIZE, Authorizations.EMPTY, false, ServerConfigurationFactory.getConfiguration(instance));
            serverSideIteratorList, serverSideIteratorOptions, Constants.SCAN_BATCH_SIZE, Authorizations.EMPTY, false, ServerConfigurationUtil.getConfiguration(instance));
         
         decodeRows(encodedResults, results);
       }
@@ -179,7 +179,7 @@ public class MetadataLocationObtainer implements TabletLocationObtainer {
     Map<KeyExtent,List<Range>> failures = new HashMap<KeyExtent,List<Range>>();
     try {
       TabletServerBatchReaderIterator.doLookup(instance, credentials, tserver, tabletsRanges, failures, unscanned, rr, columns, opts, Authorizations.EMPTY,
          ServerConfigurationFactory.getConfiguration(instance));
          ServerConfigurationUtil.getConfiguration(instance));
       if (failures.size() > 0) {
         // invalidate extents in parents cache
         if (log.isTraceEnabled())
diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
index b51840009..ddadae997 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
@@ -41,19 +41,21 @@ import jline.console.history.FileHistory;
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.conf.SiteConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.data.thrift.TConstraintViolationSummary;
@@ -419,7 +421,8 @@ public class Shell extends ShellOptions {
       instanceName = clientConfig.get(ClientProperty.INSTANCE_NAME);
     }
     if (instanceName == null || keepers == null) {
      AccumuloConfiguration conf = SiteConfiguration.getInstance(clientConfig.getAccumuloConfiguration());
      AccumuloConfiguration conf = SiteConfiguration.getInstance(ServerConfigurationUtil.convertClientConfig(DefaultConfiguration.getInstance(),
          clientConfig));
       if (instanceName == null) {
         Path instanceDir = new Path(conf.get(Property.INSTANCE_DFS_DIR), "instance_id");
         instanceId = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(instanceDir));
diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java b/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java
index 2f30a8737..547da4802 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java
@@ -24,9 +24,9 @@ import java.util.Map;
 import java.util.Scanner;
 import java.util.TreeMap;
 
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.commons.configuration.ConfigurationException;
 import org.apache.log4j.Logger;
 
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java
index 50fc0a9d9..3b5143f5c 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java
@@ -21,14 +21,14 @@ import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertTrue;
 
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.commons.codec.binary.Base64;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.log4j.Level;
diff --git a/core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java b/core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java
index 55cf9d3a1..40be70fc7 100644
-- a/core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java
++ b/core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java
@@ -20,7 +20,8 @@ import static org.junit.Assert.assertEquals;
 
 import java.util.Arrays;
 
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.commons.configuration.Configuration;
 import org.apache.commons.configuration.PropertiesConfiguration;
 import org.junit.Test;
diff --git a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
index 5ce132043..a2769bdc2 100644
-- a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
++ b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
@@ -35,11 +35,11 @@ import java.util.UUID;
 
 import jline.console.ConsoleReader;
 
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.conf.ConfigSanityCheck;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
@@ -154,22 +154,21 @@ public class ShellSetInstanceTest {
       expect(clientConf.get(ClientProperty.INSTANCE_NAME)).andReturn(null);
     }
 
    AccumuloConfiguration conf = createMock(AccumuloConfiguration.class);
    expect(clientConf.getAccumuloConfiguration()).andReturn(conf);

     mockStatic(ConfigSanityCheck.class);
     ConfigSanityCheck.validate(EasyMock.<AccumuloConfiguration>anyObject());
    expectLastCall();
    expectLastCall().atLeastOnce();
     replay(ConfigSanityCheck.class);
 
     if (!onlyHosts) {
      expect(conf.get(Property.INSTANCE_ZK_HOST)).andReturn("host1,host2").atLeastOnce();
      expect(clientConf.containsKey(Property.INSTANCE_ZK_HOST.getKey())).andReturn(true).atLeastOnce();
      expect(clientConf.getString(Property.INSTANCE_ZK_HOST.getKey())).andReturn("host1,host2").atLeastOnce();
       expect(clientConf.withZkHosts("host1,host2")).andReturn(clientConf);
     }
     if (!onlyInstance) {
      expect(conf.get(Property.INSTANCE_DFS_DIR)).andReturn("/dfs").atLeastOnce();
      expect(clientConf.containsKey(Property.INSTANCE_DFS_DIR.getKey())).andReturn(true).atLeastOnce();
      expect(clientConf.getString(Property.INSTANCE_DFS_DIR.getKey())).andReturn("/dfs").atLeastOnce();
     }
    replay(conf);

     UUID randomUUID = null;
     if (!onlyInstance) {
       mockStatic(ZooUtil.class);
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java
index a66438eeb..4b12d7bc3 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java
@@ -23,12 +23,12 @@ import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java
index 1114a7e9f..fc4b27f9e 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java
@@ -18,8 +18,8 @@ package org.apache.accumulo.examples.simple.mapreduce;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.util.CachedConfiguration;
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/reservations/ARS.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/reservations/ARS.java
index 12365b692..509a67477 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/reservations/ARS.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/reservations/ARS.java
@@ -24,13 +24,13 @@ import jline.console.ConsoleReader;
 
 import org.apache.accumulo.core.client.ConditionalWriter;
 import org.apache.accumulo.core.client.ConditionalWriter.Status;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ConditionalWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.IsolatedScanner;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Condition;
 import org.apache.accumulo.core.data.ConditionalMutation;
 import org.apache.accumulo.core.data.Key;
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
index 0a5074734..8b195ff24 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
@@ -44,11 +44,11 @@ import java.util.Set;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.master.thrift.MasterGoalState;
 import org.apache.accumulo.core.util.Daemon;
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
index 1e1c46422..43cae2dd6 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
@@ -20,9 +20,9 @@ import java.io.File;
 import java.io.FileNotFoundException;
 import java.net.MalformedURLException;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.commons.configuration.ConfigurationException;
 import org.apache.commons.configuration.PropertiesConfiguration;
diff --git a/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java b/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
index 9324da96b..799c763f7 100644
-- a/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
++ b/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
@@ -42,6 +42,7 @@ import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.ConditionalWriter;
 import org.apache.accumulo.core.client.ConditionalWriter.Result;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ConditionalWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
@@ -61,7 +62,6 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.ConditionalMutation;
 import org.apache.accumulo.core.data.Key;
diff --git a/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java b/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java
index ff822efb3..07aa1f8ad 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java
++ b/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java
@@ -36,7 +36,7 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.impl.ServerClient;
import org.apache.accumulo.core.client.impl.ServerConfigurationFactory;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
 import org.apache.accumulo.core.client.impl.Translator;
@@ -145,7 +145,7 @@ public class BulkImporter {
           public void run() {
             List<TabletLocation> tabletsToAssignMapFileTo = Collections.emptyList();
             try {
              tabletsToAssignMapFileTo = findOverlappingTablets(ServerConfigurationFactory.getConfiguration(instance), fs, locator, mapFile, credentials);
              tabletsToAssignMapFileTo = findOverlappingTablets(ServerConfigurationUtil.getConfiguration(instance), fs, locator, mapFile, credentials);
             } catch (Exception ex) {
               log.warn("Unable to find tablets that overlap file " + mapFile.toString());
             }
@@ -208,7 +208,7 @@ public class BulkImporter {
             
             try {
               timer.start(Timers.QUERY_METADATA);
              tabletsToAssignMapFileTo.addAll(findOverlappingTablets(ServerConfigurationFactory.getConfiguration(instance), fs, locator, entry.getKey(), ke, credentials));
              tabletsToAssignMapFileTo.addAll(findOverlappingTablets(ServerConfigurationUtil.getConfiguration(instance), fs, locator, entry.getKey(), ke, credentials));
               timer.stop(Timers.QUERY_METADATA);
               keListIter.remove();
             } catch (Exception ex) {
@@ -585,8 +585,8 @@ public class BulkImporter {
   private List<KeyExtent> assignMapFiles(Credentials credentials, String location, Map<KeyExtent,List<PathSize>> assignmentsPerTablet)
       throws AccumuloException, AccumuloSecurityException {
     try {
      long timeInMillis = ServerConfigurationFactory.getConfiguration(instance).getTimeInMillis(Property.TSERV_BULK_TIMEOUT);
      TabletClientService.Iface client = ThriftUtil.getTServerClient(location, ServerConfigurationFactory.getConfiguration(instance), timeInMillis);
      long timeInMillis = ServerConfigurationUtil.getConfiguration(instance).getTimeInMillis(Property.TSERV_BULK_TIMEOUT);
      TabletClientService.Iface client = ThriftUtil.getTServerClient(location, ServerConfigurationUtil.getConfiguration(instance), timeInMillis);
       try {
         HashMap<KeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>> files = new HashMap<KeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>>();
         for (Entry<KeyExtent,List<PathSize>> entry : assignmentsPerTablet.entrySet()) {
diff --git a/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java b/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java
index 5b85f188d..b296f6d2f 100644
-- a/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java
++ b/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java
@@ -23,10 +23,10 @@ import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
diff --git a/server/gc/src/main/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogs.java b/server/gc/src/main/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogs.java
index 6534bdf02..6afd42d3e 100644
-- a/server/gc/src/main/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogs.java
++ b/server/gc/src/main/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogs.java
@@ -30,7 +30,7 @@ import java.util.UUID;
 
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ServerConfigurationFactory;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.gc.thrift.GCStatus;
 import org.apache.accumulo.core.gc.thrift.GcCycleStats;
@@ -134,7 +134,7 @@ public class GarbageCollectWriteAheadLogs {
   }
   
   private int removeFiles(Map<String,Path> nameToFileMap, Map<String,ArrayList<Path>> serverToFileMap, Map<String, Path> sortedWALogs, final GCStatus status) {
    AccumuloConfiguration conf = ServerConfigurationFactory.getConfiguration(instance);
    AccumuloConfiguration conf = ServerConfigurationUtil.getConfiguration(instance);
     for (Entry<String,ArrayList<Path>> entry : serverToFileMap.entrySet()) {
       if (entry.getKey().isEmpty()) {
         // old-style log entry, just remove it
diff --git a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
index 95a7262e2..1f1b28dda 100644
-- a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
++ b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
@@ -41,7 +41,7 @@ import org.apache.accumulo.core.client.IsolatedScanner;
 import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ServerConfigurationFactory;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
@@ -154,9 +154,9 @@ public class SimpleGarbageCollector implements Iface {
     this.credentials = credentials;
     this.instance = instance;
     
    gcStartDelay = ServerConfigurationFactory.getConfiguration(instance).getTimeInMillis(Property.GC_CYCLE_START);
    long gcDelay = ServerConfigurationFactory.getConfiguration(instance).getTimeInMillis(Property.GC_CYCLE_DELAY);
    numDeleteThreads = ServerConfigurationFactory.getConfiguration(instance).getCount(Property.GC_DELETE_THREADS);
    gcStartDelay = ServerConfigurationUtil.getConfiguration(instance).getTimeInMillis(Property.GC_CYCLE_START);
    long gcDelay = ServerConfigurationUtil.getConfiguration(instance).getTimeInMillis(Property.GC_CYCLE_DELAY);
    numDeleteThreads = ServerConfigurationUtil.getConfiguration(instance).getCount(Property.GC_DELETE_THREADS);
     log.info("start delay: " + gcStartDelay + " milliseconds");
     log.info("time delay: " + gcDelay + " milliseconds");
     log.info("safemode: " + opts.safeMode);
@@ -482,7 +482,7 @@ public class SimpleGarbageCollector implements Iface {
       
       Trace.offNoFlush();
       try {
        long gcDelay = ServerConfigurationFactory.getConfiguration(instance).getTimeInMillis(Property.GC_CYCLE_DELAY);
        long gcDelay = ServerConfigurationUtil.getConfiguration(instance).getTimeInMillis(Property.GC_CYCLE_DELAY);
         log.debug("Sleeping for " + gcDelay + " milliseconds");
         Thread.sleep(gcDelay);
       } catch (InterruptedException e) {
@@ -535,8 +535,8 @@ public class SimpleGarbageCollector implements Iface {
   
   private HostAndPort startStatsService() throws UnknownHostException {
     Processor<Iface> processor = new Processor<Iface>(TraceWrap.service(this));
    int port = ServerConfigurationFactory.getConfiguration(instance).getPort(Property.GC_PORT);
    long maxMessageSize = ServerConfigurationFactory.getConfiguration(instance).getMemoryInBytes(Property.GENERAL_MAX_MESSAGE_SIZE);
    int port = ServerConfigurationUtil.getConfiguration(instance).getPort(Property.GC_PORT);
    long maxMessageSize = ServerConfigurationUtil.getConfiguration(instance).getMemoryInBytes(Property.GENERAL_MAX_MESSAGE_SIZE);
     HostAndPort result = HostAndPort.fromParts(opts.getAddress(), port);
     try {
       port = TServerUtils.startTServer(result, processor, this.getClass().getSimpleName(), "GC Monitor Service", 2, 1000, maxMessageSize).address.getPort();
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index c6f5ebb33..e17bccc9f 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -44,7 +44,7 @@ import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.admin.TableOperationsImpl;
import org.apache.accumulo.core.client.impl.ServerConfigurationFactory;
import org.apache.accumulo.core.client.impl.ServerConfigurationUtil;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.ThriftTransportPool;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
@@ -787,7 +787,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
 
     private void updatePlugins(String property) {
       if (property.equals(Property.MASTER_TABLET_BALANCER.getKey())) {
        TabletBalancer balancer = ServerConfigurationFactory.getConfiguration(instance).instantiateClassProperty(Property.MASTER_TABLET_BALANCER, TabletBalancer.class,
        TabletBalancer balancer = ServerConfigurationUtil.getConfiguration(instance).instantiateClassProperty(Property.MASTER_TABLET_BALANCER, TabletBalancer.class,
             new DefaultLoadBalancer());
         balancer.init(serverConfig);
         tabletBalancer = balancer;
diff --git a/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java b/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java
index 7bee351f4..8fdba5a32 100644
-- a/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java
++ b/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java
@@ -30,13 +30,13 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java b/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
index cf4f13485..1a314bf24 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
@@ -27,10 +27,10 @@ import java.util.UUID;
 import org.apache.accumulo.core.client.BatchScanner;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
index 05384d7aa..a26e69e92 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
@@ -24,12 +24,12 @@ import java.util.List;
 import java.util.Map;
 
 import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.KeyExtent;
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java
index 5227b2aea..dc6e972d3 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java
@@ -25,13 +25,13 @@ import java.util.concurrent.TimeUnit;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.MultiTableBatchWriter;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.log4j.Logger;
 
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java
index 749209e3e..d92dea256 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java
@@ -18,10 +18,10 @@ package org.apache.accumulo.test.randomwalk.multitable;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java
index b0c5029d6..22a7371c2 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java
@@ -19,10 +19,10 @@ package org.apache.accumulo.test.randomwalk.sequential;
 import java.io.IOException;
 import java.util.Iterator;
 
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
diff --git a/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java b/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java
index c20d00429..c4dd42d33 100644
-- a/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java
++ b/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java
@@ -21,10 +21,10 @@ import java.util.TreeSet;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.hadoop.io.Text;
 
 public abstract class ScaleTest {
diff --git a/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java b/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java
index ace5d2471..5c2698eba 100644
-- a/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java
++ b/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java
@@ -24,6 +24,7 @@ import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.MultiTableBatchWriter;
 import org.apache.accumulo.core.client.MutationsRejectedException;
@@ -34,7 +35,6 @@ import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.admin.TableOperations;
 import org.apache.accumulo.core.client.impl.MultiTableBatchWriterImpl;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Range;
- 
2.19.1.windows.1

