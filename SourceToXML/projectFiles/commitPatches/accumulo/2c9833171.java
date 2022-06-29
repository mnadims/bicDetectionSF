From 2c983317179634d6ddc10726defff303be4ae708 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Fri, 13 Feb 2015 12:48:16 -0500
Subject: [PATCH] ACCUMULO-3513 Add delegation token support for kerberos
 configurations

Generate secret keys internally to Accumulo, distribute them among
the nodes via ZK, and use the secret keys to create expiring passwords
that users can request and servers can validate. Allows for seamless
integration with existing token support in MapReduce for HDFS and YARN
access.
--
 .../org/apache/accumulo/core/Constants.java   |    5 +
 .../apache/accumulo/core/cli/ClientOpts.java  |    6 +
 .../cli/MapReduceClientOnDefaultTable.java    |   12 +-
 .../cli/MapReduceClientOnRequiredTable.java   |   17 +-
 .../core/cli/MapReduceClientOpts.java         |   50 +
 .../client/admin/DelegationTokenConfig.java   |   84 ++
 .../core/client/admin/SecurityOperations.java |    8 +
 .../core/client/impl/ClientContext.java       |   19 +-
 .../impl/DelegationTokenConfigSerializer.java |   54 +
 .../client/impl/SecurityOperationsImpl.java   |   35 +
 .../core/client/impl/ThriftTransportKey.java  |    4 +-
 .../client/mapred/AbstractInputFormat.java    |   51 +-
 .../client/mapred/AccumuloOutputFormat.java   |    4 +-
 .../client/mapreduce/AbstractInputFormat.java |   53 +-
 .../mapreduce/AccumuloOutputFormat.java       |    4 +-
 .../mapreduce/impl/DelegationTokenStub.java   |   80 ++
 .../mapreduce/lib/impl/ConfiguratorBase.java  |   81 +-
 .../mapreduce/lib/impl/InputConfigurator.java |   68 +
 .../client/mock/MockSecurityOperations.java   |    7 +
 .../security/tokens/DelegationToken.java      |  163 +++
 .../apache/accumulo/core/conf/Property.java   |    4 +
 .../master/thrift/MasterClientService.java    | 1183 +++++++++++++++++
 .../rpc/SaslClientDigestCallbackHandler.java  |  114 ++
 .../core/rpc/SaslConnectionParams.java        |  148 ++-
 .../core/rpc/SaslDigestCallbackHandler.java   |   77 ++
 .../apache/accumulo/core/rpc/ThriftUtil.java  |   12 +-
 .../AuthenticationTokenIdentifier.java        |  210 +++
 .../core/security/SystemPermission.java       |    3 +-
 .../security/thrift/TAuthenticationKey.java   |  705 ++++++++++
 .../TAuthenticationTokenIdentifier.java       |  796 +++++++++++
 .../security/thrift/TDelegationToken.java     |  520 ++++++++
 .../thrift/TDelegationTokenConfig.java        |  399 ++++++
 .../thrift/TDelegationTokenOptions.java       |  399 ++++++
 .../accumulo/core/util/ThriftMessageUtil.java |  109 ++
 core/src/main/thrift/master.thrift            |    3 +
 core/src/main/thrift/security.thrift          |   23 +
 .../admin/DelegationTokenConfigTest.java      |   63 +
 .../DelegationTokenConfigSerializerTest.java  |   40 +
 .../client/impl/ThriftTransportKeyTest.java   |   97 +-
 .../security/tokens/DelegationTokenTest.java  |   72 +
 .../SaslClientDigestCallbackHandlerTest.java  |   33 +
 .../core/rpc/SaslConnectionParamsTest.java    |  139 +-
 .../AuthenticationTokenIdentifierTest.java    |  111 ++
 .../core/util/ThriftMessageUtilTest.java      |   83 ++
 docs/src/main/asciidoc/chapters/kerberos.txt  |  110 ++
 .../accumulo/fate/zookeeper/IZooReader.java   |    4 +
 .../accumulo/fate/zookeeper/ZooReader.java    |   28 +
 .../accumulo/fate/zookeeper/ZooUtil.java      |   18 +
 .../java/org/apache/accumulo/proxy/Proxy.java |   14 +-
 .../server/AccumuloServerContext.java         |   56 +-
 .../master/state/MetaDataStateStore.java      |    1 -
 .../rpc/SaslServerConnectionParams.java       |   69 +
 .../rpc/SaslServerDigestCallbackHandler.java  |  113 ++
 ...TCredentialsUpdatingInvocationHandler.java |   18 +-
 .../accumulo/server/rpc/TServerUtils.java     |   34 +-
 .../server/rpc/UGIAssumingProcessor.java      |   55 +-
 .../security/AuditedSecurityOperation.java    |   14 +
 .../server/security/SecurityOperation.java    |    4 +
 .../server/security/SystemCredentials.java    |    4 +-
 .../delegation/AuthenticationKey.java         |  150 +++
 .../AuthenticationTokenKeyManager.java        |  169 +++
 .../AuthenticationTokenSecretManager.java     |  269 ++++
 .../ZooAuthenticationKeyDistributor.java      |  187 +++
 .../ZooAuthenticationKeyWatcher.java          |  206 +++
 .../handler/KerberosAuthenticator.java        |    3 +-
 .../server/AccumuloServerContextTest.java     |   25 +-
 .../rpc/SaslDigestCallbackHandlerTest.java    |  137 ++
 .../rpc/SaslServerConnectionParamsTest.java   |  101 ++
 .../delegation/AuthenticationKeyTest.java     |   95 ++
 .../AuthenticationTokenKeyManagerTest.java    |  196 +++
 .../AuthenticationTokenSecretManagerTest.java |  393 ++++++
 .../ZooAuthenticationKeyDistributorTest.java  |  270 ++++
 .../ZooAuthenticationKeyWatcherTest.java      |  323 +++++
 .../accumulo/gc/SimpleGarbageCollector.java   |    2 +-
 .../gc/GarbageCollectWriteAheadLogsTest.java  |    7 +
 .../gc/SimpleGarbageCollectorTest.java        |    7 +
 .../CloseWriteAheadLogReferencesTest.java     |    7 +
 .../org/apache/accumulo/master/Master.java    |   57 +-
 .../master/MasterClientServiceHandler.java    |   30 +
 .../apache/accumulo/tserver/TabletServer.java |   26 +
 .../continuous/ContinuousBatchWalker.java     |   10 +-
 .../test/continuous/ContinuousIngest.java     |   82 +-
 .../test/continuous/ContinuousMoru.java       |   14 +-
 .../test/continuous/ContinuousOpts.java       |   80 ++
 .../test/continuous/ContinuousQuery.java      |   12 +-
 .../test/continuous/ContinuousScanner.java    |    8 +-
 .../test/continuous/ContinuousWalk.java       |    8 +-
 .../accumulo/harness/MiniClusterHarness.java  |    7 +-
 .../apache/accumulo/test/ShellServerIT.java   |    2 +-
 .../accumulo/test/functional/KerberosIT.java  |  250 +++-
 90 files changed, 9587 insertions(+), 236 deletions(-)
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/admin/DelegationTokenConfig.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializer.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/DelegationTokenStub.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/security/tokens/DelegationToken.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandler.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/rpc/SaslDigestCallbackHandler.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifier.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationKey.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationTokenIdentifier.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationToken.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenConfig.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenOptions.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/util/ThriftMessageUtil.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/client/admin/DelegationTokenConfigTest.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializerTest.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/client/security/tokens/DelegationTokenTest.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandlerTest.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifierTest.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/util/ThriftMessageUtilTest.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerConnectionParams.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerDigestCallbackHandler.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationKey.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManager.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManager.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributor.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcher.java
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/rpc/SaslDigestCallbackHandlerTest.java
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/rpc/SaslServerConnectionParamsTest.java
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationKeyTest.java
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManagerTest.java
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManagerTest.java
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributorTest.java
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcherTest.java
 create mode 100644 test/src/main/java/org/apache/accumulo/test/continuous/ContinuousOpts.java

diff --git a/core/src/main/java/org/apache/accumulo/core/Constants.java b/core/src/main/java/org/apache/accumulo/core/Constants.java
index 0229d4e7f..94ada7a3d 100644
-- a/core/src/main/java/org/apache/accumulo/core/Constants.java
++ b/core/src/main/java/org/apache/accumulo/core/Constants.java
@@ -81,6 +81,11 @@ public class Constants {
   public static final String ZHDFS_RESERVATIONS = "/hdfs_reservations";
   public static final String ZRECOVERY = "/recovery";
 
  /**
   * Base znode for storing secret keys that back delegation tokens
   */
  public static final String ZDELEGATION_TOKEN_KEYS = "/delegation_token_keys";

   /**
    * Initial tablet directory name for the default tablet in all tables
    */
diff --git a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
index 216f32d36..a7d98b38e 100644
-- a/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
++ b/core/src/main/java/org/apache/accumulo/core/cli/ClientOpts.java
@@ -241,6 +241,12 @@ public class ClientOpts extends Help {
         throw new AccumuloSecurityException("No principal or authentication token was provided", SecurityErrorCode.BAD_CREDENTIALS);
       }
 
      // In MapReduce, if we create a DelegationToken, the principal is updated from the KerberosToken
      // used to obtain the DelegationToken.
      if (null != principal) {
        return principal;
      }

       // Try to extract the principal automatically from Kerberos
       if (token instanceof KerberosToken) {
         principal = ((KerberosToken) token).getPrincipal();
diff --git a/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnDefaultTable.java b/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnDefaultTable.java
index 0cf081f2d..d39554cc2 100644
-- a/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnDefaultTable.java
++ b/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnDefaultTable.java
@@ -19,6 +19,7 @@ package org.apache.accumulo.core.cli;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.hadoop.mapreduce.Job;
 
 import com.beust.jcommander.Parameter;
@@ -38,12 +39,15 @@ public class MapReduceClientOnDefaultTable extends MapReduceClientOpts {
   @Override
   public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
     super.setAccumuloConfigs(job);
    AccumuloInputFormat.setConnectorInfo(job, getPrincipal(), getToken());
    AccumuloInputFormat.setInputTableName(job, getTableName());
    final String tableName = getTableName();
    final String principal = getPrincipal();
    final AuthenticationToken token = getToken();
    AccumuloInputFormat.setConnectorInfo(job, principal, token);
    AccumuloInputFormat.setInputTableName(job, tableName);
     AccumuloInputFormat.setScanAuthorizations(job, auths);
    AccumuloOutputFormat.setConnectorInfo(job, getPrincipal(), getToken());
    AccumuloOutputFormat.setConnectorInfo(job, principal, token);
     AccumuloOutputFormat.setCreateTables(job, true);
    AccumuloOutputFormat.setDefaultTableName(job, getTableName());
    AccumuloOutputFormat.setDefaultTableName(job, tableName);
   }
 
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnRequiredTable.java b/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnRequiredTable.java
index 7719e9278..caef02d4b 100644
-- a/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnRequiredTable.java
++ b/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOnRequiredTable.java
@@ -19,11 +19,13 @@ package org.apache.accumulo.core.cli;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.hadoop.mapreduce.Job;
 
 import com.beust.jcommander.Parameter;
 
 public class MapReduceClientOnRequiredTable extends MapReduceClientOpts {

   @Parameter(names = {"-t", "--table"}, required = true, description = "table to use")
   private String tableName;
 
@@ -34,17 +36,20 @@ public class MapReduceClientOnRequiredTable extends MapReduceClientOpts {
   public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
     super.setAccumuloConfigs(job);
 
    final String principal = getPrincipal(), tableName = getTableName();

     if (tokenFile.isEmpty()) {
      AccumuloInputFormat.setConnectorInfo(job, getPrincipal(), getToken());
      AccumuloOutputFormat.setConnectorInfo(job, getPrincipal(), getToken());
      AuthenticationToken token = getToken();
      AccumuloInputFormat.setConnectorInfo(job, principal, token);
      AccumuloOutputFormat.setConnectorInfo(job, principal, token);
     } else {
      AccumuloInputFormat.setConnectorInfo(job, getPrincipal(), tokenFile);
      AccumuloOutputFormat.setConnectorInfo(job, getPrincipal(), tokenFile);
      AccumuloInputFormat.setConnectorInfo(job, principal, tokenFile);
      AccumuloOutputFormat.setConnectorInfo(job, principal, tokenFile);
     }
    AccumuloInputFormat.setInputTableName(job, getTableName());
    AccumuloInputFormat.setInputTableName(job, tableName);
     AccumuloInputFormat.setScanAuthorizations(job, auths);
     AccumuloOutputFormat.setCreateTables(job, true);
    AccumuloOutputFormat.setDefaultTableName(job, getTableName());
    AccumuloOutputFormat.setDefaultTableName(job, tableName);
   }
 
   public String getTableName() {
diff --git a/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOpts.java b/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOpts.java
index 4b3b7edf8..2a5408bb7 100644
-- a/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOpts.java
++ b/core/src/main/java/org/apache/accumulo/core/cli/MapReduceClientOpts.java
@@ -17,16 +17,66 @@
 package org.apache.accumulo.core.cli;
 
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.security.SystemPermission;
 import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 /**
  * Adds some MR awareness to the ClientOpts
  */
 public class MapReduceClientOpts extends ClientOpts {
  private static final Logger log = LoggerFactory.getLogger(MapReduceClientOpts.class);

   public void setAccumuloConfigs(Job job) throws AccumuloSecurityException {
     AccumuloInputFormat.setZooKeeperInstance(job, this.getClientConfiguration());
     AccumuloOutputFormat.setZooKeeperInstance(job, this.getClientConfiguration());
   }

  @Override
  public AuthenticationToken getToken() {
    AuthenticationToken authToken = super.getToken();
    // For MapReduce, Kerberos credentials don't make it to the Mappers and Reducers,
    // so we need to request a delegation token and use that instead.
    if (authToken instanceof KerberosToken) {
      log.info("Received KerberosToken, fetching DelegationToken for MapReduce");
      final KerberosToken krbToken = (KerberosToken) authToken;

      try {
        UserGroupInformation user = UserGroupInformation.getCurrentUser();
        if (!user.hasKerberosCredentials()) {
          throw new IllegalStateException("Expected current user to have Kerberos credentials");
        }

        String newPrincipal = user.getUserName();
        log.info("Obtaining delegation token for {}", newPrincipal);

        setPrincipal(newPrincipal);
        Connector conn = getInstance().getConnector(newPrincipal, krbToken);

        // Do the explicit check to see if the user has the permission to get a delegation token
        if (!conn.securityOperations().hasSystemPermission(conn.whoami(), SystemPermission.OBTAIN_DELEGATION_TOKEN)) {
          log.error("{} doesn't have the {} SystemPermission neccesary to obtain a delegation token. MapReduce tasks cannot automatically use the client's"
              + " credentials on remote servers. Delegation tokens provide a means to run MapReduce without distributing the user's credentials.",
              user.getUserName(), SystemPermission.OBTAIN_DELEGATION_TOKEN.name());
          throw new IllegalStateException(conn.whoami() + " does not have permission to obtain a delegation token");
        }

        // Get the delegation token from Accumulo
        return conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
      } catch (Exception e) {
        final String msg = "Failed to acquire DelegationToken for use with MapReduce";
        log.error(msg, e);
        throw new RuntimeException(msg, e);
      }
    }
    return authToken;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/DelegationTokenConfig.java b/core/src/main/java/org/apache/accumulo/core/client/admin/DelegationTokenConfig.java
new file mode 100644
index 000000000..2e25c3dfc
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/DelegationTokenConfig.java
@@ -0,0 +1,84 @@
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
package org.apache.accumulo.core.client.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.security.tokens.DelegationToken;

/**
 * Configuration options for obtaining a {@link DelegationToken}
 *
 * @since 1.7.0
 */
public class DelegationTokenConfig {

  private long lifetime = 0;

  /**
   * Requests a specific lifetime for the token that is different than the default system lifetime. The lifetime must not exceed the secret key lifetime
   * configured on the servers.
   *
   * @param lifetime
   *          Token lifetime
   * @param unit
   *          Unit of time for the lifetime
   * @return this
   */
  public DelegationTokenConfig setTokenLifetime(long lifetime, TimeUnit unit) {
    checkArgument(0 <= lifetime, "Lifetime must be non-negative");
    checkNotNull(unit, "TimeUnit was null");
    this.lifetime = TimeUnit.MILLISECONDS.convert(lifetime, unit);
    return this;
  }

  /**
   * The current token lifetime. A value of zero corresponds to using the system configured lifetime.
   *
   * @param unit
   *          The unit of time the lifetime should be returned in
   * @return Token lifetime in requested unit of time
   */
  public long getTokenLifetime(TimeUnit unit) {
    checkNotNull(unit);
    return unit.convert(lifetime, TimeUnit.MILLISECONDS);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DelegationTokenConfig) {
      DelegationTokenConfig other = (DelegationTokenConfig) o;
      return lifetime == other.lifetime;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Long.valueOf(lifetime).hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(32);
    sb.append("DelegationTokenConfig[lifetime=").append(lifetime).append("ms]");
    return sb.toString();
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperations.java b/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperations.java
index efeafc0b5..2682f95ed 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/SecurityOperations.java
@@ -21,6 +21,7 @@ import java.util.Set;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.NamespacePermission;
@@ -350,4 +351,11 @@ public interface SecurityOperations {
    */
   Set<String> listLocalUsers() throws AccumuloException, AccumuloSecurityException;
 
  /**
   * Obtain a {@link DelegationToken} for use when Kerberos credentials are unavailable (e.g. YARN Jobs)
   *
   * @return a {@link DelegationToken} for this user
   * @since 1.7.0
   */
  DelegationToken getDelegationToken(DelegationTokenConfig cfg) throws AccumuloException, AccumuloSecurityException;
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ClientContext.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ClientContext.java
index 8470da45c..7c2fb1b0d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ClientContext.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ClientContext.java
@@ -52,11 +52,11 @@ public class ClientContext {
 
   private static final Logger log = LoggerFactory.getLogger(ClientContext.class);
 
  private final Instance inst;
  protected final Instance inst;
   private Credentials creds;
   private ClientConfiguration clientConf;
   private final AccumuloConfiguration rpcConf;
  private Connector conn;
  protected Connector conn;
 
   /**
    * Instantiate a client context
@@ -122,12 +122,21 @@ public class ClientContext {
   /**
    * Retrieve SASL configuration to initiate an RPC connection to a server
    */
  public SaslConnectionParams getClientSaslParams() {
  public SaslConnectionParams getSaslParams() {
    final boolean defaultVal = Boolean.parseBoolean(ClientProperty.INSTANCE_RPC_SASL_ENABLED.getDefaultValue());

     // Use the clientConf if we have it
     if (null != clientConf) {
      return SaslConnectionParams.forConfig(clientConf);
      if (!clientConf.getBoolean(ClientProperty.INSTANCE_RPC_SASL_ENABLED.getKey(), defaultVal)) {
        return null;
      }
      return new SaslConnectionParams(clientConf, creds.getToken());
    }
    AccumuloConfiguration conf = getConfiguration();
    if (!conf.getBoolean(Property.INSTANCE_RPC_SASL_ENABLED)) {
      return null;
     }
    return SaslConnectionParams.forConfig(getConfiguration());
    return new SaslConnectionParams(conf, creds.getToken());
   }
 
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializer.java b/core/src/main/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializer.java
new file mode 100644
index 000000000..934079d98
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializer.java
@@ -0,0 +1,54 @@
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
package org.apache.accumulo.core.client.impl;

import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.security.thrift.TDelegationTokenConfig;

/**
 * Handles serialization of {@link DelegationTokenConfig}
 */
public class DelegationTokenConfigSerializer {

  /**
   * Serialize the delegation token config into the thrift variant
   *
   * @param config
   *          The configuration
   */
  public static TDelegationTokenConfig serialize(DelegationTokenConfig config) {
    TDelegationTokenConfig tconfig = new TDelegationTokenConfig();
    tconfig.setLifetime(config.getTokenLifetime(TimeUnit.MILLISECONDS));
    return tconfig;
  }

  /**
   * Deserialize the Thrift delegation token config into the non-thrift variant
   *
   * @param tconfig
   *          The thrift configuration
   */
  public static DelegationTokenConfig deserialize(TDelegationTokenConfig tconfig) {
    DelegationTokenConfig config = new DelegationTokenConfig();
    if (tconfig.isSetLifetime()) {
      config.setTokenLifetime(tconfig.getLifetime(), TimeUnit.MILLISECONDS);
    }
    return config;
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/SecurityOperationsImpl.java b/core/src/main/java/org/apache/accumulo/core/client/impl/SecurityOperationsImpl.java
index feb1ee7c5..dbaa9d1e2 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/SecurityOperationsImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/SecurityOperationsImpl.java
@@ -23,6 +23,8 @@ import java.util.Set;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
 import org.apache.accumulo.core.client.admin.SecurityOperations;
 import org.apache.accumulo.core.client.impl.thrift.ClientService;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
@@ -30,12 +32,17 @@ import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
 import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.master.thrift.MasterClientService.Client;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.security.NamespacePermission;
 import org.apache.accumulo.core.security.SystemPermission;
 import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.security.thrift.TDelegationToken;
import org.apache.accumulo.core.security.thrift.TDelegationTokenConfig;
 import org.apache.accumulo.core.trace.Tracer;
 import org.apache.accumulo.core.util.ByteBufferUtil;
 
@@ -344,4 +351,32 @@ public class SecurityOperationsImpl implements SecurityOperations {
     });
   }
 
  @Override
  public DelegationToken getDelegationToken(DelegationTokenConfig cfg) throws AccumuloException, AccumuloSecurityException {
    final TDelegationTokenConfig tConfig;
    if (null != cfg) {
      tConfig = DelegationTokenConfigSerializer.serialize(cfg);
    } else {
      tConfig = new TDelegationTokenConfig();
    }

    TDelegationToken thriftToken;
    try {
      thriftToken = MasterClient.execute(context, new ClientExecReturn<TDelegationToken,Client>() {
        @Override
        public TDelegationToken execute(Client client) throws Exception {
          return client.getDelegationToken(Tracer.traceInfo(), context.rpcCreds(), tConfig);
        }
      });
    } catch (TableNotFoundException e) {
      // should never happen
      throw new AssertionError("Received TableNotFoundException on method which should not throw that exception", e);
    }

    AuthenticationTokenIdentifier identifier = new AuthenticationTokenIdentifier(thriftToken.getIdentifier());

    // Get the password out of the thrift delegation token
    return new DelegationToken(thriftToken.getPassword(), identifier);
  }

 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java
index a84311139..891d6e1dd 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftTransportKey.java
@@ -39,7 +39,7 @@ public class ThriftTransportKey {
     this.server = server;
     this.timeout = timeout;
     this.sslParams = context.getClientSslParams();
    this.saslParams = context.getClientSaslParams();
    this.saslParams = context.getSaslParams();
     if (null != saslParams) {
       // TSasl and TSSL transport factories don't play nicely together
       if (null != sslParams) {
@@ -97,7 +97,7 @@ public class ThriftTransportKey {
     if (isSsl()) {
       prefix = "ssl:";
     } else if (isSasl()) {
      prefix = "sasl:" + saslParams.getPrincipal() + "@";
      prefix = saslParams.toString() + ":";
     }
     return prefix + server + " (" + Long.toString(timeout) + ")";
   }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
index b83a02403..0ce05d719 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
@@ -39,20 +39,26 @@ import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
 import org.apache.accumulo.core.client.impl.ClientContext;
 import org.apache.accumulo.core.client.impl.OfflineScanner;
 import org.apache.accumulo.core.client.impl.ScannerImpl;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.util.Pair;
@@ -62,6 +68,7 @@ import org.apache.hadoop.mapred.InputFormat;
 import org.apache.hadoop.mapred.InputSplit;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.security.token.Token;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 
@@ -77,8 +84,9 @@ public abstract class AbstractInputFormat<K,V> implements InputFormat<K,V> {
    * Sets the connector information needed to communicate with Accumulo in this job.
    *
    * <p>
   * <b>WARNING:</b> The serialized token is stored in the configuration and shared with all MapReduce tasks. It is BASE64 encoded to provide a charset safe
   * conversion to a string, and is not intended to be secure.
   * <b>WARNING:</b> Some tokens, when serialized, divulge sensitive information in the configuration as a means to pass the token to MapReduce tasks. This
   * information is BASE64 encoded to provide a charset safe conversion to a string, but this conversion is not intended to be secure. {@link PasswordToken} is
   * one example that is insecure in this way; however {@link DelegationToken}s, acquired using a {@link KerberosToken}, is not subject to this concern.
    *
    * @param job
    *          the Hadoop job instance to be configured
@@ -89,6 +97,29 @@ public abstract class AbstractInputFormat<K,V> implements InputFormat<K,V> {
    * @since 1.5.0
    */
   public static void setConnectorInfo(JobConf job, String principal, AuthenticationToken token) throws AccumuloSecurityException {
    if (token instanceof KerberosToken) {
      log.info("Received KerberosToken, attempting to fetch DelegationToken");
      try {
        Instance instance = getInstance(job);
        Connector conn = instance.getConnector(principal, token);
        token = conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
      } catch (Exception e) {
        log.warn("Failed to automatically obtain DelegationToken, Mappers/Reducers will likely fail to communicate with Accumulo", e);
      }
    }
    // DelegationTokens can be passed securely from user to task without serializing insecurely in the configuration
    if (token instanceof DelegationToken) {
      DelegationToken delegationToken = (DelegationToken) token;

      // Convert it into a Hadoop Token
      AuthenticationTokenIdentifier identifier = delegationToken.getIdentifier();
      Token<AuthenticationTokenIdentifier> hadoopToken = new Token<>(identifier.getBytes(), delegationToken.getPassword(), identifier.getKind(),
          delegationToken.getServiceName());

      // Add the Hadoop Token to the Job so it gets serialized and passed along.
      job.getCredentials().addToken(hadoopToken.getService(), hadoopToken);
    }

     InputConfigurator.setConnectorInfo(CLASS, job, principal, token);
   }
 
@@ -147,7 +178,8 @@ public abstract class AbstractInputFormat<K,V> implements InputFormat<K,V> {
    * @see #setConnectorInfo(JobConf, String, String)
    */
   protected static AuthenticationToken getAuthenticationToken(JobConf job) {
    return InputConfigurator.getAuthenticationToken(CLASS, job);
    AuthenticationToken token = InputConfigurator.getAuthenticationToken(CLASS, job);
    return ConfiguratorBase.unwrapAuthenticationToken(job, token);
   }
 
   /**
@@ -284,7 +316,18 @@ public abstract class AbstractInputFormat<K,V> implements InputFormat<K,V> {
    * @since 1.5.0
    */
   protected static void validateOptions(JobConf job) throws IOException {
    InputConfigurator.validateOptions(CLASS, job);
    final Instance inst = InputConfigurator.validateInstance(CLASS, job);
    String principal = InputConfigurator.getPrincipal(CLASS, job);
    AuthenticationToken token = InputConfigurator.getAuthenticationToken(CLASS, job);
    // In secure mode, we need to convert the DelegationTokenStub into a real DelegationToken
    token = ConfiguratorBase.unwrapAuthenticationToken(job, token);
    Connector conn;
    try {
      conn = inst.getConnector(principal, token);
    } catch (Exception e) {
      throw new IOException(e);
    }
    InputConfigurator.validatePermissions(CLASS, job, conn);
   }
 
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
index f877ec60d..4e95a4a8d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloOutputFormat.java
@@ -34,6 +34,7 @@ import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.OutputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
@@ -168,7 +169,8 @@ public class AccumuloOutputFormat implements OutputFormat<Text,Mutation> {
    * @see #setConnectorInfo(JobConf, String, String)
    */
   protected static AuthenticationToken getAuthenticationToken(JobConf job) {
    return OutputConfigurator.getAuthenticationToken(CLASS, job);
    AuthenticationToken token = OutputConfigurator.getAuthenticationToken(CLASS, job);
    return ConfiguratorBase.unwrapAuthenticationToken(job, token);
   }
 
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
index 5c7b78056..e1b35b282 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
@@ -39,23 +39,30 @@ import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
 import org.apache.accumulo.core.client.impl.ClientContext;
 import org.apache.accumulo.core.client.impl.OfflineScanner;
 import org.apache.accumulo.core.client.impl.ScannerImpl;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.mapreduce.InputFormat;
 import org.apache.hadoop.mapreduce.InputSplit;
@@ -63,6 +70,7 @@ import org.apache.hadoop.mapreduce.Job;
 import org.apache.hadoop.mapreduce.JobContext;
 import org.apache.hadoop.mapreduce.RecordReader;
 import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.security.token.Token;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 
@@ -79,8 +87,9 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
    * Sets the connector information needed to communicate with Accumulo in this job.
    *
    * <p>
   * <b>WARNING:</b> The serialized token is stored in the configuration and shared with all MapReduce tasks. It is BASE64 encoded to provide a charset safe
   * conversion to a string, and is not intended to be secure.
   * <b>WARNING:</b> For {@link PasswordToken}, the serialized token is stored in the configuration and shared with all MapReduce tasks. It is BASE64 encoded to
   * provide a charset safe conversion to a string, and is not intended to be secure. This is not the case for {@link KerberosToken} and the corresponding
   * {@link DelegationToken} acquired using the KerberosToken.
    *
    * @param job
    *          the Hadoop job instance to be configured
@@ -91,6 +100,29 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
    * @since 1.5.0
    */
   public static void setConnectorInfo(Job job, String principal, AuthenticationToken token) throws AccumuloSecurityException {
    if (token instanceof KerberosToken) {
      log.info("Received KerberosToken, attempting to fetch DelegationToken");
      try {
        Instance instance = getInstance(job);
        Connector conn = instance.getConnector(principal, token);
        token = conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
      } catch (Exception e) {
        log.warn("Failed to automatically obtain DelegationToken, Mappers/Reducers will likely fail to communicate with Accumulo", e);
      }
    }
    // DelegationTokens can be passed securely from user to task without serializing insecurely in the configuration
    if (token instanceof DelegationToken) {
      DelegationToken delegationToken = (DelegationToken) token;

      // Convert it into a Hadoop Token
      AuthenticationTokenIdentifier identifier = delegationToken.getIdentifier();
      Token<AuthenticationTokenIdentifier> hadoopToken = new Token<>(identifier.getBytes(), delegationToken.getPassword(), identifier.getKind(),
          delegationToken.getServiceName());

      // Add the Hadoop Token to the Job so it gets serialized and passed along.
      job.getCredentials().addToken(hadoopToken.getService(), hadoopToken);
    }

     InputConfigurator.setConnectorInfo(CLASS, job.getConfiguration(), principal, token);
   }
 
@@ -171,7 +203,8 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
    * @see #setConnectorInfo(Job, String, String)
    */
   protected static AuthenticationToken getAuthenticationToken(JobContext context) {
    return InputConfigurator.getAuthenticationToken(CLASS, context.getConfiguration());
    AuthenticationToken token = InputConfigurator.getAuthenticationToken(CLASS, context.getConfiguration());
    return ConfiguratorBase.unwrapAuthenticationToken(context, token);
   }
 
   /**
@@ -339,7 +372,19 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
    * @since 1.5.0
    */
   protected static void validateOptions(JobContext context) throws IOException {
    InputConfigurator.validateOptions(CLASS, context.getConfiguration());
    final Configuration conf = context.getConfiguration();
    final Instance inst = InputConfigurator.validateInstance(CLASS, conf);
    String principal = InputConfigurator.getPrincipal(CLASS, conf);
    AuthenticationToken token = InputConfigurator.getAuthenticationToken(CLASS, conf);
    // In secure mode, we need to convert the DelegationTokenStub into a real DelegationToken
    token = ConfiguratorBase.unwrapAuthenticationToken(context, token);
    Connector conn;
    try {
      conn = inst.getConnector(principal, token);
    } catch (Exception e) {
      throw new IOException(e);
    }
    InputConfigurator.validatePermissions(CLASS, conf, conn);
   }
 
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
index 5e0aa73ab..3164e4a07 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
@@ -34,6 +34,7 @@ import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.OutputConfigurator;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.SecurityErrorCode;
@@ -169,7 +170,8 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setConnectorInfo(Job, String, String)
    */
   protected static AuthenticationToken getAuthenticationToken(JobContext context) {
    return OutputConfigurator.getAuthenticationToken(CLASS, context.getConfiguration());
    AuthenticationToken token = OutputConfigurator.getAuthenticationToken(CLASS, context.getConfiguration());
    return ConfiguratorBase.unwrapAuthenticationToken(context, token);
   }
 
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/DelegationTokenStub.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/DelegationTokenStub.java
new file mode 100644
index 000000000..5ad91b5f0
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/DelegationTokenStub.java
@@ -0,0 +1,80 @@
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
package org.apache.accumulo.core.client.mapreduce.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

import javax.security.auth.DestroyFailedException;

import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;

/**
 * An internal stub class for passing DelegationToken information out of the Configuration back up to the appropriate implementation for mapreduce or mapred.
 */
public class DelegationTokenStub implements AuthenticationToken {

  private String serviceName;

  public DelegationTokenStub(String serviceName) {
    checkNotNull(serviceName);
    this.serviceName = serviceName;
  }

  public String getServiceName() {
    return serviceName;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void destroy() throws DestroyFailedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDestroyed() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void init(Properties properties) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<TokenProperty> getProperties() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AuthenticationToken clone() {
    throw new UnsupportedOperationException();
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBase.java
index b2b5150aa..3b5fa3a4c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/ConfiguratorBase.java
@@ -17,8 +17,11 @@
 package org.apache.accumulo.core.client.mapreduce.lib.impl;
 
 import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
 import static java.nio.charset.StandardCharsets.UTF_8;
 
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
@@ -28,15 +31,23 @@ import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.mapreduce.impl.DelegationTokenStub;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.util.Base64;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FSDataInputStream;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.util.StringUtils;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
@@ -56,7 +67,7 @@ public class ConfiguratorBase {
   }
 
   public static enum TokenSource {
    FILE, INLINE;
    FILE, INLINE, JOB;
 
     private String prefix;
 
@@ -138,8 +149,15 @@ public class ConfiguratorBase {
     checkArgument(token != null, "token is null");
     conf.setBoolean(enumToConfKey(implementingClass, ConnectorInfo.IS_CONFIGURED), true);
     conf.set(enumToConfKey(implementingClass, ConnectorInfo.PRINCIPAL), principal);
    conf.set(enumToConfKey(implementingClass, ConnectorInfo.TOKEN),
        TokenSource.INLINE.prefix() + token.getClass().getName() + ":" + Base64.encodeBase64String(AuthenticationTokenSerializer.serialize(token)));
    if (token instanceof DelegationToken) {
      // Avoid serializing the DelegationToken secret in the configuration -- the Job will do that work for us securely
      DelegationToken delToken = (DelegationToken) token;
      conf.set(enumToConfKey(implementingClass, ConnectorInfo.TOKEN), TokenSource.JOB.prefix() + token.getClass().getName() + ":"
          + delToken.getServiceName().toString());
    } else {
      conf.set(enumToConfKey(implementingClass, ConnectorInfo.TOKEN),
          TokenSource.INLINE.prefix() + token.getClass().getName() + ":" + Base64.encodeBase64String(AuthenticationTokenSerializer.serialize(token)));
    }
   }
 
   /**
@@ -230,6 +248,14 @@ public class ConfiguratorBase {
     } else if (token.startsWith(TokenSource.FILE.prefix())) {
       String tokenFileName = token.substring(TokenSource.FILE.prefix().length());
       return getTokenFromFile(conf, getPrincipal(implementingClass, conf), tokenFileName);
    } else if (token.startsWith(TokenSource.JOB.prefix())) {
      String[] args = token.substring(TokenSource.JOB.prefix().length()).split(":", 2);
      if (args.length == 2) {
        String className = args[0], serviceName = args[1];
        if (DelegationToken.class.getName().equals(className)) {
          return new DelegationTokenStub(serviceName);
        }
      }
     }
 
     throw new IllegalStateException("Token was not properly serialized into the configuration");
@@ -401,4 +427,53 @@ public class ConfiguratorBase {
     return conf.getInt(enumToConfKey(GeneralOpts.VISIBILITY_CACHE_SIZE), Constants.DEFAULT_VISIBILITY_CACHE_SIZE);
   }
 
  /**
   * Unwraps the provided {@link AuthenticationToken} if it is an instance of {@link DelegationTokenStub}, reconstituting it from the provided {@link JobConf}.
   *
   * @param job
   *          The job
   * @param token
   *          The authentication token
   */
  public static AuthenticationToken unwrapAuthenticationToken(JobConf job, AuthenticationToken token) {
    checkNotNull(job);
    checkNotNull(token);
    if (token instanceof DelegationTokenStub) {
      DelegationTokenStub delTokenStub = (DelegationTokenStub) token;
      Token<? extends TokenIdentifier> hadoopToken = job.getCredentials().getToken(new Text(delTokenStub.getServiceName()));
      AuthenticationTokenIdentifier identifier = new AuthenticationTokenIdentifier();
      try {
        identifier.readFields(new DataInputStream(new ByteArrayInputStream(hadoopToken.getIdentifier())));
        return new DelegationToken(hadoopToken.getPassword(), identifier);
      } catch (IOException e) {
        throw new RuntimeException("Could not construct DelegationToken from JobConf Credentials", e);
      }
    }
    return token;
  }

  /**
   * Unwraps the provided {@link AuthenticationToken} if it is an instance of {@link DelegationTokenStub}, reconstituting it from the provided {@link JobConf}.
   *
   * @param job
   *          The job
   * @param token
   *          The authentication token
   */
  public static AuthenticationToken unwrapAuthenticationToken(JobContext job, AuthenticationToken token) {
    checkNotNull(job);
    checkNotNull(token);
    if (token instanceof DelegationTokenStub) {
      DelegationTokenStub delTokenStub = (DelegationTokenStub) token;
      Token<? extends TokenIdentifier> hadoopToken = job.getCredentials().getToken(new Text(delTokenStub.getServiceName()));
      AuthenticationTokenIdentifier identifier = new AuthenticationTokenIdentifier();
      try {
        identifier.readFields(new DataInputStream(new ByteArrayInputStream(hadoopToken.getIdentifier())));
        return new DelegationToken(hadoopToken.getPassword(), identifier);
      } catch (IOException e) {
        throw new RuntimeException("Could not construct DelegationToken from JobConf Credentials", e);
      }
    }
    return token;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
index 5405ac031..6a6416651 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
@@ -51,6 +51,7 @@ import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
 import org.apache.accumulo.core.client.mock.impl.MockTabletLocator;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
 import org.apache.accumulo.core.data.PartialKey;
@@ -616,10 +617,73 @@ public class InputConfigurator extends ConfiguratorBase {
     return TabletLocator.getLocator(context, new Text(tableId));
   }
 
  /**
   * Validates and extracts an {@link Instance} from the configuration
   *
   * @param implementingClass
   *          the class whose name will be used as a prefix for the property configuration key
   * @param conf
   *          the Hadoop configuration object to configure
   * @since 1.7.0
   */
  public static Instance validateInstance(Class<?> implementingClass, Configuration conf) throws IOException {
    if (!isConnectorInfoSet(implementingClass, conf))
      throw new IOException("Input info has not been set.");
    String instanceKey = conf.get(enumToConfKey(implementingClass, InstanceOpts.TYPE));
    if (!"MockInstance".equals(instanceKey) && !"ZooKeeperInstance".equals(instanceKey))
      throw new IOException("Instance info has not been set.");
    return getInstance(implementingClass, conf);
  }

  /**
   * Validates that the user has permissions on the requested tables
   *
   * @param implementingClass
   *          the class whose name will be used as a prefix for the property configuration key
   * @param conf
   *          the Hadoop configuration object to configure
   * @param conn
   *          the Connector
   * @see 1.7.0
   */
  public static void validatePermissions(Class<?> implementingClass, Configuration conf, Connector conn) throws IOException {
    Map<String,InputTableConfig> inputTableConfigs = getInputTableConfigs(implementingClass, conf);
    try {
      if (getInputTableConfigs(implementingClass, conf).size() == 0)
        throw new IOException("No table set.");

      for (Map.Entry<String,InputTableConfig> tableConfig : inputTableConfigs.entrySet()) {
        if (!conn.securityOperations().hasTablePermission(getPrincipal(implementingClass, conf), tableConfig.getKey(), TablePermission.READ))
          throw new IOException("Unable to access table");
      }
      for (Map.Entry<String,InputTableConfig> tableConfigEntry : inputTableConfigs.entrySet()) {
        InputTableConfig tableConfig = tableConfigEntry.getValue();
        if (!tableConfig.shouldUseLocalIterators()) {
          if (tableConfig.getIterators() != null) {
            for (IteratorSetting iter : tableConfig.getIterators()) {
              if (!conn.tableOperations().testClassLoad(tableConfigEntry.getKey(), iter.getIteratorClass(), SortedKeyValueIterator.class.getName()))
                throw new AccumuloException("Servers are unable to load " + iter.getIteratorClass() + " as a " + SortedKeyValueIterator.class.getName());
            }
          }
        }
      }
    } catch (AccumuloException e) {
      throw new IOException(e);
    } catch (AccumuloSecurityException e) {
      throw new IOException(e);
    } catch (TableNotFoundException e) {
      throw new IOException(e);
    }
  }

   // InputFormat doesn't have the equivalent of OutputFormat's checkOutputSpecs(JobContext job)
   /**
    * Check whether a configuration is fully configured to be used with an Accumulo {@link org.apache.hadoop.mapreduce.InputFormat}.
    *
   * <p>
   * The implementation (JobContext or JobConf which created the Configuration) needs to be used to extract the proper {@link AuthenticationToken} for
   * {@link DelegationToken} support.
   *
    * @param implementingClass
    *          the class whose name will be used as a prefix for the property configuration key
    * @param conf
@@ -627,7 +691,11 @@ public class InputConfigurator extends ConfiguratorBase {
    * @throws IOException
    *           if the context is improperly configured
    * @since 1.6.0
   *
   * @see #validateInstance(Class, Configuration)
   * @see #validatePermissions(Class, Configuration, Connector)
    */
  @Deprecated
   public static void validateOptions(Class<?> implementingClass, Configuration conf) throws IOException {
 
     Map<String,InputTableConfig> inputTableConfigs = getInputTableConfigs(implementingClass, conf);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mock/MockSecurityOperations.java b/core/src/main/java/org/apache/accumulo/core/client/mock/MockSecurityOperations.java
index db88cfb05..cc51a4792 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mock/MockSecurityOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mock/MockSecurityOperations.java
@@ -21,9 +21,11 @@ import java.util.Set;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
 import org.apache.accumulo.core.client.admin.SecurityOperations;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.NamespacePermission;
@@ -222,4 +224,9 @@ class MockSecurityOperations implements SecurityOperations {
     return acu.users.keySet();
   }
 
  @Override
  public DelegationToken getDelegationToken(DelegationTokenConfig cfg) throws AccumuloException, AccumuloSecurityException {
    return null;
  }

 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/security/tokens/DelegationToken.java b/core/src/main/java/org/apache/accumulo/core/client/security/tokens/DelegationToken.java
new file mode 100644
index 000000000..bc0251f1d
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/security/tokens/DelegationToken.java
@@ -0,0 +1,163 @@
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
package org.apache.accumulo.core.client.security.tokens;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AuthenticationToken} that wraps a "Hadoop style" delegation token created by Accumulo. The only intended scope of this implementation is when a
 * KerberosToken cannot be used instead. The most common reason for this is within YARN jobs. The Kerberos credentials of the user are not passed over the wire
 * to the job itself. The delegation token serves as a mechanism to obtain a shared secret with Accumulo using a {@link KerberosToken} and then run some task
 * authenticating with that shared secret, this {@link DelegationToken}.
 *
 * @since 1.7.0
 */
public class DelegationToken extends PasswordToken {
  private static final Logger log = LoggerFactory.getLogger(DelegationToken.class);

  public static final String SERVICE_NAME = "AccumuloDelegationToken";

  private AuthenticationTokenIdentifier identifier;

  public DelegationToken() {
    super();
  }

  public DelegationToken(byte[] delegationTokenPassword, AuthenticationTokenIdentifier identifier) {
    checkNotNull(delegationTokenPassword);
    checkNotNull(identifier);
    setPassword(delegationTokenPassword);
    this.identifier = identifier;
  }

  public DelegationToken(Instance instance, UserGroupInformation user, AuthenticationTokenIdentifier identifier) {
    checkNotNull(instance);
    checkNotNull(user);
    checkNotNull(identifier);

    Credentials creds = user.getCredentials();
    Token<? extends TokenIdentifier> token = creds.getToken(new Text(SERVICE_NAME + "-" + instance.getInstanceID()));
    if (null == token) {
      throw new IllegalArgumentException("Did not find Accumulo delegation token in provided UserGroupInformation");
    }
    setPasswordFromToken(token, identifier);
  }

  public DelegationToken(Token<? extends TokenIdentifier> token, AuthenticationTokenIdentifier identifier) {
    checkNotNull(token);
    checkNotNull(identifier);
    setPasswordFromToken(token, identifier);
  }

  private void setPasswordFromToken(Token<? extends TokenIdentifier> token, AuthenticationTokenIdentifier identifier) {
    if (!AuthenticationTokenIdentifier.TOKEN_KIND.equals(token.getKind())) {
      String msg = "Expected an AuthenticationTokenIdentifier but got a " + token.getKind();
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }

    setPassword(token.getPassword());
    this.identifier = identifier;
  }

  /**
   * The identifier for this token, may be null.
   */
  public AuthenticationTokenIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * The service name used to identify this {@link Token}
   *
   * @see Token#Constructor(byte[], byte[], Text, Text)
   */
  public Text getServiceName() {
    checkNotNull(identifier);
    return new Text(SERVICE_NAME + "-" + identifier.getInstanceId());
  }

  @Override
  public void init(Properties properties) {
    // Encourage use of UserGroupInformation as entry point
  }

  @Override
  public Set<TokenProperty> getProperties() {
    // Encourage use of UserGroupInformation as entry point
    return Collections.emptySet();
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    identifier.write(out);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    identifier = new AuthenticationTokenIdentifier();
    identifier.readFields(in);
  }

  @Override
  public DelegationToken clone() {
    DelegationToken copy = new DelegationToken();
    copy.setPassword(getPassword());
    copy.identifier = new AuthenticationTokenIdentifier(identifier);
    return copy;
  }

  @Override
  public int hashCode() {
    return super.hashCode() ^ identifier.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof DelegationToken))
      return false;
    DelegationToken other = (DelegationToken) obj;
    if (!Arrays.equals(getPassword(), other.getPassword())) {
      return false;
    }
    return identifier.equals(other.identifier);
  }

}
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/Property.java b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
index 68fac7329..01f03cfff 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/Property.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
@@ -188,6 +188,10 @@ public enum Property {
       "Comma-separated list of paths to CredentialProviders"),
   GENERAL_LEGACY_METRICS("general.legacy.metrics", "false", PropertyType.BOOLEAN,
       "Use the old metric infrastructure configured by accumulo-metrics.xml, instead of Hadoop Metrics2"),
  GENERAL_DELEGATION_TOKEN_LIFETIME("general.delegation.token.lifetime", "7d", PropertyType.TIMEDURATION,
      "The length of time that delegation tokens and secret keys are valid"),
  GENERAL_DELEGATION_TOKEN_UPDATE_INTERVAL("general.delegation.token.update.interval", "1d", PropertyType.TIMEDURATION,
      "The length of time between generation of new secret keys"),
 
   // properties that are specific to master server behavior
   MASTER_PREFIX("master.", null, PropertyType.PREFIX, "Properties in this category affect the behavior of the master server"),
diff --git a/core/src/main/java/org/apache/accumulo/core/master/thrift/MasterClientService.java b/core/src/main/java/org/apache/accumulo/core/master/thrift/MasterClientService.java
index 4b90a342c..9cd1084ac 100644
-- a/core/src/main/java/org/apache/accumulo/core/master/thrift/MasterClientService.java
++ b/core/src/main/java/org/apache/accumulo/core/master/thrift/MasterClientService.java
@@ -84,6 +84,8 @@ import org.slf4j.LoggerFactory;
 
     public List<String> getActiveTservers(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException;
 
    public org.apache.accumulo.core.security.thrift.TDelegationToken getDelegationToken(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException;

   }
 
   public interface AsyncIface extends FateService .AsyncIface {
@@ -120,6 +122,8 @@ import org.slf4j.LoggerFactory;
 
     public void getActiveTservers(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
    public void getDelegationToken(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;

   }
 
   public static class Client extends FateService.Client implements Iface {
@@ -555,6 +559,34 @@ import org.slf4j.LoggerFactory;
       throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "getActiveTservers failed: unknown result");
     }
 
    public org.apache.accumulo.core.security.thrift.TDelegationToken getDelegationToken(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException
    {
      send_getDelegationToken(tinfo, credentials, cfg);
      return recv_getDelegationToken();
    }

    public void send_getDelegationToken(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg) throws org.apache.thrift.TException
    {
      getDelegationToken_args args = new getDelegationToken_args();
      args.setTinfo(tinfo);
      args.setCredentials(credentials);
      args.setCfg(cfg);
      sendBase("getDelegationToken", args);
    }

    public org.apache.accumulo.core.security.thrift.TDelegationToken recv_getDelegationToken() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException
    {
      getDelegationToken_result result = new getDelegationToken_result();
      receiveBase(result, "getDelegationToken");
      if (result.isSetSuccess()) {
        return result.success;
      }
      if (result.sec != null) {
        throw result.sec;
      }
      throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "getDelegationToken failed: unknown result");
    }

   }
   public static class AsyncClient extends FateService.AsyncClient implements AsyncIface {
     public static class Factory implements org.apache.thrift.async.TAsyncClientFactory<AsyncClient> {
@@ -1212,6 +1244,44 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public void getDelegationToken(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
      checkReady();
      getDelegationToken_call method_call = new getDelegationToken_call(tinfo, credentials, cfg, resultHandler, this, ___protocolFactory, ___transport);
      this.___currentMethod = method_call;
      ___manager.call(method_call);
    }

    public static class getDelegationToken_call extends org.apache.thrift.async.TAsyncMethodCall {
      private org.apache.accumulo.core.trace.thrift.TInfo tinfo;
      private org.apache.accumulo.core.security.thrift.TCredentials credentials;
      private org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg;
      public getDelegationToken_call(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
        super(client, protocolFactory, transport, resultHandler, false);
        this.tinfo = tinfo;
        this.credentials = credentials;
        this.cfg = cfg;
      }

      public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
        prot.writeMessageBegin(new org.apache.thrift.protocol.TMessage("getDelegationToken", org.apache.thrift.protocol.TMessageType.CALL, 0));
        getDelegationToken_args args = new getDelegationToken_args();
        args.setTinfo(tinfo);
        args.setCredentials(credentials);
        args.setCfg(cfg);
        args.write(prot);
        prot.writeMessageEnd();
      }

      public org.apache.accumulo.core.security.thrift.TDelegationToken getResult() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException {
        if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
          throw new IllegalStateException("Method call not finished!");
        }
        org.apache.thrift.transport.TMemoryInputTransport memoryTransport = new org.apache.thrift.transport.TMemoryInputTransport(getFrameBuffer().array());
        org.apache.thrift.protocol.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        return (new Client(prot)).recv_getDelegationToken();
      }
    }

   }
 
   public static class Processor<I extends Iface> extends FateService.Processor<I> implements org.apache.thrift.TProcessor {
@@ -1241,6 +1311,7 @@ import org.slf4j.LoggerFactory;
       processMap.put("reportSplitExtent", new reportSplitExtent());
       processMap.put("reportTabletStatus", new reportTabletStatus());
       processMap.put("getActiveTservers", new getActiveTservers());
      processMap.put("getDelegationToken", new getDelegationToken());
       return processMap;
     }
 
@@ -1627,6 +1698,30 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public static class getDelegationToken<I extends Iface> extends org.apache.thrift.ProcessFunction<I, getDelegationToken_args> {
      public getDelegationToken() {
        super("getDelegationToken");
      }

      public getDelegationToken_args getEmptyArgsInstance() {
        return new getDelegationToken_args();
      }

      protected boolean isOneway() {
        return false;
      }

      public getDelegationToken_result getResult(I iface, getDelegationToken_args args) throws org.apache.thrift.TException {
        getDelegationToken_result result = new getDelegationToken_result();
        try {
          result.success = iface.getDelegationToken(args.tinfo, args.credentials, args.cfg);
        } catch (org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec) {
          result.sec = sec;
        }
        return result;
      }
    }

   }
 
   public static class AsyncProcessor<I extends AsyncIface> extends FateService.AsyncProcessor<I> {
@@ -1656,6 +1751,7 @@ import org.slf4j.LoggerFactory;
       processMap.put("reportSplitExtent", new reportSplitExtent());
       processMap.put("reportTabletStatus", new reportTabletStatus());
       processMap.put("getActiveTservers", new getActiveTservers());
      processMap.put("getDelegationToken", new getDelegationToken());
       return processMap;
     }
 
@@ -2527,6 +2623,63 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public static class getDelegationToken<I extends AsyncIface> extends org.apache.thrift.AsyncProcessFunction<I, getDelegationToken_args, org.apache.accumulo.core.security.thrift.TDelegationToken> {
      public getDelegationToken() {
        super("getDelegationToken");
      }

      public getDelegationToken_args getEmptyArgsInstance() {
        return new getDelegationToken_args();
      }

      public AsyncMethodCallback<org.apache.accumulo.core.security.thrift.TDelegationToken> getResultHandler(final AsyncFrameBuffer fb, final int seqid) {
        final org.apache.thrift.AsyncProcessFunction fcall = this;
        return new AsyncMethodCallback<org.apache.accumulo.core.security.thrift.TDelegationToken>() { 
          public void onComplete(org.apache.accumulo.core.security.thrift.TDelegationToken o) {
            getDelegationToken_result result = new getDelegationToken_result();
            result.success = o;
            try {
              fcall.sendResponse(fb,result, org.apache.thrift.protocol.TMessageType.REPLY,seqid);
              return;
            } catch (Exception e) {
              LOGGER.error("Exception writing to internal frame buffer", e);
            }
            fb.close();
          }
          public void onError(Exception e) {
            byte msgType = org.apache.thrift.protocol.TMessageType.REPLY;
            org.apache.thrift.TBase msg;
            getDelegationToken_result result = new getDelegationToken_result();
            if (e instanceof org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException) {
                        result.sec = (org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException) e;
                        result.setSecIsSet(true);
                        msg = result;
            }
             else 
            {
              msgType = org.apache.thrift.protocol.TMessageType.EXCEPTION;
              msg = (org.apache.thrift.TBase)new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.INTERNAL_ERROR, e.getMessage());
            }
            try {
              fcall.sendResponse(fb,msg,msgType,seqid);
              return;
            } catch (Exception ex) {
              LOGGER.error("Exception writing to internal frame buffer", ex);
            }
            fb.close();
          }
        };
      }

      protected boolean isOneway() {
        return false;
      }

      public void start(I iface, getDelegationToken_args args, org.apache.thrift.async.AsyncMethodCallback<org.apache.accumulo.core.security.thrift.TDelegationToken> resultHandler) throws TException {
        iface.getDelegationToken(args.tinfo, args.credentials, args.cfg,resultHandler);
      }
    }

   }
 
   public static class initiateFlush_args implements org.apache.thrift.TBase<initiateFlush_args, initiateFlush_args._Fields>, java.io.Serializable, Cloneable, Comparable<initiateFlush_args>   {
@@ -18540,4 +18693,1034 @@ import org.slf4j.LoggerFactory;
 
   }
 
  public static class getDelegationToken_args implements org.apache.thrift.TBase<getDelegationToken_args, getDelegationToken_args._Fields>, java.io.Serializable, Cloneable, Comparable<getDelegationToken_args>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("getDelegationToken_args");

    private static final org.apache.thrift.protocol.TField TINFO_FIELD_DESC = new org.apache.thrift.protocol.TField("tinfo", org.apache.thrift.protocol.TType.STRUCT, (short)1);
    private static final org.apache.thrift.protocol.TField CREDENTIALS_FIELD_DESC = new org.apache.thrift.protocol.TField("credentials", org.apache.thrift.protocol.TType.STRUCT, (short)2);
    private static final org.apache.thrift.protocol.TField CFG_FIELD_DESC = new org.apache.thrift.protocol.TField("cfg", org.apache.thrift.protocol.TType.STRUCT, (short)3);

    private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
    static {
      schemes.put(StandardScheme.class, new getDelegationToken_argsStandardSchemeFactory());
      schemes.put(TupleScheme.class, new getDelegationToken_argsTupleSchemeFactory());
    }

    public org.apache.accumulo.core.trace.thrift.TInfo tinfo; // required
    public org.apache.accumulo.core.security.thrift.TCredentials credentials; // required
    public org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      TINFO((short)1, "tinfo"),
      CREDENTIALS((short)2, "credentials"),
      CFG((short)3, "cfg");

      private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

      static {
        for (_Fields field : EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          case 1: // TINFO
            return TINFO;
          case 2: // CREDENTIALS
            return CREDENTIALS;
          case 3: // CFG
            return CFG;
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      public static _Fields findByName(String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final String _fieldName;

      _Fields(short thriftId, String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public String getFieldName() {
        return _fieldName;
      }
    }

    // isset id assignments
    public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.TINFO, new org.apache.thrift.meta_data.FieldMetaData("tinfo", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.trace.thrift.TInfo.class)));
      tmpMap.put(_Fields.CREDENTIALS, new org.apache.thrift.meta_data.FieldMetaData("credentials", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.security.thrift.TCredentials.class)));
      tmpMap.put(_Fields.CFG, new org.apache.thrift.meta_data.FieldMetaData("cfg", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.security.thrift.TDelegationTokenConfig.class)));
      metaDataMap = Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(getDelegationToken_args.class, metaDataMap);
    }

    public getDelegationToken_args() {
    }

    public getDelegationToken_args(
      org.apache.accumulo.core.trace.thrift.TInfo tinfo,
      org.apache.accumulo.core.security.thrift.TCredentials credentials,
      org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg)
    {
      this();
      this.tinfo = tinfo;
      this.credentials = credentials;
      this.cfg = cfg;
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public getDelegationToken_args(getDelegationToken_args other) {
      if (other.isSetTinfo()) {
        this.tinfo = new org.apache.accumulo.core.trace.thrift.TInfo(other.tinfo);
      }
      if (other.isSetCredentials()) {
        this.credentials = new org.apache.accumulo.core.security.thrift.TCredentials(other.credentials);
      }
      if (other.isSetCfg()) {
        this.cfg = new org.apache.accumulo.core.security.thrift.TDelegationTokenConfig(other.cfg);
      }
    }

    public getDelegationToken_args deepCopy() {
      return new getDelegationToken_args(this);
    }

    @Override
    public void clear() {
      this.tinfo = null;
      this.credentials = null;
      this.cfg = null;
    }

    public org.apache.accumulo.core.trace.thrift.TInfo getTinfo() {
      return this.tinfo;
    }

    public getDelegationToken_args setTinfo(org.apache.accumulo.core.trace.thrift.TInfo tinfo) {
      this.tinfo = tinfo;
      return this;
    }

    public void unsetTinfo() {
      this.tinfo = null;
    }

    /** Returns true if field tinfo is set (has been assigned a value) and false otherwise */
    public boolean isSetTinfo() {
      return this.tinfo != null;
    }

    public void setTinfoIsSet(boolean value) {
      if (!value) {
        this.tinfo = null;
      }
    }

    public org.apache.accumulo.core.security.thrift.TCredentials getCredentials() {
      return this.credentials;
    }

    public getDelegationToken_args setCredentials(org.apache.accumulo.core.security.thrift.TCredentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public void unsetCredentials() {
      this.credentials = null;
    }

    /** Returns true if field credentials is set (has been assigned a value) and false otherwise */
    public boolean isSetCredentials() {
      return this.credentials != null;
    }

    public void setCredentialsIsSet(boolean value) {
      if (!value) {
        this.credentials = null;
      }
    }

    public org.apache.accumulo.core.security.thrift.TDelegationTokenConfig getCfg() {
      return this.cfg;
    }

    public getDelegationToken_args setCfg(org.apache.accumulo.core.security.thrift.TDelegationTokenConfig cfg) {
      this.cfg = cfg;
      return this;
    }

    public void unsetCfg() {
      this.cfg = null;
    }

    /** Returns true if field cfg is set (has been assigned a value) and false otherwise */
    public boolean isSetCfg() {
      return this.cfg != null;
    }

    public void setCfgIsSet(boolean value) {
      if (!value) {
        this.cfg = null;
      }
    }

    public void setFieldValue(_Fields field, Object value) {
      switch (field) {
      case TINFO:
        if (value == null) {
          unsetTinfo();
        } else {
          setTinfo((org.apache.accumulo.core.trace.thrift.TInfo)value);
        }
        break;

      case CREDENTIALS:
        if (value == null) {
          unsetCredentials();
        } else {
          setCredentials((org.apache.accumulo.core.security.thrift.TCredentials)value);
        }
        break;

      case CFG:
        if (value == null) {
          unsetCfg();
        } else {
          setCfg((org.apache.accumulo.core.security.thrift.TDelegationTokenConfig)value);
        }
        break;

      }
    }

    public Object getFieldValue(_Fields field) {
      switch (field) {
      case TINFO:
        return getTinfo();

      case CREDENTIALS:
        return getCredentials();

      case CFG:
        return getCfg();

      }
      throw new IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new IllegalArgumentException();
      }

      switch (field) {
      case TINFO:
        return isSetTinfo();
      case CREDENTIALS:
        return isSetCredentials();
      case CFG:
        return isSetCfg();
      }
      throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
      if (that == null)
        return false;
      if (that instanceof getDelegationToken_args)
        return this.equals((getDelegationToken_args)that);
      return false;
    }

    public boolean equals(getDelegationToken_args that) {
      if (that == null)
        return false;

      boolean this_present_tinfo = true && this.isSetTinfo();
      boolean that_present_tinfo = true && that.isSetTinfo();
      if (this_present_tinfo || that_present_tinfo) {
        if (!(this_present_tinfo && that_present_tinfo))
          return false;
        if (!this.tinfo.equals(that.tinfo))
          return false;
      }

      boolean this_present_credentials = true && this.isSetCredentials();
      boolean that_present_credentials = true && that.isSetCredentials();
      if (this_present_credentials || that_present_credentials) {
        if (!(this_present_credentials && that_present_credentials))
          return false;
        if (!this.credentials.equals(that.credentials))
          return false;
      }

      boolean this_present_cfg = true && this.isSetCfg();
      boolean that_present_cfg = true && that.isSetCfg();
      if (this_present_cfg || that_present_cfg) {
        if (!(this_present_cfg && that_present_cfg))
          return false;
        if (!this.cfg.equals(that.cfg))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public int compareTo(getDelegationToken_args other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      lastComparison = Boolean.valueOf(isSetTinfo()).compareTo(other.isSetTinfo());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetTinfo()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.tinfo, other.tinfo);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = Boolean.valueOf(isSetCredentials()).compareTo(other.isSetCredentials());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetCredentials()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.credentials, other.credentials);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = Boolean.valueOf(isSetCfg()).compareTo(other.isSetCfg());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetCfg()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.cfg, other.cfg);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      return 0;
    }

    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("getDelegationToken_args(");
      boolean first = true;

      sb.append("tinfo:");
      if (this.tinfo == null) {
        sb.append("null");
      } else {
        sb.append(this.tinfo);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("credentials:");
      if (this.credentials == null) {
        sb.append("null");
      } else {
        sb.append(this.credentials);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("cfg:");
      if (this.cfg == null) {
        sb.append("null");
      } else {
        sb.append(this.cfg);
      }
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
      if (tinfo != null) {
        tinfo.validate();
      }
      if (credentials != null) {
        credentials.validate();
      }
      if (cfg != null) {
        cfg.validate();
      }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
      try {
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class getDelegationToken_argsStandardSchemeFactory implements SchemeFactory {
      public getDelegationToken_argsStandardScheme getScheme() {
        return new getDelegationToken_argsStandardScheme();
      }
    }

    private static class getDelegationToken_argsStandardScheme extends StandardScheme<getDelegationToken_args> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, getDelegationToken_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 1: // TINFO
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.tinfo = new org.apache.accumulo.core.trace.thrift.TInfo();
                struct.tinfo.read(iprot);
                struct.setTinfoIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 2: // CREDENTIALS
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.credentials = new org.apache.accumulo.core.security.thrift.TCredentials();
                struct.credentials.read(iprot);
                struct.setCredentialsIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 3: // CFG
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.cfg = new org.apache.accumulo.core.security.thrift.TDelegationTokenConfig();
                struct.cfg.read(iprot);
                struct.setCfgIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, getDelegationToken_args struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        if (struct.tinfo != null) {
          oprot.writeFieldBegin(TINFO_FIELD_DESC);
          struct.tinfo.write(oprot);
          oprot.writeFieldEnd();
        }
        if (struct.credentials != null) {
          oprot.writeFieldBegin(CREDENTIALS_FIELD_DESC);
          struct.credentials.write(oprot);
          oprot.writeFieldEnd();
        }
        if (struct.cfg != null) {
          oprot.writeFieldBegin(CFG_FIELD_DESC);
          struct.cfg.write(oprot);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class getDelegationToken_argsTupleSchemeFactory implements SchemeFactory {
      public getDelegationToken_argsTupleScheme getScheme() {
        return new getDelegationToken_argsTupleScheme();
      }
    }

    private static class getDelegationToken_argsTupleScheme extends TupleScheme<getDelegationToken_args> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, getDelegationToken_args struct) throws org.apache.thrift.TException {
        TTupleProtocol oprot = (TTupleProtocol) prot;
        BitSet optionals = new BitSet();
        if (struct.isSetTinfo()) {
          optionals.set(0);
        }
        if (struct.isSetCredentials()) {
          optionals.set(1);
        }
        if (struct.isSetCfg()) {
          optionals.set(2);
        }
        oprot.writeBitSet(optionals, 3);
        if (struct.isSetTinfo()) {
          struct.tinfo.write(oprot);
        }
        if (struct.isSetCredentials()) {
          struct.credentials.write(oprot);
        }
        if (struct.isSetCfg()) {
          struct.cfg.write(oprot);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, getDelegationToken_args struct) throws org.apache.thrift.TException {
        TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(3);
        if (incoming.get(0)) {
          struct.tinfo = new org.apache.accumulo.core.trace.thrift.TInfo();
          struct.tinfo.read(iprot);
          struct.setTinfoIsSet(true);
        }
        if (incoming.get(1)) {
          struct.credentials = new org.apache.accumulo.core.security.thrift.TCredentials();
          struct.credentials.read(iprot);
          struct.setCredentialsIsSet(true);
        }
        if (incoming.get(2)) {
          struct.cfg = new org.apache.accumulo.core.security.thrift.TDelegationTokenConfig();
          struct.cfg.read(iprot);
          struct.setCfgIsSet(true);
        }
      }
    }

  }

  public static class getDelegationToken_result implements org.apache.thrift.TBase<getDelegationToken_result, getDelegationToken_result._Fields>, java.io.Serializable, Cloneable, Comparable<getDelegationToken_result>   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("getDelegationToken_result");

    private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.STRUCT, (short)0);
    private static final org.apache.thrift.protocol.TField SEC_FIELD_DESC = new org.apache.thrift.protocol.TField("sec", org.apache.thrift.protocol.TType.STRUCT, (short)1);

    private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
    static {
      schemes.put(StandardScheme.class, new getDelegationToken_resultStandardSchemeFactory());
      schemes.put(TupleScheme.class, new getDelegationToken_resultTupleSchemeFactory());
    }

    public org.apache.accumulo.core.security.thrift.TDelegationToken success; // required
    public org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      SUCCESS((short)0, "success"),
      SEC((short)1, "sec");

      private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

      static {
        for (_Fields field : EnumSet.allOf(_Fields.class)) {
          byName.put(field.getFieldName(), field);
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, or null if its not found.
       */
      public static _Fields findByThriftId(int fieldId) {
        switch(fieldId) {
          case 0: // SUCCESS
            return SUCCESS;
          case 1: // SEC
            return SEC;
          default:
            return null;
        }
      }

      /**
       * Find the _Fields constant that matches fieldId, throwing an exception
       * if it is not found.
       */
      public static _Fields findByThriftIdOrThrow(int fieldId) {
        _Fields fields = findByThriftId(fieldId);
        if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
        return fields;
      }

      /**
       * Find the _Fields constant that matches name, or null if its not found.
       */
      public static _Fields findByName(String name) {
        return byName.get(name);
      }

      private final short _thriftId;
      private final String _fieldName;

      _Fields(short thriftId, String fieldName) {
        _thriftId = thriftId;
        _fieldName = fieldName;
      }

      public short getThriftFieldId() {
        return _thriftId;
      }

      public String getFieldName() {
        return _fieldName;
      }
    }

    // isset id assignments
    public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.SUCCESS, new org.apache.thrift.meta_data.FieldMetaData("success", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.security.thrift.TDelegationToken.class)));
      tmpMap.put(_Fields.SEC, new org.apache.thrift.meta_data.FieldMetaData("sec", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
      metaDataMap = Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(getDelegationToken_result.class, metaDataMap);
    }

    public getDelegationToken_result() {
    }

    public getDelegationToken_result(
      org.apache.accumulo.core.security.thrift.TDelegationToken success,
      org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec)
    {
      this();
      this.success = success;
      this.sec = sec;
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public getDelegationToken_result(getDelegationToken_result other) {
      if (other.isSetSuccess()) {
        this.success = new org.apache.accumulo.core.security.thrift.TDelegationToken(other.success);
      }
      if (other.isSetSec()) {
        this.sec = new org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException(other.sec);
      }
    }

    public getDelegationToken_result deepCopy() {
      return new getDelegationToken_result(this);
    }

    @Override
    public void clear() {
      this.success = null;
      this.sec = null;
    }

    public org.apache.accumulo.core.security.thrift.TDelegationToken getSuccess() {
      return this.success;
    }

    public getDelegationToken_result setSuccess(org.apache.accumulo.core.security.thrift.TDelegationToken success) {
      this.success = success;
      return this;
    }

    public void unsetSuccess() {
      this.success = null;
    }

    /** Returns true if field success is set (has been assigned a value) and false otherwise */
    public boolean isSetSuccess() {
      return this.success != null;
    }

    public void setSuccessIsSet(boolean value) {
      if (!value) {
        this.success = null;
      }
    }

    public org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException getSec() {
      return this.sec;
    }

    public getDelegationToken_result setSec(org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec) {
      this.sec = sec;
      return this;
    }

    public void unsetSec() {
      this.sec = null;
    }

    /** Returns true if field sec is set (has been assigned a value) and false otherwise */
    public boolean isSetSec() {
      return this.sec != null;
    }

    public void setSecIsSet(boolean value) {
      if (!value) {
        this.sec = null;
      }
    }

    public void setFieldValue(_Fields field, Object value) {
      switch (field) {
      case SUCCESS:
        if (value == null) {
          unsetSuccess();
        } else {
          setSuccess((org.apache.accumulo.core.security.thrift.TDelegationToken)value);
        }
        break;

      case SEC:
        if (value == null) {
          unsetSec();
        } else {
          setSec((org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException)value);
        }
        break;

      }
    }

    public Object getFieldValue(_Fields field) {
      switch (field) {
      case SUCCESS:
        return getSuccess();

      case SEC:
        return getSec();

      }
      throw new IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new IllegalArgumentException();
      }

      switch (field) {
      case SUCCESS:
        return isSetSuccess();
      case SEC:
        return isSetSec();
      }
      throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
      if (that == null)
        return false;
      if (that instanceof getDelegationToken_result)
        return this.equals((getDelegationToken_result)that);
      return false;
    }

    public boolean equals(getDelegationToken_result that) {
      if (that == null)
        return false;

      boolean this_present_success = true && this.isSetSuccess();
      boolean that_present_success = true && that.isSetSuccess();
      if (this_present_success || that_present_success) {
        if (!(this_present_success && that_present_success))
          return false;
        if (!this.success.equals(that.success))
          return false;
      }

      boolean this_present_sec = true && this.isSetSec();
      boolean that_present_sec = true && that.isSetSec();
      if (this_present_sec || that_present_sec) {
        if (!(this_present_sec && that_present_sec))
          return false;
        if (!this.sec.equals(that.sec))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public int compareTo(getDelegationToken_result other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;

      lastComparison = Boolean.valueOf(isSetSuccess()).compareTo(other.isSetSuccess());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetSuccess()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.success, other.success);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = Boolean.valueOf(isSetSec()).compareTo(other.isSetSec());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetSec()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.sec, other.sec);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      return 0;
    }

    public _Fields fieldForId(int fieldId) {
      return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
      schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
      schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
      }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("getDelegationToken_result(");
      boolean first = true;

      sb.append("success:");
      if (this.success == null) {
        sb.append("null");
      } else {
        sb.append(this.success);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("sec:");
      if (this.sec == null) {
        sb.append("null");
      } else {
        sb.append(this.sec);
      }
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
      // check for required fields
      // check for sub-struct validity
      if (success != null) {
        success.validate();
      }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
      try {
        write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
      try {
        read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
      } catch (org.apache.thrift.TException te) {
        throw new java.io.IOException(te);
      }
    }

    private static class getDelegationToken_resultStandardSchemeFactory implements SchemeFactory {
      public getDelegationToken_resultStandardScheme getScheme() {
        return new getDelegationToken_resultStandardScheme();
      }
    }

    private static class getDelegationToken_resultStandardScheme extends StandardScheme<getDelegationToken_result> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, getDelegationToken_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 0: // SUCCESS
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.success = new org.apache.accumulo.core.security.thrift.TDelegationToken();
                struct.success.read(iprot);
                struct.setSuccessIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 1: // SEC
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.sec = new org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException();
                struct.sec.read(iprot);
                struct.setSecIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            default:
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          }
          iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        // check for required fields of primitive type, which can't be checked in the validate method
        struct.validate();
      }

      public void write(org.apache.thrift.protocol.TProtocol oprot, getDelegationToken_result struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        if (struct.success != null) {
          oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
          struct.success.write(oprot);
          oprot.writeFieldEnd();
        }
        if (struct.sec != null) {
          oprot.writeFieldBegin(SEC_FIELD_DESC);
          struct.sec.write(oprot);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class getDelegationToken_resultTupleSchemeFactory implements SchemeFactory {
      public getDelegationToken_resultTupleScheme getScheme() {
        return new getDelegationToken_resultTupleScheme();
      }
    }

    private static class getDelegationToken_resultTupleScheme extends TupleScheme<getDelegationToken_result> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, getDelegationToken_result struct) throws org.apache.thrift.TException {
        TTupleProtocol oprot = (TTupleProtocol) prot;
        BitSet optionals = new BitSet();
        if (struct.isSetSuccess()) {
          optionals.set(0);
        }
        if (struct.isSetSec()) {
          optionals.set(1);
        }
        oprot.writeBitSet(optionals, 2);
        if (struct.isSetSuccess()) {
          struct.success.write(oprot);
        }
        if (struct.isSetSec()) {
          struct.sec.write(oprot);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, getDelegationToken_result struct) throws org.apache.thrift.TException {
        TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(2);
        if (incoming.get(0)) {
          struct.success = new org.apache.accumulo.core.security.thrift.TDelegationToken();
          struct.success.read(iprot);
          struct.setSuccessIsSet(true);
        }
        if (incoming.get(1)) {
          struct.sec = new org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException();
          struct.sec.read(iprot);
          struct.setSecIsSet(true);
        }
      }
    }

  }

 }
diff --git a/core/src/main/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandler.java b/core/src/main/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandler.java
new file mode 100644
index 000000000..18dd7e13f
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandler.java
@@ -0,0 +1,114 @@
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
package org.apache.accumulo.core.rpc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side callbackhandler for sasl authentication which is the client-side sibling to the server-side {@link SaslDigestCallbackHandler}. Encoding of name,
 * password and realm information must be consistent across the pair.
 */
public class SaslClientDigestCallbackHandler extends SaslDigestCallbackHandler {
  private static final Logger log = LoggerFactory.getLogger(SaslClientDigestCallbackHandler.class);
  private static final String NAME = SaslClientDigestCallbackHandler.class.getSimpleName();

  private final String userName;
  private final char[] userPassword;

  public SaslClientDigestCallbackHandler(DelegationToken token) {
    checkNotNull(token);
    this.userName = encodeIdentifier(token.getIdentifier().getBytes());
    this.userPassword = encodePassword(token.getPassword());
  }

  public SaslClientDigestCallbackHandler(String userName, char[] userPassword) {
    checkNotNull(userName);
    checkNotNull(userPassword);
    this.userName = userName;
    this.userPassword = userPassword;
  }

  @Override
  public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
    NameCallback nc = null;
    PasswordCallback pc = null;
    RealmCallback rc = null;
    for (Callback callback : callbacks) {
      if (callback instanceof RealmChoiceCallback) {
        continue;
      } else if (callback instanceof NameCallback) {
        nc = (NameCallback) callback;
      } else if (callback instanceof PasswordCallback) {
        pc = (PasswordCallback) callback;
      } else if (callback instanceof RealmCallback) {
        rc = (RealmCallback) callback;
      } else {
        throw new UnsupportedCallbackException(callback, "Unrecognized SASL client callback");
      }
    }
    if (nc != null) {
      log.debug("SASL client callback: setting username: {}", userName);
      nc.setName(userName);
    }
    if (pc != null) {
      log.debug("SASL client callback: setting userPassword");
      pc.setPassword(userPassword);
    }
    if (rc != null) {
      log.debug("SASL client callback: setting realm: {}", rc.getDefaultText());
      rc.setText(rc.getDefaultText());
    }
  }

  @Override
  public String toString() {
    return NAME;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hcb = new HashCodeBuilder(41, 47);
    hcb.append(userName).append(userPassword);
    return hcb.toHashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (null == o) {
      return false;
    }
    if (o instanceof SaslClientDigestCallbackHandler) {
      SaslClientDigestCallbackHandler other = (SaslClientDigestCallbackHandler) o;
      return userName.equals(other.userName) && Arrays.equals(userPassword, other.userPassword);
    }
    return false;
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/rpc/SaslConnectionParams.java b/core/src/main/java/org/apache/accumulo/core/rpc/SaslConnectionParams.java
index e067e23cb..10438dec8 100644
-- a/core/src/main/java/org/apache/accumulo/core/rpc/SaslConnectionParams.java
++ b/core/src/main/java/org/apache/accumulo/core/rpc/SaslConnectionParams.java
@@ -16,6 +16,8 @@
  */
 package org.apache.accumulo.core.rpc;
 
import static com.google.common.base.Preconditions.checkNotNull;

 import java.io.IOException;
 import java.util.Collections;
 import java.util.HashMap;
@@ -23,10 +25,14 @@ import java.util.HashSet;
 import java.util.Map;
 import java.util.Map.Entry;
 
import javax.security.auth.callback.CallbackHandler;
 import javax.security.sasl.Sasl;
 
 import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.commons.configuration.MapConfiguration;
@@ -79,6 +85,34 @@ public class SaslConnectionParams {
     }
   }
 
  /**
   * The SASL mechanism to use for authentication
   */
  public enum SaslMechanism {
    GSSAPI("GSSAPI"), // Kerberos
    DIGEST_MD5("DIGEST-MD5"); // Delegation Tokens

    private final String mechanismName;

    private SaslMechanism(String mechanismName) {
      this.mechanismName = mechanismName;
    }

    public String getMechanismName() {
      return mechanismName;
    }

    public static SaslMechanism get(String mechanismName) {
      if (GSSAPI.mechanismName.equals(mechanismName)) {
        return GSSAPI;
      } else if (DIGEST_MD5.mechanismName.equals(mechanismName)) {
        return DIGEST_MD5;
      }

      throw new IllegalArgumentException("No value for " + mechanismName);
    }
  }

   private static String defaultRealm;
 
   static {
@@ -90,25 +124,47 @@ public class SaslConnectionParams {
     }
   }
 
  private String principal;
  private QualityOfProtection qop;
  private String kerberosServerPrimary;
  private final Map<String,String> saslProperties;

  private SaslConnectionParams() {
    saslProperties = new HashMap<>();
  }
  protected String principal;
  protected QualityOfProtection qop;
  protected String kerberosServerPrimary;
  protected SaslMechanism mechanism;
  protected CallbackHandler callbackHandler;
  protected final Map<String,String> saslProperties;
 
   /**
    * Generate an {@link SaslConnectionParams} instance given the provided {@link AccumuloConfiguration}. The provided configuration is converted into a
    * {@link ClientConfiguration}, ignoring any properties which are not {@link ClientProperty}s. If SASL is not being used, a null object will be returned.
    * Callers should strive to use {@link #forConfig(ClientConfiguration)}; server processes are the only intended consumers of this method.
    *
   * @param conf
   *          The configuration for clients to communicate with Accumulo
   * @return An {@link SaslConnectionParams} instance or null if SASL is not enabled
    */
  public static SaslConnectionParams forConfig(AccumuloConfiguration conf) {
  public SaslConnectionParams(AccumuloConfiguration conf, AuthenticationToken token) {
    this(new ClientConfiguration(new MapConfiguration(getProperties(conf))), token);
  }

  public SaslConnectionParams(ClientConfiguration conf, AuthenticationToken token) {
    checkNotNull(conf, "Configuration was null");
    checkNotNull(token, "AuthenticationToken was null");

    saslProperties = new HashMap<>();
    updatePrincipalFromUgi();
    updateFromConfiguration(conf);
    updateFromToken(token);
  }

  protected void updateFromToken(AuthenticationToken token) {
    if (token instanceof KerberosToken) {
      mechanism = SaslMechanism.GSSAPI;
      // No callbackhandlers necessary for GSSAPI
      callbackHandler = null;
    } else if (token instanceof DelegationToken) {
      mechanism = SaslMechanism.DIGEST_MD5;
      callbackHandler = new SaslClientDigestCallbackHandler((DelegationToken) token);
    } else {
      throw new IllegalArgumentException("Cannot determine SASL mechanism for token class: " + token.getClass());
    }
  }

  protected static Map<String,String> getProperties(AccumuloConfiguration conf) {
     final Map<String,String> clientProperties = new HashMap<>();
 
     // Servers will only have the full principal in their configuration -- parse the
@@ -136,25 +192,10 @@ public class SaslConnectionParams {
       }
     }
 
    ClientConfiguration clientConf = new ClientConfiguration(new MapConfiguration(clientProperties));
    return forConfig(clientConf);
    return clientProperties;
   }
 
  /**
   * Generate an {@link SaslConnectionParams} instance given the provided {@link ClientConfiguration}. If SASL is not being used, a null object will be
   * returned.
   *
   * @param conf
   *          The configuration for clients to communicate with Accumulo
   * @return An {@link SaslConnectionParams} instance or null if SASL is not enabled
   */
  public static SaslConnectionParams forConfig(ClientConfiguration conf) {
    if (!Boolean.parseBoolean(conf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED))) {
      return null;
    }

    SaslConnectionParams params = new SaslConnectionParams();

  protected void updatePrincipalFromUgi() {
     // Ensure we're using Kerberos auth for Hadoop UGI
     if (!UserGroupInformation.isSecurityEnabled()) {
       throw new RuntimeException("Cannot use SASL if Hadoop security is not enabled");
@@ -169,22 +210,23 @@ public class SaslConnectionParams {
     }
 
     // The full name is our principal
    params.principal = currentUser.getUserName();
    if (null == params.principal) {
    this.principal = currentUser.getUserName();
    if (null == this.principal) {
       throw new RuntimeException("Got null username from " + currentUser);
     }
 
  }

  protected void updateFromConfiguration(ClientConfiguration conf) {
     // Get the quality of protection to use
     final String qopValue = conf.get(ClientProperty.RPC_SASL_QOP);
    params.qop = QualityOfProtection.get(qopValue);
    this.qop = QualityOfProtection.get(qopValue);
 
     // Add in the SASL properties to a map so we don't have to repeatedly construct this map
    params.saslProperties.put(Sasl.QOP, params.qop.getQuality());
    this.saslProperties.put(Sasl.QOP, this.qop.getQuality());
 
     // The primary from the KRB principal on each server (e.g. primary/instance@realm)
    params.kerberosServerPrimary = conf.get(ClientProperty.KERBEROS_SERVER_PRIMARY);

    return params;
    this.kerberosServerPrimary = conf.get(ClientProperty.KERBEROS_SERVER_PRIMARY);
   }
 
   public Map<String,String> getSaslProperties() {
@@ -211,10 +253,24 @@ public class SaslConnectionParams {
     return principal;
   }
 
  /**
   * The SASL mechanism to use for authentication
   */
  public SaslMechanism getMechanism() {
    return mechanism;
  }

  /**
   * The SASL callback handler for this mechanism, may be null.
   */
  public CallbackHandler getCallbackHandler() {
    return callbackHandler;
  }

   @Override
   public int hashCode() {
     HashCodeBuilder hcb = new HashCodeBuilder(23,29);
    hcb.append(kerberosServerPrimary).append(saslProperties).append(qop.hashCode()).append(principal);
    hcb.append(kerberosServerPrimary).append(saslProperties).append(qop.hashCode()).append(principal).append(mechanism).append(callbackHandler);
     return hcb.toHashCode();
   }
 
@@ -231,6 +287,16 @@ public class SaslConnectionParams {
       if (!principal.equals(other.principal)) {
         return false;
       }
      if (!mechanism.equals(other.mechanism)) {
        return false;
      }
      if (null == callbackHandler) {
        if (null != other.callbackHandler) {
          return false;
        }
      } else if (!callbackHandler.equals(other.callbackHandler)) {
        return false;
      }
 
       return saslProperties.equals(other.saslProperties);
     }
@@ -238,6 +304,14 @@ public class SaslConnectionParams {
     return false;
   }
 
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    sb.append("SaslConnectionParams[").append("kerberosServerPrimary=").append(kerberosServerPrimary).append(", qualityOfProtection=").append(qop);
    sb.append(", principal=").append(principal).append(", mechanism=").append(mechanism).append(", callbackHandler=").append(callbackHandler).append("]");
    return sb.toString();
  }

   public static String getDefaultRealm() {
     return defaultRealm;
   }
diff --git a/core/src/main/java/org/apache/accumulo/core/rpc/SaslDigestCallbackHandler.java b/core/src/main/java/org/apache/accumulo/core/rpc/SaslDigestCallbackHandler.java
new file mode 100644
index 000000000..901bec132
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/rpc/SaslDigestCallbackHandler.java
@@ -0,0 +1,77 @@
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
package org.apache.accumulo.core.rpc;

import javax.security.auth.callback.CallbackHandler;

import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.security.token.SecretManager;
import org.apache.hadoop.security.token.SecretManager.InvalidToken;
import org.apache.hadoop.security.token.TokenIdentifier;

/**
 * Common serialization methods across the client and server callback handlers for SASL. Serialization and deserialization methods must be kept in sync.
 */
public abstract class SaslDigestCallbackHandler implements CallbackHandler {

  /**
   * Encode the serialized {@link TokenIdentifier} into a {@link String}.
   *
   * @param identifier
   *          The serialized identifier
   * @see #decodeIdentifier(String)
   */
  public String encodeIdentifier(byte[] identifier) {
    return new String(Base64.encodeBase64(identifier));
  }

  /**
   * Encode the token password into a character array.
   *
   * @param password
   *          The token password
   * @see #getPassword(SecretManager, TokenIdentifier)
   */
  public char[] encodePassword(byte[] password) {
    return new String(Base64.encodeBase64(password)).toCharArray();
  }

  /**
   * Generate the password from the provided {@link SecretManager} and {@link TokenIdentifier}.
   *
   * @param secretManager
   *          The server SecretManager
   * @param tokenid
   *          The TokenIdentifier from the client
   * @see #encodePassword(byte[])
   */
  public <T extends TokenIdentifier> char[] getPassword(SecretManager<T> secretManager, T tokenid) throws InvalidToken {
    return encodePassword(secretManager.retrievePassword(tokenid));
  }

  /**
   * Decode the encoded {@link TokenIdentifier} into bytes suitable to reconstitute the identifier.
   *
   * @param identifier
   *          The encoded, serialized {@link TokenIdentifier}
   * @see #encodeIdentifier(byte[])
   */
  public byte[] decodeIdentifier(String identifier) {
    return Base64.decodeBase64(identifier.getBytes());
  }

}
diff --git a/core/src/main/java/org/apache/accumulo/core/rpc/ThriftUtil.java b/core/src/main/java/org/apache/accumulo/core/rpc/ThriftUtil.java
index d880fb3fb..51dd5ba8b 100644
-- a/core/src/main/java/org/apache/accumulo/core/rpc/ThriftUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/rpc/ThriftUtil.java
@@ -36,6 +36,7 @@ import org.apache.accumulo.core.client.impl.ClientExec;
 import org.apache.accumulo.core.client.impl.ClientExecReturn;
 import org.apache.accumulo.core.client.impl.ThriftTransportPool;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.rpc.SaslConnectionParams.SaslMechanism;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.hadoop.security.UserGroupInformation;
@@ -65,7 +66,7 @@ public class ThriftUtil {
   private static final TFramedTransport.Factory transportFactory = new TFramedTransport.Factory(Integer.MAX_VALUE);
   private static final Map<Integer,TTransportFactory> factoryCache = new HashMap<Integer,TTransportFactory>();
 
  public static final String GSSAPI = "GSSAPI";
  public static final String GSSAPI = "GSSAPI", DIGEST_MD5 = "DIGEST-MD5";
 
   /**
    * An instance of {@link TraceProtocolFactory}
@@ -252,7 +253,7 @@ public class ThriftUtil {
    *          RPC options
    */
   public static TTransport createTransport(HostAndPort address, ClientContext context) throws TException {
    return createClientTransport(address, (int) context.getClientTimeoutInMillis(), context.getClientSslParams(), context.getClientSaslParams());
    return createClientTransport(address, (int) context.getClientTimeoutInMillis(), context.getClientSslParams(), context.getSaslParams());
   }
 
   /**
@@ -345,11 +346,14 @@ public class ThriftUtil {
           // Is this pricey enough that we want to cache it?
           final String hostname = InetAddress.getByName(address.getHostText()).getCanonicalHostName();
 
          log.trace("Opening transport to server as {} to {}/{}", currentUser, saslParams.getKerberosServerPrimary(), hostname);
          final SaslMechanism mechanism = saslParams.getMechanism();

          log.trace("Opening transport to server as {} to {}/{} using {}", currentUser, saslParams.getKerberosServerPrimary(), hostname, mechanism);
 
           // Create the client SASL transport using the information for the server
           // Despite the 'protocol' argument seeming to be useless, it *must* be the primary of the server being connected to
          transport = new TSaslClientTransport(GSSAPI, null, saslParams.getKerberosServerPrimary(), hostname, saslParams.getSaslProperties(), null, transport);
          transport = new TSaslClientTransport(mechanism.getMechanismName(), null, saslParams.getKerberosServerPrimary(), hostname,
              saslParams.getSaslProperties(), saslParams.getCallbackHandler(), transport);
 
           // Wrap it all in a processor which will run with a doAs the current user
           transport = new UGIAssumingTransport(transport, currentUser);
diff --git a/core/src/main/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifier.java b/core/src/main/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifier.java
new file mode 100644
index 000000000..0b671d871
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifier.java
@@ -0,0 +1,210 @@
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
package org.apache.accumulo.core.security;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.security.thrift.TAuthenticationTokenIdentifier;
import org.apache.accumulo.core.util.ThriftMessageUtil;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;

/**
 * Implementation that identifies the underlying {@link Token} for Accumulo.
 */
public class AuthenticationTokenIdentifier extends TokenIdentifier {
  public static final Text TOKEN_KIND = new Text("ACCUMULO_AUTH_TOKEN");

  private TAuthenticationTokenIdentifier impl = null;
  private DelegationTokenConfig cfg = null;

  public AuthenticationTokenIdentifier() {
    // noop for Writable
  }

  public AuthenticationTokenIdentifier(String principal) {
    this(principal, null);
  }

  public AuthenticationTokenIdentifier(String principal, DelegationTokenConfig cfg) {
    checkNotNull(principal);
    impl = new TAuthenticationTokenIdentifier(principal);
    this.cfg = cfg;
  }

  public AuthenticationTokenIdentifier(String principal, int keyId, long issueDate, long expirationDate, String instanceId) {
    checkNotNull(principal);
    impl = new TAuthenticationTokenIdentifier(principal);
    impl.setKeyId(keyId);
    impl.setIssueDate(issueDate);
    impl.setExpirationDate(expirationDate);
    impl.setInstanceId(instanceId);
  }

  public AuthenticationTokenIdentifier(AuthenticationTokenIdentifier identifier) {
    checkNotNull(identifier);
    impl = new TAuthenticationTokenIdentifier(identifier.getThriftIdentifier());
  }

  public AuthenticationTokenIdentifier(TAuthenticationTokenIdentifier identifier) {
    checkNotNull(identifier);
    impl = new TAuthenticationTokenIdentifier(identifier);
  }

  public void setKeyId(int keyId) {
    impl.setKeyId(keyId);
  }

  public int getKeyId() {
    checkNotNull(impl, "Identifier not initialized");
    return impl.getKeyId();
  }

  public void setIssueDate(long issueDate) {
    checkNotNull(impl, "Identifier not initialized");
    impl.setIssueDate(issueDate);
  }

  public long getIssueDate() {
    checkNotNull(impl, "Identifier not initialized");
    return impl.getIssueDate();
  }

  public void setExpirationDate(long expirationDate) {
    checkNotNull(impl, "Identifier not initialized");
    impl.setExpirationDate(expirationDate);
  }

  public long getExpirationDate() {
    checkNotNull(impl, "Identifier not initialized");
    return impl.getExpirationDate();
  }

  public void setInstanceId(String instanceId) {
    checkNotNull(impl, "Identifier not initialized");
    impl.setInstanceId(instanceId);
  }

  public String getInstanceId() {
    checkNotNull(impl, "Identifier not initialized");
    return impl.getInstanceId();
  }

  public TAuthenticationTokenIdentifier getThriftIdentifier() {
    checkNotNull(impl);
    return impl;
  }

  /**
   * A configuration from the requesting user, may be null.
   */
  public DelegationTokenConfig getConfig() {
    return cfg;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    if (null != impl) {
      ThriftMessageUtil msgUtil = new ThriftMessageUtil();
      ByteBuffer serialized = msgUtil.serialize(impl);
      out.writeInt(serialized.limit());
      out.write(serialized.array(), serialized.arrayOffset(), serialized.limit());
    } else {
      out.writeInt(0);
    }
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    int length = in.readInt();
    if (length > 0) {
      ThriftMessageUtil msgUtil = new ThriftMessageUtil();
      byte[] serialized = new byte[length];
      in.readFully(serialized);
      impl = new TAuthenticationTokenIdentifier();
      msgUtil.deserialize(serialized, impl);
    }
  }

  @Override
  public Text getKind() {
    return TOKEN_KIND;
  }

  @Override
  public UserGroupInformation getUser() {
    if (null != impl && impl.isSetPrincipal()) {
      return UserGroupInformation.createRemoteUser(impl.getPrincipal());
    }
    return null;
  }

  @Override
  public int hashCode() {
    if (null == impl) {
      return 0;
    }
    HashCodeBuilder hcb = new HashCodeBuilder(7, 11);
    if (impl.isSetPrincipal()) {
      hcb.append(impl.getPrincipal());
    }
    if (impl.isSetKeyId()) {
      hcb.append(impl.getKeyId());
    }
    if (impl.isSetIssueDate()) {
      hcb.append(impl.getIssueDate());
    }
    if (impl.isSetExpirationDate()) {
      hcb.append(impl.getExpirationDate());
    }
    if (impl.isSetInstanceId()) {
      hcb.append(impl.getInstanceId());
    }
    return hcb.toHashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(128);
    sb.append("AuthenticationTokenIdentifier(").append(impl).append(")");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (null == o) {
      return false;
    }
    if (o instanceof AuthenticationTokenIdentifier) {
      AuthenticationTokenIdentifier other = (AuthenticationTokenIdentifier) o;
      if (null == impl) {
        return null == other.impl;
      }
      return impl.equals(other.impl);
    }
    return false;
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/security/SystemPermission.java b/core/src/main/java/org/apache/accumulo/core/security/SystemPermission.java
index b998179e7..a1df5dc40 100644
-- a/core/src/main/java/org/apache/accumulo/core/security/SystemPermission.java
++ b/core/src/main/java/org/apache/accumulo/core/security/SystemPermission.java
@@ -37,7 +37,8 @@ public enum SystemPermission {
   SYSTEM((byte) 7),
   CREATE_NAMESPACE((byte) 8),
   DROP_NAMESPACE((byte) 9),
  ALTER_NAMESPACE((byte) 10);
  ALTER_NAMESPACE((byte) 10),
  OBTAIN_DELEGATION_TOKEN((byte) 11);
 
   private byte permID;
 
diff --git a/core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationKey.java b/core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationKey.java
new file mode 100644
index 000000000..4da2bb2dc
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationKey.java
@@ -0,0 +1,705 @@
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
/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.core.security.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "serial", "rawtypes", "unused"}) public class TAuthenticationKey implements org.apache.thrift.TBase<TAuthenticationKey, TAuthenticationKey._Fields>, java.io.Serializable, Cloneable, Comparable<TAuthenticationKey> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TAuthenticationKey");

  private static final org.apache.thrift.protocol.TField SECRET_FIELD_DESC = new org.apache.thrift.protocol.TField("secret", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField KEY_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("keyId", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField EXPIRATION_DATE_FIELD_DESC = new org.apache.thrift.protocol.TField("expirationDate", org.apache.thrift.protocol.TType.I64, (short)3);
  private static final org.apache.thrift.protocol.TField CREATION_DATE_FIELD_DESC = new org.apache.thrift.protocol.TField("creationDate", org.apache.thrift.protocol.TType.I64, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TAuthenticationKeyStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TAuthenticationKeyTupleSchemeFactory());
  }

  public ByteBuffer secret; // required
  public int keyId; // optional
  public long expirationDate; // optional
  public long creationDate; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    SECRET((short)1, "secret"),
    KEY_ID((short)2, "keyId"),
    EXPIRATION_DATE((short)3, "expirationDate"),
    CREATION_DATE((short)4, "creationDate");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // SECRET
          return SECRET;
        case 2: // KEY_ID
          return KEY_ID;
        case 3: // EXPIRATION_DATE
          return EXPIRATION_DATE;
        case 4: // CREATION_DATE
          return CREATION_DATE;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __KEYID_ISSET_ID = 0;
  private static final int __EXPIRATIONDATE_ISSET_ID = 1;
  private static final int __CREATIONDATE_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.KEY_ID,_Fields.EXPIRATION_DATE,_Fields.CREATION_DATE};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.SECRET, new org.apache.thrift.meta_data.FieldMetaData("secret", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.KEY_ID, new org.apache.thrift.meta_data.FieldMetaData("keyId", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.EXPIRATION_DATE, new org.apache.thrift.meta_data.FieldMetaData("expirationDate", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.CREATION_DATE, new org.apache.thrift.meta_data.FieldMetaData("creationDate", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TAuthenticationKey.class, metaDataMap);
  }

  public TAuthenticationKey() {
  }

  public TAuthenticationKey(
    ByteBuffer secret)
  {
    this();
    this.secret = secret;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TAuthenticationKey(TAuthenticationKey other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetSecret()) {
      this.secret = org.apache.thrift.TBaseHelper.copyBinary(other.secret);
;
    }
    this.keyId = other.keyId;
    this.expirationDate = other.expirationDate;
    this.creationDate = other.creationDate;
  }

  public TAuthenticationKey deepCopy() {
    return new TAuthenticationKey(this);
  }

  @Override
  public void clear() {
    this.secret = null;
    setKeyIdIsSet(false);
    this.keyId = 0;
    setExpirationDateIsSet(false);
    this.expirationDate = 0;
    setCreationDateIsSet(false);
    this.creationDate = 0;
  }

  public byte[] getSecret() {
    setSecret(org.apache.thrift.TBaseHelper.rightSize(secret));
    return secret == null ? null : secret.array();
  }

  public ByteBuffer bufferForSecret() {
    return secret;
  }

  public TAuthenticationKey setSecret(byte[] secret) {
    setSecret(secret == null ? (ByteBuffer)null : ByteBuffer.wrap(secret));
    return this;
  }

  public TAuthenticationKey setSecret(ByteBuffer secret) {
    this.secret = secret;
    return this;
  }

  public void unsetSecret() {
    this.secret = null;
  }

  /** Returns true if field secret is set (has been assigned a value) and false otherwise */
  public boolean isSetSecret() {
    return this.secret != null;
  }

  public void setSecretIsSet(boolean value) {
    if (!value) {
      this.secret = null;
    }
  }

  public int getKeyId() {
    return this.keyId;
  }

  public TAuthenticationKey setKeyId(int keyId) {
    this.keyId = keyId;
    setKeyIdIsSet(true);
    return this;
  }

  public void unsetKeyId() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __KEYID_ISSET_ID);
  }

  /** Returns true if field keyId is set (has been assigned a value) and false otherwise */
  public boolean isSetKeyId() {
    return EncodingUtils.testBit(__isset_bitfield, __KEYID_ISSET_ID);
  }

  public void setKeyIdIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __KEYID_ISSET_ID, value);
  }

  public long getExpirationDate() {
    return this.expirationDate;
  }

  public TAuthenticationKey setExpirationDate(long expirationDate) {
    this.expirationDate = expirationDate;
    setExpirationDateIsSet(true);
    return this;
  }

  public void unsetExpirationDate() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __EXPIRATIONDATE_ISSET_ID);
  }

  /** Returns true if field expirationDate is set (has been assigned a value) and false otherwise */
  public boolean isSetExpirationDate() {
    return EncodingUtils.testBit(__isset_bitfield, __EXPIRATIONDATE_ISSET_ID);
  }

  public void setExpirationDateIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __EXPIRATIONDATE_ISSET_ID, value);
  }

  public long getCreationDate() {
    return this.creationDate;
  }

  public TAuthenticationKey setCreationDate(long creationDate) {
    this.creationDate = creationDate;
    setCreationDateIsSet(true);
    return this;
  }

  public void unsetCreationDate() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __CREATIONDATE_ISSET_ID);
  }

  /** Returns true if field creationDate is set (has been assigned a value) and false otherwise */
  public boolean isSetCreationDate() {
    return EncodingUtils.testBit(__isset_bitfield, __CREATIONDATE_ISSET_ID);
  }

  public void setCreationDateIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __CREATIONDATE_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case SECRET:
      if (value == null) {
        unsetSecret();
      } else {
        setSecret((ByteBuffer)value);
      }
      break;

    case KEY_ID:
      if (value == null) {
        unsetKeyId();
      } else {
        setKeyId((Integer)value);
      }
      break;

    case EXPIRATION_DATE:
      if (value == null) {
        unsetExpirationDate();
      } else {
        setExpirationDate((Long)value);
      }
      break;

    case CREATION_DATE:
      if (value == null) {
        unsetCreationDate();
      } else {
        setCreationDate((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case SECRET:
      return getSecret();

    case KEY_ID:
      return Integer.valueOf(getKeyId());

    case EXPIRATION_DATE:
      return Long.valueOf(getExpirationDate());

    case CREATION_DATE:
      return Long.valueOf(getCreationDate());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case SECRET:
      return isSetSecret();
    case KEY_ID:
      return isSetKeyId();
    case EXPIRATION_DATE:
      return isSetExpirationDate();
    case CREATION_DATE:
      return isSetCreationDate();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TAuthenticationKey)
      return this.equals((TAuthenticationKey)that);
    return false;
  }

  public boolean equals(TAuthenticationKey that) {
    if (that == null)
      return false;

    boolean this_present_secret = true && this.isSetSecret();
    boolean that_present_secret = true && that.isSetSecret();
    if (this_present_secret || that_present_secret) {
      if (!(this_present_secret && that_present_secret))
        return false;
      if (!this.secret.equals(that.secret))
        return false;
    }

    boolean this_present_keyId = true && this.isSetKeyId();
    boolean that_present_keyId = true && that.isSetKeyId();
    if (this_present_keyId || that_present_keyId) {
      if (!(this_present_keyId && that_present_keyId))
        return false;
      if (this.keyId != that.keyId)
        return false;
    }

    boolean this_present_expirationDate = true && this.isSetExpirationDate();
    boolean that_present_expirationDate = true && that.isSetExpirationDate();
    if (this_present_expirationDate || that_present_expirationDate) {
      if (!(this_present_expirationDate && that_present_expirationDate))
        return false;
      if (this.expirationDate != that.expirationDate)
        return false;
    }

    boolean this_present_creationDate = true && this.isSetCreationDate();
    boolean that_present_creationDate = true && that.isSetCreationDate();
    if (this_present_creationDate || that_present_creationDate) {
      if (!(this_present_creationDate && that_present_creationDate))
        return false;
      if (this.creationDate != that.creationDate)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(TAuthenticationKey other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetSecret()).compareTo(other.isSetSecret());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSecret()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.secret, other.secret);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetKeyId()).compareTo(other.isSetKeyId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKeyId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.keyId, other.keyId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetExpirationDate()).compareTo(other.isSetExpirationDate());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetExpirationDate()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.expirationDate, other.expirationDate);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCreationDate()).compareTo(other.isSetCreationDate());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCreationDate()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.creationDate, other.creationDate);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TAuthenticationKey(");
    boolean first = true;

    sb.append("secret:");
    if (this.secret == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.secret, sb);
    }
    first = false;
    if (isSetKeyId()) {
      if (!first) sb.append(", ");
      sb.append("keyId:");
      sb.append(this.keyId);
      first = false;
    }
    if (isSetExpirationDate()) {
      if (!first) sb.append(", ");
      sb.append("expirationDate:");
      sb.append(this.expirationDate);
      first = false;
    }
    if (isSetCreationDate()) {
      if (!first) sb.append(", ");
      sb.append("creationDate:");
      sb.append(this.creationDate);
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TAuthenticationKeyStandardSchemeFactory implements SchemeFactory {
    public TAuthenticationKeyStandardScheme getScheme() {
      return new TAuthenticationKeyStandardScheme();
    }
  }

  private static class TAuthenticationKeyStandardScheme extends StandardScheme<TAuthenticationKey> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TAuthenticationKey struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // SECRET
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.secret = iprot.readBinary();
              struct.setSecretIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // KEY_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.keyId = iprot.readI32();
              struct.setKeyIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // EXPIRATION_DATE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.expirationDate = iprot.readI64();
              struct.setExpirationDateIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // CREATION_DATE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.creationDate = iprot.readI64();
              struct.setCreationDateIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TAuthenticationKey struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.secret != null) {
        oprot.writeFieldBegin(SECRET_FIELD_DESC);
        oprot.writeBinary(struct.secret);
        oprot.writeFieldEnd();
      }
      if (struct.isSetKeyId()) {
        oprot.writeFieldBegin(KEY_ID_FIELD_DESC);
        oprot.writeI32(struct.keyId);
        oprot.writeFieldEnd();
      }
      if (struct.isSetExpirationDate()) {
        oprot.writeFieldBegin(EXPIRATION_DATE_FIELD_DESC);
        oprot.writeI64(struct.expirationDate);
        oprot.writeFieldEnd();
      }
      if (struct.isSetCreationDate()) {
        oprot.writeFieldBegin(CREATION_DATE_FIELD_DESC);
        oprot.writeI64(struct.creationDate);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TAuthenticationKeyTupleSchemeFactory implements SchemeFactory {
    public TAuthenticationKeyTupleScheme getScheme() {
      return new TAuthenticationKeyTupleScheme();
    }
  }

  private static class TAuthenticationKeyTupleScheme extends TupleScheme<TAuthenticationKey> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TAuthenticationKey struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetSecret()) {
        optionals.set(0);
      }
      if (struct.isSetKeyId()) {
        optionals.set(1);
      }
      if (struct.isSetExpirationDate()) {
        optionals.set(2);
      }
      if (struct.isSetCreationDate()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetSecret()) {
        oprot.writeBinary(struct.secret);
      }
      if (struct.isSetKeyId()) {
        oprot.writeI32(struct.keyId);
      }
      if (struct.isSetExpirationDate()) {
        oprot.writeI64(struct.expirationDate);
      }
      if (struct.isSetCreationDate()) {
        oprot.writeI64(struct.creationDate);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TAuthenticationKey struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.secret = iprot.readBinary();
        struct.setSecretIsSet(true);
      }
      if (incoming.get(1)) {
        struct.keyId = iprot.readI32();
        struct.setKeyIdIsSet(true);
      }
      if (incoming.get(2)) {
        struct.expirationDate = iprot.readI64();
        struct.setExpirationDateIsSet(true);
      }
      if (incoming.get(3)) {
        struct.creationDate = iprot.readI64();
        struct.setCreationDateIsSet(true);
      }
    }
  }

}

diff --git a/core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationTokenIdentifier.java b/core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationTokenIdentifier.java
new file mode 100644
index 000000000..d4e75f097
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/security/thrift/TAuthenticationTokenIdentifier.java
@@ -0,0 +1,796 @@
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
/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.core.security.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "serial", "rawtypes", "unused"}) public class TAuthenticationTokenIdentifier implements org.apache.thrift.TBase<TAuthenticationTokenIdentifier, TAuthenticationTokenIdentifier._Fields>, java.io.Serializable, Cloneable, Comparable<TAuthenticationTokenIdentifier> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TAuthenticationTokenIdentifier");

  private static final org.apache.thrift.protocol.TField PRINCIPAL_FIELD_DESC = new org.apache.thrift.protocol.TField("principal", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField KEY_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("keyId", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField ISSUE_DATE_FIELD_DESC = new org.apache.thrift.protocol.TField("issueDate", org.apache.thrift.protocol.TType.I64, (short)3);
  private static final org.apache.thrift.protocol.TField EXPIRATION_DATE_FIELD_DESC = new org.apache.thrift.protocol.TField("expirationDate", org.apache.thrift.protocol.TType.I64, (short)4);
  private static final org.apache.thrift.protocol.TField INSTANCE_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("instanceId", org.apache.thrift.protocol.TType.STRING, (short)5);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TAuthenticationTokenIdentifierStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TAuthenticationTokenIdentifierTupleSchemeFactory());
  }

  public String principal; // required
  public int keyId; // optional
  public long issueDate; // optional
  public long expirationDate; // optional
  public String instanceId; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PRINCIPAL((short)1, "principal"),
    KEY_ID((short)2, "keyId"),
    ISSUE_DATE((short)3, "issueDate"),
    EXPIRATION_DATE((short)4, "expirationDate"),
    INSTANCE_ID((short)5, "instanceId");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // PRINCIPAL
          return PRINCIPAL;
        case 2: // KEY_ID
          return KEY_ID;
        case 3: // ISSUE_DATE
          return ISSUE_DATE;
        case 4: // EXPIRATION_DATE
          return EXPIRATION_DATE;
        case 5: // INSTANCE_ID
          return INSTANCE_ID;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __KEYID_ISSET_ID = 0;
  private static final int __ISSUEDATE_ISSET_ID = 1;
  private static final int __EXPIRATIONDATE_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.KEY_ID,_Fields.ISSUE_DATE,_Fields.EXPIRATION_DATE,_Fields.INSTANCE_ID};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PRINCIPAL, new org.apache.thrift.meta_data.FieldMetaData("principal", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.KEY_ID, new org.apache.thrift.meta_data.FieldMetaData("keyId", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.ISSUE_DATE, new org.apache.thrift.meta_data.FieldMetaData("issueDate", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.EXPIRATION_DATE, new org.apache.thrift.meta_data.FieldMetaData("expirationDate", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.INSTANCE_ID, new org.apache.thrift.meta_data.FieldMetaData("instanceId", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TAuthenticationTokenIdentifier.class, metaDataMap);
  }

  public TAuthenticationTokenIdentifier() {
  }

  public TAuthenticationTokenIdentifier(
    String principal)
  {
    this();
    this.principal = principal;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TAuthenticationTokenIdentifier(TAuthenticationTokenIdentifier other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetPrincipal()) {
      this.principal = other.principal;
    }
    this.keyId = other.keyId;
    this.issueDate = other.issueDate;
    this.expirationDate = other.expirationDate;
    if (other.isSetInstanceId()) {
      this.instanceId = other.instanceId;
    }
  }

  public TAuthenticationTokenIdentifier deepCopy() {
    return new TAuthenticationTokenIdentifier(this);
  }

  @Override
  public void clear() {
    this.principal = null;
    setKeyIdIsSet(false);
    this.keyId = 0;
    setIssueDateIsSet(false);
    this.issueDate = 0;
    setExpirationDateIsSet(false);
    this.expirationDate = 0;
    this.instanceId = null;
  }

  public String getPrincipal() {
    return this.principal;
  }

  public TAuthenticationTokenIdentifier setPrincipal(String principal) {
    this.principal = principal;
    return this;
  }

  public void unsetPrincipal() {
    this.principal = null;
  }

  /** Returns true if field principal is set (has been assigned a value) and false otherwise */
  public boolean isSetPrincipal() {
    return this.principal != null;
  }

  public void setPrincipalIsSet(boolean value) {
    if (!value) {
      this.principal = null;
    }
  }

  public int getKeyId() {
    return this.keyId;
  }

  public TAuthenticationTokenIdentifier setKeyId(int keyId) {
    this.keyId = keyId;
    setKeyIdIsSet(true);
    return this;
  }

  public void unsetKeyId() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __KEYID_ISSET_ID);
  }

  /** Returns true if field keyId is set (has been assigned a value) and false otherwise */
  public boolean isSetKeyId() {
    return EncodingUtils.testBit(__isset_bitfield, __KEYID_ISSET_ID);
  }

  public void setKeyIdIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __KEYID_ISSET_ID, value);
  }

  public long getIssueDate() {
    return this.issueDate;
  }

  public TAuthenticationTokenIdentifier setIssueDate(long issueDate) {
    this.issueDate = issueDate;
    setIssueDateIsSet(true);
    return this;
  }

  public void unsetIssueDate() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ISSUEDATE_ISSET_ID);
  }

  /** Returns true if field issueDate is set (has been assigned a value) and false otherwise */
  public boolean isSetIssueDate() {
    return EncodingUtils.testBit(__isset_bitfield, __ISSUEDATE_ISSET_ID);
  }

  public void setIssueDateIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ISSUEDATE_ISSET_ID, value);
  }

  public long getExpirationDate() {
    return this.expirationDate;
  }

  public TAuthenticationTokenIdentifier setExpirationDate(long expirationDate) {
    this.expirationDate = expirationDate;
    setExpirationDateIsSet(true);
    return this;
  }

  public void unsetExpirationDate() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __EXPIRATIONDATE_ISSET_ID);
  }

  /** Returns true if field expirationDate is set (has been assigned a value) and false otherwise */
  public boolean isSetExpirationDate() {
    return EncodingUtils.testBit(__isset_bitfield, __EXPIRATIONDATE_ISSET_ID);
  }

  public void setExpirationDateIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __EXPIRATIONDATE_ISSET_ID, value);
  }

  public String getInstanceId() {
    return this.instanceId;
  }

  public TAuthenticationTokenIdentifier setInstanceId(String instanceId) {
    this.instanceId = instanceId;
    return this;
  }

  public void unsetInstanceId() {
    this.instanceId = null;
  }

  /** Returns true if field instanceId is set (has been assigned a value) and false otherwise */
  public boolean isSetInstanceId() {
    return this.instanceId != null;
  }

  public void setInstanceIdIsSet(boolean value) {
    if (!value) {
      this.instanceId = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case PRINCIPAL:
      if (value == null) {
        unsetPrincipal();
      } else {
        setPrincipal((String)value);
      }
      break;

    case KEY_ID:
      if (value == null) {
        unsetKeyId();
      } else {
        setKeyId((Integer)value);
      }
      break;

    case ISSUE_DATE:
      if (value == null) {
        unsetIssueDate();
      } else {
        setIssueDate((Long)value);
      }
      break;

    case EXPIRATION_DATE:
      if (value == null) {
        unsetExpirationDate();
      } else {
        setExpirationDate((Long)value);
      }
      break;

    case INSTANCE_ID:
      if (value == null) {
        unsetInstanceId();
      } else {
        setInstanceId((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case PRINCIPAL:
      return getPrincipal();

    case KEY_ID:
      return Integer.valueOf(getKeyId());

    case ISSUE_DATE:
      return Long.valueOf(getIssueDate());

    case EXPIRATION_DATE:
      return Long.valueOf(getExpirationDate());

    case INSTANCE_ID:
      return getInstanceId();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case PRINCIPAL:
      return isSetPrincipal();
    case KEY_ID:
      return isSetKeyId();
    case ISSUE_DATE:
      return isSetIssueDate();
    case EXPIRATION_DATE:
      return isSetExpirationDate();
    case INSTANCE_ID:
      return isSetInstanceId();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TAuthenticationTokenIdentifier)
      return this.equals((TAuthenticationTokenIdentifier)that);
    return false;
  }

  public boolean equals(TAuthenticationTokenIdentifier that) {
    if (that == null)
      return false;

    boolean this_present_principal = true && this.isSetPrincipal();
    boolean that_present_principal = true && that.isSetPrincipal();
    if (this_present_principal || that_present_principal) {
      if (!(this_present_principal && that_present_principal))
        return false;
      if (!this.principal.equals(that.principal))
        return false;
    }

    boolean this_present_keyId = true && this.isSetKeyId();
    boolean that_present_keyId = true && that.isSetKeyId();
    if (this_present_keyId || that_present_keyId) {
      if (!(this_present_keyId && that_present_keyId))
        return false;
      if (this.keyId != that.keyId)
        return false;
    }

    boolean this_present_issueDate = true && this.isSetIssueDate();
    boolean that_present_issueDate = true && that.isSetIssueDate();
    if (this_present_issueDate || that_present_issueDate) {
      if (!(this_present_issueDate && that_present_issueDate))
        return false;
      if (this.issueDate != that.issueDate)
        return false;
    }

    boolean this_present_expirationDate = true && this.isSetExpirationDate();
    boolean that_present_expirationDate = true && that.isSetExpirationDate();
    if (this_present_expirationDate || that_present_expirationDate) {
      if (!(this_present_expirationDate && that_present_expirationDate))
        return false;
      if (this.expirationDate != that.expirationDate)
        return false;
    }

    boolean this_present_instanceId = true && this.isSetInstanceId();
    boolean that_present_instanceId = true && that.isSetInstanceId();
    if (this_present_instanceId || that_present_instanceId) {
      if (!(this_present_instanceId && that_present_instanceId))
        return false;
      if (!this.instanceId.equals(that.instanceId))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(TAuthenticationTokenIdentifier other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetPrincipal()).compareTo(other.isSetPrincipal());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPrincipal()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.principal, other.principal);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetKeyId()).compareTo(other.isSetKeyId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKeyId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.keyId, other.keyId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIssueDate()).compareTo(other.isSetIssueDate());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIssueDate()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.issueDate, other.issueDate);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetExpirationDate()).compareTo(other.isSetExpirationDate());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetExpirationDate()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.expirationDate, other.expirationDate);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetInstanceId()).compareTo(other.isSetInstanceId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetInstanceId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.instanceId, other.instanceId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TAuthenticationTokenIdentifier(");
    boolean first = true;

    sb.append("principal:");
    if (this.principal == null) {
      sb.append("null");
    } else {
      sb.append(this.principal);
    }
    first = false;
    if (isSetKeyId()) {
      if (!first) sb.append(", ");
      sb.append("keyId:");
      sb.append(this.keyId);
      first = false;
    }
    if (isSetIssueDate()) {
      if (!first) sb.append(", ");
      sb.append("issueDate:");
      sb.append(this.issueDate);
      first = false;
    }
    if (isSetExpirationDate()) {
      if (!first) sb.append(", ");
      sb.append("expirationDate:");
      sb.append(this.expirationDate);
      first = false;
    }
    if (isSetInstanceId()) {
      if (!first) sb.append(", ");
      sb.append("instanceId:");
      if (this.instanceId == null) {
        sb.append("null");
      } else {
        sb.append(this.instanceId);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TAuthenticationTokenIdentifierStandardSchemeFactory implements SchemeFactory {
    public TAuthenticationTokenIdentifierStandardScheme getScheme() {
      return new TAuthenticationTokenIdentifierStandardScheme();
    }
  }

  private static class TAuthenticationTokenIdentifierStandardScheme extends StandardScheme<TAuthenticationTokenIdentifier> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TAuthenticationTokenIdentifier struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // PRINCIPAL
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.principal = iprot.readString();
              struct.setPrincipalIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // KEY_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.keyId = iprot.readI32();
              struct.setKeyIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // ISSUE_DATE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.issueDate = iprot.readI64();
              struct.setIssueDateIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // EXPIRATION_DATE
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.expirationDate = iprot.readI64();
              struct.setExpirationDateIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // INSTANCE_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.instanceId = iprot.readString();
              struct.setInstanceIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TAuthenticationTokenIdentifier struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.principal != null) {
        oprot.writeFieldBegin(PRINCIPAL_FIELD_DESC);
        oprot.writeString(struct.principal);
        oprot.writeFieldEnd();
      }
      if (struct.isSetKeyId()) {
        oprot.writeFieldBegin(KEY_ID_FIELD_DESC);
        oprot.writeI32(struct.keyId);
        oprot.writeFieldEnd();
      }
      if (struct.isSetIssueDate()) {
        oprot.writeFieldBegin(ISSUE_DATE_FIELD_DESC);
        oprot.writeI64(struct.issueDate);
        oprot.writeFieldEnd();
      }
      if (struct.isSetExpirationDate()) {
        oprot.writeFieldBegin(EXPIRATION_DATE_FIELD_DESC);
        oprot.writeI64(struct.expirationDate);
        oprot.writeFieldEnd();
      }
      if (struct.instanceId != null) {
        if (struct.isSetInstanceId()) {
          oprot.writeFieldBegin(INSTANCE_ID_FIELD_DESC);
          oprot.writeString(struct.instanceId);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TAuthenticationTokenIdentifierTupleSchemeFactory implements SchemeFactory {
    public TAuthenticationTokenIdentifierTupleScheme getScheme() {
      return new TAuthenticationTokenIdentifierTupleScheme();
    }
  }

  private static class TAuthenticationTokenIdentifierTupleScheme extends TupleScheme<TAuthenticationTokenIdentifier> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TAuthenticationTokenIdentifier struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetPrincipal()) {
        optionals.set(0);
      }
      if (struct.isSetKeyId()) {
        optionals.set(1);
      }
      if (struct.isSetIssueDate()) {
        optionals.set(2);
      }
      if (struct.isSetExpirationDate()) {
        optionals.set(3);
      }
      if (struct.isSetInstanceId()) {
        optionals.set(4);
      }
      oprot.writeBitSet(optionals, 5);
      if (struct.isSetPrincipal()) {
        oprot.writeString(struct.principal);
      }
      if (struct.isSetKeyId()) {
        oprot.writeI32(struct.keyId);
      }
      if (struct.isSetIssueDate()) {
        oprot.writeI64(struct.issueDate);
      }
      if (struct.isSetExpirationDate()) {
        oprot.writeI64(struct.expirationDate);
      }
      if (struct.isSetInstanceId()) {
        oprot.writeString(struct.instanceId);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TAuthenticationTokenIdentifier struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(5);
      if (incoming.get(0)) {
        struct.principal = iprot.readString();
        struct.setPrincipalIsSet(true);
      }
      if (incoming.get(1)) {
        struct.keyId = iprot.readI32();
        struct.setKeyIdIsSet(true);
      }
      if (incoming.get(2)) {
        struct.issueDate = iprot.readI64();
        struct.setIssueDateIsSet(true);
      }
      if (incoming.get(3)) {
        struct.expirationDate = iprot.readI64();
        struct.setExpirationDateIsSet(true);
      }
      if (incoming.get(4)) {
        struct.instanceId = iprot.readString();
        struct.setInstanceIdIsSet(true);
      }
    }
  }

}

diff --git a/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationToken.java b/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationToken.java
new file mode 100644
index 000000000..904d19567
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationToken.java
@@ -0,0 +1,520 @@
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
/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.core.security.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "serial", "rawtypes", "unused"}) public class TDelegationToken implements org.apache.thrift.TBase<TDelegationToken, TDelegationToken._Fields>, java.io.Serializable, Cloneable, Comparable<TDelegationToken> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TDelegationToken");

  private static final org.apache.thrift.protocol.TField PASSWORD_FIELD_DESC = new org.apache.thrift.protocol.TField("password", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField IDENTIFIER_FIELD_DESC = new org.apache.thrift.protocol.TField("identifier", org.apache.thrift.protocol.TType.STRUCT, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TDelegationTokenStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TDelegationTokenTupleSchemeFactory());
  }

  public ByteBuffer password; // required
  public TAuthenticationTokenIdentifier identifier; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PASSWORD((short)1, "password"),
    IDENTIFIER((short)2, "identifier");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // PASSWORD
          return PASSWORD;
        case 2: // IDENTIFIER
          return IDENTIFIER;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PASSWORD, new org.apache.thrift.meta_data.FieldMetaData("password", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.IDENTIFIER, new org.apache.thrift.meta_data.FieldMetaData("identifier", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TAuthenticationTokenIdentifier.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TDelegationToken.class, metaDataMap);
  }

  public TDelegationToken() {
  }

  public TDelegationToken(
    ByteBuffer password,
    TAuthenticationTokenIdentifier identifier)
  {
    this();
    this.password = password;
    this.identifier = identifier;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TDelegationToken(TDelegationToken other) {
    if (other.isSetPassword()) {
      this.password = org.apache.thrift.TBaseHelper.copyBinary(other.password);
;
    }
    if (other.isSetIdentifier()) {
      this.identifier = new TAuthenticationTokenIdentifier(other.identifier);
    }
  }

  public TDelegationToken deepCopy() {
    return new TDelegationToken(this);
  }

  @Override
  public void clear() {
    this.password = null;
    this.identifier = null;
  }

  public byte[] getPassword() {
    setPassword(org.apache.thrift.TBaseHelper.rightSize(password));
    return password == null ? null : password.array();
  }

  public ByteBuffer bufferForPassword() {
    return password;
  }

  public TDelegationToken setPassword(byte[] password) {
    setPassword(password == null ? (ByteBuffer)null : ByteBuffer.wrap(password));
    return this;
  }

  public TDelegationToken setPassword(ByteBuffer password) {
    this.password = password;
    return this;
  }

  public void unsetPassword() {
    this.password = null;
  }

  /** Returns true if field password is set (has been assigned a value) and false otherwise */
  public boolean isSetPassword() {
    return this.password != null;
  }

  public void setPasswordIsSet(boolean value) {
    if (!value) {
      this.password = null;
    }
  }

  public TAuthenticationTokenIdentifier getIdentifier() {
    return this.identifier;
  }

  public TDelegationToken setIdentifier(TAuthenticationTokenIdentifier identifier) {
    this.identifier = identifier;
    return this;
  }

  public void unsetIdentifier() {
    this.identifier = null;
  }

  /** Returns true if field identifier is set (has been assigned a value) and false otherwise */
  public boolean isSetIdentifier() {
    return this.identifier != null;
  }

  public void setIdentifierIsSet(boolean value) {
    if (!value) {
      this.identifier = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case PASSWORD:
      if (value == null) {
        unsetPassword();
      } else {
        setPassword((ByteBuffer)value);
      }
      break;

    case IDENTIFIER:
      if (value == null) {
        unsetIdentifier();
      } else {
        setIdentifier((TAuthenticationTokenIdentifier)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case PASSWORD:
      return getPassword();

    case IDENTIFIER:
      return getIdentifier();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case PASSWORD:
      return isSetPassword();
    case IDENTIFIER:
      return isSetIdentifier();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TDelegationToken)
      return this.equals((TDelegationToken)that);
    return false;
  }

  public boolean equals(TDelegationToken that) {
    if (that == null)
      return false;

    boolean this_present_password = true && this.isSetPassword();
    boolean that_present_password = true && that.isSetPassword();
    if (this_present_password || that_present_password) {
      if (!(this_present_password && that_present_password))
        return false;
      if (!this.password.equals(that.password))
        return false;
    }

    boolean this_present_identifier = true && this.isSetIdentifier();
    boolean that_present_identifier = true && that.isSetIdentifier();
    if (this_present_identifier || that_present_identifier) {
      if (!(this_present_identifier && that_present_identifier))
        return false;
      if (!this.identifier.equals(that.identifier))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(TDelegationToken other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetPassword()).compareTo(other.isSetPassword());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPassword()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.password, other.password);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIdentifier()).compareTo(other.isSetIdentifier());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIdentifier()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.identifier, other.identifier);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TDelegationToken(");
    boolean first = true;

    sb.append("password:");
    if (this.password == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.password, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("identifier:");
    if (this.identifier == null) {
      sb.append("null");
    } else {
      sb.append(this.identifier);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (identifier != null) {
      identifier.validate();
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TDelegationTokenStandardSchemeFactory implements SchemeFactory {
    public TDelegationTokenStandardScheme getScheme() {
      return new TDelegationTokenStandardScheme();
    }
  }

  private static class TDelegationTokenStandardScheme extends StandardScheme<TDelegationToken> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TDelegationToken struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // PASSWORD
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.password = iprot.readBinary();
              struct.setPasswordIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // IDENTIFIER
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.identifier = new TAuthenticationTokenIdentifier();
              struct.identifier.read(iprot);
              struct.setIdentifierIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TDelegationToken struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.password != null) {
        oprot.writeFieldBegin(PASSWORD_FIELD_DESC);
        oprot.writeBinary(struct.password);
        oprot.writeFieldEnd();
      }
      if (struct.identifier != null) {
        oprot.writeFieldBegin(IDENTIFIER_FIELD_DESC);
        struct.identifier.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TDelegationTokenTupleSchemeFactory implements SchemeFactory {
    public TDelegationTokenTupleScheme getScheme() {
      return new TDelegationTokenTupleScheme();
    }
  }

  private static class TDelegationTokenTupleScheme extends TupleScheme<TDelegationToken> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TDelegationToken struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetPassword()) {
        optionals.set(0);
      }
      if (struct.isSetIdentifier()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetPassword()) {
        oprot.writeBinary(struct.password);
      }
      if (struct.isSetIdentifier()) {
        struct.identifier.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TDelegationToken struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.password = iprot.readBinary();
        struct.setPasswordIsSet(true);
      }
      if (incoming.get(1)) {
        struct.identifier = new TAuthenticationTokenIdentifier();
        struct.identifier.read(iprot);
        struct.setIdentifierIsSet(true);
      }
    }
  }

}

diff --git a/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenConfig.java b/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenConfig.java
new file mode 100644
index 000000000..cdde83ef6
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenConfig.java
@@ -0,0 +1,399 @@
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
/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.core.security.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "serial", "rawtypes", "unused"}) public class TDelegationTokenConfig implements org.apache.thrift.TBase<TDelegationTokenConfig, TDelegationTokenConfig._Fields>, java.io.Serializable, Cloneable, Comparable<TDelegationTokenConfig> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TDelegationTokenConfig");

  private static final org.apache.thrift.protocol.TField LIFETIME_FIELD_DESC = new org.apache.thrift.protocol.TField("lifetime", org.apache.thrift.protocol.TType.I64, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TDelegationTokenConfigStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TDelegationTokenConfigTupleSchemeFactory());
  }

  public long lifetime; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    LIFETIME((short)1, "lifetime");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // LIFETIME
          return LIFETIME;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __LIFETIME_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.LIFETIME};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.LIFETIME, new org.apache.thrift.meta_data.FieldMetaData("lifetime", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TDelegationTokenConfig.class, metaDataMap);
  }

  public TDelegationTokenConfig() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TDelegationTokenConfig(TDelegationTokenConfig other) {
    __isset_bitfield = other.__isset_bitfield;
    this.lifetime = other.lifetime;
  }

  public TDelegationTokenConfig deepCopy() {
    return new TDelegationTokenConfig(this);
  }

  @Override
  public void clear() {
    setLifetimeIsSet(false);
    this.lifetime = 0;
  }

  public long getLifetime() {
    return this.lifetime;
  }

  public TDelegationTokenConfig setLifetime(long lifetime) {
    this.lifetime = lifetime;
    setLifetimeIsSet(true);
    return this;
  }

  public void unsetLifetime() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __LIFETIME_ISSET_ID);
  }

  /** Returns true if field lifetime is set (has been assigned a value) and false otherwise */
  public boolean isSetLifetime() {
    return EncodingUtils.testBit(__isset_bitfield, __LIFETIME_ISSET_ID);
  }

  public void setLifetimeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __LIFETIME_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case LIFETIME:
      if (value == null) {
        unsetLifetime();
      } else {
        setLifetime((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case LIFETIME:
      return Long.valueOf(getLifetime());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case LIFETIME:
      return isSetLifetime();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TDelegationTokenConfig)
      return this.equals((TDelegationTokenConfig)that);
    return false;
  }

  public boolean equals(TDelegationTokenConfig that) {
    if (that == null)
      return false;

    boolean this_present_lifetime = true && this.isSetLifetime();
    boolean that_present_lifetime = true && that.isSetLifetime();
    if (this_present_lifetime || that_present_lifetime) {
      if (!(this_present_lifetime && that_present_lifetime))
        return false;
      if (this.lifetime != that.lifetime)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(TDelegationTokenConfig other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetLifetime()).compareTo(other.isSetLifetime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetLifetime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.lifetime, other.lifetime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TDelegationTokenConfig(");
    boolean first = true;

    if (isSetLifetime()) {
      sb.append("lifetime:");
      sb.append(this.lifetime);
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TDelegationTokenConfigStandardSchemeFactory implements SchemeFactory {
    public TDelegationTokenConfigStandardScheme getScheme() {
      return new TDelegationTokenConfigStandardScheme();
    }
  }

  private static class TDelegationTokenConfigStandardScheme extends StandardScheme<TDelegationTokenConfig> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TDelegationTokenConfig struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // LIFETIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.lifetime = iprot.readI64();
              struct.setLifetimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TDelegationTokenConfig struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.isSetLifetime()) {
        oprot.writeFieldBegin(LIFETIME_FIELD_DESC);
        oprot.writeI64(struct.lifetime);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TDelegationTokenConfigTupleSchemeFactory implements SchemeFactory {
    public TDelegationTokenConfigTupleScheme getScheme() {
      return new TDelegationTokenConfigTupleScheme();
    }
  }

  private static class TDelegationTokenConfigTupleScheme extends TupleScheme<TDelegationTokenConfig> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TDelegationTokenConfig struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetLifetime()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetLifetime()) {
        oprot.writeI64(struct.lifetime);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TDelegationTokenConfig struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.lifetime = iprot.readI64();
        struct.setLifetimeIsSet(true);
      }
    }
  }

}

diff --git a/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenOptions.java b/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenOptions.java
new file mode 100644
index 000000000..c19eb7563
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/security/thrift/TDelegationTokenOptions.java
@@ -0,0 +1,399 @@
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
/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.core.security.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "serial", "rawtypes", "unused"}) public class TDelegationTokenOptions implements org.apache.thrift.TBase<TDelegationTokenOptions, TDelegationTokenOptions._Fields>, java.io.Serializable, Cloneable, Comparable<TDelegationTokenOptions> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TDelegationTokenOptions");

  private static final org.apache.thrift.protocol.TField LIFETIME_FIELD_DESC = new org.apache.thrift.protocol.TField("lifetime", org.apache.thrift.protocol.TType.I64, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TDelegationTokenOptionsStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TDelegationTokenOptionsTupleSchemeFactory());
  }

  public long lifetime; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    LIFETIME((short)1, "lifetime");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // LIFETIME
          return LIFETIME;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __LIFETIME_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private _Fields optionals[] = {_Fields.LIFETIME};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.LIFETIME, new org.apache.thrift.meta_data.FieldMetaData("lifetime", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TDelegationTokenOptions.class, metaDataMap);
  }

  public TDelegationTokenOptions() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TDelegationTokenOptions(TDelegationTokenOptions other) {
    __isset_bitfield = other.__isset_bitfield;
    this.lifetime = other.lifetime;
  }

  public TDelegationTokenOptions deepCopy() {
    return new TDelegationTokenOptions(this);
  }

  @Override
  public void clear() {
    setLifetimeIsSet(false);
    this.lifetime = 0;
  }

  public long getLifetime() {
    return this.lifetime;
  }

  public TDelegationTokenOptions setLifetime(long lifetime) {
    this.lifetime = lifetime;
    setLifetimeIsSet(true);
    return this;
  }

  public void unsetLifetime() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __LIFETIME_ISSET_ID);
  }

  /** Returns true if field lifetime is set (has been assigned a value) and false otherwise */
  public boolean isSetLifetime() {
    return EncodingUtils.testBit(__isset_bitfield, __LIFETIME_ISSET_ID);
  }

  public void setLifetimeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __LIFETIME_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case LIFETIME:
      if (value == null) {
        unsetLifetime();
      } else {
        setLifetime((Long)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case LIFETIME:
      return Long.valueOf(getLifetime());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case LIFETIME:
      return isSetLifetime();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TDelegationTokenOptions)
      return this.equals((TDelegationTokenOptions)that);
    return false;
  }

  public boolean equals(TDelegationTokenOptions that) {
    if (that == null)
      return false;

    boolean this_present_lifetime = true && this.isSetLifetime();
    boolean that_present_lifetime = true && that.isSetLifetime();
    if (this_present_lifetime || that_present_lifetime) {
      if (!(this_present_lifetime && that_present_lifetime))
        return false;
      if (this.lifetime != that.lifetime)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(TDelegationTokenOptions other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetLifetime()).compareTo(other.isSetLifetime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetLifetime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.lifetime, other.lifetime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TDelegationTokenOptions(");
    boolean first = true;

    if (isSetLifetime()) {
      sb.append("lifetime:");
      sb.append(this.lifetime);
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TDelegationTokenOptionsStandardSchemeFactory implements SchemeFactory {
    public TDelegationTokenOptionsStandardScheme getScheme() {
      return new TDelegationTokenOptionsStandardScheme();
    }
  }

  private static class TDelegationTokenOptionsStandardScheme extends StandardScheme<TDelegationTokenOptions> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TDelegationTokenOptions struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // LIFETIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.lifetime = iprot.readI64();
              struct.setLifetimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TDelegationTokenOptions struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.isSetLifetime()) {
        oprot.writeFieldBegin(LIFETIME_FIELD_DESC);
        oprot.writeI64(struct.lifetime);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TDelegationTokenOptionsTupleSchemeFactory implements SchemeFactory {
    public TDelegationTokenOptionsTupleScheme getScheme() {
      return new TDelegationTokenOptionsTupleScheme();
    }
  }

  private static class TDelegationTokenOptionsTupleScheme extends TupleScheme<TDelegationTokenOptions> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TDelegationTokenOptions struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetLifetime()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetLifetime()) {
        oprot.writeI64(struct.lifetime);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TDelegationTokenOptions struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.lifetime = iprot.readI64();
        struct.setLifetimeIsSet(true);
      }
    }
  }

}

diff --git a/core/src/main/java/org/apache/accumulo/core/util/ThriftMessageUtil.java b/core/src/main/java/org/apache/accumulo/core/util/ThriftMessageUtil.java
new file mode 100644
index 000000000..c79aac015
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/util/ThriftMessageUtil.java
@@ -0,0 +1,109 @@
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.AutoExpandingBufferWriteTransport;
import org.apache.thrift.transport.TMemoryInputTransport;

/**
 * Serializes and deserializes Thrift messages to and from byte arrays. This class is not thread-safe, external synchronization is necessary if it is used
 * concurrently.
 */
public class ThriftMessageUtil {

  private final AutoExpandingBufferWriteTransport transport;
  private final TProtocol protocol;

  public ThriftMessageUtil() {
    this(64, 1.5);
  }

  public ThriftMessageUtil(int initialCapacity, double growthCoefficient) {
    // TODO does this make sense? better to push this down to the serialize method (accept the transport as an argument)?
    this.transport = new AutoExpandingBufferWriteTransport(initialCapacity, growthCoefficient);
    this.protocol = new TCompactProtocol(transport);
  }

  /**
   * Convert the {@link msg} to a byte array representation
   *
   * @param msg
   *          The message to serialize
   * @return The serialized message
   * @throws IOException
   *           When serialization fails
   */
  public ByteBuffer serialize(TBase<?,?> msg) throws IOException {
    checkNotNull(msg);
    transport.reset();
    try {
      msg.write(protocol);
      // We should flush(), but we know its a noop
    } catch (TException e) {
      throw new IOException(e);
    }
    return ByteBuffer.wrap(transport.getBuf().array(), 0, transport.getPos());
  }

  /**
   * @see #deserialize(byte[], int, int, T)
   */
  public <T extends TBase<?,?>> T deserialize(ByteBuffer serialized, T instance) throws IOException {
    checkNotNull(serialized);
    return deserialize(serialized.array(), serialized.arrayOffset(), serialized.limit(), instance);
  }

  /**
   * Assumes the entire contents of the byte array compose the serialized {@link instance}
   *
   * @see #deserialize(byte[], int, int, TBase)
   */
  public <T extends TBase<?,?>> T deserialize(byte[] serialized, T instance) throws IOException {
    return deserialize(serialized, 0, serialized.length, instance);
  }

  /**
   * Deserializes a message into the provided {@link instance} from {@link serialized}
   *
   * @param serialized
   *          The serialized representation of the object
   * @param instance
   *          An instance of the object to reconstitute
   * @return The reconstituted instance provided
   * @throws IOException
   *           When deserialization fails
   */
  public <T extends TBase<?,?>> T deserialize(byte[] serialized, int offset, int length, T instance) throws IOException {
    checkNotNull(instance);
    TCompactProtocol proto = new TCompactProtocol(new TMemoryInputTransport(serialized, offset, length));
    try {
      instance.read(proto);
    } catch (TException e) {
      throw new IOException(e);
    }
    return instance;
  }
}
diff --git a/core/src/main/thrift/master.thrift b/core/src/main/thrift/master.thrift
index d89e3818d..8a83438c2 100644
-- a/core/src/main/thrift/master.thrift
++ b/core/src/main/thrift/master.thrift
@@ -173,4 +173,7 @@ service MasterClientService extends FateService {
   oneway void reportTabletStatus(5:trace.TInfo tinfo, 1:security.TCredentials credentials, 2:string serverName, 3:TabletLoadState status, 4:data.TKeyExtent tablet)
 
   list<string> getActiveTservers(1:trace.TInfo tinfo, 2:security.TCredentials credentials) throws (1:client.ThriftSecurityException sec)

  // Delegation token request
  security.TDelegationToken getDelegationToken(1:trace.TInfo tinfo, 2:security.TCredentials credentials, 3:security.TDelegationTokenConfig cfg) throws (1:client.ThriftSecurityException sec)
 }
diff --git a/core/src/main/thrift/security.thrift b/core/src/main/thrift/security.thrift
index 66235a8e9..74b7f128b 100644
-- a/core/src/main/thrift/security.thrift
++ b/core/src/main/thrift/security.thrift
@@ -24,3 +24,26 @@ struct TCredentials {
     4:string instanceId
 }
 
struct TAuthenticationTokenIdentifier {
    1:string principal,
    2:optional i32 keyId,
    3:optional i64 issueDate,
    4:optional i64 expirationDate,
    5:optional string instanceId
}

struct TAuthenticationKey {
    1:binary secret,
    2:optional i32 keyId,
    3:optional i64 expirationDate,
    4:optional i64 creationDate
}

struct TDelegationToken {
    1:binary password,
    2:TAuthenticationTokenIdentifier identifier
}

struct TDelegationTokenConfig {
    1:optional i64 lifetime
}
\ No newline at end of file
diff --git a/core/src/test/java/org/apache/accumulo/core/client/admin/DelegationTokenConfigTest.java b/core/src/test/java/org/apache/accumulo/core/client/admin/DelegationTokenConfigTest.java
new file mode 100644
index 000000000..f1553dce7
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/client/admin/DelegationTokenConfigTest.java
@@ -0,0 +1,63 @@
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
package org.apache.accumulo.core.client.admin;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class DelegationTokenConfigTest {

  @Test
  public void testTimeUnit() {
    DelegationTokenConfig config1 = new DelegationTokenConfig(), config2 = new DelegationTokenConfig();

    config1.setTokenLifetime(1000, TimeUnit.MILLISECONDS);
    config2.setTokenLifetime(1, TimeUnit.SECONDS);

    assertEquals(config1.getTokenLifetime(TimeUnit.MILLISECONDS), config2.getTokenLifetime(TimeUnit.MILLISECONDS));
    assertEquals(config1, config2);
    assertEquals(config1.hashCode(), config2.hashCode());
  }

  @Test
  public void testNoTimeout() {
    DelegationTokenConfig config = new DelegationTokenConfig();

    config.setTokenLifetime(0, TimeUnit.MILLISECONDS);

    assertEquals(0, config.getTokenLifetime(TimeUnit.MILLISECONDS));

  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidLifetime() {
    new DelegationTokenConfig().setTokenLifetime(-1, TimeUnit.DAYS);
  }

  @Test(expected = NullPointerException.class)
  public void testSetInvalidTimeUnit() {
    new DelegationTokenConfig().setTokenLifetime(5, null);
  }

  @Test(expected = NullPointerException.class)
  public void testGetInvalidTimeUnit() {
    new DelegationTokenConfig().getTokenLifetime(null);
  }
}
diff --git a/core/src/test/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializerTest.java b/core/src/test/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializerTest.java
new file mode 100644
index 000000000..4499a5846
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/client/impl/DelegationTokenConfigSerializerTest.java
@@ -0,0 +1,40 @@
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
package org.apache.accumulo.core.client.impl;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.security.thrift.TDelegationTokenConfig;
import org.junit.Test;

public class DelegationTokenConfigSerializerTest {

  @Test
  public void test() {
    DelegationTokenConfig cfg = new DelegationTokenConfig();
    cfg.setTokenLifetime(8323, TimeUnit.HOURS);

    TDelegationTokenConfig tCfg = DelegationTokenConfigSerializer.serialize(cfg);
    assertEquals(tCfg.getLifetime(), cfg.getTokenLifetime(TimeUnit.MILLISECONDS));

    assertEquals(cfg, DelegationTokenConfigSerializer.deserialize(tCfg));
  }

}
diff --git a/core/src/test/java/org/apache/accumulo/core/client/impl/ThriftTransportKeyTest.java b/core/src/test/java/org/apache/accumulo/core/client/impl/ThriftTransportKeyTest.java
index 2723273e3..04b9ae8e5 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/impl/ThriftTransportKeyTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/impl/ThriftTransportKeyTest.java
@@ -20,17 +20,38 @@ import static org.easymock.EasyMock.createMock;
 import static org.easymock.EasyMock.expect;
 import static org.easymock.EasyMock.replay;
 import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNotEquals;
 import static org.junit.Assert.assertTrue;
 
import java.io.IOException;
import java.security.PrivilegedExceptionAction;

import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.rpc.SaslConnectionParams;
 import org.apache.accumulo.core.rpc.SslConnectionParams;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.security.UserGroupInformation;
import org.easymock.EasyMock;
import org.junit.Before;
 import org.junit.Test;
 
 import com.google.common.net.HostAndPort;
 
 public class ThriftTransportKeyTest {
 
  @Before
  public void setup() throws Exception {
    System.setProperty("java.security.krb5.realm", "accumulo");
    System.setProperty("java.security.krb5.kdc", "fake");
    Configuration conf = new Configuration(false);
    conf.set(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION, "kerberos");
    UserGroupInformation.setConfiguration(conf);
  }

   @Test(expected = RuntimeException.class)
   public void testSslAndSaslErrors() {
     ClientContext clientCtx = createMock(ClientContext.class);
@@ -38,7 +59,7 @@ public class ThriftTransportKeyTest {
     SaslConnectionParams saslParams = createMock(SaslConnectionParams.class);
 
     expect(clientCtx.getClientSslParams()).andReturn(sslParams).anyTimes();
    expect(clientCtx.getClientSaslParams()).andReturn(saslParams).anyTimes();
    expect(clientCtx.getSaslParams()).andReturn(saslParams).anyTimes();
 
     // We don't care to verify the sslparam or saslparam mocks
     replay(clientCtx);
@@ -51,20 +72,78 @@ public class ThriftTransportKeyTest {
   }
 
   @Test
  public void testSaslPrincipalIsSignificant() {
    SaslConnectionParams saslParams1 = createMock(SaslConnectionParams.class), saslParams2 = createMock(SaslConnectionParams.class);
    expect(saslParams1.getPrincipal()).andReturn("user1");
    expect(saslParams2.getPrincipal()).andReturn("user2");
  public void testConnectionCaching() throws IOException, InterruptedException {
    UserGroupInformation user1 = UserGroupInformation.createUserForTesting("user1", new String[0]);
    final KerberosToken token = EasyMock.createMock(KerberosToken.class);
    final ClientConfiguration clientConf = ClientConfiguration.loadDefault();
    // The primary is the first component of the principal
    final String primary = "accumulo";
    clientConf.withSasl(true, primary);

    // A first instance of the SASL cnxn params
    SaslConnectionParams saslParams1 = user1.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        return new SaslConnectionParams(clientConf, token);
      }
    });

    // A second instance of what should be the same SaslConnectionParams
    SaslConnectionParams saslParams2 = user1.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        return new SaslConnectionParams(clientConf, token);
      }
    });
 
    replay(saslParams1, saslParams2);
    ThriftTransportKey ttk1 = new ThriftTransportKey(HostAndPort.fromParts("localhost", 9997), 1l, null, saslParams1), ttk2 = new ThriftTransportKey(
        HostAndPort.fromParts("localhost", 9997), 1l, null, saslParams2);

    // Should equals() and hashCode() to make sure we don't throw away thrift cnxns
    assertEquals(ttk1, ttk2);
    assertEquals(ttk1.hashCode(), ttk2.hashCode());
  }

  @Test
  public void testSaslPrincipalIsSignificant() throws IOException, InterruptedException {
    UserGroupInformation user1 = UserGroupInformation.createUserForTesting("user1", new String[0]);
    final KerberosToken token = EasyMock.createMock(KerberosToken.class);
    SaslConnectionParams saslParams1 = user1.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        return new SaslConnectionParams(clientConf, token);
      }
    });

    UserGroupInformation user2 = UserGroupInformation.createUserForTesting("user2", new String[0]);
    SaslConnectionParams saslParams2 = user2.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        return new SaslConnectionParams(clientConf, token);
      }
    });
 
     ThriftTransportKey ttk1 = new ThriftTransportKey(HostAndPort.fromParts("localhost", 9997), 1l, null, saslParams1), ttk2 = new ThriftTransportKey(
         HostAndPort.fromParts("localhost", 9997), 1l, null, saslParams2);
 
     assertNotEquals(ttk1, ttk2);
     assertNotEquals(ttk1.hashCode(), ttk2.hashCode());

    verify(saslParams1, saslParams2);
   }
 
   @Test
@@ -72,7 +151,7 @@ public class ThriftTransportKeyTest {
     ClientContext clientCtx = createMock(ClientContext.class);
 
     expect(clientCtx.getClientSslParams()).andReturn(null).anyTimes();
    expect(clientCtx.getClientSaslParams()).andReturn(null).anyTimes();
    expect(clientCtx.getSaslParams()).andReturn(null).anyTimes();
 
     replay(clientCtx);
 
diff --git a/core/src/test/java/org/apache/accumulo/core/client/security/tokens/DelegationTokenTest.java b/core/src/test/java/org/apache/accumulo/core/client/security/tokens/DelegationTokenTest.java
new file mode 100644
index 000000000..f66a1eef1
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/client/security/tokens/DelegationTokenTest.java
@@ -0,0 +1,72 @@
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
package org.apache.accumulo.core.client.security.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
import org.junit.Test;

public class DelegationTokenTest {

  @Test
  public void testSerialization() throws IOException {
    AuthenticationTokenIdentifier identifier = new AuthenticationTokenIdentifier("user", 1, 1000l, 2000l, "instanceid");
    // We don't need a real serialized Token for the password
    DelegationToken token = new DelegationToken(new byte[] {'f', 'a', 'k', 'e'}, identifier);
    assertEquals(token, token);
    assertEquals(token.hashCode(), token.hashCode());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    token.write(new DataOutputStream(baos));

    DelegationToken copy = new DelegationToken();
    copy.readFields(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())));

    assertEquals(token, copy);
    assertEquals(token.hashCode(), copy.hashCode());
  }

  @Test
  public void testEquality() throws IOException {
    AuthenticationTokenIdentifier identifier = new AuthenticationTokenIdentifier("user", 1, 1000l, 2000l, "instanceid");
    // We don't need a real serialized Token for the password
    DelegationToken token = new DelegationToken(new byte[] {'f', 'a', 'k', 'e'}, identifier);

    AuthenticationTokenIdentifier identifier2 = new AuthenticationTokenIdentifier("user1", 1, 1000l, 2000l, "instanceid");
    // We don't need a real serialized Token for the password
    DelegationToken token2 = new DelegationToken(new byte[] {'f', 'a', 'k', 'e'}, identifier2);

    assertNotEquals(token, token2);
    assertNotEquals(token.hashCode(), token2.hashCode());

    // We don't need a real serialized Token for the password
    DelegationToken token3 = new DelegationToken(new byte[] {'f', 'a', 'k', 'e', '0'}, identifier);

    assertNotEquals(token, token3);
    assertNotEquals(token.hashCode(), token3.hashCode());
    assertNotEquals(token2, token3);
    assertNotEquals(token2.hashCode(), token3.hashCode());
  }
}
diff --git a/core/src/test/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandlerTest.java b/core/src/test/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandlerTest.java
new file mode 100644
index 000000000..f38e2e349
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/rpc/SaslClientDigestCallbackHandlerTest.java
@@ -0,0 +1,33 @@
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
package org.apache.accumulo.core.rpc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SaslClientDigestCallbackHandlerTest {

  @Test
  public void testEquality() {
    SaslClientDigestCallbackHandler handler1 = new SaslClientDigestCallbackHandler("user", "mypass".toCharArray()), handler2 = new SaslClientDigestCallbackHandler(
        "user", "mypass".toCharArray());
    assertEquals(handler1, handler2);
    assertEquals(handler1.hashCode(), handler2.hashCode());
  }

}
diff --git a/core/src/test/java/org/apache/accumulo/core/rpc/SaslConnectionParamsTest.java b/core/src/test/java/org/apache/accumulo/core/rpc/SaslConnectionParamsTest.java
index 3910f34af..9b77d25c8 100644
-- a/core/src/test/java/org/apache/accumulo/core/rpc/SaslConnectionParamsTest.java
++ b/core/src/test/java/org/apache/accumulo/core/rpc/SaslConnectionParamsTest.java
@@ -17,7 +17,8 @@
 package org.apache.accumulo.core.rpc;
 
 import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
 
 import java.security.PrivilegedExceptionAction;
 import java.util.Map;
@@ -27,12 +28,17 @@ import javax.security.sasl.Sasl;
 import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.rpc.SaslConnectionParams.QualityOfProtection;
import org.apache.accumulo.core.rpc.SaslConnectionParams.SaslMechanism;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
 import org.apache.hadoop.security.UserGroupInformation;
import org.easymock.EasyMock;
 import org.junit.Before;
 import org.junit.Test;
 
@@ -53,15 +59,37 @@ public class SaslConnectionParamsTest {
   }
 
   @Test
  public void testNullParams() {
    ClientConfiguration clientConf = new ClientConfiguration();
    AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
    assertEquals("false", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));
    assertNull(SaslConnectionParams.forConfig(rpcConf));
  public void testDefaultParamsAsClient() throws Exception {
    final KerberosToken token = EasyMock.createMock(KerberosToken.class);
    testUser.doAs(new PrivilegedExceptionAction<Void>() {
      @Override
      public Void run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        final SaslConnectionParams saslParams = new SaslConnectionParams(clientConf, token);
        assertEquals(primary, saslParams.getKerberosServerPrimary());

        final QualityOfProtection defaultQop = QualityOfProtection.get(Property.RPC_SASL_QOP.getDefaultValue());
        assertEquals(defaultQop, saslParams.getQualityOfProtection());

        Map<String,String> properties = saslParams.getSaslProperties();
        assertEquals(1, properties.size());
        assertEquals(defaultQop.getQuality(), properties.get(Sasl.QOP));
        assertEquals(username, saslParams.getPrincipal());
        return null;
      }
    });
   }
 
   @Test
  public void testDefaultParamsAsClient() throws Exception {
  public void testDefaultParams() throws Exception {
    final KerberosToken token = EasyMock.createMock(KerberosToken.class);
     testUser.doAs(new PrivilegedExceptionAction<Void>() {
       @Override
       public Void run() throws Exception {
@@ -71,9 +99,10 @@ public class SaslConnectionParamsTest {
         final String primary = "accumulo";
         clientConf.withSasl(true, primary);
 
        final AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
         assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));
 
        final SaslConnectionParams saslParams = SaslConnectionParams.forConfig(clientConf);
        final SaslConnectionParams saslParams = new SaslConnectionParams(rpcConf, token);
         assertEquals(primary, saslParams.getKerberosServerPrimary());
 
         final QualityOfProtection defaultQop = QualityOfProtection.get(Property.RPC_SASL_QOP.getDefaultValue());
@@ -89,7 +118,8 @@ public class SaslConnectionParamsTest {
   }
 
   @Test
  public void testDefaultParamsAsServer() throws Exception {
  public void testDelegationToken() throws Exception {
    final DelegationToken token = new DelegationToken(new byte[0], new AuthenticationTokenIdentifier("user", 1, 10l, 20l, "instanceid"));
     testUser.doAs(new PrivilegedExceptionAction<Void>() {
       @Override
       public Void run() throws Exception {
@@ -102,12 +132,16 @@ public class SaslConnectionParamsTest {
         final AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
         assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));
 
        final SaslConnectionParams saslParams = SaslConnectionParams.forConfig(rpcConf);
        final SaslConnectionParams saslParams = new SaslConnectionParams(rpcConf, token);
         assertEquals(primary, saslParams.getKerberosServerPrimary());
 
         final QualityOfProtection defaultQop = QualityOfProtection.get(Property.RPC_SASL_QOP.getDefaultValue());
         assertEquals(defaultQop, saslParams.getQualityOfProtection());
 
        assertEquals(SaslMechanism.DIGEST_MD5, saslParams.getMechanism());
        assertNotNull(saslParams.getCallbackHandler());
        assertEquals(SaslClientDigestCallbackHandler.class, saslParams.getCallbackHandler().getClass());

         Map<String,String> properties = saslParams.getSaslProperties();
         assertEquals(1, properties.size());
         assertEquals(defaultQop.getQuality(), properties.get(Sasl.QOP));
@@ -117,4 +151,89 @@ public class SaslConnectionParamsTest {
     });
   }
 
  @Test
  public void testEquality() throws Exception {
    final KerberosToken token = EasyMock.createMock(KerberosToken.class);
    SaslConnectionParams params1 = testUser.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        final AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        return new SaslConnectionParams(rpcConf, token);
      }
    });

    SaslConnectionParams params2 = testUser.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        final AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        return new SaslConnectionParams(rpcConf, token);
      }
    });

    assertEquals(params1, params2);
    assertEquals(params1.hashCode(), params2.hashCode());

    final DelegationToken delToken1 = new DelegationToken(new byte[0], new AuthenticationTokenIdentifier("user", 1, 10l, 20l, "instanceid"));
    SaslConnectionParams params3 = testUser.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        final AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        return new SaslConnectionParams(rpcConf, delToken1);
      }
    });

    assertNotEquals(params1, params3);
    assertNotEquals(params1.hashCode(), params3.hashCode());
    assertNotEquals(params2, params3);
    assertNotEquals(params2.hashCode(), params3.hashCode());

    final DelegationToken delToken2 = new DelegationToken(new byte[0], new AuthenticationTokenIdentifier("user", 1, 10l, 20l, "instanceid"));
    SaslConnectionParams params4 = testUser.doAs(new PrivilegedExceptionAction<SaslConnectionParams>() {
      @Override
      public SaslConnectionParams run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        final AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        return new SaslConnectionParams(rpcConf, delToken2);
      }
    });

    assertNotEquals(params1, params4);
    assertNotEquals(params1.hashCode(), params4.hashCode());
    assertNotEquals(params2, params4);
    assertNotEquals(params2.hashCode(), params4.hashCode());

    assertEquals(params3, params4);
    assertEquals(params3.hashCode(), params4.hashCode());
  }
 }
diff --git a/core/src/test/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifierTest.java b/core/src/test/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifierTest.java
new file mode 100644
index 000000000..d3c1f20f3
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/security/AuthenticationTokenIdentifierTest.java
@@ -0,0 +1,111 @@
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
package org.apache.accumulo.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;

public class AuthenticationTokenIdentifierTest {

  @Test
  public void testUgi() {
    String principal = "user";
    AuthenticationTokenIdentifier token = new AuthenticationTokenIdentifier(principal);
    UserGroupInformation actual = token.getUser(), expected = UserGroupInformation.createRemoteUser(principal);
    assertEquals(expected.getAuthenticationMethod(), actual.getAuthenticationMethod());
    assertEquals(expected.getUserName(), expected.getUserName());
  }

  @Test
  public void testEquality() {
    String principal = "user";
    AuthenticationTokenIdentifier token = new AuthenticationTokenIdentifier(principal);
    assertEquals(token, token);
    AuthenticationTokenIdentifier newToken = new AuthenticationTokenIdentifier(principal);
    assertEquals(token, newToken);
    assertEquals(token.hashCode(), newToken.hashCode());
  }

  @Test
  public void testExtendedEquality() {
    String principal = "user";
    AuthenticationTokenIdentifier token = new AuthenticationTokenIdentifier(principal);
    assertEquals(token, token);
    AuthenticationTokenIdentifier newToken = new AuthenticationTokenIdentifier(principal, 1, 5l, 10l, "uuid");
    assertNotEquals(token, newToken);
    assertNotEquals(token.hashCode(), newToken.hashCode());
    AuthenticationTokenIdentifier dblNewToken = new AuthenticationTokenIdentifier(principal);
    dblNewToken.setKeyId(1);
    dblNewToken.setIssueDate(5l);
    dblNewToken.setExpirationDate(10l);
    dblNewToken.setInstanceId("uuid");
  }

  @Test
  public void testToString() {
    String principal = "my_special_principal";
    AuthenticationTokenIdentifier token = new AuthenticationTokenIdentifier(principal);
    assertTrue(token.toString().contains(principal));
  }

  @Test
  public void testSerialization() throws IOException {
    String principal = "my_special_principal";
    AuthenticationTokenIdentifier token = new AuthenticationTokenIdentifier(principal);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    token.write(out);
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
    AuthenticationTokenIdentifier deserializedToken = new AuthenticationTokenIdentifier();
    deserializedToken.readFields(in);
    assertEquals(token, deserializedToken);
    assertEquals(token.hashCode(), deserializedToken.hashCode());
    assertEquals(token.toString(), deserializedToken.toString());
  }

  @Test
  public void testTokenKind() {
    String principal = "my_special_principal";
    AuthenticationTokenIdentifier token = new AuthenticationTokenIdentifier(principal);
    assertEquals(AuthenticationTokenIdentifier.TOKEN_KIND, token.getKind());
  }

  @Test
  public void testNullMsg() throws IOException {
    AuthenticationTokenIdentifier token = new AuthenticationTokenIdentifier();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    token.write(out);
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
    AuthenticationTokenIdentifier deserializedToken = new AuthenticationTokenIdentifier();
    deserializedToken.readFields(in);
    assertEquals(token, deserializedToken);
    assertEquals(token.hashCode(), deserializedToken.hashCode());
    assertEquals(token.toString(), deserializedToken.toString());

  }
}
diff --git a/core/src/test/java/org/apache/accumulo/core/util/ThriftMessageUtilTest.java b/core/src/test/java/org/apache/accumulo/core/util/ThriftMessageUtilTest.java
new file mode 100644
index 000000000..765d9ca9b
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/util/ThriftMessageUtilTest.java
@@ -0,0 +1,83 @@
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.accumulo.core.security.thrift.TAuthenticationTokenIdentifier;
import org.junit.Before;
import org.junit.Test;

public class ThriftMessageUtilTest {

  private TAuthenticationTokenIdentifier msg;
  private ThriftMessageUtil util;

  @Before
  public void setup() {
    msg = new TAuthenticationTokenIdentifier("principal");
    util = new ThriftMessageUtil();
  }

  @Test
  public void testSerialization() throws IOException {
    ByteBuffer buff = util.serialize(msg);
    TAuthenticationTokenIdentifier bbMsg = new TAuthenticationTokenIdentifier();
    util.deserialize(buff, bbMsg);
    assertEquals(msg, bbMsg);
  }

  @Test
  public void testSerializationAsByteArray() throws IOException {
    ByteBuffer buff = util.serialize(msg);
    TAuthenticationTokenIdentifier copy = new TAuthenticationTokenIdentifier();
    byte[] array = new byte[buff.limit()];
    System.arraycopy(buff.array(), 0, array, 0, buff.limit());
    util.deserialize(array, copy);
    assertEquals(msg, copy);
  }

  @Test
  public void testSerializationAsByteArrayWithLimits() throws IOException {
    ByteBuffer buff = util.serialize(msg);
    TAuthenticationTokenIdentifier copy = new TAuthenticationTokenIdentifier();

    byte[] array = new byte[buff.limit() + 14];
    // Throw some garbage in front and behind the actual message
    array[0] = 'G';
    array[1] = 'A';
    array[2] = 'R';
    array[3] = 'B';
    array[4] = 'A';
    array[5] = 'G';
    array[6] = 'E';
    System.arraycopy(buff.array(), 0, array, 7, buff.limit());
    array[7 + buff.limit()] = 'G';
    array[7 + buff.limit() + 1] = 'A';
    array[7 + buff.limit() + 2] = 'R';
    array[7 + buff.limit() + 3] = 'B';
    array[7 + buff.limit() + 4] = 'A';
    array[7 + buff.limit() + 5] = 'G';
    array[7 + buff.limit() + 6] = 'E';

    util.deserialize(array, 7, buff.limit(), copy);
    assertEquals(msg, copy);
  }
}
diff --git a/docs/src/main/asciidoc/chapters/kerberos.txt b/docs/src/main/asciidoc/chapters/kerberos.txt
index 05d7384ea..dc2484b92 100644
-- a/docs/src/main/asciidoc/chapters/kerberos.txt
++ b/docs/src/main/asciidoc/chapters/kerberos.txt
@@ -73,6 +73,27 @@ password, at the cost of needing to protect the keytab file. These principals
 will apply directly to authentication for clients accessing Accumulo and the
 Accumulo processes accessing HDFS.
 
=== Delegation Tokens

MapReduce, a common way that clients interact with Accumulo, does not map well to the
client-server model that Kerberos was originally designed to support. Specifically, the parallelization
of tasks across many nodes introduces the problem of securely sharing the user credentials across
these tasks in as safe a manner as possible. To address this problem, Hadoop introduced the notion
of a delegation token to be used in distributed execution settings.

A delegation token is nothing more than a short-term, on-the-fly password generated after authenticating with the user's
credentials.  In Hadoop itself, the Namenode and ResourceManager, for HDFS and YARN respectively, act as the gateway for
delegation tokens requests. For example, before a YARN job is submitted, the implementation will request delegation
tokens from the NameNode and ResourceManager so the YARN tasks can communicate with HDFS and YARN. In the same manner,
support has been added in the Accumulo Master to generate delegation tokens to enable interaction with Accumulo via
MapReduce when Kerberos authentication is enabled in a manner similar to HDFS and YARN.

Generating an expiring password is, arguably, more secure than distributing the user's
credentials across the cluster as only access to HDFS, YARN and Accumulo would be
compromised in the case of the token being compromised as opposed to the entire
Kerberos credential. Additional details for clients and servers will be covered
in subsequent sections.

 === Configuring Accumulo
 
 To configure Accumulo for use with Kerberos, both client-facing and server-facing
@@ -149,6 +170,12 @@ serializing traces to the trace table.
 still use a normal KerberosToken and the same keytab/principal to serialize traces. Like
 non-Kerberized instances, the table must be created and permissions granted to the trace.user.
 ** The same +_HOST+ replacement is performed on this value, substituted the FQDN for +_HOST+.
* *general.delegation.token.lifetime*=_7d_
** The length of time that the server-side secret used to create delegation tokens is valid.
   After a server-side secret expires, a delegation token created with that secret is no longer valid.
* *general.delegation.token.update.interval*=_1d_
** The frequency in which new server-side secrets should be generated to create delegation
   tokens for clients. Generating new secrets reduces the likelihood of cryptographic attacks.
 
 Although it should be a prerequisite, it is ever important that you have DNS properly
 configured for your nodes and that Accumulo is configured to use the FQDN. It
@@ -220,6 +247,34 @@ requests from.
 Both the hosts and users configuration properties also accept a value of +*+ to denote that any user or host
 is acceptable for +$PROXY_USER+.
 
===== Delegation Tokens

Within Accumulo services, the primary task to implement delegation tokens is the generation and distribution
of a shared secret among all Accumulo tabletservers and the master. The secret key allows for generation
of delegation tokens for users and verification of delegation tokens presented by clients. If a server
process is unaware of the secret key used to create a delegation token, the client cannot be authenticated.
As ZooKeeper distribution is an asynchronous operation (typically on the order of seconds), the 
value for `general.delegation.token.update.interval` should be on the order of hours to days to reduce the
likelihood of servers rejecting valid clients because the server did not yet see a new secret key.

Supporting authentication with both Kerberos credentials and delegation tokens, the SASL thrift
server accepts connections with either `GSSAPI` and `DIGEST-MD5` mechanisms set. The `DIGEST-MD5` mechanism
enables authentication as a normal username and password exchange which `DelegationToken`s leverages.

Since delegation tokens are a weaker form of authentication than Kerberos credentials, user access
to obtain delegation tokens from Accumulo is protected with the `DELEGATION_TOKEN` system permission. Only
users with the system permission are allowed to obtain delegation tokens. It is also recommended
to configure confidentiality with SASL, using the `rpc.sasl.qop=auth-conf` configuration property, to
ensure that prying eyes cannot view the `DelegationToken` as it passes over the network.

----
# Check a user's permissions
admin@REALM@accumulo> userpermissions -u user@REALM

# Grant the DELEGATION_TOKEN system permission to a user
admin@REALM@accumulo> grant System.DELEGATION_TOKEN -s -u user@REALM
----

 ==== Clients
 
 ===== Create client principal
@@ -265,6 +320,61 @@ Three items need to be set to enable access to Accumulo:
 The second and third properties *must* match the configuration of the accumulo servers; this is
 required to set up the SASL transport.
 
===== DelegationTokens with MapReduce

To use DelegationTokens in a custom MapReduce job, the call to `setConnectorInfo()` method
on `AccumuloInputFormat` or `AccumuloOutputFormat` should be the only necessary change. Instead
of providing an instance of a `KerberosToken`, the user must call `SecurityOperations.getDelegationToken`
using a `Connector` obtained with that `KerberosToken`, and pass the `DelegationToken` to
`setConnectorInfo` instead of the `KerberosToken`. It is expected that the user launching
the MapReduce job is already logged in via Kerberos via a keytab or via a locally-cached
Kerberos ticket-granting-ticket (TGT).

[source,java]
----
Instance instance = getInstance();
KerberosToken kt = new KerberosToken();
Connector conn = instance.getConnector(principal, kt);
DelegationToken dt = conn.securityOperations().getDelegationToken();

// Reading from Accumulo
AccumuloInputFormat.setConnectorInfo(job, principal, dt);

// Writing to Accumulo
AccumuloOutputFormat.setConnectorInfo(job, principal, dt);
----

If the user passes a `KerberosToken` to the `setConnectorInfo` method, the implementation will
attempt to obtain a `DelegationToken` automatically, but this does have limitations
based on the other MapReduce configuration methods already called and permissions granted
to the calling user. It is best for the user to acquire the DelegationToken on their own
and provide it directly to `setConnectorInfo`.

Users must have the `DELEGATION_TOKEN` system permission to call the `getDelegationToken`
method. The obtained delegation token is only valid for the requesting user for a period
of time dependent on Accumulo's configuration (`general.delegation.token.lifetime`).

It is also possible to obtain and use `DelegationToken`s outside of the context
of MapReduce.

[source,java]
----
String principal = "user@REALM";
Instance instance = getInstance();
Connector connector = instance.getConnector(principal, new KerberosToken());
DelegationToken delegationToken = connector.securityOperations().getDelegationToken();

Connector dtConnector = instance.getConnector(principal, delegationToken);
----

Use of the `dtConnector` will perform each operation as the original user, but without
their Kerberos credentials.

For the duration of validity of the `DelegationToken`, the user *must* take the necessary precautions
to protect the `DelegationToken` from prying eyes as it can be used by any user on any host to impersonate
the user who requested the `DelegationToken`. YARN ensures that passing the delegation token from the client
JVM to each YARN task is secure, even in multi-tenant instances.

 ==== Debugging
 
 *Q*: I have valid Kerberos credentials and a correct client configuration file but 
diff --git a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/IZooReader.java b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/IZooReader.java
index 610b1bd22..19235827c 100644
-- a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/IZooReader.java
++ b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/IZooReader.java
@@ -20,6 +20,7 @@ import java.util.List;
 
 import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
 import org.apache.zookeeper.data.Stat;
 
 public interface IZooReader {
@@ -28,6 +29,8 @@ public interface IZooReader {
 
   byte[] getData(String zPath, boolean watch, Stat stat) throws KeeperException, InterruptedException;
 
  byte[] getData(String zPath, Watcher watcher, Stat stat) throws KeeperException, InterruptedException;

   Stat getStatus(String zPath) throws KeeperException, InterruptedException;
 
   Stat getStatus(String zPath, Watcher watcher) throws KeeperException, InterruptedException;
@@ -42,4 +45,5 @@ public interface IZooReader {
 
   void sync(final String path) throws KeeperException, InterruptedException;
 
  List<ACL> getACL(String zPath, Stat stat) throws KeeperException, InterruptedException;
 }
diff --git a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooReader.java b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooReader.java
index 5706cf374..707959cc4 100644
-- a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooReader.java
++ b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooReader.java
@@ -20,12 +20,14 @@ import java.util.List;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 
import org.apache.accumulo.fate.zookeeper.ZooUtil.ZooKeeperConnectionInfo;
 import org.apache.log4j.Logger;
 import org.apache.zookeeper.AsyncCallback.VoidCallback;
 import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.KeeperException.Code;
 import org.apache.zookeeper.Watcher;
 import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
 import org.apache.zookeeper.data.Stat;
 
 public class ZooReader implements IZooReader {
@@ -34,6 +36,7 @@ public class ZooReader implements IZooReader {
   protected String keepers;
   protected int timeout;
   private final RetryFactory retryFactory;
  private final ZooKeeperConnectionInfo info;
 
   protected ZooKeeper getSession(String keepers, int timeout, String scheme, byte[] auth) {
     return ZooSession.getSession(keepers, timeout, scheme, auth);
@@ -82,6 +85,25 @@ public class ZooReader implements IZooReader {
     }
   }
 
  @Override
  public byte[] getData(String zPath, Watcher watcher, Stat stat) throws KeeperException, InterruptedException {
    final Retry retry = getRetryFactory().create();
    while (true) {
      try {
        return getZooKeeper().getData(zPath, watcher, stat);
      } catch (KeeperException e) {
        final Code code = e.code();
        if (code == Code.CONNECTIONLOSS || code == Code.OPERATIONTIMEOUT || code == Code.SESSIONEXPIRED) {
          retryOrThrow(retry, e);
        } else {
          throw e;
        }
      }

      retry.waitForNextAttempt();
    }
  }

   @Override
   public Stat getStatus(String zPath) throws KeeperException, InterruptedException {
     final Retry retry = getRetryFactory().create();
@@ -220,9 +242,15 @@ public class ZooReader implements IZooReader {
     }
   }
 
  @Override
  public List<ACL> getACL(String zPath, Stat stat) throws KeeperException, InterruptedException {
    return ZooUtil.getACL(info, zPath, stat);
  }

   public ZooReader(String keepers, int timeout) {
     this.keepers = keepers;
     this.timeout = timeout;
     this.retryFactory = RetryFactory.DEFAULT_INSTANCE;
    this.info = new ZooKeeperConnectionInfo(keepers, timeout, null, null);
   }
 }
diff --git a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooUtil.java b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooUtil.java
index 805bfffef..abb1aeb01 100644
-- a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooUtil.java
++ b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooUtil.java
@@ -518,4 +518,22 @@ public class ZooUtil {
     }
   }
 
  public static List<ACL> getACL(ZooKeeperConnectionInfo info, String zPath, Stat stat) throws KeeperException, InterruptedException {
    final Retry retry = RETRY_FACTORY.create();
    while (true) {
      try {
        return getZooKeeper(info).getACL(zPath, stat);
      } catch (KeeperException e) {
        final Code c = e.code();
        if (c == Code.CONNECTIONLOSS || c == Code.OPERATIONTIMEOUT || c == Code.SESSIONEXPIRED) {
          retryOrThrow(retry, e);
        } else {
          throw e;
        }
      }

      retry.waitForNextAttempt();
    }
  }

 }
diff --git a/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java b/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java
index f9039be94..e97481c83 100644
-- a/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java
++ b/proxy/src/main/java/org/apache/accumulo/proxy/Proxy.java
@@ -25,15 +25,17 @@ import java.util.Properties;
 
 import org.apache.accumulo.core.cli.Help;
 import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
 import org.apache.accumulo.core.rpc.SslConnectionParams;
 import org.apache.accumulo.minicluster.MiniAccumuloCluster;
 import org.apache.accumulo.proxy.thrift.AccumuloProxy;
 import org.apache.accumulo.server.metrics.MetricsFactory;
 import org.apache.accumulo.server.rpc.RpcWrapper;
import org.apache.accumulo.server.rpc.SaslServerConnectionParams;
 import org.apache.accumulo.server.rpc.ServerAddress;
 import org.apache.accumulo.server.rpc.TServerUtils;
 import org.apache.accumulo.server.rpc.ThriftServerType;
@@ -204,16 +206,15 @@ public class Proxy implements KeywordExecutable {
 
     ClientConfiguration clientConf = ClientConfiguration.loadDefault();
     SslConnectionParams sslParams = null;
    SaslConnectionParams saslParams = null;
    SaslServerConnectionParams saslParams = null;
     switch (serverType) {
       case SSL:
         sslParams = SslConnectionParams.forClient(ClientContext.convertClientConfig(clientConf));
         break;
       case SASL:
        saslParams = SaslConnectionParams.forConfig(clientConf);
        if (null == saslParams) {
        if (!clientConf.getBoolean(ClientProperty.INSTANCE_RPC_SASL_ENABLED.getKey())) {
           log.fatal("SASL thrift server was requested but it is disabled in client configuration");
          throw new RuntimeException();
          throw new RuntimeException("SASL is not enabled in configuration");
         }
 
         // Kerberos needs to be enabled to use it
@@ -233,6 +234,9 @@ public class Proxy implements KeywordExecutable {
         UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
         log.info("Logged in as " + ugi.getUserName());
 
        KerberosToken token = new KerberosToken();
        saslParams = new SaslServerConnectionParams(clientConf, token, null);

         processor = new UGIAssumingProcessor(processor);
 
         break;
diff --git a/server/base/src/main/java/org/apache/accumulo/server/AccumuloServerContext.java b/server/base/src/main/java/org/apache/accumulo/server/AccumuloServerContext.java
index 84c38530a..6a59822bd 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/AccumuloServerContext.java
++ b/server/base/src/main/java/org/apache/accumulo/server/AccumuloServerContext.java
@@ -18,19 +18,26 @@ package org.apache.accumulo.server;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ConnectorImpl;
 import org.apache.accumulo.core.client.mock.MockInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
 import org.apache.accumulo.core.rpc.SslConnectionParams;
 import org.apache.accumulo.core.security.Credentials;
import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.rpc.SaslServerConnectionParams;
 import org.apache.accumulo.server.rpc.ThriftServerType;
 import org.apache.accumulo.server.security.SecurityUtil;
 import org.apache.accumulo.server.security.SystemCredentials;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
 import org.apache.hadoop.security.UserGroupInformation;
 
 import com.google.common.base.Preconditions;
@@ -41,14 +48,23 @@ import com.google.common.base.Preconditions;
 public class AccumuloServerContext extends ClientContext {
 
   private final ServerConfigurationFactory confFactory;
  private AuthenticationTokenSecretManager secretManager;
 
   /**
    * Construct a server context from the server's configuration
    */
   public AccumuloServerContext(ServerConfigurationFactory confFactory) {
    this(confFactory, null);
  }

  /**
   * Construct a server context from the server's configuration
   */
  public AccumuloServerContext(ServerConfigurationFactory confFactory, AuthenticationTokenSecretManager secretManager) {
     super(confFactory.getInstance(), getCredentials(confFactory.getInstance()), confFactory.getConfiguration());
     this.confFactory = confFactory;
    if (null != getServerSaslParams()) {
    this.secretManager = secretManager;
    if (null != getSaslParams()) {
       // Server-side "client" check to make sure we're logged in as a user we expect to be
       enforceKerberosLogin();
     }
@@ -65,7 +81,7 @@ public class AccumuloServerContext extends ClientContext {
     UserGroupInformation loginUser;
     try {
       // The system user should be logged in via keytab when the process is started, not the currentUser() like KerberosToken
      loginUser = UserGroupInformation.getLoginUser();
      loginUser = UserGroupInformation.getCurrentUser();
     } catch (IOException e) {
       throw new RuntimeException("Could not get login user", e);
     }
@@ -99,9 +115,13 @@ public class AccumuloServerContext extends ClientContext {
     return SslConnectionParams.forServer(getConfiguration());
   }
 
  public SaslConnectionParams getServerSaslParams() {
    // Not functionally different than the client SASL params, just uses the site configuration
    return SaslConnectionParams.forConfig(getServerConfigurationFactory().getSiteConfiguration());
  @Override
  public SaslServerConnectionParams getSaslParams() {
    AccumuloConfiguration conf = getServerConfigurationFactory().getSiteConfiguration();
    if (!conf.getBoolean(Property.INSTANCE_RPC_SASL_ENABLED)) {
      return null;
    }
    return new SaslServerConnectionParams(conf, getCredentials().getToken(), secretManager);
   }
 
   /**
@@ -130,4 +150,28 @@ public class AccumuloServerContext extends ClientContext {
     }
   }
 
  public void setSecretManager(AuthenticationTokenSecretManager secretManager) {
    this.secretManager = secretManager;
  }

  public AuthenticationTokenSecretManager getSecretManager() {
    return secretManager;
  }

  // Need to override this from ClientContext to ensure that HdfsZooInstance doesn't "downcast"
  // the AccumuloServerContext into a ClientContext (via the copy-constructor on ClientContext)
  @Override
  public Connector getConnector() throws AccumuloException, AccumuloSecurityException {
    // avoid making more connectors than necessary
    if (conn == null) {
      if (inst instanceof ZooKeeperInstance || inst instanceof HdfsZooInstance) {
        // reuse existing context
        conn = new ConnectorImpl(this);
      } else {
        Credentials c = getCredentials();
        conn = getInstance().getConnector(c.getPrincipal(), c.getToken());
      }
    }
    return conn;
  }
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
index bf56a7a22..7ee6f0ccf 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
@@ -30,7 +30,6 @@ import org.apache.accumulo.core.metadata.schema.MetadataSchema;
 import org.apache.accumulo.server.AccumuloServerContext;
 
 public class MetaDataStateStore extends TabletStateStore {
  // private static final Logger log = Logger.getLogger(MetaDataStateStore.class);
 
   private static final int THREADS = 4;
   private static final int LATENCY = 1000;
diff --git a/server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerConnectionParams.java b/server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerConnectionParams.java
new file mode 100644
index 000000000..dc0b81a33
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerConnectionParams.java
@@ -0,0 +1,69 @@
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
package org.apache.accumulo.server.rpc;

import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
import org.apache.accumulo.server.security.SystemCredentials.SystemToken;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;

/**
 * Server-side SASL connection information
 */
public class SaslServerConnectionParams extends SaslConnectionParams {

  private AuthenticationTokenSecretManager secretManager;

  public SaslServerConnectionParams(AccumuloConfiguration conf, AuthenticationToken token) {
    this(conf, token, null);
  }

  public SaslServerConnectionParams(AccumuloConfiguration conf, AuthenticationToken token, AuthenticationTokenSecretManager secretManager) {
    super(conf, token);
    setSecretManager(secretManager);
  }

  public SaslServerConnectionParams(ClientConfiguration conf, AuthenticationToken token) {
    this(conf, token, null);
  }

  public SaslServerConnectionParams(ClientConfiguration conf, AuthenticationToken token, AuthenticationTokenSecretManager secretManager) {
    super(conf, token);
    setSecretManager(secretManager);
  }

  @Override
  protected void updateFromToken(AuthenticationToken token) {
    // Servers should never have a delegation token -- only a strong kerberos identity
    if (token instanceof KerberosToken || token instanceof SystemToken) {
      mechanism = SaslMechanism.GSSAPI;
    } else {
      throw new IllegalArgumentException("Cannot determine SASL mechanism for token class: " + token.getClass());
    }
  }

  public AuthenticationTokenSecretManager getSecretManager() {
    return secretManager;
  }

  public void setSecretManager(AuthenticationTokenSecretManager secretManager) {
    this.secretManager = secretManager;
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerDigestCallbackHandler.java b/server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerDigestCallbackHandler.java
new file mode 100644
index 000000000..c43f7edbd
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/rpc/SaslServerDigestCallbackHandler.java
@@ -0,0 +1,113 @@
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
package org.apache.accumulo.server.rpc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.apache.accumulo.core.rpc.SaslDigestCallbackHandler;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.SecretManager.InvalidToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CallbackHandler for SASL DIGEST-MD5 mechanism. Modified copy from Hadoop, uses our TokenIdentifier and SecretManager implementations
 */
public class SaslServerDigestCallbackHandler extends SaslDigestCallbackHandler {
  private static final Logger log = LoggerFactory.getLogger(SaslServerDigestCallbackHandler.class);
  private static final String NAME = SaslServerDigestCallbackHandler.class.getSimpleName();

  private AuthenticationTokenSecretManager secretManager;

  public SaslServerDigestCallbackHandler(AuthenticationTokenSecretManager secretManager) {
    this.secretManager = secretManager;
  }

  private AuthenticationTokenIdentifier getIdentifier(String id, AuthenticationTokenSecretManager secretManager) throws InvalidToken {
    byte[] tokenId = decodeIdentifier(id);
    AuthenticationTokenIdentifier tokenIdentifier = secretManager.createIdentifier();
    try {
      tokenIdentifier.readFields(new DataInputStream(new ByteArrayInputStream(tokenId)));
    } catch (IOException e) {
      throw (InvalidToken) new InvalidToken("Can't de-serialize tokenIdentifier").initCause(e);
    }
    return tokenIdentifier;
  }

  @Override
  public void handle(Callback[] callbacks) throws InvalidToken, UnsupportedCallbackException {
    NameCallback nc = null;
    PasswordCallback pc = null;
    AuthorizeCallback ac = null;
    for (Callback callback : callbacks) {
      if (callback instanceof AuthorizeCallback) {
        ac = (AuthorizeCallback) callback;
      } else if (callback instanceof NameCallback) {
        nc = (NameCallback) callback;
      } else if (callback instanceof PasswordCallback) {
        pc = (PasswordCallback) callback;
      } else if (callback instanceof RealmCallback) {
        continue; // realm is ignored
      } else {
        throw new UnsupportedCallbackException(callback, "Unrecognized SASL DIGEST-MD5 Callback");
      }
    }

    if (pc != null) {
      AuthenticationTokenIdentifier tokenIdentifier = getIdentifier(nc.getDefaultName(), secretManager);
      char[] password = getPassword(secretManager, tokenIdentifier);
      UserGroupInformation user = null;
      user = tokenIdentifier.getUser();

      // Set the principal since we already deserialized the token identifier
      UGIAssumingProcessor.getRpcPrincipalThreadLocal().set(user.getUserName());

      log.trace("SASL server DIGEST-MD5 callback: setting password for client: {}", tokenIdentifier.getUser());
      pc.setPassword(password);
    }
    if (ac != null) {
      String authid = ac.getAuthenticationID();
      String authzid = ac.getAuthorizationID();
      if (authid.equals(authzid)) {
        ac.setAuthorized(true);
      } else {
        ac.setAuthorized(false);
      }
      if (ac.isAuthorized()) {
        String username = getIdentifier(authzid, secretManager).getUser().getUserName();
        log.trace("SASL server DIGEST-MD5 callback: setting canonicalized client ID: {}", username);
        ac.setAuthorizedID(authzid);
      }
    }
  }

  @Override
  public String toString() {
    return NAME;
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/rpc/TCredentialsUpdatingInvocationHandler.java b/server/base/src/main/java/org/apache/accumulo/server/rpc/TCredentialsUpdatingInvocationHandler.java
index f85505d2e..150f0d35d 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/rpc/TCredentialsUpdatingInvocationHandler.java
++ b/server/base/src/main/java/org/apache/accumulo/server/rpc/TCredentialsUpdatingInvocationHandler.java
@@ -24,8 +24,10 @@ import java.util.concurrent.ConcurrentHashMap;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.rpc.SaslConnectionParams.SaslMechanism;
 import org.apache.accumulo.core.security.thrift.TCredentials;
 import org.apache.accumulo.server.security.SystemCredentials.SystemToken;
 import org.apache.accumulo.server.security.UserImpersonation;
@@ -81,6 +83,19 @@ public class TCredentialsUpdatingInvocationHandler<I> implements InvocationHandl
     }
 
     Class<? extends AuthenticationToken> tokenClass = getTokenClassFromName(tcreds.tokenClassName);

    // The Accumulo principal extracted from the SASL transport
    final String principal = UGIAssumingProcessor.rpcPrincipal();

    // If we authenticated the user over DIGEST-MD5 and they have a DelegationToken, the principals should match
    if (SaslMechanism.DIGEST_MD5 == UGIAssumingProcessor.rpcMechanism() && DelegationToken.class.isAssignableFrom(tokenClass)) {
      if (!principal.equals(tcreds.principal)) {
        log.warn("{} issued RPC with delegation token over DIGEST-MD5 as the Accumulo principal {}. Disallowing RPC", principal, tcreds.principal);
        throw new ThriftSecurityException("RPC principal did not match provided Accumulo principal", SecurityErrorCode.BAD_CREDENTIALS);
      }
      return;
    }

     // If the authentication token isn't a KerberosToken
     if (!KerberosToken.class.isAssignableFrom(tokenClass) && !SystemToken.class.isAssignableFrom(tokenClass)) {
       // Don't include messages about SystemToken since it's internal
@@ -88,9 +103,6 @@ public class TCredentialsUpdatingInvocationHandler<I> implements InvocationHandl
       throw new ThriftSecurityException("Did not receive a valid token", SecurityErrorCode.BAD_CREDENTIALS);
     }
 
    // The Accumulo principal extracted from the SASL transport
    final String principal = UGIAssumingProcessor.rpcPrincipal();

     if (null == principal) {
       log.debug("Found KerberosToken in TCredentials, but did not receive principal from SASL processor");
       throw new ThriftSecurityException("Did not extract principal from Thrift SASL processor", SecurityErrorCode.BAD_CREDENTIALS);
diff --git a/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java b/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java
index f1f896383..558b02e19 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java
++ b/server/base/src/main/java/org/apache/accumulo/server/rpc/TServerUtils.java
@@ -34,7 +34,6 @@ import javax.net.ssl.SSLServerSocket;
 
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
 import org.apache.accumulo.core.rpc.SslConnectionParams;
 import org.apache.accumulo.core.rpc.ThriftUtil;
 import org.apache.accumulo.core.rpc.UGIAssumingTransportFactory;
@@ -150,7 +149,7 @@ public class TServerUtils {
         try {
           HostAndPort addr = HostAndPort.fromParts(hostname, port);
           return TServerUtils.startTServer(addr, serverType, timedProcessor, serverName, threadName, minThreads, simpleTimerThreadpoolSize,
              timeBetweenThreadChecks, maxMessageSize, service.getServerSslParams(), service.getServerSaslParams(), service.getClientTimeoutInMillis());
              timeBetweenThreadChecks, maxMessageSize, service.getServerSslParams(), service.getSaslParams(), service.getClientTimeoutInMillis());
         } catch (TTransportException ex) {
           log.error("Unable to start TServer", ex);
           if (ex.getCause() == null || ex.getCause().getClass() == BindException.class) {
@@ -380,7 +379,7 @@ public class TServerUtils {
   }
 
   public static ServerAddress createSaslThreadPoolServer(HostAndPort address, TProcessor processor, TProtocolFactory protocolFactory, long socketTimeout,
      SaslConnectionParams params, final String serverName, String threadName, final int numThreads, final int numSTThreads, long timeBetweenThreadChecks)
      SaslServerConnectionParams params, final String serverName, String threadName, final int numThreads, final int numSTThreads, long timeBetweenThreadChecks)
       throws TTransportException {
     // We'd really prefer to use THsHaServer (or similar) to avoid 1 RPC == 1 Thread that the TThreadPoolServer does,
     // but sadly this isn't the case. Because TSaslTransport needs to issue a handshake when it open()'s which will fail
@@ -388,7 +387,7 @@ public class TServerUtils {
     log.info("Creating SASL thread pool thrift server on listening on {}:{}", address.getHostText(), address.getPort());
     TServerSocket transport = new TServerSocket(address.getPort(), (int) socketTimeout);
 
    final String hostname, fqdn;
    String hostname, fqdn;
     try {
       hostname = InetAddress.getByName(address.getHostText()).getCanonicalHostName();
       fqdn = InetAddress.getLocalHost().getCanonicalHostName();
@@ -396,10 +395,15 @@ public class TServerUtils {
       throw new TTransportException(e);
     }
 
    // If we can't get a real hostname from the provided host test, use the hostname from DNS for localhost
    if ("0.0.0.0".equals(hostname)) {
      hostname = fqdn;
    }

     // ACCUMULO-3497 an easy sanity check we can perform for the user when SASL is enabled. Clients and servers have to agree upon the FQDN
     // so that the SASL handshake can occur. If the provided hostname doesn't match the FQDN for this host, fail quickly and inform them to update
     // their configuration.
    if (!"0.0.0.0".equals(hostname) && !hostname.equals(fqdn)) {
    if (!hostname.equals(fqdn)) {
       log.error(
           "Expected hostname of '{}' but got '{}'. Ensure the entries in the Accumulo hosts files (e.g. masters, slaves) are the FQDN for each host when using SASL.",
           fqdn, hostname);
@@ -413,7 +417,7 @@ public class TServerUtils {
       throw new TTransportException(e);
     }
 
    log.trace("Logged in as {}, creating TSsaslServerTransport factory as {}/{}", serverUser, params.getKerberosServerPrimary(), hostname);
    log.debug("Logged in as {}, creating TSaslServerTransport factory with {}/{}", serverUser, params.getKerberosServerPrimary(), hostname);
 
     // Make the SASL transport factory with the instance and primary from the kerberos server principal, SASL properties
     // and the SASL callback handler from Hadoop to ensure authorization ID is the authentication ID. Despite the 'protocol' argument seeming to be useless, it
@@ -422,6 +426,14 @@ public class TServerUtils {
     saslTransportFactory.addServerDefinition(ThriftUtil.GSSAPI, params.getKerberosServerPrimary(), hostname, params.getSaslProperties(),
         new SaslRpcServer.SaslGssCallbackHandler());
 
    if (null != params.getSecretManager()) {
      log.info("Adding DIGEST-MD5 server definition for delegation tokens");
      saslTransportFactory.addServerDefinition(ThriftUtil.DIGEST_MD5, params.getKerberosServerPrimary(), hostname, params.getSaslProperties(),
          new SaslServerDigestCallbackHandler(params.getSecretManager()));
    } else {
      log.info("SecretManager is null, not adding support for delegation token authentication");
    }

     // Make sure the TTransportFactory is performing a UGI.doAs
     TTransportFactory ugiTransportFactory = new UGIAssumingTransportFactory(saslTransportFactory, serverUser);
 
@@ -440,7 +452,7 @@ public class TServerUtils {
 
   public static ServerAddress startTServer(AccumuloConfiguration conf, HostAndPort address, ThriftServerType serverType, TProcessor processor,
       String serverName, String threadName, int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
      SaslServerConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
 
     if (ThriftServerType.SASL == serverType) {
       processor = updateSaslProcessor(serverType, processor);
@@ -452,11 +464,11 @@ public class TServerUtils {
 
   /**
    * @see #startTServer(HostAndPort, ThriftServerType, TimedProcessor, TProtocolFactory, String, String, int, int, long, long, SslConnectionParams,
   *      SaslConnectionParams, long)
   *      org.apache.accumulo.core.rpc.SaslConnectionParams, long)
    */
   public static ServerAddress startTServer(HostAndPort address, ThriftServerType serverType, TimedProcessor processor, String serverName, String threadName,
      int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams, SaslConnectionParams saslParams,
      long serverSocketTimeout) throws TTransportException {
      int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslServerConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
     return startTServer(address, serverType, processor, ThriftUtil.protocolFactory(), serverName, threadName, numThreads, numSTThreads,
         timeBetweenThreadChecks, maxMessageSize, sslParams, saslParams, serverSocketTimeout);
   }
@@ -468,7 +480,7 @@ public class TServerUtils {
    */
   public static ServerAddress startTServer(HostAndPort address, ThriftServerType serverType, TimedProcessor processor, TProtocolFactory protocolFactory,
       String serverName, String threadName, int numThreads, int numSTThreads, long timeBetweenThreadChecks, long maxMessageSize, SslConnectionParams sslParams,
      SaslConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
      SaslServerConnectionParams saslParams, long serverSocketTimeout) throws TTransportException {
 
     // This is presently not supported. It's hypothetically possible, I believe, to work, but it would require changes in how the transports
     // work at the Thrift layer to ensure that both the SSL and SASL handshakes function. SASL's quality of protection addresses privacy issues.
diff --git a/server/base/src/main/java/org/apache/accumulo/server/rpc/UGIAssumingProcessor.java b/server/base/src/main/java/org/apache/accumulo/server/rpc/UGIAssumingProcessor.java
index ab106a6a3..48d18f4bc 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/rpc/UGIAssumingProcessor.java
++ b/server/base/src/main/java/org/apache/accumulo/server/rpc/UGIAssumingProcessor.java
@@ -20,6 +20,7 @@ import java.io.IOException;
 
 import javax.security.sasl.SaslServer;
 
import org.apache.accumulo.core.rpc.SaslConnectionParams.SaslMechanism;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.apache.thrift.TException;
 import org.apache.thrift.TProcessor;
@@ -40,6 +41,8 @@ public class UGIAssumingProcessor implements TProcessor {
   private static final Logger log = LoggerFactory.getLogger(UGIAssumingProcessor.class);
 
   public static final ThreadLocal<String> rpcPrincipal = new ThreadLocal<String>();
  public static final ThreadLocal<SaslMechanism> rpcMechanism = new ThreadLocal<SaslMechanism>();

   private final TProcessor wrapped;
   private final UserGroupInformation loginUser;
 
@@ -60,6 +63,14 @@ public class UGIAssumingProcessor implements TProcessor {
     return rpcPrincipal.get();
   }
 
  public static ThreadLocal<String> getRpcPrincipalThreadLocal() {
    return rpcPrincipal;
  }

  public static SaslMechanism rpcMechanism() {
    return rpcMechanism.get();
  }

   @Override
   public boolean process(final TProtocol inProt, final TProtocol outProt) throws TException {
     TTransport trans = inProt.getTransport();
@@ -71,20 +82,42 @@ public class UGIAssumingProcessor implements TProcessor {
     String authId = saslServer.getAuthorizationID();
     String endUser = authId;
 
    log.trace("Received SASL RPC from {}", endUser);
    SaslMechanism mechanism;
    try {
      mechanism = SaslMechanism.get(saslServer.getMechanismName());
    } catch (Exception e) {
      log.error("Failed to process RPC with SASL mechanism {}", saslServer.getMechanismName());
      throw e;
    }
 
    UserGroupInformation clientUgi = UserGroupInformation.createProxyUser(endUser, loginUser);
    final String remoteUser = clientUgi.getUserName();
    switch (mechanism) {
      case GSSAPI:
        UserGroupInformation clientUgi = UserGroupInformation.createProxyUser(endUser, loginUser);
        final String remoteUser = clientUgi.getUserName();
 
    try {
      // Set the principal in the ThreadLocal for access to get authorizations
      rpcPrincipal.set(remoteUser);
        try {
          // Set the principal in the ThreadLocal for access to get authorizations
          rpcPrincipal.set(remoteUser);
 
      return wrapped.process(inProt, outProt);
    } finally {
      // Unset the principal after we're done using it just to be sure that it's not incorrectly
      // used in the same thread down the line.
      rpcPrincipal.set(null);
          return wrapped.process(inProt, outProt);
        } finally {
          // Unset the principal after we're done using it just to be sure that it's not incorrectly
          // used in the same thread down the line.
          rpcPrincipal.set(null);
        }
      case DIGEST_MD5:
        // The CallbackHandler, after deserializing the TokenIdentifier in the name, has already updated
        // the rpcPrincipal for us. We don't need to do it again here.
        try {
          rpcMechanism.set(mechanism);
          return wrapped.process(inProt, outProt);
        } finally {
          // Unset the mechanism after we're done using it just to be sure that it's not incorrectly
          // used in the same thread down the line.
          rpcMechanism.set(null);
        }
      default:
        throw new IllegalArgumentException("Cannot process SASL mechanism " + mechanism);
     }
   }
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java b/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
index cc7a7cdc7..283cba3e8 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
++ b/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
@@ -454,4 +454,18 @@ public class AuditedSecurityOperation extends SecurityOperation {
       throw e;
     }
   }

  public static final String DELEGATION_TOKEN_AUDIT_TEMPLATE = "requested delegation token";

  @Override
  public boolean canObtainDelegationToken(TCredentials credentials) throws ThriftSecurityException {
    try {
      boolean result = super.canObtainDelegationToken(credentials);
      audit(credentials, result, DELEGATION_TOKEN_AUDIT_TEMPLATE);
      return result;
    } catch (ThriftSecurityException e) {
      audit(credentials, false, DELEGATION_TOKEN_AUDIT_TEMPLATE);
      throw e;
    }
  }
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java b/server/base/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
index 7adb46ec8..0b0f212f1 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
++ b/server/base/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
@@ -830,4 +830,8 @@ public class SecurityOperation {
     return hasSystemPermissionWithNamespaceId(credentials, SystemPermission.ALTER_NAMESPACE, namespaceId, false);
   }
 
  public boolean canObtainDelegationToken(TCredentials credentials) throws ThriftSecurityException {
    authenticate(credentials);
    return hasSystemPermission(credentials, SystemPermission.OBTAIN_DELEGATION_TOKEN, false);
  }
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/SystemCredentials.java b/server/base/src/main/java/org/apache/accumulo/server/security/SystemCredentials.java
index 51d50a1f9..6a915c6e2 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/security/SystemCredentials.java
++ b/server/base/src/main/java/org/apache/accumulo/server/security/SystemCredentials.java
@@ -33,7 +33,6 @@ import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
 import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.core.security.thrift.TCredentials;
 import org.apache.accumulo.core.util.Base64;
@@ -69,8 +68,7 @@ public final class SystemCredentials extends Credentials {
     check_permission();
     String principal = SYSTEM_PRINCIPAL;
     AccumuloConfiguration conf = SiteConfiguration.getInstance();
    SaslConnectionParams saslParams = SaslConnectionParams.forConfig(conf);
    if (null != saslParams) {
    if (conf.getBoolean(Property.INSTANCE_RPC_SASL_ENABLED)) {
       // Use the server's kerberos principal as the Accumulo principal. We could also unwrap the principal server-side, but the principal for SystemCredentials
       // isnt' actually used anywhere, so it really doesn't matter. We can't include the kerberos principal in the SystemToken as it would break equality when
       // different Accumulo servers are using different kerberos principals are their accumulo principal
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationKey.java b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationKey.java
new file mode 100644
index 000000000..134502a20
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationKey.java
@@ -0,0 +1,150 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.accumulo.server.security.delegation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.crypto.SecretKey;

import org.apache.accumulo.core.security.thrift.TAuthenticationKey;
import org.apache.accumulo.core.util.ThriftMessageUtil;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

/**
 * Represents a secret key used for signing and verifying authentication tokens by {@link AuthenticationTokenSecretManager}.
 */
public class AuthenticationKey implements Writable {
  private TAuthenticationKey authKey;
  private SecretKey secret;

  public AuthenticationKey() {
    // for Writable
  }

  public AuthenticationKey(int keyId, long creationDate, long expirationDate, SecretKey key) {
    checkNotNull(key);
    authKey = new TAuthenticationKey(ByteBuffer.wrap(key.getEncoded()));
    authKey.setCreationDate(creationDate);
    authKey.setKeyId(keyId);
    authKey.setExpirationDate(expirationDate);
    this.secret = key;
  }

  public int getKeyId() {
    checkNotNull(authKey);
    return authKey.getKeyId();
  }

  public long getCreationDate() {
    checkNotNull(authKey);
    return authKey.getCreationDate();
  }

  public void setCreationDate(long creationDate) {
    checkNotNull(authKey);
    authKey.setCreationDate(creationDate);
  }

  public long getExpirationDate() {
    checkNotNull(authKey);
    return authKey.getExpirationDate();
  }

  public void setExpirationDate(long expirationDate) {
    checkNotNull(authKey);
    authKey.setExpirationDate(expirationDate);
  }

  SecretKey getKey() {
    return secret;
  }

  void setKey(SecretKey secret) {
    this.secret = secret;
  }

  @Override
  public int hashCode() {
    if (null == authKey) {
      return 1;
    }
    HashCodeBuilder hcb = new HashCodeBuilder(29, 31);
    hcb.append(authKey.getKeyId()).append(authKey.getExpirationDate()).append(authKey.getCreationDate()).append(secret.getEncoded());
    return hcb.toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof AuthenticationKey)) {
      return false;
    }
    AuthenticationKey other = (AuthenticationKey) obj;
    // authKey might be null due to writable nature
    if (null == authKey && null != other.authKey) {
      return false;
    }
    return authKey.equals(other.authKey);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("AuthenticationKey[");
    if (null == authKey) {
      buf.append("null]");
    } else {
      buf.append("id=").append(authKey.getKeyId()).append(", expiration=").append(authKey.getExpirationDate()).append(", creation=")
          .append(authKey.getCreationDate()).append("]");
    }
    return buf.toString();
  }

  @Override
  public void write(DataOutput out) throws IOException {
    if (null == authKey) {
      WritableUtils.writeVInt(out, 0);
      return;
    }
    ThriftMessageUtil util = new ThriftMessageUtil();
    ByteBuffer serialized = util.serialize(authKey);
    WritableUtils.writeVInt(out, serialized.limit() - serialized.arrayOffset());
    out.write(serialized.array(), serialized.arrayOffset(), serialized.limit());
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    int length = WritableUtils.readVInt(in);
    if (0 == length) {
      return;
    }

    ThriftMessageUtil util = new ThriftMessageUtil();
    byte[] bytes = new byte[length];
    in.readFully(bytes);
    authKey = util.deserialize(bytes, new TAuthenticationKey());
    secret = AuthenticationTokenSecretManager.createSecretKey(authKey.getSecret());
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManager.java b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManager.java
new file mode 100644
index 000000000..3582cfd74
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManager.java
@@ -0,0 +1,169 @@
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
package org.apache.accumulo.server.security.delegation;

import java.util.List;

import org.apache.accumulo.core.util.Daemon;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Service that handles generation of the secret key used to create delegation tokens.
 */
public class AuthenticationTokenKeyManager extends Daemon {
  private static final Logger log = LoggerFactory.getLogger(AuthenticationTokenKeyManager.class);

  private final AuthenticationTokenSecretManager secretManager;
  private final ZooAuthenticationKeyDistributor keyDistributor;

  private long lastKeyUpdate = 0;
  private long keyUpdateInterval;
  private long tokenMaxLifetime;
  private int idSeq = 0;
  private volatile boolean keepRunning = true, initialized = false;

  /**
   * Construct the key manager which will generate new AuthenticationKeys to generate and verify delegation tokens
   *
   * @param mgr
   *          The SecretManager in use
   * @param dist
   *          The implementation to distribute AuthenticationKeys to ZooKeeper
   * @param keyUpdateInterval
   *          The frequency, in milliseconds, that new AuthenticationKeys are created
   * @param tokenMaxLifetime
   *          The lifetime, in milliseconds, of generated AuthenticationKeys (and subsequently delegation tokens).
   */
  public AuthenticationTokenKeyManager(AuthenticationTokenSecretManager mgr, ZooAuthenticationKeyDistributor dist, long keyUpdateInterval,
      long tokenMaxLifetime) {
    super("Delegation Token Key Manager");
    this.secretManager = mgr;
    this.keyDistributor = dist;
    this.keyUpdateInterval = keyUpdateInterval;
    this.tokenMaxLifetime = tokenMaxLifetime;
  }

  @VisibleForTesting
  void setKeepRunning(boolean keepRunning) {
    this.keepRunning = keepRunning;
  }

  public boolean isInitialized() {
    return initialized;
  }

  public void gracefulStop() {
    keepRunning = false;
  }

  @Override
  public void run() {
    // Make sure to initialize the secret manager with keys already in ZK
    updateStateFromCurrentKeys();
    initialized = true;

    while (keepRunning) {
      long now = System.currentTimeMillis();

      _run(now);

      try {
        Thread.sleep(5000);
      } catch (InterruptedException ie) {
        log.debug("Interrupted waiting for next update", ie);
      }
    }
  }

  @VisibleForTesting
  void updateStateFromCurrentKeys() {
    try {
      List<AuthenticationKey> currentKeys = keyDistributor.getCurrentKeys();
      if (!currentKeys.isEmpty()) {
        for (AuthenticationKey key : currentKeys) {
          // Ensure that we don't create new Keys with duplicate keyIds for keys that already exist
          // It's not a big concern if we happen to duplicate keyIds for already expired keys.
          if (key.getKeyId() > idSeq) {
            idSeq = key.getKeyId();
          }
          secretManager.addKey(key);
        }
        log.info("Added {} existing AuthenticationKeys into the local cache from ZooKeeper", currentKeys.size());

        // Try to use the last key instead of creating a new one right away. This will present more expected
        // functionality if the active master happens to die for some reasonn
        AuthenticationKey currentKey = secretManager.getCurrentKey();
        if (null != currentKey) {
          log.info("Updating last key update to {} from current secret manager key", currentKey.getCreationDate());
          lastKeyUpdate = currentKey.getCreationDate();
        }
      }
    } catch (KeeperException | InterruptedException e) {
      log.warn("Failed to fetch existing AuthenticationKeys from ZooKeeper");
    }
  }

  @VisibleForTesting
  long getLastKeyUpdate() {
    return lastKeyUpdate;
  }

  @VisibleForTesting
  int getIdSeq() {
    return idSeq;
  }

  /**
   * Internal "run" method which performs the actual work.
   *
   * @param now
   *          The current time in millis since epoch.
   */
  void _run(long now) {
    // clear any expired keys
    int removedKeys = secretManager.removeExpiredKeys(keyDistributor);
    if (removedKeys > 0) {
      log.debug("Removed {} expired keys from the local cache", removedKeys);
    }

    if (lastKeyUpdate + keyUpdateInterval < now) {
      log.debug("Key update interval passed, creating new authentication key");

      // Increment the idSeq and use the new value as the unique ID
      AuthenticationKey newKey = new AuthenticationKey(++idSeq, now, now + tokenMaxLifetime, secretManager.generateSecret());

      log.debug("Created new {}", newKey.toString());

      // Will set to be the current key given the idSeq
      secretManager.addKey(newKey);

      // advertise it to tabletservers
      try {
        keyDistributor.advertise(newKey);
      } catch (KeeperException | InterruptedException e) {
        log.error("Failed to advertise AuthenticationKey in ZooKeeper. Exiting.", e);
        throw new RuntimeException(e);
      }

      lastKeyUpdate = now;
    }
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManager.java b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManager.java
new file mode 100644
index 000000000..99173d237
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManager.java
@@ -0,0 +1,269 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.accumulo.server.security.delegation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.token.SecretManager;
import org.apache.hadoop.security.token.Token;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

/**
 * Manages an internal list of secret keys used to sign new authentication tokens as they are generated, and to validate existing tokens used for
 * authentication.
 *
 * Each TabletServer, in addition to the Master, has an instance of this {@link SecretManager} so that each can authenticate requests from clients presenting
 * delegation tokens. The Master will also run an instance of {@link AuthenticationTokenKeyManager} which handles generation of new keys and removal of old
 * keys. That class will call the methods here to ensure the in-memory cache is consistent with what is advertised in ZooKeeper.
 */
public class AuthenticationTokenSecretManager extends SecretManager<AuthenticationTokenIdentifier> {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationTokenSecretManager.class);

  private final Instance instance;
  private final long tokenMaxLifetime;
  private final ConcurrentHashMap<Integer,AuthenticationKey> allKeys = new ConcurrentHashMap<Integer,AuthenticationKey>();
  private AuthenticationKey currentKey;

  /**
   * Create a new secret manager instance for generating keys.
   *
   * @param instance
   *          Accumulo instance
   * @param tokenMaxLifetime
   *          Maximum age (in milliseconds) before a token expires and is no longer valid
   */
  public AuthenticationTokenSecretManager(Instance instance, long tokenMaxLifetime) {
    checkNotNull(instance);
    checkArgument(tokenMaxLifetime > 0, "Max lifetime must be positive");
    this.instance = instance;
    this.tokenMaxLifetime = tokenMaxLifetime;
  }

  @Override
  protected byte[] createPassword(AuthenticationTokenIdentifier identifier) {
    DelegationTokenConfig cfg = identifier.getConfig();

    long now = System.currentTimeMillis();
    final AuthenticationKey secretKey = currentKey;
    identifier.setKeyId(secretKey.getKeyId());
    identifier.setIssueDate(now);
    long expiration = now + tokenMaxLifetime;
    // Catch overflow
    if (expiration < now) {
      expiration = Long.MAX_VALUE;
    }
    identifier.setExpirationDate(expiration);

    // Limit the lifetime if the user requests it
    if (null != cfg) {
      long requestedLifetime = cfg.getTokenLifetime(TimeUnit.MILLISECONDS);
      if (0 < requestedLifetime) {
        long requestedExpirationDate = identifier.getIssueDate() + requestedLifetime;
        // Catch overflow again
        if (requestedExpirationDate < identifier.getIssueDate()) {
          requestedExpirationDate = Long.MAX_VALUE;
        }
        // Ensure that the user doesn't try to extend the expiration date -- they may only limit it
        if (requestedExpirationDate > identifier.getExpirationDate()) {
          throw new RuntimeException("Requested token lifetime exceeds configured maximum");
        }
        log.trace("Overriding token expiration date from {} to {}", identifier.getExpirationDate(), requestedExpirationDate);
        identifier.setExpirationDate(requestedExpirationDate);
      }
    }

    identifier.setInstanceId(instance.getInstanceID());
    return createPassword(identifier.getBytes(), secretKey.getKey());
  }

  @Override
  public byte[] retrievePassword(AuthenticationTokenIdentifier identifier) throws InvalidToken {
    long now = System.currentTimeMillis();
    if (identifier.getExpirationDate() < now) {
      throw new InvalidToken("Token has expired");
    }
    if (identifier.getIssueDate() > now) {
      throw new InvalidToken("Token issued in the future");
    }
    AuthenticationKey masterKey = allKeys.get(identifier.getKeyId());
    if (masterKey == null) {
      throw new InvalidToken("Unknown master key for token (id=" + identifier.getKeyId() + ")");
    }
    // regenerate the password
    return createPassword(identifier.getBytes(), masterKey.getKey());
  }

  @Override
  public AuthenticationTokenIdentifier createIdentifier() {
    // Return our TokenIdentifier implementation
    return new AuthenticationTokenIdentifier();
  }

  /**
   * Generates a delegation token for the user with the provided {@code username}.
   *
   * @param username
   *          The client to generate the delegation token for.
   * @param cfg
   *          A configuration object for obtaining the delegation token
   * @return A delegation token for {@code username} created using the {@link #currentKey}.
   */
  public Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> generateToken(String username, DelegationTokenConfig cfg)
      throws AccumuloException {
    checkNotNull(username);
    checkNotNull(cfg);

    final AuthenticationTokenIdentifier id = new AuthenticationTokenIdentifier(username, cfg);

    final StringBuilder svcName = new StringBuilder(DelegationToken.SERVICE_NAME);
    if (null != id.getInstanceId()) {
      svcName.append("-").append(id.getInstanceId());
    }
    // Create password will update the state on the identifier given currentKey. Need to call this before serializing the identifier
    byte[] password;
    try {
      password = createPassword(id);
    } catch (RuntimeException e) {
      throw new AccumuloException(e.getMessage());
    }
    // The use of the ServiceLoader inside Token doesn't work to automatically get the Identifier
    // Explicitly returning the identifier also saves an extra deserialization
    Token<AuthenticationTokenIdentifier> token = new Token<AuthenticationTokenIdentifier>(id.getBytes(), password, id.getKind(), new Text(svcName.toString()));
    return Maps.immutableEntry(token, id);
  }

  /**
   * Add the provided {@code key} to the in-memory copy of all {@link AuthenticationKey}s.
   *
   * @param key
   *          The key to add.
   */
  public synchronized void addKey(AuthenticationKey key) {
    checkNotNull(key);

    log.debug("Adding AuthenticationKey with keyId {}", key.getKeyId());

    allKeys.put(key.getKeyId(), key);
    if (currentKey == null || key.getKeyId() > currentKey.getKeyId()) {
      currentKey = key;
    }
  }

  /**
   * Removes the {@link AuthenticationKey} from the local cache of keys using the provided {@link keyId}.
   *
   * @param keyId
   *          The unique ID for the {@link AuthenticationKey} to remove.
   * @return True if the key was removed, otherwise false.
   */
  synchronized boolean removeKey(Integer keyId) {
    checkNotNull(keyId);

    log.debug("Removing AuthenticatioKey with keyId {}", keyId);

    return null != allKeys.remove(keyId);
  }

  /**
   * The current {@link AuthenticationKey}, may be null.
   *
   * @return The current key, or null.
   */
  @VisibleForTesting
  AuthenticationKey getCurrentKey() {
    return currentKey;
  }

  @VisibleForTesting
  Map<Integer,AuthenticationKey> getKeys() {
    return allKeys;
  }

  /**
   * Inspect each key cached in {@link #allKeys} and remove it if the expiration date has passed. For each removed local {@link AuthenticationKey}, the key is
   * also removed from ZooKeeper using the provided {@code keyDistributor} instance.
   *
   * @param keyDistributor
   *          ZooKeeper key distribution class
   */
  synchronized int removeExpiredKeys(ZooAuthenticationKeyDistributor keyDistributor) {
    long now = System.currentTimeMillis();
    int keysRemoved = 0;
    Iterator<Entry<Integer,AuthenticationKey>> iter = allKeys.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<Integer,AuthenticationKey> entry = iter.next();
      AuthenticationKey key = entry.getValue();
      if (key.getExpirationDate() < now) {
        log.debug("Removing expired delegation token key {}", key.getKeyId());
        iter.remove();
        keysRemoved++;
        try {
          keyDistributor.remove(key);
        } catch (KeeperException | InterruptedException e) {
          log.error("Failed to remove AuthenticationKey from ZooKeeper. Exiting", e);
          throw new RuntimeException(e);
        }
      }
    }
    return keysRemoved;
  }

  synchronized boolean isCurrentKeySet() {
    return null != currentKey;
  }

  /**
   * Atomic operation to remove all AuthenticationKeys
   */
  public synchronized void removeAllKeys() {
    allKeys.clear();
    currentKey = null;
  }

  @Override
  protected SecretKey generateSecret() {
    // Method in the parent is a different package, provide the explicit override so we can use it directly in our package.
    return super.generateSecret();
  }

  public static SecretKey createSecretKey(byte[] raw) {
    return SecretManager.createSecretKey(raw);
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributor.java b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributor.java
new file mode 100644
index 000000000..515b03673
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributor.java
@@ -0,0 +1,187 @@
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
package org.apache.accumulo.server.security.delegation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.accumulo.fate.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class that manages distribution of {@link AuthenticationKey}s, Accumulo's secret in the delegation token model, to other Accumulo nodes via ZooKeeper.
 */
public class ZooAuthenticationKeyDistributor {
  private static final Logger log = LoggerFactory.getLogger(ZooAuthenticationKeyDistributor.class);

  private final ZooReaderWriter zk;
  private final String baseNode;
  private boolean initialized = false;

  public ZooAuthenticationKeyDistributor(ZooReaderWriter zk, String baseNode) {
    checkNotNull(zk);
    checkNotNull(baseNode);
    this.zk = zk;
    this.baseNode = baseNode;
  }

  /**
   * Ensures that ZooKeeper is in a correct state to perform distribution of {@link AuthenticationKey}s.
   */
  public synchronized void initialize() throws KeeperException, InterruptedException {
    if (initialized) {
      return;
    }

    if (!zk.exists(baseNode)) {
      if (!zk.putPrivatePersistentData(baseNode, new byte[0], NodeExistsPolicy.FAIL)) {
        throw new AssertionError("Got false from putPrivatePersistentData method");
      }
    } else {
      List<ACL> acls = zk.getACL(baseNode, new Stat());
      if (1 == acls.size()) {
        ACL actualAcl = acls.get(0), expectedAcl = ZooUtil.PRIVATE.get(0);
        Id actualId = actualAcl.getId();
        // The expected outcome from ZooUtil.PRIVATE
        if (actualAcl.getPerms() == expectedAcl.getPerms() && actualId.getScheme().equals("digest") && actualId.getId().startsWith("accumulo:")) {
          initialized = true;
          return;
        }
      } else {
        log.error("Saw more than one ACL on the node");
      }

      log.error("Expected {} to have ACLs {} but was {}", baseNode, ZooUtil.PRIVATE, acls);
      throw new IllegalStateException("Delegation token secret key node in ZooKeeper is not protected.");
    }

    initialized = true;
  }

  /**
   * Fetch all {@link AuthenticationKey}s currently stored in ZooKeeper beneath the configured {@code baseNode}.
   *
   * @return A list of {@link AuthenticationKey}s
   */
  public List<AuthenticationKey> getCurrentKeys() throws KeeperException, InterruptedException {
    checkState(initialized, "Not initialized");
    List<String> children = zk.getChildren(baseNode);

    // Shortcircuit to avoid a list creation
    if (children.isEmpty()) {
      return Collections.<AuthenticationKey> emptyList();
    }

    // Deserialize each byte[] into an AuthenticationKey
    List<AuthenticationKey> keys = new ArrayList<>(children.size());
    for (String child : children) {
      byte[] data = zk.getData(qualifyPath(child), null);
      if (null != data) {
        AuthenticationKey key = new AuthenticationKey();
        try {
          key.readFields(new DataInputStream(new ByteArrayInputStream(data)));
        } catch (IOException e) {
          throw new AssertionError("Error reading from in-memory buffer which should not happen", e);
        }
        keys.add(key);
      }
    }

    return keys;
  }

  /**
   * Add the given {@link AuthenticationKey} to ZooKeeper.
   *
   * @param newKey
   *          The key to add to ZooKeeper
   */
  public synchronized void advertise(AuthenticationKey newKey) throws KeeperException, InterruptedException {
    checkState(initialized, "Not initialized");
    checkNotNull(newKey);

    // Make sure the node doesn't already exist
    String path = qualifyPath(newKey);
    if (zk.exists(path)) {
      log.warn("AuthenticationKey with ID '{}' already exists in ZooKeeper", newKey.getKeyId());
      return;
    }

    // Serialize it
    ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
    try {
      newKey.write(new DataOutputStream(baos));
    } catch (IOException e) {
      throw new AssertionError("Should not get exception writing to in-memory buffer", e);
    }

    byte[] serializedKey = baos.toByteArray();

    log.debug("Advertising AuthenticationKey with keyId {} in ZooKeeper at {}", newKey.getKeyId(), path);

    // Put it into ZK with the private ACL
    zk.putPrivatePersistentData(path, serializedKey, NodeExistsPolicy.FAIL);
  }

  /**
   * Remove the given {@link AuthenticationKey} from ZooKeeper. If the node for the provided {@code key} doesn't exist in ZooKeeper, a warning is printed but an
   * error is not thrown. Since there is only a single process managing ZooKeeper at one time, any inconsistencies should be client error.
   *
   * @param key
   *          The key to remove from ZooKeeper
   */
  public synchronized void remove(AuthenticationKey key) throws KeeperException, InterruptedException {
    checkState(initialized, "Not initialized");
    checkNotNull(key);

    String path = qualifyPath(key);
    if (!zk.exists(path)) {
      log.warn("AuthenticationKey with ID '{}' doesn't exist in ZooKeeper", key.getKeyId());
      return;
    }

    log.debug("Removing AuthenticationKey with keyId {} from ZooKeeper at {}", key.getKeyId(), path);

    // Delete the node, any version
    zk.delete(path, -1);
  }

  String qualifyPath(String keyId) {
    return baseNode + "/" + keyId;
  }

  String qualifyPath(AuthenticationKey key) {
    return qualifyPath(Integer.toString(key.getKeyId()));
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcher.java b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcher.java
new file mode 100644
index 000000000..2913343f9
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcher.java
@@ -0,0 +1,206 @@
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
package org.apache.accumulo.server.security.delegation;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watch ZooKeeper to notice changes in the published keys so that authenticate can properly occur using delegation tokens.
 */
public class ZooAuthenticationKeyWatcher implements Watcher {
  private static final Logger log = LoggerFactory.getLogger(ZooAuthenticationKeyWatcher.class);

  private final AuthenticationTokenSecretManager secretManager;
  private final ZooReader zk;
  private final String baseNode;

  public ZooAuthenticationKeyWatcher(AuthenticationTokenSecretManager secretManager, ZooReader zk, String baseNode) {
    this.secretManager = secretManager;
    this.zk = zk;
    this.baseNode = baseNode;
  }

  @Override
  public void process(WatchedEvent event) {
    if (EventType.None == event.getType()) {
      switch (event.getState()) {
        case Disconnected: // Intentional fall through of case
        case Expired: // ZooReader is handling the Expiration of the original ZooKeeper object for us
          log.debug("ZooKeeper connection disconnected, clearing secret manager");
          secretManager.removeAllKeys();
          break;
        case SyncConnected:
          log.debug("ZooKeeper reconnected, updating secret manager");
          try {
            updateAuthKeys();
          } catch (KeeperException | InterruptedException e) {
            log.error("Failed to update secret manager after ZooKeeper reconnect");
          }
          break;
        default:
          log.warn("Unhandled: " + event);
      }

      // Nothing more to do for EventType.None
      return;
    }

    String path = event.getPath();
    if (null == path) {
      return;
    }

    if (!path.startsWith(baseNode)) {
      log.info("Ignoring event for path: {}", path);
      return;
    }

    try {
      if (path.equals(baseNode)) {
        processBaseNode(event);
      } else {
        processChildNode(event);
      }
    } catch (KeeperException | InterruptedException e) {
      log.error("Failed to communicate with ZooKeeper", e);
    }
  }

  /**
   * Process the {@link WatchedEvent} for the base znode that the {@link AuthenticationKey}s are stored in.
   */
  void processBaseNode(WatchedEvent event) throws KeeperException, InterruptedException {
    switch (event.getType()) {
      case NodeDeleted:
        // The parent node was deleted, no children are possible, remove all keys
        log.debug("Parent ZNode was deleted, removing all AuthenticationKeys");
        secretManager.removeAllKeys();
        break;
      case None:
        // Not connected, don't care
        break;
      case NodeCreated: // intentional fall-through to NodeChildrenChanged
      case NodeChildrenChanged:
        // Process each child, and reset the watcher on the parent node. We know that the node exists
        updateAuthKeys(event.getPath());
        break;
      case NodeDataChanged:
        // The data on the parent changed. We aren't storing anything there so it's a noop
        break;
      default:
        log.warn("Unsupported event type: {}", event.getType());
        break;
    }
  }

  /**
   * Entry point to seed the local {@link AuthenticationKey} cache from ZooKeeper and set the first watcher for future updates in ZooKeeper.
   */
  public void updateAuthKeys() throws KeeperException, InterruptedException {
    // Might cause two watchers on baseNode, but only at startup for each tserver.
    if (zk.exists(baseNode, this)) {
      log.info("Added {} existing AuthenticationKeys to local cache from ZooKeeper", updateAuthKeys(baseNode));
    }
  }

  private int updateAuthKeys(String path) throws KeeperException, InterruptedException {
    int keysAdded = 0;
    for (String child : zk.getChildren(path, this)) {
      String childPath = path + "/" + child;
      // Get the node data and reset the watcher
      AuthenticationKey key = deserializeKey(zk.getData(childPath, this, null));
      secretManager.addKey(key);
      keysAdded++;
    }
    return keysAdded;
  }

  /**
   * Process the {@link WatchedEvent} for a node which represents an {@link AuthenticationKey}
   */
  void processChildNode(WatchedEvent event) throws KeeperException, InterruptedException {
    final String path = event.getPath();
    switch (event.getType()) {
      case NodeDeleted:
        // Key expired
        if (null == path) {
          log.error("Got null path for NodeDeleted event");
          return;
        }

        // Pull off the base ZK path and the '/' separator
        String childName = path.substring(baseNode.length() + 1);
        secretManager.removeKey(Integer.parseInt(childName));
        break;
      case None:
        // Not connected, don't care. We'll update when we're reconnected
        break;
      case NodeCreated:
        // New key created
        if (null == path) {
          log.error("Got null path for NodeCreated event");
          return;
        }
        // Get the data and reset the watcher
        AuthenticationKey key = deserializeKey(zk.getData(path, this, null));
        log.debug("Adding AuthenticationKey with keyId {}", key.getKeyId());
        secretManager.addKey(key);
        break;
      case NodeDataChanged:
        // Key changed, could happen on restart after not running Accumulo.
        if (null == path) {
          log.error("Got null path for NodeDataChanged event");
          return;
        }
        // Get the data and reset the watcher
        AuthenticationKey newKey = deserializeKey(zk.getData(path, this, null));
        // Will overwrite the old key if one exists
        secretManager.addKey(newKey);
        break;
      case NodeChildrenChanged:
        // no children for the children..
        log.warn("Unexpected NodeChildrenChanged event for authentication key node {}", path);
        break;
      default:
        log.warn("Unsupported event type: {}", event.getType());
        break;
    }
  }

  /**
   * Deserialize the bytes into an {@link AuthenticationKey}
   */
  AuthenticationKey deserializeKey(byte[] serializedKey) {
    AuthenticationKey key = new AuthenticationKey();
    try {
      key.readFields(new DataInputStream(new ByteArrayInputStream(serializedKey)));
    } catch (IOException e) {
      throw new AssertionError("Failed to read from an in-memory buffer");
    }
    return key;
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/handler/KerberosAuthenticator.java b/server/base/src/main/java/org/apache/accumulo/server/security/handler/KerberosAuthenticator.java
index 08fa55bc7..369fa89a6 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/security/handler/KerberosAuthenticator.java
++ b/server/base/src/main/java/org/apache/accumulo/server/security/handler/KerberosAuthenticator.java
@@ -27,6 +27,7 @@ import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.SiteConfiguration;
@@ -131,7 +132,7 @@ public class KerberosAuthenticator implements Authenticator {
     }
 
     // User is authenticated at the transport layer -- nothing extra is necessary
    if (token instanceof KerberosToken) {
    if (token instanceof KerberosToken || token instanceof DelegationToken) {
       return true;
     }
     return false;
diff --git a/server/base/src/test/java/org/apache/accumulo/server/AccumuloServerContextTest.java b/server/base/src/test/java/org/apache/accumulo/server/AccumuloServerContextTest.java
index 49a60a656..92b6be8ef 100644
-- a/server/base/src/test/java/org/apache/accumulo/server/AccumuloServerContextTest.java
++ b/server/base/src/test/java/org/apache/accumulo/server/AccumuloServerContextTest.java
@@ -16,6 +16,10 @@
  */
 package org.apache.accumulo.server;
 
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
 import java.security.PrivilegedExceptionAction;
 import java.util.Iterator;
 import java.util.Map.Entry;
@@ -24,12 +28,15 @@ import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.impl.ClientContext;
 import org.apache.accumulo.core.client.mock.MockInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
import org.apache.accumulo.core.security.Credentials;
 import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.rpc.SaslServerConnectionParams;
 import org.apache.accumulo.server.rpc.ThriftServerType;
import org.apache.accumulo.server.security.SystemCredentials.SystemToken;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeys;
 import org.apache.hadoop.security.UserGroupInformation;
@@ -69,17 +76,27 @@ public class AccumuloServerContextTest {
         final AccumuloConfiguration conf = ClientContext.convertClientConfig(clientConf);
         SiteConfiguration siteConfig = EasyMock.createMock(SiteConfiguration.class);
 
        EasyMock.expect(siteConfig.getBoolean(Property.INSTANCE_RPC_SASL_ENABLED)).andReturn(true);

        // Deal with SystemToken being private
        PasswordToken pw = new PasswordToken("fake");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pw.write(new DataOutputStream(baos));
        SystemToken token = new SystemToken();
        token.readFields(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())));

         ServerConfigurationFactory factory = EasyMock.createMock(ServerConfigurationFactory.class);
         EasyMock.expect(factory.getConfiguration()).andReturn(conf).anyTimes();
         EasyMock.expect(factory.getSiteConfiguration()).andReturn(siteConfig).anyTimes();
         EasyMock.expect(factory.getInstance()).andReturn(instance).anyTimes();
 
         AccumuloServerContext context = EasyMock.createMockBuilder(AccumuloServerContext.class).addMockedMethod("enforceKerberosLogin")
            .addMockedMethod("getConfiguration").addMockedMethod("getServerConfigurationFactory").createMock();
            .addMockedMethod("getConfiguration").addMockedMethod("getServerConfigurationFactory").addMockedMethod("getCredentials").createMock();
         context.enforceKerberosLogin();
         EasyMock.expectLastCall().anyTimes();
         EasyMock.expect(context.getConfiguration()).andReturn(conf).anyTimes();
         EasyMock.expect(context.getServerConfigurationFactory()).andReturn(factory).anyTimes();
        EasyMock.expect(context.getCredentials()).andReturn(new Credentials("accumulo/hostname@FAKE.COM", token)).once();
 
         // Just make the SiteConfiguration delegate to our ClientConfiguration (by way of the AccumuloConfiguration)
         // Presently, we only need get(Property) and iterator().
@@ -101,8 +118,8 @@ public class AccumuloServerContextTest {
         EasyMock.replay(factory, context, siteConfig);
 
         Assert.assertEquals(ThriftServerType.SASL, context.getThriftServerType());
        SaslConnectionParams saslParams = context.getServerSaslParams();
        Assert.assertEquals(SaslConnectionParams.forConfig(conf), saslParams);
        SaslServerConnectionParams saslParams = context.getSaslParams();
        Assert.assertEquals(new SaslServerConnectionParams(conf, token), saslParams);
         Assert.assertEquals(username, saslParams.getPrincipal());
 
         EasyMock.verify(factory, context, siteConfig);
diff --git a/server/base/src/test/java/org/apache/accumulo/server/rpc/SaslDigestCallbackHandlerTest.java b/server/base/src/test/java/org/apache/accumulo/server/rpc/SaslDigestCallbackHandlerTest.java
new file mode 100644
index 000000000..6c965ffbe
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/rpc/SaslDigestCallbackHandlerTest.java
@@ -0,0 +1,137 @@
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
package org.apache.accumulo.server.rpc;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map.Entry;

import javax.crypto.KeyGenerator;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.rpc.SaslDigestCallbackHandler;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
import org.apache.accumulo.server.security.delegation.AuthenticationKey;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
import org.apache.hadoop.security.token.Token;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SaslDigestCallbackHandlerTest {

  /**
   * Allows access to the methods on SaslDigestCallbackHandler
   */
  private static class SaslTestDigestCallbackHandler extends SaslDigestCallbackHandler {
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      throw new UnsupportedOperationException();
    }
  }

  // From org.apache.hadoop.security.token.SecretManager
  private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";
  private static final int KEY_LENGTH = 64;
  private static KeyGenerator keyGen;

  @BeforeClass
  public static void setupKeyGenerator() throws Exception {
    // From org.apache.hadoop.security.token.SecretManager
    keyGen = KeyGenerator.getInstance(DEFAULT_HMAC_ALGORITHM);
    keyGen.init(KEY_LENGTH);
  }

  private SaslTestDigestCallbackHandler handler;
  private DelegationTokenConfig cfg;

  @Before
  public void setup() {
    handler = new SaslTestDigestCallbackHandler();
    cfg = new DelegationTokenConfig();
  }

  @Test
  public void testIdentifierSerialization() throws IOException {
    AuthenticationTokenIdentifier identifier = new AuthenticationTokenIdentifier("user", 1, 100l, 1000l, "instanceid");
    byte[] serialized = identifier.getBytes();
    String name = handler.encodeIdentifier(serialized);

    byte[] reserialized = handler.decodeIdentifier(name);
    assertArrayEquals(serialized, reserialized);

    AuthenticationTokenIdentifier copy = new AuthenticationTokenIdentifier();
    copy.readFields(new DataInputStream(new ByteArrayInputStream(reserialized)));

    assertEquals(identifier, copy);
  }

  @Test
  public void testTokenSerialization() throws Exception {
    Instance instance = createMock(Instance.class);
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, 1000l);
    expect(instance.getInstanceID()).andReturn("instanceid");

    replay(instance);

    secretManager.addKey(new AuthenticationKey(1, 0l, 100l, keyGen.generateKey()));
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> entry = secretManager.generateToken("user", cfg);
    byte[] password = entry.getKey().getPassword();
    char[] encodedPassword = handler.encodePassword(password);

    char[] computedPassword = handler.getPassword(secretManager, entry.getValue());

    verify(instance);

    assertArrayEquals(computedPassword, encodedPassword);
  }

  @Test
  public void testTokenAndIdentifierSerialization() throws Exception {
    Instance instance = createMock(Instance.class);
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, 1000l);
    expect(instance.getInstanceID()).andReturn("instanceid");

    replay(instance);

    secretManager.addKey(new AuthenticationKey(1, 0l, 1000 * 100l, keyGen.generateKey()));
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> entry = secretManager.generateToken("user", cfg);
    byte[] password = entry.getKey().getPassword();
    char[] encodedPassword = handler.encodePassword(password);
    String name = handler.encodeIdentifier(entry.getValue().getBytes());

    byte[] decodedIdentifier = handler.decodeIdentifier(name);
    AuthenticationTokenIdentifier identifier = new AuthenticationTokenIdentifier();
    identifier.readFields(new DataInputStream(new ByteArrayInputStream(decodedIdentifier)));
    char[] computedPassword = handler.getPassword(secretManager, identifier);

    verify(instance);

    assertArrayEquals(computedPassword, encodedPassword);
  }
}
diff --git a/server/base/src/test/java/org/apache/accumulo/server/rpc/SaslServerConnectionParamsTest.java b/server/base/src/test/java/org/apache/accumulo/server/rpc/SaslServerConnectionParamsTest.java
new file mode 100644
index 000000000..39bf9e46a
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/rpc/SaslServerConnectionParamsTest.java
@@ -0,0 +1,101 @@
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
package org.apache.accumulo.server.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.security.sasl.Sasl;

import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.rpc.SaslConnectionParams;
import org.apache.accumulo.core.rpc.SaslConnectionParams.QualityOfProtection;
import org.apache.accumulo.core.rpc.SaslConnectionParams.SaslMechanism;
import org.apache.accumulo.server.security.SystemCredentials.SystemToken;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Before;
import org.junit.Test;

public class SaslServerConnectionParamsTest {

  private UserGroupInformation testUser;
  private String username;

  @Before
  public void setup() throws Exception {
    System.setProperty("java.security.krb5.realm", "accumulo");
    System.setProperty("java.security.krb5.kdc", "fake");
    Configuration conf = new Configuration(false);
    conf.set(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION, "kerberos");
    UserGroupInformation.setConfiguration(conf);
    testUser = UserGroupInformation.createUserForTesting("test_user", new String[0]);
    username = testUser.getUserName();
  }

  @Test
  public void testDefaultParamsAsServer() throws Exception {
    testUser.doAs(new PrivilegedExceptionAction<Void>() {
      @Override
      public Void run() throws Exception {
        final ClientConfiguration clientConf = ClientConfiguration.loadDefault();

        // The primary is the first component of the principal
        final String primary = "accumulo";
        clientConf.withSasl(true, primary);

        final AccumuloConfiguration rpcConf = ClientContext.convertClientConfig(clientConf);
        assertEquals("true", clientConf.get(ClientProperty.INSTANCE_RPC_SASL_ENABLED));

        // Deal with SystemToken being private
        PasswordToken pw = new PasswordToken("fake");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pw.write(new DataOutputStream(baos));
        SystemToken token = new SystemToken();
        token.readFields(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())));

        final SaslConnectionParams saslParams = new SaslServerConnectionParams(rpcConf, token);
        assertEquals(primary, saslParams.getKerberosServerPrimary());
        assertEquals(SaslMechanism.GSSAPI, saslParams.getMechanism());
        assertNull(saslParams.getCallbackHandler());

        final QualityOfProtection defaultQop = QualityOfProtection.get(Property.RPC_SASL_QOP.getDefaultValue());
        assertEquals(defaultQop, saslParams.getQualityOfProtection());

        Map<String,String> properties = saslParams.getSaslProperties();
        assertEquals(1, properties.size());
        assertEquals(defaultQop.getQuality(), properties.get(Sasl.QOP));
        assertEquals(username, saslParams.getPrincipal());
        return null;
      }
    });
  }

}
diff --git a/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationKeyTest.java b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationKeyTest.java
new file mode 100644
index 000000000..02e22aa6d
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationKeyTest.java
@@ -0,0 +1,95 @@
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
package org.apache.accumulo.server.security.delegation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.BeforeClass;
import org.junit.Test;

public class AuthenticationKeyTest {
  // From org.apache.hadoop.security.token.SecretManager
  private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";
  private static final int KEY_LENGTH = 64;
  private static KeyGenerator keyGen;

  @BeforeClass
  public static void setupKeyGenerator() throws Exception {
    // From org.apache.hadoop.security.token.SecretManager
    keyGen = KeyGenerator.getInstance(DEFAULT_HMAC_ALGORITHM);
    keyGen.init(KEY_LENGTH);
  }

  @Test(expected = NullPointerException.class)
  public void testNullSecretKey() {
    new AuthenticationKey(0, 0, 0, null);
  }

  @Test
  public void testAuthKey() {
    SecretKey secretKey = keyGen.generateKey();
    int keyId = 20;
    long creationDate = 38383838l, expirationDate = 83838383l;
    AuthenticationKey authKey = new AuthenticationKey(keyId, creationDate, expirationDate, secretKey);
    assertEquals(secretKey, authKey.getKey());
    assertEquals(keyId, authKey.getKeyId());
    assertEquals(expirationDate, authKey.getExpirationDate());

    // Empty instance
    AuthenticationKey badCopy = new AuthenticationKey();

    assertNotEquals(badCopy, authKey);
    assertNotEquals(badCopy.hashCode(), authKey.hashCode());

    // Different object, same arguments
    AuthenticationKey goodCopy = new AuthenticationKey(keyId, creationDate, expirationDate, secretKey);
    assertEquals(authKey, goodCopy);
    assertEquals(authKey.hashCode(), goodCopy.hashCode());
  }

  @Test
  public void testWritable() throws IOException {
    SecretKey secretKey = keyGen.generateKey();
    int keyId = 20;
    long creationDate = 38383838l, expirationDate = 83838383l;
    AuthenticationKey authKey = new AuthenticationKey(keyId, creationDate, expirationDate, secretKey);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    authKey.write(out);
    byte[] serialized = baos.toByteArray();

    DataInputStream in = new DataInputStream(new ByteArrayInputStream(serialized));
    AuthenticationKey copy = new AuthenticationKey();
    copy.readFields(in);

    assertEquals(authKey, copy);
    assertEquals(authKey.hashCode(), copy.hashCode());
    assertEquals(secretKey, copy.getKey());
    assertEquals(keyId, copy.getKeyId());
    assertEquals(expirationDate, copy.getExpirationDate());
  }
}
diff --git a/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManagerTest.java b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManagerTest.java
new file mode 100644
index 000000000..bc2968a08
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenKeyManagerTest.java
@@ -0,0 +1,196 @@
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
package org.apache.accumulo.server.security.delegation;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationTokenKeyManagerTest {
  private static final Logger log = LoggerFactory.getLogger(AuthenticationTokenKeyManagerTest.class);

  // From org.apache.hadoop.security.token.SecretManager
  private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";
  private static final int KEY_LENGTH = 64;
  private static KeyGenerator keyGen;

  @BeforeClass
  public static void setupKeyGenerator() throws Exception {
    // From org.apache.hadoop.security.token.SecretManager
    keyGen = KeyGenerator.getInstance(DEFAULT_HMAC_ALGORITHM);
    keyGen.init(KEY_LENGTH);
  }

  private AuthenticationTokenSecretManager secretManager;
  private ZooAuthenticationKeyDistributor zooDistributor;

  @Before
  public void setupMocks() {
    secretManager = createMock(AuthenticationTokenSecretManager.class);
    zooDistributor = createMock(ZooAuthenticationKeyDistributor.class);
  }

  @Test
  public void testIntervalNotPassed() {
    long updateInterval = 5 * 1000l;
    long tokenLifetime = 100 * 1000l;
    AuthenticationTokenKeyManager keyManager = new AuthenticationTokenKeyManager(secretManager, zooDistributor, updateInterval, tokenLifetime);

    // Have never updated the key
    assertEquals(0l, keyManager.getLastKeyUpdate());

    // Always check for expired keys to remove
    expect(secretManager.removeExpiredKeys(zooDistributor)).andReturn(0);

    replay(secretManager, zooDistributor);

    // Run at time 0. Last run time is still 0. 0 + 5000 > 0, so we won't generate a new key
    keyManager._run(0);

    verify(secretManager, zooDistributor);
  }

  @Test
  public void testIntervalHasPassed() throws Exception {
    long updateInterval = 0 * 1000l;
    long tokenLifetime = 100 * 1000l;
    long runTime = 10l;
    SecretKey secretKey = keyGen.generateKey();

    AuthenticationKey authKey = new AuthenticationKey(1, runTime, runTime + tokenLifetime, secretKey);
    AuthenticationTokenKeyManager keyManager = new AuthenticationTokenKeyManager(secretManager, zooDistributor, updateInterval, tokenLifetime);

    // Have never updated the key
    assertEquals(0l, keyManager.getLastKeyUpdate());

    // Always check for expired keys to remove
    expect(secretManager.removeExpiredKeys(zooDistributor)).andReturn(0);
    expect(secretManager.generateSecret()).andReturn(secretKey);
    secretManager.addKey(authKey);
    expectLastCall().once();
    zooDistributor.advertise(authKey);
    expectLastCall().once();

    replay(secretManager, zooDistributor);

    // Run at time 10. Last run time is still 0. 0 + 10 > 0, so we will generate a new key
    keyManager._run(runTime);

    verify(secretManager, zooDistributor);

    // Last key update time should match when we ran
    assertEquals(runTime, keyManager.getLastKeyUpdate());
    // KeyManager uses the incremented value for the new AuthKey (the current idSeq will match the keyId for the last generated key)
    assertEquals(authKey.getKeyId(), keyManager.getIdSeq());
  }

  @Test(timeout = 30 * 1000)
  public void testStopLoop() throws InterruptedException {
    final AuthenticationTokenKeyManager keyManager = EasyMock.createMockBuilder(AuthenticationTokenKeyManager.class).addMockedMethod("_run")
        .addMockedMethod("updateStateFromCurrentKeys").createMock();
    final CountDownLatch latch = new CountDownLatch(1);

    // Mock out the _run and updateStateFromCurrentKeys method so we just get the logic from "run()"
    keyManager._run(EasyMock.anyLong());
    expectLastCall().once();
    keyManager.updateStateFromCurrentKeys();
    expectLastCall().once();

    replay(keyManager);

    keyManager.setKeepRunning(true);

    // Wrap another Runnable around our KeyManager so we know when the thread is actually run as it's "async" when the method will actually be run after we call
    // thread.start()
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        log.info("Thread running");
        latch.countDown();
        keyManager.run();
      }
    });

    log.info("Starting thread");
    t.start();

    // Wait for the thread to start
    latch.await();
    log.info("Latch fired");

    // Wait a little bit to let the first call to _run() happen (avoid exiting the loop before any calls to _run())
    Thread.sleep(1000);

    log.info("Finished waiting, stopping keymanager");

    keyManager.gracefulStop();

    log.info("Waiting for thread to exit naturally");

    t.join();

    verify(keyManager);
  }

  @Test
  public void testExistingKeysAreAddedAtStartup() throws Exception {
    long updateInterval = 0 * 1000l;
    long tokenLifetime = 100 * 1000l;
    SecretKey secretKey1 = keyGen.generateKey(), secretKey2 = keyGen.generateKey();

    AuthenticationKey authKey1 = new AuthenticationKey(1, 0, tokenLifetime, secretKey1), authKey2 = new AuthenticationKey(2, tokenLifetime, tokenLifetime * 2,
        secretKey2);
    AuthenticationTokenKeyManager keyManager = new AuthenticationTokenKeyManager(secretManager, zooDistributor, updateInterval, tokenLifetime);

    // Have never updated the key
    assertEquals(0l, keyManager.getLastKeyUpdate());

    // Always check for expired keys to remove
    expect(zooDistributor.getCurrentKeys()).andReturn(Arrays.asList(authKey1, authKey2));
    secretManager.addKey(authKey1);
    expectLastCall().once();
    secretManager.addKey(authKey2);
    expectLastCall().once();
    expect(secretManager.getCurrentKey()).andReturn(authKey2).once();

    replay(secretManager, zooDistributor);

    // Initialize the state from zookeeper
    keyManager.updateStateFromCurrentKeys();

    verify(secretManager, zooDistributor);

    assertEquals(authKey2.getKeyId(), keyManager.getIdSeq());
    assertEquals(authKey2.getCreationDate(), keyManager.getLastKeyUpdate());
  }
}
diff --git a/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManagerTest.java b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManagerTest.java
new file mode 100644
index 000000000..b6148191c
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/AuthenticationTokenSecretManagerTest.java
@@ -0,0 +1,393 @@
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
package org.apache.accumulo.server.security.delegation;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
import org.apache.hadoop.security.token.SecretManager.InvalidToken;
import org.apache.hadoop.security.token.Token;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

public class AuthenticationTokenSecretManagerTest {
  private static final Logger log = LoggerFactory.getLogger(AuthenticationTokenSecretManagerTest.class);

  // From org.apache.hadoop.security.token.SecretManager
  private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";
  private static final int KEY_LENGTH = 64;
  private static KeyGenerator keyGen;

  @BeforeClass
  public static void setupKeyGenerator() throws Exception {
    // From org.apache.hadoop.security.token.SecretManager
    keyGen = KeyGenerator.getInstance(DEFAULT_HMAC_ALGORITHM);
    keyGen.init(KEY_LENGTH);
  }

  private Instance instance;
  private String instanceId;
  private DelegationTokenConfig cfg;

  @Before
  public void setupMocks() {
    instance = createMock(Instance.class);
    instanceId = UUID.randomUUID().toString();
    cfg = new DelegationTokenConfig();
    expect(instance.getInstanceID()).andReturn(instanceId).anyTimes();
    replay(instance);
  }

  @After
  public void verifyMocks() {
    verify(instance);
  }

  @Test
  public void testAddKey() {
    // 1 minute
    long tokenLifetime = 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a single key
    AuthenticationKey authKey = new AuthenticationKey(1, 0, tokenLifetime, keyGen.generateKey());
    secretManager.addKey(authKey);

    // Ensure it's in the cache
    Map<Integer,AuthenticationKey> keys = secretManager.getKeys();
    assertNotNull(keys);
    assertEquals(1, keys.size());
    assertEquals(authKey, Iterables.getOnlyElement(keys.values()));

    // Add the same key
    secretManager.addKey(authKey);

    // Ensure we still have only one key
    keys = secretManager.getKeys();
    assertNotNull(keys);
    assertEquals(1, keys.size());
    assertEquals(authKey, Iterables.getOnlyElement(keys.values()));
  }

  @Test
  public void testRemoveKey() {
    // 1 minute
    long tokenLifetime = 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a single key
    AuthenticationKey authKey = new AuthenticationKey(1, 0, tokenLifetime, keyGen.generateKey());
    secretManager.addKey(authKey);

    // Ensure it's in the cache
    Map<Integer,AuthenticationKey> keys = secretManager.getKeys();
    assertNotNull(keys);
    assertEquals(1, keys.size());
    assertEquals(authKey, Iterables.getOnlyElement(keys.values()));

    assertTrue(secretManager.removeKey(authKey.getKeyId()));
    assertEquals(0, secretManager.getKeys().size());
  }

  @Test
  public void testGenerateToken() throws Exception {
    // start of the test
    long then = System.currentTimeMillis();

    // 1 minute
    long tokenLifetime = 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a current key
    secretManager.addKey(new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey()));

    String principal = "user@EXAMPLE.COM";
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair = secretManager.generateToken(principal, cfg);

    assertNotNull(pair);
    Token<AuthenticationTokenIdentifier> token = pair.getKey();
    assertNotNull(token);
    assertEquals(AuthenticationTokenIdentifier.TOKEN_KIND, token.getKind());

    // Reconstitute the token identifier (will happen when clients are involved)
    AuthenticationTokenIdentifier id = new AuthenticationTokenIdentifier();
    id.readFields(new DataInputStream(new ByteArrayInputStream(token.getIdentifier())));
    long now = System.currentTimeMillis();

    // Issue date should be after the test started, but before we deserialized the token
    assertTrue("Issue date did not fall within the expected upper bound. Expected less than " + now + ", but was " + id.getIssueDate(),
        id.getIssueDate() <= now);
    assertTrue("Issue date did not fall within the expected lower bound. Expected greater than " + then + ", but was " + id.getIssueDate(),
        id.getIssueDate() >= then);

    // Expiration is the token lifetime plus the issue date
    assertEquals(id.getIssueDate() + tokenLifetime, id.getExpirationDate());

    // Verify instance ID
    assertEquals(instanceId, id.getInstanceId());

    // The returned id should be the same as the reconstructed id
    assertEquals(pair.getValue(), id);
  }

  @Test
  public void testVerifyPassword() throws Exception {
    // start of the test
    long then = System.currentTimeMillis();

    // 1 minute
    long tokenLifetime = 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a current key
    secretManager.addKey(new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey()));

    String principal = "user@EXAMPLE.COM";
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair = secretManager.generateToken(principal, cfg);
    Token<AuthenticationTokenIdentifier> token = pair.getKey();

    AuthenticationTokenIdentifier id = new AuthenticationTokenIdentifier();
    id.readFields(new DataInputStream(new ByteArrayInputStream(token.getIdentifier())));

    byte[] password = secretManager.retrievePassword(id);

    // The passwords line up against multiple calls with the same ID
    assertArrayEquals(password, secretManager.retrievePassword(id));

    // Make a second token for the same user
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair2 = secretManager.generateToken(principal, cfg);
    Token<AuthenticationTokenIdentifier> token2 = pair2.getKey();
    // Reconstitute the token identifier (will happen when clients are involved)
    AuthenticationTokenIdentifier id2 = new AuthenticationTokenIdentifier();
    id2.readFields(new DataInputStream(new ByteArrayInputStream(token2.getIdentifier())));

    // Get the password
    byte[] password2 = secretManager.retrievePassword(id2);

    // It should be different than the password for the first user.
    assertFalse("Different tokens for the same user shouldn't have the same password", Arrays.equals(password, password2));
  }

  @Test(expected = InvalidToken.class)
  public void testExpiredPasswordsThrowError() throws Exception {
    // start of the test
    long then = System.currentTimeMillis();

    // 500ms lifetime
    long tokenLifetime = 500;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a current key
    secretManager.addKey(new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey()));

    String principal = "user@EXAMPLE.COM";
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair = secretManager.generateToken(principal, cfg);
    Token<AuthenticationTokenIdentifier> token = pair.getKey();

    // Add a small buffer to make sure we move past the expiration of 0 for the token.
    Thread.sleep(1000);

    // Reconstitute the token identifier (will happen when clients are involved)
    AuthenticationTokenIdentifier id = new AuthenticationTokenIdentifier();
    id.readFields(new DataInputStream(new ByteArrayInputStream(token.getIdentifier())));

    secretManager.retrievePassword(id);
  }

  @Test(expected = InvalidToken.class)
  public void testTokenIssuedInFuture() throws Exception {
    // start of the test
    long then = System.currentTimeMillis();

    long tokenLifetime = 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a current key
    secretManager.addKey(new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey()));

    String principal = "user@EXAMPLE.COM";
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair = secretManager.generateToken(principal, cfg);
    Token<AuthenticationTokenIdentifier> token = pair.getKey();

    // Reconstitute the token identifier (will happen when clients are involved)
    AuthenticationTokenIdentifier id = new AuthenticationTokenIdentifier();
    id.readFields(new DataInputStream(new ByteArrayInputStream(token.getIdentifier())));

    // Increase the value of issueDate
    id.setIssueDate(Long.MAX_VALUE);

    secretManager.retrievePassword(id);
  }

  @Test(expected = InvalidToken.class)
  public void testRolledMasterKey() throws Exception {
    // start of the test
    long then = System.currentTimeMillis();

    long tokenLifetime = 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a current key
    AuthenticationKey authKey1 = new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey());
    secretManager.addKey(authKey1);

    String principal = "user@EXAMPLE.COM";
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair = secretManager.generateToken(principal, cfg);
    Token<AuthenticationTokenIdentifier> token = pair.getKey();

    AuthenticationTokenIdentifier id = new AuthenticationTokenIdentifier();
    id.readFields(new DataInputStream(new ByteArrayInputStream(token.getIdentifier())));

    long now = System.currentTimeMillis();
    secretManager.addKey(new AuthenticationKey(2, now, now + tokenLifetime, keyGen.generateKey()));

    // Should succeed -- the SecretManager still has authKey1
    secretManager.retrievePassword(id);

    // Remove authKey1
    secretManager.removeKey(authKey1.getKeyId());

    // Should fail -- authKey1 (presumably) expired, cannot authenticate
    secretManager.retrievePassword(id);
  }

  @Test(timeout = 20 * 1000)
  public void testMasterKeyExpiration() throws Exception {
    ZooAuthenticationKeyDistributor keyDistributor = createMock(ZooAuthenticationKeyDistributor.class);
    // start of the test
    long then = System.currentTimeMillis();

    // 10s lifetime
    long tokenLifetime = 10 * 1000l;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Make 2 keys, and add only one. The second has double the expiration of the first
    AuthenticationKey authKey1 = new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey()), authKey2 = new AuthenticationKey(2, then
        + tokenLifetime, then + tokenLifetime * 2, keyGen.generateKey());
    secretManager.addKey(authKey1);

    keyDistributor.remove(authKey1);
    expectLastCall().once();

    replay(keyDistributor);

    // Make sure expiration doesn't trigger anything yet
    assertEquals(0, secretManager.removeExpiredKeys(keyDistributor));
    assertEquals(1, secretManager.getKeys().size());

    // Add the second key, still no expiration
    secretManager.addKey(authKey2);
    assertEquals(0, secretManager.removeExpiredKeys(keyDistributor));
    assertEquals(2, secretManager.getKeys().size());
    assertEquals(authKey2, secretManager.getCurrentKey());

    // Wait for the expiration
    long now = System.currentTimeMillis();
    while (now - (then + tokenLifetime) < 0) {
      Thread.sleep(500);
      now = System.currentTimeMillis();
    }

    // Expire the first
    assertEquals(1, secretManager.removeExpiredKeys(keyDistributor));

    // Ensure the second still exists
    assertEquals(1, secretManager.getKeys().size());
    assertEquals(authKey2, Iterables.getOnlyElement(secretManager.getKeys().values()));
    assertEquals(authKey2, secretManager.getCurrentKey());

    verify(keyDistributor);
  }

  @Test
  public void testRestrictExpirationDate() throws Exception {
    // start of the test
    long then = System.currentTimeMillis();

    // 1 hr
    long tokenLifetime = 60 * 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a current key
    secretManager.addKey(new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey()));

    // 1 minute
    cfg.setTokenLifetime(1, TimeUnit.MINUTES);

    String principal = "user@EXAMPLE.COM";
    Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair = secretManager.generateToken(principal, cfg);

    assertNotNull(pair);

    long now = System.currentTimeMillis();
    long actualExpiration = pair.getValue().getExpirationDate();
    long approximateLifetime = actualExpiration - now;

    log.info("actualExpiration={}, approximateLifetime={}", actualExpiration, approximateLifetime);

    // We don't know the exact lifetime, but we know that it can be no more than what was requested
    assertTrue("Expected lifetime to be on thet order of the token lifetime, but was " + approximateLifetime,
        approximateLifetime <= cfg.getTokenLifetime(TimeUnit.MILLISECONDS));
  }

  @Test(expected = AccumuloException.class)
  public void testInvalidRequestedExpirationDate() throws Exception {
    // start of the test
    long then = System.currentTimeMillis();

    // 1 hr
    long tokenLifetime = 60 * 60 * 1000;
    AuthenticationTokenSecretManager secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);

    // Add a current key
    secretManager.addKey(new AuthenticationKey(1, then, then + tokenLifetime, keyGen.generateKey()));

    // A longer timeout than the secret key has
    cfg.setTokenLifetime(tokenLifetime + 1, TimeUnit.MILLISECONDS);

    // Should throw an exception
    secretManager.generateToken("user@EXAMPLE.COM", cfg);
  }
}
diff --git a/server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributorTest.java b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributorTest.java
new file mode 100644
index 000000000..ed40a109e
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyDistributorTest.java
@@ -0,0 +1,270 @@
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
package org.apache.accumulo.server.security.delegation;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.KeyGenerator;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.fate.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException.AuthFailedException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZooAuthenticationKeyDistributorTest {

  // From org.apache.hadoop.security.token.SecretManager
  private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";
  private static final int KEY_LENGTH = 64;
  private static KeyGenerator keyGen;

  @BeforeClass
  public static void setupKeyGenerator() throws Exception {
    // From org.apache.hadoop.security.token.SecretManager
    keyGen = KeyGenerator.getInstance(DEFAULT_HMAC_ALGORITHM);
    keyGen.init(KEY_LENGTH);
  }

  private ZooReaderWriter zrw;
  private String baseNode = Constants.ZDELEGATION_TOKEN_KEYS;

  @Before
  public void setupMocks() {
    zrw = createMock(ZooReaderWriter.class);
  }

  @Test(expected = AuthFailedException.class)
  public void testInitialize() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(false);
    expect(zrw.putPrivatePersistentData(eq(baseNode), aryEq(new byte[0]), eq(NodeExistsPolicy.FAIL))).andThrow(new AuthFailedException());

    replay(zrw);

    distributor.initialize();

    verify(zrw);
  }

  @Test
  public void testInitializeCreatesParentNode() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(false);
    expect(zrw.putPrivatePersistentData(eq(baseNode), (byte[]) anyObject(), eq(NodeExistsPolicy.FAIL))).andReturn(true);

    replay(zrw);

    distributor.initialize();

    verify(zrw);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializedNotCalledAdvertise() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    distributor.advertise(new AuthenticationKey(1, 0l, 5l, keyGen.generateKey()));
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializedNotCalledCurrentKeys() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    distributor.getCurrentKeys();
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializedNotCalledRemove() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    distributor.remove(new AuthenticationKey(1, 0l, 5l, keyGen.generateKey()));
  }

  @Test(expected = IllegalStateException.class)
  public void testMissingAcl() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(true);
    expect(zrw.getACL(eq(baseNode), anyObject(Stat.class))).andReturn(Collections.<ACL> emptyList());

    replay(zrw);

    try {
      distributor.initialize();
    } finally {
      verify(zrw);
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testBadAcl() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(true);
    expect(zrw.getACL(eq(baseNode), anyObject(Stat.class))).andReturn(
        Collections.singletonList(new ACL(ZooUtil.PRIVATE.get(0).getPerms(), new Id("digest", "somethingweird"))));

    replay(zrw);

    try {
      distributor.initialize();
    } finally {
      verify(zrw);
    }
  }

  @Test
  public void testAdvertiseKey() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    AuthenticationKey key = new AuthenticationKey(1, 0l, 10l, keyGen.generateKey());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    key.write(new DataOutputStream(baos));
    byte[] serialized = baos.toByteArray();
    String path = baseNode + "/" + key.getKeyId();

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(true);
    expect(zrw.getACL(eq(baseNode), anyObject(Stat.class))).andReturn(
        Collections.singletonList(new ACL(ZooUtil.PRIVATE.get(0).getPerms(), new Id("digest", "accumulo:DEFAULT"))));
    expect(zrw.exists(path)).andReturn(false);
    expect(zrw.putPrivatePersistentData(eq(path), aryEq(serialized), eq(NodeExistsPolicy.FAIL))).andReturn(true);

    replay(zrw);

    distributor.initialize();
    distributor.advertise(key);

    verify(zrw);
  }

  @Test
  public void testAlreadyAdvertisedKey() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    AuthenticationKey key = new AuthenticationKey(1, 0l, 10l, keyGen.generateKey());
    String path = baseNode + "/" + key.getKeyId();

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(true);
    expect(zrw.getACL(eq(baseNode), anyObject(Stat.class))).andReturn(
        Collections.singletonList(new ACL(ZooUtil.PRIVATE.get(0).getPerms(), new Id("digest", "accumulo:DEFAULT"))));
    expect(zrw.exists(path)).andReturn(true);

    replay(zrw);

    distributor.initialize();
    distributor.advertise(key);

    verify(zrw);
  }

  @Test
  public void testRemoveKey() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    AuthenticationKey key = new AuthenticationKey(1, 0l, 10l, keyGen.generateKey());
    String path = baseNode + "/" + key.getKeyId();

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(true);
    expect(zrw.getACL(eq(baseNode), anyObject(Stat.class))).andReturn(
        Collections.singletonList(new ACL(ZooUtil.PRIVATE.get(0).getPerms(), new Id("digest", "accumulo:DEFAULT"))));
    expect(zrw.exists(path)).andReturn(true);
    zrw.delete(path, -1);
    expectLastCall().once();

    replay(zrw);

    distributor.initialize();
    distributor.remove(key);

    verify(zrw);
  }

  @Test
  public void testRemoveMissingKey() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    AuthenticationKey key = new AuthenticationKey(1, 0l, 10l, keyGen.generateKey());
    String path = baseNode + "/" + key.getKeyId();

    // Attempt to create the directory and fail
    expect(zrw.exists(baseNode)).andReturn(true);
    expect(zrw.getACL(eq(baseNode), anyObject(Stat.class))).andReturn(
        Collections.singletonList(new ACL(ZooUtil.PRIVATE.get(0).getPerms(), new Id("digest", "accumulo:DEFAULT"))));
    expect(zrw.exists(path)).andReturn(false);

    replay(zrw);

    distributor.initialize();
    distributor.remove(key);

    verify(zrw);
  }

  @Test
  public void testGetCurrentKeys() throws Exception {
    ZooAuthenticationKeyDistributor distributor = new ZooAuthenticationKeyDistributor(zrw, baseNode);
    List<AuthenticationKey> keys = new ArrayList<>(5);
    List<byte[]> serializedKeys = new ArrayList<>(5);
    List<String> children = new ArrayList<>(5);
    for (int i = 1; i < 6; i++) {
      children.add(Integer.toString(i));
      AuthenticationKey key = new AuthenticationKey(i, 0l, 10l, keyGen.generateKey());
      keys.add(key);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      key.write(new DataOutputStream(baos));
      serializedKeys.add(baos.toByteArray());
    }

    expect(zrw.exists(baseNode)).andReturn(true);
    expect(zrw.getACL(eq(baseNode), anyObject(Stat.class))).andReturn(
        Collections.singletonList(new ACL(ZooUtil.PRIVATE.get(0).getPerms(), new Id("digest", "accumulo:DEFAULT"))));
    expect(zrw.getChildren(baseNode)).andReturn(children);
    for (int i = 1; i < 6; i++) {
      expect(zrw.getData(baseNode + "/" + i, null)).andReturn(serializedKeys.get(i - 1));
    }

    replay(zrw);

    distributor.initialize();
    assertEquals(keys, distributor.getCurrentKeys());

    verify(zrw);
  }
}
diff --git a/server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcherTest.java b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcherTest.java
new file mode 100644
index 000000000..a60c9bc38
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/security/delegation/ZooAuthenticationKeyWatcherTest.java
@@ -0,0 +1,323 @@
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
package org.apache.accumulo.server.security.delegation;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.crypto.KeyGenerator;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZooAuthenticationKeyWatcherTest {

  // From org.apache.hadoop.security.token.SecretManager
  private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA1";
  private static final int KEY_LENGTH = 64;
  private static KeyGenerator keyGen;

  @BeforeClass
  public static void setupKeyGenerator() throws Exception {
    // From org.apache.hadoop.security.token.SecretManager
    keyGen = KeyGenerator.getInstance(DEFAULT_HMAC_ALGORITHM);
    keyGen.init(KEY_LENGTH);
  }

  private ZooReader zk;
  private Instance instance;
  private String instanceId;
  private String baseNode;
  private long tokenLifetime = 7 * 24 * 60 * 60 * 1000; // 7days
  private AuthenticationTokenSecretManager secretManager;
  private ZooAuthenticationKeyWatcher keyWatcher;

  @Before
  public void setupMocks() {
    zk = createMock(ZooReader.class);
    instance = createMock(Instance.class);
    instanceId = UUID.randomUUID().toString();
    baseNode = "/accumulo/" + instanceId + Constants.ZDELEGATION_TOKEN_KEYS;
    expect(instance.getInstanceID()).andReturn(instanceId).anyTimes();
    secretManager = new AuthenticationTokenSecretManager(instance, tokenLifetime);
    keyWatcher = new ZooAuthenticationKeyWatcher(secretManager, zk, baseNode);
  }

  @Test
  public void testBaseNodeCreated() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeCreated, null, baseNode);

    expect(zk.getChildren(baseNode, keyWatcher)).andReturn(Collections.<String> emptyList());
    replay(instance, zk);

    keyWatcher.process(event);

    verify(instance, zk);
    assertTrue(secretManager.getKeys().isEmpty());
  }

  @Test
  public void testBaseNodeCreatedWithChildren() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeCreated, null, baseNode);
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(2, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());
    byte[] serializedKey1 = serialize(key1), serializedKey2 = serialize(key2);
    List<String> children = Arrays.asList("1", "2");

    expect(zk.getChildren(baseNode, keyWatcher)).andReturn(children);
    expect(zk.getData(baseNode + "/1", keyWatcher, null)).andReturn(serializedKey1);
    expect(zk.getData(baseNode + "/2", keyWatcher, null)).andReturn(serializedKey2);
    replay(instance, zk);

    keyWatcher.process(event);

    verify(instance, zk);
    assertEquals(2, secretManager.getKeys().size());
    assertEquals(key1, secretManager.getKeys().get(key1.getKeyId()));
    assertEquals(key2, secretManager.getKeys().get(key2.getKeyId()));
  }

  @Test
  public void testBaseNodeChildrenChanged() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeChildrenChanged, null, baseNode);
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(2, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());
    byte[] serializedKey1 = serialize(key1), serializedKey2 = serialize(key2);
    List<String> children = Arrays.asList("1", "2");

    expect(zk.getChildren(baseNode, keyWatcher)).andReturn(children);
    expect(zk.getData(baseNode + "/1", keyWatcher, null)).andReturn(serializedKey1);
    expect(zk.getData(baseNode + "/2", keyWatcher, null)).andReturn(serializedKey2);
    replay(instance, zk);

    keyWatcher.process(event);

    verify(instance, zk);
    assertEquals(2, secretManager.getKeys().size());
    assertEquals(key1, secretManager.getKeys().get(key1.getKeyId()));
    assertEquals(key2, secretManager.getKeys().get(key2.getKeyId()));
  }

  @Test
  public void testBaseNodeDeleted() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeDeleted, null, baseNode);
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(2, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());

    secretManager.addKey(key1);
    secretManager.addKey(key2);
    assertEquals(2, secretManager.getKeys().size());

    replay(instance, zk);

    keyWatcher.process(event);

    verify(instance, zk);
    assertEquals(0, secretManager.getKeys().size());
    assertFalse(secretManager.isCurrentKeySet());
  }

  @Test
  public void testBaseNodeDataChanged() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeDataChanged, null, baseNode);

    replay(instance, zk);

    keyWatcher.process(event);

    verify(instance, zk);
    assertEquals(0, secretManager.getKeys().size());
    assertFalse(secretManager.isCurrentKeySet());
  }

  @Test
  public void testChildChanged() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeCreated, null, baseNode + "/2");
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(2, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());
    secretManager.addKey(key1);
    assertEquals(1, secretManager.getKeys().size());
    byte[] serializedKey2 = serialize(key2);

    expect(zk.getData(event.getPath(), keyWatcher, null)).andReturn(serializedKey2);
    replay(instance, zk);

    keyWatcher.process(event);

    verify(instance, zk);
    assertEquals(2, secretManager.getKeys().size());
    assertEquals(key1, secretManager.getKeys().get(key1.getKeyId()));
    assertEquals(key2, secretManager.getKeys().get(key2.getKeyId()));
    assertEquals(key2, secretManager.getCurrentKey());
  }

  @Test
  public void testChildDeleted() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeDeleted, null, baseNode + "/1");
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(2, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());
    secretManager.addKey(key1);
    secretManager.addKey(key2);
    assertEquals(2, secretManager.getKeys().size());

    replay(instance, zk);

    keyWatcher.process(event);

    verify(instance, zk);
    assertEquals(1, secretManager.getKeys().size());
    assertEquals(key2, secretManager.getKeys().get(key2.getKeyId()));
    assertEquals(key2, secretManager.getCurrentKey());
  }

  @Test
  public void testChildChildrenChanged() throws Exception {
    WatchedEvent event = new WatchedEvent(EventType.NodeChildrenChanged, null, baseNode + "/2");
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(2, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());
    secretManager.addKey(key1);
    secretManager.addKey(key2);
    assertEquals(2, secretManager.getKeys().size());

    replay(instance, zk);

    // Does nothing
    keyWatcher.process(event);

    verify(instance, zk);
    assertEquals(2, secretManager.getKeys().size());
    assertEquals(key1, secretManager.getKeys().get(key1.getKeyId()));
    assertEquals(key2, secretManager.getKeys().get(key2.getKeyId()));
    assertEquals(key2, secretManager.getCurrentKey());
  }

  @Test
  public void testInitialUpdateNoNode() throws Exception {
    expect(zk.exists(baseNode, keyWatcher)).andReturn(false);

    replay(zk, instance);

    keyWatcher.updateAuthKeys();

    verify(zk, instance);
    assertEquals(0, secretManager.getKeys().size());
    assertNull(secretManager.getCurrentKey());
  }

  @Test
  public void testInitialUpdateWithKeys() throws Exception {
    List<String> children = Arrays.asList("1", "5");
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(5, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());

    expect(zk.exists(baseNode, keyWatcher)).andReturn(true);
    expect(zk.getChildren(baseNode, keyWatcher)).andReturn(children);
    expect(zk.getData(baseNode + "/" + key1.getKeyId(), keyWatcher, null)).andReturn(serialize(key1));
    expect(zk.getData(baseNode + "/" + key2.getKeyId(), keyWatcher, null)).andReturn(serialize(key2));

    replay(zk, instance);

    keyWatcher.updateAuthKeys();

    verify(zk, instance);

    assertEquals(2, secretManager.getKeys().size());
    assertEquals(key1, secretManager.getKeys().get(key1.getKeyId()));
    assertEquals(key2, secretManager.getKeys().get(key2.getKeyId()));
  }

  @Test
  public void testDisconnectAndReconnect() throws Exception {
    lostZooKeeperBase(new WatchedEvent(EventType.None, KeeperState.Disconnected, null), new WatchedEvent(EventType.None, KeeperState.SyncConnected, null));
  }

  @Test
  public void testExpiredAndReconnect() throws Exception {
    lostZooKeeperBase(new WatchedEvent(EventType.None, KeeperState.Expired, null), new WatchedEvent(EventType.None, KeeperState.SyncConnected, null));
  }

  private void lostZooKeeperBase(WatchedEvent disconnectEvent, WatchedEvent reconnectEvent) throws Exception {

    List<String> children = Arrays.asList("1", "5");
    AuthenticationKey key1 = new AuthenticationKey(1, 0l, 10000l, keyGen.generateKey()), key2 = new AuthenticationKey(5, key1.getExpirationDate(), 20000l,
        keyGen.generateKey());

    expect(zk.exists(baseNode, keyWatcher)).andReturn(true);
    expect(zk.getChildren(baseNode, keyWatcher)).andReturn(children);
    expect(zk.getData(baseNode + "/" + key1.getKeyId(), keyWatcher, null)).andReturn(serialize(key1));
    expect(zk.getData(baseNode + "/" + key2.getKeyId(), keyWatcher, null)).andReturn(serialize(key2));

    replay(zk, instance);

    // Initialize and then get disconnected
    keyWatcher.updateAuthKeys();
    keyWatcher.process(disconnectEvent);

    verify(zk, instance);

    // We should have no auth keys when we're disconnected
    assertEquals("Secret manager should be empty after a disconnect", 0, secretManager.getKeys().size());
    assertNull("Current key should be null", secretManager.getCurrentKey());

    reset(zk, instance);

    expect(zk.exists(baseNode, keyWatcher)).andReturn(true);
    expect(zk.getChildren(baseNode, keyWatcher)).andReturn(children);
    expect(zk.getData(baseNode + "/" + key1.getKeyId(), keyWatcher, null)).andReturn(serialize(key1));
    expect(zk.getData(baseNode + "/" + key2.getKeyId(), keyWatcher, null)).andReturn(serialize(key2));

    replay(zk, instance);

    // Reconnect again, get all the keys
    keyWatcher.process(reconnectEvent);

    verify(zk, instance);

    // Verify we have both keys
    assertEquals(2, secretManager.getKeys().size());
    assertEquals(key1, secretManager.getKeys().get(key1.getKeyId()));
    assertEquals(key2, secretManager.getKeys().get(key2.getKeyId()));
  }

  private byte[] serialize(AuthenticationKey key) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    key.write(new DataOutputStream(baos));
    return baos.toByteArray();
  }
}
diff --git a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
index da0b07caa..35005d810 100644
-- a/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
++ b/server/gc/src/main/java/org/apache/accumulo/gc/SimpleGarbageCollector.java
@@ -723,7 +723,7 @@ public class SimpleGarbageCollector extends AccumuloServerContext implements Ifa
     log.debug("Starting garbage collector listening on " + result);
     try {
       return TServerUtils.startTServer(getConfiguration(), result, getThriftServerType(), processor, this.getClass().getSimpleName(), "GC Monitor Service", 2,
          getConfiguration().getCount(Property.GENERAL_SIMPLETIMER_THREADPOOL_SIZE), 1000, maxMessageSize, getServerSslParams(), getServerSaslParams(), 0).address;
          getConfiguration().getCount(Property.GENERAL_SIMPLETIMER_THREADPOOL_SIZE), 1000, maxMessageSize, getServerSslParams(), getSaslParams(), 0).address;
     } catch (Exception ex) {
       log.fatal(ex, ex);
       throw new RuntimeException(ex);
diff --git a/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java b/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java
index 1d7f90fb0..5224f28c6 100644
-- a/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java
++ b/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java
@@ -117,6 +117,13 @@ public class GarbageCollectWriteAheadLogsTest {
         return systemConfig.get((Property) args[0]);
       }
     }).anyTimes();
    EasyMock.expect(siteConfig.getBoolean(EasyMock.anyObject(Property.class))).andAnswer(new IAnswer<Boolean>() {
      @Override
      public Boolean answer() {
        Object[] args = EasyMock.getCurrentArguments();
        return systemConfig.getBoolean((Property) args[0]);
      }
    }).anyTimes();
 
     EasyMock.expect(siteConfig.iterator()).andAnswer(new IAnswer<Iterator<Entry<String,String>>>() {
       @Override
diff --git a/server/gc/src/test/java/org/apache/accumulo/gc/SimpleGarbageCollectorTest.java b/server/gc/src/test/java/org/apache/accumulo/gc/SimpleGarbageCollectorTest.java
index 6fcdd37e3..d30f00b2c 100644
-- a/server/gc/src/test/java/org/apache/accumulo/gc/SimpleGarbageCollectorTest.java
++ b/server/gc/src/test/java/org/apache/accumulo/gc/SimpleGarbageCollectorTest.java
@@ -84,6 +84,13 @@ public class SimpleGarbageCollectorTest {
         return systemConfig.get((Property) args[0]);
       }
     }).anyTimes();
    EasyMock.expect(siteConfig.getBoolean(EasyMock.anyObject(Property.class))).andAnswer(new IAnswer<Boolean>() {
      @Override
      public Boolean answer() {
        Object[] args = EasyMock.getCurrentArguments();
        return systemConfig.getBoolean((Property) args[0]);
      }
    }).anyTimes();
 
     EasyMock.expect(siteConfig.iterator()).andAnswer(new IAnswer<Iterator<Entry<String,String>>>() {
       @Override
diff --git a/server/gc/src/test/java/org/apache/accumulo/gc/replication/CloseWriteAheadLogReferencesTest.java b/server/gc/src/test/java/org/apache/accumulo/gc/replication/CloseWriteAheadLogReferencesTest.java
index 120692a4a..ba688903b 100644
-- a/server/gc/src/test/java/org/apache/accumulo/gc/replication/CloseWriteAheadLogReferencesTest.java
++ b/server/gc/src/test/java/org/apache/accumulo/gc/replication/CloseWriteAheadLogReferencesTest.java
@@ -107,6 +107,13 @@ public class CloseWriteAheadLogReferencesTest {
         return systemConf.get((Property) args[0]);
       }
     }).anyTimes();
    EasyMock.expect(siteConfig.getBoolean(EasyMock.anyObject(Property.class))).andAnswer(new IAnswer<Boolean>() {
      @Override
      public Boolean answer() {
        Object[] args = EasyMock.getCurrentArguments();
        return systemConf.getBoolean((Property) args[0]);
      }
    }).anyTimes();
 
     EasyMock.expect(siteConfig.iterator()).andAnswer(new IAnswer<Iterator<Entry<String,String>>>() {
       @Override
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index be476de2e..cc6a6ceda 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -124,6 +124,9 @@ import org.apache.accumulo.server.rpc.ThriftServerType;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
 import org.apache.accumulo.server.security.SecurityOperation;
 import org.apache.accumulo.server.security.SecurityUtil;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenKeyManager;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
import org.apache.accumulo.server.security.delegation.ZooAuthenticationKeyDistributor;
 import org.apache.accumulo.server.security.handler.ZKPermHandler;
 import org.apache.accumulo.server.tables.TableManager;
 import org.apache.accumulo.server.tables.TableObserver;
@@ -188,6 +191,11 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
   private WorkDriver replicationWorkAssigner;
   RecoveryManager recoveryManager = null;
 
  // Delegation Token classes
  private final boolean delegationTokensAvailable;
  private ZooAuthenticationKeyDistributor keyDistributor;
  private AuthenticationTokenKeyManager authenticationTokenKeyManager;

   ZooLock masterLock = null;
   private TServer clientService = null;
   TabletBalancer tabletBalancer;
@@ -560,7 +568,7 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
       throw new ThriftTableOperationException(tableId, null, TableOperation.MERGE, TableOperationExceptionType.OFFLINE, "table is not online");
   }
 
  private Master(ServerConfigurationFactory config, VolumeManager fs, String hostname) throws IOException {
  public Master(ServerConfigurationFactory config, VolumeManager fs, String hostname) throws IOException {
     super(config);
     this.serverConfig = config;
     this.fs = fs;
@@ -587,6 +595,24 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
     }
 
     this.security = AuditedSecurityOperation.getInstance(this);

    // Create the secret manager (can generate and verify delegation tokens)
    final long tokenLifetime = aconf.getTimeInMillis(Property.GENERAL_DELEGATION_TOKEN_LIFETIME);
    setSecretManager(new AuthenticationTokenSecretManager(getInstance(), tokenLifetime));

    authenticationTokenKeyManager = null;
    keyDistributor = null;
    if (getConfiguration().getBoolean(Property.INSTANCE_RPC_SASL_ENABLED)) {
      // SASL is enabled, create the key distributor (ZooKeeper) and manager (generates/rolls secret keys)
      log.info("SASL is enabled, creating delegation token key manager and distributor");
      final long tokenUpdateInterval = aconf.getTimeInMillis(Property.GENERAL_DELEGATION_TOKEN_UPDATE_INTERVAL);
      keyDistributor = new ZooAuthenticationKeyDistributor(ZooReaderWriter.getInstance(), ZooUtil.getRoot(getInstance()) + Constants.ZDELEGATION_TOKEN_KEYS);
      authenticationTokenKeyManager = new AuthenticationTokenKeyManager(getSecretManager(), keyDistributor, tokenUpdateInterval, tokenLifetime);
      delegationTokensAvailable = true;
    } else {
      log.info("SASL is not enabled, delegation tokens will not be available");
      delegationTokensAvailable = false;
    }
   }
 
   public TServerConnection getConnection(TServerInstance server) {
@@ -1096,6 +1122,25 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
 
     ZooKeeperInitialization.ensureZooKeeperInitialized(zReaderWriter, zroot);
 
    // Make sure that we have a secret key (either a new one or an old one from ZK) before we start
    // the master client service.
    if (null != authenticationTokenKeyManager && null != keyDistributor) {
      log.info("Starting delegation-token key manager");
      keyDistributor.initialize();
      authenticationTokenKeyManager.start();
      boolean logged = false;
      while (!authenticationTokenKeyManager.isInitialized()) {
        // Print out a status message when we start waiting for the key manager to get initialized
        if (!logged) {
          log.info("Waiting for AuthenticationTokenKeyManager to be initialized");
          logged = true;
        }
        UtilWaitThread.sleep(200);
      }
      // And log when we are initialized
      log.info("AuthenticationTokenSecretManager is initialized");
    }

     clientHandler = new MasterClientServiceHandler(this);
     Iface rpcProxy = RpcWrapper.service(clientHandler);
     final Processor<Iface> processor;
@@ -1162,6 +1207,9 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
     replicationWorkAssigner.join(remaining(deadline));
     replicationWorkDriver.join(remaining(deadline));
     replAddress.server.stop();
    // Signal that we want it to stop, and wait for it to do so.
    authenticationTokenKeyManager.gracefulStop();
    authenticationTokenKeyManager.join(remaining(deadline));
 
     // quit, even if the tablet servers somehow jam up and the watchers
     // don't stop
@@ -1476,4 +1524,11 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
     result.deadTabletServers = obit.getList();
     return result;
   }

  /**
   * Can delegation tokens be generated for users
   */
  public boolean delegationTokensAvailable() {
    return delegationTokensAvailable;
  }
 }
diff --git a/server/master/src/main/java/org/apache/accumulo/master/MasterClientServiceHandler.java b/server/master/src/main/java/org/apache/accumulo/master/MasterClientServiceHandler.java
index 72cba26a7..3809a2981 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/MasterClientServiceHandler.java
++ b/server/master/src/main/java/org/apache/accumulo/master/MasterClientServiceHandler.java
@@ -33,6 +33,8 @@ import org.apache.accumulo.core.client.IsolatedScanner;
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.impl.DelegationTokenConfigSerializer;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
 import org.apache.accumulo.core.client.impl.thrift.TableOperation;
@@ -55,8 +57,11 @@ import org.apache.accumulo.core.metadata.RootTable;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.LogColumnFamily;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.security.thrift.TDelegationToken;
import org.apache.accumulo.core.security.thrift.TDelegationTokenConfig;
 import org.apache.accumulo.core.trace.thrift.TInfo;
 import org.apache.accumulo.core.util.ByteBufferUtil;
 import org.apache.accumulo.core.util.UtilWaitThread;
@@ -69,12 +74,14 @@ import org.apache.accumulo.server.master.LiveTServerSet.TServerConnection;
 import org.apache.accumulo.server.master.balancer.DefaultLoadBalancer;
 import org.apache.accumulo.server.master.balancer.TabletBalancer;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
 import org.apache.accumulo.server.util.NamespacePropUtil;
 import org.apache.accumulo.server.util.SystemPropUtil;
 import org.apache.accumulo.server.util.TablePropUtil;
 import org.apache.accumulo.server.util.TabletIterator.TabletDeletedException;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.token.Token;
 import org.apache.log4j.Logger;
 import org.apache.thrift.TException;
 import org.apache.zookeeper.KeeperException;
@@ -445,4 +452,27 @@ class MasterClientServiceHandler extends FateServiceHandler implements MasterCli
 
     return servers;
   }

  @Override
  public TDelegationToken getDelegationToken(TInfo tinfo, TCredentials credentials, TDelegationTokenConfig tConfig) throws ThriftSecurityException, TException {
    if (!master.security.canObtainDelegationToken(credentials)) {
      throw new ThriftSecurityException(credentials.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
    }

    // Make sure we're actually generating the secrets to make delegation tokens
    // Round-about way to verify that SASL is also enabled.
    if (!master.delegationTokensAvailable()) {
      throw new TException("Delegation tokens are not available for use");
    }

    final DelegationTokenConfig config = DelegationTokenConfigSerializer.deserialize(tConfig);
    final AuthenticationTokenSecretManager secretManager = master.getSecretManager();
    try {
      Entry<Token<AuthenticationTokenIdentifier>,AuthenticationTokenIdentifier> pair = secretManager.generateToken(credentials.principal, config);

      return new TDelegationToken(ByteBuffer.wrap(pair.getKey().getPassword()), pair.getValue().getThriftIdentifier());
    } catch (Exception e) {
      throw new TException(e.getMessage());
    }
  }
 }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index a5675dcaf..662ee313b 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -180,6 +180,8 @@ import org.apache.accumulo.server.rpc.ThriftServerType;
 import org.apache.accumulo.server.security.AuditedSecurityOperation;
 import org.apache.accumulo.server.security.SecurityOperation;
 import org.apache.accumulo.server.security.SecurityUtil;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
import org.apache.accumulo.server.security.delegation.ZooAuthenticationKeyWatcher;
 import org.apache.accumulo.server.util.FileSystemMonitor;
 import org.apache.accumulo.server.util.Halt;
 import org.apache.accumulo.server.util.MasterMetadataUtil;
@@ -312,6 +314,8 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
   private final AtomicLong totalMinorCompactions = new AtomicLong(0);
   private final ServerConfigurationFactory confFactory;
 
  private final ZooAuthenticationKeyWatcher authKeyWatcher;

   public TabletServer(ServerConfigurationFactory confFactory, VolumeManager fs) {
     super(confFactory);
     this.confFactory = confFactory;
@@ -356,6 +360,17 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
         TabletLocator.clearLocators();
       }
     }, jitter(TIME_BETWEEN_LOCATOR_CACHE_CLEARS), jitter(TIME_BETWEEN_LOCATOR_CACHE_CLEARS));

    // Create the secret manager
    setSecretManager(new AuthenticationTokenSecretManager(instance, aconf.getTimeInMillis(Property.GENERAL_DELEGATION_TOKEN_LIFETIME)));
    if (aconf.getBoolean(Property.INSTANCE_RPC_SASL_ENABLED)) {
      log.info("SASL is enabled, creating ZooKeeper watcher for AuthenticationKeys");
      // Watcher to notice new AuthenticationKeys which enable delegation tokens
      authKeyWatcher = new ZooAuthenticationKeyWatcher(getSecretManager(), ZooReaderWriter.getInstance(), ZooUtil.getRoot(instance)
          + Constants.ZDELEGATION_TOKEN_KEYS);
    } else {
      authKeyWatcher = null;
    }
   }
 
   private static long jitter(long ms) {
@@ -2421,6 +2436,17 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
       log.error("Error registering with JMX", e);
     }
 
    if (null != authKeyWatcher) {
      log.info("Seeding ZooKeeper watcher for authentication keys");
      try {
        authKeyWatcher.updateAuthKeys();
      } catch (KeeperException | InterruptedException e) {
        // TODO Does there need to be a better check? What are the error conditions that we'd fall out here? AUTH_FAILURE?
        // If we get the error, do we just put it on a timer and retry the exists(String, Watcher) call?
        log.error("Failed to perform initial check for authentication tokens in ZooKeeper. Delegation token authentication will be unavailable.", e);
      }
    }

     try {
       clientAddress = startTabletClientService();
     } catch (UnknownHostException e1) {
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousBatchWalker.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousBatchWalker.java
index a2687bb1f..c8a114370 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousBatchWalker.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousBatchWalker.java
@@ -26,6 +26,7 @@ import java.util.Set;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.cli.BatchScannerOpts;
import org.apache.accumulo.core.cli.ClientOnDefaultTable;
 import org.apache.accumulo.core.cli.ScannerOpts;
 import org.apache.accumulo.core.client.BatchScanner;
 import org.apache.accumulo.core.client.Connector;
@@ -52,16 +53,17 @@ public class ContinuousBatchWalker {
     Opts opts = new Opts();
     ScannerOpts scanOpts = new ScannerOpts();
     BatchScannerOpts bsOpts = new BatchScannerOpts();
    opts.parseArgs(ContinuousBatchWalker.class.getName(), args, scanOpts, bsOpts);
    ClientOnDefaultTable clientOpts = new ClientOnDefaultTable("ci");
    clientOpts.parseArgs(ContinuousBatchWalker.class.getName(), args, scanOpts, bsOpts, opts);
 
     Random r = new Random();
     Authorizations auths = opts.randomAuths.getAuths(r);
 
    Connector conn = opts.getConnector();
    Scanner scanner = ContinuousUtil.createScanner(conn, opts.getTableName(), auths);
    Connector conn = clientOpts.getConnector();
    Scanner scanner = ContinuousUtil.createScanner(conn, clientOpts.getTableName(), auths);
     scanner.setBatchSize(scanOpts.scanBatchSize);
 
    BatchScanner bs = conn.createBatchScanner(opts.getTableName(), auths, bsOpts.scanThreads);
    BatchScanner bs = conn.createBatchScanner(clientOpts.getTableName(), auths, bsOpts.scanThreads);
     bs.setTimeout(bsOpts.scanTimeout, TimeUnit.MILLISECONDS);
 
     while (true) {
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousIngest.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousIngest.java
index dba6ac959..ddc36aaeb 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousIngest.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousIngest.java
@@ -19,7 +19,6 @@ package org.apache.accumulo.test.continuous;
 import static java.nio.charset.StandardCharsets.UTF_8;
 
 import java.io.BufferedReader;
import java.io.IOException;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.Collections;
@@ -29,9 +28,8 @@ import java.util.UUID;
 import java.util.zip.CRC32;
 import java.util.zip.Checksum;
 
import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.cli.BatchWriterOpts;
import org.apache.accumulo.core.cli.MapReduceClientOnDefaultTable;
import org.apache.accumulo.core.cli.ClientOnDefaultTable;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.MutationsRejectedException;
@@ -46,75 +44,14 @@ import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.Text;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
 
 public class ContinuousIngest {
 
  static public class BaseOpts extends MapReduceClientOnDefaultTable {
    public class DebugConverter implements IStringConverter<String> {
      @Override
      public String convert(String debugLog) {
        Logger logger = Logger.getLogger(Constants.CORE_PACKAGE_NAME);
        logger.setLevel(Level.TRACE);
        logger.setAdditivity(false);
        try {
          logger.addAppender(new FileAppender(new PatternLayout("%d{dd HH:mm:ss,SSS} [%-8c{2}] %-5p: %m%n"), debugLog, true));
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        return debugLog;
      }
    }

    @Parameter(names = "--min", description = "lowest random row number to use")
    long min = 0;

    @Parameter(names = "--max", description = "maximum random row number to use")
    long max = Long.MAX_VALUE;

    @Parameter(names = "--debugLog", description = "file to write debugging output", converter = DebugConverter.class)
    String debugLog = null;

    BaseOpts() {
      super("ci");
    }
  }

  public static class ShortConverter implements IStringConverter<Short> {
    @Override
    public Short convert(String value) {
      return Short.valueOf(value);
    }
  }

  static public class Opts extends BaseOpts {
    @Parameter(names = "--num", description = "the number of entries to ingest")
    long num = Long.MAX_VALUE;

    @Parameter(names = "--maxColF", description = "maximum column family value to use", converter = ShortConverter.class)
    short maxColF = Short.MAX_VALUE;

    @Parameter(names = "--maxColQ", description = "maximum column qualifier value to use", converter = ShortConverter.class)
    short maxColQ = Short.MAX_VALUE;

    @Parameter(names = "--addCheckSum", description = "turn on checksums")
    boolean checksum = false;

    @Parameter(names = "--visibilities", description = "read the visibilities to ingest with from a file")
    String visFile = null;
  }

   private static final byte[] EMPTY_BYTES = new byte[0];
 
   private static List<ColumnVisibility> visibilities;
 
  private static void initVisibilities(Opts opts) throws Exception {
  private static void initVisibilities(ContinuousOpts opts) throws Exception {
     if (opts.visFile == null) {
       visibilities = Collections.singletonList(new ColumnVisibility());
       return;
@@ -140,22 +77,23 @@ public class ContinuousIngest {
 
   public static void main(String[] args) throws Exception {
 
    Opts opts = new Opts();
    ContinuousOpts opts = new ContinuousOpts();
     BatchWriterOpts bwOpts = new BatchWriterOpts();
    opts.parseArgs(ContinuousIngest.class.getName(), args, bwOpts);
    ClientOnDefaultTable clientOpts = new ClientOnDefaultTable("ci");
    clientOpts.parseArgs(ContinuousIngest.class.getName(), args, bwOpts, opts);
 
     initVisibilities(opts);
 
     if (opts.min < 0 || opts.max < 0 || opts.max <= opts.min) {
       throw new IllegalArgumentException("bad min and max");
     }
    Connector conn = opts.getConnector();
    Connector conn = clientOpts.getConnector();
 
    if (!conn.tableOperations().exists(opts.getTableName())) {
      throw new TableNotFoundException(null, opts.getTableName(), "Consult the README and create the table before starting ingest.");
    if (!conn.tableOperations().exists(clientOpts.getTableName())) {
      throw new TableNotFoundException(null, clientOpts.getTableName(), "Consult the README and create the table before starting ingest.");
     }
 
    BatchWriter bw = conn.createBatchWriter(opts.getTableName(), bwOpts.getBatchWriterConfig());
    BatchWriter bw = conn.createBatchWriter(clientOpts.getTableName(), bwOpts.getBatchWriterConfig());
     bw = Trace.wrapAll(bw, new CountSampler(1024));
 
     Random r = new Random();
@@ -233,7 +171,7 @@ public class ContinuousIngest {
     }
 
     bw.close();
    opts.stopTracing();
    clientOpts.stopTracing();
   }
 
   private static long flush(BatchWriter bw, long count, final int flushInterval, long lastFlushTime) throws MutationsRejectedException {
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java
index 4b5c3e722..48154a648 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java
@@ -24,6 +24,7 @@ import java.util.Set;
 import java.util.UUID;
 
 import org.apache.accumulo.core.cli.BatchWriterOpts;
import org.apache.accumulo.core.cli.MapReduceClientOnDefaultTable;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
@@ -33,8 +34,6 @@ import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.ColumnVisibility;
 import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.test.continuous.ContinuousIngest.BaseOpts;
import org.apache.accumulo.test.continuous.ContinuousIngest.ShortConverter;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.conf.Configured;
 import org.apache.hadoop.io.Text;
@@ -116,7 +115,7 @@ public class ContinuousMoru extends Configured implements Tool {
     }
   }
 
  static class Opts extends BaseOpts {
  static class Opts extends ContinuousOpts {
     @Parameter(names = "--maxColF", description = "maximum column family value to use", converter = ShortConverter.class)
     short maxColF = Short.MAX_VALUE;
 
@@ -131,17 +130,18 @@ public class ContinuousMoru extends Configured implements Tool {
   public int run(String[] args) throws IOException, InterruptedException, ClassNotFoundException, AccumuloSecurityException {
     Opts opts = new Opts();
     BatchWriterOpts bwOpts = new BatchWriterOpts();
    opts.parseArgs(ContinuousMoru.class.getName(), args, bwOpts);
    MapReduceClientOnDefaultTable clientOpts = new MapReduceClientOnDefaultTable("ci");
    clientOpts.parseArgs(ContinuousMoru.class.getName(), args, bwOpts, opts);
 
     Job job = Job.getInstance(getConf(), this.getClass().getSimpleName() + "_" + System.currentTimeMillis());
     job.setJarByClass(this.getClass());
 
     job.setInputFormatClass(AccumuloInputFormat.class);
    opts.setAccumuloConfigs(job);
    clientOpts.setAccumuloConfigs(job);
 
     // set up ranges
     try {
      Set<Range> ranges = opts.getConnector().tableOperations().splitRangeByTablets(opts.getTableName(), new Range(), opts.maxMaps);
      Set<Range> ranges = clientOpts.getConnector().tableOperations().splitRangeByTablets(clientOpts.getTableName(), new Range(), opts.maxMaps);
       AccumuloInputFormat.setRanges(job, ranges);
       AccumuloInputFormat.setAutoAdjustRanges(job, false);
     } catch (Exception e) {
@@ -163,7 +163,7 @@ public class ContinuousMoru extends Configured implements Tool {
     conf.set(CI_ID, UUID.randomUUID().toString());
 
     job.waitForCompletion(true);
    opts.stopTracing();
    clientOpts.stopTracing();
     return job.isSuccessful() ? 0 : 1;
   }
 
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousOpts.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousOpts.java
new file mode 100644
index 000000000..48a77e7fa
-- /dev/null
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousOpts.java
@@ -0,0 +1,80 @@
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
package org.apache.accumulo.test.continuous;

import java.io.IOException;

import org.apache.accumulo.core.Constants;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

/**
 * Common CLI arguments for the Continuous Ingest suite.
 */
public class ContinuousOpts {

  public static class DebugConverter implements IStringConverter<String> {
    @Override
    public String convert(String debugLog) {
      Logger logger = Logger.getLogger(Constants.CORE_PACKAGE_NAME);
      logger.setLevel(Level.TRACE);
      logger.setAdditivity(false);
      try {
        logger.addAppender(new FileAppender(new PatternLayout("%d{dd HH:mm:ss,SSS} [%-8c{2}] %-5p: %m%n"), debugLog, true));
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      return debugLog;
    }
  }

  public static class ShortConverter implements IStringConverter<Short> {
    @Override
    public Short convert(String value) {
      return Short.valueOf(value);
    }
  }

  @Parameter(names = "--min", description = "lowest random row number to use")
  long min = 0;

  @Parameter(names = "--max", description = "maximum random row number to use")
  long max = Long.MAX_VALUE;

  @Parameter(names = "--debugLog", description = "file to write debugging output", converter = DebugConverter.class)
  String debugLog = null;

  @Parameter(names = "--num", description = "the number of entries to ingest")
  long num = Long.MAX_VALUE;

  @Parameter(names = "--maxColF", description = "maximum column family value to use", converter = ShortConverter.class)
  short maxColF = Short.MAX_VALUE;

  @Parameter(names = "--maxColQ", description = "maximum column qualifier value to use", converter = ShortConverter.class)
  short maxColQ = Short.MAX_VALUE;

  @Parameter(names = "--addCheckSum", description = "turn on checksums")
  boolean checksum = false;

  @Parameter(names = "--visibilities", description = "read the visibilities to ingest with from a file")
  String visFile = null;
}
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousQuery.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousQuery.java
index 73048f6ab..7f89a9431 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousQuery.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousQuery.java
@@ -21,20 +21,21 @@ import static java.nio.charset.StandardCharsets.UTF_8;
 import java.util.Map.Entry;
 import java.util.Random;
 
import org.apache.accumulo.core.cli.ClientOnDefaultTable;
import org.apache.accumulo.core.cli.ClientOpts.TimeConverter;
 import org.apache.accumulo.core.cli.ScannerOpts;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.test.continuous.ContinuousIngest.BaseOpts;
 import org.apache.hadoop.io.Text;
 
 import com.beust.jcommander.Parameter;
 
 public class ContinuousQuery {
 
  public static class Opts extends BaseOpts {
  public static class Opts extends ContinuousOpts {
     @Parameter(names = "--sleep", description = "the time to wait between queries", converter = TimeConverter.class)
     long sleepTime = 100;
   }
@@ -42,10 +43,11 @@ public class ContinuousQuery {
   public static void main(String[] args) throws Exception {
     Opts opts = new Opts();
     ScannerOpts scanOpts = new ScannerOpts();
    opts.parseArgs(ContinuousQuery.class.getName(), args, scanOpts);
    ClientOnDefaultTable clientOpts = new ClientOnDefaultTable("ci");
    clientOpts.parseArgs(ContinuousQuery.class.getName(), args, scanOpts, opts);
 
    Connector conn = opts.getConnector();
    Scanner scanner = ContinuousUtil.createScanner(conn, opts.getTableName(), opts.auths);
    Connector conn = clientOpts.getConnector();
    Scanner scanner = ContinuousUtil.createScanner(conn, clientOpts.getTableName(), clientOpts.auths);
     scanner.setBatchSize(scanOpts.scanBatchSize);
 
     Random r = new Random();
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousScanner.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousScanner.java
index f68377af6..a77de3d88 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousScanner.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousScanner.java
@@ -22,6 +22,7 @@ import java.util.Iterator;
 import java.util.Map.Entry;
 import java.util.Random;
 
import org.apache.accumulo.core.cli.ClientOnDefaultTable;
 import org.apache.accumulo.core.cli.ScannerOpts;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
@@ -45,15 +46,16 @@ public class ContinuousScanner {
   public static void main(String[] args) throws Exception {
     Opts opts = new Opts();
     ScannerOpts scanOpts = new ScannerOpts();
    opts.parseArgs(ContinuousScanner.class.getName(), args, scanOpts);
    ClientOnDefaultTable clientOpts = new ClientOnDefaultTable("ci");
    clientOpts.parseArgs(ContinuousScanner.class.getName(), args, scanOpts, opts);
 
     Random r = new Random();
 
     long distance = 1000000000000l;
 
    Connector conn = opts.getConnector();
    Connector conn = clientOpts.getConnector();
     Authorizations auths = opts.randomAuths.getAuths(r);
    Scanner scanner = ContinuousUtil.createScanner(conn, opts.getTableName(), auths);
    Scanner scanner = ContinuousUtil.createScanner(conn, clientOpts.getTableName(), auths);
     scanner.setBatchSize(scanOpts.scanBatchSize);
 
     double delta = Math.min(.05, .05 / (opts.numToScan / 1000.0));
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousWalk.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousWalk.java
index 60f8ec284..f2e4805e5 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousWalk.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousWalk.java
@@ -28,6 +28,7 @@ import java.util.Map.Entry;
 import java.util.Random;
 import java.util.zip.CRC32;
 
import org.apache.accumulo.core.cli.ClientOnDefaultTable;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.data.Key;
@@ -105,16 +106,17 @@ public class ContinuousWalk {
 
   public static void main(String[] args) throws Exception {
     Opts opts = new Opts();
    opts.parseArgs(ContinuousWalk.class.getName(), args);
    ClientOnDefaultTable clientOpts = new ClientOnDefaultTable("ci");
    clientOpts.parseArgs(ContinuousWalk.class.getName(), args, opts);
 
    Connector conn = opts.getConnector();
    Connector conn = clientOpts.getConnector();
 
     Random r = new Random();
 
     ArrayList<Value> values = new ArrayList<Value>();
 
     while (true) {
      Scanner scanner = ContinuousUtil.createScanner(conn, opts.getTableName(), opts.randomAuths.getAuths(r));
      Scanner scanner = ContinuousUtil.createScanner(conn, clientOpts.getTableName(), opts.randomAuths.getAuths(r));
       String row = findAStartRow(opts.min, opts.max, scanner, r);
 
       while (row != null) {
diff --git a/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java b/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java
index 06b43038f..e53d686a0 100644
-- a/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java
++ b/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java
@@ -70,7 +70,12 @@ public class MiniClusterHarness {
   }
 
   public MiniAccumuloClusterImpl create(AccumuloIT testBase, AuthenticationToken token, TestingKdc kdc) throws Exception {
    return create(testBase.getClass().getName(), testBase.testName.getMethodName(), token, kdc);
    return create(testBase, token, kdc, MiniClusterConfigurationCallback.NO_CALLBACK);
  }

  public MiniAccumuloClusterImpl create(AccumuloIT testBase, AuthenticationToken token, TestingKdc kdc, MiniClusterConfigurationCallback configCallback)
      throws Exception {
    return create(testBase.getClass().getName(), testBase.testName.getMethodName(), token, configCallback, kdc);
   }
 
   public MiniAccumuloClusterImpl create(AccumuloClusterIT testBase, AuthenticationToken token, TestingKdc kdc) throws Exception {
diff --git a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
index 929654869..3ffa40efd 100644
-- a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
++ b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
@@ -1168,7 +1168,7 @@ public class ShellServerIT extends SharedMiniClusterIT {
   @Test
   public void systempermission() throws Exception {
     ts.exec("systempermissions");
    assertEquals(11, ts.output.get().split("\n").length - 1);
    assertEquals(12, ts.output.get().split("\n").length - 1);
     ts.exec("tablepermissions", true);
     assertEquals(6, ts.output.get().split("\n").length - 1);
   }
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/KerberosIT.java b/test/src/test/java/org/apache/accumulo/test/functional/KerberosIT.java
index 3d48657e5..75b119977 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/KerberosIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/KerberosIT.java
@@ -18,16 +18,24 @@ package org.apache.accumulo.test.functional;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
 
 import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedExceptionAction;
 import java.util.Arrays;
import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
import java.util.Map;
 import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchScanner;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
@@ -35,22 +43,29 @@ import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.security.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.ColumnVisibility;
 import org.apache.accumulo.core.security.SystemPermission;
 import org.apache.accumulo.core.security.TablePermission;
 import org.apache.accumulo.harness.AccumuloIT;
import org.apache.accumulo.harness.MiniClusterConfigurationCallback;
 import org.apache.accumulo.harness.MiniClusterHarness;
 import org.apache.accumulo.harness.TestingKdc;
import org.apache.accumulo.minicluster.ServerType;
 import org.apache.accumulo.minicluster.impl.MiniAccumuloClusterImpl;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
 import org.apache.hadoop.minikdc.MiniKdc;
@@ -63,6 +78,7 @@ import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.google.common.collect.Iterables;
 import com.google.common.collect.Sets;
 
 /**
@@ -104,7 +120,17 @@ public class KerberosIT extends AccumuloIT {
   @Before
   public void startMac() throws Exception {
     MiniClusterHarness harness = new MiniClusterHarness();
    mac = harness.create(this, new PasswordToken("unused"), kdc);
    mac = harness.create(this, new PasswordToken("unused"), kdc, new MiniClusterConfigurationCallback() {

      @Override
      public void configureMiniCluster(MiniAccumuloConfigImpl cfg, Configuration coreSite) {
        Map<String,String> site = cfg.getSiteConfig();
        site.put(Property.INSTANCE_ZK_TIMEOUT.getKey(), "10s");
        cfg.setSiteConfig(site);
      }

    });

     mac.getConfig().setNumTservers(1);
     mac.start();
     // Enabled kerberos auth
@@ -133,7 +159,7 @@ public class KerberosIT extends AccumuloIT {
     }
 
     // and the ability to modify the root and metadata tables
    for (String table : Arrays.asList(RootTable.NAME, MetadataTable.NAME)){
    for (String table : Arrays.asList(RootTable.NAME, MetadataTable.NAME)) {
       assertTrue(conn.securityOperations().hasTablePermission(conn.whoami(), table, TablePermission.ALTER_TABLE));
     }
   }
@@ -304,6 +330,226 @@ public class KerberosIT extends AccumuloIT {
     assertFalse("Had more results from iterator", iter.hasNext());
   }
 
  @Test
  public void testDelegationToken() throws Exception {
    final String tableName = getUniqueNames(1)[0];

    // Login as the "root" user
    UserGroupInformation root = UserGroupInformation.loginUserFromKeytabAndReturnUGI(kdc.getClientPrincipal(), kdc.getClientKeytab().getAbsolutePath());
    log.info("Logged in as {}", kdc.getClientPrincipal());

    final int numRows = 100, numColumns = 10;

    // As the "root" user, open up the connection and get a delegation token
    final DelegationToken delegationToken = root.doAs(new PrivilegedExceptionAction<DelegationToken>() {
      @Override
      public DelegationToken run() throws Exception {
        Connector conn = mac.getConnector(kdc.getClientPrincipal(), new KerberosToken());
        log.info("Created connector as {}", kdc.getClientPrincipal());
        assertEquals(kdc.getClientPrincipal(), conn.whoami());

        conn.tableOperations().create(tableName);
        BatchWriter bw = conn.createBatchWriter(tableName, new BatchWriterConfig());
        for (int r = 0; r < numRows; r++) {
          Mutation m = new Mutation(Integer.toString(r));
          for (int c = 0; c < numColumns; c++) {
            String col = Integer.toString(c);
            m.put(col, col, col);
          }
          bw.addMutation(m);
        }
        bw.close();

        return conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
      }
    });

    // The above login with keytab doesn't have a way to logout, so make a fake user that won't have krb credentials
    UserGroupInformation userWithoutPrivs = UserGroupInformation.createUserForTesting("fake_user", new String[0]);
    int recordsSeen = userWithoutPrivs.doAs(new PrivilegedExceptionAction<Integer>() {
      @Override
      public Integer run() throws Exception {
        Connector conn = mac.getConnector(kdc.getClientPrincipal(), delegationToken);

        BatchScanner bs = conn.createBatchScanner(tableName, Authorizations.EMPTY, 2);
        bs.setRanges(Collections.singleton(new Range()));
        int recordsSeen = Iterables.size(bs);
        bs.close();
        return recordsSeen;
      }
    });

    assertEquals(numRows * numColumns, recordsSeen);
  }

  @Test
  public void testDelegationTokenAsDifferentUser() throws Exception {
    // Login as the "root" user
    UserGroupInformation.loginUserFromKeytab(kdc.getClientPrincipal(), kdc.getClientKeytab().getAbsolutePath());
    log.info("Logged in as {}", kdc.getClientPrincipal());

    // As the "root" user, open up the connection and get a delegation token
    Connector conn = mac.getConnector(kdc.getClientPrincipal(), new KerberosToken());
    log.info("Created connector as {}", kdc.getClientPrincipal());
    assertEquals(kdc.getClientPrincipal(), conn.whoami());
    final DelegationToken delegationToken = conn.securityOperations().getDelegationToken(new DelegationTokenConfig());

    // The above login with keytab doesn't have a way to logout, so make a fake user that won't have krb credentials
    UserGroupInformation userWithoutPrivs = UserGroupInformation.createUserForTesting("fake_user", new String[0]);
    try {
      // Use the delegation token to try to log in as a different user
      userWithoutPrivs.doAs(new PrivilegedExceptionAction<Void>() {
        @Override
        public Void run() throws Exception {
          mac.getConnector("some_other_user", delegationToken);
          return null;
        }
      });
      fail("Using a delegation token as a different user should throw an exception");
    } catch (UndeclaredThrowableException e) {
      Throwable cause = e.getCause();
      assertNotNull(cause);
      // We should get an AccumuloSecurityException from trying to use a delegation token for the wrong user
      assertTrue("Expected cause to be AccumuloSecurityException, but was " + cause.getClass(), cause instanceof AccumuloSecurityException);
    }
  }

  @Test(expected = AccumuloSecurityException.class)
  public void testGetDelegationTokenDenied() throws Exception {
    String newUser = testName.getMethodName();
    final File newUserKeytab = new File(kdc.getKeytabDir(), newUser + ".keytab");
    if (newUserKeytab.exists()) {
      newUserKeytab.delete();
    }

    // Create a new user
    kdc.createPrincipal(newUserKeytab, newUser);

    newUser = kdc.qualifyUser(newUser);

    // Login as a normal user
    UserGroupInformation.loginUserFromKeytab(newUser, newUserKeytab.getAbsolutePath());

    // As the "root" user, open up the connection and get a delegation token
    Connector conn = mac.getConnector(newUser, new KerberosToken());
    log.info("Created connector as {}", newUser);
    assertEquals(newUser, conn.whoami());

    conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
  }

  @Test
  public void testRestartedMasterReusesSecretKey() throws Exception {
    // Login as the "root" user
    UserGroupInformation root = UserGroupInformation.loginUserFromKeytabAndReturnUGI(kdc.getClientPrincipal(), kdc.getClientKeytab().getAbsolutePath());
    log.info("Logged in as {}", kdc.getClientPrincipal());

    // As the "root" user, open up the connection and get a delegation token
    final DelegationToken delegationToken1 = root.doAs(new PrivilegedExceptionAction<DelegationToken>() {
      @Override
      public DelegationToken run() throws Exception {
        Connector conn = mac.getConnector(kdc.getClientPrincipal(), new KerberosToken());
        log.info("Created connector as {}", kdc.getClientPrincipal());
        assertEquals(kdc.getClientPrincipal(), conn.whoami());

        DelegationToken token = conn.securityOperations().getDelegationToken(new DelegationTokenConfig());

        assertTrue("Could not get tables with delegation token", mac.getConnector(kdc.getClientPrincipal(), token).tableOperations().list().size() > 0);

        return token;
      }
    });

    log.info("Stopping master");
    mac.getClusterControl().stop(ServerType.MASTER);
    Thread.sleep(5000);
    log.info("Restarting master");
    mac.getClusterControl().start(ServerType.MASTER);

    // Make sure our original token is still good
    root.doAs(new PrivilegedExceptionAction<Void>() {
      @Override
      public Void run() throws Exception {
        Connector conn = mac.getConnector(kdc.getClientPrincipal(), delegationToken1);

        assertTrue("Could not get tables with delegation token", conn.tableOperations().list().size() > 0);

        return null;
      }
    });

    // Get a new token, so we can compare the keyId on the second to the first
    final DelegationToken delegationToken2 = root.doAs(new PrivilegedExceptionAction<DelegationToken>() {
      @Override
      public DelegationToken run() throws Exception {
        Connector conn = mac.getConnector(kdc.getClientPrincipal(), new KerberosToken());
        log.info("Created connector as {}", kdc.getClientPrincipal());
        assertEquals(kdc.getClientPrincipal(), conn.whoami());

        DelegationToken token = conn.securityOperations().getDelegationToken(new DelegationTokenConfig());

        assertTrue("Could not get tables with delegation token", mac.getConnector(kdc.getClientPrincipal(), token).tableOperations().list().size() > 0);

        return token;
      }
    });

    // A restarted master should reuse the same secret key after a restart if the secret key hasn't expired (1day by default)
    assertEquals(delegationToken1.getIdentifier().getKeyId(), delegationToken2.getIdentifier().getKeyId());
  }

  @Test(expected = AccumuloException.class)
  public void testDelegationTokenWithInvalidLifetime() throws Throwable {
    // Login as the "root" user
    UserGroupInformation root = UserGroupInformation.loginUserFromKeytabAndReturnUGI(kdc.getClientPrincipal(), kdc.getClientKeytab().getAbsolutePath());
    log.info("Logged in as {}", kdc.getClientPrincipal());

    // As the "root" user, open up the connection and get a delegation token
    try {
      root.doAs(new PrivilegedExceptionAction<DelegationToken>() {
        @Override
        public DelegationToken run() throws Exception {
          Connector conn = mac.getConnector(kdc.getClientPrincipal(), new KerberosToken());
          log.info("Created connector as {}", kdc.getClientPrincipal());
          assertEquals(kdc.getClientPrincipal(), conn.whoami());

          // Should fail
          return conn.securityOperations().getDelegationToken(new DelegationTokenConfig().setTokenLifetime(Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        }
      });
    } catch (UndeclaredThrowableException e) {
      Throwable cause = e.getCause();
      if (null != cause) {
        throw cause;
      } else {
        throw e;
      }
    }
  }

  @Test
  public void testDelegationTokenWithReducedLifetime() throws Throwable {
    // Login as the "root" user
    UserGroupInformation root = UserGroupInformation.loginUserFromKeytabAndReturnUGI(kdc.getClientPrincipal(), kdc.getClientKeytab().getAbsolutePath());
    log.info("Logged in as {}", kdc.getClientPrincipal());

    // As the "root" user, open up the connection and get a delegation token
    final DelegationToken dt = root.doAs(new PrivilegedExceptionAction<DelegationToken>() {
      @Override
      public DelegationToken run() throws Exception {
        Connector conn = mac.getConnector(kdc.getClientPrincipal(), new KerberosToken());
        log.info("Created connector as {}", kdc.getClientPrincipal());
        assertEquals(kdc.getClientPrincipal(), conn.whoami());

        return conn.securityOperations().getDelegationToken(new DelegationTokenConfig().setTokenLifetime(5, TimeUnit.MINUTES));
      }
    });

    AuthenticationTokenIdentifier identifier = dt.getIdentifier();
    assertTrue("Expected identifier to expire in no more than 5 minutes: " + identifier,
        identifier.getExpirationDate() - identifier.getIssueDate() <= (5 * 60 * 1000));
  }

   /**
    * Creates a table, adds a record to it, and then compacts the table. A simple way to make sure that the system user exists (since the master does an RPC to
    * the tserver which will create the system user if it doesn't already exist).
- 
2.19.1.windows.1

