From 4637ca417ae31f6b4f0530b8dca2d5fbadc2e03f Mon Sep 17 00:00:00 2001
From: rkanter <rkanter@unknown>
Date: Wed, 19 Dec 2012 18:18:58 +0000
Subject: [PATCH] OOZIE-1137 In light of federation use actionLibPath instead
 of appPath (vaidya via rkanter)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1423998 13f79535-47bb-0310-9956-ffa450edef68
--
 .../action/hadoop/JavaActionExecutor.java      | 18 +++++++++---------
 release-log.txt                                |  1 +
 2 files changed, 10 insertions(+), 9 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index f7d2d6d87..8c1f84a97 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -389,7 +389,7 @@ public class JavaActionExecutor extends ActionExecutor {
         }
     }
 
    protected void addShareLib(Path appPath, Configuration conf, String actionShareLibName)
    protected void addShareLib(Configuration conf, String actionShareLibName)
     throws ActionExecutorException {
         if (actionShareLibName != null) {
             try {
@@ -398,11 +398,11 @@ public class JavaActionExecutor extends ActionExecutor {
                     Path actionLibPath = new Path(systemLibPath, actionShareLibName);
                     String user = conf.get("user.name");
                     FileSystem fs =
                        Services.get().get(HadoopAccessorService.class).createFileSystem(user, appPath.toUri(), conf);
                        Services.get().get(HadoopAccessorService.class).createFileSystem(user, actionLibPath.toUri(), conf);
                     if (fs.exists(actionLibPath)) {
                         FileStatus[] files = fs.listStatus(actionLibPath);
                         for (FileStatus file : files) {
                            addToCache(conf, appPath, file.getPath().toUri().getPath(), false);
                            addToCache(conf, actionLibPath, file.getPath().toUri().getPath(), false);
                         }
                     }
                 }
@@ -482,19 +482,19 @@ public class JavaActionExecutor extends ActionExecutor {
             }
         }
 
        addAllShareLibs(appPath, conf, context, actionXml);
        addAllShareLibs(conf, context, actionXml);
 	}
 
     // Adds action specific share libs and common share libs
    private void addAllShareLibs(Path appPath, Configuration conf, Context context, Element actionXml)
    private void addAllShareLibs(Configuration conf, Context context, Element actionXml)
             throws ActionExecutorException {
         // Add action specific share libs
        addActionShareLib(appPath, conf, context, actionXml);
        addActionShareLib(conf, context, actionXml);
         // Add common sharelibs for Oozie
        addShareLib(appPath, conf, JavaActionExecutor.OOZIE_COMMON_LIBDIR);
        addShareLib(conf, JavaActionExecutor.OOZIE_COMMON_LIBDIR);
     }
 
    private void addActionShareLib(Path appPath, Configuration conf, Context context, Element actionXml) throws ActionExecutorException {
    private void addActionShareLib(Configuration conf, Context context, Element actionXml) throws ActionExecutorException {
         XConfiguration wfJobConf = null;
         try {
             wfJobConf = new XConfiguration(new StringReader(context.getWorkflow().getConf()));
@@ -506,7 +506,7 @@ public class JavaActionExecutor extends ActionExecutor {
         // Action sharelibs are only added if user has specified to use system libpath
         if (wfJobConf.getBoolean(OozieClient.USE_SYSTEM_LIBPATH, false)) {
             // add action specific sharelibs
            addShareLib(appPath, conf, getShareLibName(context, actionXml, conf));
            addShareLib(conf, getShareLibName(context, actionXml, conf));
         }
     }
 
diff --git a/release-log.txt b/release-log.txt
index a1881682b..e97d8821e 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.4.0 release (trunk - unreleased)
 
OOZIE-1137 In light of federation use actionLibPath instead of appPath (vaidya via rkanter)
 OOZIE-1126 see if checkstyle works for oozie development. (jaoki via rkanter)
 OOZIE-1124 Split pig unit tests to a separate module (rohini via virag)
 OOZIE-1087 Remove requirement of hive-default.xml from Hive action (rkanter)
- 
2.19.1.windows.1

