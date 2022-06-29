From 2e14098c19afd5d22d879e5ea31eb0c793abbccf Mon Sep 17 00:00:00 2001
From: Myroslav Papirkovskyi <mpapyrkovskyy@hortonworks.com>
Date: Wed, 22 Jun 2016 18:43:40 +0300
Subject: [PATCH] AMBARI-17199. Scheduled requests works incorrectly for LDAP
 and Jwt users. (mpapirkovskyy)

--
 .../authorization/AmbariAuthentication.java   |  11 +-
 .../AmbariLdapAuthenticationProvider.java     |  31 +++-
 .../AmbariUserAuthentication.java             |   7 +-
 .../authorization/AuthorizationHelper.java    |   8 +-
 .../authorization/UserIdAuthentication.java   |  24 +++
 .../server/security/authorization/Users.java  |  23 +++
 .../authorization/jwt/JwtAuthentication.java  |  50 +------
 .../server/upgrade/UpgradeCatalog240.java     |  31 ++++
 .../AmbariAuthenticationTest.java             |  32 ++--
 ...henticationProviderForDNWithSpaceTest.java |   3 +
 ...nticationProviderForDuplicateUserTest.java |   6 +-
 .../AmbariLdapAuthenticationProviderTest.java |  20 ++-
 .../AuthorizationHelperTest.java              |   2 +-
 .../security/authorization/TestUsers.java     |  11 ++
 .../jwt/JwtAuthenticationFilterTest.java      |   9 +-
 .../server/upgrade/UpgradeCatalog240Test.java | 140 +++++++++++++++---
 16 files changed, 307 insertions(+), 101 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserIdAuthentication.java

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariAuthentication.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariAuthentication.java
index 9b1939fddb..7eed77d936 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariAuthentication.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariAuthentication.java
@@ -31,13 +31,15 @@ import org.springframework.security.core.userdetails.User;
  * provide functionality for resolving login aliases to
  * ambari user names.
  */
public final class AmbariAuthentication implements Authentication {
public final class AmbariAuthentication implements Authentication, UserIdAuthentication {
   private final Authentication authentication;
   private final Object principalOverride;
  private final Integer userId;
 
