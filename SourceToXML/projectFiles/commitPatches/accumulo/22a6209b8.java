From 22a6209b8bf579eeadf337c58d4f45fbfb87c8e4 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Sun, 30 Mar 2014 13:49:52 -0400
Subject: [PATCH] ACCUMULO-2586 Add in a deprecated RangeInputSplit to replace
 the 1.5.0 structure

RangeInputSplit doesn't need to be duplicative, but 1.5.0 had it as such, so
it should also be in all of 1.5
--
 .../core/client/mapred/InputFormatBase.java   | 26 ++++++++++++++-----
 .../client/mapreduce/InputFormatBase.java     | 24 +++++++++++++----
 .../BadPasswordSplitsAccumuloInputFormat.java |  2 +-
 3 files changed, 40 insertions(+), 12 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
index ae361de2b..0438b78a4 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
@@ -536,7 +536,7 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
   protected abstract static class RecordReaderBase<K,V> implements RecordReader<K,V> {
     protected long numKeysRead;
     protected Iterator<Entry<Key,Value>> scannerIterator;
    protected RangeInputSplit split;
    protected org.apache.accumulo.core.client.mapred.RangeInputSplit split;
 
     /**
      * Apply the configured iterators from the configuration to the scanner.
@@ -555,7 +555,7 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
      */
     public void initialize(InputSplit inSplit, JobConf job) throws IOException {
       Scanner scanner;
      split = (RangeInputSplit) inSplit;
      split = (org.apache.accumulo.core.client.mapred.RangeInputSplit) inSplit;
       log.debug("Initializing input split: " + split.getRange());
 
       Instance instance = split.getInstance();
@@ -849,7 +849,8 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
       throw new IOException(e);
     }
 
    ArrayList<RangeInputSplit> splits = new ArrayList<RangeInputSplit>(ranges.size());
    ArrayList<org.apache.accumulo.core.client.mapred.RangeInputSplit> splits = new ArrayList<org.apache.accumulo.core.client.mapred.RangeInputSplit>(
        ranges.size());
     HashMap<Range,ArrayList<String>> splitsToAdd = null;
 
     if (!autoAdjust)
@@ -871,7 +872,7 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
         for (Range r : extentRanges.getValue()) {
           if (autoAdjust) {
             // divide ranges into smaller ranges, based on the tablets
            splits.add(new RangeInputSplit(ke.clip(r), new String[] {location}));
            splits.add(new org.apache.accumulo.core.client.mapred.RangeInputSplit(ke.clip(r), new String[] {location}));
           } else {
             // don't divide ranges
             ArrayList<String> locations = splitsToAdd.get(r);
@@ -886,9 +887,9 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
 
     if (!autoAdjust)
       for (Entry<Range,ArrayList<String>> entry : splitsToAdd.entrySet())
        splits.add(new RangeInputSplit(entry.getKey(), entry.getValue().toArray(new String[0])));
        splits.add(new org.apache.accumulo.core.client.mapred.RangeInputSplit(entry.getKey(), entry.getValue().toArray(new String[0])));
 
    for (RangeInputSplit split : splits) {
    for (org.apache.accumulo.core.client.mapred.RangeInputSplit split : splits) {
       split.setTable(tableName);
       split.setOffline(offline);
       split.setIsolatedScan(isolated);
@@ -907,4 +908,17 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
     return splits.toArray(new InputSplit[splits.size()]);
   }
 
  /**
   * @see org.apache.accumulo.core.client.mapred.RangeInputSplit
   */
  @Deprecated
  public static class RangeInputSplit extends org.apache.accumulo.core.client.mapred.RangeInputSplit {
    public RangeInputSplit() {
      super();
    }

    public RangeInputSplit(Range range, String[] locations) {
      super(range, locations);
    }
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
index 451617616..506662047 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
@@ -547,7 +547,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
   protected abstract static class RecordReaderBase<K,V> extends RecordReader<K,V> {
     protected long numKeysRead;
     protected Iterator<Entry<Key,Value>> scannerIterator;
    protected RangeInputSplit split;
    protected org.apache.accumulo.core.client.mapreduce.RangeInputSplit split;
 
     /**
      * Apply the configured iterators from the configuration to the scanner.
@@ -567,7 +567,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
     @Override
     public void initialize(InputSplit inSplit, TaskAttemptContext attempt) throws IOException {
       Scanner scanner;
      split = (RangeInputSplit) inSplit;
      split = (org.apache.accumulo.core.client.mapreduce.RangeInputSplit) inSplit;
       log.debug("Initializing input split: " + split.getRange());
 
       Instance instance = split.getInstance();
@@ -890,7 +890,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
         for (Range r : extentRanges.getValue()) {
           if (autoAdjust) {
             // divide ranges into smaller ranges, based on the tablets
            splits.add(new RangeInputSplit(ke.clip(r), new String[] {location}));
            splits.add(new org.apache.accumulo.core.client.mapreduce.RangeInputSplit(ke.clip(r), new String[] {location}));
           } else {
             // don't divide ranges
             ArrayList<String> locations = splitsToAdd.get(r);
@@ -905,10 +905,10 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
 
     if (!autoAdjust)
       for (Entry<Range,ArrayList<String>> entry : splitsToAdd.entrySet())
        splits.add(new RangeInputSplit(entry.getKey(), entry.getValue().toArray(new String[0])));
        splits.add(new org.apache.accumulo.core.client.mapreduce.RangeInputSplit(entry.getKey(), entry.getValue().toArray(new String[0])));
 
     for (InputSplit inputSplit : splits) {
      RangeInputSplit split = (RangeInputSplit) inputSplit;
      org.apache.accumulo.core.client.mapreduce.RangeInputSplit split = (org.apache.accumulo.core.client.mapreduce.RangeInputSplit) inputSplit;
 
       split.setTable(tableName);
       split.setOffline(offline);
@@ -1319,4 +1319,18 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
     }
   }
 
  /**
   * @see org.apache.accumulo.core.client.mapreduce.RangeInputSplit
   */
  @Deprecated
  public static class RangeInputSplit extends org.apache.accumulo.core.client.mapreduce.RangeInputSplit {

    public RangeInputSplit() {
      super();
    }

    public RangeInputSplit(Range range, String[] locations) {
      super(range, locations);
    }
  }
 }
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/BadPasswordSplitsAccumuloInputFormat.java b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/BadPasswordSplitsAccumuloInputFormat.java
index ee4233f09..fce7781c1 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/BadPasswordSplitsAccumuloInputFormat.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/BadPasswordSplitsAccumuloInputFormat.java
@@ -33,7 +33,7 @@ public class BadPasswordSplitsAccumuloInputFormat extends AccumuloInputFormat {
     List<InputSplit> splits = super.getSplits(context);
     
     for (InputSplit split : splits) {
      RangeInputSplit rangeSplit = (RangeInputSplit) split;
      org.apache.accumulo.core.client.mapreduce.RangeInputSplit rangeSplit = (org.apache.accumulo.core.client.mapreduce.RangeInputSplit) split;
       rangeSplit.setToken(new PasswordToken("anythingelse"));
     }
     
- 
2.19.1.windows.1

