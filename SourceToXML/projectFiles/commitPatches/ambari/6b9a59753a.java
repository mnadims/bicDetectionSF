From 6b9a59753a924c6e07a68a0de61f0fb2792df102 Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Sat, 8 Apr 2017 15:17:14 -0400
Subject: [PATCH] AMBARI-20712 - Parallel Requests With Intersecting Hosts
 Don't Block Correctly (jonathanhurley)

--
 .../server/actionmanager/ActionScheduler.java | 154 ++++++++----------
 .../server/actionmanager/HostRoleCommand.java |  26 +++
 .../AmbariCustomCommandExecutionHelper.java   |   3 +-
 .../server/orm/dao/HostRoleCommandDAO.java    |  49 ++++++
 .../orm/entities/HostRoleCommandEntity.java   |  33 +++-
 .../resources/Ambari-DDL-Derby-CREATE.sql     |   1 +
 .../resources/Ambari-DDL-MySQL-CREATE.sql     |   1 +
 .../resources/Ambari-DDL-Oracle-CREATE.sql    |   1 +
 .../resources/Ambari-DDL-Postgres-CREATE.sql  |   1 +
 .../Ambari-DDL-SQLAnywhere-CREATE.sql         |   3 +-
 .../resources/Ambari-DDL-SQLServer-CREATE.sql |   1 +
 .../actionmanager/TestActionScheduler.java    |  41 +++--
 12 files changed, 204 insertions(+), 110 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
index 758db3516f..316f2bdbff 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
@@ -42,7 +42,6 @@ import org.apache.ambari.server.ServiceComponentHostNotFoundException;
 import org.apache.ambari.server.ServiceComponentNotFoundException;
 import org.apache.ambari.server.agent.ActionQueue;
 import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
 import org.apache.ambari.server.agent.CancelCommand;
 import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.agent.ExecutionCommand;
