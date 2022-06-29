From 6b7a7a706d6e4d8c4a5e0300666e56f6704eb00a Mon Sep 17 00:00:00 2001
From: lpuskas <lpuskas@apache.org>
Date: Wed, 5 Jul 2017 14:20:18 +0200
Subject: [PATCH] AMBARI-21307 Feature for supporting LDAP configuration from
 the UI

--
 ambari-funtest/pom.xml                        |  67 ----
 ambari-project/pom.xml                        |  52 +--
 ambari-server/pom.xml                         |  83 +----
 .../ResourceInstanceFactoryImpl.java          |   4 +
 .../AmbariConfigurationRequestSwagger.java    |  47 +++
 .../AmbariConfigurationResponseSwagger.java   |  40 +++
 .../services/AmbariConfigurationService.java  | 193 +++++++++++
 .../services/ldap/AmbariConfiguration.java    |  87 +++++
 .../services/ldap/LdapConfigOperation.java    |  43 +++
 .../ldap/LdapConfigurationRequest.java        |  49 +++
 .../ldap/LdapConfigurationService.java        | 185 ++++++++++
 .../api/services/ldap/LdapRequestInfo.java    |  61 ++++
 .../stackadvisor/StackAdvisorRequest.java     |  12 +
 .../commands/StackAdvisorCommand.java         |  54 +++
 .../server/controller/AmbariServer.java       |   3 +-
 .../server/controller/ControllerModule.java   |   3 +
 .../controller/ResourceProviderFactory.java   |  24 +-
 .../AbstractControllerResourceProvider.java   |   2 +
 .../internal/AbstractProviderModule.java      |   2 +-
 .../AmbariConfigurationResourceProvider.java  | 328 ++++++++++++++++++
 .../internal/DefaultProviderModule.java       |  24 +-
 .../server/controller/spi/Resource.java       |   5 +-
 .../ambari/server/events/AmbariEvent.java     |  11 +-
 .../events/AmbariLdapConfigChangedEvent.java  |  37 ++
 .../apache/ambari/server/ldap/LdapModule.java |  82 +++++
 .../ldap/domain/AmbariLdapConfigKeys.java     |  83 +++++
 .../ldap/domain/AmbariLdapConfiguration.java  | 199 +++++++++++
 .../AmbariLdapConfigurationFactory.java       |  34 ++
 .../AmbariLdapConfigurationProvider.java      | 120 +++++++
 .../ldap/service/AmbariLdapException.java     |  33 ++
 .../server/ldap/service/AmbariLdapFacade.java | 140 ++++++++
 .../ldap/service/AttributeDetector.java       |  41 +++
 .../LdapAttributeDetectionService.java        |  40 +++
 .../service/LdapConfigurationService.java     |  60 ++++
 .../service/LdapConnectionConfigService.java  |  36 ++
 .../server/ldap/service/LdapFacade.java       |  58 ++++
 .../DefaultLdapAttributeDetectionService.java | 200 +++++++++++
 .../ads/DefaultLdapConfigurationService.java  | 213 ++++++++++++
 .../DefaultLdapConnectionConfigService.java   | 113 ++++++
 .../ads/LdapConnectionTemplateFactory.java    | 111 ++++++
 .../detectors/AttributeDetectorFactory.java   |  75 ++++
 .../detectors/ChainedAttributeDetector.java   |  73 ++++
 .../detectors/GroupMemberAttrDetector.java    |  65 ++++
 .../ads/detectors/GroupNameAttrDetector.java  |  70 ++++
 .../detectors/GroupObjectClassDetector.java   |  73 ++++
 .../OccurrenceAndWeightBasedDetector.java     | 143 ++++++++
 .../UserGroupMemberAttrDetector.java          |  64 ++++
 .../ads/detectors/UserNameAttrDetector.java   |  71 ++++
 .../detectors/UserObjectClassDetector.java    |  69 ++++
 .../orm/dao/AmbariConfigurationDAO.java       |  89 +++++
 .../ambari/server/orm/dao/DaoUtils.java       |  13 +-
 .../entities/AmbariConfigurationEntity.java   |  70 ++++
 .../orm/entities/ConfigurationBaseEntity.java | 159 +++++++++
 .../authorization/RoleAuthorization.java      |  95 ++---
 .../resources/Ambari-DDL-Derby-CREATE.sql     |  21 ++
 .../resources/Ambari-DDL-MySQL-CREATE.sql     |  20 ++
 .../resources/Ambari-DDL-Oracle-CREATE.sql    |  20 ++
 .../resources/Ambari-DDL-Postgres-CREATE.sql  |  25 +-
 .../Ambari-DDL-SQLAnywhere-CREATE.sql         |  20 ++
 .../resources/Ambari-DDL-SQLServer-CREATE.sql |  20 ++
 .../main/resources/META-INF/persistence.xml   |   2 +
 .../commands/StackAdvisorCommandTest.java     | 212 +++++++++++
 .../server/checks/UpgradeCheckOrderTest.java  |   3 +-
 ...bariConfigurationResourceProviderTest.java | 251 ++++++++++++++
 .../StackAdvisorResourceProviderTest.java     |  97 +++---
 .../server/ldap/LdapModuleFunctionalTest.java | 149 ++++++++
 .../TestAmbariLdapConfigurationFactory.java   |  29 ++
 .../ldap/service/AmbariLdapFacadeTest.java    | 215 ++++++++++++
 ...aultLdapAttributeDetectionServiceTest.java | 188 ++++++++++
 .../DefaultLdapConfigurationServiceTest.java  | 221 ++++++++++++
 .../GroupMemberAttrDetectorTest.java          | 107 ++++++
 .../notifications/DispatchFactoryTest.java    |   3 +-
 .../server/orm/InMemoryDefaultTestModule.java |   2 +
 .../ambari/server/orm/JdbcPropertyTest.java   |   5 +-
 ...henticationProviderForDNWithSpaceTest.java |  35 +-
 .../AmbariLdapAuthenticationProviderTest.java |   3 +-
 .../AmbariLocalUserProviderTest.java          |   3 +-
 .../LdapServerPropertiesTest.java             |   5 +-
 78 files changed, 5409 insertions(+), 355 deletions(-)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationResponseSwagger.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigOperation.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapRequestInfo.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/events/AmbariLdapConfigChangedEvent.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/LdapModule.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigKeys.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapException.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapFacade.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AttributeDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapAttributeDetectionService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConfigurationService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConnectionConfigService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapFacade.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConnectionConfigService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/AttributeDetectorFactory.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/ChainedAttributeDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupNameAttrDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupObjectClassDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/OccurrenceAndWeightBasedDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserGroupMemberAttrDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserNameAttrDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserObjectClassDetector.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetectorTest.java

diff --git a/ambari-funtest/pom.xml b/ambari-funtest/pom.xml
index bb2068d901..6466af30ae 100644
-- a/ambari-funtest/pom.xml
++ b/ambari-funtest/pom.xml
@@ -196,73 +196,6 @@
       <groupId>org.springframework.ldap</groupId>
       <artifactId>spring-ldap-core</artifactId>
     </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-server-annotations</artifactId>
      <scope>test</scope>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-core-integ</artifactId>
      <scope>test</scope>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-server-integ</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-jdbm</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-kerberos-codec</artifactId>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-core</artifactId>
      <scope>test</scope>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-protocol-ldap</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>kerberos-client</artifactId>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.shared</groupId>
      <artifactId>shared-ldap</artifactId>
      <scope>test</scope>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-api</artifactId>
diff --git a/ambari-project/pom.xml b/ambari-project/pom.xml
index 00ba1bcb4c..f6e3bc7611 100644
-- a/ambari-project/pom.xml
++ b/ambari-project/pom.xml
@@ -31,6 +31,7 @@
     <ambari.dir>${project.parent.basedir}</ambari.dir>
     <powermock.version>1.6.3</powermock.version>
     <jetty.version>8.1.19.v20160209</jetty.version>
    <ldap-api.version>1.0.0</ldap-api.version>
     <checkstyle.version>6.19</checkstyle.version> <!-- last version that does not require Java 8 -->
     <swagger.version>1.5.10</swagger.version>
     <swagger.maven.plugin.version>3.1.4</swagger.maven.plugin.version>
@@ -160,57 +161,6 @@
         <artifactId>spring-ldap-core</artifactId>
         <version>2.0.4.RELEASE</version>
       </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>apacheds-server-annotations</artifactId>
        <version>2.0.0-M19</version>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>apacheds-core-integ</artifactId>
        <version>2.0.0-M19</version>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>apacheds-server-integ</artifactId>
        <version>2.0.0-M19</version>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>apacheds-jdbm</artifactId>
        <version>2.0.0-M5</version>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>apacheds-kerberos-codec</artifactId>
        <version>2.0.0-M19</version>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>apacheds-core</artifactId>
        <version>2.0.0-M19</version>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>kerberos-client</artifactId>
        <version>2.0.0-M19</version>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.server</groupId>
        <artifactId>apacheds-protocol-ldap</artifactId>
        <version>2.0.0-M19</version>
        <exclusions>
          <exclusion>
            <groupId>org.apache.directory.jdbm</groupId>
            <artifactId>apacheds-jdbm1</artifactId>
          </exclusion>
        </exclusions>
      </dependency>
      <dependency>
        <groupId>org.apache.directory.shared</groupId>
        <artifactId>shared-ldap</artifactId>
        <version>0.9.17</version>
      </dependency>
       <dependency>
         <groupId>org.slf4j</groupId>
         <artifactId>slf4j-api</artifactId>
diff --git a/ambari-server/pom.xml b/ambari-server/pom.xml
index e250da7592..a86acf5390 100644
-- a/ambari-server/pom.xml
++ b/ambari-server/pom.xml
@@ -1224,73 +1224,6 @@
       <groupId>org.springframework.ldap</groupId>
       <artifactId>spring-ldap-core</artifactId>
     </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-server-annotations</artifactId>
      <scope>test</scope>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-core-integ</artifactId>
      <scope>test</scope>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-server-integ</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-jdbm</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-kerberos-codec</artifactId>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-core</artifactId>
      <scope>test</scope>
      <exclusions>
        <exclusion>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache-core</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-protocol-ldap</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>kerberos-client</artifactId>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.shared</groupId>
      <artifactId>shared-ldap</artifactId>
      <scope>test</scope>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-api</artifactId>
@@ -1622,6 +1555,12 @@
       <groupId>org.apache.hadoop</groupId>
       <artifactId>hadoop-auth</artifactId>
       <version>${hadoop.version}</version>
      <exclusions>
        <exclusion>
          <groupId>org.apache.directory.server</groupId>
          <artifactId>apacheds-kerberos-codec</artifactId>
        </exclusion>
      </exclusions>
     </dependency>
     <dependency>
       <groupId>org.apache.hadoop</groupId>
