From 4b4bcbfb67f2cbd18da39276a59de61d0540c2e6 Mon Sep 17 00:00:00 2001
From: Vitaly Brodetskyi <vbrodetskyi@hortonworks.com>
Date: Thu, 9 Apr 2015 01:09:51 +0300
Subject: [PATCH] AMBARI-4782. Error in getting host components with state
 INSTALL_FAILED.(vbrodetskyi)

--
 .../AmbariManagementControllerImpl.java       | 107 ++++++++------
 .../ServiceComponentHostRequest.java          |  16 +++
 .../HostComponentResourceProvider.java        | 131 +++++++++++++-----
 .../AmbariManagementControllerImplTest.java   |  76 ++++++++++
 4 files changed, 248 insertions(+), 82 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index f323920761..9e3d98d669 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -18,44 +18,14 @@
 
 package org.apache.ambari.server.controller;
 
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_DRIVER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_PASSWORD;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_USERNAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CLIENTS_TO_UPDATE_CONFIGS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.VERSION;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
 import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.ClusterNotFoundException;
 import org.apache.ambari.server.DuplicateResourceException;
@@ -144,13 +114,42 @@ import org.apache.http.client.utils.URIBuilder;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_DRIVER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_PASSWORD;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_USERNAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CLIENTS_TO_UPDATE_CONFIGS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.VERSION;
 
 @Singleton
 public class AmbariManagementControllerImpl implements AmbariManagementController {
@@ -964,6 +963,8 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
 
     boolean checkDesiredState = false;
     State desiredStateToCheck = null;
    boolean checkState = false;
    State stateToCheck = null;
     boolean filterBasedConfigStaleness = false;
     boolean staleConfig = true;
     if (request.getStaleConfig() != null) {
@@ -980,6 +981,15 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       checkDesiredState = true;
     }
 
    if (!StringUtils.isEmpty(request.getState())) {
      stateToCheck = State.valueOf(request.getState());
      // maybe check should be more wider
      if (stateToCheck == null) {
        throw new IllegalArgumentException("Invalid arguments, invalid state, State=" + request.getState());
      }
      checkState = true;
    }

     Map<String, Host> hosts = clusters.getHostsForCluster(cluster.getClusterName());
 
     for (Service s : services) {
@@ -1016,6 +1026,11 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
             if (checkDesiredState && (desiredStateToCheck != sch.getDesiredState())) {
               continue;
             }

            if (checkState && stateToCheck != sch.getState()) {
              continue;
            }

             if (request.getAdminState() != null) {
               String stringToMatch =
                   sch.getComponentAdminState() == null ? "" : sch.getComponentAdminState().name();
@@ -1058,6 +1073,10 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
               continue;
             }
 
            if (checkState && stateToCheck != sch.getState()) {
              continue;
            }

             if (request.getAdminState() != null) {
               String stringToMatch =
                   sch.getComponentAdminState() == null ? "" : sch.getComponentAdminState().name();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ServiceComponentHostRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ServiceComponentHostRequest.java
index 6536bd5333..73cedb4c9a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ServiceComponentHostRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ServiceComponentHostRequest.java
@@ -26,6 +26,7 @@ public class ServiceComponentHostRequest {
   private String serviceName;
   private String componentName;
   private String hostname;
  private String state;
   private String desiredState; // CREATE/UPDATE
   private String desiredStackId; // UPDATE
   private String staleConfig; // GET - predicate
@@ -101,6 +102,20 @@ public class ServiceComponentHostRequest {
     this.desiredState = desiredState;
   }
 
  /**
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * @param state the State to set
   */
  public void setState(String state) {
    this.state = state;
  }

   /**
    * @return the desiredStackId
    */
@@ -164,6 +179,7 @@ public class ServiceComponentHostRequest {
       .append(", componentName=").append(componentName)
       .append(", hostname=").append(hostname)
       .append(", desiredState=").append(desiredState)
      .append(", state=").append(state)
       .append(", desiredStackId=").append(desiredStackId)
       .append(", staleConfig=").append(staleConfig)
       .append(", adminState=").append(adminState).append("}");
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
index 639e17020c..ab5c31c84c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
@@ -17,20 +17,10 @@
  */
 package org.apache.ambari.server.controller.internal;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

 import com.google.inject.Inject;
 import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
 import org.apache.ambari.server.controller.AmbariManagementController;
@@ -53,9 +43,6 @@ import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.MaintenanceState;
@@ -68,6 +55,18 @@ import org.apache.ambari.server.state.svccomphost.ServiceComponentHostDisableEve
 import org.apache.ambari.server.state.svccomphost.ServiceComponentHostRestoreEvent;
 import org.apache.commons.lang.StringUtils;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

 /**
  * Resource provider for host component resources.
  */
@@ -163,7 +162,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
 
     final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
     for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
      requests.add(changeRequest(propertyMap));
     }
 
     createResources(new Command<Void>() {
@@ -189,6 +188,25 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
       requests.add(getRequest(propertyMap));
     }
 
    return findResources(request, predicate, requests);
  }

  private Set<Resource> getResourcesForUpdate(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(changeRequest(propertyMap));
    }

    return findResources(request, predicate, requests);
  }


  private Set<Resource> findResources(Request request, final Predicate predicate,
                                      final Set<ServiceComponentHostRequest> requests)
          throws SystemException, NoSuchResourceException, NoSuchParentResourceException {
     Set<Resource> resources = new HashSet<Resource>();
     Set<String> requestedIds = getRequestPropertyIds(request, predicate);
     // We always need host_name for sch
@@ -204,34 +222,34 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     for (ServiceComponentHostResponse response : responses) {
       Resource resource = new ResourceImpl(Resource.Type.HostComponent);
       setResourceProperty(resource, HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID,
          response.getClusterName(), requestedIds);
              response.getClusterName(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);
              response.getServiceName(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID,
          response.getComponentName(), requestedIds);
              response.getComponentName(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_HOST_NAME_PROPERTY_ID,
          response.getHostname(), requestedIds);
              response.getHostname(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_STATE_PROPERTY_ID,
          response.getLiveState(), requestedIds);
              response.getLiveState(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID,
          response.getDesiredState(), requestedIds);
              response.getDesiredState(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_STACK_ID_PROPERTY_ID,
          response.getStackVersion(), requestedIds);
              response.getStackVersion(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID,
          response.getDesiredStackVersion(), requestedIds);
              response.getDesiredStackVersion(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_ACTUAL_CONFIGS_PROPERTY_ID,
          response.getActualConfigs(), requestedIds);
              response.getActualConfigs(), requestedIds);
       setResourceProperty(resource, HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID,
          response.isStaleConfig(), requestedIds);
      
              response.isStaleConfig(), requestedIds);

       if (response.getAdminState() != null) {
         setResourceProperty(resource, HOST_COMPONENT_DESIRED_ADMIN_STATE_PROPERTY_ID,
            response.getAdminState(), requestedIds);
                response.getAdminState(), requestedIds);
       }
      

       if (null != response.getMaintenanceState()) {
         setResourceProperty(resource, HOST_COMPONENT_MAINTENANCE_STATE_PROPERTY_ID,
            response.getMaintenanceState(), requestedIds);
                response.getMaintenanceState(), requestedIds);
       }
 
       String componentName = (String) resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
@@ -276,7 +294,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
       throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
     final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
     for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
      requests.add(changeRequest(propertyMap));
     }
     RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
       @Override
