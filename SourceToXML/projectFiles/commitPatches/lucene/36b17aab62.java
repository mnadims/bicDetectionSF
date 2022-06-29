From 36b17aab62c7a3d254fa976dfee3093af501f889 Mon Sep 17 00:00:00 2001
From: Simon Willnauer <simonw@apache.org>
Date: Wed, 5 Jan 2011 20:47:08 +0000
Subject: [PATCH] LUCENE-2831: Revise Weight#scorer & Filter#getDocIdSet API to
 pass Readers context

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1055636 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   4 +
 .../instantiated/InstantiatedIndexReader.java |   8 +
 .../lucene/index/memory/MemoryIndex.java      |   7 +
 .../apache/lucene/search/BooleanFilter.java   |  27 +--
 .../apache/lucene/search/ChainedFilter.java   |  32 ++--
 .../apache/lucene/search/DuplicateFilter.java |  18 +-
 .../search/FieldCacheRewriteMethod.java       |   7 +-
 .../org/apache/lucene/search/TermsFilter.java |   4 +-
 .../lucene/search/BooleanFilterTest.java      |   3 +-
 .../apache/lucene/search/TermsFilterTest.java |  11 +-
 .../geohash/GeoHashDistanceFilter.java        |  10 +-
 .../spatial/tier/CartesianShapeFilter.java    |  12 +-
 .../spatial/tier/LatLongDistanceFilter.java   |  13 +-
 .../lucene/spatial/tier/TestDistance.java     |   8 +-
 .../builders/NumericRangeFilterBuilder.java   |   4 +-
 .../TestNumericRangeFilterBuilder.java        |   2 +-
 .../apache/lucene/index/BufferedDeletes.java  |   7 +-
 .../apache/lucene/index/DirectoryReader.java  |  28 ++-
 .../lucene/index/FilterIndexReader.java       |   6 +
 .../org/apache/lucene/index/IndexReader.java  | 172 +++++++++++++++++-
 .../org/apache/lucene/index/MultiReader.java  |  25 +--
 .../apache/lucene/index/ParallelReader.java   |  11 +-
 .../apache/lucene/index/SegmentReader.java    |   7 +-
 .../lucene/index/SlowMultiReaderWrapper.java  |   8 +
 .../apache/lucene/search/BooleanQuery.java    |  11 +-
 .../lucene/search/CachingSpanFilter.java      |   5 +-
 .../lucene/search/CachingWrapperFilter.java   |  10 +-
 .../lucene/search/ConstantScoreQuery.java     |  11 +-
 .../lucene/search/DisjunctionMaxQuery.java    |  11 +-
 .../lucene/search/FieldCacheRangeFilter.java  |  45 ++---
 .../lucene/search/FieldCacheTermsFilter.java  |   5 +-
 .../java/org/apache/lucene/search/Filter.java |  16 +-
 .../apache/lucene/search/FilteredQuery.java   |   5 +-
 .../apache/lucene/search/IndexSearcher.java   | 161 ++++++++--------
 .../lucene/search/MatchAllDocsQuery.java      |   9 +-
 .../lucene/search/MultiPhraseQuery.java       |  13 +-
 .../search/MultiTermQueryWrapperFilter.java   |   8 +-
 .../org/apache/lucene/search/PhraseQuery.java |  15 +-
 .../lucene/search/QueryWrapperFilter.java     |  12 +-
 .../apache/lucene/search/SpanQueryFilter.java |   5 +-
 .../org/apache/lucene/search/TermQuery.java   |  10 +-
 .../java/org/apache/lucene/search/Weight.java |  38 ++--
 .../search/function/CustomScoreQuery.java     |  21 ++-
 .../search/function/ValueSourceQuery.java     |   9 +-
 .../search/payloads/PayloadNearQuery.java     |   8 +-
 .../search/payloads/PayloadTermQuery.java     |   8 +-
 .../lucene/search/spans/SpanWeight.java       |  12 +-
 .../org/apache/lucene/util/ReaderUtil.java    |  91 +++++++++
 .../search/CachingWrapperFilterHelper.java    |   7 +-
 .../lucene/search/JustCompileSearch.java      |   9 +-
 .../org/apache/lucene/search/MockFilter.java  |   4 +-
 .../org/apache/lucene/search/QueryUtils.java  |  51 ++++--
 .../lucene/search/SingleDocTestFilter.java    |   6 +-
 .../search/TestCachingWrapperFilter.java      |  27 +--
 .../search/TestDisjunctionMaxQuery.java       |  10 +-
 .../apache/lucene/search/TestDocIdSet.java    |   3 +-
 .../lucene/search/TestFilteredQuery.java      |   7 +-
 .../lucene/search/TestFilteredSearch.java     |  19 +-
 .../search/TestNumericRangeQuery32.java       |   6 +-
 .../search/TestNumericRangeQuery64.java       |   7 +-
 .../apache/lucene/search/TestScorerPerf.java  |   3 +-
 .../org/apache/lucene/search/TestSort.java    |   7 +-
 .../apache/lucene/search/TestTermScorer.java  |   7 +-
 .../search/spans/TestNearSpansOrdered.java    |   4 +-
 .../apache/lucene/search/spans/TestSpans.java |   3 +-
 .../PerSegmentSingleValuedFaceting.java       |  29 ++-
 .../org/apache/solr/schema/LatLonType.java    |  15 +-
 .../java/org/apache/solr/search/DocSet.java   |   4 +-
 .../solr/search/SolrConstantScoreQuery.java   |  13 +-
 .../org/apache/solr/search/SolrFilter.java    |   7 +-
 .../apache/solr/search/SolrIndexReader.java   |  15 +-
 .../apache/solr/search/SolrIndexSearcher.java |   2 +-
 .../apache/solr/search/SortedIntDocSet.java   |   4 +-
 .../solr/search/function/BoostedQuery.java    |  26 +--
 .../solr/search/function/FunctionQuery.java   |  23 +--
 .../search/function/QueryValueSource.java     |   2 +-
 .../function/ValueSourceRangeFilter.java      |   6 +-
 .../solr/common/util/ContentStreamTest.java   |  13 +-
 .../org/apache/solr/search/TestDocSet.java    |  20 +-
 .../test/org/apache/solr/search/TestSort.java |   5 +-
 .../solr/update/DirectUpdateHandlerTest.java  |   4 +-
 81 files changed, 855 insertions(+), 486 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 78a75fd5db7..fdd2e6c78d1 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -128,6 +128,10 @@ Changes in backwards compatibility policy
   ParallelMultiSearcher into IndexSearcher as an optional
   ExecutorServiced passed to its ctor.  (Mike McCandless)
 
* LUCENE-2837: Changed Weight#scorer, Weight#explain & Filter#getDocIdSet to
  operate on a ReaderContext instead of directly on IndexReader to enable
  searches to be aware of IndexSearcher's context. (Simon Willnauer)

 Changes in Runtime Behavior
 
 * LUCENE-2650, LUCENE-2825: The behavior of FSDirectory.open has changed. On 64-bit
diff --git a/lucene/contrib/instantiated/src/java/org/apache/lucene/store/instantiated/InstantiatedIndexReader.java b/lucene/contrib/instantiated/src/java/org/apache/lucene/store/instantiated/InstantiatedIndexReader.java
index 8fede649e66..58c5313bed4 100644
-- a/lucene/contrib/instantiated/src/java/org/apache/lucene/store/instantiated/InstantiatedIndexReader.java
++ b/lucene/contrib/instantiated/src/java/org/apache/lucene/store/instantiated/InstantiatedIndexReader.java
@@ -31,6 +31,7 @@ import java.util.Comparator;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.BitVector;
 import org.apache.lucene.util.BytesRef;
