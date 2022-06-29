From 4fa9ac5505d6604f1d74def8fc1ea6eeb3ea3fda Mon Sep 17 00:00:00 2001
From: Robert Levas <rlevas@hortonworks.com>
Date: Mon, 13 Nov 2017 13:11:42 -0500
Subject: [PATCH] AMBARI-22347.  Simplify Ambari configuration tables from
 AMBARI-21307 (rlevas)

--
 .../AmbariConfigurationRequestSwagger.java    |  14 +-
 .../services/AmbariConfigurationService.java  |  46 +-
 .../services/ldap/AmbariConfiguration.java    |  51 +-
 .../ldap/LdapConfigurationService.java        |   4 +-
 .../AmbariConfigurationResourceProvider.java  | 370 +++++++--------
 ...a => AmbariConfigurationChangedEvent.java} |  17 +-
 .../ambari/server/events/AmbariEvent.java     |   4 +-
 .../ldap/domain/AmbariLdapConfiguration.java  |  20 +-
 .../AmbariLdapConfigurationProvider.java      |  57 +--
 .../ads/LdapConnectionTemplateFactory.java    |   4 +-
 .../orm/dao/AmbariConfigurationDAO.java       | 146 ++++--
 .../entities/AmbariConfigurationEntity.java   |  94 +++-
 .../entities/AmbariConfigurationEntityPK.java |  88 ++++
 .../orm/entities/ConfigurationBaseEntity.java | 159 -------
 .../server/upgrade/SchemaUpgradeHelper.java   |   3 +-
 .../server/upgrade/UpgradeCatalog300.java     |  21 +-
 .../resources/Ambari-DDL-Derby-CREATE.sql     |  21 +-
 .../resources/Ambari-DDL-MySQL-CREATE.sql     |  20 +-
 .../resources/Ambari-DDL-Oracle-CREATE.sql    |  20 +-
 .../resources/Ambari-DDL-Postgres-CREATE.sql  |  19 +-
 .../Ambari-DDL-SQLAnywhere-CREATE.sql         |  20 +-
 .../resources/Ambari-DDL-SQLServer-CREATE.sql |  19 +-
 .../main/resources/META-INF/persistence.xml   |   1 -
 ...bariConfigurationResourceProviderTest.java | 444 +++++++++++-------
 .../orm/dao/AmbariConfigurationDAOTest.java   | 298 ++++++++++++
 .../server/upgrade/UpgradeCatalog300Test.java |  43 ++
 26 files changed, 1195 insertions(+), 808 deletions(-)
 rename ambari-server/src/main/java/org/apache/ambari/server/events/{AmbariLdapConfigChangedEvent.java => AmbariConfigurationChangedEvent.java} (59%)
 create mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntityPK.java
 delete mode 100644 ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java
 create mode 100644 ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAOTest.java

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
index 5e8094e9c7..2dca9f55f4 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationRequestSwagger.java
@@ -29,19 +29,9 @@ public interface AmbariConfigurationRequestSwagger extends ApiModel {
 
   interface AmbariConfigurationRequestInfo {
     @ApiModelProperty
    Long getId();
    String getCategoryName();
 
     @ApiModelProperty
    Map<String, Object> getData();

    @ApiModelProperty
    String getType();

    @ApiModelProperty
    Long getVersion();

    @ApiModelProperty(name = "version_tag")
    String getVersionTag();
    Map<String, Object> getProperties();
   }

 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
index 38ae7669db..86ed666f41 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariConfigurationService.java
@@ -46,21 +46,21 @@ import io.swagger.annotations.ApiResponses;
  * Ambari configuration resources may be shared with components and services in the cluster
  * (by recommending them as default values)
  *
 * Eg. LDAP configuration is stored as ambariconfiguration.
 * Eg. LDAP configuration is stored as AmbariConfiguration.
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
@@ -96,7 +96,7 @@ public class AmbariConfigurationService extends BaseService {
   })
   public Response createAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
     return handleRequest(headers, body, uri, Request.Type.POST, createResource(Resource.Type.AmbariConfiguration,
      Collections.EMPTY_MAP));
      Collections.emptyMap()));
   }
 
   @GET
@@ -108,10 +108,10 @@ public class AmbariConfigurationService extends BaseService {
     responseContainer = RESPONSE_CONTAINER_LIST)
   @ApiImplicitParams({
     @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
      defaultValue = "AmbariConfiguration/data, AmbariConfiguration/id, AmbariConfiguration/type",
      defaultValue = "AmbariConfiguration/properties, AmbariConfiguration/category",
       dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
     @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
      defaultValue = "AmbariConfiguration/id",
      defaultValue = "AmbariConfiguration/category",
       dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
     @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
     @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
@@ -123,11 +123,11 @@ public class AmbariConfigurationService extends BaseService {
   })
   public Response getAmbariConfigurations(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
     return handleRequest(headers, body, uri, Request.Type.GET, createResource(Resource.Type.AmbariConfiguration,
      Collections.EMPTY_MAP));
      Collections.emptyMap()));
   }
 
   @GET
  @Path("{configurationId}")
  @Path("{category}")
   @Produces(MediaType.TEXT_PLAIN)
   @ApiOperation(value = "Retrieve the details of an ambari configuration resource",
     nickname = "AmbariConfigurationService#getAmbariConfiguration",
@@ -142,13 +142,13 @@ public class AmbariConfigurationService extends BaseService {
     @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
   })
   public Response getAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                         @PathParam("configurationId") String configurationId) {
                                         @PathParam("category") String category) {
     return handleRequest(headers, body, uri, Request.Type.GET, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
      Collections.singletonMap(Resource.Type.AmbariConfiguration, category)));
   }
 
   @PUT
  @Path("{configurationId}")
  @Path("{category}")
   @Produces(MediaType.TEXT_PLAIN)
   @ApiOperation(value = "Updates ambari configuration resources ",
     nickname = "AmbariConfigurationService#updateAmbariConfiguration")
@@ -167,13 +167,13 @@ public class AmbariConfigurationService extends BaseService {
     @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
   })
   public Response updateAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                            @PathParam("configurationId") String configurationId) {
                                            @PathParam("category") String category) {
     return handleRequest(headers, body, uri, Request.Type.PUT, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
      Collections.singletonMap(Resource.Type.AmbariConfiguration, category)));
   }
 
   @DELETE
  @Path("{configurationId}")
  @Path("{category}")
   @Produces(MediaType.TEXT_PLAIN)
   @ApiOperation(value = "Deletes an ambari configuration resource",
     nickname = "AmbariConfigurationService#deleteAmbariConfiguration")
@@ -185,9 +185,9 @@ public class AmbariConfigurationService extends BaseService {
     @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
   })
   public Response deleteAmbariConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                            @PathParam("configurationId") String configurationId) {
                                            @PathParam("category") String category) {
     return handleRequest(headers, body, uri, Request.Type.DELETE, createResource(Resource.Type.AmbariConfiguration,
      Collections.singletonMap(Resource.Type.AmbariConfiguration, configurationId)));
      Collections.singletonMap(Resource.Type.AmbariConfiguration, category)));
   }
 
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
index b5cc9212da..e9f0b1e1a4 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/AmbariConfiguration.java
@@ -14,9 +14,7 @@
 
 package org.apache.ambari.server.api.services.ldap;
 
import java.util.Collections;
 import java.util.Map;
