From 9842b40d2b2cd6c80cbdfc0086f3529a94c423b3 Mon Sep 17 00:00:00 2001
From: Lisnichenko Dmitro <dlysnichenko@hortonworks.com>
Date: Tue, 23 Dec 2014 16:59:57 +0200
Subject: [PATCH] AMBARI-8868. API call used by Install Wizard to retry
 installation does not work (dlysnichenko)

--
 .../HostComponentResourceProvider.java        | 20 +++++++++----------
 .../HostComponentResourceProviderTest.java    |  4 ++--
 2 files changed, 12 insertions(+), 12 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
index 47d3f703db..a6c95f5d86 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/HostComponentResourceProvider.java
@@ -192,6 +192,8 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
 
     Set<Resource> resources = new HashSet<Resource>();
     Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    // We always need host_name for sch
    requestedIds.add(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);
 
     Set<ServiceComponentHostResponse> responses = getResources(new Command<Set<ServiceComponentHostResponse>>() {
       @Override
@@ -598,19 +600,17 @@ public class HostComponentResourceProvider extends AbstractControllerResourcePro
     Set<Resource> matchingResources = getResources(queryRequest, predicate);
 
     for (Resource queryResource : matchingResources) {
      if (predicate.evaluate(queryResource)) {
        Map<String, Object> updateRequestProperties = new HashMap<String, Object>();
      Map<String, Object> updateRequestProperties = new HashMap<String, Object>();
 
        // add props from query resource
        updateRequestProperties.putAll(PropertyHelper.getProperties(queryResource));
      // add props from query resource
      updateRequestProperties.putAll(PropertyHelper.getProperties(queryResource));
 
        // add properties from update request
        //todo: should we flag value size > 1?
        if (request.getProperties() != null && request.getProperties().size() != 0) {
          updateRequestProperties.putAll(request.getProperties().iterator().next());
        }
        requests.add(getRequest(updateRequestProperties));
      // add properties from update request
      //todo: should we flag value size > 1?
      if (request.getProperties() != null && request.getProperties().size() != 0) {
        updateRequestProperties.putAll(request.getProperties().iterator().next());
       }
      requests.add(getRequest(updateRequestProperties));
     }
 
     RequestStageContainer requestStages = modifyResources(new Command<RequestStageContainer>() {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
index 0ffc6e131a..c6a89ed7d7 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/HostComponentResourceProviderTest.java
@@ -268,7 +268,7 @@ public class HostComponentResourceProviderTest {
 
     Set<ServiceComponentHostResponse> nameResponse = new HashSet<ServiceComponentHostResponse>();
     nameResponse.add(new ServiceComponentHostResponse(
        "Cluster102", "Service100", "Component100", "Host100", "STARTED", "", "", "", null));
        "Cluster102", "Service100", "Component100", "Host100", "INSTALLED", "", "", "", null));
 
     // set expectations
     expect(managementController.getClusters()).andReturn(clusters).anyTimes();
@@ -323,7 +323,7 @@ public class HostComponentResourceProviderTest {
     // update the cluster named Cluster102
     Predicate predicate = new PredicateBuilder().property(
         HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").and().
        property(HostComponentResourceProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID).equals("Host100").and().
        property(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID).equals("INSTALLED").and().
         property(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("Component100").toPredicate();
     RequestStatus requestStatus = provider.updateResources(request, predicate);
     Resource responseResource = requestStatus.getRequestResource();
- 
2.19.1.windows.1

