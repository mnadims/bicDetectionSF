From 3b2e103b7e36af9acd91fe3f6f057fb2163f7ef7 Mon Sep 17 00:00:00 2001
From: Robert Levas <rlevas@hortonworks.com>
Date: Wed, 2 Nov 2016 12:16:40 -0400
Subject: [PATCH] AMBARI-18751. Upgrade Fails From 2.4.2 to 2.5 Due To Existing
 Role Authorizations (rlevas)

--
 .../server/upgrade/UpgradeCatalog242.java     | 17 ++++
 .../server/upgrade/UpgradeCatalog250.java     | 17 ----
 .../server/upgrade/UpgradeCatalog242Test.java | 90 +++++++++++++++++-
 .../server/upgrade/UpgradeCatalog250Test.java | 92 -------------------
 4 files changed, 106 insertions(+), 110 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog242.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog242.java
index 541f4da3dd..f5445ea0be 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog242.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog242.java
@@ -19,6 +19,8 @@
 package org.apache.ambari.server.upgrade;
 
 import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
@@ -123,9 +125,24 @@ public class UpgradeCatalog242 extends AbstractUpgradeCatalog {
   @Override
   protected void executeDMLUpdates() throws AmbariException, SQLException {
     addNewConfigurationsFromXml();
    createRoleAuthorizations();
     convertRolePrincipals();
   }
 
  /**
   * Create new role authorizations: CLUSTER.RUN_CUSTOM_COMMAND and AMBARI.RUN_CUSTOM_COMMAND
   *
   * @throws SQLException
   */
  @Transactional
  protected void createRoleAuthorizations() throws SQLException {
    addRoleAuthorization("CLUSTER.RUN_CUSTOM_COMMAND", "Perform custom cluster-level actions",
        Arrays.asList("AMBARI.ADMINISTRATOR:AMBARI", "CLUSTER.ADMINISTRATOR:CLUSTER"));

    addRoleAuthorization("AMBARI.RUN_CUSTOM_COMMAND", "Perform custom administrative actions",
        Collections.singletonList("AMBARI.ADMINISTRATOR:AMBARI"));
  }

   protected void updateTablesForMysql() throws SQLException {
     final Configuration.DatabaseType databaseType = configuration.getDatabaseType();
     if (databaseType == Configuration.DatabaseType.MYSQL) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
index 723edf4c89..fa7121c81f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
@@ -19,7 +19,6 @@ package org.apache.ambari.server.upgrade;
 
 import java.sql.SQLException;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
@@ -131,7 +130,6 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
   @Override
   protected void executeDMLUpdates() throws AmbariException, SQLException {
     updateAMSConfigs();
    createRoleAuthorizations();
     updateKafkaConfigs();
   }
 
@@ -192,21 +190,6 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
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

   protected void updateKafkaConfigs() throws AmbariException {
     AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
     Clusters clusters = ambariManagementController.getClusters();
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog242Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog242Test.java
index d6f33369ab..d98a2162d6 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog242Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog242Test.java
@@ -20,9 +20,9 @@ package org.apache.ambari.server.upgrade;
 
 import javax.persistence.EntityManager;
 import junit.framework.Assert;
import static org.easymock.EasyMock.aryEq;
 
 import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.aryEq;
 import static org.easymock.EasyMock.capture;
 import static org.easymock.EasyMock.createMockBuilder;
 import static org.easymock.EasyMock.createNiceMock;
@@ -38,6 +38,7 @@ import static org.easymock.EasyMock.verify;
 import java.lang.reflect.Method;
 import java.sql.SQLException;
 import java.util.ArrayList;
import java.util.Collection;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
@@ -57,12 +58,16 @@ import org.apache.ambari.server.orm.dao.PrincipalDAO;
 import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
 import org.apache.ambari.server.orm.dao.PrivilegeDAO;
 import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
 import org.apache.ambari.server.orm.dao.StackDAO;
 import org.apache.ambari.server.orm.entities.PermissionEntity;
 import org.apache.ambari.server.orm.entities.PrincipalEntity;
 import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
 import org.apache.ambari.server.orm.entities.PrivilegeEntity;
 import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
 import org.apache.ambari.server.orm.entities.StackEntity;
 import org.apache.ambari.server.state.stack.OsFamily;
 import org.easymock.Capture;
@@ -239,16 +244,21 @@ public class UpgradeCatalog242Test {
   public void testExecuteDMLUpdates() throws Exception {
     Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
     Method convertRolePrincipals = UpgradeCatalog242.class.getDeclaredMethod("convertRolePrincipals");
    Method createRoleAuthorizations = UpgradeCatalog242.class.getDeclaredMethod("createRoleAuthorizations");
 
     UpgradeCatalog242 upgradeCatalog242 = createMockBuilder(UpgradeCatalog242.class)
         .addMockedMethod(addNewConfigurationsFromXml)
         .addMockedMethod(convertRolePrincipals)
        .addMockedMethod(createRoleAuthorizations)
         .createMock();
 
 
     upgradeCatalog242.addNewConfigurationsFromXml();
     expectLastCall().once();
 
    upgradeCatalog242.createRoleAuthorizations();
    expectLastCall().once();

     upgradeCatalog242.convertRolePrincipals();
     expectLastCall().once();
 
@@ -365,4 +375,82 @@ public class UpgradeCatalog242Test {
     upgradeCatalog.convertRolePrincipals();
     easyMockSupport.verifyAll();
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
    new UpgradeCatalog242(injector).createRoleAuthorizations();
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
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
index 65ba406183..6d2011b6a4 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
@@ -36,17 +36,13 @@ import static org.junit.Assert.assertTrue;
 import java.lang.reflect.Method;
 import java.sql.Connection;
 import java.sql.ResultSet;
import java.sql.SQLException;
 import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.persistence.EntityManager;
 
import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.actionmanager.ActionManager;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.AmbariManagementController;
@@ -54,12 +50,6 @@ import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
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
@@ -209,21 +199,16 @@ public class UpgradeCatalog250Test {
   @Test
   public void testExecuteDMLUpdates() throws Exception {
     Method updateAmsConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateAMSConfigs");
    Method createRoleAuthorizations = UpgradeCatalog250.class.getDeclaredMethod("createRoleAuthorizations");
     Method updateKafkaConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateKafkaConfigs");
 
     UpgradeCatalog250 upgradeCatalog250 = createMockBuilder(UpgradeCatalog250.class)
         .addMockedMethod(updateAmsConfigs)
        .addMockedMethod(createRoleAuthorizations)
         .addMockedMethod(updateKafkaConfigs)
         .createMock();
 
     upgradeCatalog250.updateAMSConfigs();
     expectLastCall().once();
 
    upgradeCatalog250.createRoleAuthorizations();
    expectLastCall().once();

     upgradeCatalog250.updateKafkaConfigs();
     expectLastCall().once();
 
@@ -305,83 +290,6 @@ public class UpgradeCatalog250Test {
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
 
   @Test
   public void testKafkaUpdateConfigs() throws Exception{
- 
2.19.1.windows.1

