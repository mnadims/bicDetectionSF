From 9df0de7807592c606918d268e60bbebc14d7eda0 Mon Sep 17 00:00:00 2001
From: ryota <ryota@unknown>
Date: Fri, 20 Sep 2013 07:39:43 +0000
Subject: [PATCH] OOZIE-1524 Change Workflow SELECT query to fetch only
 necessary columns and consolidate JPA Executors (ryota)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1524923 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/oozie/WorkflowActionBean.java  |  35 ++-
 .../org/apache/oozie/WorkflowJobBean.java     |  16 ++
 .../coord/CoordActionStartXCommand.java       |   4 +-
 .../oozie/command/wf/ActionCheckXCommand.java |   7 +-
 .../oozie/command/wf/ActionEndXCommand.java   |   9 +-
 .../oozie/command/wf/ActionKillXCommand.java  |   8 +-
 .../oozie/command/wf/ActionStartXCommand.java |   8 +-
 .../command/wf/CompletedActionXCommand.java   |   6 +-
 .../oozie/command/wf/DefinitionXCommand.java  |   5 +-
 .../apache/oozie/command/wf/KillXCommand.java |   4 +-
 .../oozie/command/wf/ReRunXCommand.java       |   4 +-
 .../oozie/command/wf/ResumeXCommand.java      |   4 +-
 .../oozie/command/wf/SignalXCommand.java      | 107 +++++----
 .../oozie/command/wf/SuspendXCommand.java     |   5 +-
 .../jpa/WorkflowActionQueryExecutor.java      | 206 +++++++++++++++++-
 .../WorkflowActionsRunningGetJPAExecutor.java |  68 ------
 .../jpa/WorkflowJobQueryExecutor.java         | 148 ++++++++++++-
 .../oozie/service/ActionCheckerService.java   |   8 +-
 .../oozie/service/AuthorizationService.java   |   5 +-
 .../org/apache/oozie/service/JPAService.java  |   4 +-
 .../jpa/TestWorkflowActionQueryExecutor.java  | 194 +++++++++++++++--
 ...tWorkflowActionsRunningGetJPAExecutor.java |  83 -------
 .../jpa/TestWorkflowJobQueryExecutor.java     | 190 ++++++++++++----
 .../oozie/sla/TestSLAEventGeneration.java     |  10 -
 .../org/apache/oozie/test/XDataTestCase.java  |  13 ++
 release-log.txt                               |   1 +
 26 files changed, 820 insertions(+), 332 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/WorkflowActionBean.java b/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
index e7f2638e2..e1cd072de 100644
-- a/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
++ b/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
@@ -65,15 +65,15 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "UPDATE_ACTION_CHECK", query = "update WorkflowActionBean a set a.userRetryCount = :userRetryCount, a.stats = :stats, a.externalChildIDs = :externalChildIDs, a.externalStatus = :externalStatus, a.statusStr = :status, a.data = :data, a.pending = :pending, a.errorCode = :errorCode, a.errorMessage = :errorMessage, a.lastCheckTimestamp = :lastCheckTime, a.retries = :retries, a.pendingAgeTimestamp = :pendingAge, a.startTimestamp = :startTime where a.id = :id"),
 
    @NamedQuery(name = "UPDATE_ACTION_END", query = "update WorkflowActionBean a set a.stats = :stats, a.errorCode = :errorCode, a.errorMessage = :errorMessage, a.retries = :retries, a.endTimestamp = :endTime, a.statusStr = :status, a.pending = :pending, a.pendingAgeTimestamp = :pendingAge, a.signalValue = :signalValue, a.userRetryCount = :userRetryCount, a.externalStatus = :externalStatus where a.id = :id"),
    @NamedQuery(name = "UPDATE_ACTION_END", query = "update WorkflowActionBean a set a.stats = :stats, a.errorCode = :errorCode, a.errorMessage = :errorMessage, a.retries = :retries, a.endTimestamp = :endTime, a.statusStr = :status, a.retries = :retries, a.pending = :pending, a.pendingAgeTimestamp = :pendingAge, a.signalValue = :signalValue, a.userRetryCount = :userRetryCount, a.externalStatus = :externalStatus where a.id = :id"),
 
    @NamedQuery(name = "UPDATE_ACTION_PENDING", query = "update WorkflowActionBean a set a.pending = :pending where a.id = :id"),
    @NamedQuery(name = "UPDATE_ACTION_PENDING", query = "update WorkflowActionBean a set a.pending = :pending, a.pendingAgeTimestamp = :pendingAge where a.id = :id"),
 
    @NamedQuery(name = "UPDATE_ACTION_STATUS_PENDING", query = "update WorkflowActionBean a set a.statusStr = :status, a.pending = :pending where a.id = :id"),
    @NamedQuery(name = "UPDATE_ACTION_STATUS_PENDING", query = "update WorkflowActionBean a set a.statusStr = :status, a.pending = :pending, a.pendingAgeTimestamp = :pendingAge where a.id = :id"),
 
    @NamedQuery(name = "UPDATE_ACTION_PENDING_TRANS", query = "update WorkflowActionBean a set a.pending = :pending, a.transition = :transition where a.id = :id"),
    @NamedQuery(name = "UPDATE_ACTION_PENDING_TRANS", query = "update WorkflowActionBean a set a.pending = :pending, a.pendingAgeTimestamp = :pendingAge, a.transition = :transition where a.id = :id"),
 
    @NamedQuery(name = "UPDATE_ACTION_PENDING_TRANS_ERROR", query = "update WorkflowActionBean a set a.pending = :pending, a.transition = :transition, a.errorCode = :errorCode, a.errorMessage = :errorMessage where a.id = :id"),
    @NamedQuery(name = "UPDATE_ACTION_PENDING_TRANS_ERROR", query = "update WorkflowActionBean a set a.pending = :pending, a.pendingAgeTimestamp = :pendingAge, a.transition = :transition, a.errorCode = :errorCode, a.errorMessage = :errorMessage where a.id = :id"),
 
     @NamedQuery(name = "DELETE_ACTION", query = "delete from WorkflowActionBean a where a.id = :id"),
 
@@ -83,6 +83,22 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "GET_ACTION", query = "select OBJECT(a) from WorkflowActionBean a where a.id = :id"),
 
    @NamedQuery(name = "GET_ACTION_ID_TYPE", query = "select a.id, a.type from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_FAIL", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.errorCode, a.errorMessage from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_SIGNAL", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.errorCode, a.errorMessage, a.executionPath, a.signalValue, a.slaXml from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_START", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.userRetryCount, a.userRetryMax, a.userRetryInterval, a.startTimestamp, a.endTimestamp, a.errorCode, a.errorMessage, a.cred, a.conf, a.slaXml from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_CHECK", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.trackerUri, a.startTimestamp, a.endTimestamp, a.lastCheckTimestamp, a.errorCode, a.errorMessage, a.externalId, a.externalStatus, a.externalChildIDs, a.conf from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_END", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.trackerUri, a.userRetryCount, a.userRetryMax, a.userRetryInterval, a.startTimestamp, a.endTimestamp, a.errorCode, a.errorMessage, a.externalId, a.externalStatus, a.externalChildIDs, a.conf, a.data, a.stats from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_KILL", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.trackerUri, a.startTimestamp, a.endTimestamp, a.errorCode, a.errorMessage, a.externalId, a.conf, a.data from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_COMPLETED", query = "select a.id, a.wfId, a.statusStr, a.type, a.logToken from WorkflowActionBean a where a.id = :id"),

     @NamedQuery(name = "GET_ACTION_FOR_UPDATE", query = "select OBJECT(a) from WorkflowActionBean a where a.id = :id"),
 
     @NamedQuery(name = "GET_ACTION_FOR_SLA", query = "select a.id, a.statusStr, a.startTimestamp, a.endTimestamp from WorkflowActionBean a where a.id = :id"),
@@ -93,7 +109,7 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "GET_PENDING_ACTIONS", query = "select OBJECT(a) from WorkflowActionBean a where a.pending = 1 AND a.pendingAgeTimestamp < :pendingAge AND a.statusStr <> 'RUNNING'"),
 
    @NamedQuery(name = "GET_RUNNING_ACTIONS", query = "select OBJECT(a) from WorkflowActionBean a where a.pending = 1 AND a.statusStr = 'RUNNING' AND a.lastCheckTimestamp < :lastCheckTime"),
    @NamedQuery(name = "GET_RUNNING_ACTIONS", query = "select a.id from WorkflowActionBean a where a.pending = 1 AND a.statusStr = 'RUNNING' AND a.lastCheckTimestamp < :lastCheckTime"),
 
     @NamedQuery(name = "GET_RETRY_MANUAL_ACTIONS", query = "select OBJECT(a) from WorkflowActionBean a where a.wfId = :wfId AND (a.statusStr = 'START_RETRY' OR a.statusStr = 'START_MANUAL' OR a.statusStr = 'END_RETRY' OR a.statusStr = 'END_MANUAL')") })
 @Table(name = "WF_ACTIONS")
