From 21d10ccc6e463cf250414264c78acb4a6e7c83e3 Mon Sep 17 00:00:00 2001
From: Colin Patrick Mccabe <cmccabe@cloudera.com>
Date: Fri, 31 Jul 2015 14:55:14 -0700
Subject: [PATCH] HADOOP-7824. NativeIO.java flags and identifiers must be set
 correctly for each platform, not hardcoded to their Linux values (Martin
 Walsh via Colin P. McCabe)

--
 .../hadoop-common/CHANGES.txt                 |   4 +
 .../org/apache/hadoop/io/ReadaheadPool.java   |   4 +-
 .../apache/hadoop/io/nativeio/NativeIO.java   |  83 +++++-----
 .../org/apache/hadoop/io/nativeio/NativeIO.c  | 147 +++++++++++-------
 .../hadoop/io/nativeio/TestNativeIO.java      |  90 +++++++----
 .../hdfs/server/datanode/BlockReceiver.java   |  14 +-
 .../hdfs/server/datanode/BlockSender.java     |  11 +-
 .../server/datanode/TestCachingStrategy.java  |   5 +-
 .../hadoop/mapred/FadvisedChunkedFile.java    |   5 +-
 .../hadoop/mapred/FadvisedFileRegion.java     |   6 +-
 10 files changed, 232 insertions(+), 137 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 5020e91ec2c..675902db887 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1033,6 +1033,10 @@ Release 2.8.0 - UNRELEASED
     HADOOP-10945. 4-digit octal umask permissions throws a parse error (Chang
     Li via jlowe)
 
    HADOOP-7824. NativeIO.java flags and identifiers must be set correctly for
    each platform, not hardcoded to their Linux values (Martin Walsh via Colin
    P. McCabe)

 Release 2.7.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/ReadaheadPool.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/ReadaheadPool.java
index 18099dbb191..a8c06902b11 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/ReadaheadPool.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/ReadaheadPool.java
@@ -29,6 +29,8 @@
 import org.apache.hadoop.classification.InterfaceStability;
 import org.apache.hadoop.io.nativeio.NativeIO;
 
import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.POSIX_FADV_WILLNEED;

 import com.google.common.base.Preconditions;
 import com.google.common.util.concurrent.ThreadFactoryBuilder;
 