@@ -45,6 +46,8 @@ import org.apache.lucene.util.Bits;
 public class InstantiatedIndexReader extends IndexReader {
 
   private final InstantiatedIndex index;
  private ReaderContext context = new AtomicReaderContext(this);

 
   public InstantiatedIndexReader(InstantiatedIndex index) {
     super();
@@ -424,6 +427,11 @@ public class InstantiatedIndexReader extends IndexReader {
       }
     };
   }
  
  @Override
  public ReaderContext getTopReaderContext() {
    return context;
  }
 
   @Override
   public TermFreqVector[] getTermFreqVectors(int docNumber) throws IOException {
diff --git a/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java b/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
index b30adc7c7ad..f342c4f640f 100644
-- a/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
++ b/lucene/contrib/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
@@ -48,6 +48,7 @@ import org.apache.lucene.index.TermFreqVector;
 import org.apache.lucene.index.TermPositionVector;
 import org.apache.lucene.index.TermVectorMapper;
 import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
@@ -738,6 +739,7 @@ public class MemoryIndex implements Serializable {
   private final class MemoryIndexReader extends IndexReader {
     
     private IndexSearcher searcher; // needed to find searcher.getSimilarity() 
    private final ReaderContext readerInfos = new AtomicReaderContext(this);
     
     private MemoryIndexReader() {
       super(); // avoid as much superclass baggage as possible
@@ -764,6 +766,11 @@ public class MemoryIndex implements Serializable {
       if (DEBUG) System.err.println("MemoryIndexReader.docFreq: " + term + ", freq:" + freq);
       return freq;
     }
    
    @Override
    public ReaderContext getTopReaderContext() {
      return readerInfos;
    }
   
     @Override
     public Fields fields() {
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java
index 2e6868e5202..e3748774456 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/BooleanFilter.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 import java.util.ArrayList;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.OpenBitSetDISI;
@@ -41,10 +42,10 @@ public class BooleanFilter extends Filter
   ArrayList<Filter> notFilters = null;
   ArrayList<Filter> mustFilters = null;
   
  private DocIdSetIterator getDISI(ArrayList<Filter> filters, int index, IndexReader reader)
  private DocIdSetIterator getDISI(ArrayList<Filter> filters, int index, ReaderContext info)
   throws IOException
   {
    return filters.get(index).getDocIdSet(reader).iterator();
    return filters.get(index).getDocIdSet(info).iterator();
   }
 
   /**
@@ -52,21 +53,21 @@ public class BooleanFilter extends Filter
    * of the filters that have been added.
    */
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException
   {
     OpenBitSetDISI res = null;
  
    final IndexReader reader = context.reader;
     if (shouldFilters != null) {
       for (int i = 0; i < shouldFilters.size(); i++) {
         if (res == null) {
          res = new OpenBitSetDISI(getDISI(shouldFilters, i, reader), reader.maxDoc());
          res = new OpenBitSetDISI(getDISI(shouldFilters, i, context), reader.maxDoc());
         } else { 
          DocIdSet dis = shouldFilters.get(i).getDocIdSet(reader);
          DocIdSet dis = shouldFilters.get(i).getDocIdSet(context);
           if(dis instanceof OpenBitSet) {
             // optimized case for OpenBitSets
             res.or((OpenBitSet) dis);
           } else {
            res.inPlaceOr(getDISI(shouldFilters, i, reader));
            res.inPlaceOr(getDISI(shouldFilters, i, context));
           }
         }
       }
@@ -75,15 +76,15 @@ public class BooleanFilter extends Filter
     if (notFilters!=null) {
       for (int i = 0; i < notFilters.size(); i++) {
         if (res == null) {
          res = new OpenBitSetDISI(getDISI(notFilters, i, reader), reader.maxDoc());
          res = new OpenBitSetDISI(getDISI(notFilters, i, context), reader.maxDoc());
           res.flip(0, reader.maxDoc()); // NOTE: may set bits on deleted docs
         } else {
          DocIdSet dis = notFilters.get(i).getDocIdSet(reader);
          DocIdSet dis = notFilters.get(i).getDocIdSet(context);
           if(dis instanceof OpenBitSet) {
             // optimized case for OpenBitSets
             res.andNot((OpenBitSet) dis);
           } else {
            res.inPlaceNot(getDISI(notFilters, i, reader));
            res.inPlaceNot(getDISI(notFilters, i, context));
           }
         }
       }
@@ -92,14 +93,14 @@ public class BooleanFilter extends Filter
     if (mustFilters!=null) {
       for (int i = 0; i < mustFilters.size(); i++) {
         if (res == null) {
          res = new OpenBitSetDISI(getDISI(mustFilters, i, reader), reader.maxDoc());
          res = new OpenBitSetDISI(getDISI(mustFilters, i, context), reader.maxDoc());
         } else {
          DocIdSet dis = mustFilters.get(i).getDocIdSet(reader);
          DocIdSet dis = mustFilters.get(i).getDocIdSet(context);
           if(dis instanceof OpenBitSet) {
             // optimized case for OpenBitSets
             res.and((OpenBitSet) dis);
           } else {
            res.inPlaceAnd(getDISI(mustFilters, i, reader));
            res.inPlaceAnd(getDISI(mustFilters, i, context));
           }
         }
       }
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java
index e95b50660d8..a8cc00caf5f 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/ChainedFilter.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Filter;
@@ -96,21 +97,21 @@ public class ChainedFilter extends Filter
      * {@link Filter#getDocIdSet}.
      */
     @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException
     {
         int[] index = new int[1]; // use array as reference to modifiable int; 
         index[0] = 0;             // an object attribute would not be thread safe.
         if (logic != -1)
            return getDocIdSet(reader, logic, index);
            return getDocIdSet(context, logic, index);
         else if (logicArray != null)
            return getDocIdSet(reader, logicArray, index);
            return getDocIdSet(context, logicArray, index);
         else
            return getDocIdSet(reader, DEFAULT, index);
            return getDocIdSet(context, DEFAULT, index);
     }
 
    private DocIdSetIterator getDISI(Filter filter, IndexReader reader)
    private DocIdSetIterator getDISI(Filter filter, ReaderContext info)
     throws IOException {
        DocIdSet docIdSet = filter.getDocIdSet(reader);
        DocIdSet docIdSet = filter.getDocIdSet(info);
         if (docIdSet == null) {
           return DocIdSet.EMPTY_DOCIDSET.iterator();
         } else {
@@ -123,9 +124,10 @@ public class ChainedFilter extends Filter
         }
     }
 
    private OpenBitSetDISI initialResult(IndexReader reader, int logic, int[] index)
    private OpenBitSetDISI initialResult(ReaderContext info, int logic, int[] index)
     throws IOException
     {
        IndexReader reader = info.reader;
         OpenBitSetDISI result;
         /**
          * First AND operation takes place against a completely false
@@ -133,12 +135,12 @@ public class ChainedFilter extends Filter
          */
         if (logic == AND)
         {
            result = new OpenBitSetDISI(getDISI(chain[index[0]], reader), reader.maxDoc());
            result = new OpenBitSetDISI(getDISI(chain[index[0]], info), reader.maxDoc());
             ++index[0];
         }
         else if (logic == ANDNOT)
         {
            result = new OpenBitSetDISI(getDISI(chain[index[0]], reader), reader.maxDoc());
            result = new OpenBitSetDISI(getDISI(chain[index[0]], info), reader.maxDoc());
             result.flip(0,reader.maxDoc()); // NOTE: may set bits for deleted docs.
             ++index[0];
         }
@@ -155,13 +157,13 @@ public class ChainedFilter extends Filter
      * @param logic Logical operation
      * @return DocIdSet
      */
    private DocIdSet getDocIdSet(IndexReader reader, int logic, int[] index)
    private DocIdSet getDocIdSet(ReaderContext info, int logic, int[] index)
     throws IOException
     {
        OpenBitSetDISI result = initialResult(reader, logic, index);
        OpenBitSetDISI result = initialResult(info, logic, index);
         for (; index[0] < chain.length; index[0]++)
         {
            doChain(result, logic, chain[index[0]].getDocIdSet(reader));
            doChain(result, logic, chain[index[0]].getDocIdSet(info));
         }
         return result;
     }
@@ -172,16 +174,16 @@ public class ChainedFilter extends Filter
      * @param logic Logical operation
      * @return DocIdSet
      */
    private DocIdSet getDocIdSet(IndexReader reader, int[] logic, int[] index)
    private DocIdSet getDocIdSet(ReaderContext info, int[] logic, int[] index)
     throws IOException
     {
         if (logic.length != chain.length)
             throw new IllegalArgumentException("Invalid number of elements in logic array");
 
        OpenBitSetDISI result = initialResult(reader, logic[0], index);
        OpenBitSetDISI result = initialResult(info, logic[0], index);
         for (; index[0] < chain.length; index[0]++)
         {
            doChain(result, logic[index[0]], chain[index[0]].getDocIdSet(reader));
            doChain(result, logic[index[0]], chain[index[0]].getDocIdSet(info));
         }
         return result;
     }
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java
index 3a249344986..5f2e3b1528f 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/DuplicateFilter.java
@@ -19,6 +19,7 @@ import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.TermsEnum;
@@ -27,7 +28,8 @@ import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.Bits;
 
 public class DuplicateFilter extends Filter
{
{ // TODO: make duplicate filter aware of ReaderContext such that we can 
  // filter duplicates across segments
 	
 	String fieldName;
 	
@@ -70,15 +72,15 @@ public class DuplicateFilter extends Filter
 	}
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException
 	{
 		if(processingMode==PM_FAST_INVALIDATION)
 		{
			return fastBits(reader);
			return fastBits(context.reader);
 		}
 		else
 		{
			return correctBits(reader);
			return correctBits(context.reader);
 		}
 	}
 	
@@ -96,7 +98,7 @@ public class DuplicateFilter extends Filter
         } else {
           docs = termsEnum.docs(delDocs, docs);
           int doc = docs.nextDoc();
          if (doc != docs.NO_MORE_DOCS) {
          if (doc != DocsEnum.NO_MORE_DOCS) {
             if (keepMode == KM_USE_FIRST_OCCURRENCE) {
               bits.set(doc);
             } else {
@@ -104,7 +106,7 @@ public class DuplicateFilter extends Filter
               while (true) {
                 lastDoc = doc;
                 doc = docs.nextDoc();
                if (doc == docs.NO_MORE_DOCS) {
                if (doc == DocsEnum.NO_MORE_DOCS) {
                   break;
                 }
               }
@@ -136,7 +138,7 @@ public class DuplicateFilter extends Filter
             // unset potential duplicates
             docs = termsEnum.docs(delDocs, docs);
             int doc = docs.nextDoc();
            if (doc != docs.NO_MORE_DOCS) {
            if (doc != DocsEnum.NO_MORE_DOCS) {
               if (keepMode == KM_USE_FIRST_OCCURRENCE) {
                 doc = docs.nextDoc();
               }
@@ -147,7 +149,7 @@ public class DuplicateFilter extends Filter
               lastDoc = doc;
               bits.clear(lastDoc);
               doc = docs.nextDoc();
              if (doc == docs.NO_MORE_DOCS) {
              if (doc == DocsEnum.NO_MORE_DOCS) {
                 break;
               }
             }
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
index e296fcaa5ff..6dee395ab70 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/FieldCacheRewriteMethod.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 import java.util.Comparator;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.BytesRef;
@@ -108,8 +109,8 @@ public final class FieldCacheRewriteMethod extends MultiTermQuery.RewriteMethod
      * results.
      */
     @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      final FieldCache.DocTermsIndex fcsi = FieldCache.DEFAULT.getTermsIndex(reader, query.field);
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
      final FieldCache.DocTermsIndex fcsi = FieldCache.DEFAULT.getTermsIndex(context.reader, query.field);
       final OpenBitSet termSet = new OpenBitSet(fcsi.numOrd());
       TermsEnum termsEnum = query.getTermsEnum(new Terms() {
         
@@ -142,7 +143,7 @@ public final class FieldCacheRewriteMethod extends MultiTermQuery.RewriteMethod
         return DocIdSet.EMPTY_DOCIDSET;
       }
       
      return new FieldCacheRangeFilter.FieldCacheDocIdSet(reader, true) {
      return new FieldCacheRangeFilter.FieldCacheDocIdSet(context.reader, true) {
         @Override
         boolean matchDoc(int doc) throws ArrayIndexOutOfBoundsException {
           return termSet.fastGet(fcsi.getOrd(doc));
diff --git a/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java b/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java
index 5ab5834a5b4..e1ab950ab0c 100644
-- a/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java
++ b/lucene/contrib/queries/src/java/org/apache/lucene/search/TermsFilter.java
@@ -23,6 +23,7 @@ import java.util.Set;
 import java.util.TreeSet;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.Terms;
@@ -57,7 +58,8 @@ public class TermsFilter extends Filter
    * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader)
 	 */
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    IndexReader reader = context.reader;
     OpenBitSet result=new OpenBitSet(reader.maxDoc());
     Fields fields = reader.fields();
     BytesRef br = new BytesRef();
diff --git a/lucene/contrib/queries/src/test/org/apache/lucene/search/BooleanFilterTest.java b/lucene/contrib/queries/src/test/org/apache/lucene/search/BooleanFilterTest.java
index a9a6766f927..b9a8dfacd6c 100644
-- a/lucene/contrib/queries/src/test/org/apache/lucene/search/BooleanFilterTest.java
++ b/lucene/contrib/queries/src/test/org/apache/lucene/search/BooleanFilterTest.java
@@ -24,6 +24,7 @@ import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
 import org.apache.lucene.index.Term;
@@ -83,7 +84,7 @@ public class BooleanFilterTest extends LuceneTestCase {
         private void tstFilterCard(String mes, int expected, Filter filt)
         throws Throwable
         {
          DocIdSetIterator disi = filt.getDocIdSet(reader).iterator();
          DocIdSetIterator disi = filt.getDocIdSet(new AtomicReaderContext(reader)).iterator();
           int actual = 0;
           while (disi.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
             actual++;
diff --git a/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java b/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java
index 454b6de9165..27d76cdb8df 100644
-- a/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java
++ b/lucene/contrib/queries/src/test/org/apache/lucene/search/TermsFilterTest.java
@@ -21,6 +21,7 @@ import java.util.HashSet;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
@@ -59,23 +60,25 @@ public class TermsFilterTest extends LuceneTestCase {
 			w.addDocument(doc);			
 		}
 		IndexReader reader = new SlowMultiReaderWrapper(w.getReader());
		ReaderContext context = reader.getTopReaderContext();
		assertTrue(context.isAtomic);
 		w.close();
 		
 		TermsFilter tf=new TermsFilter();
 		tf.addTerm(new Term(fieldName,"19"));
		OpenBitSet bits = (OpenBitSet)tf.getDocIdSet(reader);
		OpenBitSet bits = (OpenBitSet)tf.getDocIdSet(context);
 		assertEquals("Must match nothing", 0, bits.cardinality());
 
 		tf.addTerm(new Term(fieldName,"20"));
		bits = (OpenBitSet)tf.getDocIdSet(reader);
		bits = (OpenBitSet)tf.getDocIdSet(context);
 		assertEquals("Must match 1", 1, bits.cardinality());
 		
 		tf.addTerm(new Term(fieldName,"10"));
		bits = (OpenBitSet)tf.getDocIdSet(reader);
		bits = (OpenBitSet)tf.getDocIdSet(context);
 		assertEquals("Must match 2", 2, bits.cardinality());
 		
 		tf.addTerm(new Term(fieldName,"00"));
		bits = (OpenBitSet)tf.getDocIdSet(reader);
		bits = (OpenBitSet)tf.getDocIdSet(context);
 		assertEquals("Must match 2", 2, bits.cardinality());
 		
 		reader.close();
diff --git a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java
index 69431f71f26..4f348b648d1 100644
-- a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java
++ b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashDistanceFilter.java
@@ -19,7 +19,7 @@ package org.apache.lucene.spatial.geohash;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.FieldCache.DocTerms;
 import org.apache.lucene.search.Filter;
@@ -62,15 +62,15 @@ public class GeoHashDistanceFilter extends DistanceFilter {
   }
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
 
    final DocTerms geoHashValues = FieldCache.DEFAULT.getTerms(reader, geoHashField);
    final DocTerms geoHashValues = FieldCache.DEFAULT.getTerms(context.reader, geoHashField);
     final BytesRef br = new BytesRef();
 
     final int docBase = nextDocBase;
    nextDocBase += reader.maxDoc();
    nextDocBase += context.reader.maxDoc();
 
    return new FilteredDocIdSet(startingFilter.getDocIdSet(reader)) {
    return new FilteredDocIdSet(startingFilter.getDocIdSet(context)) {
       @Override
       public boolean match(int doc) {
 
diff --git a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java
index 11527f396d4..07a833f3e2e 100644
-- a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java
++ b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilter.java
@@ -20,7 +20,7 @@ import java.io.IOException;
 import java.util.List;
 
 import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
@@ -45,8 +45,8 @@ public class CartesianShapeFilter extends Filter {
   }
   
   @Override
  public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
    final Bits delDocs = reader.getDeletedDocs();
  public DocIdSet getDocIdSet(final ReaderContext context) throws IOException {
    final Bits delDocs = context.reader.getDeletedDocs();
     final List<Double> area = shape.getArea();
     final int sz = area.size();
     
@@ -58,7 +58,7 @@ public class CartesianShapeFilter extends Filter {
       return new DocIdSet() {
         @Override
         public DocIdSetIterator iterator() throws IOException {
          return reader.termDocsEnum(delDocs, fieldName, bytesRef);
          return context.reader.termDocsEnum(delDocs, fieldName, bytesRef);
         }
         
         @Override
@@ -67,11 +67,11 @@ public class CartesianShapeFilter extends Filter {
         }
       };
     } else {
      final OpenBitSet bits = new OpenBitSet(reader.maxDoc());
      final OpenBitSet bits = new OpenBitSet(context.reader.maxDoc());
       for (int i =0; i< sz; i++) {
         double boxId = area.get(i).doubleValue();
         NumericUtils.longToPrefixCoded(NumericUtils.doubleToSortableLong(boxId), 0, bytesRef);
        final DocsEnum docsEnum = reader.termDocsEnum(delDocs, fieldName, bytesRef);
        final DocsEnum docsEnum = context.reader.termDocsEnum(delDocs, fieldName, bytesRef);
         if (docsEnum == null) continue;
         // iterate through all documents
         // which have this boxId
diff --git a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java
index 44fba384701..58b475bca60 100644
-- a/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java
++ b/lucene/contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java
@@ -18,7 +18,8 @@
 package org.apache.lucene.spatial.tier;
 
 import java.io.IOException;
import org.apache.lucene.index.IndexReader;

import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.FilteredDocIdSet;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.Filter;
@@ -64,15 +65,15 @@ public class LatLongDistanceFilter extends DistanceFilter {
   }
   
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
 
    final double[] latIndex = FieldCache.DEFAULT.getDoubles(reader, latField);
    final double[] lngIndex = FieldCache.DEFAULT.getDoubles(reader, lngField);
    final double[] latIndex = FieldCache.DEFAULT.getDoubles(context.reader, latField);
    final double[] lngIndex = FieldCache.DEFAULT.getDoubles(context.reader, lngField);
 
     final int docBase = nextDocBase;
    nextDocBase += reader.maxDoc();
    nextDocBase += context.reader.maxDoc();
 
    return new FilteredDocIdSet(startingFilter.getDocIdSet(reader)) {
    return new FilteredDocIdSet(startingFilter.getDocIdSet(context)) {
       @Override
       protected boolean match(int doc) {
         double x = latIndex[doc];
diff --git a/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java b/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java
index 0e0a787f8fe..f355fab632d 100644
-- a/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java
++ b/lucene/contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestDistance.java
@@ -22,6 +22,7 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.IndexReader;
@@ -30,6 +31,7 @@ import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.store.Directory;
 

 public class TestDistance extends LuceneTestCase {
   
   private Directory directory;
@@ -100,9 +102,9 @@ public class TestDistance extends LuceneTestCase {
     LatLongDistanceFilter f = new LatLongDistanceFilter(new QueryWrapperFilter(new MatchAllDocsQuery()),
                                                         lat, lng, 1.0, latField, lngField);
 
    IndexReader[] readers = r.getSequentialSubReaders();
    for(int i=0;i<readers.length;i++) {
      f.getDocIdSet(readers[i]);
    AtomicReaderContext[] leaves = r.getTopReaderContext().leaves();
    for (int i = 0; i < leaves.length; i++) {
      f.getDocIdSet(leaves[i]);
     }
     r.close();
   }
diff --git a/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java b/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java
index c834f8e4d98..b6bea806dee 100644
-- a/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java
++ b/lucene/contrib/xml-query-parser/src/java/org/apache/lucene/xmlparser/builders/NumericRangeFilterBuilder.java
@@ -19,7 +19,7 @@ package org.apache.lucene.xmlparser.builders;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.NumericRangeFilter;
@@ -157,7 +157,7 @@ public class NumericRangeFilterBuilder implements FilterBuilder {
 		private static final long serialVersionUID = 1L;
 
 		@Override
		public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
 			return null;
 		}
 
diff --git a/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java b/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
index dca574dd0bf..4105c014f27 100644
-- a/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
++ b/lucene/contrib/xml-query-parser/src/test/org/apache/lucene/xmlparser/builders/TestNumericRangeFilterBuilder.java
@@ -67,7 +67,7 @@ public class TestNumericRangeFilterBuilder extends LuceneTestCase {
 			IndexReader reader = IndexReader.open(ramDir, true);
 			try
 			{
				assertNull(filter.getDocIdSet(reader));
				assertNull(filter.getDocIdSet(reader.getTopReaderContext()));
 			}
 			finally
 			{
diff --git a/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java b/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java
index 3b144aada2c..8b4032a6602 100644
-- a/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java
++ b/lucene/src/java/org/apache/lucene/index/BufferedDeletes.java
@@ -26,6 +26,7 @@ import java.util.Map;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
@@ -364,12 +365,16 @@ class BufferedDeletes {
     // Delete by query
     if (deletes.queries.size() > 0) {
       IndexSearcher searcher = new IndexSearcher(reader);
      
      final ReaderContext readerContext = searcher.getTopReaderContext();
      assert readerContext.isAtomic;
       try {
         for (Entry<Query, Integer> entry : deletes.queries.entrySet()) {
           Query query = entry.getKey();
           int limit = entry.getValue().intValue();
           Weight weight = query.weight(searcher);
          Scorer scorer = weight.scorer(reader, true, false);
          
          Scorer scorer = weight.scorer(readerContext, true, false);
           if (scorer != null) {
             while(true)  {
               int doc = scorer.nextDoc();
diff --git a/lucene/src/java/org/apache/lucene/index/DirectoryReader.java b/lucene/src/java/org/apache/lucene/index/DirectoryReader.java
index ff014d99f6b..c4ed2633760 100644
-- a/lucene/src/java/org/apache/lucene/index/DirectoryReader.java
++ b/lucene/src/java/org/apache/lucene/index/DirectoryReader.java
@@ -35,7 +35,6 @@ import org.apache.lucene.store.Lock;
 import org.apache.lucene.store.LockObtainFailedException;
 import org.apache.lucene.index.codecs.CodecProvider;
 import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.lucene.util.BytesRef;
 
 import org.apache.lucene.search.FieldCache; // not great (circular); used only to purge FieldCache entry on close
@@ -60,8 +59,8 @@ class DirectoryReader extends IndexReader implements Cloneable {
   private boolean rollbackHasChanges;
 
   private SegmentReader[] subReaders;
  private ReaderContext topLevelReaderContext;
   private int[] starts;                           // 1st docno for each segment
  private final Map<SegmentReader,ReaderUtil.Slice> subReaderToSlice = new HashMap<SegmentReader,ReaderUtil.Slice>();
   private int maxDoc = 0;
   private int numDocs = -1;
   private boolean hasDeletions = false;
@@ -300,25 +299,22 @@ class DirectoryReader extends IndexReader implements Cloneable {
   private void initialize(SegmentReader[] subReaders) throws IOException {
     this.subReaders = subReaders;
     starts = new int[subReaders.length + 1];    // build starts array

    final AtomicReaderContext[] subReaderCtx = new AtomicReaderContext[subReaders.length];
    topLevelReaderContext = new CompositeReaderContext(this, subReaderCtx, subReaderCtx);
     final List<Fields> subFields = new ArrayList<Fields>();
    final List<ReaderUtil.Slice> fieldSlices = new ArrayList<ReaderUtil.Slice>();

    
     for (int i = 0; i < subReaders.length; i++) {
       starts[i] = maxDoc;
      subReaderCtx[i] = new AtomicReaderContext(topLevelReaderContext, subReaders[i], i, maxDoc, i, maxDoc);
       maxDoc += subReaders[i].maxDoc();      // compute maxDocs
 
       if (subReaders[i].hasDeletions()) {
         hasDeletions = true;
       }

      final ReaderUtil.Slice slice = new ReaderUtil.Slice(starts[i], subReaders[i].maxDoc(), i);
      subReaderToSlice.put(subReaders[i], slice);

      
       final Fields f = subReaders[i].fields();
       if (f != null) {
         subFields.add(f);
        fieldSlices.add(slice);
       }
     }
     starts[subReaders.length] = maxDoc;
@@ -844,16 +840,16 @@ class DirectoryReader extends IndexReader implements Cloneable {
       fieldSet.addAll(names);
     }
     return fieldSet;
  } 
  }
   
   @Override
  public IndexReader[] getSequentialSubReaders() {
    return subReaders;
  public ReaderContext getTopReaderContext() {
    return topLevelReaderContext;
   }

  
   @Override
  public int getSubReaderDocBase(IndexReader subReader) {
    return subReaderToSlice.get(subReader).start;
  public IndexReader[] getSequentialSubReaders() {
    return subReaders;
   }
 
   /** Returns the directory this index resides in. */
diff --git a/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java b/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
index ca211eaccea..aa18fcdb51f 100644
-- a/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
++ b/lucene/src/java/org/apache/lucene/index/FilterIndexReader.java
@@ -19,6 +19,7 @@ package org.apache.lucene.index;
 
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.search.FieldCache; // not great (circular); used only to purge FieldCache entry on close
@@ -417,6 +418,11 @@ public class FilterIndexReader extends IndexReader {
   public IndexReader[] getSequentialSubReaders() {
     return in.getSequentialSubReaders();
   }
  
  @Override
  public ReaderContext getTopReaderContext() {
    return in.getTopReaderContext();
  }
 
   @Override
   public Fields fields() throws IOException {
diff --git a/lucene/src/java/org/apache/lucene/index/IndexReader.java b/lucene/src/java/org/apache/lucene/index/IndexReader.java
index cc561d6e43b..ac0f203a8fb 100644
-- a/lucene/src/java/org/apache/lucene/index/IndexReader.java
++ b/lucene/src/java/org/apache/lucene/index/IndexReader.java
@@ -1126,7 +1126,7 @@ public abstract class IndexReader implements Cloneable,Closeable {
     if (docs == null) return 0;
     int n = 0;
     int doc;
    while ((doc = docs.nextDoc()) != docs.NO_MORE_DOCS) {
    while ((doc = docs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
       deleteDocument(doc);
       n++;
     }
@@ -1356,9 +1356,7 @@ public abstract class IndexReader implements Cloneable,Closeable {
   }
 
   /** Expert: returns the sequential sub readers that this
   *  reader is logically composed of.  For example,
   *  IndexSearcher uses this API to drive searching by one
   *  sub reader at a time.  If this reader is not composed
   *  reader is logically composed of. If this reader is not composed
    *  of sequential child readers, it should return null.
    *  If this method returns an empty array, that means this
    *  reader is a null reader (for example a MultiReader
@@ -1373,12 +1371,33 @@ public abstract class IndexReader implements Cloneable,Closeable {
   public IndexReader[] getSequentialSubReaders() {
     return null;
   }


  /** Expert: returns the docID base for this subReader. */
  public int getSubReaderDocBase(IndexReader subReader) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Expert: Returns a the root {@link ReaderContext} for this
   * {@link IndexReader}'s sub-reader tree. Iff this reader is composed of sub
   * readers ,ie. this reader being a composite reader, this method returns a
   * {@link CompositeReaderContext} holding the reader's direct children as well as a
   * view of the reader tree's atomic leaf contexts. All sub-
   * {@link ReaderContext} instances referenced from this readers top-level
   * context are private to this reader and are not shared with another context
   * tree. For example, IndexSearcher uses this API to drive searching by one
   * atomic leaf reader at a time. If this reader is not composed of child
   * readers, this method returns an {@link AtomicReaderContext}.
   * <p>
   * Note: Any of the sub-{@link CompositeReaderContext} instances reference from this
   * top-level context holds a <code>null</code> {@link CompositeReaderContext#leaves}
   * reference. Only the top-level context maintains the convenience leaf-view
   * for performance reasons.
   * <p>
   * NOTE: You should not try using sub-readers returned by this method to make
   * any changes (setNorm, deleteDocument, etc.). While this might succeed for
   * one composite reader (like MultiReader), it will most likely lead to index
   * corruption for other readers (like DirectoryReader obtained through
   * {@link #open}. Use the top-level context's reader directly.
   * 
   * @lucene.experimental
   */
  public abstract ReaderContext getTopReaderContext();
 
   /** Expert */
   public Object getCoreCacheKey() {
@@ -1431,4 +1450,137 @@ public abstract class IndexReader implements Cloneable,Closeable {
   Fields retrieveFields() {
     return fields;
   }

  /**
   * A struct like class that represents a hierarchical relationship between
   * {@link IndexReader} instances. 
   * @lucene.experimental
   */
  public static abstract class ReaderContext {
    /** The reader context for this reader's immediate parent, or null if none */
    public final ReaderContext parent;
    /** The actual reader */
    public final IndexReader reader;
    /** <code>true</code> iff the reader is an atomic reader */
    public final boolean isAtomic;
    /** <code>true</code> if this context struct represents the top level reader within the hierarchical context */
    public final boolean isTopLevel;
    /** the doc base for this reader in the parent, <tt>0</tt> if parent is null */
    public final int docBaseInParent;
    /** the ord for this reader in the parent, <tt>0</tt> if parent is null */
    public final int ordInParent;
    
    ReaderContext(ReaderContext parent, IndexReader reader,
        boolean isAtomic, boolean isTopLevel, int ordInParent, int docBaseInParent) {
      this.parent = parent;
      this.reader = reader;
      this.isAtomic = isAtomic;
      this.docBaseInParent = docBaseInParent;
      this.ordInParent = ordInParent;
      this.isTopLevel = isTopLevel;
    }
    
    /**
     * Returns the context's leaves if this context is a top-level context
     * otherwise <code>null</code>.
     * <p>
     * Note: this is convenience method since leaves can always be obtained by
     * walking the context tree.
     */
    public AtomicReaderContext[] leaves() {
      return null;
    }
    
    /**
     * Returns the context's children iff this context is a composite context
     * otherwise <code>null</code>.
     * <p>
     * Note: this method is a convenience method to prevent
     * <code>instanceof</code> checks and type-casts to
     * {@link CompositeReaderContext}.
     */
    public ReaderContext[] children() {
      return null;
    }
  }
  
  /**
   * {@link ReaderContext} for composite {@link IndexReader} instance.
   * @lucene.experimental
   */
  public static final class CompositeReaderContext extends ReaderContext {
    /** the composite readers immediate children */
    public final ReaderContext[] children;
    /** the composite readers leaf reader contexts if this is the top level reader in this context */
    public final AtomicReaderContext[] leaves;

    /**
     * Creates a {@link CompositeReaderContext} for intermediate readers that aren't
     * not top-level readers in the current context
     */
    public CompositeReaderContext(ReaderContext parent, IndexReader reader,
        int ordInParent, int docbaseInParent, ReaderContext[] children) {
      this(parent, reader, ordInParent, docbaseInParent, children, null);
    }
    
    /**
     * Creates a {@link CompositeReaderContext} for top-level readers with parent set to <code>null</code>
     */
    public CompositeReaderContext(IndexReader reader, ReaderContext[] children, AtomicReaderContext[] leaves) {
      this(null, reader, 0, 0, children, leaves);
    }
    
    private CompositeReaderContext(ReaderContext parent, IndexReader reader,
        int ordInParent, int docbaseInParent, ReaderContext[] children,
        AtomicReaderContext[] leaves) {
      super(parent, reader, false, leaves != null, ordInParent, docbaseInParent);
      this.children = children;
      this.leaves = leaves;
    }

    @Override
    public AtomicReaderContext[] leaves() {
      return leaves;
    }
    
    
    @Override
    public ReaderContext[] children() {
      return children;
    }
  }
  
  /**
   * {@link ReaderContext} for atomic {@link IndexReader} instances
   * @lucene.experimental
   */
  public static final class AtomicReaderContext extends ReaderContext {
    /** The readers ord in the top-level's leaves array */
    public final int ord;
    /** The readers absolute doc base */
    public final int docBase;
    /**
     * Creates a new {@link AtomicReaderContext} 
     */
    public AtomicReaderContext(ReaderContext parent, IndexReader reader,
        int ord, int docBase, int leafOrd, int leafDocBase) {
     this(parent, reader, ord, docBase, leafOrd, leafDocBase, false);
    }
    
    private AtomicReaderContext(ReaderContext parent, IndexReader reader,
        int ord, int docBase, int leafOrd, int leafDocBase, boolean topLevel) {
      super(parent, reader, true, topLevel,  ord, docBase);
      assert reader.getSequentialSubReaders() == null : "Atomic readers must not have subreaders";
      this.ord = leafOrd;
      this.docBase = leafDocBase;
    }
    
    /**
     * Creates a new {@link AtomicReaderContext} for a atomic reader without an immediate
     * parent.
     */
    public AtomicReaderContext(IndexReader atomicReader) {
      this(null, atomicReader, 0, 0, 0, 0, true); // toplevel!!
    }
  }
 }
diff --git a/lucene/src/java/org/apache/lucene/index/MultiReader.java b/lucene/src/java/org/apache/lucene/index/MultiReader.java
index a765f3ae10f..a519eeb6197 100644
-- a/lucene/src/java/org/apache/lucene/index/MultiReader.java
++ b/lucene/src/java/org/apache/lucene/index/MultiReader.java
@@ -33,8 +33,8 @@ import org.apache.lucene.util.ReaderUtil;
  *  their content. */
 public class MultiReader extends IndexReader implements Cloneable {
   protected IndexReader[] subReaders;
  private final ReaderContext topLevelContext;
   private int[] starts;                           // 1st docno for each segment
  private final Map<IndexReader,ReaderUtil.Slice> subReaderToSlice = new HashMap<IndexReader,ReaderUtil.Slice>();
   private boolean[] decrefOnClose;                // remember which subreaders to decRef on close
   private int maxDoc = 0;
   private int numDocs = -1;
@@ -48,7 +48,7 @@ public class MultiReader extends IndexReader implements Cloneable {
   * @param subReaders set of (sub)readers
   */
   public MultiReader(IndexReader... subReaders) throws IOException {
    initialize(subReaders, true);
    topLevelContext = initialize(subReaders, true);
   }
 
   /**
@@ -60,14 +60,13 @@ public class MultiReader extends IndexReader implements Cloneable {
    * @param subReaders set of (sub)readers
    */
   public MultiReader(IndexReader[] subReaders, boolean closeSubReaders) throws IOException {
    initialize(subReaders, closeSubReaders);
    topLevelContext = initialize(subReaders, closeSubReaders);
   }
   
  private void initialize(IndexReader[] subReaders, boolean closeSubReaders) throws IOException {
  private ReaderContext initialize(IndexReader[] subReaders, boolean closeSubReaders) throws IOException {
     this.subReaders =  subReaders.clone();
     starts = new int[subReaders.length + 1];    // build starts array
     decrefOnClose = new boolean[subReaders.length];

     for (int i = 0; i < subReaders.length; i++) {
       starts[i] = maxDoc;
       maxDoc += subReaders[i].maxDoc();      // compute maxDocs
@@ -82,14 +81,9 @@ public class MultiReader extends IndexReader implements Cloneable {
       if (subReaders[i].hasDeletions()) {
         hasDeletions = true;
       }

      final ReaderUtil.Slice slice = new ReaderUtil.Slice(starts[i],
                                                          subReaders[i].maxDoc(),
                                                          i);
      subReaderToSlice.put(subReaders[i], slice);
     }

     starts[subReaders.length] = maxDoc;
    return ReaderUtil.buildReaderContext(this);
   }
 
   @Override
@@ -97,11 +91,6 @@ public class MultiReader extends IndexReader implements Cloneable {
     throw new UnsupportedOperationException("");
   }
 
  @Override
  public int getSubReaderDocBase(IndexReader subReader) {
    return subReaderToSlice.get(subReader).start;
  }

   @Override
   public Fields fields() throws IOException {
     throw new UnsupportedOperationException("please use MultiFields.getFields, or wrap your IndexReader with SlowMultiReaderWrapper, if you really need a top level Fields");
@@ -403,4 +392,8 @@ public class MultiReader extends IndexReader implements Cloneable {
   public IndexReader[] getSequentialSubReaders() {
     return subReaders;
   }
  
  public ReaderContext getTopReaderContext() {
    return topLevelContext;
  }
 }
diff --git a/lucene/src/java/org/apache/lucene/index/ParallelReader.java b/lucene/src/java/org/apache/lucene/index/ParallelReader.java
index 77f98487d35..775c865b2b9 100644
-- a/lucene/src/java/org/apache/lucene/index/ParallelReader.java
++ b/lucene/src/java/org/apache/lucene/index/ParallelReader.java
@@ -21,7 +21,9 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.document.FieldSelectorResult;
 import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ReaderUtil;
 import org.apache.lucene.search.FieldCache; // not great (circular); used only to purge FieldCache entry on close
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.util.BytesRef;
@@ -55,7 +57,7 @@ public class ParallelReader extends IndexReader {
   private Map<IndexReader,Collection<String>> readerToFields = new HashMap<IndexReader,Collection<String>>();
   private List<IndexReader> storedFieldReaders = new ArrayList<IndexReader>();
   private Map<String,byte[]> normsCache = new HashMap<String,byte[]>();
  
  private final ReaderContext topLevelReaderContext = new AtomicReaderContext(this);
   private int maxDoc;
   private int numDocs;
   private boolean hasDeletions;
@@ -90,7 +92,7 @@ public class ParallelReader extends IndexReader {
     buffer.append(')');
     return buffer.toString();
   }

  
  /** Add an IndexReader.
   * @throws IOException if there is a low-level IO error
   */
@@ -559,6 +561,11 @@ public class ParallelReader extends IndexReader {
     }
     return fieldSet;
   }
  @Override
  public ReaderContext getTopReaderContext() {
    return topLevelReaderContext;
  }

 }
 
 
diff --git a/lucene/src/java/org/apache/lucene/index/SegmentReader.java b/lucene/src/java/org/apache/lucene/index/SegmentReader.java
index 1909f62523f..66ebced2126 100644
-- a/lucene/src/java/org/apache/lucene/index/SegmentReader.java
++ b/lucene/src/java/org/apache/lucene/index/SegmentReader.java
@@ -51,7 +51,7 @@ public class SegmentReader extends IndexReader implements Cloneable {
 
   private SegmentInfo si;
   private int readBufferSize;

  private final ReaderContext readerContext = new AtomicReaderContext(this);
   CloseableThreadLocal<FieldsReader> fieldsReaderLocal = new FieldsReaderLocal();
   CloseableThreadLocal<TermVectorsReader> termVectorsLocal = new CloseableThreadLocal<TermVectorsReader>();
 
@@ -1183,6 +1183,11 @@ public class SegmentReader extends IndexReader implements Cloneable {
     buffer.append(si.toString(core.dir, pendingDeleteCount));
     return buffer.toString();
   }
  
  @Override
  public ReaderContext getTopReaderContext() {
    return readerContext;
  }
 
   /**
    * Return the name of the segment this reader is reading.
diff --git a/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java b/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java
index 7a29870586f..2deb8b2c741 100644
-- a/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java
++ b/lucene/src/java/org/apache/lucene/index/SlowMultiReaderWrapper.java
@@ -30,6 +30,7 @@ import org.apache.lucene.util.ReaderUtil; // javadoc
 
 import org.apache.lucene.index.DirectoryReader; // javadoc
 import org.apache.lucene.index.MultiReader; // javadoc
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 /**
  * This class forces a composite reader (eg a {@link
@@ -55,10 +56,12 @@ import org.apache.lucene.index.MultiReader; // javadoc
 
 public final class SlowMultiReaderWrapper extends FilterIndexReader {
 
  private final ReaderContext readerContext;
   private final Map<String,byte[]> normsCache = new HashMap<String,byte[]>();
   
   public SlowMultiReaderWrapper(IndexReader other) {
     super(other);
    readerContext = new AtomicReaderContext(this); // emulate atomic reader!
   }
 
   @Override
@@ -103,6 +106,11 @@ public final class SlowMultiReaderWrapper extends FilterIndexReader {
     }
   }
   
  @Override
  public ReaderContext getTopReaderContext() {
    return readerContext;
  }
  
   @Override
   protected void doSetNorm(int n, String field, byte value)
       throws CorruptIndexException, IOException {
diff --git a/lucene/src/java/org/apache/lucene/search/BooleanQuery.java b/lucene/src/java/org/apache/lucene/search/BooleanQuery.java
index 56f7d098114..d756eff3bad 100644
-- a/lucene/src/java/org/apache/lucene/search/BooleanQuery.java
++ b/lucene/src/java/org/apache/lucene/search/BooleanQuery.java
@@ -18,6 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.lucene.search.BooleanClause.Occur;
@@ -223,7 +224,7 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc)
    public Explanation explain(ReaderContext context, int doc)
       throws IOException {
       final int minShouldMatch =
         BooleanQuery.this.getMinimumNumberShouldMatch();
@@ -237,7 +238,7 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
       for (Iterator<Weight> wIter = weights.iterator(); wIter.hasNext();) {
         Weight w = wIter.next();
         BooleanClause c = cIter.next();
        if (w.scorer(reader, true, true) == null) {
        if (w.scorer(context, true, true) == null) {
           if (c.isRequired()) {
             fail = true;
             Explanation r = new Explanation(0.0f, "no match on required clause (" + c.getQuery().toString() + ")");
@@ -245,7 +246,7 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
           }
           continue;
         }
        Explanation e = w.explain(reader, doc);
        Explanation e = w.explain(context, doc);
         if (e.isMatch()) {
           if (!c.isProhibited()) {
             sumExpl.addDetail(e);
@@ -299,7 +300,7 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer)
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer)
         throws IOException {
       List<Scorer> required = new ArrayList<Scorer>();
       List<Scorer> prohibited = new ArrayList<Scorer>();
@@ -307,7 +308,7 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
       Iterator<BooleanClause> cIter = clauses.iterator();
       for (Weight w  : weights) {
         BooleanClause c =  cIter.next();
        Scorer subScorer = w.scorer(reader, true, false);
        Scorer subScorer = w.scorer(context, true, false);
         if (subScorer == null) {
           if (c.isRequired()) {
             return null;
diff --git a/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java b/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java
index d19c872ee58..1939406749e 100644
-- a/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java
++ b/lucene/src/java/org/apache/lucene/search/CachingSpanFilter.java
@@ -17,6 +17,7 @@ package org.apache.lucene.search;
 
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.Bits;
 
 import java.io.IOException;
@@ -60,8 +61,8 @@ public class CachingSpanFilter extends SpanFilter {
   }
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    SpanFilterResult result = getCachedResult(reader);
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    SpanFilterResult result = getCachedResult(context.reader);
     return result != null ? result.getDocIdSet() : null;
   }
   
diff --git a/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java b/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java
index d51eed25172..1fc5c9f8b80 100644
-- a/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java
++ b/lucene/src/java/org/apache/lucene/search/CachingWrapperFilter.java
@@ -23,6 +23,7 @@ import java.util.Map;
 import java.util.WeakHashMap;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.OpenBitSetDISI;
 import org.apache.lucene.util.Bits;
 
@@ -37,6 +38,9 @@ import org.apache.lucene.util.Bits;
  * {@link DeletesMode#DYNAMIC}).
  */
 public class CachingWrapperFilter extends Filter {
  // TODO: make this filter aware of ReaderContext. a cached filter could 
  // specify the actual readers key or something similar to indicate on which
  // level of the readers hierarchy it should be cached.
   Filter filter;
 
   /**
@@ -191,8 +195,8 @@ public class CachingWrapperFilter extends Filter {
   int hitCount, missCount;
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {

  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    final IndexReader reader = context.reader;
     final Object coreKey = reader.getCoreCacheKey();
     final Object delCoreKey = reader.hasDeletions() ? reader.getDeletedDocs() : coreKey;
 
@@ -205,7 +209,7 @@ public class CachingWrapperFilter extends Filter {
     missCount++;
 
     // cache miss
    docIdSet = docIdSetToCache(filter.getDocIdSet(reader), reader);
    docIdSet = docIdSetToCache(filter.getDocIdSet(context), reader);
 
     if (docIdSet != null) {
       cache.put(coreKey, delCoreKey, docIdSet);
diff --git a/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java b/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
index fe76121d3c2..6af8ed5305c 100644
-- a/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
++ b/lucene/src/java/org/apache/lucene/search/ConstantScoreQuery.java
@@ -18,6 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 
@@ -132,18 +133,18 @@ public class ConstantScoreQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(ReaderContext context,  boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       final DocIdSetIterator disi;
       if (filter != null) {
         assert query == null;
        final DocIdSet dis = filter.getDocIdSet(reader);
        final DocIdSet dis = filter.getDocIdSet(context);
         if (dis == null)
           return null;
         disi = dis.iterator();
       } else {
         assert query != null && innerWeight != null;
         disi =
          innerWeight.scorer(reader, scoreDocsInOrder, topScorer);
          innerWeight.scorer(context, scoreDocsInOrder, topScorer);
       }
       if (disi == null)
         return null;
@@ -156,8 +157,8 @@ public class ConstantScoreQuery extends Query {
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      final Scorer cs = scorer(reader, true, false);
    public Explanation explain(ReaderContext context, int doc) throws IOException {
      final Scorer cs = scorer(context, true, false);
       final boolean exists = (cs != null && cs.advance(doc) == doc);
 
       final ComplexExplanation result = new ComplexExplanation();
diff --git a/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java b/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
index b6cd0295247..83f7764776f 100644
-- a/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
++ b/lucene/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
@@ -23,6 +23,7 @@ import java.util.Iterator;
 import java.util.Set;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 
 /**
@@ -141,12 +142,12 @@ public class DisjunctionMaxQuery extends Query implements Iterable<Query> {
 
     /* Create the scorer used to score our associated DisjunctionMaxQuery */
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
         boolean topScorer) throws IOException {
       Scorer[] scorers = new Scorer[weights.size()];
       int idx = 0;
       for (Weight w : weights) {
        Scorer subScorer = w.scorer(reader, true, false);
        Scorer subScorer = w.scorer(context, true, false);
         if (subScorer != null && subScorer.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
           scorers[idx++] = subScorer;
         }
@@ -158,13 +159,13 @@ public class DisjunctionMaxQuery extends Query implements Iterable<Query> {
 
     /* Explain the score we computed for doc */
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      if (disjuncts.size() == 1) return weights.get(0).explain(reader,doc);
    public Explanation explain(ReaderContext context, int doc) throws IOException {
      if (disjuncts.size() == 1) return weights.get(0).explain(context,doc);
       ComplexExplanation result = new ComplexExplanation();
       float max = 0.0f, sum = 0.0f;
       result.setDescription(tieBreakerMultiplier == 0.0f ? "max of:" : "max plus " + tieBreakerMultiplier + " times others of:");
       for (Weight wt : weights) {
        Explanation e = wt.explain(reader, doc);
        Explanation e = wt.explain(context, doc);
         if (e.isMatch()) {
           result.setMatch(Boolean.TRUE);
           result.addDetail(e);
diff --git a/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java b/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java
index 6c4245a5d70..e0a03169285 100644
-- a/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java
++ b/lucene/src/java/org/apache/lucene/search/FieldCacheRangeFilter.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.util.NumericUtils;
 import org.apache.lucene.util.Bits;
@@ -73,7 +74,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   
   /** This method is implemented for each data type */
   @Override
  public abstract DocIdSet getDocIdSet(IndexReader reader) throws IOException;
  public abstract DocIdSet getDocIdSet(ReaderContext context) throws IOException;
 
   /**
    * Creates a string range filter using {@link FieldCache#getTermsIndex}. This works with all
@@ -83,8 +84,8 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<String> newStringRange(String field, String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<String>(field, null, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
        final FieldCache.DocTermsIndex fcsi = FieldCache.DEFAULT.getTermsIndex(reader, field);
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
        final FieldCache.DocTermsIndex fcsi = FieldCache.DEFAULT.getTermsIndex(context.reader, field);
         final BytesRef spare = new BytesRef();
         final int lowerPoint = fcsi.binarySearchLookup(lowerVal == null ? null : new BytesRef(lowerVal), spare);
         final int upperPoint = fcsi.binarySearchLookup(upperVal == null ? null : new BytesRef(upperVal), spare);
@@ -124,7 +125,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
         
         // for this DocIdSet, we can ignore deleted docs
         // because deleted docs have an order of 0 (null entry in StringIndex)
        return new FieldCacheDocIdSet(reader, true) {
        return new FieldCacheDocIdSet(context.reader, true) {
           @Override
           final boolean matchDoc(int doc) {
             final int docOrd = fcsi.getOrd(doc);
@@ -152,7 +153,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Byte> newByteRange(String field, FieldCache.ByteParser parser, Byte lowerVal, Byte upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Byte>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         final byte inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           final byte i = lowerVal.byteValue();
@@ -174,9 +175,9 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
         if (inclusiveLowerPoint > inclusiveUpperPoint)
           return DocIdSet.EMPTY_DOCIDSET;
         
        final byte[] values = FieldCache.DEFAULT.getBytes(reader, field, (FieldCache.ByteParser) parser);
        final byte[] values = FieldCache.DEFAULT.getBytes(context.reader, field, (FieldCache.ByteParser) parser);
         // we only respect deleted docs if the range contains 0
        return new FieldCacheDocIdSet(reader, !(inclusiveLowerPoint <= 0 && inclusiveUpperPoint >= 0)) {
        return new FieldCacheDocIdSet(context.reader, !(inclusiveLowerPoint <= 0 && inclusiveUpperPoint >= 0)) {
           @Override
           boolean matchDoc(int doc) {
             return values[doc] >= inclusiveLowerPoint && values[doc] <= inclusiveUpperPoint;
@@ -203,7 +204,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Short> newShortRange(String field, FieldCache.ShortParser parser, Short lowerVal, Short upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Short>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         final short inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           short i = lowerVal.shortValue();
@@ -225,9 +226,9 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
         if (inclusiveLowerPoint > inclusiveUpperPoint)
           return DocIdSet.EMPTY_DOCIDSET;
         
        final short[] values = FieldCache.DEFAULT.getShorts(reader, field, (FieldCache.ShortParser) parser);
        final short[] values = FieldCache.DEFAULT.getShorts(context.reader, field, (FieldCache.ShortParser) parser);
         // ignore deleted docs if range doesn't contain 0
        return new FieldCacheDocIdSet(reader, !(inclusiveLowerPoint <= 0 && inclusiveUpperPoint >= 0)) {
        return new FieldCacheDocIdSet(context.reader, !(inclusiveLowerPoint <= 0 && inclusiveUpperPoint >= 0)) {
           @Override
           boolean matchDoc(int doc) {
             return values[doc] >= inclusiveLowerPoint && values[doc] <= inclusiveUpperPoint;
@@ -254,7 +255,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Integer> newIntRange(String field, FieldCache.IntParser parser, Integer lowerVal, Integer upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Integer>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         final int inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           int i = lowerVal.intValue();
@@ -276,9 +277,9 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
         if (inclusiveLowerPoint > inclusiveUpperPoint)
           return DocIdSet.EMPTY_DOCIDSET;
         
        final int[] values = FieldCache.DEFAULT.getInts(reader, field, (FieldCache.IntParser) parser);
        final int[] values = FieldCache.DEFAULT.getInts(context.reader, field, (FieldCache.IntParser) parser);
         // ignore deleted docs if range doesn't contain 0
        return new FieldCacheDocIdSet(reader, !(inclusiveLowerPoint <= 0 && inclusiveUpperPoint >= 0)) {
        return new FieldCacheDocIdSet(context.reader, !(inclusiveLowerPoint <= 0 && inclusiveUpperPoint >= 0)) {
           @Override
           boolean matchDoc(int doc) {
             return values[doc] >= inclusiveLowerPoint && values[doc] <= inclusiveUpperPoint;
@@ -305,7 +306,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Long> newLongRange(String field, FieldCache.LongParser parser, Long lowerVal, Long upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Long>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         final long inclusiveLowerPoint, inclusiveUpperPoint;
         if (lowerVal != null) {
           long i = lowerVal.longValue();
@@ -327,9 +328,9 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
         if (inclusiveLowerPoint > inclusiveUpperPoint)
           return DocIdSet.EMPTY_DOCIDSET;
         
        final long[] values = FieldCache.DEFAULT.getLongs(reader, field, (FieldCache.LongParser) parser);
        final long[] values = FieldCache.DEFAULT.getLongs(context.reader, field, (FieldCache.LongParser) parser);
         // ignore deleted docs if range doesn't contain 0
        return new FieldCacheDocIdSet(reader, !(inclusiveLowerPoint <= 0L && inclusiveUpperPoint >= 0L)) {
        return new FieldCacheDocIdSet(context.reader, !(inclusiveLowerPoint <= 0L && inclusiveUpperPoint >= 0L)) {
           @Override
           boolean matchDoc(int doc) {
             return values[doc] >= inclusiveLowerPoint && values[doc] <= inclusiveUpperPoint;
@@ -356,7 +357,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Float> newFloatRange(String field, FieldCache.FloatParser parser, Float lowerVal, Float upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Float>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         // we transform the floating point numbers to sortable integers
         // using NumericUtils to easier find the next bigger/lower value
         final float inclusiveLowerPoint, inclusiveUpperPoint;
@@ -382,9 +383,9 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
         if (inclusiveLowerPoint > inclusiveUpperPoint)
           return DocIdSet.EMPTY_DOCIDSET;
         
        final float[] values = FieldCache.DEFAULT.getFloats(reader, field, (FieldCache.FloatParser) parser);
        final float[] values = FieldCache.DEFAULT.getFloats(context.reader, field, (FieldCache.FloatParser) parser);
         // ignore deleted docs if range doesn't contain 0
        return new FieldCacheDocIdSet(reader, !(inclusiveLowerPoint <= 0.0f && inclusiveUpperPoint >= 0.0f)) {
        return new FieldCacheDocIdSet(context.reader, !(inclusiveLowerPoint <= 0.0f && inclusiveUpperPoint >= 0.0f)) {
           @Override
           boolean matchDoc(int doc) {
             return values[doc] >= inclusiveLowerPoint && values[doc] <= inclusiveUpperPoint;
@@ -411,7 +412,7 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
   public static FieldCacheRangeFilter<Double> newDoubleRange(String field, FieldCache.DoubleParser parser, Double lowerVal, Double upperVal, boolean includeLower, boolean includeUpper) {
     return new FieldCacheRangeFilter<Double>(field, parser, lowerVal, upperVal, includeLower, includeUpper) {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         // we transform the floating point numbers to sortable integers
         // using NumericUtils to easier find the next bigger/lower value
         final double inclusiveLowerPoint, inclusiveUpperPoint;
@@ -437,9 +438,9 @@ public abstract class FieldCacheRangeFilter<T> extends Filter {
         if (inclusiveLowerPoint > inclusiveUpperPoint)
           return DocIdSet.EMPTY_DOCIDSET;
         
        final double[] values = FieldCache.DEFAULT.getDoubles(reader, field, (FieldCache.DoubleParser) parser);
        final double[] values = FieldCache.DEFAULT.getDoubles(context.reader, field, (FieldCache.DoubleParser) parser);
         // ignore deleted docs if range doesn't contain 0
        return new FieldCacheDocIdSet(reader, !(inclusiveLowerPoint <= 0.0 && inclusiveUpperPoint >= 0.0)) {
        return new FieldCacheDocIdSet(context.reader, !(inclusiveLowerPoint <= 0.0 && inclusiveUpperPoint >= 0.0)) {
           @Override
           boolean matchDoc(int doc) {
             return values[doc] >= inclusiveLowerPoint && values[doc] <= inclusiveUpperPoint;
diff --git a/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java b/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java
index 57f8be754a4..684b139e4cf 100644
-- a/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java
++ b/lucene/src/java/org/apache/lucene/search/FieldCacheTermsFilter.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 
 import org.apache.lucene.index.DocsEnum; // javadoc @link
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.BytesRef;
 
@@ -115,8 +116,8 @@ public class FieldCacheTermsFilter extends Filter {
   }
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    return new FieldCacheTermsFilterDocIdSet(getFieldCache().getTermsIndex(reader, field));
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    return new FieldCacheTermsFilterDocIdSet(getFieldCache().getTermsIndex(context.reader, field));
   }
 
   protected class FieldCacheTermsFilterDocIdSet extends DocIdSet {
diff --git a/lucene/src/java/org/apache/lucene/search/Filter.java b/lucene/src/java/org/apache/lucene/search/Filter.java
index f8061ebf52f..2dea148e94e 100644
-- a/lucene/src/java/org/apache/lucene/search/Filter.java
++ b/lucene/src/java/org/apache/lucene/search/Filter.java
@@ -19,7 +19,7 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.DocIdBitSet;
 
 /** 
@@ -38,10 +38,13 @@ public abstract class Filter implements java.io.Serializable {
    * must refer to document IDs for that segment, not for
    * the top-level reader.
    * 
   * @param reader a {@link IndexReader} instance opened on the index currently
   *         searched on. The provided reader is always an
   *         atomic reader, so you can call reader.fields()
   *         or reader.getDeletedDocs(), for example.
   * @param context a {@link ReaderContext} instance opened on the index currently
   *         searched on. Note, it is likely that the provided reader info does not
   *         represent the whole underlying index i.e. if the index has more than
   *         one segment the given reader only represents a single segment.
   *         The provided context is always an atomic context, so you can call 
   *         {@link IndexReader#fields()} or  {@link IndexReader#getDeletedDocs()}
   *         on the context's reader, for example.
    *          
    * @return a DocIdSet that provides the documents which should be permitted or
    *         prohibited in search results. <b>NOTE:</b> null can be returned if
@@ -49,5 +52,6 @@ public abstract class Filter implements java.io.Serializable {
    * 
    * @see DocIdBitSet
    */
  public abstract DocIdSet getDocIdSet(IndexReader reader) throws IOException;
  // TODO make this context an AtomicContext
  public abstract DocIdSet getDocIdSet(ReaderContext context) throws IOException;
 }
diff --git a/lucene/src/java/org/apache/lucene/search/FilteredQuery.java b/lucene/src/java/org/apache/lucene/search/FilteredQuery.java
index 6f27cfc6773..f0b6001665f 100644
-- a/lucene/src/java/org/apache/lucene/search/FilteredQuery.java
++ b/lucene/src/java/org/apache/lucene/search/FilteredQuery.java
@@ -18,6 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 
@@ -81,7 +82,7 @@ extends Query {
       }
 
       @Override
      public Explanation explain (IndexReader ir, int i) throws IOException {
      public Explanation explain (ReaderContext ir, int i) throws IOException {
         Explanation inner = weight.explain (ir, i);
         if (getBoost()!=1) {
           Explanation preBoost = inner;
@@ -111,7 +112,7 @@ extends Query {
 
       // return a filtering scorer
       @Override
      public Scorer scorer(IndexReader indexReader, boolean scoreDocsInOrder, boolean topScorer)
      public Scorer scorer(ReaderContext indexReader, boolean scoreDocsInOrder, boolean topScorer)
           throws IOException {
         final Scorer scorer = weight.scorer(indexReader, true, false);
         if (scorer == null) {
diff --git a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
index 25f552c5fdf..eb2a3809e89 100644
-- a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
++ b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
@@ -18,9 +18,7 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.ArrayList;
 import java.util.Iterator;
import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.concurrent.Callable;
 import java.util.concurrent.CompletionService;
@@ -35,6 +33,8 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.NIOFSDirectory;    // javadoc
@@ -57,14 +57,15 @@ import org.apache.lucene.util.ThreadInterruptedException;
  * use your own (non-Lucene) objects instead.</p>
  */
 public class IndexSearcher {
  IndexReader reader;
  final IndexReader reader; // package private for testing!
   private boolean closeReader;
   
   // NOTE: these members might change in incompatible ways
   // in the next release
  protected final IndexReader[] subReaders;
  protected final ReaderContext readerContext;
  protected final AtomicReaderContext[] leafContexts;
   protected final IndexSearcher[] subSearchers;
  protected final int[] docStarts;
//  protected final int[] docStarts;
   private final ExecutorService executor;
 
   /** The Similarity implementation used by this searcher. */
@@ -115,83 +116,73 @@ public class IndexSearcher {
     this(r, false, executor);
   }
 
  /** Expert: directly specify the reader, subReaders and
   *  their docID starts.
  /**
   * Creates a searcher searching the provided top-level {@link ReaderContext}.
   * <p>
   * Given a non-<code>null</code> {@link ExecutorService} this method runs
   * searches for each segment separately, using the provided ExecutorService.
   * IndexSearcher will not shutdown/awaitTermination this ExecutorService on
   * close; you must do so, eventually, on your own. NOTE: if you are using
   * {@link NIOFSDirectory}, do not use the shutdownNow method of
   * ExecutorService as this uses Thread.interrupt under-the-hood which can
   * silently close file descriptors (see <a
   * href="https://issues.apache.org/jira/browse/LUCENE-2239">LUCENE-2239</a>).
    * 
   * @lucene.experimental */
  public IndexSearcher(IndexReader reader, IndexReader[] subReaders, int[] docStarts) {
    this.reader = reader;
    this.subReaders = subReaders;
    this.docStarts = docStarts;
    subSearchers = new IndexSearcher[subReaders.length];
    for(int i=0;i<subReaders.length;i++) {
      subSearchers[i] = new IndexSearcher(subReaders[i]);
    }
    closeReader = false;
    executor = null;
   * @see ReaderContext
   * @see IndexReader#getTopReaderContext()
   * @lucene.experimental
   */
  public IndexSearcher(ReaderContext context, ExecutorService executor) {
    this(context, false, executor);
  }

  /**
   * Creates a searcher searching the provided top-level {@link ReaderContext}.
   *
   * @see ReaderContext
   * @see IndexReader#getTopReaderContext()
   * @lucene.experimental
   */
  public IndexSearcher(ReaderContext context) {
    this(context, null);
   }
   
  /** Expert: directly specify the reader, subReaders and
   *  their docID starts, and an ExecutorService.  In this
   *  case, each segment will be separately searched using the
   *  ExecutorService.  IndexSearcher will not
   *  shutdown/awaitTermination this ExecutorService on
   *  close; you must do so, eventually, on your own.  NOTE:
   *  if you are using {@link NIOFSDirectory}, do not use
   *  the shutdownNow method of ExecutorService as this uses
   *  Thread.interrupt under-the-hood which can silently
   *  close file descriptors (see <a
   *  href="https://issues.apache.org/jira/browse/LUCENE-2239">LUCENE-2239</a>).
   * 
   * @lucene.experimental */
  public IndexSearcher(IndexReader reader, IndexReader[] subReaders, int[] docStarts, ExecutorService executor) {
    this.reader = reader;
    this.subReaders = subReaders;
    this.docStarts = docStarts;
    subSearchers = new IndexSearcher[subReaders.length];
    for(int i=0;i<subReaders.length;i++) {
      subSearchers[i] = new IndexSearcher(subReaders[i]);
    }
    closeReader = false;
    this.executor = executor;
  // convinience ctor for other IR based ctors
  private IndexSearcher(IndexReader reader, boolean closeReader, ExecutorService executor) {
    this(reader.getTopReaderContext(), closeReader, executor);
   }
 
  private IndexSearcher(IndexReader r, boolean closeReader, ExecutorService executor) {
    reader = r;
  private IndexSearcher(ReaderContext context, boolean closeReader, ExecutorService executor) {
    // TODO: eable this assert once SolrIndexReader and friends are refactored to use ReaderContext
    // We can't assert this here since SolrIndexReader will fail in some contexts - once solr is consistent we should be fine here
    // Lucene instead passes all tests even with this assert!
    // assert context.isTopLevel: "IndexSearcher's ReaderContext must be topLevel for reader" + context.reader;
    reader = context.reader;
     this.executor = executor;
     this.closeReader = closeReader;

    List<IndexReader> subReadersList = new ArrayList<IndexReader>();
    gatherSubReaders(subReadersList, reader);
    subReaders = subReadersList.toArray(new IndexReader[subReadersList.size()]);
    docStarts = new int[subReaders.length];
    subSearchers = new IndexSearcher[subReaders.length];
    int maxDoc = 0;
    for (int i = 0; i < subReaders.length; i++) {
      docStarts[i] = maxDoc;
      maxDoc += subReaders[i].maxDoc();
      if (subReaders[i] == r) {
    this.readerContext = context;
    if (context.isAtomic) {
      assert context.leaves() == null : "AtomicReaderContext must not have any leaves";
      this.leafContexts = new AtomicReaderContext[] { (AtomicReaderContext) context };
    } else {
      assert context.leaves() != null : "non-atomic top-level context must have leaves";
      this.leafContexts = context.leaves();
    }
    subSearchers = new IndexSearcher[this.leafContexts.length];
    for (int i = 0; i < subSearchers.length; i++) { // TODO do we need those IS if executor is null?
      if (leafContexts[i].reader == context.reader) {
         subSearchers[i] = this;
       } else {
        subSearchers[i] = new IndexSearcher(subReaders[i]);
        subSearchers[i] = new IndexSearcher(leafContexts[i].reader.getTopReaderContext()); // we need to get a TL context for sub searchers!
       }
     }
   }
 
  protected void gatherSubReaders(List<IndexReader> allSubReaders, IndexReader r) {
    ReaderUtil.gatherSubReaders(allSubReaders, r);
  }

   /** Return the {@link IndexReader} this searches. */
   public IndexReader getIndexReader() {
     return reader;
   }
 
  /** Returns the atomic subReaders used by this searcher. */
  public IndexReader[] getSubReaders() {
    return subReaders;
  }

   /** Expert: Returns one greater than the largest possible document number.
    * 
    * @see org.apache.lucene.index.IndexReader#maxDoc()
@@ -206,7 +197,7 @@ public class IndexSearcher {
       return reader.docFreq(term);
     } else {
       final ExecutionHelper<Integer> runner = new ExecutionHelper<Integer>(executor);
      for(int i = 0; i < subReaders.length; i++) {
      for(int i = 0; i < subSearchers.length; i++) {
         final IndexSearcher searchable = subSearchers[i];
         runner.submit(new Callable<Integer>() {
             public Integer call() throws IOException {
@@ -369,9 +360,9 @@ public class IndexSearcher {
       final Lock lock = new ReentrantLock();
       final ExecutionHelper<TopDocs> runner = new ExecutionHelper<TopDocs>(executor);
     
      for (int i = 0; i < subReaders.length; i++) { // search each sub
      for (int i = 0; i < subSearchers.length; i++) { // search each sub
         runner.submit(
                      new MultiSearcherCallableNoSort(lock, subSearchers[i], weight, filter, nDocs, hq, docStarts[i]));
                      new MultiSearcherCallableNoSort(lock, subSearchers[i], weight, filter, nDocs, hq, leafContexts[i].docBase));
       }
 
       int totalHits = 0;
@@ -438,9 +429,9 @@ public class IndexSearcher {
       final FieldDocSortedHitQueue hq = new FieldDocSortedHitQueue(nDocs);
       final Lock lock = new ReentrantLock();
       final ExecutionHelper<TopFieldDocs> runner = new ExecutionHelper<TopFieldDocs>(executor);
      for (int i = 0; i < subReaders.length; i++) { // search each sub
      for (int i = 0; i < subSearchers.length; i++) { // search each sub
         runner.submit(
                      new MultiSearcherCallableWithSort(lock, subSearchers[i], weight, filter, nDocs, hq, sort, docStarts[i]));
                      new MultiSearcherCallableWithSort(lock, subSearchers[i], weight, filter, nDocs, hq, sort, leafContexts[i].docBase));
       }
       int totalHits = 0;
       float maxScore = Float.NEGATIVE_INFINITY;
@@ -484,27 +475,27 @@ public class IndexSearcher {
 
     // always use single thread:
     if (filter == null) {
      for (int i = 0; i < subReaders.length; i++) { // search each subreader
        collector.setNextReader(subReaders[i], docStarts[i]);
        Scorer scorer = weight.scorer(subReaders[i], !collector.acceptsDocsOutOfOrder(), true);
      for (int i = 0; i < leafContexts.length; i++) { // search each subreader
        collector.setNextReader(leafContexts[i].reader, leafContexts[i].docBase);
        Scorer scorer = weight.scorer(leafContexts[i], !collector.acceptsDocsOutOfOrder(), true);
         if (scorer != null) {
           scorer.score(collector);
         }
       }
     } else {
      for (int i = 0; i < subReaders.length; i++) { // search each subreader
        collector.setNextReader(subReaders[i], docStarts[i]);
        searchWithFilter(subReaders[i], weight, filter, collector);
      for (int i = 0; i < leafContexts.length; i++) { // search each subreader
        collector.setNextReader(leafContexts[i].reader, leafContexts[i].docBase);
        searchWithFilter(leafContexts[i], weight, filter, collector);
       }
     }
   }
 
  private void searchWithFilter(IndexReader reader, Weight weight,
  private void searchWithFilter(ReaderContext context, Weight weight,
       final Filter filter, final Collector collector) throws IOException {
 
     assert filter != null;
     
    Scorer scorer = weight.scorer(reader, true, false);
    Scorer scorer = weight.scorer(context, true, false);
     if (scorer == null) {
       return;
     }
@@ -513,7 +504,7 @@ public class IndexSearcher {
     assert docID == -1 || docID == DocIdSetIterator.NO_MORE_DOCS;
 
     // CHECKME: use ConjunctionScorer here?
    DocIdSet filterDocIdSet = filter.getDocIdSet(reader);
    DocIdSet filterDocIdSet = filter.getDocIdSet(context);
     if (filterDocIdSet == null) {
       // this means the filter does not accept any documents.
       return;
@@ -581,10 +572,10 @@ public class IndexSearcher {
    * @throws BooleanQuery.TooManyClauses
    */
   protected Explanation explain(Weight weight, int doc) throws IOException {
    int n = ReaderUtil.subIndex(doc, docStarts);
    int deBasedDoc = doc - docStarts[n];
    int n = ReaderUtil.subIndex(doc, leafContexts);
    int deBasedDoc = doc - leafContexts[n].docBase;
     
    return weight.explain(subReaders[n], deBasedDoc);
    return weight.explain(leafContexts[n], deBasedDoc);
   }
 
   private boolean fieldSortDoTrackScores;
@@ -615,6 +606,14 @@ public class IndexSearcher {
     return query.weight(this);
   }
 
  /**
   * Returns this searchers the top-level {@link ReaderContext}.
   * @see IndexReader#getTopReaderContext()
   */
  /* Sugar for .getIndexReader().getTopReaderContext() */
  public ReaderContext getTopReaderContext() {
    return readerContext;
  }
 
   /**
    * A thread subclass for searching a single searchable 
diff --git a/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java b/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
index eb4fcc1ae0d..38625194474 100644
-- a/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
++ b/lucene/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
@@ -18,6 +18,7 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.lucene.util.Bits;
@@ -126,13 +127,13 @@ public class MatchAllDocsQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new MatchAllScorer(reader, similarity, this,
          normsField != null ? reader.norms(normsField) : null);
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new MatchAllScorer(context.reader, similarity, this,
          normsField != null ? context.reader.norms(normsField) : null);
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc) {
    public Explanation explain(ReaderContext context, int doc) {
       // explain query weight
       Explanation queryExpl = new ComplexExplanation
         (true, getValue(), "MatchAllDocsQuery, product of:");
diff --git a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
index 2eb23cd7bfd..42b2086ea3c 100644
-- a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 import java.util.*;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsEnum;
 import org.apache.lucene.index.DocsAndPositionsEnum;
@@ -167,10 +168,10 @@ public class MultiPhraseQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       if (termArrays.size() == 0)                  // optimize zero-term case
         return null;

      final IndexReader reader = context.reader;
       final Bits delDocs = reader.getDeletedDocs();
       
       PhraseQuery.PostingsAndFreq[] postingsFreqs = new PhraseQuery.PostingsAndFreq[termArrays.size()];
@@ -219,7 +220,7 @@ public class MultiPhraseQuery extends Query {
 
       if (slop == 0) {
         ExactPhraseScorer s = new ExactPhraseScorer(this, postingsFreqs, similarity,
                                                    reader.norms(field));
            reader.norms(field));
         if (s.noDocs) {
           return null;
         } else {
@@ -232,7 +233,7 @@ public class MultiPhraseQuery extends Query {
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc)
    public Explanation explain(ReaderContext context, int doc)
       throws IOException {
       ComplexExplanation result = new ComplexExplanation();
       result.setDescription("weight("+getQuery()+" in "+doc+"), product of:");
@@ -263,7 +264,7 @@ public class MultiPhraseQuery extends Query {
       fieldExpl.setDescription("fieldWeight("+getQuery()+" in "+doc+
                                "), product of:");
 
      Scorer scorer = scorer(reader, true, false);
      Scorer scorer = scorer(context, true, false);
       if (scorer == null) {
         return new Explanation(0.0f, "no matching docs");
       }
@@ -283,7 +284,7 @@ public class MultiPhraseQuery extends Query {
       fieldExpl.addDetail(idfExpl);
 
       Explanation fieldNormExpl = new Explanation();
      byte[] fieldNorms = reader.norms(field);
      byte[] fieldNorms = context.reader.norms(field);
       float fieldNorm =
         fieldNorms!=null ? similarity.decodeNormValue(fieldNorms[doc]) : 1.0f;
       fieldNormExpl.setValue(fieldNorm);
diff --git a/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java b/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java
index 21b271a550b..6d591c8a984 100644
-- a/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java
++ b/lucene/src/java/org/apache/lucene/search/MultiTermQueryWrapperFilter.java
@@ -19,8 +19,9 @@ package org.apache.lucene.search;
 
 import java.io.IOException;
 
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.index.DocsEnum;
@@ -104,7 +105,8 @@ public class MultiTermQueryWrapperFilter<Q extends MultiTermQuery> extends Filte
    * results.
    */
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    final IndexReader reader = context.reader;
     final Fields fields = reader.fields();
     if (fields == null) {
       // reader has no fields
@@ -121,7 +123,7 @@ public class MultiTermQueryWrapperFilter<Q extends MultiTermQuery> extends Filte
     assert termsEnum != null;
     if (termsEnum.next() != null) {
       // fill into a OpenBitSet
      final OpenBitSet bitSet = new OpenBitSet(reader.maxDoc());
      final OpenBitSet bitSet = new OpenBitSet(context.reader.maxDoc());
       int termCount = 0;
       final Bits delDocs = reader.getDeletedDocs();
       DocsEnum docsEnum = null;
diff --git a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
index c5c287b84a8..7142461ef25 100644
-- a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 import java.util.Set;
 import java.util.ArrayList;
 
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsAndPositionsEnum;
 import org.apache.lucene.index.IndexReader;
@@ -174,10 +175,10 @@ public class PhraseQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       if (terms.size() == 0)			  // optimize zero-term case
         return null;

      final IndexReader reader = context.reader;
       PostingsAndFreq[] postingsFreqs = new PostingsAndFreq[terms.size()];
       final Bits delDocs = reader.getDeletedDocs();
       for (int i = 0; i < terms.size(); i++) {
@@ -206,7 +207,7 @@ public class PhraseQuery extends Query {
 
       if (slop == 0) {				  // optimize exact case
         ExactPhraseScorer s = new ExactPhraseScorer(this, postingsFreqs, similarity,
                                                    reader.norms(field));
            reader.norms(field));
         if (s.noDocs) {
           return null;
         } else {
@@ -215,12 +216,12 @@ public class PhraseQuery extends Query {
       } else {
         return
           new SloppyPhraseScorer(this, postingsFreqs, similarity, slop,
                                 reader.norms(field));
              reader.norms(field));
       }
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc)
    public Explanation explain(ReaderContext context, int doc)
       throws IOException {
 
       Explanation result = new Explanation();
@@ -267,7 +268,7 @@ public class PhraseQuery extends Query {
       fieldExpl.setDescription("fieldWeight("+field+":"+query+" in "+doc+
                                "), product of:");
 
      Scorer scorer = scorer(reader, true, false);
      Scorer scorer = scorer(context, true, false);
       if (scorer == null) {
         return new Explanation(0.0f, "no matching docs");
       }
@@ -287,7 +288,7 @@ public class PhraseQuery extends Query {
       fieldExpl.addDetail(idfExpl);
 
       Explanation fieldNormExpl = new Explanation();
      byte[] fieldNorms = reader.norms(field);
      byte[] fieldNorms = context.reader.norms(field);
       float fieldNorm =
         fieldNorms!=null ? similarity.decodeNormValue(fieldNorms[doc]) : 1.0f;
       fieldNormExpl.setValue(fieldNorm);
diff --git a/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java b/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java
index 3aa6d4d2245..a2c2c29eaa1 100644
-- a/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java
++ b/lucene/src/java/org/apache/lucene/search/QueryWrapperFilter.java
@@ -18,9 +18,7 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;


import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 /** 
  * Constrains search results to only match those which also match a provided
@@ -48,12 +46,14 @@ public class QueryWrapperFilter extends Filter {
   }
 
   @Override
  public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
    final Weight weight = query.weight(new IndexSearcher(reader));
  public DocIdSet getDocIdSet(final ReaderContext context) throws IOException {
    // get a private context that is used to rewrite, createWeight and score eventually
    final ReaderContext privateContext = context.reader.getTopReaderContext();
    final Weight weight = query.weight(new IndexSearcher(privateContext));
     return new DocIdSet() {
       @Override
       public DocIdSetIterator iterator() throws IOException {
        return weight.scorer(reader, true, false);
        return weight.scorer(privateContext, true, false);
       }
       @Override
       public boolean isCacheable() { return false; }
diff --git a/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java b/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java
index 4c8265155ea..68649be097a 100644
-- a/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java
++ b/lucene/src/java/org/apache/lucene/search/SpanQueryFilter.java
@@ -17,6 +17,7 @@ package org.apache.lucene.search;
 
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.spans.SpanQuery;
 import org.apache.lucene.search.spans.Spans;
 import org.apache.lucene.util.OpenBitSet;
@@ -52,8 +53,8 @@ public class SpanQueryFilter extends SpanFilter {
   }
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    SpanFilterResult result = bitSpans(reader);
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    SpanFilterResult result = bitSpans(context.reader);
     return result.getDocIdSet();
   }
 
diff --git a/lucene/src/java/org/apache/lucene/search/TermQuery.java b/lucene/src/java/org/apache/lucene/search/TermQuery.java
index 6eb34c6eab9..bf8346cfb50 100644
-- a/lucene/src/java/org/apache/lucene/search/TermQuery.java
++ b/lucene/src/java/org/apache/lucene/search/TermQuery.java
@@ -21,8 +21,10 @@ import java.io.IOException;
 import java.util.Set;
 
 import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.Term;
 import org.apache.lucene.search.Explanation.IDFExplanation;
 import org.apache.lucene.util.ToStringUtils;
 
@@ -75,7 +77,8 @@ public class TermQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      final IndexReader reader = context.reader;
       DocsEnum docs = reader.termDocsEnum(reader.getDeletedDocs(),
                                           term.field(),
                                           term.bytes());
@@ -88,8 +91,9 @@ public class TermQuery extends Query {
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc)
    public Explanation explain(ReaderContext context, int doc)
       throws IOException {
      final IndexReader reader = context.reader;
 
       ComplexExplanation result = new ComplexExplanation();
       result.setDescription("weight("+getQuery()+" in "+doc+"), product of:");
diff --git a/lucene/src/java/org/apache/lucene/search/Weight.java b/lucene/src/java/org/apache/lucene/search/Weight.java
index 1da8f5ef436..016904eb833 100644
-- a/lucene/src/java/org/apache/lucene/search/Weight.java
++ b/lucene/src/java/org/apache/lucene/search/Weight.java
@@ -21,16 +21,26 @@ import java.io.IOException;
 import java.io.Serializable;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 /**
  * Expert: Calculate query weights and build query scorers.
  * <p>
 * The purpose of {@link Weight} is to ensure searching does not
 * modify a {@link Query}, so that a {@link Query} instance can be reused. <br>
 * The purpose of {@link Weight} is to ensure searching does not modify a
 * {@link Query}, so that a {@link Query} instance can be reused. <br>
  * {@link IndexSearcher} dependent state of the query should reside in the
  * {@link Weight}. <br>
  * {@link IndexReader} dependent state should reside in the {@link Scorer}.
  * <p>
 * Since {@link Weight} creates {@link Scorer} instances for a given
 * {@link ReaderContext} ({@link #scorer(ReaderContext, boolean, boolean)})
 * callers must maintain the relationship between the searcher's top-level
 * {@link ReaderContext} and the context used to create a {@link Scorer}. A
 * {@link ReaderContext} used to create a {@link Scorer} should be a leaf
 * context ({@link AtomicReaderContext}) of the searcher's top-level context,
 * otherwise the scorer's state will be undefined. 
 * <p>
  * A <code>Weight</code> is used in the following way:
  * <ol>
  * <li>A <code>Weight</code> is constructed by a top-level query, given a
@@ -41,9 +51,11 @@ import org.apache.lucene.index.IndexReader;
  * query.
  * <li>The query normalization factor is passed to {@link #normalize(float)}. At
  * this point the weighting is complete.
 * <li>A <code>Scorer</code> is constructed by {@link #scorer(IndexReader,boolean,boolean)}.
 * <li>A <code>Scorer</code> is constructed by
 * {@link #scorer(ReaderContext,boolean,boolean)}.
  * </ol>
  * 
 * 
  * @since 2.9
  */
 public abstract class Weight implements Serializable {
@@ -51,12 +63,12 @@ public abstract class Weight implements Serializable {
   /**
    * An explanation of the score computation for the named document.
    * 
   * @param reader sub-reader containing the give doc
   * @param doc
   * @param context the readers context to create the {@link Explanation} for.
   * @param doc the document's id relative to the given context's reader
    * @return an Explanation for the score
   * @throws IOException
   * @throws IOException if an {@link IOException} occurs
    */
  public abstract Explanation explain(IndexReader reader, int doc) throws IOException;
  public abstract Explanation explain(ReaderContext context, int doc) throws IOException;
 
   /** The query that this concerns. */
   public abstract Query getQuery();
@@ -78,9 +90,12 @@ public abstract class Weight implements Serializable {
    * in-order.<br>
    * <b>NOTE:</b> null can be returned if no documents will be scored by this
    * query.
   * <b>NOTE: Calling this method with a {@link ReaderContext} that is not a
   * leaf context ({@link AtomicReaderContext}) of the searcher's top-level context 
   * used to create this {@link Weight} instance can cause undefined behavior.
    * 
   * @param reader
   *          the {@link IndexReader} for which to return the {@link Scorer}.
   * @param context
   *          the {@link ReaderContext} for which to return the {@link Scorer}.
    * @param scoreDocsInOrder
    *          specifies whether in-order scoring of documents is required. Note
    *          that if set to false (i.e., out-of-order scoring is required),
@@ -96,7 +111,8 @@ public abstract class Weight implements Serializable {
    * @return a {@link Scorer} which scores documents in/out-of order.
    * @throws IOException
    */
  public abstract Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
  // TODO make this context an AtomicContext if possible
  public abstract Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
       boolean topScorer) throws IOException;
   
   /** The sum of squared weights of contained query clauses. */
@@ -106,7 +122,7 @@ public abstract class Weight implements Serializable {
    * Returns true iff this implementation scores docs only out of order. This
    * method is used in conjunction with {@link Collector}'s
    * {@link Collector#acceptsDocsOutOfOrder() acceptsDocsOutOfOrder} and
   * {@link #scorer(org.apache.lucene.index.IndexReader, boolean, boolean)} to
   * {@link #scorer(ReaderContext, boolean, boolean)} to
    * create a matching {@link Scorer} instance for a given {@link Collector}, or
    * vice versa.
    * <p>
diff --git a/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java b/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java
index e1e39f3fd21..ecea73df689 100755
-- a/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java
++ b/lucene/src/java/org/apache/lucene/search/function/CustomScoreQuery.java
@@ -22,6 +22,7 @@ import java.util.Set;
 import java.util.Arrays;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.ComplexExplanation;
 import org.apache.lucene.search.Explanation;
@@ -239,40 +240,40 @@ public class CustomScoreQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
       // Pass true for "scoresDocsInOrder", because we
       // require in-order scoring, even if caller does not,
       // since we call advance on the valSrcScorers.  Pass
       // false for "topScorer" because we will not invoke
       // score(Collector) on these scorers:
      Scorer subQueryScorer = subQueryWeight.scorer(reader, true, false);
      Scorer subQueryScorer = subQueryWeight.scorer(context, true, false);
       if (subQueryScorer == null) {
         return null;
       }
       Scorer[] valSrcScorers = new Scorer[valSrcWeights.length];
       for(int i = 0; i < valSrcScorers.length; i++) {
         valSrcScorers[i] = valSrcWeights[i].scorer(reader, true, topScorer);
         valSrcScorers[i] = valSrcWeights[i].scorer(context, true, topScorer);
       }
      return new CustomScorer(similarity, reader, this, subQueryScorer, valSrcScorers);
      return new CustomScorer(similarity, context.reader, this, subQueryScorer, valSrcScorers);
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      Explanation explain = doExplain(reader, doc);
    public Explanation explain(ReaderContext context, int doc) throws IOException {
      Explanation explain = doExplain(context, doc);
       return explain == null ? new Explanation(0.0f, "no matching docs") : explain;
     }
     
    private Explanation doExplain(IndexReader reader, int doc) throws IOException {
      Explanation subQueryExpl = subQueryWeight.explain(reader, doc);
    private Explanation doExplain(ReaderContext info, int doc) throws IOException {
      Explanation subQueryExpl = subQueryWeight.explain(info, doc);
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
       }
       // match
       Explanation[] valSrcExpls = new Explanation[valSrcWeights.length];
       for(int i = 0; i < valSrcWeights.length; i++) {
        valSrcExpls[i] = valSrcWeights[i].explain(reader, doc);
        valSrcExpls[i] = valSrcWeights[i].explain(info, doc);
       }
      Explanation customExp = CustomScoreQuery.this.getCustomScoreProvider(reader).customExplain(doc,subQueryExpl,valSrcExpls);
      Explanation customExp = CustomScoreQuery.this.getCustomScoreProvider(info.reader).customExplain(doc,subQueryExpl,valSrcExpls);
       float sc = getValue() * customExp.getValue();
       Explanation res = new ComplexExplanation(
         true, sc, CustomScoreQuery.this.toString() + ", product of:");
diff --git a/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java b/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
index bf7a0216dcc..25af66e85d9 100644
-- a/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
++ b/lucene/src/java/org/apache/lucene/search/function/ValueSourceQuery.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search.function;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.lucene.util.Bits;
@@ -98,14 +99,14 @@ public class ValueSourceQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new ValueSourceScorer(similarity, reader, this);
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new ValueSourceScorer(similarity, context.reader, this);
     }
 
     /*(non-Javadoc) @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int) */
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      DocValues vals = valSrc.getValues(reader);
    public Explanation explain(ReaderContext context, int doc) throws IOException {
      DocValues vals = valSrc.getValues(context.reader);
       float sc = queryWeight * vals.floatVal(doc);
 
       Explanation result = new ComplexExplanation(
diff --git a/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java b/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java
index 37bb6c7d32c..1bb3b7a5f95 100644
-- a/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java
++ b/lucene/src/java/org/apache/lucene/search/payloads/PayloadNearQuery.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search.payloads;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.Explanation;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.IndexSearcher;
@@ -143,10 +143,10 @@ public class PayloadNearQuery extends SpanNearQuery {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
         boolean topScorer) throws IOException {
      return new PayloadNearSpanScorer(query.getSpans(reader), this,
          similarity, reader.norms(query.getField()));
      return new PayloadNearSpanScorer(query.getSpans(context.reader), this,
          similarity, context.reader.norms(query.getField()));
     }
   }
 
diff --git a/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java b/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java
index 1d251447132..4aa29583473 100644
-- a/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java
++ b/lucene/src/java/org/apache/lucene/search/payloads/PayloadTermQuery.java
@@ -17,9 +17,9 @@ package org.apache.lucene.search.payloads;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Weight;
@@ -74,10 +74,10 @@ public class PayloadTermQuery extends SpanTermQuery {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder,
         boolean topScorer) throws IOException {
      return new PayloadTermSpanScorer((TermSpans) query.getSpans(reader),
          this, similarity, reader.norms(query.getField()));
      return new PayloadTermSpanScorer((TermSpans) query.getSpans(context.reader),
          this, similarity, context.reader.norms(query.getField()));
     }
 
     protected class PayloadTermSpanScorer extends SpanScorer {
diff --git a/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java b/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java
index 37451fecb2d..6142ad42453 100644
-- a/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java
++ b/lucene/src/java/org/apache/lucene/search/spans/SpanWeight.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search.spans;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.*;
 import org.apache.lucene.search.Explanation.IDFExplanation;
@@ -72,13 +72,13 @@ public class SpanWeight extends Weight {
   }
 
   @Override
  public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    return new SpanScorer(query.getSpans(reader), this, similarity, reader
  public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    return new SpanScorer(query.getSpans(context.reader), this, similarity, context.reader
         .norms(query.getField()));
   }
 
   @Override
  public Explanation explain(IndexReader reader, int doc)
  public Explanation explain(ReaderContext context, int doc)
     throws IOException {
 
     ComplexExplanation result = new ComplexExplanation();
@@ -111,12 +111,12 @@ public class SpanWeight extends Weight {
     fieldExpl.setDescription("fieldWeight("+field+":"+query.toString(field)+
                              " in "+doc+"), product of:");
 
    Explanation tfExpl = ((SpanScorer)scorer(reader, true, false)).explain(doc);
    Explanation tfExpl = ((SpanScorer)scorer(context, true, false)).explain(doc);
     fieldExpl.addDetail(tfExpl);
     fieldExpl.addDetail(idfExpl);
 
     Explanation fieldNormExpl = new Explanation();
    byte[] fieldNorms = reader.norms(field);
    byte[] fieldNorms = context.reader.norms(field);
     float fieldNorm =
       fieldNorms!=null ? similarity.decodeNormValue(fieldNorms[doc]) : 1.0f;
     fieldNormExpl.setValue(fieldNorm);
diff --git a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
index 430fc9bf38e..bc03c9a9376 100644
-- a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
++ b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
@@ -19,9 +19,13 @@ package org.apache.lucene.util;
 
 import java.util.ArrayList;
 import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.CompositeReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 /**
  * Common util methods for dealing with {@link IndexReader}s.
@@ -148,6 +152,67 @@ public final class ReaderUtil {
         .toArray(new IndexReader[subReadersList.size()]);
     return subReaders[subIndex];
   }
  
  public static ReaderContext buildReaderContext(IndexReader reader) {
    return new ReaderContextBuilder(reader).build();
  }
  
  public static class ReaderContextBuilder {
    private final IndexReader reader;
    private final AtomicReaderContext[] leaves;
    private int leafOrd = 0;
    private int leafDocBase = 0;
    public ReaderContextBuilder(IndexReader reader) {
      this.reader = reader;
      leaves = new AtomicReaderContext[numLeaves(reader)];
    }
    
    public ReaderContext build() {
      return build(null, reader, 0, 0);
    }
    
    private ReaderContext build(CompositeReaderContext parent, IndexReader reader, int ord, int docBase) {
      IndexReader[] sequentialSubReaders = reader.getSequentialSubReaders();
      if (sequentialSubReaders == null) {
        AtomicReaderContext atomic = new AtomicReaderContext(parent, reader, ord, docBase, leafOrd, leafDocBase);
        leaves[leafOrd++] = atomic;
        leafDocBase += reader.maxDoc();
        return atomic;
      } else {
        ReaderContext[] children = new ReaderContext[sequentialSubReaders.length];
        final CompositeReaderContext newParent;
        if (parent == null) {
          newParent = new CompositeReaderContext(reader, children, leaves);
        } else {
          newParent = new CompositeReaderContext(parent, reader, ord, docBase, children);
        }
        
        int newDocBase = 0;
        for (int i = 0; i < sequentialSubReaders.length; i++) {
          build(newParent, sequentialSubReaders[i], i, newDocBase);
          newDocBase += sequentialSubReaders[i].maxDoc();
        }
        return newParent;
      }
    }
    
    private int numLeaves(IndexReader reader) {
      final AtomicInteger numLeaves = new AtomicInteger();
      try {
        new Gather(reader) {
          @Override
          protected void add(int base, IndexReader r) {
            numLeaves.incrementAndGet();
          }
        }.run();
      } catch (IOException ioe) {
        // won't happen
        throw new RuntimeException(ioe);
      }
      return numLeaves.get();
    }
    
  }
 
 
   /**
@@ -175,4 +240,30 @@ public final class ReaderUtil {
     }
     return hi;
   }
  
  /**
   * Returns index of the searcher/reader for document <code>n</code> in the
   * array used to construct this searcher/reader.
   */
  public static int subIndex(int n, AtomicReaderContext[] leaves) { // find
    // searcher/reader for doc n:
    int size = leaves.length;
    int lo = 0; // search starts array
    int hi = size - 1; // for first element less than n, return its index
    while (hi >= lo) {
      int mid = (lo + hi) >>> 1;
      int midValue = leaves[mid].docBase;
      if (n < midValue)
        hi = mid - 1;
      else if (n > midValue)
        lo = mid + 1;
      else { // found a match
        while (mid + 1 < size && leaves[mid + 1].docBase == midValue) {
          mid++; // scan to last match
        }
        return mid;
      }
    }
    return hi;
  }
 }
diff --git a/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java b/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java
index 80df5720f0f..5acd441b18d 100644
-- a/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java
++ b/lucene/src/test/org/apache/lucene/search/CachingWrapperFilterHelper.java
@@ -20,7 +20,8 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import junit.framework.Assert;
import org.apache.lucene.index.IndexReader;

import org.apache.lucene.index.IndexReader.ReaderContext;
 
 /**
  * A unit test helper class to test when the filter is getting cached and when it is not.
@@ -41,10 +42,10 @@ public class CachingWrapperFilterHelper extends CachingWrapperFilter {
   }
   
   @Override
  public synchronized DocIdSet getDocIdSet(IndexReader reader) throws IOException {
  public synchronized DocIdSet getDocIdSet(ReaderContext context) throws IOException {
 
     final int saveMissCount = missCount;
    DocIdSet docIdSet = super.getDocIdSet(reader);
    DocIdSet docIdSet = super.getDocIdSet(context);
 
     if (shouldHaveCache) {
       Assert.assertEquals("Cache should have data ", saveMissCount, missCount);
diff --git a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
index daa24c91658..25c43b69921 100644
-- a/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
++ b/lucene/src/test/org/apache/lucene/search/JustCompileSearch.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.PriorityQueue;
 
@@ -152,7 +153,7 @@ final class JustCompileSearch {
     // still added here in case someone will add abstract methods in the future.
     
     @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
       return null;
     }
   }
@@ -281,7 +282,7 @@ final class JustCompileSearch {
     }
     
     @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
       return null;
     }    
   }
@@ -333,7 +334,7 @@ final class JustCompileSearch {
   static final class JustCompileWeight extends Weight {
 
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
    public Explanation explain(ReaderContext context, int doc) throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
 
@@ -358,7 +359,7 @@ final class JustCompileSearch {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer)
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer)
         throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
diff --git a/lucene/src/test/org/apache/lucene/search/MockFilter.java b/lucene/src/test/org/apache/lucene/search/MockFilter.java
index 36b4247fa91..1ac9207e9ef 100644
-- a/lucene/src/test/org/apache/lucene/search/MockFilter.java
++ b/lucene/src/test/org/apache/lucene/search/MockFilter.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.DocIdBitSet;
 import java.util.BitSet;
 
@@ -25,7 +25,7 @@ public class MockFilter extends Filter {
   private boolean wasCalled;
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) {
  public DocIdSet getDocIdSet(ReaderContext context) {
     wasCalled = true;
     return new DocIdBitSet(new BitSet());
   }
diff --git a/lucene/src/test/org/apache/lucene/search/QueryUtils.java b/lucene/src/test/org/apache/lucene/search/QueryUtils.java
index c2c8b17fc52..fd52b748a74 100644
-- a/lucene/src/test/org/apache/lucene/search/QueryUtils.java
++ b/lucene/src/test/org/apache/lucene/search/QueryUtils.java
@@ -12,6 +12,8 @@ import junit.framework.Assert;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.MultiReader;
@@ -210,14 +212,22 @@ public class QueryUtils {
       throw e2;
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
     if (q.weight(s).scoresDocsOutOfOrder()) return;  // in this case order of skipTo() might differ from that of next().
 
     final int skip_op = 0;
@@ -247,8 +257,8 @@ public class QueryUtils {
 
         s.search(q, new Collector() {
           private Scorer sc;
          private IndexReader reader;
           private Scorer scorer;
          private int leafPtr;
 
           @Override
           public void setScorer(Scorer scorer) throws IOException {
@@ -262,7 +272,7 @@ public class QueryUtils {
             try {
               if (scorer == null) {
                 Weight w = q.weight(s);
                scorer = w.scorer(reader, true, false);
                scorer = w.scorer(context[leafPtr], true, false);
               }
               
               int op = order[(opidx[0]++) % order.length];
@@ -305,14 +315,17 @@ public class QueryUtils {
             // previous reader, hits NO_MORE_DOCS
             if (lastReader[0] != null) {
               final IndexReader previousReader = lastReader[0];
              Weight w = q.weight(new IndexSearcher(previousReader));
              Scorer scorer = w.scorer(previousReader, true, false);
              IndexSearcher indexSearcher = new IndexSearcher(previousReader);
              Weight w = q.weight(indexSearcher);
              Scorer scorer = w.scorer(indexSearcher.getTopReaderContext(), true, false);
               if (scorer != null) {
                 boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
                 Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
               }
              leafPtr++;
             }
            this.reader = lastReader[0] = reader;
            lastReader[0] = reader;
            assert context[leafPtr].reader == reader;
             this.scorer = null;
             lastDoc[0] = -1;
           }
@@ -327,8 +340,9 @@ public class QueryUtils {
           // confirm that skipping beyond the last doc, on the
           // previous reader, hits NO_MORE_DOCS
           final IndexReader previousReader = lastReader[0];
          Weight w = q.weight(new IndexSearcher(previousReader));
          Scorer scorer = w.scorer(previousReader, true, false);
          IndexSearcher indexSearcher = new IndexSearcher(previousReader);
          Weight w = q.weight(indexSearcher);
          Scorer scorer = w.scorer(previousReader.getTopReaderContext() , true, false);
           if (scorer != null) {
             boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
             Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
@@ -343,10 +357,10 @@ public class QueryUtils {
     final float maxDiff = 1e-3f;
     final int lastDoc[] = {-1};
     final IndexReader lastReader[] = {null};

    final ReaderContext[] context = getLeaves(s);
     s.search(q,new Collector() {
       private Scorer scorer;
      private IndexReader reader;
      private int leafPtr;
       @Override
       public void setScorer(Scorer scorer) throws IOException {
         this.scorer = scorer;
@@ -358,7 +372,7 @@ public class QueryUtils {
           long startMS = System.currentTimeMillis();
           for (int i=lastDoc[0]+1; i<=doc; i++) {
             Weight w = q.weight(s);
            Scorer scorer = w.scorer(reader, true, false);
            Scorer scorer = w.scorer(context[leafPtr], true, false);
             Assert.assertTrue("query collected "+doc+" but skipTo("+i+") says no more docs!",scorer.advance(i) != DocIdSetIterator.NO_MORE_DOCS);
             Assert.assertEquals("query collected "+doc+" but skipTo("+i+") got to "+scorer.docID(),doc,scorer.docID());
             float skipToScore = scorer.score();
@@ -383,15 +397,17 @@ public class QueryUtils {
         // previous reader, hits NO_MORE_DOCS
         if (lastReader[0] != null) {
           final IndexReader previousReader = lastReader[0];
          Weight w = q.weight(new IndexSearcher(previousReader));
          Scorer scorer = w.scorer(previousReader, true, false);
          IndexSearcher indexSearcher = new IndexSearcher(previousReader);
          Weight w = q.weight(indexSearcher);
          Scorer scorer = w.scorer(indexSearcher.getTopReaderContext(), true, false);
           if (scorer != null) {
             boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
             Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
           }
          leafPtr++;
         }
 
        this.reader = lastReader[0] = reader;
        lastReader[0] = reader;
         lastDoc[0] = -1;
       }
       @Override
@@ -404,8 +420,9 @@ public class QueryUtils {
       // confirm that skipping beyond the last doc, on the
       // previous reader, hits NO_MORE_DOCS
       final IndexReader previousReader = lastReader[0];
      Weight w = q.weight(new IndexSearcher(previousReader));
      Scorer scorer = w.scorer(previousReader, true, false);
      IndexSearcher indexSearcher = new IndexSearcher(previousReader);
      Weight w = q.weight(indexSearcher);
      Scorer scorer = w.scorer(indexSearcher.getTopReaderContext(), true, false);
       if (scorer != null) {
         boolean more = scorer.advance(lastDoc[0] + 1) != DocIdSetIterator.NO_MORE_DOCS;
         Assert.assertFalse("query's last doc was "+ lastDoc[0] +" but skipTo("+(lastDoc[0]+1)+") got to "+scorer.docID(),more);
diff --git a/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java b/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java
index bd1df4e3ee0..2625cda5b67 100644
-- a/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java
++ b/lucene/src/test/org/apache/lucene/search/SingleDocTestFilter.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.DocIdBitSet;
 
 import java.util.BitSet;
@@ -31,8 +31,8 @@ public class SingleDocTestFilter extends Filter {
   }
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    BitSet bits = new BitSet(reader.maxDoc());
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    BitSet bits = new BitSet(context.reader.maxDoc());
     bits.set(doc);
     return new DocIdBitSet(bits);
   }
diff --git a/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java b/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java
index f56e440b0cd..a38fe553a26 100644
-- a/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java
++ b/lucene/src/test/org/apache/lucene/search/TestCachingWrapperFilter.java
@@ -23,6 +23,8 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.SerialMergeScheduler;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
@@ -40,20 +42,20 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     writer.close();
 
     IndexReader reader = IndexReader.open(dir, true);

    ReaderContext context = reader.getTopReaderContext();
     MockFilter filter = new MockFilter();
     CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
 
     // first time, nested filter is called
    cacher.getDocIdSet(reader);
    cacher.getDocIdSet(context);
     assertTrue("first time", filter.wasCalled());
 
     // make sure no exception if cache is holding the wrong docIdSet
    cacher.getDocIdSet(reader);
    cacher.getDocIdSet(context);
 
     // second time, nested filter should not be called
     filter.clear();
    cacher.getDocIdSet(reader);
    cacher.getDocIdSet(context);
     assertFalse("second time", filter.wasCalled());
 
     reader.close();
@@ -66,17 +68,18 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     writer.close();
 
     IndexReader reader = IndexReader.open(dir, true);
    ReaderContext context = reader.getTopReaderContext();
 
     final Filter filter = new Filter() {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) {
      public DocIdSet getDocIdSet(ReaderContext context) {
         return null;
       }
     };
     CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
 
     // the caching filter should return the empty set constant
    assertSame(DocIdSet.EMPTY_DOCIDSET, cacher.getDocIdSet(reader));
    assertSame(DocIdSet.EMPTY_DOCIDSET, cacher.getDocIdSet(context));
     
     reader.close();
     dir.close();
@@ -88,10 +91,11 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     writer.close();
 
     IndexReader reader = IndexReader.open(dir, true);
    ReaderContext context = reader.getTopReaderContext();
 
     final Filter filter = new Filter() {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) {
      public DocIdSet getDocIdSet(ReaderContext context) {
         return new DocIdSet() {
           @Override
           public DocIdSetIterator iterator() {
@@ -103,16 +107,17 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
 
     // the caching filter should return the empty set constant
    assertSame(DocIdSet.EMPTY_DOCIDSET, cacher.getDocIdSet(reader));
    assertSame(DocIdSet.EMPTY_DOCIDSET, cacher.getDocIdSet(context));
     
     reader.close();
     dir.close();
   }
   
   private static void assertDocIdSetCacheable(IndexReader reader, Filter filter, boolean shouldCacheable) throws IOException {
    ReaderContext context = reader.getTopReaderContext();
     final CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
    final DocIdSet originalSet = filter.getDocIdSet(reader);
    final DocIdSet cachedSet = cacher.getDocIdSet(reader);
    final DocIdSet originalSet = filter.getDocIdSet(context);
    final DocIdSet cachedSet = cacher.getDocIdSet(context);
     assertTrue(cachedSet.isCacheable());
     assertEquals(shouldCacheable, originalSet.isCacheable());
     //System.out.println("Original: "+originalSet.getClass().getName()+" -- cached: "+cachedSet.getClass().getName());
@@ -140,7 +145,7 @@ public class TestCachingWrapperFilter extends LuceneTestCase {
     // a openbitset filter is always cacheable
     assertDocIdSetCacheable(reader, new Filter() {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) {
      public DocIdSet getDocIdSet(ReaderContext context) {
         return new OpenBitSet();
       }
     }, true);
diff --git a/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java b/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java
index b89b6897c8a..1b89d4dd7dc 100644
-- a/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestDisjunctionMaxQuery.java
@@ -25,6 +25,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.store.Directory;
 
 import java.text.DecimalFormat;
@@ -163,9 +164,9 @@ public class TestDisjunctionMaxQuery extends LuceneTestCase {
     dq.add(tq("dek", "DOES_NOT_EXIST"));
     
     QueryUtils.check(random, dq, s);
    
    assertTrue(s.getTopReaderContext().isAtomic);
     final Weight dw = dq.weight(s);
    final Scorer ds = dw.scorer(s.getIndexReader(), true, false);
    final Scorer ds = dw.scorer(s.getTopReaderContext(), true, false);
     final boolean skipOk = ds.advance(3) != DocIdSetIterator.NO_MORE_DOCS;
     if (skipOk) {
       fail("firsttime skipTo found a match? ... "
@@ -177,11 +178,10 @@ public class TestDisjunctionMaxQuery extends LuceneTestCase {
     final DisjunctionMaxQuery dq = new DisjunctionMaxQuery(0.0f);
     dq.add(tq("dek", "albino"));
     dq.add(tq("dek", "DOES_NOT_EXIST"));
    
    assertTrue(s.getTopReaderContext().isAtomic);
     QueryUtils.check(random, dq, s);
    
     final Weight dw = dq.weight(s);
    final Scorer ds = dw.scorer(s.getIndexReader(), true, false);
    final Scorer ds = dw.scorer(s.getTopReaderContext(), true, false);
     assertTrue("firsttime skipTo found no match",
         ds.advance(3) != DocIdSetIterator.NO_MORE_DOCS);
     assertEquals("found wrong docid", "d4", r.document(ds.docID()).get("id"));
diff --git a/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java b/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java
index 78f517c67c7..5ff89d5c888 100644
-- a/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java
++ b/lucene/src/test/org/apache/lucene/search/TestDocIdSet.java
@@ -28,6 +28,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field.Index;
 import org.apache.lucene.document.Field.Store;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.LuceneTestCase;
@@ -114,7 +115,7 @@ public class TestDocIdSet extends LuceneTestCase {
     // Now search w/ a Filter which returns a null DocIdSet
     Filter f = new Filter() {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         return null;
       }
     };
diff --git a/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java b/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java
index ef0bf485aff..dd1655ad2a0 100644
-- a/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestFilteredQuery.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.BooleanClause.Occur;
@@ -87,7 +88,7 @@ public class TestFilteredQuery extends LuceneTestCase {
   private static Filter newStaticFilterB() {
     return new Filter() {
       @Override
      public DocIdSet getDocIdSet (IndexReader reader) {
      public DocIdSet getDocIdSet (ReaderContext context) {
         BitSet bitset = new BitSet(5);
         bitset.set (1);
         bitset.set (3);
@@ -158,7 +159,7 @@ public class TestFilteredQuery extends LuceneTestCase {
   private static Filter newStaticFilterA() {
     return new Filter() {
       @Override
      public DocIdSet getDocIdSet (IndexReader reader) {
      public DocIdSet getDocIdSet (ReaderContext context) {
         BitSet bitset = new BitSet(5);
         bitset.set(0, 5);
         return new DocIdBitSet(bitset);
@@ -216,7 +217,7 @@ public class TestFilteredQuery extends LuceneTestCase {
     bq.add(new TermQuery(new Term("field", "two")), BooleanClause.Occur.SHOULD);
     ScoreDoc[] hits = searcher.search(query, 1000).scoreDocs;
     assertEquals(1, hits.length);
    QueryUtils.check(random, query,searcher);    
    QueryUtils.check(random, query, searcher);    
   }
 }
 
diff --git a/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java b/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java
index 951abdd246c..a4b02fa9592 100644
-- a/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java
++ b/lucene/src/test/org/apache/lucene/search/TestFilteredSearch.java
@@ -25,6 +25,8 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.IndexWriterConfig.OpenMode;
@@ -33,6 +35,7 @@ import org.apache.lucene.store.LockObtainFailedException;
 import org.apache.lucene.util.OpenBitSet;
 
 

 /**
  *
  */
@@ -59,7 +62,7 @@ public class TestFilteredSearch extends LuceneTestCase {
     directory.close();
   }
 
  public void searchFiltered(IndexWriter writer, Directory directory, SimpleDocIdSetFilter filter, boolean optimize) {
  public void searchFiltered(IndexWriter writer, Directory directory, Filter filter, boolean optimize) {
     try {
       for (int i = 0; i < 60; i++) {//Simple docs
         Document doc = new Document();
@@ -75,7 +78,6 @@ public class TestFilteredSearch extends LuceneTestCase {
      
      
       IndexSearcher indexSearcher = new IndexSearcher(directory, true);
      filter.setTopReader(indexSearcher.getIndexReader());
       ScoreDoc[] hits = indexSearcher.search(booleanQuery, filter, 1000).scoreDocs;
       assertEquals("Number of matched documents", 1, hits.length);
       indexSearcher.close();
@@ -89,20 +91,17 @@ public class TestFilteredSearch extends LuceneTestCase {
   public static final class SimpleDocIdSetFilter extends Filter {
     private final int[] docs;
     private int index;
    private IndexReader topReader;
    
     public SimpleDocIdSetFilter(int[] docs) {
       this.docs = docs;
     }
 
    public void setTopReader(IndexReader r) {
      topReader = r;
    }

     @Override
    public DocIdSet getDocIdSet(IndexReader reader) {
    public DocIdSet getDocIdSet(ReaderContext context) {
      assert context.isAtomic;
       final OpenBitSet set = new OpenBitSet();
      int docBase = topReader.getSubReaderDocBase(reader);
      final int limit = docBase+reader.maxDoc();
      int docBase = ((AtomicReaderContext)context).docBase;
      final int limit = docBase+context.reader.maxDoc();
       for (;index < docs.length; index++) {
         final int docId = docs[index];
         if(docId > limit)
diff --git a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java
index 699e4c20f8c..f4b330e8fe9 100644
-- a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java
++ b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery32.java
@@ -177,13 +177,13 @@ public class TestNumericRangeQuery32 extends LuceneTestCase {
   @Test
   public void testInverseRange() throws Exception {
     NumericRangeFilter<Integer> f = NumericRangeFilter.newIntRange("field8", 8, 1000, -1000, true, true);
    assertSame("A inverse range should return the EMPTY_DOCIDSET instance", DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader())));
    assertSame("A inverse range should return the EMPTY_DOCIDSET instance", DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
     f = NumericRangeFilter.newIntRange("field8", 8, Integer.MAX_VALUE, null, false, false);
     assertSame("A exclusive range starting with Integer.MAX_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader())));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
     f = NumericRangeFilter.newIntRange("field8", 8, null, Integer.MIN_VALUE, false, false);
     assertSame("A exclusive range ending with Integer.MIN_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader())));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
   }
   
   @Test
diff --git a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java
index 63a3409a8c6..2e5c3e8b256 100644
-- a/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java
++ b/lucene/src/test/org/apache/lucene/search/TestNumericRangeQuery64.java
@@ -182,13 +182,14 @@ public class TestNumericRangeQuery64 extends LuceneTestCase {
   @Test
   public void testInverseRange() throws Exception {
     NumericRangeFilter<Long> f = NumericRangeFilter.newLongRange("field8", 8, 1000L, -1000L, true, true);
    assertSame("A inverse range should return the EMPTY_DOCIDSET instance", DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader())));
    assertSame("A inverse range should return the EMPTY_DOCIDSET instance", DocIdSet.EMPTY_DOCIDSET,
        f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
     f = NumericRangeFilter.newLongRange("field8", 8, Long.MAX_VALUE, null, false, false);
     assertSame("A exclusive range starting with Long.MAX_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader())));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
     f = NumericRangeFilter.newLongRange("field8", 8, null, Long.MIN_VALUE, false, false);
     assertSame("A exclusive range ending with Long.MIN_VALUE should return the EMPTY_DOCIDSET instance",
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader())));
               DocIdSet.EMPTY_DOCIDSET, f.getDocIdSet(new SlowMultiReaderWrapper(searcher.getIndexReader()).getTopReaderContext()));
   }
   
   @Test
diff --git a/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java b/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
index 7aba01f55ee..364b452ce23 100755
-- a/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
++ b/lucene/src/test/org/apache/lucene/search/TestScorerPerf.java
@@ -7,6 +7,7 @@ import java.util.BitSet;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.IndexWriterConfig.OpenMode;
@@ -141,7 +142,7 @@ public class TestScorerPerf extends LuceneTestCase {
     final BitSet rnd = sets[random.nextInt(sets.length)];
     Query q = new ConstantScoreQuery(new Filter() {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) {
      public DocIdSet getDocIdSet(ReaderContext context) {
         return new DocIdBitSet(rnd);
       }
     });
diff --git a/lucene/src/test/org/apache/lucene/search/TestSort.java b/lucene/src/test/org/apache/lucene/search/TestSort.java
index a4b22d654e0..b0889f21332 100644
-- a/lucene/src/test/org/apache/lucene/search/TestSort.java
++ b/lucene/src/test/org/apache/lucene/search/TestSort.java
@@ -34,6 +34,7 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.CorruptIndexException;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.MultiReader;
@@ -687,9 +688,9 @@ public class TestSort extends LuceneTestCase implements Serializable {
     // a filter that only allows through the first hit
     Filter filt = new Filter() {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
        BitSet bs = new BitSet(reader.maxDoc());
        bs.set(0, reader.maxDoc());
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
        BitSet bs = new BitSet(context.reader.maxDoc());
        bs.set(0, context.reader.maxDoc());
         bs.set(docs1.scoreDocs[0].doc);
         return new DocIdBitSet(bs);
       }
diff --git a/lucene/src/test/org/apache/lucene/search/TestTermScorer.java b/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
index 53900f67b84..7265ada5b1c 100644
-- a/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
++ b/lucene/src/test/org/apache/lucene/search/TestTermScorer.java
@@ -28,6 +28,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.store.Directory;
 
 public class TestTermScorer extends LuceneTestCase {
@@ -71,7 +72,7 @@ public class TestTermScorer extends LuceneTestCase {
     
     Weight weight = termQuery.weight(indexSearcher);
     
    Scorer ts = weight.scorer(indexSearcher.getIndexReader(), true, true);
    Scorer ts = weight.scorer(indexSearcher.getTopReaderContext(), true, true);
     // we have 2 documents with the term all in them, one document for all the
     // other values
     final List<TestHit> docs = new ArrayList<TestHit>();
@@ -132,7 +133,7 @@ public class TestTermScorer extends LuceneTestCase {
     
     Weight weight = termQuery.weight(indexSearcher);
     
    Scorer ts = weight.scorer(indexSearcher.getIndexReader(), true, true);
    Scorer ts = weight.scorer(indexSearcher.getTopReaderContext(), true, true);
     assertTrue("next did not return a doc",
         ts.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);
     assertTrue("score is not correct", ts.score() == 1.6931472f);
@@ -150,7 +151,7 @@ public class TestTermScorer extends LuceneTestCase {
     
     Weight weight = termQuery.weight(indexSearcher);
     
    Scorer ts = weight.scorer(indexSearcher.getIndexReader(), true, true);
    Scorer ts = weight.scorer(indexSearcher.getTopReaderContext(), true, true);
     assertTrue("Didn't skip", ts.advance(3) != DocIdSetIterator.NO_MORE_DOCS);
     // The next doc should be doc 5
     assertTrue("doc should be number 5", ts.docID() == 5);
diff --git a/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java b/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java
index d5b6b406a05..fc3fb442bab 100644
-- a/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java
++ b/lucene/src/test/org/apache/lucene/search/spans/TestNearSpansOrdered.java
@@ -168,7 +168,7 @@ public class TestNearSpansOrdered extends LuceneTestCase {
   public void testSpanNearScorerSkipTo1() throws Exception {
     SpanNearQuery q = makeQuery();
     Weight w = q.weight(searcher);
    Scorer s = w.scorer(searcher.getIndexReader(), true, false);
    Scorer s = w.scorer(searcher.getTopReaderContext(), true, false);
     assertEquals(1, s.advance(1));
   }
   /**
@@ -177,7 +177,7 @@ public class TestNearSpansOrdered extends LuceneTestCase {
    */
   public void testSpanNearScorerExplain() throws Exception {
     SpanNearQuery q = makeQuery();
    Explanation e = q.weight(searcher).explain(searcher.getIndexReader(), 1);
    Explanation e = q.weight(searcher).explain(searcher.getTopReaderContext(), 1);
     assertTrue("Scorer explanation value for doc#1 isn't positive: "
                + e.toString(),
                0.0f < e.getValue());
diff --git a/lucene/src/test/org/apache/lucene/search/spans/TestSpans.java b/lucene/src/test/org/apache/lucene/search/spans/TestSpans.java
index ac96892d5a0..9d3c83adbae 100644
-- a/lucene/src/test/org/apache/lucene/search/spans/TestSpans.java
++ b/lucene/src/test/org/apache/lucene/search/spans/TestSpans.java
@@ -29,6 +29,7 @@ import org.apache.lucene.store.Directory;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.index.SlowMultiReaderWrapper;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.RandomIndexWriter;
@@ -420,7 +421,7 @@ public class TestSpans extends LuceneTestCase {
       }
     };
 
    Scorer spanScorer = snq.weight(searcher).scorer(new SlowMultiReaderWrapper(searcher.getIndexReader()), true, false);
    Scorer spanScorer = snq.weight(searcher).scorer(new AtomicReaderContext(new SlowMultiReaderWrapper(searcher.getIndexReader())), true, false);
 
     assertTrue("first doc", spanScorer.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);
     assertEquals("first doc number", spanScorer.docID(), 11);
diff --git a/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java b/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java
index fc1016597a7..24938cbb9c2 100755
-- a/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java
++ b/solr/src/java/org/apache/solr/request/PerSegmentSingleValuedFaceting.java
@@ -1,5 +1,8 @@
 package org.apache.solr.request;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
@@ -67,10 +70,7 @@ class PerSegmentSingleValuedFaceting {
     // reuse the translation logic to go from top level set to per-segment set
     baseSet = docs.getTopFilter();
 
    SolrIndexReader topReader = searcher.getReader();
    final SolrIndexReader[] leafReaders = topReader.getLeafReaders();
    int[] offsets = topReader.getLeafOffsets();

    final AtomicReaderContext[] leaves = searcher.getTopReaderContext().leaves();
     // The list of pending tasks that aren't immediately submitted
     // TODO: Is there a completion service, or a delegating executor that can
     // limit the number of concurrent tasks submitted to a bigger executor?
@@ -78,8 +78,8 @@ class PerSegmentSingleValuedFaceting {
 
     int threads = nThreads <= 0 ? Integer.MAX_VALUE : nThreads;
 
    for (int i=0; i<leafReaders.length; i++) {
      final SegFacet segFacet = new SegFacet(leafReaders[i], offsets[i]);
    for (int i=0; i<leaves.length; i++) {
      final SegFacet segFacet = new SegFacet(leaves[i]);
 
       Callable<SegFacet> task = new Callable<SegFacet>() {
         public SegFacet call() throws Exception {
@@ -101,7 +101,7 @@ class PerSegmentSingleValuedFaceting {
     // now merge the per-segment results
     PriorityQueue<SegFacet> queue = new PriorityQueue<SegFacet>() {
       {
        initialize(leafReaders.length);
        initialize(leaves.length);
       }
       @Override
       protected boolean lessThan(SegFacet a, SegFacet b) {
@@ -112,7 +112,7 @@ class PerSegmentSingleValuedFaceting {
 
     boolean hasMissingCount=false;
     int missingCount=0;
    for (int i=0; i<leafReaders.length; i++) {
    for (int i=0; i<leaves.length; i++) {
       SegFacet seg = null;
 
       try {
@@ -209,12 +209,9 @@ class PerSegmentSingleValuedFaceting {
   }
 
   class SegFacet {
    SolrIndexReader reader;
    int readerOffset;

    SegFacet(SolrIndexReader reader, int readerOffset) {
      this.reader = reader;
      this.readerOffset = readerOffset;
    ReaderContext info;
    SegFacet(ReaderContext info) {
      this.info = info;
     }
     
     FieldCache.DocTermsIndex si;
@@ -228,7 +225,7 @@ class PerSegmentSingleValuedFaceting {
     BytesRef tempBR = new BytesRef();
 
     void countTerms() throws IOException {
      si = FieldCache.DEFAULT.getTermsIndex(reader, fieldName);
      si = FieldCache.DEFAULT.getTermsIndex(info.reader, fieldName);
       // SolrCore.log.info("reader= " + reader + "  FC=" + System.identityHashCode(si));
 
       if (prefix!=null) {
@@ -250,7 +247,7 @@ class PerSegmentSingleValuedFaceting {
         // count collection array only needs to be as big as the number of terms we are
         // going to collect counts for.
         final int[] counts = this.counts = new int[nTerms];
        DocIdSet idSet = baseSet.getDocIdSet(reader);
        DocIdSet idSet = baseSet.getDocIdSet(info);
         DocIdSetIterator iter = idSet.iterator();
 
 
diff --git a/solr/src/java/org/apache/solr/schema/LatLonType.java b/solr/src/java/org/apache/solr/schema/LatLonType.java
index 5bda359d146..c76187fd325 100644
-- a/solr/src/java/org/apache/solr/schema/LatLonType.java
++ b/solr/src/java/org/apache/solr/schema/LatLonType.java
@@ -19,6 +19,7 @@ package org.apache.solr.schema;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.search.*;
 import org.apache.lucene.spatial.DistanceUtils;
@@ -27,7 +28,6 @@ import org.apache.lucene.util.Bits;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.response.TextResponseWriter;
 import org.apache.solr.search.QParser;
import org.apache.solr.search.SolrIndexReader;
 import org.apache.solr.search.SpatialOptions;
 import org.apache.solr.search.function.DocValues;
 import org.apache.solr.search.function.ValueSource;
@@ -371,18 +371,13 @@ class SpatialDistanceQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new SpatialScorer(getSimilarity(searcher), reader, this);
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new SpatialScorer(getSimilarity(searcher), context.reader, this);
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      SolrIndexReader topReader = (SolrIndexReader)reader;
      SolrIndexReader[] subReaders = topReader.getLeafReaders();
      int[] offsets = topReader.getLeafOffsets();
      int readerPos = SolrIndexReader.readerIndex(doc, offsets);
      int readerBase = offsets[readerPos];
      return ((SpatialScorer)scorer(subReaders[readerPos], true, true)).explain(doc-readerBase);
    public Explanation explain(ReaderContext context, int doc) throws IOException {
      return ((SpatialScorer)scorer(context, true, true)).explain(doc);
     }
   }
 
diff --git a/solr/src/java/org/apache/solr/search/DocSet.java b/solr/src/java/org/apache/solr/search/DocSet.java
index a053e057a81..32dc4f53e94 100644
-- a/solr/src/java/org/apache/solr/search/DocSet.java
++ b/solr/src/java/org/apache/solr/search/DocSet.java
@@ -23,6 +23,7 @@ import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 import java.io.IOException;
 
@@ -246,8 +247,9 @@ abstract class DocSetBase implements DocSet {
 
     return new Filter() {
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext ctx) throws IOException {
         int offset = 0;
        IndexReader reader = ctx.reader;
         SolrIndexReader r = (SolrIndexReader)reader;
         while (r.getParent() != null) {
           offset += r.getBase();
diff --git a/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java b/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
index 50c94775285..422f5926199 100755
-- a/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
++ b/solr/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
@@ -2,6 +2,7 @@ package org.apache.solr.search;
 
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.solr.search.function.ValueSource;
 import org.apache.solr.common.SolrException;
 
@@ -89,14 +90,14 @@ public class SolrConstantScoreQuery extends ConstantScoreQuery {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new ConstantScorer(similarity, reader, this);
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new ConstantScorer(similarity, context, this);
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
    public Explanation explain(ReaderContext context, int doc) throws IOException {
 
      ConstantScorer cs = new ConstantScorer(similarity, reader, this);
      ConstantScorer cs = new ConstantScorer(similarity, context, this);
       boolean exists = cs.docIdSetIterator.advance(doc) == doc;
 
       ComplexExplanation result = new ComplexExplanation();
@@ -123,10 +124,10 @@ public class SolrConstantScoreQuery extends ConstantScoreQuery {
     final float theScore;
     int doc = -1;
 
    public ConstantScorer(Similarity similarity, IndexReader reader, ConstantWeight w) throws IOException {
    public ConstantScorer(Similarity similarity, ReaderContext info, ConstantWeight w) throws IOException {
       super(similarity);
       theScore = w.getValue();
      DocIdSet docIdSet = filter instanceof SolrFilter ? ((SolrFilter)filter).getDocIdSet(w.context, reader) : filter.getDocIdSet(reader);
      DocIdSet docIdSet = filter instanceof SolrFilter ? ((SolrFilter)filter).getDocIdSet(w.context, info) : filter.getDocIdSet(info);
       if (docIdSet == null) {
         docIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
       } else {
diff --git a/solr/src/java/org/apache/solr/search/SolrFilter.java b/solr/src/java/org/apache/solr/search/SolrFilter.java
index 2a368c30e96..91009320b3b 100644
-- a/solr/src/java/org/apache/solr/search/SolrFilter.java
++ b/solr/src/java/org/apache/solr/search/SolrFilter.java
@@ -21,6 +21,7 @@ import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 import java.util.Map;
 import java.io.IOException;
@@ -37,10 +38,10 @@ public abstract class SolrFilter extends Filter {
    * The context object will be passed to getDocIdSet() where this info can be retrieved. */
   public abstract void createWeight(Map context, IndexSearcher searcher) throws IOException;
   
  public abstract DocIdSet getDocIdSet(Map context, IndexReader reader) throws IOException;
  public abstract DocIdSet getDocIdSet(Map context, ReaderContext readerContext) throws IOException;
 
   @Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    return getDocIdSet(null, reader);
  public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
    return getDocIdSet(null, context);
   }
 }
diff --git a/solr/src/java/org/apache/solr/search/SolrIndexReader.java b/solr/src/java/org/apache/solr/search/SolrIndexReader.java
index 0a3b8c81b8f..72536896377 100755
-- a/solr/src/java/org/apache/solr/search/SolrIndexReader.java
++ b/solr/src/java/org/apache/solr/search/SolrIndexReader.java
@@ -19,12 +19,14 @@ package org.apache.solr.search;
 
 
 import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.LockObtainFailedException;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ReaderUtil;
 
 import java.io.IOException;
 import java.util.Collection;
@@ -40,6 +42,7 @@ public class SolrIndexReader extends FilterIndexReader {
   private int[] leafOffsets;
   private final SolrIndexReader parent;
   private final int base; // docid offset of this reader within parent
  private final ReaderContext topLevelContext;
 
   private static int[] zeroIntArray = new int[]{0};
 
@@ -79,7 +82,7 @@ public class SolrIndexReader extends FilterIndexReader {
       leafReaders = new SolrIndexReader[]{this};
       leafOffsets = zeroIntArray;
     }

    topLevelContext = ReaderUtil.buildReaderContext(this);
   }
 
   private SolrIndexReader[] getLeaves(int numLeaves) {
@@ -363,11 +366,6 @@ public class SolrIndexReader extends FilterIndexReader {
     return subReaders;
   }
 
  @Override
  public int getSubReaderDocBase(IndexReader subReader) {
    return in.getSubReaderDocBase(subReader);
  }

   @Override
   public int hashCode() {
     return in.hashCode();
@@ -493,6 +491,11 @@ public class SolrIndexReader extends FilterIndexReader {
   public int getTermInfosIndexDivisor() {
     return in.getTermInfosIndexDivisor();
   }
  
  @Override
  public ReaderContext getTopReaderContext() {
    return topLevelContext;
  }
 }
 
 
diff --git a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
index 569de901800..d7dcff8525b 100644
-- a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
++ b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
@@ -904,7 +904,7 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
   * This method is not cache-aware and no caches are checked.
   */
   public DocSet convertFilter(Filter lfilter) throws IOException {
    DocIdSet docSet = lfilter.getDocIdSet(this.reader);
    DocIdSet docSet = lfilter.getDocIdSet(this.reader.getTopReaderContext());
     OpenBitSet obs = new OpenBitSet();
     DocIdSetIterator it = docSet.iterator();
     int doc;
diff --git a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
index 661a4338bb6..b0bb860407b 100755
-- a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
++ b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
@@ -22,6 +22,7 @@ import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 
 import java.io.IOException;
 
@@ -551,8 +552,9 @@ public class SortedIntDocSet extends DocSetBase {
       int lastEndIdx = 0;
 
       @Override
      public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
      public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
         int offset = 0;
        IndexReader reader = context.reader;
         SolrIndexReader r = (SolrIndexReader)reader;
         while (r.getParent() != null) {
           offset += r.getBase();
diff --git a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
index ad45f7bb15f..c1ea5e97870 100755
-- a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
++ b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
@@ -19,6 +19,7 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.util.ToStringUtils;
 import org.apache.solr.search.SolrIndexReader;
 
@@ -91,33 +92,26 @@ public class BoostedQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      Scorer subQueryScorer = qWeight.scorer(reader, true, false);
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      Scorer subQueryScorer = qWeight.scorer(context, true, false);
       if(subQueryScorer == null) {
         return null;
       }
      return new BoostedQuery.CustomScorer(getSimilarity(searcher), searcher, reader, this, subQueryScorer, boostVal);
      return new BoostedQuery.CustomScorer(getSimilarity(searcher), searcher, context.reader, this, subQueryScorer, boostVal);
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      SolrIndexReader topReader = (SolrIndexReader)reader;
      SolrIndexReader[] subReaders = topReader.getLeafReaders();
      int[] offsets = topReader.getLeafOffsets();
      int readerPos = SolrIndexReader.readerIndex(doc, offsets);
      int readerBase = offsets[readerPos];

      Explanation subQueryExpl = qWeight.explain(reader,doc);
    public Explanation explain(ReaderContext readerContext, int doc) throws IOException {
      Explanation subQueryExpl = qWeight.explain(readerContext,doc);
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
       }

      DocValues vals = boostVal.getValues(context, subReaders[readerPos]);
      float sc = subQueryExpl.getValue() * vals.floatVal(doc-readerBase);
      DocValues vals = boostVal.getValues(context, readerContext.reader);
      float sc = subQueryExpl.getValue() * vals.floatVal(doc);
       Explanation res = new ComplexExplanation(
         true, sc, BoostedQuery.this.toString() + ", product of:");
       res.addDetail(subQueryExpl);
      res.addDetail(vals.explain(doc-readerBase));
      res.addDetail(vals.explain(doc));
       return res;
     }
   }
@@ -168,7 +162,7 @@ public class BoostedQuery extends Query {
     }
 
     public Explanation explain(int doc) throws IOException {
      Explanation subQueryExpl = weight.qWeight.explain(reader,doc);
      Explanation subQueryExpl = weight.qWeight.explain(reader.getTopReaderContext() ,doc);
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
       }
diff --git a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
index 6a8f5f40072..397798f064e 100644
-- a/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
++ b/solr/src/java/org/apache/solr/search/function/FunctionQuery.java
@@ -18,10 +18,10 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.search.*;
 import org.apache.lucene.index.MultiFields;
 import org.apache.lucene.util.Bits;
import org.apache.solr.search.SolrIndexReader;
 
 import java.io.IOException;
 import java.util.Set;
@@ -94,18 +94,13 @@ public class FunctionQuery extends Query {
     }
 
     @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new AllScorer(getSimilarity(searcher), reader, this);
    public Scorer scorer(ReaderContext context, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new AllScorer(getSimilarity(searcher), context, this);
     }
 
     @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      SolrIndexReader topReader = (SolrIndexReader)reader;
      SolrIndexReader[] subReaders = topReader.getLeafReaders();
      int[] offsets = topReader.getLeafOffsets();
      int readerPos = SolrIndexReader.readerIndex(doc, offsets);
      int readerBase = offsets[readerPos];
      return ((AllScorer)scorer(subReaders[readerPos], true, true)).explain(doc-readerBase);
    public Explanation explain(ReaderContext context, int doc) throws IOException {
      return ((AllScorer)scorer(context, true, true)).explain(doc);
     }
   }
 
@@ -119,16 +114,18 @@ public class FunctionQuery extends Query {
     final boolean hasDeletions;
     final Bits delDocs;
 
    public AllScorer(Similarity similarity, IndexReader reader, FunctionWeight w) throws IOException {
    public AllScorer(Similarity similarity, ReaderContext context, FunctionWeight w) throws IOException {
       super(similarity);
       this.weight = w;
       this.qWeight = w.getValue();
      this.reader = reader;
      this.reader = context.reader;
       this.maxDoc = reader.maxDoc();
       this.hasDeletions = reader.hasDeletions();
       this.delDocs = MultiFields.getDeletedDocs(reader);
       assert !hasDeletions || delDocs != null;
      vals = func.getValues(weight.context, reader);
      Map funcContext = weight.context;
      funcContext.put(reader, context);
      vals = func.getValues(funcContext, reader);
     }
 
     @Override
diff --git a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
index 37bc35e7a68..cf65b3968fe 100755
-- a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
@@ -90,7 +90,7 @@ class QueryDocValues extends DocValues {
     try {
       if (doc < lastDocRequested) {
         // out-of-order access.... reset scorer.
        scorer = weight.scorer(reader, true, false);
        scorer = weight.scorer(reader.getTopReaderContext(), true, false);
         if (scorer==null) return defVal;
         scorerDoc = -1;
       }
diff --git a/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java b/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java
index 581792c9039..8813736a849 100755
-- a/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java
++ b/solr/src/java/org/apache/solr/search/function/ValueSourceRangeFilter.java
@@ -20,7 +20,7 @@ package org.apache.solr.search.function;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.solr.search.SolrFilter;
 
 import java.io.IOException;
@@ -49,10 +49,10 @@ public class ValueSourceRangeFilter extends SolrFilter {
     this.includeUpper = upperVal != null && includeUpper;
   }
 
  public DocIdSet getDocIdSet(final Map context, final IndexReader reader) throws IOException {
  public DocIdSet getDocIdSet(final Map context, final ReaderContext readerContext) throws IOException {
      return new DocIdSet() {
        public DocIdSetIterator iterator() throws IOException {
         return valueSource.getValues(context, reader).getRangeScorer(reader, lowerVal, upperVal, includeLower, includeUpper);
         return valueSource.getValues(context, readerContext.reader).getRangeScorer(readerContext.reader, lowerVal, upperVal, includeLower, includeUpper);
        }
      };
   }
diff --git a/solr/src/test/org/apache/solr/common/util/ContentStreamTest.java b/solr/src/test/org/apache/solr/common/util/ContentStreamTest.java
index 5aa7138d5b7..01e0b6985f9 100755
-- a/solr/src/test/org/apache/solr/common/util/ContentStreamTest.java
++ b/solr/src/test/org/apache/solr/common/util/ContentStreamTest.java
@@ -25,6 +25,7 @@ import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.StringReader;
import java.net.ConnectException;
 import java.net.URL;
 
 import org.apache.commons.io.IOUtils;
@@ -65,12 +66,16 @@ public class ContentStreamTest extends LuceneTestCase
   {
     String content = null;
     URL url = new URL( "http://svn.apache.org/repos/asf/lucene/dev/trunk/" );
    InputStream in = url.openStream();
    InputStream in = null;
     try {
      in = url.openStream();
       content = IOUtils.toString( in );
    } 
    finally {
      IOUtils.closeQuietly(in);
    } catch (ConnectException ex) {
      assumeNoException("Unable to connect to " + url + " to run the test.", ex);
    }finally {
      if (in != null) {
        IOUtils.closeQuietly(in);
      }
     }
     
     assertTrue( content.length() > 10 ); // found something...
diff --git a/solr/src/test/org/apache/solr/search/TestDocSet.java b/solr/src/test/org/apache/solr/search/TestDocSet.java
index e52aecbf115..280e58d39ae 100644
-- a/solr/src/test/org/apache/solr/search/TestDocSet.java
++ b/solr/src/test/org/apache/solr/search/TestDocSet.java
@@ -26,6 +26,7 @@ import org.apache.lucene.util.OpenBitSet;
 import org.apache.lucene.util.OpenBitSetIterator;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.MultiReader;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.DocIdSet;
@@ -404,6 +405,7 @@ public class TestDocSet extends LuceneTestCase {
   }
 
   public void doFilterTest(SolrIndexReader reader) throws IOException {
    ReaderContext topLevelContext = reader.getTopReaderContext();
     OpenBitSet bs = getRandomSet(reader.maxDoc(), rand.nextInt(reader.maxDoc()+1));
     DocSet a = new BitDocSet(bs);
     DocSet b = getIntDocSet(bs);
@@ -412,23 +414,23 @@ public class TestDocSet extends LuceneTestCase {
     Filter fb = b.getTopFilter();
 
     // test top-level
    DocIdSet da = fa.getDocIdSet(reader);
    DocIdSet db = fb.getDocIdSet(reader);
    DocIdSet da = fa.getDocIdSet(topLevelContext);
    DocIdSet db = fb.getDocIdSet(topLevelContext);
     doTestIteratorEqual(da, db);
 
     // first test in-sequence sub readers
    for (SolrIndexReader sir : reader.getLeafReaders()) {
      da = fa.getDocIdSet(sir);
      db = fb.getDocIdSet(sir);
    for (ReaderContext readerInfo : topLevelContext.leaves()) {
      da = fa.getDocIdSet(readerInfo);
      db = fb.getDocIdSet(readerInfo);
       doTestIteratorEqual(da, db);
     }  
 
    int nReaders = reader.getLeafReaders().length;
    int nReaders = topLevelContext.leaves().length;
     // now test out-of-sequence sub readers
     for (int i=0; i<nReaders; i++) {
      SolrIndexReader sir = reader.getLeafReaders()[rand.nextInt(nReaders)];
      da = fa.getDocIdSet(sir);
      db = fb.getDocIdSet(sir);
      ReaderContext readerInfo = topLevelContext.leaves()[rand.nextInt(nReaders)];
      da = fa.getDocIdSet(readerInfo);
      db = fb.getDocIdSet(readerInfo);
       doTestIteratorEqual(da, db);
     }
   }
diff --git a/solr/src/test/org/apache/solr/search/TestSort.java b/solr/src/test/org/apache/solr/search/TestSort.java
index 23069740acd..60b46237416 100755
-- a/solr/src/test/org/apache/solr/search/TestSort.java
++ b/solr/src/test/org/apache/solr/search/TestSort.java
@@ -21,6 +21,7 @@ import org.apache.lucene.analysis.core.SimpleAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderContext;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.search.*;
@@ -106,8 +107,8 @@ public class TestSort extends AbstractSolrTestCase {
       for (int i=0; i<qiter; i++) {
         Filter filt = new Filter() {
           @Override
          public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
            return randSet(reader.maxDoc());
          public DocIdSet getDocIdSet(ReaderContext context) throws IOException {
            return randSet(context.reader.maxDoc());
           }
         };
 
diff --git a/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java b/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
index 2e05af23d3d..dbe1c512ae0 100644
-- a/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
++ b/solr/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
@@ -245,7 +245,7 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     SolrQueryRequest sr = req("q","foo");
     SolrIndexReader r = sr.getSearcher().getReader();
     assertTrue(r.maxDoc() > r.numDocs());   // should have deletions
    assertTrue(r.getLeafReaders().length > 1);  // more than 1 segment
    assertFalse(r.getTopReaderContext().isAtomic);  // more than 1 segment
     sr.close();
 
     assertU(commit("expungeDeletes","true"));
@@ -254,7 +254,7 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     r = sr.getSearcher().getReader();
     assertEquals(r.maxDoc(), r.numDocs());  // no deletions
     assertEquals(4,r.maxDoc());             // no dups
    assertTrue(r.getLeafReaders().length > 1);  // still more than 1 segment
    assertFalse(r.getTopReaderContext().isAtomic);  //still more than 1 segment
     sr.close();
   }
   
- 
2.19.1.windows.1

