From 8767e4cde172b6e6070e3fd45325ede617b99343 Mon Sep 17 00:00:00 2001
From: Colin McCabe <cmccabe@apache.org>
Date: Thu, 11 Jul 2013 21:31:04 +0000
Subject: [PATCH] HADOOP-9418.  Add symlink support to DistributedFileSystem
 (Andrew Wang via Colin Patrick McCabe)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1502373 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt   |   3 +
 .../org/apache/hadoop/hdfs/DFSClient.java     |  15 +-
 .../hadoop/hdfs/DistributedFileSystem.java    | 863 +++++++++++++++---
 .../apache/hadoop/hdfs/tools/DFSAdmin.java    |  13 +-
 .../hadoop/fs/TestSymlinkHdfsFileSystem.java  | 107 +++
 .../hdfs/TestDistributedFileSystem.java       |   7 +
 .../snapshot/TestNestedSnapshots.java         |  18 +-
 7 files changed, 875 insertions(+), 151 deletions(-)
 create mode 100644 hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestSymlinkHdfsFileSystem.java

diff --git a/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt b/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
index b3f81b0b8a5..3f92b18016f 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
++ b/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
@@ -263,6 +263,9 @@ Release 2.1.1-beta - UNRELEASED
 
   IMPROVEMENTS
 
    HADOOP-9418.  Add symlink support to DistributedFileSystem (Andrew Wang via
    Colin Patrick McCabe)

   OPTIMIZATIONS
 
   BUG FIXES
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSClient.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSClient.java
index 6638768f27b..b881a8b226a 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSClient.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSClient.java
@@ -1020,7 +1020,8 @@ boolean recoverLease(String src) throws IOException {
       return namenode.recoverLease(src, clientName);
     } catch (RemoteException re) {
       throw re.unwrapRemoteException(FileNotFoundException.class,
                                     AccessControlException.class);
                                     AccessControlException.class,
                                     UnresolvedPathException.class);
     }
   }
 
