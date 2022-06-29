From d929be9f70c040c09553ba5178fffffc7f99931b Mon Sep 17 00:00:00 2001
From: egashira <ryota.egashira@yahoo.com>
Date: Mon, 2 Feb 2015 09:59:41 -0800
Subject: [PATCH] OOZIE-2119 Distcp action fails when -D option in arguments
 (ryota)

--
 .../apache/oozie/action/hadoop/TestDistcpMain.java  | 13 +++++++++++++
 release-log.txt                                     |  1 +
 .../org/apache/oozie/action/hadoop/DistcpMain.java  |  3 ++-
 3 files changed, 16 insertions(+), 1 deletion(-)

diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java
index 84351f1de..9581c5f38 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistcpMain.java
@@ -61,6 +61,7 @@ public class TestDistcpMain extends MainTestCase {
         // Check normal execution
         DistcpMain.main(new String[]{inputDir.toString(), outputDir.toString()});
         assertTrue(getFileSystem().exists(outputDir));
        fs.delete(outputDir,true);
 
         // Check exception handling
         try {
@@ -68,6 +69,18 @@ public class TestDistcpMain extends MainTestCase {
         } catch(RuntimeException re) {
             assertTrue(re.getMessage().indexOf("Returned value from distcp is non-zero") != -1);
         }

        // test -D option
        jobConf.set("mapred.job.queue.name", "non-exist");
        fs.delete(new Path(getTestCaseDir(), "action.xml"), true);
        os = new FileOutputStream(actionXml);
        jobConf.writeXml(os);

        assertFalse(getFileSystem().exists(outputDir));
        String option = "-Dmapred.job.queue.name=default"; // overwrite queue setting
        DistcpMain.main(new String[] { option, inputDir.toString(), outputDir.toString() });
        assertTrue(getFileSystem().exists(outputDir));

         return null;
     }
 }
diff --git a/release-log.txt b/release-log.txt
index 86a4f8dec..3a049a5ae 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-2119 Distcp action fails when -D option in arguments (ryota)
 OOZIE-2112 Child Job URL doesn't show properly with Hive on Tez (ryota)
 OOZIE-2122 fix test case failure of TestLiteWorkflowAppService (ryota)
 OOZIE-2055 PauseTransitService does not proceed forward if any job has issue (puru)
diff --git a/sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java b/sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java
index 67b445e25..325798f51 100644
-- a/sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java
++ b/sharelib/distcp/src/main/java/org/apache/oozie/action/hadoop/DistcpMain.java
@@ -23,6 +23,7 @@ import java.lang.reflect.InvocationTargetException;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
 
 public class DistcpMain extends JavaMain {
 
@@ -60,7 +61,7 @@ public class DistcpMain extends JavaMain {
         }
         try {
             Tool distcp = (Tool) construct.newInstance(constArgs);
            int i = distcp.run(args);
            int i = ToolRunner.run(distcp, args);
             if (i != 0) {
                 throw new RuntimeException("Returned value from distcp is non-zero (" + i + ")");
             }
- 
2.19.1.windows.1