import java.util.Set;
 
 /**
  * Domain POJO representing generic ambari configuration data.
@@ -28,22 +26,7 @@ public class AmbariConfiguration {
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
  private Map<String, Object> properties = null;
 
   public String getType() {
     return type;
@@ -53,35 +36,11 @@ public class AmbariConfiguration {
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
  public Map<String, Object> getProperties() {
    return properties;
   }
 
  public void setCreatedTs(long createdTs) {
    this.createdTs = createdTs;
  public void setProperties(Map<String, Object> data) {
    this.properties = data;
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
index 13f8835655..00c13f6cfa 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/ldap/LdapConfigurationService.java
@@ -94,7 +94,7 @@ public class LdapConfigurationService extends AmbariConfigurationService {
       validateRequest(ldapConfigurationRequest);
 
       AmbariLdapConfiguration ambariLdapConfiguration = ambariLdapConfigurationFactory.createLdapConfiguration(
        ldapConfigurationRequest.getAmbariConfiguration().getData().iterator().next());
        ldapConfigurationRequest.getAmbariConfiguration().getProperties());
 
       LdapConfigOperation action = LdapConfigOperation.fromAction(ldapConfigurationRequest.getRequestInfo().getAction());
       switch (action) {
@@ -154,7 +154,7 @@ public class LdapConfigurationService extends AmbariConfigurationService {
     }
 
     if (null == ldapConfigurationRequest.getAmbariConfiguration()
      || ldapConfigurationRequest.getAmbariConfiguration().getData().size() != 1) {
      || ldapConfigurationRequest.getAmbariConfiguration().getProperties() != null) {
       errMsg = String.format("No / Invalid configuration data provided. Request: [%s]", ldapConfigurationRequest);
       LOGGER.error(errMsg);
       throw new IllegalArgumentException(errMsg);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
index 4f4cc7070d..a24400dffd 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProvider.java
@@ -14,15 +14,15 @@
 
 package org.apache.ambari.server.controller.internal;
 
import java.util.Calendar;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.EnumSet;
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;

import javax.inject.Inject;
import java.util.TreeMap;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
@@ -35,20 +35,18 @@ import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
 import org.apache.ambari.server.controller.spi.SystemException;
 import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
 import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
 import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.orm.entities.ConfigurationBaseEntity;
 import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.Inject;
 
 /**
  * Resource provider for AmbariConfiguration resources.
@@ -56,62 +54,24 @@ import com.google.inject.assistedinject.AssistedInject;
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
  static final String AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId("AmbariConfiguration", "category");
  static final String AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId("AmbariConfiguration", "properties");
 
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
  private static final Set<String> PROPERTIES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID,
          AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID)
      )
  );
 
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
  private static final Map<Resource.Type, String> PK_PROPERTY_MAP = Collections.unmodifiableMap(
      Collections.singletonMap(Resource.Type.AmbariConfiguration, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
   );
 
  private static final Set<String> PK_PROPERTY_IDS = Collections.unmodifiableSet(
      new HashSet<>(PK_PROPERTY_MAP.values())
  );
 
   @Inject
   private AmbariConfigurationDAO ambariConfigurationDAO;
@@ -119,210 +79,224 @@ public class AmbariConfigurationResourceProvider extends AbstractAuthorizedResou
   @Inject
   private AmbariEventPublisher publisher;
 

  private Gson gson;

  @AssistedInject
   public AmbariConfigurationResourceProvider() {
     super(PROPERTIES, PK_PROPERTY_MAP);
    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION));
 
    gson = new GsonBuilder().create();
    Set<RoleAuthorization> authorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
    setRequiredCreateAuthorizations(authorizations);
    setRequiredDeleteAuthorizations(authorizations);
    setRequiredUpdateAuthorizations(authorizations);
    setRequiredGetAuthorizations(authorizations);
   }
 
   @Override
   protected Set<String> getPKPropertyIds() {
    return Sets.newHashSet(ResourcePropertyId.ID.getPropertyId());
    return PK_PROPERTY_IDS;
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
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
 
    // todo filter by configuration type
    // notify subscribers about the configuration changes
    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED,
      ambariConfigurationEntity.getId()));
    createOrAddProperties(null, request.getProperties(), true);
 
     return getRequestStatus(null);
   }
 

   @Override
   protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Set<Resource> resources = Sets.newHashSet();
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
 
    // retrieves allconfigurations, filtering is done at a higher level
    List<AmbariConfigurationEntity> ambariConfigurationEntities = ambariConfigurationDAO.findAll();
    for (AmbariConfigurationEntity ambariConfigurationEntity : ambariConfigurationEntities) {
      try {
        resources.add(toResource(ambariConfigurationEntity, getPropertyIds()));
      } catch (AmbariException e) {
        LOGGER.error("Error while retrieving ambari configuration", e);
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
    }
    return resources;
    });
   }
 

   @Override
   protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
    UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
 
    Long idFromRequest = Long.valueOf((String) PredicateHelper.getProperties(predicate).get(ResourcePropertyId.ID.getPropertyId()));
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);
 
    if (null == idFromRequest) {
    if (null == categoryName) {
       LOGGER.debug("No resource id provided in the request");
     } else {
      LOGGER.debug("Deleting amari configuration with id: {}", idFromRequest);
      LOGGER.debug("Deleting Ambari configuration with id: {}", categoryName);
       try {
        ambariConfigurationDAO.removeByPK(idFromRequest);
        ambariConfigurationDAO.removeByCategory(categoryName);
       } catch (IllegalStateException e) {
         throw new NoSuchResourceException(e.getMessage());
       }

     }
 
     // notify subscribers about the configuration changes
    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED, idFromRequest));


    publisher.publish(new AmbariConfigurationChangedEvent(categoryName));
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
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
 
    try {

      AmbariConfigurationEntity entityFromRequest = getEntityFromRequest(request);
      persistedEntity.getConfigurationBaseEntity().setVersionTag(entityFromRequest.getConfigurationBaseEntity().getVersionTag());
      persistedEntity.getConfigurationBaseEntity().setVersion(entityFromRequest.getConfigurationBaseEntity().getVersion());
      persistedEntity.getConfigurationBaseEntity().setType(entityFromRequest.getConfigurationBaseEntity().getType());
      persistedEntity.getConfigurationBaseEntity().setConfigurationData(entityFromRequest.getConfigurationBaseEntity().getConfigurationData());
      persistedEntity.getConfigurationBaseEntity().setConfigurationAttributes(entityFromRequest.getConfigurationBaseEntity().getConfigurationAttributes());
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);
    createOrAddProperties(categoryName, request.getProperties(), false);
 
    return getRequestStatus(null);
  }
 
      ambariConfigurationDAO.update(persistedEntity);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage());
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
 
    publisher.publish(new AmbariLdapConfigChangedEvent(AmbariEvent.AmbariEventType.LDAP_CONFIG_CHANGED,
      persistedEntity.getId()));

  private Resource toResource(String categoryName, Map<String, String> properties, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    setResourceProperty(resource, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName, requestedIds);
    setResourceProperty(resource, AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID, properties, requestedIds);
    return resource;
  }
 
    return getRequestStatus(null);
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
 
  }
    for (Map.Entry<String, Object> entry : resourceProperties.entrySet()) {
      String propertyName = entry.getKey();
 
  private Resource toResource(AmbariConfigurationEntity entity, Set<String> requestedIds) throws AmbariException {
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
 
    if (null == entity) {
      throw new IllegalArgumentException("Null entity can't be transformed into a resource");
    if (categoryName == null) {
      categoryName = defaultCategoryName;
     }
 
    if (null == entity.getConfigurationBaseEntity()) {
      throw new IllegalArgumentException("Invalid configuration entity can't be transformed into a resource");
    if (StringUtils.isEmpty(categoryName)) {
      throw new SystemException("The configuration type must be set");
     }
    Resource resource = new ResourceImpl(Resource.Type.AmbariConfiguration);
    Set<Map<String, String>> configurationSet = gson.fromJson(entity.getConfigurationBaseEntity().getConfigurationData(), Set.class);
 
    setResourceProperty(resource, ResourcePropertyId.ID.getPropertyId(), entity.getId(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.TYPE.getPropertyId(), entity.getConfigurationBaseEntity().getType(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.DATA.getPropertyId(), configurationSet, requestedIds);
    setResourceProperty(resource, ResourcePropertyId.VERSION.getPropertyId(), entity.getConfigurationBaseEntity().getVersion(), requestedIds);
    setResourceProperty(resource, ResourcePropertyId.VERSION_TAG.getPropertyId(), entity.getConfigurationBaseEntity().getVersionTag(), requestedIds);
    if (properties.isEmpty()) {
      throw new SystemException("The configuration properties must be set");
    }
 
    return resource;
    return Collections.singletonMap(categoryName, properties);
   }
 
  private AmbariConfigurationEntity getEntityFromRequest(Request request) throws AmbariException {
  private Set<Resource> getAmbariConfigurationResources(Set<String> requestedIds, Map<String, Object> propertyMap) {
    Set<Resource> resources = new HashSet<>();
 
    AmbariConfigurationEntity ambariConfigurationEntity = new AmbariConfigurationEntity();
    ambariConfigurationEntity.setConfigurationBaseEntity(new ConfigurationBaseEntity());
    String categoryName = getStringProperty(propertyMap, AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID);
 
    // set of resource properties (eache entry in the set belongs to a different resource)
    Set<Map<String, Object>> resourcePropertiesSet = request.getProperties();
    List<AmbariConfigurationEntity> entities = (categoryName == null)
        ? ambariConfigurationDAO.findAll()
        : ambariConfigurationDAO.findByCategory(categoryName);
 
    if (resourcePropertiesSet.size() != 1) {
      throw new AmbariException("There must be only one resource specified in the request");
    }
    if (entities != null) {
      Map<String, Map<String, String>> configurations = new HashMap<>();
 
    // the configuration type must be set
    if (getValueFromResourceProperties(ResourcePropertyId.TYPE, resourcePropertiesSet.iterator().next()) == null) {
      throw new AmbariException("The configuration type must be set");
    }
      for (AmbariConfigurationEntity entity : entities) {
        String category = entity.getCategoryName();
        Map<String, String> properties = configurations.get(category);
 
        if (properties == null) {
          properties = new TreeMap<>();
          configurations.put(category, properties);
        }
 
    for (ResourcePropertyId resourcePropertyId : ResourcePropertyId.values()) {
      Object requestValue = getValueFromResourceProperties(resourcePropertyId, resourcePropertiesSet.iterator().next());
        properties.put(entity.getPropertyName(), entity.getPropertyValue());
      }
 
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
      for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
        resources.add(toResource(entry.getKey(), entry.getValue(), requestedIds));
       }
     }
    ambariConfigurationEntity.getConfigurationBaseEntity().setCreateTimestamp(Calendar.getInstance().getTimeInMillis());
    return ambariConfigurationEntity;
 
    return resources;
   }
 
  private Object getValueFromResourceProperties(ResourcePropertyId resourcePropertyIdEnum, Map<String, Object> resourceProperties) {
    LOGGER.debug("Locating resource property [{}] in the resource properties map ...", resourcePropertyIdEnum);
    Object requestValue = null;
  private String getStringProperty(Map<String, Object> propertyMap, String propertyId) {
    String value = null;
 
    if (resourceProperties.containsKey(resourcePropertyIdEnum.getPropertyId())) {
      requestValue = resourceProperties.get(resourcePropertyIdEnum.getPropertyId());
      LOGGER.debug("Found resource property {} in the resource properties map, value: {}", resourcePropertyIdEnum, requestValue);
    if (propertyMap != null) {
      Object o = propertyMap.get(propertyId);
      if (o instanceof String) {
        value = (String) o;
      }
     }
    return requestValue;
  }
 
    return value;
  }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariLdapConfigChangedEvent.java b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariConfigurationChangedEvent.java
similarity index 59%
rename from ambari-server/src/main/java/org/apache/ambari/server/events/AmbariLdapConfigChangedEvent.java
rename to ambari-server/src/main/java/org/apache/ambari/server/events/AmbariConfigurationChangedEvent.java
index 48799d793b..69a15b44ea 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariLdapConfigChangedEvent.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariConfigurationChangedEvent.java
@@ -15,23 +15,22 @@
 package org.apache.ambari.server.events;
 
 /**
 * Event signaling the creation or changing of an LDAP configuration entry.
 * Event signaling the creation or changing of an Ambari configuration entry.
  */
