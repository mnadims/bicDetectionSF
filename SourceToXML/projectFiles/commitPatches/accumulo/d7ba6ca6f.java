From d7ba6ca6f26c5ffc280a8b18d60e0c264910540d Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Thu, 16 May 2013 16:20:21 +0000
Subject: [PATCH] ACCUMULO-1421 fix warnings introduced by previous commits

git-svn-id: https://svn.apache.org/repos/asf/accumulo/branches/1.5@1483435 13f79535-47bb-0310-9956-ffa450edef68
--
 .../accumulo/core/client/mapreduce/InputFormatBase.java  | 9 +++++----
 .../main/java/org/apache/accumulo/server/Accumulo.java   | 7 +++----
 2 files changed, 8 insertions(+), 8 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
index 1833bea31..ca1784df5 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
@@ -785,7 +785,8 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
         // its possible that the cache could contain complete, but old information about a tables tablets... so clear it
         tl.invalidateCache();
         while (!tl.binRanges(ranges, binnedRanges,
            new TCredentials(getPrincipal(context), getTokenClass(context), ByteBuffer.wrap(getToken(context)), getInstance(context).getInstanceID())).isEmpty()) {
            new TCredentials(getPrincipal(context), getTokenClass(context), ByteBuffer.wrap(getToken(context)), getInstance(context).getInstanceID()))
            .isEmpty()) {
           if (!(instance instanceof MockInstance)) {
             if (tableId == null)
               tableId = Tables.getTableId(instance, tableName);
@@ -1342,14 +1343,14 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
     }
     
   }

  
   // use reflection to pull the Configuration out of the JobContext for Hadoop 1 and Hadoop 2 compatibility
   public static Configuration getConfiguration(JobContext context) {
     try {
      Class c = InputFormatBase.class.getClassLoader().loadClass("org.apache.hadoop.mapreduce.JobContext");
      Class<?> c = InputFormatBase.class.getClassLoader().loadClass("org.apache.hadoop.mapreduce.JobContext");
       Method m = c.getMethod("getConfiguration");
       Object o = m.invoke(context, new Object[0]);
      return (Configuration)o;
      return (Configuration) o;
     } catch (Exception e) {
       throw new RuntimeException(e);
     }
diff --git a/server/src/main/java/org/apache/accumulo/server/Accumulo.java b/server/src/main/java/org/apache/accumulo/server/Accumulo.java
index fb196af51..2e7f12bc2 100644
-- a/server/src/main/java/org/apache/accumulo/server/Accumulo.java
++ b/server/src/main/java/org/apache/accumulo/server/Accumulo.java
@@ -40,7 +40,6 @@ import org.apache.hadoop.fs.FileStatus;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.FSConstants;
 import org.apache.log4j.Logger;
 import org.apache.log4j.helpers.LogLog;
 import org.apache.log4j.xml.DOMConfigurator;
@@ -212,12 +211,12 @@ public class Accumulo {
     }
     log.info("Connected to HDFS");
   }

  
   private static boolean isInSafeMode(FileSystem fs) throws IOException {
     if (!(fs instanceof DistributedFileSystem))
       return false;
     DistributedFileSystem dfs = (DistributedFileSystem) FileSystem.get(CachedConfiguration.getInstance());
    // So this:  if (!dfs.setSafeMode(SafeModeAction.SAFEMODE_GET))
    // So this: if (!dfs.setSafeMode(SafeModeAction.SAFEMODE_GET))
     // Becomes this:
     Class<?> constantClass;
     try {
@@ -252,7 +251,7 @@ public class Accumulo {
     }
     try {
       Method setSafeMode = dfs.getClass().getMethod("setSafeMode", safeModeAction);
      return (Boolean)setSafeMode.invoke(dfs, get);
      return (Boolean) setSafeMode.invoke(dfs, get);
     } catch (Exception ex) {
       throw new RuntimeException("cannot find method setSafeMode");
     }
- 
2.19.1.windows.1

