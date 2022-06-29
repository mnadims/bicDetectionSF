From 76ea14ed31b27204907010100705eeaa2922e00e Mon Sep 17 00:00:00 2001
From: Nahappan Somasundaram <nsomasundaram@hortonworks.com>
Date: Mon, 3 Oct 2016 19:54:16 -0700
Subject: [PATCH] AMBARI-18520: Ambari usernames should not be converted to
 lowercase before storing in the DB.

--
 .../server/api/services/UserService.java      |  2 +-
 .../internal/UserResourceProvider.java        | 25 ++++++-
 .../predicate/ComparisonPredicate.java        | 16 ++++-
 .../controller/predicate/EqualsPredicate.java | 15 ++++
 .../server/security/authorization/User.java   |  2 +-
 .../server/security/authorization/Users.java  |  8 +--
 .../server/api/services/UserServiceTest.java  | 71 -------------------
 .../security/SecurityHelperImplTest.java      |  4 +-
 .../AmbariUserAuthenticationFilterTest.java   |  2 +-
 .../server/upgrade/UpgradeCatalog240Test.java |  6 +-
 10 files changed, 65 insertions(+), 86 deletions(-)
 delete mode 100644 ambari-server/src/test/java/org/apache/ambari/server/api/services/UserServiceTest.java

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/UserService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/UserService.java
index c46c373e56..a0fccadbfa 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/UserService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/UserService.java
@@ -172,6 +172,6 @@ public class UserService extends BaseService {
    */
   private ResourceInstance createUserResource(String userName) {
     return createResource(Resource.Type.User,
        Collections.singletonMap(Resource.Type.User, StringUtils.lowerCase(userName)));
        Collections.singletonMap(Resource.Type.User, userName));
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserResourceProvider.java
index 0324d38066..adf660baba 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/UserResourceProvider.java
@@ -21,6 +21,7 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.controller.UserRequest;
 import org.apache.ambari.server.controller.UserResponse;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
 import org.apache.ambari.server.controller.spi.*;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
 import org.apache.ambari.server.security.authorization.AuthorizationException;
@@ -35,7 +36,7 @@ import java.util.Set;
 /**
  * Resource provider for user resources.
  */
public class UserResourceProvider extends AbstractControllerResourceProvider {
public class UserResourceProvider extends AbstractControllerResourceProvider implements ResourcePredicateEvaluator {
 
   // ----- Property ID constants ---------------------------------------------
 
@@ -188,6 +189,28 @@ public class UserResourceProvider extends AbstractControllerResourceProvider {
     return getRequestStatus(null);
   }
 
  /**
   * ResourcePredicateEvaluator implementation. If property type is User/user_name,
   * we do a case insensitive comparison so that we can return the retrieved
   * username when it differs only in case with respect to the requested username.
   *
   * @param predicate  the predicate
   * @param resource   the resource
   *
     * @return
     */
  @Override
  public boolean evaluate(Predicate predicate, Resource resource) {
    if (predicate instanceof EqualsPredicate) {
      EqualsPredicate equalsPredicate = (EqualsPredicate)predicate;
      String propertyId = equalsPredicate.getPropertyId();
      if (propertyId.equals(USER_USERNAME_PROPERTY_ID)) {
        return equalsPredicate.evaluateIgnoreCase(resource);
      }
    }
    return predicate.evaluate(resource);
  }

   @Override
   protected Set<String> getPKPropertyIds() {
     return pkPropertyIds;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/ComparisonPredicate.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/ComparisonPredicate.java
index a36f0fb074..be8016e56e 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/ComparisonPredicate.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/ComparisonPredicate.java
@@ -76,7 +76,15 @@ public abstract class ComparisonPredicate<T> extends PropertyPredicate implement
     visitor.acceptComparisonPredicate(this);
   }
 
  protected int compareValueToIgnoreCase(Object propertyValue) throws ClassCastException{
    return compareValueTo(propertyValue, true); // case insensitive
  }

   protected int compareValueTo(Object propertyValue) throws ClassCastException{
    return compareValueTo(propertyValue, false); // case sensitive
  }

  private int compareValueTo(Object propertyValue, boolean ignoreCase) throws ClassCastException {
     if (doubleValue != null) {
       if (propertyValue instanceof Number) {
         return doubleValue.compareTo(((Number) propertyValue).doubleValue());
@@ -88,8 +96,14 @@ public abstract class ComparisonPredicate<T> extends PropertyPredicate implement
         }
       }
     }

     if (stringValue != null) {
      return stringValue.compareTo(propertyValue.toString());
      if (ignoreCase) {
        return stringValue.compareToIgnoreCase(propertyValue.toString());
      }
      else {
        return stringValue.compareTo(propertyValue.toString());
      }
     }
 
     return getValue().compareTo((T) propertyValue);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/EqualsPredicate.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/EqualsPredicate.java
index 64f5c6fc94..7ac0e7a0c0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/EqualsPredicate.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/predicate/EqualsPredicate.java
@@ -39,6 +39,21 @@ public class EqualsPredicate<T> extends ComparisonPredicate<T> {
         propertyValue != null && compareValueTo(propertyValue) == 0;
   }
 
  /**
   * Case insensitive equality support for string types
   *
   * @param resource
   * @return
     */
  public boolean evaluateIgnoreCase(Resource resource) {
    Object propertyValue  = resource.getPropertyValue(getPropertyId());
    Object predicateValue = getValue();

    return predicateValue == null ?
            propertyValue == null :
            propertyValue != null && compareValueToIgnoreCase(propertyValue) == 0;
  }

   @Override
   public String getOperator() {
     return "=";
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java
index 85104fc5d6..3064f62b1d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/User.java
@@ -46,7 +46,7 @@ public class User {
 
   public User(UserEntity userEntity) {
     userId = userEntity.getUserId();
    userName = StringUtils.lowerCase(userEntity.getUserName()); // normalize to lower case
    userName = userEntity.getUserName();
     createTime = userEntity.getCreateTime();
     userType = userEntity.getUserType();
     ldapUser = userEntity.getLdapUser();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
index e547f058d4..a4f0031666 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/Users.java
@@ -282,11 +282,9 @@ public class Users {
       throw new AmbariException("UserType not specified.");
     }
 
    // store user name in lower case
    userName = StringUtils.lowerCase(userName);

    if (getUser(userName, userType) != null) {
      throw new AmbariException("User " + userName + " already exists");
    User existingUser = getUser(userName, userType);
    if (existingUser != null) {
      throw new AmbariException("User " + existingUser.getUserName() + " already exists");
     }
 
     PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findById(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/api/services/UserServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/api/services/UserServiceTest.java
deleted file mode 100644
index 0ed0a6657c..0000000000
-- a/ambari-server/src/test/java/org/apache/ambari/server/api/services/UserServiceTest.java
++ /dev/null
@@ -1,71 +0,0 @@
/*
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
package org.apache.ambari.server.api.services;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for UserService.
 */
public class UserServiceTest {

  @Test
  public void testCreateResourcesWithUppercaseUsername() {
    // GIVEN
    UserService userService = new TestUserService();
    // WHEN
    Response response = userService.getUser(null, null, null, "MyUser");
    // THEN
    assertEquals("myuser", ((UserEntity) response.getEntity()).getUserName());
  }

  class TestUserService extends UserService {
    @Override
    protected Response handleRequest(HttpHeaders headers, String body, UriInfo uriInfo,
                                     Request.Type requestType, final ResourceInstance resource) {
      return new Response() {
        @Override
        public Object getEntity() {
          UserEntity entity = new UserEntity();
          entity.setUserName(resource.getKeyValueMap().get(Resource.Type.User));
          return entity;
        }

        @Override
        public int getStatus() {
          return 0;
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
          return null;
        }
      };
    }
  }
}
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java
index a4bd6c1503..344254689e 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/SecurityHelperImplTest.java
@@ -44,13 +44,13 @@ public class SecurityHelperImplTest {
     SecurityContext ctx = SecurityContextHolder.getContext();
     UserEntity userEntity = new UserEntity();
     userEntity.setPrincipal(new PrincipalEntity());
    userEntity.setUserName("username"); // with user entity, always use lower case
    userEntity.setUserName("userName");
     userEntity.setUserId(1);
     User user = new User(userEntity);
     Authentication auth = new AmbariUserAuthentication(null, user, null);
     ctx.setAuthentication(auth);
 
    Assert.assertEquals("username", SecurityHelperImpl.getInstance().getCurrentUserName());
    Assert.assertEquals("userName", SecurityHelperImpl.getInstance().getCurrentUserName());
   }
 
   @Test
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java
index fda31887d5..80a66fdfc2 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariUserAuthenticationFilterTest.java
@@ -52,7 +52,7 @@ import static org.junit.Assert.assertNull;
 public class AmbariUserAuthenticationFilterTest {
   private static final String TEST_INTERNAL_TOKEN = "test token";
   private static final String TEST_USER_ID_HEADER = "1";
  private static final String TEST_USER_NAME = "username"; // use lower case with user entity
  private static final String TEST_USER_NAME = "userName";
   private static final int TEST_USER_ID = 1;
 
   @Before
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
index 099af7e475..958758fb34 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog240Test.java
@@ -2532,17 +2532,17 @@ public class UpgradeCatalog240Test {
     final Users users = createMock(Users.class);
 
     RequestScheduleEntity requestScheduleEntity = new RequestScheduleEntity();
    requestScheduleEntity.setCreateUser("createduser"); // use lower case user name with request schedule entity
    requestScheduleEntity.setCreateUser("createdUser");
     requestScheduleEntity.setClusterId(1L);
 
     expect(requestScheduleDAO.findAll()).andReturn(Collections.singletonList(requestScheduleEntity)).once();
 
     UserEntity userEntity = new UserEntity();
    userEntity.setUserName("createduser"); // use lower case user name with user entity
    userEntity.setUserName("createdUser");
     userEntity.setUserId(1);
     userEntity.setPrincipal(new PrincipalEntity());
     User user = new User(userEntity);
    expect(users.getUserIfUnique("createduser")).andReturn(user).once();
    expect(users.getUserIfUnique("createdUser")).andReturn(user).once();
 
     expect(requestScheduleDAO.merge(requestScheduleEntity)).andReturn(requestScheduleEntity).once();
 
- 
2.19.1.windows.1

