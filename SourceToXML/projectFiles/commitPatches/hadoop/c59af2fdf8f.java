From c59af2fdf8f7f53117c626a895dab7fd78cf08ec Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Tue, 13 Oct 2015 21:29:50 +0100
Subject: [PATCH] HADOOP-10775. Shell operations to fail with meaningful errors
 on windows if winutils.exe not found. (stevel)

--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../java/org/apache/hadoop/fs/FileUtil.java   |  29 -
 .../java/org/apache/hadoop/fs/HardLink.java   |   8 +
 .../org/apache/hadoop/util/DataChecksum.java  |   2 +-
 .../hadoop/util/NativeLibraryChecker.java     |  25 +-
 .../java/org/apache/hadoop/util/Shell.java    | 730 +++++++++++++-----
 .../apache/hadoop/util/SysInfoWindows.java    |   5 +-
 .../org/apache/hadoop/fs/SymlinkBaseTest.java |  30 +-
 .../org/apache/hadoop/fs/TestFileUtil.java    |   8 +-
 .../apache/hadoop/fs/TestSymlinkLocalFS.java  |  11 -
 .../security/TestUserGroupInformation.java    |   3 +-
 .../org/apache/hadoop/util/TestShell.java     | 265 ++++++-
 .../org/apache/hadoop/util/TestWinUtils.java  | 124 +--
 .../yarn/util/WindowsBasedProcessTree.java    |  10 +-
 .../server/nodemanager/ContainerExecutor.java |   2 +-
 .../WindowsSecureContainerExecutor.java       |   3 +-
 .../launcher/ContainerLaunch.java             |  14 +-
 .../nodemanager/TestContainerExecutor.java    |   1 +
 .../launcher/TestContainerLaunch.java         |  31 +-
 19 files changed, 918 insertions(+), 386 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index f3c341c7232..4b6683077a4 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -885,6 +885,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12344. Improve validateSocketPathSecurity0 error message (Casey
     Brotherton via Colin P. McCabe)
 
    HADOOP-10775. Shell operations to fail with meaningful errors on windows if
    winutils.exe not found. (stevel)

   OPTIMIZATIONS
 
     HADOOP-11785. Reduce the number of listStatus operation in distcp
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileUtil.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileUtil.java
index 3c0e90da2d9..e74c41c8290 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileUtil.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileUtil.java
@@ -784,35 +784,6 @@ public static int symLink(String target, String linkname) throws IOException{
     File linkFile = new File(
         Path.getPathWithoutSchemeAndAuthority(new Path(linkname)).toString());
 
    // If not on Java7+, copy a file instead of creating a symlink since
    // Java6 has close to no support for symlinks on Windows. Specifically
    // File#length and File#renameTo do not work as expected.
    // (see HADOOP-9061 for additional details)
    // We still create symlinks for directories, since the scenario in this
    // case is different. The directory content could change in which
    // case the symlink loses its purpose (for example task attempt log folder
    // is symlinked under userlogs and userlogs are generated afterwards).
    if (Shell.WINDOWS && !Shell.isJava7OrAbove() && targetFile.isFile()) {
      try {
        LOG.warn("FileUtil#symlink: On Windows+Java6, copying file instead " +
            "of creating a symlink. Copying " + target + " -> " + linkname);

        if (!linkFile.getParentFile().exists()) {
          LOG.warn("Parent directory " + linkFile.getParent() +
              " does not exist.");
          return 1;
        } else {
          org.apache.commons.io.FileUtils.copyFile(targetFile, linkFile);
        }
      } catch (IOException ex) {
        LOG.warn("FileUtil#symlink failed to copy the file with error: "
            + ex.getMessage());
        // Exit with non-zero exit code
        return 1;
      }
      return 0;
    }

     String[] cmd = Shell.getSymlinkCommand(
         targetFile.toString(),
         linkFile.toString());
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
index 209ba6997ae..0de019d34a1 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
@@ -122,6 +122,12 @@ void setLinkCountCmdTemplate(String[] template) {
   @VisibleForTesting
   static class HardLinkCGWin extends HardLinkCommandGetter {
 
    /**
     * Build the windows link command. This must not
     * use an exception-raising reference to WINUTILS, as
     * some tests examine the command.
     */
    @SuppressWarnings("deprecation")
     static String[] getLinkCountCommand = {
         Shell.WINUTILS, "hardlink", "stat", null};
 
@@ -130,6 +136,8 @@ void setLinkCountCmdTemplate(String[] template) {
      */
     @Override
     String[] linkCount(File file) throws IOException {
      // trigger the check for winutils
      Shell.getWinutilsFile();
       String[] buf = new String[getLinkCountCommand.length];
       System.arraycopy(getLinkCountCommand, 0, buf, 0, 
                        getLinkCountCommand.length);
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/DataChecksum.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/DataChecksum.java
index a38ec325fec..d9dc7af1fc2 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/DataChecksum.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/DataChecksum.java
@@ -75,7 +75,7 @@ public static Type valueOf(int id) {
    * is chosen depending on the platform.
    */
   public static Checksum newCrc32() {
    return Shell.isJava7OrAbove()? new CRC32(): new PureJavaCrc32();
    return new CRC32();
   }
 
   public static DataChecksum newDataChecksum(Type type, int bytesPerChecksum ) {
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java
index 81448ab2d4d..9d84ced8569 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/NativeLibraryChecker.java
@@ -18,7 +18,6 @@
 
 package org.apache.hadoop.util;
 
import org.apache.hadoop.util.NativeCodeLoader;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.crypto.OpensslCipher;
 import org.apache.hadoop.io.compress.Lz4Codec;
@@ -27,10 +26,17 @@
 import org.apache.hadoop.io.compress.zlib.ZlibFactory;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
 
 @InterfaceAudience.Private
 @InterfaceStability.Unstable
 public class NativeLibraryChecker {
  public static final Logger LOG =
      LoggerFactory.getLogger(NativeLibraryChecker.class);

   /**
    * A tool to test native library availability, 
    */
@@ -99,12 +105,17 @@ public static void main(String[] args) {
       }
     }
 
    // winutils.exe is required on Windows
    winutilsPath = Shell.getWinUtilsPath();
    if (winutilsPath != null) {
      winutilsExists = true;
    } else {
      winutilsPath = "";
    if (Shell.WINDOWS) {
      // winutils.exe is required on Windows
      try {
        winutilsPath = Shell.getWinutilsFile().getCanonicalPath();
        winutilsExists = true;
      } catch (IOException e) {
        LOG.debug("No Winutils: ", e);
        winutilsPath = e.getMessage();
        winutilsExists = false;
      }
      System.out.printf("winutils: %b %s%n", winutilsExists, winutilsPath);
     }
 
     System.out.println("Native library checking:");
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
index ca70ef30fd6..4370d89ec89 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
@@ -19,6 +19,7 @@
 
 import java.io.BufferedReader;
 import java.io.File;
import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.InputStream;
@@ -30,41 +31,74 @@
 import java.util.TimerTask;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.annotations.VisibleForTesting;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
/** 
 * A base class for running a Unix command.
 * 
 * <code>Shell</code> can be used to run unix commands like <code>du</code> or
/**
 * A base class for running a Shell command.
 *
 * <code>Shell</code> can be used to run shell commands like <code>du</code> or
  * <code>df</code>. It also offers facilities to gate commands by 
  * time-intervals.
  */
@InterfaceAudience.LimitedPrivate({"HDFS", "MapReduce"})
@InterfaceStability.Unstable
abstract public class Shell {
  
  public static final Log LOG = LogFactory.getLog(Shell.class);
  
  private static boolean IS_JAVA7_OR_ABOVE =
      System.getProperty("java.version").substring(0, 3).compareTo("1.7") >= 0;
@InterfaceAudience.Public
@InterfaceStability.Evolving
public abstract class Shell {
  public static final Logger LOG = LoggerFactory.getLogger(Shell.class);

  /**
   * Text to include when there are windows-specific problems.
   * {@value}
   */
  private static final String WINDOWS_PROBLEMS =
      "https://wiki.apache.org/hadoop/WindowsProblems";

  /**
   * Name of the windows utils binary: {@value}.
   */
  static final String WINUTILS_EXE = "winutils.exe";

  /**
   * System property for the Hadoop home directory: {@value}.
   */
  public static final String SYSPROP_HADOOP_HOME_DIR = "hadoop.home.dir";
 
  /**
   * Environment variable for Hadoop's home dir: {@value}.
   */
  public static final String ENV_HADOOP_HOME = "HADOOP_HOME";

  /**
   * query to see if system is Java 7 or later.
   * Now that Hadoop requires Java 7 or later, this always returns true.
   * @deprecated This call isn't needed any more: please remove uses of it.
   * @return true, always.
   */
  @Deprecated
   public static boolean isJava7OrAbove() {
    return IS_JAVA7_OR_ABOVE;
    return true;
   }
 
   /**
    * Maximum command line length in Windows
    * KB830473 documents this as 8191
    */
  public static final int WINDOWS_MAX_SHELL_LENGHT = 8191;
  public static final int WINDOWS_MAX_SHELL_LENGTH = 8191;
 
   /**
   * Checks if a given command (String[]) fits in the Windows maximum command line length
   * Note that the input is expected to already include space delimiters, no extra count
   * will be added for delimiters.
   * mis-spelling of {@link #WINDOWS_MAX_SHELL_LENGTH}.
   * @deprecated use the correctly spelled constant.
   */
  @Deprecated
  public static final int WINDOWS_MAX_SHELL_LENGHT = WINDOWS_MAX_SHELL_LENGTH;

  /**
   * Checks if a given command (String[]) fits in the Windows maximum command
   * line length Note that the input is expected to already include space
   * delimiters, no extra count will be added for delimiters.
    *
    * @param commands command parts, including any space delimiters
    */
@@ -74,19 +108,19 @@ public static void checkWindowsCommandLineLength(String...commands)
     for (String s: commands) {
       len += s.length();
     }
    if (len > WINDOWS_MAX_SHELL_LENGHT) {
    if (len > WINDOWS_MAX_SHELL_LENGTH) {
       throw new IOException(String.format(
          "The command line has a length of %d exceeds maximum allowed length of %d. " +
          "Command starts with: %s",
          len, WINDOWS_MAX_SHELL_LENGHT,
          StringUtils.join("", commands).substring(0, 100)));
        "The command line has a length of %d exceeds maximum allowed length" +
            " of %d. Command starts with: %s",
        len, WINDOWS_MAX_SHELL_LENGTH,
        StringUtils.join("", commands).substring(0, 100)));
     }
   }
 
  /** a Unix command to get the current user's name */
  public final static String USER_NAME_COMMAND = "whoami";
  /** a Unix command to get the current user's name: {@value}. */
  public static final String USER_NAME_COMMAND = "whoami";
 
  /** Windows CreateProcess synchronization object */
  /** Windows <code>CreateProcess</code> synchronization object. */
   public static final Object WindowsProcessLaunchLock = new Object();
 
   // OSType detection
@@ -100,9 +134,13 @@ public static void checkWindowsCommandLineLength(String...commands)
     OS_TYPE_OTHER
   }
 
  /**
   * Get the type of the operating system, as determined from parsing
   * the <code>os.name</code> property.
   */
   public static final OSType osType = getOSType();
 
  static private OSType getOSType() {
  private static OSType getOSType() {
     String osName = System.getProperty("os.name");
     if (osName.startsWith("Windows")) {
       return OSType.OS_TYPE_WIN;
@@ -131,46 +169,49 @@ static private OSType getOSType() {
   public static final boolean PPC_64
                 = System.getProperties().getProperty("os.arch").contains("ppc64");
 
  /** a Unix command to get the current user's groups list */
  /** a Unix command to get the current user's groups list. */
   public static String[] getGroupsCommand() {
     return (WINDOWS)? new String[]{"cmd", "/c", "groups"}
                     : new String[]{"bash", "-c", "groups"};
   }
 
   /**
   * a Unix command to get a given user's groups list.
   * A command to get a given user's groups list.
    * If the OS is not WINDOWS, the command will get the user's primary group
    * first and finally get the groups list which includes the primary group.
    * i.e. the user's primary group will be included twice.
    */
   public static String[] getGroupsForUserCommand(final String user) {
    //'groups username' command return is non-consistent across different unixes
    return (WINDOWS)? new String[] { WINUTILS, "groups", "-F", "\"" + user + "\""}
                    : new String [] {"bash", "-c", "id -gn " + user
                                     + "&& id -Gn " + user};
    //'groups username' command return is inconsistent across different unixes
    return WINDOWS ?
      new String[]
          { getWinutilsPath(), "groups", "-F", "\"" + user + "\"" }
      : new String [] {"bash", "-c", "id -gn " + user + "&& id -Gn " + user};
   }
 
  /** a Unix command to get a given netgroup's user list */
  /** A command to get a given netgroup's user list. */
   public static String[] getUsersForNetgroupCommand(final String netgroup) {
     //'groups username' command return is non-consistent across different unixes
    return (WINDOWS)? new String [] {"cmd", "/c", "getent netgroup " + netgroup}
    return WINDOWS ? new String [] {"cmd", "/c", "getent netgroup " + netgroup}
                     : new String [] {"bash", "-c", "getent netgroup " + netgroup};
   }
 
   /** Return a command to get permission information. */
   public static String[] getGetPermissionCommand() {
    return (WINDOWS) ? new String[] { WINUTILS, "ls", "-F" }
    return (WINDOWS) ? new String[] { getWinutilsPath(), "ls", "-F" }
                      : new String[] { "/bin/ls", "-ld" };
   }
 
  /** Return a command to set permission */
  /** Return a command to set permission. */
   public static String[] getSetPermissionCommand(String perm, boolean recursive) {
     if (recursive) {
      return (WINDOWS) ? new String[] { WINUTILS, "chmod", "-R", perm }
                         : new String[] { "chmod", "-R", perm };
      return (WINDOWS) ?
          new String[] { getWinutilsPath(), "chmod", "-R", perm }
          : new String[] { "chmod", "-R", perm };
     } else {
      return (WINDOWS) ? new String[] { WINUTILS, "chmod", perm }
                       : new String[] { "chmod", perm };
      return (WINDOWS) ?
          new String[] { getWinutilsPath(), "chmod", perm }
          : new String[] { "chmod", perm };
     }
   }
 
@@ -182,45 +223,52 @@ static private OSType getOSType() {
    * @param file String file to set
    * @return String[] containing command and arguments
    */
  public static String[] getSetPermissionCommand(String perm, boolean recursive,
                                                 String file) {
  public static String[] getSetPermissionCommand(String perm,
      boolean recursive, String file) {
     String[] baseCmd = getSetPermissionCommand(perm, recursive);
     String[] cmdWithFile = Arrays.copyOf(baseCmd, baseCmd.length + 1);
     cmdWithFile[cmdWithFile.length - 1] = file;
     return cmdWithFile;
   }
 
  /** Return a command to set owner */
  /** Return a command to set owner. */
   public static String[] getSetOwnerCommand(String owner) {
    return (WINDOWS) ? new String[] { WINUTILS, "chown", "\"" + owner + "\"" }
                     : new String[] { "chown", owner };
    return (WINDOWS) ?
        new String[] { getWinutilsPath(), "chown", "\"" + owner + "\"" }
        : new String[] { "chown", owner };
   }
  
  /** Return a command to create symbolic links */

  /** Return a command to create symbolic links. */
   public static String[] getSymlinkCommand(String target, String link) {
    return WINDOWS ? new String[] { WINUTILS, "symlink", link, target }
                   : new String[] { "ln", "-s", target, link };
    return WINDOWS ?
       new String[] { getWinutilsPath(), "symlink", link, target }
       : new String[] { "ln", "-s", target, link };
   }
 
  /** Return a command to read the target of the a symbolic link*/
  /** Return a command to read the target of the a symbolic link. */
   public static String[] getReadlinkCommand(String link) {
    return WINDOWS ? new String[] { WINUTILS, "readlink", link }
    return WINDOWS ?
        new String[] { getWinutilsPath(), "readlink", link }
         : new String[] { "readlink", link };
   }
 
  /** Return a command for determining if process with specified pid is alive. */
  /**
   * Return a command for determining if process with specified pid is alive.
   * @param pid process ID
   * @return a <code>kill -0</code> command or equivalent
   */
   public static String[] getCheckProcessIsAliveCommand(String pid) {
     return getSignalKillCommand(0, pid);
   }
 
  /** Return a command to send a signal to a given pid */
  /** Return a command to send a signal to a given pid. */
   public static String[] getSignalKillCommand(int code, String pid) {
     // Code == 0 means check alive
     if (Shell.WINDOWS) {
       if (0 == code) {
        return new String[] { Shell.WINUTILS, "task", "isAlive", pid };
        return new String[] {Shell.getWinutilsPath(), "task", "isAlive", pid };
       } else {
        return new String[] { Shell.WINUTILS, "task", "kill", pid };
        return new String[] {Shell.getWinutilsPath(), "task", "kill", pid };
       }
     }
 
@@ -232,18 +280,20 @@ static private OSType getOSType() {
     }
   }
 
  /** Regular expression for environment variables: {@value}. */
   public static final String ENV_NAME_REGEX = "[A-Za-z_][A-Za-z0-9_]*";
  /** Return a regular expression string that match environment variables */

  /** Return a regular expression string that match environment variables. */
   public static String getEnvironmentVariableRegex() {
     return (WINDOWS)
         ? "%(" + ENV_NAME_REGEX + "?)%"
         : "\\$(" + ENV_NAME_REGEX + ")";
   }
  

   /**
    * Returns a File referencing a script with the given basename, inside the
   * given parent directory.  The file extension is inferred by platform: ".cmd"
   * on Windows, or ".sh" otherwise.
   * given parent directory.  The file extension is inferred by platform:
   * <code>".cmd"</code> on Windows, or <code>".sh"</code> otherwise.
    * 
    * @param parent File parent directory
    * @param basename String script file basename
@@ -254,9 +304,11 @@ public static File appendScriptExtension(File parent, String basename) {
   }
 
   /**
   * Returns a script file name with the given basename.  The file extension is
   * inferred by platform: ".cmd" on Windows, or ".sh" otherwise.
   * 
   * Returns a script file name with the given basename.
   *
   * The file extension is inferred by platform:
   * <code>".cmd"</code> on Windows, or <code>".sh"</code> otherwise.
   *
    * @param basename String script file basename
    * @return String script file name
    */
@@ -267,129 +319,372 @@ public static String appendScriptExtension(String basename) {
   /**
    * Returns a command to run the given script.  The script interpreter is
    * inferred by platform: cmd on Windows or bash otherwise.
   * 
   *
    * @param script File script to run
    * @return String[] command to run the script
    */
   public static String[] getRunScriptCommand(File script) {
     String absolutePath = script.getAbsolutePath();
    return WINDOWS ? new String[] { "cmd", "/c", absolutePath } :
      new String[] { "/bin/bash", absolutePath };
    return WINDOWS ?
      new String[] { "cmd", "/c", absolutePath }
      : new String[] { "/bin/bash", absolutePath };
   }
 
  /** a Unix command to set permission */
  /** a Unix command to set permission: {@value}. */
   public static final String SET_PERMISSION_COMMAND = "chmod";
  /** a Unix command to set owner */
  /** a Unix command to set owner: {@value}. */
   public static final String SET_OWNER_COMMAND = "chown";
 
  /** a Unix command to set the change user's groups list */
  /** a Unix command to set the change user's groups list: {@value}. */
   public static final String SET_GROUP_COMMAND = "chgrp";
  /** a Unix command to create a link */
  /** a Unix command to create a link: {@value}. */
   public static final String LINK_COMMAND = "ln";
  /** a Unix command to get a link target */
  /** a Unix command to get a link target: {@value}. */
   public static final String READ_LINK_COMMAND = "readlink";
 
  /**Time after which the executing script would be timedout*/
  /**Time after which the executing script would be timedout. */
   protected long timeOutInterval = 0L;
   /** If or not script timed out*/
  private AtomicBoolean timedOut;
  private final AtomicBoolean timedOut = new AtomicBoolean(false);
 

  /** Centralized logic to discover and validate the sanity of the Hadoop 
   *  home directory. Returns either NULL or a directory that exists and 
   *  was specified via either -Dhadoop.home.dir or the HADOOP_HOME ENV 
   *  variable.  This does a lot of work so it should only be called 
  /**
   *  Centralized logic to discover and validate the sanity of the Hadoop
   *  home directory.
   *
   *  This does a lot of work so it should only be called
    *  privately for initialization once per process.
   **/
  private static String checkHadoopHome() {
   *
   * @return A directory that exists and via was specified on the command line
   * via <code>-Dhadoop.home.dir</code> or the <code>HADOOP_HOME</code>
   * environment variable.
   * @throws FileNotFoundException if the properties are absent or the specified
   * path is not a reference to a valid directory.
   */
  private static File checkHadoopHome() throws FileNotFoundException {
 
     // first check the Dflag hadoop.home.dir with JVM scope
    String home = System.getProperty("hadoop.home.dir");
    String home = System.getProperty(SYSPROP_HADOOP_HOME_DIR);
 
     // fall back to the system/user-global env variable
     if (home == null) {
      home = System.getenv("HADOOP_HOME");
      home = System.getenv(ENV_HADOOP_HOME);
     }
    return checkHadoopHomeInner(home);
  }
 
    try {
       // couldn't find either setting for hadoop's home directory
       if (home == null) {
         throw new IOException("HADOOP_HOME or hadoop.home.dir are not set.");
       }
  /*
  A set of exception strings used to construct error messages;
  these are referred to in tests
  */
  static final String E_DOES_NOT_EXIST = "does not exist";
  static final String E_IS_RELATIVE = "is not an absolute path.";
  static final String E_NOT_DIRECTORY = "is not a directory.";
  static final String E_NO_EXECUTABLE = "Could not locate Hadoop executable";
  static final String E_NOT_EXECUTABLE_FILE = "Not an executable file";
  static final String E_HADOOP_PROPS_UNSET = ENV_HADOOP_HOME + " and "
      + SYSPROP_HADOOP_HOME_DIR + " are unset.";
  static final String E_HADOOP_PROPS_EMPTY = ENV_HADOOP_HOME + " or "
      + SYSPROP_HADOOP_HOME_DIR + " set to an empty string";
  static final String E_NOT_A_WINDOWS_SYSTEM = "Not a Windows system";
 
       if (home.startsWith("\"") && home.endsWith("\"")) {
         home = home.substring(1, home.length()-1);
       }
  /**
   *  Validate the accessibility of the Hadoop home directory.
   *
   * @return A directory that is expected to be the hadoop home directory
   * @throws FileNotFoundException if the specified
   * path is not a reference to a valid directory.
   */
  @VisibleForTesting
  static File checkHadoopHomeInner(String home) throws FileNotFoundException {
    // couldn't find either setting for hadoop's home directory
    if (home == null) {
      throw new FileNotFoundException(E_HADOOP_PROPS_UNSET);
    }
    // strip off leading and trailing double quotes
    while (home.startsWith("\"")) {
      home = home.substring(1);
    }
    while (home.endsWith("\"")) {
      home = home.substring(0, home.length() - 1);
    }
 
       // check that the home setting is actually a directory that exists
       File homedir = new File(home);
       if (!homedir.isAbsolute() || !homedir.exists() || !homedir.isDirectory()) {
         throw new IOException("Hadoop home directory " + homedir
           + " does not exist, is not a directory, or is not an absolute path.");
       }
    // after stripping any quotes, check for home dir being non-empty
    if (home.isEmpty()) {
      throw new FileNotFoundException(E_HADOOP_PROPS_EMPTY);
    }

    // check that the hadoop home dir value
    // is an absolute reference to a directory
    File homedir = new File(home);
    if (!homedir.isAbsolute()) {
      throw new FileNotFoundException("Hadoop home directory " + homedir
          + " " + E_IS_RELATIVE);
    }
    if (!homedir.exists()) {
      throw new FileNotFoundException("Hadoop home directory " + homedir
          + " " + E_DOES_NOT_EXIST);
    }
    if (!homedir.isDirectory()) {
      throw new FileNotFoundException("Hadoop home directory " + homedir
          + " "+ E_NOT_DIRECTORY);
    }
    return homedir;
  }
 
       home = homedir.getCanonicalPath();
  /**
   * The Hadoop home directory.
   */
  private static final File HADOOP_HOME_FILE;

  /**
   * Rethrowable cause for the failure to determine the hadoop
   * home directory
   */
  private static final IOException HADOOP_HOME_DIR_FAILURE_CAUSE;
 
  static {
    File home;
    IOException ex;
    try {
      home = checkHadoopHome();
      ex = null;
     } catch (IOException ioe) {
       if (LOG.isDebugEnabled()) {
         LOG.debug("Failed to detect a valid hadoop home directory", ioe);
       }
      ex = ioe;
       home = null;
     }
    
    return home;
    HADOOP_HOME_FILE = home;
    HADOOP_HOME_DIR_FAILURE_CAUSE = ex;
   }
  private static String HADOOP_HOME_DIR = checkHadoopHome();
 
  // Public getter, throws an exception if HADOOP_HOME failed validation
  // checks and is being referenced downstream.
  public static final String getHadoopHome() throws IOException {
    if (HADOOP_HOME_DIR == null) {
      throw new IOException("Misconfigured HADOOP_HOME cannot be referenced.");
    }
  /**
   * Optionally extend an error message with some OS-specific text.
   * @param message core error message
   * @return error message, possibly with some extra text
   */
  private static String addOsText(String message) {
    return WINDOWS ? (message + " -see " + WINDOWS_PROBLEMS) : message;
  }
 
    return HADOOP_HOME_DIR;
  /**
   * Create a {@code FileNotFoundException} with the inner nested cause set
   * to the given exception. Compensates for the fact that FNFE doesn't
   * have an initializer that takes an exception.
   * @param text error text
   * @param ex inner exception
   * @return a new exception to throw.
   */
  private static FileNotFoundException fileNotFoundException(String text,
      Exception ex) {
    return (FileNotFoundException) new FileNotFoundException(text)
        .initCause(ex);
  }

  /**
   * Get the Hadoop home directory. Raises an exception if not found
   * @return the home dir
   * @throws IOException if the home directory cannot be located.
   */
  public static String getHadoopHome() throws IOException {
    return getHadoopHomeDir().getCanonicalPath();
   }
 
  /** fully qualify the path to a binary that should be in a known hadoop 
  /**
   * Get the Hadoop home directory. If it is invalid,
   * throw an exception.
   * @return a path referring to hadoop home.
   * @throws FileNotFoundException if the directory doesn't exist.
   */
  private static File getHadoopHomeDir() throws FileNotFoundException {
    if (HADOOP_HOME_DIR_FAILURE_CAUSE != null) {
      throw fileNotFoundException(
          addOsText(HADOOP_HOME_DIR_FAILURE_CAUSE.toString()),
          HADOOP_HOME_DIR_FAILURE_CAUSE);
    }
    return HADOOP_HOME_FILE;
  }

  /**
   *  Fully qualify the path to a binary that should be in a known hadoop
    *  bin location. This is primarily useful for disambiguating call-outs 
    *  to executable sub-components of Hadoop to avoid clashes with other 
    *  executables that may be in the path.  Caveat:  this call doesn't 
    *  just format the path to the bin directory.  It also checks for file 
    *  existence of the composed path. The output of this call should be 
    *  cached by callers.
   * */
  public static final String getQualifiedBinPath(String executable) 
  throws IOException {
   *
   * @param executable executable
   * @return executable file reference
   * @throws FileNotFoundException if the path does not exist
   */
  public static File getQualifiedBin(String executable)
      throws FileNotFoundException {
     // construct hadoop bin path to the specified executable
    String fullExeName = HADOOP_HOME_DIR + File.separator + "bin" 
      + File.separator + executable;
    return getQualifiedBinInner(getHadoopHomeDir(), executable);
  }

  /**
   * Inner logic of {@link #getQualifiedBin(String)}, accessible
   * for tests.
   * @param hadoopHomeDir home directory (assumed to be valid)
   * @param executable executable
   * @return path to the binary
   * @throws FileNotFoundException if the executable was not found/valid
   */
  static File getQualifiedBinInner(File hadoopHomeDir, String executable)
      throws FileNotFoundException {
    String binDirText = "Hadoop bin directory ";
    File bin = new File(hadoopHomeDir, "bin");
    if (!bin.exists()) {
      throw new FileNotFoundException(addOsText(binDirText + E_DOES_NOT_EXIST
          + ": " + bin));
    }
    if (!bin.isDirectory()) {
      throw new FileNotFoundException(addOsText(binDirText + E_NOT_DIRECTORY
          + ": " + bin));
    }
 
    File exeFile = new File(fullExeName);
    File exeFile = new File(bin, executable);
     if (!exeFile.exists()) {
      throw new IOException("Could not locate executable " + fullExeName
        + " in the Hadoop binaries.");
      throw new FileNotFoundException(
          addOsText(E_NO_EXECUTABLE + ": " + exeFile));
    }
    if (!exeFile.isFile()) {
      throw new FileNotFoundException(
          addOsText(E_NOT_EXECUTABLE_FILE + ": " + exeFile));
     }
    try {
      return exeFile.getCanonicalFile();
    } catch (IOException e) {
      // this isn't going to happen, because of all the upfront checks.
      // so if it does, it gets converted to a FNFE and rethrown
      throw fileNotFoundException(e.toString(), e);
    }
  }
 
    return exeFile.getCanonicalPath();
  /**
   *  Fully qualify the path to a binary that should be in a known hadoop
   *  bin location. This is primarily useful for disambiguating call-outs
   *  to executable sub-components of Hadoop to avoid clashes with other
   *  executables that may be in the path.  Caveat:  this call doesn't
   *  just format the path to the bin directory.  It also checks for file
   *  existence of the composed path. The output of this call should be
   *  cached by callers.
   *
   * @param executable executable
   * @return executable file reference
   * @throws FileNotFoundException if the path does not exist
   * @throws IOException on path canonicalization failures
   */
  public static String getQualifiedBinPath(String executable)
      throws IOException {
    return getQualifiedBin(executable).getCanonicalPath();
   }
 
  /** a Windows utility to emulate Unix commands */
  public static final String WINUTILS = getWinUtilsPath();
  /**
   * Location of winutils as a string; null if not found.
   * <p>
   * <i>Important: caller must check for this value being null</i>.
   * The lack of such checks has led to many support issues being raised.
   * <p>
   * @deprecated use one of the exception-raising getter methods,
   * specifically {@link #getWinutilsPath()} or {@link #getWinutilsFile()}
   */
  @Deprecated
  public static final String WINUTILS;

  /** Canonical path to winutils, private to Shell. */
  private static final String WINUTILS_PATH;
 
  public static final String getWinUtilsPath() {
    String winUtilsPath = null;
  /** file reference to winutils. */
  private static final File WINUTILS_FILE;
 
    try {
      if (WINDOWS) {
        winUtilsPath = getQualifiedBinPath("winutils.exe");
  /** the exception raised on a failure to init the WINUTILS fields. */
  private static final IOException WINUTILS_FAILURE;

  /*
   * Static WINUTILS_* field initializer.
   * On non-Windows systems sets the paths to null, and
   * adds a specific exception to the failure cause, so
   * that on any attempt to resolve the paths will raise
   * a meaningful exception.
   */
  static {
    IOException ioe = null;
    String path = null;
    File file = null;
    // invariant: either there's a valid file and path,
    // or there is a cached IO exception.
    if (WINDOWS) {
      try {
        file = getQualifiedBin(WINUTILS_EXE);
        path = file.getCanonicalPath();
        ioe = null;
      } catch (IOException e) {
        LOG.warn("Did not find {}: {}", WINUTILS_EXE, e);
        // stack trace comes at debug level
        LOG.debug("Failed to find " + WINUTILS_EXE, e);
        file = null;
        path = null;
        ioe = e;
       }
    } catch (IOException ioe) {
       LOG.error("Failed to locate the winutils binary in the hadoop binary path",
         ioe);
    } else {
      // on a non-windows system, the invariant is kept
      // by adding an explicit exception.
      ioe = new FileNotFoundException(E_NOT_A_WINDOWS_SYSTEM);
    }
    WINUTILS_PATH = path;
    WINUTILS_FILE = file;

    WINUTILS = path;
    WINUTILS_FAILURE = ioe;
  }

  /**
   * Predicate to indicate whether or not the path to winutils is known.
   *
   * If true, then {@link #WINUTILS} is non-null, and both
   * {@link #getWinutilsPath()} and {@link #getWinutilsFile()}
   * will successfully return this value. Always false on non-windows systems.
   * @return true if there is a valid path to the binary
   */
  public static boolean hasWinutilsPath() {
    return WINUTILS_PATH != null;
  }

  /**
   * Locate the winutils binary, or fail with a meaningful
   * exception and stack trace as an RTE.
   * This method is for use in methods which don't explicitly throw
   * an <code>IOException</code>.
   * @return the path to {@link #WINUTILS_EXE}
   * @throws RuntimeException if the path is not resolvable
   */
  public static String getWinutilsPath() {
    if (WINUTILS_FAILURE == null) {
      return WINUTILS_PATH;
    } else {
      throw new RuntimeException(WINUTILS_FAILURE.toString(),
          WINUTILS_FAILURE);
     }
  }
 
    return winUtilsPath;
  /**
   * Get a file reference to winutils.
   * Always raises an exception if there isn't one
   * @return the file instance referring to the winutils bin.
   * @throws FileNotFoundException on any failure to locate that file.
   */
  public static File getWinutilsFile() throws FileNotFoundException {
    if (WINUTILS_FAILURE == null) {
      return WINUTILS_FILE;
    } else {
      // raise a new exception to generate a new stack trace
      throw fileNotFoundException(WINUTILS_FAILURE.toString(),
          WINUTILS_FAILURE);
    }
   }
 
   public static final boolean isBashSupported = checkIsBashSupported();
@@ -412,7 +707,15 @@ private static boolean checkIsBashSupported() {
     return supported;
   }
 
  /**
   * Flag which is true if setsid exists.
   */
   public static final boolean isSetsidAvailable = isSetsidSupported();

  /**
   * Look for <code>setsid</code>.
   * @return true if <code>setsid</code> was present
   */
   private static boolean isSetsidSupported() {
     if (Shell.WINDOWS) {
       return false;
@@ -427,7 +730,8 @@ private static boolean isSetsidSupported() {
       LOG.debug("setsid is not available on this machine. So not using it.");
       setsidSupported = false;
     }  catch (Error err) {
      if (err.getMessage().contains("posix_spawn is not " +
      if (err.getMessage() != null
          && err.getMessage().contains("posix_spawn is not " +
           "a supported process launch mechanism")
           && (Shell.FREEBSD || Shell.MAC)) {
         // HADOOP-11924: This is a workaround to avoid failure of class init
@@ -444,69 +748,86 @@ private static boolean isSetsidSupported() {
     return setsidSupported;
   }
 
  /** Token separator regex used to parse Shell tool outputs */
  /** Token separator regex used to parse Shell tool outputs. */
   public static final String TOKEN_SEPARATOR_REGEX
                 = WINDOWS ? "[|\n\r]" : "[ \t\n\r\f]";
 
  private long    interval;   // refresh interval in msec
  private long    lastTime;   // last time the command was performed
  final private boolean redirectErrorStream; // merge stdout and stderr
  private long interval;   // refresh interval in msec
  private long lastTime;   // last time the command was performed
  private final boolean redirectErrorStream; // merge stdout and stderr
   private Map<String, String> environment; // env for the command execution
   private File dir;
   private Process process; // sub process used to execute the command
   private int exitCode;
 
  /**If or not script finished executing*/
  private volatile AtomicBoolean completed;
  
  public Shell() {
  /** Flag to indicate whether or not the script has finished executing. */
  private final AtomicBoolean completed = new AtomicBoolean(false);

  /**
   * Create an instance with no minimum interval between runs; stderr is
   * not merged with stdout.
   */
  protected Shell() {
     this(0L);
   }
  
  public Shell(long interval) {

  /**
   * Create an instance with a minimum interval between executions; stderr is
   * not merged with stdout.
   * @param interval interval in milliseconds between command executions.
   */
  protected Shell(long interval) {
     this(interval, false);
   }
 
   /**
   * @param interval the minimum duration to wait before re-executing the 
   *        command.
   * Create a shell instance which can be re-executed when the {@link #run()}
   * method is invoked with a given elapsed time between calls.
   *
   * @param interval the minimum duration in milliseconds to wait before
   *        re-executing the command. If set to 0, there is no minimum.
   * @param redirectErrorStream should the error stream be merged with
   *        the normal output stream?
    */
  public Shell(long interval, boolean redirectErrorStream) {
  protected Shell(long interval, boolean redirectErrorStream) {
     this.interval = interval;
    this.lastTime = (interval<0) ? 0 : -interval;
    this.lastTime = (interval < 0) ? 0 : -interval;
     this.redirectErrorStream = redirectErrorStream;
   }
  
  /** set the environment for the command 

  /**
   * Set the environment for the command.
    * @param env Mapping of environment variables
    */
   protected void setEnvironment(Map<String, String> env) {
     this.environment = env;
   }
 
  /** set the working directory 
   * @param dir The directory where the command would be executed
  /**
   * Set the working directory.
   * @param dir The directory where the command will be executed
    */
   protected void setWorkingDirectory(File dir) {
     this.dir = dir;
   }
 
  /** check to see if a command needs to be executed and execute if needed */
  /** Check to see if a command needs to be executed and execute if needed. */
   protected void run() throws IOException {
    if (lastTime + interval > Time.monotonicNow())
    if (lastTime + interval > Time.monotonicNow()) {
       return;
    }
     exitCode = 0; // reset for next run
     runCommand();
   }
 
  /** Run a command */
  /** Run the command. */
   private void runCommand() throws IOException { 
     ProcessBuilder builder = new ProcessBuilder(getExecString());
     Timer timeOutTimer = null;
     ShellTimeoutTimerTask timeoutTimerTask = null;
    timedOut = new AtomicBoolean(false);
    completed = new AtomicBoolean(false);
    
    timedOut.set(false);
    completed.set(false);

     if (environment != null) {
       builder.environment().putAll(this.environment);
     }
@@ -539,11 +860,11 @@ private void runCommand() throws IOException {
     final BufferedReader errReader = 
             new BufferedReader(new InputStreamReader(
                 process.getErrorStream(), Charset.defaultCharset()));
    BufferedReader inReader = 
    BufferedReader inReader =
             new BufferedReader(new InputStreamReader(
                 process.getInputStream(), Charset.defaultCharset()));
     final StringBuffer errMsg = new StringBuffer();
    

     // read error and input streams as this would free up the buffers
     // free the error stream buffer
     Thread errThread = new Thread() {
@@ -641,28 +962,30 @@ private static void joinThread(Thread t) {
     }
   }
 
  /** return an array containing the command name & its parameters */ 
  /** return an array containing the command name and its parameters. */
   protected abstract String[] getExecString();
  

   /** Parse the execution result */
   protected abstract void parseExecResult(BufferedReader lines)
   throws IOException;
 
  /** 
   * Get the environment variable
  /**
   * Get an environment variable.
   * @param env the environment var
   * @return the value or null if it was unset.
    */
   public String getEnvironment(String env) {
     return environment.get(env);
   }
  
  /** get the current sub-process executing the given command 

  /** get the current sub-process executing the given command.
    * @return process executing the command
    */
   public Process getProcess() {
     return process;
   }
 
  /** get the exit code 
  /** get the exit code.
    * @return the exit code of the process
    */
   public int getExitCode() {
@@ -674,12 +997,12 @@ public int getExitCode() {
    */
   public static class ExitCodeException extends IOException {
     private final int exitCode;
    

     public ExitCodeException(int exitCode, String message) {
       super(message);
       this.exitCode = exitCode;
     }
    

     public int getExitCode() {
       return exitCode;
     }
@@ -694,7 +1017,7 @@ public String toString() {
       return sb.toString();
     }
   }
  

   public interface CommandExecutor {
 
     void execute() throws IOException;
@@ -704,33 +1027,33 @@ public String toString() {
     String getOutput() throws IOException;
 
     void close();
    

   }
  

   /**
    * A simple shell command executor.
    * 
   * <code>ShellCommandExecutor</code>should be used in cases where the output 
   * of the command needs no explicit parsing and where the command, working 
   * directory and the environment remains unchanged. The output of the command 
   * <code>ShellCommandExecutor</code>should be used in cases where the output
   * of the command needs no explicit parsing and where the command, working
   * directory and the environment remains unchanged. The output of the command
    * is stored as-is and is expected to be small.
    */
   public static class ShellCommandExecutor extends Shell 
       implements CommandExecutor {
    

     private String[] command;
     private StringBuffer output;
    
    


     public ShellCommandExecutor(String[] execString) {
       this(execString, null);
     }
    

     public ShellCommandExecutor(String[] execString, File dir) {
       this(execString, dir, null);
     }
   
    public ShellCommandExecutor(String[] execString, File dir, 

    public ShellCommandExecutor(String[] execString, File dir,
                                  Map<String, String> env) {
       this(execString, dir, env , 0L);
     }
@@ -746,7 +1069,7 @@ public ShellCommandExecutor(String[] execString, File dir,
      *            key-value pairs specified in the map. If null, the current
      *            environment is not modified.
      * @param timeout Specifies the time in milliseconds, after which the
     *                command will be killed and the status marked as timedout.
     *                command will be killed and the status marked as timed-out.
      *                If 0, the command will not be timed out. 
      */
     public ShellCommandExecutor(String[] execString, File dir, 
@@ -760,10 +1083,19 @@ public ShellCommandExecutor(String[] execString, File dir,
       }
       timeOutInterval = timeout;
     }
        
 
    /** Execute the shell command. */
    /**
     * Execute the shell command.
     * @throws IOException if the command fails, or if the command is
     * not well constructed.
     */
     public void execute() throws IOException {
      for (String s : command) {
        if (s == null) {
          throw new IOException("(null) entry in command string: "
              + StringUtils.join(" ", command));
        }
      }
       this.run();    
     }
 
@@ -781,8 +1113,8 @@ protected void parseExecResult(BufferedReader lines) throws IOException {
         output.append(buf, 0, nRead);
       }
     }
    
    /** Get the output of the shell command.*/

    /** Get the output of the shell command. */
     public String getOutput() {
       return (output == null) ? "" : output.toString();
     }
@@ -813,7 +1145,7 @@ public String toString() {
     public void close() {
     }
   }
  

   /**
    * To check if the passed script to shell command executor timed out or
    * not.
@@ -823,15 +1155,15 @@ public void close() {
   public boolean isTimedOut() {
     return timedOut.get();
   }
  

   /**
   * Set if the command has timed out.
   * Declare that the command has timed out.
    * 
    */
   private void setTimedOut() {
     this.timedOut.set(true);
   }
  

   /** 
    * Static method to execute a shell command. 
    * Covers most of the simple cases without requiring the user to implement  
@@ -842,17 +1174,18 @@ private void setTimedOut() {
   public static String execCommand(String ... cmd) throws IOException {
     return execCommand(null, cmd, 0L);
   }
  
  /** 
   * Static method to execute a shell command. 
   * Covers most of the simple cases without requiring the user to implement  

  /**
   * Static method to execute a shell command.
   * Covers most of the simple cases without requiring the user to implement
    * the <code>Shell</code> interface.
    * @param env the map of environment key=value
    * @param cmd shell command to execute.
    * @param timeout time in milliseconds after which script should be marked timeout
   * @return the output of the executed command.o
   * @return the output of the executed command.
   * @throws IOException on any problem.
    */
  

   public static String execCommand(Map<String, String> env, String[] cmd,
       long timeout) throws IOException {
     ShellCommandExecutor exec = new ShellCommandExecutor(cmd, null, env, 
@@ -861,25 +1194,26 @@ public static String execCommand(Map<String, String> env, String[] cmd,
     return exec.getOutput();
   }
 
  /** 
   * Static method to execute a shell command. 
   * Covers most of the simple cases without requiring the user to implement  
  /**
   * Static method to execute a shell command.
   * Covers most of the simple cases without requiring the user to implement
    * the <code>Shell</code> interface.
    * @param env the map of environment key=value
    * @param cmd shell command to execute.
    * @return the output of the executed command.
   * @throws IOException on any problem.
    */
  public static String execCommand(Map<String,String> env, String ... cmd) 
  public static String execCommand(Map<String,String> env, String ... cmd)
   throws IOException {
     return execCommand(env, cmd, 0L);
   }
  

   /**
    * Timer which is used to timeout scripts spawned off by shell.
    */
   private static class ShellTimeoutTimerTask extends TimerTask {
 
    private Shell shell;
    private final Shell shell;
 
     public ShellTimeoutTimerTask(Shell shell) {
       this.shell = shell;
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java
index f3fb364bf30..1fd036e41d0 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/SysInfoWindows.java
@@ -70,9 +70,10 @@ void reset() {
   }
 
   String getSystemInfoInfoFromShell() {
    ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
        new String[] {Shell.WINUTILS, "systeminfo" });
     try {
      ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
          new String[] {Shell.getWinutilsFile().getCanonicalPath(),
              "systeminfo" });
       shellExecutor.execute();
       return shellExecutor.getOutput();
     } catch (IOException e) {
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java
index 8018946e60a..9d66732b4ec 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java
@@ -52,13 +52,6 @@
   abstract protected String testBaseDir2() throws IOException;
   abstract protected URI testURI();
 
  // Returns true if the filesystem is emulating symlink support. Certain
  // checks will be bypassed if that is the case.
  //
  protected boolean emulatingSymlinksOnWindows() {
    return false;
  }

   protected IOException unwrapException(IOException e) {
     return e;
   }
@@ -235,7 +228,6 @@ public void testOpenResolvesLinks() throws IOException {
   @Test(timeout=10000)
   /** Stat a link to a file */
   public void testStatLinkToFile() throws IOException {
    assumeTrue(!emulatingSymlinksOnWindows());
     Path file = new Path(testBaseDir1()+"/file");
     Path linkToFile = new Path(testBaseDir1()+"/linkToFile");
     createAndWriteFile(file);
@@ -362,11 +354,6 @@ public void testRecursiveLinks() throws IOException {
   private void checkLink(Path linkAbs, Path expectedTarget, Path targetQual)
       throws IOException {
 
    // If we are emulating symlinks then many of these checks will fail
    // so we skip them.
    //
    assumeTrue(!emulatingSymlinksOnWindows());

     Path dir = new Path(testBaseDir1());
     // isFile/Directory
     assertTrue(wrapper.isFile(linkAbs));
@@ -663,7 +650,6 @@ public void testCreateDirViaSymlink() throws IOException {
   @Test(timeout=10000)
   /** Create symlink through a symlink */
   public void testCreateLinkViaLink() throws IOException {
    assumeTrue(!emulatingSymlinksOnWindows());
     Path dir1        = new Path(testBaseDir1());
     Path file        = new Path(testBaseDir1(), "file");
     Path linkToDir   = new Path(testBaseDir2(), "linkToDir");
@@ -706,7 +692,6 @@ public void testListStatusUsingLink() throws IOException {
   @Test(timeout=10000)
   /** Test create symlink using the same path */
   public void testCreateLinkTwice() throws IOException {
    assumeTrue(!emulatingSymlinksOnWindows());
     Path file = new Path(testBaseDir1(), "file");
     Path link = new Path(testBaseDir1(), "linkToFile");
     createAndWriteFile(file);
@@ -895,8 +880,7 @@ public void testRenameSymlinkViaSymlink() throws IOException {
     assertFalse(wrapper.exists(linkViaLink));
     // Check that we didn't rename the link target
     assertTrue(wrapper.exists(file));
    assertTrue(wrapper.getFileLinkStatus(linkNewViaLink).isSymlink() ||
        emulatingSymlinksOnWindows());
    assertTrue(wrapper.getFileLinkStatus(linkNewViaLink).isSymlink());
     readFile(linkNewViaLink);
   }
 
@@ -1034,8 +1018,7 @@ public void testRenameSymlinkNonExistantDest() throws IOException {
     createAndWriteFile(file);
     wrapper.createSymlink(file, link1, false);
     wrapper.rename(link1, link2);
    assertTrue(wrapper.getFileLinkStatus(link2).isSymlink() ||
        emulatingSymlinksOnWindows());
    assertTrue(wrapper.getFileLinkStatus(link2).isSymlink());
     readFile(link2);
     readFile(file);
     assertFalse(wrapper.exists(link1));
@@ -1059,11 +1042,8 @@ public void testRenameSymlinkToExistingFile() throws IOException {
     }
     wrapper.rename(link, file1, Rename.OVERWRITE);
     assertFalse(wrapper.exists(link));

    if (!emulatingSymlinksOnWindows()) {
      assertTrue(wrapper.getFileLinkStatus(file1).isSymlink());
      assertEquals(file2, wrapper.getLinkTarget(file1));
    }
    assertTrue(wrapper.getFileLinkStatus(file1).isSymlink());
    assertEquals(file2, wrapper.getLinkTarget(file1));
   }
 
   @Test(timeout=10000)
@@ -1125,7 +1105,6 @@ public void testRenameSymlinkToItself() throws IOException {
   @Test(timeout=10000)
   /** Rename a symlink */
   public void testRenameSymlink() throws IOException {
    assumeTrue(!emulatingSymlinksOnWindows());
     Path file  = new Path(testBaseDir1(), "file");
     Path link1 = new Path(testBaseDir1(), "linkToFile1");
     Path link2 = new Path(testBaseDir1(), "linkToFile2");
@@ -1223,7 +1202,6 @@ public void testRenameSymlinkToDirItLinksTo() throws IOException {
   @Test(timeout=10000)
   /** Test rename the symlink's target */
   public void testRenameLinkTarget() throws IOException {
    assumeTrue(!emulatingSymlinksOnWindows());
     Path file    = new Path(testBaseDir1(), "file");
     Path fileNew = new Path(testBaseDir1(), "fileNew");
     Path link    = new Path(testBaseDir1(), "linkToFile");
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java
index 3418adeb6b1..5fc0b2dc2aa 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileUtil.java
@@ -983,13 +983,7 @@ public void testSymlinkLength() throws Exception {
     file.delete();
     Assert.assertFalse(file.exists());
 
    if (Shell.WINDOWS && !Shell.isJava7OrAbove()) {
      // On Java6 on Windows, we copied the file
      Assert.assertEquals(data.length, link.length());
    } else {
      // Otherwise, the target file size is zero
      Assert.assertEquals(0, link.length());
    }
    Assert.assertEquals(0, link.length());
 
     link.delete();
     Assert.assertFalse(link.exists());
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
index 602af97f360..ab55f003ae4 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
@@ -68,16 +68,6 @@ protected URI testURI() {
     }
   }
 
  @Override
  protected boolean emulatingSymlinksOnWindows() {
    // Java 6 on Windows has very poor symlink support. Specifically
    // Specifically File#length and File#renameTo do not work as expected.
    // (see HADOOP-9061 for additional details)
    // Hence some symlink tests will be skipped.
    //
    return (Shell.WINDOWS && !Shell.isJava7OrAbove());
  }

   @Override
   public void testCreateDanglingLink() throws IOException {
     // Dangling symlinks are not supported on Windows local file system.
@@ -186,7 +176,6 @@ public void testDanglingLink() throws IOException {
    * file scheme (eg file://host/tmp/test).
    */  
   public void testGetLinkStatusPartQualTarget() throws IOException {
    assumeTrue(!emulatingSymlinksOnWindows());
     Path fileAbs  = new Path(testBaseDir1()+"/file");
     Path fileQual = new Path(testURI().toString(), fileAbs);
     Path dir      = new Path(testBaseDir1());
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
index 5b8eac60f9e..6e279643840 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUserGroupInformation.java
@@ -218,7 +218,8 @@ public void testGetServerSideGroups() throws IOException,
     }
     // get the groups
     pp = Runtime.getRuntime().exec(Shell.WINDOWS ?
      Shell.WINUTILS + " groups -F" : "id -Gn");
      Shell.getWinutilsPath() + " groups -F"
      : "id -Gn");
     br = new BufferedReader(new InputStreamReader(pp.getInputStream()));
     String line = br.readLine();
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
index fc202da6cf7..138d0255ac6 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestShell.java
@@ -17,11 +17,12 @@
  */
 package org.apache.hadoop.util;
 
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
 import org.junit.Assert;
 
 import java.io.BufferedReader;
 import java.io.File;
import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.PrintWriter;
@@ -30,8 +31,31 @@
 import java.lang.management.ThreadMXBean;
 
 import org.apache.hadoop.fs.FileUtil;
import static org.apache.hadoop.util.Shell.*;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
 
public class TestShell extends TestCase {
public class TestShell extends Assert {
  /**
   * Set the timeout for every test
   */
  @Rule
  public Timeout testTimeout = new Timeout(30000);

  @Rule
  public TestName methodName = new TestName();

  private File rootTestDir = new File(System.getProperty("test.build.data", "target/"));

  /**
   * A filename generated uniquely for each test method. The file
   * itself is neither created nor deleted during test setup/teardown.
   */
  private File methodDir;
 
   private static class Command extends Shell {
     private int runCount = 0;
@@ -45,7 +69,7 @@ private Command(long interval) {
       // There is no /bin/echo equivalent on Windows so just launch it as a
       // shell built-in.
       //
      return Shell.WINDOWS ?
      return WINDOWS ?
           (new String[] {"cmd.exe", "/c", "echo", "hello"}) :
           (new String[] {"echo", "hello"});
     }
@@ -60,6 +84,14 @@ public int getRunCount() {
     }
   }
 
  @Before
  public void setup() {
    rootTestDir.mkdirs();
    assertTrue("Not a directory " + rootTestDir, rootTestDir.isDirectory());
    methodDir = new File(rootTestDir, methodName.getMethodName());
  }

  @Test
   public void testInterval() throws IOException {
     testInterval(Long.MIN_VALUE / 60000);  // test a negative interval
     testInterval(0L);  // test a zero interval
@@ -79,6 +111,7 @@ private void assertInString(String string, String search) {
     }
   }
 
  @Test
   public void testShellCommandExecutorToString() throws Throwable {
     Shell.ShellCommandExecutor sce=new Shell.ShellCommandExecutor(
             new String[] { "ls", "..","arg 2"});
@@ -87,30 +120,28 @@ public void testShellCommandExecutorToString() throws Throwable {
     assertInString(command, " .. ");
     assertInString(command, "\"arg 2\"");
   }
  

  @Test
   public void testShellCommandTimeout() throws Throwable {
    if(Shell.WINDOWS) {
      // setExecutable does not work on Windows
      return;
    }
    String rootDir = new File(System.getProperty(
        "test.build.data", "/tmp")).getAbsolutePath();
    Assume.assumeFalse(WINDOWS);
    String rootDir = rootTestDir.getAbsolutePath();
     File shellFile = new File(rootDir, "timeout.sh");
     String timeoutCommand = "sleep 4; echo \"hello\"";
    PrintWriter writer = new PrintWriter(new FileOutputStream(shellFile));
    writer.println(timeoutCommand);
    writer.close();
    Shell.ShellCommandExecutor shexc;
    try (PrintWriter writer = new PrintWriter(new FileOutputStream(shellFile))) {
      writer.println(timeoutCommand);
      writer.close();
    }
     FileUtil.setExecutable(shellFile, true);
    Shell.ShellCommandExecutor shexc 
    = new Shell.ShellCommandExecutor(new String[]{shellFile.getAbsolutePath()},
                                      null, null, 100);
    shexc = new Shell.ShellCommandExecutor(new String[]{shellFile.getAbsolutePath()},
        null, null, 100);
     try {
       shexc.execute();
     } catch (Exception e) {
       //When timing out exception is thrown.
     }
     shellFile.delete();
    assertTrue("Script didnt not timeout" , shexc.isTimedOut());
    assertTrue("Script did not timeout" , shexc.isTimedOut());
   }
   
   private static int countTimerThreads() {
@@ -129,7 +160,8 @@ private static int countTimerThreads() {
     }
     return count;
   }
  

  @Test
   public void testShellCommandTimerLeak() throws Exception {
     String quickCommand[] = new String[] {"/bin/sleep", "100"};
     
@@ -152,16 +184,17 @@ public void testShellCommandTimerLeak() throws Exception {
     assertEquals(timersBefore, timersAfter);
   }
 
  @Test
   public void testGetCheckProcessIsAliveCommand() throws Exception {
     String anyPid = "9999";
    String[] checkProcessAliveCommand = Shell.getCheckProcessIsAliveCommand(
    String[] checkProcessAliveCommand = getCheckProcessIsAliveCommand(
         anyPid);
 
     String[] expectedCommand;
 
     if (Shell.WINDOWS) {
       expectedCommand =
          new String[]{ Shell.WINUTILS, "task", "isAlive", anyPid };
          new String[]{getWinutilsPath(), "task", "isAlive", anyPid };
     } else if (Shell.isSetsidAvailable) {
       expectedCommand = new String[] { "bash", "-c", "kill -0 -- -" + anyPid };
     } else {
@@ -170,17 +203,18 @@ public void testGetCheckProcessIsAliveCommand() throws Exception {
     Assert.assertArrayEquals(expectedCommand, checkProcessAliveCommand);
   }
 
  @Test
   public void testGetSignalKillCommand() throws Exception {
     String anyPid = "9999";
     int anySignal = 9;
    String[] checkProcessAliveCommand = Shell.getSignalKillCommand(anySignal,
    String[] checkProcessAliveCommand = getSignalKillCommand(anySignal,
         anyPid);
 
     String[] expectedCommand;
 
     if (Shell.WINDOWS) {
       expectedCommand =
          new String[]{ Shell.WINUTILS, "task", "isAlive", anyPid };
          new String[]{getWinutilsPath(), "task", "kill", anyPid };
     } else if (Shell.isSetsidAvailable) {
       expectedCommand = new String[] { "bash", "-c", "kill -9 -- -" + anyPid };
     } else {
@@ -188,7 +222,6 @@ public void testGetSignalKillCommand() throws Exception {
     }
     Assert.assertArrayEquals(expectedCommand, checkProcessAliveCommand);
   }
  
 
   private void testInterval(long interval) throws IOException {
     Command command = new Command(interval);
@@ -203,4 +236,190 @@ private void testInterval(long interval) throws IOException {
       assertEquals(2, command.getRunCount());
     }
   }

  @Test
  public void testHadoopHomeUnset() throws Throwable {
    assertHomeResolveFailed(null, "unset");
  }

  @Test
  public void testHadoopHomeEmpty() throws Throwable {
    assertHomeResolveFailed("", E_HADOOP_PROPS_EMPTY);
  }

  @Test
  public void testHadoopHomeEmptyDoubleQuotes() throws Throwable {
    assertHomeResolveFailed("\"\"", E_HADOOP_PROPS_EMPTY);
  }

  @Test
  public void testHadoopHomeEmptySingleQuote() throws Throwable {
    assertHomeResolveFailed("\"", E_HADOOP_PROPS_EMPTY);
  }

  @Test
  public void testHadoopHomeValid() throws Throwable {
    File f = checkHadoopHomeInner(rootTestDir.getCanonicalPath());
    assertEquals(rootTestDir, f);
  }

  @Test
  public void testHadoopHomeValidQuoted() throws Throwable {
    File f = checkHadoopHomeInner('"'+ rootTestDir.getCanonicalPath() + '"');
    assertEquals(rootTestDir, f);
  }

  @Test
  public void testHadoopHomeNoDir() throws Throwable {
    assertHomeResolveFailed(methodDir.getCanonicalPath(), E_DOES_NOT_EXIST);
  }

  @Test
  public void testHadoopHomeNotADir() throws Throwable {
    File touched = touch(methodDir);
    try {
      assertHomeResolveFailed(touched.getCanonicalPath(), E_NOT_DIRECTORY);
    } finally {
      FileUtils.deleteQuietly(touched);
    }
  }

  @Test
  public void testHadoopHomeRelative() throws Throwable {
    assertHomeResolveFailed("./target", E_IS_RELATIVE);
  }

  @Test
  public void testBinDirMissing() throws Throwable {
    FileNotFoundException ex = assertWinutilsResolveFailed(methodDir,
        E_DOES_NOT_EXIST);
    assertInString(ex.toString(), "Hadoop bin directory");
  }

  @Test
  public void testHadoopBinNotADir() throws Throwable {
    File bin = new File(methodDir, "bin");
    touch(bin);
    try {
      assertWinutilsResolveFailed(methodDir, E_NOT_DIRECTORY);
    } finally {
      FileUtils.deleteQuietly(methodDir);
    }
  }

  @Test
  public void testBinWinUtilsFound() throws Throwable {
    try {
      File bin = new File(methodDir, "bin");
      File winutils = new File(bin, WINUTILS_EXE);
      touch(winutils);
      assertEquals(winutils.getCanonicalPath(),
          getQualifiedBinInner(methodDir, WINUTILS_EXE).getCanonicalPath());
    } finally {
      FileUtils.deleteQuietly(methodDir);
    }
  }

  @Test
  public void testBinWinUtilsNotAFile() throws Throwable {
    try {
      File bin = new File(methodDir, "bin");
      File winutils = new File(bin, WINUTILS_EXE);
      winutils.mkdirs();
      assertWinutilsResolveFailed(methodDir, E_NOT_EXECUTABLE_FILE);
    } finally {
      FileUtils.deleteDirectory(methodDir);
    }
  }

  /**
   * This test takes advantage of the invariant winutils path is valid
   * or access to it will raise an exception holds on Linux, and without
   * any winutils binary even if HADOOP_HOME points to a real hadoop
   * directory, the exception reporting can be validated
   */
  @Test
  public void testNoWinutilsOnUnix() throws Throwable {
    Assume.assumeFalse(WINDOWS);
    try {
      getWinutilsFile();
    } catch (FileNotFoundException ex) {
      assertExContains(ex, E_NOT_A_WINDOWS_SYSTEM);
    }
    try {
      getWinutilsPath();
    } catch (RuntimeException ex) {
      assertExContains(ex, E_NOT_A_WINDOWS_SYSTEM);
      if ( ex.getCause() == null
          || !(ex.getCause() instanceof FileNotFoundException)) {
        throw ex;
      }
    }
  }

  /**
   * Touch a file; creating parent dirs on demand.
   * @param path path of file
   * @return the file created
   * @throws IOException on any failure to write
   */
  private File touch(File path) throws IOException {
    path.getParentFile().mkdirs();
    FileUtils.writeByteArrayToFile(path, new byte[]{});
    return path;
  }

  /**
   * Assert that an attept to resolve the hadoop home dir failed with
   * an expected text in the exception string value.
   * @param path input
   * @param expectedText expected exception text
   * @return the caught exception
   * @throws FileNotFoundException any FileNotFoundException that was thrown
   * but which did not contain the expected text
   */
  private FileNotFoundException assertHomeResolveFailed(String path,
      String expectedText) throws Exception {
    try {
      File f = checkHadoopHomeInner(path);
      fail("Expected an exception with the text `" + expectedText + "`"
          + " -but got the path " + f);
      // unreachable
      return null;
    } catch (FileNotFoundException ex) {
      assertExContains(ex, expectedText);
      return ex;
    }
  }

  /**
   * Assert that an attept to resolve the {@code bin/winutils.exe} failed with
   * an expected text in the exception string value.
   * @param hadoopHome hadoop home directory
   * @param expectedText expected exception text
   * @return the caught exception
   * @throws Exception any Exception that was thrown
   * but which did not contain the expected text
   */
  private FileNotFoundException assertWinutilsResolveFailed(File hadoopHome,
      String expectedText) throws Exception {
    try {
      File f = getQualifiedBinInner(hadoopHome, WINUTILS_EXE);
      fail("Expected an exception with the text `" + expectedText + "`"
          + " -but got the path " + f);
      // unreachable
      return null;
    } catch (FileNotFoundException ex) {
      assertExContains(ex, expectedText);
      return ex;
    }
  }

  private void assertExContains(Exception ex, String expectedText)
      throws Exception {
    if (!ex.toString().contains(expectedText)) {
      throw ex;
    }
  }

 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java
index 987c7068a82..6fc8969819e 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/util/TestWinUtils.java
@@ -45,13 +45,18 @@
 
   private static final Log LOG = LogFactory.getLog(TestWinUtils.class);
   private static File TEST_DIR = new File(System.getProperty("test.build.data",
      "/tmp"), TestWinUtils.class.getSimpleName());
      "target"+File.pathSeparator + "tmp"), TestWinUtils.class.getSimpleName());

  String winutils;
 
   @Before
  public void setUp() {
  public void setUp() throws IOException {
     // Not supported on non-Windows platforms
     assumeTrue(Shell.WINDOWS);
     TEST_DIR.mkdirs();
    assertTrue("Failed to create Test directory " + TEST_DIR,
        TEST_DIR.isDirectory() );
    winutils = Shell.getWinutilsPath();
   }
 
   @After
@@ -59,46 +64,55 @@ public void tearDown() throws IOException {
     FileUtil.fullyDelete(TEST_DIR);
   }
 
  private void requireWinutils() throws IOException {
    Shell.getWinutilsPath();
  }

   // Helper routine that writes the given content to the file.
   private void writeFile(File file, String content) throws IOException {
     byte[] data = content.getBytes();
    FileOutputStream os = new FileOutputStream(file);
    os.write(data);
    os.close();
    try (FileOutputStream os = new FileOutputStream(file)) {
      os.write(data);
      os.close();
    }
   }
 
   // Helper routine that reads the first 100 bytes from the file.
   private String readFile(File file) throws IOException {
    FileInputStream fos = new FileInputStream(file);
    byte[] b = new byte[100];
    fos.read(b);
    return b.toString();
    byte[] b;
    try (FileInputStream fos = new FileInputStream(file)) {
      b = new byte[100];
      int count = fos.read(b);
      assertEquals(100, count);
    }
    return new String(b);
   }
 
   @Test (timeout = 30000)
   public void testLs() throws IOException {
    requireWinutils();
     final String content = "6bytes";
     final int contentSize = content.length();
     File testFile = new File(TEST_DIR, "file1");
     writeFile(testFile, content);
 
     // Verify permissions and file name return tokens
    String testPath = testFile.getCanonicalPath();
     String output = Shell.execCommand(
        Shell.WINUTILS, "ls", testFile.getCanonicalPath());
        winutils, "ls", testPath);
     String[] outputArgs = output.split("[ \r\n]");
    assertTrue(outputArgs[0].equals("-rwx------"));
    assertTrue(outputArgs[outputArgs.length - 1]
        .equals(testFile.getCanonicalPath()));
    assertEquals("-rwx------", outputArgs[0]);
    assertEquals(outputArgs[outputArgs.length - 1], testPath);
 
     // Verify most tokens when using a formatted output (other tokens
     // will be verified with chmod/chown)
     output = Shell.execCommand(
        Shell.WINUTILS, "ls", "-F", testFile.getCanonicalPath());
        winutils, "ls", "-F", testPath);
     outputArgs = output.split("[|\r\n]");
     assertEquals(9, outputArgs.length);
    assertTrue(outputArgs[0].equals("-rwx------"));
    assertEquals("-rwx------", outputArgs[0]);
     assertEquals(contentSize, Long.parseLong(outputArgs[4]));
    assertTrue(outputArgs[8].equals(testFile.getCanonicalPath()));
    assertEquals(outputArgs[8], testPath);
 
     testFile.delete();
     assertFalse(testFile.exists());
@@ -106,41 +120,42 @@ public void testLs() throws IOException {
 
   @Test (timeout = 30000)
   public void testGroups() throws IOException {
    requireWinutils();
     String currentUser = System.getProperty("user.name");
 
     // Verify that groups command returns information about the current user
     // groups when invoked with no args
     String outputNoArgs = Shell.execCommand(
        Shell.WINUTILS, "groups").trim();
        winutils, "groups").trim();
     String output = Shell.execCommand(
        Shell.WINUTILS, "groups", currentUser).trim();
        winutils, "groups", currentUser).trim();
     assertEquals(output, outputNoArgs);
 
     // Verify that groups command with the -F flag returns the same information
     String outputFormat = Shell.execCommand(
        Shell.WINUTILS, "groups", "-F", currentUser).trim();
        winutils, "groups", "-F", currentUser).trim();
     outputFormat = outputFormat.replace("|", " ");
     assertEquals(output, outputFormat);
   }
 
   private void chmod(String mask, File file) throws IOException {
     Shell.execCommand(
        Shell.WINUTILS, "chmod", mask, file.getCanonicalPath());
        winutils, "chmod", mask, file.getCanonicalPath());
   }
 
   private void chmodR(String mask, File file) throws IOException {
     Shell.execCommand(
        Shell.WINUTILS, "chmod", "-R", mask, file.getCanonicalPath());
        winutils, "chmod", "-R", mask, file.getCanonicalPath());
   }
 
   private String ls(File file) throws IOException {
     return Shell.execCommand(
        Shell.WINUTILS, "ls", file.getCanonicalPath());
        winutils, "ls", file.getCanonicalPath());
   }
 
   private String lsF(File file) throws IOException {
     return Shell.execCommand(
        Shell.WINUTILS, "ls", "-F", file.getCanonicalPath());
        winutils, "ls", "-F", file.getCanonicalPath());
   }
 
   private void assertPermissions(File file, String expected)
@@ -151,6 +166,7 @@ private void assertPermissions(File file, String expected)
 
   private void testChmodInternal(String mode, String expectedPerm)
       throws IOException {
    requireWinutils();
     File a = new File(TEST_DIR, "file1");
     assertTrue(a.createNewFile());
 
@@ -168,6 +184,7 @@ private void testChmodInternal(String mode, String expectedPerm)
   }
 
   private void testNewFileChmodInternal(String expectedPerm) throws IOException {
    requireWinutils();
     // Create a new directory
     File dir = new File(TEST_DIR, "dir1");
 
@@ -190,6 +207,7 @@ private void testNewFileChmodInternal(String expectedPerm) throws IOException {
 
   private void testChmodInternalR(String mode, String expectedPerm,
       String expectedPermx) throws IOException {
    requireWinutils();
     // Setup test folder hierarchy
     File a = new File(TEST_DIR, "a");
     assertTrue(a.mkdir());
@@ -226,6 +244,7 @@ private void testChmodInternalR(String mode, String expectedPerm,
 
   @Test (timeout = 30000)
   public void testBasicChmod() throws IOException {
    requireWinutils();
     // - Create a file.
     // - Change mode to 377 so owner does not have read permission.
     // - Verify the owner truly does not have the permissions to read.
@@ -249,7 +268,7 @@ public void testBasicChmod() throws IOException {
  
     try {
       writeFile(a, "test");
      assertFalse("writeFile should have failed!", true);
      fail("writeFile should have failed!");
     } catch (IOException ex) {
       LOG.info("Expected: Failed write to a file with permissions 577");
     }
@@ -261,14 +280,14 @@ public void testBasicChmod() throws IOException {
     // - Change mode to 677 so owner does not have execute permission.
     // - Verify the owner truly does not have the permissions to execute the file.
 
    File winutilsFile = new File(Shell.WINUTILS);
    File winutilsFile = Shell.getWinutilsFile();
     File aExe = new File(TEST_DIR, "a.exe");
     FileUtils.copyFile(winutilsFile, aExe);
     chmod("677", aExe);
 
     try {
       Shell.execCommand(aExe.getCanonicalPath(), "ls");
      assertFalse("executing " + aExe + " should have failed!", true);
      fail("executing " + aExe + " should have failed!");
     } catch (IOException ex) {
       LOG.info("Expected: Failed to execute a file with permissions 677");
     }
@@ -278,6 +297,7 @@ public void testBasicChmod() throws IOException {
   /** Validate behavior of chmod commands on directories on Windows. */
   @Test (timeout = 30000)
   public void testBasicChmodOnDir() throws IOException {
    requireWinutils();
     // Validate that listing a directory with no read permission fails
     File a = new File(TEST_DIR, "a");
     File b = new File(a, "b");
@@ -287,8 +307,7 @@ public void testBasicChmodOnDir() throws IOException {
     // Remove read permissions on directory a
     chmod("300", a);
     String[] files = a.list();
    assertTrue("Listing a directory without read permission should fail",
        null == files);
    assertNull("Listing a directory without read permission should fail", files);
 
     // restore permissions
     chmod("700", a);
@@ -306,7 +325,7 @@ public void testBasicChmodOnDir() throws IOException {
       // FILE_WRITE_DATA/FILE_ADD_FILE privilege is denied on
       // the dir.
       c.createNewFile();
      assertFalse("writeFile should have failed!", true);
      fail("writeFile should have failed!");
     } catch (IOException ex) {
       LOG.info("Expected: Failed to create a file when directory "
           + "permissions are 577");
@@ -356,6 +375,7 @@ public void testBasicChmodOnDir() throws IOException {
 
   @Test (timeout = 30000)
   public void testChmod() throws IOException {
    requireWinutils();
     testChmodInternal("7", "-------rwx");
     testChmodInternal("70", "----rwx---");
     testChmodInternal("u-x,g+r,o=g", "-rw-r--r--");
@@ -376,7 +396,7 @@ public void testChmod() throws IOException {
 
   private void chown(String userGroup, File file) throws IOException {
     Shell.execCommand(
        Shell.WINUTILS, "chown", userGroup, file.getCanonicalPath());
        winutils, "chown", userGroup, file.getCanonicalPath());
   }
 
   private void assertOwners(File file, String expectedUser,
@@ -390,6 +410,7 @@ private void assertOwners(File file, String expectedUser,
 
   @Test (timeout = 30000)
   public void testChown() throws IOException {
    requireWinutils();
     File a = new File(TEST_DIR, "a");
     assertTrue(a.createNewFile());
     String username = System.getProperty("user.name");
@@ -415,12 +436,13 @@ public void testChown() throws IOException {
 
   @Test (timeout = 30000)
   public void testSymlinkRejectsForwardSlashesInLink() throws IOException {
    requireWinutils();
     File newFile = new File(TEST_DIR, "file");
     assertTrue(newFile.createNewFile());
     String target = newFile.getPath();
     String link = new File(TEST_DIR, "link").getPath().replaceAll("\\\\", "/");
     try {
      Shell.execCommand(Shell.WINUTILS, "symlink", link, target);
      Shell.execCommand(winutils, "symlink", link, target);
       fail(String.format("did not receive expected failure creating symlink "
         + "with forward slashes in link: link = %s, target = %s", link, target));
     } catch (IOException e) {
@@ -431,12 +453,13 @@ public void testSymlinkRejectsForwardSlashesInLink() throws IOException {
 
   @Test (timeout = 30000)
   public void testSymlinkRejectsForwardSlashesInTarget() throws IOException {
    requireWinutils();
     File newFile = new File(TEST_DIR, "file");
     assertTrue(newFile.createNewFile());
     String target = newFile.getPath().replaceAll("\\\\", "/");
     String link = new File(TEST_DIR, "link").getPath();
     try {
      Shell.execCommand(Shell.WINUTILS, "symlink", link, target);
      Shell.execCommand(winutils, "symlink", link, target);
       fail(String.format("did not receive expected failure creating symlink "
         + "with forward slashes in target: link = %s, target = %s", link, target));
     } catch (IOException e) {
@@ -447,6 +470,7 @@ public void testSymlinkRejectsForwardSlashesInTarget() throws IOException {
 
   @Test (timeout = 30000)
   public void testReadLink() throws IOException {
    requireWinutils();
     // Create TEST_DIR\dir1\file1.txt
     //
     File dir1 = new File(TEST_DIR, "dir1");
@@ -462,18 +486,18 @@ public void testReadLink() throws IOException {
     // symlink to file1.txt.
     //
     Shell.execCommand(
        Shell.WINUTILS, "symlink", dirLink.toString(), dir1.toString());
        winutils, "symlink", dirLink.toString(), dir1.toString());
     Shell.execCommand(
        Shell.WINUTILS, "symlink", fileLink.toString(), file1.toString());
        winutils, "symlink", fileLink.toString(), file1.toString());
 
     // Read back the two links and ensure we get what we expected.
     //
    String readLinkOutput = Shell.execCommand(Shell.WINUTILS,
    String readLinkOutput = Shell.execCommand(winutils,
         "readlink",
         dirLink.toString());
     assertThat(readLinkOutput, equalTo(dir1.toString()));
 
    readLinkOutput = Shell.execCommand(Shell.WINUTILS,
    readLinkOutput = Shell.execCommand(winutils,
         "readlink",
         fileLink.toString());
     assertThat(readLinkOutput, equalTo(file1.toString()));
@@ -483,7 +507,7 @@ public void testReadLink() throws IOException {
     try {
       // No link name specified.
       //
      Shell.execCommand(Shell.WINUTILS, "readlink", "");
      Shell.execCommand(winutils, "readlink", "");
       fail("Failed to get Shell.ExitCodeException when reading bad symlink");
     } catch (Shell.ExitCodeException ece) {
       assertThat(ece.getExitCode(), is(1));
@@ -492,7 +516,7 @@ public void testReadLink() throws IOException {
     try {
       // Bad link name.
       //
      Shell.execCommand(Shell.WINUTILS, "readlink", "ThereIsNoSuchLink");
      Shell.execCommand(winutils, "readlink", "ThereIsNoSuchLink");
       fail("Failed to get Shell.ExitCodeException when reading bad symlink");
     } catch (Shell.ExitCodeException ece) {
       assertThat(ece.getExitCode(), is(1));
@@ -501,7 +525,7 @@ public void testReadLink() throws IOException {
     try {
       // Non-symlink directory target.
       //
      Shell.execCommand(Shell.WINUTILS, "readlink", dir1.toString());
      Shell.execCommand(winutils, "readlink", dir1.toString());
       fail("Failed to get Shell.ExitCodeException when reading bad symlink");
     } catch (Shell.ExitCodeException ece) {
       assertThat(ece.getExitCode(), is(1));
@@ -510,7 +534,7 @@ public void testReadLink() throws IOException {
     try {
       // Non-symlink file target.
       //
      Shell.execCommand(Shell.WINUTILS, "readlink", file1.toString());
      Shell.execCommand(winutils, "readlink", file1.toString());
       fail("Failed to get Shell.ExitCodeException when reading bad symlink");
     } catch (Shell.ExitCodeException ece) {
       assertThat(ece.getExitCode(), is(1));
@@ -519,7 +543,7 @@ public void testReadLink() throws IOException {
     try {
       // Too many parameters.
       //
      Shell.execCommand(Shell.WINUTILS, "readlink", "a", "b");
      Shell.execCommand(winutils, "readlink", "a", "b");
       fail("Failed to get Shell.ExitCodeException with bad parameters");
     } catch (Shell.ExitCodeException ece) {
       assertThat(ece.getExitCode(), is(1));
@@ -529,6 +553,7 @@ public void testReadLink() throws IOException {
   @SuppressWarnings("deprecation")
   @Test(timeout=10000)
   public void testTaskCreate() throws IOException {
    requireWinutils();
     File batch = new File(TEST_DIR, "testTaskCreate.cmd");
     File proof = new File(TEST_DIR, "testTaskCreate.out");
     FileWriter fw = new FileWriter(batch);
@@ -538,7 +563,7 @@ public void testTaskCreate() throws IOException {
     
     assertFalse(proof.exists());
     
    Shell.execCommand(Shell.WINUTILS, "task", "create", "testTaskCreate" + testNumber, 
    Shell.execCommand(winutils, "task", "create", "testTaskCreate" + testNumber,
         batch.getAbsolutePath());
     
     assertTrue(proof.exists());
@@ -550,30 +575,31 @@ public void testTaskCreate() throws IOException {
 
   @Test (timeout = 30000)
   public void testTaskCreateWithLimits() throws IOException {
    requireWinutils();
     // Generate a unique job id
     String jobId = String.format("%f", Math.random());
 
     // Run a task without any options
    String out = Shell.execCommand(Shell.WINUTILS, "task", "create",
    String out = Shell.execCommand(winutils, "task", "create",
         "job" + jobId, "cmd /c echo job" + jobId);
     assertTrue(out.trim().equals("job" + jobId));
 
     // Run a task without any limits
     jobId = String.format("%f", Math.random());
    out = Shell.execCommand(Shell.WINUTILS, "task", "create", "-c", "-1", "-m",
    out = Shell.execCommand(winutils, "task", "create", "-c", "-1", "-m",
         "-1", "job" + jobId, "cmd /c echo job" + jobId);
     assertTrue(out.trim().equals("job" + jobId));
 
     // Run a task with limits (128MB should be enough for a cmd)
     jobId = String.format("%f", Math.random());
    out = Shell.execCommand(Shell.WINUTILS, "task", "create", "-c", "10000", "-m",
    out = Shell.execCommand(winutils, "task", "create", "-c", "10000", "-m",
         "128", "job" + jobId, "cmd /c echo job" + jobId);
     assertTrue(out.trim().equals("job" + jobId));
 
     // Run a task without enough memory
     try {
       jobId = String.format("%f", Math.random());
      out = Shell.execCommand(Shell.WINUTILS, "task", "create", "-m", "128", "job"
      out = Shell.execCommand(winutils, "task", "create", "-m", "128", "job"
           + jobId, "java -Xmx256m -version");
       fail("Failed to get Shell.ExitCodeException with insufficient memory");
     } catch (Shell.ExitCodeException ece) {
@@ -584,7 +610,7 @@ public void testTaskCreateWithLimits() throws IOException {
     //
     try {
       jobId = String.format("%f", Math.random());
      Shell.execCommand(Shell.WINUTILS, "task", "create", "-c", "-1", "-m",
      Shell.execCommand(winutils, "task", "create", "-c", "-1", "-m",
           "-1", "foo", "job" + jobId, "cmd /c echo job" + jobId);
       fail("Failed to get Shell.ExitCodeException with bad parameters");
     } catch (Shell.ExitCodeException ece) {
@@ -593,7 +619,7 @@ public void testTaskCreateWithLimits() throws IOException {
 
     try {
       jobId = String.format("%f", Math.random());
      Shell.execCommand(Shell.WINUTILS, "task", "create", "-c", "-m", "-1",
      Shell.execCommand(winutils, "task", "create", "-c", "-m", "-1",
           "job" + jobId, "cmd /c echo job" + jobId);
       fail("Failed to get Shell.ExitCodeException with bad parameters");
     } catch (Shell.ExitCodeException ece) {
@@ -602,7 +628,7 @@ public void testTaskCreateWithLimits() throws IOException {
 
     try {
       jobId = String.format("%f", Math.random());
      Shell.execCommand(Shell.WINUTILS, "task", "create", "-c", "foo",
      Shell.execCommand(winutils, "task", "create", "-c", "foo",
           "job" + jobId, "cmd /c echo job" + jobId);
       fail("Failed to get Shell.ExitCodeException with bad parameters");
     } catch (Shell.ExitCodeException ece) {
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java
index ebe8df12547..41b26f418c6 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-common/src/main/java/org/apache/hadoop/yarn/util/WindowsBasedProcessTree.java
@@ -51,8 +51,11 @@
     
   public static boolean isAvailable() {
     if (Shell.WINDOWS) {
      if (!Shell.hasWinutilsPath()) {
        return false;
      }
       ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
          new String[] { Shell.WINUTILS, "help" });
          new String[] { Shell.getWinutilsPath(), "help" });
       try {
         shellExecutor.execute();
       } catch (IOException e) {
@@ -75,9 +78,10 @@ public WindowsBasedProcessTree(String pid) {
 
   // helper method to override while testing
   String getAllProcessInfoFromShell() {
    ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
        new String[] { Shell.WINUTILS, "task", "processList", taskProcessId });
     try {
      ShellCommandExecutor shellExecutor = new ShellCommandExecutor(
          new String[] {Shell.getWinutilsFile().getCanonicalPath(),
              "task", "processList", taskProcessId });
       shellExecutor.execute();
       return shellExecutor.getOutput();
     } catch (IOException e) {
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java
index 68bfbbfdd14..a83ef844de8 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/ContainerExecutor.java
@@ -401,7 +401,7 @@ protected Path getPidFilePath(ContainerId containerId) {
           cpuRate = Math.min(10000, (int) (containerCpuPercentage * 100));
         }
       }
      return new String[] { Shell.WINUTILS, "task", "create", "-m",
      return new String[] { Shell.getWinutilsPath(), "task", "create", "-m",
           String.valueOf(memory), "-c", String.valueOf(cpuRate), groupId,
           "cmd /c " + command };
     } else {
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java
index fd2e31b7b48..70fdcdaa5db 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/WindowsSecureContainerExecutor.java
@@ -578,7 +578,8 @@ public void setConf(Configuration conf) {
       LOG.debug(String.format("getRunCommand: %s exists:%b", 
           command, f.exists()));
     }
    return new String[] { Shell.WINUTILS, "task", "createAsUser", groupId, 
    return new String[] { Shell.getWinutilsPath(), "task",
        "createAsUser", groupId,
         userName, pidFile.toString(), "cmd /c " + command };
   }
   
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java
index 9718098a6be..43493327ef7 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/ContainerLaunch.java
@@ -24,7 +24,6 @@
 import java.io.DataOutputStream;
 import java.io.File;
 import java.io.IOException;
import java.io.OutputStream;
 import java.io.PrintStream;
 import java.nio.ByteBuffer;
 import java.util.ArrayList;
@@ -747,16 +746,9 @@ protected void link(Path src, Path dst) throws IOException {
       File srcFile = new File(src.toUri().getPath());
       String srcFileStr = srcFile.getPath();
       String dstFileStr = new File(dst.toString()).getPath();
      // If not on Java7+ on Windows, then copy file instead of symlinking.
      // See also FileUtil#symLink for full explanation.
      if (!Shell.isJava7OrAbove() && srcFile.isFile()) {
        lineWithLenCheck(String.format("@copy \"%s\" \"%s\"", srcFileStr, dstFileStr));
        errorCheck();
      } else {
        lineWithLenCheck(String.format("@%s symlink \"%s\" \"%s\"", Shell.WINUTILS,
          dstFileStr, srcFileStr));
        errorCheck();
      }
      lineWithLenCheck(String.format("@%s symlink \"%s\" \"%s\"",
          Shell.getWinutilsPath(), dstFileStr, srcFileStr));
      errorCheck();
     }
 
     @Override
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/TestContainerExecutor.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/TestContainerExecutor.java
index 2ebf4ec289b..bc87b0331b6 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/TestContainerExecutor.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/TestContainerExecutor.java
@@ -34,6 +34,7 @@
 import static org.junit.Assert.*;
 import static org.junit.Assume.assumeTrue;
 
@SuppressWarnings("deprecation")
 public class TestContainerExecutor {
   
   private ContainerExecutor containerExecutor = new DefaultContainerExecutor();
diff --git a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java
index ea6bb1dc19d..f85b01e31f0 100644
-- a/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java
++ b/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/test/java/org/apache/hadoop/yarn/server/nodemanager/containermanager/launcher/TestContainerLaunch.java
@@ -978,7 +978,7 @@ public void testWindowsShellScriptBuilderCommand() throws IOException {
     Assume.assumeTrue(Shell.WINDOWS);
 
     // The tests are built on assuming 8191 max command line length
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGHT);
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGTH);
 
     ShellScriptBuilder builder = ShellScriptBuilder.create();
 
@@ -987,11 +987,11 @@ public void testWindowsShellScriptBuilderCommand() throws IOException {
         org.apache.commons.lang.StringUtils.repeat("A", 1024)));
     builder.command(Arrays.asList(
         org.apache.commons.lang.StringUtils.repeat(
            "E", Shell.WINDOWS_MAX_SHELL_LENGHT - callCmd.length())));
            "E", Shell.WINDOWS_MAX_SHELL_LENGTH - callCmd.length())));
     try {
       builder.command(Arrays.asList(
           org.apache.commons.lang.StringUtils.repeat(
              "X", Shell.WINDOWS_MAX_SHELL_LENGHT -callCmd.length() + 1)));
              "X", Shell.WINDOWS_MAX_SHELL_LENGTH -callCmd.length() + 1)));
       fail("longCommand was expected to throw");
     } catch(IOException e) {
       assertThat(e.getMessage(), containsString(expectedMessage));
@@ -1026,17 +1026,17 @@ public void testWindowsShellScriptBuilderEnv() throws IOException {
     Assume.assumeTrue(Shell.WINDOWS);
 
     // The tests are built on assuming 8191 max command line length
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGHT);
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGTH);
 
     ShellScriptBuilder builder = ShellScriptBuilder.create();
 
     // test env
     builder.env("somekey", org.apache.commons.lang.StringUtils.repeat("A", 1024));
     builder.env("somekey", org.apache.commons.lang.StringUtils.repeat(
        "A", Shell.WINDOWS_MAX_SHELL_LENGHT - ("@set somekey=").length()));
        "A", Shell.WINDOWS_MAX_SHELL_LENGTH - ("@set somekey=").length()));
     try {
       builder.env("somekey", org.apache.commons.lang.StringUtils.repeat(
          "A", Shell.WINDOWS_MAX_SHELL_LENGHT - ("@set somekey=").length()) + 1);
          "A", Shell.WINDOWS_MAX_SHELL_LENGTH - ("@set somekey=").length()) + 1);
       fail("long env was expected to throw");
     } catch(IOException e) {
       assertThat(e.getMessage(), containsString(expectedMessage));
@@ -1051,17 +1051,17 @@ public void testWindowsShellScriptBuilderMkdir() throws IOException {
     Assume.assumeTrue(Shell.WINDOWS);
 
     // The tests are built on assuming 8191 max command line length
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGHT);
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGTH);
 
     ShellScriptBuilder builder = ShellScriptBuilder.create();
 
     // test mkdir
     builder.mkdir(new Path(org.apache.commons.lang.StringUtils.repeat("A", 1024)));
     builder.mkdir(new Path(org.apache.commons.lang.StringUtils.repeat(
        "E", (Shell.WINDOWS_MAX_SHELL_LENGHT - mkDirCmd.length())/2)));
        "E", (Shell.WINDOWS_MAX_SHELL_LENGTH - mkDirCmd.length())/2)));
     try {
       builder.mkdir(new Path(org.apache.commons.lang.StringUtils.repeat(
          "X", (Shell.WINDOWS_MAX_SHELL_LENGHT - mkDirCmd.length())/2 +1)));
          "X", (Shell.WINDOWS_MAX_SHELL_LENGTH - mkDirCmd.length())/2 +1)));
       fail("long mkdir was expected to throw");
     } catch(IOException e) {
       assertThat(e.getMessage(), containsString(expectedMessage));
@@ -1072,11 +1072,10 @@ public void testWindowsShellScriptBuilderMkdir() throws IOException {
   public void testWindowsShellScriptBuilderLink() throws IOException {
     // Test is only relevant on Windows
     Assume.assumeTrue(Shell.WINDOWS);

    String linkCmd = "@" +Shell.WINUTILS + " symlink \"\" \"\"";
    String linkCmd = "@" + Shell.getWinutilsPath() + " symlink \"\" \"\"";
 
     // The tests are built on assuming 8191 max command line length
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGHT);
    assertEquals(8191, Shell.WINDOWS_MAX_SHELL_LENGTH);
 
     ShellScriptBuilder builder = ShellScriptBuilder.create();
 
@@ -1085,15 +1084,15 @@ public void testWindowsShellScriptBuilderLink() throws IOException {
         new Path(org.apache.commons.lang.StringUtils.repeat("B", 1024)));
     builder.link(
         new Path(org.apache.commons.lang.StringUtils.repeat(
            "E", (Shell.WINDOWS_MAX_SHELL_LENGHT - linkCmd.length())/2)),
            "E", (Shell.WINDOWS_MAX_SHELL_LENGTH - linkCmd.length())/2)),
         new Path(org.apache.commons.lang.StringUtils.repeat(
            "F", (Shell.WINDOWS_MAX_SHELL_LENGHT - linkCmd.length())/2)));
            "F", (Shell.WINDOWS_MAX_SHELL_LENGTH - linkCmd.length())/2)));
     try {
       builder.link(
           new Path(org.apache.commons.lang.StringUtils.repeat(
              "X", (Shell.WINDOWS_MAX_SHELL_LENGHT - linkCmd.length())/2 + 1)),
              "X", (Shell.WINDOWS_MAX_SHELL_LENGTH - linkCmd.length())/2 + 1)),
           new Path(org.apache.commons.lang.StringUtils.repeat(
              "Y", (Shell.WINDOWS_MAX_SHELL_LENGHT - linkCmd.length())/2) + 1));
              "Y", (Shell.WINDOWS_MAX_SHELL_LENGTH - linkCmd.length())/2) + 1));
       fail("long link was expected to throw");
     } catch(IOException e) {
       assertThat(e.getMessage(), containsString(expectedMessage));
- 
2.19.1.windows.1

