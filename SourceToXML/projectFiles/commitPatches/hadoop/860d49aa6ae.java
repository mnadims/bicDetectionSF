From 860d49aa6ae7790d970d7f2322fed890b0e5cda2 Mon Sep 17 00:00:00 2001
From: Mingliang Liu <liuml07@apache.org>
Date: Wed, 23 Nov 2016 16:32:42 -0800
Subject: [PATCH] HADOOP-13605. Clean up FileSystem javadocs, logging; improve
 diagnostics on FS load. Contributed by Steve Loughran

--
 .../java/org/apache/hadoop/fs/FileSystem.java | 1449 ++++++++++-------
 .../site/markdown/filesystem/filesystem.md    |    2 +-
 .../org/apache/hadoop/fs/TestDefaultUri.java  |   40 +-
 .../hadoop/fs/TestFileSystemCaching.java      |    8 +-
 4 files changed, 920 insertions(+), 579 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
index 9e984559149..f581f613935 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileSystem.java
@@ -1,4 +1,4 @@
/**
/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
@@ -75,6 +75,8 @@
 
 import com.google.common.base.Preconditions;
 import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.*;
@@ -88,79 +90,118 @@
  * <p>
  *
  * All user code that may potentially use the Hadoop Distributed
 * File System should be written to use a FileSystem object.  The
 * Hadoop DFS is a multi-machine system that appears as a single
 * disk.  It's useful because of its fault tolerance and potentially
 * very large capacity.
 * 
 * File System should be written to use a FileSystem object or its
 * successor, {@link FileContext}.
 *
  * <p>
  * The local implementation is {@link LocalFileSystem} and distributed
 * implementation is DistributedFileSystem.
 * implementation is DistributedFileSystem. There are other implementations
 * for object stores and (outside the Apache Hadoop codebase),
 * third party filesystems.
 * <p>
 * Notes
 * <ol>
 * <li>The behaviour of the filesystem is
 * <a href="https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/filesystem/filesystem.html">
 * specified in the Hadoop documentation. </a>
 * However, the normative specification of the behavior of this class is
 * actually HDFS: if HDFS does not behave the way these Javadocs or
 * the specification in the Hadoop documentations define, assume that
 * the documentation is incorrect.
 * </li>
 * <li>The term {@code FileSystem} refers to an instance of this class.</li>
 * <li>The acronym "FS" is used as an abbreviation of FileSystem.</li>
 * <li>The term {@code filesystem} refers to the distributed/local filesystem
 * itself, rather than the class used to interact with it.</li>
 * <li>The term "file" refers to a file in the remote filesystem,
 * rather than instances of {@code java.io.File}.</li>
 * </ol>
  *****************************************************************/