@@ -414,6 +430,13 @@ public class WorkflowActionBean implements Writable, WorkflowAction, JsonBean {
         pendingAgeTimestamp = DateUtils.convertDateToTimestamp(new Date());
     }
 
    /**
     * Set pending flag
     */
    public void setPending(int i) {
        pending = i;
    }

     /**
      * Set a time when the action will be pending, normally a time in the
      * future.
diff --git a/core/src/main/java/org/apache/oozie/WorkflowJobBean.java b/core/src/main/java/org/apache/oozie/WorkflowJobBean.java
index ac69da631..0ddee0342 100644
-- a/core/src/main/java/org/apache/oozie/WorkflowJobBean.java
++ b/core/src/main/java/org/apache/oozie/WorkflowJobBean.java
@@ -88,6 +88,22 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "GET_WORKFLOW", query = "select OBJECT(w) from WorkflowJobBean w where w.id = :id"),
 
    @NamedQuery(name = "GET_WORKFLOW_STARTTIME", query = "select w.id, w.startTimestamp from WorkflowJobBean w where w.id = :id"),

    @NamedQuery(name = "GET_WORKFLOW_USER_GROUP", query = "select w.user, w.group from WorkflowJobBean w where w.id = :id"),

    @NamedQuery(name = "GET_WORKFLOW_SUSPEND", query = "select w.id, w.user, w.group, w.appName, w.statusStr, w.parentId, w.startTimestamp, w.endTimestamp, w.logToken, w.wfInstance  from WorkflowJobBean w where w.id = :id"),

    @NamedQuery(name = "GET_WORKFLOW_RERUN", query = "select w.id, w.user, w.group, w.appName, w.statusStr, w.run, w.logToken, w.wfInstance from WorkflowJobBean w where w.id = :id"),

    @NamedQuery(name = "GET_WORKFLOW_DEFINITION", query = "select w.id, w.user, w.group, w.appName, w.logToken, w.wfInstance from WorkflowJobBean w where w.id = :id"),

    @NamedQuery(name = "GET_WORKFLOW_ACTION_OP", query = "select w.id, w.user, w.group, w.appName, w.appPath, w.statusStr, w.parentId, w.logToken, w.wfInstance, w.protoActionConf from WorkflowJobBean w where w.id = :id"),

    @NamedQuery(name = "GET_WORKFLOW_KILL", query = "select w.id, w.user, w.group, w.appName, w.appPath, w.statusStr, w.parentId, w.startTimestamp, w.endTimestamp, w.logToken, w.wfInstance, w.slaXml from WorkflowJobBean w where w.id = :id"),

    @NamedQuery(name = "GET_WORKFLOW_RESUME", query = "select w.id, w.user, w.group, w.appName, w.appPath, w.statusStr, w.parentId, w.startTimestamp, w.endTimestamp, w.logToken, w.wfInstance, w.protoActionConf from WorkflowJobBean w where w.id = :id"),

     @NamedQuery(name = "GET_WORKFLOW_FOR_UPDATE", query = "select OBJECT(w) from WorkflowJobBean w where w.id = :id"),
 
     @NamedQuery(name = "GET_WORKFLOW_FOR_SLA", query = "select w.id, w.statusStr, w.startTimestamp, w.endTimestamp from WorkflowJobBean w where w.id = :id"),
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionStartXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionStartXCommand.java
index 1a6a931f4..dca5f4a77 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionStartXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionStartXCommand.java
@@ -47,7 +47,7 @@ import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.jdom.Element;
 import org.jdom.JDOMException;
@@ -188,7 +188,7 @@ public class CoordActionStartXCommand extends CoordinatorXCommand<Void> {
                 JPAService jpaService = Services.get().get(JPAService.class);
                 if (jpaService != null) {
                     log.debug("Updating WF record for WFID :" + wfId + " with parent id: " + actionId);
                    WorkflowJobBean wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(wfId));
                    WorkflowJobBean wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_STARTTIME, wfId);
                     wfJob.setParentId(actionId);
                     wfJob.setLastModifiedTime(new Date());
                     BatchQueryExecutor executor = BatchQueryExecutor.getInstance();
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionCheckXCommand.java
index f900fc37d..f7cb9405c 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionCheckXCommand.java
@@ -33,10 +33,9 @@ import org.apache.oozie.client.WorkflowJob;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowActionGetJPAExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
@@ -87,8 +86,8 @@ public class ActionCheckXCommand extends ActionXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(jobId));
                this.wfAction = jpaService.execute(new WorkflowActionGetJPAExecutor(actionId));
                this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_ACTION_OP, jobId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_CHECK, actionId);
                 LogUtils.setLogInfo(wfJob, logInfo);
                 LogUtils.setLogInfo(wfAction, logInfo);
             }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java
index dfa60fc21..fb9dec3e8 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java
@@ -42,8 +42,8 @@ import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowActionGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.ActionService;
@@ -97,8 +97,9 @@ public class ActionEndXCommand extends ActionXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(jobId));
                this.wfAction = jpaService.execute(new WorkflowActionGetJPAExecutor(actionId));
                this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_ACTION_OP,
                        jobId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_END, actionId);
                 LogUtils.setLogInfo(wfJob, logInfo);
                 LogUtils.setLogInfo(wfAction, logInfo);
             }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
index e51d33e3a..d4d01f3ca 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
@@ -34,8 +34,8 @@ import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowActionGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.action.ActionExecutor;
@@ -95,8 +95,8 @@ public class ActionKillXCommand extends ActionXCommand<Void> {
             jpaService = Services.get().get(JPAService.class);
 
             if (jpaService != null) {
                this.wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(jobId));
                this.wfAction = jpaService.execute(new WorkflowActionGetJPAExecutor(actionId));
                this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_ACTION_OP, jobId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_KILL, actionId);
                 LogUtils.setLogInfo(wfJob, logInfo);
                 LogUtils.setLogInfo(wfAction, logInfo);
             }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
index f7e00aeca..7e9415a10 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
@@ -43,9 +43,9 @@ import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowActionGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.ActionService;
 import org.apache.oozie.service.EventHandlerService;
@@ -97,8 +97,8 @@ public class ActionStartXCommand extends ActionXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(jobId));
                this.wfAction = jpaService.execute(new WorkflowActionGetJPAExecutor(actionId));
                this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW, jobId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_START, actionId);
                 LogUtils.setLogInfo(wfJob, logInfo);
                 LogUtils.setLogInfo(wfAction, logInfo);
             }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
index 2dcd82601..65d91bd41 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
@@ -24,7 +24,8 @@ import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.executor.jpa.WorkflowActionGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.service.ActionService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
@@ -61,7 +62,8 @@ public class CompletedActionXCommand extends WorkflowXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfactionBean = jpaService.execute(new WorkflowActionGetJPAExecutor(this.actionId));
                this.wfactionBean = WorkflowActionQueryExecutor.getInstance().get(
                        WorkflowActionQuery.GET_ACTION_COMPLETED, this.actionId);
             }
             else {
                 throw new CommandException(ErrorCode.E0610);
diff --git a/core/src/main/java/org/apache/oozie/command/wf/DefinitionXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/DefinitionXCommand.java
index 35ec979d1..64d4822be 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/DefinitionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/DefinitionXCommand.java
@@ -22,7 +22,8 @@ import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.XException;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.LogUtils;
@@ -53,7 +54,7 @@ public class DefinitionXCommand extends WorkflowXCommand<String> {
             JPAService jpaService = Services.get().get(JPAService.class);
 
             if (jpaService != null) {
                this.wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(jobId));
                this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_DEFINITION, jobId);
                 LogUtils.setLogInfo(wfJob, logInfo);
             }
             else {
diff --git a/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java
index 1ae160d8a..7fb968cd6 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java
@@ -33,7 +33,7 @@ import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.executor.jpa.WorkflowActionsGetForJobJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.service.ActionService;
@@ -92,7 +92,7 @@ public class KillXCommand extends WorkflowXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(wfId));
                this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_KILL, wfId);
                 this.actionList = jpaService.execute(new WorkflowActionsGetForJobJPAExecutor(wfId));
                 LogUtils.setLogInfo(wfJob, logInfo);
             }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
index 7dc0f6e25..9287211af 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
@@ -45,8 +45,8 @@ import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.WorkflowActionsGetForJobJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.DagXLogInfoService;
@@ -272,7 +272,7 @@ public class ReRunXCommand extends WorkflowXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfBean = jpaService.execute(new WorkflowJobGetJPAExecutor(this.jobId));
                this.wfBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_RERUN, this.jobId);
                 this.actions = jpaService.execute(new WorkflowActionsGetForJobJPAExecutor(this.jobId));
             }
             else {
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ResumeXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ResumeXCommand.java
index f6ddd3326..7b8ee3ba3 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ResumeXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ResumeXCommand.java
@@ -40,7 +40,7 @@ import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.executor.jpa.WorkflowJobGetActionsJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.HadoopAccessorException;
@@ -182,7 +182,7 @@ public class ResumeXCommand extends WorkflowXCommand<Void> {
             throw new CommandException(ErrorCode.E0610);
         }
         try {
            workflow = jpaService.execute(new WorkflowJobGetJPAExecutor(id));
            workflow = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_RESUME, id);
         }
         catch (JPAExecutorException e) {
             throw new CommandException(e);
diff --git a/core/src/main/java/org/apache/oozie/command/wf/SignalXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/SignalXCommand.java
index aa1acb071..0992026b1 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/SignalXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/SignalXCommand.java
@@ -34,9 +34,9 @@ import org.apache.oozie.command.wf.ActionXCommand.ActionExecutorContext;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowActionGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.ELService;
 import org.apache.oozie.service.EventHandlerService;
@@ -79,7 +79,6 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
     private String wfJobErrorCode;
     private String wfJobErrorMsg;
 

     public SignalXCommand(String name, int priority, String jobId) {
         super(name, name, priority);
         this.jobId = ParamChecker.notEmpty(jobId, "jobId");
@@ -110,10 +109,10 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfJob = jpaService.execute(new WorkflowJobGetJPAExecutor(jobId));
                this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW, jobId);
                 LogUtils.setLogInfo(wfJob, logInfo);
                 if (actionId != null) {
                    this.wfAction = jpaService.execute(new WorkflowActionGetJPAExecutor(actionId));
                    this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_SIGNAL, actionId);
                     LogUtils.setLogInfo(wfAction, logInfo);
                 }
             }
@@ -158,14 +157,14 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                 wfJob.setWorkflowInstance(workflowInstance);
                 generateEvent = true;
                 // 1. Add SLA status event for WF-JOB with status STARTED
                SLAEventBean slaEvent = SLADbXOperations.createStatusEvent(wfJob.getSlaXml(), jobId,
                        Status.STARTED, SlaAppType.WORKFLOW_JOB);
                if(slaEvent != null) {
                SLAEventBean slaEvent = SLADbXOperations.createStatusEvent(wfJob.getSlaXml(), jobId, Status.STARTED,
                        SlaAppType.WORKFLOW_JOB);
                if (slaEvent != null) {
                     insertList.add(slaEvent);
                 }
                 // 2. Add SLA registration events for all WF_ACTIONS
                createSLARegistrationForAllActions(workflowInstance.getApp().getDefinition(), wfJob.getUser(), wfJob
                        .getGroup(), wfJob.getConf());
                createSLARegistrationForAllActions(workflowInstance.getApp().getDefinition(), wfJob.getUser(),
                        wfJob.getGroup(), wfJob.getConf());
                 queue(new NotificationXCommand(wfJob));
             }
             else {
@@ -205,7 +204,8 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                 for (String actionToKillId : WorkflowStoreService.getActionsToKill(workflowInstance)) {
                     WorkflowActionBean actionToKill;
 
                    actionToKill = jpaService.execute(new WorkflowActionGetJPAExecutor(actionToKillId));
                    actionToKill = WorkflowActionQueryExecutor.getInstance().get(
                            WorkflowActionQuery.GET_ACTION_ID_TYPE, actionToKillId);
 
                     actionToKill.setPending();
                     actionToKill.setStatus(WorkflowActionBean.Status.KILLED);
@@ -215,8 +215,8 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                 }
 
                 for (String actionToFailId : WorkflowStoreService.getActionsToFail(workflowInstance)) {
                    WorkflowActionBean actionToFail = jpaService.execute(new WorkflowActionGetJPAExecutor(
                            actionToFailId));
                    WorkflowActionBean actionToFail = WorkflowActionQueryExecutor.getInstance().get(
                            WorkflowActionQuery.GET_ACTION_FAIL, actionToFailId);
                     actionToFail.resetPending();
                     actionToFail.setStatus(WorkflowActionBean.Status.FAILED);
                     if (wfJobErrorCode != null) {
@@ -226,7 +226,7 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                     queue(new NotificationXCommand(wfJob, actionToFail));
                     SLAEventBean slaEvent = SLADbXOperations.createStatusEvent(wfAction.getSlaXml(), wfAction.getId(),
                             Status.FAILED, SlaAppType.WORKFLOW_ACTION);
                    if(slaEvent != null) {
                    if (slaEvent != null) {
                         insertList.add(slaEvent);
                     }
                     updateList.add(new UpdateEntry<WorkflowActionQuery>(
@@ -254,9 +254,9 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                 default: // TODO SUSPENDED
                     break;
             }
            SLAEventBean slaEvent = SLADbXOperations.createStatusEvent(wfJob.getSlaXml(), jobId,
                    slaStatus, SlaAppType.WORKFLOW_JOB);
            if(slaEvent != null) {
            SLAEventBean slaEvent = SLADbXOperations.createStatusEvent(wfJob.getSlaXml(), jobId, slaStatus,
                    SlaAppType.WORKFLOW_JOB);
            if (slaEvent != null) {
                 insertList.add(slaEvent);
             }
             queue(new NotificationXCommand(wfJob));
@@ -276,9 +276,8 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                         String tmpNodeConf = nodeDef.getConf();
                         String actionConf = context.getELEvaluator().evaluate(tmpNodeConf, String.class);
                         LOG.debug(
                                "Try to resolve KillNode message for jobid [{0}], actionId [{1}], before resolve [{2}], " +
                                "after resolve [{3}]",
                                jobId, actionId, tmpNodeConf, actionConf);
                                "Try to resolve KillNode message for jobid [{0}], actionId [{1}], before resolve [{2}], "
                                        + "after resolve [{3}]", jobId, actionId, tmpNodeConf, actionConf);
                         if (wfAction.getErrorCode() != null) {
                             wfAction.setErrorInfo(wfAction.getErrorCode(), actionConf);
                         }
@@ -304,39 +303,35 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                 if (skipVar != null) {
                     skipNewAction = skipVar.equals("true");
                 }
                try {
                    if (skipNewAction) {
                        WorkflowActionBean oldAction;

                        oldAction = jpaService.execute(new WorkflowActionGetJPAExecutor(newAction.getId()));
 
                        oldAction.setPending();
                        updateList.add(new UpdateEntry<WorkflowActionQuery>(WorkflowActionQuery.UPDATE_ACTION_PENDING, oldAction));
                if (skipNewAction) {
                    WorkflowActionBean oldAction = new WorkflowActionBean();
                    oldAction.setId(newAction.getId());
                    oldAction.setPending();
                    updateList.add(new UpdateEntry<WorkflowActionQuery>(WorkflowActionQuery.UPDATE_ACTION_PENDING,
                            oldAction));
                    queue(new SignalXCommand(jobId, oldAction.getId()));
                }
                else {
                    try {
                        // Make sure that transition node for a forked action
                        // is inserted only once
                        WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_ID_TYPE,
                                newAction.getId());
 
                        queue(new SignalXCommand(jobId, oldAction.getId()));
                        continue;
                     }
                    else {
                        try {
                            // Make sure that transition node for a forked action
                            // is inserted only once
                            jpaService.execute(new WorkflowActionGetJPAExecutor(newAction.getId()));
                            continue;
                        }
                        catch (JPAExecutorException jee) {
                        }
                        checkForSuspendNode(newAction);
                        newAction.setPending();
                        String actionSlaXml = getActionSLAXml(newAction.getName(), workflowInstance.getApp()
                                .getDefinition(), wfJob.getConf());
                        newAction.setSlaXml(actionSlaXml);
                        insertList.add(newAction);
                        LOG.debug("SignalXCommand: Name: " + newAction.getName() + ", Id: " + newAction.getId()
                                + ", Authcode:" + newAction.getCred());
                        queue(new ActionStartXCommand(newAction.getId(), newAction.getType()));
                    catch (JPAExecutorException jee) {
                     }
                }
                catch (JPAExecutorException je) {
                    throw new CommandException(je);
                    checkForSuspendNode(newAction);
                    newAction.setPending();
                    String actionSlaXml = getActionSLAXml(newAction.getName(), workflowInstance.getApp()
                            .getDefinition(), wfJob.getConf());
                    newAction.setSlaXml(actionSlaXml);
                    insertList.add(newAction);
                    LOG.debug("SignalXCommand: Name: " + newAction.getName() + ", Id: " + newAction.getId()
                            + ", Authcode:" + newAction.getCred());
                    queue(new ActionStartXCommand(newAction.getId(), newAction.getType()));
                 }
             }
         }
@@ -354,11 +349,10 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
         catch (JPAExecutorException je) {
             throw new CommandException(je);
         }
        LOG.debug(
                "Updated the workflow status to " + wfJob.getId() + "  status =" + wfJob.getStatusStr());
        LOG.debug("Updated the workflow status to " + wfJob.getId() + "  status =" + wfJob.getStatusStr());
         if (wfJob.getStatus() != WorkflowJob.Status.RUNNING && wfJob.getStatus() != WorkflowJob.Status.SUSPENDED) {
             updateParentIfNecessary(wfJob);
            new WfEndXCommand(wfJob).call(); //To delete the WF temp dir
            new WfEndXCommand(wfJob).call(); // To delete the WF temp dir
         }
         LOG.debug("ENDED SignalCommand for jobid=" + jobId + ", actionId=" + actionId);
         return null;
@@ -417,11 +411,11 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
                 if (eSla != null) {
                     String slaXml = resolveSla(eSla, conf);
                     eSla = XmlUtils.parseXml(slaXml);
                    String actionId = Services.get().get(UUIDService.class).generateChildId(jobId,
                            action.getAttributeValue("name") + "");
                    String actionId = Services.get().get(UUIDService.class)
                            .generateChildId(jobId, action.getAttributeValue("name") + "");
                     SLAEventBean slaEvent = SLADbXOperations.createSlaRegistrationEvent(eSla, actionId,
                             SlaAppType.WORKFLOW_ACTION, user, group);
                    if(slaEvent != null) {
                    if (slaEvent != null) {
                         insertList.add(slaEvent);
                     }
                 }
@@ -439,7 +433,8 @@ public class SignalXCommand extends WorkflowXCommand<Void> {
             String[] values = wfjobConf.getTrimmedStrings(OozieClient.OOZIE_SUSPEND_ON_NODES);
             if (values != null) {
                 if (values.length == 1 && values[0].equals("*")) {
                    LOG.info("Reached suspend node at [{0}], suspending workflow [{1}]", newAction.getName(), wfJob.getId());
                    LOG.info("Reached suspend node at [{0}], suspending workflow [{1}]", newAction.getName(),
                            wfJob.getId());
                     queue(new SuspendXCommand(jobId));
                 }
                 else {
diff --git a/core/src/main/java/org/apache/oozie/command/wf/SuspendXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/SuspendXCommand.java
index ea9a3dada..1e8a12295 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/SuspendXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/SuspendXCommand.java
@@ -32,7 +32,7 @@ import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.executor.jpa.WorkflowActionRetryManualGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
@@ -147,7 +147,8 @@ public class SuspendXCommand extends WorkflowXCommand<Void> {
         try {
             jpaService = Services.get().get(JPAService.class);
             if (jpaService != null) {
                this.wfJobBean = jpaService.execute(new WorkflowJobGetJPAExecutor(this.wfid));
                this.wfJobBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_SUSPEND,
                        this.wfid);
             }
             else {
                 throw new CommandException(ErrorCode.E0610);
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
index 52d6ae081..a8983df10 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
@@ -17,14 +17,18 @@
  */
 package org.apache.oozie.executor.jpa;
 
