From 0c90b8c3629ffbf98fdafa5081979a7d42aaf056 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Mon, 9 Mar 2015 14:44:12 -0700
Subject: [PATCH] OOZIE-2126 SSH action can be too fast for Oozie sometimes
 (rkanter)

--
 .../main/java/org/apache/oozie/ErrorCode.java |   2 +
 .../command/wf/CompletedActionXCommand.java   |  38 +++-
 .../apache/oozie/service/CallbackService.java |   7 +
 core/src/main/resources/oozie-default.xml     |   9 +
 .../wf/TestCompletedActionXCommand.java       | 200 ++++++++++++++++++
 .../service/TestConfigurationService.java     |   1 +
 release-log.txt                               |   1 +
 7 files changed, 251 insertions(+), 7 deletions(-)
 create mode 100644 core/src/test/java/org/apache/oozie/command/wf/TestCompletedActionXCommand.java

diff --git a/core/src/main/java/org/apache/oozie/ErrorCode.java b/core/src/main/java/org/apache/oozie/ErrorCode.java
index 7630c2f66..2fd2e9995 100644
-- a/core/src/main/java/org/apache/oozie/ErrorCode.java
++ b/core/src/main/java/org/apache/oozie/ErrorCode.java
@@ -174,6 +174,8 @@ public enum ErrorCode {
     E0818(XLog.STD, "Action [{0}] status is running but WF Job [{1}] status is [{2}]. Expected status is RUNNING or SUSPENDED."),
     E0819(XLog.STD, "Unable to delete the temp dir of job WF Job [{0}]."),
     E0820(XLog.STD, "Action user retry max [{0}] is over system defined max [{1}], re-assign to use system max."),
    E0821(XLog.STD, "Received early callback for action still in PREP state; will wait [{0}]ms and requeue up to [{1}] more times"),
    E0822(XLog.STD, "Received early callback for action [{0}] while still in PREP state and exhausted all requeues"),
 
     E0900(XLog.OPS, "JobTracker [{0}] not allowed, not in Oozie's whitelist. Allowed values are: {1}"),
     E0901(XLog.OPS, "NameNode [{0}] not allowed, not in Oozie's whitelist. Allowed values are: {1}"),
diff --git a/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
index b1226ccf8..bc39bce14 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/CompletedActionXCommand.java
@@ -28,6 +28,7 @@ import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.service.ActionService;
import org.apache.oozie.service.CallbackService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.LogUtils;
 import org.apache.oozie.util.ParamChecker;
@@ -39,11 +40,18 @@ public class CompletedActionXCommand extends WorkflowXCommand<Void> {
     private final String actionId;
     private final String externalStatus;
     private WorkflowActionBean wfactionBean;
    private int earlyRequeueCount;
 
    public CompletedActionXCommand(String actionId, String externalStatus, Properties actionData, int priority) {
    public CompletedActionXCommand(String actionId, String externalStatus, Properties actionData, int priority,
                                   int earlyRequeueCount) {
         super("callback", "callback", priority);
         this.actionId = ParamChecker.notEmpty(actionId, "actionId");
         this.externalStatus = ParamChecker.notEmpty(externalStatus, "externalStatus");
        this.earlyRequeueCount = earlyRequeueCount;
    }

    public CompletedActionXCommand(String actionId, String externalStatus, Properties actionData, int priority) {
        this(actionId, externalStatus, actionData, 1, 0);
     }
 
     public CompletedActionXCommand(String actionId, String externalStatus, Properties actionData) {
@@ -79,7 +87,8 @@ public class CompletedActionXCommand extends WorkflowXCommand<Void> {
      */
     @Override
     protected void eagerVerifyPrecondition() throws CommandException, PreconditionException {
        if (this.wfactionBean.getStatus() != WorkflowActionBean.Status.RUNNING) {
        if (this.wfactionBean.getStatus() != WorkflowActionBean.Status.RUNNING
                && this.wfactionBean.getStatus() != WorkflowActionBean.Status.PREP) {
             throw new CommandException(ErrorCode.E0800, actionId, this.wfactionBean.getStatus());
         }
     }
@@ -91,11 +100,26 @@ public class CompletedActionXCommand extends WorkflowXCommand<Void> {
      */
     @Override
     protected Void execute() throws CommandException {
        ActionExecutor executor = Services.get().get(ActionService.class).getExecutor(this.wfactionBean.getType());
        // this is done because oozie notifications (of sub-wfs) is send
        // every status change, not only on completion.
        if (executor.isCompleted(externalStatus)) {
            queue(new ActionCheckXCommand(this.wfactionBean.getId(), getPriority(), -1));
        // If the action is still in PREP, we probably received a callback before Oozie was able to update from PREP to RUNNING;
        // we'll requeue this command a few times and hope that it switches to RUNNING before giving up
        if (this.wfactionBean.getStatus() == WorkflowActionBean.Status.PREP) {
            int maxEarlyRequeueCount = Services.get().get(CallbackService.class).getEarlyRequeueMaxRetries();
            if (this.earlyRequeueCount < maxEarlyRequeueCount) {
                long delay = getRequeueDelay();
                LOG.warn("Received early callback for action still in PREP state; will wait [{0}]ms and requeue up to [{1}] more"
                        + " times", delay, (maxEarlyRequeueCount - earlyRequeueCount));
                queue(new CompletedActionXCommand(this.actionId, this.externalStatus, null, this.getPriority(),
                        this.earlyRequeueCount + 1), delay);
            } else {
                throw new CommandException(ErrorCode.E0822, actionId);
            }
        } else {    // RUNNING
            ActionExecutor executor = Services.get().get(ActionService.class).getExecutor(this.wfactionBean.getType());
            // this is done because oozie notifications (of sub-wfs) is send
            // every status change, not only on completion.
            if (executor.isCompleted(externalStatus)) {
                queue(new ActionCheckXCommand(this.wfactionBean.getId(), getPriority(), -1));
            }
         }
         return null;
     }
diff --git a/core/src/main/java/org/apache/oozie/service/CallbackService.java b/core/src/main/java/org/apache/oozie/service/CallbackService.java
index 7fa07f18f..405701d76 100644
-- a/core/src/main/java/org/apache/oozie/service/CallbackService.java
++ b/core/src/main/java/org/apache/oozie/service/CallbackService.java
@@ -36,7 +36,10 @@ public class CallbackService implements Service {
 
     public static final String CONF_BASE_URL = CONF_PREFIX + "base.url";
 
    public static final String CONF_EARLY_REQUEUE_MAX_RETRIES = CONF_PREFIX + "early.requeue.max.retries";

     private Configuration oozieConf;
    private int earlyRequeueMaxRetries;
 
     /**
      * Initialize the service.
@@ -45,6 +48,7 @@ public class CallbackService implements Service {
      */
     public void init(Services services) {
         oozieConf = services.getConf();
        earlyRequeueMaxRetries = ConfigurationService.getInt(CONF_EARLY_REQUEUE_MAX_RETRIES);
     }
 
     /**
@@ -132,4 +136,7 @@ public class CallbackService implements Service {
         }
     }
 
    public int getEarlyRequeueMaxRetries() {
        return earlyRequeueMaxRetries;
    }
 }
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 98433300e..cb65502e0 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -1518,6 +1518,15 @@
         </description>
     </property>
 
    <property>
        <name>oozie.service.CallbackService.early.requeue.max.retries</name>
        <value>5</value>
        <description>
            If Oozie receives a callback too early (while the action is in PREP state), it will requeue the command this many times
            to give the action time to transition to RUNNING.
        </description>
    </property>

     <!-- CallbackServlet -->
 
     <property>
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestCompletedActionXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestCompletedActionXCommand.java
new file mode 100644
index 000000000..a4f0e837d
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/command/wf/TestCompletedActionXCommand.java
@@ -0,0 +1,200 @@
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

package org.apache.oozie.command.wf;

import org.apache.oozie.WorkflowActionBean;
import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.client.WorkflowJob;
import org.apache.oozie.command.XCommand;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.service.InstrumentationService;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.Instrumentation;
import org.apache.oozie.workflow.WorkflowInstance;

public class TestCompletedActionXCommand extends XDataTestCase {
    private Services services;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        services.init();
    }

    @Override
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
    }

    public void testEarlyCallbackTimeout() throws Exception {
        final Instrumentation inst = Services.get().get(InstrumentationService.class).get();

        WorkflowJobBean job = addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
        WorkflowActionBean action = addRecordToWfActionTable(job.getId(), "1", WorkflowAction.Status.PREP);
        final CompletedActionXCommand cmd = new CompletedActionXCommand(action.getId(), "SUCCEEDED", null);

        long xexceptionCount;
        try {
            xexceptionCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                    + ".xexceptions").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            xexceptionCount = 0L;
        }
        assertEquals(0L, xexceptionCount);

        long executionsCount;
        try {
            executionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                    + ".executions").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            executionsCount = 0L;
        }
        assertEquals(0L, executionsCount);

        long executionCount;
        try {
            executionCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                    + ".execution").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            executionCount = 0L;
        }
        assertEquals(0L, executionCount);

        cmd.call();
        int timeout = 10000 * 5 * 2;
        waitFor(timeout, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                long xexceptionCount;
                try {
                    xexceptionCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                            + ".xexceptions").getValue();
                } catch(NullPointerException npe) {
                    //counter might be null
                    xexceptionCount = 0L;
                }
                return (xexceptionCount == 1L);
            }
        });
        executionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                + ".executions").getValue();
        assertEquals(6L, executionsCount);
        try {
            executionCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                    + ".execution").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            executionCount = 0L;
        }
        assertEquals(0L, executionCount);
        xexceptionCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                + ".xexceptions").getValue();
        assertEquals(1L, xexceptionCount);
    }

    public void testEarlyCallbackTransitionToRunning() throws Exception {
        final Instrumentation inst = Services.get().get(InstrumentationService.class).get();

        WorkflowJobBean job = addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
        final WorkflowActionBean action = addRecordToWfActionTable(job.getId(), "1", WorkflowAction.Status.PREP);
        final CompletedActionXCommand cmd = new CompletedActionXCommand(action.getId(), "SUCCEEDED", null);

        long xexceptionCount;
        try {
            xexceptionCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                    + ".xexceptions").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            xexceptionCount = 0L;
        }
        assertEquals(0L, xexceptionCount);

        long executionsCount;
        try {
            executionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                    + ".executions").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            executionsCount = 0L;
        }
        assertEquals(0L, executionsCount);

        long checkXCommandExecutionsCount;
        try {
            checkXCommandExecutionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(
                    "action.check.executions").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            checkXCommandExecutionsCount = 0L;
        }
        assertEquals(0L, checkXCommandExecutionsCount);

        cmd.call();
        int timeout = 100000 * 5 * 2;
        waitFor(timeout, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                long executionsCount;
                try {
                    executionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                            + ".executions").getValue();
                } catch (NullPointerException npe){
                    //counter might be null
                    executionsCount = 0L;
                }
                if (executionsCount == 3 && !action.getStatus().equals(WorkflowAction.Status.RUNNING)) {
                    // Transition the action to RUNNING
                    action.setStatus(WorkflowAction.Status.RUNNING);
                    WorkflowActionQueryExecutor.getInstance().executeUpdate(
                            WorkflowActionQueryExecutor.WorkflowActionQuery.UPDATE_ACTION, action);
                }
                long checkXCommandExecutionsCount;
                try {
                    checkXCommandExecutionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(
                            "action.check.executions").getValue();
                } catch (NullPointerException npe){
                    //counter might be null
                    checkXCommandExecutionsCount = 0L;
                }
                return (checkXCommandExecutionsCount == 1L);
            }
        });
        executionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                + ".executions").getValue();
        assertTrue("expected a value greater than 3L, but found " + executionsCount, executionsCount >= 3L);
        checkXCommandExecutionsCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(
                    "action.check.executions").getValue();
        assertEquals(1L, checkXCommandExecutionsCount);
        try {
            xexceptionCount = inst.getCounters().get(XCommand.INSTRUMENTATION_GROUP).get(cmd.getName()
                    + ".xexceptions").getValue();
        } catch (NullPointerException npe){
            //counter might be null
            xexceptionCount = 0L;
        }
        assertEquals(0L, xexceptionCount);
    }
}
diff --git a/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java b/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java
index b1dde2c19..ddb3d58f3 100644
-- a/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java
++ b/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java
@@ -174,6 +174,7 @@ public class TestConfigurationService extends XTestCase {
         assertEquals(ConfigUtils.STRING_DEFAULT, ConfigurationService.get(testConf, "test.nonexist"));
 
         assertEquals("http://localhost:8080/oozie/callback", ConfigurationService.get(CallbackService.CONF_BASE_URL));
        assertEquals(5, ConfigurationService.getInt(CallbackService.CONF_EARLY_REQUEUE_MAX_RETRIES));
         assertEquals("gz", ConfigurationService.get(CodecFactory.COMPRESSION_OUTPUT_CODEC));
         assertEquals(4096, ConfigurationService.getInt(XLogStreamingService.STREAM_BUFFER_LEN));
         assertEquals(10000,  ConfigurationService.getLong(JvmPauseMonitorService.WARN_THRESHOLD_KEY));
diff --git a/release-log.txt b/release-log.txt
index 1e6e101c6..6ab5737e4 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-2126 SSH action can be too fast for Oozie sometimes (rkanter)
 OOZIE-2142 Changing the JT whitelist causes running Workflows to stay RUNNING forever (rkanter)
 OOZIE-2164 make master parameterizable in Spark action example (wypoon via rkanter)
 OOZIE-2155 Incorrect DST Shifts are occurring based on the Database timezone (rkanter)
- 
2.19.1.windows.1

