From 6d9820ab3d6d7ff025fca7c218bfd951ba859775 Mon Sep 17 00:00:00 2001
From: Tsz-wo Sze <szetszwo@apache.org>
Date: Mon, 4 Jul 2011 02:39:14 +0000
Subject: [PATCH] HADOOP-7437. IOUtils.copybytes will suppress the stream
 closure exceptions.  Contributed by Uma Maheswara Rao G

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1142535 13f79535-47bb-0310-9956-ffa450edef68
--
 common/CHANGES.txt                            |  3 ++
 .../java/org/apache/hadoop/io/IOUtils.java    |  6 +++
 .../org/apache/hadoop/io/TestIOUtils.java     | 45 +++++++++++++++++++
 3 files changed, 54 insertions(+)

diff --git a/common/CHANGES.txt b/common/CHANGES.txt
index 4bdc0a720dc..69f961c115e 100644
-- a/common/CHANGES.txt
++ b/common/CHANGES.txt
@@ -340,6 +340,9 @@ Trunk (unreleased changes)
     HADOOP-7428. IPC connection is orphaned with null 'out' member.
     (todd via eli)
 
    HADOOP-7437. IOUtils.copybytes will suppress the stream closure exceptions.
    (Uma Maheswara Rao G via szetszwo)

 Release 0.22.0 - Unreleased
 
   INCOMPATIBLE CHANGES
diff --git a/common/src/java/org/apache/hadoop/io/IOUtils.java b/common/src/java/org/apache/hadoop/io/IOUtils.java
index c60b95befec..f5875d8d96b 100644
-- a/common/src/java/org/apache/hadoop/io/IOUtils.java
++ b/common/src/java/org/apache/hadoop/io/IOUtils.java
@@ -136,6 +136,12 @@ public static void copyBytes(InputStream in, OutputStream out, long count,
         out.write(buf, 0, bytesRead);
         bytesRemaining -= bytesRead;
       }
      if (close) {
        out.close();
        out = null;
        in.close();
        in = null;
      }
     } finally {
       if (close) {
         closeStream(out);
diff --git a/common/src/test/core/org/apache/hadoop/io/TestIOUtils.java b/common/src/test/core/org/apache/hadoop/io/TestIOUtils.java
index 44d03ea4538..d4f5057f7ce 100644
-- a/common/src/test/core/org/apache/hadoop/io/TestIOUtils.java
++ b/common/src/test/core/org/apache/hadoop/io/TestIOUtils.java
@@ -18,6 +18,9 @@
 
 package org.apache.hadoop.io;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
@@ -65,4 +68,46 @@ public void testCopyBytesShouldNotCloseStreamsWhenCloseIsFalse()
     Mockito.verify(inputStream, Mockito.atMost(0)).close();
     Mockito.verify(outputStream, Mockito.atMost(0)).close();
   }
  
  @Test
  public void testCopyBytesWithCountShouldCloseStreamsWhenCloseIsTrue()
      throws Exception {
    InputStream inputStream = Mockito.mock(InputStream.class);
    OutputStream outputStream = Mockito.mock(OutputStream.class);
    Mockito.doReturn(-1).when(inputStream).read(new byte[4096], 0, 1);
    IOUtils.copyBytes(inputStream, outputStream, (long) 1, true);
    Mockito.verify(inputStream, Mockito.atLeastOnce()).close();
    Mockito.verify(outputStream, Mockito.atLeastOnce()).close();
  }

  @Test
  public void testCopyBytesWithCountShouldNotCloseStreamsWhenCloseIsFalse()
      throws Exception {
    InputStream inputStream = Mockito.mock(InputStream.class);
    OutputStream outputStream = Mockito.mock(OutputStream.class);
    Mockito.doReturn(-1).when(inputStream).read(new byte[4096], 0, 1);
    IOUtils.copyBytes(inputStream, outputStream, (long) 1, false);
    Mockito.verify(inputStream, Mockito.atMost(0)).close();
    Mockito.verify(outputStream, Mockito.atMost(0)).close();
  }

  @Test
  public void testCopyBytesWithCountShouldThrowOutTheStreamClosureExceptions()
      throws Exception {
    InputStream inputStream = Mockito.mock(InputStream.class);
    OutputStream outputStream = Mockito.mock(OutputStream.class);
    Mockito.doReturn(-1).when(inputStream).read(new byte[4096], 0, 1);
    Mockito.doThrow(new IOException("Exception in closing the stream")).when(
        outputStream).close();
    try {
      IOUtils.copyBytes(inputStream, outputStream, (long) 1, true);
      fail("Should throw out the exception");
    } catch (IOException e) {
      assertEquals("Not throwing the expected exception.",
          "Exception in closing the stream", e.getMessage());
    }
    Mockito.verify(inputStream, Mockito.atLeastOnce()).close();
    Mockito.verify(outputStream, Mockito.atLeastOnce()).close();
  }
  
 }
- 
2.19.1.windows.1

