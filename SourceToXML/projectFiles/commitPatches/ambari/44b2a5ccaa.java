From 44b2a5ccaa7b58878d19634acf364eb90a9ef34e Mon Sep 17 00:00:00 2001
From: John Speidel <jspeidel@hortonworks.com>
Date: Mon, 2 Feb 2015 16:44:10 -0500
Subject: [PATCH] AMBARI-9367. Fix regression of new high level "add hosts"
 api.              Also, ensure that kerberos client is added to hostgroups if
 kerberos is enabled.

--
 .../HostComponentResourceProvider.java        | 60 ++++++++++++-------
 .../internal/HostResourceProvider.java        | 28 ++++++++-
 .../HostComponentResourceProviderTest.java    | 31 +++++++---
 3 files changed, 90 insertions(+), 29 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
index b1e05cc299..b513de781f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
@@ -264,7 +264,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
       throw new IllegalArgumentException("Received an update request with no properties");
     }
 
    RequestStageContainer requestStages = doUpdateResources(null, request, predicate);
    RequestStageContainer requestStages = doUpdateResources(null, request, predicate, false);
 
     RequestStatusResponse response = null;
     if (requestStages != null) {
@@ -343,7 +343,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
 
     try {
       LOG.info("Installing all components on added hosts");
      requestStages = doUpdateResources(null, installRequest, installPredicate);
      requestStages = doUpdateResources(null, installRequest, installPredicate, true);
       notifyUpdate(Resource.Type.HostComponent, installRequest, installPredicate);
 
       Map<String, Object> startProperties = new HashMap<String, Object>();
@@ -362,7 +362,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
       LOG.info("Starting all non-client components on added hosts");
       //todo: if a host in in state HEARTBEAT_LOST, no stage will be created, so if this occurs during INSTALL
       //todo: then no INSTALL stage will exist which will result in invalid state transition INIT->STARTED
      doUpdateResources(requestStages, startRequest, startPredicate);
      doUpdateResources(requestStages, startRequest, startPredicate, true);
       notifyUpdate(Resource.Type.HostComponent, startRequest, startPredicate);
       try {
         requestStages.persist();
@@ -616,8 +616,25 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     return serviceComponentHostRequest;
   }
 
  private RequestStageContainer doUpdateResources(final RequestStageContainer stages, final Request request, Predicate predicate)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {
  /**
   * Update resources.
   *
   * @param stages                  request stage container
   * @param request                 request
   * @param predicate               request predicate
   * @param performQueryEvaluation  should query be evaluated for matching resource set
   * @return
   * @throws UnsupportedPropertyException   an unsupported property was specified in the request
   * @throws SystemException                an unknown exception occurred
   * @throws NoSuchResourceException        the query didn't match any resources
   * @throws NoSuchParentResourceException  a specified parent resource doesn't exist
   */
  private RequestStageContainer doUpdateResources(final RequestStageContainer stages, final Request request,
                                                  Predicate predicate, boolean performQueryEvaluation)
                                                  throws UnsupportedPropertyException,
                                                         SystemException,
                                                         NoSuchResourceException,
                                                         NoSuchParentResourceException {
 
     final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
 
@@ -631,23 +648,23 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     Set<Resource> matchingResources = getResources(queryRequest, predicate);
 
     for (Resource queryResource : matchingResources) {
      //todo: this was removed for BUG-28737 and the removal of this breaks
      //todo: the new "add hosts" api.  BUG-4818 is the root cause and needs
      //todo: to be addressed and then this predicate evaluation should be
      //todo: uncommented to fix "add hosts".
//    if (predicate.evaluate(queryResource)) {
      Map<String, Object> updateRequestProperties = new HashMap<String, Object>();

      // add props from query resource
      updateRequestProperties.putAll(PropertyHelper.getProperties(queryResource));

      // add properties from update request
      //todo: should we flag value size > 1?
      if (request.getProperties() != null && request.getProperties().size() != 0) {
        updateRequestProperties.putAll(request.getProperties().iterator().next());
      //todo: predicate evaluation was removed for BUG-28737 and the removal of this breaks
      //todo: the new "add hosts" api.  BUG-4818 is the root cause and needs to be addressed
      //todo: and then this predicate evaluation should always be performed and the
      //todo: temporary performQueryEvaluation flag hack should be removed.
      if (! performQueryEvaluation || predicate.evaluate(queryResource)) {
        Map<String, Object> updateRequestProperties = new HashMap<String, Object>();

        // add props from query resource
        updateRequestProperties.putAll(PropertyHelper.getProperties(queryResource));

        // add properties from update request
        //todo: should we flag value size > 1?
        if (request.getProperties() != null && request.getProperties().size() != 0) {
          updateRequestProperties.putAll(request.getProperties().iterator().next());
        }
        requests.add(getRequest(updateRequestProperties));
       }
      requests.add(getRequest(updateRequestProperties));
//    }
     }
 
     RequestStageContainer requestStages = modifyResources(new Command<RequestStageContainer>() {
@@ -662,6 +679,7 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     return requestStages;
   }
 

   /**
    * Determine whether a host component state change is valid.
    * Looks at projected state from the current stages associated with the request.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostResourceProvider.java
index b5d2d6da56..3a359e5a18 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostResourceProvider.java
@@ -56,6 +56,7 @@ import org.apache.ambari.server.state.Config;
 import org.apache.ambari.server.state.DesiredConfig;
 import org.apache.ambari.server.state.Host;
 import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.SecurityType;
 import org.apache.ambari.server.state.ServiceComponentHost;
 import org.apache.ambari.server.state.configgroup.ConfigGroup;
 import org.slf4j.Logger;
@@ -547,6 +548,7 @@ public class HostResourceProvider extends BaseBlueprintProcessor {
       BlueprintEntity blueprint = getExistingBlueprint(bpName);
       Stack stack = parseStack(blueprint);
       Map<String, HostGroupImpl> blueprintHostGroups = parseBlueprintHostGroups(blueprint, stack);
      addKerberosClientIfNecessary(clusterName, blueprintHostGroups);
       addHostToHostgroup(hgName, hostname, blueprintHostGroups);
       createHostAndComponentResources(blueprintHostGroups, clusterName, this);
       //todo: optimize: update once per hostgroup with added hosts
@@ -556,6 +558,31 @@ public class HostResourceProvider extends BaseBlueprintProcessor {
         installAndStart(clusterName, addedHosts);
   }
 
  /**
   * Add the kerberos client to groups if kerberos is enabled for the cluster.
   *
   * @param clusterName  cluster name
   * @param groups       host groups
   *
   * @throws NoSuchParentResourceException unable to get cluster instance
   */
  private void addKerberosClientIfNecessary(String clusterName, Map<String, HostGroupImpl> groups)
      throws NoSuchParentResourceException {

    //todo: logic would ideally be contained in the stack
    Cluster cluster;
    try {
      cluster = getManagementController().getClusters().getCluster(clusterName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException("Parent Cluster resource doesn't exist.  clusterName= " + clusterName);
    }
    if (cluster.getSecurityType() == SecurityType.KERBEROS) {
      for (HostGroupImpl group : groups.values()) {
        group.addComponent("KERBEROS_CLIENT");
      }
    }
  }

   /**
    * Add the new host to an existing config group.
    *
@@ -674,7 +701,6 @@ public class HostResourceProvider extends BaseBlueprintProcessor {
       }
     }
 

     for (Host h : hosts) {
       if (clusterName != null) {
         if (clusters.getClustersForHost(h.getHostName()).contains(cluster)) {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
index 337cc74857..8688e286c6 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
@@ -351,10 +351,12 @@ public class HostComponentResourceProviderTest {
     Cluster cluster = createNiceMock(Cluster.class);
     Service service = createNiceMock(Service.class);
     ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent clientComponent = createNiceMock(ServiceComponent.class);
     ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost clientComponentHost = createNiceMock(ServiceComponentHost.class);
     RequestStageContainer stageContainer = createNiceMock(RequestStageContainer.class);
     MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    // INIT->INSTALLED state transition causes check for kerverized cluster
    // INIT->INSTALLED state transition causes check for kerberized cluster
     KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
 
     Collection<String> hosts = new HashSet<String>();
@@ -366,28 +368,40 @@ public class HostComponentResourceProviderTest {
     Set<ServiceComponentHostResponse> nameResponse = new HashSet<ServiceComponentHostResponse>();
     nameResponse.add(new ServiceComponentHostResponse(
         "Cluster102", "Service100", "Component100", "Host100", "INIT", "", "INIT", "", null));
    nameResponse.add(new ServiceComponentHostResponse(
        "Cluster102", "Service100", "some-client", "Host100", "INIT", "", "INIT", "", null));
     Set<ServiceComponentHostResponse> nameResponse2 = new HashSet<ServiceComponentHostResponse>();
     nameResponse2.add(new ServiceComponentHostResponse(
         "Cluster102", "Service100", "Component100", "Host100", "INIT", "", "INSTALLED", "", null));
    nameResponse2.add(new ServiceComponentHostResponse(
        "Cluster102", "Service100", "some-client", "Host100", "INIT", "", "INSTALLED", "", null));
 
 
     // set expectations
     expect(managementController.getClusters()).andReturn(clusters).anyTimes();
     expect(managementController.findServiceName(cluster, "Component100")).andReturn("Service100").anyTimes();
    expect(managementController.findServiceName(cluster, "some-client")).andReturn("Service100").anyTimes();
     expect(clusters.getCluster("Cluster102")).andReturn(cluster).anyTimes();
     expect(cluster.getService("Service100")).andReturn(service).anyTimes();
     expect(service.getServiceComponent("Component100")).andReturn(component).anyTimes();
    expect(service.getServiceComponent("some-client")).andReturn(clientComponent).anyTimes();
     expect(component.getServiceComponentHost("Host100")).andReturn(componentHost).anyTimes();
     expect(component.getName()).andReturn("Component100").anyTimes();
    expect(clientComponent.getServiceComponentHost("Host100")).andReturn(clientComponentHost).anyTimes();
    expect(clientComponent.getName()).andReturn("some-client").anyTimes();
    expect(clientComponent.isClientComponent()).andReturn(true).anyTimes();
     // actual state is always INIT until stages actually execute
     expect(componentHost.getState()).andReturn(State.INIT).anyTimes();
     expect(componentHost.getHostName()).andReturn("Host100").anyTimes();
     expect(componentHost.getServiceComponentName()).andReturn("Component100").anyTimes();
    expect(clientComponentHost.getState()).andReturn(State.INIT).anyTimes();
    expect(clientComponentHost.getHostName()).andReturn("Host100").anyTimes();
    expect(clientComponentHost.getServiceComponentName()).andReturn("some-client").anyTimes();
     expect(response.getMessage()).andReturn("response msg").anyTimes();
 

     //Cluster is default type.  Maintenance mode is not being tested here so the default is returned.
     expect(maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, componentHost)).andReturn(true).anyTimes();
    expect(maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, clientComponentHost)).andReturn(true).anyTimes();
 
     //todo: can we change to prevent having to call twice?
     expect(managementController.getHostComponents(
@@ -397,8 +411,9 @@ public class HostComponentResourceProviderTest {
 
     Map<String, Map<State, List<ServiceComponentHost>>> changedHosts =
         new HashMap<String, Map<State, List<ServiceComponentHost>>>();
    List<ServiceComponentHost> changedComponentHosts = Collections.singletonList(componentHost);
    changedHosts.put("Component100", Collections.singletonMap(State.INSTALLED, changedComponentHosts));

    changedHosts.put("Component100", Collections.singletonMap(State.INSTALLED, Collections.singletonList(componentHost)));
    changedHosts.put("some-client", Collections.singletonMap(State.INSTALLED, Collections.singletonList(clientComponentHost)));
 
     Map<String, Map<State, List<ServiceComponentHost>>> changedHosts2 =
         new HashMap<String, Map<State, List<ServiceComponentHost>>>();
@@ -425,11 +440,12 @@ public class HostComponentResourceProviderTest {
         eq(managementController))).
         andReturn(provider).anyTimes();
 
    expect(kerberosHelper.isClusterKerberosEnabled(cluster)).andReturn(false).once();
    expect(kerberosHelper.isClusterKerberosEnabled(cluster)).andReturn(false).times(2);
 
     // replay
     replay(managementController, response, resourceProviderFactory, clusters, cluster, service,
        component, componentHost, stageContainer, maintenanceStateHelper, kerberosHelper);
        component, componentHost, stageContainer, maintenanceStateHelper, kerberosHelper, clientComponent,
        clientComponentHost);
 
     Map<String, Object> properties = new LinkedHashMap<String, Object>();
     properties.put(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
@@ -438,7 +454,8 @@ public class HostComponentResourceProviderTest {
 
     assertSame(response, requestResponse);
     // verify
    verify(managementController, response, resourceProviderFactory, stageContainer, kerberosHelper);
    verify(managementController, response, resourceProviderFactory, stageContainer, kerberosHelper,
           clientComponent, clientComponentHost);
   }
 
   @Test
- 
2.19.1.windows.1

