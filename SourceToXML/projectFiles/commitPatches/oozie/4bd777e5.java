From 4bd777e5e45ce097aa07fbc1385623139d102069 Mon Sep 17 00:00:00 2001
From: egashira <ryota.egashira@yahoo.com>
Date: Wed, 15 Oct 2014 15:33:33 -0700
Subject: [PATCH] OOZIE-1728 When an ApplicationMaster restarts, it restarts
 the launcher job: DistCp followup (ryota)

--
 core/pom.xml                                  |  6 ++
 .../action/hadoop/DistcpActionExecutor.java   | 21 +++-
 .../hadoop/TestDistCpActionExecutor.java      |  3 +-
 .../oozie/action/hadoop/TestDistcpMain.java   | 73 ++++++++++++++
 release-log.txt                               |  3 +-
 sharelib/distcp/pom.xml                       | 11 ---
 .../oozie/action/hadoop/DistcpMain.java       | 97 +++++++++++++++++++
 7 files changed, 196 insertions(+), 18 deletions(-)
 rename {sharelib/distcp => core}/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java (98%)
 create mode 100644 core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java
 create mode 100644 sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java

diff --git a/core/pom.xml b/core/pom.xml
index 7cd1f7006..597775cb6 100644
-- a/core/pom.xml
++ b/core/pom.xml
@@ -260,6 +260,12 @@
             <scope>compile</scope>
         </dependency>
 
         <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-sharelib-distcp</artifactId>
            <scope>compile</scope>
        </dependency>

         <dependency>
             <groupId>org.mockito</groupId>
             <artifactId>mockito-all</artifactId>
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java
index 86d21fb04..4d2f7b282 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java
@@ -18,6 +18,7 @@
 
 package org.apache.oozie.action.hadoop;
 
