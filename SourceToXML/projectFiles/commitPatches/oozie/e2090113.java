From e20901133e43ff2fce39512d5c78abc588acbe39 Mon Sep 17 00:00:00 2001
From: Bowen Zhang <bowenzhangusa@yahoo.com>
Date: Tue, 1 Jul 2014 13:02:55 -0700
Subject: [PATCH] OOZIE-1532 Purging should remove completed children job for
 long running coordinator jobs (bzhang)

--
 .../apache/oozie/CoordinatorActionBean.java   |   2 +
 .../org/apache/oozie/WorkflowJobBean.java     |   2 +
 .../apache/oozie/command/PurgeXCommand.java   |  60 ++++++-
 .../jpa/CoordActionsDeleteJPAExecutor.java    |  92 +++++++++++
 .../CoordJobGetActionsSubsetJPAExecutor.java  |  31 +++-
 .../jpa/WorkflowJobQueryExecutor.java         |  17 +-
 .../apache/oozie/service/PurgeService.java    |  15 +-
 core/src/main/resources/oozie-default.xml     |  10 ++
 .../oozie/command/TestPurgeXCommand.java      | 127 +++++++++++++++
 .../TestCoordActionsDeleteJPAExecutor.java    | 154 ++++++++++++++++++
 .../jpa/TestWorkflowJobQueryExecutor.java     |  24 +++
 release-log.txt                               |   1 +
 12 files changed, 525 insertions(+), 10 deletions(-)
 create mode 100644 core/src/main/java/org/apache/oozie/executor/jpa/CoordActionsDeleteJPAExecutor.java
 create mode 100644 core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionsDeleteJPAExecutor.java

diff --git a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
index 51eaf2de8..795bf639e 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
@@ -80,6 +80,8 @@ import org.json.simple.JSONObject;
 
         @NamedQuery(name = "DELETE_ACTIONS_FOR_COORDINATOR", query = "delete from CoordinatorActionBean a where a.jobId = :jobId"),
 
        @NamedQuery(name = "DELETE_ACTIONS_FOR_LONG_RUNNING_COORDINATOR", query = "delete from CoordinatorActionBean a where a.id = :actionId"),

         @NamedQuery(name = "DELETE_UNSCHEDULED_ACTION", query = "delete from CoordinatorActionBean a where a.id = :id and (a.statusStr = 'WAITING' OR a.statusStr = 'READY')"),
 
         // Query used by XTestcase to setup tables
diff --git a/core/src/main/java/org/apache/oozie/WorkflowJobBean.java b/core/src/main/java/org/apache/oozie/WorkflowJobBean.java
index 5fbee8204..36bc4ae0b 100644
-- a/core/src/main/java/org/apache/oozie/WorkflowJobBean.java
++ b/core/src/main/java/org/apache/oozie/WorkflowJobBean.java
@@ -85,6 +85,8 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "GET_COMPLETED_WORKFLOWS_WITH_NO_PARENT_OLDER_THAN", query = "select w.id from WorkflowJobBean w where w.endTimestamp < :endTime and w.parentId is null"),
 
    @NamedQuery(name = "GET_COMPLETED_COORD_WORKFLOWS_OLDER_THAN", query = "select w.id, w.parentId from WorkflowJobBean w where w.endTimestamp < :endTime and w.parentId like '%C@%'"),

     @NamedQuery(name = "GET_WORKFLOW", query = "select OBJECT(w) from WorkflowJobBean w where w.id = :id"),
 
     @NamedQuery(name = "GET_WORKFLOW_STARTTIME", query = "select w.id, w.startTimestamp from WorkflowJobBean w where w.id = :id"),
diff --git a/core/src/main/java/org/apache/oozie/command/PurgeXCommand.java b/core/src/main/java/org/apache/oozie/command/PurgeXCommand.java
index 99737199c..da94d3990 100644
-- a/core/src/main/java/org/apache/oozie/command/PurgeXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/PurgeXCommand.java
@@ -22,10 +22,14 @@ import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 
import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.XException;
 import org.apache.oozie.executor.jpa.BundleJobsDeleteJPAExecutor;
 import org.apache.oozie.executor.jpa.BundleJobsGetForPurgeJPAExecutor;
