From 20056273ec9279f4823e495cdecfe514537f1223 Mon Sep 17 00:00:00 2001
From: Sumit Mohanty <smohanty@hortonworks.com>
Date: Fri, 19 Feb 2016 13:33:00 -0800
Subject: [PATCH] AMBARI-15011. Decrease the load on ambari database after
 cluster creation (Sebastian Toader via smohanty)

--
 .../server/configuration/Configuration.java   |  77 ++++++++
 .../server/controller/ControllerModule.java   |  16 ++
 .../server/orm/dao/HostRoleCommandDAO.java    | 166 ++++++++++++++----
 .../orm/entities/HostRoleCommandEntity.java   |   7 +-
 .../serveraction/ServerActionExecutor.java    |  13 +-
 .../server/state/cluster/ClusterImpl.java     |  36 ++--
 .../server/upgrade/UpgradeCatalog222.java     |   5 +
 .../resources/Ambari-DDL-MySQL-CREATE.sql     |   1 +
 .../resources/Ambari-DDL-Oracle-CREATE.sql    |   1 +
 .../resources/Ambari-DDL-Postgres-CREATE.sql  |   3 +
 .../Ambari-DDL-Postgres-EMBEDDED-CREATE.sql   |   1 +
 .../Ambari-DDL-SQLAnywhere-CREATE.sql         |   1 +
 .../resources/Ambari-DDL-SQLServer-CREATE.sql |   1 +
 .../actionmanager/TestActionScheduler.java    |  24 +--
 .../server/agent/AgentResourceTest.java       |   2 +
 .../configuration/ConfigurationTest.java      |  95 ++++++++++
 .../server/controller/KerberosHelperTest.java |   2 +
 .../ambari/server/state/ConfigHelperTest.java |   2 +
 .../server/upgrade/UpgradeCatalog222Test.java |  55 ++++--
 .../ambari/server/utils/StageUtilsTest.java   |   2 +
 20 files changed, 435 insertions(+), 75 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java b/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
