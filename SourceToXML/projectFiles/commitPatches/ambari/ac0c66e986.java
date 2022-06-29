From ac0c66e986e14db6a746dfe1b84f36a662dacfbb Mon Sep 17 00:00:00 2001
From: Robert Levas <rlevas@hortonworks.com>
Date: Thu, 22 Sep 2016 12:36:52 -0400
Subject: [PATCH] AMBARI-18433. Enforce granular role-based access control for
 custom actions (rlevas)

--
 .../admin-web/app/scripts/services/Cluster.js |   2 +
 .../internal/RequestResourceProvider.java     |  84 ++++++-----
 .../authorization/RoleAuthorization.java      |   2 +
 .../server/upgrade/UpgradeCatalog250.java     |  16 +++
 .../resources/Ambari-DDL-Derby-CREATE.sql     |  11 +-
 .../resources/Ambari-DDL-MySQL-CREATE.sql     |  11 +-
 .../resources/Ambari-DDL-Oracle-CREATE.sql    |  11 +-
 .../resources/Ambari-DDL-Postgres-CREATE.sql  |  11 +-
 .../Ambari-DDL-SQLAnywhere-CREATE.sql         |  11 +-
 .../resources/Ambari-DDL-SQLServer-CREATE.sql |  11 +-
 .../system_action_definitions.xml             |   5 +
 .../internal/RequestResourceProviderTest.java | 134 +++++++++++++++---
 .../security/TestAuthenticationFactory.java   |   1 +
 .../server/upgrade/UpgradeCatalog250Test.java | 117 +++++++++++++--
 14 files changed, 340 insertions(+), 87 deletions(-)

diff --git a/ambari-admin/src/main/resources/ui/admin-web/app/scripts/services/Cluster.js b/ambari-admin/src/main/resources/ui/admin-web/app/scripts/services/Cluster.js
index c17c36dab4..02c231a160 100644
-- a/ambari-admin/src/main/resources/ui/admin-web/app/scripts/services/Cluster.js
++ b/ambari-admin/src/main/resources/ui/admin-web/app/scripts/services/Cluster.js
@@ -67,6 +67,7 @@ angular.module('ambariAdminConsole')
       "CLUSTER.TOGGLE_ALERTS",
       "CLUSTER.TOGGLE_KERBEROS",
       "CLUSTER.UPGRADE_DOWNGRADE_STACK",
      "CLUSTER.RUN_CUSTOM_COMMAND",
       "AMBARI.ADD_DELETE_CLUSTERS",
       "AMBARI.ASSIGN_ROLES",
       "AMBARI.EDIT_STACK_REPOS",
