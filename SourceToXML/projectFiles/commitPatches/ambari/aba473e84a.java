From aba473e84a5a24d12b29a2bf9e858019c023f6fd Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Fri, 31 Mar 2017 12:35:25 -0400
Subject: [PATCH] AMBARI-20646 - Large Long Running Requests Can Slow Down the
 ActionScheduler (jonathanhurley)

--
 .../actionmanager/ActionDBAccessor.java       |  16 +-
 .../actionmanager/ActionDBAccessorImpl.java   |  13 +-
 .../server/actionmanager/ActionScheduler.java |   2 +-
 .../ambari/server/orm/dao/StageDAO.java       |  68 ++++-----
 .../server/orm/entities/StageEntity.java      |   9 +-
 .../serveraction/ServerActionExecutor.java    | 114 +++++++-------
 .../TestActionDBAccessorImpl.java             |  27 ++--
 .../actionmanager/TestActionScheduler.java    | 139 ++++++++++--------
 .../ambari/server/orm/dao/RequestDAOTest.java |  21 ++-
 .../ServerActionExecutorTest.java             |   2 +-
 .../RetryUpgradeActionServiceTest.java        |  12 +-
 11 files changed, 227 insertions(+), 196 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
index 9325d03f04..b0550c0138 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
@@ -76,17 +76,19 @@ public interface ActionDBAccessor {
                        boolean skipSupported, boolean hostUnknownState);
 
   /**
   * Returns all the pending stages, including queued and not-queued. A stage is
   * considered in progress if it is in progress for any host.
   * Returns the next stage which is in-progress for every in-progress request
   * in the system. Since stages are always synchronous, there is no reason to
   * return more than the most recent stage per request. Returning every single
   * stage in the requesrt would be extremely inffecient and wasteful. However,
   * since requests can run in parallel, this method must return the most recent
   * stage for every request. The results will be sorted by request ID.
    * <p/>
   * The results will be sorted by request ID and then stage ID making this call
   * expensive in some scenarios. Use {@link #getCommandsInProgressCount()} in
   * order to determine if there are stages that are in progress before getting
   * the stages from this method.
   * Use {@link #getCommandsInProgressCount()} in order to determine if there
   * are stages that are in progress before getting the stages from this method.
    *
    * @see HostRoleStatus#IN_PROGRESS_STATUSES
    */
  public List<Stage> getStagesInProgress();
  public List<Stage> getFirstStageInProgressPerRequest();
 
   /**
    * Returns all the pending stages in a request, including queued and not-queued. A stage is
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
index ab4feaa552..8c4eae89e7 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
@@ -285,11 +285,16 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
    * {@inheritDoc}
    */
   @Override
  @Experimental(feature = ExperimentalFeature.PARALLEL_PROCESSING)
  public List<Stage> getStagesInProgress() {
    List<StageEntity> stageEntities = stageDAO.findByStatuses(
  public List<Stage> getFirstStageInProgressPerRequest() {
    List<StageEntity> stageEntities = stageDAO.findFirstStageByStatus(
       HostRoleStatus.IN_PROGRESS_STATUSES);
    return getStagesForEntities(stageEntities);

    List<Stage> stages = new ArrayList<>(stageEntities.size());
    for (StageEntity stageEntity : stageEntities) {
      stages.add(stageFactory.createExisting(stageEntity));
    }

    return stages;
   }
 
   @Experimental(feature = ExperimentalFeature.PARALLEL_PROCESSING)
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
index 0984c5cd94..758db3516f 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
@@ -355,7 +355,7 @@ class ActionScheduler implements Runnable {
       }
 
       Set<Long> runningRequestIds = new HashSet<>();
      List<Stage> stages = db.getStagesInProgress();
      List<Stage> stages = db.getFirstStageInProgressPerRequest();
       if (LOG.isDebugEnabled()) {
         LOG.debug("Scheduler wakes up");
         LOG.debug("Processing {} in progress stages ", stages.size());
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/StageDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/StageDAO.java
index 5151fb3b70..c2919b20d5 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/StageDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/StageDAO.java
@@ -22,10 +22,8 @@ import java.util.ArrayList;
 import java.util.Collection;
 import java.util.EnumSet;
 import java.util.HashMap;
import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
import java.util.Set;
 
 import javax.persistence.EntityManager;
 import javax.persistence.TypedQuery;
@@ -129,41 +127,6 @@ public class StageDAO {
     return daoUtils.selectList(query, requestId);
   }
 
  /**
   * Gets all of the stage IDs associated with a request.
   *
   * @param requestId
   * @return the list of stage IDs.
   */
  @RequiresSession
  public List<Long> findIdsByRequestId(long requestId) {
    TypedQuery<Long> query = entityManagerProvider.get().createNamedQuery(
        "StageEntity.findIdsByRequestId", Long.class);

    query.setParameter("requestId", requestId);
    return daoUtils.selectList(query);
  }

  /**
   * Get the list of stage entities for the given request id and stage ids.
   *
   * @param requestId  the request ids
   * @param stageIds   the set of stage ids
   *
   * @return the set of entities for the given ids
   */
  @RequiresSession
  public List<StageEntity> findByStageIds(Long requestId, Set<Long> stageIds) {
    List<StageEntity> stageEntities = new LinkedList<>();

    for (StageEntity stage : findByRequestId(requestId)) {
      if (stageIds.contains(stage.getStageId())) {
        stageEntities.add(stage);
      }
    }
    return stageEntities;
  }

   @RequiresSession
   public List<StageEntity> findByRequestIdAndCommandStatuses(Long requestId, Collection<HostRoleStatus> statuses) {
     TypedQuery<StageEntity> query = entityManagerProvider.get().createNamedQuery(
@@ -175,17 +138,36 @@ public class StageDAO {
   }
 
   /**
   * Finds the first stage matching any of the specified statuses for every
   * request. For example, to find the first {@link HostRoleStatus#IN_PROGRESS}
   * stage for every request, pass in
   * {@link HostRoleStatus#IN_PROGRESS_STATUSES}.
    *
   * @param statuses {@link HostRoleStatus}
   * @return list of stage entities
   * @param statuses
   *          {@link HostRoleStatus}
   * @return the list of the first matching stage for the given statuses for
   *         every request.
    */
   @RequiresSession
  public List<StageEntity> findByStatuses(Collection<HostRoleStatus> statuses) {
    TypedQuery<StageEntity> query = entityManagerProvider.get().createNamedQuery(
        "StageEntity.findByStatuses", StageEntity.class);
  public List<StageEntity> findFirstStageByStatus(Collection<HostRoleStatus> statuses) {
    TypedQuery<Object[]> query = entityManagerProvider.get().createNamedQuery(
        "StageEntity.findFirstStageByStatus", Object[].class);
 
     query.setParameter("statuses", statuses);
    return daoUtils.selectList(query);

    List<Object[]> results = daoUtils.selectList(query);
    List<StageEntity> stages = new ArrayList<>();

    for (Object[] result : results) {
      StageEntityPK stagePK = new StageEntityPK();
      stagePK.setRequestId((Long) result[0]);
      stagePK.setStageId((Long) result[1]);

      StageEntity stage = findByPK(stagePK);
      stages.add(stage);
    }

    return stages;
   }
 
   @RequiresSession
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/StageEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/StageEntity.java
index f68338f291..49c1594ca7 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/StageEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/StageEntity.java
@@ -46,17 +46,14 @@ import org.apache.ambari.server.actionmanager.HostRoleStatus;
 @IdClass(org.apache.ambari.server.orm.entities.StageEntityPK.class)
 @NamedQueries({
     @NamedQuery(
        name = "StageEntity.findByStatuses",
        query = "SELECT stage from StageEntity stage WHERE stage.status IN :statuses ORDER BY stage.requestId, stage.stageId"),
        name = "StageEntity.findFirstStageByStatus",
        query = "SELECT stage.requestId, MIN(stage.stageId) from StageEntity stage, HostRoleCommandEntity hrc WHERE hrc.status IN :statuses AND hrc.stageId = stage.stageId AND hrc.requestId = stage.requestId GROUP by stage.requestId ORDER BY stage.requestId"),
     @NamedQuery(
         name = "StageEntity.findByPK",
         query = "SELECT stage from StageEntity stage WHERE stage.requestId = :requestId AND stage.stageId = :stageId"),
     @NamedQuery(
         name = "StageEntity.findByRequestIdAndCommandStatuses",
        query = "SELECT stage from StageEntity stage WHERE stage.status IN :statuses AND stage.requestId = :requestId ORDER BY stage.stageId"),
    @NamedQuery(
        name = "StageEntity.findIdsByRequestId",
        query = "SELECT stage.stageId FROM StageEntity stage WHERE stage.requestId = :requestId ORDER BY stage.stageId ASC") })
        query = "SELECT stage from StageEntity stage WHERE stage.status IN :statuses AND stage.requestId = :requestId ORDER BY stage.stageId") })
 public class StageEntity {
 
   @Column(name = "cluster_id", updatable = false, nullable = false)
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java
index b0be6b3c38..68124fc3d4 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/serveraction/ServerActionExecutor.java
@@ -19,11 +19,15 @@
 package org.apache.ambari.server.serveraction;
 
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
 
 import org.apache.ambari.server.AmbariException;
 import org.apache.ambari.server.Role;
@@ -32,13 +36,11 @@ import org.apache.ambari.server.actionmanager.ActionDBAccessor;
 import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
 import org.apache.ambari.server.actionmanager.HostRoleCommand;
 import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
 import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
 import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.utils.StageUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.security.core.context.SecurityContextHolder;
@@ -74,6 +76,7 @@ public class ServerActionExecutor {
   @Inject
   private static Configuration configuration;
 

   /**
    * Maps request IDs to "blackboards" of shared data.
    * <p/>
@@ -83,13 +86,6 @@ public class ServerActionExecutor {
   private final Map<Long, ConcurrentMap<String, Object>> requestSharedDataMap =
     new HashMap<>();
 
  /**
   * The hostname of the (Ambari) server.
   * <p/>
   * This hostname is cached so that cycles are spent querying for it more than once.
   */
  private final String serverHostName;

   /**
    * Database accessor to query and update the database of action commands.
    */
@@ -116,6 +112,13 @@ public class ServerActionExecutor {
    */
   private Thread executorThread = null;
 
  /**
   * A timer used to clear out {@link #requestSharedDataMap}. Since this "cache"
   * isn't timer- or access-based, then we must periodically check it in order
   * to clear out any stale data.
   */
  private final Timer cacheTimer = new Timer("server-action-executor-cache-timer", true);

   /**
    * Statically initialize the Injector
    * <p/>
@@ -134,9 +137,12 @@ public class ServerActionExecutor {
    * @param sleepTimeMS the time (in milliseconds) to wait between polling the database for more tasks
    */
   public ServerActionExecutor(ActionDBAccessor db, long sleepTimeMS) {
    serverHostName = StageUtils.getHostName();
     this.db = db;
     this.sleepTimeMS = (sleepTimeMS < 1) ? POLLING_TIMEOUT_MS : sleepTimeMS;

    // start in 1 hour, run every hour
    cacheTimer.schedule(new ServerActionSharedRequestEvictor(), TimeUnit.HOURS.toMillis(1),
        TimeUnit.HOURS.toMillis(1));
   }
 
   /**
@@ -241,48 +247,6 @@ public class ServerActionExecutor {
     }
   }
 
  /**
   * Cleans up orphaned shared data Maps due to completed or failed request
   * contexts. We are unable to use {@link Request#getStatus()} since this field
   * is not populated in the database but, instead, calculated in realtime.
   */
  private void cleanRequestShareDataContexts() {
    // if the cache is empty, do nothing
    if (requestSharedDataMap.isEmpty()) {
      return;
    }

    try {
      // for every item in the map, get the request and check its status
      synchronized (requestSharedDataMap) {
        Set<Long> requestIds = requestSharedDataMap.keySet();
        List<Request> requests = db.getRequests(requestIds);
        for (Request request : requests) {
          // calcuate the status from the stages and then remove from the map if
          // necessary
          CalculatedStatus calculatedStatus = CalculatedStatus.statusFromStages(
              request.getStages());

          // calcuate the status of the request
          HostRoleStatus status = calculatedStatus.getStatus();

          // remove the request from the map if the request is COMPLETED or
          // FAILED
          switch (status) {
            case FAILED:
            case COMPLETED:
              requestSharedDataMap.remove(request.getRequestId());
              break;
            default:
              break;
          }
        }
      }
    } catch (Exception exception) {
      LOG.warn("Unable to clear the server-side action request cache", exception);
    }
  }

   /**
    * A helper method to create CommandReports indicating the action/task is in progress
    *
@@ -450,8 +414,6 @@ public class ServerActionExecutor {
         }
       }
     }

    cleanRequestShareDataContexts();
   }
 
   /**
@@ -599,4 +561,46 @@ public class ServerActionExecutor {
       this.executionCommand = executionCommand;
     }
   }

  /**
   * The {@link ServerActionSharedRequestEvictor} is used to clear the shared
   * request cache periodically. This service will only run periodically and,
   * when it does, it will try to make the least expensive call to determine if
   * entries need to be evicted.
   */
  private class ServerActionSharedRequestEvictor extends TimerTask {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      // if the cache is empty, do nothing
      if (requestSharedDataMap.isEmpty()) {
        return;
      }

      // if the cache has requests, see if any are still in progress
      try {
        // find the requests in progress; there's no need to get the request
        // itself since that could be a massive object; we just need the ID
        Set<Long> requestsInProgress = new HashSet<>();
        List<Stage> currentStageInProgressPerRequest = db.getFirstStageInProgressPerRequest();
        for (Stage stage : currentStageInProgressPerRequest) {
          requestsInProgress.add(stage.getRequestId());
        }

        // for every item in the map, get the request and check its status
        synchronized (requestSharedDataMap) {
          Set<Long> cachedRequestIds = requestSharedDataMap.keySet();
          for (long cachedRequestId : cachedRequestIds) {
            if (!requestsInProgress.contains(cachedRequestId)) {
              requestSharedDataMap.remove(cachedRequestId);
            }
          }
        }
      } catch (Exception exception) {
        LOG.warn("Unable to clear the server-side action request cache", exception);
      }
    }
  }
 }
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionDBAccessorImpl.java b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionDBAccessorImpl.java
index 81eef3b7c3..c1056dd813 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionDBAccessorImpl.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionDBAccessorImpl.java
@@ -202,11 +202,11 @@ public class TestActionDBAccessorImpl {
   public void testGetStagesInProgressWithFailures() throws AmbariException {
     populateActionDB(db, hostName, requestId, stageId);
     populateActionDB(db, hostName, requestId + 1, stageId);
    List<Stage> stages = db.getStagesInProgress();
    List<Stage> stages = db.getFirstStageInProgressPerRequest();
     assertEquals(2, stages.size());
 
     db.abortOperation(requestId);
    stages = db.getStagesInProgress();
    stages = db.getFirstStageInProgressPerRequest();
     assertEquals(1, stages.size());
     assertEquals(requestId+1, stages.get(0).getRequestId());
   }
@@ -220,9 +220,9 @@ public class TestActionDBAccessorImpl {
 
     // verify stages and proper ordering
     int commandsInProgressCount = db.getCommandsInProgressCount();
    List<Stage> stages = db.getStagesInProgress();
    List<Stage> stages = db.getFirstStageInProgressPerRequest();
     assertEquals(18, commandsInProgressCount);
    assertEquals(9, stages.size());
    assertEquals(3, stages.size());
 
     long lastRequestId = Integer.MIN_VALUE;
     for (Stage stage : stages) {
@@ -235,9 +235,9 @@ public class TestActionDBAccessorImpl {
 
     // verify stages and proper ordering
     commandsInProgressCount = db.getCommandsInProgressCount();
    stages = db.getStagesInProgress();
    stages = db.getFirstStageInProgressPerRequest();
     assertEquals(12, commandsInProgressCount);
    assertEquals(6, stages.size());
    assertEquals(2, stages.size());
 
     // find the first stage, and change one command to COMPLETED
     stages.get(0).setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(),
@@ -247,9 +247,9 @@ public class TestActionDBAccessorImpl {
 
     // the first stage still has at least 1 command IN_PROGRESS
     commandsInProgressCount = db.getCommandsInProgressCount();
    stages = db.getStagesInProgress();
    stages = db.getFirstStageInProgressPerRequest();
     assertEquals(11, commandsInProgressCount);
    assertEquals(6, stages.size());
    assertEquals(2, stages.size());
 
     // find the first stage, and change the other command to COMPLETED
     stages.get(0).setHostRoleStatus(hostName,
@@ -260,9 +260,9 @@ public class TestActionDBAccessorImpl {
 
     // verify stages and proper ordering
     commandsInProgressCount = db.getCommandsInProgressCount();
    stages = db.getStagesInProgress();
    stages = db.getFirstStageInProgressPerRequest();
     assertEquals(10, commandsInProgressCount);
    assertEquals(5, stages.size());
    assertEquals(2, stages.size());
   }
 
   @Test
@@ -274,15 +274,16 @@ public class TestActionDBAccessorImpl {
     }
 
     // create 1 request, 3 stages per host, each with 2 commands
    for (int i = 0; i < 1000; i++) {
    int requestCount = 1000;
    for (int i = 0; i < requestCount; i++) {
       String hostName = "c64-" + i;
       populateActionDBMultipleStages(3, db, hostName, requestId + i, stageId);
     }
 
     int commandsInProgressCount = db.getCommandsInProgressCount();
    List<Stage> stages = db.getStagesInProgress();
    List<Stage> stages = db.getFirstStageInProgressPerRequest();
     assertEquals(6000, commandsInProgressCount);
    assertEquals(3000, stages.size());
    assertEquals(requestCount, stages.size());
   }
 
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
index 2b5d2f30e4..d7d3d404d0 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
@@ -210,13 +210,12 @@ public class TestActionScheduler {
     ActionDBAccessor db = mock(ActionDBAccessorImpl.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
     Mockito.doNothing().when(hostRoleCommandDAOMock).publishTaskCreateEvent(anyListOf(HostRoleCommand.class));
    List<Stage> stages = new ArrayList<>();
     Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
       "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stages.add(s);
 
    List<Stage> stages = Collections.singletonList(s);
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     RequestEntity request = mock(RequestEntity.class);
     when(request.isExclusive()).thenReturn(false);
@@ -307,20 +306,19 @@ public class TestActionScheduler {
     hostEntity.setHostName(hostname);
     hostDAO.create(hostEntity);
 
    List<Stage> stages = new ArrayList<>();
     final Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
       "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
     s.addHostRoleExecutionCommand(hostname, Role.SECONDARY_NAMENODE, RoleCommand.INSTALL,
             new ServiceComponentHostInstallEvent("SECONDARY_NAMENODE", hostname, System.currentTimeMillis(), "HDP-1.2.0"),
             "cluster1", "HDFS", false, false);
     s.setHostRoleStatus(hostname, "SECONDARY_NAMENODE", HostRoleStatus.IN_PROGRESS);
    stages.add(s);
    List<Stage> stages = Collections.singletonList(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
     Mockito.doNothing().when(hostRoleCommandDAOMock).publishTaskCreateEvent(anyListOf(HostRoleCommand.class));
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     RequestEntity request = mock(RequestEntity.class);
     when(request.isExclusive()).thenReturn(false);
@@ -397,10 +395,10 @@ public class TestActionScheduler {
     when(host.getState()).thenReturn(HostState.HEARTBEAT_LOST);
     when(host.getHostName()).thenReturn(hostname);
 
    final List<Stage> stages = new ArrayList<>();
     final Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
       "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stages.add(s);

    List<Stage> stages = Collections.singletonList(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
 
@@ -409,7 +407,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
     Mockito.doNothing().when(hostRoleCommandDAOMock).publishTaskCreateEvent(anyListOf(HostRoleCommand.class));
 
@@ -484,14 +482,13 @@ public class TestActionScheduler {
     when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
     when(serviceObj.getCluster()).thenReturn(oneClusterMock);
 
    final List<Stage> stages = new ArrayList<>();
     final Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 1L, "stageWith2Tasks",
       CLUSTER_HOST_INFO, "{\"command_param\":\"param_value\"}", "{\"host_param\":\"param_value\"}");
     addInstallTaskToStage(stage, hostname1, "cluster1", Role.DATANODE,
       RoleCommand.INSTALL, Service.Type.HDFS, 1);
     addInstallTaskToStage(stage, hostname2, "cluster1", Role.NAMENODE,
       RoleCommand.INSTALL, Service.Type.HDFS, 2);
    stages.add(stage);
    final List<Stage> stages = Collections.singletonList(stage);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
 
@@ -500,7 +497,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
     Mockito.doNothing().when(hostRoleCommandDAOMock).publishTaskCreateEvent(anyListOf(HostRoleCommand.class));
 
@@ -606,10 +603,9 @@ public class TestActionScheduler {
     Clusters fsm = mock(Clusters.class);
     UnitOfWork unitOfWork = mock(UnitOfWork.class);
 
    List<Stage> stages = new ArrayList<>();
     Map<String, String> payload = new HashMap<>();
     final Stage s = getStageWithServerAction(1, 977, payload, "test", 1200, false, false);
    stages.add(s);
    List<Stage> stages = Collections.singletonList(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
@@ -620,7 +616,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
     doAnswer(new Answer<Void>() {
       @Override
       public Void answer(InvocationOnMock invocation) throws Throwable {
@@ -737,7 +733,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     Properties properties = new Properties();
     properties.put(Configuration.PARALLEL_STAGE_EXECUTION.getKey(), "true");
@@ -766,11 +762,10 @@ public class TestActionScheduler {
     Clusters fsm = mock(Clusters.class);
     UnitOfWork unitOfWork = mock(UnitOfWork.class);
 
    List<Stage> stages = new ArrayList<>();
     Map<String, String> payload = new HashMap<>();
     payload.put(MockServerAction.PAYLOAD_FORCE_FAIL, "timeout");
     final Stage s = getStageWithServerAction(1, 977, payload, "test", 2, false, false);
    stages.add(s);
    List<Stage> stages = Collections.singletonList(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
@@ -781,7 +776,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
     doAnswer(new Answer<Void>() {
       @Override
       public Void answer(InvocationOnMock invocation) throws Throwable {
@@ -981,11 +976,10 @@ public class TestActionScheduler {
     Clusters fsm = mock(Clusters.class);
     UnitOfWork unitOfWork = mock(UnitOfWork.class);
 
    List<Stage> stages = new ArrayList<>();
     Map<String, String> payload = new HashMap<>();
     payload.put(MockServerAction.PAYLOAD_FORCE_FAIL, "exception");
     final Stage s = getStageWithServerAction(1, 977, payload, "test", 300, false, false);
    stages.add(s);
    List<Stage> stages = Collections.singletonList(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
@@ -996,7 +990,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     doAnswer(new Answer<Void>() {
       @Override
@@ -1146,7 +1140,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     Properties properties = new Properties();
     Configuration conf = new Configuration(properties);
@@ -1238,7 +1232,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     Properties properties = new Properties();
     properties.put(Configuration.PARALLEL_STAGE_EXECUTION.getKey(), "false");
@@ -1315,7 +1309,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     Properties properties = new Properties();
     properties.put(Configuration.PARALLEL_STAGE_EXECUTION.getKey(), "true");
@@ -1357,9 +1351,13 @@ public class TestActionScheduler {
     when(scomp.getServiceComponentHosts()).thenReturn(hosts);
 
     final List<Stage> stages = new ArrayList<>();

     stages.add(
         getStageWithSingleTask(
             hostname, "cluster1", Role.NAMENODE, RoleCommand.UPGRADE, Service.Type.HDFS, 1, 1, 1));

    List<Stage> firstStageInProgress = Collections.singletonList(stages.get(0));

     stages.add(
         getStageWithSingleTask(
             hostname, "cluster1", Role.DATANODE, RoleCommand.UPGRADE, Service.Type.HDFS, 2, 2, 1));
@@ -1376,7 +1374,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(firstStageInProgress);
     doAnswer(new Answer<Void>() {
       @Override
       public Void answer(InvocationOnMock invocation) throws Throwable {
@@ -1517,8 +1515,6 @@ public class TestActionScheduler {
     hostDAO.create(hostEntity1);
     hostDAO.create(hostEntity2);
 
    final List<Stage> stages = new ArrayList<>();

     long now = System.currentTimeMillis();
     Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 1L,
         "testRequestFailureBasedOnSuccessFactor", CLUSTER_HOST_INFO, "", "");
@@ -1545,7 +1541,7 @@ public class TestActionScheduler {
     addHostRoleExecutionCommand(now, stage, Role.GANGLIA_MONITOR, Service.Type.GANGLIA,
         RoleCommand.INSTALL, host2, "cluster1");
 
    stages.add(stage);
    final List<Stage> stages = Collections.singletonList(stage);
 
     HostRoleStatus[] statusesAtIterOne = {HostRoleStatus.QUEUED, HostRoleStatus.QUEUED,
         HostRoleStatus.QUEUED, HostRoleStatus.QUEUED, HostRoleStatus.FAILED,
@@ -1572,7 +1568,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
     doAnswer(new Answer<Void>() {
       @Override
       public Void answer(InvocationOnMock invocation) throws Throwable {
@@ -1740,7 +1736,9 @@ public class TestActionScheduler {
         "cluster1", Service.Type.HDFS.toString(), false, false);
     stage.getExecutionCommandWrapper("host3",
         Role.DATANODE.toString()).getExecutionCommand();

     stages.add(stage);
    List<Stage> stageInProgress = Collections.singletonList(stage);
 
     stage.getOrderedHostRoleCommands().get(0).setTaskId(1);
     stage.getOrderedHostRoleCommands().get(1).setTaskId(2);
@@ -1758,8 +1756,8 @@ public class TestActionScheduler {
     when(request.isExclusive()).thenReturn(false);
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getCommandsInProgressCount()).thenReturn(stageInProgress.size());
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stageInProgress);
     doAnswer(new Answer<Void>() {
       @Override
       public Void answer(InvocationOnMock invocation) throws Throwable {
@@ -2013,7 +2011,7 @@ public class TestActionScheduler {
       "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
 
     when(db.getCommandsInProgressCount()).thenReturn(1);
    when(db.getStagesInProgress()).thenReturn(Collections.singletonList(s1));
    when(db.getFirstStageInProgressPerRequest()).thenReturn(Collections.singletonList(s1));
 
     //Keep large number of attempts so that the task is not expired finally
     //Small action timeout to test rescheduling
@@ -2030,7 +2028,7 @@ public class TestActionScheduler {
     assertEquals(clusterHostInfo1, ((ExecutionCommand) (ac.get(0))).getClusterHostInfo());
 
     when(db.getCommandsInProgressCount()).thenReturn(1);
    when(db.getStagesInProgress()).thenReturn(Collections.singletonList(s2));
    when(db.getFirstStageInProgressPerRequest()).thenReturn(Collections.singletonList(s2));
 
     //Verify that ActionSheduler does not return cached value of cluster host info for new requestId
     ac = waitForQueueSize(hostname, aq, 1, scheduler);
@@ -2083,14 +2081,13 @@ public class TestActionScheduler {
                 "dummyService", "dummyComponent", "dummyHostname"));
     when(serviceObj.getCluster()).thenReturn(oneClusterMock);
 
    final List<Stage> stages = new ArrayList<>();
     Stage stage1 = stageFactory.createNew(1, "/tmp", "cluster1", 1L, "stageWith2Tasks",
             CLUSTER_HOST_INFO, "", "");
     addInstallTaskToStage(stage1, hostname1, "cluster1", Role.HBASE_MASTER,
             RoleCommand.INSTALL, Service.Type.HBASE, 1);
     addInstallTaskToStage(stage1, hostname1, "cluster1", Role.HBASE_REGIONSERVER,
             RoleCommand.INSTALL, Service.Type.HBASE, 2);
    stages.add(stage1);
    final List<Stage> stages = Collections.singletonList(stage1);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
 
@@ -2099,7 +2096,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
 
     ActionScheduler scheduler = new ActionScheduler(100, 50000, db, aq, fsm, 3,
         new HostsMap((String) null), unitOfWork, null, conf, entityManagerProviderMock,
@@ -2155,10 +2152,9 @@ public class TestActionScheduler {
     Clusters fsm = mock(Clusters.class);
     UnitOfWork unitOfWork = mock(UnitOfWork.class);
 
    List<Stage> stages = new ArrayList<>();
     Map<String, String> payload = new HashMap<>();
     final Stage s = getStageWithServerAction(1, 977, payload, "test", 300, false, false);
    stages.add(s);
    List<Stage> stages = Collections.singletonList(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
@@ -2169,7 +2165,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
     doAnswer(new Answer<Void>() {
       @Override
       public Void answer(InvocationOnMock invocation) throws Throwable {
@@ -2259,6 +2255,7 @@ public class TestActionScheduler {
     long requestId = 1;
     final List<Stage> allStages = new ArrayList<>();
     final List<Stage> stagesInProgress = new ArrayList<>();
    final List<Stage> firstStageInProgress = new ArrayList<>();
     final List<HostRoleCommand> tasksInProgress = new ArrayList<>();
     final List<HostRoleCommandEntity> hrcEntitiesInProgress = new ArrayList<>();
 
@@ -2279,6 +2276,7 @@ public class TestActionScheduler {
         Service.Type.HDFS, namenodeCmdTaskId, 2, (int) requestId);
 
     tasksInProgress.addAll(stageWithTask.getOrderedHostRoleCommands());
    firstStageInProgress.add(stageWithTask);
     stagesInProgress.add(stageWithTask);
     allStages.add(stageWithTask);
 
@@ -2319,7 +2317,7 @@ public class TestActionScheduler {
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
     when(db.getCommandsInProgressCount()).thenReturn(stagesInProgress.size());
    when(db.getStagesInProgress()).thenReturn(stagesInProgress);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stagesInProgress);
     when(db.getStagesInProgressForRequest(requestId)).thenReturn(stagesInProgress);
     when(db.getAllStages(anyLong())).thenReturn(allStages);
 
@@ -2458,26 +2456,34 @@ public class TestActionScheduler {
     long requestId2 = 2;
     long requestId3 = 3;
 
    final List<Stage> firstStageInProgressByRequest = new ArrayList<>();
     final List<Stage> stagesInProgress = new ArrayList<>();
     int namenodeCmdTaskId = 1;
    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname1, "cluster1", Role.NAMENODE, RoleCommand.START,
                    Service.Type.HDFS, namenodeCmdTaskId, 1, (int) requestId1));
    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname1, "cluster1", Role.DATANODE, RoleCommand.START,
                    Service.Type.HDFS, 2, 2, (int) requestId1));
    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname2, "cluster1", Role.DATANODE, RoleCommand.STOP, //Exclusive
                    Service.Type.HDFS, 3, 3, (int) requestId2));
 
    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname3, "cluster1", Role.DATANODE, RoleCommand.START,
                    Service.Type.HDFS, 4, 4, (int) requestId3));
    Stage request1Stage1 = getStageWithSingleTask(hostname1, "cluster1", Role.NAMENODE,
        RoleCommand.START,
        Service.Type.HDFS, namenodeCmdTaskId, 1, (int) requestId1);

    Stage request1Stage2 = getStageWithSingleTask(hostname1, "cluster1", Role.DATANODE,
        RoleCommand.START,
        Service.Type.HDFS, 2, 2, (int) requestId1);

    Stage request2Stage1 = getStageWithSingleTask(hostname2, "cluster1", Role.DATANODE,
        RoleCommand.STOP, // Exclusive
        Service.Type.HDFS, 3, 3, (int) requestId2);

    Stage request3Stage1 = getStageWithSingleTask(hostname3, "cluster1", Role.DATANODE,
        RoleCommand.START,
        Service.Type.HDFS, 4, 4, (int) requestId3);
 
    firstStageInProgressByRequest.add(request1Stage1);
    firstStageInProgressByRequest.add(request2Stage1);
    firstStageInProgressByRequest.add(request3Stage1);

    stagesInProgress.add(request1Stage1);
    stagesInProgress.add(request1Stage2);
    stagesInProgress.add(request2Stage1);
    stagesInProgress.add(request3Stage1);
 
     Host host1 = mock(Host.class);
     when(fsm.getHost(anyString())).thenReturn(host1);
@@ -2498,7 +2504,7 @@ public class TestActionScheduler {
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
     Mockito.doNothing().when(hostRoleCommandDAOMock).publishTaskCreateEvent(anyListOf(HostRoleCommand.class));
     when(db.getCommandsInProgressCount()).thenReturn(stagesInProgress.size());
    when(db.getStagesInProgress()).thenReturn(stagesInProgress);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(firstStageInProgressByRequest);
 
     List<HostRoleCommand> requestTasks = new ArrayList<>();
     for (Stage stage : stagesInProgress) {
@@ -2585,6 +2591,10 @@ public class TestActionScheduler {
     Assert.assertFalse(startedRequests.containsKey(requestId3));
 
     stagesInProgress.remove(0);
    firstStageInProgressByRequest.clear();
    firstStageInProgressByRequest.add(request1Stage2);
    firstStageInProgressByRequest.add(request2Stage1);
    firstStageInProgressByRequest.add(request3Stage1);
 
     scheduler.doWork();
 
@@ -2595,6 +2605,9 @@ public class TestActionScheduler {
     // Execution of request 2
 
     stagesInProgress.remove(0);
    firstStageInProgressByRequest.clear();
    firstStageInProgressByRequest.add(request2Stage1);
    firstStageInProgressByRequest.add(request3Stage1);
 
     scheduler.doWork();
 
@@ -2605,6 +2618,8 @@ public class TestActionScheduler {
     // Execution of request 3
 
     stagesInProgress.remove(0);
    firstStageInProgressByRequest.clear();
    firstStageInProgressByRequest.add(request3Stage1);
 
     scheduler.doWork();
 
@@ -2715,6 +2730,7 @@ public class TestActionScheduler {
     Stage stage = null;
     Stage stage2 = null;
     final List<Stage> stages = new ArrayList<>();
    final List<Stage> firstStageInProgress = new ArrayList<>();
     stages.add(stage = getStageWithSingleTask(hostname1, "cluster1", Role.NAMENODE,
         RoleCommand.STOP, Service.Type.HDFS, 1, 1, 1));
 
@@ -2735,6 +2751,9 @@ public class TestActionScheduler {
     HostRoleCommand command = stage.getOrderedHostRoleCommands().iterator().next();
     command.setStatus(HostRoleStatus.FAILED);
 
    // still in progress even though 1 task has been failed
    firstStageInProgress.add(stage);

     ActionDBAccessor db = mock(ActionDBAccessor.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
     Mockito.doNothing().when(hostRoleCommandDAOMock).publishTaskCreateEvent(anyListOf(HostRoleCommand.class));
@@ -2743,8 +2762,8 @@ public class TestActionScheduler {
     when(request.isExclusive()).thenReturn(false);
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getCommandsInProgressCount()).thenReturn(firstStageInProgress.size());
    when(db.getFirstStageInProgressPerRequest()).thenReturn(firstStageInProgress);
 
     doAnswer(new Answer<Void>() {
       @Override
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/RequestDAOTest.java b/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/RequestDAOTest.java
index 9b62671b5a..17cebc3a29 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/RequestDAOTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/orm/dao/RequestDAOTest.java
@@ -42,6 +42,7 @@ import org.apache.ambari.server.orm.entities.RequestEntity;
 import org.apache.ambari.server.orm.entities.ResourceEntity;
 import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
 import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
 import org.apache.ambari.server.security.authorization.ResourceType;
 import org.junit.After;
 import org.junit.Assert;
@@ -124,7 +125,25 @@ public class RequestDAOTest {
     group.add(4L);
 
     // !!! accepted
    List<StageEntity> stages = stageDAO.findByStageIds(requestEntity.getRequestId(), group);
    List<StageEntity> stages = new ArrayList<>();
    StageEntityPK primaryKey = new StageEntityPK();
    primaryKey.setRequestId(requestEntity.getRequestId());
    primaryKey.setStageId(2L);

    StageEntity stage = stageDAO.findByPK(primaryKey);
    Assert.assertNotNull(stage);
    stages.add(stage);

    primaryKey.setStageId(3L);
    stage = stageDAO.findByPK(primaryKey);
    Assert.assertNotNull(stage);
    stages.add(stage);

    primaryKey.setStageId(4L);
    stage = stageDAO.findByPK(primaryKey);
    Assert.assertNotNull(stage);
    stages.add(stage);

     CalculatedStatus calc3 = CalculatedStatus.statusFromStageEntities(stages);
 
     // !!! aggregated
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/serveraction/ServerActionExecutorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/serveraction/ServerActionExecutorTest.java
index 44d5b63836..2feef41edf 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/serveraction/ServerActionExecutorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/serveraction/ServerActionExecutorTest.java
@@ -268,7 +268,7 @@ public class ServerActionExecutorTest {
   private ActionDBAccessor createMockActionDBAccessor(final Request request, final List<Stage> stages) {
     ActionDBAccessor db = mock(ActionDBAccessor.class);
 
    when(db.getStagesInProgress()).thenReturn(stages);
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
     doAnswer(new Answer() {
       @Override
       public Object answer(InvocationOnMock invocation) throws Throwable {
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/state/services/RetryUpgradeActionServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/state/services/RetryUpgradeActionServiceTest.java
index e2ce6e785b..2c0b50753d 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/state/services/RetryUpgradeActionServiceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/state/services/RetryUpgradeActionServiceTest.java
@@ -20,7 +20,6 @@ package org.apache.ambari.server.state.services;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.HashSet;
 import java.util.List;
 
 import org.apache.ambari.server.AmbariException;
@@ -42,6 +41,7 @@ import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
 import org.apache.ambari.server.orm.entities.RequestEntity;
 import org.apache.ambari.server.orm.entities.StackEntity;
 import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
 import org.apache.ambari.server.orm.entities.UpgradeEntity;
 import org.apache.ambari.server.state.Cluster;
 import org.apache.ambari.server.state.Clusters;
@@ -148,9 +148,11 @@ public class RetryUpgradeActionServiceTest {
     }
 
     // Case 4: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that does NOT meet conditions to be retried.
    List<StageEntity> stages = stageDAO.findByStageIds(upgradeRequestId, new HashSet<Long>(){{ add(stageId); }});
    Assert.assertTrue(!stages.isEmpty() && stages.size() == 1);
    StageEntity stageEntity = stages.get(0);
    StageEntityPK primaryKey = new StageEntityPK();
    primaryKey.setRequestId(upgradeRequestId);
    primaryKey.setStageId(stageId);

    StageEntity stageEntity = stageDAO.findByPK(primaryKey);
 
     HostRoleCommandEntity hrc2 = new HostRoleCommandEntity();
     hrc2.setStage(stageEntity);
@@ -202,7 +204,7 @@ public class RetryUpgradeActionServiceTest {
 
     // Ensure that task 2 transitioned from HOLDING_TIMEDOUT to PENDING
     Assert.assertEquals(HostRoleStatus.PENDING, hostRoleCommandDAO.findByPK(hrc2.getTaskId()).getStatus());
    

     // Case 7: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that was already retried and has now expired.
     now = System.currentTimeMillis();
     hrc2.setOriginalStartTime(now - (timeoutMins * 60000) - 1);
- 
2.19.1.windows.1

