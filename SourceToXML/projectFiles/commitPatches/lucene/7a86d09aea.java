From 7a86d09aea58f7bb7217f96b5a41c601d7221782 Mon Sep 17 00:00:00 2001
From: Uwe Schindler <uschindler@apache.org>
Date: Fri, 19 Nov 2010 18:49:54 +0000
Subject: [PATCH] LUCENE-2769: FilterIndexReader in trunk does not implement
 getSequentialSubReaders() correctly

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1036977 13f79535-47bb-0310-9956-ffa450edef68
--
 .../src/java/org/apache/lucene/index/FilterIndexReader.java | 6 +++---
 .../org/apache/lucene/index/SlowMultiReaderWrapper.java     | 5 +++++
 .../test/org/apache/lucene/index/TestFilterIndexReader.java | 2 +-
 3 files changed, 9 insertions(+), 4 deletions(-)

diff --git a/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java b/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
index 8149fd12567..ca211eaccea 100644
-- a/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
++ b/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
@@ -279,7 +279,7 @@ public class FilterIndexReader extends IndexReader {
   
   @Override
   public Bits getDeletedDocs() {
    return MultiFields.getDeletedDocs(in);
    return in.getDeletedDocs();
   }
   
   @Override
@@ -415,12 +415,12 @@ public class FilterIndexReader extends IndexReader {
   
   @Override
   public IndexReader[] getSequentialSubReaders() {
    return null;
    return in.getSequentialSubReaders();
   }
 
   @Override
   public Fields fields() throws IOException {
    return MultiFields.getFields(in);
    return in.fields();
   }
 
   /** If the subclass of FilteredIndexReader modifies the
diff --git a/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java b/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java
index 77f9dc4a69e..d50d8fec430 100644
-- a/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java
++ b/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java
@@ -82,4 +82,9 @@ public final class SlowMultiReaderWrapper extends FilterIndexReader {
   public void doClose() throws IOException {
     throw new UnsupportedOperationException("please call close on the original reader instead");
   }

  @Override
  public IndexReader[] getSequentialSubReaders() {
    return null;
  } 
 }
diff --git a/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java b/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java
index 49c43a48f5e..43dd8226f57 100644
-- a/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java
++ b/lucene/src/test/org/apache/lucene/index/TestFilterIndexReader.java
@@ -149,7 +149,7 @@ public class TestFilterIndexReader extends LuceneTestCase {
     //IndexReader reader = new TestReader(IndexReader.open(directory, true));
     Directory target = newDirectory();
     writer = new IndexWriter(target, newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer()));
    IndexReader reader = new TestReader(IndexReader.open(directory, true));
    IndexReader reader = new TestReader(SlowMultiReaderWrapper.wrap(IndexReader.open(directory, true)));
     writer.addIndexes(reader);
     writer.close();
     reader.close();
- 
2.19.1.windows.1