public class AmbariLdapConfigChangedEvent extends AmbariEvent {
public class AmbariConfigurationChangedEvent extends AmbariEvent {
 
  private Long configurationId;
  private final String categoryName;
 
   /**
    * Constructor.
    *
   * @param eventType the type of event (not {@code null}).
    */
  public AmbariLdapConfigChangedEvent(AmbariEventType eventType, Long configurationId) {
    super(eventType);
    this.configurationId = configurationId;
  public AmbariConfigurationChangedEvent(String categoryName) {
    super(AmbariEventType.AMBARI_CONFIGURATION_CHANGED);
    this.categoryName = categoryName;
   }
 
  public Long getConfigurationId() {
    return configurationId;
  public String getCategoryName() {
    return categoryName;
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java
index 0f9ff52147..0ece73b50f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/events/AmbariEvent.java
@@ -143,9 +143,9 @@ public abstract class AmbariEvent {
     USER_CREATED,
 
     /**
     * LDAP config changed event;
     * Ambari configuration changed event;
      */
    LDAP_CONFIG_CHANGED;
    AMBARI_CONFIGURATION_CHANGED;
 
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
index 8b26cd3e29..0c1ec0ae71 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/domain/AmbariLdapConfiguration.java
@@ -19,8 +19,8 @@ import java.util.Map;
 
 import javax.inject.Inject;
 
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -179,21 +179,25 @@ public class AmbariLdapConfiguration {
 
   @Override
   public boolean equals(Object o) {
    if (this == o) return true;
    if (this == o) {
      return true;
    }
 
    if (o == null || getClass() != o.getClass()) return false;
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
 
     AmbariLdapConfiguration that = (AmbariLdapConfiguration) o;
 
     return new EqualsBuilder()
      .append(configurationMap, that.configurationMap)
      .isEquals();
        .append(configurationMap, that.configurationMap)
        .isEquals();
   }
 
   @Override
   public int hashCode() {
     return new HashCodeBuilder(17, 37)
      .append(configurationMap)
      .toHashCode();
        .append(configurationMap)
        .toHashCode();
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
index c88d420e9a..b32d1ed9fb 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/AmbariLdapConfigurationProvider.java
@@ -14,14 +14,11 @@
 
 package org.apache.ambari.server.ldap.service;
 
import java.util.HashMap;
import java.util.List;
 import java.util.Map;
import java.util.Set;
 
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
 import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
@@ -32,8 +29,9 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
 
 /**
  * Provider implementation for LDAP configurations.
@@ -41,7 +39,7 @@ import com.google.gson.GsonBuilder;
  * It's responsible for managing LDAP configurations in the application.
  * Whenever requested, this provider returns an AmbariLdapConfiguration which is always in sync with the persisted LDAP
  * configuration resource.
 *
 * <p>
  * The provider receives notifications on CRUD operations related to the persisted resource and reloads the cached
  * configuration instance accordingly.
  */
@@ -60,8 +58,6 @@ public class AmbariLdapConfigurationProvider implements Provider<AmbariLdapConfi
   @Inject
   private AmbariLdapConfigurationFactory ldapConfigurationFactory;
 
  private Gson gson = new GsonBuilder().create();

   @Inject
   public AmbariLdapConfigurationProvider() {
   }
@@ -73,48 +69,45 @@ public class AmbariLdapConfigurationProvider implements Provider<AmbariLdapConfi
 
   @Override
   public AmbariLdapConfiguration get() {
    return instance != null ? instance : loadInstance(null);
    return instance != null ? instance : loadInstance();
   }
 
   /**
    * Loads the AmbariLdapConfiguration from the database.
    *
   * @param configurationId the configuration id
    * @return the AmbariLdapConfiguration instance
    */
  private AmbariLdapConfiguration loadInstance(Long configurationId) {
    AmbariConfigurationEntity configEntity = null;
  private AmbariLdapConfiguration loadInstance() {
    List<AmbariConfigurationEntity> configEntities;
 
     LOGGER.info("Loading LDAP configuration ...");
    if (null == configurationId) {
    configEntities = ambariConfigurationDAOProvider.get().findByCategory("ldap-configuration");
 
      LOGGER.debug("Initial loading of the ldap configuration ...");
      configEntity = ambariConfigurationDAOProvider.get().getLdapConfiguration();
    if (configEntities != null) {
      Map<String, Object> properties = toProperties(configEntities);
      instance = ldapConfigurationFactory.createLdapConfiguration(properties);
    }
 
    } else {
    LOGGER.info("Loaded LDAP configuration instance: [ {} ]", instance);
 
      LOGGER.debug("Reloading configuration based on the provied id: {}", configurationId);
      configEntity = ambariConfigurationDAOProvider.get().findByPK(configurationId);
    return instance;
  }
 
    }
  private Map<String, Object> toProperties(List<AmbariConfigurationEntity> configEntities) {
    Map<String, Object> map = new HashMap<>();
 
    if (configEntity != null) {
      Set propertyMaps = gson.fromJson(configEntity.getConfigurationBaseEntity().getConfigurationData(), Set.class);
      instance = ldapConfigurationFactory.createLdapConfiguration((Map<String, Object>) propertyMaps.iterator().next());
    for (AmbariConfigurationEntity entity : configEntities) {
      map.put(entity.getPropertyName(), entity.getPropertyValue());
     }
 
    LOGGER.info("Loaded LDAP configuration instance: [ {} ]", instance);

    return instance;
    return map;
   }
 
   // On changing the configuration, the provider gets updated with the fresh value
   @Subscribe
  public void ambariLdapConfigChanged(AmbariLdapConfigChangedEvent event) {
  public void ambariLdapConfigChanged(AmbariConfigurationChangedEvent event) {
     LOGGER.info("LDAP config changed event received: {}", event);
    loadInstance(event.getConfigurationId());
    loadInstance();
     LOGGER.info("Refreshed LDAP config instance.");
   }


 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java
index 8467af08b6..5e4e0ca5b6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/ldap/service/ads/LdapConnectionTemplateFactory.java
@@ -18,7 +18,7 @@ import javax.inject.Inject;
 import javax.inject.Provider;
 import javax.inject.Singleton;
 
import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
 import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
 import org.apache.ambari.server.ldap.service.AmbariLdapException;
 import org.apache.ambari.server.ldap.service.LdapConnectionConfigService;
@@ -103,7 +103,7 @@ public class LdapConnectionTemplateFactory {
    * @throws AmbariLdapException
    */
   @Subscribe
  public void onConfigChange(AmbariLdapConfigChangedEvent event) throws AmbariLdapException {
  public void onConfigChange(AmbariConfigurationChangedEvent event) throws AmbariLdapException {
     ldapConnectionTemplateInstance = create(ambariLdapConfigurationProvider.get());
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java
index 83293efb82..e4446d79b8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAO.java
@@ -14,13 +14,20 @@
 
 package org.apache.ambari.server.orm.dao;
 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 import javax.inject.Inject;
 import javax.inject.Singleton;
 import javax.persistence.EntityExistsException;
 import javax.persistence.EntityNotFoundException;
 import javax.persistence.TypedQuery;
 
import org.apache.ambari.server.orm.RequiresSession;
 import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntityPK;
import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -32,7 +39,7 @@ import com.google.inject.persist.Transactional;
  */
 
 @Singleton
public class AmbariConfigurationDAO extends CrudDAO<AmbariConfigurationEntity, Long> {
public class AmbariConfigurationDAO extends CrudDAO<AmbariConfigurationEntity, AmbariConfigurationEntityPK> {
 
   private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationDAO.class);
 
@@ -41,49 +48,132 @@ public class AmbariConfigurationDAO extends CrudDAO<AmbariConfigurationEntity, L
     super(AmbariConfigurationEntity.class);
   }
 
  /**
   * Returns the Ambari configuration properties with the requested category name from the database.
   *
   * @param categoryName the configuration category name
   * @return the configuration entity
   */
  @RequiresSession
  public List<AmbariConfigurationEntity> findByCategory(String categoryName) {
    TypedQuery<AmbariConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
        "AmbariConfigurationEntity.findByCategory", AmbariConfigurationEntity.class);
    query.setParameter("categoryName", categoryName);
    return daoUtils.selectList(query);
  }

  /**
   * Removes the Ambari configuration properties with the requested category name from the database.
   *
   * @param categoryName the configuration category name
   * @return the number of items removed
   */
  @Transactional
  public int removeByCategory(String categoryName) {
    TypedQuery<AmbariConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
        "AmbariConfigurationEntity.deleteByCategory", AmbariConfigurationEntity.class);
    query.setParameter("categoryName", categoryName);
    return query.executeUpdate();
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
    // make sure only one entry exists per configuration type...
    AmbariConfigurationEntity foundEntity = findByPK(new AmbariConfigurationEntityPK(entity.getCategoryName(), entity.getPropertyName()));
    if (foundEntity != null) {
      String message = String.format("Only one configuration entry can exist for the category %s and name %s", entity.getCategoryName(), entity.getPropertyName());
      LOGGER.error(message);
      throw new EntityExistsException(message);
     }

     super.create(entity);
   }
 
  @Override
  public AmbariConfigurationEntity merge(AmbariConfigurationEntity entity) {
    AmbariConfigurationEntity foundEntity = findByPK(new AmbariConfigurationEntityPK(entity.getCategoryName(), entity.getPropertyName()));
    if (foundEntity == null) {
      String message = String.format("The configuration entry for the category %s and name %s does not exist", entity.getCategoryName(), entity.getPropertyName());
      LOGGER.debug(message);
      throw new EntityNotFoundException(message);
    }
 
  @Transactional
  public void update(AmbariConfigurationEntity entity) {
    if (entity.getId() == null || findByPK(entity.getId()) == null) {
      String msg = String.format("The entity with id [ %s ] is not found", entity.getId());
      LOGGER.debug(msg);
      throw new EntityNotFoundException(msg);
    AmbariConfigurationEntity updatedEntity = entity;

    if (!StringUtils.equals(foundEntity.getPropertyValue(), entity.getPropertyValue())) {
      // updating the existing entity
      updatedEntity = super.merge(entity);
      entityManagerProvider.get().flush();
     }
 
    // updating the existing entity
    super.merge(entity);
    entityManagerProvider.get().flush();
    return updatedEntity;
   }
 
   /**
   * Returns the LDAP configuration from the database.
   * Reconciles the properties associted with an Ambari confgiration category (for example, ldap-configuration)
   * using persisted properties and the supplied properties.
   * <p>
   * if <code>removeIfNotProvided</code> is <code>true</code>, only properties that exist in the new set of
   * properties will be persisted; others will be removed.
   * <p>
   * If <code>removeIfNotProvided</code> is <code>false</code>, then the new properties will be used
   * to update or append to the set of persisted properties.
    *
   * @return the configuration entity
   * @param categoryName        the category name for the set of properties
   * @param properties          a map of name to value pairs
   * @param removeIfNotProvided <code>true</code> to explicitly set the set of properties for the category; <code>false</code> to upadate the set of properties for the category
   * @return <code>true</code> if changes were made; <code>false</code> if not changes were made.
    */
   @Transactional
  public AmbariConfigurationEntity getLdapConfiguration() {
    LOGGER.info("Looking up the LDAP configuration ....");
    AmbariConfigurationEntity ldapConfigEntity = null;
  public boolean reconcileCategory(String categoryName, Map<String, String> properties, boolean removeIfNotProvided) {
    boolean changesDetected = false;
    List<AmbariConfigurationEntity> existingEntities = findByCategory(categoryName);
    Map<String, String> propertiesToProcess = new HashMap<>();
 
    TypedQuery<AmbariConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
      "AmbariConfigurationEntity.findByType", AmbariConfigurationEntity.class);
    query.setParameter("typeName", "ldap-configuration");
    if (properties != null) {
      propertiesToProcess.putAll(properties);
    }

    if (existingEntities != null) {
      for (AmbariConfigurationEntity entity : existingEntities) {
        String propertyName = entity.getPropertyName();

        if (propertiesToProcess.containsKey(propertyName)) {
          String newPropertyValue = propertiesToProcess.get(propertyName);
          if (!StringUtils.equals(newPropertyValue, entity.getPropertyValue())) {
            // Update the entry...
            entity.setPropertyValue(newPropertyValue);
            merge(entity);
            changesDetected = true;
          }
        } else if (removeIfNotProvided) {
          // Remove the entry since it is not in the new set of properties...
          remove(entity);
          changesDetected = true;
        }

        // If already processed, remove it so we know no to add it later...
        propertiesToProcess.remove(propertyName);
      }
    }

    // Add the new entries...
    if (!propertiesToProcess.isEmpty()) {
      for (Map.Entry<String, String> property : propertiesToProcess.entrySet()) {
        AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
        entity.setCategoryName(categoryName);
        entity.setPropertyName(property.getKey());
        entity.setPropertyValue(property.getValue());
        create(entity);
      }

      changesDetected = true;
    }

    if (changesDetected) {
      entityManagerProvider.get().flush();
    }
 
    ldapConfigEntity = daoUtils.selectSingle(query);
    LOGGER.info("Returned entity: {} ", ldapConfigEntity);
    return ldapConfigEntity;
    return changesDetected;
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java
index c9f4695469..8cd6751927 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntity.java
@@ -14,57 +14,99 @@
 
 package org.apache.ambari.server.orm.entities;
 
import javax.persistence.CascadeType;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.IdClass;
 import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
 import javax.persistence.Table;
 
@Entity
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

 @Table(name = "ambari_configuration")
 @NamedQueries({
  @NamedQuery(
    name = "AmbariConfigurationEntity.findByType",
    query = "select ace from AmbariConfigurationEntity ace where ace.configurationBaseEntity.type = :typeName")
    @NamedQuery(
        name = "AmbariConfigurationEntity.findByCategory",
        query = "select ace from AmbariConfigurationEntity ace where ace.categoryName = :categoryName"),
    @NamedQuery(
        name = "AmbariConfigurationEntity.deleteByCategory",
        query = "delete from AmbariConfigurationEntity ace where ace.categoryName = :categoryName")
 })

@IdClass(AmbariConfigurationEntityPK.class)
@Entity
 public class AmbariConfigurationEntity {
 
   @Id
  @Column(name = "id")
  private Long id;
  @Column(name = "category_name")
  private String categoryName;

  @Id
  @Column(name = "property_name")
  private String propertyName;

  @Column(name = "property_value")
  private String propertyValue;
 
  @OneToOne(cascade = CascadeType.ALL)
  @MapsId
  @JoinColumn(name = "id")
  private ConfigurationBaseEntity configurationBaseEntity;
  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String category) {
    this.categoryName = category;
  }
 
  public Long getId() {
    return id;
  public String getPropertyName() {
    return propertyName;
   }
 
  public void setId(Long id) {
    this.id = id;
  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
   }
 
  public ConfigurationBaseEntity getConfigurationBaseEntity() {
    return configurationBaseEntity;
  public String getPropertyValue() {
    return propertyValue;
   }
 
  public void setConfigurationBaseEntity(ConfigurationBaseEntity configurationBaseEntity) {
    this.configurationBaseEntity = configurationBaseEntity;
  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
   }
 
   @Override
   public String toString() {
     return "AmbariConfigurationEntity{" +
      "id=" + id +
      ", configurationBaseEntity=" + configurationBaseEntity +
      '}';
        ", category=" + categoryName +
        ", name=" + propertyName +
        ", value=" + propertyValue +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AmbariConfigurationEntity that = (AmbariConfigurationEntity) o;

    return new EqualsBuilder()
        .append(categoryName, that.categoryName)
        .append(propertyName, that.propertyName)
        .append(propertyValue, that.propertyValue)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(categoryName)
        .append(propertyName)
        .append(propertyValue)
        .toHashCode();
   }
 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntityPK.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntityPK.java
new file mode 100644
index 0000000000..3674e12781
-- /dev/null
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/AmbariConfigurationEntityPK.java
@@ -0,0 +1,88 @@
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

package org.apache.ambari.server.orm.entities;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Composite primary key for {@link AmbariConfigurationEntity}.
 */
public class AmbariConfigurationEntityPK implements Serializable {

  private String categoryName;
  private String propertyName;

  /**
   * Constructor.
   *
   * @param categoryName configuration category name
   * @param propertyName configuration property name
   */
  public AmbariConfigurationEntityPK(String categoryName, String propertyName) {
    this.categoryName = categoryName;
    this.propertyName = propertyName;
  }

  /**
   * Get the configuration category name.
   *
   * @return category name
   */
  public String getCategoryName() {
    return categoryName;
  }

  /**
   * Get the property name.
   *
   * @return property name
   */
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AmbariConfigurationEntityPK that = (AmbariConfigurationEntityPK) o;

    return new EqualsBuilder()
        .append(categoryName, that.categoryName)
        .append(propertyName, that.propertyName)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(categoryName)
        .append(propertyName)
        .toHashCode();
  }
}
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java
deleted file mode 100644
index 9ad30d7d1a..0000000000
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ConfigurationBaseEntity.java
++ /dev/null
@@ -1,159 +0,0 @@
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
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/SchemaUpgradeHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/SchemaUpgradeHelper.java
index 8812ef5bfc..9c77129c46 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/SchemaUpgradeHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/SchemaUpgradeHelper.java
@@ -34,6 +34,7 @@ import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.audit.AuditLoggerModule;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
 import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.utils.EventBusSynchronizer;
 import org.apache.ambari.server.utils.VersionUtils;
@@ -373,7 +374,7 @@ public class SchemaUpgradeHelper {
         System.exit(1);
       }
 
      Injector injector = Guice.createInjector(new UpgradeHelperModule(), new AuditLoggerModule());
      Injector injector = Guice.createInjector(new UpgradeHelperModule(), new AuditLoggerModule(), new LdapModule());
       SchemaUpgradeHelper schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);
 
       //Fail if MySQL database has tables with MyISAM engine
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog300.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog300.java
index d3e924e66c..2de60957bc 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog300.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog300.java
@@ -66,21 +66,21 @@ public class UpgradeCatalog300 extends AbstractUpgradeCatalog {
   protected static final String STAGE_DISPLAY_STATUS_COLUMN = "display_status";
   protected static final String REQUEST_TABLE = "request";
   protected static final String REQUEST_DISPLAY_STATUS_COLUMN = "display_status";
  protected static final String CLUSTER_CONFIG_TABLE = "clusterconfig";
  protected static final String CLUSTER_CONFIG_SELECTED_COLUMN = "selected";
  protected static final String CLUSTER_CONFIG_SELECTED_TIMESTAMP_COLUMN = "selected_timestamp";
   protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
   protected static final String HRC_OPS_DISPLAY_NAME_COLUMN = "ops_display_name";
  protected static final String COMPONENT_TABLE = "servicecomponentdesiredstate";
   protected static final String COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
   protected static final String COMPONENT_STATE_TABLE = "hostcomponentstate";
   protected static final String SERVICE_DESIRED_STATE_TABLE = "servicedesiredstate";
   protected static final String SECURITY_STATE_COLUMN = "security_state";
 
  protected static final String AMBARI_CONFIGURATION_TABLE = "ambari_configuration";
  protected static final String AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN = "category_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN = "property_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN = "property_value";

   @Inject
   DaoUtils daoUtils;
 

   // ----- Constructors ------------------------------------------------------
 
   /**
@@ -123,6 +123,7 @@ public class UpgradeCatalog300 extends AbstractUpgradeCatalog {
     updateStageTable();
     addOpsDisplayNameColumnToHostRoleCommand();
     removeSecurityState();
    addAmbariConfigurationTable();
   }
 
   protected void updateStageTable() throws SQLException {
@@ -134,6 +135,16 @@ public class UpgradeCatalog300 extends AbstractUpgradeCatalog {
         new DBAccessor.DBColumnInfo(REQUEST_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
   }
 
  protected void addAmbariConfigurationTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, String.class, 255, null, true));

    dbAccessor.createTable(AMBARI_CONFIGURATION_TABLE, columns);
    dbAccessor.addPKConstraint(AMBARI_CONFIGURATION_TABLE, "PK_ambari_configuration", AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN);
  }

   /**
    * {@inheritDoc}
    */
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
index 7d634941d9..7045240b30 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
@@ -84,22 +84,11 @@ CREATE TABLE clusterconfig (
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
  category_name VARCHAR(100) NOT NULL,
  property_name VARCHAR(100) NOT NULL,
  property_value VARCHAR(255) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (category_name, property_name));
 
 CREATE TABLE serviceconfig (
   service_config_id BIGINT NOT NULL,
@@ -1175,8 +1164,6 @@ INSERT INTO ambari_sequences (sequence_name, sequence_value)
   union all
   select 'servicecomponent_version_id_seq', 0 FROM SYSIBM.SYSDUMMY1
   union all
  select 'configuration_id_seq', 0 FROM SYSIBM.SYSDUMMY1
  union all
   select 'hostcomponentdesiredstate_id_seq', 0 FROM SYSIBM.SYSDUMMY1;
 
 
diff --git a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
index af17353ff3..c950c7ef83 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
@@ -104,22 +104,11 @@ CREATE TABLE clusterconfig (
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
  category_name VARCHAR(100) NOT NULL,
  property_name VARCHAR(100) NOT NULL,
  property_value VARCHAR(255) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (category_name, property_name));
 
 CREATE TABLE serviceconfig (
   service_config_id BIGINT NOT NULL,
@@ -1137,7 +1126,6 @@ INSERT INTO ambari_sequences(sequence_name, sequence_value) VALUES
   ('remote_cluster_id_seq', 0),
   ('remote_cluster_service_id_seq', 0),
   ('servicecomponent_version_id_seq', 0),
  ('configuration_id_seq', 0),
   ('hostcomponentdesiredstate_id_seq', 0);
 
 INSERT INTO adminresourcetype (resource_type_id, resource_type_name) VALUES
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
index 89c7971303..537ae196c5 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
@@ -84,22 +84,11 @@ CREATE TABLE clusterconfig (
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
  category_name VARCHAR2(100) NOT NULL,
  property_name VARCHAR2(100) NOT NULL,
  property_value VARCHAR2(255) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (category_name, property_name));
 
 CREATE TABLE serviceconfig (
   service_config_id NUMBER(19) NOT NULL,
@@ -1116,7 +1105,6 @@ INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('ambari_oper
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_service_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('servicecomponent_version_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('configuration_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('hostcomponentdesiredstate_id_seq', 0);
 
 INSERT INTO metainfo("metainfo_key", "metainfo_value") values ('version', '${ambariSchemaVersion}');
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
index 3d2bd3a107..b4952c2e86 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
@@ -66,21 +66,11 @@ CREATE TABLE clusters (
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
  category_name VARCHAR(100) NOT NULL,
  property_name VARCHAR(100) NOT NULL,
  property_value VARCHAR(255) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (category_name, property_name)
 );
 
 CREATE TABLE clusterconfig (
@@ -1116,7 +1106,6 @@ INSERT INTO ambari_sequences (sequence_name, sequence_value) VALUES
   ('remote_cluster_id_seq', 0),
   ('remote_cluster_service_id_seq', 0),
   ('servicecomponent_version_id_seq', 0),
  ('configuration_id_seq', 0),
   ('hostcomponentdesiredstate_id_seq', 0);
 
 INSERT INTO adminresourcetype (resource_type_id, resource_type_name) VALUES
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
index 55a6c61017..4fb0d0981a 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
@@ -83,22 +83,11 @@ CREATE TABLE clusterconfig (
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
  category_name VARCHAR(100) NOT NULL,
  property_name VARCHAR(100) NOT NULL,
  property_value VARCHAR(255) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (category_name, property_name));
 
 CREATE TABLE serviceconfig (
   service_config_id NUMERIC(19) NOT NULL,
@@ -1115,7 +1104,6 @@ INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_clus
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('remote_cluster_service_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('servicecomponent_version_id_seq', 0);
 INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('hostcomponentdesiredstate_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, sequence_value) values ('configuration_id_seq', 0);
 
 insert into adminresourcetype (resource_type_id, resource_type_name)
   select 1, 'AMBARI'
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
index e5e8af59c3..8a88aba905 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
@@ -97,21 +97,11 @@ CREATE TABLE clusterconfig (
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
  category_name VARCHAR(100) NOT NULL,
  property_name VARCHAR(100) NOT NULL,
  property_value VARCHAR(255) NOT NULL,
  CONSTRAINT PK_ambari_configuration PRIMARY KEY (category_name, property_name)
 );
 
 CREATE TABLE serviceconfig (
@@ -1140,7 +1130,6 @@ BEGIN TRANSACTION
     ('remote_cluster_id_seq', 0),
     ('remote_cluster_service_id_seq', 0),
     ('servicecomponent_version_id_seq', 0),
    ('configuration_id_seq', 0),
     ('hostcomponentdesiredstate_id_seq', 0);
 
   insert into adminresourcetype (resource_type_id, resource_type_name)
diff --git a/ambari-server/src/main/resources/META-INF/persistence.xml b/ambari-server/src/main/resources/META-INF/persistence.xml
index 67eef705af..686c8312cd 100644
-- a/ambari-server/src/main/resources/META-INF/persistence.xml
++ b/ambari-server/src/main/resources/META-INF/persistence.xml
@@ -97,7 +97,6 @@
     <class>org.apache.ambari.server.orm.entities.KerberosDescriptorEntity</class>
     <class>org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity</class>
     <class>org.apache.ambari.server.orm.entities.RemoteAmbariClusterServiceEntity</class>
    <class>org.apache.ambari.server.orm.entities.ConfigurationBaseEntity</class>
     <class>org.apache.ambari.server.orm.entities.AmbariConfigurationEntity</class>
 
     <properties>
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
index c2a14218e7..a2ecb271ed 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/AmbariConfigurationResourceProviderTest.java
@@ -14,238 +14,364 @@
 
 package org.apache.ambari.server.controller.internal;
 
import static org.apache.ambari.server.controller.internal.AmbariConfigurationResourceProvider.AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.AmbariConfigurationResourceProvider.AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
 import java.util.Map;
 import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;
 
 import org.apache.ambari.server.controller.spi.Predicate;
 import org.apache.ambari.server.controller.spi.Request;
 import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
 import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.events.AmbariLdapConfigChangedEvent;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
 import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.orm.entities.ConfigurationBaseEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
 import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
 import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.After;
 import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
 
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
 
 public class AmbariConfigurationResourceProviderTest extends EasyMockSupport {
 
  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);
  private static final String CATEGORY_NAME_1 = "test-category-1";
  private static final String CATEGORY_NAME_2 = "test-category-2";
 
  @Mock
  private Request requestMock;
  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }
 
  @Mock
  private AmbariConfigurationDAO ambariConfigurationDAO;
  @Test
  public void testCreateResources_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }
 
  @Mock
  private AmbariEventPublisher publisher;
  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }
 
  private Capture<AmbariConfigurationEntity> ambariConfigurationEntityCapture;
  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator());
  }
 
  private Gson gson;
  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }
 
  private static final String DATA_MOCK_STR = "[\n" +
    "      {\n" +
    "        \"authentication.ldap.baseDn\" : \"dc=ambari,dc=apache,dc=org\",\n" +
    "        \"authentication.ldap.primaryUrl\" : \"localhost:33389\",\n" +
    "        \"authentication.ldap.secondaryUrl\" : \"localhost:333\"\n" +
    "      }\n" +
    "    ]";
  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceOperator());
  }
 
  private static final Long PK_LONG = Long.valueOf(1);
  private static final String PK_STRING = String.valueOf(1);
  private static final String VERSION_TAG = "test version";
  private static final String VERSION = "1";
  private static final String TYPE = "AmbariConfiguration";
  private void testCreateResources(Authentication authentication) throws Exception {
    Injector injector = createInjector();
 
  @TestSubject
  private AmbariConfigurationResourceProvider ambariConfigurationResourceProvider = new AmbariConfigurationResourceProvider();
    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);
 
  @Before
  public void setup() {
    ambariConfigurationEntityCapture = Capture.newInstance();
    gson = new GsonBuilder().create();
  }
    Set<Map<String, Object>> propertySets = new HashSet<>();
 
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
    Map<String, String> properties1 = new HashMap<>();
    properties1.put("property1a", "value1");
    properties1.put("property2a", "value2");
    propertySets.add(toRequestProperties(CATEGORY_NAME_1, properties1));

    Map<String, String> properties2 = new HashMap<>();
    properties2.put("property1b", "value1");
    properties2.put("property2b", "value2");
    propertySets.add(toRequestProperties(CATEGORY_NAME_2, properties2));

    Request request = createMock(Request.class);
    expect(request.getProperties()).andReturn(propertySets).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();
    Capture<Map<String, String>> capturedProperties2 = newCapture();

    AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
    expect(dao.reconcileCategory(eq(CATEGORY_NAME_1), capture(capturedProperties1), eq(true)))
        .andReturn(true)
        .once();
    expect(dao.reconcileCategory(eq(CATEGORY_NAME_2), capture(capturedProperties2), eq(true)))
        .andReturn(true)
        .once();

    AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);
    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().times(2);
 
     replayAll();
 
    // WHEN
    ambariConfigurationResourceProvider.createResourcesAuthorized(requestMock);
    SecurityContextHolder.getContext().setAuthentication(authentication);
 
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
    resourceProvider.createResources(request);

    verifyAll();

    validateCapturedProperties(properties1, capturedProperties1);
    validateCapturedProperties(properties2, capturedProperties2);
   }
 
   @Test
  public void testRemoveAmbariConfigurationRequestResultsInTheProperPersistenceCall() throws Exception {
    // GIVEN
    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();
  public void testDeleteResources_Administrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }
 
    Capture<Long> pkCapture = Capture.newInstance();
    ambariConfigurationDAO.removeByPK(EasyMock.capture(pkCapture));
    publisher.publish(EasyMock.anyObject(AmbariLdapConfigChangedEvent.class));
  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }
 
    replayAll();
  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterOperator());
  }
 
    // WHEN
    ambariConfigurationResourceProvider.deleteResourcesAuthorized(requestMock, predicate);
  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }
 
    // THEN
    Assert.assertEquals("The pk of the entity to be removed doen't match the expected id", Long.valueOf(1), pkCapture.getValue());
  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceOperator());
   }
 
  private void testDeleteResources(Authentication authentication) throws Exception {
    Injector injector = createInjector();
 
  @Test
  public void testRetrieveAmbariConfigurationShouldResultsInTheProperDAOCall() throws Exception {
    // GIVEN
    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();
    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);

    Predicate predicate = new PredicateBuilder()
        .property(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(CATEGORY_NAME_1)
        .toPredicate();

    Request request = createMock(Request.class);

    AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
    expect(dao.removeByCategory(CATEGORY_NAME_1)).andReturn(1).once();

    AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);
    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().once();
 
    EasyMock.expect(ambariConfigurationDAO.findAll()).andReturn(Lists.newArrayList(createDummyAmbariConfigurationEntity()));
     replayAll();
 
    // WHEN
    Set<Resource> resourceSet = ambariConfigurationResourceProvider.getResourcesAuthorized(requestMock, predicate);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    resourceProvider.deleteResources(request, predicate);
 
    // THEN
    Assert.assertNotNull(resourceSet);
    Assert.assertFalse(resourceSet.isEmpty());
    verifyAll();
   }
 
   @Test
  public void testUpdateAmbariConfigurationShouldResultInTheProperDAOCalls() throws Exception {
    // GIVEN
  public void testGetResources_Administrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testGetResources(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);
 
    Predicate predicate = new PredicateBuilder().property(
      AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId()).equals("1").toPredicate();
    Predicate predicate = new PredicateBuilder()
        .property(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(CATEGORY_NAME_1)
        .toPredicate();
 
    // properteies in the request, representing the updated configuration
    Set<Map<String, Object>> resourcePropertiesSet = Sets.newHashSet(new PropertiesMapBuilder()
      .withId(PK_LONG)
      .withVersion("2")
      .withVersionTag("version-2")
      .withData(DATA_MOCK_STR)
      .withType(TYPE)
      .build());
    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).anyTimes();
 
    EasyMock.expect(requestMock.getProperties()).andReturn(resourcePropertiesSet);
    Map<String, String> properties = new HashMap<>();
    properties.put("property1a", "value1");
    properties.put("property2a", "value2");
 
    AmbariConfigurationEntity persistedEntity = createDummyAmbariConfigurationEntity();
    EasyMock.expect(ambariConfigurationDAO.findByPK(PK_LONG)).andReturn(persistedEntity);
    ambariConfigurationDAO.update(EasyMock.capture(ambariConfigurationEntityCapture));
    publisher.publish(EasyMock.anyObject(AmbariLdapConfigChangedEvent.class));
    AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
    expect(dao.findByCategory(CATEGORY_NAME_1)).andReturn(createEntities(CATEGORY_NAME_1, properties)).once();
 
     replayAll();
 
    // WHEN
    ambariConfigurationResourceProvider.updateResourcesAuthorized(requestMock, predicate);
    SecurityContextHolder.getContext().setAuthentication(authentication);
 
    // the captured entity should be the updated one
    AmbariConfigurationEntity updatedEntity = ambariConfigurationEntityCapture.getValue();
    Set<Resource> response = resourceProvider.getResources(request, predicate);
 
    // THEN
    Assert.assertNotNull(updatedEntity);
    Assert.assertEquals("The updated version is wrong", Integer.valueOf(2), updatedEntity.getConfigurationBaseEntity().getVersion());
  }
    verifyAll();
 
  private class PropertiesMapBuilder {
    junit.framework.Assert.assertNotNull(response);
    junit.framework.Assert.assertEquals(1, response.size());
 
    private Map<String, Object> resourcePropertiesMap = Maps.newHashMap();
    Resource resource = response.iterator().next();
    junit.framework.Assert.assertEquals(Resource.Type.AmbariConfiguration, resource.getType());
 
    private PropertiesMapBuilder() {
    }
    Map<String, Map<String, Object>> propertiesMap = resource.getPropertiesMap();
    junit.framework.Assert.assertEquals(2, propertiesMap.size());
 
    public PropertiesMapBuilder withId(Long id) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.ID.getPropertyId(), id);
      return this;
    }
    junit.framework.Assert.assertEquals(CATEGORY_NAME_1, propertiesMap.get(Resource.Type.AmbariConfiguration.name()).get("category"));
 
    private PropertiesMapBuilder withVersion(String version) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.VERSION.getPropertyId(), version);
      return this;
    }
    Map<String, Object> retrievedProperties = propertiesMap.get(Resource.Type.AmbariConfiguration.name() + "/properties");
    junit.framework.Assert.assertEquals(2, retrievedProperties.size());
 
    private PropertiesMapBuilder withVersionTag(String versionTag) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.VERSION_TAG.getPropertyId(), versionTag);
      return this;
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      junit.framework.Assert.assertEquals(entry.getValue(), retrievedProperties.get(entry.getKey()));
     }
  }
 
    private PropertiesMapBuilder withData(String dataJson) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.DATA.getPropertyId(), dataJson);
      return this;
    }
  @Test
  public void testUpdateResources_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }
 
