From 109f41c6607e889f88709038b9fce8d266d154e1 Mon Sep 17 00:00:00 2001
From: ryota <ryota@unknown>
Date: Tue, 19 Nov 2013 00:55:09 +0000
Subject: [PATCH] OOZIE-1604 <java-opts> and <java-opt> not added to
 Application Master property in uber mode (ryota)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1543280 13f79535-47bb-0310-9956-ffa450edef68
--
 .../action/hadoop/JavaActionExecutor.java     |  4 +-
 .../action/hadoop/TestJavaActionExecutor.java | 96 ++++++++++++++++++-
 release-log.txt                               |  1 +
 3 files changed, 95 insertions(+), 6 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index 4bbd705e4..20f702a14 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -233,7 +233,6 @@ public class JavaActionExecutor extends ActionExecutor {
                 XConfiguration actionDefaultConf = has.createActionDefaultConf(conf.get(HADOOP_JOB_TRACKER), getType());
                 injectLauncherProperties(actionDefaultConf, launcherConf);
                 injectLauncherProperties(inlineConf, launcherConf);
                injectLauncherUseUberMode(launcherConf);
                 checkForDisallowedProps(launcherConf, "launcher configuration");
                 XConfiguration.copy(launcherConf, conf);
             }
@@ -734,6 +733,9 @@ public class JavaActionExecutor extends ActionExecutor {
                 launcherJobConf.set("mapred.child.java.opts", opts);
             }
 
            // setting for uber mode
            injectLauncherUseUberMode(launcherJobConf);

             // properties from action that are needed by the launcher (e.g. QUEUE NAME, ACLs)
             // maybe we should add queue to the WF schema, below job-tracker
             actionConfToLauncherConf(actionConf, launcherJobConf);
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
index 644c66149..5cf68313d 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
@@ -1575,8 +1575,8 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         action.setType(ae.getType());
 
         Context context = new Context(wf, action);
        JobConf launcherConf = ae.createBaseHadoopConf(context, actionXml1);
        ae.setupLauncherConf(launcherConf, actionXml1, getFsTestCaseDir(), context);
        JobConf launcherConf = new JobConf();
        launcherConf = ae.createLauncherConf(getFileSystem(), context, action, actionXml1, launcherConf);
         // memoryMB (2048 + 512)
         assertEquals("2560", launcherConf.get(JavaActionExecutor.YARN_AM_RESOURCE_MB));
         // heap size in child.opts (2048 + 512)
@@ -1607,8 +1607,7 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
                         + "<property><name>oozie.launcher.mapred.child.env</name><value>B=bar</value></property>"
                         + "</configuration>" + "<main-class>MAIN-CLASS</main-class>" + "</java>");
 
        launcherConf = ae.createBaseHadoopConf(context, actionXml2);
        ae.setupLauncherConf(launcherConf, actionXml2, getFsTestCaseDir(), context);
        launcherConf = ae.createLauncherConf(getFileSystem(), context, action, actionXml2, launcherConf);
 
         // memoryMB (3072 + 512)
         assertEquals("3584", launcherConf.get(JavaActionExecutor.YARN_AM_RESOURCE_MB));
@@ -1644,7 +1643,7 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
                         + "</configuration>" + "<main-class>MAIN-CLASS</main-class>" + "</java>");
 
         launcherConf = ae.createBaseHadoopConf(context, actionXml3);
        ae.setupLauncherConf(launcherConf, actionXml3, getFsTestCaseDir(), context);
        launcherConf = ae.createLauncherConf(getFileSystem(), context, action, actionXml3, launcherConf);
 
         // memoryMB (limit to 4096)
         assertEquals("4096", launcherConf.get(JavaActionExecutor.YARN_AM_RESOURCE_MB));
