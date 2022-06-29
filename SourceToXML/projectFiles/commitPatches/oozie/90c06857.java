From 90c0685746f6115db9accdc05d80a099e116e873 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Tue, 16 Sep 2014 11:07:30 -0700
Subject: [PATCH] OOZIE-1954 Add a way for the MapReduce action to be
 configured by Java code (rkanter)

--
 .../src/main/resources/oozie-workflow-0.5.xsd |   1 +
 .../action/hadoop/JavaActionExecutor.java     |   4 +
 .../action/hadoop/MapperReducerForTest.java   |  25 +++++
 .../site/twiki/WorkflowFunctionalSpec.twiki   | 104 ++++++++++++++++--
 .../job-with-config-class.properties          |  25 +++++
 .../src/main/apps/map-reduce/job.properties   |   2 +-
 .../map-reduce/workflow-with-config-class.xml |  52 +++++++++
 .../SampleOozieActionConfigurator.java        |  50 +++++++++
 release-log.txt                               |   1 +
 .../oozie/action/hadoop/LauncherMain.java     |  25 +++++
 .../oozie/action/hadoop/LauncherMapper.java   |   5 +
 .../oozie/action/hadoop/MapReduceMain.java    |  19 ++--
 .../hadoop/OozieActionConfigurator.java       |  37 +++++++
 .../OozieActionConfiguratorException.java     |  41 +++++++
 .../apache/oozie/action/hadoop/PipesMain.java |  18 ++-
 .../OozieActionConfiguratorForTest.java       |  34 ++++++
 .../hadoop/TestMapReduceActionExecutor.java   | 100 +++++++++++++++++
 17 files changed, 516 insertions(+), 27 deletions(-)
 create mode 100644 examples/src/main/apps/map-reduce/job-with-config-class.properties
 create mode 100644 examples/src/main/apps/map-reduce/workflow-with-config-class.xml
 create mode 100644 examples/src/main/java/org/apache/oozie/example/SampleOozieActionConfigurator.java
 create mode 100644 sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfigurator.java
 create mode 100644 sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorException.java
 create mode 100644 sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorForTest.java

