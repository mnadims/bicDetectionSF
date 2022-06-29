From 3f2743b58ba65c4bdc7d37df5348d17124bf305a Mon Sep 17 00:00:00 2001
From: Robert Levas <rlevas@hortonworks.com>
Date: Wed, 15 Nov 2017 12:11:34 -0500
Subject: [PATCH] AMBARI-22418.  Make Ambari configuration API consistent with
 existing API. (rlevas)

--
 .../ResourceInstanceFactoryImpl.java          |   9 +-
 ...ootServiceComponentResourceDefinition.java |   9 +-
 .../services/AmbariConfigurationService.java  | 193 -------
 .../server/api/services/AmbariMetaInfo.java   |   4 +-
 ...ComponentConfigurationRequestSwagger.java} |  18 +-
 ...omponentConfigurationResponseSwagger.java} |  19 +-
 ...tServiceComponentConfigurationService.java | 226 +++++++++
 .../api/services/RootServiceService.java      |   7 +
 .../services/ldap/AmbariConfiguration.java    |   6 +-
 .../ldap/LdapConfigurationRequest.java        |   2 +-
 .../ldap/LdapConfigurationService.java        |   6 +-
 .../commands/StackAdvisorCommand.java         |  62 ++-
 .../server/controller/ControllerModule.java   |   4 +-
 .../controller/MaintenanceStateHelper.java    |   3 +-
 .../controller/ResourceProviderFactory.java   |   4 +-
 .../server/controller/RootComponent.java      |  27 +
 .../ambari/server/controller/RootService.java |  36 ++
 .../RootServiceResponseFactory.java           |  44 +-
 .../AbstractControllerResourceProvider.java   |   4 +-
 .../AmbariConfigurationResourceProvider.java  | 302 -----------
 ...omponentConfigurationResourceProvider.java | 476 ++++++++++++++++++
 .../RootServiceComponentPropertyProvider.java |   4 +-
 .../RootServiceComponentResourceProvider.java |   4 +-
 .../server/controller/spi/Resource.java       |   6 +-
 .../alerts/AlertReceivedListener.java         |  10 +-
 .../alerts/AlertStateChangedListener.java     |   6 +-
 .../ldap/domain/AmbariLdapConfiguration.java  |   6 +-
 .../AmbariLdapConfigurationFactory.java       |   2 +-
 .../AmbariLdapConfigurationProvider.java      |   6 +-
 .../AmbariServiceAlertDefinitions.java        |  20 +-
 .../server/orm/dao/AlertDefinitionDAO.java    |   7 +-
 .../server/orm/dao/AlertDispatchDAO.java      |   4 +-
 .../state/alert/AlertDefinitionHash.java      |   8 +-
 .../server/state/cluster/ClusterImpl.java     |   4 +-
 .../services/AmbariServerAlertService.java    |   4 +-
 .../commands/StackAdvisorCommandTest.java     |   8 +-
 .../AmbariManagementControllerTest.java       |  16 +-
 .../RootServiceResponseFactoryTest.java       |  27 +-
 ...entConfigurationResourceProviderTest.java} |  80 +--
 ...tServiceComponentPropertyProviderTest.java |  15 +-
 ...tServiceComponentResourceProviderTest.java |   9 +-
 .../server/ldap/LdapModuleFunctionalTest.java |   4 +-
 .../TestAmbariLdapConfigurationFactory.java   |   2 +-
 .../ldap/service/AmbariLdapFacadeTest.java    |   4 +-
 ...aultLdapAttributeDetectionServiceTest.java |   8 +-
 .../DefaultLdapConfigurationServiceTest.java  |  12 +-
 .../metadata/AgentAlertDefinitionsTest.java   |   6 +-
 .../orm/dao/AlertDefinitionDAOTest.java       |   7 +-
 .../alerts/AlertReceivedListenerTest.java     |  16 +-
 .../alerts/AlertStateChangedEventTest.java    |   4 +-
 50 files changed, 1036 insertions(+), 734 deletions(-)
 delete mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
 rename ambari-server/src/main/java/org/apache/ambari/server/api/services/{AmbariConfigurationRequestSwagger.java => RootServiceComponentConfigurationRequestSwagger.java} (64%)
 rename ambari-server/src/main/java/org/apache/ambari/server/api/services/{AmbariConfigurationResponseSwagger.java => RootServiceComponentConfigurationResponseSwagger.java} (64%)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationService.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/controller/RootComponent.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/controller/RootService.java
 delete mode 100644 ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java
 rename ambari-server/src/test/java/org/apache/ambari/server/controller/internal/{AmbariConfigurationResourceProviderTest.java => RootServiceComponentConfigurationResourceProviderTest.java} (78%)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java
index f5fb6e9900..fecaedc459 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/resources/ResourceInstanceFactoryImpl.java
@@ -237,6 +237,11 @@ public class ResourceInstanceFactoryImpl implements ResourceInstanceFactory {
         resourceDefinition = new RootServiceComponentResourceDefinition();
         break;
 
      case RootServiceComponentConfiguration:
        resourceDefinition = new SimpleResourceDefinition(Resource.Type.RootServiceComponentConfiguration,
            "configuration", "configurations");
        break;

       case RootServiceHostComponent:
         resourceDefinition = new RootServiceHostComponentResourceDefinition();
         break;
@@ -471,10 +476,6 @@ public class ResourceInstanceFactoryImpl implements ResourceInstanceFactory {
       case RemoteCluster:
         resourceDefinition = new RemoteClusterResourceDefinition();
         break;
      case AmbariConfiguration:
        resourceDefinition = new SimpleResourceDefinition(Resource.Type.AmbariConfiguration, "ambariconfiguration", "ambariconfigurations");

        break;
 
       default:
         throw new IllegalArgumentException("Unsupported resource type: " + type);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/resources/RootServiceComponentResourceDefinition.java b/ambari-server/src/main/java/org/apache/ambari/server/api/resources/RootServiceComponentResourceDefinition.java
index e8cb570c56..1c036e4dfc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/resources/RootServiceComponentResourceDefinition.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/resources/RootServiceComponentResourceDefinition.java
@@ -19,6 +19,7 @@
 package org.apache.ambari.server.api.resources;
 
 import java.util.Collections;
import java.util.HashSet;
 import java.util.Set;
 
 import org.apache.ambari.server.controller.spi.Resource;
@@ -44,10 +45,12 @@ public class RootServiceComponentResourceDefinition extends
   public String getSingularName() {
     return "component";
   }
  

   @Override
   public Set<SubResourceDefinition> getSubResourceDefinitions() {
    return Collections.singleton(new SubResourceDefinition(
        Resource.Type.RootServiceHostComponent, Collections.singleton(Resource.Type.Host), true));
    Set<SubResourceDefinition> definitions = new HashSet<>();
    definitions.add(new SubResourceDefinition(Resource.Type.RootServiceHostComponent, Collections.singleton(Resource.Type.Host), true));
    definitions.add(new SubResourceDefinition(Resource.Type.RootServiceComponentConfiguration));
    return definitions;
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
deleted file mode 100644
index 86ed666f41..0000000000
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
++ /dev/null
@@ -1,193 +0,0 @@
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
 * Eg. LDAP configuration is stored as AmbariConfiguration.
 * The request payload has the form:
 *
 * <pre>
 *      {
 *        "AmbariConfiguration": {
 *          "category": "ldap-configuration",
 *          "properties": {
 *             "authentication.ldap.primaryUrl": "localhost:33389"
 *             "authentication.ldap.secondaryUrl": "localhost:333"
 *             "authentication.ldap.baseDn": "dc=ambari,dc=apache,dc=org"
 *             // ......
 *          }
 *        }
 *      }
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
      Collections.emptyMap()));
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
      defaultValue = "AmbariConfiguration/properties, AmbariConfiguration/category",
      dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
      defaultValue = "AmbariConfiguration/category",
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
      Collections.emptyMap()));
  }

  @GET
  @Path("{category}")
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
                                         @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, category)));
  }

  @PUT
  @Path("{category}")
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
                                            @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.PUT, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, category)));
  }

  @DELETE
  @Path("{category}")
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
                                            @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.DELETE, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, category)));
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java
index 46ee65abea..b1eba8fe40 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java
@@ -43,7 +43,7 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.ParentObjectNotFoundException;
 import org.apache.ambari.server.StackAccessException;
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
 import org.apache.ambari.server.customactions.ActionDefinition;
 import org.apache.ambari.server.customactions.ActionDefinitionManager;