@@ -1658,6 +1657,93 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         assertEquals("A=foo,B=bar", launcherConf.get(JavaActionExecutor.YARN_AM_ENV));
     }
 
    public void testUpdateConfForUberModeForJavaOpts() throws Exception {
        Services.get().getConf().setBoolean("oozie.action.launcher.mapreduce.job.ubertask.enable", true);

        Element actionXml1 = XmlUtils
                .parseXml("<java>"
                        + "<job-tracker>"
                        + getJobTrackerUri()
                        + "</job-tracker>"
                        + "<name-node>"
                        + getNameNodeUri()
                        + "</name-node>"
                        + "<configuration>"
                        + "<property><name>oozie.launcher.yarn.app.mapreduce.am.command-opts</name>"
                        + "<value>-Xmx1024m -Djava.net.preferIPv4Stack=true </value></property>"
                        + "<property><name>oozie.launcher.mapred.child.java.opts</name><value>-Xmx1536m</value></property>"
                        + "</configuration>" + "<main-class>MAIN-CLASS</main-class>"
                        + "<java-opt>-Xmx2048m</java-opt>"
                        + "<java-opt>-Dkey1=val1</java-opt>"
                        + "<java-opt>-Dkey2=val2</java-opt>"
                        + "</java>");
        JavaActionExecutor ae = new JavaActionExecutor();
        XConfiguration protoConf = new XConfiguration();
        protoConf.set(WorkflowAppService.HADOOP_USER, getTestUser());

        WorkflowJobBean wf = createBaseWorkflow(protoConf, "action");
        WorkflowActionBean action = (WorkflowActionBean) wf.getActions().get(0);
        action.setType(ae.getType());

        Context context = new Context(wf, action);
        JobConf launcherConf = new JobConf();
        launcherConf = ae.createLauncherConf(getFileSystem(), context, action, actionXml1, launcherConf);

        // heap size (2048 + 512)
        int heapSize = ae.extractHeapSizeMB(launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS));
        assertEquals("-Xmx1024m -Djava.net.preferIPv4Stack=true -Xmx1536m -Xmx2048m -Dkey1=val1 -Dkey2=val2 -Xmx2560m",
                launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS).trim());
        assertEquals(2560, heapSize);

        Element actionXml2 = XmlUtils
                .parseXml("<java>"
                        + "<job-tracker>"
                        + getJobTrackerUri()
                        + "</job-tracker>"
                        + "<name-node>"
                        + getNameNodeUri()
                        + "</name-node>"
                        + "<configuration>"
                        + "<property><name>oozie.launcher.yarn.app.mapreduce.am.command-opts</name>"
                        + "<value>-Xmx1024m -Djava.net.preferIPv4Stack=true </value></property>"
                        + "<property><name>oozie.launcher.mapred.child.java.opts</name><value>-Xmx1536m</value></property>"
                        + "</configuration>" + "<main-class>MAIN-CLASS</main-class>"
                        + "<java-opts>-Xmx2048m -Dkey1=val1</java-opts>"
                        + "</java>");

        launcherConf = ae.createLauncherConf(getFileSystem(), context, action, actionXml2, launcherConf);

        // heap size (2048 + 512)
        heapSize = ae.extractHeapSizeMB(launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS));
        assertEquals("-Xmx1024m -Djava.net.preferIPv4Stack=true -Xmx1536m -Xmx2048m -Dkey1=val1 -Xmx2560m",
                launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS).trim());
        assertEquals(2560, heapSize);

        Element actionXml3 = XmlUtils
                .parseXml("<java>"
                        + "<job-tracker>"
                        + getJobTrackerUri()
                        + "</job-tracker>"
                        + "<name-node>"
                        + getNameNodeUri()
                        + "</name-node>"
                        + "<configuration>"
                        + "<property><name>oozie.launcher.yarn.app.mapreduce.am.command-opts</name>"
                        + "<value>-Xmx2048m -Djava.net.preferIPv4Stack=true </value></property>"
                        + "<property><name>oozie.launcher.mapred.child.java.opts</name><value>-Xmx3072m</value></property>"
                        + "</configuration>" + "<main-class>MAIN-CLASS</main-class>"
                        + "<java-opts>-Xmx1024m -Dkey1=val1</java-opts>"
                        + "</java>");

        launcherConf = ae.createLauncherConf(getFileSystem(), context, action, actionXml3, launcherConf);

        // heap size (2048 + 512)
        heapSize = ae.extractHeapSizeMB(launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS));
        assertEquals("-Xmx2048m -Djava.net.preferIPv4Stack=true -Xmx3072m -Xmx1024m -Dkey1=val1 -Xmx2560m",
                launcherConf.get(JavaActionExecutor.YARN_AM_COMMAND_OPTS).trim());
        assertEquals(2560, heapSize);
    }

     public void testAddToCache() throws Exception {
         JavaActionExecutor ae = new JavaActionExecutor();
         Configuration conf = new XConfiguration();
diff --git a/release-log.txt b/release-log.txt
index 0a250de9b..88b4530ef 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1604 <java-opts> and <java-opt> not added to Application Master property in uber mode (ryota)
 OOZIE-1584 Setup sharelib using script and pickup latest(honor ship.launcher) and remove DFS dependency at startup (puru via ryota)
 OOZIE-1550 Create a safeguard to kill errant recursive workflows before they bring down oozie (rkanter)
 OOZIE-1314 IllegalArgumentException: wfId cannot be empty (shwethags via virag)
- 
2.19.1.windows.1

