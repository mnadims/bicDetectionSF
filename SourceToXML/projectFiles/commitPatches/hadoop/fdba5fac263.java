From fdba5fac263f9bf79fccf566c36bbc42ef67e875 Mon Sep 17 00:00:00 2001
From: Colin McCabe <cmccabe@apache.org>
Date: Wed, 14 Aug 2013 23:12:55 +0000
Subject: [PATCH] HADOOP-9652.  RawLocalFs#getFileLinkStatus does not fill in
 the link owner and mode.  (Andrew Wang via Colin Patrick McCabe)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1514088 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |   4 +
 .../hadoop/fs/DelegateToFileSystem.java       |  19 +-
 .../java/org/apache/hadoop/fs/HardLink.java   |  40 +----
 .../apache/hadoop/fs/RawLocalFileSystem.java  |  74 +++++++-
 .../main/java/org/apache/hadoop/fs/Stat.java  | 167 ++++++++++++++++++
 .../apache/hadoop/fs/local/RawLocalFs.java    |  94 +---------
 .../java/org/apache/hadoop/util/Shell.java    |  56 +++++-
 .../apache/hadoop/fs/TestLocalFileSystem.java |  19 +-
 .../java/org/apache/hadoop/fs/TestStat.java   | 122 +++++++++++++
 .../apache/hadoop/fs/TestSymlinkLocalFS.java  |  15 +-
 10 files changed, 453 insertions(+), 157 deletions(-)
 create mode 100644 hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Stat.java
 create mode 100644 hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestStat.java

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 77f735504b6..52073fa31c8 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -331,6 +331,10 @@ Release 2.3.0 - UNRELEASED
     HADOOP-9817. FileSystem#globStatus and FileContext#globStatus need to work
     with symlinks. (Colin Patrick McCabe via Andrew Wang)
 
    HADOOP-9652.  RawLocalFs#getFileLinkStatus does not fill in the link owner
    and mode.  (Andrew Wang via Colin Patrick McCabe)


 Release 2.1.1-beta - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
