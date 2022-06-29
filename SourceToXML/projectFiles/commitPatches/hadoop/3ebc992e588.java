From 3ebc992e588c076afd4db0c8f28e9f5f86e93979 Mon Sep 17 00:00:00 2001
From: Eli Collins <eli@apache.org>
Date: Tue, 28 Jun 2011 05:34:20 +0000
Subject: [PATCH] HADOOP-7429. Add another IOUtils#copyBytes method.
 Contributed by Eli Collins

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1140442 13f79535-47bb-0310-9956-ffa450edef68
--
 common/CHANGES.txt                            |  2 +
 .../java/org/apache/hadoop/io/IOUtils.java    | 83 +++++++++++++------
 2 files changed, 61 insertions(+), 24 deletions(-)

diff --git a/common/CHANGES.txt b/common/CHANGES.txt
index eee47e05490..feae1ef333f 100644
-- a/common/CHANGES.txt
++ b/common/CHANGES.txt
@@ -237,6 +237,8 @@ Trunk (unreleased changes)
     HADOOP-310. Additional constructor requested in BytesWritable. (Brock
     Noland via atm)
 
    HADOOP-7429. Add another IOUtils#copyBytes method. (eli)

   OPTIMIZATIONS
   
     HADOOP-7333. Performance improvement in PureJavaCrc32. (Eric Caspole
diff --git a/common/src/java/org/apache/hadoop/io/IOUtils.java b/common/src/java/org/apache/hadoop/io/IOUtils.java
index ca6df1bd7e8..29db28032ba 100644
-- a/common/src/java/org/apache/hadoop/io/IOUtils.java
++ b/common/src/java/org/apache/hadoop/io/IOUtils.java
@@ -36,6 +36,7 @@
 
   /**
    * Copies from one stream to another.
   *
    * @param in InputStrem to read from
    * @param out OutputStream to write to
    * @param buffSize the size of the buffer 
@@ -44,7 +45,6 @@
    */
   public static void copyBytes(InputStream in, OutputStream out, int buffSize, boolean close) 
     throws IOException {

     try {
       copyBytes(in, out, buffSize);
       if(close) {
@@ -70,7 +70,6 @@ public static void copyBytes(InputStream in, OutputStream out, int buffSize, boo
    */
   public static void copyBytes(InputStream in, OutputStream out, int buffSize) 
     throws IOException {

     PrintStream ps = out instanceof PrintStream ? (PrintStream)out : null;
     byte buf[] = new byte[buffSize];
     int bytesRead = in.read(buf);
@@ -82,9 +81,11 @@ public static void copyBytes(InputStream in, OutputStream out, int buffSize)
       bytesRead = in.read(buf);
     }
   }

   /**
    * Copies from one stream to another. <strong>closes the input and output streams 
    * at the end</strong>.
   *
    * @param in InputStrem to read from
    * @param out OutputStream to write to
    * @param conf the Configuration object 
@@ -96,7 +97,8 @@ public static void copyBytes(InputStream in, OutputStream out, Configuration con
   
   /**
    * Copies from one stream to another.
   * @param in InputStrem to read from
   *
   * @param in InputStream to read from
    * @param out OutputStream to write to
    * @param conf the Configuration object
    * @param close whether or not close the InputStream and 
@@ -106,21 +108,50 @@ public static void copyBytes(InputStream in, OutputStream out, Configuration con
     throws IOException {
     copyBytes(in, out, conf.getInt("io.file.buffer.size", 4096),  close);
   }

  /**
   * Copies count bytes from one stream to another.
   *
   * @param in InputStream to read from
   * @param out OutputStream to write to
   * @param count number of bytes to copy
   * @throws IOException if bytes can not be read or written
   */
  public static void copyBytes(InputStream in, OutputStream out, long count)
      throws IOException {
    byte buf[] = new byte[4096];
    long bytesRemaining = count;
    int bytesRead;

    while (bytesRemaining > 0) {
      int bytesToRead = (int)
        (bytesRemaining < buf.length ? bytesRemaining : buf.length);

      bytesRead = in.read(buf, 0, bytesToRead);
      if (bytesRead == -1)
        break;

      out.write(buf, 0, bytesRead);
      bytesRemaining -= bytesRead;
    }
  }
   
  /** Reads len bytes in a loop.
   * @param in The InputStream to read from
  /**
   * Reads len bytes in a loop.
   *
   * @param in InputStream to read from
    * @param buf The buffer to fill
    * @param off offset from the buffer
    * @param len the length of bytes to read
    * @throws IOException if it could not read requested number of bytes 
    * for any reason (including EOF)
    */
  public static void readFully( InputStream in, byte buf[],
      int off, int len ) throws IOException {
  public static void readFully(InputStream in, byte buf[],
      int off, int len) throws IOException {
     int toRead = len;
    while ( toRead > 0 ) {
      int ret = in.read( buf, off, toRead );
      if ( ret < 0 ) {
    while (toRead > 0) {
      int ret = in.read(buf, off, toRead);
      if (ret < 0) {
         throw new IOException( "Premature EOF from inputStream");
       }
       toRead -= ret;
@@ -128,16 +159,17 @@ public static void readFully( InputStream in, byte buf[],
     }
   }
   
  /** Similar to readFully(). Skips bytes in a loop.
  /**
   * Similar to readFully(). Skips bytes in a loop.
    * @param in The InputStream to skip bytes from
    * @param len number of bytes to skip.
    * @throws IOException if it could not skip requested number of bytes 
    * for any reason (including EOF)
    */
  public static void skipFully( InputStream in, long len ) throws IOException {
    while ( len > 0 ) {
      long ret = in.skip( len );
      if ( ret < 0 ) {
  public static void skipFully(InputStream in, long len) throws IOException {
    while (len > 0) {
      long ret = in.skip(len);
      if (ret < 0) {
         throw new IOException( "Premature EOF from inputStream");
       }
       len -= ret;
@@ -147,11 +179,12 @@ public static void skipFully( InputStream in, long len ) throws IOException {
   /**
    * Close the Closeable objects and <b>ignore</b> any {@link IOException} or 
    * null pointers. Must only be used for cleanup in exception handlers.
   *
    * @param log the log to record problems to at debug level. Can be null.
    * @param closeables the objects to close
    */
   public static void cleanup(Log log, java.io.Closeable... closeables) {
    for(java.io.Closeable c : closeables) {
    for (java.io.Closeable c : closeables) {
       if (c != null) {
         try {
           c.close();
@@ -167,27 +200,29 @@ public static void cleanup(Log log, java.io.Closeable... closeables) {
   /**
    * Closes the stream ignoring {@link IOException}.
    * Must only be called in cleaning up from exception handlers.
   *
    * @param stream the Stream to close
    */
  public static void closeStream( java.io.Closeable stream ) {
  public static void closeStream(java.io.Closeable stream) {
     cleanup(null, stream);
   }
   
   /**
   * Closes the socket ignoring {@link IOException} 
   * Closes the socket ignoring {@link IOException}
   *
    * @param sock the Socket to close
    */
  public static void closeSocket( Socket sock ) {
    // avoids try { close() } dance
    if ( sock != null ) {
  public static void closeSocket(Socket sock) {
    if (sock != null) {
       try {
       sock.close();
      } catch ( IOException ignored ) {
        sock.close();
      } catch (IOException ignored) {
       }
     }
   }
   
  /** /dev/null of OutputStreams.
  /**
   * The /dev/null of OutputStreams.
    */
   public static class NullOutputStream extends OutputStream {
     public void write(byte[] b, int off, int len) throws IOException {
- 
2.19.1.windows.1

