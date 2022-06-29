From 9752f7e772398bf0c7eb86b63984a8f5b2b2f9b7 Mon Sep 17 00:00:00 2001
From: Robert Levas <rlevas@hortonworks.com>
Date: Thu, 24 Mar 2016 16:11:02 -0400
Subject: [PATCH] AMBARI-15543. RBAC based user access to view instances are
 not honoured (rlevas)

--
 .../authorization/AuthorizationHelper.java    |  3 -
 .../AuthorizationHelperTest.java              | 93 ++++++++++++++++++-
 2 files changed, 92 insertions(+), 4 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
index b136182392..0c675b88dc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
@@ -160,9 +160,6 @@ public class AuthorizationHelper {
         if (ResourceType.AMBARI == privilegeResourceType) {
           // This resource type indicates administrative access
           resourceOK = true;
        } else if (ResourceType.VIEW == privilegeResourceType) {
          // For a VIEW USER.
          resourceOK = true;
         } else if ((resourceType == null) || (resourceType == privilegeResourceType)) {
           resourceOK = (resourceId == null) || resourceId.equals(privilegeResource.getId());
         } else {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java
index 62f719d8a8..ada5ff5259 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java
@@ -130,6 +130,10 @@ public class AuthorizationHelperTest {
     RoleAuthorizationEntity administratorRoleAuthorizationEntity = new RoleAuthorizationEntity();
     administratorRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.AMBARI_MANAGE_USERS.getId());
 
    ResourceTypeEntity ambariResourceTypeEntity = new ResourceTypeEntity();
    ambariResourceTypeEntity.setId(1);
    ambariResourceTypeEntity.setName(ResourceType.AMBARI.name());

     ResourceTypeEntity clusterResourceTypeEntity = new ResourceTypeEntity();
     clusterResourceTypeEntity.setId(1);
     clusterResourceTypeEntity.setName(ResourceType.CLUSTER.name());
@@ -138,6 +142,10 @@ public class AuthorizationHelperTest {
     cluster2ResourceTypeEntity.setId(2);
     cluster2ResourceTypeEntity.setName(ResourceType.CLUSTER.name());
 
    ResourceEntity ambariResourceEntity = new ResourceEntity();
    ambariResourceEntity.setResourceType(ambariResourceTypeEntity);
    ambariResourceEntity.setId(1L);

     ResourceEntity clusterResourceEntity = new ResourceEntity();
     clusterResourceEntity.setResourceType(clusterResourceTypeEntity);
     clusterResourceEntity.setId(1L);
@@ -176,7 +184,7 @@ public class AuthorizationHelperTest {
 
     PrivilegeEntity administratorPrivilegeEntity = new PrivilegeEntity();
     administratorPrivilegeEntity.setPermission(administratorPermissionEntity);
    administratorPrivilegeEntity.setResource(clusterResourceEntity);
    administratorPrivilegeEntity.setResource(ambariResourceEntity);
 
     GrantedAuthority readOnlyAuthority = new AmbariGrantedAuthority(readOnlyPrivilegeEntity);
     GrantedAuthority readOnly2Authority = new AmbariGrantedAuthority(readOnly2PrivilegeEntity);
@@ -246,6 +254,89 @@ public class AuthorizationHelperTest {
     assertTrue(AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, 1L, EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS)));
   }
 
  @Test
  public void testIsAuthorizedForSpecificView() {
    RoleAuthorizationEntity readOnlyRoleAuthorizationEntity = new RoleAuthorizationEntity();
    readOnlyRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.CLUSTER_VIEW_METRICS.getId());

    RoleAuthorizationEntity viewUseRoleAuthorizationEntity = new RoleAuthorizationEntity();
    viewUseRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.VIEW_USE.getId());

    RoleAuthorizationEntity administratorRoleAuthorizationEntity = new RoleAuthorizationEntity();
    administratorRoleAuthorizationEntity.setAuthorizationId(RoleAuthorization.AMBARI_MANAGE_USERS.getId());

    ResourceTypeEntity ambariResourceTypeEntity = new ResourceTypeEntity();
    ambariResourceTypeEntity.setId(1);
    ambariResourceTypeEntity.setName(ResourceType.AMBARI.name());

    ResourceTypeEntity clusterResourceTypeEntity = new ResourceTypeEntity();
    clusterResourceTypeEntity.setId(1);
    clusterResourceTypeEntity.setName(ResourceType.CLUSTER.name());

    ResourceTypeEntity viewResourceTypeEntity = new ResourceTypeEntity();
    viewResourceTypeEntity.setId(30);
    viewResourceTypeEntity.setName(ResourceType.VIEW.name());

    ResourceEntity ambariResourceEntity = new ResourceEntity();
    ambariResourceEntity.setResourceType(ambariResourceTypeEntity);
    ambariResourceEntity.setId(1L);

    ResourceEntity clusterResourceEntity = new ResourceEntity();
    clusterResourceEntity.setResourceType(clusterResourceTypeEntity);
    clusterResourceEntity.setId(1L);

    ResourceEntity viewResourceEntity = new ResourceEntity();
    viewResourceEntity.setResourceType(viewResourceTypeEntity);
    viewResourceEntity.setId(53L);

    PermissionEntity readOnlyPermissionEntity = new PermissionEntity();
    readOnlyPermissionEntity.setAuthorizations(Collections.singleton(readOnlyRoleAuthorizationEntity));

    PermissionEntity viewUsePermissionEntity = new PermissionEntity();
    viewUsePermissionEntity.setAuthorizations(Arrays.asList(readOnlyRoleAuthorizationEntity,
        viewUseRoleAuthorizationEntity));

    PermissionEntity administratorPermissionEntity = new PermissionEntity();
    administratorPermissionEntity.setAuthorizations(Arrays.asList(readOnlyRoleAuthorizationEntity,
        viewUseRoleAuthorizationEntity,
        administratorRoleAuthorizationEntity));

    PrivilegeEntity readOnlyPrivilegeEntity = new PrivilegeEntity();
    readOnlyPrivilegeEntity.setPermission(readOnlyPermissionEntity);
    readOnlyPrivilegeEntity.setResource(clusterResourceEntity);

    PrivilegeEntity viewUsePrivilegeEntity = new PrivilegeEntity();
    viewUsePrivilegeEntity.setPermission(viewUsePermissionEntity);
    viewUsePrivilegeEntity.setResource(viewResourceEntity);

    PrivilegeEntity administratorPrivilegeEntity = new PrivilegeEntity();
    administratorPrivilegeEntity.setPermission(administratorPermissionEntity);
    administratorPrivilegeEntity.setResource(ambariResourceEntity);

    GrantedAuthority readOnlyAuthority = new AmbariGrantedAuthority(readOnlyPrivilegeEntity);
    GrantedAuthority viewUseAuthority = new AmbariGrantedAuthority(viewUsePrivilegeEntity);
    GrantedAuthority administratorAuthority = new AmbariGrantedAuthority(administratorPrivilegeEntity);

    Authentication readOnlyUser = new TestAuthentication(Collections.singleton(readOnlyAuthority));
    Authentication viewUser = new TestAuthentication(Arrays.asList(readOnlyAuthority, viewUseAuthority));
    Authentication administratorUser = new TestAuthentication(Collections.singleton(administratorAuthority));

    SecurityContext context = SecurityContextHolder.getContext();
    Set<RoleAuthorization> permissionsViewUse = EnumSet.of(RoleAuthorization.VIEW_USE);

    context.setAuthentication(readOnlyUser);
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 53L, permissionsViewUse));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 50L, permissionsViewUse));

    context.setAuthentication(viewUser);
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 53L, permissionsViewUse));
    assertFalse(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 50L, permissionsViewUse));

    context.setAuthentication(administratorUser);
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 53L, permissionsViewUse));
    assertTrue(AuthorizationHelper.isAuthorized(ResourceType.VIEW, 50L, permissionsViewUse));
  }

   private class TestAuthentication implements Authentication {
     private final Collection<? extends GrantedAuthority> grantedAuthorities;
 
- 
2.19.1.windows.1

