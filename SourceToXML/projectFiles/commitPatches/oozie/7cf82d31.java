From 7cf82d3122c3d8db45f12f7ae52631d3fefd739f Mon Sep 17 00:00:00 2001
From: mona <mona@unknown>
Date: Thu, 31 Jan 2013 05:55:20 +0000
Subject: [PATCH] OOZIE-1179 coord action in WAITING when no definition of
 dataset in coord job xml (mona)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1440858 13f79535-47bb-0310-9956-ffa450edef68
--
 .../coord/CoordActionInputCheckXCommand.java  |  9 +-
 ...oordActionGetForInputCheckJPAExecutor.java |  2 -
 .../TestCoordActionInputCheckXCommand.java    | 87 +++++++++++++++++++
 .../command/coord/TestCoordRerunXCommand.java |  7 --
 .../org/apache/oozie/test/XDataTestCase.java  |  2 +-
 core/src/test/resources/wf-no-op.xml          | 21 +++++
 6 files changed, 114 insertions(+), 14 deletions(-)
 create mode 100644 core/src/test/resources/wf-no-op.xml

diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
index 19641bbe0..7d06ba780 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
@@ -35,6 +35,7 @@ import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.coord.CoordELEvaluator;
 import org.apache.oozie.coord.CoordELFunctions;
 import org.apache.oozie.executor.jpa.CoordActionGetForInputCheckJPAExecutor;
