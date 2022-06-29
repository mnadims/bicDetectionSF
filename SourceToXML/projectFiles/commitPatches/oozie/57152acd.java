From 57152acd5c9aa081d9c8357009f8741ee026e352 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Tue, 30 Aug 2016 11:05:23 -0700
Subject: [PATCH] OOZIE-2649 Can't override sub-workflow configuration property
 if defined in parent workflow XML (asasvari via rkanter)

--
 .../oozie/SubWorkflowActionExecutor.java      |  3 +-
 .../workflow/lite/LiteWorkflowAppParser.java  |  9 +-
 .../oozie/TestSubWorkflowActionExecutor.java  | 83 ++++++++++++++-----
 release-log.txt                               |  1 +
 4 files changed, 74 insertions(+), 22 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
index 1ea70970b..b6d2b1228 100644
-- a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
@@ -182,8 +182,6 @@ public class SubWorkflowActionExecutor extends ActionExecutor {
 
                 XConfiguration subWorkflowConf = new XConfiguration();
 
                injectInline(eConf.getChild("configuration", ns), subWorkflowConf);

                 Configuration parentConf = new XConfiguration(new StringReader(context.getWorkflow().getConf()));
 
                 if (eConf.getChild(("propagate-configuration"), ns) != null) {
@@ -212,6 +210,7 @@ public class SubWorkflowActionExecutor extends ActionExecutor {
                     subWorkflowConf.set(OozieClient.GROUP_NAME, group);
                 }
 
                injectInline(eConf.getChild("configuration", ns), subWorkflowConf);
                 injectCallback(context, subWorkflowConf);
                 injectRecovery(extId, subWorkflowConf);
                 injectParent(context.getWorkflow().getId(), subWorkflowConf);
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
index bbd81a944..0541634bb 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
@@ -104,6 +104,8 @@ public class LiteWorkflowAppParser {
     private static final String DECISION_CASE_E = "case";
     private static final String DECISION_DEFAULT_E = "default";
 
    private static final String SUBWORKFLOW_E = "sub-workflow";

     private static final String KILL_MESSAGE_E = "message";
     public static final String VALIDATE_FORK_JOIN = "oozie.validate.ForkJoin";
     public static final String WF_VALIDATE_FORK_JOIN = "oozie.wf.validate.ForkJoin";
@@ -481,7 +483,12 @@ public class LiteWorkflowAppParser {
                             jobConf.set(OOZIE_GLOBAL, getGlobalString(gData));
                         }
                         eActionConf = elem;
                        handleDefaultsAndGlobal(gData, configDefault, elem);
                        if (SUBWORKFLOW_E.equals(elem.getName())) {
                            handleDefaultsAndGlobal(gData, null, elem);
                        }
                        else {
                            handleDefaultsAndGlobal(gData, configDefault, elem);
                        }
                     }
                 }
 
diff --git a/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java b/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
index bdbbfd935..e074d482f 100644
-- a/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
@@ -194,6 +194,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         String defaultConf = workflow.getConf();
         XConfiguration newConf = new XConfiguration(new StringReader(defaultConf));
         newConf.set("abc", "xyz");
        newConf.set("job_prop", "job_prop_val");
         workflow.setConf(newConf.toXmlString());
 
         final WorkflowActionBean action = (WorkflowActionBean) workflow.getActions().get(0);
@@ -205,6 +206,10 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
                 "          <name>a</name>" +
                 "          <value>A</value>" +
                 "        </property>" +
                "        <property>" +
                "          <name>job_prop</name>" +
                "          <value>sub_prop_val</value>" +
                "        </property>" +
                 "      </configuration>" +
                 "</sub-workflow>");
 
@@ -232,6 +237,8 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         WorkflowJob wf = oozieClient.getJobInfo(action.getExternalId());
         Configuration childConf = getWorkflowConfig(wf);
         assertEquals("xyz", childConf.get("abc"));
        assertEquals("A", childConf.get("a"));
        assertEquals("sub_prop_val", childConf.get("job_prop"));
     }
 
     public void testGetGroupFromParent() throws Exception {
@@ -363,6 +370,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         WorkflowJob wf = oozieClient.getJobInfo(action.getExternalId());
         Configuration childConf = getWorkflowConfig(wf);
         assertNull(childConf.get("abc"));
        assertEquals("A", childConf.get("a"));
     }
 
     public void testSubworkflowLib() throws Exception {
@@ -684,7 +692,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
             Path subWorkflowAppPath = createSubWorkflowXml();
 
             createConfigDefaultXml();

            createSubWorkflowConfigDefaultXml();
             String workflowUri = createTestWorkflowXml(subWorkflowAppPath);
 
             LocalOozie.start();
@@ -722,7 +730,10 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
             assertEquals("foo3", actionConf.get("foo3"));
             // Checking the action conf configuration.
             assertEquals("actionconf", subWorkflowConf.get("foo3"));

            assertEquals("subactionconf", actionConf.get("foo4"));
            // config defaults are present
            assertEquals("default", subWorkflowConf.get("parentConfigDefault"));
            assertEquals("default", actionConf.get("subwfConfigDefault"));
         } finally {
             LocalOozie.stop();
         }
