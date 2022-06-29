From cc3c2d8a8fc16387deefa7bc47df6a90a1fc5fb6 Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <ecn@apache.org>
Date: Tue, 21 May 2013 19:13:43 +0000
Subject: [PATCH] ACCUMULO-1421 use reflection to work around class/interface
 change in Counter

git-svn-id: https://svn.apache.org/repos/asf/accumulo/branches/1.5@1484908 13f79535-47bb-0310-9956-ffa450edef68
--
 .../test/continuous/ContinuousVerify.java     | 30 +++++++++++++++----
 1 file changed, 25 insertions(+), 5 deletions(-)

diff --git a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java
index fd03706f0..e232de678 100644
-- a/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java
++ b/test/src/main/java/org/apache/accumulo/test/continuous/ContinuousVerify.java
@@ -17,6 +17,7 @@
 package org.apache.accumulo.test.continuous;
 
 import java.io.IOException;
import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
@@ -36,6 +37,7 @@ import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.LongWritable;
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.io.VLongWritable;
import org.apache.hadoop.mapred.Counters.Counter;
 import org.apache.hadoop.mapreduce.Job;
 import org.apache.hadoop.mapreduce.Mapper;
 import org.apache.hadoop.mapreduce.Reducer;
@@ -52,6 +54,24 @@ import com.beust.jcommander.validators.PositiveInteger;
 
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
  
  private static void increment(Object obj) {
    try {
      INCREMENT.invoke(obj, 1L);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
  
   public static final VLongWritable DEF = new VLongWritable(-1);
   
   public static class CMapper extends Mapper<Key,Value,LongWritable,VLongWritable> {
@@ -59,7 +79,7 @@ public class ContinuousVerify extends Configured implements Tool {
     private LongWritable row = new LongWritable();
     private LongWritable ref = new LongWritable();
     private VLongWritable vrow = new VLongWritable();
    

     private long corrupt = 0;
     
     @Override
@@ -71,7 +91,7 @@ public class ContinuousVerify extends Configured implements Tool {
       try {
         ContinuousWalk.validate(key, data);
       } catch (BadChecksumException bce) {
        context.getCounter(Counts.CORRUPT).increment(1);
        increment(context.getCounter(Counts.CORRUPT));
         if (corrupt < 1000) {
           System.out.println("ERROR Bad checksum : " + key);
         } else if (corrupt == 1000) {
@@ -126,12 +146,12 @@ public class ContinuousVerify extends Configured implements Tool {
         }
         
         context.write(new Text(ContinuousIngest.genRow(key.get())), new Text(sb.toString()));
        context.getCounter(Counts.UNDEFINED).increment(1);
        increment(context.getCounter(Counts.UNDEFINED));
         
       } else if (defCount > 0 && refs.size() == 0) {
        context.getCounter(Counts.UNREFERENCED).increment(1);
        increment(context.getCounter(Counts.UNREFERENCED));
       } else {
        context.getCounter(Counts.REFERENCED).increment(1);
        increment(context.getCounter(Counts.REFERENCED));
       }
       
     }
- 
2.19.1.windows.1

