From 42beb56a2ed0176bf0c47fe1b844f01d459130d1 Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Thu, 23 Aug 2012 15:24:46 +0000
Subject: [PATCH] HADOOP-8632. Configuration leaking class-loaders (Costin Leau
 via bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1376543 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  2 ++
 .../org/apache/hadoop/conf/Configuration.java | 20 ++++++++++++-------
 .../apache/hadoop/conf/TestConfiguration.java |  7 +++++++
 3 files changed, 22 insertions(+), 7 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index a9fef8e9803..ca47619fa89 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -426,6 +426,8 @@ Branch-2 ( Unreleased changes )
     HADOOP-8721. ZKFC should not retry 45 times when attempting a graceful
     fence during a failover. (Vinayakumar B via atm)
 
    HADOOP-8632. Configuration leaking class-loaders (Costin Leau via bobby)

   BREAKDOWN OF HDFS-3042 SUBTASKS
 
     HADOOP-8220. ZKFailoverController doesn't handle failure to become active
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
index 83ac3867494..e9b76609adf 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
@@ -30,6 +30,7 @@
 import java.io.OutputStreamWriter;
 import java.io.Reader;
 import java.io.Writer;
import java.lang.ref.WeakReference;
 import java.net.InetSocketAddress;
 import java.net.URL;
 import java.util.ArrayList;
@@ -219,8 +220,8 @@ public String toString() {
   private static final CopyOnWriteArrayList<String> defaultResources =
     new CopyOnWriteArrayList<String>();
 
  private static final Map<ClassLoader, Map<String, Class<?>>>
    CACHE_CLASSES = new WeakHashMap<ClassLoader, Map<String, Class<?>>>();
  private static final Map<ClassLoader, Map<String, WeakReference<Class<?>>>>
    CACHE_CLASSES = new WeakHashMap<ClassLoader, Map<String, WeakReference<Class<?>>>>();
 
   /**
    * Sentinel value to store negative cache results in {@link #CACHE_CLASSES}.
@@ -1531,28 +1532,33 @@ public InetSocketAddress updateConnectAddr(String name,
    * @return the class object, or null if it could not be found.
    */
   public Class<?> getClassByNameOrNull(String name) {
    Map<String, Class<?>> map;
    Map<String, WeakReference<Class<?>>> map;
     
     synchronized (CACHE_CLASSES) {
       map = CACHE_CLASSES.get(classLoader);
       if (map == null) {
         map = Collections.synchronizedMap(
          new WeakHashMap<String, Class<?>>());
          new WeakHashMap<String, WeakReference<Class<?>>>());
         CACHE_CLASSES.put(classLoader, map);
       }
     }
 
    Class<?> clazz = map.get(name);
    Class<?> clazz = null;
    WeakReference<Class<?>> ref = map.get(name); 
    if (ref != null) {
       clazz = ref.get();
    }
     
     if (clazz == null) {
       try {
         clazz = Class.forName(name, true, classLoader);
       } catch (ClassNotFoundException e) {
         // Leave a marker that the class isn't found
        map.put(name, NEGATIVE_CACHE_SENTINEL);
        map.put(name, new WeakReference<Class<?>>(NEGATIVE_CACHE_SENTINEL));
         return null;
       }
       // two putters can race here, but they'll put the same class
      map.put(name, clazz);
      map.put(name, new WeakReference<Class<?>>(clazz));
       return clazz;
     } else if (clazz == NEGATIVE_CACHE_SENTINEL) {
       return null; // not found
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
index 576d9210007..679ced34eec 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
@@ -39,6 +39,7 @@
 
 import junit.framework.TestCase;
 import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration.IntegerRanges;
@@ -1157,6 +1158,12 @@ public void testSetPattern() {
         configuration.getPattern("testPattern", Pattern.compile("")).pattern());
   }
   
  public void testGetClassByNameOrNull() throws Exception {
   Configuration config = new Configuration();
   Class<?> clazz = config.getClassByNameOrNull("java.lang.Object");
   assertNotNull(clazz);
  }
  
   public static void main(String[] argv) throws Exception {
     junit.textui.TestRunner.main(new String[]{
       TestConfiguration.class.getName()
- 
2.19.1.windows.1

