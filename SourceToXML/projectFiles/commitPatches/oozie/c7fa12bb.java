From c7fa12bb1abac643c60c7282626c39b690ae378b Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Fri, 6 Jan 2017 10:36:26 -0800
Subject: [PATCH] OOZIE-2748 NPE in LauncherMapper.printArgs() (pbacsko via
 rkanter)

--
 .../action/hadoop/TestJavaActionExecutor.java | 26 ++++++
 release-log.txt                               |  1 +
 sharelib/oozie/pom.xml                        |  6 ++
 .../oozie/action/hadoop/LauncherMapper.java   | 17 +++-
 .../action/hadoop/TestLauncherMapper.java     | 88 +++++++++++++++++++
 5 files changed, 136 insertions(+), 2 deletions(-)
 create mode 100644 sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java

diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
index 8965cdff8..1c4b42966 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
@@ -2941,4 +2941,30 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         assertEquals("DEBUG", conf.get(oozieActionHiveRootLogger));
     }
 
    public void testEmptyArgs() throws Exception {
        String actionXml = "<java>" +
                "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                "<name-node>" + getNameNodeUri() + "</name-node>" +
                "<main-class>" + LauncherMainTester.class.getName() + "</main-class>" +
                "<arg></arg>" +
                "</java>";

        Context context = createContext(actionXml, null);
        final RunningJob runningJob = submitAction(context);
        waitFor(60 * 1000, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                return runningJob.isComplete();
            }
        });
        assertTrue(runningJob.isSuccessful());
        ActionExecutor ae = new JavaActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(ae.isCompleted(context.getAction().getExternalStatus()));
        assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
        assertNull(context.getAction().getData());

        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());
    }
 }
diff --git a/release-log.txt b/release-log.txt
index 925957b7f..e0c5be5ba 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.4.0 release (trunk - unreleased)
 
OOZIE-2748 NPE in LauncherMapper.printArgs() (pbacsko via rkanter)
 OOZIE-2754 Document Ssh action failure if output is written to stdout/stderr upon login (asasvari via rkanter)
 OOZIE-2654 Zookeeper dependent services should not depend on Connectionstate to be valid before cleaning up (venkatnrangan via abhishekbafna)
 OOZIE-2519 Oozie HA with SSL info is slightly incorrect (andras.piros via rkanter)
diff --git a/sharelib/oozie/pom.xml b/sharelib/oozie/pom.xml
index 4a89934f7..f3ea0716a 100644
-- a/sharelib/oozie/pom.xml
++ b/sharelib/oozie/pom.xml
@@ -61,6 +61,12 @@
             <scope>test</scope>
         </dependency>
 
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-all</artifactId>
            <scope>test</scope>
        </dependency>

         <dependency>
             <groupId>org.apache.oozie</groupId>
             <artifactId>oozie-hadoop-utils</artifactId>
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
index 727148664..8edebac11 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
@@ -50,6 +50,8 @@ import org.apache.hadoop.mapred.Mapper;
 import org.apache.hadoop.mapred.OutputCollector;
 import org.apache.hadoop.mapred.Reporter;
 
import com.google.common.base.Strings;

 public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, Runnable {
 
     static final String CONF_OOZIE_ACTION_MAIN_CLASS = "oozie.launcher.action.main.class";
@@ -494,10 +496,21 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
 
     public static String[] getMainArguments(Configuration conf) {
         String[] args = new String[conf.getInt(CONF_OOZIE_ACTION_MAIN_ARG_COUNT, 0)];

        int pos = 0;
         for (int i = 0; i < args.length; i++) {
            args[i] = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
            String arg = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
            if (!Strings.isNullOrEmpty(arg)) {
                args[pos++] = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
            }
         }
        return args;

        // this is to skip null args, that is <arg></arg> in the workflow XML -- in this case,
        // args[] might look like {"arg1", "arg2", null, null} at this point
        String[] retArray = new String[pos];
        System.arraycopy(args, 0, retArray, 0, pos);

        return retArray;
     }
 
     private void setupHeartBeater(Reporter reporter) {
diff --git a/sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java b/sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java
new file mode 100644
index 000000000..1dd800277
-- /dev/null
++ b/sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java
@@ -0,0 +1,88 @@
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

import static org.apache.oozie.action.hadoop.LauncherMapper.CONF_OOZIE_ACTION_MAIN_ARG_COUNT;
import static org.apache.oozie.action.hadoop.LauncherMapper.CONF_OOZIE_ACTION_MAIN_ARG_PREFIX;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;

import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class TestLauncherMapper {
    @Mock
    private Configuration conf;  // we have to use mock, because conf.set(null) throws exception

    @Test
    public void testLauncherMapperArgsHandlingWithoutNulls() {
       setupConf(Lists.newArrayList("a", "b", "c"));

       String args[] = LauncherMapper.getMainArguments(conf);

       assertTrue(Arrays.equals(new String[] { "a", "b", "c"}, args));
    }

    @Test
    public void testLauncherMapperArgsHandlingWhenArgsContainNulls() {
        setupConf(Lists.newArrayList("a", null, "b", null, "c"));

        String args[] = LauncherMapper.getMainArguments(conf);

        assertTrue(Arrays.equals(new String[] { "a", "b", "c"}, args));
    }

    @Test
    public void testLauncherMapperArgsHandlingWhenArgsContainsNullsOnly() {
        setupConf(Lists.<String>newArrayList(null, null, null));

        String args[] = LauncherMapper.getMainArguments(conf);

        assertTrue(Arrays.equals(new String[] {}, args));
    }

    @Test
    public void testLauncherMapperArgsHandlingWhenArgsContainsOneNull() {
        setupConf(Lists.<String>newArrayList((String) null));

        String args[] = LauncherMapper.getMainArguments(conf);

        assertTrue(Arrays.equals(new String[] {}, args));
    }

    private void setupConf(List<String> argList) {
        int argCount = argList.size();

        given(conf.getInt(eq(CONF_OOZIE_ACTION_MAIN_ARG_COUNT), eq(0))).willReturn(argCount);

        for (int i = 0; i < argCount; i++) {
            given(conf.get(eq(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i))).willReturn(argList.get(i));
        }
    }
}
- 
2.19.1.windows.1

