From 9ba7a8e612e933da24c4eae0f927bf2fb6af32b0 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Wed, 22 Nov 2006 02:47:49 +0000
Subject: [PATCH] RAMDirectory.sizeInBytes, public flushRamSegments: LUCENE-709

git-svn-id: https://svn.apache.org/repos/asf/lucene/java/trunk@478014 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |   6 +
 .../org/apache/lucene/index/IndexWriter.java  |  24 ++-
 .../org/apache/lucene/store/RAMDirectory.java | 159 ++++++++++++------
 src/java/org/apache/lucene/store/RAMFile.java |  58 ++++++-
 .../apache/lucene/store/RAMInputStream.java   |   2 +-
 .../apache/lucene/store/RAMOutputStream.java  |  18 +-
 .../lucene/index/store/TestRAMDirectory.java  |  54 +++++-
 7 files changed, 253 insertions(+), 68 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index ba576ea44c1..ff7fc961307 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -67,6 +67,12 @@ New features
     characters in terms via a unicode escape of the form \uXXXX
     (Michael Busch via Yonik Seeley)
 
 9. LUCENE-709: Added RAMDirectory.sizeInBytes(), IndexWriter.ramSizeInBytes()
    and IndexWriter.flushRamSegments(), allowing applications to
    control the amount of memory used to buffer documents.
    (Chuck Williams via Yonik Seeley)


 API Changes
 
  1. LUCENE-438: Remove "final" from Token, implement Cloneable, allow