@@ -355,15 +354,15 @@ class ActionScheduler implements Runnable {
       }
 
       Set<Long> runningRequestIds = new HashSet<>();
      List<Stage> stages = db.getFirstStageInProgressPerRequest();
      List<Stage> firstStageInProgressPerRequest = db.getFirstStageInProgressPerRequest();
       if (LOG.isDebugEnabled()) {
         LOG.debug("Scheduler wakes up");
        LOG.debug("Processing {} in progress stages ", stages.size());
        LOG.debug("Processing {} in progress stages", firstStageInProgressPerRequest.size());
       }
 
      publishInProgressTasks(stages);
      publishInProgressTasks(firstStageInProgressPerRequest);
 
      if (stages.isEmpty()) {
      if (firstStageInProgressPerRequest.isEmpty()) {
         // Nothing to do
         if (LOG.isDebugEnabled()) {
           LOG.debug("There are no stages currently in progress.");
@@ -375,11 +374,19 @@ class ActionScheduler implements Runnable {
 
       int i_stage = 0;
 
      HashSet<String> hostsWithTasks = getListOfHostsWithPendingTask(stages);
      actionQueue.updateListOfHostsWithPendingTask(hostsWithTasks);
      // get the range of requests in progress
      long iLowestRequestIdInProgress = firstStageInProgressPerRequest.get(0).getRequestId();
      long iHighestRequestIdInProgress = firstStageInProgressPerRequest.get(
          firstStageInProgressPerRequest.size() - 1).getRequestId();
 
      stages = filterParallelPerHostStages(stages);
      // At this point the stages is a filtered list
      List<String> hostsWithPendingTasks = hostRoleCommandDAO.getHostsWithPendingTasks(
          iLowestRequestIdInProgress, iHighestRequestIdInProgress);

      actionQueue.updateListOfHostsWithPendingTask(new HashSet<>(hostsWithPendingTasks));

      // filter the stages in progress down to those which can be scheduled in
      // parallel
      List<Stage> stages = filterParallelPerHostStages(firstStageInProgressPerRequest);
 
       boolean exclusiveRequestIsGoing = false;
       // This loop greatly depends on the fact that order of stages in
@@ -565,123 +572,92 @@ class ActionScheduler implements Runnable {
   }
 
   /**
   * Returns the list of hosts that have a task assigned
   *
   * @param stages
   * @return
   */
  private HashSet<String> getListOfHostsWithPendingTask(List<Stage> stages) {
    HashSet<String> hostsWithTasks = new HashSet<>();
    for (Stage s : stages) {
      hostsWithTasks.addAll(s.getHosts());
    }
    return hostsWithTasks;
  }

  /**
   * Returns filtered list of stages such that the returned list is an ordered list of stages that may
   * be executed in parallel or in the order in which they are presented
   * Returns filtered list of stages such that the returned list is an ordered
   * list of stages that may be executed in parallel or in the order in which
   * they are presented.
    * <p/>
   * Assumption: the list of stages supplied as input are ordered by request id and then stage id.
   * The specified stages must be ordered by request ID and may only contain the
   * next stage in progress per request (as returned by
   * {@link ActionDBAccessor#getFirstStageInProgressPerRequest()}. This is
   * because there is a requirement that within a request, no two stages may
   * ever run in parallel.
    * <p/>
   * Rules:
   * The following rules will be applied to the list:
    * <ul>
   * <li>
   * Stages are filtered such that the first stage in the list (assumed to be the first pending
   * stage from the earliest active request) has priority
   * </li>
   * <li>
   * No stage in any request may be executed before an earlier stage in the same request
   * </li>
   * <li>
   * A stages in different requests may be performed in parallel if the relevant hosts for the
   * stage in the later requests do not intersect with the union of hosts from (pending) stages
   * in earlier requests
   * <li>Stages are filtered such that the first stage in the list (assumed to
   * be the first pending stage from the earliest active request) has priority.
    * </li>
   * <li>No stage in any request may be executed before an earlier stage in the
   * same request. This requirement is automatically covered by virtue of the
   * supplied stages only being for the next stage in progress per request.</li>
   * <li>A stage in different request may be performed in parallel
   * if-and-only-if the relevant hosts for the stage in the later requests do
   * not intersect with the union of hosts from (pending) stages in earlier
   * requests. In order to accomplish this</li>
    * </ul>
    *
   * @param stages the stages to process
   * @param firstStageInProgressPerRequest
   *          the stages to process, one stage per request
    * @return a list of stages that may be executed in parallel
    */
  private List<Stage> filterParallelPerHostStages(List<Stage> stages) {
  private List<Stage> filterParallelPerHostStages(List<Stage> firstStageInProgressPerRequest) {
    // if there's only 1 stage in progress in 1 request, simply return that stage
    if (firstStageInProgressPerRequest.size() == 1) {
      return firstStageInProgressPerRequest;
    }

     List<Stage> retVal = new ArrayList<>();
    Set<String> affectedHosts = new HashSet<>();
    Set<Long> affectedRequests = new HashSet<>();
 
    for (Stage s : stages) {
      long requestId = s.getRequestId();
    // set the lower range (inclusive) of requests to limit the query a bit
    // since there can be a LOT of commands
    long lowerRequestIdInclusive = firstStageInProgressPerRequest.get(0).getRequestId();

    // determine if this stage can be scheduled in parallel with the other
    // stages from other requests
    for (Stage stage : firstStageInProgressPerRequest) {
      long requestId = stage.getRequestId();
 
       if (LOG.isTraceEnabled()) {
        LOG.trace("==> Processing stage: {}/{} ({}) for {}", requestId, s.getStageId(), s.getRequestContext());
        LOG.trace("==> Processing stage: {}/{} ({}) for {}", requestId, stage.getStageId(), stage.getRequestContext());
       }
 
       boolean addStage = true;
 
      // there are at least 2 request in progress concurrently; determine which
      // hosts are affected
      HashSet<String> hostsInProgressForEarlierRequests = new HashSet<>(
          hostRoleCommandDAO.getBlockingHostsForRequest(lowerRequestIdInclusive, requestId));

       // Iterate over the relevant hosts for this stage to see if any intersect with the set of
       // hosts needed for previous stages.  If any intersection occurs, this stage may not be
       // executed in parallel.
      for (String host : s.getHosts()) {
      for (String host : stage.getHosts()) {
         LOG.trace("===> Processing Host {}", host);
 
        if (affectedHosts.contains(host)) {
        if (hostsInProgressForEarlierRequests.contains(host)) {
           if (LOG.isTraceEnabled()) {
            LOG.trace("===>  Skipping stage since it utilizes at least one host that a previous stage requires: {}/{} ({})", s.getRequestId(), s.getStageId(), s.getRequestContext());
            LOG.trace("===>  Skipping stage since it utilizes at least one host that a previous stage requires: {}/{} ({})", stage.getRequestId(), stage.getStageId(), stage.getRequestContext());
           }
 
          addStage &= false;
        } else {
          if (!Stage.INTERNAL_HOSTNAME.equalsIgnoreCase(host) && !isStageHasBackgroundCommandsOnly(s, host)) {
            LOG.trace("====>  Adding host to affected hosts: {}", host);
            affectedHosts.add(host);
          }

          addStage &= true;
        }
      }

      // If this stage is for a request that we have already processed, the it cannot execute in
      // parallel since only one stage per request my execute at a time. The first time we encounter
      // a request id, will be for the first pending stage for that request, so it is a candidate
      // for execution at this time - if the previous test for host intersection succeeds.
      if (affectedRequests.contains(requestId)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("===>  Skipping stage since the request it is in has been processed already: {}/{} ({})", s.getRequestId(), s.getStageId(), s.getRequestContext());
        }

        addStage = false;
      } else {
        if (LOG.isTraceEnabled()) {
          LOG.trace("====>  Adding request to affected requests: {}", requestId);
          addStage = false;
          break;
         }

        affectedRequests.add(requestId);
        addStage &= true;
       }
 
      // If both tests pass - the stage is the first pending stage in its request and the hosts
      // required in the stage do not intersect with hosts from stages that should occur before this,
      // than add it to the list of stages that may be executed in parallel.
      // add the stage is no other prior stages for prior requests intersect the
      // hosts in this stage
       if (addStage) {
         if (LOG.isTraceEnabled()) {
          LOG.trace("===>  Adding stage to return value: {}/{} ({})", s.getRequestId(), s.getStageId(), s.getRequestContext());
          LOG.trace("===>  Adding stage to return value: {}/{} ({})", stage.getRequestId(), stage.getStageId(), stage.getRequestContext());
         }
 
        retVal.add(s);
        retVal.add(stage);
       }
     }
 
     return retVal;
   }
 
  private boolean isStageHasBackgroundCommandsOnly(Stage s, String host) {
    for (ExecutionCommandWrapper c : s.getExecutionCommands(host)) {
      if (c.getCommandType() != AgentCommandType.BACKGROUND_EXECUTION_COMMAND) {
        return false;
      }
    }
    return true;
  }

   private boolean hasPreviousStageFailed(Stage stage) {
     boolean failed = false;
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java
index 651eb2413a..87a6edf4d6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java
@@ -68,6 +68,7 @@ public class HostRoleCommand {
   private String commandDetail;
   private String customCommandName;
   private ExecutionCommandWrapper executionCommandWrapper;
  private boolean isBackgroundCommand = false;
 
   @Inject
   private ExecutionCommandDAO executionCommandDAO;
@@ -179,6 +180,7 @@ public class HostRoleCommand {
     event = new ServiceComponentHostEventWrapper(hostRoleCommandEntity.getEvent());
     commandDetail = hostRoleCommandEntity.getCommandDetail();
     customCommandName = hostRoleCommandEntity.getCustomCommandName();
    isBackgroundCommand = hostRoleCommandEntity.isBackgroundCommand();
   }
 
   //todo: why is this not symmetrical with the constructor which takes an entity
@@ -201,6 +203,7 @@ public class HostRoleCommand {
     hostRoleCommandEntity.setRoleCommand(roleCommand);
     hostRoleCommandEntity.setCommandDetail(commandDetail);
     hostRoleCommandEntity.setCustomCommandName(customCommandName);
    hostRoleCommandEntity.setBackgroundCommand(isBackgroundCommand);
 
     HostEntity hostEntity = hostDAO.findById(hostId);
     if (null != hostEntity) {
@@ -432,6 +435,29 @@ public class HostRoleCommand {
     return requestId;
   }
 
  /**
   * Gets whether this command runs in the background and does not block other
   * commands.
   *
   * @return {@code true} if this command runs in the background, {@code false}
   *         otherise.
   */
  public boolean isBackgroundCommand() {
    return isBackgroundCommand;
  }

  /**
   * Sets whether this command runs in the background and does not block other
   * commands.
   *
   * @param isBackgroundCommand
   *          {@code true} if this command runs in the background, {@code false}
   *          otherise.
   */
  public void setBackgroundCommand(boolean isBackgroundCommand) {
    this.isBackgroundCommand = isBackgroundCommand;
  }

   /**
    * Gets whether commands which fail and are retryable are automatically
    * skipped and marked with {@link HostRoleStatus#SKIPPED_FAILED}.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariCustomCommandExecutionHelper.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariCustomCommandExecutionHelper.java
index 77d5bf89db..d5018f55b2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariCustomCommandExecutionHelper.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariCustomCommandExecutionHelper.java
@@ -144,7 +144,7 @@ public class AmbariCustomCommandExecutionHelper {
   private final static String ALIGN_MAINTENANCE_STATE = "align_maintenance_state";
 
   public final static int MIN_STRICT_SERVICE_CHECK_TIMEOUT = 120;
  

   @Inject
   private ActionMetadata actionMetadata;
 
@@ -378,6 +378,7 @@ public class AmbariCustomCommandExecutionHelper {
 
       //set type background
       if(customCommandDefinition != null && customCommandDefinition.isBackground()){
        cmd.setBackgroundCommand(true);
         execCmd.setCommandType(AgentCommandType.BACKGROUND_EXECUTION_COMMAND);
       }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
index 79b8bc9f31..73181623c1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
@@ -43,6 +43,7 @@ import org.apache.ambari.server.RoleCommand;
 import org.apache.ambari.server.actionmanager.HostRoleCommand;
 import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
 import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
 import org.apache.ambari.server.api.query.JpaPredicateVisitor;
 import org.apache.ambari.server.api.query.JpaSortBuilder;
 import org.apache.ambari.server.configuration.Configuration;
@@ -913,6 +914,54 @@ public class HostRoleCommandDAO {
     return daoUtils.selectList(typedQuery);
   }
 
  /**
   * Gets a lists of hosts with commands in progress given a range of requests.
   * The range of requests should include all requests with at least 1 stage in
   * progress.
   *
   * @return the list of hosts with commands in progress.
   * @see HostRoleStatus#IN_PROGRESS_STATUSES
   */
  @RequiresSession
  public List<String> getHostsWithPendingTasks(long iLowestRequestIdInProgress,
      long iHighestRequestIdInProgress) {
    TypedQuery<String> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findHostsByCommandStatus", String.class);

    query.setParameter("iLowestRequestIdInProgress", iLowestRequestIdInProgress);
    query.setParameter("iHighestRequestIdInProgress", iHighestRequestIdInProgress);
    query.setParameter("statuses", HostRoleStatus.IN_PROGRESS_STATUSES);
    return daoUtils.selectList(query);
  }

  /**
   * Gets a lists of hosts with commands in progress which occurr before the
   * specified request ID. This will only return commands which are not
   * {@link AgentCommandType#BACKGROUND_EXECUTION_COMMAND} as thsee commands do
   * not block future requests.
   *
   * @param lowerRequestIdInclusive
   *          the lowest request ID to consider (inclusive) when getting any
   *          blocking hosts.
   * @param requestId
   *          the request ID to calculate any blocking hosts for (essentially,
   *          the upper limit exclusive)
   * @return the list of hosts from older running requests which will block
   *         those same hosts in the specified request ID.
   * @see HostRoleStatus#IN_PROGRESS_STATUSES
   */
  @RequiresSession
  public List<String> getBlockingHostsForRequest(long lowerRequestIdInclusive,
      long requestId) {
    TypedQuery<String> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.getBlockingHostsForRequest", String.class);

    query.setParameter("lowerRequestIdInclusive", lowerRequestIdInclusive);
    query.setParameter("upperRequestIdExclusive", requestId);
    query.setParameter("statuses", HostRoleStatus.IN_PROGRESS_STATUSES);
    return daoUtils.selectList(query);
  }

   /**
    * The {@link HostRoleCommandPredicateVisitor} is used to convert an Ambari
    * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java
index a809295a14..fdec5f040a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostRoleCommandEntity.java
@@ -70,7 +70,14 @@ import org.apache.commons.lang.ArrayUtils;
     @NamedQuery(name = "HostRoleCommandEntity.findByHostRoleNullHost", query = "SELECT command FROM HostRoleCommandEntity command WHERE command.hostEntity IS NULL AND command.requestId=:requestId AND command.stageId=:stageId AND command.role=:role"),
     @NamedQuery(name = "HostRoleCommandEntity.findByStatusBetweenStages", query = "SELECT command FROM HostRoleCommandEntity command WHERE command.requestId = :requestId AND command.stageId >= :minStageId AND command.stageId <= :maxStageId AND command.status = :status"),
     @NamedQuery(name = "HostRoleCommandEntity.updateAutoSkipExcludeRoleCommand", query = "UPDATE HostRoleCommandEntity command SET command.autoSkipOnFailure = :autoSkipOnFailure WHERE command.requestId = :requestId AND command.roleCommand <> :roleCommand"),
    @NamedQuery(name = "HostRoleCommandEntity.updateAutoSkipForRoleCommand", query = "UPDATE HostRoleCommandEntity command SET command.autoSkipOnFailure = :autoSkipOnFailure WHERE command.requestId = :requestId AND command.roleCommand = :roleCommand")
    @NamedQuery(name = "HostRoleCommandEntity.updateAutoSkipForRoleCommand", query = "UPDATE HostRoleCommandEntity command SET command.autoSkipOnFailure = :autoSkipOnFailure WHERE command.requestId = :requestId AND command.roleCommand = :roleCommand"),
    @NamedQuery(
        name = "HostRoleCommandEntity.findHostsByCommandStatus",
        query = "SELECT DISTINCT(host.hostName) FROM HostRoleCommandEntity command, HostEntity host WHERE (command.requestId >= :iLowestRequestIdInProgress AND command.requestId <= :iHighestRequestIdInProgress) AND command.status IN :statuses AND command.hostId = host.hostId AND host.hostName IS NOT NULL"),
    @NamedQuery(
        name = "HostRoleCommandEntity.getBlockingHostsForRequest",
        query = "SELECT DISTINCT(host.hostName) FROM HostRoleCommandEntity command, HostEntity host WHERE command.requestId >= :lowerRequestIdInclusive AND command.requestId < :upperRequestIdExclusive AND command.status IN :statuses AND command.isBackgroundCommand=0 AND command.hostId = host.hostId AND host.hostName IS NOT NULL")

 })
 public class HostRoleCommandEntity {
 
@@ -195,6 +202,10 @@ public class HostRoleCommandEntity {
   @OneToOne(mappedBy = "hostRoleCommandEntity", cascade = CascadeType.REMOVE)
   private TopologyLogicalTaskEntity topologyLogicalTaskEntity;
 
  @Basic
  @Column(name = "is_background_command", nullable = false)
  private short isBackgroundCommand = 0;

   public Long getTaskId() {
     return taskId;
   }
@@ -407,6 +418,26 @@ public class HostRoleCommandEntity {
     autoSkipOnFailure = skipFailures ? 1 : 0;
   }
 
  /**
   * Sets whether this is a command is a background command and will not block
   * other commands.
   *
   * @param runInBackground
   *          {@code true} if this is a background command, {@code false}
   *          otherwise.
   */
  public void setBackgroundCommand(boolean runInBackground) {
    isBackgroundCommand = (short) (runInBackground ? 1 : 0);
  }

  /**
   * Gets whether this command runs in the background and will not block other
   * commands.
   */
  public boolean isBackgroundCommand() {
    return isBackgroundCommand == 0 ? false : true;
  }

   @Override
   public boolean equals(Object o) {
     if (this == o) {
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
index 6744a74b54..5785a9dd48 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql
@@ -383,6 +383,7 @@ CREATE TABLE host_role_command (
   role_command VARCHAR(255),
   command_detail VARCHAR(255),
   custom_command_name VARCHAR(255),
  is_background_command SMALLINT DEFAULT 0 NOT NULL,
   CONSTRAINT PK_host_role_command PRIMARY KEY (task_id),
   CONSTRAINT FK_host_role_command_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
   CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
index 6279f83c58..96ef0ac206 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-MySQL-CREATE.sql
@@ -404,6 +404,7 @@ CREATE TABLE host_role_command (
   structured_out LONGBLOB,
   command_detail VARCHAR(255),
   custom_command_name VARCHAR(255),
  is_background_command SMALLINT DEFAULT 0 NOT NULL,
   CONSTRAINT PK_host_role_command PRIMARY KEY (task_id),
   CONSTRAINT FK_host_role_command_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
   CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
index 470eb6082c..3396ce9a8b 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Oracle-CREATE.sql
@@ -384,6 +384,7 @@ CREATE TABLE host_role_command (
   structured_out BLOB NULL,
   command_detail VARCHAR2(255) NULL,
   custom_command_name VARCHAR2(255) NULL,
  is_background_command SMALLINT DEFAULT 0 NOT NULL,
   CONSTRAINT PK_host_role_command PRIMARY KEY (task_id),
   CONSTRAINT FK_host_role_command_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
   CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
index 87ffb7feb0..c6bfa94a52 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-Postgres-CREATE.sql
@@ -383,6 +383,7 @@ CREATE TABLE host_role_command (
   role_command VARCHAR(255),
   command_detail VARCHAR(255),
   custom_command_name VARCHAR(255),
  is_background_command SMALLINT DEFAULT 0 NOT NULL,
   CONSTRAINT PK_host_role_command PRIMARY KEY (task_id),
   CONSTRAINT FK_host_role_command_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
   CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
index a460194c1e..bbf5d3cc63 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLAnywhere-CREATE.sql
@@ -75,7 +75,7 @@ CREATE TABLE clusterconfig (
   config_data TEXT NOT NULL,
   config_attributes TEXT,
   create_timestamp NUMERIC(19) NOT NULL,
  selected_timestamp NUMERIC(19) NOT NULL DEFAULT 0,  
  selected_timestamp NUMERIC(19) NOT NULL DEFAULT 0,
   CONSTRAINT PK_clusterconfig PRIMARY KEY (config_id),
   CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id),
   CONSTRAINT FK_clusterconfig_stack_id FOREIGN KEY (stack_id) REFERENCES stack(stack_id),
@@ -382,6 +382,7 @@ CREATE TABLE host_role_command (
   structured_out IMAGE,
   command_detail VARCHAR(255),
   custom_command_name VARCHAR(255),
  is_background_command SMALLINT DEFAULT 0 NOT NULL,
   CONSTRAINT PK_host_role_command PRIMARY KEY (task_id),
   CONSTRAINT FK_host_role_command_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
   CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));
diff --git a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
index 237c892332..13ab01dd33 100644
-- a/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
++ b/ambari-server/src/main/resources/Ambari-DDL-SQLServer-CREATE.sql
@@ -388,6 +388,7 @@ CREATE TABLE host_role_command (
   role_command VARCHAR(255),
   command_detail VARCHAR(255),
   custom_command_name VARCHAR(255),
  is_background_command SMALLINT DEFAULT 0 NOT NULL,
   CONSTRAINT PK_host_role_command PRIMARY KEY CLUSTERED (task_id),
   CONSTRAINT FK_host_role_command_host_id FOREIGN KEY (host_id) REFERENCES hosts (host_id),
   CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id));
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
index d7d3d404d0..b1a75249c6 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
@@ -1106,41 +1106,45 @@ public class TestActionScheduler {
     hosts.put(hostname4, sch);
     when(scomp.getServiceComponentHosts()).thenReturn(hosts);
 
    List<Stage> stages = new ArrayList<>();
    stages.add(
    List<Stage> firstStageInProgressPerRequest = new ArrayList<>();

    firstStageInProgressPerRequest.add(
             getStageWithSingleTask(
                     hostname1, "cluster1", Role.DATANODE,
                     RoleCommand.START, Service.Type.HDFS, 1, 1, 1));
    stages.add( // Stage with the same hostname, should not be scheduled

    // Stage with the same hostname, should not be scheduled
    firstStageInProgressPerRequest.add(
             getStageWithSingleTask(
                     hostname1, "cluster1", Role.GANGLIA_MONITOR,
                     RoleCommand.START, Service.Type.GANGLIA, 2, 2, 2));
 
    stages.add(
    firstStageInProgressPerRequest.add(
             getStageWithSingleTask(
                     hostname2, "cluster1", Role.DATANODE,
                     RoleCommand.START, Service.Type.HDFS, 3, 3, 3));
 
    stages.add(
    firstStageInProgressPerRequest.add(
         getStageWithSingleTask(
             hostname3, "cluster1", Role.DATANODE,
             RoleCommand.START, Service.Type.HDFS, 4, 4, 4));
 
    stages.add( // Stage with the same request id, should not be scheduled
        getStageWithSingleTask(
            hostname4, "cluster1", Role.GANGLIA_MONITOR,
            RoleCommand.START, Service.Type.GANGLIA, 5, 5, 4));

     ActionDBAccessor db = mock(ActionDBAccessor.class);
     HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
     Mockito.doNothing().when(hostRoleCommandDAOMock).publishTaskCreateEvent(anyListOf(HostRoleCommand.class));
 
    List<String> blockingHostsRequest1 = new ArrayList<>();
    when(hostRoleCommandDAOMock.getBlockingHostsForRequest(1, 1)).thenReturn(blockingHostsRequest1);

    List<String> blockingHostsRequest2 = Lists.newArrayList(hostname1);
    when(hostRoleCommandDAOMock.getBlockingHostsForRequest(1, 2)).thenReturn(blockingHostsRequest2);

     RequestEntity request = mock(RequestEntity.class);
     when(request.isExclusive()).thenReturn(false);
     when(db.getRequestEntity(anyLong())).thenReturn(request);
 
    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
    when(db.getCommandsInProgressCount()).thenReturn(firstStageInProgressPerRequest.size());
    when(db.getFirstStageInProgressPerRequest()).thenReturn(firstStageInProgressPerRequest);
 
     Properties properties = new Properties();
     Configuration conf = new Configuration(properties);
@@ -1152,11 +1156,10 @@ public class TestActionScheduler {
 
     scheduler.doWork();
 
    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(0).getHostRoleStatus(hostname1, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(1).getHostRoleStatus(hostname1, "GANGLIA_MONITOR"));
    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(2).getHostRoleStatus(hostname2, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(3).getHostRoleStatus(hostname3, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(4).getHostRoleStatus(hostname4, "GANGLIA_MONITOR"));
    Assert.assertEquals(HostRoleStatus.QUEUED, firstStageInProgressPerRequest.get(0).getHostRoleStatus(hostname1, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, firstStageInProgressPerRequest.get(1).getHostRoleStatus(hostname1, "GANGLIA_MONITOR"));
    Assert.assertEquals(HostRoleStatus.QUEUED, firstStageInProgressPerRequest.get(2).getHostRoleStatus(hostname2, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.QUEUED, firstStageInProgressPerRequest.get(3).getHostRoleStatus(hostname3, "DATANODE"));
   }
 
 
@@ -2065,6 +2068,8 @@ public class TestActionScheduler {
     hosts.put(hostname1, sch1);
     when(scomp.getServiceComponentHosts()).thenReturn(hosts);
 
    HostRoleCommandDAO hostRoleCommandDAO = mock(HostRoleCommandDAO.class);

     HostEntity hostEntity = new HostEntity();
     hostEntity.setHostName(hostname1);
     hostDAO.create(hostEntity);
@@ -2100,7 +2105,7 @@ public class TestActionScheduler {
 
     ActionScheduler scheduler = new ActionScheduler(100, 50000, db, aq, fsm, 3,
         new HostsMap((String) null), unitOfWork, null, conf, entityManagerProviderMock,
        (HostRoleCommandDAO)null, (HostRoleCommandFactory)null);
        hostRoleCommandDAO, (HostRoleCommandFactory) null);
 
     final CountDownLatch abortCalls = new CountDownLatch(2);
 
- 
2.19.1.windows.1

