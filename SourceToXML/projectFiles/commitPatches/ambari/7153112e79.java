From 7153112e7951af1b91c2254202148fd6afd83726 Mon Sep 17 00:00:00 2001
From: Sumit Mohanty <smohanty@hortonworks.com>
Date: Tue, 15 Dec 2015 17:06:07 -0800
Subject: [PATCH] AMBARI-14194. Role Based Access Control support for Metrics
 (Swapan Shridhar via smohanty)

--
 .../internal/AbstractPropertyProvider.java    | 170 +++++++++-
 .../StackDefinedPropertyProvider.java         |  11 +-
 .../controller/jmx/JMXPropertyProvider.java   |   2 +-
 .../metrics/MetricsPropertyProvider.java      |   7 +-
 .../metrics/MetricsPropertyProviderProxy.java |   9 +-
 .../MetricsReportPropertyProviderProxy.java   |   6 +-
 .../metrics/RestMetricsPropertyProvider.java  |   2 +-
 .../ThreadPoolEnabledPropertyProvider.java    |   8 +-
 .../authorization/AuthorizationHelper.java    |   3 +
 .../StackDefinedPropertyProviderTest.java     | 201 +++++++++---
 .../metrics/JMXPropertyProviderTest.java      | 291 ++++++++++++------
 .../RestMetricsPropertyProviderTest.java      | 235 +++++++++-----
 .../ganglia/GangliaPropertyProviderTest.java  | 160 ++++++++--
 .../timeline/AMSPropertyProviderTest.java     | 229 ++++++++++----
 14 files changed, 991 insertions(+), 343 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractPropertyProvider.java
index 4a0c44f0aa..2b7ee4eebb 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractPropertyProvider.java
@@ -7,7 +7,7 @@
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *        http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
@@ -18,15 +18,27 @@
 
 package org.apache.ambari.server.controller.internal;
 
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
 import org.apache.ambari.server.controller.metrics.MetricReportingAdapter;
 import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.TemporalInfo;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
 import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;

 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.text.DecimalFormat;
import java.util.EnumSet;
 import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
