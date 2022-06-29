From d4ba96481c76487bc1fdd7fbde8392bc97c3226d Mon Sep 17 00:00:00 2001
From: Nishant <nishant.monu51@gmail.com>
Date: Tue, 29 Aug 2017 11:48:37 +0530
Subject: [PATCH] AMBARI-21836. Fix Upgrade failure because of missing table
 after AMBARI-21076 checkin.

--
 .../org/apache/ambari/server/upgrade/UpgradeCatalog260.java   | 4 ----
 .../apache/ambari/server/upgrade/UpgradeCatalog260Test.java   | 2 --
 2 files changed, 6 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
index b4e7a02778..2bd0f930b2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog260.java
@@ -386,9 +386,6 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
   }
 
   private void removeComponent(String componentName, String configPrefix) throws SQLException {
    String supersetConfigMappingRemoveSQL = String.format(
        "DELETE FROM %s WHERE type_name like '%s%%'",
        CLUSTER_CONFIG_MAPPING_TABLE, configPrefix);
 
     String serviceConfigMappingRemoveSQL = String.format(
         "DELETE FROM %s WHERE config_id IN (SELECT config_id from %s where type_name like '%s%%')",
@@ -410,7 +407,6 @@ public class UpgradeCatalog260 extends AbstractUpgradeCatalog {
         "DELETE FROM %s WHERE component_name = '%s'",
         SERVICE_COMPONENT_DESIRED_STATE, componentName);
 
    dbAccessor.executeQuery(supersetConfigMappingRemoveSQL);
     dbAccessor.executeQuery(serviceConfigMappingRemoveSQL);
     dbAccessor.executeQuery(supersetConfigRemoveSQL);
     dbAccessor.executeQuery(hostComponentDesiredStateRemoveSQL);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
index b6c323e107..d8e8171115 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog260Test.java
@@ -487,8 +487,6 @@ public class UpgradeCatalog260Test {
     expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).anyTimes();
     expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();
 
    dbAccessor.executeQuery("DELETE FROM clusterconfigmapping WHERE type_name like 'druid-superset%'");
    expectLastCall().once();
     dbAccessor.executeQuery("DELETE FROM serviceconfigmapping WHERE config_id IN (SELECT config_id from clusterconfig where type_name like 'druid-superset%')");
     expectLastCall().once();
     dbAccessor.executeQuery("DELETE FROM clusterconfig WHERE type_name like 'druid-superset%'");
- 
2.19.1.windows.1

