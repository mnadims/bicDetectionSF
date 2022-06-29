From 43480c12f6fea605c720ae64aa4a9d0208d82735 Mon Sep 17 00:00:00 2001
From: rkanter <rkanter@unknown>
Date: Sat, 22 Dec 2012 23:53:37 +0000
Subject: [PATCH] OOZIE-1144 OOZIE-1137 breaks the sharelib (rkanter)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1425376 13f79535-47bb-0310-9956-ffa450edef68
--
 .../action/hadoop/JavaActionExecutor.java     | 27 +++++++----
 .../action/hadoop/TestJavaActionExecutor.java | 46 +++++++++++++++++++
 release-log.txt                               |  1 +
 3 files changed, 65 insertions(+), 9 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index 8c1f84a97..00ae5a0cb 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -389,7 +389,7 @@ public class JavaActionExecutor extends ActionExecutor {
         }
     }
 
    protected void addShareLib(Configuration conf, String actionShareLibName)
    protected void addShareLib(Path appPath, Configuration conf, String actionShareLibName)
     throws ActionExecutorException {
         if (actionShareLibName != null) {
             try {
@@ -397,8 +397,16 @@ public class JavaActionExecutor extends ActionExecutor {
                 if (systemLibPath != null) {
                     Path actionLibPath = new Path(systemLibPath, actionShareLibName);
                     String user = conf.get("user.name");
                    FileSystem fs =
                        Services.get().get(HadoopAccessorService.class).createFileSystem(user, actionLibPath.toUri(), conf);
                    FileSystem fs;
                    // If the actionLibPath has a valid scheme and authority, then use them to determine the filesystem that the
                    // sharelib resides on; otherwise, assume it resides on the same filesystem as the appPath and use the appPath
                    // to determine the filesystem
                    if (actionLibPath.toUri().getScheme() != null && actionLibPath.toUri().getAuthority() != null) {
                        fs = Services.get().get(HadoopAccessorService.class).createFileSystem(user, actionLibPath.toUri(), conf);
                    }
                    else {
                        fs = Services.get().get(HadoopAccessorService.class).createFileSystem(user, appPath.toUri(), conf);
                    }
                     if (fs.exists(actionLibPath)) {
                         FileStatus[] files = fs.listStatus(actionLibPath);
                         for (FileStatus file : files) {
@@ -482,19 +490,20 @@ public class JavaActionExecutor extends ActionExecutor {
             }
         }
 
        addAllShareLibs(conf, context, actionXml);
        addAllShareLibs(appPath, conf, context, actionXml);
 	}
 
     // Adds action specific share libs and common share libs
    private void addAllShareLibs(Configuration conf, Context context, Element actionXml)
    private void addAllShareLibs(Path appPath, Configuration conf, Context context, Element actionXml)
             throws ActionExecutorException {
         // Add action specific share libs
        addActionShareLib(conf, context, actionXml);
        addActionShareLib(appPath, conf, context, actionXml);
         // Add common sharelibs for Oozie
        addShareLib(conf, JavaActionExecutor.OOZIE_COMMON_LIBDIR);
        addShareLib(appPath, conf, JavaActionExecutor.OOZIE_COMMON_LIBDIR);
     }
 
    private void addActionShareLib(Configuration conf, Context context, Element actionXml) throws ActionExecutorException {
    private void addActionShareLib(Path appPath, Configuration conf, Context context, Element actionXml)
            throws ActionExecutorException {
         XConfiguration wfJobConf = null;
         try {
             wfJobConf = new XConfiguration(new StringReader(context.getWorkflow().getConf()));
@@ -506,7 +515,7 @@ public class JavaActionExecutor extends ActionExecutor {
         // Action sharelibs are only added if user has specified to use system libpath
         if (wfJobConf.getBoolean(OozieClient.USE_SYSTEM_LIBPATH, false)) {
             // add action specific sharelibs
            addShareLib(conf, getShareLibName(context, actionXml, conf));
            addShareLib(appPath, conf, getShareLibName(context, actionXml, conf));
         }
     }
 
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
index 022ab7a9b..7c0c11290 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
@@ -1019,6 +1019,52 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         assertTrue(cacheFilesStr.contains(jar3Path.toString()));
     }
 
    public void testAddShareLibSchemeAndAuthority() throws Exception {
        JavaActionExecutor ae = new JavaActionExecutor() {
            @Override
            protected String getDefaultShareLibName(Element actionXml) {
                return "java-action-executor";
            }
        };
        String actionXml = "<java>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>" + "<main-class>" + LauncherMainTester.class.getName()
                + "</main-class>" + "</java>";
        Element eActionXml = XmlUtils.parseXml(actionXml);
        Context context = createContext(actionXml, null);

        // Set sharelib to a relative path (i.e. no scheme nor authority)
        Services.get().destroy();
        setSystemProperty(WorkflowAppService.SYSTEM_LIB_PATH, "/user/" + getOozieUser() + "/share/");
        new Services().init();
        Path appPath = getAppPath();
        JobConf conf = ae.createBaseHadoopConf(context, eActionXml);
        // The next line should not throw an Exception because it will get the scheme and authority from the appPath, and not the
        // sharelib path because it doesn't have a scheme or authority
        ae.addShareLib(appPath, conf, "java-action-executor");

        appPath = new Path("foo://bar:1234/blah");
        conf = ae.createBaseHadoopConf(context, eActionXml);
        // The next line should throw an Exception because it will get the scheme and authority from the appPath, which is obviously
        // invalid, and not the sharelib path because it doesn't have a scheme or authority
        try {
            ae.addShareLib(appPath, conf, "java-action-executor");
        }
        catch (ActionExecutorException aee) {
            assertEquals("E0902", aee.getErrorCode());
            assertTrue(aee.getMessage().contains("[No FileSystem for scheme: foo]"));
        }

        // Set sharelib to a full path (i.e. include scheme and authority)
        Services.get().destroy();
        setSystemProperty(WorkflowAppService.SYSTEM_LIB_PATH, getNameNodeUri() + "/user/" + getOozieUser() + "/share/");
        new Services().init();
        appPath = new Path("foo://bar:1234/blah");
        conf = ae.createBaseHadoopConf(context, eActionXml);
        // The next line should not throw an Exception because it will get the scheme and authority from the sharelib path (and not
        // from the obviously invalid appPath)
        ae.addShareLib(appPath, conf, "java-action-executor");
    }

     public void testFilesystemScheme() throws Exception {
         try {
             String actionXml = "<java>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
diff --git a/release-log.txt b/release-log.txt
index 8ec3b395e..6acaef078 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.4.0 release (trunk - unreleased)
 
OOZIE-1144 OOZIE-1137 breaks the sharelib (rkanter)
 OOZIE-1035 Improve forkjoin validation to allow same errorTo transitions (rkanter)
 OOZIE-1137 In light of federation use actionLibPath instead of appPath (vaidya via rkanter)
 OOZIE-1126 see if checkstyle works for oozie development. (jaoki via rkanter)
- 
2.19.1.windows.1

