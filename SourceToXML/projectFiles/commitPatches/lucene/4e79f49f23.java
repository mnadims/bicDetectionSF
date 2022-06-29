From 4e79f49f2382ce29b884653c73280772faf1056c Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Sat, 27 Jan 2007 20:39:14 +0000
Subject: [PATCH] LUCENE-785: make RAMDirectory serializable again

git-svn-id: https://svn.apache.org/repos/asf/lucene/java/trunk@500611 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/lucene/store/RAMDirectory.java    |  5 +----
 .../lucene/index/store/TestRAMDirectory.java     | 16 ++++++++++++++++
 .../apache/lucene/store/MockRAMDirectory.java    |  4 ++--
 3 files changed, 19 insertions(+), 6 deletions(-)

diff --git a/src/java/org/apache/lucene/store/RAMDirectory.java b/src/java/org/apache/lucene/store/RAMDirectory.java
index a25dba28f2d..d1640ab4603 100644
-- a/src/java/org/apache/lucene/store/RAMDirectory.java
++ b/src/java/org/apache/lucene/store/RAMDirectory.java
@@ -39,8 +39,6 @@ public class RAMDirectory extends Directory implements Serializable {
   private static final long serialVersionUID = 1l;
 
   HashMap fileMap = new HashMap();
  private Set fileNames = fileMap.keySet();
  Collection files = fileMap.values();
   long sizeInBytes = 0;
   
   // *****
@@ -101,6 +99,7 @@ public class RAMDirectory extends Directory implements Serializable {
 
   /** Returns an array of strings, one for each file in the directory. */
   public synchronized final String[] list() {
    Set fileNames = fileMap.keySet();
     String[] result = new String[fileNames.size()];
     int i = 0;
     Iterator it = fileNames.iterator();
@@ -230,8 +229,6 @@ public class RAMDirectory extends Directory implements Serializable {
   /** Closes the store to future operations, releasing associated memory. */
   public final void close() {
     fileMap = null;
    fileNames = null;
    files = null;
   }
 
 }
diff --git a/src/test/org/apache/lucene/index/store/TestRAMDirectory.java b/src/test/org/apache/lucene/index/store/TestRAMDirectory.java
index 147aad9819b..6bd369063aa 100644
-- a/src/test/org/apache/lucene/index/store/TestRAMDirectory.java
++ b/src/test/org/apache/lucene/index/store/TestRAMDirectory.java
@@ -19,6 +19,10 @@ package org.apache.lucene.index.store;
 
 import java.io.File;
 import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

 
 import junit.framework.TestCase;
 
@@ -190,6 +194,18 @@ public class TestRAMDirectory extends TestCase {
     writer.close();
   }
 

  public void testSerializable() throws IOException {
    Directory dir = new RAMDirectory();
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
    assertEquals("initially empty", 0, bos.size());
    ObjectOutput out = new ObjectOutputStream(bos);
    int headerSize = bos.size();
    out.writeObject(dir);
    out.close();
    assertTrue("contains more then just header", headerSize < bos.size());
  } 

   public void tearDown() {
     // cleanup 
     if (indexDir != null && indexDir.exists()) {
diff --git a/src/test/org/apache/lucene/store/MockRAMDirectory.java b/src/test/org/apache/lucene/store/MockRAMDirectory.java
index 0333526d568..ca1e43f3260 100644
-- a/src/test/org/apache/lucene/store/MockRAMDirectory.java
++ b/src/test/org/apache/lucene/store/MockRAMDirectory.java
@@ -108,7 +108,7 @@ public class MockRAMDirectory extends RAMDirectory {
   /** Provided for testing purposes.  Use sizeInBytes() instead. */
   public synchronized final long getRecomputedSizeInBytes() {
     long size = 0;
    Iterator it = files.iterator();
    Iterator it = fileMap.values().iterator();
     while (it.hasNext())
       size += ((RAMFile) it.next()).getSizeInBytes();
     return size;
@@ -122,7 +122,7 @@ public class MockRAMDirectory extends RAMDirectory {
 
   final long getRecomputedActualSizeInBytes() {
     long size = 0;
    Iterator it = files.iterator();
    Iterator it = fileMap.values().iterator();
     while (it.hasNext())
       size += ((RAMFile) it.next()).length;
     return size;
- 
2.19.1.windows.1

