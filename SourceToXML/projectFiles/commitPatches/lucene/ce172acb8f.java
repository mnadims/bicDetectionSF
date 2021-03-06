From ce172acb8fec6c3bbb18837a4d640da6c5aad649 Mon Sep 17 00:00:00 2001
From: markrmiller <markrmiller@apache.org>
Date: Fri, 1 Apr 2016 12:21:59 -0400
Subject: [PATCH] SOLR-4509: Move to non deprecated HttpClient impl classes to
 remove stale connection check on every request and move connection lifecycle
 management towards the client.

--
 solr/CHANGES.txt                              |  28 +-
 .../solrj/embedded/JettySolrRunner.java       |  25 +-
 .../org/apache/solr/core/BlobRepository.java  |   3 +-
 .../org/apache/solr/core/CoreContainer.java   |  78 ++--
 .../org/apache/solr/handler/IndexFetcher.java |  44 +-
 .../solr/handler/ReplicationHandler.java      |  23 +-
 .../component/HttpShardHandlerFactory.java    |  45 +-
 .../component/IterativeMergeStrategy.java     |   3 -
 .../solr/security/AuthenticationPlugin.java   |   3 -
 ...ugin.java => HttpClientBuilderPlugin.java} |  17 +-
 .../apache/solr/security/KerberosPlugin.java  |  17 +-
 .../security/PKIAuthenticationPlugin.java     |  45 +-
 .../org/apache/solr/servlet/HttpSolrCall.java |   3 +-
 .../solr/update/UpdateShardHandler.java       |  57 +--
 .../java/org/apache/solr/util/SolrCLI.java    |  23 +-
 solr/core/src/test-files/log4j.properties     |   4 +-
 .../apache/solr/TestDistributedSearch.java    |  12 +-
 .../client/solrj/ConnectionReuseTest.java     | 195 ++++----
 .../solrj/embedded/TestJettySolrRunner.java   |  12 +-
 .../solr/cloud/BaseCdcrDistributedZkTest.java |   9 +-
 .../solr/cloud/BasicDistributedZkTest.java    |  29 +-
 .../cloud/ChaosMonkeyNothingIsSafeTest.java   |   8 +-
 .../CollectionsAPIDistributedZkTest.java      |  12 +-
 .../apache/solr/cloud/HttpPartitionTest.java  |  10 +-
 .../LeaderFailoverAfterPartitionTest.java     |   9 +-
 .../apache/solr/cloud/SSLMigrationTest.java   |   2 +-
 .../org/apache/solr/cloud/ShardSplitTest.java |   1 -
 .../cloud/TestAuthenticationFramework.java    |  52 +--
 .../solr/cloud/TestCloudDeleteByQuery.java    |  12 +-
 .../cloud/TestMiniSolrCloudClusterBase.java   |   2 +-
 .../cloud/TestRandomRequestDistribution.java  | 104 +++--
 .../cloud/TestSolrCloudWithKerberosAlt.java   |  13 +-
 .../TestTolerantUpdateProcessorCloud.java     |  17 +-
 ...estTolerantUpdateProcessorRandomCloud.java |  58 ++-
 .../apache/solr/cloud/ZkControllerTest.java   |   9 +-
 .../cloud/overseer/ZkStateReaderTest.java     |   5 +-
 .../solr/core/OpenCloseCoreStressTest.java    |   2 -
 .../apache/solr/core/TestCoreContainer.java   |   1 -
 .../solr/handler/TestReplicationHandler.java  |   2 -
 .../handler/TestReplicationHandlerBackup.java |   2 -
 .../apache/solr/handler/TestRestoreCore.java  |   2 -
 ...istributedQueryElevationComponentTest.java |   1 +
 .../search/AnalyticsMergeStrategyTest.java    |   2 +
 .../solr/search/stats/TestDistribIDF.java     |  82 ++--
 .../security/BasicAuthIntegrationTest.java    |  41 +-
 .../PKIAuthenticationIntegrationTest.java     |   2 -
 .../security/TestAuthorizationFramework.java  |   3 +-
 .../apache/solr/update/AutoCommitTest.java    |   2 +-
 solr/server/etc/jetty-http.xml                |   2 +-
 solr/server/etc/jetty-https.xml               |   2 +-
 solr/server/etc/jetty.xml                     |   2 +-
 .../client/solrj/impl/CloudSolrClient.java    |   8 +
 .../impl/ConcurrentUpdateSolrClient.java      |  20 +-
 .../solrj/impl/HttpClientConfigurer.java      | 100 ----
 .../client/solrj/impl/HttpClientUtil.java     | 438 +++++++++---------
 .../client/solrj/impl/HttpSolrClient.java     | 122 ++---
 ...igurer.java => Krb5HttpClientBuilder.java} |  81 +++-
 .../client/solrj/impl/LBHttpSolrClient.java   |  33 +-
 .../solrj/impl/SolrHttpClientBuilder.java     |  91 ++++
 .../impl/SolrHttpClientContextBuilder.java    |  96 ++++
 solr/solrj/src/test-files/log4j.properties    |   2 +
 .../client/solrj/SolrExampleBinaryTest.java   |   2 -
 .../solr/client/solrj/SolrExampleXMLTest.java |   2 -
 .../solr/client/solrj/SolrExceptionTest.java  |   9 +-
 .../solrj/SolrSchemalessExampleTest.java      |   7 +-
 .../client/solrj/TestLBHttpSolrClient.java    |  15 +-
 .../solrj/embedded/JettyWebappTest.java       |   3 +-
 .../solrj/embedded/SolrExampleJettyTest.java  |   3 +-
 .../solrj/impl/BasicHttpSolrClientTest.java   |  87 ++--
 .../solrj/impl/CloudSolrClientTest.java       |  10 +-
 .../solrj/impl/ExternalHttpClientTest.java    |  75 ---
 .../client/solrj/impl/HttpClientUtilTest.java | 162 -------
 .../solrj/impl/LBHttpSolrClientTest.java      |  17 +-
 .../solr/client/solrj/request/SchemaTest.java |   9 +-
 .../solr/BaseDistributedSearchTestCase.java   |   7 -
 .../org/apache/solr/SolrJettyTestBase.java    |  18 +-
 .../java/org/apache/solr/SolrTestCaseJ4.java  |   7 +-
 .../cloud/AbstractFullDistribZkTestBase.java  |   4 -
 .../org/apache/solr/util/RestTestHarness.java |   4 +-
 .../org/apache/solr/util/SSLTestConfig.java   |  79 +++-
 80 files changed, 1347 insertions(+), 1301 deletions(-)
 rename solr/core/src/java/org/apache/solr/security/{HttpClientInterceptorPlugin.java => HttpClientBuilderPlugin.java} (63%)
 delete mode 100644 solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientConfigurer.java
 rename solr/solrj/src/java/org/apache/solr/client/solrj/impl/{Krb5HttpClientConfigurer.java => Krb5HttpClientBuilder.java} (67%)
 create mode 100644 solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientBuilder.java
 create mode 100644 solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientContextBuilder.java
 delete mode 100644 solr/solrj/src/test/org/apache/solr/client/solrj/impl/ExternalHttpClientTest.java
 delete mode 100644 solr/solrj/src/test/org/apache/solr/client/solrj/impl/HttpClientUtilTest.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 326beecbb27..5ab52b29542 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -17,7 +17,33 @@ servlet container in the directory named "example".
 See the Quick Start guide at http://lucene.apache.org/solr/quickstart.html
 
 ==================  7.0.0 ==================
(No Changes)

Upgrading from Solr 5.x
----------------------

* HttpClientInterceptorPlugin is now HttpClientBuilderPlugin and must work with a 
  SolrHttpClientBuilder rather than an HttpClientConfigurer.
  
* HttpClientUtil now allows configuring HttpClient instances via SolrHttpClientBuilder
  rather than an HttpClientConfigurer.

* SolrClient implementations now use their own internal configuration for socket timeouts,
  connect timeouts, and allowing redirects rather than what is set as the default when
  building the HttpClient instance. Use the appropriate setters on the SolrClient instance.
  
* HttpSolrClient#setAllowCompression has been removed and compression must be enabled as
  a constructor param. 
  
* HttpSolrClient#setDefaultMaxConnectionsPerHost and
  HttpSolrClient#setMaxTotalConnections have been removed. These now default very
  high and can only be changed via param when creating an HttpClient instance.
  
Optimizations
----------------------

* SOLR-4509: Move to non deprecated HttpClient impl classes to remove stale connection 
  check on every request and move connection lifecycle management towards the client.
  (Ryan Zezeski, Mark Miller, Shawn Heisey, Steve Davids)
 
 ==================  6.1.0 ==================
 
diff --git a/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java b/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java
index 88ea5677aef..871fe4cf819 100644
-- a/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java
++ b/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java
@@ -35,7 +35,6 @@ import java.util.LinkedList;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Random;
import java.util.SortedMap;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicLong;
 
@@ -44,7 +43,6 @@ import org.apache.solr.servlet.SolrDispatchFilter;
 import org.eclipse.jetty.server.Connector;
 import org.eclipse.jetty.server.HttpConfiguration;
 import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LowResourceMonitor;
 import org.eclipse.jetty.server.SecureRequestCustomizer;
 import org.eclipse.jetty.server.Server;
 import org.eclipse.jetty.server.ServerConnector;
@@ -71,6 +69,10 @@ public class JettySolrRunner {
 
   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
  private static final int THREAD_POOL_MAX_THREADS = 10000;
  // NOTE: needs to be larger than SolrHttpClient.threadPoolSweeperMaxIdleTime
  private static final int THREAD_POOL_MAX_IDLE_TIME_MS = 120000;
  
   Server server;
 
   FilterHolder dispatchFilter;
@@ -161,8 +163,8 @@ public class JettySolrRunner {
   private void init(int port) {
 
     QueuedThreadPool qtp = new QueuedThreadPool();
    qtp.setMaxThreads(10000);
    qtp.setIdleTimeout((int) TimeUnit.SECONDS.toMillis(5));
    qtp.setMaxThreads(THREAD_POOL_MAX_THREADS);
    qtp.setIdleTimeout(THREAD_POOL_MAX_IDLE_TIME_MS);
     qtp.setStopTimeout((int) TimeUnit.MINUTES.toMillis(1));
     server = new Server(qtp);
     server.manage(qtp);
@@ -179,7 +181,7 @@ public class JettySolrRunner {
       // talking to that server, but for the purposes of testing that should 
       // be good enough
       final SslContextFactory sslcontext = SSLConfig.createContextFactory(config.sslConfig);

      
       ServerConnector connector;
       if (sslcontext != null) {
         HttpConfiguration configuration = new HttpConfiguration();
@@ -192,21 +194,18 @@ public class JettySolrRunner {
       }
 
       connector.setReuseAddress(true);
      connector.setSoLingerTime(0);
      connector.setSoLingerTime(-1);
       connector.setPort(port);
       connector.setHost("127.0.0.1");

      // Enable Low Resources Management
      LowResourceMonitor lowResources = new LowResourceMonitor(server);
      lowResources.setLowResourcesIdleTimeout(1500);
      lowResources.setMaxConnections(10000);
      server.addBean(lowResources);

      connector.setIdleTimeout(THREAD_POOL_MAX_IDLE_TIME_MS);
      
       server.setConnectors(new Connector[] {connector});
       server.setSessionIdManager(new HashSessionIdManager(new Random()));
     } else {
       ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
       connector.setPort(port);
      connector.setSoLingerTime(-1);
      connector.setIdleTimeout(THREAD_POOL_MAX_IDLE_TIME_MS);
       server.setConnectors(new Connector[] {connector});
     }
 
diff --git a/solr/core/src/java/org/apache/solr/core/BlobRepository.java b/solr/core/src/java/org/apache/solr/core/BlobRepository.java
index 67398269113..09461f07b81 100644
-- a/solr/core/src/java/org/apache/solr/core/BlobRepository.java
++ b/solr/core/src/java/org/apache/solr/core/BlobRepository.java
@@ -38,6 +38,7 @@ import java.util.zip.ZipInputStream;
 import org.apache.http.HttpResponse;
 import org.apache.http.client.HttpClient;
 import org.apache.http.client.methods.HttpGet;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.cloud.ClusterState;
 import org.apache.solr.common.cloud.DocCollection;
@@ -116,7 +117,7 @@ public class BlobRepository {
         HttpGet httpGet = new HttpGet(url);
         ByteBuffer b;
         try {
          HttpResponse entity = httpClient.execute(httpGet);
          HttpResponse entity = httpClient.execute(httpGet, HttpClientUtil.createNewHttpClientRequestContext());
           int statusCode = entity.getStatusLine().getStatusCode();
           if (statusCode != 200) {
             throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "no such blob or version available: " + key);
diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index c140fb4dba8..4d57ad1d054 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -16,6 +16,17 @@
  */
 package org.apache.solr.core;
 
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.EMPTY_MAP;
import static org.apache.solr.common.params.CommonParams.AUTHC_PATH;
import static org.apache.solr.common.params.CommonParams.AUTHZ_PATH;
import static org.apache.solr.common.params.CommonParams.COLLECTIONS_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.CONFIGSETS_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.CORES_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.INFO_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.ZK_PATH;
import static org.apache.solr.security.AuthenticationPlugin.AUTHENTICATION_PLUGIN_PROP;

 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.nio.file.Path;
@@ -32,10 +43,14 @@ import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.config.Lookup;
 import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.client.solrj.impl.SolrHttpClientContextBuilder;
import org.apache.solr.client.solrj.impl.SolrHttpClientContextBuilder.AuthSchemeRegistryProvider;
import org.apache.solr.client.solrj.impl.SolrHttpClientContextBuilder.CredentialsProviderProvider;
 import org.apache.solr.client.solrj.util.SolrIdentifierValidator;
 import org.apache.solr.cloud.Overseer;
 import org.apache.solr.cloud.ZkController;
@@ -52,14 +67,13 @@ import org.apache.solr.handler.admin.CoreAdminHandler;
 import org.apache.solr.handler.admin.InfoHandler;
 import org.apache.solr.handler.admin.SecurityConfHandler;
 import org.apache.solr.handler.admin.ZookeeperInfoHandler;
import org.apache.solr.handler.component.HttpShardHandlerFactory;
 import org.apache.solr.handler.component.ShardHandlerFactory;
 import org.apache.solr.logging.LogWatcher;
 import org.apache.solr.logging.MDCLoggingContext;
 import org.apache.solr.request.SolrRequestHandler;
 import org.apache.solr.security.AuthenticationPlugin;
 import org.apache.solr.security.AuthorizationPlugin;
import org.apache.solr.security.HttpClientInterceptorPlugin;
import org.apache.solr.security.HttpClientBuilderPlugin;
 import org.apache.solr.security.PKIAuthenticationPlugin;
 import org.apache.solr.security.SecurityPluginHolder;
 import org.apache.solr.update.SolrCoreState;
@@ -69,16 +83,8 @@ import org.apache.zookeeper.KeeperException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.EMPTY_MAP;
import static org.apache.solr.common.params.CommonParams.AUTHC_PATH;
import static org.apache.solr.common.params.CommonParams.AUTHZ_PATH;
import static org.apache.solr.common.params.CommonParams.COLLECTIONS_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.CONFIGSETS_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.CORES_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.INFO_HANDLER_PATH;
import static org.apache.solr.common.params.CommonParams.ZK_PATH;
import static org.apache.solr.security.AuthenticationPlugin.AUTHENTICATION_PLUGIN_PROP;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
 
 
 /**
@@ -291,7 +297,7 @@ public class CoreContainer {
     }
     if (authenticationPlugin != null) {
       authenticationPlugin.plugin.init(authenticationConfig);
      addHttpConfigurer(authenticationPlugin.plugin);
      setupHttpClientForAuthPlugin(authenticationPlugin.plugin);
     }
     this.authenticationPlugin = authenticationPlugin;
     try {
@@ -300,26 +306,44 @@ public class CoreContainer {
 
   }
 
  private void addHttpConfigurer(Object authcPlugin) {
    if (authcPlugin instanceof HttpClientInterceptorPlugin) {
      // Setup HttpClient to use the plugin's configurer for internode communication
      HttpClientConfigurer configurer = ((HttpClientInterceptorPlugin) authcPlugin).getClientConfigurer();
      HttpClientUtil.setConfigurer(configurer);

  private void setupHttpClientForAuthPlugin(Object authcPlugin) {
    if (authcPlugin instanceof HttpClientBuilderPlugin) {
      // Setup HttpClient for internode communication
      SolrHttpClientBuilder builder = ((HttpClientBuilderPlugin) authcPlugin).getHttpClientBuilder(HttpClientUtil.getHttpClientBuilder());
      
       // The default http client of the core container's shardHandlerFactory has already been created and
       // configured using the default httpclient configurer. We need to reconfigure it using the plugin's
       // http client configurer to set it up for internode communication.
      log.info("Reconfiguring the shard handler factory and update shard handler.");
      if (getShardHandlerFactory() instanceof HttpShardHandlerFactory) {
        ((HttpShardHandlerFactory) getShardHandlerFactory()).reconfigureHttpClient(configurer);
      log.info("Reconfiguring HttpClient settings.");

      SolrHttpClientContextBuilder httpClientBuilder = new SolrHttpClientContextBuilder();
      if (builder.getCredentialsProviderProvider() != null) {
        httpClientBuilder.setDefaultCredentialsProvider(new CredentialsProviderProvider() {
          
          @Override
          public CredentialsProvider getCredentialsProvider() {
            return builder.getCredentialsProviderProvider().getCredentialsProvider();
          }
        });
       }
      getUpdateShardHandler().reconfigureHttpClient(configurer);
      if (builder.getAuthSchemeRegistryProvider() != null) {
        httpClientBuilder.setAuthSchemeRegistryProvider(new AuthSchemeRegistryProvider() {
          
          @Override
          public Lookup<AuthSchemeProvider> getAuthSchemeRegistry() {
            return builder.getAuthSchemeRegistryProvider().getAuthSchemeRegistry();
          }
        });
      }

      HttpClientUtil.setHttpClientRequestContextBuilder(httpClientBuilder);

     } else {
       if (pkiAuthenticationPlugin != null) {
         //this happened due to an authc plugin reload. no need to register the pkiAuthc plugin again
         if(pkiAuthenticationPlugin.isInterceptorRegistered()) return;
         log.info("PKIAuthenticationPlugin is managing internode requests");
        addHttpConfigurer(pkiAuthenticationPlugin);
        setupHttpClientForAuthPlugin(pkiAuthenticationPlugin);
         pkiAuthenticationPlugin.setInterceptorRegistered();
       }
     }
diff --git a/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java b/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java
index 39c4158425d..7f38acd2dd2 100644
-- a/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java
++ b/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java
@@ -172,15 +172,17 @@ public class IndexFetcher {
 
   private final HttpClient myHttpClient;
 
  private static HttpClient createHttpClient(SolrCore core, String connTimeout, String readTimeout, String httpBasicAuthUser, String httpBasicAuthPassword, boolean useCompression) {
  private Integer connTimeout;

  private Integer soTimeout;

  private static HttpClient createHttpClient(SolrCore core, String httpBasicAuthUser, String httpBasicAuthPassword, boolean useCompression) {
     final ModifiableSolrParams httpClientParams = new ModifiableSolrParams();
    httpClientParams.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, connTimeout != null ? connTimeout : "5000");
    httpClientParams.set(HttpClientUtil.PROP_SO_TIMEOUT, readTimeout != null ? readTimeout : "20000");
     httpClientParams.set(HttpClientUtil.PROP_BASIC_AUTH_USER, httpBasicAuthUser);
     httpClientParams.set(HttpClientUtil.PROP_BASIC_AUTH_PASS, httpBasicAuthPassword);
     httpClientParams.set(HttpClientUtil.PROP_ALLOW_COMPRESSION, useCompression);
 
    return HttpClientUtil.createClient(httpClientParams, core.getCoreDescriptor().getCoreContainer().getUpdateShardHandler().getConnectionManager());
    return HttpClientUtil.createClient(httpClientParams, core.getCoreDescriptor().getCoreContainer().getUpdateShardHandler().getConnectionManager(), true);
   }
 
   public IndexFetcher(final NamedList initArgs, final ReplicationHandler handler, final SolrCore sc) {
@@ -199,11 +201,22 @@ public class IndexFetcher {
     String compress = (String) initArgs.get(COMPRESSION);
     useInternalCompression = INTERNAL.equals(compress);
     useExternalCompression = EXTERNAL.equals(compress);
    String connTimeout = (String) initArgs.get(HttpClientUtil.PROP_CONNECTION_TIMEOUT);
    String readTimeout = (String) initArgs.get(HttpClientUtil.PROP_SO_TIMEOUT);
    connTimeout = getParameter(initArgs, HttpClientUtil.PROP_CONNECTION_TIMEOUT, 30000, null);
    soTimeout = getParameter(initArgs, HttpClientUtil.PROP_SO_TIMEOUT, 120000, null);

     String httpBasicAuthUser = (String) initArgs.get(HttpClientUtil.PROP_BASIC_AUTH_USER);
     String httpBasicAuthPassword = (String) initArgs.get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    myHttpClient = createHttpClient(solrCore, connTimeout, readTimeout, httpBasicAuthUser, httpBasicAuthPassword, useExternalCompression);
    myHttpClient = createHttpClient(solrCore, httpBasicAuthUser, httpBasicAuthPassword, useExternalCompression);
  }
  
  protected <T> T getParameter(NamedList initArgs, String configKey, T defaultValue, StringBuilder sb) {
    T toReturn = defaultValue;
    if (initArgs != null) {
      T temp = (T) initArgs.get(configKey);
      toReturn = (temp != null) ? temp : defaultValue;
    }
    if(sb!=null && toReturn != null) sb.append(configKey).append(" : ").append(toReturn).append(",");
    return toReturn;
   }
 
   /**
@@ -219,8 +232,8 @@ public class IndexFetcher {
 
     // TODO modify to use shardhandler
     try (HttpSolrClient client = new HttpSolrClient(masterUrl, myHttpClient)) {
      client.setSoTimeout(60000);
      client.setConnectionTimeout(15000);
      client.setSoTimeout(soTimeout);
      client.setConnectionTimeout(connTimeout);
 
       return client.request(req);
     } catch (SolrServerException e) {
@@ -241,8 +254,8 @@ public class IndexFetcher {
 
     // TODO modify to use shardhandler
     try (HttpSolrClient client = new HttpSolrClient(masterUrl, myHttpClient)) {
      client.setSoTimeout(60000);
      client.setConnectionTimeout(15000);
      client.setSoTimeout(soTimeout);
      client.setConnectionTimeout(connTimeout);
       NamedList response = client.request(req);
 
       List<Map<String, Object>> files = (List<Map<String,Object>>) response.get(CMD_GET_FILE_LIST);
@@ -1607,8 +1620,8 @@ public class IndexFetcher {
 
       // TODO use shardhandler
       try (HttpSolrClient client = new HttpSolrClient(masterUrl, myHttpClient, null)) {
        client.setSoTimeout(60000);
        client.setConnectionTimeout(15000);
        client.setSoTimeout(soTimeout);
        client.setConnectionTimeout(connTimeout);
         QueryRequest req = new QueryRequest(params);
         response = client.request(req);
         is = (InputStream) response.get("stream");
@@ -1716,8 +1729,8 @@ public class IndexFetcher {
 
     // TODO use shardhandler
     try (HttpSolrClient client = new HttpSolrClient(masterUrl, myHttpClient)) {
      client.setSoTimeout(60000);
      client.setConnectionTimeout(15000);
      client.setSoTimeout(soTimeout);
      client.setConnectionTimeout(connTimeout);
       QueryRequest request = new QueryRequest(params);
       return client.request(request);
     }
@@ -1725,6 +1738,7 @@ public class IndexFetcher {
 
   public void destroy() {
     abortFetch();
    HttpClientUtil.close(myHttpClient);
   }
 
   String getMasterUrl() {
diff --git a/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java b/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
index b8c9692d2c9..267ab3d31c0 100644
-- a/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
@@ -387,8 +387,15 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
       return currentIndexFetcher.fetchLatestIndex(forceReplication);
     } catch (Exception e) {
       SolrException.log(LOG, "Index fetch failed ", e);
      if (currentIndexFetcher != pollingIndexFetcher) {
        currentIndexFetcher.destroy();
      }
     } finally {
       if (pollingIndexFetcher != null) {
       if( currentIndexFetcher != pollingIndexFetcher) {
         currentIndexFetcher.destroy();
       }
        
         currentIndexFetcher = pollingIndexFetcher;
       }
       indexFetchLock.unlock();
@@ -1243,20 +1250,18 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
     core.addCloseHook(new CloseHook() {
       @Override
       public void preClose(SolrCore core) {
        try {
          if (executorService != null) executorService.shutdown(); // we don't wait for shutdown - this can deadlock core reload
        } finally {
            if (pollingIndexFetcher != null) {
              pollingIndexFetcher.destroy();
            }
        if (executorService != null) executorService.shutdown(); // we don't wait for shutdown - this can deadlock core reload
      }

      @Override
      public void postClose(SolrCore core) {
        if (pollingIndexFetcher != null) {
          pollingIndexFetcher.destroy();
         }
         if (currentIndexFetcher != null && currentIndexFetcher != pollingIndexFetcher) {
           currentIndexFetcher.destroy();
         }
       }

      @Override
      public void postClose(SolrCore core) {}
     });
 
     core.addCloseHook(new CloseHook() {
diff --git a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
index d2800d75ce0..04128382422 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
++ b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
@@ -15,12 +15,11 @@
  * limitations under the License.
  */
 package org.apache.solr.handler.component;

 import org.apache.commons.lang.StringUtils;
 import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.CloseableHttpClient;
 import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
 import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
 import org.apache.solr.client.solrj.request.QueryRequest;
@@ -67,7 +66,7 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements org.
       new DefaultSolrThreadFactory("httpShardExecutor")
   );
 
  protected HttpClient defaultClient;
  protected CloseableHttpClient defaultClient;
   private LBHttpSolrClient loadbalancer;
   //default values:
   int soTimeout = UpdateShardHandlerConfig.DEFAULT_DISTRIBUPDATESOTIMEOUT;
@@ -79,7 +78,6 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements org.
   int keepAliveTime = 5;
   int queueSize = -1;
   boolean accessPolicy = false;
  boolean useRetries = false;
 
   private String scheme = null;
 
@@ -140,7 +138,6 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements org.
     this.keepAliveTime = getParameter(args, MAX_THREAD_IDLE_TIME, keepAliveTime,sb);
     this.queueSize = getParameter(args, INIT_SIZE_OF_QUEUE, queueSize,sb);
     this.accessPolicy = getParameter(args, INIT_FAIRNESS_POLICY, accessPolicy,sb);
    this.useRetries = getParameter(args, USE_RETRIES, useRetries,sb);
     log.info("created with {}",sb);
     
     // magic sysprop to make tests reproducible: set by SolrTestCaseJ4.
@@ -165,13 +162,6 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements org.
 
     this.defaultClient = HttpClientUtil.createClient(clientParams);
     
    // must come after createClient
    if (useRetries) {
      // our default retry handler will never retry on IOException if the request has been sent already,
      // but for these read only requests we can use the standard DefaultHttpRequestRetryHandler rules
      ((DefaultHttpClient) this.defaultClient).setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler());
    }
    
     this.loadbalancer = createLoadbalancer(defaultClient);
   }
   
@@ -179,30 +169,18 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements org.
     ModifiableSolrParams clientParams = new ModifiableSolrParams();
     clientParams.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, maxConnectionsPerHost);
     clientParams.set(HttpClientUtil.PROP_MAX_CONNECTIONS, maxConnections);
    clientParams.set(HttpClientUtil.PROP_SO_TIMEOUT, soTimeout);
    clientParams.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, connectionTimeout);
    if (!useRetries) {
      clientParams.set(HttpClientUtil.PROP_USE_RETRY, false);
    }
     return clientParams;
   }
 
  /**
   * For an already created internal httpclient, this can be used to configure it 
   * again. Useful for authentication plugins.
   * @param configurer an HttpClientConfigurer instance
   */
  public void reconfigureHttpClient(HttpClientConfigurer configurer) {
    log.info("Reconfiguring the default client with: " + configurer);
    configurer.configure((DefaultHttpClient)this.defaultClient, getClientParams());
  }

   protected ThreadPoolExecutor getThreadPoolExecutor(){
     return this.commExecutor;
   }
 
   protected LBHttpSolrClient createLoadbalancer(HttpClient httpClient){
    return new LBHttpSolrClient(httpClient);
    LBHttpSolrClient client = new LBHttpSolrClient(httpClient);
    client.setConnectionTimeout(connectionTimeout);
    client.setSoTimeout(soTimeout);
    return client;
   }
 
   protected <T> T getParameter(NamedList initArgs, String configKey, T defaultValue, StringBuilder sb) {
@@ -222,14 +200,13 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements org.
       ExecutorUtil.shutdownAndAwaitTermination(commExecutor);
     } finally {
       try {
        if (defaultClient != null) {
          defaultClient.getConnectionManager().shutdown();
        }
      } finally {
        
         if (loadbalancer != null) {
           loadbalancer.close();
         }
      } finally { 
        if (defaultClient != null) {
          HttpClientUtil.close(defaultClient);
        }
       }
     }
   }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/IterativeMergeStrategy.java b/solr/core/src/java/org/apache/solr/handler/component/IterativeMergeStrategy.java