    private PropertiesMapBuilder withType(String type) {
      resourcePropertiesMap.put(AmbariConfigurationResourceProvider.ResourcePropertyId.TYPE.getPropertyId(), type);
      return this;
    }
  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }
 
  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterOperator());
  }
 
    public Map<String, Object> build() {
      return this.resourcePropertiesMap;
    }
  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    ResourceProvider resourceProvider = injector.getInstance(AmbariConfigurationResourceProvider.class);

    Predicate predicate = new PredicateBuilder()
        .property(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(CATEGORY_NAME_1)
        .toPredicate();

    Set<Map<String, Object>> propertySets = new HashSet<>();
 
    Map<String, String> properties1 = new HashMap<>();
    properties1.put("property1a", "value1");
    properties1.put("property2a", "value2");
    propertySets.add(toRequestProperties(CATEGORY_NAME_1, properties1));

    Request request = createMock(Request.class);
    expect(request.getProperties()).andReturn(propertySets).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();

    AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
    expect(dao.reconcileCategory(eq(CATEGORY_NAME_1), capture(capturedProperties1), eq(false)))
        .andReturn(true)
        .once();

    AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);
    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().times(1);

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    resourceProvider.updateResources(request, predicate);

    verifyAll();

    validateCapturedProperties(properties1, capturedProperties1);
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
  private List<AmbariConfigurationEntity> createEntities(String categoryName, Map<String, String> properties) {
    List<AmbariConfigurationEntity> entities = new ArrayList<>();
 
    return acEntity;
    for (Map.Entry<String, String> property : properties.entrySet()) {
      AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
      entity.setCategoryName(categoryName);
      entity.setPropertyName(property.getKey());
      entity.setPropertyValue(property.getValue());
      entities.add(entity);
    }

    return entities;
  }

  private Map<String, Object> toRequestProperties(String categoryName1, Map<String, String> properties) {
    Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(AMBARI_CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName1);
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      requestProperties.put(AMBARI_CONFIGURATION_PROPERTIES_PROPERTY_ID + "/" + entry.getKey(), entry.getValue());
    }
    return requestProperties;
   }
 
  private void validateCapturedProperties(Map<String, String> expectedProperties, Capture<Map<String, String>> capturedProperties) {
    junit.framework.Assert.assertTrue(capturedProperties.hasCaptured());
 
    Map<String, String> properties = capturedProperties.getValue();
    junit.framework.Assert.assertNotNull(properties);

    // Convert the Map to a TreeMap to help with comparisons
    expectedProperties = new TreeMap<>(expectedProperties);
    properties = new TreeMap<>(properties);
    junit.framework.Assert.assertEquals(expectedProperties, properties);
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariConfigurationDAO.class).toInstance(createMock(AmbariConfigurationDAO.class));
        bind(AmbariEventPublisher.class).toInstance(createMock(AmbariEventPublisher.class));
      }
    });
  }
 }
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAOTest.java b/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAOTest.java
new file mode 100644
index 0000000000..f801fd61ee
-- /dev/null
++ b/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/AmbariConfigurationDAOTest.java
@@ -0,0 +1,298 @@
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