import org.apache.oozie.executor.jpa.CoordActionsDeleteJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.executor.jpa.CoordJobsCountNotForPurgeFromParentIdJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobsDeleteJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobsGetForPurgeJPAExecutor;
@@ -50,21 +54,30 @@ public class PurgeXCommand extends XCommand<Void> {
     private int wfOlderThan;
     private int coordOlderThan;
     private int bundleOlderThan;
    private boolean purgeOldCoordAction = false;
     private final int limit;
     private List<String> wfList;
    private List<String> coordActionList;
     private List<String> coordList;
     private List<String> bundleList;
     private int wfDel;
     private int coordDel;
    private int coordActionDel;
     private int bundleDel;
 
     public PurgeXCommand(int wfOlderThan, int coordOlderThan, int bundleOlderThan, int limit) {
        this(wfOlderThan, coordOlderThan, bundleOlderThan, limit, false);
    }

    public PurgeXCommand(int wfOlderThan, int coordOlderThan, int bundleOlderThan, int limit, boolean purgeOldCoordAction) {
         super("purge", "purge", 0);
         this.wfOlderThan = wfOlderThan;
         this.coordOlderThan = coordOlderThan;
         this.bundleOlderThan = bundleOlderThan;
        this.purgeOldCoordAction = purgeOldCoordAction;
         this.limit = limit;
         wfList = new ArrayList<String>();
        coordActionList = new ArrayList<String>();
         coordList = new ArrayList<String>();
         bundleList = new ArrayList<String>();
         wfDel = 0;
@@ -87,6 +100,20 @@ public class PurgeXCommand extends XCommand<Void> {
                     size = wfList.size();
                     wfList.addAll(jpaService.execute(new WorkflowJobsGetForPurgeJPAExecutor(wfOlderThan, wfList.size(), limit)));
                 } while(size != wfList.size());
                if (purgeOldCoordAction) {
                    LOG.debug("Purging workflows of long running coordinators is turned on");
                    do {
                        size = coordActionList.size();
                        long olderThan = wfOlderThan;
                        List<WorkflowJobBean> jobBeans = WorkflowJobQueryExecutor.getInstance().getList(
                                WorkflowJobQuery.GET_COMPLETED_COORD_WORKFLOWS_OLDER_THAN, olderThan,
                                coordActionList.size(), limit);
                        for (WorkflowJobBean bean : jobBeans) {
                            coordActionList.add(bean.getParentId());
                            wfList.add(bean.getId());
                        }
                    } while(size != coordActionList.size());
                }
                 do {
                     size = coordList.size();
                     coordList.addAll(jpaService.execute(
@@ -112,7 +139,7 @@ public class PurgeXCommand extends XCommand<Void> {
      */
     @Override
     protected Void execute() throws CommandException {
        LOG.debug("STARTED Purge to purge Workflow Jobs older than [{0}] days, Coordinator Jobs older than [{1}] days, and Bundle"
        LOG.info("STARTED Purge to purge Workflow Jobs older than [{0}] days, Coordinator Jobs older than [{1}] days, and Bundle"
                 + "jobs older than [{2}] days.", wfOlderThan, coordOlderThan, bundleOlderThan);
 
         // Process parentless workflows to purge them and their children
@@ -125,6 +152,15 @@ public class PurgeXCommand extends XCommand<Void> {
             }
         }
 
        // Process coordinator actions of long running coordinators and purge them
        if (!coordActionList.isEmpty()) {
            try {
                purgeCoordActions(coordActionList);
            }
            catch (JPAExecutorException je) {
                throw new CommandException(je);
            }
        }
         // Processs parentless coordinators to purge them and their children
         if (!coordList.isEmpty()) {
             try {
@@ -145,7 +181,8 @@ public class PurgeXCommand extends XCommand<Void> {
             }
         }
 
        LOG.debug("ENDED Purge deleted [{0}] workflows, [{1}] coordinators, [{2}] bundles", wfDel, coordDel, bundleDel);
        LOG.info("ENDED Purge deleted [{0}] workflows, [{1}] coordinatorActions, [{2}] coordinators, [{3}] bundles",
                wfDel, coordActionDel, coordDel, bundleDel);
         return null;
     }
 
@@ -158,6 +195,9 @@ public class PurgeXCommand extends XCommand<Void> {
      */
     private void processWorkflows(List<String> wfs) throws JPAExecutorException {
         List<String> wfsToPurge = processWorkflowsHelper(wfs);
        for (String id: wfsToPurge) {
            LOG.debug("Purging workflow " + id);
        }
         purgeWorkflows(wfsToPurge);
     }
 
@@ -212,6 +252,7 @@ public class PurgeXCommand extends XCommand<Void> {
                     new WorkflowJobsCountNotForPurgeFromCoordParentIdJPAExecutor(wfOlderThan, coordId));
             if (numChildrenNotReady == 0) {
                 coordsToPurge.add(coordId);
                LOG.debug("Purging coordinator " + coordId);
                 // Get all of the direct children for this coord
                 List<String> children = new ArrayList<String>();
                 int size;
@@ -245,6 +286,7 @@ public class PurgeXCommand extends XCommand<Void> {
                     new CoordJobsCountNotForPurgeFromParentIdJPAExecutor(coordOlderThan, bundleId));
             if (numChildrenNotReady == 0) {
                 bundlesToPurge.add(bundleId);
                LOG.debug("Purging bundle " + bundleId);
                 // Get all of the direct children for this bundle
                 List<String> children = new ArrayList<String>();
                 int size;
@@ -278,6 +320,20 @@ public class PurgeXCommand extends XCommand<Void> {
         }
     }
 
    /**
     * Purge coordActions of long running coordinators and purge them
     *
     * @param coordActions List of coordActions to purge
     * @throws JPAExecutorException If a JPA executor has a problem
     */
    private void purgeCoordActions(List<String> coordActions) throws JPAExecutorException {
        coordActionDel = coordActions.size();
        for (int startIndex = 0; startIndex < coordActions.size(); ) {
            int endIndex = (startIndex + limit < coordActions.size()) ? (startIndex + limit) : coordActions.size();
            jpaService.execute(new CoordActionsDeleteJPAExecutor(coordActions.subList(startIndex, endIndex)));
            startIndex = endIndex;
        }
    }
     /**
      * Purge the coordinators in SOME order in batches of size 'limit' (its in reverse order only for convenience)
      *
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionsDeleteJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionsDeleteJPAExecutor.java
new file mode 100644
index 000000000..0e007184b
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionsDeleteJPAExecutor.java
@@ -0,0 +1,92 @@
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

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.oozie.ErrorCode;
import org.apache.oozie.FaultInjection;
import org.apache.oozie.util.ParamChecker;
/**
 * Delete Coord actions of long running coordinators, return the number of actions that were deleted.
 */
public class CoordActionsDeleteJPAExecutor implements JPAExecutor<Integer> {
    private Collection<String> deleteList;

    /**
     * Initialize the JPAExecutor using the delete list of CoordinatorActionBeans
     * @param deleteList
     */
    public CoordActionsDeleteJPAExecutor(Collection<String> deleteList) {
        this.deleteList = deleteList;
    }

    public CoordActionsDeleteJPAExecutor() {
    }

    /**
     * Sets the delete list for CoordinatorActionBeans
     *
     * @param deleteList
     */
    public void setDeleteList(Collection<String> deleteList) {
        this.deleteList = deleteList;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.oozie.executor.jpa.JPAExecutor#getName()
     */
    @Override
    public String getName() {
        return "CoordActionsDeleteJPAExecutor";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.oozie.executor.jpa.JPAExecutor#execute(javax.persistence.
     * EntityManager)
     */
    @Override
    public Integer execute(EntityManager em) throws JPAExecutorException {
        int actionsDeleted = 0;
        try {
            // Only used by test cases to check for rollback of transaction
            FaultInjection.activate("org.apache.oozie.command.SkipCommitFaultInjection");
            if (deleteList != null) {
                for (String id : deleteList) {
                    ParamChecker.notNull(id, "Coordinator Action Id");

                    // Delete coordAction
                    Query g = em.createNamedQuery("DELETE_ACTIONS_FOR_LONG_RUNNING_COORDINATOR");
                    g.setParameter("actionId", id);
                    g.executeUpdate();
                    actionsDeleted++;
                }
            }
        }
        catch (Exception e) {
            throw new JPAExecutorException(ErrorCode.E0603, e.getMessage(), e);
        }
        return actionsDeleted;
    }
}
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java
index 873f0810e..420a466ae 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java
@@ -101,6 +101,7 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
             StringBuilder statusClause = new StringBuilder();
             getStatusClause(statusClause, filterMap.get(CoordinatorEngine.POSITIVE_FILTER), true);
             getStatusClause(statusClause, filterMap.get(CoordinatorEngine.NEGATIVE_FILTER), false);
            getIdClause(statusClause);
             // Insert 'where' before 'order by'
             sbTotal.insert(offset, statusClause);
             q = em.createQuery(sbTotal.toString());
@@ -108,8 +109,7 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
         if (desc) {
             q = em.createQuery(q.toString().concat(" desc"));
         }
        q.setParameter("jobId", coordJobId);
        q.setFirstResult(start - 1);
        q.setParameter("jobId", coordJobId);;
         q.setMaxResults(len);
         return q;
     }
@@ -124,15 +124,15 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
             for (String statusVal : filterList) {
                 if (!isStatus) {
                     if (positive) {
                        sb.append(" and a.statusStr IN (\'" + statusVal + "\'");
                        sb.append(" and a.statusStr IN (\'").append(statusVal).append("\'");
                     }
                     else {
                        sb.append(" and a.statusStr NOT IN (\'" + statusVal + "\'");
                        sb.append(" and a.statusStr NOT IN (\'").append(statusVal).append("\'");
                     }
                     isStatus = true;
                 }
                 else {
                    sb.append(",\'" + statusVal + "\'");
                    sb.append(",\'").append(statusVal).append("\'");
                 }
             }
             sb.append(") ");
@@ -140,6 +140,27 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
         return sb;
     }
 
    // Form the where clause for coord action ids
    private StringBuilder getIdClause(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        sb.append("and a.id IN (");
        boolean isFirst = true;
        for (int i = start; i < start + len; i++) {
            if (isFirst) {
                sb.append("\'").append(coordJobId).append("@").append(i).append("\'");
                isFirst = false;
            }
            else {
                sb.append(", \'").append(coordJobId).append("@").append(i).append("\'");
            }
        }
        sb.append(") ");

        return sb;
    }

     private CoordinatorActionBean getBeanForRunningCoordAction(Object arr[]) {
         CoordinatorActionBean bean = new CoordinatorActionBean();
         if (arr[0] != null) {
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java
index e2a9438fe..733fd64b6 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowJobQueryExecutor.java
@@ -59,7 +59,8 @@ public class WorkflowJobQueryExecutor extends QueryExecutor<WorkflowJobBean, Wor
         GET_WORKFLOW_KILL,
         GET_WORKFLOW_RESUME,
         GET_WORKFLOW_STATUS,
        GET_WORKFLOWS_PARENT_COORD_RERUN
        GET_WORKFLOWS_PARENT_COORD_RERUN,
        GET_COMPLETED_COORD_WORKFLOWS_OLDER_THAN
     };
 
     private static WorkflowJobQueryExecutor instance = new WorkflowJobQueryExecutor();
@@ -185,6 +186,14 @@ public class WorkflowJobQueryExecutor extends QueryExecutor<WorkflowJobBean, Wor
             case GET_WORKFLOWS_PARENT_COORD_RERUN:
                 query.setParameter("parentId", parameters[0]);
                 break;
            case GET_COMPLETED_COORD_WORKFLOWS_OLDER_THAN:
                long dayInMs = 24 * 60 * 60 * 1000;
                long olderThanDays = (Long) parameters[0];
                Timestamp maxEndtime = new Timestamp(System.currentTimeMillis() - (olderThanDays * dayInMs));
                query.setParameter("endTime", maxEndtime);
                query.setFirstResult((Integer) parameters[1]);
                query.setMaxResults((Integer) parameters[2]);
                break;
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
                         + namedQuery.name());
@@ -322,6 +331,12 @@ public class WorkflowJobQueryExecutor extends QueryExecutor<WorkflowJobBean, Wor
                 bean.setStartTime(DateUtils.toDate((Timestamp) arr[2]));
                 bean.setEndTime(DateUtils.toDate((Timestamp) arr[3]));
                 break;
            case GET_COMPLETED_COORD_WORKFLOWS_OLDER_THAN:
                bean = new WorkflowJobBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setParentId((String) arr[1]);
                break;
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct job bean for "
                         + namedQuery.name());
diff --git a/core/src/main/java/org/apache/oozie/service/PurgeService.java b/core/src/main/java/org/apache/oozie/service/PurgeService.java
index 9cc3ebe2e..9eeee3057 100644
-- a/core/src/main/java/org/apache/oozie/service/PurgeService.java
++ b/core/src/main/java/org/apache/oozie/service/PurgeService.java
@@ -32,6 +32,7 @@ public class PurgeService implements Service {
     public static final String CONF_OLDER_THAN = CONF_PREFIX + "older.than";
     public static final String COORD_CONF_OLDER_THAN = CONF_PREFIX + "coord.older.than";
     public static final String BUNDLE_CONF_OLDER_THAN = CONF_PREFIX + "bundle.older.than";
    public static final String PURGE_OLD_COORD_ACTION = CONF_PREFIX + "purge.old.coord.action";
     /**
      * Time interval, in seconds, at which the purge jobs service will be scheduled to run.
      */
@@ -47,6 +48,7 @@ public class PurgeService implements Service {
         private int coordOlderThan;
         private int bundleOlderThan;
         private int limit;
        private boolean purgeOldCoordAction = false;
 
         public PurgeRunnable(int wfOlderThan, int coordOlderThan, int bundleOlderThan, int limit) {
             this.wfOlderThan = wfOlderThan;
@@ -55,11 +57,20 @@ public class PurgeService implements Service {
             this.limit = limit;
         }
 
        public PurgeRunnable(int wfOlderThan, int coordOlderThan, int bundleOlderThan, int limit,
                             boolean purgeOldCoordAction) {
            this.wfOlderThan = wfOlderThan;
            this.coordOlderThan = coordOlderThan;
            this.bundleOlderThan = bundleOlderThan;
            this.limit = limit;
            this.purgeOldCoordAction = purgeOldCoordAction;
        }

         public void run() {
             // Only queue the purge command if this is the first server
             if (Services.get().get(JobsConcurrencyService.class).isFirstServer()) {
                 Services.get().get(CallableQueueService.class).queue(
                        new PurgeXCommand(wfOlderThan, coordOlderThan, bundleOlderThan, limit));
                        new PurgeXCommand(wfOlderThan, coordOlderThan, bundleOlderThan, limit, purgeOldCoordAction));
             }
         }
 
@@ -75,7 +86,7 @@ public class PurgeService implements Service {
         Configuration conf = services.getConf();
         Runnable purgeJobsRunnable = new PurgeRunnable(conf.getInt(
                 CONF_OLDER_THAN, 30), conf.getInt(COORD_CONF_OLDER_THAN, 7), conf.getInt(BUNDLE_CONF_OLDER_THAN, 7),
                                      conf.getInt(PURGE_LIMIT, 100));
                                      conf.getInt(PURGE_LIMIT, 100), conf.getBoolean(PURGE_OLD_COORD_ACTION, false));
         services.get(SchedulerService.class).schedule(purgeJobsRunnable, 10, conf.getInt(CONF_PURGE_INTERVAL, 3600),
                                                       SchedulerService.Unit.SEC);
     }
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 61ac38835..982c82fa6 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -287,6 +287,16 @@
             Completed bundle jobs older than this value, in days, will be purged by the PurgeService.
         </description>
     </property>

    <property>
        <name>oozie.service.PurgeService.purge.old.coord.action</name>
        <value>false</value>
        <description>
            Whether to purge completed workflows and their corresponding coordinator actions
            of long running coordinator jobs if the completed workflow jobs are older than the value
            specified in oozie.service.PurgeService.older.than.
        </description>
    </property>
     
     <property>
 		<name>oozie.service.PurgeService.purge.limit</name>
diff --git a/core/src/test/java/org/apache/oozie/command/TestPurgeXCommand.java b/core/src/test/java/org/apache/oozie/command/TestPurgeXCommand.java
index 666271ed5..979cbbc26 100644
-- a/core/src/test/java/org/apache/oozie/command/TestPurgeXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/TestPurgeXCommand.java
@@ -677,6 +677,133 @@ public class TestPurgeXCommand extends XDataTestCase {
         }
     }
 
    /**
     * Test : The workflow should get purged, but the coordinator parent shouldn't get purged -->
     * the workflow and corresponding coord actions will get purged after we turn the purge.old.coord.action on
     * Coordinator itself will not be purged
     *
     * @throws Exception
     */
    public void testPurgeLongRunningCoordWithWFChild() throws Exception {
        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);

        CoordinatorJobBean coordJob = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        WorkflowJobBean wfJob = addRecordToWfJobTable(WorkflowJob.Status.SUCCEEDED, WorkflowInstance.Status.SUCCEEDED);
        WorkflowActionBean wfAction = addRecordToWfActionTable(wfJob.getId(), "1", WorkflowAction.Status.OK);
        CoordinatorActionBean coordAction = addRecordToCoordActionTable(coordJob.getId(), 1, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", wfJob.getId(), "SUCCEEDED", 0);

        WorkflowJobGetJPAExecutor wfJobGetCmd = new WorkflowJobGetJPAExecutor(wfJob.getId());
        WorkflowActionGetJPAExecutor wfActionGetCmd = new WorkflowActionGetJPAExecutor(wfAction.getId());
        CoordJobGetJPAExecutor coordJobGetCmd = new CoordJobGetJPAExecutor(coordJob.getId());
        CoordActionGetJPAExecutor coordActionGetCmd = new CoordActionGetJPAExecutor(coordAction.getId());

        wfJob = jpaService.execute(wfJobGetCmd);
        wfAction = jpaService.execute(wfActionGetCmd);
        coordJob = jpaService.execute(coordJobGetCmd);
        coordAction = jpaService.execute(coordActionGetCmd);
        assertEquals(WorkflowJob.Status.SUCCEEDED, wfJob.getStatus());
        assertEquals(WorkflowAction.Status.OK, wfAction.getStatus());
        assertEquals(CoordinatorJob.Status.RUNNING, coordJob.getStatus());
        assertEquals(CoordinatorAction.Status.SUCCEEDED, coordAction.getStatus());

        new PurgeXCommand(7, getNumDaysToNotBePurged(coordJob.getLastModifiedTime()), 1, 10, true).call();

        try {
            jpaService.execute(coordJobGetCmd);
        }
        catch (JPAExecutorException je) {
            fail("Coordinator Job should not have been purged");
        }

        try {
            jpaService.execute(coordActionGetCmd);
            fail("Coordinator Action should have been purged");
        }
        catch (JPAExecutorException je) {
            assertEquals(ErrorCode.E0605, je.getErrorCode());
        }

        try {
            jpaService.execute(wfJobGetCmd);
            fail("Workflow Job should have been purged");
        }
        catch (JPAExecutorException je) {
            assertEquals(ErrorCode.E0604, je.getErrorCode());
        }

        try {
            jpaService.execute(wfActionGetCmd);
            fail("Workflow Action should have been purged");
        }
        catch (JPAExecutorException je) {
            assertEquals(ErrorCode.E0605, je.getErrorCode());
        }
    }

    /**
     * Test : The workflow should get purged, but the coordinator parent shouldn't get purged -->
     * the workflow and corresponding coord actions will NOT get purged after we turn the purge.old.coord.action off
     * Neither will be purged
     *
     * @throws Exception
     */
    public void testPurgeLongRunningCoordWithWFChildNegative() throws Exception {
        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);

        CoordinatorJobBean coordJob = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        WorkflowJobBean wfJob = addRecordToWfJobTable(WorkflowJob.Status.SUCCEEDED, WorkflowInstance.Status.SUCCEEDED);
        WorkflowActionBean wfAction = addRecordToWfActionTable(wfJob.getId(), "1", WorkflowAction.Status.OK);
        CoordinatorActionBean coordAction = addRecordToCoordActionTable(coordJob.getId(), 1, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", wfJob.getId(), "SUCCEEDED", 0);

        WorkflowJobGetJPAExecutor wfJobGetCmd = new WorkflowJobGetJPAExecutor(wfJob.getId());
        WorkflowActionGetJPAExecutor wfActionGetCmd = new WorkflowActionGetJPAExecutor(wfAction.getId());
        CoordJobGetJPAExecutor coordJobGetCmd = new CoordJobGetJPAExecutor(coordJob.getId());
        CoordActionGetJPAExecutor coordActionGetCmd = new CoordActionGetJPAExecutor(coordAction.getId());

        wfJob = jpaService.execute(wfJobGetCmd);
        wfAction = jpaService.execute(wfActionGetCmd);
        coordJob = jpaService.execute(coordJobGetCmd);
        coordAction = jpaService.execute(coordActionGetCmd);
        assertEquals(WorkflowJob.Status.SUCCEEDED, wfJob.getStatus());
        assertEquals(WorkflowAction.Status.OK, wfAction.getStatus());
        assertEquals(CoordinatorJob.Status.RUNNING, coordJob.getStatus());
        assertEquals(CoordinatorAction.Status.SUCCEEDED, coordAction.getStatus());

        new PurgeXCommand(7, getNumDaysToNotBePurged(coordJob.getLastModifiedTime()), 1, 10, false).call();

        try {
            jpaService.execute(coordJobGetCmd);
        }
        catch (JPAExecutorException je) {
            fail("Coordinator Job should not have been purged");
        }

        try {
            jpaService.execute(coordActionGetCmd);
        }
        catch (JPAExecutorException je) {
            fail("Coordinator Action should not have been purged");
        }

        try {
            jpaService.execute(wfJobGetCmd);
        }
        catch (JPAExecutorException je) {
            fail("Workflow Job should not have been purged");
        }

        try {
            jpaService.execute(wfActionGetCmd);
        }
        catch (JPAExecutorException je) {
            fail("Workflow Action should not have been purged");
        }
    }

     /**
      * Test : The workflow should not get purged, but the coordinator parent should get purged --> neither will get purged
      *
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionsDeleteJPAExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionsDeleteJPAExecutor.java
new file mode 100644
index 000000000..02392dc9f
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionsDeleteJPAExecutor.java
@@ -0,0 +1,154 @@
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

import java.util.ArrayList;
import java.util.List;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.FaultInjection;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.command.SkipCommitFaultInjection;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XDataTestCase;
public class TestCoordActionsDeleteJPAExecutor extends XDataTestCase {
    Services services;
    private String[] excludedServices = { "org.apache.oozie.service.StatusTransitService",
            "org.apache.oozie.service.PauseTransitService", "org.apache.oozie.service.PurgeService",
            "org.apache.oozie.service.CoordMaterializeTriggerService", "org.apache.oozie.service.RecoveryService" };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        setClassesToBeExcluded(services.getConf(), excludedServices);
        services.init();
    }

    @Override
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
    }

    public void testDeleteCoordActions() throws Exception {
        CoordinatorJobBean job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        CoordinatorActionBean action1 = addRecordToCoordActionTable(job.getId(), 1, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", 0);
        CoordinatorActionBean action2 = addRecordToCoordActionTable(job.getId(), 2, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", 0);
        CoordinatorActionBean action3 = addRecordToCoordActionTable(job.getId(), 3, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", 0);

        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);

        List<String> deleteList = new ArrayList<String>();
        deleteList.add(action1.getId());
        deleteList.add(action2.getId());
        deleteList.add(action3.getId());

        jpaService.execute(new CoordActionsDeleteJPAExecutor(deleteList));

        try {
            jpaService.execute(new CoordActionGetJPAExecutor(action1.getId()));
            fail("CoordinatorAction action1 should have been deleted");
        }
        catch (JPAExecutorException je) {
            assertEquals(ErrorCode.E0605, je.getErrorCode());
        }

        try {
            jpaService.execute(new CoordActionGetJPAExecutor(action2.getId()));
            fail("CoordinatorAction action1 should have been deleted");
        }
        catch (JPAExecutorException je) {
            assertEquals(ErrorCode.E0605, je.getErrorCode());
        }

        try {
            jpaService.execute(new CoordActionGetJPAExecutor(action3.getId()));
            fail("CoordinatorAction action1 should have been deleted");
        }
        catch (JPAExecutorException je) {
            assertEquals(ErrorCode.E0605, je.getErrorCode());
        }
    }

    public void testDeleteCoordActionsRollback() throws Exception {
        CoordinatorJobBean job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        CoordinatorActionBean action1 = addRecordToCoordActionTable(job.getId(), 1, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", 0);
        CoordinatorActionBean action2 = addRecordToCoordActionTable(job.getId(), 2, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", 0);
        CoordinatorActionBean action3 = addRecordToCoordActionTable(job.getId(), 3, CoordinatorAction.Status.SUCCEEDED,
                "coord-action-get.xml", 0);

        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);

        List<String> deleteList = new ArrayList<String>();
        deleteList.add(action1.getId());
        deleteList.add(action2.getId());
        deleteList.add(action3.getId());

        try {
            // set fault injection to true, so transaction is roll backed
            setSystemProperty(FaultInjection.FAULT_INJECTION, "true");
            setSystemProperty(SkipCommitFaultInjection.ACTION_FAILOVER_FAULT_INJECTION, "true");

            try {
                jpaService.execute(new CoordActionsDeleteJPAExecutor(deleteList));
                fail("Should have skipped commit for failover testing");
            }
            catch (RuntimeException re) {
                assertEquals("Skipping Commit for Failover Testing", re.getMessage());
            }
        }
        finally {
            // Remove fault injection
            FaultInjection.deactivate("org.apache.oozie.command.SkipCommitFaultInjection");
        }

        try {
            jpaService.execute(new CoordActionGetJPAExecutor(action1.getId()));
        }
        catch (JPAExecutorException je) {
            fail("Coordinator Action1 should not have been deleted");
        }

        try {
            jpaService.execute(new CoordActionGetJPAExecutor(action2.getId()));
        }
        catch (JPAExecutorException je) {
            fail("Coordinator Action2 should not have been deleted");
        }

        try {
            jpaService.execute(new CoordActionGetJPAExecutor(action3.getId()));
        }
        catch (JPAExecutorException je) {
            fail("Coordinator Action3 should not have been deleted");
        }
    }
}
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java
index 7a1068562..02fabd2d9 100644
-- a/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestWorkflowJobQueryExecutor.java
@@ -18,8 +18,10 @@
 package org.apache.oozie.executor.jpa;
 
 import java.nio.ByteBuffer;
import java.util.Arrays;
 import java.util.Date;
 import java.util.List;
import java.util.HashSet;
 
 import javax.persistence.EntityManager;
 import javax.persistence.Query;
@@ -35,6 +37,7 @@ import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.workflow.WorkflowInstance;
 
 public class TestWorkflowJobQueryExecutor extends XDataTestCase {
@@ -339,5 +342,26 @@ public class TestWorkflowJobQueryExecutor extends XDataTestCase {
         assertEquals(2, wfsForRerun.size());
         assertEquals(wfJob1.getId(), wfsForRerun.get(0).getId());
         assertEquals(wfJob2.getId(), wfsForRerun.get(1).getId());

        // GET_COMPLETED_COORD_WORKFLOWS_OLDER_THAN
        coordJob = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, null, null, false,
                false, 1);
        wfJob1 = addRecordToWfJobTable(WorkflowJob.Status.SUCCEEDED, WorkflowInstance.Status.SUCCEEDED,
                coordJob.getId() + "@1");
        wfJob1.setEndTime(DateUtils.parseDateOozieTZ("2009-12-18T03:00Z"));
        WorkflowJobQueryExecutor.getInstance().executeUpdate(WorkflowJobQuery.UPDATE_WORKFLOW, wfJob1);
        wfJob2 = addRecordToWfJobTable(WorkflowJob.Status.SUCCEEDED, WorkflowInstance.Status.SUCCEEDED,
                coordJob.getId() + "@2");
        wfJob2.setEndTime(DateUtils.parseDateOozieTZ("2009-12-18T03:00Z"));
        WorkflowJobQueryExecutor.getInstance().executeUpdate(WorkflowJobQuery.UPDATE_WORKFLOW, wfJob2);
        long olderthan = 30;
        List<WorkflowJobBean> jobBeans = WorkflowJobQueryExecutor.getInstance().getList(
                WorkflowJobQuery.GET_COMPLETED_COORD_WORKFLOWS_OLDER_THAN, olderthan,
                0, 10);

        HashSet<String> jobIds = new HashSet<String>(Arrays.asList(wfJob1.getId(), wfJob2.getId()));
        assertEquals(2, jobBeans.size());
        assertTrue(jobIds.contains(jobBeans.get(0).getId()));
        assertTrue(jobIds.contains(jobBeans.get(1).getId()));
     }
 }
diff --git a/release-log.txt b/release-log.txt
index bbfc50e53..755c9f00f 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1532 Purging should remove completed children job for long running coordinator jobs (bzhang)
 OOZIE-1909 log prefix information missing in JavaActionExecutor.check (ryota)
 OOZIE-1907 DB upgrade from 3.3.0 to trunk fails on derby (rkanter)
 OOZIE-1877 Setting to fail oozie server startup in case of sharelib misconfiguration (puru via rohini)
- 
2.19.1.windows.1

