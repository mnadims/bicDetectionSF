From e33c1f4951faac982acc48f18a0e1256c1d11ea5 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Mon, 7 Apr 2014 20:06:31 -0400
Subject: [PATCH] ACCUMULO-2648 Make AccumuloInputFormat use correct
 RangeInputSplit

--
 .../apache/accumulo/core/client/mapred/InputFormatBase.java | 1 +
 .../accumulo/core/client/mapreduce/AccumuloInputFormat.java | 6 +++---
 .../accumulo/core/client/mapreduce/InputFormatBase.java     | 5 +++--
 .../client/mapreduce/EmptySplitsAccumuloInputFormat.java    | 6 +++---
 4 files changed, 10 insertions(+), 8 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
index 0438b78a4..16efa89b9 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
@@ -909,6 +909,7 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
   }
 
   /**
   * @deprecated since 1.5.2; Use {@link org.apache.accumulo.core.client.mapred.RangeInputSplit} instead.
    * @see org.apache.accumulo.core.client.mapred.RangeInputSplit
    */
   @Deprecated
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
index 0220339ac..c08b50b0c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloInputFormat.java
@@ -50,10 +50,10 @@ public class AccumuloInputFormat extends InputFormatBase<Key,Value> {
   @Override
   public RecordReader<Key,Value> createRecordReader(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
     log.setLevel(getLogLevel(context));
    

     // Override the log level from the configuration as if the RangeInputSplit has one it's the more correct one to use.
    if (split instanceof RangeInputSplit) {
      RangeInputSplit risplit = (RangeInputSplit) split;
    if (split instanceof org.apache.accumulo.core.client.mapreduce.RangeInputSplit) {
      org.apache.accumulo.core.client.mapreduce.RangeInputSplit risplit = (org.apache.accumulo.core.client.mapreduce.RangeInputSplit) split;
       Level level = risplit.getLogLevel();
       if (null != level) {
         log.setLevel(level);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
index 506662047..0e9444d20 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
@@ -1198,8 +1198,8 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
       for (Entry<String,String> opt : setting.getOptions().entrySet()) {
         String deprecatedOption;
         try {
          deprecatedOption = setting.getName() + AccumuloIteratorOption.FIELD_SEP + URLEncoder.encode(opt.getKey(), "UTF-8")
              + AccumuloIteratorOption.FIELD_SEP + URLEncoder.encode(opt.getValue(), "UTF-8");
          deprecatedOption = setting.getName() + AccumuloIteratorOption.FIELD_SEP + URLEncoder.encode(opt.getKey(), "UTF-8") + AccumuloIteratorOption.FIELD_SEP
              + URLEncoder.encode(opt.getValue(), "UTF-8");
         } catch (UnsupportedEncodingException e) {
           throw new RuntimeException(e);
         }
@@ -1320,6 +1320,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
   }
 
   /**
   * @deprecated since 1.5.2; Use {@link org.apache.accumulo.core.client.mapreduce.RangeInputSplit} instead.
    * @see org.apache.accumulo.core.client.mapreduce.RangeInputSplit
    */
   @Deprecated
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/EmptySplitsAccumuloInputFormat.java b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/EmptySplitsAccumuloInputFormat.java
index 440dbf7a6..68ac78f35 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/EmptySplitsAccumuloInputFormat.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/EmptySplitsAccumuloInputFormat.java
@@ -27,11 +27,11 @@ import org.apache.hadoop.mapreduce.JobContext;
  * AccumuloInputFormat which returns an "empty" RangeInputSplit
  */
 public class EmptySplitsAccumuloInputFormat extends AccumuloInputFormat {
  

   @Override
   public List<InputSplit> getSplits(JobContext context) throws IOException {
     super.getSplits(context);
    
    return Arrays.<InputSplit> asList(new RangeInputSplit());

    return Arrays.<InputSplit> asList(new org.apache.accumulo.core.client.mapreduce.RangeInputSplit());
   }
 }
- 
2.19.1.windows.1

