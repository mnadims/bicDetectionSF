From cbb1eac9db427db0047847e3ed5564979be5571c Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Mon, 16 Jun 2014 17:30:12 -0700
Subject: [PATCH] OOZIE-1879 Workflow Rerun causes error depending on the order
 of forked nodes (rkanter)

--
 .../org/apache/oozie/WorkflowActionBean.java  |  2 +-
 .../oozie/command/wf/ReRunXCommand.java       | 10 ++-
 .../jpa/WorkflowActionQueryExecutor.java      |  1 +
 .../apache/oozie/workflow/WorkflowLib.java    |  8 ++-
 .../workflow/lite/LiteWorkflowInstance.java   | 72 +++++++++++++++++++
 .../oozie/workflow/lite/LiteWorkflowLib.java  |  7 +-
 .../oozie/command/wf/TestReRunXCommand.java   | 68 ++++++++++++++++++
 core/src/test/resources/rerun-wf-fork.xml     | 63 ++++++++++++++++
 release-log.txt                               |  1 +
 9 files changed, 226 insertions(+), 6 deletions(-)
 create mode 100644 core/src/test/resources/rerun-wf-fork.xml

diff --git a/core/src/main/java/org/apache/oozie/WorkflowActionBean.java b/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
index 686199518..e64e9bf6a 100644
-- a/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
++ b/core/src/main/java/org/apache/oozie/WorkflowActionBean.java
@@ -109,7 +109,7 @@ import org.json.simple.JSONObject;
 
     @NamedQuery(name = "GET_RETRY_MANUAL_ACTIONS", query = "select OBJECT(a) from WorkflowActionBean a where a.wfId = :wfId AND (a.statusStr = 'START_RETRY' OR a.statusStr = 'START_MANUAL' OR a.statusStr = 'END_RETRY' OR a.statusStr = 'END_MANUAL')"),
 
    @NamedQuery(name = "GET_ACTIONS_FOR_WORKFLOW_RERUN", query = "select a.id, a.name, a.statusStr from WorkflowActionBean a where a.wfId = :wfId order by a.startTimestamp") })
    @NamedQuery(name = "GET_ACTIONS_FOR_WORKFLOW_RERUN", query = "select a.id, a.name, a.statusStr, a.endTimestamp from WorkflowActionBean a where a.wfId = :wfId order by a.startTimestamp") })
 @Table(name = "WF_ACTIONS")
 public class WorkflowActionBean implements Writable, WorkflowAction, JsonBean {
     @Id
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
index fe588d4da..5dd06ca0c 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
@@ -158,8 +158,16 @@ public class ReRunXCommand extends WorkflowXCommand<Void> {
             // Resetting the conf to contain all the resolved values is necessary to ensure propagation of Oozie properties to Hadoop calls downstream
             conf = ((XConfiguration) conf).resolve();
 
            // Prepare the action endtimes map
            Map<String, Date> actionEndTimes = new HashMap<String, Date>();
            for (WorkflowActionBean action : actions) {
                if (action.getEndTime() != null) {
                    actionEndTimes.put(action.getName(), action.getEndTime());
                }
            }

             try {
                newWfInstance = workflowLib.createInstance(app, conf, jobId);
                newWfInstance = workflowLib.createInstance(app, conf, jobId, actionEndTimes);
             }
             catch (WorkflowException e) {
                 throw new CommandException(e);
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
index 9156a27c0..0c323a379 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/WorkflowActionQueryExecutor.java
@@ -364,6 +364,7 @@ public class WorkflowActionQueryExecutor extends
                 bean.setId((String) arr[0]);
                 bean.setName((String) arr[1]);
                 bean.setStatusStr((String) arr[2]);
                bean.setEndTime(DateUtils.toDate((Timestamp) arr[3]));
                 break;
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct action bean for "
diff --git a/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java b/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java
index 7e4c90a66..e79e59d87 100644
-- a/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java
++ b/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java
@@ -17,6 +17,8 @@
  */
 package org.apache.oozie.workflow;
 
import java.util.Date;
import java.util.Map;
 import org.apache.hadoop.conf.Configuration;
 
 
@@ -50,15 +52,17 @@ public interface WorkflowLib {
     public WorkflowInstance createInstance(WorkflowApp app, Configuration conf) throws WorkflowException;
 
     /**
     * Create a workflow instance with the given wfId. This will be used for re-running workflows.
     * Create a workflow instance with the given wfId and actions endtime map. This will be used for re-running workflows.
      *
      * @param app application to create a workflow instance of.
      * @param conf job configuration.
      * @param wfId Workflow ID.
     * @param actionEndTimes A map of the actions to their endtimes; actions with no endtime should be omitted
      * @return the newly created workflow instance.
      * @throws WorkflowException thrown if the instance could not be created.
      */
    public WorkflowInstance createInstance(WorkflowApp app, Configuration conf, String wfId) throws WorkflowException;
    public WorkflowInstance createInstance(WorkflowApp app, Configuration conf, String wfId, Map<String, Date> actionEndTimes)
            throws WorkflowException;
 
     /**
      * Insert a workflow instance in storage.
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java
index bf8dc05d5..a5db84a2d 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowInstance.java
@@ -37,6 +37,10 @@ import java.io.IOException;
 import java.io.ByteArrayOutputStream;
 import java.io.ByteArrayInputStream;
 import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
@@ -45,6 +49,8 @@ import java.util.Map;
 public class LiteWorkflowInstance implements Writable, WorkflowInstance {
     private static final String TRANSITION_TO = "transition.to";
 
    private final Date FAR_INTO_THE_FUTURE = new Date(Long.MAX_VALUE);

     private XLog log;
 
     private static String PATH_SEPARATOR = "/";
@@ -154,6 +160,7 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
     private Map<String, NodeInstance> executionPaths = new HashMap<String, NodeInstance>();
     private Map<String, String> persistentVars = new HashMap<String, String>();
     private Map<String, Object> transientVars = new HashMap<String, Object>();
    private ActionEndTimesComparator actionEndTimesComparator = null;
 
     protected LiteWorkflowInstance() {
         log = XLog.getLog(getClass());
@@ -168,6 +175,11 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
         status = Status.PREP;
     }
 
    public LiteWorkflowInstance(LiteWorkflowApp def, Configuration conf, String instanceId, Map<String, Date> actionEndTimes) {
        this(def, conf, instanceId);
        actionEndTimesComparator = new ActionEndTimesComparator(actionEndTimes);
    }

     public synchronized boolean start() throws WorkflowException {
         if (status != Status.PREP) {
             throw new WorkflowException(ErrorCode.E0719);
@@ -294,6 +306,16 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
                                 }
 
                             }

                            // If we're doing a rerun, then we need to make sure to put the actions in pathToStart into the order
                            // that they ended in.  Otherwise, it could result in an error later on in some edge cases.
                            // e.g. You have a fork with two nodes, A and B, that both succeeded, followed by a join and some more
                            // nodes, some of which failed.  If you do the rerun, it will always signal A and then B, even if in the
                            // original run B signaled first and then A.  By sorting this, we maintain the proper signal ordering.
                            if (actionEndTimesComparator != null && pathsToStart.size() > 1) {
                                Collections.sort(pathsToStart, actionEndTimesComparator);
                            }

                             // signal all new synch transitions
                             for (String pathToStart : pathsToStart) {
                                 signal(pathToStart, "::synch::");
@@ -585,6 +607,14 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
             dOut.writeUTF(entry.getKey());
             writeStringAsBytes(entry.getValue(), dOut);
         }
        if (actionEndTimesComparator != null) {
            Map<String, Date> actionEndTimes = actionEndTimesComparator.getActionEndTimes();
            dOut.writeInt(actionEndTimes.size());
            for (Map.Entry<String, Date> entry : actionEndTimes.entrySet()) {
                dOut.writeUTF(entry.getKey());
                dOut.writeLong(entry.getValue().getTime());
            }
        }
     }
 
     @Override
@@ -616,6 +646,21 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
             String vVal = readBytesAsString(dIn);
             persistentVars.put(vName, vVal);
         }
        int numActionEndTimes = -1;
        try {
            numActionEndTimes = dIn.readInt();
        } catch (IOException ioe) {
            // This means that there isn't an actionEndTimes, so just ignore
        }
        if (numActionEndTimes > 0) {
            Map<String, Date> actionEndTimes = new HashMap<String, Date>(numActionEndTimes);
            for (int x = 0; x < numActionEndTimes; x++) {
                String name = dIn.readUTF();
                long endTime = dIn.readLong();
                actionEndTimes.put(name, new Date(endTime));
            }
            actionEndTimesComparator = new ActionEndTimesComparator(actionEndTimes);
        }
         refreshLog();
     }
 
@@ -671,4 +716,31 @@ public class LiteWorkflowInstance implements Writable, WorkflowInstance {
         return instanceId.hashCode();
     }
 
    private class ActionEndTimesComparator implements Comparator<String> {

        private final Map<String, Date> actionEndTimes;

        public ActionEndTimesComparator(Map<String, Date> actionEndTimes) {
            this.actionEndTimes = actionEndTimes;
        }

        @Override
        public int compare(String node1, String node2) {
            Date date1 = FAR_INTO_THE_FUTURE;
            Date date2 = FAR_INTO_THE_FUTURE;
            NodeInstance node1Instance = executionPaths.get(node1);
            if (node1Instance != null) {
                date1 = this.actionEndTimes.get(node1Instance.nodeName);
            }
            NodeInstance node2Instance = executionPaths.get(node2);
            if (node2Instance != null) {
                date2 = this.actionEndTimes.get(node2Instance.nodeName);
            }
            return date1.compareTo(date2);
        }

        public Map<String, Date> getActionEndTimes() {
            return actionEndTimes;
        }
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java
index 7f6f1ccab..0e0aefdc7 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java
@@ -30,6 +30,8 @@ import org.apache.hadoop.conf.Configuration;
 
 import javax.xml.validation.Schema;
 import java.io.StringReader;
import java.util.Date;
import java.util.Map;
 
 //TODO javadoc
 public abstract class LiteWorkflowLib implements WorkflowLib {
@@ -63,9 +65,10 @@ public abstract class LiteWorkflowLib implements WorkflowLib {
     }
 
     @Override
    public WorkflowInstance createInstance(WorkflowApp app, Configuration conf, String wfId) throws WorkflowException {
    public WorkflowInstance createInstance(WorkflowApp app, Configuration conf, String wfId, Map<String, Date> actionEndTimes)
            throws WorkflowException {
         ParamChecker.notNull(app, "app");
         ParamChecker.notNull(wfId, "wfId");
        return new LiteWorkflowInstance((LiteWorkflowApp) app, conf, wfId);
        return new LiteWorkflowInstance((LiteWorkflowApp) app, conf, wfId, actionEndTimes);
     }
 }
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
index 1688dc926..5bae614af 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
@@ -24,13 +24,16 @@ import java.io.FileWriter;
 import java.io.IOException;
 import java.io.Reader;
 import java.io.Writer;
import java.util.List;
 import org.apache.hadoop.fs.Path;
 import org.apache.oozie.local.LocalOozie;
import org.apache.oozie.action.hadoop.ShellActionExecutor;
 import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.CoordinatorJob;
 import org.apache.oozie.client.WorkflowJob;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.OozieClientException;
import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.command.coord.CoordActionStartXCommand;
 import org.apache.oozie.executor.jpa.CoordActionGetJPAExecutor;
 import org.apache.oozie.test.XDataTestCase;
@@ -39,7 +42,9 @@ import org.apache.oozie.util.IOUtils;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.service.ActionService;
 import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.SchemaService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.XLogService;
 
@@ -110,6 +115,69 @@ public class TestReRunXCommand extends XDataTestCase {
         assertEquals(WorkflowJob.Status.SUCCEEDED, wfClient.getJobInfo(jobId1).getStatus());
     }
 
    /**
     * This tests a specific edge case where rerun can fail when there's a fork, the actions in the fork succeed, but an action
     * after the fork fails.  Previously, the rerun would step through the forked actions in the order they were listed in the
     * fork action's XML; if they happened to finish in a different order, this would cause an error during rerun.  This is fixed by
     * enforcing the same order in LiteWorkflowInstance#signal, which this test verifies.
     *
     * @throws Exception
     */
    public void testRerunFork() throws Exception {
        // We need the shell schema and action for this test
        Services.get().getConf().set(ActionService.CONF_ACTION_EXECUTOR_EXT_CLASSES, ShellActionExecutor.class.getName());
        Services.get().setService(ActionService.class);
        Services.get().getConf().set(SchemaService.WF_CONF_EXT_SCHEMAS, "shell-action-0.3.xsd");
        Services.get().setService(SchemaService.class);

        Reader reader = IOUtils.getResourceAsReader("rerun-wf-fork.xml", -1);
        Writer writer = new FileWriter(new File(getTestCaseDir(), "workflow.xml"));
        IOUtils.copyCharStream(reader, writer);

        final OozieClient wfClient = LocalOozie.getClient();
        Properties conf = wfClient.createConfiguration();
        conf.setProperty("nameNode", getNameNodeUri());
        conf.setProperty("jobTracker", getJobTrackerUri());
        conf.setProperty(OozieClient.APP_PATH, getTestCaseFileUri("workflow.xml"));
        conf.setProperty(OozieClient.USER_NAME, getTestUser());
        conf.setProperty("cmd3", "echo1");      // expected to fail

        final String jobId1 = wfClient.submit(conf);
        wfClient.start(jobId1);
        waitFor(40 * 1000, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                return wfClient.getJobInfo(jobId1).getStatus() == WorkflowJob.Status.KILLED;
            }
        });
        assertEquals(WorkflowJob.Status.KILLED, wfClient.getJobInfo(jobId1).getStatus());
        List<WorkflowAction> actions = wfClient.getJobInfo(jobId1).getActions();
        assertEquals(WorkflowAction.Status.OK, actions.get(1).getStatus());     // fork
        assertEquals(WorkflowAction.Status.OK, actions.get(2).getStatus());     // sh1
        assertEquals(WorkflowAction.Status.OK, actions.get(3).getStatus());     // sh2
        assertEquals(WorkflowAction.Status.OK, actions.get(4).getStatus());     // join
        assertEquals(WorkflowAction.Status.ERROR, actions.get(5).getStatus());  // sh3

        // rerun failed node, which is after the fork
        conf.setProperty(OozieClient.RERUN_FAIL_NODES, "true");
        conf.setProperty("cmd3", "echo");      // expected to succeed

        wfClient.reRun(jobId1, conf);
        waitFor(40 * 1000, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                return wfClient.getJobInfo(jobId1).getStatus() == WorkflowJob.Status.SUCCEEDED;
            }
        });
        assertEquals(WorkflowJob.Status.SUCCEEDED, wfClient.getJobInfo(jobId1).getStatus());
        actions = wfClient.getJobInfo(jobId1).getActions();
        assertEquals(WorkflowAction.Status.OK, actions.get(1).getStatus());     // fork
        assertEquals(WorkflowAction.Status.OK, actions.get(2).getStatus());     // sh1
        assertEquals(WorkflowAction.Status.OK, actions.get(3).getStatus());     // sh2
        assertEquals(WorkflowAction.Status.OK, actions.get(4).getStatus());     // join
        assertEquals(WorkflowAction.Status.OK, actions.get(5).getStatus());     // sh3
    }

     /*
      * Test to ensure parameterized configuration variables get resolved in workflow rerun
      */