import java.sql.Timestamp;
import java.util.ArrayList;
 import java.util.List;
 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.StringBlob;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.util.DateUtils;
 
 import com.google.common.annotations.VisibleForTesting;
 
@@ -44,7 +48,16 @@ public class WorkflowActionQueryExecutor extends
         UPDATE_ACTION_STATUS_PENDING,
         UPDATE_ACTION_PENDING_TRANS,
         UPDATE_ACTION_PENDING_TRANS_ERROR,
        GET_ACTION
        GET_ACTION,
        GET_ACTION_ID_TYPE,
        GET_ACTION_FAIL,
        GET_ACTION_SIGNAL,
        GET_ACTION_START,
        GET_ACTION_CHECK,
        GET_ACTION_END,
        GET_ACTION_KILL,
        GET_ACTION_COMPLETED,
        GET_RUNNING_ACTIONS
     };
 
     private static WorkflowActionQueryExecutor instance = new WorkflowActionQueryExecutor();
@@ -106,21 +119,25 @@ public class WorkflowActionQueryExecutor extends
                 break;
             case UPDATE_ACTION_PENDING:
                 query.setParameter("pending", actionBean.getPending());
                query.setParameter("pendingAge", actionBean.getPendingAgeTimestamp());
                 query.setParameter("id", actionBean.getId());
                 break;
             case UPDATE_ACTION_STATUS_PENDING:
                 query.setParameter("status", actionBean.getStatus().toString());
                 query.setParameter("pending", actionBean.getPending());
                query.setParameter("pendingAge", actionBean.getPendingAgeTimestamp());
                 query.setParameter("id", actionBean.getId());
                 break;
             case UPDATE_ACTION_PENDING_TRANS:
                 query.setParameter("transition", actionBean.getTransition());
                 query.setParameter("pending", actionBean.getPending());
                query.setParameter("pendingAge", actionBean.getPendingAgeTimestamp());
                 query.setParameter("id", actionBean.getId());
                 break;
             case UPDATE_ACTION_PENDING_TRANS_ERROR:
                 query.setParameter("transition", actionBean.getTransition());
                 query.setParameter("pending", actionBean.getPending());
                query.setParameter("pendingAge", actionBean.getPendingAgeTimestamp());
                 query.setParameter("errorCode", actionBean.getErrorCode());
                 query.setParameter("errorMessage", actionBean.getErrorMessage());
                 query.setParameter("id", actionBean.getId());
