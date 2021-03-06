From 7a65a1823ac3fe81f6f8f896cb01095afb9ab2fb Mon Sep 17 00:00:00 2001
From: Shai Erera <shaie@apache.org>
Date: Tue, 13 Apr 2010 14:04:18 +0000
Subject: [PATCH] LUCENE-2386: IndexWriter commits unnecessarily on fresh
 Directory (take #2)

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@933613 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |  6 ++
 .../lucene/TestSnapshotDeletionPolicy.java    |  2 +-
 .../index/TestBackwardsCompatibility.java     |  2 +-
 .../org/apache/lucene/index/TestCrash.java    | 28 ++++---
 .../lucene/index/TestDeletionPolicy.java      | 24 +++---
 .../lucene/index/TestIndexFileDeleter.java    |  6 +-
 .../apache/lucene/index/TestIndexReader.java  |  3 +
 .../lucene/index/TestIndexReaderReopen.java   |  2 +
 .../apache/lucene/index/TestIndexWriter.java  | 21 +++--
 .../lucene/index/TestIndexWriterDelete.java   |  3 +-
 .../index/TestIndexWriterExceptions.java      |  4 +-
 .../lucene/index/TestIndexWriterReader.java   |  2 +
 .../lucene/index/TestStressIndexing.java      |  3 +-
 .../lucene/index/TestStressIndexing2.java     |  2 +
 .../lucene/store/TestBufferedIndexInput.java  |  1 +
 .../apache/lucene/store/TestLockFactory.java  |  3 +-
 .../apache/lucene/store/TestWindowsMMap.java  | 26 +++---
 .../TestNumericRangeFilterBuilder.java        |  4 +-
 .../apache/lucene/index/IndexFileDeleter.java | 29 ++++---
 .../lucene/index/IndexNotFoundException.java  | 32 ++++++++
 .../org/apache/lucene/index/IndexReader.java  |  2 +-
 .../org/apache/lucene/index/IndexWriter.java  | 15 +---
 .../org/apache/lucene/index/SegmentInfos.java |  2 +-
 .../org/apache/lucene/store/Directory.java    |  9 ++-
 .../lucene/TestSnapshotDeletionPolicy.java    |  7 +-
 .../index/TestBackwardsCompatibility.java     |  5 +-
 .../org/apache/lucene/index/TestCrash.java    | 35 +++++---
 .../lucene/index/TestDeletionPolicy.java      | 24 +++---
 .../lucene/index/TestIndexFileDeleter.java    |  6 +-
 .../apache/lucene/index/TestIndexReader.java  |  8 +-
 .../lucene/index/TestIndexReaderReopen.java   |  1 +
 .../apache/lucene/index/TestIndexWriter.java  | 79 +++++++++++++++++--
 .../lucene/index/TestIndexWriterDelete.java   |  2 +-
 .../index/TestIndexWriterExceptions.java      |  2 +
 .../lucene/index/TestIndexWriterReader.java   | 12 +++
 .../lucene/index/TestNoDeletionPolicy.java    |  5 +-
 .../lucene/index/TestStressIndexing.java      |  3 +-
 .../lucene/index/TestStressIndexing2.java     |  1 +
 .../apache/lucene/store/TestLockFactory.java  |  2 +-
 .../apache/lucene/store/TestWindowsMMap.java  | 33 +++++---
 40 files changed, 312 insertions(+), 144 deletions(-)
 create mode 100644 lucene/src/java/org/apache/lucene/index/IndexNotFoundException.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 46cb73c95c2..bd4d9c96301 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -105,6 +105,12 @@ Changes in backwards compatibility policy
   of incrementToken(), tokenStream(), and reusableTokenStream().
   (Uwe Schindler, Robert Muir)
 
* LUCENE-2386: IndexWriter no longer performs an empty commit upon new index
  creation. Previously, if you passed an empty Directory and set OpenMode to
  CREATE*, IndexWriter would make a first empty commit. If you need that 
  behavior you can call writer.commit()/close() immediately after you create it.
  (Shai Erera, Mike McCandless)
  
 Changes in runtime behavior
 
 * LUCENE-1923: Made IndexReader.toString() produce something
diff --git a/lucene/backwards/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java b/lucene/backwards/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java
index 89a57571d9b..02d0227de6b 100644
-- a/lucene/backwards/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java
++ b/lucene/backwards/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java
@@ -113,7 +113,7 @@ public class TestSnapshotDeletionPolicy extends LuceneTestCase
 
     SnapshotDeletionPolicy dp = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
     final IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_CURRENT), dp, IndexWriter.MaxFieldLength.UNLIMITED);

    writer.commit();
     // Force frequent flushes
     writer.setMaxBufferedDocs(2);
 
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java b/lucene/backwards/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
index 07a7668f4ea..bb09f9b4ea9 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
@@ -553,7 +553,7 @@ public class TestBackwardsCompatibility extends LuceneTestCase
       expected = new String[] {"_0.cfs",
                                "_0_1.del",
                                "_0_1.s" + contentFieldIndex,
                               "segments_3",
                               "segments_2",
                                "segments.gen"};
 
       String[] actual = dir.listAll();
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestCrash.java b/lucene/backwards/src/test/org/apache/lucene/index/TestCrash.java
index 09275e263c8..9bedfb68a39 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestCrash.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestCrash.java
@@ -25,20 +25,24 @@ import org.apache.lucene.store.MockRAMDirectory;
 import org.apache.lucene.store.NoLockFactory;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
 
 public class TestCrash extends LuceneTestCase {
 
  private IndexWriter initIndex() throws IOException {
    return initIndex(new MockRAMDirectory());
  private IndexWriter initIndex(boolean initialCommit) throws IOException {
    return initIndex(new MockRAMDirectory(), initialCommit);
   }
 
  private IndexWriter initIndex(MockRAMDirectory dir) throws IOException {
  private IndexWriter initIndex(MockRAMDirectory dir, boolean initialCommit) throws IOException {
     dir.setLockFactory(NoLockFactory.getNoLockFactory());
 
     IndexWriter writer  = new IndexWriter(dir, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
     //writer.setMaxBufferedDocs(2);
     writer.setMaxBufferedDocs(10);
     ((ConcurrentMergeScheduler) writer.getMergeScheduler()).setSuppressExceptions();
    if (initialCommit) {
      writer.commit();
    }
 
     Document doc = new Document();
     doc.add(new Field("content", "aaa", Field.Store.YES, Field.Index.ANALYZED));
@@ -58,7 +62,7 @@ public class TestCrash extends LuceneTestCase {
   }
 
   public void testCrashWhileIndexing() throws IOException {
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(true);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
     crash(writer);
     IndexReader reader = IndexReader.open(dir, false);
@@ -66,11 +70,11 @@ public class TestCrash extends LuceneTestCase {
   }
 
   public void testWriterAfterCrash() throws IOException {
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(true);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
     dir.setPreventDoubleWrite(false);
     crash(writer);
    writer = initIndex(dir);
    writer = initIndex(dir, false);
     writer.close();
 
     IndexReader reader = IndexReader.open(dir, false);
@@ -78,10 +82,10 @@ public class TestCrash extends LuceneTestCase {
   }
 
   public void testCrashAfterReopen() throws IOException {
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
     writer.close();
    writer = initIndex(dir);
    writer = initIndex(dir, false);
     assertEquals(314, writer.maxDoc());
     crash(writer);
 
@@ -100,7 +104,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashAfterClose() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close();
@@ -119,7 +123,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashAfterCloseNoWait() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close(false);
@@ -138,7 +142,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashReaderDeletes() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close(false);
@@ -159,7 +163,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashReaderDeletesAfterClose() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close(false);
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestDeletionPolicy.java b/lucene/backwards/src/test/org/apache/lucene/index/TestDeletionPolicy.java
index c4751ff9817..b7a7fefee3e 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestDeletionPolicy.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestDeletionPolicy.java
@@ -290,7 +290,7 @@ public class TestDeletionPolicy extends LuceneTestCase
       writer.optimize();
       writer.close();
 
      assertEquals(2, policy.numOnInit);
      assertEquals(1, policy.numOnInit);
 
       // If we are not auto committing then there should
       // be exactly 2 commits (one per close above):
@@ -298,8 +298,8 @@ public class TestDeletionPolicy extends LuceneTestCase
 
       // Test listCommits
       Collection commits = IndexReader.listCommits(dir);
      // 1 from opening writer + 2 from closing writer
      assertEquals(3, commits.size());
      // 2 from closing writer
      assertEquals(2, commits.size());
 
       Iterator it = commits.iterator();
       // Make sure we can open a reader on each commit:
@@ -357,7 +357,7 @@ public class TestDeletionPolicy extends LuceneTestCase
     writer.close();
 
     Collection commits = IndexReader.listCommits(dir);
    assertEquals(6, commits.size());
    assertEquals(5, commits.size());
     IndexCommit lastCommit = null;
     Iterator it = commits.iterator();
     while(it.hasNext()) {
@@ -374,7 +374,7 @@ public class TestDeletionPolicy extends LuceneTestCase
     writer.optimize();
     writer.close();
 
    assertEquals(7, IndexReader.listCommits(dir).size());
    assertEquals(6, IndexReader.listCommits(dir).size());
 
     // Now open writer on the commit just before optimize:
     writer = new IndexWriter(dir, new WhitespaceAnalyzer(), policy, IndexWriter.MaxFieldLength.LIMITED, lastCommit);
@@ -395,7 +395,7 @@ public class TestDeletionPolicy extends LuceneTestCase
     writer.close();
 
     // Now 8 because we made another commit
    assertEquals(8, IndexReader.listCommits(dir).size());
    assertEquals(7, IndexReader.listCommits(dir).size());
     
     r = IndexReader.open(dir, true);
     // Not optimized because we rolled it back, and now only
@@ -465,7 +465,7 @@ public class TestDeletionPolicy extends LuceneTestCase
       writer.optimize();
       writer.close();
 
      assertEquals(2, policy.numOnInit);
      assertEquals(1, policy.numOnInit);
       // If we are not auto committing then there should
       // be exactly 2 commits (one per close above):
       assertEquals(2, policy.numOnCommit);
@@ -506,7 +506,7 @@ public class TestDeletionPolicy extends LuceneTestCase
       }
 
       assertTrue(policy.numDelete > 0);
      assertEquals(N+1, policy.numOnInit);
      assertEquals(N, policy.numOnInit);
       assertEquals(N+1, policy.numOnCommit);
 
       // Simplistic check: just verify only the past N segments_N's still
@@ -580,8 +580,8 @@ public class TestDeletionPolicy extends LuceneTestCase
       // this is a commit
       writer.close();
 
      assertEquals(2*(N+2), policy.numOnInit);
      assertEquals(2*(N+2)-1, policy.numOnCommit);
      assertEquals(2*(N+1)+1, policy.numOnInit);
      assertEquals(2*(N+2), policy.numOnCommit);
 
       IndexSearcher searcher = new IndexSearcher(dir, false);
       ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
@@ -678,8 +678,8 @@ public class TestDeletionPolicy extends LuceneTestCase
         writer.close();
       }
 
      assertEquals(1+3*(N+1), policy.numOnInit);
      assertEquals(3*(N+1), policy.numOnCommit);
      assertEquals(3*(N+1), policy.numOnInit);
      assertEquals(3*(N+1)+1, policy.numOnCommit);
 
       IndexSearcher searcher = new IndexSearcher(dir, false);
       ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexFileDeleter.java b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexFileDeleter.java
index 3f9b716715c..a23a2984292 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexFileDeleter.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexFileDeleter.java
@@ -136,11 +136,11 @@ public class TestIndexFileDeleter extends LuceneTestCase
     copyFile(dir, "_0.cfs", "deletable");
 
     // Create some old segments file:
    copyFile(dir, "segments_3", "segments");
    copyFile(dir, "segments_3", "segments_2");
    copyFile(dir, "segments_2", "segments");
    copyFile(dir, "segments_2", "segments_1");
 
     // Create a bogus cfs file shadowing a non-cfs segment:
    copyFile(dir, "_2.cfs", "_3.cfs");
    copyFile(dir, "_1.cfs", "_2.cfs");
 
     String[] filesPre = dir.listAll();
 
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReader.java b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReader.java
index 85dedd0151c..83acfb946a4 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReader.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReader.java
@@ -39,6 +39,7 @@ import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexReader.FieldOption;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.IndexSearcher;
@@ -474,6 +475,7 @@ public class TestIndexReader extends LuceneTestCase
 
         //  add 11 documents with term : aaa
         writer  = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        writer.commit();
         for (int i = 0; i < 11; i++)
         {
             addDoc(writer, searchTerm.text());
@@ -1765,6 +1767,7 @@ public class TestIndexReader extends LuceneTestCase
   public void testPrepareCommitIsCurrent() throws Throwable {
     Directory dir = new MockRAMDirectory();
     IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
    writer.commit();
     Document doc = new Document();
     writer.addDocument(doc);
     IndexReader r = IndexReader.open(dir, true);
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReaderReopen.java b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReaderReopen.java
index 56d97826f8f..1fcf011d026 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReaderReopen.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexReaderReopen.java
@@ -36,6 +36,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Field.Index;
 import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriter.MaxFieldLength;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.ScoreDoc;
@@ -172,6 +173,7 @@ public class TestIndexReaderReopen extends LuceneTestCase {
   private void doTestReopenWithCommit (Directory dir, boolean withReopen) throws IOException {
     IndexWriter iwriter = new IndexWriter(dir, new KeywordAnalyzer(), true, MaxFieldLength.LIMITED);
     iwriter.setMergeScheduler(new SerialMergeScheduler());
    iwriter.commit();
     IndexReader reader = IndexReader.open(dir, false);
     try {
       int M = 3;
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriter.java b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriter.java
index a47ebe66104..b8367c2768b 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriter.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriter.java
@@ -47,6 +47,7 @@ import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.PhraseQuery;
 import org.apache.lucene.search.Query;
@@ -783,7 +784,7 @@ public class TestIndexWriter extends LuceneTestCase {
         writer.close();
 
         long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
        assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
        assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
         // Make the next segments file, with last byte
         // missing, to simulate a writer that crashed while
@@ -843,7 +844,7 @@ public class TestIndexWriter extends LuceneTestCase {
         writer.close();
 
         long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
        assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
        assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
         String fileNameIn = SegmentInfos.getCurrentSegmentFileName(dir);
         String fileNameOut = IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS,
@@ -908,7 +909,7 @@ public class TestIndexWriter extends LuceneTestCase {
         writer.close();
 
         long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
        assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
        assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
         String[] files = dir.listAll();
         for(int i=0;i<files.length;i++) {
@@ -2324,7 +2325,7 @@ public class TestIndexWriter extends LuceneTestCase {
   public void testImmediateDiskFull() throws IOException {
     MockRAMDirectory dir = new MockRAMDirectory();
     IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
    dir.setMaxSizeInBytes(dir.getRecomputedActualSizeInBytes());
    dir.setMaxSizeInBytes(Math.max(1, dir.getRecomputedActualSizeInBytes()));
     writer.setMaxBufferedDocs(2);
     final Document doc = new Document();
     doc.add(new Field("field", "aaa bbb ccc ddd eee fff ggg hhh iii jjj", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
@@ -2647,7 +2648,7 @@ public class TestIndexWriter extends LuceneTestCase {
     writer.close();
 
     long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
    assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
    assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
     final String segmentsFileName = SegmentInfos.getCurrentSegmentFileName(dir);
     IndexInput in = dir.openInput(segmentsFileName);
@@ -2675,7 +2676,8 @@ public class TestIndexWriter extends LuceneTestCase {
     IndexWriter writer  = new IndexWriter(dir, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
     writer.setMaxBufferedDocs(2);
     writer.setMergeFactor(5);

    writer.commit();
    
     for (int i = 0; i < 23; i++)
       addDoc(writer);
 
@@ -3542,7 +3544,8 @@ public class TestIndexWriter extends LuceneTestCase {
     IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
     writer.setMaxBufferedDocs(2);
     writer.setMergeFactor(5);

    writer.commit();
    
     for (int i = 0; i < 23; i++)
       addDoc(writer);
 
@@ -3595,7 +3598,8 @@ public class TestIndexWriter extends LuceneTestCase {
 
     writer.setMaxBufferedDocs(2);
     writer.setMergeFactor(5);

    writer.commit();
    
     for (int i = 0; i < 23; i++)
       addDoc(writer);
 
@@ -3679,6 +3683,7 @@ public class TestIndexWriter extends LuceneTestCase {
 
       dir2 = new MockRAMDirectory();
       writer2 = new IndexWriter(dir2, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
      writer2.commit();
       cms = (ConcurrentMergeScheduler) writer2.getMergeScheduler();
 
       readers = new IndexReader[NUM_COPY];
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterDelete.java b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
index 2a866128cb6..d030d3fa09b 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
@@ -23,6 +23,7 @@ import java.util.Arrays;
 import org.apache.lucene.analysis.WhitespaceAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.TermQuery;
@@ -764,7 +765,7 @@ public class TestIndexWriterDelete extends LuceneTestCase {
     MockRAMDirectory dir = new MockRAMDirectory();
     IndexWriter modifier = new IndexWriter(dir,
                                            new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);

    modifier.commit();
     dir.failOn(failure.reset());
 
     for (int i = 0; i < keywords.length; i++) {
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
index babc13bb4ba..e0edca32d8f 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
@@ -138,7 +138,8 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
     ((ConcurrentMergeScheduler) writer.getMergeScheduler()).setSuppressExceptions();
     //writer.setMaxBufferedDocs(10);
     writer.setRAMBufferSizeMB(0.1);

    writer.commit();
    
     if (DEBUG)
       writer.setInfoStream(System.out);
 
@@ -176,6 +177,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
     ((ConcurrentMergeScheduler) writer.getMergeScheduler()).setSuppressExceptions();
     //writer.setMaxBufferedDocs(10);
     writer.setRAMBufferSizeMB(0.2);
    writer.commit();
 
     if (DEBUG)
       writer.setInfoStream(System.out);
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterReader.java b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterReader.java
index 38be635561d..6c8799dec8c 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterReader.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestIndexWriterReader.java
@@ -30,6 +30,7 @@ import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Field.Index;
 import org.apache.lucene.document.Field.Store;
 import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
@@ -642,6 +643,7 @@ public class TestIndexWriterReader extends LuceneTestCase {
     Directory dir1 = new MockRAMDirectory();
     IndexWriter writer = new IndexWriter(dir1, new WhitespaceAnalyzer(),
                                          IndexWriter.MaxFieldLength.LIMITED);
    writer.commit();
     writer.setInfoStream(infoStream);
 
     // create the index
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing.java b/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing.java
index 01c51d5a825..cc3de2a2090 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing.java
@@ -19,6 +19,7 @@ package org.apache.lucene.index;
 import org.apache.lucene.util.*;
 import org.apache.lucene.store.*;
 import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.analysis.*;
 import org.apache.lucene.search.*;
 import org.apache.lucene.queryParser.*;
@@ -121,7 +122,7 @@ public class TestStressIndexing extends LuceneTestCase {
   */
   public void runStressTest(Directory directory, MergeScheduler mergeScheduler) throws Exception {
     IndexWriter modifier = new IndexWriter(directory, ANALYZER, true, IndexWriter.MaxFieldLength.UNLIMITED);

    modifier.commit();
     modifier.setMaxBufferedDocs(10);
 
     TimedThread[] threads = new TimedThread[4];
diff --git a/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing2.java b/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing2.java
index 0f02bcdaeb5..0d7a8dabae3 100644
-- a/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing2.java
++ b/lucene/backwards/src/test/org/apache/lucene/index/TestStressIndexing2.java
@@ -16,6 +16,7 @@ package org.apache.lucene.index;
 
 import org.apache.lucene.store.*;
 import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.analysis.*;
 
 import org.apache.lucene.util.LuceneTestCase;
@@ -124,6 +125,7 @@ public class TestStressIndexing2 extends LuceneTestCase {
   public DocsAndWriter indexRandomIWReader(int nThreads, int iterations, int range, Directory dir) throws IOException, InterruptedException {
     Map docs = new HashMap();
     IndexWriter w = new MockIndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
    w.commit();
     w.setUseCompoundFile(false);
 
     /***
diff --git a/lucene/backwards/src/test/org/apache/lucene/store/TestBufferedIndexInput.java b/lucene/backwards/src/test/org/apache/lucene/store/TestBufferedIndexInput.java
index def32daddb6..e89a4f7e075 100755
-- a/lucene/backwards/src/test/org/apache/lucene/store/TestBufferedIndexInput.java
++ b/lucene/backwards/src/test/org/apache/lucene/store/TestBufferedIndexInput.java
@@ -241,6 +241,7 @@ public class TestBufferedIndexInput extends LuceneTestCase {
 
     public void testSetBufferSize() throws IOException {
       File indexDir = new File(System.getProperty("tempDir"), "testSetBufferSize");
      indexDir.mkdirs(); // required for this MockFSDir since we don't commit on IW creation anymore.
       MockFSDirectory dir = new MockFSDirectory(indexDir, newRandom());
       try {
         IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
diff --git a/lucene/backwards/src/test/org/apache/lucene/store/TestLockFactory.java b/lucene/backwards/src/test/org/apache/lucene/store/TestLockFactory.java
index 75215840b09..37bb83e3772 100755
-- a/lucene/backwards/src/test/org/apache/lucene/store/TestLockFactory.java
++ b/lucene/backwards/src/test/org/apache/lucene/store/TestLockFactory.java
@@ -85,7 +85,8 @@ public class TestLockFactory extends LuceneTestCase {
 
         IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true,
                                              IndexWriter.MaxFieldLength.LIMITED);

        writer.commit(); // required so the second open succeed
        
         // Create a 2nd IndexWriter.  This is normally not allowed but it should run through since we're not
         // using any locks:
         IndexWriter writer2 = null;
diff --git a/lucene/backwards/src/test/org/apache/lucene/store/TestWindowsMMap.java b/lucene/backwards/src/test/org/apache/lucene/store/TestWindowsMMap.java
index 45aa61b0b4d..d1e54410136 100644
-- a/lucene/backwards/src/test/org/apache/lucene/store/TestWindowsMMap.java
++ b/lucene/backwards/src/test/org/apache/lucene/store/TestWindowsMMap.java
@@ -64,14 +64,18 @@ public class TestWindowsMMap extends LuceneTestCase {
     new File(System.getProperty("tempDir"),"testLuceneMmap").getAbsolutePath();
 
   public void testMmapIndex() throws Exception {
    FSDirectory storeDirectory;
    storeDirectory = new MMapDirectory(new File(storePathname), null);
    // sometimes the directory is not cleaned by rmDir, because on Windows it
    // may take some time until the files are finally dereferenced. So clean the
    // directory up front, or otherwise new IndexWriter will fail.
    rmDir(new File(storePathname));
    FSDirectory storeDirectory = new MMapDirectory(new File(storePathname), null);
 
     // plan to add a set of useful stopwords, consider changing some of the
     // interior filters.
     StandardAnalyzer analyzer = new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_CURRENT, Collections.emptySet());
     // TODO: something about lock timeouts and leftover locks.
     IndexWriter writer = new IndexWriter(storeDirectory, analyzer, true, IndexWriter.MaxFieldLength.LIMITED);
    writer.commit();
     IndexSearcher searcher = new IndexSearcher(storeDirectory, true);
     
     for(int dx = 0; dx < 1000; dx ++) {
@@ -83,14 +87,16 @@ public class TestWindowsMMap extends LuceneTestCase {
     
     searcher.close();
     writer.close();
                rmDir(new File(storePathname));
    rmDir(new File(storePathname));
   }
 
        private void rmDir(File dir) {
          File[] files = dir.listFiles();
          for (int i = 0; i < files.length; i++) {
            files[i].delete();
          }
          dir.delete();
        }
  private void rmDir(File dir) {
    if (!dir.exists()) {
      return;
    }
    for (File file : dir.listFiles()) {
      file.delete();
    }
    dir.delete();
  }
 }
diff --git a/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java b/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
index 5e7fe7b7ffc..b5f4915f851 100644
-- a/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
++ b/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
@@ -29,6 +29,7 @@ import org.apache.lucene.util.LuceneTestCase;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.IndexWriter.MaxFieldLength;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.NumericRangeFilter;
@@ -62,7 +63,8 @@ public class TestNumericRangeFilterBuilder extends LuceneTestCase {
 		Filter filter = filterBuilder.getFilter(doc.getDocumentElement());
 
 		RAMDirectory ramDir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(ramDir, null, MaxFieldLength.UNLIMITED);
		IndexWriter writer = new IndexWriter(ramDir, new IndexWriterConfig(TEST_VERSION_CURRENT, null));
		writer.commit();
 		try
 		{
 			IndexReader reader = IndexReader.open(ramDir, true);
diff --git a/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java b/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java
index 5059435bad5..aa82f565d25 100644
-- a/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java
++ b/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java
@@ -31,6 +31,7 @@ import java.util.Map;
 
 import org.apache.lucene.index.codecs.CodecProvider;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoSuchDirectoryException;
 
 /*
  * This class keeps track of each SegmentInfos instance that
@@ -144,16 +145,21 @@ final class IndexFileDeleter {
     long currentGen = segmentInfos.getGeneration();
     indexFilenameFilter = new IndexFileNameFilter(codecs);
     
    String[] files = directory.listAll();

     CommitPoint currentCommitPoint = null;
    boolean seenIndexFiles = false;
    String[] files = null;
    try {
      files = directory.listAll();
    } catch (NoSuchDirectoryException e) {  
      // it means the directory is empty, so ignore it.
      files = new String[0];
    }
 
    for(int i=0;i<files.length;i++) {

      String fileName = files[i];
    for (String fileName : files) {
 
       if ((indexFilenameFilter.accept(null, fileName)) && !fileName.endsWith("write.lock") && !fileName.equals(IndexFileNames.SEGMENTS_GEN)) {

        seenIndexFiles = true;
        
         // Add this file to refCounts with initial count 0:
         getRefCount(fileName);
 
@@ -195,7 +201,10 @@ final class IndexFileDeleter {
       }
     }
 
    if (currentCommitPoint == null) {
    // If we haven't seen any Lucene files, then currentCommitPoint is expected
    // to be null, because it means it's a fresh Directory. Therefore it cannot
    // be any NFS cache issues - so just ignore.
    if (currentCommitPoint == null && seenIndexFiles) {
       // We did not in fact see the segments_N file
       // corresponding to the segmentInfos that was passed
       // in.  Yet, it must exist, because our caller holds
@@ -235,13 +244,15 @@ final class IndexFileDeleter {
 
     // Finally, give policy a chance to remove things on
     // startup:
    policy.onInit(commits);
    if (seenIndexFiles) {
      policy.onInit(commits);
    }
 
     // Always protect the incoming segmentInfos since
     // sometime it may not be the most recent commit
     checkpoint(segmentInfos, false);
     
    startingCommitDeleted = currentCommitPoint.isDeleted();
    startingCommitDeleted = currentCommitPoint == null ? false : currentCommitPoint.isDeleted();
 
     deleteCommits();
   }
diff --git a/lucene/src/java/org/apache/lucene/index/IndexNotFoundException.java b/lucene/src/java/org/apache/lucene/index/IndexNotFoundException.java
new file mode 100644
index 00000000000..5e7107448b8
-- /dev/null
++ b/lucene/src/java/org/apache/lucene/index/IndexNotFoundException.java
@@ -0,0 +1,32 @@
package org.apache.lucene.index;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FileNotFoundException;

/**
 * Signals that no index was found in the Directory. Possibly because the
 * directory is empty, however can slso indicate an index corruption.
 */
public final class IndexNotFoundException extends FileNotFoundException {

  public IndexNotFoundException(String msg) {
    super(msg);
  }

}
diff --git a/lucene/src/java/org/apache/lucene/index/IndexReader.java b/lucene/src/java/org/apache/lucene/index/IndexReader.java
index 85a61212200..acd95817c11 100644
-- a/lucene/src/java/org/apache/lucene/index/IndexReader.java
++ b/lucene/src/java/org/apache/lucene/index/IndexReader.java
@@ -1334,7 +1334,7 @@ public abstract class IndexReader implements Cloneable,Closeable {
    *  it by calling {@link IndexReader#open(IndexCommit,boolean)}
    *  There must be at least one commit in
    *  the Directory, else this method throws {@link
   *  java.io.IOException}.  Note that if a commit is in
   *  IndexNotFoundException}.  Note that if a commit is in
    *  progress while this method is running, that commit
    *  may or may not be returned array.  */
   public static Collection<IndexCommit> listCommits(Directory dir) throws IOException {
diff --git a/lucene/src/java/org/apache/lucene/index/IndexWriter.java b/lucene/src/java/org/apache/lucene/index/IndexWriter.java
index 9a1c78f7885..3e71f822c03 100644
-- a/lucene/src/java/org/apache/lucene/index/IndexWriter.java
++ b/lucene/src/java/org/apache/lucene/index/IndexWriter.java
@@ -1115,25 +1115,16 @@ public class IndexWriter implements Closeable {
         // against an index that's currently open for
         // searching.  In this case we write the next
         // segments_N file with no segments:
        boolean doCommit;
         try {
           segmentInfos.read(directory, codecs);
           segmentInfos.clear();
          doCommit = false;
         } catch (IOException e) {
           // Likely this means it's a fresh directory
          doCommit = true;
         }
 
        if (doCommit) {
          // Only commit if there is no segments file in
          // this dir already.
          segmentInfos.commit(directory);
        } else {
          // Record that we have a change (zero out all
          // segments) pending:
          changeCount++;
        }
        // Record that we have a change (zero out all
        // segments) pending:
        changeCount++;
       } else {
         segmentInfos.read(directory, codecs);
 
diff --git a/lucene/src/java/org/apache/lucene/index/SegmentInfos.java b/lucene/src/java/org/apache/lucene/index/SegmentInfos.java
index a368cecf2bb..4d53e9869a0 100644
-- a/lucene/src/java/org/apache/lucene/index/SegmentInfos.java
++ b/lucene/src/java/org/apache/lucene/index/SegmentInfos.java
@@ -649,7 +649,7 @@ public final class SegmentInfos extends Vector<SegmentInfo> {
           
           if (gen == -1) {
             // Neither approach found a generation
            throw new FileNotFoundException("no segments* file found in " + directory + ": files: " + Arrays.toString(files));
            throw new IndexNotFoundException("no segments* file found in " + directory + ": files: " + Arrays.toString(files));
           }
         }
 
diff --git a/lucene/src/java/org/apache/lucene/store/Directory.java b/lucene/src/java/org/apache/lucene/store/Directory.java
index 8ab8d07b308..0fc2ae3df00 100644
-- a/lucene/src/java/org/apache/lucene/store/Directory.java
++ b/lucene/src/java/org/apache/lucene/store/Directory.java
@@ -48,9 +48,12 @@ public abstract class Directory implements Closeable {
    * this Directory instance). */
   protected LockFactory lockFactory;
 
  /** Returns an array of strings, one for each file in the
   *  directory.
   * @throws IOException
  /**
   * Returns an array of strings, one for each file in the directory.
   * 
   * @throws NoSuchDirectoryException if the directory is not prepared for any
   *         write operations (such as {@link #createOutput(String)}).
   * @throws IOException in case of other IO errors
    */
   public abstract String[] listAll() throws IOException;
 
diff --git a/lucene/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java b/lucene/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java
index c43f16364d5..4f29e74e506 100644
-- a/lucene/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java
++ b/lucene/src/test/org/apache/lucene/TestSnapshotDeletionPolicy.java
@@ -45,8 +45,8 @@ import org.apache.lucene.util._TestUtil;
 // http://lucenebook.com
 //
 
public class TestSnapshotDeletionPolicy extends LuceneTestCase
{
public class TestSnapshotDeletionPolicy extends LuceneTestCase {
  
   public static final String INDEX_PATH = "test.snapshots";
 
   public void testSnapshotDeletionPolicy() throws Exception {
@@ -119,7 +119,8 @@ public class TestSnapshotDeletionPolicy extends LuceneTestCase
         TEST_VERSION_CURRENT, 
         new StandardAnalyzer(TEST_VERSION_CURRENT)).setIndexDeletionPolicy(dp)
         .setMaxBufferedDocs(2));

    writer.commit();
    
     final Thread t = new Thread() {
         @Override
         public void run() {
diff --git a/lucene/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java b/lucene/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
index 75ddaa4f103..d58f385d1ff 100644
-- a/lucene/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
++ b/lucene/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
@@ -558,11 +558,10 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
       assertTrue("could not locate the 'content' field number in the _2.cfs segment", contentFieldIndex != -1);
 
       // Now verify file names:
      String[] expected;
      expected = new String[] {"_0.cfs",
      String[] expected = new String[] {"_0.cfs",
                                "_0_1.del",
                                "_0_1.s" + contentFieldIndex,
                               "segments_3",
                               "segments_2",
                                "segments.gen"};
 
       String[] actual = dir.listAll();
diff --git a/lucene/src/test/org/apache/lucene/index/TestCrash.java b/lucene/src/test/org/apache/lucene/index/TestCrash.java
index 3734f69b927..883d89419ec 100644
-- a/lucene/src/test/org/apache/lucene/index/TestCrash.java
++ b/lucene/src/test/org/apache/lucene/index/TestCrash.java
@@ -28,16 +28,19 @@ import org.apache.lucene.document.Field;
 
 public class TestCrash extends LuceneTestCase {
 
  private IndexWriter initIndex() throws IOException {
    return initIndex(new MockRAMDirectory());
  private IndexWriter initIndex(boolean initialCommit) throws IOException {
    return initIndex(new MockRAMDirectory(), initialCommit);
   }
 
  private IndexWriter initIndex(MockRAMDirectory dir) throws IOException {
  private IndexWriter initIndex(MockRAMDirectory dir, boolean initialCommit) throws IOException {
     dir.setLockFactory(NoLockFactory.getNoLockFactory());
 
     IndexWriter writer  = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setMaxBufferedDocs(10));
     ((ConcurrentMergeScheduler) writer.getConfig().getMergeScheduler()).setSuppressExceptions();

    if (initialCommit) {
      writer.commit();
    }
    
     Document doc = new Document();
     doc.add(new Field("content", "aaa", Field.Store.YES, Field.Index.ANALYZED));
     doc.add(new Field("id", "0", Field.Store.YES, Field.Index.ANALYZED));
@@ -56,7 +59,10 @@ public class TestCrash extends LuceneTestCase {
   }
 
   public void testCrashWhileIndexing() throws IOException {
    IndexWriter writer = initIndex();
    // This test relies on being able to open a reader before any commit
    // happened, so we must create an initial commit just to allow that, but
    // before any documents were added.
    IndexWriter writer = initIndex(true);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
     crash(writer);
     IndexReader reader = IndexReader.open(dir, false);
@@ -64,11 +70,14 @@ public class TestCrash extends LuceneTestCase {
   }
 
   public void testWriterAfterCrash() throws IOException {
    IndexWriter writer = initIndex();
    // This test relies on being able to open a reader before any commit
    // happened, so we must create an initial commit just to allow that, but
    // before any documents were added.
    IndexWriter writer = initIndex(true);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
     dir.setPreventDoubleWrite(false);
     crash(writer);
    writer = initIndex(dir);
    writer = initIndex(dir, false);
     writer.close();
 
     IndexReader reader = IndexReader.open(dir, false);
@@ -76,10 +85,10 @@ public class TestCrash extends LuceneTestCase {
   }
 
   public void testCrashAfterReopen() throws IOException {
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
     writer.close();
    writer = initIndex(dir);
    writer = initIndex(dir, false);
     assertEquals(314, writer.maxDoc());
     crash(writer);
 
@@ -98,7 +107,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashAfterClose() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close();
@@ -117,7 +126,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashAfterCloseNoWait() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close(false);
@@ -136,7 +145,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashReaderDeletes() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close(false);
@@ -157,7 +166,7 @@ public class TestCrash extends LuceneTestCase {
 
   public void testCrashReaderDeletesAfterClose() throws IOException {
     
    IndexWriter writer = initIndex();
    IndexWriter writer = initIndex(false);
     MockRAMDirectory dir = (MockRAMDirectory) writer.getDirectory();
 
     writer.close(false);
diff --git a/lucene/src/test/org/apache/lucene/index/TestDeletionPolicy.java b/lucene/src/test/org/apache/lucene/index/TestDeletionPolicy.java
index 2f43d52fdd3..b418e59860d 100644
-- a/lucene/src/test/org/apache/lucene/index/TestDeletionPolicy.java
++ b/lucene/src/test/org/apache/lucene/index/TestDeletionPolicy.java
@@ -305,7 +305,7 @@ public class TestDeletionPolicy extends LuceneTestCase {
       writer.optimize();
       writer.close();
 
      assertEquals(2, policy.numOnInit);
      assertEquals(1, policy.numOnInit);
 
       // If we are not auto committing then there should
       // be exactly 2 commits (one per close above):
@@ -313,8 +313,8 @@ public class TestDeletionPolicy extends LuceneTestCase {
 
       // Test listCommits
       Collection<IndexCommit> commits = IndexReader.listCommits(dir);
      // 1 from opening writer + 2 from closing writer
      assertEquals(3, commits.size());
      // 2 from closing writer
      assertEquals(2, commits.size());
 
       // Make sure we can open a reader on each commit:
       for (final IndexCommit commit : commits) {
@@ -374,7 +374,7 @@ public class TestDeletionPolicy extends LuceneTestCase {
     writer.close();
 
     Collection<IndexCommit> commits = IndexReader.listCommits(dir);
    assertEquals(6, commits.size());
    assertEquals(5, commits.size());
     IndexCommit lastCommit = null;
     for (final IndexCommit commit : commits) {
       if (lastCommit == null || commit.getGeneration() > lastCommit.getGeneration())
@@ -389,7 +389,7 @@ public class TestDeletionPolicy extends LuceneTestCase {
     writer.optimize();
     writer.close();
 
    assertEquals(7, IndexReader.listCommits(dir).size());
    assertEquals(6, IndexReader.listCommits(dir).size());
 
     // Now open writer on the commit just before optimize:
     writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT))
@@ -412,7 +412,7 @@ public class TestDeletionPolicy extends LuceneTestCase {
     writer.close();
 
     // Now 8 because we made another commit
    assertEquals(8, IndexReader.listCommits(dir).size());
    assertEquals(7, IndexReader.listCommits(dir).size());
     
     r = IndexReader.open(dir, true);
     // Not optimized because we rolled it back, and now only
@@ -491,7 +491,7 @@ public class TestDeletionPolicy extends LuceneTestCase {
       writer.optimize();
       writer.close();
 
      assertEquals(2, policy.numOnInit);
      assertEquals(1, policy.numOnInit);
       // If we are not auto committing then there should
       // be exactly 2 commits (one per close above):
       assertEquals(2, policy.numOnCommit);
@@ -537,7 +537,7 @@ public class TestDeletionPolicy extends LuceneTestCase {
       }
 
       assertTrue(policy.numDelete > 0);
      assertEquals(N+1, policy.numOnInit);
      assertEquals(N, policy.numOnInit);
       assertEquals(N+1, policy.numOnCommit);
 
       // Simplistic check: just verify only the past N segments_N's still
@@ -625,8 +625,8 @@ public class TestDeletionPolicy extends LuceneTestCase {
       // this is a commit
       writer.close();
 
      assertEquals(2*(N+2), policy.numOnInit);
      assertEquals(2*(N+2)-1, policy.numOnCommit);
      assertEquals(2*(N+1)+1, policy.numOnInit);
      assertEquals(2*(N+2), policy.numOnCommit);
 
       IndexSearcher searcher = new IndexSearcher(dir, false);
       ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
@@ -735,8 +735,8 @@ public class TestDeletionPolicy extends LuceneTestCase {
         writer.close();
       }
 
      assertEquals(1+3*(N+1), policy.numOnInit);
      assertEquals(3*(N+1), policy.numOnCommit);
      assertEquals(3*(N+1), policy.numOnInit);
      assertEquals(3*(N+1)+1, policy.numOnCommit);
 
       IndexSearcher searcher = new IndexSearcher(dir, false);
       ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexFileDeleter.java b/lucene/src/test/org/apache/lucene/index/TestIndexFileDeleter.java
index 7ead2dd998e..f210435de5c 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexFileDeleter.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexFileDeleter.java
@@ -138,11 +138,11 @@ public class TestIndexFileDeleter extends LuceneTestCase {
     copyFile(dir, "_0.cfs", "deletable");
 
     // Create some old segments file:
    copyFile(dir, "segments_3", "segments");
    copyFile(dir, "segments_3", "segments_2");
    copyFile(dir, "segments_2", "segments");
    copyFile(dir, "segments_2", "segments_1");
 
     // Create a bogus cfs file shadowing a non-cfs segment:
    copyFile(dir, "_2.cfs", "_3.cfs");
    copyFile(dir, "_1.cfs", "_2.cfs");
 
     String[] filesPre = dir.listAll();
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexReader.java b/lucene/src/test/org/apache/lucene/index/TestIndexReader.java
index a2a55d11f2c..30d8cd201e9 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexReader.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexReader.java
@@ -466,18 +466,17 @@ public class TestIndexReader extends LuceneTestCase
     public void testLockObtainFailed() throws IOException {
         Directory dir = new RAMDirectory();
 
        IndexWriter writer = null;
        IndexReader reader = null;
         Term searchTerm = new Term("content", "aaa");
 
         //  add 11 documents with term : aaa
        writer  = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
        writer.commit();
         for (int i = 0; i < 11; i++) {
             addDoc(writer, searchTerm.text());
         }
 
         // Create reader:
        reader = IndexReader.open(dir, false);
        IndexReader reader = IndexReader.open(dir, false);
 
         // Try to make changes
         try {
@@ -1749,6 +1748,7 @@ public class TestIndexReader extends LuceneTestCase
     Directory dir = new MockRAMDirectory();
     IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
         TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
    writer.commit();
     Document doc = new Document();
     writer.addDocument(doc);
     IndexReader r = IndexReader.open(dir, true);
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexReaderReopen.java b/lucene/src/test/org/apache/lucene/index/TestIndexReaderReopen.java
index f7a8855c1f4..e99cc6639a6 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexReaderReopen.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexReaderReopen.java
@@ -174,6 +174,7 @@ public class TestIndexReaderReopen extends LuceneTestCase {
     IndexWriter iwriter = new IndexWriter(dir, new IndexWriterConfig(
         TEST_VERSION_CURRENT, new KeywordAnalyzer()).setOpenMode(
         OpenMode.CREATE).setMergeScheduler(new SerialMergeScheduler()));
    iwriter.commit();
     IndexReader reader = IndexReader.open(dir, false);
     try {
       int M = 3;
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
index 443b90bdb9a..8f04ac32758 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
@@ -70,6 +70,7 @@ import org.apache.lucene.store.IndexOutput;
 import org.apache.lucene.store.Lock;
 import org.apache.lucene.store.LockFactory;
 import org.apache.lucene.store.MockRAMDirectory;
import org.apache.lucene.store.NoLockFactory;
 import org.apache.lucene.store.RAMDirectory;
 import org.apache.lucene.store.SingleInstanceLockFactory;
 import org.apache.lucene.util.UnicodeUtil;
@@ -778,7 +779,7 @@ public class TestIndexWriter extends LuceneTestCase {
         writer.close();
 
         long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
        assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
        assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
         // Make the next segments file, with last byte
         // missing, to simulate a writer that crashed while
@@ -838,7 +839,7 @@ public class TestIndexWriter extends LuceneTestCase {
         writer.close();
 
         long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
        assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
        assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
         String fileNameIn = SegmentInfos.getCurrentSegmentFileName(dir);
         String fileNameOut = IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS,
@@ -903,7 +904,7 @@ public class TestIndexWriter extends LuceneTestCase {
         writer.close();
 
         long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
        assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
        assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
         String[] files = dir.listAll();
         for(int i=0;i<files.length;i++) {
@@ -2326,7 +2327,7 @@ public class TestIndexWriter extends LuceneTestCase {
   public void testImmediateDiskFull() throws IOException {
     MockRAMDirectory dir = new MockRAMDirectory();
     IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setMaxBufferedDocs(2));
    dir.setMaxSizeInBytes(dir.getRecomputedActualSizeInBytes());
    dir.setMaxSizeInBytes(Math.max(1, dir.getRecomputedActualSizeInBytes()));
     final Document doc = new Document();
     doc.add(new Field("field", "aaa bbb ccc ddd eee fff ggg hhh iii jjj", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
     try {
@@ -2644,7 +2645,7 @@ public class TestIndexWriter extends LuceneTestCase {
     writer.close();
 
     long gen = SegmentInfos.getCurrentSegmentGeneration(dir);
    assertTrue("segment generation should be > 1 but got " + gen, gen > 1);
    assertTrue("segment generation should be > 0 but got " + gen, gen > 0);
 
     final String segmentsFileName = SegmentInfos.getCurrentSegmentFileName(dir);
     IndexInput in = dir.openInput(segmentsFileName);
@@ -2673,7 +2674,8 @@ public class TestIndexWriter extends LuceneTestCase {
         TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT))
         .setMaxBufferedDocs(2));
     ((LogMergePolicy) writer.getConfig().getMergePolicy()).setMergeFactor(5);

    writer.commit();
    
     for (int i = 0; i < 23; i++)
       addDoc(writer);
 
@@ -3534,7 +3536,8 @@ public class TestIndexWriter extends LuceneTestCase {
 
     IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setMaxBufferedDocs(2));
     ((LogMergePolicy) writer.getConfig().getMergePolicy()).setMergeFactor(5);

    writer.commit();
    
     for (int i = 0; i < 23; i++)
       addDoc(writer);
 
@@ -3585,7 +3588,8 @@ public class TestIndexWriter extends LuceneTestCase {
 
     IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setMaxBufferedDocs(2));
     ((LogMergePolicy) writer.getConfig().getMergePolicy()).setMergeFactor(5);

    writer.commit();
    
     for (int i = 0; i < 23; i++)
       addDoc(writer);
 
@@ -3670,6 +3674,7 @@ public class TestIndexWriter extends LuceneTestCase {
 
       dir2 = new MockRAMDirectory();
       writer2 = new IndexWriter(dir2, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
      writer2.commit();
       cms = (ConcurrentMergeScheduler) writer2.getConfig().getMergeScheduler();
 
       readers = new IndexReader[NUM_COPY];
@@ -4952,4 +4957,62 @@ public class TestIndexWriter extends LuceneTestCase {
     w.close();
     dir.close();
   }
  
  public void testNoCommits() throws Exception {
    // Tests that if we don't call commit(), the directory has 0 commits. This has
    // changed since LUCENE-2386, where before IW would always commit on a fresh
    // new index.
    Directory dir = new RAMDirectory();
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
    try {
      IndexReader.listCommits(dir);
      fail("listCommits should have thrown an exception over empty index");
    } catch (IndexNotFoundException e) {
      // that's expected !
    }
    // No changes still should generate a commit, because it's a new index.
    writer.close();
    assertEquals("expected 1 commits!", 1, IndexReader.listCommits(dir).size());
  }

  public void testEmptyFSDirWithNoLock() throws Exception {
    // Tests that if FSDir is opened w/ a NoLockFactory (or SingleInstanceLF),
    // then IndexWriter ctor succeeds. Previously (LUCENE-2386) it failed 
    // when listAll() was called in IndexFileDeleter.
    FSDirectory dir = FSDirectory.open(new File(TEMP_DIR, "emptyFSDirNoLock"), NoLockFactory.getNoLockFactory());
    new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT))).close();
  }

  public void testEmptyDirRollback() throws Exception {
    // Tests that if IW is created over an empty Directory, some documents are
    // indexed, flushed (but not committed) and then IW rolls back, then no 
    // files are left in the Directory.
    Directory dir = new MockRAMDirectory();
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
        TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT))
        .setMaxBufferedDocs(2));
    // Creating over empty dir should not create any files.
    assertEquals(0, dir.listAll().length);
    Document doc = new Document();
    // create as many files as possible
    doc.add(new Field("c", "val", Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
    writer.addDocument(doc);
    // Adding just one document does not call flush yet.
    assertEquals("only the stored and term vector files should exist in the directory", 5, dir.listAll().length);
    
    doc = new Document();
    doc.add(new Field("c", "val", Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
    writer.addDocument(doc);
    // The second document should cause a flush.
    assertTrue("flush should have occurred and files created", dir.listAll().length > 5);
   
    // After rollback, IW should remove all files
    writer.rollback();
    assertEquals("no files should exist in the directory after rollback", 0, dir.listAll().length);

    // Since we rolled-back above, that close should be a no-op
    writer.close();
    assertEquals("expected a no-op close after IW.rollback()", 0, dir.listAll().length);
  }
  
 }
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
index c5e3383f76d..7bb4416b754 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
@@ -749,7 +749,7 @@ public class TestIndexWriterDelete extends LuceneTestCase {
 
     MockRAMDirectory dir = new MockRAMDirectory();
     IndexWriter modifier = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));

    modifier.commit();
     dir.failOn(failure.reset());
 
     for (int i = 0; i < keywords.length; i++) {
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
index b6afdf85359..855b7b8b2b6 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
@@ -134,6 +134,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
     MockIndexWriter writer  = new MockIndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setRAMBufferSizeMB(0.1));
     ((ConcurrentMergeScheduler) writer.getConfig().getMergeScheduler()).setSuppressExceptions();
     //writer.setMaxBufferedDocs(10);
    writer.commit();
 
     if (VERBOSE)
       writer.setInfoStream(System.out);
@@ -171,6 +172,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
     MockIndexWriter writer  = new MockIndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setRAMBufferSizeMB(0.2));
     ((ConcurrentMergeScheduler) writer.getConfig().getMergeScheduler()).setSuppressExceptions();
     //writer.setMaxBufferedDocs(10);
    writer.commit();
 
     if (VERBOSE)
       writer.setInfoStream(System.out);
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterReader.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterReader.java
index 511fdbe57c6..2a5640b30ff 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterReader.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterReader.java
@@ -561,6 +561,7 @@ public class TestIndexWriterReader extends LuceneTestCase {
   public void testAfterCommit() throws Exception {
     Directory dir1 = new MockRAMDirectory();
     IndexWriter writer = new IndexWriter(dir1, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
    writer.commit();
     writer.setInfoStream(infoStream);
 
     // create the index
@@ -828,4 +829,15 @@ public class TestIndexWriterReader extends LuceneTestCase {
     w.close();
     dir.close();
   }
  
  public void testEmptyIndex() throws Exception {
    // Ensures that getReader works on an empty index, which hasn't been committed yet.
    Directory dir = new MockRAMDirectory();
    IndexWriter w = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
    IndexReader r = w.getReader();
    assertEquals(0, r.numDocs());
    r.close();
    w.close();
  }

 }
diff --git a/lucene/src/test/org/apache/lucene/index/TestNoDeletionPolicy.java b/lucene/src/test/org/apache/lucene/index/TestNoDeletionPolicy.java
index f780a06795a..3d18e96ce1c 100644
-- a/lucene/src/test/org/apache/lucene/index/TestNoDeletionPolicy.java
++ b/lucene/src/test/org/apache/lucene/index/TestNoDeletionPolicy.java
@@ -82,10 +82,7 @@ public class TestNoDeletionPolicy extends LuceneTestCaseJ4 {
       doc.add(new Field("c", "a" + i, Store.YES, Index.ANALYZED));
       writer.addDocument(doc);
       writer.commit();
      // the reason to expect i + 2 commits is because when IndexWriter is
      // created it creates a first commit. If this ever changes, then the
      // expected should be i + 1 (and this comment removed).
      assertEquals("wrong number of commits !", i + 2, IndexReader.listCommits(dir).size());
      assertEquals("wrong number of commits !", i + 1, IndexReader.listCommits(dir).size());
     }
     writer.close();
   }
diff --git a/lucene/src/test/org/apache/lucene/index/TestStressIndexing.java b/lucene/src/test/org/apache/lucene/index/TestStressIndexing.java
index 1bc66a190ee..0b445839545 100644
-- a/lucene/src/test/org/apache/lucene/index/TestStressIndexing.java
++ b/lucene/src/test/org/apache/lucene/index/TestStressIndexing.java
@@ -122,7 +122,8 @@ public class TestStressIndexing extends MultiCodecTestCase {
         TEST_VERSION_CURRENT, new SimpleAnalyzer(TEST_VERSION_CURRENT))
         .setOpenMode(OpenMode.CREATE).setMaxBufferedDocs(10).setMergeScheduler(
             mergeScheduler));

    modifier.commit();
    
     TimedThread[] threads = new TimedThread[4];
     int numThread = 0;
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestStressIndexing2.java b/lucene/src/test/org/apache/lucene/index/TestStressIndexing2.java
index d38ba1fe55f..78600499d61 100644
-- a/lucene/src/test/org/apache/lucene/index/TestStressIndexing2.java
++ b/lucene/src/test/org/apache/lucene/index/TestStressIndexing2.java
@@ -150,6 +150,7 @@ public class TestStressIndexing2 extends MultiCodecTestCase {
     IndexWriter w = new MockIndexWriter(dir, new IndexWriterConfig(
         TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setOpenMode(OpenMode.CREATE).setRAMBufferSizeMB(
         0.1).setMaxBufferedDocs(maxBufferedDocs));
    w.commit();
     LogMergePolicy lmp = (LogMergePolicy) w.getConfig().getMergePolicy();
     lmp.setUseCompoundFile(false);
     lmp.setUseCompoundDocStore(false);
diff --git a/lucene/src/test/org/apache/lucene/store/TestLockFactory.java b/lucene/src/test/org/apache/lucene/store/TestLockFactory.java
index c07ece490b0..6db3e64be7e 100755
-- a/lucene/src/test/org/apache/lucene/store/TestLockFactory.java
++ b/lucene/src/test/org/apache/lucene/store/TestLockFactory.java
@@ -82,7 +82,7 @@ public class TestLockFactory extends LuceneTestCase {
                    NoLockFactory.class.isInstance(dir.getLockFactory()));
 
         IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));

        writer.commit(); // required so the second open succeed 
         // Create a 2nd IndexWriter.  This is normally not allowed but it should run through since we're not
         // using any locks:
         IndexWriter writer2 = null;
diff --git a/lucene/src/test/org/apache/lucene/store/TestWindowsMMap.java b/lucene/src/test/org/apache/lucene/store/TestWindowsMMap.java
index 8ccc79aa47c..8a9bd1559c0 100644
-- a/lucene/src/test/org/apache/lucene/store/TestWindowsMMap.java
++ b/lucene/src/test/org/apache/lucene/store/TestWindowsMMap.java
@@ -66,17 +66,22 @@ public class TestWindowsMMap extends LuceneTestCase {
     new File(TEMP_DIR,"testLuceneMmap").getAbsolutePath();
 
   public void testMmapIndex() throws Exception {
    FSDirectory storeDirectory;
    storeDirectory = new MMapDirectory(new File(storePathname), null);

    // sometimes the directory is not cleaned by rmDir, because on Windows it
    // may take some time until the files are finally dereferenced. So clean the
    // directory up front, or otherwise new IndexWriter will fail.
    File dirPath = new File(storePathname);
    rmDir(dirPath);
    MMapDirectory dir = new MMapDirectory(dirPath, null);
    
     // plan to add a set of useful stopwords, consider changing some of the
     // interior filters.
     StandardAnalyzer analyzer = new StandardAnalyzer(TEST_VERSION_CURRENT, Collections.emptySet());
     // TODO: something about lock timeouts and leftover locks.
    IndexWriter writer = new IndexWriter(storeDirectory, new IndexWriterConfig(
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
         TEST_VERSION_CURRENT, analyzer)
         .setOpenMode(OpenMode.CREATE));
    IndexSearcher searcher = new IndexSearcher(storeDirectory, true);
    writer.commit();
    IndexSearcher searcher = new IndexSearcher(dir, true);
     
     for(int dx = 0; dx < 1000; dx ++) {
       String f = randomField();
@@ -87,14 +92,16 @@ public class TestWindowsMMap extends LuceneTestCase {
     
     searcher.close();
     writer.close();
                rmDir(new File(storePathname));
    rmDir(dirPath);
   }
 
        private void rmDir(File dir) {
          File[] files = dir.listFiles();
          for (int i = 0; i < files.length; i++) {
            files[i].delete();
          }
          dir.delete();
        }
  private void rmDir(File dir) {
    if (!dir.exists()) {
      return;
    }
    for (File file : dir.listFiles()) {
      file.delete();
    }
    dir.delete();
  }
 }
- 
2.19.1.windows.1