package org.apache.ambari.server.orm.dao;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Provider;

import junit.framework.Assert;

public class AmbariConfigurationDAOTest extends EasyMockSupport {

  private static final String CATEGORY_NAME = "test-category";
  private static Method methodMerge;
  private static Method methodRemove;
  private static Method methodCreate;
  private static Method methodFindByCategory;

  private static Field fieldEntityManagerProvider;

  @BeforeClass
  public static void beforeKDCKerberosOperationHandlerTest() throws Exception {
    methodMerge = AmbariConfigurationDAO.class.getMethod("merge", AmbariConfigurationEntity.class);
    methodRemove = CrudDAO.class.getMethod("remove", Object.class);
    methodCreate = AmbariConfigurationDAO.class.getMethod("create", AmbariConfigurationEntity.class);
    methodFindByCategory = AmbariConfigurationDAO.class.getMethod("findByCategory", String.class);

    fieldEntityManagerProvider = CrudDAO.class.getDeclaredField("entityManagerProvider");
  }

  @Test
  public void testReconcileCategoryNewCategory() throws Exception {
    Capture<AmbariConfigurationEntity> capturedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(null).once();

    dao.create(capture(capturedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> properties;
    properties = new HashMap<>();
    properties.put("property1", "value1");
    properties.put("property2", "value2");
    dao.reconcileCategory(CATEGORY_NAME, properties, true);

    verifyAll();

    validateCapturedEntities(CATEGORY_NAME, properties, capturedEntities);
  }

  @Test
  public void testReconcileCategoryReplaceCategory() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedRemovedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    dao.remove(capture(capturedRemovedEntities));
    expectLastCall().anyTimes();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property1_new", "value1");
    newProperties.put("property2_new", "value2");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, true);

    verifyAll();

    validateCapturedEntities(CATEGORY_NAME, newProperties, capturedCreatedEntities);
    validateCapturedEntities(CATEGORY_NAME, existingProperties, capturedRemovedEntities);
  }

