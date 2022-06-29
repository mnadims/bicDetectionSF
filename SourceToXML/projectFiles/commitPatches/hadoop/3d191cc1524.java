From 3d191cc15244e1e29f837b34a9bd1d029e003064 Mon Sep 17 00:00:00 2001
From: Akira Ajisaka <aajisaka@apache.org>
Date: Sun, 31 Jul 2016 11:29:15 +0900
Subject: [PATCH] HADOOP-13440. FileContext does not react on changing umask
 via configuration.

--
 .../java/org/apache/hadoop/fs/FileContext.java   | 16 +++++++---------
 1 file changed, 7 insertions(+), 9 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
index 78321868e85..e6a4cf406c2 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
@@ -215,20 +215,18 @@ public boolean accept(final Path file) {
    * The FileContext is defined by.
    *  1) defaultFS (slash)
    *  2) wd
   *  3) umask
   *  3) umask (Obtained by FsPermission.getUMask(conf))
    */   
   private final AbstractFileSystem defaultFS; //default FS for this FileContext.
   private Path workingDir;          // Fully qualified
  private FsPermission umask;
   private final Configuration conf;
   private final UserGroupInformation ugi;
   final boolean resolveSymlinks;
   private final Tracer tracer;
 
   private FileContext(final AbstractFileSystem defFs,
    final FsPermission theUmask, final Configuration aConf) {
                      final Configuration aConf) {
     defaultFS = defFs;
    umask = theUmask;
     conf = aConf;
     tracer = FsTracer.get(aConf);
     try {
@@ -354,7 +352,7 @@ public AbstractFileSystem run() throws UnsupportedFileSystemException {
    */
   public static FileContext getFileContext(final AbstractFileSystem defFS,
                     final Configuration aConf) {
    return new FileContext(defFS, FsPermission.getUMask(aConf), aConf);
    return new FileContext(defFS, aConf);
   }
   
   /**
@@ -564,7 +562,7 @@ public Path getHomeDirectory() {
    * @return the umask of this FileContext
    */
   public FsPermission getUMask() {
    return umask;
    return FsPermission.getUMask(conf);
   }
   
   /**
@@ -572,7 +570,7 @@ public FsPermission getUMask() {
    * @param newUmask  the new umask
    */
   public void setUMask(final FsPermission newUmask) {
    umask = newUmask;
    FsPermission.setUMask(conf, newUmask);
   }
   
   
@@ -673,7 +671,7 @@ public FSDataOutputStream create(final Path f,
     CreateOpts.Perms permOpt = CreateOpts.getOpt(CreateOpts.Perms.class, opts);
     FsPermission permission = (permOpt != null) ? permOpt.getValue() :
                                       FILE_DEFAULT_PERM;
    permission = permission.applyUMask(umask);
    permission = permission.applyUMask(getUMask());
 
     final CreateOpts[] updatedOpts = 
                       CreateOpts.setOpt(CreateOpts.perms(permission), opts);
@@ -720,7 +718,7 @@ public void mkdir(final Path dir, final FsPermission permission,
       IOException {
     final Path absDir = fixRelativePart(dir);
     final FsPermission absFerms = (permission == null ? 
          FsPermission.getDirDefault() : permission).applyUMask(umask);
          FsPermission.getDirDefault() : permission).applyUMask(getUMask());
     new FSLinkResolver<Void>() {
       @Override
       public Void next(final AbstractFileSystem fs, final Path p) 
- 
2.19.1.windows.1

