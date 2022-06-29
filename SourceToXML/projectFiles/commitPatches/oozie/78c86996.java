From 78c8699693408dcbcf6892ecbb3ee1037908d943 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Thu, 21 Apr 2016 16:03:38 -0700
Subject: [PATCH] OOZIE-2511 SubWorkflow missing variable set from option if
 config-default is present in parent workflow (asasvari via rkanter)

--
 .../oozie/SubWorkflowActionExecutor.java      |   6 +-
 .../oozie/TestSubWorkflowActionExecutor.java  | 184 +++++++++++++-----
 release-log.txt                               |   1 +
 3 files changed, 138 insertions(+), 53 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
index 20e4caf12..f77e52cd2 100644
-- a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
@@ -179,7 +179,11 @@ public class SubWorkflowActionExecutor extends ActionExecutor {
                 String appPath = eConf.getChild("app-path", ns).getTextTrim();
 
                 XConfiguration subWorkflowConf = new XConfiguration();

                injectInline(eConf.getChild("configuration", ns), subWorkflowConf);

                 Configuration parentConf = new XConfiguration(new StringReader(context.getWorkflow().getConf()));

                 if (eConf.getChild(("propagate-configuration"), ns) != null) {
                     XConfiguration.copy(parentConf, subWorkflowConf);
                 }
@@ -205,7 +209,7 @@ public class SubWorkflowActionExecutor extends ActionExecutor {
                 if(group != null) {
                     subWorkflowConf.set(OozieClient.GROUP_NAME, group);
                 }
                injectInline(eConf.getChild("configuration", ns), subWorkflowConf);

                 injectCallback(context, subWorkflowConf);
                 injectRecovery(extId, subWorkflowConf);
                 injectParent(context.getWorkflow().getId(), subWorkflowConf);
diff --git a/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java b/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
index 26e5031af..bdbbfd935 100644
-- a/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
@@ -230,7 +230,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         assertEquals(WorkflowAction.Status.OK, action.getStatus());
 
         WorkflowJob wf = oozieClient.getJobInfo(action.getExternalId());
        Configuration childConf = new XConfiguration(new StringReader(wf.getConf()));
        Configuration childConf = getWorkflowConfig(wf);
         assertEquals("xyz", childConf.get("abc"));
     }
 
@@ -281,7 +281,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         assertEquals(WorkflowAction.Status.OK, action.getStatus());
 
         WorkflowJob wf = oozieClient.getJobInfo(action.getExternalId());
        Configuration childConf = new XConfiguration(new StringReader(wf.getConf()));
        Configuration childConf = getWorkflowConfig(wf);
 
         assertFalse(getTestGroup() == childConf.get(OozieClient.GROUP_NAME));
 
@@ -361,7 +361,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         assertEquals(WorkflowAction.Status.OK, action.getStatus());
 
         WorkflowJob wf = oozieClient.getJobInfo(action.getExternalId());
        Configuration childConf = new XConfiguration(new StringReader(wf.getConf()));
        Configuration childConf = getWorkflowConfig(wf);
         assertNull(childConf.get("abc"));
     }
 
@@ -409,7 +409,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
 
         WorkflowAppService wps = Services.get().get(WorkflowAppService.class);
         WorkflowJob wf = oozieClient.getJobInfo(action.getExternalId());
        Configuration childConf = new XConfiguration(new StringReader(wf.getConf()));
        Configuration childConf = getWorkflowConfig(wf);
         childConf = wps.createProtoActionConf(childConf, true);
         assertEquals(childConf.get(WorkflowAppService.APP_LIB_PATH_LIST), subwfLibJar.toString());
     }
@@ -419,7 +419,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         FileSystem fs = getFileSystem();
         Writer writer = new OutputStreamWriter(fs.create(new Path(subWorkflowAppPath, "workflow.xml")));
         // Infinitly recursive workflow
        

         String appStr = "<workflow-app xmlns=\"uri:oozie:workflow:0.4\" name=\"workflow\">" +
                 "<start to=\"subwf\"/>" +
                 "<action name=\"subwf\">" +
@@ -574,6 +574,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
             Writer writer = new OutputStreamWriter(fs.create(subWorkflowPath));
             writer.write(getLazyWorkflow());
             writer.close();

             String workflowUri = getTestCaseFileUri("workflow.xml");
             String appXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.4\" name=\"workflow\">" +
                     "<start to=\"subwf\"/>" +