@@ -2159,7 +2160,11 @@ public void renameSnapshot(String snapshotDir, String snapshotOldName,
    */
   public void allowSnapshot(String snapshotRoot) throws IOException {
     checkOpen();
    namenode.allowSnapshot(snapshotRoot);
    try {
      namenode.allowSnapshot(snapshotRoot);
    } catch (RemoteException re) {
      throw re.unwrapRemoteException();
    }
   }
   
   /**
@@ -2169,7 +2174,11 @@ public void allowSnapshot(String snapshotRoot) throws IOException {
    */
   public void disallowSnapshot(String snapshotRoot) throws IOException {
     checkOpen();
    namenode.disallowSnapshot(snapshotRoot);
    try {
      namenode.disallowSnapshot(snapshotRoot);
    } catch (RemoteException re) {
      throw re.unwrapRemoteException();
    }
   }
   
   /**
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java
index 2a01d7fd3d9..8127689713d 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DistributedFileSystem.java
@@ -34,17 +34,24 @@
 import org.apache.hadoop.fs.ContentSummary;
 import org.apache.hadoop.fs.CreateFlag;
 import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSLinkResolver;
import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.apache.hadoop.fs.FileChecksum;
 import org.apache.hadoop.fs.FileStatus;
 import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileSystemLinkResolver;
 import org.apache.hadoop.fs.FsServerDefaults;
 import org.apache.hadoop.fs.FsStatus;
 import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.MD5MD5CRC32FileChecksum;
 import org.apache.hadoop.fs.Options;
 import org.apache.hadoop.fs.Options.ChecksumOpt;
import org.apache.hadoop.fs.ParentNotDirectoryException;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.PathFilter;
 import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.UnresolvedLinkException;
import org.apache.hadoop.fs.UnsupportedFileSystemException;
 import org.apache.hadoop.fs.VolumeId;
 import org.apache.hadoop.fs.permission.FsPermission;
 import org.apache.hadoop.hdfs.client.HdfsAdmin;
@@ -54,12 +61,12 @@
 import org.apache.hadoop.hdfs.protocol.DirectoryListing;
 import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
 import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.SnapshotDiffReport;
 import org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType;
 import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
 import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
 import org.apache.hadoop.hdfs.protocol.HdfsLocatedFileStatus;
 import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.SnapshotDiffReport;
 import org.apache.hadoop.hdfs.protocol.SnapshottableDirectoryStatus;
 import org.apache.hadoop.hdfs.security.token.block.InvalidBlockTokenException;
 import org.apache.hadoop.hdfs.security.token.delegation.DelegationTokenIdentifier;
@@ -146,22 +153,14 @@ public short getDefaultReplication() {
     return dfs.getDefaultReplication();
   }
 
  private Path makeAbsolute(Path f) {
    if (f.isAbsolute()) {
      return f;
    } else {
      return new Path(workingDir, f);
    }
  }

   @Override
   public void setWorkingDirectory(Path dir) {
    String result = makeAbsolute(dir).toUri().getPath();
    String result = fixRelativePart(dir).toUri().getPath();
     if (!DFSUtil.isValidName(result)) {
       throw new IllegalArgumentException("Invalid DFS directory name " + 
                                          result);
     }
    workingDir = makeAbsolute(dir);
    workingDir = fixRelativePart(dir);
   }
 
   
@@ -170,9 +169,18 @@ public Path getHomeDirectory() {
     return makeQualified(new Path("/user/" + dfs.ugi.getShortUserName()));
   }
 
  /**
   * Checks that the passed URI belongs to this filesystem, resolves the path
   * component against the current working directory if relative, and finally
   * returns the absolute path component.
   * 
   * @param file URI to check and resolve
   * @return resolved absolute path component of {file}
   * @throws IllegalArgumentException if URI does not belong to this DFS
   */
   private String getPathName(Path file) {
     checkPath(file);
    String result = makeAbsolute(file).toUri().getPath();
    String result = file.toUri().getPath();
     if (!DFSUtil.isValidName(result)) {
       throw new IllegalArgumentException("Pathname " + result + " from " +
                                          file+" is not a valid DFS filename.");
@@ -191,10 +199,21 @@ private String getPathName(Path file) {
   
   @Override
   public BlockLocation[] getFileBlockLocations(Path p, 
      long start, long len) throws IOException {
      final long start, final long len) throws IOException {
     statistics.incrementReadOps(1);
    return dfs.getBlockLocations(getPathName(p), start, len);

    final Path absF = fixRelativePart(p);
    return new FileSystemLinkResolver<BlockLocation[]>() {
      @Override
      public BlockLocation[] doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.getBlockLocations(getPathName(p), start, len);
      }
      @Override
      public BlockLocation[] next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.getFileBlockLocations(p, start, len);
      }
    }.resolve(this, absF);
   }
 
   /**
@@ -239,28 +258,68 @@ public void setVerifyChecksum(boolean verifyChecksum) {
    * @return true if the file is already closed
    * @throws IOException if an error occurs
    */
  public boolean recoverLease(Path f) throws IOException {
    return dfs.recoverLease(getPathName(f));
  public boolean recoverLease(final Path f) throws IOException {
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<Boolean>() {
      @Override
      public Boolean doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.recoverLease(getPathName(p));
      }
      @Override
      public Boolean next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          return myDfs.recoverLease(p);
        }
        throw new UnsupportedOperationException("Cannot recoverLease through" +
            " a symlink to a non-DistributedFileSystem: " + f + " -> " + p);
      }
    }.resolve(this, absF);
   }
 
  @SuppressWarnings("deprecation")
   @Override
  public HdfsDataInputStream open(Path f, int bufferSize) throws IOException {
  public FSDataInputStream open(Path f, final int bufferSize)
      throws IOException {
     statistics.incrementReadOps(1);
    return new DFSClient.DFSDataInputStream(
          dfs.open(getPathName(f), bufferSize, verifyChecksum));
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<FSDataInputStream>() {
      @Override
      public FSDataInputStream doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return new HdfsDataInputStream(
            dfs.open(getPathName(p), bufferSize, verifyChecksum));
      }
      @Override
      public FSDataInputStream next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.open(p, bufferSize);
      }
    }.resolve(this, absF);
   }
 
  /** This optional operation is not yet supported. */
   @Override
  public HdfsDataOutputStream append(Path f, int bufferSize,
      Progressable progress) throws IOException {
  public FSDataOutputStream append(Path f, final int bufferSize,
      final Progressable progress) throws IOException {
     statistics.incrementWriteOps(1);
    return dfs.append(getPathName(f), bufferSize, progress, statistics);
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<FSDataOutputStream>() {
      @Override
      public FSDataOutputStream doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.append(getPathName(p), bufferSize, progress, statistics);
      }
      @Override
      public FSDataOutputStream next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.append(p, bufferSize);
      }
    }.resolve(this, absF);
   }
 
   @Override
  public HdfsDataOutputStream create(Path f, FsPermission permission,
  public FSDataOutputStream create(Path f, FsPermission permission,
       boolean overwrite, int bufferSize, short replication, long blockSize,
       Progressable progress) throws IOException {
     return this.create(f, permission,
@@ -279,61 +338,125 @@ public HdfsDataOutputStream create(Path f, FsPermission permission,
    * replication, to move the blocks from favored nodes. A value of null means
    * no favored nodes for this create
    */
  public HdfsDataOutputStream create(Path f, FsPermission permission,
      boolean overwrite, int bufferSize, short replication, long blockSize,
      Progressable progress, InetSocketAddress[] favoredNodes) throws IOException {
  public HdfsDataOutputStream create(final Path f,
      final FsPermission permission, final boolean overwrite,
      final int bufferSize, final short replication, final long blockSize,
      final Progressable progress, final InetSocketAddress[] favoredNodes)
          throws IOException {
     statistics.incrementWriteOps(1);
    final DFSOutputStream out = dfs.create(getPathName(f), permission,
        overwrite ? EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE)
            : EnumSet.of(CreateFlag.CREATE),
        true, replication, blockSize, progress, bufferSize, null, favoredNodes);
    return new HdfsDataOutputStream(out, statistics);
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<HdfsDataOutputStream>() {
      @Override
      public HdfsDataOutputStream doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        final DFSOutputStream out = dfs.create(getPathName(f), permission,
            overwrite ? EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE)
                : EnumSet.of(CreateFlag.CREATE),
            true, replication, blockSize, progress, bufferSize, null,
            favoredNodes);
        return new HdfsDataOutputStream(out, statistics);
      }
      @Override
      public HdfsDataOutputStream next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          return myDfs.create(p, permission, overwrite, bufferSize, replication,
              blockSize, progress, favoredNodes);
        }
        throw new UnsupportedOperationException("Cannot create with" +
            " favoredNodes through a symlink to a non-DistributedFileSystem: "
            + f + " -> " + p);
      }
    }.resolve(this, absF);
   }
   
   @Override
  public HdfsDataOutputStream create(Path f, FsPermission permission,
    EnumSet<CreateFlag> cflags, int bufferSize, short replication, long blockSize,
    Progressable progress, ChecksumOpt checksumOpt) throws IOException {
  public FSDataOutputStream create(final Path f, final FsPermission permission,
    final EnumSet<CreateFlag> cflags, final int bufferSize,
    final short replication, final long blockSize, final Progressable progress,
    final ChecksumOpt checksumOpt) throws IOException {
     statistics.incrementWriteOps(1);
    final DFSOutputStream out = dfs.create(getPathName(f), permission, cflags,
        replication, blockSize, progress, bufferSize, checksumOpt);
    return new HdfsDataOutputStream(out, statistics);
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<FSDataOutputStream>() {
      @Override
      public FSDataOutputStream doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return new HdfsDataOutputStream(dfs.create(getPathName(p), permission,
            cflags, replication, blockSize, progress, bufferSize, checksumOpt),
            statistics);
      }
      @Override
      public FSDataOutputStream next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.create(p, permission, cflags, bufferSize,
            replication, blockSize, progress, checksumOpt);
      }
    }.resolve(this, absF);
   }
  
  @SuppressWarnings("deprecation")

   @Override
   protected HdfsDataOutputStream primitiveCreate(Path f,
     FsPermission absolutePermission, EnumSet<CreateFlag> flag, int bufferSize,
     short replication, long blockSize, Progressable progress,
     ChecksumOpt checksumOpt) throws IOException {
     statistics.incrementWriteOps(1);
    return new HdfsDataOutputStream(dfs.primitiveCreate(getPathName(f),
    return new HdfsDataOutputStream(dfs.primitiveCreate(
        getPathName(fixRelativePart(f)),
         absolutePermission, flag, true, replication, blockSize,
         progress, bufferSize, checksumOpt),statistics);
   } 
   }
 
   /**
    * Same as create(), except fails if parent directory doesn't already exist.
    */
   @Override
  public HdfsDataOutputStream createNonRecursive(Path f, FsPermission permission,
      EnumSet<CreateFlag> flag, int bufferSize, short replication,
      long blockSize, Progressable progress) throws IOException {
  @SuppressWarnings("deprecation")
  public FSDataOutputStream createNonRecursive(final Path f,
      final FsPermission permission, final EnumSet<CreateFlag> flag,
      final int bufferSize, final short replication, final long blockSize,
      final Progressable progress) throws IOException {
     statistics.incrementWriteOps(1);
     if (flag.contains(CreateFlag.OVERWRITE)) {
       flag.add(CreateFlag.CREATE);
     }
    return new HdfsDataOutputStream(dfs.create(getPathName(f), permission, flag,
        false, replication, blockSize, progress, 
        bufferSize, null), statistics);
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<FSDataOutputStream>() {
      @Override
      public FSDataOutputStream doCall(final Path p) throws IOException,
          UnresolvedLinkException {
        return new HdfsDataOutputStream(dfs.create(getPathName(p), permission,
            flag, false, replication, blockSize, progress, bufferSize, null),
            statistics);
      }

      @Override
      public FSDataOutputStream next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.createNonRecursive(p, permission, flag, bufferSize,
            replication, blockSize, progress);
      }
    }.resolve(this, absF);
   }
 
   @Override
   public boolean setReplication(Path src, 
                                short replication
                                final short replication
                                ) throws IOException {
     statistics.incrementWriteOps(1);
    return dfs.setReplication(getPathName(src), replication);
    Path absF = fixRelativePart(src);
    return new FileSystemLinkResolver<Boolean>() {
      @Override
      public Boolean doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.setReplication(getPathName(p), replication);
      }
      @Override
      public Boolean next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.setReplication(p, replication);
      }
    }.resolve(this, absF);
   }
   
   /**
@@ -346,12 +469,44 @@ public boolean setReplication(Path src,
    */
   @Override
   public void concat(Path trg, Path [] psrcs) throws IOException {
    String [] srcs = new String [psrcs.length];
    for(int i=0; i<psrcs.length; i++) {
      srcs[i] = getPathName(psrcs[i]);
    }
     statistics.incrementWriteOps(1);
    dfs.concat(getPathName(trg), srcs);
    // Make target absolute
    Path absF = fixRelativePart(trg);
    // Make all srcs absolute
    Path[] srcs = new Path[psrcs.length];
    for (int i=0; i<psrcs.length; i++) {
      srcs[i] = fixRelativePart(psrcs[i]);
    }
    // Try the concat without resolving any links
    String[] srcsStr = new String[psrcs.length];
    try {
      for (int i=0; i<psrcs.length; i++) {
        srcsStr[i] = getPathName(srcs[i]);
      }
      dfs.concat(getPathName(trg), srcsStr);
    } catch (UnresolvedLinkException e) {
      // Exception could be from trg or any src.
      // Fully resolve trg and srcs. Fail if any of them are a symlink.
      FileStatus stat = getFileLinkStatus(absF);
      if (stat.isSymlink()) {
        throw new IOException("Cannot concat with a symlink target: "
            + trg + " -> " + stat.getPath());
      }
      absF = fixRelativePart(stat.getPath());
      for (int i=0; i<psrcs.length; i++) {
        stat = getFileLinkStatus(srcs[i]);
        if (stat.isSymlink()) {
          throw new IOException("Cannot concat with a symlink src: "
              + psrcs[i] + " -> " + stat.getPath());
        }
        srcs[i] = fixRelativePart(stat.getPath());
      }
      // Try concat again. Can still race with another symlink.
      for (int i=0; i<psrcs.length; i++) {
        srcsStr[i] = getPathName(srcs[i]);
      }
      dfs.concat(getPathName(absF), srcsStr);
    }
   }
 
   
