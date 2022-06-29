From a00ffffc54a8063c1728d2f3ba24db04504864da Mon Sep 17 00:00:00 2001
From: Attila Doroszlai <adoroszlai@hortonworks.com>
Date: Mon, 5 Dec 2016 16:42:14 -0500
Subject: [PATCH] AMBARI-19086. LDAP sync creates groups with Local type
 (Attila Doroszla via rlevas)

--
 .../apache/ambari/server/orm/entities/GroupEntity.java |  5 +++--
 .../ambari/server/security/authorization/Users.java    | 10 ++++------
 .../security/ldap/AmbariLdapDataPopulatorTest.java     |  3 ++-
 3 files changed, 9 insertions(+), 9 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java
index 58b2e5d5ac..dc71b61dc9 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java
@@ -101,7 +101,7 @@ public class GroupEntity {
     return ldapGroup == 0 ? Boolean.FALSE : Boolean.TRUE;
   }
 
  public void setLdapGroup(Boolean ldapGroup) {
  private void setLdapGroup(Boolean ldapGroup) {
     if (ldapGroup == null) {
       this.ldapGroup = null;
     } else {
@@ -113,8 +113,9 @@ public class GroupEntity {
     return groupType;
   }
 
  public void setgroupType(GroupType groupType) {
  public void setGroupType(GroupType groupType) {
     this.groupType = groupType;
    setLdapGroup(groupType == GroupType.LDAP);
   }
 
   public Set<MemberEntity> getMemberEntities() {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
index 2cd538c9a1..e69bbc9108 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
@@ -49,10 +49,8 @@ import org.apache.ambari.server.orm.entities.PermissionEntity;
 import org.apache.ambari.server.orm.entities.PrincipalEntity;
 import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
 import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
 import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
 import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
 import org.apache.ambari.server.security.ldap.LdapBatchDto;
 import org.apache.ambari.server.security.ldap.LdapUserGroupMemberDto;
 import org.apache.commons.lang.StringUtils;
@@ -277,7 +275,7 @@ public class Users {
   public synchronized void setGroupLdap(String groupName) throws AmbariException {
     GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
     if (groupEntity != null) {
      groupEntity.setLdapGroup(true);
      groupEntity.setGroupType(GroupType.LDAP);
       groupDAO.merge(groupEntity);
     } else {
       throw new AmbariException("Group " + groupName + " doesn't exist");
@@ -435,7 +433,7 @@ public class Users {
     final GroupEntity groupEntity = new GroupEntity();
     groupEntity.setGroupName(groupName);
     groupEntity.setPrincipal(principalEntity);
    groupEntity.setgroupType(groupType);
    groupEntity.setGroupType(groupType);
 
     groupDAO.create(groupEntity);
   }
@@ -701,7 +699,7 @@ public class Users {
     final Set<GroupEntity> groupsToBecomeLdap = new HashSet<GroupEntity>();
     for (String groupName : batchInfo.getGroupsToBecomeLdap()) {
       final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
      groupEntity.setLdapGroup(true);
      groupEntity.setGroupType(GroupType.LDAP);
       allGroups.put(groupEntity.getGroupName(), groupEntity);
       groupsToBecomeLdap.add(groupEntity);
     }
@@ -737,7 +735,7 @@ public class Users {
       final GroupEntity groupEntity = new GroupEntity();
       groupEntity.setGroupName(groupName);
       groupEntity.setPrincipal(principalEntity);
      groupEntity.setLdapGroup(true);
      groupEntity.setGroupType(GroupType.LDAP);
 
       allGroups.put(groupEntity.getGroupName(), groupEntity);
       groupsToCreate.add(groupEntity);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/ldap/AmbariLdapDataPopulatorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/ldap/AmbariLdapDataPopulatorTest.java
index 1866b12c19..2840e3d0b7 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/ldap/AmbariLdapDataPopulatorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/ldap/AmbariLdapDataPopulatorTest.java
@@ -38,6 +38,7 @@ import org.apache.ambari.server.orm.entities.PrivilegeEntity;
 import org.apache.ambari.server.orm.entities.UserEntity;
 import org.apache.ambari.server.security.authorization.AmbariLdapUtils;
 import org.apache.ambari.server.security.authorization.Group;
import org.apache.ambari.server.security.authorization.GroupType;
 import org.apache.ambari.server.security.authorization.LdapServerProperties;
 import org.apache.ambari.server.security.authorization.User;
 import org.apache.ambari.server.security.authorization.Users;
@@ -1576,7 +1577,7 @@ public class AmbariLdapDataPopulatorTest {
     final GroupEntity ldapGroup = new GroupEntity();
     ldapGroup.setGroupId(1);
     ldapGroup.setGroupName("ldapGroup");
    ldapGroup.setLdapGroup(true);
    ldapGroup.setGroupType(GroupType.LDAP);
     ldapGroup.setMemberEntities(new HashSet<MemberEntity>());
 
     final User ldapUserWithoutGroup = createLdapUserWithoutGroup();
- 
2.19.1.windows.1