@@ -629,7 +630,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
             });
 
             WorkflowJob job = wfClient.getJobInfo(wfClient.getJobInfo(jobId).getActions().get(2).getExternalId());
            assertEquals(job.getStatus(), WorkflowJob.Status.SUCCEEDED);
            assertEquals(WorkflowJob.Status.SUCCEEDED, job.getStatus());
             assertEquals(job.getId(), subWorkflowExternalId);
 
         } finally {
@@ -640,57 +641,64 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
 
     public void testParentGlobalConf() throws Exception {
         try {
            Path subWorkflowAppPath = getFsTestCaseDir();
            FileSystem fs = getFileSystem();
            Path subWorkflowPath = new Path(subWorkflowAppPath, "workflow.xml");
            Writer writer = new OutputStreamWriter(fs.create(subWorkflowPath));
            writer.write(getWorkflow());
            writer.close();
            Path subWorkflowAppPath = createSubWorkflowXml();
 
            String workflowUri = getTestCaseFileUri("workflow.xml");
            String appXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.4\" name=\"workflow\">" +
                    "<global>" +
                    "   <configuration>" +
                    "        <property>" +
                    "            <name>foo2</name>" +
                    "            <value>foo2</value>" +
                    "        </property>" +
                    "        <property>" +
                    "            <name>foo3</name>" +
                    "            <value>foo3</value>" +
                    "        </property>" +
                    "    </configuration>" +
                    "</global>" +
                    "<start to=\"subwf\"/>" +
                    "<action name=\"subwf\">" +
                    "     <sub-workflow xmlns='uri:oozie:workflow:0.4'>" +
                    "          <app-path>" + subWorkflowAppPath.toString() + "</app-path>" +
                    "<propagate-configuration/>" +
                    "   <configuration>" +
                    "        <property>" +
                    "            <name>foo3</name>" +
                    "            <value>actionconf</value>" +
                    "        </property>" +
                    "   </configuration>" +
                    "     </sub-workflow>" +
                    "     <ok to=\"end\"/>" +
                    "     <error to=\"fail\"/>" +
                    "</action>" +
                    "<kill name=\"fail\">" +
                    "     <message>Sub workflow failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>" +
                    "</kill>" +
                    "<end name=\"end\"/>" +
                    "</workflow-app>";
            String workflowUri = createTestWorkflowXml(subWorkflowAppPath);
            LocalOozie.start();
            final OozieClient wfClient = LocalOozie.getClient();
            Properties conf = wfClient.createConfiguration();
            conf.setProperty(OozieClient.APP_PATH, workflowUri);
            conf.setProperty(OozieClient.USER_NAME, getTestUser());
            conf.setProperty("appName", "var-app-name");
            final String jobId = wfClient.submit(conf);
            wfClient.start(jobId);

            waitFor(JOB_TIMEOUT, new Predicate() {
                public boolean evaluate() throws Exception {
                    return (wfClient.getJobInfo(jobId).getStatus() == WorkflowJob.Status.SUCCEEDED) &&
                            (wfClient.getJobInfo(jobId).getActions().get(1).getStatus() == WorkflowAction.Status.OK);
                }
            });
            WorkflowJob subWorkflow = wfClient.getJobInfo(wfClient.getJobInfo(jobId).
                    getActions().get(1).getExternalId());

            Configuration subWorkflowConf = getWorkflowConfig(subWorkflow);
            Element eConf = XmlUtils.parseXml(subWorkflow.getActions().get(1).getConf());
            Element element = eConf.getChild("configuration", eConf.getNamespace());
            Configuration actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(element).toString()));


            assertEquals("foo1", actionConf.get("foo1"));
            assertEquals("subconf", actionConf.get("foo2"));
            assertEquals("foo3", actionConf.get("foo3"));

            // Checking the action conf configuration.
            assertEquals("actionconf", subWorkflowConf.get("foo3"));
        } finally {
            LocalOozie.stop();
        }
    }

    public void testParentGlobalConfWithConfigDefault() throws Exception {
        try {
            Path subWorkflowAppPath = createSubWorkflowXml();

            createConfigDefaultXml();

            String workflowUri = createTestWorkflowXml(subWorkflowAppPath);
 
            writeToFile(appXml, workflowUri);
             LocalOozie.start();
             final OozieClient wfClient = LocalOozie.getClient();
             Properties conf = wfClient.createConfiguration();
             conf.setProperty(OozieClient.APP_PATH, workflowUri);
             conf.setProperty(OozieClient.USER_NAME, getTestUser());
             conf.setProperty("appName", "var-app-name");
            conf.setProperty("foo", "other");
             final String jobId = wfClient.submit(conf);
             wfClient.start(jobId);
            // configuration should have overridden value
            assertEquals("other",
                    new XConfiguration(new StringReader(wfClient.getJobInfo(jobId).getConf())).get("foo"));
 
             waitFor(JOB_TIMEOUT, new Predicate() {
                 public boolean evaluate() throws Exception {
@@ -700,20 +708,92 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
             });
             WorkflowJob subWorkflow = wfClient.getJobInfo(wfClient.getJobInfo(jobId).
                     getActions().get(1).getExternalId());
            Configuration subWorkflowConf = new XConfiguration(new StringReader(subWorkflow.getConf()));

            Configuration subWorkflowConf = getWorkflowConfig(subWorkflow);
             Element eConf = XmlUtils.parseXml(subWorkflow.getActions().get(1).getConf());
             Element element = eConf.getChild("configuration", eConf.getNamespace());
             Configuration actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(element).toString()));
            assertEquals(actionConf.get("foo1"), "foo1");
            assertEquals(actionConf.get("foo2"), "subconf");
            assertEquals(actionConf.get("foo3"), "foo3");

            // configuration in subWorkflow should have overridden value
            assertEquals("other", subWorkflowConf.get("foo"));

            assertEquals("foo1", actionConf.get("foo1"));
            assertEquals("subconf", actionConf.get("foo2"));
            assertEquals("foo3", actionConf.get("foo3"));
             // Checking the action conf configuration.
            assertEquals(subWorkflowConf.get("foo3"), "actionconf");
            assertEquals("actionconf", subWorkflowConf.get("foo3"));

         } finally {
             LocalOozie.stop();
         }
     }
 
    private Configuration getWorkflowConfig(WorkflowJob workflow) throws IOException {
        return new XConfiguration(new StringReader(workflow.getConf()));
    }

    private String createTestWorkflowXml(Path subWorkflowAppPath) throws IOException {
        String workflowUri = getTestCaseFileUri("workflow.xml");
        String appXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.4\" name=\"workflow\">" +
                "<global>" +
                "   <configuration>" +
                "        <property>" +
                "            <name>foo2</name>" +
                "            <value>foo2</value>" +
                "        </property>" +
                "        <property>" +
                "            <name>foo3</name>" +
                "            <value>foo3</value>" +
                "        </property>" +
                "    </configuration>" +
                "</global>" +
                "<start to=\"subwf\"/>" +
                "<action name=\"subwf\">" +
                "     <sub-workflow xmlns='uri:oozie:workflow:0.4'>" +
                "          <app-path>" + subWorkflowAppPath.toString() + "</app-path>" +
                "<propagate-configuration/>" +
                "   <configuration>" +
                "        <property>" +
                "            <name>foo3</name>" +
                "            <value>actionconf</value>" +
                "        </property>" +
                "   </configuration>" +
                "     </sub-workflow>" +
                "     <ok to=\"end\"/>" +
                "     <error to=\"fail\"/>" +
                "</action>" +
                "<kill name=\"fail\">" +
                "     <message>Sub workflow failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>" +
                "</kill>" +
                "<end name=\"end\"/>" +
                "</workflow-app>";

        writeToFile(appXml, workflowUri);
        return workflowUri;
    }

    private Path createSubWorkflowXml() throws IOException {
        Path subWorkflowAppPath = getFsTestCaseDir();
        FileSystem fs = getFileSystem();
        Path subWorkflowPath = new Path(subWorkflowAppPath, "workflow.xml");
        Writer writer = new OutputStreamWriter(fs.create(subWorkflowPath));
        writer.write(getWorkflow());
        writer.close();
        return subWorkflowAppPath;
    }

    private void createConfigDefaultXml() throws IOException {
        String config_defaultUri=getTestCaseFileUri("config-default.xml");
        String config_default="<configuration>\n" +
                "<property>\n" +
                "<name>foo</name>\n" +
                "<value>default</value>\n" +
                "</property>\n" +
                "</configuration>";

        writeToFile(config_default, config_defaultUri);
    }

     public String getWorkflow() {
         return  "<workflow-app xmlns='uri:oozie:workflow:0.4' name='app'>" +
                 "<global>" +
diff --git a/release-log.txt b/release-log.txt
index d9abb4b54..67f48e8f6 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2511 SubWorkflow missing variable set from option if config-default is present in parent workflow (asasvari via rkanter)
 OOZIE-2391 spark-opts value in workflow.xml is not parsed properly (gezapeti via rkanter)
 OOZIE-2489 XML parsing is vulnerable (fdenes via rkanter)
 OOZIE-2485 Oozie client keeps trying to use expired auth token (rkanter)
- 
2.19.1.windows.1

