From 4140f73774e6c8f1509e13d3e196847500204f0b Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Thu, 6 Jan 2011 01:03:00 +0000
Subject: [PATCH] LUCENE-2831: fix isTopLevel

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1055695 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/lucene/index/IndexReader.java    | 17 ++++++-----------
 .../org/apache/lucene/search/IndexSearcher.java |  5 +----
 2 files changed, 7 insertions(+), 15 deletions(-)

diff --git a/lucene/src/java/org/apache/lucene/index/IndexReader.java b/lucene/src/java/org/apache/lucene/index/IndexReader.java
index ac0f203a8fb..fb09bbc5917 100644
-- a/lucene/src/java/org/apache/lucene/index/IndexReader.java
++ b/lucene/src/java/org/apache/lucene/index/IndexReader.java
@@ -1471,13 +1471,13 @@ public abstract class IndexReader implements Cloneable,Closeable {
     public final int ordInParent;
     
     ReaderContext(ReaderContext parent, IndexReader reader,
        boolean isAtomic, boolean isTopLevel, int ordInParent, int docBaseInParent) {
        boolean isAtomic, int ordInParent, int docBaseInParent) {
       this.parent = parent;
       this.reader = reader;
       this.isAtomic = isAtomic;
       this.docBaseInParent = docBaseInParent;
       this.ordInParent = ordInParent;
      this.isTopLevel = isTopLevel;
      this.isTopLevel = parent==null;
     }
     
     /**
@@ -1533,7 +1533,7 @@ public abstract class IndexReader implements Cloneable,Closeable {
     private CompositeReaderContext(ReaderContext parent, IndexReader reader,
         int ordInParent, int docbaseInParent, ReaderContext[] children,
         AtomicReaderContext[] leaves) {
      super(parent, reader, false, leaves != null, ordInParent, docbaseInParent);
      super(parent, reader, false, ordInParent, docbaseInParent);
       this.children = children;
       this.leaves = leaves;
     }
@@ -1561,15 +1561,10 @@ public abstract class IndexReader implements Cloneable,Closeable {
     public final int docBase;
     /**
      * Creates a new {@link AtomicReaderContext} 
     */
     */    
     public AtomicReaderContext(ReaderContext parent, IndexReader reader,
         int ord, int docBase, int leafOrd, int leafDocBase) {
     this(parent, reader, ord, docBase, leafOrd, leafDocBase, false);
    }
    
    private AtomicReaderContext(ReaderContext parent, IndexReader reader,
        int ord, int docBase, int leafOrd, int leafDocBase, boolean topLevel) {
      super(parent, reader, true, topLevel,  ord, docBase);
      super(parent, reader, true, ord, docBase);
       assert reader.getSequentialSubReaders() == null : "Atomic readers must not have subreaders";
       this.ord = leafOrd;
       this.docBase = leafDocBase;
@@ -1580,7 +1575,7 @@ public abstract class IndexReader implements Cloneable,Closeable {
      * parent.
      */
     public AtomicReaderContext(IndexReader atomicReader) {
      this(null, atomicReader, 0, 0, 0, 0, true); // toplevel!!
      this(null, atomicReader, 0, 0, 0, 0);
     }
   }
 }
diff --git a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
index eb2a3809e89..73f5f834e3e 100644
-- a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
++ b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
@@ -153,10 +153,7 @@ public class IndexSearcher {
   }
 
   private IndexSearcher(ReaderContext context, boolean closeReader, ExecutorService executor) {
    // TODO: eable this assert once SolrIndexReader and friends are refactored to use ReaderContext
    // We can't assert this here since SolrIndexReader will fail in some contexts - once solr is consistent we should be fine here
    // Lucene instead passes all tests even with this assert!
    // assert context.isTopLevel: "IndexSearcher's ReaderContext must be topLevel for reader" + context.reader;
    assert context.isTopLevel: "IndexSearcher's ReaderContext must be topLevel for reader" + context.reader;
     reader = context.reader;
     this.executor = executor;
     this.closeReader = closeReader;
- 
2.19.1.windows.1

