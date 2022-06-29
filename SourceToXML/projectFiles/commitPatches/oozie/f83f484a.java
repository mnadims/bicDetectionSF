From f83f484a1a34c9446cf43ba7d627aefabaf58921 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Tue, 7 Jun 2016 08:32:13 -0700
Subject: [PATCH] OOZIE-2553 Cred tag is required for all actions in the
 workflow even if an action does not require it (me.venkatr via rohini)

--
 .../action/hadoop/JavaActionExecutor.java     | 12 +++---
 .../apache/oozie/workflow/lite/NodeDef.java   |  5 ++-
 .../action/hadoop/ActionExecutorTestCase.java |  2 -
 .../action/hadoop/TestJavaActionExecutor.java | 38 ++++++++++++++++++-
 .../command/wf/TestActionCheckXCommand.java   |  1 -
 .../command/wf/TestActionStartXCommand.java   |  4 --
 .../oozie/event/TestEventGeneration.java      |  1 -
 .../oozie/service/TestRecoveryService.java    |  1 -
 .../oozie/store/TestDBWorkflowStore.java      |  1 -
 .../org/apache/oozie/test/XDataTestCase.java  |  1 -
 release-log.txt                               |  1 +
 11 files changed, 49 insertions(+), 18 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index 6893bb8ed..639003e82 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -1307,11 +1307,13 @@ public class JavaActionExecutor extends ActionExecutor {
         HashMap<String, CredentialsProperties> props = new HashMap<String, CredentialsProperties>();
         if (context != null && action != null) {
             String credsInAction = action.getCred();
            LOG.debug("Get credential '" + credsInAction + "' properties for action : " + action.getId());
            String[] credNames = credsInAction.split(",");
            for (String credName : credNames) {
                CredentialsProperties credProps = getCredProperties(context, credName);
                props.put(credName, credProps);
            if (credsInAction != null) {
                LOG.debug("Get credential '" + credsInAction + "' properties for action : " + action.getId());
                String[] credNames = credsInAction.split(",");
                for (String credName : credNames) {
                    CredentialsProperties credProps = getCredProperties(context, credName);
                    props.put(credName, credProps);
                }
             }
         }
         else {
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/NodeDef.java b/core/src/main/java/org/apache/oozie/workflow/lite/NodeDef.java
index a395b7734..9e66d2807 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/NodeDef.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/NodeDef.java
@@ -40,7 +40,7 @@ public class NodeDef implements Writable {
     private Class<? extends NodeHandler> handlerClass;
     private String conf = null;
     private List<String> transitions = new ArrayList<String>();
    private String cred = "null";
    private String cred = null;
     private String userRetryMax = "null";
     private String userRetryInterval = "null";
 
@@ -154,6 +154,9 @@ public class NodeDef implements Writable {
         nodeDefVersion = LiteWorkflowStoreService.NODE_DEF_VERSION_1;
         name = dataInput.readUTF();
         cred = dataInput.readUTF();
        if (cred.equals("null")) {
            cred = null;
        }
         String handlerClassName = dataInput.readUTF();
         if ((handlerClassName != null) && (handlerClassName.length() > 0)) {
             try {
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/ActionExecutorTestCase.java b/core/src/test/java/org/apache/oozie/action/hadoop/ActionExecutorTestCase.java
index e1c450c62..d74160a09 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/ActionExecutorTestCase.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/ActionExecutorTestCase.java
@@ -262,7 +262,6 @@ public abstract class ActionExecutorTestCase extends XHCatTestCase {
 
         WorkflowActionBean action = new WorkflowActionBean();
         action.setName(actionName);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(workflow.getId(), actionName));
         workflow.getActions().add(action);
         return workflow;
@@ -296,7 +295,6 @@ public abstract class ActionExecutorTestCase extends XHCatTestCase {
 
         WorkflowActionBean action = new WorkflowActionBean();
         action.setName(actionName);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(workflow.getId(), actionName));
         workflow.getActions().add(action);
         return workflow;
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
index 057c9fb1c..85bb993b3 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
@@ -35,6 +35,7 @@ import java.util.HashMap;
 import java.util.Map;
 import java.util.Properties;
 
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.filecache.DistributedCache;
 import org.apache.hadoop.fs.FSDataOutputStream;
@@ -1025,6 +1026,42 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         }
     }
 

    public void testCredentialsWithoutCredTag() throws Exception {
        // create a workflow with credentials
        // add a pig action without cred tag
        String workflowXml = "<workflow-app xmlns='uri:oozie:workflow:0.2.5' name='pig-wf'>" + "<credentials>"
                + "<credential name='abcname' type='abc'>" + "<property>" + "<name>property1</name>"
                + "<value>value1</value>" + "</property>" + "<property>" + "<name>property2</name>"
                + "<value>value2</value>" + "</property>" + "<property>" + "<name>${property3}</name>"
                + "<value>${value3}</value>" + "</property>" + "</credential>" + "</credentials>"
                + "<start to='pig1' />" + "<action name='pig1'>" + "<pig>" + "</pig>"
                + "<ok to='end' />" + "<error to='fail' />" + "</action>" + "<kill name='fail'>"
                + "<message>Pig failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>" + "</kill>"
                + "<end name='end' />" + "</workflow-app>";

        JavaActionExecutor ae = new JavaActionExecutor();
        WorkflowJobBean wfBean = addRecordToWfJobTable("test1", workflowXml);
        WorkflowActionBean action = (WorkflowActionBean) wfBean.getActions().get(0);
        action.setType(ae.getType());
        String actionXml = "<pig>" + "<job-tracker>${jobTracker}</job-tracker>" + "<name-node>${nameNode}</name-node>"
                + "<prepare>" + "<delete path='outputdir' />" + "</prepare>" + "<configuration>" + "<property>"
                + "<name>mapred.compress.map.output</name>" + "<value>true</value>" + "</property>" + "<property>"
                + "<name>mapred.job.queue.name</name>" + "<value>${queueName}</value>" + "</property>"
                + "</configuration>" + "<script>org/apache/oozie/examples/pig/id.pig</script>"
                + "<param>INPUT=${inputDir}</param>" + "<param>OUTPUT=${outputDir}/pig-output</param>" + "</pig>";
        action.setConf(actionXml);
        Context context = new Context(wfBean, action);

        Element actionXmlconf = XmlUtils.parseXml(action.getConf());
        // action job configuration
        Configuration actionConf = ae.createBaseHadoopConf(context, actionXmlconf);

        // should not throw JA021 exception
        HashMap<String, CredentialsProperties> credProperties = ae.setCredentialPropertyToActionConf(context, action,
                    actionConf);
    }

     public void testCredentialsSkip() throws Exception {
         // Try setting oozie.credentials.skip at different levels, and verifying the correct behavior
         // oozie-site: false -- job-level: null -- action-level: null
@@ -1163,7 +1200,6 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         wfBean.setStatus(WorkflowJob.Status.SUCCEEDED);
         WorkflowActionBean action = new WorkflowActionBean();
         action.setName("test");
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(wfBean.getId(), "test"));
         wfBean.getActions().add(action);
         return wfBean;
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java
index f503b1f6a..5898d1af7 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestActionCheckXCommand.java
@@ -633,7 +633,6 @@ public class TestActionCheckXCommand extends XDataTestCase {
         WorkflowActionBean action = new WorkflowActionBean();
         String actionname = "testAction";
         action.setName(actionname);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(wfId, actionname));
         action.setJobId(wfId);
         action.setType("map-reduce");
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestActionStartXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestActionStartXCommand.java
index b7489e94c..ea90c087f 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestActionStartXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestActionStartXCommand.java
@@ -334,7 +334,6 @@ public class TestActionStartXCommand extends XDataTestCase {
         WorkflowActionBean action = new WorkflowActionBean();
         String actionname = "testAction";
         action.setName(actionname);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(wfId, actionname));
         action.setJobId(wfId);
         action.setType("map-reduce");
@@ -421,7 +420,6 @@ public class TestActionStartXCommand extends XDataTestCase {
         WorkflowActionBean action = new WorkflowActionBean();
         String actionname = "testAction";
         action.setName(actionname);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(wfId, actionname));
         action.setJobId(wfId);
         action.setType("map-reduce");
@@ -529,7 +527,6 @@ public class TestActionStartXCommand extends XDataTestCase {
         WorkflowActionBean action = new WorkflowActionBean();
         String actionname = "testAction";
         action.setName(actionname);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(wfId, actionname));
         action.setJobId(wfId);
         action.setType("map-reduce");
@@ -568,7 +565,6 @@ public class TestActionStartXCommand extends XDataTestCase {
         WorkflowActionBean action = new WorkflowActionBean();
         String actionname = "testAction";
         action.setName(actionname);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(wfId, actionname));
         action.setJobId(wfId);
         action.setType("fs");
diff --git a/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java b/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java
index f662d8a91..afd3f8fff 100644
-- a/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java
++ b/core/src/test/java/org/apache/oozie/event/TestEventGeneration.java
@@ -686,7 +686,6 @@ public class TestEventGeneration extends XDataTestCase {
         action.setStartTime(new Date());
         action.setEndTime(new Date());
         action.setLastCheckTime(new Date());
        action.setCred("null");
         action.setPendingOnly();
 
         String actionXml = "<java>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
diff --git a/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java b/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java
index 13d8e8d2e..eab177b9a 100644
-- a/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java
++ b/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java
@@ -864,7 +864,6 @@ public class TestRecoveryService extends XDataTestCase {
         WorkflowActionBean action = new WorkflowActionBean();
         String actionname = "testAction";
         action.setName(actionname);
        action.setCred("null");
         action.setId(Services.get().get(UUIDService.class).generateChildId(wfId, actionname));
         action.setJobId(wfId);
         action.setType("map-reduce");
diff --git a/core/src/test/java/org/apache/oozie/store/TestDBWorkflowStore.java b/core/src/test/java/org/apache/oozie/store/TestDBWorkflowStore.java
index c263e8603..b571fb532 100644
-- a/core/src/test/java/org/apache/oozie/store/TestDBWorkflowStore.java
++ b/core/src/test/java/org/apache/oozie/store/TestDBWorkflowStore.java
@@ -477,7 +477,6 @@ public class TestDBWorkflowStore extends XTestCase {
         a31.setId(str.toString());
         a31.setJobId(wfBean3.getId());
         a31.setStatus(WorkflowAction.Status.PREP);
        a31.setCred("null");
         store.beginTrx();
         store.insertAction(a31);
         store.commitTrx();
diff --git a/core/src/test/java/org/apache/oozie/test/XDataTestCase.java b/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
index a9aa79a7c..081d2f1f8 100644
-- a/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
++ b/core/src/test/java/org/apache/oozie/test/XDataTestCase.java
@@ -1417,7 +1417,6 @@ public abstract class XDataTestCase extends XHCatTestCase {
         action.setStartTime(currDate);
         action.setEndTime(currDate);
         action.setLastCheckTime(currDate);
        action.setCred("null");
         action.setStats("dummyStats");
         if (pending) {
             action.setPending();
diff --git a/release-log.txt b/release-log.txt
index 189ca2195..feea868bb 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2553 Cred tag is required for all actions in the workflow even if an action does not require it (me.venkatr via rohini)
 OOZIE-2503 show ChildJobURLs to spark action (satishsaley via puru)
 OOZIE-2551 Feature request: epoch timestamp generation (jtolar via puru)
 OOZIE-2542 Option to disable OpenJPA BrokerImpl finalization (puru)
- 
2.19.1.windows.1

