From 783d56e523ee98eb5ca52550b877b790b9cae9ee Mon Sep 17 00:00:00 2001
From: rkanter <rkanter@unknown>
Date: Mon, 23 Sep 2013 18:24:23 +0000
Subject: [PATCH] OOZIE-1546
 TestMapReduceActionExecutorUberJar.testMapReduceWithUberJarEnabled fails
 (rkanter)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1525669 13f79535-47bb-0310-9956-ffa450edef68
--
 .../action/hadoop/MapperReducerUberJarForTest.java  | 13 +++++++------
 release-log.txt                                     |  1 +
 .../action/hadoop/TestMapReduceActionExecutor.java  |  8 ++++----
 3 files changed, 12 insertions(+), 10 deletions(-)

diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerUberJarForTest.java b/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerUberJarForTest.java
index d126a3b5c..dba3c61de 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerUberJarForTest.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/MapperReducerUberJarForTest.java
@@ -27,10 +27,11 @@ import java.io.IOException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
 import org.apache.hadoop.io.Text;
 
/**
 * This is just like MapperReducerForTest except that this map function outputs the classpath as the value
 */
 public class MapperReducerUberJarForTest implements Mapper, Reducer {
     public static final String GROUP = "g";
     public static final String NAME = "c";
@@ -45,18 +46,18 @@ public class MapperReducerUberJarForTest implements Mapper, Reducer {
     public void close() throws IOException {
     }
 
    private static final LongWritable zero = new LongWritable(0);

     @SuppressWarnings("unchecked")
     public void map(Object key, Object value, OutputCollector collector, Reporter reporter) throws IOException {
        StringBuilder sb = new StringBuilder();
         ClassLoader applicationClassLoader = this.getClass().getClassLoader();
         if (applicationClassLoader == null) {
             applicationClassLoader = ClassLoader.getSystemClassLoader();
         }
         URL[] urls = ((URLClassLoader) applicationClassLoader).getURLs();
         for (URL url : urls) {
            collector.collect(zero, new Text(url.toString()));
            sb.append(url.toString()).append("@");
         }
        collector.collect(key, new Text(sb.toString()));
         reporter.incrCounter(GROUP, NAME, 5l);
     }
 
@@ -64,7 +65,7 @@ public class MapperReducerUberJarForTest implements Mapper, Reducer {
     public void reduce(Object key, Iterator values, OutputCollector collector, Reporter reporter)
             throws IOException {
         while (values.hasNext()) {
            collector.collect(values.next(), NullWritable.get());
            collector.collect(key, values.next());
         }
     }
 }
diff --git a/release-log.txt b/release-log.txt
index 34bc08579..e8e7f4220 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1546 TestMapReduceActionExecutorUberJar.testMapReduceWithUberJarEnabled fails (rkanter)
 OOZIE-1545 RecoveryService keeps repeatedly queueing SuspendXCommand (rohini)
 OOZIE-1547 Change Coordinator SELECT query to fetch only necessary columns and consolidate JPA Executors (ryota)
 OOZIE-1529 Disable job DAG display for workflow having huge actions (puru via rohini)
diff --git a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
index ae3269ef1..20645d775 100644
-- a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
++ b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
@@ -502,11 +502,11 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         boolean containsLib1Jar = false;
         String lib1JarStr = "jobcache/" + jobID + "/jars/lib/lib1.jar";
         Pattern lib1JarPatYarn = Pattern.compile(
                ".*appcache/application_" + jobID.replaceFirst("job_", "") + "/filecache/.*/uber.jar/lib/lib1.jar");
                ".*appcache/application_" + jobID.replaceFirst("job_", "") + "/filecache/.*/uber.jar/lib/lib1.jar.*");
         boolean containsLib2Jar = false;
         String lib2JarStr = "jobcache/" + jobID + "/jars/lib/lib1.jar";
         Pattern lib2JarPatYarn = Pattern.compile(
                ".*appcache/application_" + jobID.replaceFirst("job_", "") + "/filecache/.*/uber.jar/lib/lib2.jar");
                ".*appcache/application_" + jobID.replaceFirst("job_", "") + "/filecache/.*/uber.jar/lib/lib2.jar.*");
 
         FileStatus[] fstats = getFileSystem().listStatus(outputDir);
         for (FileStatus fstat : fstats) {
@@ -516,8 +516,8 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
                 Scanner sc = new Scanner(is);
                 while (sc.hasNextLine()) {
                     String line = sc.nextLine();
                    containsLib1Jar = (containsLib1Jar || line.endsWith(lib1JarStr) || lib1JarPatYarn.matcher(line).matches());
                    containsLib2Jar = (containsLib2Jar || line.endsWith(lib2JarStr) || lib2JarPatYarn.matcher(line).matches());
                    containsLib1Jar = (containsLib1Jar || line.contains(lib1JarStr) || lib1JarPatYarn.matcher(line).matches());
                    containsLib2Jar = (containsLib2Jar || line.contains(lib2JarStr) || lib2JarPatYarn.matcher(line).matches());
                 }
                 sc.close();
                 is.close();
- 
2.19.1.windows.1

