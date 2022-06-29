From e02969e17385dd5d53b0572a3eadac2cbfd24adb Mon Sep 17 00:00:00 2001
From: Aravindan Vijayan <avijayan@hortonworks.com>
Date: Fri, 24 Feb 2017 12:55:49 -0800
Subject: [PATCH] AMBARI-20179 : AMS Collector shuts down with Helix-Zk related
 exception if partial /ambari-metrics-cluster znode exists. (avijayan)

--
 .../availability/MetricCollectorHAController.java  | 14 ++++++++++++--
 1 file changed, 12 insertions(+), 2 deletions(-)

diff --git a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java
index 12c255e1d7..53e63040cc 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/availability/MetricCollectorHAController.java
@@ -18,6 +18,8 @@
 package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability;
 
 import com.google.common.base.Joiner;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.apache.commons.collections.CollectionUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
@@ -37,6 +39,7 @@ import org.apache.helix.model.OnlineOfflineSMD;
 import org.apache.helix.model.StateModelDefinition;
 import org.apache.helix.tools.StateModelConfigGenerator;;
 import java.util.ArrayList;
import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeSet;
@@ -123,8 +126,15 @@ public class MetricCollectorHAController {
     admin.addCluster(clusterName, false);
 
     // Adding host to the cluster
    List<String> nodes = admin.getInstancesInCluster(clusterName);
    if (nodes == null || !nodes.contains(instanceConfig.getInstanceName())) {
    List<String> nodes = Collections.EMPTY_LIST;
    try {
      nodes =  admin.getInstancesInCluster(clusterName);
    } catch (ZkNoNodeException ex) {
      LOG.warn("Child znode under /" + CLUSTER_NAME + " not found.Recreating the cluster.");
        admin.addCluster(clusterName, true);
    }

    if (CollectionUtils.isEmpty(nodes) || !nodes.contains(instanceConfig.getInstanceName())) {
       LOG.info("Adding participant instance " + instanceConfig);
       admin.addInstance(clusterName, instanceConfig);
     }
- 
2.19.1.windows.1

