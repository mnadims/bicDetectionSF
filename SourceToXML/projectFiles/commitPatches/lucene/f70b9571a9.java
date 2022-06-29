From f70b9571a9f773232e2cc18be21227aed1f5e086 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Fri, 7 Jan 2011 14:31:09 +0000
Subject: [PATCH] LUCENE-2831: pre-migrate to atomic context

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1056337 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/src/java/org/apache/solr/search/DocSet.java    | 13 +++++--------
 .../org/apache/solr/search/SortedIntDocSet.java     | 13 +++++--------
 .../src/test/org/apache/solr/search/TestDocSet.java |  5 +++++
 3 files changed, 15 insertions(+), 16 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/DocSet.java b/solr/src/java/org/apache/solr/search/DocSet.java
index 32dc4f53e94..59b3286c98a 100644
-- a/solr/src/java/org/apache/solr/search/DocSet.java
++ b/solr/src/java/org/apache/solr/search/DocSet.java
@@ -248,17 +248,14 @@ abstract class DocSetBase implements DocSet {
     return new Filter() {
       @Override
       public DocIdSet getDocIdSet(ReaderContext ctx) throws IOException {
        int offset = 0;
        IndexReader.AtomicReaderContext context = (IndexReader.AtomicReaderContext)ctx;  // TODO: remove after lucene migration
         IndexReader reader = ctx.reader;
        SolrIndexReader r = (SolrIndexReader)reader;
        while (r.getParent() != null) {
          offset += r.getBase();
          r = r.getParent();
        }
 
        if (r==reader) return bs;
        if (context.isTopLevel) {
          return bs;
        }
 
        final int base = offset;
        final int base = context.docBase;
         final int maxDoc = reader.maxDoc();
         final int max = base + maxDoc;   // one past the max doc in this segment.
 
diff --git a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
index b0bb860407b..052bac50ef6 100755
-- a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
++ b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
@@ -23,6 +23,7 @@ import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
 
@@ -552,15 +553,11 @@ public class SortedIntDocSet extends DocSetBase {
       int lastEndIdx = 0;
 
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
        int offset = 0;
      public DocIdSet getDocIdSet(ReaderContext contextX) throws IOException {
        AtomicReaderContext context = (AtomicReaderContext)contextX;  // TODO: remove after lucene migration
         IndexReader reader = context.reader;
        SolrIndexReader r = (SolrIndexReader)reader;
        while (r.getParent() != null) {
          offset += r.getBase();
          r = r.getParent();
        }
        final int base = offset;

        final int base = context.docBase;
         final int maxDoc = reader.maxDoc();
         final int max = base + maxDoc;   // one past the max doc in this segment.
         int sidx = Math.max(0,lastEndIdx);
diff --git a/solr/src/test/org/apache/solr/search/TestDocSet.java b/solr/src/test/org/apache/solr/search/TestDocSet.java
index 280e58d39ae..8a87ac5d72b 100644
-- a/solr/src/test/org/apache/solr/search/TestDocSet.java
++ b/solr/src/test/org/apache/solr/search/TestDocSet.java
@@ -413,10 +413,15 @@ public class TestDocSet extends LuceneTestCase {
     Filter fa = a.getTopFilter();
     Filter fb = b.getTopFilter();
 
    /*** top level filters are no longer supported
     // test top-level
     DocIdSet da = fa.getDocIdSet(topLevelContext);
     DocIdSet db = fb.getDocIdSet(topLevelContext);
     doTestIteratorEqual(da, db);
    ***/

    DocIdSet da;
    DocIdSet db;
 
     // first test in-sequence sub readers
     for (ReaderContext readerInfo : topLevelContext.leaves()) {
- 
2.19.1.windows.1