  @Test
  public void testReconcileCategoryUpdateCategoryKeepNotSpecified() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedMergedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    expect(dao.merge(capture(capturedMergedEntities))).andReturn(createNiceMock(AmbariConfigurationEntity.class)).anyTimes();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property1", "new_value1");
    newProperties.put("property2_new", "value2");
    newProperties.put("property3", "value3");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, false);

    verifyAll();

    Map<String, String> expectedProperties;

    expectedProperties = new HashMap<>();
    expectedProperties.put("property2_new", "value2");
    expectedProperties.put("property3", "value3");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedCreatedEntities);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property1", "new_value1");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedMergedEntities);
  }

  @Test
  public void testReconcileCategoryUpdateCategoryRemoveNotSpecified() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedRemovedEntities = newCapture(CaptureType.ALL);
    Capture<AmbariConfigurationEntity> capturedMergedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    expect(dao.merge(capture(capturedMergedEntities))).andReturn(createNiceMock(AmbariConfigurationEntity.class)).anyTimes();

    dao.remove(capture(capturedRemovedEntities));
    expectLastCall().anyTimes();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property1", "new_value1");
    newProperties.put("property2_new", "value2");
    newProperties.put("property3", "value3");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, true);

    verifyAll();

    Map<String, String> expectedProperties;

    expectedProperties = new HashMap<>();
    expectedProperties.put("property2_new", "value2");
    expectedProperties.put("property3", "value3");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedCreatedEntities);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property2", "value2");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedRemovedEntities);

    expectedProperties = new HashMap<>();
    expectedProperties.put("property1", "new_value1");
    validateCapturedEntities(CATEGORY_NAME, expectedProperties, capturedMergedEntities);
  }

  @Test
  public void testReconcileCategoryAppendCategory() throws Exception {

    Map<String, String> existingProperties;
    existingProperties = new HashMap<>();
    existingProperties.put("property1", "value1");
    existingProperties.put("property2", "value2");

    Capture<AmbariConfigurationEntity> capturedCreatedEntities = newCapture(CaptureType.ALL);

    AmbariConfigurationDAO dao = createDao();

    expect(dao.findByCategory(CATEGORY_NAME)).andReturn(toEntities(CATEGORY_NAME, existingProperties)).once();

    dao.create(capture(capturedCreatedEntities));
    expectLastCall().anyTimes();

    replayAll();

    Map<String, String> newProperties;
    newProperties = new HashMap<>();
    newProperties.put("property3", "value3");
    newProperties.put("property4", "value3");
    dao.reconcileCategory(CATEGORY_NAME, newProperties, false);

    verifyAll();

    validateCapturedEntities(CATEGORY_NAME, newProperties, capturedCreatedEntities);
  }

  private AmbariConfigurationDAO createDao() throws IllegalAccessException {
    AmbariConfigurationDAO dao = createMockBuilder(AmbariConfigurationDAO.class)
        .addMockedMethods(methodMerge, methodRemove, methodCreate, methodFindByCategory)
        .createMock();

    EntityManager entityManager = createMock(EntityManager.class);
    entityManager.flush();
    expectLastCall().anyTimes();

    Provider<EntityManager> entityManagerProvider = createMock(Provider.class);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();

    fieldEntityManagerProvider.set(dao, entityManagerProvider);

    return dao;
  }

  private List<AmbariConfigurationEntity> toEntities(String categoryName, Map<String, String> properties) {
    List<AmbariConfigurationEntity> entities = new ArrayList<>();

    for (Map.Entry<String, String> property : properties.entrySet()) {
      AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
      entity.setCategoryName(categoryName);
      entity.setPropertyName(property.getKey());
      entity.setPropertyValue(property.getValue());
      entities.add(entity);
    }

    return entities;
  }

  private void validateCapturedEntities(String expectedCategoryName, Map<String, String> expectedProperties, Capture<AmbariConfigurationEntity> capturedEntities) {
    Assert.assertTrue(capturedEntities.hasCaptured());

    List<AmbariConfigurationEntity> entities = capturedEntities.getValues();
    Assert.assertNotNull(entities);

    Map<String, String> capturedProperties = new TreeMap<>();
    for (AmbariConfigurationEntity entity : entities) {
      Assert.assertEquals(expectedCategoryName, entity.getCategoryName());
      capturedProperties.put(entity.getPropertyName(), entity.getPropertyValue());
    }

    // Convert the Map to a TreeMap to help with comparisons
    expectedProperties = new TreeMap<>(expectedProperties);
    Assert.assertEquals(expectedProperties, capturedProperties);
  }

}
\ No newline at end of file
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog300Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog300Test.java
index bd8f5cbd57..747f99b618 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog300Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog300Test.java
@@ -17,6 +17,10 @@
  */
 package org.apache.ambari.server.upgrade;
 