@@ -166,6 +183,7 @@ public class WorkflowActionQueryExecutor extends
                 query.setParameter("retries", actionBean.getRetries());
                 query.setParameter("status", actionBean.getStatus().toString());
                 query.setParameter("endTime", actionBean.getEndTimestamp());
                query.setParameter("retries", actionBean.getRetries());
                 query.setParameter("pending", actionBean.getPending());
                 query.setParameter("pendingAge", actionBean.getPendingAgeTimestamp());
                 query.setParameter("signalValue", actionBean.getSignalValue());
@@ -185,14 +203,25 @@ public class WorkflowActionQueryExecutor extends
     public Query getSelectQuery(WorkflowActionQuery namedQuery, EntityManager em, Object... parameters)
             throws JPAExecutorException {
         Query query = em.createNamedQuery(namedQuery.name());
        WorkflowActionQuery waQuery = (WorkflowActionQuery) namedQuery;
        switch (waQuery) {
        switch (namedQuery) {
             case GET_ACTION:
            case GET_ACTION_ID_TYPE:
            case GET_ACTION_FAIL:
            case GET_ACTION_SIGNAL:
            case GET_ACTION_START:
            case GET_ACTION_CHECK:
            case GET_ACTION_END:
            case GET_ACTION_KILL:
            case GET_ACTION_COMPLETED:
                 query.setParameter("id", parameters[0]);
                 break;
            case GET_RUNNING_ACTIONS:
                Timestamp ts = new Timestamp(System.currentTimeMillis() - (Integer)parameters[0] * 1000);
                query.setParameter("lastCheckTime", ts);
                break;
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
                        + waQuery.name());
                        + namedQuery.name());
         }
         return query;
     }