@@ -745,6 +756,10 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
                 "            <name>foo3</name>" +
                 "            <value>foo3</value>" +
                 "        </property>" +
                "        <property>" +
                "            <name>foo4</name>" +
                "            <value>actionconf</value>" +
                "        </property>" +
                 "    </configuration>" +
                 "</global>" +
                 "<start to=\"subwf\"/>" +
@@ -773,27 +788,51 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
     }
 
     private Path createSubWorkflowXml() throws IOException {
        Path subWorkflowAppPath = getFsTestCaseDir();
        FileSystem fs = getFileSystem();
        Path subWorkflowPath = new Path(subWorkflowAppPath, "workflow.xml");
        Writer writer = new OutputStreamWriter(fs.create(subWorkflowPath));
        writer.write(getWorkflow());
        writer.close();
        return subWorkflowAppPath;
        return createSubWorkflowFile(getWorkflow(), "workflow.xml");
     }
 
     private void createConfigDefaultXml() throws IOException {
        String config_defaultUri=getTestCaseFileUri("config-default.xml");
        String config_default="<configuration>\n" +
                "<property>\n" +
                "<name>foo</name>\n" +
                "<value>default</value>\n" +
                "</property>\n" +
        String config_defaultUri = getTestCaseFileUri("config-default.xml");
        String config_default =
                "<configuration>" +
                "    <property>" +
                "      <name>foo</name>" +
                "      <value>default</value>" +
                "    </property>" +
                "    <property>" +
                "      <name>parentConfigDefault</name>" +
                "      <value>default</value>" +
                "    </property>" +
                 "</configuration>";
 
         writeToFile(config_default, config_defaultUri);
     }
 
    private void createSubWorkflowConfigDefaultXml() throws IOException {
        String config_default = "<configuration>" +
                        "    <property>" +
                        "      <name>subwfConfigDefault</name>" +
                        "      <value>default</value>" +
                        "    </property>" +
                        "    <property>" +
                        "      <name>foo4</name>" +
                        "      <value>default</value>" +
                        "    </property>" +
                        "</configuration>";
        createSubWorkflowFile(config_default, "config-default.xml");
    }

    private Path createSubWorkflowFile(String content, String fileName) throws IOException
    {
        Path subWorkflowAppPath = getFsTestCaseDir();
        FileSystem fs = getFileSystem();
        Path subWorkflowPath = new Path(subWorkflowAppPath, fileName);
        Writer writer = new OutputStreamWriter(fs.create(subWorkflowPath));
        writer.write(content);
        writer.close();
        return subWorkflowAppPath;
    }

     public String getWorkflow() {
         return  "<workflow-app xmlns='uri:oozie:workflow:0.4' name='app'>" +
                 "<global>" +
@@ -811,10 +850,16 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
                 "<start to='java' />" +
                 "<action name='java'>" +
                 "<java>" +
                "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                "<name-node>" + getNameNodeUri() + "</name-node>" +
                "<main-class>" + LauncherMainTester.class.getName() + "</main-class>" +
                "<arg>exit0</arg>" +
                "    <job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                "    <name-node>" + getNameNodeUri() + "</name-node>" +
                "        <configuration>" +
                "            <property>" +
                "                <name>foo4</name>" +
                "                <value>subactionconf</value>" +
                "            </property>" +
                "        </configuration>" +
                "    <main-class>" + LauncherMainTester.class.getName() + "</main-class>" +
                "    <arg>exit0</arg>" +
                 "</java>"
                 + "<ok to='end' />"
                 + "<error to='fail' />"
diff --git a/release-log.txt b/release-log.txt
index 58e8f9305..37f3b71cf 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2649 Can't override sub-workflow configuration property if defined in parent workflow XML (asasvari via rkanter)
 OOZIE-2656 OozieShareLibCLI uses op system username instead of Kerberos to upload jars (gezapeti via rkanter)
 OOZIE-1173 Refactor: use ParamChecker inXOozieClient (abhishekbafna via jaydeepvishwakarma)
 OOZIE-2657 Clean up redundant access modifiers from oozie interfaces (abhishekbafna via jaydeepvishwakarma)
- 
2.19.1.windows.1

