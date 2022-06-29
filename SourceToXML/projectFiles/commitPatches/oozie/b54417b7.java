From b54417b79b12b20450ff4758f3b690d4200a83dd Mon Sep 17 00:00:00 2001
From: Harsh J <harsh@cloudera.com>
Date: Sat, 27 Dec 2014 15:31:03 +0530
Subject: [PATCH] OOZIE-2102. Streaming actions are broken cause of incorrect
 method signature. Contributed by Harsh J.

--
 .../apache/oozie/action/hadoop/PipesMain.java   |  2 --
 .../oozie/action/hadoop/StreamingMain.java      | 17 +++++++----------
 2 files changed, 7 insertions(+), 12 deletions(-)

diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
index 0d38040fc..bf91b4327 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
@@ -65,8 +65,6 @@ public class PipesMain extends MapReduceMain {
             }
         }
 
        addActionConf(jobConf, jobConf);

         //propagate delegation related props from launcher job to MR job
         if (getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION") != null) {
             jobConf.set("mapreduce.job.credentials.binary", getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION"));
diff --git a/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java b/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java
index b41249005..c65c8594d 100644
-- a/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java
++ b/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java
@@ -29,9 +29,8 @@ public class StreamingMain extends MapReduceMain {
         run(StreamingMain.class, args);
     }
 
    protected RunningJob submitJob(Configuration actionConf) throws Exception {
        JobConf jobConf = new JobConf();

    @Override
    protected RunningJob submitJob(JobConf jobConf) throws Exception {
         jobConf.set("mapred.mapper.class", "org.apache.hadoop.streaming.PipeMapper");
         jobConf.set("mapred.reducer.class", "org.apache.hadoop.streaming.PipeReducer");
         jobConf.set("mapred.map.runner.class", "org.apache.hadoop.streaming.PipeMapRunner");
@@ -46,24 +45,24 @@ public class StreamingMain extends MapReduceMain {
 
         jobConf.set("stream.addenvironment", "");
 
        String value = actionConf.get("oozie.streaming.mapper");
        String value = jobConf.get("oozie.streaming.mapper");
         if (value != null) {
             jobConf.set("stream.map.streamprocessor", value);
         }
        value = actionConf.get("oozie.streaming.reducer");
        value = jobConf.get("oozie.streaming.reducer");
         if (value != null) {
             jobConf.set("stream.reduce.streamprocessor", value);
         }
        value = actionConf.get("oozie.streaming.record-reader");
        value = jobConf.get("oozie.streaming.record-reader");
         if (value != null) {
             jobConf.set("stream.recordreader.class", value);
         }
        String[] values = getStrings(actionConf, "oozie.streaming.record-reader-mapping");
        String[] values = getStrings(jobConf, "oozie.streaming.record-reader-mapping");
         for (String s : values) {
             String[] kv = s.split("=");
             jobConf.set("stream.recordreader." + kv[0], kv[1]);
         }
        values = getStrings(actionConf, "oozie.streaming.env");
        values = getStrings(jobConf, "oozie.streaming.env");
         value = jobConf.get("stream.addenvironment", "");
         if (value.length() > 0) {
             value = value + " ";
@@ -73,8 +72,6 @@ public class StreamingMain extends MapReduceMain {
         }
         jobConf.set("stream.addenvironment", value);
 
        addActionConf(jobConf, actionConf);

         // propagate delegation related props from launcher job to MR job
         if (getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION") != null) {
             jobConf.set("mapreduce.job.credentials.binary", getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION"));
- 
2.19.1.windows.1