@@ -76,6 +77,7 @@ angular.module('ambariAdminConsole')
       "AMBARI.MANAGE_USERS",
       "AMBARI.MANAGE_VIEWS",
       "AMBARI.RENAME_CLUSTER",
      "AMBARI.RUN_CUSTOM_COMMAND",
       "SERVICE.SET_SERVICE_USERS_GROUPS"
     ],
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RequestResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RequestResourceProvider.java
index d38234f3de..8c1bc57cec 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RequestResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RequestResourceProvider.java
@@ -1,4 +1,4 @@
/**
/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
@@ -186,53 +186,61 @@ public class RequestResourceProvider extends AbstractControllerResourceProvider
 
         String clusterName = actionRequest.getClusterName();
 
        if(clusterName == null) {
          String actionName = actionRequest.getActionName();
        ResourceType resourceType;
        Long resourceId;
 
          // Ensure that the actionName is not null or empty.  A null actionName will result in
          // a NPE at when getting the action definition.  The string "_unknown_action_" should not
          // result in a valid action definition and should be easy to understand in any error message
          // that gets displayed or logged due to an authorization issue.
          if(StringUtils.isEmpty(actionName)) {
            actionName = "_unknown_action_";
          }
        if (StringUtils.isEmpty(clusterName)) {
          resourceType = ResourceType.AMBARI;
          resourceId = null;
        } else {
          resourceType = ResourceType.CLUSTER;
          resourceId = getClusterResourceId(clusterName);
        }
 
          ActionDefinition actionDefinition = getManagementController().getAmbariMetaInfo().getActionDefinition(actionName);
          Set<RoleAuthorization> permissions = (actionDefinition == null) ? null : actionDefinition.getPermissions();
        if (actionRequest.isCommand()) {
          String commandName = actionRequest.getCommandName();
 
          if(permissions == null) {
            if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND)) {
              throw new AuthorizationException(String.format("The authenticated user is not authorized to execute the '%s'command.", actionName));
            }
          if (StringUtils.isEmpty(commandName)) {
            commandName = "_unknown_command_";
           }
          else {
            // Since we cannot tell whether the action is to be exectued for the system or a
            // non-disclosed cluster, specify that the resource is a CLUSTER with no resource id.
            // This should ensure that a user with a role for any cluster with the appropriate
            // permissions or an Ambari administrator can execute the command.
            if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, null, permissions)) {
              throw new AuthorizationException(String.format("The authenticated user is not authorized to execute the '%s'command.", actionName));

          if (commandName.endsWith("_SERVICE_CHECK")) {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_RUN_SERVICE_CHECK)) {
              throw new AuthorizationException("The authenticated user is not authorized to execute service checks.");
            }
          } else if (commandName.equals("DECOMMISSION")) {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION)) {
              throw new AuthorizationException("The authenticated user is not authorized to decommission services.");
            }
          } else {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND)) {
              throw new AuthorizationException(String.format("The authenticated user is not authorized to execute the command, %s.",
                  commandName));
             }
           }
        }
        else if(actionRequest.isCommand()) {
          if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER,
              getClusterResourceId(clusterName), RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND)) {
            throw new AuthorizationException("The authenticated user is not authorized to execute custom service commands.");
          }
        }
        else {
        } else {
           String actionName = actionRequest.getActionName();
 
          // actionName is expected to not be null since the action request is not a command
          if(actionName.contains("SERVICE_CHECK")) {
            if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, getClusterResourceId(clusterName), RoleAuthorization.SERVICE_RUN_SERVICE_CHECK)) {
          if (StringUtils.isEmpty(actionName)) {
            actionName = "_unknown_action_";
          }

          if (actionName.contains("SERVICE_CHECK")) {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_RUN_SERVICE_CHECK)) {
               throw new AuthorizationException("The authenticated user is not authorized to execute service checks.");
             }
          }
          else if(actionName.equals("DECOMMISSION")) {
            if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, getClusterResourceId(clusterName), RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION)) {
              throw new AuthorizationException("The authenticated user is not authorized to decommission services.");
          } else {
            // A custom action has been requested
            ActionDefinition actionDefinition = (actionName == null)
                ? null
                : getManagementController().getAmbariMetaInfo().getActionDefinition(actionName);

            Set<RoleAuthorization> permissions = (actionDefinition == null)
                ? null
                : actionDefinition.getPermissions();

            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, permissions)) {
              throw new AuthorizationException(String.format("The authenticated user is not authorized to execute the action %s.", actionName));
             }
           }
         }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java
index 0157d49f1e..4a0ea713fb 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java
@@ -38,6 +38,7 @@ public enum RoleAuthorization {
   AMBARI_MANAGE_USERS("AMBARI.MANAGE_USERS"),
   AMBARI_MANAGE_VIEWS("AMBARI.MANAGE_VIEWS"),
   AMBARI_RENAME_CLUSTER("AMBARI.RENAME_CLUSTER"),
  AMBARI_RUN_CUSTOM_COMMAND("AMBARI.RUN_CUSTOM_COMMAND"),
   CLUSTER_MANAGE_CREDENTIALS("CLUSTER.MANAGE_CREDENTIALS"),
   CLUSTER_MODIFY_CONFIGS("CLUSTER.MODIFY_CONFIGS"),
   CLUSTER_MANAGE_CONFIG_GROUPS("CLUSTER.MANAGE_CONFIG_GROUPS"),
@@ -51,6 +52,7 @@ public enum RoleAuthorization {
   CLUSTER_VIEW_METRICS("CLUSTER.VIEW_METRICS"),
   CLUSTER_VIEW_STACK_DETAILS("CLUSTER.VIEW_STACK_DETAILS"),
   CLUSTER_VIEW_STATUS_INFO("CLUSTER.VIEW_STATUS_INFO"),
  CLUSTER_RUN_CUSTOM_COMMAND("CLUSTER.RUN_CUSTOM_COMMAND"),
   HOST_ADD_DELETE_COMPONENTS("HOST.ADD_DELETE_COMPONENTS"),
   HOST_ADD_DELETE_HOSTS("HOST.ADD_DELETE_HOSTS"),
   HOST_TOGGLE_MAINTENANCE("HOST.TOGGLE_MAINTENANCE"),
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
index 35c773acbf..185bd58cc5 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
@@ -19,6 +19,8 @@ package org.apache.ambari.server.upgrade;
 
 import java.sql.SQLException;
 import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
@@ -107,6 +109,7 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
   @Override
   protected void executeDMLUpdates() throws AmbariException, SQLException {
     updateAMSConfigs();
    createRoleAuthorizations();
   }
 
   protected void updateHostVersionTable() throws SQLException {
@@ -166,6 +169,19 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
     return content;
   }
 
  /**
   * Create new role authorizations: CLUSTER.RUN_CUSTOM_COMMAND and AMBARI.RUN_CUSTOM_COMMAND
   *
   * @throws SQLException
   */
  protected void createRoleAuthorizations() throws SQLException {
    LOG.info("Adding authorizations");
 
    addRoleAuthorization("CLUSTER.RUN_CUSTOM_COMMAND", "Perform custom cluster-level actions",
        Arrays.asList("AMBARI.ADMINISTRATOR:AMBARI", "CLUSTER.ADMINISTRATOR:CLUSTER"));

    addRoleAuthorization("AMBARI.RUN_CUSTOM_COMMAND", "Perform custom administrative actions",
        Collections.singletonList("AMBARI.ADMINISTRATOR:AMBARI"));
  }
 }
 
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
index 38f78c5520..c2c965dea4 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
@@ -1255,6 +1255,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'CLUSTER.TOGGLE_KERBEROS', 'Enable/disable Kerberos' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'CLUSTER.UPGRADE_DOWNGRADE_STACK', 'Upgrade/downgrade stack' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'CLUSTER.MANAGE_USER_PERSISTED_DATA', 'Manage cluster-level user persisted data' FROM SYSIBM.SYSDUMMY1 UNION ALL
  SELECT 'CLUSTER.RUN_CUSTOM_COMMAND', 'Perform custom cluster-level actions' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' FROM SYSIBM.SYSDUMMY1 UNION ALL
