From 1aa40cd3953ea1e5863eb830f3529da6a3223498 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Sat, 8 Jan 2011 01:17:43 +0000
Subject: [PATCH] LUCENE-2831: attempt to use the correct reader context rather
 than doing getTopReaderContext on a leaf

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1056585 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/schema/LatLonType.java    |  4 +-
 .../java/org/apache/solr/search/Grouping.java |  2 +-
 .../solr/search/SolrConstantScoreQuery.java   |  2 +-
 .../apache/solr/search/ValueSourceParser.java |  2 -
 .../solr/search/function/BoostedQuery.java    |  2 +-
 .../solr/search/function/FunctionQuery.java   |  2 +-
 .../search/function/QueryValueSource.java     | 29 ++++++++++---
 .../solr/search/function/ValueSource.java     | 43 ++++++++++++++++++-
 .../apache/solr/search/TestIndexSearcher.java |  2 +-
 9 files changed, 72 insertions(+), 16 deletions(-)

diff --git a/solr/src/java/org/apache/solr/schema/LatLonType.java b/solr/src/java/org/apache/solr/schema/LatLonType.java
index c76187fd325..75694f7eaa9 100644
-- a/solr/src/java/org/apache/solr/schema/LatLonType.java
++ b/solr/src/java/org/apache/solr/schema/LatLonType.java
@@ -342,8 +342,8 @@ class SpatialDistanceQuery extends Query {
 
     public SpatialWeight(IndexSearcher searcher) throws IOException {
       this.searcher = searcher;
      this.latContext = latSource.newContext();
      this.lonContext = lonSource.newContext();
      this.latContext = latSource.newContext(searcher);
      this.lonContext = lonSource.newContext(searcher);
       latSource.createWeight(latContext, searcher);
       lonSource.createWeight(lonContext, searcher);
     }
diff --git a/solr/src/java/org/apache/solr/search/Grouping.java b/solr/src/java/org/apache/solr/search/Grouping.java
index f76616adfbf..c46d25ea4b5 100755
-- a/solr/src/java/org/apache/solr/search/Grouping.java
++ b/solr/src/java/org/apache/solr/search/Grouping.java
@@ -151,7 +151,7 @@ public class Grouping {
     
     @Override
     void prepare() throws IOException {
        Map context = ValueSource.newContext();
        Map context = ValueSource.newContext(searcher);
         groupBy.createWeight(context, searcher);
     }
 
diff --git a/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java b/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
index 422f5926199..0c5179e3d8e 100755
-- a/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
++ b/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
@@ -62,7 +62,7 @@ public class SolrConstantScoreQuery extends ConstantScoreQuery {
 
     public ConstantWeight(IndexSearcher searcher) throws IOException {
       this.similarity = getSimilarity(searcher);
      this.context = ValueSource.newContext();
      this.context = ValueSource.newContext(searcher);
       if (filter instanceof SolrFilter)
         ((SolrFilter)filter).createWeight(context, searcher);
     }
diff --git a/solr/src/java/org/apache/solr/search/ValueSourceParser.java b/solr/src/java/org/apache/solr/search/ValueSourceParser.java
index f9ceb1feec6..116058fc301 100755
-- a/solr/src/java/org/apache/solr/search/ValueSourceParser.java
++ b/solr/src/java/org/apache/solr/search/ValueSourceParser.java
@@ -890,8 +890,6 @@ abstract class Double2Parser extends NamedParser {
 
     @Override
     public void createWeight(Map context, IndexSearcher searcher) throws IOException {
      a.createWeight(context,searcher);
      b.createWeight(context,searcher);
     }
 
     public int hashCode() {
diff --git a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
index c1ea5e97870..9530484fe99 100755
-- a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
++ b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
@@ -66,7 +66,7 @@ public class BoostedQuery extends Query {
     public BoostedWeight(IndexSearcher searcher) throws IOException {
       this.searcher = searcher;
       this.qWeight = q.weight(searcher);
      this.context = boostVal.newContext();
      this.context = boostVal.newContext(searcher);
       boostVal.createWeight(context,searcher);
     }
 
diff --git a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
index 397798f064e..1a6ad49bb6e 100644
-- a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
++ b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
@@ -67,7 +67,7 @@ public class FunctionQuery extends Query {
 
     public FunctionWeight(IndexSearcher searcher) throws IOException {
       this.searcher = searcher;
      this.context = func.newContext();
      this.context = func.newContext(searcher);
       func.createWeight(context, searcher);
     }
 
diff --git a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
index cf65b3968fe..b3b0c66b44d 100755
-- a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
@@ -44,8 +44,8 @@ public class QueryValueSource extends ValueSource {
   }
 
   @Override
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    return new QueryDocValues(reader, q, defVal, context==null ? null : (Weight)context.get(this));
  public DocValues getValues(Map fcontext, IndexReader reader) throws IOException {
    return new QueryDocValues(reader, q, defVal, fcontext);
   }
 
   public int hashCode() {
@@ -71,6 +71,7 @@ class QueryDocValues extends DocValues {
   final IndexReader reader;
   final Weight weight;
   final float defVal;
  final Map fcontext;
 
   Scorer scorer;
   int scorerDoc; // the document the scorer is on
@@ -79,18 +80,36 @@ class QueryDocValues extends DocValues {
   // to trigger a scorer reset on first access.
   int lastDocRequested=Integer.MAX_VALUE;
 
  public QueryDocValues(IndexReader reader, Query q, float defVal, Weight w) throws IOException {
  public QueryDocValues(IndexReader reader, Query q, float defVal, Map fcontext) throws IOException {
     this.reader = reader;
     this.q = q;
     this.defVal = defVal;
    weight = w!=null ? w : q.weight(new IndexSearcher(reader));
    this.fcontext = fcontext;

    Weight w = fcontext==null ? null : (Weight)fcontext.get(q);
    if (w == null) {
       IndexSearcher weightSearcher = fcontext == null ? new IndexSearcher(reader) : (IndexSearcher)fcontext.get("searcher");

       // TODO: sort by function doesn't weight (SOLR-1297 is open because of this bug)... so weightSearcher will currently be null
       if (weightSearcher == null) weightSearcher = new IndexSearcher(reader);

       w = q.weight(weightSearcher);
    }
    weight = w;
   }
 
   public float floatVal(int doc) {
     try {
       if (doc < lastDocRequested) {
         // out-of-order access.... reset scorer.
        scorer = weight.scorer(reader.getTopReaderContext(), true, false);
        IndexReader.AtomicReaderContext ctx = ValueSource.readerToContext(fcontext, reader);

        if (ctx == null) {
          // TODO: this is because SOLR-1297 does not weight
          ctx = (IndexReader.AtomicReaderContext)reader.getTopReaderContext();  // this is the incorrect context
        }

        scorer = weight.scorer(ctx, true, false);
         if (scorer==null) return defVal;
         scorerDoc = -1;
       }
diff --git a/solr/src/java/org/apache/solr/search/function/ValueSource.java b/solr/src/java/org/apache/solr/search/function/ValueSource.java
index 48a56ce15cf..daaffd65504 100644
-- a/solr/src/java/org/apache/solr/search/function/ValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/ValueSource.java
@@ -18,6 +18,8 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.FieldComparator;
 import org.apache.lucene.search.FieldComparatorSource;
 import org.apache.lucene.search.Scorer;
@@ -25,6 +27,7 @@ import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.index.MultiFields;
import org.apache.solr.common.SolrException;
 
 import java.io.IOException;
 import java.io.Serializable;
@@ -90,10 +93,46 @@ public abstract class ValueSource implements Serializable {
   /**
    * Returns a new non-threadsafe context map.
    */
  public static Map newContext() {
    return new IdentityHashMap();
  public static Map newContext(IndexSearcher searcher) {
    Map context = new IdentityHashMap();
    context.put("searcher", searcher);
    return context;
   }
 
  /* @lucene.internal
   * This will most likely go away in the future.
   */
  public static AtomicReaderContext readerToContext(Map fcontext, IndexReader reader) {
    Object v = fcontext.get(reader);
    if (v == null) {
      IndexSearcher searcher = (IndexSearcher)fcontext.get("searcher");
      if (searcher == null) {
        return null;
        // TODO
        // throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "no searcher found in function context");
      }
      ReaderContext rcontext = searcher.getIndexReader().getTopReaderContext();
      if (rcontext.isAtomic) {
        assert rcontext.reader == reader;
        fcontext.put(rcontext.reader, (AtomicReaderContext)rcontext);
      } else {
        for (AtomicReaderContext subCtx : rcontext.leaves()) {
          fcontext.put(subCtx.reader, subCtx);
        }
      }

      v = fcontext.get(reader);
      if (v == null) {
        return null;
        // TODO
        // throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "reader " + reader + " is not from the top reader " + searcher.getIndexReader());
      }
    }

    return (AtomicReaderContext)v;
  }


   class ValueSourceComparatorSource extends FieldComparatorSource {
 
 
diff --git a/solr/src/test/org/apache/solr/search/TestIndexSearcher.java b/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
index 8b0f0edecf6..4ecd72df623 100755
-- a/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
++ b/solr/src/test/org/apache/solr/search/TestIndexSearcher.java
@@ -36,7 +36,7 @@ public class TestIndexSearcher extends SolrTestCaseJ4 {
   private String getStringVal(SolrQueryRequest sqr, String field, int doc) throws IOException {
     SchemaField sf = sqr.getSchema().getField(field);
     ValueSource vs = sf.getType().getValueSource(sf, null);
    Map context = ValueSource.newContext();
    Map context = ValueSource.newContext(sqr.getSearcher());
     vs.createWeight(context, sqr.getSearcher());
     SolrIndexReader sr = sqr.getSearcher().getReader();
     int idx = SolrIndexReader.readerIndex(doc, sr.getLeafOffsets());
- 
2.19.1.windows.1