@@ -316,7 +334,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     final RequestStageContainer requestStages;
     Map<String, Object> installProperties = new HashMap<String, Object>();
 
    installProperties.put(HOST_COMPONENT_STATE_PROPERTY_ID, "INSTALLED");
    installProperties.put(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, "INSTALLED");
     Map<String, String> requestInfo = new HashMap<String, String>();
     requestInfo.put("context", "Install and start components on added hosts");
     Request installRequest = PropertyHelper.getUpdateRequest(installProperties, requestInfo);
@@ -338,7 +356,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
       notifyUpdate(Resource.Type.HostComponent, installRequest, installPredicate);
 
       Map<String, Object> startProperties = new HashMap<String, Object>();
      startProperties.put(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
      startProperties.put(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, "STARTED");
       Request startRequest = PropertyHelper.getUpdateRequest(startProperties, requestInfo);
       // Important to query against desired_state as this has been updated when install stage was created
       // If I query against state, then the getRequest compares predicate prop against desired_state and then when the predicate
@@ -573,9 +591,9 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
         (String) properties.get(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID),
         (String) properties.get(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID),
         (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
    serviceComponentHostRequest.setDesiredStackId(
        (String) properties.get(HOST_COMPONENT_STACK_ID_PROPERTY_ID));
        (String) properties.get(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID));
    serviceComponentHostRequest.setState((String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
    serviceComponentHostRequest.setDesiredStackId((String) properties.get(HOST_COMPONENT_STACK_ID_PROPERTY_ID));
     if (properties.get(HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID) != null) {
       serviceComponentHostRequest.setStaleConfig(
           properties.get(HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID).toString().toLowerCase());
@@ -594,6 +612,42 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     return serviceComponentHostRequest;
   }
 
  /**
   * Put changes to component request object from a map of property values.
   *
   * @param properties the predicate
   * @return the component request object
   */
  private ServiceComponentHostRequest changeRequest(Map<String, Object> properties) {
    ServiceComponentHostRequest serviceComponentHostRequest = new ServiceComponentHostRequest(
            (String) properties.get(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID),
            (String) properties.get(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID),
            (String) properties.get(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID),
            (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
            (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
    if (properties.get(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID) != null) {
      serviceComponentHostRequest.setDesiredState((String)properties.get(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID));
    }
    serviceComponentHostRequest.setDesiredStackId(
            (String) properties.get(HOST_COMPONENT_STACK_ID_PROPERTY_ID));
    if (properties.get(HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID) != null) {
      serviceComponentHostRequest.setStaleConfig(
              properties.get(HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID).toString().toLowerCase());
    }

    if (properties.get(HOST_COMPONENT_DESIRED_ADMIN_STATE_PROPERTY_ID) != null) {
      serviceComponentHostRequest.setAdminState(
              properties.get(HOST_COMPONENT_DESIRED_ADMIN_STATE_PROPERTY_ID).toString());
    }

    Object o = properties.get(HOST_COMPONENT_MAINTENANCE_STATE_PROPERTY_ID);
    if (null != o) {
      serviceComponentHostRequest.setMaintenanceState (o.toString());
    }

    return serviceComponentHostRequest;
  }

   /**
    * Update resources.
    *
@@ -623,7 +677,8 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
 
     Request queryRequest = PropertyHelper.getReadRequest(queryIds);
     // will take care of 404 exception
    Set<Resource> matchingResources = getResources(queryRequest, predicate);

    Set<Resource> matchingResources = getResourcesForUpdate(queryRequest, predicate);
 
     for (Resource queryResource : matchingResources) {
       //todo: predicate evaluation was removed for BUG-28737 and the removal of this breaks
@@ -641,7 +696,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
         if (request.getProperties() != null && request.getProperties().size() != 0) {
           updateRequestProperties.putAll(request.getProperties().iterator().next());
         }
        requests.add(getRequest(updateRequestProperties));
        requests.add(changeRequest(updateRequestProperties));
       }
     }
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerImplTest.java
index f7144b8661..a71b2dba31 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerImplTest.java
@@ -56,6 +56,7 @@ import org.apache.ambari.server.state.ServiceComponentHost;
 import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.state.ServiceOsSpecific;
 import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
 import org.easymock.Capture;
 import org.junit.Before;
 import org.junit.Test;
@@ -923,6 +924,81 @@ public class AmbariManagementControllerImplTest {
     verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component);
   }
 
  @Test
  public void testGetHostComponents___ServiceComponentHostFilteredByState() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
            "cluster1", null, "component1", "host1", null);
    request1.setState("INSTALLED");


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class));
    expect(maintHelper.getEffectiveState(
            anyObject(ServiceComponentHost.class),
            anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
            new HashMap<String, Host>() {{
              put("host1", host);
            }}).anyTimes();

    expect(cluster.getDesiredStackVersion()).andReturn(stack);
    expect(cluster.getClusterName()).andReturn("cl1");
    expect(stack.getStackName()).andReturn("stackName");
    expect(stack.getStackVersion()).andReturn("stackVersion");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1").anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(new HashMap<String, ServiceComponentHost>() {{
      put("host1", componentHost1);
    }});

    expect(componentHost1.getState()).andReturn(State.INSTALLED);
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
            service, component, componentHost1, response1);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> responses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertTrue(responses.size() == 1);
    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component, componentHost1, response1);
  }

   @Test
   public void testGetHostComponents___OR_Predicate_ServiceComponentHostNotFoundException() throws Exception {
     // member state mocks
- 
2.19.1.windows.1

