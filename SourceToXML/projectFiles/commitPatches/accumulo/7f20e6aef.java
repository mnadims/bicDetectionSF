From 7f20e6aef77f4fe60a93d06ee13e41d8585125cf Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <eric.newton@gmail.com>
Date: Mon, 1 Dec 2014 12:05:34 -0500
Subject: [PATCH] ACCUMULO-3372 avoid holding the lock while constructing a
 cached object

--
 .../conf/ServerConfigurationFactory.java      | 51 ++++++++++++-------
 1 file changed, 33 insertions(+), 18 deletions(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java b/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java
index 8e62a873f..35b65565b 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java
++ b/server/base/src/main/java/org/apache/accumulo/server/conf/ServerConfigurationFactory.java
@@ -148,15 +148,20 @@ public class ServerConfigurationFactory {
 
   public TableConfiguration getTableConfiguration(String tableId) {
     checkPermissions();
    TableConfiguration conf;
     synchronized (tableConfigs) {
      TableConfiguration conf = tableConfigs.get(instanceID).get(tableId);
      if (conf == null && Tables.exists(instance, tableId)) {
        conf = new TableConfiguration(instance.getInstanceID(), tableId, getNamespaceConfigurationForTable(tableId));
        ConfigSanityCheck.validate(conf);
      conf = tableConfigs.get(instanceID).get(tableId);
    }
    // can't hold the lock during the construction and validation of the config, 
    // which may result in creating multiple objects for the same id, but that's ok.
    if (conf == null && Tables.exists(instance, tableId)) {
      conf = new TableConfiguration(instance.getInstanceID(), tableId, getNamespaceConfigurationForTable(tableId));
      ConfigSanityCheck.validate(conf);
      synchronized (tableConfigs) {
         tableConfigs.get(instanceID).put(tableId, conf);
       }
      return conf;
     }
    return conf;
   }
 
   public TableConfiguration getTableConfiguration(KeyExtent extent) {
@@ -165,31 +170,41 @@ public class ServerConfigurationFactory {
 
   public NamespaceConfiguration getNamespaceConfigurationForTable(String tableId) {
     checkPermissions();
    NamespaceConfiguration conf;
     synchronized (tableParentConfigs) {
      NamespaceConfiguration conf = tableParentConfigs.get(instanceID).get(tableId);
      if (conf == null) {
        // changed - include instance in constructor call
        conf = new TableParentConfiguration(tableId, instance, getConfiguration());
        ConfigSanityCheck.validate(conf);
      conf = tableParentConfigs.get(instanceID).get(tableId);
    }
    // can't hold the lock during the construction and validation of the config, 
    // which may result in creating multiple objects for the same id, but that's ok.
    if (conf == null) {
      // changed - include instance in constructor call
      conf = new TableParentConfiguration(tableId, instance, getConfiguration());
      ConfigSanityCheck.validate(conf);
      synchronized (tableParentConfigs) {
         tableParentConfigs.get(instanceID).put(tableId, conf);
       }
      return conf;
     }
    return conf;
   }
 
   public NamespaceConfiguration getNamespaceConfiguration(String namespaceId) {
     checkPermissions();
    NamespaceConfiguration conf;
    // can't hold the lock during the construction and validation of the config, 
    // which may result in creating multiple objects for the same id, but that's ok.
     synchronized (namespaceConfigs) {
      NamespaceConfiguration conf = namespaceConfigs.get(instanceID).get(namespaceId);
      if (conf == null) {
        // changed - include instance in constructor call
        conf = new NamespaceConfiguration(namespaceId, instance, getConfiguration());
        conf.setZooCacheFactory(zcf);
        ConfigSanityCheck.validate(conf);
      conf = namespaceConfigs.get(instanceID).get(namespaceId);
    }
    if (conf == null) {
      // changed - include instance in constructor call
      conf = new NamespaceConfiguration(namespaceId, instance, getConfiguration());
      conf.setZooCacheFactory(zcf);
      ConfigSanityCheck.validate(conf);
      synchronized (namespaceConfigs) {
         namespaceConfigs.get(instanceID).put(namespaceId, conf);
       }
      return conf;
     }
    return conf;
   }
 
   public Instance getInstance() {
- 
2.19.1.windows.1

