From a943f323b6ef9a614edee55c075eb63567b5c80a Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Fri, 19 Jul 2013 19:05:22 -0400
Subject: [PATCH] ACCUMULO-1132 Provide AuthenticationToken type for system
 user

--
 .../client/admin/SecurityOperationsImpl.java  |   2 +-
 .../core/client/impl/ConnectorImpl.java       |   5 +-
 .../client/security/tokens/PasswordToken.java |   7 +-
 .../core/security/CredentialHelper.java       |   2 +-
 .../accumulo/core/security/Credentials.java   |  18 +-
 server/pom.xml                                |   6 +
 .../server/client/ClientServiceHandler.java   |  14 +-
 .../server/client/HdfsZooInstance.java        |   3 -
 .../client/security/token/SystemToken.java    |  30 ---
 .../gc/GarbageCollectWriteAheadLogs.java      |   8 +-
 .../server/gc/SimpleGarbageCollector.java     |   9 +-
 .../server/master/LiveTServerSet.java         |  24 +-
 .../apache/accumulo/server/master/Master.java |   8 +-
 .../server/master/TabletGroupWatcher.java     |  12 +-
 .../master/balancer/TableLoadBalancer.java    |   4 +-
 .../master/balancer/TabletBalancer.java       |   8 +-
 .../master/state/MetaDataStateStore.java      |   4 +-
 .../server/master/tableOps/BulkImport.java    |   4 +-
 .../server/master/tableOps/CloneTable.java    |  10 +-
 .../server/master/tableOps/CreateTable.java   |  12 +-
 .../server/master/tableOps/DeleteTable.java   |   6 +-
 .../server/master/tableOps/ImportTable.java   |   8 +-
 .../accumulo/server/monitor/Monitor.java      |   6 +-
 .../monitor/servlets/TServersServlet.java     |  18 +-
 .../monitor/servlets/TablesServlet.java       |   6 +-
 .../server/problems/ProblemReport.java        |   6 +-
 .../server/problems/ProblemReports.java       |   8 +-
 .../security/AuditedSecurityOperation.java    |   2 +-
 .../server/security/SecurityOperation.java    | 207 ++++++++----------
 ...yConstants.java => SystemCredentials.java} | 113 ++++++----
 .../accumulo/server/tabletserver/Tablet.java  |  34 +--
 .../server/tabletserver/TabletServer.java     |  78 ++++---
 .../apache/accumulo/server/util/Admin.java    |   6 +-
 .../server/util/FindOfflineTablets.java       |   6 +-
 .../accumulo/server/util/Initialize.java      |   4 +-
 .../server/util/MetadataTableUtil.java        |  16 +-
 .../security/SystemCredentialsTest.java       |  67 ++++++
 server/src/test/resources/accumulo-site.xml   |  32 +++
 .../apache/accumulo/test/GetMasterStats.java  |   6 +-
 .../continuous/ContinuousStatsCollector.java  |   7 +-
 .../test/functional/SplitRecoveryTest.java    |  26 +--
 .../metadata/MetadataBatchScanTest.java       |   6 +-
 .../test/performance/thrift/NullTserver.java  |   4 +-
 .../test/randomwalk/concurrent/Shutdown.java  |  24 +-
 .../test/randomwalk/concurrent/StartAll.java  |   8 +-
 .../randomwalk/security/WalkingSecurity.java  |   3 +-
 46 files changed, 484 insertions(+), 413 deletions(-)
 delete mode 100644 server/src/main/java/org/apache/accumulo/server/client/security/token/SystemToken.java
 rename server/src/main/java/org/apache/accumulo/server/security/{SecurityConstants.java => SystemCredentials.java} (51%)
 create mode 100644 server/src/test/java/org/apache/accumulo/server/security/SystemCredentialsTest.java
 create mode 100644 server/src/test/resources/accumulo-site.xml

diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperationsImpl.java b/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperationsImpl.java
index 84a1ebd31..d5e1d8b3d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperationsImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperationsImpl.java
@@ -157,7 +157,7 @@ public class SecurityOperationsImpl implements SecurityOperations {
         client.changeLocalUserPassword(Tracer.traceInfo(), credentials, principal, ByteBuffer.wrap(token.getPassword()));
       }
     });
    if (this.credentials.principal.equals(principal)) {
    if (this.credentials.getPrincipal().equals(principal)) {
       this.credentials = toChange;
     }
   }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ConnectorImpl.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ConnectorImpl.java
