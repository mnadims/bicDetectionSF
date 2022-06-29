From cd91b960477235e891566feb9a07174d61756f6b Mon Sep 17 00:00:00 2001
From: mona <chitnis@yahoo-inc.com>
Date: Mon, 28 Jul 2014 18:54:47 -0700
Subject: [PATCH] OOZIE-1944 Recursive variable resolution broken when same
 parameter name in config-default and action conf (mona)

--
 .../workflow/lite/LiteWorkflowAppParser.java  | 21 +++--
 release-log.txt                               |  1 +
 .../hadoop/TestMapReduceActionExecutor.java   | 81 +++++++++++++++----
 3 files changed, 75 insertions(+), 28 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
index e47e619b9..6cc2b833c 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
@@ -627,21 +627,20 @@ public class LiteWorkflowAppParser {
                 }
             }
             try {
                XConfiguration actionConf;
                Element actionConfiguration = eActionConf.getChild("configuration", actionNs);
                if (actionConfiguration == null) {
                    actionConf = new XConfiguration();
                }
                else {
                    actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(actionConfiguration)
                            .toString()));
                }
                XConfiguration actionConf = new XConfiguration();
                if (configDefault != null)
                    XConfiguration.copy(configDefault, actionConf);
                 if (globalConfiguration != null) {
                     Configuration globalConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(
                             globalConfiguration).toString()));
                    XConfiguration.injectDefaults(globalConf, actionConf);
                    XConfiguration.copy(globalConf, actionConf);
                }
                Element actionConfiguration = eActionConf.getChild("configuration", actionNs);
                if (actionConfiguration != null) {
                    //copy and override
                    XConfiguration.copy(new XConfiguration(new StringReader(XmlUtils.prettyPrint(
                            actionConfiguration).toString())), actionConf);
                 }
                XConfiguration.injectDefaults(configDefault, actionConf);
                 int position = eActionConf.indexOf(actionConfiguration);
                 eActionConf.removeContent(actionConfiguration); //replace with enhanced one
                 Element eConfXml = XmlUtils.parseXml(actionConf.toXmlString(false));
diff --git a/release-log.txt b/release-log.txt
index 6ec2bcb01..869931427 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -5,6 +5,7 @@ OOZIE-1943 Bump up trunk to 4.2.0-SNAPSHOT (bzhang)
 
 -- Oozie 4.1.0 release (4.1 - unreleased)
 
OOZIE-1944 Recursive variable resolution broken when same parameter name in config-default and action conf (mona)
 OOZIE-1906 Service to periodically remove ZK lock (puru via rohini)
 OOZIE-1812 Bulk API with bundle Id should relax regex check for Id (puru via rohini)
 OOZIE-1915 Move system properties to conf properties (puru via rohini)
