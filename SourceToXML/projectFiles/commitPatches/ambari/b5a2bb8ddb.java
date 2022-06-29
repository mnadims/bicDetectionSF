From b5a2bb8ddbc7badcdd459b443077d429c5e8235d Mon Sep 17 00:00:00 2001
From: Vishal Ghugare <ghugare@us.ibm.com>
Date: Tue, 15 Nov 2016 09:19:06 -0500
Subject: [PATCH] AMBARI-12263.  Support PAM as authentication mechanism for
 accessing Ambari UI/REST (Vishal Ghugare via rlevas)

--
 ambari-server/pom.xml                         |  10 +
 ambari-server/sbin/ambari-server              |   4 +
 .../server/configuration/Configuration.java   |  23 ++
 .../AmbariManagementControllerImpl.java       |   7 +-
 .../server/controller/AmbariServer.java       |   3 +
 .../server/controller/GroupResponse.java      |  14 +
 .../internal/GroupResourceProvider.java       |   4 +
 .../UserPrivilegeResourceProvider.java        |   3 +
 .../ambari/server/orm/dao/GroupDAO.java       |  19 +-
 .../ambari/server/orm/dao/ResourceDAO.java    |  21 ++
 .../server/orm/entities/GroupEntity.java      |  18 ++
 .../server/security/ClientSecurityType.java   |   3 +-
 .../AmbariPamAuthenticationProvider.java      | 252 ++++++++++++++++++
 .../server/security/authorization/Group.java  |   6 +
 .../security/authorization/GroupType.java     |  25 ++
 .../PamAuthenticationException.java           |  36 +++
 .../security/authorization/UserType.java      |   3 +-
 .../server/security/authorization/Users.java  |  54 +++-
 .../server/upgrade/UpgradeCatalog250.java     |  11 +
 .../src/main/python/ambari-server.py          |   7 +-
 .../main/python/ambari_server/setupActions.py |   1 +
 .../python/ambari_server/setupSecurity.py     |  53 +++-
 .../resources/Ambari-DDL-MySQL-CREATE.sql     |   1 +
 .../resources/Ambari-DDL-Oracle-CREATE.sql    |   1 +
 .../resources/Ambari-DDL-Postgres-CREATE.sql  |   1 +
 .../Ambari-DDL-SQLAnywhere-CREATE.sql         |   1 +
 .../resources/Ambari-DDL-SQLServer-CREATE.sql |   1 +
 .../src/main/resources/properties.json        |   1 +
 .../webapp/WEB-INF/spring-security.xml        |   1 +
 .../AmbariPamAuthenticationProviderTest.java  |  97 +++++++
 .../security/authorization/TestUsers.java     |  10 +-
 .../server/upgrade/UpgradeCatalog250Test.java |  13 +
 32 files changed, 686 insertions(+), 18 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProvider.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/security/authorization/GroupType.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/security/authorization/PamAuthenticationException.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProviderTest.java

diff --git a/ambari-server/pom.xml b/ambari-server/pom.xml
index e02b7a59c6..36c57de525 100644
-- a/ambari-server/pom.xml
++ b/ambari-server/pom.xml
@@ -1469,6 +1469,16 @@
       <version>1.0.0.0-SNAPSHOT</version>
       <scope>test</scope>
     </dependency>
    <dependency>
      <groupId>org.kohsuke</groupId>
      <artifactId>libpam4j</artifactId>
      <version>1.8</version>
    </dependency>
    <dependency>
      <groupId>net.java.dev.jna</groupId>
      <artifactId>jna</artifactId>
      <version>4.1.0</version>
    </dependency>
   </dependencies>
 
   <pluginRepositories>
