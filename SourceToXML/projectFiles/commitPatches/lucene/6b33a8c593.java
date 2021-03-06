From 6b33a8c5936c44b14517503d9c61d1b4648fc131 Mon Sep 17 00:00:00 2001
From: "Chris M. Hostetter" <hossman@apache.org>
Date: Thu, 9 Jan 2014 23:55:46 +0000
Subject: [PATCH] SOLR-5618: Fix false cache hits in queryResultCache when
 hashCodes are equal and duplicate filter queries exist in one of the requests

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1556988 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   3 +
 .../apache/solr/search/QueryResultKey.java    |  62 +++++--
 .../apache/solr/core/QueryResultKeyTest.java  | 161 +++++++++++++++---
 .../org/apache/solr/search/TestFiltering.java |  44 ++++-
 4 files changed, 234 insertions(+), 36 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index b23c103de63..1e8f35b8d11 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -353,6 +353,9 @@ Bug Fixes
 * SOLR-5543: Core swaps resulted in duplicate core entries in solr.xml when 
   using solr.xml persistence. (Bill Bell, Alan Woodward)
 
* SOLR-5618: Fix false cache hits in queryResultCache when hashCodes are equal 
  and duplicate filter queries exist in one of the requests (hossman)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/search/QueryResultKey.java b/solr/core/src/java/org/apache/solr/search/QueryResultKey.java
index b09b6062f35..e0ac6d913a6 100644
-- a/solr/core/src/java/org/apache/solr/search/QueryResultKey.java
++ b/solr/core/src/java/org/apache/solr/search/QueryResultKey.java
@@ -21,6 +21,7 @@ import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
 import java.util.List;