import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.hadoop.conf.Configuration;
@@ -27,9 +28,9 @@ import org.apache.oozie.service.Services;
 import org.apache.oozie.util.XLog;
 import org.jdom.Element;
 

 public class DistcpActionExecutor extends JavaActionExecutor{
    public static final String CONF_OOZIE_DISTCP_ACTION_MAIN_CLASS = "org.apache.hadoop.tools.DistCp";
    public static final String CONF_OOZIE_DISTCP_ACTION_MAIN_CLASS = "org.apache.oozie.action.hadoop.DistcpMain";
    private static final String DISTCP_MAIN_CLASS_NAME = "org.apache.hadoop.tools.DistCp";
     public static final String CLASS_NAMES = "oozie.actions.main.classnames";
     private static final XLog LOG = XLog.getLog(DistcpActionExecutor.class);
     public static final String DISTCP_TYPE = "distcp";
@@ -47,13 +48,20 @@ public class DistcpActionExecutor extends JavaActionExecutor{
         if(name != null){
             classNameDistcp = name;
         }
        actionConf.set(JavaMain.JAVA_MAIN_CLASS, classNameDistcp);
        actionConf.set(JavaMain.JAVA_MAIN_CLASS, DISTCP_MAIN_CLASS_NAME);
         return actionConf;
     }
 
     @Override
     public List<Class> getLauncherClasses() {
       return super.getLauncherClasses();
        List<Class> classes = new ArrayList<Class>();
        try {
            classes.add(Class.forName(CONF_OOZIE_DISTCP_ACTION_MAIN_CLASS));
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found", e);
        }
        return classes;
     }
 
     /**
@@ -106,4 +114,9 @@ public class DistcpActionExecutor extends JavaActionExecutor{
         return "distcp";
     }
 
    @Override
    protected String getLauncherMain(Configuration launcherConf, Element actionXml) {
        return launcherConf.get(LauncherMapper.CONF_OOZIE_ACTION_MAIN_CLASS, CONF_OOZIE_DISTCP_ACTION_MAIN_CLASS);
    }

 }
diff --git a/sharelib/distcp/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
similarity index 98%
rename from sharelib/distcp/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
rename to core/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
index 7a098f340..d6ac5542a 100644
-- a/sharelib/distcp/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
@@ -49,7 +49,7 @@ public class TestDistCpActionExecutor extends ActionExecutorTestCase{
     @SuppressWarnings("unchecked")
     public void testSetupMethods() throws Exception {
         DistcpActionExecutor ae = new DistcpActionExecutor();
        assertEquals(Arrays.asList(JavaMain.class), ae.getLauncherClasses());
        assertEquals(Arrays.asList(DistcpMain.class), ae.getLauncherClasses());
     }
 
     public void testDistCpFile() throws Exception {
@@ -130,7 +130,6 @@ public class TestDistCpActionExecutor extends ActionExecutorTestCase{
         return new Context(wf, action);
     }
 

     protected RunningJob submitAction(Context context) throws Exception {
         DistcpActionExecutor ae = new DistcpActionExecutor();
 
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java
new file mode 100644
index 000000000..84351f1de
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java
@@ -0,0 +1,73 @@
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

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.oozie.util.XConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.oozie.action.hadoop.DistcpMain;

public class TestDistcpMain extends MainTestCase {

    @Override
    public Void call() throws Exception {

        XConfiguration jobConf = new XConfiguration();
        XConfiguration.copy(createJobConf(), jobConf);

        FileSystem fs = getFileSystem();
        Path inputDir = new Path(getFsTestCaseDir(), "input");
        fs.mkdirs(inputDir);
        Writer writer = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")));
        writer.write("hello");
        writer.close();
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        jobConf.set(LauncherMapper.CONF_OOZIE_ACTION_MAIN_CLASS, "org.apache.hadoop.tools.DistCp");

        jobConf.set("mapreduce.job.tags", "" + System.currentTimeMillis());
        setSystemProperty("oozie.job.launch.time", "" + System.currentTimeMillis());

        File actionXml = new File(getTestCaseDir(), "action.xml");
        OutputStream os = new FileOutputStream(actionXml);
        jobConf.writeXml(os);
        os.close();

        System.setProperty("oozie.action.conf.xml", actionXml.getAbsolutePath());

        // Check normal execution
        DistcpMain.main(new String[]{inputDir.toString(), outputDir.toString()});
        assertTrue(getFileSystem().exists(outputDir));

        // Check exception handling
        try {
            DistcpMain.main(new String[0]);
        } catch(RuntimeException re) {
            assertTrue(re.getMessage().indexOf("Returned value from distcp is non-zero") != -1);
        }
        return null;
    }
}
diff --git a/release-log.txt b/release-log.txt
index 353174c4e..6fb9a8ff0 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,7 +1,7 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-1728 When an ApplicationMaster restarts, it restarts the launcher job: DistCp followup (ryota)
 OOZIE-2009 Requeue CoordActionInputCheck in case of permission error (ryota)
OOZIE-2005 Coordinator rerun fails to initialize error code and message (ryota)
 OOZIE-1896 ZKUUIDService - Too many job submission fails (puru)
 OOZIE-2019 SLA miss processed on server2 not send email (puru)
 OOZIE-1391 Sub wf suspend doesn't update parent wf (jaydeepvishwakarma via shwethags)
@@ -35,6 +35,7 @@ OOZIE-1943 Bump up trunk to 4.2.0-SNAPSHOT (bzhang)
 
 -- Oozie 4.1.0 release (4.1 - unreleased)
 
OOZIE-2005 Coordinator rerun fails to initialize error code and message (ryota)
 OOZIE-2026 fix synchronization in SLACalculatorMemory.addJobStatus to avoid duplicated SLA message (ryota)
 OOZIE-2017 On startup, StatusTransitService can transition Coordinators that were in PREPSUSPENDED to RUNNING (rkanter)
 OOZIE-1932 Services should load CallableQueueService after MemoryLocksService (mona)
diff --git a/sharelib/distcp/pom.xml b/sharelib/distcp/pom.xml
index 04e436d42..b788ed01f 100644
-- a/sharelib/distcp/pom.xml
++ b/sharelib/distcp/pom.xml
@@ -43,17 +43,6 @@
             <artifactId>oozie-hadoop-distcp</artifactId>
             <scope>compile</scope>
         </dependency>
        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-core</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-core</artifactId>
            <classifier>tests</classifier>
            <scope>test</scope>
        </dependency>
         <dependency>
             <groupId>org.apache.oozie</groupId>
             <artifactId>oozie-hadoop</artifactId>
diff --git a/sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java b/sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java
new file mode 100644
index 000000000..67b445e25
-- /dev/null
++ b/sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java
@@ -0,0 +1,97 @@
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;

public class DistcpMain extends JavaMain {

    private Constructor<?> construct;
    private Object[] constArgs;

    public static void main(String[] args) throws Exception {
        run(DistcpMain.class, args);
    }

    @Override
    protected void run(String[] args) throws Exception {

        Configuration actionConf = loadActionConf();
        LauncherMainHadoopUtils.killChildYarnJobs(actionConf);
        Class<?> klass = actionConf.getClass(LauncherMapper.CONF_OOZIE_ACTION_MAIN_CLASS,
                org.apache.hadoop.tools.DistCp.class);
        System.out.println("Main class        : " + klass.getName());
        System.out.println("Arguments         :");
        for (String arg : args) {
            System.out.println("                    " + arg);
        }

        // propagate delegation related props from launcher job to MR job
        if (getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION") != null) {
            actionConf.set("mapreduce.job.credentials.binary", getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION"));
        }

        getConstructorAndArgs(klass, actionConf);
        if (construct == null) {
            throw new RuntimeException("Distcp constructor was not found, unable to instantiate");
        }
        if (constArgs == null) {
            throw new RuntimeException("Arguments for distcp constructor is null, unable to instantiate");
        }
        try {
            Tool distcp = (Tool) construct.newInstance(constArgs);
            int i = distcp.run(args);
            if (i != 0) {
                throw new RuntimeException("Returned value from distcp is non-zero (" + i + ")");
            }
        }
        catch (InvocationTargetException ex) {
            throw new JavaMainException(ex.getCause());
        }
    }

    protected void getConstructorAndArgs(Class<?> klass, Configuration actionConf) throws Exception {
        Constructor<?>[] allConstructors = klass.getConstructors();
        for (Constructor<?> cstruct : allConstructors) {
            Class<?>[] pType = cstruct.getParameterTypes();
            construct = cstruct;
            if (pType.length == 1 && pType[0].equals(Class.forName("org.apache.hadoop.conf.Configuration"))) {
                System.out.println("found Distcp v1 Constructor");
                System.out.println("                    " + cstruct.toString());
                constArgs = new Object[1];
                constArgs[0] = actionConf;
                break;
            }
            else if (pType.length == 2 && pType[0].equals(Class.forName("org.apache.hadoop.conf.Configuration"))) {
                // 2nd argument is org.apache.hadoop.tools.DistCpOptions
                System.out.println("found Distcp v2 Constructor");
                System.out.println("                    " + cstruct.toString());
                constArgs = new Object[2];
                constArgs[0] = actionConf;
                constArgs[1] = null;
                break;
            }
        }
    }

}
- 
2.19.1.windows.1

