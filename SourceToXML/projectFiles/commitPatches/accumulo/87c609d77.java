From 87c609d77f82a3ec0b9fc3d37690c81464d982af Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 24 Jul 2014 13:51:19 -0400
Subject: [PATCH] ACCUMULO-3013 Inline reflection for Counter increment within
 ContinuousVerify

In 1.6, the CounterUtils class in server no longer exists, in favor of the
inlined method that was done in this commit. This alleviates modification
of the continuous-ingest scripts to launch jobs.

The CounterUtils class is still referenced by another class in server, but
both were also removed in 1.6 so they are not modified at all.
--
 .../test/continuous/ContinuousMoru.java       |  3 +-
 .../test/continuous/ContinuousVerify.java     | 30 +++++++++++++++----
 .../accumulo/test/functional/RunTests.java    |  5 ++--
 3 files changed, 29 insertions(+), 9 deletions(-)

diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java
index 0d52f129c..b7f2193b9 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousMoru.java
@@ -32,7 +32,6 @@ import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.ColumnVisibility;
 import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.server.util.reflection.CounterUtils;
 import org.apache.accumulo.test.continuous.ContinuousIngest.BaseOpts;
 import org.apache.accumulo.test.continuous.ContinuousIngest.ShortConverter;
 import org.apache.hadoop.conf.Configuration;
@@ -111,7 +110,7 @@ public class ContinuousMoru extends Configured implements Tool {
         }
         
       } else {
        CounterUtils.increment(context.getCounter(Counts.SELF_READ));
        ContinuousVerify.increment(context.getCounter(Counts.SELF_READ));
       }
     }
   }
diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java
index ebeee8ac8..f31967806 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java
@@ -17,6 +17,7 @@
 package org.apache.accumulo.test.continuous;
 
 import java.io.IOException;
import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
@@ -31,13 +32,13 @@ import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.server.util.reflection.CounterUtils;
 import org.apache.accumulo.test.continuous.ContinuousWalk.BadChecksumException;
 import org.apache.hadoop.conf.Configured;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.LongWritable;
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.io.VLongWritable;
import org.apache.hadoop.mapred.Counters.Counter;
 import org.apache.hadoop.mapreduce.Job;
 import org.apache.hadoop.mapreduce.Mapper;
 import org.apache.hadoop.mapreduce.Reducer;
@@ -54,6 +55,25 @@ import com.beust.jcommander.validators.PositiveInteger;
  */
 
 public class ContinuousVerify extends Configured implements Tool {
  // work around hadoop-1/hadoop-2 runtime incompatibility
  static private Method INCREMENT;

  static {
    try {
      INCREMENT = Counter.class.getMethod("increment", Long.TYPE);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void increment(Object obj) {
    try {
      INCREMENT.invoke(obj, 1L);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

   public static final VLongWritable DEF = new VLongWritable(-1);
 
   public static class CMapper extends Mapper<Key,Value,LongWritable,VLongWritable> {
@@ -74,7 +94,7 @@ public class ContinuousVerify extends Configured implements Tool {
       try {
         ContinuousWalk.validate(key, data);
       } catch (BadChecksumException bce) {
        CounterUtils.increment(context.getCounter(Counts.CORRUPT));
        increment(context.getCounter(Counts.CORRUPT));
         if (corrupt < 1000) {
           log.error("Bad checksum : " + key);
         } else if (corrupt == 1000) {
@@ -129,12 +149,12 @@ public class ContinuousVerify extends Configured implements Tool {
         }
 
         context.write(new Text(ContinuousIngest.genRow(key.get())), new Text(sb.toString()));
        CounterUtils.increment(context.getCounter(Counts.UNDEFINED));
        increment(context.getCounter(Counts.UNDEFINED));
 
       } else if (defCount > 0 && refs.size() == 0) {
        CounterUtils.increment(context.getCounter(Counts.UNREFERENCED));
        increment(context.getCounter(Counts.UNREFERENCED));
       } else {
        CounterUtils.increment(context.getCounter(Counts.REFERENCED));
        increment(context.getCounter(Counts.REFERENCED));
       }
 
     }
diff --git a/test/src/main/java/org/apache/accumulo/test/functional/RunTests.java b/test/src/main/java/org/apache/accumulo/test/functional/RunTests.java
index 06c6fdbf6..adca2ce35 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/RunTests.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/RunTests.java
@@ -27,13 +27,14 @@ import java.util.Map;
 
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.server.util.reflection.CounterUtils;
import org.apache.accumulo.test.continuous.ContinuousVerify;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.conf.Configured;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.LongWritable;
 import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Counters.Counter;
 import org.apache.hadoop.mapreduce.Job;
 import org.apache.hadoop.mapreduce.Mapper;
 import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
@@ -132,7 +133,7 @@ public class RunTests extends Configured implements Tool {
             if (resultLine.length() > 0) {
               Outcome outcome = OUTCOME_COUNTERS.get(resultLine.charAt(0));
               if (outcome != null) {
                CounterUtils.increment(context.getCounter(outcome));
                ContinuousVerify.increment(context.getCounter(outcome));
               }
             }
             String taskAttemptId = context.getTaskAttemptID().toString();
- 
2.19.1.windows.1