@SuppressWarnings("DeprecatedIsStillUsed")
 @InterfaceAudience.Public
 @InterfaceStability.Stable
 public abstract class FileSystem extends Configured implements Closeable {
  public static final String FS_DEFAULT_NAME_KEY = 
  public static final String FS_DEFAULT_NAME_KEY =
                    CommonConfigurationKeys.FS_DEFAULT_NAME_KEY;
  public static final String DEFAULT_FS = 
  public static final String DEFAULT_FS =
                    CommonConfigurationKeys.FS_DEFAULT_NAME_DEFAULT;
 
  /**
   * This log is widely used in the org.apache.hadoop.fs code and tests,
   * so must be considered something to only be changed with care.
   */
  @InterfaceAudience.Private
   public static final Log LOG = LogFactory.getLog(FileSystem.class);
 
   /**
   * Priority of the FileSystem shutdown hook.
   * The SLF4J logger to use in logging within the FileSystem class itself.
   */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(FileSystem.class);

  /**
   * Priority of the FileSystem shutdown hook: {@value}.
    */
   public static final int SHUTDOWN_HOOK_PRIORITY = 10;
 
  /**
   * Prefix for trash directory: {@value}.
   */
   public static final String TRASH_PREFIX = ".Trash";
   public static final String USER_HOME_PREFIX = "/user";
 
  /** FileSystem cache */
  /** FileSystem cache. */
   static final Cache CACHE = new Cache();
 
   /** The key this instance is stored under in the cache. */
   private Cache.Key key;
 
  /** Recording statistics per a FileSystem class */
  private static final Map<Class<? extends FileSystem>, Statistics> 
    statisticsTable =
      new IdentityHashMap<Class<? extends FileSystem>, Statistics>();
  
  /** Recording statistics per a FileSystem class. */
  private static final Map<Class<? extends FileSystem>, Statistics>
      statisticsTable = new IdentityHashMap<>();

   /**
    * The statistics for this file system.
    */
   protected Statistics statistics;
 
   /**
   * A cache of files that should be deleted when filesystem is closed
   * A cache of files that should be deleted when the FileSystem is closed
    * or the JVM is exited.
    */
  private Set<Path> deleteOnExit = new TreeSet<Path>();
  
  private final Set<Path> deleteOnExit = new TreeSet<>();

  /**
   * Should symbolic links be resolved by {@link FileSystemLinkResolver}.
   * Set to the value of
   * {@link CommonConfigurationKeysPublic#FS_CLIENT_RESOLVE_REMOTE_SYMLINKS_KEY}
   */
   boolean resolveSymlinks;
 
   /**
   * This method adds a file system for testing so that we can find it later. It
   * is only for testing.
   * This method adds a FileSystem instance to the cache so that it can
   * be retrieved later. It is only for testing.
    * @param uri the uri to store it under
    * @param conf the configuration to store it under
   * @param fs the file system to store
   * @throws IOException
   * @param fs the FileSystem to store
   * @throws IOException if the current user cannot be determined.
    */
  @VisibleForTesting
   static void addFileSystemForTesting(URI uri, Configuration conf,
       FileSystem fs) throws IOException {
     CACHE.map.put(new Cache.Key(uri, conf), fs);
   }
 
   /**
   * Get a filesystem instance based on the uri, the passed
   * configuration and the user
   * Get a FileSystem instance based on the uri, the passed in
   * configuration and the user.
    * @param uri of the filesystem
    * @param conf the configuration to use
    * @param user to perform the get as
    * @return the filesystem instance
   * @throws IOException
   * @throws InterruptedException
   * @throws IOException failure to load
   * @throws InterruptedException If the {@code UGI.doAs()} call was
   * somehow interrupted.
    */
   public static FileSystem get(final URI uri, final Configuration conf,
         final String user) throws IOException, InterruptedException {
@@ -177,14 +218,15 @@ public FileSystem run() throws IOException {
   }
 
   /**
   * Returns the configured filesystem implementation.
   * Returns the configured FileSystem implementation.
    * @param conf the configuration to use
    */
   public static FileSystem get(Configuration conf) throws IOException {
     return get(getDefaultUri(conf), conf);
   }
  
  /** Get the default filesystem URI from a configuration.

  /**
   * Get the default FileSystem URI from a configuration.
    * @param conf the configuration to use
    * @return the uri of the default filesystem
    */
@@ -196,7 +238,8 @@ public static URI getDefaultUri(Configuration conf) {
     return uri;
   }
 
  /** Set the default filesystem URI in a configuration.
  /**
   * Set the default FileSystem URI in a configuration.
    * @param conf the configuration to alter
    * @param uri the new default filesystem uri
    */
@@ -204,7 +247,7 @@ public static void setDefaultUri(Configuration conf, URI uri) {
     conf.set(FS_DEFAULT_NAME_KEY, uri.toString());
   }
 
  /** Set the default filesystem URI in a configuration.
  /** Set the default FileSystem URI in a configuration.
    * @param conf the configuration to alter
    * @param uri the new default filesystem uri
    */
@@ -212,10 +255,21 @@ public static void setDefaultUri(Configuration conf, String uri) {
     setDefaultUri(conf, URI.create(fixName(uri)));
   }
 
  /** Called after a new FileSystem instance is constructed.
   * @param name a uri whose authority section names the host, port, etc.
  /**
   * Initialize a FileSystem.
   *
   * Called after the new FileSystem instance is constructed, and before it
   * is ready for use.
   *
   * FileSystem implementations overriding this method MUST forward it to
   * their superclass, though the order in which it is done, and whether
   * to alter the configuration before the invocation are options of the
   * subclass.
   * @param name a URI whose authority section names the host, port, etc.
    *   for this FileSystem
    * @param conf the configuration
   * @throws IOException on any failure to initialize this instance.
   * @throws IllegalArgumentException if the URI is considered invalid.
    */
   public void initialize(URI name, Configuration conf) throws IOException {
     final String scheme;
@@ -226,27 +280,34 @@ public void initialize(URI name, Configuration conf) throws IOException {
     }
     statistics = getStatistics(scheme, getClass());
     resolveSymlinks = conf.getBoolean(
        CommonConfigurationKeys.FS_CLIENT_RESOLVE_REMOTE_SYMLINKS_KEY,
        CommonConfigurationKeys.FS_CLIENT_RESOLVE_REMOTE_SYMLINKS_DEFAULT);
        CommonConfigurationKeysPublic.FS_CLIENT_RESOLVE_REMOTE_SYMLINKS_KEY,
        CommonConfigurationKeysPublic.FS_CLIENT_RESOLVE_REMOTE_SYMLINKS_DEFAULT);
   }
 
   /**
   * Return the protocol scheme for the FileSystem.
   * <p/>
   * Return the protocol scheme for this FileSystem.
   * <p>
    * This implementation throws an <code>UnsupportedOperationException</code>.
    *
   * @return the protocol scheme for the FileSystem.
   * @return the protocol scheme for this FileSystem.
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default).
    */
   public String getScheme() {
    throw new UnsupportedOperationException("Not implemented by the " + getClass().getSimpleName() + " FileSystem implementation");
    throw new UnsupportedOperationException("Not implemented by the "
        + getClass().getSimpleName() + " FileSystem implementation");
   }
 
  /** Returns a URI whose scheme and authority identify this FileSystem.*/
  /**
   * Returns a URI which identifies this FileSystem.
   *
   * @return the URI of this filesystem.
   */
   public abstract URI getUri();
  

   /**
    * Return a canonicalized form of this FileSystem's URI.
   * 
   *
    * The default implementation simply calls {@link #canonicalizeUri(URI)}
    * on the filesystem's own URI, so subclasses typically only need to
    * implement that method.
@@ -256,16 +317,17 @@ public String getScheme() {
   protected URI getCanonicalUri() {
     return canonicalizeUri(getUri());
   }
  

   /**
    * Canonicalize the given URI.
   * 
   * This is filesystem-dependent, but may for example consist of
   *
   * This is implementation-dependent, and may for example consist of
    * canonicalizing the hostname using DNS and adding the default
    * port if not specified.
   * 
   *
    * The default implementation simply fills in the default port if
   * not specified and if the filesystem has a default port.
   * not specified and if {@link #getDefaultPort()} returns a
   * default port.
    *
    * @return URI
    * @see NetUtils#getCanonicalUri(URI, int)
@@ -283,12 +345,12 @@ protected URI canonicalizeUri(URI uri) {
             uri);
       }
     }
    

     return uri;
   }
  

   /**
   * Get the default port for this file system.
   * Get the default port for this FileSystem.
    * @return the default port or 0 if there isn't one
    */
   protected int getDefaultPort() {
@@ -301,25 +363,25 @@ protected static FileSystem getFSofPath(final Path absOrFqPath,
     absOrFqPath.checkNotSchemeWithRelative();
     absOrFqPath.checkNotRelative();
 
    // Uses the default file system if not fully qualified
    // Uses the default FileSystem if not fully qualified
     return get(absOrFqPath.toUri(), conf);
   }
 
   /**
   * Get a canonical service name for this file system.  The token cache is
   * the only user of the canonical service name, and uses it to lookup this
   * filesystem's service tokens.
   * If file system provides a token of its own then it must have a canonical
   * name, otherwise canonical name can be null.
   * 
   * Default Impl: If the file system has child file systems 
   * (such as an embedded file system) then it is assumed that the fs has no
   * Get a canonical service name for this FileSystem.
   * The token cache is the only user of the canonical service name,
   * and uses it to lookup this FileSystem's service tokens.
   * If the file system provides a token of its own then it must have a
   * canonical name, otherwise the canonical name can be null.
   *
   * Default implementation: If the FileSystem has child file systems
   * (such as an embedded file system) then it is assumed that the FS has no
    * tokens of its own and hence returns a null name; otherwise a service
    * name is built using Uri and port.
   * 
   *
    * @return a service string that uniquely identifies this file system, null
    *         if the filesystem does not implement tokens
   * @see SecurityUtil#buildDTServiceName(URI, int) 
   * @see SecurityUtil#buildDTServiceName(URI, int)
    */
   @InterfaceAudience.LimitedPrivate({ "HDFS", "MapReduce" })
   public String getCanonicalServiceName() {
@@ -328,28 +390,29 @@ public String getCanonicalServiceName() {
       : null;
   }
 
  /** @deprecated call #getUri() instead.*/
  /** @deprecated call {@link #getUri()} instead.*/
   @Deprecated
   public String getName() { return getUri().toString(); }
 
  /** @deprecated call #get(URI,Configuration) instead. */
  /** @deprecated call {@link #get(URI, Configuration)} instead. */
   @Deprecated
   public static FileSystem getNamed(String name, Configuration conf)
     throws IOException {
     return get(URI.create(fixName(name)), conf);
   }
  

   /** Update old-format filesystem names, for back-compatibility.  This should
    * eventually be replaced with a checkName() method that throws an exception
   * for old-format names. */ 
   * for old-format names.
   */
   private static String fixName(String name) {
     // convert old-format name to new-format name
     if (name.equals("local")) {         // "local" is now "file:///".
      LOG.warn("\"local\" is a deprecated filesystem name."
      LOGGER.warn("\"local\" is a deprecated filesystem name."
                +" Use \"file:///\" instead.");
       name = "file:///";
     } else if (name.indexOf('/')==-1) {   // unqualified is "hdfs://"
      LOG.warn("\""+name+"\" is a deprecated filesystem name."
      LOGGER.warn("\""+name+"\" is a deprecated filesystem name."
                +" Use \"hdfs://"+name+"/\" instead.");
       name = "hdfs://"+name;
     }
@@ -357,19 +420,36 @@ private static String fixName(String name) {
   }
 
   /**
   * Get the local file system.
   * @param conf the configuration to configure the file system with
   * Get the local FileSystem.
   * @param conf the configuration to configure the FileSystem with
   * if it is newly instantiated.
    * @return a LocalFileSystem
   * @throws IOException if somehow the local FS cannot be instantiated.
    */
   public static LocalFileSystem getLocal(Configuration conf)
     throws IOException {
     return (LocalFileSystem)get(LocalFileSystem.NAME, conf);
   }
 
  /** Returns the FileSystem for this URI's scheme and authority.  The scheme
   * of the URI determines a configuration property name,
   * <tt>fs.<i>scheme</i>.class</tt> whose value names the FileSystem class.
   * The entire URI is passed to the FileSystem instance's initialize method.
  /**
   * Get a FileSystem for this URI's scheme and authority.
   * <ol>
   * <li>
   *   If the configuration has the property
   *   {@code "fs.$SCHEME.impl.disable.cache"} set to true,
   *   a new instance will be created, initialized with the supplied URI and
   *   configuration, then returned without being cached.
   * </li>
   * <li>
   *   If the there is a cached FS instance matching the same URI, it will
   *   be returned.
   * </li>
   * <li>
   *   Otherwise: a new FS instance will be created, initialized with the
   *   configuration and URI, cached and returned to the caller.
   * </li>
   * </ol>
   * @throws IOException if the FileSystem cannot be instantiated.
    */
   public static FileSystem get(URI uri, Configuration conf) throws IOException {
     String scheme = uri.getScheme();
@@ -386,9 +466,9 @@ public static FileSystem get(URI uri, Configuration conf) throws IOException {
         return get(defaultUri, conf);              // return default
       }
     }
    
     String disableCacheName = String.format("fs.%s.impl.disable.cache", scheme);
     if (conf.getBoolean(disableCacheName, false)) {
      LOGGER.debug("Bypassing cache to create filesystem {}", uri);
       return createFileSystem(uri, conf);
     }
 
@@ -396,14 +476,15 @@ public static FileSystem get(URI uri, Configuration conf) throws IOException {
   }
 
   /**
   * Returns the FileSystem for this URI's scheme and authority and the 
   * passed user. Internally invokes {@link #newInstance(URI, Configuration)}
   * Returns the FileSystem for this URI's scheme and authority and the
   * given user. Internally invokes {@link #newInstance(URI, Configuration)}
    * @param uri of the filesystem
    * @param conf the configuration to use
    * @param user to perform the get as
    * @return filesystem instance
   * @throws IOException
   * @throws InterruptedException
   * @throws IOException if the FileSystem cannot be instantiated.
   * @throws InterruptedException If the {@code UGI.doAs()} call was
   *         somehow interrupted.
    */
   public static FileSystem newInstance(final URI uri, final Configuration conf,
       final String user) throws IOException, InterruptedException {
@@ -414,47 +495,56 @@ public static FileSystem newInstance(final URI uri, final Configuration conf,
     return ugi.doAs(new PrivilegedExceptionAction<FileSystem>() {
       @Override
       public FileSystem run() throws IOException {
        return newInstance(uri,conf); 
        return newInstance(uri, conf);
       }
     });
   }
  /** Returns the FileSystem for this URI's scheme and authority.  The scheme
   * of the URI determines a configuration property name,
   * <tt>fs.<i>scheme</i>.class</tt> whose value names the FileSystem class.

  /**
   * Returns the FileSystem for this URI's scheme and authority.
    * The entire URI is passed to the FileSystem instance's initialize method.
    * This always returns a new FileSystem object.
   * @param uri FS URI
   * @param config configuration to use
   * @return the new FS instance
   * @throws IOException FS creation or initialization failure.
    */
  public static FileSystem newInstance(URI uri, Configuration conf) throws IOException {
  public static FileSystem newInstance(URI uri, Configuration config)
      throws IOException {
     String scheme = uri.getScheme();
     String authority = uri.getAuthority();
 
     if (scheme == null) {                       // no scheme: use default FS
      return newInstance(conf);
      return newInstance(config);
     }
 
     if (authority == null) {                       // no authority
      URI defaultUri = getDefaultUri(conf);
      URI defaultUri = getDefaultUri(config);
       if (scheme.equals(defaultUri.getScheme())    // if scheme matches default
           && defaultUri.getAuthority() != null) {  // & default has authority
        return newInstance(defaultUri, conf);              // return default
        return newInstance(defaultUri, config);              // return default
       }
     }
    return CACHE.getUnique(uri, conf);
    return CACHE.getUnique(uri, config);
   }
 
  /** Returns a unique configured filesystem implementation.
  /**
   * Returns a unique configured FileSystem implementation for the default
   * filesystem of the supplied configuration.
    * This always returns a new FileSystem object.
    * @param conf the configuration to use
   * @return the new FS instance
   * @throws IOException FS creation or initialization failure.
    */
   public static FileSystem newInstance(Configuration conf) throws IOException {
     return newInstance(getDefaultUri(conf), conf);
   }
 
   /**
   * Get a unique local file system object
   * @param conf the configuration to configure the file system with
   * @return a LocalFileSystem
   * This always returns a new FileSystem object.
   * Get a unique local FileSystem object.
   * @param conf the configuration to configure the FileSystem with
   * @return a new LocalFileSystem object.
   * @throws IOException FS creation or initialization failure.
    */
   public static LocalFileSystem newInstanceLocal(Configuration conf)
     throws IOException {
@@ -462,65 +552,70 @@ public static LocalFileSystem newInstanceLocal(Configuration conf)
   }
 
   /**
   * Close all cached filesystems. Be sure those filesystems are not
   * used anymore.
   * 
   * @throws IOException
   * Close all cached FileSystem instances. After this operation, they
   * may not be used in any operations.
   *
   * @throws IOException a problem arose closing one or more filesystem.
    */
   public static void closeAll() throws IOException {
     CACHE.closeAll();
   }
 
   /**
   * Close all cached filesystems for a given UGI. Be sure those filesystems 
   * are not used anymore.
   * Close all cached FileSystem instances for a given UGI.
   * Be sure those filesystems are not used anymore.
    * @param ugi user group info to close
   * @throws IOException
   * @throws IOException a problem arose closing one or more filesystem.
    */
  public static void closeAllForUGI(UserGroupInformation ugi) 
  public static void closeAllForUGI(UserGroupInformation ugi)
   throws IOException {
     CACHE.closeAll(ugi);
   }
 
  /** 
   * Make sure that a path specifies a FileSystem.
   * @param path to use
  /**
   * Qualify a path to one which uses this FileSystem and, if relative,
   * made absolute.
   * @param path to qualify.
   * @return this path if it contains a scheme and authority and is absolute, or
   * a new path that includes a path and authority and is fully qualified
   * @see Path#makeQualified(URI, Path)
   * @throws IllegalArgumentException if the path has a schema/URI different
   * from this FileSystem.
    */
   public Path makeQualified(Path path) {
     checkPath(path);
     return path.makeQualified(this.getUri(), this.getWorkingDirectory());
   }
    

   /**
   * Get a new delegation token for this file system.
   * Get a new delegation token for this FileSystem.
    * This is an internal method that should have been declared protected
    * but wasn't historically.
    * Callers should use {@link #addDelegationTokens(String, Credentials)}
   * 
   *
    * @param renewer the account name that is allowed to renew the token.
   * @return a new delegation token
   * @throws IOException
   * @return a new delegation token or null if the FS does not support tokens.
   * @throws IOException on any problem obtaining a token
    */
   @InterfaceAudience.Private()
   public Token<?> getDelegationToken(String renewer) throws IOException {
     return null;
   }
  

   /**
    * Obtain all delegation tokens used by this FileSystem that are not
   * already present in the given Credentials.  Existing tokens will neither
   * already present in the given Credentials. Existing tokens will neither
    * be verified as valid nor having the given renewer.  Missing tokens will
    * be acquired and added to the given Credentials.
   * 
   * Default Impl: works for simple fs with its own token
   * and also for an embedded fs whose tokens are those of its
   * children file system (i.e. the embedded fs has not tokens of its
   * own).
   * 
   *
   * Default Impl: works for simple FS with its own token
   * and also for an embedded FS whose tokens are those of its
   * child FileSystems (i.e. the embedded FS has no tokens of its own).
   *
    * @param renewer the user allowed to renew the delegation tokens
    * @param credentials cache in which to add new delegation tokens
    * @return list of new delegation tokens
   * @throws IOException
   * @throws IOException problems obtaining a token
    */
   @InterfaceAudience.LimitedPrivate({ "HDFS", "MapReduce" })
   public Token<?>[] addDelegationTokens(
@@ -528,18 +623,18 @@ public Path makeQualified(Path path) {
     if (credentials == null) {
       credentials = new Credentials();
     }
    final List<Token<?>> tokens = new ArrayList<Token<?>>();
    final List<Token<?>> tokens = new ArrayList<>();
     collectDelegationTokens(renewer, credentials, tokens);
     return tokens.toArray(new Token<?>[tokens.size()]);
   }
  

   /**
   * Recursively obtain the tokens for this FileSystem and all descended
   * FileSystems as determined by getChildFileSystems().
   * Recursively obtain the tokens for this FileSystem and all descendant
   * FileSystems as determined by {@link #getChildFileSystems()}.
    * @param renewer the user allowed to renew the delegation tokens
    * @param credentials cache in which to add the new delegation tokens
    * @param tokens list in which to add acquired tokens
   * @throws IOException
   * @throws IOException problems obtaining a token
    */
   private void collectDelegationTokens(final String renewer,
                                        final Credentials credentials,
@@ -570,30 +665,34 @@ private void collectDelegationTokens(final String renewer,
   /**
    * Get all the immediate child FileSystems embedded in this FileSystem.
    * It does not recurse and get grand children.  If a FileSystem
   * has multiple child FileSystems, then it should return a unique list
   * has multiple child FileSystems, then it must return a unique list
    * of those FileSystems.  Default is to return null to signify no children.
   * 
   * @return FileSystems used by this FileSystem
   *
   * @return FileSystems that are direct children of this FileSystem,
   *         or null for "no children"
    */
   @InterfaceAudience.LimitedPrivate({ "HDFS" })
   @VisibleForTesting
   public FileSystem[] getChildFileSystems() {
     return null;
   }
  
  /** create a file with the provided permission

  /**
   * Create a file with the provided permission.
   *
    * The permission of the file is set to be the provided permission as in
    * setPermission, not permission&~umask
   * 
   * It is implemented using two RPCs. It is understood that it is inefficient,
   *
   * The HDFS implementation is implemented using two RPCs.
   * It is understood that it is inefficient,
    * but the implementation is thread-safe. The other option is to change the
    * value of umask in configuration to be 0, but it is not thread-safe.
   * 
   * @param fs file system handle
   *
   * @param fs FileSystem
    * @param file the name of the file to be created
    * @param permission the permission of the file
    * @return an output stream
   * @throws IOException
   * @throws IOException IO failure
    */
   public static FSDataOutputStream create(FileSystem fs,
       Path file, FsPermission permission) throws IOException {
@@ -604,20 +703,21 @@ public static FSDataOutputStream create(FileSystem fs,
     return out;
   }
 
  /** create a directory with the provided permission
  /**
   * Create a directory with the provided permission.
    * The permission of the directory is set to be the provided permission as in
    * setPermission, not permission&~umask
   * 
   *
    * @see #create(FileSystem, Path, FsPermission)
   * 
   * @param fs file system handle
   *
   * @param fs FileSystem handle
    * @param dir the name of the directory to be created
    * @param permission the permission of the directory
    * @return true if the directory creation succeeds; false otherwise
   * @throws IOException
   * @throws IOException A problem creating the directories.
    */
   public static boolean mkdirs(FileSystem fs, Path dir, FsPermission permission)
  throws IOException {
      throws IOException {
     // create the directory using the default permission
     boolean result = fs.mkdirs(dir);
     // set its permission to be the supplied one
@@ -633,9 +733,16 @@ protected FileSystem() {
     super(null);
   }
 
  /** 
  /**
    * Check that a Path belongs to this FileSystem.
   *
   * The base implementation performs case insensitive equality checks
   * of the URIs' schemes and authorities. Subclasses may implement slightly
   * different checks.
    * @param path to check
   * @throws IllegalArgumentException if the path is not considered to be
   * part of this FileSystem.
   *
    */
   protected void checkPath(Path path) {
     URI uri = path.toUri();
@@ -667,25 +774,37 @@ protected void checkPath(Path path) {
           return;
       }
     }
    throw new IllegalArgumentException("Wrong FS: "+path+
                                       ", expected: "+this.getUri());
    throw new IllegalArgumentException("Wrong FS: " + path +
                                       ", expected: " + this.getUri());
   }
 
   /**
   * Return an array containing hostnames, offset and size of 
   * portions of the given file.  For a nonexistent 
   * file or regions, null will be returned.
   * Return an array containing hostnames, offset and size of
   * portions of the given file.  For nonexistent
   * file or regions, {@code null} is returned.
    *
   * This call is most helpful with DFS, where it returns 
   * hostnames of machines that contain the given file.
   * <pre>
   *   if f == null :
   *     result = null
   *   elif f.getLen() <= start:
   *     result = []
   *   else result = [ locations(FS, b) for b in blocks(FS, p, s, s+l)]
   * </pre>
   * This call is most helpful with and distributed filesystem
   * where the hostnames of machines that contain blocks of the given file
   * can be determined.
    *
   * The FileSystem will simply return an elt containing 'localhost'.
   * The default implementation returns an array containing one element:
   * <pre>
   * BlockLocation( { "localhost:50010" },  { "localhost" }, 0, file.getLen())
   * </pre>>
    *
    * @param file FilesStatus to get data from
    * @param start offset into the given file
    * @param len length for which to get locations for
   * @throws IOException IO failure
    */
  public BlockLocation[] getFileBlockLocations(FileStatus file, 
  public BlockLocation[] getFileBlockLocations(FileStatus file,
       long start, long len) throws IOException {
     if (file == null) {
       return null;
@@ -704,24 +823,28 @@ protected void checkPath(Path path) {
     return new BlockLocation[] {
       new BlockLocation(name, host, 0, file.getLen()) };
   }
 
 
   /**
   * Return an array containing hostnames, offset and size of 
   * portions of the given file.  For a nonexistent 
   * file or regions, null will be returned.
   * Return an array containing hostnames, offset and size of
   * portions of the given file.  For a nonexistent
   * file or regions, {@code null} is returned.
    *
   * This call is most helpful with DFS, where it returns 
   * hostnames of machines that contain the given file.
   * This call is most helpful with location-aware distributed
   * filesystems, where it returns hostnames of machines that
   * contain the given file.
    *
   * The FileSystem will simply return an elt containing 'localhost'.
   * A FileSystem will normally return the equivalent result
   * of passing the {@code FileStatus} of the path to
   * {@link #getFileBlockLocations(FileStatus, long, long)}
    *
    * @param p path is used to identify an FS since an FS could have
    *          another FS that it could be delegating the call to
    * @param start offset into the given file
    * @param len length for which to get locations for
   * @throws FileNotFoundException when the path does not exist
   * @throws IOException IO failure
    */
  public BlockLocation[] getFileBlockLocations(Path p, 
  public BlockLocation[] getFileBlockLocations(Path p,
       long start, long len) throws IOException {
     if (p == null) {
       throw new NullPointerException();
@@ -729,46 +852,47 @@ protected void checkPath(Path path) {
     FileStatus file = getFileStatus(p);
     return getFileBlockLocations(file, start, len);
   }
  

   /**
   * Return a set of server default configuration values
   * Return a set of server default configuration values.
    * @return server default configuration values
   * @throws IOException
   * @throws IOException IO failure
    * @deprecated use {@link #getServerDefaults(Path)} instead
    */
   @Deprecated
   public FsServerDefaults getServerDefaults() throws IOException {
    Configuration conf = getConf();
    // CRC32 is chosen as default as it is available in all 
    Configuration config = getConf();
    // CRC32 is chosen as default as it is available in all
     // releases that support checksum.
     // The client trash configuration is ignored.
    return new FsServerDefaults(getDefaultBlockSize(), 
        conf.getInt("io.bytes.per.checksum", 512), 
        64 * 1024, 
    return new FsServerDefaults(getDefaultBlockSize(),
        config.getInt("io.bytes.per.checksum", 512),
        64 * 1024,
         getDefaultReplication(),
        conf.getInt(IO_FILE_BUFFER_SIZE_KEY, IO_FILE_BUFFER_SIZE_DEFAULT),
        config.getInt(IO_FILE_BUFFER_SIZE_KEY, IO_FILE_BUFFER_SIZE_DEFAULT),
         false,
         FS_TRASH_INTERVAL_DEFAULT,
         DataChecksum.Type.CRC32);
   }
 
   /**
   * Return a set of server default configuration values
   * Return a set of server default configuration values.
    * @param p path is used to identify an FS since an FS could have
    *          another FS that it could be delegating the call to
    * @return server default configuration values
   * @throws IOException
   * @throws IOException IO failure
    */
   public FsServerDefaults getServerDefaults(Path p) throws IOException {
     return getServerDefaults();
   }
 
   /**
   * Return the fully-qualified path of path f resolving the path
   * through any symlinks or mount point
   * Return the fully-qualified path of path, resolving the path
   * through any symlinks or mount point.
    * @param p path to be resolved
   * @return fully qualified path 
   * @throws FileNotFoundException
   * @return fully qualified path
   * @throws FileNotFoundException if the path is not present
   * @throws IOException for any other error
    */
    public Path resolvePath(final Path p) throws IOException {
      checkPath(p);
@@ -779,13 +903,15 @@ public Path resolvePath(final Path p) throws IOException {
    * Opens an FSDataInputStream at the indicated Path.
    * @param f the file name to open
    * @param bufferSize the size of the buffer to be used.
   * @throws IOException IO failure
    */
   public abstract FSDataInputStream open(Path f, int bufferSize)
     throws IOException;
    

   /**
    * Opens an FSDataInputStream at the indicated Path.
    * @param f the file to open
   * @throws IOException IO failure
    */
   public FSDataInputStream open(Path f) throws IOException {
     return open(f, getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
@@ -796,6 +922,7 @@ public FSDataInputStream open(Path f) throws IOException {
    * Create an FSDataOutputStream at the indicated Path.
    * Files are overwritten by default.
    * @param f the file to create
   * @throws IOException IO failure
    */
   public FSDataOutputStream create(Path f) throws IOException {
     return create(f, true);
@@ -806,10 +933,11 @@ public FSDataOutputStream create(Path f) throws IOException {
    * @param f the file to create
    * @param overwrite if a file with this name already exists, then if true,
    *   the file will be overwritten, and if false an exception will be thrown.
   * @throws IOException IO failure
    */
   public FSDataOutputStream create(Path f, boolean overwrite)
       throws IOException {
    return create(f, overwrite, 
    return create(f, overwrite,
                   getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
                       IO_FILE_BUFFER_SIZE_DEFAULT),
                   getDefaultReplication(f),
@@ -822,10 +950,11 @@ public FSDataOutputStream create(Path f, boolean overwrite)
    * Files are overwritten by default.
    * @param f the file to create
    * @param progress to report progress
   * @throws IOException IO failure
    */
  public FSDataOutputStream create(Path f, Progressable progress) 
  public FSDataOutputStream create(Path f, Progressable progress)
       throws IOException {
    return create(f, true, 
    return create(f, true,
                   getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
                       IO_FILE_BUFFER_SIZE_DEFAULT),
                   getDefaultReplication(f),
@@ -837,10 +966,11 @@ public FSDataOutputStream create(Path f, Progressable progress)
    * Files are overwritten by default.
    * @param f the file to create
    * @param replication the replication factor
   * @throws IOException IO failure
    */
   public FSDataOutputStream create(Path f, short replication)
       throws IOException {
    return create(f, true, 
    return create(f, true,
                   getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
                       IO_FILE_BUFFER_SIZE_DEFAULT),
                   replication,
@@ -854,65 +984,70 @@ public FSDataOutputStream create(Path f, short replication)
    * @param f the file to create
    * @param replication the replication factor
    * @param progress to report progress
   * @throws IOException IO failure
    */
  public FSDataOutputStream create(Path f, short replication, 
  public FSDataOutputStream create(Path f, short replication,
       Progressable progress) throws IOException {
    return create(f, true, 
    return create(f, true,
                   getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
                       IO_FILE_BUFFER_SIZE_DEFAULT),
                   replication, getDefaultBlockSize(f), progress);
   }
 
    

   /**
    * Create an FSDataOutputStream at the indicated Path.
   * @param f the file name to create
   * @param overwrite if a file with this name already exists, then if true,
   * @param f the file to create
   * @param overwrite if a path with this name already exists, then if true,
    *   the file will be overwritten, and if false an error will be thrown.
    * @param bufferSize the size of the buffer to be used.
   * @throws IOException IO failure
    */
  public FSDataOutputStream create(Path f, 
  public FSDataOutputStream create(Path f,
                                    boolean overwrite,
                                    int bufferSize
                                    ) throws IOException {
    return create(f, overwrite, bufferSize, 
    return create(f, overwrite, bufferSize,
                   getDefaultReplication(f),
                   getDefaultBlockSize(f));
   }
    

   /**
   * Create an FSDataOutputStream at the indicated Path with write-progress
   * reporting.
   * Create an {@link FSDataOutputStream} at the indicated Path
   * with write-progress reporting.
   *
   * The frequency of callbacks is implementation-specific; it may be "none".
    * @param f the path of the file to open
    * @param overwrite if a file with this name already exists, then if true,
    *   the file will be overwritten, and if false an error will be thrown.
    * @param bufferSize the size of the buffer to be used.
   * @throws IOException IO failure
    */
  public FSDataOutputStream create(Path f, 
  public FSDataOutputStream create(Path f,
                                    boolean overwrite,
                                    int bufferSize,
                                    Progressable progress
                                    ) throws IOException {
    return create(f, overwrite, bufferSize, 
    return create(f, overwrite, bufferSize,
                   getDefaultReplication(f),
                   getDefaultBlockSize(f), progress);
   }
    
    


   /**
    * Create an FSDataOutputStream at the indicated Path.
    * @param f the file name to open
    * @param overwrite if a file with this name already exists, then if true,
    *   the file will be overwritten, and if false an error will be thrown.
    * @param bufferSize the size of the buffer to be used.
   * @param replication required block replication for the file. 
   * @param replication required block replication for the file.
   * @throws IOException IO failure
    */
  public FSDataOutputStream create(Path f, 
                                   boolean overwrite,
                                   int bufferSize,
                                   short replication,
                                   long blockSize
                                   ) throws IOException {
  public FSDataOutputStream create(Path f,
      boolean overwrite,
      int bufferSize,
      short replication,
      long blockSize) throws IOException {
     return create(f, overwrite, bufferSize, replication, blockSize, null);
   }
 
@@ -923,7 +1058,8 @@ public FSDataOutputStream create(Path f,
    * @param overwrite if a file with this name already exists, then if true,
    *   the file will be overwritten, and if false an error will be thrown.
    * @param bufferSize the size of the buffer to be used.
   * @param replication required block replication for the file. 
   * @param replication required block replication for the file.
   * @throws IOException IO failure
    */
   public FSDataOutputStream create(Path f,
                                             boolean overwrite,
@@ -948,7 +1084,7 @@ public FSDataOutputStream create(Path f,
    * @param replication required block replication for the file.
    * @param blockSize block size
    * @param progress the progress reporter
   * @throws IOException
   * @throws IOException IO failure
    * @see #setPermission(Path, FsPermission)
    */
   public abstract FSDataOutputStream create(Path f,
@@ -958,7 +1094,7 @@ public abstract FSDataOutputStream create(Path f,
       short replication,
       long blockSize,
       Progressable progress) throws IOException;
  

   /**
    * Create an FSDataOutputStream at the indicated Path with write-progress
    * reporting.
@@ -969,7 +1105,7 @@ public abstract FSDataOutputStream create(Path f,
    * @param replication required block replication for the file.
    * @param blockSize block size
    * @param progress the progress reporter
   * @throws IOException
   * @throws IOException IO failure
    * @see #setPermission(Path, FsPermission)
    */
   public FSDataOutputStream create(Path f,
@@ -982,10 +1118,10 @@ public FSDataOutputStream create(Path f,
     return create(f, permission, flags, bufferSize, replication,
         blockSize, progress, null);
   }
  

   /**
    * Create an FSDataOutputStream at the indicated Path with a custom
   * checksum option
   * checksum option.
    * @param f the file name to open
    * @param permission file permission
    * @param flags {@link CreateFlag}s to use for this stream.
@@ -995,7 +1131,7 @@ public FSDataOutputStream create(Path f,
    * @param progress the progress reporter
    * @param checksumOpt checksum parameter. If null, the values
    *        found in conf will be used.
   * @throws IOException
   * @throws IOException IO failure
    * @see #setPermission(Path, FsPermission)
    */
   public FSDataOutputStream create(Path f,
@@ -1009,27 +1145,31 @@ public FSDataOutputStream create(Path f,
     // Checksum options are ignored by default. The file systems that
     // implement checksum need to override this method. The full
     // support is currently only available in DFS.
    return create(f, permission, flags.contains(CreateFlag.OVERWRITE), 
    return create(f, permission, flags.contains(CreateFlag.OVERWRITE),
         bufferSize, replication, blockSize, progress);
   }
 
  /*.
  /**
    * This create has been added to support the FileContext that processes
   * the permission
   * with umask before calling this method.
   * the permission with umask before calling this method.
    * This a temporary method added to support the transition from FileSystem
    * to FileContext for user applications.
   * @throws IOException IO failure
    */
   @Deprecated
   protected FSDataOutputStream primitiveCreate(Path f,
     FsPermission absolutePermission, EnumSet<CreateFlag> flag, int bufferSize,
     short replication, long blockSize, Progressable progress,
     ChecksumOpt checksumOpt) throws IOException {
      FsPermission absolutePermission,
      EnumSet<CreateFlag> flag,
      int bufferSize,
      short replication,
      long blockSize,
      Progressable progress,
      ChecksumOpt checksumOpt) throws IOException {
 
     boolean pathExists = exists(f);
     CreateFlag.validate(f, pathExists, flag);
    
    // Default impl  assumes that permissions do not matter and 

    // Default impl  assumes that permissions do not matter and
     // nor does the bytesPerChecksum  hence
     // calling the regular create is good enough.
     // FSs that implement permissions should override this.
@@ -1037,25 +1177,27 @@ protected FSDataOutputStream primitiveCreate(Path f,
     if (pathExists && flag.contains(CreateFlag.APPEND)) {
       return append(f, bufferSize, progress);
     }
    

     return this.create(f, absolutePermission,
         flag.contains(CreateFlag.OVERWRITE), bufferSize, replication,
         blockSize, progress);
   }
  

   /**
    * This version of the mkdirs method assumes that the permission is absolute.
    * It has been added to support the FileContext that processes the permission
    * with umask before calling this method.
    * This a temporary method added to support the transition from FileSystem
    * to FileContext for user applications.
   * @param f path
   * @param absolutePermission permissions
   * @return true if the directory was actually created.
   * @throws IOException IO failure
   * @see #mkdirs(Path, FsPermission)
    */
   @Deprecated
   protected boolean primitiveMkdir(Path f, FsPermission absolutePermission)
     throws IOException {
    // Default impl is to assume that permissions do not matter and hence
    // calling the regular mkdirs is good enough.
    // FSs that implement permissions should override this.
    return this.mkdirs(f, absolutePermission);
   }
 
@@ -1068,10 +1210,10 @@ protected boolean primitiveMkdir(Path f, FsPermission absolutePermission)
    * to FileContext for user applications.
    */
   @Deprecated
  protected void primitiveMkdir(Path f, FsPermission absolutePermission, 
  protected void primitiveMkdir(Path f, FsPermission absolutePermission,
                     boolean createParent)
     throws IOException {
    

     if (!createParent) { // parent must exist.
       // since the this.mkdirs makes parent dirs automatically
       // we must throw exception if parent does not exist.
@@ -1103,7 +1245,7 @@ protected void primitiveMkdir(Path f, FsPermission absolutePermission,
    * @param replication required block replication for the file.
    * @param blockSize block size
    * @param progress the progress reporter
   * @throws IOException
   * @throws IOException IO failure
    * @see #setPermission(Path, FsPermission)
    */
   public FSDataOutputStream createNonRecursive(Path f,
@@ -1126,7 +1268,7 @@ public FSDataOutputStream createNonRecursive(Path f,
    * @param replication required block replication for the file.
    * @param blockSize block size
    * @param progress the progress reporter
   * @throws IOException
   * @throws IOException IO failure
    * @see #setPermission(Path, FsPermission)
    */
    public FSDataOutputStream createNonRecursive(Path f, FsPermission permission,
@@ -1149,7 +1291,7 @@ public FSDataOutputStream createNonRecursive(Path f, FsPermission permission,
     * @param replication required block replication for the file.
     * @param blockSize block size
     * @param progress the progress reporter
    * @throws IOException
    * @throws IOException IO failure
     * @see #setPermission(Path, FsPermission)
     */
     public FSDataOutputStream createNonRecursive(Path f, FsPermission permission,
@@ -1162,8 +1304,9 @@ public FSDataOutputStream createNonRecursive(Path f, FsPermission permission,
   /**
    * Creates the given Path as a brand-new zero-length file.  If
    * create fails, or if it already existed, return false.
   *
   * <i>Important: the default implementation is not atomic</i>
    * @param f path to use for create
   * @throws IOException IO failure
    */
   public boolean createNewFile(Path f) throws IOException {
     if (exists(f)) {
@@ -1177,21 +1320,27 @@ public boolean createNewFile(Path f) throws IOException {
 
   /**
    * Append to an existing file (optional operation).
   * Same as append(f, getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
   *     IO_FILE_BUFFER_SIZE_DEFAULT), null)
   * Same as
   * {@code append(f, getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
   *     IO_FILE_BUFFER_SIZE_DEFAULT), null)}
    * @param f the existing file to be appended.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default).
    */
   public FSDataOutputStream append(Path f) throws IOException {
     return append(f, getConf().getInt(IO_FILE_BUFFER_SIZE_KEY,
         IO_FILE_BUFFER_SIZE_DEFAULT), null);
   }

   /**
    * Append to an existing file (optional operation).
    * Same as append(f, bufferSize, null).
    * @param f the existing file to be appended.
    * @param bufferSize the size of the buffer to be used.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default).
    */
   public FSDataOutputStream append(Path f, int bufferSize) throws IOException {
     return append(f, bufferSize, null);
@@ -1202,7 +1351,9 @@ public FSDataOutputStream append(Path f, int bufferSize) throws IOException {
    * @param f the existing file to be appended.
    * @param bufferSize the size of the buffer to be used.
    * @param progress for reporting progress if it is not null.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default).
    */
   public abstract FSDataOutputStream append(Path f, int bufferSize,
       Progressable progress) throws IOException;
@@ -1211,34 +1362,40 @@ public abstract FSDataOutputStream append(Path f, int bufferSize,
    * Concat existing files together.
    * @param trg the path to the target destination.
    * @param psrcs the paths to the sources to use for the concatenation.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default).
    */
   public void concat(final Path trg, final Path [] psrcs) throws IOException {
    throw new UnsupportedOperationException("Not implemented by the " + 
    throw new UnsupportedOperationException("Not implemented by the " +
         getClass().getSimpleName() + " FileSystem implementation");
   }
 
  /**
   * Get replication.
   * 
   * @deprecated Use getFileStatus() instead
   * Get the replication factor.
   *
   * @deprecated Use {@link #getFileStatus(Path)} instead
    * @param src file name
    * @return file replication
   * @throws IOException
   */ 
   * @throws FileNotFoundException if the path does not resolve.
   * @throws IOException an IO failure
   */
   @Deprecated
   public short getReplication(Path src) throws IOException {
     return getFileStatus(src).getReplication();
   }
 
   /**
   * Set replication for an existing file.
   * 
   * Set the replication for an existing file.
   * If a filesystem does not support replication, it will always
   * return true: the check for a file existing may be bypassed.
   * This is the default behavior.
    * @param src file name
    * @param replication new replication
    * @throws IOException
   * @return true if successful;
   *         false if file does not exist or is a directory
   * @return true if successful, or the feature in unsupported;
   *         false if replication is supported but the file does not exist,
   *         or is a directory
    */
   public boolean setReplication(Path src, short replication)
     throws IOException {
@@ -1246,8 +1403,7 @@ public boolean setReplication(Path src, short replication)
   }
 
   /**
   * Renames Path src to Path dst.  Can take place on local fs
   * or remote DFS.
   * Renames Path src to Path dst.
    * @param src path to be renamed
    * @param dst new path after rename
    * @throws IOException on failure
@@ -1258,9 +1414,9 @@ public boolean setReplication(Path src, short replication)
   /**
    * Renames Path src to Path dst
    * <ul>
   * <li>Fails if src is a file and dst is a directory.
   * <li>Fails if src is a directory and dst is a file.
   * <li>Fails if the parent of dst does not exist or is a file.
   *   <li>Fails if src is a file and dst is a directory.</li>
   *   <li>Fails if src is a directory and dst is a file.</li>
   *   <li>Fails if the parent of dst does not exist or is a file.</li>
    * </ul>
    * <p>
    * If OVERWRITE option is not passed as an argument, rename fails
@@ -1274,12 +1430,17 @@ public boolean setReplication(Path src, short replication)
    * implementation. Please refer to the file system documentation for
    * details. This default implementation is non atomic.
    * <p>
   * This method is deprecated since it is a temporary method added to 
   * support the transition from FileSystem to FileContext for user 
   * This method is deprecated since it is a temporary method added to
   * support the transition from FileSystem to FileContext for user
    * applications.
   * 
   *
    * @param src path to be renamed
    * @param dst new path after rename
   * @throws FileNotFoundException src path does not exist, or the parent
   * path of dst does not exist.
   * @throws FileAlreadyExistsException dest path exists and is a file
   * @throws ParentNotDirectoryException if the parent path of dest is not
   * a directory
    * @throws IOException on failure
    */
   @Deprecated
@@ -1344,10 +1505,10 @@ protected void rename(final Path src, final Path dst,
   /**
    * Truncate the file in the indicated path to the indicated size.
    * <ul>
   * <li>Fails if path is a directory.
   * <li>Fails if path does not exist.
   * <li>Fails if path is not closed.
   * <li>Fails if new size is greater than current size.
   *   <li>Fails if path is a directory.</li>
   *   <li>Fails if path does not exist.</li>
   *   <li>Fails if path is not closed.</li>
   *   <li>Fails if new size is greater than current size.</li>
    * </ul>
    * @param f The path to the file to be truncated
    * @param newLength The size the file is to be truncated to
@@ -1358,44 +1519,61 @@ protected void rename(final Path src, final Path dst,
    * <code>false</code> if a background process of adjusting the length of
    * the last block has been started, and clients should wait for it to
    * complete before proceeding with further file updates.
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default).
    */
   public boolean truncate(Path f, long newLength) throws IOException {
     throw new UnsupportedOperationException("Not implemented by the " +
         getClass().getSimpleName() + " FileSystem implementation");
   }
  

   /**
   * Delete a file 
   * Delete a file/directory.
    * @deprecated Use {@link #delete(Path, boolean)} instead.
    */
   @Deprecated
   public boolean delete(Path f) throws IOException {
     return delete(f, true);
   }
  

   /** Delete a file.
    *
    * @param f the path to delete.
   * @param recursive if path is a directory and set to 
   * @param recursive if path is a directory and set to
    * true, the directory is deleted else throws an exception. In
   * case of a file the recursive can be set to either true or false. 
   * @return  true if delete is successful else false. 
   * @throws IOException
   * case of a file the recursive can be set to either true or false.
   * @return  true if delete is successful else false.
   * @throws IOException IO failure
    */
   public abstract boolean delete(Path f, boolean recursive) throws IOException;
 
   /**
   * Mark a path to be deleted when FileSystem is closed.
   * When the JVM shuts down,
   * all FileSystem objects will be closed automatically.
   * Then,
   * the marked path will be deleted as a result of closing the FileSystem.
   * Mark a path to be deleted when its FileSystem is closed.
   * When the JVM shuts down cleanly, all cached FileSystem objects will be
   * closed automatically —these the marked paths will be deleted as a result.
   *
   * If a FileSystem instance is not cached, i.e. has been created with
   * {@link #createFileSystem(URI, Configuration)}, then the paths will
   * be deleted in when {@link #close()} is called on that instance.
    *
   * The path has to exist in the file system.
   * 
   * The path must exist in the filesystem at the time of the method call;
   * it does not have to exist at the time of JVM shutdown.
   *
   * Notes
   * <ol>
   *   <li>Clean shutdown of the JVM cannot be guaranteed.</li>
   *   <li>The time to shut down a FileSystem will depends on the number of
   *   files to delete. For filesystems where the cost of checking
   *   for the existence of a file/directory and the actual delete operation
   *   (for example: object stores) is high, the time to shutdown the JVM can be
   *   significantly extended by over-use of this feature.</li>
   *   <li>Connectivity problems with a remote filesystem may delay shutdown
   *   further, and may cause the files to not be deleted.</li>
   * </ol>
    * @param f the path to delete.
    * @return  true if deleteOnExit is successful, otherwise false.
   * @throws IOException
   * @throws IOException IO failure
    */
   public boolean deleteOnExit(Path f) throws IOException {
     if (!exists(f)) {
@@ -1406,10 +1584,11 @@ public boolean deleteOnExit(Path f) throws IOException {
     }
     return true;
   }
  

   /**
   * Cancel the deletion of the path when the FileSystem is closed
   * Cancel the scheduled deletion of the path when the FileSystem is closed.
    * @param f the path to cancel deletion
   * @return true if the path was found in the delete-on-exit list.
    */
   public boolean cancelDeleteOnExit(Path f) {
     synchronized (deleteOnExit) {
@@ -1418,8 +1597,12 @@ public boolean cancelDeleteOnExit(Path f) {
   }
 
   /**
   * Delete all files that were marked as delete-on-exit. This recursively
   * deletes all files in the specified paths.
   * Delete all paths that were marked as delete-on-exit. This recursively
   * deletes all files and directories in the specified paths.
   *
   * The time to process this operation is {@code O(paths)}, with the actual
   * time dependent on the time for existence and deletion operations to
   * complete, successfully or not.
    */
   protected void processDeleteOnExit() {
     synchronized (deleteOnExit) {
@@ -1431,15 +1614,17 @@ protected void processDeleteOnExit() {
           }
         }
         catch (IOException e) {
          LOG.info("Ignoring failure to deleteOnExit for path " + path);
          LOGGER.info("Ignoring failure to deleteOnExit for path {}", path);
         }
         iter.remove();
       }
     }
   }
  
  /** Check if exists.
   * @param f source file

  /** Check if a path exists.
   * @param f source path
   * @return true if the path exists
   * @throws IOException IO failure
    */
   public boolean exists(Path f) throws IOException {
     try {
@@ -1450,9 +1635,10 @@ public boolean exists(Path f) throws IOException {
   }
 
   /** True iff the named path is a directory.
   * Note: Avoid using this method. Instead reuse the FileStatus 
   * Note: Avoid using this method. Instead reuse the FileStatus
    * returned by getFileStatus() or listStatus() methods.
    * @param f path to check
   * @throws IOException IO failure
    */
   public boolean isDirectory(Path f) throws IOException {
     try {
@@ -1463,9 +1649,10 @@ public boolean isDirectory(Path f) throws IOException {
   }
 
   /** True iff the named path is a regular file.
   * Note: Avoid using this method. Instead reuse the FileStatus 
   * returned by getFileStatus() or listStatus() methods.
   * Note: Avoid using this method. Instead reuse the FileStatus
   * returned by {@link #getFileStatus(Path)} or listStatus() methods.
    * @param f path to check
   * @throws IOException IO failure
    */
   public boolean isFile(Path f) throws IOException {
     try {
@@ -1474,17 +1661,24 @@ public boolean isFile(Path f) throws IOException {
       return false;               // f does not exist
     }
   }
  
  /** The number of bytes in a file. */
  /** @deprecated Use getFileStatus() instead */

  /**
   * The number of bytes in a file.
   * @return the number of bytes; 0 for a directory
   * @deprecated Use {@link #getFileStatus(Path)} instead.
   * @throws FileNotFoundException if the path does not resolve
   * @throws IOException IO failure
   */
   @Deprecated
   public long getLength(Path f) throws IOException {
     return getFileStatus(f).getLen();
   }
    

   /** Return the {@link ContentSummary} of a given {@link Path}.
  * @param f path to use
  */
   * @param f path to use
   * @throws FileNotFoundException if the path does not resolve
   * @throws IOException IO failure
   */
   public ContentSummary getContentSummary(Path f) throws IOException {
     FileStatus status = getFileStatus(f);
     if (status.isFile()) {
@@ -1511,18 +1705,23 @@ public ContentSummary getContentSummary(Path f) throws IOException {
 
   /** Return the {@link QuotaUsage} of a given {@link Path}.
    * @param f path to use
   * @return the quota usage
   * @throws IOException IO failure
    */
   public QuotaUsage getQuotaUsage(Path f) throws IOException {
     return getContentSummary(f);
   }
 
  final private static PathFilter DEFAULT_FILTER = new PathFilter() {
    @Override
    public boolean accept(Path file) {
      return true;
    }
  };
    
  /**
   * The default filter accepts all paths.
   */
  private static final PathFilter DEFAULT_FILTER = new PathFilter() {
      @Override
      public boolean accept(Path file) {
        return true;
      }
    };

   /**
    * List the statuses of the files/directories in the given path if the path is
    * a directory.
@@ -1536,7 +1735,8 @@ public boolean accept(Path file) {
    * @throws FileNotFoundException when the path does not exist
    * @throws IOException see specific implementation
    */
  public abstract FileStatus[] listStatus(Path f) throws IOException;
  public abstract FileStatus[] listStatus(Path f) throws FileNotFoundException,
                                                         IOException;
 
   /**
    * Represents a batch of directory entries when iteratively listing a
@@ -1599,9 +1799,11 @@ protected DirectoryEntries listStatusBatch(Path f, byte[] token) throws
     return new DirectoryEntries(listing, null, false);
   }
 
  /*
  /**
    * Filter files/directories in the given path using the user-supplied path
    * filter. Results are added to the given array <code>results</code>.
   * @throws FileNotFoundException when the path does not exist
   * @throws IOException see specific implementation
    */
   private void listStatus(ArrayList<FileStatus> results, Path f,
       PathFilter filter) throws FileNotFoundException, IOException {
@@ -1615,15 +1817,17 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
   }
 
   /**
   * List corrupted file blocks.
    * @return an iterator over the corrupt files under the given path
    * (may contain duplicates if a file has more than one corrupt block)
   * @throws IOException
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default).
   * @throws IOException IO failure
    */
   public RemoteIterator<Path> listCorruptFileBlocks(Path path)
     throws IOException {
     throw new UnsupportedOperationException(getClass().getCanonicalName() +
                                            " does not support" +
                                            " listCorruptFileBlocks");
        " does not support listCorruptFileBlocks");
   }
 
   /**
@@ -1632,19 +1836,19 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
    * <p>
    * Does not guarantee to return the List of files/directories status in a
    * sorted order.
   * 
   *
    * @param f
    *          a path name
    * @param filter
    *          the user-supplied path filter
    * @return an array of FileStatus objects for the files under the given path
    *         after applying the filter
   * @throws FileNotFoundException when the path does not exist;
   *         IOException see specific implementation   
   * @throws FileNotFoundException when the path does not exist
   * @throws IOException see specific implementation
    */
  public FileStatus[] listStatus(Path f, PathFilter filter) 
  public FileStatus[] listStatus(Path f, PathFilter filter)
                                    throws FileNotFoundException, IOException {
    ArrayList<FileStatus> results = new ArrayList<FileStatus>();
    ArrayList<FileStatus> results = new ArrayList<>();
     listStatus(results, f, filter);
     return results.toArray(new FileStatus[results.size()]);
   }
@@ -1655,13 +1859,13 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
    * <p>
    * Does not guarantee to return the List of files/directories status in a
    * sorted order.
   * 
   *
    * @param files
    *          a list of paths
    * @return a list of statuses for the files under the given paths after
    *         applying the filter default Path filter
   * @throws FileNotFoundException when the path does not exist;
   *         IOException see specific implementation
   * @throws FileNotFoundException when the path does not exist
   * @throws IOException see specific implementation
    */
   public FileStatus[] listStatus(Path[] files)
       throws FileNotFoundException, IOException {
@@ -1674,15 +1878,15 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
    * <p>
    * Does not guarantee to return the List of files/directories status in a
    * sorted order.
   * 
   *
    * @param files
    *          a list of paths
    * @param filter
    *          the user-supplied path filter
    * @return a list of statuses for the files under the given paths after
    *         applying the filter
   * @throws FileNotFoundException when the path does not exist;
   *         IOException see specific implementation
   * @throws FileNotFoundException when the path does not exist
   * @throws IOException see specific implementation
    */
   public FileStatus[] listStatus(Path[] files, PathFilter filter)
       throws FileNotFoundException, IOException {
@@ -1696,7 +1900,7 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
   /**
    * <p>Return all the files that match filePattern and are not checksum
    * files. Results are sorted by their names.
   * 
   *
    * <p>
    * A filename pattern is composed of <i>regular</i> characters and
    * <i>special pattern matching</i> characters, which are:
@@ -1736,7 +1940,7 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
    *    <p>
    *    <dt> <tt> {ab,cd} </tt>
    *    <dd> Matches a string from the string set <tt>{<i>ab, cd</i>} </tt>
   *    
   *
    *    <p>
    *    <dt> <tt> {ab,c{de,fh}} </tt>
    *    <dd> Matches a string from the string set <tt>{<i>ab, cde, cfh</i>}</tt>
@@ -1748,17 +1952,17 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
    * @param pathPattern a regular expression specifying a pth pattern
 
    * @return an array of paths that match the path pattern
   * @throws IOException
   * @throws IOException IO failure
    */
   public FileStatus[] globStatus(Path pathPattern) throws IOException {
     return new Globber(this, pathPattern, DEFAULT_FILTER).glob();
   }
  

   /**
   * Return an array of FileStatus objects whose path names match
   * Return an array of {@link FileStatus} objects whose path names match
    * {@code pathPattern} and is accepted by the user-supplied path filter.
    * Results are sorted by their path names.
   * 
   *
    * @param pathPattern a regular expression specifying the path pattern
    * @param filter a user-supplied path filter
    * @return null if {@code pathPattern} has no glob and the path does not exist
@@ -1771,17 +1975,17 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
       throws IOException {
     return new Globber(this, pathPattern, filter).glob();
   }
  

   /**
    * List the statuses of the files/directories in the given path if the path is
   * a directory. 
   * a directory.
    * Return the file's status and block locations If the path is a file.
   * 
   *
    * If a returned status is a file, it contains the file's block locations.
   * 
   *
    * @param f is the path
    *
   * @return an iterator that traverses statuses of the files/directories 
   * @return an iterator that traverses statuses of the files/directories
    *         in the given path
    *
    * @throws FileNotFoundException If <code>f</code> does not exist
@@ -1793,12 +1997,12 @@ private void listStatus(ArrayList<FileStatus> results, Path f,
   }
 
   /**
   * Listing a directory
   * List a directory.
    * The returned results include its block location if it is a file
    * The results are filtered by the given path filter
    * @param f a path
    * @param filter a path filter
   * @return an iterator that traverses statuses of the files/directories 
   * @return an iterator that traverses statuses of the files/directories
    *         in the given path
    * @throws FileNotFoundException if <code>f</code> does not exist
    * @throws IOException if any I/O error occurred
@@ -1876,14 +2080,17 @@ public T next() throws IOException {
 
   /**
    * Returns a remote iterator so that followup calls are made on demand
   * while consuming the entries. Each file system implementation should
   * while consuming the entries. Each FileSystem implementation should
    * override this method and provide a more efficient implementation, if
   * possible. 
   * possible.
   *
    * Does not guarantee to return the iterator that traverses statuses
    * of the files in a sorted order.
    *
    * @param p target path
    * @return remote iterator
   * @throws FileNotFoundException if <code>p</code> does not exist
   * @throws IOException if any I/O error occurred
    */
   public RemoteIterator<FileStatus> listStatusIterator(final Path p)
   throws FileNotFoundException, IOException {
@@ -1894,30 +2101,29 @@ public T next() throws IOException {
    * List the statuses and block locations of the files in the given path.
    * Does not guarantee to return the iterator that traverses statuses
    * of the files in a sorted order.
   * 
   * If the path is a directory, 
   * <pre>
   * If the path is a directory,
    *   if recursive is false, returns files in the directory;
    *   if recursive is true, return files in the subtree rooted at the path.
    * If the path is a file, return the file's status and block locations.
   * 
   * </pre>
    * @param f is the path
    * @param recursive if the subdirectories need to be traversed recursively
    *
    * @return an iterator that traverses statuses of the files
    *
    * @throws FileNotFoundException when the path does not exist;
   *         IOException see specific implementation
   * @throws IOException see specific implementation
    */
   public RemoteIterator<LocatedFileStatus> listFiles(
       final Path f, final boolean recursive)
   throws FileNotFoundException, IOException {
     return new RemoteIterator<LocatedFileStatus>() {
      private Stack<RemoteIterator<LocatedFileStatus>> itors = 
        new Stack<RemoteIterator<LocatedFileStatus>>();
      private Stack<RemoteIterator<LocatedFileStatus>> itors = new Stack<>();
       private RemoteIterator<LocatedFileStatus> curItor =
         listLocatedStatus(f);
       private LocatedFileStatus curFile;
     

       @Override
       public boolean hasNext() throws IOException {
         while (curFile == null) {
@@ -1955,14 +2161,14 @@ public LocatedFileStatus next() throws IOException {
           LocatedFileStatus result = curFile;
           curFile = null;
           return result;
        } 
        }
         throw new java.util.NoSuchElementException("No more entry in " + f);
       }
     };
   }
  
  /** Return the current user's home directory in this filesystem.
   * The default implementation returns "/user/$USER/".

  /** Return the current user's home directory in this FileSystem.
   * The default implementation returns {@code "/user/$USER/"}.
    */
   public Path getHomeDirectory() {
     return this.makeQualified(
@@ -1971,29 +2177,28 @@ public Path getHomeDirectory() {
 
 
   /**
   * Set the current working directory for the given file system. All relative
   * Set the current working directory for the given FileSystem. All relative
    * paths will be resolved relative to it.
   * 
   *
    * @param new_dir Path of new working directory
    */
   public abstract void setWorkingDirectory(Path new_dir);
    

   /**
   * Get the current working directory for the given file system
   * Get the current working directory for the given FileSystem
    * @return the directory pathname
    */
   public abstract Path getWorkingDirectory();
  
  
  /**
   * Note: with the new FilesContext class, getWorkingDirectory()
   * will be removed. 
   * The working directory is implemented in FilesContext.
   * 
   * Some file systems like LocalFileSystem have an initial workingDir

  /**
   * Note: with the new FileContext class, getWorkingDirectory()
   * will be removed.
   * The working directory is implemented in FileContext.
   *
   * Some FileSystems like LocalFileSystem have an initial workingDir
    * that we use as the starting workingDir. For other file systems
    * like HDFS there is no built in notion of an initial workingDir.
   * 
   *
    * @return if there is built in notion of workingDir then it
    * is returned; else a null is returned.
    */
@@ -2003,6 +2208,9 @@ protected Path getInitialWorkingDirectory() {
 
   /**
    * Call {@link #mkdirs(Path, FsPermission)} with default permission.
   * @param f path
   * @return true if the directory was created
   * @throws IOException IO failure
    */
   public boolean mkdirs(Path f) throws IOException {
     return mkdirs(f, FsPermission.getDirDefault());
@@ -2010,19 +2218,21 @@ public boolean mkdirs(Path f) throws IOException {
 
   /**
    * Make the given file and all non-existent parents into
   * directories. Has the semantics of Unix 'mkdir -p'.
   * directories. Has roughly the semantics of Unix @{code mkdir -p}.
    * Existence of the directory hierarchy is not an error.
    * @param f path to create
    * @param permission to apply to f
   * @throws IOException IO failure
    */
   public abstract boolean mkdirs(Path f, FsPermission permission
       ) throws IOException;
 
   /**
   * The src file is on the local disk.  Add it to FS at
   * The src file is on the local disk.  Add it to filesystem at
    * the given dst name and the source is kept intact afterwards
    * @param src path
    * @param dst path
   * @throws IOException IO failure
    */
   public void copyFromLocalFile(Path src, Path dst)
     throws IOException {
@@ -2030,10 +2240,11 @@ public void copyFromLocalFile(Path src, Path dst)
   }
 
   /**
   * The src files is on the local disk.  Add it to FS at
   * The src files is on the local disk.  Add it to filesystem at
    * the given dst name, removing the source afterwards.
   * @param srcs path
   * @param srcs source paths
    * @param dst path
   * @throws IOException IO failure
    */
   public void moveFromLocalFile(Path[] srcs, Path dst)
     throws IOException {
@@ -2041,10 +2252,11 @@ public void moveFromLocalFile(Path[] srcs, Path dst)
   }
 
   /**
   * The src file is on the local disk.  Add it to FS at
   * The src file is on the local disk.  Add it to the filesystem at
    * the given dst name, removing the source afterwards.
   * @param src path
   * @param src local path
    * @param dst path
   * @throws IOException IO failure
    */
   public void moveFromLocalFile(Path src, Path dst)
     throws IOException {
@@ -2052,7 +2264,7 @@ public void moveFromLocalFile(Path src, Path dst)
   }
 
   /**
   * The src file is on the local disk.  Add it to FS at
   * The src file is on the local disk.  Add it to the filesystem at
    * the given dst name.
    * delSrc indicates if the source should be removed
    * @param delSrc whether to delete the src
@@ -2063,80 +2275,83 @@ public void copyFromLocalFile(boolean delSrc, Path src, Path dst)
     throws IOException {
     copyFromLocalFile(delSrc, true, src, dst);
   }
  

   /**
   * The src files are on the local disk.  Add it to FS at
   * The src files are on the local disk.  Add it to the filesystem at
    * the given dst name.
    * delSrc indicates if the source should be removed
    * @param delSrc whether to delete the src
    * @param overwrite whether to overwrite an existing file
    * @param srcs array of paths which are source
    * @param dst path
   * @throws IOException IO failure
    */
  public void copyFromLocalFile(boolean delSrc, boolean overwrite, 
  public void copyFromLocalFile(boolean delSrc, boolean overwrite,
                                 Path[] srcs, Path dst)
     throws IOException {
     Configuration conf = getConf();
     FileUtil.copy(getLocal(conf), srcs, this, dst, delSrc, overwrite, conf);
   }
  

   /**
   * The src file is on the local disk.  Add it to FS at
   * The src file is on the local disk.  Add it to the filesystem at
    * the given dst name.
    * delSrc indicates if the source should be removed
    * @param delSrc whether to delete the src
    * @param overwrite whether to overwrite an existing file
    * @param src path
    * @param dst path
   * @throws IOException IO failure
    */
  public void copyFromLocalFile(boolean delSrc, boolean overwrite, 
  public void copyFromLocalFile(boolean delSrc, boolean overwrite,
                                 Path src, Path dst)
     throws IOException {
     Configuration conf = getConf();
     FileUtil.copy(getLocal(conf), src, this, dst, delSrc, overwrite, conf);
   }
    

   /**
   * The src file is under FS, and the dst is on the local disk.
   * Copy it from FS control to the local dst name.
   * @param src path
   * @param dst path
   * Copy it a file from the remote filesystem to the local one.
   * @param src path src file in the remote filesystem
   * @param dst path local destination
   * @throws IOException IO failure
    */
   public void copyToLocalFile(Path src, Path dst) throws IOException {
     copyToLocalFile(false, src, dst);
   }
    

   /**
   * The src file is under FS, and the dst is on the local disk.
   * Copy it from FS control to the local dst name.
   * Remove the source afterwards
   * @param src path
   * @param dst path
   * Copy a file to the local filesystem, then delete it from the
   * remote filesystem (if successfully copied).
   * @param src path src file in the remote filesystem
   * @param dst path local destination
   * @throws IOException IO failure
    */
   public void moveToLocalFile(Path src, Path dst) throws IOException {
     copyToLocalFile(true, src, dst);
   }
 
   /**
   * The src file is under FS, and the dst is on the local disk.
   * Copy it from FS control to the local dst name.
   * Copy it a file from a remote filesystem to the local one.
    * delSrc indicates if the src will be removed or not.
    * @param delSrc whether to delete the src
   * @param src path
   * @param dst path
   */   
   * @param src path src file in the remote filesystem
   * @param dst path local destination
   * @throws IOException IO failure
   */
   public void copyToLocalFile(boolean delSrc, Path src, Path dst)
     throws IOException {
     copyToLocalFile(delSrc, src, dst, false);
   }
  
    /**
   * The src file is under FS, and the dst is on the local disk. Copy it from FS
   * control to the local dst name. delSrc indicates if the src will be removed

  /**
   * The src file is under this filesystem, and the dst is on the local disk.
   * Copy it from the remote filesystem to the local dst name.
   * delSrc indicates if the src will be removed
    * or not. useRawLocalFileSystem indicates whether to use RawLocalFileSystem
   * as local file system or not. RawLocalFileSystem is non crc file system.So,
   * It will not create any crc files at local.
   * 
   * as the local file system or not. RawLocalFileSystem is non checksumming,
   * So, It will not create any crc files at local.
   *
    * @param delSrc
    *          whether to delete the src
    * @param src
@@ -2145,9 +2360,8 @@ public void copyToLocalFile(boolean delSrc, Path src, Path dst)
    *          path
    * @param useRawLocalFileSystem
    *          whether to use RawLocalFileSystem as local file system or not.
   * 
   * @throws IOException
   *           - if any IO error
   *
   * @throws IOException for any IO error
    */
   public void copyToLocalFile(boolean delSrc, Path src, Path dst,
       boolean useRawLocalFileSystem) throws IOException {
@@ -2162,12 +2376,14 @@ public void copyToLocalFile(boolean delSrc, Path src, Path dst,
   }
 
   /**
   * Returns a local File that the user can write output to.  The caller
   * provides both the eventual FS target name and the local working
   * file.  If the FS is local, we write directly into the target.  If
   * the FS is remote, we write into the tmp local area.
   * Returns a local file that the user can write output to.  The caller
   * provides both the eventual target name in this FileSystem
   * and the local working file path.
   * If this FileSystem is local, we write directly into the target.  If
   * the FileSystem is not local, we write into the tmp local area.
    * @param fsOutputFile path of output file
    * @param tmpLocalFile path of local tmp file
   * @throws IOException IO failure
    */
   public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile)
     throws IOException {
@@ -2175,12 +2391,14 @@ public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile)
   }
 
   /**
   * Called when we're all done writing to the target.  A local FS will
   * do nothing, because we've written to exactly the right place.  A remote
   * FS will copy the contents of tmpLocalFile to the correct target at
   * Called when we're all done writing to the target.
   * A local FS will do nothing, because we've written to exactly the
   * right place.
   * A remote FS will copy the contents of tmpLocalFile to the correct target at
    * fsOutputFile.
    * @param fsOutputFile path of output file
    * @param tmpLocalFile path to local tmp file
   * @throws IOException IO failure
    */
   public void completeLocalOutput(Path fsOutputFile, Path tmpLocalFile)
     throws IOException {
@@ -2188,8 +2406,14 @@ public void completeLocalOutput(Path fsOutputFile, Path tmpLocalFile)
   }
 
   /**
   * No more filesystem operations are needed.  Will
   * release any held locks.
   * Close this FileSystem instance.
   * Will release any held locks, delete all files queued for deletion
   * through calls to {@link #deleteOnExit(Path)}, and remove this FS instance
   * from the cache, if cached.
   *
   * After this operation, the outcome of any method call on this FileSystem
   * instance, or any input/output stream created by it is <i>undefined</i>.
   * @throws IOException IO failure
    */
   @Override
   public void close() throws IOException {
@@ -2198,13 +2422,19 @@ public void close() throws IOException {
     CACHE.remove(this.key, this);
   }
 
  /** Return the total size of all files in the filesystem. */
  /**
   * Return the total size of all files in the filesystem.
   * @throws IOException IO failure
   */
   public long getUsed() throws IOException {
     Path path = new Path("/");
     return getUsed(path);
   }
 
  /** Return the total size of all files from a specified path. */
  /**
   * Return the total size of all files from a specified path.
   * @throws IOException IO failure
   */
   public long getUsed(Path path) throws IOException {
     return getContentSummary(path).getLength();
   }
@@ -2213,8 +2443,10 @@ public long getUsed(Path path) throws IOException {
    * Get the block size for a particular file.
    * @param f the filename
    * @return the number of bytes in a block
   * @deprecated Use {@link #getFileStatus(Path)} instead
   * @throws FileNotFoundException if the path is not present
   * @throws IOException IO failure
    */
  /** @deprecated Use getFileStatus() instead */
   @Deprecated
   public long getBlockSize(Path f) throws IOException {
     return getFileStatus(f).getBlockSize();
@@ -2222,7 +2454,7 @@ public long getBlockSize(Path f) throws IOException {
 
   /**
    * Return the number of bytes that large input files should be optimally
   * be split into to minimize i/o time.
   * be split into to minimize I/O time.
    * @deprecated use {@link #getDefaultBlockSize(Path)} instead
    */
   @Deprecated
@@ -2230,9 +2462,10 @@ public long getDefaultBlockSize() {
     // default to 32MB: large enough to minimize the impact of seeks
     return getConf().getLong("fs.local.block.size", 32 * 1024 * 1024);
   }
    
  /** Return the number of bytes that large input files should be optimally
   * be split into to minimize i/o time.  The given path will be used to

  /**
   * Return the number of bytes that large input files should be optimally
   * be split into to minimize I/O time.  The given path will be used to
    * locate the actual filesystem.  The full path does not have to exist.
    * @param f path of file
    * @return the default block size for the path's filesystem
@@ -2243,27 +2476,29 @@ public long getDefaultBlockSize(Path f) {
 
   /**
    * Get the default replication.
   * @return the replication; the default value is "1".
    * @deprecated use {@link #getDefaultReplication(Path)} instead
    */
   @Deprecated
   public short getDefaultReplication() { return 1; }
 
   /**
   * Get the default replication for a path.   The given path will be used to
   * locate the actual filesystem.  The full path does not have to exist.
   * Get the default replication for a path.
   * The given path will be used to locate the actual FileSystem to query.
   * The full path does not have to exist.
    * @param path of the file
   * @return default replication for the path's filesystem 
   * @return default replication for the path's filesystem
    */
   public short getDefaultReplication(Path path) {
     return getDefaultReplication();
   }
  

   /**
    * Return a file status object that represents the path.
    * @param f The path we want information from
    * @return a FileStatus object
   * @throws FileNotFoundException when the path does not exist;
   *         IOException see specific implementation
   * @throws FileNotFoundException when the path does not exist
   * @throws IOException see specific implementation
    */
   public abstract FileStatus getFileStatus(Path f) throws IOException;
 
@@ -2272,10 +2507,12 @@ public short getDefaultReplication(Path path) {
    * checks to perform.  If the requested permissions are granted, then the
    * method returns normally.  If access is denied, then the method throws an
    * {@link AccessControlException}.
   * <p/>
   * The default implementation of this method calls {@link #getFileStatus(Path)}
   * <p>
   * The default implementation calls {@link #getFileStatus(Path)}
    * and checks the returned permissions against the requested permissions.
   * Note that the getFileStatus call will be subject to authorization checks.
   *
   * Note that the {@link #getFileStatus(Path)} call will be subject to
   * authorization checks.
    * Typically, this requires search (execute) permissions on each directory in
    * the path's prefix, but this is implementation-defined.  Any file system
    * that provides a richer authorization model (such as ACLs) may override the
@@ -2305,11 +2542,12 @@ public void access(Path path, FsAction mode) throws AccessControlException,
    *
    * @param stat FileStatus to check
    * @param mode type of access to check
   * @throws AccessControlException if access is denied
    * @throws IOException for any error
    */
   @InterfaceAudience.Private
   static void checkAccessPermissions(FileStatus stat, FsAction mode)
      throws IOException {
      throws AccessControlException, IOException {
     FsPermission perm = stat.getPermission();
     UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
     String user = ugi.getShortUserName();
@@ -2332,7 +2570,7 @@ static void checkAccessPermissions(FileStatus stat, FsAction mode)
   }
 
   /**
   * See {@link FileContext#fixRelativePart}
   * See {@link FileContext#fixRelativePart}.
    */
   protected Path fixRelativePart(Path p) {
     if (p.isUriPathAbsolute()) {
@@ -2343,12 +2581,12 @@ protected Path fixRelativePart(Path p) {
   }
 
   /**
   * See {@link FileContext#createSymlink(Path, Path, boolean)}
   * See {@link FileContext#createSymlink(Path, Path, boolean)}.
    */
   public void createSymlink(final Path target, final Path link,
       final boolean createParent) throws AccessControlException,
       FileAlreadyExistsException, FileNotFoundException,
      ParentNotDirectoryException, UnsupportedFileSystemException, 
      ParentNotDirectoryException, UnsupportedFileSystemException,
       IOException {
     // Supporting filesystems should override this method
     throw new UnsupportedOperationException(
@@ -2356,7 +2594,9 @@ public void createSymlink(final Path target, final Path link,
   }
 
   /**
   * See {@link FileContext#getFileLinkStatus(Path)}
   * See {@link FileContext#getFileLinkStatus(Path)}.
   * @throws FileNotFoundException when the path does not exist
   * @throws IOException see specific implementation
    */
   public FileStatus getFileLinkStatus(final Path f)
       throws AccessControlException, FileNotFoundException,
@@ -2366,14 +2606,16 @@ public FileStatus getFileLinkStatus(final Path f)
   }
 
   /**
   * See {@link AbstractFileSystem#supportsSymlinks()}
   * See {@link AbstractFileSystem#supportsSymlinks()}.
    */
   public boolean supportsSymlinks() {
     return false;
   }
 
   /**
   * See {@link FileContext#getLinkTarget(Path)}
   * See {@link FileContext#getLinkTarget(Path)}.
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public Path getLinkTarget(Path f) throws IOException {
     // Supporting filesystems should override this method
@@ -2382,7 +2624,9 @@ public Path getLinkTarget(Path f) throws IOException {
   }
 
   /**
   * See {@link AbstractFileSystem#getLinkTarget(Path)}
   * See {@link AbstractFileSystem#getLinkTarget(Path)}.
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   protected Path resolveLink(Path f) throws IOException {
     // Supporting filesystems should override this method
@@ -2391,12 +2635,13 @@ protected Path resolveLink(Path f) throws IOException {
   }
 
   /**
   * Get the checksum of a file.
   * Get the checksum of a file, if the FS supports checksums.
    *
    * @param f The file path
    * @return The file checksum.  The default return value is null,
    *  which indicates that no checksum algorithm is implemented
    *  in the corresponding FileSystem.
   * @throws IOException IO failure
    */
   public FileChecksum getFileChecksum(Path f) throws IOException {
     return getFileChecksum(f, Long.MAX_VALUE);
@@ -2407,7 +2652,8 @@ public FileChecksum getFileChecksum(Path f) throws IOException {
    * specific length.
    * @param f The file path
    * @param length The length of the file range for checksum calculation
   * @return The file checksum.
   * @return The file checksum or null if checksums are not supported.
   * @throws IOException IO failure
    */
   public FileChecksum getFileChecksum(Path f, final long length)
       throws IOException {
@@ -2415,8 +2661,9 @@ public FileChecksum getFileChecksum(Path f, final long length)
   }
 
   /**
   * Set the verify checksum flag. This is only applicable if the 
   * corresponding FileSystem supports checksum. By default doesn't do anything.
   * Set the verify checksum flag. This is only applicable if the
   * corresponding filesystem supports checksums.
   * By default doesn't do anything.
    * @param verifyChecksum Verify checksum flag
    */
   public void setVerifyChecksum(boolean verifyChecksum) {
@@ -2424,9 +2671,10 @@ public void setVerifyChecksum(boolean verifyChecksum) {
   }
 
   /**
   * Set the write checksum flag. This is only applicable if the 
   * corresponding FileSystem supports checksum. By default doesn't do anything.
   * @param writeChecksum Write checsum flag
   * Set the write checksum flag. This is only applicable if the
   * corresponding filesystem supports checksums.
   * By default doesn't do anything.
   * @param writeChecksum Write checksum flag
    */
   public void setWriteChecksum(boolean writeChecksum) {
     //doesn't do anything
@@ -2434,9 +2682,9 @@ public void setWriteChecksum(boolean writeChecksum) {
 
   /**
    * Returns a status object describing the use and capacity of the
   * file system. If the file system has multiple partitions, the
   * filesystem. If the filesystem has multiple partitions, the
    * use and capacity of the root partition is reflected.
   * 
   *
    * @return a FsStatus object
    * @throws IOException
    *           see specific implementation
@@ -2447,11 +2695,11 @@ public FsStatus getStatus() throws IOException {
 
   /**
    * Returns a status object describing the use and capacity of the
   * file system. If the file system has multiple partitions, the
   * filesystem. If the filesystem has multiple partitions, the
    * use and capacity of the partition pointed to by the specified
    * path is reflected.
    * @param p Path for which status should be obtained. null means
   * the default partition. 
   * the default partition.
    * @return a FsStatus object
    * @throws IOException
    *           see specific implementation
@@ -2464,6 +2712,7 @@ public FsStatus getStatus(Path p) throws IOException {
    * Set permission of a path.
    * @param p The path
    * @param permission permission
   * @throws IOException IO failure
    */
   public void setPermission(Path p, FsPermission permission
       ) throws IOException {
@@ -2475,20 +2724,22 @@ public void setPermission(Path p, FsPermission permission
    * @param p The path
    * @param username If it is null, the original username remains unchanged.
    * @param groupname If it is null, the original groupname remains unchanged.
   * @throws IOException IO failure
    */
   public void setOwner(Path p, String username, String groupname
       ) throws IOException {
   }
 
   /**
   * Set access time of a file
   * Set access time of a file.
    * @param p The path
    * @param mtime Set the modification time of this file.
   *              The number of milliseconds since Jan 1, 1970. 
   *              The number of milliseconds since Jan 1, 1970.
    *              A value of -1 means that this call should not set modification time.
    * @param atime Set the access time of this file.
   *              The number of milliseconds since Jan 1, 1970. 
   *              The number of milliseconds since Jan 1, 1970.
    *              A value of -1 means that this call should not set access time.
   * @throws IOException IO failure
    */
   public void setTimes(Path p, long mtime, long atime
       ) throws IOException {
@@ -2498,47 +2749,56 @@ public void setTimes(Path p, long mtime, long atime
    * Create a snapshot with a default name.
    * @param path The directory where snapshots will be taken.
    * @return the snapshot path.
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
    */
   public final Path createSnapshot(Path path) throws IOException {
     return createSnapshot(path, null);
   }
 
   /**
   * Create a snapshot
   * Create a snapshot.
    * @param path The directory where snapshots will be taken.
    * @param snapshotName The name of the snapshot
    * @return the snapshot path.
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
    */
   public Path createSnapshot(Path path, String snapshotName)
       throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
         + " doesn't support createSnapshot");
   }
  

   /**
   * Rename a snapshot
   * Rename a snapshot.
    * @param path The directory path where the snapshot was taken
    * @param snapshotOldName Old name of the snapshot
    * @param snapshotNewName New name of the snapshot
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void renameSnapshot(Path path, String snapshotOldName,
       String snapshotNewName) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
         + " doesn't support renameSnapshot");
   }
  

   /**
   * Delete a snapshot of a directory
   * Delete a snapshot of a directory.
    * @param path  The directory that the to-be-deleted snapshot belongs to
    * @param snapshotName The name of the snapshot
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void deleteSnapshot(Path path, String snapshotName)
       throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
         + " doesn't support deleteSnapshot");
   }
  

   /**
    * Modifies ACL entries of files and directories.  This method can add new ACL
    * entries or modify the permissions on existing ACL entries.  All existing
@@ -2548,6 +2808,8 @@ public void deleteSnapshot(Path path, String snapshotName)
    * @param path Path to modify
    * @param aclSpec List<AclEntry> describing modifications
    * @throws IOException if an ACL could not be modified
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void modifyAclEntries(Path path, List<AclEntry> aclSpec)
       throws IOException {
@@ -2560,8 +2822,10 @@ public void modifyAclEntries(Path path, List<AclEntry> aclSpec)
    * retained.
    *
    * @param path Path to modify
   * @param aclSpec List<AclEntry> describing entries to remove
   * @param aclSpec List describing entries to remove
    * @throws IOException if an ACL could not be modified
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void removeAclEntries(Path path, List<AclEntry> aclSpec)
       throws IOException {
@@ -2574,6 +2838,8 @@ public void removeAclEntries(Path path, List<AclEntry> aclSpec)
    *
    * @param path Path to modify
    * @throws IOException if an ACL could not be modified
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void removeDefaultAcl(Path path)
       throws IOException {
@@ -2588,6 +2854,8 @@ public void removeDefaultAcl(Path path)
    *
    * @param path Path to modify
    * @throws IOException if an ACL could not be removed
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void removeAcl(Path path)
       throws IOException {
@@ -2600,9 +2868,11 @@ public void removeAcl(Path path)
    * entries.
    *
    * @param path Path to modify
   * @param aclSpec List<AclEntry> describing modifications, must include entries
   * @param aclSpec List describing modifications, which must include entries
    *   for user, group, and others for compatibility with permission bits.
    * @throws IOException if an ACL could not be modified
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void setAcl(Path path, List<AclEntry> aclSpec) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
@@ -2615,6 +2885,8 @@ public void setAcl(Path path, List<AclEntry> aclSpec) throws IOException {
    * @param path Path to get
    * @return AclStatus describing the ACL of the file or directory
    * @throws IOException if an ACL could not be read
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public AclStatus getAclStatus(Path path) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
@@ -2625,13 +2897,15 @@ public AclStatus getAclStatus(Path path) throws IOException {
    * Set an xattr of a file or directory.
    * The name must be prefixed with the namespace followed by ".". For example,
    * "user.attr".
   * <p/>
   * <p>
    * Refer to the HDFS extended attributes user documentation for details.
    *
    * @param path Path to modify
    * @param name xattr name.
    * @param value xattr value.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void setXAttr(Path path, String name, byte[] value)
       throws IOException {
@@ -2643,14 +2917,16 @@ public void setXAttr(Path path, String name, byte[] value)
    * Set an xattr of a file or directory.
    * The name must be prefixed with the namespace followed by ".". For example,
    * "user.attr".
   * <p/>
   * <p>
    * Refer to the HDFS extended attributes user documentation for details.
    *
    * @param path Path to modify
    * @param name xattr name.
    * @param value xattr value.
    * @param flag xattr set flag
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void setXAttr(Path path, String name, byte[] value,
       EnumSet<XAttrSetFlag> flag) throws IOException {
@@ -2662,13 +2938,15 @@ public void setXAttr(Path path, String name, byte[] value,
    * Get an xattr name and value for a file or directory.
    * The name must be prefixed with the namespace followed by ".". For example,
    * "user.attr".
   * <p/>
   * <p>
    * Refer to the HDFS extended attributes user documentation for details.
    *
    * @param path Path to get extended attribute
    * @param name xattr name.
    * @return byte[] xattr value.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public byte[] getXAttr(Path path, String name) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
@@ -2679,12 +2957,14 @@ public void setXAttr(Path path, String name, byte[] value,
    * Get all of the xattr name/value pairs for a file or directory.
    * Only those xattrs which the logged-in user has permissions to view
    * are returned.
   * <p/>
   * <p>
    * Refer to the HDFS extended attributes user documentation for details.
    *
    * @param path Path to get extended attributes
    * @return Map describing the XAttrs of the file or directory
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public Map<String, byte[]> getXAttrs(Path path) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
@@ -2695,13 +2975,15 @@ public void setXAttr(Path path, String name, byte[] value,
    * Get all of the xattrs name/value pairs for a file or directory.
    * Only those xattrs which the logged-in user has permissions to view
    * are returned.
   * <p/>
   * <p>
    * Refer to the HDFS extended attributes user documentation for details.
    *
    * @param path Path to get extended attributes
    * @param names XAttr names.
    * @return Map describing the XAttrs of the file or directory
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public Map<String, byte[]> getXAttrs(Path path, List<String> names)
       throws IOException {
@@ -2713,12 +2995,14 @@ public void setXAttr(Path path, String name, byte[] value,
    * Get all of the xattr names for a file or directory.
    * Only those xattr names which the logged-in user has permissions to view
    * are returned.
   * <p/>
   * <p>
    * Refer to the HDFS extended attributes user documentation for details.
    *
    * @param path Path to get extended attributes
    * @return List<String> of the XAttr names of the file or directory
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public List<String> listXAttrs(Path path) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
@@ -2729,12 +3013,14 @@ public void setXAttr(Path path, String name, byte[] value,
    * Remove an xattr of a file or directory.
    * The name must be prefixed with the namespace followed by ".". For example,
    * "user.attr".
   * <p/>
   * <p>
    * Refer to the HDFS extended attributes user documentation for details.
    *
    * @param path Path to remove extended attribute
    * @param name xattr name
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void removeXAttr(Path path, String name) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
@@ -2748,7 +3034,9 @@ public void removeXAttr(Path path, String name) throws IOException {
    * @param policyName the name of the target storage policy. The list
    *                   of supported Storage policies can be retrieved
    *                   via {@link #getAllStoragePolicies}.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void setStoragePolicy(final Path src, final String policyName)
       throws IOException {
@@ -2759,7 +3047,9 @@ public void setStoragePolicy(final Path src, final String policyName)
   /**
    * Unset the storage policy set for a given file or directory.
    * @param src file or directory path.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public void unsetStoragePolicy(final Path src) throws IOException {
     throw new UnsupportedOperationException(getClass().getSimpleName()
@@ -2771,7 +3061,9 @@ public void unsetStoragePolicy(final Path src) throws IOException {
    *
    * @param src file or directory path.
    * @return storage policy for give file.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public BlockStoragePolicySpi getStoragePolicy(final Path src)
       throws IOException {
@@ -2783,7 +3075,9 @@ public BlockStoragePolicySpi getStoragePolicy(final Path src)
    * Retrieve all the storage policies supported by this file system.
    *
    * @return all storage policies supported by this filesystem.
   * @throws IOException
   * @throws IOException IO failure
   * @throws UnsupportedOperationException if the operation is unsupported
   *         (default outcome).
    */
   public Collection<? extends BlockStoragePolicySpi> getAllStoragePolicies()
       throws IOException {
@@ -2796,7 +3090,7 @@ public BlockStoragePolicySpi getStoragePolicy(final Path src)
    * is deleted.
    *
    * @param path the trash root of the path to be determined.
   * @return the default implementation returns "/user/$USER/.Trash".
   * @return the default implementation returns {@code /user/$USER/.Trash}
    */
   public Path getTrashRoot(Path path) {
     return this.makeQualified(new Path(getHomeDirectory().toUri().getPath(),
@@ -2809,7 +3103,7 @@ public Path getTrashRoot(Path path) {
    * @param allUsers return trash roots for all users if true.
    * @return all the trash root directories.
    *         Default FileSystem returns .Trash under users' home directories if
   *         /user/$USER/.Trash exists.
   *         {@code /user/$USER/.Trash} exists.
    */
   public Collection<FileStatus> getTrashRoots(boolean allUsers) {
     Path userHome = new Path(getHomeDirectory().toUri().getPath());
@@ -2834,7 +3128,7 @@ public Path getTrashRoot(Path path) {
         }
       }
     } catch (IOException e) {
      LOG.warn("Cannot get all trash roots", e);
      LOGGER.warn("Cannot get all trash roots", e);
     }
     return ret;
   }
@@ -2842,23 +3136,37 @@ public Path getTrashRoot(Path path) {
   // making it volatile to be able to do a double checked locking
   private volatile static boolean FILE_SYSTEMS_LOADED = false;
 
  /**
   * Filesystems listed as services.
   */
   private static final Map<String, Class<? extends FileSystem>>
    SERVICE_FILE_SYSTEMS = new HashMap<String, Class<? extends FileSystem>>();
      SERVICE_FILE_SYSTEMS = new HashMap<>();
 
  /**
   * Load the filesystem declarations from service resources.
   * This is a synchronized operation.
   */
   private static void loadFileSystems() {
    LOGGER.debug("Loading filesystems");
     synchronized (FileSystem.class) {
       if (!FILE_SYSTEMS_LOADED) {
         ServiceLoader<FileSystem> serviceLoader = ServiceLoader.load(FileSystem.class);
         Iterator<FileSystem> it = serviceLoader.iterator();
         while (it.hasNext()) {
          FileSystem fs = null;
          FileSystem fs;
           try {
             fs = it.next();
             try {
               SERVICE_FILE_SYSTEMS.put(fs.getScheme(), fs.getClass());
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{}:// = {} from {}",
                    fs.getScheme(), fs.getClass(),
                    ClassUtil.findContainingJar(fs.getClass()));
              }
             } catch (Exception e) {
              LOG.warn("Cannot load: " + fs + " from " +
                  ClassUtil.findContainingJar(fs.getClass()), e);
              LOGGER.warn("Cannot load: {} from {}", fs,
                  ClassUtil.findContainingJar(fs.getClass()));
              LOGGER.info("Full exception loading: {}", fs, e);
             }
           } catch (ServiceConfigurationError ee) {
             LOG.warn("Cannot load filesystem: " + ee);
@@ -2877,47 +3185,75 @@ private static void loadFileSystems() {
     }
   }
 
  /**
   * Get the FileSystem implementation class of a filesystem.
   * This triggers a scan and load of all FileSystem implementations listed as
   * services and discovered via the {@link ServiceLoader}
   * @param scheme URL scheme of FS
   * @param conf configuration: can be null, in which case the check for
   * a filesystem binding declaration in the configuration is skipped.
   * @return the filesystem
   * @throws UnsupportedFileSystemException if there was no known implementation
   *         for the scheme.
   * @throws IOException if the filesystem could not be loaded
   */
   public static Class<? extends FileSystem> getFileSystemClass(String scheme,
       Configuration conf) throws IOException {
     if (!FILE_SYSTEMS_LOADED) {
       loadFileSystems();
     }
    LOGGER.debug("Looking for FS supporting {}", scheme);
     Class<? extends FileSystem> clazz = null;
     if (conf != null) {
      clazz = (Class<? extends FileSystem>) conf.getClass("fs." + scheme + ".impl", null);
      String property = "fs." + scheme + ".impl";
      LOGGER.debug("looking for configuration option {}", property);
      clazz = (Class<? extends FileSystem>) conf.getClass(
          property, null);
    } else {
      LOGGER.debug("No configuration: skipping check for fs.{}.impl", scheme);
     }
     if (clazz == null) {
      LOGGER.debug("Looking in service filesystems for implementation class");
       clazz = SERVICE_FILE_SYSTEMS.get(scheme);
    } else {
      LOGGER.debug("Filesystem {} defined in configuration option", scheme);
     }
     if (clazz == null) {
      throw new IOException("No FileSystem for scheme: " + scheme);
      throw new UnsupportedFileSystemException("No FileSystem for scheme "
          + "\"" + scheme + "\"");
     }
    LOGGER.debug("FS for {} is {}", scheme, clazz);
     return clazz;
   }
 
  private static FileSystem createFileSystem(URI uri, Configuration conf
      ) throws IOException {
  /**
   * Create and initialize a new instance of a FileSystem.
   * @param uri URI containing the FS schema and FS details
   * @param conf configuration to use to look for the FS instance declaration
   * and to pass to the {@link FileSystem#initialize(URI, Configuration)}.
   * @return the initialized filesystem.
   * @throws IOException problems loading or initializing the FileSystem
   */
  private static FileSystem createFileSystem(URI uri, Configuration conf)
      throws IOException {
     Tracer tracer = FsTracer.get(conf);
    TraceScope scope = tracer.newScope("FileSystem#createFileSystem");
    scope.addKVAnnotation("scheme", uri.getScheme());
    try {
    try(TraceScope scope = tracer.newScope("FileSystem#createFileSystem")) {
      scope.addKVAnnotation("scheme", uri.getScheme());
       Class<?> clazz = getFileSystemClass(uri.getScheme(), conf);
       FileSystem fs = (FileSystem)ReflectionUtils.newInstance(clazz, conf);
       fs.initialize(uri, conf);
       return fs;
    } finally {
      scope.close();
     }
   }
 
  /** Caching FileSystem objects */
  /** Caching FileSystem objects. */
   static class Cache {
     private final ClientFinalizer clientFinalizer = new ClientFinalizer();
 
    private final Map<Key, FileSystem> map = new HashMap<Key, FileSystem>();
    private final Set<Key> toAutoClose = new HashSet<Key>();
    private final Map<Key, FileSystem> map = new HashMap<>();
    private final Set<Key> toAutoClose = new HashSet<>();
 
    /** A variable that makes all objects in the cache unique */
    /** A variable that makes all objects in the cache unique. */
     private static AtomicLong unique = new AtomicLong(1);
 
     FileSystem get(URI uri, Configuration conf) throws IOException{
@@ -2925,13 +3261,27 @@ FileSystem get(URI uri, Configuration conf) throws IOException{
       return getInternal(uri, conf, key);
     }
 
    /** The objects inserted into the cache using this method are all unique */
    /** The objects inserted into the cache using this method are all unique. */
     FileSystem getUnique(URI uri, Configuration conf) throws IOException{
       Key key = new Key(uri, conf, unique.getAndIncrement());
       return getInternal(uri, conf, key);
     }
 
    private FileSystem getInternal(URI uri, Configuration conf, Key key) throws IOException{
    /**
     * Get the FS instance if the key maps to an instance, creating and
     * initializing the FS if it is not found.
     * If this is the first entry in the map and the JVM is not shutting down,
     * this registers a shutdown hook to close filesystems, and adds this
     * FS to the {@code toAutoClose} set if {@code "fs.automatic.close"}
     * is set in the configuration (default: true).
     * @param uri filesystem URI
     * @param conf configuration
     * @param key key to store/retrieve this FileSystem in the cache
     * @return a cached or newly instantiated FileSystem.
     * @throws IOException
     */
    private FileSystem getInternal(URI uri, Configuration conf, Key key)
        throws IOException{
       FileSystem fs;
       synchronized (this) {
         fs = map.get(key);
@@ -2947,7 +3297,7 @@ private FileSystem getInternal(URI uri, Configuration conf, Key key) throws IOEx
           fs.close(); // close the new file system
           return oldfs;  // return the old file system
         }
        

         // now insert the new file system into the map
         if (map.isEmpty()
                 && !ShutdownHookManager.get().isShutdownInProgress()) {
@@ -2972,6 +3322,11 @@ synchronized void remove(Key key, FileSystem fs) {
       }
     }
 
    /**
     * Close all FileSystems in the cache, whether they are marked for
     * automatic closing or not.
     * @throws IOException a problem arose closing one or more FileSystem.
     */
     synchronized void closeAll() throws IOException {
       closeAll(false);
     }
@@ -2979,13 +3334,14 @@ synchronized void closeAll() throws IOException {
     /**
      * Close all FileSystem instances in the Cache.
      * @param onlyAutomatic only close those that are marked for automatic closing
     * @throws IOException a problem arose closing one or more FileSystem.
      */
     synchronized void closeAll(boolean onlyAutomatic) throws IOException {
      List<IOException> exceptions = new ArrayList<IOException>();
      List<IOException> exceptions = new ArrayList<>();
 
       // Make a copy of the keys in the map since we'll be modifying
       // the map while iterating over it, which isn't safe.
      List<Key> keys = new ArrayList<Key>();
      List<Key> keys = new ArrayList<>();
       keys.addAll(map.keySet());
 
       for (Key key : keys) {
@@ -3020,23 +3376,23 @@ public synchronized void run() {
         try {
           closeAll(true);
         } catch (IOException e) {
          LOG.info("FileSystem.Cache.closeAll() threw an exception:\n" + e);
          LOGGER.info("FileSystem.Cache.closeAll() threw an exception:\n" + e);
         }
       }
     }
 
     synchronized void closeAll(UserGroupInformation ugi) throws IOException {
      List<FileSystem> targetFSList = new ArrayList<FileSystem>();
      //Make a pass over the list and collect the filesystems to close
      List<FileSystem> targetFSList = new ArrayList<>(map.entrySet().size());
      //Make a pass over the list and collect the FileSystems to close
       //we cannot close inline since close() removes the entry from the Map
       for (Map.Entry<Key, FileSystem> entry : map.entrySet()) {
         final Key key = entry.getKey();
         final FileSystem fs = entry.getValue();
         if (ugi.equals(key.ugi) && fs != null) {
          targetFSList.add(fs);   
          targetFSList.add(fs);
         }
       }
      List<IOException> exceptions = new ArrayList<IOException>();
      List<IOException> exceptions = new ArrayList<>();
       //now make a pass over the target list and close each
       for (FileSystem fs : targetFSList) {
         try {
@@ -3068,7 +3424,7 @@ synchronized void closeAll(UserGroupInformation ugi) throws IOException {
         authority = uri.getAuthority()==null ?
             "" : StringUtils.toLowerCase(uri.getAuthority());
         this.unique = unique;
        

         this.ugi = UserGroupInformation.getCurrentUser();
       }
 
@@ -3078,7 +3434,7 @@ public int hashCode() {
       }
 
       static boolean isEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));        
        return a == b || (a != null && a.equals(b));
       }
 
       @Override
@@ -3086,42 +3442,42 @@ public boolean equals(Object obj) {
         if (obj == this) {
           return true;
         }
        if (obj != null && obj instanceof Key) {
        if (obj instanceof Key) {
           Key that = (Key)obj;
           return isEqual(this.scheme, that.scheme)
                  && isEqual(this.authority, that.authority)
                  && isEqual(this.ugi, that.ugi)
                  && (this.unique == that.unique);
         }
        return false;        
        return false;
       }
 
       @Override
       public String toString() {
        return "("+ugi.toString() + ")@" + scheme + "://" + authority;        
        return "("+ugi.toString() + ")@" + scheme + "://" + authority;
       }
     }
   }
  

   /**
    * Tracks statistics about how many reads, writes, and so forth have been
    * done in a FileSystem.
   * 
   * Since there is only one of these objects per FileSystem, there will 
   *
   * Since there is only one of these objects per FileSystem, there will
    * typically be many threads writing to this object.  Almost every operation
    * on an open file will involve a write to this object.  In contrast, reading
    * statistics is done infrequently by most programs, and not at all by others.
    * Hence, this is optimized for writes.
   * 
   * Each thread writes to its own thread-local area of memory.  This removes 
   *
   * Each thread writes to its own thread-local area of memory.  This removes
    * contention and allows us to scale up to many, many threads.  To read
   * statistics, the reader thread totals up the contents of all of the 
   * statistics, the reader thread totals up the contents of all of the
    * thread-local data areas.
    */
   public static final class Statistics {
     /**
      * Statistics data.
     * 
     *
      * There is only a single writer to thread-local StatisticsData objects.
      * Hence, volatile is adequate here-- we do not need AtomicLong or similar
      * to prevent lost updates.
@@ -3180,23 +3536,23 @@ public String toString() {
             + readOps + " read ops, " + largeReadOps + " large read ops, "
             + writeOps + " write ops";
       }
      

       public long getBytesRead() {
         return bytesRead;
       }
      

       public long getBytesWritten() {
         return bytesWritten;
       }
      

       public int getReadOps() {
         return readOps;
       }
      

       public int getLargeReadOps() {
         return largeReadOps;
       }
      

       public int getWriteOps() {
         return writeOps;
       }
@@ -3236,6 +3592,7 @@ public long getBytesReadDistanceOfFiveOrLarger() {
     /**
      * Thread-local data.
      */
    @SuppressWarnings("ThreadLocalNotStaticFinal")
     private final ThreadLocal<StatisticsData> threadData;
 
     /**
@@ -3254,7 +3611,7 @@ public long getBytesReadDistanceOfFiveOrLarger() {
     private static final Thread STATS_DATA_CLEANER;
 
     static {
      STATS_DATA_REF_QUEUE = new ReferenceQueue<Thread>();
      STATS_DATA_REF_QUEUE = new ReferenceQueue<>();
       // start a single daemon cleaner thread
       STATS_DATA_CLEANER = new Thread(new StatisticsDataReferenceCleaner());
       STATS_DATA_CLEANER.
@@ -3266,13 +3623,13 @@ public long getBytesReadDistanceOfFiveOrLarger() {
     public Statistics(String scheme) {
       this.scheme = scheme;
       this.rootData = new StatisticsData();
      this.threadData = new ThreadLocal<StatisticsData>();
      this.allData = new HashSet<StatisticsDataReference>();
      this.threadData = new ThreadLocal<>();
      this.allData = new HashSet<>();
     }
 
     /**
      * Copy constructor.
     * 
     *
      * @param other    The input Statistics object which is cloned.
      */
     public Statistics(Statistics other) {
@@ -3288,8 +3645,8 @@ public Void aggregate() {
           return null;
         }
       });
      this.threadData = new ThreadLocal<StatisticsData>();
      this.allData = new HashSet<StatisticsDataReference>();
      this.threadData = new ThreadLocal<>();
      this.allData = new HashSet<>();
     }
 
     /**
@@ -3297,10 +3654,10 @@ public Void aggregate() {
      * with that thread. On the thread being garbage collected, it is enqueued
      * to the reference queue for clean-up.
      */
    private class StatisticsDataReference extends WeakReference<Thread> {
    private final class StatisticsDataReference extends WeakReference<Thread> {
       private final StatisticsData data;
 
      public StatisticsDataReference(StatisticsData data, Thread thread) {
      private StatisticsDataReference(StatisticsData data, Thread thread) {
         super(thread, STATS_DATA_REF_QUEUE);
         this.data = data;
       }
@@ -3339,11 +3696,11 @@ public void run() {
                 (StatisticsDataReference)STATS_DATA_REF_QUEUE.remove();
             ref.cleanUp();
           } catch (InterruptedException ie) {
            LOG.warn("Cleaner thread interrupted, will stop", ie);
            LOGGER.warn("Cleaner thread interrupted, will stop", ie);
             Thread.currentThread().interrupt();
           } catch (Throwable th) {
            LOG.warn("Exception in the cleaner thread but it will continue to "
                + "run", th);
            LOGGER.warn("Exception in the cleaner thread but it will" +
                " continue to run", th);
           }
         }
       }
@@ -3367,23 +3724,23 @@ public StatisticsData getThreadStatistics() {
     }
 
     /**
     * Increment the bytes read in the statistics
     * Increment the bytes read in the statistics.
      * @param newBytes the additional bytes read
      */
     public void incrementBytesRead(long newBytes) {
       getThreadStatistics().bytesRead += newBytes;
     }
    

     /**
     * Increment the bytes written in the statistics
     * Increment the bytes written in the statistics.
      * @param newBytes the additional bytes written
      */
     public void incrementBytesWritten(long newBytes) {
       getThreadStatistics().bytesWritten += newBytes;
     }
    

     /**
     * Increment the number of read operations
     * Increment the number of read operations.
      * @param count number of read operations
      */
     public void incrementReadOps(int count) {
@@ -3391,7 +3748,7 @@ public void incrementReadOps(int count) {
     }
 
     /**
     * Increment the number of large read operations
     * Increment the number of large read operations.
      * @param count number of large read operations
      */
     public void incrementLargeReadOps(int count) {
@@ -3399,7 +3756,7 @@ public void incrementLargeReadOps(int count) {
     }
 
     /**
     * Increment the number of write operations
     * Increment the number of write operations.
      * @param count number of write operations
      */
     public void incrementWriteOps(int count) {
@@ -3438,7 +3795,7 @@ public void incrementBytesReadByDistance(int distance, long newBytes) {
      * this Statistics object.
      *
      * For each StatisticsData object, we will call accept on the visitor.
     * Finally, at the end, we will call aggregate to get the final total. 
     * Finally, at the end, we will call aggregate to get the final total.
      *
      * @param         visitor to use.
      * @return        The total.
@@ -3453,7 +3810,7 @@ public void incrementBytesReadByDistance(int distance, long newBytes) {
     }
 
     /**
     * Get the total number of bytes read
     * Get the total number of bytes read.
      * @return the number of bytes
      */
     public long getBytesRead() {
@@ -3470,9 +3827,9 @@ public Long aggregate() {
         }
       });
     }
    

     /**
     * Get the total number of bytes written
     * Get the total number of bytes written.
      * @return the number of bytes
      */
     public long getBytesWritten() {
@@ -3489,9 +3846,9 @@ public Long aggregate() {
         }
       });
     }
    

     /**
     * Get the number of file system read operations such as list files
     * Get the number of file system read operations such as list files.
      * @return number of read operations
      */
     public int getReadOps() {
@@ -3512,7 +3869,7 @@ public Integer aggregate() {
 
     /**
      * Get the number of large file system read operations such as list files
     * under a large directory
     * under a large directory.
      * @return number of large read operations
      */
     public int getLargeReadOps() {
@@ -3531,7 +3888,7 @@ public Integer aggregate() {
     }
 
     /**
     * Get the number of file system write operations such as create, append 
     * Get the number of file system write operations such as create, append
      * rename etc.
      * @return number of write operations
      */
@@ -3581,7 +3938,7 @@ public long getBytesReadByDistance(int distance) {
     }
 
     /**
     * Get all statistics data
     * Get all statistics data.
      * MR or other frameworks can use the method to get all statistics at once.
      * @return the StatisticsData
      */
@@ -3650,7 +4007,7 @@ public Void aggregate() {
         }
       });
     }
    

     /**
      * Get the uri scheme associated with this statistics object.
      * @return the schema associated with this set of statistics
@@ -3664,7 +4021,7 @@ synchronized int getAllThreadLocalDataSize() {
       return allData.size();
     }
   }
  

   /**
    * Get the Map of Statistics object indexed by URI Scheme.
    * @return a Map having a key as URI scheme and value as Statistics object
@@ -3672,7 +4029,7 @@ synchronized int getAllThreadLocalDataSize() {
    */
   @Deprecated
   public static synchronized Map<String, Statistics> getStatistics() {
    Map<String, Statistics> result = new HashMap<String, Statistics>();
    Map<String, Statistics> result = new HashMap<>();
     for(Statistics stat: statisticsTable.values()) {
       result.put(stat.getScheme(), stat);
     }
@@ -3685,11 +4042,11 @@ synchronized int getAllThreadLocalDataSize() {
    */
   @Deprecated
   public static synchronized List<Statistics> getAllStatistics() {
    return new ArrayList<Statistics>(statisticsTable.values());
    return new ArrayList<>(statisticsTable.values());
   }
  

   /**
   * Get the statistics for a particular file system
   * Get the statistics for a particular file system.
    * @param cls the class to lookup
    * @return a statistics object
    * @deprecated use {@link #getGlobalStorageStatistics()}
@@ -3714,22 +4071,22 @@ public StorageStatistics provide() {
     }
     return result;
   }
  

   /**
   * Reset all statistics for all file systems
   * Reset all statistics for all file systems.
    */
   public static synchronized void clearStatistics() {
     GlobalStorageStatistics.INSTANCE.reset();
   }
 
   /**
   * Print all statistics for all file systems
   * Print all statistics for all file systems to {@code System.out}
    */
   public static synchronized
   void printStatistics() throws IOException {
    for (Map.Entry<Class<? extends FileSystem>, Statistics> pair: 
    for (Map.Entry<Class<? extends FileSystem>, Statistics> pair:
             statisticsTable.entrySet()) {
      System.out.println("  FileSystem " + pair.getKey().getName() + 
      System.out.println("  FileSystem " + pair.getKey().getName() +
                          ": " + pair.getValue());
     }
   }
@@ -3737,8 +4094,6 @@ void printStatistics() throws IOException {
   // Symlinks are temporarily disabled - see HADOOP-10020 and HADOOP-10052
   private static boolean symlinksEnabled = false;
 
  private static Configuration conf = null;

   @VisibleForTesting
   public static boolean areSymlinksEnabled() {
     return symlinksEnabled;
diff --git a/hadoop-common-project/hadoop-common/src/site/markdown/filesystem/filesystem.md b/hadoop-common-project/hadoop-common/src/site/markdown/filesystem/filesystem.md
index b18b5f60353..201d3974fd3 100644
-- a/hadoop-common-project/hadoop-common/src/site/markdown/filesystem/filesystem.md
++ b/hadoop-common-project/hadoop-common/src/site/markdown/filesystem/filesystem.md
@@ -419,7 +419,7 @@ If the filesystem is not location aware, it SHOULD return
         BlockLocation(["localhost:9866"] ,
                   ["localhost"],
                   ["/default/localhost"]
                   0, F.getLen())
                   0, f.getLen())
        ] ;
 
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDefaultUri.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDefaultUri.java
index f2327353a67..b84d66aa4ce 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDefaultUri.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDefaultUri.java
@@ -21,14 +21,14 @@
 import static org.hamcrest.CoreMatchers.instanceOf;
 import static org.hamcrest.CoreMatchers.is;
 import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
 
 import java.io.IOException;
 import java.net.URI;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.test.GenericTestUtils;

 import org.junit.Test;
import static org.apache.hadoop.test.LambdaTestUtils.*;
 
 /**
  * Test default URI related APIs in {@link FileSystem}.
@@ -69,15 +69,12 @@ public void tetGetDefaultUriNoScheme() {
   }
 
   @Test
  public void tetGetDefaultUriNoSchemeTrailingSlash() {
  public void tetGetDefaultUriNoSchemeTrailingSlash() throws Exception {
     conf.set(FS_DEFAULT_NAME_KEY, "nn_host/");
    try {
      FileSystem.getDefaultUri(conf);
      fail("Expect IAE: No scheme in default FS");
    } catch (IllegalArgumentException e) {
      GenericTestUtils.assertExceptionContains(
          "No scheme in default FS", e);
    }
    intercept(IllegalArgumentException.class,
        "No scheme in default FS",
        () -> FileSystem.getDefaultUri(conf));

   }
 
   @Test
@@ -88,28 +85,19 @@ public void tetFsGet() throws IOException {
   }
 
   @Test
  public void tetFsGetNoScheme() throws IOException {
  public void tetFsGetNoScheme() throws Exception {
     // Bare host name or address indicates hdfs scheme
     conf.set(FS_DEFAULT_NAME_KEY, "nn_host");
    try {
      FileSystem.get(conf);
      fail("Expect IOE: No FileSystem for scheme: hdfs");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "No FileSystem for scheme: hdfs", e);
    }
    intercept(UnsupportedFileSystemException.class, "hdfs",
        () -> FileSystem.get(conf));
   }
 
   @Test
  public void tetFsGetNoSchemeTrailingSlash() throws IOException {
  public void tetFsGetNoSchemeTrailingSlash() throws Exception {
     // Bare host name or address with trailing slash is invalid
     conf.set(FS_DEFAULT_NAME_KEY, "nn_host/");
    try {
      FileSystem.get(conf);
      fail("Expect IAE: No scheme in default FS");
    } catch (IllegalArgumentException e) {
      GenericTestUtils.assertExceptionContains(
          "No scheme in default FS", e);
    }
    intercept(IllegalArgumentException.class,
        "No scheme in default FS",
        () -> FileSystem.get(conf));
   }
 }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileSystemCaching.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileSystemCaching.java
index 07b07dc8bc4..69ef71e7985 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileSystemCaching.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileSystemCaching.java
@@ -95,16 +95,14 @@ public void testDefaultFsUris() throws Exception {
     try {
       fs = FileSystem.get(URI.create("//host"), conf);
       fail("got fs with auth but no scheme");
    } catch (Exception e) {
      assertEquals("No FileSystem for scheme: null", e.getMessage());
    } catch (UnsupportedFileSystemException e) {
     }
    

     // no scheme, different auth
     try {
       fs = FileSystem.get(URI.create("//host2"), conf);
       fail("got fs with auth but no scheme");
    } catch (Exception e) {
      assertEquals("No FileSystem for scheme: null", e.getMessage());
    } catch (UnsupportedFileSystemException e) {
     }
   }
   
- 
2.19.1.windows.1

