From 746be5c2917310ab7202767a1a6bb2c79ad26dd6 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Mon, 2 Mar 2015 15:03:01 -0800
Subject: [PATCH] OOZIE-2158 Overrides in action conf in streaming action do
 not work (rohini)

--
 release-log.txt                               |  1 +
 .../apache/oozie/action/hadoop/PipesMain.java | 20 ++++---
 .../oozie/action/hadoop/StreamingMain.java    | 21 ++++---
 .../hadoop/TestMapReduceActionExecutor.java   | 60 +++++++++++++++++--
 4 files changed, 82 insertions(+), 20 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index ca6dfc2fd..0851d1c47 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-2158 Overrides in action conf in streaming action do not work (rohini)
 OOZIE-2042 Max substitution for config variables should be configurable (seoeun25 via puru)
 OOZIE-1913 Devise a way to turn off SLA alerts for bundle/coordinator flexibly (puru)
 OOZIE-2071 Add a Spark example (pavan kumar via rkanter)
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
index bf91b4327..5b5e9db22 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/PipesMain.java
@@ -32,32 +32,32 @@ public class PipesMain extends MapReduceMain {
     }
 
     @Override
    protected RunningJob submitJob(JobConf jobConf) throws Exception {
        String value = jobConf.get("oozie.pipes.map");
    protected void addActionConf(JobConf jobConf, Configuration actionConf) {
        String value = actionConf.get("oozie.pipes.map");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.mapper", true);
             jobConf.set("mapred.mapper.class", value);
         }
        value = jobConf.get("oozie.pipes.reduce");
        value = actionConf.get("oozie.pipes.reduce");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.reducer", true);
             jobConf.set("mapred.reducer.class", value);
         }
        value = jobConf.get("oozie.pipes.inputformat");
        value = actionConf.get("oozie.pipes.inputformat");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.recordreader", true);
             jobConf.set("mapred.input.format.class", value);
         }
        value = jobConf.get("oozie.pipes.partitioner");
        value = actionConf.get("oozie.pipes.partitioner");
         if (value != null) {
             jobConf.set("mapred.partitioner.class", value);
         }
        value = jobConf.get("oozie.pipes.writer");
        value = actionConf.get("oozie.pipes.writer");
         if (value != null) {
             jobConf.setBoolean("hadoop.pipes.java.recordwriter", true);
             jobConf.set("mapred.output.format.class", value);
         }
        value = jobConf.get("oozie.pipes.program");
        value = actionConf.get("oozie.pipes.program");
         if (value != null) {
             jobConf.set("hadoop.pipes.executable", value);
             if (value.contains("#")) {
@@ -65,6 +65,12 @@ public class PipesMain extends MapReduceMain {
             }
         }
 
        super.addActionConf(jobConf, actionConf);
    }

    @Override
    protected RunningJob submitJob(JobConf jobConf) throws Exception {

         //propagate delegation related props from launcher job to MR job
         if (getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION") != null) {
             jobConf.set("mapreduce.job.credentials.binary", getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION"));
diff --git a/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java b/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java
index c65c8594d..991bf7e84 100644
-- a/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java
++ b/sharelib/streaming/src/main/java/org/apache/oozie/action/hadoop/StreamingMain.java
@@ -29,8 +29,9 @@ public class StreamingMain extends MapReduceMain {
         run(StreamingMain.class, args);
     }
 

     @Override
    protected RunningJob submitJob(JobConf jobConf) throws Exception {
    protected void addActionConf(JobConf jobConf, Configuration actionConf) {
         jobConf.set("mapred.mapper.class", "org.apache.hadoop.streaming.PipeMapper");
         jobConf.set("mapred.reducer.class", "org.apache.hadoop.streaming.PipeReducer");
         jobConf.set("mapred.map.runner.class", "org.apache.hadoop.streaming.PipeMapRunner");
@@ -43,26 +44,24 @@ public class StreamingMain extends MapReduceMain {
         jobConf.set("mapred.create.symlink", "yes");
         jobConf.set("mapred.used.genericoptionsparser", "true");
 
        jobConf.set("stream.addenvironment", "");

        String value = jobConf.get("oozie.streaming.mapper");
        String value = actionConf.get("oozie.streaming.mapper");
         if (value != null) {
             jobConf.set("stream.map.streamprocessor", value);
         }
        value = jobConf.get("oozie.streaming.reducer");
        value = actionConf.get("oozie.streaming.reducer");
         if (value != null) {
             jobConf.set("stream.reduce.streamprocessor", value);
         }
        value = jobConf.get("oozie.streaming.record-reader");
        value = actionConf.get("oozie.streaming.record-reader");
         if (value != null) {
             jobConf.set("stream.recordreader.class", value);
         }
        String[] values = getStrings(jobConf, "oozie.streaming.record-reader-mapping");
        String[] values = getStrings(actionConf, "oozie.streaming.record-reader-mapping");
         for (String s : values) {
             String[] kv = s.split("=");
             jobConf.set("stream.recordreader." + kv[0], kv[1]);
         }
        values = getStrings(jobConf, "oozie.streaming.env");
        values = getStrings(actionConf, "oozie.streaming.env");
         value = jobConf.get("stream.addenvironment", "");
         if (value.length() > 0) {
             value = value + " ";
@@ -72,6 +71,12 @@ public class StreamingMain extends MapReduceMain {
         }
         jobConf.set("stream.addenvironment", value);
 
        super.addActionConf(jobConf, actionConf);
    }

    @Override
    protected RunningJob submitJob(JobConf jobConf) throws Exception {

         // propagate delegation related props from launcher job to MR job
         if (getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION") != null) {
             jobConf.set("mapreduce.job.credentials.binary", getFilePathFromEnv("HADOOP_TOKEN_FILE_LOCATION"));
diff --git a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
index 3204c00c6..d4095dafd 100644
-- a/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
++ b/sharelib/streaming/src/test/java/org/apache/oozie/action/hadoop/TestMapReduceActionExecutor.java
@@ -20,8 +20,10 @@ package org.apache.oozie.action.hadoop;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.PathFilter;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.JobClient;
 import org.apache.hadoop.mapred.RunningJob;
@@ -45,7 +47,9 @@ import org.apache.oozie.util.ClassUtils;
 import org.jdom.Element;
 
 import java.io.File;
import java.io.FileNotFoundException;
 import java.io.FileWriter;
import java.io.IOException;
 import java.io.OutputStream;
 import java.io.InputStream;
 import java.io.FileInputStream;
@@ -55,6 +59,7 @@ import java.io.OutputStreamWriter;
 import java.io.StringReader;
 import java.net.URI;
 import java.util.Arrays;
import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Scanner;
@@ -63,6 +68,8 @@ import java.util.regex.Pattern;
 import java.util.zip.ZipEntry;
 
 import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.util.PropertiesUtils;
 
@@ -799,7 +806,7 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         return conf;
     }
 
    public void testStreaming() throws Exception {
    private void runStreamingWordCountJob(Path inputDir, Path outputDir, XConfiguration streamingConf) throws Exception {
         FileSystem fs = getFileSystem();
         Path streamingJar = new Path(getFsTestCaseDir(), "jar/hadoop-streaming.jar");
 
@@ -807,9 +814,6 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         OutputStream os = fs.create(new Path(getAppPath(), streamingJar));
         IOUtils.copyStream(is, os);
 
        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

         Writer w = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")));
         w.write("dummy\n");
         w.write("dummy\n");
@@ -818,11 +822,57 @@ public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
         String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                 + getNameNodeUri() + "</name-node>" + "      <streaming>" + "        <mapper>cat</mapper>"
                 + "        <reducer>wc</reducer>" + "      </streaming>"
                + getStreamingConfig(inputDir.toString(), outputDir.toString()).toXmlString(false) + "<file>"
                + streamingConf.toXmlString(false) + "<file>"
                 + streamingJar + "</file>" + "</map-reduce>";
         _testSubmit("streaming", actionXml);
     }
 
    public void testStreaming() throws Exception {
        FileSystem fs = getFileSystem();
        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");
        final XConfiguration streamingConf = getStreamingConfig(inputDir.toString(), outputDir.toString());

        runStreamingWordCountJob(inputDir, outputDir, streamingConf);

        final FSDataInputStream dis = fs.open(getOutputFile(outputDir, fs));
        final List<String> lines = org.apache.commons.io.IOUtils.readLines(dis);
        dis.close();
        assertEquals(1, lines.size());
        // Not sure why it is 14 instead of 12. \n twice ??
        assertEquals("2       2      14", lines.get(0).trim());
    }

    public void testStreamingConfOverride() throws Exception {
        FileSystem fs = getFileSystem();
        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");
        final XConfiguration streamingConf = getStreamingConfig(inputDir.toString(), outputDir.toString());
        streamingConf.set("mapred.output.format.class", "org.apache.hadoop.mapred.SequenceFileOutputFormat");

        runStreamingWordCountJob(inputDir, outputDir, streamingConf);

        SequenceFile.Reader seqFile = new SequenceFile.Reader(fs, getOutputFile(outputDir, fs), getFileSystem().getConf());
        Text key = new Text(), value = new Text();
        if (seqFile.next(key, value)) {
            assertEquals("2       2      14", key.toString().trim());
            assertEquals("", value.toString());
        }
        assertFalse(seqFile.next(key, value));
        seqFile.close();
    }

    private Path getOutputFile(Path outputDir, FileSystem fs) throws FileNotFoundException, IOException {
        final FileStatus[] files = fs.listStatus(outputDir, new PathFilter() {

            @Override
            public boolean accept(Path path) {
                return path.getName().startsWith("part");
            }
        });
        return files[0].getPath(); //part-[m/r]-00000
    }

     protected XConfiguration getPipesConfig(String inputDir, String outputDir) {
         XConfiguration conf = new XConfiguration();
         conf.setBoolean("hadoop.pipes.java.recordreader", true);
- 
2.19.1.windows.1

