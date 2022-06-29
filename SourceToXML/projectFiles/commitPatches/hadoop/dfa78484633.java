From dfa78484633b3ce21471d527b9c24671e3ca5df9 Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Wed, 14 Oct 2015 20:25:33 +0100
Subject: [PATCH] HADOOP-12478. Shell.getWinUtilsPath() has been renamed
 Shell.getWinutilsPath(). (stevel)

--
 .../hadoop-common/CHANGES.txt                 |  3 +++
 .../java/org/apache/hadoop/fs/HardLink.java   |  2 +-
 .../hadoop/util/NativeLibraryChecker.java     |  2 +-
 .../java/org/apache/hadoop/util/Shell.java    | 26 +++++++++----------
 .../apache/hadoop/util/SysInfoWindows.java    |  2 +-
 .../security/TestUserGroupInformation.java    |  2 +-
 .../org/apache/hadoop/util/TestShell.java     |  8 +++---
 .../org/apache/hadoop/util/TestWinUtils.java  |  6 ++---
 .../yarn/util/WindowsBasedProcessTree.java    |  4 +--
 .../server/nodemanager/ContainerExecutor.java |  2 +-
 .../WindowsSecureContainerExecutor.java       |  2 +-
 .../launcher/ContainerLaunch.java             |  2 +-
 .../launcher/TestContainerLaunch.java         |  2 +-
 13 files changed, 33 insertions(+), 30 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 8cd36a589dd..7efc885add2 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1225,6 +1225,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-11515. Upgrade jsch lib to jsch-0.1.51 to avoid problems running
     on java7. (stevel and ozawa)
 
    HADOOP-12478. Shell.getWinUtilsPath() has been renamed
    Shell.getWinutilsPath(). (stevel)

   OPTIMIZATIONS
 
     HADOOP-12051. ProtobufRpcEngine.invoke() should use Exception.toString()
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
index 0de019d34a1..8b47dfeb9a7 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
@@ -137,7 +137,7 @@ void setLinkCountCmdTemplate(String[] template) {
     @Override
     String[] linkCount(File file) throws IOException {
       // trigger the check for winutils
      Shell.getWinutilsFile();
      Shell.getWinUtilsFile();
       String[] buf = new String[getLinkCountCommand.length];
       System.arraycopy(getLinkCountCommand, 0, buf, 0, 
                        getLinkCountCommand.length);
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java
index 9d84ced8569..d8c68992a5e 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java
@@ -108,7 +108,7 @@ public static void main(String[] args) {
     if (Shell.WINDOWS) {
       // winutils.exe is required on Windows
       try {
        winutilsPath = Shell.getWinutilsFile().getCanonicalPath();
        winutilsPath = Shell.getWinUtilsFile().getCanonicalPath();
         winutilsExists = true;
       } catch (IOException e) {
         LOG.debug("No Winutils: ", e);
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
index 4370d89ec89..d6eca69d95b 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
@@ -185,7 +185,7 @@ private static OSType getOSType() {
     //'groups username' command return is inconsistent across different unixes
     return WINDOWS ?
       new String[]
          { getWinutilsPath(), "groups", "-F", "\"" + user + "\"" }
          { getWinUtilsPath(), "groups", "-F", "\"" + user + "\"" }
       : new String [] {"bash", "-c", "id -gn " + user + "&& id -Gn " + user};
   }
 
@@ -198,7 +198,7 @@ private static OSType getOSType() {
 
   /** Return a command to get permission information. */
   public static String[] getGetPermissionCommand() {
    return (WINDOWS) ? new String[] { getWinutilsPath(), "ls", "-F" }
    return (WINDOWS) ? new String[] { getWinUtilsPath(), "ls", "-F" }
                      : new String[] { "/bin/ls", "-ld" };
   }
 
@@ -206,11 +206,11 @@ private static OSType getOSType() {
   public static String[] getSetPermissionCommand(String perm, boolean recursive) {
     if (recursive) {
       return (WINDOWS) ?
          new String[] { getWinutilsPath(), "chmod", "-R", perm }
          new String[] { getWinUtilsPath(), "chmod", "-R", perm }
           : new String[] { "chmod", "-R", perm };
     } else {
       return (WINDOWS) ?
          new String[] { getWinutilsPath(), "chmod", perm }
          new String[] { getWinUtilsPath(), "chmod", perm }
           : new String[] { "chmod", perm };
     }
   }
@@ -234,21 +234,21 @@ private static OSType getOSType() {
   /** Return a command to set owner. */
   public static String[] getSetOwnerCommand(String owner) {
     return (WINDOWS) ?
        new String[] { getWinutilsPath(), "chown", "\"" + owner + "\"" }
        new String[] { getWinUtilsPath(), "chown", "\"" + owner + "\"" }
         : new String[] { "chown", owner };
   }
 
   /** Return a command to create symbolic links. */
   public static String[] getSymlinkCommand(String target, String link) {
     return WINDOWS ?
       new String[] { getWinutilsPath(), "symlink", link, target }
       new String[] { getWinUtilsPath(), "symlink", link, target }
        : new String[] { "ln", "-s", target, link };
   }
 
   /** Return a command to read the target of the a symbolic link. */
   public static String[] getReadlinkCommand(String link) {
     return WINDOWS ?
        new String[] { getWinutilsPath(), "readlink", link }
        new String[] { getWinUtilsPath(), "readlink", link }
         : new String[] { "readlink", link };
   }
 
@@ -266,9 +266,9 @@ private static OSType getOSType() {
     // Code == 0 means check alive
     if (Shell.WINDOWS) {
       if (0 == code) {
        return new String[] {Shell.getWinutilsPath(), "task", "isAlive", pid };
        return new String[] {Shell.getWinUtilsPath(), "task", "isAlive", pid };
       } else {
        return new String[] {Shell.getWinutilsPath(), "task", "kill", pid };
        return new String[] {Shell.getWinUtilsPath(), "task", "kill", pid };
       }
     }
 
@@ -590,7 +590,7 @@ public static String getQualifiedBinPath(String executable)
    * The lack of such checks has led to many support issues being raised.
    * <p>
    * @deprecated use one of the exception-raising getter methods,
   * specifically {@link #getWinutilsPath()} or {@link #getWinutilsFile()}
   * specifically {@link #getWinUtilsPath()} or {@link #getWinUtilsFile()}
    */
   @Deprecated
   public static final String WINUTILS;
@@ -646,7 +646,7 @@ public static String getQualifiedBinPath(String executable)
    * Predicate to indicate whether or not the path to winutils is known.
    *
    * If true, then {@link #WINUTILS} is non-null, and both
   * {@link #getWinutilsPath()} and {@link #getWinutilsFile()}
   * {@link #getWinUtilsPath()} and {@link #getWinUtilsFile()}
    * will successfully return this value. Always false on non-windows systems.
    * @return true if there is a valid path to the binary
    */
@@ -662,7 +662,7 @@ public static boolean hasWinutilsPath() {
    * @return the path to {@link #WINUTILS_EXE}
    * @throws RuntimeException if the path is not resolvable
    */
  public static String getWinutilsPath() {
  public static String getWinUtilsPath() {
     if (WINUTILS_FAILURE == null) {
       return WINUTILS_PATH;
     } else {
@@ -677,7 +677,7 @@ public static String getWinutilsPath() {
    * @return the file instance referring to the winutils bin.
    * @throws FileNotFoundException on any failure to locate that file.
    */
  public static File getWinutilsFile() throws FileNotFoundException {
  public static File getWinUtilsFile() throws FileNotFoundException {
     if (WINUTILS_FAILURE == null) {
       return WINUTILS_FILE;
     } else {
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java
index 1fd036e41d0..3b009efa8de 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java
@@ -72,7 +72,7 @@ void reset() {
   String getSystemInfoInfoFromShell() {
     try {
       ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
          new String[] {Shell.getWinutilsFile().getCanonicalPath(),
          new String[] {Shell.getWinUtilsFile().getCanonicalPath(),
               "systeminfo" });
       shellExecutor.execute();
       return shellExecutor.getOutput();
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
index 6e279643840..54cfc2d4890 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
@@ -218,7 +218,7 @@ public void testGetServerSideGroups() throws IOException,
     }
     // get the groups
     pp = Runtime.getRuntime().exec(Shell.WINDOWS ?
      Shell.getWinutilsPath() + " groups -F"
      Shell.getWinUtilsPath() + " groups -F"
       : "id -Gn");
     br = new BufferedReader(new InputStreamReader(pp.getInputStream()));
     String line = br.readLine();
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
index 138d0255ac6..a9f7f6ddd46 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
@@ -194,7 +194,7 @@ public void testGetCheckProcessIsAliveCommand() throws Exception {
 
     if (Shell.WINDOWS) {
       expectedCommand =
          new String[]{getWinutilsPath(), "task", "isAlive", anyPid };
          new String[]{getWinUtilsPath(), "task", "isAlive", anyPid };
     } else if (Shell.isSetsidAvailable) {
       expectedCommand = new String[] { "bash", "-c", "kill -0 -- -" + anyPid };
     } else {
@@ -214,7 +214,7 @@ public void testGetSignalKillCommand() throws Exception {
 
     if (Shell.WINDOWS) {
       expectedCommand =
          new String[]{getWinutilsPath(), "task", "kill", anyPid };
          new String[]{getWinUtilsPath(), "task", "kill", anyPid };
     } else if (Shell.isSetsidAvailable) {
       expectedCommand = new String[] { "bash", "-c", "kill -9 -- -" + anyPid };
     } else {
@@ -342,12 +342,12 @@ public void testBinWinUtilsNotAFile() throws Throwable {
   public void testNoWinutilsOnUnix() throws Throwable {
     Assume.assumeFalse(WINDOWS);
     try {
      getWinutilsFile();
      getWinUtilsFile();
     } catch (FileNotFoundException ex) {
       assertExContains(ex, E_NOT_A_WINDOWS_SYSTEM);
     }
     try {
      getWinutilsPath();
      getWinUtilsPath();
     } catch (RuntimeException ex) {
       assertExContains(ex, E_NOT_A_WINDOWS_SYSTEM);
       if ( ex.getCause() == null
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java
index 6fc8969819e..fde28227679 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java
@@ -56,7 +56,7 @@ public void setUp() throws IOException {
     TEST_DIR.mkdirs();
     assertTrue("Failed to create Test directory " + TEST_DIR,
         TEST_DIR.isDirectory() );
    winutils = Shell.getWinutilsPath();
    winutils = Shell.getWinUtilsPath();
   }
 
   @After
@@ -65,7 +65,7 @@ public void tearDown() throws IOException {
   }
 
   private void requireWinutils() throws IOException {
    Shell.getWinutilsPath();
    Shell.getWinUtilsPath();
   }
 
   // Helper routine that writes the given content to the file.
@@ -280,7 +280,7 @@ public void testBasicChmod() throws IOException {
     // - Change mode to 677 so owner does not have execute permission.
     // - Verify the owner truly does not have the permissions to execute the file.
 
    File winutilsFile = Shell.getWinutilsFile();
    File winutilsFile = Shell.getWinUtilsFile();
     File aExe = new File(TEST_DIR, "a.exe");
     FileUtils.copyFile(winutilsFile, aExe);
     chmod("677", aExe);
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java
index 41b26f418c6..1bf25a5492b 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java
@@ -55,7 +55,7 @@ public static boolean isAvailable() {
         return false;
       }
       ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
          new String[] { Shell.getWinutilsPath(), "help" });
          new String[] { Shell.getWinUtilsPath(), "help" });
       try {
         shellExecutor.execute();
       } catch (IOException e) {
@@ -80,7 +80,7 @@ public WindowsBasedProcessTree(String pid) {
   String getAllProcessInfoFromShell() {
     try {
       ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
          new String[] {Shell.getWinutilsFile().getCanonicalPath(),
          new String[] {Shell.getWinUtilsFile().getCanonicalPath(),
               "task", "processList", taskProcessId });
       shellExecutor.execute();
       return shellExecutor.getOutput();
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java
index a83ef844de8..6d75a1cfc64 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java
@@ -401,7 +401,7 @@ protected Path getPidFilePath(ContainerId containerId) {
           cpuRate = Math.min(10000, (int) (containerCpuPercentage * 100));
         }
       }
      return new String[] { Shell.getWinutilsPath(), "task", "create", "-m",
      return new String[] { Shell.getWinUtilsPath(), "task", "create", "-m",
           String.valueOf(memory), "-c", String.valueOf(cpuRate), groupId,
           "cmd /c " + command };
     } else {
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java
index 70fdcdaa5db..8d307441cc4 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java
@@ -578,7 +578,7 @@ public void setConf(Configuration conf) {
       LOG.debug(String.format("getRunCommand: %s exists:%b", 
           command, f.exists()));
     }
    return new String[] { Shell.getWinutilsPath(), "task",
    return new String[] { Shell.getWinUtilsPath(), "task",
         "createAsUser", groupId,
         userName, pidFile.toString(), "cmd /c " + command };
   }
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java
index 43493327ef7..fc8615bfbb8 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java
@@ -747,7 +747,7 @@ protected void link(Path src, Path dst) throws IOException {
       String srcFileStr = srcFile.getPath();
       String dstFileStr = new File(dst.toString()).getPath();
       lineWithLenCheck(String.format("@%s symlink \"%s\" \"%s\"",
          Shell.getWinutilsPath(), dstFileStr, srcFileStr));
          Shell.getWinUtilsPath(), dstFileStr, srcFileStr));
       errorCheck();
     }
 
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java
index f85b01e31f0..0abae2b09d1 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java
@@ -1072,7 +1072,7 @@ public void testWindowsShellScriptBuilderMkdir() throws IOException {
   public void testWindowsShellScriptBuilderLink() throws IOException {
     // Test is only relevant on Windows
     Assume.assumeTrue(Shell.WINDOWS);
    String linkCmd = "@" + Shell.getWinutilsPath() + " symlink \"\" \"\"";
    String linkCmd = "@" + Shell.getWinUtilsPath() + " symlink \"\" \"\"";
 
     // The tests are built on assuming 8191 max command line length
     assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGTH);
- 
2.19.1.windows.1

