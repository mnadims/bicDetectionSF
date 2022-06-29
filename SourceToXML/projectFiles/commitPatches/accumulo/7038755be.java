From 7038755be153e11ca5ea7278d96746d72b24ea05 Mon Sep 17 00:00:00 2001
From: Michael Berman <mberman@sqrrl.com>
Date: Tue, 19 Nov 2013 14:22:10 -0500
Subject: [PATCH] ACCUMULO-1009

Signed-off-by: Eric Newton <eric.newton@gmail.com>
--
 .../org/apache/accumulo/core/Constants.java   |   2 +
 .../apache/accumulo/core/cli/ClientOpts.java  |  52 ++-
 .../apache/accumulo/core/client/Instance.java |   2 +
 .../core/client/ZooKeeperInstance.java        |  72 ++--
 .../client/impl/ConditionalWriterImpl.java    |   2 +-
 .../core/client/impl/MasterClient.java        |   2 +-
 .../core/client/impl/ServerClient.java        |   3 +-
 .../impl/TabletServerBatchReaderIterator.java |   2 +-
 .../client/impl/TabletServerBatchWriter.java  |   2 +-
 .../core/client/impl/ThriftTransportKey.java  |  20 +-
 .../core/client/impl/ThriftTransportPool.java |  25 +-
 .../client/mapred/AbstractInputFormat.java    |  16 +
 .../client/mapred/AccumuloInputFormat.java    |   4 +-
 .../mapred/AccumuloMultiTableInputFormat.java |   3 +-
 .../client/mapred/AccumuloOutputFormat.java   |  23 +-
 .../client/mapred/AccumuloRowInputFormat.java |   3 +-
 .../client/mapreduce/AbstractInputFormat.java |  19 +-
 .../client/mapreduce/AccumuloInputFormat.java |   3 +-
 .../mapreduce/AccumuloOutputFormat.java       |  24 +-
 .../mapreduce/AccumuloRowInputFormat.java     |   3 +-
 .../mapreduce/lib/util/ConfiguratorBase.java  |  49 ++-
 .../core/client/mock/MockInstance.java        |   1 +
 .../core/conf/AccumuloConfiguration.java      |  16 +
 .../core/conf/ClientConfiguration.java        | 310 +++++++++++++++++
 .../apache/accumulo/core/conf/Property.java   |  19 +
 .../accumulo/core/conf/PropertyType.java      |   3 +-
 .../accumulo/core/security/Credentials.java   |   2 +-
 .../accumulo/core/security/SecurityUtil.java  |   4 +-
 .../core/util/SslConnectionParams.java        | 205 +++++++++++
 .../apache/accumulo/core/util/ThriftUtil.java | 129 ++++---
 .../accumulo/core/util/shell/Shell.java       |  25 +-
 .../core/util/shell/ShellOptionsJC.java       |  27 ++
 .../client/impl/TabletLocatorImplTest.java    |   1 +
 .../lib/util/ConfiguratorBaseTest.java        |  45 ++-
 .../core/conf/ClientConfigurationTest.java    |  65 ++++
 .../core/util/shell/ShellSetInstanceTest.java |  56 +--
 .../simple/filedata/FileDataQuery.java        |   3 +-
 .../simple/mapreduce/TokenFileWordCount.java  |   3 +-
 .../minicluster/MiniAccumuloCluster.java      |  48 ++-
 .../minicluster/MiniAccumuloConfig.java       |  29 +-
 .../minicluster/MiniAccumuloInstance.java     |  21 +-
 .../MiniAccumuloClusterGCTest.java            |   3 +-
 .../minicluster/MiniAccumuloClusterTest.java  |  10 +-
 .../apache/accumulo/proxy/ProxyServer.java    |   3 +-
 .../server/cli/ClientOnDefaultTable.java      |   2 +-
 .../server/cli/ClientOnRequiredTable.java     |   2 +-
 .../accumulo/server/cli/ClientOpts.java       |   2 +-
 .../accumulo/server/client/BulkImporter.java  |   2 +-
 .../server/client/HdfsZooInstance.java        |   1 +
 .../accumulo/server/util/TServerUtils.java    | 158 ++++++---
 .../accumulo/utils/metanalysis/IndexMeta.java |   7 +-
 test/pom.xml                                  |  27 ++
 .../apache/accumulo/test/IMMLGBenchmark.java  |   3 +-
 .../org/apache/accumulo/test/TestIngest.java  |   3 -
 .../metadata/MetadataBatchScanTest.java       |   3 +-
 .../test/performance/thrift/NullTserver.java  |   3 +-
 .../accumulo/test/randomwalk/State.java       |   3 +-
 .../test/randomwalk/multitable/CopyTool.java  |   7 +-
 .../sequential/MapRedVerifyTool.java          |   7 +-
 .../accumulo/test/scalability/ScaleTest.java  |   3 +-
 .../test/MultiTableBatchWriterTest.java       |  24 +-
 .../apache/accumulo/test/ShellServerIT.java   |  71 ++--
 .../test/functional/AbstractMacIT.java        |  37 +-
 .../functional/AccumuloInputFormatIT.java     |   5 +-
 .../accumulo/test/functional/BulkIT.java      |  14 +-
 .../test/functional/ConcurrencyIT.java        |  24 +-
 .../test/functional/ConfigurableMacIT.java    |   3 +-
 .../accumulo/test/functional/MapReduceIT.java |  19 +-
 .../accumulo/test/functional/ScannerIT.java   |  32 +-
 .../accumulo/test/functional/ShutdownIT.java  |   7 +-
 .../accumulo/test/functional/SimpleMacIT.java |  17 +
 .../accumulo/test/functional/SslIT.java       |  62 ++++
 .../test/functional/SslWithClientAuthIT.java  |  71 ++++
 .../apache/accumulo/test/util/CertUtils.java  | 324 ++++++++++++++++++
 .../accumulo/test/util/CertUtilsTest.java     | 158 +++++++++
 75 files changed, 2095 insertions(+), 365 deletions(-)
 create mode 100644 core/src/main/java/org/apache/accumulo/core/conf/ClientConfiguration.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/util/SslConnectionParams.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java
 create mode 100644 test/src/test/java/org/apache/accumulo/test/functional/SslIT.java
 create mode 100644 test/src/test/java/org/apache/accumulo/test/functional/SslWithClientAuthIT.java
 create mode 100644 test/src/test/java/org/apache/accumulo/test/util/CertUtils.java
 create mode 100644 test/src/test/java/org/apache/accumulo/test/util/CertUtilsTest.java