diff --git a/ambari-server/sbin/ambari-server b/ambari-server/sbin/ambari-server
index bdbdd0f4f4..f08db13d62 100755
-- a/ambari-server/sbin/ambari-server
++ b/ambari-server/sbin/ambari-server
@@ -132,6 +132,10 @@ case "${1:-}" in
         echo -e "Updating jce policy"
         $PYTHON "$AMBARI_PYTHON_EXECUTABLE" $@
         ;;
  setup-pam)
        echo -e "Setting up PAM properties..."
        $PYTHON "$AMBARI_PYTHON_EXECUTABLE" $@
        ;;
   setup-ldap)
         echo -e "Setting up LDAP properties..."
         $PYTHON "$AMBARI_PYTHON_EXECUTABLE" $@
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java b/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
index 0b8e195292..b8b8f54c6a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
@@ -724,6 +724,21 @@ public class Configuration {
   public static final ConfigurationProperty<String> JCE_NAME = new ConfigurationProperty<>(
       "jce.name", null);
 
  /**
   * The auto group creation by Ambari.
   */
  @Markdown(
      description = "The auto group creation by Ambari")
  public static final ConfigurationProperty<Boolean> AUTO_GROUP_CREATION = new ConfigurationProperty<>(
      "auto.group.creation", Boolean.FALSE);

  /**
   * The PAM configuration file.
   */
  @Markdown(description = "The PAM configuration file.")
  public static final ConfigurationProperty<String> PAM_CONFIGURATION_FILE = new ConfigurationProperty<>(
      "pam.configuration", null);

   /**
    * The type of authentication mechanism used by Ambari.
    *
@@ -5747,4 +5762,12 @@ public class Configuration {
     String acceptors = getProperty(SRVR_API_ACCEPTOR_THREAD_COUNT);
     return StringUtils.isEmpty(acceptors) ? null : Integer.parseInt(acceptors);
   }
 
  public String getPamConfigurationFile() {
    return getProperty(PAM_CONFIGURATION_FILE);
  }

  public String getAutoGroupCreation() {
    return getProperty(AUTO_GROUP_CREATION);
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index b04fdd77c0..8e2fe741aa 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -129,6 +129,7 @@ import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
 import org.apache.ambari.server.security.authorization.AuthorizationException;
 import org.apache.ambari.server.security.authorization.AuthorizationHelper;
 import org.apache.ambari.server.security.authorization.Group;
import org.apache.ambari.server.security.authorization.GroupType;
 import org.apache.ambari.server.security.authorization.ResourceType;
 import org.apache.ambari.server.security.authorization.RoleAuthorization;
 import org.apache.ambari.server.security.authorization.User;
@@ -973,7 +974,7 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       if (group != null) {
         throw new AmbariException("Group already exists.");
       }
      users.createGroup(request.getGroupName());
      users.createGroup(request.getGroupName(), GroupType.LOCAL);
     }
   }
 
@@ -3685,7 +3686,7 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
       // get them all
       if (null == request.getGroupName()) {
         for (Group group: users.getAllGroups()) {
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup());
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup(), group.getGroupType());
           responses.add(response);
         }
       } else {
@@ -3698,7 +3699,7 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
                 + request.getGroupName() + "'");
           }
         } else {
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup());
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup(), group.getGroupType());
           responses.add(response);
         }
       }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java
index e54d54e166..537ebc5b6c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java
@@ -103,6 +103,7 @@ import org.apache.ambari.server.security.authorization.AmbariLocalUserProvider;
 import org.apache.ambari.server.security.authorization.AmbariUserAuthorizationFilter;
 import org.apache.ambari.server.security.authorization.PermissionHelper;
 import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.authorization.AmbariPamAuthenticationProvider;
 import org.apache.ambari.server.security.authorization.internal.AmbariInternalAuthenticationProvider;
 import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
 import org.apache.ambari.server.security.unsecured.rest.CertificateDownload;
@@ -339,6 +340,8 @@ public class AmbariServer {
         injector.getInstance(AmbariUserAuthorizationFilter.class));
       factory.registerSingleton("ambariInternalAuthenticationProvider",
         injector.getInstance(AmbariInternalAuthenticationProvider.class));
      factory.registerSingleton("ambariPamAuthenticationProvider",
	injector.getInstance(AmbariPamAuthenticationProvider.class));
 
       // Spring Security xml config depends on this Bean
       String[] contextLocations = {SPRING_CONTEXT_LOCATION};
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/GroupResponse.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/GroupResponse.java
index ef28f61425..0baccc7398 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/GroupResponse.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/GroupResponse.java
@@ -17,16 +17,26 @@
  */
 package org.apache.ambari.server.controller;
 
import org.apache.ambari.server.security.authorization.GroupType;

 /**
  * Represents a user group maintenance response.
  */
 public class GroupResponse {
   private final String groupName;
   private final boolean ldapGroup;
  private final GroupType groupType;

  public GroupResponse(String groupName, boolean ldapGroup, GroupType groupType) {
    this.groupName = groupName;
    this.ldapGroup = ldapGroup;
    this.groupType = groupType;
  }
 
   public GroupResponse(String groupName, boolean ldapGroup) {
     this.groupName = groupName;
     this.ldapGroup = ldapGroup;
    this.groupType = GroupType.LOCAL;
   }
 
   public String getGroupName() {
@@ -37,6 +47,10 @@ public class GroupResponse {
     return ldapGroup;
   }
 
  public GroupType getGroupType() {
    return groupType;
  }

   @Override
   public boolean equals(Object o) {
     if (this == o)
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/GroupResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/GroupResourceProvider.java
index e1aa5acf53..e07dece700 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/GroupResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/GroupResourceProvider.java
@@ -49,6 +49,7 @@ public class GroupResourceProvider extends AbstractControllerResourceProvider {
   // Groups
   public static final String GROUP_GROUPNAME_PROPERTY_ID  = PropertyHelper.getPropertyId("Groups", "group_name");
   public static final String GROUP_LDAP_GROUP_PROPERTY_ID = PropertyHelper.getPropertyId("Groups", "ldap_group");
  public static final String GROUP_GROUPTYPE_PROPERTY_ID  = PropertyHelper.getPropertyId("Groups", "group_type");
 
   private static Set<String> pkPropertyIds =
       new HashSet<String>(Arrays.asList(new String[]{
@@ -132,6 +133,9 @@ public class GroupResourceProvider extends AbstractControllerResourceProvider {
       setResourceProperty(resource, GROUP_LDAP_GROUP_PROPERTY_ID,
           groupResponse.isLdapGroup(), requestedIds);
 
      setResourceProperty(resource, GROUP_GROUPTYPE_PROPERTY_ID,
          groupResponse.getGroupType(), requestedIds);

       resources.add(resource);
     }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserPrivilegeResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserPrivilegeResourceProvider.java
index ba32a5f162..0575c1d94d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserPrivilegeResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserPrivilegeResourceProvider.java
@@ -288,6 +288,9 @@ public class UserPrivilegeResourceProvider extends ReadOnlyResourceProvider {
           userEntity = usersCache.get().getUnchecked(userName);
         }
 
        if (userEntity == null) {
            userEntity = userDAO.findUserByNameAndType(userName, UserType.PAM);
        }
         if (userEntity == null) {
           throw new SystemException("User " + userName + " was not found");
         }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/GroupDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/GroupDAO.java
index 255c5e6da8..8b5902c102 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/GroupDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/GroupDAO.java
@@ -29,13 +29,14 @@ import javax.persistence.TypedQuery;
 
 import org.apache.ambari.server.orm.RequiresSession;
 import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.security.authorization.GroupType;

 import com.google.inject.Inject;
 import com.google.inject.Provider;
 import com.google.inject.Singleton;
 import com.google.inject.persist.Transactional;
 
import org.apache.ambari.server.orm.entities.PrincipalEntity;

 @Singleton
 public class GroupDAO {
   @Inject
@@ -65,6 +66,20 @@ public class GroupDAO {
     }
   }
 
  @RequiresSession
  public GroupEntity findGroupByNameAndType(String groupName, GroupType groupType) {
    // do case insensitive compare
    TypedQuery<GroupEntity> query = entityManagerProvider.get().createQuery(
        "SELECT group_entity FROM GroupEntity group_entity WHERE group_entity.groupType=:type AND lower(group_entity.groupName)=lower(:name)", GroupEntity.class);
    query.setParameter("type", groupType);
    query.setParameter("name", groupName);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

   /**
    * Find the group entities for the given list of principals
    *
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ResourceDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ResourceDAO.java
index e4ed9c6fd1..e57f265f24 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ResourceDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ResourceDAO.java
@@ -26,6 +26,7 @@ import org.apache.ambari.server.orm.RequiresSession;
 import org.apache.ambari.server.orm.entities.ResourceEntity;
 
 import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
 import javax.persistence.TypedQuery;
 import java.util.List;
 
@@ -54,6 +55,26 @@ public class ResourceDAO {
     return entityManagerProvider.get().find(ResourceEntity.class, id);
   }
 
  /**
   * Find a resource with the given resource type id.
   *
   * @param id  type id
   *
   * @return  a matching resource or null
   */
  @RequiresSession
  public ResourceEntity findByResourceTypeId(Integer id) {
    TypedQuery<ResourceEntity> query = entityManagerProvider.get().createQuery(
        "SELECT resource FROM ResourceEntity resource WHERE resource.resourceType.id =:resourceTypeId",
        ResourceEntity.class);
    query.setParameter("resourceTypeId", id);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

   /**
    * Find all resources.
    *
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java
index 00e233e63b..58b2e5d5ac 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/GroupEntity.java
@@ -19,9 +19,12 @@ package org.apache.ambari.server.orm.entities;
 
 import java.util.Set;
 
import javax.persistence.Basic;
 import javax.persistence.CascadeType;
 import javax.persistence.Column;
 import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.Id;
@@ -35,6 +38,8 @@ import javax.persistence.Table;
 import javax.persistence.TableGenerator;
 import javax.persistence.UniqueConstraint;
 
import org.apache.ambari.server.security.authorization.GroupType;

 @Entity
 @Table(name = "groups", uniqueConstraints = {@UniqueConstraint(columnNames = {"group_name", "ldap_group"})})
 @TableGenerator(name = "group_id_generator",
@@ -59,6 +64,11 @@ public class GroupEntity {
   @Column(name = "ldap_group")
   private Integer ldapGroup = 0;
 
  @Column(name = "group_type")
  @Enumerated(EnumType.STRING)
  @Basic
  private GroupType groupType = GroupType.LOCAL;

   @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
   private Set<MemberEntity> memberEntities;
 
@@ -99,6 +109,14 @@ public class GroupEntity {
     }
   }
 
  public GroupType getGroupType() {
    return groupType;
  }

  public void setgroupType(GroupType groupType) {
    this.groupType = groupType;
  }

   public Set<MemberEntity> getMemberEntities() {
     return memberEntities;
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/ClientSecurityType.java b/ambari-server/src/main/java/org/apache/ambari/server/security/ClientSecurityType.java
index 26d4da7f3e..fa853a6633 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/ClientSecurityType.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/ClientSecurityType.java
@@ -19,7 +19,8 @@ package org.apache.ambari.server.security;
 
 public enum ClientSecurityType {
   LOCAL("local"),
  LDAP("ldap");
  LDAP("ldap"),
  PAM("pam");
 
   private String value;
   ClientSecurityType(String value) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProvider.java
new file mode 100644
index 0000000000..ab66271e9c
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProvider.java
@@ -0,0 +1,252 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Inject;

/**
 * Provides PAM user authentication & authorization logic for Ambari Server
 */

public class AmbariPamAuthenticationProvider implements AuthenticationProvider {

  @Inject
  private Users users;
  @Inject
  protected UserDAO userDAO;
  @Inject
  protected GroupDAO groupDAO;

  private static Logger LOG = LoggerFactory.getLogger(AmbariPamAuthenticationProvider.class);

  private final Configuration configuration;

  @Inject
  public AmbariPamAuthenticationProvider(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Performs PAM Initialization
   *
   * @param authentication
   * @return authentication
   */

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
      if(isPamEnabled()){
        PAM pam;
        try{
          //Set PAM configuration file (found under /etc/pam.d)
          String pamConfig = configuration.getPamConfigurationFile();
          pam = new PAM(pamConfig);

        } catch(PAMException ex) {
          LOG.error("Unable to Initialize PAM." + ex.getMessage());
          throw new AuthenticationServiceException("Unable to Initialize PAM - ", ex);
        }

        return authenticateViaPam(pam, authentication);
    } else {
       return null;
    }
  }

  /**
   * Performs PAM Authentication
   *
   * @param pam
   * @param authentication
   * @return authentication
   */

  protected Authentication authenticateViaPam(PAM pam, Authentication authentication) throws AuthenticationException{
    if(isPamEnabled()){
      try {
          String userName = String.valueOf(authentication.getPrincipal());
          String passwd = String.valueOf(authentication.getCredentials());

          // authenticate using PAM
          UnixUser unixUser = pam.authenticate(userName,passwd);

          //Get all the groups that user belongs to
          //Change all group names to lower case.
          Set<String> groups = new HashSet<String>();

          for(String group: unixUser.getGroups()){
            groups.add(group.toLowerCase());
          }

          ambariPamAuthorization(userName,groups);

          Collection<AmbariGrantedAuthority> userAuthorities =
              users.getUserAuthorities(userName, UserType.PAM);

          final User user = users.getUser(userName, UserType.PAM);

          Principal principal = new Principal() {
            @Override
            public String getName() {
              return user.getUserName();
            }
          };

          UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(principal, null, userAuthorities);
          SecurityContextHolder.getContext().setAuthentication(token);
          return token;

        } catch (PAMException ex) {
          LOG.error("Unable to sign in. Invalid username/password combination - " + ex.getMessage());
          Throwable t = ex.getCause();
          throw new PamAuthenticationException("Unable to sign in. Invalid username/password combination.",t);

        } finally {
          pam.dispose();
        }

      }
      else {
        return null;
      }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Check if PAM authentication is enabled in server properties
   * @return true if enabled
   */
  private boolean isPamEnabled() {
    return configuration.getClientSecurityType() == ClientSecurityType.PAM;
  }

  /**
   * Check if PAM authentication is enabled in server properties
   * @return true if enabled
   */
  private boolean isAutoGroupCreationAllowed() {
    return configuration.getAutoGroupCreation().equals("true");
  }


  /**
   * Performs PAM authorization by creating user & group(s)
   *
   * @param userName user name
   * @param userGroups Collection of groups
   * @return
   */
  private void ambariPamAuthorization(String userName,Set<String> userGroups){
    try {
      User existingUser = users.getUser(userName,UserType.PAM);

      if (existingUser == null ) {
        users.createUser(userName, null, UserType.PAM, true, false);
      }

      UserEntity userEntity = userDAO.findUserByNameAndType(userName, UserType.PAM);

      if(isAutoGroupCreationAllowed()){
        for(String userGroup: userGroups){
          if(users.getGroupByNameAndType(userGroup, GroupType.PAM) == null){
            users.createGroup(userGroup, GroupType.PAM);
          }

          final GroupEntity groupEntity = groupDAO.findGroupByNameAndType(userGroup, GroupType.PAM);

          if (!isUserInGroup(userEntity, groupEntity)){
            users.addMemberToGroup(userGroup,userName);
          }
        }

        Set<String> ambariUserGroups = getUserGroups(userName, UserType.PAM);

        for(String group: ambariUserGroups){
          if(userGroups == null || !userGroups.contains(group)){
            users.removeMemberFromGroup(group, userName);
          }
        }
      }

    } catch (AmbariException e) {
      e.printStackTrace();
    }
  }

  /**
   * Performs a check if given user belongs to given group.
   *
   * @param userEntity user entity
   * @param groupEntity group entity
   * @return true if user presents in group
   */
  private boolean isUserInGroup(UserEntity userEntity, GroupEntity groupEntity) {
    for (MemberEntity memberEntity: userEntity.getMemberEntities()) {
      if (memberEntity.getGroup().equals(groupEntity)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Extracts all groups a user belongs to
   *
   * @param userName user name
   * @return Collection of group names
   */
  private Set<String> getUserGroups(String userName, UserType userType) {
    UserEntity userEntity = userDAO.findUserByNameAndType(userName, userType);
    Set<String> groups = new HashSet<String>();
    for (MemberEntity memberEntity: userEntity.getMemberEntities()) {
      groups.add(memberEntity.getGroup().getGroupName());
    }

    return groups;
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Group.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Group.java
index b20df8d886..715c41ccba 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Group.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Group.java
@@ -26,11 +26,13 @@ public class Group {
   private final int groupId;
   private final String groupName;
   private final boolean ldapGroup;
  private final GroupType groupType;
 
   Group(GroupEntity groupEntity) {
     this.groupId = groupEntity.getGroupId();
     this.groupName = groupEntity.getGroupName();
     this.ldapGroup = groupEntity.getLdapGroup();
    this.groupType = groupEntity.getGroupType();
   }
 
   public int getGroupId() {
@@ -45,6 +47,10 @@ public class Group {
     return ldapGroup;
   }
 
  public GroupType getGroupType() {
    return groupType;
  }

   @Override
   public String toString() {
     return "Group [groupId=" + groupId + ", groupName=" + groupName
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/GroupType.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/GroupType.java
new file mode 100644
index 0000000000..d427f3a497
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/GroupType.java
@@ -0,0 +1,25 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

public enum GroupType {
  LOCAL,
  LDAP,
  JWT,
  PAM
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/PamAuthenticationException.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/PamAuthenticationException.java
new file mode 100644
index 0000000000..6c09a67100
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/PamAuthenticationException.java
@@ -0,0 +1,36 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import org.springframework.security.core.AuthenticationException;

public class PamAuthenticationException extends AuthenticationException{

   public PamAuthenticationException() {
        this("The user authentication failed");
     }

  public PamAuthenticationException(String msg, Throwable t) {
    super(msg, t);
  }

  public PamAuthenticationException(String msg) {
    super(msg);
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserType.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserType.java
index aa9f3e0455..e60d58e196 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserType.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserType.java
@@ -20,5 +20,6 @@ package org.apache.ambari.server.security.authorization;
 public enum UserType {
   LOCAL,
   LDAP,
  JWT
  JWT,
  PAM
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
index 8ac7ebbd36..2cd538c9a1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
@@ -41,6 +41,7 @@ import org.apache.ambari.server.orm.dao.PrincipalDAO;
 import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
 import org.apache.ambari.server.orm.dao.PrivilegeDAO;
 import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
 import org.apache.ambari.server.orm.dao.UserDAO;
 import org.apache.ambari.server.orm.entities.GroupEntity;
 import org.apache.ambari.server.orm.entities.MemberEntity;
@@ -48,7 +49,10 @@ import org.apache.ambari.server.orm.entities.PermissionEntity;
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
@@ -88,6 +92,8 @@ public class Users {
   @Inject
   protected ResourceDAO resourceDAO;
   @Inject
  protected ResourceTypeDAO resourceTypeDAO;
  @Inject
   protected PrincipalTypeDAO principalTypeDAO;
   @Inject
   protected PasswordEncoder passwordEncoder;
@@ -127,6 +133,11 @@ public class Users {
     if (userEntity == null) {
       userEntity = userDAO.findUserByNameAndType(userName, UserType.JWT);
     }

    if (userEntity == null) {
        userEntity = userDAO.findUserByNameAndType(userName, UserType.PAM);
    }

     return (null == userEntity) ? null : new User(userEntity);
   }
 
@@ -368,6 +379,18 @@ public class Users {
     return (null == groupEntity) ? null : new Group(groupEntity);
   }
 
  /**
   * Gets group by given name & type.
   *
   * @param groupName group name
   * @param groupType group type
   * @return group
   */
  public Group getGroupByNameAndType(String groupName, GroupType groupType) {
    final GroupEntity groupEntity = groupDAO.findGroupByNameAndType(groupName, groupType);
    return (null == groupEntity) ? null : new Group(groupEntity);
  }

   /**
    * Gets group members.
    *
@@ -393,10 +416,10 @@ public class Users {
   }
 
   /**
   * Creates new local group with provided name
   * Creates new group with provided name & type
    */
   @Transactional
  public synchronized void createGroup(String groupName) {
  public synchronized void createGroup(String groupName, GroupType groupType) {
     // create an admin principal to represent this group
     PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findById(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE);
     if (principalTypeEntity == null) {
@@ -412,6 +435,7 @@ public class Users {
     final GroupEntity groupEntity = new GroupEntity();
     groupEntity.setGroupName(groupName);
     groupEntity.setPrincipal(principalEntity);
    groupEntity.setgroupType(groupType);
 
     groupDAO.create(groupEntity);
   }
@@ -479,6 +503,32 @@ public class Users {
     }
   }
 
  /**
   * Grants privilege to provided group.
   *
   * @param groupId group id
   * @param resourceId resource id
   * @param resourceType resource type
   * @param permissionName permission name
   */
  public synchronized void grantPrivilegeToGroup(Integer groupId, Long resourceId, ResourceType resourceType, String permissionName) {
    final GroupEntity group = groupDAO.findByPK(groupId);
    final PrivilegeEntity privilege = new PrivilegeEntity();
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(resourceType.getId());
    resourceTypeEntity.setName(resourceType.name());
    privilege.setPermission(permissionDAO.findPermissionByNameAndType(permissionName,resourceTypeEntity));
    privilege.setPrincipal(group.getPrincipal());
    privilege.setResource(resourceDAO.findById(resourceId));
    if (!group.getPrincipal().getPrivileges().contains(privilege)) {
      privilegeDAO.create(privilege);
      group.getPrincipal().getPrivileges().add(privilege);
      principalDAO.merge(group.getPrincipal()); //explicit merge for Derby support
      groupDAO.merge(group);
      privilegeDAO.merge(privilege);
    }
  }

   /**
    * Revokes AMBARI.ADMINISTRATOR privilege from provided user.
    *
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
index 3425dd7697..e81568cd9b 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
@@ -46,6 +46,8 @@ import com.google.inject.Injector;
 public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
 
   protected static final String HOST_VERSION_TABLE = "host_version";
  protected static final String GROUPS_TABLE = "groups";
  protected static final String GROUP_TYPE_COL = "group_type";
   private static final String AMS_ENV = "ams-env";
   private static final String KAFKA_BROKER = "kafka-broker";
   private static final String KAFKA_TIMELINE_METRICS_HOST = "kafka.timeline.metrics.host";
@@ -109,6 +111,7 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
   protected void executeDDLUpdates() throws AmbariException, SQLException {
     updateHostVersionTable();
     createComponentVersionTable();
    updateGroupsTable();
     dbAccessor.addColumn("stage",
       new DBAccessor.DBColumnInfo("command_execution_type", String.class, 32, CommandExecutionType.STAGE.toString(),
         false));
@@ -140,6 +143,14 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
     dbAccessor.addUniqueConstraint(HOST_VERSION_TABLE, "UQ_host_repo", "repo_version_id", "host_id");
   }
 
  protected void updateGroupsTable() throws SQLException {
    LOG.info("Updating the {} table", GROUPS_TABLE);

    dbAccessor.addColumn(GROUPS_TABLE, new DBColumnInfo(GROUP_TYPE_COL, String.class, null, "LOCAL", false));
    dbAccessor.executeQuery("UPDATE groups SET group_type='LDAP' WHERE ldap_group=1");
    dbAccessor.addUniqueConstraint(GROUPS_TABLE, "UNQ_groups_0", "group_name", "group_type");
  }

   protected void updateAMSConfigs() throws AmbariException {
     AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
     Clusters clusters = ambariManagementController.getClusters();
diff --git a/ambari-server/src/main/python/ambari-server.py b/ambari-server/src/main/python/ambari-server.py
index d6c6c10146..d43e0f2ea3 100755
-- a/ambari-server/src/main/python/ambari-server.py
++ b/ambari-server/src/main/python/ambari-server.py
@@ -52,8 +52,8 @@ from ambari_server.setupActions import BACKUP_ACTION, LDAP_SETUP_ACTION, LDAP_SY
   SETUP_ACTION, SETUP_SECURITY_ACTION,START_ACTION, STATUS_ACTION, STOP_ACTION, RESTART_ACTION, UPGRADE_ACTION, \
   UPGRADE_STACK_ACTION, SETUP_JCE_ACTION, SET_CURRENT_ACTION, START_ACTION, STATUS_ACTION, STOP_ACTION, UPGRADE_ACTION, \
   UPGRADE_STACK_ACTION, SETUP_JCE_ACTION, SET_CURRENT_ACTION, ENABLE_STACK_ACTION, SETUP_SSO_ACTION, \
  DB_CLEANUP_ACTION, INSTALL_MPACK_ACTION, UPGRADE_MPACK_ACTION
from ambari_server.setupSecurity import setup_ldap, sync_ldap, setup_master_key, setup_ambari_krb5_jaas
  DB_CLEANUP_ACTION, INSTALL_MPACK_ACTION, UPGRADE_MPACK_ACTION, PAM_SETUP_ACTION
from ambari_server.setupSecurity import setup_ldap, sync_ldap, setup_master_key, setup_ambari_krb5_jaas, setup_pam
 from ambari_server.userInput import get_validated_string_input
 
 from ambari_server_main import server_process_main
@@ -651,7 +651,8 @@ def create_user_action_map(args, options):
         SETUP_SSO_ACTION: UserActionRestart(setup_sso, options),
         DB_CLEANUP_ACTION: UserAction(db_cleanup, options),
         INSTALL_MPACK_ACTION: UserAction(install_mpack, options),
        UPGRADE_MPACK_ACTION: UserAction(upgrade_mpack, options)
        UPGRADE_MPACK_ACTION: UserAction(upgrade_mpack, options),
        PAM_SETUP_ACTION: UserAction(setup_pam)
       }
   return action_map
 
diff --git a/ambari-server/src/main/python/ambari_server/setupActions.py b/ambari-server/src/main/python/ambari_server/setupActions.py
index 697bc1d0b2..c87e0b2344 100644
-- a/ambari-server/src/main/python/ambari_server/setupActions.py
++ b/ambari-server/src/main/python/ambari_server/setupActions.py
@@ -46,3 +46,4 @@ ENABLE_STACK_ACTION = "enable-stack"
 DB_CLEANUP_ACTION = "db-cleanup"
 INSTALL_MPACK_ACTION = "install-mpack"
 UPGRADE_MPACK_ACTION = "upgrade-mpack"
PAM_SETUP_ACTION = "setup-pam"
diff --git a/ambari-server/src/main/python/ambari_server/setupSecurity.py b/ambari-server/src/main/python/ambari_server/setupSecurity.py
index ef27ced313..1508d27b38 100644
-- a/ambari-server/src/main/python/ambari_server/setupSecurity.py
++ b/ambari-server/src/main/python/ambari_server/setupSecurity.py
@@ -67,8 +67,12 @@ REGEX_ANYTHING = ".*"
 
 CLIENT_SECURITY_KEY = "client.security"
 
AUTO_GROUP_CREATION = "auto.group.creation"

 SERVER_API_LDAP_URL = 'ldap_sync_events'
 
PAM_CONFIG_FILE = 'pam.configuration'

 
 def read_master_key(isReset=False, options = None):
   passwordPattern = ".*"
@@ -271,12 +275,17 @@ def sync_ldap(options):
           'root-level privileges'
     raise FatalException(4, err)
 
  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY_KEY,"") == 'pam':
    err = "PAM is configured. Can not sync LDAP."
    raise FatalException(1, err)

   server_status, pid = is_server_runing()
   if not server_status:
     err = 'Ambari Server is not running.'
     raise FatalException(1, err)
 
  properties = get_ambari_properties()
   if properties == -1:
     raise FatalException(1, "Failed to read properties file.")
 
@@ -614,6 +623,11 @@ def setup_ldap(options):
     raise FatalException(4, err)
 
   properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY_KEY,"") == 'pam':
    err = "PAM is configured. Can not setup LDAP."
    raise FatalException(1, err)

   isSecure = get_is_secure(properties)
 
   ldap_property_list_reqd = init_ldap_properties_list_reqd(properties, options)
@@ -812,3 +826,40 @@ def ensure_can_start_under_current_user(ambari_user):
           "command as root, as sudo or as user \"{1}\"".format(current_user, ambari_user)
     raise FatalException(1, err)
   return current_user

class PamPropTemplate:
  def __init__(self, properties, i_prop_name, i_prop_val_pattern, i_prompt_regex, i_allow_empty_prompt, i_prop_name_default=None):
    self.prop_name = i_prop_name
    self.pam_prop_name = get_value_from_properties(properties, i_prop_name, i_prop_name_default)
    self.pam_prop_val_prompt = i_prop_val_pattern.format(get_prompt_default(self.pam_prop_name))
    self.prompt_regex = i_prompt_regex
    self.allow_empty_prompt = i_allow_empty_prompt

def setup_pam():
  if not is_root():
    err = 'Ambari-server setup-pam should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY_KEY,"") == 'ldap':
    err = "LDAP is configured. Can not setup PAM."
    raise FatalException(1, err)

  pam_property_value_map = {}
  pam_property_value_map[CLIENT_SECURITY_KEY] = 'pam'

  pamConfig = get_validated_string_input("Enter PAM configuration file: ", PAM_CONFIG_FILE, REGEX_ANYTHING,
                                         "Invalid characters in the input!", False, False)

  pam_property_value_map[PAM_CONFIG_FILE] = pamConfig

  if get_YN_input("Do you want to allow automatic group creation [y/n] (y)? ", True):
    pam_property_value_map[AUTO_GROUP_CREATION] = 'true'
  else:
    pam_property_value_map[AUTO_GROUP_CREATION] = 'false'

  update_properties_2(properties, pam_property_value_map)
  print 'Saving...done'
  return 0
diff --git a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
index 37a975709a..09042b50c2 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
@@ -295,6 +295,7 @@ CREATE TABLE groups (
   principal_id BIGINT NOT NULL,
   group_name VARCHAR(255) NOT NULL,
   ldap_group INTEGER NOT NULL DEFAULT 0,
  group_type VARCHAR(255) NOT NULL DEFAULT 'LOCAL',
   CONSTRAINT PK_groups PRIMARY KEY (group_id),
   CONSTRAINT FK_groups_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
   CONSTRAINT UNQ_groups_0 UNIQUE (group_name, ldap_group));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
index 15d6120459..e2c2dd5a89 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
@@ -286,6 +286,7 @@ CREATE TABLE groups (
   principal_id NUMBER(19) NOT NULL,
   group_name VARCHAR2(255) NOT NULL,
   ldap_group NUMBER(10) DEFAULT 0,
  group_type VARCHAR(255) DEFAULT 'LOCAL' NOT NULL,
   CONSTRAINT PK_groups PRIMARY KEY (group_id),
   CONSTRAINT FK_groups_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
   CONSTRAINT UNQ_groups_0 UNIQUE (group_name, ldap_group));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
index 5a82a52cc7..4e9a5350a6 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
@@ -295,6 +295,7 @@ CREATE TABLE groups (
   principal_id BIGINT NOT NULL,
   group_name VARCHAR(255) NOT NULL,
   ldap_group INTEGER NOT NULL DEFAULT 0,
  group_type VARCHAR(255) NOT NULL DEFAULT 'LOCAL',
   CONSTRAINT PK_groups PRIMARY KEY (group_id),
   UNIQUE (ldap_group, group_name),
   CONSTRAINT FK_groups_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
index 659e4dc18e..0ba7df6f2e 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
@@ -284,6 +284,7 @@ CREATE TABLE groups (
   principal_id NUMERIC(19) NOT NULL,
   group_name VARCHAR(255) NOT NULL,
   ldap_group INTEGER NOT NULL DEFAULT 0,
  group_type VARCHAR(255) NOT NULL DEFAULT 'LOCAL',
   CONSTRAINT PK_groups PRIMARY KEY (group_id),
   CONSTRAINT FK_groups_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
   CONSTRAINT UNQ_groups_0 UNIQUE (group_name, ldap_group));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
index e9a258aade..d8cad6fc22 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
@@ -299,6 +299,7 @@ CREATE TABLE groups (
   principal_id BIGINT NOT NULL,
   group_name VARCHAR(255) NOT NULL,
   ldap_group INTEGER NOT NULL DEFAULT 0,
  group_type VARCHAR(255) NOT NULL DEFAULT 'LOCAL',
   CONSTRAINT PK_groups PRIMARY KEY CLUSTERED (group_id),
   CONSTRAINT FK_groups_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id),
   CONSTRAINT UNQ_groups_0 UNIQUE (group_name, ldap_group));
diff --git a/ambari-server/src/main/resources/properties.json b/ambari-server/src/main/resources/properties.json
index 6bbb32319b..b7e0988b07 100644
-- a/ambari-server/src/main/resources/properties.json
++ b/ambari-server/src/main/resources/properties.json
@@ -183,6 +183,7 @@
     "Group":[
         "Groups/group_name",
         "Groups/ldap_group",
        "Groups/group_type",
         "_"
     ],
     "Member":[
diff --git a/ambari-server/src/main/resources/webapp/WEB-INF/spring-security.xml b/ambari-server/src/main/resources/webapp/WEB-INF/spring-security.xml
index 500c0bf829..9eca92090c 100644
-- a/ambari-server/src/main/resources/webapp/WEB-INF/spring-security.xml
++ b/ambari-server/src/main/resources/webapp/WEB-INF/spring-security.xml
@@ -32,6 +32,7 @@
 
   <authentication-manager alias="authenticationManager">
     <authentication-provider ref="ambariLocalAuthenticationProvider"/>
    <authentication-provider ref="ambariPamAuthenticationProvider"/>
     <authentication-provider ref="ambariLdapAuthenticationProvider"/>
     <authentication-provider ref="ambariInternalAuthenticationProvider"/>
     <authentication-provider ref="kerberosServiceAuthenticationProvider"/>
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProviderTest.java
new file mode 100644
index 0000000000..2a6c75488c
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariPamAuthenticationProviderTest.java
@@ -0,0 +1,97 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.security.ClientSecurityType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.UnixUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

public class AmbariPamAuthenticationProviderTest {

  private static Injector injector;

  @Inject
  private AmbariPamAuthenticationProvider authenticationProvider;
  @Inject
  Configuration configuration;

  @Before
  public void setUp() {
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule());
    injector.injectMembers(this);
    injector.getInstance(GuiceJpaInitializer.class);
    configuration.setClientSecurityType(ClientSecurityType.PAM);
    configuration.setProperty(Configuration.PAM_CONFIGURATION_FILE, "ambari-pam");
  }

  @After
  public void tearDown() throws Exception {
    injector.getInstance(PersistService.class).stop();
  }

  @Test(expected = AuthenticationException.class)
  public void testBadCredential() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken("notFound", "wrong");
    authenticationProvider.authenticate(authentication);
  }

  @Test
  public void testAuthenticate() throws Exception {
    PAM pam = createNiceMock(PAM.class);
    UnixUser unixUser = createNiceMock(UnixUser.class);
    expect(pam.authenticate(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class))).andReturn(unixUser).atLeastOnce();
    expect(unixUser.getGroups()).andReturn(new HashSet<String>(Arrays.asList("group"))).atLeastOnce();
    EasyMock.replay(unixUser);
    EasyMock.replay(pam);
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");
    Authentication result = authenticationProvider.authenticateViaPam(pam,authentication);
    assertEquals("allowedUser", result.getName());
  }

  @Test
  public void testDisabled() throws Exception {
    configuration.setClientSecurityType(ClientSecurityType.LOCAL);
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");
    Authentication auth = authenticationProvider.authenticate(authentication);
    Assert.assertTrue(auth == null);
  }
}
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
index f54ac5cae1..7d112fc1d1 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
@@ -285,7 +285,7 @@ public class TestUsers {
 
   @Test
   public void testSetGroupLdap() throws Exception {
    users.createGroup("group");
    users.createGroup("group", GroupType.LOCAL);
 
     users.setGroupLdap("group");
     Assert.assertNotNull(users.getGroup("group"));
@@ -302,8 +302,8 @@ public class TestUsers {
   public void testCreateGetRemoveGroup() throws Exception {
     final String groupName = "engineering1";
     final String groupName2 = "engineering2";
    users.createGroup(groupName);
    users.createGroup(groupName2);
    users.createGroup(groupName, GroupType.LOCAL);
    users.createGroup(groupName2, GroupType.LOCAL);
 
     final Group group = users.getGroup(groupName);
     assertNotNull(group);
@@ -328,8 +328,8 @@ public class TestUsers {
   public void testMembers() throws Exception {
     final String groupName = "engineering";
     final String groupName2 = "engineering2";
    users.createGroup(groupName);
    users.createGroup(groupName2);
    users.createGroup(groupName, GroupType.LOCAL);
    users.createGroup(groupName2, GroupType.LOCAL);
     users.createUser("user1", "user1");
     users.createUser("user2", "user2");
     users.createUser("user3", "user3");
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
index 8ed81dfd83..14fc20b8b6 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog250Test.java
@@ -101,6 +101,11 @@ public class UpgradeCatalog250Test {
 
     // !!! setup capture for host_version
     dbAccessor.addUniqueConstraint("host_version", "UQ_host_repo", "repo_version_id", "host_id");

    Capture<DBAccessor.DBColumnInfo> groupGroupType = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog250.GROUPS_TABLE), capture(groupGroupType));
    dbAccessor.addUniqueConstraint("groups", "UNQ_groups_0", "group_name", "group_type");

     expectLastCall().once();
 
     // !!! setup capture for servicecomponent_version
@@ -143,6 +148,14 @@ public class UpgradeCatalog250Test {
     UpgradeCatalog250 upgradeCatalog250 = injector.getInstance(UpgradeCatalog250.class);
     upgradeCatalog250.executeDDLUpdates();
 
    DBAccessor.DBColumnInfo capturedGroupTypeColumn = groupGroupType.getValue();
    Assert.assertNotNull(capturedGroupTypeColumn);
    Assert.assertEquals(UpgradeCatalog250.GROUP_TYPE_COL, capturedGroupTypeColumn.getName());
    Assert.assertEquals(String.class, capturedGroupTypeColumn.getType());
    Assert.assertEquals(null, capturedGroupTypeColumn.getLength());
    Assert.assertEquals("LOCAL", capturedGroupTypeColumn.getDefaultValue());
    Assert.assertEquals(false, capturedGroupTypeColumn.isNullable());

     verify(dbAccessor);
 
     // !!! check the captured for host_version
- 
2.19.1.windows.1

