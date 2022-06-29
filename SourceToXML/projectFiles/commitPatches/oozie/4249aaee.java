From 4249aaeea2dde6a925c239d11b846804f6fc546c Mon Sep 17 00:00:00 2001
From: virag <virag@unknown>
Date: Tue, 7 May 2013 20:59:53 +0000
Subject: [PATCH] OOZIE-1231 Provide access to launcher job URL from web
 console when using Map Reduce action (ryota via virag)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1480076 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/oozie/client/OozieClient.java  | 12 ++-
 .../action/hadoop/JavaActionExecutor.java     |  9 ++-
 .../hadoop/MapReduceActionExecutor.java       | 17 ++++
 .../oozie/command/wf/ActionStartXCommand.java | 10 +++
 .../apache/oozie/servlet/V1JobServlet.java    | 68 +++++++++++++++-
 .../apache/oozie/servlet/V2JobServlet.java    | 45 +++++++++++
 .../apache/oozie/servlet/VersionServlet.java  |  1 +
 .../hadoop/TestMapReduceActionExecutor.java   | 36 ++++-----
 .../org/apache/oozie/client/TestOozieCLI.java | 10 ++-
 .../oozie/client/TestWorkflowClient.java      |  8 +-
 .../command/wf/TestActionCheckXCommand.java   | 18 ++---
 .../oozie/servlet/TestVersionServlet.java     |  4 +-
 docs/src/site/twiki/WebServicesAPI.twiki      | 80 +++++++++++++++++++
 release-log.txt                               |  1 +
 webapp/src/main/webapp/WEB-INF/web.xml        | 27 +++++++
 webapp/src/main/webapp/oozie-console.js       | 24 +++---
 16 files changed, 315 insertions(+), 55 deletions(-)
 create mode 100644 core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java

