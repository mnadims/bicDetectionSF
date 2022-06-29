From 7be21c9148fa0a1cb088d13407974b60dac68b36 Mon Sep 17 00:00:00 2001
From: "Doroszlai, Attila" <adoroszlai@apache.org>
Date: Sun, 14 Jan 2018 21:02:35 +0100
Subject: [PATCH] AMBARI-22779. Cannot scale cluster if Ambari Server restarted
 since blueprint cluster creation

--
 .../org/apache/ambari/server/topology/TopologyManager.java   | 5 ++++-
 1 file changed, 4 insertions(+), 1 deletion(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java b/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
index d07dec09fb..6bdc8963e5 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/topology/TopologyManager.java
@@ -496,6 +496,9 @@ public class TopologyManager {
 
     hostNameCheck(request, topology);
     request.setClusterId(clusterId);
    if (ambariContext.isTopologyResolved(clusterId)) {
      getOrCreateTopologyTaskExecutor(clusterId).start();
    }
 
     // this registers/updates all request host groups
     topology.update(request);
@@ -968,7 +971,7 @@ public class TopologyManager {
     persistedState.registerInTopologyHostInfo(host);
   }
 
  private ExecutorService getOrCreateTopologyTaskExecutor(Long clusterId) {
  private ManagedThreadPoolExecutor getOrCreateTopologyTaskExecutor(Long clusterId) {
     ManagedThreadPoolExecutor topologyTaskExecutor = this.topologyTaskExecutorServiceMap.get(clusterId);
     if (topologyTaskExecutor == null) {
       LOG.info("Creating TopologyTaskExecutorService for clusterId: {}", clusterId);
- 
2.19.1.windows.1