index aec83a7ab1..4a980eeae3 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java
@@ -184,6 +184,9 @@ public class Configuration {
   public static final String LDAP_REFERRAL_KEY = "authentication.ldap.referral";
   public static final String LDAP_PAGINATION_ENABLED_KEY = "authentication.ldap.pagination.enabled";
   public static final String SERVER_EC_CACHE_SIZE = "server.ecCacheSize";
  public static final String SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED = "server.hrcStatusSummary.cache.enabled";
  public static final String SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE = "server.hrcStatusSummary.cache.size";
  public static final String SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION = "server.hrcStatusSummary.cache.expiryDuration";
   public static final String SERVER_STALE_CONFIG_CACHE_ENABLED_KEY = "server.cache.isStale.enabled";
   public static final String SERVER_PERSISTENCE_TYPE_KEY = "server.persistence.type";
   public static final String SERVER_JDBC_USER_NAME_KEY = "server.jdbc.user.name";
@@ -367,6 +370,11 @@ public class Configuration {
 
   public static final String CUSTOM_ACTION_DEFINITION_KEY = "custom.action.definitions";
   public static final String SHARED_RESOURCES_DIR_KEY = "shared.resources.dir";

  protected static final boolean SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED_DEFAULT = true;
  protected static final long SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE_DEFAULT = 10000L;
  protected static final long SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_DEFAULT = 30; //minutes

   private static final String CUSTOM_ACTION_DEFINITION_DEF_VALUE = "/var/lib/ambari-server/resources/custom_action_definitions";
 
   private static final long SERVER_EC_CACHE_SIZE_DEFAULT = 10000L;
@@ -1756,6 +1764,75 @@ public class Configuration {
     return value;
   }
 
  /**
   * Caching of host role command status summary can be enabled/disabled
   * through the {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED} config property.
   * This method returns the value of {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED}
   * config property. If this config property is not defined than returns the default defined by {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED_DEFAULT}.
   * @return true if caching is to be enabled otherwise false.
   */
  public boolean getHostRoleCommandStatusSummaryCacheEnabled() {
    String stringValue = properties.getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED);
    boolean value = SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED_DEFAULT;
    if (stringValue != null) {
      try {
        value = Boolean.valueOf(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * In order to avoid the cache storing host role command status summary objects exhaust
   * memory we set a max record number allowed for the cache. This limit can be configured
   * through {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE} config property. The method returns
   * the value of this config property. If this config property is not defined than
   * the default value specified by {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE_DEFAULT} is returned.
   * @return the upper limit for the number of cached host role command summaries.
   */
  public long getHostRoleCommandStatusSummaryCacheSize() {
    String stringValue = properties.getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE_DEFAULT;
    if (stringValue != null) {
      try {
        value = Long.valueOf(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }

  /**
   * As a safety measure the cache storing host role command status summaries should auto expire after a while.
   * The expiry duration is specified through the {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION} config property
   * expressed in minutes. The method returns the value of this config property. If this config property is not defined than
   * the default value specified by {@link #SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_DEFAULT}
   * @return the cache expiry duration in minutes
   */
  public long getHostRoleCommandStatusSummaryCacheExpiryDuration() {
    String stringValue = properties.getProperty(SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION);
    long value = SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_DEFAULT;
    if (stringValue != null) {
      try {
        value = Long.valueOf(stringValue);
      }
      catch (NumberFormatException ignored) {
      }

    }

    return value;
  }



   /**
    * @return whether staleConfig's flag is cached.
    */
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
index 76ff6dbe41..daca64dc80 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/ControllerModule.java
@@ -65,6 +65,7 @@ import org.apache.ambari.server.notifications.NotificationDispatcher;
 import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.DBAccessorImpl;
 import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
 import org.apache.ambari.server.scheduler.ExecutionScheduler;
 import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
 import org.apache.ambari.server.security.AmbariEntryPoint;
@@ -338,6 +339,21 @@ public class ControllerModule extends AbstractModule {
     bindConstant().annotatedWith(Names.named("executionCommandCacheSize")).
         to(configuration.getExecutionCommandsCacheSize());
 

    // Host role commands status summary max cache enable/disable
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_ENABLED)).
      to(configuration.getHostRoleCommandStatusSummaryCacheEnabled());

    // Host role commands status summary max cache size
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_SIZE)).
      to(configuration.getHostRoleCommandStatusSummaryCacheSize());
    // Host role command status summary cache expiry duration in minutes
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES)).
      to(configuration.getHostRoleCommandStatusSummaryCacheExpiryDuration());




     bind(AmbariManagementController.class).to(
       AmbariManagementControllerImpl.class);
     bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
index 4fd03e5d4b..deca9b1735 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
@@ -18,9 +18,6 @@
 
 package org.apache.ambari.server.orm.dao;
 
import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;
import static org.apache.ambari.server.orm.dao.DaoUtils.ORACLE_LIST_LIMIT;

 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Collection;
@@ -28,6 +25,7 @@ import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
import java.util.concurrent.TimeUnit;
 
 import javax.persistence.EntityManager;
 import javax.persistence.TypedQuery;
@@ -49,16 +47,27 @@ import org.apache.ambari.server.orm.entities.HostEntity;
 import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
 import org.apache.ambari.server.orm.entities.HostRoleCommandEntity_;
 import org.apache.ambari.server.orm.entities.StageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
 import com.google.common.collect.Lists;
 import com.google.inject.Inject;
 import com.google.inject.Provider;
 import com.google.inject.Singleton;
import com.google.inject.name.Named;
 import com.google.inject.persist.Transactional;
 
import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;
import static org.apache.ambari.server.orm.dao.DaoUtils.ORACLE_LIST_LIMIT;

 @Singleton
 public class HostRoleCommandDAO {
 
  private static final Logger LOG = LoggerFactory.getLogger(HostRoleCommandDAO.class);

   private static final String SUMMARY_DTO = String.format(
     "SELECT NEW %s(" +
       "MAX(hrc.stage.skippable), " +
@@ -92,12 +101,122 @@ public class HostRoleCommandDAO {
    */
   private static final String COMPLETED_REQUESTS_SQL = "SELECT DISTINCT task.requestId FROM HostRoleCommandEntity task WHERE task.requestId NOT IN (SELECT task.requestId FROM HostRoleCommandEntity task WHERE task.status IN :notCompletedStatuses) ORDER BY task.requestId {0}";
 
  /**
   * A cache that holds {@link HostRoleCommandStatusSummaryDTO} grouped by stage id for requests by request id.
   * The JPQL computing the host role command status summary for a request is rather expensive
   * thus this cache helps reducing the load on the database
   */
  private final LoadingCache<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> hrcStatusSummaryCache;

  /**
   * Specifies whether caching for {@link HostRoleCommandStatusSummaryDTO} grouped by stage id for requests
   * is enabled.
   */
  private final boolean hostRoleCommandStatusSummaryCacheEnabled;


   @Inject
   Provider<EntityManager> entityManagerProvider;
 
   @Inject
   DaoUtils daoUtils;
 
  public final static String HRC_STATUS_SUMMARY_CACHE_SIZE =  "hostRoleCommandStatusSummaryCacheSize";
  public final static String HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES = "hostRoleCommandStatusCacheExpiryDurationMins";
  public final static String HRC_STATUS_SUMMARY_CACHE_ENABLED =  "hostRoleCommandStatusSummaryCacheEnabled";

  /**
   * Invalidates the host role command status summary cache entry that corresponds to the given request.
   * @param requestId the key of the cache entry to be invalidated.
   */
  protected void invalidateHostRoleCommandStatusSummaryCache(Long requestId) {
    if (!hostRoleCommandStatusSummaryCacheEnabled )
      return;

    LOG.debug("Invalidating host role command status summary cache for request {} !", requestId);
    hrcStatusSummaryCache.invalidate(requestId);

  }

  /**
   * Invalidates those entries in host role command status cache which are dependent on the passed {@link org.apache.ambari.server.orm.entities.HostRoleCommandEntity}
   * entity.
   * @param hostRoleCommandEntity
   */
  protected void invalidateHostRoleCommandStatusCache(HostRoleCommandEntity hostRoleCommandEntity) {
    if ( !hostRoleCommandStatusSummaryCacheEnabled )
      return;

    if (hostRoleCommandEntity != null) {
      Long requestId = hostRoleCommandEntity.getRequestId();
      if (requestId == null) {
        StageEntity stageEntity = hostRoleCommandEntity.getStage();
        if (stageEntity != null)
          requestId = stageEntity.getRequestId();
      }

      if (requestId != null)
        invalidateHostRoleCommandStatusSummaryCache(requestId.longValue());
    }

  }

  /**
   * Loads the counts of tasks for a request and groups them by stage id.
   * This allows for very efficient loading when there are a huge number of stages
   * and tasks to iterate (for example, during a Stack Upgrade).
   * @param requestId the request id
   * @return the map of stage-to-summary objects
   */
  @RequiresSession
  protected Map<Long, HostRoleCommandStatusSummaryDTO> loadAggregateCounts(Long requestId) {

    TypedQuery<HostRoleCommandStatusSummaryDTO> query = entityManagerProvider.get().createQuery(
      SUMMARY_DTO, HostRoleCommandStatusSummaryDTO.class);

    query.setParameter("requestId", requestId);
    query.setParameter("aborted", HostRoleStatus.ABORTED);
    query.setParameter("completed", HostRoleStatus.COMPLETED);
    query.setParameter("failed", HostRoleStatus.FAILED);
    query.setParameter("holding", HostRoleStatus.HOLDING);
    query.setParameter("holding_failed", HostRoleStatus.HOLDING_FAILED);
    query.setParameter("holding_timedout", HostRoleStatus.HOLDING_TIMEDOUT);
    query.setParameter("in_progress", HostRoleStatus.IN_PROGRESS);
    query.setParameter("pending", HostRoleStatus.PENDING);
    query.setParameter("queued", HostRoleStatus.QUEUED);
    query.setParameter("timedout", HostRoleStatus.TIMEDOUT);
    query.setParameter("skipped_failed", HostRoleStatus.SKIPPED_FAILED);

    Map<Long, HostRoleCommandStatusSummaryDTO> map = new HashMap<Long, HostRoleCommandStatusSummaryDTO>();

    for (HostRoleCommandStatusSummaryDTO dto : daoUtils.selectList(query)) {
      map.put(dto.getStageId(), dto);
    }

    return map;
  }

  @Inject
  public HostRoleCommandDAO(@Named(HRC_STATUS_SUMMARY_CACHE_ENABLED) boolean hostRoleCommandStatusSummaryCacheEnabled, @Named(HRC_STATUS_SUMMARY_CACHE_SIZE) long hostRoleCommandStatusSummaryCacheLimit, @Named(HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES) long hostRoleCommandStatusSummaryCacheExpiryDurationMins) {
    this.hostRoleCommandStatusSummaryCacheEnabled = hostRoleCommandStatusSummaryCacheEnabled;

    LOG.info("Host role command status summary cache {} !", hostRoleCommandStatusSummaryCacheEnabled ? "enabled" : "disabled");


    hrcStatusSummaryCache = CacheBuilder.newBuilder()
      .maximumSize(hostRoleCommandStatusSummaryCacheLimit)
      .expireAfterAccess(hostRoleCommandStatusSummaryCacheExpiryDurationMins, TimeUnit.MINUTES)
      .build(new CacheLoader<Long, Map<Long, HostRoleCommandStatusSummaryDTO>>() {
        @Override
        public Map<Long, HostRoleCommandStatusSummaryDTO> load(Long requestId) throws Exception {
          LOG.debug("Cache miss for host role command status summary object for request {}, fetching from JPA", requestId);
          Map<Long, HostRoleCommandStatusSummaryDTO> hrcCommandStatusByStageId = loadAggregateCounts(requestId);

          return hrcCommandStatusByStageId;
        }
      });
  }

   @RequiresSession
   public HostRoleCommandEntity findByPK(long taskId) {
     return entityManagerProvider.get().find(HostRoleCommandEntity.class, taskId);
@@ -425,11 +544,16 @@ public class HostRoleCommandDAO {
   @Transactional
   public void create(HostRoleCommandEntity stageEntity) {
     entityManagerProvider.get().persist(stageEntity);

    invalidateHostRoleCommandStatusCache(stageEntity);
   }
 
   @Transactional
   public HostRoleCommandEntity merge(HostRoleCommandEntity stageEntity) {
     HostRoleCommandEntity entity = entityManagerProvider.get().merge(stageEntity);

    invalidateHostRoleCommandStatusCache(entity);

     return entity;
   }
 
@@ -446,6 +570,8 @@ public class HostRoleCommandDAO {
     List<HostRoleCommandEntity> managedList = new ArrayList<HostRoleCommandEntity>(entities.size());
     for (HostRoleCommandEntity entity : entities) {
       managedList.add(entityManagerProvider.get().merge(entity));

      invalidateHostRoleCommandStatusCache(entity);
     }
     return managedList;
   }
@@ -453,6 +579,8 @@ public class HostRoleCommandDAO {
   @Transactional
   public void remove(HostRoleCommandEntity stageEntity) {
     entityManagerProvider.get().remove(merge(stageEntity));

    invalidateHostRoleCommandStatusCache(stageEntity);
   }
 
   @Transactional
@@ -463,39 +591,17 @@ public class HostRoleCommandDAO {
 
   /**
    * Finds the counts of tasks for a request and groups them by stage id.
   * This allows for very efficient loading when there are a huge number of stages
   * and tasks to iterate (for example, during a Stack Upgrade).
    * @param requestId the request id
    * @return the map of stage-to-summary objects
    */
  @RequiresSession
   public Map<Long, HostRoleCommandStatusSummaryDTO> findAggregateCounts(Long requestId) {

    TypedQuery<HostRoleCommandStatusSummaryDTO> query = entityManagerProvider.get().createQuery(
        SUMMARY_DTO, HostRoleCommandStatusSummaryDTO.class);

    query.setParameter("requestId", requestId);
    query.setParameter("aborted", HostRoleStatus.ABORTED);
    query.setParameter("completed", HostRoleStatus.COMPLETED);
    query.setParameter("failed", HostRoleStatus.FAILED);
    query.setParameter("holding", HostRoleStatus.HOLDING);
    query.setParameter("holding_failed", HostRoleStatus.HOLDING_FAILED);
    query.setParameter("holding_timedout", HostRoleStatus.HOLDING_TIMEDOUT);
    query.setParameter("in_progress", HostRoleStatus.IN_PROGRESS);
    query.setParameter("pending", HostRoleStatus.PENDING);
    query.setParameter("queued", HostRoleStatus.QUEUED);
    query.setParameter("timedout", HostRoleStatus.TIMEDOUT);
    query.setParameter("skipped_failed", HostRoleStatus.SKIPPED_FAILED);

    Map<Long, HostRoleCommandStatusSummaryDTO> map = new HashMap<Long, HostRoleCommandStatusSummaryDTO>();

    for (HostRoleCommandStatusSummaryDTO dto : daoUtils.selectList(query)) {
      map.put(dto.getStageId(), dto);
    }

    return map;
    if (hostRoleCommandStatusSummaryCacheEnabled)
      return hrcStatusSummaryCache.getUnchecked(requestId);
    else
      return loadAggregateCounts(requestId); // if caching not enabled fall back to fetching through JPA
   }
 

   /**
    * Updates the {@link HostRoleCommandEntity#isFailureAutoSkipped()} flag for
    * all commands for the given request.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java
index af71c403f7..1674175aaa 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java
@@ -32,6 +32,7 @@ import javax.persistence.FetchType;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.Id;
import javax.persistence.Index;
 import javax.persistence.JoinColumn;
 import javax.persistence.JoinColumns;
 import javax.persistence.Lob;
@@ -48,7 +49,11 @@ import org.apache.ambari.server.actionmanager.HostRoleStatus;
 import org.apache.commons.lang.ArrayUtils;
 
 @Entity
@Table(name = "host_role_command")
@Table(name = "host_role_command"
       , indexes = {
           @Index(name = "idx_hrc_request_id", columnList = "request_id")
         , @Index(name = "idx_hrc_status_role", columnList = "status, role")
       })
 @TableGenerator(name = "host_role_command_id_generator",
     table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
     , pkColumnValue = "host_role_command_id_seq"
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java
index 20cf5bb200..f93cf4371c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java
@@ -392,17 +392,8 @@ public class ServerActionExecutor {
    * @throws InterruptedException
    */
   public void doWork() throws InterruptedException {
    List<HostRoleCommand> tasks = db.getTasksByHostRoleAndStatus(serverHostName,
        Role.AMBARI_SERVER_ACTION.toString(), HostRoleStatus.QUEUED);

    if (null == tasks || tasks.isEmpty()) {
      // !!! if the server is not a part of the cluster,
      // !!! just look for anything designated AMBARI_SERVER_ACTION.
      // !!! do we even need to worry about servername in the first place?  We're
      // !!! _on_ the ambari server!
      tasks = db.getTasksByRoleAndStatus(Role.AMBARI_SERVER_ACTION.name(),
          HostRoleStatus.QUEUED);
    }
    List<HostRoleCommand> tasks = db.getTasksByRoleAndStatus(Role.AMBARI_SERVER_ACTION.name(),
      HostRoleStatus.QUEUED);
 
     if ((tasks != null) && !tasks.isEmpty()) {
       for (HostRoleCommand task : tasks) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
index 57941d01e9..42129758f0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/cluster/ClusterImpl.java
@@ -2462,13 +2462,31 @@ public class ClusterImpl implements Cluster {
     clusterGlobalLock.readLock().lock();
     try {
       List<ServiceConfigVersionResponse> serviceConfigVersionResponses = new ArrayList<ServiceConfigVersionResponse>();
      Set<Long> activeIds = getActiveServiceConfigVersionIds();
 
      for (ServiceConfigEntity serviceConfigEntity : serviceConfigDAO.getServiceConfigs(getClusterId())) {
      List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getServiceConfigs(getClusterId());
      Map<String, ServiceConfigVersionResponse> activeServiceConfigResponses = new HashMap<>();

      for (ServiceConfigEntity serviceConfigEntity : serviceConfigs) {
         ServiceConfigVersionResponse serviceConfigVersionResponse = convertToServiceConfigVersionResponse(serviceConfigEntity);
 
        ServiceConfigVersionResponse activeServiceConfigResponse = activeServiceConfigResponses.get(serviceConfigVersionResponse.getServiceName());
        if (activeServiceConfigResponse == null) {
          activeServiceConfigResponse = serviceConfigVersionResponse;
          activeServiceConfigResponses.put(serviceConfigVersionResponse.getServiceName(), serviceConfigVersionResponse);
        }

         serviceConfigVersionResponse.setConfigurations(new ArrayList<ConfigurationResponse>());
        serviceConfigVersionResponse.setIsCurrent(activeIds.contains(serviceConfigEntity.getServiceConfigId()));

        if (serviceConfigEntity.getGroupId() == null) {
          if (serviceConfigVersionResponse.getCreateTime() > activeServiceConfigResponse.getCreateTime())
            activeServiceConfigResponses.put(serviceConfigVersionResponse.getServiceName(), serviceConfigVersionResponse);
        }
        else if (clusterConfigGroups != null && clusterConfigGroups.containsKey(serviceConfigEntity.getGroupId())){
          if (serviceConfigVersionResponse.getVersion() > activeServiceConfigResponse.getVersion())
            activeServiceConfigResponses.put(serviceConfigVersionResponse.getServiceName(), serviceConfigVersionResponse);
        }

        serviceConfigVersionResponse.setIsCurrent(false);
 
         List<ClusterConfigEntity> clusterConfigEntities = serviceConfigEntity.getClusterConfigEntities();
         for (ClusterConfigEntity clusterConfigEntity : clusterConfigEntities) {
@@ -2484,6 +2502,10 @@ public class ClusterImpl implements Cluster {
         serviceConfigVersionResponses.add(serviceConfigVersionResponse);
       }
 
      for (ServiceConfigVersionResponse serviceConfigVersionResponse: activeServiceConfigResponses.values()) {
        serviceConfigVersionResponse.setIsCurrent(true);
      }

       return serviceConfigVersionResponses;
     } finally {
       clusterGlobalLock.readLock().unlock();
@@ -2502,14 +2524,6 @@ public class ClusterImpl implements Cluster {
     return responses;
   }
 
  private Set<Long> getActiveServiceConfigVersionIds() {
    Set<Long> idSet = new HashSet<Long>();
    for (ServiceConfigEntity entity : getActiveServiceConfigVersionEntities()) {
      idSet.add(entity.getServiceConfigId());
    }
    return idSet;
  }

   private List<ServiceConfigEntity> getActiveServiceConfigVersionEntities() {
 
     List<ServiceConfigEntity> activeServiceConfigVersions = new ArrayList<ServiceConfigEntity>();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog222.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog222.java
index 88b3151fbc..0aa1e7a850 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog222.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog222.java
@@ -115,6 +115,7 @@ public class UpgradeCatalog222 extends AbstractUpgradeCatalog {
     updateAlerts();
     updateStormConfigs();
     updateAMSConfigs();
    updateHostRoleCommands();
   }
 
   protected void updateStormConfigs() throws  AmbariException {
@@ -153,6 +154,10 @@ public class UpgradeCatalog222 extends AbstractUpgradeCatalog {
 
   }
 
  protected void updateHostRoleCommands() throws SQLException{
    dbAccessor.createIndex("idx_hrc_status", "host_role_command", "status", "role");
  }

   protected void updateAMSConfigs() throws AmbariException {
     AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
     Clusters clusters = ambariManagementController.getClusters();
diff --git a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
index e39590248e..1ea1646df6 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
@@ -677,6 +677,7 @@ CREATE TABLE setting (
 -- tasks indices --
 CREATE INDEX idx_stage_request_id ON stage (request_id);
 CREATE INDEX idx_hrc_request_id ON host_role_command (request_id);
CREATE INDEX idx_hrc_status_role ON host_role_command (status, role);
 CREATE INDEX idx_rsc_request_id ON role_success_criteria (request_id);
 
 -- altering tables by creating unique constraints----------
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
index 0f957e6646..e5fa3e19f2 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
@@ -666,6 +666,7 @@ CREATE TABLE setting (
 -- tasks indices --
 CREATE INDEX idx_stage_request_id ON stage (request_id);
 CREATE INDEX idx_hrc_request_id ON host_role_command (request_id);
CREATE INDEX idx_hrc_status_role ON host_role_command (status, role);
 CREATE INDEX idx_rsc_request_id ON role_success_criteria (request_id);
 
 --------altering tables by creating unique constraints----------
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
index 7daf4aee6f..150ea9b401 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
@@ -670,8 +670,11 @@ CREATE TABLE setting (
 -- tasks indices --
 CREATE INDEX idx_stage_request_id ON stage (request_id);
 CREATE INDEX idx_hrc_request_id ON host_role_command (request_id);
CREATE INDEX idx_hrc_status_role ON host_role_command (status, role);
 CREATE INDEX idx_rsc_request_id ON role_success_criteria (request_id);
 


 --------altering tables by creating unique constraints----------
 ALTER TABLE users ADD CONSTRAINT UNQ_users_0 UNIQUE (user_name, user_type);
 ALTER TABLE clusterconfig ADD CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag);
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql
index 28c025f9c9..044333641f 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql
@@ -752,6 +752,7 @@ GRANT ALL PRIVILEGES ON TABLE ambari.setting TO :username;
 -- tasks indices --
 CREATE INDEX idx_stage_request_id ON ambari.stage (request_id);
 CREATE INDEX idx_hrc_request_id ON ambari.host_role_command (request_id);
CREATE INDEX idx_hrc_status_role ON ambari.host_role_command (status, role);
 CREATE INDEX idx_rsc_request_id ON ambari.role_success_criteria (request_id);
 
 --------altering tables by creating unique constraints----------
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
index c9d6ac22f0..1f45bc72fc 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
@@ -666,6 +666,7 @@ CREATE TABLE setting (
 -- tasks indices --
 CREATE INDEX idx_stage_request_id ON stage (request_id);
 CREATE INDEX idx_hrc_request_id ON host_role_command (request_id);
CREATE INDEX idx_hrc_status_role ON host_role_command (status, role);
 CREATE INDEX idx_rsc_request_id ON role_success_criteria (request_id);
 
 -- altering tables by creating unique constraints----------
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
index 4d6f0e8dd7..b1edbbd127 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
@@ -775,6 +775,7 @@ CREATE TABLE setting (
 -- tasks indices --
 CREATE INDEX idx_stage_request_id ON stage (request_id);
 CREATE INDEX idx_hrc_request_id ON host_role_command (request_id);
CREATE INDEX idx_hrc_status_role ON host_role_command (status, role);
 CREATE INDEX idx_rsc_request_id ON role_success_criteria (request_id);
 
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
index bc4d397b4c..af6fb9b7ff 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
@@ -607,8 +607,8 @@ public class TestActionScheduler {
     doAnswer(new Answer<List<HostRoleCommand>>() {
       @Override
       public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[1];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[2];
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];
 
         HostRoleCommand task = s.getHostRoleCommand(null, role);
 
@@ -618,7 +618,7 @@ public class TestActionScheduler {
           return Collections.emptyList();
         }
       }
    }).when(db).getTasksByHostRoleAndStatus(anyString(), anyString(), any(HostRoleStatus.class));
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));
 
     ServerActionExecutor.init(injector);
     ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
@@ -762,8 +762,8 @@ public class TestActionScheduler {
     doAnswer(new Answer<List<HostRoleCommand>>() {
       @Override
       public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[1];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[2];
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];
 
         HostRoleCommand task = s.getHostRoleCommand(null, role);
 
@@ -774,7 +774,7 @@ public class TestActionScheduler {
         }
 
       }
    }).when(db).getTasksByHostRoleAndStatus(anyString(), anyString(), any(HostRoleStatus.class));
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));
 
     ServerActionExecutor.init(injector);
     ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
@@ -843,8 +843,8 @@ public class TestActionScheduler {
     doAnswer(new Answer<List<HostRoleCommand>>() {
       @Override
       public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[1];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[2];
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];
 
         HostRoleCommand task = s.getHostRoleCommand(null, role);
 
@@ -854,7 +854,7 @@ public class TestActionScheduler {
           return Collections.emptyList();
         }
       }
    }).when(db).getTasksByHostRoleAndStatus(anyString(), anyString(), any(HostRoleStatus.class));
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));
 
     ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
         new HostsMap((String) null), unitOfWork, null, conf);
@@ -1951,8 +1951,8 @@ public class TestActionScheduler {
     doAnswer(new Answer<List<HostRoleCommand>>() {
       @Override
       public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[1];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[2];
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];
 
         HostRoleCommand task = s.getHostRoleCommand(null, role);
 
@@ -1962,7 +1962,7 @@ public class TestActionScheduler {
           return Collections.emptyList();
         }
       }
    }).when(db).getTasksByHostRoleAndStatus(anyString(), anyString(), any(HostRoleStatus.class));
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));
 
     doAnswer(new Answer<HostRoleCommand>() {
       @Override
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java
index 510e1fba72..6cb9e6f353 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/agent/AgentResourceTest.java
@@ -36,6 +36,7 @@ import org.apache.ambari.server.agent.rest.AgentResource;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
 import org.apache.ambari.server.security.SecurityHelper;
 import org.apache.ambari.server.security.SecurityHelperImpl;
 import org.apache.ambari.server.stack.StackManagerFactory;
@@ -308,6 +309,7 @@ public class AgentResourceTest extends RandomPortJerseyTest {
       bind(HeartBeatHandler.class).toInstance(handler);
       bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
       bind(DBAccessor.class).toInstance(mock(DBAccessor.class));
      bind(HostRoleCommandDAO.class).toInstance(mock(HostRoleCommandDAO.class));
     }
 
     private void installDependencies() {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/configuration/ConfigurationTest.java b/ambari-server/src/test/java/org/apache/ambari/server/configuration/ConfigurationTest.java
index 4e236f37b0..3ecb5aa703 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/configuration/ConfigurationTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/configuration/ConfigurationTest.java
@@ -563,4 +563,99 @@ public class ConfigurationTest {
     Assert.assertEquals(44, configuration.getPropertyProvidersThreadPoolMaxSize());
   }
 

  public void testGetHostRoleCommandStatusSummaryCacheSize() throws  Exception {
    // Given
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);
    ambariProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE, "3000");

    // When
    long actualCacheSize = configuration.getHostRoleCommandStatusSummaryCacheSize();

    // Then
    Assert.assertEquals(actualCacheSize, 3000L);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheSizeDefault() throws  Exception {
    // Given
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);

    // When
    long actualCacheSize = configuration.getHostRoleCommandStatusSummaryCacheSize();

    // Then
    Assert.assertEquals(actualCacheSize, Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_SIZE_DEFAULT);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheExpiryDuration() throws  Exception {
    // Given
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);
    ambariProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION, "60");

    // When
    long actualCacheExpiryDuration = configuration.getHostRoleCommandStatusSummaryCacheExpiryDuration();

    // Then
    Assert.assertEquals(actualCacheExpiryDuration, 60L);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheExpiryDurationDefault() throws  Exception {
    // Given
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);

    // When
    long actualCacheExpiryDuration = configuration.getHostRoleCommandStatusSummaryCacheExpiryDuration();

    // Then
    Assert.assertEquals(actualCacheExpiryDuration, Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_DEFAULT);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheEnabled() throws  Exception {
    // Given
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);
    ambariProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED, "true");

    // When
    boolean actualCacheEnabledConfig = configuration.getHostRoleCommandStatusSummaryCacheEnabled();

    // Then
    Assert.assertEquals(actualCacheEnabledConfig, true);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheDisabled() throws  Exception {
    // Given
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);
    ambariProperties.setProperty(Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED, "false");

    // When
    boolean actualCacheEnabledConfig = configuration.getHostRoleCommandStatusSummaryCacheEnabled();

    // Then
    Assert.assertEquals(actualCacheEnabledConfig, false);
  }

  @Test
  public void testGetHostRoleCommandStatusSummaryCacheEnabledDefault() throws  Exception {
    // Given
    final Properties ambariProperties = new Properties();
    final Configuration configuration = new Configuration(ambariProperties);

    // When
    boolean actualCacheEnabledConfig = configuration.getHostRoleCommandStatusSummaryCacheEnabled();

    // Then
    Assert.assertEquals(actualCacheEnabledConfig, Configuration.SERVER_HRC_STATUS_SUMMARY_CACHE_ENABLED_DEFAULT);
  }

 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
index 2dcde00adf..f6027f361b 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/KerberosHelperTest.java
@@ -42,6 +42,7 @@ import org.apache.ambari.server.controller.spi.Resource;
 import org.apache.ambari.server.controller.spi.ResourceProvider;
 import org.apache.ambari.server.metadata.RoleCommandOrder;
 import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
 import org.apache.ambari.server.security.SecurityHelper;
 import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
 import org.apache.ambari.server.security.encryption.CredentialStoreService;
@@ -215,6 +216,7 @@ public class KerberosHelperTest extends EasyMockSupport {
         bind(CreatePrincipalsServerAction.class).toInstance(createMock(CreatePrincipalsServerAction.class));
         bind(CreateKeytabFilesServerAction.class).toInstance(createMock(CreateKeytabFilesServerAction.class));
         bind(StackAdvisorHelper.class).toInstance(createMock(StackAdvisorHelper.class));
        bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
       }
     });
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/state/ConfigHelperTest.java b/ambari-server/src/test/java/org/apache/ambari/server/state/ConfigHelperTest.java
index 98424b796a..9fe0fc3a19 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/state/ConfigHelperTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/state/ConfigHelperTest.java
@@ -48,6 +48,7 @@ import org.apache.ambari.server.controller.spi.ClusterController;
 import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
 import org.apache.ambari.server.security.SecurityHelper;
 import org.apache.ambari.server.security.TestAuthenticationFactory;
 import org.apache.ambari.server.stack.StackManagerFactory;
@@ -754,6 +755,7 @@ public class ConfigHelperTest {
           bind(Clusters.class).toInstance(createNiceMock(ClustersImpl.class));
           bind(ClusterController.class).toInstance(clusterController);
           bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
          bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
         }
       });
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog222Test.java b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog222Test.java
index 6061e067bc..077df33484 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog222Test.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/upgrade/UpgradeCatalog222Test.java
@@ -19,12 +19,12 @@
 package org.apache.ambari.server.upgrade;
 
 
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

 import org.apache.ambari.server.actionmanager.ActionManager;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.controller.AmbariManagementController;
@@ -33,6 +33,7 @@ import org.apache.ambari.server.controller.ConfigurationRequest;
 import org.apache.ambari.server.controller.ConfigurationResponse;
 import org.apache.ambari.server.controller.KerberosHelper;
 import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
 import org.apache.ambari.server.orm.dao.StackDAO;
@@ -40,6 +41,7 @@ import org.apache.ambari.server.orm.entities.StackEntity;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.stack.OsFamily;
 import org.easymock.Capture;
 import org.easymock.EasyMock;
 import org.easymock.EasyMockSupport;
@@ -47,10 +49,14 @@ import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
import javax.persistence.EntityManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
 
 import static org.easymock.EasyMock.anyObject;
 import static org.easymock.EasyMock.anyString;
@@ -58,6 +64,7 @@ import static org.easymock.EasyMock.capture;
 import static org.easymock.EasyMock.createMockBuilder;
 import static org.easymock.EasyMock.createNiceMock;
 import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
 import static org.easymock.EasyMock.expect;
 import static org.easymock.EasyMock.expectLastCall;
 import static org.easymock.EasyMock.replay;
@@ -101,6 +108,7 @@ public class UpgradeCatalog222Test {
     Method updateAlerts = UpgradeCatalog222.class.getDeclaredMethod("updateAlerts");
     Method updateStormConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateStormConfigs");
     Method updateAMSConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateAMSConfigs");
    Method updateHostRoleCommands = UpgradeCatalog222.class.getDeclaredMethod("updateHostRoleCommands");
 
 
     UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
@@ -108,6 +116,7 @@ public class UpgradeCatalog222Test {
             .addMockedMethod(updateAlerts)
             .addMockedMethod(updateStormConfigs)
             .addMockedMethod(updateAMSConfigs)
            .addMockedMethod(updateHostRoleCommands)
             .createMock();
 
     upgradeCatalog222.addNewConfigurationsFromXml();
@@ -118,6 +127,8 @@ public class UpgradeCatalog222Test {
     expectLastCall().once();
     upgradeCatalog222.updateAMSConfigs();
     expectLastCall().once();
    upgradeCatalog222.updateHostRoleCommands();
    expectLastCall().once();
 
     replay(upgradeCatalog222);
 
@@ -203,4 +214,28 @@ public class UpgradeCatalog222Test {
 
   }
 
  @Test
  public void testUpdateHostRoleCommands() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    dbAccessor.createIndex(eq("idx_hrc_status"), eq("host_role_command"), eq("status"), eq("role"));
    expectLastCall().once();

    replay(dbAccessor);

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog222 upgradeCatalog222 = injector.getInstance(UpgradeCatalog222.class);
    upgradeCatalog222.updateHostRoleCommands();


    verify(dbAccessor);
  }

 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/utils/StageUtilsTest.java b/ambari-server/src/test/java/org/apache/ambari/server/utils/StageUtilsTest.java
index 854263c911..215d13791f 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/utils/StageUtilsTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/utils/StageUtilsTest.java
@@ -37,6 +37,7 @@ import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.api.services.AmbariMetaInfo;
 import org.apache.ambari.server.orm.DBAccessor;
 import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
 import org.apache.ambari.server.security.SecurityHelper;
 import org.apache.ambari.server.security.encryption.CredentialStoreService;
 import org.apache.ambari.server.stack.StackManagerFactory;
@@ -118,6 +119,7 @@ public class StageUtilsTest extends EasyMockSupport {
         bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
         bind(HostDAO.class).toInstance(createNiceMock(HostDAO.class));
         bind(PersistedState.class).toInstance(createNiceMock(PersistedState.class));
        bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
       }
     });
 
- 
2.19.1.windows.1

