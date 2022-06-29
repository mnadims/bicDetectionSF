From f122beb129913a98f3bd32882ef34c8104c35f96 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 28 May 2015 13:28:13 -0400
Subject: [PATCH] ACCUMULO-3859 Ensure multiple TableConfiguration instances
 are not created.

If an instance of a TableConfiguration is cached which isn't the same
instance held by a Tablet, this will result in the Tablet never
receiving updates for constraints and more.
--
 .../conf/ServerConfigurationFactory.java      | 31 ++++++++++++-------
 1 file changed, 20 insertions(+), 11 deletions(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java b/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java
index 96ff5d693..7981f3b5d 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java
++ b/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java
@@ -34,14 +34,9 @@ import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
  */
 public class ServerConfigurationFactory {
 
  private static final Map<String,Map<String,TableConfiguration>> tableConfigs;
  private static final Map<String,Map<String,NamespaceConfiguration>> namespaceConfigs;
  private static final Map<String,Map<String,NamespaceConfiguration>> tableParentConfigs;
  static {
    tableConfigs = new HashMap<String,Map<String,TableConfiguration>>(1);
    namespaceConfigs = new HashMap<String,Map<String,NamespaceConfiguration>>(1);
    tableParentConfigs = new HashMap<String,Map<String,NamespaceConfiguration>>(1);
  }
  private static final Map<String,Map<String,TableConfiguration>> tableConfigs = new HashMap<String,Map<String,TableConfiguration>>(1);
  private static final Map<String,Map<String,NamespaceConfiguration>> namespaceConfigs = new HashMap<String,Map<String,NamespaceConfiguration>>(1);
  private static final Map<String,Map<String,NamespaceConfiguration>> tableParentConfigs = new HashMap<String,Map<String,NamespaceConfiguration>>(1);
 
   private static void addInstanceToCaches(String iid) {
     synchronized (tableConfigs) {
@@ -152,13 +147,27 @@ public class ServerConfigurationFactory {
     synchronized (tableConfigs) {
       conf = tableConfigs.get(instanceID).get(tableId);
     }
    // can't hold the lock during the construction and validation of the config,
    // which may result in creating multiple objects for the same id, but that's ok.

    // Can't hold the lock during the construction and validation of the config,
    // which would result in creating multiple objects for the same id.
    //
    // ACCUMULO-3859 We _cannot_ all multiple instances to be created for a table. If the TableConfiguration
    // instance a Tablet holds is not the same as the one cached here, any ConfigurationObservers that
    // Tablet sets will never see updates from ZooKeeper which means that things like constraints and
    // default visibility labels will never be updated in a Tablet until it is reloaded.
     if (conf == null && Tables.exists(instance, tableId)) {
       conf = new TableConfiguration(instance.getInstanceID(), tableId, getNamespaceConfigurationForTable(tableId));
       ConfigSanityCheck.validate(conf);
       synchronized (tableConfigs) {
        tableConfigs.get(instanceID).put(tableId, conf);
        Map<String,TableConfiguration> configs = tableConfigs.get(instanceID);
        TableConfiguration existingConf = configs.get(tableId);
        if (null == existingConf) {
          // Configuration doesn't exist yet
          configs.put(tableId, conf);
        } else {
          // Someone beat us to the punch, reuse their instance instead of replacing it
          conf = existingConf;
        }
       }
     }
     return conf;
- 
2.19.1.windows.1