diff --git a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
index 7e60289de..e78c98a3b 100644
-- a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
++ b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
@@ -80,6 +80,15 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
     }
 
     public void testConfigDefaultPropsToAction() throws Exception {
        String actionXml = "<map-reduce>"
                + "        <prepare>"
                + "          <delete path=\"${nameNode}/user/${wf:user()}/mr/${outputDir}\"/>"
                + "        </prepare>"
                + "        <configuration>"
                + "          <property><name>bb</name><value>BB</value></property>"
                + "          <property><name>cc</name><value>from_action</value></property>"
                + "        </configuration>"
                + "      </map-reduce>";
         String wfXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.5\" name=\"map-reduce-wf\">"
         + "<global>"
         + "<job-tracker>${jobTracker}</job-tracker>"
@@ -88,15 +97,7 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         + "</global>"
         + "    <start to=\"mr-node\"/>"
         + "    <action name=\"mr-node\">"
        + "      <map-reduce>"
        + "        <prepare>"
        + "          <delete path=\"${nameNode}/user/${wf:user()}/mr/${outputDir}\"/>"
        + "        </prepare>"
        + "        <configuration>"
        + "          <property><name>bb</name><value>BB</value></property>"
        + "          <property><name>cc</name><value>from_action</value></property>"
        + "        </configuration>"
        + "      </map-reduce>"
        + actionXml
         + "    <ok to=\"end\"/>"
         + "    <error to=\"fail\"/>"
         + "</action>"
@@ -118,7 +119,7 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
 
         OutputStream os = new FileOutputStream(getTestCaseDir() + "/config-default.xml");
         XConfiguration defaultConf = new XConfiguration();
        defaultConf.set("outputDir", "output-data-dir");
        defaultConf.set("outputDir", "default-output-dir");
         defaultConf.set("mapred.mapper.class", "MM");
         defaultConf.set("mapred.reducer.class", "RR");
         defaultConf.set("cc", "from_default");
@@ -133,18 +134,16 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
                 wfId + "@mr-node");
 
         // check NN and JT settings
        Element eConf = XmlUtils.parseXml(mrAction.getConf());
        eConf = eConf.getChild("name-node", eConf.getNamespace());
        Element eAction = XmlUtils.parseXml(mrAction.getConf());
        Element eConf = eAction.getChild("name-node", eAction.getNamespace());
         assertEquals(getNameNodeUri(), eConf.getText());
        eConf = XmlUtils.parseXml(mrAction.getConf());
        eConf = eConf.getChild("job-tracker", eConf.getNamespace());
        eConf = eAction.getChild("job-tracker", eAction.getNamespace());
         assertEquals(getJobTrackerUri(), eConf.getText());
 
         // check other m-r settings
        eConf = XmlUtils.parseXml(mrAction.getConf());
        eConf = eConf.getChild("configuration", eConf.getNamespace());
        eConf = eAction.getChild("configuration", eAction.getNamespace());
         Configuration actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(eConf).toString()));
        assertEquals("output-data-dir", actionConf.get("outputDir"));
        assertEquals("default-output-dir", actionConf.get("outputDir"));
         assertEquals("MM", actionConf.get("mapred.mapper.class"));
         assertEquals("RR", actionConf.get("mapred.reducer.class"));
         // check that default did not overwrite same property explicit in action conf
@@ -153,6 +152,54 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         assertEquals("AA", actionConf.get("aa"));
         assertEquals("BB", actionConf.get("bb"));
 
        //test no infinite recursion by param referring to itself e.g. path = ${path}/sub-path
        actionXml = "<map-reduce>"
                + "        <prepare>"
                + "          <delete path=\"${nameNode}/user/${wf:user()}/mr/${outputDir}\"/>"
                + "        </prepare>"
                + "        <configuration>"
                + "          <property><name>cc</name><value>${cc}/action_cc</value></property>"
                + "        </configuration>"
                + "      </map-reduce>";

        wfXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.5\" name=\"map-reduce-wf\">"
                + "<global>"
                + "<job-tracker>${jobTracker}</job-tracker>"
                + "<name-node>${nameNode}</name-node>"
                + "<configuration><property><name>outputDir</name><value>global-output-dir</value></property></configuration>"
                + "</global>"
                + "    <start to=\"mr-node\"/>"
                + "    <action name=\"mr-node\">"
                + actionXml
                + "    <ok to=\"end\"/>"
                + "    <error to=\"fail\"/>"
                + "</action>"
                + "<kill name=\"fail\">"
                + "    <message>Map/Reduce failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>"
                + "</kill>"
                + "<end name=\"end\"/>"
                + "</workflow-app>";

         writer = new FileWriter(getTestCaseDir() + "/workflow.xml");
         IOUtils.copyCharStream(new StringReader(wfXml), writer);

         wfId = new SubmitXCommand(conf).call();
         new StartXCommand(wfId).call();
         sleep(3000);

         mrAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION,
                 wfId + "@mr-node");

         // check param
         eAction = XmlUtils.parseXml(mrAction.getConf());
         eConf = eAction.getChild("configuration", eAction.getNamespace());
         actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(eConf).toString()));
         // action param referring to same param name given in defaults cc = ${cc}/action_cc
         assertEquals("from_default/action_cc", actionConf.get("cc"));
         // check global is retained and has precedence over config-default
         eConf = eAction.getChild("name-node", eAction.getNamespace());
         assertEquals(getNameNodeUri(), eConf.getText());
         assertEquals("global-output-dir", actionConf.get("outputDir"));
     }
 
     @SuppressWarnings("unchecked")
- 
2.19.1.windows.1

