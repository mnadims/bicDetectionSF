From 268f5cb65244fd0de540c676bfc135b661f5bb1a Mon Sep 17 00:00:00 2001
From: Ajit Kumar <ajit@apache.org>
Date: Fri, 5 Aug 2016 12:14:22 -0700
Subject: [PATCH] AMBARI-18011. API for bulk delete hostcomponents (ajit)

--
 .../api/services/HostComponentService.java    |   6 +-
 .../AmbariManagementController.java           |   3 +-
 .../AmbariManagementControllerImpl.java       | 189 ++++++++++--------
 .../HostComponentResourceProvider.java        |   8 +-
 .../AmbariManagementControllerTest.java       |  53 ++---
 .../HostComponentResourceProviderTest.java    |   8 +-
 6 files changed, 146 insertions(+), 121 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/HostComponentService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/HostComponentService.java
index 4990ad71c7..72351c3a01 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/HostComponentService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/HostComponentService.java
@@ -215,12 +215,12 @@ public class HostComponentService extends BaseService {
    */
   @DELETE
   @Produces("text/plain")
  public Response deleteHostComponents(@Context HttpHeaders headers, @Context UriInfo ui) {
  public Response deleteHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
 
    return handleRequest(headers, null, ui, Request.Type.DELETE,
    return handleRequest(headers, body, ui, Request.Type.DELETE,
         createHostComponentResource(m_clusterName, m_hostName, null));
   }
  

   @GET
   @Path("{hostComponentName}/processes")
   @Produces("text/plain")
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
index 5cf2de7b95..9da6fd48b1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementController.java
@@ -28,6 +28,7 @@ import org.apache.ambari.server.RoleCommand;
 import org.apache.ambari.server.actionmanager.ActionManager;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
 import org.apache.ambari.server.controller.internal.RequestStageContainer;
 import org.apache.ambari.server.controller.logging.LoggingSearchPropertyProvider;
 import org.apache.ambari.server.controller.metrics.MetricPropertyProviderFactory;
@@ -296,7 +297,7 @@ public interface AmbariManagementController {
    *
    * @throws AmbariException thrown if the resource cannot be deleted
    */
  RequestStatusResponse deleteHostComponents(
  DeleteStatusMetaData deleteHostComponents(
       Set<ServiceComponentHostRequest> requests) throws AmbariException, AuthorizationException;
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index 075b85a806..95a14d5dc1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -89,6 +89,7 @@ import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
 import org.apache.ambari.server.controller.internal.RequestOperationLevel;
 import org.apache.ambari.server.controller.internal.RequestResourceFilter;
 import org.apache.ambari.server.controller.internal.RequestStageContainer;
@@ -3123,6 +3124,30 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
     }
   }
 
  private void checkIfHostComponentsInDeleteFriendlyState(ServiceComponentHostRequest request, Cluster cluster) throws AmbariException {
    Service service = cluster.getService(request.getServiceName());
    ServiceComponent component = service.getServiceComponent(request.getComponentName());
    ServiceComponentHost componentHost = component.getServiceComponentHost(request.getHostname());

    if (!componentHost.canBeRemoved()) {
      throw new AmbariException("Host Component cannot be removed"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + request.getServiceName()
              + ", componentName=" + request.getComponentName()
              + ", hostname=" + request.getHostname()
              + ", request=" + request);
    }

    // Only allow removing master/slave components in DISABLED/UNKNOWN/INSTALL_FAILED/INIT state without stages
    // generation.
    // Clients may be removed without a state check.
    if (!component.isClientComponent() &&
            !componentHost.getState().isRemovableState()) {
      throw new AmbariException("To remove master or slave components they must be in " +
              "DISABLED/INIT/INSTALLED/INSTALL_FAILED/UNKNOWN state. Current=" + componentHost.getState() + ".");
    }
  }

   @Override
   public String findServiceName(Cluster cluster, String componentName) throws AmbariException {
     StackId stackId = cluster.getDesiredStackVersion();
@@ -3224,10 +3249,10 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
   }
 
   @Override
  public RequestStatusResponse deleteHostComponents(
  public DeleteStatusMetaData deleteHostComponents(
       Set<ServiceComponentHostRequest> requests) throws AmbariException, AuthorizationException {
 
    Set<ServiceComponentHostRequest> expanded = new HashSet<ServiceComponentHostRequest>();
    Set<ServiceComponentHostRequest> expanded = new HashSet<>();
 
     // if any request are for the whole host, they need to be expanded
     for (ServiceComponentHostRequest request : requests) {
@@ -3254,7 +3279,8 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       }
     }
 
    Map<ServiceComponent, Set<ServiceComponentHost>> safeToRemoveSCHs = new HashMap<ServiceComponent, Set<ServiceComponentHost>>();
    Map<ServiceComponent, Set<ServiceComponentHost>> safeToRemoveSCHs = new HashMap<>();
    DeleteStatusMetaData deleteStatusMetaData = new DeleteStatusMetaData();
 
     for (ServiceComponentHostRequest request : expanded) {
 
@@ -3279,88 +3305,41 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       ServiceComponent component = service.getServiceComponent(request.getComponentName());
       ServiceComponentHost componentHost = component.getServiceComponentHost(request.getHostname());
 
      if (!componentHost.canBeRemoved()) {
        throw new AmbariException("Host Component cannot be removed"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      // Only allow removing master/slave components in DISABLED/UNKNOWN/INSTALL_FAILED/INIT state without stages
      // generation.
      // Clients may be removed without a state check.
      if (!component.isClientComponent() &&
          !componentHost.getState().isRemovableState()) {
        throw new AmbariException("To remove master or slave components they must be in " +
            "DISABLED/INIT/INSTALLED/INSTALL_FAILED/UNKNOWN state. Current=" + componentHost.getState() + ".");
      }

       setRestartRequiredServices(service, request.getComponentName());

      if (!safeToRemoveSCHs.containsKey(component)) {
        safeToRemoveSCHs.put(component, new HashSet<ServiceComponentHost>());
      try {
        checkIfHostComponentsInDeleteFriendlyState(request, cluster);
        if (!safeToRemoveSCHs.containsKey(component)) {
          safeToRemoveSCHs.put(component, new HashSet<ServiceComponentHost>());
        }
        safeToRemoveSCHs.get(component).add(componentHost);
      } catch (Exception ex) {
        deleteStatusMetaData.addException(request.getHostname() + "/" + request.getComponentName(), ex);
       }
      safeToRemoveSCHs.get(component).add(componentHost);
     }
 
    for (Entry<ServiceComponent, Set<ServiceComponentHost>> entry
            : safeToRemoveSCHs.entrySet()) {
    for (Entry<ServiceComponent, Set<ServiceComponentHost>> entry : safeToRemoveSCHs.entrySet()) {
       for (ServiceComponentHost componentHost : entry.getValue()) {
        String included_hostname = componentHost.getHostName();
        String serviceName = entry.getKey().getServiceName();
        String master_component_name = null;
        String slave_component_name = componentHost.getServiceComponentName();
        HostComponentAdminState desiredAdminState = componentHost.getComponentAdminState();
        State slaveState = componentHost.getState();
        //Delete hostcomponents
        entry.getKey().deleteServiceComponentHosts(componentHost.getHostName());
        // If deleted hostcomponents support decomission and were decommited and stopped
        if (AmbariCustomCommandExecutionHelper.masterToSlaveMappingForDecom.containsValue(slave_component_name)
                && desiredAdminState.equals(HostComponentAdminState.DECOMMISSIONED)
                && slaveState.equals(State.INSTALLED)) {

          for (Entry<String, String> entrySet : AmbariCustomCommandExecutionHelper.masterToSlaveMappingForDecom.entrySet()) {
            if (entrySet.getValue().equals(slave_component_name)) {
              master_component_name = entrySet.getKey();
            }
          }
          //Clear exclud file or draining list except HBASE
          if (!serviceName.equals(Service.Type.HBASE.toString())) {
            HashMap<String, String> requestProperties = new HashMap<String, String>();
            requestProperties.put("context", "Remove host " +
                    included_hostname + " from exclude file");
            requestProperties.put("exclusive", "true");
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("included_hosts", included_hostname);
            params.put("slave_type", slave_component_name);
            params.put(AmbariCustomCommandExecutionHelper.UPDATE_EXCLUDE_FILE_ONLY, "true");

            //Create filter for RECOMISSION command
            RequestResourceFilter resourceFilter
                    = new RequestResourceFilter(serviceName, master_component_name, null);
            //Create request for RECOMISSION command
            ExecuteActionRequest actionRequest = new ExecuteActionRequest(
                    entry.getKey().getClusterName(), AmbariCustomCommandExecutionHelper.DECOMMISSION_COMMAND_NAME, null,
                    Collections.singletonList(resourceFilter), null, params, true);
            //Send request
            createAction(actionRequest, requestProperties);
          }

          //Mark master component as needed to restart for remove host info from components UI
          Cluster cluster = clusters.getCluster(entry.getKey().getClusterName());
          Service service = cluster.getService(serviceName);
          ServiceComponent sc = service.getServiceComponent(master_component_name);

          if (sc != null && sc.isMasterComponent()) {
            for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
              sch.setRestartRequired(true);
            }
          }
        try {
          deleteHostComponent(entry.getKey(), componentHost);
          deleteStatusMetaData.addDeletedKey(componentHost.getHostName() + "/" + componentHost.getServiceComponentName());
 
        } catch (Exception ex) {
          deleteStatusMetaData.addException(componentHost.getHostName() + "/" + componentHost.getServiceComponentName(), ex);
         }
      }
    }
 
    //Do not break behavior for existing clients where delete request contains only 1 host component.
    //Response for these requests will have empty body with appropriate error code.
    if (deleteStatusMetaData.getDeletedKeys().size() + deleteStatusMetaData.getExceptionForKeys().size() == 1) {
      if (deleteStatusMetaData.getDeletedKeys().size() == 1) {
        return null;
      }
      Exception ex =  deleteStatusMetaData.getExceptionForKeys().values().iterator().next();
      if (ex instanceof AmbariException) {
        throw (AmbariException)ex;
      } else {
        throw new AmbariException(ex.getMessage(), ex);
       }
     }
 
@@ -3368,7 +3347,61 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
     if (!safeToRemoveSCHs.isEmpty()) {
       setMonitoringServicesRestartRequired(requests);
     }
    return null;
    return deleteStatusMetaData;
  }

  private void deleteHostComponent(ServiceComponent serviceComponent, ServiceComponentHost componentHost) throws AmbariException {
    String included_hostname = componentHost.getHostName();
    String serviceName = serviceComponent.getServiceName();
    String master_component_name = null;
    String slave_component_name = componentHost.getServiceComponentName();
    HostComponentAdminState desiredAdminState = componentHost.getComponentAdminState();
    State slaveState = componentHost.getState();
    //Delete hostcomponents
    serviceComponent.deleteServiceComponentHosts(componentHost.getHostName());
    // If deleted hostcomponents support decomission and were decommited and stopped
    if (AmbariCustomCommandExecutionHelper.masterToSlaveMappingForDecom.containsValue(slave_component_name)
            && desiredAdminState.equals(HostComponentAdminState.DECOMMISSIONED)
            && slaveState.equals(State.INSTALLED)) {

      for (Entry<String, String> entrySet : AmbariCustomCommandExecutionHelper.masterToSlaveMappingForDecom.entrySet()) {
        if (entrySet.getValue().equals(slave_component_name)) {
          master_component_name = entrySet.getKey();
        }
      }
      //Clear exclud file or draining list except HBASE
      if (!serviceName.equals(Service.Type.HBASE.toString())) {
        HashMap<String, String> requestProperties = new HashMap<String, String>();
        requestProperties.put("context", "Remove host " +
                included_hostname + " from exclude file");
        requestProperties.put("exclusive", "true");
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("included_hosts", included_hostname);
        params.put("slave_type", slave_component_name);
        params.put(AmbariCustomCommandExecutionHelper.UPDATE_EXCLUDE_FILE_ONLY, "true");

        //Create filter for RECOMISSION command
        RequestResourceFilter resourceFilter
                = new RequestResourceFilter(serviceName, master_component_name, null);
        //Create request for RECOMISSION command
        ExecuteActionRequest actionRequest = new ExecuteActionRequest(
                serviceComponent.getClusterName(), AmbariCustomCommandExecutionHelper.DECOMMISSION_COMMAND_NAME, null,
                Collections.singletonList(resourceFilter), null, params, true);
        //Send request
        createAction(actionRequest, requestProperties);
      }

      //Mark master component as needed to restart for remove host info from components UI
      Cluster cluster = clusters.getCluster(serviceComponent.getClusterName());
      Service service = cluster.getService(serviceName);
      ServiceComponent sc = service.getServiceComponent(master_component_name);

      if (sc != null && sc.isMasterComponent()) {
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          sch.setRestartRequired(true);
        }
      }
    }
   }
 
   @Override
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
index df2b476ee0..4c840570f4 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
@@ -294,20 +294,20 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
   @Override
   protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
       throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    final Set<ServiceComponentHostRequest> requests = new HashSet<>();
     for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
       requests.add(changeRequest(propertyMap));
     }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
    DeleteStatusMetaData deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
       @Override
      public RequestStatusResponse invoke() throws AmbariException, AuthorizationException {
      public DeleteStatusMetaData invoke() throws AmbariException, AuthorizationException {
         return getManagementController().deleteHostComponents(requests);
       }
     });
 
     notifyDelete(Resource.Type.HostComponent, predicate);
 
    return getRequestStatus(response);
    return getRequestStatus(null, null, deleteStatusMetaData);
   }
 
   @Override
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
index 3ad1f1fc77..420c078a08 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
@@ -18,6 +18,7 @@
 
 package org.apache.ambari.server.controller;
 
import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
 import static org.easymock.EasyMock.capture;
 import static org.easymock.EasyMock.createNiceMock;
 import static org.easymock.EasyMock.createStrictMock;
@@ -8866,11 +8867,10 @@ public class AmbariManagementControllerTest {
   public void testDeleteHostComponentInVariousStates() throws Exception {
     String cluster1 = getUniqueName();
     createCluster(cluster1);
    clusters.getCluster(cluster1)
        .setDesiredStackVersion(new StackId("HDP-1.3.1"));
    String serviceName = "HDFS";
    clusters.getCluster(cluster1).setDesiredStackVersion(new StackId("HDP-1.3.1"));
    String hdfs = "HDFS";
     String mapred = "MAPREDUCE";
    createService(cluster1, serviceName, null);
    createService(cluster1, hdfs, null);
     createService(cluster1, mapred, null);
     String componentName1 = "NAMENODE";
     String componentName2 = "DATANODE";
@@ -8879,9 +8879,9 @@ public class AmbariManagementControllerTest {
     String componentName5 = "TASKTRACKER";
     String componentName6 = "MAPREDUCE_CLIENT";
 
    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3, State.INIT);
    createServiceComponent(cluster1, hdfs, componentName1, State.INIT);
    createServiceComponent(cluster1, hdfs, componentName2, State.INIT);
    createServiceComponent(cluster1, hdfs, componentName3, State.INIT);
     createServiceComponent(cluster1, mapred, componentName4, State.INIT);
     createServiceComponent(cluster1, mapred, componentName5, State.INIT);
     createServiceComponent(cluster1, mapred, componentName6, State.INIT);
@@ -8890,19 +8890,19 @@ public class AmbariManagementControllerTest {
 
     addHostToCluster(host1, cluster1);
 
    createServiceComponentHost(cluster1, serviceName, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host1, null);
    createServiceComponentHost(cluster1, hdfs, componentName1, host1, null);
    createServiceComponentHost(cluster1, hdfs, componentName2, host1, null);
    createServiceComponentHost(cluster1, hdfs, componentName3, host1, null);
     createServiceComponentHost(cluster1, mapred, componentName4, host1, null);
     createServiceComponentHost(cluster1, mapred, componentName5, host1, null);
     createServiceComponentHost(cluster1, mapred, componentName6, host1, null);
 
     // Install
    installService(cluster1, serviceName, false, false);
    installService(cluster1, hdfs, false, false);
     installService(cluster1, mapred, false, false);
 
     Cluster cluster = clusters.getCluster(cluster1);
    Service s1 = cluster.getService(serviceName);
    Service s1 = cluster.getService(hdfs);
     Service s2 = cluster.getService(mapred);
     ServiceComponent sc1 = s1.getServiceComponent(componentName1);
     sc1.getServiceComponentHosts().values().iterator().next().setState(State.STARTED);
@@ -8910,7 +8910,7 @@ public class AmbariManagementControllerTest {
     Set<ServiceComponentHostRequest> schRequests = new HashSet<ServiceComponentHostRequest>();
     // delete HC
     schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName1, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName1, host1, null));
     try {
       controller.deleteHostComponents(schRequests);
       Assert.fail("Expect failure while deleting.");
@@ -8934,13 +8934,14 @@ public class AmbariManagementControllerTest {
     sc6.getServiceComponentHosts().values().iterator().next().setState(State.INIT);
 
     schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName1, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName2, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName3, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName1, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName2, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName3, host1, null));
     schRequests.add(new ServiceComponentHostRequest(cluster1, mapred, componentName4, host1, null));
     schRequests.add(new ServiceComponentHostRequest(cluster1, mapred, componentName5, host1, null));
     schRequests.add(new ServiceComponentHostRequest(cluster1, mapred, componentName6, host1, null));
    controller.deleteHostComponents(schRequests);
    DeleteStatusMetaData deleteStatusMetaData = controller.deleteHostComponents(schRequests);
    Assert.assertEquals(0, deleteStatusMetaData.getExceptionForKeys().size());
   }
 
   @Test
@@ -9174,24 +9175,14 @@ public class AmbariManagementControllerTest {
 
     Set<ServiceComponentHostRequest> schRequests = new HashSet<ServiceComponentHostRequest>();
     schRequests.add(new ServiceComponentHostRequest(cluster1, null, null, host1, null));
    try {
      controller.deleteHostComponents(schRequests);
      fail("Expected exception while deleting all host components.");
    } catch (AmbariException e) {
    }
    Assert.assertEquals(3, cluster.getServiceComponentHosts(host1).size());

    DeleteStatusMetaData deleteStatusMetaData = controller.deleteHostComponents(schRequests);
    Assert.assertEquals(1, deleteStatusMetaData.getExceptionForKeys().size());
    Assert.assertEquals(1, cluster.getServiceComponentHosts(host1).size());
 
     sch.handleEvent(new ServiceComponentHostStopEvent(sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
     sch.handleEvent(new ServiceComponentHostStoppedEvent (sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
 
    schRequests.clear();
    // disable HC, DN was already stopped
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName1, host1, "DISABLED"));
    updateHostComponents(schRequests, new HashMap<String,String>(), false);

    // delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(cluster1, null, null, host1, null));
     controller.deleteHostComponents(schRequests);
 
     Assert.assertEquals(0, cluster.getServiceComponentHosts(host1).size());
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
index ef1b821ca0..5dc69e92e8 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
@@ -430,7 +430,7 @@ public class HostComponentResourceProviderTest {
     Resource.Type type = Resource.Type.HostComponent;
 
     AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    DeleteStatusMetaData deleteStatusMetaData = createNiceMock(DeleteStatusMetaData.class);
     Injector injector = createNiceMock(Injector.class);
 
     HostComponentResourceProvider provider =
@@ -441,10 +441,10 @@ public class HostComponentResourceProviderTest {
     // set expectations
     expect(managementController.deleteHostComponents(
         AbstractResourceProviderTest.Matcher.getHostComponentRequestSet(
            null, null, "Component100", "Host100", null, null))).andReturn(response);
            null, null, "Component100", "Host100", null, null))).andReturn(deleteStatusMetaData);
 
     // replay
    replay(managementController, response);
    replay(managementController, deleteStatusMetaData);
 
     SecurityContextHolder.getContext().setAuthentication(authentication);
 
@@ -466,7 +466,7 @@ public class HostComponentResourceProviderTest {
     Assert.assertNull(lastEvent.getRequest());
 
     // verify
    verify(managementController, response);
    verify(managementController, deleteStatusMetaData);
   }
 
   @Test
- 
2.19.1.windows.1