import org.apache.oozie.executor.jpa.CoordActionUpdateForInputCheckJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionUpdateForModifiedTimeJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
@@ -98,7 +99,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
             // update lastModifiedTime
             coordAction.setLastModifiedTime(new Date());
             try {
                jpaService.execute(new org.apache.oozie.executor.jpa.CoordActionUpdateForInputCheckJPAExecutor(coordAction));
                jpaService.execute(new CoordActionUpdateForInputCheckJPAExecutor(coordAction));
             }
             catch (JPAExecutorException e) {
                 throw new CommandException(e);
@@ -138,7 +139,8 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
                 nonExistList.append(CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR).append(nonResolvedList);
             }
             String nonExistListStr = nonExistList.toString();
            if (!missingDeps.equals(nonExistListStr)) {
            if (!nonExistListStr.equals(missingDeps) || missingDeps.isEmpty()) {
                // missingDeps empty means action should become READY
                 isChangeInDependency = true;
                 coordAction.setMissingDependencies(nonExistListStr);
             }
@@ -170,8 +172,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
             if(jpaService != null) {
                 try {
                     if (isChangeInDependency) {
                        jpaService.execute(new org.apache.oozie.executor.jpa.CoordActionUpdateForInputCheckJPAExecutor(
                                coordAction));
                        jpaService.execute(new CoordActionUpdateForInputCheckJPAExecutor(coordAction));
                     }
                     else {
                         jpaService.execute(new CoordActionUpdateForModifiedTimeJPAExecutor(coordAction));
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInputCheckJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInputCheckJPAExecutor.java
index be1c44323..aa7c94f68 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInputCheckJPAExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInputCheckJPAExecutor.java
@@ -18,8 +18,6 @@
 package org.apache.oozie.executor.jpa;
 
 import java.sql.Timestamp;
import java.util.List;

 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
index b11109602..f061524b9 100644
-- a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
@@ -445,6 +445,93 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
         assertEquals(testedValue, effectiveValue);
     }
 
    /**
     * This test verifies that for a coordinator with no input dependencies
     * action is not stuck in WAITING
     *
     * @throws Exception
     */
    public void testNoDatasetDependency() throws Exception {
        /*
         * create coordinator job
         */
        CoordinatorJobBean coordJob = new CoordinatorJobBean();
        coordJob.setId("0000000" + new Date().getTime() + "-TestCoordActionInputCheckXCommand-C");
        coordJob.setAppName("testApp");
        coordJob.setAppPath("testAppPath");
        coordJob.setStatus(CoordinatorJob.Status.RUNNING);
        coordJob.setCreatedTime(new Date());
        coordJob.setLastModifiedTime(new Date());
        coordJob.setUser("testUser");
        coordJob.setGroup("testGroup");
        coordJob.setAuthToken("notoken");
        coordJob.setTimeZone("UTC");
        coordJob.setTimeUnit(Timeunit.DAY);
        coordJob.setMatThrottling(2);
        try {
            coordJob.setStartTime(DateUtils.parseDateOozieTZ("2009-02-01T23:59" + TZ));
            coordJob.setEndTime(DateUtils.parseDateOozieTZ("2009-02-02T23:59" + TZ));
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Could not set Date/time");
        }
        XConfiguration jobConf = new XConfiguration();
        jobConf.set(OozieClient.USER_NAME, getTestUser());
        String confStr = jobConf.toXmlString(false);
        coordJob.setConf(confStr);
        String wfXml = IOUtils.getResourceAsString("wf-no-op.xml", -1);
        writeToFile(wfXml, getFsTestCaseDir(), "workflow.xml");
        String appXml = "<coordinator-app xmlns='uri:oozie:coordinator:0.2' name='NAME' frequency=\"1\" start='2009-02-01T01:00"
                + TZ + "' end='2009-02-03T23:59" + TZ + "' timezone='UTC' freq_timeunit='DAY' end_of_duration='NONE'>";
        appXml += "<output-events>";
        appXml += "<data-out name='LOCAL_A' dataset='local_a'>";
        appXml += "<dataset name='local_a' frequency='7' initial-instance='2009-01-01T01:00" + TZ
                + "' timezone='UTC' freq_timeunit='DAY' end_of_duration='NONE'>";
        appXml += "<uri-template>file://" + getFsTestCaseDir() + "/${YEAR}/${MONTH}/${DAY}</uri-template>";
        appXml += "</dataset>";
        appXml += "<start-instance>${coord:current(-3)}</start-instance>";
        appXml += "<instance>${coord:current(0)}</instance>";
        appXml += "</data-out>";
        appXml += "</output-events>";
        appXml += "<action>";
        appXml += "<workflow>";
        appXml += "<app-path>" + getFsTestCaseDir() + "/workflow.xml</app-path>";
        appXml += "</workflow>";
        appXml += "</action>";
        appXml += "</coordinator-app>";
        coordJob.setJobXml(appXml);
        coordJob.setLastActionNumber(0);
        coordJob.setFrequency(1);
        coordJob.setConcurrency(1);
        JPAService jpaService = Services.get().get(JPAService.class);
        if (jpaService != null) {
            try {
                jpaService.execute(new CoordJobInsertJPAExecutor(coordJob));
            }
            catch (JPAExecutorException e) {
                throw new CommandException(e);
            }
        }
        else {
            fail("Unable to insert the test job record to table");
        }
        new CoordMaterializeTransitionXCommand(coordJob.getId(), 3600).call();
        /*
         * check coord action READY
         */
        new CoordActionInputCheckXCommand(coordJob.getId() + "@1", coordJob.getId()).call();
        CoordinatorActionBean action = null;
        try {
            jpaService = Services.get().get(JPAService.class);
            action = jpaService.execute(new CoordActionGetJPAExecutor(coordJob.getId() + "@1"));
        }
        catch (JPAExecutorException se) {
            fail("Action ID " + coordJob.getId() + "@1" + " was not stored properly in db");
        }
        assertEquals(action.getStatus(), CoordinatorAction.Status.READY);
    }

     protected CoordinatorJobBean addRecordToCoordJobTableForWaiting(String testFileName, CoordinatorJob.Status status,
             Date start, Date end, boolean pending, boolean doneMatd, int lastActionNum) throws Exception {
 
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordRerunXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordRerunXCommand.java
index 1c1fdf549..c9f6c5510 100644
-- a/core/src/test/java/org/apache/oozie/command/coord/TestCoordRerunXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordRerunXCommand.java
@@ -1101,13 +1101,6 @@ public class TestCoordRerunXCommand extends XDataTestCase {
         return conf;
     }
 
    private void writeToFile(String content, Path appPath, String fileName) throws IOException {
        FileSystem fs = getFileSystem();
        Writer writer = new OutputStreamWriter(fs.create(new Path(appPath, fileName), true));
        writer.write(content);
        writer.close();
    }

     @SuppressWarnings("unchecked")
     private String[] getActionXmlUrls(Element eAction, String user, String group) {
         Element outputList = eAction.getChild("input-events", eAction.getNamespace());
diff --git a/core/src/test/java/org/apache/oozie/test/XDataTestCase.java b/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
index 99052de30..05744270f 100644
-- a/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
++ b/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
@@ -927,7 +927,7 @@ public abstract class XDataTestCase extends XFsTestCase {
         return jobConf;
     }
 
    private void writeToFile(String content, Path appPath, String fileName) throws IOException {
    protected void writeToFile(String content, Path appPath, String fileName) throws IOException {
         FileSystem fs = getFileSystem();
         Writer writer = new OutputStreamWriter(fs.create(new Path(appPath, fileName), true));
         writer.write(content);
diff --git a/core/src/test/resources/wf-no-op.xml b/core/src/test/resources/wf-no-op.xml
new file mode 100644
index 000000000..b53922c8f
-- /dev/null
++ b/core/src/test/resources/wf-no-op.xml
@@ -0,0 +1,21 @@
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<workflow-app xmlns="uri:oozie:workflow:0.2" name="no-op-wf">
    <start to="end"/>
    <end name="end"/>
</workflow-app>
- 
2.19.1.windows.1

