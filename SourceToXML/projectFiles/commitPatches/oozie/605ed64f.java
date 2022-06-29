From 605ed64f75172f1ad2ea098023f81b42475c6cd6 Mon Sep 17 00:00:00 2001
From: Mona Chitnis <chitnis@yahoo-inc.com>
Date: Tue, 28 Jan 2014 11:53:56 -0800
Subject: [PATCH] OOZIE-1644 Default config from config-default.xml is not
 propagated to actions (mona)

--
 .../oozie/command/wf/ReRunXCommand.java       |  2 +-
 .../oozie/command/wf/SubmitXCommand.java      | 13 ++-
 .../oozie/service/LiteWorkflowAppService.java | 21 +++--
 .../oozie/service/WorkflowAppService.java     | 21 +++--
 .../org/apache/oozie/util/XConfiguration.java |  8 +-
 .../apache/oozie/workflow/WorkflowLib.java    |  9 +-
 .../workflow/lite/LiteWorkflowAppParser.java  | 79 ++++++++++-------
 .../oozie/workflow/lite/LiteWorkflowLib.java  |  8 +-
 .../lite/TestLiteWorkflowAppParser.java       | 48 ++++++-----
 .../site/twiki/WorkflowFunctionalSpec.twiki   | 30 ++++---
 release-log.txt                               |  1 +
 .../hadoop/TestMapReduceActionExecutor.java   | 85 ++++++++++++++++++-
 12 files changed, 227 insertions(+), 98 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
