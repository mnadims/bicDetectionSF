From af6aba67bbb26d4116532d45f3ea0117aa3cb6a8 Mon Sep 17 00:00:00 2001
From: Bob Nettleton <rnettleton@hortonworks.com>
Date: Thu, 15 Dec 2016 15:26:41 -0500
Subject: [PATCH] AMBARI-19105. Ambari LogSearch REST Layer should use a
 configurable timeout for HTTP connections to LogSearch Server. (rnettleton)

--
 ambari-server/docs/configuration/index.md     | 15 ++--
 .../server/api/services/ClusterService.java   |  2 +-
 .../server/api/services/LoggingService.java   | 19 +++--
 .../server/configuration/Configuration.java   | 34 ++++++++
 .../AmbariManagementController.java           | 12 +++
 .../AmbariManagementControllerImpl.java       |  8 ++
 .../internal/LoggingResourceProvider.java     | 24 ------
 .../LogSearchDataRetrievalService.java        | 32 ++++++-
 .../LoggingRequestHelperFactoryImpl.java      | 27 +++++-
 .../logging/LoggingRequestHelperImpl.java     | 35 ++++++--
 .../LoggingSearchPropertyProvider.java        | 14 ++--
 .../server/controller/logging/Utils.java      |  2 +-
 .../api/services/LoggingServiceTest.java      |  3 +-
 .../LogSearchDataRetrievalServiceTest.java    | 17 ++++
 .../LoggingRequestHelperFactoryImplTest.java  | 66 ++++++++++++++-
 .../logging/LoggingRequestHelperImplTest.java | 84 +++++++++++++++++++
 .../server/controller/logging/UtilsTest.java  |  2 +-
 17 files changed, 340 insertions(+), 56 deletions(-)

