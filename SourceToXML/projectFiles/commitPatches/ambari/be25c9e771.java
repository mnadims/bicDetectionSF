From be25c9e77197be8e056c1bb9aa3651c16dd6fd62 Mon Sep 17 00:00:00 2001
From: Myroslav Papirkovskyi <mpapyrkovskyy@hortonworks.com>
Date: Thu, 28 Apr 2016 18:52:40 +0300
Subject: [PATCH] AMBARI-15671. On Ambari Agent restart currently running
 commands on that agent should be immediately aborted. (mpapirkovskyy)

--
 .../actionmanager/ActionDBAccessor.java       |   6 +
 .../actionmanager/ActionDBAccessorImpl.java   |  15 +-
 .../server/actionmanager/ActionScheduler.java |  31 +++-
 .../actionmanager/TestActionScheduler.java    | 151 +++++++++++++++---
 4 files changed, 177 insertions(+), 26 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
index 9aba4c95a6..dcfe359dba 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessor.java
@@ -64,6 +64,12 @@ public interface ActionDBAccessor {
    */
   public void timeoutHostRole(String host, long requestId, long stageId, String role);
 
  /**
   * Mark the task as to have timed out
   */
  void timeoutHostRole(String host, long requestId, long stageId,
                       String role, boolean skipSupported);

   /**
    * Returns all the pending stages, including queued and not-queued. A stage is
    * considered in progress if it is in progress for any host.
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
index 06311c2d44..8e6fb3faad 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionDBAccessorImpl.java
@@ -236,11 +236,22 @@ public class ActionDBAccessorImpl implements ActionDBAccessor {
   @Override
   public void timeoutHostRole(String host, long requestId, long stageId,
                               String role) {
    timeoutHostRole(host, requestId, stageId, role, false);
  }

  @Override
  public void timeoutHostRole(String host, long requestId, long stageId,
                              String role, boolean skipSupported) {
     long now = System.currentTimeMillis();
     List<HostRoleCommandEntity> commands =
        hostRoleCommandDAO.findByHostRole(host, requestId, stageId, role);
            hostRoleCommandDAO.findByHostRole(host, requestId, stageId, role);
     for (HostRoleCommandEntity command : commands) {
      command.setStatus(command.isRetryAllowed() ? HostRoleStatus.HOLDING_TIMEDOUT : HostRoleStatus.TIMEDOUT);
      if (skipSupported) {
        command.setStatus(HostRoleStatus.SKIPPED_FAILED);
      } else {
        command.setStatus(command.isRetryAllowed() ? HostRoleStatus.HOLDING_TIMEDOUT : HostRoleStatus.TIMEDOUT);
      }

       command.setEndTime(now);
 
       auditLog(command, requestId);
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
index 95d17630df..33c0a1f781 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
@@ -590,7 +590,7 @@ class ActionScheduler implements Runnable {
    * @return the stats for the roles in the stage which are used to determine
    * whether stage has succeeded or failed
    */
  private Map<String, RoleStats> processInProgressStage(Stage s,
  protected Map<String, RoleStats> processInProgressStage(Stage s,
       List<ExecutionCommand> commandsToSchedule) throws AmbariException {
     LOG.debug("==> Collecting commands to schedule...");
     // Map to track role status
@@ -694,12 +694,17 @@ class ActionScheduler implements Runnable {
           LOG.info("Host:" + host + ", role:" + roleStr + ", actionId:" + s.getActionId() + " timed out");
           if (s.getAttemptCount(host, roleStr) >= maxAttempts) {
             LOG.warn("Host:" + host + ", role:" + roleStr + ", actionId:" + s.getActionId() + " expired");
            db.timeoutHostRole(host, s.getRequestId(), s.getStageId(), c.getRole());
            db.timeoutHostRole(host, s.getRequestId(), s.getStageId(), c.getRole(), s.isAutoSkipOnFailureSupported());
             //Reinitialize status
             status = s.getHostRoleStatus(host, roleStr);
 
             if (null != cluster) {
              transitionToFailedState(cluster.getClusterName(), c.getServiceName(), roleStr, host, now, false);
              if (!RoleCommand.CUSTOM_COMMAND.equals(c.getRoleCommand())
                && !RoleCommand.SERVICE_CHECK.equals(c.getRoleCommand())
                && !RoleCommand.ACTIONEXECUTE.equals(c.getRoleCommand())) {
                //commands above don't affect host component state (e.g. no in_progress state in process), transition will fail
                transitionToFailedState(cluster.getClusterName(), c.getServiceName(), roleStr, host, now, false);
              }
               if (c.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
                 processActionDeath(cluster.getClusterName(), c.getHostname(), roleStr);
               }
@@ -832,6 +837,19 @@ class ActionScheduler implements Runnable {
     return roleStats;
   }
 
  /**
   * Checks if ambari-agent was restarted during role command execution
   * @param host the host with ambari-agent to check
   * @param stage the stage
   * @param role the role to check
   * @return {@code true} if ambari-agent was restarted
   */
  protected boolean wasAgentRestartedDuringOperation(Host host, Stage stage, String role) {
    String hostName = host.getHostName();
    long taskStartTime = stage.getHostRoleCommand(hostName, role).getStartTime();
    return taskStartTime > 0 && taskStartTime <= host.getLastRegistrationTime();
  }

   /**
    * Checks if timeout is required.
    * @param status      the status of the current role
@@ -843,7 +861,7 @@ class ActionScheduler implements Runnable {
    * @return {@code true} if timeout is needed
    * @throws AmbariException
    */
  private boolean timeOutActionNeeded(HostRoleStatus status, Stage stage,
  protected boolean timeOutActionNeeded(HostRoleStatus status, Stage stage,
       Host host, String role, long currentTime, long taskTimeout) throws
     AmbariException {
     if (( !status.equals(HostRoleStatus.QUEUED) ) &&
@@ -852,8 +870,9 @@ class ActionScheduler implements Runnable {
     }
 
     // Fast fail task if host state is unknown
    if (null != host && host.getState().equals(HostState.HEARTBEAT_LOST)) {
      LOG.debug("Timing out action since agent is not heartbeating.");
    if (null != host &&
      (host.getState().equals(HostState.HEARTBEAT_LOST) || wasAgentRestartedDuringOperation(host, stage, role))) {
      LOG.debug("Timing out action since agent is not heartbeating or agent was restarted.");
       return true;
     }
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
index af6fb9b7ff..7a8890d964 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
@@ -20,14 +20,12 @@ package org.apache.ambari.server.actionmanager;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.*;
 import static org.mockito.Mockito.atLeastOnce;
 import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
 import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
 import static org.mockito.Mockito.times;
 import static org.mockito.Mockito.verify;
 import static org.mockito.Mockito.when;
@@ -59,6 +57,7 @@ import org.apache.ambari.server.agent.CommandReport;
 import org.apache.ambari.server.agent.ExecutionCommand;
 import org.apache.ambari.server.configuration.Configuration;
 import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.events.AmbariEvent;
 import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
 import org.apache.ambari.server.orm.GuiceJpaInitializer;
 import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
@@ -75,6 +74,7 @@ import org.apache.ambari.server.state.Service;
 import org.apache.ambari.server.state.ServiceComponent;
 import org.apache.ambari.server.state.ServiceComponentHost;
 import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
 import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
 import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
 import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
@@ -299,7 +299,7 @@ public class TestActionScheduler {
         command.setStatus(HostRoleStatus.TIMEDOUT);
         return null;
       }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString());
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString(), anyBoolean());
 
 
     //Small action timeout to test rescheduling
@@ -379,7 +379,7 @@ public class TestActionScheduler {
         command.setStatus(HostRoleStatus.TIMEDOUT);
         return null;
       }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString());
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString(), anyBoolean());
 
     //Small action timeout to test rescheduling
     AmbariEventPublisher aep = EasyMock.createNiceMock(AmbariEventPublisher.class);
@@ -479,7 +479,7 @@ public class TestActionScheduler {
         }
         return null;
       }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString());
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString(), anyBoolean());
 
     doAnswer(new Answer<Void>() {
       @Override
@@ -791,6 +791,109 @@ public class TestActionScheduler {
         stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION"));
   }
 
  @Test
  public void testTimeOutWithHostNull() throws AmbariException {
    Stage s = getStageWithServerAction(1, 977, null, "test", 2);
    s.setHostRoleStatus(null, Role.AMBARI_SERVER_ACTION.toString(), HostRoleStatus.IN_PROGRESS);

    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class)
      .withConstructor(long.class, long.class, ActionDBAccessor.class, ActionQueue.class, Clusters.class, int.class,
        HostsMap.class, UnitOfWork.class, AmbariEventPublisher.class, Configuration.class)
      .withArgs(100L, 50L, null, null, null, -1, null, null, null, null)
      .createNiceMock();

    EasyMock.replay(scheduler);

    // currentTime should be set to -1 and taskTimeout to 1 because it is needed for timeOutActionNeeded method will return false value
    Assert.assertEquals(false, scheduler.timeOutActionNeeded(HostRoleStatus.IN_PROGRESS, s, null, Role.AMBARI_SERVER_ACTION.toString(), -1L, 1L));

    EasyMock.verify(scheduler);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartExecuteCommand() throws Exception {
    testTimeoutRequest(RoleCommand.EXECUTE);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartCustomCommand() throws Exception {
    testTimeoutRequest(RoleCommand.CUSTOM_COMMAND);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartActionExecute() throws Exception {
    testTimeoutRequest(RoleCommand.ACTIONEXECUTE);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartServiceCheck() throws Exception {
    testTimeoutRequest(RoleCommand.SERVICE_CHECK);
  }

  private void testTimeoutRequest(RoleCommand roleCommand) throws AmbariException, InvalidStateTransitionException {
    final long HOST_REGISTRATION_TIME = 100L;
    final long STAGE_TASK_START_TIME = HOST_REGISTRATION_TIME - 1L;

    ActionQueue aq = new ActionQueue();
    Clusters fsm = EasyMock.createMock(Clusters.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);
    Service service = EasyMock.createMock(Service.class);
    ServiceComponent serviceComponent = EasyMock.createMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = EasyMock.createMock(ServiceComponentHost.class);
    Host host = EasyMock.createMock(Host.class);
    ActionDBAccessor db = EasyMock.createMock(ActionDBAccessor.class);
    AmbariEventPublisher ambariEventPublisher = EasyMock.createMock(AmbariEventPublisher.class);

    EasyMock.expect(fsm.getCluster(EasyMock.anyString())).andReturn(cluster).anyTimes();
    EasyMock.expect(fsm.getHost(EasyMock.anyString())).andReturn(host);
    EasyMock.expect(cluster.getService(EasyMock.anyString())).andReturn(null);
    EasyMock.expect(host.getLastRegistrationTime()).andReturn(HOST_REGISTRATION_TIME);
    EasyMock.expect(host.getHostName()).andReturn(Stage.INTERNAL_HOSTNAME).anyTimes();
    EasyMock.expect(host.getState()).andReturn(HostState.HEALTHY);

    if (RoleCommand.ACTIONEXECUTE.equals(roleCommand)) {
      EasyMock.expect(cluster.getClusterName()).andReturn("clusterName").anyTimes();
      EasyMock.expect(cluster.getClusterId()).andReturn(1L);

      ambariEventPublisher.publish(EasyMock.anyObject(AmbariEvent.class));
      EasyMock.expectLastCall();
    } else if (RoleCommand.EXECUTE.equals(roleCommand)) {
      EasyMock.expect(cluster.getClusterName()).andReturn("clusterName");
      EasyMock.expect(cluster.getService(EasyMock.anyString())).andReturn(service);
      EasyMock.expect(service.getServiceComponent(EasyMock.anyString())).andReturn(serviceComponent);
      EasyMock.expect(serviceComponent.getServiceComponentHost(EasyMock.anyString())).andReturn(serviceComponentHost);

      serviceComponentHost.handleEvent(EasyMock.anyObject(ServiceComponentHostEvent.class));
      EasyMock.expectLastCall();
    }

    Stage s = getStageWithServerAction(1, 977, null, "test", 2);
    s.setStartTime(null, Role.AMBARI_SERVER_ACTION.toString(), STAGE_TASK_START_TIME);
    s.setHostRoleStatus(null, Role.AMBARI_SERVER_ACTION.toString(), HostRoleStatus.IN_PROGRESS);
    s.getExecutionCommands(null).get(0).getExecutionCommand().setServiceName("Service name");
    s.getExecutionCommands(null).get(0).getExecutionCommand().setRoleCommand(roleCommand);

    aq.enqueue(Stage.INTERNAL_HOSTNAME, s.getExecutionCommands(null).get(0).getExecutionCommand());
    List<ExecutionCommand> commandsToSchedule = new ArrayList<ExecutionCommand>();

    db.timeoutHostRole(EasyMock.anyString(), EasyMock.anyLong(), EasyMock.anyLong(), EasyMock.anyString(), EasyMock.anyBoolean());
    EasyMock.expectLastCall();

    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class)
      .withConstructor(long.class, long.class, ActionDBAccessor.class, ActionQueue.class, Clusters.class, int.class,
        HostsMap.class, UnitOfWork.class, AmbariEventPublisher.class, Configuration.class)
      .withArgs(100L, 50L, db, aq, fsm, -1, null, null, ambariEventPublisher, null)
      .createNiceMock();

    EasyMock.replay(scheduler, fsm, host, db, cluster, ambariEventPublisher, service, serviceComponent, serviceComponentHost);

    scheduler.processInProgressStage(s, commandsToSchedule);

    EasyMock.verify(scheduler, fsm, host, db, cluster, ambariEventPublisher, service, serviceComponent, serviceComponentHost);

    Assert.assertTrue("ActionQueue should be empty after request was timeout", aq.size(Stage.INTERNAL_HOSTNAME) == 0);
  }

   @Test
   public void testServerActionFailed() throws Exception {
     ActionQueue aq = new ActionQueue();
@@ -956,8 +1059,10 @@ public class TestActionScheduler {
 
     Properties properties = new Properties();
     Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null), unitOfWork, null, conf);
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null), unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());
 
     scheduler.doWork();
 
@@ -1044,9 +1149,12 @@ public class TestActionScheduler {
     Properties properties = new Properties();
     properties.put(Configuration.PARALLEL_STAGE_EXECUTION_KEY, "false");
     Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
             new HostsMap((String) null),
            unitOfWork, null, conf);
            unitOfWork, null, conf));


    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());
 
     scheduler.doWork();
 
@@ -1115,9 +1223,11 @@ public class TestActionScheduler {
     Properties properties = new Properties();
     properties.put(Configuration.PARALLEL_STAGE_EXECUTION_KEY, "true");
     Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
         new HostsMap((String) null),
        unitOfWork, null, conf);
        unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());
 
     scheduler.doWork();
 
@@ -1637,6 +1747,7 @@ public class TestActionScheduler {
     Stage stage = stageFactory.createNew(requestId, "/tmp", clusterName, 1L, "getStageWithSingleTask",
       CLUSTER_HOST_INFO, "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
     stage.setStageId(stageId);
    //stage.setAutoSkipFailureSupported(true);
     return stage;
   }
 
@@ -2266,8 +2377,10 @@ public class TestActionScheduler {
     Properties properties = new Properties();
     Configuration conf = new Configuration(properties);
 
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());
 
     // Execution of request 1
 
@@ -2464,8 +2577,10 @@ public class TestActionScheduler {
       }
     }).when(db).abortOperation(anyLong());
 
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());
 
     scheduler.doWork();
 
- 
2.19.1.windows.1

