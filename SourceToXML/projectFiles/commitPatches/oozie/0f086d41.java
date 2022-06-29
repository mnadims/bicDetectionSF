From 0f086d41b8f274abe0959705f76fa3225ab5ff2e Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Sat, 9 Jan 2016 16:02:12 -0800
Subject: [PATCH] OOZIE-2030 Configuration properties from global section is
 not getting set in Hadoop job conf when using sub-workflow action in Oozie
 workflow.xml (jaydeepvishwakarma via rohini)

--
 .../oozie/SubWorkflowActionExecutor.java      |   4 +
 .../workflow/lite/LiteWorkflowAppParser.java  | 135 ++++++++++++++++--
 .../oozie/TestSubWorkflowActionExecutor.java  | 113 ++++++++++++++-
 release-log.txt                               |   1 +
 4 files changed, 238 insertions(+), 15 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
index 33efc6053..6bf35983b 100644
-- a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
@@ -306,4 +306,8 @@ public class SubWorkflowActionExecutor extends ActionExecutor {
     public boolean isCompleted(String externalStatus) {
         return FINAL_STATUS.contains(externalStatus);
     }

    public boolean supportsConfigurationJobXML() {
        return true;
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
index d3a652360..03c84f15b 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
@@ -18,8 +18,10 @@
 
 package org.apache.oozie.workflow.lite;
 
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.io.Writable;
import org.apache.oozie.action.oozie.SubWorkflowActionExecutor;
 import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.workflow.WorkflowException;
 import org.apache.oozie.util.ELUtils;
 import org.apache.oozie.util.IOUtils;
 import org.apache.oozie.util.XConfiguration;
@@ -27,7 +29,9 @@ import org.apache.oozie.util.XmlUtils;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.ParameterVerifier;
 import org.apache.oozie.util.ParameterVerifierException;
import org.apache.oozie.util.WritableUtils;
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.workflow.WorkflowException;
 import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.ActionService;
@@ -46,6 +50,12 @@ import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
 import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Deque;
@@ -54,6 +64,7 @@ import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
import java.util.zip.*;
 
 /**
  * Class to parse and validate workflow xml
@@ -95,6 +106,7 @@ public class LiteWorkflowAppParser {
 
     public static final String DEFAULT_NAME_NODE = "oozie.actions.default.name-node";
     public static final String DEFAULT_JOB_TRACKER = "oozie.actions.default.job-tracker";
    public static final String OOZIE_GLOBAL = "oozie.wf.globalconf";
 
     private static final String JOB_TRACKER = "job-tracker";
     private static final String NAME_NODE = "name-node";
@@ -417,7 +429,9 @@ public class LiteWorkflowAppParser {
             throws WorkflowException {
         Namespace ns = root.getNamespace();
         LiteWorkflowApp def = null;
        GlobalSectionData gData = null;
        GlobalSectionData gData = jobConf.get(OOZIE_GLOBAL) == null ?
                null : getGlobalFromString(jobConf.get(OOZIE_GLOBAL));
        boolean serializedGlobalConf = false;
         for (Element eNode : (List<Element>) root.getChildren()) {
             if (eNode.getName().equals(START_E)) {
                 def = new LiteWorkflowApp(root.getAttributeValue(NAME_A), strDef,
@@ -457,6 +471,11 @@ public class LiteWorkflowAppParser {
                     } else if (SLA_INFO.equals(elem.getName()) || CREDENTIALS.equals(elem.getName())) {
                         continue;
                     } else {
                        if (!serializedGlobalConf  && elem.getName().equals(SubWorkflowActionExecutor.ACTION_TYPE) &&
                                elem.getChild(("propagate-configuration"), ns) != null) {
                            serializedGlobalConf = true;
                            jobConf.set(OOZIE_GLOBAL, getGlobalString(gData));
                        }
                         eActionConf = elem;
                         handleDefaultsAndGlobal(gData, configDefault, elem);
                     }
@@ -484,6 +503,10 @@ public class LiteWorkflowAppParser {
             } else if (SLA_INFO.equals(eNode.getName()) || CREDENTIALS.equals(eNode.getName())) {
                 // No operation is required
             } else if (eNode.getName().equals(GLOBAL)) {
                if(jobConf.get(OOZIE_GLOBAL) != null) {
                    gData = getGlobalFromString(jobConf.get(OOZIE_GLOBAL));
                    handleDefaultsAndGlobal(gData, null, eNode);
                }
                 gData = parseGlobalSection(ns, eNode);
             } else if (eNode.getName().equals(PARAMETERS)) {
                 // No operation is required
@@ -494,6 +517,47 @@ public class LiteWorkflowAppParser {
         return def;
     }
 
    /**
     * Read the GlobalSectionData from Base64 string.
     * @param globalStr
     * @return GlobalSectionData
     * @throws WorkflowException
     */
    private GlobalSectionData getGlobalFromString(String globalStr) throws WorkflowException {
        GlobalSectionData globalSectionData = new GlobalSectionData();
        try {
            byte[] data = Base64.decodeBase64(globalStr);
            Inflater inflater = new Inflater();
            DataInputStream ois = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data), inflater));
            globalSectionData.readFields(ois);
            ois.close();
        } catch (Exception ex) {
            throw new WorkflowException(ErrorCode.E0700, "Error while processing global section conf");
        }
        return globalSectionData;
    }


    /**
     * Write the GlobalSectionData to a Base64 string.
     * @param globalSectionData
     * @return String
     * @throws WorkflowException
     */
    private String getGlobalString(GlobalSectionData globalSectionData) throws WorkflowException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream oos = null;
        try {
            Deflater def = new Deflater();
            oos = new DataOutputStream(new DeflaterOutputStream(baos, def));
            globalSectionData.write(oos);
            oos.close();
        } catch (IOException e) {
            throw new WorkflowException(ErrorCode.E0700, "Error while processing global section conf");
        }
        return Base64.encodeBase64String(baos.toByteArray());
    }

     /**
      * Validate workflow xml
      *
@@ -570,11 +634,14 @@ public class LiteWorkflowAppParser {
         parent.addContent(child);
     }
 
    private class GlobalSectionData {
        final String jobTracker;
        final String nameNode;
        final List<String> jobXmls;
        final Configuration conf;
    private class GlobalSectionData implements Writable {
        String jobTracker;
        String nameNode;
        List<String> jobXmls;
        Configuration conf;

        public GlobalSectionData() {
        }
 
         public GlobalSectionData(String jobTracker, String nameNode, List<String> jobXmls, Configuration conf) {
             this.jobTracker = jobTracker;
@@ -582,6 +649,43 @@ public class LiteWorkflowAppParser {
             this.jobXmls = jobXmls;
             this.conf = conf;
         }

        @Override
        public void write(DataOutput dataOutput) throws IOException {
            WritableUtils.writeStr(dataOutput, jobTracker);
            WritableUtils.writeStr(dataOutput, nameNode);

            if(jobXmls != null && !jobXmls.isEmpty()) {
                dataOutput.writeInt(jobXmls.size());
                for (String content : jobXmls) {
                    WritableUtils.writeStr(dataOutput, content);
                }
            } else {
                dataOutput.writeInt(0);
            }
            if(conf != null) {
                WritableUtils.writeStr(dataOutput, XmlUtils.prettyPrint(conf).toString());
            } else {
                WritableUtils.writeStr(dataOutput, null);
            }
        }

        @Override
        public void readFields(DataInput dataInput) throws IOException {
            jobTracker = WritableUtils.readStr(dataInput);
            nameNode = WritableUtils.readStr(dataInput);
            int length = dataInput.readInt();
            if (length > 0) {
                jobXmls = new ArrayList<String>();
                for (int i = 0; i < length; i++) {
                    jobXmls.add(WritableUtils.readStr(dataInput));
                }
            }
            String confString = WritableUtils.readStr(dataInput);
            if(confString != null) {
                conf = new XConfiguration(new StringReader(confString));
            }
        }
     }
 
     private GlobalSectionData parseGlobalSection(Namespace ns, Element global) throws WorkflowException {
@@ -625,20 +729,23 @@ public class LiteWorkflowAppParser {
 
     private void handleDefaultsAndGlobal(GlobalSectionData gData, Configuration configDefault, Element actionElement)
             throws WorkflowException {

         ActionExecutor ae = Services.get().get(ActionService.class).getExecutor(actionElement.getName());
        if (ae == null) {
        if (ae == null && !GLOBAL.equals(actionElement.getName())) {
             throw new WorkflowException(ErrorCode.E0723, actionElement.getName(), ActionService.class.getName());
         }
 
         Namespace actionNs = actionElement.getNamespace();
 
        if (ae.requiresNameNodeJobTracker()) {
        if (SubWorkflowActionExecutor.ACTION_TYPE.equals(actionElement.getName()) ||
                GLOBAL.equals(actionElement.getName()) || ae.requiresNameNodeJobTracker()) {
             if (actionElement.getChild(NAME_NODE, actionNs) == null) {
                 if (gData != null && gData.nameNode != null) {
                     addChildElement(actionElement, actionNs, NAME_NODE, gData.nameNode);
                 } else if (defaultNameNode != null) {
                     addChildElement(actionElement, actionNs, NAME_NODE, defaultNameNode);
                } else {
                } else if (!(SubWorkflowActionExecutor.ACTION_TYPE.equals(actionElement.getName()) ||
                        GLOBAL.equals(actionElement.getName()))) {
                     throw new WorkflowException(ErrorCode.E0701, "No " + NAME_NODE + " defined");
                 }
             }
@@ -647,13 +754,14 @@ public class LiteWorkflowAppParser {
                     addChildElement(actionElement, actionNs, JOB_TRACKER, gData.jobTracker);
                 } else if (defaultJobTracker != null) {
                     addChildElement(actionElement, actionNs, JOB_TRACKER, defaultJobTracker);
                } else {
                } else if (!(SubWorkflowActionExecutor.ACTION_TYPE.equals(actionElement.getName()) ||
                        GLOBAL.equals(actionElement.getName()))) {
                     throw new WorkflowException(ErrorCode.E0701, "No " + JOB_TRACKER + " defined");
                 }
             }
         }
 
        if (ae.supportsConfigurationJobXML()) {
        if ( GLOBAL.equals(actionElement.getName()) || ae.supportsConfigurationJobXML()) {
             @SuppressWarnings("unchecked")
             List<Element> actionJobXmls = actionElement.getChildren(JOB_XML, actionNs);
             if (gData != null && gData.jobXmls != null) {
@@ -706,5 +814,4 @@ public class LiteWorkflowAppParser {
             }
         }
     }

}
}
\ No newline at end of file
diff --git a/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java b/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
index 9ab897ac8..26e5031af 100644
-- a/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/oozie/TestSubWorkflowActionExecutor.java
@@ -24,6 +24,7 @@ import org.apache.hadoop.fs.Path;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.action.hadoop.ActionExecutorTestCase;
import org.apache.oozie.action.hadoop.LauncherMainTester;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.WorkflowJob;
@@ -33,6 +34,8 @@ import org.apache.oozie.service.Services;
 import org.apache.oozie.service.WorkflowAppService;
 import org.apache.oozie.service.XLogService;
 import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
 
 import java.io.*;
 import java.net.URI;
@@ -544,7 +547,7 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
     }
 
     public String getLazyWorkflow() {
        return  "<workflow-app xmlns='uri:oozie:workflow:0.3' name='app'>" +
        return  "<workflow-app xmlns='uri:oozie:workflow:0.4' name='app'>" +
                 "<start to='java' />" +
                 "       <action name='java'>" +
                 "<java>" +
@@ -634,4 +637,112 @@ public class TestSubWorkflowActionExecutor extends ActionExecutorTestCase {
         }
 
     }

    public void testParentGlobalConf() throws Exception {
        try {
            Path subWorkflowAppPath = getFsTestCaseDir();
            FileSystem fs = getFileSystem();
            Path subWorkflowPath = new Path(subWorkflowAppPath, "workflow.xml");
            Writer writer = new OutputStreamWriter(fs.create(subWorkflowPath));
            writer.write(getWorkflow());
            writer.close();

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
            Configuration subWorkflowConf = new XConfiguration(new StringReader(subWorkflow.getConf()));
            Element eConf = XmlUtils.parseXml(subWorkflow.getActions().get(1).getConf());
            Element element = eConf.getChild("configuration", eConf.getNamespace());
            Configuration actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(element).toString()));
            assertEquals(actionConf.get("foo1"), "foo1");
            assertEquals(actionConf.get("foo2"), "subconf");
            assertEquals(actionConf.get("foo3"), "foo3");
            // Checking the action conf configuration.
            assertEquals(subWorkflowConf.get("foo3"), "actionconf");
        } finally {
            LocalOozie.stop();
        }
    }

    public String getWorkflow() {
        return  "<workflow-app xmlns='uri:oozie:workflow:0.4' name='app'>" +
                "<global>" +
                "   <configuration>" +
                "        <property>" +
                "            <name>foo1</name>" +
                "            <value>foo1</value>" +
                "        </property>" +
                "        <property>" +
                "            <name>foo2</name>" +
                "            <value>subconf</value>" +
                "        </property>" +
                "    </configuration>" +
                "</global>" +
                "<start to='java' />" +
                "<action name='java'>" +
                "<java>" +
                "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                "<name-node>" + getNameNodeUri() + "</name-node>" +
                "<main-class>" + LauncherMainTester.class.getName() + "</main-class>" +
                "<arg>exit0</arg>" +
                "</java>"
                + "<ok to='end' />"
                + "<error to='fail' />"
                + "</action>"
                + "<kill name='fail'>"
                + "<message>shell action fail, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>"
                + "</kill>"
                + "<end name='end' />"
                + "</workflow-app>";
    }
 }
diff --git a/release-log.txt b/release-log.txt
index 275db88d2..bf8d35faa 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2030 Configuration properties from global section is not getting set in Hadoop job conf when using sub-workflow action in Oozie workflow.xml (jaydeepvishwakarma via rohini)
 OOZIE-2380 Oozie Hive action failed with wrong tmp path (vaifer via rkanter)
 OOZIE-2222 Oozie UI parent job should be clickable (puru)
 OOZIE-2407 AbandonedService should not send mail if there is no abandoned coord (puru)
- 
2.19.1.windows.1

