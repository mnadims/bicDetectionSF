From c7b350b678b82bae1c0834744249cb534fed18f1 Mon Sep 17 00:00:00 2001
From: Aravindan Vijayan <avijayan@hortonworks.com>
Date: Mon, 31 Jul 2017 14:30:27 -0700
Subject: [PATCH] AMBARI-21593 : AMS stopped after RU [AMS distributed mode
 with 2 collectors] (avijayan)

--
 .../MetricCollectorHAController.java          | 42 ++++++++++++++-----
 1 file changed, 32 insertions(+), 10 deletions(-)

diff --git a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java
index 53e63040cc..addb14e672 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java
@@ -26,6 +26,7 @@ import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricsSystemInitializationException;
 import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
 import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixException;
 import org.apache.helix.HelixManager;
 import org.apache.helix.HelixManagerFactory;
 import org.apache.helix.InstanceType;
@@ -123,20 +124,41 @@ public class MetricCollectorHAController {
     admin = new ZKHelixAdmin(zkConnectUrl);
     // create cluster
     LOG.info("Creating zookeeper cluster node: " + clusterName);
    admin.addCluster(clusterName, false);
    boolean clusterAdded = admin.addCluster(clusterName, false);
    LOG.info("Was cluster added successfully? " + clusterAdded);
 
     // Adding host to the cluster
    List<String> nodes = Collections.EMPTY_LIST;
    try {
      nodes =  admin.getInstancesInCluster(clusterName);
    } catch (ZkNoNodeException ex) {
      LOG.warn("Child znode under /" + CLUSTER_NAME + " not found.Recreating the cluster.");
        admin.addCluster(clusterName, true);
    boolean success = false;
    int tries = 5;
    int sleepTimeInSeconds = 5;

    for (int i = 0; i < tries && !success; i++) {
      try {
        List<String> nodes = admin.getInstancesInCluster(clusterName);
        if (CollectionUtils.isEmpty(nodes) || !nodes.contains(instanceConfig.getInstanceName())) {
          LOG.info("Adding participant instance " + instanceConfig);
          admin.addInstance(clusterName, instanceConfig);
          success = true;
        }
      } catch (HelixException | ZkNoNodeException ex) {
        LOG.warn("Helix Cluster not yet setup fully.");
        if (i < tries - 1) {
          LOG.info("Waiting for " + sleepTimeInSeconds + " seconds and retrying.");
          TimeUnit.SECONDS.sleep(sleepTimeInSeconds);
        } else {
          LOG.error(ex);
        }
      }
     }
 
    if (CollectionUtils.isEmpty(nodes) || !nodes.contains(instanceConfig.getInstanceName())) {
      LOG.info("Adding participant instance " + instanceConfig);
      admin.addInstance(clusterName, instanceConfig);
    if (!success) {
      LOG.info("Trying to create " + clusterName + " again since waiting for the creation did not help.");
      admin.addCluster(clusterName, true);
      List<String> nodes = admin.getInstancesInCluster(clusterName);
      if (CollectionUtils.isEmpty(nodes) || !nodes.contains(instanceConfig.getInstanceName())) {
        LOG.info("Adding participant instance " + instanceConfig);
        admin.addInstance(clusterName, instanceConfig);
      }
     }
 
     // Add a state model
- 
2.19.1.windows.1