diff --git a/ambari-server/docs/configuration/index.md b/ambari-server/docs/configuration/index.md
index 6ff263c395..50864f2da2 100644
-- a/ambari-server/docs/configuration/index.md
++ b/ambari-server/docs/configuration/index.md
@@ -132,6 +132,7 @@ The following are the properties which can be used to configure Ambari.
 | default.kdcserver.port | The port used to communicate with the Kerberos Key Distribution Center. |`88` | 
 | extensions.path | The location on the Ambari Server where stack extensions exist.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/extensions`</ul> | | 
 | http.cache-control | The value that will be used to set the `Cache-Control` HTTP response header. |`no-store` | 
| http.charset | The value that will be used to set the Character encoding to HTTP response header. |`utf-8` | 
 | http.pragma | The value that will be used to set the `PRAGMA` HTTP response header. |`no-cache` | 
 | http.strict-transport-security | When using SSL, this will be used to set the `Strict-Transport-Security` response header. |`max-age=31536000` | 
 | http.x-content-type-options | The value that will be used to set the `X-CONTENT-TYPE` HTTP response header. |`nosniff` | 
@@ -147,6 +148,8 @@ The following are the properties which can be used to configure Ambari.
 | kerberos.operation.retry.timeout | The time to wait (in seconds) between failed kerberos operations retries. |`10` | 
 | ldap.sync.username.collision.behavior | Determines how to handle username collision while updating from LDAP.<br/><br/>The following are examples of valid values:<ul><li>`skip`<li>`convert`</ul> |`convert` | 
 | log4j.monitor.delay | Indicates the delay, in milliseconds, for the log4j monitor to check for changes |`300000` | 
| logsearch.portal.connect.timeout | The time, in milliseconds, that the Ambari Server will wait while attempting to connect to the LogSearch Portal service. |`5000` | 
| logsearch.portal.read.timeout | The time, in milliseconds, that the Ambari Server will wait while attempting to read a response from the LogSearch Portal service. |`5000` | 
 | metadata.path | The location on the Ambari Server where the stack resources exist.<br/><br/>The following are examples of valid values:<ul><li>`/var/lib/ambari-server/resources/stacks`</ul> | | 
 | metrics.retrieval-service.cache.timeout | The amount of time, in minutes, that JMX and REST metrics retrieved directly can remain in the cache. |`30` | 
 | metrics.retrieval-service.request.ttl | The number of seconds to wait between issuing JMX or REST metric requests to the same endpoint. This property is used to throttle requests to the same URL being made too close together<br/><br/> This property is related to `metrics.retrieval-service.request.ttl.enabled`. |`5` | 
@@ -231,16 +234,16 @@ The following are the properties which can be used to configure Ambari.
 | server.jdbc.user.passwd | The password for the user when logging into the database. |`bigdata` | 
 | server.locks.profiling | Enable the profiling of internal locks. |`false` | 
 | server.metrics.retrieval-service.thread.priority | The priority of threads used by the service which retrieves JMX and REST metrics directly from their respective endpoints. |`5` | 
| server.metrics.retrieval-service.threadpool.size.core | The core number of threads used to retrieve JMX and REST metrics directly from their respective endpoints. |`8` | 
| server.metrics.retrieval-service.threadpool.size.max | The maximum number of threads used to retrieve JMX and REST metrics directly from their respective endpoints. |`16` | 
| server.metrics.retrieval-service.threadpool.worker.size | The number of queued requests allowed for JMX and REST metrics before discarding old requests which have not been fullfilled. |`160` | 
| server.metrics.retrieval-service.threadpool.size.core | The core number of threads used to retrieve JMX and REST metrics directly from their respective endpoints. |`16` | 
| server.metrics.retrieval-service.threadpool.size.max | The maximum number of threads used to retrieve JMX and REST metrics directly from their respective endpoints. |`32` | 
| server.metrics.retrieval-service.threadpool.worker.size | The number of queued requests allowed for JMX and REST metrics before discarding old requests which have not been fullfilled. |`320` | 
 | server.operations.retry-attempts | The number of retry attempts for failed API and blueprint operations. |`0` | 
 | server.os_family | The operating system family for all hosts in the cluster. This is used when bootstrapping agents and when enabling Kerberos.<br/><br/>The following are examples of valid values:<ul><li>`redhat`<li>`ubuntu`</ul> | | 
 | server.os_type | The operating system version for all hosts in the cluster. This is used when bootstrapping agents and when enabling Kerberos.<br/><br/>The following are examples of valid values:<ul><li>`6`<li>`7`</ul> | | 
 | server.persistence.type | The type of database connection being used. Unless using an embedded PostgresSQL server, then this should be `remote`.<br/><br/>The following are examples of valid values:<ul><li>`local`<li>`remote`</ul> |`local` | 
 | server.property-provider.threadpool.completion.timeout | The maximum time, in milliseconds, that federated requests for data can execute before being terminated. Increasing this value could result in degraded performanc from the REST APIs. |`5000` | 
| server.property-provider.threadpool.size.core | The core number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints. |`8` | 
| server.property-provider.threadpool.size.max | The maximum number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints. |`16` | 
| server.property-provider.threadpool.size.core | The core number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints. |`16` | 
| server.property-provider.threadpool.size.max | The maximum number of threads that will be used to retrieve data from federated datasources, such as remote JMX endpoints. |`32` | 
 | server.property-provider.threadpool.worker.size | The maximum size of pending federated datasource requests, such as those to JMX endpoints, which can be queued before rejecting new requests. |`2147483647` | 
 | server.requestlogs.namepattern | The pattern of request log file name |`ambari-access-yyyy_mm_dd.log` | 
 | server.requestlogs.path | The location on the Ambari Server where request logs can be created. | | 
@@ -276,6 +279,7 @@ The following are the properties which can be used to configure Ambari.
 | task.query.parameterlist.size | The maximum number of tasks which can be queried by ID from the database. |`999` | 
 | topology.task.creation.parallel | Indicates whether parallel topology task creation is enabled |`false` | 
 | topology.task.creation.parallel.threads | The number of threads to use for parallel topology task creation if enabled |`10` | 
| view.extract-after-cluster-config | Drives view extraction in case of blueprint deployments; non-system views are deployed when cluster configuration is successful |`false` | 
 | view.extraction.threadpool.size.core | The number of threads used to extract Ambari Views when Ambari Server is starting up. |`10` | 
 | view.extraction.threadpool.size.max | The maximum number of threads used to extract Ambari Views when Ambari Server is starting up. |`20` | 
 | view.extraction.threadpool.timeout | The time, in milliseconds, that non-core threads will live when extraction views on Ambari Server startup. |`100000` | 
@@ -285,6 +289,7 @@ The following are the properties which can be used to configure Ambari.
 | views.ambari.request.read.timeout.millis | The amount of time, in milliseconds, that a view will wait before terminating an HTTP(S) read request to the Ambari REST API. |`45000` | 
 | views.dir | The directory on the Ambari Server file system used for expanding Views and storing webapp work. |`/var/lib/ambari-server/resources/views` | 
 | views.http.cache-control | The value that will be used to set the `Cache-Control` HTTP response header for Ambari View requests. |`no-store` | 
| views.http.charset | The value that will be used to set the Character encoding to HTTP response header for Ambari View requests. |`utf-8` | 
 | views.http.pragma | The value that will be used to set the `PRAGMA` HTTP response header for Ambari View requests. |`no-cache` | 
 | views.http.strict-transport-security | The value that will be used to set the `Strict-Transport-Security` HTTP response header for Ambari View requests. |`max-age=31536000` | 
 | views.http.x-content-type-options | The value that will be used to set the `X-CONTENT-TYPE` HTTP response header for Ambari View requests. |`nosniff` | 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ClusterService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ClusterService.java
index 072c4a29fa..9f6feaa691 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ClusterService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ClusterService.java
@@ -662,7 +662,7 @@ public class ClusterService extends BaseService {
   @Path("{clusterName}/logging")
   public LoggingService getLogging(@Context javax.ws.rs.core.Request request,
                                    @PathParam("clusterName") String clusterName) {
    return new LoggingService(clusterName);
    return AmbariServer.getController().getLoggingService(clusterName);
   }
 
   // ----- helper methods ----------------------------------------------------
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/LoggingService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/LoggingService.java
index ea4960fdf0..d83aa253f0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/LoggingService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/LoggingService.java
@@ -42,7 +42,6 @@ import org.apache.ambari.server.controller.internal.ResourceImpl;
 import org.apache.ambari.server.controller.logging.LogQueryResponse;
 import org.apache.ambari.server.controller.logging.LoggingRequestHelper;
 import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactory;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactoryImpl;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.security.authorization.AuthorizationException;
 import org.apache.ambari.server.security.authorization.AuthorizationHelper;
@@ -52,6 +51,8 @@ import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.utils.RetryHelper;
 import org.apache.commons.lang.StringUtils;
 
import com.google.inject.Inject;

 /**
  * This Service provides access to the LogSearch query services, including:
  *     - Access to all service log files in a given cluster
@@ -68,19 +69,19 @@ public class LoggingService extends BaseService {
 
   private final ControllerFactory controllerFactory;
 
  private final LoggingRequestHelperFactory helperFactory;
  @Inject
  private LoggingRequestHelperFactory helperFactory;
 
 
   private final String clusterName;
 
   public LoggingService(String clusterName) {
    this(clusterName, new DefaultControllerFactory(), new LoggingRequestHelperFactoryImpl());
    this(clusterName, new DefaultControllerFactory());
   }
 
  public LoggingService(String clusterName, ControllerFactory controllerFactory, LoggingRequestHelperFactory helperFactory) {
  public LoggingService(String clusterName, ControllerFactory controllerFactory) {
     this.clusterName = clusterName;
     this.controllerFactory = controllerFactory;
    this.helperFactory = helperFactory;
   }
 
   @GET
@@ -207,6 +208,14 @@ public class LoggingService extends BaseService {
     return responseBuilder.build();
   }
 
  /**
   * Package-level setter that facilitates simpler unit testing
   *
   * @param helperFactory
   */
  void setLoggingRequestHelperFactory(LoggingRequestHelperFactory helperFactory) {
    this.helperFactory = helperFactory;
  }
 
   /**
    * Internal interface that defines an access factory for the
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java b/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
index f9b6878b2b..22d8168bd9 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
@@ -2561,6 +2561,22 @@ public class Configuration {
   public static final ConfigurationProperty<Integer> SRVR_API_ACCEPTOR_THREAD_COUNT = new ConfigurationProperty<>(
       "client.api.acceptor.count", null);
 
  /**
   * The time, in milliseconds, that the Ambari Server will wait while attempting to connect to the
   * LogSearch Portal service.
   */
  @Markdown(description = "The time, in milliseconds, that the Ambari Server will wait while attempting to connect to the LogSearch Portal service.")
  public static final ConfigurationProperty<Integer> LOGSEARCH_PORTAL_CONNECT_TIMEOUT = new ConfigurationProperty<>(
          "logsearch.portal.connect.timeout", 5000);

  /**
   * The time, in milliseconds, that the Ambari Server will wait while attempting to read a response from the
   * LogSearch Portal service.
   */
  @Markdown(description = "The time, in milliseconds, that the Ambari Server will wait while attempting to read a response from the LogSearch Portal service.")
  public static final ConfigurationProperty<Integer> LOGSEARCH_PORTAL_READ_TIMEOUT = new ConfigurationProperty<>(
    "logsearch.portal.read.timeout", 5000);

   private static final Logger LOG = LoggerFactory.getLogger(
     Configuration.class);
 
@@ -5314,6 +5330,24 @@ public class Configuration {
     return Boolean.parseBoolean(getProperty(TOPOLOGY_TASK_PARALLEL_CREATION_ENABLED));
   }
 
  /**
   * Get the connect timeout used for connecting to the LogSearch Portal Service
   *
   * @return
   */
  public int getLogSearchPortalConnectTimeout() {
    return NumberUtils.toInt(getProperty(LOGSEARCH_PORTAL_CONNECT_TIMEOUT));
  }

  /**
   * Get the read timeout used for connecting to the LogSearch Portal Service
   *
   * @return
   */
  public int getLogSearchPortalReadTimeout() {
    return NumberUtils.toInt(getProperty(LOGSEARCH_PORTAL_READ_TIMEOUT));
  }

   /**
    * Generates a markdown table which includes:
    * <ul>
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
index 389f973027..cc203242b7 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
@@ -28,6 +28,7 @@ import org.apache.ambari.server.RoleCommand;
 import org.apache.ambari.server.actionmanager.ActionManager;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.LoggingService;
 import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
 import org.apache.ambari.server.controller.internal.RequestStageContainer;
 import org.apache.ambari.server.controller.logging.LoggingSearchPropertyProvider;
@@ -872,6 +873,17 @@ public interface AmbariManagementController {
    */
   LoggingSearchPropertyProvider getLoggingSearchPropertyProvider();
 

  /**
   * Gets the LoggingService instance from the dependency injection framework.
   *
   * @param clusterName the cluster name associated with this LoggingService instance
   *
   * @return an instance of LoggingService associated with the specified cluster.
   */
  LoggingService getLoggingService(String clusterName);


   /**
    * Returns KerberosHelper instance
    * @return
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index 6b5731cd4d..5fe7585026 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -87,6 +87,7 @@ import org.apache.ambari.server.actionmanager.StageFactory;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.LoggingService;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.configuration.Configuration.DatabaseType;
 import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
@@ -5081,6 +5082,13 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
     return injector.getInstance(LoggingSearchPropertyProvider.class);
   }
 
  @Override
  public LoggingService getLoggingService(String clusterName) {
    LoggingService loggingService = new LoggingService(clusterName);
    injector.injectMembers(loggingService);
    return loggingService;
  }

   /**
    * {@inheritDoc}
    */
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/LoggingResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/LoggingResourceProvider.java
index 2eb1a639d0..b46399c0cf 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/LoggingResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/LoggingResourceProvider.java
@@ -22,8 +22,6 @@ package org.apache.ambari.server.controller.internal;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
@@ -113,28 +111,6 @@ public class LoggingResourceProvider extends AbstractControllerResourceProvider
     return Collections.singleton(resource);
   }
 
  private static List<Map<String, String>> createTestData(Resource resource) {
    // just create some test data for verifying basic resource code, not an actual result
    Map<String, String> levelCounts = new HashMap<String, String>();
    levelCounts.put("INFO", "100");
    levelCounts.put("WARN", "250");
    levelCounts.put("DEBUG", "300");

    resource.setProperty("logLevels", levelCounts);

    List<Map <String, String>> listOfResults = new LinkedList<Map<String, String>>();
    Map<String, String> resultOne = new HashMap<String, String>();
    resultOne.put("data", "This is a test sentence.");
    resultOne.put("score", "100");
    resultOne.put("level", "INFO");
    resultOne.put("type", "hdfs_namenode");
    resultOne.put("host", "c6401.ambari.apache.org");
    resultOne.put("LoggerName", "NameNodeLogger");

    listOfResults.add(resultOne);
    return listOfResults;
  }

   @Override
   public Set<String> checkPropertyIds(Set<String> propertyIds) {
     Set<String> unSupportedProperties =
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java
index e65cd59f3f..ce6094c0cb 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java
@@ -29,11 +29,14 @@ import org.apache.commons.collections.CollectionUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 

 import com.google.common.cache.Cache;
 import com.google.common.cache.CacheBuilder;
 import com.google.common.collect.Sets;
 import com.google.common.util.concurrent.AbstractService;
 import com.google.inject.Inject;
import com.google.inject.Injector;

 
 /**
  * The {@link LogSearchDataRetrievalService} is an Ambari Service that
@@ -64,9 +67,16 @@ public class LogSearchDataRetrievalService extends AbstractService {
 
   private static Logger LOG = LoggerFactory.getLogger(LogSearchDataRetrievalService.class);
 
  /**
   * Factory instance used to handle URL string generation requests on the
   *   main request thread.
   */
   @Inject
   private LoggingRequestHelperFactory loggingRequestHelperFactory;
 
  @Inject
  private Injector injector;

   /**
    * A Cache of host+component names to a set of log files associated with
    *  that Host/Component combination.  This data is retrieved from the
@@ -200,6 +210,15 @@ public class LogSearchDataRetrievalService extends AbstractService {
     this.loggingRequestHelperFactory = loggingRequestHelperFactory;
   }
 
  /**
   * Package-level setter to facilitate simpler unit testing
   *
   * @param injector
   */
  void setInjector(Injector injector) {
    this.injector = injector;
  }

   /**
    * This protected method provides a way for unit-tests to insert a
    * mock executor for simpler unit-testing.
@@ -220,7 +239,14 @@ public class LogSearchDataRetrievalService extends AbstractService {
   }
 
   private void startLogSearchFileNameRequest(String host, String component, String cluster) {
    executor.execute(new LogSearchFileNameRequestRunnable(host, component, cluster, logFileNameCache, currentRequests));
    // Create a separate instance of LoggingRequestHelperFactory for
    // each task launched, since these tasks will occur on a separate thread
    // TODO: In a future patch, this should be refactored, to either remove the need
    // TODO: for the separate factory instance at the level of this class, or to make
    // TODO: the LoggingRequestHelperFactory implementation thread-safe, so that
    // TODO: a single factory instance can be shared across multiple threads safely
    executor.execute(new LogSearchFileNameRequestRunnable(host, component, cluster, logFileNameCache, currentRequests,
                                                          injector.getInstance(LoggingRequestHelperFactory.class)));
   }
 
   private AmbariManagementController getController() {
@@ -258,8 +284,8 @@ public class LogSearchDataRetrievalService extends AbstractService {
 
     private AmbariManagementController controller;
 
    LogSearchFileNameRequestRunnable(String host, String component, String cluster, Cache<String, Set<String>> logFileNameCache, Set<String> currentRequests) {
      this(host, component, cluster, logFileNameCache, currentRequests, new LoggingRequestHelperFactoryImpl(), AmbariServer.getController());
    LogSearchFileNameRequestRunnable(String host, String component, String cluster, Cache<String, Set<String>> logFileNameCache, Set<String> currentRequests, LoggingRequestHelperFactory loggingRequestHelperFactory) {
      this(host, component, cluster, logFileNameCache, currentRequests, loggingRequestHelperFactory, AmbariServer.getController());
     }
 
     LogSearchFileNameRequestRunnable(String host, String component, String cluster, Cache<String, Set<String>> logFileNameCache, Set<String> currentRequests,
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImpl.java
index afe1757bef..2f4f8d7e6d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImpl.java
@@ -19,7 +19,10 @@ package org.apache.ambari.server.controller.logging;
 
 import java.util.List;
 
import javax.inject.Inject;

 import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
@@ -28,6 +31,7 @@ import org.apache.ambari.server.state.ServiceComponentHost;
 import org.apache.ambari.server.state.State;
 import org.apache.log4j.Logger;
 

 public class LoggingRequestHelperFactoryImpl implements LoggingRequestHelperFactory {
 
   private static final Logger LOG = Logger.getLogger(LoggingRequestHelperFactoryImpl.class);
@@ -42,9 +46,17 @@ public class LoggingRequestHelperFactoryImpl implements LoggingRequestHelperFact
 
   private static final String LOGSEARCH_UI_PROTOCOL = "logsearch_ui_protocol";
 
  @Inject
  private Configuration ambariServerConfiguration;
 
   @Override
   public LoggingRequestHelper getHelper(AmbariManagementController ambariManagementController, String clusterName) {

    if (ambariServerConfiguration == null) {
      LOG.error("Ambari Server configuration object not available, cannot create request helper");
      return null;
    }

     Clusters clusters =
       ambariManagementController.getClusters();
 
@@ -90,7 +102,11 @@ public class LoggingRequestHelperFactoryImpl implements LoggingRequestHelperFact
         final String logSearchProtocol =
           logSearchEnvConfig.getProperties().get(LOGSEARCH_UI_PROTOCOL);
 
        return new LoggingRequestHelperImpl(logSearchHostName, logSearchPortNumber, logSearchProtocol, ambariManagementController.getCredentialStoreService(), cluster);
        final LoggingRequestHelperImpl loggingRequestHelper = new LoggingRequestHelperImpl(logSearchHostName, logSearchPortNumber, logSearchProtocol, ambariManagementController.getCredentialStoreService(), cluster);
        // set configured timeouts for the Ambari connection to the LogSearch Portal service
        loggingRequestHelper.setLogSearchConnectTimeoutInMilliseconds(ambariServerConfiguration.getLogSearchPortalConnectTimeout());
        loggingRequestHelper.setLogSearchReadTimeoutInMilliseconds(ambariServerConfiguration.getLogSearchPortalReadTimeout());
        return loggingRequestHelper;
       }
     } catch (AmbariException ambariException) {
       LOG.error("Error occurred while trying to obtain the cluster, cluster name = " + clusterName, ambariException);
@@ -99,4 +115,13 @@ public class LoggingRequestHelperFactoryImpl implements LoggingRequestHelperFact
 
     return null;
   }

  /**
   * Package-level setter to facilitate simpler unit testing
   *
   * @param ambariServerConfiguration the Ambari Server configuration properties
   */
  void setAmbariServerConfiguration(Configuration ambariServerConfiguration) {
    this.ambariServerConfiguration = ambariServerConfiguration;
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java
index 88996d7c4d..e915538869 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java
@@ -55,12 +55,14 @@ import org.apache.ambari.server.state.Config;
 import org.apache.commons.codec.binary.Base64;
 import org.apache.commons.lang.StringUtils;
 import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
 import org.codehaus.jackson.map.AnnotationIntrospector;
 import org.codehaus.jackson.map.ObjectMapper;
 import org.codehaus.jackson.map.ObjectReader;
 import org.codehaus.jackson.map.annotate.JsonSerialize;
 import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
 /**
  * Convenience class to handle the connection details of a LogSearch query request.
@@ -68,7 +70,7 @@ import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
  */
 public class LoggingRequestHelperImpl implements LoggingRequestHelper {
 
  private static Logger LOG = Logger.getLogger(LoggingRequestHelperImpl.class);
  private static Logger LOG = LoggerFactory.getLogger(LoggingRequestHelperImpl.class);
 
   private static final String LOGSEARCH_ADMIN_JSON_CONFIG_TYPE_NAME = "logsearch-admin-json";
 
@@ -114,6 +116,11 @@ public class LoggingRequestHelperImpl implements LoggingRequestHelper {
 
   private SSLSocketFactory sslSocketFactory;
 
  private int logSearchConnectTimeoutInMilliseconds = DEFAULT_LOGSEARCH_CONNECT_TIMEOUT_IN_MILLISECONDS;

  private int logSearchReadTimeoutInMilliseconds = DEFAULT_LOGSEARCH_READ_TIMEOUT_IN_MILLISECONDS;


   public LoggingRequestHelperImpl(String hostName, String portNumber, String protocol, CredentialStoreService credentialStoreService, Cluster cluster) {
     this(hostName, portNumber, protocol, credentialStoreService, cluster, new DefaultNetworkConnection());
   }
@@ -127,6 +134,22 @@ public class LoggingRequestHelperImpl implements LoggingRequestHelper {
     this.networkConnection = networkConnection;
   }
 
  public int getLogSearchConnectTimeoutInMilliseconds() {
    return this.logSearchConnectTimeoutInMilliseconds;
  }

  public void setLogSearchConnectTimeoutInMilliseconds(int logSearchConnectTimeoutInMilliseconds) {
    this.logSearchConnectTimeoutInMilliseconds = logSearchConnectTimeoutInMilliseconds;
  }

  public int getLogSearchReadTimeoutInMilliseconds() {
    return this.logSearchReadTimeoutInMilliseconds;
  }

  public void setLogSearchReadTimeoutInMilliseconds(int logSearchReadTimeoutInMilliseconds) {
    this.logSearchReadTimeoutInMilliseconds = logSearchReadTimeoutInMilliseconds;
  }

   public LogQueryResponse sendQueryRequest(Map<String, String> queryParameters) {
     try {
       // use the Apache builder to create the correct URI
@@ -135,10 +158,12 @@ public class LoggingRequestHelperImpl implements LoggingRequestHelper {
       HttpURLConnection httpURLConnection  = (HttpURLConnection) logSearchURI.toURL().openConnection();
       secure(httpURLConnection, protocol);
       httpURLConnection.setRequestMethod("GET");
      httpURLConnection.setConnectTimeout(DEFAULT_LOGSEARCH_CONNECT_TIMEOUT_IN_MILLISECONDS);
      httpURLConnection.setReadTimeout(DEFAULT_LOGSEARCH_READ_TIMEOUT_IN_MILLISECONDS);
      httpURLConnection.setConnectTimeout(logSearchConnectTimeoutInMilliseconds);
      httpURLConnection.setReadTimeout(logSearchReadTimeoutInMilliseconds);
 
       addCookiesFromCookieStore(httpURLConnection);
      LOG.debug("Attempting request to LogSearch Portal Server, with connect timeout = {} milliseconds and read timeout = {} milliseconds",
        logSearchConnectTimeoutInMilliseconds, logSearchReadTimeoutInMilliseconds);
 
       setupCredentials(httpURLConnection);
 
@@ -282,7 +307,7 @@ public class LoggingRequestHelperImpl implements LoggingRequestHelper {
     queryParameters.put("pageSize", "1");
 
     LogQueryResponse response = sendQueryRequest(queryParameters);
    if ((response != null) && (!response.getListOfResults().isEmpty())) {
    if ((response != null) && (response.getListOfResults() != null) && (!response.getListOfResults().isEmpty())) {
       LogLineResult lineOne = response.getListOfResults().get(0);
       // this assumes that each component has only one associated log file,
       // which may not always hold true
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java
index 6ffcdf9025..36c485a525 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java
@@ -43,13 +43,14 @@ import org.apache.ambari.server.state.ComponentInfo;
 import org.apache.ambari.server.state.LogDefinition;
 import org.apache.ambari.server.state.StackId;
 import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 import com.google.inject.Inject;
 
 public class LoggingSearchPropertyProvider implements PropertyProvider {
 
  private static final Logger LOG = Logger.getLogger(LoggingSearchPropertyProvider.class);
  private static final Logger LOG = LoggerFactory.getLogger(LoggingSearchPropertyProvider.class);
 
   private static final String CLUSTERS_PATH = "/api/v1/clusters";
 
@@ -68,12 +69,9 @@ public class LoggingSearchPropertyProvider implements PropertyProvider {
   @Inject
   private LogSearchDataRetrievalService logSearchDataRetrievalService;
 
  @Inject
   private LoggingRequestHelperFactory loggingRequestHelperFactory;
   
  public LoggingSearchPropertyProvider() {
    loggingRequestHelperFactory = new LoggingRequestHelperFactoryImpl();
  }

   @Override
   public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {
     Map<String, Boolean> isLogSearchRunning = new HashMap<>();
@@ -223,11 +221,11 @@ public class LoggingSearchPropertyProvider implements PropertyProvider {
     this.ambariManagementController = ambariManagementController;
   }
 
  protected void setLogSearchDataRetrievalService(LogSearchDataRetrievalService logSearchDataRetrievalService) {
  void setLogSearchDataRetrievalService(LogSearchDataRetrievalService logSearchDataRetrievalService) {
     this.logSearchDataRetrievalService = logSearchDataRetrievalService;
   }
 
  protected void setLoggingRequestHelperFactory(LoggingRequestHelperFactory loggingRequestHelperFactory) {
  void setLoggingRequestHelperFactory(LoggingRequestHelperFactory loggingRequestHelperFactory) {
     this.loggingRequestHelperFactory = loggingRequestHelperFactory;
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/Utils.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/Utils.java
index fdc9267ea0..969d4428aa 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/Utils.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/Utils.java
@@ -19,7 +19,7 @@ package org.apache.ambari.server.controller.logging;
 
 import java.util.concurrent.atomic.AtomicInteger;
 
import org.apache.log4j.Logger;
import org.slf4j.Logger;
 
 /**
  * Utility class to hold static convenience methods for
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/api/services/LoggingServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/api/services/LoggingServiceTest.java
index 64fff1e891..800aca2194 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/api/services/LoggingServiceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/api/services/LoggingServiceTest.java
@@ -125,7 +125,8 @@ public class LoggingServiceTest {
     SecurityContextHolder.getContext().setAuthentication(authentication);
 
     LoggingService loggingService =
      new LoggingService(expectedClusterName, controllerFactoryMock, helperFactoryMock);
      new LoggingService(expectedClusterName, controllerFactoryMock);
    loggingService.setLoggingRequestHelperFactory(helperFactoryMock);
 
     Response resource = loggingService.getSearchEngine("", null, uriInfoMock);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalServiceTest.java
index 0bd681b14d..59aa9c85fa 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalServiceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalServiceTest.java
@@ -28,6 +28,16 @@ import java.util.Collections;
 import java.util.Set;
 import java.util.concurrent.Executor;
 

import org.apache.ambari.server.controller.AmbariManagementController;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.google.inject.Injector;



 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.easymock.EasyMockSupport;
 import org.junit.Test;
@@ -117,15 +127,21 @@ public class LogSearchDataRetrievalServiceTest {
 
     Executor executorMock = mockSupport.createMock(Executor.class);
 
    Injector injectorMock =
      mockSupport.createMock(Injector.class);

     // expect the executor to be called to execute the LogSearch request
     executorMock.execute(isA(LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable.class));
     // executor should only be called once
     expectLastCall().once();
 
    expect(injectorMock.getInstance(LoggingRequestHelperFactory.class)).andReturn(helperFactoryMock);

     mockSupport.replayAll();
 
     LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
     retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setInjector(injectorMock);
     // call the initialization routine called by the Google framework
     retrievalService.doStart();
     retrievalService.setExecutor(executorMock);
@@ -137,6 +153,7 @@ public class LogSearchDataRetrievalServiceTest {
 
     assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);
     assertEquals("Incorrect number of entries in the current request set", 1, retrievalService.getCurrentRequests().size());

     assertTrue("Incorrect HostComponent set on request set",
                 retrievalService.getCurrentRequests().contains(expectedComponentName + "+" + expectedHostName));
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImplTest.java
index 7c8405d488..71a2d4974e 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperFactoryImplTest.java
@@ -19,6 +19,7 @@
 package org.apache.ambari.server.controller.logging;
 
 import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertNull;
 import static org.junit.Assert.assertTrue;
@@ -27,6 +28,7 @@ import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.security.encryption.CredentialStoreService;
 import org.apache.ambari.server.state.Cluster;
@@ -38,6 +40,8 @@ import org.apache.ambari.server.state.State;
 import org.easymock.EasyMockSupport;
 import org.junit.Test;
 


 public class LoggingRequestHelperFactoryImplTest {
 
   @Test
@@ -45,6 +49,8 @@ public class LoggingRequestHelperFactoryImplTest {
     final String expectedClusterName = "testclusterone";
     final String expectedHostName = "c6410.ambari.apache.org";
     final String expectedPortNumber = "61889";
    final int expectedConnectTimeout = 3000;
    final int expectedReadTimeout = 3000;
 
     EasyMockSupport mockSupport = new EasyMockSupport();
 
@@ -66,6 +72,9 @@ public class LoggingRequestHelperFactoryImplTest {
     CredentialStoreService credentialStoreServiceMock =
       mockSupport.createMock(CredentialStoreService.class);
 
    Configuration serverConfigMock =
      mockSupport.createMock(Configuration.class);

     Map<String, String> testProperties =
       new HashMap<String, String>();
     testProperties.put("logsearch_ui_port", expectedPortNumber);
@@ -79,13 +88,17 @@ public class LoggingRequestHelperFactoryImplTest {
     expect(logSearchEnvConfig.getProperties()).andReturn(testProperties).atLeastOnce();
     expect(serviceComponentHostMock.getHostName()).andReturn(expectedHostName).atLeastOnce();
     expect(serviceComponentHostMock.getState()).andReturn(State.STARTED).atLeastOnce();

    expect(serverConfigMock.getLogSearchPortalConnectTimeout()).andReturn(expectedConnectTimeout);
    expect(serverConfigMock.getLogSearchPortalReadTimeout()).andReturn(expectedReadTimeout);
 
     mockSupport.replayAll();
 
     LoggingRequestHelperFactory helperFactory =
       new LoggingRequestHelperFactoryImpl();
 
    // set the configuration mock using the concrete type
    ((LoggingRequestHelperFactoryImpl)helperFactory).setAmbariServerConfiguration(serverConfigMock);

     LoggingRequestHelper helper =
       helperFactory.getHelper(controllerMock, expectedClusterName);
 
@@ -95,6 +108,12 @@ public class LoggingRequestHelperFactoryImplTest {
     assertTrue("Helper created was not of the expected type",
       helper instanceof LoggingRequestHelperImpl);
 
    assertEquals("Helper factory did not set the expected connect timeout on the helper instance",
      expectedConnectTimeout, ((LoggingRequestHelperImpl)helper).getLogSearchConnectTimeoutInMilliseconds());

    assertEquals("Helper factory did not set the expected read timeout on the helper instance",
      expectedReadTimeout, ((LoggingRequestHelperImpl)helper).getLogSearchReadTimeoutInMilliseconds());

     mockSupport.verifyAll();
   }
 
@@ -121,6 +140,9 @@ public class LoggingRequestHelperFactoryImplTest {
     ServiceComponentHost serviceComponentHostMock =
       mockSupport.createMock(ServiceComponentHost.class);
 
    Configuration serverConfigMock =
      mockSupport.createMock(Configuration.class);

     Map<String, String> testProperties =
       new HashMap<String, String>();
     testProperties.put("logsearch_ui_port", expectedPortNumber);
@@ -141,6 +163,9 @@ public class LoggingRequestHelperFactoryImplTest {
     LoggingRequestHelperFactory helperFactory =
       new LoggingRequestHelperFactoryImpl();
 
    // set the configuration mock using the concrete type
    ((LoggingRequestHelperFactoryImpl)helperFactory).setAmbariServerConfiguration(serverConfigMock);

     LoggingRequestHelper helper =
       helperFactory.getHelper(controllerMock, expectedClusterName);
 
@@ -168,6 +193,9 @@ public class LoggingRequestHelperFactoryImplTest {
     Config logSearchEnvConfig =
       mockSupport.createMock(Config.class);
 
    Configuration serverConfigMock =
      mockSupport.createMock(Configuration.class);

     expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
     expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
     expect(clusterMock.getDesiredConfigByType("logsearch-env")).andReturn(logSearchEnvConfig).atLeastOnce();
@@ -179,6 +207,9 @@ public class LoggingRequestHelperFactoryImplTest {
     LoggingRequestHelperFactory helperFactory =
       new LoggingRequestHelperFactoryImpl();
 
    // set the configuration mock using the concrete type
    ((LoggingRequestHelperFactoryImpl)helperFactory).setAmbariServerConfiguration(serverConfigMock);

     LoggingRequestHelper helper =
       helperFactory.getHelper(controllerMock, expectedClusterName);
 
@@ -203,6 +234,9 @@ public class LoggingRequestHelperFactoryImplTest {
     Cluster clusterMock =
       mockSupport.createMock(Cluster.class);
 
    Configuration serverConfigMock =
      mockSupport.createMock(Configuration.class);

     expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
     expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
     // do not include LOGSEARCH in this map, to simulate the case when LogSearch is not deployed
@@ -213,6 +247,36 @@ public class LoggingRequestHelperFactoryImplTest {
     LoggingRequestHelperFactory helperFactory =
       new LoggingRequestHelperFactoryImpl();
 
    // set the configuration mock using the concrete type
    ((LoggingRequestHelperFactoryImpl)helperFactory).setAmbariServerConfiguration(serverConfigMock);

    LoggingRequestHelper helper =
      helperFactory.getHelper(controllerMock, expectedClusterName);

    assertNull("LoggingRequestHelper object returned by the factory should have been null",
      helper);

    mockSupport.verifyAll();
  }

  @Test
  public void testHelperCreationWithNoAmbariServerConfiguration() throws Exception {
    final String expectedClusterName = "testclusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    mockSupport.replayAll();

    LoggingRequestHelperFactory helperFactory =
      new LoggingRequestHelperFactoryImpl();

    // set the configuration mock using the concrete type
    // set the configuration object to null, to simulate an error in dependency injection
    ((LoggingRequestHelperFactoryImpl)helperFactory).setAmbariServerConfiguration(null);

     LoggingRequestHelper helper =
       helperFactory.getHelper(controllerMock, expectedClusterName);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImplTest.java
index 3129f6e18a..654c02dde0 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImplTest.java
@@ -105,6 +105,9 @@ public class LoggingRequestHelperImplTest {
       "{\"name\":\"WARN\",\"value\":\"41\"},{\"name\":\"INFO\",\"value\":\"186\"},{\"name\":\"DEBUG\",\"value\":\"0\"}," +
       "{\"name\":\"TRACE\",\"value\":\"0\"}]}";
 
  private static final String TEST_JSON_INPUT_NULL_LOG_LIST =
    "{\"startIndex\":0,\"pageSize\":0,\"totalCount\":0,\"resultSize\":0,\"sortType\":null,\"sortBy\":null,\"queryTimeMS\":1479850014987,\"logList\":null,\"listSize\":0}";

 
   private final String EXPECTED_HOST_NAME = "c6401.ambari.apache.org";
 
@@ -471,6 +474,87 @@ public class LoggingRequestHelperImplTest {
     assertEquals("Response did not include the expected file name",
       "/var/log/hadoop/hdfs/hadoop-hdfs-namenode-c6401.ambari.apache.org.log",
       result.iterator().next());

    mockSupport.verifyAll();
  }

  @Test
  public void testLogFileNameRequestWithNullLogList() throws Exception {
    final String expectedComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    LoggingRequestHelperImpl.NetworkConnection networkConnectionMock =
      mockSupport.createMock(LoggingRequestHelperImpl.NetworkConnection.class);

    Config adminPropertiesConfigMock =
      mockSupport.createMock(Config.class);

    Map<String, String> testConfigProperties =
      new HashMap<String, String>();
    testConfigProperties.put("logsearch_admin_username", "admin-user");
    testConfigProperties.put("logsearch_admin_password", "admin-pwd");
    testConfigProperties = Collections.unmodifiableMap(testConfigProperties);

    Capture<HttpURLConnection> captureURLConnection = new Capture<HttpURLConnection>();
    Capture<HttpURLConnection> captureURLConnectionForAuthentication = new Capture<HttpURLConnection>();

    expect(clusterMock.getDesiredConfigByType("logsearch-admin-json")).andReturn(adminPropertiesConfigMock).atLeastOnce();
    expect(adminPropertiesConfigMock.getProperties()).andReturn(testConfigProperties).atLeastOnce();
    expect(networkConnectionMock.readQueryResponseFromServer(capture(captureURLConnection))).andReturn(new StringBuffer(TEST_JSON_INPUT_NULL_LOG_LIST)).atLeastOnce();

    // expect that basic authentication is setup, with the expected encoded credentials
    networkConnectionMock.setupBasicAuthentication(capture(captureURLConnectionForAuthentication), eq(EXPECTED_ENCODED_CREDENTIALS));

    mockSupport.replayAll();

    LoggingRequestHelper helper =
      new LoggingRequestHelperImpl(EXPECTED_HOST_NAME, EXPECTED_PORT_NUMBER, EXPECTED_PROTOCOL, credentialStoreServiceMock, clusterMock, networkConnectionMock);

    // invoke query request
    Set<String> result =
      helper.sendGetLogFileNamesRequest(expectedComponentName, EXPECTED_HOST_NAME);

    // verify that the HttpURLConnection was created with the propert values
    HttpURLConnection httpURLConnection =
      captureURLConnection.getValue();

    assertEquals("URLConnection did not have the correct hostname information",
      EXPECTED_HOST_NAME, httpURLConnection.getURL().getHost());
    assertEquals("URLConnection did not have the correct port information",
      EXPECTED_PORT_NUMBER, httpURLConnection.getURL().getPort() + "");
    assertEquals("URLConnection did not have the expected http protocol scheme",
      "http", httpURLConnection.getURL().getProtocol());
    assertEquals("URLConnection did not have the expected method set",
      "GET", httpURLConnection.getRequestMethod());

    assertSame("HttpUrlConnection instances passed into NetworkConnection mock should have been the same instance",
      httpURLConnection, captureURLConnectionForAuthentication.getValue());

    final String resultQuery =
      httpURLConnection.getURL().getQuery();

    // verify that the query contains the three required parameters
    assertTrue("host_name parameter was not included in query",
      resultQuery.contains("host_name=c6401.ambari.apache.org"));
    assertTrue("component_name parameter was not included in the query",
      resultQuery.contains("component_name=" + expectedComponentName));
    assertTrue("pageSize parameter was not included in query",
      resultQuery.contains("pageSize=1"));

    assertNotNull("Response object should not be null",
      result);
    assertEquals("Response Set was not of the expected size, expected an empty set",
      0, result.size());

    mockSupport.verifyAll();
   }
 
   /**
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/UtilsTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/UtilsTest.java
index 63b46ac6f7..60f4725b07 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/UtilsTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/UtilsTest.java
@@ -24,10 +24,10 @@ import static org.junit.Assert.assertSame;
 
 import java.util.concurrent.atomic.AtomicInteger;
 
import org.apache.log4j.Logger;
 import org.easymock.Capture;
 import org.easymock.EasyMockSupport;
 import org.junit.Test;
import org.slf4j.Logger;
 
 public class UtilsTest {
 
- 
2.19.1.windows.1