diff --git a/client/src/main/java/org/apache/oozie/client/OozieClient.java b/client/src/main/java/org/apache/oozie/client/OozieClient.java
index 412bd9d76..03601ae58 100644
-- a/client/src/main/java/org/apache/oozie/client/OozieClient.java
++ b/client/src/main/java/org/apache/oozie/client/OozieClient.java
@@ -77,7 +77,9 @@ public class OozieClient {
 
     public static final long WS_PROTOCOL_VERSION_0 = 0;
 
    public static final long WS_PROTOCOL_VERSION = 1;
    public static final long WS_PROTOCOL_VERSION_1 = 1;

    public static final long WS_PROTOCOL_VERSION = 2; // pointer to current version
 
     public static final String USER_NAME = "user.name";
 
@@ -252,9 +254,10 @@ public class OozieClient {
                     if (array == null) {
                         throw new OozieClientException("HTTP error", "no response message");
                     }
                    if (!array.contains(WS_PROTOCOL_VERSION) && !array.contains(WS_PROTOCOL_VERSION_0)) {
                    if (!array.contains(WS_PROTOCOL_VERSION) && !array.contains(WS_PROTOCOL_VERSION_1)
                            && !array.contains(WS_PROTOCOL_VERSION_0)) {
                         StringBuilder msg = new StringBuilder();
                        msg.append("Supported version [").append(WS_PROTOCOL_VERSION).append(
                        msg.append("Supported version [").append(WS_PROTOCOL_VERSION_1).append(
                                 "] or less, Unsupported versions[");
                         String separator = "";
                         for (Object version : array) {
@@ -266,6 +269,9 @@ public class OozieClient {
                     if (array.contains(WS_PROTOCOL_VERSION)) {
                         protocolUrl = baseUrl + "v" + WS_PROTOCOL_VERSION + "/";
                     }
                    else if (array.contains(WS_PROTOCOL_VERSION_1)) {
                        protocolUrl = baseUrl + "v" + WS_PROTOCOL_VERSION_1 + "/";
                    }
                     else {
                         if (array.contains(WS_PROTOCOL_VERSION_0)) {
                             protocolUrl = baseUrl + "v" + WS_PROTOCOL_VERSION_0 + "/";
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index 802285996..91dfd755c 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -973,6 +973,11 @@ public class JavaActionExecutor extends ActionExecutor {
         return Services.get().get(HadoopAccessorService.class).createJobClient(user, jobConf);
     }
 
    protected RunningJob getRunningJob(Context context, WorkflowAction action, JobClient jobClient) throws Exception{
        RunningJob runningJob = jobClient.getJob(JobID.forName(action.getExternalId()));
        return runningJob;
    }

     @Override
     public void check(Context context, WorkflowAction action) throws ActionExecutorException {
         JobClient jobClient = null;
@@ -982,7 +987,7 @@ public class JavaActionExecutor extends ActionExecutor {
             FileSystem actionFs = context.getAppFileSystem();
             JobConf jobConf = createBaseHadoopConf(context, actionXml);
             jobClient = createJobClient(context, jobConf);
            RunningJob runningJob = jobClient.getJob(JobID.forName(action.getExternalId()));
            RunningJob runningJob = getRunningJob(context, action, jobClient);
             if (runningJob == null) {
                 context.setExternalStatus(FAILED);
                 context.setExecutionData(FAILED, null);
@@ -1011,7 +1016,7 @@ public class JavaActionExecutor extends ActionExecutor {
                                 action.getId());
                     }
 
                    context.setStartData(newId, action.getTrackerUri(), runningJob.getTrackingURL());
                    context.setExternalChildIDs(newId);
                     XLog.getLog(getClass()).info(XLog.STD, "External ID swap, old ID [{0}] new ID [{1}]", launcherId,
                             newId);
                 }
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
index 623a70fa7..ffa5a7eb9 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
@@ -29,6 +29,7 @@ import org.apache.hadoop.mapred.JobClient;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.JobID;
 import org.apache.hadoop.mapred.RunningJob;
import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.service.Services;
@@ -316,4 +317,20 @@ public class MapReduceActionExecutor extends JavaActionExecutor {
         MapReduceMain.setStrings(conf, "oozie.streaming.env", env);
     }
 
    @Override
    protected RunningJob getRunningJob(Context context, WorkflowAction action, JobClient jobClient) throws Exception{

        RunningJob runningJob;
        String launcherJobId = action.getExternalId();
        String childJobId = action.getExternalChildIDs();

        if (childJobId != null && childJobId.length() > 0) {
            runningJob = jobClient.getJob(JobID.forName(childJobId));
        }
        else {
            runningJob = jobClient.getJob(JobID.forName(launcherJobId));
        }

        return runningJob;
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
index 4b2c6e9d8..5cf277716 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionStartXCommand.java
@@ -32,6 +32,7 @@ import org.apache.oozie.XException;
 import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.action.control.ControlNodeActionExecutor;
import org.apache.oozie.action.hadoop.MapReduceActionExecutor;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.WorkflowJob;
@@ -158,10 +159,12 @@ public class ActionStartXCommand extends ActionXCommand<Void> {
             if (wfAction.getStatus() == WorkflowActionBean.Status.START_RETRY
                     || wfAction.getStatus() == WorkflowActionBean.Status.START_MANUAL) {
                 isRetry = true;
                prepareForRetry(wfAction);
             }
             boolean isUserRetry = false;
             if (wfAction.getStatus() == WorkflowActionBean.Status.USER_RETRY) {
                 isUserRetry = true;
                prepareForRetry(wfAction);
             }
             context = new ActionXCommand.ActionExecutorContext(wfJob, wfAction, isRetry, isUserRetry);
             boolean caught = false;
@@ -341,4 +344,11 @@ public class ActionStartXCommand extends ActionXCommand<Void> {
         return getName() + "_" + actionId;
     }
 
    private void prepareForRetry(WorkflowActionBean wfAction) {
        if (wfAction.getType().equals("map-reduce")) {
            // need to delete child job id of original run
            wfAction.setExternalChildIDs("");
        }
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
index 159714b5d..ce75161ec 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
@@ -24,6 +24,8 @@ import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.*;
import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.client.WorkflowJob;
 import org.apache.oozie.client.rest.*;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.coord.CoordRerunXCommand;
@@ -46,6 +48,10 @@ public class V1JobServlet extends BaseJobServlet {
         super(INSTRUMENTATION_NAME);
     }
 
    protected V1JobServlet(String instrumentation_name){
        super(instrumentation_name);
    }

     /*
      * protected method to start a job
      */
@@ -664,7 +670,22 @@ public class V1JobServlet extends BaseJobServlet {
      * @return JsonBean WorkflowJobBean
      * @throws XServletException
      */
    private JsonBean getWorkflowJob(HttpServletRequest request, HttpServletResponse response) throws XServletException {
    protected JsonBean getWorkflowJob(HttpServletRequest request, HttpServletResponse response) throws XServletException {
        JsonBean jobBean = getWorkflowJobBean(request, response);
        // for backward compatibility (OOZIE-1231)
        swapMRActionID((WorkflowJob)jobBean);
        return jobBean;
    }

    /**
     * Get workflow job
     *
     * @param request servlet request
     * @param response servlet response
     * @return JsonBean WorkflowJobBean
     * @throws XServletException
     */
    protected JsonBean getWorkflowJobBean(HttpServletRequest request, HttpServletResponse response) throws XServletException {
         JsonBean jobBean = null;
         String jobId = getResourceName(request);
         String startStr = request.getParameter(RestConstants.OFFSET_PARAM);
@@ -681,10 +702,41 @@ public class V1JobServlet extends BaseJobServlet {
         catch (DagEngineException ex) {
             throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ex);
         }

         return jobBean;
     }
 
    private void swapMRActionID(WorkflowJob wjBean) {
        List<WorkflowAction> actions = wjBean.getActions();
        if (actions != null) {
            for (WorkflowAction wa : actions) {
                swapMRActionID(wa);
            }
        }
    }

    private void swapMRActionID(WorkflowAction waBean) {
        if (waBean.getType().equals("map-reduce")) {
            String childId = waBean.getExternalChildIDs();
            if (childId != null && !childId.equals("")) {
                String consoleBase = getConsoleBase(waBean.getConsoleUrl());
                ((WorkflowActionBean) waBean).setConsoleUrl(consoleBase + childId);
                ((WorkflowActionBean) waBean).setExternalId(childId);
                ((WorkflowActionBean) waBean).setExternalChildIDs("");
            }
        }
    }

    private String getConsoleBase(String url) {
        String consoleBase = null;
        if (url.indexOf("application") != -1) {
            consoleBase = url.split("application_[0-9]+_[0-9]+")[0];
        }
        else {
            consoleBase = url.split("job_[0-9]+_[0-9]+")[0];
        }
        return consoleBase;
    }

     protected JsonBean getJMSConnectionInfo(HttpServletRequest request, HttpServletResponse response) throws XServletException{
         JsonBean jmsBean = null;
         String jobId = getResourceName(request);
@@ -707,7 +759,16 @@ public class V1JobServlet extends BaseJobServlet {
      * @return JsonBean WorkflowActionBean
      * @throws XServletException
      */
    private JsonBean getWorkflowAction(HttpServletRequest request, HttpServletResponse response)
    protected JsonBean getWorkflowAction(HttpServletRequest request, HttpServletResponse response)
            throws XServletException {

        JsonBean actionBean = getWorkflowActionBean(request, response);
        // for backward compatibility (OOZIE-1231)
        swapMRActionID((WorkflowAction)actionBean);
        return actionBean;
    }

    protected JsonBean getWorkflowActionBean(HttpServletRequest request, HttpServletResponse response)
             throws XServletException {
         DagEngine dagEngine = Services.get().get(DagEngineService.class).getDagEngine(getUser(request),
                 getAuthToken(request));
@@ -720,7 +781,6 @@ public class V1JobServlet extends BaseJobServlet {
         catch (BaseEngineException ex) {
             throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ex);
         }

         return actionBean;
     }
 
diff --git a/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
new file mode 100644
index 000000000..ce52b6bb1
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
@@ -0,0 +1,45 @@
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
package org.apache.oozie.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oozie.client.rest.JsonBean;

@SuppressWarnings("serial")
public class V2JobServlet extends V1JobServlet {

    private static final String INSTRUMENTATION_NAME = "v2job";

    public V2JobServlet() {
        super(INSTRUMENTATION_NAME);
    }

    @Override
    protected JsonBean getWorkflowJob(HttpServletRequest request, HttpServletResponse response) throws XServletException {
        JsonBean jobBean = super.getWorkflowJobBean(request, response);
        return jobBean;
    }

    @Override
    protected JsonBean getWorkflowAction(HttpServletRequest request, HttpServletResponse response) throws XServletException {
        JsonBean actionBean = super.getWorkflowActionBean(request, response);
        return actionBean;
    }
}
diff --git a/core/src/main/java/org/apache/oozie/servlet/VersionServlet.java b/core/src/main/java/org/apache/oozie/servlet/VersionServlet.java
index b8c0561d6..a2bb58fe9 100644
-- a/core/src/main/java/org/apache/oozie/servlet/VersionServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/VersionServlet.java
@@ -46,6 +46,7 @@ public class VersionServlet extends JsonRestServlet {
             throws ServletException, IOException {
         JSONArray versions = new JSONArray();
         versions.add(OozieClient.WS_PROTOCOL_VERSION_0);
        versions.add(OozieClient.WS_PROTOCOL_VERSION_1);
         versions.add(OozieClient.WS_PROTOCOL_VERSION);
         sendJsonResponse(response, HttpServletResponse.SC_OK, versions);
     }
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
index cf2140fc4..79e57e794 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
@@ -316,13 +316,13 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
 
         MapReduceActionExecutor ae = new MapReduceActionExecutor();
         ae.check(context, context.getAction());
        assertFalse(launcherId.equals(context.getAction().getExternalId()));
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
 
         JobConf conf = ae.createBaseHadoopConf(context, XmlUtils.parseXml(actionXml));
         String user = conf.get("user.name");
         String group = conf.get("group.name");
         JobClient jobClient = Services.get().get(HadoopAccessorService.class).createJobClient(user, conf);
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalId()));
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalChildIDs()));
 
         waitFor(120 * 1000, new Predicate() {
             public boolean evaluate() throws Exception {
@@ -343,8 +343,8 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         String counters = context.getVar("hadoop.counters");
         assertTrue(counters.contains("Counter"));
 
        //External Child IDs will always be null in case of MR action.
        assertNull(context.getExternalChildIDs());
        //External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());
 
         return mrJob.getID().toString();
     }
@@ -365,13 +365,13 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
 
         MapReduceActionExecutor ae = new MapReduceActionExecutor();
         ae.check(context, context.getAction());
        assertFalse(launcherId.equals(context.getAction().getExternalId()));
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
 
         JobConf conf = ae.createBaseHadoopConf(context, XmlUtils.parseXml(actionXml));
         String user = conf.get("user.name");
         String group = conf.get("group.name");
         JobClient jobClient = Services.get().get(HadoopAccessorService.class).createJobClient(user, conf);
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalId()));
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalChildIDs()));
 
         waitFor(120 * 1000, new Predicate() {
             public boolean evaluate() throws Exception {
@@ -657,13 +657,13 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
 
         MapReduceActionExecutor ae = new MapReduceActionExecutor();
         ae.check(context, context.getAction());
        assertFalse(launcherId.equals(context.getAction().getExternalId()));
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
 
         JobConf conf = ae.createBaseHadoopConf(context, XmlUtils.parseXml(actionXml));
         String user = conf.get("user.name");
         String group = conf.get("group.name");
         JobClient jobClient = Services.get().get(HadoopAccessorService.class).createJobClient(user, conf);
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalId()));
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalChildIDs()));
 
         waitFor(120 * 1000, new Predicate() {
             public boolean evaluate() throws Exception {
@@ -684,8 +684,8 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         assertTrue(context.getExecutionStats().contains("ACTION_TYPE"));
         assertTrue(context.getExecutionStats().contains("Counter"));
 
        // External Child IDs will always be null in case of MR action.
        assertNull(context.getExternalChildIDs());
        // External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());
 
         // hadoop.counters will always be set in case of MR action.
         assertNotNull(context.getVar("hadoop.counters"));
@@ -732,13 +732,13 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
 
         MapReduceActionExecutor ae = new MapReduceActionExecutor();
         ae.check(context, context.getAction());
        assertFalse(launcherId.equals(context.getAction().getExternalId()));
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
 
         JobConf conf = ae.createBaseHadoopConf(context, XmlUtils.parseXml(actionXml));
         String user = conf.get("user.name");
         String group = conf.get("group.name");
         JobClient jobClient = Services.get().get(HadoopAccessorService.class).createJobClient(user, conf);
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalId()));
        final RunningJob mrJob = jobClient.getJob(JobID.forName(context.getAction().getExternalChildIDs()));
 
         waitFor(120 * 1000, new Predicate() {
             public boolean evaluate() throws Exception {
@@ -757,8 +757,8 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         // Assert for stats info stored in the context.
         assertNull(context.getExecutionStats());
 
        // External Child IDs will always be null in case of MR action.
        assertNull(context.getExternalChildIDs());
        // External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());
 
         // hadoop.counters will always be set in case of MR action.
         assertNotNull(context.getVar("hadoop.counters"));
@@ -816,7 +816,7 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
 
         MapReduceActionExecutor ae = new MapReduceActionExecutor();
         ae.check(context, context.getAction());
        assertFalse(launcherId.equals(context.getAction().getExternalId()));
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
 
         JobConf conf = ae.createBaseHadoopConf(context,
                 XmlUtils.parseXml(actionXml));
@@ -825,7 +825,7 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         JobClient jobClient = Services.get().get(HadoopAccessorService.class)
                 .createJobClient(user, conf);
         final RunningJob mrJob = jobClient.getJob(JobID.forName(context
                .getAction().getExternalId()));
                .getAction().getExternalChildIDs()));
 
         waitFor(120 * 1000, new Predicate() {
             public boolean evaluate() throws Exception {
@@ -848,8 +848,8 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         // Assert for stats info stored in the context.
         assertNull(context.getExecutionStats());
 
        // External Child IDs will always be null in case of MR action.
        assertNull(context.getExternalChildIDs());
        // External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());
 
         // hadoop.counters will always be set in case of MR action.
         assertNotNull(context.getVar("hadoop.counters"));
diff --git a/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java b/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java
index c4c06a116..6b44e4edf 100644
-- a/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java
++ b/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java
@@ -34,6 +34,7 @@ import org.apache.oozie.servlet.MockDagEngineService;
 import org.apache.oozie.servlet.V1AdminServlet;
 import org.apache.oozie.servlet.V1JobServlet;
 import org.apache.oozie.servlet.V1JobsServlet;
import org.apache.oozie.servlet.V2JobServlet;
 import org.apache.oozie.util.XConfiguration;
 
 //hardcoding options instead using constants on purpose, to detect changes to option names if any and correct docs.
@@ -44,6 +45,7 @@ public class TestOozieCLI extends DagServletTestCase {
         new V1JobServlet();
         new V1JobsServlet();
         new V1AdminServlet();
        new V2JobServlet();
     }
 
     static final boolean IS_SECURITY_ENABLED = false;
@@ -51,7 +53,7 @@ public class TestOozieCLI extends DagServletTestCase {
     static final String[] END_POINTS = {"/versions", VERSION + "/jobs", VERSION + "/job/*", VERSION + "/admin/*"};
     static final Class[] SERVLET_CLASSES =
  { HeaderTestingVersionServlet.class, V1JobsServlet.class, V1JobServlet.class,
            V1AdminServlet.class };
            V1AdminServlet.class, V2JobServlet.class };
 
     @Override
     protected void setUp() throws Exception {
@@ -512,7 +514,7 @@ public class TestOozieCLI extends DagServletTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 assertEquals(RestConstants.JOB_SHOW_INFO, MockDagEngineService.did);
 
                args = new String[]{"job", "-timezone", "PST", "-oozie", oozieUrl, "-info", 
                args = new String[]{"job", "-timezone", "PST", "-oozie", oozieUrl, "-info",
                     MockDagEngineService.JOB_ID + "1" + MockDagEngineService.JOB_ID_END};
                 assertEquals(0, new OozieCLI().run(args));
                 assertEquals(RestConstants.JOB_SHOW_INFO, MockDagEngineService.did);
@@ -544,14 +546,14 @@ public class TestOozieCLI extends DagServletTestCase {
                 assertEquals(0, new OozieCLI().run(args));
                 assertEquals(RestConstants.JOBS_FILTER_PARAM, MockDagEngineService.did);
                 
                args = new String[]{"jobs", "-timezone", "PST", "-len", "3", "-offset", "2", "-oozie", oozieUrl, 
                args = new String[]{"jobs", "-timezone", "PST", "-len", "3", "-offset", "2", "-oozie", oozieUrl,
                     "-filter", "name=x"};
                 assertEquals(0, new OozieCLI().run(args));
                 assertEquals(RestConstants.JOBS_FILTER_PARAM, MockDagEngineService.did);
                 
                 args = new String[]{"jobs", "-jobtype", "coord",  "-filter", "status=FAILED", "-oozie", oozieUrl};
                 assertEquals(0, new OozieCLI().run(args));
                assertEquals(RestConstants.JOBS_FILTER_PARAM, MockDagEngineService.did);                
                assertEquals(RestConstants.JOBS_FILTER_PARAM, MockDagEngineService.did);
                 return null;
             }
         });
diff --git a/core/src/test/java/org/apache/oozie/client/TestWorkflowClient.java b/core/src/test/java/org/apache/oozie/client/TestWorkflowClient.java
index a4b04103c..bf443725c 100644
-- a/core/src/test/java/org/apache/oozie/client/TestWorkflowClient.java
++ b/core/src/test/java/org/apache/oozie/client/TestWorkflowClient.java
@@ -30,6 +30,9 @@ import org.apache.oozie.servlet.MockDagEngineService;
 import org.apache.oozie.servlet.V0JobServlet;
 import org.apache.oozie.servlet.V0JobsServlet;
 import org.apache.oozie.servlet.V1AdminServlet;
import org.apache.oozie.servlet.V1JobServlet;
import org.apache.oozie.servlet.V1JobsServlet;
import org.apache.oozie.servlet.V2JobServlet;
 
 public class TestWorkflowClient extends DagServletTestCase {
 
@@ -37,14 +40,17 @@ public class TestWorkflowClient extends DagServletTestCase {
         new HeaderTestingVersionServlet();
         new V0JobServlet();
         new V0JobsServlet();
        new V1JobsServlet();
         new V1AdminServlet();
        new V1JobServlet();
        new V2JobServlet();
     }
 
     private static final boolean IS_SECURITY_ENABLED = false;
     static final String VERSION = "/v" + OozieClient.WS_PROTOCOL_VERSION;
     static final String[] END_POINTS = {"/versions", VERSION + "/jobs", VERSION + "/job/*", VERSION + "/admin/*"};
     static final Class[] SERVLET_CLASSES = {HeaderTestingVersionServlet.class, V0JobsServlet.class,
            V0JobServlet.class, V1AdminServlet.class};
            V0JobServlet.class, V1AdminServlet.class, V1JobServlet.class, V2JobServlet.class, V1JobsServlet.class};
 
     protected void setUp() throws Exception {
         super.setUp();
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java
index c80057593..467eca4a2 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java
@@ -220,10 +220,11 @@ public class TestActionCheckXCommand extends XDataTestCase {
         new ActionCheckXCommand(action.getId()).call();
         action = jpaService.execute(wfActionGetCmd);
         String mapperId = action.getExternalId();
        String childId = action.getExternalChildIDs();
 
        assertFalse(launcherId.equals(mapperId));
        assertTrue(launcherId.equals(mapperId));
 
        final RunningJob mrJob = jobClient.getJob(JobID.forName(mapperId));
        final RunningJob mrJob = jobClient.getJob(JobID.forName(childId));
 
         waitFor(120 * 1000, new Predicate() {
             public boolean evaluate() throws Exception {
@@ -329,10 +330,11 @@ public class TestActionCheckXCommand extends XDataTestCase {
         new ActionCheckXCommand(actionId).call();
         WorkflowActionBean action4 = jpaService.execute(wfActionGetCmd);
         String mapperId = action4.getExternalId();
        String childId = action4.getExternalChildIDs();
 
        assertFalse(launcherId.equals(mapperId));
        assertTrue(launcherId.equals(mapperId));
 
        final RunningJob mrJob = jobClient.getJob(JobID.forName(mapperId));
        final RunningJob mrJob = jobClient.getJob(JobID.forName(childId));
 
         waitFor(120 * 1000, new Predicate() {
             @Override
@@ -392,7 +394,7 @@ public class TestActionCheckXCommand extends XDataTestCase {
 
         new ActionCheckXCommand(action1.getId()).call();
         WorkflowActionBean action2 = jpaService.execute(wfActionGetCmd);
        String originalMapperId = action2.getExternalId();
        String originalMapperId = action2.getExternalChildIDs();
 
         assertFalse(originalLauncherId.equals(originalMapperId));
 
@@ -442,7 +444,6 @@ public class TestActionCheckXCommand extends XDataTestCase {
         String launcherId = action3.getExternalId();
 
         assertFalse(originalLauncherId.equals(launcherId));
        assertFalse(originalMapperId.equals(launcherId));
 
         final RunningJob launcherJob2 = jobClient.getJob(JobID.forName(launcherId));
 
@@ -452,16 +453,15 @@ public class TestActionCheckXCommand extends XDataTestCase {
                 return launcherJob2.isComplete();
             }
         });

         assertTrue(launcherJob2.isSuccessful());
         assertTrue(LauncherMapper.hasIdSwap(launcherJob2));
 
         new ActionCheckXCommand(actionId).call();
         WorkflowActionBean action4 = jpaService.execute(wfActionGetCmd);
        String mapperId = action4.getExternalId();
        String mapperId = action4.getExternalChildIDs();
         assertFalse(originalMapperId.equals(mapperId));
 
        assertFalse(launcherId.equals(mapperId));

         final RunningJob mrJob = jobClient.getJob(JobID.forName(mapperId));
 
         waitFor(120 * 1000, new Predicate() {
diff --git a/core/src/test/java/org/apache/oozie/servlet/TestVersionServlet.java b/core/src/test/java/org/apache/oozie/servlet/TestVersionServlet.java
index d5ab3323f..f9d7d421f 100644
-- a/core/src/test/java/org/apache/oozie/servlet/TestVersionServlet.java
++ b/core/src/test/java/org/apache/oozie/servlet/TestVersionServlet.java
@@ -47,8 +47,8 @@ public class TestVersionServlet extends DagServletTestCase {
                 assertEquals(HttpServletResponse.SC_OK, conn.getResponseCode());
                 assertTrue(conn.getHeaderField("content-type").startsWith(RestConstants.JSON_CONTENT_TYPE));
                 JSONArray array = (JSONArray) JSONValue.parse(new InputStreamReader(conn.getInputStream()));
                assertEquals(2, array.size());
                assertEquals(OozieClient.WS_PROTOCOL_VERSION, array.get(1));
                assertEquals(3, array.size());
                assertEquals(OozieClient.WS_PROTOCOL_VERSION_1, array.get(1));
                 return null;
             }
         });
diff --git a/docs/src/site/twiki/WebServicesAPI.twiki b/docs/src/site/twiki/WebServicesAPI.twiki
index 7faaa8910..71db0ae03 100644
-- a/docs/src/site/twiki/WebServicesAPI.twiki
++ b/docs/src/site/twiki/WebServicesAPI.twiki
@@ -897,6 +897,86 @@ Content-Type: application/json;charset=UTF-8
 }
 </verbatim>
 
---++ Oozie Web Services API, V2 (Workflow , Coordinator And Bundle)

The Oozie Web Services API is a HTTP REST JSON API.

All responses are in =UTF-8=.

Assuming Oozie is runing at =OOZIE_URL=, the following web services end points are supported:

   * <OOZIE_URL>/versions
   * <OOZIE_URL>/v2/admin
   * <OOZIE_URL>/v2/job
   * <OOZIE_URL>/v2/jobs

Please note that v1 and v2 are almost identical.
Only difference is the JSON format of Job Information API (*/job) particularly for map-reduce action.
No change for other actions.

In v1, externalId and consoleUrl point to spawned child job ID, and exteranlChildIDs is null in map-reduce action.
In v2, externalId and consoleUrl point to launcher job ID, and exteranlChildIDs is spawned child job ID in map-reduce action.

v2/admin, v2/jobs remain the same with v1/admin, v1/jobs

---+++ Job and Jobs End-Points

---++++ Job Information

A HTTP GET request retrieves the job information.

*Request:*

<verbatim>
GET /oozie/v2/job/job-3?show=info&timezone=GMT
</verbatim>

*Response for a workflow job:*

<verbatim>
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
.
{
**jobType: "workflow",
  id: "0-200905191240-oozie-W",
  appName: "indexer-workflow",
  appPath: "hdfs://user/bansalm/indexer.wf",
  externalId: "0-200905191230-oozie-pepe",
  user: "bansalm",
  group: "other",
  status: "RUNNING",
  conf: "<configuration> ... </configuration>",
  createdTime: "Thu, 01 Jan 2009 00:00:00 GMT",
  startTime: "Fri, 02 Jan 2009 00:00:00 GMT",
  endTime: null,
  run: 0,
  actions: [
    {
      id: "0-200905191240-oozie-W@indexer",
      name: "indexer",
      type: "map-reduce",
      conf: "<configuration> ...</configuration>",
      startTime: "Thu, 01 Jan 2009 00:00:00 GMT",
      endTime: "Fri, 02 Jan 2009 00:00:00 GMT",
      status: "OK",
      externalId: "job-123-200903101010",
      externalStatus: "SUCCEEDED",
      trackerUri: "foo:8021",
      consoleUrl: "http://foo:50040/jobdetailshistory.jsp?jobId=job-123-200903101010",
      transition: "reporter",
      data: null,
      stats: null,
      externalChildIDs: "job-123-200903101011"
      errorCode: null,
      errorMessage: null,
      retries: 0
    },
    ...
  ]
}
</verbatim>

 </noautolink>
 
 
diff --git a/release-log.txt b/release-log.txt
index 946d90dd8..bd0f7c257 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1231 Provide access to launcher job URL from web console when using Map Reduce action (ryota via virag)
 OOZIE-1335 The launcher job should use uber mode in Hadoop 2 by default (rkanter)
 OOZIE-1297 Add chgrp in FS action (ryota via virag)
 OOZIE-1329 fix coverage org.apache.oozie.tools (agorshkov via virag)
diff --git a/webapp/src/main/webapp/WEB-INF/web.xml b/webapp/src/main/webapp/WEB-INF/web.xml
index 633b0c855..a17691817 100644
-- a/webapp/src/main/webapp/WEB-INF/web.xml
++ b/webapp/src/main/webapp/WEB-INF/web.xml
@@ -83,6 +83,13 @@
         <load-on-startup>1</load-on-startup>
     </servlet>
 
    <servlet>
        <servlet-name>v2job</servlet-name>
        <display-name>WS API for a specific Workflow Job</display-name>
        <servlet-class>org.apache.oozie.servlet.V2JobServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>

     <servlet>
         <servlet-name>sla-event</servlet-name>
         <display-name>WS API for specific SLA Events</display-name>
@@ -106,6 +113,11 @@
         <url-pattern>/v1/admin/*</url-pattern>
     </servlet-mapping>
 
    <servlet-mapping>
        <servlet-name>v1admin</servlet-name>
        <url-pattern>/v2/admin/*</url-pattern>
    </servlet-mapping>

     <servlet-mapping>
         <servlet-name>callback</servlet-name>
         <url-pattern>/callback/*</url-pattern>
@@ -121,6 +133,11 @@
         <url-pattern>/v1/jobs</url-pattern>
     </servlet-mapping>
 
    <servlet-mapping>
        <servlet-name>v1jobs</servlet-name>
        <url-pattern>/v2/jobs</url-pattern>
    </servlet-mapping>

     <servlet-mapping>
         <servlet-name>v0job</servlet-name>
         <url-pattern>/v0/job/*</url-pattern>
@@ -131,11 +148,21 @@
         <url-pattern>/v1/job/*</url-pattern>
     </servlet-mapping>
 
    <servlet-mapping>
        <servlet-name>v2job</servlet-name>
        <url-pattern>/v2/job/*</url-pattern>
    </servlet-mapping>

     <servlet-mapping>
         <servlet-name>sla-event</servlet-name>
         <url-pattern>/v1/sla/*</url-pattern>
     </servlet-mapping>
 
    <servlet-mapping>
        <servlet-name>sla-event</servlet-name>
        <url-pattern>/v2/sla/*</url-pattern>
    </servlet-mapping>

     <!-- welcome-file -->
     <welcome-file-list>
         <welcome-file>index.html</welcome-file>
diff --git a/webapp/src/main/webapp/oozie-console.js b/webapp/src/main/webapp/oozie-console.js
index f98f0acde..7ea18a399 100644
-- a/webapp/src/main/webapp/oozie-console.js
++ b/webapp/src/main/webapp/oozie-console.js
@@ -32,7 +32,7 @@ var oozie_host = "";
 var flattenedObject;
 
 function getOozieClientVersion() {
    return 1;
    return 2;
 }
 
 function getOozieVersionsUrl() {
@@ -546,17 +546,17 @@ function jobDetailsPopup(response, request) {
             
             // Tab to show list of child Job URLs for pig action
             var childJobsItem = {
				title : 'Child Job URLs',
				autoScroll : true,
				frame : true,
				labelAlign : 'right',
				labelWidth : 70,
				items : urlUnit
			};
            if (actionStatus.type == "pig" || actionStatus.type == "hive") {
				var tabPanel = win.items.get(0);
				tabPanel.add(childJobsItem);
			}
                title : 'Child Job URLs',
                autoScroll : true,
                frame : true,
                labelAlign : 'right',
                labelWidth : 70,
                items : urlUnit
            };
            if (actionStatus.type == "pig" || actionStatus.type == "hive" || actionStatus.type == "map-reduce") {
                var tabPanel = win.items.get(0);
                tabPanel.add(childJobsItem);
            }
             win.setPosition(50, 50);
             win.show();
         }
- 
2.19.1.windows.1

