From a8b8b4155b6655ef407e2f0d623722047235fc6b Mon Sep 17 00:00:00 2001
From: Sumit Mohanty <smohanty@hortonworks.com>
Date: Tue, 31 Jan 2017 13:11:13 -0800
Subject: [PATCH] AMBARI-19719. Update rpc port for LLAP to 0 for HDP stack
 (Siddharth Seth via smohanty)

--
 .../server/upgrade/UpgradeCatalog250.java     | 30 ++++++++++++++-----
 .../configuration/hive-interactive-site.xml   |  2 +-
 2 files changed, 23 insertions(+), 9 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
index 45d2874f3e..6c9026279a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
@@ -233,7 +233,7 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
           Set<String> installedServices = cluster.getServices().keySet();
 
           if (installedServices.contains("HIVE")) {
            Config hiveSite = cluster.getDesiredConfigByType("hive-interactive-site");
            Config hiveSite = cluster.getDesiredConfigByType(HIVE_INTERACTIVE_SITE);
             if (hiveSite != null) {
               Map<String, String> hiveSiteProperties = hiveSite.getProperties();
               String schedulerDelay = hiveSiteProperties.get("hive.llap.task.scheduler.locality.delay");
@@ -244,19 +244,19 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
                     int schedulerDelayInt = Integer.parseInt(schedulerDelay);
                     if (schedulerDelayInt == -1) {
                       // Old default. Set to new default.
                      updateConfigurationProperties("hive-interactive-site", Collections
                      updateConfigurationProperties(HIVE_INTERACTIVE_SITE, Collections
                                                         .singletonMap("hive.llap.task.scheduler.locality.delay", "8000"), true,
                                                     false);
                     }
                   } catch (NumberFormatException e) {
                     // Invalid existing value. Set to new default.
                    updateConfigurationProperties("hive-interactive-site", Collections
                    updateConfigurationProperties(HIVE_INTERACTIVE_SITE, Collections
                                                       .singletonMap("hive.llap.task.scheduler.locality.delay", "8000"), true,
                                                   false);
                   }
                 }
               }
              updateConfigurationProperties("hive-interactive-site",
              updateConfigurationProperties(HIVE_INTERACTIVE_SITE,
                                             Collections.singletonMap("hive.mapjoin.hybridgrace.hashtable", "true"), true,
                                             false);
               updateConfigurationProperties("tez-interactive-site",
@@ -612,6 +612,7 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
    *
    * @throws AmbariException
    */
  private static final String HIVE_INTERACTIVE_SITE = "hive-interactive-site";
   protected void updateHIVEInteractiveConfigs() throws AmbariException {
     AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
     Clusters clusters = ambariManagementController.getClusters();
@@ -620,16 +621,29 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
 
       if (clusterMap != null && !clusterMap.isEmpty()) {
         for (final Cluster cluster : clusterMap.values()) {
          Config hiveInteractiveSite = cluster.getDesiredConfigByType("hive-interactive-site");
          Config hiveInteractiveSite = cluster.getDesiredConfigByType(HIVE_INTERACTIVE_SITE);
           if (hiveInteractiveSite != null) {
            updateConfigurationProperties("hive-interactive-site", Collections.singletonMap("hive.tez.container.size",
            updateConfigurationProperties(HIVE_INTERACTIVE_SITE, Collections.singletonMap("hive.tez.container.size",
                 "SET_ON_FIRST_INVOCATION"), true, true);
 
            updateConfigurationProperties("hive-interactive-site", Collections.singletonMap("hive.auto.convert.join.noconditionaltask.size",
            updateConfigurationProperties(HIVE_INTERACTIVE_SITE, Collections.singletonMap("hive.auto.convert.join.noconditionaltask.size",
                 "1000000000"), true, true);
            updateConfigurationProperties("hive-interactive-site",
            updateConfigurationProperties(HIVE_INTERACTIVE_SITE,
                 Collections.singletonMap("hive.llap.execution.mode", "only"),
                 true, true);
            String llapRpcPortString = hiveInteractiveSite.getProperties().get("hive.llap.daemon.rpc.port");
            if (StringUtils.isNotBlank(llapRpcPortString)) {
              try {
                int llapRpcPort = Integer.parseInt(llapRpcPortString);
                if (llapRpcPort == 15001) {
                  updateConfigurationProperties(HIVE_INTERACTIVE_SITE,
                      Collections.singletonMap("hive.llap.daemon.rpc.port", "only"),
                      true, true);
                }
              } catch (NumberFormatException e) {
                LOG.warn("Unable to parse llap.rpc.port as integer: " + llapRpcPortString);
              }
            }
           }
         }
       }
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/HIVE/configuration/hive-interactive-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.5/services/HIVE/configuration/hive-interactive-site.xml
index 640f30f7cc..93e2020856 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.5/services/HIVE/configuration/hive-interactive-site.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.5/services/HIVE/configuration/hive-interactive-site.xml
@@ -537,7 +537,7 @@ limitations under the License.
   </property>
   <property>
     <name>hive.llap.daemon.rpc.port</name>
    <value>15001</value>
    <value>0</value>
     <description>The LLAP daemon RPC port.</description>
     <on-ambari-upgrade add="true"/>
   </property>
- 
2.19.1.windows.1