@@ -1688,6 +1627,16 @@
       <artifactId>jna</artifactId>
       <version>4.2.2</version>
     </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>apacheds-all</artifactId>
      <version>2.0.0-M24</version>
    </dependency>
    <dependency>
      <groupId>org.apache.directory.server</groupId>
      <artifactId>kerberos-client</artifactId>
      <version>2.0.0-M24</version>
    </dependency>
     <dependency>
       <groupId>com.networknt</groupId>
       <artifactId>json-schema-validator</artifactId>
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java
index d0d115d682..f5fb6e9900 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java
@@ -471,6 +471,10 @@ public class ResourceInstanceFactoryImpl implements ResourceInstanceFactory {
       case RemoteCluster:
         resourceDefinition = new RemoteClusterResourceDefinition();
         break;
      case AmbariConfiguration:
        resourceDefinition = new SimpleResourceDefinition(Resource.Type.AmbariConfiguration, "ambariconfiguration", "ambariconfigurations");

        break;
 
       default:
         throw new IllegalArgumentException("Unsupported resource type: " + type);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
new file mode 100644
index 0000000000..5e8094e9c7
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
@@ -0,0 +1,47 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Map;

import org.apache.ambari.server.controller.ApiModel;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request data model for {@link org.apache.ambari.server.api.services.AmbariConfigurationService}
 */
public interface AmbariConfigurationRequestSwagger extends ApiModel {

  @ApiModelProperty(name = "AmbariConfiguration")
  AmbariConfigurationRequestInfo getAmbariConfiguration();

  interface AmbariConfigurationRequestInfo {
    @ApiModelProperty
    Long getId();

    @ApiModelProperty
    Map<String, Object> getData();

    @ApiModelProperty
    String getType();

    @ApiModelProperty
    Long getVersion();

    @ApiModelProperty(name = "version_tag")
    String getVersionTag();
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationResponseSwagger.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationResponseSwagger.java
new file mode 100644
index 0000000000..c55ac1dd60
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationResponseSwagger.java
@@ -0,0 +1,40 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Map;

import org.apache.ambari.server.controller.ApiModel;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response data model for {@link org.apache.ambari.server.api.services.AmbariConfigurationService}
 */
public interface AmbariConfigurationResponseSwagger extends ApiModel {

  @ApiModelProperty(name = "AmbariConfiguration")
  AmbariConfigurationResponseInfo getAmbariConfigurationResponse();

  interface AmbariConfigurationResponseInfo {
    @ApiModelProperty
    Long getId();

    @ApiModelProperty
    Map<String, Object> getData();

    @ApiModelProperty
    String getType();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
new file mode 100644
index 0000000000..38ae7669db
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
@@ -0,0 +1,193 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Collections;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Rest endpoint for managing ambari configurations. Supports CRUD operations.
 * Ambari configurations are resources that relate to the ambari server instance even before a cluster is provisioned.
 *
 * Ambari configuration resources may be shared with components and services in the cluster
 * (by recommending them as default values)
 *
 * Eg. LDAP configuration is stored as ambariconfiguration.
 * The request payload has the form:
 *
 * <pre>
 *      {
 *        "AmbariConfiguration": {
 *            "type": "ldap-configuration",
 *            "data": [
 *                {
 *                 "authentication.ldap.primaryUrl": "localhost:33389"
 *                 "authentication.ldap.secondaryUrl": "localhost:333"
 *                 "authentication.ldap.baseDn": "dc=ambari,dc=apache,dc=org"
 *                 // ......
 *         ]
 *     }
 * </pre>
 */
@Path("/ambariconfigs/")
@Api(value = "Ambari Configurations", description = "Endpoint for Ambari configuration related operations")
public class AmbariConfigurationService extends BaseService {

  private static final String AMBARI_CONFIGURATION_REQUEST_TYPE =
    "org.apache.ambari.server.api.services.AmbariConfigurationRequestSwagger";

  /**
   * Creates an ambari configuration resource.
   *
   * @param body    the payload in json format
   * @param headers http headers
   * @param uri     request uri information
   * @return
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates an ambari configuration resource",
    nickname = "AmbariConfigurationService#createAmbariConfiguration")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = AMBARI_CONFIGURATION_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.POST, createResource(Resource.Type.AmbariConfiguration,
      Collections.EMPTY_MAP));
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve all ambari configuration resources",
    nickname = "AmbariConfigurationService#getAmbariConfigurations",
    notes = "Returns all Ambari configurations.",
    response = AmbariConfigurationResponseSwagger.class,
    responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
      defaultValue = "AmbariConfiguration/data, AmbariConfiguration/id, AmbariConfiguration/type",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
      defaultValue = "AmbariConfiguration/id",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getAmbariConfigurations(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(Resource.Type.AmbariConfiguration,
      Collections.EMPTY_MAP));
  }

  @GET
  @Path("{configurationId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve the details of an ambari configuration resource",
    nickname = "AmbariConfigurationService#getAmbariConfiguration",
    response = AmbariConfigurationResponseSwagger.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "AmbariConfiguration/*",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                         @PathParam("configurationId") String configurationId) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
  }

  @PUT
  @Path("{configurationId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates ambari configuration resources ",
    nickname = "AmbariConfigurationService#updateAmbariConfiguration")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = AMBARI_CONFIGURATION_REQUEST_TYPE, paramType = PARAM_TYPE_BODY),
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "AmbariConfiguration/*",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                            @PathParam("configurationId") String configurationId) {
    return handleRequest(headers, body, uri, Request.Type.PUT, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
  }

  @DELETE
  @Path("{configurationId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes an ambari configuration resource",
    nickname = "AmbariConfigurationService#deleteAmbariConfiguration")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                            @PathParam("configurationId") String configurationId) {
    return handleRequest(headers, body, uri, Request.Type.DELETE, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
new file mode 100644
index 0000000000..b5cc9212da
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
@@ -0,0 +1,87 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services.ldap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Domain POJO representing generic ambari configuration data.
 */
public class AmbariConfiguration {

  /**
   * The type of the configuration,  eg.: ldap-configuration
   */
  private String type;

  /**
   * Version tag
   */
  private String versionTag;

  /**
   * Version number
   */
  private Integer version;

  /**
   * Created timestamp
   */
  private long createdTs;

  private Set<Map<String, Object>> data = Collections.emptySet();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<Map<String, Object>> getData() {
    return data;
  }

  public void setData(Set<Map<String, Object>> data) {
    this.data = data;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public long getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(long createdTs) {
    this.createdTs = createdTs;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigOperation.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigOperation.java
new file mode 100644
index 0000000000..478d4ff188
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigOperation.java
@@ -0,0 +1,43 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services.ldap;

/**
 * Enumeration for supported operations related to LDAP configuration.
 */
public enum LdapConfigOperation {
  TEST_CONNECTION("test-connection"),
  TEST_ATTRIBUTES("test-attributes"),
  DETECT_ATTRIBUTES("detect-attributes");

  private String actionStr;

  LdapConfigOperation(String actionStr) {
    this.actionStr = actionStr;
  }

  public static LdapConfigOperation fromAction(String action) {
    for (LdapConfigOperation val : LdapConfigOperation.values()) {
      if (val.action().equals(action)) {
        return val;
      }
    }
    throw new IllegalStateException("Action [ " + action + " ] is not supported");
  }

  public String action() {
    return this.actionStr;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java
new file mode 100644
index 0000000000..2e478c4329
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java
@@ -0,0 +1,49 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services.ldap;


import com.google.gson.annotations.SerializedName;

/**
 * Request object wrapping information for LDAP configuration related request calls.
 */
public class LdapConfigurationRequest {

  @SerializedName("AmbariConfiguration")
  private AmbariConfiguration ambariConfiguration;

  @SerializedName("RequestInfo")
  private LdapRequestInfo requestInfo;

  public LdapConfigurationRequest() {
  }

  public AmbariConfiguration getAmbariConfiguration() {
    return ambariConfiguration;
  }

  public void setAmbariConfiguration(AmbariConfiguration ambariConfiguration) {
    this.ambariConfiguration = ambariConfiguration;
  }

  public LdapRequestInfo getRequestInfo() {
    return requestInfo;
  }

  public void setRequestInfo(LdapRequestInfo requestInfo) {
    this.requestInfo = requestInfo;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
new file mode 100644
index 0000000000..13f8835655
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
@@ -0,0 +1,185 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services.ldap;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariConfigurationService;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.google.common.collect.Sets;

/**
 * Endpoint designated to LDAP specific operations.
 */
@StaticallyInject
@Path("/ldapconfigs/")
public class LdapConfigurationService extends AmbariConfigurationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigurationService.class);

  @Inject
  private static LdapFacade ldapFacade;

  @Inject
  private static AmbariLdapConfigurationFactory ambariLdapConfigurationFactory;


  @POST
  @ApiIgnore // until documented
  @Path("/validate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateConfiguration(LdapConfigurationRequest ldapConfigurationRequest) {

    // check if the user is authorized to perform the operation
    authorize();

    Set<String> groups = Sets.newHashSet();
    Object responseEntity = null;

    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));
    try {

      validateRequest(ldapConfigurationRequest);

      AmbariLdapConfiguration ambariLdapConfiguration = ambariLdapConfigurationFactory.createLdapConfiguration(
        ldapConfigurationRequest.getAmbariConfiguration().getData().iterator().next());

      LdapConfigOperation action = LdapConfigOperation.fromAction(ldapConfigurationRequest.getRequestInfo().getAction());
      switch (action) {

        case TEST_CONNECTION:

          LOGGER.info("Testing connection to the LDAP server ...");
          ldapFacade.checkConnection(ambariLdapConfiguration);

          break;
        case TEST_ATTRIBUTES:

          LOGGER.info("Testing LDAP attributes ....");
          groups = ldapFacade.checkLdapAttributes(ldapConfigurationRequest.getRequestInfo().getParameters(), ambariLdapConfiguration);
          responseEntity = groups;

          break;
        case DETECT_ATTRIBUTES:

          LOGGER.info("Detecting LDAP attributes ...");
          ambariLdapConfiguration = ldapFacade.detectAttributes(ambariLdapConfiguration);
          responseEntity = ambariLdapConfiguration;

          break;
        default:
          LOGGER.warn("No action provided ...");
          throw new IllegalArgumentException("No request action provided");
      }

    } catch (Exception e) {
      result.setResultStatus(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e));
      responseEntity = e.getMessage();
    }

    return Response.status(result.getStatus().getStatusCode()).entity(responseEntity).build();
  }

  private void setResult(Set<String> groups, Result result) {
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    resource.setProperty("groups", groups);
    result.getResultTree().addChild(resource, "payload");
  }

  private void validateRequest(LdapConfigurationRequest ldapConfigurationRequest) {
    String errMsg;

    if (null == ldapConfigurationRequest) {
      errMsg = "No ldap configuraiton request provided";
      LOGGER.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }

    if (null == ldapConfigurationRequest.getRequestInfo()) {
      errMsg = String.format("No request information provided. Request: [%s]", ldapConfigurationRequest);
      LOGGER.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }

    if (null == ldapConfigurationRequest.getAmbariConfiguration()
      || ldapConfigurationRequest.getAmbariConfiguration().getData().size() != 1) {
      errMsg = String.format("No / Invalid configuration data provided. Request: [%s]", ldapConfigurationRequest);
      LOGGER.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }
  }

  private void authorize() {
    try {
      Authentication authentication = AuthorizationHelper.getAuthentication();

      if (authentication == null || !authentication.isAuthenticated()) {
        throw new AuthorizationException("Authentication data is not available, authorization to perform the requested operation is not granted");
      }

      if (!AuthorizationHelper.isAuthorized(authentication, ResourceType.AMBARI, null, requiredAuthorizations())) {
        throw new AuthorizationException("The authenticated user does not have the appropriate authorizations to create the requested resource(s)");
      }
    } catch (AuthorizationException e) {
      LOGGER.error("Unauthorized operation.", e);
      throw new IllegalArgumentException("User is not authorized to perform the operation", e);
    }

  }

  private Set<RoleAuthorization> requiredAuthorizations() {
    return Sets.newHashSet(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapRequestInfo.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapRequestInfo.java
new file mode 100644
index 0000000000..eeecfeec38
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapRequestInfo.java
@@ -0,0 +1,61 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services.ldap;

import java.util.Map;

import org.apache.ambari.server.controller.RequestPostRequest;

/**
 * Bean holding LDAP request specific request information.
 */
public class LdapRequestInfo implements RequestPostRequest.RequestInfo {

  // no-arg costructor facilitating JSON serialization
  public LdapRequestInfo() {
  }

  private String action;

  private Map<String, Object> parameters;

  @Override
  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public String getCommand() {
    return null;
  }

  @Override
  public RequestPostRequest.OperationLevel getOperationLevel() {
    return null;
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorRequest.java
index 3a2b488457..cd26c5643b 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/StackAdvisorRequest.java
@@ -31,6 +31,8 @@ import org.apache.ambari.server.api.services.stackadvisor.recommendations.Recomm
 import org.apache.ambari.server.state.ChangedConfigInfo;
 import org.apache.commons.lang.StringUtils;
 
import com.google.common.base.Preconditions;

 /**
  * Stack advisor request.
  */
@@ -48,6 +50,7 @@ public class StackAdvisorRequest {
   private List<ChangedConfigInfo> changedConfigurations = new LinkedList<>();
   private Set<RecommendationResponse.ConfigGroup> configGroups;
   private Map<String, String> userContext = new HashMap<>();
  private Map<String, Object> ldapConfig = new HashMap<>();
 
   public String getStackName() {
     return stackName;
@@ -93,6 +96,8 @@ public class StackAdvisorRequest {
     return configurations;
   }
 
  public Map<String, Object> getLdapConfig() { return ldapConfig; }

   public List<ChangedConfigInfo> getChangedConfigurations() {
     return changedConfigurations;
   }
@@ -189,6 +194,13 @@ public class StackAdvisorRequest {
       return this;
     }
 
    public StackAdvisorRequestBuilder withLdapConfig(Map<String, Object> ldapConfig) {
      Preconditions.checkNotNull(ldapConfig);
      this.instance.ldapConfig = ldapConfig;
      return this;
    }


     public StackAdvisorRequest build() {
       return this.instance;
     }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
index 356754d807..2dc45de226 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
@@ -84,6 +84,7 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
       + ",services/configurations/dependencies/StackConfigurationDependency/dependency_name"
       + ",services/configurations/dependencies/StackConfigurationDependency/dependency_type,services/configurations/StackConfigurations/type"
       + "&services/StackServices/service_name.in(%s)";
  private static final String GET_LDAP_CONFIG_URI = "/api/v1/configurations?AmbariConfiguration/type=ldap&fields=AmbariConfiguration/*";
   private static final String SERVICES_PROPERTY = "services";
   private static final String SERVICES_COMPONENTS_PROPERTY = "components";
   private static final String CONFIG_GROUPS_PROPERTY = "config-groups";
@@ -95,6 +96,7 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
   private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed-configurations";
   private static final String USER_CONTEXT_PROPERTY = "user-context";
   private static final String AMBARI_SERVER_CONFIGURATIONS_PROPERTY = "ambari-server-properties";
  protected static final String LDAP_CONFIGURATION_PROPERTY = "ldap-configuration";
 
   private File recommendationsDir;
   private String recommendationsArtifactsLifetime;
@@ -160,6 +162,7 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
       populateConfigurations(root, request);
       populateConfigGroups(root, request);
       populateAmbariServerInfo(root);
      populateLdapConfiguration(root);
       data.servicesJSON = mapper.writeValueAsString(root);
     } catch (Exception e) {
       // should not happen
@@ -171,6 +174,52 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
     return data;
   }
 
  /**
   * Retrieves the LDAP configuration if exists and adds it to services.json
   * @param root The JSON document that will become service.json when passed to the stack advisor engine
   * @throws StackAdvisorException
   * @throws IOException
   */
  protected void populateLdapConfiguration(ObjectNode root) throws StackAdvisorException, IOException {
    Response response = handleRequest(null, null, new LocalUriInfo(GET_LDAP_CONFIG_URI), Request.Type.GET,
        createConfigResource());

    if (response.getStatus() != Status.OK.getStatusCode()) {
      String message = String.format(
          "Error occured during retrieving ldap configuration, status=%s, response=%s",
          response.getStatus(), (String) response.getEntity());
      LOG.warn(message);
      throw new StackAdvisorException(message);
    }

    String ldapConfigJSON = (String) response.getEntity();
    if (LOG.isDebugEnabled()) {
      LOG.debug("LDAP configuration: {}", ldapConfigJSON);
    }

    JsonNode ldapConfigRoot = mapper.readTree(ldapConfigJSON);
    ArrayNode ldapConfigs = ((ArrayNode)ldapConfigRoot.get("items"));
    int numConfigs = ldapConfigs.size();
    // Zero or one config may exist
    switch (numConfigs) {
      case 0:
        LOG.debug("No LDAP config is stored in the DB");
        break;
      case 1:
        ArrayNode ldapConfigData = (ArrayNode)ldapConfigs.get(0).get("AmbariConfiguration").get("data");
        if (ldapConfigData.size() == 0) {
          throw new StackAdvisorException("No configuration data for LDAP configuration.");
        }
        if (ldapConfigData.size() > 1) {
          throw new StackAdvisorException("Ambigous configuration data for LDAP configuration.");
        }
        root.put(LDAP_CONFIGURATION_PROPERTY, ldapConfigData.get(0));
        break;
      default:
        throw new StackAdvisorException(String.format("Multiple (%s) LDAP configs are found in the DB.", numConfigs));
    }
  }

   protected void populateAmbariServerInfo(ObjectNode root) throws StackAdvisorException {
     Map<String, String> serverProperties = metaInfo.getAmbariServerProperties();
 
@@ -437,6 +486,11 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
     return createResource(Resource.Type.Host, mapIds);
   }
 
  protected ResourceInstance createConfigResource() {
    return createResource(Resource.Type.AmbariConfiguration, new HashMap<>());
  }


   private ResourceInstance createStackVersionResource(String stackName, String stackVersion) {
     Map<Resource.Type, String> mapIds = new HashMap<>();
     mapIds.put(Resource.Type.Stack, stackName);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java
index 8988be007b..6ceed4a780 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java
@@ -77,6 +77,7 @@ import org.apache.ambari.server.controller.internal.ViewPermissionResourceProvid
 import org.apache.ambari.server.controller.metrics.ThreadPoolEnabledPropertyProvider;
 import org.apache.ambari.server.controller.utilities.KerberosChecker;
 import org.apache.ambari.server.controller.utilities.KerberosIdentityCleaner;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.metrics.system.MetricsService;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.PersistenceType;
@@ -1061,7 +1062,7 @@ public class AmbariServer {
 
   public static void main(String[] args) throws Exception {
     logStartup();
    Injector injector = Guice.createInjector(new ControllerModule(), new AuditLoggerModule());
    Injector injector = Guice.createInjector(new ControllerModule(), new AuditLoggerModule(), new LdapModule());
 
     AmbariServer server = null;
     try {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
index dc97871ddc..1425e1bd32 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
@@ -63,6 +63,7 @@ import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.configuration.Configuration.ConnectionPoolType;
 import org.apache.ambari.server.configuration.Configuration.DatabaseType;
 import org.apache.ambari.server.controller.internal.AlertTargetResourceProvider;
import org.apache.ambari.server.controller.internal.AmbariConfigurationResourceProvider;
 import org.apache.ambari.server.controller.internal.ClusterStackVersionResourceProvider;
 import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
 import org.apache.ambari.server.controller.internal.CredentialResourceProvider;
@@ -470,6 +471,7 @@ public class ControllerModule extends AbstractModule {
         .implement(ResourceProvider.class, Names.named("credential"), CredentialResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("kerberosDescriptor"), KerberosDescriptorResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("upgrade"), UpgradeResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("ambariConfiguration"), AmbariConfigurationResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("clusterStackVersion"), ClusterStackVersionResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("alertTarget"), AlertTargetResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("viewInstance"), ViewInstanceResourceProvider.class)
@@ -508,6 +510,7 @@ public class ControllerModule extends AbstractModule {
     install(new FactoryModuleBuilder().implement(CollectionPersisterService.class, CsvFilePersisterService.class).build(CollectionPersisterServiceFactory.class));
 
     install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));

   }
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java
index a1987755f7..711ae10f7e 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java
@@ -22,6 +22,8 @@ package org.apache.ambari.server.controller;
 import java.util.Map;
 import java.util.Set;
 
import javax.inject.Named;

 import org.apache.ambari.server.controller.internal.AlertTargetResourceProvider;
 import org.apache.ambari.server.controller.internal.ClusterStackVersionResourceProvider;
 import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
@@ -30,18 +32,15 @@ import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.Resource.Type;
 import org.apache.ambari.server.controller.spi.ResourceProvider;
 
import com.google.inject.name.Named;
 
 public interface ResourceProviderFactory {
   @Named("host")
  ResourceProvider getHostResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);
  ResourceProvider getHostResourceProvider(Set<String> propertyIds, Map<Type, String> keyPropertyIds,
                                           AmbariManagementController managementController);
 
   @Named("hostComponent")
  ResourceProvider getHostComponentResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);
  ResourceProvider getHostComponentResourceProvider(Set<String> propertyIds, Map<Type, String> keyPropertyIds,
                                                    AmbariManagementController managementController);
 
   @Named("service")
   ResourceProvider getServiceResourceProvider(AmbariManagementController managementController);
@@ -50,9 +49,8 @@ public interface ResourceProviderFactory {
   ResourceProvider getComponentResourceProvider(AmbariManagementController managementController);
 
   @Named("member")
  ResourceProvider getMemberResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);
  ResourceProvider getMemberResourceProvider(Set<String> propertyIds, Map<Type, String> keyPropertyIds,
                                             AmbariManagementController managementController);
 
   @Named("hostKerberosIdentity")
   ResourceProvider getHostKerberosIdentityResourceProvider(AmbariManagementController managementController);
@@ -64,13 +62,15 @@ public interface ResourceProviderFactory {
   ResourceProvider getRepositoryVersionResourceProvider();
 
   @Named("kerberosDescriptor")
  ResourceProvider getKerberosDescriptorResourceProvider(AmbariManagementController managementController,
                                                         Set<String> propertyIds,
  ResourceProvider getKerberosDescriptorResourceProvider(AmbariManagementController managementController, Set<String> propertyIds,
                                                          Map<Resource.Type, String> keyPropertyIds);
 
   @Named("upgrade")
   UpgradeResourceProvider getUpgradeResourceProvider(AmbariManagementController managementController);
 
  @Named("ambariConfiguration")
  ResourceProvider getAmbariConfigurationResourceProvider();

   @Named("clusterStackVersion")
   ClusterStackVersionResourceProvider getClusterStackVersionResourceProvider(AmbariManagementController managementController);
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java
index a98ad46150..1dc0841d19 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java
@@ -254,6 +254,8 @@ public abstract class AbstractControllerResourceProvider extends AbstractAuthori
         return new ClusterKerberosDescriptorResourceProvider(managementController);
       case LoggingQuery:
         return new LoggingResourceProvider(propertyIds, keyPropertyIds, managementController);
      case AmbariConfiguration:
        return resourceProviderFactory.getAmbariConfigurationResourceProvider();
       case AlertTarget:
         return resourceProviderFactory.getAlertTargetResourceProvider();
       case ViewInstance:
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java
index 1cd2d10507..1501a0186a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractProviderModule.java
@@ -224,7 +224,7 @@ public abstract class AbstractProviderModule implements ProviderModule,
    * are going to work unless refactoring is complete.
    */
   @Inject
  AmbariManagementController managementController;
  protected AmbariManagementController managementController;
 
   @Inject
   TimelineMetricCacheProvider metricCacheProvider;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
new file mode 100644
index 0000000000..4f4cc7070d
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
@@ -0,0 +1,328 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.orm.entities.ConfigurationBaseEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for AmbariConfiguration resources.
 */
public class AmbariConfigurationResourceProvider extends AbstractAuthorizedResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationResourceProvider.class);
  private static final String DEFAULT_VERSION_TAG = "Default version";
  private static final Integer DEFAULT_VERSION = 1;

  /**
   * Resource property id constants.
   */
  public enum ResourcePropertyId {

    ID("AmbariConfiguration/id"),
    TYPE("AmbariConfiguration/type"),
    VERSION("AmbariConfiguration/version"),
    VERSION_TAG("AmbariConfiguration/version_tag"),
    DATA("AmbariConfiguration/data");

    private String propertyId;

    ResourcePropertyId(String propertyId) {
      this.propertyId = propertyId;
    }

    String getPropertyId() {
      return this.propertyId;
    }

    public static ResourcePropertyId fromString(String propertyIdStr) {
      ResourcePropertyId propertyIdFromStr = null;

      for (ResourcePropertyId id : ResourcePropertyId.values()) {
        if (id.getPropertyId().equals(propertyIdStr)) {
          propertyIdFromStr = id;
          break;
        }
      }

      if (propertyIdFromStr == null) {
        throw new IllegalArgumentException("Unsupported property type: " + propertyIdStr);
      }

      return propertyIdFromStr;

    }
  }

  private static Set<String> PROPERTIES = Sets.newHashSet(
    ResourcePropertyId.ID.getPropertyId(),
    ResourcePropertyId.TYPE.getPropertyId(),
    ResourcePropertyId.VERSION.getPropertyId(),
    ResourcePropertyId.VERSION_TAG.getPropertyId(),
    ResourcePropertyId.DATA.getPropertyId());

  private static Map<Resource.Type, String> PK_PROPERTY_MAP = Collections.unmodifiableMap(
    new HashMap<Resource.Type, String>() {{
      put(Resource.Type.AmbariConfiguration, ResourcePropertyId.ID.getPropertyId());
    }}
  );


  @Inject
  private AmbariConfigurationDAO ambariConfigurationDAO;

  @Inject
  private AmbariEventPublisher publisher;


  private Gson gson;

  @AssistedInject
  public AmbariConfigurationResourceProvider() {
    super(PROPERTIES, PK_PROPERTY_MAP);
    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION));

    gson = new GsonBuilder().create();
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return Sets.newHashSet(ResourcePropertyId.ID.getPropertyId());
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request) throws SystemException, UnsupportedPropertyException,
    ResourceAlreadyExistsException, NoSuchParentResourceException {

    LOGGER.info("Creating new ambari configuration resource ...");
    AmbariConfigurationEntity ambariConfigurationEntity = null;
    try {
      ambariConfigurationEntity = getEntityFromRequest(request);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage());
    }

    LOGGER.info("Persisting new ambari configuration: {} ", ambariConfigurationEntity);

    try {
      ambariConfigurationDAO.create(ambariConfigurationEntity);
    } catch (Exception e) {
      LOGGER.error("Failed to create resource", e);
      throw new ResourceAlreadyExistsException(e.getMessage());
    }

    // todo filter by configuration type
    // notify subscribers about the configuration changes
    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED,
      ambariConfigurationEntity.getId()));

    return getRequestStatus(null);
  }


  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Set<Resource> resources = Sets.newHashSet();

    // retrieves allconfigurations, filtering is done at a higher level
    List<AmbariConfigurationEntity> ambariConfigurationEntities = ambariConfigurationDAO.findAll();
    for (AmbariConfigurationEntity ambariConfigurationEntity : ambariConfigurationEntities) {
      try {
        resources.add(toResource(ambariConfigurationEntity, getPropertyIds()));
      } catch (AmbariException e) {
        LOGGER.error("Error while retrieving ambari configuration", e);
      }
    }
    return resources;
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Long idFromRequest = Long.valueOf((String) PredicateHelper.getProperties(predicate).get(ResourcePropertyId.ID.getPropertyId()));

    if (null == idFromRequest) {
      LOGGER.debug("No resource id provided in the request");
    } else {
      LOGGER.debug("Deleting amari configuration with id: {}", idFromRequest);
      try {
        ambariConfigurationDAO.removeByPK(idFromRequest);
      } catch (IllegalStateException e) {
        throw new NoSuchResourceException(e.getMessage());
      }

    }

    // notify subscribers about the configuration changes
    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED, idFromRequest));


    return getRequestStatus(null);

  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Long idFromRequest = Long.valueOf((String) PredicateHelper.getProperties(predicate).get(ResourcePropertyId.ID.getPropertyId()));

    AmbariConfigurationEntity persistedEntity = ambariConfigurationDAO.findByPK(idFromRequest);
    if (persistedEntity == null) {
      String errorMsg = String.format("Entity with primary key [ %s ] not found in the database.", idFromRequest);
      LOGGER.error(errorMsg);
      throw new NoSuchResourceException(errorMsg);
    }

    try {

      AmbariConfigurationEntity entityFromRequest = getEntityFromRequest(request);
      persistedEntity.getConfigurationBaseEntity().setVersionTag(entityFromRequest.getConfigurationBaseEntity().getVersionTag());
      persistedEntity.getConfigurationBaseEntity().setVersion(entityFromRequest.getConfigurationBaseEntity().getVersion());
      persistedEntity.getConfigurationBaseEntity().setType(entityFromRequest.getConfigurationBaseEntity().getType());
      persistedEntity.getConfigurationBaseEntity().setConfigurationData(entityFromRequest.getConfigurationBaseEntity().getConfigurationData());
      persistedEntity.getConfigurationBaseEntity().setConfigurationAttributes(entityFromRequest.getConfigurationBaseEntity().getConfigurationAttributes());


      ambariConfigurationDAO.update(persistedEntity);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage());
    }

    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED,
      persistedEntity.getId()));


    return getRequestStatus(null);

  }

  private Resource toResource(AmbariConfigurationEntity entity, Set<String> requestedIds) throws AmbariException {

    if (null == entity) {
      throw new IllegalArgumentException("Null entity can't be transformed into a resource");
    }

    if (null == entity.getConfigurationBaseEntity()) {
      throw new IllegalArgumentException("Invalid configuration entity can't be transformed into a resource");
    }
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    Set<Map<String, String>> configurationSet = gson.fromJson(entity.getConfigurationBaseEntity().getConfigurationData(), Set.class);

    setResourceProperty(resource, ResourcePropertyId.ID.getPropertyId(), entity.getId(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.TYPE.getPropertyId(), entity.getConfigurationBaseEntity().getType(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.DATA.getPropertyId(), configurationSet, requestedIds);
    setResourceProperty(resource, ResourcePropertyId.VERSION.getPropertyId(), entity.getConfigurationBaseEntity().getVersion(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.VERSION_TAG.getPropertyId(), entity.getConfigurationBaseEntity().getVersionTag(), requestedIds);

    return resource;
  }

  private AmbariConfigurationEntity getEntityFromRequest(Request request) throws AmbariException {

    AmbariConfigurationEntity ambariConfigurationEntity = new AmbariConfigurationEntity();
    ambariConfigurationEntity.setConfigurationBaseEntity(new ConfigurationBaseEntity());

    // set of resource properties (eache entry in the set belongs to a different resource)
    Set<Map<String, Object>> resourcePropertiesSet = request.getProperties();

    if (resourcePropertiesSet.size() != 1) {
      throw new AmbariException("There must be only one resource specified in the request");
    }

    // the configuration type must be set
    if (getValueFromResourceProperties(ResourcePropertyId.TYPE, resourcePropertiesSet.iterator().next()) == null) {
      throw new AmbariException("The configuration type must be set");
    }


    for (ResourcePropertyId resourcePropertyId : ResourcePropertyId.values()) {
      Object requestValue = getValueFromResourceProperties(resourcePropertyId, resourcePropertiesSet.iterator().next());

      switch (resourcePropertyId) {
        case DATA:
          if (requestValue == null) {
            throw new IllegalArgumentException("No configuration data is provided in the request");
          }
          ambariConfigurationEntity.getConfigurationBaseEntity().setConfigurationData(gson.toJson(requestValue));
          break;
        case TYPE:
          ambariConfigurationEntity.getConfigurationBaseEntity().setType((String) requestValue);
          break;
        case VERSION:
          Integer version = (requestValue == null) ? DEFAULT_VERSION : Integer.valueOf((String) requestValue);
          ambariConfigurationEntity.getConfigurationBaseEntity().setVersion((version));
          break;
        case VERSION_TAG:
          String versionTag = requestValue == null ? DEFAULT_VERSION_TAG : (String) requestValue;
          ambariConfigurationEntity.getConfigurationBaseEntity().setVersionTag(versionTag);
          break;
        default:
          LOGGER.debug("Ignored property in the request: {}", resourcePropertyId);
          break;
      }
    }
    ambariConfigurationEntity.getConfigurationBaseEntity().setCreateTimestamp(Calendar.getInstance().getTimeInMillis());
    return ambariConfigurationEntity;

  }

  private Object getValueFromResourceProperties(ResourcePropertyId resourcePropertyIdEnum, Map<String, Object> resourceProperties) {
    LOGGER.debug("Locating resource property [{}] in the resource properties map ...", resourcePropertyIdEnum);
    Object requestValue = null;

    if (resourceProperties.containsKey(resourcePropertyIdEnum.getPropertyId())) {
      requestValue = resourceProperties.get(resourcePropertyIdEnum.getPropertyId());
      LOGGER.debug("Found resource property {} in the resource properties map, value: {}", resourcePropertyIdEnum, requestValue);
    }
    return requestValue;
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/DefaultProviderModule.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/DefaultProviderModule.java
index 43779a3704..c3758b3f5e 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/DefaultProviderModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/DefaultProviderModule.java
@@ -21,20 +21,18 @@ package org.apache.ambari.server.controller.internal;
 import java.util.Map;
 import java.util.Set;
 
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.ResourceProvider;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 /**
  * The default provider module implementation.
  */
 public class DefaultProviderModule extends AbstractProviderModule {
  @Inject
  private AmbariManagementController managementController;

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProviderModule.class);
 
   // ----- Constructors ------------------------------------------------------
 
@@ -42,9 +40,7 @@ public class DefaultProviderModule extends AbstractProviderModule {
    * Create a default provider module.
    */
   public DefaultProviderModule() {
    if (managementController == null) {
      managementController = AmbariServer.getController();
    }
    super();
   }
 
 
@@ -52,8 +48,10 @@ public class DefaultProviderModule extends AbstractProviderModule {
 
   @Override
   protected ResourceProvider createResourceProvider(Resource.Type type) {
    Set<String>               propertyIds    = PropertyHelper.getPropertyIds(type);
    Map<Resource.Type,String> keyPropertyIds = PropertyHelper.getKeyPropertyIds(type);

    LOGGER.debug("Creating resource provider for the type: {}", type);
    Set<String> propertyIds = PropertyHelper.getPropertyIds(type);
    Map<Resource.Type, String> keyPropertyIds = PropertyHelper.getKeyPropertyIds(type);
 
     switch (type.getInternalType()) {
       case Workflow:
@@ -118,10 +116,10 @@ public class DefaultProviderModule extends AbstractProviderModule {
         return new ArtifactResourceProvider(managementController);
       case RemoteCluster:
         return new RemoteClusterResourceProvider();

       default:
        LOGGER.debug("Delegating creation of resource provider for: {} to the AbstractControllerResourceProvider", type.getInternalType());
         return AbstractControllerResourceProvider.getResourceProvider(type, propertyIds,
            keyPropertyIds, managementController);
          keyPropertyIds, managementController);
     }
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java
index 362b4e631c..78353735ac 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java
@@ -160,7 +160,8 @@ public interface Resource {
     VersionDefinition,
     ClusterKerberosDescriptor,
     LoggingQuery,
    RemoteCluster;
    RemoteCluster,
    AmbariConfiguration;
 
     /**
      * Get the {@link Type} that corresponds to this InternalType.
@@ -282,6 +283,8 @@ public interface Resource {
     public static final Type ClusterKerberosDescriptor = InternalType.ClusterKerberosDescriptor.getType();
     public static final Type LoggingQuery = InternalType.LoggingQuery.getType();
     public static final Type RemoteCluster = InternalType.RemoteCluster.getType();
    public static final Type AmbariConfiguration = InternalType.AmbariConfiguration.getType();

 
     /**
      * The type name.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java
index 9a5ee79913..0f9ff52147 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java
@@ -140,7 +140,13 @@ public abstract class AmbariEvent {
     /**
      * Local user has been created.
      */
    USER_CREATED;
    USER_CREATED,

    /**
     * LDAP config changed event;
     */
    LDAP_CONFIG_CHANGED;

   }
 
   /**
@@ -151,8 +157,7 @@ public abstract class AmbariEvent {
   /**
    * Constructor.
    *
   * @param eventType
   *          the type of event (not {@code null}).
   * @param eventType the type of event (not {@code null}).
    */
   public AmbariEvent(AmbariEventType eventType) {
     m_eventType = eventType;
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariLdapConfigChangedEvent.java b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariLdapConfigChangedEvent.java
new file mode 100644
index 0000000000..48799d793b
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariLdapConfigChangedEvent.java
@@ -0,0 +1,37 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

/**
 * Event signaling the creation or changing of an LDAP configuration entry.
 */
public class AmbariLdapConfigChangedEvent extends AmbariEvent {

  private Long configurationId;

  /**
   * Constructor.
   *
   * @param eventType the type of event (not {@code null}).
   */
  public AmbariLdapConfigChangedEvent(AmbariEventType eventType, Long configurationId) {
    super(eventType);
    this.configurationId = configurationId;
  }

  public Long getConfigurationId() {
    return configurationId;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/LdapModule.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/LdapModule.java
new file mode 100644
index 0000000000..089da1df7b
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/LdapModule.java
@@ -0,0 +1,82 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.ambari.server.ldap;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.ldap.service.AmbariLdapFacade;
import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.LdapConfigurationService;
import org.apache.ambari.server.ldap.service.LdapConnectionConfigService;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapConfigurationService;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapConnectionConfigService;
import org.apache.ambari.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupObjectClassDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserGroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserObjectClassDetector;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * GUICE configuration module for setting up LDAP related infrastructure.
 */
public class LdapModule extends AbstractModule {

  public static final String USER_ATTRIBUTES_DETECTORS = "UserAttributesDetectors";
  public static final String GROUP_ATTRIBUTES_DETECTORS = "GroupAttributesDetectors";

  @Override
  protected void configure() {
    bind(LdapFacade.class).to(AmbariLdapFacade.class);
    bind(LdapConfigurationService.class).to(DefaultLdapConfigurationService.class);
    bind(LdapAttributeDetectionService.class).to(DefaultLdapAttributeDetectionService.class);
    bind(LdapConnectionConfigService.class).to(DefaultLdapConnectionConfigService.class);

    // this binding requires the JPA module!
    bind(AmbariLdapConfiguration.class).toProvider(AmbariLdapConfigurationProvider.class);

    bind(AttributeDetectorFactory.class);

    install(new FactoryModuleBuilder().build(AmbariLdapConfigurationFactory.class));

    // binding the set of user attributes detector
    Multibinder<AttributeDetector> userAttributeDetectorBinder = Multibinder.newSetBinder(binder(), AttributeDetector.class,
      Names.named(USER_ATTRIBUTES_DETECTORS));
    userAttributeDetectorBinder.addBinding().to(UserObjectClassDetector.class);
    userAttributeDetectorBinder.addBinding().to(UserNameAttrDetector.class);
    userAttributeDetectorBinder.addBinding().to(UserGroupMemberAttrDetector.class);


    // binding the set of group attributes detector
    Multibinder<AttributeDetector> groupAttributeDetectorBinder = Multibinder.newSetBinder(binder(), AttributeDetector.class,
      Names.named(GROUP_ATTRIBUTES_DETECTORS));
    groupAttributeDetectorBinder.addBinding().to(GroupObjectClassDetector.class);
    groupAttributeDetectorBinder.addBinding().to(GroupNameAttrDetector.class);
    groupAttributeDetectorBinder.addBinding().to(GroupMemberAttrDetector.class);

  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigKeys.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigKeys.java
new file mode 100644
index 0000000000..da655adb57
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigKeys.java
@@ -0,0 +1,83 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.domain;

/**
 * Constants representing supported LDAP related property names
 * // todo extend this with validation information, description, defaults maybe
 */
public enum AmbariLdapConfigKeys {

  LDAP_ENABLED("ambari.ldap.authentication.enabled"),
  SERVER_HOST("ambari.ldap.connectivity.server.host"),
  SERVER_PORT("ambari.ldap.connectivity.server.port"),
  USE_SSL("ambari.ldap.connectivity.use_ssl"),

  TRUST_STORE("ambari.ldap.connectivity.trust_store"),
  TRUST_STORE_TYPE("ambari.ldap.connectivity.trust_store.type"),
  TRUST_STORE_PATH("ambari.ldap.connectivity.trust_store.path"),
  TRUST_STORE_PASSWORD("ambari.ldap.connectivity.trust_store.password"),
  ANONYMOUS_BIND("ambari.ldap.connectivity.anonymous_bind"),

  BIND_DN("ambari.ldap.connectivity.bind_dn"),
  BIND_PASSWORD("ambari.ldap.connectivity.bind_password"),

  ATTR_DETECTION("ambari.ldap.attributes.detection"), // manual | auto

  DN_ATTRIBUTE("ambari.ldap.attributes.dn_attr"),

  USER_OBJECT_CLASS("ambari.ldap.attributes.user.object_class"),
  USER_NAME_ATTRIBUTE("ambari.ldap.attributes.user.name_attr"),
  USER_GROUP_MEMBER_ATTRIBUTE("ambari.ldap.attributes.user.group_member_attr"),
  USER_SEARCH_BASE("ambari.ldap.attributes.user.search_base"),

  GROUP_OBJECT_CLASS("ambari.ldap.attributes.group.object_class"),
  GROUP_NAME_ATTRIBUTE("ambari.ldap.attributes.group.name_attr"),
  GROUP_MEMBER_ATTRIBUTE("ambari.ldap.attributes.group.member_attr"),
  GROUP_SEARCH_BASE("ambari.ldap.attributes.group.search_base"),

  USER_SEARCH_FILTER("ambari.ldap.advanced.user_search_filter"),
  USER_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.user_member_replace_pattern"),
  USER_MEMBER_FILTER("ambari.ldap.advanced.user_member_filter"),

  GROUP_SEARCH_FILTER("ambari.ldap.advanced.group_search_filter"),
  GROUP_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.group_member_replace_pattern"),
  GROUP_MEMBER_FILTER("ambari.ldap.advanced.group_member_filter"),

  FORCE_LOWERCASE_USERNAMES("ambari.ldap.advanced.force_lowercase_usernames"),
  REFERRAL_HANDLING("ambari.ldap.advanced.referrals"), // folow
  PAGINATION_ENABLED("ambari.ldap.advanced.pagination_enabled"); // true | false

  private String propertyName;

  AmbariLdapConfigKeys(String propName) {
    this.propertyName = propName;
  }

  public String key() {
    return this.propertyName;
  }

  public static AmbariLdapConfigKeys fromKeyStr(String keyStr) {
    for (AmbariLdapConfigKeys key : values()) {
      if (key.key().equals(keyStr)) {
        return key;
      }
    }

    throw new IllegalStateException("invalid konfiguration key found!");

  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
new file mode 100644
index 0000000000..8b26cd3e29
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
@@ -0,0 +1,199 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.ambari.server.ldap.domain;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * This class is an immutable representation of all the LDAP related configurationMap entries.
 */
public class AmbariLdapConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapConfiguration.class);

  private final Map<String, Object> configurationMap;

  private Object configValue(AmbariLdapConfigKeys ambariLdapConfigKeys) {
    Object value = null;
    if (configurationMap.containsKey(ambariLdapConfigKeys.key())) {
      value = configurationMap.get(ambariLdapConfigKeys.key());
    } else {
      LOGGER.warn("Ldap configuration property [{}] hasn't been set", ambariLdapConfigKeys.key());
    }
    return value;
  }

  public void setValueFor(AmbariLdapConfigKeys ambariLdapConfigKeys, Object value) {
    configurationMap.put(ambariLdapConfigKeys.key(), value);
  }

  // intentionally package private, instances to be created through the factory
  @Inject
  AmbariLdapConfiguration(@Assisted Map<String, Object> configuration) {
    this.configurationMap = configuration;
  }

  public boolean ldapEnabled() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.LDAP_ENABLED));
  }

  public String serverHost() {
    return (String) configValue(AmbariLdapConfigKeys.SERVER_HOST);
  }

  public int serverPort() {
    return Integer.valueOf((String) configValue(AmbariLdapConfigKeys.SERVER_PORT));
  }

  public boolean useSSL() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.USE_SSL));
  }

  public String trustStore() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE);
  }

  public String trustStoreType() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE_TYPE);
  }

  public String trustStorePath() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE_PATH);
  }

  public String trustStorePassword() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE_PASSWORD);
  }

  public boolean anonymousBind() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.ANONYMOUS_BIND));
  }

  public String bindDn() {
    return (String) configValue(AmbariLdapConfigKeys.BIND_DN);
  }

  public String bindPassword() {
    return (String) configValue(AmbariLdapConfigKeys.BIND_PASSWORD);
  }

  public String attributeDetection() {
    return (String) configValue(AmbariLdapConfigKeys.ATTR_DETECTION);
  }

  public String dnAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.DN_ATTRIBUTE);
  }

  public String userObjectClass() {
    return (String) configValue(AmbariLdapConfigKeys.USER_OBJECT_CLASS);
  }

  public String userNameAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE);
  }

  public String userSearchBase() {
    return (String) configValue(AmbariLdapConfigKeys.USER_SEARCH_BASE);
  }

  public String groupObjectClass() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_OBJECT_CLASS);
  }

  public String groupNameAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE);
  }

  public String groupMemberAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_MEMBER_ATTRIBUTE);
  }

  public String groupSearchBase() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_SEARCH_BASE);
  }

  public String userSearchFilter() {
    return (String) configValue(AmbariLdapConfigKeys.USER_SEARCH_FILTER);
  }

  public String userMemberReplacePattern() {
    return (String) configValue(AmbariLdapConfigKeys.USER_MEMBER_REPLACE_PATTERN);
  }

  public String userMemberFilter() {
    return (String) configValue(AmbariLdapConfigKeys.USER_MEMBER_FILTER);
  }

  public String groupSearchFilter() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_SEARCH_FILTER);
  }

  public String groupMemberReplacePattern() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_MEMBER_REPLACE_PATTERN);
  }

  public String groupMemberFilter() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_MEMBER_FILTER);
  }

  public boolean forceLowerCaseUserNames() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.FORCE_LOWERCASE_USERNAMES));
  }

  public boolean paginationEnabled() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.PAGINATION_ENABLED));
  }

  public String referralHandling() {
    return (String) configValue(AmbariLdapConfigKeys.REFERRAL_HANDLING);
  }


  @Override
  public String toString() {
    return configurationMap.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    AmbariLdapConfiguration that = (AmbariLdapConfiguration) o;

    return new EqualsBuilder()
      .append(configurationMap, that.configurationMap)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(configurationMap)
      .toHashCode();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java
new file mode 100644
index 0000000000..2b9f24be89
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java
@@ -0,0 +1,34 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.domain;

import java.util.Map;

/**
 * Factory interface for AmbariLdapConfiguration instances.
 * It's registered as a factory in the GUICE context (so no implementations required)
 *
 * To be extended with other factory methods upon needs.
 */
public interface AmbariLdapConfigurationFactory {

  /**
   * Creates an AmbariLdapConfiguration instance with the provided map of configuration settings.
   *
   * @param configuration a map where keys are the configuration properties and values are the configuration values
   * @return an AmbariLdapConfiguration instance
   */
  AmbariLdapConfiguration createLdapConfiguration(Map<String, Object> configuration);
}
\ No newline at end of file
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
new file mode 100644
index 0000000000..c88d420e9a
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
@@ -0,0 +1,120 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Provider implementation for LDAP configurations.
 * It needs to be registered in the related GUICE module as a provider.
 * It's responsible for managing LDAP configurations in the application.
 * Whenever requested, this provider returns an AmbariLdapConfiguration which is always in sync with the persisted LDAP
 * configuration resource.
 *
 * The provider receives notifications on CRUD operations related to the persisted resource and reloads the cached
 * configuration instance accordingly.
 */
@Singleton
public class AmbariLdapConfigurationProvider implements Provider<AmbariLdapConfiguration> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapAuthenticationProvider.class);
  private AmbariLdapConfiguration instance;

  @Inject
  private AmbariEventPublisher publisher;

  @Inject
  private Provider<AmbariConfigurationDAO> ambariConfigurationDAOProvider;

  @Inject
  private AmbariLdapConfigurationFactory ldapConfigurationFactory;

  private Gson gson = new GsonBuilder().create();

  @Inject
  public AmbariLdapConfigurationProvider() {
  }

  @Inject
  void register() {
    publisher.register(this);
  }

  @Override
  public AmbariLdapConfiguration get() {
    return instance != null ? instance : loadInstance(null);
  }

  /**
   * Loads the AmbariLdapConfiguration from the database.
   *
   * @param configurationId the configuration id
   * @return the AmbariLdapConfiguration instance
   */
  private AmbariLdapConfiguration loadInstance(Long configurationId) {
    AmbariConfigurationEntity configEntity = null;

    LOGGER.info("Loading LDAP configuration ...");
    if (null == configurationId) {

      LOGGER.debug("Initial loading of the ldap configuration ...");
      configEntity = ambariConfigurationDAOProvider.get().getLdapConfiguration();

    } else {

      LOGGER.debug("Reloading configuration based on the provied id: {}", configurationId);
      configEntity = ambariConfigurationDAOProvider.get().findByPK(configurationId);

    }

    if (configEntity != null) {
      Set propertyMaps = gson.fromJson(configEntity.getConfigurationBaseEntity().getConfigurationData(), Set.class);
      instance = ldapConfigurationFactory.createLdapConfiguration((Map<String, Object>) propertyMaps.iterator().next());
    }

    LOGGER.info("Loaded LDAP configuration instance: [ {} ]", instance);

    return instance;
  }

  // On changing the configuration, the provider gets updated with the fresh value
  @Subscribe
  public void ambariLdapConfigChanged(AmbariLdapConfigChangedEvent event) {
    LOGGER.info("LDAP config changed event received: {}", event);
    loadInstance(event.getConfigurationId());
    LOGGER.info("Refreshed LDAP config instance.");
  }


}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapException.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapException.java
new file mode 100644
index 0000000000..cb38accd2b
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapException.java
@@ -0,0 +1,33 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