@@ -1263,7 +1264,8 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.ASSIGN_ROLES', 'Assign roles' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.MANAGE_STACK_VERSIONS', 'Manage stack versions' FROM SYSIBM.SYSDUMMY1 UNION ALL
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs'  FROM SYSIBM.SYSDUMMY1;
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs'  FROM SYSIBM.SYSDUMMY1 UNION ALL
  SELECT 'AMBARI.RUN_CUSTOM_COMMAND', 'Perform custom administrative actions' FROM SYSIBM.SYSDUMMY1;
 
 -- Set authorizations for View User role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1403,7 +1405,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR'  UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
 
 -- Set authorizations for Administrator role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1443,6 +1446,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
@@ -1452,7 +1456,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.ASSIGN_ROLES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_STACK_VERSIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
  SELECT permission_id, 'AMBARI.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
 
 INSERT INTO adminprivilege (privilege_id, permission_id, resource_id, principal_id)
   SELECT 1, 1, 1, 1 FROM SYSIBM.SYSDUMMY1 ;
diff --git a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
index 25948aa7ed..1d555151d0 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
@@ -1184,6 +1184,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'CLUSTER.TOGGLE_ALERTS', 'Enable/disable cluster-level alerts' UNION ALL
   SELECT 'CLUSTER.TOGGLE_KERBEROS', 'Enable/disable Kerberos' UNION ALL
   SELECT 'CLUSTER.UPGRADE_DOWNGRADE_STACK', 'Upgrade/downgrade stack' UNION ALL
  SELECT 'CLUSTER.RUN_CUSTOM_COMMAND', 'Perform custom cluster-level actions' UNION ALL
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage administrative settings' UNION ALL
@@ -1192,7 +1193,8 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
   SELECT 'AMBARI.ASSIGN_ROLES', 'Assign roles' UNION ALL
   SELECT 'AMBARI.MANAGE_STACK_VERSIONS', 'Manage stack versions' UNION ALL
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs';
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs' UNION ALL
  SELECT 'AMBARI.RUN_CUSTOM_COMMAND', 'Perform custom administrative actions';
 
 -- Set authorizations for View User role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1334,7 +1336,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
 
 -- Set authorizations for Administrator role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1377,6 +1380,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
