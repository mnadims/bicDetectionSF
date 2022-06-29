From 5940a3764ca9cb1f95a7378713e77f38742d9ddd Mon Sep 17 00:00:00 2001
From: mona <mona@unknown>
Date: Thu, 17 Oct 2013 18:23:00 +0000
Subject: [PATCH] OOZIE-1559 Fix missing fields from new SELECT queries and
 Recovery Service picking up killed control nodes (ryota,mona via mona)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1533196 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/oozie/CoordinatorJobBean.java  |   2 +-
 .../org/apache/oozie/WorkflowActionBean.java  |  12 +-
 .../coord/CoordActionInputCheckXCommand.java  |   1 -
 .../CoordMaterializeTransitionXCommand.java   |   4 +-
 .../oozie/command/wf/ActionEndXCommand.java   |   2 +-
 .../oozie/command/wf/ActionKillXCommand.java  |   2 +-
 .../apache/oozie/command/wf/KillXCommand.java |   4 +-
 .../executor/jpa/CoordJobQueryExecutor.java   |   1 +
 .../jpa/WorkflowActionQueryExecutor.java      | 113 ++++++------------
 .../oozie/event/TestEventGeneration.java      |   9 +-
 .../jpa/TestWorkflowActionQueryExecutor.java  |  81 ++++++-------
 release-log.txt                               |   1 +
 12 files changed, 90 insertions(+), 142 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java b/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
index 9923478ea..f189b69b4 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
@@ -91,7 +91,7 @@ import org.json.simple.JSONObject;
 
         @NamedQuery(name = "GET_COORD_JOB_ACTION_KILL", query = "select w.id, w.user, w.group, w.appName, w.statusStr from CoordinatorJobBean w where w.id = :id"),
 
        @NamedQuery(name = "GET_COORD_JOB_MATERIALIZE", query = "select w.id, w.user, w.group, w.appName, w.statusStr, w.frequency, w.matThrottling, w.timeOut, w.timeZone, w.startTimestamp, w.endTimestamp, w.pauseTimestamp, w.nextMaterializedTimestamp, w.lastActionTimestamp, w.lastActionNumber, w.doneMaterialization, w.bundleId, w.conf, w.jobXml from CoordinatorJobBean w where w.id = :id"),
        @NamedQuery(name = "GET_COORD_JOB_MATERIALIZE", query = "select w.id, w.user, w.group, w.appName, w.statusStr, w.frequency, w.matThrottling, w.timeOut, w.timeZone, w.startTimestamp, w.endTimestamp, w.pauseTimestamp, w.nextMaterializedTimestamp, w.lastActionTimestamp, w.lastActionNumber, w.doneMaterialization, w.bundleId, w.conf, w.jobXml, w.appNamespace from CoordinatorJobBean w where w.id = :id"),
 
         @NamedQuery(name = "GET_COORD_JOB_SUSPEND_KILL", query = "select w.id, w.user, w.group, w.appName, w.statusStr, w.bundleId, w.appNamespace, w.doneMaterialization from CoordinatorJobBean w where w.id = :id"),
 
diff --git a/core/src/main/java/org/apache/oozie/WorkflowActionBean.java b/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
index 17ed5c7eb..2b1f0448f 100644
-- a/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
++ b/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
@@ -65,7 +65,7 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "UPDATE_ACTION_CHECK", query = "update WorkflowActionBean a set a.userRetryCount = :userRetryCount, a.stats = :stats, a.externalChildIDs = :externalChildIDs, a.externalStatus = :externalStatus, a.statusStr = :status, a.data = :data, a.pending = :pending, a.errorCode = :errorCode, a.errorMessage = :errorMessage, a.lastCheckTimestamp = :lastCheckTime, a.retries = :retries, a.pendingAgeTimestamp = :pendingAge, a.startTimestamp = :startTime where a.id = :id"),
 
    @NamedQuery(name = "UPDATE_ACTION_END", query = "update WorkflowActionBean a set a.stats = :stats, a.errorCode = :errorCode, a.errorMessage = :errorMessage, a.retries = :retries, a.endTimestamp = :endTime, a.statusStr = :status, a.retries = :retries, a.pending = :pending, a.pendingAgeTimestamp = :pendingAge, a.signalValue = :signalValue, a.userRetryCount = :userRetryCount, a.externalStatus = :externalStatus where a.id = :id"),
    @NamedQuery(name = "UPDATE_ACTION_END", query = "update WorkflowActionBean a set a.stats = :stats, a.errorCode = :errorCode, a.errorMessage = :errorMessage, a.retries = :retries, a.endTimestamp = :endTime, a.statusStr = :status, a.pending = :pending, a.pendingAgeTimestamp = :pendingAge, a.signalValue = :signalValue, a.userRetryCount = :userRetryCount, a.externalStatus = :externalStatus where a.id = :id"),
 
     @NamedQuery(name = "UPDATE_ACTION_PENDING", query = "update WorkflowActionBean a set a.pending = :pending, a.pendingAgeTimestamp = :pendingAge where a.id = :id"),
 
