From 396fcc6c453b28ad09a5940a390ca1a88b33cbfa Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Fri, 4 Sep 2015 15:03:41 -0700
Subject: [PATCH] OOZIE-2355 Hive2 Action doesn't pass along oozie configs to
 jobconf (rkanter)

--
 examples/src/main/apps/hive2/script.q                      | 1 +
 release-log.txt                                            | 1 +
 .../java/org/apache/oozie/action/hadoop/Hive2Main.java     | 7 +++++++
 3 files changed, 9 insertions(+)

diff --git a/examples/src/main/apps/hive2/script.q b/examples/src/main/apps/hive2/script.q
index 3abc757c6..37d656451 100644
-- a/examples/src/main/apps/hive2/script.q
++ b/examples/src/main/apps/hive2/script.q
@@ -15,5 +15,6 @@
 -- See the License for the specific language governing permissions and
 -- limitations under the License.
 --
DROP TABLE IF EXISTS test;
 CREATE EXTERNAL TABLE test (a INT) STORED AS TEXTFILE LOCATION '${INPUT}';
 INSERT OVERWRITE DIRECTORY '${OUTPUT}' SELECT * FROM test;
diff --git a/release-log.txt b/release-log.txt
index 88e03fccd..0bd450ea9 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2355 Hive2 Action doesn't pass along oozie configs to jobconf (rkanter)
 OOZIE-2318 Provide better solution for specifying SSL truststore to Oozie Client (rkanter)
 OOZIE-2344 Enabling 'oozie.action.jobinfo.enable' doesn't inject the job information into the map/reduce job's configuration. (akshayrai09 via rkanter)
 OOZIE-2350 Package changes for release (shwethags)
diff --git a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
index 97af28b17..56f5451ca 100644
-- a/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
++ b/sharelib/hive2/src/main/java/org/apache/oozie/action/hadoop/Hive2Main.java
@@ -27,6 +27,7 @@ import java.io.PrintStream;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
import java.util.Map;
 import java.util.Set;
 import java.util.regex.Pattern;
 
@@ -210,10 +211,16 @@ public class Hive2Main extends LauncherMain {
             arguments.add(beelineArg);
         }
 
        // Propagate MR job tag if defined
         if (actionConf.get(LauncherMain.MAPREDUCE_JOB_TAGS) != null ) {
             arguments.add("--hiveconf");
             arguments.add("mapreduce.job.tags=" + actionConf.get(LauncherMain.MAPREDUCE_JOB_TAGS));
         }
        // Propagate "oozie.*" configs
        for (Map.Entry<String, String> oozieConfig : actionConf.getValByRegex("^oozie\\.(?!launcher).+").entrySet()) {
            arguments.add("--hiveconf");
            arguments.add(oozieConfig.getKey() + "=" + oozieConfig.getValue());
        }
 
         System.out.println("Beeline command arguments :");
         for (String arg : arguments) {
- 
2.19.1.windows.1

