From 28023b77595991fe3be590a929b7d162556f1d4a Mon Sep 17 00:00:00 2001
From: Harsh J <harsh@apache.org>
Date: Sat, 22 Sep 2012 20:34:16 +0000
Subject: [PATCH] HADOOP-7256. Resource leak during failure scenario of closing
 of resources. Contributed by Ramkrishna S. Vasudevan. (harsh)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1388893 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../java/org/apache/hadoop/io/IOUtils.java    |  5 ++-
 .../org/apache/hadoop/io/TestIOUtils.java     | 37 +++++++++++++++++--
 3 files changed, 41 insertions(+), 4 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index b049af43232..119d5ae55ab 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -215,6 +215,9 @@ Trunk (Unreleased)
     HADOOP-8821. Fix findbugs warning related to concatenating string in a 
     for loop in Configuration#dumpDeprecatedKeys(). (suresh)
 
    HADOOP-7256. Resource leak during failure scenario of closing
    of resources. (Ramkrishna S. Vasudevan via harsh)

   OPTIMIZATIONS
 
     HADOOP-7761. Improve the performance of raw comparisons. (todd)
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java
index a3315a869e4..6be7446c994 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/IOUtils.java
@@ -25,6 +25,7 @@
 import java.nio.channels.WritableByteChannel;
 
 import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
@@ -36,6 +37,7 @@
 @InterfaceAudience.Public
 @InterfaceStability.Evolving
 public class IOUtils {
  public static final Log LOG = LogFactory.getLog(IOUtils.class);
 
   /**
    * Copies from one stream to another.
@@ -235,7 +237,7 @@ public static void cleanup(Log log, java.io.Closeable... closeables) {
       if (c != null) {
         try {
           c.close();
        } catch(IOException e) {
        } catch(Throwable e) {
           if (log != null && log.isDebugEnabled()) {
             log.debug("Exception in closing " + c, e);
           }
@@ -264,6 +266,7 @@ public static void closeSocket(Socket sock) {
       try {
         sock.close();
       } catch (IOException ignored) {
        LOG.debug("Ignoring exception while closing socket", ignored);
       }
     }
   }
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/TestIOUtils.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/TestIOUtils.java
index b78b1ea9f31..52aa1ccd6d2 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/TestIOUtils.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/io/TestIOUtils.java
@@ -66,6 +66,36 @@ public void testCopyBytesShouldCloseInputSteamWhenOutputStreamCloseThrowsExcepti
     Mockito.verify(outputStream, Mockito.atLeastOnce()).close();
   }
 
  @Test
  public void testCopyBytesShouldCloseInputSteamWhenOutputStreamCloseThrowsRunTimeException()
      throws Exception {
    InputStream inputStream = Mockito.mock(InputStream.class);
    OutputStream outputStream = Mockito.mock(OutputStream.class);
    Mockito.doReturn(-1).when(inputStream).read(new byte[1]);
    Mockito.doThrow(new RuntimeException()).when(outputStream).close();
    try {
      IOUtils.copyBytes(inputStream, outputStream, 1, true);
      fail("Didn't throw exception");
    } catch (RuntimeException e) {
    }
    Mockito.verify(outputStream, Mockito.atLeastOnce()).close();
  }

  @Test
  public void testCopyBytesShouldCloseInputSteamWhenInputStreamCloseThrowsRunTimeException()
      throws Exception {
    InputStream inputStream = Mockito.mock(InputStream.class);
    OutputStream outputStream = Mockito.mock(OutputStream.class);
    Mockito.doReturn(-1).when(inputStream).read(new byte[1]);
    Mockito.doThrow(new RuntimeException()).when(inputStream).close();
    try {
      IOUtils.copyBytes(inputStream, outputStream, 1, true);
      fail("Didn't throw exception");
    } catch (RuntimeException e) {
    }
    Mockito.verify(inputStream, Mockito.atLeastOnce()).close();
  }

   @Test
   public void testCopyBytesShouldNotCloseStreamsWhenCloseIsFalse()
       throws Exception {
@@ -76,7 +106,7 @@ public void testCopyBytesShouldNotCloseStreamsWhenCloseIsFalse()
     Mockito.verify(inputStream, Mockito.atMost(0)).close();
     Mockito.verify(outputStream, Mockito.atMost(0)).close();
   }
  

   @Test
   public void testCopyBytesWithCountShouldCloseStreamsWhenCloseIsTrue()
       throws Exception {
@@ -117,7 +147,7 @@ public void testCopyBytesWithCountShouldThrowOutTheStreamClosureExceptions()
     Mockito.verify(inputStream, Mockito.atLeastOnce()).close();
     Mockito.verify(outputStream, Mockito.atLeastOnce()).close();
   }
  

   @Test
   public void testWriteFully() throws IOException {
     final int INPUT_BUFFER_LEN = 10000;
@@ -148,6 +178,7 @@ public void testWriteFully() throws IOException {
       for (int i = HALFWAY; i < input.length; i++) {
         assertEquals(input[i - HALFWAY], output[i]);
       }
      raf.close();
     } finally {
       File f = new File(TEST_FILE_NAME);
       if (f.exists()) {
@@ -177,7 +208,7 @@ public void testWrappedReadForCompressedData() throws IOException {
           "Error while reading compressed data", ioe);
     }
   }
  

   @Test
   public void testSkipFully() throws IOException {
     byte inArray[] = new byte[] {0, 1, 2, 3, 4};
- 
2.19.1.windows.1

