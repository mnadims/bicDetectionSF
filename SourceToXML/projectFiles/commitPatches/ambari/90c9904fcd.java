From 90c9904fcdeaf5a970cdd837a79144c9d18fd4e9 Mon Sep 17 00:00:00 2001
From: Bob Nettleton <rnettleton@hortonworks.com>
Date: Fri, 1 Jul 2016 14:00:27 -0400
Subject: [PATCH] AMBARI-17510. LogSearch REST Integration component can cause
 performance issues. (rnettleton)

--
 .../AmbariManagementController.java           |   8 +
 .../AmbariManagementControllerImpl.java       |   6 +
 .../internal/AbstractProviderModule.java      |   8 +-
 .../LogSearchDataRetrievalService.java        | 231 ++++++++++++++++++
 .../logging/LoggingRequestHelperImpl.java     |   7 +
 .../LoggingSearchPropertyProvider.java        |  55 ++---
 .../LoggingSearchPropertyProviderTest.java    |  79 ++----
 7 files changed, 300 insertions(+), 94 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
index 947a9f4631..5cf2de7b95 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
@@ -29,6 +29,7 @@ import org.apache.ambari.server.actionmanager.ActionManager;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.logging.LoggingSearchPropertyProvider;
 import org.apache.ambari.server.controller.metrics.MetricPropertyProviderFactory;
 import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
 import org.apache.ambari.server.events.AmbariEvent;
@@ -859,6 +860,13 @@ public interface AmbariManagementController {
    */
   MetricPropertyProviderFactory getMetricPropertyProviderFactory();
 
  /**
   * Gets the LoggingSearchPropertyProvider instance.
   *
   * @return the injected {@link LoggingSearchPropertyProvider}
   */
  LoggingSearchPropertyProvider getLoggingSearchPropertyProvider();

   /**
    * Returns KerberosHelper instance
    * @return
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index fe7e757ae8..5b4dae2140 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -95,6 +95,7 @@ import org.apache.ambari.server.controller.internal.RequestStageContainer;
 import org.apache.ambari.server.controller.internal.URLStreamProvider;
 import org.apache.ambari.server.controller.internal.WidgetLayoutResourceProvider;
 import org.apache.ambari.server.controller.internal.WidgetResourceProvider;
import org.apache.ambari.server.controller.logging.LoggingSearchPropertyProvider;
 import org.apache.ambari.server.controller.metrics.MetricPropertyProviderFactory;
 import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
 import org.apache.ambari.server.controller.spi.Resource;
@@ -4872,6 +4873,11 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
     return injector.getInstance(MetricPropertyProviderFactory.class);
   }
 
  @Override
  public LoggingSearchPropertyProvider getLoggingSearchPropertyProvider() {
    return injector.getInstance(LoggingSearchPropertyProvider.class);
  }

   /**
    * {@inheritDoc}
    */
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java
index 5ac66d8ef3..75d8449ffe 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java
@@ -33,6 +33,7 @@ import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 
import com.google.inject.Injector;
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.Role;
 import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
@@ -237,6 +238,7 @@ public abstract class AbstractProviderModule implements ProviderModule,
   @Inject
   protected AmbariEventPublisher eventPublisher;
 

   /**
    * The map of host components.
    */
@@ -857,8 +859,10 @@ public abstract class AbstractProviderModule implements ProviderModule,
             PropertyHelper.getPropertyId("HostRoles", "component_name"),
             HTTP_PROPERTY_REQUESTS));
 
          //TODO, this may need to be conditional based on the presence/absence of LogSearch
          providers.add(new LoggingSearchPropertyProvider());
          // injecting the Injector type won't seem to work in this module, so
          // this follows the current pattern of relying on the management controller
          // to instantiate this PropertyProvider
          providers.add(managementController.getLoggingSearchPropertyProvider());
         }
         break;
         case RootServiceComponent:
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java
new file mode 100644
index 0000000000..877f4e3b26
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LogSearchDataRetrievalService.java
@@ -0,0 +1,231 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.logging;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@link LogSearchDataRetrievalService} is an Ambari Service that
 *   is used by the Ambari LogSearch integration code to obtain response
 *   data from the LogSearch server.
 *
 * In order to improve the performance of the LogSearch integration layer in
 *   Ambari, this service implements the following:
 *
 *  <ul>
 *    <li>A cache for LogSearch data that typically is returned by the LogSearch REST API</li>
 *    <li>Implements the remote request for LogSearch data not found in the cache on a separate
 *        thread, which keeps the request from affecting the overall performance of the
 *        Ambari REST API</li>
 *  </ul>
 *
 *  As with other services annotated with {@link AmbariService}, this class may be
 *    injected in order to obtain cached access to the LogSearch responses.
 *
 *  Caches are initially empty in this implementation, and a remote request
 *    to the LogSearch server will be made upon the first request for a given
 *    response.
 *
 *
 */
