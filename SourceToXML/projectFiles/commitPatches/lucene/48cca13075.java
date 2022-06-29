From 48cca13075451e51483505ff8f6e75af3c919ad2 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Thu, 13 May 2010 16:51:18 +0000
Subject: [PATCH] LUCENE-2459: fix FilterIndexReader to (by default) emulate
 flex API on top of pre-flex API

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@943935 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/index/FilterIndexReader.java    | 15 +++++++++++++--
 .../lucene/index/TestFilterIndexReader.java       |  8 ++++++++
 2 files changed, 21 insertions(+), 2 deletions(-)

diff --git a/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java b/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
index 20a96bdd37f..c9c048a0d3d 100644
-- a/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
++ b/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
@@ -119,7 +119,7 @@ public class FilterIndexReader extends IndexReader {
   
   @Override
   public Bits getDeletedDocs() throws IOException {
    return in.getDeletedDocs();
    return MultiFields.getDeletedDocs(in);
   }
   
   @Override
@@ -291,7 +291,18 @@ public class FilterIndexReader extends IndexReader {
   
   @Override
   public IndexReader[] getSequentialSubReaders() {
    return in.getSequentialSubReaders();
    return null;
  }

  /* Flex API wrappers. */
  @Override
  public Fields fields() throws IOException {
    return new LegacyFields(this);
  }

  @Override
  public Terms terms(String field) throws IOException {
    return new LegacyTerms(this, field);
   }
 
   /** If the subclass of FilteredIndexReader modifies the
diff --git a/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java b/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java
index 00b7c4fc791..eee1ff6a013 100644
-- a/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java
++ b/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java
@@ -113,7 +113,15 @@ public class TestFilterIndexReader extends LuceneTestCase {
 
     writer.close();
 
    //IndexReader reader = new TestReader(IndexReader.open(directory, true));
    RAMDirectory target = new MockRAMDirectory();
    writer = new IndexWriter(target, new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer()));
     IndexReader reader = new TestReader(IndexReader.open(directory, true));
    writer.addIndexes(reader);
    writer.close();
    reader.close();
    reader = IndexReader.open(target, true);
    
 
     assertTrue(reader.isOptimized());
     
- 
2.19.1.windows.1

