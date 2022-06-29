From c9c0d45b52a244b90dda31ba210c590b9af2f9f4 Mon Sep 17 00:00:00 2001
From: Adam Fuchs <afuchs@apache.org>
Date: Thu, 16 May 2013 15:49:33 +0000
Subject: [PATCH] ACCUMULO-1421 added reflection to improve runtime
 compatibility between hadoop 1 and 2 JobContext objects

git-svn-id: https://svn.apache.org/repos/asf/accumulo/branches/1.5@1483417 13f79535-47bb-0310-9956-ffa450edef68
--
 .../mapreduce/AccumuloFileOutputFormat.java   |  4 +-
 .../mapreduce/AccumuloOutputFormat.java       | 20 ++++----
 .../client/mapreduce/InputFormatBase.java     | 47 ++++++++++++-------
 3 files changed, 42 insertions(+), 29 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
index 5e92b74c7..cfcefda8c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
@@ -65,7 +65,7 @@ public class AccumuloFileOutputFormat extends FileOutputFormat<Key,Value> {
    * @since 1.5.0
    */
   protected static AccumuloConfiguration getAccumuloConfiguration(JobContext context) {
    return FileOutputConfigurator.getAccumuloConfiguration(CLASS, context.getConfiguration());
    return FileOutputConfigurator.getAccumuloConfiguration(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -141,7 +141,7 @@ public class AccumuloFileOutputFormat extends FileOutputFormat<Key,Value> {
   @Override
   public RecordWriter<Key,Value> getRecordWriter(TaskAttemptContext context) throws IOException {
     // get the path of the temporary output file
    final Configuration conf = context.getConfiguration();
    final Configuration conf = InputFormatBase.getConfiguration(context);
     final AccumuloConfiguration acuConf = getAccumuloConfiguration(context);
     
     final String extension = acuConf.get(Property.TABLE_FILE_TYPE);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
index 9b768e36f..9c022190c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloOutputFormat.java
@@ -104,7 +104,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static Boolean isConnectorInfoSet(JobContext context) {
    return OutputConfigurator.isConnectorInfoSet(CLASS, context.getConfiguration());
    return OutputConfigurator.isConnectorInfoSet(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -117,7 +117,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static String getPrincipal(JobContext context) {
    return OutputConfigurator.getPrincipal(CLASS, context.getConfiguration());
    return OutputConfigurator.getPrincipal(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -130,7 +130,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static String getTokenClass(JobContext context) {
    return OutputConfigurator.getTokenClass(CLASS, context.getConfiguration());
    return OutputConfigurator.getTokenClass(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -144,7 +144,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static byte[] getToken(JobContext context) {
    return OutputConfigurator.getToken(CLASS, context.getConfiguration());
    return OutputConfigurator.getToken(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -186,7 +186,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setMockInstance(Job, String)
    */
   protected static Instance getInstance(JobContext context) {
    return OutputConfigurator.getInstance(CLASS, context.getConfiguration());
    return OutputConfigurator.getInstance(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -212,7 +212,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setLogLevel(Job, Level)
    */
   protected static Level getLogLevel(JobContext context) {
    return OutputConfigurator.getLogLevel(CLASS, context.getConfiguration());
    return OutputConfigurator.getLogLevel(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -239,7 +239,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setDefaultTableName(Job, String)
    */
   protected static String getDefaultTableName(JobContext context) {
    return OutputConfigurator.getDefaultTableName(CLASS, context.getConfiguration());
    return OutputConfigurator.getDefaultTableName(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -266,7 +266,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setBatchWriterOptions(Job, BatchWriterConfig)
    */
   protected static BatchWriterConfig getBatchWriterOptions(JobContext context) {
    return OutputConfigurator.getBatchWriterOptions(CLASS, context.getConfiguration());
    return OutputConfigurator.getBatchWriterOptions(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -295,7 +295,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setCreateTables(Job, boolean)
    */
   protected static Boolean canCreateTables(JobContext context) {
    return OutputConfigurator.canCreateTables(CLASS, context.getConfiguration());
    return OutputConfigurator.canCreateTables(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
@@ -324,7 +324,7 @@ public class AccumuloOutputFormat extends OutputFormat<Text,Mutation> {
    * @see #setSimulationMode(Job, boolean)
    */
   protected static Boolean getSimulationMode(JobContext context) {
    return OutputConfigurator.getSimulationMode(CLASS, context.getConfiguration());
    return OutputConfigurator.getSimulationMode(CLASS, InputFormatBase.getConfiguration(context));
   }
   
   /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
index 6243d3ccf..1833bea31 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
@@ -20,6 +20,7 @@ import java.io.DataInput;
 import java.io.DataOutput;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
 import java.math.BigInteger;
 import java.net.InetAddress;
 import java.net.URLDecoder;
@@ -126,7 +127,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static Boolean isConnectorInfoSet(JobContext context) {
    return InputConfigurator.isConnectorInfoSet(CLASS, context.getConfiguration());
    return InputConfigurator.isConnectorInfoSet(CLASS, getConfiguration(context));
   }
   
   /**
@@ -139,7 +140,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static String getPrincipal(JobContext context) {
    return InputConfigurator.getPrincipal(CLASS, context.getConfiguration());
    return InputConfigurator.getPrincipal(CLASS, getConfiguration(context));
   }
   
   /**
@@ -152,7 +153,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static String getTokenClass(JobContext context) {
    return InputConfigurator.getTokenClass(CLASS, context.getConfiguration());
    return InputConfigurator.getTokenClass(CLASS, getConfiguration(context));
   }
   
   /**
@@ -166,7 +167,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setConnectorInfo(Job, String, AuthenticationToken)
    */
   protected static byte[] getToken(JobContext context) {
    return InputConfigurator.getToken(CLASS, context.getConfiguration());
    return InputConfigurator.getToken(CLASS, getConfiguration(context));
   }
   
   /**
@@ -208,7 +209,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setMockInstance(Job, String)
    */
   protected static Instance getInstance(JobContext context) {
    return InputConfigurator.getInstance(CLASS, context.getConfiguration());
    return InputConfigurator.getInstance(CLASS, getConfiguration(context));
   }
   
   /**
@@ -234,7 +235,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setLogLevel(Job, Level)
    */
   protected static Level getLogLevel(JobContext context) {
    return InputConfigurator.getLogLevel(CLASS, context.getConfiguration());
    return InputConfigurator.getLogLevel(CLASS, getConfiguration(context));
   }
   
   /**
@@ -260,7 +261,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setInputTableName(Job, String)
    */
   protected static String getInputTableName(JobContext context) {
    return InputConfigurator.getInputTableName(CLASS, context.getConfiguration());
    return InputConfigurator.getInputTableName(CLASS, getConfiguration(context));
   }
   
   /**
@@ -286,7 +287,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setScanAuthorizations(Job, Authorizations)
    */
   protected static Authorizations getScanAuthorizations(JobContext context) {
    return InputConfigurator.getScanAuthorizations(CLASS, context.getConfiguration());
    return InputConfigurator.getScanAuthorizations(CLASS, getConfiguration(context));
   }
   
   /**
@@ -314,7 +315,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setRanges(Job, Collection)
    */
   protected static List<Range> getRanges(JobContext context) throws IOException {
    return InputConfigurator.getRanges(CLASS, context.getConfiguration());
    return InputConfigurator.getRanges(CLASS, getConfiguration(context));
   }
   
   /**
@@ -341,7 +342,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #fetchColumns(Job, Collection)
    */
   protected static Set<Pair<Text,Text>> getFetchedColumns(JobContext context) {
    return InputConfigurator.getFetchedColumns(CLASS, context.getConfiguration());
    return InputConfigurator.getFetchedColumns(CLASS, getConfiguration(context));
   }
   
   /**
@@ -367,7 +368,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #addIterator(Job, IteratorSetting)
    */
   protected static List<IteratorSetting> getIterators(JobContext context) {
    return InputConfigurator.getIterators(CLASS, context.getConfiguration());
    return InputConfigurator.getIterators(CLASS, getConfiguration(context));
   }
   
   /**
@@ -398,7 +399,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setAutoAdjustRanges(Job, boolean)
    */
   protected static boolean getAutoAdjustRanges(JobContext context) {
    return InputConfigurator.getAutoAdjustRanges(CLASS, context.getConfiguration());
    return InputConfigurator.getAutoAdjustRanges(CLASS, getConfiguration(context));
   }
   
   /**
@@ -427,7 +428,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setScanIsolation(Job, boolean)
    */
   protected static boolean isIsolated(JobContext context) {
    return InputConfigurator.isIsolated(CLASS, context.getConfiguration());
    return InputConfigurator.isIsolated(CLASS, getConfiguration(context));
   }
   
   /**
@@ -457,7 +458,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setLocalIterators(Job, boolean)
    */
   protected static boolean usesLocalIterators(JobContext context) {
    return InputConfigurator.usesLocalIterators(CLASS, context.getConfiguration());
    return InputConfigurator.usesLocalIterators(CLASS, getConfiguration(context));
   }
   
   /**
@@ -505,7 +506,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @see #setOfflineTableScan(Job, boolean)
    */
   protected static boolean isOfflineScan(JobContext context) {
    return InputConfigurator.isOfflineScan(CLASS, context.getConfiguration());
    return InputConfigurator.isOfflineScan(CLASS, getConfiguration(context));
   }
   
   /**
@@ -519,7 +520,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @since 1.5.0
    */
   protected static TabletLocator getTabletLocator(JobContext context) throws TableNotFoundException {
    return InputConfigurator.getTabletLocator(CLASS, context.getConfiguration());
    return InputConfigurator.getTabletLocator(CLASS, getConfiguration(context));
   }
   
   // InputFormat doesn't have the equivalent of OutputFormat's checkOutputSpecs(JobContext job)
@@ -533,7 +534,7 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
    * @since 1.5.0
    */
   protected static void validateOptions(JobContext context) throws IOException {
    InputConfigurator.validateOptions(CLASS, context.getConfiguration());
    InputConfigurator.validateOptions(CLASS, getConfiguration(context));
   }
   
   /**
@@ -1341,5 +1342,17 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
     }
     
   }

  // use reflection to pull the Configuration out of the JobContext for Hadoop 1 and Hadoop 2 compatibility
  public static Configuration getConfiguration(JobContext context) {
    try {
      Class c = InputFormatBase.class.getClassLoader().loadClass("org.apache.hadoop.mapreduce.JobContext");
      Method m = c.getMethod("getConfiguration");
      Object o = m.invoke(context, new Object[0]);
      return (Configuration)o;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
   
 }
- 
2.19.1.windows.1