index 170208209..3c6e44537 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ConnectorImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ConnectorImpl.java
@@ -58,9 +58,8 @@ public class ConnectorImpl extends Connector {
     
     this.credentials = cred;
     
    // hardcoded string for SYSTEM user since the definition is
    // in server code
    if (!cred.getPrincipal().equals("!SYSTEM")) {
    // Skip fail fast for system services; string literal for class name, to avoid
    if (!"org.apache.accumulo.server.security.SystemCredentials$SystemToken".equals(cred.getTokenClassName())) {
       ServerClient.execute(instance, new ClientExec<ClientService.Client>() {
         @Override
         public void execute(ClientService.Client iface) throws Exception {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/security/tokens/PasswordToken.java b/core/src/main/java/org/apache/accumulo/core/client/security/tokens/PasswordToken.java
index 50d6938c0..c39fb8d9a 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/security/tokens/PasswordToken.java
++ b/core/src/main/java/org/apache/accumulo/core/client/security/tokens/PasswordToken.java
@@ -137,15 +137,14 @@ public class PasswordToken implements AuthenticationToken {
       }
     }
   }

  
   @Override
   public void init(Properties properties) {
    if (properties.containsKey("password")){
    if (properties.containsKey("password")) {
       setPassword(CharBuffer.wrap(properties.get("password")));
    }else
    } else
       throw new IllegalArgumentException("Missing 'password' property");
   }

   
   @Override
   public Set<TokenProperty> getProperties() {
diff --git a/core/src/main/java/org/apache/accumulo/core/security/CredentialHelper.java b/core/src/main/java/org/apache/accumulo/core/security/CredentialHelper.java
index 69e3ba128..15fc47ac5 100644
-- a/core/src/main/java/org/apache/accumulo/core/security/CredentialHelper.java
++ b/core/src/main/java/org/apache/accumulo/core/security/CredentialHelper.java
@@ -77,7 +77,7 @@ public class CredentialHelper {
   }
   
   public static AuthenticationToken extractToken(TCredentials toAuth) throws AccumuloSecurityException {
    return extractToken(toAuth.tokenClassName, toAuth.getToken());
    return extractToken(toAuth.getTokenClassName(), toAuth.getToken());
   }
   
   public static TCredentials createSquelchError(String principal, AuthenticationToken token, String instanceID) {
diff --git a/core/src/main/java/org/apache/accumulo/core/security/Credentials.java b/core/src/main/java/org/apache/accumulo/core/security/Credentials.java
index 31fe18d93..2c1dd8b4e 100644
-- a/core/src/main/java/org/apache/accumulo/core/security/Credentials.java
++ b/core/src/main/java/org/apache/accumulo/core/security/Credentials.java
@@ -24,20 +24,30 @@ import org.apache.accumulo.core.security.thrift.TCredentials;
 /**
  * A wrapper for internal use. This class carries the instance, principal, and authentication token for use in the public API, in a non-serialized form. This is
  * important, so that the authentication token carried in a {@link Connector} can be destroyed, invalidating future RPC operations from that {@link Connector}.
 * <p>
 * See ACCUMULO-1312
 * 
 * @since 1.6.0
  */
 public class Credentials {
   
  private Instance instance;
   private String principal;
   private AuthenticationToken token;
   
  public Credentials(Instance instance, String principal, AuthenticationToken token) {
    this.instance = instance;
  public Credentials(String principal, AuthenticationToken token) {
     this.principal = principal;
     this.token = token;
   }
   
  public TCredentials toThrift() {
  public String getPrincipal() {
    return principal;
  }
  
  public AuthenticationToken getToken() {
    return token;
  }
  
  public TCredentials toThrift(Instance instance) {
     return CredentialHelper.createSquelchError(principal, token, instance.getInstanceID());
   }
   
diff --git a/server/pom.xml b/server/pom.xml
index 75447be0d..ff846b404 100644
-- a/server/pom.xml
++ b/server/pom.xml
@@ -124,6 +124,12 @@
     </dependency>
   </dependencies>
   <build>
    <testResources>
      <testResource>
        <filtering>true</filtering>
        <directory>src/test/resources</directory>
      </testResource>
    </testResources>
     <pluginManagement>
       <plugins>
         <plugin>
diff --git a/server/src/main/java/org/apache/accumulo/server/client/ClientServiceHandler.java b/server/src/main/java/org/apache/accumulo/server/client/ClientServiceHandler.java
index 6c3f110e1..6fd6a65ff 100644
-- a/server/src/main/java/org/apache/accumulo/server/client/ClientServiceHandler.java
++ b/server/src/main/java/org/apache/accumulo/server/client/ClientServiceHandler.java
@@ -135,14 +135,14 @@ public class ClientServiceHandler implements ClientService.Iface {
   @Override
   public void changeLocalUserPassword(TInfo tinfo, TCredentials credentials, String principal, ByteBuffer password) throws ThriftSecurityException {
     PasswordToken token = new PasswordToken(password);
    TCredentials toChange = CredentialHelper.createSquelchError(principal, token, credentials.instanceId);
    TCredentials toChange = CredentialHelper.createSquelchError(principal, token, credentials.getInstanceId());
     security.changePassword(credentials, toChange);
   }
   
   @Override
   public void createLocalUser(TInfo tinfo, TCredentials credentials, String principal, ByteBuffer password) throws ThriftSecurityException {
     PasswordToken token = new PasswordToken(password);
    TCredentials newUser = CredentialHelper.createSquelchError(principal, token, credentials.instanceId);
    TCredentials newUser = CredentialHelper.createSquelchError(principal, token, credentials.getInstanceId());
     security.createUser(credentials, newUser, new Authorizations());
   }
   
@@ -230,11 +230,10 @@ public class ClientServiceHandler implements ClientService.Iface {
   }
   
   @Override
  public List<String> bulkImportFiles(TInfo tinfo, final TCredentials tikw, final long tid, final String tableId, final List<String> files,
  public List<String> bulkImportFiles(TInfo tinfo, final TCredentials credentials, final long tid, final String tableId, final List<String> files,
       final String errorDir, final boolean setTime) throws ThriftSecurityException, ThriftTableOperationException, TException {
     try {
      final TCredentials credentials = new TCredentials(tikw);
      if (!security.hasSystemPermission(credentials, credentials.getPrincipal(), SystemPermission.SYSTEM))
      if (!security.canPerformSystemActions(credentials))
         throw new AccumuloSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
       return transactionWatcher.run(Constants.BULK_ARBITRATOR_TYPE, tid, new Callable<List<String>>() {
         @Override
@@ -281,7 +280,6 @@ public class ClientServiceHandler implements ClientService.Iface {
     }
   }
   
  @SuppressWarnings({"rawtypes", "unchecked"})
   @Override
   public boolean checkTableClass(TInfo tinfo, TCredentials credentials, String tableName, String className, String interfaceMatch) throws TException,
       ThriftTableOperationException, ThriftSecurityException {
@@ -291,7 +289,7 @@ public class ClientServiceHandler implements ClientService.Iface {
     String tableId = checkTableId(tableName, null);
     
     ClassLoader loader = getClass().getClassLoader();
    Class shouldMatch;
    Class<?> shouldMatch;
     try {
       shouldMatch = loader.loadClass(interfaceMatch);
       
@@ -307,7 +305,7 @@ public class ClientServiceHandler implements ClientService.Iface {
         currentLoader = AccumuloVFSClassLoader.getClassLoader();
       }
       
      Class test = currentLoader.loadClass(className).asSubclass(shouldMatch);
      Class<?> test = currentLoader.loadClass(className).asSubclass(shouldMatch);
       test.newInstance();
       return true;
     } catch (Exception e) {
diff --git a/server/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java b/server/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
index db5ece024..f306b8604 100644
-- a/server/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
++ b/server/src/main/java/org/apache/accumulo/server/client/HdfsZooInstance.java
@@ -145,7 +145,6 @@ public class HdfsZooInstance implements Instance {
   }
   
   @Override
  // Not really deprecated, just not for client use
   public Connector getConnector(String principal, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
     return getConnector(CredentialHelper.create(principal, token, getInstanceID()));
   }
@@ -156,13 +155,11 @@ public class HdfsZooInstance implements Instance {
   }
   
   @Override
  // Not really deprecated, just not for client use
   public Connector getConnector(String user, byte[] pass) throws AccumuloException, AccumuloSecurityException {
     return getConnector(user, new PasswordToken(pass));
   }
   
   @Override
  // Not really deprecated, just not for client use
   public Connector getConnector(String user, ByteBuffer pass) throws AccumuloException, AccumuloSecurityException {
     return getConnector(user, ByteBufferUtil.toBytes(pass));
   }
diff --git a/server/src/main/java/org/apache/accumulo/server/client/security/token/SystemToken.java b/server/src/main/java/org/apache/accumulo/server/client/security/token/SystemToken.java
deleted file mode 100644
index 72b2217ee..000000000
-- a/server/src/main/java/org/apache/accumulo/server/client/security/token/SystemToken.java
++ /dev/null
@@ -1,30 +0,0 @@
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
package org.apache.accumulo.server.client.security.token;

import org.apache.accumulo.core.client.security.tokens.PasswordToken;

/**
 * @since 1.5.0
 */

public class SystemToken extends PasswordToken {
  
  public SystemToken(byte[] systemPassword) {
    super(systemPassword);
  }
}
diff --git a/server/src/main/java/org/apache/accumulo/server/gc/GarbageCollectWriteAheadLogs.java b/server/src/main/java/org/apache/accumulo/server/gc/GarbageCollectWriteAheadLogs.java
index d50cff216..9bf7bf618 100644
-- a/server/src/main/java/org/apache/accumulo/server/gc/GarbageCollectWriteAheadLogs.java
++ b/server/src/main/java/org/apache/accumulo/server/gc/GarbageCollectWriteAheadLogs.java
@@ -40,7 +40,7 @@ import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.server.ServerConstants;
 import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.AddressUtil;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.accumulo.server.util.MetadataTableUtil.LogEntry;
@@ -165,7 +165,7 @@ public class GarbageCollectWriteAheadLogs {
           Client tserver = null;
           try {
             tserver = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
            tserver.removeLogs(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), paths2strings(entry.getValue()));
            tserver.removeLogs(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), paths2strings(entry.getValue()));
             log.debug("deleted " + entry.getValue() + " from " + entry.getKey());
             status.currentLog.deleted += entry.getValue().size();
           } catch (TException e) {
@@ -206,7 +206,7 @@ public class GarbageCollectWriteAheadLogs {
       result.add(path.toString());
     return result;
   }

  
   private static Map<String,ArrayList<Path>> mapServersToFiles(Map<Path,String> fileToServerMap) {
     Map<String,ArrayList<Path>> result = new HashMap<String,ArrayList<Path>>();
     for (Entry<Path,String> fileServer : fileToServerMap.entrySet()) {
@@ -223,7 +223,7 @@ public class GarbageCollectWriteAheadLogs {
   private static int removeMetadataEntries(Map<Path,String> fileToServerMap, Set<Path> sortedWALogs, GCStatus status) throws IOException, KeeperException,
       InterruptedException {
     int count = 0;
    Iterator<LogEntry> iterator = MetadataTableUtil.getLogEntries(SecurityConstants.getSystemCredentials());
    Iterator<LogEntry> iterator = MetadataTableUtil.getLogEntries(SystemCredentials.get().getAsThrift());
     while (iterator.hasNext()) {
       for (String filename : iterator.next().logSet) {
         Path path;
diff --git a/server/src/main/java/org/apache/accumulo/server/gc/SimpleGarbageCollector.java b/server/src/main/java/org/apache/accumulo/server/gc/SimpleGarbageCollector.java
index f18e5bc1d..de7328253 100644
-- a/server/src/main/java/org/apache/accumulo/server/gc/SimpleGarbageCollector.java
++ b/server/src/main/java/org/apache/accumulo/server/gc/SimpleGarbageCollector.java
@@ -85,7 +85,7 @@ import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.accumulo.server.master.state.tables.TableManager;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.Halt;
 import org.apache.accumulo.server.util.TServerUtils;
 import org.apache.accumulo.server.util.TabletIterator;
@@ -162,7 +162,7 @@ public class SimpleGarbageCollector implements Iface {
     if (opts.address != null)
       gc.useAddress(address);
     
    gc.init(fs, instance, SecurityConstants.getSystemCredentials(), serverConf.getConfiguration().getBoolean(Property.GC_TRASH_IGNORE));
    gc.init(fs, instance, SystemCredentials.get().getAsThrift(), serverConf.getConfiguration().getBoolean(Property.GC_TRASH_IGNORE));
     Accumulo.enableTracing(address, "gc");
     gc.run();
   }
@@ -582,8 +582,7 @@ public class SimpleGarbageCollector implements Iface {
       Map<Key,Value> tabletKeyValues = tabletIterator.next();
       
       for (Entry<Key,Value> entry : tabletKeyValues.entrySet()) {
        if (entry.getKey().getColumnFamily().equals(DataFileColumnFamily.NAME)
            || entry.getKey().getColumnFamily().equals(ScanFileColumnFamily.NAME)) {
        if (entry.getKey().getColumnFamily().equals(DataFileColumnFamily.NAME) || entry.getKey().getColumnFamily().equals(ScanFileColumnFamily.NAME)) {
           
           String cf = entry.getKey().getColumnQualifier().toString();
           String delete = cf;
@@ -638,7 +637,7 @@ public class SimpleGarbageCollector implements Iface {
     if (!offline) {
       Connector c;
       try {
        c = instance.getConnector(SecurityConstants.SYSTEM_PRINCIPAL, SecurityConstants.getSystemToken());
        c = instance.getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get().getToken());
         writer = c.createBatchWriter(MetadataTable.NAME, new BatchWriterConfig());
         rootWriter = c.createBatchWriter(RootTable.NAME, new BatchWriterConfig());
       } catch (AccumuloException e) {
diff --git a/server/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java b/server/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java
index bebff7f45..68255b879 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java
++ b/server/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java
@@ -37,7 +37,7 @@ import org.apache.accumulo.core.util.ServerServices;
 import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.AddressUtil;
 import org.apache.accumulo.server.util.Halt;
 import org.apache.accumulo.server.util.time.SimpleTimer;
@@ -83,7 +83,7 @@ public class LiveTServerSet implements Watcher {
     public void assignTablet(ZooLock lock, KeyExtent extent) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.loadTablet(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock), extent.toThrift());
        client.loadTablet(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock), extent.toThrift());
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -92,7 +92,7 @@ public class LiveTServerSet implements Watcher {
     public void unloadTablet(ZooLock lock, KeyExtent extent, boolean save) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.unloadTablet(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock), extent.toThrift(), save);
        client.unloadTablet(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock), extent.toThrift(), save);
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -107,7 +107,7 @@ public class LiveTServerSet implements Watcher {
       
       try {
         TabletClientService.Client client = ThriftUtil.createClient(new TabletClientService.Client.Factory(), transport);
        return client.getTabletServerStatus(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
        return client.getTabletServerStatus(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
       } finally {
         if (transport != null)
           transport.close();
@@ -117,7 +117,7 @@ public class LiveTServerSet implements Watcher {
     public void halt(ZooLock lock) throws TException, ThriftSecurityException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.halt(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock));
        client.halt(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock));
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -126,7 +126,7 @@ public class LiveTServerSet implements Watcher {
     public void fastHalt(ZooLock lock) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.fastHalt(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock));
        client.fastHalt(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock));
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -135,8 +135,8 @@ public class LiveTServerSet implements Watcher {
     public void flush(ZooLock lock, String tableId, byte[] startRow, byte[] endRow) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.flush(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock), tableId,
            startRow == null ? null : ByteBuffer.wrap(startRow), endRow == null ? null : ByteBuffer.wrap(endRow));
        client.flush(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock), tableId, startRow == null ? null : ByteBuffer.wrap(startRow),
            endRow == null ? null : ByteBuffer.wrap(endRow));
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -145,7 +145,7 @@ public class LiveTServerSet implements Watcher {
     public void chop(ZooLock lock, KeyExtent extent) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.chop(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock), extent.toThrift());
        client.chop(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock), extent.toThrift());
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -154,7 +154,7 @@ public class LiveTServerSet implements Watcher {
     public void splitTablet(ZooLock lock, KeyExtent extent, Text splitPoint) throws TException, ThriftSecurityException, NotServingTabletException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.splitTablet(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), extent.toThrift(),
        client.splitTablet(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), extent.toThrift(),
             ByteBuffer.wrap(splitPoint.getBytes(), 0, splitPoint.getLength()));
       } finally {
         ThriftUtil.returnClient(client);
@@ -164,7 +164,7 @@ public class LiveTServerSet implements Watcher {
     public void flushTablet(ZooLock lock, KeyExtent extent) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.flushTablet(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock), extent.toThrift());
        client.flushTablet(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock), extent.toThrift());
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -173,7 +173,7 @@ public class LiveTServerSet implements Watcher {
     public void compact(ZooLock lock, String tableId, byte[] startRow, byte[] endRow) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, conf);
       try {
        client.compact(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), lockString(lock), tableId,
        client.compact(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), lockString(lock), tableId,
             startRow == null ? null : ByteBuffer.wrap(startRow), endRow == null ? null : ByteBuffer.wrap(endRow));
       } finally {
         ThriftUtil.returnClient(client);
diff --git a/server/src/main/java/org/apache/accumulo/server/master/Master.java b/server/src/main/java/org/apache/accumulo/server/master/Master.java
index b5ffd0abe..0cb037811 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/Master.java
++ b/server/src/main/java/org/apache/accumulo/server/master/Master.java
@@ -129,8 +129,8 @@ import org.apache.accumulo.server.master.tableOps.TraceRepo;
 import org.apache.accumulo.server.master.tserverOps.ShutdownTServer;
 import org.apache.accumulo.server.monitor.Monitor;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityConstants;
 import org.apache.accumulo.server.security.SecurityOperation;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.AddressUtil;
 import org.apache.accumulo.server.util.DefaultMap;
 import org.apache.accumulo.server.util.Halt;
@@ -291,7 +291,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
           @Override
           public void run() {
             try {
              MetadataTableUtil.moveMetaDeleteMarkers(instance, SecurityConstants.getSystemCredentials());
              MetadataTableUtil.moveMetaDeleteMarkers(instance, SystemCredentials.get().getAsThrift());
               Accumulo.updateAccumuloVersion(fs);
               
               log.info("Upgrade complete");
@@ -409,7 +409,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
   }
   
   public Connector getConnector() throws AccumuloException, AccumuloSecurityException {
    return instance.getConnector(SecurityConstants.SYSTEM_PRINCIPAL, SecurityConstants.getSystemToken());
    return instance.getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get().getToken());
   }
   
   private void waitAround(EventCoordinator.Listener listener) {
@@ -1503,7 +1503,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
       }
     });
     
    TCredentials systemAuths = SecurityConstants.getSystemCredentials();
    TCredentials systemAuths = SystemCredentials.get().getAsThrift();
     watchers.add(new TabletGroupWatcher(this, new MetaDataStateStore(instance, systemAuths, this), null));
     watchers.add(new TabletGroupWatcher(this, new RootTabletStateStore(instance, systemAuths, this), watchers.get(0)));
     watchers.add(new TabletGroupWatcher(this, new ZooTabletStateStore(new ZooStore(zroot)), watchers.get(1)));
diff --git a/server/src/main/java/org/apache/accumulo/server/master/TabletGroupWatcher.java b/server/src/main/java/org/apache/accumulo/server/master/TabletGroupWatcher.java
index c0479ddb8..fb905c929 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/TabletGroupWatcher.java
++ b/server/src/main/java/org/apache/accumulo/server/master/TabletGroupWatcher.java
@@ -70,7 +70,7 @@ import org.apache.accumulo.server.master.state.TabletLocationState;
 import org.apache.accumulo.server.master.state.TabletState;
 import org.apache.accumulo.server.master.state.TabletStateStore;
 import org.apache.accumulo.server.master.state.tables.TableManager;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.TabletTime;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.hadoop.io.Text;
@@ -410,7 +410,7 @@ class TabletGroupWatcher extends Daemon {
         if (key.compareColumnFamily(DataFileColumnFamily.NAME) == 0) {
           datafiles.add(new FileRef(this.master.fs, key));
           if (datafiles.size() > 1000) {
            MetadataTableUtil.addDeleteEntries(extent, datafiles, SecurityConstants.getSystemCredentials());
            MetadataTableUtil.addDeleteEntries(extent, datafiles, SystemCredentials.get().getAsThrift());
             datafiles.clear();
           }
         } else if (TabletsSection.ServerColumnFamily.TIME_COLUMN.hasColumns(key)) {
@@ -420,12 +420,12 @@ class TabletGroupWatcher extends Daemon {
         } else if (TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.hasColumns(key)) {
           datafiles.add(new FileRef(this.master.fs, key));
           if (datafiles.size() > 1000) {
            MetadataTableUtil.addDeleteEntries(extent, datafiles, SecurityConstants.getSystemCredentials());
            MetadataTableUtil.addDeleteEntries(extent, datafiles, SystemCredentials.get().getAsThrift());
             datafiles.clear();
           }
         }
       }
      MetadataTableUtil.addDeleteEntries(extent, datafiles, SecurityConstants.getSystemCredentials());
      MetadataTableUtil.addDeleteEntries(extent, datafiles, SystemCredentials.get().getAsThrift());
       BatchWriter bw = conn.createBatchWriter(targetSystemTable, new BatchWriterConfig());
       try {
         deleteTablets(info, deleteRange, bw, conn);
@@ -448,8 +448,8 @@ class TabletGroupWatcher extends Daemon {
       } else {
         // Recreate the default tablet to hold the end of the table
         Master.log.debug("Recreating the last tablet to point to " + extent.getPrevEndRow());
        MetadataTableUtil.addTablet(new KeyExtent(extent.getTableId(), null, extent.getPrevEndRow()), Constants.DEFAULT_TABLET_LOCATION,
            SecurityConstants.getSystemCredentials(), timeType, this.master.masterLock);
        MetadataTableUtil.addTablet(new KeyExtent(extent.getTableId(), null, extent.getPrevEndRow()), Constants.DEFAULT_TABLET_LOCATION, SystemCredentials
            .get().getAsThrift(), timeType, this.master.masterLock);
       }
     } catch (Exception ex) {
       throw new AccumuloException(ex);
diff --git a/server/src/main/java/org/apache/accumulo/server/master/balancer/TableLoadBalancer.java b/server/src/main/java/org/apache/accumulo/server/master/balancer/TableLoadBalancer.java
index b9cecbf19..3e0a2bf0e 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/balancer/TableLoadBalancer.java
++ b/server/src/main/java/org/apache/accumulo/server/master/balancer/TableLoadBalancer.java
@@ -33,7 +33,7 @@ import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.master.thrift.TabletServerStatus;
 import org.apache.accumulo.server.master.state.TServerInstance;
 import org.apache.accumulo.server.master.state.TabletMigration;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
 import org.apache.log4j.Logger;
 
@@ -119,7 +119,7 @@ public class TableLoadBalancer extends TabletBalancer {
   protected TableOperations getTableOperations() {
     if (tops == null)
       try {
        tops = configuration.getInstance().getConnector(SecurityConstants.getSystemPrincipal(), SecurityConstants.getSystemToken()).tableOperations();
        tops = configuration.getInstance().getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get().getToken()).tableOperations();
       } catch (AccumuloException e) {
         log.error("Unable to access table operations from within table balancer", e);
       } catch (AccumuloSecurityException e) {
diff --git a/server/src/main/java/org/apache/accumulo/server/master/balancer/TabletBalancer.java b/server/src/main/java/org/apache/accumulo/server/master/balancer/TabletBalancer.java
index d6dce2f40..625fa4073 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/balancer/TabletBalancer.java
++ b/server/src/main/java/org/apache/accumulo/server/master/balancer/TabletBalancer.java
@@ -22,7 +22,6 @@ import java.util.Map;
 import java.util.Set;
 import java.util.SortedMap;
 
import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.master.thrift.TabletServerStatus;
@@ -33,7 +32,8 @@ import org.apache.accumulo.core.util.ThriftUtil;
 import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.accumulo.server.master.state.TServerInstance;
 import org.apache.accumulo.server.master.state.TabletMigration;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.log4j.Logger;
 import org.apache.thrift.TException;
 import org.apache.thrift.transport.TTransportException;
@@ -43,7 +43,7 @@ public abstract class TabletBalancer {
   private static final Logger log = Logger.getLogger(TabletBalancer.class);
   
   protected ServerConfiguration configuration;

  
   /**
    * Initialize the TabletBalancer. This gives the balancer the opportunity to read the configuration.
    */
@@ -98,7 +98,7 @@ public abstract class TabletBalancer {
     log.debug("Scanning tablet server " + tserver + " for table " + tableId);
     Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), tserver.getLocation(), configuration.getConfiguration());
     try {
      List<TabletStats> onlineTabletsForTable = client.getTabletStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), tableId);
      List<TabletStats> onlineTabletsForTable = client.getTabletStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), tableId);
       return onlineTabletsForTable;
     } catch (TTransportException e) {
       log.error("Unable to connect to " + tserver + ": " + e);
diff --git a/server/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java b/server/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
index b58e618a4..5cb7b0c0f 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
++ b/server/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
@@ -32,7 +32,7 @@ import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
 import org.apache.accumulo.core.security.CredentialHelper;
 import org.apache.accumulo.core.security.thrift.TCredentials;
 import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.hadoop.io.Text;
 
 public class MetaDataStateStore extends TabletStateStore {
@@ -59,7 +59,7 @@ public class MetaDataStateStore extends TabletStateStore {
   }
   
   protected MetaDataStateStore(String tableName) {
    this(HdfsZooInstance.getInstance(), SecurityConstants.getSystemCredentials(), null, tableName);
    this(HdfsZooInstance.getInstance(), SystemCredentials.get().getAsThrift(), null, tableName);
   }
   
   public MetaDataStateStore() {
diff --git a/server/src/main/java/org/apache/accumulo/server/master/tableOps/BulkImport.java b/server/src/main/java/org/apache/accumulo/server/master/tableOps/BulkImport.java
index 4f44d79ad..cfbdc97eb 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/tableOps/BulkImport.java
++ b/server/src/main/java/org/apache/accumulo/server/master/tableOps/BulkImport.java
@@ -68,7 +68,7 @@ import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.master.LiveTServerSet.TServerConnection;
 import org.apache.accumulo.server.master.Master;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.UniqueNameAllocator;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.accumulo.server.zookeeper.DistributedWorkQueue;
@@ -557,7 +557,7 @@ class LoadFiles extends MasterRepo {
               server = pair.getFirst();
               List<String> attempt = Collections.singletonList(file);
               log.debug("Asking " + pair.getFirst() + " to bulk import " + file);
              List<String> fail = client.bulkImportFiles(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), tid, tableId, attempt, errorDir, setTime);
              List<String> fail = client.bulkImportFiles(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), tid, tableId, attempt, errorDir, setTime);
               if (fail.isEmpty()) {
                 loaded.add(file);
               } else {
diff --git a/server/src/main/java/org/apache/accumulo/server/master/tableOps/CloneTable.java b/server/src/main/java/org/apache/accumulo/server/master/tableOps/CloneTable.java
index 8bf437d4e..3534a7885 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/tableOps/CloneTable.java
++ b/server/src/main/java/org/apache/accumulo/server/master/tableOps/CloneTable.java
@@ -32,7 +32,7 @@ import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.master.Master;
 import org.apache.accumulo.server.master.state.tables.TableManager;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.log4j.Logger;
 
@@ -108,14 +108,14 @@ class CloneMetadata extends MasterRepo {
     Instance instance = HdfsZooInstance.getInstance();
     // need to clear out any metadata entries for tableId just in case this
     // died before and is executing again
    MetadataTableUtil.deleteTable(cloneInfo.tableId, false, SecurityConstants.getSystemCredentials(), environment.getMasterLock());
    MetadataTableUtil.deleteTable(cloneInfo.tableId, false, SystemCredentials.get().getAsThrift(), environment.getMasterLock());
     MetadataTableUtil.cloneTable(instance, cloneInfo.srcTableId, cloneInfo.tableId);
     return new FinishCloneTable(cloneInfo);
   }
   
   @Override
   public void undo(long tid, Master environment) throws Exception {
    MetadataTableUtil.deleteTable(cloneInfo.tableId, false, SecurityConstants.getSystemCredentials(), environment.getMasterLock());
    MetadataTableUtil.deleteTable(cloneInfo.tableId, false, SystemCredentials.get().getAsThrift(), environment.getMasterLock());
   }
   
 }
@@ -183,7 +183,7 @@ class ClonePermissions extends MasterRepo {
     // give all table permissions to the creator
     for (TablePermission permission : TablePermission.values()) {
       try {
        AuditedSecurityOperation.getInstance().grantTablePermission(SecurityConstants.getSystemCredentials(), cloneInfo.user, cloneInfo.tableId, permission);
        AuditedSecurityOperation.getInstance().grantTablePermission(SystemCredentials.get().getAsThrift(), cloneInfo.user, cloneInfo.tableId, permission);
       } catch (ThriftSecurityException e) {
         Logger.getLogger(FinishCloneTable.class).error(e.getMessage(), e);
         throw e;
@@ -198,7 +198,7 @@ class ClonePermissions extends MasterRepo {
   
   @Override
   public void undo(long tid, Master environment) throws Exception {
    AuditedSecurityOperation.getInstance().deleteTable(SecurityConstants.getSystemCredentials(), cloneInfo.tableId);
    AuditedSecurityOperation.getInstance().deleteTable(SystemCredentials.get().getAsThrift(), cloneInfo.tableId);
   }
 }
 
diff --git a/server/src/main/java/org/apache/accumulo/server/master/tableOps/CreateTable.java b/server/src/main/java/org/apache/accumulo/server/master/tableOps/CreateTable.java
index d9acd8da7..2f35f9776 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/tableOps/CreateTable.java
++ b/server/src/main/java/org/apache/accumulo/server/master/tableOps/CreateTable.java
@@ -36,8 +36,8 @@ import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.master.Master;
 import org.apache.accumulo.server.master.state.tables.TableManager;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityConstants;
 import org.apache.accumulo.server.security.SecurityOperation;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.TabletTime;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.accumulo.server.util.TablePropUtil;
@@ -115,7 +115,7 @@ class PopulateMetadata extends MasterRepo {
   public Repo<Master> call(long tid, Master environment) throws Exception {
     
     KeyExtent extent = new KeyExtent(new Text(tableInfo.tableId), null, null);
    MetadataTableUtil.addTablet(extent, Constants.DEFAULT_TABLET_LOCATION, SecurityConstants.getSystemCredentials(), tableInfo.timeType,
    MetadataTableUtil.addTablet(extent, Constants.DEFAULT_TABLET_LOCATION, SystemCredentials.get().getAsThrift(), tableInfo.timeType,
         environment.getMasterLock());
     
     return new FinishCreateTable(tableInfo);
@@ -124,7 +124,7 @@ class PopulateMetadata extends MasterRepo {
   
   @Override
   public void undo(long tid, Master environment) throws Exception {
    MetadataTableUtil.deleteTable(tableInfo.tableId, false, SecurityConstants.getSystemCredentials(), environment.getMasterLock());
    MetadataTableUtil.deleteTable(tableInfo.tableId, false, SystemCredentials.get().getAsThrift(), environment.getMasterLock());
   }
   
 }
@@ -153,7 +153,7 @@ class CreateDir extends MasterRepo {
   @Override
   public void undo(long tid, Master master) throws Exception {
     VolumeManager fs = master.getFileSystem();
    for(String dir : ServerConstants.getTablesDirs()) {
    for (String dir : ServerConstants.getTablesDirs()) {
       fs.deleteRecursively(new Path(dir + "/" + tableInfo.tableId));
     }
     
@@ -225,7 +225,7 @@ class SetupPermissions extends MasterRepo {
     SecurityOperation security = AuditedSecurityOperation.getInstance();
     for (TablePermission permission : TablePermission.values()) {
       try {
        security.grantTablePermission(SecurityConstants.getSystemCredentials(), tableInfo.user, tableInfo.tableId, permission);
        security.grantTablePermission(SystemCredentials.get().getAsThrift(), tableInfo.user, tableInfo.tableId, permission);
       } catch (ThriftSecurityException e) {
         Logger.getLogger(FinishCreateTable.class).error(e.getMessage(), e);
         throw e;
@@ -240,7 +240,7 @@ class SetupPermissions extends MasterRepo {
   
   @Override
   public void undo(long tid, Master env) throws Exception {
    AuditedSecurityOperation.getInstance().deleteTable(SecurityConstants.getSystemCredentials(), tableInfo.tableId);
    AuditedSecurityOperation.getInstance().deleteTable(SystemCredentials.get().getAsThrift(), tableInfo.tableId);
   }
   
 }
diff --git a/server/src/main/java/org/apache/accumulo/server/master/tableOps/DeleteTable.java b/server/src/main/java/org/apache/accumulo/server/master/tableOps/DeleteTable.java
index 7d6186e86..3786d27cf 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/tableOps/DeleteTable.java
++ b/server/src/main/java/org/apache/accumulo/server/master/tableOps/DeleteTable.java
@@ -47,7 +47,7 @@ import org.apache.accumulo.server.master.state.TabletState;
 import org.apache.accumulo.server.master.state.tables.TableManager;
 import org.apache.accumulo.server.problems.ProblemReports;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.Text;
@@ -155,7 +155,7 @@ class CleanUp extends MasterRepo {
       // Intentionally do not pass master lock. If master loses lock, this operation may complete before master can kill itself.
       // If the master lock passed to deleteTable, it is possible that the delete mutations will be dropped. If the delete operations
       // are dropped and the operation completes, then the deletes will not be repeated.
      MetadataTableUtil.deleteTable(tableId, refCount != 0, SecurityConstants.getSystemCredentials(), null);
      MetadataTableUtil.deleteTable(tableId, refCount != 0, SystemCredentials.get().getAsThrift(), null);
     } catch (Exception e) {
       log.error("error deleting " + tableId + " from metadata table", e);
     }
@@ -189,7 +189,7 @@ class CleanUp extends MasterRepo {
     
     // remove any permissions associated with this table
     try {
      AuditedSecurityOperation.getInstance().deleteTable(SecurityConstants.getSystemCredentials(), tableId);
      AuditedSecurityOperation.getInstance().deleteTable(SystemCredentials.get().getAsThrift(), tableId);
     } catch (ThriftSecurityException e) {
       log.error(e.getMessage(), e);
     }
diff --git a/server/src/main/java/org/apache/accumulo/server/master/tableOps/ImportTable.java b/server/src/main/java/org/apache/accumulo/server/master/tableOps/ImportTable.java
index ae6930bd1..364c2676d 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/tableOps/ImportTable.java
++ b/server/src/main/java/org/apache/accumulo/server/master/tableOps/ImportTable.java
@@ -59,8 +59,8 @@ import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.master.Master;
 import org.apache.accumulo.server.master.state.tables.TableManager;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityConstants;
 import org.apache.accumulo.server.security.SecurityOperation;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.UniqueNameAllocator;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.accumulo.server.util.TablePropUtil;
@@ -293,7 +293,7 @@ class PopulateMetadataTable extends MasterRepo {
   
   @Override
   public void undo(long tid, Master environment) throws Exception {
    MetadataTableUtil.deleteTable(tableInfo.tableId, false, SecurityConstants.getSystemCredentials(), environment.getMasterLock());
    MetadataTableUtil.deleteTable(tableInfo.tableId, false, SystemCredentials.get().getAsThrift(), environment.getMasterLock());
   }
 }
 
@@ -484,7 +484,7 @@ class ImportSetupPermissions extends MasterRepo {
     SecurityOperation security = AuditedSecurityOperation.getInstance();
     for (TablePermission permission : TablePermission.values()) {
       try {
        security.grantTablePermission(SecurityConstants.getSystemCredentials(), tableInfo.user, tableInfo.tableId, permission);
        security.grantTablePermission(SystemCredentials.get().getAsThrift(), tableInfo.user, tableInfo.tableId, permission);
       } catch (ThriftSecurityException e) {
         Logger.getLogger(ImportSetupPermissions.class).error(e.getMessage(), e);
         throw e;
@@ -499,7 +499,7 @@ class ImportSetupPermissions extends MasterRepo {
   
   @Override
   public void undo(long tid, Master env) throws Exception {
    AuditedSecurityOperation.getInstance().deleteTable(SecurityConstants.getSystemCredentials(), tableInfo.tableId);
    AuditedSecurityOperation.getInstance().deleteTable(SystemCredentials.get().getAsThrift(), tableInfo.tableId);
   }
 }
 
diff --git a/server/src/main/java/org/apache/accumulo/server/monitor/Monitor.java b/server/src/main/java/org/apache/accumulo/server/monitor/Monitor.java
index 56e473a5a..5957f261f 100644
-- a/server/src/main/java/org/apache/accumulo/server/monitor/Monitor.java
++ b/server/src/main/java/org/apache/accumulo/server/monitor/Monitor.java
@@ -70,7 +70,7 @@ import org.apache.accumulo.server.monitor.servlets.trace.ShowTrace;
 import org.apache.accumulo.server.monitor.servlets.trace.Summary;
 import org.apache.accumulo.server.problems.ProblemReports;
 import org.apache.accumulo.server.problems.ProblemType;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.EmbeddedWebServer;
 import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.log4j.Logger;
@@ -292,7 +292,7 @@ public class Monitor {
         try {
           client = MasterClient.getConnection(HdfsZooInstance.getInstance());
           if (client != null) {
            mmi = client.getMasterStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
            mmi = client.getMasterStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
             retry = false;
           } else {
             mmi = null;
@@ -432,7 +432,7 @@ public class Monitor {
           address = new ServerServices(new String(zk.getData(path + "/" + locks.get(0), null, null))).getAddress(Service.GC_CLIENT);
           GCMonitorService.Client client = ThriftUtil.getClient(new GCMonitorService.Client.Factory(), address, config.getConfiguration());
           try {
            result = client.getStatus(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
            result = client.getStatus(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
           } finally {
             ThriftUtil.returnClient(client);
           }
diff --git a/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TServersServlet.java b/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TServersServlet.java
index 095725efb..848460887 100644
-- a/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TServersServlet.java
++ b/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TServersServlet.java
@@ -27,7 +27,6 @@ import java.util.Map.Entry;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.master.thrift.DeadServer;
 import org.apache.accumulo.core.master.thrift.MasterMonitorInfo;
@@ -51,8 +50,9 @@ import org.apache.accumulo.server.monitor.util.celltypes.PercentageType;
 import org.apache.accumulo.server.monitor.util.celltypes.ProgressChartType;
 import org.apache.accumulo.server.monitor.util.celltypes.TServerLinkType;
 import org.apache.accumulo.server.monitor.util.celltypes.TableLinkType;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.TabletStatsKeeper;
import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.commons.codec.binary.Base64;
 
 public class TServersServlet extends BasicServlet {
@@ -126,9 +126,9 @@ public class TServersServlet extends BasicServlet {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, Monitor.getSystemConfiguration());
       try {
         for (String tableId : Monitor.getMmi().tableMap.keySet()) {
          tsStats.addAll(client.getTabletStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials(), tableId));
          tsStats.addAll(client.getTabletStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift(), tableId));
         }
        historical = client.getHistoricalStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
        historical = client.getHistoricalStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
       } finally {
         ThriftUtil.returnClient(client);
       }
@@ -239,12 +239,10 @@ public class TServersServlet extends BasicServlet {
     
     opHistoryDetails.addRow("Split", historical.splits.num, historical.splits.fail, null, null,
         historical.splits.num != 0 ? (historical.splits.elapsed / historical.splits.num) : null, splitStdDev, historical.splits.elapsed);
    opHistoryDetails.addRow("Major&nbsp;Compaction", total.majors.num, total.majors.fail,
        total.majors.num != 0 ? (total.majors.queueTime / total.majors.num) : null, majorQueueStdDev,
        total.majors.num != 0 ? (total.majors.elapsed / total.majors.num) : null, majorStdDev, total.majors.elapsed);
    opHistoryDetails.addRow("Minor&nbsp;Compaction", total.minors.num, total.minors.fail,
        total.minors.num != 0 ? (total.minors.queueTime / total.minors.num) : null, minorQueueStdDev,
        total.minors.num != 0 ? (total.minors.elapsed / total.minors.num) : null, minorStdDev, total.minors.elapsed);
    opHistoryDetails.addRow("Major&nbsp;Compaction", total.majors.num, total.majors.fail, total.majors.num != 0 ? (total.majors.queueTime / total.majors.num)
        : null, majorQueueStdDev, total.majors.num != 0 ? (total.majors.elapsed / total.majors.num) : null, majorStdDev, total.majors.elapsed);
    opHistoryDetails.addRow("Minor&nbsp;Compaction", total.minors.num, total.minors.fail, total.minors.num != 0 ? (total.minors.queueTime / total.minors.num)
        : null, minorQueueStdDev, total.minors.num != 0 ? (total.minors.elapsed / total.minors.num) : null, minorStdDev, total.minors.elapsed);
     opHistoryDetails.generate(req, sb);
   }
   
diff --git a/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TablesServlet.java b/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TablesServlet.java
index 127989c2e..85d17ff5c 100644
-- a/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TablesServlet.java
++ b/server/src/main/java/org/apache/accumulo/server/monitor/servlets/TablesServlet.java
@@ -47,7 +47,7 @@ import org.apache.accumulo.server.monitor.util.celltypes.DurationType;
 import org.apache.accumulo.server.monitor.util.celltypes.NumberType;
 import org.apache.accumulo.server.monitor.util.celltypes.TableLinkType;
 import org.apache.accumulo.server.monitor.util.celltypes.TableStateType;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.hadoop.io.Text;
 
 public class TablesServlet extends BasicServlet {
@@ -151,8 +151,8 @@ public class TablesServlet extends BasicServlet {
       locs.add(instance.getRootTabletLocation());
     } else {
       String systemTableName = MetadataTable.ID.equals(tableId) ? RootTable.NAME : MetadataTable.NAME;
      MetaDataTableScanner scanner = new MetaDataTableScanner(instance, SecurityConstants.getSystemCredentials(), new Range(KeyExtent.getMetadataEntry(
          new Text(tableId), new Text()), KeyExtent.getMetadataEntry(new Text(tableId), null)), systemTableName);
      MetaDataTableScanner scanner = new MetaDataTableScanner(instance, SystemCredentials.get().getAsThrift(), new Range(KeyExtent.getMetadataEntry(new Text(
          tableId), new Text()), KeyExtent.getMetadataEntry(new Text(tableId), null)), systemTableName);
       
       while (scanner.hasNext()) {
         TabletLocationState state = scanner.next();
diff --git a/server/src/main/java/org/apache/accumulo/server/problems/ProblemReport.java b/server/src/main/java/org/apache/accumulo/server/problems/ProblemReport.java
index a34de9fad..530ef764f 100644
-- a/server/src/main/java/org/apache/accumulo/server/problems/ProblemReport.java
++ b/server/src/main/java/org/apache/accumulo/server/problems/ProblemReport.java
@@ -34,7 +34,7 @@ import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
 import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
 import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.hadoop.io.Text;
@@ -125,13 +125,13 @@ public class ProblemReport {
   void removeFromMetadataTable() throws Exception {
     Mutation m = new Mutation(new Text("~err_" + tableName));
     m.putDelete(new Text(problemType.name()), new Text(resource));
    MetadataTableUtil.getMetadataTable(SecurityConstants.getSystemCredentials()).update(m);
    MetadataTableUtil.getMetadataTable(SystemCredentials.get().getAsThrift()).update(m);
   }
   
   void saveToMetadataTable() throws Exception {
     Mutation m = new Mutation(new Text("~err_" + tableName));
     m.put(new Text(problemType.name()), new Text(resource), new Value(encode()));
    MetadataTableUtil.getMetadataTable(SecurityConstants.getSystemCredentials()).update(m);
    MetadataTableUtil.getMetadataTable(SystemCredentials.get().getAsThrift()).update(m);
   }
   
   void removeFromZooKeeper() throws Exception {
diff --git a/server/src/main/java/org/apache/accumulo/server/problems/ProblemReports.java b/server/src/main/java/org/apache/accumulo/server/problems/ProblemReports.java
index 5b82621c0..5422e903a 100644
-- a/server/src/main/java/org/apache/accumulo/server/problems/ProblemReports.java
++ b/server/src/main/java/org/apache/accumulo/server/problems/ProblemReports.java
@@ -47,7 +47,7 @@ import org.apache.accumulo.core.util.NamingThreadFactory;
 import org.apache.accumulo.core.zookeeper.ZooUtil;
 import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
 import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.MetadataTableUtil;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.commons.collections.map.LRUMap;
@@ -155,7 +155,7 @@ public class ProblemReports implements Iterable<ProblemReport> {
       return;
     }
     
    Connector connector = HdfsZooInstance.getInstance().getConnector(SecurityConstants.getSystemPrincipal(), SecurityConstants.getSystemToken());
    Connector connector = HdfsZooInstance.getInstance().getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get().getToken());
     Scanner scanner = connector.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
     scanner.addScanIterator(new IteratorSetting(1, "keys-only", SortedKeyIterator.class));
     
@@ -174,7 +174,7 @@ public class ProblemReports implements Iterable<ProblemReport> {
     }
     
     if (hasProblems)
      MetadataTableUtil.getMetadataTable(SecurityConstants.getSystemCredentials()).update(delMut);
      MetadataTableUtil.getMetadataTable(SystemCredentials.get().getAsThrift()).update(delMut);
   }
   
   public Iterator<ProblemReport> iterator(final String table) {
@@ -210,7 +210,7 @@ public class ProblemReports implements Iterable<ProblemReport> {
           if (iter2 == null) {
             try {
               if ((table == null || !table.equals(MetadataTable.ID)) && iter1Count == 0) {
                Connector connector = HdfsZooInstance.getInstance().getConnector(SecurityConstants.getSystemPrincipal(), SecurityConstants.getSystemToken());
                Connector connector = HdfsZooInstance.getInstance().getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get().getToken());
                 Scanner scanner = connector.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
                 
                 scanner.setTimeout(3, TimeUnit.SECONDS);
diff --git a/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java b/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
index 125915b28..a74f58438 100644
-- a/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
++ b/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
@@ -92,7 +92,7 @@ public class AuditedSecurityOperation extends SecurityOperation {
   
   // Is INFO the right level to check? Do we even need that check?
   private static boolean shouldAudit(TCredentials credentials) {
    return !credentials.getPrincipal().equals(SecurityConstants.SYSTEM_PRINCIPAL);
    return !SystemCredentials.get().getToken().getClass().getName().equals(credentials.getTokenClassName());
   }
   
   /*
diff --git a/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java b/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
index e948894a6..2b9833188 100644
-- a/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
++ b/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
@@ -103,12 +103,7 @@ public class SecurityOperation {
     return toRet;
   }
   
  /**
   * 
   * @deprecated not for client use
   */
  @Deprecated
  public SecurityOperation(String instanceId) {
  protected SecurityOperation(String instanceId) {
     ZKUserPath = Constants.ZROOT + "/" + instanceId + "/users";
     zooCache = new ZooCache();
   }
@@ -128,7 +123,7 @@ public class SecurityOperation {
   public void initializeSecurity(TCredentials credentials, String rootPrincipal, byte[] token) throws AccumuloSecurityException, ThriftSecurityException {
     authenticate(credentials);
     
    if (!credentials.getPrincipal().equals(SecurityConstants.SYSTEM_PRINCIPAL))
    if (!isSystemUser(credentials))
       throw new AccumuloSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
     
     authenticator.initializeSecurity(credentials, rootPrincipal, token);
@@ -148,27 +143,34 @@ public class SecurityOperation {
     return rootUserName;
   }
   
  public boolean isSystemUser(TCredentials credentials) {
    return SystemCredentials.get().getToken().getClass().getName().equals(credentials.getTokenClassName());
  }
  
   private void authenticate(TCredentials credentials) throws ThriftSecurityException {
     if (!credentials.getInstanceId().equals(HdfsZooInstance.getInstance().getInstanceID()))
       throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.INVALID_INSTANCEID);
     
    if (SecurityConstants.getSystemCredentials().equals(credentials))
      return;
    else if (credentials.getPrincipal().equals(SecurityConstants.SYSTEM_PRINCIPAL)) {
      throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.BAD_CREDENTIALS);
    }
    
    try {
      AuthenticationToken token = reassembleToken(credentials);
      if (!authenticator.authenticateUser(credentials.getPrincipal(), token)) {
        throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.BAD_CREDENTIALS);
    if (isSystemUser(credentials)) {
      authenticateSystemUser(credentials);
    } else {
      try {
        AuthenticationToken token = reassembleToken(credentials);
        if (!authenticator.authenticateUser(credentials.getPrincipal(), token)) {
          throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.BAD_CREDENTIALS);
        }
      } catch (AccumuloSecurityException e) {
        log.debug(e);
        throw e.asThriftException();
       }
    } catch (AccumuloSecurityException e) {
      log.debug(e);
      throw e.asThriftException();
     }
   }
   
  private void authenticateSystemUser(TCredentials credentials) throws ThriftSecurityException {
    if (SystemCredentials.get().getToken().equals(credentials.getToken()))
      throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.BAD_CREDENTIALS);
  }
  
   public boolean canAskAboutUser(TCredentials credentials, String user) throws ThriftSecurityException {
     // Authentication done in canPerformSystemActions
     if (!(canPerformSystemActions(credentials) || credentials.getPrincipal().equals(user)))
@@ -178,7 +180,7 @@ public class SecurityOperation {
   
   public boolean authenticateUser(TCredentials credentials, TCredentials toAuth) throws ThriftSecurityException {
     canAskAboutUser(credentials, toAuth.getPrincipal());
    // User is already authenticated from canAskAboutUser, this gets around issues with !SYSTEM user
    // User is already authenticated from canAskAboutUser
     if (credentials.equals(toAuth))
       return true;
     try {
@@ -189,11 +191,6 @@ public class SecurityOperation {
     }
   }
   
  /**
   * @param toAuth
   * @return
   * @throws AccumuloSecurityException
   */
   private AuthenticationToken reassembleToken(TCredentials toAuth) throws AccumuloSecurityException {
     String tokenClass = toAuth.getTokenClassName();
     if (authenticator.validTokenClass(tokenClass)) {
@@ -207,13 +204,9 @@ public class SecurityOperation {
     
     targetUserExists(user);
     
    if (!credentials.getPrincipal().equals(user) && !hasSystemPermission(credentials.getPrincipal(), SystemPermission.SYSTEM, false))
    if (!credentials.getPrincipal().equals(user) && !hasSystemPermission(credentials, SystemPermission.SYSTEM, false))
       throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
     
    // system user doesn't need record-level authorizations for the tables it reads (for now)
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
      return Authorizations.EMPTY;
    
     try {
       return authorizor.getCachedUserAuthorizations(user);
     } catch (AccumuloSecurityException e) {
@@ -222,6 +215,11 @@ public class SecurityOperation {
   }
   
   public Authorizations getUserAuthorizations(TCredentials credentials) throws ThriftSecurityException {
    // system user doesn't need record-level authorizations for the tables it reads
    if (isSystemUser(credentials)) {
      authenticate(credentials);
      return Authorizations.EMPTY;
    }
     return getUserAuthorizations(credentials, credentials.getPrincipal());
   }
   
@@ -230,8 +228,20 @@ public class SecurityOperation {
    * 
    * @return true if a user exists and has permission; false otherwise
    */
  private boolean hasSystemPermission(String user, SystemPermission permission, boolean useCached) throws ThriftSecurityException {
    if (user.equals(getRootUsername()) || user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
  private boolean hasSystemPermission(TCredentials credentials, SystemPermission permission, boolean useCached) throws ThriftSecurityException {
    if (isSystemUser(credentials))
      return true;
    return _hasSystemPermission(credentials.getPrincipal(), permission, useCached);
  }
  
  /**
   * Checks if a user has a system permission<br/>
   * This cannot check if a system user has permission.
   * 
   * @return true if a user exists and has permission; false otherwise
   */
  private boolean _hasSystemPermission(String user, SystemPermission permission, boolean useCached) throws ThriftSecurityException {
    if (user.equals(getRootUsername()))
       return true;
     
     targetUserExists(user);
@@ -250,10 +260,19 @@ public class SecurityOperation {
    * 
    * @return true if a user exists and has permission; false otherwise
    */
  protected boolean hasTablePermission(String user, String table, TablePermission permission, boolean useCached) throws ThriftSecurityException {
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
  protected boolean hasTablePermission(TCredentials credentials, String table, TablePermission permission, boolean useCached) throws ThriftSecurityException {
    if (isSystemUser(credentials))
       return true;
    
    return _hasTablePermission(credentials.getPrincipal(), table, permission, useCached);
  }
  
  /**
   * Checks if a user has a table permission<br/>
   * This cannot check if a system user has permission.
   * 
   * @return true if a user exists and has permission; false otherwise
   */
  protected boolean _hasTablePermission(String user, String table, TablePermission permission, boolean useCached) throws ThriftSecurityException {
     targetUserExists(user);
     
     if ((table.equals(MetadataTable.ID) || table.equals(RootTable.ID)) && permission.equals(TablePermission.READ))
@@ -273,16 +292,14 @@ public class SecurityOperation {
   // some people just aren't allowed to ask about other users; here are those who can ask
   private boolean canAskAboutOtherUsers(TCredentials credentials, String user) throws ThriftSecurityException {
     authenticate(credentials);
    return credentials.getPrincipal().equals(user) || hasSystemPermission(credentials.getPrincipal(), SystemPermission.SYSTEM, false)
        || hasSystemPermission(credentials.getPrincipal(), SystemPermission.CREATE_USER, false)
        || hasSystemPermission(credentials.getPrincipal(), SystemPermission.ALTER_USER, false)
        || hasSystemPermission(credentials.getPrincipal(), SystemPermission.DROP_USER, false);
    return credentials.getPrincipal().equals(user) || hasSystemPermission(credentials, SystemPermission.SYSTEM, false)
        || hasSystemPermission(credentials, SystemPermission.CREATE_USER, false) || hasSystemPermission(credentials, SystemPermission.ALTER_USER, false)
        || hasSystemPermission(credentials, SystemPermission.DROP_USER, false);
   }
   
   private void targetUserExists(String user) throws ThriftSecurityException {
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL) || user.equals(getRootUsername()))
    if (user.equals(getRootUsername()))
       return;
    
     try {
       if (!authenticator.userExists(user))
         throw new ThriftSecurityException(user, SecurityErrorCode.USER_DOESNT_EXIST);
@@ -293,7 +310,7 @@ public class SecurityOperation {
   
   public boolean canScan(TCredentials credentials, String table) throws ThriftSecurityException {
     authenticate(credentials);
    return hasTablePermission(credentials.getPrincipal(), table, TablePermission.READ, true);
    return hasTablePermission(credentials, table, TablePermission.READ, true);
   }
   
   public boolean canScan(TCredentials credentials, String table, TRange range, List<TColumn> columns, List<IterInfo> ssiList,
@@ -308,14 +325,13 @@ public class SecurityOperation {
   
   public boolean canWrite(TCredentials credentials, String table) throws ThriftSecurityException {
     authenticate(credentials);
    return hasTablePermission(credentials.getPrincipal(), table, TablePermission.WRITE, true);
    return hasTablePermission(credentials, table, TablePermission.WRITE, true);
   }
   
   public boolean canSplitTablet(TCredentials credentials, String table) throws ThriftSecurityException {
     authenticate(credentials);
    return hasSystemPermission(credentials.getPrincipal(), SystemPermission.ALTER_TABLE, false)
        || hasSystemPermission(credentials.getPrincipal(), SystemPermission.SYSTEM, false)
        || hasTablePermission(credentials.getPrincipal(), table, TablePermission.ALTER_TABLE, false);
    return hasSystemPermission(credentials, SystemPermission.ALTER_TABLE, false) || hasSystemPermission(credentials, SystemPermission.SYSTEM, false)
        || hasTablePermission(credentials, table, TablePermission.ALTER_TABLE, false);
   }
   
   /**
@@ -323,19 +339,17 @@ public class SecurityOperation {
    */
   public boolean canPerformSystemActions(TCredentials credentials) throws ThriftSecurityException {
     authenticate(credentials);
    return hasSystemPermission(credentials.getPrincipal(), SystemPermission.SYSTEM, false);
    return hasSystemPermission(credentials, SystemPermission.SYSTEM, false);
   }
   
   public boolean canFlush(TCredentials c, String tableId) throws ThriftSecurityException {
     authenticate(c);
    return hasTablePermission(c.getPrincipal(), tableId, TablePermission.WRITE, false)
        || hasTablePermission(c.getPrincipal(), tableId, TablePermission.ALTER_TABLE, false);
    return hasTablePermission(c, tableId, TablePermission.WRITE, false) || hasTablePermission(c, tableId, TablePermission.ALTER_TABLE, false);
   }
   
   public boolean canAlterTable(TCredentials c, String tableId) throws ThriftSecurityException {
     authenticate(c);
    return hasTablePermission(c.getPrincipal(), tableId, TablePermission.ALTER_TABLE, false)
        || hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false);
    return hasTablePermission(c, tableId, TablePermission.ALTER_TABLE, false) || hasSystemPermission(c, SystemPermission.ALTER_TABLE, false);
   }
   
   public boolean canCreateTable(TCredentials c, String tableName) throws ThriftSecurityException {
@@ -344,42 +358,39 @@ public class SecurityOperation {
   
   public boolean canCreateTable(TCredentials c) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.CREATE_TABLE, false);
    return hasSystemPermission(c, SystemPermission.CREATE_TABLE, false);
   }
   
   public boolean canRenameTable(TCredentials c, String tableId, String oldTableName, String newTableName) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c.getPrincipal(), tableId, TablePermission.ALTER_TABLE, false);
    return hasSystemPermission(c, SystemPermission.ALTER_TABLE, false) || hasTablePermission(c, tableId, TablePermission.ALTER_TABLE, false);
   }
   
   public boolean canCloneTable(TCredentials c, String tableId, String tableName) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.CREATE_TABLE, false)
        && hasTablePermission(c.getPrincipal(), tableId, TablePermission.READ, false);
    return hasSystemPermission(c, SystemPermission.CREATE_TABLE, false) && hasTablePermission(c, tableId, TablePermission.READ, false);
   }
   
   public boolean canDeleteTable(TCredentials c, String tableId) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.DROP_TABLE, false)
        || hasTablePermission(c.getPrincipal(), tableId, TablePermission.DROP_TABLE, false);
    return hasSystemPermission(c, SystemPermission.DROP_TABLE, false) || hasTablePermission(c, tableId, TablePermission.DROP_TABLE, false);
   }
   
   public boolean canOnlineOfflineTable(TCredentials c, String tableId, TableOperation op) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.SYSTEM, false) || hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c.getPrincipal(), tableId, TablePermission.ALTER_TABLE, false);
    return hasSystemPermission(c, SystemPermission.SYSTEM, false) || hasSystemPermission(c, SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c, tableId, TablePermission.ALTER_TABLE, false);
   }
   
   public boolean canMerge(TCredentials c, String tableId) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.SYSTEM, false) || hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c.getPrincipal(), tableId, TablePermission.ALTER_TABLE, false);
    return hasSystemPermission(c, SystemPermission.SYSTEM, false) || hasSystemPermission(c, SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c, tableId, TablePermission.ALTER_TABLE, false);
   }
   
   public boolean canDeleteRange(TCredentials c, String tableId, String tableName, Text startRow, Text endRow) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.SYSTEM, false) || hasTablePermission(c.getPrincipal(), tableId, TablePermission.WRITE, false);
    return hasSystemPermission(c, SystemPermission.SYSTEM, false) || hasTablePermission(c, tableId, TablePermission.WRITE, false);
   }
   
   public boolean canBulkImport(TCredentials c, String tableId, String tableName, String dir, String failDir) throws ThriftSecurityException {
@@ -388,98 +399,66 @@ public class SecurityOperation {
   
   public boolean canBulkImport(TCredentials c, String tableId) throws ThriftSecurityException {
     authenticate(c);
    return hasTablePermission(c.getPrincipal(), tableId, TablePermission.BULK_IMPORT, false);
    return hasTablePermission(c, tableId, TablePermission.BULK_IMPORT, false);
   }
   
   public boolean canCompact(TCredentials c, String tableId) throws ThriftSecurityException {
     authenticate(c);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c.getPrincipal(), tableId, TablePermission.ALTER_TABLE, false)
        || hasTablePermission(c.getPrincipal(), tableId, TablePermission.WRITE, false);
    return hasSystemPermission(c, SystemPermission.ALTER_TABLE, false) || hasTablePermission(c, tableId, TablePermission.ALTER_TABLE, false)
        || hasTablePermission(c, tableId, TablePermission.WRITE, false);
   }
   
   public boolean canChangeAuthorizations(TCredentials c, String user) throws ThriftSecurityException {
     authenticate(c);
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
      throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    return hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_USER, false);
    return hasSystemPermission(c, SystemPermission.ALTER_USER, false);
   }
   
   public boolean canChangePassword(TCredentials c, String user) throws ThriftSecurityException {
     authenticate(c);
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
      throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    return c.getPrincipal().equals(user) || hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_USER, false);
    return c.getPrincipal().equals(user) || hasSystemPermission(c, SystemPermission.ALTER_USER, false);
   }
   
   public boolean canCreateUser(TCredentials c, String user) throws ThriftSecurityException {
     authenticate(c);
    
    // don't allow creating a user with the same name as system user
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
      throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    
    return hasSystemPermission(c.getPrincipal(), SystemPermission.CREATE_USER, false);
    return hasSystemPermission(c, SystemPermission.CREATE_USER, false);
   }
   
   public boolean canDropUser(TCredentials c, String user) throws ThriftSecurityException {
     authenticate(c);
    
    // can't delete root or system users
    if (user.equals(getRootUsername()) || user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
    if (user.equals(getRootUsername()))
       throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    
    return hasSystemPermission(c.getPrincipal(), SystemPermission.DROP_USER, false);
    return hasSystemPermission(c, SystemPermission.DROP_USER, false);
   }
   
   public boolean canGrantSystem(TCredentials c, String user, SystemPermission sysPerm) throws ThriftSecurityException {
     authenticate(c);
    
    // can't modify system user
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
      throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    
     // can't grant GRANT
     if (sysPerm.equals(SystemPermission.GRANT))
       throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.GRANT_INVALID);
    
    return hasSystemPermission(c.getPrincipal(), SystemPermission.GRANT, false);
    return hasSystemPermission(c, SystemPermission.GRANT, false);
   }
   
   public boolean canGrantTable(TCredentials c, String user, String table) throws ThriftSecurityException {
     authenticate(c);
    
    // can't modify system user
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
      throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    
    return hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c.getPrincipal(), table, TablePermission.GRANT, false);
    return hasSystemPermission(c, SystemPermission.ALTER_TABLE, false) || hasTablePermission(c, table, TablePermission.GRANT, false);
   }
   
   public boolean canRevokeSystem(TCredentials c, String user, SystemPermission sysPerm) throws ThriftSecurityException {
     authenticate(c);
    
    // can't modify system or root user
    if (user.equals(getRootUsername()) || user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
    // can't modify root user
    if (user.equals(getRootUsername()))
       throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
     
     // can't revoke GRANT
     if (sysPerm.equals(SystemPermission.GRANT))
       throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.GRANT_INVALID);
     
    return hasSystemPermission(c.getPrincipal(), SystemPermission.GRANT, false);
    return hasSystemPermission(c, SystemPermission.GRANT, false);
   }
   
   public boolean canRevokeTable(TCredentials c, String user, String table) throws ThriftSecurityException {
     authenticate(c);
    
    // can't modify system user
    if (user.equals(SecurityConstants.SYSTEM_PRINCIPAL))
      throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    
    return hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false)
        || hasTablePermission(c.getPrincipal(), table, TablePermission.GRANT, false);
    return hasSystemPermission(c, SystemPermission.ALTER_TABLE, false) || hasTablePermission(c, table, TablePermission.GRANT, false);
   }
   
   public void changeAuthorizations(TCredentials credentials, String user, Authorizations authorizations) throws ThriftSecurityException {
@@ -602,13 +581,13 @@ public class SecurityOperation {
   public boolean hasSystemPermission(TCredentials credentials, String user, SystemPermission permissionById) throws ThriftSecurityException {
     if (!canAskAboutOtherUsers(credentials, user))
       throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    return hasSystemPermission(user, permissionById, false);
    return _hasSystemPermission(user, permissionById, false);
   }
   
   public boolean hasTablePermission(TCredentials credentials, String user, String tableId, TablePermission permissionById) throws ThriftSecurityException {
     if (!canAskAboutOtherUsers(credentials, user))
       throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    return hasTablePermission(user, tableId, permissionById, false);
    return _hasTablePermission(user, tableId, permissionById, false);
   }
   
   public Set<String> listUsers(TCredentials credentials) throws ThriftSecurityException {
@@ -635,11 +614,11 @@ public class SecurityOperation {
   
   public boolean canExport(TCredentials credentials, String tableId, String tableName, String exportDir) throws ThriftSecurityException {
     authenticate(credentials);
    return hasTablePermission(credentials.getPrincipal(), tableId, TablePermission.READ, false);
    return hasTablePermission(credentials, tableId, TablePermission.READ, false);
   }
   
   public boolean canImport(TCredentials credentials, String tableName, String importDir) throws ThriftSecurityException {
     authenticate(credentials);
    return hasSystemPermission(credentials.getPrincipal(), SystemPermission.CREATE_TABLE, false);
    return hasSystemPermission(credentials, SystemPermission.CREATE_TABLE, false);
   }
 }
diff --git a/server/src/main/java/org/apache/accumulo/server/security/SecurityConstants.java b/server/src/main/java/org/apache/accumulo/server/security/SystemCredentials.java
similarity index 51%
rename from server/src/main/java/org/apache/accumulo/server/security/SecurityConstants.java
rename to server/src/main/java/org/apache/accumulo/server/security/SystemCredentials.java
index 5c42a6900..f30419afa 100644
-- a/server/src/main/java/org/apache/accumulo/server/security/SecurityConstants.java
++ b/server/src/main/java/org/apache/accumulo/server/security/SystemCredentials.java
@@ -28,71 +28,76 @@ import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.security.CredentialHelper;
import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.security.thrift.TCredentials;
 import org.apache.accumulo.server.ServerConstants;
 import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.hadoop.io.Writable;
 
public class SecurityConstants {
  private static SecurityPermission SYSTEM_CREDENTIALS_PERMISSION = new SecurityPermission("systemCredentialsPermission");
  static Logger log = Logger.getLogger(SecurityConstants.class);
/**
 * Credentials for the system services.
 * 
 * @since 1.6.0
 */
public final class SystemCredentials extends Credentials {
  
  private static final SecurityPermission SYSTEM_CREDENTIALS_PERMISSION = new SecurityPermission("systemCredentialsPermission");
   
  public static final String SYSTEM_PRINCIPAL = "!SYSTEM";
  private static final AuthenticationToken SYSTEM_TOKEN = makeSystemPassword();
  private static final TCredentials systemCredentials = CredentialHelper.createSquelchError(SYSTEM_PRINCIPAL, SYSTEM_TOKEN, HdfsZooInstance.getInstance()
      .getInstanceID());
  public static byte[] confChecksum = null;
  private static SystemCredentials SYSTEM_CREDS = null;
  private static final String SYSTEM_PRINCIPAL = "!SYSTEM";
  private static final SystemToken SYSTEM_TOKEN = SystemToken.get();
   
  public static AuthenticationToken getSystemToken() {
    return SYSTEM_TOKEN;
  private final TCredentials AS_THRIFT;
  
  private SystemCredentials() {
    super(SYSTEM_PRINCIPAL, SYSTEM_TOKEN);
    AS_THRIFT = toThrift(HdfsZooInstance.getInstance());
   }
   
  public static TCredentials getSystemCredentials() {
  public static SystemCredentials get() {
     SecurityManager sm = System.getSecurityManager();
     if (sm != null) {
       sm.checkPermission(SYSTEM_CREDENTIALS_PERMISSION);
     }
    return systemCredentials;
    if (SYSTEM_CREDS == null) {
      SYSTEM_CREDS = new SystemCredentials();
      
    }
    return SYSTEM_CREDS;
   }
   
  public static String getSystemPrincipal() {
    return SYSTEM_PRINCIPAL;
  public TCredentials getAsThrift() {
    return AS_THRIFT;
   }
   
  private static AuthenticationToken makeSystemPassword() {
    int wireVersion = ServerConstants.WIRE_VERSION;
    byte[] inst = HdfsZooInstance.getInstance().getInstanceID().getBytes(Constants.UTF8);
    try {
      confChecksum = getSystemConfigChecksum();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to compute configuration checksum", e);
    }
  /**
   * An {@link AuthenticationToken} type for Accumulo servers for inter-server communication.
   * 
   * @since 1.6.0
   */
  public static final class SystemToken extends PasswordToken {
     
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(3 * (Integer.SIZE / Byte.SIZE) + inst.length + confChecksum.length);
    DataOutputStream out = new DataOutputStream(bytes);
    try {
      out.write(wireVersion * -1);
      out.write(inst.length);
      out.write(inst);
      out.write(confChecksum.length);
      out.write(confChecksum);
    } catch (IOException e) {
      throw new RuntimeException(e); // this is impossible with
      // ByteArrayOutputStream; crash hard
      // if this happens
    /**
     * A Constructor for {@link Writable}.
     */
    public SystemToken() {}
    
    private SystemToken(byte[] systemPassword) {
      super(systemPassword);
     }
    return new PasswordToken(Base64.encodeBase64(bytes.toByteArray()));
  }
  
  private static byte[] getSystemConfigChecksum() throws NoSuchAlgorithmException {
    if (confChecksum == null) {
      MessageDigest md = MessageDigest.getInstance(Constants.PW_HASH_ALGORITHM);
    
    private static SystemToken get() {
      byte[] confChecksum;
      MessageDigest md;
      try {
        md = MessageDigest.getInstance(Constants.PW_HASH_ALGORITHM);
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("Failed to compute configuration checksum", e);
      }
       
      // seed the config with the version and instance id, so at least
      // it's not empty
      // seed the config with the version and instance id, so at least it's not empty
       md.update(ServerConstants.WIRE_VERSION.toString().getBytes(Constants.UTF8));
       md.update(HdfsZooInstance.getInstance().getInstanceID().getBytes(Constants.UTF8));
       
@@ -103,9 +108,25 @@ public class SecurityConstants {
           md.update(entry.getValue().getBytes(Constants.UTF8));
         }
       }
      
       confChecksum = md.digest();
      
      int wireVersion = ServerConstants.WIRE_VERSION;
      byte[] inst = HdfsZooInstance.getInstance().getInstanceID().getBytes(Constants.UTF8);
      
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(3 * (Integer.SIZE / Byte.SIZE) + inst.length + confChecksum.length);
      DataOutputStream out = new DataOutputStream(bytes);
      try {
        out.write(wireVersion * -1);
        out.write(inst.length);
        out.write(inst);
        out.write(confChecksum.length);
        out.write(confChecksum);
      } catch (IOException e) {
        // this is impossible with ByteArrayOutputStream; crash hard if this happens
        throw new RuntimeException(e);
      }
      return new SystemToken(Base64.encodeBase64(bytes.toByteArray()));
     }
    return confChecksum;
   }
  
 }
diff --git a/server/src/main/java/org/apache/accumulo/server/tabletserver/Tablet.java b/server/src/main/java/org/apache/accumulo/server/tabletserver/Tablet.java
index 1305be6e8..e9b973ae3 100644
-- a/server/src/main/java/org/apache/accumulo/server/tabletserver/Tablet.java
++ b/server/src/main/java/org/apache/accumulo/server/tabletserver/Tablet.java
@@ -106,7 +106,7 @@ import org.apache.accumulo.server.master.tableOps.CompactRange.CompactionIterato
 import org.apache.accumulo.server.problems.ProblemReport;
 import org.apache.accumulo.server.problems.ProblemReports;
 import org.apache.accumulo.server.problems.ProblemType;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.Compactor.CompactionCanceledException;
 import org.apache.accumulo.server.tabletserver.Compactor.CompactionEnv;
 import org.apache.accumulo.server.tabletserver.FileManager.ScanFileManager;
@@ -583,7 +583,7 @@ public class Tablet {
       
       if (filesToDelete.size() > 0) {
         log.debug("Removing scan refs from metadata " + extent + " " + filesToDelete);
        MetadataTableUtil.removeScanFiles(extent, filesToDelete, SecurityConstants.getSystemCredentials(), tabletServer.getLock());
        MetadataTableUtil.removeScanFiles(extent, filesToDelete, SystemCredentials.get().getAsThrift(), tabletServer.getLock());
       }
     }
     
@@ -604,7 +604,7 @@ public class Tablet {
       
       if (filesToDelete.size() > 0) {
         log.debug("Removing scan refs from metadata " + extent + " " + filesToDelete);
        MetadataTableUtil.removeScanFiles(extent, filesToDelete, SecurityConstants.getSystemCredentials(), tabletServer.getLock());
        MetadataTableUtil.removeScanFiles(extent, filesToDelete, SystemCredentials.get().getAsThrift(), tabletServer.getLock());
       }
     }
     
@@ -680,7 +680,7 @@ public class Tablet {
       }
       
       synchronized (bulkFileImportLock) {
        TCredentials auths = SecurityConstants.getSystemCredentials();
        TCredentials auths = SystemCredentials.get().getAsThrift();
         Connector conn;
         try {
           conn = HdfsZooInstance.getInstance().getConnector(auths.getPrincipal(), CredentialHelper.extractToken(auths));
@@ -838,7 +838,7 @@ public class Tablet {
       // very important to write delete entries outside of log lock, because
       // this !METADATA write does not go up... it goes sideways or to itself
       if (absMergeFile != null)
        MetadataTableUtil.addDeleteEntries(extent, Collections.singleton(absMergeFile), SecurityConstants.getSystemCredentials());
        MetadataTableUtil.addDeleteEntries(extent, Collections.singleton(absMergeFile), SystemCredentials.get().getAsThrift());
       
       Set<String> unusedWalLogs = beginClearingUnusedLogs();
       try {
@@ -846,7 +846,7 @@ public class Tablet {
         // need to write to !METADATA before writing to walog, when things are done in the reverse order
         // data could be lost... the minor compaction start even should be written before the following metadata
         // write is made
        TCredentials creds = SecurityConstants.getSystemCredentials();
        TCredentials creds = SystemCredentials.get().getAsThrift();
         
         synchronized (timeLock) {
           if (commitSession.getMaxCommittedTime() > persistedTime)
@@ -1037,7 +1037,7 @@ public class Tablet {
         Set<FileRef> filesInUseByScans = waitForScansToFinish(oldDatafiles, false, 10000);
         if (filesInUseByScans.size() > 0)
           log.debug("Adding scan refs to metadata " + extent + " " + filesInUseByScans);
        MetadataTableUtil.replaceDatafiles(extent, oldDatafiles, filesInUseByScans, newDatafile, compactionId, dfv, SecurityConstants.getSystemCredentials(),
        MetadataTableUtil.replaceDatafiles(extent, oldDatafiles, filesInUseByScans, newDatafile, compactionId, dfv, SystemCredentials.get().getAsThrift(),
             tabletServer.getClientAddressString(), lastLocation, tabletServer.getLock());
         removeFilesAfterScan(filesInUseByScans);
       }
@@ -1131,7 +1131,7 @@ public class Tablet {
       Text rowName = extent.getMetadataEntry();
       
       String tableId = extent.isMeta() ? RootTable.ID : MetadataTable.ID;
      ScannerImpl mdScanner = new ScannerImpl(HdfsZooInstance.getInstance(), SecurityConstants.getSystemCredentials(), tableId, Authorizations.EMPTY);
      ScannerImpl mdScanner = new ScannerImpl(HdfsZooInstance.getInstance(), SystemCredentials.get().getAsThrift(), tableId, Authorizations.EMPTY);
       
       // Commented out because when no data file is present, each tablet will scan through metadata table and return nothing
       // reduced batch size to improve performance
@@ -1161,7 +1161,7 @@ public class Tablet {
     
     if (ke.isMeta()) {
       try {
        logEntries = MetadataTableUtil.getLogEntries(SecurityConstants.getSystemCredentials(), ke);
        logEntries = MetadataTableUtil.getLogEntries(SystemCredentials.get().getAsThrift(), ke);
       } catch (Exception ex) {
         throw new RuntimeException("Unable to read tablet log entries", ex);
       }
@@ -2213,7 +2213,7 @@ public class Tablet {
       }
       
       if (updateMetadata) {
        TCredentials creds = SecurityConstants.getSystemCredentials();
        TCredentials creds = SystemCredentials.get().getAsThrift();
         // if multiple threads were allowed to update this outside of a sync block, then it would be
         // a race condition
         MetadataTableUtil.updateTabletFlushID(extent, tableFlushID, creds, tabletServer.getLock());
@@ -2729,7 +2729,7 @@ public class Tablet {
     }
     
     try {
      Pair<List<LogEntry>,SortedMap<FileRef,DataFileValue>> fileLog = MetadataTableUtil.getFileAndLogEntries(SecurityConstants.getSystemCredentials(), extent);
      Pair<List<LogEntry>,SortedMap<FileRef,DataFileValue>> fileLog = MetadataTableUtil.getFileAndLogEntries(SystemCredentials.get().getAsThrift(), extent);
       
       if (fileLog.getFirst().size() != 0) {
         String msg = "Closed tablet " + extent + " has walog entries in " + MetadataTable.NAME + " " + fileLog.getFirst();
@@ -3516,12 +3516,12 @@ public class Tablet {
       // it is possible that some of the bulk loading flags will be deleted after being read below because the bulk load
       // finishes.... therefore split could propogate load flags for a finished bulk load... there is a special iterator
       // on the !METADATA table to clean up this type of garbage
      Map<FileRef,Long> bulkLoadedFiles = MetadataTableUtil.getBulkFilesLoaded(SecurityConstants.getSystemCredentials(), extent);
      Map<FileRef,Long> bulkLoadedFiles = MetadataTableUtil.getBulkFilesLoaded(SystemCredentials.get().getAsThrift(), extent);
       
      MetadataTableUtil.splitTablet(high, extent.getPrevEndRow(), splitRatio, SecurityConstants.getSystemCredentials(), tabletServer.getLock());
      MetadataTableUtil.addNewTablet(low, lowDirectory, tabletServer.getTabletSession(), lowDatafileSizes, bulkLoadedFiles,
          SecurityConstants.getSystemCredentials(), time, lastFlushID, lastCompactID, tabletServer.getLock());
      MetadataTableUtil.finishSplit(high, highDatafileSizes, highDatafilesToRemove, SecurityConstants.getSystemCredentials(), tabletServer.getLock());
      MetadataTableUtil.splitTablet(high, extent.getPrevEndRow(), splitRatio, SystemCredentials.get().getAsThrift(), tabletServer.getLock());
      MetadataTableUtil.addNewTablet(low, lowDirectory, tabletServer.getTabletSession(), lowDatafileSizes, bulkLoadedFiles, SystemCredentials.get()
          .getAsThrift(), time, lastFlushID, lastCompactID, tabletServer.getLock());
      MetadataTableUtil.finishSplit(high, highDatafileSizes, highDatafilesToRemove, SystemCredentials.get().getAsThrift(), tabletServer.getLock());
       
       log.log(TLevel.TABLET_HIST, extent + " split " + low + " " + high);
       
@@ -3807,7 +3807,7 @@ public class Tablet {
       try {
         // if multiple threads were allowed to update this outside of a sync block, then it would be
         // a race condition
        MetadataTableUtil.updateTabletCompactID(extent, compactionId, SecurityConstants.getSystemCredentials(), tabletServer.getLock());
        MetadataTableUtil.updateTabletCompactID(extent, compactionId, SystemCredentials.get().getAsThrift(), tabletServer.getLock());
       } finally {
         synchronized (this) {
           majorCompactionInProgress = false;
diff --git a/server/src/main/java/org/apache/accumulo/server/tabletserver/TabletServer.java b/server/src/main/java/org/apache/accumulo/server/tabletserver/TabletServer.java
index 9d50f07f7..ceed0ee0e 100644
-- a/server/src/main/java/org/apache/accumulo/server/tabletserver/TabletServer.java
++ b/server/src/main/java/org/apache/accumulo/server/tabletserver/TabletServer.java
@@ -156,8 +156,8 @@ import org.apache.accumulo.server.metrics.AbstractMetricsImpl;
 import org.apache.accumulo.server.problems.ProblemReport;
 import org.apache.accumulo.server.problems.ProblemReports;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityConstants;
 import org.apache.accumulo.server.security.SecurityOperation;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.Compactor.CompactionInfo;
 import org.apache.accumulo.server.tabletserver.Tablet.CommitSession;
 import org.apache.accumulo.server.tabletserver.Tablet.KVEntry;
@@ -228,7 +228,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
   private static long gcTimeIncreasedCount;
   
   private static final long MAX_TIME_TO_WAIT_FOR_SCAN_RESULT_MILLIS = 1000;
  private static final long RECENTLY_SPLIT_MILLIES = 60*1000;
  private static final long RECENTLY_SPLIT_MILLIES = 60 * 1000;
   
   private TabletServerLogger logger;
   
@@ -1749,31 +1749,29 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     
     private ZooCache masterLockCache = new ZooCache();
     
    private void checkPermission(TCredentials credentials, String lock, boolean requiresSystemPermission, final String request) throws ThriftSecurityException {
      if (requiresSystemPermission) {
        boolean fatal = false;
        try {
          log.debug("Got " + request + " message from user: " + credentials.getPrincipal());
          if (!security.canPerformSystemActions(credentials)) {
            log.warn("Got " + request + " message from user: " + credentials.getPrincipal());
            throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
          }
        } catch (ThriftSecurityException e) {
          log.warn("Got " + request + " message from unauthenticatable user: " + e.getUser());
          if (e.getUser().equals(SecurityConstants.SYSTEM_PRINCIPAL)) {
            log.fatal("Got message from a service with a mismatched configuration. Please ensure a compatible configuration.", e);
            fatal = true;
          }
          throw e;
        } finally {
          if (fatal) {
            Halt.halt(1, new Runnable() {
              @Override
              public void run() {
                logGCInfo(getSystemConfiguration());
              }
            });
          }
    private void checkPermission(TCredentials credentials, String lock, final String request) throws ThriftSecurityException {
      boolean fatal = false;
      try {
        log.debug("Got " + request + " message from user: " + credentials.getPrincipal());
        if (!security.canPerformSystemActions(credentials)) {
          log.warn("Got " + request + " message from user: " + credentials.getPrincipal());
          throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
        }
      } catch (ThriftSecurityException e) {
        log.warn("Got " + request + " message from unauthenticatable user: " + e.getUser());
        if (SystemCredentials.get().getAsThrift().getTokenClassName().equals(credentials.getTokenClassName())) {
          log.fatal("Got message from a service with a mismatched configuration. Please ensure a compatible configuration.", e);
          fatal = true;
        }
        throw e;
      } finally {
        if (fatal) {
          Halt.halt(1, new Runnable() {
            @Override
            public void run() {
              logGCInfo(getSystemConfiguration());
            }
          });
         }
       }
       
@@ -1815,7 +1813,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     public void loadTablet(TInfo tinfo, TCredentials credentials, String lock, final TKeyExtent textent) {
       
       try {
        checkPermission(credentials, lock, true, "loadTablet");
        checkPermission(credentials, lock, "loadTablet");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -1891,7 +1889,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public void unloadTablet(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent textent, boolean save) {
       try {
        checkPermission(credentials, lock, true, "unloadTablet");
        checkPermission(credentials, lock, "unloadTablet");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -1905,7 +1903,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public void flush(TInfo tinfo, TCredentials credentials, String lock, String tableId, ByteBuffer startRow, ByteBuffer endRow) {
       try {
        checkPermission(credentials, lock, true, "flush");
        checkPermission(credentials, lock, "flush");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -1942,7 +1940,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public void flushTablet(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent textent) throws TException {
       try {
        checkPermission(credentials, lock, true, "flushTablet");
        checkPermission(credentials, lock, "flushTablet");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -1962,7 +1960,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public void halt(TInfo tinfo, TCredentials credentials, String lock) throws ThriftSecurityException {
       
      checkPermission(credentials, lock, true, "halt");
      checkPermission(credentials, lock, "halt");
       
       Halt.halt(0, new Runnable() {
         @Override
@@ -1996,7 +1994,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public List<ActiveScan> getActiveScans(TInfo tinfo, TCredentials credentials) throws ThriftSecurityException, TException {
       try {
        checkPermission(credentials, null, true, "getScans");
        checkPermission(credentials, null, "getScans");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -2008,7 +2006,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public void chop(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent textent) throws TException {
       try {
        checkPermission(credentials, lock, true, "chop");
        checkPermission(credentials, lock, "chop");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -2025,7 +2023,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public void compact(TInfo tinfo, TCredentials credentials, String lock, String tableId, ByteBuffer startRow, ByteBuffer endRow) throws TException {
       try {
        checkPermission(credentials, lock, true, "compact");
        checkPermission(credentials, lock, "compact");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -2115,7 +2113,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     @Override
     public List<ActiveCompaction> getActiveCompactions(TInfo tinfo, TCredentials credentials) throws ThriftSecurityException, TException {
       try {
        checkPermission(credentials, null, true, "getActiveCompactions");
        checkPermission(credentials, null, "getActiveCompactions");
       } catch (ThriftSecurityException e) {
         log.error(e, e);
         throw new RuntimeException(e);
@@ -2612,7 +2610,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
     entry.server = logs.get(0).getLogger();
     entry.filename = logs.get(0).getFileName();
     entry.logSet = logSet;
    MetadataTableUtil.addLogEntry(SecurityConstants.getSystemCredentials(), entry, getLock());
    MetadataTableUtil.addLogEntry(SystemCredentials.get().getAsThrift(), entry, getLock());
   }
   
   private int startServer(AccumuloConfiguration conf, Property portHint, TProcessor processor, String threadName) throws UnknownHostException {
@@ -2792,7 +2790,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
           while (!serverStopRequested && mm != null && client != null && client.getOutputProtocol() != null
               && client.getOutputProtocol().getTransport() != null && client.getOutputProtocol().getTransport().isOpen()) {
             try {
              mm.send(SecurityConstants.getSystemCredentials(), getClientAddressString(), iface);
              mm.send(SystemCredentials.get().getAsThrift(), getClientAddressString(), iface);
               mm = null;
             } catch (TException ex) {
               log.warn("Error sending message: queuing message again");
@@ -2899,7 +2897,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
         TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN, TabletsSection.TabletColumnFamily.SPLIT_RATIO_COLUMN,
         TabletsSection.TabletColumnFamily.OLD_PREV_ROW_COLUMN, TabletsSection.ServerColumnFamily.TIME_COLUMN});
     
    ScannerImpl scanner = new ScannerImpl(HdfsZooInstance.getInstance(), SecurityConstants.getSystemCredentials(), tableToVerify, Authorizations.EMPTY);
    ScannerImpl scanner = new ScannerImpl(HdfsZooInstance.getInstance(), SystemCredentials.get().getAsThrift(), tableToVerify, Authorizations.EMPTY);
     scanner.setRange(extent.toMetadataRange());
     
     TreeMap<Key,Value> tkv = new TreeMap<Key,Value>();
@@ -2933,7 +2931,7 @@ public class TabletServer extends AbstractMetricsImpl implements org.apache.accu
       
       KeyExtent fke;
       try {
        fke = MetadataTableUtil.fixSplit(metadataEntry, tabletEntries.get(metadataEntry), instance, SecurityConstants.getSystemCredentials(), lock);
        fke = MetadataTableUtil.fixSplit(metadataEntry, tabletEntries.get(metadataEntry), instance, SystemCredentials.get().getAsThrift(), lock);
       } catch (IOException e) {
         log.error("Error fixing split " + metadataEntry);
         throw new AccumuloException(e.toString());
diff --git a/server/src/main/java/org/apache/accumulo/server/util/Admin.java b/server/src/main/java/org/apache/accumulo/server/util/Admin.java
index fca811e18..215b9c79a 100644
-- a/server/src/main/java/org/apache/accumulo/server/util/Admin.java
++ b/server/src/main/java/org/apache/accumulo/server/util/Admin.java
@@ -36,7 +36,7 @@ import org.apache.accumulo.core.security.CredentialHelper;
 import org.apache.accumulo.core.security.thrift.TCredentials;
 import org.apache.accumulo.server.cli.ClientOpts;
 import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.log4j.Logger;
 
@@ -88,8 +88,8 @@ public class Admin {
       String principal;
       AuthenticationToken token;
       if (opts.getToken() == null) {
        principal = SecurityConstants.getSystemPrincipal();
        token = SecurityConstants.getSystemToken();
        principal = SystemCredentials.get().getPrincipal();
        token = SystemCredentials.get().getToken();
       } else {
         principal = opts.principal;
         token = opts.getToken();
diff --git a/server/src/main/java/org/apache/accumulo/server/util/FindOfflineTablets.java b/server/src/main/java/org/apache/accumulo/server/util/FindOfflineTablets.java
index de27112eb..f180ccd3b 100644
-- a/server/src/main/java/org/apache/accumulo/server/util/FindOfflineTablets.java
++ b/server/src/main/java/org/apache/accumulo/server/util/FindOfflineTablets.java
@@ -33,7 +33,7 @@ import org.apache.accumulo.server.master.state.TServerInstance;
 import org.apache.accumulo.server.master.state.TabletLocationState;
 import org.apache.accumulo.server.master.state.TabletState;
 import org.apache.accumulo.server.master.state.tables.TableManager;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.commons.collections.iterators.IteratorChain;
 import org.apache.log4j.Logger;
 
@@ -48,8 +48,8 @@ public class FindOfflineTablets {
     opts.parseArgs(FindOfflineTablets.class.getName(), args);
     final AtomicBoolean scanning = new AtomicBoolean(false);
     Instance instance = opts.getInstance();
    MetaDataTableScanner rootScanner = new MetaDataTableScanner(instance, SecurityConstants.getSystemCredentials(), MetadataSchema.TabletsSection.getRange());
    MetaDataTableScanner metaScanner = new MetaDataTableScanner(instance, SecurityConstants.getSystemCredentials(), MetadataSchema.TabletsSection.getRange());
    MetaDataTableScanner rootScanner = new MetaDataTableScanner(instance, SystemCredentials.get().getAsThrift(), MetadataSchema.TabletsSection.getRange());
    MetaDataTableScanner metaScanner = new MetaDataTableScanner(instance, SystemCredentials.get().getAsThrift(), MetadataSchema.TabletsSection.getRange());
     @SuppressWarnings("unchecked")
     Iterator<TabletLocationState> scanner = new IteratorChain(rootScanner, metaScanner);
     LiveTServerSet tservers = new LiveTServerSet(instance, DefaultConfiguration.getDefaultConfiguration(), new Listener() {
diff --git a/server/src/main/java/org/apache/accumulo/server/util/Initialize.java b/server/src/main/java/org/apache/accumulo/server/util/Initialize.java
index 7d4e6f201..843184d62 100644
-- a/server/src/main/java/org/apache/accumulo/server/util/Initialize.java
++ b/server/src/main/java/org/apache/accumulo/server/util/Initialize.java
@@ -64,7 +64,7 @@ import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.accumulo.server.iterators.MetadataBulkLoadFilter;
 import org.apache.accumulo.server.master.state.tables.TableManager;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.TabletTime;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.hadoop.conf.Configuration;
@@ -439,7 +439,7 @@ public class Initialize {
   }
   
   private static void initSecurity(Opts opts, String iid) throws AccumuloSecurityException, ThriftSecurityException {
    AuditedSecurityOperation.getInstance(iid, true).initializeSecurity(SecurityConstants.getSystemCredentials(), DEFAULT_ROOT_USER, opts.rootpass);
    AuditedSecurityOperation.getInstance(iid, true).initializeSecurity(SystemCredentials.get().getAsThrift(), DEFAULT_ROOT_USER, opts.rootpass);
   }
   
   protected static void initMetadataConfig() throws IOException {
diff --git a/server/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java b/server/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
index 816df8b50..b2cd11492 100644
-- a/server/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
++ b/server/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
@@ -81,7 +81,7 @@ import org.apache.accumulo.server.fs.FileRef;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.zookeeper.ZooLock;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.hadoop.fs.FileStatus;
@@ -490,7 +490,7 @@ public class MetadataTableUtil {
   }
   
   public static void addDeleteEntry(String tableId, String path) throws IOException {
    update(SecurityConstants.getSystemCredentials(), createDeleteMutation(tableId, path), new KeyExtent(new Text(tableId), null, null));
    update(SystemCredentials.get().getAsThrift(), createDeleteMutation(tableId, path), new KeyExtent(new Text(tableId), null, null));
   }
   
   public static Mutation createDeleteMutation(String tableId, String pathToRemove) throws IOException {
@@ -975,7 +975,7 @@ public class MetadataTableUtil {
       } else {
         Mutation m = new Mutation(entry.extent.getMetadataEntry());
         m.putDelete(LogColumnFamily.NAME, new Text(entry.server + "/" + entry.filename));
        update(SecurityConstants.getSystemCredentials(), zooLock, m, entry.extent);
        update(SystemCredentials.get().getAsThrift(), zooLock, m, entry.extent);
       }
     }
   }
@@ -1126,7 +1126,7 @@ public class MetadataTableUtil {
   
   public static void cloneTable(Instance instance, String srcTableId, String tableId) throws Exception {
     
    Connector conn = instance.getConnector(SecurityConstants.SYSTEM_PRINCIPAL, SecurityConstants.getSystemToken());
    Connector conn = instance.getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get().getToken());
     BatchWriter bw = conn.createBatchWriter(MetadataTable.NAME, new BatchWriterConfig());
     
     while (true) {
@@ -1151,7 +1151,7 @@ public class MetadataTableUtil {
         bw.flush();
         
         // delete what we have cloned and try again
        deleteTable(tableId, false, SecurityConstants.getSystemCredentials(), null);
        deleteTable(tableId, false, SystemCredentials.get().getAsThrift(), null);
         
         log.debug("Tablets merged in table " + srcTableId + " while attempting to clone, trying again");
         
@@ -1181,7 +1181,7 @@ public class MetadataTableUtil {
   public static void chopped(KeyExtent extent, ZooLock zooLock) {
     Mutation m = new Mutation(extent.getMetadataEntry());
     ChoppedColumnFamily.CHOPPED_COLUMN.put(m, new Value("chopped".getBytes()));
    update(SecurityConstants.getSystemCredentials(), zooLock, m, extent);
    update(SystemCredentials.get().getAsThrift(), zooLock, m, extent);
   }
   
   public static void removeBulkLoadEntries(Connector conn, String tableId, long tid) throws Exception {
@@ -1242,7 +1242,7 @@ public class MetadataTableUtil {
     
     // new KeyExtent is only added to force update to write to the metadata table, not the root table
     // because bulk loads aren't supported to the metadata table
    update(SecurityConstants.getSystemCredentials(), m, new KeyExtent(new Text("anythingNotMetadata"), null, null));
    update(SystemCredentials.get().getAsThrift(), m, new KeyExtent(new Text("anythingNotMetadata"), null, null));
   }
   
   public static void removeBulkLoadInProgressFlag(String path) {
@@ -1252,7 +1252,7 @@ public class MetadataTableUtil {
     
     // new KeyExtent is only added to force update to write to the metadata table, not the root table
     // because bulk loads aren't supported to the metadata table
    update(SecurityConstants.getSystemCredentials(), m, new KeyExtent(new Text("anythingNotMetadata"), null, null));
    update(SystemCredentials.get().getAsThrift(), m, new KeyExtent(new Text("anythingNotMetadata"), null, null));
   }
   
   public static void moveMetaDeleteMarkers(Instance instance, TCredentials creds) {
diff --git a/server/src/test/java/org/apache/accumulo/server/security/SystemCredentialsTest.java b/server/src/test/java/org/apache/accumulo/server/security/SystemCredentialsTest.java
new file mode 100644
index 000000000..f422ecba9
-- /dev/null
++ b/server/src/test/java/org/apache/accumulo/server/security/SystemCredentialsTest.java
@@ -0,0 +1,67 @@
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
package org.apache.accumulo.server.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ConnectorImpl;
import org.apache.accumulo.core.security.Credentials;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.server.security.SystemCredentials.SystemToken;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class SystemCredentialsTest {
  
  @BeforeClass
  public static void setUp() throws IOException {
    File testInstanceId = new File(new File(new File(new File("target"), "instanceTest"), "instance_id"), UUID.fromString(
        "00000000-0000-0000-0000-000000000000").toString());
    if (!testInstanceId.exists()) {
      testInstanceId.getParentFile().mkdirs();
      testInstanceId.createNewFile();
    }
  }
  
  /**
   * This is a test to ensure the string literal in {@link ConnectorImpl#ConnectorImpl(Instance, TCredentials)} is kept up-to-date if we move the
   * {@link SystemToken}<br/>
   * This check will not be needed after ACCUMULO-1578
   */
  @Test
  public void testSystemToken() {
    assertEquals("org.apache.accumulo.server.security.SystemCredentials$SystemToken", SystemToken.class.getName());
    assertEquals(SystemCredentials.get().getToken().getClass(), SystemToken.class);
    assertEquals(SystemCredentials.get().getAsThrift().getTokenClassName(), SystemToken.class.getName());
  }
  
  @Test
  public void testSystemCredentials() {
    Credentials a = SystemCredentials.get();
    Credentials b = SystemCredentials.get();
    assertTrue(a == b);
  }
}
diff --git a/server/src/test/resources/accumulo-site.xml b/server/src/test/resources/accumulo-site.xml
new file mode 100644
index 000000000..2aa9fff5e
-- /dev/null
++ b/server/src/test/resources/accumulo-site.xml
@@ -0,0 +1,32 @@
<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<configuration>

  <property>
    <name>instance.dfs.dir</name>
    <value>${project.build.directory}/instanceTest</value>
  </property>

  <property>
    <name>instance.secret</name>
    <value>TEST_SYSTEM_SECRET</value>
  </property>

</configuration>
diff --git a/test/src/main/java/org/apache/accumulo/test/GetMasterStats.java b/test/src/main/java/org/apache/accumulo/test/GetMasterStats.java
index 65cf80c1e..caef67036 100644
-- a/test/src/main/java/org/apache/accumulo/test/GetMasterStats.java
++ b/test/src/main/java/org/apache/accumulo/test/GetMasterStats.java
@@ -19,7 +19,6 @@ package org.apache.accumulo.test;
 import java.io.IOException;
 import java.util.Map.Entry;
 
import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.accumulo.core.client.impl.MasterClient;
 import org.apache.accumulo.core.master.MasterNotRunningException;
 import org.apache.accumulo.core.master.thrift.MasterClientService;
@@ -29,7 +28,8 @@ import org.apache.accumulo.core.master.thrift.TableInfo;
 import org.apache.accumulo.core.master.thrift.TabletServerStatus;
 import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.monitor.Monitor;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.thrift.transport.TTransportException;
 
 public class GetMasterStats {
@@ -44,7 +44,7 @@ public class GetMasterStats {
     MasterMonitorInfo stats = null;
     try {
       client = MasterClient.getConnectionWithRetry(HdfsZooInstance.getInstance());
      stats = client.getMasterStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
      stats = client.getMasterStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
     } finally {
       if (client != null)
         MasterClient.close(client);
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousStatsCollector.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousStatsCollector.java
index ea677da62..8345ac440 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousStatsCollector.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousStatsCollector.java
@@ -45,7 +45,7 @@ import org.apache.accumulo.server.cli.ClientOnRequiredTable;
 import org.apache.accumulo.server.fs.VolumeManager;
 import org.apache.accumulo.server.fs.VolumeManagerImpl;
 import org.apache.accumulo.server.monitor.Monitor;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.trace.instrument.Tracer;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.ContentSummary;
@@ -134,7 +134,7 @@ public class ContinuousStatsCollector {
       MasterClientService.Iface client = null;
       try {
         client = MasterClient.getConnectionWithRetry(opts.getInstance());
        MasterMonitorInfo stats = client.getMasterStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
        MasterMonitorInfo stats = client.getMasterStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
         
         TableInfo all = new TableInfo();
         Map<String,TableInfo> tableSummaries = new HashMap<String,TableInfo>();
@@ -177,8 +177,7 @@ public class ContinuousStatsCollector {
     
   }
   
  static class Opts extends ClientOnRequiredTable {
  }
  static class Opts extends ClientOnRequiredTable {}
   
   public static void main(String[] args) {
     Opts opts = new Opts();
diff --git a/test/src/main/java/org/apache/accumulo/test/functional/SplitRecoveryTest.java b/test/src/main/java/org/apache/accumulo/test/functional/SplitRecoveryTest.java
index 8cb79c318..802d94288 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/SplitRecoveryTest.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/SplitRecoveryTest.java
@@ -52,7 +52,7 @@ import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.fs.FileRef;
 import org.apache.accumulo.server.master.state.Assignment;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tabletserver.TabletServer;
 import org.apache.accumulo.server.tabletserver.TabletTime;
 import org.apache.accumulo.server.util.FileUtil;
@@ -140,7 +140,7 @@ public class SplitRecoveryTest extends FunctionalTest {
       KeyExtent extent = extents[i];
       
       String tdir = ServerConstants.getTablesDirs()[0] + "/" + extent.getTableId().toString() + "/dir_" + i;
      MetadataTableUtil.addTablet(extent, tdir, SecurityConstants.getSystemCredentials(), TabletTime.LOGICAL_TIME_ID, zl);
      MetadataTableUtil.addTablet(extent, tdir, SystemCredentials.get().getAsThrift(), TabletTime.LOGICAL_TIME_ID, zl);
       SortedMap<FileRef,DataFileValue> mapFiles = new TreeMap<FileRef,DataFileValue>();
       mapFiles.put(new FileRef(tdir + "/" + RFile.EXTENSION + "_000_000"), new DataFileValue(1000017 + i, 10000 + i));
       
@@ -149,7 +149,7 @@ public class SplitRecoveryTest extends FunctionalTest {
       }
       int tid = 0;
       TransactionWatcher.ZooArbitrator.start(Constants.BULK_ARBITRATOR_TYPE, tid);
      MetadataTableUtil.updateTabletDataFile(tid, extent, mapFiles, "L0", SecurityConstants.getSystemCredentials(), zl);
      MetadataTableUtil.updateTabletDataFile(tid, extent, mapFiles, "L0", SystemCredentials.get().getAsThrift(), zl);
     }
     
     KeyExtent extent = extents[extentToSplit];
@@ -170,21 +170,21 @@ public class SplitRecoveryTest extends FunctionalTest {
     MetadataTableUtil.splitDatafiles(extent.getTableId(), midRow, splitRatio, new HashMap<FileRef,FileUtil.FileInfo>(), mapFiles, lowDatafileSizes,
         highDatafileSizes, highDatafilesToRemove);
     
    MetadataTableUtil.splitTablet(high, extent.getPrevEndRow(), splitRatio, SecurityConstants.getSystemCredentials(), zl);
    MetadataTableUtil.splitTablet(high, extent.getPrevEndRow(), splitRatio, SystemCredentials.get().getAsThrift(), zl);
     TServerInstance instance = new TServerInstance(location, zl.getSessionId());
    Writer writer = new Writer(HdfsZooInstance.getInstance(), SecurityConstants.getSystemCredentials(), MetadataTable.ID);
    Writer writer = new Writer(HdfsZooInstance.getInstance(), SystemCredentials.get().getAsThrift(), MetadataTable.ID);
     Assignment assignment = new Assignment(high, instance);
     Mutation m = new Mutation(assignment.tablet.getMetadataEntry());
     m.put(TabletsSection.FutureLocationColumnFamily.NAME, assignment.server.asColumnQualifier(), assignment.server.asMutationValue());
     writer.update(m);
     
     if (steps >= 1) {
      Map<FileRef,Long> bulkFiles = MetadataTableUtil.getBulkFilesLoaded(SecurityConstants.getSystemCredentials(), extent);
      MetadataTableUtil.addNewTablet(low, "/lowDir", instance, lowDatafileSizes, bulkFiles, SecurityConstants.getSystemCredentials(),
          TabletTime.LOGICAL_TIME_ID + "0", -1l, -1l, zl);
      Map<FileRef,Long> bulkFiles = MetadataTableUtil.getBulkFilesLoaded(SystemCredentials.get().getAsThrift(), extent);
      MetadataTableUtil.addNewTablet(low, "/lowDir", instance, lowDatafileSizes, bulkFiles, SystemCredentials.get().getAsThrift(), TabletTime.LOGICAL_TIME_ID
          + "0", -1l, -1l, zl);
     }
     if (steps >= 2)
      MetadataTableUtil.finishSplit(high, highDatafileSizes, highDatafilesToRemove, SecurityConstants.getSystemCredentials(), zl);
      MetadataTableUtil.finishSplit(high, highDatafileSizes, highDatafilesToRemove, SystemCredentials.get().getAsThrift(), zl);
     
     TabletServer.verifyTabletInformation(high, instance, null, "127.0.0.1:0", zl);
     
@@ -192,8 +192,8 @@ public class SplitRecoveryTest extends FunctionalTest {
       ensureTabletHasNoUnexpectedMetadataEntries(low, lowDatafileSizes);
       ensureTabletHasNoUnexpectedMetadataEntries(high, highDatafileSizes);
       
      Map<FileRef,Long> lowBulkFiles = MetadataTableUtil.getBulkFilesLoaded(SecurityConstants.getSystemCredentials(), low);
      Map<FileRef,Long> highBulkFiles = MetadataTableUtil.getBulkFilesLoaded(SecurityConstants.getSystemCredentials(), high);
      Map<FileRef,Long> lowBulkFiles = MetadataTableUtil.getBulkFilesLoaded(SystemCredentials.get().getAsThrift(), low);
      Map<FileRef,Long> highBulkFiles = MetadataTableUtil.getBulkFilesLoaded(SystemCredentials.get().getAsThrift(), high);
       
       if (!lowBulkFiles.equals(highBulkFiles)) {
         throw new Exception(" " + lowBulkFiles + " != " + highBulkFiles + " " + low + " " + high);
@@ -208,7 +208,7 @@ public class SplitRecoveryTest extends FunctionalTest {
   }
   
   private void ensureTabletHasNoUnexpectedMetadataEntries(KeyExtent extent, SortedMap<FileRef,DataFileValue> expectedMapFiles) throws Exception {
    Scanner scanner = new ScannerImpl(HdfsZooInstance.getInstance(), SecurityConstants.getSystemCredentials(), MetadataTable.ID, Authorizations.EMPTY);
    Scanner scanner = new ScannerImpl(HdfsZooInstance.getInstance(), SystemCredentials.get().getAsThrift(), MetadataTable.ID, Authorizations.EMPTY);
     scanner.setRange(extent.toMetadataRange());
     
     HashSet<ColumnFQ> expectedColumns = new HashSet<ColumnFQ>();
@@ -247,7 +247,7 @@ public class SplitRecoveryTest extends FunctionalTest {
       throw new Exception("Not all expected columns seen " + extent + " " + expectedColumns);
     }
     
    SortedMap<FileRef,DataFileValue> fixedMapFiles = MetadataTableUtil.getDataFileSizes(extent, SecurityConstants.getSystemCredentials());
    SortedMap<FileRef,DataFileValue> fixedMapFiles = MetadataTableUtil.getDataFileSizes(extent, SystemCredentials.get().getAsThrift());
     verifySame(expectedMapFiles, fixedMapFiles);
   }
   
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java b/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
index 5602f144e..3545170e2 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/metadata/MetadataBatchScanTest.java
@@ -42,7 +42,7 @@ import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.AddressUtil;
 import org.apache.accumulo.core.util.Stat;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.hadoop.io.Text;
 
 /**
@@ -56,8 +56,8 @@ public class MetadataBatchScanTest {
   
   public static void main(String[] args) throws Exception {
     
    final Connector connector = new ZooKeeperInstance("acu14", "localhost")
        .getConnector(SecurityConstants.SYSTEM_PRINCIPAL, SecurityConstants.getSystemToken());
    final Connector connector = new ZooKeeperInstance("acu14", "localhost").getConnector(SystemCredentials.get().getPrincipal(), SystemCredentials.get()
        .getToken());
     
     TreeSet<Long> splits = new TreeSet<Long>();
     Random r = new Random(42);
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
index d4b1c8e2e..41a4d541e 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
@@ -62,7 +62,7 @@ import org.apache.accumulo.server.master.state.MetaDataStateStore;
 import org.apache.accumulo.server.master.state.MetaDataTableScanner;
 import org.apache.accumulo.server.master.state.TServerInstance;
 import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.util.TServerUtils;
 import org.apache.accumulo.server.zookeeper.TransactionWatcher;
 import org.apache.accumulo.trace.thrift.TInfo;
@@ -230,7 +230,7 @@ public class NullTserver {
     
     // read the locations for the table
     Range tableRange = new KeyExtent(new Text(tableId), null, null).toMetadataRange();
    MetaDataTableScanner s = new MetaDataTableScanner(zki, SecurityConstants.getSystemCredentials(), tableRange);
    MetaDataTableScanner s = new MetaDataTableScanner(zki, SystemCredentials.get().getAsThrift(), tableRange);
     long randomSessionID = opts.port;
     TServerInstance instance = new TServerInstance(addr, randomSessionID);
     List<Assignment> assignments = new ArrayList<Assignment>();
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/Shutdown.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/Shutdown.java
index b28375249..aa4c619e9 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/Shutdown.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/Shutdown.java
@@ -24,7 +24,7 @@ import org.apache.accumulo.core.master.thrift.MasterGoalState;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.master.state.SetGoalState;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.test.randomwalk.State;
 import org.apache.accumulo.test.randomwalk.Test;
 import org.apache.accumulo.trace.instrument.Tracer;
@@ -32,25 +32,25 @@ import org.apache.accumulo.trace.instrument.Tracer;
 public class Shutdown extends Test {
   
   @Override
  public void visit(State state, Properties props) throws Exception  {
  public void visit(State state, Properties props) throws Exception {
     log.debug("shutting down");
    SetGoalState.main(new String[]{MasterGoalState.CLEAN_STOP.name()});
    SetGoalState.main(new String[] {MasterGoalState.CLEAN_STOP.name()});
     
     while (!state.getConnector().instanceOperations().getTabletServers().isEmpty()) {
       UtilWaitThread.sleep(1000);
     }
     
     while (true) {
        try {
          Client client = MasterClient.getConnection(HdfsZooInstance.getInstance());
          client.getMasterStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
        } catch (Exception e) {
          // assume this is due to server shutdown
          break;
        }
        UtilWaitThread.sleep(1000);
      try {
        Client client = MasterClient.getConnection(HdfsZooInstance.getInstance());
        client.getMasterStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
      } catch (Exception e) {
        // assume this is due to server shutdown
        break;
      }
      UtilWaitThread.sleep(1000);
     }

    
     log.debug("tablet servers stopped");
   }
   
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/StartAll.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/StartAll.java
index 8b99a5580..45844b095 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/StartAll.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/concurrent/StartAll.java
@@ -25,7 +25,7 @@ import org.apache.accumulo.core.master.thrift.MasterMonitorInfo;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.master.state.SetGoalState;
import org.apache.accumulo.server.security.SecurityConstants;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.test.randomwalk.State;
 import org.apache.accumulo.test.randomwalk.Test;
 import org.apache.accumulo.trace.instrument.Tracer;
@@ -35,13 +35,13 @@ public class StartAll extends Test {
   @Override
   public void visit(State state, Properties props) throws Exception {
     log.info("Starting all servers");
    SetGoalState.main(new String[]{MasterGoalState.NORMAL.name()});
    Process exec = Runtime.getRuntime().exec(new String[]{System.getenv().get("ACCUMULO_HOME") + "/bin/start-all.sh"});
    SetGoalState.main(new String[] {MasterGoalState.NORMAL.name()});
    Process exec = Runtime.getRuntime().exec(new String[] {System.getenv().get("ACCUMULO_HOME") + "/bin/start-all.sh"});
     exec.waitFor();
     while (true) {
       try {
         Client client = MasterClient.getConnection(HdfsZooInstance.getInstance());
        MasterMonitorInfo masterStats = client.getMasterStats(Tracer.traceInfo(), SecurityConstants.getSystemCredentials());
        MasterMonitorInfo masterStats = client.getMasterStats(Tracer.traceInfo(), SystemCredentials.get().getAsThrift());
         if (!masterStats.tServerInfo.isEmpty())
           break;
       } catch (Exception ex) {
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/security/WalkingSecurity.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/security/WalkingSecurity.java
index bd97dd42e..9cff8f7aa 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/security/WalkingSecurity.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/security/WalkingSecurity.java
@@ -69,7 +69,6 @@ public class WalkingSecurity extends SecurityOperation implements Authorizor, Au
     super(author, authent, pm, instanceId);
   }
   
  @SuppressWarnings("deprecation")
   public WalkingSecurity(State state2) {
     super(state2.getInstance().getInstanceID());
     this.state = state2;
@@ -401,7 +400,7 @@ public class WalkingSecurity extends SecurityOperation implements Authorizor, Au
   public boolean validTokenClass(String tokenClass) {
     return tokenClass.equals(PasswordToken.class.getCanonicalName());
   }

  
   public static void clearInstance() {
     instance = null;
   }
- 
2.19.1.windows.1