@@ -1179,7 +1179,7 @@ public class AmbariMetaInfo {
         String componentName = definition.getComponentName();
 
         // the AMBARI service is special, skip it here
        if (Services.AMBARI.name().equals(serviceName)) {
        if (RootService.AMBARI.name().equals(serviceName)) {
           continue;
         }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationRequestSwagger.java
similarity index 64%
rename from ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
rename to ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationRequestSwagger.java
index 2dca9f55f4..dffa12570a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationRequestSwagger.java
@@ -20,18 +20,24 @@ import org.apache.ambari.server.controller.ApiModel;
 import io.swagger.annotations.ApiModelProperty;
 
 /**
 * Request data model for {@link org.apache.ambari.server.api.services.AmbariConfigurationService}
 * Request data model for {@link org.apache.ambari.server.api.services.RootServiceComponentConfigurationService}
  */
public interface AmbariConfigurationRequestSwagger extends ApiModel {
public interface RootServiceComponentConfigurationRequestSwagger extends ApiModel {
 
  @ApiModelProperty(name = "AmbariConfiguration")
  AmbariConfigurationRequestInfo getAmbariConfiguration();
  @ApiModelProperty(name = "Configuration")
  RootServiceComponentConfigurationRequestInfo getRootServiceComponentConfigurationRequestInfo();

  interface RootServiceComponentConfigurationRequestInfo {
    @ApiModelProperty
    String getServiceName();

    @ApiModelProperty
    String getComponentName();
 
  interface AmbariConfigurationRequestInfo {
     @ApiModelProperty
     String getCategoryName();
 
     @ApiModelProperty
    Map<String, Object> getProperties();
    Map<String, String> getProperties();
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationResponseSwagger.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationResponseSwagger.java
similarity index 64%
rename from ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationResponseSwagger.java
rename to ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationResponseSwagger.java
index c55ac1dd60..fb3c09d256 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationResponseSwagger.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationResponseSwagger.java
@@ -20,21 +20,24 @@ import org.apache.ambari.server.controller.ApiModel;
 import io.swagger.annotations.ApiModelProperty;
 
 /**
 * Response data model for {@link org.apache.ambari.server.api.services.AmbariConfigurationService}
 * Response data model for {@link org.apache.ambari.server.api.services.RootServiceComponentConfigurationService}
  */
public interface AmbariConfigurationResponseSwagger extends ApiModel {
public interface RootServiceComponentConfigurationResponseSwagger extends ApiModel {
 
  @ApiModelProperty(name = "AmbariConfiguration")
  AmbariConfigurationResponseInfo getAmbariConfigurationResponse();
  @ApiModelProperty(name = "Configuration")
  RootServiceComponentConfigurationResponseInfo getRootServiceComponentConfigurationResponseInfo();
 
  interface AmbariConfigurationResponseInfo {
  interface RootServiceComponentConfigurationResponseInfo {
     @ApiModelProperty
    Long getId();
    String getServiceName();
 
     @ApiModelProperty
    Map<String, Object> getData();
    String getComponentName();
 
     @ApiModelProperty
    String getType();
    String getCategoryName();

    @ApiModelProperty
    Map<String, Object> getProperties();
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationService.java
new file mode 100644
index 0000000000..c7c37a6956
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceComponentConfigurationService.java
@@ -0,0 +1,226 @@
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

import java.util.HashMap;
import java.util.Map;

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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Rest endpoint for managing ambari root service component configurations. Supports CRUD operations.
 * Ambari configurations are resources that relate to the ambari server instance even before a cluster is provisioned.
 * <p>
 * Ambari configuration resources may be shared with components and services in the cluster
 * (by recommending them as default values)
 * <p>
 * Eg. LDAP configuration is stored as Configuration.
 * The request payload has the form:
 * <p>
 * <pre>
 *      {
 *        "Configuration": {
 *          "service_name": "AMBARI",
 *          "component_name": "AMBARI_SERVER",
 *          "category": "ldap-configuration",
 *          "properties": {
 *             "authentication.ldap.primaryUrl": "localhost:33389"
 *             "authentication.ldap.secondaryUrl": "localhost:333"
 *             "authentication.ldap.baseDn": "dc=ambari,dc=apache,dc=org"
 *             // ......
 *          }
 *        }
 *      }
 * </pre>
 */
@Api(value = "Root Service Configurations", description = "Endpoint for Ambari root service component configuration related operations")
public class RootServiceComponentConfigurationService extends BaseService {

  private static final String REQUEST_TYPE =
      "org.apache.ambari.server.api.services.RootServiceComponentConfigurationRequestSwagger";

  private final String serviceName;
  private final String componentName;

  public RootServiceComponentConfigurationService(String serviceName, String componentName) {
    this.serviceName = serviceName;
    this.componentName = componentName;
  }

  /**
   * Creates a root service component configuration resource.
   *
   * @param body    the payload in json format
   * @param headers http headers
   * @param uri     request uri information
   * @return
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a root service component configuration resource",
      nickname = "RootServiceComponentConfigurationService#createConfiguration")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.POST, createResource(null));
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve all root service component configuration resources",
      nickname = "RootServiceComponentConfigurationService#getConfigurations",
      notes = "Returns all root service component configurations.",
      response = RootServiceComponentConfigurationResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Configuration/properties, Configuration/category, Configuration/component_name, Configuration/service_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "Configuration/category",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getConfigurations(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(null));
  }

  @GET
  @Path("{category}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve the details of a root service component configuration resource",
      nickname = "RootServiceComponentConfigurationService#getConfiguration",
      response = RootServiceComponentConfigurationResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "Configuration/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                   @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(category));
  }

  @PUT
  @Path("{category}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates root service component configuration resources ",
      nickname = "RootServiceComponentConfigurationService#updateConfiguration")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = REQUEST_TYPE, paramType = PARAM_TYPE_BODY),
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "Configuration/*",
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
  public Response updateConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                      @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.PUT, createResource(category));
  }

  @DELETE
  @Path("{category}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a root service component configuration resource",
      nickname = "RootServiceComponentConfigurationService#deleteConfiguration")
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                      @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.DELETE, createResource(category));
  }

  ResourceInstance createResource(String categoryName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootService, serviceName);
    mapIds.put(Resource.Type.RootServiceComponent, componentName);
    mapIds.put(Resource.Type.RootServiceComponentConfiguration, categoryName);

    return createResource(Resource.Type.RootServiceComponentConfiguration, mapIds);
  }

}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceService.java
index 5afb7dc90c..1ab2797b7c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/RootServiceService.java
@@ -297,6 +297,13 @@ public class RootServiceService extends BaseService {
     return handleRequest(headers, body, ui, Request.Type.GET, resource);
   }
 
  @Path("{serviceName}/components/{componentName}/configurations")
  public RootServiceComponentConfigurationService getAmbariServerConfigurationHandler(@Context javax.ws.rs.core.Request request,
                                                                                      @PathParam("serviceName") String serviceName,
                                                                                      @PathParam("componentName") String componentName) {
    return new RootServiceComponentConfigurationService(serviceName, componentName);
  }

   protected ResourceInstance createServiceResource(String serviceName) {
     Map<Resource.Type, String> mapIds = Collections.singletonMap(Resource.Type.RootService, serviceName);
     return createResource(Resource.Type.RootService, mapIds);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
index e9f0b1e1a4..7bac65ec83 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
@@ -26,7 +26,7 @@ public class AmbariConfiguration {
    */
   private String type;
 
  private Map<String, Object> properties = null;
  private Map<String, String> properties = null;
 
   public String getType() {
     return type;
@@ -36,11 +36,11 @@ public class AmbariConfiguration {
     this.type = type;
   }
 
  public Map<String, Object> getProperties() {
  public Map<String, String> getProperties() {
     return properties;
   }
 
  public void setProperties(Map<String, Object> data) {
  public void setProperties(Map<String, String> data) {
     this.properties = data;
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java
index 2e478c4329..0e065e5f21 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationRequest.java
@@ -22,7 +22,7 @@ import com.google.gson.annotations.SerializedName;
  */
 public class LdapConfigurationRequest {
 
  @SerializedName("AmbariConfiguration")
  @SerializedName("Configuration")
   private AmbariConfiguration ambariConfiguration;
 
   @SerializedName("RequestInfo")
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
index 00c13f6cfa..22784cd599 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
@@ -40,7 +40,7 @@ import javax.ws.rs.core.Response;
 
 import org.apache.ambari.annotations.ApiIgnore;
 import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariConfigurationService;
import org.apache.ambari.server.api.services.BaseService;
 import org.apache.ambari.server.api.services.Result;
 import org.apache.ambari.server.api.services.ResultImpl;
 import org.apache.ambari.server.api.services.ResultStatus;
@@ -64,7 +64,7 @@ import com.google.common.collect.Sets;
  */
 @StaticallyInject
 @Path("/ldapconfigs/")
public class LdapConfigurationService extends AmbariConfigurationService {
public class LdapConfigurationService extends BaseService {
 
   private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigurationService.class);
 
@@ -133,7 +133,7 @@ public class LdapConfigurationService extends AmbariConfigurationService {
   }
 
   private void setResult(Set<String> groups, Result result) {
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    Resource resource = new ResourceImpl(Resource.Type.RootServiceComponentConfiguration);
     resource.setProperty("groups", groups);
     result.getResultTree().addChild(resource, "payload");
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
index 91edf867ae..17591ec7d0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommand.java
@@ -45,6 +45,8 @@ import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
 import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
 import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
 import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.utils.DateUtils;
@@ -84,7 +86,9 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
       + ",services/configurations/dependencies/StackConfigurationDependency/dependency_name"
       + ",services/configurations/dependencies/StackConfigurationDependency/dependency_type,services/configurations/StackConfigurations/type"
       + "&services/StackServices/service_name.in(%s)";
  private static final String GET_LDAP_CONFIG_URI = "/api/v1/configurations?AmbariConfiguration/type=ldap&fields=AmbariConfiguration/*";
  private static final String GET_AMBARI_LDAP_CONFIG_URI = "/api/v1/services/AMBARI/components/AMBARI_SERVER/configurations" +
      "?Configuration/category=ldap-configuration" +
      "&fields=Configuration/properties";
   private static final String SERVICES_PROPERTY = "services";
   private static final String SERVICES_COMPONENTS_PROPERTY = "components";
   private static final String CONFIG_GROUPS_PROPERTY = "config-groups";
@@ -96,7 +100,7 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
   private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed-configurations";
   private static final String USER_CONTEXT_PROPERTY = "user-context";
   private static final String AMBARI_SERVER_CONFIGURATIONS_PROPERTY = "ambari-server-properties";
  protected static final String LDAP_CONFIGURATION_PROPERTY = "ldap-configuration";
  static final String LDAP_CONFIGURATION_PROPERTY = "ldap-configuration";
 
   private File recommendationsDir;
   private String recommendationsArtifactsLifetime;
@@ -176,17 +180,18 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
 
   /**
    * Retrieves the LDAP configuration if exists and adds it to services.json
   *
    * @param root The JSON document that will become service.json when passed to the stack advisor engine
    * @throws StackAdvisorException
    * @throws IOException
    */
  protected void populateLdapConfiguration(ObjectNode root) throws StackAdvisorException, IOException {
    Response response = handleRequest(null, null, new LocalUriInfo(GET_LDAP_CONFIG_URI), Request.Type.GET,
   void populateLdapConfiguration(ObjectNode root) throws StackAdvisorException, IOException {
    Response response = handleRequest(null, null, new LocalUriInfo(GET_AMBARI_LDAP_CONFIG_URI), Request.Type.GET,
         createConfigResource());
 
     if (response.getStatus() != Status.OK.getStatusCode()) {
       String message = String.format(
          "Error occured during retrieving ldap configuration, status=%s, response=%s",
          "Error occurred during retrieving ldap configuration, status=%s, response=%s",
           response.getStatus(), (String) response.getEntity());
       LOG.warn(message);
       throw new StackAdvisorException(message);
@@ -198,25 +203,28 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
     }
 
     JsonNode ldapConfigRoot = mapper.readTree(ldapConfigJSON);
    ArrayNode ldapConfigs = ((ArrayNode)ldapConfigRoot.get("items"));
    ArrayNode ldapConfigs = ((ArrayNode) ldapConfigRoot.get("items"));
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

    if (numConfigs == 1) {
      JsonNode ldapConfigItem = ldapConfigs.get(0);
      if (ldapConfigItem == null) {
        throw new StackAdvisorException("Unexpected JSON document encountered: missing data");
      }

      JsonNode ldapConfiguration = ldapConfigItem.get("Configuration");
      if (ldapConfiguration == null) {
        throw new StackAdvisorException("Unexpected JSON document encountered: missing the Configuration object");
      }

      JsonNode ldapConfigurationProperties = ldapConfiguration.get("properties");
      if (ldapConfigurationProperties == null) {
        throw new StackAdvisorException("Unexpected JSON document encountered: missing the Configuration/properties object");
      }

      root.put(LDAP_CONFIGURATION_PROPERTY, ldapConfigurationProperties);
    } else if (numConfigs > 1) {
      throw new StackAdvisorException(String.format("Multiple (%s) LDAP configs are found in the DB.", numConfigs));
     }
   }
 
@@ -486,8 +494,12 @@ public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extend
     return createResource(Resource.Type.Host, mapIds);
   }
 
  protected ResourceInstance createConfigResource() {
    return createResource(Resource.Type.AmbariConfiguration, new HashMap<>());
  private ResourceInstance createConfigResource() {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootService, RootService.AMBARI.name());
    mapIds.put(Resource.Type.RootServiceComponent, RootComponent.AMBARI_SERVER.name());

    return createResource(Resource.Type.RootServiceComponentConfiguration, mapIds);
   }
 
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
index 1425e1bd32..ed7513ff56 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
@@ -63,7 +63,6 @@ import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.configuration.Configuration.ConnectionPoolType;
 import org.apache.ambari.server.configuration.Configuration.DatabaseType;
 import org.apache.ambari.server.controller.internal.AlertTargetResourceProvider;
import org.apache.ambari.server.controller.internal.AmbariConfigurationResourceProvider;
 import org.apache.ambari.server.controller.internal.ClusterStackVersionResourceProvider;
 import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
 import org.apache.ambari.server.controller.internal.CredentialResourceProvider;
@@ -73,6 +72,7 @@ import org.apache.ambari.server.controller.internal.HostResourceProvider;
 import org.apache.ambari.server.controller.internal.KerberosDescriptorResourceProvider;
 import org.apache.ambari.server.controller.internal.MemberResourceProvider;
 import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider;
 import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
 import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
 import org.apache.ambari.server.controller.internal.ViewInstanceResourceProvider;
@@ -471,10 +471,10 @@ public class ControllerModule extends AbstractModule {
         .implement(ResourceProvider.class, Names.named("credential"), CredentialResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("kerberosDescriptor"), KerberosDescriptorResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("upgrade"), UpgradeResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("ambariConfiguration"), AmbariConfigurationResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("clusterStackVersion"), ClusterStackVersionResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("alertTarget"), AlertTargetResourceProvider.class)
         .implement(ResourceProvider.class, Names.named("viewInstance"), ViewInstanceResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("rootServiceHostComponentConfiguration"), RootServiceComponentConfigurationResourceProvider.class)
         .build(ResourceProviderFactory.class));
 
     install(new FactoryModuleBuilder().implement(
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/MaintenanceStateHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/MaintenanceStateHelper.java
index d9a585f630..1ffe841403 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/MaintenanceStateHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/MaintenanceStateHelper.java
@@ -24,7 +24,6 @@ import java.util.Set;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
 import org.apache.ambari.server.controller.internal.RequestOperationLevel;
 import org.apache.ambari.server.controller.internal.RequestResourceFilter;
 import org.apache.ambari.server.controller.spi.Resource;
@@ -265,7 +264,7 @@ public class MaintenanceStateHelper {
     }
 
     // the AMBARI service is not a real service; it's never in MM
    if( StringUtils.equals(Services.AMBARI.name(), serviceName)){
    if( StringUtils.equals(RootService.AMBARI.name(), serviceName)){
       return MaintenanceState.OFF;
     }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java
index 711ae10f7e..f6ca16bc69 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ResourceProviderFactory.java
@@ -68,8 +68,8 @@ public interface ResourceProviderFactory {
   @Named("upgrade")
   UpgradeResourceProvider getUpgradeResourceProvider(AmbariManagementController managementController);
 
  @Named("ambariConfiguration")
  ResourceProvider getAmbariConfigurationResourceProvider();
  @Named("rootServiceHostComponentConfiguration")
  ResourceProvider getRootServiceHostComponentConfigurationResourceProvider();
 
   @Named("clusterStackVersion")
   ClusterStackVersionResourceProvider getClusterStackVersionResourceProvider(AmbariManagementController managementController);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/RootComponent.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/RootComponent.java
new file mode 100644
index 0000000000..74cdfcf084
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/RootComponent.java
@@ -0,0 +1,27 @@
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

package org.apache.ambari.server.controller;

/**
 * RootComponent is an enumeration of root-level components.
 */
public enum RootComponent {
  AMBARI_SERVER,
  AMBARI_AGENT
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/RootService.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/RootService.java
new file mode 100644
index 0000000000..22f571dfc7
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/RootService.java
@@ -0,0 +1,36 @@
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

package org.apache.ambari.server.controller;

/**
 * RootService is an enumeration of root-level services.
 */
public enum RootService {
  AMBARI(RootComponent.values());

  private final RootComponent[] components;

  RootService(RootComponent[] components) {
    this.components = components;
  }

  public RootComponent[] getComponents() {
    return components;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/RootServiceResponseFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/RootServiceResponseFactory.java
index ad9ed202dc..63d41fd7cf 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/RootServiceResponseFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/RootServiceResponseFactory.java
@@ -62,9 +62,9 @@ public class RootServiceResponseFactory extends
       serviceName = request.getServiceName();
 
     if (serviceName != null) {
      Services service;
      RootService service;
       try {
        service = Services.valueOf(serviceName);
        service = RootService.valueOf(serviceName);
       }
       catch (IllegalArgumentException ex) {
         throw new ObjectNotFoundException("Root service name: " + serviceName);
@@ -74,7 +74,7 @@ public class RootServiceResponseFactory extends
     } else {
       response = new HashSet<>();
       
      for (Services service: Services.values())    
      for (RootService service: RootService.values())
         response.add(new RootServiceResponse(service.toString()));
     }    
     return response;
@@ -87,10 +87,10 @@ public class RootServiceResponseFactory extends
 
     String serviceName = request.getServiceName();
     String componentName = request.getComponentName();
    Services service;
    RootService service;
 
     try {
      service = Services.valueOf(serviceName);
      service = RootService.valueOf(serviceName);
     }
     catch (IllegalArgumentException ex) {
       throw new ObjectNotFoundException("Root service name: " + serviceName);
@@ -100,9 +100,9 @@ public class RootServiceResponseFactory extends
     }
     
     if (componentName != null) {
      Components component;
      RootComponent component;
       try {
        component = Components.valueOf(componentName);
        component = RootComponent.valueOf(componentName);
         if (!ArrayUtils.contains(service.getComponents(), component))
           throw new ObjectNotFoundException("No component name: " + componentName + "in service: " + serviceName);
       }
@@ -114,7 +114,7 @@ public class RootServiceResponseFactory extends
                                        getComponentProperties(componentName)));
     } else {
     
      for (Components component: service.getComponents())    
      for (RootComponent component: service.getComponents())
         response.add(new RootServiceComponentResponse(serviceName, component.toString(),
                      getComponentVersion(component.name(), null),
                      getComponentProperties(component.name())));
@@ -123,7 +123,7 @@ public class RootServiceResponseFactory extends
   }
 
   private String getComponentVersion(String componentName, HostResponse host) {
    Components component = Components.valueOf(componentName);
    RootComponent component = RootComponent.valueOf(componentName);
     String componentVersion;
       
     switch (component) {
@@ -150,10 +150,10 @@ public class RootServiceResponseFactory extends
     
     Map<String, String> response;
     Set<String> propertiesToHideInResponse;
    Components component = null;
    RootComponent component = null;
 
     if (componentName != null) {
      component = Components.valueOf(componentName);
      component = RootComponent.valueOf(componentName);
       
       switch (component) {
       case AMBARI_SERVER:
@@ -176,24 +176,6 @@ public class RootServiceResponseFactory extends
     return response;
   }
 
  
  public enum Services {
    AMBARI(Components.values());
    private Components[] components;

    Services(Components[] components) {
      this.components = components;
    }

    public Components[] getComponents() {
      return components;
    }
  }
  
  public enum Components {
    AMBARI_SERVER, AMBARI_AGENT
  }

   @Override
   public Set<RootServiceHostComponentResponse> getRootServiceHostComponent(RootServiceHostComponentRequest request, Set<HostResponse> hosts) throws AmbariException {
     Set<RootServiceHostComponentResponse> response = new HashSet<>();
@@ -208,7 +190,7 @@ public class RootServiceResponseFactory extends
       Set<HostResponse> filteredHosts = new HashSet<>(hosts);
       
       //Make some filtering of hosts if need
      if (component.getComponentName().equals(Components.AMBARI_SERVER.name())) {
      if (component.getComponentName().equals(RootComponent.AMBARI_SERVER.name())) {
         CollectionUtils.filter(filteredHosts, new Predicate() {
           @Override
           public boolean evaluate(Object arg0) {
@@ -220,7 +202,7 @@ public class RootServiceResponseFactory extends
       
       for (HostResponse host : filteredHosts) {
         String state;
        if (component.getComponentName().equals(Components.AMBARI_SERVER.name())) {
        if (component.getComponentName().equals(RootComponent.AMBARI_SERVER.name())) {
           state = RUNNING_STATE;
         } else {
           state = host.getHostState().toString();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java
index 1dc0841d19..cc2548cc61 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AbstractControllerResourceProvider.java
@@ -206,6 +206,8 @@ public abstract class AbstractControllerResourceProvider extends AbstractAuthori
         return new RootServiceResourceProvider(propertyIds, keyPropertyIds, managementController);
       case RootServiceComponent:
         return new RootServiceComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case RootServiceComponentConfiguration:
        return resourceProviderFactory.getRootServiceHostComponentConfigurationResourceProvider();
       case RootServiceHostComponent:
         return new RootServiceHostComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
       case ConfigGroup:
@@ -254,8 +256,6 @@ public abstract class AbstractControllerResourceProvider extends AbstractAuthori
         return new ClusterKerberosDescriptorResourceProvider(managementController);
       case LoggingQuery:
         return new LoggingResourceProvider(propertyIds, keyPropertyIds, managementController);
      case AmbariConfiguration:
        return resourceProviderFactory.getAmbariConfigurationResourceProvider();
       case AlertTarget:
         return resourceProviderFactory.getAlertTargetResourceProvider();
       case ViewInstance:
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
deleted file mode 100644
index a24400dffd..0000000000
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
++ /dev/null
@@ -1,302 +0,0 @@
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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Resource provider for AmbariConfiguration resources.
 */
public class AmbariConfigurationResourceProvider extends AbstractAuthorizedResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationResourceProvider.class);

  static final String AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId("AmbariConfiguration", "category");
  static final String AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId("AmbariConfiguration", "properties");

  private static final Set<String> PROPERTIES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID,
          AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID)
      )
  );

  private static final Map<Resource.Type, String> PK_PROPERTY_MAP = Collections.unmodifiableMap(
      Collections.singletonMap(Resource.Type.AmbariConfiguration, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
  );

  private static final Set<String> PK_PROPERTY_IDS = Collections.unmodifiableSet(
      new HashSet<>(PK_PROPERTY_MAP.values())
  );

  @Inject
  private AmbariConfigurationDAO ambariConfigurationDAO;

  @Inject
  private AmbariEventPublisher publisher;

  public AmbariConfigurationResourceProvider() {
    super(PROPERTIES, PK_PROPERTY_MAP);

    Set<RoleAuthorization> authorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
    setRequiredCreateAuthorizations(authorizations);
    setRequiredDeleteAuthorizations(authorizations);
    setRequiredUpdateAuthorizations(authorizations);
    setRequiredGetAuthorizations(authorizations);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    createOrAddProperties(null, request.getProperties(), true);

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    return getResources(new Command<Set<Resource>>() {
      @Override
      public Set<Resource> invoke() throws AmbariException {
        Set<Resource> resources = new HashSet<>();
        Set<String> requestedIds = getRequestPropertyIds(request, predicate);

        if (CollectionUtils.isEmpty(requestedIds)) {
          requestedIds = PROPERTIES;
        }

        if (predicate == null) {
          Set<Resource> _resources = getAmbariConfigurationResources(requestedIds, null);
          if (!CollectionUtils.isEmpty(_resources)) {
            resources.addAll(_resources);
          }
        } else {
          for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
            Set<Resource> _resources = getAmbariConfigurationResources(requestedIds, propertyMap);
            if (!CollectionUtils.isEmpty(_resources)) {
              resources.addAll(_resources);
            }
          }
        }

        return resources;
      }
    });
  }


  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String categoryName = (String) PredicateHelper.getProperties(predicate).get(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);

    if (null == categoryName) {
      LOGGER.debug("No resource id provided in the request");
    } else {
      LOGGER.debug("Deleting Ambari configuration with id: {}", categoryName);
      try {
        ambariConfigurationDAO.removeByCategory(categoryName);
      } catch (IllegalStateException e) {
        throw new NoSuchResourceException(e.getMessage());
      }
    }

    // notify subscribers about the configuration changes
    publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String categoryName = (String) PredicateHelper.getProperties(predicate).get(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);
    createOrAddProperties(categoryName, request.getProperties(), false);

    return getRequestStatus(null);
  }

  /**
   * Retrieves groups of properties from the request data and create or updates them as needed.
   * <p>
   * Each group of properties is expected to have a category (<code>AmbariConfiguration/category</code>)
   * value and one or more property (<code>AmbariConfiguration/properties/property.name</code>) values.
   * If a category cannot be determined from the propery set, the default category value (passed in)
   * is used.  If a default category is set, it is assumed that it was parsed from the request predicate
   * (if availabe).
   *
   * @param defaultCategoryName            the default category to use if needed
   * @param requestProperties              a collection of property maps parsed from the request
   * @param removePropertiesIfNotSpecified <code>true</code> to remove existing properties that have not been specifed in the request; <code>false</code> append or update the existing set of properties with values from the request
   * @throws SystemException if an error occurs saving the configuration data
   */
  private void createOrAddProperties(String defaultCategoryName, Set<Map<String, Object>> requestProperties, boolean removePropertiesIfNotSpecified)
      throws SystemException {
    // set of resource properties (each entry in the set belongs to a different resource)
    if (requestProperties != null) {
      for (Map<String, Object> resourceProperties : requestProperties) {
        Map<String, Map<String, String>> entityMap = parseProperties(defaultCategoryName, resourceProperties);

        if (entityMap != null) {
          for (Map.Entry<String, Map<String, String>> entry : entityMap.entrySet()) {
            String categoryName = entry.getKey();

            if (ambariConfigurationDAO.reconcileCategory(categoryName, entry.getValue(), removePropertiesIfNotSpecified)) {
              // notify subscribers about the configuration changes
              publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
            }
          }
        }
      }
    }
  }

  private Resource toResource(String categoryName, Map<String, String> properties, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    setResourceProperty(resource, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName, requestedIds);
    setResourceProperty(resource, AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID, properties, requestedIds);
    return resource;
  }

  /**
   * Parse the property map from a request into a map of category names to maps of property names and values.
   *
   * @param defaultCategoryName the default category name to use if one is not found in the map of properties
   * @param resourceProperties  a map of properties from a request item
   * @return a map of category names to maps of name/value pairs
   * @throws SystemException if an issue with the data is determined
   */
  private Map<String, Map<String, String>> parseProperties(String defaultCategoryName, Map<String, Object> resourceProperties) throws SystemException {
    String categoryName = null;
    Map<String, String> properties = new HashMap<>();

    for (Map.Entry<String, Object> entry : resourceProperties.entrySet()) {
      String propertyName = entry.getKey();

      if (AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          categoryName = (String) entry.getValue();
        }
      } else {
        String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
        if ((propertyCategory != null) && propertyCategory.equals(AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID)) {
          String name = PropertyHelper.getPropertyName(entry.getKey());
          Object value = entry.getValue();
          properties.put(name, (value == null) ? null : value.toString());
        }
      }
    }

    if (categoryName == null) {
      categoryName = defaultCategoryName;
    }

    if (StringUtils.isEmpty(categoryName)) {
      throw new SystemException("The configuration type must be set");
    }

    if (properties.isEmpty()) {
      throw new SystemException("The configuration properties must be set");
    }

    return Collections.singletonMap(categoryName, properties);
  }

  private Set<Resource> getAmbariConfigurationResources(Set<String> requestedIds, Map<String, Object> propertyMap) {
    Set<Resource> resources = new HashSet<>();

    String categoryName = getStringProperty(propertyMap, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);

    List<AmbariConfigurationEntity> entities = (categoryName == null)
        ? ambariConfigurationDAO.findAll()
        : ambariConfigurationDAO.findByCategory(categoryName);

    if (entities != null) {
      Map<String, Map<String, String>> configurations = new HashMap<>();

      for (AmbariConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        Map<String, String> properties = configurations.get(category);

        if (properties == null) {
          properties = new TreeMap<>();
          configurations.put(category, properties);
        }

        properties.put(entity.getPropertyName(), entity.getPropertyValue());
      }

      for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
        resources.add(toResource(entry.getKey(), entry.getValue(), requestedIds));
      }
    }

    return resources;
  }

  private String getStringProperty(Map<String, Object> propertyMap, String propertyId) {
    String value = null;

    if (propertyMap != null) {
      Object o = propertyMap.get(propertyId);
      if (o instanceof String) {
        value = (String) o;
      }
    }

    return value;
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java
new file mode 100644
index 0000000000..ea9cf4ff48
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProvider.java
@@ -0,0 +1,476 @@
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

package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
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
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class RootServiceComponentConfigurationResourceProvider extends AbstractAuthorizedResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(RootServiceComponentConfigurationResourceProvider.class);

  static final String RESOURCE_KEY = "Configuration";

  static final String CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "category");
  static final String CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "properties");
  static final String CONFIGURATION_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "component_name");
  static final String CONFIGURATION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "service_name");

  private static final Set<String> PROPERTIES;

  private static final Map<Resource.Type, String> PK_PROPERTY_MAP;

  private static final Set<String> PK_PROPERTY_IDS;

  static {
    Set<String> set = new HashSet<>();
    set.add(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    set.add(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    set.add(CONFIGURATION_CATEGORY_PROPERTY_ID);
    set.add(CONFIGURATION_PROPERTIES_PROPERTY_ID);

    PROPERTIES = Collections.unmodifiableSet(set);

    Map<Resource.Type, String> map = new HashMap<>();
    map.put(Resource.Type.RootService, CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    map.put(Resource.Type.RootServiceComponent, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    map.put(Resource.Type.RootServiceComponentConfiguration, CONFIGURATION_CATEGORY_PROPERTY_ID);

    PK_PROPERTY_MAP = Collections.unmodifiableMap(map);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(new HashSet<>(PK_PROPERTY_MAP.values()));
  }

  @Inject
  private AmbariConfigurationDAO ambariConfigurationDAO;

  @Inject
  private AmbariEventPublisher publisher;

  public RootServiceComponentConfigurationResourceProvider() {
    super(PROPERTIES, PK_PROPERTY_MAP);

    Set<RoleAuthorization> authorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
    setRequiredCreateAuthorizations(authorizations);
    setRequiredDeleteAuthorizations(authorizations);
    setRequiredUpdateAuthorizations(authorizations);
    setRequiredGetAuthorizations(authorizations);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    createOrAddProperties(null, null, null, request.getProperties(), true);

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    return getResources(new Command<Set<Resource>>() {
      @Override
      public Set<Resource> invoke() throws AmbariException {
        Set<Resource> resources = new HashSet<>();
        Set<String> requestedIds = getRequestPropertyIds(request, predicate);

        if (CollectionUtils.isEmpty(requestedIds)) {
          requestedIds = PROPERTIES;
        }

        if (predicate == null) {
          Set<Resource> _resources;
          try {
            _resources = getConfigurationResources(requestedIds, null);
          } catch (NoSuchResourceException e) {
            throw new AmbariException(e.getMessage(), e);
          }

          if (!CollectionUtils.isEmpty(_resources)) {
            resources.addAll(_resources);
          }
        } else {
          for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
            Set<Resource> _resources;
            try {
              _resources = getConfigurationResources(requestedIds, propertyMap);
            } catch (NoSuchResourceException e) {
              throw new AmbariException(e.getMessage(), e);
            }

            if (!CollectionUtils.isEmpty(_resources)) {
              resources.addAll(_resources);
            }
          }
        }

        return resources;
      }
    });
  }


  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String serviceName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_CATEGORY_PROPERTY_ID);

    ConfigurationHandler handler = getConfigurationHandler(serviceName, componentName);
    if (handler != null) {
      handler.removeConfiguration(categoryName);
    } else {
      throw new SystemException(String.format("Configurations may not be updated for the %s component of the root service %s", componentName, serviceName));
    }

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String serviceName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_CATEGORY_PROPERTY_ID);

    createOrAddProperties(serviceName, componentName, categoryName, request.getProperties(), false);

    return getRequestStatus(null);
  }

  private Resource toResource(String serviceName, String componentName, String categoryName, Map<String, String> properties, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.RootServiceComponentConfiguration);
    setResourceProperty(resource, CONFIGURATION_SERVICE_NAME_PROPERTY_ID, serviceName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID, componentName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_PROPERTIES_PROPERTY_ID, properties, requestedIds);
    return resource;
  }

  /**
   * Retrieves groups of properties from the request data and create or updates them as needed.
   * <p>
   * Each group of properties is expected to have a category (<code>AmbariConfiguration/category</code>)
   * value and one or more property (<code>AmbariConfiguration/properties/property.name</code>) values.
   * If a category cannot be determined from the propery set, the default category value (passed in)
   * is used.  If a default category is set, it is assumed that it was parsed from the request predicate
   * (if availabe).
   *
   * @param defaultServiceName             the default service name to use if needed
   * @param defaultComponentName           the default component name to use if needed
   * @param defaultCategoryName            the default category to use if needed
   * @param requestProperties              a collection of property maps parsed from the request
   * @param removePropertiesIfNotSpecified <code>true</code> to remove existing properties that have not been specifed in the request;
   *                                       <code>false</code> append or update the existing set of properties with values from the request
   * @throws SystemException if an error occurs saving the configuration data
   */
  private void createOrAddProperties(String defaultServiceName, String defaultComponentName, String defaultCategoryName,
                                     Set<Map<String, Object>> requestProperties, boolean removePropertiesIfNotSpecified)
      throws SystemException {
    // set of resource properties (each entry in the set belongs to a different resource)
    if (requestProperties != null) {
      for (Map<String, Object> resourceProperties : requestProperties) {
        RequestDetails requestDetails = parseProperties(defaultServiceName, defaultComponentName, defaultCategoryName, resourceProperties);

        ConfigurationHandler handler = getConfigurationHandler(requestDetails.serviceName, requestDetails.componentName);

        if (handler != null) {
          handler.updateCategory(requestDetails.categoryName, requestDetails.properties, removePropertiesIfNotSpecified);
        } else {
          throw new SystemException(String.format("Configurations may not be updated for the %s component of the root service, %s", requestDetails.serviceName, requestDetails.componentName));
        }
      }
    }
  }

  /**
   * Parse the property map from a request into a map of services to components to category names to maps of property names and values.
   *
   * @param defaultServiceName   the default service name to use if one is not found in the map of properties
   * @param defaultComponentName the default component name to use if one is not found in the map of properties
   * @param defaultCategoryName  the default category name to use if one is not found in the map of properties
   * @param resourceProperties   a map of properties from a request item   @return a map of category names to maps of name/value pairs
   * @throws SystemException if an issue with the data is determined
   */
  private RequestDetails parseProperties(String defaultServiceName, String defaultComponentName, String defaultCategoryName, Map<String, Object> resourceProperties) throws SystemException {
    String serviceName = defaultServiceName;
    String componentName = defaultComponentName;
    String categoryName = defaultCategoryName;
    Map<String, String> properties = new HashMap<>();

    for (Map.Entry<String, Object> entry : resourceProperties.entrySet()) {
      String propertyName = entry.getKey();

      if (CONFIGURATION_CATEGORY_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          categoryName = (String) entry.getValue();
        }
      } else if (CONFIGURATION_COMPONENT_NAME_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          componentName = (String) entry.getValue();
        }
      } else if (CONFIGURATION_SERVICE_NAME_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          serviceName = (String) entry.getValue();
        }
      } else {
        String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
        if ((propertyCategory != null) && propertyCategory.equals(CONFIGURATION_PROPERTIES_PROPERTY_ID)) {
          String name = PropertyHelper.getPropertyName(entry.getKey());
          Object value = entry.getValue();
          properties.put(name, (value == null) ? null : value.toString());
        }
      }
    }

    if (StringUtils.isEmpty(serviceName)) {
      throw new SystemException("The service name must be set");
    }

    if (StringUtils.isEmpty(componentName)) {
      throw new SystemException("The component name must be set");
    }

    if (StringUtils.isEmpty(categoryName)) {
      throw new SystemException("The configuration category must be set");
    }

    if (properties.isEmpty()) {
      throw new SystemException("The configuration properties must be set");
    }

    return new RequestDetails(serviceName, componentName, categoryName, properties);
  }

  /**
   * Retrieves the requested configration resources
   *
   * @param requestedIds the requested properties ids
   * @param propertyMap  the request properties
   * @return a set of resources built from the found data
   * @throws NoSuchResourceException if the requested resource was not found
   */
  private Set<Resource> getConfigurationResources(Set<String> requestedIds, Map<String, Object> propertyMap) throws NoSuchResourceException {
    Set<Resource> resources = new HashSet<>();

    String serviceName = getStringProperty(propertyMap, CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = getStringProperty(propertyMap, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);

    ConfigurationHandler handler = getConfigurationHandler(serviceName, componentName);

    if (handler != null) {
      String categoryName = getStringProperty(propertyMap, CONFIGURATION_CATEGORY_PROPERTY_ID);
      Map<String, Map<String, String>> configurations = handler.getConfigurations(categoryName);

      if (configurations != null) {
        for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
          resources.add(toResource(serviceName, componentName, entry.getKey(), entry.getValue(), requestedIds));
        }
      }
    }

    return resources;
  }

  /**
   * Returns the internal configuration handler used to support various configuration storage facilites.
   *
   * @param serviceName   the service name
   * @param componentName the component name
   * @return
   */
  private ConfigurationHandler getConfigurationHandler(String serviceName, String componentName) {
    if (RootService.AMBARI.name().equals(serviceName)) {
      if (RootComponent.AMBARI_SERVER.name().equals(componentName)) {
        return new AmbariServerConfigurationHandler();
      }
    }

    return null;
  }


  private String getStringProperty(Map<String, Object> propertyMap, String propertyId) {
    String value = null;

    if (propertyMap != null) {
      Object o = propertyMap.get(propertyId);
      if (o instanceof String) {
        value = (String) o;
      }
    }

    return value;
  }

  /**
   * ConfigurationHandler is an interface to be implemented to support the relevant types of storage
   * used to persist root-level component configurations.
   */
  private abstract class ConfigurationHandler {
    /**
     * Retrieve the request configurations.
     *
     * @param categoryName the category name (or <code>null</code> for all)
     * @return a map of category names to properties (name/value pairs).
     * @throws NoSuchResourceException if the requested data is not found
     */
    public abstract Map<String, Map<String, String>> getConfigurations(String categoryName) throws NoSuchResourceException;

    /**
     * Delete the requested configuration.
     *
     * @param categoryName the category name
     * @throws NoSuchResourceException if the requested category does not exist
     */
    public abstract void removeConfiguration(String categoryName) throws NoSuchResourceException;

    /**
     * Set or update a configuration category with the specified properties.
     * <p>
     * If <code>removePropertiesIfNotSpecified</code> is <code>true</code>, the persisted category is to include only the specified properties.
     * <p>
     * If <code>removePropertiesIfNotSpecified</code> is <code>false</code>, the persisted category is to include the union of the existing and specified properties.
     * <p>
     * In any case, existing property values will be overwritten by the one specified in the property map.
     *
     * @param categoryName                   the category name
     * @param properties                     a map of properties to set
     * @param removePropertiesIfNotSpecified <code>true</code> to ensure the set of properties are only those that have be explicitly specified;
     *                                       <code>false</code> to update the set of exising properties with the specified set of properties, adding missing properties but not removing any properties
     */
    public abstract void updateCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified);
  }

  /**
   * AmbariServerConfigurationHandler handle Ambari server specific configuration properties.
   */
  private class AmbariServerConfigurationHandler extends ConfigurationHandler {
    @Override
    public Map<String, Map<String, String>> getConfigurations(String categoryName)
        throws NoSuchResourceException {
      Map<String, Map<String, String>> configurations = null;

      List<AmbariConfigurationEntity> entities = (categoryName == null)
          ? ambariConfigurationDAO.findAll()
          : ambariConfigurationDAO.findByCategory(categoryName);

      if (entities != null) {
        configurations = new HashMap<>();

        for (AmbariConfigurationEntity entity : entities) {
          String category = entity.getCategoryName();
          Map<String, String> properties = configurations.get(category);

          if (properties == null) {
            properties = new TreeMap<>();
            configurations.put(category, properties);
          }

          properties.put(entity.getPropertyName(), entity.getPropertyValue());
        }
      }

      return configurations;
    }

    @Override
    public void removeConfiguration(String categoryName) throws NoSuchResourceException {
      if (null == categoryName) {
        LOGGER.debug("No resource id provided in the request");
      } else {
        LOGGER.debug("Deleting Ambari configuration with id: {}", categoryName);
        try {
          if (ambariConfigurationDAO.removeByCategory(categoryName) > 0) {
            publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
          }
        } catch (IllegalStateException e) {
          throw new NoSuchResourceException(e.getMessage());
        }
      }
    }

    @Override
    public void updateCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) {
      if (ambariConfigurationDAO.reconcileCategory(categoryName, properties, removePropertiesIfNotSpecified)) {
        // notify subscribers about the configuration changes
        publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
      }
    }
  }

  /**
   * RequestDetails is a container for details parsed from the request.
   */
  private class RequestDetails {
    final String serviceName;
    final String componentName;
    final String categoryName;
    final Map<String, String> properties;

    private RequestDetails(String serviceName, String componentName, String categoryName, Map<String, String> properties) {
      this.serviceName = serviceName;
      this.componentName = componentName;
      this.categoryName = categoryName;
      this.properties = properties;
    }
  }
}

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProvider.java
index 433c1fae90..b5bbc94c12 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProvider.java
@@ -29,7 +29,7 @@ import java.util.Set;
 
 import javax.crypto.Cipher;
 
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.PropertyProvider;
 import org.apache.ambari.server.controller.spi.Request;
@@ -126,7 +126,7 @@ public class RootServiceComponentPropertyProvider extends BaseProvider implement
 
     for (Resource resource : resources) {
       // If this resource represents the AMBARI_SERVER component, handle it's specific properties...
      if (RootServiceResponseFactory.Components.AMBARI_SERVER.name().equals(resource.getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID))) {
      if (RootComponent.AMBARI_SERVER.name().equals(resource.getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID))) {
         // Attempt to fill in the cipher details only if explicitly asked for.
         if (requestedIds.contains(JCE_POLICY_PROPERTY_ID) || requestedIds.contains(CIPHER_PROPERTIES_PROPERTY_ID)) {
           setCipherDetails(resource, requestedIds);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProvider.java
index 3925aebdae..dfca00e6aa 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProvider.java
@@ -25,9 +25,9 @@ import java.util.Set;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.controller.RootServiceComponentRequest;
 import org.apache.ambari.server.controller.RootServiceComponentResponse;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
 import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
 import org.apache.ambari.server.controller.spi.NoSuchResourceException;
 import org.apache.ambari.server.controller.spi.Predicate;
@@ -99,7 +99,7 @@ public class RootServiceComponentResourceProvider extends ReadOnlyResourceProvid
       setResourceProperty(resource, PROPERTIES_PROPERTY_ID, response.getProperties(), requestedIds);
       setResourceProperty(resource, COMPONENT_VERSION_PROPERTY_ID, response.getComponentVersion(), requestedIds);
       
      if (Components.AMBARI_SERVER.name().equals(response.getComponentName())) {
      if (RootComponent.AMBARI_SERVER.name().equals(response.getComponentName())) {
         setResourceProperty(resource, SERVER_CLOCK_PROPERTY_ID, response.getServerClock(), requestedIds);
       }      
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java
index 78353735ac..90e031563e 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/spi/Resource.java
@@ -110,6 +110,7 @@ public interface Resource {
     TaskAttempt,
     RootService,
     RootServiceComponent,
    RootServiceComponentConfiguration,
     RootServiceHostComponent,
     View,
     ViewURL,
@@ -160,8 +161,7 @@ public interface Resource {
     VersionDefinition,
     ClusterKerberosDescriptor,
     LoggingQuery,
    RemoteCluster,
    AmbariConfiguration;
    RemoteCluster;
 
     /**
      * Get the {@link Type} that corresponds to this InternalType.
@@ -232,6 +232,7 @@ public interface Resource {
     public static final Type TaskAttempt = InternalType.TaskAttempt.getType();
     public static final Type RootService = InternalType.RootService.getType();
     public static final Type RootServiceComponent = InternalType.RootServiceComponent.getType();
    public static final Type RootServiceComponentConfiguration = InternalType.RootServiceComponentConfiguration.getType();
     public static final Type RootServiceHostComponent = InternalType.RootServiceHostComponent.getType();
     public static final Type View = InternalType.View.getType();
     public static final Type ViewURL = InternalType.ViewURL.getType();
@@ -283,7 +284,6 @@ public interface Resource {
     public static final Type ClusterKerberosDescriptor = InternalType.ClusterKerberosDescriptor.getType();
     public static final Type LoggingQuery = InternalType.LoggingQuery.getType();
     public static final Type RemoteCluster = InternalType.RemoteCluster.getType();
    public static final Type AmbariConfiguration = InternalType.AmbariConfiguration.getType();
 
 
     /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertReceivedListener.java b/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertReceivedListener.java
index 266c7e81c1..71988143c3 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertReceivedListener.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertReceivedListener.java
@@ -26,8 +26,8 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.EagerSingleton;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.events.AlertEvent;
 import org.apache.ambari.server.events.AlertReceivedEvent;
 import org.apache.ambari.server.events.AlertStateChangeEvent;
@@ -434,9 +434,9 @@ public class AlertReceivedListener {
     String hostName = alert.getHostName();
 
     // AMBARI/AMBARI_SERVER is always a valid service/component combination
    String ambariServiceName = Services.AMBARI.name();
    String ambariServerComponentName = Components.AMBARI_SERVER.name();
    String ambariAgentComponentName = Components.AMBARI_AGENT.name();
    String ambariServiceName = RootService.AMBARI.name();
    String ambariServerComponentName = RootComponent.AMBARI_SERVER.name();
    String ambariAgentComponentName = RootComponent.AMBARI_AGENT.name();
     if (ambariServiceName.equals(serviceName) && ambariServerComponentName.equals(componentName)) {
       return true;
     }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertStateChangedListener.java b/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertStateChangedListener.java
index d5dc530394..8701b6df55 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertStateChangedListener.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertStateChangedListener.java
@@ -24,7 +24,7 @@ import java.util.UUID;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.events.AlertStateChangeEvent;
 import org.apache.ambari.server.events.publishers.AlertEventPublisher;
 import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
@@ -67,7 +67,7 @@ import com.google.inject.Singleton;
  * <ul>
  * <li>If {@link AlertTargetEntity#isEnabled()} is {@code false}
  * <li>If the cluster is upgrading or the upgrade is suspended, only
 * {@link Services#AMBARI} alerts will be dispatched.
 * {@link RootService#AMBARI} alerts will be dispatched.
  * </ul>
  */
 @Singleton
@@ -230,7 +230,7 @@ public class AlertStateChangedListener {
       if (null != cluster.getUpgradeInProgress()) {
         // only send AMBARI alerts if in an upgrade
         String serviceName = definition.getServiceName();
        if (!StringUtils.equals(serviceName, Services.AMBARI.name())) {
        if (!StringUtils.equals(serviceName, RootService.AMBARI.name())) {
           LOG.debug(
               "Skipping alert notifications for {} because the cluster is upgrading",
               definition.getDefinitionName(), target);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
index 0c1ec0ae71..6c466bae37 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
@@ -33,7 +33,7 @@ public class AmbariLdapConfiguration {
 
   private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapConfiguration.class);
 
  private final Map<String, Object> configurationMap;
  private final Map<String, String> configurationMap;
 
   private Object configValue(AmbariLdapConfigKeys ambariLdapConfigKeys) {
     Object value = null;
@@ -45,13 +45,13 @@ public class AmbariLdapConfiguration {
     return value;
   }
 
  public void setValueFor(AmbariLdapConfigKeys ambariLdapConfigKeys, Object value) {
  public void setValueFor(AmbariLdapConfigKeys ambariLdapConfigKeys, String value) {
     configurationMap.put(ambariLdapConfigKeys.key(), value);
   }
 
   // intentionally package private, instances to be created through the factory
   @Inject
  AmbariLdapConfiguration(@Assisted Map<String, Object> configuration) {
  AmbariLdapConfiguration(@Assisted Map<String, String> configuration) {
     this.configurationMap = configuration;
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java
index 2b9f24be89..aafd204abc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfigurationFactory.java
@@ -30,5 +30,5 @@ public interface AmbariLdapConfigurationFactory {
    * @param configuration a map where keys are the configuration properties and values are the configuration values
    * @return an AmbariLdapConfiguration instance
    */
  AmbariLdapConfiguration createLdapConfiguration(Map<String, Object> configuration);
  AmbariLdapConfiguration createLdapConfiguration(Map<String, String> configuration);
 }
\ No newline at end of file
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
index b32d1ed9fb..ac9c1bc8ee 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
@@ -84,7 +84,7 @@ public class AmbariLdapConfigurationProvider implements Provider<AmbariLdapConfi
     configEntities = ambariConfigurationDAOProvider.get().findByCategory("ldap-configuration");
 
     if (configEntities != null) {
      Map<String, Object> properties = toProperties(configEntities);
      Map<String, String> properties = toProperties(configEntities);
       instance = ldapConfigurationFactory.createLdapConfiguration(properties);
     }
 
@@ -93,8 +93,8 @@ public class AmbariLdapConfigurationProvider implements Provider<AmbariLdapConfi
     return instance;
   }
 
  private Map<String, Object> toProperties(List<AmbariConfigurationEntity> configEntities) {
    Map<String, Object> map = new HashMap<>();
  private Map<String, String> toProperties(List<AmbariConfigurationEntity> configEntities) {
    Map<String, String> map = new HashMap<>();
 
     for (AmbariConfigurationEntity entity : configEntities) {
       map.put(entity.getPropertyName(), entity.getPropertyValue());
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/metadata/AmbariServiceAlertDefinitions.java b/ambari-server/src/main/java/org/apache/ambari/server/metadata/AmbariServiceAlertDefinitions.java
index 1e20571ab2..d6b0c995ff 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/metadata/AmbariServiceAlertDefinitions.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/metadata/AmbariServiceAlertDefinitions.java
@@ -23,8 +23,8 @@ import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;
 
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.state.alert.AlertDefinition;
 import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
 import org.slf4j.Logger;
@@ -36,7 +36,7 @@ import com.google.inject.Singleton;
 /**
  * The {@link AmbariServiceAlertDefinitions} class is used to represent the
  * alerts defined in {@code alerts.json} which are for
 * {@link Components#AMBARI_AGENT} and {@link Components#AMBARI_SERVER}. These
 * {@link RootComponent#AMBARI_AGENT} and {@link RootComponent#AMBARI_SERVER}. These
  * alerts are bound to the host and are not part of a cluster or hadoop service.
  */
 @Singleton
@@ -67,7 +67,7 @@ public class AmbariServiceAlertDefinitions {
    * Gets all of the {@link AlertDefinition}s that exist on the path for all
    * agent hosts.
    *
   * @return the alerts with {@link Components#AMBARI_AGENT} as the component
   * @return the alerts with {@link RootComponent#AMBARI_AGENT} as the component
    *         and {@code AMBARI} as the service.
    */
   public List<AlertDefinition> getAgentDefinitions() {
@@ -75,15 +75,15 @@ public class AmbariServiceAlertDefinitions {
       return m_agentDefinitions;
     }
 
    m_agentDefinitions = getDefinitions(Components.AMBARI_AGENT);
    m_agentDefinitions = getDefinitions(RootComponent.AMBARI_AGENT);
     return m_agentDefinitions;
   }
 
   /**
    * Gets all of the {@link AlertDefinition}s that exist on the path for
   * {@link Components#AMBARI_SERVER}.
   * {@link RootComponent#AMBARI_SERVER}.
    *
   * @return the alerts with {@link Components#AMBARI_SERVER} as the component
   * @return the alerts with {@link RootComponent#AMBARI_SERVER} as the component
    *         and {@code AMBARI} as the service.
    */
   public List<AlertDefinition> getServerDefinitions() {
@@ -91,7 +91,7 @@ public class AmbariServiceAlertDefinitions {
       return m_serverDefinitions;
     }
 
    m_serverDefinitions = getDefinitions(Components.AMBARI_SERVER);
    m_serverDefinitions = getDefinitions(RootComponent.AMBARI_SERVER);
     return m_serverDefinitions;
   }
 
@@ -104,7 +104,7 @@ public class AmbariServiceAlertDefinitions {
    * @return the alert definitions for {@code AMBARI} service for the given
    *         component.
    */
  private List<AlertDefinition> getDefinitions(Components component) {
  private List<AlertDefinition> getDefinitions(RootComponent component) {
     List<AlertDefinition> definitions = new ArrayList<>();
 
     InputStream inputStream = ClassLoader.getSystemResourceAsStream("alerts.json");
@@ -112,7 +112,7 @@ public class AmbariServiceAlertDefinitions {
 
     try {
       Set<AlertDefinition> allDefinitions = m_factory.getAlertDefinitions(
          reader, Services.AMBARI.name());
          reader, RootService.AMBARI.name());
 
       String componentName = component.name();
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAO.java
index cda03f307b..424910b035 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAO.java
@@ -25,7 +25,8 @@ import javax.persistence.EntityManager;
 import javax.persistence.TypedQuery;
 
 import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.controller.internal.AlertDefinitionResourceProvider;
 import org.apache.ambari.server.events.AlertDefinitionChangedEvent;
 import org.apache.ambari.server.events.AlertDefinitionDeleteEvent;
@@ -300,10 +301,10 @@ public class AlertDefinitionDAO {
     query.setParameter("clusterId", clusterId);
 
     query.setParameter("serviceName",
        RootServiceResponseFactory.Services.AMBARI.name());
        RootService.AMBARI.name());
 
     query.setParameter("componentName",
        RootServiceResponseFactory.Components.AMBARI_AGENT.name());
        RootComponent.AMBARI_AGENT.name());
 
     return daoUtils.selectList(query);
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDispatchDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDispatchDAO.java
index 5c6a82ffcf..1746048113 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDispatchDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AlertDispatchDAO.java
@@ -33,7 +33,7 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.api.query.JpaPredicateVisitor;
 import org.apache.ambari.server.api.query.JpaSortBuilder;
 import org.apache.ambari.server.controller.AlertNoticeRequest;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.utilities.PredicateHelper;
 import org.apache.ambari.server.orm.RequiresSession;
@@ -452,7 +452,7 @@ public class AlertDispatchDAO {
 
     // AMBARI is a special service that we let through, otherwise we need to
     // verify that the service exists before we create the default group
    String ambariServiceName = Services.AMBARI.name();
    String ambariServiceName = RootService.AMBARI.name();
     if (!ambariServiceName.equals(serviceName)) {
       Cluster cluster = m_clusters.get().getClusterById(clusterId);
       Map<String, Service> services = cluster.getServices();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/alert/AlertDefinitionHash.java b/ambari-server/src/main/java/org/apache/ambari/server/state/alert/AlertDefinitionHash.java
index 15f7048ab0..a1c7249805 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/alert/AlertDefinitionHash.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/alert/AlertDefinitionHash.java
@@ -37,8 +37,8 @@ import org.apache.ambari.server.ClusterNotFoundException;
 import org.apache.ambari.server.agent.ActionQueue;
 import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
 import org.apache.ambari.server.agent.AlertDefinitionCommand;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
 import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
 import org.apache.ambari.server.state.Cluster;
@@ -375,8 +375,8 @@ public class AlertDefinitionHash {
       return affectedHosts;
     }
 
    String ambariServiceName = Services.AMBARI.name();
    String agentComponentName = Components.AMBARI_AGENT.name();
    String ambariServiceName = RootService.AMBARI.name();
    String agentComponentName = RootComponent.AMBARI_AGENT.name();
 
     // intercept host agent alerts; they affect all hosts
     if (ambariServiceName.equals(definitionServiceName)
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
index 9c0b0ca01a..8f5e4f48a5 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
@@ -57,7 +57,7 @@ import org.apache.ambari.server.controller.AmbariSessionManager;
 import org.apache.ambari.server.controller.ClusterResponse;
 import org.apache.ambari.server.controller.ConfigurationResponse;
 import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
 import org.apache.ambari.server.events.AmbariEvent.AmbariEventType;
 import org.apache.ambari.server.events.ClusterConfigChangedEvent;
@@ -2050,7 +2050,7 @@ public class ClusterImpl implements Cluster {
       // server-side events either don't have a service name or are AMBARI;
       // either way they are not handled by this method since it expects a
       // real service and component
      if (StringUtils.isBlank(serviceName) || Services.AMBARI.name().equals(serviceName)) {
      if (StringUtils.isBlank(serviceName) || RootService.AMBARI.name().equals(serviceName)) {
         continue;
       }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/services/AmbariServerAlertService.java b/ambari-server/src/main/java/org/apache/ambari/server/state/services/AmbariServerAlertService.java
index d3237a94b8..305f693741 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/services/AmbariServerAlertService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/services/AmbariServerAlertService.java
@@ -29,7 +29,7 @@ import java.util.concurrent.TimeUnit;
 
 import org.apache.ambari.server.AmbariService;
 import org.apache.ambari.server.alerts.AlertRunnable;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
 import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
 import org.apache.ambari.server.state.Cluster;
@@ -133,7 +133,7 @@ public class AmbariServerAlertService extends AbstractScheduledService {
   /**
    * {@inheritDoc}
    * <p/>
   * Compares all known {@link Components#AMBARI_SERVER} alerts with those that
   * Compares all known {@link RootComponent#AMBARI_SERVER} alerts with those that
    * are scheduled. If any are not scheduled or have their intervals changed,
    * then reschedule those.
    */
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java b/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java
index 959db1547b..2afbf8aa87 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/api/services/stackadvisor/commands/StackAdvisorCommandTest.java
@@ -301,9 +301,9 @@ public class StackAdvisorCommandTest {
       "items",
       list(
         map(
          "AmbariConfiguration",
          "Configuration",
           map(
            "data", list(ldapConfigData)
            "properties", ldapConfigData
           )
         )
       )
@@ -380,7 +380,7 @@ public class StackAdvisorCommandTest {
       "items",
       list(
         map(
          "AmbariConfiguration",
          "Configuration",
           map(
             "data",
             list(ldapConfigData, ldapConfigData)
@@ -417,7 +417,7 @@ public class StackAdvisorCommandTest {
       .build();
 
     Map<String, Object> ldapConfig = map(
      "AmbariConfiguration",
      "Configuration",
       map(
         "data",
         list(
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
index 7094caae87..d95dcef2e0 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
@@ -8248,13 +8248,13 @@ public class AmbariManagementControllerTest {
 
     RootServiceRequest request = new RootServiceRequest(null);
     Set<RootServiceResponse> responses = controller.getRootServices(Collections.singleton(request));
    Assert.assertEquals(RootServiceResponseFactory.Services.values().length, responses.size());
    Assert.assertEquals(RootService.values().length, responses.size());
 
    RootServiceRequest requestWithParams = new RootServiceRequest(RootServiceResponseFactory.Services.AMBARI.toString());
    RootServiceRequest requestWithParams = new RootServiceRequest(RootService.AMBARI.toString());
     Set<RootServiceResponse> responsesWithParams = controller.getRootServices(Collections.singleton(requestWithParams));
     Assert.assertEquals(1, responsesWithParams.size());
     for (RootServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), RootServiceResponseFactory.Services.AMBARI.toString());
      Assert.assertEquals(responseWithParams.getServiceName(), RootService.AMBARI.toString());
     }
 
     RootServiceRequest invalidRequest = new RootServiceRequest(NON_EXT_VALUE);
@@ -8268,18 +8268,18 @@ public class AmbariManagementControllerTest {
   @Test
   public void testGetRootServiceComponents() throws Exception {
 
    RootServiceComponentRequest request = new RootServiceComponentRequest(RootServiceResponseFactory.Services.AMBARI.toString(), null);
    RootServiceComponentRequest request = new RootServiceComponentRequest(RootService.AMBARI.toString(), null);
     Set<RootServiceComponentResponse> responses = controller.getRootServiceComponents(Collections.singleton(request));
    Assert.assertEquals(RootServiceResponseFactory.Services.AMBARI.getComponents().length, responses.size());
    Assert.assertEquals(RootService.AMBARI.getComponents().length, responses.size());
 
     RootServiceComponentRequest requestWithParams = new RootServiceComponentRequest(
        RootServiceResponseFactory.Services.AMBARI.toString(),
        RootServiceResponseFactory.Services.AMBARI.getComponents()[0].toString());
        RootService.AMBARI.toString(),
        RootService.AMBARI.getComponents()[0].toString());
 
     Set<RootServiceComponentResponse> responsesWithParams = controller.getRootServiceComponents(Collections.singleton(requestWithParams));
     Assert.assertEquals(1, responsesWithParams.size());
     for (RootServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), RootServiceResponseFactory.Services.AMBARI.getComponents()[0].toString());
      Assert.assertEquals(responseWithParams.getComponentName(), RootService.AMBARI.getComponents()[0].toString());
     }
 
     RootServiceComponentRequest invalidRequest = new RootServiceComponentRequest(NON_EXT_VALUE, NON_EXT_VALUE);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/RootServiceResponseFactoryTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/RootServiceResponseFactoryTest.java
index c27ef7e0dd..e194115c17 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/RootServiceResponseFactoryTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/RootServiceResponseFactoryTest.java
@@ -29,7 +29,6 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.H2DatabaseCleaner;
 import org.apache.ambari.server.ObjectNotFoundException;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
 import org.junit.After;
@@ -66,13 +65,13 @@ public class RootServiceResponseFactoryTest {
     // Request a null service name
     RootServiceRequest request = new RootServiceRequest(null);
     Set<RootServiceResponse> rootServices = responseFactory.getRootServices(request);
    assertEquals(RootServiceResponseFactory.Services.values().length,
    assertEquals(RootService.values().length,
         rootServices.size());
 
     // null request
     request = null;
     rootServices = responseFactory.getRootServices(request);
    assertEquals(RootServiceResponseFactory.Services.values().length,
    assertEquals(RootService.values().length,
         rootServices.size());
 
     // Request nonexistent service
@@ -85,12 +84,12 @@ public class RootServiceResponseFactoryTest {
 
     // Request existent service
     request = new RootServiceRequest(
        RootServiceResponseFactory.Services.AMBARI.name());
        RootService.AMBARI.name());
 
     rootServices = responseFactory.getRootServices(request);
     assertEquals(1, rootServices.size());
     assertTrue(rootServices.contains(new RootServiceResponse(
        RootServiceResponseFactory.Services.AMBARI.name())));
        RootService.AMBARI.name())));
   }
 
   @Test
@@ -106,7 +105,7 @@ public class RootServiceResponseFactoryTest {
       assertTrue(e instanceof ObjectNotFoundException);
     }
 
    RootServiceResponseFactory.Components ambariServerComponent = RootServiceResponseFactory.Components.AMBARI_SERVER;
    RootComponent ambariServerComponent = RootComponent.AMBARI_SERVER;
 
     // Request null service name, not-null component name
     request = new RootServiceComponentRequest(null, ambariServerComponent.name());
@@ -118,18 +117,18 @@ public class RootServiceResponseFactoryTest {
     }
 
     // Request existent service name, null component name
    String serviceName = RootServiceResponseFactory.Services.AMBARI.name();
    String serviceName = RootService.AMBARI.name();
     request = new RootServiceComponentRequest(serviceName, null);
 
     rootServiceComponents = responseFactory.getRootServiceComponents(request);
     assertEquals(
        RootServiceResponseFactory.Services.AMBARI.getComponents().length,
        RootService.AMBARI.getComponents().length,
         rootServiceComponents.size());
 
     String ambariVersion = ambariMetaInfo.getServerVersion();
 
    for (int i = 0; i < RootServiceResponseFactory.Services.AMBARI.getComponents().length; i++) {
      Components component = RootServiceResponseFactory.Services.AMBARI.getComponents()[i];
    for (int i = 0; i < RootService.AMBARI.getComponents().length; i++) {
      RootComponent component = RootService.AMBARI.getComponents()[i];
 
       if (component.name().equals(ambariServerComponent.name())) {
         for (RootServiceComponentResponse response : rootServiceComponents) {
@@ -148,14 +147,14 @@ public class RootServiceResponseFactoryTest {
 
     // Request existent service name, existent component name
     request = new RootServiceComponentRequest(
        RootServiceResponseFactory.Services.AMBARI.name(),
        RootServiceResponseFactory.Services.AMBARI.getComponents()[0].name());
        RootService.AMBARI.name(),
        RootService.AMBARI.getComponents()[0].name());
 
     rootServiceComponents = responseFactory.getRootServiceComponents(request);
     assertEquals(1, rootServiceComponents.size());
     for (RootServiceComponentResponse response : rootServiceComponents) {
       if (response.getComponentName().equals(
          RootServiceResponseFactory.Services.AMBARI.getComponents()[0].name())) {
          RootService.AMBARI.getComponents()[0].name())) {
         assertEquals(ambariVersion, response.getComponentVersion());
         assertEquals(2, response.getProperties().size());
         assertTrue(response.getProperties().containsKey("jdk_location"));
@@ -166,7 +165,7 @@ public class RootServiceResponseFactoryTest {
     // Request existent service name, and component, not belongs to requested
     // service
     request = new RootServiceComponentRequest(
        RootServiceResponseFactory.Services.AMBARI.name(), "XXX");
        RootService.AMBARI.name(), "XXX");
     
     try {
       rootServiceComponents = responseFactory.getRootServiceComponents(request);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProviderTest.java
similarity index 78%
rename from ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
rename to ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProviderTest.java
index a2ecb271ed..5016160c65 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentConfigurationResourceProviderTest.java
@@ -14,8 +14,10 @@
 
 package org.apache.ambari.server.controller.internal;
 
import static org.apache.ambari.server.controller.internal.AmbariConfigurationResourceProvider.AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.AmbariConfigurationResourceProvider.AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_CATEGORY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_COMPONENT_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_SERVICE_NAME_PROPERTY_ID;
 import static org.easymock.EasyMock.anyObject;
 import static org.easymock.EasyMock.capture;
 import static org.easymock.EasyMock.eq;
@@ -33,6 +35,9 @@ import java.util.TreeMap;
 
 import javax.persistence.EntityManager;
 
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.predicate.AndPredicate;
 import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
@@ -55,7 +60,9 @@ import com.google.inject.AbstractModule;
 import com.google.inject.Guice;
 import com.google.inject.Injector;
 
public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
import junit.framework.Assert;

public class RootServiceComponentConfigurationResourceProviderTest extends EasyMockSupport {
 
   private static final String CATEGORY_NAME_1 = "test-category-1";
   private static final String CATEGORY_NAME_2 = "test-category-2";
@@ -93,7 +100,7 @@ public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
   private void testCreateResources(Authentication authentication) throws Exception {
     Injector injector = createInjector();
 
    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);
    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);
 
     Set<Map<String, Object>> propertySets = new HashSet<>();
 
@@ -165,12 +172,9 @@ public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
   private void testDeleteResources(Authentication authentication) throws Exception {
     Injector injector = createInjector();
 
    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);
    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);
 
    Predicate predicate = new PredicateBuilder()
        .property(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(CATEGORY_NAME_1)
        .toPredicate();
    Predicate predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1);
 
     Request request = createMock(Request.class);
 
@@ -218,12 +222,9 @@ public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
   private void testGetResources(Authentication authentication) throws Exception {
     Injector injector = createInjector();
 
    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);
    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);
 
    Predicate predicate = new PredicateBuilder()
        .property(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(CATEGORY_NAME_1)
        .toPredicate();
    Predicate predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1);
 
     Request request = createMock(Request.class);
     expect(request.getPropertyIds()).andReturn(null).anyTimes();
@@ -243,22 +244,22 @@ public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
 
     verifyAll();
 
    junit.framework.Assert.assertNotNull(response);
    junit.framework.Assert.assertEquals(1, response.size());
    Assert.assertNotNull(response);
    Assert.assertEquals(1, response.size());
 
     Resource resource = response.iterator().next();
    junit.framework.Assert.assertEquals(Resource.Type.AmbariConfiguration, resource.getType());
    Assert.assertEquals(Resource.Type.RootServiceComponentConfiguration, resource.getType());
 
     Map<String, Map<String, Object>> propertiesMap = resource.getPropertiesMap();
    junit.framework.Assert.assertEquals(2, propertiesMap.size());
    Assert.assertEquals(2, propertiesMap.size());
 
    junit.framework.Assert.assertEquals(CATEGORY_NAME_1, propertiesMap.get(Resource.Type.AmbariConfiguration.name()).get("category"));
    Assert.assertEquals(CATEGORY_NAME_1, propertiesMap.get(RootServiceComponentConfigurationResourceProvider.RESOURCE_KEY).get("category"));
 
    Map<String, Object> retrievedProperties = propertiesMap.get(Resource.Type.AmbariConfiguration.name() + "/properties");
    junit.framework.Assert.assertEquals(2, retrievedProperties.size());
    Map<String, Object> retrievedProperties = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.RESOURCE_KEY + "/properties");
    Assert.assertEquals(2, retrievedProperties.size());
 
     for (Map.Entry<String, String> entry : properties.entrySet()) {
      junit.framework.Assert.assertEquals(entry.getValue(), retrievedProperties.get(entry.getKey()));
      Assert.assertEquals(entry.getValue(), retrievedProperties.get(entry.getKey()));
     }
   }
 
@@ -290,12 +291,9 @@ public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
   private void testUpdateResources(Authentication authentication) throws Exception {
     Injector injector = createInjector();
 
    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);
    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);
 
    Predicate predicate = new PredicateBuilder()
        .property(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(CATEGORY_NAME_1)
        .toPredicate();
    Predicate predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1);
 
     Set<Map<String, Object>> propertySets = new HashSet<>();
 
@@ -329,6 +327,22 @@ public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
     validateCapturedProperties(properties1, capturedProperties1);
   }
 
  private Predicate createPredicate(String serviceName, String componentName, String categoryName) {
    Predicate predicateService = new PredicateBuilder()
        .property(CONFIGURATION_SERVICE_NAME_PROPERTY_ID)
        .equals(serviceName)
        .toPredicate();
    Predicate predicateComponent = new PredicateBuilder()
        .property(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID)
        .equals(componentName)
        .toPredicate();
    Predicate predicateCategory = new PredicateBuilder()
        .property(CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(categoryName)
        .toPredicate();
    return new AndPredicate(predicateService, predicateComponent, predicateCategory);
  }

   private List<AmbariConfigurationEntity> createEntities(String categoryName, Map<String, String> properties) {
     List<AmbariConfigurationEntity> entities = new ArrayList<>();
 
@@ -345,23 +359,25 @@ public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
 
   private Map<String, Object> toRequestProperties(String categoryName1, Map<String, String> properties) {
     Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName1);
    requestProperties.put(CONFIGURATION_SERVICE_NAME_PROPERTY_ID, "AMBARI");
    requestProperties.put(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID, "AMBARI_SERVER");
    requestProperties.put(CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName1);
     for (Map.Entry<String, String> entry : properties.entrySet()) {
      requestProperties.put(AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID + "/" + entry.getKey(), entry.getValue());
      requestProperties.put(CONFIGURATION_PROPERTIES_PROPERTY_ID + "/" + entry.getKey(), entry.getValue());
     }
     return requestProperties;
   }
 
   private void validateCapturedProperties(Map<String, String> expectedProperties, Capture<Map<String, String>> capturedProperties) {
    junit.framework.Assert.assertTrue(capturedProperties.hasCaptured());
    Assert.assertTrue(capturedProperties.hasCaptured());
 
     Map<String, String> properties = capturedProperties.getValue();
    junit.framework.Assert.assertNotNull(properties);
    Assert.assertNotNull(properties);
 
     // Convert the Map to a TreeMap to help with comparisons
     expectedProperties = new TreeMap<>(expectedProperties);
     properties = new TreeMap<>(properties);
    junit.framework.Assert.assertEquals(expectedProperties, properties);
    Assert.assertEquals(expectedProperties, properties);
   }
 
   private Injector createInjector() throws Exception {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProviderTest.java
index a202516650..d3f9bd1a01 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentPropertyProviderTest.java
@@ -24,7 +24,8 @@ import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.utilities.PropertyHelper;
@@ -34,27 +35,27 @@ import org.junit.Test;
 public class RootServiceComponentPropertyProviderTest {
   @Test
   public void testPopulateResources_AmbariServer_None() throws Exception {
    testPopulateResources(RootServiceResponseFactory.Components.AMBARI_SERVER.name(), false, false, false, false);
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), false, false, false, false);
   }
 
   @Test
   public void testPopulateResources_AmbariServer_CiphersAndJCEPolicy() throws Exception {
    testPopulateResources(RootServiceResponseFactory.Components.AMBARI_SERVER.name(), true, true, true, true);
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), true, true, true, true);
   }
 
   @Test
   public void testPopulateResources_AmbariServer_JCEPolicy() throws Exception {
    testPopulateResources(RootServiceResponseFactory.Components.AMBARI_SERVER.name(), false, true, false, true);
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), false, true, false, true);
   }
 
   @Test
   public void testPopulateResources_AmbariServer_Ciphers() throws Exception {
    testPopulateResources(RootServiceResponseFactory.Components.AMBARI_SERVER.name(), true, false, true, false);
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), true, false, true, false);
   }
 
   @Test
   public void testPopulateResources_AmbariAgent_CiphersAndJCEPolicy() throws Exception {
    testPopulateResources(RootServiceResponseFactory.Components.AMBARI_AGENT.name(), true, true, false, false);
    testPopulateResources(RootComponent.AMBARI_AGENT.name(), true, true, false, false);
   }
 
   public void testPopulateResources(String componentName,
@@ -64,7 +65,7 @@ public class RootServiceComponentPropertyProviderTest {
     Resource resource = new ResourceImpl(Resource.Type.RootService);
 
     resource.setProperty(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID, componentName);
    resource.setProperty(RootServiceComponentResourceProvider.SERVICE_NAME_PROPERTY_ID, RootServiceResponseFactory.Services.AMBARI.name());
    resource.setProperty(RootServiceComponentResourceProvider.SERVICE_NAME_PROPERTY_ID, RootService.AMBARI.name());
 
     HashSet<String> requestIds = new HashSet<>();
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProviderTest.java
index 222340b4e7..4316647e8e 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/RootServiceComponentResourceProviderTest.java
@@ -29,8 +29,9 @@ import java.util.Map;
 import java.util.Set;
 
 import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.controller.RootServiceComponentResponse;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
 import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
@@ -50,12 +51,12 @@ public class RootServiceComponentResourceProviderTest {
     AmbariManagementController managementController = createMock(AmbariManagementController.class);
 
     Set<RootServiceComponentResponse> allResponse = new HashSet<>();
    String serviceName = RootServiceResponseFactory.Services.AMBARI.name();
    String serviceName = RootService.AMBARI.name();
     Map<String, String> emptyMap = Collections.emptyMap();
     allResponse.add(new RootServiceComponentResponse(serviceName, "component1", "1.1.1", emptyMap));
     allResponse.add(new RootServiceComponentResponse(serviceName, "component2", "1.1.1", emptyMap));
     allResponse.add(new RootServiceComponentResponse(serviceName, "component3", "1.1.1", emptyMap));
    allResponse.add(new RootServiceComponentResponse(serviceName, RootServiceResponseFactory.Components.AMBARI_SERVER.name(), "1.1.1", emptyMap));
    allResponse.add(new RootServiceComponentResponse(serviceName, RootComponent.AMBARI_SERVER.name(), "1.1.1", emptyMap));
 
     Set<RootServiceComponentResponse> nameResponse = new HashSet<>();
     nameResponse.add(new RootServiceComponentResponse(serviceName, "component4", "1.1.1", emptyMap));
@@ -92,7 +93,7 @@ public class RootServiceComponentResourceProviderTest {
       String componentName = (String) resource.getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID);
       String componentVersion = (String) resource.getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_VERSION_PROPERTY_ID);
       Long server_clock = (Long) resource.getPropertyValue(RootServiceComponentResourceProvider.SERVER_CLOCK_PROPERTY_ID);
      if (componentName.equals(RootServiceResponseFactory.Components.AMBARI_SERVER.name())){
      if (componentName.equals(RootComponent.AMBARI_SERVER.name())){
         Assert.assertNotNull(server_clock);
       } else {
         Assert.assertNull(server_clock);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java
index 30f5e22ee0..3917cdf034 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/LdapModuleFunctionalTest.java
@@ -125,8 +125,8 @@ public class LdapModuleFunctionalTest {
     return ldapPropsMap;
   }
 
  private static Map<String, Object> getADProps() {
    Map<String, Object> ldapPropsMap = Maps.newHashMap();
  private static Map<String, String> getADProps() {
    Map<String, String> ldapPropsMap = Maps.newHashMap();
 
 
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java
index aa26498d92..10822504e7 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/domain/TestAmbariLdapConfigurationFactory.java
@@ -23,7 +23,7 @@ import java.util.Map;
 public class TestAmbariLdapConfigurationFactory implements AmbariLdapConfigurationFactory {
 
   @Override
  public AmbariLdapConfiguration createLdapConfiguration(Map<String, Object> configuration) {
  public AmbariLdapConfiguration createLdapConfiguration(Map<String, String> configuration) {
     return new AmbariLdapConfiguration(configuration);
   }
 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java
index db0e5a96ad..97ce30eb6a 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/AmbariLdapFacadeTest.java
@@ -163,12 +163,12 @@ public class AmbariLdapFacadeTest extends EasyMockSupport {
   public void testShouldLdapAttributeDetectionDelegateToTheRightServiceCalls() throws Exception {
 
     // configuration map with user attributes detected
    Map<String, Object> userConfigMap = Maps.newHashMap();
    Map<String, String> userConfigMap = Maps.newHashMap();
     userConfigMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), "uid");
     AmbariLdapConfiguration userAttrDecoratedConfig = ambariLdapConfigurationFactory.createLdapConfiguration(userConfigMap);
 
     // configuration map with user+group attributes detected
    Map<String, Object> groupConfigMap = Maps.newHashMap(userConfigMap);
    Map<String, String> groupConfigMap = Maps.newHashMap(userConfigMap);
     groupConfigMap.put(AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE.key(), "dn");
     AmbariLdapConfiguration groupAttrDecoratedConfig = ambariLdapConfigurationFactory.createLdapConfiguration(groupConfigMap);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java
index 09dea1c210..a44bf7cadd 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapAttributeDetectionServiceTest.java
@@ -78,7 +78,7 @@ public class DefaultLdapAttributeDetectionServiceTest extends EasyMockSupport {
   @SuppressWarnings("unchecked")
   public void shouldLdapUserAttributeDetection() throws Exception {
     // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    Map<String, String> configMap = Maps.newHashMap();
     configMap.put(AmbariLdapConfigKeys.USER_SEARCH_BASE.key(), "dc=example,dc=com");
     AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);
 
@@ -109,7 +109,7 @@ public class DefaultLdapAttributeDetectionServiceTest extends EasyMockSupport {
   @Test(expected = AmbariLdapException.class)
   public void testShouldUserAttributeDetectionFailWhenLdapOerationFails() throws Exception {
     // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    Map<String, String> configMap = Maps.newHashMap();
     configMap.put(AmbariLdapConfigKeys.USER_SEARCH_BASE.key(), "dc=example,dc=com");
     AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);
 
@@ -129,7 +129,7 @@ public class DefaultLdapAttributeDetectionServiceTest extends EasyMockSupport {
   @SuppressWarnings("unchecked")
   public void shouldLdapGroupAttributeDetection() throws Exception {
     // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    Map<String, String> configMap = Maps.newHashMap();
     configMap.put(AmbariLdapConfigKeys.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
     AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);
 
@@ -160,7 +160,7 @@ public class DefaultLdapAttributeDetectionServiceTest extends EasyMockSupport {
   @Test(expected = AmbariLdapException.class)
   public void testShouldGroupAttributeDetectionFailWhenLdapOerationFails() throws Exception {
     // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    Map<String, String> configMap = Maps.newHashMap();
     configMap.put(AmbariLdapConfigKeys.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
     AmbariLdapConfiguration ldapConfiguration = ldapConfigurationFactory.createLdapConfiguration(configMap);
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java
index 4d6d2a6a50..ec78e5662a 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/ldap/service/ads/DefaultLdapConfigurationServiceTest.java
@@ -102,7 +102,7 @@ public class DefaultLdapConfigurationServiceTest extends EasyMockSupport {
   @Test
   public void testShouldUserAttributeConfigurationCheckSucceedWhenUserDnIsFound() throws Exception {
     // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    Map<String, String> configMap = Maps.newHashMap();
     configMap.put(AmbariLdapConfigKeys.USER_OBJECT_CLASS.key(), "person");
     configMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), "uid");
 
@@ -126,7 +126,7 @@ public class DefaultLdapConfigurationServiceTest extends EasyMockSupport {
   @Test(expected = AmbariLdapException.class)
   public void testShouldUserAttributeConfigurationCheckFailWhenNoUsersFound() throws Exception {
     // GIVEN
    Map<String, Object> configMap = Maps.newHashMap();
    Map<String, String> configMap = Maps.newHashMap();
     configMap.put(AmbariLdapConfigKeys.USER_OBJECT_CLASS.key(), "posixAccount");
     configMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), "dn");
 
@@ -155,7 +155,7 @@ public class DefaultLdapConfigurationServiceTest extends EasyMockSupport {
   public void testShouldGroupAttributeConfigurationCheckSucceedWhenGroupForUserDnIsFound() throws Exception {
     // GIVEN
 
    Map<String, Object> configMap = groupConfigObjectMap();
    Map<String, String> configMap = groupConfigObjectMap();
 
     SearchRequest sr = new SearchRequestImpl();
 
@@ -184,7 +184,7 @@ public class DefaultLdapConfigurationServiceTest extends EasyMockSupport {
   public void testShouldGroupAttributeConfigurationCheckFailWhenNoGroupsForUserDnFound() throws Exception {
     // GIVEN
 
    Map<String, Object> configMap = groupConfigObjectMap();
    Map<String, String> configMap = groupConfigObjectMap();
 
     SearchRequest sr = new SearchRequestImpl();
 
@@ -208,8 +208,8 @@ public class DefaultLdapConfigurationServiceTest extends EasyMockSupport {
 
   }
 
  private Map<String, Object> groupConfigObjectMap() {
    Map<String, Object> configMap = Maps.newHashMap();
  private Map<String, String> groupConfigObjectMap() {
    Map<String, String> configMap = Maps.newHashMap();
     configMap.put(AmbariLdapConfigKeys.GROUP_OBJECT_CLASS.key(), "groupOfNames");
     configMap.put(AmbariLdapConfigKeys.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");
     configMap.put(AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE.key(), "uid");
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/metadata/AgentAlertDefinitionsTest.java b/ambari-server/src/test/java/org/apache/ambari/server/metadata/AgentAlertDefinitionsTest.java
index adaf236b3d..cb234ea6d3 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/metadata/AgentAlertDefinitionsTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/metadata/AgentAlertDefinitionsTest.java
@@ -22,7 +22,7 @@ import java.util.List;
 import javax.persistence.EntityManager;
 
 import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.controller.RootComponent;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
 import org.apache.ambari.server.state.alert.AlertDefinition;
@@ -63,7 +63,7 @@ public class AgentAlertDefinitionsTest {
     Assert.assertEquals(3, definitions.size());
 
     for( AlertDefinition definition : definitions){
      Assert.assertEquals(Components.AMBARI_AGENT.name(),
      Assert.assertEquals(RootComponent.AMBARI_AGENT.name(),
           definition.getComponentName());
 
       Assert.assertEquals("AMBARI", definition.getServiceName());
@@ -80,7 +80,7 @@ public class AgentAlertDefinitionsTest {
     Assert.assertEquals(4, definitions.size());
 
     for (AlertDefinition definition : definitions) {
      Assert.assertEquals(Components.AMBARI_SERVER.name(),
      Assert.assertEquals(RootComponent.AMBARI_SERVER.name(),
           definition.getComponentName());
 
       Assert.assertEquals("AMBARI", definition.getServiceName());
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAOTest.java b/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAOTest.java
index d559e0c316..9ebc2e5481 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAOTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AlertDefinitionDAOTest.java
@@ -31,7 +31,8 @@ import java.util.TimeZone;
 import java.util.UUID;
 
 import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
 import org.apache.ambari.server.orm.OrmTestHelper;
@@ -148,8 +149,8 @@ public class AlertDefinitionDAOTest {
     for (; i < 15; i++) {
       AlertDefinitionEntity definition = new AlertDefinitionEntity();
       definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName(RootServiceResponseFactory.Services.AMBARI.name());
      definition.setComponentName(RootServiceResponseFactory.Components.AMBARI_AGENT.name());
      definition.setServiceName(RootService.AMBARI.name());
      definition.setComponentName(RootComponent.AMBARI_AGENT.name());
       definition.setClusterId(clusterId);
       definition.setHash(UUID.randomUUID().toString());
       definition.setScheduleInterval(60);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertReceivedListenerTest.java b/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertReceivedListenerTest.java
index 3ec6943d25..3056dd1e22 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertReceivedListenerTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertReceivedListenerTest.java
@@ -30,8 +30,8 @@ import javax.persistence.EntityManager;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.events.AlertReceivedEvent;
 import org.apache.ambari.server.events.AlertStateChangeEvent;
 import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
@@ -335,8 +335,8 @@ public class AlertReceivedListenerTest {
   @Test
   public void testAgentAlertFromInvalidHost() {
     String definitionName = ALERT_DEFINITION + "1";
    String serviceName = Services.AMBARI.name();
    String componentName = Components.AMBARI_AGENT.name();
    String serviceName = RootService.AMBARI.name();
    String componentName = RootComponent.AMBARI_AGENT.name();
 
     Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1,
         AlertState.OK);
@@ -374,8 +374,8 @@ public class AlertReceivedListenerTest {
   @Test
   public void testAmbariServerValidAlerts() {
     String definitionName = ALERT_DEFINITION + "1";
    String serviceName = Services.AMBARI.name();
    String componentName = Components.AMBARI_SERVER.name();
    String serviceName = RootService.AMBARI.name();
    String componentName = RootComponent.AMBARI_SERVER.name();
 
     Alert alert = new Alert(definitionName, null, serviceName, componentName, HOST1,
         AlertState.OK);
@@ -415,8 +415,8 @@ public class AlertReceivedListenerTest {
   @Test
   public void testMissingClusterAndInvalidHost() {
     String definitionName = ALERT_DEFINITION + "1";
    String serviceName = Services.AMBARI.name();
    String componentName = Components.AMBARI_AGENT.name();
    String serviceName = RootService.AMBARI.name();
    String componentName = RootComponent.AMBARI_AGENT.name();
 
     Alert alert1 = new Alert(definitionName, null, serviceName, componentName, HOST1,
         AlertState.OK);
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertStateChangedEventTest.java b/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertStateChangedEventTest.java
index bc8222c7c8..c3db717c26 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertStateChangedEventTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/state/alerts/AlertStateChangedEventTest.java
@@ -24,7 +24,7 @@ import java.util.List;
 import java.util.Set;
 
 import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.controller.RootService;
 import org.apache.ambari.server.events.AggregateAlertRecalculateEvent;
 import org.apache.ambari.server.events.AlertEvent;
 import org.apache.ambari.server.events.AlertStateChangeEvent;
@@ -501,7 +501,7 @@ public class AlertStateChangedEventTest extends EasyMockSupport {
     // create the definition for the AMBARI service
     AlertDefinitionEntity definition = createNiceMock(AlertDefinitionEntity.class);
     EasyMock.expect(definition.getDefinitionId()).andReturn(1L).anyTimes();
    EasyMock.expect(definition.getServiceName()).andReturn(Services.AMBARI.name()).anyTimes();
    EasyMock.expect(definition.getServiceName()).andReturn(RootService.AMBARI.name()).anyTimes();
     EasyMock.expect(definition.getLabel()).andReturn("ambari-foo-alert").anyTimes();
     EasyMock.expect(definition.getDescription()).andReturn("Ambari Foo Alert").anyTimes();
 
- 
2.19.1.windows.1