@@ -87,15 +87,11 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "GET_ACTION_FAIL", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.errorCode, a.errorMessage from WorkflowActionBean a where a.id = :id"),
 
    @NamedQuery(name = "GET_ACTION_SIGNAL", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.errorCode, a.errorMessage, a.executionPath, a.signalValue, a.slaXml from WorkflowActionBean a where a.id = :id"),
    @NamedQuery(name = "GET_ACTION_SIGNAL", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.pendingAgeTimestamp, a.type, a.logToken, a.transition, a.errorCode, a.errorMessage, a.executionPath, a.signalValue, a.slaXml from WorkflowActionBean a where a.id = :id"),
 
    @NamedQuery(name = "GET_ACTION_START", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.userRetryCount, a.userRetryMax, a.userRetryInterval, a.startTimestamp, a.endTimestamp, a.errorCode, a.errorMessage, a.cred, a.conf, a.slaXml from WorkflowActionBean a where a.id = :id"),
    @NamedQuery(name = "GET_ACTION_START", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.pendingAgeTimestamp, a.type, a.logToken, a.transition, a.retries, a.userRetryCount, a.userRetryMax, a.userRetryInterval, a.startTimestamp, a.endTimestamp, a.errorCode, a.errorMessage, a.cred, a.externalId, a.externalStatus, a.conf, a.slaXml from WorkflowActionBean a where a.id = :id"),
 
    @NamedQuery(name = "GET_ACTION_CHECK", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.trackerUri, a.startTimestamp, a.endTimestamp, a.lastCheckTimestamp, a.errorCode, a.errorMessage, a.externalId, a.externalStatus, a.externalChildIDs, a.conf from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_END", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.trackerUri, a.userRetryCount, a.userRetryMax, a.userRetryInterval, a.startTimestamp, a.endTimestamp, a.errorCode, a.errorMessage, a.externalId, a.externalStatus, a.externalChildIDs, a.conf, a.data, a.stats from WorkflowActionBean a where a.id = :id"),

    @NamedQuery(name = "GET_ACTION_KILL", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.type, a.logToken, a.transition, a.retries, a.trackerUri, a.startTimestamp, a.endTimestamp, a.errorCode, a.errorMessage, a.externalId, a.conf, a.data from WorkflowActionBean a where a.id = :id"),
    @NamedQuery(name = "GET_ACTION_CHECK", query = "select a.id, a.wfId, a.name, a.statusStr, a.pending, a.pendingAgeTimestamp, a.type, a.logToken, a.transition, a.retries, a.userRetryCount, a.userRetryMax, a.userRetryInterval, a.trackerUri, a.startTimestamp, a.endTimestamp, a.lastCheckTimestamp, a.errorCode, a.errorMessage, a.externalId, a.externalStatus, a.externalChildIDs, a.conf from WorkflowActionBean a where a.id = :id"),
 
     @NamedQuery(name = "GET_ACTION_COMPLETED", query = "select a.id, a.wfId, a.statusStr, a.type, a.logToken from WorkflowActionBean a where a.id = :id"),
 
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
index ddc3768c2..7ec84be88 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
@@ -40,7 +40,6 @@ import org.apache.oozie.dependency.URIHandlerException;
 import org.apache.oozie.executor.jpa.CoordActionGetForInputCheckJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
index d9db20179..af9fe8796 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
@@ -455,12 +455,12 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
         job.setPending();
 
         if (jobEndTime.compareTo(endMatdTime) <= 0) {
            LOG.info("[" + job.getId() + "]: all actions have been materialized, job status = " + job.getStatus()
                    + ", set pending to true");
            LOG.info("[" + job.getId() + "]: all actions have been materialized, set pending to true");
             // set doneMaterialization to true when materialization is done
             job.setDoneMaterialization();
         }
         job.setStatus(StatusUtils.getStatus(job));
        LOG.info("Coord Job status updated to = " + job.getStatus());
         job.setNextMaterializedTime(endMatdTime);
     }
 
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java
index fb9dec3e8..ac2408f71 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionEndXCommand.java
@@ -99,7 +99,7 @@ public class ActionEndXCommand extends ActionXCommand<Void> {
             if (jpaService != null) {
                 this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_ACTION_OP,
                         jobId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_END, actionId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION, actionId);
                 LogUtils.setLogInfo(wfJob, logInfo);
                 LogUtils.setLogInfo(wfAction, logInfo);
             }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
index d4d01f3ca..863bf7d24 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
@@ -96,7 +96,7 @@ public class ActionKillXCommand extends ActionXCommand<Void> {
 
             if (jpaService != null) {
                 this.wfJob = WorkflowJobQueryExecutor.getInstance().get(WorkflowJobQuery.GET_WORKFLOW_ACTION_OP, jobId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_KILL, actionId);
                this.wfAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION, actionId);
                 LogUtils.setLogInfo(wfJob, logInfo);
                 LogUtils.setLogInfo(wfAction, logInfo);
             }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java
index 7fb968cd6..37a2f8b64 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/KillXCommand.java
@@ -142,7 +142,9 @@ public class KillXCommand extends WorkflowXCommand<Void> {
             for (WorkflowActionBean action : actionList) {
                 if (action.getStatus() == WorkflowActionBean.Status.RUNNING
                         || action.getStatus() == WorkflowActionBean.Status.DONE) {
                    action.setPending();
                    if (!(actionService.getExecutor(action.getType()) instanceof ControlNodeActionExecutor)) {
                        action.setPending();
                    }
                     action.setStatus(WorkflowActionBean.Status.KILLED);
                     updateList.add(new UpdateEntry<WorkflowActionQuery>(WorkflowActionQuery.UPDATE_ACTION_STATUS_PENDING, action));
 
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
index eff54ea47..8e9743678 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
@@ -279,6 +279,7 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
                 bean.setBundleId((String) arr[16]);
                 bean.setConfBlob((StringBlob) arr[17]);
                 bean.setJobXmlBlob((StringBlob) arr[18]);
                bean.setAppNamespace((String) arr[19]);
                 break;
             case GET_COORD_JOB_SUSPEND_KILL:
                 bean = new CoordinatorJobBean();
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
index d0d7677da..d95ff08a3 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
@@ -54,8 +54,6 @@ public class WorkflowActionQueryExecutor extends
         GET_ACTION_SIGNAL,
         GET_ACTION_START,
         GET_ACTION_CHECK,
        GET_ACTION_END,
        GET_ACTION_KILL,
         GET_ACTION_COMPLETED,
         GET_RUNNING_ACTIONS,
         GET_PENDING_ACTIONS,
@@ -185,7 +183,6 @@ public class WorkflowActionQueryExecutor extends
                 query.setParameter("retries", actionBean.getRetries());
                 query.setParameter("status", actionBean.getStatus().toString());
                 query.setParameter("endTime", actionBean.getEndTimestamp());
                query.setParameter("retries", actionBean.getRetries());
                 query.setParameter("pending", actionBean.getPending());
                 query.setParameter("pendingAge", actionBean.getPendingAgeTimestamp());
                 query.setParameter("signalValue", actionBean.getSignalValue());
@@ -212,8 +209,6 @@ public class WorkflowActionQueryExecutor extends
             case GET_ACTION_SIGNAL:
             case GET_ACTION_START:
             case GET_ACTION_CHECK:
            case GET_ACTION_END:
            case GET_ACTION_KILL:
             case GET_ACTION_COMPLETED:
                 query.setParameter("id", parameters[0]);
                 break;
@@ -279,13 +274,14 @@ public class WorkflowActionQueryExecutor extends
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
                bean.setPendingAge(DateUtils.toDate((Timestamp) arr[5]));
                bean.setType((String) arr[6]);
                bean.setLogToken((String) arr[7]);
                bean.setTransition((String) arr[8]);
                bean.setErrorInfo((String) arr[9], (String) arr[10]);
                bean.setExecutionPath((String) arr[11]);
                bean.setSignalValue((String) arr[12]);
                bean.setSlaXmlBlob((StringBlob) arr[13]);
                 break;
             case GET_ACTION_START:
                 bean = new WorkflowActionBean();
@@ -295,69 +291,24 @@ public class WorkflowActionQueryExecutor extends
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
                bean.setPendingAge(DateUtils.toDate((Timestamp) arr[5]));
                bean.setType((String) arr[6]);
                bean.setLogToken((String) arr[7]);
                bean.setTransition((String) arr[8]);
                bean.setRetries((Integer) arr[9]);
                 bean.setUserRetryCount((Integer) arr[10]);
                 bean.setUserRetryMax((Integer) arr[11]);
                 bean.setUserRetryInterval((Integer) arr[12]);
                 bean.setStartTime(DateUtils.toDate((Timestamp) arr[13]));
                 bean.setEndTime(DateUtils.toDate((Timestamp) arr[14]));
                 bean.setErrorInfo((String) arr[15], (String) arr[16]);
                bean.setExternalId((String) arr[17]);
                bean.setExternalStatus((String) arr[18]);
                bean.setExternalChildIDsBlob((StringBlob) arr[19]);
                bean.setCred((String) arr[17]);
                bean.setExternalId((String) arr[18]);
                bean.setExternalStatus((String) arr[19]);
                 bean.setConfBlob((StringBlob) arr[20]);
                bean.setDataBlob((StringBlob) arr[21]);
                bean.setStatsBlob((StringBlob) arr[22]);
                bean.setSlaXmlBlob((StringBlob) arr[21]);
                 break;
            case GET_ACTION_KILL:
            case GET_ACTION_CHECK:
                 bean = new WorkflowActionBean();
                 arr = (Object[]) ret;
                 bean.setId((String) arr[0]);
@@ -365,17 +316,23 @@ public class WorkflowActionQueryExecutor extends
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
                bean.setPendingAge(DateUtils.toDate((Timestamp) arr[5]));
                bean.setType((String) arr[6]);
                bean.setLogToken((String) arr[7]);
                bean.setTransition((String) arr[8]);
                bean.setRetries((Integer) arr[9]);
                bean.setUserRetryCount((Integer) arr[10]);
                bean.setUserRetryMax((Integer) arr[11]);
                bean.setUserRetryInterval((Integer) arr[12]);
                bean.setTrackerUri((String) arr[13]);
                bean.setStartTime(DateUtils.toDate((Timestamp) arr[14]));
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[15]));
                bean.setLastCheckTime(DateUtils.toDate((Timestamp) arr[16]));
                bean.setErrorInfo((String) arr[17], (String) arr[18]);
                bean.setExternalId((String) arr[19]);
                bean.setExternalStatus((String) arr[20]);
                bean.setExternalChildIDsBlob((StringBlob) arr[21]);
                bean.setConfBlob((StringBlob) arr[22]);
                 break;
             case GET_ACTION_COMPLETED:
                 bean = new WorkflowActionBean();
diff --git a/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java b/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java
index 330cff868..c68fa4810 100644
-- a/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java
++ b/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java
@@ -361,10 +361,13 @@ public class TestEventGeneration extends XDataTestCase {
         ehs.setAppTypes(new HashSet<String>(Arrays.asList("workflow_action")));
         WorkflowJobBean job = this.addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
         WorkflowActionBean action = this.addRecordToWfActionTable(job.getId(), "1", WorkflowAction.Status.PREP, true);
        WorkflowActionGetJPAExecutor wfActionGetCmd = new WorkflowActionGetJPAExecutor(action.getId());
        // adding record sets externalChildID to dummy workflow-id so resetting it
        action.setExternalChildIDs(null);
        WorkflowActionQueryExecutor.getInstance().executeUpdate(WorkflowActionQuery.UPDATE_ACTION_START, action);
 
         // Starting job
         new ActionStartXCommand(action.getId(), "map-reduce").call();
        WorkflowActionGetJPAExecutor wfActionGetCmd = new WorkflowActionGetJPAExecutor(action.getId());
         action = jpaService.execute(wfActionGetCmd);
         assertEquals(WorkflowAction.Status.RUNNING, action.getStatus());
         assertEquals(1, queue.size());
@@ -488,12 +491,12 @@ public class TestEventGeneration extends XDataTestCase {
     public void testForNoDuplicates() throws Exception {
         // test workflow job events
         Reader reader = IOUtils.getResourceAsReader("wf-no-op.xml", -1);
        Writer writer = new FileWriter(new File(getTestCaseDir(), "workflow.xml"));
        Writer writer = new FileWriter(getTestCaseDir() + "/workflow.xml");
         IOUtils.copyCharStream(reader, writer);
 
         final DagEngine engine = new DagEngine(getTestUser());
         Configuration conf = new XConfiguration();
        conf.set(OozieClient.APP_PATH, getTestCaseFileUri("workflow.xml"));
        conf.set(OozieClient.APP_PATH, "file://" + getTestCaseDir() + File.separator + "workflow.xml");
         conf.set(OozieClient.USER_NAME, getTestUser());
 
         final String jobId1 = engine.submitJob(conf, true);
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java
index 77db9cf90..7c7c37bbd 100644
-- a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowActionQueryExecutor.java
@@ -191,16 +191,18 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
     }
 
     public void testGet() throws Exception {
        WorkflowActionBean bean = addRecordToWfActionTable("workflowId","testAction", WorkflowAction.Status.PREP);
        WorkflowActionBean bean = addRecordToWfActionTable("workflowId", "testAction", WorkflowAction.Status.PREP, "",
                true);
         WorkflowActionBean retBean;
 
        //GET_WORKFFLOW_ID_TYPE
        //GET_ACTION_ID_TYPE_LASTCHECK
         retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_ID_TYPE_LASTCHECK,
                 bean.getId());
         assertEquals(bean.getId(), retBean.getId());
         assertEquals(bean.getType(), retBean.getType());
        assertEquals(bean.getLastCheckTime(), retBean.getLastCheckTime());
 
        //GET_WORKFFLOW_FAIL
        //GET_ACTION_FAIL
         retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_FAIL, bean.getId());
         assertEquals(bean.getId(), retBean.getId());
         assertEquals(bean.getJobId(), retBean.getJobId());
@@ -218,13 +220,14 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
         assertNull(retBean.getStats());
         assertNull(retBean.getExternalChildIDs());
 
        //GET_WORKFFLOW_SIGNAL
        //GET_ACTION_SIGNAL
         retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_SIGNAL, bean.getId());
         assertEquals(bean.getId(), retBean.getId());
         assertEquals(bean.getJobId(), retBean.getJobId());
         assertEquals(bean.getName(), retBean.getName());
         assertEquals(bean.getStatusStr(), retBean.getStatusStr());
         assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getPendingAge().getTime(), retBean.getPendingAge().getTime());
         assertEquals(bean.getType(), retBean.getType());
         assertEquals(bean.getLogToken(), retBean.getLogToken());
         assertEquals(bean.getTransition(), retBean.getTransition());
@@ -256,23 +259,29 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
         assertEquals(bean.getErrorCode(), retBean.getErrorCode());
         assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
         assertEquals(bean.getCred(), retBean.getCred());
        assertEquals(bean.getExternalId(), retBean.getExternalId());
        assertEquals(bean.getExternalStatus(), retBean.getExternalStatus());
         assertEquals(bean.getConf(), retBean.getConf());
         assertEquals(bean.getSlaXml(), retBean.getSlaXml());
         assertNull(retBean.getData());
         assertNull(retBean.getStats());
         assertNull(retBean.getExternalChildIDs());
 
        // GET_WORKFLOW_CHECK
        // GET_ACTION_CHECK
         retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_CHECK, bean.getId());
         assertEquals(bean.getId(), retBean.getId());
         assertEquals(bean.getJobId(), retBean.getJobId());
         assertEquals(bean.getName(), retBean.getName());
         assertEquals(bean.getStatusStr(), retBean.getStatusStr());
         assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getPendingAge().getTime(), retBean.getPendingAge().getTime());
         assertEquals(bean.getType(), retBean.getType());
         assertEquals(bean.getLogToken(), retBean.getLogToken());
         assertEquals(bean.getTransition(), retBean.getTransition());
         assertEquals(bean.getRetries(), retBean.getRetries());
        assertEquals(bean.getUserRetryCount(), retBean.getUserRetryCount());
        assertEquals(bean.getUserRetryMax(), retBean.getUserRetryMax());
        assertEquals(bean.getUserRetryInterval(), retBean.getUserRetryInterval());
         assertEquals(bean.getTrackerUri(), retBean.getTrackerUri());
         assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
         assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
@@ -287,68 +296,48 @@ public class TestWorkflowActionQueryExecutor extends XDataTestCase {
         assertNull(retBean.getStats());
         assertNull(retBean.getSlaXml());
 
        // GET_WORKFLOW_END
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_END, bean.getId());
        //GET_ACTION_COMPLETED
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_COMPLETED, bean.getId());
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
        assertNull(retBean.getConf());
        assertNull(retBean.getData());
        assertNull(retBean.getStats());
        assertNull(retBean.getExternalChildIDs());
 
        // GET_WORKFLOW_KILL
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION_KILL, bean.getId());
        // GET_ACTION (entire obj)
        retBean = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION, bean.getId());
         assertEquals(bean.getId(), retBean.getId());
         assertEquals(bean.getJobId(), retBean.getJobId());
         assertEquals(bean.getName(), retBean.getName());
         assertEquals(bean.getStatusStr(), retBean.getStatusStr());
         assertEquals(bean.getPending(), retBean.getPending());
        assertEquals(bean.getPendingAge().getTime(), retBean.getPendingAge().getTime());
         assertEquals(bean.getType(), retBean.getType());
         assertEquals(bean.getLogToken(), retBean.getLogToken());
         assertEquals(bean.getTransition(), retBean.getTransition());
         assertEquals(bean.getRetries(), retBean.getRetries());
        assertEquals(bean.getTrackerUri(), retBean.getTrackerUri());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
        assertEquals(bean.getUserRetryCount(), retBean.getUserRetryCount());
        assertEquals(bean.getUserRetryMax(), retBean.getUserRetryMax());
        assertEquals(bean.getUserRetryInterval(), retBean.getUserRetryInterval());
         assertEquals(bean.getStartTime().getTime(), retBean.getStartTime().getTime());
         assertEquals(bean.getEndTime().getTime(), retBean.getEndTime().getTime());
        assertEquals(bean.getCreatedTime().getTime(), retBean.getCreatedTime().getTime());
        assertEquals(bean.getLastCheckTime().getTime(), retBean.getLastCheckTime().getTime());
        assertEquals(bean.getErrorCode(), retBean.getErrorCode());
         assertEquals(bean.getErrorMessage(), retBean.getErrorMessage());
        assertEquals(bean.getExternalId(), retBean.getExternalId());
        assertEquals(bean.getExecutionPath(), retBean.getExecutionPath());
        assertEquals(bean.getSignalValue(), retBean.getSignalValue());
        assertEquals(bean.getCred(), retBean.getCred());
         assertEquals(bean.getConf(), retBean.getConf());
        assertEquals(bean.getSlaXml(), retBean.getSlaXml());
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
        assertEquals(bean.getStats(), retBean.getStats());
        assertEquals(bean.getExternalChildIDs(), retBean.getExternalChildIDs());
     }
 
     public void testGetList() throws Exception {
diff --git a/release-log.txt b/release-log.txt
index 3a0ed38b4..7cbce39c8 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1559 Fix missing fields from new SELECT queries and Recovery Service picking up killed control nodes (ryota,mona via mona)
 OOZIE-1569 Maintain backward incompatibility for running jobs before upgrade (mona)
 OOZIE-1568 TestWorkflowClient.testSla is flakey (rkanter)
 OOZIE-1517 Support using MS SQL Server as a metastore (dwann via rkanter)
- 
2.19.1.windows.1