@@ -359,7 +514,35 @@ public void concat(Path trg, Path [] psrcs) throws IOException {
   @Override
   public boolean rename(Path src, Path dst) throws IOException {
     statistics.incrementWriteOps(1);
    return dfs.rename(getPathName(src), getPathName(dst));
    // Both Paths have to belong to this DFS
    final Path absSrc = fixRelativePart(src);
    final Path absDst = fixRelativePart(dst);
    FileSystem srcFS = getFSofPath(absSrc, getConf());
    FileSystem dstFS = getFSofPath(absDst, getConf());
    if (!srcFS.getUri().equals(getUri()) ||
        !dstFS.getUri().equals(getUri())) {
      throw new IOException("Renames across FileSystems not supported");
    }
    // Try the rename without resolving first
    try {
      return dfs.rename(getPathName(absSrc), getPathName(absDst));
    } catch (UnresolvedLinkException e) {
      // Fully resolve the source
      final Path source = getFileLinkStatus(absSrc).getPath();
      // Keep trying to resolve the destination
      return new FileSystemLinkResolver<Boolean>() {
        @Override
        public Boolean doCall(final Path p)
            throws IOException, UnresolvedLinkException {
          return dfs.rename(getPathName(source), getPathName(p));
        }
        @Override
        public Boolean next(final FileSystem fs, final Path p)
            throws IOException {
          return fs.rename(source, p);
        }
      }.resolve(this, absDst);
    }
   }
 
   /** 
@@ -367,62 +550,102 @@ public boolean rename(Path src, Path dst) throws IOException {
    */
   @SuppressWarnings("deprecation")
   @Override
  public void rename(Path src, Path dst, Options.Rename... options) throws IOException {
  public void rename(Path src, Path dst, final Options.Rename... options)
      throws IOException {
     statistics.incrementWriteOps(1);
    dfs.rename(getPathName(src), getPathName(dst), options);
    // Both Paths have to belong to this DFS
    final Path absSrc = fixRelativePart(src);
    final Path absDst = fixRelativePart(dst);
    FileSystem srcFS = getFSofPath(absSrc, getConf());
    FileSystem dstFS = getFSofPath(absDst, getConf());
    if (!srcFS.getUri().equals(getUri()) ||
        !dstFS.getUri().equals(getUri())) {
      throw new IOException("Renames across FileSystems not supported");
    }
    // Try the rename without resolving first
    try {
      dfs.rename(getPathName(absSrc), getPathName(absDst), options);
    } catch (UnresolvedLinkException e) {
      // Fully resolve the source
      final Path source = getFileLinkStatus(absSrc).getPath();
      // Keep trying to resolve the destination
      new FileSystemLinkResolver<Void>() {
        @Override
        public Void doCall(final Path p)
            throws IOException, UnresolvedLinkException {
          dfs.rename(getPathName(source), getPathName(p), options);
          return null;
        }
        @Override
        public Void next(final FileSystem fs, final Path p)
            throws IOException {
          // Since we know it's this DFS for both, can just call doCall again
          return doCall(p);
        }
      }.resolve(this, absDst);
    }
   }
   
   @Override
  public boolean delete(Path f, boolean recursive) throws IOException {
  public boolean delete(Path f, final boolean recursive) throws IOException {
     statistics.incrementWriteOps(1);
    return dfs.delete(getPathName(f), recursive);
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<Boolean>() {
      @Override
      public Boolean doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.delete(getPathName(p), recursive);
      }
      @Override
      public Boolean next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.delete(p, recursive);
      }
    }.resolve(this, absF);
   }
   
   @Override
   public ContentSummary getContentSummary(Path f) throws IOException {
     statistics.incrementReadOps(1);
    return dfs.getContentSummary(getPathName(f));
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<ContentSummary>() {
      @Override
      public ContentSummary doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.getContentSummary(getPathName(p));
      }
      @Override
      public ContentSummary next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.getContentSummary(p);
      }
    }.resolve(this, absF);
   }
 
   /** Set a directory's quotas
    * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#setQuota(String, long, long) 
    */
  public void setQuota(Path src, long namespaceQuota, long diskspaceQuota) 
                       throws IOException {
    dfs.setQuota(getPathName(src), namespaceQuota, diskspaceQuota);
  }
  
  private FileStatus makeQualified(HdfsFileStatus f, Path parent) {
    return new FileStatus(f.getLen(), f.isDir(), f.getReplication(),
        f.getBlockSize(), f.getModificationTime(),
        f.getAccessTime(),
        f.getPermission(), f.getOwner(), f.getGroup(),
        (f.getFullPath(parent)).makeQualified(
            getUri(), getWorkingDirectory())); // fully-qualify path
  }

  private LocatedFileStatus makeQualifiedLocated(
      HdfsLocatedFileStatus f, Path parent) {
    return new LocatedFileStatus(f.getLen(), f.isDir(), f.getReplication(),
        f.getBlockSize(), f.getModificationTime(),
        f.getAccessTime(),
        f.getPermission(), f.getOwner(), f.getGroup(),
        null,
        (f.getFullPath(parent)).makeQualified(
            getUri(), getWorkingDirectory()), // fully-qualify path
        DFSUtil.locatedBlocks2Locations(f.getBlockLocations()));
  public void setQuota(Path src, final long namespaceQuota,
      final long diskspaceQuota) throws IOException {
    Path absF = fixRelativePart(src);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.setQuota(getPathName(p), namespaceQuota, diskspaceQuota);
        return null;
      }
      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        // setQuota is not defined in FileSystem, so we only can resolve
        // within this DFS
        return doCall(p);
      }
    }.resolve(this, absF);
   }
 
  /**
   * List all the entries of a directory
   *
   * Note that this operation is not atomic for a large directory.
   * The entries of a directory may be fetched from NameNode multiple times.
   * It only guarantees that  each name occurs once if a directory
   * undergoes changes between the calls.
   */
  @Override
  public FileStatus[] listStatus(Path p) throws IOException {
  private FileStatus[] listStatusInternal(Path p) throws IOException {
     String src = getPathName(p);
 
     // fetch the first batch of entries in the directory
@@ -437,7 +660,7 @@ private LocatedFileStatus makeQualifiedLocated(
     if (!thisListing.hasMore()) { // got all entries of the directory
       FileStatus[] stats = new FileStatus[partialListing.length];
       for (int i = 0; i < partialListing.length; i++) {
        stats[i] = makeQualified(partialListing[i], p);
        stats[i] = partialListing[i].makeQualified(getUri(), p);
       }
       statistics.incrementReadOps(1);
       return stats;
@@ -451,7 +674,7 @@ private LocatedFileStatus makeQualifiedLocated(
       new ArrayList<FileStatus>(totalNumEntries);
     // add the first batch of entries to the array list
     for (HdfsFileStatus fileStatus : partialListing) {
      listing.add(makeQualified(fileStatus, p));
      listing.add(fileStatus.makeQualified(getUri(), p));
     }
     statistics.incrementLargeReadOps(1);
  
@@ -465,7 +688,7 @@ private LocatedFileStatus makeQualifiedLocated(
  
       partialListing = thisListing.getPartialListing();
       for (HdfsFileStatus fileStatus : partialListing) {
        listing.add(makeQualified(fileStatus, p));
        listing.add(fileStatus.makeQualified(getUri(), p));
       }
       statistics.incrementLargeReadOps(1);
     } while (thisListing.hasMore());
@@ -473,6 +696,31 @@ private LocatedFileStatus makeQualifiedLocated(
     return listing.toArray(new FileStatus[listing.size()]);
   }
 
  /**
   * List all the entries of a directory
   *
   * Note that this operation is not atomic for a large directory.
   * The entries of a directory may be fetched from NameNode multiple times.
   * It only guarantees that  each name occurs once if a directory
   * undergoes changes between the calls.
   */
  @Override
  public FileStatus[] listStatus(Path p) throws IOException {
    Path absF = fixRelativePart(p);
    return new FileSystemLinkResolver<FileStatus[]>() {
      @Override
      public FileStatus[] doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return listStatusInternal(p);
      }
      @Override
      public FileStatus[] next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.listStatus(p);
      }
    }.resolve(this, absF);
  }

   @Override
   protected RemoteIterator<LocatedFileStatus> listLocatedStatus(final Path p,
       final PathFilter filter)
@@ -484,7 +732,9 @@ private LocatedFileStatus makeQualifiedLocated(
       private LocatedFileStatus curStat = null;
 
       { // initializer
        src = getPathName(p);
        // Fully resolve symlinks in path first to avoid additional resolution
        // round-trips as we fetch more batches of listings
        src = getPathName(resolvePath(p));
         // fetch the first batch of entries in the directory
         thisListing = dfs.listPaths(src, HdfsFileStatus.EMPTY_NAME, true);
         statistics.incrementReadOps(1);
@@ -496,8 +746,9 @@ private LocatedFileStatus makeQualifiedLocated(
       @Override
       public boolean hasNext() throws IOException {
         while (curStat == null && hasNextNoFilter()) {
          LocatedFileStatus next = makeQualifiedLocated(
              (HdfsLocatedFileStatus)thisListing.getPartialListing()[i++], p);
          LocatedFileStatus next = 
              ((HdfsLocatedFileStatus)thisListing.getPartialListing()[i++])
              .makeQualifiedLocated(getUri(), p);
           if (filter.accept(next.getPath())) {
             curStat = next;
           }
@@ -547,8 +798,7 @@ public LocatedFileStatus next() throws IOException {
    *                    effective permission.
    */
   public boolean mkdir(Path f, FsPermission permission) throws IOException {
    statistics.incrementWriteOps(1);
    return dfs.mkdirs(getPathName(f), permission, false);
    return mkdirsInternal(f, permission, false);
   }
 
   /**
@@ -564,8 +814,32 @@ public boolean mkdir(Path f, FsPermission permission) throws IOException {
    */
   @Override
   public boolean mkdirs(Path f, FsPermission permission) throws IOException {
    return mkdirsInternal(f, permission, true);
  }

  private boolean mkdirsInternal(Path f, final FsPermission permission,
      final boolean createParent) throws IOException {
     statistics.incrementWriteOps(1);
    return dfs.mkdirs(getPathName(f), permission, true);
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<Boolean>() {
      @Override
      public Boolean doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.mkdirs(getPathName(p), permission, createParent);
      }

      @Override
      public Boolean next(final FileSystem fs, final Path p)
          throws IOException {
        // FileSystem doesn't have a non-recursive mkdir() method
        // Best we can do is error out
        if (!createParent) {
          throw new IOException("FileSystem does not support non-recursive"
              + "mkdir");
        }
        return fs.mkdirs(p, permission);
      }
    }.resolve(this, absF);
   }
 
   @SuppressWarnings("deprecation")
@@ -791,42 +1065,207 @@ public boolean reportChecksumFailure(Path f,
   @Override
   public FileStatus getFileStatus(Path f) throws IOException {
     statistics.incrementReadOps(1);
    HdfsFileStatus fi = dfs.getFileInfo(getPathName(f));
    if (fi != null) {
      return makeQualified(fi, f);
    } else {
      throw new FileNotFoundException("File does not exist: " + f);
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<FileStatus>() {
      @Override
      public FileStatus doCall(final Path p) throws IOException,
          UnresolvedLinkException {
        HdfsFileStatus fi = dfs.getFileInfo(getPathName(p));
        if (fi != null) {
          return fi.makeQualified(getUri(), p);
        } else {
          throw new FileNotFoundException("File does not exist: " + p);
        }
      }
      @Override
      public FileStatus next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.getFileStatus(p);
      }
    }.resolve(this, absF);
  }

  @Override
  public void createSymlink(final Path target, final Path link,
      final boolean createParent) throws AccessControlException,
      FileAlreadyExistsException, FileNotFoundException,
      ParentNotDirectoryException, UnsupportedFileSystemException, 
      IOException {
    statistics.incrementWriteOps(1);
    final Path absF = fixRelativePart(link);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p) throws IOException,
          UnresolvedLinkException {
        dfs.createSymlink(target.toString(), getPathName(p), createParent);
        return null;
      }
      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException, UnresolvedLinkException {
        fs.createSymlink(target, p, createParent);
        return null;
      }
    }.resolve(this, absF);
  }

  @Override
  public boolean supportsSymlinks() {
    return true;
  }

  @Override
  public FileStatus getFileLinkStatus(final Path f)
      throws AccessControlException, FileNotFoundException,
      UnsupportedFileSystemException, IOException {
    statistics.incrementReadOps(1);
    final Path absF = fixRelativePart(f);
    FileStatus status = new FileSystemLinkResolver<FileStatus>() {
      @Override
      public FileStatus doCall(final Path p) throws IOException,
          UnresolvedLinkException {
        HdfsFileStatus fi = dfs.getFileLinkInfo(getPathName(p));
        if (fi != null) {
          return fi.makeQualified(getUri(), p);
        } else {
          throw new FileNotFoundException("File does not exist: " + p);
        }
      }
      @Override
      public FileStatus next(final FileSystem fs, final Path p)
        throws IOException, UnresolvedLinkException {
        return fs.getFileLinkStatus(p);
      }
    }.resolve(this, absF);
    // Fully-qualify the symlink
    if (status.isSymlink()) {
      Path targetQual = FSLinkResolver.qualifySymlinkTarget(this.getUri(),
          status.getPath(), status.getSymlink());
      status.setSymlink(targetQual);
    }
    return status;
  }

  @Override
  public Path getLinkTarget(final Path f) throws AccessControlException,
      FileNotFoundException, UnsupportedFileSystemException, IOException {
    statistics.incrementReadOps(1);
    final Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<Path>() {
      @Override
      public Path doCall(final Path p) throws IOException,
          UnresolvedLinkException {
        HdfsFileStatus fi = dfs.getFileLinkInfo(getPathName(p));
        if (fi != null) {
          return fi.makeQualified(getUri(), p).getSymlink();
        } else {
          throw new FileNotFoundException("File does not exist: " + p);
        }
      }
      @Override
      public Path next(final FileSystem fs, final Path p)
        throws IOException, UnresolvedLinkException {
        return fs.getLinkTarget(p);
      }
    }.resolve(this, absF);
  }

  @Override
  protected Path resolveLink(Path f) throws IOException {
    statistics.incrementReadOps(1);
    String target = dfs.getLinkTarget(getPathName(fixRelativePart(f)));
    if (target == null) {
      throw new FileNotFoundException("File does not exist: " + f.toString());
     }
    return new Path(target);
   }
 
   @Override
  public MD5MD5CRC32FileChecksum getFileChecksum(Path f) throws IOException {
  public FileChecksum getFileChecksum(Path f) throws IOException {
     statistics.incrementReadOps(1);
    return dfs.getFileChecksum(getPathName(f));
    Path absF = fixRelativePart(f);
    return new FileSystemLinkResolver<FileChecksum>() {
      @Override
      public FileChecksum doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.getFileChecksum(getPathName(p));
      }

      @Override
      public FileChecksum next(final FileSystem fs, final Path p)
          throws IOException {
        return fs.getFileChecksum(p);
      }
    }.resolve(this, absF);
   }
 
   @Override
  public void setPermission(Path p, FsPermission permission
  public void setPermission(Path p, final FsPermission permission
       ) throws IOException {
     statistics.incrementWriteOps(1);
    dfs.setPermission(getPathName(p), permission);
    Path absF = fixRelativePart(p);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.setPermission(getPathName(p), permission);
        return null;
      }

      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        fs.setPermission(p, permission);
        return null;
      }
    }.resolve(this, absF);
   }
 
   @Override
  public void setOwner(Path p, String username, String groupname
  public void setOwner(Path p, final String username, final String groupname
       ) throws IOException {
     if (username == null && groupname == null) {
       throw new IOException("username == null && groupname == null");
     }
     statistics.incrementWriteOps(1);
    dfs.setOwner(getPathName(p), username, groupname);
    Path absF = fixRelativePart(p);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.setOwner(getPathName(p), username, groupname);
        return null;
      }

      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        fs.setOwner(p, username, groupname);
        return null;
      }
    }.resolve(this, absF);
   }
 
   @Override
  public void setTimes(Path p, long mtime, long atime
  public void setTimes(Path p, final long mtime, final long atime
       ) throws IOException {
     statistics.incrementWriteOps(1);
    dfs.setTimes(getPathName(p), mtime, atime);
    Path absF = fixRelativePart(p);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.setTimes(getPathName(p), mtime, atime);
        return null;
      }

      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        fs.setTimes(p, mtime, atime);
        return null;
      }
    }.resolve(this, absF);
   }
   
 
@@ -902,7 +1341,7 @@ public void cancelDelegationToken(Token<DelegationTokenIdentifier> token)
    * The bandwidth parameter is the max number of bytes per second of network
    * bandwidth to be used by a datanode during balancing.
    *
   * @param bandwidth Blanacer bandwidth in bytes per second for all datanodes.
   * @param bandwidth Balancer bandwidth in bytes per second for all datanodes.
    * @throws IOException
    */
   public void setBalancerBandwidth(long bandwidth) throws IOException {
@@ -943,25 +1382,111 @@ public boolean isInSafeMode() throws IOException {
   }
 
   /** @see HdfsAdmin#allowSnapshot(Path) */
  public void allowSnapshot(Path path) throws IOException {
    dfs.allowSnapshot(getPathName(path));
  public void allowSnapshot(final Path path) throws IOException {
    Path absF = fixRelativePart(path);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.allowSnapshot(getPathName(p));
        return null;
      }

      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          myDfs.allowSnapshot(p);
        } else {
          throw new UnsupportedOperationException("Cannot perform snapshot"
              + " operations on a symlink to a non-DistributedFileSystem: "
              + path + " -> " + p);
        }
        return null;
      }
    }.resolve(this, absF);
   }
   
   /** @see HdfsAdmin#disallowSnapshot(Path) */
  public void disallowSnapshot(Path path) throws IOException {
    dfs.disallowSnapshot(getPathName(path));
  public void disallowSnapshot(final Path path) throws IOException {
    Path absF = fixRelativePart(path);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.disallowSnapshot(getPathName(p));
        return null;
      }

      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          myDfs.disallowSnapshot(p);
        } else {
          throw new UnsupportedOperationException("Cannot perform snapshot"
              + " operations on a symlink to a non-DistributedFileSystem: "
              + path + " -> " + p);
        }
        return null;
      }
    }.resolve(this, absF);
   }
   
   @Override
  public Path createSnapshot(Path path, String snapshotName) 
  public Path createSnapshot(final Path path, final String snapshotName) 
       throws IOException {
    return new Path(dfs.createSnapshot(getPathName(path), snapshotName));
    Path absF = fixRelativePart(path);
    return new FileSystemLinkResolver<Path>() {
      @Override
      public Path doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return new Path(dfs.createSnapshot(getPathName(p), snapshotName));
      }

      @Override
      public Path next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          return myDfs.createSnapshot(p);
        } else {
          throw new UnsupportedOperationException("Cannot perform snapshot"
              + " operations on a symlink to a non-DistributedFileSystem: "
              + path + " -> " + p);
        }
      }
    }.resolve(this, absF);
   }
   
   @Override
  public void renameSnapshot(Path path, String snapshotOldName,
      String snapshotNewName) throws IOException {
    dfs.renameSnapshot(getPathName(path), snapshotOldName, snapshotNewName);
  public void renameSnapshot(final Path path, final String snapshotOldName,
      final String snapshotNewName) throws IOException {
    Path absF = fixRelativePart(path);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.renameSnapshot(getPathName(p), snapshotOldName, snapshotNewName);
        return null;
      }

      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          myDfs.renameSnapshot(p, snapshotOldName, snapshotNewName);
        } else {
          throw new UnsupportedOperationException("Cannot perform snapshot"
              + " operations on a symlink to a non-DistributedFileSystem: "
              + path + " -> " + p);
        }
        return null;
      }
    }.resolve(this, absF);
   }
   
   /**
@@ -974,9 +1499,31 @@ public void renameSnapshot(Path path, String snapshotOldName,
   }
   
   @Override
  public void deleteSnapshot(Path snapshotDir, String snapshotName)
  public void deleteSnapshot(final Path snapshotDir, final String snapshotName)
       throws IOException {
    dfs.deleteSnapshot(getPathName(snapshotDir), snapshotName);
    Path absF = fixRelativePart(snapshotDir);
    new FileSystemLinkResolver<Void>() {
      @Override
      public Void doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        dfs.deleteSnapshot(getPathName(p), snapshotName);
        return null;
      }

      @Override
      public Void next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          myDfs.deleteSnapshot(p, snapshotName);
        } else {
          throw new UnsupportedOperationException("Cannot perform snapshot"
              + " operations on a symlink to a non-DistributedFileSystem: "
              + snapshotDir + " -> " + p);
        }
        return null;
      }
    }.resolve(this, absF);
   }
 
   /**
@@ -985,9 +1532,31 @@ public void deleteSnapshot(Path snapshotDir, String snapshotName)
    * 
    * @see DFSClient#getSnapshotDiffReport(Path, String, String)
    */
  public SnapshotDiffReport getSnapshotDiffReport(Path snapshotDir,
      String fromSnapshot, String toSnapshot) throws IOException {
    return dfs.getSnapshotDiffReport(getPathName(snapshotDir), fromSnapshot, toSnapshot);
  public SnapshotDiffReport getSnapshotDiffReport(final Path snapshotDir,
      final String fromSnapshot, final String toSnapshot) throws IOException {
    Path absF = fixRelativePart(snapshotDir);
    return new FileSystemLinkResolver<SnapshotDiffReport>() {
      @Override
      public SnapshotDiffReport doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.getSnapshotDiffReport(getPathName(p), fromSnapshot,
            toSnapshot);
      }

      @Override
      public SnapshotDiffReport next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          myDfs.getSnapshotDiffReport(p, fromSnapshot, toSnapshot);
        } else {
          throw new UnsupportedOperationException("Cannot perform snapshot"
              + " operations on a symlink to a non-DistributedFileSystem: "
              + snapshotDir + " -> " + p);
        }
        return null;
      }
    }.resolve(this, absF);
   }
  
   /**
@@ -998,8 +1567,28 @@ public SnapshotDiffReport getSnapshotDiffReport(Path snapshotDir,
    * @throws FileNotFoundException if the file does not exist.
    * @throws IOException If an I/O error occurred     
    */
  public boolean isFileClosed(Path src) throws IOException {
    return dfs.isFileClosed(getPathName(src));
  public boolean isFileClosed(final Path src) throws IOException {
    Path absF = fixRelativePart(src);
    return new FileSystemLinkResolver<Boolean>() {
      @Override
      public Boolean doCall(final Path p)
          throws IOException, UnresolvedLinkException {
        return dfs.isFileClosed(getPathName(p));
      }

      @Override
      public Boolean next(final FileSystem fs, final Path p)
          throws IOException {
        if (fs instanceof DistributedFileSystem) {
          DistributedFileSystem myDfs = (DistributedFileSystem)fs;
          return myDfs.isFileClosed(p);
        } else {
          throw new UnsupportedOperationException("Cannot call isFileClosed"
              + " on a symlink to a non-DistributedFileSystem: "
              + src + " -> " + p);
        }
      }
    }.resolve(this, absF);
   }
   
 }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java
