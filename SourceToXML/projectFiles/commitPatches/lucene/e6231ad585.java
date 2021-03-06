From e6231ad585c5a1799d881ebf2f25a917546fe795 Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Sat, 23 Feb 2013 16:28:13 +0000
Subject: [PATCH] SOLR-3855: Fix faceting on numeric fields with precisionStep
 < Integer.MAX_VALUE, facet.mincount=0 and facet.method=fcs.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1449360 13f79535-47bb-0310-9956-ffa450edef68
--
 .../handler/component/StatsComponent.java     |   6 +-
 .../apache/solr/request/NumericFacets.java    |  50 ++++++++-
 .../org/apache/solr/request/SimpleFacets.java | 103 ++++++++++++------
 .../org/apache/solr/request/TestFaceting.java |  30 +++++
 4 files changed, 142 insertions(+), 47 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java b/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
index dc433b42783..377613e0aee 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
@@ -35,7 +35,6 @@ import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.UnInvertedField;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TrieField;
 import org.apache.solr.search.DocIterator;
 import org.apache.solr.search.DocSet;
 import org.apache.solr.search.SolrIndexSearcher;
@@ -214,10 +213,7 @@ class SimpleStats {
         FieldType ft = sf.getType();
         NamedList<?> stv;
 
        // Currently, only UnInvertedField can deal with multi-part trie fields
        String prefix = TrieField.getMainValuePrefix(ft);

        if (sf.multiValued() || ft.multiValuedFieldCache() || prefix!=null) {
        if (sf.multiValued() || ft.multiValuedFieldCache()) {
           //use UnInvertedField for multivalued fields
           UnInvertedField uif = UnInvertedField.getUnInvertedField(f, searcher);
           stv = uif.getStats(searcher, docs, facets).getStatsValues();
diff --git a/solr/core/src/java/org/apache/solr/request/NumericFacets.java b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
index 7a6ec4c9861..a19356bf4c5 100644
-- a/solr/core/src/java/org/apache/solr/request/NumericFacets.java
++ b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
@@ -40,10 +40,12 @@ import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.CharsRef;
 import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.StringHelper;
 import org.apache.solr.common.params.FacetParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TrieField;
 import org.apache.solr.search.DocIterator;
 import org.apache.solr.search.DocSet;
 import org.apache.solr.search.SolrIndexSearcher;
@@ -266,10 +268,28 @@ final class NumericFacets {
         }
         final Terms terms = searcher.getAtomicReader().terms(fieldName);
         if (terms != null) {
          final String prefixStr = TrieField.getMainValuePrefix(ft);
          final BytesRef prefix;
          if (prefixStr != null) {
            prefix = new BytesRef(prefixStr);
          } else {
            prefix = new BytesRef();
          }
           final TermsEnum termsEnum = terms.iterator(null);
          BytesRef term = termsEnum.next();
          BytesRef term;
          switch (termsEnum.seekCeil(prefix)) {
            case FOUND:
            case NOT_FOUND:
              term = termsEnum.term();
              break;
            case END:
              term = null;
              break;
            default:
              throw new AssertionError();
          }
           final CharsRef spare = new CharsRef();
          for (int skipped = hashTable.size; skipped < offset && term != null; ) {
          for (int skipped = hashTable.size; skipped < offset && term != null && StringHelper.startsWith(term, prefix); ) {
             ft.indexedToReadable(term, spare);
             final String termStr = spare.toString();
             if (!alreadySeen.contains(termStr)) {
@@ -277,7 +297,7 @@ final class NumericFacets {
             }
             term = termsEnum.next();
           }
          for ( ; term != null && (limit < 0 || result.size() < limit); term = termsEnum.next()) {
          for ( ; term != null && StringHelper.startsWith(term, prefix) && (limit < 0 || result.size() < limit); term = termsEnum.next()) {
             ft.indexedToReadable(term, spare);
             final String termStr = spare.toString();
             if (!alreadySeen.contains(termStr)) {
@@ -301,13 +321,31 @@ final class NumericFacets {
       }
       final Terms terms = searcher.getAtomicReader().terms(fieldName);
       if (terms != null) {
        final String prefixStr = TrieField.getMainValuePrefix(ft);
        final BytesRef prefix;
        if (prefixStr != null) {
          prefix = new BytesRef(prefixStr);
        } else {
          prefix = new BytesRef();
        }
         final TermsEnum termsEnum = terms.iterator(null);
        BytesRef term;
        switch (termsEnum.seekCeil(prefix)) {
          case FOUND:
          case NOT_FOUND:
            term = termsEnum.term();
            break;
          case END:
            term = null;
            break;
          default:
            throw new AssertionError();
        }
         final CharsRef spare = new CharsRef();
        BytesRef term = termsEnum.next();
        for (int i = 0; i < offset && term != null; ++i) {
        for (int i = 0; i < offset && term != null && StringHelper.startsWith(term, prefix); ++i) {
           term = termsEnum.next();
         }
        for ( ; term != null && (limit < 0 || result.size() < limit); term = termsEnum.next()) {
        for ( ; term != null && StringHelper.startsWith(term, prefix) && (limit < 0 || result.size() < limit); term = termsEnum.next()) {
           ft.indexedToReadable(term, spare);
           final String termStr = spare.toString();
           Integer count = counts.get(termStr);
diff --git a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
index f0f48787b14..49a6c67d4bb 100644
-- a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
++ b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
@@ -320,6 +320,10 @@ public class SimpleFacets {
     return collector.getGroupCount();
   }
 
  enum FacetMethod {
    ENUM, FC, FCS;
  }

   public NamedList<Integer> getTermCounts(String field) throws IOException {
     int offset = params.getFieldInt(field, FacetParams.FACET_OFFSET, 0);
     int limit = params.getFieldInt(field, FacetParams.FACET_LIMIT, 100);
@@ -342,57 +346,84 @@ public class SimpleFacets {
     FieldType ft = sf.getType();
 
     // determine what type of faceting method to use
    String method = params.getFieldParam(field, FacetParams.FACET_METHOD);
    boolean enumMethod = FacetParams.FACET_METHOD_enum.equals(method);
    final String methodStr = params.getFieldParam(field, FacetParams.FACET_METHOD);
    FacetMethod method = null;
    if (FacetParams.FACET_METHOD_enum.equals(methodStr)) {
      method = FacetMethod.ENUM;
    } else if (FacetParams.FACET_METHOD_fcs.equals(methodStr)) {
      method = FacetMethod.FCS;
    } else if (FacetParams.FACET_METHOD_fc.equals(methodStr)) {
      method = FacetMethod.FC;
    }
 
    // TODO: default to per-segment or not?
    boolean per_segment = FacetParams.FACET_METHOD_fcs.equals(method) // explicit
        || (ft.getNumericType() != null && sf.hasDocValues()); // numeric doc values are per-segment by default
    if (method == FacetMethod.ENUM && TrieField.getMainValuePrefix(ft) != null) {
      // enum can't deal with trie fields that index several terms per value
      method = sf.multiValued() ? FacetMethod.FC : FacetMethod.FCS;
    }
 
     if (method == null && ft instanceof BoolField) {
       // Always use filters for booleans... we know the number of values is very small.
      enumMethod = true;
      method = FacetMethod.ENUM;
    }

    final boolean multiToken = sf.multiValued() || ft.multiValuedFieldCache();
    
    if (method == null && ft.getNumericType() != null && !sf.multiValued()) {
      // the per-segment approach is optimal for numeric field types since there
      // are no global ords to merge and no need to create an expensive
      // top-level reader
      method = FacetMethod.FCS;
    }

    if (ft.getNumericType() != null && sf.hasDocValues()) {
      // only fcs is able to leverage the numeric field caches
      method = FacetMethod.FCS;
     }
    boolean multiToken = sf.multiValued() || ft.multiValuedFieldCache();
 
    if (TrieField.getMainValuePrefix(ft) != null) {
      // A TrieField with multiple parts indexed per value... currently only
      // UnInvertedField can handle this case, so force it's use.
      enumMethod = false;
      multiToken = true;
    if (method == null) {
      // TODO: default to per-segment or not?
      method = FacetMethod.FC;
    }

    if (method == FacetMethod.FCS && multiToken) {
      // only fc knows how to deal with multi-token fields
      method = FacetMethod.FC;
     }
 
     if (params.getFieldBool(field, GroupParams.GROUP_FACET, false)) {
       counts = getGroupedCounts(searcher, docs, field, multiToken, offset,limit, mincount, missing, sort, prefix);
     } else {
      // unless the enum method is explicitly specified, use a counting method.
      if (enumMethod) {
        counts = getFacetTermEnumCounts(searcher, docs, field, offset, limit, mincount,missing,sort,prefix);
      } else {
        if (multiToken) {
          UnInvertedField uif = UnInvertedField.getUnInvertedField(field, searcher);
          counts = uif.getCounts(searcher, docs, offset, limit, mincount,missing,sort,prefix);
        } else {
          // TODO: future logic could use filters instead of the fieldcache if
          // the number of terms in the field is small enough.
          if (per_segment) {
            if (ft.getNumericType() != null && !sf.multiValued()) {
              // force numeric faceting
              if (prefix != null && !prefix.isEmpty()) {
                throw new SolrException(ErrorCode.BAD_REQUEST, FacetParams.FACET_PREFIX + " is not supported on numeric types");
              }
              counts = NumericFacets.getCounts(searcher, docs, field, offset, limit, mincount, missing, sort);
            } else {
              PerSegmentSingleValuedFaceting ps = new PerSegmentSingleValuedFaceting(searcher, docs, field, offset,limit, mincount, missing, sort, prefix);
              Executor executor = threads == 0 ? directExecutor : facetExecutor;
              ps.setNumThreads(threads);
              counts = ps.getFacetCounts(executor);
      assert method != null;
      switch (method) {
        case ENUM:
          assert TrieField.getMainValuePrefix(ft) == null;
          counts = getFacetTermEnumCounts(searcher, docs, field, offset, limit, mincount,missing,sort,prefix);
          break;
        case FCS:
          assert !multiToken;
          if (ft.getNumericType() != null && !sf.multiValued()) {
            // force numeric faceting
            if (prefix != null && !prefix.isEmpty()) {
              throw new SolrException(ErrorCode.BAD_REQUEST, FacetParams.FACET_PREFIX + " is not supported on numeric types");
             }
            counts = NumericFacets.getCounts(searcher, docs, field, offset, limit, mincount, missing, sort);
          } else {
            PerSegmentSingleValuedFaceting ps = new PerSegmentSingleValuedFaceting(searcher, docs, field, offset,limit, mincount, missing, sort, prefix);
            Executor executor = threads == 0 ? directExecutor : facetExecutor;
            ps.setNumThreads(threads);
            counts = ps.getFacetCounts(executor);
          }
          break;
        case FC:
          if (multiToken || TrieField.getMainValuePrefix(ft) != null) {
            UnInvertedField uif = UnInvertedField.getUnInvertedField(field, searcher);
            counts = uif.getCounts(searcher, docs, offset, limit, mincount,missing,sort,prefix);
           } else {
             counts = getFieldCacheCounts(searcher, docs, field, offset,limit, mincount, missing, sort, prefix);
           }

        }
          break;
        default:
          throw new AssertionError();
       }
     }
 
diff --git a/solr/core/src/test/org/apache/solr/request/TestFaceting.java b/solr/core/src/test/org/apache/solr/request/TestFaceting.java
index 6c3f9b3f9d6..9cd0486b0aa 100755
-- a/solr/core/src/test/org/apache/solr/request/TestFaceting.java
++ b/solr/core/src/test/org/apache/solr/request/TestFaceting.java
@@ -17,6 +17,8 @@
 
 package org.apache.solr.request;
 
import java.util.ArrayList;
import java.util.List;
 import java.util.Locale;
 import java.util.Random;
 
@@ -25,6 +27,7 @@ import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.util.BytesRef;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.FacetParams;
 import org.junit.After;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -263,5 +266,32 @@ public class TestFaceting extends SolrTestCaseJ4 {
             );
   }
 
  @Test
  public void testTrieFields() {
    // make sure that terms are correctly filtered even for trie fields that index several
    // terms for a single value
    List<String> fields = new ArrayList<String>();
    fields.add("id");
    fields.add("7");
    final String[] suffixes = new String[] {"ti", "tis", "tf", "tfs", "tl", "tls", "td", "tds"};
    for (String suffix : suffixes) {
      fields.add("f_" + suffix);
      fields.add("42");
    }
    assertU(adoc(fields.toArray(new String[0])));
    assertU(commit());
    for (String suffix : suffixes) {
      for (String facetMethod : new String[] {FacetParams.FACET_METHOD_enum, FacetParams.FACET_METHOD_fc, FacetParams.FACET_METHOD_fcs}) {
        for (String facetSort : new String[] {FacetParams.FACET_SORT_COUNT, FacetParams.FACET_SORT_INDEX}) {
          for (String value : new String[] {"42", "43"}) { // match or not
            final String field = "f_" + suffix;
            assertQ("field=" + field + ",method=" + facetMethod + ",sort=" + facetSort,
                req("q", field + ":" + value, FacetParams.FACET, "true", FacetParams.FACET_FIELD, field, FacetParams.FACET_MINCOUNT, "0", FacetParams.FACET_SORT, facetSort, FacetParams.FACET_METHOD, facetMethod),
                "*[count(//lst[@name='" + field + "']/int)=1]"); // exactly 1 facet count
          }
        }
      }
    }
  }
 
 }
\ No newline at end of file
- 
2.19.1.windows.1

