From 52124d8ebfe4c7ee73caea0f10cf88feb68f5ded Mon Sep 17 00:00:00 2001
From: Simon Willnauer <simonw@apache.org>
Date: Tue, 11 Jan 2011 11:59:26 +0000
Subject: [PATCH] LUCENE-2831: cut over to AtomicReaderContext in
 Weight#scorer, Weight#explain & Filter#getDocIdSet

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1057595 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/search/BooleanFilter.java   |  8 +--
 .../apache/lucene/search/ChainedFilter.java   | 24 +++----
 .../apache/lucene/search/DuplicateFilter.java |  4 +-
 .../search/FieldCacheRewriteMethod.java       |  4 +-
 .../org/apache/lucene/search/TermsFilter.java |  4 +-
 .../apache/lucene/search/TermsFilterTest.java |  5 +-
 .../geohash/GeoHashDistanceFilter.java        |  4 +-
 .../spatial/tier/CartesianShapeFilter.java    |  4 +-
 .../spatial/tier/LatLongDistanceFilter.java   |  4 +-
 .../lucene/spatial/tier/TestDistance.java     |  3 +-
 .../builders/NumericRangeFilterBuilder.java   |  4 +-
 .../TestNumericRangeFilterBuilder.java        |  6 +-
 .../apache/lucene/index/BufferedDeletes.java  |  7 +-
 .../apache/lucene/search/BooleanQuery.java    |  6 +-
 .../lucene/search/CachingSpanFilter.java      |  4 +-
 .../lucene/search/CachingWrapperFilter.java   |  4 +-
 .../lucene/search/ConstantScoreQuery.java     |  6 +-
 .../lucene/search/DisjunctionMaxQuery.java    |  6 +-
 .../lucene/search/FieldCacheRangeFilter.java  | 18 ++---
 .../lucene/search/FieldCacheTermsFilter.java  |  4 +-
 .../java/org/apache/lucene/search/Filter.java |  7 +-
 .../apache/lucene/search/FilteredQuery.java   |  6 +-
 .../apache/lucene/search/IndexSearcher.java   | 67 ++++++++-----------
 .../lucene/search/MatchAllDocsQuery.java      |  6 +-
 .../lucene/search/MultiPhraseQuery.java       |  6 +-
 .../search/MultiTermQueryWrapperFilter.java   |  4 +-
 .../org/apache/lucene/search/PhraseQuery.java |  6 +-
 .../lucene/search/QueryWrapperFilter.java     |  8 ++-
 .../apache/lucene/search/SpanQueryFilter.java |  4 +-
 .../org/apache/lucene/search/TermQuery.java   | 19 +++++-
 .../java/org/apache/lucene/search/Weight.java | 22 ++----
 .../search/function/CustomScoreQuery.java     |  8 +--
 .../search/function/ValueSourceQuery.java     |  6 +-
 .../search/payloads/PayloadNearQuery.java     |  4 +-
 .../search/payloads/PayloadTermQuery.java     |  4 +-
 .../lucene/search/spans/SpanWeight.java       |  6 +-
 .../org/apache/lucene/util/ReaderUtil.java    | 15 +++++
 .../search/CachingWrapperFilterHelper.java    |  4 +-
 .../lucene/search/JustCompileSearch.java      | 10 +--
 .../org/apache/lucene/search/MockFilter.java  |  4 +-
 .../org/apache/lucene/search/QueryUtils.java  | 23 +++----
 .../lucene/search/SingleDocTestFilter.java    |  4 +-
 .../search/TestCachingWrapperFilter.java      | 22 +++---
 .../search/TestDisjunctionMaxQuery.java       |  6 +-
 .../apache/lucene/search/TestDocIdSet.java    |  4 +-
 .../lucene/search/TestFilteredQuery.java      |  6 +-
 .../lucene/search/TestFilteredSearch.java     |  3 +-
 .../search/TestNumericRangeQuery32.java       |  8 ++-
 .../search/TestNumericRangeQuery64.java       |  8 ++-
 .../apache/lucene/search/TestScorerPerf.java  |  4 +-
 .../org/apache/lucene/search/TestSort.java    |  4 +-
 .../apache/lucene/search/TestTermScorer.java  | 15 +++--
 .../search/spans/TestNearSpansOrdered.java    |  7 +-
 .../PerSegmentSingleValuedFaceting.java       | 16 ++---
 .../org/apache/solr/schema/LatLonType.java    |  6 +-
 .../java/org/apache/solr/search/DocSet.java   |  4 +-
 .../solr/search/SolrConstantScoreQuery.java   | 10 +--
 .../org/apache/solr/search/SolrFilter.java    |  3 +-
 .../apache/solr/search/SortedIntDocSet.java   |  3 +-
 .../solr/search/function/BoostedQuery.java    |  7 +-
 .../solr/search/function/FunctionQuery.java   |  5 +-
 .../search/function/ScaleFloatFunction.java   |  5 +-
 .../solr/search/function/ValueSource.java     |  1 -
 .../org/apache/solr/search/TestDocSet.java    | 17 +++--
 .../test/org/apache/solr/search/TestSort.java |  4 +-
 65 files changed, 276 insertions(+), 264 deletions(-)

diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java
index e3748774456..99a84b20bb7 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 import java.util.ArrayList;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.OpenBitSetDISI;
@@ -42,10 +42,10 @@ public class BooleanFilter extends Filter
   ArrayList<Filter> notFilters = null;
   ArrayList<Filter> mustFilters = null;
   
  private DocIdSetIterator getDISI(ArrayList<Filter> filters, int index, ReaderContext info)
  private DocIdSetIterator getDISI(ArrayList<Filter> filters, int index, AtomicReaderContext context)
   throws IOException
   {
    return filters.get(index).getDocIdSet(info).iterator();
    return filters.get(index).getDocIdSet(context).iterator();
   }
 
   /**
@@ -53,7 +53,7 @@ public class BooleanFilter extends Filter
    * of the filters that have been added.
    */
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException
   {
     OpenBitSetDISI res = null;
     final IndexReader reader = context.reader;
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java
index a8cc00caf5f..4041792f5af 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java
@@ -20,7 +20,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Filter;
@@ -97,7 +97,7 @@ public class ChainedFilter extends Filter
      * {@link Filter#getDocIdSet}.
      */
     @Override
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException
    public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException
     {
         int[] index = new int[1]; // use array as reference to modifiable int; 
         index[0] = 0;             // an object attribute would not be thread safe.
@@ -109,9 +109,9 @@ public class ChainedFilter extends Filter
             return getDocIdSet(context, DEFAULT, index);
     }
 
    private DocIdSetIterator getDISI(Filter filter, ReaderContext info)
    private DocIdSetIterator getDISI(Filter filter, AtomicReaderContext context)
     throws IOException {
        DocIdSet docIdSet = filter.getDocIdSet(info);
        DocIdSet docIdSet = filter.getDocIdSet(context);
         if (docIdSet == null) {
           return DocIdSet.EMPTY_DOCIDSET.iterator();
         } else {
@@ -124,10 +124,10 @@ public class ChainedFilter extends Filter
         }
     }
 
    private OpenBitSetDISI initialResult(ReaderContext info, int logic, int[] index)
    private OpenBitSetDISI initialResult(AtomicReaderContext context, int logic, int[] index)
     throws IOException
     {
        IndexReader reader = info.reader;
        IndexReader reader = context.reader;
         OpenBitSetDISI result;
         /**
          * First AND operation takes place against a completely false
@@ -135,12 +135,12 @@ public class ChainedFilter extends Filter
          */
         if (logic == AND)
         {
            result = new OpenBitSetDISI(getDISI(chain[index[0]], info), reader.maxDoc());
            result = new OpenBitSetDISI(getDISI(chain[index[0]], context), reader.maxDoc());
             ++index[0];
         }
         else if (logic == ANDNOT)
         {
            result = new OpenBitSetDISI(getDISI(chain[index[0]], info), reader.maxDoc());
            result = new OpenBitSetDISI(getDISI(chain[index[0]], context), reader.maxDoc());
             result.flip(0,reader.maxDoc()); // NOTE: may set bits for deleted docs.
             ++index[0];
         }
@@ -157,13 +157,13 @@ public class ChainedFilter extends Filter
      * @param logic Logical operation
      * @return DocIdSet
      */
    private DocIdSet getDocIdSet(ReaderContext info, int logic, int[] index)
    private DocIdSet getDocIdSet(AtomicReaderContext context, int logic, int[] index)
     throws IOException
     {
        OpenBitSetDISI result = initialResult(info, logic, index);
        OpenBitSetDISI result = initialResult(context, logic, index);
         for (; index[0] < chain.length; index[0]++)
         {
            doChain(result, logic, chain[index[0]].getDocIdSet(info));
            doChain(result, logic, chain[index[0]].getDocIdSet(context));
         }
         return result;
     }
@@ -174,7 +174,7 @@ public class ChainedFilter extends Filter
      * @param logic Logical operation
      * @return DocIdSet
      */
    private DocIdSet getDocIdSet(ReaderContext info, int[] logic, int[] index)
    private DocIdSet getDocIdSet(AtomicReaderContext info, int[] logic, int[] index)
     throws IOException
     {
         if (logic.length != chain.length)
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java
index 5f2e3b1528f..3b0c8de8296 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java
@@ -19,7 +19,7 @@ import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.TermsEnum;
@@ -72,7 +72,7 @@ public class DuplicateFilter extends Filter
 	}
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException
 	{
 		if(processingMode==PM_FAST_INVALIDATION)
 		{
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
index 6dee395ab70..5e83c75bc9e 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 import java.util.Comparator;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.BytesRef;
@@ -109,7 +109,7 @@ public final class FieldCacheRewriteMethod extends MultiTermQuery.RewriteMethod
      * results.
      */
     @Override
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
       final FieldCache.DocTermsIndex fcsi = FieldCache.DEFAULT.getTermsIndex(context.reader, query.field);
       final OpenBitSet termSet = new OpenBitSet(fcsi.numOrd());
       TermsEnum termsEnum = query.getTermsEnum(new Terms() {
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java
index e1ab950ab0c..f5c48c90fb2 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java
@@ -23,7 +23,7 @@ import java.util.Set;
 import java.util.TreeSet;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.Terms;
@@ -58,7 +58,7 @@ public class TermsFilter extends Filter
    * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader)
 	 */
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     IndexReader reader = context.reader;
     OpenBitSet result=new OpenBitSet(reader.maxDoc());
     Fields fields = reader.fields();
diff --git a/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java b/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java
index 27d76cdb8df..dfe6f8b8f6e 100644
-- a/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java
++ b/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java
@@ -21,7 +21,7 @@ import java.util.HashSet;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
@@ -60,7 +60,8 @@ public class TermsFilterTest extends LuceneTestCase {
 			w.addDocument(doc);			
 		}
 		IndexReader reader = new SlowMultiReaderWrapper(w.getReader());
		ReaderContext context = reader.getTopReaderContext();
		assertTrue(reader.getTopReaderContext().isAtomic);
		AtomicReaderContext context = (AtomicReaderContext) reader.getTopReaderContext();
 		assertTrue(context.isAtomic);
 		w.close();
 		
diff --git a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java
index 4f348b648d1..2751dbc9e34 100644
-- a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java
++ b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java
@@ -19,7 +19,7 @@ package org.apache.lucene.spatial.geohash;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.FieldCache.DocTerms;
 import org.apache.lucene.search.Filter;
@@ -62,7 +62,7 @@ public class GeoHashDistanceFilter extends DistanceFilter {
   }
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
 
     final DocTerms geoHashValues = FieldCache.DEFAULT.getTerms(context.reader, geoHashField);
     final BytesRef br = new BytesRef();
diff --git a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java
index 07a833f3e2e..6ee8fbeb771 100644
-- a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java
++ b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java
@@ -20,7 +20,7 @@ import java.io.IOException;
 import java.util.List;
 
 import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
@@ -45,7 +45,7 @@ public class CartesianShapeFilter extends Filter {
   }
   
   @Override
  public DocIdSet getDocIdSet(final ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(final AtomicReaderContext context) throws IOException {
     final Bits delDocs = context.reader.getDeletedDocs();
     final List<Double> area = shape.getArea();
     final int sz = area.size();
diff --git a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java
index 58b475bca60..94c3bd86ba0 100644
-- a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java
++ b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java
@@ -19,7 +19,7 @@ package org.apache.lucene.spatial.tier;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.FilteredDocIdSet;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.Filter;
@@ -65,7 +65,7 @@ public class LatLongDistanceFilter extends DistanceFilter {
   }
   
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
 
     final double[] latIndex = FieldCache.DEFAULT.getDoubles(context.reader, latField);
     final double[] lngIndex = FieldCache.DEFAULT.getDoubles(context.reader, lngField);
diff --git a/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java b/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java
index f355fab632d..f63e6acbcf3 100644
-- a/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java
++ b/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java
@@ -29,6 +29,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.QueryWrapperFilter;
 import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.lucene.store.Directory;
 
 
@@ -102,7 +103,7 @@ public class TestDistance extends LuceneTestCase {
     LatLongDistanceFilter f = new LatLongDistanceFilter(new QueryWrapperFilter(new MatchAllDocsQuery()),
                                                         lat, lng, 1.0, latField, lngField);
 
    AtomicReaderContext[] leaves = r.getTopReaderContext().leaves();
    AtomicReaderContext[] leaves = ReaderUtil.leaves(r.getTopReaderContext());
     for (int i = 0; i < leaves.length; i++) {
       f.getDocIdSet(leaves[i]);
     }
diff --git a/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java b/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java
index b6bea806dee..ea5f5741c34 100644
-- a/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java
++ b/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java
@@ -19,7 +19,7 @@ package org.apache.lucene.xmlparser.builders;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.NumericRangeFilter;
@@ -157,7 +157,7 @@ public class NumericRangeFilterBuilder implements FilterBuilder {
 		private static final long serialVersionUID = 1L;
 
 		@Override
		public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
		public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
 			return null;
 		}
 
diff --git a/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java b/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
index 4105c014f27..028cc752b85 100644
-- a/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
++ b/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
@@ -28,7 +28,9 @@ import javax.xml.parsers.ParserConfigurationException;
 import org.apache.lucene.util.LuceneTestCase;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SlowMultiReaderWrapper;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.NumericRangeFilter;
 import org.apache.lucene.store.Directory;
@@ -64,10 +66,10 @@ public class TestNumericRangeFilterBuilder extends LuceneTestCase {
 		writer.commit();
 		try
 		{
			IndexReader reader = IndexReader.open(ramDir, true);
			IndexReader reader = new SlowMultiReaderWrapper(IndexReader.open(ramDir, true));
 			try
 			{
				assertNull(filter.getDocIdSet(reader.getTopReaderContext()));
				assertNull(filter.getDocIdSet((AtomicReaderContext) reader.getTopReaderContext()));
 			}
 			finally
 			{
diff --git a/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java b/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java
index 8b4032a6602..d41abb8af0d 100644
-- a/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java
++ b/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java
@@ -26,7 +26,7 @@ import java.util.Map;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
@@ -365,9 +365,8 @@ class BufferedDeletes {
     // Delete by query
     if (deletes.queries.size() > 0) {
       IndexSearcher searcher = new IndexSearcher(reader);
      
      final ReaderContext readerContext = searcher.getTopReaderContext();
      assert readerContext.isAtomic;
      assert searcher.getTopReaderContext().isAtomic;
      final AtomicReaderContext readerContext = (AtomicReaderContext) searcher.getTopReaderContext();
       try {
         for (Entry<Query, Integer> entry : deletes.queries.entrySet()) {
           Query query = entry.getKey();
diff --git a/lucene/src/java/org/apache/lucene/search/BooleanQuery.java b/lucene/src/java/org/apache/lucene/search/BooleanQuery.java
index 65523a6da7d..65159d17569 100644
-- a/lucene/src/java/org/apache/lucene/search/BooleanQuery.java
++ b/lucene/src/java/org/apache/lucene/search/BooleanQuery.java
@@ -18,7 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.lucene.search.BooleanClause.Occur;
@@ -212,7 +212,7 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc)
    public Explanation explain(AtomicReaderContext context, int doc)
       throws IOException {
       final int minShouldMatch =
         BooleanQuery.this.getMinimumNumberShouldMatch();
@@ -288,7 +288,7 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer)
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer)
         throws IOException {
       List<Scorer> required = new ArrayList<Scorer>();
       List<Scorer> prohibited = new ArrayList<Scorer>();
diff --git a/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java b/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java
index 1939406749e..b1a2fa80767 100644
-- a/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java
++ b/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
 
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.Bits;
 
 import java.io.IOException;
@@ -61,7 +61,7 @@ public class CachingSpanFilter extends SpanFilter {
   }
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     SpanFilterResult result = getCachedResult(context.reader);
     return result != null ? result.getDocIdSet() : null;
   }
diff --git a/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java b/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java
index 1fc5c9f8b80..1f865670b56 100644
-- a/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java
++ b/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java
@@ -23,7 +23,7 @@ import java.util.Map;
 import java.util.WeakHashMap;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.OpenBitSetDISI;
 import org.apache.lucene.util.Bits;
 
@@ -195,7 +195,7 @@ public class CachingWrapperFilter extends Filter {
   int hitCount, missCount;
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     final IndexReader reader = context.reader;
     final Object coreKey = reader.getCoreCacheKey();
     final Object delCoreKey = reader.hasDeletions() ? reader.getDeletedDocs() : coreKey;
diff --git a/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java b/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
index 10090e98ee9..c8b8c9da180 100644
-- a/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
++ b/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
@@ -18,7 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 
@@ -133,7 +133,7 @@ public class ConstantScoreQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context,  boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context,  boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       final DocIdSetIterator disi;
       if (filter != null) {
         assert query == null;
@@ -157,7 +157,7 @@ public class ConstantScoreQuery extends Query {
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
       final Scorer cs = scorer(context, true, false);
       final boolean exists = (cs != null && cs.advance(doc) == doc);
 
diff --git a/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java b/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
index 83f7764776f..f7a797eec20 100644
-- a/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
++ b/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
@@ -23,7 +23,7 @@ import java.util.Iterator;
 import java.util.Set;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 
 /**
@@ -142,7 +142,7 @@ public class DisjunctionMaxQuery extends Query implements Iterable<Query> {
 
     /* Create the scorer used to score our associated DisjunctionMaxQuery */
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
         boolean topScorer) throws IOException {
       Scorer[] scorers = new Scorer[weights.size()];
       int idx = 0;
@@ -159,7 +159,7 @@ public class DisjunctionMaxQuery extends Query implements Iterable<Query> {
 
     /* Explain the score we computed for doc */
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
       if (disjuncts.size() == 1) return weights.get(0).explain(context,doc);
       ComplexExplanation result = new ComplexExplanation();
       float max = 0.0f, sum = 0.0f;
diff --git a/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java b/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java
index e0a03169285..9293e509608 100644
-- a/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java
++ b/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.util.NumericUtils;
 import org.apache.lucene.util.Bits;
@@ -74,7 +74,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   
   /** This method is implemented for each data type */
   @Override
  public abstract DocIdSet getDocIdSet(ReaderContext context) throws IOException;
  public abstract DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException;
 
   /**
    * Creates a string range filter using {@link FieldCache#getTermsIndex}. This works with all
@@ -84,7 +84,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<String> newStringRange(String field, String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<String>(field, null, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         final FieldCache.DocTermsIndex fcsi = FieldCache.DEFAULT.getTermsIndex(context.reader, field);
         final BytesRef spare = new BytesRef();
         final int lowerPoint = fcsi.binarySearchLookup(lowerVal == null ? null : new BytesRef(lowerVal), spare);
@@ -153,7 +153,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Byte> newByteRange(String field, FieldCache.ByteParser parser, Byte lowerVal, Byte upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Byte>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         final byte inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           final byte i = lowerVal.byteValue();
@@ -204,7 +204,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Short> newShortRange(String field, FieldCache.ShortParser parser, Short lowerVal, Short upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Short>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         final short inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           short i = lowerVal.shortValue();
@@ -255,7 +255,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Integer> newIntRange(String field, FieldCache.IntParser parser, Integer lowerVal, Integer upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Integer>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         final int inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           int i = lowerVal.intValue();
@@ -306,7 +306,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Long> newLongRange(String field, FieldCache.LongParser parser, Long lowerVal, Long upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Long>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         final long inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           long i = lowerVal.longValue();
@@ -357,7 +357,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Float> newFloatRange(String field, FieldCache.FloatParser parser, Float lowerVal, Float upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Float>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         // we transform the floating point numbers to sortable integers
         // using NumericUtils to easier find the next bigger/lower value
         final float inclusiveLowerPoint, inclusiveUpperPoint;
@@ -412,7 +412,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Double> newDoubleRange(String field, FieldCache.DoubleParser parser, Double lowerVal, Double upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Double>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         // we transform the floating point numbers to sortable integers
         // using NumericUtils to easier find the next bigger/lower value
         final double inclusiveLowerPoint, inclusiveUpperPoint;
diff --git a/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java b/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java
index 684b139e4cf..7c9099b6d2c 100644
-- a/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java
++ b/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 
 import org.apache.lucene.index.DocsEnum; // javadoc @link
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.BytesRef;
 
@@ -116,7 +116,7 @@ public class FieldCacheTermsFilter extends Filter {
   }
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     return new FieldCacheTermsFilterDocIdSet(getFieldCache().getTermsIndex(context.reader, field));
   }
 
diff --git a/lucene/src/java/org/apache/lucene/search/Filter.java b/lucene/src/java/org/apache/lucene/search/Filter.java
index 2dea148e94e..f4404c71860 100644
-- a/lucene/src/java/org/apache/lucene/search/Filter.java
++ b/lucene/src/java/org/apache/lucene/search/Filter.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.DocIdBitSet;
 
 /** 
@@ -38,7 +38,7 @@ public abstract class Filter implements java.io.Serializable {
    * must refer to document IDs for that segment, not for
    * the top-level reader.
    * 
   * @param context a {@link ReaderContext} instance opened on the index currently
   * @param context a {@link AtomicReaderContext} instance opened on the index currently
    *         searched on. Note, it is likely that the provided reader info does not
    *         represent the whole underlying index i.e. if the index has more than
    *         one segment the given reader only represents a single segment.
@@ -52,6 +52,5 @@ public abstract class Filter implements java.io.Serializable {
    * 
    * @see DocIdBitSet
    */
  // TODO make this context an AtomicContext
  public abstract DocIdSet getDocIdSet(ReaderContext context) throws IOException;
  public abstract DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException;
 }
diff --git a/lucene/src/java/org/apache/lucene/search/FilteredQuery.java b/lucene/src/java/org/apache/lucene/search/FilteredQuery.java
index f0b6001665f..20ba46fa3b6 100644
-- a/lucene/src/java/org/apache/lucene/search/FilteredQuery.java
++ b/lucene/src/java/org/apache/lucene/search/FilteredQuery.java
@@ -18,7 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 
@@ -82,7 +82,7 @@ extends Query {
       }
 
       @Override
      public Explanation explain (ReaderContext ir, int i) throws IOException {
      public Explanation explain (AtomicReaderContext ir, int i) throws IOException {
         Explanation inner = weight.explain (ir, i);
         if (getBoost()!=1) {
           Explanation preBoost = inner;
@@ -112,7 +112,7 @@ extends Query {
 
       // return a filtering scorer
       @Override
      public Scorer scorer(ReaderContext indexReader, boolean scoreDocsInOrder, boolean topScorer)
      public Scorer scorer(AtomicReaderContext indexReader, boolean scoreDocsInOrder, boolean topScorer)
           throws IOException {
         final Scorer scorer = weight.scorer(indexReader, true, false);
         if (scorer == null) {
diff --git a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
index 2fdff34ad13..0dd8dfb85e3 100644
-- a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
++ b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
@@ -145,7 +145,7 @@ public class IndexSearcher {
    * @lucene.experimental
    */
   public IndexSearcher(ReaderContext context) {
    this(context, null);
    this(context, (ExecutorService) null);
   }
   
   // convenience ctor for other IR based ctors
@@ -159,14 +159,8 @@ public class IndexSearcher {
     this.executor = executor;
     this.closeReader = closeReader;
     this.readerContext = context;
    if (context.isAtomic) {
      assert context.leaves() == null : "AtomicReaderContext must not have any leaves";
      this.leafContexts = new AtomicReaderContext[] { (AtomicReaderContext) context };
    } else {
      assert context.leaves() != null : "non-atomic top-level context must have leaves";
      this.leafContexts = context.leaves();
    }

    leafContexts = ReaderUtil.leaves(context);
    
     if (executor == null) {
       subSearchers = null;
     } else {
@@ -175,12 +169,25 @@ public class IndexSearcher {
         if (leafContexts[i].reader == context.reader) {
           subSearchers[i] = this;
         } else {
          subSearchers[i] = new IndexSearcher(leafContexts[i].reader.getTopReaderContext()); // we need to get a TL context for sub searchers!
          subSearchers[i] = new IndexSearcher(context, leafContexts[i]);
         }
       }
     }
   }

  
  /* Ctor for concurrent sub-searchers searching only on a specific leaf of the given top-reader context
   * - instead of searching over all leaves this searcher only searches a single leaf searcher slice. Hence, 
   * for scorer and filter this looks like an ordinary search in the hierarchy such that there is no difference
   * between single and multi-threaded */
  private IndexSearcher(ReaderContext topLevel, AtomicReaderContext leaf) {
    readerContext = topLevel;
    reader = topLevel.reader;
    leafContexts = new AtomicReaderContext[] {leaf};
    executor = null;
    subSearchers = null;
    closeReader = false;
  }
  
   /** Return the {@link IndexReader} this searches. */
   public IndexReader getIndexReader() {
     return reader;
@@ -365,7 +372,7 @@ public class IndexSearcher {
     
       for (int i = 0; i < subSearchers.length; i++) { // search each sub
         runner.submit(
                      new MultiSearcherCallableNoSort(lock, subSearchers[i], weight, filter, nDocs, hq, leafContexts[i].docBase));
                      new SearcherCallableNoSort(lock, subSearchers[i], weight, filter, nDocs, hq));
       }
 
       int totalHits = 0;
@@ -434,7 +441,7 @@ public class IndexSearcher {
       final ExecutionHelper<TopFieldDocs> runner = new ExecutionHelper<TopFieldDocs>(executor);
       for (int i = 0; i < subSearchers.length; i++) { // search each sub
         runner.submit(
                      new MultiSearcherCallableWithSort(lock, subSearchers[i], weight, filter, nDocs, hq, sort, leafContexts[i].docBase));
                      new SearcherCallableWithSort(lock, subSearchers[i], weight, filter, nDocs, hq, sort));
       }
       int totalHits = 0;
       float maxScore = Float.NEGATIVE_INFINITY;
@@ -493,7 +500,7 @@ public class IndexSearcher {
     }
   }
 
  private void searchWithFilter(ReaderContext context, Weight weight,
  private void searchWithFilter(AtomicReaderContext context, Weight weight,
       final Filter filter, final Collector collector) throws IOException {
 
     assert filter != null;
@@ -621,7 +628,7 @@ public class IndexSearcher {
   /**
    * A thread subclass for searching a single searchable 
    */
  private static final class MultiSearcherCallableNoSort implements Callable<TopDocs> {
  private static final class SearcherCallableNoSort implements Callable<TopDocs> {
 
     private final Lock lock;
     private final IndexSearcher searchable;
@@ -629,17 +636,15 @@ public class IndexSearcher {
     private final Filter filter;
     private final int nDocs;
     private final HitQueue hq;
    private final int docBase;
 
    public MultiSearcherCallableNoSort(Lock lock, IndexSearcher searchable, Weight weight,
        Filter filter, int nDocs, HitQueue hq, int docBase) {
    public SearcherCallableNoSort(Lock lock, IndexSearcher searchable, Weight weight,
        Filter filter, int nDocs, HitQueue hq) {
       this.lock = lock;
       this.searchable = searchable;
       this.weight = weight;
       this.filter = filter;
       this.nDocs = nDocs;
       this.hq = hq;
      this.docBase = docBase;
     }
 
     public TopDocs call() throws IOException {
@@ -647,7 +652,6 @@ public class IndexSearcher {
       final ScoreDoc[] scoreDocs = docs.scoreDocs;
       for (int j = 0; j < scoreDocs.length; j++) { // merge scoreDocs into hq
         final ScoreDoc scoreDoc = scoreDocs[j];
        scoreDoc.doc += docBase; // convert doc 
         //it would be so nice if we had a thread-safe insert 
         lock.lock();
         try {
@@ -665,7 +669,7 @@ public class IndexSearcher {
   /**
    * A thread subclass for searching a single searchable 
    */
  private static final class MultiSearcherCallableWithSort implements Callable<TopFieldDocs> {
  private static final class SearcherCallableWithSort implements Callable<TopFieldDocs> {
 
     private final Lock lock;
     private final IndexSearcher searchable;
@@ -673,37 +677,21 @@ public class IndexSearcher {
     private final Filter filter;
     private final int nDocs;
     private final FieldDocSortedHitQueue hq;
    private final int docBase;
     private final Sort sort;
 
    public MultiSearcherCallableWithSort(Lock lock, IndexSearcher searchable, Weight weight,
        Filter filter, int nDocs, FieldDocSortedHitQueue hq, Sort sort, int docBase) {
    public SearcherCallableWithSort(Lock lock, IndexSearcher searchable, Weight weight,
        Filter filter, int nDocs, FieldDocSortedHitQueue hq, Sort sort) {
       this.lock = lock;
       this.searchable = searchable;
       this.weight = weight;
       this.filter = filter;
       this.nDocs = nDocs;
       this.hq = hq;
      this.docBase = docBase;
       this.sort = sort;
     }
 
     public TopFieldDocs call() throws IOException {
       final TopFieldDocs docs = searchable.search (weight, filter, nDocs, sort);
      // If one of the Sort fields is FIELD_DOC, need to fix its values, so that
      // it will break ties by doc Id properly. Otherwise, it will compare to
      // 'relative' doc Ids, that belong to two different searchables.
      for (int j = 0; j < docs.fields.length; j++) {
        if (docs.fields[j].getType() == SortField.DOC) {
          // iterate over the score docs and change their fields value
          for (int j2 = 0; j2 < docs.scoreDocs.length; j2++) {
            FieldDoc fd = (FieldDoc) docs.scoreDocs[j2];
            fd.fields[j] = Integer.valueOf(((Integer) fd.fields[j]).intValue() + docBase);
          }
          break;
        }
      }

       lock.lock();
       try {
         hq.setFields(docs.fields);
@@ -714,7 +702,6 @@ public class IndexSearcher {
       final ScoreDoc[] scoreDocs = docs.scoreDocs;
       for (int j = 0; j < scoreDocs.length; j++) { // merge scoreDocs into hq
         final FieldDoc fieldDoc = (FieldDoc) scoreDocs[j];
        fieldDoc.doc += docBase; // convert doc 
         //it would be so nice if we had a thread-safe insert 
         lock.lock();
         try {
diff --git a/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java b/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
index 38625194474..f3bad0cdc17 100644
-- a/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
++ b/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
@@ -18,7 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.lucene.util.Bits;
@@ -127,13 +127,13 @@ public class MatchAllDocsQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       return new MatchAllScorer(context.reader, similarity, this,
           normsField != null ? context.reader.norms(normsField) : null);
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc) {
    public Explanation explain(AtomicReaderContext context, int doc) {
       // explain query weight
       Explanation queryExpl = new ComplexExplanation
         (true, getValue(), "MatchAllDocsQuery, product of:");
diff --git a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
index 42b2086ea3c..4aa6cfc914d 100644
-- a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 import java.util.*;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.DocsAndPositionsEnum;
@@ -168,7 +168,7 @@ public class MultiPhraseQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       if (termArrays.size() == 0)                  // optimize zero-term case
         return null;
       final IndexReader reader = context.reader;
@@ -233,7 +233,7 @@ public class MultiPhraseQuery extends Query {
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc)
    public Explanation explain(AtomicReaderContext context, int doc)
       throws IOException {
       ComplexExplanation result = new ComplexExplanation();
       result.setDescription("weight("+getQuery()+" in "+doc+"), product of:");
diff --git a/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java b/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java
index 6d591c8a984..8a6df063b83 100644
-- a/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java
++ b/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 
 import org.apache.lucene.index.Fields;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.index.DocsEnum;
@@ -105,7 +105,7 @@ public class MultiTermQueryWrapperFilter<Q extends MultiTermQuery> extends Filte
    * results.
    */
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     final IndexReader reader = context.reader;
     final Fields fields = reader.fields();
     if (fields == null) {
diff --git a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
index 7142461ef25..c465f3cb023 100644
-- a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 import java.util.Set;
 import java.util.ArrayList;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsAndPositionsEnum;
 import org.apache.lucene.index.IndexReader;
@@ -175,7 +175,7 @@ public class PhraseQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       if (terms.size() == 0)			  // optimize zero-term case
         return null;
       final IndexReader reader = context.reader;
@@ -221,7 +221,7 @@ public class PhraseQuery extends Query {
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc)
    public Explanation explain(AtomicReaderContext context, int doc)
       throws IOException {
 
       Explanation result = new Explanation();
diff --git a/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java b/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java
index a2c2c29eaa1..31077e46683 100644
-- a/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java
++ b/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java
@@ -18,7 +18,8 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import org.apache.lucene.index.IndexReader.ReaderContext;

import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 /** 
  * Constrains search results to only match those which also match a provided
@@ -46,9 +47,10 @@ public class QueryWrapperFilter extends Filter {
   }
 
   @Override
  public DocIdSet getDocIdSet(final ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(final AtomicReaderContext context) throws IOException {
     // get a private context that is used to rewrite, createWeight and score eventually
    final ReaderContext privateContext = context.reader.getTopReaderContext();
    assert context.reader.getTopReaderContext().isAtomic;
    final AtomicReaderContext privateContext = (AtomicReaderContext) context.reader.getTopReaderContext();
     final Weight weight = query.weight(new IndexSearcher(privateContext));
     return new DocIdSet() {
       @Override
diff --git a/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java b/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java
index 68649be097a..6c12dac39b7 100644
-- a/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java
++ b/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
 
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.spans.SpanQuery;
 import org.apache.lucene.search.spans.Spans;
 import org.apache.lucene.util.OpenBitSet;
@@ -53,7 +53,7 @@ public class SpanQueryFilter extends SpanFilter {
   }
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     SpanFilterResult result = bitSpans(context.reader);
     return result.getDocIdSet();
   }
diff --git a/lucene/src/java/org/apache/lucene/search/TermQuery.java b/lucene/src/java/org/apache/lucene/search/TermQuery.java
index bf8346cfb50..5ed96f8ddca 100644
-- a/lucene/src/java/org/apache/lucene/search/TermQuery.java
++ b/lucene/src/java/org/apache/lucene/search/TermQuery.java
@@ -42,9 +42,11 @@ public class TermQuery extends Query {
     private float queryNorm;
     private float queryWeight;
     private IDFExplanation idfExp;
    private transient ReaderContext weightContext; // only set if -ea for assert in scorer()
 
     public TermWeight(IndexSearcher searcher)
       throws IOException {
      assert setWeightContext(searcher);
       this.similarity = getSimilarity(searcher);
       if (docFreq != -1) {
         idfExp = similarity.idfExplain(term, searcher, docFreq);
@@ -77,7 +79,8 @@ public class TermQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      assert assertTopReaderContext(context);
       final IndexReader reader = context.reader;
       DocsEnum docs = reader.termDocsEnum(reader.getDeletedDocs(),
                                           term.field(),
@@ -89,9 +92,21 @@ public class TermQuery extends Query {
 
       return new TermScorer(this, docs, similarity, reader.norms(term.field()));
     }
    
    private boolean assertTopReaderContext(ReaderContext context) {
      while (context.parent != null) {
        context = context.parent;
      }
      return weightContext == context;
    }
    
    private boolean setWeightContext(IndexSearcher searcher) {
      weightContext = searcher.getTopReaderContext();
      return true;
    }
 
     @Override
    public Explanation explain(ReaderContext context, int doc)
    public Explanation explain(AtomicReaderContext context, int doc)
       throws IOException {
       final IndexReader reader = context.reader;
 
diff --git a/lucene/src/java/org/apache/lucene/search/Weight.java b/lucene/src/java/org/apache/lucene/search/Weight.java
index 016904eb833..e649530e530 100644
-- a/lucene/src/java/org/apache/lucene/search/Weight.java
++ b/lucene/src/java/org/apache/lucene/search/Weight.java
@@ -34,12 +34,9 @@ import org.apache.lucene.index.IndexReader.ReaderContext;
  * {@link IndexReader} dependent state should reside in the {@link Scorer}.
  * <p>
  * Since {@link Weight} creates {@link Scorer} instances for a given
 * {@link ReaderContext} ({@link #scorer(ReaderContext, boolean, boolean)})
 * {@link AtomicReaderContext} ({@link #scorer(AtomicReaderContext, boolean, boolean)})
  * callers must maintain the relationship between the searcher's top-level
 * {@link ReaderContext} and the context used to create a {@link Scorer}. A
 * {@link ReaderContext} used to create a {@link Scorer} should be a leaf
 * context ({@link AtomicReaderContext}) of the searcher's top-level context,
 * otherwise the scorer's state will be undefined. 
 * {@link ReaderContext} and the context used to create a {@link Scorer}. 
  * <p>
  * A <code>Weight</code> is used in the following way:
  * <ol>
@@ -52,10 +49,9 @@ import org.apache.lucene.index.IndexReader.ReaderContext;
  * <li>The query normalization factor is passed to {@link #normalize(float)}. At
  * this point the weighting is complete.
  * <li>A <code>Scorer</code> is constructed by
 * {@link #scorer(ReaderContext,boolean,boolean)}.
 * {@link #scorer(AtomicReaderContext,boolean,boolean)}.
  * </ol>
  * 
 * 
  * @since 2.9
  */
 public abstract class Weight implements Serializable {
@@ -68,7 +64,7 @@ public abstract class Weight implements Serializable {
    * @return an Explanation for the score
    * @throws IOException if an {@link IOException} occurs
    */
  public abstract Explanation explain(ReaderContext context, int doc) throws IOException;
  public abstract Explanation explain(AtomicReaderContext context, int doc) throws IOException;
 
   /** The query that this concerns. */
   public abstract Query getQuery();
@@ -90,12 +86,9 @@ public abstract class Weight implements Serializable {
    * in-order.<br>
    * <b>NOTE:</b> null can be returned if no documents will be scored by this
    * query.
   * <b>NOTE: Calling this method with a {@link ReaderContext} that is not a
   * leaf context ({@link AtomicReaderContext}) of the searcher's top-level context 
   * used to create this {@link Weight} instance can cause undefined behavior.
    * 
    * @param context
   *          the {@link ReaderContext} for which to return the {@link Scorer}.
   *          the {@link AtomicReaderContext} for which to return the {@link Scorer}.
    * @param scoreDocsInOrder
    *          specifies whether in-order scoring of documents is required. Note
    *          that if set to false (i.e., out-of-order scoring is required),
@@ -111,8 +104,7 @@ public abstract class Weight implements Serializable {
    * @return a {@link Scorer} which scores documents in/out-of order.
    * @throws IOException
    */
  // TODO make this context an AtomicContext if possible
  public abstract Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
  public abstract Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
       boolean topScorer) throws IOException;
   
   /** The sum of squared weights of contained query clauses. */
@@ -122,7 +114,7 @@ public abstract class Weight implements Serializable {
    * Returns true iff this implementation scores docs only out of order. This
    * method is used in conjunction with {@link Collector}'s
    * {@link Collector#acceptsDocsOutOfOrder() acceptsDocsOutOfOrder} and
   * {@link #scorer(ReaderContext, boolean, boolean)} to
   * {@link #scorer(AtomicReaderContext, boolean, boolean)} to
    * create a matching {@link Scorer} instance for a given {@link Collector}, or
    * vice versa.
    * <p>
diff --git a/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java b/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java
index ecea73df689..fd3bc3e81fb 100755
-- a/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java
++ b/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java
@@ -22,7 +22,7 @@ import java.util.Set;
 import java.util.Arrays;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.ComplexExplanation;
 import org.apache.lucene.search.Explanation;
@@ -240,7 +240,7 @@ public class CustomScoreQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       // Pass true for "scoresDocsInOrder", because we
       // require in-order scoring, even if caller does not,
       // since we call advance on the valSrcScorers.  Pass
@@ -258,12 +258,12 @@ public class CustomScoreQuery extends Query {
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
       Explanation explain = doExplain(context, doc);
       return explain == null ? new Explanation(0.0f, "no matching docs") : explain;
     }
     
    private Explanation doExplain(ReaderContext info, int doc) throws IOException {
    private Explanation doExplain(AtomicReaderContext info, int doc) throws IOException {
       Explanation subQueryExpl = subQueryWeight.explain(info, doc);
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
diff --git a/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java b/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
index 25af66e85d9..1a3f7706e67 100644
-- a/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
++ b/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search.function;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.lucene.util.Bits;
@@ -99,13 +99,13 @@ public class ValueSourceQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       return new ValueSourceScorer(similarity, context.reader, this);
     }
 
     /*(non-Javadoc) @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int) */
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
       DocValues vals = valSrc.getValues(context.reader);
       float sc = queryWeight * vals.floatVal(doc);
 
diff --git a/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java b/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java
index 1bb3b7a5f95..2504e7ed28b 100644
-- a/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java
++ b/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search.payloads;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.Explanation;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.IndexSearcher;
@@ -143,7 +143,7 @@ public class PayloadNearQuery extends SpanNearQuery {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
         boolean topScorer) throws IOException {
       return new PayloadNearSpanScorer(query.getSpans(context.reader), this,
           similarity, context.reader.norms(query.getField()));
diff --git a/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java b/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java
index 4aa29583473..048f3affa30 100644
-- a/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java
++ b/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search.payloads;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsAndPositionsEnum;
 import org.apache.lucene.search.IndexSearcher;
@@ -74,7 +74,7 @@ public class PayloadTermQuery extends SpanTermQuery {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
         boolean topScorer) throws IOException {
       return new PayloadTermSpanScorer((TermSpans) query.getSpans(context.reader),
           this, similarity, context.reader.norms(query.getField()));
diff --git a/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java b/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java
index 6142ad42453..4b360cfb8ea 100644
-- a/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java
++ b/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search.spans;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.Explanation.IDFExplanation;
@@ -72,13 +72,13 @@ public class SpanWeight extends Weight {
   }
 
   @Override
  public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
  public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
     return new SpanScorer(query.getSpans(context.reader), this, similarity, context.reader
         .norms(query.getField()));
   }
 
   @Override
  public Explanation explain(ReaderContext context, int doc)
  public Explanation explain(AtomicReaderContext context, int doc)
     throws IOException {
 
     ComplexExplanation result = new ComplexExplanation();
diff --git a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
index 701cfee2368..e1533433c1b 100644
-- a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
++ b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
@@ -213,6 +213,21 @@ public final class ReaderUtil {
     
   }
 
  /**
   * Returns the context's leaves or the context itself as the only element of
   * the returned array. If the context's #leaves() method returns
   * <code>null</code> the given context must be an instance of
   * {@link AtomicReaderContext}
   */
  public static AtomicReaderContext[] leaves(ReaderContext context) {
    assert context != null && context.isTopLevel : "context must be non-null & top-level";
    final AtomicReaderContext[] leaves = context.leaves();
    if (leaves == null) {
      assert context.isAtomic : "top-level context without leaves must be atomic";
      return new AtomicReaderContext[] { (AtomicReaderContext) context };
    }
    return leaves;
  }
 
   /**
    * Returns index of the searcher/reader for document <code>n</code> in the
diff --git a/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java b/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java
index 5acd441b18d..41872acd8bd 100644
-- a/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java
++ b/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java
@@ -21,7 +21,7 @@ import java.io.IOException;
 
 import junit.framework.Assert;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 /**
  * A unit test helper class to test when the filter is getting cached and when it is not.
@@ -42,7 +42,7 @@ public class CachingWrapperFilterHelper extends CachingWrapperFilter {
   }
   
   @Override
  public synchronized DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public synchronized DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
 
     final int saveMissCount = missCount;
     DocIdSet docIdSet = super.getDocIdSet(context);
diff --git a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
index f0488f5b7d4..2e43904b584 100644
-- a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
++ b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
@@ -20,7 +20,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.index.FieldInvertState;
 import org.apache.lucene.util.PriorityQueue;
@@ -154,7 +154,7 @@ final class JustCompileSearch {
     // still added here in case someone will add abstract methods in the future.
     
     @Override
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
       return null;
     }
   }
@@ -283,7 +283,7 @@ final class JustCompileSearch {
     }
     
     @Override
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
       return null;
     }    
   }
@@ -335,7 +335,7 @@ final class JustCompileSearch {
   static final class JustCompileWeight extends Weight {
 
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
 
@@ -360,7 +360,7 @@ final class JustCompileSearch {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer)
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer)
         throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
diff --git a/lucene/src/test/org/apache/lucene/search/MockFilter.java b/lucene/src/test/org/apache/lucene/search/MockFilter.java
index 1ac9207e9ef..1152db0f3d8 100644
-- a/lucene/src/test/org/apache/lucene/search/MockFilter.java
++ b/lucene/src/test/org/apache/lucene/search/MockFilter.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.DocIdBitSet;
 import java.util.BitSet;
 
@@ -25,7 +25,7 @@ public class MockFilter extends Filter {
   private boolean wasCalled;
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) {
  public DocIdSet getDocIdSet(AtomicReaderContext context) {
     wasCalled = true;
     return new DocIdBitSet(new BitSet());
   }
diff --git a/lucene/src/test/org/apache/lucene/search/QueryUtils.java b/lucene/src/test/org/apache/lucene/search/QueryUtils.java
index fd52b748a74..27dda23ce40 100644
-- a/lucene/src/test/org/apache/lucene/search/QueryUtils.java
++ b/lucene/src/test/org/apache/lucene/search/QueryUtils.java
@@ -13,13 +13,13 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.MultiReader;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.MockDirectoryWrapper;
 import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.ReaderUtil;
 
 import static org.apache.lucene.util.LuceneTestCase.TEST_VERSION_CURRENT;
 
@@ -213,21 +213,14 @@ public class QueryUtils {
     }
   }
   
  private static AtomicReaderContext[] getLeaves(IndexSearcher searcher) {
    ReaderContext topLevelReaderContext = searcher.getTopReaderContext();
    if (topLevelReaderContext.isAtomic) {
      return new AtomicReaderContext[] {(AtomicReaderContext) topLevelReaderContext};
    } else {
      return topLevelReaderContext.leaves();
    }
  }

 
   /** alternate scorer skipTo(),skipTo(),next(),next(),skipTo(),skipTo(), etc
    * and ensure a hitcollector receives same docs and scores
    */
   public static void checkSkipTo(final Query q, final IndexSearcher s) throws IOException {
     //System.out.println("Checking "+q);
    final AtomicReaderContext[] context = getLeaves(s);
    final AtomicReaderContext[] context = ReaderUtil.leaves(s.getTopReaderContext());
     if (q.weight(s).scoresDocsOutOfOrder()) return;  // in this case order of skipTo() might differ from that of next().
 
     final int skip_op = 0;
@@ -317,7 +310,7 @@ public class QueryUtils {
               final IndexReader previousReader = lastReader[0];
               IndexSearcher indexSearcher = new IndexSearcher(previousReader);
               Weight w = q.weight(indexSearcher);
              Scorer scorer = w.scorer(indexSearcher.getTopReaderContext(), true, false);
              Scorer scorer = w.scorer((AtomicReaderContext)indexSearcher.getTopReaderContext(), true, false);
               if (scorer != null) {
                 boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
                 Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
@@ -342,7 +335,7 @@ public class QueryUtils {
           final IndexReader previousReader = lastReader[0];
           IndexSearcher indexSearcher = new IndexSearcher(previousReader);
           Weight w = q.weight(indexSearcher);
          Scorer scorer = w.scorer(previousReader.getTopReaderContext() , true, false);
          Scorer scorer = w.scorer((AtomicReaderContext)previousReader.getTopReaderContext() , true, false);
           if (scorer != null) {
             boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
             Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
@@ -357,7 +350,7 @@ public class QueryUtils {
     final float maxDiff = 1e-3f;
     final int lastDoc[] = {-1};
     final IndexReader lastReader[] = {null};
    final ReaderContext[] context = getLeaves(s);
    final AtomicReaderContext[] context = ReaderUtil.leaves(s.getTopReaderContext());
     s.search(q,new Collector() {
       private Scorer scorer;
       private int leafPtr;
@@ -399,7 +392,7 @@ public class QueryUtils {
           final IndexReader previousReader = lastReader[0];
           IndexSearcher indexSearcher = new IndexSearcher(previousReader);
           Weight w = q.weight(indexSearcher);
          Scorer scorer = w.scorer(indexSearcher.getTopReaderContext(), true, false);
          Scorer scorer = w.scorer((AtomicReaderContext)indexSearcher.getTopReaderContext(), true, false);
           if (scorer != null) {
             boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
             Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
@@ -422,7 +415,7 @@ public class QueryUtils {
       final IndexReader previousReader = lastReader[0];
       IndexSearcher indexSearcher = new IndexSearcher(previousReader);
       Weight w = q.weight(indexSearcher);
      Scorer scorer = w.scorer(indexSearcher.getTopReaderContext(), true, false);
      Scorer scorer = w.scorer((AtomicReaderContext)indexSearcher.getTopReaderContext(), true, false);
       if (scorer != null) {
         boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
         Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
diff --git a/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java b/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java
index 2625cda5b67..a33a6c178a2 100644
-- a/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java
++ b/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.DocIdBitSet;
 
 import java.util.BitSet;
@@ -31,7 +31,7 @@ public class SingleDocTestFilter extends Filter {
   }
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     BitSet bits = new BitSet(context.reader.maxDoc());
     bits.set(doc);
     return new DocIdBitSet(bits);
diff --git a/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java b/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java
index a38fe553a26..36c6dfeeef8 100644
-- a/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java
++ b/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java
@@ -24,7 +24,6 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.SerialMergeScheduler;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
@@ -41,8 +40,8 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     RandomIndexWriter writer = new RandomIndexWriter(random, dir);
     writer.close();
 
    IndexReader reader = IndexReader.open(dir, true);
    ReaderContext context = reader.getTopReaderContext();
    IndexReader reader = new SlowMultiReaderWrapper(IndexReader.open(dir, true));
    AtomicReaderContext context = (AtomicReaderContext) reader.getTopReaderContext();
     MockFilter filter = new MockFilter();
     CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
 
@@ -67,12 +66,12 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     RandomIndexWriter writer = new RandomIndexWriter(random, dir);
     writer.close();
 
    IndexReader reader = IndexReader.open(dir, true);
    ReaderContext context = reader.getTopReaderContext();
    IndexReader reader = new SlowMultiReaderWrapper(IndexReader.open(dir, true));
    AtomicReaderContext context = (AtomicReaderContext) reader.getTopReaderContext();
 
     final Filter filter = new Filter() {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) {
      public DocIdSet getDocIdSet(AtomicReaderContext context) {
         return null;
       }
     };
@@ -90,12 +89,12 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     RandomIndexWriter writer = new RandomIndexWriter(random, dir);
     writer.close();
 
    IndexReader reader = IndexReader.open(dir, true);
    ReaderContext context = reader.getTopReaderContext();
    IndexReader reader = new SlowMultiReaderWrapper(IndexReader.open(dir, true));
    AtomicReaderContext context = (AtomicReaderContext) reader.getTopReaderContext();
 
     final Filter filter = new Filter() {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) {
      public DocIdSet getDocIdSet(AtomicReaderContext context) {
         return new DocIdSet() {
           @Override
           public DocIdSetIterator iterator() {
@@ -114,7 +113,8 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
   }
   
   private static void assertDocIdSetCacheable(IndexReader reader, Filter filter, boolean shouldCacheable) throws IOException {
    ReaderContext context = reader.getTopReaderContext();
    assertTrue(reader.getTopReaderContext().isAtomic);
    AtomicReaderContext context = (AtomicReaderContext) reader.getTopReaderContext();
     final CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
     final DocIdSet originalSet = filter.getDocIdSet(context);
     final DocIdSet cachedSet = cacher.getDocIdSet(context);
@@ -145,7 +145,7 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     // a openbitset filter is always cacheable
     assertDocIdSetCacheable(reader, new Filter() {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) {
      public DocIdSet getDocIdSet(AtomicReaderContext context) {
         return new OpenBitSet();
       }
     }, true);
diff --git a/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java b/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java
index aa7b9faaff5..595c18c9762 100644
-- a/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java
@@ -22,11 +22,11 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
 import org.apache.lucene.index.FieldInvertState;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.store.Directory;
 
 import java.text.DecimalFormat;
@@ -168,7 +168,7 @@ public class TestDisjunctionMaxQuery extends LuceneTestCase {
     QueryUtils.check(random, dq, s);
     assertTrue(s.getTopReaderContext().isAtomic);
     final Weight dw = dq.weight(s);
    final Scorer ds = dw.scorer(s.getTopReaderContext(), true, false);
    final Scorer ds = dw.scorer((AtomicReaderContext)s.getTopReaderContext(), true, false);
     final boolean skipOk = ds.advance(3) != DocIdSetIterator.NO_MORE_DOCS;
     if (skipOk) {
       fail("firsttime skipTo found a match? ... "
@@ -183,7 +183,7 @@ public class TestDisjunctionMaxQuery extends LuceneTestCase {
     assertTrue(s.getTopReaderContext().isAtomic);
     QueryUtils.check(random, dq, s);
     final Weight dw = dq.weight(s);
    final Scorer ds = dw.scorer(s.getTopReaderContext(), true, false);
    final Scorer ds = dw.scorer((AtomicReaderContext)s.getTopReaderContext(), true, false);
     assertTrue("firsttime skipTo found no match",
         ds.advance(3) != DocIdSetIterator.NO_MORE_DOCS);
     assertEquals("found wrong docid", "d4", r.document(ds.docID()).get("id"));
diff --git a/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java b/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java
index 5ff89d5c888..6ca1192b25c 100644
-- a/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java
++ b/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java
@@ -28,7 +28,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field.Index;
 import org.apache.lucene.document.Field.Store;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.LuceneTestCase;
@@ -115,7 +115,7 @@ public class TestDocIdSet extends LuceneTestCase {
     // Now search w/ a Filter which returns a null DocIdSet
     Filter f = new Filter() {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         return null;
       }
     };
diff --git a/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java b/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java
index dd1655ad2a0..bca34a1f594 100644
-- a/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java
@@ -20,7 +20,7 @@ package org.apache.lucene.search;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.BooleanClause.Occur;
@@ -88,7 +88,7 @@ public class TestFilteredQuery extends LuceneTestCase {
   private static Filter newStaticFilterB() {
     return new Filter() {
       @Override
      public DocIdSet getDocIdSet (ReaderContext context) {
      public DocIdSet getDocIdSet (AtomicReaderContext context) {
         BitSet bitset = new BitSet(5);
         bitset.set (1);
         bitset.set (3);
@@ -159,7 +159,7 @@ public class TestFilteredQuery extends LuceneTestCase {
   private static Filter newStaticFilterA() {
     return new Filter() {
       @Override
      public DocIdSet getDocIdSet (ReaderContext context) {
      public DocIdSet getDocIdSet (AtomicReaderContext context) {
         BitSet bitset = new BitSet(5);
         bitset.set(0, 5);
         return new DocIdBitSet(bitset);
diff --git a/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java b/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java
index a4b02fa9592..365f2317b36 100644
-- a/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java
++ b/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java
@@ -25,7 +25,6 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
@@ -97,7 +96,7 @@ public class TestFilteredSearch extends LuceneTestCase {
     }
 
     @Override
    public DocIdSet getDocIdSet(ReaderContext context) {
    public DocIdSet getDocIdSet(AtomicReaderContext context) {
       assert context.isAtomic;
       final OpenBitSet set = new OpenBitSet();
       int docBase = ((AtomicReaderContext)context).docBase;
diff --git a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java
index f4b330e8fe9..18b1ded0a24 100644
-- a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java
++ b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java
@@ -22,6 +22,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.NumericField;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.index.RandomIndexWriter;
@@ -176,14 +177,15 @@ public class TestNumericRangeQuery32 extends LuceneTestCase {
   
   @Test
   public void testInverseRange() throws Exception {
    AtomicReaderContext context = (AtomicReaderContext) new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext();
     NumericRangeFilter<Integer> f = NumericRangeFilter.newIntRange("field8", 8, 1000, -1000, true, true);
    assertSame("A inverse range should return the EMPTY_DOCIDSET instance", DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
    assertSame("A inverse range should return the EMPTY_DOCIDSET instance", DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(context));
     f = NumericRangeFilter.newIntRange("field8", 8, Integer.MAX_VALUE, null, false, false);
     assertSame("A exclusive range starting with Integer.MAX_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(context));
     f = NumericRangeFilter.newIntRange("field8", 8, null, Integer.MIN_VALUE, false, false);
     assertSame("A exclusive range ending with Integer.MIN_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(context));
   }
   
   @Test
diff --git a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java
index 2e5c3e8b256..27aebfce451 100644
-- a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java
++ b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java
@@ -25,6 +25,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.LuceneTestCase;
@@ -181,15 +182,16 @@ public class TestNumericRangeQuery64 extends LuceneTestCase {
   
   @Test
   public void testInverseRange() throws Exception {
    AtomicReaderContext context = (AtomicReaderContext) new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext();
     NumericRangeFilter<Long> f = NumericRangeFilter.newLongRange("field8", 8, 1000L, -1000L, true, true);
     assertSame("A inverse range should return the EMPTY_DOCIDSET instance", DocIdSet.EMPTY_DOCIDSET,
        f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
        f.getDocIdSet(context));
     f = NumericRangeFilter.newLongRange("field8", 8, Long.MAX_VALUE, null, false, false);
     assertSame("A exclusive range starting with Long.MAX_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(context));
     f = NumericRangeFilter.newLongRange("field8", 8, null, Long.MIN_VALUE, false, false);
     assertSame("A exclusive range ending with Long.MIN_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(context));
   }
   
   @Test
diff --git a/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java b/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
index 364b452ce23..55c4042eca0 100755
-- a/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
++ b/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
@@ -7,7 +7,7 @@ import java.util.BitSet;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.IndexWriterConfig.OpenMode;
@@ -142,7 +142,7 @@ public class TestScorerPerf extends LuceneTestCase {
     final BitSet rnd = sets[random.nextInt(sets.length)];
     Query q = new ConstantScoreQuery(new Filter() {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) {
      public DocIdSet getDocIdSet(AtomicReaderContext context) {
         return new DocIdBitSet(rnd);
       }
     });
diff --git a/lucene/src/test/org/apache/lucene/search/TestSort.java b/lucene/src/test/org/apache/lucene/search/TestSort.java
index b0889f21332..32b58632374 100644
-- a/lucene/src/test/org/apache/lucene/search/TestSort.java
++ b/lucene/src/test/org/apache/lucene/search/TestSort.java
@@ -34,7 +34,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.MultiReader;
@@ -688,7 +688,7 @@ public class TestSort extends LuceneTestCase implements Serializable {
     // a filter that only allows through the first hit
     Filter filt = new Filter() {
       @Override
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         BitSet bs = new BitSet(context.reader.maxDoc());
         bs.set(0, context.reader.maxDoc());
         bs.set(docs1.scoreDocs[0].doc);
diff --git a/lucene/src/test/org/apache/lucene/search/TestTermScorer.java b/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
index 7265ada5b1c..139df5077eb 100644
-- a/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
++ b/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
@@ -28,7 +28,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.store.Directory;
 
 public class TestTermScorer extends LuceneTestCase {
@@ -71,8 +71,8 @@ public class TestTermScorer extends LuceneTestCase {
     TermQuery termQuery = new TermQuery(allTerm);
     
     Weight weight = termQuery.weight(indexSearcher);
    
    Scorer ts = weight.scorer(indexSearcher.getTopReaderContext(), true, true);
    assertTrue(indexSearcher.getTopReaderContext().isAtomic);
    Scorer ts = weight.scorer((AtomicReaderContext)indexSearcher.getTopReaderContext(), true, true);
     // we have 2 documents with the term all in them, one document for all the
     // other values
     final List<TestHit> docs = new ArrayList<TestHit>();
@@ -132,8 +132,8 @@ public class TestTermScorer extends LuceneTestCase {
     TermQuery termQuery = new TermQuery(allTerm);
     
     Weight weight = termQuery.weight(indexSearcher);
    
    Scorer ts = weight.scorer(indexSearcher.getTopReaderContext(), true, true);
    assertTrue(indexSearcher.getTopReaderContext().isAtomic);
    Scorer ts = weight.scorer((AtomicReaderContext) indexSearcher.getTopReaderContext(), true, true);
     assertTrue("next did not return a doc",
         ts.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);
     assertTrue("score is not correct", ts.score() == 1.6931472f);
@@ -150,8 +150,9 @@ public class TestTermScorer extends LuceneTestCase {
     TermQuery termQuery = new TermQuery(allTerm);
     
     Weight weight = termQuery.weight(indexSearcher);
    
    Scorer ts = weight.scorer(indexSearcher.getTopReaderContext(), true, true);
    assertTrue(indexSearcher.getTopReaderContext().isAtomic);

    Scorer ts = weight.scorer((AtomicReaderContext) indexSearcher.getTopReaderContext(), true, true);
     assertTrue("Didn't skip", ts.advance(3) != DocIdSetIterator.NO_MORE_DOCS);
     // The next doc should be doc 5
     assertTrue("doc should be number 5", ts.docID() == 5);
diff --git a/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java b/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java
index fc3fb442bab..e5497e470f7 100644
-- a/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java
++ b/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java
@@ -21,6 +21,7 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
@@ -168,7 +169,8 @@ public class TestNearSpansOrdered extends LuceneTestCase {
   public void testSpanNearScorerSkipTo1() throws Exception {
     SpanNearQuery q = makeQuery();
     Weight w = q.weight(searcher);
    Scorer s = w.scorer(searcher.getTopReaderContext(), true, false);
    assertTrue(searcher.getTopReaderContext().isAtomic);
    Scorer s = w.scorer((AtomicReaderContext) searcher.getTopReaderContext(), true, false);
     assertEquals(1, s.advance(1));
   }
   /**
@@ -177,7 +179,8 @@ public class TestNearSpansOrdered extends LuceneTestCase {
    */
   public void testSpanNearScorerExplain() throws Exception {
     SpanNearQuery q = makeQuery();
    Explanation e = q.weight(searcher).explain(searcher.getTopReaderContext(), 1);
    assertTrue(searcher.getTopReaderContext().isAtomic);
    Explanation e = q.weight(searcher).explain((AtomicReaderContext) searcher.getTopReaderContext(), 1);
     assertTrue("Scorer explanation value for doc#1 isn't positive: "
                + e.toString(),
                0.0f < e.getValue());
diff --git a/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java b/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java
index 8743072d5da..30f99a6f0a6 100755
-- a/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java
++ b/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java
@@ -1,8 +1,6 @@
 package org.apache.solr.request;
 
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
@@ -10,6 +8,7 @@ import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.util.PriorityQueue;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.lucene.util.packed.Direct16;
 import org.apache.lucene.util.packed.Direct32;
 import org.apache.lucene.util.packed.Direct8;
@@ -19,7 +18,6 @@ import org.apache.solr.common.params.FacetParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexReader;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.util.BoundedTreeSet;
 import org.apache.solr.util.ByteUtils;
@@ -70,7 +68,7 @@ class PerSegmentSingleValuedFaceting {
     // reuse the translation logic to go from top level set to per-segment set
     baseSet = docs.getTopFilter();
 
    final AtomicReaderContext[] leaves = searcher.getTopReaderContext().leaves();
    final AtomicReaderContext[] leaves = ReaderUtil.leaves(searcher.getTopReaderContext());
     // The list of pending tasks that aren't immediately submitted
     // TODO: Is there a completion service, or a delegating executor that can
     // limit the number of concurrent tasks submitted to a bigger executor?
@@ -209,9 +207,9 @@ class PerSegmentSingleValuedFaceting {
   }
 
   class SegFacet {
    ReaderContext info;
    SegFacet(ReaderContext info) {
      this.info = info;
    AtomicReaderContext context;
    SegFacet(AtomicReaderContext context) {
      this.context = context;
     }
     
     FieldCache.DocTermsIndex si;
@@ -225,7 +223,7 @@ class PerSegmentSingleValuedFaceting {
     BytesRef tempBR = new BytesRef();
 
     void countTerms() throws IOException {
      si = FieldCache.DEFAULT.getTermsIndex(info.reader, fieldName);
      si = FieldCache.DEFAULT.getTermsIndex(context.reader, fieldName);
       // SolrCore.log.info("reader= " + reader + "  FC=" + System.identityHashCode(si));
 
       if (prefix!=null) {
@@ -247,7 +245,7 @@ class PerSegmentSingleValuedFaceting {
         // count collection array only needs to be as big as the number of terms we are
         // going to collect counts for.
         final int[] counts = this.counts = new int[nTerms];
        DocIdSet idSet = baseSet.getDocIdSet(info);
        DocIdSet idSet = baseSet.getDocIdSet(context);
         DocIdSetIterator iter = idSet.iterator();
 
 
diff --git a/solr/src/java/org/apache/solr/schema/LatLonType.java b/solr/src/java/org/apache/solr/schema/LatLonType.java
index 75694f7eaa9..2e35dcf72db 100644
-- a/solr/src/java/org/apache/solr/schema/LatLonType.java
++ b/solr/src/java/org/apache/solr/schema/LatLonType.java
@@ -19,7 +19,7 @@ package org.apache.solr.schema;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.search.*;
 import org.apache.lucene.spatial.DistanceUtils;
@@ -371,12 +371,12 @@ class SpatialDistanceQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       return new SpatialScorer(getSimilarity(searcher), context.reader, this);
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
       return ((SpatialScorer)scorer(context, true, true)).explain(doc);
     }
   }
diff --git a/solr/src/java/org/apache/solr/search/DocSet.java b/solr/src/java/org/apache/solr/search/DocSet.java
index 59b3286c98a..e4482ac0c73 100644
-- a/solr/src/java/org/apache/solr/search/DocSet.java
++ b/solr/src/java/org/apache/solr/search/DocSet.java
@@ -23,7 +23,7 @@ import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
 
@@ -247,7 +247,7 @@ abstract class DocSetBase implements DocSet {
 
     return new Filter() {
       @Override
      public DocIdSet getDocIdSet(ReaderContext ctx) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext ctx) throws IOException {
         IndexReader.AtomicReaderContext context = (IndexReader.AtomicReaderContext)ctx;  // TODO: remove after lucene migration
         IndexReader reader = ctx.reader;
 
diff --git a/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java b/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
index 0c5179e3d8e..fd63bb92fb9 100755
-- a/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
++ b/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
@@ -2,7 +2,7 @@ package org.apache.solr.search;
 
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.solr.search.function.ValueSource;
 import org.apache.solr.common.SolrException;
 
@@ -90,12 +90,12 @@ public class SolrConstantScoreQuery extends ConstantScoreQuery {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       return new ConstantScorer(similarity, context, this);
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
 
       ConstantScorer cs = new ConstantScorer(similarity, context, this);
       boolean exists = cs.docIdSetIterator.advance(doc) == doc;
@@ -124,10 +124,10 @@ public class SolrConstantScoreQuery extends ConstantScoreQuery {
     final float theScore;
     int doc = -1;
 
    public ConstantScorer(Similarity similarity, ReaderContext info, ConstantWeight w) throws IOException {
    public ConstantScorer(Similarity similarity, AtomicReaderContext context, ConstantWeight w) throws IOException {
       super(similarity);
       theScore = w.getValue();
      DocIdSet docIdSet = filter instanceof SolrFilter ? ((SolrFilter)filter).getDocIdSet(w.context, info) : filter.getDocIdSet(info);
      DocIdSet docIdSet = filter instanceof SolrFilter ? ((SolrFilter)filter).getDocIdSet(w.context, context) : filter.getDocIdSet(context);
       if (docIdSet == null) {
         docIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
       } else {
diff --git a/solr/src/java/org/apache/solr/search/SolrFilter.java b/solr/src/java/org/apache/solr/search/SolrFilter.java
index 91009320b3b..1eddff4ff43 100644
-- a/solr/src/java/org/apache/solr/search/SolrFilter.java
++ b/solr/src/java/org/apache/solr/search/SolrFilter.java
@@ -21,6 +21,7 @@ import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexReader.ReaderContext;
 
 import java.util.Map;
@@ -41,7 +42,7 @@ public abstract class SolrFilter extends Filter {
   public abstract DocIdSet getDocIdSet(Map context, ReaderContext readerContext) throws IOException;
 
   @Override
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
  public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
     return getDocIdSet(null, context);
   }
 }
diff --git a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
index 052bac50ef6..07a62d3e041 100755
-- a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
++ b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
@@ -22,7 +22,6 @@ import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 
 import java.io.IOException;
@@ -553,7 +552,7 @@ public class SortedIntDocSet extends DocSetBase {
       int lastEndIdx = 0;
 
       @Override
      public DocIdSet getDocIdSet(ReaderContext contextX) throws IOException {
      public DocIdSet getDocIdSet(AtomicReaderContext contextX) throws IOException {
         AtomicReaderContext context = (AtomicReaderContext)contextX;  // TODO: remove after lucene migration
         IndexReader reader = context.reader;
 
diff --git a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
index 963a776d3a0..645f3805cb8 100755
-- a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
++ b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
@@ -19,9 +19,8 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.util.ToStringUtils;
import org.apache.solr.search.SolrIndexReader;
 
 import java.io.IOException;
 import java.util.Set;
@@ -92,7 +91,7 @@ public class BoostedQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       Scorer subQueryScorer = qWeight.scorer(context, true, false);
       if(subQueryScorer == null) {
         return null;
@@ -101,7 +100,7 @@ public class BoostedQuery extends Query {
     }
 
     @Override
    public Explanation explain(ReaderContext readerContext, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext readerContext, int doc) throws IOException {
       Explanation subQueryExpl = qWeight.explain(readerContext,doc);
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
diff --git a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
index 1a6ad49bb6e..dc0f644995e 100644
-- a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
++ b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
@@ -18,6 +18,7 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.MultiFields;
@@ -94,12 +95,12 @@ public class FunctionQuery extends Query {
     }
 
     @Override
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       return new AllScorer(getSimilarity(searcher), context, this);
     }
 
     @Override
    public Explanation explain(ReaderContext context, int doc) throws IOException {
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
       return ((AllScorer)scorer(context, true, true)).explain(doc);
     }
   }
diff --git a/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java b/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
index 74875bedbfc..da746d6a2a0 100755
-- a/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
@@ -20,7 +20,7 @@ package org.apache.solr.search.function;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.search.SolrIndexReader;
import org.apache.lucene.util.ReaderUtil;
 
 import java.io.IOException;
 import java.util.Map;
@@ -59,8 +59,7 @@ public class ScaleFloatFunction extends ValueSource {
   private ScaleInfo createScaleInfo(Map context, IndexReader reader) throws IOException {
     IndexReader.ReaderContext ctx = ValueSource.readerToContext(context, reader);
     while (ctx.parent != null) ctx = ctx.parent;
    AtomicReaderContext[] leaves = ctx.leaves();
    if (ctx == null) leaves = new AtomicReaderContext[] {(AtomicReaderContext)ctx};
    final AtomicReaderContext[] leaves = ReaderUtil.leaves(ctx);
 
     float minVal = Float.POSITIVE_INFINITY;
     float maxVal = Float.NEGATIVE_INFINITY;
diff --git a/solr/src/java/org/apache/solr/search/function/ValueSource.java b/solr/src/java/org/apache/solr/search/function/ValueSource.java
index daaffd65504..80f61f6018d 100644
-- a/solr/src/java/org/apache/solr/search/function/ValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/ValueSource.java
@@ -27,7 +27,6 @@ import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.index.MultiFields;
import org.apache.solr.common.SolrException;
 
 import java.io.IOException;
 import java.io.Serializable;
diff --git a/solr/src/test/org/apache/solr/search/TestDocSet.java b/solr/src/test/org/apache/solr/search/TestDocSet.java
index 8a87ac5d72b..25eda5c9259 100644
-- a/solr/src/test/org/apache/solr/search/TestDocSet.java
++ b/solr/src/test/org/apache/solr/search/TestDocSet.java
@@ -24,8 +24,10 @@ import java.io.IOException;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.OpenBitSetIterator;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.MultiReader;
 import org.apache.lucene.search.Filter;
@@ -424,18 +426,19 @@ public class TestDocSet extends LuceneTestCase {
     DocIdSet db;
 
     // first test in-sequence sub readers
    for (ReaderContext readerInfo : topLevelContext.leaves()) {
      da = fa.getDocIdSet(readerInfo);
      db = fb.getDocIdSet(readerInfo);
    for (AtomicReaderContext readerContext : ReaderUtil.leaves(topLevelContext)) {
      da = fa.getDocIdSet(readerContext);
      db = fb.getDocIdSet(readerContext);
       doTestIteratorEqual(da, db);
     }  
 
    int nReaders = topLevelContext.leaves().length;
    AtomicReaderContext[] leaves = ReaderUtil.leaves(topLevelContext);
    int nReaders = leaves.length;
     // now test out-of-sequence sub readers
     for (int i=0; i<nReaders; i++) {
      ReaderContext readerInfo = topLevelContext.leaves()[rand.nextInt(nReaders)];
      da = fa.getDocIdSet(readerInfo);
      db = fb.getDocIdSet(readerInfo);
      AtomicReaderContext readerContext = leaves[rand.nextInt(nReaders)];
      da = fa.getDocIdSet(readerContext);
      db = fb.getDocIdSet(readerContext);
       doTestIteratorEqual(da, db);
     }
   }
diff --git a/solr/src/test/org/apache/solr/search/TestSort.java b/solr/src/test/org/apache/solr/search/TestSort.java
index 60b46237416..c4b0c1450c7 100755
-- a/solr/src/test/org/apache/solr/search/TestSort.java
++ b/solr/src/test/org/apache/solr/search/TestSort.java
@@ -21,7 +21,7 @@ import org.apache.lucene.analysis.core.SimpleAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.search.*;
@@ -107,7 +107,7 @@ public class TestSort extends AbstractSolrTestCase {
       for (int i=0; i<qiter; i++) {
         Filter filt = new Filter() {
           @Override
          public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
          public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
             return randSet(context.reader.maxDoc());
           }
         };
- 
2.19.1.windows.1