import java.util.ArrayList;
 
 /** A hash key encapsulating a query, a list of filters, and a sort
  *
@@ -47,6 +48,8 @@ public final class QueryResultKey {
 
     if (filters != null) {
       for (Query filt : filters)
        // NOTE: simple summation used here so keys with the same filters but in
        // different orders get the same hashCode
         h += filt.hashCode();
     }
 
@@ -78,7 +81,7 @@ public final class QueryResultKey {
     // first.
     if (this.sfields.length != other.sfields.length) return false;
     if (!this.query.equals(other.query)) return false;
    if (!isEqual(this.filters, other.filters)) return false;
    if (!unorderedCompare(this.filters, other.filters)) return false;
 
     for (int i=0; i<sfields.length; i++) {
       SortField sf1 = this.sfields[i];
@@ -89,17 +92,27 @@ public final class QueryResultKey {
     return true;
   }
 

  // Do fast version, expecting that filters are ordered and only
  // fall back to unordered compare on the first non-equal elements.
  // This will only be called if the hash code of the entire key already
  // matched, so the slower unorderedCompare should pretty much never
  // be called if filter lists are generally ordered.
  private static boolean isEqual(List<Query> fqList1, List<Query> fqList2) {
  /** 
   * compares the two lists of queries in an unordered manner such that this method 
   * returns true if the 2 lists are the same size, and contain the same elements.
   *
   * This method should only be used if the lists come from QueryResultKeys which have 
   * already been found to have equal hashCodes, since the unordered comparison aspects 
   * of the logic are not cheap.
   * 
   * @return true if the lists of equivilent other then the ordering
   */
  private static boolean unorderedCompare(List<Query> fqList1, List<Query> fqList2) {
    // Do fast version first, expecting that filters are usually in the same order
    //
    // Fall back to unordered compare logic on the first non-equal elements.
    // The slower unorderedCompare should pretty much never be called if filter 
    // lists are generally ordered consistently
     if (fqList1 == fqList2) return true;  // takes care of identity and null cases
     if (fqList1 == null || fqList2 == null) return false;
     int sz = fqList1.size();
     if (sz != fqList2.size()) return false;

     for (int i = 0; i < sz; i++) {
       if (!fqList1.get(i).equals(fqList2.get(i))) {
         return unorderedCompare(fqList1, fqList2, i);
@@ -108,18 +121,37 @@ public final class QueryResultKey {
     return true;
   }
 

  /** 
   * Does an unordered comparison of the elements of two lists of queries starting at 
   * the specified start index.
   * 
   * This method should only be called on lists which are the same size, and where 
   * all items with an index less then the specified start index are the same.
   *
   * @return true if the list items after start are equivilent other then the ordering
   */
   private static boolean unorderedCompare(List<Query> fqList1, List<Query> fqList2, int start) {
    int sz = fqList1.size();
    outer:
    assert null != fqList1;
    assert null != fqList2;

    final int sz = fqList1.size();
    assert fqList2.size() == sz;

    // SOLR-5618: if we had a garuntee that the lists never contained any duplicates,
    // this logic could be a lot simplier 
    //
    // (And of course: if the SolrIndexSearcher / QueryCommmand was ever changed to
    // sort the filter query list, then this whole method could be eliminated).

    final ArrayList<Query> set2 = new ArrayList<Query>(fqList2.subList(start, sz));
     for (int i = start; i < sz; i++) {
       Query q1 = fqList1.get(i);
      for (int j = start; j < sz; j++) {
        if (q1.equals(fqList2.get(j)))
          continue outer;
      if ( ! set2.remove(q1) ) {
        return false;
       }
      return false;
     }
    return true;
    return set2.isEmpty();
   }
 
 }
diff --git a/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java b/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java
index 1c07d85a0a0..1ffe0deb0d0 100644
-- a/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java
++ b/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java
@@ -17,6 +17,7 @@
 
 package org.apache.solr.core;
 
import java.util.Arrays;
 import java.util.ArrayList;
 import java.util.List;
 
@@ -27,53 +28,175 @@ import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util._TestUtil;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.search.QueryResultKey;
 import org.junit.Test;
 
 public class QueryResultKeyTest extends SolrTestCaseJ4 {
 
  @Test
  public void testFiltersHashCode() {
  public void testFiltersOutOfOrder1() {
     // the hashcode should be the same even when the list
     // of filters is in a different order
     
     Sort sort = new Sort(new SortField("test", SortField.Type.INT));
    List<Query> filters = new ArrayList<Query>();
    filters.add(new TermQuery(new Term("test", "field")));
    filters.add(new TermQuery(new Term("test2", "field2")));
    
     BooleanQuery query = new BooleanQuery();
     query.add(new TermQuery(new Term("test", "field")), Occur.MUST);
     
    List<Query> filters = Arrays.<Query>asList(new TermQuery(new Term("test", "field")),
                                               new TermQuery(new Term("test2", "field2")));
     QueryResultKey qrk1 = new QueryResultKey(query , filters, sort, 1);
     
    List<Query> filters2 = new ArrayList<Query>();
    filters2.add(new TermQuery(new Term("test2", "field2")));
    filters2.add(new TermQuery(new Term("test", "field")));
    List<Query> filters2 = Arrays.<Query>asList(new TermQuery(new Term("test2", "field2")),
                                                new TermQuery(new Term("test", "field")));
     QueryResultKey qrk2 = new QueryResultKey(query , filters2, sort, 1);
    
    assertEquals(qrk1.hashCode(), qrk2.hashCode());
    assertKeyEquals(qrk1, qrk2);
   }
 
   @Test
  public void testQueryResultKeySortedFilters() {
  public void testFiltersOutOfOrder2() {
     Query fq1 = new TermQuery(new Term("test1", "field1"));
     Query fq2 = new TermQuery(new Term("test2", "field2"));
 
     Query query = new TermQuery(new Term("test3", "field3"));
    List<Query> filters = new ArrayList<Query>();
    filters.add(fq1);
    filters.add(fq2);
    List<Query> filters = Arrays.asList(fq1, fq2);
 
     QueryResultKey key = new QueryResultKey(query, filters, null, 0);
 
    List<Query> newFilters = new ArrayList<Query>();
    newFilters.add(fq2);
    newFilters.add(fq1);
    List<Query> newFilters = Arrays.asList(fq2, fq1);
     QueryResultKey newKey = new QueryResultKey(query, newFilters, null, 0);
 
    assertEquals(key, newKey);
    assertKeyEquals(key, newKey);
  }

  public void testQueryResultKeyUnSortedFiltersWithDups() {
    Query query = new TermQuery(new Term("main", "val"));

    // we need Query clauses that have identical hashCodes 
    // but are not equal unless the term is equals
    Query fq_aa = new FlatHashTermQuery("fq_a");
    Query fq_ab = new FlatHashTermQuery("fq_a");
    Query fq_ac = new FlatHashTermQuery("fq_a");
    Query fq_zz = new FlatHashTermQuery("fq_z");

    assertEquals(fq_aa.hashCode(), fq_ab.hashCode());
    assertEquals(fq_aa.hashCode(), fq_ac.hashCode());
    assertEquals(fq_aa.hashCode(), fq_zz.hashCode());

    assertEquals(fq_aa, fq_ab);
    assertEquals(fq_aa, fq_ac);
    assertEquals(fq_ab, fq_aa);
    assertEquals(fq_ab, fq_ac);
    assertEquals(fq_ac, fq_aa);
    assertEquals(fq_ac, fq_ab);

    assertTrue( ! fq_aa.equals(fq_zz) );
    assertTrue( ! fq_ab.equals(fq_zz) );
    assertTrue( ! fq_ac.equals(fq_zz) );
    assertTrue( ! fq_zz.equals(fq_aa) );
    assertTrue( ! fq_zz.equals(fq_ab) );
    assertTrue( ! fq_zz.equals(fq_ac) );

    List<Query> filters1 = Arrays.asList(fq_aa, fq_ab);
    List<Query> filters2 = Arrays.asList(fq_zz, fq_ac);

    QueryResultKey key1 = new QueryResultKey(query, filters1, null, 0);
    QueryResultKey key2 = new QueryResultKey(query, filters2, null, 0);
    
    assertEquals(key1.hashCode(), key2.hashCode());

    assertKeyNotEquals(key1, key2);
  }

  public void testRandomQueryKeyEquality() {


    final int minIters = atLeast(100 * 1000);
    final Query base = new FlatHashTermQuery("base");
    
    // ensure we cover both code paths at least once
    boolean didEquals = false;
    boolean didNotEquals = false;
    int iter = 1;
    while (iter <= minIters || (! didEquals ) || (! didNotEquals ) ) {
      iter++;
      int[] numsA = smallArrayOfRandomNumbers();
      int[] numsB = smallArrayOfRandomNumbers();
      QueryResultKey aa = new QueryResultKey(base, buildFiltersFromNumbers(numsA), null, 0);
      QueryResultKey bb = new QueryResultKey(base, buildFiltersFromNumbers(numsB), null, 0);
      // now that we have our keys, sort the numbers so we know what to expect
      Arrays.sort(numsA);
      Arrays.sort(numsB);
      if (Arrays.equals(numsA, numsB)) {
        didEquals = true;
        assertKeyEquals(aa, bb);
      } else {
        didNotEquals = true;
        assertKeyNotEquals(aa, bb);
      }
    }
    assert minIters <= iter;
  }

  /**
   * does bi-directional equality check as well as verifying hashCode
   */
  public void assertKeyEquals(QueryResultKey key1, QueryResultKey key2) {
    assertNotNull(key1);
    assertNotNull(key2);
    assertEquals(key1.hashCode(), key2.hashCode());
    assertEquals(key1, key2);
    assertEquals(key2, key1);
  }

  /**
   * does bi-directional check that the keys are <em>not</em> equals
   */
  public void assertKeyNotEquals(QueryResultKey key1, QueryResultKey key2) {
    assertTrue( ! key1.equals(key2) );
    assertTrue( ! key2.equals(key1) );
  }

  /**
   * returns a "small" list of "small" random numbers.  The idea behind this method is 
   * that multiple calls have a decent change of returning two arrays which are the 
   * same size and contain the same numbers but in a differnet order.
   *
   * the array is garunteed to always have at least 1 element
   */
  private int[] smallArrayOfRandomNumbers() {
    int size = _TestUtil.nextInt(random(), 1, 5);
    int[] result = new int[size];
    for (int i=0; i < size; i++) {
      result[i] = _TestUtil.nextInt(random(), 1, 5);
    }
    return result;
   }
 
  /**
   * Creates an array of Filter queries using {@link FlatHashTermQuery} based on the 
   * specified ints
   */
  private List<Query> buildFiltersFromNumbers(int[] values) {
    ArrayList<Query> filters = new ArrayList<Query>(values.length);
    for (int val : values) {
      filters.add(new FlatHashTermQuery(String.valueOf(val)));
    }
    return filters;
  }

  /**
   * Quick and dirty subclass of TermQuery that uses fixed field name and a constant 
   * value hashCode, regardless of the Term value.
   */
  private static class FlatHashTermQuery extends TermQuery {
    public FlatHashTermQuery(String val) {
      super(new Term("some_field", val));
    }

    @Override
    public int hashCode() {
      return 42;
    }
  }
 }
diff --git a/solr/core/src/test/org/apache/solr/search/TestFiltering.java b/solr/core/src/test/org/apache/solr/search/TestFiltering.java
index b0a4bd8b9a0..75c0a3b9c81 100644
-- a/solr/core/src/test/org/apache/solr/search/TestFiltering.java
++ b/solr/core/src/test/org/apache/solr/search/TestFiltering.java
@@ -20,6 +20,7 @@ package org.apache.solr.search;
 
 import org.apache.lucene.util.OpenBitSet;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.request.SolrQueryRequest;
 import org.junit.BeforeClass;
@@ -259,6 +260,8 @@ public class TestFiltering extends SolrTestCaseJ4 {
         }
       }
       assertU(commit());
      // sanity check
      assertJQ(req("q", "*:*"), "/response/numFound==" + model.indexSize);
 
       int totalMatches=0;
       int nonZeros=0;
@@ -322,11 +325,10 @@ public class TestFiltering extends SolrTestCaseJ4 {
         } catch (Exception e) {
           // show the indexIter and queryIter for easier debugging
           SolrException.log(log, e);
          String s= "FAILURE: iiter=" + iiter + " qiter=" + qiter + " request="+params;
          String s= "FAILURE: indexSize=" + model.indexSize + " iiter=" + iiter + " qiter=" + qiter + " request="+params;
           log.error(s);
           fail(s);
         }

       }
 
       // After making substantial changes to this test, make sure that we still get a
@@ -336,4 +338,42 @@ public class TestFiltering extends SolrTestCaseJ4 {
     }
   }
 
  public void testHossssSanity() throws Exception {
    
    SolrParams match_0 
      = params("q",  "{!frange v=val_i l=0 u=1}",
               "fq", "{!frange v=val_i l=1 u=1}",
               "fq", "{!frange v=val_i l=0 u=1}",
               "fq", "-_query_:\"{!frange v=val_i l=1 u=1}\"",
               "fq", "-_query_:\"{!frange v=val_i l=0 u=1}\"");
    
    SolrParams match_1
      = params("q",  "{!frange v=val_i l=0 u=1}",
               "fq", "{!frange v=val_i l=0 u=1}",
               "fq", "{!frange v=val_i l=0 u=1}",
               "fq", "-_query_:\"{!frange v=val_i l=1 u=1}\"",
               "fq", "-_query_:\"{!frange v=val_i l=1 u=1}\"");
    
    final int numDocs = 10;

    for (int i = 0; i < numDocs; i++) {
      String val = Integer.toString(i);
      assertU(adoc("id",val,f,val));
    }
    assertU(commit());

    // sanity check
    assertJQ(req("q", "*:*"), "/response/numFound==" + numDocs);

    // 1 then 0
    assertJQ(req(match_1), "/response/numFound==1");
    assertJQ(req(match_0), "/response/numFound==0");

    // clear caches
    assertU(commit());

    // 0 then 1
    assertJQ(req(match_0), "/response/numFound==0");
    assertJQ(req(match_1), "/response/numFound==1");
  }
 }
- 
2.19.1.windows.1