diff --git a/core/src/test/resources/rerun-wf-fork.xml b/core/src/test/resources/rerun-wf-fork.xml
new file mode 100644
index 000000000..8fa8f34fd
-- /dev/null
++ b/core/src/test/resources/rerun-wf-fork.xml
@@ -0,0 +1,63 @@
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
<workflow-app xmlns="uri:oozie:workflow:0.4" name="rerun-wf-fork">
    <global>
       <job-tracker>${jobTracker}</job-tracker>
       <name-node>${nameNode}</name-node>
    </global>

    <start to="f"/>

    <fork name="f">
        <path start="sh1"/>
        <path start="sh2"/>
    </fork>

    <action name="sh1">
        <shell xmlns="uri:oozie:shell-action:0.3">
            <exec>sleep</exec>
            <argument>15</argument>
        </shell>
        <ok to="j"/>
        <error to="k"/>
    </action>

    <action name="sh2">
        <shell xmlns="uri:oozie:shell-action:0.3">
            <exec>echo</exec>
        </shell>
        <ok to="j"/>
        <error to="k"/>
    </action>

    <join name="j" to="sh3"/>

    <action name="sh3">
        <shell xmlns="uri:oozie:shell-action:0.3">
            <exec>${cmd3}</exec>
        </shell>
        <ok to="end"/>
        <error to="k"/>
    </action>

    <kill name="k">
        <message>kill</message>
    </kill>

    <end name="end"/>
</workflow-app>
diff --git a/release-log.txt b/release-log.txt
index 74416783d..43a5aad0d 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1879 Workflow Rerun causes error depending on the order of forked nodes (rkanter)
 OOZIE-1659 oozie-site is missing email-action-0.2 schema (jagatsingh via rkanter)
 OOZIE-1492 Make sure HA works with HCat (ryota)
 OOZIE-1869 Sharelib update shows vip/load balancer address as one of the hostname (puru via ryota)
- 
2.19.1.windows.1