diff --git a/client/src/main/resources/oozie-workflow-0.5.xsd b/client/src/main/resources/oozie-workflow-0.5.xsd
index 6620a4e39..b01580c24 100644
-- a/client/src/main/resources/oozie-workflow-0.5.xsd
++ b/client/src/main/resources/oozie-workflow-0.5.xsd
@@ -173,6 +173,7 @@
             </xs:choice>
             <xs:element name="job-xml" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
             <xs:element name="configuration" type="workflow:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
            <xs:element name="config-class" type="xs:string" minOccurs="0" maxOccurs="1"/>
             <xs:element name="file" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
             <xs:element name="archive" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
         </xs:sequence>
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index 53f979c63..201cfa319 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -253,6 +253,10 @@ public class JavaActionExecutor extends ActionExecutor {
                 injectLauncherUseUberMode(launcherConf);
                 XConfiguration.copy(launcherConf, conf);
             }
            e = actionXml.getChild("config-class", actionXml.getNamespace());
            if (e != null) {
                conf.set(LauncherMapper.OOZIE_ACTION_CONFIG_CLASS, e.getTextTrim());
            }
             return conf;
         }
         catch (IOException ex) {
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerForTest.java b/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerForTest.java
index d89990c56..8f08ddd9f 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerForTest.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerForTest.java
@@ -18,6 +18,9 @@
 
 package org.apache.oozie.action.hadoop;
 
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.mapred.Mapper;
 import org.apache.hadoop.mapred.OutputCollector;
 import org.apache.hadoop.mapred.Reporter;
@@ -30,12 +33,34 @@ import java.util.Iterator;
 public class MapperReducerForTest implements Mapper, Reducer {
     public static final String GROUP = "g";
     public static final String NAME = "c";
    /**
     * If specified in the job conf, the mapper will write out the job.xml file here.
     */
    public static final String JOB_XML_OUTPUT_LOCATION = "oozie.job.xml.output.location";
 
     public static void main(String[] args) {
         System.out.println("hello!");
     }
 
    @Override
     public void configure(JobConf jobConf) {
        try {
            String loc = jobConf.get(JOB_XML_OUTPUT_LOCATION);
            if (loc != null) {
                Path p = new Path(loc);
                FileSystem fs = p.getFileSystem(jobConf);
                if (!fs.exists(p)) {
                    FSDataOutputStream out = fs.create(p);
                    try {
                        jobConf.writeXml(out);
                    } finally {
                        out.close();
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
     }
 
     public void close() throws IOException {
diff --git a/docs/src/site/twiki/WorkflowFunctionalSpec.twiki b/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
index 3319bcc43..43fddbba8 100644
-- a/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
++ b/docs/src/site/twiki/WorkflowFunctionalSpec.twiki
@@ -568,10 +568,11 @@ Hadoop JobConf properties can be specified as part of
    * the =config-default.xml= or
    * JobConf XML file bundled with the workflow application or
    * <global> tag in workflow definition or
   * Inline =map-reduce= action configuration. 
   * Inline =map-reduce= action configuration or
   * An implementation of OozieActionConfigurator specified by the <config-class> tag in workflow definition.
 
The configuration properties are loaded in the following above order i.e. =streaming=, =job-xml= and =configuration=, and 
the precedence order is later values override earlier values.
The configuration properties are loaded in the following above order i.e. =streaming=, =job-xml=, =configuration=,
and =config-class=, and the precedence order is later values override earlier values.
 
 Streaming and inline property values can be parameterized (templatized) using EL expressions.
 
@@ -579,7 +580,7 @@ The Hadoop =mapred.job.tracker= and =fs.default.name= properties must not be pre
 configuration.
 
 #FilesAchives
---++++ 3.2.2.1 Adding Files and Archives for the Job
---+++++ 3.2.2.1 Adding Files and Archives for the Job
 
 The =file=, =archive= elements make available, to map-reduce jobs, files and archives. If the specified path is
 relative, it is assumed the file or archiver are within the application directory, in the corresponding sub-path.
@@ -595,8 +596,88 @@ To force a symlink for a file on the task running directory, use a '#' followed
 
 Refer to Hadoop distributed cache documentation for details more details on files and archives.
 
---+++++ 3.2.2.2 Configuring the MapReduce action with Java code

Java code can be used to further configure the MapReduce action.  This can be useful if you already have "driver" code for your
MapReduce action, if you're more familiar with MapReduce's Java API, if there's some configuration that requires logic, or some
configuration that's difficult to do in straight XML (e.g. Avro).

Create a class that implements the org.apache.oozie.action.hadoop.OozieActionConfigurator interface from the "oozie-sharelib-oozie"
artifact.  It contains a single method that recieves a =JobConf= as an argument.  Any configuration properties set on this =JobConf=
will be used by the MapReduce action.

The OozieActionConfigurator has this signature:
<verbatim>
public interface OozieActionConfigurator {
    public void configure(JobConf actionConf) throws OozieActionConfiguratorException;
}
</verbatim>
where =actionConf= is the =JobConf= you can update.  If you need to throw an Exception, you can wrap it in
an =OozieActionConfiguratorException=, also in the "oozie-sharelib-oozie" artifact.

For example:
<verbatim>
package com.example;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.oozie.action.hadoop.OozieActionConfigurator;
import org.apache.oozie.action.hadoop.OozieActionConfiguratorException;
import org.apache.oozie.example.SampleMapper;
import org.apache.oozie.example.SampleReducer;

public class MyConfigClass implements OozieActionConfigurator {

    @Override
    public void configure(JobConf actionConf) throws OozieActionConfiguratorException {
        if (actionConf.getUser() == null) {
            throw new OozieActionConfiguratorException("No user set");
        }
        actionConf.setMapperClass(SampleMapper.class);
        actionConf.setReducerClass(SampleReducer.class);
        FileInputFormat.setInputPaths(actionConf, new Path("/user/" + actionConf.getUser() + "/input-data"));
        FileOutputFormat.setOutputPath(actionConf, new Path("/user/" + actionConf.getUser() + "/output"));
        ...
    }
}
</verbatim>

To use your config class in your MapReduce action, simply compile it into a jar, make the jar available to your action, and specify
the class name in the =config-class= element (this requires at least schema 0.5):
<verbatim>
<workflow-app name="[WF-DEF-NAME]" xmlns="uri:oozie:workflow:0.5">
    ...
    <action name="[NODE-NAME]">
        <map-reduce>
            ...
            <job-xml>[JOB-XML-FILE]</job-xml>
            <configuration>
                <property>
                    <name>[PROPERTY-NAME]</name>
                    <value>[PROPERTY-VALUE]</value>
                </property>
                ...
            </configuration>
            <config-class>com.example.MyConfigClass</config-class>
            ...
        </map-reduce>
        <ok to="[NODE-NAME]"/>
        <error to="[NODE-NAME]"/>
    </action>
    ...
</workflow-app>
</verbatim>

Another example of this can be found in the "map-reduce" example that comes with Oozie.

A useful tip: The initial =JobConf= passed to the =configure= method includes all of the properties listed in the =configuration=
section of the MR action in a workflow.  If you need to pass any information to your OozieActionConfigurator, you can simply put
them here.

 #StreamingMapReduceAction
---+++++ 3.2.2.2 Streaming
---+++++ 3.2.2.3 Streaming
 
 Streaming information can be specified in the =streaming= element.
 
@@ -613,7 +694,7 @@ The Mapper/Reducer can be overridden by a =mapred.mapper.class= or =mapred.reduc
 file or =configuration= elements.
 
 #PipesMapReduceAction
---+++++ 3.2.2.3 Pipes
---+++++ 3.2.2.4 Pipes
 
 Pipes information can be specified in the =pipes= element.
 
@@ -629,10 +710,10 @@ the =file= and =archive= elements described in the previous section.
 
 Pipe properties can be overridden by specifying them in the =job-xml= file or =configuration= element.
 
---+++++ 3.2.2.4 Syntax
---+++++ 3.2.2.5 Syntax
 
 <verbatim>
<workflow-app name="[WF-DEF-NAME]" xmlns="uri:oozie:workflow:0.1">
<workflow-app name="[WF-DEF-NAME]" xmlns="uri:oozie:workflow:0.5">
     ...
     <action name="[NODE-NAME]">
         <map-reduce>
@@ -670,6 +751,7 @@ Pipe properties can be overridden by specifying them in the =job-xml= file or =c
                 </property>
                 ...
             </configuration>
            <config-class>com.example.MyConfigClass</config-class>
             <file>[FILE-PATH]</file>
             ...
             <archive>[FILE-PATH]</archive>
@@ -700,6 +782,11 @@ The =configuration= element, if present, contains JobConf properties for the Had
 Properties specified in the =configuration= element override properties specified in the file specified in the
  =job-xml= element.
 
As of schema 0.5, the =config-class= element, if present, contains a class that implements OozieActionConfigurator that can be used
to further configure the MapReduce job.

Properties specified in the =config-class= class override properties specified in =configuration= element.

 External Stats can be turned on/off by specifying the property _oozie.action.external.stats.write_ as _true_ or _false_ in the configuration element of workflow.xml. The default value for this property is _false_.
 
 The =file= element, if present, must specify the target sybolic link for binaries by separating the original file and target with a # (file#target-sym-link). This is not required for libraries.
@@ -2639,6 +2726,7 @@ to be executed.
             </xs:choice>
             <xs:element name="job-xml" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
             <xs:element name="configuration" type="workflow:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
            <xs:element name="config-class" type="xs:string" minOccurs="0" maxOccurs="1"/>
             <xs:element name="file" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
             <xs:element name="archive" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
         </xs:sequence>
diff --git a/examples/src/main/apps/map-reduce/job-with-config-class.properties b/examples/src/main/apps/map-reduce/job-with-config-class.properties
new file mode 100644
index 000000000..0b14cb749
-- /dev/null
++ b/examples/src/main/apps/map-reduce/job-with-config-class.properties
@@ -0,0 +1,25 @@
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

nameNode=hdfs://localhost:8020
jobTracker=localhost:8021
queueName=default
examplesRoot=examples

oozie.wf.application.path=${nameNode}/user/${user.name}/${examplesRoot}/apps/map-reduce/workflow-with-config-class.xml
outputDir=map-reduce
diff --git a/examples/src/main/apps/map-reduce/job.properties b/examples/src/main/apps/map-reduce/job.properties
index 7b7a24c95..7115229b6 100644
-- a/examples/src/main/apps/map-reduce/job.properties
++ b/examples/src/main/apps/map-reduce/job.properties
@@ -21,5 +21,5 @@ jobTracker=localhost:8021
 queueName=default
 examplesRoot=examples
 
oozie.wf.application.path=${nameNode}/user/${user.name}/${examplesRoot}/apps/map-reduce
oozie.wf.application.path=${nameNode}/user/${user.name}/${examplesRoot}/apps/map-reduce/workflow.xml
 outputDir=map-reduce
diff --git a/examples/src/main/apps/map-reduce/workflow-with-config-class.xml b/examples/src/main/apps/map-reduce/workflow-with-config-class.xml
new file mode 100644
index 000000000..0deab665a
-- /dev/null
++ b/examples/src/main/apps/map-reduce/workflow-with-config-class.xml
@@ -0,0 +1,52 @@
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
<workflow-app xmlns="uri:oozie:workflow:0.5" name="map-reduce-wf">
    <start to="mr-node"/>
    <action name="mr-node">
        <map-reduce>
            <job-tracker>${jobTracker}</job-tracker>
            <name-node>${nameNode}</name-node>
            <prepare>
                <delete path="${nameNode}/user/${wf:user()}/${examplesRoot}/output-data/${outputDir}"/>
            </prepare>
            <!-- most of the <configuration> properties are being set by SampleOozieActionConfigurator -->
            <configuration>
                <property>
                    <name>mapred.job.queue.name</name>
                    <value>${queueName}</value>
                </property>
                <!-- These two are not Hadoop properties, but SampleOozieActionConfigurator can use them -->
                <property>
                    <name>examples.root</name>
                    <value>${examplesRoot}</value>
                </property>
                <property>
                    <name>output.dir.name</name>
                    <value>${outputDir}</value>
                </property>
            </configuration>
            <config-class>org.apache.oozie.example.SampleOozieActionConfigurator</config-class>
        </map-reduce>
        <ok to="end"/>
        <error to="fail"/>
    </action>
    <kill name="fail">
        <message>Map/Reduce failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>
    </kill>
    <end name="end"/>
</workflow-app>
diff --git a/examples/src/main/java/org/apache/oozie/example/SampleOozieActionConfigurator.java b/examples/src/main/java/org/apache/oozie/example/SampleOozieActionConfigurator.java
new file mode 100644
index 000000000..ff38a5469
-- /dev/null
++ b/examples/src/main/java/org/apache/oozie/example/SampleOozieActionConfigurator.java
@@ -0,0 +1,50 @@
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
package org.apache.oozie.example;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.oozie.action.hadoop.OozieActionConfigurator;
import org.apache.oozie.action.hadoop.OozieActionConfiguratorException;

public class SampleOozieActionConfigurator implements OozieActionConfigurator {

    @Override
    public void configure(JobConf actionConf) throws OozieActionConfiguratorException {
        if (actionConf.getUser() == null) {
            throw new OozieActionConfiguratorException("No user set");
        }
        if (actionConf.get("examples.root") == null) {
            throw new OozieActionConfiguratorException("examples.root not set");
        }
        if (actionConf.get("output.dir.name") == null) {
            throw new OozieActionConfiguratorException("output.dir.name not set");
        }

        actionConf.setMapperClass(SampleMapper.class);
        actionConf.setReducerClass(SampleReducer.class);
        actionConf.setNumMapTasks(1);
        FileInputFormat.setInputPaths(actionConf,
                new Path("/user/" + actionConf.getUser() + "/" + actionConf.get("examples.root") + "/input-data/text"));
        FileOutputFormat.setOutputPath(actionConf,
                new Path("/user/" + actionConf.getUser() + "/" + actionConf.get("examples.root") + "/output-data/"
                        + actionConf.get("output.dir.name")));
    }
}
diff --git a/release-log.txt b/release-log.txt
index aad376430..a4bd20bc3 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-1954 Add a way for the MapReduce action to be configured by Java code (rkanter)
 OOZIE-2003 Checkstyle issues (rkanter via shwethags)
 OOZIE-1457 Create a Hive Server 2 action (rkanter)
 OOZIE-1950 Coordinator job info should support timestamp (nominal time) (shwethags)
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java
index ed5b88c0f..8cfefbb5a 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMain.java
@@ -30,6 +30,7 @@ import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import org.apache.hadoop.util.Shell;
 import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.mapred.JobConf;
 
 public abstract class LauncherMain {
 
@@ -121,6 +122,30 @@ public abstract class LauncherMain {
         }
         return path;
     }

    /**
     * Will run the user specified OozieActionConfigurator subclass (if one is provided) to update the action configuration.
     *
     * @param actionConf The action configuration to update
     * @throws OozieActionConfiguratorException
     */
    protected static void runConfigClass(JobConf actionConf) throws OozieActionConfiguratorException {
        String configClass = System.getProperty(LauncherMapper.OOZIE_ACTION_CONFIG_CLASS);
        if (configClass != null) {
            try {
                Class<?> klass = Class.forName(configClass);
                Class<? extends OozieActionConfigurator> actionConfiguratorKlass = klass.asSubclass(OozieActionConfigurator.class);
                OozieActionConfigurator actionConfigurator = actionConfiguratorKlass.newInstance();
                actionConfigurator.configure(actionConf);
            } catch (ClassNotFoundException e) {
                throw new OozieActionConfiguratorException("An Exception occured while instantiating the action config class", e);
            } catch (InstantiationException e) {
                throw new OozieActionConfiguratorException("An Exception occured while instantiating the action config class", e);
            } catch (IllegalAccessException e) {
                throw new OozieActionConfiguratorException("An Exception occured while instantiating the action config class", e);
            }
        }
    }
 }
 
 class LauncherMainException extends Exception {
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
index ada0706f3..4923fe311 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
@@ -58,6 +58,7 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
     static final String CONF_OOZIE_ACTION_MAIN_ARG_COUNT = ACTION_PREFIX + "main.arg.count";
     static final String CONF_OOZIE_ACTION_MAIN_ARG_PREFIX = ACTION_PREFIX + "main.arg.";
     static final String CONF_OOZIE_EXTERNAL_STATS_MAX_SIZE = "oozie.external.stats.max.size";
    static final String OOZIE_ACTION_CONFIG_CLASS = ACTION_PREFIX + "config.class";
     static final String CONF_OOZIE_ACTION_FS_GLOB_MAX = "oozie.action.fs.glob.max";
     static final int GLOB_MAX_DEFAULT = 1000;
 
@@ -434,6 +435,10 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
         System.setProperty(ACTION_PREFIX + ACTION_DATA_OUTPUT_PROPS, new File(ACTION_DATA_OUTPUT_PROPS).getAbsolutePath());
         System.setProperty(ACTION_PREFIX + ACTION_DATA_ERROR_PROPS, new File(ACTION_DATA_ERROR_PROPS).getAbsolutePath());
         System.setProperty("oozie.job.launch.time", getJobConf().get("oozie.job.launch.time"));
        String actionConfigClass = getJobConf().get(OOZIE_ACTION_CONFIG_CLASS);
        if (actionConfigClass != null) {
            System.setProperty(OOZIE_ACTION_CONFIG_CLASS, actionConfigClass);
        }
     }
 
     // Method to execute the prepare actions
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java
index 1a9a19435..61cec7ec8 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/MapReduceMain.java
@@ -47,9 +47,15 @@ public class MapReduceMain extends LauncherMain {
         Configuration actionConf = new Configuration(false);
         actionConf.addResource(new Path("file:///", System.getProperty("oozie.action.conf.xml")));
 
        logMasking("Map-Reduce job configuration:", new HashSet<String>(), actionConf);
        JobConf jobConf = new JobConf();
        addActionConf(jobConf, actionConf);

        // Run a config class if given to update the job conf
        runConfigClass(jobConf);

        logMasking("Map-Reduce job configuration:", new HashSet<String>(), jobConf);
 
        String jobId = LauncherMainHadoopUtils.getYarnJobForMapReduceAction(actionConf);
        String jobId = LauncherMainHadoopUtils.getYarnJobForMapReduceAction(jobConf);
         File idFile = new File(System.getProperty(LauncherMapper.ACTION_PREFIX + LauncherMapper.ACTION_DATA_NEW_ID));
         if (jobId != null) {
             if (!idFile.exists()) {
@@ -64,7 +70,7 @@ public class MapReduceMain extends LauncherMain {
             System.out.println("Submitting Oozie action Map-Reduce job");
             System.out.println();
             // submitting job
            RunningJob runningJob = submitJob(actionConf);
            RunningJob runningJob = submitJob(jobConf);
 
             jobId = runningJob.getID().toString();
             writeJobIdFile(idFile, jobId);
@@ -87,12 +93,9 @@ public class MapReduceMain extends LauncherMain {
         }
     }
 
    protected RunningJob submitJob(Configuration actionConf) throws Exception {
        JobConf jobConf = new JobConf();
        addActionConf(jobConf, actionConf);

    protected RunningJob submitJob(JobConf jobConf) throws Exception {
         // Set for uber jar
        String uberJar = actionConf.get(OOZIE_MAPREDUCE_UBER_JAR);
        String uberJar = jobConf.get(OOZIE_MAPREDUCE_UBER_JAR);
         if (uberJar != null && uberJar.trim().length() > 0) {
             jobConf.setJar(uberJar);
         }
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfigurator.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfigurator.java
new file mode 100644
index 000000000..46ae700bf
-- /dev/null
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfigurator.java
@@ -0,0 +1,37 @@
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

package org.apache.oozie.action.hadoop;

import org.apache.hadoop.mapred.JobConf;

/**
 * Users can implement this interface to provide a class for Oozie to configure the MapReduce action with.  Make sure that the jar
 * with this class is available with the action and then simply specify the class name in the "config-class" field in the MapReduce
 * action XML.
 */
public interface OozieActionConfigurator {
    /**
     * This method should update the passed in configuration with additional changes; it will be used by the action.  If any
     * Exceptions need to be thrown, they should be wrapped in an OozieActionConfiguratorException
     *
     * @param actionConf The action configuration
     * @throws OozieActionConfiguratorException
     */
    public void configure(JobConf actionConf) throws OozieActionConfiguratorException;
}
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorException.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorException.java
new file mode 100644
index 000000000..075b5e4d8
-- /dev/null
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorException.java
@@ -0,0 +1,41 @@
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

package org.apache.oozie.action.hadoop;

/**
 * Thrown by implementations of the OozieActionConfigurator class.
 */
@SuppressWarnings("serial")
public class OozieActionConfiguratorException extends Exception {

    private OozieActionConfiguratorException() {
    }

    public OozieActionConfiguratorException(String message) {
        super(message);
    }

    public OozieActionConfiguratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public OozieActionConfiguratorException(Throwable cause) {
        super(cause);
    }
}
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
index ca32b5faa..0d38040fc 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
@@ -32,34 +32,32 @@ public class PipesMain extends MapReduceMain {
     }
 
     @Override
    protected RunningJob submitJob(Configuration actionConf) throws Exception {
        JobConf jobConf = new JobConf();

        String value = actionConf.get("oozie.pipes.map");
    protected RunningJob submitJob(JobConf jobConf) throws Exception {
        String value = jobConf.get("oozie.pipes.map");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.mapper", true);
             jobConf.set("mapred.mapper.class", value);
         }
        value = actionConf.get("oozie.pipes.reduce");
        value = jobConf.get("oozie.pipes.reduce");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.reducer", true);
             jobConf.set("mapred.reducer.class", value);
         }
        value = actionConf.get("oozie.pipes.inputformat");
        value = jobConf.get("oozie.pipes.inputformat");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.recordreader", true);
             jobConf.set("mapred.input.format.class", value);
         }
        value = actionConf.get("oozie.pipes.partitioner");
        value = jobConf.get("oozie.pipes.partitioner");
         if (value != null) {
             jobConf.set("mapred.partitioner.class", value);
         }
        value = actionConf.get("oozie.pipes.writer");
        value = jobConf.get("oozie.pipes.writer");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.recordwriter", true);
             jobConf.set("mapred.output.format.class", value);
         }
        value = actionConf.get("oozie.pipes.program");
        value = jobConf.get("oozie.pipes.program");
         if (value != null) {
             jobConf.set("hadoop.pipes.executable", value);
             if (value.contains("#")) {
@@ -67,7 +65,7 @@ public class PipesMain extends MapReduceMain {
             }
         }
 
        addActionConf(jobConf, actionConf);
        addActionConf(jobConf, jobConf);
 
         //propagate delegation related props from launcher job to MR job
         if (getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION") != null) {
diff --git a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorForTest.java b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorForTest.java
new file mode 100644
index 000000000..ba23d959d
-- /dev/null
++ b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/OozieActionConfiguratorForTest.java
@@ -0,0 +1,34 @@
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
package org.apache.oozie.action.hadoop;

import org.apache.hadoop.mapred.JobConf;

public class OozieActionConfiguratorForTest implements OozieActionConfigurator {

    @Override
    public void configure(JobConf actionConf) throws OozieActionConfiguratorException {
        if (actionConf.getBoolean("oozie.test.throw.exception", false)) {
            throw new OozieActionConfiguratorException("doh");
        }

        actionConf.set("A", "a");
        actionConf.set("B", "c");
    }

}
diff --git a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
index d43ddcaf0..50927ced6 100644
-- a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
++ b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
@@ -56,6 +56,7 @@ import java.io.StringReader;
 import java.net.URI;
 import java.util.Arrays;
 import java.util.Map;
import java.util.Properties;
 import java.util.Scanner;
 import java.util.jar.JarOutputStream;
 import java.util.regex.Pattern;
@@ -63,6 +64,7 @@ import java.util.zip.ZipEntry;
 
 import org.apache.hadoop.fs.FileStatus;
 import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.util.PropertiesUtils;
 
 public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
 
@@ -546,6 +548,104 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         _testSubmit("map-reduce", actionXml);
     }
 
    public void testMapReduceWithConfigClass() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        Writer w = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")));
        w.write("dummy\n");
        w.write("dummy\n");
        w.close();

        Path jobXml = new Path(getFsTestCaseDir(), "job.xml");
        XConfiguration conf = getMapReduceConfig(inputDir.toString(), outputDir.toString());
        conf.set(MapperReducerForTest.JOB_XML_OUTPUT_LOCATION, jobXml.toUri().toString());
        conf.set("B", "b");
        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + conf.toXmlString(false)
                + "<config-class>" + OozieActionConfiguratorForTest.class.getName() + "</config-class>" + "</map-reduce>";

        _testSubmit("map-reduce", actionXml);
        Configuration conf2 = new Configuration(false);
        conf2.addResource(fs.open(jobXml));
        assertEquals("a", conf2.get("A"));
        assertEquals("c", conf2.get("B"));
    }

    public void testMapReduceWithConfigClassNotFound() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        Writer w = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")));
        w.write("dummy\n");
        w.write("dummy\n");
        w.close();

        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + getMapReduceConfig(inputDir.toString(), outputDir.toString()).toXmlString(false)
                + "<config-class>org.apache.oozie.does.not.exist</config-class>" + "</map-reduce>";

        Context context = createContext("map-reduce", actionXml);
        final RunningJob launcherJob = submitAction(context);
        waitFor(120 * 2000, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                return launcherJob.isComplete();
            }
        });
        assertTrue(launcherJob.isSuccessful());
        assertFalse(LauncherMapperHelper.isMainSuccessful(launcherJob));

        final Map<String, String> actionData = LauncherMapperHelper.getActionData(fs, context.getActionDir(),
                context.getProtoActionConf());
        Properties errorProps = PropertiesUtils.stringToProperties(actionData.get(LauncherMapper.ACTION_DATA_ERROR_PROPS));
        assertEquals("An Exception occured while instantiating the action config class",
                errorProps.getProperty("exception.message"));
        assertTrue(errorProps.getProperty("exception.stacktrace").startsWith(OozieActionConfiguratorException.class.getName()));
    }

    public void testMapReduceWithConfigClassThrowException() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        Writer w = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")));
        w.write("dummy\n");
        w.write("dummy\n");
        w.close();

        XConfiguration conf = getMapReduceConfig(inputDir.toString(), outputDir.toString());
        conf.setBoolean("oozie.test.throw.exception", true);        // causes OozieActionConfiguratorForTest to throw an exception
        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + conf.toXmlString(false)
                + "<config-class>" + OozieActionConfiguratorForTest.class.getName() + "</config-class>" + "</map-reduce>";

        Context context = createContext("map-reduce", actionXml);
        final RunningJob launcherJob = submitAction(context);
        waitFor(120 * 2000, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                return launcherJob.isComplete();
            }
        });
        assertTrue(launcherJob.isSuccessful());
        assertFalse(LauncherMapperHelper.isMainSuccessful(launcherJob));

        final Map<String, String> actionData = LauncherMapperHelper.getActionData(fs, context.getActionDir(),
                context.getProtoActionConf());
        Properties errorProps = PropertiesUtils.stringToProperties(actionData.get(LauncherMapper.ACTION_DATA_ERROR_PROPS));
        assertEquals("doh", errorProps.getProperty("exception.message"));
        assertTrue(errorProps.getProperty("exception.stacktrace").startsWith(OozieActionConfiguratorException.class.getName()));
    }

     public void testMapReduceWithCredentials() throws Exception {
         FileSystem fs = getFileSystem();
 
- 
2.19.1.windows.1

