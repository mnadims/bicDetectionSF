From 390fbf21d2c5fdee1a94247d51722673883328ce Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Fri, 11 Jun 2010 09:45:04 +0000
Subject: [PATCH] LUCENE-2496: don't throw NPE on trying to CREATE over a
 corrupt index

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@953628 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |  4 +++
 .../apache/lucene/index/IndexFileDeleter.java | 15 ++++------
 .../apache/lucene/index/TestIndexWriter.java  | 29 ++++++++++++++++++-
 3 files changed, 38 insertions(+), 10 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index f22de792ae0..cf623a3abae 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -481,6 +481,10 @@ Bug fixes
   files when a mergedSegmentWarmer is set on IndexWriter.  (Mike
   McCandless)
 
* LUCENE-2496: Don't throw NPE if IndexWriter is opened with CREATE on
  a prior (corrupt) index missing its segments_N file.  (Mike
  McCandless)

 New features
 
 * LUCENE-2128: Parallelized fetching document frequencies during weight
diff --git a/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java b/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java
index 70785711477..5b5c9d2f800 100644
-- a/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java
++ b/lucene/src/java/org/apache/lucene/index/IndexFileDeleter.java
@@ -134,8 +134,10 @@ final class IndexFileDeleter {
     this.docWriter = docWriter;
     this.infoStream = infoStream;
 
    final String currentSegmentsFile = segmentInfos.getCurrentSegmentFileName();

     if (infoStream != null)
      message("init: current segments file is \"" + segmentInfos.getCurrentSegmentFileName() + "\"; deletionPolicy=" + policy);
      message("init: current segments file is \"" + currentSegmentsFile + "\"; deletionPolicy=" + policy);
 
     this.policy = policy;
     this.directory = directory;
@@ -146,7 +148,6 @@ final class IndexFileDeleter {
     indexFilenameFilter = new IndexFileNameFilter(codecs);
     
     CommitPoint currentCommitPoint = null;
    boolean seenIndexFiles = false;
     String[] files = null;
     try {
       files = directory.listAll();
@@ -158,7 +159,6 @@ final class IndexFileDeleter {
     for (String fileName : files) {
 
       if ((indexFilenameFilter.accept(null, fileName)) && !fileName.endsWith("write.lock") && !fileName.equals(IndexFileNames.SEGMENTS_GEN)) {
        seenIndexFiles = true;
         
         // Add this file to refCounts with initial count 0:
         getRefCount(fileName);
@@ -201,10 +201,7 @@ final class IndexFileDeleter {
       }
     }
 
    // If we haven't seen any Lucene files, then currentCommitPoint is expected
    // to be null, because it means it's a fresh Directory. Therefore it cannot
    // be any NFS cache issues - so just ignore.
    if (currentCommitPoint == null && seenIndexFiles) {
    if (currentCommitPoint == null && currentSegmentsFile != null) {
       // We did not in fact see the segments_N file
       // corresponding to the segmentInfos that was passed
       // in.  Yet, it must exist, because our caller holds
@@ -214,7 +211,7 @@ final class IndexFileDeleter {
       // try now to explicitly open this commit point:
       SegmentInfos sis = new SegmentInfos();
       try {
        sis.read(directory, segmentInfos.getCurrentSegmentFileName(), codecs);
        sis.read(directory, currentSegmentsFile, codecs);
       } catch (IOException e) {
         throw new CorruptIndexException("failed to locate current segments_N file");
       }
@@ -244,7 +241,7 @@ final class IndexFileDeleter {
 
     // Finally, give policy a chance to remove things on
     // startup:
    if (seenIndexFiles) {
    if (currentSegmentsFile != null) {
       policy.onInit(commits);
     }
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
index c319cc7a26e..a3d12032b1f 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
@@ -4954,5 +4954,32 @@ public class TestIndexWriter extends LuceneTestCase {
     writer.close();
     assertEquals("expected a no-op close after IW.rollback()", 0, dir.listAll().length);
   }
  

  public void testNoSegmentFile() throws IOException {
    File tempDir = _TestUtil.getTempDir("noSegmentFile");
    try {
      Directory dir = FSDirectory.open(tempDir);
      dir.setLockFactory(new NoLockFactory());
      IndexWriter w = new IndexWriter(dir, new IndexWriterConfig(
                                                                 TEST_VERSION_CURRENT, new MockAnalyzer())
                                      .setMaxBufferedDocs(2));

      Document doc = new Document();
      doc.add(new Field("c", "val", Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
      w.addDocument(doc);
      w.addDocument(doc);
      String[] files = dir.listAll();
      for(String file : files) {
        System.out.println("file=" + file);
      }
      IndexWriter w2 = new IndexWriter(dir, new IndexWriterConfig(
                                                                  TEST_VERSION_CURRENT, new MockAnalyzer())
                                       .setMaxBufferedDocs(2).setOpenMode(OpenMode.CREATE));

      w2.close();
      dir.close();
    } finally {
      _TestUtil.rmDir(tempDir);
    }
  }
 }
- 
2.19.1.windows.1

