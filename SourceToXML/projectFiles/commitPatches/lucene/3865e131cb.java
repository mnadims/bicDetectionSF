From 3865e131cb380b28bfe0e5ab796f551f6dddf03a Mon Sep 17 00:00:00 2001
From: Erick Erickson <erick@apache.org>
Date: Wed, 21 Aug 2013 20:31:39 +0000
Subject: [PATCH] SOLR-5057 - queryResultCache should match out-of-order fq
 clauses

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1516299 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  2 ++
 .../apache/solr/search/QueryResultKey.java    | 35 ++++++++++++++++---
 .../apache/solr/core/QueryResultKeyTest.java  | 20 +++++++++++
 3 files changed, 53 insertions(+), 4 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index b0f62badcec..c9154b27283 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -155,6 +155,8 @@ Optimizations
 * SOLR-5134: Have HdfsIndexOutput extend BufferedIndexOutput. 
   (Mark Miller, Uwe Schindler)
 
 * SOLR-5057: QueryResultCache should not related with the order of fq's list (Feihong Huang via Erick Erickson)

 Other Changes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/search/QueryResultKey.java b/solr/core/src/java/org/apache/solr/search/QueryResultKey.java
index b439fe3ca0a..b09b6062f35 100644
-- a/solr/core/src/java/org/apache/solr/search/QueryResultKey.java
++ b/solr/core/src/java/org/apache/solr/search/QueryResultKey.java
@@ -90,9 +90,36 @@ public final class QueryResultKey {
   }
 
 
  private static boolean isEqual(Object o1, Object o2) {
    if (o1==o2) return true;  // takes care of identity and null cases
    if (o1==null || o2==null) return false;
    return o1.equals(o2);
  // Do fast version, expecting that filters are ordered and only
  // fall back to unordered compare on the first non-equal elements.
  // This will only be called if the hash code of the entire key already
  // matched, so the slower unorderedCompare should pretty much never
  // be called if filter lists are generally ordered.
  private static boolean isEqual(List<Query> fqList1, List<Query> fqList2) {
    if (fqList1 == fqList2) return true;  // takes care of identity and null cases
    if (fqList1 == null || fqList2 == null) return false;
    int sz = fqList1.size();
    if (sz != fqList2.size()) return false;
    for (int i = 0; i < sz; i++) {
      if (!fqList1.get(i).equals(fqList2.get(i))) {
        return unorderedCompare(fqList1, fqList2, i);
      }
    }
    return true;
   }

  private static boolean unorderedCompare(List<Query> fqList1, List<Query> fqList2, int start) {
    int sz = fqList1.size();
    outer:
    for (int i = start; i < sz; i++) {
      Query q1 = fqList1.get(i);
      for (int j = start; j < sz; j++) {
        if (q1.equals(fqList2.get(j)))
          continue outer;
      }
      return false;
    }
    return true;
  }

 }
diff --git a/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java b/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java
index 8ad39d526ae..1c07d85a0a0 100644
-- a/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java
++ b/solr/core/src/test/org/apache/solr/core/QueryResultKeyTest.java
@@ -56,4 +56,24 @@ public class QueryResultKeyTest extends SolrTestCaseJ4 {
     assertEquals(qrk1.hashCode(), qrk2.hashCode());
   }
 
  @Test
  public void testQueryResultKeySortedFilters() {
    Query fq1 = new TermQuery(new Term("test1", "field1"));
    Query fq2 = new TermQuery(new Term("test2", "field2"));

    Query query = new TermQuery(new Term("test3", "field3"));
    List<Query> filters = new ArrayList<Query>();
    filters.add(fq1);
    filters.add(fq2);

    QueryResultKey key = new QueryResultKey(query, filters, null, 0);

    List<Query> newFilters = new ArrayList<Query>();
    newFilters.add(fq2);
    newFilters.add(fq1);
    QueryResultKey newKey = new QueryResultKey(query, newFilters, null, 0);

    assertEquals(key, newKey);
  }

 }
- 
2.19.1.windows.1

