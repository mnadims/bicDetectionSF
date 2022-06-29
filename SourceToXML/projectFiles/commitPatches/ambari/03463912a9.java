From 03463912a95bdddab9c78a120b1de75b34c03395 Mon Sep 17 00:00:00 2001
From: John Speidel <jspeidel@hortonworks.com>
Date: Mon, 22 Dec 2014 16:34:22 -0500
Subject: [PATCH] AMBARI-8867.  Ensure that bluepint deployment sets each
 config type on cluster               no more than once

--
 .../internal/ClusterResourceProvider.java      | 18 ++++++++++++------
 1 file changed, 12 insertions(+), 6 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterResourceProvider.java
index f9aca1d808..2d6ad8f8a2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterResourceProvider.java
@@ -815,17 +815,23 @@ public class ClusterResourceProvider extends BaseBlueprintProcessor {
     // create a list of config requests on a per-service basis, in order
     // to properly support the new service configuration versioning mechanism
     // in Ambari
    Collection<String> encounteredConfigTypes = new HashSet<String>();
     for (String service : getServicesToDeploy(stack, blueprintHostGroups)) {
       BlueprintServiceConfigRequest blueprintConfigRequest =
         new BlueprintServiceConfigRequest(service);
 
       for (String serviceConfigType : stack.getConfigurationTypes(service)) {
        // skip handling of cluster-env here
        if (!serviceConfigType.equals("cluster-env")) {
          if (mapClusterConfigurations.containsKey(serviceConfigType)) {
            blueprintConfigRequest.addConfigElement(serviceConfigType,
              mapClusterConfigurations.get(serviceConfigType),
              mapClusterAttributes.get(serviceConfigType));
        //todo: This is a temporary fix to ensure that we don't try to add the same
        //todo: config type multiple times.
        //todo: This is to unblock BUG-28939 and will be correctly fixed as part of BUG-29145.
        if (encounteredConfigTypes.add(serviceConfigType)) {
          // skip handling of cluster-env here
          if (!serviceConfigType.equals("cluster-env")) {
            if (mapClusterConfigurations.containsKey(serviceConfigType)) {
              blueprintConfigRequest.addConfigElement(serviceConfigType,
                mapClusterConfigurations.get(serviceConfigType),
                mapClusterAttributes.get(serviceConfigType));
            }
           }
         }
       }
- 
2.19.1.windows.1