index 433737af6..fe588d4da 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ReRunXCommand.java
@@ -126,7 +126,7 @@ public class ReRunXCommand extends WorkflowXCommand<Void> {
         WorkflowAppService wps = Services.get().get(WorkflowAppService.class);
         try {
             XLog.Info.get().setParameter(DagXLogInfoService.TOKEN, conf.get(OozieClient.LOG_TOKEN));
            WorkflowApp app = wps.parseDef(conf);
            WorkflowApp app = wps.parseDef(conf, null);
             XConfiguration protoActionConf = wps.createProtoActionConf(conf, true);
             WorkflowLib workflowLib = Services.get().get(WorkflowStoreService.class).getWorkflowLibWithNoDB();
 
diff --git a/core/src/main/java/org/apache/oozie/command/wf/SubmitXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/SubmitXCommand.java
index a81bf1019..0a68673d7 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/SubmitXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/SubmitXCommand.java
@@ -33,7 +33,6 @@ import org.apache.oozie.service.WorkflowAppService;
 import org.apache.oozie.service.HadoopAccessorService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.DagXLogInfoService;
import org.apache.oozie.util.ConfigUtils;
 import org.apache.oozie.util.ELUtils;
 import org.apache.oozie.util.LogUtils;
 import org.apache.oozie.sla.SLAOperations;
@@ -131,18 +130,14 @@ public class SubmitXCommand extends WorkflowXCommand<String> {
         WorkflowAppService wps = Services.get().get(WorkflowAppService.class);
         try {
             XLog.Info.get().setParameter(DagXLogInfoService.TOKEN, conf.get(OozieClient.LOG_TOKEN));
            WorkflowApp app = wps.parseDef(conf);
            XConfiguration protoActionConf = wps.createProtoActionConf(conf, true);
            WorkflowLib workflowLib = Services.get().get(WorkflowStoreService.class).getWorkflowLibWithNoDB();

             String user = conf.get(OozieClient.USER_NAME);
            String group = ConfigUtils.getWithDeprecatedCheck(conf, OozieClient.JOB_ACL, OozieClient.GROUP_NAME, null);
             URI uri = new URI(conf.get(OozieClient.APP_PATH));
             HadoopAccessorService has = Services.get().get(HadoopAccessorService.class);
             Configuration fsConf = has.createJobConf(uri.getAuthority());
             FileSystem fs = has.createFileSystem(user, uri, fsConf);
 
             Path configDefault = null;
            Configuration defaultConf = null;
             // app path could be a directory
             Path path = new Path(uri.getPath());
             if (!fs.isFile(path)) {
@@ -153,7 +148,7 @@ public class SubmitXCommand extends WorkflowXCommand<String> {
 
             if (fs.exists(configDefault)) {
                 try {
                    Configuration defaultConf = new XConfiguration(fs.open(configDefault));
                    defaultConf = new XConfiguration(fs.open(configDefault));
                     PropertiesUtils.checkDisallowedProperties(defaultConf, DISALLOWED_DEFAULT_PROPERTIES);
                     XConfiguration.injectDefaults(defaultConf, conf);
                 }
@@ -162,6 +157,10 @@ public class SubmitXCommand extends WorkflowXCommand<String> {
                 }
             }
 
            WorkflowApp app = wps.parseDef(conf, defaultConf);
            XConfiguration protoActionConf = wps.createProtoActionConf(conf, true);
            WorkflowLib workflowLib = Services.get().get(WorkflowStoreService.class).getWorkflowLibWithNoDB();

             PropertiesUtils.checkDisallowedProperties(conf, DISALLOWED_USER_PROPERTIES);
 
             // Resolving all variables in the job properties.
diff --git a/core/src/main/java/org/apache/oozie/service/LiteWorkflowAppService.java b/core/src/main/java/org/apache/oozie/service/LiteWorkflowAppService.java
index 40eeeff33..0e29a0956 100644
-- a/core/src/main/java/org/apache/oozie/service/LiteWorkflowAppService.java
++ b/core/src/main/java/org/apache/oozie/service/LiteWorkflowAppService.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -36,14 +36,25 @@ public class LiteWorkflowAppService extends WorkflowAppService {
      * @return workflow application.
      */
     public WorkflowApp parseDef(Configuration jobConf) throws WorkflowException {
        return parseDef(jobConf, null);
    }

    public WorkflowApp parseDef(Configuration jobConf, Configuration configDefault) throws WorkflowException {
         String appPath = ParamChecker.notEmpty(jobConf.get(OozieClient.APP_PATH), OozieClient.APP_PATH);
         String user = ParamChecker.notEmpty(jobConf.get(OozieClient.USER_NAME), OozieClient.USER_NAME);
         String workflowXml = readDefinition(appPath, user, jobConf);
        return parseDef(workflowXml, jobConf);
        return parseDef(workflowXml, jobConf, configDefault);
     }
 
    public WorkflowApp parseDef(String workflowXml, Configuration jobConf) throws WorkflowException {
    @Override
    public WorkflowApp parseDef(String wfXml, Configuration jobConf) throws WorkflowException {
        return parseDef(wfXml, jobConf, null);
    }

    public WorkflowApp parseDef(String workflowXml, Configuration jobConf, Configuration configDefault)
            throws WorkflowException {
         WorkflowLib workflowLib = Services.get().get(WorkflowStoreService.class).getWorkflowLibWithNoDB();
        return workflowLib.parseDef(workflowXml, jobConf);
        return workflowLib.parseDef(workflowXml, jobConf, configDefault);
     }

 }
diff --git a/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java b/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java
index 81535a464..c2f38365a 100644
-- a/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java
++ b/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -262,12 +262,23 @@ public abstract class WorkflowAppService implements Service {
     /**
      * Parse workflow definition.
      *
     * @param jobConf job configuration.
     * @return workflow application.
     * @throws WorkflowException thrown if the workflow application could not be parsed.
     * @param jobConf
     * @return
     * @throws WorkflowException
      */
     public abstract WorkflowApp parseDef(Configuration jobConf) throws WorkflowException;
 
    /**
     * Parse workflow definition along with config-default.xml config
     *
     * @param jobConf job configuration
     * @param configDefault config from config-default.xml
     * @return workflow application thrown if the workflow application could not
     *         be parsed
     * @throws WorkflowException
     */
    public abstract WorkflowApp parseDef(Configuration jobConf, Configuration configDefault) throws WorkflowException;

     /**
      * Parse workflow definition.
      * @param wfXml workflow.
diff --git a/core/src/main/java/org/apache/oozie/util/XConfiguration.java b/core/src/main/java/org/apache/oozie/util/XConfiguration.java
index 0b1d8e420..c16ab80c0 100644
-- a/core/src/main/java/org/apache/oozie/util/XConfiguration.java
++ b/core/src/main/java/org/apache/oozie/util/XConfiguration.java
@@ -200,9 +200,11 @@ public class XConfiguration extends Configuration {
      * @param target target configuration.
      */
     public static void injectDefaults(Configuration source, Configuration target) {
        for (Map.Entry<String, String> entry : source) {
            if (target.get(entry.getKey()) == null) {
                target.set(entry.getKey(), entry.getValue());
        if (source != null) {
            for (Map.Entry<String, String> entry : source) {
                if (target.get(entry.getKey()) == null) {
                    target.set(entry.getKey(), entry.getValue());
                }
             }
         }
     }
diff --git a/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java b/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java
index d32f770ad..7e4c90a66 100644
-- a/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java
++ b/core/src/main/java/org/apache/oozie/workflow/WorkflowLib.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -32,11 +32,12 @@ public interface WorkflowLib {
      *
      * @param wfXml string containing the workflow definition.
      * @param jobConf job configuration
     * @param configDefault configuration from config-default.xml
      * @return the parse workflow application.
      * @throws WorkflowException thrown if the definition could not be parsed.
      */
    public WorkflowApp parseDef(String wfXml, Configuration jobConf) throws WorkflowException;

    public WorkflowApp parseDef(String wfXml, Configuration jobConf, Configuration configDefault)
            throws WorkflowException;
 
     /**
      * Create a workflow instance.
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
index 31852b469..685503a47 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
@@ -19,6 +19,7 @@ package org.apache.oozie.workflow.lite;
 
 import org.apache.oozie.workflow.WorkflowException;
 import org.apache.oozie.util.IOUtils;
import org.apache.oozie.util.XConfiguration;
 import org.apache.oozie.util.XmlUtils;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.ParameterVerifier;
@@ -36,6 +37,7 @@ import org.xml.sax.SAXException;
 import javax.xml.transform.stream.StreamSource;
 import javax.xml.validation.Schema;
 import javax.xml.validation.Validator;

 import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
@@ -125,6 +127,10 @@ public class LiteWorkflowAppParser {
         this.actionHandlerClass = actionHandlerClass;
     }
 
    public LiteWorkflowApp validateAndParse(Reader reader, Configuration jobConf) throws WorkflowException {
        return validateAndParse(reader, jobConf, null);
    }

     /**
      * Parse and validate xml to {@link LiteWorkflowApp}
      *
@@ -132,7 +138,8 @@ public class LiteWorkflowAppParser {
      * @return LiteWorkflowApp
      * @throws WorkflowException
      */
    public LiteWorkflowApp validateAndParse(Reader reader, Configuration jobConf) throws WorkflowException {
    public LiteWorkflowApp validateAndParse(Reader reader, Configuration jobConf, Configuration configDefault)
            throws WorkflowException {
         try {
             StringWriter writer = new StringWriter();
             IOUtils.copyCharStream(reader, writer);
@@ -145,7 +152,7 @@ public class LiteWorkflowAppParser {
 
             Element wfDefElement = XmlUtils.parseXml(strDef);
             ParameterVerifier.verifyParameters(jobConf, wfDefElement);
            LiteWorkflowApp app = parse(strDef, wfDefElement);
            LiteWorkflowApp app = parse(strDef, wfDefElement, configDefault);
             Map<String, VisitStatus> traversed = new HashMap<String, VisitStatus>();
             traversed.put(app.getNode(StartNodeDef.START).getName(), VisitStatus.VISITING);
             validate(app, app.getNode(StartNodeDef.START), traversed);
@@ -372,8 +379,8 @@ public class LiteWorkflowAppParser {
      * @return LiteWorkflowApp
      * @throws WorkflowException
      */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private LiteWorkflowApp parse(String strDef, Element root) throws WorkflowException {
    @SuppressWarnings({"unchecked"})
    private LiteWorkflowApp parse(String strDef, Element root, Configuration configDefault) throws WorkflowException {
         Namespace ns = root.getNamespace();
         LiteWorkflowApp def = null;
         Element global = null;
@@ -435,7 +442,7 @@ public class LiteWorkflowAppParser {
                                                     }
                                                     else {
                                                         eActionConf = elem;
                                                        handleGlobal(ns, global, elem);
                                                        handleGlobal(ns, global, configDefault, elem);
                                                         }
                                                 }
                                             }
@@ -557,9 +564,12 @@ public class LiteWorkflowAppParser {
      * @throws WorkflowException
      */
 
    private void handleGlobal(Namespace ns, Element global, Element eActionConf) throws WorkflowException {
    @SuppressWarnings("unchecked")
    private void handleGlobal(Namespace ns, Element global, Configuration configDefault, Element eActionConf)
            throws WorkflowException {
 
        // Use the action's namespace when getting children of the action (will be different than ns for extension actions)
        // Use the action's namespace when getting children of the action (will
        // be different than ns for extension actions)
         Namespace actionNs = eActionConf.getNamespace();
 
         if (global != null) {
@@ -587,6 +597,7 @@ public class LiteWorkflowAppParser {
                     for(Element actionXml: actionJobXml){
                         if(jobXml.getText().equals(actionXml.getText())){
                             alreadyExists = true;
                            break;
                         }
                     }
 
@@ -598,35 +609,39 @@ public class LiteWorkflowAppParser {
 
                 }
             }

            if (globalConfiguration != null) {
            try {
                XConfiguration actionConf;
                 Element actionConfiguration = eActionConf.getChild("configuration", actionNs);
                 if (actionConfiguration == null) {
                    actionConfiguration = new Element("configuration", actionNs);
                    eActionConf.addContent(actionConfiguration);
                    actionConf = new XConfiguration();
                 }
                for (Element globalConfig : (List<Element>) globalConfiguration.getChildren()) {
                    boolean isSet = false;
                    String globalVarName = globalConfig.getChildText("name", ns);
                    for (Element local : (List<Element>) actionConfiguration.getChildren()) {
                        if (local.getChildText("name", actionNs).equals(globalVarName)) {
                            isSet = true;
                        }
                    }
                    if (!isSet) {
                        Element varToCopy = new Element("property", actionNs);
                        Element varName = new Element("name", actionNs);
                        Element varValue = new Element("value", actionNs);

                        varName.setText(globalConfig.getChildText("name", ns));
                        varValue.setText(globalConfig.getChildText("value", ns));

                        varToCopy.addContent(varName);
                        varToCopy.addContent(varValue);

                        actionConfiguration.addContent(varToCopy);
                    }
                else {
                    actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(actionConfiguration)
                            .toString()));
                }
                if (globalConfiguration != null) {
                    Configuration globalConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(
                            globalConfiguration).toString()));
                    XConfiguration.injectDefaults(globalConf, actionConf);
                 }
                XConfiguration.injectDefaults(configDefault, actionConf);
                int position = eActionConf.indexOf(actionConfiguration);
                eActionConf.removeContent(actionConfiguration); //replace with enhanced one
                Element eConfXml = XmlUtils.parseXml(actionConf.toXmlString(false));
                eConfXml.detach();
                eConfXml.setNamespace(actionNs);
                if (position > 0) {
                    eActionConf.addContent(position, eConfXml);
                }
                else {
                    eActionConf.addContent(eConfXml);
                }
            }
            catch (IOException e) {
                throw new WorkflowException(ErrorCode.E0700, "Error while processing action conf");
            }
            catch (JDOMException e) {
                throw new WorkflowException(ErrorCode.E0700, "Error while processing action conf");
             }
         }
         else {
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java
index 12f7ed067..7f6f1ccab 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowLib.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -49,10 +49,10 @@ public abstract class LiteWorkflowLib implements WorkflowLib {
     }
 
     @Override
    public WorkflowApp parseDef(String appXml, Configuration jobConf) throws WorkflowException {
    public WorkflowApp parseDef(String appXml, Configuration jobConf, Configuration configDefault) throws WorkflowException {
         ParamChecker.notEmpty(appXml, "appXml");
         return new LiteWorkflowAppParser(schema, controlHandlerClass, decisionHandlerClass, actionHandlerClass)
                .validateAndParse(new StringReader(appXml), jobConf);
                .validateAndParse(new StringReader(appXml), jobConf, configDefault);
     }
 
     @Override
diff --git a/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java b/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
index a250c8cd3..8f3464c6e 100644
-- a/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
++ b/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
@@ -84,15 +84,16 @@ public class TestLiteWorkflowAppParser extends XTestCase {
              "  <name-node>bar</name-node>\r\n" +
              "  <configuration>\r\n" +
              "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
              "      <name>b</name>\r\n" +
              "      <value>B</value>\r\n" +
              "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A</value>\r\n" +
             "    </property>\r\n" +
              "  </configuration>\r\n" +
              "</map-reduce>";
        d = d.replaceAll(" xmlns=?(\"|\')(\"|\')", "");
         assertEquals(expectedD.replaceAll(" ",""), d.replaceAll(" ", ""));
 
     }
@@ -126,15 +127,16 @@ public class TestLiteWorkflowAppParser extends XTestCase {
              "  <job-xml>/spam2</job-xml>\r\n" +
              "  <configuration>\r\n" +
              "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
              "      <name>b</name>\r\n" +
              "      <value>B</value>\r\n" +
              "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A</value>\r\n" +
             "    </property>\r\n" +
              "  </configuration>\r\n" +
              "</map-reduce>";
        d = d.replaceAll(" xmlns=?(\"|\')(\"|\')", "");
         assertEquals(expectedD.replaceAll(" ",""), d.replaceAll(" ", ""));
 
     }
@@ -157,13 +159,13 @@ public class TestLiteWorkflowAppParser extends XTestCase {
                 "  </prepare>\r\n" +
                 "  <configuration>\r\n" +
                 "    <property>\r\n" +
                "      <name>a</name>\r\n" +
                "      <value>A2</value>\r\n" +
                "    </property>\r\n" +
                "    <property>\r\n" +
                 "      <name>b</name>\r\n" +
                 "      <value>B</value>\r\n" +
                 "    </property>\r\n" +
                "    <property>\r\n" +
                "      <name>a</name>\r\n" +
                "      <value>A2</value>\r\n" +
                "    </property>\r\n" +
                 "  </configuration>\r\n" +
                 "  <script>/tmp</script>\r\n" +
                 "  <param>x</param>\r\n" +
@@ -172,6 +174,7 @@ public class TestLiteWorkflowAppParser extends XTestCase {
                 "  <job-tracker>foo</job-tracker>\r\n" +
                 "  <name-node>bar</name-node>\r\n" +
                 "</pig>";
        e = e.replaceAll(" xmlns=?(\"|\')(\"|\')", "");
         assertEquals(expectedE.replaceAll(" ", ""), e.replaceAll(" ", ""));
 
     }
@@ -194,16 +197,16 @@ public class TestLiteWorkflowAppParser extends XTestCase {
              "  </prepare>\r\n" +
              "  <configuration>\r\n" +
              "    <property>\r\n" +
             "      <name>c</name>\r\n" +
             "      <value>C</value>\r\n" +
             "      <name>b</name>\r\n" +
             "      <value>B</value>\r\n" +
              "    </property>\r\n" +
              "    <property>\r\n" +
              "      <name>a</name>\r\n" +
              "      <value>A</value>\r\n" +
              "    </property>\r\n" +
              "    <property>\r\n" +
             "      <name>b</name>\r\n" +
             "      <value>B</value>\r\n" +
             "      <name>c</name>\r\n" +
             "      <value>C</value>\r\n" +
              "    </property>\r\n" +
              "  </configuration>\r\n" +
              "  <script>script.q</script>\r\n" +
@@ -212,7 +215,7 @@ public class TestLiteWorkflowAppParser extends XTestCase {
              "  <job-tracker>foo</job-tracker>\r\n" +
              "  <name-node>bar</name-node>\r\n" +
              "</hive>";
        System.out.println("AAA " + expectedA.replaceAll(" ", ""));
        a = a.replaceAll(" xmlns=?(\"|\')(\"|\')", "");
         assertEquals(expectedA.replaceAll(" ",""), a.replaceAll(" ", ""));
     }
 
@@ -236,17 +239,18 @@ public class TestLiteWorkflowAppParser extends XTestCase {
              "  </prepare>\r\n" +
              "  <configuration>\r\n" +
              "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A2</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
              "      <name>b</name>\r\n" +
              "      <value>B</value>\r\n" +
              "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A2</value>\r\n" +
             "    </property>\r\n" +
              "  </configuration>\r\n" +
              "  <arg>/tmp/data.txt</arg>\r\n" +
              "  <arg>/tmp2/data.txt</arg>\r\n" +
              "</distcp>";
        b = b.replaceAll(" xmlns=?(\"|\')(\"|\')", "");
         assertEquals(expectedB.replaceAll(" ",""), b.replaceAll(" ", ""));
     }
 
@@ -792,7 +796,7 @@ public class TestLiteWorkflowAppParser extends XTestCase {
             assertTrue(we.getMessage().contains("three"));
         }
     }
    

     /*
      *f->(2,3)
      *2->decision node->{4,end}
diff --git a/docs/src/site/twiki/WorkflowFunctionalSpec.twiki b/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
index da4cc5455..77084969f 100644
-- a/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
++ b/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
@@ -560,11 +560,14 @@ configurations.
 The =map-reduce= action has to be configured with all the necessary Hadoop JobConf properties to run the Hadoop
 map/reduce job.
 
Hadoop JobConf properties can be specified in a JobConf XML file bundled with the workflow application or they can be
indicated inline in the =map-reduce= action configuration.
Hadoop JobConf properties can be specified as part of 
   * the =config-default.xml= or
   * JobConf XML file bundled with the workflow application or
   * <global> tag in workflow definition or
   * Inline =map-reduce= action configuration. 
 
The configuration properties are loaded in the following order, =streaming=, =job-xml= and =configuration=, and later
values override earlier values.
The configuration properties are loaded in the following above order i.e. =streaming=, =job-xml= and =configuration=, and 
the precedence order is later values override earlier values.
 
 Streaming and inline property values can be parameterized (templatized) using EL expressions.
 
@@ -829,11 +832,14 @@ A =pig= action can be configured to perform HDFS files/directories cleanup or HC
 starting the Pig job. This capability enables Oozie to retry a Pig job in the situation of a transient failure (Pig
 creates temporary directories for intermediate data, thus a retry without cleanup would fail).
 
Hadoop JobConf properties can be specified in a JobConf XML file bundled with the workflow application or they can be
indicated inline in the =pig= action configuration.
Hadoop JobConf properties can be specified as part of 
   * the =config-default.xml= or
   * JobConf XML file bundled with the workflow application or
   * <global> tag in workflow definition or
   * Inline =pig= action configuration. 
 
The configuration properties are loaded in the following order, =job-xml= and =configuration=, and later
values override earlier values.
The configuration properties are loaded in the following above order i.e. =job-xml= and =configuration=, and 
the precedence order is later values override earlier values.
 
 Inline property values can be parameterized (templatized) using EL expressions.
 
@@ -1496,9 +1502,9 @@ used within the =transition= elements of a node.
 When a workflow job is submitted to Oozie, the submitter may specify as many workflow job properties as required
 (similar to Hadoop JobConf properties).
 
Workflow applications may define default values for the workflow job parameters. They must be defined in a
Workflow applications may define default values for the workflow job or action parameters. They must be defined in a
  =config-default.xml= file bundled with the workflow application archive (refer to section '7 Workflow
 Applications Packaging'). Workflow job properties have precedence over the default values.
 Applications Packaging'). Job or action properties specified in the workflow definition have precedence over the default values.
 
 Properties that are a valid Java identifier, <code>[A-Za-z_][0-9A-Za-z_]*</code>, are available as '${NAME}'
 variables within the workflow definition.
@@ -2182,11 +2188,11 @@ specify an uber jar is governed by the =oozie.action.mapreduce.uber.jar.enable=
 </action>
 </verbatim>
 
The =config-default.xml= file defines, if any, default values for the workflow job parameters. This file must be in
The =config-default.xml= file defines, if any, default values for the workflow job or action parameters. This file must be in
 the Hadoop Configuration XML format. EL expressions are not supported and =user.name= property cannot be specified in
 this file.
 
Any other resources like =job.xml= files referenced from a workflow action action node must be included under the
Any other resources like =job.xml= files referenced from a workflow action node must be included under the
 corresponding path, relative paths always start from the root of the workflow application.
 
 ---++ 8 External Data Assumptions
diff --git a/release-log.txt b/release-log.txt
index 518eee4dd..dee1cba04 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1644 Default config from config-default.xml is not propagated to actions (mona)
 OOZIE-1645 Oozie upgrade DB command fails due to missing dependencies for mssql (omaliuvanchuk via rkanter)
 OOZIE-1668 Coord log streaming start and end time should be of action list start and end time (puru via rohini)
 OOZIE-1674 DB upgrade from 3.3.0 to trunk fails on postgres (rkanter)
diff --git a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
index 00f0261a8..7e60289de 100644
-- a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
++ b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
@@ -28,8 +28,12 @@ import org.apache.hadoop.mapred.JobID;
 import org.apache.hadoop.streaming.StreamJob;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.service.URIHandlerService;
import org.apache.oozie.command.wf.StartXCommand;
import org.apache.oozie.command.wf.SubmitXCommand;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
 import org.apache.oozie.service.WorkflowAppService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.HadoopAccessorService;
@@ -40,6 +44,7 @@ import org.apache.oozie.util.ClassUtils;
 import org.jdom.Element;
 
 import java.io.File;
import java.io.FileWriter;
 import java.io.OutputStream;
 import java.io.InputStream;
 import java.io.FileInputStream;
@@ -48,9 +53,7 @@ import java.io.Writer;
 import java.io.OutputStreamWriter;
 import java.io.StringReader;
 import java.net.URI;
import java.util.ArrayList;
 import java.util.Arrays;
import java.util.List;
 import java.util.Map;
 import java.util.Scanner;
 import java.util.jar.JarOutputStream;
@@ -76,6 +79,82 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
                 + "</configuration>" + "</map-reduce>");
     }
 
    public void testConfigDefaultPropsToAction() throws Exception {
        String wfXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.5\" name=\"map-reduce-wf\">"
        + "<global>"
        + "<job-tracker>${jobTracker}</job-tracker>"
        + "<name-node>${nameNode}</name-node>"
        + "<configuration><property><name>aa</name><value>AA</value></property></configuration>"
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
        + "    <ok to=\"end\"/>"
        + "    <error to=\"fail\"/>"
        + "</action>"
        + "<kill name=\"fail\">"
        + "    <message>Map/Reduce failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>"
        + "</kill>"
        + "<end name=\"end\"/>"
        + "</workflow-app>";

        Writer writer = new FileWriter(getTestCaseDir() + "/workflow.xml");
        IOUtils.copyCharStream(new StringReader(wfXml), writer);

        Configuration conf = new XConfiguration();
        conf.set("nameNode", getNameNodeUri());
        conf.set("jobTracker", getJobTrackerUri());
        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set(OozieClient.APP_PATH, "file://" + getTestCaseDir() + File.separator + "workflow.xml");
        conf.set(OozieClient.LOG_TOKEN, "t");

        OutputStream os = new FileOutputStream(getTestCaseDir() + "/config-default.xml");
        XConfiguration defaultConf = new XConfiguration();
        defaultConf.set("outputDir", "output-data-dir");
        defaultConf.set("mapred.mapper.class", "MM");
        defaultConf.set("mapred.reducer.class", "RR");
        defaultConf.set("cc", "from_default");
        defaultConf.writeXml(os);
        os.close();

        String wfId = new SubmitXCommand(conf).call();
        new StartXCommand(wfId).call();
        sleep(3000);

        WorkflowActionBean mrAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION,
                wfId + "@mr-node");

        // check NN and JT settings
        Element eConf = XmlUtils.parseXml(mrAction.getConf());
        eConf = eConf.getChild("name-node", eConf.getNamespace());
        assertEquals(getNameNodeUri(), eConf.getText());
        eConf = XmlUtils.parseXml(mrAction.getConf());
        eConf = eConf.getChild("job-tracker", eConf.getNamespace());
        assertEquals(getJobTrackerUri(), eConf.getText());

        // check other m-r settings
        eConf = XmlUtils.parseXml(mrAction.getConf());
        eConf = eConf.getChild("configuration", eConf.getNamespace());
        Configuration actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(eConf).toString()));
        assertEquals("output-data-dir", actionConf.get("outputDir"));
        assertEquals("MM", actionConf.get("mapred.mapper.class"));
        assertEquals("RR", actionConf.get("mapred.reducer.class"));
        // check that default did not overwrite same property explicit in action conf
        assertEquals("from_action", actionConf.get("cc"));
        // check that original conf and from global was not deleted
        assertEquals("AA", actionConf.get("aa"));
        assertEquals("BB", actionConf.get("bb"));

    }

     @SuppressWarnings("unchecked")
     public void testSetupMethods() throws Exception {
         MapReduceActionExecutor ae = new MapReduceActionExecutor();
- 
2.19.1.windows.1