index a8f6ca96e8f..83677e42019 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/IterativeMergeStrategy.java
++ b/solr/core/src/java/org/apache/solr/handler/component/IterativeMergeStrategy.java
@@ -20,13 +20,10 @@ import java.lang.invoke.MethodHandles;
 import java.util.concurrent.Callable;
 import java.util.concurrent.Future;
 import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 import java.util.List;
 import java.util.ArrayList;
 
import org.apache.lucene.util.NamedThreadFactory;
 import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
 import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.request.QueryRequest;
diff --git a/solr/core/src/java/org/apache/solr/security/AuthenticationPlugin.java b/solr/core/src/java/org/apache/solr/security/AuthenticationPlugin.java
index 52296337fef..47daec1dd14 100644
-- a/solr/core/src/java/org/apache/solr/security/AuthenticationPlugin.java
++ b/solr/core/src/java/org/apache/solr/security/AuthenticationPlugin.java
@@ -22,15 +22,12 @@ import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
 import java.io.Closeable;
 import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
 import java.security.Principal;
 import java.util.Map;
 
 import org.apache.http.auth.BasicUserPrincipal;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
 
 /**
  * 
diff --git a/solr/core/src/java/org/apache/solr/security/HttpClientInterceptorPlugin.java b/solr/core/src/java/org/apache/solr/security/HttpClientBuilderPlugin.java
similarity index 63%
rename from solr/core/src/java/org/apache/solr/security/HttpClientInterceptorPlugin.java
rename to solr/core/src/java/org/apache/solr/security/HttpClientBuilderPlugin.java
index d7598df6156..8b7e80bee75 100644
-- a/solr/core/src/java/org/apache/solr/security/HttpClientInterceptorPlugin.java
++ b/solr/core/src/java/org/apache/solr/security/HttpClientBuilderPlugin.java
@@ -16,15 +16,22 @@
  */
 package org.apache.solr.security;
 
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
 
public interface HttpClientInterceptorPlugin {
/**
 * Plugin interface for configuring internal HttpClients. This
 * relies on the internal HttpClient implementation and is subject to
 * change.
 * 
 * @lucene.experimental
 */
public interface HttpClientBuilderPlugin {
   /**
    *
   * @return Returns an instance of a HttpClientConfigurer to be used for configuring the
   * httpclients for use with SolrJ clients.
   * @return Returns an instance of a SolrHttpClientBuilder to be used for configuring the
   * HttpClients for use with SolrJ clients.
    *
    * @lucene.experimental
    */
  public HttpClientConfigurer getClientConfigurer();
  public SolrHttpClientBuilder getHttpClientBuilder(SolrHttpClientBuilder builder);
 }
diff --git a/solr/core/src/java/org/apache/solr/security/KerberosPlugin.java b/solr/core/src/java/org/apache/solr/security/KerberosPlugin.java
index ad7f29bffe0..2ef7fb88ab1 100644
-- a/solr/core/src/java/org/apache/solr/security/KerberosPlugin.java
++ b/solr/core/src/java/org/apache/solr/security/KerberosPlugin.java
@@ -30,6 +30,7 @@ import javax.servlet.Filter;
 import javax.servlet.FilterChain;
 import javax.servlet.FilterConfig;
 import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
 import javax.servlet.RequestDispatcher;
 import javax.servlet.Servlet;
 import javax.servlet.ServletContext;
@@ -39,23 +40,22 @@ import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
 import javax.servlet.SessionCookieConfig;
 import javax.servlet.SessionTrackingMode;
import javax.servlet.FilterRegistration.Dynamic;
 import javax.servlet.descriptor.JspConfigDescriptor;
 
 import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.cloud.ZkController;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.impl.Krb5HttpClientBuilder;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.core.CoreContainer;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
public class KerberosPlugin extends AuthenticationPlugin implements HttpClientInterceptorPlugin {
public class KerberosPlugin extends AuthenticationPlugin implements HttpClientBuilderPlugin {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
  HttpClientConfigurer kerberosConfigurer = new Krb5HttpClientConfigurer();
  Krb5HttpClientBuilder kerberosBuilder = new Krb5HttpClientBuilder();
   Filter kerberosFilter = new KerberosFilter();
   
   public static final String NAME_RULES_PARAM = "solr.kerberos.name.rules";
@@ -145,12 +145,13 @@ public class KerberosPlugin extends AuthenticationPlugin implements HttpClientIn
   }
 