import static org.apache.ambari.server.upgrade.UpgradeCatalog300.AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog300.AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog300.AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog300.AMBARI_CONFIGURATION_TABLE;
 import static org.apache.ambari.server.upgrade.UpgradeCatalog300.COMPONENT_DESIRED_STATE_TABLE;
 import static org.apache.ambari.server.upgrade.UpgradeCatalog300.COMPONENT_STATE_TABLE;
 import static org.apache.ambari.server.upgrade.UpgradeCatalog300.SECURITY_STATE_COLUMN;
@@ -42,6 +46,7 @@ import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
import java.util.List;
 import java.util.Map;
 
 import javax.persistence.EntityManager;
@@ -186,6 +191,15 @@ public class UpgradeCatalog300Test {
     dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
     expectLastCall().once();
 
    // Ambari configuration table addition...
    Capture<List<DBAccessor.DBColumnInfo>> ambariConfigurationTableColumns = newCapture();

    dbAccessor.createTable(eq(AMBARI_CONFIGURATION_TABLE), capture(ambariConfigurationTableColumns));
    expectLastCall().once();
    dbAccessor.addPKConstraint(AMBARI_CONFIGURATION_TABLE, "PK_ambari_configuration", AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN);
    expectLastCall().once();
    // Ambari configuration table addition...

     replay(dbAccessor, configuration);
 
     Injector injector = Guice.createInjector(module);
@@ -197,6 +211,35 @@ public class UpgradeCatalog300Test {
     Assert.assertEquals(null, capturedOpsDisplayNameColumn.getDefaultValue());
     Assert.assertEquals(String.class, capturedOpsDisplayNameColumn.getType());
 
    // Ambari configuration table addition...
    Assert.assertTrue(ambariConfigurationTableColumns.hasCaptured());
    List<DBAccessor.DBColumnInfo> columns = ambariConfigurationTableColumns.getValue();
    Assert.assertEquals(3, columns.size());

    for (DBAccessor.DBColumnInfo column : columns) {
      String columnName = column.getName();

      if (AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(100), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertFalse(column.isNullable());
      } else if (AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(100), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertFalse(column.isNullable());
      } else if (AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(255), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertTrue(column.isNullable());
      } else {
        Assert.fail("Unexpected column name: " + columnName);
      }
    }
    // Ambari configuration table addition...

     verify(dbAccessor);
   }
 
- 
2.19.1.windows.1