diff --git a/src/java/org/apache/lucene/index/IndexWriter.java b/src/java/org/apache/lucene/index/IndexWriter.java
index d02c9435a68..167249deaef 100644
-- a/src/java/org/apache/lucene/index/IndexWriter.java
++ b/src/java/org/apache/lucene/index/IndexWriter.java
@@ -99,9 +99,9 @@ public class IndexWriter {
 
   private Similarity similarity = Similarity.getDefault(); // how to normalize
 
  private SegmentInfos segmentInfos = new SegmentInfos(); // the segments
  private SegmentInfos ramSegmentInfos = new SegmentInfos(); // the segments in ramDirectory
  private final Directory ramDirectory = new RAMDirectory(); // for temp segs
  private SegmentInfos segmentInfos = new SegmentInfos();       // the segments
  private SegmentInfos ramSegmentInfos = new SegmentInfos();    // the segments in ramDirectory
  private final RAMDirectory ramDirectory = new RAMDirectory(); // for temp segs
   private IndexFileDeleter deleter;
 
   private Lock writeLock;
@@ -827,14 +827,28 @@ public class IndexWriter {
     }
   }
 
  /** Merges all RAM-resident segments, then may merge segments. */
  private final void flushRamSegments() throws IOException {
  /** Expert:  Flushes all RAM-resident segments (buffered documents), then may merge segments. */
  public final synchronized void flushRamSegments() throws IOException {
     if (ramSegmentInfos.size() > 0) {
       mergeSegments(ramSegmentInfos, 0, ramSegmentInfos.size());
       maybeMergeSegments(minMergeDocs);
     }
   }
 
  /** Expert:  Return the total size of all index files currently cached in memory.
   * Useful for size management with flushRamDocs()
   */
  public final long ramSizeInBytes() {
    return ramDirectory.sizeInBytes();
  }

  /** Expert:  Return the number of documents whose segments are currently cached in memory.
   * Useful when calling flushRamSegments()
   */
  public final synchronized int numRamDocs() {
    return ramSegmentInfos.size();
  }
  
   /** Incremental segment merger.  */
   private final void maybeMergeSegments(int startUpperBound) throws IOException {
     long lowerBound = -1;
diff --git a/src/java/org/apache/lucene/store/RAMDirectory.java b/src/java/org/apache/lucene/store/RAMDirectory.java
index 9d5aa94ca37..dd908278a8a 100644
-- a/src/java/org/apache/lucene/store/RAMDirectory.java
++ b/src/java/org/apache/lucene/store/RAMDirectory.java
@@ -21,12 +21,11 @@ import java.io.IOException;
 import java.io.FileNotFoundException;
 import java.io.File;
 import java.io.Serializable;
import java.util.Hashtable;
import java.util.Collection;
 import java.util.Enumeration;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
 
 /**
  * A memory-resident {@link Directory} implementation.  Locking
@@ -39,7 +38,14 @@ public final class RAMDirectory extends Directory implements Serializable {
 
   private static final long serialVersionUID = 1l;
 
  Hashtable files = new Hashtable();
  private HashMap fileMap = new HashMap();
  private Set fileNames = fileMap.keySet();
  private Collection files = fileMap.values();
  long sizeInBytes = 0;
  
  // *****
  // Lock acquisition sequence:  RAMDirectory, then RAMFile
  // *****
 
   /** Constructs an empty {@link Directory}. */
   public RAMDirectory() {
@@ -107,85 +113,144 @@ public final class RAMDirectory extends Directory implements Serializable {
 
   /** Returns an array of strings, one for each file in the directory. */
   public synchronized final String[] list() {
    String[] result = new String[files.size()];
    String[] result = new String[fileNames.size()];
     int i = 0;
    Enumeration names = files.keys();
    while (names.hasMoreElements())
      result[i++] = (String)names.nextElement();
    Iterator it = fileNames.iterator();
    while (it.hasNext())
      result[i++] = (String)it.next();
     return result;
   }
 
   /** Returns true iff the named file exists in this directory. */
   public final boolean fileExists(String name) {
    RAMFile file = (RAMFile)files.get(name);
    RAMFile file;
    synchronized (this) {
      file = (RAMFile)fileMap.get(name);
    }
     return file != null;
   }
 
  /** Returns the time the named file was last modified. */
  public final long fileModified(String name) {
    RAMFile file = (RAMFile)files.get(name);
    return file.lastModified;
  /** Returns the time the named file was last modified.
   * @throws IOException if the file does not exist
   */
  public final long fileModified(String name) throws IOException {
    RAMFile file;
    synchronized (this) {
      file = (RAMFile)fileMap.get(name);
    }
    if (file==null)
      throw new FileNotFoundException(name);
    return file.getLastModified();
   }
 
  /** Set the modified time of an existing file to now. */
  public void touchFile(String name) {
//     final boolean MONITOR = false;

    RAMFile file = (RAMFile)files.get(name);
  /** Set the modified time of an existing file to now.
   * @throws IOException if the file does not exist
   */
  public void touchFile(String name) throws IOException {
    RAMFile file;
    synchronized (this) {
      file = (RAMFile)fileMap.get(name);
    }
    if (file==null)
      throw new FileNotFoundException(name);
    
     long ts2, ts1 = System.currentTimeMillis();
     do {
       try {
         Thread.sleep(0, 1);
       } catch (InterruptedException e) {}
       ts2 = System.currentTimeMillis();
//       if (MONITOR) {
//         count++;
//       }
     } while(ts1 == ts2);

    file.lastModified = ts2;

//     if (MONITOR)
//         System.out.println("SLEEP COUNT: " + count);
    
    file.setLastModified(ts2);
   }
 
  /** Returns the length in bytes of a file in the directory. */
  public final long fileLength(String name) {
    RAMFile file = (RAMFile)files.get(name);
    return file.length;
  /** Returns the length in bytes of a file in the directory.
   * @throws IOException if the file does not exist
   */
  public final long fileLength(String name) throws IOException {
    RAMFile file;
    synchronized (this) {
      file = (RAMFile)fileMap.get(name);
    }
    if (file==null)
      throw new FileNotFoundException(name);
    return file.getLength();
  }
  
  /** Return total size in bytes of all files in this directory */
  public synchronized final long sizeInBytes() {
    return sizeInBytes;
  }
  
  /** Provided for testing purposes.  Use sizeInBytes() instead. */
  public synchronized final long getRecomputedSizeInBytes() {
    long size = 0;
    Iterator it = files.iterator();
    while (it.hasNext())
      size += ((RAMFile) it.next()).getSizeInBytes();
    return size;
   }
 
  /** Removes an existing file in the directory. */
  public final void deleteFile(String name) {
    files.remove(name);
  /** Removes an existing file in the directory.
   * @throws IOException if the file does not exist
   */
  public synchronized final void deleteFile(String name) throws IOException {
    RAMFile file = (RAMFile)fileMap.get(name);
    if (file!=null) {
        fileMap.remove(name);
        file.directory = null;
        sizeInBytes -= file.sizeInBytes;       // updates to RAMFile.sizeInBytes synchronized on directory
    } else
      throw new FileNotFoundException(name);
   }
 
  /** Removes an existing file in the directory. */
  public final void renameFile(String from, String to) {
    RAMFile file = (RAMFile)files.get(from);
    files.remove(from);
    files.put(to, file);
  /** Removes an existing file in the directory.
   * @throws IOException if from does not exist
   */
  public synchronized final void renameFile(String from, String to) throws IOException {
    RAMFile fromFile = (RAMFile)fileMap.get(from);
    if (fromFile==null)
      throw new FileNotFoundException(from);
    RAMFile toFile = (RAMFile)fileMap.get(to);
    if (toFile!=null) {
      sizeInBytes -= toFile.sizeInBytes;       // updates to RAMFile.sizeInBytes synchronized on directory
      toFile.directory = null;
    }
    fileMap.remove(from);
    fileMap.put(to, fromFile);
   }
 
  /** Creates a new, empty file in the directory with the given name.
      Returns a stream writing this file. */
  /** Creates a new, empty file in the directory with the given name. Returns a stream writing this file. */
   public final IndexOutput createOutput(String name) {
    RAMFile file = new RAMFile();
    files.put(name, file);
    RAMFile file = new RAMFile(this);
    synchronized (this) {
      RAMFile existing = (RAMFile)fileMap.get(name);
      if (existing!=null) {
        sizeInBytes -= existing.sizeInBytes;
        existing.directory = null;
      }
      fileMap.put(name, file);
    }
     return new RAMOutputStream(file);
   }
 
   /** Returns a stream reading an existing file. */
   public final IndexInput openInput(String name) throws IOException {
    RAMFile file = (RAMFile)files.get(name);
    if (file == null) {
      throw new FileNotFoundException(name);
    RAMFile file;
    synchronized (this) {
      file = (RAMFile)fileMap.get(name);
     }
    if (file == null)
      throw new FileNotFoundException(name);
     return new RAMInputStream(file);
   }
 
   /** Closes the store to future operations, releasing associated memory. */
   public final void close() {
    fileMap = null;
    fileNames = null;
     files = null;
   }

 }
diff --git a/src/java/org/apache/lucene/store/RAMFile.java b/src/java/org/apache/lucene/store/RAMFile.java
index d66d91d896c..9e408746a1d 100644
-- a/src/java/org/apache/lucene/store/RAMFile.java
++ b/src/java/org/apache/lucene/store/RAMFile.java
@@ -17,14 +17,66 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.util.Vector;
import java.util.ArrayList;
 import java.io.Serializable;
 
 class RAMFile implements Serializable {
 
   private static final long serialVersionUID = 1l;
 
  Vector buffers = new Vector();
  // Direct read-only access to state supported for streams since a writing stream implies no other concurrent streams
  ArrayList buffers = new ArrayList();
   long length;
  long lastModified = System.currentTimeMillis();
  RAMDirectory directory;
  long sizeInBytes;                  // Only maintained if in a directory; updates synchronized on directory

  // This is publicly modifiable via Directory.touchFile(), so direct access not supported
  private long lastModified = System.currentTimeMillis();

  // File used as buffer, in no RAMDirectory
  RAMFile() {}
  
  RAMFile(RAMDirectory directory) {
    this.directory = directory;
  }

  // For non-stream access from thread that might be concurrent with writing
  synchronized long getLength() {
    return length;
  }

  synchronized void setLength(long length) {
    this.length = length;
  }

  // For non-stream access from thread that might be concurrent with writing
  synchronized long getLastModified() {
    return lastModified;
  }

  synchronized void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }

  // Only one writing stream with no concurrent reading streams, so no file synchronization required
  final byte[] addBuffer(int size) {
    byte[] buffer = new byte[size];
    if (directory!=null)
      synchronized (directory) {             // Ensure addition of buffer and adjustment to directory size are atomic wrt directory
        buffers.add(buffer);
        directory.sizeInBytes += size;
        sizeInBytes += size;
      }
    else
      buffers.add(buffer);
    return buffer;
  }

  // Only valid if in a directory
  long getSizeInBytes() {
    synchronized (directory) {
      return sizeInBytes;
    }
  }
  
 }
diff --git a/src/java/org/apache/lucene/store/RAMInputStream.java b/src/java/org/apache/lucene/store/RAMInputStream.java
index c4acb567faf..34f9d0695de 100644
-- a/src/java/org/apache/lucene/store/RAMInputStream.java
++ b/src/java/org/apache/lucene/store/RAMInputStream.java
@@ -41,7 +41,7 @@ class RAMInputStream extends BufferedIndexInput implements Cloneable {
       int bufferOffset = (int)(start%BUFFER_SIZE);
       int bytesInBuffer = BUFFER_SIZE - bufferOffset;
       int bytesToCopy = bytesInBuffer >= remainder ? remainder : bytesInBuffer;
      byte[] buffer = (byte[])file.buffers.elementAt(bufferNumber);
      byte[] buffer = (byte[])file.buffers.get(bufferNumber);
       System.arraycopy(buffer, bufferOffset, dest, destOffset, bytesToCopy);
       destOffset += bytesToCopy;
       start += bytesToCopy;
diff --git a/src/java/org/apache/lucene/store/RAMOutputStream.java b/src/java/org/apache/lucene/store/RAMOutputStream.java
index f1d76afe520..8ccd8f70fb1 100644
-- a/src/java/org/apache/lucene/store/RAMOutputStream.java
++ b/src/java/org/apache/lucene/store/RAMOutputStream.java
@@ -50,7 +50,7 @@ public class RAMOutputStream extends BufferedIndexOutput {
       if (nextPos > end) {                        // at the last buffer
         length = (int)(end - pos);
       }
      out.writeBytes((byte[])file.buffers.elementAt(buffer++), length);
      out.writeBytes((byte[])file.buffers.get(buffer++), length);
       pos = nextPos;
     }
   }
@@ -63,7 +63,7 @@ public class RAMOutputStream extends BufferedIndexOutput {
       throw new RuntimeException(e.toString());
     }
 
    file.length = 0;
    file.setLength(0);
   }
 
   public void flushBuffer(byte[] src, int len) {
@@ -76,12 +76,10 @@ public class RAMOutputStream extends BufferedIndexOutput {
       int remainInSrcBuffer = len - bufferPos;
       int bytesToCopy = bytesInBuffer >= remainInSrcBuffer ? remainInSrcBuffer : bytesInBuffer;
 
      if (bufferNumber == file.buffers.size()) {
        buffer = new byte[BUFFER_SIZE];
        file.buffers.addElement(buffer);
      } else {
        buffer = (byte[]) file.buffers.elementAt(bufferNumber);
      }
      if (bufferNumber == file.buffers.size())
        buffer = file.addBuffer(BUFFER_SIZE);
      else
        buffer = (byte[]) file.buffers.get(bufferNumber);
 
       System.arraycopy(src, bufferPos, buffer, bufferOffset, bytesToCopy);
       bufferPos += bytesToCopy;
@@ -89,9 +87,9 @@ public class RAMOutputStream extends BufferedIndexOutput {
     }
 
     if (pointer > file.length)
      file.length = pointer;
      file.setLength(pointer);
 
    file.lastModified = System.currentTimeMillis();
    file.setLastModified(System.currentTimeMillis());
   }
 
   public void close() throws IOException {
diff --git a/src/test/org/apache/lucene/index/store/TestRAMDirectory.java b/src/test/org/apache/lucene/index/store/TestRAMDirectory.java
index 75083457cbc..967cfefef55 100644
-- a/src/test/org/apache/lucene/index/store/TestRAMDirectory.java
++ b/src/test/org/apache/lucene/index/store/TestRAMDirectory.java
@@ -64,7 +64,6 @@ public class TestRAMDirectory extends TestCase {
       writer.addDocument(doc);
     }
     assertEquals(docsToAdd, writer.docCount());
    writer.optimize();
     writer.close();
   }
   
@@ -73,9 +72,12 @@ public class TestRAMDirectory extends TestCase {
     Directory dir = FSDirectory.getDirectory(indexDir, false);
     RAMDirectory ramDir = new RAMDirectory(dir);
     
    // close the underlaying directory and delete the index
    // close the underlaying directory
     dir.close();
     
    // Check size
    assertEquals(ramDir.sizeInBytes(), ramDir.getRecomputedSizeInBytes());
    
     // open reader to test document count
     IndexReader reader = IndexReader.open(ramDir);
     assertEquals(docsToAdd, reader.numDocs());
@@ -98,6 +100,9 @@ public class TestRAMDirectory extends TestCase {
     
     RAMDirectory ramDir = new RAMDirectory(indexDir);
     
    // Check size
    assertEquals(ramDir.sizeInBytes(), ramDir.getRecomputedSizeInBytes());
    
     // open reader to test document count
     IndexReader reader = IndexReader.open(ramDir);
     assertEquals(docsToAdd, reader.numDocs());
@@ -120,6 +125,9 @@ public class TestRAMDirectory extends TestCase {
     
     RAMDirectory ramDir = new RAMDirectory(indexDir.getCanonicalPath());
     
    // Check size
    assertEquals(ramDir.sizeInBytes(), ramDir.getRecomputedSizeInBytes());
    
     // open reader to test document count
     IndexReader reader = IndexReader.open(ramDir);
     assertEquals(docsToAdd, reader.numDocs());
@@ -137,6 +145,48 @@ public class TestRAMDirectory extends TestCase {
     reader.close();
     searcher.close();
   }
  
  private final int numThreads = 50;
  private final int docsPerThread = 40;
  
  public void testRAMDirectorySize() throws IOException, InterruptedException {
      
    final RAMDirectory ramDir = new RAMDirectory(indexDir.getCanonicalPath());
    final IndexWriter writer  = new IndexWriter(ramDir, new WhitespaceAnalyzer(), false);
    writer.optimize();
    
    assertEquals(ramDir.sizeInBytes(), ramDir.getRecomputedSizeInBytes());
    
    Thread[] threads = new Thread[numThreads];
    for (int i=0; i<numThreads; i++) {
      final int num = i;
      threads[i] = new Thread(){
        public void run() {
          for (int j=1; j<docsPerThread; j++) {
            Document doc = new Document();
            doc.add(new Field("sizeContent", English.intToEnglish(num*docsPerThread+j).trim(), Field.Store.YES, Field.Index.UN_TOKENIZED));
            try {
              writer.addDocument(doc);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            synchronized (ramDir) {
              assertEquals(ramDir.sizeInBytes(), ramDir.getRecomputedSizeInBytes());
            }
          }
        }
      };
    }
    for (int i=0; i<numThreads; i++)
      threads[i].start();
    for (int i=0; i<numThreads; i++)
      threads[i].join();

    writer.optimize();
    assertEquals(ramDir.sizeInBytes(), ramDir.getRecomputedSizeInBytes());
    
    writer.close();
  }
 
   public void tearDown() {
     // cleanup 
- 
2.19.1.windows.1