@AmbariService
public class LogSearchDataRetrievalService extends AbstractService {

  private static Logger LOG = LoggerFactory.getLogger(LogSearchDataRetrievalService.class);

  @Inject
  private Configuration configuration;

  /**
   * A Cache of host+component names to a set of log files associated with
   *  that Host/Component combination.  This data is retrieved from the
   *  LogSearch server, but cached here for better performance.
   */
  private Cache<String, Set<String>> logFileNameCache;

  /**
   * a Cache of host+component names to a generated URI that
   *  can be used to access the "tail" of a given log file.
   *
   * This data is generated by ambari-server, but cached here to
   *  avoid re-creating these strings upon multiple calls to the
   *  associated HostComponent resource.
   */
  private Cache<String, String> logFileTailURICache;

  /**
   * Executor instance to be used to run REST queries against
   * the LogSearch service.
   */
  private Executor executor;

  @Override
  protected void doStart() {

    LOG.debug("Initializing caches");
    // initialize the log file name cache
    logFileNameCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
    // initialize the log file tail URI cache
    logFileTailURICache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

    // initialize the Executor
    executor = Executors.newSingleThreadExecutor();
  }

  @Override
  protected void doStop() {
    LOG.debug("Invalidating LogSearch caches");
    // invalidate the cache
    logFileNameCache.invalidateAll();

    logFileTailURICache.invalidateAll();
  }

  /**
   * This method attempts to obtain the log file names for the specified component
   *   on the specified host.  A cache lookup is first attempted. If the cache does not contain
   *   this data, an asynchronous task is launched in order to make the REST request to
   *   the LogSearch server to obtain this data.
   *
   * Once the data is available in the cache, subsequent calls for a given Host/Component
   *   combination should return non-null.
   *
   * @param component the component name
   * @param host the host name
   * @param cluster the cluster name
   *
   * @return a Set<String> that includes the log file names associated with this Host/Component
   *         combination, or null if that object does not exist in the cache.
   */
  public Set<String> getLogFileNames(String component, String host, String cluster) {
    String key = generateKey(component, host);

    // check cache for data
    Set<String> cacheResult =
      logFileNameCache.getIfPresent(key);

    if (cacheResult != null) {
      LOG.debug("LogFileNames result for key = {} found in cache", key);
      return cacheResult;
    } else {
      // queue up a thread to make the LogSearch REST request to obtain this information
      LOG.debug("LogFileNames result for key = {} not in cache, queueing up remote request", key);
      startLogSearchFileNameRequest(host, component, cluster);
    }

    return null;
  }

  public String getLogFileTailURI(String baseURI, String component, String host, String cluster) {
    String key = generateKey(component, host);

    String result = logFileTailURICache.getIfPresent(key);
    if (result != null) {
      // return cached result
      return result;
    } else {
      // create URI and add to cache before returning
      LoggingRequestHelper helper =
        new LoggingRequestHelperFactoryImpl().getHelper(getController(), cluster);
      String tailFileURI =
        helper.createLogFileTailURI(baseURI, component, host);

      if (tailFileURI != null) {
        logFileTailURICache.put(key, tailFileURI);
        return tailFileURI;
      }
    }

    return null;
  }