index 1293448eea3..708ca4ada5b 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
@@ -113,7 +113,14 @@ public FileStatus getFileStatus(Path f) throws IOException {
 
   @Override
   public FileStatus getFileLinkStatus(final Path f) throws IOException {
    return getFileStatus(f);
    FileStatus status = fsImpl.getFileLinkStatus(f);
    // FileSystem#getFileLinkStatus qualifies the link target
    // AbstractFileSystem needs to return it plain since it's qualified
    // in FileContext, so re-get and set the plain target
    if (status.isSymlink()) {
      status.setSymlink(fsImpl.getLinkTarget(f));
    }
    return status;
   }
 
   @Override
@@ -199,22 +206,18 @@ public void setVerifyChecksum(boolean verifyChecksum) throws IOException {
 
   @Override
   public boolean supportsSymlinks() {
    return false;
    return fsImpl.supportsSymlinks();
   }  
   
   @Override
   public void createSymlink(Path target, Path link, boolean createParent) 
       throws IOException { 
    throw new IOException("File system does not support symlinks");
    fsImpl.createSymlink(target, link, createParent);
   } 
   
   @Override
   public Path getLinkTarget(final Path f) throws IOException {
    /* We should never get here. Any file system that threw an 
     * UnresolvedLinkException, causing this function to be called,
     * should override getLinkTarget. 
     */
    throw new AssertionError();
    return fsImpl.getLinkTarget(f);
   }
 
   @Override //AbstractFileSystem
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
index 5e462cdc441..bf5ed6d58f7 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/HardLink.java
@@ -41,15 +41,6 @@
  */
 public class HardLink { 
 
  public enum OSType {
    OS_TYPE_UNIX,
    OS_TYPE_WIN,
    OS_TYPE_SOLARIS,
    OS_TYPE_MAC,
    OS_TYPE_FREEBSD
  }
  
  public static OSType osType;
   private static HardLinkCommandGetter getHardLinkCommand;
   
   public final LinkStats linkStats; //not static
@@ -57,19 +48,18 @@
   //initialize the command "getters" statically, so can use their 
   //methods without instantiating the HardLink object
   static { 
    osType = getOSType();
    if (osType == OSType.OS_TYPE_WIN) {
    if (Shell.WINDOWS) {
       // Windows
       getHardLinkCommand = new HardLinkCGWin();
     } else {
      // Unix
      // Unix or Linux
       getHardLinkCommand = new HardLinkCGUnix();
       //override getLinkCountCommand for the particular Unix variant
       //Linux is already set as the default - {"stat","-c%h", null}
      if (osType == OSType.OS_TYPE_MAC || osType == OSType.OS_TYPE_FREEBSD) {
      if (Shell.MAC || Shell.FREEBSD) {
         String[] linkCountCmdTemplate = {"/usr/bin/stat","-f%l", null};
         HardLinkCGUnix.setLinkCountCmdTemplate(linkCountCmdTemplate);
      } else if (osType == OSType.OS_TYPE_SOLARIS) {
      } else if (Shell.SOLARIS) {
         String[] linkCountCmdTemplate = {"ls","-l", null};
         HardLinkCGUnix.setLinkCountCmdTemplate(linkCountCmdTemplate);        
       }
@@ -80,26 +70,6 @@ public HardLink() {
     linkStats = new LinkStats();
   }
   
  static private OSType getOSType() {
    String osName = System.getProperty("os.name");
    if (Shell.WINDOWS) {
      return OSType.OS_TYPE_WIN;
    }
    else if (osName.contains("SunOS") 
            || osName.contains("Solaris")) {
       return OSType.OS_TYPE_SOLARIS;
    }
    else if (osName.contains("Mac")) {
       return OSType.OS_TYPE_MAC;
    }
    else if (osName.contains("FreeBSD")) {
       return OSType.OS_TYPE_FREEBSD;
    }
    else {
      return OSType.OS_TYPE_UNIX;
    }
  }
  
   /**
    * This abstract class bridges the OS-dependent implementations of the 
    * needed functionality for creating hardlinks and querying link counts.
@@ -548,7 +518,7 @@ public static int getLinkCount(File fileName) throws IOException {
       if (inpMsg == null || exitValue != 0) {
         throw createIOException(fileName, inpMsg, errMsg, exitValue, null);
       }
      if (osType == OSType.OS_TYPE_SOLARIS) {
      if (Shell.SOLARIS) {
         String[] result = inpMsg.split("\\s+");
         return Integer.parseInt(result[1]);
       } else {
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
index d693214163b..42f77fc3508 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/RawLocalFileSystem.java
@@ -51,6 +51,7 @@
 public class RawLocalFileSystem extends FileSystem {
   static final URI NAME = URI.create("file:///");
   private Path workingDir;
  private static final boolean useDeprecatedFileStatus = !Stat.isAvailable();
   
   public RawLocalFileSystem() {
     workingDir = getInitialWorkingDirectory();
@@ -385,8 +386,11 @@ public boolean delete(Path p, boolean recursive) throws IOException {
       throw new FileNotFoundException("File " + f + " does not exist");
     }
     if (localf.isFile()) {
      if (!useDeprecatedFileStatus) {
        return new FileStatus[] { getFileStatus(f) };
      }
       return new FileStatus[] {
        new RawLocalFileStatus(localf, getDefaultBlockSize(f), this) };
        new DeprecatedRawLocalFileStatus(localf, getDefaultBlockSize(f), this)};
     }
 
     File[] names = localf.listFiles();
@@ -516,15 +520,22 @@ public String toString() {
   
   @Override
   public FileStatus getFileStatus(Path f) throws IOException {
    return getFileLinkStatusInternal(f, true);
  }

  @Deprecated
  private FileStatus deprecatedGetFileStatus(Path f) throws IOException {
     File path = pathToFile(f);
     if (path.exists()) {
      return new RawLocalFileStatus(pathToFile(f), getDefaultBlockSize(f), this);
      return new DeprecatedRawLocalFileStatus(pathToFile(f),
          getDefaultBlockSize(f), this);
     } else {
       throw new FileNotFoundException("File " + f + " does not exist");
     }
   }
 
  static class RawLocalFileStatus extends FileStatus {
  @Deprecated
  static class DeprecatedRawLocalFileStatus extends FileStatus {
     /* We can add extra fields here. It breaks at least CopyFiles.FilePair().
      * We recognize if the information is already loaded by check if
      * onwer.equals("").
@@ -533,7 +544,7 @@ private boolean isPermissionLoaded() {
       return !super.getOwner().isEmpty(); 
     }
     
    RawLocalFileStatus(File f, long defaultBlockSize, FileSystem fs) { 
    DeprecatedRawLocalFileStatus(File f, long defaultBlockSize, FileSystem fs) {
       super(f.length(), f.isDirectory(), 1, defaultBlockSize,
           f.lastModified(), new Path(f.getPath()).makeQualified(fs.getUri(),
             fs.getWorkingDirectory()));
@@ -699,7 +710,7 @@ public void createSymlink(Path target, Path link, boolean createParent)
    */
   @Override
   public FileStatus getFileLinkStatus(final Path f) throws IOException {
    FileStatus fi = getFileLinkStatusInternal(f);
    FileStatus fi = getFileLinkStatusInternal(f, false);
     // getFileLinkStatus is supposed to return a symlink with a
     // qualified path
     if (fi.isSymlink()) {
@@ -710,7 +721,35 @@ public FileStatus getFileLinkStatus(final Path f) throws IOException {
     return fi;
   }
 
  private FileStatus getFileLinkStatusInternal(final Path f) throws IOException {
  /**
   * Public {@link FileStatus} methods delegate to this function, which in turn
   * either call the new {@link Stat} based implementation or the deprecated
   * methods based on platform support.
   * 
   * @param f Path to stat
   * @param dereference whether to dereference the final path component if a
   *          symlink
   * @return FileStatus of f
   * @throws IOException
   */
  private FileStatus getFileLinkStatusInternal(final Path f,
      boolean dereference) throws IOException {
    if (!useDeprecatedFileStatus) {
      return getNativeFileLinkStatus(f, dereference);
    } else if (dereference) {
      return deprecatedGetFileStatus(f);
    } else {
      return deprecatedGetFileLinkStatusInternal(f);
    }
  }

  /**
   * Deprecated. Remains for legacy support. Should be removed when {@link Stat}
   * gains support for Windows and other operating systems.
   */
  @Deprecated
  private FileStatus deprecatedGetFileLinkStatusInternal(final Path f)
      throws IOException {
     String target = FileUtil.readLink(new File(f.toString()));
 
     try {
@@ -746,10 +785,31 @@ private FileStatus getFileLinkStatusInternal(final Path f) throws IOException {
       throw e;
     }
   }
  /**
   * Calls out to platform's native stat(1) implementation to get file metadata
   * (permissions, user, group, atime, mtime, etc). This works around the lack
   * of lstat(2) in Java 6.
   * 
   *  Currently, the {@link Stat} class used to do this only supports Linux
   *  and FreeBSD, so the old {@link #deprecatedGetFileLinkStatusInternal(Path)}
   *  implementation (deprecated) remains further OS support is added.
   *
   * @param f File to stat
   * @param dereference whether to dereference symlinks
   * @return FileStatus of f
   * @throws IOException
   */
  private FileStatus getNativeFileLinkStatus(final Path f,
      boolean dereference) throws IOException {
    checkPath(f);
    Stat stat = new Stat(f, getDefaultBlockSize(f), dereference, this);
    FileStatus status = stat.getFileStatus();
    return status;
  }
 
   @Override
   public Path getLinkTarget(Path f) throws IOException {
    FileStatus fi = getFileLinkStatusInternal(f);
    FileStatus fi = getFileLinkStatusInternal(f, false);
     // return an unqualified symlink target
     return fi.getSymlink();
   }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Stat.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Stat.java
new file mode 100644
index 00000000000..36dd8811e77
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Stat.java
@@ -0,0 +1,167 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.fs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Shell;

import com.google.common.annotations.VisibleForTesting;

/**
 * Wrapper for the Unix stat(1) command. Used to workaround the lack of 
 * lstat(2) in Java 6.
 */
@InterfaceAudience.LimitedPrivate({"HDFS", "MapReduce"})
@InterfaceStability.Evolving
public class Stat extends Shell {

  private final Path original;
  private final Path qualified;
  private final Path path;
  private final long blockSize;
  private final boolean dereference;

  private FileStatus stat;

  public Stat(Path path, long blockSize, boolean deref, FileSystem fs)
      throws IOException {
    super(0L, true);
    // Original path
    this.original = path;
    // Qualify the original and strip out URI fragment via toUri().getPath()
    Path stripped = new Path(
        original.makeQualified(fs.getUri(), fs.getWorkingDirectory())
        .toUri().getPath());
    // Re-qualify the bare stripped path and store it
    this.qualified = 
        stripped.makeQualified(fs.getUri(), fs.getWorkingDirectory());
    // Strip back down to a plain path
    this.path = new Path(qualified.toUri().getPath());
    this.blockSize = blockSize;
    this.dereference = deref;
  }

  public FileStatus getFileStatus() throws IOException {
    run();
    return stat;
  }

  /**
   * Whether Stat is supported on the current platform
   * @return
   */
  public static boolean isAvailable() {
    if (Shell.LINUX || Shell.FREEBSD) {
      return true;
    }
    return false;
  }

  @VisibleForTesting
  FileStatus getFileStatusForTesting() {
    return stat;
  }

  @Override
  protected String[] getExecString() {
    String derefFlag = "-";
    if (dereference) {
      derefFlag = "-L";
    }
    if (Shell.LINUX) {
      return new String[] {
          "stat", derefFlag + "c", "%s,%F,%Y,%X,%a,%U,%G,%N", path.toString() };
    } else if (Shell.FREEBSD) {
      return new String[] {
          "stat", derefFlag + "f", "%z,%HT,%m,%a,%Op,%Su,%Sg,`link' -> `%Y'",
          path.toString() };
    } else {
      throw new UnsupportedOperationException(
          "stat is not supported on this platform");
    }
  }

  @Override
  protected void parseExecResult(BufferedReader lines) throws IOException {
    // Reset stat
    stat = null;

    String line = lines.readLine();
    if (line == null) {
      throw new IOException("Unable to stat path: " + original);
    }
    if (line.endsWith("No such file or directory") ||
        line.endsWith("Not a directory")) {
      throw new FileNotFoundException("File " + original + " does not exist");
    }
    if (line.endsWith("Too many levels of symbolic links")) {
      throw new IOException("Possible cyclic loop while following symbolic" +
          " link " + original);
    }
    // 6,symbolic link,6,1373584236,1373584236,lrwxrwxrwx,andrew,andrew,`link' -> `target'
    StringTokenizer tokens = new StringTokenizer(line, ",");
    try {
      long length = Long.parseLong(tokens.nextToken());
      boolean isDir = tokens.nextToken().equalsIgnoreCase("directory") ? true
          : false;
      // Convert from seconds to milliseconds
      long modTime = Long.parseLong(tokens.nextToken())*1000;
      long accessTime = Long.parseLong(tokens.nextToken())*1000;
      String octalPerms = tokens.nextToken();
      // FreeBSD has extra digits beyond 4, truncate them
      if (octalPerms.length() > 4) {
        int len = octalPerms.length();
        octalPerms = octalPerms.substring(len-4, len);
      }
      FsPermission perms = new FsPermission(Short.parseShort(octalPerms, 8));
      String owner = tokens.nextToken();
      String group = tokens.nextToken();
      String symStr = tokens.nextToken();
      // 'notalink'
      // 'link' -> `target'
      // '' -> ''
      Path symlink = null;
      StringTokenizer symTokens = new StringTokenizer(symStr, "`");
      symTokens.nextToken();
      try {
        String target = symTokens.nextToken();
        target = target.substring(0, target.length()-1);
        if (!target.isEmpty()) {
          symlink = new Path(target);
        }
      } catch (NoSuchElementException e) {
        // null if not a symlink
      }
      // Set stat
      stat = new FileStatus(length, isDir, 1, blockSize, modTime, accessTime,
          perms, owner, group, symlink, qualified);
    } catch (NumberFormatException e) {
      throw new IOException("Unexpected stat output: " + line, e);
    } catch (NoSuchElementException e) {
      throw new IOException("Unexpected stat output: " + line, e);
    }
  }
}
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/RawLocalFs.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/RawLocalFs.java
index 605bade09a8..6cb2792eebc 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/RawLocalFs.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/local/RawLocalFs.java
@@ -17,8 +17,6 @@
  */
 package org.apache.hadoop.fs.local;
 
import java.io.File;
import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
@@ -28,13 +26,9 @@
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.AbstractFileSystem;
 import org.apache.hadoop.fs.DelegateToFileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileUtil;
 import org.apache.hadoop.fs.FsConstants;
 import org.apache.hadoop.fs.FsServerDefaults;
import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.hadoop.fs.permission.FsPermission;
 
 /**
  * The RawLocalFs implementation of AbstractFileSystem.
@@ -72,90 +66,12 @@ public int getUriDefaultPort() {
   public FsServerDefaults getServerDefaults() throws IOException {
     return LocalConfigKeys.getServerDefaults();
   }
  
  @Override
  public boolean supportsSymlinks() {
    return true;
  }

  @Override
  public void createSymlink(Path target, Path link, boolean createParent)
      throws IOException {
    final String targetScheme = target.toUri().getScheme();
    if (targetScheme != null && !"file".equals(targetScheme)) {
      throw new IOException("Unable to create symlink to non-local file "+
          "system: "+target.toString());
    }

    if (createParent) {
      mkdir(link.getParent(), FsPermission.getDirDefault(), true);
    }

    // NB: Use createSymbolicLink in java.nio.file.Path once available
    int result = FileUtil.symLink(target.toString(), link.toString());
    if (result != 0) {
      throw new IOException("Error " + result + " creating symlink " +
          link + " to " + target);
    }
  }
 
  /**
   * Return a FileStatus representing the given path. If the path refers 
   * to a symlink return a FileStatus representing the link rather than
   * the object the link refers to.
   */
  @Override
  public FileStatus getFileLinkStatus(final Path f) throws IOException {
    String target = FileUtil.readLink(new File(f.toString()));
    try {
      FileStatus fs = getFileStatus(f);
      // If f refers to a regular file or directory      
      if (target.isEmpty()) {
        return fs;
      }
      // Otherwise f refers to a symlink
      return new FileStatus(fs.getLen(), 
          false,
          fs.getReplication(), 
          fs.getBlockSize(),
          fs.getModificationTime(),
          fs.getAccessTime(),
          fs.getPermission(),
          fs.getOwner(),
          fs.getGroup(),
          new Path(target),
          f);
    } catch (FileNotFoundException e) {
      /* The exists method in the File class returns false for dangling 
       * links so we can get a FileNotFoundException for links that exist.
       * It's also possible that we raced with a delete of the link. Use
       * the readBasicFileAttributes method in java.nio.file.attributes 
       * when available.
       */
      if (!target.isEmpty()) {
        return new FileStatus(0, false, 0, 0, 0, 0, FsPermission.getDefault(), 
            "", "", new Path(target), f);        
      }
      // f refers to a file or directory that does not exist
      throw e;
    }
  }
  
   @Override
   public boolean isValidName(String src) {
     // Different local file systems have different validation rules.  Skip
     // validation here and just let the OS handle it.  This is consistent with
     // RawLocalFileSystem.
     return true;
   }
  
   @Override
  public Path getLinkTarget(Path f) throws IOException {
    /* We should never get here. Valid local links are resolved transparently
     * by the underlying local file system and accessing a dangling link will 
     * result in an IOException, not an UnresolvedLinkException, so FileContext
     * should never call this function.
     */
    throw new AssertionError();
  public boolean isValidName(String src) {
    // Different local file systems have different validation rules. Skip
    // validation here and just let the OS handle it. This is consistent with
    // RawLocalFileSystem.
    return true;
   }
 }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
index 2817736f281..0a8ce2e9983 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java
@@ -58,6 +58,45 @@ public static boolean isJava7OrAbove() {
   /** Windows CreateProcess synchronization object */
   public static final Object WindowsProcessLaunchLock = new Object();
 
  // OSType detection

  public enum OSType {
    OS_TYPE_LINUX,
    OS_TYPE_WIN,
    OS_TYPE_SOLARIS,
    OS_TYPE_MAC,
    OS_TYPE_FREEBSD,
    OS_TYPE_OTHER
  }

  public static final OSType osType = getOSType();

  static private OSType getOSType() {
    String osName = System.getProperty("os.name");
    if (osName.startsWith("Windows")) {
      return OSType.OS_TYPE_WIN;
    } else if (osName.contains("SunOS") || osName.contains("Solaris")) {
      return OSType.OS_TYPE_SOLARIS;
    } else if (osName.contains("Mac")) {
      return OSType.OS_TYPE_MAC;
    } else if (osName.contains("FreeBSD")) {
      return OSType.OS_TYPE_FREEBSD;
    } else if (osName.startsWith("Linux")) {
      return OSType.OS_TYPE_LINUX;
    } else {
      // Some other form of Unix
      return OSType.OS_TYPE_OTHER;
    }
  }

  // Helper static vars for each platform
  public static final boolean WINDOWS = (osType == OSType.OS_TYPE_WIN);
  public static final boolean SOLARIS = (osType == OSType.OS_TYPE_SOLARIS);
  public static final boolean MAC     = (osType == OSType.OS_TYPE_MAC);
  public static final boolean FREEBSD = (osType == OSType.OS_TYPE_FREEBSD);
  public static final boolean LINUX   = (osType == OSType.OS_TYPE_LINUX);
  public static final boolean OTHER   = (osType == OSType.OS_TYPE_OTHER);

   /** a Unix command to get the current user's groups list */
   public static String[] getGroupsCommand() {
     return (WINDOWS)? new String[]{"cmd", "/c", "groups"}
@@ -282,13 +321,6 @@ public static final String getQualifiedBinPath(String executable)
     return exeFile.getCanonicalPath();
   }
 
  /** Set to true on Windows platforms */
  public static final boolean WINDOWS /* borrowed from Path.WINDOWS */
                = System.getProperty("os.name").startsWith("Windows");

  public static final boolean LINUX
                = System.getProperty("os.name").startsWith("Linux");
  
   /** a Windows utility to emulate Unix commands */
   public static final String WINUTILS = getWinUtilsPath();
 
@@ -336,6 +368,7 @@ private static boolean isSetsidSupported() {
 
   private long    interval;   // refresh interval in msec
   private long    lastTime;   // last time the command was performed
  final private boolean redirectErrorStream; // merge stdout and stderr
   private Map<String, String> environment; // env for the command execution
   private File dir;
   private Process process; // sub process used to execute the command
@@ -348,13 +381,18 @@ public Shell() {
     this(0L);
   }
   
  public Shell(long interval) {
    this(interval, false);
  }

   /**
    * @param interval the minimum duration to wait before re-executing the 
    *        command.
    */
  public Shell( long interval ) {
  public Shell(long interval, boolean redirectErrorStream) {
     this.interval = interval;
     this.lastTime = (interval<0) ? 0 : -interval;
    this.redirectErrorStream = redirectErrorStream;
   }
   
   /** set the environment for the command 
@@ -393,6 +431,8 @@ private void runCommand() throws IOException {
     if (dir != null) {
       builder.directory(this.dir);
     }

    builder.redirectErrorStream(redirectErrorStream);
     
     if (Shell.WINDOWS) {
       synchronized (WindowsProcessLaunchLock) {
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
index cb6a6421134..dacb2c9b82f 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestLocalFileSystem.java
@@ -26,6 +26,7 @@
 import static org.apache.hadoop.fs.FileSystemTestHelper.*;
 
 import java.io.*;
import java.net.URI;
 import java.util.Arrays;
 import java.util.Random;
 
@@ -363,12 +364,12 @@ public void testSetTimes() throws Exception {
 
     FileStatus status = fileSys.getFileStatus(path);
     assertTrue("check we're actually changing something", newModTime != status.getModificationTime());
    assertEquals(0, status.getAccessTime());
    long accessTime = status.getAccessTime();
 
     fileSys.setTimes(path, newModTime, -1);
     status = fileSys.getFileStatus(path);
     assertEquals(newModTime, status.getModificationTime());
    assertEquals(0, status.getAccessTime());
    assertEquals(accessTime, status.getAccessTime());
   }
 
   /**
@@ -520,4 +521,18 @@ private void verifyRead(FSDataInputStream stm, byte[] fileContents,
       fail(s);
     }
   }

  @Test
  public void testStripFragmentFromPath() throws Exception {
    FileSystem fs = FileSystem.getLocal(new Configuration());
    Path pathQualified = TEST_PATH.makeQualified(fs.getUri(),
        fs.getWorkingDirectory());
    Path pathWithFragment = new Path(
        new URI(pathQualified.toString() + "#glacier"));
    // Create test file with fragment
    FileSystemTestHelper.createFile(fs, pathWithFragment);
    Path resolved = fs.resolvePath(pathWithFragment);
    assertEquals("resolvePath did not strip fragment from Path", pathQualified,
        resolved);
  }
 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestStat.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestStat.java
new file mode 100644
index 00000000000..4397f2d534c
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestStat.java
@@ -0,0 +1,122 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.fs;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringReader;

import org.apache.hadoop.conf.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestStat {

  private static Stat stat;

  @BeforeClass
  public static void setup() throws Exception {
    stat = new Stat(new Path("/dummypath"),
        4096l, false, FileSystem.get(new Configuration()));
  }

  private class StatOutput {
    final String doesNotExist;
    final String directory;
    final String file;
    final String symlink;
    final String stickydir;

    StatOutput(String doesNotExist, String directory, String file,
        String symlink, String stickydir) {
      this.doesNotExist = doesNotExist;
      this.directory = directory;
      this.file = file;
      this.symlink = symlink;
      this.stickydir = stickydir;
    }

    void test() throws Exception {
      BufferedReader br;
      FileStatus status;

      try {
        br = new BufferedReader(new StringReader(doesNotExist));
        stat.parseExecResult(br);
      } catch (FileNotFoundException e) {
        // expected
      }

      br = new BufferedReader(new StringReader(directory));
      stat.parseExecResult(br);
      status = stat.getFileStatusForTesting();
      assertTrue(status.isDirectory());

      br = new BufferedReader(new StringReader(file));
      stat.parseExecResult(br);
      status = stat.getFileStatusForTesting();
      assertTrue(status.isFile());

      br = new BufferedReader(new StringReader(symlink));
      stat.parseExecResult(br);
      status = stat.getFileStatusForTesting();
      assertTrue(status.isSymlink());

      br = new BufferedReader(new StringReader(stickydir));
      stat.parseExecResult(br);
      status = stat.getFileStatusForTesting();
      assertTrue(status.isDirectory());
      assertTrue(status.getPermission().getStickyBit());
    }
  }

  @Test(timeout=10000)
  public void testStatLinux() throws Exception {
    StatOutput linux = new StatOutput(
        "stat: cannot stat `watermelon': No such file or directory",
        "4096,directory,1373584236,1373586485,755,andrew,root,`.'",
        "0,regular empty file,1373584228,1373584228,644,andrew,andrew,`target'",
        "6,symbolic link,1373584236,1373584236,777,andrew,andrew,`link' -> `target'",
        "4096,directory,1374622334,1375124212,1755,andrew,andrew,`stickydir'");
    linux.test();
  }

  @Test(timeout=10000)
  public void testStatFreeBSD() throws Exception {
    StatOutput freebsd = new StatOutput(
        "stat: symtest/link: stat: No such file or directory",
        "512,Directory,1373583695,1373583669,40755,awang,awang,`link' -> `'",
        "0,Regular File,1373508937,1373508937,100644,awang,awang,`link' -> `'",
        "6,Symbolic Link,1373508941,1373508941,120755,awang,awang,`link' -> `target'",
        "512,Directory,1375139537,1375139537,41755,awang,awang,`link' -> `'");
    freebsd.test();
  }

  @Test(timeout=10000)
  public void testStatFileNotFound() throws Exception {
    try {
      stat.getFileStatus();
      fail("Expected FileNotFoundException");
    } catch (FileNotFoundException e) {
      // expected
    }
  }
}
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
index eb0e1089bf3..c82dcc8a124 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestSymlinkLocalFS.java
@@ -31,6 +31,7 @@
 
 import org.apache.hadoop.fs.permission.FsPermission;
 import org.apache.hadoop.util.Shell;
import org.apache.hadoop.security.UserGroupInformation;
 import org.junit.Test;
 
 /**
@@ -134,6 +135,7 @@ public void testDanglingLink() throws IOException {
     Path fileAbs  = new Path(testBaseDir1()+"/file");
     Path fileQual = new Path(testURI().toString(), fileAbs);
     Path link     = new Path(testBaseDir1()+"/linkToFile");
    Path linkQual = new Path(testURI().toString(), link.toString());
     wrapper.createSymlink(fileAbs, link, false);
     // Deleting the link using FileContext currently fails because
     // resolve looks up LocalFs rather than RawLocalFs for the path 
@@ -151,18 +153,15 @@ public void testDanglingLink() throws IOException {
       // Expected. File's exists method returns false for dangling links
     }
     // We can stat a dangling link
    UserGroupInformation user = UserGroupInformation.getCurrentUser();
     FileStatus fsd = wrapper.getFileLinkStatus(link);
     assertEquals(fileQual, fsd.getSymlink());
     assertTrue(fsd.isSymlink());
     assertFalse(fsd.isDirectory());
    assertEquals("", fsd.getOwner());
    assertEquals("", fsd.getGroup());
    assertEquals(link, fsd.getPath());
    assertEquals(0, fsd.getLen());
    assertEquals(0, fsd.getBlockSize());
    assertEquals(0, fsd.getReplication());
    assertEquals(0, fsd.getAccessTime());
    assertEquals(FsPermission.getDefault(), fsd.getPermission());
    assertEquals(user.getUserName(), fsd.getOwner());
    // Compare against user's primary group
    assertEquals(user.getGroupNames()[0], fsd.getGroup());
    assertEquals(linkQual, fsd.getPath());
     // Accessing the link 
     try {
       readFile(link);
- 
2.19.1.windows.1