index 38465679dfe..c56bcb985aa 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java
@@ -51,6 +51,7 @@
 import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
 import org.apache.hadoop.hdfs.server.namenode.NameNode;
 import org.apache.hadoop.hdfs.server.namenode.TransferFsImage;
import org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotException;
 import org.apache.hadoop.ipc.RPC;
 import org.apache.hadoop.ipc.RemoteException;
 import org.apache.hadoop.net.NetUtils;
@@ -414,7 +415,11 @@ public void setSafeMode(String[] argv, int idx) throws IOException {
    */
   public void allowSnapshot(String[] argv) throws IOException {   
     DistributedFileSystem dfs = getDFS();
    dfs.allowSnapshot(new Path(argv[1]));
    try {
      dfs.allowSnapshot(new Path(argv[1]));
    } catch (SnapshotException e) {
      throw new RemoteException(e.getClass().getName(), e.getMessage());
    }
     System.out.println("Allowing snaphot on " + argv[1] + " succeeded");
   }
   
@@ -426,7 +431,11 @@ public void allowSnapshot(String[] argv) throws IOException {
    */
   public void disallowSnapshot(String[] argv) throws IOException {  
     DistributedFileSystem dfs = getDFS();
    dfs.disallowSnapshot(new Path(argv[1]));
    try {
      dfs.disallowSnapshot(new Path(argv[1]));
    } catch (SnapshotException e) {
      throw new RemoteException(e.getClass().getName(), e.getMessage());
    }
     System.out.println("Disallowing snaphot on " + argv[1] + " succeeded");
   }
   
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestSymlinkHdfsFileSystem.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestSymlinkHdfsFileSystem.java
new file mode 100644
index 00000000000..bf42e24b01d
-- /dev/null
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/fs/TestSymlinkHdfsFileSystem.java
@@ -0,0 +1,107 @@
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestSymlinkHdfsFileSystem extends TestSymlinkHdfs {

  @BeforeClass
  public static void testSetup() throws Exception {
    wrapper = new FileSystemTestWrapper(dfs, "/tmp/TestSymlinkHdfsFileSystem");
  }

  @Override
  @Ignore("FileSystem adds missing authority in absolute URIs")
  @Test(timeout=1000)
  public void testCreateWithPartQualPathFails() throws IOException {}

  @Ignore("FileSystem#create creates parent directories," +
      " so dangling links to directories are created")
  @Override
  @Test(timeout=1000)
  public void testCreateFileViaDanglingLinkParent() throws IOException {}

  // Additional tests for DFS-only methods

  @Test(timeout=10000)
  public void testRecoverLease() throws IOException {
    Path dir  = new Path(testBaseDir1());
    Path file = new Path(testBaseDir1(), "file");
    Path link = new Path(testBaseDir1(), "link");
    wrapper.setWorkingDirectory(dir);
    createAndWriteFile(file);
    wrapper.createSymlink(file, link, false);
    // Attempt recoverLease through a symlink
    boolean closed = dfs.recoverLease(link);
    assertTrue("Expected recoverLease to return true", closed);
  }

  @Test(timeout=10000)
  public void testIsFileClosed() throws IOException {
    Path dir  = new Path(testBaseDir1());
    Path file = new Path(testBaseDir1(), "file");
    Path link = new Path(testBaseDir1(), "link");
    wrapper.setWorkingDirectory(dir);
    createAndWriteFile(file);
    wrapper.createSymlink(file, link, false);
    // Attempt recoverLease through a symlink
    boolean closed = dfs.isFileClosed(link);
    assertTrue("Expected isFileClosed to return true", closed);
  }

  @Test(timeout=10000)
  public void testConcat() throws Exception {
    Path dir  = new Path(testBaseDir1());
    Path link = new Path(testBaseDir1(), "link");
    Path dir2 = new Path(testBaseDir2());
    wrapper.createSymlink(dir2, link, false);
    wrapper.setWorkingDirectory(dir);
    // Concat with a target and srcs through a link
    Path target = new Path(link, "target");
    createAndWriteFile(target);
    Path[] srcs = new Path[3];
    for (int i=0; i<srcs.length; i++) {
      srcs[i] = new Path(link, "src-" + i);
      createAndWriteFile(srcs[i]);
    }
    dfs.concat(target, srcs);
  }

  @Test(timeout=10000)
  public void testSnapshot() throws Exception {
    Path dir  = new Path(testBaseDir1());
    Path link = new Path(testBaseDir1(), "link");
    Path dir2 = new Path(testBaseDir2());
    wrapper.createSymlink(dir2, link, false);
    wrapper.setWorkingDirectory(dir);
    dfs.allowSnapshot(link);
    dfs.disallowSnapshot(link);
    dfs.allowSnapshot(link);
    dfs.createSnapshot(link, "mcmillan");
    dfs.renameSnapshot(link, "mcmillan", "seaborg");
    dfs.deleteSnapshot(link, "seaborg");
  }
}
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDistributedFileSystem.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDistributedFileSystem.java
index 9e2fd277b47..9b4f3130d7f 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDistributedFileSystem.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDistributedFileSystem.java
@@ -157,6 +157,13 @@ public void testDFSCloseOrdering() throws Exception {
     public boolean exists(Path p) {
       return true; // trick out deleteOnExit
     }
    // Symlink resolution doesn't work with a mock, since it doesn't
    // have a valid Configuration to resolve paths to the right FileSystem.
    // Just call the DFSClient directly to register the delete
    @Override
    public boolean delete(Path f, final boolean recursive) throws IOException {
      return dfs.delete(f.toUri().getPath(), recursive);
    }
   }
 
   @Test
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/namenode/snapshot/TestNestedSnapshots.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/namenode/snapshot/TestNestedSnapshots.java
index a983098224d..8ee8e48df59 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/namenode/snapshot/TestNestedSnapshots.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/namenode/snapshot/TestNestedSnapshots.java
@@ -135,7 +135,7 @@ public void testNestedSnapshots() throws Exception {
     try {
       hdfs.disallowSnapshot(rootPath);
       fail("Expect snapshot exception when disallowing snapshot on root again");
    } catch (RemoteException e) {
    } catch (SnapshotException e) {
       GenericTestUtils.assertExceptionContains(
           "Root is not a snapshottable directory", e);
     }
@@ -149,16 +149,16 @@ public void testNestedSnapshots() throws Exception {
     try {
       hdfs.allowSnapshot(rootPath);
       Assert.fail();
    } catch(RemoteException se) {
    } catch (SnapshotException se) {
       assertNestedSnapshotException(
          (SnapshotException) se.unwrapRemoteException(), "subdirectory");
          se, "subdirectory");
     }
     try {
       hdfs.allowSnapshot(foo);
       Assert.fail();
    } catch(RemoteException se) {
    } catch (SnapshotException se) {
       assertNestedSnapshotException(
          (SnapshotException) se.unwrapRemoteException(), "subdirectory");
          se, "subdirectory");
     }
 
     final Path sub1Bar = new Path(bar, "sub1");
@@ -167,16 +167,16 @@ public void testNestedSnapshots() throws Exception {
     try {
       hdfs.allowSnapshot(sub1Bar);
       Assert.fail();
    } catch(RemoteException se) {
    } catch (SnapshotException se) {
       assertNestedSnapshotException(
          (SnapshotException) se.unwrapRemoteException(), "ancestor");
          se, "ancestor");
     }
     try {
       hdfs.allowSnapshot(sub2Bar);
       Assert.fail();
    } catch(RemoteException se) {
    } catch (SnapshotException se) {
       assertNestedSnapshotException(
          (SnapshotException) se.unwrapRemoteException(), "ancestor");
          se, "ancestor");
     }
   }
   
- 
2.19.1.windows.1

