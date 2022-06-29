From 0cbc5fa385412dd7438f794a49c4ae584be54582 Mon Sep 17 00:00:00 2001
From: Nahappan Somasundaram <nsomasundaram@hortonworks.com>
Date: Wed, 22 Jun 2016 16:28:31 -0700
Subject: [PATCH] AMBARI-17383: User names should be case insensitive

--
 .../AmbariManagementControllerImpl.java       |  3 +-
 .../apache/ambari/server/orm/dao/UserDAO.java |  4 +-
 .../AmbariLdapAuthenticationProvider.java     |  3 +-
 .../AmbariLocalUserProvider.java              |  2 +-
 .../server/security/authorization/User.java   |  4 +-
 .../server/security/authorization/Users.java  | 57 ++++++-------------
 .../internal/UserResourceProviderTest.java    |  2 +-
 .../security/SecurityHelperImplTest.java      |  5 +-
 .../AmbariUserAuthenticationFilterTest.java   |  3 +-
 .../security/authorization/TestUsers.java     | 40 +++++++++----
 .../server/upgrade/UpgradeCatalog240Test.java |  6 +-
 11 files changed, 64 insertions(+), 65 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
index 9f82a90811..fe7e757ae8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariManagementControllerImpl.java
@@ -128,6 +128,7 @@ import org.apache.ambari.server.security.authorization.Group;
 import org.apache.ambari.server.security.authorization.ResourceType;
 import org.apache.ambari.server.security.authorization.RoleAuthorization;
 import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserType;
 import org.apache.ambari.server.security.authorization.Users;
 import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
 import org.apache.ambari.server.security.encryption.CredentialStoreService;
@@ -917,7 +918,7 @@ public class AmbariManagementControllerImpl implements AmbariManagementControlle
         throw new AmbariException("Username and password must be supplied.");
       }
 
      users.createUser(request.getUsername(), request.getPassword(), request.isActive(), request.isAdmin(), false);
      users.createUser(request.getUsername(), request.getPassword(), UserType.LOCAL, request.isActive(), request.isAdmin());
     }
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/UserDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/UserDAO.java
index d209cfcfc5..d3c2d891cb 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/UserDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/UserDAO.java
@@ -37,6 +37,8 @@ import com.google.inject.Singleton;
 import com.google.inject.persist.Transactional;
 import org.apache.ambari.server.security.authorization.UserType;
 