@@ -205,15 +234,167 @@ public class WorkflowActionQueryExecutor extends
         return ret;
     }
 
    private WorkflowActionBean constructBean(WorkflowActionQuery namedQuery, Object ret) throws JPAExecutorException {
        WorkflowActionBean bean;
        Object[] arr;
        switch (namedQuery) {
            case GET_ACTION:
                bean = (WorkflowActionBean) ret;
                break;
            case GET_ACTION_ID_TYPE:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setType((String) arr[1]);
                break;
            case GET_ACTION_FAIL:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setJobId((String) arr[1]);
                bean.setName((String) arr[2]);
                bean.setStatusStr((String) arr[3]);
                bean.setPending((Integer) arr[4]);
                bean.setType((String) arr[5]);
                bean.setLogToken((String) arr[6]);
                bean.setTransition((String) arr[7]);
                bean.setErrorInfo((String) arr[8], (String) arr[9]);
                break;
            case GET_ACTION_SIGNAL:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setJobId((String) arr[1]);
                bean.setName((String) arr[2]);
                bean.setStatusStr((String) arr[3]);
                bean.setPending((Integer) arr[4]);
                bean.setType((String) arr[5]);
                bean.setLogToken((String) arr[6]);
                bean.setTransition((String) arr[7]);
                bean.setErrorInfo((String) arr[8], (String) arr[9]);
                bean.setExecutionPath((String) arr[10]);
                bean.setSignalValue((String) arr[11]);
                bean.setSlaXmlBlob((StringBlob) arr[12]);
                break;
            case GET_ACTION_START:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setJobId((String) arr[1]);
                bean.setName((String) arr[2]);
                bean.setStatusStr((String) arr[3]);
                bean.setPending((Integer) arr[4]);
                bean.setType((String) arr[5]);
                bean.setLogToken((String) arr[6]);
                bean.setTransition((String) arr[7]);
                bean.setRetries((Integer) arr[8]);
                bean.setUserRetryCount((Integer) arr[9]);
                bean.setUserRetryMax((Integer) arr[10]);
                bean.setUserRetryInterval((Integer) arr[11]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[12]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[13]));
                bean.setErrorInfo((String) arr[14], (String) arr[15]);
                bean.setCred((String) arr[16]);
                bean.setConfBlob((StringBlob) arr[17]);
                bean.setSlaXmlBlob((StringBlob) arr[18]);
                break;
            case GET_ACTION_CHECK:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setJobId((String) arr[1]);
                bean.setName((String) arr[2]);
                bean.setStatusStr((String) arr[3]);
                bean.setPending((Integer) arr[4]);
                bean.setType((String) arr[5]);
                bean.setLogToken((String) arr[6]);
                bean.setTransition((String) arr[7]);
                bean.setRetries((Integer) arr[8]);
                bean.setTrackerUri((String) arr[9]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[10]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[11]));
                bean.setLastCheckTime(DateUtils.toDate((Timestamp) arr[12]));
                bean.setErrorInfo((String) arr[13], (String) arr[14]);
                bean.setExternalId((String) arr[15]);
                bean.setExternalStatus((String) arr[16]);
                bean.setExternalChildIDsBlob((StringBlob) arr[17]);
                bean.setConfBlob((StringBlob) arr[18]);
                break;
            case GET_ACTION_END:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setJobId((String) arr[1]);
                bean.setName((String) arr[2]);
                bean.setStatusStr((String) arr[3]);
                bean.setPending((Integer) arr[4]);
                bean.setType((String) arr[5]);
                bean.setLogToken((String) arr[6]);
                bean.setTransition((String) arr[7]);
                bean.setRetries((Integer) arr[8]);
                bean.setTrackerUri((String) arr[9]);
                bean.setUserRetryCount((Integer) arr[10]);
                bean.setUserRetryMax((Integer) arr[11]);
                bean.setUserRetryInterval((Integer) arr[12]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[13]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[14]));
                bean.setErrorInfo((String) arr[15], (String) arr[16]);
                bean.setExternalId((String) arr[17]);
                bean.setExternalStatus((String) arr[18]);
                bean.setExternalChildIDsBlob((StringBlob) arr[19]);
                bean.setConfBlob((StringBlob) arr[20]);
                bean.setDataBlob((StringBlob) arr[21]);
                bean.setStatsBlob((StringBlob) arr[22]);
                break;
            case GET_ACTION_KILL:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setJobId((String) arr[1]);
                bean.setName((String) arr[2]);
                bean.setStatusStr((String) arr[3]);
                bean.setPending((Integer) arr[4]);
                bean.setType((String) arr[5]);
                bean.setLogToken((String) arr[6]);
                bean.setTransition((String) arr[7]);
                bean.setRetries((Integer) arr[8]);
                bean.setTrackerUri((String) arr[9]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[10]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[11]));
                bean.setErrorInfo((String) arr[12], (String) arr[13]);
                bean.setExternalId((String) arr[14]);
                bean.setConfBlob((StringBlob) arr[15]);
                bean.setDataBlob((StringBlob) arr[16]);
                break;
            case GET_ACTION_COMPLETED:
                bean = new WorkflowActionBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setJobId((String) arr[1]);
                bean.setStatusStr((String) arr[2]);
                bean.setType((String) arr[3]);
                bean.setLogToken((String) arr[4]);
                break;
            case GET_RUNNING_ACTIONS:
                bean = new WorkflowActionBean();
                bean.setId((String)ret);
                break;
            default:
                throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct action bean for "
                        + namedQuery.name());
        }
        return bean;
    }

     @Override
     public WorkflowActionBean get(WorkflowActionQuery namedQuery, Object... parameters) throws JPAExecutorException {
         EntityManager em = jpaService.getEntityManager();
         Query query = getSelectQuery(namedQuery, em, parameters);
        WorkflowActionBean bean = null;
        bean = (WorkflowActionBean) jpaService.executeGet(namedQuery.name(), query, em);
        if (bean == null) {
        Object ret = jpaService.executeGet(namedQuery.name(), query, em);
        if (ret == null) {
             throw new JPAExecutorException(ErrorCode.E0605, query.toString());
         }
        WorkflowActionBean bean = constructBean(namedQuery, ret);
         return bean;
     }
 
@@ -222,10 +403,13 @@ public class WorkflowActionQueryExecutor extends
             throws JPAExecutorException {
         EntityManager em = jpaService.getEntityManager();
         Query query = getSelectQuery(namedQuery, em, parameters);
        List<WorkflowActionBean> beanList = (List<WorkflowActionBean>) jpaService.executeGetList(namedQuery.name(),
                query, em);
        if (beanList == null || beanList.size() == 0) {
            throw new JPAExecutorException(ErrorCode.E0605, query.toString());
        List<?> retList = (List<?>) jpaService.executeGetList(namedQuery.name(), query, em);
        List<WorkflowActionBean> beanList = null;
        if (retList != null) {
            beanList = new ArrayList<WorkflowActionBean>();
            for (Object ret : retList) {
                beanList.add(constructBean(namedQuery, ret));
            }
         }
         return beanList;
     }
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionsRunningGetJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionsRunningGetJPAExecutor.java
index ec722bcdd..e69de29bb 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionsRunningGetJPAExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionsRunningGetJPAExecutor.java
@@ -1,68 +0,0 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.executor.jpa;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.oozie.ErrorCode;
import org.apache.oozie.WorkflowActionBean;

/**
 * JPA Executor to get running workflow actions
 */
public class WorkflowActionsRunningGetJPAExecutor implements JPAExecutor<List<WorkflowActionBean>> {

    private final long checkAgeSecs;

    public WorkflowActionsRunningGetJPAExecutor(long checkAgeSecs) {
        this.checkAgeSecs = checkAgeSecs;
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.executor.jpa.JPAExecutor#execute(javax.persistence.EntityManager)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<WorkflowActionBean> execute(EntityManager em) throws JPAExecutorException {
        List<WorkflowActionBean> actions;
        try {
            Timestamp ts = new Timestamp(System.currentTimeMillis() - checkAgeSecs * 1000);
            Query q = em.createNamedQuery("GET_RUNNING_ACTIONS");
            q.setParameter("lastCheckTime", ts);
            actions = q.getResultList();
         }
        catch (Exception e) {
            throw new JPAExecutorException(ErrorCode.E0605, "null", e);
        }
        return actions;
    }

    /* (non-Javadoc)
     * @see org.apache.oozie.executor.jpa.JPAExecutor#getName()
     */
    @Override
    public String getName() {
        return "WorkflowActionsRunningGetJPAExecutor";
    }

}
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java
index 58c936bb4..6d78deee7 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java
@@ -17,14 +17,21 @@
  */
 package org.apache.oozie.executor.jpa;
 
import java.sql.Timestamp;
import java.util.ArrayList;
 import java.util.List;
 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
import org.apache.oozie.BinaryBlob;
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.StringBlob;
import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.util.DateUtils;
 
 import com.google.common.annotations.VisibleForTesting;
 
@@ -43,7 +50,14 @@ public class WorkflowJobQueryExecutor extends QueryExecutor<WorkflowJobBean, Wor
         UPDATE_WORKFLOW_STATUS_INSTANCE_MOD_START_END,
         UPDATE_WORKFLOW_RERUN,
         GET_WORKFLOW,
        DELETE_WORKFLOW
        GET_WORKFLOW_STARTTIME,
        GET_WORKFLOW_USER_GROUP,
        GET_WORKFLOW_SUSPEND,
        GET_WORKFLOW_ACTION_OP,
        GET_WORKFLOW_RERUN,
        GET_WORKFLOW_DEFINITION,
        GET_WORKFLOW_KILL,
        GET_WORKFLOW_RESUME
     };
 
     private static WorkflowJobQueryExecutor instance = new WorkflowJobQueryExecutor();
@@ -154,6 +168,14 @@ public class WorkflowJobQueryExecutor extends QueryExecutor<WorkflowJobBean, Wor
         Query query = em.createNamedQuery(namedQuery.name());
         switch (namedQuery) {
             case GET_WORKFLOW:
            case GET_WORKFLOW_STARTTIME:
            case GET_WORKFLOW_USER_GROUP:
            case GET_WORKFLOW_SUSPEND:
            case GET_WORKFLOW_ACTION_OP:
            case GET_WORKFLOW_RERUN:
            case GET_WORKFLOW_DEFINITION:
            case GET_WORKFLOW_KILL:
            case GET_WORKFLOW_RESUME:
                 query.setParameter("id", parameters[0]);
                 break;
             default:
@@ -171,14 +193,123 @@ public class WorkflowJobQueryExecutor extends QueryExecutor<WorkflowJobBean, Wor
         return ret;
     }
 
    private WorkflowJobBean constructBean(WorkflowJobQuery namedQuery, Object ret) throws JPAExecutorException {
        WorkflowJobBean bean;
        Object[] arr;
        switch (namedQuery) {
            case GET_WORKFLOW:
                bean = (WorkflowJobBean) ret;
                break;
            case GET_WORKFLOW_STARTTIME:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[1]));
                break;
            case GET_WORKFLOW_USER_GROUP:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setUser((String) arr[0]);
                bean.setGroup((String) arr[1]);
                break;
            case GET_WORKFLOW_SUSPEND:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setUser((String) arr[1]);
                bean.setGroup((String) arr[2]);
                bean.setAppName((String) arr[3]);
                bean.setStatusStr((String) arr[4]);
                bean.setParentId((String) arr[5]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[6]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[7]));
                bean.setLogToken((String) arr[8]);
                bean.setWfInstanceBlob((BinaryBlob) (arr[9]));
                break;
            case GET_WORKFLOW_ACTION_OP:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setUser((String) arr[1]);
                bean.setGroup((String) arr[2]);
                bean.setAppName((String) arr[3]);
                bean.setAppPath((String) arr[4]);
                bean.setStatusStr((String) arr[5]);
                bean.setParentId((String) arr[6]);
                bean.setLogToken((String) arr[7]);
                bean.setWfInstanceBlob((BinaryBlob) (arr[8]));
                bean.setProtoActionConfBlob((StringBlob) arr[9]);
                break;
            case GET_WORKFLOW_RERUN:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setUser((String) arr[1]);
                bean.setGroup((String) arr[2]);
                bean.setAppName((String) arr[3]);
                bean.setStatusStr((String) arr[4]);
                bean.setRun((Integer) arr[5]);
                bean.setLogToken((String) arr[6]);
                bean.setWfInstanceBlob((BinaryBlob) (arr[7]));
                break;
            case GET_WORKFLOW_DEFINITION:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setUser((String) arr[1]);
                bean.setGroup((String) arr[2]);
                bean.setAppName((String) arr[3]);
                bean.setLogToken((String) arr[4]);
                bean.setWfInstanceBlob((BinaryBlob) (arr[5]));
                break;
            case GET_WORKFLOW_KILL:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setUser((String) arr[1]);
                bean.setGroup((String) arr[2]);
                bean.setAppName((String) arr[3]);
                bean.setAppPath((String) arr[4]);
                bean.setStatusStr((String) arr[5]);
                bean.setParentId((String) arr[6]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[7]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[8]));
                bean.setLogToken((String) arr[9]);
                bean.setWfInstanceBlob((BinaryBlob) (arr[10]));
                bean.setSlaXmlBlob((StringBlob) arr[11]);
                break;
            case GET_WORKFLOW_RESUME:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setUser((String) arr[1]);
                bean.setGroup((String) arr[2]);
                bean.setAppName((String) arr[3]);
                bean.setAppPath((String) arr[4]);
                bean.setStatusStr((String) arr[5]);
                bean.setParentId((String) arr[6]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[7]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[8]));
                bean.setLogToken((String) arr[9]);
                bean.setWfInstanceBlob((BinaryBlob) (arr[10]));
                bean.setProtoActionConfBlob((StringBlob) arr[11]);
                break;
            default:
                throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct job bean for "
                        + namedQuery.name());
        }
        return bean;
    }

     @Override
     public WorkflowJobBean get(WorkflowJobQuery namedQuery, Object... parameters) throws JPAExecutorException {
         EntityManager em = jpaService.getEntityManager();
         Query query = getSelectQuery(namedQuery, em, parameters);
        WorkflowJobBean bean = (WorkflowJobBean) jpaService.executeGet(namedQuery.name(), query, em);
        if (bean == null) {
        Object ret = jpaService.executeGet(namedQuery.name(), query, em);
        if (ret == null) {
             throw new JPAExecutorException(ErrorCode.E0604, query.toString());
         }
        WorkflowJobBean bean = constructBean(namedQuery, ret);
         return bean;
     }
 
@@ -186,10 +317,13 @@ public class WorkflowJobQueryExecutor extends QueryExecutor<WorkflowJobBean, Wor
     public List<WorkflowJobBean> getList(WorkflowJobQuery namedQuery, Object... parameters) throws JPAExecutorException {
         EntityManager em = jpaService.getEntityManager();
         Query query = getSelectQuery(namedQuery, em, parameters);
        List<WorkflowJobBean> beanList = (List<WorkflowJobBean>) jpaService
                .executeGetList(namedQuery.name(), query, em);
        if (beanList == null || beanList.size() == 0) {
            throw new JPAExecutorException(ErrorCode.E0604, query.toString());
        List<?> retList = (List<?>) jpaService.executeGetList(namedQuery.name(), query, em);
        List<WorkflowJobBean> beanList = null;
        if (retList != null) {
            beanList = new ArrayList<WorkflowJobBean>();
            for (Object ret : retList) {
                beanList.add(constructBean(namedQuery, ret));
            }
         }
         return beanList;
     }
diff --git a/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java b/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java
index cf11a36c6..9cbc22305 100644
-- a/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java
++ b/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java
@@ -21,7 +21,6 @@ import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.command.CommandException;
@@ -29,7 +28,8 @@ import org.apache.oozie.command.coord.CoordActionCheckXCommand;
 import org.apache.oozie.command.wf.ActionCheckXCommand;
 import org.apache.oozie.executor.jpa.CoordActionsRunningGetJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowActionsRunningGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.util.XCallable;
 import org.apache.oozie.util.XLog;
 
@@ -111,8 +111,8 @@ public class ActionCheckerService implements Service {
 
             List<WorkflowActionBean> actions;
             try {
                actions = jpaService
                        .execute(new WorkflowActionsRunningGetJPAExecutor(actionCheckDelay));
                actions = WorkflowActionQueryExecutor.getInstance().getList(WorkflowActionQuery.GET_RUNNING_ACTIONS,
                        actionCheckDelay);
             }
             catch (JPAExecutorException je) {
                 throw new CommandException(je);
diff --git a/core/src/main/java/org/apache/oozie/service/AuthorizationService.java b/core/src/main/java/org/apache/oozie/service/AuthorizationService.java
index e132dbca1..9e6b76f26 100644
-- a/core/src/main/java/org/apache/oozie/service/AuthorizationService.java
++ b/core/src/main/java/org/apache/oozie/service/AuthorizationService.java
@@ -38,7 +38,8 @@ import org.apache.oozie.client.XOozieClient;
 import org.apache.oozie.executor.jpa.BundleJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.util.ConfigUtils;
 import org.apache.oozie.util.Instrumentation;
 import org.apache.oozie.util.XLog;
@@ -433,7 +434,7 @@ public class AuthorizationService implements Service {
                     JPAService jpaService = Services.get().get(JPAService.class);
                     if (jpaService != null) {
                         try {
                            jobBean = jpaService.execute(new WorkflowJobGetJPAExecutor(jobId));
                            jobBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_USER_GROUP, jobId);
                         }
                         catch (JPAExecutorException je) {
                             throw new AuthorizationException(je);
diff --git a/core/src/main/java/org/apache/oozie/service/JPAService.java b/core/src/main/java/org/apache/oozie/service/JPAService.java
index 9f3f14828..0e944a692 100644
-- a/core/src/main/java/org/apache/oozie/service/JPAService.java
++ b/core/src/main/java/org/apache/oozie/service/JPAService.java
@@ -439,7 +439,7 @@ public class JPAService implements Service, Instrumentable {
      * @return list containing results that match the query
      * @throws JPAExecutorException
      */
    public List executeGetList(String namedQueryName, Query query, EntityManager em) throws JPAExecutorException {
    public List<?> executeGetList(String namedQueryName, Query query, EntityManager em) throws JPAExecutorException {
         Instrumentation.Cron cron = new Instrumentation.Cron();
         try {
 
@@ -450,7 +450,7 @@ public class JPAService implements Service, Instrumentable {
 
             cron.start();
             em.getTransaction().begin();
            List resultList = null;
            List<?> resultList = null;
             try {
                 resultList = query.getResultList();
             }
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java
index bc819f10e..fa64b3257 100644
-- a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java
@@ -17,6 +17,8 @@
  */
 package org.apache.oozie.executor.jpa;
 
import java.util.List;

 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 import org.apache.oozie.WorkflowActionBean;
@@ -44,20 +46,11 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
 
     @Override
     protected void tearDown() throws Exception {
        System.out.println("Debug: In teardown");
        new Throwable().printStackTrace();
        try {
            services.destroy();
            super.tearDown();
        }
        catch (Exception e) {
            System.out.println("Debug: exception In teardown");
            e.printStackTrace();
            throw e;
        }
        services.destroy();
        super.tearDown();
     }
 
    public void testGetQuery() throws Exception {
    public void testGetUpdateQuery() throws Exception {
         EntityManager em = jpaService.getEntityManager();
         WorkflowJobBean job = this.addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
         WorkflowActionBean bean = addRecordToWfActionTable(job.getId(), "1", WorkflowAction.Status.PREP);
@@ -102,24 +95,28 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
         query = WorkflowActionQueryExecutor.getInstance().getUpdateQuery(WorkflowActionQuery.UPDATE_ACTION_PENDING,
                 bean, em);
         assertEquals(query.getParameterValue("pending"), bean.getPending());
        assertEquals(query.getParameterValue("pendingAge"), bean.getPendingAgeTimestamp());
         assertEquals(query.getParameterValue("id"), bean.getId());
 
         // UPDATE_ACTION_STATUS_PENDING
         query = WorkflowActionQueryExecutor.getInstance().getUpdateQuery(
                 WorkflowActionQuery.UPDATE_ACTION_STATUS_PENDING, bean, em);
         assertEquals(query.getParameterValue("pending"), bean.getPending());
        assertEquals(query.getParameterValue("pendingAge"), bean.getPendingAgeTimestamp());
         assertEquals(query.getParameterValue("status"), bean.getStatus().toString());
         assertEquals(query.getParameterValue("id"), bean.getId());
         // UPDATE_ACTION_PENDING_TRANS
         query = WorkflowActionQueryExecutor.getInstance().getUpdateQuery(
                 WorkflowActionQuery.UPDATE_ACTION_PENDING_TRANS, bean, em);
         assertEquals(query.getParameterValue("pending"), bean.getPending());
        assertEquals(query.getParameterValue("pendingAge"), bean.getPendingAgeTimestamp());
         assertEquals(query.getParameterValue("transition"), bean.getTransition());
         assertEquals(query.getParameterValue("id"), bean.getId());
         // UPDATE_ACTION_PENDING_TRANS_ERROR
         query = WorkflowActionQueryExecutor.getInstance().getUpdateQuery(
                 WorkflowActionQuery.UPDATE_ACTION_PENDING_TRANS_ERROR, bean, em);
         assertEquals(query.getParameterValue("pending"), bean.getPending());
        assertEquals(query.getParameterValue("pendingAge"), bean.getPendingAgeTimestamp());
         assertEquals(query.getParameterValue("transition"), bean.getTransition());
         assertEquals(query.getParameterValue("errorCode"), bean.getErrorCode());
         assertEquals(query.getParameterValue("errorMessage"), bean.getErrorMessage());
@@ -169,6 +166,7 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
         assertEquals(query.getParameterValue("retries"), bean.getRetries());
         assertEquals(query.getParameterValue("endTime"), bean.getEndTimestamp());
         assertEquals(query.getParameterValue("status"), bean.getStatus().toString());
        assertEquals(query.getParameterValue("retries"), bean.getRetries());
         assertEquals(query.getParameterValue("pending"), bean.getPending());
         assertEquals(query.getParameterValue("pendingAge"), bean.getPendingAgeTimestamp());
         assertEquals(query.getParameterValue("signalValue"), bean.getSignalValue());
@@ -184,18 +182,186 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
         WorkflowJobBean job = this.addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
         WorkflowActionBean bean = addRecordToWfActionTable(job.getId(), "1", WorkflowAction.Status.PREP);
         bean.setStatus(WorkflowAction.Status.RUNNING);
        bean.setName("test-name");
         WorkflowActionQueryExecutor.getInstance().executeUpdate(WorkflowActionQuery.UPDATE_ACTION, bean);
         WorkflowActionBean retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION,
                 bean.getId());
        assertEquals("test-name", retBean.getName());
         assertEquals(retBean.getStatus(), WorkflowAction.Status.RUNNING);
     }
 
     public void testGet() throws Exception {
        // TODO
        WorkflowActionBean bean = addRecordToWfActionTable("workflowId","testAction", WorkflowAction.Status.PREP);
        WorkflowActionBean retBean;

        //GET_WORKFFLOW_ID_TYPE
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_ID_TYPE, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getType(), retBean.getType());

        //GET_WORKFFLOW_FAIL
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_FAIL, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getJobId(), retBean.getJobId());
        assertEquals(bean.getName(), retBean.getName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(bean.getTransition(), retBean.getTransition());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
        assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
        assertNull(retBean.getConf());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getData());
        assertNull(retBean.getStats());
        assertNull(retBean.getExternalChildIDs());

        //GET_WORKFFLOW_SIGNAL
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_SIGNAL, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getJobId(), retBean.getJobId());
        assertEquals(bean.getName(), retBean.getName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(bean.getTransition(), retBean.getTransition());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
        assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
        assertEquals(bean.getExecutionPath(), retBean.getExecutionPath());
        assertEquals(bean.getSignalValue(), retBean.getSignalValue());
        assertEquals(bean.getSlaXml(), retBean.getSlaXml());
        assertNull(retBean.getConf());
        assertNull(retBean.getData());
        assertNull(retBean.getStats());
        assertNull(retBean.getExternalChildIDs());

        // GET_WORKFLOW_START
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_START, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getJobId(), retBean.getJobId());
        assertEquals(bean.getName(), retBean.getName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(bean.getTransition(), retBean.getTransition());
        assertEquals(bean.getUserRetryCount(), retBean.getUserRetryCount());
        assertEquals(bean.getUserRetryMax(), retBean.getUserRetryMax());
        assertEquals(bean.getUserRetryInterval(), retBean.getUserRetryInterval());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
        assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
        assertEquals(bean.getCred(), retBean.getCred());
        assertEquals(bean.getConf(), retBean.getConf());
        assertEquals(bean.getSlaXml(), retBean.getSlaXml());
        assertNull(retBean.getData());
        assertNull(retBean.getStats());
        assertNull(retBean.getExternalChildIDs());

        // GET_WORKFLOW_CHECK
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_CHECK, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getJobId(), retBean.getJobId());
        assertEquals(bean.getName(), retBean.getName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(bean.getTransition(), retBean.getTransition());
        assertEquals(bean.getRetries(), retBean.getRetries());
        assertEquals(bean.getTrackerUri(), retBean.getTrackerUri());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertEquals(bean.getLastCheckTime().getTime(), retBean.getLastCheckTime().getTime());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
        assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
        assertEquals(bean.getExternalId(), retBean.getExternalId());
        assertEquals(bean.getExternalStatus(), retBean.getExternalStatus());
        assertEquals(bean.getExternalChildIDs(), retBean.getExternalChildIDs());
        assertEquals(bean.getConf(), retBean.getConf());
        assertNull(retBean.getData());
        assertNull(retBean.getStats());
        assertNull(retBean.getSlaXml());

        // GET_WORKFLOW_END
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_END, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getJobId(), retBean.getJobId());
        assertEquals(bean.getName(), retBean.getName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(bean.getTransition(), retBean.getTransition());
        assertEquals(bean.getRetries(), retBean.getRetries());
        assertEquals(bean.getTrackerUri(), retBean.getTrackerUri());
        assertEquals(bean.getUserRetryCount(), retBean.getUserRetryCount());
        assertEquals(bean.getUserRetryMax(), retBean.getUserRetryMax());
        assertEquals(bean.getUserRetryInterval(), retBean.getUserRetryInterval());
        assertEquals(bean.getExternalId(), retBean.getExternalId());
        assertEquals(bean.getExternalStatus(), retBean.getExternalStatus());
        assertEquals(bean.getExternalChildIDs(), retBean.getExternalChildIDs());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
        assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
        assertEquals(bean.getConf(), retBean.getConf());
        assertEquals(bean.getData(), retBean.getData());
        assertEquals(bean.getStats(), retBean.getStats());
        assertNull(retBean.getSlaXml());

        // GET_WORKFLOW_KILL
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_KILL, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getJobId(), retBean.getJobId());
        assertEquals(bean.getName(), retBean.getName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(bean.getTransition(), retBean.getTransition());
        assertEquals(bean.getRetries(), retBean.getRetries());
        assertEquals(bean.getTrackerUri(), retBean.getTrackerUri());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
        assertEquals(bean.getExternalId(), retBean.getExternalId());
        assertEquals(bean.getConf(), retBean.getConf());
        assertEquals(bean.getData(), retBean.getData());
        assertNull(retBean.getExternalChildIDs());
        assertNull(retBean.getStats());
        assertNull(retBean.getSlaXml());

        //GET_WORKFLOW_COMPLETED
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_COMPLETED, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getJobId(), retBean.getJobId());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getConf());
        assertNull(retBean.getData());
        assertNull(retBean.getStats());
        assertNull(retBean.getExternalChildIDs());
     }
 
     public void testGetList() throws Exception {
        // TODO
      //GET_RUNNING_ACTIONS
        addRecordToWfActionTable("wrkflow","1", WorkflowAction.Status.RUNNING, true);
        addRecordToWfActionTable("wrkflow","2", WorkflowAction.Status.RUNNING, true);
        addRecordToWfActionTable("wrkflow","3", WorkflowAction.Status.RUNNING, true);
        List<WorkflowActionBean> retList = WorkflowActionQueryExecutor.getInstance().getList(
                WorkflowActionQuery.GET_RUNNING_ACTIONS, 0);
        assertEquals(3, retList.size());
        for(WorkflowActionBean bean : retList){
            assertTrue(bean.getId().equals("wrkflow@1") || bean.getId().equals("wrkflow@2") || bean.getId().equals("wrkflow@3"));
        }

     }
 
     public void testInsert() throws Exception {
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionsRunningGetJPAExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionsRunningGetJPAExecutor.java
index c7ec84f26..e69de29bb 100644
-- a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionsRunningGetJPAExecutor.java
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionsRunningGetJPAExecutor.java
@@ -1,83 +0,0 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.executor.jpa;

import java.util.List;

import org.apache.oozie.WorkflowActionBean;
import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.client.WorkflowJob;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.workflow.WorkflowInstance;

public class TestWorkflowActionsRunningGetJPAExecutor extends XDataTestCase {
    Services services;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        services.init();
        cleanUpDBTables();
    }

    @Override
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
    }

    public void testWfActionsRunningGet() throws Exception {
        WorkflowJobBean job = addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
        addRecordToWfActionTable(job.getId(), "1", WorkflowAction.Status.OK);
        addRecordToWfActionTableWithRunningStatus(job.getId(), "2", WorkflowAction.Status.RUNNING);
        sleep(2000);
        _testGetRunningActions(1);
    }

    private void _testGetRunningActions(long checkAgeSecs) throws Exception {
        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);
        WorkflowActionsRunningGetJPAExecutor runningActionsGetExe = new WorkflowActionsRunningGetJPAExecutor(checkAgeSecs);
        List<WorkflowActionBean> list = jpaService.execute(runningActionsGetExe);
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    protected WorkflowActionBean addRecordToWfActionTableWithRunningStatus(String wfId, String actionName, WorkflowAction.Status status) throws Exception {
        WorkflowActionBean action = createWorkflowAction(wfId, actionName, status);
        action.setPending();
        try {
            JPAService jpaService = Services.get().get(JPAService.class);
            assertNotNull(jpaService);
            WorkflowActionInsertJPAExecutor actionInsertCmd = new WorkflowActionInsertJPAExecutor(action);
            jpaService.execute(actionInsertCmd);
        }
        catch (JPAExecutorException je) {
            je.printStackTrace();
            fail("Unable to insert the test wf action record to table");
            throw je;
        }
        return action;
    }


}
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java
index a26de28a5..6e7231987 100644
-- a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java
@@ -17,6 +17,9 @@
  */
 package org.apache.oozie.executor.jpa;
 
import java.nio.ByteBuffer;
import java.util.Date;

 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
@@ -47,7 +50,7 @@ public class TestWorkflowJobQueryExecutor extends XDataTestCase {
         super.tearDown();
     }
 
    public void testGetQuery() throws Exception {
    public void testGetUpdateQuery() throws Exception {
         EntityManager em = jpaService.getEntityManager();
         WorkflowJobBean bean = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
 
@@ -143,53 +146,162 @@ public class TestWorkflowJobQueryExecutor extends XDataTestCase {
         assertEquals(query.getParameterValue("wfInstance"), bean.getWfInstanceBlob());
         assertEquals(query.getParameterValue("lastModTime"), bean.getLastModifiedTimestamp());
         assertEquals(query.getParameterValue("id"), bean.getId());
    }

    public void testExecuteUpdate() throws Exception {

        WorkflowJobBean bean = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
        bean.setStatus(WorkflowJob.Status.RUNNING);
        WorkflowJobQueryExecutor.getInstance().executeUpdate(WorkflowJobQuery.UPDATE_WORKFLOW, bean);
        WorkflowJobBean bean2 = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW, bean.getId());
        assertEquals(bean2.getStatus(), WorkflowJob.Status.RUNNING);
    }

    public void testGetSelectQuery() throws Exception {
        EntityManager em = jpaService.getEntityManager();
        WorkflowJobBean bean = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
 
         // GET_WORKFLOW
        query = WorkflowJobQueryExecutor.getInstance().getSelectQuery(WorkflowJobQuery.GET_WORKFLOW, em, bean.getId());
        Query query = WorkflowJobQueryExecutor.getInstance().getSelectQuery(WorkflowJobQuery.GET_WORKFLOW, em, bean.getId());
         assertEquals(query.getParameterValue("id"), bean.getId());
    }
 
    public void testExecuteUpdate() throws Throwable {

        try {
            WorkflowJobBean bean = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
            bean.setStatus(WorkflowJob.Status.RUNNING);
            WorkflowJobQueryExecutor.getInstance().executeUpdate(WorkflowJobQuery.UPDATE_WORKFLOW, bean);
            WorkflowJobBean bean2 = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW,
                    bean.getId());
            assertEquals(bean2.getStatus(), WorkflowJob.Status.RUNNING);
        }
        catch (Throwable e) {
            // TODO Auto-generated catch block
            System.out.println("Debug: encountered exception");
            e.printStackTrace();
            throw e;
        }
        // GET_WORKFLOW_SUSPEND
        query = WorkflowJobQueryExecutor.getInstance().getSelectQuery(WorkflowJobQuery.GET_WORKFLOW_SUSPEND, em, bean.getId());
        assertEquals(query.getParameterValue("id"), bean.getId());
     }
 
    public void testInsert() throws Throwable {
        try {
            WorkflowJobBean bean = new WorkflowJobBean();
            bean.setId("test-oozie-wrk");
            bean.setAppName("test");
            bean.setUser("oozie");
            WorkflowJobQueryExecutor.getInstance().insert(bean);
            WorkflowJobBean retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW,
                    "test-oozie-wrk");
            assertNotNull(retBean);
            assertEquals(retBean.getAppName(), "test");
            assertEquals(retBean.getUser(), "oozie");
        }
        catch (Throwable e) {
            System.out.println("Debug: encountered exception testinsert");
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw e;
        }
    public void testInsert() throws Exception {
        WorkflowJobBean bean = new WorkflowJobBean();
        bean.setId("test-oozie-wrk");
        bean.setAppName("test");
        bean.setUser("oozie");
        WorkflowJobQueryExecutor.getInstance().insert(bean);
        WorkflowJobBean retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW,
                "test-oozie-wrk");
        assertNotNull(retBean);
        assertEquals(retBean.getAppName(), "test");
        assertEquals(retBean.getUser(), "oozie");
     }
 
     public void testGet() throws Exception {
        // TODO
        WorkflowJobBean bean = addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
        bean.setStartTime(new Date(System.currentTimeMillis() - 10));
        bean.setEndTime(new Date());
        WorkflowJobQueryExecutor.getInstance().executeUpdate(WorkflowJobQuery.UPDATE_WORKFLOW, bean);
        WorkflowJobBean retBean;
        // GET_WORKFLOW_STARTTIME
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_STARTTIME, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertNull(retBean.getWorkflowInstance());
        assertNull(retBean.getProtoActionConf());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getConf());
        // GET_WORKFLOW_USER_GROUP
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_USER_GROUP, bean.getId());
        assertEquals(bean.getUser(), retBean.getUser());
        assertEquals(bean.getGroup(), retBean.getGroup());
        assertNull(retBean.getWorkflowInstance());
        assertNull(retBean.getProtoActionConf());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getConf());

        // GET_WORKFLOW_SUSPEND
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_SUSPEND, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getUser(), retBean.getUser());
        assertEquals(bean.getGroup(), retBean.getGroup());
        assertEquals(bean.getAppName(), retBean.getAppName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getParentId(), retBean.getParentId());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(ByteBuffer.wrap(bean.getWfInstanceBlob().getBytes()).getInt(),
                ByteBuffer.wrap(retBean.getWfInstanceBlob().getBytes()).getInt());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertNull(retBean.getProtoActionConf());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getConf());

        // GET_WORKFLOW_ACTION_OP
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_ACTION_OP, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getUser(), retBean.getUser());
        assertEquals(bean.getGroup(), retBean.getGroup());
        assertEquals(bean.getAppName(), retBean.getAppName());
        assertEquals(bean.getAppPath(), retBean.getAppPath());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getParentId(), retBean.getParentId());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(ByteBuffer.wrap(bean.getWfInstanceBlob().getBytes()).getInt(),
                ByteBuffer.wrap(retBean.getWfInstanceBlob().getBytes()).getInt());
        assertEquals(bean.getProtoActionConf(), retBean.getProtoActionConf());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getConf());

        //GET_WORKFLOW_RERUN
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_RERUN, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getUser(), retBean.getUser());
        assertEquals(bean.getGroup(), retBean.getGroup());
        assertEquals(bean.getAppName(), retBean.getAppName());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getRun(), retBean.getRun());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(ByteBuffer.wrap(bean.getWfInstanceBlob().getBytes()).getInt(),
                ByteBuffer.wrap(retBean.getWfInstanceBlob().getBytes()).getInt());
        assertNull(retBean.getProtoActionConf());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getConf());

        //GET_WORKFLOW_DEFINITION
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_DEFINITION, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getUser(), retBean.getUser());
        assertEquals(bean.getGroup(), retBean.getGroup());
        assertEquals(bean.getAppName(), retBean.getAppName());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(ByteBuffer.wrap(bean.getWfInstanceBlob().getBytes()).getInt(),
                ByteBuffer.wrap(retBean.getWfInstanceBlob().getBytes()).getInt());
        assertNull(retBean.getProtoActionConf());
        assertNull(retBean.getSlaXml());
        assertNull(retBean.getConf());

        // GET_WORKFLOW_KILL
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_KILL, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getUser(), retBean.getUser());
        assertEquals(bean.getGroup(), retBean.getGroup());
        assertEquals(bean.getAppName(), retBean.getAppName());
        assertEquals(bean.getAppPath(), retBean.getAppPath());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getParentId(), retBean.getParentId());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(ByteBuffer.wrap(bean.getWfInstanceBlob().getBytes()).getInt(),
                ByteBuffer.wrap(retBean.getWfInstanceBlob().getBytes()).getInt());
        assertEquals(bean.getSlaXml(), retBean.getSlaXml());
        assertNull(retBean.getProtoActionConf());
        assertNull(retBean.getConf());

        // GET_WORKFLOW_RESUME
        retBean = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_RESUME, bean.getId());
        assertEquals(bean.getId(), retBean.getId());
        assertEquals(bean.getUser(), retBean.getUser());
        assertEquals(bean.getGroup(), retBean.getGroup());
        assertEquals(bean.getAppName(), retBean.getAppName());
        assertEquals(bean.getAppPath(), retBean.getAppPath());
        assertEquals(bean.getStatusStr(), retBean.getStatusStr());
        assertEquals(bean.getParentId(), retBean.getParentId());
        assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
        assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertEquals(bean.getLogToken(), retBean.getLogToken());
        assertEquals(ByteBuffer.wrap(bean.getWfInstanceBlob().getBytes()).getInt(),
                ByteBuffer.wrap(retBean.getWfInstanceBlob().getBytes()).getInt());
        assertEquals(bean.getProtoActionConf(), retBean.getProtoActionConf());
        assertNull(retBean.getConf());
        assertNull(retBean.getSlaXml());
     }
 
     public void testGetList() throws Exception {
diff --git a/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java b/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java
index 9cdde7d58..01e7e835a 100644
-- a/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java
++ b/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java
@@ -17,12 +17,10 @@
  */
 package org.apache.oozie.sla;
 
import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.HashSet;
import java.util.List;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.Path;
@@ -30,16 +28,13 @@ import org.apache.oozie.AppType;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.CoordinatorJob;
 import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.WorkflowJob;
 import org.apache.oozie.client.event.Event;
 import org.apache.oozie.client.event.JobEvent;
import org.apache.oozie.client.event.SLAEvent;
 import org.apache.oozie.client.event.SLAEvent.SLAStatus;
 import org.apache.oozie.client.event.SLAEvent.EventStatus;
 import org.apache.oozie.client.rest.RestConstants;
@@ -55,13 +50,9 @@ import org.apache.oozie.command.wf.StartXCommand;
 import org.apache.oozie.command.wf.SubmitXCommand;
 import org.apache.oozie.event.CoordinatorActionEvent;
 import org.apache.oozie.event.listener.JobEventListener;
import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.CoordActionGetJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
import org.apache.oozie.executor.jpa.BatchQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
import org.apache.oozie.executor.jpa.WorkflowActionsGetForJobJPAExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobInsertJPAExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
@@ -70,7 +61,6 @@ import org.apache.oozie.executor.jpa.sla.SLASummaryGetJPAExecutor;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.service.EventHandlerService.EventWorker;
 import org.apache.oozie.sla.listener.SLAJobEventListener;
 import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.test.XDataTestCase;
diff --git a/core/src/test/java/org/apache/oozie/test/XDataTestCase.java b/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
index 8a796f3b2..b0eaee279 100644
-- a/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
++ b/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
@@ -25,6 +25,7 @@ import java.io.OutputStreamWriter;
 import java.io.Reader;
 import java.io.UnsupportedEncodingException;
 import java.io.Writer;
import java.text.SimpleDateFormat;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.regex.Matcher;
@@ -1170,6 +1171,7 @@ public abstract class XDataTestCase extends XHCatTestCase {
         workflow.setUser(conf.get(OozieClient.USER_NAME));
         workflow.setGroup(conf.get(OozieClient.GROUP_NAME));
         workflow.setWorkflowInstance(wfInstance);
        workflow.setSlaXml("<sla></sla>");
         return workflow;
     }
 
@@ -1221,6 +1223,17 @@ public abstract class XDataTestCase extends XHCatTestCase {
                 + "<property><name>mapred.output.dir</name><value>" + outputDir.toString() + "</value></property>"
                 + "</configuration>" + "</map-reduce>";
         action.setConf(actionXml);
        action.setSlaXml("<sla></sla>");
        action.setData("dummy data");
        action.setStats("dummy stats");
        action.setExternalChildIDs("00000001-dummy-oozie-wrkf-W");
        action.setRetries(2);
        action.setUserRetryCount(1);
        action.setUserRetryMax(2);
        action.setUserRetryInterval(1);
        action.setErrorInfo("dummyErrorCode", "dummyErrorMessage");
        action.setExternalId("dummy external id");
        action.setExternalStatus("RUNNING");
 
         return action;
     }
diff --git a/release-log.txt b/release-log.txt
index 9e614d6d3..7a363d477 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1524 Change Workflow SELECT query to fetch only necessary columns and consolidate JPA Executors (ryota)
 OOZIE-1515 Passing superset of action id range should be allowed (mona)
 OOZIE-1530 Fork-join mismatch makes workflow Failed but some actions stay Running (mona)
 OOZIE-1539 Load more coordinator jobs eligible to be materialized in MaterializeTriggerService (mona)
- 
2.19.1.windows.1

