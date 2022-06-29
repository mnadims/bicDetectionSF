From b074b2ceec82d081441d7dd3bd7dee8506c67bd6 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Tue, 18 Aug 2015 14:00:46 -0700
Subject: [PATCH] OOZIE-2332 Add ability to provide Hive and Hive 2 Action
 queries inline in workflows (prateekrungta via rkanter)

--
 .../java/org/apache/oozie/cli/OozieCLI.java   |   8 +-
 client/src/main/resources/hive-action-0.6.xsd |  72 +++++++
 .../src/main/resources/hive2-action-0.2.xsd   |  74 +++++++
 .../action/hadoop/Hive2ActionExecutor.java    |  48 +++--
 .../action/hadoop/HiveActionExecutor.java     |  43 ++--
 .../hadoop/ScriptLanguageActionExecutor.java  |  17 +-
 core/src/main/resources/oozie-default.xml     |   4 +-
 .../site/twiki/DG_Hive2ActionExtension.twiki  |  69 ++++++-
 .../site/twiki/DG_HiveActionExtension.twiki   |  64 +++++-
 release-log.txt                               |   1 +
 .../apache/oozie/action/hadoop/HiveMain.java  |  75 ++++---
 .../action/hadoop/TestHiveActionExecutor.java | 161 ++++++++++-----
 .../oozie/action/hadoop/TestHiveMain.java     |   9 +-
 .../apache/oozie/action/hadoop/Hive2Main.java |  73 ++++---
 .../hadoop/TestHive2ActionExecutor.java       | 191 +++++++++++++-----
 15 files changed, 703 insertions(+), 206 deletions(-)
 create mode 100644 client/src/main/resources/hive-action-0.6.xsd
 create mode 100644 client/src/main/resources/hive2-action-0.2.xsd

diff --git a/client/src/main/java/org/apache/oozie/cli/OozieCLI.java b/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
index 48bac7d78..65291ff13 100644
-- a/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
++ b/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
@@ -2081,6 +2081,8 @@ public class OozieCLI {
                         "hive-action-0.4.xsd")));
                 sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                         "hive-action-0.5.xsd")));
                sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "hive-action-0.6.xsd")));
                 sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                         "sqoop-action-0.2.xsd")));
                 sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
@@ -2093,8 +2095,10 @@ public class OozieCLI {
                         "ssh-action-0.2.xsd")));
                 sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                         "hive2-action-0.1.xsd")));
                sources.add(new StreamSource(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("spark-action-0.1.xsd")));
                sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "hive2-action-0.2.xsd")));
                sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "spark-action-0.1.xsd")));
                 SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                 Schema schema = factory.newSchema(sources.toArray(new StreamSource[sources.size()]));
                 Validator validator = schema.newValidator();
diff --git a/client/src/main/resources/hive-action-0.6.xsd b/client/src/main/resources/hive-action-0.6.xsd
new file mode 100644
index 000000000..720c4a2a0
-- /dev/null
++ b/client/src/main/resources/hive-action-0.6.xsd
@@ -0,0 +1,72 @@
<?xml version="1.0" encoding="UTF-8"?>
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
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:hive="uri:oozie:hive-action:0.6" elementFormDefault="qualified"
           targetNamespace="uri:oozie:hive-action:0.6">

    <xs:element name="hive" type="hive:ACTION"/>

    <xs:complexType name="ACTION">
        <xs:sequence>
            <xs:element name="job-tracker" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="name-node" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="prepare" type="hive:PREPARE" minOccurs="0" maxOccurs="1"/>
            <xs:element name="job-xml" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="configuration" type="hive:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
            <xs:choice minOccurs="1" maxOccurs="1">
                <xs:element name="script" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="query" type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:choice>
            <xs:element name="param" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="argument" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="file" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="archive" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="PREPARE">
        <xs:sequence>
            <xs:element name="delete" type="hive:DELETE" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="mkdir" type="hive:MKDIR" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="DELETE">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>

    <xs:complexType name="MKDIR">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>

</xs:schema>
diff --git a/client/src/main/resources/hive2-action-0.2.xsd b/client/src/main/resources/hive2-action-0.2.xsd
new file mode 100644
index 000000000..f5d7a6084
-- /dev/null
++ b/client/src/main/resources/hive2-action-0.2.xsd
@@ -0,0 +1,74 @@
<?xml version="1.0" encoding="UTF-8"?>
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
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:hive2="uri:oozie:hive2-action:0.2" elementFormDefault="qualified"
           targetNamespace="uri:oozie:hive2-action:0.2">

    <xs:element name="hive2" type="hive2:ACTION"/>

    <xs:complexType name="ACTION">
        <xs:sequence>
            <xs:element name="job-tracker" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="name-node" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="prepare" type="hive2:PREPARE" minOccurs="0" maxOccurs="1"/>
            <xs:element name="job-xml" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="configuration" type="hive2:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
            <xs:element name="jdbc-url" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="password" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:choice minOccurs="1" maxOccurs="1">
                <xs:element name="script" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="query" type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:choice>
            <xs:element name="param" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="argument" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="file" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="archive" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="PREPARE">
        <xs:sequence>
            <xs:element name="delete" type="hive2:DELETE" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="mkdir" type="hive2:MKDIR" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="DELETE">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>

    <xs:complexType name="MKDIR">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>

</xs:schema>
\ No newline at end of file
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/Hive2ActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/Hive2ActionExecutor.java
index 704b762cc..b5b1bf908 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/Hive2ActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/Hive2ActionExecutor.java
@@ -21,11 +21,9 @@ package org.apache.oozie.action.hadoop;
 import static org.apache.oozie.action.hadoop.LauncherMapper.CONF_OOZIE_ACTION_MAIN_CLASS;
 
 import java.io.IOException;
import java.io.StringReader;
 import java.net.URISyntaxException;
 import java.util.ArrayList;
 import java.util.List;
