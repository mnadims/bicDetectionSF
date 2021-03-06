From 283cf1c23a9da8288d991bf207ede3f32cd4a23f Mon Sep 17 00:00:00 2001
From: virag <virag@unknown>
Date: Thu, 10 Jan 2013 22:44:36 +0000
Subject: [PATCH] OOZIE-1161 Remove unnecessary db updates for some of the
 blobs like missing_dependencies' of Coordinator Action (virag)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1431715 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/oozie/CoordinatorActionBean.java   |  2 +
 .../coord/CoordActionInputCheckXCommand.java  | 21 ++++-
 ...ctionUpdateForModifiedTimeJPAExecutor.java | 72 +++++++++++++++++
 ...ctionUpdateForModifiedTimeJPAExecutor.java | 77 +++++++++++++++++++
 release-log.txt                               |  1 +
 5 files changed, 170 insertions(+), 3 deletions(-)
 create mode 100644 core/src/main/java/org/apache/oozie/executor/jpa/CoordActionUpdateForModifiedTimeJPAExecutor.java
 create mode 100644 core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionUpdateForModifiedTimeJPAExecutor.java

diff --git a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
index 3711ef2c1..bfefadec9 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
@@ -59,6 +59,8 @@ import org.apache.openjpa.persistence.jdbc.Index;
         // Update query for Start
         @NamedQuery(name = "UPDATE_COORD_ACTION_FOR_START", query = "update CoordinatorActionBean w set w.status =:status, w.lastModifiedTimestamp = :lastModifiedTime, w.runConf = :runConf, w.externalId = :externalId, w.pending = :pending, w.errorCode = :errorCode, w.errorMessage = :errorMessage  where w.id = :id"),
 
        @NamedQuery(name = "UPDATE_COORD_ACTION_FOR_MODIFIED_DATE", query = "update CoordinatorActionBean w set w.lastModifiedTimestamp = :lastModifiedTime where w.id = :id"),

         @NamedQuery(name = "DELETE_COMPLETED_ACTIONS_FOR_COORDINATOR", query = "delete from CoordinatorActionBean a where a.jobId = :jobId and (a.status = 'SUCCEEDED' OR a.status = 'FAILED' OR a.status= 'KILLED')"),
 
         @NamedQuery(name = "DELETE_UNSCHEDULED_ACTION", query = "delete from CoordinatorActionBean a where a.id = :id and (a.status = 'WAITING' OR a.status = 'READY')"),
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
index 87458f8e4..19641bbe0 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
@@ -35,6 +35,7 @@ import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.coord.CoordELEvaluator;
 import org.apache.oozie.coord.CoordELFunctions;
 import org.apache.oozie.executor.jpa.CoordActionGetForInputCheckJPAExecutor;