@@ -96,6 +108,136 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
 
   // ----- helper methods ----------------------------------------------------
 
  /**
   * Retrieves passed-in Resource's Type
   *
   * @param resources Set of Resources.
   * @return Type of resource from the Set.
   */
  protected String getResourceTypeFromResources(Set<Resource> resources) {
    String resType = null;
    if (resources != null) {
      Iterator<Resource> itr = resources.iterator();
      if (itr.hasNext()) {
        // Pick the 1st resource, as the passed in resources will have same Type,
        // in a given call.
        Resource res = itr.next();
        if (res != null) {
          resType = res.getType().toString();
        }
      }
    }
    return resType;
  }

  /**
   * Retrieves all the cluster names to which the passed-in Resource's belong.
   *
   * @param resources Set of Resources.
   * @return Cluster's Name
   */
  protected Set<String> getClustersNameFromResources(Set<Resource> resources, String clusterNamePropertyId) {
    Set<String> clusNames = new HashSet<String>();
    if (resources != null) {
      Iterator<Resource> itr = resources.iterator();
      while (itr.hasNext()) {
        Resource res = itr.next();
        if (res != null) {
          clusNames.add((String) res.getPropertyValue(clusterNamePropertyId));
        }
      }
    }
    return clusNames;
  }

  /**
   * Retrieves all the 'Cluster's Resource Ids' from the passed-in Resources.
   *
   * @param resources Set of Resources.
   * @param clusterNamePropertyId ClusterName PropertyId.
   * @return cluster Id.
   */
  protected Set<Long> getClustersResourceId(Set<Resource> resources, String clusterNamePropertyId) {
    Set<Long> clusterResId = new HashSet<Long>();
    if (clusterNamePropertyId != null) {
      try {
        AmbariManagementController amc = AmbariServer.getController();
        Set<String> clusterNames = getClustersNameFromResources(resources, clusterNamePropertyId);
        Iterator<String> clusNameItr = clusterNames.iterator();
        while (clusNameItr.hasNext()) {
          clusterResId.add(amc.getClusters().getCluster(clusNameItr.next()).getResourceId());
        }
      } catch (AmbariException e) {
        LOG.error("Cluster Id couldn't be retrieved.");
      } catch (Exception e) {
        LOG.error("Cluster Id couldn't be retrieved");
      }
    }
    if(LOG.isDebugEnabled()) {
      LOG.debug("Retrieved Cluster Ids = " + clusterResId.toString());
    }
    return clusterResId;
  }


  /**
   * Check the User's authorization for retrieving the Metrics.
   *
   * @param resources Set of Resources.
   * @param clusterNamePropertyId ClusterName PropertyId.
   * @return boolean
   * @throws AuthorizationException
   */
  protected boolean checkAuthorizationForMetrics(Set<Resource> resources, String clusterNamePropertyId) throws AuthorizationException {
    String resType = null;

    // Get the Type
    resType = getResourceTypeFromResources(resources);
    if (resType == null) {
      return false;
    }

    // Get the cluster Id.
    Set<Long> clusterResIds = getClustersResourceId(resources, clusterNamePropertyId);
    if (clusterResIds.size() == 0) {
      return false;
    }

    if(LOG.isDebugEnabled()) {
      LOG.debug("Retrieved cluster's Resource Id = " + clusterResIds + ", Resource Type = " + resType);
    }
    Iterator<Long> clusResIdsItr = clusterResIds.iterator();
    while (clusResIdsItr.hasNext()) {
      Long clusResId = clusResIdsItr.next();
      Resource.InternalType resTypeVal = Resource.InternalType.valueOf(resType);
      switch (resTypeVal) {
        case Cluster:
          if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, clusResId, EnumSet.of(RoleAuthorization.CLUSTER_VIEW_METRICS))) {
            throw new AuthorizationException("The authenticated user does not have authorization to view cluster metrics");
          }
          break;
        case Host:
          if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, clusResId, EnumSet.of(RoleAuthorization.HOST_VIEW_METRICS))) {
            throw new AuthorizationException("The authenticated user does not have authorization to view Host metrics");
          }
          break;
        case Component :
          if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, clusResId, EnumSet.of(RoleAuthorization.SERVICE_VIEW_METRICS))) {
            throw new AuthorizationException("The authenticated user does not have authorization to view Service metrics");
          }
          break;
        case HostComponent:
          if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, clusResId, EnumSet.of(RoleAuthorization.SERVICE_VIEW_METRICS))) {
            throw new AuthorizationException("The authenticated user does not have authorization to view Service metrics");
          }
          break;
        default:
          LOG.error("Unsuported Resource Type for Metrics");
          return false;
      }
    }
    return true;
  }
   /**
    * Get a map of metric / property info based on the given component name and property id.
    * Note that the property id may map to multiple metrics if the property id is a category.
@@ -139,7 +281,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
       }
     }
 
    if (!propertyId.endsWith("/")){
    if (!propertyId.endsWith("/")) {
       propertyId += "/";
     }
 
@@ -198,13 +340,13 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
         matcher = FIND_ARGUMENT_METHOD_REGEX.matcher(argName);
         while (matcher.find()) {
           // find the end of the method
          int openParenIndex  = argName.indexOf('(', matcher.start());
          int openParenIndex = argName.indexOf('(', matcher.start());
           int closeParenIndex = indexOfClosingParenthesis(argName, openParenIndex);
 
           String methodName = argName.substring(matcher.start() + 1, openParenIndex);
          String args       = argName.substring(openParenIndex + 1, closeParenIndex);
          String args = argName.substring(openParenIndex + 1, closeParenIndex);
 
          List<Object>   argList    = new LinkedList<Object>();
          List<Object> argList = new LinkedList<Object>();
           List<Class<?>> paramTypes = new LinkedList<Class<?>>();
 
           // for each argument of the method ...
@@ -217,7 +359,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
             value = invokeArgumentMethod(value, methodName, argList, paramTypes);
           } catch (Exception e) {
             throw new IllegalArgumentException("Can't apply method " + methodName + " for argument " +
                argName + " in " + propertyId, e);
              argName + " in " + propertyId, e);
           }
         }
         if (value.equals(val)) {
@@ -234,7 +376,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
    * Find the index of the closing parenthesis in the given string.
    */
   private static int indexOfClosingParenthesis(String s, int index) {
    int depth  = 0;
    int depth = 0;
     int length = s.length();
 
     while (index < length) {
@@ -242,8 +384,8 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
       if (c == '(') {
         ++depth;
       } else if (c == ')') {
        if (--depth ==0 ){
         return index;
        if (--depth == 0) {
          return index;
         }
       }
     }
@@ -258,7 +400,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
 
     // only supports strings and integers
     if (arg.contains("\"")) {
      argList.add(arg.substring(1, arg.length() -1));
      argList.add(arg.substring(1, arg.length() - 1));
       paramTypes.add(String.class);
     } else {
       Integer number = Integer.parseInt(arg);
@@ -272,7 +414,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
    */
   private static String invokeArgumentMethod(String argValue, String methodName, List<Object> argList,
                                              List<Class<?>> paramTypes)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
     // invoke the method through reflection
     Method method = String.class.getMethod(methodName, paramTypes.toArray(new Class<?>[paramTypes.size()]));
 
@@ -296,7 +438,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
     }
 
     if (!componentMetricMap.containsKey(propertyId) && regexKey != null
        && !regexKey.equals(propertyId)) {
      && !regexKey.equals(propertyId)) {
 
       PropertyInfo propertyInfo = componentMetricMap.get(regexKey);
       if (propertyInfo != null) {
@@ -320,7 +462,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
   protected PropertyInfo updatePropertyInfo(String propertyKey, String id, PropertyInfo propertyInfo) {
     List<String> regexGroups = getRegexGroups(propertyKey, id);
     String propertyId = propertyInfo.getPropertyId();
    if(propertyId != null) {
    if (propertyId != null) {
       for (String regexGroup : regexGroups) {
         regexGroup = regexGroup.replace("/", ".");
         propertyId = propertyId.replaceFirst(FIND_REGEX_IN_METRIC_REGEX, regexGroup);
@@ -354,7 +496,7 @@ public abstract class AbstractPropertyProvider extends BaseProvider implements P
 
     String category = PropertyHelper.getPropertyCategory(propertyId);
     while (category != null) {
      if(categoryIds.contains(category)) {
      if (categoryIds.contains(category)) {
         return true;
       }
       category = PropertyHelper.getPropertyCategory(category);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProvider.java
index a1b4f3faa2..edd11c14f9 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProvider.java
@@ -33,6 +33,7 @@ import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.security.authorization.AuthorizationException;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.StackId;
@@ -40,6 +41,7 @@ import org.apache.ambari.server.state.stack.Metric;
 import org.apache.ambari.server.state.stack.MetricDefinition;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.lang.reflect.Constructor;
 import java.lang.reflect.Method;
 import java.util.ArrayList;
@@ -50,8 +52,6 @@ import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;

 /**
  * This class analyzes a service's metrics to determine if additional
  * metrics should be fetched.  It's okay to maintain state here since these
@@ -206,8 +206,11 @@ public class StackDefinedPropertyProvider implements PropertyProvider {
         pp.populateResources(resources, request, predicate);
       }
 
    } catch (Exception e) {
      e.printStackTrace();
    } catch (AuthorizationException e) {
      // Need to rethrow the catched 'AuthorizationException'.
      throw e;
    }
    catch (Exception e) {
       throw new SystemException("Error loading deferred resources", e);
     }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/jmx/JMXPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/jmx/JMXPropertyProvider.java
index 2748dd4bd5..2079e72669 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/jmx/JMXPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/jmx/JMXPropertyProvider.java
@@ -121,7 +121,7 @@ public class JMXPropertyProvider extends ThreadPoolEnabledPropertyProvider {
                              String componentNamePropertyId,
                              String statePropertyId) {
 
    super(componentMetrics, hostNamePropertyId, metricHostProvider);
    super(componentMetrics, hostNamePropertyId, metricHostProvider, clusterNamePropertyId);
 
     this.streamProvider           = streamProvider;
     this.jmxHostProvider          = jmxHostProvider;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProvider.java
index f1c5c8182a..a346051893 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProvider.java
@@ -26,8 +26,6 @@ import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import java.util.Map;
@@ -129,12 +127,15 @@ public abstract class MetricsPropertyProvider extends AbstractPropertyProvider {
   @Override
   public Set<Resource> populateResources(Set<Resource> resources,
                 Request request, Predicate predicate) throws SystemException {

     Set<String> ids = getRequestPropertyIds(request, predicate);
     if (ids.isEmpty()) {
       return resources;
     }
 
    if(!checkAuthorizationForMetrics(resources, clusterNamePropertyId)) {
      return resources;
    }

     // Re-initialize in case of reuse.
     metricsPaddingMethod = DEFAULT_PADDING_METHOD;
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProviderProxy.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProviderProxy.java
index ac11556c45..c48aa231b0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProviderProxy.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsPropertyProviderProxy.java
@@ -34,11 +34,10 @@ import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;

 import java.util.Map;
 import java.util.Set;
 
import static org.apache.ambari.server.controller.metrics.MetricsPaddingMethod.ZERO_PADDING_PARAM;
 import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
 import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.GANGLIA;
 import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.TIMELINE_METRICS;
@@ -49,6 +48,7 @@ public class MetricsPropertyProviderProxy extends AbstractPropertyProvider {
   private AMSPropertyProvider amsPropertyProvider;
   private GangliaPropertyProvider gangliaPropertyProvider;
   private TimelineMetricCacheProvider cacheProvider;
  private String clusterNamePropertyId;
 
   public MetricsPropertyProviderProxy(
     InternalType type,
@@ -65,6 +65,7 @@ public class MetricsPropertyProviderProxy extends AbstractPropertyProvider {
     super(componentPropertyInfoMap);
     this.metricsServiceProvider = serviceProvider;
     this.cacheProvider = cacheProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
 
     switch (type) {
       case Host:
@@ -183,6 +184,10 @@ public class MetricsPropertyProviderProxy extends AbstractPropertyProvider {
   public Set<Resource> populateResources(Set<Resource> resources, Request request,
                                          Predicate predicate) throws SystemException {
 
    if(!checkAuthorizationForMetrics(resources, clusterNamePropertyId)) {
      return resources;
    }

     MetricsService metricsService = metricsServiceProvider.getMetricsServiceType();
 
     if (metricsService != null) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsReportPropertyProviderProxy.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsReportPropertyProviderProxy.java
index 4d2ce018ee..f28c34d61a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsReportPropertyProviderProxy.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/MetricsReportPropertyProviderProxy.java
@@ -28,7 +28,6 @@ import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
 
 import java.util.Map;
 import java.util.Set;
@@ -42,6 +41,7 @@ public class MetricsReportPropertyProviderProxy extends AbstractPropertyProvider
   private MetricsReportPropertyProvider gangliaMetricsReportProvider;
   private final MetricsServiceProvider metricsServiceProvider;
   private TimelineMetricCacheProvider cacheProvider;
  private String clusterNamePropertyId;
 
   public MetricsReportPropertyProviderProxy(
     Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
@@ -56,6 +56,7 @@ public class MetricsReportPropertyProviderProxy extends AbstractPropertyProvider
     super(componentPropertyInfoMap);
     this.metricsServiceProvider = serviceProvider;
     this.cacheProvider = cacheProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
 
     createReportPropertyProviders(componentPropertyInfoMap,
       streamProvider,
@@ -105,6 +106,9 @@ public class MetricsReportPropertyProviderProxy extends AbstractPropertyProvider
   public Set<Resource> populateResources(Set<Resource> resources, Request request,
                                          Predicate predicate) throws SystemException {
 
    if(!checkAuthorizationForMetrics(resources, clusterNamePropertyId)) {
      return resources;
    }
     MetricsService metricsService = metricsServiceProvider.getMetricsServiceType();
 
     if (metricsService != null) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java
index b32adda106..fc76b1ec10 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java
@@ -127,7 +127,7 @@ public class RestMetricsPropertyProvider extends ThreadPoolEnabledPropertyProvid
     String statePropertyId,
     String componentName){
 
    super(componentMetrics, hostNamePropertyId, metricHostProvider);
    super(componentMetrics, hostNamePropertyId, metricHostProvider, clusterNamePropertyId);
     this.metricsProperties = metricsProperties;
     this.streamProvider = streamProvider;
     this.clusterNamePropertyId = clusterNamePropertyId;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/ThreadPoolEnabledPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/ThreadPoolEnabledPropertyProvider.java
index 8a35636cdd..1e961a6b24 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/ThreadPoolEnabledPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/ThreadPoolEnabledPropertyProvider.java
@@ -51,6 +51,7 @@ public abstract class ThreadPoolEnabledPropertyProvider extends AbstractProperty
   public static final Set<String> healthyStates = Collections.singleton("STARTED");
   protected final String hostNamePropertyId;
   private final MetricHostProvider metricHostProvider;
  private final String clusterNamePropertyId;
 
   /**
    * Executor service is shared between all childs of current class
@@ -78,10 +79,12 @@ public abstract class ThreadPoolEnabledPropertyProvider extends AbstractProperty
    */
   public ThreadPoolEnabledPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics,
                                            String hostNamePropertyId,
                                           MetricHostProvider metricHostProvider) {
                                           MetricHostProvider metricHostProvider,
                                           String clusterNamePropertyId) {
     super(componentMetrics);
     this.hostNamePropertyId = hostNamePropertyId;
     this.metricHostProvider = metricHostProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
   }
 
   // ----- Thread pool -------------------------------------------------------
@@ -117,6 +120,9 @@ public abstract class ThreadPoolEnabledPropertyProvider extends AbstractProperty
   public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
       throws SystemException {
 
    if(!checkAuthorizationForMetrics(resources, clusterNamePropertyId)) {
      return resources;
    }
     // Get a valid ticket for the request.
     Ticket ticket = new Ticket();
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
index 0c675b88dc..b136182392 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
@@ -160,6 +160,9 @@ public class AuthorizationHelper {
         if (ResourceType.AMBARI == privilegeResourceType) {
           // This resource type indicates administrative access
           resourceOK = true;
        } else if (ResourceType.VIEW == privilegeResourceType) {
          // For a VIEW USER.
          resourceOK = true;
         } else if ((resourceType == null) || (resourceType == privilegeResourceType)) {
           resourceOK = (resourceId == null) || resourceId.equals(privilegeResource.getId());
         } else {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProviderTest.java
index bb6673cebe..0ae3e6aad8 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackDefinedPropertyProviderTest.java
@@ -17,22 +17,17 @@
  */
 package org.apache.ambari.server.controller.internal;
 
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

 import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
 import com.google.inject.Module;
import com.google.inject.persist.PersistService;
 import com.google.inject.util.Modules;
import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
 import org.apache.ambari.server.controller.jmx.TestStreamProvider;
 import org.apache.ambari.server.controller.metrics.JMXPropertyProviderTest;
 import org.apache.ambari.server.controller.metrics.MetricsServiceProvider;
@@ -48,10 +43,11 @@ import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.spi.TemporalInfo;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
 import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.Host;
@@ -63,10 +59,23 @@ import org.junit.Assert;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
 
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
 
 /**
  * Tests the stack defined property provider.
@@ -75,6 +84,7 @@ public class StackDefinedPropertyProviderTest {
   private static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = "HostRoles/host_name";
   private static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = "HostRoles/component_name";
   private static final String HOST_COMPONENT_STATE_PROPERTY_ID = "HostRoles/state";
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
 
   private Clusters clusters = null;
   private Injector injector = null;
@@ -111,9 +121,9 @@ public class StackDefinedPropertyProviderTest {
     clusters = injector.getInstance(Clusters.class);
     StackId stackId = new StackId("HDP-2.0.5");
 
    clusters.addCluster("c1", stackId);
    clusters.addCluster("c2", stackId);
 
    Cluster cluster = clusters.getCluster("c1");
    Cluster cluster = clusters.getCluster("c2");
 
     cluster.setDesiredStackVersion(stackId);
     helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
@@ -128,7 +138,25 @@ public class StackDefinedPropertyProviderTest {
     host.setHostAttributes(hostAttributes);
     host.persist();
 
    clusters.mapHostToCluster("h1", "c1");
    clusters.mapHostToCluster("h1", "c2");

    // Setting up Mocks for Controller, Clusters etc, queried as part of user's Role context
    // while fetching Metrics.
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
    Clusters clustersMock = createNiceMock(Clusters.class);
    Cluster clusterMock = createNiceMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clustersMock).anyTimes();
    expect(clustersMock.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(clusterMock).anyTimes();
    expect(clusterMock.getResourceId()).andReturn(2L).anyTimes();
    try {
      expect(clustersMock.getCluster(anyObject(String.class))).andReturn(clusterMock).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    replay(amc, clustersMock, clusterMock);
   }
 
   @After
@@ -137,6 +165,95 @@ public class StackDefinedPropertyProviderTest {
   }
 
   @Test
  public void testStackDefinedPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

  @Test
  public void testStackDefinedPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

  @Test
  public void testStackDefinedPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

  @Test(expected = AuthorizationException.class)
  public void testStackDefinedPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

   public void testPopulateHostComponentResources() throws Exception {
     JMXPropertyProviderTest.TestJMXHostProvider tj = new JMXPropertyProviderTest.TestJMXHostProvider(true);
     JMXPropertyProviderTest.TestMetricHostProvider tm = new JMXPropertyProviderTest.TestMetricHostProvider();
@@ -150,7 +267,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty("HostRoles/host_name", "h1");
     resource.setProperty("HostRoles/component_name", "NAMENODE");
     resource.setProperty("HostRoles/state", "STARTED");
@@ -171,7 +288,6 @@ public class StackDefinedPropertyProviderTest {
   }
 
 
  @Test
   public void testCustomProviders() throws Exception {
 
     StackDefinedPropertyProvider sdpp = new StackDefinedPropertyProvider(
@@ -181,7 +297,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty("HostRoles/host_name", "h1");
     resource.setProperty("HostRoles/component_name", "DATANODE");
     resource.setProperty("HostRoles/state", "STARTED");
@@ -326,7 +442,6 @@ public class StackDefinedPropertyProviderTest {
     }
   }
 
  @Test
   public void testPopulateResources_HDP2() throws Exception {
 
     URLStreamProvider  streamProvider = new TestStreamProvider();
@@ -350,7 +465,7 @@ public class StackDefinedPropertyProviderTest {
     // resourcemanager
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -378,7 +493,7 @@ public class StackDefinedPropertyProviderTest {
     //namenode
     resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -389,7 +504,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
   }
 
  @Test
   public void testPopulateResources_HDP2_params() throws Exception {
     TestStreamProvider  streamProvider = new TestStreamProvider();
     JMXPropertyProviderTest.TestJMXHostProvider hostProvider = new JMXPropertyProviderTest.TestJMXHostProvider(false);
@@ -411,7 +525,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -442,7 +556,6 @@ public class StackDefinedPropertyProviderTest {
   }
 
 
  @Test
   public void testPopulateResources_HDP2_params_singleProperty() throws Exception {
     TestStreamProvider  streamProvider = new TestStreamProvider();
     JMXPropertyProviderTest.TestJMXHostProvider hostProvider = new JMXPropertyProviderTest.TestJMXHostProvider(false);
@@ -464,7 +577,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -480,7 +593,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
   }
 
  @Test
   public void testPopulateResources_HDP2_params_category() throws Exception {
     TestStreamProvider  streamProvider = new TestStreamProvider();
     JMXPropertyProviderTest.TestJMXHostProvider hostProvider = new JMXPropertyProviderTest.TestJMXHostProvider(false);
@@ -502,7 +614,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -533,7 +645,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
   }
 
  @Test
   public void testPopulateResources_HDP2_params_category2() throws Exception {
     TestStreamProvider  streamProvider = new TestStreamProvider();
     JMXPropertyProviderTest.TestJMXHostProvider hostProvider = new JMXPropertyProviderTest.TestJMXHostProvider(false);
@@ -555,7 +666,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -592,7 +703,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
   }
 
  @Test
   public void testPopulateResources_jmx_JournalNode() throws Exception {
     TestStreamProvider  streamProvider = new TestStreamProvider();
     JMXPropertyProviderTest.TestJMXHostProvider hostProvider = new JMXPropertyProviderTest.TestJMXHostProvider(false);
@@ -614,7 +724,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -712,10 +822,9 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertEquals(8444, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "lastWrittenTxId")));
   }
 
  @Test
   public void testPopulateResources_jmx_Storm() throws Exception {
     // Adjust stack version for cluster
    Cluster cluster = clusters.getCluster("c1");
    Cluster cluster = clusters.getCluster("c2");
     cluster.setDesiredStackVersion(new StackId("HDP-2.1.1"));
 
     TestStreamProvider  streamProvider = new TestStreamProvider();
@@ -739,7 +848,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -760,7 +869,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertEquals(4637.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "nimbus.uptime")));
   }
 
  @Test
   public void testPopulateResources_NoRegionServer() throws Exception {
     TestStreamProvider  streamProvider = new TestStreamProvider();
     JMXPropertyProviderTest.TestJMXHostProvider hostProvider = new JMXPropertyProviderTest.TestJMXHostProvider(false);
@@ -782,7 +890,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_REGIONSERVER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -797,7 +905,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertEquals(preSize, resource.getPropertiesMap().size());
   }
 
  @Test
   public void testPopulateResources_HBaseMaster2() throws Exception {
     TestStreamProvider  streamProvider = new TestStreamProvider();
     JMXPropertyProviderTest.TestJMXHostProvider hostProvider = new JMXPropertyProviderTest.TestJMXHostProvider(false);
@@ -819,7 +926,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_MASTER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -838,7 +945,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertTrue(map.get("metrics/hbase/master").containsKey("IsActiveMaster"));
   }
 
  @Test
   public void testPopulateResources_params_category5() throws Exception {
     org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider streamProvider =
         new org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider("temporal_ganglia_data_yarn_queues.txt");
@@ -863,7 +969,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "dev01.ambari.apache.org");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
 
@@ -886,7 +992,6 @@ public class StackDefinedPropertyProviderTest {
     Assert.assertNotNull(resource.getPropertyValue(RM_AVAILABLE_MEMORY_PROPERTY));
   }
 
  @Test
   public void testPopulateResources_ganglia_JournalNode() throws Exception {
     org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider streamProvider =
         new org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider("journalnode_ganglia_data.txt");
@@ -911,7 +1016,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");
 
@@ -1001,7 +1106,6 @@ public class StackDefinedPropertyProviderTest {
     }
   }
 
  @Test
   public void testPopulateResources_resourcemanager_clustermetrics() throws Exception {
 
     String[] metrics = new String[] {
@@ -1035,7 +1139,7 @@ public class StackDefinedPropertyProviderTest {
     for (String metric : metrics) {
       Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
      resource.setProperty("HostRoles/cluster_name", "c1");
      resource.setProperty("HostRoles/cluster_name", "c2");
       resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
       resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
 
@@ -1053,7 +1157,6 @@ public class StackDefinedPropertyProviderTest {
 
   }
 
  @Test
   public void testPopulateResourcesWithAggregateFunctionMetrics() throws Exception {
 
     String metric = "metrics/rpc/NumOpenConnections._sum";
@@ -1086,7 +1189,7 @@ public class StackDefinedPropertyProviderTest {
 
     Resource resource = new ResourceImpl(Resource.Type.Component);
 
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/cluster_name", "c2");
     resource.setProperty("HostRoles/service_name", "HBASE");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_REGIONSERVER");
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/JMXPropertyProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/JMXPropertyProviderTest.java
index f0c12800fc..f76c3227e1 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/JMXPropertyProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/JMXPropertyProviderTest.java
@@ -18,6 +18,9 @@
 
 package org.apache.ambari.server.controller.metrics;
 
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
 import org.apache.ambari.server.controller.internal.ResourceImpl;
 import org.apache.ambari.server.controller.jmx.JMXHostProvider;
 import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
@@ -27,58 +30,150 @@ import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.spi.TemporalInfo;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.junit.After;
 import org.junit.Assert;
import org.junit.Before;
 import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;

 import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
 
 /**
  * JMX property provider tests.
  */
 public class JMXPropertyProviderTest {
  protected static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
   protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
   protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
   protected static final String HOST_COMPONENT_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "state");
 
   public static final int NUMBER_OF_RESOURCES = 400;
 
  @Before
  public void setUpCommonMocks() throws AmbariException, NoSuchFieldException, IllegalAccessException {
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();

    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }

    replay(amc, clusters, cluster);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

   @Test
  public void testJMXPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testJMXPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testJMXPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test(expected = AuthorizationException.class)
  public void testJMXPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

   public void testPopulateResources() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestStreamProvider streamProvider = new TestStreamProvider();
     TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
     TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();

     JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));
 
     // namenode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
 
     // request with an empty set should get all supported properties
     Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

     Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
 
     Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"), streamProvider.getLastSpec());
 
     // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28,      resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
    Assert.assertEquals(13670605, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
     Assert.assertEquals(1006632960, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
     Assert.assertEquals(473433016, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
     Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
@@ -90,7 +185,7 @@ public class JMXPropertyProviderTest {
 
     // datanode
     resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");
 
@@ -102,7 +197,7 @@ public class JMXPropertyProviderTest {
     Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "50075", "/jmx"), streamProvider.getLastSpec());
 
     // see test/resources/hdfs_datanode_jmx.json for values
    Assert.assertEquals(856,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(856, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
     Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
     Assert.assertEquals(9772616, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
     Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
@@ -114,7 +209,7 @@ public class JMXPropertyProviderTest {
 
     // hbase master
     resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_MASTER");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -132,7 +227,7 @@ public class JMXPropertyProviderTest {
 
     Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "60010", "/jmx"), streamProvider.getLastSpec());
 
    Assert.assertEquals(8, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(9, PropertyHelper.getProperties(resource).size());
     Assert.assertEquals(1069416448, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
     Assert.assertEquals(4806976, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
     Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
@@ -142,61 +237,59 @@ public class JMXPropertyProviderTest {
     Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));
   }
 
  @Test
   public void testPopulateResources_singleProperty() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestStreamProvider streamProvider = new TestStreamProvider();
     TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
     TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
 
     JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));
 
     // namenode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
 
     // only ask for one property
     Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/rpc/ReceivedBytes"), temporalInfoMap);
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/rpc/ReceivedBytes"), temporalInfoMap);
 
     Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
 
     Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"), streamProvider.getLastSpec());
 
     // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
    Assert.assertEquals(13670605, resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
     Assert.assertNull(resource.getPropertyValue("metrics/dfs/namenode/CreateFileOps"));
   }
 
  @Test
   public void testPopulateResources_category() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestStreamProvider streamProvider = new TestStreamProvider();
     TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
     TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
 
     JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));
 
     // namenode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
     resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -204,37 +297,36 @@ public class JMXPropertyProviderTest {
     // request with an empty set should get all supported properties
     // only ask for one property
     Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/dfs"), temporalInfoMap);
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/dfs"), temporalInfoMap);
 
     Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
 
    Assert.assertEquals(propertyProvider.getSpec("http","domu-12-31-39-0e-34-e1.compute-1.internal", "50070","/jmx"), streamProvider.getLastSpec());
    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"), streamProvider.getLastSpec());
 
     // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(184320,  resource.getPropertyValue("metrics/dfs/FSNamesystem/CapacityUsed"));
    Assert.assertEquals(21,  resource.getPropertyValue("metrics/dfs/FSNamesystem/UnderReplicatedBlocks"));
    Assert.assertEquals(184320, resource.getPropertyValue("metrics/dfs/FSNamesystem/CapacityUsed"));
    Assert.assertEquals(21, resource.getPropertyValue("metrics/dfs/FSNamesystem/UnderReplicatedBlocks"));
     Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
   }
 
  @Test
   public void testPopulateResourcesWithUnknownPort() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestStreamProvider streamProvider = new TestStreamProvider();
     TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
     TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
 
     JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));
 
     // namenode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
 
@@ -243,32 +335,31 @@ public class JMXPropertyProviderTest {
 
     Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
 
    Assert.assertEquals(propertyProvider.getSpec("http","domu-12-31-39-0e-34-e1.compute-1.internal", "50070","/jmx"), streamProvider.getLastSpec());
    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"), streamProvider.getLastSpec());
 
     // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28,      resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
    Assert.assertEquals(13670605, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
     Assert.assertEquals(1006632960, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
     Assert.assertEquals(473433016, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
     Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
     Assert.assertEquals(23634400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
   }
 
  @Test
   public void testPopulateResourcesUnhealthyResource() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestStreamProvider streamProvider = new TestStreamProvider();
     TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
     TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
 
     JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));
 
     // namenode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
@@ -286,45 +377,42 @@ public class JMXPropertyProviderTest {
     Assert.assertNull(streamProvider.getLastSpec());
   }
 
  @Test
   public void testPopulateResourcesMany() throws Exception {
     // Set the provider to take 50 millis to return the JMX values
    TestStreamProvider  streamProvider = new TestStreamProvider(50L);
    TestStreamProvider streamProvider = new TestStreamProvider(50L);
     TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
     TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
     Set<Resource> resources = new HashSet<Resource>();
 
     JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));
 
     for (int i = 0; i < NUMBER_OF_RESOURCES; ++i) {
       // datanode
       Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
       resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
       resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");
       resource.setProperty("unique_id", i);
 
       resources.add(resource);
     }

     // request with an empty set should get all supported properties
     Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());
 
     Set<Resource> resourceSet = propertyProvider.populateResources(resources, request, null);
 
     Assert.assertEquals(NUMBER_OF_RESOURCES, resourceSet.size());

     for (Resource resource : resourceSet) {
       // see test/resources/hdfs_datanode_jmx.json for values
      Assert.assertEquals(856,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
      Assert.assertEquals(856, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
       Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
       Assert.assertEquals(9772616, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
       Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
@@ -332,30 +420,29 @@ public class JMXPropertyProviderTest {
     }
   }
 
  @Test
   public void testPopulateResourcesTimeout() throws Exception {
     // Set the provider to take 100 millis to return the JMX values
    TestStreamProvider  streamProvider = new TestStreamProvider(100L);
    TestStreamProvider streamProvider = new TestStreamProvider(100L);
     TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
     TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
     Set<Resource> resources = new HashSet<Resource>();
 
     JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        "HostRoles/cluster_name",
        "HostRoles/host_name",
        "HostRoles/component_name",
        "HostRoles/state");
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      "HostRoles/cluster_name",
      "HostRoles/host_name",
      "HostRoles/component_name",
      "HostRoles/state");
 
     // set the provider timeout to 50 millis
     propertyProvider.setPopulateTimeout(50L);
 
     // datanode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
     resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");
 
@@ -398,7 +485,7 @@ public class JMXPropertyProviderTest {
 
     @Override
     public String getPort(String clusterName, String componentName) throws
        SystemException {
      SystemException {
 
       if (unknownPort) {
         return null;
@@ -409,10 +496,16 @@ public class JMXPropertyProviderTest {
       else if (componentName.equals("DATANODE"))
         return "50075";
       else if (componentName.equals("HBASE_MASTER"))
        return null == clusterName ? "60010" : "60011";
      else  if (componentName.equals("JOURNALNODE"))
        if(clusterName == "c2") {
          return "60011";
        } else {
          // Caters the case where 'clusterName' is null or
          // any other name (includes hardcoded name "c1").
          return "60010";
        }
      else if (componentName.equals("JOURNALNODE"))
         return "8480";
      else  if (componentName.equals("STORM_REST_API"))
      else if (componentName.equals("STORM_REST_API"))
         return "8745";
       else
         return null;
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProviderTest.java
index 82b42f20e1..220f905e3d 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProviderTest.java
@@ -7,7 +7,7 @@
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
@@ -18,16 +18,11 @@
 
 package org.apache.ambari.server.controller.metrics;
 
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
 import org.apache.ambari.server.controller.internal.PropertyInfo;
 import org.apache.ambari.server.controller.internal.ResourceImpl;
 import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
@@ -40,17 +35,30 @@ import org.apache.ambari.server.controller.spi.TemporalInfo;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.stack.Metric;
 import org.apache.ambari.server.state.stack.MetricDefinition;
import org.junit.After;
 import org.junit.Assert;
 import org.junit.BeforeClass;
 import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
 
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
 
 
 /**
@@ -64,6 +72,7 @@ public class RestMetricsPropertyProviderTest {
   protected static final String HOST_COMPONENT_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "state");
   protected static final Map<String, String> metricsProperties = new HashMap<String, String>();
   protected static final Map<String, Metric> componentMetrics = new HashMap<String, Metric>();
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
   public static final int NUMBER_OF_RESOURCES = 400;
   private static Injector injector;
   private static Clusters clusters;
@@ -93,9 +102,81 @@ public class RestMetricsPropertyProviderTest {
     clusters = injector.getInstance(Clusters.class);
     clusters.addCluster("c1", new StackId("HDP-2.1.1"));
     c1 = clusters.getCluster("c1");

    // Setting up Mocks for Controller, Clusters etc, queried as part of user's Role context
    // while fetching Metrics.
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
    Clusters clustersMock = createNiceMock(Clusters.class);
    Cluster clusterMock = createNiceMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clustersMock).anyTimes();
    expect(clustersMock.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(clusterMock).anyTimes();
    expect(clusterMock.getResourceId()).andReturn(2L).anyTimes();
    try {
      expect(clustersMock.getCluster(anyObject(String.class))).andReturn(clusterMock).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    replay(amc, clustersMock, clusterMock);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
   }
 
   @Test
  public void testRestMetricsPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testRestMetricsPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testRestMetricsPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test(expected = AuthorizationException.class)
  public void testRestMetricsPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

   public void testPopulateResources() throws Exception {
     MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
     expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
@@ -110,16 +191,16 @@ public class RestMetricsPropertyProviderTest {
     TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();
 
     RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
        injector,
        metricDefinition.getProperties(),
        componentMetrics,
        streamProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        "STORM_REST_API");
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");
 
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
@@ -131,7 +212,6 @@ public class RestMetricsPropertyProviderTest {
 
     // request with an empty set should get all supported properties
     Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

     Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());
     Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "wrong.metric")));
 
@@ -144,11 +224,8 @@ public class RestMetricsPropertyProviderTest {
     Assert.assertEquals(3.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.used")));
     Assert.assertEquals(1.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "topologies")));
     Assert.assertEquals(4637.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "nimbus.uptime")));


   }
 
  @Test
   public void testPopulateResources_singleProperty() throws Exception {
     MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
     expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
@@ -162,16 +239,16 @@ public class RestMetricsPropertyProviderTest {
     TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();
 
     RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
        injector,
        metricDefinition.getProperties(),
        componentMetrics,
        streamProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        "STORM_REST_API");
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");
 
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
@@ -190,7 +267,6 @@ public class RestMetricsPropertyProviderTest {
     Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/taskstotal"));
   }
 
  @Test
   public void testPopulateResources_category() throws Exception {
     MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
     expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
@@ -204,16 +280,16 @@ public class RestMetricsPropertyProviderTest {
     TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();
 
     RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
        injector,
        metricDefinition.getProperties(),
        componentMetrics,
        streamProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        "STORM_REST_API");
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");
 
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
@@ -236,7 +312,6 @@ public class RestMetricsPropertyProviderTest {
     Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/taskstotal"));
   }
 
  @Test
   public void testPopulateResourcesUnhealthyResource() throws Exception {
     MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
     expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
@@ -250,16 +325,16 @@ public class RestMetricsPropertyProviderTest {
     TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();
 
     RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
        injector,
        metricDefinition.getProperties(),
        componentMetrics,
        streamProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        "STORM_REST_API");
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");
 
 
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
@@ -278,7 +353,6 @@ public class RestMetricsPropertyProviderTest {
     Assert.assertNull(streamProvider.getLastSpec());
   }
 
  @Test
   public void testPopulateResourcesMany() throws Exception {
     MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
     expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
@@ -294,16 +368,16 @@ public class RestMetricsPropertyProviderTest {
     Set<Resource> resources = new HashSet<Resource>();
 
     RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
        injector,
        metricDefinition.getProperties(),
        componentMetrics,
        streamProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        "STORM_REST_API");
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");
 
     for (int i = 0; i < NUMBER_OF_RESOURCES; ++i) {
       // strom_rest_api
@@ -333,7 +407,6 @@ public class RestMetricsPropertyProviderTest {
     }
   }
 
  @Test
   public void testPopulateResourcesTimeout() throws Exception {
     MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
     expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
@@ -349,16 +422,16 @@ public class RestMetricsPropertyProviderTest {
     Set<Resource> resources = new HashSet<Resource>();
 
     RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
        injector,
        metricDefinition.getProperties(),
        componentMetrics,
        streamProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        "STORM_REST_API");
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");
 
     // set the provider timeout to 50 millis
     restMetricsPropertyProvider.setPopulateTimeout(50L);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/ganglia/GangliaPropertyProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/ganglia/GangliaPropertyProviderTest.java
index 6fefffee1e..b513ba5148 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/ganglia/GangliaPropertyProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/ganglia/GangliaPropertyProviderTest.java
@@ -17,8 +17,11 @@
  */
 package org.apache.ambari.server.controller.metrics.ganglia;
 
import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
 import org.apache.ambari.server.configuration.ComponentSSLConfigurationTest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
 import org.apache.ambari.server.controller.internal.PropertyInfo;
 import org.apache.ambari.server.controller.internal.ResourceImpl;
 import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
@@ -29,17 +32,25 @@ import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.spi.TemporalInfo;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.utils.CollectionPresentationUtils;
 import org.apache.commons.collections.CollectionUtils;
 import org.apache.commons.collections.Predicate;
 import org.apache.http.NameValuePair;
 import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
 import org.junit.Assert;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.powermock.api.easymock.PowerMock;
 import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
@@ -49,11 +60,14 @@ import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;

 import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
 import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.GANGLIA;
 import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
 import static org.easymock.EasyMock.eq;
 import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
 
 /**
  * Test the Ganglia property provider.
@@ -98,8 +112,87 @@ public class GangliaPropertyProviderTest {
     this.configuration = configuration;
   }
 
  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

   @Test
  public void testGangliaPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

  @Test
  public void testGangliaPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

  @Test
  public void testGangliaPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

  @Test(expected = AuthorizationException.class)
  public void testGangliaPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

   public void testPopulateResources() throws Exception {
    setUpCommonMocks();
     TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
 
@@ -115,7 +208,7 @@ public class GangliaPropertyProviderTest {
 
     // namenode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
 
@@ -131,12 +224,13 @@ public class GangliaPropertyProviderTest {
         "://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPDataNode%2CHDPSlaves&h=domU-12-31-39-0E-34-E1.compute-1.internal&m=jvm.metrics.gcCount&s=10&e=20&r=1";
     Assert.assertEquals(expected, streamProvider.getLastSpec());
 
    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(4, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID));
 
 
     // tasktracker
     resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "TASKTRACKER");
 
@@ -194,7 +288,7 @@ public class GangliaPropertyProviderTest {
     
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
 
    Assert.assertEquals(6, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(7, PropertyHelper.getProperties(resource).size());
 
     Assert.assertNotNull(resource.getPropertyValue(shuffle_exceptions_caught));
 
@@ -211,7 +305,6 @@ public class GangliaPropertyProviderTest {
     Assert.assertNotNull(resource.getPropertyValue(shuffle_success_outputs));
   }
   
  @Test
   public void testPopulateResources_checkHostComponent() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
     MetricHostProvider hostProvider =  PowerMock.createPartialMock(MetricHostProvider.class,
@@ -228,7 +321,7 @@ public class GangliaPropertyProviderTest {
 
     // datanode
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
 
@@ -252,7 +345,6 @@ public class GangliaPropertyProviderTest {
     
   }
 
  @Test
   public void testPopulateResources_checkHost() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("host_temporal_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -268,6 +360,7 @@ public class GangliaPropertyProviderTest {
 
     // host
     Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "corp-hadoopda05.client.ext");
 
     // only ask for one property
@@ -285,7 +378,6 @@ public class GangliaPropertyProviderTest {
     Assert.assertEquals(226, val.length);
   }
 
  @Test
   public void testPopulateManyResources() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data_1.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -303,14 +395,17 @@ public class GangliaPropertyProviderTest {
 
     // host
     Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
     resources.add(resource);
 
     resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E2.compute-1.internal");
     resources.add(resource);
 
     resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E3.compute-1.internal");
     resources.add(resource);
 
@@ -348,12 +443,11 @@ public class GangliaPropertyProviderTest {
     Assert.assertEquals(expected.substring(369 + httpsVariation), streamProvider.getLastSpec().substring(369 + httpsVariation));
 
     for (Resource res : resources) {
      Assert.assertEquals(2, PropertyHelper.getProperties(res).size());
      Assert.assertEquals(3, PropertyHelper.getProperties(res).size());
       Assert.assertNotNull(res.getPropertyValue(PROPERTY_ID));
     }
   }
 
  @Test
   public void testPopulateResources__LargeNumberOfHostResources() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -373,6 +467,7 @@ public class GangliaPropertyProviderTest {
     
     for (int i = 0; i < 150; ++i) {
       Resource resource = new ResourceImpl(Resource.Type.Host);
      resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
       resource.setProperty(HOST_NAME_PROPERTY_ID, "host" + i);
       resources.add(resource);
       
@@ -412,7 +507,6 @@ public class GangliaPropertyProviderTest {
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
   }
   
  @Test
   public void testPopulateResources_params() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -430,6 +524,7 @@ public class GangliaPropertyProviderTest {
     // flume
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");
 
@@ -466,11 +561,10 @@ public class GangliaPropertyProviderTest {
     
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
     
    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(4, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
   }
 
  @Test
   public void testPopulateResources_paramsMixed() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -488,6 +582,7 @@ public class GangliaPropertyProviderTest {
     // flume
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");
 
@@ -528,12 +623,11 @@ public class GangliaPropertyProviderTest {
     
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
        
    Assert.assertEquals(22, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(23, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID2));
     Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
   }
 
  @Test
   public void testPopulateResources_paramsAll() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -550,6 +644,7 @@ public class GangliaPropertyProviderTest {
     // flume
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");
 
@@ -569,11 +664,10 @@ public class GangliaPropertyProviderTest {
     Assert.assertTrue(CollectionPresentationUtils.isStringPermutationOfCollection(streamProvider.getLastSpec().substring(66 + httpsVariation, 92 + httpsVariation), components, "%2C", 0, 0));
     Assert.assertTrue(streamProvider.getLastSpec().substring(92 + httpsVariation).startsWith(expected.substring(92 + httpsVariation)));
 
    Assert.assertEquals(33, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(34, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
   }
 
  @Test
   public void testPopulateResources_params_category1() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -591,6 +685,7 @@ public class GangliaPropertyProviderTest {
     // flume
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");
 
@@ -627,11 +722,10 @@ public class GangliaPropertyProviderTest {
     
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
 
    Assert.assertEquals(21, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(22, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
   }
 
  @Test
   public void testPopulateResources_params_category2() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -649,6 +743,7 @@ public class GangliaPropertyProviderTest {
     // flume
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");
 
@@ -685,11 +780,10 @@ public class GangliaPropertyProviderTest {
     
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
 
    Assert.assertEquals(21, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(22, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
   }
 
  @Test
   public void testPopulateResources_params_category3() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -707,6 +801,7 @@ public class GangliaPropertyProviderTest {
     // flume
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");
 
@@ -744,11 +839,10 @@ public class GangliaPropertyProviderTest {
     
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
 
    Assert.assertEquals(11, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(12, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
   }
 
  @Test
   public void testPopulateResources_params_category4() throws Exception {
     TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
     TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
@@ -766,6 +860,7 @@ public class GangliaPropertyProviderTest {
     // flume
     Resource resource = new ResourceImpl(Resource.Type.HostComponent);
 
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
     resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
     resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");
 
@@ -803,7 +898,7 @@ public class GangliaPropertyProviderTest {
     
     Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
     
    Assert.assertEquals(11, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(12, PropertyHelper.getProperties(resource).size());
     Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
   }
 
@@ -855,6 +950,25 @@ public class GangliaPropertyProviderTest {
     return metricsBuilder.toString();
   }
 
  private void setUpCommonMocks() throws AmbariException, NoSuchFieldException, IllegalAccessException {
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();
    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    replay(amc, clusters, cluster);
    PowerMock.replayAll();
  }

   public static class TestGangliaServiceProvider implements MetricsServiceProvider {
 
     @Override
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/timeline/AMSPropertyProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/timeline/AMSPropertyProviderTest.java
index 6b5926b47d..3c72dbf2e4 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/timeline/AMSPropertyProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/metrics/timeline/AMSPropertyProviderTest.java
@@ -7,7 +7,7 @@
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
@@ -29,6 +29,7 @@ import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
 import org.apache.ambari.server.controller.internal.URLStreamProvider;
 import org.apache.ambari.server.controller.metrics.MetricHostProvider;
 import org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCache;
 import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheEntryFactory;
 import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
 import org.apache.ambari.server.controller.spi.Request;
@@ -36,26 +37,29 @@ import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.spi.TemporalInfo;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.ComponentInfo;
 import org.apache.ambari.server.state.StackId;
 import org.apache.http.client.utils.URIBuilder;
import org.easymock.EasyMock;
import org.junit.After;
 import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.powermock.api.easymock.PowerMock;
 import org.powermock.core.classloader.annotations.PowerMockIgnore;
 import org.powermock.core.classloader.annotations.PrepareForTest;
 import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.context.SecurityContextHolder;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
@@ -91,28 +95,104 @@ public class AMSPropertyProviderTest {
   private static final String AGGREGATE_METRICS_FILE_PATH = FILE_PATH_PREFIX + "aggregate_component_metric.json";
 
   private static TimelineMetricCacheEntryFactory cacheEntryFactory;
  private static TimelineMetricCacheProvider cacheProvider;
 
  @BeforeClass
  public static void setupCache() {
  @Before
  public void setupCache() {
     cacheEntryFactory = new TimelineMetricCacheEntryFactory(new Configuration());
    cacheProvider = new TimelineMetricCacheProvider(new Configuration(), cacheEntryFactory);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testAMSPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));

    SecurityContextHolder.getContext();
    testPopulateResourcesForSingleHostMetric();
    testPopulateResourcesForSingleHostMetricPointInTime();
    testPopulateResourcesForMultipleHostMetricscPointInTime();
    testPopulateResourcesForMultipleHostMetrics();
    testPopulateResourcesForRegexpMetrics();
    testPopulateResourcesForSingleComponentMetric();
    testPopulateMetricsForEmbeddedHBase();
    testAggregateFunctionForComponentMetrics();
    testFilterOutOfBandMetricData();
    testPopulateResourcesForHostComponentHostMetrics();
   }
 
   @Test
  public void testAMSPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));

    testPopulateResourcesForSingleHostMetric();
    testPopulateResourcesForSingleHostMetricPointInTime();
    testPopulateResourcesForMultipleHostMetricscPointInTime();
    testPopulateResourcesForMultipleHostMetrics();
    testPopulateResourcesForRegexpMetrics();
    testPopulateResourcesForSingleComponentMetric();
    testPopulateMetricsForEmbeddedHBase();
    testAggregateFunctionForComponentMetrics();
    testFilterOutOfBandMetricData();
    testPopulateResourcesForHostComponentHostMetrics();
  }

  @Test
  public void testAMSPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));

    testPopulateResourcesForSingleHostMetric();
    testPopulateResourcesForSingleHostMetricPointInTime();
    testPopulateResourcesForMultipleHostMetricscPointInTime();
    testPopulateResourcesForMultipleHostMetrics();
    testPopulateResourcesForRegexpMetrics();
    testPopulateResourcesForSingleComponentMetric();
    testPopulateMetricsForEmbeddedHBase();
    testAggregateFunctionForComponentMetrics();
    testFilterOutOfBandMetricData();
    testPopulateResourcesForHostComponentHostMetrics();
  }

  @Test(expected = AuthorizationException.class)
  public void testAMSPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));

    testPopulateResourcesForSingleHostMetric();
    testPopulateResourcesForSingleHostMetricPointInTime();
    testPopulateResourcesForMultipleHostMetricscPointInTime();
    testPopulateResourcesForMultipleHostMetrics();
    testPopulateResourcesForRegexpMetrics();
    testPopulateResourcesForSingleComponentMetric();
    testPopulateMetricsForEmbeddedHBase();
    testAggregateFunctionForComponentMetrics();
    testFilterOutOfBandMetricData();
    testPopulateResourcesForHostComponentHostMetrics();
  }

   public void testPopulateResourcesForSingleHostMetric() throws Exception {
     setUpCommonMocks();
     TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
     AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       HOST_NAME_PROPERTY_ID
@@ -142,7 +222,6 @@ public class AMSPropertyProviderTest {
     Assert.assertEquals(111, val.length);
   }
 
  @Test
   public void testPopulateResourcesForSingleHostMetricPointInTime() throws Exception {
     setUpCommonMocks();
 
@@ -152,11 +231,15 @@ public class AMSPropertyProviderTest {
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
     Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

     AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       HOST_NAME_PROPERTY_ID
@@ -186,20 +269,22 @@ public class AMSPropertyProviderTest {
     Assert.assertEquals(41.088, val, 0.001);
   }
 
  @Test
   public void testPopulateResourcesForMultipleHostMetricscPointInTime() throws Exception {
     setUpCommonMocks();
     TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_HOST_METRICS_FILE_PATH);
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
     AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       HOST_NAME_PROPERTY_ID
@@ -210,7 +295,10 @@ public class AMSPropertyProviderTest {
     resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
     Map<String, TemporalInfo> temporalInfoMap = Collections.emptyMap();
     Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{ add(PROPERTY_ID1); add(PROPERTY_ID2); }}, temporalInfoMap);
      new HashSet<String>() {{
        add(PROPERTY_ID1);
        add(PROPERTY_ID2);
      }}, temporalInfoMap);
     Set<Resource> resources =
       propertyProvider.populateResources(Collections.singleton(resource), request, null);
     Assert.assertEquals(1, resources.size());
@@ -227,29 +315,31 @@ public class AMSPropertyProviderTest {
     uriBuilder2.addParameter("hostname", "h1");
     uriBuilder2.addParameter("appId", "HOST");
     Assert.assertTrue(uriBuilder.toString().equals(streamProvider.getLastSpec())
        || uriBuilder2.toString().equals(streamProvider.getLastSpec()));
      || uriBuilder2.toString().equals(streamProvider.getLastSpec()));
     Double val1 = (Double) res.getPropertyValue(PROPERTY_ID1);
     Assert.assertNotNull("No value for property " + PROPERTY_ID1, val1);
     Assert.assertEquals(41.088, val1, 0.001);
    Double val2 = (Double)res.getPropertyValue(PROPERTY_ID2);
    Double val2 = (Double) res.getPropertyValue(PROPERTY_ID2);
     Assert.assertNotNull("No value for property " + PROPERTY_ID2, val2);
     Assert.assertEquals(2.47025664E8, val2, 0.1);
   }
 
  @Test
   public void testPopulateResourcesForMultipleHostMetrics() throws Exception {
     setUpCommonMocks();
     TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_HOST_METRICS_FILE_PATH);
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
     AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       HOST_NAME_PROPERTY_ID
@@ -297,30 +387,32 @@ public class AMSPropertyProviderTest {
     Assert.assertEquals(86, val.length);
   }
 
  @Test
   public void testPopulateResourcesForRegexpMetrics() throws Exception {
     setUpCommonMocks();
     TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_COMPONENT_REGEXP_METRICS_FILE_PATH);
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds =
        new HashMap<String, Map<String, PropertyInfo>>() {{
      put("RESOURCEMANAGER", new HashMap<String, PropertyInfo>() {{
        put("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AvailableMB",
      new HashMap<String, Map<String, PropertyInfo>>() {{
        put("RESOURCEMANAGER", new HashMap<String, PropertyInfo>() {{
          put("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AvailableMB",
             new PropertyInfo("yarn.QueueMetrics.Queue=(.+).AvailableMB", true, false));
      }});
    }};
        }});
      }};
 
     AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
        propertyIds,
        streamProvider,
        sslConfiguration,
        cacheProvider,
        metricHostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
     );
 
 
@@ -332,9 +424,9 @@ public class AMSPropertyProviderTest {
     Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
     temporalInfoMap.put(propertyId1, new TemporalInfoImpl(1416528759233L, 1416531129231L, 1L));
     Request request = PropertyHelper.getReadRequest(
        Collections.singleton(propertyId1), temporalInfoMap);
      Collections.singleton(propertyId1), temporalInfoMap);
     Set<Resource> resources =
        propertyProvider.populateResources(Collections.singleton(resource), request, null);
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
     Assert.assertEquals(1, resources.size());
     Resource res = resources.iterator().next();
     Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
@@ -350,13 +442,15 @@ public class AMSPropertyProviderTest {
     Assert.assertEquals(238, val.length);
   }
 
  @Test
   public void testPopulateResourcesForSingleComponentMetric() throws Exception {
     setUpCommonMocks();
     TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_COMPONENT_METRICS_FILE_PATH);
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds =
       PropertyHelper.getMetricPropertyIds(Resource.Type.Component);
@@ -365,7 +459,7 @@ public class AMSPropertyProviderTest {
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       COMPONENT_NAME_PROPERTY_ID
@@ -397,17 +491,19 @@ public class AMSPropertyProviderTest {
     Assert.assertEquals(238, val.length);
   }
 
  @Test
   public void testPopulateMetricsForEmbeddedHBase() throws Exception {
     AmbariManagementController ams = createNiceMock(AmbariManagementController.class);
     PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(ams);
    expect(AmbariServer.getController()).andReturn(ams).anyTimes();
     AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
     Clusters clusters = createNiceMock(Clusters.class);
     Cluster cluster = createNiceMock(Cluster.class);
     ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId= new StackId("HDP","2.2");
     expect(ams.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("HostRoles/cluster_name")).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();

    StackId stackId = new StackId("HDP", "2.2");
     try {
       expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
     } catch (AmbariException e) {
@@ -417,7 +513,7 @@ public class AMSPropertyProviderTest {
     expect(ams.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
     expect(ambariMetaInfo.getComponentToService("HDP", "2.2", "METRICS_COLLECTOR")).andReturn("AMS").anyTimes();
     expect(ambariMetaInfo.getComponent("HDP", "2.2", "AMS", "METRICS_COLLECTOR"))
            .andReturn(componentInfo).anyTimes();
      .andReturn(componentInfo).anyTimes();
     expect(componentInfo.getTimelineAppid()).andReturn("AMS-HBASE");
     replay(ams, clusters, cluster, ambariMetaInfo, componentInfo);
     PowerMock.replayAll();
@@ -426,6 +522,9 @@ public class AMSPropertyProviderTest {
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds =
       PropertyHelper.getMetricPropertyIds(Resource.Type.Component);
@@ -434,7 +533,7 @@ public class AMSPropertyProviderTest {
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       COMPONENT_NAME_PROPERTY_ID
@@ -465,17 +564,19 @@ public class AMSPropertyProviderTest {
     Assert.assertEquals(189, val.length);
   }
 
  @Test
   public void testAggregateFunctionForComponentMetrics() throws Exception {
     AmbariManagementController ams = createNiceMock(AmbariManagementController.class);
     PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(ams);
    expect(AmbariServer.getController()).andReturn(ams).anyTimes();
     AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
     Clusters clusters = createNiceMock(Clusters.class);
     Cluster cluster = createNiceMock(Cluster.class);
     ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId= new StackId("HDP","2.2");
    StackId stackId = new StackId("HDP", "2.2");
     expect(ams.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("HostRoles/cluster_name")).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();

     try {
       expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
     } catch (AmbariException e) {
@@ -485,7 +586,7 @@ public class AMSPropertyProviderTest {
     expect(ams.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
     expect(ambariMetaInfo.getComponentToService("HDP", "2.2", "HBASE_REGIONSERVER")).andReturn("HBASE").anyTimes();
     expect(ambariMetaInfo.getComponent("HDP", "2.2", "HBASE", "HBASE_REGIONSERVER"))
            .andReturn(componentInfo).anyTimes();
      .andReturn(componentInfo).anyTimes();
     expect(componentInfo.getTimelineAppid()).andReturn("HBASE");
     replay(ams, clusters, cluster, ambariMetaInfo, componentInfo);
     PowerMock.replayAll();
@@ -494,6 +595,9 @@ public class AMSPropertyProviderTest {
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds =
       PropertyHelper.getMetricPropertyIds(Resource.Type.Component);
@@ -503,7 +607,7 @@ public class AMSPropertyProviderTest {
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       COMPONENT_NAME_PROPERTY_ID
@@ -533,20 +637,22 @@ public class AMSPropertyProviderTest {
     Assert.assertEquals(32, val.length);
   }
 
  @Test
   public void testFilterOutOfBandMetricData() throws Exception {
     setUpCommonMocks();
     TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
     AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       HOST_NAME_PROPERTY_ID
@@ -601,7 +707,6 @@ public class AMSPropertyProviderTest {
     }
   }
 
  @Test
   public void testPopulateResourcesForHostComponentHostMetrics() throws Exception {
     setUpCommonMocks();
     TestStreamProviderForHostComponentHostMetricsTest streamProvider =
@@ -609,13 +714,16 @@ public class AMSPropertyProviderTest {
     injectCacheEntryFactoryWithStreamProvider(streamProvider);
     TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
     ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
 
     Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
     AMSPropertyProvider propertyProvider = new AMSHostComponentPropertyProvider(
       propertyIds,
       streamProvider,
       sslConfiguration,
      cacheProvider,
      cacheProviderMock,
       metricHostProvider,
       CLUSTER_NAME_PROPERTY_ID,
       HOST_NAME_PROPERTY_ID,
@@ -711,30 +819,23 @@ public class AMSPropertyProviderTest {
     }
   }
 
  // Helper function to setup common Mocks.
   private void setUpCommonMocks() throws AmbariException {
    AmbariManagementController ams = createNiceMock(AmbariManagementController.class);

    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
     PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(ams);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    expect(AmbariServer.getController()).andReturn(amc).anyTimes();
     Clusters clusters = createNiceMock(Clusters.class);
     Cluster cluster = createNiceMock(Cluster.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId= new StackId("HDP","2.2");
    expect(ams.getClusters()).andReturn(clusters).anyTimes();
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();
     try {
       expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
     } catch (AmbariException e) {
       e.printStackTrace();
     }
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(ams.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariMetaInfo.getComponentToService(anyObject(String.class),
            anyObject(String.class), anyObject(String.class))).andReturn("HDFS").anyTimes();
    expect(ambariMetaInfo.getComponent(anyObject(String.class),anyObject(String.class),
            anyObject(String.class), anyObject(String.class)))
            .andReturn(componentInfo).anyTimes();

    replay(ams, clusters, cluster, ambariMetaInfo);
    replay(amc, clusters, cluster);
     PowerMock.replayAll();
   }
 
- 
2.19.1.windows.1

