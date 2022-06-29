From bf5ca2f5e6714c8e0536504de9d54c8d6afc1425 Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Tue, 28 Jun 2016 11:01:10 -0400
Subject: [PATCH] AMBARI-17464 - Failed task status during EU is wrongly
 reported as SKIPPED_FAILED instead of TIMED_OUT (jonathanhurley)

--
 .../server/actionmanager/ActionScheduler.java | 17 ++++-
 .../server/actionmanager/HostRoleCommand.java | 10 +++
 .../ambari/server/actionmanager/Stage.java    | 23 ++++++
 .../actionmanager/TestActionScheduler.java    | 72 ++++++++++++++-----
 4 files changed, 101 insertions(+), 21 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
index 8c27d3c48e..b3aab9fe17 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/ActionScheduler.java
@@ -691,10 +691,18 @@ class ActionScheduler implements Runnable {
           status = HostRoleStatus.ABORTED;
         } else if (timeOutActionNeeded(status, s, hostObj, roleStr, now, commandTimeout)) {
           // Process command timeouts
          LOG.info("Host:" + host + ", role:" + roleStr + ", actionId:" + s.getActionId() + " timed out");
           if (s.getAttemptCount(host, roleStr) >= maxAttempts) {
            LOG.warn("Host:" + host + ", role:" + roleStr + ", actionId:" + s.getActionId() + " expired");
            db.timeoutHostRole(host, s.getRequestId(), s.getStageId(), c.getRole(), s.isAutoSkipOnFailureSupported());
            LOG.warn("Host: {}, role: {}, actionId: {} expired and will be failed", host, roleStr,
                s.getActionId());

            // determine if the task should be auto skipped
            boolean isSkipSupported = s.isAutoSkipOnFailureSupported();
            HostRoleCommand hostRoleCommand = s.getHostRoleCommand(c.getTaskId());
            if (isSkipSupported && null != hostRoleCommand) {
              isSkipSupported = hostRoleCommand.isFailureAutoSkipped();
            }

            db.timeoutHostRole(host, s.getRequestId(), s.getStageId(), c.getRole(), isSkipSupported);
             //Reinitialize status
             status = s.getHostRoleStatus(host, roleStr);
 
@@ -714,6 +722,9 @@ class ActionScheduler implements Runnable {
             LOG.info("Removing command from queue, host={}, commandId={} ", host, c.getCommandId());
             actionQueue.dequeue(host, c.getCommandId());
           } else {
            LOG.info("Host: {}, role: {}, actionId: {} timed out and will be rescheduled", host,
                roleStr, s.getActionId());

             // reschedule command
             commandsToSchedule.add(c);
             LOG.trace("===> commandsToSchedule(reschedule)=" + commandsToSchedule.size());
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java
index 2b9c10b027..ff2ce92e59 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleCommand.java
@@ -416,6 +416,16 @@ public class HostRoleCommand {
     return requestId;
   }
 
  /**
   * Gets whether commands which fail and are retryable are automatically
   * skipped and marked with {@link HostRoleStatus#SKIPPED_FAILED}.
   *
   * @return
   */
  public boolean isFailureAutoSkipped() {
    return autoSkipFailure;
  }

   @Override
   public int hashCode() {
     return Long.valueOf(taskId).hashCode();
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/Stage.java b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/Stage.java
index 3fbeef9604..f03d8eac2c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/Stage.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/actionmanager/Stage.java
@@ -732,6 +732,29 @@ public class Stage {
     return hostRoleCommands;
   }
 
  /**
   * Gets the {@link HostRoleCommand} matching the specified ID from this stage.
   * This will not hit the database, instead using the pre-cached list of HRCs
   * from the construction of the stage.
   *
   * @param taskId
   *          the ID to match
   * @return the {@link HostRoleCommand} or {@code null} if none match.
   */
  public HostRoleCommand getHostRoleCommand(long taskId) {
    for (Map.Entry<String, Map<String, HostRoleCommand>> hostEntry : hostRoleCommands.entrySet()) {
      Map<String, HostRoleCommand> hostCommands = hostEntry.getValue();
      for (Map.Entry<String, HostRoleCommand> hostCommand : hostCommands.entrySet()) {
        HostRoleCommand hostRoleCommand = hostCommand.getValue();
        if (null != hostRoleCommand && hostRoleCommand.getTaskId() == taskId) {
          return hostRoleCommand;
        }
      }
    }

    return null;
  }

   /**
    * This method should be used only in stage planner. To add
    * a new execution command use
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
index d2c7de940e..d92d87aa0d 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/actionmanager/TestActionScheduler.java
@@ -24,7 +24,12 @@ import static org.easymock.EasyMock.replay;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
 import static org.mockito.Mockito.atLeastOnce;
 import static org.mockito.Mockito.doAnswer;
 import static org.mockito.Mockito.doReturn;
@@ -572,7 +577,7 @@ public class TestActionScheduler {
 
     List<Stage> stages = new ArrayList<Stage>();
     Map<String, String> payload = new HashMap<String, String>();
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 1200);
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 1200, false, false);
     stages.add(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
@@ -727,7 +732,7 @@ public class TestActionScheduler {
     List<Stage> stages = new ArrayList<Stage>();
     Map<String, String> payload = new HashMap<String, String>();
     payload.put(MockServerAction.PAYLOAD_FORCE_FAIL, "timeout");
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 2);
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 2, false, false);
     stages.add(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
@@ -798,7 +803,7 @@ public class TestActionScheduler {
 
   @Test
   public void testTimeOutWithHostNull() throws AmbariException {
    Stage s = getStageWithServerAction(1, 977, null, "test", 2);
    Stage s = getStageWithServerAction(1, 977, null, "test", 2, false, false);
     s.setHostRoleStatus(null, Role.AMBARI_SERVER_ACTION.toString(), HostRoleStatus.IN_PROGRESS);
 
     ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class)
@@ -817,25 +822,47 @@ public class TestActionScheduler {
 
   @Test
   public void testTimeoutRequestDueAgentRestartExecuteCommand() throws Exception {
    testTimeoutRequest(RoleCommand.EXECUTE);
    testTimeoutRequest(RoleCommand.EXECUTE, false, false);
   }
 
   @Test
   public void testTimeoutRequestDueAgentRestartCustomCommand() throws Exception {
    testTimeoutRequest(RoleCommand.CUSTOM_COMMAND);
    testTimeoutRequest(RoleCommand.CUSTOM_COMMAND, false, false);
   }
 
   @Test
   public void testTimeoutRequestDueAgentRestartActionExecute() throws Exception {
    testTimeoutRequest(RoleCommand.ACTIONEXECUTE);
    testTimeoutRequest(RoleCommand.ACTIONEXECUTE, false, false);
   }
 
   @Test
   public void testTimeoutRequestDueAgentRestartServiceCheck() throws Exception {
    testTimeoutRequest(RoleCommand.SERVICE_CHECK);
    testTimeoutRequest(RoleCommand.SERVICE_CHECK, false, false);
   }
 
  private void testTimeoutRequest(RoleCommand roleCommand) throws AmbariException, InvalidStateTransitionException {
  /**
   * Ensures that the task is timed out but is not skipped just because its
   * stage is skipped.
   *
   * @throws Exception
   */
  @Test
  public void testTimeoutWithSkippableStageButNotCommand() throws Exception {
    testTimeoutRequest(RoleCommand.EXECUTE, true, false);
  }

  /**
   * Ensures that the task is timed out and that it will be skipped.
   *
   * @throws Exception
   */
  @Test
  public void testTimeoutWithSkippableCommand() throws Exception {
    testTimeoutRequest(RoleCommand.EXECUTE, true, true);
  }

  private void testTimeoutRequest(RoleCommand roleCommand, boolean stageSupportsAutoSkip,
      boolean autoSkipFailedTask) throws AmbariException, InvalidStateTransitionException {
     final long HOST_REGISTRATION_TIME = 100L;
     final long STAGE_TASK_START_TIME = HOST_REGISTRATION_TIME - 1L;
 
@@ -872,7 +899,7 @@ public class TestActionScheduler {
       EasyMock.expectLastCall();
     }
 
    Stage s = getStageWithServerAction(1, 977, null, "test", 2);
    Stage s = getStageWithServerAction(1, 977, null, "test", 2, stageSupportsAutoSkip, autoSkipFailedTask);
     s.setStartTime(null, Role.AMBARI_SERVER_ACTION.toString(), STAGE_TASK_START_TIME);
     s.setHostRoleStatus(null, Role.AMBARI_SERVER_ACTION.toString(), HostRoleStatus.IN_PROGRESS);
     s.getExecutionCommands(null).get(0).getExecutionCommand().setServiceName("Service name");
@@ -881,7 +908,10 @@ public class TestActionScheduler {
     aq.enqueue(Stage.INTERNAL_HOSTNAME, s.getExecutionCommands(null).get(0).getExecutionCommand());
     List<ExecutionCommand> commandsToSchedule = new ArrayList<ExecutionCommand>();
 
    db.timeoutHostRole(EasyMock.anyString(), EasyMock.anyLong(), EasyMock.anyLong(), EasyMock.anyString(), EasyMock.anyBoolean());
    boolean taskShouldBeSkipped = stageSupportsAutoSkip && autoSkipFailedTask;
    db.timeoutHostRole(EasyMock.anyString(), EasyMock.anyLong(), EasyMock.anyLong(),
        EasyMock.anyString(), EasyMock.eq(taskShouldBeSkipped));

     EasyMock.expectLastCall();
 
     ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class)
@@ -910,7 +940,7 @@ public class TestActionScheduler {
     List<Stage> stages = new ArrayList<Stage>();
     Map<String, String> payload = new HashMap<String, String>();
     payload.put(MockServerAction.PAYLOAD_FORCE_FAIL, "exception");
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 300);
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 300, false, false);
     stages.add(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
@@ -978,20 +1008,27 @@ public class TestActionScheduler {
     assertEquals("test", stages.get(0).getRequestContext());
   }
 
  private Stage getStageWithServerAction(long requestId, long stageId,
                                                Map<String, String> payload, String requestContext,
                                                int timeout) {
  private Stage getStageWithServerAction(long requestId, long stageId, Map<String, String> payload,
      String requestContext, int timeout, boolean stageSupportsAutoSkip,
      boolean autoSkipFailedTask) {
 
     Stage stage = stageFactory.createNew(requestId, "/tmp", "cluster1", 1L, requestContext, CLUSTER_HOST_INFO,
       "{}", "{}");

     stage.setStageId(stageId);
    stage.setSkippable(stageSupportsAutoSkip);
    stage.setAutoSkipFailureSupported(stageSupportsAutoSkip);
 
     stage.addServerActionCommand(MockServerAction.class.getName(), null,
         Role.AMBARI_SERVER_ACTION,
         RoleCommand.EXECUTE, "cluster1",
         new ServiceComponentHostServerActionEvent(null, System.currentTimeMillis()),
         payload,
        null, null, timeout, false, false);
        null, null, timeout, false, autoSkipFailedTask);

    // make sure the task ID matches the command ID
    stage.getExecutionCommands(null).get(0).getExecutionCommand().setTaskId(
        stage.getOrderedHostRoleCommands().get(0).getTaskId());
 
     return stage;
   }
@@ -1752,7 +1789,6 @@ public class TestActionScheduler {
     Stage stage = stageFactory.createNew(requestId, "/tmp", clusterName, 1L, "getStageWithSingleTask",
       CLUSTER_HOST_INFO, "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
     stage.setStageId(stageId);
    //stage.setAutoSkipFailureSupported(true);
     return stage;
   }
 
@@ -2034,7 +2070,7 @@ public class TestActionScheduler {
 
     List<Stage> stages = new ArrayList<Stage>();
     Map<String, String> payload = new HashMap<String, String>();
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 300);
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 300, false, false);
     stages.add(s);
 
     ActionDBAccessor db = mock(ActionDBAccessor.class);
- 
2.19.1.windows.1