import org.apache.commons.lang.StringUtils;

 @Singleton
 public class UserDAO {
 
@@ -75,7 +77,7 @@ public class UserDAO {
   @RequiresSession
   public UserEntity findUserByNameAndType(String userName, UserType userType) {
     TypedQuery<UserEntity> query = entityManagerProvider.get().createQuery("SELECT user FROM UserEntity user WHERE " +
        "user.userType=:type AND user.userName=:name", UserEntity.class);
        "user.userType=:type AND lower(user.userName)=lower(:name)", UserEntity.class); // do case insensitive compare
     query.setParameter("type", userType);
     query.setParameter("name", userName);
     try {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
index 0bf7ec269f..8527271027 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
@@ -195,7 +195,8 @@ public class AmbariLdapAuthenticationProvider implements AuthenticationProvider
 
     UserEntity userEntity = userDAO.findLdapUserByName(userName);
 
    if (userEntity == null || !StringUtils.equals(userEntity.getUserName(), userName)) {
    // lookup is case insensitive, so no need for string comparison
    if (userEntity == null) {
       LOG.info("user not found ");
       throw new UsernameNotFoundException("Username " + userName + " not found");
     }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProvider.java
index a8c9b19ade..f3ae0c3374 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProvider.java
@@ -63,7 +63,7 @@ public class AmbariLocalUserProvider extends AbstractUserDetailsAuthenticationPr
 
     UserEntity userEntity = userDAO.findLocalUserByName(userName);
 
    if (userEntity == null || !StringUtils.equals(userEntity.getUserName(), userName)) {
    if (userEntity == null) {
       //TODO case insensitive name comparison is a temporary solution, until users API will change to use id as PK
       LOG.info("user not found ");
       throw new UsernameNotFoundException("Username " + userName + " not found");
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java
index 720918b646..85104fc5d6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java
@@ -26,8 +26,10 @@ import org.apache.ambari.server.orm.entities.MemberEntity;
 import org.apache.ambari.server.orm.entities.PermissionEntity;
 import org.apache.ambari.server.orm.entities.PrivilegeEntity;
 import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.commons.lang.StringUtils;
 import org.springframework.security.core.GrantedAuthority;
 

 /**
  * Describes user of web-services
  */
@@ -44,7 +46,7 @@ public class User {
 
   public User(UserEntity userEntity) {
     userId = userEntity.getUserId();
    userName = userEntity.getUserName();
    userName = StringUtils.lowerCase(userEntity.getUserName()); // normalize to lower case
     createTime = userEntity.getCreateTime();
     userType = userEntity.getUserType();
     ldapUser = userEntity.getLdapUser();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
index 1a7b58db37..5caaa2d5dc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
@@ -263,57 +263,30 @@ public class Users {
    * @throws AmbariException if user already exists
    */
   public void createUser(String userName, String password) throws AmbariException {
    createUser(userName, password, true, false, false);
    createUser(userName, password, UserType.LOCAL, true, false);
   }
 
   /**
   * Creates new local user with provided userName and password.
   * Creates new user with provided userName and password.
    *
    * @param userName user name
    * @param password password
   * @param userType user type
    * @param active is user active
    * @param admin is user admin
   * @param ldapUser is user LDAP
    * @throws AmbariException if user already exists
    */
  public synchronized void createUser(String userName, String password, Boolean active, Boolean admin, Boolean ldapUser) throws AmbariException {

    if (getAnyUser(userName) != null) {
      throw new AmbariException("User " + userName + " already exists");
    }

    // create an admin principal to represent this user
    PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findById(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
    if (principalTypeEntity == null) {
      principalTypeEntity = new PrincipalTypeEntity();
      principalTypeEntity.setId(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
      principalTypeEntity.setName(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME);
      principalTypeDAO.create(principalTypeEntity);
    }
    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalDAO.create(principalEntity);

    UserEntity userEntity = new UserEntity();
    userEntity.setUserName(userName);
    userEntity.setUserPassword(passwordEncoder.encode(password));
    userEntity.setPrincipal(principalEntity);
    if (active != null) {
      userEntity.setActive(active);
    }
    if (ldapUser != null) {
      userEntity.setLdapUser(ldapUser);
  public synchronized void createUser(String userName, String password, UserType userType, Boolean active, Boolean
      admin) throws AmbariException {
    // if user type is not provided, assume LOCAL since the default
    // value of user_type in the users table is LOCAL
    if (userType == null) {
      throw new AmbariException("UserType not specified.");
     }
 
    userDAO.create(userEntity);
    // store user name in lower case
    userName = StringUtils.lowerCase(userName);
 
    if (admin != null && admin) {
      grantAdminPrivilege(userEntity.getUserId());
    }
  }

  public synchronized void createUser(String userName, String password, UserType userType, Boolean active, Boolean
      admin) throws AmbariException {
     if (getUser(userName, userType) != null) {
       throw new AmbariException("User " + userName + " already exists");
     }
@@ -331,7 +304,7 @@ public class Users {
 
     UserEntity userEntity = new UserEntity();
     userEntity.setUserName(userName);
    if (userType == null || userType == UserType.LOCAL) {
    if (userType == UserType.LOCAL) {
       //passwords should be stored for local users only
       userEntity.setUserPassword(passwordEncoder.encode(password));
     }
@@ -339,8 +312,10 @@ public class Users {
     if (active != null) {
       userEntity.setActive(active);
     }
    if (userType != null) {
      userEntity.setUserType(userType);

    userEntity.setUserType(userType);
    if (userType == UserType.LDAP) {
      userEntity.setLdapUser(true);
     }
 
     userDAO.create(userEntity);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java
index dc22bb914d..d96e7b5415 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/UserResourceProviderTest.java
@@ -249,7 +249,7 @@ public class UserResourceProviderTest extends EasyMockSupport {
     Injector injector = createInjector();
 
     Users users = injector.getInstance(Users.class);
    users.createUser("User100", "password", (Boolean) null, null, false);
    users.createUser("User100", "password", UserType.LOCAL, (Boolean) null, null);
     expectLastCall().atLeastOnce();
 
     // replay
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java
index a509f54143..a4bd6c1503 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java
@@ -22,6 +22,7 @@ import org.apache.ambari.server.orm.entities.PrincipalEntity;
 import org.apache.ambari.server.orm.entities.UserEntity;
 import org.apache.ambari.server.security.authorization.AmbariUserAuthentication;
 import org.apache.ambari.server.security.authorization.User;
import org.apache.commons.lang.StringUtils;
 import org.junit.Assert;
 import org.junit.Test;
 import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
@@ -43,13 +44,13 @@ public class SecurityHelperImplTest {
     SecurityContext ctx = SecurityContextHolder.getContext();
     UserEntity userEntity = new UserEntity();
     userEntity.setPrincipal(new PrincipalEntity());
    userEntity.setUserName("userName");
    userEntity.setUserName("username"); // with user entity, always use lower case
     userEntity.setUserId(1);
     User user = new User(userEntity);
     Authentication auth = new AmbariUserAuthentication(null, user, null);
     ctx.setAuthentication(auth);
 
    Assert.assertEquals("userName", SecurityHelperImpl.getInstance().getCurrentUserName());
    Assert.assertEquals("username", SecurityHelperImpl.getInstance().getCurrentUserName());
   }
 
   @Test
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java
index b20607814f..fda31887d5 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java
@@ -23,6 +23,7 @@ import org.apache.ambari.server.orm.entities.UserEntity;
 import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
 import org.apache.ambari.server.security.authorization.internal.InternalTokenClientFilter;
 import org.apache.ambari.server.security.authorization.internal.InternalTokenStorage;
import org.apache.commons.lang.StringUtils;
 import org.easymock.Capture;
 import org.junit.Before;
 import org.junit.Test;
@@ -51,7 +52,7 @@ import static org.junit.Assert.assertNull;
 public class AmbariUserAuthenticationFilterTest {
   private static final String TEST_INTERNAL_TOKEN = "test token";
   private static final String TEST_USER_ID_HEADER = "1";
  private static final String TEST_USER_NAME = "userName";
  private static final String TEST_USER_NAME = "username"; // use lower case with user entity
   private static final int TEST_USER_ID = 1;
 
   @Before
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
index 44fb73c264..aa70be2d16 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/TestUsers.java
@@ -160,17 +160,27 @@ public class TestUsers {
 
   @Test
   public void testGetAnyUser() throws Exception {
    users.createUser("user", "user", true, false, false);
    users.createUser("user_ldap", "user_ldap", true, false, true);
    users.createUser("user", "user", UserType.LOCAL, true, false);
    users.createUser("user_ldap", "user_ldap", UserType.LDAP, true, false);
 
     assertEquals("user", users.getAnyUser("user").getUserName());
     assertEquals("user_ldap", users.getAnyUser("user_ldap").getUserName());
     Assert.assertNull(users.getAnyUser("non_existing"));
   }
 
  @Test
  public void testGetAnyUserCaseInsensitive() throws Exception {
    users.createUser("user", "user", UserType.LOCAL, true, false);
    users.createUser("user_ldap", "user_ldap", UserType.LDAP, true, false);

    assertEquals("user", users.getAnyUser("USER").getUserName());
    assertEquals("user_ldap", users.getAnyUser("USER_LDAP").getUserName());
    Assert.assertNull(users.getAnyUser("non_existing"));
  }

   @Test
   public void testGetUserById() throws Exception {
    users.createUser("user", "user", true, false, false);
    users.createUser("user", "user", UserType.LOCAL, true, false);
     User createdUser = users.getUser("user", UserType.LOCAL);
     User userById = users.getUser(createdUser.getUserId());
 
@@ -204,7 +214,7 @@ public class TestUsers {
   @Test
   public void testSetUserLdap() throws Exception {
     users.createUser("user", "user");
    users.createUser("user_ldap", "user_ldap", true, false, true);
    users.createUser("user_ldap", "user_ldap", UserType.LDAP, true, false);
 
     users.setUserLdap("user");
     Assert.assertEquals(true, users.getAnyUser("user").isLdapUser());
@@ -388,7 +398,7 @@ public class TestUsers {
 
   @Test
   public void testModifyPassword_UserByAdmin() throws Exception {
    users.createUser("admin", "admin", true, true, false);
    users.createUser("admin", "admin", UserType.LOCAL, true, true);
     users.createUser("user", "user");
 
     UserEntity userEntity = userDAO.findUserByName("user");
@@ -416,12 +426,12 @@ public class TestUsers {
   public void testCreateUserDefaultParams() throws Exception {
     final Users spy = Mockito.spy(users);
     spy.createUser("user", "user");
    Mockito.verify(spy).createUser("user", "user", true, false, false);
    Mockito.verify(spy).createUser("user", "user", UserType.LOCAL, true, false);
   }
 
   @Test
   public void testCreateUserFiveParams() throws Exception {
    users.createUser("user", "user", false, false, false);
    users.createUser("user", "user", UserType.LOCAL, false, false);
 
     final User createdUser = users.getAnyUser("user");
     Assert.assertEquals("user", createdUser.getUserName());
@@ -429,7 +439,7 @@ public class TestUsers {
     Assert.assertEquals(false, createdUser.isLdapUser());
     Assert.assertEquals(false, createdUser.isAdmin());
 
    users.createUser("user2", "user2", true, true, true);
    users.createUser("user2", "user2", UserType.LDAP, true, true);
     final User createdUser2 = users.getAnyUser("user2");
     Assert.assertEquals("user2", createdUser2.getUserName());
     Assert.assertEquals(true, createdUser2.isActive());
@@ -443,6 +453,12 @@ public class TestUsers {
     users.createUser("user", "user");
   }
 
  @Test(expected = AmbariException.class)
  public void testCreateUserDuplicateCaseInsensitive() throws Exception {
    users.createUser("user", "user");
    users.createUser("USER", "user");
  }

   @Test
   public void testRemoveUser() throws Exception {
     users.createUser("user1", "user1");
@@ -468,7 +484,7 @@ public class TestUsers {
 
   @Test
   public void testRevokeAdminPrivilege() throws Exception {
    users.createUser("admin", "admin", true, true, false);
    users.createUser("admin", "admin", UserType.LOCAL, true, true);
 
     final User admin = users.getAnyUser("admin");
     users.revokeAdminPrivilege(admin.getUserId());
@@ -478,8 +494,8 @@ public class TestUsers {
 
   @Test
   public void testIsUserCanBeRemoved() throws Exception {
    users.createUser("admin", "admin", true, true, false);
    users.createUser("admin2", "admin2", true, true, false);
    users.createUser("admin", "admin", UserType.LOCAL, true, true);
    users.createUser("admin2", "admin2", UserType.LOCAL, true, true);
 
     Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
     Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin2")));
@@ -490,7 +506,7 @@ public class TestUsers {
     users.createUser("user", "user");
     Assert.assertFalse(users.isUserCanBeRemoved(userDAO.findUserByName("admin2")));
 
    users.createUser("admin3", "admin3", true, true, false);
    users.createUser("admin3", "admin3", UserType.LOCAL, true, true);
     Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin2")));
     Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin3")));
   }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
index 4ff751dcba..0e4b4ebae0 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
@@ -2350,17 +2350,17 @@ public class UpgradeCatalog240Test {
     final Users users = createMock(Users.class);
 
     RequestScheduleEntity requestScheduleEntity = new RequestScheduleEntity();
    requestScheduleEntity.setCreateUser("createdUser");
    requestScheduleEntity.setCreateUser("createduser"); // use lower case user name with request schedule entity
     requestScheduleEntity.setClusterId(1L);
 
     expect(requestScheduleDAO.findAll()).andReturn(Collections.singletonList(requestScheduleEntity)).once();
 
     UserEntity userEntity = new UserEntity();
    userEntity.setUserName("createdUser");
    userEntity.setUserName("createduser"); // use lower case user name with user entity
     userEntity.setUserId(1);
     userEntity.setPrincipal(new PrincipalEntity());
     User user = new User(userEntity);
    expect(users.getUserIfUnique("createdUser")).andReturn(user).once();
    expect(users.getUserIfUnique("createduser")).andReturn(user).once();
 
     expect(requestScheduleDAO.merge(requestScheduleEntity)).andReturn(requestScheduleEntity).once();
 
- 
2.19.1.windows.1