   @Override
  public HttpClientConfigurer getClientConfigurer() {
    return kerberosConfigurer;
  public SolrHttpClientBuilder getHttpClientBuilder(SolrHttpClientBuilder builder) {
    return kerberosBuilder.getBuilder(builder);
   }
 
   public void close() {
     kerberosFilter.destroy();
    kerberosBuilder.close();
   }
 
   protected static ServletContext noContext = new ServletContext() {
diff --git a/solr/core/src/java/org/apache/solr/security/PKIAuthenticationPlugin.java b/solr/core/src/java/org/apache/solr/security/PKIAuthenticationPlugin.java
index 3c65af29647..26b29fe1fa8 100644
-- a/solr/core/src/java/org/apache/solr/security/PKIAuthenticationPlugin.java
++ b/solr/core/src/java/org/apache/solr/security/PKIAuthenticationPlugin.java
@@ -16,11 +16,8 @@
  */
 package org.apache.solr.security;
 
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import static java.nio.charset.StandardCharsets.UTF_8;

 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.nio.ByteBuffer;
@@ -30,17 +27,22 @@ import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

 import org.apache.http.HttpException;
 import org.apache.http.HttpRequest;
 import org.apache.http.HttpRequestInterceptor;
 import org.apache.http.HttpResponse;
 import org.apache.http.auth.BasicUserPrincipal;
 import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.http.protocol.HttpContext;
 import org.apache.http.util.EntityUtils;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
 import org.apache.solr.common.util.Base64;
 import org.apache.solr.common.util.ExecutorUtil;
 import org.apache.solr.common.util.StrUtils;
@@ -56,17 +58,15 @@ import org.apache.solr.util.CryptoKeys;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import static java.nio.charset.StandardCharsets.UTF_8;

 
public class PKIAuthenticationPlugin extends AuthenticationPlugin implements HttpClientInterceptorPlugin {
public class PKIAuthenticationPlugin extends AuthenticationPlugin implements HttpClientBuilderPlugin {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
   private final CryptoKeys.RSAKeyPair keyPair = new CryptoKeys.RSAKeyPair();
   private final CoreContainer cores;
   private final int MAX_VALIDITY = Integer.parseInt(System.getProperty("pkiauth.ttl", "5000"));
   private final String myNodeName;

  private final HttpHeaderClientInterceptor interceptor = new HttpHeaderClientInterceptor();
   private boolean interceptorRegistered = false;
 
   public void setInterceptorRegistered(){
@@ -197,7 +197,7 @@ public class PKIAuthenticationPlugin extends AuthenticationPlugin implements Htt
     try {
       String uri = url + PATH + "?wt=json&omitHeader=true";
       log.debug("Fetching fresh public key from : {}",uri);
      HttpResponse rsp = cores.getUpdateShardHandler().getHttpClient().execute(new HttpGet(uri));
      HttpResponse rsp = cores.getUpdateShardHandler().getHttpClient().execute(new HttpGet(uri), HttpClientUtil.createNewHttpClientRequestContext());
       byte[] bytes = EntityUtils.toByteArray(rsp.getEntity());
       Map m = (Map) Utils.fromJSON(bytes);
       String key = (String) m.get("key");
@@ -217,11 +217,10 @@ public class PKIAuthenticationPlugin extends AuthenticationPlugin implements Htt
 
   }
 
  private HttpHeaderClientConfigurer clientConfigurer = new HttpHeaderClientConfigurer();

   @Override
  public HttpClientConfigurer getClientConfigurer() {
    return clientConfigurer;
  public SolrHttpClientBuilder getHttpClientBuilder(SolrHttpClientBuilder builder) {
    HttpClientUtil.addRequestInterceptor(interceptor);
    return builder;
   }
 
   public SolrRequestHandler getRequestHandler() {
@@ -242,13 +241,9 @@ public class PKIAuthenticationPlugin extends AuthenticationPlugin implements Htt
     return req.getUserPrincipal() != SU;
   }
 
  private class HttpHeaderClientConfigurer extends HttpClientConfigurer implements
      HttpRequestInterceptor {
  private class HttpHeaderClientInterceptor implements HttpRequestInterceptor {
 
    @Override
    public void configure(DefaultHttpClient httpClient, SolrParams config) {
      super.configure(httpClient, config);
      httpClient.addRequestInterceptor(this);
    public HttpHeaderClientInterceptor() {
     }
 
     @Override
@@ -299,12 +294,12 @@ public class PKIAuthenticationPlugin extends AuthenticationPlugin implements Htt
 
   boolean disabled() {
     return cores.getAuthenticationPlugin() == null ||
        cores.getAuthenticationPlugin() instanceof HttpClientInterceptorPlugin;
        cores.getAuthenticationPlugin() instanceof HttpClientBuilderPlugin;
   }
 
   @Override
   public void close() throws IOException {

    HttpClientUtil.removeRequestInterceptor(interceptor);
   }
 
   public String getPublicKey() {
diff --git a/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java b/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java
index 63cfb7cc9a2..46871543521 100644
-- a/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java
++ b/solr/core/src/java/org/apache/solr/servlet/HttpSolrCall.java
@@ -58,6 +58,7 @@ import org.apache.http.client.methods.HttpPut;
 import org.apache.http.client.methods.HttpRequestBase;
 import org.apache.http.entity.InputStreamEntity;
 import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.cloud.Aliases;
@@ -557,7 +558,7 @@ public class HttpSolrCall {
         method.removeHeaders(CONTENT_LENGTH_HEADER);
       }
 
      final HttpResponse response = solrDispatchFilter.httpClient.execute(method);
      final HttpResponse response = solrDispatchFilter.httpClient.execute(method, HttpClientUtil.createNewHttpClientRequestContext());
       int httpStatus = response.getStatusLine().getStatusCode();
       httpEntity = response.getEntity();
 
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
index a44b8f87b76..4fe869c25c9 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
@@ -16,27 +16,21 @@
  */
 package org.apache.solr.update;
 
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

 import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
 import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
 import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.cloud.RecoveryStrategy;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.common.util.IOUtils;
 import org.apache.solr.common.util.SolrjNamedThreadFactory;
import org.apache.solr.core.NodeConfig;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

 public class UpdateShardHandler {
   
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
@@ -54,53 +48,25 @@ public class UpdateShardHandler {
   private ExecutorService recoveryExecutor = ExecutorUtil.newMDCAwareCachedThreadPool(
       new SolrjNamedThreadFactory("recoveryExecutor"));
   
  private PoolingClientConnectionManager clientConnectionManager;
  
   private final CloseableHttpClient client;
 
  private final UpdateShardHandlerConfig cfg;
  private final PoolingHttpClientConnectionManager clientConnectionManager;
 
   public UpdateShardHandler(UpdateShardHandlerConfig cfg) {
    this.cfg = cfg;
    clientConnectionManager = new PoolingClientConnectionManager(SchemeRegistryFactory.createSystemDefault());
    clientConnectionManager = new PoolingHttpClientConnectionManager(HttpClientUtil.getSchemaRegisteryProvider().getSchemaRegistry());
     if (cfg != null ) {
       clientConnectionManager.setMaxTotal(cfg.getMaxUpdateConnections());
       clientConnectionManager.setDefaultMaxPerRoute(cfg.getMaxUpdateConnectionsPerHost());
     }
 
    ModifiableSolrParams clientParams = getClientParams();
    ModifiableSolrParams clientParams = new ModifiableSolrParams();
     log.info("Creating UpdateShardHandler HTTP client with params: {}", clientParams);
     client = HttpClientUtil.createClient(clientParams, clientConnectionManager);
   }

  protected ModifiableSolrParams getClientParams() {
    ModifiableSolrParams clientParams = new ModifiableSolrParams();
    if (cfg != null) {
      clientParams.set(HttpClientUtil.PROP_SO_TIMEOUT,
          cfg.getDistributedSocketTimeout());
      clientParams.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT,
          cfg.getDistributedConnectionTimeout());
    }
    // in the update case, we want to do retries, and to use
    // the default Solr retry handler that createClient will 
    // give us
    clientParams.set(HttpClientUtil.PROP_USE_RETRY, true);
    return clientParams;
  }
  
   
   public HttpClient getHttpClient() {
     return client;
   }

  public void reconfigureHttpClient(HttpClientConfigurer configurer) {
    log.info("Reconfiguring the default client with: " + configurer);
    configurer.configure((DefaultHttpClient)client, getClientParams());
  }

  public ClientConnectionManager getConnectionManager() {
    return clientConnectionManager;
  }
   
   /**
    * This method returns an executor that is not meant for disk IO and that will
@@ -112,6 +78,11 @@ public class UpdateShardHandler {
     return updateExecutor;
   }
   

  public PoolingHttpClientConnectionManager getConnectionManager() {
    return clientConnectionManager;
  }

   /**
    * In general, RecoveryStrategy threads do not do disk IO, but they open and close SolrCores
    * in async threads, amoung other things, and can trigger disk IO, so we use this alternate 
@@ -131,8 +102,8 @@ public class UpdateShardHandler {
     } catch (Exception e) {
       SolrException.log(log, e);
     } finally {
      IOUtils.closeQuietly(client);
      clientConnectionManager.shutdown();
      HttpClientUtil.close(client);
      clientConnectionManager.close();
     }
   }
 
diff --git a/solr/core/src/java/org/apache/solr/util/SolrCLI.java b/solr/core/src/java/org/apache/solr/util/SolrCLI.java
index 19aa52ac184..ae11118954c 100644
-- a/solr/core/src/java/org/apache/solr/util/SolrCLI.java
++ b/solr/core/src/java/org/apache/solr/util/SolrCLI.java
@@ -16,6 +16,8 @@
  */
 package org.apache.solr.util;
 
import static org.apache.solr.common.params.CommonParams.NAME;

 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
@@ -35,7 +37,6 @@ import java.util.Arrays;
 import java.util.Collection;
 import java.util.Enumeration;
 import java.util.HashMap;
import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Locale;
@@ -79,9 +80,9 @@ import org.apache.solr.client.solrj.SolrClient;
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
 import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
 import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrException;
@@ -101,8 +102,6 @@ import org.noggit.ObjectBuilder;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import static org.apache.solr.common.params.CommonParams.NAME;

 /**
  * Command-line utility for working with Solr.
  */
@@ -275,15 +274,15 @@ public class SolrCLI {
       exit(0);
     }
 
    String configurerClassName = System.getProperty("solr.authentication.httpclient.configurer");
    if (configurerClassName!=null) {
    String builderClassName = System.getProperty("solr.authentication.httpclient.builder");
    if (builderClassName!=null) {
       try {
        Class c = Class.forName(configurerClassName);
        HttpClientConfigurer configurer = (HttpClientConfigurer)c.newInstance();
        HttpClientUtil.setConfigurer(configurer);
        log.info("Set HttpClientConfigurer from: "+configurerClassName);
        Class c = Class.forName(builderClassName);
        SolrHttpClientBuilder builder = (SolrHttpClientBuilder)c.newInstance();
        HttpClientUtil.setHttpClientBuilder(builder);
        log.info("Set HttpClientConfigurer from: "+builderClassName);
       } catch (Exception ex) {
        throw new RuntimeException("Error during loading of configurer '"+configurerClassName+"'.", ex);
        throw new RuntimeException("Error during loading of configurer '"+builderClassName+"'.", ex);
       }
     }
 
@@ -651,7 +650,7 @@ public class SolrCLI {
     // ensure we're requesting JSON back from Solr
     HttpGet httpGet = new HttpGet(new URIBuilder(getUrl).setParameter(CommonParams.WT, CommonParams.JSON).build());
     // make the request and get back a parsed JSON object
    Map<String,Object> json = httpClient.execute(httpGet, new SolrResponseHandler());
    Map<String,Object> json = httpClient.execute(httpGet, new SolrResponseHandler(), HttpClientUtil.createNewHttpClientRequestContext());
     // check the response JSON from Solr to see if it is an error
     Long statusCode = asLong("/responseHeader/status", json);
     if (statusCode == -1) {
diff --git a/solr/core/src/test-files/log4j.properties b/solr/core/src/test-files/log4j.properties
index e056163a660..26972038f9c 100644
-- a/solr/core/src/test-files/log4j.properties
++ b/solr/core/src/test-files/log4j.properties
@@ -29,8 +29,8 @@ log4j.logger.org.apache.solr.hadoop=INFO
 #log4j.logger.org.apache.solr.common.cloud.ClusterStateUtil=DEBUG
 #log4j.logger.org.apache.solr.cloud.OverseerAutoReplicaFailoverThread=DEBUG
 
#log4j.logger.org.apache.http.impl.conn.PoolingClientConnectionManager=DEBUG
#log4j.logger.org.apache.http.impl.conn.PoolingHttpClientConnectionManager=DEBUG
 #log4j.logger.org.apache.http.impl.conn.BasicClientConnectionManager=DEBUG
 #log4j.logger.org.apache.http=DEBUG
 #log4j.logger.org.apache.solr.client.solrj.impl.SolrHttpRequestRetryHandler=DEBUG
#log4j.logger.org.eclipse.jetty.server=DEBUG
#log4j.logger.org.eclipse.jetty=DEBUG
diff --git a/solr/core/src/test/org/apache/solr/TestDistributedSearch.java b/solr/core/src/test/org/apache/solr/TestDistributedSearch.java
index dbc48ddd7c0..a25498aef25 100644
-- a/solr/core/src/test/org/apache/solr/TestDistributedSearch.java
++ b/solr/core/src/test/org/apache/solr/TestDistributedSearch.java
@@ -55,6 +55,7 @@ import org.apache.solr.handler.component.TrackingShardHandlerFactory;
 import org.apache.solr.handler.component.TrackingShardHandlerFactory.RequestTrackingQueue;
 import org.apache.solr.handler.component.TrackingShardHandlerFactory.ShardRequestAndParams;
 import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
 import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -89,6 +90,15 @@ public class TestDistributedSearch extends BaseDistributedSearchTestCase {
     return "solr-trackingshardhandler.xml";
   }
 
  @BeforeClass
  public static void beforeClass() {
    // we shutdown a jetty and start it and try to use
    // the same http client pretty fast - this lowered setting makes sure
    // we validate the connection before use on the restarted
    // server so that we don't use a bad one
    System.setProperty("validateAfterInactivity", "200");
  }
  
   @Test
   public void test() throws Exception {
     QueryResponse rsp = null;
@@ -988,7 +998,7 @@ public class TestDistributedSearch extends BaseDistributedSearchTestCase {
 
       // restart the jettys
       for (JettySolrRunner downJetty : downJettys) {
        downJetty.start();
        ChaosMonkey.start(downJetty);
       }
     }
 
diff --git a/solr/core/src/test/org/apache/solr/client/solrj/ConnectionReuseTest.java b/solr/core/src/test/org/apache/solr/client/solrj/ConnectionReuseTest.java
index 0ec9876b61f..0d789750278 100644
-- a/solr/core/src/test/org/apache/solr/client/solrj/ConnectionReuseTest.java
++ b/solr/core/src/test/org/apache/solr/client/solrj/ConnectionReuseTest.java
@@ -18,24 +18,25 @@ package org.apache.solr.client.solrj;
 
 import java.io.IOException;
 import java.net.URL;
import java.util.concurrent.ExecutionException;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.HttpClientConnection;
 import org.apache.http.HttpConnectionMetrics;
 import org.apache.http.HttpException;
 import org.apache.http.HttpHost;
 import org.apache.http.HttpRequest;
 import org.apache.http.HttpVersion;
 import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionRequest;
 import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.ConnectionRequest;
 import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
 import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
 import org.apache.solr.SolrTestCaseJ4.SuppressSSL;
 import org.apache.solr.client.solrj.impl.CloudSolrClient;
 import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
@@ -51,7 +52,7 @@ import org.junit.Test;
 public class ConnectionReuseTest extends AbstractFullDistribZkTestBase {
   
   private AtomicInteger id = new AtomicInteger();
  
  private HttpClientContext context = HttpClientContext.create();
   @BeforeClass
   public static void beforeConnectionReuseTest() {
     if (true) TestInjection.failUpdateRequests = "true:100";
@@ -74,114 +75,122 @@ public class ConnectionReuseTest extends AbstractFullDistribZkTestBase {
   
   @Test
   public void test() throws Exception {

     URL url = new URL(((HttpSolrClient) clients.get(0)).getBaseURL());
    
    SolrClient client;
    HttpClient httpClient = HttpClientUtil.createClient(null);
    int rndClient = random().nextInt(3);
    if (rndClient == 0) {
      client = new ConcurrentUpdateSolrClient(url.toString(), httpClient, 6, 1); // currently only testing with 1 thread
    } else if (rndClient == 1)  {
      client = new HttpSolrClient(url.toString(), httpClient);
    } else if (rndClient == 2) {
      client = new CloudSolrClient(zkServer.getZkAddress(), random().nextBoolean(), httpClient);
      ((CloudSolrClient) client).setParallelUpdates(random().nextBoolean());
      ((CloudSolrClient) client).setDefaultCollection(DEFAULT_COLLECTION);
      ((CloudSolrClient) client).getLbClient().setConnectionTimeout(30000);
      ((CloudSolrClient) client).getLbClient().setSoTimeout(60000);
    } else {
      throw new RuntimeException("impossible");
    }
    
    PoolingClientConnectionManager cm = (PoolingClientConnectionManager) httpClient.getConnectionManager();

    HttpHost target = new HttpHost(url.getHost(), url.getPort(), isSSLMode() ? "https" : "http");
    HttpRoute route = new HttpRoute(target);
    
    ClientConnectionRequest mConn = getClientConnectionRequest(httpClient, route);
   
    ManagedClientConnection conn1 = getConn(mConn);
    headerRequest(target, route, conn1);
    conn1.releaseConnection();
    cm.releaseConnection(conn1, -1, TimeUnit.MILLISECONDS);
    
    int queueBreaks = 0;
    int cnt1 = atLeast(3);
    int cnt2 = atLeast(30);
    for (int j = 0; j < cnt1; j++) {
      for (int i = 0; i < cnt2; i++) {
        boolean done = false;
        AddUpdateCommand c = new AddUpdateCommand(null);
        c.solrDoc = sdoc("id", id.incrementAndGet());
        try {
          client.add(c.solrDoc);
        } catch (Exception e) {
          e.printStackTrace();
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    SolrClient client = null;
    CloseableHttpClient httpClient = HttpClientUtil.createClient(null, cm);
    try {
      int rndClient = random().nextInt(3);
      if (rndClient == 0) {
        client = new ConcurrentUpdateSolrClient(url.toString(), httpClient, 6, 1); // currently only testing with 1
                                                                                   // thread
      } else if (rndClient == 1) {
        client = new HttpSolrClient(url.toString(), httpClient);
      } else if (rndClient == 2) {
        client = new CloudSolrClient(zkServer.getZkAddress(), random().nextBoolean(), httpClient);
        ((CloudSolrClient) client).setParallelUpdates(random().nextBoolean());
        ((CloudSolrClient) client).setDefaultCollection(DEFAULT_COLLECTION);
        ((CloudSolrClient) client).getLbClient().setConnectionTimeout(30000);
        ((CloudSolrClient) client).getLbClient().setSoTimeout(60000);
      } else {
        throw new RuntimeException("impossible");
      }

      HttpHost target = new HttpHost(url.getHost(), url.getPort(), isSSLMode() ? "https" : "http");
      HttpRoute route = new HttpRoute(target);

      ConnectionRequest mConn = getClientConnectionRequest(httpClient, route, cm);

      HttpClientConnection conn1 = getConn(mConn);
      headerRequest(target, route, conn1, cm);

      cm.releaseConnection(conn1, null, -1, TimeUnit.MILLISECONDS);

      int queueBreaks = 0;
      int cnt1 = atLeast(3);
      int cnt2 = atLeast(30);
      for (int j = 0; j < cnt1; j++) {
        for (int i = 0; i < cnt2; i++) {
          boolean done = false;
          AddUpdateCommand c = new AddUpdateCommand(null);
          c.solrDoc = sdoc("id", id.incrementAndGet());
          try {
            client.add(c.solrDoc);
          } catch (Exception e) {
            e.printStackTrace();
          }
          if (!done && i > 0 && i < cnt2 - 1 && client instanceof ConcurrentUpdateSolrClient
              && random().nextInt(10) > 8) {
            queueBreaks++;
            done = true;
            Thread.sleep(350); // wait past streaming client poll time of 250ms
          }
         }
        if (!done && i > 0 && i < cnt2 - 1 && client instanceof ConcurrentUpdateSolrClient && random().nextInt(10) > 8) {
          queueBreaks++;
          done = true;
          Thread.sleep(350); // wait past streaming client poll time of 250ms
        if (client instanceof ConcurrentUpdateSolrClient) {
          ((ConcurrentUpdateSolrClient) client).blockUntilFinished();
         }
       }

      route = new HttpRoute(new HttpHost(url.getHost(), url.getPort(), isSSLMode() ? "https" : "http"));

      mConn = cm.requestConnection(route, null);

      HttpClientConnection conn2 = getConn(mConn);

      HttpConnectionMetrics metrics = conn2.getMetrics();
      headerRequest(target, route, conn2, cm);

      cm.releaseConnection(conn2, null, -1, TimeUnit.MILLISECONDS);

      assertNotNull("No connection metrics found - is the connection getting aborted? server closing the connection? "
          + client.getClass().getSimpleName(), metrics);

      // we try and make sure the connection we get has handled all of the requests in this test
       if (client instanceof ConcurrentUpdateSolrClient) {
        ((ConcurrentUpdateSolrClient) client).blockUntilFinished();
        // we can't fully control queue polling breaking up requests - allow a bit of leeway
        int exp = cnt1 + queueBreaks + 2;
        assertTrue(
            "We expected all communication via streaming client to use one connection! expected=" + exp + " got="
                + metrics.getRequestCount(),
            Math.max(exp, metrics.getRequestCount()) - Math.min(exp, metrics.getRequestCount()) < 3);
      } else {
        assertTrue("We expected all communication to use one connection! " + client.getClass().getSimpleName() + " "
            + metrics.getRequestCount(),
            cnt1 * cnt2 + 2 <= metrics.getRequestCount());
       }
    }
 
    route = new HttpRoute(new HttpHost(url.getHost(), url.getPort(), isSSLMode() ? "https" : "http"));

    mConn = cm.requestConnection(route, null);
   
    ManagedClientConnection conn2 = getConn(mConn);

    HttpConnectionMetrics metrics = conn2.getMetrics();
    headerRequest(target, route, conn2);
    conn2.releaseConnection();
    cm.releaseConnection(conn2, -1, TimeUnit.MILLISECONDS);

    
    assertNotNull("No connection metrics found - is the connection getting aborted? server closing the connection? " + client.getClass().getSimpleName(), metrics);
    
    // we try and make sure the connection we get has handled all of the requests in this test
    if (client instanceof ConcurrentUpdateSolrClient) {
      // we can't fully control queue polling breaking up requests - allow a bit of leeway
      int exp = cnt1 + queueBreaks + 2;
      assertTrue(
          "We expected all communication via streaming client to use one connection! expected=" + exp + " got="
              + metrics.getRequestCount(),
          Math.max(exp, metrics.getRequestCount()) - Math.min(exp, metrics.getRequestCount()) < 3);
    } else {
      assertTrue("We expected all communication to use one connection! " + client.getClass().getSimpleName(),
          cnt1 * cnt2 + 2 <= metrics.getRequestCount());
    } finally {
      client.close();
      HttpClientUtil.close(httpClient);
     }
    
    client.close();
   }
 
  public ManagedClientConnection getConn(ClientConnectionRequest mConn)
      throws InterruptedException, ConnectionPoolTimeoutException {
    ManagedClientConnection conn = mConn.getConnection(30, TimeUnit.SECONDS);
    conn.setIdleDuration(-1, TimeUnit.MILLISECONDS);
    conn.markReusable();
  public HttpClientConnection getConn(ConnectionRequest mConn)
      throws InterruptedException, ConnectionPoolTimeoutException, ExecutionException {
    HttpClientConnection conn = mConn.get(30, TimeUnit.SECONDS);

     return conn;
   }
 
  public void headerRequest(HttpHost target, HttpRoute route, ManagedClientConnection conn)
  public void headerRequest(HttpHost target, HttpRoute route, HttpClientConnection conn, PoolingHttpClientConnectionManager cm)
       throws IOException, HttpException {
     HttpRequest req = new BasicHttpRequest("OPTIONS", "*", HttpVersion.HTTP_1_1);
 
     req.addHeader("Host", target.getHostName());
    BasicHttpParams p = new BasicHttpParams();
    HttpProtocolParams.setVersion(p, HttpVersion.HTTP_1_1);
    if (!conn.isOpen()) conn.open(route, new BasicHttpContext(null), p);
    if (!conn.isOpen()) {
      // establish connection based on its route info
      cm.connect(conn, route, 1000, context);
      // and mark it as route complete
      cm.routeComplete(conn, route, context);
    }
     conn.sendRequestHeader(req);
     conn.flush();
     conn.receiveResponseHeader();
   }
 
  public ClientConnectionRequest getClientConnectionRequest(HttpClient httpClient, HttpRoute route) {
    ClientConnectionRequest mConn = ((PoolingClientConnectionManager) httpClient.getConnectionManager()).requestConnection(route, null);
  public ConnectionRequest getClientConnectionRequest(HttpClient httpClient, HttpRoute route, PoolingHttpClientConnectionManager cm) {
    ConnectionRequest mConn = cm.requestConnection(route, null);
     return mConn;
   }
 
diff --git a/solr/core/src/test/org/apache/solr/client/solrj/embedded/TestJettySolrRunner.java b/solr/core/src/test/org/apache/solr/client/solrj/embedded/TestJettySolrRunner.java
index 17facec7241..dc8896d0bea 100644
-- a/solr/core/src/test/org/apache/solr/client/solrj/embedded/TestJettySolrRunner.java
++ b/solr/core/src/test/org/apache/solr/client/solrj/embedded/TestJettySolrRunner.java
@@ -53,13 +53,13 @@ public class TestJettySolrRunner extends SolrTestCaseJ4 {
     try {
       runner.start();
 
      SolrClient client = new HttpSolrClient(runner.getBaseUrl().toString());
      try (SolrClient client = new HttpSolrClient(runner.getBaseUrl().toString())) {
        CoreAdminRequest.Create createReq = new CoreAdminRequest.Create();
        createReq.setCoreName("newcore");
        createReq.setConfigSet("minimal");
 
      CoreAdminRequest.Create createReq = new CoreAdminRequest.Create();
      createReq.setCoreName("newcore");
      createReq.setConfigSet("minimal");

      client.request(createReq);
        client.request(createReq);
      }
 
       assertTrue(Files.exists(coresDir.resolve("newcore").resolve("core.properties")));
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java
index fe94309bba2..ca4f91219b4 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java
@@ -168,6 +168,11 @@ public class BaseCdcrDistributedZkTest extends AbstractDistribZkTestBase {
 
   @After
   public void baseAfter() throws Exception {
    for (List<CloudJettyRunner> runners : cloudJettys.values()) {
      for (CloudJettyRunner runner : runners) {
        runner.client.close();
      }
    }
     destroyServers();
   }
 
@@ -175,8 +180,6 @@ public class BaseCdcrDistributedZkTest extends AbstractDistribZkTestBase {
     CloudSolrClient server = new CloudSolrClient(zkServer.getZkAddress(), random().nextBoolean());
     server.setParallelUpdates(random().nextBoolean());
     if (defaultCollection != null) server.setDefaultCollection(defaultCollection);
    server.getLbClient().getHttpClient().getParams()
        .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
     return server;
   }
 
@@ -747,8 +750,6 @@ public class BaseCdcrDistributedZkTest extends AbstractDistribZkTestBase {
       // setup the server...
       HttpSolrClient s = new HttpSolrClient(baseUrl);
       s.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      s.setDefaultMaxConnectionsPerHost(100);
      s.setMaxTotalConnections(100);
       return s;
     } catch (Exception ex) {
       throw new RuntimeException(ex);
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index 8222e91677f..6eee7dbb947 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -404,17 +404,18 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     for (Slice slice : dColl.getActiveSlices()) {
       long sliceDocCount = -1;
       for (Replica rep : slice.getReplicas()) {
        HttpSolrClient one = new HttpSolrClient(rep.getCoreUrl());
        SolrQuery query = new SolrQuery("*:*");
        query.setDistrib(false);
        QueryResponse resp = one.query(query);
        long hits = resp.getResults().getNumFound();
        if (sliceDocCount == -1) {
          sliceDocCount = hits;
          docTotal += hits; 
        } else {
          if (hits != sliceDocCount) {
            return -1;
        try (HttpSolrClient one = new HttpSolrClient(rep.getCoreUrl())) {
          SolrQuery query = new SolrQuery("*:*");
          query.setDistrib(false);
          QueryResponse resp = one.query(query);
          long hits = resp.getResults().getNumFound();
          if (sliceDocCount == -1) {
            sliceDocCount = hits;
            docTotal += hits;
          } else {
            if (hits != sliceDocCount) {
              return -1;
            }
           }
         }
       }
@@ -963,7 +964,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
       @Override
       public Object call() {
         try (HttpSolrClient client = new HttpSolrClient(baseUrl)) {
          client.setConnectionTimeout(15000);
          // client.setConnectionTimeout(15000);
           Create createCmd = new Create();
           createCmd.setRoles("none");
           createCmd.setCoreName(collection + num);
@@ -1124,9 +1125,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     try {
       // setup the server...
       HttpSolrClient client = new HttpSolrClient(baseUrl + "/" + collection);
      client.setSoTimeout(120000);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);

       return client;
     }
     catch (Exception ex) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
index 7dceada1668..3ddf99da545 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
@@ -70,6 +70,8 @@ public class ChaosMonkeyNothingIsSafeTest extends AbstractFullDistribZkTestBase
   
   protected static final String[] fieldNames = new String[]{"f_i", "f_f", "f_d", "f_l", "f_dt"};
   protected static final RandVal[] randVals = new RandVal[]{rint, rfloat, rdouble, rlong, rdate};

  private int clientSoTimeout;
   
   public String[] getFieldNames() {
     return fieldNames;
@@ -109,6 +111,7 @@ public class ChaosMonkeyNothingIsSafeTest extends AbstractFullDistribZkTestBase
 
   @Test
   public void test() throws Exception {
    cloudClient.setSoTimeout(clientSoTimeout);
     boolean testSuccessful = false;
     try {
       handle.clear();
@@ -293,8 +296,7 @@ public class ChaosMonkeyNothingIsSafeTest extends AbstractFullDistribZkTestBase
       setName("FullThrottleStopableIndexingThread");
       setDaemon(true);
       this.clients = clients;
      HttpClientUtil.setConnectionTimeout(httpClient, clientConnectionTimeout);
      HttpClientUtil.setSoTimeout(httpClient, clientSoTimeout);

       cusc = new ConcurrentUpdateSolrClient(
           ((HttpSolrClient) clients.get(0)).getBaseURL(), httpClient, 8,
           2) {
@@ -303,6 +305,8 @@ public class ChaosMonkeyNothingIsSafeTest extends AbstractFullDistribZkTestBase
           log.warn("cusc error", ex);
         }
       };
      cusc.setConnectionTimeout(10000);
      cusc.setSoTimeout(clientSoTimeout);
     }
     
     @Override
diff --git a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
index 641dadfc236..5c80f2d691a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
@@ -97,6 +97,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
   @BeforeClass
   public static void beforeCollectionsAPIDistributedZkTest() {
     TestInjection.randomDelayInCoreCreation = "true:20";
    System.setProperty("validateAfterInactivity", "200");
   }
   
   @Override
@@ -1196,11 +1197,12 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
           null, client, props);
       assertNotNull(newReplica);
 
      HttpSolrClient coreclient = new HttpSolrClient(newReplica.getStr(ZkStateReader.BASE_URL_PROP));
      CoreAdminResponse status = CoreAdminRequest.getStatus(newReplica.getStr("core"), coreclient);
      NamedList<Object> coreStatus = status.getCoreStatus(newReplica.getStr("core"));
      String instanceDirStr = (String) coreStatus.get("instanceDir");
      assertEquals(Paths.get(instanceDirStr).toString(), instancePathStr);
      try (HttpSolrClient coreclient = new HttpSolrClient(newReplica.getStr(ZkStateReader.BASE_URL_PROP))) {
        CoreAdminResponse status = CoreAdminRequest.getStatus(newReplica.getStr("core"), coreclient);
        NamedList<Object> coreStatus = status.getCoreStatus(newReplica.getStr("core"));
        String instanceDirStr = (String) coreStatus.get("instanceDir");
        assertEquals(Paths.get(instanceDirStr).toString(), instancePathStr);
      }
 
       //Test to make sure we can't create another replica with an existing core_name of that collection
       String coreName = newReplica.getStr(CORE_NAME_PROP);
diff --git a/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java b/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java
index f1960aa952e..9dcbb8c4365 100644
-- a/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java
@@ -253,12 +253,14 @@ public class HttpPartitionTest extends AbstractFullDistribZkTestBase {
 
     // Check that doc 3 is on the leader but not on the notLeaders
     Replica leader = cloudClient.getZkStateReader().getLeaderRetry(testCollectionName, "shard1", 10000);
    HttpSolrClient leaderSolr = getHttpSolrClient(leader, testCollectionName);
    assertDocExists(leaderSolr, testCollectionName, "3");
    try (HttpSolrClient leaderSolr = getHttpSolrClient(leader, testCollectionName)) {
      assertDocExists(leaderSolr, testCollectionName, "3");
    }
 
     for (Replica notLeader : notLeaders) {
      HttpSolrClient notLeaderSolr = getHttpSolrClient(notLeader, testCollectionName);
      assertDocNotExists(notLeaderSolr, testCollectionName, "3");
      try (HttpSolrClient notLeaderSolr = getHttpSolrClient(notLeader, testCollectionName)) {
        assertDocNotExists(notLeaderSolr, testCollectionName, "3");
      }
     }
 
     // Retry sending doc 3
diff --git a/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java b/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java
index 0436d5e874b..9f1abdedc6d 100644
-- a/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java
@@ -21,12 +21,14 @@ import org.apache.solr.SolrTestCaseJ4.SuppressSSL;
 import org.apache.solr.client.solrj.embedded.JettySolrRunner;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.cloud.Replica;
 import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.lang.invoke.MethodHandles;
import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
@@ -170,9 +172,10 @@ public class LeaderFailoverAfterPartitionTest extends HttpPartitionTest {
             printClusterStateInfo(testCollectionName),
         participatingReplicas.size() >= 2);
 
    
    sendDoc(6);

    SolrInputDocument doc = new SolrInputDocument();
    doc.addField(id, String.valueOf(6));
    doc.addField("a_t", "hello" + 6);
    sendDocsWithRetry(Collections.singletonList(doc), 1, 3, 1);
 
     Set<String> replicasToCheck = new HashSet<>();
     for (Replica stillUp : participatingReplicas)
diff --git a/solr/core/src/test/org/apache/solr/cloud/SSLMigrationTest.java b/solr/core/src/test/org/apache/solr/cloud/SSLMigrationTest.java
index a0bb08f822a..c1b500ca09e 100644
-- a/solr/core/src/test/org/apache/solr/cloud/SSLMigrationTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/SSLMigrationTest.java
@@ -70,7 +70,7 @@ public class SSLMigrationTest extends AbstractFullDistribZkTestBase {
       runner.stop();
     }
     
    HttpClientUtil.setConfigurer(sslConfig.getHttpClientConfigurer());
    HttpClientUtil.setHttpClientBuilder(sslConfig.getHttpClientBuilder());
     for(int i = 0; i < this.jettys.size(); i++) {
       JettySolrRunner runner = jettys.get(i);
       JettyConfig config = JettyConfig.builder()
diff --git a/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java b/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java
index 6d4b9cc3b8f..c691cf037c2 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java
@@ -604,7 +604,6 @@ public class ShardSplitTest extends BasicDistributedZkTest {
   @Override
   protected CloudSolrClient createCloudClient(String defaultCollection) {
     CloudSolrClient client = super.createCloudClient(defaultCollection);
    client.getLbClient().getHttpClient().getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5 * 60 * 1000);
     return client;
   }
 }
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestAuthenticationFramework.java b/solr/core/src/test/org/apache/solr/cloud/TestAuthenticationFramework.java
index b9c72643030..4ee983bca5a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestAuthenticationFramework.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestAuthenticationFramework.java
@@ -16,26 +16,26 @@
  */
 package org.apache.solr.cloud;
 
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

 import javax.servlet.FilterChain;
 import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;

 import org.apache.http.HttpException;
 import org.apache.http.HttpRequest;
 import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.http.protocol.HttpContext;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util.LuceneTestCase.SuppressSysoutChecks;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
 import org.apache.solr.security.AuthenticationPlugin;
import org.apache.solr.security.HttpClientInterceptorPlugin;
import org.apache.solr.security.HttpClientBuilderPlugin;
 import org.apache.solr.util.RevertDefaultThreadHandlerRule;
 import org.junit.ClassRule;
 import org.junit.Rule;
@@ -45,6 +45,8 @@ import org.junit.rules.TestRule;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;

 /**
  * Test of the MiniSolrCloudCluster functionality with authentication enabled.
  */
@@ -114,10 +116,10 @@ public class TestAuthenticationFramework extends TestMiniSolrCloudCluster {
     super.tearDown();
   }
   
  public static class MockAuthenticationPlugin extends AuthenticationPlugin implements HttpClientInterceptorPlugin {
  public static class MockAuthenticationPlugin extends AuthenticationPlugin implements HttpClientBuilderPlugin {
     public static String expectedUsername;
     public static String expectedPassword;

    private HttpRequestInterceptor interceptor;
     @Override
     public void init(Map<String,Object> pluginConfig) {}
 
@@ -141,25 +143,23 @@ public class TestAuthenticationFramework extends TestMiniSolrCloudCluster {
     }
 
     @Override
    public HttpClientConfigurer getClientConfigurer() {
      return new MockClientConfigurer();
    public SolrHttpClientBuilder getHttpClientBuilder(SolrHttpClientBuilder httpClientBuilder) {
      interceptor = new HttpRequestInterceptor() {
        @Override
        public void process(HttpRequest req, HttpContext rsp) throws HttpException, IOException {
          req.addHeader("username", requestUsername);
          req.addHeader("password", requestPassword);
        }
      };

      HttpClientUtil.addRequestInterceptor(interceptor);
      return httpClientBuilder;
     }
 
     @Override
    public void close() {}
    
    private static class MockClientConfigurer extends HttpClientConfigurer {
      @Override
      public void configure(DefaultHttpClient httpClient, SolrParams config) {
        super.configure(httpClient, config);
        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
          @Override
          public void process(HttpRequest req, HttpContext rsp) throws HttpException, IOException {
            req.addHeader("username", requestUsername);
            req.addHeader("password", requestPassword);
          }
        });
      }
    public void close() {
      HttpClientUtil.removeRequestInterceptor(interceptor);
     }
    
   }
 }
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java b/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
index 27818966c03..26db949e5c3 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
@@ -48,7 +48,7 @@ import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.SimpleOrderedMap;

import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;
 
@@ -89,6 +89,16 @@ public class TestCloudDeleteByQuery extends SolrCloudTestCase {
   /** id field doc routing prefix for shard2 */
   private static final String S_TWO_PRE = "XYZ!";
   
  @AfterClass
  private static void afterClass() throws Exception {
    CLOUD_CLIENT.close();
    S_ONE_LEADER_CLIENT.close();
    S_TWO_LEADER_CLIENT.close();
    S_ONE_NON_LEADER_CLIENT.close();
    S_TWO_NON_LEADER_CLIENT.close();
    NO_COLLECTION_CLIENT.close();
  }
  
   @BeforeClass
   private static void createMiniSolrCloudCluster() throws Exception {
     
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java
index 18285617d9a..33fa43b1588 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java
@@ -63,7 +63,7 @@ public class TestMiniSolrCloudClusterBase extends LuceneTestCase {
     NUM_SHARDS = 2;
     REPLICATION_FACTOR = 2;
   }

  
   @Rule
   public TestRule solrTestRules = RuleChain
       .outerRule(new SystemPropertiesRestoreRule());
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java b/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
index 256774d08c3..d0c2da47f10 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
@@ -96,13 +96,14 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
     assertEquals(1, replicas.size());
     String baseUrl = replicas.iterator().next().getStr(ZkStateReader.BASE_URL_PROP);
     if (!baseUrl.endsWith("/")) baseUrl += "/";
    HttpSolrClient client = new HttpSolrClient(baseUrl + "a1x2");
    client.setSoTimeout(5000);
    client.setConnectionTimeout(2000);
    try (HttpSolrClient client = new HttpSolrClient(baseUrl + "a1x2")) {
      client.setSoTimeout(5000);
      client.setConnectionTimeout(2000);
 
    log.info("Making requests to " + baseUrl + "a1x2");
    for (int i=0; i < 10; i++)  {
      client.query(new SolrQuery("*:*"));
      log.info("Making requests to " + baseUrl + "a1x2");
      for (int i = 0; i < 10; i++) {
        client.query(new SolrQuery("*:*"));
      }
     }
 
     Map<String, Integer> shardVsCount = new HashMap<>();
@@ -173,58 +174,59 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
     if (!baseUrl.endsWith("/")) baseUrl += "/";
     String path = baseUrl + "football";
     log.info("Firing queries against path=" + path);
    HttpSolrClient client = new HttpSolrClient(path);
    client.setSoTimeout(5000);
    client.setConnectionTimeout(2000);

    SolrCore leaderCore = null;
    for (JettySolrRunner jetty : jettys) {
      CoreContainer container = jetty.getCoreContainer();
      for (SolrCore core : container.getCores()) {
        if (core.getName().equals(leader.getStr(ZkStateReader.CORE_NAME_PROP))) {
          leaderCore = core;
          break;
    try (HttpSolrClient client = new HttpSolrClient(path)) {
      client.setSoTimeout(5000);
      client.setConnectionTimeout(2000);

      SolrCore leaderCore = null;
      for (JettySolrRunner jetty : jettys) {
        CoreContainer container = jetty.getCoreContainer();
        for (SolrCore core : container.getCores()) {
          if (core.getName().equals(leader.getStr(ZkStateReader.CORE_NAME_PROP))) {
            leaderCore = core;
            break;
          }
         }
       }
    }
    assertNotNull(leaderCore);

    //All queries should be served by the active replica
    //To make sure that's true we keep querying the down replica
    //If queries are getting processed by the down replica then the cluster state hasn't updated for that replica locally
    //So we keep trying till it has updated and then verify if ALL queries go to the active reploca
    long count = 0;
    while (true) {
      count++;
      client.query(new SolrQuery("*:*"));

      SolrRequestHandler select = leaderCore.getRequestHandler("");
      long c = (long) select.getStatistics().get("requests");

      if (c == 1) {
        break;  //cluster state has got update locally
      } else {
        Thread.sleep(100);
      }
      assertNotNull(leaderCore);

      // All queries should be served by the active replica
      // To make sure that's true we keep querying the down replica
      // If queries are getting processed by the down replica then the cluster state hasn't updated for that replica
      // locally
      // So we keep trying till it has updated and then verify if ALL queries go to the active reploca
      long count = 0;
      while (true) {
        count++;
        client.query(new SolrQuery("*:*"));

        SolrRequestHandler select = leaderCore.getRequestHandler("");
        long c = (long) select.getStatistics().get("requests");
 
      if (count > 10000) {
        fail("After 10k queries we still see all requests being processed by the down replica");
        if (c == 1) {
          break; // cluster state has got update locally
        } else {
          Thread.sleep(100);
        }

        if (count > 10000) {
          fail("After 10k queries we still see all requests being processed by the down replica");
        }
       }
    }
 
    //Now we fire a few additional queries and make sure ALL of them
    //are served by the active replica
    int moreQueries = TestUtil.nextInt(random(), 4, 10);
    count = 1; //Since 1 query has already hit the leader
    for (int i=0; i<moreQueries; i++) {
      client.query(new SolrQuery("*:*"));
      count++;
      // Now we fire a few additional queries and make sure ALL of them
      // are served by the active replica
      int moreQueries = TestUtil.nextInt(random(), 4, 10);
      count = 1; // Since 1 query has already hit the leader
      for (int i = 0; i < moreQueries; i++) {
        client.query(new SolrQuery("*:*"));
        count++;
 
      SolrRequestHandler select = leaderCore.getRequestHandler("");
      long c = (long) select.getStatistics().get("requests");
        SolrRequestHandler select = leaderCore.getRequestHandler("");
        long c = (long) select.getStatistics().get("requests");
 
      assertEquals("Query wasn't served by leader", count, c);
        assertEquals("Query wasn't served by leader", count, c);
      }
     }

   }
 }
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java b/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java
index f4dc97de95b..1e2b2428f24 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java
@@ -16,15 +16,12 @@
  */
 package org.apache.solr.cloud;
 
import javax.security.auth.login.Configuration;

 import java.io.File;
 import java.util.List;
 import java.util.Locale;
 import java.util.Properties;
 
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;
import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;
import javax.security.auth.login.Configuration;
 
 import org.apache.commons.io.FileUtils;
 import org.apache.hadoop.minikdc.MiniKdc;
@@ -36,8 +33,6 @@ import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.embedded.JettyConfig;
 import org.apache.solr.client.solrj.embedded.JettySolrRunner;
 import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
 import org.apache.solr.client.solrj.request.CollectionAdminRequest;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrInputDocument;
@@ -54,6 +49,9 @@ import org.junit.Test;
 import org.junit.rules.RuleChain;
 import org.junit.rules.TestRule;
 
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;
import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;

 /**
  * Test 5 nodes Solr cluster with Kerberos plugin enabled.
  * This test is Ignored right now as Mini KDC has a known bug that
@@ -82,6 +80,7 @@ public class TestSolrCloudWithKerberosAlt extends LuceneTestCase {
   private MiniKdc kdc;
 
   private Locale savedLocale; // in case locale is broken and we need to fill in a working locale
  
   @Rule
   public TestRule solrTestRules = RuleChain
       .outerRule(new SystemPropertiesRestoreRule());
@@ -101,7 +100,6 @@ public class TestSolrCloudWithKerberosAlt extends LuceneTestCase {
     savedLocale = KerberosTestUtil.overrideLocaleIfNotSpportedByMiniKdc();
     super.setUp();
     setupMiniKdc();
    HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());
   }
 
   private void setupMiniKdc() throws Exception {
@@ -157,7 +155,6 @@ public class TestSolrCloudWithKerberosAlt extends LuceneTestCase {
   }
 
   protected void testCollectionCreateSearchDelete() throws Exception {
    HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());
     String collectionName = "testkerberoscollection";
 
     MiniSolrCloudCluster miniCluster
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java
index 054c0745fd4..929d736d79f 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java
@@ -17,6 +17,7 @@
 package org.apache.solr.cloud;
 
 import java.io.File;
import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.net.URL;
 import java.util.ArrayList;
@@ -49,7 +50,6 @@ import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.util.RevertDefaultThreadHandlerRule;
 
 import org.junit.AfterClass;
 import org.junit.Before;
@@ -204,6 +204,21 @@ public class TestTolerantUpdateProcessorCloud extends SolrCloudTestCase {
     }
   }
   
  @AfterClass
  public static void afterClass() throws IOException {
   close(S_ONE_LEADER_CLIENT);
   close(S_TWO_LEADER_CLIENT);
   close(S_ONE_NON_LEADER_CLIENT);
   close(S_TWO_NON_LEADER_CLIENT);
   close(NO_COLLECTION_CLIENT);
  }
  
  private static void close(SolrClient client) throws IOException {
    if (client != null) {
      client.close();
    }
  }
  
   @Before
   private void clearCollection() throws Exception {
     assertEquals(0, CLOUD_CLIENT.deleteByQuery("*:*").getStatus());
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java
index b3f0423c999..a722ad2cdb0 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java
@@ -16,59 +16,43 @@
  */
 package org.apache.solr.cloud;
 
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.addErr;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.assertUpdateTolerantErrors;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.delIErr;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.delQErr;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.f;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.update;
import static org.apache.solr.common.params.CursorMarkParams.CURSOR_MARK_PARAM;
import static org.apache.solr.common.params.CursorMarkParams.CURSOR_MARK_START;

 import java.io.File;
import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.net.URL;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.BitSet;
 import java.util.HashMap;
import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
import java.util.Set;
 
 import org.apache.lucene.util.TestUtil;
import org.apache.solr.cloud.SolrCloudTestCase;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.assertUpdateTolerantErrors;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.addErr;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.delIErr;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.delQErr;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.f;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.update;
import static org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.ExpectedErr;
 import org.apache.solr.client.solrj.SolrClient;
 import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.request.UpdateRequest;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.client.solrj.response.UpdateResponse;
import static org.apache.solr.common.params.CursorMarkParams.CURSOR_MARK_PARAM;
import static org.apache.solr.common.params.CursorMarkParams.CURSOR_MARK_NEXT;
import static org.apache.solr.common.params.CursorMarkParams.CURSOR_MARK_START;
import org.apache.solr.cloud.TestTolerantUpdateProcessorCloud.ExpectedErr;
 import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.ToleratedUpdateError;
import org.apache.solr.common.ToleratedUpdateError.CmdType;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
 import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.util.RevertDefaultThreadHandlerRule;

 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;

 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -92,7 +76,7 @@ public class TestTolerantUpdateProcessorRandomCloud extends SolrCloudTestCase {
   /** A basic client for operations at the cloud level, default collection will be set */
   private static CloudSolrClient CLOUD_CLIENT;
   /** one HttpSolrClient for each server */
  private static List<SolrClient> NODE_CLIENTS;
  private static List<HttpSolrClient> NODE_CLIENTS;
 
   @BeforeClass
   private static void createMiniSolrCloudCluster() throws Exception {
@@ -123,7 +107,12 @@ public class TestTolerantUpdateProcessorRandomCloud extends SolrCloudTestCase {
     CLOUD_CLIENT = cluster.getSolrClient();
     CLOUD_CLIENT.setDefaultCollection(COLLECTION_NAME);
 
    NODE_CLIENTS = new ArrayList<SolrClient>(numServers);
    if (NODE_CLIENTS != null) {
      for (HttpSolrClient client : NODE_CLIENTS) {
        client.close();
      }
    }
    NODE_CLIENTS = new ArrayList<HttpSolrClient>(numServers);
     
     for (JettySolrRunner jetty : cluster.getJettySolrRunners()) {
       URL jettyURL = jetty.getBaseUrl();
@@ -142,6 +131,15 @@ public class TestTolerantUpdateProcessorRandomCloud extends SolrCloudTestCase {
     assertEquals("index should be empty", 0L, countDocs(CLOUD_CLIENT));
   }
   
  @AfterClass
  public static void afterClass() throws IOException {
    if (NODE_CLIENTS != null) {
      for (HttpSolrClient client : NODE_CLIENTS) {
        client.close();
      }
    }
  }
  
   public void testRandomUpdates() throws Exception {
     final int maxDocId = atLeast(10000);
     final BitSet expectedDocIds = new BitSet(maxDocId+1);
diff --git a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
index 7b293ca5ea6..912369787a0 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
@@ -326,7 +326,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
   }
 
   private static class MockCoreContainer extends CoreContainer {

    UpdateShardHandler updateShardHandler = new UpdateShardHandler(UpdateShardHandlerConfig.DEFAULT);
     public MockCoreContainer() {
       super((Object)null);
       this.shardHandlerFactory = new HttpShardHandlerFactory();
@@ -338,7 +338,12 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
     
     @Override
     public UpdateShardHandler getUpdateShardHandler() {
      return new UpdateShardHandler(UpdateShardHandlerConfig.DEFAULT);
      return updateShardHandler;
    }
    
    @Override
    public void shutdown() {
      updateShardHandler.close();
     }
 
   }
diff --git a/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java b/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java
index 10cc46c5165..3c45e2327e1 100644
-- a/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java
@@ -32,7 +32,6 @@ import org.apache.solr.common.cloud.Slice;
 import org.apache.solr.common.cloud.SolrZkClient;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.util.Utils;
import org.apache.zookeeper.KeeperException;
 
 public class ZkStateReaderTest extends SolrTestCaseJ4 {
 
@@ -96,7 +95,7 @@ public class ZkStateReaderTest extends SolrTestCaseJ4 {
         if (explicitRefresh) {
           reader.forceUpdateCollection("c1");
         } else {
          for (int i = 0; i < 100; ++i) {
          for (int i = 0; i < 500; ++i) {
             if (reader.getClusterState().hasCollection("c1")) {
               break;
             }
@@ -124,7 +123,7 @@ public class ZkStateReaderTest extends SolrTestCaseJ4 {
         if (explicitRefresh) {
           reader.forceUpdateCollection("c1");
         } else {
          for (int i = 0; i < 100; ++i) {
          for (int i = 0; i < 500; ++i) {
             if (reader.getClusterState().getCollection("c1").getStateFormat() == 2) {
               break;
             }
diff --git a/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java b/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java
index 7875b7c6c13..71f3ee9820e 100644
-- a/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java
++ b/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java
@@ -149,14 +149,12 @@ public class OpenCloseCoreStressTest extends SolrTestCaseJ4 {
 
     for (int idx = 0; idx < indexingThreads; ++idx) {
       HttpSolrClient client = new HttpSolrClient(url);
      client.setDefaultMaxConnectionsPerHost(25);
       client.setConnectionTimeout(30000);
       client.setSoTimeout(60000);
       indexingClients.add(client);
     }
     for (int idx = 0; idx < queryThreads; ++idx) {
       HttpSolrClient client = new HttpSolrClient(url);
      client.setDefaultMaxConnectionsPerHost(25);
       client.setConnectionTimeout(30000);
       client.setSoTimeout(30000);
       queryingClients.add(client);
diff --git a/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java b/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
index 44f9e5c3c14..d23b8b14714 100644
-- a/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
++ b/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
@@ -336,7 +336,6 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
 
     CoreContainer cc = init(CUSTOM_HANDLERS_SOLR_XML);
     try {
      cc.load();
       assertThat(cc.getCollectionsHandler(), is(instanceOf(CustomCollectionsHandler.class)));
       assertThat(cc.getInfoHandler(), is(instanceOf(CustomInfoHandler.class)));
       assertThat(cc.getMultiCoreHandler(), is(instanceOf(CustomCoreAdminHandler.class)));
diff --git a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
index eab3e87ec49..8ea8d1b7e03 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
++ b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
@@ -159,8 +159,6 @@ public class TestReplicationHandler extends SolrTestCaseJ4 {
       HttpSolrClient client = new HttpSolrClient(buildUrl(port) + "/" + DEFAULT_TEST_CORENAME);
       client.setConnectionTimeout(15000);
       client.setSoTimeout(60000);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       return client;
     }
     catch (Exception ex) {
diff --git a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
index 29c0dd75239..fe1a4ea14bb 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
++ b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
@@ -82,8 +82,6 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
       HttpSolrClient client = new HttpSolrClient(buildUrl(port, context) + "/" + DEFAULT_TEST_CORENAME);
       client.setConnectionTimeout(15000);
       client.setSoTimeout(60000);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       return client;
     }
     catch (Exception ex) {
diff --git a/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java b/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java
index 1218783d7b2..0a8076474db 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java
++ b/solr/core/src/test/org/apache/solr/handler/TestRestoreCore.java
@@ -75,8 +75,6 @@ public class TestRestoreCore extends SolrJettyTestBase {
       HttpSolrClient client = new HttpSolrClient(buildUrl(port, context) + "/" + DEFAULT_TEST_CORENAME);
       client.setConnectionTimeout(15000);
       client.setSoTimeout(60000);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       return client;
     }
     catch (Exception ex) {
diff --git a/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryElevationComponentTest.java b/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryElevationComponentTest.java
index 0c18b25fc24..91f39dcc8d1 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryElevationComponentTest.java
++ b/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryElevationComponentTest.java
@@ -116,6 +116,7 @@ public class DistributedQueryElevationComponentTest extends BaseDistributedSearc
         .setSort("id", SolrQuery.ORDER.desc);
     setDistributedParams(solrQuery);
     response = client.query(solrQuery);
    client.close();
 
     assertTrue(response.getResults().getNumFound() > 0);
     document = response.getResults().get(0);
diff --git a/solr/core/src/test/org/apache/solr/search/AnalyticsMergeStrategyTest.java b/solr/core/src/test/org/apache/solr/search/AnalyticsMergeStrategyTest.java
index ed3318213d9..160d2f7fc0e 100644
-- a/solr/core/src/test/org/apache/solr/search/AnalyticsMergeStrategyTest.java
++ b/solr/core/src/test/org/apache/solr/search/AnalyticsMergeStrategyTest.java
@@ -19,6 +19,7 @@ package org.apache.solr.search;
 import org.apache.lucene.util.Constants;
 import org.apache.solr.BaseDistributedSearchTestCase;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.SolrTestCaseJ4.SuppressObjectReleaseTracker;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.util.NamedList;
@@ -32,6 +33,7 @@ import org.junit.Test;
  */
 
 @SolrTestCaseJ4.SuppressSSL(bugUrl="https://issues.apache.org/jira/browse/SOLR-8433")
@SuppressObjectReleaseTracker(bugUrl="https://issues.apache.org/jira/browse/SOLR-8899")
 public class AnalyticsMergeStrategyTest extends BaseDistributedSearchTestCase {
 
 
diff --git a/solr/core/src/test/org/apache/solr/search/stats/TestDistribIDF.java b/solr/core/src/test/org/apache/solr/search/stats/TestDistribIDF.java
index 9bb4d21eec2..877742b8b7d 100644
-- a/solr/core/src/test/org/apache/solr/search/stats/TestDistribIDF.java
++ b/solr/core/src/test/org/apache/solr/search/stats/TestDistribIDF.java
@@ -117,27 +117,30 @@ public class TestDistribIDF extends SolrTestCaseJ4 {
 
     //Test against all nodes
     for (JettySolrRunner jettySolrRunner : solrCluster.getJettySolrRunners()) {
      SolrClient solrClient = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString());
      SolrClient solrClient_local = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString());
      try (SolrClient solrClient = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString())) {
        try (SolrClient solrClient_local = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString())) {
 
      SolrQuery query = new SolrQuery("cat:football");
      query.setFields("*,score");
      QueryResponse queryResponse = solrClient.query("onecollection", query);
      assertEquals(2, queryResponse.getResults().getNumFound());
      float score1 = (float) queryResponse.getResults().get(0).get("score");
      float score2 = (float) queryResponse.getResults().get(1).get("score");
      assertEquals("Doc1 score=" + score1 + " Doc2 score=" + score2, 0, Float.compare(score1, score2));
          SolrQuery query = new SolrQuery("cat:football");
          query.setFields("*,score");
          QueryResponse queryResponse = solrClient.query("onecollection", query);
          assertEquals(2, queryResponse.getResults().getNumFound());
          float score1 = (float) queryResponse.getResults().get(0).get("score");
          float score2 = (float) queryResponse.getResults().get(1).get("score");
          assertEquals("Doc1 score=" + score1 + " Doc2 score=" + score2, 0, Float.compare(score1, score2));
 
      query = new SolrQuery("cat:football");
      query.setShowDebugInfo(true);
      query.setFields("*,score");
      queryResponse = solrClient_local.query("onecollection_local", query);
      assertEquals(2, queryResponse.getResults().getNumFound());
      assertEquals(2, queryResponse.getResults().get(0).get("id"));
      assertEquals(1, queryResponse.getResults().get(1).get("id"));
      float score1_local = (float) queryResponse.getResults().get(0).get("score");
      float score2_local = (float) queryResponse.getResults().get(1).get("score");
      assertEquals("Doc1 score=" + score1_local + " Doc2 score=" + score2_local, 1, Float.compare(score1_local, score2_local));
          query = new SolrQuery("cat:football");
          query.setShowDebugInfo(true);
          query.setFields("*,score");
          queryResponse = solrClient_local.query("onecollection_local", query);
          assertEquals(2, queryResponse.getResults().getNumFound());
          assertEquals(2, queryResponse.getResults().get(0).get("id"));
          assertEquals(1, queryResponse.getResults().get(1).get("id"));
          float score1_local = (float) queryResponse.getResults().get(0).get("score");
          float score2_local = (float) queryResponse.getResults().get(1).get("score");
          assertEquals("Doc1 score=" + score1_local + " Doc2 score=" + score2_local, 1,
              Float.compare(score1_local, score2_local));
        }
      }
     }
   }
 
@@ -161,28 +164,33 @@ public class TestDistribIDF extends SolrTestCaseJ4 {
 
     //Test against all nodes
     for (JettySolrRunner jettySolrRunner : solrCluster.getJettySolrRunners()) {
      SolrClient solrClient = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString());
      SolrClient solrClient_local = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString());
 
      SolrQuery query = new SolrQuery("cat:football");
      query.setFields("*,score").add("collection", "collection1,collection2");
      QueryResponse queryResponse = solrClient.query("collection1", query);
      assertEquals(2, queryResponse.getResults().getNumFound());
      float score1 = (float) queryResponse.getResults().get(0).get("score");
      float score2 = (float) queryResponse.getResults().get(1).get("score");
      assertEquals("Doc1 score=" + score1 + " Doc2 score=" + score2, 0, Float.compare(score1, score2));
      try (SolrClient solrClient = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString())) {
 
        try (SolrClient solrClient_local = new HttpSolrClient(jettySolrRunner.getBaseUrl().toString())) {
          SolrQuery query = new SolrQuery("cat:football");
          query.setFields("*,score").add("collection", "collection1,collection2");
          QueryResponse queryResponse = solrClient.query("collection1", query);
          assertEquals(2, queryResponse.getResults().getNumFound());
          float score1 = (float) queryResponse.getResults().get(0).get("score");
          float score2 = (float) queryResponse.getResults().get(1).get("score");
          assertEquals("Doc1 score=" + score1 + " Doc2 score=" + score2, 0, Float.compare(score1, score2));
 
      query = new SolrQuery("cat:football");
      query.setFields("*,score").add("collection", "collection1_local,collection2_local");
      queryResponse = solrClient_local.query("collection1_local", query);
      assertEquals(2, queryResponse.getResults().getNumFound());
      assertEquals(2, queryResponse.getResults().get(0).get("id"));
      assertEquals(1, queryResponse.getResults().get(1).get("id"));
      float score1_local = (float) queryResponse.getResults().get(0).get("score");
      float score2_local = (float) queryResponse.getResults().get(1).get("score");
      assertEquals("Doc1 score=" + score1_local + " Doc2 score=" + score2_local, 1, Float.compare(score1_local, score2_local));
          query = new SolrQuery("cat:football");
          query.setFields("*,score").add("collection", "collection1_local,collection2_local");
          queryResponse = solrClient_local.query("collection1_local", query);
          assertEquals(2, queryResponse.getResults().getNumFound());
          assertEquals(2, queryResponse.getResults().get(0).get("id"));
          assertEquals(1, queryResponse.getResults().get(1).get("id"));
          float score1_local = (float) queryResponse.getResults().get(0).get("score");
          float score2_local = (float) queryResponse.getResults().get(1).get("score");
          assertEquals("Doc1 score=" + score1_local + " Doc2 score=" + score2_local, 1,
              Float.compare(score1_local, score2_local));
        }

      }
     }
    
   }
 
   private void createCollection(String name, String config) throws Exception {
diff --git a/solr/core/src/test/org/apache/solr/security/BasicAuthIntegrationTest.java b/solr/core/src/test/org/apache/solr/security/BasicAuthIntegrationTest.java
index 96a8c146eb0..9361615d9ff 100644
-- a/solr/core/src/test/org/apache/solr/security/BasicAuthIntegrationTest.java
++ b/solr/core/src/test/org/apache/solr/security/BasicAuthIntegrationTest.java
@@ -36,6 +36,7 @@ import org.apache.http.util.EntityUtils;
 import org.apache.solr.client.solrj.SolrRequest;
 import org.apache.solr.client.solrj.embedded.JettySolrRunner;
 import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.request.CollectionAdminRequest;
 import org.apache.solr.client.solrj.request.GenericSolrRequest;
@@ -61,7 +62,6 @@ import static java.nio.charset.StandardCharsets.UTF_8;
 import static java.util.Collections.singletonMap;
 import static org.apache.solr.common.cloud.ZkStateReader.BASE_URL_PROP;
 

 public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
 
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
@@ -78,7 +78,7 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     cloudSolrClient.setDefaultCollection(null);
 
     NamedList<Object> rsp;
    HttpClient cl = cloudSolrClient.getLbClient().getHttpClient();
    HttpClient cl = HttpClientUtil.createClient(null);
     String baseUrl = getRandomReplica(zkStateReader.getClusterState().getCollection(defaultCollName), random()).getStr(BASE_URL_PROP);
     verifySecurityStatus(cl, baseUrl + authcPrefix, "/errorMessages", null, 20);
     zkClient.setData("/security.json", STD_CONF.replaceAll("'", "\"").getBytes(UTF_8), true);
@@ -94,6 +94,7 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
         break;
       }
     }

     assertTrue("No server found to restart , looking for : "+baseUrl , found);
 
     String command = "{\n" +
@@ -118,6 +119,7 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     verifySecurityStatus(cl, baseUrl + authcPrefix, "authentication.enabled", "true", 20);
     HttpResponse r = cl.execute(httpPost);
     int statusCode = r.getStatusLine().getStatusCode();
    Utils.consumeFully(r.getEntity());
     assertEquals("proper_cred sent, but access denied", 200, statusCode);
     baseUrl = getRandomReplica(zkStateReader.getClusterState().getCollection(defaultCollName), random()).getStr(BASE_URL_PROP);
 
@@ -132,6 +134,7 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
     r = cl.execute(httpPost);
     assertEquals(200, r.getStatusLine().getStatusCode());
    Utils.consumeFully(r.getEntity());
 
     baseUrl = getRandomReplica(zkStateReader.getClusterState().getCollection(defaultCollName), random()).getStr(BASE_URL_PROP);
     verifySecurityStatus(cl, baseUrl + authzPrefix, "authorization/user-role/harry", NOT_NULL_PREDICATE, 20);
@@ -148,6 +151,7 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     verifySecurityStatus(cl, baseUrl + authzPrefix, "authorization/user-role/harry", NOT_NULL_PREDICATE, 20);
     r = cl.execute(httpPost);
     assertEquals(200, r.getStatusLine().getStatusCode());
    Utils.consumeFully(r.getEntity());
 
     verifySecurityStatus(cl, baseUrl + authzPrefix, "authorization/permissions[1]/collection", "x", 20);
 
@@ -156,25 +160,26 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     httpPost.setEntity(new ByteArrayEntity(Utils.toJSON(singletonMap("set-permission", Utils.makeMap
         ("name","collection-admin-edit", "role", "admin" )))));
     r = cl.execute(httpPost);

    Utils.consumeFully(r.getEntity());
     verifySecurityStatus(cl, baseUrl + authzPrefix, "authorization/permissions[2]/name", "collection-admin-edit", 20);
 
     CollectionAdminRequest.Reload reload = new CollectionAdminRequest.Reload();
     reload.setCollectionName(defaultCollName);
 
    HttpSolrClient solrClient = new HttpSolrClient(baseUrl);
    try {
      rsp = solrClient.request(reload);
      fail("must have failed");
    } catch (HttpSolrClient.RemoteSolrException e) {
    try (HttpSolrClient solrClient = new HttpSolrClient(baseUrl)) {
      try {
        rsp = solrClient.request(reload);
        fail("must have failed");
      } catch (HttpSolrClient.RemoteSolrException e) {
 
    }
    reload.setMethod(SolrRequest.METHOD.POST);
    try {
      rsp = solrClient.request(reload);
      fail("must have failed");
    } catch (HttpSolrClient.RemoteSolrException e) {
      }
      reload.setMethod(SolrRequest.METHOD.POST);
      try {
        rsp = solrClient.request(reload);
        fail("must have failed");
      } catch (HttpSolrClient.RemoteSolrException e) {
 
      }
     }
     cloudSolrClient.request(new CollectionAdminRequest.Reload()
         .setCollectionName(defaultCollName)
@@ -197,6 +202,7 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
     r = cl.execute(httpPost);
     assertEquals(200,r.getStatusLine().getStatusCode());
    Utils.consumeFully(r.getEntity());
 
     SolrInputDocument doc = new SolrInputDocument();
     doc.setField("id","4");
@@ -205,6 +211,8 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     update.add(doc);
     update.setCommitWithin(100);
     cloudSolrClient.request(update);
    
    HttpClientUtil.close(cl);
   }
 
   public static void verifySecurityStatus(HttpClient cl, String url, String objPath, Object expected, int count) throws Exception {
@@ -213,9 +221,10 @@ public class BasicAuthIntegrationTest extends TestMiniSolrCloudClusterBase {
     List<String> hierarchy = StrUtils.splitSmart(objPath, '/');
     for (int i = 0; i < count; i++) {
       HttpGet get = new HttpGet(url);
      s = EntityUtils.toString(cl.execute(get).getEntity());
      HttpResponse rsp = cl.execute(get);
      s = EntityUtils.toString(rsp.getEntity());
       Map m = (Map) Utils.fromJSONString(s);

      Utils.consumeFully(rsp.getEntity());
       Object actual = Utils.getObjectByPath(m, true, hierarchy);
       if (expected instanceof Predicate) {
         Predicate predicate = (Predicate) expected;
diff --git a/solr/core/src/test/org/apache/solr/security/PKIAuthenticationIntegrationTest.java b/solr/core/src/test/org/apache/solr/security/PKIAuthenticationIntegrationTest.java
index 26aee070d9a..30fe9338f72 100644
-- a/solr/core/src/test/org/apache/solr/security/PKIAuthenticationIntegrationTest.java
++ b/solr/core/src/test/org/apache/solr/security/PKIAuthenticationIntegrationTest.java
@@ -31,8 +31,6 @@ import org.apache.solr.cloud.AbstractFullDistribZkTestBase;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.util.Utils;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.zookeeper.CreateMode;
 import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
diff --git a/solr/core/src/test/org/apache/solr/security/TestAuthorizationFramework.java b/solr/core/src/test/org/apache/solr/security/TestAuthorizationFramework.java
index 3f573767981..4c4b52eb867 100644
-- a/solr/core/src/test/org/apache/solr/security/TestAuthorizationFramework.java
++ b/solr/core/src/test/org/apache/solr/security/TestAuthorizationFramework.java
@@ -28,6 +28,7 @@ import org.apache.http.client.HttpClient;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.util.EntityUtils;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.cloud.AbstractFullDistribZkTestBase;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.params.ModifiableSolrParams;
@@ -89,7 +90,7 @@ public class TestAuthorizationFramework extends AbstractFullDistribZkTestBase {
     List<String> hierarchy = StrUtils.splitSmart(objPath, '/');
     for (int i = 0; i < count; i++) {
       HttpGet get = new HttpGet(url);
      s = EntityUtils.toString(cl.execute(get).getEntity());
      s = EntityUtils.toString(cl.execute(get, HttpClientUtil.createNewHttpClientRequestContext()).getEntity());
       Map m = (Map) Utils.fromJSONString(s);
 
       Object actual = Utils.getObjectByPath(m, true, hierarchy);
diff --git a/solr/core/src/test/org/apache/solr/update/AutoCommitTest.java b/solr/core/src/test/org/apache/solr/update/AutoCommitTest.java
index 080a02fd087..3221693e547 100644
-- a/solr/core/src/test/org/apache/solr/update/AutoCommitTest.java
++ b/solr/core/src/test/org/apache/solr/update/AutoCommitTest.java
@@ -317,7 +317,7 @@ public class AutoCommitTest extends AbstractSolrTestCase {
     
     // Delete one document with commitWithin
     req.setContentStreams( toContentStreams(
      delI("529", "commitWithin", "1000"), null ) );
      delI("529", "commitWithin", "2000"), null ) );
     trigger.reset();
     handler.handleRequest( req, rsp );
       
diff --git a/solr/server/etc/jetty-http.xml b/solr/server/etc/jetty-http.xml
index 90e523ad4a8..6d92830ec10 100644
-- a/solr/server/etc/jetty-http.xml
++ b/solr/server/etc/jetty-http.xml
@@ -35,7 +35,7 @@
         </Arg>
         <Set name="host"><Property name="jetty.host" /></Set>
         <Set name="port"><Property name="jetty.port" default="8983" /></Set>
        <Set name="idleTimeout"><Property name="solr.jetty.http.idleTimeout" default="50000"/></Set>
        <Set name="idleTimeout"><Property name="solr.jetty.http.idleTimeout" default="120000"/></Set>
         <Set name="soLingerTime"><Property name="solr.jetty.http.soLingerTime" default="-1"/></Set>
         <Set name="acceptorPriorityDelta"><Property name="solr.jetty.http.acceptorPriorityDelta" default="0"/></Set>
         <Set name="selectorPriorityDelta"><Property name="solr.jetty.http.selectorPriorityDelta" default="0"/></Set>
diff --git a/solr/server/etc/jetty-https.xml b/solr/server/etc/jetty-https.xml
index e2770b1eb30..d34d4bd14ce 100644
-- a/solr/server/etc/jetty-https.xml
++ b/solr/server/etc/jetty-https.xml
@@ -42,7 +42,7 @@
         </Arg>
         <Set name="host"><Property name="solr.jetty.host" /></Set>
         <Set name="port"><Property name="solr.jetty.https.port" default="8983" /></Set>
        <Set name="idleTimeout"><Property name="solr.jetty.https.timeout" default="50000"/></Set>
        <Set name="idleTimeout"><Property name="solr.jetty.https.timeout" default="120000"/></Set>
         <Set name="soLingerTime"><Property name="solr.jetty.https.soLingerTime" default="-1"/></Set>
         <Set name="acceptorPriorityDelta"><Property name="solr.jetty.ssl.acceptorPriorityDelta" default="0"/></Set>
         <Set name="selectorPriorityDelta"><Property name="solr.jetty.ssl.selectorPriorityDelta" default="0"/></Set>
diff --git a/solr/server/etc/jetty.xml b/solr/server/etc/jetty.xml
index c819f0409cf..f1b94c855db 100644
-- a/solr/server/etc/jetty.xml
++ b/solr/server/etc/jetty.xml
@@ -35,7 +35,7 @@
   <Get name="ThreadPool">
     <Set name="minThreads" type="int"><Property name="solr.jetty.threads.min" default="10"/></Set>
     <Set name="maxThreads" type="int"><Property name="solr.jetty.threads.max" default="10000"/></Set>
    <Set name="idleTimeout" type="int"><Property name="solr.jetty.threads.idle.timeout" default="5000"/></Set>
    <Set name="idleTimeout" type="int"><Property name="solr.jetty.threads.idle.timeout" default="120000"/></Set>
     <Set name="stopTimeout" type="int"><Property name="solr.jetty.threads.stop.timeout" default="60000"/></Set>
     <Set name="detailedDump">false</Set>
   </Get>
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
index edfe1c3e117..43a4c183391 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
@@ -1345,4 +1345,12 @@ public class CloudSolrClient extends SolrClient {
     }    
     return results;
   }
  
  public void setConnectionTimeout(int timeout) {
    this.lbClient.setConnectionTimeout(timeout); 
  }

  public void setSoTimeout(int timeout) {
    this.lbClient.setSoTimeout(timeout);
  }
 }
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrClient.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrClient.java
index 5bec96a27f5..d197e4bb506 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrClient.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrClient.java
@@ -20,6 +20,7 @@ import org.apache.http.HttpResponse;
 import org.apache.http.HttpStatus;
 import org.apache.http.NameValuePair;
 import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig.Builder;
 import org.apache.http.client.methods.HttpPost;
 import org.apache.http.entity.ContentProducer;
 import org.apache.http.entity.EntityTemplate;
@@ -86,6 +87,8 @@ public class ConcurrentUpdateSolrClient extends SolrClient {
   int pollQueueTime = 250;
   private final boolean streamDeletes;
   private boolean internalHttpClient;
  private volatile Integer connectionTimeout;
  private volatile Integer soTimeout;
 
   /**
    * Uses an internally managed HttpClient instance.
@@ -274,11 +277,22 @@ public class ConcurrentUpdateSolrClient extends SolrClient {
 
           method = new HttpPost(client.getBaseURL() + "/update"
               + requestParams.toQueryString());
          
          Builder requestConfigBuilder = HttpClientUtil.createDefaultRequestConfigBuilder();
          if (soTimeout != null) {
            requestConfigBuilder.setSocketTimeout(soTimeout);
          }
          if (connectionTimeout != null) {
            requestConfigBuilder.setConnectTimeout(connectionTimeout);
          }
  
          method.setConfig(requestConfigBuilder.build());
          
           method.setEntity(template);
           method.addHeader("User-Agent", HttpSolrClient.AGENT);
           method.addHeader("Content-Type", contentType);
 
          response = client.getHttpClient().execute(method);
          response = client.getHttpClient().execute(method, HttpClientUtil.createNewHttpClientRequestContext());
           rspBody = response.getEntity().getContent();
           int statusCode = response.getStatusLine().getStatusCode();
           if (statusCode != HttpStatus.SC_OK) {
@@ -489,7 +503,7 @@ public class ConcurrentUpdateSolrClient extends SolrClient {
   }
   
   public void setConnectionTimeout(int timeout) {
    HttpClientUtil.setConnectionTimeout(client.getHttpClient(), timeout);
    this.connectionTimeout = timeout;
   }
 
   /**
@@ -497,7 +511,7 @@ public class ConcurrentUpdateSolrClient extends SolrClient {
    * not for indexing.
    */
   public void setSoTimeout(int timeout) {
    HttpClientUtil.setSoTimeout(client.getHttpClient(), timeout);
    this.soTimeout = timeout;
   }
 
   public void shutdownNow() {
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientConfigurer.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientConfigurer.java
deleted file mode 100644
index 0f97bd0f894..00000000000
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientConfigurer.java
++ /dev/null
@@ -1,100 +0,0 @@
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
package org.apache.solr.client.solrj.impl;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;

/**
 * The default http client configurer. If the behaviour needs to be customized a
 * new HttpCilentConfigurer can be set by calling
 * {@link HttpClientUtil#setConfigurer(HttpClientConfigurer)}
 */
public class HttpClientConfigurer {
  
  public void configure(DefaultHttpClient httpClient, SolrParams config) {
    
    if (config.get(HttpClientUtil.PROP_MAX_CONNECTIONS) != null) {
      HttpClientUtil.setMaxConnections(httpClient,
          config.getInt(HttpClientUtil.PROP_MAX_CONNECTIONS));
    }
    
    if (config.get(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST) != null) {
      HttpClientUtil.setMaxConnectionsPerHost(httpClient,
          config.getInt(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST));
    }
    
    if (config.get(HttpClientUtil.PROP_CONNECTION_TIMEOUT) != null) {
      HttpClientUtil.setConnectionTimeout(httpClient,
          config.getInt(HttpClientUtil.PROP_CONNECTION_TIMEOUT));
    }
    
    if (config.get(HttpClientUtil.PROP_SO_TIMEOUT) != null) {
      HttpClientUtil.setSoTimeout(httpClient,
          config.getInt(HttpClientUtil.PROP_SO_TIMEOUT));
    }
    
    if (config.get(HttpClientUtil.PROP_FOLLOW_REDIRECTS) != null) {
      HttpClientUtil.setFollowRedirects(httpClient,
          config.getBool(HttpClientUtil.PROP_FOLLOW_REDIRECTS));
    }
    
    // always call setUseRetry, whether it is in config or not
    HttpClientUtil.setUseRetry(httpClient,
        config.getBool(HttpClientUtil.PROP_USE_RETRY, true));
    
    final String basicAuthUser = config
        .get(HttpClientUtil.PROP_BASIC_AUTH_USER);
    final String basicAuthPass = config
        .get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    HttpClientUtil.setBasicAuth(httpClient, basicAuthUser, basicAuthPass);
    
    if (config.get(HttpClientUtil.PROP_ALLOW_COMPRESSION) != null) {
      HttpClientUtil.setAllowCompression(httpClient,
          config.getBool(HttpClientUtil.PROP_ALLOW_COMPRESSION));
    }
    
    boolean sslCheckPeerName = toBooleanDefaultIfNull(
        toBooleanObject(System.getProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME)), true);
    if(sslCheckPeerName == false) {
      HttpClientUtil.setHostNameVerifier(httpClient, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }
  }
  
  public static boolean toBooleanDefaultIfNull(Boolean bool, boolean valueIfNull) {
    if (bool == null) {
      return valueIfNull;
    }
    return bool.booleanValue() ? true : false;
  }
  
  public static Boolean toBooleanObject(String str) {
    if ("true".equalsIgnoreCase(str)) {
      return Boolean.TRUE;
    } else if ("false".equalsIgnoreCase(str)) {
      return Boolean.FALSE;
    }
    // no match
    return null;
  }
}
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
index 2ad89757e04..72297b3499e 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
@@ -19,11 +19,11 @@ package org.apache.solr.client.solrj.impl;
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
 import java.util.zip.GZIPInputStream;
 import java.util.zip.InflaterInputStream;
 
@@ -37,40 +37,50 @@ import org.apache.http.HttpResponse;
 import org.apache.http.HttpResponseInterceptor;
 import org.apache.http.auth.AuthScope;
 import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
 import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientParamBean;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
 import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicCredentialsProvider;
 import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager; // jdoc
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
 import org.apache.http.protocol.HttpContext;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ObjectReleaseTracker;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Utility class for creating/configuring httpclient instances. 
 * 
 * This class can touch internal HttpClient details and is subject to change.
 * 
 * @lucene.experimental
  */
 public class HttpClientUtil {
  // socket timeout measured in ms, closes a socket if read
  // takes longer than x ms to complete. throws
  // java.net.SocketTimeoutException: Read timed out exception
  public static final String PROP_SO_TIMEOUT = "socketTimeout";
  // connection timeout measures in ms, closes a socket if connection
  // cannot be established within x ms. with a
  // java.net.SocketTimeoutException: Connection timed out
  public static final String PROP_CONNECTION_TIMEOUT = "connTimeout";
  
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  private static final int DEFAULT_CONNECT_TIMEOUT = 60000;
  private static final int DEFAULT_SO_TIMEOUT = 600000;
  
  private static final int VALIDATE_AFTER_INACTIVITY_DEFAULT = 3000;
  private static final int EVICT_IDLE_CONNECTIONS_DEFAULT = 50000;
  private static final String VALIDATE_AFTER_INACTIVITY = "validateAfterInactivity";
  private static final String EVICT_IDLE_CONNECTIONS = "evictIdleConnections";

   // Maximum connections allowed per host
   public static final String PROP_MAX_CONNECTIONS_PER_HOST = "maxConnectionsPerHost";
   // Maximum total connections allowed
@@ -79,8 +89,6 @@ public class HttpClientUtil {
   public static final String PROP_USE_RETRY = "retry";
   // Allow compression (deflate,gzip) if server supports it
   public static final String PROP_ALLOW_COMPRESSION = "allowCompression";
  // Follow redirects
  public static final String PROP_FOLLOW_REDIRECTS = "followRedirects";
   // Basic auth username 
   public static final String PROP_BASIC_AUTH_USER = "httpBasicAuthUser";
   // Basic auth password 
@@ -88,25 +96,92 @@ public class HttpClientUtil {
   
   public static final String SYS_PROP_CHECK_PEER_NAME = "solr.ssl.checkPeerName";
   
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  // * NOTE* The following params configure the default request config and this
  // is overridden by SolrJ clients. Use the setters on the SolrJ clients to
  // to configure these settings if that is the intent.
  
  // Follow redirects
  public static final String PROP_FOLLOW_REDIRECTS = "followRedirects";
  
  // socket timeout measured in ms, closes a socket if read
  // takes longer than x ms to complete. throws
  // java.net.SocketTimeoutException: Read timed out exception
  public static final String PROP_SO_TIMEOUT = "socketTimeout";
  // connection timeout measures in ms, closes a socket if connection
  // cannot be established within x ms. with a
  // java.net.SocketTimeoutException: Connection timed out
  public static final String PROP_CONNECTION_TIMEOUT = "connTimeout";
   
   static final DefaultHttpRequestRetryHandler NO_RETRY = new DefaultHttpRequestRetryHandler(
       0, false);
 
  private static HttpClientConfigurer configurer = new HttpClientConfigurer();
  private static volatile SolrHttpClientBuilder httpClientBuilder;
  
  private static SolrHttpClientContextBuilder httpClientRequestContextBuilder = new SolrHttpClientContextBuilder();
  
  static {
    resetHttpClientBuilder();
  }
  
  public static abstract class SchemaRegistryProvider {
    public abstract Registry<ConnectionSocketFactory> getSchemaRegistry();
  }
  
  private static volatile SchemaRegistryProvider schemaRegistryProvider;
  private static volatile String cookiePolicy;
 
   private static final List<HttpRequestInterceptor> interceptors = Collections.synchronizedList(new ArrayList<HttpRequestInterceptor>());
   
  /**
   * Replace the {@link HttpClientConfigurer} class used in configuring the http
   * clients with a custom implementation.
   */
  public static void setConfigurer(HttpClientConfigurer newConfigurer) {
    configurer = newConfigurer;
  private static class DynamicInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
      interceptors.forEach(new Consumer<HttpRequestInterceptor>() {

        @Override
        public void accept(HttpRequestInterceptor interceptor) {
          try {
            interceptor.process(request, context);
          } catch (Exception e) {
            logger.error("", e);
          }
        }
      });

    }
   }
   
  public static HttpClientConfigurer getConfigurer() {
    return configurer;
  public static void setHttpClientBuilder(SolrHttpClientBuilder newHttpClientBuilder) {
    httpClientBuilder = newHttpClientBuilder;
  }
  
  public static void setHttpClientProvider(SolrHttpClientBuilder newHttpClientBuilder) {
    httpClientBuilder = newHttpClientBuilder;
  }

  public static void setSchemeRegistryProvider(SchemaRegistryProvider newRegistryProvider) {
    schemaRegistryProvider = newRegistryProvider;
  }
  
  public static SolrHttpClientBuilder getHttpClientBuilder() {
    return httpClientBuilder;
  }
  
  public static SchemaRegistryProvider getSchemaRegisteryProvider() {
    return schemaRegistryProvider;
  }
  
  public static void resetHttpClientBuilder() {
    schemaRegistryProvider = new SchemaRegistryProvider() {

      @Override
      public Registry<ConnectionSocketFactory> getSchemaRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory> create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
      }
    };
    httpClientBuilder = SolrHttpClientBuilder.create();

   }
   
   /**
@@ -116,195 +191,137 @@ public class HttpClientUtil {
    *          http client configuration, if null a client with default
    *          configuration (no additional configuration) is created. 
    */
  public static CloseableHttpClient createClient(final SolrParams params) {
    final ModifiableSolrParams config = new ModifiableSolrParams(params);
    if (logger.isDebugEnabled()) {
      logger.debug("Creating new http client, config:" + config);
  public static CloseableHttpClient createClient(SolrParams params) {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(schemaRegistryProvider.getSchemaRegistry());

    return createClient(params, cm);
  }
  
  public static CloseableHttpClient createClient(SolrParams params, PoolingHttpClientConnectionManager cm) {
    if (params == null) {
      params = new ModifiableSolrParams();
     }
    final DefaultHttpClient httpClient = HttpClientFactory.createHttpClient();
    configureClient(httpClient, config);
    return httpClient;
    
    return createClient(params, cm, false);
   }
   
   /**
    * Creates new http client by using the provided configuration.
    * 
    */
  public static CloseableHttpClient createClient(final SolrParams params, ClientConnectionManager cm) {
  public static CloseableHttpClient createClient(final SolrParams params, PoolingHttpClientConnectionManager cm, boolean sharedConnectionManager) {
     final ModifiableSolrParams config = new ModifiableSolrParams(params);
     if (logger.isDebugEnabled()) {
       logger.debug("Creating new http client, config:" + config);
     }
    final DefaultHttpClient httpClient = HttpClientFactory.createHttpClient(cm);
    configureClient(httpClient, config);
    return httpClient;
  }

  /**
   * Configures {@link DefaultHttpClient}, only sets parameters if they are
   * present in config.
   */
  public static void configureClient(final DefaultHttpClient httpClient,
      SolrParams config) {
    configurer.configure(httpClient,  config);
    synchronized(interceptors) {
      for(HttpRequestInterceptor interceptor: interceptors) {
        httpClient.addRequestInterceptor(interceptor);
      }
 
    if (params.get(PROP_SO_TIMEOUT) != null || params.get(PROP_CONNECTION_TIMEOUT) != null) {
      throw new SolrException(ErrorCode.SERVER_ERROR, "The socket connect and read timeout cannot be set here and must be set");
     }
  }
  
  public static void close(HttpClient httpClient) { 
    if (httpClient instanceof CloseableHttpClient) {
      org.apache.solr.common.util.IOUtils.closeQuietly((CloseableHttpClient) httpClient);
    
    cm.setMaxTotal(params.getInt(HttpClientUtil.PROP_MAX_CONNECTIONS, 10000));
    cm.setDefaultMaxPerRoute(params.getInt(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 10000));
    cm.setValidateAfterInactivity(Integer.getInteger(VALIDATE_AFTER_INACTIVITY, VALIDATE_AFTER_INACTIVITY_DEFAULT));

    
    HttpClientBuilder newHttpClientBuilder = HttpClientBuilder.create();

    if (sharedConnectionManager) {
      newHttpClientBuilder.setConnectionManagerShared(true);
     } else {
      httpClient.getConnectionManager().shutdown();
      newHttpClientBuilder.setConnectionManagerShared(false);
     }
  }
    
    ConnectionKeepAliveStrategy keepAliveStrat = new ConnectionKeepAliveStrategy() {
      @Override
      public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        // we only close connections based on idle time, not ttl expiration
        return -1;
      }
    };
 
  public static void addRequestInterceptor(HttpRequestInterceptor interceptor) {
    interceptors.add(interceptor);
  }
    if (httpClientBuilder.getAuthSchemeRegistryProvider() != null) {
      newHttpClientBuilder.setDefaultAuthSchemeRegistry(httpClientBuilder.getAuthSchemeRegistryProvider().getAuthSchemeRegistry());
    }
    if (httpClientBuilder.getCookieSpecRegistryProvider() != null) {
      newHttpClientBuilder.setDefaultCookieSpecRegistry(httpClientBuilder.getCookieSpecRegistryProvider().getCookieSpecRegistry());
    }
    if (httpClientBuilder.getCredentialsProviderProvider() != null) {
      newHttpClientBuilder.setDefaultCredentialsProvider(httpClientBuilder.getCredentialsProviderProvider().getCredentialsProvider());
    }
 
  public static void removeRequestInterceptor(HttpRequestInterceptor interceptor) {
    interceptors.remove(interceptor);
    newHttpClientBuilder.addInterceptorLast(new DynamicInterceptor());
    
    newHttpClientBuilder = newHttpClientBuilder.setKeepAliveStrategy(keepAliveStrat)
        .evictIdleConnections((long) Integer.getInteger(EVICT_IDLE_CONNECTIONS, EVICT_IDLE_CONNECTIONS_DEFAULT), TimeUnit.MILLISECONDS);
    
    HttpClientBuilder builder = setupBuilder(newHttpClientBuilder, params == null ? new ModifiableSolrParams() : params);
    
    HttpClient httpClient = builder.setConnectionManager(cm).build();
    
    assert ObjectReleaseTracker.track(httpClient);
    return (CloseableHttpClient) httpClient;
   }
  
  private static HttpClientBuilder setupBuilder(HttpClientBuilder builder, SolrParams config) {
   
    Builder requestConfigBuilder = RequestConfig.custom()
        .setRedirectsEnabled(config.getBool(HttpClientUtil.PROP_FOLLOW_REDIRECTS, false)).setDecompressionEnabled(false)
        .setConnectTimeout(config.getInt(HttpClientUtil.PROP_CONNECTION_TIMEOUT, DEFAULT_CONNECT_TIMEOUT))
        .setSocketTimeout(config.getInt(HttpClientUtil.PROP_SO_TIMEOUT, DEFAULT_SO_TIMEOUT));
 
  /**
   * Control HTTP payload compression.
   * 
   * @param allowCompression
   *          true will enable compression (needs support from server), false
   *          will disable compression.
   */
  public static void setAllowCompression(DefaultHttpClient httpClient,
      boolean allowCompression) {
    httpClient
        .removeRequestInterceptorByClass(UseCompressionRequestInterceptor.class);
    httpClient
        .removeResponseInterceptorByClass(UseCompressionResponseInterceptor.class);
    if (allowCompression) {
      httpClient.addRequestInterceptor(new UseCompressionRequestInterceptor());
      httpClient
          .addResponseInterceptor(new UseCompressionResponseInterceptor());
    String cpolicy = cookiePolicy;
    if (cpolicy != null) {
      requestConfigBuilder.setCookieSpec(cpolicy);
     }
  }
    
    RequestConfig requestConfig = requestConfigBuilder.build();
    
    HttpClientBuilder retBuilder = builder.setDefaultRequestConfig(requestConfig);

    if (config.getBool(HttpClientUtil.PROP_USE_RETRY, true)) {
      retBuilder = retBuilder.setRetryHandler(new SolrHttpRequestRetryHandler(3));
 
  /**
   * Set http basic auth information. If basicAuthUser or basicAuthPass is null
   * the basic auth configuration is cleared. Currently this is not preemtive
   * authentication. So it is not currently possible to do a post request while
   * using this setting.
   */
  public static void setBasicAuth(DefaultHttpClient httpClient,
      String basicAuthUser, String basicAuthPass) {
    if (basicAuthUser != null && basicAuthPass != null) {
      httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(basicAuthUser, basicAuthPass));
     } else {
      httpClient.getCredentialsProvider().clear();
      retBuilder = retBuilder.setRetryHandler(NO_RETRY);
     }
  }
 
  /**
   * Set max connections allowed per host. This call will only work when
   * {@link ThreadSafeClientConnManager} or
   * {@link PoolingClientConnectionManager} is used.
   */
  public static void setMaxConnectionsPerHost(HttpClient httpClient,
      int max) {
    // would have been nice if there was a common interface
    if (httpClient.getConnectionManager() instanceof ThreadSafeClientConnManager) {
      ThreadSafeClientConnManager mgr = (ThreadSafeClientConnManager)httpClient.getConnectionManager();
      mgr.setDefaultMaxPerRoute(max);
    } else if (httpClient.getConnectionManager() instanceof PoolingClientConnectionManager) {
      PoolingClientConnectionManager mgr = (PoolingClientConnectionManager)httpClient.getConnectionManager();
      mgr.setDefaultMaxPerRoute(max);
    final String basicAuthUser = config.get(HttpClientUtil.PROP_BASIC_AUTH_USER);
    final String basicAuthPass = config.get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    
    if (basicAuthUser != null && basicAuthPass != null) {
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(basicAuthUser, basicAuthPass));
      retBuilder.setDefaultCredentialsProvider(credsProvider);
     }
  }

  /**
   * Set max total connections allowed. This call will only work when
   * {@link ThreadSafeClientConnManager} or
   * {@link PoolingClientConnectionManager} is used.
   */
  public static void setMaxConnections(final HttpClient httpClient,
      int max) {
    // would have been nice if there was a common interface
    if (httpClient.getConnectionManager() instanceof ThreadSafeClientConnManager) {
      ThreadSafeClientConnManager mgr = (ThreadSafeClientConnManager)httpClient.getConnectionManager();
      mgr.setMaxTotal(max);
    } else if (httpClient.getConnectionManager() instanceof PoolingClientConnectionManager) {
      PoolingClientConnectionManager mgr = (PoolingClientConnectionManager)httpClient.getConnectionManager();
      mgr.setMaxTotal(max);
    
    if (config.getBool(HttpClientUtil.PROP_ALLOW_COMPRESSION, false)) {
      retBuilder.addInterceptorFirst(new UseCompressionRequestInterceptor());
      retBuilder.addInterceptorFirst(new UseCompressionResponseInterceptor());
    } else {
      retBuilder.disableContentCompression();
     }
  }
  
 
  /**
   * Defines the socket timeout (SO_TIMEOUT) in milliseconds. A timeout value of
   * zero is interpreted as an infinite timeout.
   * 
   * @param timeout timeout in milliseconds
   */
  public static void setSoTimeout(HttpClient httpClient, int timeout) {
    HttpConnectionParams.setSoTimeout(httpClient.getParams(),
        timeout);
    return retBuilder;
   }
 
  /**
   * Control retry handler 
   * @param useRetry when false the client will not try to retry failed requests.
   */
  public static void setUseRetry(final DefaultHttpClient httpClient,
      boolean useRetry) {
    if (!useRetry) {
      httpClient.setHttpRequestRetryHandler(NO_RETRY);
    } else {
      // if the request is not fully sent, we retry
      // streaming updates are not a problem, because they are not retryable
      httpClient.setHttpRequestRetryHandler(new SolrHttpRequestRetryHandler(3));
    }
  }
  public static void close(HttpClient httpClient) { 
 
  /**
   * Set connection timeout. A timeout value of zero is interpreted as an
   * infinite timeout.
   * 
   * @param timeout
   *          connection Timeout in milliseconds
   */
  public static void setConnectionTimeout(final HttpClient httpClient,
      int timeout) {
      HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),
          timeout);
  }
    org.apache.solr.common.util.IOUtils.closeQuietly((CloseableHttpClient) httpClient);
 
  /**
   * Set follow redirects.
   *
   * @param followRedirects  When true the client will follow redirects.
   */
  public static void setFollowRedirects(HttpClient httpClient,
      boolean followRedirects) {
    new ClientParamBean(httpClient.getParams()).setHandleRedirects(followRedirects);
    assert ObjectReleaseTracker.release(httpClient);
   }
 
  public static void setHostNameVerifier(DefaultHttpClient httpClient,
      X509HostnameVerifier hostNameVerifier) {
    Scheme httpsScheme = httpClient.getConnectionManager().getSchemeRegistry().get("https");
    if (httpsScheme != null) {
      SSLSocketFactory sslSocketFactory = (SSLSocketFactory) httpsScheme.getSchemeSocketFactory();
      sslSocketFactory.setHostnameVerifier(hostNameVerifier);
    }
  public static void addRequestInterceptor(HttpRequestInterceptor interceptor) {
    interceptors.add(interceptor);
   }
  
  public static void setStaleCheckingEnabled(final HttpClient httpClient, boolean enabled) {
    HttpConnectionParams.setStaleCheckingEnabled(httpClient.getParams(), enabled);

  public static void removeRequestInterceptor(HttpRequestInterceptor interceptor) {
    interceptors.remove(interceptor);
   }
   
  public static void setTcpNoDelay(final HttpClient httpClient, boolean tcpNoDelay) {
    HttpConnectionParams.setTcpNoDelay(httpClient.getParams(), tcpNoDelay);
  public static void clearRequestInterceptors() {
    interceptors.clear();
   }
   
   private static class UseCompressionRequestInterceptor implements
@@ -374,35 +391,34 @@ public class HttpClientUtil {
     }
   }
 
  public static class HttpClientFactory {
    private static Class<? extends DefaultHttpClient> defaultHttpClientClass = DefaultHttpClient.class;
    private static Class<? extends SystemDefaultHttpClient> systemDefaultHttpClientClass = SystemDefaultHttpClient.class;

  public static void setHttpClientRequestContextBuilder(SolrHttpClientContextBuilder httpClientContextBuilder) {
    httpClientRequestContextBuilder = httpClientContextBuilder;
  }
 
    public static SystemDefaultHttpClient createHttpClient() {
      Constructor<? extends SystemDefaultHttpClient> constructor;
      try {
        constructor = systemDefaultHttpClientClass.getDeclaredConstructor();
        return constructor.newInstance();
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new SolrException(ErrorCode.SERVER_ERROR, "Unable to create HttpClient instance. ", e);
      }
    }
  /**
   * 
   */
  public static HttpClientContext createNewHttpClientRequestContext() {
    return httpClientRequestContextBuilder.createContext();
  }
  
  public static Builder createDefaultRequestConfigBuilder() {
    String cpolicy = cookiePolicy;
    Builder builder = RequestConfig.custom();
 
    public static DefaultHttpClient createHttpClient(ClientConnectionManager cm) {
      Constructor<? extends DefaultHttpClient> constructor;
      try {
        constructor = defaultHttpClientClass.getDeclaredConstructor(new Class[]{ClientConnectionManager.class});
        return constructor.newInstance(new Object[]{cm});
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new SolrException(ErrorCode.SERVER_ERROR, "Unable to create HttpClient instance, registered class is: " + defaultHttpClientClass, e);
      }
    builder.setSocketTimeout(DEFAULT_SO_TIMEOUT)
        .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
        .setRedirectsEnabled(false)
        .setDecompressionEnabled(false); // we do our own compression / decompression
    if (cpolicy != null) {
      builder.setCookieSpec(cpolicy);
     }
    return builder;
  }
 
    public static void setHttpClientImpl(Class<? extends DefaultHttpClient> defaultHttpClient, Class<? extends SystemDefaultHttpClient> systemDefaultHttpClient) {
      defaultHttpClientClass = defaultHttpClient;
      systemDefaultHttpClientClass = systemDefaultHttpClient;
    }
  public static void setCookiePolicy(String policyName) {
    cookiePolicy = policyName;
   }
 

 }
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpSolrClient.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpSolrClient.java
index fe445970561..29a56a65194 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpSolrClient.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpSolrClient.java
@@ -16,6 +16,24 @@
  */
 package org.apache.solr.client.solrj.impl;
 
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

 import org.apache.commons.io.IOUtils;
 import org.apache.http.Header;
 import org.apache.http.HttpEntity;
@@ -23,6 +41,7 @@ import org.apache.http.HttpResponse;
 import org.apache.http.HttpStatus;
 import org.apache.http.NameValuePair;
 import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig.Builder;
 import org.apache.http.client.entity.UrlEncodedFormEntity;
 import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
 import org.apache.http.client.methods.HttpGet;
@@ -30,15 +49,15 @@ import org.apache.http.client.methods.HttpPost;
 import org.apache.http.client.methods.HttpPut;
 import org.apache.http.client.methods.HttpRequestBase;
 import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.HttpClientConnectionManager;
 import org.apache.http.entity.ContentType;
 import org.apache.http.entity.InputStreamEntity;
 import org.apache.http.entity.mime.FormBodyPart;
 import org.apache.http.entity.mime.HttpMultipartMode;
 import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
 import org.apache.http.entity.mime.content.InputStreamBody;
 import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.http.message.BasicHeader;
 import org.apache.http.message.BasicNameValuePair;
 import org.apache.http.util.EntityUtils;
@@ -61,24 +80,6 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.slf4j.MDC;
 
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

 /**
  * A SolrClient implementation that talks directly to a Solr server via HTTP
  *
@@ -145,13 +146,15 @@ public class HttpSolrClient extends SolrClient {
   
   private final HttpClient httpClient;
   
  private volatile boolean followRedirects = false;
  private volatile Boolean followRedirects = false;
   
   private volatile boolean useMultiPartPost;
   private final boolean internalClient;
 
   private volatile Set<String> queryParams = Collections.emptySet();

  private volatile Integer connectionTimeout;
  private volatile Integer soTimeout;
  
   /**
    * @param baseURL
    *          The URL of the Solr server. For example, "
@@ -166,7 +169,12 @@ public class HttpSolrClient extends SolrClient {
     this(baseURL, client, new BinaryResponseParser());
   }
   
  
   public HttpSolrClient(String baseURL, HttpClient client, ResponseParser parser) {
    this(baseURL, client, parser, false);
  }
  
  public HttpSolrClient(String baseURL, HttpClient client, ResponseParser parser, boolean allowCompression) {
     this.baseUrl = baseURL;
     if (baseUrl.endsWith("/")) {
       baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
@@ -183,9 +191,8 @@ public class HttpSolrClient extends SolrClient {
     } else {
       internalClient = true;
       ModifiableSolrParams params = new ModifiableSolrParams();
      params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, 128);
      params.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 32);
       params.set(HttpClientUtil.PROP_FOLLOW_REDIRECTS, followRedirects);
      params.set(HttpClientUtil.PROP_ALLOW_COMPRESSION, allowCompression);
       httpClient = HttpClientUtil.createClient(params);
     }
     
@@ -344,6 +351,7 @@ public class HttpSolrClient extends SolrClient {
       if (streams != null) {
         throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "GET can't send streams!");
       }

       return new HttpGet(basePath + path + wparams.toQueryString());
     }
 
@@ -370,6 +378,7 @@ public class HttpSolrClient extends SolrClient {
         String fullQueryUrl = url + queryParams.toQueryString();
         HttpEntityEnclosingRequestBase postOrPut = SolrRequest.METHOD.POST == request.getMethod() ?
             new HttpPost(fullQueryUrl) : new HttpPut(fullQueryUrl);

         if (!isMultipart) {
           postOrPut.addHeader("Content-Type",
               "application/x-www-form-urlencoded; charset=UTF-8");
@@ -391,6 +400,7 @@ public class HttpSolrClient extends SolrClient {
           }
         }
 
        // TODO: remove deprecated - first simple attempt failed, see {@link MultipartEntityBuilder}
         if (isMultipart && streams != null) {
           for (ContentStream content : streams) {
             String contentType = content.getContentType();
@@ -473,13 +483,26 @@ public class HttpSolrClient extends SolrClient {
   
   protected NamedList<Object> executeMethod(HttpRequestBase method, final ResponseParser processor) throws SolrServerException {
     method.addHeader("User-Agent", AGENT);
 
    Builder requestConfigBuilder = HttpClientUtil.createDefaultRequestConfigBuilder();
    if (soTimeout != null) {
      requestConfigBuilder.setSocketTimeout(soTimeout);
    }
    if (connectionTimeout != null) {
      requestConfigBuilder.setConnectTimeout(connectionTimeout);
    }
    if (followRedirects != null) {
      requestConfigBuilder.setRedirectsEnabled(followRedirects);
    }

    method.setConfig(requestConfigBuilder.build());
     
     HttpEntity entity = null;
     InputStream respBody = null;
     boolean shouldClose = true;
     try {
       // Execute the method.
      final HttpResponse response = httpClient.execute(method);
      final HttpResponse response = httpClient.execute(method, HttpClientUtil.createNewHttpClientRequestContext());
       int httpStatus = response.getStatusLine().getStatusCode();
       
       // Read the contents
@@ -647,7 +670,7 @@ public class HttpSolrClient extends SolrClient {
    *          Timeout in milliseconds
    **/
   public void setConnectionTimeout(int timeout) {
    HttpClientUtil.setConnectionTimeout(httpClient, timeout);
    this.connectionTimeout = timeout;
   }
   
   /**
@@ -658,7 +681,7 @@ public class HttpSolrClient extends SolrClient {
    *          Timeout in milliseconds
    **/
   public void setSoTimeout(int timeout) {
    HttpClientUtil.setSoTimeout(httpClient, timeout);
    this.soTimeout = timeout;
   }
   
   /**
@@ -671,22 +694,6 @@ public class HttpSolrClient extends SolrClient {
    */
   public void setFollowRedirects(boolean followRedirects) {
     this.followRedirects = followRedirects;
    HttpClientUtil.setFollowRedirects(httpClient,  followRedirects);
  }
  
  /**
   * Allow server-&gt;client communication to be compressed. Currently gzip and
   * deflate are supported. If the server supports compression the response will
   * be compressed. This method is only allowed if the http client is of type
   * DefatulHttpClient.
   */
  public void setAllowCompression(boolean allowCompression) {
    if (httpClient instanceof DefaultHttpClient) {
      HttpClientUtil.setAllowCompression((DefaultHttpClient) httpClient, allowCompression);
    } else {
      throw new UnsupportedOperationException(
          "HttpClient instance was not of type DefaultHttpClient");
    }
   }
   
   public void setRequestWriter(RequestWriter requestWriter) {
@@ -694,7 +701,7 @@ public class HttpSolrClient extends SolrClient {
   }
   
   /**
   * Close the {@link ClientConnectionManager} from the internal client.
   * Close the {@link HttpClientConnectionManager} from the internal client.
    */
   @Override
   public void close() throws IOException {
@@ -702,33 +709,6 @@ public class HttpSolrClient extends SolrClient {
       HttpClientUtil.close(httpClient);
     }
   }

  /**
   * Set the maximum number of connections that can be open to a single host at
   * any given time. If http client was created outside the operation is not
   * allowed.
   */
  public void setDefaultMaxConnectionsPerHost(int max) {
    if (internalClient) {
      HttpClientUtil.setMaxConnectionsPerHost(httpClient, max);
    } else {
      throw new UnsupportedOperationException(
          "Client was created outside of HttpSolrServer");
    }
  }
  
  /**
   * Set the maximum number of connections that can be open at any given time.
   * If http client was created outside the operation is not allowed.
   */
  public void setMaxTotalConnections(int max) {
    if (internalClient) {
      HttpClientUtil.setMaxConnections(httpClient, max);
    } else {
      throw new UnsupportedOperationException(
          "Client was created outside of HttpSolrServer");
    }
  }
   
   public boolean isUseMultiPartPost() {
     return useMultiPartPost;
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/Krb5HttpClientConfigurer.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/Krb5HttpClientBuilder.java
similarity index 67%
rename from solr/solrj/src/java/org/apache/solr/client/solrj/impl/Krb5HttpClientConfigurer.java
rename to solr/solrj/src/java/org/apache/solr/client/solrj/impl/Krb5HttpClientBuilder.java
index 8b2705bdc5f..6c7c64f2cd1 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/Krb5HttpClientConfigurer.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/Krb5HttpClientBuilder.java
@@ -32,32 +32,47 @@ import org.apache.http.HttpEntityEnclosingRequest;
 import org.apache.http.HttpException;
 import org.apache.http.HttpRequest;
 import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthSchemeProvider;
 import org.apache.http.auth.AuthScope;
 import org.apache.http.auth.Credentials;
import org.apache.http.auth.AuthSchemeRegistry;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.BufferedHttpEntity;
 import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
 import org.apache.http.protocol.HttpContext;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.params.ClientPNames;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder.AuthSchemeRegistryProvider;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder.CookieSpecRegistryProvider;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder.CredentialsProviderProvider;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
import org.apache.http.entity.BufferedHttpEntity;
 
 /**
 * Kerberos-enabled HttpClientConfigurer
 * Kerberos-enabled SolrHttpClientBuilder
  */
public class Krb5HttpClientConfigurer extends HttpClientConfigurer {
public class Krb5HttpClientBuilder  {
   
   public static final String LOGIN_CONFIG_PROP = "java.security.auth.login.config";
   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   
   private static final Configuration jaasConfig = new SolrJaasConfiguration();
 
  public void configure(DefaultHttpClient httpClient, SolrParams config) {
    super.configure(httpClient, config);
  public Krb5HttpClientBuilder() {
 
  }
  
  public SolrHttpClientBuilder getBuilder() {
    return getBuilder(HttpClientUtil.getHttpClientBuilder());
  }
  
  public void close() {
    HttpClientUtil.removeRequestInterceptor(bufferedEntityInterceptor);
  }
  
  public SolrHttpClientBuilder getBuilder(SolrHttpClientBuilder builder) {
     if (System.getProperty(LOGIN_CONFIG_PROP) != null) {
       String configValue = System.getProperty(LOGIN_CONFIG_PROP);
 
@@ -80,9 +95,16 @@ public class Krb5HttpClientConfigurer extends HttpClientConfigurer {
 
         javax.security.auth.login.Configuration.setConfiguration(jaasConfig);
         //Enable only SPNEGO authentication scheme.
        AuthSchemeRegistry registry = new AuthSchemeRegistry();
        registry.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true, false));
        httpClient.setAuthSchemes(registry);

        builder.setAuthSchemeRegistryProvider(new AuthSchemeRegistryProvider() {
          @Override
          public Lookup<AuthSchemeProvider> getAuthSchemeRegistry() {
            Lookup<AuthSchemeProvider> authProviders = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true, false))                
                .build();
            return authProviders;
          }
        });
         // Get the credentials from the JAAS configuration rather than here
         Credentials useJaasCreds = new Credentials() {
           public String getPassword() {
@@ -92,18 +114,35 @@ public class Krb5HttpClientConfigurer extends HttpClientConfigurer {
             return null;
           }
         };

        SolrPortAwareCookieSpecFactory cookieFactory = new SolrPortAwareCookieSpecFactory();
        httpClient.getCookieSpecs().register(cookieFactory.POLICY_NAME, cookieFactory);
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, cookieFactory.POLICY_NAME);
         
        httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, useJaasCreds);
        HttpClientUtil.setCookiePolicy(SolrPortAwareCookieSpecFactory.POLICY_NAME);
        
        builder.setCookieSpecRegistryProvider(new CookieSpecRegistryProvider() {
          @Override
          public Lookup<CookieSpecProvider> getCookieSpecRegistry() {
            SolrPortAwareCookieSpecFactory cookieFactory = new SolrPortAwareCookieSpecFactory();
 
        httpClient.addRequestInterceptor(bufferedEntityInterceptor);
      } else {
        httpClient.getCredentialsProvider().clear();
            Lookup<CookieSpecProvider> cookieRegistry = RegistryBuilder.<CookieSpecProvider> create()
                .register(SolrPortAwareCookieSpecFactory.POLICY_NAME, cookieFactory).build();

            return cookieRegistry;
          }
        });
        
        builder.setDefaultCredentialsProvider(new CredentialsProviderProvider() {
          
          @Override
          public CredentialsProvider getCredentialsProvider() {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, useJaasCreds);
            return credentialsProvider;
          }
        });
        HttpClientUtil.addRequestInterceptor(bufferedEntityInterceptor);
       }
     }
    
    return builder;
   }
 
   // Set a buffered entity based request interceptor
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/LBHttpSolrClient.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/LBHttpSolrClient.java
index 6d4711e8faf..fabb933d104 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/LBHttpSolrClient.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/LBHttpSolrClient.java
@@ -109,6 +109,9 @@ public class LBHttpSolrClient extends SolrClient {
   private volatile RequestWriter requestWriter;
 
   private Set<String> queryParams = new HashSet<>();
  private Integer connectionTimeout;

  private Integer soTimeout;
 
   static {
     solrQuery.setRows(0);
@@ -261,6 +264,12 @@ public class LBHttpSolrClient extends SolrClient {
 
   protected HttpSolrClient makeSolrClient(String server) {
     HttpSolrClient client = new HttpSolrClient(server, httpClient, parser);
    if (connectionTimeout != null) {
      client.setConnectionTimeout(connectionTimeout);
    }
    if (soTimeout != null) {
      client.setSoTimeout(soTimeout);
    }
     if (requestWriter != null) {
       client.setRequestWriter(requestWriter);
     }
@@ -459,7 +468,17 @@ public class LBHttpSolrClient extends SolrClient {
   }
 
   public void setConnectionTimeout(int timeout) {
    HttpClientUtil.setConnectionTimeout(httpClient, timeout);
    this.connectionTimeout = timeout;
    synchronized (aliveServers) {
      Iterator<ServerWrapper> wrappersIt = aliveServers.values().iterator();
      while (wrappersIt.hasNext()) {
        wrappersIt.next().client.setConnectionTimeout(timeout);
      }
    }
    Iterator<ServerWrapper> wrappersIt = zombieServers.values().iterator();
    while (wrappersIt.hasNext()) {
      wrappersIt.next().client.setConnectionTimeout(timeout);
    }
   }
 
   /**
@@ -467,7 +486,17 @@ public class LBHttpSolrClient extends SolrClient {
    * not for indexing.
    */
   public void setSoTimeout(int timeout) {
    HttpClientUtil.setSoTimeout(httpClient, timeout);
    this.soTimeout = timeout;
    synchronized (aliveServers) {
      Iterator<ServerWrapper> wrappersIt = aliveServers.values().iterator();
      while (wrappersIt.hasNext()) {
        wrappersIt.next().client.setSoTimeout(timeout);
      }
    }
    Iterator<ServerWrapper> wrappersIt = zombieServers.values().iterator();
    while (wrappersIt.hasNext()) {
      wrappersIt.next().client.setSoTimeout(timeout);
    }
   }
 
   @Override
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientBuilder.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientBuilder.java
new file mode 100644
index 00000000000..98217f81595
-- /dev/null
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientBuilder.java
@@ -0,0 +1,91 @@
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
package org.apache.solr.client.solrj.impl;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.config.Lookup;
import org.apache.http.cookie.CookieSpecProvider;

/**
 * Builder class for configuring internal HttpClients. This
 * relies on the internal HttpClient implementation and is subject to
 * change.
 * 
 * @lucene.experimental
 */
public class SolrHttpClientBuilder {
  public static SolrHttpClientBuilder create() {
    return new SolrHttpClientBuilder();
  }
  
  public interface HttpRequestInterceptorProvider {
    public HttpRequestInterceptor getHttpRequestInterceptor();
  }
  
  public interface CredentialsProviderProvider {
    public CredentialsProvider getCredentialsProvider();
  }
  
  public interface AuthSchemeRegistryProvider {
    public Lookup<AuthSchemeProvider> getAuthSchemeRegistry();
  }
  
  public interface CookieSpecRegistryProvider {
    public Lookup<CookieSpecProvider> getCookieSpecRegistry();
  }
  
  private CookieSpecRegistryProvider cookieSpecRegistryProvider;
  private AuthSchemeRegistryProvider authSchemeRegistryProvider;
  private CredentialsProviderProvider credentialsProviderProvider;

  protected SolrHttpClientBuilder() {
    super();
  }

  public final SolrHttpClientBuilder setCookieSpecRegistryProvider(
      final CookieSpecRegistryProvider cookieSpecRegistryProvider) {
    this.cookieSpecRegistryProvider = cookieSpecRegistryProvider;
    return this;
  }
  
  public final SolrHttpClientBuilder setDefaultCredentialsProvider(
      final CredentialsProviderProvider credentialsProviderProvider) {
    this.credentialsProviderProvider = credentialsProviderProvider;
    return this;
  }
  
  public final SolrHttpClientBuilder setAuthSchemeRegistryProvider(
      final AuthSchemeRegistryProvider authSchemeRegistryProvider) {
    this.authSchemeRegistryProvider = authSchemeRegistryProvider;
    return this;
  }

  public AuthSchemeRegistryProvider getAuthSchemeRegistryProvider() {
    return authSchemeRegistryProvider;
  }

  public CookieSpecRegistryProvider getCookieSpecRegistryProvider() {
    return cookieSpecRegistryProvider;
  }

  public CredentialsProviderProvider getCredentialsProviderProvider() {
    return credentialsProviderProvider;
  }

}
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientContextBuilder.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientContextBuilder.java
new file mode 100644
index 00000000000..b678df7ea19
-- /dev/null
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/SolrHttpClientContextBuilder.java
@@ -0,0 +1,96 @@
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
package org.apache.solr.client.solrj.impl;

import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.cookie.CookieSpecProvider;

public class SolrHttpClientContextBuilder {
  public static SolrHttpClientContextBuilder create() {
    return new SolrHttpClientContextBuilder();
  }
  
  public static abstract class CredentialsProviderProvider {
    public abstract CredentialsProvider getCredentialsProvider();
  }
  
  public static abstract class AuthSchemeRegistryProvider {
    public abstract Lookup<AuthSchemeProvider> getAuthSchemeRegistry();
  }
  
  public static abstract class CookieSpecRegistryProvider {
    public abstract Lookup<CookieSpecProvider> getCookieSpecRegistry();
  }
  
  private CookieSpecRegistryProvider cookieSpecRegistryProvider;
  private AuthSchemeRegistryProvider authSchemeRegistryProvider;
  private CredentialsProviderProvider credentialsProviderProvider;

  public SolrHttpClientContextBuilder() {
    super();
  }

  public final SolrHttpClientContextBuilder setCookieSpecRegistryProvider(
      final CookieSpecRegistryProvider cookieSpecRegistryProvider) {
    this.cookieSpecRegistryProvider = cookieSpecRegistryProvider;
    return this;
  }
  
  public final SolrHttpClientContextBuilder setDefaultCredentialsProvider(
      final CredentialsProviderProvider credentialsProviderProvider) {
    this.credentialsProviderProvider = credentialsProviderProvider;
    return this;
  }
  
  public final SolrHttpClientContextBuilder setAuthSchemeRegistryProvider(
      final AuthSchemeRegistryProvider authSchemeRegistryProvider) {
    this.authSchemeRegistryProvider = authSchemeRegistryProvider;
    return this;
  }

  public AuthSchemeRegistryProvider getAuthSchemeRegistryProvider() {
    return authSchemeRegistryProvider;
  }

  public CookieSpecRegistryProvider getCookieSpecRegistryProvider() {
    return cookieSpecRegistryProvider;
  }

  public CredentialsProviderProvider getCredentialsProviderProvider() {
    return credentialsProviderProvider;
  }
  
  public HttpClientContext createContext() {
    HttpClientContext context = new HttpClientContext();
    if (getCredentialsProviderProvider() != null) {
      context.setCredentialsProvider(getCredentialsProviderProvider().getCredentialsProvider());
    }
    if (getAuthSchemeRegistryProvider() != null) {
      context.setAuthSchemeRegistry( getAuthSchemeRegistryProvider().getAuthSchemeRegistry());
    }
    
    if (getCookieSpecRegistryProvider() != null) {
      context.setCookieSpecRegistry(getCookieSpecRegistryProvider().getCookieSpecRegistry());
    }
    
    return context;
  }

}
diff --git a/solr/solrj/src/test-files/log4j.properties b/solr/solrj/src/test-files/log4j.properties
index 9355270fa82..dae4f6f4181 100644
-- a/solr/solrj/src/test-files/log4j.properties
++ b/solr/solrj/src/test-files/log4j.properties
@@ -28,3 +28,5 @@ log4j.logger.org.apache.solr.hadoop=INFO
 
 #log4j.logger.org.apache.solr.common.cloud.ClusterStateUtil=DEBUG
 #log4j.logger.org.apache.solr.cloud.OverseerAutoReplicaFailoverThread=DEBUG

# log4j.logger.org.apache.http.impl.conn.PoolingHttpClientConnectionManager=DEBUG
\ No newline at end of file
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleBinaryTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleBinaryTest.java
index 04807fe2e75..1d70cd9e605 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleBinaryTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleBinaryTest.java
@@ -42,8 +42,6 @@ public class SolrExampleBinaryTest extends SolrExampleTests {
       String url = jetty.getBaseUrl().toString() + "/collection1";
       HttpSolrClient client = new HttpSolrClient( url );
       client.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       client.setUseMultiPartPost(random().nextBoolean());
 
       // where the magic happens
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleXMLTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleXMLTest.java
index f1b77e4e38d..338e449af18 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleXMLTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleXMLTest.java
@@ -40,8 +40,6 @@ public class SolrExampleXMLTest extends SolrExampleTests {
       HttpSolrClient client = new HttpSolrClient(url);
       client.setUseMultiPartPost(random().nextBoolean());
       client.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       client.setParser(new XMLResponseParser());
       client.setRequestWriter(new RequestWriter());
       return client;
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExceptionTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExceptionTest.java
index 75a030d8fb7..9fbaffbdc83 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExceptionTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExceptionTest.java
@@ -38,10 +38,11 @@ public class SolrExceptionTest extends LuceneTestCase {
       // switched to a local address to avoid going out on the net, ns lookup issues, etc.
       // set a 1ms timeout to let the connection fail faster.
       httpClient = HttpClientUtil.createClient(null);
      HttpClientUtil.setConnectionTimeout(httpClient,  1);
      SolrClient client = new HttpSolrClient("http://[ff01::114]:11235/solr/", httpClient);
      SolrQuery query = new SolrQuery("test123");
      client.query(query);
      try (HttpSolrClient client = new HttpSolrClient("http://[ff01::114]:11235/solr/", httpClient)) {
        client.setConnectionTimeout(1);
        SolrQuery query = new SolrQuery("test123");
        client.query(query);
      }
       httpClient.close();
     } catch (SolrServerException sse) {
       gotExpectedError = true;
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java
index dbef180a68e..317d4cd7c05 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java
@@ -23,9 +23,11 @@ import org.apache.http.client.methods.HttpPost;
 import org.apache.http.entity.InputStreamEntity;
 import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
 import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.Utils;
 import org.apache.solr.util.ExternalPaths;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -79,7 +81,8 @@ public class SolrSchemalessExampleTest extends SolrExampleTestsBase {
     HttpPost post = new HttpPost(client.getBaseURL() + "/update/json/docs");
     post.setHeader("Content-Type", "application/json");
     post.setEntity(new InputStreamEntity(new ByteArrayInputStream(json.getBytes("UTF-8")), -1));
    HttpResponse response = httpClient.execute(post);
    HttpResponse response = httpClient.execute(post, HttpClientUtil.createNewHttpClientRequestContext());
    Utils.consumeFully(response.getEntity());
     assertEquals(200, response.getStatusLine().getStatusCode());
     client.commit();
     assertNumFound("*:*", 2);
@@ -133,8 +136,6 @@ public class SolrSchemalessExampleTest extends SolrExampleTestsBase {
       String url = jetty.getBaseUrl().toString() + "/collection1";
       HttpSolrClient client = new HttpSolrClient(url);
       client.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       client.setUseMultiPartPost(random().nextBoolean());
       
       if (random().nextBoolean()) {
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrClient.java b/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrClient.java
index 5bebd644617..70cd77dc053 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrClient.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrClient.java
@@ -33,7 +33,6 @@ import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.client.solrj.response.SolrResponseBase;
 import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.util.TimeOut;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
@@ -92,7 +91,7 @@ public class TestLBHttpSolrClient extends SolrTestCaseJ4 {
   public void setUp() throws Exception {
     super.setUp();
     httpClient = HttpClientUtil.createClient(null);
    HttpClientUtil.setConnectionTimeout(httpClient,  1000);

     for (int i = 0; i < solr.length; i++) {
       solr[i] = new SolrInstance("solr/collection1" + i, createTempDir("instance-" + i).toFile(), 0);
       solr[i].setUp();
@@ -125,7 +124,7 @@ public class TestLBHttpSolrClient extends SolrTestCaseJ4 {
         aSolr.tearDown();
       }
     }
    httpClient.close();
    HttpClientUtil.close(httpClient);
     super.tearDown();
   }
 
@@ -204,12 +203,12 @@ public class TestLBHttpSolrClient extends SolrTestCaseJ4 {
     for (int i = 0; i < solr.length; i++) {
       s[i] = solr[i].getUrl();
     }
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, 250);
    params.set(HttpClientUtil.PROP_SO_TIMEOUT, 250);
    CloseableHttpClient myHttpClient = HttpClientUtil.createClient(params);

    CloseableHttpClient myHttpClient = HttpClientUtil.createClient(null);
     try {
       LBHttpSolrClient client = new LBHttpSolrClient(myHttpClient, s);
      client.setConnectionTimeout(250);
      client.setSoTimeout(250);
       client.setAliveCheckInterval(500);
   
       // Kill a server and test again
@@ -225,7 +224,7 @@ public class TestLBHttpSolrClient extends SolrTestCaseJ4 {
       // Wait for the alive check to complete
       waitForServer(30, client, 3, "solr1");
     } finally {
      myHttpClient.close();
      HttpClientUtil.close(myHttpClient);
     }
   }
   
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java
index fa22f80dcd2..467b5705b81 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java
@@ -31,6 +31,7 @@ import org.apache.http.client.methods.HttpRequestBase;
 import org.apache.http.impl.client.HttpClients;
 import org.apache.solr.SolrJettyTestBase;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.util.ExternalPaths;
 import org.eclipse.jetty.server.Connector;
 import org.eclipse.jetty.server.HttpConnectionFactory;
@@ -108,7 +109,7 @@ public class JettyWebappTest extends SolrTestCaseJ4
 
     HttpClient client = HttpClients.createDefault();
     HttpRequestBase m = new HttpGet(adminPath);
    HttpResponse response = client.execute(m);
    HttpResponse response = client.execute(m, HttpClientUtil.createNewHttpClientRequestContext());
     assertEquals(200, response.getStatusLine().getStatusCode());
     Header header = response.getFirstHeader("X-Frame-Options");
     assertEquals("DENY", header.getValue().toUpperCase(Locale.ROOT));
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/SolrExampleJettyTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/SolrExampleJettyTest.java
index 035619698eb..d8347cec3c7 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/SolrExampleJettyTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/SolrExampleJettyTest.java
@@ -23,6 +23,7 @@ import org.apache.http.entity.InputStreamEntity;
 import org.apache.solr.SolrTestCaseJ4.SuppressSSL;
 import org.apache.solr.client.solrj.SolrExampleTests;
 import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.SolrDocument;
@@ -75,7 +76,7 @@ public class SolrExampleJettyTest extends SolrExampleTests {
     HttpPost post = new HttpPost(client.getBaseURL() + "/update/json/docs");
     post.setHeader("Content-Type", "application/json");
     post.setEntity(new InputStreamEntity(new ByteArrayInputStream(json.getBytes("UTF-8")), -1));
    HttpResponse response = httpClient.execute(post);
    HttpResponse response = httpClient.execute(post, HttpClientUtil.createNewHttpClientRequestContext());
     assertEquals(200, response.getStatusLine().getStatusCode());
     client.commit();
     QueryResponse rsp = getSolrClient().query(new SolrQuery("*:*"));
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/BasicHttpSolrClientTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/BasicHttpSolrClientTest.java
index 2c7ac9d9652..ad20e956632 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/BasicHttpSolrClientTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/BasicHttpSolrClientTest.java
@@ -16,15 +16,11 @@
  */
 package org.apache.solr.client.solrj.impl;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.invoke.MethodHandles;
 import java.net.URISyntaxException;
import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Enumeration;
@@ -35,6 +31,11 @@ import java.util.Map;
 import java.util.Set;
 import java.util.TreeSet;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 import org.apache.http.Header;
 import org.apache.http.HttpEntity;
 import org.apache.http.HttpException;
@@ -43,15 +44,14 @@ import org.apache.http.HttpRequestInterceptor;
 import org.apache.http.HttpResponse;
 import org.apache.http.ParseException;
 import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
 import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
 import org.apache.http.client.utils.URIBuilder;
 import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecRegistry;
 import org.apache.http.impl.client.BasicCookieStore;
 import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.RequestWrapper;
 import org.apache.http.impl.cookie.BasicClientCookie;
 import org.apache.http.protocol.HttpContext;
 import org.apache.solr.SolrJettyTestBase;
@@ -68,6 +68,7 @@ import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SuppressForbidden;
 import org.apache.solr.util.SSLTestConfig;
@@ -524,49 +525,60 @@ public class BasicHttpSolrClientTest extends SolrJettyTestBase {
   
   @Test
   public void testCompression() throws Exception {
    SolrQuery q = new SolrQuery("*:*");
    
     try (HttpSolrClient client = new HttpSolrClient(jetty.getBaseUrl().toString() + "/debug/foo")) {
      SolrQuery q = new SolrQuery("*:*");
      
       // verify request header gets set
       DebugServlet.clear();
       try {
         client.query(q);
       } catch (ParseException ignored) {}
      assertNull(DebugServlet.headers.get("Accept-Encoding"));
      client.setAllowCompression(true);
      assertNull(DebugServlet.headers.toString(), DebugServlet.headers.get("Accept-Encoding")); 
    }
    
    try (HttpSolrClient client = new HttpSolrClient(jetty.getBaseUrl().toString() + "/debug/foo", null, null, true)) {
       try {
         client.query(q);
       } catch (ParseException ignored) {}
       assertNotNull(DebugServlet.headers.get("Accept-Encoding"));
      client.setAllowCompression(false);
    }
    
    try (HttpSolrClient client = new HttpSolrClient(jetty.getBaseUrl().toString() + "/debug/foo", null, null, false)) {
       try {
         client.query(q);
       } catch (ParseException ignored) {}
      assertNull(DebugServlet.headers.get("Accept-Encoding"));
     }

    assertNull(DebugServlet.headers.get("Accept-Encoding"));
     
     // verify server compresses output
     HttpGet get = new HttpGet(jetty.getBaseUrl().toString() + "/collection1" +
                               "/select?q=foo&wt=xml");
     get.setHeader("Accept-Encoding", "gzip");
    CloseableHttpClient httpclient = HttpClientUtil.createClient(null);
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(HttpClientUtil.PROP_ALLOW_COMPRESSION, true);
    
    RequestConfig config = RequestConfig.custom().setDecompressionEnabled(false).build();   
    get.setConfig(config);
    
    CloseableHttpClient httpclient = HttpClientUtil.createClient(params);
     HttpEntity entity = null;
     try {
      HttpResponse response = httpclient.execute(get);
      HttpResponse response = httpclient.execute(get, HttpClientUtil.createNewHttpClientRequestContext());
       entity = response.getEntity();
       Header ceheader = entity.getContentEncoding();
      assertNotNull(Arrays.asList(response.getAllHeaders()).toString(), ceheader);
       assertEquals("gzip", ceheader.getValue());
     } finally {
       if (entity != null) {
         entity.getContent().close();
       }
      httpclient.close();
      HttpClientUtil.close(httpclient);
     }
     
     // verify compressed response can be handled
     try (HttpSolrClient client = new HttpSolrClient(jetty.getBaseUrl().toString() + "/collection1")) {
      client.setAllowCompression(true);
      SolrQuery q = new SolrQuery("foo");
      q = new SolrQuery("foo");
       QueryResponse response = client.query(q);
       assertEquals(0, response.getStatus());
     }
@@ -589,28 +601,11 @@ public class BasicHttpSolrClientTest extends SolrJettyTestBase {
     }
 
   }
  
  @Test
  public void testSetParametersExternalClient() throws IOException{

    try (CloseableHttpClient httpClient = HttpClientUtil.createClient(null);
         HttpSolrClient solrClient = new HttpSolrClient(jetty.getBaseUrl().toString(), httpClient)) {

      try {
        solrClient.setMaxTotalConnections(1);
        fail("Operation should not succeed.");
      } catch (UnsupportedOperationException ignored) {}
      try {
        solrClient.setDefaultMaxConnectionsPerHost(1);
        fail("Operation should not succeed.");
      } catch (UnsupportedOperationException ignored) {}

    }
  }
 
   @Test
   public void testGetRawStream() throws SolrServerException, IOException{
    try (CloseableHttpClient client = HttpClientUtil.createClient(null)) {
    CloseableHttpClient client = HttpClientUtil.createClient(null);
    try {
       HttpSolrClient solrClient = new HttpSolrClient(jetty.getBaseUrl().toString() + "/collection1",
           client, null);
       QueryRequest req = new QueryRequest();
@@ -618,6 +613,8 @@ public class BasicHttpSolrClientTest extends SolrJettyTestBase {
       InputStream stream = (InputStream) response.get("stream");
       assertNotNull(stream);
       stream.close();
    } finally {
      HttpClientUtil.close(client);;
     }
   }
 
@@ -646,7 +643,7 @@ public class BasicHttpSolrClientTest extends SolrJettyTestBase {
     IOException {
       log.info("Intercepted params: "+context);
 
      RequestWrapper wrapper = (RequestWrapper) request;
      HttpRequestWrapper wrapper = (HttpRequestWrapper) request;
       URIBuilder uribuilder = new URIBuilder(wrapper.getURI());
       uribuilder.addParameter("b", "\u4321");
       try {
@@ -672,17 +669,15 @@ public class BasicHttpSolrClientTest extends SolrJettyTestBase {
       cookie.setPath("/");
       cookie.setDomain(jetty.getBaseUrl().getHost());
 
      CookieStore cookieStore = new BasicCookieStore();        
      CookieSpecRegistry registry = (CookieSpecRegistry) context.getAttribute(ClientContext.COOKIESPEC_REGISTRY);
      String policy = HttpClientParams.getCookiePolicy(request.getParams());
      CookieSpec cookieSpec = registry.getCookieSpec(policy, request.getParams());
      CookieStore cookieStore = new BasicCookieStore();
      CookieSpec cookieSpec = new SolrPortAwareCookieSpecFactory().create(context);
     // CookieSpec cookieSpec = registry.lookup(policy).create(context);
       // Add the cookies to the request
       List<Header> headers = cookieSpec.formatCookies(Collections.singletonList(cookie));
       for (Header header : headers) {
         request.addHeader(header);
       }
      context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
      context.setAttribute(ClientContext.COOKIE_SPEC, cookieSpec);
      context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
     }
   };
 
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
index 77d8d0fff3b..8c50e8eb915 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
@@ -631,15 +631,13 @@ public class CloudSolrClientTest extends AbstractFullDistribZkTestBase {
   }
 
   public void customHttpClientTest() throws IOException {

    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(HttpClientUtil.PROP_SO_TIMEOUT, 1000);

    try (CloseableHttpClient client = HttpClientUtil.createClient(params);
         CloudSolrClient solrClient = new CloudSolrClient(zkServer.getZkAddress(), client)) {
    CloseableHttpClient client = HttpClientUtil.createClient(null);
    try (CloudSolrClient solrClient = new CloudSolrClient(zkServer.getZkAddress(), client)) {
 
       assertTrue(solrClient.getLbClient().getHttpClient() == client);
 
    } finally {
      HttpClientUtil.close(client);
     }
   }
 }
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/ExternalHttpClientTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/ExternalHttpClientTest.java
deleted file mode 100644
index c788ae396ed..00000000000
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/ExternalHttpClientTest.java
++ /dev/null
@@ -1,75 +0,0 @@
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
package org.apache.solr.client.solrj.impl;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.SolrJettyTestBase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class ExternalHttpClientTest extends SolrJettyTestBase {
  @BeforeClass
  public static void beforeTest() throws Exception {
    JettyConfig jettyConfig = JettyConfig.builder()
        .withServlet(new ServletHolder(BasicHttpSolrClientTest.SlowServlet.class), "/slow/*")
        .withSSLConfig(sslConfig)
        .build();
    createJetty(legacyExampleCollection1SolrHome(), jettyConfig);
  }

  /**
   * The internal client created by HttpSolrClient is a SystemDefaultHttpClient
   * which takes care of merging request level params (such as timeout) with the
   * configured defaults.
   *
   * However, if an external HttpClient is passed to HttpSolrClient,
   * the logic in InternalHttpClient.executeMethod replaces the configured defaults
   * by request level params if they exist. That is why we must test a setting such
   * as timeout with an external client to assert that the defaults are indeed being
   * used
   *
   * See SOLR-6245 for more details
   */
  @Test
  public void testTimeoutWithExternalClient() throws Exception {

    HttpClientBuilder builder = HttpClientBuilder.create();
    RequestConfig config = RequestConfig.custom().setSocketTimeout(2000).build();
    builder.setDefaultRequestConfig(config);

    try (CloseableHttpClient httpClient = builder.build();
         HttpSolrClient solrClient = new HttpSolrClient(jetty.getBaseUrl().toString() + "/slow/foo", httpClient)) {

      SolrQuery q = new SolrQuery("*:*");
      try {
        solrClient.query(q, SolrRequest.METHOD.GET);
        fail("No exception thrown.");
      } catch (SolrServerException e) {
        assertTrue(e.getMessage().contains("Timeout"));
      }
    }
  }
}
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/HttpClientUtilTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/HttpClientUtilTest.java
deleted file mode 100644
index c881b7a93bf..00000000000
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/HttpClientUtilTest.java
++ /dev/null
@@ -1,162 +0,0 @@
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
package org.apache.solr.client.solrj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.auth.AuthScope;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.SSLTestConfig;
import org.junit.Test;

public class HttpClientUtilTest {

  @Test
  public void testNoParamsSucceeds() throws IOException {
    CloseableHttpClient client = HttpClientUtil.createClient(null);
    client.close();
  }

  @Test
  public void testSetParams() {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(HttpClientUtil.PROP_ALLOW_COMPRESSION, true);
    params.set(HttpClientUtil.PROP_BASIC_AUTH_PASS, "pass");
    params.set(HttpClientUtil.PROP_BASIC_AUTH_USER, "user");
    params.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, 12345);
    params.set(HttpClientUtil.PROP_FOLLOW_REDIRECTS, true);
    params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, 22345);
    params.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 32345);
    params.set(HttpClientUtil.PROP_SO_TIMEOUT, 42345);
    params.set(HttpClientUtil.PROP_USE_RETRY, false);
    DefaultHttpClient client = (DefaultHttpClient) HttpClientUtil.createClient(params);
    try {
      assertEquals(12345, HttpConnectionParams.getConnectionTimeout(client.getParams()));
      assertEquals(PoolingClientConnectionManager.class, client.getConnectionManager().getClass());
      assertEquals(22345, ((PoolingClientConnectionManager)client.getConnectionManager()).getMaxTotal());
      assertEquals(32345, ((PoolingClientConnectionManager)client.getConnectionManager()).getDefaultMaxPerRoute());
      assertEquals(42345, HttpConnectionParams.getSoTimeout(client.getParams()));
      assertEquals(HttpClientUtil.NO_RETRY, client.getHttpRequestRetryHandler());
      assertEquals("pass", client.getCredentialsProvider().getCredentials(new AuthScope("127.0.0.1", 1234)).getPassword());
      assertEquals("user", client.getCredentialsProvider().getCredentials(new AuthScope("127.0.0.1", 1234)).getUserPrincipal().getName());
      assertEquals(true, client.getParams().getParameter(ClientPNames.HANDLE_REDIRECTS));
    } finally {
      client.close();
    }
  }

  @Test
  public void testAuthSchemeConfiguration() {
    System.setProperty(Krb5HttpClientConfigurer.LOGIN_CONFIG_PROP, "test");
    try {
      HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());
      AbstractHttpClient client = (AbstractHttpClient)HttpClientUtil.createClient(null);
      assertEquals(1, client.getAuthSchemes().getSchemeNames().size());
      assertTrue(AuthSchemes.SPNEGO.equalsIgnoreCase(client.getAuthSchemes().getSchemeNames().get(0)));
    } finally {
      //Cleanup the system property.
      System.clearProperty(Krb5HttpClientConfigurer.LOGIN_CONFIG_PROP);
    }
  }

  @Test
  public void testReplaceConfigurer() throws IOException{
    
    try {
    final AtomicInteger counter = new AtomicInteger();
    HttpClientConfigurer custom = new HttpClientConfigurer(){
      @Override
      public void configure(DefaultHttpClient httpClient, SolrParams config) {
        super.configure(httpClient, config);
        counter.set(config.getInt("custom-param", -1));
      }
      
    };
    
    HttpClientUtil.setConfigurer(custom);
    
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set("custom-param", 5);
    HttpClientUtil.createClient(params).close();
    assertEquals(5, counter.get());
    } finally {
      //restore default configurer
      HttpClientUtil.setConfigurer(new HttpClientConfigurer());
    }

  }
  
  @Test
  @SuppressWarnings("deprecation")
  public void testSSLSystemProperties() throws IOException {
    CloseableHttpClient client = HttpClientUtil.createClient(null);
    try {
      SSLTestConfig.setSSLSystemProperties();
      assertNotNull("HTTPS scheme could not be created using the javax.net.ssl.* system properties.", 
          client.getConnectionManager().getSchemeRegistry().get("https"));
      
      System.clearProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME);
      client.close();
      client = HttpClientUtil.createClient(null);
      assertEquals(BrowserCompatHostnameVerifier.class, getHostnameVerifier(client).getClass());
      
      System.setProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME, "true");
      client.close();
      client = HttpClientUtil.createClient(null);
      assertEquals(BrowserCompatHostnameVerifier.class, getHostnameVerifier(client).getClass());
      
      System.setProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME, "");
      client.close();
      client = HttpClientUtil.createClient(null);
      assertEquals(BrowserCompatHostnameVerifier.class, getHostnameVerifier(client).getClass());
      
      System.setProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME, "false");
      client.close();
      client = HttpClientUtil.createClient(null);
      assertEquals(AllowAllHostnameVerifier.class, getHostnameVerifier(client).getClass());
    } finally {
      SSLTestConfig.clearSSLSystemProperties();
      System.clearProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME);
      client.close();
    }
  }
  
  @SuppressWarnings("deprecation")
  private X509HostnameVerifier getHostnameVerifier(HttpClient client) {
    return ((SSLSocketFactory) client.getConnectionManager().getSchemeRegistry()
        .get("https").getSchemeSocketFactory()).getHostnameVerifier();
  }
  
}
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/LBHttpSolrClientTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/LBHttpSolrClientTest.java
index dba18d07732..94af2ca26ce 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/LBHttpSolrClientTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/LBHttpSolrClientTest.java
@@ -38,18 +38,23 @@ public class LBHttpSolrClientTest {
    */
   @Test
   public void testLBHttpSolrClientHttpClientResponseParserStringArray() throws IOException {

    try (CloseableHttpClient httpClient = HttpClientUtil.createClient(new ModifiableSolrParams());
    CloseableHttpClient httpClient = HttpClientUtil.createClient(new ModifiableSolrParams());
    try (
          LBHttpSolrClient testClient = new LBHttpSolrClient(httpClient, (ResponseParser) null);
          HttpSolrClient httpSolrClient = testClient.makeSolrClient("http://127.0.0.1:8080")) {
       assertNull("Generated server should have null parser.", httpSolrClient.getParser());
    } finally {
      HttpClientUtil.close(httpClient);
     }
 
     ResponseParser parser = new BinaryResponseParser();
    try (CloseableHttpClient httpClient = HttpClientUtil.createClient(new ModifiableSolrParams());
         LBHttpSolrClient testClient = new LBHttpSolrClient(httpClient, parser);
         HttpSolrClient httpSolrClient = testClient.makeSolrClient("http://127.0.0.1:8080")) {
      assertEquals("Invalid parser passed to generated server.", parser, httpSolrClient.getParser());
    httpClient = HttpClientUtil.createClient(new ModifiableSolrParams());
    try {
      try ( LBHttpSolrClient testClient = new LBHttpSolrClient(httpClient, parser); HttpSolrClient httpSolrClient = testClient.makeSolrClient("http://127.0.0.1:8080")) {
        assertEquals("Invalid parser passed to generated server.", parser, httpSolrClient.getParser());
      }
    } finally {
      HttpClientUtil.close(httpClient);
     }
   }
   
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/request/SchemaTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/request/SchemaTest.java
index 72051b123aa..be36bf3a61d 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/request/SchemaTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/request/SchemaTest.java
@@ -105,12 +105,11 @@ public class SchemaTest extends RestTestBase {
   }
 
   @After
  public void cleanup() throws Exception {
  public void cleanup() throws Exception  {
     if (jetty != null) {
       jetty.stop();
       jetty = null;
     }
    client = null;
     if (restTestHarness != null) {
       restTestHarness.close();
     }
@@ -424,7 +423,8 @@ public class SchemaTest extends RestTestBase {
     fieldAttributes.put("type", "string");
     SchemaRequest.AddDynamicField addDFieldUpdateSchemaRequest =
         new SchemaRequest.AddDynamicField(fieldAttributes);
    SchemaResponse.UpdateResponse addDFieldFirstResponse = addDFieldUpdateSchemaRequest.process(getSolrClient());
    SolrClient client = getSolrClient();
    SchemaResponse.UpdateResponse addDFieldFirstResponse = addDFieldUpdateSchemaRequest.process(client);
     assertValidSchemaResponse(addDFieldFirstResponse);
 
     SchemaResponse.UpdateResponse addDFieldSecondResponse = addDFieldUpdateSchemaRequest.process(getSolrClient());
@@ -680,7 +680,8 @@ public class SchemaTest extends RestTestBase {
     fieldTypeDefinition.setAttributes(fieldTypeAttributes);
     SchemaRequest.AddFieldType addFieldTypeRequest =
         new SchemaRequest.AddFieldType(fieldTypeDefinition);
    SchemaResponse.UpdateResponse addFieldTypeResponse = addFieldTypeRequest.process(getSolrClient());
    SolrClient c = getSolrClient();
    SchemaResponse.UpdateResponse addFieldTypeResponse = addFieldTypeRequest.process(c);
     assertValidSchemaResponse(addFieldTypeResponse);
 
     SchemaRequest.FieldType fieldTypeRequest = new SchemaRequest.FieldType(fieldTypeName);
diff --git a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
index a751459e596..4ae46f8fa91 100644
-- a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
++ b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
@@ -229,9 +229,6 @@ public abstract class BaseDistributedSearchTestCase extends SolrTestCaseJ4 {
   protected boolean verifyStress = true;
   protected int nThreads = 3;
 
  protected int clientConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
  protected int clientSoTimeout = 90000;

   public static int ORDERED = 1;
   public static int SKIP = 2;
   public static int SKIPVAL = 4;
@@ -443,10 +440,6 @@ public abstract class BaseDistributedSearchTestCase extends SolrTestCaseJ4 {
     try {
       // setup the client...
       HttpSolrClient client = new HttpSolrClient(buildUrl(port) + "/" + DEFAULT_TEST_CORENAME);
      client.setConnectionTimeout(clientConnectionTimeout);
      client.setSoTimeout(clientSoTimeout);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       return client;
     }
     catch (Exception ex) {
diff --git a/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java b/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java
index 0e77a471381..5924593e63c 100644
-- a/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java
@@ -22,10 +22,10 @@ import org.apache.solr.client.solrj.SolrClient;
 import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
 import org.apache.solr.client.solrj.embedded.JettyConfig;
 import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.util.ExternalPaths;
 import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 import org.slf4j.Logger;
@@ -119,6 +119,11 @@ abstract public class SolrJettyTestBase extends SolrTestCaseJ4
     return jetty;
   }
 
  @After
  public void afterClass() throws Exception {
    if (client != null) client.close();
    client = null;
  }
 
   @AfterClass
   public static void afterSolrJettyTestBase() throws Exception {
@@ -126,8 +131,6 @@ abstract public class SolrJettyTestBase extends SolrTestCaseJ4
       jetty.stop();
       jetty = null;
     }
    if (client != null) client.close();
    client = null;
   }
 
 
@@ -153,15 +156,18 @@ abstract public class SolrJettyTestBase extends SolrTestCaseJ4
         String url = jetty.getBaseUrl().toString() + "/" + "collection1";
         HttpSolrClient client = new HttpSolrClient( url );
         client.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        client.setDefaultMaxConnectionsPerHost(100);
        client.setMaxTotalConnections(100);
         return client;
       }
       catch( Exception ex ) {
         throw new RuntimeException( ex );
       }
     } else {
      return new EmbeddedSolrServer( h.getCoreContainer(), "collection1" );
      return new EmbeddedSolrServer( h.getCoreContainer(), "collection1" ) {
        @Override
        public void close() {
          // do not close core container
        }
      };
     }
   }
 
diff --git a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
index 3ce252f9b4d..6b28eb81a07 100644
-- a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
++ b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
@@ -66,7 +66,6 @@ import org.apache.lucene.util.LuceneTestCase.SuppressSysoutChecks;
 import org.apache.lucene.util.QuickPatchThreadsFilter;
 import org.apache.lucene.util.TestUtil;
 import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
 import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.util.ClientUtils;
 import org.apache.solr.cloud.IpTables;
@@ -226,7 +225,7 @@ public abstract class SolrTestCaseJ4 extends LuceneTestCase {
     
     sslConfig = buildSSLConfig();
     //will use ssl specific or default depending on sslConfig
    HttpClientUtil.setConfigurer(sslConfig.getHttpClientConfigurer());
    HttpClientUtil.setHttpClientBuilder(sslConfig.getHttpClientBuilder());
     if(isSSLMode()) {
       // SolrCloud tests should usually clear this
       System.setProperty("urlScheme", "https");
@@ -269,9 +268,7 @@ public abstract class SolrTestCaseJ4 extends LuceneTestCase {
       System.clearProperty("useCompoundFile");
       System.clearProperty("urlScheme");
       
      if (isSSLMode()) {
        HttpClientUtil.setConfigurer(new HttpClientConfigurer());
      }
      HttpClientUtil.resetHttpClientBuilder();
 
       // clean up static
       sslConfig = null;
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
index a584dbd450b..090dfe8fddd 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
@@ -1629,8 +1629,6 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
       HttpSolrClient client = new HttpSolrClient(url);
       client.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
       client.setSoTimeout(60000);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       return client;
     } catch (Exception ex) {
       throw new RuntimeException(ex);
@@ -1642,8 +1640,6 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
       // setup the server...
       HttpSolrClient client = new HttpSolrClient(baseUrl + "/" + collection);
       client.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      client.setDefaultMaxConnectionsPerHost(100);
      client.setMaxTotalConnections(100);
       return client;
     }
     catch (Exception ex) {
diff --git a/solr/test-framework/src/java/org/apache/solr/util/RestTestHarness.java b/solr/test-framework/src/java/org/apache/solr/util/RestTestHarness.java
index de5f6623dfe..3f2a699cae9 100644
-- a/solr/test-framework/src/java/org/apache/solr/util/RestTestHarness.java
++ b/solr/test-framework/src/java/org/apache/solr/util/RestTestHarness.java
@@ -204,7 +204,7 @@ public class RestTestHarness extends BaseTestHarness implements Closeable {
   private String getResponse(HttpUriRequest request) throws IOException {
     HttpEntity entity = null;
     try {
      entity = httpClient.execute(request).getEntity();
      entity = httpClient.execute(request, HttpClientUtil.createNewHttpClientRequestContext()).getEntity();
       return EntityUtils.toString(entity, StandardCharsets.UTF_8);
     } finally {
       EntityUtils.consumeQuietly(entity);
@@ -213,6 +213,6 @@ public class RestTestHarness extends BaseTestHarness implements Closeable {
 
   @Override
   public void close() throws IOException {
    httpClient.close();
    HttpClientUtil.close(httpClient);
   }
 }
diff --git a/solr/test-framework/src/java/org/apache/solr/util/SSLTestConfig.java b/solr/test-framework/src/java/org/apache/solr/util/SSLTestConfig.java
index 8d626dc1c2d..68c4b076c0d 100644
-- a/solr/test-framework/src/java/org/apache/solr/util/SSLTestConfig.java
++ b/solr/test-framework/src/java/org/apache/solr/util/SSLTestConfig.java
@@ -25,15 +25,21 @@ import java.security.UnrecoverableKeyException;
 
 import javax.net.ssl.SSLContext;
 
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
 import org.apache.http.conn.scheme.Scheme;
 import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
 import org.apache.http.conn.ssl.SSLContexts;
 import org.apache.http.conn.ssl.SSLSocketFactory;
 import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.solr.client.solrj.embedded.SSLConfig;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpClientUtil.SchemaRegistryProvider;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
 import org.eclipse.jetty.util.resource.Resource;
 import org.eclipse.jetty.util.security.CertificateUtils;
 
@@ -44,7 +50,6 @@ public class SSLTestConfig extends SSLConfig {
   private static String TEST_KEYSTORE_PATH = TEST_KEYSTORE != null
       && TEST_KEYSTORE.exists() ? TEST_KEYSTORE.getAbsolutePath() : null;
   private static String TEST_KEYSTORE_PASSWORD = "secret";
  private static HttpClientConfigurer DEFAULT_CONFIGURER = new HttpClientConfigurer();
   
   public SSLTestConfig() {
     this(false, false);
@@ -59,12 +64,13 @@ public class SSLTestConfig extends SSLConfig {
   }
   
   /**
   * Will provide an HttpClientConfigurer for SSL support (adds https and
   * Will provide an SolrHttpClientBuilder for SSL support (adds https and
    * removes http schemes) is SSL is enabled, otherwise return the default
   * configurer
   * SolrHttpClientBuilder
    */
  public HttpClientConfigurer getHttpClientConfigurer() {
    return isSSLMode() ? new SSLHttpClientConfigurer() : DEFAULT_CONFIGURER;
  public SolrHttpClientBuilder getHttpClientBuilder() {
    SolrHttpClientBuilder builder = HttpClientUtil.getHttpClientBuilder();
    return isSSLMode() ? new SSLHttpClientBuilderProvider().getBuilder(builder) : builder;
   }
 
   /**
@@ -88,20 +94,53 @@ public class SSLTestConfig extends SSLConfig {
     }
   }
   
  private class SSLHttpClientConfigurer extends HttpClientConfigurer {
    @SuppressWarnings("deprecation")
    public void configure(DefaultHttpClient httpClient, SolrParams config) {
      super.configure(httpClient, config);
      SchemeRegistry registry = httpClient.getConnectionManager().getSchemeRegistry();
      // Make sure no tests cheat by using HTTP
      registry.unregister("http");
      try {
        registry.register(new Scheme("https", 443, new SSLSocketFactory(buildSSLContext())));
      } catch (KeyManagementException | UnrecoverableKeyException
          | NoSuchAlgorithmException | KeyStoreException ex) {
        throw new IllegalStateException("Unable to setup https scheme for HTTPClient to test SSL.", ex);
      }
  private class SSLHttpClientBuilderProvider  {
    

    public SolrHttpClientBuilder getBuilder(SolrHttpClientBuilder builder) {

      HttpClientUtil.setSchemeRegistryProvider(new SchemaRegistryProvider() {
        
        @Override
        public Registry<ConnectionSocketFactory> getSchemaRegistry() {
          SSLConnectionSocketFactory sslConnectionFactory;
          try {
            boolean sslCheckPeerName = toBooleanDefaultIfNull(
                toBooleanObject(System.getProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME)), true);
            if (sslCheckPeerName == false) {
              sslConnectionFactory = new SSLConnectionSocketFactory(buildSSLContext(),
                  SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            } else {
              sslConnectionFactory = new SSLConnectionSocketFactory(buildSSLContext());
            }
          } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new IllegalStateException("Unable to setup https scheme for HTTPClient to test SSL.", e);
          }
          return  RegistryBuilder.<ConnectionSocketFactory>create()
              .register("https", sslConnectionFactory).build();
        }
      });
      HttpClientUtil.setHttpClientBuilder(builder);
      return builder;
    }

  }
  
  public static boolean toBooleanDefaultIfNull(Boolean bool, boolean valueIfNull) {
    if (bool == null) {
      return valueIfNull;
    }
    return bool.booleanValue() ? true : false;
  }
  
  public static Boolean toBooleanObject(String str) {
    if ("true".equalsIgnoreCase(str)) {
      return Boolean.TRUE;
    } else if ("false".equalsIgnoreCase(str)) {
      return Boolean.FALSE;
     }
    // no match
    return null;
   }
   
   public static void setSSLSystemProperties() {
- 
2.19.1.windows.1