@@ -204,7 +206,7 @@ public void run() {
       // other FD, which may be wasted work, but won't cause a problem.
       try {
         NativeIO.POSIX.getCacheManipulator().posixFadviseIfPossible(identifier,
            fd, off, len, NativeIO.POSIX.POSIX_FADV_WILLNEED);
            fd, off, len, POSIX_FADV_WILLNEED);
       } catch (IOException ioe) {
         if (canceled) {
           // no big deal - the reader canceled the request and closed
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java
index 77a40ea76ff..a123f182592 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/nativeio/NativeIO.java
@@ -56,51 +56,54 @@
 @InterfaceStability.Unstable
 public class NativeIO {
   public static class POSIX {
    // Flags for open() call from bits/fcntl.h
    public static final int O_RDONLY   =    00;
    public static final int O_WRONLY   =    01;
    public static final int O_RDWR     =    02;
    public static final int O_CREAT    =  0100;
    public static final int O_EXCL     =  0200;
    public static final int O_NOCTTY   =  0400;
    public static final int O_TRUNC    = 01000;
    public static final int O_APPEND   = 02000;
    public static final int O_NONBLOCK = 04000;
    public static final int O_SYNC   =  010000;

    // Flags for posix_fadvise() from bits/fcntl.h
    // Flags for open() call from bits/fcntl.h - Set by JNI
    public static int O_RDONLY = -1;
    public static int O_WRONLY = -1;
    public static int O_RDWR = -1;
    public static int O_CREAT = -1;
    public static int O_EXCL = -1;
    public static int O_NOCTTY = -1;
    public static int O_TRUNC = -1;
    public static int O_APPEND = -1;
    public static int O_NONBLOCK = -1;
    public static int O_SYNC = -1;

    // Flags for posix_fadvise() from bits/fcntl.h - Set by JNI
     /* No further special treatment.  */
    public static final int POSIX_FADV_NORMAL = 0;
    public static int POSIX_FADV_NORMAL = -1;
     /* Expect random page references.  */
    public static final int POSIX_FADV_RANDOM = 1;
    public static int POSIX_FADV_RANDOM = -1;
     /* Expect sequential page references.  */
    public static final int POSIX_FADV_SEQUENTIAL = 2;
    public static int POSIX_FADV_SEQUENTIAL = -1;
     /* Will need these pages.  */
    public static final int POSIX_FADV_WILLNEED = 3;
    public static int POSIX_FADV_WILLNEED = -1;
     /* Don't need these pages.  */
    public static final int POSIX_FADV_DONTNEED = 4;
    public static int POSIX_FADV_DONTNEED = -1;
     /* Data will be accessed once.  */
    public static final int POSIX_FADV_NOREUSE = 5;
    public static int POSIX_FADV_NOREUSE = -1;
 
 
    // Updated by JNI when supported by glibc.  Leave defaults in case kernel
    // supports sync_file_range, but glibc does not.
     /* Wait upon writeout of all pages
        in the range before performing the
        write.  */
    public static final int SYNC_FILE_RANGE_WAIT_BEFORE = 1;
    public static int SYNC_FILE_RANGE_WAIT_BEFORE = 1;
     /* Initiate writeout of all those
        dirty pages in the range which are
        not presently under writeback.  */
    public static final int SYNC_FILE_RANGE_WRITE = 2;

    public static int SYNC_FILE_RANGE_WRITE = 2;
     /* Wait upon writeout of all pages in
        the range after performing the
        write.  */
    public static final int SYNC_FILE_RANGE_WAIT_AFTER = 4;
    public static int SYNC_FILE_RANGE_WAIT_AFTER = 4;
 
     private static final Log LOG = LogFactory.getLog(NativeIO.class);
 
    // Set to true via JNI if possible
    public static boolean fadvisePossible = false;

     private static boolean nativeLoaded = false;
    private static boolean fadvisePossible = true;
     private static boolean syncFileRangePossible = true;
 
     static final String WORKAROUND_NON_THREADSAFE_CALLS_KEY =
@@ -262,8 +265,6 @@ static void posixFadviseIfPossible(String identifier,
       if (nativeLoaded && fadvisePossible) {
         try {
           posix_fadvise(fd, offset, len, flags);
        } catch (UnsupportedOperationException uoe) {
          fadvisePossible = false;
         } catch (UnsatisfiedLinkError ule) {
           fadvisePossible = false;
         }
@@ -344,21 +345,21 @@ public static void munmap(MappedByteBuffer buffer) {
       private String owner, group;
       private int mode;
 
      // Mode constants
      public static final int S_IFMT = 0170000;      /* type of file */
      public static final int   S_IFIFO  = 0010000;  /* named pipe (fifo) */
      public static final int   S_IFCHR  = 0020000;  /* character special */
      public static final int   S_IFDIR  = 0040000;  /* directory */
      public static final int   S_IFBLK  = 0060000;  /* block special */
      public static final int   S_IFREG  = 0100000;  /* regular */
      public static final int   S_IFLNK  = 0120000;  /* symbolic link */
      public static final int   S_IFSOCK = 0140000;  /* socket */
      public static final int S_ISUID = 0004000;  /* set user id on execution */
      public static final int S_ISGID = 0002000;  /* set group id on execution */
      public static final int S_ISVTX = 0001000;  /* save swapped text even after use */
      public static final int S_IRUSR = 0000400;  /* read permission, owner */
      public static final int S_IWUSR = 0000200;  /* write permission, owner */
      public static final int S_IXUSR = 0000100;  /* execute/search permission, owner */
      // Mode constants - Set by JNI
      public static int S_IFMT = -1;    /* type of file */
      public static int S_IFIFO  = -1;  /* named pipe (fifo) */
      public static int S_IFCHR  = -1;  /* character special */
      public static int S_IFDIR  = -1;  /* directory */
      public static int S_IFBLK  = -1;  /* block special */
      public static int S_IFREG  = -1;  /* regular */
      public static int S_IFLNK  = -1;  /* symbolic link */
      public static int S_IFSOCK = -1;  /* socket */
      public static int S_ISUID = -1;  /* set user id on execution */
      public static int S_ISGID = -1;  /* set group id on execution */
      public static int S_ISVTX = -1;  /* save swapped text even after use */
      public static int S_IRUSR = -1;  /* read permission, owner */
      public static int S_IWUSR = -1;  /* write permission, owner */
      public static int S_IXUSR = -1;  /* execute/search permission, owner */
 
       Stat(int ownerId, int groupId, int mode) {
         this.ownerId = ownerId;
diff --git a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
index bc78ab2bc0a..a716a02a8f7 100644
-- a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
++ b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
@@ -58,6 +58,15 @@
 #define MMAP_PROT_WRITE org_apache_hadoop_io_nativeio_NativeIO_POSIX_MMAP_PROT_WRITE
 #define MMAP_PROT_EXEC org_apache_hadoop_io_nativeio_NativeIO_POSIX_MMAP_PROT_EXEC
 
#define NATIVE_IO_POSIX_CLASS "org/apache/hadoop/io/nativeio/NativeIO$POSIX"
#define NATIVE_IO_STAT_CLASS "org/apache/hadoop/io/nativeio/NativeIO$POSIX$Stat"

#define SET_INT_OR_RETURN(E, C, F) \
  { \
    setStaticInt(E, C, #F, F); \
    if ((*E)->ExceptionCheck(E)) return; \
  }

 // the NativeIO$POSIX$Stat inner class and its constructor
 static jclass stat_clazz;
 static jmethodID stat_ctor;
@@ -101,12 +110,90 @@ static int workaround_non_threadsafe_calls(JNIEnv *env, jclass clazz) {
   return result;
 }
 
/**
 * Sets a static boolean field to the specified value.
 */
static void setStaticBoolean(JNIEnv *env, jclass clazz, char *field,
  jboolean val) {
    jfieldID fid = (*env)->GetStaticFieldID(env, clazz, field, "Z");
    if (fid != NULL) {
      (*env)->SetStaticBooleanField(env, clazz, fid, val);
    }
}

/**
 * Sets a static int field to the specified value.
 */
static void setStaticInt(JNIEnv *env, jclass clazz, char *field,
  jint val) {
    jfieldID fid = (*env)->GetStaticFieldID(env, clazz, field, "I");
    if (fid != NULL) {
      (*env)->SetStaticIntField(env, clazz, fid, val);
    }
}

/**
 * Initialises a list of java constants that are platform specific.
 * These are only initialized in UNIX.
 * Any exceptions that occur will be dealt at the level above.
**/
static void consts_init(JNIEnv *env) {
  jclass clazz = (*env)->FindClass(env, NATIVE_IO_POSIX_CLASS);
  if (clazz == NULL) {
    return; // exception has been raised
  }
  SET_INT_OR_RETURN(env, clazz, O_RDONLY);
  SET_INT_OR_RETURN(env, clazz, O_WRONLY);
  SET_INT_OR_RETURN(env, clazz, O_RDWR);
  SET_INT_OR_RETURN(env, clazz, O_CREAT);
  SET_INT_OR_RETURN(env, clazz, O_EXCL);
  SET_INT_OR_RETURN(env, clazz, O_NOCTTY);
  SET_INT_OR_RETURN(env, clazz, O_TRUNC);
  SET_INT_OR_RETURN(env, clazz, O_APPEND);
  SET_INT_OR_RETURN(env, clazz, O_NONBLOCK);
  SET_INT_OR_RETURN(env, clazz, O_SYNC);
#ifdef HAVE_POSIX_FADVISE
  setStaticBoolean(env, clazz, "fadvisePossible", JNI_TRUE);
  SET_INT_OR_RETURN(env, clazz, POSIX_FADV_NORMAL);
  SET_INT_OR_RETURN(env, clazz, POSIX_FADV_RANDOM);
  SET_INT_OR_RETURN(env, clazz, POSIX_FADV_SEQUENTIAL);
  SET_INT_OR_RETURN(env, clazz, POSIX_FADV_WILLNEED);
  SET_INT_OR_RETURN(env, clazz, POSIX_FADV_DONTNEED);
  SET_INT_OR_RETURN(env, clazz, POSIX_FADV_NOREUSE);
#else
  setStaticBoolean(env, clazz, "fadvisePossible", JNI_FALSE);
#endif
#ifdef HAVE_SYNC_FILE_RANGE
  SET_INT_OR_RETURN(env, clazz, SYNC_FILE_RANGE_WAIT_BEFORE);
  SET_INT_OR_RETURN(env, clazz, SYNC_FILE_RANGE_WRITE);
  SET_INT_OR_RETURN(env, clazz, SYNC_FILE_RANGE_WAIT_AFTER);
#endif
  clazz = (*env)->FindClass(env, NATIVE_IO_STAT_CLASS);
  if (clazz == NULL) {
    return; // exception has been raised
  }
  SET_INT_OR_RETURN(env, clazz, S_IFMT);
  SET_INT_OR_RETURN(env, clazz, S_IFIFO);
  SET_INT_OR_RETURN(env, clazz, S_IFCHR);
  SET_INT_OR_RETURN(env, clazz, S_IFDIR);
  SET_INT_OR_RETURN(env, clazz, S_IFBLK);
  SET_INT_OR_RETURN(env, clazz, S_IFREG);
  SET_INT_OR_RETURN(env, clazz, S_IFLNK);
  SET_INT_OR_RETURN(env, clazz, S_IFSOCK);
  SET_INT_OR_RETURN(env, clazz, S_ISUID);
  SET_INT_OR_RETURN(env, clazz, S_ISGID);
  SET_INT_OR_RETURN(env, clazz, S_ISVTX);
  SET_INT_OR_RETURN(env, clazz, S_IRUSR);
  SET_INT_OR_RETURN(env, clazz, S_IWUSR);
  SET_INT_OR_RETURN(env, clazz, S_IXUSR);
}

 static void stat_init(JNIEnv *env, jclass nativeio_class) {
   jclass clazz = NULL;
   jclass obj_class = NULL;
   jmethodID  obj_ctor = NULL;
   // Init Stat
  clazz = (*env)->FindClass(env, "org/apache/hadoop/io/nativeio/NativeIO$POSIX$Stat");
  clazz = (*env)->FindClass(env, NATIVE_IO_STAT_CLASS);
   if (!clazz) {
     return; // exception has been raised
   }
@@ -180,39 +267,6 @@ static void nioe_deinit(JNIEnv *env) {
   nioe_ctor = NULL;
 }
 
/*
 * Compatibility mapping for fadvise flags. Return the proper value from fnctl.h.
 * If the value is not known, return the argument unchanged.
 */
static int map_fadvise_flag(jint flag) {
#ifdef HAVE_POSIX_FADVISE
  switch(flag) {
    case org_apache_hadoop_io_nativeio_NativeIO_POSIX_POSIX_FADV_NORMAL:
      return POSIX_FADV_NORMAL;
      break;
    case org_apache_hadoop_io_nativeio_NativeIO_POSIX_POSIX_FADV_RANDOM:
      return POSIX_FADV_RANDOM;
      break;
    case org_apache_hadoop_io_nativeio_NativeIO_POSIX_POSIX_FADV_SEQUENTIAL:
      return POSIX_FADV_SEQUENTIAL;
      break;
    case org_apache_hadoop_io_nativeio_NativeIO_POSIX_POSIX_FADV_WILLNEED:
      return POSIX_FADV_WILLNEED;
      break;
    case org_apache_hadoop_io_nativeio_NativeIO_POSIX_POSIX_FADV_DONTNEED:
      return POSIX_FADV_DONTNEED;
      break;
    case org_apache_hadoop_io_nativeio_NativeIO_POSIX_POSIX_FADV_NOREUSE:
      return POSIX_FADV_NOREUSE;
      break;
    default:
      return flag;
  }
#else
  return flag;
#endif
}

 /*
  * private static native void initNative();
  *
@@ -223,6 +277,10 @@ static int map_fadvise_flag(jint flag) {
 JNIEXPORT void JNICALL
 Java_org_apache_hadoop_io_nativeio_NativeIO_initNative(
   JNIEnv *env, jclass clazz) {
#ifdef UNIX
  consts_init(env);
  PASS_EXCEPTIONS_GOTO(env, error);
#endif
   stat_init(env, clazz);
   PASS_EXCEPTIONS_GOTO(env, error);
   nioe_init(env);
@@ -345,7 +403,7 @@ Java_org_apache_hadoop_io_nativeio_NativeIO_00024POSIX_posix_1fadvise(
   PASS_EXCEPTIONS(env);
 
   int err = 0;
  if ((err = posix_fadvise(fd, (off_t)offset, (off_t)len, map_fadvise_flag(flags)))) {
  if ((err = posix_fadvise(fd, (off_t)offset, (off_t)len, flags))) {
 #ifdef __FreeBSD__
     throw_ioe(env, errno);
 #else
@@ -448,22 +506,6 @@ Java_org_apache_hadoop_io_nativeio_NativeIO_00024POSIX_mlock_1native(
 #endif
 }
 
#ifdef __FreeBSD__
static int toFreeBSDFlags(int flags)
{
  int rc = flags & 03;
  if ( flags &  0100 ) rc |= O_CREAT;
  if ( flags &  0200 ) rc |= O_EXCL;
  if ( flags &  0400 ) rc |= O_NOCTTY;
  if ( flags & 01000 ) rc |= O_TRUNC;
  if ( flags & 02000 ) rc |= O_APPEND;
  if ( flags & 04000 ) rc |= O_NONBLOCK;
  if ( flags &010000 ) rc |= O_SYNC;
  if ( flags &020000 ) rc |= O_ASYNC;
  return rc;
}
#endif

 /*
  * Class:     org_apache_hadoop_io_nativeio_NativeIO_POSIX
  * Method:    open
@@ -479,9 +521,6 @@ Java_org_apache_hadoop_io_nativeio_NativeIO_00024POSIX_open(
   jint flags, jint mode)
 {
 #ifdef UNIX
#ifdef __FreeBSD__
  flags = toFreeBSDFlags(flags);
#endif
   jobject ret = NULL;
 
   const char *path = (*env)->GetStringUTFChars(env, j_path, NULL);
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
index bf3ece7894c..13fdbc17a40 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/nativeio/TestNativeIO.java
@@ -55,6 +55,9 @@
 import org.apache.hadoop.util.NativeCodeLoader;
 import org.apache.hadoop.util.Time;
 
import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.*;
import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.Stat.*;

 public class TestNativeIO {
   static final Log LOG = LogFactory.getLog(TestNativeIO.class);
 
@@ -93,9 +96,8 @@ public void testFstat() throws Exception {
     assertEquals(expectedOwner, owner);
     assertNotNull(stat.getGroup());
     assertTrue(!stat.getGroup().isEmpty());
    assertEquals("Stat mode field should indicate a regular file",
      NativeIO.POSIX.Stat.S_IFREG,
      stat.getMode() & NativeIO.POSIX.Stat.S_IFMT);
    assertEquals("Stat mode field should indicate a regular file", S_IFREG,
      stat.getMode() & S_IFMT);
   }
 
   /**
@@ -128,8 +130,7 @@ public void run() {
               assertNotNull(stat.getGroup());
               assertTrue(!stat.getGroup().isEmpty());
               assertEquals("Stat mode field should indicate a regular file",
                NativeIO.POSIX.Stat.S_IFREG,
                stat.getMode() & NativeIO.POSIX.Stat.S_IFMT);
                S_IFREG, stat.getMode() & S_IFMT);
             } catch (Throwable t) {
               thrown.set(t);
             }
@@ -338,8 +339,7 @@ public void testOpenMissingWithoutCreate() throws Exception {
     LOG.info("Open a missing file without O_CREAT and it should fail");
     try {
       FileDescriptor fd = NativeIO.POSIX.open(
        new File(TEST_DIR, "doesntexist").getAbsolutePath(),
        NativeIO.POSIX.O_WRONLY, 0700);
        new File(TEST_DIR, "doesntexist").getAbsolutePath(), O_WRONLY, 0700);
       fail("Able to open a new file without O_CREAT");
     } catch (NativeIOException nioe) {
       LOG.info("Got expected exception", nioe);
@@ -356,7 +356,7 @@ public void testOpenWithCreate() throws Exception {
     LOG.info("Test creating a file with O_CREAT");
     FileDescriptor fd = NativeIO.POSIX.open(
       new File(TEST_DIR, "testWorkingOpen").getAbsolutePath(),
      NativeIO.POSIX.O_WRONLY | NativeIO.POSIX.O_CREAT, 0700);
      O_WRONLY | O_CREAT, 0700);
     assertNotNull(true);
     assertTrue(fd.valid());
     FileOutputStream fos = new FileOutputStream(fd);
@@ -369,7 +369,7 @@ public void testOpenWithCreate() throws Exception {
     try {
       fd = NativeIO.POSIX.open(
         new File(TEST_DIR, "testWorkingOpen").getAbsolutePath(),
        NativeIO.POSIX.O_WRONLY | NativeIO.POSIX.O_CREAT | NativeIO.POSIX.O_EXCL, 0700);
        O_WRONLY | O_CREAT | O_EXCL, 0700);
       fail("Was able to create existing file with O_EXCL");
     } catch (NativeIOException nioe) {
       LOG.info("Got expected exception for failed exclusive create", nioe);
@@ -390,7 +390,7 @@ public void testFDDoesntLeak() throws IOException {
     for (int i = 0; i < 10000; i++) {
       FileDescriptor fd = NativeIO.POSIX.open(
         new File(TEST_DIR, "testNoFdLeak").getAbsolutePath(),
        NativeIO.POSIX.O_WRONLY | NativeIO.POSIX.O_CREAT, 0700);
        O_WRONLY | O_CREAT, 0700);
       assertNotNull(true);
       assertTrue(fd.valid());
       FileOutputStream fos = new FileOutputStream(fd);
@@ -436,8 +436,7 @@ public void testPosixFadvise() throws Exception {
     FileInputStream fis = new FileInputStream("/dev/zero");
     try {
       NativeIO.POSIX.posix_fadvise(
          fis.getFD(), 0, 0,
          NativeIO.POSIX.POSIX_FADV_SEQUENTIAL);
          fis.getFD(), 0, 0, POSIX_FADV_SEQUENTIAL);
     } catch (UnsupportedOperationException uoe) {
       // we should just skip the unit test on machines where we don't
       // have fadvise support
@@ -450,20 +449,14 @@ public void testPosixFadvise() throws Exception {
     }
 
     try {
      NativeIO.POSIX.posix_fadvise(
          fis.getFD(), 0, 1024,
          NativeIO.POSIX.POSIX_FADV_SEQUENTIAL);

      NativeIO.POSIX.posix_fadvise(fis.getFD(), 0, 1024, POSIX_FADV_SEQUENTIAL);
       fail("Did not throw on bad file");
     } catch (NativeIOException nioe) {
       assertEquals(Errno.EBADF, nioe.getErrno());
     }
     
     try {
      NativeIO.POSIX.posix_fadvise(
          null, 0, 1024,
          NativeIO.POSIX.POSIX_FADV_SEQUENTIAL);

      NativeIO.POSIX.posix_fadvise(null, 0, 1024, POSIX_FADV_SEQUENTIAL);
       fail("Did not throw on null file");
     } catch (NullPointerException npe) {
       // expected
@@ -476,9 +469,8 @@ public void testSyncFileRange() throws Exception {
       new File(TEST_DIR, "testSyncFileRange"));
     try {
       fos.write("foo".getBytes());
      NativeIO.POSIX.sync_file_range(
          fos.getFD(), 0, 1024,
          NativeIO.POSIX.SYNC_FILE_RANGE_WRITE);
      NativeIO.POSIX.sync_file_range(fos.getFD(), 0, 1024,
        SYNC_FILE_RANGE_WRITE);
       // no way to verify that this actually has synced,
       // but if it doesn't throw, we can assume it worked
     } catch (UnsupportedOperationException uoe) {
@@ -489,9 +481,8 @@ public void testSyncFileRange() throws Exception {
       fos.close();
     }
     try {
      NativeIO.POSIX.sync_file_range(
          fos.getFD(), 0, 1024,
          NativeIO.POSIX.SYNC_FILE_RANGE_WRITE);
      NativeIO.POSIX.sync_file_range(fos.getFD(), 0, 1024,
	   SYNC_FILE_RANGE_WRITE);
       fail("Did not throw on bad file");
     } catch (NativeIOException nioe) {
       assertEquals(Errno.EBADF, nioe.getErrno());
@@ -657,4 +648,51 @@ public void testCopyFileUnbuffered() throws Exception {
       FileUtils.deleteQuietly(TEST_DIR);
     }
   }

  @Test (timeout=10000)
  public void testNativePosixConsts() {
    assumeTrue("Native POSIX constants not required for Windows",
      !Path.WINDOWS);
    assertTrue("Native 0_RDONLY const not set", O_RDONLY >= 0);
    assertTrue("Native 0_WRONLY const not set", O_WRONLY >= 0);
    assertTrue("Native 0_RDWR const not set", O_RDWR >= 0);
    assertTrue("Native 0_CREAT const not set", O_CREAT >= 0);
    assertTrue("Native 0_EXCL const not set", O_EXCL >= 0);
    assertTrue("Native 0_NOCTTY const not set", O_NOCTTY >= 0);
    assertTrue("Native 0_TRUNC const not set", O_TRUNC >= 0);
    assertTrue("Native 0_APPEND const not set", O_APPEND >= 0);
    assertTrue("Native 0_NONBLOCK const not set", O_NONBLOCK >= 0);
    assertTrue("Native 0_SYNC const not set", O_SYNC >= 0);
    assertTrue("Native S_IFMT const not set", S_IFMT >= 0);
    assertTrue("Native S_IFIFO const not set", S_IFIFO >= 0);
    assertTrue("Native S_IFCHR const not set", S_IFCHR >= 0);
    assertTrue("Native S_IFDIR const not set", S_IFDIR >= 0);
    assertTrue("Native S_IFBLK const not set", S_IFBLK >= 0);
    assertTrue("Native S_IFREG const not set", S_IFREG >= 0);
    assertTrue("Native S_IFLNK const not set", S_IFLNK >= 0);
    assertTrue("Native S_IFSOCK const not set", S_IFSOCK >= 0);
    assertTrue("Native S_ISUID const not set", S_ISUID >= 0);
    assertTrue("Native S_ISGID const not set", S_ISGID >= 0);
    assertTrue("Native S_ISVTX const not set", S_ISVTX >= 0);
    assertTrue("Native S_IRUSR const not set", S_IRUSR >= 0);
    assertTrue("Native S_IWUSR const not set", S_IWUSR >= 0);
    assertTrue("Native S_IXUSR const not set", S_IXUSR >= 0);
  }

  @Test (timeout=10000)
  public void testNativeFadviseConsts() {
    assumeTrue("Fadvise constants not supported", fadvisePossible);
    assertTrue("Native POSIX_FADV_NORMAL const not set",
      POSIX_FADV_NORMAL >= 0);
    assertTrue("Native POSIX_FADV_RANDOM const not set",
      POSIX_FADV_RANDOM >= 0);
    assertTrue("Native POSIX_FADV_SEQUENTIAL const not set",
      POSIX_FADV_SEQUENTIAL >= 0);
    assertTrue("Native POSIX_FADV_WILLNEED const not set",
      POSIX_FADV_WILLNEED >= 0);
    assertTrue("Native POSIX_FADV_DONTNEED const not set",
      POSIX_FADV_DONTNEED >= 0);
    assertTrue("Native POSIX_FADV_NOREUSE const not set",
      POSIX_FADV_NOREUSE >= 0);
  }
 }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockReceiver.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockReceiver.java
index 55c9d572c82..1cb308f7319 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockReceiver.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockReceiver.java
@@ -58,6 +58,9 @@
 import org.apache.hadoop.util.StringUtils;
 import org.apache.hadoop.util.Time;
 
import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.POSIX_FADV_DONTNEED;
import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.SYNC_FILE_RANGE_WRITE;

 import com.google.common.annotations.VisibleForTesting;
 
 /** A class that receives a block and writes to its own disk, meanwhile
@@ -791,12 +794,12 @@ private void manageWriterOsCache(long offsetInBlock) {
             this.datanode.getFSDataset().submitBackgroundSyncFileRangeRequest(
                 block, outFd, lastCacheManagementOffset,
                 offsetInBlock - lastCacheManagementOffset,
                NativeIO.POSIX.SYNC_FILE_RANGE_WRITE);
                SYNC_FILE_RANGE_WRITE);
           } else {
             NativeIO.POSIX.syncFileRangeIfPossible(outFd,
                lastCacheManagementOffset, offsetInBlock
                    - lastCacheManagementOffset,
                NativeIO.POSIX.SYNC_FILE_RANGE_WRITE);
                lastCacheManagementOffset,
                offsetInBlock - lastCacheManagementOffset,
                SYNC_FILE_RANGE_WRITE);
           }
         }
         //
@@ -812,8 +815,7 @@ private void manageWriterOsCache(long offsetInBlock) {
         long dropPos = lastCacheManagementOffset - CACHE_DROP_LAG_BYTES;
         if (dropPos > 0 && dropCacheBehindWrites) {
           NativeIO.POSIX.getCacheManipulator().posixFadviseIfPossible(
              block.getBlockName(), outFd, 0, dropPos,
              NativeIO.POSIX.POSIX_FADV_DONTNEED);
              block.getBlockName(), outFd, 0, dropPos, POSIX_FADV_DONTNEED);
         }
         lastCacheManagementOffset = offsetInBlock;
         long duration = Time.monotonicNow() - begin;
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockSender.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockSender.java
index 79f4dd7aa2e..fb8b132dfb4 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockSender.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/datanode/BlockSender.java
@@ -51,6 +51,9 @@
 import org.apache.htrace.Trace;
 import org.apache.htrace.TraceScope;
 
import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.POSIX_FADV_DONTNEED;
import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.POSIX_FADV_SEQUENTIAL;

 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Preconditions;
 
@@ -411,8 +414,7 @@ public void close() throws IOException {
       try {
         NativeIO.POSIX.getCacheManipulator().posixFadviseIfPossible(
             block.getBlockName(), blockInFd, lastCacheDropOffset,
            offset - lastCacheDropOffset,
            NativeIO.POSIX.POSIX_FADV_DONTNEED);
            offset - lastCacheDropOffset, POSIX_FADV_DONTNEED);
       } catch (Exception e) {
         LOG.warn("Unable to drop cache on file close", e);
       }
@@ -729,8 +731,7 @@ private long doSendBlock(DataOutputStream out, OutputStream baseStream,
     if (isLongRead() && blockInFd != null) {
       // Advise that this file descriptor will be accessed sequentially.
       NativeIO.POSIX.getCacheManipulator().posixFadviseIfPossible(
          block.getBlockName(), blockInFd, 0, 0,
          NativeIO.POSIX.POSIX_FADV_SEQUENTIAL);
          block.getBlockName(), blockInFd, 0, 0, POSIX_FADV_SEQUENTIAL);
     }
     
     // Trigger readahead of beginning of file if configured.
@@ -818,7 +819,7 @@ private void manageOsCache() throws IOException {
         long dropLength = offset - lastCacheDropOffset;
         NativeIO.POSIX.getCacheManipulator().posixFadviseIfPossible(
             block.getBlockName(), blockInFd, lastCacheDropOffset,
            dropLength, NativeIO.POSIX.POSIX_FADV_DONTNEED);
            dropLength, POSIX_FADV_DONTNEED);
         lastCacheDropOffset = offset;
       }
     }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/datanode/TestCachingStrategy.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/datanode/TestCachingStrategy.java
index 709554a0ea4..bd1a7771a14 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/datanode/TestCachingStrategy.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/datanode/TestCachingStrategy.java
@@ -39,6 +39,9 @@
 import org.apache.hadoop.io.nativeio.NativeIO;
 import org.apache.hadoop.io.nativeio.NativeIO.POSIX.CacheManipulator;
 import org.apache.hadoop.io.nativeio.NativeIOException;

import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.POSIX_FADV_DONTNEED;

 import org.junit.Assert;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -78,7 +81,7 @@ public static void setupTest() {
     synchronized void fadvise(int offset, int len, int flags) {
       LOG.debug("got fadvise(offset=" + offset + ", len=" + len +
           ",flags=" + flags + ")");
      if (flags == NativeIO.POSIX.POSIX_FADV_DONTNEED) {
      if (flags == POSIX_FADV_DONTNEED) {
         for (int i = 0; i < len; i++) {
           dropped[(offset + i)] = true;
         }
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedChunkedFile.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedChunkedFile.java
index 70e68292b95..7e24e8985b1 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedChunkedFile.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedChunkedFile.java
@@ -27,6 +27,9 @@
 import org.apache.hadoop.io.ReadaheadPool;
 import org.apache.hadoop.io.ReadaheadPool.ReadaheadRequest;
 import org.apache.hadoop.io.nativeio.NativeIO;

import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.POSIX_FADV_DONTNEED;

 import org.jboss.netty.handler.stream.ChunkedFile;
 
 public class FadvisedChunkedFile extends ChunkedFile {
@@ -72,7 +75,7 @@ public void close() throws Exception {
         NativeIO.POSIX.getCacheManipulator().posixFadviseIfPossible(identifier,
             fd,
             getStartOffset(), getEndOffset() - getStartOffset(),
            NativeIO.POSIX.POSIX_FADV_DONTNEED);
            POSIX_FADV_DONTNEED);
       } catch (Throwable t) {
         LOG.warn("Failed to manage OS cache for " + identifier, t);
       }
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedFileRegion.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedFileRegion.java
index f1acfff9b25..6fd46a5310c 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedFileRegion.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-shuffle/src/main/java/org/apache/hadoop/mapred/FadvisedFileRegion.java
@@ -30,6 +30,9 @@
 import org.apache.hadoop.io.ReadaheadPool;
 import org.apache.hadoop.io.ReadaheadPool.ReadaheadRequest;
 import org.apache.hadoop.io.nativeio.NativeIO;

import static org.apache.hadoop.io.nativeio.NativeIO.POSIX.POSIX_FADV_DONTNEED;

 import org.jboss.netty.channel.DefaultFileRegion;
 
 import com.google.common.annotations.VisibleForTesting;
@@ -155,8 +158,7 @@ public void transferSuccessful() {
     if (manageOsCache && getCount() > 0) {
       try {
         NativeIO.POSIX.getCacheManipulator().posixFadviseIfPossible(identifier,
           fd, getPosition(), getCount(),
           NativeIO.POSIX.POSIX_FADV_DONTNEED);
            fd, getPosition(), getCount(), POSIX_FADV_DONTNEED);
       } catch (Throwable t) {
         LOG.warn("Failed to manage OS cache for " + identifier, t);
       }
- 
2.19.1.windows.1