  private void startLogSearchFileNameRequest(String host, String component, String cluster) {
    executor.execute(new LogSearchFileNameRequestRunnable(host, component, cluster));
  }

  private AmbariManagementController getController() {
    return AmbariServer.getController();
  }



  private static String generateKey(String component, String host) {
    return component + "+" + host;
  }


  /**
   * A {@link Runnable} used to make requests to the remote LogSearch server's
   *   REST API.
   *
   * This implementation will update a cache shared with the {@link LogSearchDataRetrievalService},
   *   which can then be used for subsequent requests for the same data.
   *
   */
  private class LogSearchFileNameRequestRunnable implements Runnable {

    private final String host;

    private final String component;

    private final String cluster;

    private LogSearchFileNameRequestRunnable(String host, String component, String cluster) {
      this.host = host;
      this.component = component;
      this.cluster = cluster;
    }

    @Override
    public void run() {
      LOG.debug("LogSearchFileNameRequestRunnable: starting...");
      LoggingRequestHelper helper =
        new LoggingRequestHelperFactoryImpl().getHelper(getController(), cluster);

      if (helper != null) {
        // make request to LogSearch service
        Set<String> logFileNamesResult =
          helper.sendGetLogFileNamesRequest(component, host);

        // update the cache if result is available
        if (logFileNamesResult != null) {
          LOG.debug("LogSearchFileNameRequestRunnable: request was successful, updating cache");
          logFileNameCache.put(generateKey(component, host), logFileNamesResult);
        } else {
          LOG.debug("LogSearchFileNameRequestRunnable: remote request was not successful");
        }
      } else {
        LOG.debug("LogSearchFileNameRequestRunnable: request helper was null.  This may mean that LogSearch is not available, or could be a potential connection problem.");
      }
    }
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java
index d8c71e2465..276a65e1ce 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingRequestHelperImpl.java
@@ -76,6 +76,10 @@ public class LoggingRequestHelperImpl implements LoggingRequestHelper {
 
   private static final String PAGE_SIZE_QUERY_PARAMETER_NAME = "pageSize";
 
  private static final int DEFAULT_LOGSEARCH_CONNECT_TIMEOUT_IN_MILLISECONDS = 5000;

  private static final int DEFAULT_LOGSEARCH_READ_TIMEOUT_IN_MILLISECONDS = 5000;

   private static AtomicInteger errorLogCounterForLogSearchConnectionExceptions = new AtomicInteger(0);
 
   private final String hostName;
@@ -108,6 +112,9 @@ public class LoggingRequestHelperImpl implements LoggingRequestHelper {
 
       HttpURLConnection httpURLConnection  = (HttpURLConnection)logSearchURI.toURL().openConnection();
       httpURLConnection.setRequestMethod("GET");
      httpURLConnection.setConnectTimeout(DEFAULT_LOGSEARCH_CONNECT_TIMEOUT_IN_MILLISECONDS);
      httpURLConnection.setReadTimeout(DEFAULT_LOGSEARCH_READ_TIMEOUT_IN_MILLISECONDS);

 
       setupCredentials(httpURLConnection);
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java
index ff7e7f5a96..a28e04acd8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProvider.java
@@ -17,10 +17,10 @@
  */
 package org.apache.ambari.server.controller.logging;
 
import com.google.inject.Inject;
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
 import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.PropertyProvider;
 import org.apache.ambari.server.controller.spi.Request;
@@ -48,25 +48,18 @@ public class LoggingSearchPropertyProvider implements PropertyProvider {
 
   private static AtomicInteger errorLogCounterForLogSearchConnectionExceptions = new AtomicInteger(0);
 
  private final LoggingRequestHelperFactory requestHelperFactory;
  @Inject
  private AmbariManagementController ambariManagementController;
 
  private final ControllerFactory controllerFactory;
  @Inject
  private LogSearchDataRetrievalService logSearchDataRetrievalService;
 
   public LoggingSearchPropertyProvider() {
    this(new LoggingRequestHelperFactoryImpl(), new DefaultControllerFactory());
  }

  protected LoggingSearchPropertyProvider(LoggingRequestHelperFactory requestHelperFactory, ControllerFactory controllerFactory) {
    this.requestHelperFactory = requestHelperFactory;
    this.controllerFactory = controllerFactory;
   }
 
   @Override
   public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {
 
    AmbariManagementController controller =
      controllerFactory.getAmbariManagementController();

     for (Resource resource : resources) {
       // obtain the required identifying properties on the host component resource
       final String componentName = (String)resource.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "component_name"));
@@ -75,25 +68,18 @@ public class LoggingSearchPropertyProvider implements PropertyProvider {
 
       // query the stack definitions to find the correct component name (stack name mapped to LogSearch-defined name)
       final String mappedComponentNameForLogSearch =
        getMappedComponentNameForSearch(clusterName, componentName, controller);
        getMappedComponentNameForSearch(clusterName, componentName, ambariManagementController);
 
       if (mappedComponentNameForLogSearch != null) {
         HostComponentLoggingInfo loggingInfo =
           new HostComponentLoggingInfo();
 
        // make query to LogSearch server to find the associated file names
        // create helper instance using factory
        LoggingRequestHelper requestHelper =
          requestHelperFactory.getHelper(controller, clusterName);
 
         // if LogSearch service is available
        if (requestHelper != null) {
        if (logSearchDataRetrievalService != null) {
           // send query to obtain logging metadata
           Set<String> logFileNames =
            requestHelper.sendGetLogFileNamesRequest(mappedComponentNameForLogSearch, hostName);

          LogLevelQueryResponse levelQueryResponse =
            requestHelper.sendLogLevelQueryRequest(mappedComponentNameForLogSearch, hostName);
            logSearchDataRetrievalService.getLogFileNames(mappedComponentNameForLogSearch, hostName, clusterName);
 
           if ((logFileNames != null) && (!logFileNames.isEmpty())) {
             loggingInfo.setComponentName(mappedComponentNameForLogSearch);
@@ -102,19 +88,14 @@ public class LoggingSearchPropertyProvider implements PropertyProvider {
 
             for (String fileName : logFileNames) {
               // generate the URIs that can be used by clients to obtain search results/tail log results/etc
              final String searchEngineURI = controller.getAmbariServerURI(getFullPathToSearchEngine(clusterName));
              final String logFileTailURI = requestHelper.createLogFileTailURI(searchEngineURI, mappedComponentNameForLogSearch, hostName);
              final String searchEngineURI = ambariManagementController.getAmbariServerURI(getFullPathToSearchEngine(clusterName));
              final String logFileTailURI = logSearchDataRetrievalService.getLogFileTailURI(searchEngineURI, mappedComponentNameForLogSearch, hostName, clusterName);
               // all log files are assumed to be service types for now
               listOfFileDefinitions.add(new LogFileDefinitionInfo(fileName, LogFileType.SERVICE, searchEngineURI, logFileTailURI));
             }
 
             loggingInfo.setListOfLogFileDefinitions(listOfFileDefinitions);
 
            // add the log levels for this host component to the logging structure
            if (levelQueryResponse != null) {
              loggingInfo.setListOfLogLevels(levelQueryResponse.getNameValueList());
            }

             LOG.debug("Adding logging info for component name = " + componentName + " on host name = " + hostName);
             // add the logging metadata for this host component
             resource.setProperty("logging", loggingInfo);
@@ -170,18 +151,12 @@ public class LoggingSearchPropertyProvider implements PropertyProvider {
     return Collections.emptySet();
   }
 
  /**
   * Internal interface used to control how the AmbariManagementController
   * instance is obtained.  This is useful for unit testing as well.
   */
  interface ControllerFactory {
    AmbariManagementController getAmbariManagementController();
  protected void setAmbariManagementController(AmbariManagementController ambariManagementController) {
    this.ambariManagementController = ambariManagementController;
   }
 
  private static class DefaultControllerFactory implements ControllerFactory {
    @Override
    public AmbariManagementController getAmbariManagementController() {
      return AmbariServer.getController();
    }
  protected void setLogSearchDataRetrievalService(LogSearchDataRetrievalService logSearchDataRetrievalService) {
    this.logSearchDataRetrievalService = logSearchDataRetrievalService;
   }

 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProviderTest.java
index 593f6609a9..8b71b65cf9 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/logging/LoggingSearchPropertyProviderTest.java
@@ -20,7 +20,6 @@ package org.apache.ambari.server.controller.logging;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
@@ -44,6 +43,7 @@ import static org.easymock.EasyMock.expect;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
 import static org.junit.Assert.assertSame;
 
 /**
@@ -101,12 +101,6 @@ public class LoggingSearchPropertyProviderTest {
     // expect set method to be called
     resourceMock.setProperty(eq("logging"), capture(captureLogInfo));
 
    LoggingRequestHelperFactory helperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    LoggingRequestHelper helperMock =
      mockSupport.createMock(LoggingRequestHelper.class);

     LogLevelQueryResponse levelQueryResponse =
       new LogLevelQueryResponse();
 
@@ -120,20 +114,12 @@ public class LoggingSearchPropertyProviderTest {
 
     levelQueryResponse.setNameValueList(testListOfLogLevels);
 

    expect(helperMock.sendGetLogFileNamesRequest(expectedLogSearchComponentName, "c6401.ambari.apache.org")).andReturn(Collections.singleton(expectedLogFilePath));
    expect(helperMock.sendLogLevelQueryRequest(expectedLogSearchComponentName,"c6401.ambari.apache.org")).andReturn(levelQueryResponse).atLeastOnce();
    expect(helperMock.createLogFileTailURI(expectedAmbariURL + expectedSearchEnginePath, expectedLogSearchComponentName, "c6401.ambari.apache.org")).andReturn("").atLeastOnce();

     Request requestMock =
       mockSupport.createMock(Request.class);
 
     Predicate predicateMock =
       mockSupport.createMock(Predicate.class);
 
    LoggingSearchPropertyProvider.ControllerFactory factoryMock =
      mockSupport.createMock(LoggingSearchPropertyProvider.ControllerFactory.class);

     AmbariManagementController controllerMock =
       mockSupport.createMock(AmbariManagementController.class);
 
@@ -155,7 +141,13 @@ public class LoggingSearchPropertyProviderTest {
     LogDefinition logDefinitionMock =
       mockSupport.createMock(LogDefinition.class);
 
    expect(factoryMock.getAmbariManagementController()).andReturn(controllerMock);
    LogSearchDataRetrievalService dataRetrievalServiceMock =
      mockSupport.createMock(LogSearchDataRetrievalService.class);

    expect(dataRetrievalServiceMock.getLogFileNames(expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(Collections.singleton(expectedLogFilePath)).atLeastOnce();
    expect(dataRetrievalServiceMock.getLogFileTailURI(expectedAmbariURL + expectedSearchEnginePath, expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn("").atLeastOnce();


     expect(controllerMock.getAmbariServerURI(expectedSearchEnginePath)).
       andReturn(expectedAmbariURL + expectedSearchEnginePath).atLeastOnce();
     expect(controllerMock.getAmbariMetaInfo()).andReturn(metaInfoMock).atLeastOnce();
@@ -171,12 +163,13 @@ public class LoggingSearchPropertyProviderTest {
     expect(componentInfoMock.getLogs()).andReturn(Collections.singletonList(logDefinitionMock)).atLeastOnce();
     expect(logDefinitionMock.getLogId()).andReturn(expectedLogSearchComponentName).atLeastOnce();
 
    expect(helperFactoryMock.getHelper(controllerMock, "clusterone")).andReturn(helperMock).atLeastOnce();

     mockSupport.replayAll();
 
    PropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider(helperFactoryMock, factoryMock);
    LoggingSearchPropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider();

    propertyProvider.setAmbariManagementController(controllerMock);
    propertyProvider.setLogSearchDataRetrievalService(dataRetrievalServiceMock);
 
 
     Set<Resource> returnedResources =
@@ -210,32 +203,9 @@ public class LoggingSearchPropertyProviderTest {
     assertEquals("Incorrect URL path to searchEngine",
       expectedAmbariURL + expectedSearchEnginePath, definitionInfo.getSearchEngineURL());
 

     // verify that the log level count information
    // was properly added to the HostComponent resource
    assertNotNull("LogLevel counts should not be null",
      returnedLogInfo.getListOfLogLevels());
    assertEquals("LogLevel counts were of an incorrect size",
      3, returnedLogInfo.getListOfLogLevels().size());

    List<NameValuePair> returnedLevelList =
      returnedLogInfo.getListOfLogLevels();

    assertEquals("NameValue name for log level was incorrect",
      "ERROR", returnedLevelList.get(0).getName());
    assertEquals("NameValue name for log level was incorrect",
      "150", returnedLevelList.get(0).getValue());

    assertEquals("NameValue name for log level was incorrect",
      "WARN", returnedLevelList.get(1).getName());
    assertEquals("NameValue name for log level was incorrect",
      "500", returnedLevelList.get(1).getValue());

    assertEquals("NameValue name for log level was incorrect",
      "INFO", returnedLevelList.get(2).getName());
    assertEquals("NameValue name for log level was incorrect",
      "2200", returnedLevelList.get(2).getValue());

    // was not added to the HostComponent resource
    assertNull(returnedLogInfo.getListOfLogLevels());
 
     mockSupport.verifyAll();
   }
@@ -273,9 +243,6 @@ public class LoggingSearchPropertyProviderTest {
     Predicate predicateMock =
       mockSupport.createMock(Predicate.class);
 
    LoggingSearchPropertyProvider.ControllerFactory factoryMock =
      mockSupport.createMock(LoggingSearchPropertyProvider.ControllerFactory.class);

     AmbariManagementController controllerMock =
       mockSupport.createMock(AmbariManagementController.class);
 
@@ -297,7 +264,9 @@ public class LoggingSearchPropertyProviderTest {
     LogDefinition logDefinitionMock =
       mockSupport.createMock(LogDefinition.class);
 
    expect(factoryMock.getAmbariManagementController()).andReturn(controllerMock);
    LogSearchDataRetrievalService dataRetrievalServiceMock =
      mockSupport.createMock(LogSearchDataRetrievalService.class);

     expect(controllerMock.getAmbariMetaInfo()).andReturn(metaInfoMock).atLeastOnce();
     expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
     expect(clustersMock.getCluster("clusterone")).andReturn(clusterMock).atLeastOnce();
@@ -308,16 +277,22 @@ public class LoggingSearchPropertyProviderTest {
     expect(metaInfoMock.getComponentToService(expectedStackName, expectedStackVersion, expectedComponentName)).andReturn(expectedServiceName).atLeastOnce();
     expect(metaInfoMock.getComponent(expectedStackName, expectedStackVersion, expectedServiceName, expectedComponentName)).andReturn(componentInfoMock).atLeastOnce();
 


     // simulate the case when LogSearch is not deployed, or is not available for some reason
    expect(helperFactoryMock.getHelper(controllerMock, "clusterone")).andReturn(null).atLeastOnce();
    expect(dataRetrievalServiceMock.getLogFileNames(expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(null).atLeastOnce();
 
     expect(componentInfoMock.getLogs()).andReturn(Collections.singletonList(logDefinitionMock)).atLeastOnce();
     expect(logDefinitionMock.getLogId()).andReturn(expectedLogSearchComponentName).atLeastOnce();
 
     mockSupport.replayAll();
 
    PropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider(helperFactoryMock, factoryMock);
    LoggingSearchPropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider();

    propertyProvider.setAmbariManagementController(controllerMock);
    propertyProvider.setLogSearchDataRetrievalService(dataRetrievalServiceMock);

 
     // execute the populate resources method, verify that no exceptions occur, due to
     // the LogSearch helper not being available
- 
2.19.1.windows.1