import java.util.Properties;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
@@ -44,11 +42,15 @@ public class Hive2ActionExecutor extends ScriptLanguageActionExecutor {
     static final String HIVE2_JDBC_URL = "oozie.hive2.jdbc.url";
     static final String HIVE2_PASSWORD = "oozie.hive2.password";
     static final String HIVE2_SCRIPT = "oozie.hive2.script";
    static final String HIVE2_QUERY = "oozie.hive2.query";
     static final String HIVE2_PARAMS = "oozie.hive2.params";
     static final String HIVE2_ARGS = "oozie.hive2.args";
 
    private boolean addScriptToCache;

     public Hive2ActionExecutor() {
         super("hive2");
        this.addScriptToCache = false;
     }
 
     @Override
@@ -63,6 +65,11 @@ public class Hive2ActionExecutor extends ScriptLanguageActionExecutor {
         return classes;
     }
 
    @Override
    protected boolean shouldAddScriptToCache(){
        return this.addScriptToCache;
    }

     @Override
     protected String getLauncherMain(Configuration launcherConf, Element actionXml) {
         return launcherConf.get(CONF_OOZIE_ACTION_MAIN_CLASS, HIVE2_MAIN_CLASS_NAME);
@@ -76,19 +83,29 @@ public class Hive2ActionExecutor extends ScriptLanguageActionExecutor {
         Namespace ns = actionXml.getNamespace();
 
         String jdbcUrl = actionXml.getChild("jdbc-url", ns).getTextTrim();
        conf.set(HIVE2_JDBC_URL, jdbcUrl);
 
         String password = null;
         Element passwordElement = actionXml.getChild("password", ns);
         if (passwordElement != null) {
             password = actionXml.getChild("password", ns).getTextTrim();
            conf.set(HIVE2_PASSWORD, password);
         }
 
        String script = actionXml.getChild("script", ns).getTextTrim();
        String scriptName = new Path(script).getName();
        String beelineScriptContent = context.getProtoActionConf().get(HIVE2_SCRIPT);

        if (beelineScriptContent == null){
            addToCache(conf, appPath, script + "#" + scriptName, false);
        Element queryElement = actionXml.getChild("query", ns);
        Element scriptElement  = actionXml.getChild("script", ns);
        if(scriptElement != null) {
            String script = scriptElement.getTextTrim();
            String scriptName = new Path(script).getName();
            this.addScriptToCache = true;
            conf.set(HIVE2_SCRIPT, scriptName);
        } else if(queryElement != null) {
            // Unable to use getTextTrim due to https://issues.apache.org/jira/browse/HIVE-8182
            String query = queryElement.getText();
            conf.set(HIVE2_QUERY, query);
        } else {
            throw new ActionExecutorException(ActionExecutorException.ErrorType.ERROR, "INVALID_ARGUMENTS",
                "Hive 2 action requires one of <script> or <query> to be set. Neither were found.");
         }
 
         List<Element> params = (List<Element>) actionXml.getChildren("param", ns);
@@ -96,6 +113,8 @@ public class Hive2ActionExecutor extends ScriptLanguageActionExecutor {
         for (int i = 0; i < params.size(); i++) {
             strParams[i] = params.get(i).getTextTrim();
         }
        MapReduceMain.setStrings(conf, HIVE2_PARAMS, strParams);

         String[] strArgs = null;
         List<Element> eArgs = actionXml.getChildren("argument", ns);
         if (eArgs != null && eArgs.size() > 0) {
@@ -104,22 +123,11 @@ public class Hive2ActionExecutor extends ScriptLanguageActionExecutor {
                 strArgs[i] = eArgs.get(i).getTextTrim();
             }
         }
        MapReduceMain.setStrings(conf, HIVE2_ARGS, strArgs);
 
        setHive2Props(conf, jdbcUrl, password, scriptName, strParams, strArgs);
         return conf;
     }
 
    public static void setHive2Props(Configuration conf, String jdbcUrl, String password, String script, String[] params,
            String[] args) {
        conf.set(HIVE2_JDBC_URL, jdbcUrl);
        if (password != null) {
            conf.set(HIVE2_PASSWORD, password);
        }
        conf.set(HIVE2_SCRIPT, script);
        MapReduceMain.setStrings(conf, HIVE2_PARAMS, params);
        MapReduceMain.setStrings(conf, HIVE2_ARGS, args);
    }

     @Override
     protected boolean getCaptureOutput(WorkflowAction action) throws JDOMException {
         return true;
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/HiveActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/HiveActionExecutor.java
index bc5b0336a..c74e9e61c 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/HiveActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/HiveActionExecutor.java
@@ -21,11 +21,9 @@ package org.apache.oozie.action.hadoop;
 import static org.apache.oozie.action.hadoop.LauncherMapper.CONF_OOZIE_ACTION_MAIN_CLASS;
 
 import java.io.IOException;
import java.io.StringReader;
 import java.net.URISyntaxException;
 import java.util.ArrayList;
 import java.util.List;
import java.util.Properties;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
@@ -33,7 +31,6 @@ import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.RunningJob;
 import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.action.ActionExecutor.Context;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.XOozieClient;
 import org.apache.oozie.service.ConfigurationService;
@@ -46,12 +43,16 @@ import org.jdom.Namespace;
 public class HiveActionExecutor extends ScriptLanguageActionExecutor {
 
     private static final String HIVE_MAIN_CLASS_NAME = "org.apache.oozie.action.hadoop.HiveMain";
    static final String HIVE_QUERY = "oozie.hive.query";
     static final String HIVE_SCRIPT = "oozie.hive.script";
     static final String HIVE_PARAMS = "oozie.hive.params";
     static final String HIVE_ARGS = "oozie.hive.args";
 
    private boolean addScriptToCache;

     public HiveActionExecutor() {
         super("hive");
        this.addScriptToCache = false;
     }
 
     @Override
@@ -66,6 +67,11 @@ public class HiveActionExecutor extends ScriptLanguageActionExecutor {
         return classes;
     }
 
    @Override
    protected boolean shouldAddScriptToCache() {
        return this.addScriptToCache;
    }

     @Override
     protected String getLauncherMain(Configuration launcherConf, Element actionXml) {
         return launcherConf.get(CONF_OOZIE_ACTION_MAIN_CLASS, HIVE_MAIN_CLASS_NAME);
@@ -78,12 +84,20 @@ public class HiveActionExecutor extends ScriptLanguageActionExecutor {
         Configuration conf = super.setupActionConf(actionConf, context, actionXml, appPath);
 
         Namespace ns = actionXml.getNamespace();
        String script = actionXml.getChild("script", ns).getTextTrim();
        String scriptName = new Path(script).getName();
        String hiveScriptContent = context.getProtoActionConf().get(XOozieClient.HIVE_SCRIPT);

        if (hiveScriptContent == null){
            addToCache(conf, appPath, script + "#" + scriptName, false);
        Element scriptElement = actionXml.getChild("script", ns);
        Element queryElement = actionXml.getChild("query", ns);
        if (scriptElement != null){
            String script = scriptElement.getTextTrim();
            String scriptName = new Path(script).getName();
            this.addScriptToCache = true;
            conf.set(HIVE_SCRIPT, scriptName);
        } else if (queryElement != null) {
            // Unable to use getTextTrim due to https://issues.apache.org/jira/browse/HIVE-8182
            String query = queryElement.getText();
            conf.set(HIVE_QUERY, query);
        } else {
            throw new ActionExecutorException(ActionExecutorException.ErrorType.ERROR, "INVALID_ARGUMENTS",
                "Hive action requires one of <script> or <query> to be set. Neither were found.");
         }
 
         List<Element> params = (List<Element>) actionXml.getChildren("param", ns);
@@ -91,6 +105,8 @@ public class HiveActionExecutor extends ScriptLanguageActionExecutor {
         for (int i = 0; i < params.size(); i++) {
             strParams[i] = params.get(i).getTextTrim();
         }
        MapReduceMain.setStrings(conf, HIVE_PARAMS, strParams);

         String[] strArgs = null;
         List<Element> eArgs = actionXml.getChildren("argument", ns);
         if (eArgs != null && eArgs.size() > 0) {
@@ -99,17 +115,10 @@ public class HiveActionExecutor extends ScriptLanguageActionExecutor {
                 strArgs[i] = eArgs.get(i).getTextTrim();
             }
         }

        setHiveScript(conf, scriptName, strParams, strArgs);
        MapReduceMain.setStrings(conf, HIVE_ARGS, strArgs);
         return conf;
     }
 
    public static void setHiveScript(Configuration conf, String script, String[] params, String[] args) {
        conf.set(HIVE_SCRIPT, script);
        MapReduceMain.setStrings(conf, HIVE_PARAMS, params);
        MapReduceMain.setStrings(conf, HIVE_ARGS, args);
    }

     @Override
     protected boolean getCaptureOutput(WorkflowAction action) throws JDOMException {
         return true;
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/ScriptLanguageActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/ScriptLanguageActionExecutor.java
index e16124fac..f2541265c 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/ScriptLanguageActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/ScriptLanguageActionExecutor.java
@@ -42,10 +42,23 @@ public abstract class ScriptLanguageActionExecutor extends JavaActionExecutor {
         return null;
     }
 
    protected boolean shouldAddScriptToCache(){
        return true;
    }

     @Override
     protected Configuration setupLauncherConf(Configuration conf, Element actionXml, Path appPath, Context context)
            throws ActionExecutorException {
        throws ActionExecutorException {
         super.setupLauncherConf(conf, actionXml, appPath, context);
        if(shouldAddScriptToCache()) {
            addScriptToCache(conf, actionXml, appPath, context);
        }
        return conf;

    }

    protected void addScriptToCache(Configuration conf, Element actionXml, Path appPath, Context context)
        throws ActionExecutorException {
         Namespace ns = actionXml.getNamespace();
         String script = actionXml.getChild("script", ns).getTextTrim();
         String name = new Path(script).getName();
@@ -82,8 +95,6 @@ public abstract class ScriptLanguageActionExecutor extends JavaActionExecutor {
         else {
             addToCache(conf, appPath, script + "#" + name, false);
         }

        return conf;
     }
 
     protected abstract String getScriptName();
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 9689ce053..32a1df044 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -1442,12 +1442,12 @@
             oozie-workflow-0.4.5.xsd,oozie-workflow-0.5.xsd,
             shell-action-0.1.xsd,shell-action-0.2.xsd,shell-action-0.3.xsd,
             email-action-0.1.xsd,email-action-0.2.xsd,
            hive-action-0.2.xsd,hive-action-0.3.xsd,hive-action-0.4.xsd,hive-action-0.5.xsd,
            hive-action-0.2.xsd,hive-action-0.3.xsd,hive-action-0.4.xsd,hive-action-0.5.xsd,hive-action-0.6.xsd,
             sqoop-action-0.2.xsd,sqoop-action-0.3.xsd,sqoop-action-0.4.xsd,
             ssh-action-0.1.xsd,ssh-action-0.2.xsd,
             distcp-action-0.1.xsd,distcp-action-0.2.xsd,
             oozie-sla-0.1.xsd,oozie-sla-0.2.xsd,
            hive2-action-0.1.xsd,
            hive2-action-0.1.xsd, hive2-action-0.2.xsd,
             spark-action-0.1.xsd
         </value>
         <description>
diff --git a/docs/src/site/twiki/DG_Hive2ActionExtension.twiki b/docs/src/site/twiki/DG_Hive2ActionExtension.twiki
index 37aff88b7..4cd588eff 100644
-- a/docs/src/site/twiki/DG_Hive2ActionExtension.twiki
++ b/docs/src/site/twiki/DG_Hive2ActionExtension.twiki
@@ -16,8 +16,9 @@ The workflow job will wait until the Hive Server 2 job completes before
 continuing to the next action.
 
 To run the Hive Server 2 job, you have to configure the =hive2= action with the
=job-tracker=, =name-node=, =jdbc-url=, =password=, and Hive =script= elements as
well as the necessary parameters and configuration.
=job-tracker=, =name-node=, =jdbc-url=, =password= elements, and either
Hive's =script= or =query= element, as well as the necessary parameters
and configuration.
 
 A =hive2= action can be configured to create or delete HDFS directories
 before starting the Hive Server 2 job.
@@ -99,6 +100,10 @@ execute. The Hive script can be templatized with variables of the form
 =${VARIABLE}=. The values of these variables can then be specified
 using the =params= element.
 
The =query= element available from uri:oozie:hive2-action:0.2, can be used instead of the =script= element. It allows for embedding
queries within the =worklfow.xml= directly.  Similar to the =script= element, it also allows for the templatization of variables
in the form =${VARIABLE}=.

 The =params= element, if present, contains parameters to be passed to
 the Hive script.
 
@@ -150,6 +155,66 @@ with a Kerberized Hive Server 2.
 
 ---+++ AE.A Appendix A, Hive 2 XML-Schema
 
---++++ Hive 2 Action Schema Version 0.2
<verbatim>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:hive2="uri:oozie:hive2-action:0.2" elementFormDefault="qualified"
           targetNamespace="uri:oozie:hive2-action:0.2">
.
    <xs:element name="hive2" type="hive2:ACTION"/>
.
    <xs:complexType name="ACTION">
        <xs:sequence>
            <xs:element name="job-tracker" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="name-node" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="prepare" type="hive2:PREPARE" minOccurs="0" maxOccurs="1"/>
            <xs:element name="job-xml" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="configuration" type="hive2:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
            <xs:element name="jdbc-url" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="password" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:choice minOccurs="1" maxOccurs="1">
                <xs:element name="script" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="query"  type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:choice>
            <xs:element name="param" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="argument" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="file" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="archive" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
.
    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
.
    <xs:complexType name="PREPARE">
        <xs:sequence>
            <xs:element name="delete" type="hive2:DELETE" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="mkdir" type="hive2:MKDIR" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
.
    <xs:complexType name="DELETE">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>
.
    <xs:complexType name="MKDIR">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>
.
</xs:schema>
</verbatim>

 ---++++ Hive 2 Action Schema Version 0.1
 <verbatim>
 <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
diff --git a/docs/src/site/twiki/DG_HiveActionExtension.twiki b/docs/src/site/twiki/DG_HiveActionExtension.twiki
index c8f50307b..93e122ba2 100644
-- a/docs/src/site/twiki/DG_HiveActionExtension.twiki
++ b/docs/src/site/twiki/DG_HiveActionExtension.twiki
@@ -17,7 +17,7 @@ The workflow job will wait until the Hive job completes before
 continuing to the next action.
 
 To run the Hive job, you have to configure the =hive= action with the
=job-tracker=, =name-node= and Hive =script= elements as
=job-tracker=, =name-node= and Hive =script= (or Hive =query=) elements as
 well as the necessary parameters and configuration.
 
 A =hive= action can be configured to create or delete HDFS directories
@@ -95,6 +95,11 @@ execute. The Hive script can be templatized with variables of the form
 =${VARIABLE}=. The values of these variables can then be specified
 using the =params= element.
 
The =query= element available from uri:oozie:hive-action:0.6, can be used instead of the
=script= element. It allows for embedding queries within the =worklfow.xml= directly.
Similar to the =script= element, it also allows for the templatization of variables in the
form =${VARIABLE}=.

 The =params= element, if present, contains parameters to be passed to
 the Hive script.
 
@@ -149,6 +154,63 @@ property =oozie.hive.log.level=. The default value is =INFO=.
 
 ---+++ AE.A Appendix A, Hive XML-Schema
 
---++++ Hive Action Schema Version 0.6
<verbatim>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:hive="uri:oozie:hive-action:0.6" elementFormDefault="qualified"
           targetNamespace="uri:oozie:hive-action:0.6">
.
    <xs:element name="hive" type="hive:ACTION"/>
.
    <xs:complexType name="ACTION">
        <xs:sequence>
            <xs:element name="job-tracker" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="name-node" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="prepare" type="hive:PREPARE" minOccurs="0" maxOccurs="1"/>
            <xs:element name="job-xml" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="configuration" type="hive:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
            <xs:choice minOccurs="1" maxOccurs="1">
                <xs:element name="script" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="query"  type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:choice>
            <xs:element name="param" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="argument" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="file" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="archive" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
.
    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
.
    <xs:complexType name="PREPARE">
        <xs:sequence>
            <xs:element name="delete" type="hive:DELETE" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="mkdir" type="hive:MKDIR" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
.
    <xs:complexType name="DELETE">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>
.
    <xs:complexType name="MKDIR">
        <xs:attribute name="path" type="xs:string" use="required"/>
    </xs:complexType>
.
</xs:schema>
</verbatim>
 ---++++ Hive Action Schema Version 0.5
 <verbatim>
 <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
diff --git a/release-log.txt b/release-log.txt
index 556e88b3e..eedbce63e 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2332 Add ability to provide Hive and Hive 2 Action queries inline in workflows (prateekrungta via rkanter)
 OOZIE-2329 Make handling yarn restarts configurable (puru)
 OOZIE-2228 Statustransit service doesn't pick bundle with suspend status (puru)
 OOZIE-2325 Shell action fails if user overrides oozie.launcher.mapreduce.map.env (kailongs via puru)
diff --git a/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java b/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java
index 84bdb7900..58c28d328 100644
-- a/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java
++ b/sharelib/hive/src/main/java/org/apache/oozie/action/hadoop/HiveMain.java
@@ -34,6 +34,7 @@ import java.util.Properties;
 import java.util.Set;
 import java.util.regex.Pattern;
 
import org.apache.commons.io.FileUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.hive.cli.CliDriver;
@@ -205,15 +206,6 @@ public class HiveMain extends LauncherMain {
         Configuration hiveConf = setUpHiveSite();
 
         List<String> arguments = new ArrayList<String>();
        String scriptPath = hiveConf.get(HiveActionExecutor.HIVE_SCRIPT);

        if (scriptPath == null) {
            throw new RuntimeException("Action Configuration does not have [" +  HiveActionExecutor.HIVE_SCRIPT + "] property");
        }

        if (!new File(scriptPath).exists()) {
            throw new RuntimeException("Hive script file [" + scriptPath + "] does not exist");
        }
 
         String logFile = setUpHiveLog4J(hiveConf);
         arguments.add("--hiveconf");
@@ -221,24 +213,45 @@ public class HiveMain extends LauncherMain {
         arguments.add("--hiveconf");
         arguments.add("hive.log4j.exec.file=" + new File(HIVE_EXEC_L4J_PROPS).getAbsolutePath());
 
        // print out current directory & its contents
        File localDir = new File("dummy").getAbsoluteFile().getParentFile();
        System.out.println("Current (local) dir = " + localDir.getAbsolutePath());
        System.out.println("------------------------");
        for (String file : localDir.list()) {
            System.out.println("  " + file);
        String scriptPath = hiveConf.get(HiveActionExecutor.HIVE_SCRIPT);
        String query = hiveConf.get(HiveActionExecutor.HIVE_QUERY);
        if (scriptPath != null) {
            if (!new File(scriptPath).exists()) {
                throw new RuntimeException("Hive script file [" + scriptPath + "] does not exist");
            }
            // print out current directory & its contents
            File localDir = new File("dummy").getAbsoluteFile().getParentFile();
            System.out.println("Current (local) dir = " + localDir.getAbsolutePath());
            System.out.println("------------------------");
            for (String file : localDir.list()) {
                System.out.println("  " + file);
            }
            System.out.println("------------------------");
            System.out.println();
            // Prepare the Hive Script
            String script = readStringFromFile(scriptPath);
            System.out.println();
            System.out.println("Script [" + scriptPath + "] content: ");
            System.out.println("------------------------");
            System.out.println(script);
            System.out.println("------------------------");
            System.out.println();
            arguments.add("-f");
            arguments.add(scriptPath);
        } else if (query != null) {
            System.out.println("Query: ");
            System.out.println("------------------------");
            System.out.println(query);
            System.out.println("------------------------");
            System.out.println();
            String filename = createScriptFile(query);
            arguments.add("-f");
            arguments.add(filename);
        } else {
            throw new RuntimeException("Action Configuration does not have ["
                +  HiveActionExecutor.HIVE_SCRIPT + "], or ["
                +  HiveActionExecutor.HIVE_QUERY + "] property");
         }
        System.out.println("------------------------");
        System.out.println();

        // Prepare the Hive Script
        String script = readStringFromFile(scriptPath);
        System.out.println();
        System.out.println("Script [" + scriptPath + "] content: ");
        System.out.println("------------------------");
        System.out.println(script);
        System.out.println("------------------------");
        System.out.println();
 
         // Pass any parameters to Hive via arguments
         String[] params = MapReduceMain.getStrings(hiveConf, HiveActionExecutor.HIVE_PARAMS);
@@ -261,9 +274,6 @@ public class HiveMain extends LauncherMain {
             System.out.println();
         }
 
        arguments.add("-f");
        arguments.add(scriptPath);

         String[] hiveArgs = MapReduceMain.getStrings(hiveConf, HiveActionExecutor.HIVE_ARGS);
         for (String hiveArg : hiveArgs) {
             if (DISALLOWED_HIVE_OPTIONS.contains(hiveArg)) {
@@ -302,6 +312,13 @@ public class HiveMain extends LauncherMain {
         }
     }
 
    private String createScriptFile(String query) throws IOException {
        String filename = "oozie-hive-query-" + System.currentTimeMillis() + ".hql";
        File f = new File(filename);
        FileUtils.writeStringToFile(f, query, "UTF-8");
        return filename;
    }

     private void runHive(String[] args) throws Exception {
         CliDriver.main(args);
     }
diff --git a/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java b/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java
index a11d40961..b966d4b63 100644
-- a/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java
++ b/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java
@@ -39,7 +39,6 @@ import org.apache.hadoop.mapred.JobID;
 import org.apache.hadoop.mapred.RunningJob;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.action.hadoop.ActionExecutorTestCase.Context;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.HadoopAccessorService;
@@ -76,6 +75,7 @@ public class TestHiveActionExecutor extends ActionExecutorTestCase {
     private String getHiveScript(String inputPath, String outputPath) {
         StringBuilder buffer = new StringBuilder(NEW_LINE);
         buffer.append("set -v;").append(NEW_LINE);
        buffer.append("DROP TABLE IF EXISTS test;").append(NEW_LINE);
         buffer.append("CREATE EXTERNAL TABLE test (a INT) STORED AS");
         buffer.append(NEW_LINE).append("TEXTFILE LOCATION '");
         buffer.append(inputPath).append("';").append(NEW_LINE);
@@ -86,7 +86,7 @@ public class TestHiveActionExecutor extends ActionExecutorTestCase {
         return buffer.toString();
     }
 
    private String getActionXml() {
    private String getActionScriptXml() {
         String script = "<hive xmlns=''uri:oozie:hive-action:0.2''>" +
         "<job-tracker>{0}</job-tracker>" +
         "<name-node>{1}</name-node>" +
@@ -117,58 +117,121 @@ public class TestHiveActionExecutor extends ActionExecutorTestCase {
         return MessageFormat.format(script, getJobTrackerUri(), getNameNodeUri());
     }
 
    private String getActionQueryXml(String query) {
        String script = "<hive xmlns=''uri:oozie:hive-action:0.6''>" +
            "<job-tracker>{0}</job-tracker>" +
            "<name-node>{1}</name-node>" +
            "<configuration>" +
            "<property>" +
            "<name>javax.jdo.option.ConnectionURL</name>" +
            "<value>jdbc:derby:" + getTestCaseDir() + "/db;create=true</value>" +
            "</property>" +
            "<property>" +
            "<name>javax.jdo.option.ConnectionDriverName</name>" +
            "<value>org.apache.derby.jdbc.EmbeddedDriver</value>" +
            "</property>" +
            "<property>" +
            "<name>javax.jdo.option.ConnectionUserName</name>" +
            "<value>sa</value>" +
            "</property>" +
            "<property>" +
            "<name>javax.jdo.option.ConnectionPassword</name>" +
            "<value> </value>" +
            "</property>" +
            "<property>" +
            "<name>oozie.hive.log.level</name>" +
            "<value>DEBUG</value>" +
            "</property>" +
            "</configuration>";
        return MessageFormat.format(script, getJobTrackerUri(), getNameNodeUri())
            + "<query>" + query + "</query>" +
            "</hive>";
    }

     public void testHiveAction() throws Exception {
         Path inputDir = new Path(getFsTestCaseDir(), INPUT_DIRNAME);
         Path outputDir = new Path(getFsTestCaseDir(), OUTPUT_DIRNAME);

        String hiveScript = getHiveScript(inputDir.toString(), outputDir.toString());
         FileSystem fs = getFileSystem();
        Path script = new Path(getAppPath(), HIVE_SCRIPT_FILENAME);
        Writer scriptWriter = new OutputStreamWriter(fs.create(script));
        scriptWriter.write(getHiveScript(inputDir.toString(), outputDir.toString()));
        scriptWriter.close();
 
        Writer dataWriter = new OutputStreamWriter(fs.create(new Path(inputDir, DATA_FILENAME)));
        dataWriter.write(SAMPLE_DATA_TEXT);
        dataWriter.close();

        Context context = createContext(getActionXml());
        final RunningJob launcherJob = submitAction(context);
        String launcherId = context.getAction().getExternalId();
        waitFor(200 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                return launcherJob.isComplete();
            }
        });
        assertTrue(launcherJob.isSuccessful());
        Configuration conf = new XConfiguration();
        conf.set("user.name", getTestUser());
        Map<String, String> actionData = LauncherMapperHelper.getActionData(getFileSystem(), context.getActionDir(),
        {
            Path script = new Path(getAppPath(), HIVE_SCRIPT_FILENAME);
            Writer scriptWriter = new OutputStreamWriter(fs.create(script));
            scriptWriter.write(hiveScript);
            scriptWriter.close();
            Writer dataWriter = new OutputStreamWriter(fs.create(new Path(inputDir, DATA_FILENAME)));
            dataWriter.write(SAMPLE_DATA_TEXT);
            dataWriter.close();
            Context context = createContext(getActionScriptXml());
            Namespace ns = Namespace.getNamespace("uri:oozie:hive-action:0.2");
            final RunningJob launcherJob = submitAction(context, ns);
            String launcherId = context.getAction().getExternalId();
            waitFor(200 * 1000, new Predicate() {
                public boolean evaluate() throws Exception {
                    return launcherJob.isComplete();
                }
            });
            assertTrue(launcherJob.isSuccessful());
            Configuration conf = new XConfiguration();
            conf.set("user.name", getTestUser());
            Map<String, String> actionData = LauncherMapperHelper.getActionData(getFileSystem(), context.getActionDir(),
                 conf);
        assertFalse(LauncherMapperHelper.hasIdSwap(actionData));

        HiveActionExecutor ae = new HiveActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
        assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
        assertNotNull(context.getAction().getData());
        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        assertNotNull(context.getAction().getData());
        Properties outputData = new Properties();
        outputData.load(new StringReader(context.getAction().getData()));
        assertTrue(outputData.containsKey(LauncherMain.HADOOP_JOBS));
        assertEquals(outputData.get(LauncherMain.HADOOP_JOBS), context.getExternalChildIDs());

        //while this works in a real cluster, it does not with miniMR
        //assertTrue(outputData.getProperty(LauncherMain.HADOOP_JOBS).trim().length() > 0);
        //assertTrue(!actionData.get(LauncherMapper.ACTION_DATA_EXTERNAL_CHILD_IDS).isEmpty());

        assertTrue(fs.exists(outputDir));
        assertTrue(fs.isDirectory(outputDir));
            assertFalse(LauncherMapperHelper.hasIdSwap(actionData));
            HiveActionExecutor ae = new HiveActionExecutor();
            ae.check(context, context.getAction());
            assertTrue(launcherId.equals(context.getAction().getExternalId()));
            assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
            assertNotNull(context.getAction().getData());
            ae.end(context, context.getAction());
            assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());
            assertNotNull(context.getAction().getData());
            Properties outputData = new Properties();
            outputData.load(new StringReader(context.getAction().getData()));
            assertTrue(outputData.containsKey(LauncherMain.HADOOP_JOBS));
            assertEquals(outputData.get(LauncherMain.HADOOP_JOBS), context.getExternalChildIDs());
            //while this works in a real cluster, it does not with miniMR
            //assertTrue(outputData.getProperty(LauncherMain.HADOOP_JOBS).trim().length() > 0);
            //assertTrue(!actionData.get(LauncherMapper.ACTION_DATA_EXTERNAL_CHILD_IDS).isEmpty());
            assertTrue(fs.exists(outputDir));
            assertTrue(fs.isDirectory(outputDir));
        }
        {
            Context context = createContext(getActionQueryXml(hiveScript));
            Namespace ns = Namespace.getNamespace("uri:oozie:hive-action:0.6");
            final RunningJob launcherJob = submitAction(context, ns);
            String launcherId = context.getAction().getExternalId();
            waitFor(200 * 1000, new Predicate() {
                public boolean evaluate() throws Exception {
                    return launcherJob.isComplete();
                }
            });
            assertTrue(launcherJob.isSuccessful());
            Configuration conf = new XConfiguration();
            conf.set("user.name", getTestUser());
            Map<String, String> actionData = LauncherMapperHelper.getActionData(getFileSystem(), context.getActionDir(),
                conf);
            assertFalse(LauncherMapperHelper.hasIdSwap(actionData));
            HiveActionExecutor ae = new HiveActionExecutor();
            ae.check(context, context.getAction());
            assertTrue(launcherId.equals(context.getAction().getExternalId()));
            assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
            assertNotNull(context.getAction().getData());
            ae.end(context, context.getAction());
            assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());
            assertNotNull(context.getAction().getData());
            Properties outputData = new Properties();
            outputData.load(new StringReader(context.getAction().getData()));
            assertTrue(outputData.containsKey(LauncherMain.HADOOP_JOBS));
            assertEquals(outputData.get(LauncherMain.HADOOP_JOBS), context.getExternalChildIDs());
            //while this works in a real cluster, it does not with miniMR
            //assertTrue(outputData.getProperty(LauncherMain.HADOOP_JOBS).trim().length() > 0);
            //assertTrue(!actionData.get(LauncherMapper.ACTION_DATA_EXTERNAL_CHILD_IDS).isEmpty());
            assertTrue(fs.exists(outputDir));
            assertTrue(fs.isDirectory(outputDir));
        }
     }
 
    private RunningJob submitAction(Context context) throws Exception {
    private RunningJob submitAction(Context context, Namespace ns) throws Exception {
         HiveActionExecutor ae = new HiveActionExecutor();
 
         WorkflowAction action = context.getAction();
@@ -183,7 +246,6 @@ public class TestHiveActionExecutor extends ActionExecutorTestCase {
         assertNotNull(jobTracker);
         assertNotNull(consoleUrl);
         Element e = XmlUtils.parseXml(action.getConf());
        Namespace ns = Namespace.getNamespace("uri:oozie:hive-action:0.2");
         XConfiguration conf =
                 new XConfiguration(new StringReader(XmlUtils.prettyPrint(e.getChild("configuration", ns)).toString()));
         conf.set("mapred.job.tracker", e.getChildTextTrim("job-tracker", ns));
@@ -246,8 +308,9 @@ public class TestHiveActionExecutor extends ActionExecutorTestCase {
         dataWriter.write(SAMPLE_DATA_TEXT);
         dataWriter.close();
 
        Context context = createContext(getActionXml());
        submitAction(context);
        Context context = createContext(getActionScriptXml());
        Namespace ns = Namespace.getNamespace("uri:oozie:hive-action:0.2");
        submitAction(context, ns);
         FSDataInputStream os = fs.open(new Path(context.getActionDir(), LauncherMapper.ACTION_CONF_XML));
         XConfiguration conf = new XConfiguration();
         conf.addResource(os);
diff --git a/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveMain.java b/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveMain.java
index d06a62bc9..d72e298e0 100644
-- a/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveMain.java
++ b/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveMain.java
@@ -50,6 +50,7 @@ public class TestHiveMain extends MainTestCase {
     private String getHiveScript(String inputPath, String outputPath) {
         StringBuilder buffer = new StringBuilder(NEW_LINE);
         buffer.append("set -v;").append(NEW_LINE);
        buffer.append("DROP TABLE IF EXISTS test;").append(NEW_LINE);
         buffer.append("CREATE EXTERNAL TABLE test (a INT) STORED AS");
         buffer.append(NEW_LINE).append("TEXTFILE LOCATION '");
         buffer.append(inputPath).append("';").append(NEW_LINE);
@@ -100,8 +101,12 @@ public class TestHiveMain extends MainTestCase {
 
             SharelibUtils.addToDistributedCache("hive", fs, getFsTestCaseDir(), jobConf);
 
            HiveActionExecutor.setHiveScript(jobConf, script.toString(), new String[]{"IN=" + inputDir.toUri().getPath(),
                    "OUT=" + outputDir.toUri().getPath()}, new String[] { "-v" });
            jobConf.set(HiveActionExecutor.HIVE_SCRIPT, script.toString());
            MapReduceMain.setStrings(jobConf, HiveActionExecutor.HIVE_PARAMS, new String[]{
                "IN=" + inputDir.toUri().getPath(),
                "OUT=" + outputDir.toUri().getPath()});
            MapReduceMain.setStrings(jobConf, HiveActionExecutor.HIVE_ARGS,
                new String[]{ "-v" });
 
             File actionXml = new File(getTestCaseDir(), "action.xml");
             OutputStream os = new FileOutputStream(actionXml);
diff --git a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
index 557969e5d..97af28b17 100644
-- a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
++ b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
@@ -30,6 +30,7 @@ import java.util.List;
 import java.util.Set;
 import java.util.regex.Pattern;
 
import org.apache.commons.io.FileUtils;
 import org.apache.commons.io.output.TeeOutputStream;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.Path;
@@ -136,32 +137,44 @@ public class Hive2Main extends LauncherMain {
         arguments.add("org.apache.hive.jdbc.HiveDriver");
 
         String scriptPath = actionConf.get(Hive2ActionExecutor.HIVE2_SCRIPT);
        if (scriptPath == null) {
            throw new RuntimeException("Action Configuration does not have [" +  Hive2ActionExecutor.HIVE2_SCRIPT
                    + "] property");
        }
        if (!new File(scriptPath).exists()) {
            throw new RuntimeException("Hive 2 script file [" + scriptPath + "] does not exist");
        }

        // print out current directory & its contents
        File localDir = new File("dummy").getAbsoluteFile().getParentFile();
        System.out.println("Current (local) dir = " + localDir.getAbsolutePath());
        System.out.println("------------------------");
        for (String file : localDir.list()) {
            System.out.println("  " + file);
        String query = actionConf.get(Hive2ActionExecutor.HIVE2_QUERY);
        if (scriptPath != null) {
            if (!new File(scriptPath).exists()) {
                throw new RuntimeException("Hive 2 script file [" + scriptPath + "] does not exist");
            }
            // print out current directory & its contents
            File localDir = new File("dummy").getAbsoluteFile().getParentFile();
            System.out.println("Current (local) dir = " + localDir.getAbsolutePath());
            System.out.println("------------------------");
            for (String file : localDir.list()) {
                System.out.println("  " + file);
            }
            System.out.println("------------------------");
            System.out.println();
            // Prepare the Hive Script
            String script = readStringFromFile(scriptPath);
            System.out.println();
            System.out.println("Script [" + scriptPath + "] content: ");
            System.out.println("------------------------");
            System.out.println(script);
            System.out.println("------------------------");
            System.out.println();
            arguments.add("-f");
            arguments.add(scriptPath);
        } else if (query != null) {
            System.out.println("Query: ");
            System.out.println("------------------------");
            System.out.println(query);
            System.out.println("------------------------");
            System.out.println();
            String filename = createScriptFile(query);
            arguments.add("-f");
            arguments.add(filename);
        } else {
            throw new RuntimeException("Action Configuration does not have ["
                +  Hive2ActionExecutor.HIVE2_SCRIPT + "], or ["
                +  Hive2ActionExecutor.HIVE2_QUERY + "] property");
         }
        System.out.println("------------------------");
        System.out.println();

        // Prepare the Hive Script
        String script = readStringFromFile(scriptPath);
        System.out.println();
        System.out.println("Script [" + scriptPath + "] content: ");
        System.out.println("------------------------");
        System.out.println(script);
        System.out.println("------------------------");
        System.out.println();
 
         // Pass any parameters to Beeline via arguments
         String[] params = MapReduceMain.getStrings(actionConf, Hive2ActionExecutor.HIVE2_PARAMS);
@@ -184,9 +197,6 @@ public class Hive2Main extends LauncherMain {
             System.out.println();
         }
 
        arguments.add("-f");
        arguments.add(scriptPath);

         // This tells BeeLine to look for a delegation token; otherwise it won't and will fail in secure mode because there are no
         // Kerberos credentials.  In non-secure mode, this argument is ignored so we can simply always pass it.
         arguments.add("-a");
@@ -235,6 +245,13 @@ public class Hive2Main extends LauncherMain {
         }
     }
 
    private String createScriptFile(String query) throws IOException {
        String filename = "oozie-hive2-query-" + System.currentTimeMillis() + ".hql";
        File f = new File(filename);
        FileUtils.writeStringToFile(f, query, "UTF-8");
        return filename;
    }

     private void runBeeline(String[] args, String logFile) throws Exception {
         // We do this instead of calling BeeLine.main so we can duplicate the error stream for harvesting Hadoop child job IDs
         BeeLine beeLine = new BeeLine();
diff --git a/sharelib/hive2/src/test/java/org/apache/oozie/action/hadoop/TestHive2ActionExecutor.java b/sharelib/hive2/src/test/java/org/apache/oozie/action/hadoop/TestHive2ActionExecutor.java
index 16d026757..5963e42fd 100644
-- a/sharelib/hive2/src/test/java/org/apache/oozie/action/hadoop/TestHive2ActionExecutor.java
++ b/sharelib/hive2/src/test/java/org/apache/oozie/action/hadoop/TestHive2ActionExecutor.java
@@ -71,18 +71,60 @@ public class TestHive2ActionExecutor extends ActionExecutorTestCase {
     }
 
     @SuppressWarnings("unchecked")
    public void testSetupMethods() throws Exception {
    public void testSetupMethodsForScript() throws Exception {
         Hive2ActionExecutor ae = new Hive2ActionExecutor();
         List<Class> classes = new ArrayList<Class>();
         classes.add(Hive2Main.class);
         assertEquals(classes, ae.getLauncherClasses());
 
         Element actionXml = XmlUtils.parseXml("<hive2>" +
            "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
            "<name-node>" + getNameNodeUri() + "</name-node>" +
            "<jdbc-url>jdbc:hive2://foo:1234/bar</jdbc-url>" +
            "<password>pass</password>" +
            "<script>script.q</script>" +
            "<param>a=A</param>" +
            "<param>b=B</param>" +
            "<argument>-c</argument>" +
            "<argument>--dee</argument>" +
            "</hive2>");

        XConfiguration protoConf = new XConfiguration();
        protoConf.set(WorkflowAppService.HADOOP_USER, getTestUser());

        WorkflowJobBean wf = createBaseWorkflow(protoConf, "hive2-action");
        WorkflowActionBean action = (WorkflowActionBean) wf.getActions().get(0);
        action.setType(ae.getType());

        Context context = new Context(wf, action);

        Configuration conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        assertEquals("jdbc:hive2://foo:1234/bar", conf.get("oozie.hive2.jdbc.url"));
        assertEquals("pass", conf.get("oozie.hive2.password"));
        assertEquals("script.q", conf.get("oozie.hive2.script"));
        assertEquals("2", conf.get("oozie.hive2.params.size"));
        assertEquals("a=A", conf.get("oozie.hive2.params.0"));
        assertEquals("b=B", conf.get("oozie.hive2.params.1"));
        assertEquals("2", conf.get("oozie.hive2.args.size"));
        assertEquals("-c", conf.get("oozie.hive2.args.0"));
        assertEquals("--dee", conf.get("oozie.hive2.args.1"));
    }

    @SuppressWarnings("unchecked")
    public void testSetupMethodsForQuery() throws Exception {
        Hive2ActionExecutor ae = new Hive2ActionExecutor();
        List<Class> classes = new ArrayList<Class>();
        classes.add(Hive2Main.class);
        assertEquals(classes, ae.getLauncherClasses());

        String sampleQuery = "SELECT count(*) from foobar";
        Element actionXml = XmlUtils.parseXml("<hive2  xmlns=\"uri:oozie:hive2-action:0.2\">" +
                 "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                 "<name-node>" + getNameNodeUri() + "</name-node>" +
                 "<jdbc-url>jdbc:hive2://foo:1234/bar</jdbc-url>" +
                 "<password>pass</password>" +
                "<script>script.q</script>" +
                "<query>" + sampleQuery + "</query>" +
                 "<param>a=A</param>" +
                 "<param>b=B</param>" +
                 "<argument>-c</argument>" +
@@ -102,7 +144,8 @@ public class TestHive2ActionExecutor extends ActionExecutorTestCase {
         ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
         assertEquals("jdbc:hive2://foo:1234/bar", conf.get("oozie.hive2.jdbc.url"));
         assertEquals("pass", conf.get("oozie.hive2.password"));
        assertEquals("script.q", conf.get("oozie.hive2.script"));
        assertEquals(sampleQuery, conf.get("oozie.hive2.query"));
        assertNull(conf.get("oozie.hive2.script"));
         assertEquals("2", conf.get("oozie.hive2.params.size"));
         assertEquals("a=A", conf.get("oozie.hive2.params.0"));
         assertEquals("b=B", conf.get("oozie.hive2.params.1"));
@@ -114,26 +157,39 @@ public class TestHive2ActionExecutor extends ActionExecutorTestCase {
     private String getHive2Script(String inputPath, String outputPath) {
         StringBuilder buffer = new StringBuilder(NEW_LINE);
         buffer.append("set -v;").append(NEW_LINE);
        buffer.append("DROP TABLE IF EXISTS test;").append(NEW_LINE);
         buffer.append("CREATE EXTERNAL TABLE test (a INT) STORED AS");
         buffer.append(NEW_LINE).append("TEXTFILE LOCATION '");
         buffer.append(inputPath).append("';").append(NEW_LINE);
         buffer.append("INSERT OVERWRITE DIRECTORY '");
         buffer.append(outputPath).append("'").append(NEW_LINE);
         buffer.append("SELECT (a-1) FROM test;").append(NEW_LINE);

         return buffer.toString();
     }
 
    private String getActionXml() {
    private String getScriptActionXml() {
         String script = "<hive2 xmlns=''uri:oozie:hive2-action:0.1''>" +
            "<job-tracker>{0}</job-tracker>" +
            "<name-node>{1}</name-node>" +
            "<configuration></configuration>" +
            "<jdbc-url>{2}</jdbc-url>" +
            "<password>dummy</password>" +
            "<script>" + HIVE_SCRIPT_FILENAME + "</script>" +
            "</hive2>";
        return MessageFormat.format(script, getJobTrackerUri(), getNameNodeUri(), getHiveServer2JdbcURL(""));
    }

    private String getQueryActionXml(String query) {
        String script = "<hive2 xmlns=\"uri:oozie:hive2-action:0.2\">" +
         "<job-tracker>{0}</job-tracker>" +
         "<name-node>{1}</name-node>" +
         "<configuration></configuration>" +
         "<jdbc-url>{2}</jdbc-url>" +
        "<password>dummy</password>" +
        "<script>" + HIVE_SCRIPT_FILENAME + "</script>" +
        "</hive2>";
        return MessageFormat.format(script, getJobTrackerUri(), getNameNodeUri(), getHiveServer2JdbcURL(""));
        "<password>dummy</password>";
        String expanded = MessageFormat.format(script, getJobTrackerUri(), getNameNodeUri(), getHiveServer2JdbcURL(""));
        // MessageFormat strips single quotes, which causes issues with the hive query parser
        return expanded +
            "<query>" + query + "</query>" + "</hive2>";
     }
 
     @SuppressWarnings("deprecation")
@@ -141,51 +197,85 @@ public class TestHive2ActionExecutor extends ActionExecutorTestCase {
         setupHiveServer2();
         Path inputDir = new Path(getFsTestCaseDir(), INPUT_DIRNAME);
         Path outputDir = new Path(getFsTestCaseDir(), OUTPUT_DIRNAME);

         FileSystem fs = getFileSystem();
        Path script = new Path(getAppPath(), HIVE_SCRIPT_FILENAME);
        Writer scriptWriter = new OutputStreamWriter(fs.create(script));
        scriptWriter.write(getHive2Script(inputDir.toString(), outputDir.toString()));
        scriptWriter.close();

        Writer dataWriter = new OutputStreamWriter(fs.create(new Path(inputDir, DATA_FILENAME)));
        dataWriter.write(SAMPLE_DATA_TEXT);
        dataWriter.close();

        Context context = createContext(getActionXml());
        final RunningJob launcherJob = submitAction(context);
        String launcherId = context.getAction().getExternalId();
        waitFor(200 * 1000, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                return launcherJob.isComplete();
            }
        });
        assertTrue(launcherJob.isSuccessful());
        Configuration conf = new XConfiguration();
        conf.set("user.name", getTestUser());
        Map<String, String> actionData = LauncherMapperHelper.getActionData(getFileSystem(), context.getActionDir(),
                conf);
        assertFalse(LauncherMapperHelper.hasIdSwap(actionData));
 
        Hive2ActionExecutor ae = new Hive2ActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));
        assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        assertNotNull(context.getAction().getData());
        Properties outputData = new Properties();
        outputData.load(new StringReader(context.getAction().getData()));
        assertTrue(outputData.containsKey(LauncherMain.HADOOP_JOBS));
        assertEquals(outputData.get(LauncherMain.HADOOP_JOBS), context.getExternalChildIDs());

        assertTrue(fs.exists(outputDir));
        assertTrue(fs.isDirectory(outputDir));
        {
            String query = getHive2Script(inputDir.toString(), outputDir.toString());
            Writer dataWriter = new OutputStreamWriter(fs.create(new Path(inputDir, DATA_FILENAME)));
            dataWriter.write(SAMPLE_DATA_TEXT);
            dataWriter.close();
            Context context = createContext(getQueryActionXml(query));
            final RunningJob launcherJob = submitAction(context,
                Namespace.getNamespace("uri:oozie:hive2-action:0.2"));
            String launcherId = context.getAction().getExternalId();
            waitFor(200 * 1000, new Predicate() {
                @Override
                public boolean evaluate() throws Exception {
                    return launcherJob.isComplete();
                }
            });
            assertTrue(launcherJob.isSuccessful());
            Configuration conf = new XConfiguration();
            conf.set("user.name", getTestUser());
            Map<String, String> actionData = LauncherMapperHelper.getActionData(getFileSystem(), context.getActionDir(),
                conf);
            assertFalse(LauncherMapperHelper.hasIdSwap(actionData));
            Hive2ActionExecutor ae = new Hive2ActionExecutor();
            ae.check(context, context.getAction());
            assertTrue(launcherId.equals(context.getAction().getExternalId()));
            assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
            ae.end(context, context.getAction());
            assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());
            assertNotNull(context.getAction().getData());
            Properties outputData = new Properties();
            outputData.load(new StringReader(context.getAction().getData()));
            assertTrue(outputData.containsKey(LauncherMain.HADOOP_JOBS));
            assertEquals(outputData.get(LauncherMain.HADOOP_JOBS), context.getExternalChildIDs());
            assertTrue(fs.exists(outputDir));
            assertTrue(fs.isDirectory(outputDir));
        }
        {
            Path script = new Path(getAppPath(), HIVE_SCRIPT_FILENAME);
            Writer scriptWriter = new OutputStreamWriter(fs.create(script));
            scriptWriter.write(getHive2Script(inputDir.toString(), outputDir.toString()));
            scriptWriter.close();

            Writer dataWriter = new OutputStreamWriter(fs.create(new Path(inputDir, DATA_FILENAME)));
            dataWriter.write(SAMPLE_DATA_TEXT);
            dataWriter.close();
            Context context = createContext(getScriptActionXml());
            final RunningJob launcherJob = submitAction(context,
                Namespace.getNamespace("uri:oozie:hive2-action:0.1"));
            String launcherId = context.getAction().getExternalId();
            waitFor(200 * 1000, new Predicate() {
                @Override
                public boolean evaluate() throws Exception {
                    return launcherJob.isComplete();
                }
            });
            assertTrue(launcherJob.isSuccessful());
            Configuration conf = new XConfiguration();
            conf.set("user.name", getTestUser());
            Map<String, String> actionData = LauncherMapperHelper.getActionData(getFileSystem(), context.getActionDir(),
                conf);
            assertFalse(LauncherMapperHelper.hasIdSwap(actionData));
            Hive2ActionExecutor ae = new Hive2ActionExecutor();
            ae.check(context, context.getAction());
            assertTrue(launcherId.equals(context.getAction().getExternalId()));
            assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
            ae.end(context, context.getAction());
            assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());
            assertNotNull(context.getAction().getData());
            Properties outputData = new Properties();
            outputData.load(new StringReader(context.getAction().getData()));
            assertTrue(outputData.containsKey(LauncherMain.HADOOP_JOBS));
            assertEquals(outputData.get(LauncherMain.HADOOP_JOBS), context.getExternalChildIDs());
            assertTrue(fs.exists(outputDir));
            assertTrue(fs.isDirectory(outputDir));
        }
     }
 
    private RunningJob submitAction(Context context) throws Exception {
    private RunningJob submitAction(Context context, Namespace ns) throws Exception {
         Hive2ActionExecutor ae = new Hive2ActionExecutor();
 
         WorkflowAction action = context.getAction();
@@ -200,7 +290,6 @@ public class TestHive2ActionExecutor extends ActionExecutorTestCase {
         assertNotNull(jobTracker);
         assertNotNull(consoleUrl);
         Element e = XmlUtils.parseXml(action.getConf());
        Namespace ns = Namespace.getNamespace("uri:oozie:hive2-action:0.1");
         XConfiguration conf =
                 new XConfiguration(new StringReader(XmlUtils.prettyPrint(e.getChild("configuration", ns)).toString()));
         conf.set("mapred.job.tracker", e.getChildTextTrim("job-tracker", ns));
- 
2.19.1.windows.1