import org.apache.oozie.executor.jpa.CoordActionUpdateForModifiedTimeJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.service.HadoopAccessorException;
@@ -111,6 +112,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
 
         StringBuilder actionXml = new StringBuilder(coordAction.getActionXml());
         Instrumentation.Cron cron = new Instrumentation.Cron();
        boolean isChangeInDependency = false;
         try {
             Configuration actionConf = new XConfiguration(new StringReader(coordAction.getRunConf()));
             cron.start();
@@ -118,7 +120,8 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
             StringBuilder nonExistList = new StringBuilder();
             StringBuilder nonResolvedList = new StringBuilder();
             String firstMissingDependency = "";
            CoordCommandUtils.getResolvedList(coordAction.getMissingDependencies(), nonExistList, nonResolvedList);
            String missingDeps = coordAction.getMissingDependencies();
            CoordCommandUtils.getResolvedList(missingDeps, nonExistList, nonResolvedList);
 
             // For clarity regarding which is the missing dependency in synchronous order
             // instead of printing entire list, some of which, may be available
@@ -134,7 +137,12 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
             if (nonResolvedList.length() > 0 && status == false) {
                 nonExistList.append(CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR).append(nonResolvedList);
             }
            coordAction.setMissingDependencies(nonExistList.toString());
            String nonExistListStr = nonExistList.toString();
            if (!missingDeps.equals(nonExistListStr)) {
                isChangeInDependency = true;
                coordAction.setMissingDependencies(nonExistListStr);
            }
			coordAction.setMissingDependencies(nonExistList.toString());
             if (status == true) {
                 coordAction.setStatus(CoordinatorAction.Status.READY);
                 // pass jobID to the CoordActionReadyXCommand
@@ -161,7 +169,13 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
             cron.stop();
             if(jpaService != null) {
                 try {
                    jpaService.execute(new org.apache.oozie.executor.jpa.CoordActionUpdateForInputCheckJPAExecutor(coordAction));
                    if (isChangeInDependency) {
                        jpaService.execute(new org.apache.oozie.executor.jpa.CoordActionUpdateForInputCheckJPAExecutor(
                                coordAction));
                    }
                    else {
                        jpaService.execute(new CoordActionUpdateForModifiedTimeJPAExecutor(coordAction));
                    }
                 }
                 catch(JPAExecutorException jex) {
                     throw new CommandException(ErrorCode.E1021, jex.getMessage(), jex);
@@ -511,6 +525,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
     /* (non-Javadoc)
      * @see org.apache.oozie.command.XCommand#eagerLoadState()
      */
    // TODO - why loadState() is being called from eagerLoadState();
     @Override
     protected void eagerLoadState() throws CommandException {
         loadState();
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionUpdateForModifiedTimeJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionUpdateForModifiedTimeJPAExecutor.java
new file mode 100644
index 000000000..50aa29fe5
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionUpdateForModifiedTimeJPAExecutor.java
@@ -0,0 +1,72 @@
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

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.util.ParamChecker;

/**
 * Updates the last modified time of CoordinatorAction and persists it.
 * It executes SQL update query and return type is Void.
 */
public class CoordActionUpdateForModifiedTimeJPAExecutor implements JPAExecutor<Void> {

    private CoordinatorActionBean coordAction = null;

    public CoordActionUpdateForModifiedTimeJPAExecutor(CoordinatorActionBean coordAction) {
        ParamChecker.notNull(coordAction, "coordAction");
        this.coordAction = coordAction;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.oozie.executor.jpa.JPAExecutor#execute(javax.persistence.
     * EntityManager)
     */
    @Override
    public Void execute(EntityManager em) throws JPAExecutorException {
        try {
            Query q = em.createNamedQuery("UPDATE_COORD_ACTION_FOR_MODIFIED_DATE");
            q.setParameter("id", coordAction.getId());
            q.setParameter("lastModifiedTime", new Date());
            q.executeUpdate();
            // Since the return type is Void, we have to return null
            return null;
        }
        catch (Exception e) {
            throw new JPAExecutorException(ErrorCode.E0603, e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.oozie.executor.jpa.JPAExecutor#getName()
     */
    @Override
    public String getName() {
        return "CoordActionUpdateForModifiedTimeJPAExecutor";
    }
}
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionUpdateForModifiedTimeJPAExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionUpdateForModifiedTimeJPAExecutor.java
new file mode 100644
index 000000000..0747fba11
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionUpdateForModifiedTimeJPAExecutor.java
@@ -0,0 +1,77 @@
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

import java.util.Date;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XDataTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestCoordActionUpdateForModifiedTimeJPAExecutor extends XDataTestCase {
    Services services;

    @Before
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        services.init();
        cleanUpDBTables();
    }

    @After
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
    }

    @Test
    public void testCoordActionUpdateModifiedTime() throws Exception {
        int actionNum = 1;
        CoordinatorJobBean job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        CoordinatorActionBean action = addRecordToCoordActionTable(job.getId(), actionNum,
                CoordinatorAction.Status.RUNNING, "coord-action-get.xml", 0);
        _testCoordActionUpdateModifiedTime(action);
    }

    private void _testCoordActionUpdateModifiedTime(CoordinatorActionBean action) throws Exception {
        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);
        Date currentDate = new Date();
        assertTrue(currentDate.getTime() - action.getLastModifiedTime().getTime() > 0);
        // Call the JPAUpdate executor to execute the Update command
        CoordActionUpdateForModifiedTimeJPAExecutor coordUpdCmd = new CoordActionUpdateForModifiedTimeJPAExecutor(
                action);
        jpaService.execute(coordUpdCmd);

        CoordActionGetJPAExecutor coordGetCmd = new CoordActionGetJPAExecutor(action.getId());
        CoordinatorActionBean newAction = jpaService.execute(coordGetCmd);

        assertNotNull(newAction);
        assertTrue(newAction.getLastModifiedTime().getTime() - currentDate.getTime() > 0);
    }

}
diff --git a/release-log.txt b/release-log.txt
index c51ddf0a2..6fc277bfb 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.4.0 release (trunk - unreleased)
 
OOZIE-1161 Remove unnecessary db updates for some of the blobs like missing_dependencies' of Coordinator Action (virag)
 OOZIE-1164 typo in toString() method for org.apache.oozie.client.rest.JsonCoordinatorJob.java (bowenzhangusa via rkanter)
 OOZIE-1152 Unit test for JavaActionExecutor has a wrong action XML (jaoki via harsh)
 OOZIE-1144 OOZIE-1137 breaks the sharelib (rkanter)
- 
2.19.1.windows.1