@@ -1385,7 +1389,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.ASSIGN_ROLES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_STACK_VERSIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
 
 INSERT INTO adminprivilege (privilege_id, permission_id, resource_id, principal_id) VALUES
   (1, 1, 1, 1);
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
index 07cd6a8418..49f3e2f601 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
@@ -1203,6 +1203,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'CLUSTER.TOGGLE_ALERTS', 'Enable/disable cluster-level alerts' FROM dual UNION ALL
   SELECT 'CLUSTER.TOGGLE_KERBEROS', 'Enable/disable Kerberos' FROM dual UNION ALL
   SELECT 'CLUSTER.UPGRADE_DOWNGRADE_STACK', 'Upgrade/downgrade stack' FROM dual UNION ALL
  SELECT 'CLUSTER.RUN_CUSTOM_COMMAND', 'Perform custom cluster-level actions' FROM dual UNION ALL
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' FROM dual UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' FROM dual UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' FROM dual UNION ALL
@@ -1211,7 +1212,8 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' FROM dual UNION ALL
   SELECT 'AMBARI.ASSIGN_ROLES', 'Assign roles' FROM dual UNION ALL
   SELECT 'AMBARI.MANAGE_STACK_VERSIONS', 'Manage stack versions' FROM dual UNION ALL
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs' FROM dual;
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs' FROM dual UNION ALL
  SELECT 'AMBARI.RUN_CUSTOM_COMMAND', 'Perform custom administrative actions' FROM dual;
 
 -- Set authorizations for View User role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1353,7 +1355,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL;
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
 
 -- Set authorizations for Administrator role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1396,6 +1399,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
@@ -1404,7 +1408,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.ASSIGN_ROLES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_STACK_VERSIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
 
 insert into adminprivilege (privilege_id, permission_id, resource_id, principal_id)
   select 1, 1, 1, 1 from dual;
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
index f03767b41e..7aa52ef3c3 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
@@ -1175,6 +1175,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'CLUSTER.TOGGLE_ALERTS', 'Enable/disable cluster-level alerts' UNION ALL
   SELECT 'CLUSTER.TOGGLE_KERBEROS', 'Enable/disable Kerberos' UNION ALL
   SELECT 'CLUSTER.UPGRADE_DOWNGRADE_STACK', 'Upgrade/downgrade stack' UNION ALL
  SELECT 'CLUSTER.RUN_CUSTOM_COMMAND', 'Perform custom cluster-level actions' UNION ALL
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage administrative settings' UNION ALL
@@ -1183,7 +1184,8 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
   SELECT 'AMBARI.ASSIGN_ROLES', 'Assign roles' UNION ALL
   SELECT 'AMBARI.MANAGE_STACK_VERSIONS', 'Manage stack versions' UNION ALL
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs';
  SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs' UNION ALL
  SELECT 'AMBARI.RUN_CUSTOM_COMMAND', 'Perform custom administrative actions';
 
 -- Set authorizations for View User role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1325,7 +1327,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
  SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
 
 -- Set authorizations for Administrator role
 INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1368,6 +1371,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
