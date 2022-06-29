From e0b7cde711b1b9e1a03660ec635041eeb9755049 Mon Sep 17 00:00:00 2001
From: Peter Bacsko <pbacsko@cloudera.com>
Date: Mon, 15 May 2017 12:50:28 +0200
Subject: [PATCH] OOZIE-2872 Address backward compatibility issue introduced by
 OOZIE-2748 (pbacsko)

--
 .../action/hadoop/JavaActionExecutor.java     |  6 +++
 core/src/main/resources/oozie-default.xml     |  9 +++++
 .../action/hadoop/TestJavaActionExecutor.java | 17 +++++++--
 release-log.txt                               |  1 +
 .../oozie/action/hadoop/LauncherMapper.java   | 35 +++++++++++++-----
 .../action/hadoop/TestLauncherMapper.java     | 37 +++++++++++++++++--
 6 files changed, 88 insertions(+), 17 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index d60a5c7b9..06ae5fd9a 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -998,6 +998,12 @@ public class JavaActionExecutor extends ActionExecutor {
                 args[i] = list.get(i).getTextTrim();
             }
             LauncherMapperHelper.setupMainArguments(launcherJobConf, args);
            // backward compatibility flag - see OOZIE-2872
            if (ConfigurationService.getBoolean(LauncherMapper.CONF_OOZIE_NULL_ARGS_ALLOWED)) {
                launcherJobConf.setBoolean(LauncherMapper.CONF_OOZIE_NULL_ARGS_ALLOWED, true);
            } else {
                launcherJobConf.setBoolean(LauncherMapper.CONF_OOZIE_NULL_ARGS_ALLOWED, false);
            }
 
             // Make mapred.child.java.opts and mapreduce.map.java.opts equal, but give values from the latter priority; also append
             // <java-opt> and <java-opts> and give those highest priority
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 076401d8c..205c89b9b 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -3047,4 +3047,13 @@ will be the requeue interval for the actions which are waiting for a long time w
         <description>Regex pattern for HCat URIs. The regex can be modified by users as per requirement
             for parsing/splitting the HCat URIs.</description>
     </property>

    <property>
        <name>oozie.actions.null.args.allowed</name>
        <value>true</value>
        <description>
            When set to true, empty arguments (like &lt;arg&gt;&lt;/arg&gt;) will be passed as "null" to the main method of a
            given action. That is, the args[] array will contain "null" elements. When set to false, then "nulls" are removed.
        </description>
    </property>
 </configuration>
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
index d1f53fee4..b27b3d8a2 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
@@ -2941,7 +2941,18 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         assertEquals("DEBUG", conf.get(oozieActionHiveRootLogger));
     }
 
    public void testEmptyArgs() throws Exception {
    public void testEmptyArgsWithNullArgsNotAllowed() throws Exception {
        testEmptyArgs(false, "SUCCEEDED", WorkflowAction.Status.OK);
    }

    public void testEmptyArgsWithNullArgsAllowed() throws Exception {
        testEmptyArgs(true, "FAILED/KILLED", WorkflowAction.Status.ERROR);
    }

    private void testEmptyArgs(boolean nullArgsAllowed, String expectedExternalStatus, WorkflowAction.Status expectedStatus)
            throws Exception {
        ConfigurationService.setBoolean(LauncherMapper.CONF_OOZIE_NULL_ARGS_ALLOWED, nullArgsAllowed);

         String actionXml = "<java>" +
                 "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                 "<name-node>" + getNameNodeUri() + "</name-node>" +
@@ -2961,11 +2972,11 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         ActionExecutor ae = new JavaActionExecutor();
         ae.check(context, context.getAction());
         assertTrue(ae.isCompleted(context.getAction().getExternalStatus()));
        assertEquals("SUCCEEDED", context.getAction().getExternalStatus());
        assertEquals(expectedExternalStatus, context.getAction().getExternalStatus());
         assertNull(context.getAction().getData());
 
         ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());
        assertEquals(expectedStatus, context.getAction().getStatus());
     }
 
     public void testMaxOutputDataSetByUser() {
diff --git a/release-log.txt b/release-log.txt
index 5800715f2..03d0df931 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.4.0 release (trunk - unreleased)
 
OOZIE-2872 Address backward compatibility issue introduced by OOZIE-2748 (pbacsko)
 OOZIE-2780 Upgrade minimum Hadoop version to 2.6.0 (dbist13 via rkanter)
 OOZIE-2824 Fix typos in documentation (lzeke via gezapeti)
 OOZIE-2874 Make the Launcher Mapper map-only job's InputFormat class pluggable (andras.piros via gezapeti)
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
index 8edebac11..8657c678c 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
@@ -63,6 +63,7 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
     static final String CONF_OOZIE_EXTERNAL_STATS_MAX_SIZE = "oozie.external.stats.max.size";
     static final String OOZIE_ACTION_CONFIG_CLASS = ACTION_PREFIX + "config.class";
     static final String CONF_OOZIE_ACTION_FS_GLOB_MAX = ACTION_PREFIX + "fs.glob.max";
    static final String CONF_OOZIE_NULL_ARGS_ALLOWED = ACTION_PREFIX + "null.args.allowed";
 
     static final String COUNTER_GROUP = "oozie.launcher";
     static final String COUNTER_LAUNCHER_ERROR = "oozie.launcher.error";
@@ -497,18 +498,28 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
     public static String[] getMainArguments(Configuration conf) {
         String[] args = new String[conf.getInt(CONF_OOZIE_ACTION_MAIN_ARG_COUNT, 0)];
 
        int pos = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
            if (!Strings.isNullOrEmpty(arg)) {
                args[pos++] = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
        String[] retArray;

        if (conf.getBoolean(CONF_OOZIE_NULL_ARGS_ALLOWED, true)) {
            for (int i = 0; i < args.length; i++) {
                args[i] = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
            }

            retArray = args;
        } else {
            int pos = 0;
            for (int i = 0; i < args.length; i++) {
                String arg = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
                if (!Strings.isNullOrEmpty(arg)) {
                    args[pos++] = conf.get(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i);
                }
             }
        }
 
        // this is to skip null args, that is <arg></arg> in the workflow XML -- in this case,
        // args[] might look like {"arg1", "arg2", null, null} at this point
        String[] retArray = new String[pos];
        System.arraycopy(args, 0, retArray, 0, pos);
            // this is to skip null args, that is <arg></arg> in the workflow XML -- in this case,
            // args[] might look like {"arg1", "arg2", null, null} at this point
            retArray = new String[pos];
            System.arraycopy(args, 0, retArray, 0, pos);
        }
 
         return retArray;
     }
@@ -632,6 +643,10 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
         System.out.println(banner);
         boolean maskNextArg = false;
         for (String arg : args) {
            if (arg == null) {
                arg = "null"; // prevent NPE in pwd masking
            }

             if (maskNextArg) {
                 System.out.println("             " + "********");
                 maskNextArg = false;
diff --git a/sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java b/sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java
index 1dd800277..51b1d6f8c 100644
-- a/sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java
++ b/sharelib/oozie/src/test/java/org/apache/oozie/action/hadoop/TestLauncherMapper.java
@@ -23,6 +23,7 @@ import static org.apache.oozie.action.hadoop.LauncherMapper.CONF_OOZIE_ACTION_MA
 import static org.junit.Assert.assertTrue;
 import static org.mockito.BDDMockito.given;
 import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyBoolean;
 
 import java.util.Arrays;
 import java.util.List;
@@ -41,8 +42,9 @@ public class TestLauncherMapper {
     private Configuration conf;  // we have to use mock, because conf.set(null) throws exception
 
     @Test
    public void testLauncherMapperArgsHandlingWithoutNulls() {
    public void testArgsHandlingWithoutNullsAndNullsNotAllowed() {
        setupConf(Lists.newArrayList("a", "b", "c"));
       setEnableNullArgsAllowed(false);
 
        String args[] = LauncherMapper.getMainArguments(conf);
 
@@ -50,8 +52,9 @@ public class TestLauncherMapper {
     }
 
     @Test
    public void testLauncherMapperArgsHandlingWhenArgsContainNulls() {
    public void testHandlingWhenArgsContainNullsAndNullsNotAllowed() {
         setupConf(Lists.newArrayList("a", null, "b", null, "c"));
        setEnableNullArgsAllowed(false);
 
         String args[] = LauncherMapper.getMainArguments(conf);
 
@@ -59,8 +62,9 @@ public class TestLauncherMapper {
     }
 
     @Test
    public void testLauncherMapperArgsHandlingWhenArgsContainsNullsOnly() {
    public void testArgsHandlingWhenArgsContainsNullsOnlyAndNullsNotAllowed() {
         setupConf(Lists.<String>newArrayList(null, null, null));
        setEnableNullArgsAllowed(false);
 
         String args[] = LauncherMapper.getMainArguments(conf);
 
@@ -68,14 +72,35 @@ public class TestLauncherMapper {
     }
 
     @Test
    public void testLauncherMapperArgsHandlingWhenArgsContainsOneNull() {
    public void testArgsHandlingWhenArgsContainsOneNullAndNullsNotAllowed() {
         setupConf(Lists.<String>newArrayList((String) null));
        setEnableNullArgsAllowed(false);
 
         String args[] = LauncherMapper.getMainArguments(conf);
 
         assertTrue(Arrays.equals(new String[] {}, args));
     }
 
    @Test
    public void testHandlingWhenArgsContainNullsAndNullAllowed() {
        setupConf(Lists.newArrayList("a", null, "b", null, "c"));
        setEnableNullArgsAllowed(true);

        String args[] = LauncherMapper.getMainArguments(conf);

        assertTrue(Arrays.equals(new String[] { "a", null, "b", null, "c"}, args));
    }

    @Test
    public void testArgsHandlingWhenArgsContainsOneNullAndNullsAllowed() {
        setupConf(Lists.<String>newArrayList((String) null));
        setEnableNullArgsAllowed(true);

        String args[] = LauncherMapper.getMainArguments(conf);

        assertTrue(Arrays.equals(new String[] { null }, args));
    }

     private void setupConf(List<String> argList) {
         int argCount = argList.size();
 
@@ -85,4 +110,8 @@ public class TestLauncherMapper {
             given(conf.get(eq(CONF_OOZIE_ACTION_MAIN_ARG_PREFIX + i))).willReturn(argList.get(i));
         }
     }

    private void setEnableNullArgsAllowed(boolean nullArgsAllowed) {
        given(conf.getBoolean(eq(LauncherMapper.CONF_OOZIE_NULL_ARGS_ALLOWED), anyBoolean())).willReturn(nullArgsAllowed);
    }
 }
- 
2.19.1.windows.1

