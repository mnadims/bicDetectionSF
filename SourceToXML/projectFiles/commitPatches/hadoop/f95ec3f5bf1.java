From f95ec3f5bf12bee07c90943cff3b135e6a7e7a8b Mon Sep 17 00:00:00 2001
From: Christopher Douglas <cdouglas@apache.org>
Date: Mon, 7 Sep 2009 21:54:45 +0000
Subject: [PATCH] HADOOP-6133. Add a caching layer to
 Configuration::getClassByName to alleviate a performance regression
 introduced in a compatibility layer. Contributed by Todd Lipcon

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@812285 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  7 +++++-
 .../org/apache/hadoop/conf/Configuration.java | 25 ++++++++++++++++++-
 2 files changed, 30 insertions(+), 2 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index b3e5b2e0757..a32e6c79056 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -512,7 +512,8 @@ Trunk (unreleased changes)
     HADOOP-6176. Add a couple package private methods to AccessTokenHandler
     for testing.  (Kan Zhang via szetszwo)
 
    HADOOP-6182. Fix ReleaseAudit warnings (Giridharan Kesavan and Lee Tucker via gkesavan)
    HADOOP-6182. Fix ReleaseAudit warnings (Giridharan Kesavan and Lee Tucker
    via gkesavan)
 
     HADOOP-6173. Change src/native/packageNativeHadoop.sh to package all
     native library files.  (Hong Tang via szetszwo)
@@ -526,6 +527,10 @@ Trunk (unreleased changes)
     HADOOP-6231. Allow caching of filesystem instances to be disabled on a
     per-instance basis. (tomwhite)
 
    HADOOP-6133. Add a caching layer to Configuration::getClassByName to
    alleviate a performance regression introduced in a compatibility layer.
    (Todd Lipcon via cdouglas)

   OPTIMIZATIONS
 
     HADOOP-5595. NameNode does not need to run a replicator to choose a
diff --git a/src/java/org/apache/hadoop/conf/Configuration.java b/src/java/org/apache/hadoop/conf/Configuration.java
index cfb1ba8d70a..8bf4c1c436e 100644
-- a/src/java/org/apache/hadoop/conf/Configuration.java
++ b/src/java/org/apache/hadoop/conf/Configuration.java
@@ -170,6 +170,9 @@
    */
   private static final ArrayList<String> defaultResources = 
     new ArrayList<String>();

  private static final Map<ClassLoader, Map<String, Class<?>>>
    CACHE_CLASSES = new WeakHashMap<ClassLoader, Map<String, Class<?>>>();
   
   /**
    * Flag to indicate if the storage of resource which updates a key needs 
@@ -1029,7 +1032,27 @@ public void setStrings(String name, String... values) {
    * @throws ClassNotFoundException if the class is not found.
    */
   public Class<?> getClassByName(String name) throws ClassNotFoundException {
    return Class.forName(name, true, classLoader);
    Map<String, Class<?>> map;
    
    synchronized (CACHE_CLASSES) {
      map = CACHE_CLASSES.get(classLoader);
      if (map == null) {
        map = Collections.synchronizedMap(
          new WeakHashMap<String, Class<?>>());
        CACHE_CLASSES.put(classLoader, map);
      }
    }

    Class clazz = map.get(name);
    if (clazz == null) {
      clazz = Class.forName(name, true, classLoader);
      if (clazz != null) {
        // two putters can race here, but they'll put the same class
        map.put(name, clazz);
      }
    }

    return clazz;
   }
 
   /** 
- 
2.19.1.windows.1