public class AmbariLdapException extends Exception {
  public AmbariLdapException() {
    super();
  }

  public AmbariLdapException(String message) {
    super(message);
  }

  public AmbariLdapException(String message, Throwable cause) {
    super(message, cause);
  }

  public AmbariLdapException(Throwable cause) {
    super(cause);
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapFacade.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapFacade.java
new file mode 100644
index 0000000000..0118840b73
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapFacade.java
@@ -0,0 +1,140 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.ambari.server.ldap.service;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AmbariLdapFacade implements LdapFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapFacade.class);

  /**
   * Additional parameters expected to be provided along with the configuration
   */
  public enum Parameters {
    TEST_USER_NAME("ambari.ldap.test.user.name"),
    TEST_USER_PASSWORD("ambari.ldap.test.user.password");

    private String parameterKey;

    Parameters(String parameterKey) {
      this.parameterKey = parameterKey;
    }

    public String getParameterKey() {
      return parameterKey;
    }

  }

  @Inject
  private LdapConfigurationService ldapConfigurationService;

  @Inject
  private LdapAttributeDetectionService ldapAttributeDetectionService;

  @Inject
  public AmbariLdapFacade() {
  }

  @Override
  public void checkConnection(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      ldapConfigurationService.checkConnection(ambariLdapConfiguration);
      LOGGER.info("Validating LDAP connection related configuration: SUCCESS");

    } catch (Exception e) {

      LOGGER.error("Validating LDAP connection configuration failed", e);
      throw new AmbariLdapException(e);

    }

  }


  @Override
  public AmbariLdapConfiguration detectAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOGGER.info("Detecting LDAP configuration attributes ...");

    try {
      LOGGER.info("Detecting user attributes ....");
      // decorate the configuration with detected user attributes
      ambariLdapConfiguration = ldapAttributeDetectionService.detectLdapUserAttributes(ambariLdapConfiguration);

      LOGGER.info("Detecting group attributes ....");
      // decorate the configuration with detected group attributes
      ambariLdapConfiguration = ldapAttributeDetectionService.detectLdapGroupAttributes(ambariLdapConfiguration);

      LOGGER.info("Attribute detection finished.");
      return ambariLdapConfiguration;

    } catch (Exception e) {

      LOGGER.error("Error during LDAP attribute detection", e);
      throw new AmbariLdapException(e);

    }
  }

  @Override
  public Set<String> checkLdapAttributes(Map<String, Object> parameters, AmbariLdapConfiguration ldapConfiguration) throws AmbariLdapException {
    String userName = getTestUserNameFromParameters(parameters);
    String testUserPass = getTestUserPasswordFromParameters(parameters);

    if (null == userName) {
      throw new IllegalArgumentException("No test user available for testing LDAP attributes");
    }

    LOGGER.info("Testing LDAP user attributes with test user: {}", userName);
    String userDn = ldapConfigurationService.checkUserAttributes(userName, testUserPass, ldapConfiguration);

    // todo handle the case where group membership is stored in the user rather than the group
    LOGGER.info("Testing LDAP group attributes with test user dn: {}", userDn);
    Set<String> groups = ldapConfigurationService.checkGroupAttributes(userDn, ldapConfiguration);

    return groups;
  }


  private String getTestUserNameFromParameters(Map<String, Object> parameters) {
    return (String) parameterValue(parameters, Parameters.TEST_USER_NAME);
  }

  private String getTestUserPasswordFromParameters(Map<String, Object> parameters) {
    return (String) parameterValue(parameters, Parameters.TEST_USER_PASSWORD);
  }

  private Object parameterValue(Map<String, Object> parameters, Parameters parameter) {
    Object value = null;
    if (parameters.containsKey(parameter.getParameterKey())) {
      value = parameters.get(parameter.getParameterKey());
    } else {
      LOGGER.warn("Parameter [{}] is missing from parameters", parameter.getParameterKey());
    }
    return value;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AttributeDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AttributeDetector.java
new file mode 100644
index 0000000000..f39a1fd601
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AttributeDetector.java
@@ -0,0 +1,41 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

import java.util.Map;

/**
 * Operations for detecting LDAP related settings.
 * The basis for the attribute or value detection is a set of entries returned by a search operation.
 * Individual attribute detector implementations are responsible for detecting a specific set of attributes or values
 */
public interface AttributeDetector<T> {

  /**
   * Collects potential attribute names or values from a set of result entries.
   *
   * @param entry a result entry returned by a search operation
   */
  void collect(T entry);

  /**
   * Implements the decision based on which the "best" possible attribute or value is selected.
   *
   * @return a map of the form <property-key, detected-value>
   */
  Map<String, String> detect();


}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapAttributeDetectionService.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapAttributeDetectionService.java
new file mode 100644
index 0000000000..c08a2e0f26
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapAttributeDetectionService.java
@@ -0,0 +1,40 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;

/**
 * Contract defining operations to detect user and group attributes.
 */
public interface LdapAttributeDetectionService {

  /**
   * Decorates the passed in configuration with the detected ldap user attribute values
   *
   * @param ambariLdapConfiguration configuration instance holding connection details
   * @return the configuration decorated with user related attributes
   */
  AmbariLdapConfiguration detectLdapUserAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;

  /**
   * Decorates the passed in configuration with the detected ldap group attribute values
   *
   * @param ambariLdapConfiguration configuration instance holding connection details
   * @return the configuration decorated with group related attributes
   */
  AmbariLdapConfiguration detectLdapGroupAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;
}

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConfigurationService.java
new file mode 100644
index 0000000000..4b82aa295f
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConfigurationService.java
@@ -0,0 +1,60 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;

/**
 * Collection of operations for validating ldap configuration.
 * It's intended to decouple implementations using different libraries.
 */
public interface LdapConfigurationService {

  /**
   * Tests the connection based on the provided configuration.
   *
   * @param configuration the ambari ldap configuration instance
   * @throws AmbariLdapException if the connection is not possible
   */
  void checkConnection(AmbariLdapConfiguration configuration) throws AmbariLdapException;


  /**
   * Implements LDAP user related configuration settings validation logic.
   * Implementers communicate with the LDAP server (search, bind) to validate attributes in the provided configuration
   * instance
   *
   * @param testUserName  the test username
   * @param testPassword  the test password
   * @param configuration the available ldap configuration
   * @return The DN of the found user entry
   * @throws AmbariException if the connection couldn't be estabilisheds
   */
  String checkUserAttributes(String testUserName, String testPassword, AmbariLdapConfiguration configuration) throws AmbariLdapException;

  /**
   * Checks whether the group related LDAP attributes in the configuration are correct.
   *
   * @param userDn
   * @param ambariLdapConfiguration
   * @return
   * @throws AmbariLdapException
   */
  Set<String> checkGroupAttributes(String userDn, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConnectionConfigService.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConnectionConfigService.java
new file mode 100644
index 0000000000..a882075134
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapConnectionConfigService.java
@@ -0,0 +1,36 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;

/**
 * Contract for creating connection configuration instances.
 * Implementers are in charge for implementing any required custom logic based on the ambari configuration properties.
 * (Eg.: using custom key stores etc...)
 */
public interface LdapConnectionConfigService {

  /**
   * Creates and sets up an ldap connection configuration instance based on the provided ambari ldap configuration instance.
   *
   * @param ambariLdapConfiguration instance holding configuration values
   * @return a set up ldap connection configuration instance
   * @throws AmbariLdapException if an error occurs while setting up the connection configuration
   */
  LdapConnectionConfig createLdapConnectionConfig(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapFacade.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapFacade.java
new file mode 100644
index 0000000000..ef84d1bb2b
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/LdapFacade.java
@@ -0,0 +1,58 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;

/**
 * The contract defining all the operations required by the application when communicating with an arbitrary LDAP server.
 * This interface is intended to decouple LDAP specific details from the application.
 *
 * Any operation that requires interaction with an LDAP server from within Ambari should go through this interface.
 * (LDAP)
 */
public interface LdapFacade {

  /**
   * Tests the connection to the LDAP server based on the provided configuration.
   *
   * @param ambariLdapConfiguration the available ldap related configuration
   * @throws AmbariLdapException if the connection fails or other problems occur during the operation
   */
  void checkConnection(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;


  /**
   * Runs the user and group attribute detection algorithms.
   * The method is not intended to be used as a coniguration factory, the returned instance may not be suitable for use.
   *
   * @param ambariLdapConfiguration partially filled configuration instance to be extended with detected properties
   * @return a configuration instance, with properties filled with potentially correct values
   * @throws AmbariLdapException
   */
  AmbariLdapConfiguration detectAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;

  /**
   * Checks user and group related LDAP configuration attributes in the configuration object with the help of the provided parameters
   *
   * @param parameters              a map of property name and value pairs holding information to facilitate checking the attributes
   * @param ambariLdapConfiguration configutration instance with available attributes
   * @throws AmbariLdapException if the attribute checking fails
   */
  Set<String> checkLdapAttributes(Map<String, Object> parameters, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionService.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionService.java
new file mode 100644
index 0000000000..a9a9b539f2
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionService.java
@@ -0,0 +1,200 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service implementation that performs user and group attribute detection based on a sample set of entries returned by
 * an ldap search operation. A accuracy of detected values may depend on the size of the sample result set
 */
@Singleton
public class DefaultLdapAttributeDetectionService implements LdapAttributeDetectionService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLdapAttributeDetectionService.class);

  /**
   * The maximum size of the entry set the detection is performed on
   */
  private static final int SAMPLE_RESULT_SIZE = 50;

  @Inject
  private AttributeDetectorFactory attributeDetectorFactory;

  @Inject
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactory;

  @Inject
  public DefaultLdapAttributeDetectionService() {
  }

  @Override
  public AmbariLdapConfiguration detectLdapUserAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOG.info("Detecting LDAP user attributes ...");

    // perform a search using the user search base
    if (Strings.isEmpty(ambariLdapConfiguration.userSearchBase())) {
      LOG.warn("No user search base provided");
      return ambariLdapConfiguration;
    }

    try {

      LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(ambariLdapConfiguration);
      AttributeDetector<Entry> userAttributeDetector = attributeDetectorFactory.userAttributDetector();

      SearchRequest searchRequest = assembleUserSearchRequest(ldapConnectionTemplate, ambariLdapConfiguration);

      // do the search
      List<Entry> entries = ldapConnectionTemplate.search(searchRequest, getEntryMapper());

      for (Entry entry : entries) {
        LOG.info("Collecting user attribute information from the sample entry with dn: [{}]", entry.getDn());
        userAttributeDetector.collect(entry);
      }

      // select attributes based on the collected information
      Map<String, String> detectedUserAttributes = userAttributeDetector.detect();

      // setting the attributes into the configuration
      setDetectedAttributes(ambariLdapConfiguration, detectedUserAttributes);

      LOG.info("Decorated ambari ldap config : [{}]", ambariLdapConfiguration);

    } catch (Exception e) {

      LOG.error("Ldap operation failed while detecting user attributes", e);
      throw new AmbariLdapException(e);

    }

    return ambariLdapConfiguration;
  }


  @Override
  public AmbariLdapConfiguration detectLdapGroupAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOG.info("Detecting LDAP group attributes ...");

    // perform a search using the user search base
    if (Strings.isEmpty(ambariLdapConfiguration.groupSearchBase())) {
      LOG.warn("No group search base provided");
      return ambariLdapConfiguration;
    }

    try {

      LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(ambariLdapConfiguration);
      AttributeDetector<Entry> groupAttributeDetector = attributeDetectorFactory.groupAttributeDetector();

      SearchRequest searchRequest = assembleGroupSearchRequest(ldapConnectionTemplate, ambariLdapConfiguration);

      // do the search
      List<Entry> groupEntries = ldapConnectionTemplate.search(searchRequest, getEntryMapper());

      for (Entry groupEntry : groupEntries) {

        LOG.info("Collecting group attribute information from the sample entry with dn: [{}]", groupEntry.getDn());
        groupAttributeDetector.collect(groupEntry);

      }

      // select attributes based on the collected information
      Map<String, String> detectedGroupAttributes = groupAttributeDetector.detect();

      // setting the attributes into the configuration
      setDetectedAttributes(ambariLdapConfiguration, detectedGroupAttributes);

      LOG.info("Decorated ambari ldap config : [{}]", ambariLdapConfiguration);

    } catch (Exception e) {

      LOG.error("Ldap operation failed while detecting group attributes", e);
      throw new AmbariLdapException(e);

    }

    return ambariLdapConfiguration;
  }

  private void setDetectedAttributes(AmbariLdapConfiguration ambariLdapConfiguration, Map<String, String> detectedAttributes) {

    for (Map.Entry<String, String> detecteMapEntry : detectedAttributes.entrySet()) {
      LOG.info("Setting detected configuration value: [{}] - > [{}]", detecteMapEntry.getKey(), detecteMapEntry.getValue());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfigKeys.fromKeyStr(detecteMapEntry.getKey()), detecteMapEntry.getValue());
    }

  }

  private SearchRequest assembleUserSearchRequest(LdapConnectionTemplate ldapConnectionTemplate, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      SearchRequest req = ldapConnectionTemplate.newSearchRequest(ambariLdapConfiguration.userSearchBase(),
        FilterBuilder.present(ambariLdapConfiguration.dnAttribute()).toString(), SearchScope.SUBTREE);
      req.setSizeLimit(SAMPLE_RESULT_SIZE);

      return req;

    } catch (Exception e) {
      LOG.error("Could not assemble ldap search request", e);
      throw new AmbariLdapException(e);
    }
  }

  private SearchRequest assembleGroupSearchRequest(LdapConnectionTemplate ldapConnectionTemplate, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      SearchRequest req = ldapConnectionTemplate.newSearchRequest(ambariLdapConfiguration.groupSearchBase(),
        FilterBuilder.present(ambariLdapConfiguration.dnAttribute()).toString(), SearchScope.SUBTREE);
      req.setSizeLimit(SAMPLE_RESULT_SIZE);

      return req;

    } catch (Exception e) {
      LOG.error("Could not assemble ldap search request", e);
      throw new AmbariLdapException(e);
    }
  }

  public EntryMapper<Entry> getEntryMapper() {
    return new EntryMapper<Entry>() {
      @Override
      public Entry map(Entry entry) throws LdapException {
        return entry;
      }
    };
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationService.java
new file mode 100644
index 0000000000..3f6995c48a
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationService.java
@@ -0,0 +1,213 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapConfigurationService;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Implementation of the validation logic using the Apache Directory API.
 */
@Singleton
public class DefaultLdapConfigurationService implements LdapConfigurationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLdapConfigurationService.class);

  @Inject
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactory;

  @Inject
  public DefaultLdapConfigurationService() {
  }


  @Override
  public void checkConnection(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOGGER.info("Trying to connect to the LDAP server using provided configuration...");
    LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(ambariLdapConfiguration);

    // check if the connection from the connection pool of the template is connected
    Boolean isConnected = ldapConnectionTemplate.execute(new ConnectionCallback<Boolean>() {
      @Override
      public Boolean doWithConnection(LdapConnection connection) throws LdapException {
        return connection.isConnected() && connection.isAuthenticated();
      }
    });

    if (!isConnected) {
      LOGGER.error("Could not connect to the LDAP server");
      throw new AmbariLdapException("Could not connect to the LDAP server. Configuration: " + ambariLdapConfiguration);
    }

    LOGGER.info("Successfully conencted to the LDAP.");

  }

  /**
   * Checks the user attributes provided in the configuration instance by issuing a search for a (known) test user in the LDAP.
   * Attributes are considered correct if there is at least one entry found.
   *
   * Invalid attributes are signaled by throwing an exception.
   *
   * @param testUserName            the test username
   * @param testPassword            the test password
   * @param ambariLdapConfiguration the available LDAP configuration to be validated
   * @return the DN of the test user
   * @throws AmbariLdapException if an error occurs
   */
  @Override
  public String checkUserAttributes(String testUserName, String testPassword, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    String userDn;
    try {
      LOGGER.info("Checking user attributes for user [{}] ...", testUserName);

      // set up a filter based on the provided attributes
      String filter = FilterBuilder.and(
        FilterBuilder.equal(SchemaConstants.OBJECT_CLASS_AT, ambariLdapConfiguration.userObjectClass()),
        FilterBuilder.equal(ambariLdapConfiguration.userNameAttribute(), testUserName))
        .toString();

      LOGGER.info("Searching for the user: [{}] using the search filter: [{}]", testUserName, filter);
      userDn = ldapConnectionTemplateFactory.create(ambariLdapConfiguration).searchFirst(new Dn(ambariLdapConfiguration.userSearchBase()), filter, SearchScope.SUBTREE, getUserDnNameEntryMapper(ambariLdapConfiguration));

      if (null == userDn) {
        LOGGER.info("Could not find user based on the provided configuration. User attributes are not complete ");
        throw new AmbariLdapException("User attribute configuration incomplete");
      }
      LOGGER.info("Attribute validation succeeded. Filter: [{}]", filter);


    } catch (Exception e) {

      LOGGER.error("User attributes validation failed.", e);
      throw new AmbariLdapException(e.getMessage(), e);

    }
    return userDn;
  }

  /**
   * Checks whether the provided group related settings are correct.
   *
   * @param userDn                  a user DN to check
   * @param ambariLdapConfiguration the available LDAP configuration to be validated
   * @return
   * @throws AmbariLdapException
   */
  @Override
  public Set<String> checkGroupAttributes(String userDn, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    List<String> groups = Lists.newArrayList();
    try {
      LOGGER.info("Checking group attributes for user dn: [{}] ...", userDn);

      // set up a filter based on the provided attributes
      String filter = FilterBuilder.and(
        FilterBuilder.equal(SchemaConstants.OBJECT_CLASS_AT, ambariLdapConfiguration.groupObjectClass()),
        FilterBuilder.equal(ambariLdapConfiguration.groupMemberAttribute(), userDn)
      ).toString();

      LOGGER.info("Searching for the groups the user dn: [{}] is member of using the search filter: [{}]", userDn, filter);
      LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(ambariLdapConfiguration);

      // assemble a search request
      SearchRequest searchRequest = ldapConnectionTemplate.newSearchRequest(new Dn(ambariLdapConfiguration.groupSearchBase()), filter, SearchScope.SUBTREE);
      // attributes to be returned
      searchRequest.addAttributes(ambariLdapConfiguration.groupMemberAttribute(), ambariLdapConfiguration.groupNameAttribute());

      // perform the search
      groups = ldapConnectionTemplate.search(searchRequest, getGroupNameEntryMapper(ambariLdapConfiguration));

      if (groups == null || groups.isEmpty()) {
        LOGGER.info("No groups found for the user dn. Group attributes configuration is incomplete");
        throw new AmbariLdapException("Group attribute ldap configuration is incomplete");
      }

      LOGGER.info("Group attribute configuration check succeeded.");

    } catch (Exception e) {

      LOGGER.error("User attributes validation failed.", e);
      throw new AmbariLdapException(e.getMessage(), e);

    }

    return new HashSet<>(groups);
  }


  /**
   * Entry mapper for handling user search results.
   *
   * @param ambariLdapConfiguration ambari ldap configuration values
   * @return user dn entry mapper instance
   */
  private EntryMapper<String> getGroupNameEntryMapper(AmbariLdapConfiguration ambariLdapConfiguration) {

    EntryMapper<String> entryMapper = new EntryMapper<String>() {
      @Override
      public String map(Entry entry) throws LdapException {
        return entry.get(ambariLdapConfiguration.groupNameAttribute()).get().getString();
      }
    };

    return entryMapper;
  }

  /**
   * Entry mapper for handling group searches.
   *
   * @param ambariLdapConfiguration ambari ldap configuration values
   * @return
   */
  private EntryMapper<String> getUserDnNameEntryMapper(AmbariLdapConfiguration ambariLdapConfiguration) {

    EntryMapper<String> entryMapper = new EntryMapper<String>() {
      @Override
      public String map(Entry entry) throws LdapException {
        return entry.getDn().getNormName();
      }
    };

    return entryMapper;
  }


}



diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConnectionConfigService.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConnectionConfigService.java
new file mode 100644
index 0000000000..9afcf51a49
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConnectionConfigService.java
@@ -0,0 +1,113 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapConnectionConfigService;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultLdapConnectionConfigService implements LdapConnectionConfigService {

  private static Logger LOG = LoggerFactory.getLogger(DefaultLdapConnectionConfigService.class);

  @Inject
  public DefaultLdapConnectionConfigService() {
  }

  @Override
  public LdapConnectionConfig createLdapConnectionConfig(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {

    LOG.debug("Assembling ldap connection config based on: {}", ambariLdapConfiguration);

    LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(ambariLdapConfiguration.serverHost());
    config.setLdapPort(ambariLdapConfiguration.serverPort());
    config.setName(ambariLdapConfiguration.bindDn());
    config.setCredentials(ambariLdapConfiguration.bindPassword());
    config.setUseSsl(ambariLdapConfiguration.useSSL());

    if ("custom".equals(ambariLdapConfiguration.trustStore())) {
      LOG.info("Using custom trust manager configuration");
      config.setTrustManagers(trustManagers(ambariLdapConfiguration));
    }

    return config;
  }


  /**
   * Configure the trust managers to use the custom keystore.
   *
   * @param ambariLdapConfiguration congiguration instance holding current values
   * @return the array of trust managers
   * @throws AmbariLdapException if an error occurs while setting up the connection
   */
  private TrustManager[] trustManagers(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
      tmFactory.init(keyStore(ambariLdapConfiguration));
      return tmFactory.getTrustManagers();

    } catch (Exception e) {

      LOG.error("Failed to initialize trust managers", e);
      throw new AmbariLdapException(e);

    }

  }

  private KeyStore keyStore(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {

    // validating configuration settings
    if (Strings.isEmpty(ambariLdapConfiguration.trustStoreType())) {
      throw new AmbariLdapException("Key Store Type must be specified");
    }

    if (Strings.isEmpty(ambariLdapConfiguration.trustStorePath())) {
      throw new AmbariLdapException("Key Store Path must be specified");
    }

    try {

      KeyStore ks = KeyStore.getInstance(ambariLdapConfiguration.trustStoreType());
      FileInputStream fis = new FileInputStream(ambariLdapConfiguration.trustStorePath());
      ks.load(fis, ambariLdapConfiguration.trustStorePassword().toCharArray());
      return ks;

    } catch (Exception e) {

      LOG.error("Failed to create keystore", e);
      throw new AmbariLdapException(e);

    }
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java
new file mode 100644
index 0000000000..8467af08b6
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java
@@ -0,0 +1,111 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapConnectionConfigService;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Factory for creating LdapConnectionTemplate instances.
 * Depending on the usage context, the instance can be constructed based on the provided configuration or based on the persisted settings.
 */
@Singleton
public class LdapConnectionTemplateFactory {

  private static final Logger LOG = LoggerFactory.getLogger(LdapConnectionTemplateFactory.class);

  // Inject the persisted configuration (when available) check the provider implementation for details.
  @Inject
  private Provider<AmbariLdapConfiguration> ambariLdapConfigurationProvider;


  @Inject
  private LdapConnectionConfigService ldapConnectionConfigService;

  // cached instance that only changes when the underlying configuration changes.
  private LdapConnectionTemplate ldapConnectionTemplateInstance;


  @Inject
  public LdapConnectionTemplateFactory() {
  }

  /**
   * Creates a new instance based on the provided configuration. Use this factory method whle operating with ambari configuration not yet persisted.
   *
   * @param ambariLdapConfiguration ambari ldap configuration instance
   * @return an instance of LdapConnectionTemplate
   */
  public LdapConnectionTemplate create(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOG.info("Constructing new instance based on the provided ambari ldap configuration: {}", ambariLdapConfiguration);

    // create the connection config
    LdapConnectionConfig ldapConnectionConfig = ldapConnectionConfigService.createLdapConnectionConfig(ambariLdapConfiguration);

    // create the connection factory
    LdapConnectionFactory ldapConnectionFactory = new DefaultLdapConnectionFactory(ldapConnectionConfig);

    // create the connection pool
    LdapConnectionPool ldapConnectionPool = new LdapConnectionPool(new ValidatingPoolableLdapConnectionFactory(ldapConnectionFactory));

    LdapConnectionTemplate template = new LdapConnectionTemplate(ldapConnectionPool);
    LOG.info("Ldap connection template instance: {}", template);

    return template;

  }

  /**
   * Loads the persisted LDAP configuration.
   *
   * @return theh persisted
   */
  public LdapConnectionTemplate load() throws AmbariLdapException {

    if (null == ldapConnectionTemplateInstance) {
      ldapConnectionTemplateInstance = create(ambariLdapConfigurationProvider.get());
    }
    return ldapConnectionTemplateInstance;
  }

  /**
   * The returned connection template instance is recreated whenever the ambari ldap configuration changes
   *
   * @param event
   * @throws AmbariLdapException
   */
  @Subscribe
  public void onConfigChange(AmbariLdapConfigChangedEvent event) throws AmbariLdapException {
    ldapConnectionTemplateInstance = create(ambariLdapConfigurationProvider.get());
  }


}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/AttributeDetectorFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/AttributeDetectorFactory.java
new file mode 100644
index 0000000000..eba0bd9ba5
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/AttributeDetectorFactory.java
@@ -0,0 +1,75 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for attribute detector chains.
 */
@Singleton
public class AttributeDetectorFactory {

  private static final Logger LOG = LoggerFactory.getLogger(AttributeDetectorFactory.class);
  private static final String USER_ATTRIBUTES_DETECTORS = "UserAttributesDetectors";
  private static final String GROUP_ATTRIBUTES_DETECTORS = "GroupAttributesDetectors";
  /**
   * The set of group attribute detectors, configured by GUICE (check the relevant guice module implementation)
   */
  @Inject
  @Named(GROUP_ATTRIBUTES_DETECTORS)
  Set<AttributeDetector> groupAttributeDetectors;
  /**
   * The set of user attribute detectors, configured by GUICE (check the relevant guice module implementation)
   */
  @Inject
  @Named(USER_ATTRIBUTES_DETECTORS)
  private Set<AttributeDetector> userAttributeDetectors;

  @Inject
  public AttributeDetectorFactory() {
  }

  /**
   * Creates a chained attribute detector instance with user attribute detectors
   *
   * @return the constructed ChainedAttributeDetector instance
   */
  public ChainedAttributeDetector userAttributDetector() {
    LOG.info("Creating instance with user attribute detectors: [{}]", userAttributeDetectors);
    return new ChainedAttributeDetector(userAttributeDetectors);
  }

  /**
   * Creates a chained attribute detector instance with user attribute detectors
   *
   * @return the constructed ChainedAttributeDetector instance
   */

  public ChainedAttributeDetector groupAttributeDetector() {
    LOG.info("Creating instance with group attribute detectors: [{}]", groupAttributeDetectors);
    return new ChainedAttributeDetector(groupAttributeDetectors);
  }


}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/ChainedAttributeDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/ChainedAttributeDetector.java
new file mode 100644
index 0000000000..094922b21a
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/ChainedAttributeDetector.java
@@ -0,0 +1,73 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Attribute detector implementation that performs the attribute detection on a configured set of attribute detectors.
 * (it implements the composite design pattern)
 */
@Singleton
public class ChainedAttributeDetector implements AttributeDetector<Entry> {

  private static final Logger LOG = LoggerFactory.getLogger(ChainedAttributeDetector.class);

  /**
   * The set of detectors this instance delegates to
   */
  private final Set<AttributeDetector> detectors;

  @Inject
  public ChainedAttributeDetector(Set<AttributeDetector> detectors) {
    this.detectors = detectors;
  }

  @Override
  public void collect(Entry entry) {
    for (AttributeDetector detector : detectors) {
      LOG.info("Collecting information for the detector: [{}]", detector);
      detector.collect(entry);
    }
  }

  @Override
  public Map<String, String> detect() {
    Map<String, String> detectedAttributes = Maps.newHashMap();
    for (AttributeDetector detector : detectors) {
      LOG.info("Detecting ldap configuration value using the detector: [{}]", detector);
      detectedAttributes.putAll(detector.detect());
    }
    return detectedAttributes;
  }

  @Override
  public String toString() {
    return "ChainedAttributeDetector{" +
      "detectors=" + detectors +
      '}';
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetector.java
new file mode 100644
index 0000000000..8c34ef81aa
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetector.java
@@ -0,0 +1,65 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import javax.inject.Inject;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.directory.api.ldap.model.entry.Entry;

public class GroupMemberAttrDetector extends OccurrenceAndWeightBasedDetector {

  enum GroupMemberAttr {

    MEMBER("member", 1),
    MEMBER_UID("memberUid", 1),
    UNIQUE_MEMBER("uniqueMember", 1);

    private String attrName;
    private Integer weight;

    GroupMemberAttr(String attr, Integer weght) {
      this.attrName = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String attrName() {
      return this.attrName;
    }

  }

  @Inject
  public GroupMemberAttrDetector() {
    for (GroupMemberAttr groupMemberAttr : GroupMemberAttr.values()) {
      occurrenceMap().put(groupMemberAttr.attrName(), 0);
      weightsMap().put(groupMemberAttr.attrName(), groupMemberAttr.weight());
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    return entry.containsAttribute(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariLdapConfigKeys.GROUP_MEMBER_ATTRIBUTE.key();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupNameAttrDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupNameAttrDetector.java
new file mode 100644
index 0000000000..0315ef2b62
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupNameAttrDetector.java
@@ -0,0 +1,70 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import javax.inject.Inject;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupNameAttrDetector extends OccurrenceAndWeightBasedDetector {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserNameAttrDetector.class);

  private enum GroupNameAttr {

    DISTINGUISHED_NAME("distinguishedName", 1),

    CN("cn", 1);

    private String attrName;
    private Integer weight;

    GroupNameAttr(String attr, Integer weght) {
      this.attrName = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String attrName() {
      return this.attrName;
    }

  }

  @Inject
  public GroupNameAttrDetector() {

    for (GroupNameAttr groupNameAttr : GroupNameAttr.values()) {
      occurrenceMap().put(groupNameAttr.attrName(), 0);
      weightsMap().put(groupNameAttr.attrName(), groupNameAttr.weight());
    }
  }


  @Override
  protected boolean applies(Entry entry, String attribute) {
    return entry.containsAttribute(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE.key();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupObjectClassDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupObjectClassDetector.java
new file mode 100644
index 0000000000..b681134b5a
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupObjectClassDetector.java
@@ -0,0 +1,73 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import javax.inject.Inject;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupObjectClassDetector extends OccurrenceAndWeightBasedDetector {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroupObjectClassDetector.class);

  private enum ObjectClassValue {

    GROUP("group", 1),

    GROUP_OF_NAMES("groupOfNames", 1),

    POSIX_GROUP("posixGroup", 1),

    GROUP_OF_UNIQUE_NAMES("groupOfUniqueNames", 1);

    private String ocVal;
    private Integer weight;

    ObjectClassValue(String attr, Integer weght) {
      this.ocVal = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String ocVal() {
      return this.ocVal;
    }

  }

  @Inject
  public GroupObjectClassDetector() {
    for (ObjectClassValue ocVal : ObjectClassValue.values()) {
      occurrenceMap().put(ocVal.ocVal(), 0);
      weightsMap().put(ocVal.ocVal(), ocVal.weight());
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    return entry.hasObjectClass(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariLdapConfigKeys.GROUP_OBJECT_CLASS.key();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/OccurrenceAndWeightBasedDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/OccurrenceAndWeightBasedDetector.java
new file mode 100644
index 0000000000..6ce7ca6f73
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/OccurrenceAndWeightBasedDetector.java
@@ -0,0 +1,143 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import java.util.Map;

import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Attribute detector implementation that detects attributes considering their count of occurrence in a sample set of entries.
 * When multiple values are checked these values can be assigned a weight, that represents it's importance.
 */
public abstract class OccurrenceAndWeightBasedDetector implements AttributeDetector<Entry> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OccurrenceAndWeightBasedDetector.class);

  /**
   * A map in which the keys are the attributes that are checked in an entry and the values are the number the key occurs
   * in the sample entry set.
   */
  private Map<String, Integer> occurrenceMap = Maps.newHashMap();

  /**
   * A map in which the keys are the attributes that are checked in an entry and the values are the weight of the attribute.
   */
  private Map<String, Integer> weightsMap = Maps.newHashMap();

  protected Map<String, Integer> occurrenceMap() {
    return occurrenceMap;
  }

  protected Map<String, Integer> weightsMap() {
    return weightsMap;
  }


  /**
   * Checks whether the provided atribute is present in the entry.s
   *
   * @param entry     the entry being procesed
   * @param attribute the attribute being detected
   * @return true if the attribute is present, false otherwise
   */
  protected abstract boolean applies(Entry entry, String attribute);

  /**
   * The configuration key being detected.
   *
   * @return the key as a string
   */
  public abstract String detectedProperty();

  /**
   * Calculates the attribute value based on the two maps.
   *
   * @return a map with a single element, the key is the configuration key, the value is the detected attribute value
   */
  @Override
  public Map<String, String> detect() {
    LOGGER.info("Calculating the most probable attribute/value ...");
    Map<String, String> detectedMap = Maps.newHashMap();

    Map.Entry<String, Integer> selectedEntry = null;

    for (Map.Entry<String, Integer> entry : occurrenceMap().entrySet()) {
      if (selectedEntry == null) {

        selectedEntry = entry;
        LOGGER.debug("Initial attribute / value entry: {}", selectedEntry);
        continue;

      }

      if (selectedEntry.getValue() < entry.getValue()) {

        LOGGER.info("Changing potential attribute / value entry from : [{}] to: [{}]", selectedEntry, entry);
        selectedEntry = entry;

      }
    }

    // check whether the selected entry is valid (has occured in the sample result set)
    String detectedVal = "N/A";

    if (selectedEntry.getValue() > 0) {
      detectedVal = selectedEntry.getKey();
    } else {
      LOGGER.warn("Unable to detect attribute or attribute value");
    }

    LOGGER.info("Detected attribute or value: [{}]", detectedVal);
    detectedMap.put(detectedProperty(), detectedVal);
    return detectedMap;
  }


  /**
   * Collects the information about the attribute to be detected from the provided entry.
   *
   * @param entry a result entry returned by a search operation
   */
  @Override
  public void collect(Entry entry) {
    LOGGER.info("Collecting ldap attributes/values form entry with dn: [{}]", entry.getDn());

    for (String attributeValue : occurrenceMap().keySet()) {
      if (applies(entry, attributeValue)) {

        Integer cnt = occurrenceMap().get(attributeValue).intValue();
        if (weightsMap().containsKey(attributeValue)) {
          cnt = cnt + weightsMap().get(attributeValue);
        } else {
          cnt = cnt + 1;
        }
        occurrenceMap().put(attributeValue, cnt);

        LOGGER.info("Collected potential name attr: {}, count: {}", attributeValue, cnt);

      } else {
        LOGGER.info("The result entry doesn't contain the attribute: [{}]", attributeValue);
      }
    }
  }


}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserGroupMemberAttrDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserGroupMemberAttrDetector.java
new file mode 100644
index 0000000000..b34a2b2bfe
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserGroupMemberAttrDetector.java
@@ -0,0 +1,64 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import javax.inject.Inject;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.directory.api.ldap.model.entry.Entry;

public class UserGroupMemberAttrDetector extends OccurrenceAndWeightBasedDetector {

  private enum UserGroupMemberAttr {

    MEMBER_OF("memberOf", 1),
    IS_MEMBER_OF("ismemberOf", 1);

    private String attrName;
    private Integer weight;

    UserGroupMemberAttr(String attr, Integer weght) {
      this.attrName = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String attrName() {
      return this.attrName;
    }

  }

  @Inject
  public UserGroupMemberAttrDetector() {
    for (UserGroupMemberAttr userGroupMemberAttr : UserGroupMemberAttr.values()) {
      occurrenceMap().put(userGroupMemberAttr.attrName(), 0);
      weightsMap().put(userGroupMemberAttr.attrName(), userGroupMemberAttr.weight);
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    return entry.containsAttribute(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariLdapConfigKeys.USER_GROUP_MEMBER_ATTRIBUTE.key();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserNameAttrDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserNameAttrDetector.java
new file mode 100644
index 0000000000..dec445923a
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserNameAttrDetector.java
@@ -0,0 +1,71 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UserNameAttrDetector extends OccurrenceAndWeightBasedDetector {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserNameAttrDetector.class);

  private enum UserNameAttrs {
    SAM_ACCOUNT_NAME("sAMAccountName", 5),
    UID("uid", 3),
    CN("cn", 1);

    private String attrName;
    private Integer weight;

    UserNameAttrs(String attr, Integer weght) {
      this.attrName = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String attrName() {
      return this.attrName;
    }

  }

  @Inject
  public UserNameAttrDetector() {
    for (UserNameAttrs nameAttr : UserNameAttrs.values()) {
      occurrenceMap().put(nameAttr.attrName(), 0);
      weightsMap().put(nameAttr.attrName(), nameAttr.weight());
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    LOGGER.info("Checking for attribute  [{}] in entry [{}]", attribute, entry.getDn());
    return entry.containsAttribute(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key();
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserObjectClassDetector.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserObjectClassDetector.java
new file mode 100644
index 0000000000..bf2f5b8a76
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/detectors/UserObjectClassDetector.java
@@ -0,0 +1,69 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import javax.inject.Inject;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserObjectClassDetector extends OccurrenceAndWeightBasedDetector {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserObjectClassDetector.class);

  private enum ObjectClassValue {
    PERSON("person", 1),
    POSIX_ACCOUNT("posixAccount", 1);

    private String ocVal;
    private Integer weight;

    ObjectClassValue(String attr, Integer weght) {
      this.ocVal = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String ocVal() {
      return this.ocVal;
    }

  }

  @Inject
  public UserObjectClassDetector() {
    for (ObjectClassValue ocVal : ObjectClassValue.values()) {
      occurrenceMap().put(ocVal.ocVal(), 0);
      weightsMap().put(ocVal.ocVal(), ocVal.weight());
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    LOGGER.info("Checking for object class [{}] in entry [{}]", attribute, entry.getDn());
    return entry.hasObjectClass(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariLdapConfigKeys.USER_OBJECT_CLASS.key();
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java
new file mode 100644
index 0000000000..83293efb82
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java
@@ -0,0 +1,89 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.dao;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

/**
 * DAO dealing with ambari configuration related JPA operations.
 * Operations delegate to the JPA provider implementation of CRUD operations.
 */

@Singleton
public class AmbariConfigurationDAO extends CrudDAO<AmbariConfigurationEntity, Long> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationDAO.class);

  @Inject
  public AmbariConfigurationDAO() {
    super(AmbariConfigurationEntity.class);
  }

  @Transactional
  public void create(AmbariConfigurationEntity entity) {
    // make  sure only one LDAP config entry exists
    if ("ldap-configuration".equals(entity.getConfigurationBaseEntity().getType())) {
      AmbariConfigurationEntity ldapConfigEntity = getLdapConfiguration();
      if (ldapConfigEntity != null) {
        LOGGER.error("Only one LDAP configuration entry can exist!");
        throw new EntityExistsException("LDAP configuration entity already exists!");
      }
    }
    super.create(entity);
  }


  @Transactional
  public void update(AmbariConfigurationEntity entity) {
    if (entity.getId() == null || findByPK(entity.getId()) == null) {
      String msg = String.format("The entity with id [ %s ] is not found", entity.getId());
      LOGGER.debug(msg);
      throw new EntityNotFoundException(msg);
    }

    // updating the existing entity
    super.merge(entity);
    entityManagerProvider.get().flush();
  }

  /**
   * Returns the LDAP configuration from the database.
   *
   * @return the configuration entity
   */
  @Transactional
  public AmbariConfigurationEntity getLdapConfiguration() {
    LOGGER.info("Looking up the LDAP configuration ....");
    AmbariConfigurationEntity ldapConfigEntity = null;

    TypedQuery<AmbariConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
      "AmbariConfigurationEntity.findByType", AmbariConfigurationEntity.class);
    query.setParameter("typeName", "ldap-configuration");

    ldapConfigEntity = daoUtils.selectSingle(query);
    LOGGER.info("Returned entity: {} ", ldapConfigEntity);
    return ldapConfigEntity;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/DaoUtils.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/DaoUtils.java
index cd3faf087c..e6112ad05d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/DaoUtils.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/DaoUtils.java
@@ -18,8 +18,6 @@
 
 package org.apache.ambari.server.orm.dao;
 
import static org.apache.ambari.server.orm.DBAccessor.DbType;

 import java.util.Collections;
 import java.util.List;
 
@@ -31,19 +29,10 @@ import javax.persistence.criteria.CriteriaBuilder;
 import javax.persistence.criteria.CriteriaQuery;
 import javax.persistence.criteria.Root;
 
import org.apache.ambari.server.orm.DBAccessor;

import com.google.inject.Inject;
 import com.google.inject.Singleton;
 
 @Singleton
 public class DaoUtils {
  @Inject
  private DBAccessor dbAccessor;

  public DbType getDbType() {
    return dbAccessor.getDbType();
  }
 
   public <T> List<T> selectAll(EntityManager entityManager, Class<T> entityClass) {
     CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
@@ -92,7 +81,7 @@ public class DaoUtils {
 
   public void setParameters(Query query, Object... parameters) {
     for (int i = 0; i < parameters.length; i++) {
      query.setParameter(i+1, parameters[i]);
      query.setParameter(i + 1, parameters[i]);
     }
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java
new file mode 100644
index 0000000000..c9f4695469
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java
@@ -0,0 +1,70 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "ambari_configuration")
@NamedQueries({
  @NamedQuery(
    name = "AmbariConfigurationEntity.findByType",
    query = "select ace from AmbariConfigurationEntity ace where ace.configurationBaseEntity.type = :typeName")
})

public class AmbariConfigurationEntity {

  @Id
  @Column(name = "id")
  private Long id;

  @OneToOne(cascade = CascadeType.ALL)
  @MapsId
  @JoinColumn(name = "id")
  private ConfigurationBaseEntity configurationBaseEntity;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ConfigurationBaseEntity getConfigurationBaseEntity() {
    return configurationBaseEntity;
  }

  public void setConfigurationBaseEntity(ConfigurationBaseEntity configurationBaseEntity) {
    this.configurationBaseEntity = configurationBaseEntity;
  }

  @Override
  public String toString() {
    return "AmbariConfigurationEntity{" +
      "id=" + id +
      ", configurationBaseEntity=" + configurationBaseEntity +
      '}';
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java
new file mode 100644
index 0000000000..9ad30d7d1a
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java
@@ -0,0 +1,159 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Table(name = "configuration_base")
@TableGenerator(
  name = "configuration_id_generator",
  table = "ambari_sequences",
  pkColumnName = "sequence_name",
  valueColumnName = "sequence_value",
  pkColumnValue = "configuration_id_seq",
  initialValue = 1
)
@Entity
public class ConfigurationBaseEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "configuration_id_generator")
  private Long id;

  @Column(name = "version")
  private Integer version;

  @Column(name = "version_tag")
  private String versionTag;

  @Column(name = "type")
  private String type;

  @Column(name = "data")
  private String configurationData;

  @Column(name = "attributes")
  private String configurationAttributes;

  @Column(name = "create_timestamp")
  private Long createTimestamp;

  public Long getId() {
    return id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getConfigurationData() {
    return configurationData;
  }

  public void setConfigurationData(String configurationData) {
    this.configurationData = configurationData;
  }

  public String getConfigurationAttributes() {
    return configurationAttributes;
  }

  public void setConfigurationAttributes(String configurationAttributes) {
    this.configurationAttributes = configurationAttributes;
  }

  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long createTimestamp) {
    this.createTimestamp = createTimestamp;
  }

  @Override
  public String toString() {
    return "ConfigurationBaseEntity{" +
      "id=" + id +
      ", version=" + version +
      ", versionTag='" + versionTag + '\'' +
      ", type='" + type + '\'' +
      ", configurationData='" + configurationData + '\'' +
      ", configurationAttributes='" + configurationAttributes + '\'' +
      ", createTimestamp=" + createTimestamp +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    ConfigurationBaseEntity that = (ConfigurationBaseEntity) o;

    return new EqualsBuilder()
      .append(id, that.id)
      .append(version, that.version)
      .append(versionTag, that.versionTag)
      .append(type, that.type)
      .append(configurationData, that.configurationData)
      .append(configurationAttributes, that.configurationAttributes)
      .append(createTimestamp, that.createTimestamp)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(id)
      .append(version)
      .append(versionTag)
      .append(type)
      .append(configurationData)
      .append(configurationAttributes)
      .append(createTimestamp)
      .toHashCode();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java
index cd35c2c991..3c50628a55 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/RoleAuthorization.java
@@ -39,6 +39,7 @@ public enum RoleAuthorization {
   AMBARI_MANAGE_VIEWS("AMBARI.MANAGE_VIEWS"),
   AMBARI_RENAME_CLUSTER("AMBARI.RENAME_CLUSTER"),
   AMBARI_RUN_CUSTOM_COMMAND("AMBARI.RUN_CUSTOM_COMMAND"),
  AMBARI_MANAGE_CONFIGURATION("AMBARI.MANAGE_CONFIGURATION"),
   CLUSTER_MANAGE_CREDENTIALS("CLUSTER.MANAGE_CREDENTIALS"),
   CLUSTER_MODIFY_CONFIGS("CLUSTER.MODIFY_CONFIGS"),
   CLUSTER_MANAGE_CONFIG_GROUPS("CLUSTER.MANAGE_CONFIG_GROUPS"),
@@ -84,58 +85,58 @@ public enum RoleAuthorization {
   VIEW_USE("VIEW.USE");
 
   public static final Set<RoleAuthorization> AUTHORIZATIONS_VIEW_CLUSTER = EnumSet.of(
      CLUSTER_VIEW_STATUS_INFO,
      CLUSTER_VIEW_ALERTS,
      CLUSTER_VIEW_CONFIGS,
      CLUSTER_VIEW_METRICS,
      CLUSTER_VIEW_STACK_DETAILS,
      CLUSTER_MODIFY_CONFIGS,
      CLUSTER_MANAGE_CONFIG_GROUPS,
      CLUSTER_TOGGLE_ALERTS,
      CLUSTER_TOGGLE_KERBEROS,
      CLUSTER_UPGRADE_DOWNGRADE_STACK);
    CLUSTER_VIEW_STATUS_INFO,
    CLUSTER_VIEW_ALERTS,
    CLUSTER_VIEW_CONFIGS,
    CLUSTER_VIEW_METRICS,
    CLUSTER_VIEW_STACK_DETAILS,
    CLUSTER_MODIFY_CONFIGS,
    CLUSTER_MANAGE_CONFIG_GROUPS,
    CLUSTER_TOGGLE_ALERTS,
    CLUSTER_TOGGLE_KERBEROS,
    CLUSTER_UPGRADE_DOWNGRADE_STACK);
 
   public static final Set<RoleAuthorization> AUTHORIZATIONS_UPDATE_CLUSTER = EnumSet.of(
      CLUSTER_TOGGLE_ALERTS,
      CLUSTER_TOGGLE_KERBEROS,
      CLUSTER_UPGRADE_DOWNGRADE_STACK,
      CLUSTER_MODIFY_CONFIGS,
      CLUSTER_MANAGE_AUTO_START,
      SERVICE_MODIFY_CONFIGS);
    CLUSTER_TOGGLE_ALERTS,
    CLUSTER_TOGGLE_KERBEROS,
    CLUSTER_UPGRADE_DOWNGRADE_STACK,
    CLUSTER_MODIFY_CONFIGS,
    CLUSTER_MANAGE_AUTO_START,
    SERVICE_MODIFY_CONFIGS);
 
   public static final Set<RoleAuthorization> AUTHORIZATIONS_VIEW_SERVICE = EnumSet.of(
      SERVICE_VIEW_ALERTS,
      SERVICE_VIEW_CONFIGS,
      SERVICE_VIEW_METRICS,
      SERVICE_VIEW_STATUS_INFO,
      SERVICE_COMPARE_CONFIGS,
      SERVICE_ADD_DELETE_SERVICES,
      SERVICE_DECOMMISSION_RECOMMISSION,
      SERVICE_ENABLE_HA,
      SERVICE_MANAGE_CONFIG_GROUPS,
      SERVICE_MODIFY_CONFIGS,
      SERVICE_START_STOP,
      SERVICE_TOGGLE_MAINTENANCE,
      SERVICE_TOGGLE_ALERTS,
      SERVICE_MOVE,
      SERVICE_RUN_CUSTOM_COMMAND,
      SERVICE_RUN_SERVICE_CHECK);
    SERVICE_VIEW_ALERTS,
    SERVICE_VIEW_CONFIGS,
    SERVICE_VIEW_METRICS,
    SERVICE_VIEW_STATUS_INFO,
    SERVICE_COMPARE_CONFIGS,
    SERVICE_ADD_DELETE_SERVICES,
    SERVICE_DECOMMISSION_RECOMMISSION,
    SERVICE_ENABLE_HA,
    SERVICE_MANAGE_CONFIG_GROUPS,
    SERVICE_MODIFY_CONFIGS,
    SERVICE_START_STOP,
    SERVICE_TOGGLE_MAINTENANCE,
    SERVICE_TOGGLE_ALERTS,
    SERVICE_MOVE,
    SERVICE_RUN_CUSTOM_COMMAND,
    SERVICE_RUN_SERVICE_CHECK);
 
   public static final Set<RoleAuthorization> AUTHORIZATIONS_UPDATE_SERVICE = EnumSet.of(
      SERVICE_ADD_DELETE_SERVICES,
      SERVICE_DECOMMISSION_RECOMMISSION,
      SERVICE_ENABLE_HA,
      SERVICE_MANAGE_CONFIG_GROUPS,
      SERVICE_MODIFY_CONFIGS,
      SERVICE_START_STOP,
      SERVICE_TOGGLE_MAINTENANCE,
      SERVICE_TOGGLE_ALERTS,
      SERVICE_MOVE,
      SERVICE_RUN_CUSTOM_COMMAND,
      SERVICE_RUN_SERVICE_CHECK,
      SERVICE_MANAGE_ALERTS,
      SERVICE_MANAGE_AUTO_START,
      SERVICE_SET_SERVICE_USERS_GROUPS);
    SERVICE_ADD_DELETE_SERVICES,
    SERVICE_DECOMMISSION_RECOMMISSION,
    SERVICE_ENABLE_HA,
    SERVICE_MANAGE_CONFIG_GROUPS,
    SERVICE_MODIFY_CONFIGS,
    SERVICE_START_STOP,
    SERVICE_TOGGLE_MAINTENANCE,
    SERVICE_TOGGLE_ALERTS,
    SERVICE_MOVE,
    SERVICE_RUN_CUSTOM_COMMAND,
    SERVICE_RUN_SERVICE_CHECK,
    SERVICE_MANAGE_ALERTS,
    SERVICE_MANAGE_AUTO_START,
    SERVICE_SET_SERVICE_USERS_GROUPS);
 
   private final String id;
 
@@ -162,7 +163,7 @@ public enum RoleAuthorization {
   /**
    * Safely translates a role authorization Id to a RoleAuthorization
    *
   * @param authenticationId  an authentication id
   * @param authenticationId an authentication id
    * @return a RoleAuthorization or null if no translation can be made
    */
   public static RoleAuthorization translate(String authenticationId) {
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
index 015ec0a9e7..2b4d15cd37 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
@@ -84,6 +84,23 @@ CREATE TABLE clusterconfig (
   CONSTRAINT UQ_config_type_tag UNIQUE (version_tag, type_name, cluster_id),
   CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version));
 
CREATE TABLE configuration_base (
  id BIGINT NOT NULL,
  version_tag VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  type VARCHAR(255) NOT NULL,
  data VARCHAR(3000) NOT NULL,
  attributes VARCHAR(3000),
  create_timestamp BIGINT NOT NULL,
  CONSTRAINT PK_configuration_base PRIMARY KEY (id)
);

CREATE TABLE ambari_configuration (
  id BIGINT NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (id),
  CONSTRAINT FK_ambari_conf_conf_base FOREIGN KEY (id) REFERENCES configuration_base (id)
);

 CREATE TABLE serviceconfig (
   service_config_id BIGINT NOT NULL,
   cluster_id BIGINT NOT NULL,
@@ -1147,6 +1164,8 @@ INSERT INTO ambari_sequences (sequence_name, sequence_value)
   union all
   select 'servicecomponent_version_id_seq', 0 FROM SYSIBM.SYSDUMMY1
   union all
  select 'configuration_id_seq', 0 FROM SYSIBM.SYSDUMMY1
  union all
   select 'hostcomponentdesiredstate_id_seq', 0 FROM SYSIBM.SYSDUMMY1;
 
 
@@ -1247,6 +1266,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' FROM SYSIBM.SYSDUMMY1 UNION ALL
  SELECT 'AMBARI.MANAGE_CONFIGURATION', 'Manage ambari configurations' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.MANAGE_USERS', 'Manage users' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.MANAGE_GROUPS', 'Manage groups' FROM SYSIBM.SYSDUMMY1 UNION ALL
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' FROM SYSIBM.SYSDUMMY1 UNION ALL
@@ -1448,6 +1468,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_CONFIGURATION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_USERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR'  UNION ALL
diff --git a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
index eb9ca96465..b48720512e 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
@@ -104,6 +104,23 @@ CREATE TABLE clusterconfig (
   CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag),
   CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version));
 
CREATE TABLE configuration_base (
  id BIGINT NOT NULL,
  version_tag VARCHAR(100) NOT NULL,
  version BIGINT NOT NULL,
  type VARCHAR(100) NOT NULL,
  data LONGTEXT NOT NULL,
  attributes LONGTEXT,
  create_timestamp BIGINT NOT NULL,
  CONSTRAINT PK_configuration_base PRIMARY KEY (id)
);

CREATE TABLE ambari_configuration (
  id BIGINT NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (id),
  CONSTRAINT FK_ambari_conf_conf_base FOREIGN KEY (id) REFERENCES configuration_base (id)
);

 CREATE TABLE serviceconfig (
   service_config_id BIGINT NOT NULL,
   cluster_id BIGINT NOT NULL,
@@ -1111,6 +1128,7 @@ INSERT INTO ambari_sequences(sequence_name, sequence_value) VALUES
   ('remote_cluster_id_seq', 0),
   ('remote_cluster_service_id_seq', 0),
   ('servicecomponent_version_id_seq', 0),
  ('configuration_id_seq', 0),
   ('hostcomponentdesiredstate_id_seq', 0);
 
 INSERT INTO adminresourcetype (resource_type_id, resource_type_name) VALUES
@@ -1195,6 +1213,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage administrative settings' UNION ALL
  SELECT 'AMBARI.MANAGE_CONFIGURATION', 'Manage ambari configuration' UNION ALL
   SELECT 'AMBARI.MANAGE_USERS', 'Manage users' UNION ALL
   SELECT 'AMBARI.MANAGE_GROUPS', 'Manage groups' UNION ALL
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
@@ -1400,6 +1419,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_CONFIGURATION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_USERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
index dac3f28501..bb87618992 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
@@ -84,6 +84,23 @@ CREATE TABLE clusterconfig (
   CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag),
   CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version));
 
CREATE TABLE configuration_base (
  id NUMBER(19) NOT NULL,
  version_tag VARCHAR(255) NOT NULL,
  version NUMBER(19) NOT NULL,
  type VARCHAR(255) NOT NULL,
  data CLOB NOT NULL,
  attributes CLOB,
  create_timestamp NUMBER(19) NOT NULL,
  CONSTRAINT PK_configuration_base PRIMARY KEY (id)
);

CREATE TABLE ambari_configuration (
  id NUMBER(19) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (id),
  CONSTRAINT FK_ambari_conf_conf_base FOREIGN KEY (id) REFERENCES configuration_base (id)
);

 CREATE TABLE serviceconfig (
   service_config_id NUMBER(19) NOT NULL,
   cluster_id NUMBER(19) NOT NULL,
@@ -1090,6 +1107,7 @@ INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('ambari_oper
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_service_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('servicecomponent_version_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('configuration_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('hostcomponentdesiredstate_id_seq', 0);
 
 INSERT INTO metainfo("metainfo_key", "metainfo_value") values ('version', '${ambariSchemaVersion}');
@@ -1193,6 +1211,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' FROM dual UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' FROM dual UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' FROM dual UNION ALL
  SELECT 'AMBARI.MANAGE_CONFIGURATION', 'Manage ambari configuration' FROM dual UNION ALL
   SELECT 'AMBARI.MANAGE_USERS', 'Manage users' FROM dual UNION ALL
   SELECT 'AMBARI.MANAGE_GROUPS', 'Manage groups' FROM dual UNION ALL
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' FROM dual UNION ALL
@@ -1398,6 +1417,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_CONFIGURATION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_USERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
index c321a38d7c..7c0611d580 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
@@ -62,8 +62,26 @@ CREATE TABLE clusters (
   desired_cluster_state VARCHAR(255) NOT NULL,
   desired_stack_id BIGINT NOT NULL,
   CONSTRAINT PK_clusters PRIMARY KEY (cluster_id),
  CONSTRAINT FK_clusters_desired_stack_id FOREIGN KEY (desired_stack_id) REFERENCES stack(stack_id),
  CONSTRAINT FK_clusters_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource(resource_id));
  CONSTRAINT FK_clusters_desired_stack_id FOREIGN KEY (desired_stack_id) REFERENCES stack (stack_id),
  CONSTRAINT FK_clusters_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource (resource_id)
);

CREATE TABLE configuration_base (
  id BIGINT NOT NULL,
  version_tag VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  type VARCHAR(255) NOT NULL,
  data TEXT NOT NULL,
  attributes TEXT,
  create_timestamp BIGINT NOT NULL,
  CONSTRAINT PK_configuration_base PRIMARY KEY (id)
);

CREATE TABLE ambari_configuration (
  id BIGINT NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (id),
  CONSTRAINT FK_ambari_conf_conf_base FOREIGN KEY (id) REFERENCES configuration_base (id)
);
 
 CREATE TABLE clusterconfig (
   config_id BIGINT NOT NULL,
@@ -1091,6 +1109,7 @@ INSERT INTO ambari_sequences (sequence_name, sequence_value) VALUES
   ('remote_cluster_id_seq', 0),
   ('remote_cluster_service_id_seq', 0),
   ('servicecomponent_version_id_seq', 0),
  ('configuration_id_seq', 0),
   ('hostcomponentdesiredstate_id_seq', 0);
 
 INSERT INTO adminresourcetype (resource_type_id, resource_type_name) VALUES
@@ -1175,6 +1194,7 @@ INSERT INTO roleauthorization(authorization_id, authorization_name)
   SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
   SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
   SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage administrative settings' UNION ALL
  SELECT 'AMBARI.MANAGE_CONFIGURATION', 'Manage ambari configuration' UNION ALL
   SELECT 'AMBARI.MANAGE_USERS', 'Manage users' UNION ALL
   SELECT 'AMBARI.MANAGE_GROUPS', 'Manage groups' UNION ALL
   SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
@@ -1380,6 +1400,7 @@ INSERT INTO permission_roleauthorization(permission_id, authorization_id)
   SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
  SELECT permission_id, 'AMBARI.MANAGE_CONFIGURATION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_USERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
   SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
index 8740ed7fdd..e240c5a33e 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
@@ -83,6 +83,23 @@ CREATE TABLE clusterconfig (
   CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag),
   CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version));
 
CREATE TABLE configuration_base (
  id NUMERIC(19) NOT NULL,
  version_tag VARCHAR(255) NOT NULL,
  version NUMERIC(19) NOT NULL,
  type VARCHAR(255) NOT NULL,
  data TEXT NOT NULL,
  attributes TEXT,
  create_timestamp NUMERIC(19) NOT NULL,
  CONSTRAINT PK_configuration_base PRIMARY KEY (id)
);

CREATE TABLE ambari_configuration (
  id NUMERIC(19) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (id),
  CONSTRAINT FK_ambari_conf_conf_base FOREIGN KEY (id) REFERENCES configuration_base (id)
);

 CREATE TABLE serviceconfig (
   service_config_id NUMERIC(19) NOT NULL,
   cluster_id NUMERIC(19) NOT NULL,
@@ -1089,6 +1106,7 @@ INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_clus
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_service_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('servicecomponent_version_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('hostcomponentdesiredstate_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('configuration_id_seq', 0);
 
 insert into adminresourcetype (resource_type_id, resource_type_name)
   select 1, 'AMBARI'
@@ -1189,6 +1207,7 @@ insert into adminpermission(permission_id, permission_name, resource_type_id, pe
     SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
     SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
     SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' UNION ALL
    SELECT 'AMBARI.MANAGE_CONFIGURATION', 'Manage ambari configuration' UNION ALL
     SELECT 'AMBARI.MANAGE_USERS', 'Manage users' UNION ALL
     SELECT 'AMBARI.MANAGE_GROUPS', 'Manage groups' UNION ALL
     SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
@@ -1394,6 +1413,7 @@ insert into adminpermission(permission_id, permission_name, resource_type_id, pe
     SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'AMBARI.MANAGE_CONFIGURATION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_USERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
index 415589d3a8..3839ee4dda 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
@@ -97,6 +97,23 @@ CREATE TABLE clusterconfig (
   CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag),
   CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version));
 
CREATE TABLE configuration_base (
  id BIGINT NOT NULL,
  version_tag VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  type VARCHAR(255) NOT NULL,
  data VARCHAR(MAX) NOT NULL,
  attributes VARCHAR(MAX),
  create_timestamp BIGINT NOT NULL,
  CONSTRAINT PK_configuration_base PRIMARY KEY (id)
);

CREATE TABLE ambari_configuration (
  id BIGINT NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (id),
  CONSTRAINT FK_ambari_conf_conf_base FOREIGN KEY (id) REFERENCES configuration_base (id)
);

 CREATE TABLE serviceconfig (
   service_config_id BIGINT NOT NULL,
   cluster_id BIGINT NOT NULL,
@@ -1114,6 +1131,7 @@ BEGIN TRANSACTION
     ('remote_cluster_id_seq', 0),
     ('remote_cluster_service_id_seq', 0),
     ('servicecomponent_version_id_seq', 0),
    ('configuration_id_seq', 0),
     ('hostcomponentdesiredstate_id_seq', 0);
 
   insert into adminresourcetype (resource_type_id, resource_type_name)
@@ -1202,6 +1220,7 @@ BEGIN TRANSACTION
     SELECT 'AMBARI.ADD_DELETE_CLUSTERS', 'Create new clusters' UNION ALL
     SELECT 'AMBARI.RENAME_CLUSTER', 'Rename clusters' UNION ALL
     SELECT 'AMBARI.MANAGE_SETTINGS', 'Manage settings' UNION ALL
    SELECT 'AMBARI.MANAGE_CONFIGURATION', 'Manage ambari configuration' UNION ALL
     SELECT 'AMBARI.MANAGE_USERS', 'Manage users' UNION ALL
     SELECT 'AMBARI.MANAGE_GROUPS', 'Manage groups' UNION ALL
     SELECT 'AMBARI.MANAGE_VIEWS', 'Manage Ambari Views' UNION ALL
@@ -1407,6 +1426,7 @@ BEGIN TRANSACTION
     SELECT permission_id, 'AMBARI.ADD_DELETE_CLUSTERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.RENAME_CLUSTER' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_SETTINGS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
    SELECT permission_id, 'AMBARI.MANAGE_CONFIGURATION' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_USERS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_GROUPS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
     SELECT permission_id, 'AMBARI.MANAGE_VIEWS' FROM adminpermission WHERE permission_name='AMBARI.ADMINISTRATOR' UNION ALL
diff --git a/ambari-server/src/main/resources/META-INF/persistence.xml b/ambari-server/src/main/resources/META-INF/persistence.xml
index e4045ef536..0f8e964676 100644
-- a/ambari-server/src/main/resources/META-INF/persistence.xml
++ b/ambari-server/src/main/resources/META-INF/persistence.xml
@@ -96,6 +96,8 @@
     <class>org.apache.ambari.server.orm.entities.KerberosDescriptorEntity</class>
     <class>org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity</class>
     <class>org.apache.ambari.server.orm.entities.RemoteAmbariClusterServiceEntity</class>
    <class>org.apache.ambari.server.orm.entities.ConfigurationBaseEntity</class>
    <class>org.apache.ambari.server.orm.entities.AmbariConfigurationEntity</class>
 
     <properties>
       <property name="eclipselink.cache.size.default" value="10000" />
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java b/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java
index eaa471661d..959db1547b 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java
@@ -18,6 +18,7 @@
 
 package org.apache.ambari.server.api.services.stackadvisor.commands;
 
import static org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand.LDAP_CONFIGURATION_PROPERTY;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
@@ -33,12 +34,21 @@ import java.io.File;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Collections;
import java.util.HashMap;
 import java.util.Iterator;
import java.util.List;
 import java.util.Map;
 
 import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
 
import org.apache.ambari.server.api.resources.ResourceInstance;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.ResultStatus;
 import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
 import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
 import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
@@ -50,6 +60,7 @@ import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.commons.io.FileUtils;
 import org.codehaus.jackson.JsonNode;
 import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
 import org.codehaus.jackson.node.ArrayNode;
 import org.codehaus.jackson.node.ObjectNode;
 import org.junit.After;
@@ -59,6 +70,8 @@ import org.junit.rules.TemporaryFolder;
 import org.mockito.invocation.InvocationOnMock;
 import org.mockito.stubbing.Answer;
 
import com.google.common.collect.Lists;

 /**
  * StackAdvisorCommand unit tests.
  */
@@ -265,6 +278,197 @@ public class StackAdvisorCommandTest {
     assertEquals(0, stackVersions.size());
   }
 
  @Test
  public void testPopulateLdapConfig() throws Exception {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    TestStackAdvisorCommand command = spy(new TestStackAdvisorCommand(recommendationsDir, recommendationsArtifactsLifetime,
      ServiceInfo.ServiceAdvisorType.PYTHON, requestId, saRunner, metaInfo));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion").build();

    Map<String, Object> ldapConfigData = map(
      "authentication.ldap.primaryUrl", "localhost:33389",
      "authentication.ldap.secondaryUrl", "localhost:333",
      "authentication.ldap.baseDn", "c=ambari,dc=apache,dc=org"
    );

    Map<String, Object> storedLdapConfigResult =  map(
      "items",
      list(
        map(
          "AmbariConfiguration",
          map(
            "data", list(ldapConfigData)
          )
        )
      )
    );

    Response response =
      Response.status(ResultStatus.STATUS.OK.getStatus()).entity(jsonString(storedLdapConfigResult)).build();

    doReturn(response).when(command).handleRequest(any(), any(), any(), any(), any(), any());

    JsonNode servicesRootNode = json("{}");
    command.populateLdapConfiguration((ObjectNode)servicesRootNode);

    JsonNode expectedLdapConfig = json(
      map(LDAP_CONFIGURATION_PROPERTY, ldapConfigData)
    );

    assertEquals(expectedLdapConfig, servicesRootNode);
  }

  @Test
  public void testPopulateLdapConfig_NoConfigs() throws Exception {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    TestStackAdvisorCommand command = spy(new TestStackAdvisorCommand(recommendationsDir, recommendationsArtifactsLifetime,
      ServiceInfo.ServiceAdvisorType.PYTHON, requestId, saRunner, metaInfo));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion").build();

    Map<String, Object> storedLdapConfigResult =  map(
      "items", list()
    );

    Response response =
      Response.status(ResultStatus.STATUS.OK.getStatus()).entity(jsonString(storedLdapConfigResult)).build();

    doReturn(response).when(command).handleRequest(any(), any(), any(), any(), any(), any());

    JsonNode servicesRootNode = json("{}");
    command.populateLdapConfiguration((ObjectNode)servicesRootNode);

    JsonNode expectedLdapConfig = json("{}");

    assertEquals(expectedLdapConfig, servicesRootNode);
  }

  /**
   * An ambigous ldap config that has two items in its data[] array should result in exception
   */
  @Test(expected = StackAdvisorException.class)
  public void testPopulateLdapConfig_multipleConfigs() throws Exception {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    TestStackAdvisorCommand command = spy(new TestStackAdvisorCommand(recommendationsDir, recommendationsArtifactsLifetime,
        ServiceInfo.ServiceAdvisorType.PYTHON, requestId, saRunner, metaInfo));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion").build();

    Map<String, Object> ldapConfigData = map(
      "authentication.ldap.primaryUrl", "localhost:33389",
      "authentication.ldap.secondaryUrl", "localhost:333",
      "authentication.ldap.baseDn", "c=ambari,dc=apache,dc=org"
    );

    Map<String, Object> storedLdapConfigResult =  map(
      "items",
      list(
        map(
          "AmbariConfiguration",
          map(
            "data",
            list(ldapConfigData, ldapConfigData)
          )
        )
      )
    );

    Response response =
     Response.status(ResultStatus.STATUS.OK.getStatus()).entity(jsonString(storedLdapConfigResult)).build();

    doReturn(response).when(command).handleRequest(any(), any(), any(), any(), any(), any());

    JsonNode servicesRootNode = json("{}");
    command.populateLdapConfiguration((ObjectNode)servicesRootNode);
  }

  /**
   * An if multiple ambari configurations are stored with 'ldap-config' type, an
   * exception should be thrown
   */
  @Test(expected = StackAdvisorException.class)
  public void testPopulateLdapConfig_multipleResults() throws Exception {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    TestStackAdvisorCommand command = spy(new TestStackAdvisorCommand(recommendationsDir, recommendationsArtifactsLifetime,
      ServiceInfo.ServiceAdvisorType.PYTHON, requestId, saRunner, metaInfo));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
      .build();

    Map<String, Object> ldapConfig = map(
      "AmbariConfiguration",
      map(
        "data",
        list(
          map(
            "authentication.ldap.primaryUrl", "localhost:33389",
            "authentication.ldap.secondaryUrl", "localhost:333",
            "authentication.ldap.baseDn", "c=ambari,dc=apache,dc=org"
          )
        )
      )
    );

    Map<String, Object> storedLdapConfigResult = map(
      "items",
      list(ldapConfig, ldapConfig)
    );

    Response response =
      Response.status(ResultStatus.STATUS.OK.getStatus()).entity(jsonString(storedLdapConfigResult)).build();

    doReturn(response).when(command).handleRequest(any(), any(), any(), any(), any(), any());

    JsonNode servicesRootNode = json("{}");
    command.populateLdapConfiguration((ObjectNode)servicesRootNode);
  }

  private static String jsonString(Object obj) throws IOException {
    return new ObjectMapper().writeValueAsString(obj);
  }

  private static JsonNode json(Object obj) throws IOException {
    return new ObjectMapper().convertValue(obj, JsonNode.class);
  }

  private static JsonNode json(String jsonString) throws IOException {
    return new ObjectMapper().readTree(jsonString);
  }

  private static List<Object> list(Object... items) {
    return Lists.newArrayList(items);
  }

  private static Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> map = new HashMap<>();
    Iterator<Object> iterator = Arrays.asList(keysAndValues).iterator();
    while (iterator.hasNext()) {
      map.put(iterator.next().toString(), iterator.next());
    }
    return map;
  }

   class TestStackAdvisorCommand extends StackAdvisorCommand<TestResource> {
     public TestStackAdvisorCommand(File recommendationsDir, String recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType serviceAdvisorType,
                                    int requestId, StackAdvisorRunner saRunner, AmbariMetaInfo metaInfo) {
@@ -290,6 +494,14 @@ public class StackAdvisorCommandTest {
     protected TestResource updateResponse(StackAdvisorRequest request, TestResource response) {
       return response;
     }

    // Overridden to ensure visiblity in tests
    @Override
    public javax.ws.rs.core.Response handleRequest(HttpHeaders headers, String body,
                                                                  UriInfo uriInfo, Request.Type requestType,
                                                                  MediaType mediaType, ResourceInstance resource) {
      return super.handleRequest(headers, body, uriInfo, requestType, mediaType, resource);
    }
   }
 
   public static class TestResource extends StackAdvisorResponse {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/checks/UpgradeCheckOrderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/checks/UpgradeCheckOrderTest.java
index aa975e2ef6..0bc1584b95 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/checks/UpgradeCheckOrderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/checks/UpgradeCheckOrderTest.java
@@ -25,6 +25,7 @@ import java.util.Set;
 import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
 import org.junit.Assert;
 import org.junit.Test;
 import org.springframework.beans.factory.config.BeanDefinition;
@@ -54,7 +55,7 @@ public class UpgradeCheckOrderTest {
     properties.setProperty(Configuration.OS_VERSION.getKey(), "centos6");
     properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), sourceResourceDirectory);
 
    Injector injector = Guice.createInjector(new ControllerModule(properties), new AuditLoggerModule());
    Injector injector = Guice.createInjector(new ControllerModule(properties), new AuditLoggerModule(), new LdapModule());
     UpgradeCheckRegistry registry = injector.getInstance(UpgradeCheckRegistry.class);
     UpgradeCheckRegistry registry2 = injector.getInstance(UpgradeCheckRegistry.class);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
new file mode 100644
index 0000000000..c2a14218e7
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
@@ -0,0 +1,251 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.orm.entities.ConfigurationBaseEntity;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private Request requestMock;

  @Mock
  private AmbariConfigurationDAO ambariConfigurationDAO;

  @Mock
  private AmbariEventPublisher publisher;

  private Capture<AmbariConfigurationEntity> ambariConfigurationEntityCapture;

  private Gson gson;

  private static final String DATA_MOCK_STR = "[\n" +
    "      {\n" +
    "        \"authentication.ldap.baseDn\" : \"dc=ambari,dc=apache,dc=org\",\n" +
    "        \"authentication.ldap.primaryUrl\" : \"localhost:33389\",\n" +
    "        \"authentication.ldap.secondaryUrl\" : \"localhost:333\"\n" +
    "      }\n" +
    "    ]";

  private static final Long PK_LONG = Long.valueOf(1);
  private static final String PK_STRING = String.valueOf(1);
  private static final String VERSION_TAG = "test version";
  private static final String VERSION = "1";
  private static final String TYPE = "AmbariConfiguration";

  @TestSubject
  private AmbariConfigurationResourceProvider ambariConfigurationResourceProvider = new AmbariConfigurationResourceProvider();

  @Before
  public void setup() {
    ambariConfigurationEntityCapture = Capture.newInstance();
    gson = new GsonBuilder().create();
  }

  @Test
  public void testCreateAmbariConfigurationRequestResultsInTheProperPersistenceCall() throws Exception {

    // GIVEN
    // configuration properties parsed from the request
    Set<Map<String, Object>> resourcePropertiesSet = Sets.newHashSet(
      new PropertiesMapBuilder()
        .withId(PK_LONG)
        .withVersion(VERSION)
        .withVersionTag(VERSION_TAG)
        .withData(DATA_MOCK_STR)
        .withType(TYPE)
        .build());

    // mock the request to return the properties
    EasyMock.expect(requestMock.getProperties()).andReturn(resourcePropertiesSet);

    // capture the entity the DAO gets called with
    ambariConfigurationDAO.create(EasyMock.capture(ambariConfigurationEntityCapture));
    publisher.publish(EasyMock.anyObject(AmbariLdapConfigChangedEvent.class));

    replayAll();

    // WHEN
    ambariConfigurationResourceProvider.createResourcesAuthorized(requestMock);

    // THEN
    AmbariConfigurationEntity capturedAmbariConfigurationEntity = ambariConfigurationEntityCapture.getValue();
    Assert.assertNotNull(capturedAmbariConfigurationEntity);
    Assert.assertNull("The entity identifier should be null", capturedAmbariConfigurationEntity.getId());
    Assert.assertEquals("The entity version is not the expected", Integer.valueOf(VERSION),
      capturedAmbariConfigurationEntity.getConfigurationBaseEntity().getVersion());
    Assert.assertEquals("The entity version tag is not the expected", VERSION_TAG,
      capturedAmbariConfigurationEntity.getConfigurationBaseEntity().getVersionTag());
    Assert.assertEquals("The entity data is not the expected", DATA_MOCK_STR,
      gson.fromJson(capturedAmbariConfigurationEntity.getConfigurationBaseEntity().getConfigurationData(), String.class));
  }

  @Test
  public void testRemoveAmbariConfigurationRequestResultsInTheProperPersistenceCall() throws Exception {
    // GIVEN
    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();

    Capture<Long> pkCapture = Capture.newInstance();
    ambariConfigurationDAO.removeByPK(EasyMock.capture(pkCapture));
    publisher.publish(EasyMock.anyObject(AmbariLdapConfigChangedEvent.class));

    replayAll();

    // WHEN
    ambariConfigurationResourceProvider.deleteResourcesAuthorized(requestMock, predicate);

    // THEN
    Assert.assertEquals("The pk of the entity to be removed doen't match the expected id", Long.valueOf(1), pkCapture.getValue());
  }


  @Test
  public void testRetrieveAmbariConfigurationShouldResultsInTheProperDAOCall() throws Exception {
    // GIVEN
    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();

    EasyMock.expect(ambariConfigurationDAO.findAll()).andReturn(Lists.newArrayList(createDummyAmbariConfigurationEntity()));
    replayAll();

    // WHEN
    Set<Resource> resourceSet = ambariConfigurationResourceProvider.getResourcesAuthorized(requestMock, predicate);

    // THEN
    Assert.assertNotNull(resourceSet);
    Assert.assertFalse(resourceSet.isEmpty());
  }

  @Test
  public void testUpdateAmbariConfigurationShouldResultInTheProperDAOCalls() throws Exception {
    // GIVEN

    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();

    // properteies in the request, representing the updated configuration
    Set<Map<String, Object>> resourcePropertiesSet = Sets.newHashSet(new PropertiesMapBuilder()
      .withId(PK_LONG)
      .withVersion("2")
      .withVersionTag("version-2")
      .withData(DATA_MOCK_STR)
      .withType(TYPE)
      .build());

    EasyMock.expect(requestMock.getProperties()).andReturn(resourcePropertiesSet);

    AmbariConfigurationEntity persistedEntity = createDummyAmbariConfigurationEntity();
    EasyMock.expect(ambariConfigurationDAO.findByPK(PK_LONG)).andReturn(persistedEntity);
    ambariConfigurationDAO.update(EasyMock.capture(ambariConfigurationEntityCapture));
    publisher.publish(EasyMock.anyObject(AmbariLdapConfigChangedEvent.class));

    replayAll();

    // WHEN
    ambariConfigurationResourceProvider.updateResourcesAuthorized(requestMock, predicate);

    // the captured entity should be the updated one
    AmbariConfigurationEntity updatedEntity = ambariConfigurationEntityCapture.getValue();

    // THEN
    Assert.assertNotNull(updatedEntity);
    Assert.assertEquals("The updated version is wrong", Integer.valueOf(2), updatedEntity.getConfigurationBaseEntity().getVersion());
  }

  private class PropertiesMapBuilder {

    private Map<String, Object> resourcePropertiesMap = Maps.newHashMap();

    private PropertiesMapBuilder() {
    }

    public PropertiesMapBuilder withId(Long id) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId(), id);
      return this;
    }

    private PropertiesMapBuilder withVersion(String version) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.VERSION.getPropertyId(), version);
      return this;
    }

    private PropertiesMapBuilder withVersionTag(String versionTag) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.VERSION_TAG.getPropertyId(), versionTag);
      return this;
    }

    private PropertiesMapBuilder withData(String dataJson) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.DATA.getPropertyId(), dataJson);
      return this;
    }

    private PropertiesMapBuilder withType(String type) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.TYPE.getPropertyId(), type);
      return this;
    }


    public Map<String, Object> build() {
      return this.resourcePropertiesMap;
    }

  }

  private AmbariConfigurationEntity createDummyAmbariConfigurationEntity() {
    AmbariConfigurationEntity acEntity = new AmbariConfigurationEntity();
    ConfigurationBaseEntity configurationBaseEntity = new ConfigurationBaseEntity();
    acEntity.setConfigurationBaseEntity(configurationBaseEntity);
    acEntity.setId(PK_LONG);
    acEntity.getConfigurationBaseEntity().setConfigurationData(DATA_MOCK_STR);
    acEntity.getConfigurationBaseEntity().setVersion(Integer.valueOf(VERSION));
    acEntity.getConfigurationBaseEntity().setVersionTag(VERSION_TAG);
    acEntity.getConfigurationBaseEntity().setType("ldap-config");

    return acEntity;
  }


}
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackAdvisorResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackAdvisorResourceProviderTest.java
index ab60948b1b..05232eac49 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackAdvisorResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/StackAdvisorResourceProviderTest.java
@@ -27,43 +27,35 @@ import static org.junit.Assert.assertNotNull;
 import static org.mockito.Mockito.doReturn;
 import static org.mockito.Mockito.mock;
 
import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
import java.util.Iterator;
 import java.util.LinkedHashSet;
import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
import javax.annotation.Nonnull;

 import org.apache.ambari.server.controller.AmbariManagementController;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.junit.Assert;
import org.junit.Before;
 import org.junit.Test;
 
import com.google.common.collect.Lists;

 public class StackAdvisorResourceProviderTest {
 
  private RecommendationResourceProvider provider;

   @Test
   public void testCalculateConfigurations() throws Exception {

    Map<Resource.Type, String> keyPropertyIds = Collections.emptyMap();
    Set<String> propertyIds = Collections.emptySet();
    AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    RecommendationResourceProvider provider = new RecommendationResourceProvider(propertyIds,
        keyPropertyIds, ambariManagementController);

    Request request = mock(Request.class);
    Set<Map<String, Object>> propertiesSet = new HashSet<>();
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", "string");
    List<Object> array = new ArrayList<>();
    array.add("array1");
    array.add("array2");
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", array);
    propertiesSet.add(propertiesMap);

    doReturn(propertiesSet).when(request).getProperties();
    Request request = createMockRequest(
        CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", "string",
        CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", Lists.newArrayList("array1", "array2"));
 
     Map<String, Map<String, Map<String, String>>> calculatedConfigurations = provider.calculateConfigurations(request);
 
@@ -79,27 +71,37 @@ public class StackAdvisorResourceProviderTest {
     assertEquals("[array1, array2]", properties.get("array_prop"));
   }
 
  @Test
  public void testReadUserContext() throws Exception {

  @Nonnull
  private RecommendationResourceProvider createRecommendationResourceProvider() {
     Map<Resource.Type, String> keyPropertyIds = Collections.emptyMap();
     Set<String> propertyIds = Collections.emptySet();
     AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    RecommendationResourceProvider provider = new RecommendationResourceProvider(propertyIds,
                                                                                 keyPropertyIds, ambariManagementController);
    return new RecommendationResourceProvider(propertyIds,
        keyPropertyIds, ambariManagementController);
  }
 
  @Nonnull
  private Request createMockRequest(Object... propertyKeysAndValues) {
     Request request = mock(Request.class);
     Set<Map<String, Object>> propertiesSet = new HashSet<>();
     Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", "string");
    List<Object> array = new ArrayList<>();
    array.add("array1");
    array.add("array2");
    propertiesMap.put(USER_CONTEXT_OPERATION_PROPERTY, "op1");
    propertiesMap.put(USER_CONTEXT_OPERATION_DETAILS_PROPERTY, "op_det");
    Iterator<Object> it = Arrays.asList(propertyKeysAndValues).iterator();
    while(it.hasNext()) {
      String key = (String)it.next();
      Object value = it.next();
      propertiesMap.put(key, value);
    }
     propertiesSet.add(propertiesMap);

     doReturn(propertiesSet).when(request).getProperties();
    return request;
  }

  @Test
  public void testReadUserContext() throws Exception {
    Request request = createMockRequest(
        CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", "string",
        USER_CONTEXT_OPERATION_PROPERTY, "op1",
        USER_CONTEXT_OPERATION_DETAILS_PROPERTY, "op_det");
 
     Map<String, String> userContext = provider.readUserContext(request);
 
@@ -111,24 +113,9 @@ public class StackAdvisorResourceProviderTest {
 
   @Test
   public void testCalculateConfigurationsWithNullPropertyValues() throws Exception {

    Map<Resource.Type, String> keyPropertyIds = Collections.emptyMap();
    Set<String> propertyIds = Collections.emptySet();
    AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    RecommendationResourceProvider provider = new RecommendationResourceProvider(propertyIds,
      keyPropertyIds, ambariManagementController);

    Request request = mock(Request.class);
    Set<Map<String, Object>> propertiesSet = new HashSet<>();
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", null); //null value means no value specified for the property
    List<Object> array = new ArrayList<>();
    array.add("array1");
    array.add("array2");
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", array);
    propertiesSet.add(propertiesMap);

    doReturn(propertiesSet).when(request).getProperties();
    Request request = createMockRequest(
        CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", null,
        CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", Lists.newArrayList("array1", "array2"));
 
     Map<String, Map<String, Map<String, String>>> calculatedConfigurations = provider.calculateConfigurations(request);
 
@@ -142,19 +129,18 @@ public class StackAdvisorResourceProviderTest {
 
     assertEquals("[array1, array2]", properties.get("array_prop"));
 

     // config properties with null values should be ignored
     assertFalse(properties.containsKey("string_prop"));

   }
 
 
   @Test
   public void testStackAdvisorWithEmptyHosts() {
     Map<Resource.Type, String> keyPropertyIds = Collections.emptyMap();
     Set<String> propertyIds = Collections.emptySet();
     AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
     RecommendationResourceProvider provider = new RecommendationResourceProvider(propertyIds,
      keyPropertyIds, ambariManagementController);
            keyPropertyIds, ambariManagementController);
 
     Request request = mock(Request.class);
     Set<Map<String, Object>> propertiesSet = new HashSet<>();
@@ -170,4 +156,9 @@ public class StackAdvisorResourceProviderTest {
     } catch (Exception e) {
     }
   }

  @Before
  public void init() {
    provider = createRecommendationResourceProvider();
  }
 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java
new file mode 100644
index 0000000000..30f5e22ee0
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java
@@ -0,0 +1,149 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap;

import java.util.Map;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.domain.TestAmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.ads.LdapConnectionTemplateFactory;
import org.apache.ambari.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Test for the GUICE LdapModule setup
 *
 * - checks the module's bindings (can the GUICE context be created properely)
 * - checks for specific instances in the GUICE context (re they constructed properly, what is the instance' scope)
 *
 * It's named functional test as it creates a GUICE context. ("Real" unit tests only mock a class' collaborators, and
 * are more lightweight)
 *
 * By default the test is ignored, as it connects to external LDAP instances, thus in different environments may fail
 */
@Ignore
public class LdapModuleFunctionalTest {

  private static final Logger LOG = LoggerFactory.getLogger(LdapModuleFunctionalTest.class);
  private static Injector injector;


  @BeforeClass
  public static void beforeClass() throws Exception {

    // overriding bindings for testing purposes
    Module testModule = Modules.override(new LdapModule()).with(new AbstractModule() {
      @Override
      protected void configure() {
        // override the configuration instance binding not to access the database
        bind(AmbariLdapConfiguration.class).toInstance(new TestAmbariLdapConfigurationFactory().createLdapConfiguration(getADProps()));
      }
    });

    injector = Guice.createInjector(testModule);
  }

  @Test
  public void shouldLdapTemplateBeInstantiated() throws Exception {
    // GIVEN
    // the injector is set up
    Assert.assertNotNull(injector);

    // WHEN
    LdapConnectionTemplateFactory ldapConnectionTemplateFactory = injector.getInstance(LdapConnectionTemplateFactory.class);
    AmbariLdapConfigurationFactory ambariLdapConfigurationFactory = injector.getInstance(AmbariLdapConfigurationFactory.class);
    AmbariLdapConfiguration ldapConfiguration = ambariLdapConfigurationFactory.createLdapConfiguration(getADProps());
    LdapConnectionTemplate template = ldapConnectionTemplateFactory.create(ldapConfiguration);

    // THEN
    Assert.assertNotNull(template);
    //template.authenticate(new Dn("cn=read-only-admin,dc=example,dc=com"), "password".toCharArray());

    Boolean success = template.execute(new ConnectionCallback<Boolean>() {
      @Override
      public Boolean doWithConnection(LdapConnection connection) throws LdapException {

        return connection.isConnected() && connection.isAuthenticated();
      }
    });

    Assert.assertTrue("Could not bind to the LDAP server", success);

  }


  private static Map<String, Object> getProps() {
    Map<String, Object> ldapPropsMap = Maps.newHashMap();

    ldapPropsMap.put(AmbariLdapConfigKeys.ANONYMOUS_BIND.key(), "true");
    ldapPropsMap.put(AmbariLdapConfigKeys.SERVER_HOST.key(), "ldap.forumsys.com");
    ldapPropsMap.put(AmbariLdapConfigKeys.SERVER_PORT.key(), "389");
    ldapPropsMap.put(AmbariLdapConfigKeys.BIND_DN.key(), "cn=read-only-admin,dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfigKeys.BIND_PASSWORD.key(), "password");
//    ldapPropsMap.put(AmbariLdapConfigKeys.USE_SSL.key(), "true");

    ldapPropsMap.put(AmbariLdapConfigKeys.USER_OBJECT_CLASS.key(), SchemaConstants.PERSON_OC);
    ldapPropsMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), SchemaConstants.UID_AT);
    ldapPropsMap.put(AmbariLdapConfigKeys.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfigKeys.DN_ATTRIBUTE.key(), SchemaConstants.UID_AT);
//    ldapPropsMap.put(AmbariLdapConfigKeys.TRUST_STORE.key(), "custom");
    ldapPropsMap.put(AmbariLdapConfigKeys.TRUST_STORE_TYPE.key(), "JKS");
//    ldapPropsMap.put(AmbariLdapConfigKeys.TRUST_STORE_PATH.key(), "/Users/lpuskas/my_truststore/KeyStore.jks");


    return ldapPropsMap;
  }

  private static Map<String, Object> getADProps() {
    Map<String, Object> ldapPropsMap = Maps.newHashMap();



    return ldapPropsMap;
  }

  @Test
  public void testShouldDetectorsBeBound() throws Exception {
    // GIVEN

    // WHEN
    AttributeDetectorFactory f = injector.getInstance(AttributeDetectorFactory.class);

    // THEN
    Assert.assertNotNull(f);
    LOG.info(f.groupAttributeDetector().toString());
    LOG.info(f.userAttributDetector().toString());

  }
}
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java
new file mode 100644
index 0000000000..aa26498d92
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java
@@ -0,0 +1,29 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.domain;


import java.util.Map;

/**
 * Implementation used for testing purposes only!
 */
public class TestAmbariLdapConfigurationFactory implements AmbariLdapConfigurationFactory {

  @Override
  public AmbariLdapConfiguration createLdapConfiguration(Map<String, Object> configuration) {
    return new AmbariLdapConfiguration(configuration);
  }
}
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java
new file mode 100644
index 0000000000..db0e5a96ad
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java
@@ -0,0 +1,215 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.domain.TestAmbariLdapConfigurationFactory;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Unit test suite for the LdapFacade operations.
 */
public class AmbariLdapFacadeTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  public LdapConfigurationService ldapConfigurationServiceMock;

  @Mock(type = MockType.STRICT)
  public LdapAttributeDetectionService ldapAttributeDetectionServiceMock;

  private AmbariLdapConfigurationFactory ambariLdapConfigurationFactory;


  @TestSubject
  private LdapFacade ldapFacade = new AmbariLdapFacade();

  private AmbariLdapConfiguration ambariLdapConfiguration;


  private Capture<AmbariLdapConfiguration> ambariLdapConfigurationCapture;

  @Before
  public void before() {
    ambariLdapConfigurationFactory = new TestAmbariLdapConfigurationFactory();
    ambariLdapConfiguration = ambariLdapConfigurationFactory.createLdapConfiguration(Maps.newHashMap());
    ambariLdapConfigurationCapture = Capture.newInstance();


    resetAll();
  }

  /**
   * Tests whether the facade method call delegates to the proper service call.
   * The thest is success if the same instance is passed to the service.
   *
   * @throws Exception
   */
  @Test
  public void testShouldConfigurationCheckDelegateToTheRightServiceCall() throws Exception {
    // GIVEN
    // the mocks are set up
    ldapConfigurationServiceMock.checkConnection(EasyMock.capture(ambariLdapConfigurationCapture));
    replayAll();
    // WHEN
    // the facade method is called
    ldapFacade.checkConnection(ambariLdapConfiguration);

    // THEN
    // the captured configuration instance is the same the facade method got called with
    Assert.assertEquals("The configuration instance souldn't change before passing it to the service",
      ambariLdapConfiguration, ambariLdapConfigurationCapture.getValue());
  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldConfigurationCheckFailureResultInAmbariLdapException() throws Exception {
    // GIVEN
    ldapConfigurationServiceMock.checkConnection(EasyMock.anyObject(AmbariLdapConfiguration.class));
    EasyMock.expectLastCall().andThrow(new AmbariLdapException("Testing ..."));
    replayAll();

    // WHEN
    ldapFacade.checkConnection(ambariLdapConfiguration);

    // THEN
    // exception is thrown

  }

  @Test
  public void testShouldLdapAttributesCheckDelegateToTheRightServiceCalls() throws Exception {
    // GIVEN

    Map<String, Object> parameters = Maps.newHashMap();
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_NAME.getParameterKey(), "testUser");
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_PASSWORD.getParameterKey(), "testPassword");


    Capture<String> testUserCapture = Capture.newInstance();
    Capture<String> testPasswordCapture = Capture.newInstance();
    Capture<String> userDnCapture = Capture.newInstance();

    EasyMock.expect(ldapConfigurationServiceMock.checkUserAttributes(EasyMock.capture(testUserCapture), EasyMock.capture(testPasswordCapture),
      EasyMock.capture(ambariLdapConfigurationCapture))).andReturn("userDn");

    EasyMock.expect(ldapConfigurationServiceMock.checkGroupAttributes(EasyMock.capture(userDnCapture),
      EasyMock.capture(ambariLdapConfigurationCapture))).andReturn(Sets.newHashSet("userGroup"));

    replayAll();

    // WHEN
    Set<String> testUserGroups = ldapFacade.checkLdapAttributes(parameters, ambariLdapConfiguration);

    // THEN
    Assert.assertEquals("testUser", testUserCapture.getValue());
    Assert.assertEquals("testPassword", testPasswordCapture.getValue());
    Assert.assertEquals("userDn", userDnCapture.getValue());

    Assert.assertTrue(testUserGroups.contains("userGroup"));

  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldAttributeCheckFailuresResultInAmbariLdapException() throws Exception {
    // GIVEN
    Map<String, Object> parameters = Maps.newHashMap();
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_NAME.getParameterKey(), "testUser");
    parameters.put(AmbariLdapFacade.Parameters.TEST_USER_PASSWORD.getParameterKey(), "testPassword");

    EasyMock.expect(ldapConfigurationServiceMock.checkUserAttributes(EasyMock.anyString(), EasyMock.anyString(),
      EasyMock.anyObject(AmbariLdapConfiguration.class))).andThrow(new AmbariLdapException("Testing ..."));

    replayAll();

    // WHEN
    Set<String> testUserGroups = ldapFacade.checkLdapAttributes(parameters, ambariLdapConfiguration);
    // THEN
    // Exception is thrown
  }

  @Test
  public void testShouldLdapAttributeDetectionDelegateToTheRightServiceCalls() throws Exception {

    // configuration map with user attributes detected
    Map<String, Object> userConfigMap = Maps.newHashMap();
    userConfigMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), "uid");
    AmbariLdapConfiguration userAttrDecoratedConfig = ambariLdapConfigurationFactory.createLdapConfiguration(userConfigMap);

    // configuration map with user+group attributes detected
    Map<String, Object> groupConfigMap = Maps.newHashMap(userConfigMap);
    groupConfigMap.put(AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE.key(), "dn");
    AmbariLdapConfiguration groupAttrDecoratedConfig = ambariLdapConfigurationFactory.createLdapConfiguration(groupConfigMap);

    Capture<AmbariLdapConfiguration> userAttrDetectionConfigCapture = Capture.newInstance();
    Capture<AmbariLdapConfiguration> groupAttrDetectionConfigCapture = Capture.newInstance();

    // GIVEN
    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapUserAttributes(EasyMock.capture(userAttrDetectionConfigCapture)))
      .andReturn(userAttrDecoratedConfig);

    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapGroupAttributes(EasyMock.capture(groupAttrDetectionConfigCapture)))
      .andReturn(groupAttrDecoratedConfig);

    replayAll();

    // WHEN
    AmbariLdapConfiguration detected = ldapFacade.detectAttributes(ambariLdapConfiguration);

    // THEN
    Assert.assertEquals("User attribute detection called with the wrong configuration", ambariLdapConfiguration,
      userAttrDetectionConfigCapture.getValue());

    Assert.assertEquals("Group attribute detection called with the wrong configuration", userAttrDecoratedConfig,
      groupAttrDetectionConfigCapture.getValue());

    Assert.assertEquals("Attribute detection returned an invalid configuration", groupAttrDecoratedConfig, detected);

  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldAttributeDetectionFailuresResultInAmbariLdapException() throws Exception {
    // GIVEN
    EasyMock.expect(ldapAttributeDetectionServiceMock.detectLdapUserAttributes(EasyMock.anyObject(AmbariLdapConfiguration.class)))
      .andThrow(new AmbariLdapException("Testing ..."));

    replayAll();

    // WHEN
    ldapFacade.detectAttributes(ambariLdapConfiguration);

    // THEN
    // Exception is thrown
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java
new file mode 100644
index 0000000000..09dea1c210
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java
@@ -0,0 +1,188 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.domain.TestAmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.ads.detectors.AttributeDetectorFactory;
import org.apache.ambari.server.ldap.service.ads.detectors.ChainedAttributeDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserNameAttrDetector;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class DefaultLdapAttributeDetectionServiceTest extends EasyMockSupport {
  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private AttributeDetectorFactory attributeDetectorFactoryMock;

  @Mock
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactoryMock;

  @Mock
  private LdapConnectionTemplate ldapConnectionTemplateMock;

  @Mock
  private SearchRequest searchRequestMock;


  private AmbariLdapConfigurationFactory ldapConfigurationFactory = new TestAmbariLdapConfigurationFactory();

  @TestSubject
  private DefaultLdapAttributeDetectionService defaultLdapAttributeDetectionService = new DefaultLdapAttributeDetectionService();

  @Before
  public void before() {
    resetAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldLdapUserAttributeDetection() throws Exception {
    // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(AmbariLdapConfigKeys.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);

    List<Object> entryList = Lists.newArrayList(new DefaultEntry("uid=gauss"));

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(entryMapperMock().getClass())))
      .andReturn(entryList);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyString(), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(searchRequestMock);

    EasyMock.expect(attributeDetectorFactoryMock.userAttributDetector())
      .andReturn(new ChainedAttributeDetector(Sets.newHashSet(new UserNameAttrDetector())));

    EasyMock.expect(searchRequestMock.setSizeLimit(50)).andReturn(searchRequestMock);

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapUserAttributes(ldapConfiguration);

    // THEN
    Assert.assertNotNull(decorated);
    Assert.assertEquals("N/A", ldapConfiguration.userNameAttribute());
  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldUserAttributeDetectionFailWhenLdapOerationFails() throws Exception {
    // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(AmbariLdapConfigKeys.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andThrow(new AmbariLdapException("Testing ..."));

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapUserAttributes(ldapConfiguration);

    // THEN
    // exception is thrown

  }


  @Test
  @SuppressWarnings("unchecked")
  public void shouldLdapGroupAttributeDetection() throws Exception {
    // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(AmbariLdapConfigKeys.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);

    List<Object> entryList = Lists.newArrayList(new DefaultEntry("uid=gauss"));

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(entryMapperMock().getClass())))
      .andReturn(entryList);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyString(), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(searchRequestMock);

    EasyMock.expect(attributeDetectorFactoryMock.groupAttributeDetector())
      .andReturn(new ChainedAttributeDetector(Sets.newHashSet(new GroupMemberAttrDetector())));

    EasyMock.expect(searchRequestMock.setSizeLimit(50)).andReturn(searchRequestMock);

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapGroupAttributes(ldapConfiguration);

    // THEN
    Assert.assertNotNull(decorated);
    Assert.assertEquals("N/A", ldapConfiguration.groupMemberAttribute());
  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldGroupAttributeDetectionFailWhenLdapOerationFails() throws Exception {
    // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(AmbariLdapConfigKeys.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
    AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);

    EasyMock.expect(ldapConnectionTemplateFactoryMock.create(ldapConfiguration)).andThrow(new AmbariLdapException("Testing ..."));

    // WHEN
    replayAll();
    AmbariLdapConfiguration decorated = defaultLdapAttributeDetectionService.detectLdapGroupAttributes(ldapConfiguration);

    // THEN
    // exception is thrown

  }


  private EntryMapper<Entry> entryMapperMock() {
    return new EntryMapper<Entry>() {
      @Override
      public Entry map(Entry entry) throws LdapException {
        return null;
      }
    };
  }

}
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java
new file mode 100644
index 0000000000..4d6d2a6a50
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java
@@ -0,0 +1,221 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.TestAmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapConfigurationService;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DefaultLdapConfigurationServiceTest extends EasyMockSupport {
  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactory;

  @Mock(type = MockType.STRICT)
  private LdapConnectionTemplate ldapConnectionTemplateMock;


  @TestSubject
  private LdapConfigurationService ldapConfigurationService = new DefaultLdapConfigurationService();

  @Before
  public void before() {
    resetAll();
  }

  @Test
  public void testShouldConnectionCheckSucceedWhenConnectionCallbackSucceeds() throws Exception {
    // GIVEN
    AmbariLdapConfiguration ambariLdapConfiguration = new TestAmbariLdapConfigurationFactory().createLdapConfiguration(Maps.newHashMap());

    // the cllback returns TRUE
    EasyMock.expect(ldapConnectionTemplateMock.execute(EasyMock.anyObject(ConnectionCallback.class))).andReturn(Boolean.TRUE);
    EasyMock.expect(ldapConnectionTemplateFactory.create(ambariLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    replayAll();
    // WHEN
    ldapConfigurationService.checkConnection(ambariLdapConfiguration);

    // THEN
    // no exceptions are thrown

  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldConnectionCheckFailWhenConnectionCallbackFails() throws Exception {

    // GIVEN
    AmbariLdapConfiguration ambariLdapConfiguration = new TestAmbariLdapConfigurationFactory().createLdapConfiguration(Maps.newHashMap());

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateMock.execute(EasyMock.anyObject(ConnectionCallback.class))).andReturn(Boolean.FALSE);
    EasyMock.expect(ldapConnectionTemplateFactory.create(ambariLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    replayAll();
    // WHEN
    ldapConfigurationService.checkConnection(ambariLdapConfiguration);

    // THEN
    // exception is thrown

  }

  @Test
  public void testShouldUserAttributeConfigurationCheckSucceedWhenUserDnIsFound() throws Exception {
    // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(AmbariLdapConfigKeys.USER_OBJECT_CLASS.key(), "person");
    configMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), "uid");

    AmbariLdapConfiguration ambariLdapConfiguration = new TestAmbariLdapConfigurationFactory().createLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(ambariLdapConfiguration)).andReturn(ldapConnectionTemplateMock);
    // users found with dn
    EasyMock.expect(ldapConnectionTemplateMock.searchFirst(EasyMock.anyObject(Dn.class), EasyMock.anyString(), EasyMock.anyObject(SearchScope.class),
      EasyMock.anyObject(EntryMapper.class))).andReturn("dn");

    replayAll();
    // WHEN
    String userDn = ldapConfigurationService.checkUserAttributes("testUser", "testPassword", ambariLdapConfiguration);

    // THEN
    Assert.assertEquals("The found userDn is not the expected one", userDn, "dn");

  }

  @Test(expected = AmbariLdapException.class)
  public void testShouldUserAttributeConfigurationCheckFailWhenNoUsersFound() throws Exception {
    // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(AmbariLdapConfigKeys.USER_OBJECT_CLASS.key(), "posixAccount");
    configMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), "dn");

    AmbariLdapConfiguration ambariLdapConfiguration = new TestAmbariLdapConfigurationFactory().createLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(ambariLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    // no users found, the returned dn is null
    EasyMock.expect(ldapConnectionTemplateMock.searchFirst(EasyMock.anyObject(Dn.class), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class),
      EasyMock.anyObject(EntryMapper.class))).andReturn(null);

    replayAll();
    // WHEN
    String userDn = ldapConfigurationService.checkUserAttributes("testUser", "testPassword",
      ambariLdapConfiguration);

    // THEN
    Assert.assertEquals("The found userDn is not the expected one", userDn, "dn");

  }


  @Test
  public void testShouldGroupAttributeConfigurationCheckSucceedWhenGroupForUserDnIsFound() throws Exception {
    // GIVEN

    Map<String, Object> configMap = groupConfigObjectMap();

    SearchRequest sr = new SearchRequestImpl();

    AmbariLdapConfiguration ambariLdapConfiguration = new TestAmbariLdapConfigurationFactory().createLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(ambariLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyObject(Dn.class), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(sr);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(EntryMapper.class)))
      .andReturn(Lists.newArrayList("userGroup"));

    replayAll();
    // WHEN
    Set<String> userGroups = ldapConfigurationService.checkGroupAttributes("userDn", ambariLdapConfiguration);

    // THEN
    Assert.assertNotNull("No groups found", userGroups);

  }


  @Test(expected = AmbariLdapException.class)
  public void testShouldGroupAttributeConfigurationCheckFailWhenNoGroupsForUserDnFound() throws Exception {
    // GIVEN

    Map<String, Object> configMap = groupConfigObjectMap();

    SearchRequest sr = new SearchRequestImpl();

    AmbariLdapConfiguration ambariLdapConfiguration = new TestAmbariLdapConfigurationFactory().createLdapConfiguration(configMap);

    // the callback returns FALSE
    EasyMock.expect(ldapConnectionTemplateFactory.create(ambariLdapConfiguration)).andReturn(ldapConnectionTemplateMock);

    EasyMock.expect(ldapConnectionTemplateMock.newSearchRequest(EasyMock.anyObject(Dn.class), EasyMock.anyString(),
      EasyMock.anyObject(SearchScope.class))).andReturn(sr);

    EasyMock.expect(ldapConnectionTemplateMock.search(EasyMock.anyObject(SearchRequest.class), EasyMock.anyObject(EntryMapper.class)))
      .andReturn(Lists.newArrayList());

    replayAll();
    // WHEN
    Set<String> userGroups = ldapConfigurationService.checkGroupAttributes("userDn", ambariLdapConfiguration);

    // THEN
    Assert.assertNotNull("No groups found", userGroups);

  }

  private Map<String, Object> groupConfigObjectMap() {
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(AmbariLdapConfigKeys.GROUP_OBJECT_CLASS.key(), "groupOfNames");
    configMap.put(AmbariLdapConfigKeys.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
    configMap.put(AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE.key(), "uid");
    configMap.put(AmbariLdapConfigKeys.GROUP_MEMBER_ATTRIBUTE.key(), "member");
    return configMap;
  }


}
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetectorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetectorTest.java
new file mode 100644
index 0000000000..79af467a42
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/detectors/GroupMemberAttrDetectorTest.java
@@ -0,0 +1,107 @@
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Test suite for the attribute detector implementation
 */
public class GroupMemberAttrDetectorTest {

  private static final Logger LOG = LoggerFactory.getLogger(GroupMemberAttrDetector.class);

  @TestSubject
  GroupMemberAttrDetector groupMemberAttrDetector = new GroupMemberAttrDetector();

  @Test
  public void testShouldDetectAttributeBasedOnOccurrence() throws Exception {
    // GIVEN
    // Mimic a sample set of entries where group membership attributes  are different
    List<Entry> sampleEntryList = Lists.newArrayList();

    sampleEntryList.addAll(getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr.MEMBER_UID, 2));

    // this is the expected property to be detected as in the sample set the most entries have it
    sampleEntryList.addAll(getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr.UNIQUE_MEMBER, 7));
    sampleEntryList.addAll(getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr.MEMBER, 5));

    // WHEN
    for (Entry entry : sampleEntryList) {
      groupMemberAttrDetector.collect(entry);
    }

    // The most frequently encountered attribute will be selected
    Map<String, String> detectedAttributeMap = groupMemberAttrDetector.detect();

    // THEN
    Assert.assertEquals(1, detectedAttributeMap.size());
    Map.Entry<String, String> selectedEntry = detectedAttributeMap.entrySet().iterator().next();

    Assert.assertEquals("The selected configuration property is not the expected one", groupMemberAttrDetector.detectedProperty(), selectedEntry.getKey());
    Assert.assertEquals("The selected configuration property value is not the expected one", GroupMemberAttrDetector.GroupMemberAttr.UNIQUE_MEMBER.attrName(), selectedEntry.getValue());


  }

  @Test
  public void testShouldDetectorPassWhenEmptySampleSetProvided() throws Exception {
    // GIVEN
    List<Entry> sampleEntryList = Lists.newArrayList();

    // WHEN
    // WHEN
    for (Entry entry : sampleEntryList) {
      groupMemberAttrDetector.collect(entry);
    }

    Map<String, String> detectedAttributeMap = groupMemberAttrDetector.detect();
    // THEN
    Assert.assertEquals(1, detectedAttributeMap.size());
    Map.Entry<String, String> selectedEntry = detectedAttributeMap.entrySet().iterator().next();

    Assert.assertEquals("The selected configuration property is not the expected one", groupMemberAttrDetector.detectedProperty(), selectedEntry.getKey());
    Assert.assertEquals("The selected configuration property value is not the expected one", "N/A", selectedEntry.getValue());

  }

  private List<Entry> getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr member, int count) {
    List<Entry> entryList = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      Entry entry = new DefaultEntry();
      try {
        entry.setDn("dn=" + member.name() + "-" + i);
        entry.add(new DefaultAttribute(member.attrName(), new StringValue("xxx")));
        entryList.add(entry);
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }
    }
    return entryList;
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/notifications/DispatchFactoryTest.java b/ambari-server/src/test/java/org/apache/ambari/server/notifications/DispatchFactoryTest.java
index 382799c2c7..d34d732ea1 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/notifications/DispatchFactoryTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/notifications/DispatchFactoryTest.java
@@ -23,6 +23,7 @@ import java.util.Properties;
 import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.notifications.dispatchers.EmailDispatcher;
 import org.apache.ambari.server.notifications.dispatchers.SNMPDispatcher;
 import org.junit.Assert;
@@ -55,7 +56,7 @@ public class DispatchFactoryTest {
     properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(),sourceResourceDirectory);
     properties.setProperty(Configuration.ALERTS_SNMP_DISPATCH_UDP_PORT.getKey(),snmpPort.toString());
 
    Injector injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties));
    Injector injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties), new LdapModule());
     DispatchFactory dispatchFactory = injector.getInstance(DispatchFactory.class);
     DispatchFactory dispatchFactory2 = injector.getInstance(DispatchFactory.class);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/orm/InMemoryDefaultTestModule.java b/ambari-server/src/test/java/org/apache/ambari/server/orm/InMemoryDefaultTestModule.java
index 434a2a1e22..ebc25969c6 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/orm/InMemoryDefaultTestModule.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/orm/InMemoryDefaultTestModule.java
@@ -26,6 +26,7 @@ import java.util.concurrent.atomic.AtomicReference;
 import org.apache.ambari.server.audit.AuditLogger;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.stack.StackManager;
 import org.apache.ambari.server.stack.StackManagerFactory;
 import org.apache.ambari.server.stack.StackManagerMock;
@@ -122,6 +123,7 @@ public class InMemoryDefaultTestModule extends AbstractModule {
     }
 
     try {
      install(new LdapModule());
       install(Modules.override(new BeanDefinitionsCachingTestControllerModule(properties)).with(new AbstractModule() {
         @Override
         protected void configure() {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/orm/JdbcPropertyTest.java b/ambari-server/src/test/java/org/apache/ambari/server/orm/JdbcPropertyTest.java
index 427cede296..14c5dd68e5 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/orm/JdbcPropertyTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/orm/JdbcPropertyTest.java
@@ -23,6 +23,7 @@ import org.apache.ambari.server.H2DatabaseCleaner;
 import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.state.Clusters;
 import org.junit.After;
 import org.junit.Assert;
@@ -53,7 +54,7 @@ public class JdbcPropertyTest {
 
   @Test
   public void testNormal() throws Exception {
    injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties));
    injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties), new LdapModule());
     injector.getInstance(GuiceJpaInitializer.class);
 
     injector.getInstance(Clusters.class);
@@ -62,7 +63,7 @@ public class JdbcPropertyTest {
   @Test
   public void testJdbcProperty() throws Exception {
     properties.setProperty(Configuration.SERVER_JDBC_PROPERTIES_PREFIX + "shutdown", "true");
    injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties));
    injector = Guice.createInjector(new AuditLoggerModule(), new ControllerModule(properties), new LdapModule());
     injector.getInstance(GuiceJpaInitializer.class);
     try {
       injector.getInstance(Clusters.class);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java
index 442414f14d..566d6b7ff7 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderForDNWithSpaceTest.java
@@ -26,6 +26,7 @@ import org.apache.ambari.server.H2DatabaseCleaner;
 import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.dao.UserDAO;
 import org.apache.ambari.server.security.ClientSecurityType;
@@ -49,23 +50,23 @@ import com.google.inject.Injector;
 
 @RunWith(FrameworkRunner.class)
 @CreateDS(allowAnonAccess = true,
    name = "AmbariLdapAuthenticationProviderForDNWithSpaceTest",
    partitions = {
        @CreatePartition(name = "Root",
            suffix = "dc=the apache,dc=org",
            contextEntry = @ContextEntry(
                entryLdif =
                    "dn: dc=the apache,dc=org\n" +
                        "dc: the apache\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" +
                        "dn: dc=ambari,dc=the apache,dc=org\n" +
                        "dc: ambari\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"))
    })
  name = "AmbariLdapAuthenticationProviderForDNWithSpaceTest",
  partitions = {
    @CreatePartition(name = "Root",
      suffix = "dc=the apache,dc=org",
      contextEntry = @ContextEntry(
        entryLdif =
          "dn: dc=the apache,dc=org\n" +
            "dc: the apache\n" +
            "objectClass: top\n" +
            "objectClass: domain\n\n" +
            "dn: dc=ambari,dc=the apache,dc=org\n" +
            "dc: ambari\n" +
            "objectClass: top\n" +
            "objectClass: domain\n\n"))
  })
 @CreateLdapServer(allowAnonymousAccess = true,
    transports = {@CreateTransport(protocol = "LDAP")})
  transports = {@CreateTransport(protocol = "LDAP")})
 @ApplyLdifFiles("users_for_dn_with_space.ldif")
 public class AmbariLdapAuthenticationProviderForDNWithSpaceTest extends AmbariLdapAuthenticationProviderBaseTest {
 
@@ -83,7 +84,7 @@ public class AmbariLdapAuthenticationProviderForDNWithSpaceTest extends AmbariLd
 
   @Before
   public void setUp() throws Exception {
    injector = Guice.createInjector(new ControllerModule(getTestProperties()), new AuditLoggerModule());
    injector = Guice.createInjector(new ControllerModule(getTestProperties()), new AuditLoggerModule(), new LdapModule());
     injector.getInstance(GuiceJpaInitializer.class);
     injector.injectMembers(this);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java
index 4941bc7afb..d8be8097fd 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProviderTest.java
@@ -29,6 +29,7 @@ import static org.junit.Assert.fail;
 import org.apache.ambari.server.H2DatabaseCleaner;
 import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.dao.UserDAO;
 import org.apache.ambari.server.orm.entities.UserEntity;
@@ -90,7 +91,7 @@ public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticati
 
   @Before
   public void setUp() {
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule());
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule(), new LdapModule());
     injector.injectMembers(this);
     injector.getInstance(GuiceJpaInitializer.class);
     configuration.setClientSecurityType(ClientSecurityType.LDAP);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProviderTest.java
index 2362823b30..d889372857 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/AmbariLocalUserProviderTest.java
@@ -27,6 +27,7 @@ import static org.junit.Assert.assertTrue;
 
 import org.apache.ambari.server.H2DatabaseCleaner;
 import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.OrmTestHelper;
 import org.apache.ambari.server.orm.dao.UserDAO;
@@ -56,7 +57,7 @@ public class AmbariLocalUserProviderTest {
 
   @BeforeClass
   public static void prepareData() {
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule());
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule(), new LdapModule());
     injector.getInstance(GuiceJpaInitializer.class);
     injector.getInstance(OrmTestHelper.class).createTestUsers();
   }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/LdapServerPropertiesTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/LdapServerPropertiesTest.java
index 5747408954..0e1515bb5b 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/LdapServerPropertiesTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/authorization/LdapServerPropertiesTest.java
@@ -25,6 +25,7 @@ import java.util.List;
 
 import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.ldap.LdapModule;
 import org.junit.Before;
 import org.junit.Test;
 
@@ -45,13 +46,13 @@ public class LdapServerPropertiesTest {
   Configuration configuration;
 
   public LdapServerPropertiesTest() {
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule());
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule(), new LdapModule());
     injector.injectMembers(this);
   }
 
   @Before
   public void setUp() throws Exception {
    ldapServerProperties =  new LdapServerProperties();
    ldapServerProperties = new LdapServerProperties();
     ldapServerProperties.setAnonymousBind(true);
     ldapServerProperties.setBaseDN("dc=ambari,dc=apache,dc=org");
     ldapServerProperties.setManagerDn("uid=manager," + ldapServerProperties.getBaseDN());
- 
2.19.1.windows.1