diff --git a/core/src/main/java/org/apache/accumulo/core/Constants.java b/core/src/main/java/org/apache/accumulo/core/Constants.java
index 9db0c405c..644775ae3 100644
-- a/core/src/main/java/org/apache/accumulo/core/Constants.java
++ b/core/src/main/java/org/apache/accumulo/core/Constants.java
@@ -105,4 +105,6 @@ public class Constants {
   public static final String EXPORT_FILE = "exportMetadata.zip";
   public static final String EXPORT_INFO_FILE = "accumulo_export_info.txt";
   
  // Variables that will be substituted with environment vars in PropertyType.PATH values
  public static final String[] PATH_PROPERTY_ENV_VARS = new String[]{"$ACCUMULO_HOME", "$ACCUMULO_CONF_DIR"};
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
index 3013cec4e..1d26a00a9 100644
-- a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
++ b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
@@ -35,6 +35,8 @@ import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.Properties;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.security.Authorizations;
@@ -160,6 +162,12 @@ public class ClientOpts extends Help {
   @Parameter(names = "--site-file", description = "Read the given accumulo site file to find the accumulo instance")
   public String siteFile = null;
   
  @Parameter(names = "--ssl", description = "Connect to accumulo over SSL")
  public boolean sslEnabled = false;

  @Parameter(names = "--config-file", description = "Read the given client config file.  If omitted, the path searched can be specified with $ACCUMULO_CLIENT_CONF_PATH, which defaults to ~/.accumulo/config:$ACCUMULO_CONF_DIR/client.conf:/etc/accumulo/client.conf")
  public String clientConfigFile = null;

   public void startDebugLogging() {
     if (debug)
       Logger.getLogger(Constants.CORE_PACKAGE_NAME).setLevel(Level.TRACE);
@@ -186,12 +194,39 @@ public class ClientOpts extends Help {
   }
   
   protected Instance cachedInstance = null;
  protected ClientConfiguration cachedClientConfig = null;
   
   synchronized public Instance getInstance() {
     if (cachedInstance != null)
       return cachedInstance;
     if (mock)
       return cachedInstance = new MockInstance(instance);
    return cachedInstance = new ZooKeeperInstance(this.getClientConfiguration());
  }

  public Connector getConnector() throws AccumuloException, AccumuloSecurityException {
    if (this.principal == null || this.getToken() == null)
      throw new AccumuloSecurityException("You must provide a user (-u) and password (-p)", SecurityErrorCode.BAD_CREDENTIALS);
    return getInstance().getConnector(principal, getToken());
  }

  public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
    AccumuloInputFormat.setZooKeeperInstance(job, this.getClientConfiguration());
    AccumuloOutputFormat.setZooKeeperInstance(job, this.getClientConfiguration());
  }

  protected ClientConfiguration getClientConfiguration() throws IllegalArgumentException {
    if (cachedClientConfig != null)
      return cachedClientConfig;

    ClientConfiguration clientConfig;
    try {
      clientConfig = ClientConfiguration.loadDefault(clientConfigFile);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    if (sslEnabled)
      clientConfig.setProperty(ClientProperty.INSTANCE_RPC_SSL_ENABLED, "true");
     if (siteFile != null) {
       AccumuloConfiguration config = new AccumuloConfiguration() {
         Configuration xml = new Configuration();
@@ -220,20 +255,11 @@ public class ClientOpts extends Help {
       this.zookeepers = config.get(Property.INSTANCE_ZK_HOST);
       Path instanceDir = new Path(config.get(Property.INSTANCE_DFS_DIR), "instance_id");
       String instanceIDFromFile = ZooUtil.getInstanceIDFromHdfs(instanceDir);
      return cachedInstance = new ZooKeeperInstance(UUID.fromString(instanceIDFromFile), zookeepers);
      if (config.getBoolean(Property.INSTANCE_RPC_SSL_ENABLED))
        clientConfig.setProperty(ClientProperty.INSTANCE_RPC_SSL_ENABLED, "true");
      return cachedClientConfig = clientConfig.withInstance(UUID.fromString(instanceIDFromFile)).withZkHosts(zookeepers);
     }
    return cachedInstance = new ZooKeeperInstance(this.instance, this.zookeepers);
  }
  
  public Connector getConnector() throws AccumuloException, AccumuloSecurityException {
    if (this.principal == null || this.getToken() == null)
      throw new AccumuloSecurityException("You must provide a user (-u) and password (-p)", SecurityErrorCode.BAD_CREDENTIALS);
    return getInstance().getConnector(principal, getToken());
  }
  
  public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
    AccumuloInputFormat.setZooKeeperInstance(job, instance, zookeepers);
    AccumuloOutputFormat.setZooKeeperInstance(job, instance, zookeepers);
    return cachedClientConfig = clientConfig.withInstance(instance).withZkHosts(zookeepers);
   }
   
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/Instance.java b/core/src/main/java/org/apache/accumulo/core/client/Instance.java
index 27d502f4a..f8a7682b0 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/Instance.java
++ b/core/src/main/java/org/apache/accumulo/core/client/Instance.java
@@ -140,7 +140,9 @@ public interface Instance {
    * 
    * @param conf
    *          accumulo configuration
   * @deprecated since 1.6.0
    */
  @Deprecated
   public abstract void setConfiguration(AccumuloConfiguration conf);
   
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java b/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
index 07cc0a3e2..fb4ab79a6 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ZooKeeperInstance.java
@@ -27,7 +27,8 @@ import org.apache.accumulo.core.client.impl.ConnectorImpl;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.metadata.RootTable;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.util.ArgumentChecker;
@@ -37,6 +38,7 @@ import org.apache.accumulo.core.util.TextUtil;
 import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.commons.configuration.Configuration;
 import org.apache.hadoop.io.Text;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
@@ -68,6 +70,9 @@ public class ZooKeeperInstance implements Instance {
 
   private final int zooKeepersSessionTimeOut;
 
  private AccumuloConfiguration accumuloConf;
  private ClientConfiguration clientConf;

   private volatile boolean closed = false;
 
   /**
@@ -76,10 +81,11 @@ public class ZooKeeperInstance implements Instance {
    *          The name of specific accumulo instance. This is set at initialization time.
    * @param zooKeepers
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
    */

  @Deprecated
   public ZooKeeperInstance(String instanceName, String zooKeepers) {
    this(instanceName, zooKeepers, (int) AccumuloConfiguration.getDefaultConfiguration().getTimeInMillis(Property.INSTANCE_ZK_TIMEOUT));
    this(ClientConfiguration.loadDefault().withInstance(instanceName).withZkHosts(zooKeepers));
   }
 
   /**
@@ -90,16 +96,11 @@ public class ZooKeeperInstance implements Instance {
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
    * @param sessionTimeout
    *          zoo keeper session time out in milliseconds.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
    */

  @Deprecated
   public ZooKeeperInstance(String instanceName, String zooKeepers, int sessionTimeout) {
    ArgumentChecker.notNull(instanceName, zooKeepers);
    this.instanceName = instanceName;
    this.zooKeepers = zooKeepers;
    this.zooKeepersSessionTimeOut = sessionTimeout;
    zooCache = ZooCache.getInstance(zooKeepers, sessionTimeout);
    getInstanceID();
    clientInstances.incrementAndGet();
    this(ClientConfiguration.loadDefault().withInstance(instanceName).withZkHosts(zooKeepers).withZkTimeout(sessionTimeout));
   }
 
   /**
@@ -108,10 +109,11 @@ public class ZooKeeperInstance implements Instance {
    *          The UUID that identifies the accumulo instance you want to connect to.
    * @param zooKeepers
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
    */

  @Deprecated
   public ZooKeeperInstance(UUID instanceId, String zooKeepers) {
    this(instanceId, zooKeepers, (int) AccumuloConfiguration.getDefaultConfiguration().getTimeInMillis(Property.INSTANCE_ZK_TIMEOUT));
    this(ClientConfiguration.loadDefault().withInstance(instanceId).withZkHosts(zooKeepers));
   }
 
   /**
@@ -122,14 +124,34 @@ public class ZooKeeperInstance implements Instance {
    *          A comma separated list of zoo keeper server locations. Each location can contain an optional port, of the format host:port.
    * @param sessionTimeout
    *          zoo keeper session time out in milliseconds.
   * @deprecated since 1.6.0; Use {@link #ZooKeeperInstance(ClientConfiguration)} instead.
    */

  @Deprecated
   public ZooKeeperInstance(UUID instanceId, String zooKeepers, int sessionTimeout) {
    ArgumentChecker.notNull(instanceId, zooKeepers);
    this.instanceId = instanceId.toString();
    this.zooKeepers = zooKeepers;
    this.zooKeepersSessionTimeOut = sessionTimeout;
    zooCache = ZooCache.getInstance(zooKeepers, sessionTimeout);
    this(ClientConfiguration.loadDefault().withInstance(instanceId).withZkHosts(zooKeepers).withZkTimeout(sessionTimeout));
  }

  /**
   * @param config
   *          Client configuration for specifying connection options.
   *          See {@link ClientConfiguration} which extends Configuration with convenience methods specific to Accumulo.
   * @since 1.6.0
   */

  public ZooKeeperInstance(Configuration config) {
    ArgumentChecker.notNull(config);
    if (config instanceof ClientConfiguration) {
      this.clientConf = (ClientConfiguration)config;
    } else {
      this.clientConf = new ClientConfiguration(config);
    }
    this.instanceId = clientConf.get(ClientProperty.INSTANCE_ID);
    this.instanceName = clientConf.get(ClientProperty.INSTANCE_NAME);
    if ((instanceId == null) == (instanceName == null))
      throw new IllegalArgumentException("Expected exactly one of instanceName and instanceId to be set");
    this.zooKeepers = clientConf.get(ClientProperty.INSTANCE_ZK_HOST);
    this.zooKeepersSessionTimeOut = (int) AccumuloConfiguration.getTimeInMillis(clientConf.get(ClientProperty.INSTANCE_ZK_TIMEOUT));
    zooCache = ZooCache.getInstance(zooKeepers, zooKeepersSessionTimeOut);
     clientInstances.incrementAndGet();
   }
 
@@ -241,18 +263,18 @@ public class ZooKeeperInstance implements Instance {
     }
   }
 
  private AccumuloConfiguration conf = null;

   @Override
   public AccumuloConfiguration getConfiguration() {
    if (conf == null)
      conf = AccumuloConfiguration.getDefaultConfiguration();
    return conf;
    if (accumuloConf == null) {
      accumuloConf = clientConf.getAccumuloConfiguration();
    }
    return accumuloConf;
   }
 
   @Override
  @Deprecated
   public void setConfiguration(AccumuloConfiguration conf) {
    this.conf = conf;
    this.accumuloConf = conf;
   }
 
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java
index 6b2a1cfcb..bfbac86de 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ConditionalWriterImpl.java
@@ -534,7 +534,7 @@ class ConditionalWriterImpl implements ConditionalWriter {
   private TabletClientService.Iface getClient(String location) throws TTransportException {
     TabletClientService.Iface client;
     if (timeout < instance.getConfiguration().getTimeInMillis(Property.GENERAL_RPC_TIMEOUT))
      client = ThriftUtil.getTServerClient(location, timeout);
      client = ThriftUtil.getTServerClient(location, instance.getConfiguration(), timeout);
     else
       client = ThriftUtil.getTServerClient(location, instance.getConfiguration());
     return client;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java b/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java
index 32c80f9af..dd28fcac8 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/MasterClient.java
@@ -61,7 +61,7 @@ public class MasterClient {
     
     try {
       // Master requests can take a long time: don't ever time out
      MasterClientService.Client client = ThriftUtil.getClientNoTimeout(new MasterClientService.Client.Factory(), master);
      MasterClientService.Client client = ThriftUtil.getClientNoTimeout(new MasterClientService.Client.Factory(), master, instance.getConfiguration());
       return client;
     } catch (TTransportException tte) {
       if (tte.getCause().getClass().equals(UnknownHostException.class)) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java
index 218bd365f..90db5ee6b 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ServerClient.java
@@ -32,6 +32,7 @@ import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.util.ArgumentChecker;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.core.util.ServerServices;
import org.apache.accumulo.core.util.SslConnectionParams;
 import org.apache.accumulo.core.util.ServerServices.Service;
 import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.accumulo.core.util.UtilWaitThread;
@@ -141,7 +142,7 @@ public class ServerClient {
       if (data != null && !new String(data).equals("master"))
         servers.add(new ThriftTransportKey(
           new ServerServices(new String(data)).getAddressString(Service.TSERV_CLIENT),
          rpcTimeout));
          rpcTimeout, SslConnectionParams.forClient(instance.getConfiguration())));
     }
     
     boolean opened = false;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
index 03763049e..77182073c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
@@ -633,7 +633,7 @@ public class TabletServerBatchReaderIterator implements Iterator<Entry<Key,Value
     try {
       TabletClientService.Client client;
       if (timeoutTracker.getTimeOut() < conf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT))
        client = ThriftUtil.getTServerClient(server, timeoutTracker.getTimeOut());
        client = ThriftUtil.getTServerClient(server, conf, timeoutTracker.getTimeOut());
       else
         client = ThriftUtil.getTServerClient(server, conf);
       
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
index 0dd86bf5a..e2c2802d5 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchWriter.java
@@ -856,7 +856,7 @@ public class TabletServerBatchWriter {
         TabletClientService.Iface client;
         
         if (timeoutTracker.getTimeOut() < instance.getConfiguration().getTimeInMillis(Property.GENERAL_RPC_TIMEOUT))
          client = ThriftUtil.getTServerClient(location, timeoutTracker.getTimeOut());
          client = ThriftUtil.getTServerClient(location, instance.getConfiguration(), timeoutTracker.getTimeOut());
         else
           client = ThriftUtil.getTServerClient(location, instance.getConfiguration());
         
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java
index f07139d16..2816da780 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java
@@ -17,16 +17,17 @@
 package org.apache.accumulo.core.client.impl;
 
 import org.apache.accumulo.core.util.ArgumentChecker;
import org.apache.accumulo.core.util.SslConnectionParams;
 
 class ThriftTransportKey {
   private final String location;
   private final int port;
   private final long timeout;
  private final SslConnectionParams sslParams;
   
   private int hash = -1;
   
  ThriftTransportKey(String location, long timeout) {
    
  ThriftTransportKey(String location, long timeout, SslConnectionParams sslParams) {
     ArgumentChecker.notNull(location);
     String[] locationAndPort = location.split(":", 2);
     if (locationAndPort.length == 2) {
@@ -36,6 +37,7 @@ class ThriftTransportKey {
       throw new IllegalArgumentException("Location was expected to contain port but did not. location=" + location);
     
     this.timeout = timeout;
    this.sslParams = sslParams;
   }
   
   String getLocation() {
@@ -50,23 +52,31 @@ class ThriftTransportKey {
     return timeout;
   }
   
  public boolean isSsl() {
    return sslParams != null;
  }

   @Override
   public boolean equals(Object o) {
     if (!(o instanceof ThriftTransportKey))
       return false;
     ThriftTransportKey ttk = (ThriftTransportKey) o;
    return location.equals(ttk.location) && port == ttk.port && timeout == ttk.timeout;
    return location.equals(ttk.location) && port == ttk.port && timeout == ttk.timeout && (!isSsl() || (ttk.isSsl() && sslParams.equals(ttk.sslParams)));
   }
   
   @Override
   public int hashCode() {
     if (hash == -1)
      hash = (location + Integer.toString(port) + Long.toString(timeout)).hashCode();
      hash = toString().hashCode();
     return hash;
   }
   
   @Override
   public String toString() {
    return location + ":" + Integer.toString(port) + " (" + Long.toString(timeout) + ")";
    return (isSsl()?"ssl:":"") + location + ":" + Integer.toString(port) + " (" + Long.toString(timeout) + ")";
  }

  public SslConnectionParams getSslParams() {
    return sslParams;
   }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportPool.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportPool.java
index e7dabb52b..765a4fcfc 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportPool.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportPool.java
@@ -16,7 +16,6 @@
  */
 package org.apache.accumulo.core.client.impl;
 
import java.io.IOException;
 import java.security.SecurityPermission;
 import java.util.ArrayList;
 import java.util.Collections;
@@ -35,10 +34,9 @@ import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.util.Daemon;
 import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.TTimeoutTransport;
import org.apache.accumulo.core.util.SslConnectionParams;
 import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.log4j.Logger;
import org.apache.thrift.transport.TSocket;
 import org.apache.thrift.transport.TTransport;
 import org.apache.thrift.transport.TTransportException;
 
@@ -362,11 +360,11 @@ public class ThriftTransportPool {
   private ThriftTransportPool() {}
   
   public TTransport getTransportWithDefaultTimeout(HostAndPort addr, AccumuloConfiguration conf) throws TTransportException {
    return getTransport(String.format("%s:%d", addr.getHostText(), addr.getPort()), conf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT));
    return getTransport(String.format("%s:%d", addr.getHostText(), addr.getPort()), conf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT), SslConnectionParams.forClient(conf));
   }
   
  public TTransport getTransport(String location, long milliseconds) throws TTransportException {
    return getTransport(new ThriftTransportKey(location, milliseconds));
  public TTransport getTransport(String location, long milliseconds, SslConnectionParams sslParams) throws TTransportException {
    return getTransport(new ThriftTransportKey(location, milliseconds, sslParams));
   }
   
   private TTransport getTransport(ThriftTransportKey cacheKey) throws TTransportException {
@@ -456,19 +454,8 @@ public class ThriftTransportPool {
   }
   
   private TTransport createNewTransport(ThriftTransportKey cacheKey) throws TTransportException {
    TTransport transport;
    if (cacheKey.getTimeout() == 0) {
      transport = new TSocket(cacheKey.getLocation(), cacheKey.getPort());
    } else {
      try {
        transport = TTimeoutTransport.create(HostAndPort.fromParts(cacheKey.getLocation(), cacheKey.getPort()), cacheKey.getTimeout());
      } catch (IOException ex) {
        throw new TTransportException(ex);
      }
    }
    transport = ThriftUtil.transportFactory().getTransport(transport);
    transport.open();
    
    TTransport transport = ThriftUtil.createClientTransport(HostAndPort.fromParts(cacheKey.getLocation(), cacheKey.getPort()), (int)cacheKey.getTimeout(), cacheKey.getSslParams());

     if (log.isTraceEnabled())
       log.trace("Creating new connection to connection to " + cacheKey.getLocation() + ":" + cacheKey.getPort());
     
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
index 856936e44..53ac4a14c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
@@ -43,6 +43,7 @@ import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
 import org.apache.accumulo.core.client.mapreduce.lib.util.InputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Range;
@@ -179,11 +180,26 @@ public abstract class AbstractInputFormat<K,V> implements InputFormat<K,V> {
    * @param zooKeepers
    *          a comma-separated list of zookeeper servers
    * @since 1.5.0
   * @deprecated since 1.6.0; Use {@link #setZooKeeperInstance(JobConf, ClientConfiguration)} instead.
    */
  @Deprecated
   public static void setZooKeeperInstance(JobConf job, String instanceName, String zooKeepers) {
     InputConfigurator.setZooKeeperInstance(CLASS, job, instanceName, zooKeepers);
   }
 
  /**
   * Configures a {@link org.apache.accumulo.core.client.ZooKeeperInstance} for this job.
   *
   * @param job
   *          the Hadoop job instance to be configured
   * @param clientConfig
   *          client configuration containing connection options
   * @since 1.6.0
   */
  public static void setZooKeeperInstance(JobConf job, ClientConfiguration clientConfig) {
    InputConfigurator.setZooKeeperInstance(CLASS, job, clientConfig);
  }

   /**
    * Configures a {@link org.apache.accumulo.core.client.mock.MockInstance} for this job.
    * 
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java
index cccd7b8b0..ffd74a50b 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloInputFormat.java
@@ -20,6 +20,7 @@ import java.io.IOException;
 import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
@@ -40,7 +41,8 @@ import org.apache.hadoop.mapred.Reporter;
  * <li>{@link AccumuloInputFormat#setConnectorInfo(JobConf, String, AuthenticationToken)}
  * <li>{@link AccumuloInputFormat#setConnectorInfo(JobConf, String, String)}
  * <li>{@link AccumuloInputFormat#setScanAuthorizations(JobConf, Authorizations)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(JobConf, String, String)} OR {@link AccumuloInputFormat#setMockInstance(JobConf, String)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(JobConf, ClientConfiguration)} OR
 * {@link AccumuloInputFormat#setMockInstance(JobConf, String)}
  * </ul>
  * 
  * Other static methods are optional.
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java
index 61838db85..f6eb294bd 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloMultiTableInputFormat.java
@@ -21,6 +21,7 @@ import java.util.Map;
 
 import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
 import org.apache.accumulo.core.client.mapreduce.lib.util.InputConfigurator;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.util.format.DefaultFormatter;
@@ -39,7 +40,7 @@ import org.apache.hadoop.mapred.Reporter;
  * <li>{@link AccumuloInputFormat#setConnectorInfo(JobConf, String, org.apache.accumulo.core.client.security.tokens.AuthenticationToken)}
  * <li>{@link AccumuloInputFormat#setConnectorInfo(JobConf, String, String)}
  * <li>{@link AccumuloInputFormat#setScanAuthorizations(JobConf, org.apache.accumulo.core.security.Authorizations)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(JobConf, String, String)} OR {@link AccumuloInputFormat#setMockInstance(JobConf, String)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(JobConf, ClientConfiguration)} OR {@link AccumuloInputFormat#setMockInstance(JobConf, String)}
  * <li>{@link AccumuloMultiTableInputFormat#setInputTableConfigs(org.apache.hadoop.mapred.JobConf, java.util.Map)}
  * </ul>
  * 
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
index 908b8b3a8..6b418d603 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
@@ -38,6 +38,7 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
@@ -61,7 +62,7 @@ import org.apache.log4j.Logger;
  * <ul>
  * <li>{@link AccumuloOutputFormat#setConnectorInfo(JobConf, String, AuthenticationToken)}
  * <li>{@link AccumuloOutputFormat#setConnectorInfo(JobConf, String, String)}
 * <li>{@link AccumuloOutputFormat#setZooKeeperInstance(JobConf, String, String)} OR {@link AccumuloOutputFormat#setMockInstance(JobConf, String)}
 * <li>{@link AccumuloOutputFormat#setZooKeeperInstance(JobConf, ClientConfiguration)} OR {@link AccumuloOutputFormat#setMockInstance(JobConf, String)}
  * </ul>
  * 
  * Other static methods are optional.
@@ -182,11 +183,31 @@ public class AccumuloOutputFormat implements OutputFormat<Text,Mutation> {
    * @param zooKeepers
    *          a comma-separated list of zookeeper servers
    * @since 1.5.0
   * @deprecated since 1.6.0; Use {@link #setZooKeeperInstance(JobConf, ClientConfiguration)} instead.
    */

  @Deprecated
   public static void setZooKeeperInstance(JobConf job, String instanceName, String zooKeepers) {
     OutputConfigurator.setZooKeeperInstance(CLASS, job, instanceName, zooKeepers);
   }
 
  /**
   * Configures a {@link ZooKeeperInstance} for this job.
   *
   * @param job
   *          the Hadoop job instance to be configured
   * @param instanceName
   *          the Accumulo instance name
   * @param zooKeepers
   *          a comma-separated list of zookeeper servers
   * @param clientConfig
   *          client configuration for specifying connection timeouts, SSL connection options, etc.
   * @since 1.6.0
   */
  public static void setZooKeeperInstance(JobConf job, ClientConfiguration clientConfig) {
    OutputConfigurator.setZooKeeperInstance(CLASS, job, clientConfig);
  }

   /**
    * Configures a {@link MockInstance} for this job.
    * 
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java
index fe5003b50..9c6189b3d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloRowInputFormat.java
@@ -21,6 +21,7 @@ import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
@@ -42,7 +43,7 @@ import org.apache.hadoop.mapred.Reporter;
  * <li>{@link AccumuloRowInputFormat#setConnectorInfo(JobConf, String, AuthenticationToken)}
  * <li>{@link AccumuloRowInputFormat#setInputTableName(JobConf, String)}
  * <li>{@link AccumuloRowInputFormat#setScanAuthorizations(JobConf, Authorizations)}
 * <li>{@link AccumuloRowInputFormat#setZooKeeperInstance(JobConf, String, String)} OR {@link AccumuloRowInputFormat#setMockInstance(JobConf, String)}
 * <li>{@link AccumuloRowInputFormat#setZooKeeperInstance(JobConf, String, String, ClientConfiguration)} OR {@link AccumuloRowInputFormat#setMockInstance(JobConf, String)}
  * </ul>
  * 
  * Other static methods are optional.
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
index 626a785d7..9d8024eda 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
@@ -46,6 +46,7 @@ import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.lib.util.InputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
@@ -189,11 +190,26 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
    * @param zooKeepers
    *          a comma-separated list of zookeeper servers
    * @since 1.5.0
   * @deprecated since 1.6.0; Use {@link #setZooKeeperInstance(Job, ClientConfiguration)} instead.
    */
  @Deprecated
   public static void setZooKeeperInstance(Job job, String instanceName, String zooKeepers) {
     InputConfigurator.setZooKeeperInstance(CLASS, job.getConfiguration(), instanceName, zooKeepers);
   }
 
  /**
   * Configures a {@link org.apache.accumulo.core.client.ZooKeeperInstance} for this job.
   *
   * @param job
   *          the Hadoop job instance to be configured
   * @param clientConfig
   *          client configuration containing connection options
   * @since 1.6.0
   */
  public static void setZooKeeperInstance(Job job, ClientConfiguration clientConfig) {
    InputConfigurator.setZooKeeperInstance(CLASS, job.getConfiguration(), clientConfig);
  }

   /**
    * Configures a {@link org.apache.accumulo.core.client.mock.MockInstance} for this job.
    * 
@@ -379,7 +395,6 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
       // but the scanner will use the table id resolved at job setup time
       InputTableConfig tableConfig = getInputTableConfig(attempt, split.getTableName());
 

       try {
         log.debug("Creating connector with user: " + principal);
         log.debug("Creating scanner for table: " + split.getTableName());
@@ -456,7 +471,7 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
 
     return InputConfigurator.binOffline(tableId, ranges, instance, conn);
   }
  

   /**
    * Gets the splits of the tables that have been set on the job.
    * 
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
index 9ecae5316..0539c9345 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
@@ -20,6 +20,7 @@ import java.io.IOException;
 import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
@@ -39,7 +40,7 @@ import org.apache.hadoop.mapreduce.TaskAttemptContext;
  * <ul>
  * <li>{@link AccumuloInputFormat#setConnectorInfo(Job, String, AuthenticationToken)}
  * <li>{@link AccumuloInputFormat#setScanAuthorizations(Job, Authorizations)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(Job, String, String)} OR {@link AccumuloInputFormat#setMockInstance(Job, String)}
 * <li>{@link AccumuloInputFormat#setZooKeeperInstance(Job, ClientConfiguration)} OR {@link AccumuloInputFormat#setMockInstance(Job, String)}
  * </ul>
  * 
  * Other static methods are optional.
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
index 727bfecce..6782b4b37 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
@@ -38,6 +38,7 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
@@ -62,7 +63,7 @@ import org.apache.log4j.Logger;
  * <ul>
  * <li>{@link AccumuloOutputFormat#setConnectorInfo(Job, String, AuthenticationToken)}
  * <li>{@link AccumuloOutputFormat#setConnectorInfo(Job, String, String)}
 * <li>{@link AccumuloOutputFormat#setZooKeeperInstance(Job, String, String)} OR {@link AccumuloOutputFormat#setMockInstance(Job, String)}
 * <li>{@link AccumuloOutputFormat#setZooKeeperInstance(Job, ClientConfiguration)} OR {@link AccumuloOutputFormat#setMockInstance(Job, String)}
  * </ul>
  * 
  * Other static methods are optional.
@@ -183,11 +184,30 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @param zooKeepers
    *          a comma-separated list of zookeeper servers
    * @since 1.5.0
   * @deprecated since 1.6.0; Use {@link #setZooKeeperInstance(Job, ClientConfiguration)} instead.
    */
  @Deprecated
   public static void setZooKeeperInstance(Job job, String instanceName, String zooKeepers) {
     OutputConfigurator.setZooKeeperInstance(CLASS, job.getConfiguration(), instanceName, zooKeepers);
   }
 
  /**
   * Configures a {@link ZooKeeperInstance} for this job.
   *
   * @param job
   *          the Hadoop job instance to be configured
   * @param instanceName
   *          the Accumulo instance name
   * @param zooKeepers
   *          a comma-separated list of zookeeper servers
   * @param clientConfig
   *          client configuration for specifying connection timeouts, SSL connection options, etc.
   * @since 1.6.0
   */
  public static void setZooKeeperInstance(Job job, ClientConfiguration clientConfig) {
    OutputConfigurator.setZooKeeperInstance(CLASS, job.getConfiguration(), clientConfig);
  }

   /**
    * Configures a {@link MockInstance} for this job.
    * 
@@ -208,7 +228,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    *          the Hadoop context for the configured job
    * @return an Accumulo instance
    * @since 1.5.0
   * @see #setZooKeeperInstance(Job, String, String)
   * @see #setZooKeeperInstance(Job, String, String, ClientConfiguration)
    * @see #setMockInstance(Job, String)
    */
   protected static Instance getInstance(JobContext context) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java
index 992990d40..a52b098cb 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloRowInputFormat.java
@@ -21,6 +21,7 @@ import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
@@ -42,7 +43,7 @@ import org.apache.hadoop.mapreduce.TaskAttemptContext;
  * <li>{@link AccumuloRowInputFormat#setConnectorInfo(Job, String, AuthenticationToken)}
  * <li>{@link AccumuloRowInputFormat#setInputTableName(Job, String)}
  * <li>{@link AccumuloRowInputFormat#setScanAuthorizations(Job, Authorizations)}
 * <li>{@link AccumuloRowInputFormat#setZooKeeperInstance(Job, String, String)} OR {@link AccumuloRowInputFormat#setMockInstance(Job, String)}
 * <li>{@link AccumuloRowInputFormat#setZooKeeperInstance(Job, String, String, ClientConfiguration)} OR {@link AccumuloRowInputFormat#setMockInstance(Job, String)}
  * </ul>
  * 
  * Other static methods are optional.
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java
index 4f8cdb647..c0fcc7231 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBase.java
@@ -27,6 +27,7 @@ import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.util.ArgumentChecker;
 import org.apache.commons.codec.binary.Base64;
@@ -72,7 +73,7 @@ public class ConfiguratorBase {
    * @since 1.5.0
    */
   protected static enum InstanceOpts {
    TYPE, NAME, ZOO_KEEPERS;
    TYPE, NAME, ZOO_KEEPERS, CLIENT_CONFIG;
   }
 
   /**
@@ -277,16 +278,38 @@ public class ConfiguratorBase {
    * @param zooKeepers
    *          a comma-separated list of zookeeper servers
    * @since 1.5.0
   * @deprecated since 1.6.0; Use {@link #setZooKeeperInstance(Class, Configuration, ClientConfiguration)} instead.
    */

  @Deprecated
   public static void setZooKeeperInstance(Class<?> implementingClass, Configuration conf, String instanceName, String zooKeepers) {
    ArgumentChecker.notNull(instanceName, zooKeepers);
    setZooKeeperInstance(implementingClass, conf, new ClientConfiguration().withInstance(instanceName).withZkHosts(zooKeepers));
  }

  /**
   * Configures a {@link ZooKeeperInstance} for this job.
   *
   * @param implementingClass
   *          the class whose name will be used as a prefix for the property configuration key
   * @param conf
   *          the Hadoop configuration object to configure
   * @param instanceName
   *          the Accumulo instance name
   * @param zooKeepers
   *          a comma-separated list of zookeeper servers
   * @param clientConfig
   *          client configuration for specifying connection timeouts, SSL connection options, etc.
   * @since 1.5.0
   */
  public static void setZooKeeperInstance(Class<?> implementingClass, Configuration conf, ClientConfiguration clientConfig) {
     String key = enumToConfKey(implementingClass, InstanceOpts.TYPE);
     if (!conf.get(key, "").isEmpty())
       throw new IllegalStateException("Instance info can only be set once per job; it has already been configured with " + conf.get(key));
     conf.set(key, "ZooKeeperInstance");

    ArgumentChecker.notNull(instanceName, zooKeepers);
    conf.set(enumToConfKey(implementingClass, InstanceOpts.NAME), instanceName);
    conf.set(enumToConfKey(implementingClass, InstanceOpts.ZOO_KEEPERS), zooKeepers);
    if (clientConfig != null) {
      conf.set(enumToConfKey(implementingClass, InstanceOpts.CLIENT_CONFIG), clientConfig.serialize());
    }
   }
 
   /**
@@ -319,17 +342,23 @@ public class ConfiguratorBase {
    *          the Hadoop configuration object to configure
    * @return an Accumulo instance
    * @since 1.5.0
   * @see #setZooKeeperInstance(Class, Configuration, String, String)
   * @see #setZooKeeperInstance(Class, Configuration, String, String, ClientConfiguration)
    * @see #setMockInstance(Class, Configuration, String)
    */
   public static Instance getInstance(Class<?> implementingClass, Configuration conf) {
     String instanceType = conf.get(enumToConfKey(implementingClass, InstanceOpts.TYPE), "");
     if ("MockInstance".equals(instanceType))
       return new MockInstance(conf.get(enumToConfKey(implementingClass, InstanceOpts.NAME)));
    else if ("ZooKeeperInstance".equals(instanceType))
      return new ZooKeeperInstance(conf.get(enumToConfKey(implementingClass, InstanceOpts.NAME)), conf.get(enumToConfKey(implementingClass,
          InstanceOpts.ZOO_KEEPERS)));
    else if (instanceType.isEmpty())
    else if ("ZooKeeperInstance".equals(instanceType)) {
      String clientConfigString = conf.get(enumToConfKey(implementingClass, InstanceOpts.CLIENT_CONFIG));
      if (clientConfigString == null) {
        String instanceName = conf.get(enumToConfKey(implementingClass, InstanceOpts.NAME));
        String zookeepers = conf.get(enumToConfKey(implementingClass, InstanceOpts.ZOO_KEEPERS));
        return new ZooKeeperInstance(ClientConfiguration.loadDefault().withInstance(instanceName).withZkHosts(zookeepers));
      } else {
        return new ZooKeeperInstance(ClientConfiguration.deserialize(clientConfigString));
      }
    } else if (instanceType.isEmpty())
       throw new IllegalStateException("Instance has not been configured for " + implementingClass.getSimpleName());
     else
       throw new IllegalStateException("Unrecognized instance type " + instanceType);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mock/MockInstance.java b/core/src/main/java/org/apache/accumulo/core/client/mock/MockInstance.java
index d39146452..5b7dcbf9c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mock/MockInstance.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mock/MockInstance.java
@@ -145,6 +145,7 @@ public class MockInstance implements Instance {
   }
   
   @Override
  @Deprecated
   public void setConfiguration(AccumuloConfiguration conf) {
     this.conf = conf;
   }
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java b/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java
index da170e984..3aed8c199 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/AccumuloConfiguration.java
@@ -22,6 +22,7 @@ import java.util.Map;
 import java.util.Map.Entry;
 import java.util.TreeMap;
 
import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.TableNotFoundException;
@@ -180,6 +181,21 @@ public abstract class AccumuloConfiguration implements Iterable<Entry<String,Str
     return Integer.parseInt(countString);
   }
   
  public String getPath(Property property) {
    checkType(property, PropertyType.PATH);

    String pathString = get(property);
    if (pathString == null) return null;

    for (String replaceableEnvVar : Constants.PATH_PROPERTY_ENV_VARS) {
      String envValue = System.getenv(replaceableEnvVar);
      if (envValue != null)
        pathString = pathString.replace("$" + replaceableEnvVar, envValue);
    }

    return pathString;
  }

   public static synchronized DefaultConfiguration getDefaultConfiguration() {
     return DefaultConfiguration.getInstance();
   }
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/ClientConfiguration.java b/core/src/main/java/org/apache/accumulo/core/conf/ClientConfiguration.java
new file mode 100644
index 000000000..5bb95aeb3
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/conf/ClientConfiguration.java
@@ -0,0 +1,310 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.conf;

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

import org.apache.accumulo.core.util.ArgumentChecker;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Contains a list of property keys recognized by the Accumulo client and convenience methods for setting them.
 */
public class ClientConfiguration extends CompositeConfiguration {
  public static final String USER_ACCUMULO_DIR_NAME = ".accumulo";
  public static final String USER_CONF_FILENAME = "config";
  public static final String GLOBAL_CONF_FILENAME = "client.conf";

  public enum ClientProperty {
    RPC_SSL_TRUSTSTORE_PATH(Property.RPC_SSL_TRUSTSTORE_PATH),
    RPC_SSL_TRUSTSTORE_PASSWORD(Property.RPC_SSL_TRUSTSTORE_PASSWORD),
    RPC_SSL_TRUSTSTORE_TYPE(Property.RPC_SSL_TRUSTSTORE_TYPE),
    RPC_SSL_KEYSTORE_PATH(Property.RPC_SSL_KEYSTORE_PATH),
    RPC_SSL_KEYSTORE_PASSWORD(Property.RPC_SSL_KEYSTORE_PASSWORD),
    RPC_SSL_KEYSTORE_TYPE(Property.RPC_SSL_KEYSTORE_TYPE),
    RPC_USE_JSSE(Property.RPC_USE_JSSE),
    INSTANCE_RPC_SSL_CLIENT_AUTH(Property.INSTANCE_RPC_SSL_CLIENT_AUTH),
    INSTANCE_RPC_SSL_ENABLED(Property.INSTANCE_RPC_SSL_ENABLED),
    INSTANCE_ZK_HOST(Property.INSTANCE_ZK_HOST),
    INSTANCE_ZK_TIMEOUT(Property.INSTANCE_ZK_TIMEOUT),
    INSTANCE_NAME("client.instance.name", null, PropertyType.STRING, "Name of Accumulo instance to connect to"),
    INSTANCE_ID("client.instance.id", null, PropertyType.STRING, "UUID of Accumulo instance to connect to"),
    ;

    private String key;
    private String defaultValue;
    private PropertyType type;
    private String description;

    private Property accumuloProperty = null;

    private ClientProperty(Property prop) {
      this(prop.getKey(), prop.getDefaultValue(), prop.getType(), prop.getDescription());
      accumuloProperty = prop;
    }

    private ClientProperty(String key, String defaultValue, PropertyType type, String description) {
      this.key = key;
      this.defaultValue = defaultValue;
      this.type = type;
      this.description = description;
    }

    public String getKey() {
      return key;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public PropertyType getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public Property getAccumuloProperty() {
      return accumuloProperty;
    }

    public static ClientProperty getPropertyByKey(String key) {
      for (ClientProperty prop : ClientProperty.values())
        if (prop.getKey().equals(key))
          return prop;
      return null;
    }
  };

  public ClientConfiguration(List<? extends Configuration> configs) {
    super(configs);
  }

  public ClientConfiguration(Configuration... configs) {
    this(Arrays.asList(configs));
  }

  public static ClientConfiguration loadDefault() {
    return loadFromSearchPath(getDefaultSearchPath());
  }

  public static ClientConfiguration loadDefault(String overridePropertiesFilename) throws FileNotFoundException, ConfigurationException {
    if (overridePropertiesFilename == null)
      return loadDefault();
    else
      return new ClientConfiguration(new PropertiesConfiguration(overridePropertiesFilename));
  }

  private static ClientConfiguration loadFromSearchPath(List<String> paths) {
    try {
      List<Configuration> configs = new LinkedList<Configuration>();
      for (String path : paths) {
        File conf = new File(path);
        if (conf.canRead()) {
          configs.add(new PropertiesConfiguration(conf));
       }
      }
      return new ClientConfiguration(configs);
    } catch (ConfigurationException e) {
      throw new IllegalStateException("Error loading client configuration", e);
    }
  }

  public static ClientConfiguration deserialize(String serializedConfig) {
    PropertiesConfiguration propConfig = new PropertiesConfiguration();
    try {
      propConfig.load(new StringReader(serializedConfig));
    } catch (ConfigurationException e) {
      throw new IllegalArgumentException("Error deserializing client configuration: " + serializedConfig, e);
    }
    return new ClientConfiguration(propConfig);
  }

  private static List<String> getDefaultSearchPath() {
    String clientConfSearchPath = System.getenv("ACCUMULO_CLIENT_CONF_PATH");
    List<String> clientConfPaths;
    if (clientConfSearchPath != null) {
      clientConfPaths = Arrays.asList(clientConfSearchPath.split(File.pathSeparator));
    } else {
      // if $ACCUMULO_CLIENT_CONF_PATH env isn't set, priority from top to bottom is:
      // ~/.accumulo/config
      // $ACCUMULO_CONF_DIR/client.conf -OR- $ACCUMULO_HOME/conf/client.conf (depending on whether $ACCUMULO_CONF_DIR is set)
      // /etc/accumulo/client.conf
      clientConfPaths = new LinkedList<String>();
      clientConfPaths.add(System.getProperty("user.home") + File.separator + USER_ACCUMULO_DIR_NAME + File.separator + USER_CONF_FILENAME);
      if (System.getenv("ACCUMULO_CONF_DIR") != null) {
        clientConfPaths.add(System.getenv("ACCUMULO_CONF_DIR") + File.separator + GLOBAL_CONF_FILENAME);
      } else if (System.getenv("ACCUMULO_HOME") != null) {
        clientConfPaths.add(System.getenv("ACCUMULO_HOME") + File.separator + "conf" + File.separator + GLOBAL_CONF_FILENAME);
      }
      clientConfPaths.add("/etc/accumulo/" + GLOBAL_CONF_FILENAME);
    }
    return clientConfPaths;
  }

  public String serialize() {
    PropertiesConfiguration propConfig = new PropertiesConfiguration();
    propConfig.copy(this);
    StringWriter writer = new StringWriter();
    try {
      propConfig.save(writer);
    } catch (ConfigurationException e) {
      // this should never happen
      throw new IllegalStateException(e);
    }
    return writer.toString();
  }

  public String get(ClientProperty prop) {
    if (this.containsKey(prop.getKey()))
      return this.getString(prop.getKey());
    else
      return prop.getDefaultValue();
  }

  public void setProperty(ClientProperty prop, String value) {
    this.setProperty(prop.getKey(), value);
  }

  public ClientConfiguration with(ClientProperty prop, String value) {
    this.setProperty(prop.getKey(), value);
    return this;
  }

  public ClientConfiguration withInstance(String instanceName) {
    ArgumentChecker.notNull(instanceName);
    return with(ClientProperty.INSTANCE_NAME, instanceName);
  }

  public ClientConfiguration withInstance(UUID instanceId) {
    ArgumentChecker.notNull(instanceId);
    return with(ClientProperty.INSTANCE_ID, instanceId.toString());
  }

  public ClientConfiguration withZkHosts(String zooKeepers) {
    ArgumentChecker.notNull(zooKeepers);
    return with(ClientProperty.INSTANCE_ZK_HOST, zooKeepers);
  }

  public ClientConfiguration withZkTimeout(int timeout) {
    return with(ClientProperty.INSTANCE_ZK_TIMEOUT, String.valueOf(timeout));
  }

  public ClientConfiguration withSsl(boolean sslEnabled) {
    return withSsl(sslEnabled, false);
  }

  public ClientConfiguration withSsl(boolean sslEnabled, boolean useJsseConfig) {
    return with(ClientProperty.INSTANCE_RPC_SSL_ENABLED, String.valueOf(sslEnabled))
        .with(ClientProperty.RPC_USE_JSSE, String.valueOf(useJsseConfig));
  }

  public ClientConfiguration withTruststore(String path) {
    return withTruststore(path, null, null);
  }

  public ClientConfiguration withTruststore(String path, String password, String type) {
    ArgumentChecker.notNull(path);
    setProperty(ClientProperty.RPC_SSL_TRUSTSTORE_PATH, path);
    if (password != null)
      setProperty(ClientProperty.RPC_SSL_TRUSTSTORE_PASSWORD, password);
    if (type != null)
      setProperty(ClientProperty.RPC_SSL_TRUSTSTORE_TYPE, type);
    return this;
  }

  public ClientConfiguration withKeystore(String path) {
    return withKeystore(path, null, null);
  }

  public ClientConfiguration withKeystore(String path, String password, String type) {
    ArgumentChecker.notNull(path);
    setProperty(ClientProperty.INSTANCE_RPC_SSL_CLIENT_AUTH, "true");
    setProperty(ClientProperty.RPC_SSL_KEYSTORE_PATH, path);
    if (password != null)
      setProperty(ClientProperty.RPC_SSL_KEYSTORE_PASSWORD, password);
    if (type != null)
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
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/Property.java b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
index b915ee431..4a38d1585 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/Property.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
@@ -77,6 +77,23 @@ public enum Property {
           + "of that file.  Sometimes, you change your strategy and want to use the new strategy, not the old one.  (Most commonly, this will be "
           + "because you have moved key material from one spot to another.)  If you want to override the recorded key strategy with the one in "
           + "the configuration file, set this property to true."),
  // SSL properties local to each node (see also instance.ssl.enabled which must be consistent across all nodes in an instance)
  RPC_PREFIX("rpc.", null, PropertyType.PREFIX, "Properties in this category related to the configuration of SSL keys for RPC.  See also instance.ssl.enabled"),
  RPC_SSL_KEYSTORE_PATH("rpc.javax.net.ssl.keyStore", "$ACCUMULO_CONF_DIR/ssl/keystore.jks", PropertyType.PATH,
      "Path of the keystore file for the servers' private SSL key"),
  @Sensitive
  RPC_SSL_KEYSTORE_PASSWORD("rpc.javax.net.ssl.keyStorePassword", "", PropertyType.STRING,
      "Password used to encrypt the SSL private keystore.  Leave blank to use the Accumulo instance secret"),
  RPC_SSL_KEYSTORE_TYPE("rpc.javax.net.ssl.keyStoreType", "jks", PropertyType.STRING,
      "Type of SSL keystore"),
  RPC_SSL_TRUSTSTORE_PATH("rpc.javax.net.ssl.trustStore", "$ACCUMULO_CONF_DIR/ssl/truststore.jks", PropertyType.PATH,
      "Path of the truststore file for the root cert"),
  @Sensitive
  RPC_SSL_TRUSTSTORE_PASSWORD("rpc.javax.net.ssl.trustStorePassword", "", PropertyType.STRING,
      "Password used to encrypt the SSL truststore.  Leave blank to use no password"),
  RPC_SSL_TRUSTSTORE_TYPE("rpc.javax.net.ssl.trustStoreType", "jks", PropertyType.STRING,
        "Type of SSL truststore"),
  RPC_USE_JSSE("rpc.useJsse", "false", PropertyType.BOOLEAN, "Use JSSE system properties to configure SSL rather than general.javax.net.ssl.* Accumulo properties"),
   // instance properties (must be the same for every node in an instance)
   INSTANCE_PREFIX("instance.", null, PropertyType.PREFIX,
       "Properties in this category must be consistent throughout a cloud. This is enforced and servers won't be able to communicate if these differ."),
@@ -106,6 +123,8 @@ public enum Property {
       "The authorizor class that accumulo will use to determine what labels a user has privilege to see"),
   INSTANCE_SECURITY_PERMISSION_HANDLER("instance.security.permissionHandler", "org.apache.accumulo.server.security.handler.ZKPermHandler",
       PropertyType.CLASSNAME, "The permission handler class that accumulo will use to determine if a user has privilege to perform an action"),
  INSTANCE_RPC_SSL_ENABLED("instance.rpc.ssl.enabled", "false", PropertyType.BOOLEAN, "Use SSL for socket connections from clients and among accumulo services"),
  INSTANCE_RPC_SSL_CLIENT_AUTH("instance.rpc.ssl.clientAuth", "false", PropertyType.BOOLEAN, "Require clients to present certs signed by a trusted root"),
 
   // general properties
   GENERAL_PREFIX("general.", null, PropertyType.PREFIX,
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java b/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
index fc454423f..688b1863e 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/PropertyType.java
@@ -18,6 +18,7 @@ package org.apache.accumulo.core.conf;
 
 import java.util.regex.Pattern;
 
import org.apache.accumulo.core.Constants;
 import org.apache.hadoop.fs.Path;
 
 public enum PropertyType {
@@ -50,7 +51,7 @@ public enum PropertyType {
           + "Examples of invalid fractions/percentages are '', '10 percent', 'Hulk Hogan'"),
   
   PATH("path", ".*",
      "A string that represents a filesystem path, which can be either relative or absolute to some directory. The filesystem depends on the property."),
      "A string that represents a filesystem path, which can be either relative or absolute to some directory. The filesystem depends on the property.  The following environment variables will be substituted: " + Constants.PATH_PROPERTY_ENV_VARS),
   ABSOLUTEPATH("absolute path", null,
       "An absolute filesystem path. The filesystem depends on the property. This is the same as path, but enforces that its root is explicitly specified.") {
     @Override
diff --git a/core/src/main/java/org/apache/accumulo/core/security/Credentials.java b/core/src/main/java/org/apache/accumulo/core/security/Credentials.java
index 0552e7e6e..45708a8c0 100644
-- a/core/src/main/java/org/apache/accumulo/core/security/Credentials.java
++ b/core/src/main/java/org/apache/accumulo/core/security/Credentials.java
@@ -70,7 +70,7 @@ public class Credentials {
    * Converts the current object to a serialized form. The object returned from this contains a non-destroyable version of the {@link AuthenticationToken}, so
    * references to it should be tightly controlled.
    */
  public final String serialize() throws AccumuloSecurityException {
  public final String serialize() {
     return (getPrincipal() == null ? "-" : Base64.encodeBase64String(getPrincipal().getBytes(Constants.UTF8))) + ":"
         + (getToken() == null ? "-" : Base64.encodeBase64String(getToken().getClass().getName().getBytes(Constants.UTF8))) + ":"
         + (getToken() == null ? "-" : Base64.encodeBase64String(AuthenticationTokenSerializer.serialize(getToken())));
diff --git a/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java b/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java
index 8add1a7ac..4ffcc36d7 100644
-- a/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/security/SecurityUtil.java
@@ -37,13 +37,11 @@ public class SecurityUtil {
   public static void serverLogin() {
     @SuppressWarnings("deprecation")
     AccumuloConfiguration acuConf = AccumuloConfiguration.getSiteConfiguration();
    String keyTab = acuConf.get(Property.GENERAL_KERBEROS_KEYTAB);
    String keyTab = acuConf.getPath(Property.GENERAL_KERBEROS_KEYTAB);
     if (keyTab == null || keyTab.length() == 0)
       return;
     
     usingKerberos = true;
    if (keyTab.contains("$ACCUMULO_HOME") && System.getenv("ACCUMULO_HOME") != null)
      keyTab = keyTab.replace("$ACCUMULO_HOME", System.getenv("ACCUMULO_HOME"));
     
     String principalConfig = acuConf.get(Property.GENERAL_KERBEROS_PRINCIPAL);
     if (principalConfig == null || principalConfig.length() == 0)
diff --git a/core/src/main/java/org/apache/accumulo/core/util/SslConnectionParams.java b/core/src/main/java/org/apache/accumulo/core/util/SslConnectionParams.java
new file mode 100644
index 000000000..6fde38a43
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/util/SslConnectionParams.java
@@ -0,0 +1,205 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.log4j.Logger;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;

public class SslConnectionParams  {
  private static final Logger log = Logger.getLogger(SslConnectionParams.class);

  private boolean useJsse = false;
  private boolean clientAuth = false;

  private boolean keyStoreSet;
  private String keyStorePath;
  private String keyStorePass;
  private String keyStoreType;

  private boolean trustStoreSet;
  private String trustStorePath;
  private String trustStorePass;
  private String trustStoreType;

  public static SslConnectionParams forConfig(AccumuloConfiguration conf, boolean server) {
    if (!conf.getBoolean(Property.INSTANCE_RPC_SSL_ENABLED))
      return null;

    SslConnectionParams result = new SslConnectionParams();
    boolean requireClientAuth = conf.getBoolean(Property.INSTANCE_RPC_SSL_CLIENT_AUTH);
    if (server) {
      result.setClientAuth(requireClientAuth);
    }
    if (conf.getBoolean(Property.RPC_USE_JSSE)) {
      result.setUseJsse(true);
      return result;
    }

    try {
      if (!server || requireClientAuth) {
        result.setTrustStoreFromConf(conf);
      }
      if (server || requireClientAuth) {
        result.setKeyStoreFromConf(conf);
      }
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("Could not load configured keystore file", e);
    }

    return result;
  }

  private static String passwordFromConf(AccumuloConfiguration conf, String defaultPassword, Property passwordOverrideProperty) {
    String keystorePassword = conf.get(passwordOverrideProperty);
    if (!keystorePassword.isEmpty()) {
      log.debug("Using explicit SSL private key password from " + passwordOverrideProperty.getKey());
    } else {
      keystorePassword = defaultPassword;
    }
    return keystorePassword;
  }

  private static String storePathFromConf(AccumuloConfiguration conf, Property pathProperty) throws FileNotFoundException {
    return findKeystore(conf.getPath(pathProperty));
  }

  public void setKeyStoreFromConf(AccumuloConfiguration conf) throws FileNotFoundException {
    keyStoreSet = true;
    keyStorePath = storePathFromConf(conf, Property.RPC_SSL_KEYSTORE_PATH);
    keyStorePass = passwordFromConf(conf, conf.get(Property.INSTANCE_SECRET), Property.RPC_SSL_KEYSTORE_PASSWORD);
    keyStoreType = conf.get(Property.RPC_SSL_KEYSTORE_TYPE);
  }

  public void setTrustStoreFromConf(AccumuloConfiguration conf) throws FileNotFoundException {
    trustStoreSet = true;
    trustStorePath = storePathFromConf(conf, Property.RPC_SSL_TRUSTSTORE_PATH);
    trustStorePass = passwordFromConf(conf, "", Property.RPC_SSL_TRUSTSTORE_PASSWORD);
    trustStoreType = conf.get(Property.RPC_SSL_TRUSTSTORE_TYPE);
  }

  public static SslConnectionParams forServer(AccumuloConfiguration configuration) {
    return forConfig(configuration, true);
  }

  public static SslConnectionParams forClient(AccumuloConfiguration configuration) {
    return forConfig(configuration, false);
  }

  private static String findKeystore(String keystorePath) throws FileNotFoundException {
    try {
      // first just try the file
      File file = new File(keystorePath);
      if (file.exists())
        return file.getAbsolutePath();
      if (!file.isAbsolute()) {
        // try classpath
        URL url = SslConnectionParams.class.getClassLoader().getResource(keystorePath);
        if (url != null) {
            file = new File(url.toURI());
            if (file.exists())
              return file.getAbsolutePath();
        }
      }
    } catch (Exception e) {
      log.warn("Exception finding keystore", e);
    }
    throw new FileNotFoundException("Failed to load SSL keystore from " + keystorePath);
  }

  public void setUseJsse(boolean useJsse) {
    this.useJsse = useJsse;
  }

  public boolean useJsse() {
    return useJsse;
  }

  public void setClientAuth(boolean clientAuth) {
    this.clientAuth = clientAuth;
  }

  public boolean isClientAuth() {
    return clientAuth;
  }

  public TSSLTransportParameters getTTransportParams() {
    if (useJsse)
      throw new IllegalStateException("Cannot get TTransportParams for JSEE configuration.");
    TSSLTransportParameters params = new TSSLTransportParameters();
    params.requireClientAuth(clientAuth);
    if (keyStoreSet) {
      params.setKeyStore(keyStorePath, keyStorePass, null, keyStoreType);
    }
    if (trustStoreSet) {
      params.setTrustStore(trustStorePath, trustStorePass, null, trustStoreType);
    }
    return params;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash = 31*hash + (clientAuth?0:1);
    hash = 31*hash + (useJsse?0:1);
    if (useJsse)
      return hash;
    hash = 31*hash + (keyStoreSet?0:1);
    hash = 31*hash + (trustStoreSet?0:1);
    if (keyStoreSet) {
      hash = 31*hash + keyStorePath.hashCode();
    }
    if (trustStoreSet) {
      hash = 31*hash + trustStorePath.hashCode();
    }
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SslConnectionParams))
      return false;

    SslConnectionParams other = (SslConnectionParams)obj;
    if (clientAuth != other.clientAuth)
      return false;
    if (useJsse)
      return other.useJsse;
    if (keyStoreSet) {
      if (!other.keyStoreSet)
        return false;
      if (!keyStorePath.equals(other.keyStorePath) ||
          !keyStorePass.equals(other.keyStorePass) ||
          !keyStoreType.equals(other.keyStoreType))
        return false;
    }
    if (trustStoreSet) {
      if (!other.trustStoreSet)
        return false;
      if (!trustStorePath.equals(other.trustStorePath) ||
          !trustStorePass.equals(other.trustStorePass) ||
          !trustStoreType.equals(other.trustStoreType))
        return false;
    }
    return true;
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java b/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java
index e8dd6a29a..fab02b241 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/util/ThriftUtil.java
@@ -17,6 +17,7 @@
 package org.apache.accumulo.core.util;
 
 import java.io.IOException;
import java.net.InetAddress;
 import java.util.HashMap;
 import java.util.Map;
 
@@ -40,6 +41,9 @@ import org.apache.thrift.protocol.TMessage;
 import org.apache.thrift.protocol.TProtocol;
 import org.apache.thrift.protocol.TProtocolFactory;
 import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TSocket;
 import org.apache.thrift.transport.TTransport;
 import org.apache.thrift.transport.TTransportException;
 import org.apache.thrift.transport.TTransportFactory;
@@ -48,15 +52,15 @@ import com.google.common.net.HostAndPort;
 
 public class ThriftUtil {
   private static final Logger log = Logger.getLogger(ThriftUtil.class);
  

   public static class TraceProtocol extends TCompactProtocol {
    

     @Override
     public void writeMessageBegin(TMessage message) throws TException {
       Trace.start("client:" + message.name);
       super.writeMessageBegin(message);
     }
    

     @Override
     public void writeMessageEnd() throws TException {
       super.writeMessageEnd();
@@ -64,63 +68,65 @@ public class ThriftUtil {
       if (currentTrace != null)
         currentTrace.stop();
     }
    

     public TraceProtocol(TTransport transport) {
       super(transport);
     }
   }
  

   public static class TraceProtocolFactory extends TCompactProtocol.Factory {
     private static final long serialVersionUID = 1L;
    

     @Override
     public TProtocol getProtocol(TTransport trans) {
       return new TraceProtocol(trans);
     }
   }
  

   static private TProtocolFactory protocolFactory = new TraceProtocolFactory();
   static private TTransportFactory transportFactory = new TFramedTransport.Factory(Integer.MAX_VALUE);
  

   static public <T extends TServiceClient> T createClient(TServiceClientFactory<T> factory, TTransport transport) {
     return factory.getClient(protocolFactory.getProtocol(transport), protocolFactory.getProtocol(transport));
   }
  

   static public <T extends TServiceClient> T getClient(TServiceClientFactory<T> factory, HostAndPort address, AccumuloConfiguration conf)
       throws TTransportException {
     return createClient(factory, ThriftTransportPool.getInstance().getTransportWithDefaultTimeout(address, conf));
   }
  
  static public <T extends TServiceClient> T getClientNoTimeout(TServiceClientFactory<T> factory, String address) throws TTransportException {
    return createClient(factory, ThriftTransportPool.getInstance().getTransport(address, 0));
  }
  
  static public <T extends TServiceClient> T getClient(TServiceClientFactory<T> factory, String address, Property property, AccumuloConfiguration configuration)

  static public <T extends TServiceClient> T getClientNoTimeout(TServiceClientFactory<T> factory, String address, AccumuloConfiguration configuration)
       throws TTransportException {
    long timeout = configuration.getTimeInMillis(property);
    TTransport transport = ThriftTransportPool.getInstance().getTransport(address, timeout);
    return getClient(factory, address, 0, configuration);
  }

  static public <T extends TServiceClient> T getClient(TServiceClientFactory<T> factory, String address, Property timeoutProperty,
      AccumuloConfiguration configuration) throws TTransportException {
    long timeout = configuration.getTimeInMillis(timeoutProperty);
    TTransport transport = ThriftTransportPool.getInstance().getTransport(address, timeout, SslConnectionParams.forClient(configuration));
     return createClient(factory, transport);
   }
  
  static public <T extends TServiceClient> T getClient(TServiceClientFactory<T> factory, String address, long timeout) throws TTransportException {
    TTransport transport = ThriftTransportPool.getInstance().getTransport(address, timeout);

  static public <T extends TServiceClient> T getClient(TServiceClientFactory<T> factory, String address, long timeout, AccumuloConfiguration configuration)
      throws TTransportException {
    TTransport transport = ThriftTransportPool.getInstance().getTransport(address, timeout, SslConnectionParams.forClient(configuration));
     return createClient(factory, transport);
   }
  

   static public void returnClient(TServiceClient iface) { // Eew... the typing here is horrible
     if (iface != null) {
       ThriftTransportPool.getInstance().returnTransport(iface.getInputProtocol().getTransport());
     }
   }
  

   static public TabletClientService.Client getTServerClient(String address, AccumuloConfiguration conf) throws TTransportException {
     return getClient(new TabletClientService.Client.Factory(), address, Property.GENERAL_RPC_TIMEOUT, conf);
   }
  
  static public TabletClientService.Client getTServerClient(String address, long timeout) throws TTransportException {
    return getClient(new TabletClientService.Client.Factory(), address, timeout);

  static public TabletClientService.Client getTServerClient(String address, AccumuloConfiguration conf, long timeout) throws TTransportException {
    return getClient(new TabletClientService.Client.Factory(), address, timeout, conf);
   }
  

   public static void execute(String address, AccumuloConfiguration conf, ClientExec<TabletClientService.Client> exec) throws AccumuloException,
       AccumuloSecurityException {
     while (true) {
@@ -141,7 +147,7 @@ public class ThriftUtil {
       }
     }
   }
  

   public static <T> T execute(String address, AccumuloConfiguration conf, ClientExecReturn<T,TabletClientService.Client> exec) throws AccumuloException,
       AccumuloSecurityException {
     while (true) {
@@ -161,34 +167,20 @@ public class ThriftUtil {
       }
     }
   }
  

   /**
    * create a transport that is not pooled
    */
   public static TTransport createTransport(HostAndPort address, AccumuloConfiguration conf) throws TException {
    TTransport transport = null;
    
    try {
      transport = TTimeoutTransport.create(address, conf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT));
      transport = ThriftUtil.transportFactory().getTransport(transport);
      transport.open();
      TTransport tmp = transport;
      transport = null;
      return tmp;
    } catch (IOException ex) {
      throw new TTransportException(ex);
    } finally {
      if (transport != null)
        transport.close();
    }
    return createClientTransport(address, (int) conf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT), SslConnectionParams.forClient(conf));
   }
 
   public static TTransportFactory transportFactory() {
     return transportFactory;
   }
  

   private final static Map<Integer,TTransportFactory> factoryCache = new HashMap<Integer,TTransportFactory>();
  

   synchronized public static TTransportFactory transportFactory(int maxFrameSize) {
     TTransportFactory factory = factoryCache.get(maxFrameSize);
     if (factory == null) {
@@ -197,18 +189,59 @@ public class ThriftUtil {
     }
     return factory;
   }
  

   synchronized public static TTransportFactory transportFactory(long maxFrameSize) {
     if (maxFrameSize > Integer.MAX_VALUE || maxFrameSize < 1)
       throw new RuntimeException("Thrift transport frames are limited to " + Integer.MAX_VALUE);
     return transportFactory((int) maxFrameSize);
   }
  

   public static TProtocolFactory protocolFactory() {
     return protocolFactory;
   }
  

  public static TServerSocket getServerSocket(int port, int timeout, InetAddress address, SslConnectionParams params) throws TTransportException {
    if (params.useJsse()) {
      return TSSLTransportFactory.getServerSocket(port, timeout, params.isClientAuth(), address);
    } else {
      return TSSLTransportFactory.getServerSocket(port, timeout, address, params.getTTransportParams());
    }
  }

   public static void close() {
     ThriftTransportPool.close();
   }

  public static TTransport createClientTransport(HostAndPort address, int timeout, SslConnectionParams sslParams) throws TTransportException {
    boolean success = false;
    TTransport transport = null;
    try {
      if (sslParams != null) {
        // TSSLTransportFactory handles timeout 0 -> forever natively
        if (sslParams.useJsse()) {
          transport = TSSLTransportFactory.getClientSocket(address.getHostText(), address.getPort(), timeout);
        } else {
          transport = TSSLTransportFactory.getClientSocket(address.getHostText(), address.getPort(), timeout, sslParams.getTTransportParams());
        }
        // TSSLTransportFactory leaves transports open, so no need to open here
      } else if (timeout == 0) {
        transport = new TSocket(address.getHostText(), address.getPort());
        transport.open();
      } else {
        try {
          transport = TTimeoutTransport.create(address, timeout);
        } catch (IOException ex) {
          throw new TTransportException(ex);
        }
        transport.open();
      }
      transport = ThriftUtil.transportFactory().getTransport(transport);
      success = true;
    } finally {
      if (!success && transport != null) {
        transport.close();
      }
    }
    return transport;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
index 52e1d0499..b51840009 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
@@ -50,7 +50,10 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.SiteConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.data.thrift.TConstraintViolationSummary;
@@ -398,15 +401,25 @@ public class Shell extends ShellOptions {
         instanceName = options.getZooKeeperInstanceName();
         hosts = options.getZooKeeperHosts();
       }
      instance = getZooInstance(instanceName, hosts);
      try {
        instance = getZooInstance(instanceName, hosts, options.getClientConfiguration());
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to load client config from " +  options.getClientConfigFile(), e);
      }
     }
   }
   
  private static Instance getZooInstance(String instanceName, String keepers) {
  /*
   * Takes instanceName and keepers as separate arguments, rather than just packaged into the clientConfig,
   * so that we can fail over to accumulo-site.xml or HDFS config if they're unspecified.
   */
  private static Instance getZooInstance(String instanceName, String keepers, ClientConfiguration clientConfig) {
     UUID instanceId = null;
    if (instanceName == null) {
      instanceName = clientConfig.get(ClientProperty.INSTANCE_NAME);
    }
     if (instanceName == null || keepers == null) {
      @SuppressWarnings("deprecation")
      AccumuloConfiguration conf = AccumuloConfiguration.getSiteConfiguration();
      AccumuloConfiguration conf = SiteConfiguration.getInstance(clientConfig.getAccumuloConfiguration());
       if (instanceName == null) {
         Path instanceDir = new Path(conf.get(Property.INSTANCE_DFS_DIR), "instance_id");
         instanceId = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(instanceDir));
@@ -416,9 +429,9 @@ public class Shell extends ShellOptions {
       }
     }
     if (instanceId != null) {
      return new ZooKeeperInstance(instanceId, keepers);
      return new ZooKeeperInstance(clientConfig.withInstance(instanceId).withZkHosts(keepers));
     } else {
      return new ZooKeeperInstance(instanceName, keepers);
      return new ZooKeeperInstance(clientConfig.withInstance(instanceName).withZkHosts(keepers));
     }
   }
   
diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java b/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java
index cb1f1c855..2f30a8737 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/ShellOptionsJC.java
@@ -25,6 +25,9 @@ import java.util.Scanner;
 import java.util.TreeMap;
 
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
import org.apache.commons.configuration.ConfigurationException;
 import org.apache.log4j.Logger;
 
 import com.beust.jcommander.DynamicParameter;
@@ -161,6 +164,12 @@ public class ShellOptionsJC {
   @Parameter(names = {"-z", "--zooKeeperInstance"}, description = "use a zookeeper instance with the given instance name and list of zoo hosts", arity = 2)
   private List<String> zooKeeperInstance = new ArrayList<String>();
   
  @Parameter(names = {"--ssl"}, description = "use ssl to connect to accumulo")
  private boolean useSsl = false;

  @Parameter(names = "--config-file", description = "read the given client config file.  If omitted, the path searched can be specified with $ACCUMULO_CLIENT_CONF_PATH, which defaults to ~/.accumulo/config:$ACCUMULO_CONF_DIR/client.conf:/etc/accumulo/client.conf")
  private String clientConfigFile = null;

   @Parameter(names = {"-zi", "--zooKeeperInstanceName"}, description="use a zookeeper instance with the given instance name")
   private String zooKeeperInstanceName;
 
@@ -247,4 +256,22 @@ public class ShellOptionsJC {
   public List<String> getUnrecognizedOptions() {
     return unrecognizedOptions;
   }

  public boolean useSsl() {
    return useSsl;
  }

  public String getClientConfigFile() {
    return clientConfigFile;
  }

  public ClientConfiguration getClientConfiguration() throws ConfigurationException,
  FileNotFoundException {
    ClientConfiguration clientConfig = ClientConfiguration.loadDefault(getClientConfigFile());
    if (useSsl()) {
      clientConfig.setProperty(ClientProperty.INSTANCE_RPC_SSL_ENABLED, "true");
    }
    return clientConfig;
  }

 }
diff --git a/core/src/test/java/org/apache/accumulo/core/client/impl/TabletLocatorImplTest.java b/core/src/test/java/org/apache/accumulo/core/client/impl/TabletLocatorImplTest.java
index 8f3fa1dc4..7abacb8eb 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/impl/TabletLocatorImplTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/impl/TabletLocatorImplTest.java
@@ -445,6 +445,7 @@ public class TabletLocatorImplTest extends TestCase {
     }
     
     @Override
    @Deprecated
     public void setConfiguration(AccumuloConfiguration conf) {
       this.conf = conf;
     }
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java
index 62564faba..50fc0a9d9 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/lib/util/ConfiguratorBaseTest.java
@@ -27,6 +27,8 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
 import org.apache.commons.codec.binary.Base64;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.log4j.Level;
@@ -75,16 +77,47 @@ public class ConfiguratorBaseTest {
     assertEquals("file:testFile", conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.ConnectorInfo.TOKEN)));
   }
 
  @SuppressWarnings("deprecation")
   @Test
  public void testSetZooKeeperInstance() {
  public void testSetZooKeeperInstance_legacy() {
     Configuration conf = new Configuration();
     ConfiguratorBase.setZooKeeperInstance(this.getClass(), conf, "testInstanceName", "testZooKeepers");
    assertEquals("testInstanceName", conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.NAME)));
    assertEquals("testZooKeepers", conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.ZOO_KEEPERS)));
    ClientConfiguration clientConf = ClientConfiguration.deserialize(conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.CLIENT_CONFIG)));
    assertEquals("testInstanceName", clientConf.get(ClientProperty.INSTANCE_NAME));
    assertEquals("testZooKeepers", clientConf.get(ClientProperty.INSTANCE_ZK_HOST));
     assertEquals(ZooKeeperInstance.class.getSimpleName(), conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.TYPE)));
    // TODO uncomment this after ACCUMULO-1699
    // Instance instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    // assertEquals(ZooKeeperInstance.class.getName(), instance.getClass().getName());

    Instance instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    assertEquals(ZooKeeperInstance.class.getName(), instance.getClass().getName());
    assertEquals("testInstanceName", ((ZooKeeperInstance)instance).getInstanceName());
    assertEquals("testZooKeepers", ((ZooKeeperInstance)instance).getZooKeepers());

    // Also make sure we can still deserialize job configurations with the old keys
    conf = new Configuration();
    conf.set(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.TYPE), ZooKeeperInstance.class.getSimpleName());
    conf.set(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.NAME), "testInstanceName");
    conf.set(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.ZOO_KEEPERS), "testZooKeepers");
    instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    assertEquals("testInstanceName", ((ZooKeeperInstance)instance).getInstanceName());
    assertEquals("testZooKeepers", ((ZooKeeperInstance)instance).getZooKeepers());
  }

  @Test
  public void testSetZooKeeperInstance() {
    Configuration conf = new Configuration();
    ConfiguratorBase.setZooKeeperInstance(this.getClass(), conf, new ClientConfiguration().withInstance("testInstanceName").withZkHosts("testZooKeepers").withSsl(true).withZkTimeout(1234));
    ClientConfiguration clientConf = ClientConfiguration.deserialize(conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.CLIENT_CONFIG)));
    assertEquals("testInstanceName", clientConf.get(ClientProperty.INSTANCE_NAME));
    assertEquals("testZooKeepers", clientConf.get(ClientProperty.INSTANCE_ZK_HOST));
    assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SSL_ENABLED));
    assertEquals("1234", clientConf.get(ClientProperty.INSTANCE_ZK_TIMEOUT));
    assertEquals(ZooKeeperInstance.class.getSimpleName(), conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.TYPE)));

    Instance instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    assertEquals(ZooKeeperInstance.class.getName(), instance.getClass().getName());
    assertEquals("testInstanceName", ((ZooKeeperInstance)instance).getInstanceName());
    assertEquals("testZooKeepers", ((ZooKeeperInstance)instance).getZooKeepers());
    assertEquals(1234000, ((ZooKeeperInstance)instance).getZooKeepersSessionTimeOut());
   }
 
   @Test
diff --git a/core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java b/core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java
new file mode 100644
index 000000000..55cf9d3a1
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/conf/ClientConfigurationTest.java
@@ -0,0 +1,65 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.conf;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

public class ClientConfigurationTest {
  @Test
  public void testOverrides() throws Exception {
    ClientConfiguration clientConfig = createConfig();
    assertExpectedConfig(clientConfig);
  }

  @Test
  public void testSerialization() throws Exception {
    ClientConfiguration clientConfig = createConfig();
    // sanity check that we're starting with what we're expecting
    assertExpectedConfig(clientConfig);

    String serialized = clientConfig.serialize();
    ClientConfiguration deserializedClientConfig = ClientConfiguration.deserialize(serialized);
    assertExpectedConfig(deserializedClientConfig);
  }

  private void assertExpectedConfig(ClientConfiguration clientConfig) {
    assertEquals("firstZkHosts", clientConfig.get(ClientProperty.INSTANCE_ZK_HOST));
    assertEquals("secondInstanceName", clientConfig.get(ClientProperty.INSTANCE_NAME));
    assertEquals("123s", clientConfig.get(ClientProperty.INSTANCE_ZK_TIMEOUT));
    assertEquals(ClientProperty.RPC_SSL_TRUSTSTORE_TYPE.getDefaultValue(), clientConfig.get(ClientProperty.RPC_SSL_TRUSTSTORE_TYPE));
  }

  private ClientConfiguration createConfig() {
    Configuration first = new PropertiesConfiguration();
    first.addProperty(ClientProperty.INSTANCE_ZK_HOST.getKey(), "firstZkHosts");
    Configuration second = new PropertiesConfiguration();
    second.addProperty(ClientProperty.INSTANCE_ZK_HOST.getKey(), "secondZkHosts");
    second.addProperty(ClientProperty.INSTANCE_NAME.getKey(), "secondInstanceName");
    Configuration third = new PropertiesConfiguration();
    third.addProperty(ClientProperty.INSTANCE_ZK_HOST.getKey(), "thirdZkHosts");
    third.addProperty(ClientProperty.INSTANCE_NAME.getKey(), "thirdInstanceName");
    third.addProperty(ClientProperty.INSTANCE_ZK_TIMEOUT.getKey(), "123s");
    return new ClientConfiguration(Arrays.asList(first, second, third));
  }
}
diff --git a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
index 23ca13a7b..5ce132043 100644
-- a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
++ b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
@@ -38,10 +38,14 @@ import jline.console.ConsoleReader;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration;
import org.apache.accumulo.core.conf.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.conf.ConfigSanityCheck;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.hadoop.fs.Path;
 import org.apache.log4j.Level;
import org.easymock.EasyMock;
 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;
@@ -51,7 +55,7 @@ import org.powermock.core.classloader.annotations.PrepareForTest;
 import org.powermock.modules.junit4.PowerMockRunner;
 
 @RunWith(PowerMockRunner.class)
@PrepareForTest({Shell.class, AccumuloConfiguration.class, ZooUtil.class})
@PrepareForTest({Shell.class, ZooUtil.class, ConfigSanityCheck.class})
 public class ShellSetInstanceTest {
   public static class TestOutputStream extends OutputStream {
     StringBuilder sb = new StringBuilder();
@@ -121,38 +125,49 @@ public class ShellSetInstanceTest {
     testSetInstance_HdfsZooInstance(false, false, false);
   }
   
  @SuppressWarnings("deprecation")
   private void testSetInstance_HdfsZooInstance(boolean explicitHdfs, boolean onlyInstance, boolean onlyHosts)
     throws Exception {
    ClientConfiguration clientConf = createMock(ClientConfiguration.class);
     ShellOptionsJC opts = createMock(ShellOptionsJC.class);
     expect(opts.isFake()).andReturn(false);
    expect(opts.getClientConfiguration()).andReturn(clientConf);
     expect(opts.isHdfsZooInstance()).andReturn(explicitHdfs);
     if (!explicitHdfs) {
       expect(opts.getZooKeeperInstance())
         .andReturn(Collections.<String>emptyList());
       if (onlyInstance) {
         expect(opts.getZooKeeperInstanceName()).andReturn("instance");
        expect(clientConf.withInstance("instance")).andReturn(clientConf);
       } else {
         expect(opts.getZooKeeperInstanceName()).andReturn(null);
       }
       if (onlyHosts) {
         expect(opts.getZooKeeperHosts()).andReturn("host3,host4");
        expect(clientConf.withZkHosts("host3,host4")).andReturn(clientConf);
       } else {
         expect(opts.getZooKeeperHosts()).andReturn(null);
       }
     }
     replay(opts);
 
    if (!onlyInstance) {
      expect(clientConf.get(ClientProperty.INSTANCE_NAME)).andReturn(null);
    }

     AccumuloConfiguration conf = createMock(AccumuloConfiguration.class);
    mockStatic(AccumuloConfiguration.class);
    expect(AccumuloConfiguration.getSiteConfiguration()).andReturn(conf);
    replay(AccumuloConfiguration.class);
    expect(clientConf.getAccumuloConfiguration()).andReturn(conf);

    mockStatic(ConfigSanityCheck.class);
    ConfigSanityCheck.validate(EasyMock.<AccumuloConfiguration>anyObject());
    expectLastCall();
    replay(ConfigSanityCheck.class);
 
     if (!onlyHosts) {
      expect(conf.get(Property.INSTANCE_ZK_HOST)).andReturn("host1,host2");
      expect(conf.get(Property.INSTANCE_ZK_HOST)).andReturn("host1,host2").atLeastOnce();
      expect(clientConf.withZkHosts("host1,host2")).andReturn(clientConf);
     }
     if (!onlyInstance) {
      expect(conf.get(Property.INSTANCE_DFS_DIR)).andReturn("/dfs");
      expect(conf.get(Property.INSTANCE_DFS_DIR)).andReturn("/dfs").atLeastOnce();
     }
     replay(conf);
     UUID randomUUID = null;
@@ -162,21 +177,13 @@ public class ShellSetInstanceTest {
       expect(ZooUtil.getInstanceIDFromHdfs(anyObject(Path.class)))
         .andReturn(randomUUID.toString());
       replay(ZooUtil.class);
      expect(clientConf.withInstance(randomUUID)).andReturn(clientConf);
     }
    replay(clientConf);
 
     ZooKeeperInstance theInstance = createMock(ZooKeeperInstance.class);
     
    String expectedHosts = "host1,host2";
    if (onlyHosts)
      expectedHosts = "host3,host4";

    if (!onlyInstance) {
      expectNew(ZooKeeperInstance.class, randomUUID, expectedHosts)
        .andReturn(theInstance);
    } else {
      expectNew(ZooKeeperInstance.class, "instance", expectedHosts)
        .andReturn(theInstance);
    }
    expectNew(ZooKeeperInstance.class, clientConf).andReturn(theInstance);
     replay(theInstance, ZooKeeperInstance.class);
 
     shell.setInstance(opts);
@@ -191,28 +198,31 @@ public class ShellSetInstanceTest {
     testSetInstance_ZKInstance(false);
   }
   private void testSetInstance_ZKInstance(boolean dashZ) throws Exception {
    ClientConfiguration clientConf = createMock(ClientConfiguration.class);
     ShellOptionsJC opts = createMock(ShellOptionsJC.class);
     expect(opts.isFake()).andReturn(false);
    expect(opts.getClientConfiguration()).andReturn(clientConf);
     expect(opts.isHdfsZooInstance()).andReturn(false);
     if (dashZ) {
      expect(clientConf.withInstance("foo")).andReturn(clientConf);
      expect(clientConf.withZkHosts("host1,host2")).andReturn(clientConf);
       List<String> zl = new java.util.ArrayList<String>();
       zl.add("foo");
       zl.add("host1,host2");
       expect(opts.getZooKeeperInstance()).andReturn(zl);
       expectLastCall().anyTimes();
     } else {
      expect(clientConf.withInstance("bar")).andReturn(clientConf);
      expect(clientConf.withZkHosts("host3,host4")).andReturn(clientConf);
       expect(opts.getZooKeeperInstance()).andReturn(Collections.<String>emptyList());
       expect(opts.getZooKeeperInstanceName()).andReturn("bar");
       expect(opts.getZooKeeperHosts()).andReturn("host3,host4");
     }
    replay(clientConf);
     replay(opts);
 
     ZooKeeperInstance theInstance = createMock(ZooKeeperInstance.class);
    if (dashZ)
      expectNew(ZooKeeperInstance.class, "foo", "host1,host2").andReturn(theInstance);
    else
      expectNew(ZooKeeperInstance.class, "bar", "host3,host4")
      .andReturn(theInstance);
    expectNew(ZooKeeperInstance.class, clientConf).andReturn(theInstance);
     replay(theInstance, ZooKeeperInstance.class);
 
     shell.setInstance(opts);
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java
index ecb42c767..a66438eeb 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/filedata/FileDataQuery.java
@@ -28,6 +28,7 @@ import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
@@ -46,7 +47,7 @@ public class FileDataQuery {
   
   public FileDataQuery(String instanceName, String zooKeepers, String user, AuthenticationToken token, String tableName, Authorizations auths) throws AccumuloException,
       AccumuloSecurityException, TableNotFoundException {
    ZooKeeperInstance instance = new ZooKeeperInstance(instanceName, zooKeepers);
    ZooKeeperInstance instance = new ZooKeeperInstance(ClientConfiguration.loadDefault().withInstance(instanceName).withZkHosts(zooKeepers));
     conn = instance.getConnector(user, token);
     lastRefs = new ArrayList<Entry<Key,Value>>();
     cis = new ChunkInputStream();
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java
index 16e0356e2..1114a7e9f 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/mapreduce/TokenFileWordCount.java
@@ -19,6 +19,7 @@ package org.apache.accumulo.examples.simple.mapreduce;
 import java.io.IOException;
 
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.util.CachedConfiguration;
@@ -82,7 +83,7 @@ public class TokenFileWordCount extends Configured implements Tool {
     job.setOutputValueClass(Mutation.class);
     
     // AccumuloInputFormat not used here, but it uses the same functions.
    AccumuloOutputFormat.setZooKeeperInstance(job, instance, zookeepers);
    AccumuloOutputFormat.setZooKeeperInstance(job, ClientConfiguration.loadDefault().withInstance(instance).withZkHosts(zookeepers));
     AccumuloOutputFormat.setConnectorInfo(job, user, tokenFile);
     AccumuloOutputFormat.setCreateTables(job, true);
     AccumuloOutputFormat.setDefaultTableName(job, tableName);
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
index 42882bb82..8e7e2ba73 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
@@ -48,6 +48,7 @@ import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.master.thrift.MasterGoalState;
 import org.apache.accumulo.core.util.Daemon;
@@ -60,6 +61,7 @@ import org.apache.accumulo.server.init.Initialize;
 import org.apache.accumulo.server.util.PortUtils;
 import org.apache.accumulo.server.util.time.SimpleTimer;
 import org.apache.accumulo.start.Main;
import org.apache.commons.configuration.MapConfiguration;
 import org.apache.accumulo.start.classloader.vfs.MiniDFSUtil;
 import org.apache.accumulo.tserver.TabletServer;
 import org.apache.commons.io.FileUtils;
@@ -71,6 +73,9 @@ import org.apache.hadoop.hdfs.DFSConfigKeys;
 import org.apache.hadoop.hdfs.MiniDFSCluster;
 import org.apache.zookeeper.server.ZooKeeperServerMain;
 
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

 /**
  * A utility class that will create Zookeeper and Accumulo processes that write all of their data to a single local directory. This class makes it easy to test
  * code against a real Accumulo instance. Its much more accurate for testing than {@link org.apache.accumulo.core.client.mock.MockAccumulo}, but much slower.
@@ -232,6 +237,9 @@ public class MiniAccumuloCluster {
     argList.addAll(Arrays.asList(javaBin, "-Dproc=" + clazz.getSimpleName(), "-cp", classpath));
     argList.add("-Djava.library.path=" + config.getLibDir());
     argList.addAll(extraJvmOpts);
    for (Entry<String,String> sysProp : config.getSystemProperties().entrySet()) {
      argList.add(String.format("-D%s=%s", sysProp.getKey(), sysProp.getValue()));
    }
     argList.addAll(Arrays.asList("-XX:+UseConcMarkSweepGC", "-XX:CMSInitiatingOccupancyFraction=75", Main.class.getName(), className));
     argList.addAll(Arrays.asList(args));
 
@@ -239,6 +247,15 @@ public class MiniAccumuloCluster {
 
     builder.environment().put("ACCUMULO_HOME", config.getDir().getAbsolutePath());
     builder.environment().put("ACCUMULO_LOG_DIR", config.getLogDir().getAbsolutePath());
    builder.environment().put("ACCUMULO_CLIENT_CONF_PATH", config.getClientConfFile().getAbsolutePath());

    // if we're running under accumulo.start, we forward these env vars
    String env = System.getenv("HADOOP_PREFIX");
    if (env != null)
      builder.environment().put("HADOOP_PREFIX", env);
    env = System.getenv("ZOOKEEPER_HOME");
    if (env != null)
      builder.environment().put("ZOOKEEPER_HOME", env);
     builder.environment().put("ACCUMULO_CONF_DIR", config.getConfDir().getAbsolutePath());
     // hadoop-2.2 puts error messages in the logs if this is not set
     builder.environment().put("HADOOP_HOME", config.getDir().getAbsolutePath());
@@ -333,19 +350,19 @@ public class MiniAccumuloCluster {
       dfsUri = "file://";
     }
 
    File clientConfFile = config.getClientConfFile();
    // Write only the properties that correspond to ClientConfiguration properties
    writeConfigProperties(clientConfFile, Maps.filterEntries(config.getSiteConfig(), new Predicate<Entry<String,String>>() {
      public boolean apply(Entry<String,String> v) {
        return ClientConfiguration.ClientProperty.getPropertyByKey(v.getKey()) != null;
      }
    }));

     File siteFile = new File(config.getConfDir(), "accumulo-site.xml");
     writeConfig(siteFile, config.getSiteConfig().entrySet());
 
    FileWriter fileWriter = new FileWriter(siteFile);
    fileWriter.append("<configuration>\n");

    for (Entry<String,String> entry : config.getSiteConfig().entrySet())
      fileWriter.append("<property><name>" + entry.getKey() + "</name><value>" + entry.getValue() + "</value></property>\n");
    fileWriter.append("</configuration>\n");
    fileWriter.close();

     zooCfgFile = new File(config.getConfDir(), "zoo.cfg");
    fileWriter = new FileWriter(zooCfgFile);
    FileWriter fileWriter = new FileWriter(zooCfgFile);
 
     // zookeeper uses Properties to read its config, so use that to write in order to properly escape things like Windows paths
     Properties zooCfg = new Properties();
@@ -383,6 +400,13 @@ public class MiniAccumuloCluster {
     fileWriter.append("</configuration>\n");
     fileWriter.close();
   }
  private void writeConfigProperties(File file, Map<String,String> settings) throws IOException {
    FileWriter fileWriter = new FileWriter(file);

    for (Entry<String,String> entry : settings.entrySet())
      fileWriter.append(entry.getKey() + "=" + entry.getValue() + "\n");
    fileWriter.close();
  }
 
   /**
    * Starts Accumulo and Zookeeper processes. Can only be called once.
@@ -597,10 +621,14 @@ public class MiniAccumuloCluster {
    * @since 1.6.0
    */
   public Connector getConnector(String user, String passwd) throws AccumuloException, AccumuloSecurityException {
    Instance instance = new ZooKeeperInstance(this.getInstanceName(), this.getZooKeepers());
    Instance instance = new ZooKeeperInstance(getClientConfig());
     return instance.getConnector(user, new PasswordToken(passwd));
   }
 
  public ClientConfiguration getClientConfig() {
    return new ClientConfiguration(Arrays.asList(new MapConfiguration(config.getSiteConfig()))).withInstance(this.getInstanceName()).withZkHosts(this.getZooKeepers());
  }

   public FileSystem getFileSystem() {
     try {
       return FileSystem.get(new URI(dfsUri), new Configuration());
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloConfig.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloConfig.java
index bfa79221d..5c7050aab 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloConfig.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloConfig.java
@@ -39,8 +39,8 @@ public class MiniAccumuloConfig {
   private Map<String,String> siteConfig = new HashMap<String,String>();
   private int numTservers = 2;
   private Map<ServerType,Long> memoryConfig = new HashMap<ServerType,Long>();

   private boolean jdwpEnabled = false;
  private Map<String,String> systemProperties = new HashMap<String,String>();
 
   private String instanceName = "miniInstance";
 
@@ -359,6 +359,15 @@ public class MiniAccumuloConfig {
     this.useMiniDFS = useMiniDFS;
   }
 
  /**
   * @return location of client conf file containing connection parameters for connecting to this minicluster
   *
   * @since 1.6.0
   */
  public File getClientConfFile() {
    return new File(getConfDir(), "client.conf");
  }

   /**
    * Whether or not the Accumulo garbage collector proces will run
    */
@@ -376,6 +385,24 @@ public class MiniAccumuloConfig {
   }
 
   /**
   * @return sets system properties set for service processes
   *
   * @since 1.6.0
   */
  public void setSystemProperties(Map<String,String> systemProperties) {
    this.systemProperties = new HashMap<String,String>(systemProperties);
  }

  /**
   * @return a copy of the system properties for service processes
   *
   * @since 1.6.0
   */
  public Map<String,String> getSystemProperties() {
    return new HashMap<String,String>(systemProperties);
  }

  /*
    * Gets the classpath elements to use when spawning processes.
    * 
    * @return the classpathItems, if set
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
index 540d7aea6..1e1c46422 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloInstance.java
@@ -17,11 +17,15 @@
 package org.apache.accumulo.minicluster;
 
 import java.io.File;
import java.io.FileNotFoundException;
 import java.net.MalformedURLException;
 
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
 import org.apache.hadoop.conf.Configuration;
 
 /**
@@ -32,11 +36,20 @@ public class MiniAccumuloInstance extends ZooKeeperInstance {
   /**
    * Construct an {@link Instance} entry point to Accumulo using a {@link MiniAccumuloCluster} directory
    */
  public MiniAccumuloInstance(String instanceName, File directory) {
    super(instanceName, getZooKeepersFromDir(directory));
  public MiniAccumuloInstance(String instanceName, File directory) throws FileNotFoundException {
    super(new ClientConfiguration(getConfigProperties(directory)).withInstance(instanceName).withZkHosts(getZooKeepersFromDir(directory)));
  }

  public static PropertiesConfiguration getConfigProperties(File directory) {
    try {
      return new PropertiesConfiguration(new File(new File(directory, "conf"), "client.conf"));
    } catch (ConfigurationException e) {
      // this should never happen since we wrote the config file ourselves
      throw new IllegalArgumentException(e);
    }
   }
   
  private static String getZooKeepersFromDir(File directory) {
  private static String getZooKeepersFromDir(File directory) throws FileNotFoundException {
     if (!directory.isDirectory())
       throw new IllegalArgumentException("Not a directory " + directory.getPath());
     File configFile = new File(new File(directory, "conf"), "accumulo-site.xml");
@@ -44,7 +57,7 @@ public class MiniAccumuloInstance extends ZooKeeperInstance {
     try {
       conf.addResource(configFile.toURI().toURL());
     } catch (MalformedURLException e) {
      throw new IllegalStateException("Missing file: " + configFile.getPath());
      throw new FileNotFoundException("Missing file: " + configFile.getPath());
     }
     return conf.get(Property.INSTANCE_ZK_HOST.getKey());
   }
diff --git a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java
index 281e8052b..a29bbc0e8 100644
-- a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java
++ b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java
@@ -69,7 +69,6 @@ public class MiniAccumuloClusterGCTest {
     }
   }
 
  
   private static File testDir = new File(System.getProperty("user.dir") + "/target/" + MiniAccumuloClusterGCTest.class.getName());
   private static MiniAccumuloConfig macConfig;
   private static MiniAccumuloCluster accumulo;
@@ -103,7 +102,7 @@ public class MiniAccumuloClusterGCTest {
   // This test seems to be a little too unstable for a unit test
   @Ignore
   public void test() throws Exception {
    ZooKeeperInstance inst = new ZooKeeperInstance(accumulo.getInstanceName(), accumulo.getZooKeepers());
    ZooKeeperInstance inst = new ZooKeeperInstance(accumulo.getClientConfig());
     Connector c = inst.getConnector("root", new PasswordToken(passwd));
 
     final String table = "foobar";
diff --git a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java
index 26a154623..5de981dad 100644
-- a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java
++ b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java
@@ -31,7 +31,6 @@ import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
@@ -86,8 +85,7 @@ public class MiniAccumuloClusterTest {
 
   @Test(timeout = 30000)
   public void test() throws Exception {
    Connector conn = new ZooKeeperInstance(accumulo.getConfig().getInstanceName(), accumulo.getConfig().getZooKeepers()).getConnector("root",
        new PasswordToken("superSecret"));
    Connector conn = accumulo.getConnector("root", "superSecret");
 
     conn.tableOperations().create("table1");
 
@@ -102,8 +100,7 @@ public class MiniAccumuloClusterTest {
 
     conn.tableOperations().attachIterator("table1", is);
 
    Connector uconn = new ZooKeeperInstance(accumulo.getConfig().getInstanceName(), accumulo.getConfig().getZooKeepers()).getConnector("user1",
        new PasswordToken("pass1"));
    Connector uconn = accumulo.getConnector("user1", "pass1");
 
     BatchWriter bw = uconn.createBatchWriter("table1", new BatchWriterConfig());
 
@@ -162,8 +159,7 @@ public class MiniAccumuloClusterTest {
   @Test(timeout = 60000)
   public void testPerTableClasspath() throws Exception {
 
    Connector conn = new ZooKeeperInstance(accumulo.getConfig().getInstanceName(), accumulo.getConfig().getZooKeepers()).getConnector("root",
        new PasswordToken("superSecret"));
    Connector conn = accumulo.getConnector("root", "superSecret");
 
     conn.tableOperations().create("table2");
 
diff --git a/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java b/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
index c92f73b0a..d979d40f1 100644
-- a/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
++ b/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
@@ -61,6 +61,7 @@ import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.ConditionalMutation;
 import org.apache.accumulo.core.data.Key;
@@ -175,7 +176,7 @@ public class ProxyServer implements AccumuloProxy.Iface {
     if (useMock != null && Boolean.parseBoolean(useMock))
       instance = new MockInstance();
     else
      instance = new ZooKeeperInstance(props.getProperty("instance"), props.getProperty("zookeepers"));
      instance = new ZooKeeperInstance(ClientConfiguration.loadDefault().withInstance(props.getProperty("instance")).withZkHosts(props.getProperty("zookeepers")));
     
     try {
       String tokenProp = props.getProperty("tokenClass", PasswordToken.class.getName());
diff --git a/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnDefaultTable.java b/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnDefaultTable.java
index 53f5ac259..588c35cb8 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnDefaultTable.java
++ b/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnDefaultTable.java
@@ -36,7 +36,7 @@ public class ClientOnDefaultTable extends org.apache.accumulo.core.cli.ClientOnD
     if (instance == null) {
       return cachedInstance = HdfsZooInstance.getInstance();
     }
    return cachedInstance = new ZooKeeperInstance(this.instance, this.zookeepers);
    return cachedInstance = new ZooKeeperInstance(this.getClientConfiguration());
   }
   public ClientOnDefaultTable(String table) {
     super(table);
diff --git a/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnRequiredTable.java b/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnRequiredTable.java
index e9e9bf126..f2e04e42c 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnRequiredTable.java
++ b/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOnRequiredTable.java
@@ -36,6 +36,6 @@ public class ClientOnRequiredTable extends org.apache.accumulo.core.cli.ClientOn
     if (instance == null) {
       return cachedInstance = HdfsZooInstance.getInstance();
     }
    return cachedInstance = new ZooKeeperInstance(this.instance, this.zookeepers);
    return cachedInstance = new ZooKeeperInstance(getClientConfiguration());
   }
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOpts.java b/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOpts.java
index 6f3516a43..c19b7b069 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOpts.java
++ b/server/base/src/main/java/org/apache/accumulo/server/cli/ClientOpts.java
@@ -34,6 +34,6 @@ public class ClientOpts extends org.apache.accumulo.core.cli.ClientOpts {
     if (instance == null) {
       return HdfsZooInstance.getInstance();
     }
    return new ZooKeeperInstance(this.instance, this.zookeepers);
    return new ZooKeeperInstance(this.getClientConfiguration());
   }
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java b/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java
index 606941d41..ecbe0c4a3 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java
++ b/server/base/src/main/java/org/apache/accumulo/server/client/BulkImporter.java
@@ -585,7 +585,7 @@ public class BulkImporter {
       throws AccumuloException, AccumuloSecurityException {
     try {
       long timeInMillis = instance.getConfiguration().getTimeInMillis(Property.TSERV_BULK_TIMEOUT);
      TabletClientService.Iface client = ThriftUtil.getTServerClient(location, timeInMillis);
      TabletClientService.Iface client = ThriftUtil.getTServerClient(location, instance.getConfiguration(), timeInMillis);
       try {
         HashMap<KeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>> files = new HashMap<KeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>>();
         for (Entry<KeyExtent,List<PathSize>> entry : assignmentsPerTablet.entrySet()) {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java b/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
index 9e6bbe78f..00714f931 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
++ b/server/base/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
@@ -175,6 +175,7 @@ public class HdfsZooInstance implements Instance {
   }
   
   @Override
  @Deprecated
   public void setConfiguration(AccumuloConfiguration conf) {
     this.conf = conf;
   }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java b/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java
index 8abd10420..eec64efce 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/TServerUtils.java
@@ -18,6 +18,7 @@ package org.apache.accumulo.server.util;
 
 import java.io.IOException;
 import java.lang.reflect.Field;
import java.net.BindException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.ServerSocket;
@@ -33,6 +34,7 @@ import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.util.Daemon;
 import org.apache.accumulo.core.util.LoggingRunnable;
 import org.apache.accumulo.core.util.SimpleThreadPool;
import org.apache.accumulo.core.util.SslConnectionParams;
 import org.apache.accumulo.core.util.TBufferedSocket;
 import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.accumulo.core.util.UtilWaitThread;
@@ -47,6 +49,7 @@ import org.apache.thrift.server.TServer;
 import org.apache.thrift.server.TThreadPoolServer;
 import org.apache.thrift.transport.TNonblockingSocket;
 import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
 import org.apache.thrift.transport.TTransport;
 import org.apache.thrift.transport.TTransportException;
 
@@ -54,19 +57,19 @@ import com.google.common.net.HostAndPort;
 
 public class TServerUtils {
   private static final Logger log = Logger.getLogger(TServerUtils.class);
  

   public static final ThreadLocal<String> clientAddress = new ThreadLocal<String>();
  

   public static class ServerAddress {
     public final TServer server;
     public final HostAndPort address;
    

     public ServerAddress(TServer server, HostAndPort address) {
       this.server = server;
       this.address = address;
     }
   }
  

   /**
    * Start a server, at the given port, or higher, if that port is not available.
    * 
@@ -81,6 +84,7 @@ public class TServerUtils {
    * @param portSearchProperty
    * @param minThreadProperty
    * @param timeBetweenThreadChecksProperty
   * @param generalSslEnabled
    * @return the server object created, and the port actually used
    * @throws UnknownHostException
    *           when we don't know our own address
@@ -105,12 +109,12 @@ public class TServerUtils {
     TServerUtils.TimedProcessor timedProcessor = new TServerUtils.TimedProcessor(processor, serverName, threadName);
     Random random = new Random();
     for (int j = 0; j < 100; j++) {
      

       // Are we going to slide around, looking for an open port?
       int portsToSearch = 1;
       if (portSearch)
         portsToSearch = 1000;
      

       for (int i = 0; i < portsToSearch; i++) {
         int port = portHint + i;
         if (portHint != 0 && i > 0)
@@ -119,22 +123,34 @@ public class TServerUtils {
           port = 1024 + port % (65535 - 1024);
         try {
           HostAndPort addr = HostAndPort.fromParts(address, port);
          return TServerUtils.startTServer(addr, timedProcessor, serverName, threadName, minThreads, timeBetweenThreadChecks, maxMessageSize);
        } catch (Exception ex) {
          log.info("Unable to use port " + port + ", retrying. (Thread Name = " + threadName + ")");
          UtilWaitThread.sleep(250);
          return TServerUtils.startTServer(addr, timedProcessor, serverName, threadName, minThreads, timeBetweenThreadChecks, maxMessageSize,
              SslConnectionParams.forServer(conf), conf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT));
        } catch (TTransportException ex) {
          log.error("Unable to start TServer", ex);
          if (ex.getCause() == null || ex.getCause().getClass() == BindException.class) {
            // Note: with a TNonblockingServerSocket a "port taken" exception is a cause-less
            // TTransportException, and with a TSocket created by TSSLTransportFactory, it
            // comes through as caused by a BindException.
            log.info("Unable to use port " + port + ", retrying. (Thread Name = " + threadName + ")");
            UtilWaitThread.sleep(250);
          } else {
            // thrift is passing up a nested exception that isn't a BindException,
            // so no reason to believe retrying on a different port would help.
            log.error("Unable to start TServer", ex);
            break;
          }
         }
       }
     }
     throw new UnknownHostException("Unable to find a listen port");
   }
  

   public static class TimedProcessor implements TProcessor {
    

     final TProcessor other;
     ThriftMetrics metrics = null;
     long idleStart = 0;
    

     TimedProcessor(TProcessor next, String serverName, String threadName) {
       this.other = next;
       // Register the metrics MBean
@@ -146,7 +162,7 @@ public class TServerUtils {
       }
       idleStart = System.currentTimeMillis();
     }
    

     @Override
     public boolean process(TProtocol in, TProtocol out) throws TException {
       long now = 0;
@@ -169,41 +185,46 @@ public class TServerUtils {
       }
     }
   }
  

   public static class ClientInfoProcessorFactory extends TProcessorFactory {
    

     public ClientInfoProcessorFactory(TProcessor processor) {
       super(processor);
     }
    

     @Override
     public TProcessor getProcessor(TTransport trans) {
       if (trans instanceof TBufferedSocket) {
         TBufferedSocket tsock = (TBufferedSocket) trans;
         clientAddress.set(tsock.getClientString());
      } else if (trans instanceof TSocket) {
        TSocket tsock = (TSocket) trans;
        clientAddress.set(tsock.getSocket().getInetAddress().getHostAddress() + ":" + tsock.getSocket().getPort());
      } else {
        log.warn("Unable to extract clientAddress from transport of type " + trans.getClass());
       }
       return super.getProcessor(trans);
     }
   }
  

   public static class THsHaServer extends org.apache.thrift.server.THsHaServer {
     public THsHaServer(Args args) {
       super(args);
     }
    

     @Override
     protected Runnable getRunnable(FrameBuffer frameBuffer) {
       return new Invocation(frameBuffer);
     }
    

     private class Invocation implements Runnable {
      

       private final FrameBuffer frameBuffer;
      

       public Invocation(final FrameBuffer frameBuffer) {
         this.frameBuffer = frameBuffer;
       }
      

       @Override
       public void run() {
         if (frameBuffer.trans_ instanceof TNonblockingSocket) {
@@ -215,24 +236,10 @@ public class TServerUtils {
       }
     }
   }
  
  public static ServerAddress startHsHaServer(HostAndPort address, TProcessor processor, final String serverName, String threadName, final int numThreads,

  public static ServerAddress createHsHaServer(HostAndPort address, TProcessor processor, final String serverName, String threadName, final int numThreads,
       long timeBetweenThreadChecks, long maxMessageSize) throws TTransportException {
     TNonblockingServerSocket transport = new TNonblockingServerSocket(new InetSocketAddress(address.getHostText(), address.getPort()));
    // check for the special "bind to everything address"
    String hostname = address.getHostText();
    if (hostname.equals("0.0.0.0")) {
      // can't get the address from the bind, so we'll do our best to invent our hostname
      try {
        hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        throw new TTransportException(e);
      }
    }
    int port = address.getPort();
    if (port == 0) {
      port = transport.getPort();
    }
     THsHaServer.Args options = new THsHaServer.Args(transport);
     options.protocolFactory(ThriftUtil.protocolFactory());
     options.transportFactory(ThriftUtil.transportFactory(maxMessageSize));
@@ -267,12 +274,15 @@ public class TServerUtils {
     }, timeBetweenThreadChecks, timeBetweenThreadChecks);
     options.executorService(pool);
     options.processorFactory(new TProcessorFactory(processor));
    return new ServerAddress(new THsHaServer(options), HostAndPort.fromParts(hostname, port));
    if (address.getPort() == 0) {
      address = HostAndPort.fromParts(address.getHostText(), transport.getPort());
    }
    return new ServerAddress(new THsHaServer(options), address);
   }
  
  public static ServerAddress startThreadPoolServer(HostAndPort address, TProcessor processor, String serverName, String threadName, int numThreads)

  public static ServerAddress createThreadPoolServer(HostAndPort address, TProcessor processor, String serverName, String threadName, int numThreads)
       throws TTransportException {
    

     // if port is zero, then we must bind to get the port number
     ServerSocket sock;
     try {
@@ -284,23 +294,52 @@ public class TServerUtils {
       throw new TTransportException(ex);
     }
     TServerTransport transport = new TBufferedServerSocket(sock, 32 * 1024);
    return new ServerAddress(createThreadPoolServer(transport, processor), address);
  }

  public static TServer createThreadPoolServer(TServerTransport transport, TProcessor processor) {
     TThreadPoolServer.Args options = new TThreadPoolServer.Args(transport);
     options.protocolFactory(ThriftUtil.protocolFactory());
     options.transportFactory(ThriftUtil.transportFactory());
     options.processorFactory(new ClientInfoProcessorFactory(processor));
    return new ServerAddress(new TThreadPoolServer(options), address);
    return new TThreadPoolServer(options);
   }
  
  public static ServerAddress startTServer(HostAndPort address, TProcessor processor, String serverName, String threadName, int numThreads, long timeBetweenThreadChecks, long maxMessageSize)

  public static ServerAddress createSslThreadPoolServer(HostAndPort address, TProcessor processor, long socketTimeout, SslConnectionParams sslParams)
       throws TTransportException {
    return startTServer(address, new TimedProcessor(processor, serverName, threadName), serverName, threadName, numThreads, timeBetweenThreadChecks, maxMessageSize);
    org.apache.thrift.transport.TServerSocket transport;
    try {
      transport = ThriftUtil.getServerSocket(address.getPort(), (int) socketTimeout, InetAddress.getByName(address.getHostText()), sslParams);
    } catch (UnknownHostException e) {
      throw new TTransportException(e);
    }
    if (address.getPort() == 0) {
      address = HostAndPort.fromParts(address.getHostText(), transport.getServerSocket().getLocalPort());
    }
    return new ServerAddress(createThreadPoolServer(transport, processor), address);
   }
  
  public static ServerAddress startTServer(HostAndPort address, TimedProcessor processor, String serverName, String threadName, int numThreads, long timeBetweenThreadChecks, long maxMessageSize)
      throws TTransportException {
    ServerAddress result = startHsHaServer(address, processor, serverName, threadName, numThreads, timeBetweenThreadChecks, maxMessageSize);
    //ServerAddress result = startThreadPoolServer(address, processor, serverName, threadName, -1);
    final TServer finalServer = result.server;

  public static ServerAddress startTServer(HostAndPort address, TProcessor processor, String serverName, String threadName, int numThreads,
      long timeBetweenThreadChecks, long maxMessageSize) throws TTransportException {
    return startTServer(address, processor, serverName, threadName, numThreads, timeBetweenThreadChecks, maxMessageSize, null, -1);
  }

  public static ServerAddress startTServer(HostAndPort address, TProcessor processor, String serverName, String threadName, int numThreads,
      long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams, long sslSocketTimeout) throws TTransportException {
    return startTServer(address, new TimedProcessor(processor, serverName, threadName), serverName, threadName, numThreads, timeBetweenThreadChecks,
        maxMessageSize, sslParams, sslSocketTimeout);
  }

  public static ServerAddress startTServer(HostAndPort address, TimedProcessor processor, String serverName, String threadName, int numThreads,
      long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams, long sslSocketTimeout) throws TTransportException {

    ServerAddress serverAddress;
    if (sslParams != null) {
      serverAddress = createSslThreadPoolServer(address, processor, sslSocketTimeout, sslParams);
    } else {
      serverAddress = createHsHaServer(address, processor, serverName, threadName, numThreads, timeBetweenThreadChecks, maxMessageSize);
    }
    final TServer finalServer = serverAddress.server;
     Runnable serveTask = new Runnable() {
       @Override
       public void run() {
@@ -314,9 +353,18 @@ public class TServerUtils {
     serveTask = new LoggingRunnable(TServerUtils.log, serveTask);
     Thread thread = new Daemon(serveTask, threadName);
     thread.start();
    return result;
    // check for the special "bind to everything address"
    if (serverAddress.address.getHostText().equals("0.0.0.0")) {
      // can't get the address from the bind, so we'll do our best to invent our hostname
      try {
        serverAddress = new ServerAddress(finalServer, HostAndPort.fromParts(InetAddress.getLocalHost().getHostName(), serverAddress.address.getPort()));
      } catch (UnknownHostException e) {
        throw new TTransportException(e);
      }
    }
    return serverAddress;
   }
  

   // Existing connections will keep our thread running: reach in with reflection and insist that they shutdown.
   public static void stopTServer(TServer s) {
     if (s == null)
diff --git a/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java b/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java
index 8e7b22139..5b85f188d 100644
-- a/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java
++ b/server/extras/src/main/java/org/apache/accumulo/utils/metanalysis/IndexMeta.java
@@ -26,6 +26,7 @@ import java.util.Map;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
@@ -120,6 +121,10 @@ public class IndexMeta extends Configured implements Tool {
   static class Opts extends ClientOpts {
     @Parameter(description = "<logfile> { <logfile> ...}")
     List<String> logFiles = new ArrayList<String>();
    
    public ClientConfiguration getConf() {
      return this.getClientConfiguration();
    }
   }
   
   @Override
@@ -146,7 +151,7 @@ public class IndexMeta extends Configured implements Tool {
     job.setNumReduceTasks(0);
     
     job.setOutputFormatClass(AccumuloOutputFormat.class);
    AccumuloOutputFormat.setZooKeeperInstance(job, opts.instance, opts.zookeepers);
    AccumuloOutputFormat.setZooKeeperInstance(job, opts.getConf());
     AccumuloOutputFormat.setConnectorInfo(job, opts.principal, opts.getToken());
     AccumuloOutputFormat.setCreateTables(job, false);
     
diff --git a/test/pom.xml b/test/pom.xml
index 54b8733bb..3e2f03383 100644
-- a/test/pom.xml
++ b/test/pom.xml
@@ -25,6 +25,9 @@
   <artifactId>accumulo-test</artifactId>
   <name>Testing</name>
   <description>Tests for Apache Accumulo.</description>
  <properties>
    <bouncycastle.version>1.49</bouncycastle.version>
  </properties>
   <dependencies>
     <dependency>
       <groupId>com.beust</groupId>
@@ -145,6 +148,18 @@
       <artifactId>hadoop-minicluster</artifactId>
       <scope>test</scope>
     </dependency>
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcpkix-jdk15on</artifactId>
      <version>${bouncycastle.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcprov-jdk15on</artifactId>
      <version>${bouncycastle.version}</version>
      <scope>test</scope>
    </dependency>
     <dependency>
       <groupId>org.mortbay.jetty</groupId>
       <artifactId>jetty</artifactId>
@@ -223,6 +238,18 @@
               </execution>
             </executions>
           </plugin>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <configuration>
              <systemProperties>
                <property>
                   <name>org.apache.accumulo.test.functional.useSslForIT</name>
                   <value>${useSslForIT}</value>
                </property>
              </systemProperties>
            </configuration>
          </plugin>
         </plugins>
       </build>
     </profile>
diff --git a/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java b/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java
index 948a741e5..7bee351f4 100644
-- a/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java
++ b/test/src/main/java/org/apache/accumulo/test/IMMLGBenchmark.java
@@ -36,6 +36,7 @@ import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
@@ -51,7 +52,7 @@ import org.apache.hadoop.io.Text;
  */
 public class IMMLGBenchmark {
   public static void main(String[] args) throws Exception {
    ZooKeeperInstance zki = new ZooKeeperInstance("test16", "localhost");
    ZooKeeperInstance zki = new ZooKeeperInstance(new ClientConfiguration().withInstance("test16").withZkHosts("localhost"));
     Connector conn = zki.getConnector("root", new PasswordToken("secret"));
     
     int numlg = Integer.parseInt(args[0]);
diff --git a/test/src/main/java/org/apache/accumulo/test/TestIngest.java b/test/src/main/java/org/apache/accumulo/test/TestIngest.java
index 972a20e3d..1efd872e0 100644
-- a/test/src/main/java/org/apache/accumulo/test/TestIngest.java
++ b/test/src/main/java/org/apache/accumulo/test/TestIngest.java
@@ -49,7 +49,6 @@ import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.core.util.FastFormat;
 import org.apache.accumulo.fate.zookeeper.ZooReader;
 import org.apache.accumulo.server.cli.ClientOnDefaultTable;
import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.accumulo.trace.instrument.Trace;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
@@ -180,8 +179,6 @@ public class TestIngest {
     Opts opts = new Opts();
     BatchWriterOpts bwOpts = new BatchWriterOpts();
     opts.parseArgs(TestIngest.class.getName(), args, bwOpts);
    opts.getInstance().setConfiguration(ServerConfiguration.getSiteConfiguration());

     
     Instance instance = opts.getInstance();
     
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java b/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
index c6ad74feb..cf4f13485 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
@@ -30,6 +30,7 @@ import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Mutation;
@@ -57,7 +58,7 @@ public class MetadataBatchScanTest {
   
   public static void main(String[] args) throws Exception {
     
    final Connector connector = new ZooKeeperInstance("acu14", "localhost").getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get()
    final Connector connector = new ZooKeeperInstance(new ClientConfiguration().withInstance("acu14").withZkHosts("localhost")).getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get()
         .getToken());
     
     TreeSet<Long> splits = new TreeSet<Long>();
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
index 78b2564b3..05384d7aa 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
@@ -29,6 +29,7 @@ import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.KeyExtent;
@@ -247,7 +248,7 @@ public class NullTserver {
     HostAndPort addr = HostAndPort.fromParts(InetAddress.getLocalHost().getHostName(), opts.port);
     
     // modify !METADATA
    ZooKeeperInstance zki = new ZooKeeperInstance(opts.iname, opts.keepers);
    ZooKeeperInstance zki = new ZooKeeperInstance(new ClientConfiguration().withInstance(opts.iname).withZkHosts(opts.keepers));
     String tableId = Tables.getTableId(zki, opts.tableName);
     
     // read the locations for the table
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java
index ea6d8ed19..5227b2aea 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/State.java
@@ -31,6 +31,7 @@ import org.apache.accumulo.core.client.MultiTableBatchWriter;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.log4j.Logger;
 
@@ -124,7 +125,7 @@ public class State {
     if (instance == null) {
       String instance = props.getProperty("INSTANCE");
       String zookeepers = props.getProperty("ZOOKEEPERS");
      this.instance = new ZooKeeperInstance(instance, zookeepers);
      this.instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(instance).withZkHosts(zookeepers));
     }
     return instance;
   }
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java
index e104c99b0..749209e3e 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/multitable/CopyTool.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
@@ -55,11 +56,13 @@ public class CopyTool extends Configured implements Tool {
       return 1;
     }
     
    ClientConfiguration clientConf = new ClientConfiguration().withInstance(args[3]).withZkHosts(args[4]);

     job.setInputFormatClass(AccumuloInputFormat.class);
     AccumuloInputFormat.setConnectorInfo(job, args[0], new PasswordToken(args[1]));
     AccumuloInputFormat.setInputTableName(job, args[2]);
     AccumuloInputFormat.setScanAuthorizations(job, Authorizations.EMPTY);
    AccumuloInputFormat.setZooKeeperInstance(job, args[3], args[4]);
    AccumuloInputFormat.setZooKeeperInstance(job, clientConf);
     
     job.setMapperClass(SeqMapClass.class);
     job.setMapOutputKeyClass(Text.class);
@@ -71,7 +74,7 @@ public class CopyTool extends Configured implements Tool {
     AccumuloOutputFormat.setConnectorInfo(job, args[0], new PasswordToken(args[1]));
     AccumuloOutputFormat.setCreateTables(job, true);
     AccumuloOutputFormat.setDefaultTableName(job, args[5]);
    AccumuloOutputFormat.setZooKeeperInstance(job, args[3], args[4]);
    AccumuloOutputFormat.setZooKeeperInstance(job, clientConf);
     
     job.waitForCompletion(true);
     return job.isSuccessful() ? 0 : 1;
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java
index 6c7cc6315..b0c5029d6 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/sequential/MapRedVerifyTool.java
@@ -22,6 +22,7 @@ import java.util.Iterator;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
@@ -86,10 +87,12 @@ public class MapRedVerifyTool extends Configured implements Tool {
       return 1;
     }
     
    ClientConfiguration clientConf = new ClientConfiguration().withInstance(args[3]).withZkHosts(args[4]);

     job.setInputFormatClass(AccumuloInputFormat.class);
     AccumuloInputFormat.setConnectorInfo(job, args[0], new PasswordToken(args[1]));
     AccumuloInputFormat.setInputTableName(job, args[2]);
    AccumuloInputFormat.setZooKeeperInstance(job, args[3], args[4]);
    AccumuloInputFormat.setZooKeeperInstance(job, clientConf);
     
     job.setMapperClass(SeqMapClass.class);
     job.setMapOutputKeyClass(NullWritable.class);
@@ -102,7 +105,7 @@ public class MapRedVerifyTool extends Configured implements Tool {
     AccumuloOutputFormat.setConnectorInfo(job, args[0], new PasswordToken(args[1]));
     AccumuloOutputFormat.setCreateTables(job, true);
     AccumuloOutputFormat.setDefaultTableName(job, args[5]);
    AccumuloOutputFormat.setZooKeeperInstance(job, args[3], args[4]);
    AccumuloOutputFormat.setZooKeeperInstance(job, clientConf);
     
     job.waitForCompletion(true);
     return job.isSuccessful() ? 0 : 1;
diff --git a/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java b/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java
index e6ba77b2b..c20d00429 100644
-- a/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java
++ b/test/src/main/java/org/apache/accumulo/test/scalability/ScaleTest.java
@@ -24,6 +24,7 @@ import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.hadoop.io.Text;
 
 public abstract class ScaleTest {
@@ -47,7 +48,7 @@ public abstract class ScaleTest {
     String password = this.scaleProps.getProperty("PASSWORD");
     System.out.println(password);
     
    conn = new ZooKeeperInstance(instanceName, zookeepers).getConnector(user, new PasswordToken(password));
    conn = new ZooKeeperInstance(new ClientConfiguration().withInstance(instanceName).withZkHosts(zookeepers)).getConnector(user, new PasswordToken(password));
   }
   
   protected void startTimer() {
diff --git a/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java b/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java
index f7e114624..4fe3d3f87 100644
-- a/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java
++ b/test/src/test/java/org/apache/accumulo/test/MultiTableBatchWriterTest.java
@@ -34,13 +34,13 @@ import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.admin.TableOperations;
 import org.apache.accumulo.core.client.impl.MultiTableBatchWriterImpl;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.Credentials;
import org.apache.accumulo.core.security.thrift.TCredentials;
 import org.apache.accumulo.minicluster.MiniAccumuloCluster;
 import org.apache.accumulo.minicluster.MiniAccumuloConfig;
 import org.junit.AfterClass;
@@ -48,7 +48,6 @@ import org.junit.Assert;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.security.Credential;
 
 import com.google.common.collect.Maps;
 
@@ -69,11 +68,11 @@ public class MultiTableBatchWriterTest {
   public static void tearDownAfterClass() throws Exception {
     cluster.stop();
     folder.delete();
  }
 }
   
   @Test
   public void testTableRenameDataValidation() throws Exception {
    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
@@ -141,7 +140,7 @@ public class MultiTableBatchWriterTest {
 
   @Test
   public void testTableRenameSameWriters() throws Exception {
    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
@@ -203,7 +202,7 @@ public class MultiTableBatchWriterTest {
 
   @Test
   public void testTableRenameNewWriters() throws Exception {
    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
@@ -285,7 +284,7 @@ public class MultiTableBatchWriterTest {
 
   @Test
   public void testTableRenameNewWritersNoCaching() throws Exception {
    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
@@ -334,7 +333,7 @@ public class MultiTableBatchWriterTest {
 
   @Test
   public void testTableDelete() throws Exception {
    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
@@ -391,8 +390,7 @@ public class MultiTableBatchWriterTest {
 
   @Test
   public void testOfflineTable() throws Exception {

    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
@@ -448,8 +446,7 @@ public class MultiTableBatchWriterTest {
 
   @Test
   public void testOfflineTableWithCache() throws Exception {

    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
@@ -508,8 +505,7 @@ public class MultiTableBatchWriterTest {
 
   @Test
   public void testOfflineTableWithoutCache() throws Exception {

    ZooKeeperInstance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
    ZooKeeperInstance instance = new ZooKeeperInstance(new ClientConfiguration().withInstance(cluster.getInstanceName()).withZkHosts(cluster.getZooKeepers()));
     Connector connector = instance.getConnector("root", password);
 
     BatchWriterConfig config = new BatchWriterConfig();
diff --git a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
index dbf5f4c09..ec906d64d 100644
-- a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
++ b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
@@ -26,7 +26,6 @@ import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.PrintWriter;
 import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
 import java.util.Map.Entry;
 
 import jline.console.ConsoleReader;
@@ -46,8 +45,6 @@ import org.apache.accumulo.core.metadata.RootTable;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.accumulo.core.util.shell.Shell;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
 import org.apache.accumulo.test.functional.SimpleMacIT;
 import org.apache.accumulo.tracer.TraceServer;
 import org.apache.commons.io.FileUtils;
@@ -59,9 +56,7 @@ import org.apache.hadoop.tools.DistCp;
 import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
import org.junit.Rule;
 import org.junit.Test;
import org.junit.rules.TemporaryFolder;
 
 public class ShellServerIT extends SimpleMacIT {
   public static class TestOutputStream extends OutputStream {
@@ -99,8 +94,6 @@ public class ShellServerIT extends SimpleMacIT {
     }
   }
 
  private static String secret = "superSecret";
  public static MiniAccumuloCluster cluster;
   public static TestOutputStream output;
   public static StringInputStream input;
   public static Shell shell;
@@ -152,28 +145,21 @@ public class ShellServerIT extends SimpleMacIT {
 
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
    folder.create();
    MiniAccumuloConfig cfg = new MiniAccumuloConfig(folder.newFolder("miniAccumulo"), secret);
    cluster = new MiniAccumuloCluster(cfg);
    cluster.start();

     // history file is updated in $HOME
    System.setProperty("HOME", folder.getRoot().getAbsolutePath());
    System.setProperty("HOME", getFolder().getAbsolutePath());
 
     // start the shell
     output = new TestOutputStream();
     input = new StringInputStream();
     shell = new Shell(new ConsoleReader(input, output));
     shell.setLogErrorsToConsole();
    shell.config("-u", "root", "-p", secret, "-z", cluster.getConfig().getInstanceName(), cluster.getConfig().getZooKeepers());
    shell.config("-u", "root", "-p", ROOT_PASSWORD, "-z", getStaticCluster().getConfig().getInstanceName(), getStaticCluster().getConfig().getZooKeepers(),
        "--config-file", getStaticCluster().getConfig().getClientConfFile().getAbsolutePath());
     exec("quit", true);
     shell.start();
     shell.setExit(false);
 
    // use reflection to call this method so it does not need to be made public
    Method method = cluster.getClass().getDeclaredMethod("exec", Class.class, String[].class);
    method.setAccessible(true);
    traceProcess = (Process) method.invoke(cluster, TraceServer.class, new String[0]);
    traceProcess = getStaticCluster().exec(TraceServer.class);
 
     // give the tracer some time to start
     UtilWaitThread.sleep(1000);
@@ -181,14 +167,12 @@ public class ShellServerIT extends SimpleMacIT {
 
   @AfterClass
   public static void tearDownAfterClass() throws Exception {
    cluster.stop();
     traceProcess.destroy();
    folder.delete();
   }
 
   @After
  public void tearDown() throws Exception {
    Connector c = cluster.getConnector("root", secret);
  public void deleteTables() throws Exception {
    Connector c = getConnector();
     for (String table : c.tableOperations().list()) {
       if (!table.equals(MetadataTable.NAME) && !table.equals(RootTable.NAME) && !table.equals("trace"))
         try {
@@ -207,10 +191,10 @@ public class ShellServerIT extends SimpleMacIT {
     exec("addsplits row5", true);
     exec("config -t t -s table.split.threshold=345M", true);
     exec("offline t", true);
    String export = "file://" + folder.newFolder().toString();
    String export = "file://" + new File(getFolder(), "ShellServerIT.export").toString();
     exec("exporttable -t t " + export, true);
     DistCp cp = newDistCp();
    String import_ = "file://" + folder.newFolder().toString();
    String import_ = "file://" + new File(getFolder(), "ShellServerIT.import").toString();
     cp.run(new String[] {"-f", export + "/distcp.txt", import_});
     exec("importtable t2 " + import_, true);
     exec("config -t t2 -np", true, "345M", true);
@@ -260,7 +244,7 @@ public class ShellServerIT extends SimpleMacIT {
   @Test(timeout = 30 * 1000)
   public void execfile() throws Exception {
     // execfile
    File file = folder.newFile();
    File file = File.createTempFile("ShellServerIT.execfile", ".conf", getFolder());
     PrintWriter writer = new PrintWriter(file.getAbsolutePath());
     writer.println("about");
     writer.close();
@@ -326,7 +310,7 @@ public class ShellServerIT extends SimpleMacIT {
     exec("scan", true, "row1", true);
     exec("droptable -f t", true);
     exec("deleteuser xyzzy", false, "delete yourself", true);
    input.set(secret + "\n" + secret + "\n");
    input.set(ROOT_PASSWORD + "\n" + ROOT_PASSWORD + "\n");
     exec("user root", true);
     exec("revoke -u xyzzy -s System.CREATE_TABLE", true);
     exec("revoke -u xyzzy -s System.GOOFY", false);
@@ -598,10 +582,12 @@ public class ShellServerIT extends SimpleMacIT {
   public void importDirectory() throws Exception {
     Configuration conf = new Configuration();
     FileSystem fs = FileSystem.get(conf);
    File importDir = folder.newFolder("import");
    File importDir = new File(getFolder(), "import");
    importDir.mkdir();
     String even = new File(importDir, "even.rf").toString();
     String odd = new File(importDir, "odd.rf").toString();
    File errorsDir = folder.newFolder("errors");
    File errorsDir = new File(getFolder(), "errors");
    errorsDir.mkdir();
     fs.mkdirs(new Path(errorsDir.toString()));
     AccumuloConfiguration aconf = AccumuloConfiguration.getDefaultConfiguration();
     FileSKVWriter evenWriter = FileOperations.getInstance().openWriter(even, fs, conf, aconf);
@@ -751,7 +737,7 @@ public class ShellServerIT extends SimpleMacIT {
       @Override
       public void run() {
         try {
          Connector connector = cluster.getConnector("root", secret);
          Connector connector = getConnector();
           Scanner s = connector.createScanner("t", Authorizations.EMPTY);
           for (@SuppressWarnings("unused")
           Entry<Key,Value> kv : s)
@@ -769,20 +755,26 @@ public class ShellServerIT extends SimpleMacIT {
     assertTrue(last.contains("RUNNING"));
     String parts[] = last.split("\\|");
     assertEquals(13, parts.length);
    String hostPortPattern = ".+:\\d+";
    String tserver = parts[0].trim();
    assertTrue(tserver.matches(hostPortPattern));
    assertTrue(getConnector().instanceOperations().getTabletServers().contains(tserver));
    String client = parts[1].trim();
    assertTrue(client.matches(hostPortPattern));
    // TODO: any way to tell if the client address is accurate? could be local IP, host, loopback...?
     thread.join();
     exec("deletetable -f t", true);
   }
 
  @Rule
  public TemporaryFolder folder2 = new TemporaryFolder(new File(System.getProperty("user.dir") + "/target"));

   @Test(timeout = 30 * 1000)
   public void testPertableClasspath() throws Exception {
    File fooFilterJar = folder2.newFile("FooFilter.jar");
    File fooFilterJar = File.createTempFile("FooFilter", ".jar");
     FileUtils.copyURLToFile(this.getClass().getResource("/FooFilter.jar"), fooFilterJar);
    fooFilterJar.deleteOnExit();
 
    File fooConstraintJar = folder2.newFile("FooConstraint.jar");
    File fooConstraintJar = File.createTempFile("FooConstraint", ".jar");
     FileUtils.copyURLToFile(this.getClass().getResource("/FooConstraint.jar"), fooConstraintJar);
    fooConstraintJar.deleteOnExit();
 
     exec(
         "config -s " + Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey() + "cx1=" + fooFilterJar.toURI().toString() + "," + fooConstraintJar.toURI().toString(),
@@ -832,7 +824,7 @@ public class ShellServerIT extends SimpleMacIT {
 
   @Test(timeout = 30 * 1000)
   public void badLogin() throws Exception {
    input.set(secret + "\n");
    input.set(ROOT_PASSWORD + "\n");
     String err = exec("user NoSuchUser", false);
     assertTrue(err.contains("BAD_CREDENTIALS for user NoSuchUser"));
   }
@@ -872,7 +864,7 @@ public class ShellServerIT extends SimpleMacIT {
     input.set("secret\n");
     exec("user test_user", true);
     assertTrue(exec("whoami", true).contains("test_user"));
    input.set(secret + "\n");
    input.set(ROOT_PASSWORD + "\n");
     exec("user root", true);
   }
 
@@ -886,11 +878,4 @@ public class ShellServerIT extends SimpleMacIT {
     exec("scan -t !METADATA -np -c file");
     return output.get().split("\n").length - 1;
   }

  public static TemporaryFolder folder = new TemporaryFolder(new File(System.getProperty("user.dir") + "/target/"));

  public MiniAccumuloCluster getCluster() {
    return cluster;
  }

 }
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/AbstractMacIT.java b/test/src/test/java/org/apache/accumulo/test/functional/AbstractMacIT.java
index d24b85bcf..f74b2050c 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/AbstractMacIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/AbstractMacIT.java
@@ -17,6 +17,7 @@
 package org.apache.accumulo.test.functional;
 
 import java.io.File;
import java.util.Map;
 import java.util.Random;
 import java.util.concurrent.atomic.AtomicInteger;
 
@@ -25,7 +26,10 @@ import org.apache.accumulo.core.cli.ScannerOpts;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.test.util.CertUtils;
 import org.apache.commons.io.FileUtils;
 import org.apache.log4j.Logger;
 import org.junit.Rule;
@@ -81,8 +85,39 @@ public abstract class AbstractMacIT {
     return names;
   }
 
  protected static void configureForEnvironment(MiniAccumuloConfig cfg, Class<?> testClass, File folder) {
    if ("true".equals(System.getProperty("org.apache.accumulo.test.functional.useSslForIT"))) {
      configureForSsl(cfg, folder);
    }
  }

  protected static void configureForSsl(MiniAccumuloConfig cfg, File folder) {
    Map<String,String> siteConfig = cfg.getSiteConfig();
    if ("true".equals(siteConfig.get(Property.INSTANCE_RPC_SSL_ENABLED.getKey()))) {
      // already enabled; don't mess with it
      return;
    }

    File sslDir = new File(folder, "ssl");
    sslDir.mkdirs();
    File rootKeystoreFile = new File(sslDir, "root-" + cfg.getInstanceName() + ".jks");
    File localKeystoreFile = new File(sslDir, "local-" + cfg.getInstanceName() + ".jks");
    File publicTruststoreFile = new File(sslDir, "public-" + cfg.getInstanceName() + ".jks");
    try {
      new CertUtils(Property.RPC_SSL_KEYSTORE_TYPE.getDefaultValue(), "o=Apache Accumulo,cn=MiniAccumuloCluster", "RSA", 2048, "sha1WithRSAEncryption")
          .createAll(rootKeystoreFile, localKeystoreFile, publicTruststoreFile, cfg.getInstanceName(), cfg.getRootPassword());
    } catch (Exception e) {
      throw new RuntimeException("error creating MAC keystore", e);
    }

    siteConfig.put(Property.INSTANCE_RPC_SSL_ENABLED.getKey(), "true");
    siteConfig.put(Property.RPC_SSL_KEYSTORE_PATH.getKey(), localKeystoreFile.getAbsolutePath());
    siteConfig.put(Property.RPC_SSL_KEYSTORE_PASSWORD.getKey(), cfg.getRootPassword());
    siteConfig.put(Property.RPC_SSL_TRUSTSTORE_PATH.getKey(), publicTruststoreFile.getAbsolutePath());
    cfg.setSiteConfig(siteConfig);
  }

   public abstract Connector getConnector() throws AccumuloException, AccumuloSecurityException;
 
   public abstract String rootPath();

 }
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/AccumuloInputFormatIT.java b/test/src/test/java/org/apache/accumulo/test/functional/AccumuloInputFormatIT.java
index d38536a33..3ebafffa9 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/AccumuloInputFormatIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/AccumuloInputFormatIT.java
@@ -64,7 +64,7 @@ public class AccumuloInputFormatIT extends SimpleMacIT {
     @SuppressWarnings("deprecation")
     Job job = new Job();
     AccumuloInputFormat.setInputTableName(job, table);
    AccumuloInputFormat.setZooKeeperInstance(job, getConnector().getInstance().getInstanceName(), getConnector().getInstance().getZooKeepers());
    AccumuloInputFormat.setZooKeeperInstance(job, getStaticCluster().getClientConfig());
     AccumuloInputFormat.setConnectorInfo(job, "root", new PasswordToken(ROOT_PASSWORD));
 
     // split table
@@ -100,7 +100,8 @@ public class AccumuloInputFormatIT extends SimpleMacIT {
 
     // auto adjust ranges
     ranges = new ArrayList<Range>();
    for (int i = 0; i < 5; i++) // overlapping ranges
    for (int i = 0; i < 5; i++)
      // overlapping ranges
       ranges.add(new Range(String.format("%09d", i), String.format("%09d", i + 2)));
     AccumuloInputFormat.setRanges(job, ranges);
     splits = inputFormat.getSplits(job);
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/BulkIT.java b/test/src/test/java/org/apache/accumulo/test/functional/BulkIT.java
index 2fb58270c..d86b70428 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/BulkIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/BulkIT.java
@@ -16,7 +16,14 @@
  */
 package org.apache.accumulo.test.functional;
 
import java.io.IOException;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.test.TestIngest;
 import org.apache.accumulo.test.TestIngest.Opts;
@@ -32,8 +39,11 @@ public class BulkIT extends SimpleMacIT {
 
   @Test(timeout = 4 * 60 * 1000)
   public void test() throws Exception {
    Connector c = getConnector();
    String tableName = getTableNames(1)[0];
    runTest(getConnector(), getTableNames(1)[0]);
  }

  static void runTest(Connector c, String tableName) throws AccumuloException, AccumuloSecurityException, TableExistsException, IOException, TableNotFoundException,
      MutationsRejectedException {
     c.tableOperations().create(tableName);
     FileSystem fs = FileSystem.get(CachedConfiguration.getInstance());
     String base = "target/accumulo-maven-plugin";
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/ConcurrencyIT.java b/test/src/test/java/org/apache/accumulo/test/functional/ConcurrencyIT.java
index c3d3160f0..87f6bd782 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/ConcurrencyIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/ConcurrencyIT.java
@@ -20,11 +20,16 @@ import java.util.Collections;
 import java.util.EnumSet;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
@@ -75,13 +80,18 @@ public class ConcurrencyIT extends ConfigurableMacIT {
   @Test(timeout = 2 * 60 * 1000)
   public void run() throws Exception {
     Connector c = getConnector();
    runTest(c);
  }

  static void runTest(Connector c) throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException,
      MutationsRejectedException, Exception, InterruptedException {
     c.tableOperations().create("cct");
     IteratorSetting is = new IteratorSetting(10, SlowIterator.class);
     SlowIterator.setSleepTime(is, 50);
     c.tableOperations().attachIterator("cct", is, EnumSet.of(IteratorScope.minc, IteratorScope.majc));
     c.tableOperations().setProperty("cct", Property.TABLE_MAJC_RATIO.getKey(), "1.0");
     
    BatchWriter bw = getConnector().createBatchWriter("cct", new BatchWriterConfig());
    BatchWriter bw = c.createBatchWriter("cct", new BatchWriterConfig());
     for (int i = 0; i < 50; i++) {
       Mutation m = new Mutation(new Text(String.format("%06d", i)));
       m.put(new Text("cf1"), new Text("cq1"), new Value("foo".getBytes()));
@@ -89,14 +99,14 @@ public class ConcurrencyIT extends ConfigurableMacIT {
     }
     bw.flush();
     
    ScanTask st0 = new ScanTask(getConnector(), 300);
    ScanTask st0 = new ScanTask(c, 300);
     st0.start();
     
    ScanTask st1 = new ScanTask(getConnector(), 100);
    ScanTask st1 = new ScanTask(c, 100);
     st1.start();
     
     UtilWaitThread.sleep(50);
    getConnector().tableOperations().flush("cct", null, null, true);
    c.tableOperations().flush("cct", null, null, true);
     
     for (int i = 0; i < 50; i++) {
       Mutation m = new Mutation(new Text(String.format("%06d", i)));
@@ -106,7 +116,7 @@ public class ConcurrencyIT extends ConfigurableMacIT {
     
     bw.flush();
     
    ScanTask st2 = new ScanTask(getConnector(), 100);
    ScanTask st2 = new ScanTask(c, 100);
     st2.start();
     
     st1.join();
@@ -117,11 +127,11 @@ public class ConcurrencyIT extends ConfigurableMacIT {
     if (st2.count != 50)
       throw new Exception("Thread 2 did not see 50, saw " + st2.count);
     
    ScanTask st3 = new ScanTask(getConnector(), 150);
    ScanTask st3 = new ScanTask(c, 150);
     st3.start();
     
     UtilWaitThread.sleep(50);
    getConnector().tableOperations().flush("cct", null, null, false);
    c.tableOperations().flush("cct", null, null, false);
     
     st3.join();
     if (st3.count != 50)
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/ConfigurableMacIT.java b/test/src/test/java/org/apache/accumulo/test/functional/ConfigurableMacIT.java
index 3f60f1dac..21c2bb737 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/ConfigurableMacIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/ConfigurableMacIT.java
@@ -42,6 +42,7 @@ public class ConfigurableMacIT extends AbstractMacIT {
   public void setUp() throws Exception {
     MiniAccumuloConfig cfg = new MiniAccumuloConfig(createTestDir(this.getClass().getName()), ROOT_PASSWORD);
     configure(cfg);
    configureForEnvironment(cfg, getClass(), createSharedTestDir(this.getClass().getName() + "-ssl"));
     cluster = new MiniAccumuloCluster(cfg);
     cluster.start();
   }
@@ -70,7 +71,7 @@ public class ConfigurableMacIT extends AbstractMacIT {
   }
 
   public String getMonitor() throws KeeperException, InterruptedException {
    Instance instance = new ZooKeeperInstance(getCluster().getInstanceName(), getCluster().getZooKeepers());
    Instance instance = new ZooKeeperInstance(getCluster().getClientConfig());
     return MonitorUtil.getLocation(instance);
   }
 
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/MapReduceIT.java b/test/src/test/java/org/apache/accumulo/test/functional/MapReduceIT.java
index 9e42e5594..0867e7305 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/MapReduceIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/MapReduceIT.java
@@ -18,18 +18,26 @@ package org.apache.accumulo.test.functional;
 
 import static org.junit.Assert.assertEquals;
 
import java.io.IOException;
 import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.examples.simple.mapreduce.RowHash;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
 import org.apache.hadoop.io.Text;
 import org.codehaus.plexus.util.Base64;
 import org.junit.Test;
@@ -45,7 +53,11 @@ public class MapReduceIT extends ConfigurableMacIT {
 
   @Test(timeout = 60 * 1000)
   public void test() throws Exception {
    Connector c = getConnector();
    runTest(getConnector(), getCluster());
  }

  static void runTest(Connector c, MiniAccumuloCluster cluster) throws AccumuloException, AccumuloSecurityException, TableExistsException,
      TableNotFoundException, MutationsRejectedException, IOException, InterruptedException, NoSuchAlgorithmException {
     c.tableOperations().create(tablename);
     BatchWriter bw = c.createBatchWriter(tablename, new BatchWriterConfig());
     for (int i = 0; i < 10; i++) {
@@ -54,9 +66,8 @@ public class MapReduceIT extends ConfigurableMacIT {
       bw.addMutation(m);
     }
     bw.close();

    Process hash = exec(RowHash.class, "-i", c.getInstance().getInstanceName(), "-z", c.getInstance().getZooKeepers(), "-u", "root", "-p", ROOT_PASSWORD, "-t",
        tablename, "--column", input_cfcq);
    Process hash = cluster.exec(RowHash.class, "-i", c.getInstance().getInstanceName(), "-z", c.getInstance().getZooKeepers(), "-u", "root", "-p",
        ROOT_PASSWORD, "-t", tablename, "--column", input_cfcq);
     assertEquals(0, hash.waitFor());
 
     Scanner s = c.createScanner(tablename, Authorizations.EMPTY);
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/ScannerIT.java b/test/src/test/java/org/apache/accumulo/test/functional/ScannerIT.java
index 79130898d..e364b4626 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/ScannerIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/ScannerIT.java
@@ -46,55 +46,55 @@ public class ScannerIT extends SimpleMacIT {
     final String table = "table";
     Connector c = getConnector();
     c.tableOperations().create(table);
    

     BatchWriter bw = c.createBatchWriter(table, new BatchWriterConfig());
    

     Mutation m = new Mutation("a");
     for (int i = 0; i < 10; i++) {
       m.put(Integer.toString(i), "", "");
     }
    

     bw.addMutation(m);
     bw.close();
    

     Scanner s = c.createScanner(table, new Authorizations());
    

     IteratorSetting cfg = new IteratorSetting(100, SlowIterator.class);
     SlowIterator.setSleepTime(cfg, 100l);
     s.addScanIterator(cfg);
     s.setReadaheadThreshold(5);
     s.setBatchSize(1);
     s.setRange(new Range());
    

     Stopwatch sw = new Stopwatch();
     Iterator<Entry<Key,Value>> iterator = s.iterator();
    

     sw.start();
     while (iterator.hasNext()) {
       sw.stop();
      

       // While we "do work" in the client, we should be fetching the next result
       UtilWaitThread.sleep(100l);
       iterator.next();
       sw.start();
     }
     sw.stop();
    

     long millisWithWait = sw.elapsed(TimeUnit.MILLISECONDS);
    

     s = c.createScanner(table, new Authorizations());
     s.addScanIterator(cfg);
     s.setRange(new Range());
     s.setBatchSize(1);
     s.setReadaheadThreshold(0l);
    

     sw = new Stopwatch();
     iterator = s.iterator();
    

     sw.start();
     while (iterator.hasNext()) {
       sw.stop();
      

       // While we "do work" in the client, we should be fetching the next result
       UtilWaitThread.sleep(100l);
       iterator.next();
@@ -103,10 +103,10 @@ public class ScannerIT extends SimpleMacIT {
     sw.stop();
 
     long millisWithNoWait = sw.elapsed(TimeUnit.MILLISECONDS);
    

     // The "no-wait" time should be much less than the "wait-time"
    Assert.assertTrue("Expected less time to be taken with immediate readahead (" + millisWithNoWait 
        + ") than without immediate readahead (" + millisWithWait + ")", millisWithNoWait < millisWithWait);
    Assert.assertTrue("Expected less time to be taken with immediate readahead (" + millisWithNoWait + ") than without immediate readahead (" + millisWithWait
        + ")", millisWithNoWait < millisWithWait);
   }
 
 }
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/ShutdownIT.java b/test/src/test/java/org/apache/accumulo/test/functional/ShutdownIT.java
index 8d5882102..d1943677a 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/ShutdownIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/ShutdownIT.java
@@ -19,11 +19,13 @@ package org.apache.accumulo.test.functional;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 
import java.io.IOException;
 import java.util.List;
 import java.util.concurrent.atomic.AtomicReference;
 
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
 import org.apache.accumulo.server.util.Admin;
 import org.apache.accumulo.test.TestIngest;
 import org.apache.accumulo.test.TestRandomDeletes;
@@ -90,7 +92,10 @@ public class ShutdownIT extends ConfigurableMacIT {
   
   @Test(timeout = 2 * 60 * 1000)
   public void adminStop() throws Exception {
    Connector c = getConnector();
    runAdminStopTest(getConnector(), cluster);
  }

  static void runAdminStopTest(Connector c, MiniAccumuloCluster cluster) throws InterruptedException, IOException {
     assertEquals(0, cluster.exec(TestIngest.class, "-i", cluster.getInstanceName(), "-z", cluster.getZooKeepers(), "-u", "root", "-p", ROOT_PASSWORD, "--createTable").waitFor());
     List<String> tabletServers = c.instanceOperations().getTabletServers();
     assertEquals(2, tabletServers.size());
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java b/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java
index 9086f13a4..10db5156b 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/SimpleMacIT.java
@@ -26,6 +26,8 @@ import org.apache.accumulo.minicluster.MiniAccumuloCluster;
 import org.apache.accumulo.minicluster.MiniAccumuloConfig;
 import org.apache.accumulo.minicluster.MiniAccumuloInstance;
 import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
 import org.junit.BeforeClass;
 
 public class SimpleMacIT extends AbstractMacIT {
@@ -39,6 +41,7 @@ public class SimpleMacIT extends AbstractMacIT {
     if (getInstanceOneConnector() == null && cluster == null) {
       folder = createSharedTestDir(SimpleMacIT.class.getName());
       MiniAccumuloConfig cfg = new MiniAccumuloConfig(folder, ROOT_PASSWORD);
      configureForEnvironment(cfg, SimpleMacIT.class, createSharedTestDir(SimpleMacIT.class.getName() + "-ssl"));
       cluster = new MiniAccumuloCluster(cfg);
       cluster.start();
       Runtime.getRuntime().addShutdownHook(new Thread() {
@@ -61,6 +64,20 @@ public class SimpleMacIT extends AbstractMacIT {
     return (getInstanceOneConnector() == null ? cluster.getConfig().getDir() : getInstanceOnePath()).getAbsolutePath();
   }
 
  public static MiniAccumuloCluster getStaticCluster() {
    return cluster;
  }

  public static File getFolder() {
    return folder;
  }

  @After
  public void cleanUp() throws Exception {}

  @AfterClass
  public static void tearDown() throws Exception {}

   private static Connector getInstanceOneConnector() {
     try {
       return new MiniAccumuloInstance("instance1", getInstanceOnePath()).getConnector("root", new PasswordToken(ROOT_PASSWORD));
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/SslIT.java b/test/src/test/java/org/apache/accumulo/test/functional/SslIT.java
new file mode 100644
index 000000000..6a29ad708
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/functional/SslIT.java
@@ -0,0 +1,62 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.test.functional;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.junit.Test;

/**
 * Do a selection of ITs with SSL turned on that cover a range of different connection scenarios. Note that you can run *all* the ITs against SSL-enabled mini
 * clusters with `mvn verify -DuseSslForIT`
 *
 */
public class SslIT extends ConfigurableMacIT {
  @Override
  public void configure(MiniAccumuloConfig cfg) {
    super.configure(cfg);
    configureForSsl(cfg, createSharedTestDir(this.getClass().getName() + "-ssl"));
  }

  @Test(timeout = 60 * 1000)
  public void binary() throws AccumuloException, AccumuloSecurityException, Exception {
    getConnector().tableOperations().create("bt");
    BinaryIT.runTest(getConnector());
  }

  @Test(timeout = 2 * 60 * 1000)
  public void concurrency() throws Exception {
    ConcurrencyIT.runTest(getConnector());
  }

  @Test(timeout = 2 * 60 * 1000)
  public void adminStop() throws Exception {
    ShutdownIT.runAdminStopTest(getConnector(), getCluster());
  }

  @Test(timeout = 2 * 60 * 1000)
  public void bulk() throws Exception {
    BulkIT.runTest(getConnector(), getTableNames(1)[0]);
  }

  @Test(timeout = 60 * 1000)
  public void mapReduce() throws Exception {
    MapReduceIT.runTest(getConnector(), getCluster());
  }

}
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/SslWithClientAuthIT.java b/test/src/test/java/org/apache/accumulo/test/functional/SslWithClientAuthIT.java
new file mode 100644
index 000000000..c40e2b37f
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/functional/SslWithClientAuthIT.java
@@ -0,0 +1,71 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.test.functional;

import java.util.Map;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.junit.Test;

/**
 * Run all the same tests as SslIT, but with client auth turned on.
 *
 * All the methods are overridden just to make it easier to run individual tests from an IDE.
 *
 */
public class SslWithClientAuthIT extends SslIT {
  @Override
  public void configure(MiniAccumuloConfig cfg) {
    super.configure(cfg);
    Map<String,String> site = cfg.getSiteConfig();
    site.put(Property.INSTANCE_RPC_SSL_CLIENT_AUTH.getKey(), "true");
    cfg.setSiteConfig(site);
  }

  @Override
  @Test(timeout = 60000)
  public void binary() throws AccumuloException, AccumuloSecurityException, Exception {
    super.binary();
  }

  @Override
  @Test(timeout = 120000)
  public void concurrency() throws Exception {
    super.concurrency();
  }

  @Override
  @Test(timeout = 120000)
  public void adminStop() throws Exception {
    super.adminStop();
  }

  @Override
  @Test(timeout = 120000)
  public void bulk() throws Exception {
    super.bulk();
  }

  @Override
  @Test(timeout = 60000)
  public void mapReduce() throws Exception {
    super.mapReduce();
  }
}
diff --git a/test/src/test/java/org/apache/accumulo/test/util/CertUtils.java b/test/src/test/java/org/apache/accumulo/test/util/CertUtils.java
new file mode 100644
index 000000000..bb7b16db5
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/util/CertUtils.java
@@ -0,0 +1,324 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.commons.io.FileExistsException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class CertUtils {
  private static final Logger log = Logger.getLogger(CertUtils.class);
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  static class Opts extends Help {
    @Parameter(description = "generate-all | generate-local | generate-self-trusted", required = true, arity = 1)
    List<String> operation = null;

    @Parameter(names = {"--local-keystore"}, description = "Target path for generated keystore")
    String localKeystore = null;

    @Parameter(names = {"--root-keystore"}, description = "Path to root truststore, generated with generate-all, or used for signing with generate-local")
    String rootKeystore = null;

    @Parameter(names = {"--root-truststore"}, description = "Target path for generated public root truststore")
    String truststore = null;

    @Parameter(names = {"--keystore-type"}, description = "Type of keystore file to use")
    String keystoreType = "JKS";

    @Parameter(
        names = {"--keystore-password"},
        description = "Password used to encrypt keystores.  If omitted, the instance-wide secret will be used.  If specified, the password must also be explicitly configured in Accumulo.")
    String keystorePassword = null;

    @Parameter(names = {"--key-name-prefix"}, description = "Prefix for names of generated keys")
    String keyNamePrefix = CertUtils.class.getSimpleName();

    @Parameter(names = {"--issuer-rdn"}, description = "RDN string for issuer, for example: 'c=US,o=My Organization,cn=My Name'")
    String issuerDirString = "o=Apache Accumulo";

    @Parameter(names = "--site-file", description = "Load configuration from the given site file")
    public String siteFile = null;

    @Parameter(names = "--signing-algorithm", description = "Algorithm used to sign certificates")
    public String signingAlg = "SHA256WITHRSA";

    @Parameter(names = "--encryption-algorithm", description = "Algorithm used to encrypt private keys")
    public String encryptionAlg = "RSA";

    @Parameter(names = "--keysize", description = "Key size used by encryption algorithm")
    public int keysize = 2048;

    @SuppressWarnings("deprecation")
    public AccumuloConfiguration getConfiguration() {
      if (siteFile == null) {
        return AccumuloConfiguration.getSiteConfiguration();
      } else {
        return new AccumuloConfiguration() {
          Configuration xml = new Configuration();
          {
            xml.addResource(new Path(siteFile));
          }

          @Override
          public Iterator<Entry<String,String>> iterator() {
            TreeMap<String,String> map = new TreeMap<String,String>();
            for (Entry<String,String> props : DefaultConfiguration.getInstance())
              map.put(props.getKey(), props.getValue());
            for (Entry<String,String> props : xml)
              map.put(props.getKey(), props.getValue());
            return map.entrySet().iterator();
          }

          @Override
          public String get(Property property) {
            String value = xml.get(property.getKey());
            if (value != null)
              return value;
            return DefaultConfiguration.getInstance().get(property);
          }

          @Override
          public void getProperties(Map<String,String> props, PropertyFilter filter) {
            for (Entry<String,String> entry : this)
              if (filter.accept(entry.getKey()))
                props.put(entry.getKey(), entry.getValue());
          }
        };
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Opts opts = new Opts();
    opts.parseArgs(CertUtils.class.getName(), args);
    String operation = opts.operation.get(0);

    String keyPassword = opts.keystorePassword;
    if (keyPassword == null)
      keyPassword = getDefaultKeyPassword();
    CertUtils certUtils = new CertUtils(opts.keystoreType, opts.issuerDirString, opts.encryptionAlg, opts.keysize, opts.signingAlg);

    if ("generate-all".equals(operation)) {
      certUtils.createAll(new File(opts.rootKeystore), new File(opts.localKeystore), new File(opts.truststore), opts.keyNamePrefix, keyPassword);
    } else if ("generate-local".equals(operation)) {
      certUtils.createSignedCert(new File(opts.localKeystore), opts.keyNamePrefix + "-local", "", opts.rootKeystore, "");
    } else if ("generate-self-trusted".equals(operation)) {
      certUtils.createSelfSignedCert(new File(opts.truststore), opts.keyNamePrefix + "-selfTrusted", "");
    } else {
      JCommander jcommander = new JCommander(opts);
      jcommander.setProgramName(CertUtils.class.getName());
      jcommander.usage();
      System.err.println("Unrecognized operation: " + opts.operation);
      System.exit(0);
    }
  }

  @SuppressWarnings("deprecation")
  private static String getDefaultKeyPassword() {
    return AccumuloConfiguration.getSiteConfiguration().get(Property.INSTANCE_SECRET);
  }

  private String issuerDirString;
  private String keystoreType;
  private String encryptionAlgorithm;
  private int keysize;
  private String signingAlgorithm;

  public CertUtils(String keystoreType, String issuerDirString, String encryptionAlgorithm, int keysize, String signingAlgorithm) {
    super();
    this.keystoreType = keystoreType;
    this.issuerDirString = issuerDirString;
    this.encryptionAlgorithm = encryptionAlgorithm;
    this.keysize = keysize;
    this.signingAlgorithm = signingAlgorithm;
  }

  public void createAll(File rootKeystoreFile, File localKeystoreFile, File trustStoreFile, String keyNamePrefix, String systemPassword)
      throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, OperatorCreationException, AccumuloSecurityException,
      NoSuchProviderException, UnrecoverableKeyException, FileNotFoundException {
    createSelfSignedCert(rootKeystoreFile, keyNamePrefix + "-root", systemPassword);
    createSignedCert(localKeystoreFile, keyNamePrefix + "-local", systemPassword, rootKeystoreFile.getAbsolutePath(), systemPassword);
    createPublicCert(trustStoreFile, keyNamePrefix + "-public", rootKeystoreFile.getAbsolutePath(), systemPassword);
  }

  public void createPublicCert(File targetKeystoreFile, String keyName, String rootKeystorePath, String rootKeystorePassword) throws NoSuchAlgorithmException,
      CertificateException, FileNotFoundException, IOException, KeyStoreException, UnrecoverableKeyException {
    KeyStore signerKeystore = KeyStore.getInstance(keystoreType);
    char[] signerPasswordArray = rootKeystorePassword.toCharArray();
    signerKeystore.load(new FileInputStream(rootKeystorePath), signerPasswordArray);
    Certificate rootCert = findCert(signerKeystore);

    KeyStore keystore = KeyStore.getInstance(keystoreType);
    keystore.load(null, null);
    keystore.setCertificateEntry(keyName + "Cert", rootCert);
    keystore.store(new FileOutputStream(targetKeystoreFile), new char[0]);
  }

  public void createSignedCert(File targetKeystoreFile, String keyName, String keystorePassword, String signerKeystorePath, String signerKeystorePassword)
      throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, OperatorCreationException, AccumuloSecurityException,
      UnrecoverableKeyException, NoSuchProviderException {
    KeyStore signerKeystore = KeyStore.getInstance(keystoreType);
    char[] signerPasswordArray = signerKeystorePassword.toCharArray();
    signerKeystore.load(new FileInputStream(signerKeystorePath), signerPasswordArray);
    Certificate signerCert = findCert(signerKeystore);
    PrivateKey signerKey = findPrivateKey(signerKeystore, signerPasswordArray);

    KeyPair kp = generateKeyPair();
    X509CertificateObject cert = generateCert(keyName, kp, false, signerCert.getPublicKey(), signerKey);

    char[] password = keystorePassword.toCharArray();
    KeyStore keystore = KeyStore.getInstance(keystoreType);
    keystore.load(null, null);
    keystore.setCertificateEntry(keyName + "Cert", cert);
    keystore.setKeyEntry(keyName + "Key", kp.getPrivate(), password, new Certificate[] {cert, signerCert});
    keystore.store(new FileOutputStream(targetKeystoreFile), password);
  }

  public void createSelfSignedCert(File targetKeystoreFile, String keyName, String keystorePassword) throws KeyStoreException, CertificateException,
      NoSuchAlgorithmException, IOException, OperatorCreationException, AccumuloSecurityException, NoSuchProviderException {
    if (targetKeystoreFile.exists()) {
      throw new FileExistsException(targetKeystoreFile);
    }

    KeyPair kp = generateKeyPair();

    X509CertificateObject cert = generateCert(keyName, kp, true, kp.getPublic(), kp.getPrivate());

    char[] password = keystorePassword.toCharArray();
    KeyStore keystore = KeyStore.getInstance(keystoreType);
    keystore.load(null, null);
    keystore.setCertificateEntry(keyName + "Cert", cert);
    keystore.setKeyEntry(keyName + "Key", kp.getPrivate(), password, new Certificate[] {cert});
    keystore.store(new FileOutputStream(targetKeystoreFile), password);
  }

  private KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    KeyPairGenerator gen = KeyPairGenerator.getInstance(encryptionAlgorithm);
    gen.initialize(keysize);
    return gen.generateKeyPair();
  }

  private X509CertificateObject generateCert(String keyName, KeyPair kp, boolean isCertAuthority, PublicKey signerPublicKey, PrivateKey signerPrivateKey)
      throws IOException, CertIOException, OperatorCreationException, CertificateException, NoSuchAlgorithmException {
    Calendar startDate = Calendar.getInstance();
    Calendar endDate = Calendar.getInstance();
    endDate.add(Calendar.YEAR, 100);

    BigInteger serialNumber = BigInteger.valueOf((startDate.getTimeInMillis()));
    X500Name issuer = new X500Name(IETFUtils.rDNsFromString(issuerDirString, RFC4519Style.INSTANCE));
    JcaX509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(issuer, serialNumber, startDate.getTime(), endDate.getTime(), issuer, kp.getPublic());
    JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
    certGen.addExtension(X509Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(kp.getPublic()));
    certGen.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(isCertAuthority));
    certGen.addExtension(X509Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(signerPublicKey));
    if (isCertAuthority) {
      certGen.addExtension(X509Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
    }
    X509CertificateHolder cert = certGen.build(new JcaContentSignerBuilder(signingAlgorithm).build(signerPrivateKey));
    return new X509CertificateObject(cert.toASN1Structure());
  }

  static Certificate findCert(KeyStore keyStore) throws KeyStoreException {
    Enumeration<String> aliases = keyStore.aliases();
    Certificate cert = null;
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      if (keyStore.isCertificateEntry(alias)) {
        if (cert == null) {
          cert = keyStore.getCertificate(alias);
        } else {
          log.warn("Found multiple certificates in keystore.  Ignoring " + alias);
        }
      }
    }
    if (cert == null) {
      throw new KeyStoreException("Could not find cert in keystore");
    }
    return cert;
  }

  static PrivateKey findPrivateKey(KeyStore keyStore, char[] keystorePassword) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
    Enumeration<String> aliases = keyStore.aliases();
    PrivateKey key = null;
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      if (keyStore.isKeyEntry(alias)) {
        if (key == null) {
          key = (PrivateKey) keyStore.getKey(alias, keystorePassword);
        } else {
          log.warn("Found multiple keys in keystore.  Ignoring " + alias);
        }
      }
    }
    if (key == null) {
      throw new KeyStoreException("Could not find private key in keystore");
    }
    return key;
  }
}
diff --git a/test/src/test/java/org/apache/accumulo/test/util/CertUtilsTest.java b/test/src/test/java/org/apache/accumulo/test/util/CertUtilsTest.java
new file mode 100644
index 000000000..bb2a9339b
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/util/CertUtilsTest.java
@@ -0,0 +1,158 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.test.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.security.cert.Certificate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CertUtilsTest {
  private static final String KEYSTORE_TYPE = "JKS";
  private static final String PASSWORD = "CertUtilsTestPassword";
  private static final char[] PASSWORD_CHARS = PASSWORD.toCharArray();
  private static final String RDN_STRING = "o=Apache Accumulo,cn=CertUtilsTest";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private CertUtils getUtils() {
    return new CertUtils(KEYSTORE_TYPE, RDN_STRING, "RSA", 2048, "sha1WithRSAEncryption");
  }

  @Test
  public void createSelfSigned() throws Exception {
    CertUtils certUtils = getUtils();
    File keyStoreFile = new File(folder.getRoot(), "selfsigned.jks");
    certUtils.createSelfSignedCert(keyStoreFile, "test", PASSWORD);

    KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    keyStore.load(new FileInputStream(keyStoreFile), PASSWORD_CHARS);
    Certificate cert = CertUtils.findCert(keyStore);

    cert.verify(cert.getPublicKey()); // throws exception if it can't be verified
  }

  @Test
  public void createPublicSelfSigned() throws Exception {
    CertUtils certUtils = getUtils();
    File rootKeyStoreFile = new File(folder.getRoot(), "root.jks");
    certUtils.createSelfSignedCert(rootKeyStoreFile, "test", PASSWORD);
    File publicKeyStoreFile = new File(folder.getRoot(), "public.jks");
    certUtils.createPublicCert(publicKeyStoreFile, "test", rootKeyStoreFile.getAbsolutePath(), PASSWORD);

    KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    keyStore.load(new FileInputStream(publicKeyStoreFile), new char[0]);
    try {
      CertUtils.findPrivateKey(keyStore, PASSWORD_CHARS);
      fail("expected not to find private key in keystore");
    } catch (KeyStoreException e) {
      assertTrue(e.getMessage().contains("private key"));
    }
    Certificate cert = CertUtils.findCert(keyStore);
    cert.verify(cert.getPublicKey()); // throws exception if it can't be verified
  }

  @Test
  public void createSigned() throws Exception {
    CertUtils certUtils = getUtils();
    File rootKeyStoreFile = new File(folder.getRoot(), "root.jks");
    certUtils.createSelfSignedCert(rootKeyStoreFile, "test", PASSWORD);
    File signedKeyStoreFile = new File(folder.getRoot(), "signed.jks");
    certUtils.createSignedCert(signedKeyStoreFile, "test", PASSWORD, rootKeyStoreFile.getAbsolutePath(), PASSWORD);

    KeyStore rootKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    rootKeyStore.load(new FileInputStream(rootKeyStoreFile), PASSWORD_CHARS);
    Certificate rootCert = CertUtils.findCert(rootKeyStore);

    KeyStore signedKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    signedKeyStore.load(new FileInputStream(signedKeyStoreFile), PASSWORD_CHARS);
    Certificate signedCert = CertUtils.findCert(signedKeyStore);

    try {
      signedCert.verify(signedCert.getPublicKey());
      fail("signed cert should not be able to verify itself");
    } catch (SignatureException e) {
      // expected
    }

    signedCert.verify(rootCert.getPublicKey()); // throws exception if it can't be verified
  }

  @Test
  public void publicOnlyVerfication() throws Exception {
    // this approximates the real life scenario. the client will only have the public key of each
    // cert (the root made by us as below, but the signed cert extracted by the SSL transport)
    CertUtils certUtils = getUtils();
    File rootKeyStoreFile = new File(folder.getRoot(), "root.jks");
    certUtils.createSelfSignedCert(rootKeyStoreFile, "test", PASSWORD);
    File publicRootKeyStoreFile = new File(folder.getRoot(), "publicroot.jks");
    certUtils.createPublicCert(publicRootKeyStoreFile, "test", rootKeyStoreFile.getAbsolutePath(), PASSWORD);
    File signedKeyStoreFile = new File(folder.getRoot(), "signed.jks");
    certUtils.createSignedCert(signedKeyStoreFile, "test", PASSWORD, rootKeyStoreFile.getAbsolutePath(), PASSWORD);
    File publicSignedKeyStoreFile = new File(folder.getRoot(), "publicsigned.jks");
    certUtils.createPublicCert(publicSignedKeyStoreFile, "test", signedKeyStoreFile.getAbsolutePath(), PASSWORD);

    KeyStore rootKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    rootKeyStore.load(new FileInputStream(publicRootKeyStoreFile), new char[0]);
    KeyStore signedKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    signedKeyStore.load(new FileInputStream(publicSignedKeyStoreFile), new char[0]);
    Certificate rootCert = CertUtils.findCert(rootKeyStore);
    Certificate signedCert = CertUtils.findCert(signedKeyStore);

    try {
      signedCert.verify(signedCert.getPublicKey());
      fail("signed cert should not be able to verify itself");
    } catch (SignatureException e) {
      // expected
    }

    signedCert.verify(rootCert.getPublicKey()); // throws exception if it can't be verified
  }

  @Test
  public void signingChain() throws Exception {
    // no reason the keypair we generate for the tservers need to be able to sign anything,
    // but this is a way to make sure the private and public keys created actually correspond.
    CertUtils certUtils = getUtils();
    File rootKeyStoreFile = new File(folder.getRoot(), "root.jks");
    certUtils.createSelfSignedCert(rootKeyStoreFile, "test", PASSWORD);
    File signedCaKeyStoreFile = new File(folder.getRoot(), "signedca.jks");
    certUtils.createSignedCert(signedCaKeyStoreFile, "test", PASSWORD, rootKeyStoreFile.getAbsolutePath(), PASSWORD);
    File signedLeafKeyStoreFile = new File(folder.getRoot(), "signedleaf.jks");
    certUtils.createSignedCert(signedLeafKeyStoreFile, "test", PASSWORD, signedCaKeyStoreFile.getAbsolutePath(), PASSWORD);

    KeyStore caKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    caKeyStore.load(new FileInputStream(signedCaKeyStoreFile), PASSWORD_CHARS);
    Certificate caCert = CertUtils.findCert(caKeyStore);

    KeyStore leafKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    leafKeyStore.load(new FileInputStream(signedLeafKeyStoreFile), PASSWORD_CHARS);
    Certificate leafCert = CertUtils.findCert(leafKeyStore);

    leafCert.verify(caCert.getPublicKey()); // throws exception if it can't be verified
  }
}
- 
2.19.1.windows.1