@@ -1376,7 +1380,8 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.ASSIGN_ROLES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_STACK_VERSIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
  SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
 
 INSERT INTO adminprivilege (privilege_id, permission_id, resource_id, principal_id) VALUES
   (1, 1, 1, 1);
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
index 535d847955..0c95471161 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
@@ -1200,6 +1200,7 @@ insert into adminpermission(permission_id, permission_name, resource_type_id, pe
     SELECT 'CLUSTER.MANAGE_CONFIG_GROUPS', 'Manage cluster config groups' UNION ALL
     SELECT 'CLUSTER.TOGGLE_KERBEROS', 'Enable/disable Kerberos' UNION ALL
     SELECT 'CLUSTER.UPGRADE_DOWNGRADE_STACK', 'Upgrade/downgrade stack' UNION ALL
    SELECT 'CLUSTER.RUN_CUSTOM_COMMAND', 'Perform custom cluster-level actions' UNION ALL
     SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
     SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
     SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' UNION ALL
@@ -1208,7 +1209,8 @@ insert into adminpermission(permission_id, permission_name, resource_type_id, pe
     SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
     SELECT 'AMBARI.ASSIGN_ROLES', 'Assign roles' UNION ALL
     SELECT 'AMBARI.MANAGE_STACK_VERSIONS', 'Manage stack versions' UNION ALL
    SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs';
    SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs' UNION ALL
    SELECT 'AMBARI.RUN_CUSTOM_COMMAND', 'Perform custom administrative actions';
 
   -- Set authorizations for View User role
   INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1350,7 +1352,8 @@ insert into adminpermission(permission_id, permission_name, resource_type_id, pe
     SELECT permission_id, 'CLUSTER.TOGGLE_ALERTS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
    SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
 
   -- Set authorizations for Administrator role
   INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1393,6 +1396,7 @@ insert into adminpermission(permission_id, permission_name, resource_type_id, pe
     SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'CLUSTER.RUN_SERVICE_CHECK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
@@ -1401,7 +1405,8 @@ insert into adminpermission(permission_id, permission_name, resource_type_id, pe
     SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.ASSIGN_ROLES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_STACK_VERSIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
    SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'AMBARI.RUN_SERVICE_CHECK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
 
 insert into adminprivilege (privilege_id, permission_id, resource_id, principal_id)
   select 1, 1, 1, 1;
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
index 1bfde7a502..631b5c43e4 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
@@ -1203,6 +1203,7 @@ BEGIN TRANSACTION
     SELECT 'CLUSTER.MANAGE_CONFIG_GROUPS', 'Manage cluster config groups' UNION ALL
     SELECT 'CLUSTER.TOGGLE_KERBEROS', 'Enable/disable Kerberos' UNION ALL
     SELECT 'CLUSTER.UPGRADE_DOWNGRADE_STACK', 'Upgrade/downgrade stack' UNION ALL
    SELECT 'CLUSTER.RUN_CUSTOM_COMMAND', 'Perform custom cluster-level actions' UNION ALL
     SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
     SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
     SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' UNION ALL
@@ -1211,7 +1212,8 @@ BEGIN TRANSACTION
     SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
     SELECT 'AMBARI.ASSIGN_ROLES', 'Assign roles' UNION ALL
     SELECT 'AMBARI.MANAGE_STACK_VERSIONS', 'Manage stack versions' UNION ALL
    SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs';
    SELECT 'AMBARI.EDIT_STACK_REPOS', 'Edit stack repository URLs' UNION ALL
    SELECT 'AMBARI.RUN_CUSTOM_COMMAND', 'Perform custom administrative actions';
 
   -- Set authorizations for View User role
   INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1353,7 +1355,8 @@ BEGIN TRANSACTION
     SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.MANAGE_CONFIG_GROUPS' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
    SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='CLUSTER.ADMINISTRATOR';
 
   -- Set authorizations for Administrator role
   INSERT INTO permission_roleauthorization(permission_id, authorization_id)
@@ -1396,6 +1399,7 @@ BEGIN TRANSACTION
     SELECT permission_id, 'CLUSTER.TOGGLE_KERBEROS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.UPGRADE_DOWNGRADE_STACK' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'CLUSTER.MANAGE_USER_PERSISTED_DATA' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'CLUSTER.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
@@ -1404,7 +1408,8 @@ BEGIN TRANSACTION
     SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.ASSIGN_ROLES' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_STACK_VERSIONS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
    SELECT permission_id, 'AMBARI.EDIT_STACK_REPOS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'AMBARI.RUN_CUSTOM_COMMAND' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR';
 
   insert into adminprivilege (privilege_id, permission_id, resource_id, principal_id)
     select 1, 1, 1, 1;
diff --git a/ambari-server/src/main/resources/custom_action_definitions/system_action_definitions.xml b/ambari-server/src/main/resources/custom_action_definitions/system_action_definitions.xml
index bc1c2710ff..fc17584688 100644
-- a/ambari-server/src/main/resources/custom_action_definitions/system_action_definitions.xml
++ b/ambari-server/src/main/resources/custom_action_definitions/system_action_definitions.xml
@@ -39,6 +39,7 @@
     <defaultTimeout>60</defaultTimeout>
     <description>Update repo files on hosts</description>
     <targetType>ALL</targetType>
    <permissions>HOST.ADD_DELETE_COMPONENTS, HOST.ADD_DELETE_HOSTS, SERVICE.ADD_DELETE_SERVICES</permissions>
   </actionDefinition>
   <actionDefinition>
     <actionName>clear_repocache</actionName>
@@ -49,6 +50,7 @@
     <defaultTimeout>60</defaultTimeout>
     <description>Clear repository cache on hosts</description>
     <targetType>ALL</targetType>
    <permissions>HOST.ADD_DELETE_COMPONENTS, HOST.ADD_DELETE_HOSTS, SERVICE.ADD_DELETE_SERVICES</permissions>
   </actionDefinition>
   <actionDefinition>
     <actionName>validate_configs</actionName>
@@ -59,6 +61,7 @@
     <defaultTimeout>60</defaultTimeout>
     <description>Validate if provided service config can be applied to specified hosts</description>
     <targetType>ALL</targetType>
    <permissions>CLUSTER.MODIFY_CONFIGS, SERVICE.MODIFY_CONFIGS</permissions>
   </actionDefinition>
   <actionDefinition>
     <actionName>install_packages</actionName>
@@ -69,6 +72,7 @@
     <defaultTimeout>60</defaultTimeout>
     <description>Distribute repositories and install packages</description>
     <targetType>ALL</targetType>
    <permissions>HOST.ADD_DELETE_COMPONENTS, HOST.ADD_DELETE_HOSTS, SERVICE.ADD_DELETE_SERVICES</permissions>
   </actionDefinition>
   <actionDefinition>
     <actionName>ru_execute_tasks</actionName>
@@ -78,5 +82,6 @@
     <targetComponent/>
     <description>Perform upgrade action</description>
     <targetType>ANY</targetType>
    <permissions>CLUSTER.UPGRADE_DOWNGRADE_STACK</permissions>
   </actionDefinition>
 </actionDefinitions>
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RequestResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RequestResourceProviderTest.java
index d06aa1e4c6..5dfc74d9cf 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RequestResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RequestResourceProviderTest.java
@@ -1313,49 +1313,122 @@ public class RequestResourceProviderTest {
 
   @Test
   public void testCreateResourcesCheckHostForNonClusterAsAdministrator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createAdministrator(), "check_host",
    testCreateResources(TestAuthenticationFactory.createAdministrator(), null, null, "check_host",
         EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
   }
 
  @Test
  @Test(expected = AuthorizationException.class)
   public void testCreateResourcesCheckHostForNonClusterAsClusterAdministrator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createClusterAdministrator(), "check_host",
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), null, null, "check_host",
         EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
   }
 
  @Test
  @Test(expected = AuthorizationException.class)
   public void testCreateResourcesCheckHostForNonClusterAsClusterOperator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createClusterOperator(), "check_host",
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), null, null, "check_host",
         EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
   }
 
   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesCheckHostForNonClusterAsServiceAdministrator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createServiceAdministrator(), "check_host",
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), null, null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test
  public void testCreateResourcesCheckHostForClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "c1", null, "check_host",
         EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
   }
 
   @Test
  public void testCreateResourcesCheckJavaForNonClusterAsAdministrator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createAdministrator(), "check_java", null);
  public void testCreateResourcesCheckHostForClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "c1", null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test
  public void testCreateResourcesCheckHostForClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "c1", null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
   }
 
   @Test(expected = AuthorizationException.class)
  public void testCreateResourcesCheckJavaForNonClusterAsClusterAdministrator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createClusterAdministrator(), "check_java", null);
  public void testCreateResourcesCheckHostForClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "c1", null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
   }
 
   @Test(expected = AuthorizationException.class)
  public void testCreateResourcesCheckJavaForNonClusterAsClusterOperator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createClusterOperator(), "check_java", null);
  public void testCreateResourcesServiceCheckForClusterAsClusterUser() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterUser(), "c1", "SOME_SERVICE_CHECK", null, null);
  }
  @Test
  public void testCreateResourcesDecommissionForClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesDecommissionForClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesDecommissionForClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesDecommissionForClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
   }
 
   @Test(expected = AuthorizationException.class)
  public void testCreateResourcesDecommissionForClusterAsClusterUser() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterUser(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesCustomActionNoPrivsForNonClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), null, null, "custom_action", null);
  }

  @Test
  public void testCreateResourcesCustomActionNoPrivsForNonClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), null, null, "custom_action", null);
  }

  @Test
  public void testCreateResourcesCustomActionNoPrivsForNonClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), null, null, "custom_action", null);
  }

  @Test
   public void testCreateResourcesForNonClusterAsServiceAdministrator() throws Exception {
    testCreateResourcesForNonCluster(TestAuthenticationFactory.createServiceAdministrator(), "check_java", null);
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), null, null, "custom_action", null);
   }
 
  private void testCreateResourcesForNonCluster(Authentication authentication, String actionName, Set<RoleAuthorization> permissions) throws Exception {
  private void testCreateResources(Authentication authentication, String clusterName, String commandName, String actionName, Set<RoleAuthorization> permissions) throws Exception {
     Resource.Type type = Resource.Type.Request;
 
     Capture<ExecuteActionRequest> actionRequest = newCapture();
@@ -1373,8 +1446,18 @@ public class RequestResourceProviderTest {
     expect(actionDefinition.getPermissions()).andReturn(permissions).anyTimes();
     expect(response.getMessage()).andReturn("Message").anyTimes();
 
    Cluster cluster = createMock(Cluster.class);
    Clusters clusters = createMock(Clusters.class);
    if(clusterName != null) {
      expect(cluster.getResourceId()).andReturn(4L).anyTimes();

      expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();

      expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    }

     // replay
    replay(managementController, metaInfo, actionDefinition, response);
    replay(managementController, metaInfo, actionDefinition, response, cluster, clusters);
 
     // add the property map to a set for the request.  add more maps for multiple creates
     Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
@@ -1387,11 +1470,16 @@ public class RequestResourceProviderTest {
     filterSet.add(filterMap);
 
     properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName);
     propertySet.add(properties);
 
     Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(RequestResourceProvider.ACTION_ID, actionName);
    if(commandName != null) {
      requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, commandName);
    }
    if(actionName != null) {
      requestInfoProperties.put(RequestResourceProvider.ACTION_ID, actionName);
    }
 
     SecurityContextHolder.getContext().setAuthentication(authentication);
 
@@ -1406,9 +1494,15 @@ public class RequestResourceProviderTest {
     ExecuteActionRequest capturedRequest = actionRequest.getValue();
 
     Assert.assertTrue(actionRequest.hasCaptured());
    Assert.assertFalse("expected an action", capturedRequest.isCommand());
    Assert.assertEquals(actionName, capturedRequest.getActionName());
    Assert.assertEquals(null, capturedRequest.getCommandName());

    if(actionName != null) {
      Assert.assertFalse("expected an action", capturedRequest.isCommand());
      Assert.assertEquals(actionName, capturedRequest.getActionName());
    }
    if(commandName != null) {
      Assert.assertTrue("expected a command", capturedRequest.isCommand());
      Assert.assertEquals(commandName, capturedRequest.getCommandName());
    }
     Assert.assertNotNull(capturedRequest.getResourceFilters());
     Assert.assertEquals(1, capturedRequest.getResourceFilters().size());
     RequestResourceFilter capturedResourceFilter = capturedRequest.getResourceFilters().get(0);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/TestAuthenticationFactory.java b/ambari-server/src/test/java/org/apache/ambari/server/security/TestAuthenticationFactory.java
index d97cd9aa9f..12d1ac5935 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/TestAuthenticationFactory.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/TestAuthenticationFactory.java
@@ -217,6 +217,7 @@ public class TestAuthenticationFactory {
         RoleAuthorization.SERVICE_VIEW_METRICS,
         RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
         RoleAuthorization.SERVICE_VIEW_OPERATIONAL_LOGS,
        RoleAuthorization.CLUSTER_RUN_CUSTOM_COMMAND,
         RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA)));
     return permissionEntity;
   }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