  public AmbariAuthentication(Authentication authentication) {
  public AmbariAuthentication(Authentication authentication, Integer userId) {
     this.authentication = authentication;
     this.principalOverride = getPrincipalOverride();
    this.userId = userId;
   }
 
 
@@ -219,4 +221,9 @@ public final class AmbariAuthentication implements Authentication {
 
     return principal;
   }

  @Override
  public Integer getUserId() {
    return userId;
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
index da47407d63..0bf7ec269f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
@@ -21,12 +21,16 @@ import com.google.inject.Inject;
 import java.util.List;
 
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
 import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.dao.IncorrectResultSizeDataAccessException;
 import org.springframework.ldap.core.support.LdapContextSource;
 import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
 import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.AuthenticationException;
@@ -44,15 +48,18 @@ public class AmbariLdapAuthenticationProvider implements AuthenticationProvider
   Configuration configuration;
 
   private AmbariLdapAuthoritiesPopulator authoritiesPopulator;
  private UserDAO userDAO;
 
   private ThreadLocal<LdapServerProperties> ldapServerProperties = new ThreadLocal<LdapServerProperties>();
   private ThreadLocal<LdapAuthenticationProvider> providerThreadLocal = new ThreadLocal<LdapAuthenticationProvider>();
   private ThreadLocal<String> ldapUserSearchFilterThreadLocal = new ThreadLocal<>();
 
   @Inject
  public AmbariLdapAuthenticationProvider(Configuration configuration, AmbariLdapAuthoritiesPopulator authoritiesPopulator) {
  public AmbariLdapAuthenticationProvider(Configuration configuration,
                                          AmbariLdapAuthoritiesPopulator authoritiesPopulator, UserDAO userDAO) {
     this.configuration = configuration;
     this.authoritiesPopulator = authoritiesPopulator;
    this.userDAO = userDAO;
   }
 
   @Override
@@ -62,8 +69,9 @@ public class AmbariLdapAuthenticationProvider implements AuthenticationProvider
 
       try {
         Authentication auth = loadLdapAuthenticationProvider(username).authenticate(authentication);
        Integer userId = getUserId(auth);
 
        return new AmbariAuthentication(auth);
        return new AmbariAuthentication(auth, userId);
       } catch (AuthenticationException e) {
         LOG.debug("Got exception during LDAP authentification attempt", e);
         // Try to help in troubleshooting
@@ -182,4 +190,23 @@ public class AmbariLdapAuthenticationProvider implements AuthenticationProvider
       .getUserSearchFilter(configuration.isLdapAlternateUserSearchEnabled() && AmbariLdapUtils.isUserPrincipalNameFormat(userName));
   }
 
  private Integer getUserId(Authentication authentication) {
    String userName = authentication.getName();

    UserEntity userEntity = userDAO.findLdapUserByName(userName);

    if (userEntity == null || !StringUtils.equals(userEntity.getUserName(), userName)) {
      LOG.info("user not found ");
      throw new UsernameNotFoundException("Username " + userName + " not found");
    }

    if (!userEntity.getActive()) {
      LOG.debug("User account is disabled");

      throw new DisabledException("Username " + userName + " is disabled");
    }

    return userEntity.getUserId();
  }

 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariUserAuthentication.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariUserAuthentication.java
index f9c5cf4886..ae764e51db 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariUserAuthentication.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariUserAuthentication.java
@@ -21,7 +21,7 @@ import org.springframework.security.core.Authentication;
 
 import java.util.Collection;
 
public class AmbariUserAuthentication implements Authentication {
public class AmbariUserAuthentication implements Authentication, UserIdAuthentication {
 
   private String serializedToken;
   private User user;
@@ -68,4 +68,9 @@ public class AmbariUserAuthentication implements Authentication {
   public String getName() {
     return user.getUserName();
   }

  @Override
  public Integer getUserId() {
    return user.getUserId();
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
index 8befc3f71a..8639a2f2b8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AuthorizationHelper.java
@@ -107,14 +107,14 @@ public class AuthorizationHelper {
     SecurityContext securityContext = SecurityContextHolder.getContext();
 
     Authentication authentication = securityContext.getAuthentication();
    AmbariUserAuthentication auth;
    if (authentication instanceof AmbariUserAuthentication) {
      auth = (AmbariUserAuthentication) authentication;
    UserIdAuthentication auth;
    if (authentication instanceof UserIdAuthentication) {
      auth = (UserIdAuthentication) authentication;
     } else {
       return -1;
     }
 
    return auth.getPrincipal().getUserId();
    return auth.getUserId();
   }
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserIdAuthentication.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserIdAuthentication.java
new file mode 100644
index 0000000000..f2a9daf2a4
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/UserIdAuthentication.java
@@ -0,0 +1,24 @@
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

public interface UserIdAuthentication {

  Integer getUserId();
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
index f1abb90692..1a7b58db37 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
@@ -123,6 +123,29 @@ public class Users {
     return (null == userEntity) ? null : new User(userEntity);
   }
 
  /**
   * Retrieves User then userName is unique in users DB. Will return null if there no user with provided userName or
   * there are some users with provided userName but with different types.
   * @param userName
   * @return User if userName is unique in DB, null otherwise
   */
  public User getUserIfUnique(String userName) {
    List<UserEntity> userEntities = new ArrayList<>();
    UserEntity userEntity = userDAO.findUserByNameAndType(userName, UserType.LOCAL);
    if (userEntity != null) {
      userEntities.add(userEntity);
    }
    userEntity = userDAO.findUserByNameAndType(userName, UserType.LDAP);
    if (userEntity != null) {
      userEntities.add(userEntity);
    }
    userEntity = userDAO.findUserByNameAndType(userName, UserType.JWT);
    if (userEntity != null) {
      userEntities.add(userEntity);
    }
    return (userEntities.isEmpty() || userEntities.size() > 1) ? null : new User(userEntities.get(0));
  }

   /**
    * Modifies password of local user
    * @throws AmbariException
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthentication.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthentication.java
index 1b7442c65f..3088b09716 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthentication.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthentication.java
@@ -17,62 +17,18 @@
  */
 package org.apache.ambari.server.security.authorization.jwt;
 
import com.nimbusds.jwt.SignedJWT;
 import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.AmbariUserAuthentication;
 import org.apache.ambari.server.security.authorization.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
 
 import java.util.Collection;
 
 /**
  * Internal token which describes JWT authentication
  */
public class JwtAuthentication implements Authentication {

  private String serializedToken;
  private User user;
  private Collection<AmbariGrantedAuthority> userAuthorities;
  private boolean authenticated = false;
public class JwtAuthentication extends AmbariUserAuthentication {
 
   public JwtAuthentication(String token, User user, Collection<AmbariGrantedAuthority> userAuthorities) {
    this.serializedToken = token;
    this.user = user;
    this.userAuthorities = userAuthorities;
  }

  @Override
  public Collection<? extends AmbariGrantedAuthority> getAuthorities() {
    return userAuthorities;
  }

  @Override
  public String getCredentials() {
    return serializedToken;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public User getPrincipal() {
    return user;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
    this.authenticated = authenticated;
  }

  @Override
  public String getName() {
    return user.getUserName();
    super(token, user, userAuthorities);
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog240.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog240.java
index 6ecfa717dc..b0d52114ea 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog240.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog240.java
@@ -54,6 +54,7 @@ import org.apache.ambari.server.orm.dao.PrincipalDAO;
 import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
 import org.apache.ambari.server.orm.dao.PrivilegeDAO;
 import org.apache.ambari.server.orm.dao.RemoteAmbariClusterDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
 import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
 import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
 import org.apache.ambari.server.orm.dao.UserDAO;
@@ -67,6 +68,7 @@ import org.apache.ambari.server.orm.entities.PrincipalEntity;
 import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
 import org.apache.ambari.server.orm.entities.PrivilegeEntity;
 import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
 import org.apache.ambari.server.orm.entities.ResourceEntity;
 import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
 import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
@@ -75,6 +77,8 @@ import org.apache.ambari.server.orm.entities.ViewEntityEntity;
 import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
 import org.apache.ambari.server.orm.entities.WidgetEntity;
 import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
 import org.apache.ambari.server.state.AlertFirmness;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
@@ -223,6 +227,12 @@ public class UpgradeCatalog240 extends AbstractUpgradeCatalog {
   @Inject
   PrincipalDAO principalDAO;
 
  @Inject
  RequestScheduleDAO requestScheduleDAO;

  @Inject
  Users users;

   /**
    * Logger.
    */
@@ -398,6 +408,27 @@ public class UpgradeCatalog240 extends AbstractUpgradeCatalog {
     removeAuthorizations();
     addConnectionTimeoutParamForWebAndMetricAlerts();
     addSliderClientConfig();
    updateRequestScheduleEntityUserIds();
  }

  /**
   * Populates authenticated_user_id field by correct user id calculated from user name
   * @throws SQLException
   */
  protected void updateRequestScheduleEntityUserIds() throws SQLException {
    List<RequestScheduleEntity> requestScheduleEntities = requestScheduleDAO.findAll();
    for (RequestScheduleEntity requestScheduleEntity : requestScheduleEntities) {
      String createdUserName = requestScheduleEntity.getCreateUser();

      if (createdUserName != null) {
        User user = users.getUserIfUnique(createdUserName);

        if (user != null && StringUtils.equals(user.getUserName(), createdUserName)) {
          requestScheduleEntity.setAuthenticatedUserId(user.getUserId());
          requestScheduleDAO.merge(requestScheduleEntity);
        }
      }
    }
   }
 
   protected void updateClusterInheritedPermissionsConfig() throws SQLException {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariAuthenticationTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariAuthenticationTest.java
index 19656b1329..d8c6be8fcd 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariAuthenticationTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariAuthenticationTest.java
@@ -48,6 +48,8 @@ import static org.easymock.EasyMock.verify;
 
 public class AmbariAuthenticationTest extends EasyMockSupport {
 
  private final Integer DEFAULT_USER_ID = 0;

   @Rule
   public EasyMockRule mocks = new EasyMockRule(this);
 
@@ -76,7 +78,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
     };
 
     Authentication authentication = new TestingAuthenticationToken(origPrincipal, "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     Object principal = ambariAuthentication.getPrincipal();
@@ -90,7 +92,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
   public void testGetPrincipal() throws Exception {
     // Given
     Authentication authentication = new TestingAuthenticationToken("user", "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     Object principal = ambariAuthentication.getPrincipal();
@@ -108,7 +110,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
 
     replayAll();
 
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     verifyAll();
@@ -124,7 +126,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
     UserDetails userDetails = new User("user", "password", Collections.<GrantedAuthority>emptyList());
     Authentication authentication = new TestingAuthenticationToken(userDetails, userDetails.getPassword());
 
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     Object principal = ambariAuthentication.getPrincipal();
@@ -144,7 +146,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
 
     replayAll();
 
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     Object principal = ambariAuthentication.getPrincipal();
@@ -168,7 +170,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
       }
     };
     Authentication authentication = new TestingAuthenticationToken(origPrincipal, "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     String name = ambariAuthentication.getName();
@@ -181,7 +183,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
   public void testGetName() throws Exception {
     // Given
     Authentication authentication = new TestingAuthenticationToken("user", "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     String name = ambariAuthentication.getName();
@@ -199,7 +201,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
 
     replayAll();
 
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     String name = ambariAuthentication.getName();
@@ -215,7 +217,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
     UserDetails userDetails = new User("user", "password", Collections.<GrantedAuthority>emptyList());
     Authentication authentication = new TestingAuthenticationToken(userDetails, userDetails.getPassword());
 
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     String name = ambariAuthentication.getName();
@@ -235,7 +237,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
 
     replayAll();
 
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     String name = ambariAuthentication.getName();
@@ -249,7 +251,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
   public void testGetAuthorities() throws Exception {
     // Given
     Authentication authentication = new TestingAuthenticationToken("user", "password", "test_role");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     Collection<?>  grantedAuthorities =  ambariAuthentication.getAuthorities();
@@ -265,7 +267,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
     // Given
     String passord = "password";
     Authentication authentication = new TestingAuthenticationToken("user", passord);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     Object credentials = ambariAuthentication.getCredentials();
@@ -279,7 +281,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
     // Given
     TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password");
     authentication.setDetails("test auth details");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);
 
     // When
     Object authDetails = ambariAuthentication.getDetails();
@@ -297,7 +299,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
 
     replayAll();
 
    Authentication ambariAuthentication = new AmbariAuthentication(testAuthentication);
    Authentication ambariAuthentication = new AmbariAuthentication(testAuthentication, DEFAULT_USER_ID);
 
     // When
     ambariAuthentication.isAuthenticated();
@@ -314,7 +316,7 @@ public class AmbariAuthenticationTest extends EasyMockSupport {
 
     replayAll();
 
    Authentication ambariAuthentication = new AmbariAuthentication(testAuthentication);
    Authentication ambariAuthentication = new AmbariAuthentication(testAuthentication, DEFAULT_USER_ID);
 
     // When
     ambariAuthentication.setAuthenticated(true);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java
index 65347057c4..ece3dab536 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java
@@ -72,6 +72,8 @@ public class AmbariLdapAuthenticationProviderForDNWithSpaceTest extends AmbariLd
   @Inject
   private UserDAO userDAO;
   @Inject
  private Users users;
  @Inject
   Configuration configuration;
 
   @Before
@@ -96,6 +98,7 @@ public class AmbariLdapAuthenticationProviderForDNWithSpaceTest extends AmbariLd
   @Test
   public void testAuthenticate() throws Exception {
     assertNull("User alread exists in DB", userDAO.findLdapUserByName("the allowedUser"));
    users.createUser("the allowedUser", "password", UserType.LDAP, true, false);
     Authentication authentication = new UsernamePasswordAuthenticationToken("the allowedUser", "password");
     Authentication result = authenticationProvider.authenticate(authentication);
     assertTrue(result.isAuthenticated());
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDuplicateUserTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDuplicateUserTest.java
index 43f860e8f0..02e4021abb 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDuplicateUserTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDuplicateUserTest.java
@@ -20,6 +20,7 @@ package org.apache.ambari.server.security.authorization;
 import java.util.Properties;
 
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.UserDAO;
 import org.apache.directory.server.annotations.CreateLdapServer;
 import org.apache.directory.server.annotations.CreateTransport;
 import org.apache.directory.server.core.annotations.ApplyLdifFiles;
@@ -71,6 +72,9 @@ public class AmbariLdapAuthenticationProviderForDuplicateUserTest extends Ambari
   @Mock(type = MockType.NICE)
   private AmbariLdapAuthoritiesPopulator authoritiesPopulator;
 
  @Mock(type = MockType.NICE)
  private UserDAO userDAO;

   private AmbariLdapAuthenticationProvider authenticationProvider;
 
   @Before
@@ -86,7 +90,7 @@ public class AmbariLdapAuthenticationProviderForDuplicateUserTest extends Ambari
 
     Configuration configuration = new Configuration(properties);
 
    authenticationProvider = new AmbariLdapAuthenticationProvider(configuration, authoritiesPopulator);
    authenticationProvider = new AmbariLdapAuthenticationProvider(configuration, authoritiesPopulator, userDAO);
   }
 
   @Test
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java
index 6d4ec609fe..9392910095 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java
@@ -28,6 +28,7 @@ import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
 import org.apache.ambari.server.security.ClientSecurityType;
 import org.apache.directory.server.annotations.CreateLdapServer;
 import org.apache.directory.server.annotations.CreateTransport;
@@ -86,6 +87,8 @@ public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticati
   @Inject
   private UserDAO userDAO;
   @Inject
  private Users users;
  @Inject
   Configuration configuration;
 
   @Before
@@ -115,7 +118,7 @@ public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticati
     AmbariLdapAuthenticationProvider provider = createMockBuilder(AmbariLdapAuthenticationProvider.class)
             .addMockedMethod("loadLdapAuthenticationProvider")
             .addMockedMethod("isLdapEnabled")
            .withConstructor(configuration, authoritiesPopulator).createMock();
            .withConstructor(configuration, authoritiesPopulator, userDAO).createMock();
     // Create the last thrown exception
     org.springframework.security.core.AuthenticationException exception =
             createNiceMock(org.springframework.security.core.AuthenticationException.class);
@@ -151,7 +154,7 @@ public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticati
     AmbariLdapAuthenticationProvider provider = createMockBuilder(AmbariLdapAuthenticationProvider.class)
             .addMockedMethod("loadLdapAuthenticationProvider")
             .addMockedMethod("isLdapEnabled")
            .withConstructor(configuration, authoritiesPopulator).createMock();
            .withConstructor(configuration, authoritiesPopulator, userDAO).createMock();
     // Create the cause
     org.springframework.ldap.AuthenticationException cause =
             createNiceMock(org.springframework.ldap.AuthenticationException.class);
@@ -181,11 +184,17 @@ public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticati
   @Test
   public void testAuthenticate() throws Exception {
     assertNull("User alread exists in DB", userDAO.findLdapUserByName("allowedUser"));
    users.createUser("allowedUser", "password", UserType.LDAP, true, false);
    UserEntity ldapUser = userDAO.findLdapUserByName("allowedUser");
     Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");
    Authentication result = authenticationProvider.authenticate(authentication);
    
    AmbariAuthentication result = (AmbariAuthentication) authenticationProvider.authenticate(authentication);
     assertTrue(result.isAuthenticated());
    result = authenticationProvider.authenticate(authentication);
    assertEquals(ldapUser.getUserId(), result.getUserId());

    result = (AmbariAuthentication) authenticationProvider.authenticate(authentication);
     assertTrue(result.isAuthenticated());
    assertEquals(ldapUser.getUserId(), result.getUserId());
   }
 
   @Test
@@ -199,7 +208,8 @@ public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticati
   @Test
   public void testAuthenticateLoginAlias() throws Exception {
     // Given
    assertNull("User already exists in DB", userDAO.findLdapUserByName("allowedUser"));
    assertNull("User already exists in DB", userDAO.findLdapUserByName("allowedUser@ambari.apache.org"));
    users.createUser("allowedUser@ambari.apache.org", "password", UserType.LDAP, true, false);
     Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser@ambari.apache.org", "password");
     configuration.setProperty(Configuration.LDAP_ALT_USER_SEARCH_ENABLED_KEY, "true");
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java
index 56f224c242..8409a6b372 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AuthorizationHelperTest.java
@@ -185,7 +185,7 @@ public class AuthorizationHelperTest  extends EasyMockSupport {
     replay(servletRequestAttributes);
 
     Authentication auth = new UsernamePasswordAuthenticationToken("user1@domain.com", null);
    SecurityContextHolder.getContext().setAuthentication(new AmbariAuthentication(auth));
    SecurityContextHolder.getContext().setAuthentication(new AmbariAuthentication(auth, 0));
 
     String user = AuthorizationHelper.getAuthenticatedName();
     Assert.assertEquals("user1", user);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
index bcff6b4ea1..44fb73c264 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
@@ -495,4 +495,15 @@ public class TestUsers {
     Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin3")));
   }
 
  @Test
  public void testGetUserIfUnique() throws Exception {
    users.createUser("admin", "admin", UserType.LOCAL, true, false);

    Assert.assertNotNull(users.getUserIfUnique("admin"));

    users.createUser("admin", "admin", UserType.LDAP, true, false);

    Assert.assertNull(users.getUserIfUnique("admin"));
  }

 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthenticationFilterTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthenticationFilterTest.java
index a2730c4223..71bbf110c8 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthenticationFilterTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/jwt/JwtAuthenticationFilterTest.java
@@ -25,6 +25,7 @@ import com.nimbusds.jose.crypto.RSASSASigner;
 import com.nimbusds.jwt.JWTClaimsSet;
 import com.nimbusds.jwt.SignedJWT;
 import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
 import org.apache.ambari.server.security.authorization.User;
 import org.apache.ambari.server.security.authorization.UserType;
 import org.apache.ambari.server.security.authorization.Users;
@@ -178,16 +179,18 @@ public class JwtAuthenticationFilterTest {
     expect(user.getUserName()).andReturn("test-user");
     expect(user.getUserType()).andReturn(UserType.JWT);
 
    expect(user.getUserId()).andReturn(1);

     replay(users, request, response, chain, filter, entryPoint, user, authority);
 
     filter.doFilter(request, response, chain);
 
    verify(users, request, response, chain, filter, entryPoint, user, authority);

     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertEquals(1L, AuthorizationHelper.getAuthenticatedId());
 
    assertEquals(true, authentication.isAuthenticated());
    verify(users, request, response, chain, filter, entryPoint, user, authority);
 
    assertEquals(true, authentication.isAuthenticated());
   }
 
   @Test
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
index dfb87b1cd9..d2bb499945 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
@@ -19,6 +19,27 @@
 package org.apache.ambari.server.upgrade;
 
 
import javax.persistence.EntityManager;
import junit.framework.Assert;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

 import java.io.File;
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
@@ -52,6 +73,7 @@ import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
 import org.apache.ambari.server.orm.dao.ClusterDAO;
 import org.apache.ambari.server.orm.dao.PrivilegeDAO;
 import org.apache.ambari.server.orm.dao.RemoteAmbariClusterDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
 import org.apache.ambari.server.orm.dao.StackDAO;
 import org.apache.ambari.server.orm.dao.UserDAO;
 import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
@@ -62,12 +84,15 @@ import org.apache.ambari.server.orm.entities.PermissionEntity;
 import org.apache.ambari.server.orm.entities.PrincipalEntity;
 import org.apache.ambari.server.orm.entities.PrivilegeEntity;
 import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
 import org.apache.ambari.server.orm.entities.ResourceEntity;
 import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
 import org.apache.ambari.server.orm.entities.UserEntity;
 import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
 import org.apache.ambari.server.orm.entities.WidgetEntity;
 import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
 import org.apache.ambari.server.stack.StackManagerFactory;
 import org.apache.ambari.server.state.AlertFirmness;
 import org.apache.ambari.server.state.Cluster;
@@ -108,25 +133,7 @@ import com.google.inject.Injector;
 import com.google.inject.Module;
 import com.google.inject.Provider;
 
import junit.framework.Assert;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.springframework.security.crypto.password.PasswordEncoder;
 
 public class UpgradeCatalog240Test {
   private static final String CAPACITY_SCHEDULER_CONFIG_TYPE = "capacity-scheduler";
@@ -192,7 +199,7 @@ public class UpgradeCatalog240Test {
     expect(dbAccessor.getConnection()).andReturn(connection);
     dbAccessor.createTable(eq("extensionlink"), capture(capturedExtensionLinkColumns), eq("link_id"));
     dbAccessor.addUniqueConstraint("extensionlink", "UQ_extension_link", "stack_id", "extension_id");
    dbAccessor.addFKConstraint("extensionlink", "FK_extensionlink_extension_id", "extension_id", "extension", 
    dbAccessor.addFKConstraint("extensionlink", "FK_extensionlink_extension_id", "extension_id", "extension",
                                "extension_id", false);
     dbAccessor.addFKConstraint("extensionlink", "FK_extensionlink_stack_id", "stack_id", "stack",
                                "stack_id", false);
@@ -328,6 +335,7 @@ public class UpgradeCatalog240Test {
         binder.bind(DBAccessor.class).toInstance(dbAccessor);
         binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
         binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
       };
 
@@ -566,6 +574,7 @@ public class UpgradeCatalog240Test {
     Method removeAuthorizations = UpgradeCatalog240.class.getDeclaredMethod("removeAuthorizations");
     Method addConnectionTimeoutParamForWebAndMetricAlerts = AbstractUpgradeCatalog.class.getDeclaredMethod("addConnectionTimeoutParamForWebAndMetricAlerts");
     Method addSliderClientConfig = UpgradeCatalog240.class.getDeclaredMethod("addSliderClientConfig");
    Method updateRequestScheduleEntityUserIds = UpgradeCatalog240.class.getDeclaredMethod("updateRequestScheduleEntityUserIds");
 
     Capture<String> capturedStatements = newCapture(CaptureType.ALL);
 
@@ -608,6 +617,7 @@ public class UpgradeCatalog240Test {
             .addMockedMethod(addConnectionTimeoutParamForWebAndMetricAlerts)
             .addMockedMethod(updateHBaseConfigs)
             .addMockedMethod(addSliderClientConfig)
            .addMockedMethod(updateRequestScheduleEntityUserIds)
             .createMock();
 
     Field field = AbstractUpgradeCatalog.class.getDeclaredField("dbAccessor");
@@ -645,6 +655,7 @@ public class UpgradeCatalog240Test {
     upgradeCatalog240.addConnectionTimeoutParamForWebAndMetricAlerts();
     upgradeCatalog240.updateHBaseConfigs();
     upgradeCatalog240.addSliderClientConfig();
    upgradeCatalog240.updateRequestScheduleEntityUserIds();
 
     replay(upgradeCatalog240, dbAccessor);
 
@@ -698,6 +709,7 @@ public class UpgradeCatalog240Test {
         binder.bind(EntityManager.class).toInstance(entityManager);
         binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
         binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -753,6 +765,7 @@ public class UpgradeCatalog240Test {
         binder.bind(EntityManager.class).toInstance(entityManager);
         binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
         binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -1447,6 +1460,7 @@ public class UpgradeCatalog240Test {
         bind(DBAccessor.class).toInstance(dbAccessor);
         bind(OsFamily.class).toInstance(osFamily);
         bind(EntityManager.class).toInstance(entityManager);
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -1593,6 +1607,7 @@ public class UpgradeCatalog240Test {
         bind(DBAccessor.class).toInstance(dbAccessor);
         bind(OsFamily.class).toInstance(osFamily);
         bind(EntityManager.class).toInstance(entityManager);
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -1647,6 +1662,7 @@ public class UpgradeCatalog240Test {
         bind(AlertDefinitionDAO.class).toInstance(mockAlertDefinitionDAO);
         bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
         bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -1854,6 +1870,7 @@ public class UpgradeCatalog240Test {
         bind(ClusterDAO.class).toInstance(clusterDAO);
         bind(DBAccessor.class).toInstance(ems.createNiceMock(DBAccessor.class));
         bind(OsFamily.class).toInstance(ems.createNiceMock(OsFamily.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -1911,6 +1928,7 @@ public class UpgradeCatalog240Test {
         bind(WidgetDAO.class).toInstance(widgetDAO);
         bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
         bind(AmbariMetaInfo.class).toInstance(metaInfo);
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
     expect(controller.getClusters()).andReturn(clusters).anyTimes();
@@ -1967,6 +1985,7 @@ public class UpgradeCatalog240Test {
         bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
         bind(RemoteAmbariClusterDAO.class).toInstance(clusterDAO);
         bind(ViewInstanceDAO.class).toInstance(instanceDAO);
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -2117,6 +2136,7 @@ public class UpgradeCatalog240Test {
         bind(AlertDefinitionDAO.class).toInstance(mockAlertDefinitionDAO);
         bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
         bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
       }
     });
 
@@ -2239,6 +2259,7 @@ public class UpgradeCatalog240Test {
       @Override
       protected void configure() {
         bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(PasswordEncoder.class).toInstance(createMock(PasswordEncoder.class));
         bind(Clusters.class).toInstance(mockClusters);
         bind(EntityManager.class).toInstance(entityManager);
         bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
@@ -2267,5 +2288,84 @@ public class UpgradeCatalog240Test {
   }
 
 

  @Test
  public void testUpdateRequestScheduleEntityUserIds() throws Exception{
    final RequestScheduleDAO requestScheduleDAO = createMock(RequestScheduleDAO.class);
    final Users users = createMock(Users.class);

    RequestScheduleEntity requestScheduleEntity = new RequestScheduleEntity();
    requestScheduleEntity.setCreateUser("createdUser");
    requestScheduleEntity.setClusterId(1L);

    expect(requestScheduleDAO.findAll()).andReturn(Collections.singletonList(requestScheduleEntity)).once();

    UserEntity userEntity = new UserEntity();
    userEntity.setUserName("createdUser");
    userEntity.setUserId(1);
    userEntity.setPrincipal(new PrincipalEntity());
    User user = new User(userEntity);
    expect(users.getUserIfUnique("createdUser")).andReturn(user).once();

    expect(requestScheduleDAO.merge(requestScheduleEntity)).andReturn(requestScheduleEntity).once();

    final Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(RequestScheduleDAO.class).toInstance(requestScheduleDAO);
        bind(Users.class).toInstance(users);
        bind(PasswordEncoder.class).toInstance(createMock(PasswordEncoder.class));
        bind(DBAccessor.class).toInstance(createMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(EntityManager.class).toInstance(entityManager);
      }
    });

    UpgradeCatalog240 upgradeCatalog240 = new UpgradeCatalog240(injector);

    replay(requestScheduleDAO, users);

    upgradeCatalog240.updateRequestScheduleEntityUserIds();

    verify(requestScheduleDAO, users);

    assertEquals(Integer.valueOf(1), requestScheduleEntity.getAuthenticatedUserId());
  }

  @Test
  public void testUpdateRequestScheduleEntityWithUnuniqueUser() throws Exception{
    final RequestScheduleDAO requestScheduleDAO = createMock(RequestScheduleDAO.class);
    final Users users = createMock(Users.class);

    RequestScheduleEntity requestScheduleEntity = new RequestScheduleEntity();
    requestScheduleEntity.setCreateUser("createdUser");
    requestScheduleEntity.setClusterId(1L);

    expect(requestScheduleDAO.findAll()).andReturn(Collections.singletonList(requestScheduleEntity)).once();

    expect(users.getUserIfUnique("createdUser")).andReturn(null).once();

    final Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(RequestScheduleDAO.class).toInstance(requestScheduleDAO);
        bind(Users.class).toInstance(users);
        bind(PasswordEncoder.class).toInstance(createMock(PasswordEncoder.class));
        bind(DBAccessor.class).toInstance(createMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(EntityManager.class).toInstance(entityManager);
      }
    });

    UpgradeCatalog240 upgradeCatalog240 = new UpgradeCatalog240(injector);

    replay(requestScheduleDAO, users);

    upgradeCatalog240.updateRequestScheduleEntityUserIds();

    verify(requestScheduleDAO, users);

    assertEquals(null, requestScheduleEntity.getAuthenticatedUserId());
  }
 }
 
- 
2.19.1.windows.1

