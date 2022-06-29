From 859222146c0a6c168b0323bbb3715b24011e4edb Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <ecn@apache.org>
Date: Thu, 16 May 2013 18:41:42 +0000
Subject: [PATCH] ACCUMULO-1421 simplify reflection lookup of SafeModeAction

git-svn-id: https://svn.apache.org/repos/asf/accumulo/trunk@1483498 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/accumulo/server/Accumulo.java    | 17 +++--------------
 1 file changed, 3 insertions(+), 14 deletions(-)

diff --git a/server/src/main/java/org/apache/accumulo/server/Accumulo.java b/server/src/main/java/org/apache/accumulo/server/Accumulo.java
index 2e7f12bc2..6776c40c1 100644
-- a/server/src/main/java/org/apache/accumulo/server/Accumulo.java
++ b/server/src/main/java/org/apache/accumulo/server/Accumulo.java
@@ -218,29 +218,18 @@ public class Accumulo {
     DistributedFileSystem dfs = (DistributedFileSystem) FileSystem.get(CachedConfiguration.getInstance());
     // So this: if (!dfs.setSafeMode(SafeModeAction.SAFEMODE_GET))
     // Becomes this:
    Class<?> constantClass;
    Class<?> safeModeAction;
     try {
       // hadoop 2.0
      constantClass = Class.forName("org.apache.hadoop.hdfs.protocol.HdfsConstants");
      safeModeAction = Class.forName("org.apache.hadoop.hdfs.protocol.HdfsConstants$SafeModeAction");
     } catch (ClassNotFoundException ex) {
       // hadoop 1.0
       try {
        constantClass = Class.forName("org.apache.hadoop.hdfs.protocol.FSConstants");
        safeModeAction = Class.forName("org.apache.hadoop.hdfs.protocol.FSConstants$SafeModeAction");
       } catch (ClassNotFoundException e) {
         throw new RuntimeException("Cannot figure out the right class for Constants");
       }
     }
    Class<?> safeModeAction = null;
    for (Class<?> klass : constantClass.getDeclaredClasses()) {
      if (klass.getSimpleName().equals("SafeModeAction")) {
        safeModeAction = klass;
        break;
      }
    }
    if (safeModeAction == null) {
      throw new RuntimeException("Cannot find SafeModeAction in constants class");
    }
    
     Object get = null;
     for (Object obj : safeModeAction.getEnumConstants()) {
       if (obj.toString().equals("SAFEMODE_GET"))
- 
2.19.1.windows.1