index c4e0a7c78a..7b6c3ad9b5 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
@@ -18,24 +18,24 @@
 
 package org.apache.ambari.server.upgrade;
 
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

 import javax.persistence.EntityManager;
 
 import com.google.common.collect.Maps;
 import com.google.gson.Gson;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.actionmanager.ActionManager;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
 import org.apache.ambari.server.controller.KerberosHelper;
 import org.apache.ambari.server.controller.MaintenanceStateHelper;
 import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.Config;
@@ -54,14 +54,26 @@ import com.google.inject.Module;
 import com.google.inject.Provider;
 
 import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
 import java.util.HashMap;
 import java.util.Map;
 
 import static org.easymock.EasyMock.anyObject;
 import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
 import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
 import static org.junit.Assert.assertTrue;

 /**
  * {@link UpgradeCatalog250} unit tests.
  */
@@ -111,14 +123,19 @@ public class UpgradeCatalog250Test {
   @Test
   public void testExecuteDMLUpdates() throws Exception {
     Method updateAmsConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateAMSConfigs");
    Method createRoleAuthorizations = UpgradeCatalog250.class.getDeclaredMethod("createRoleAuthorizations");
 
     UpgradeCatalog250 upgradeCatalog250 = createMockBuilder(UpgradeCatalog250.class)
      .addMockedMethod(updateAmsConfigs)
      .createMock();
        .addMockedMethod(updateAmsConfigs)
        .addMockedMethod(createRoleAuthorizations)
        .createMock();
 
     upgradeCatalog250.updateAMSConfigs();
     expectLastCall().once();
 
    upgradeCatalog250.createRoleAuthorizations();
    expectLastCall().once();

     replay(upgradeCatalog250);
 
     upgradeCatalog250.executeDMLUpdates();
@@ -196,4 +213,82 @@ public class UpgradeCatalog250Test {
     Map<String, String> updatedProperties = propertiesCapture.getValue();
     assertTrue(Maps.difference(newPropertiesAmsEnv, updatedProperties).areEqual());
   }

  @Test
  public void testCreateRoleAuthorizations() throws AmbariException, SQLException {

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    ResourceTypeEntity ambariResourceTypeEntity = easyMockSupport.createMock(ResourceTypeEntity.class);

    ResourceTypeEntity clusterResourceTypeEntity = easyMockSupport.createMock(ResourceTypeEntity.class);

    Collection<RoleAuthorizationEntity> ambariAdministratorAuthorizations = new ArrayList<RoleAuthorizationEntity>();
    Collection<RoleAuthorizationEntity> clusterAdministratorAuthorizations = new ArrayList<RoleAuthorizationEntity>();

    PermissionEntity clusterAdministratorPermissionEntity = easyMockSupport.createMock(PermissionEntity.class);
    expect(clusterAdministratorPermissionEntity.getAuthorizations())
        .andReturn(clusterAdministratorAuthorizations)
        .times(1);

    PermissionEntity ambariAdministratorPermissionEntity = easyMockSupport.createMock(PermissionEntity.class);
    expect(ambariAdministratorPermissionEntity.getAuthorizations())
        .andReturn(ambariAdministratorAuthorizations)
        .times(2);

    PermissionDAO permissionDAO = easyMockSupport.createMock(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR", ambariResourceTypeEntity))
        .andReturn(ambariAdministratorPermissionEntity)
        .times(2);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.ADMINISTRATOR", clusterResourceTypeEntity))
        .andReturn(clusterAdministratorPermissionEntity)
        .times(1);
    expect(permissionDAO.merge(ambariAdministratorPermissionEntity))
        .andReturn(ambariAdministratorPermissionEntity)
        .times(2);
    expect(permissionDAO.merge(clusterAdministratorPermissionEntity))
        .andReturn(clusterAdministratorPermissionEntity)
        .times(1);

    ResourceTypeDAO resourceTypeDAO = easyMockSupport.createMock(ResourceTypeDAO.class);
    expect(resourceTypeDAO.findByName("AMBARI")).andReturn(ambariResourceTypeEntity).times(2);
    expect(resourceTypeDAO.findByName("CLUSTER")).andReturn(clusterResourceTypeEntity).times(1);

    RoleAuthorizationDAO roleAuthorizationDAO = easyMockSupport.createMock(RoleAuthorizationDAO.class);
    expect(roleAuthorizationDAO.findById("CLUSTER.RUN_CUSTOM_COMMAND")).andReturn(null).times(1);
    expect(roleAuthorizationDAO.findById("AMBARI.RUN_CUSTOM_COMMAND")).andReturn(null).times(1);

    Capture<RoleAuthorizationEntity> captureClusterRunCustomCommandEntity = newCapture();
    roleAuthorizationDAO.create(capture(captureClusterRunCustomCommandEntity));
    expectLastCall().times(1);

    Capture<RoleAuthorizationEntity> captureAmbariRunCustomCommandEntity = newCapture();
    roleAuthorizationDAO.create(capture(captureAmbariRunCustomCommandEntity));
    expectLastCall().times(1);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(RoleAuthorizationDAO.class)).andReturn(roleAuthorizationDAO).atLeastOnce();
    expect(injector.getInstance(PermissionDAO.class)).andReturn(permissionDAO).atLeastOnce();
    expect(injector.getInstance(ResourceTypeDAO.class)).andReturn(resourceTypeDAO).atLeastOnce();

    easyMockSupport.replayAll();
    new UpgradeCatalog250(injector).createRoleAuthorizations();
    easyMockSupport.verifyAll();

    RoleAuthorizationEntity ambariRunCustomCommandEntity = captureAmbariRunCustomCommandEntity.getValue();
    RoleAuthorizationEntity clusterRunCustomCommandEntity = captureClusterRunCustomCommandEntity.getValue();

    Assert.assertEquals("AMBARI.RUN_CUSTOM_COMMAND", ambariRunCustomCommandEntity.getAuthorizationId());
    Assert.assertEquals("Perform custom administrative actions", ambariRunCustomCommandEntity.getAuthorizationName());

    Assert.assertEquals("CLUSTER.RUN_CUSTOM_COMMAND", clusterRunCustomCommandEntity.getAuthorizationId());
    Assert.assertEquals("Perform custom cluster-level actions", clusterRunCustomCommandEntity.getAuthorizationName());

    Assert.assertEquals(2, ambariAdministratorAuthorizations.size());
    Assert.assertTrue(ambariAdministratorAuthorizations.contains(clusterRunCustomCommandEntity));
    Assert.assertTrue(ambariAdministratorAuthorizations.contains(ambariRunCustomCommandEntity));

    Assert.assertEquals(1, clusterAdministratorAuthorizations.size());
    Assert.assertTrue(clusterAdministratorAuthorizations.contains(clusterRunCustomCommandEntity));
  }
 }
- 
2.19.1.windows.1

