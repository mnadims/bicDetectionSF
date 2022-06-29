From b0db06bad568b7eedf528379a2fe5ac935992d56 Mon Sep 17 00:00:00 2001
From: Chris Hostetter <hossman@apache.org>
Date: Fri, 20 Jan 2017 13:27:09 -0700
Subject: [PATCH] SOLR-10013: Fix DV range query bug introduced by LUCENE-7643
 by disabling and optimization (LUCENE-7649 to track re-enabling or removing
 completely)

--
 .../SortedNumericDocValuesRangeQuery.java     |  5 +--
 .../SortedSetDocValuesRangeQuery.java         |  5 +--
 .../lucene/search/TestDocValuesQueries.java   | 33 +++++++++++++++++++
 3 files changed, 39 insertions(+), 4 deletions(-)

diff --git a/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java b/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java
index 18805b287c0..d5f75a76d74 100644
-- a/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java
++ b/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java
@@ -19,7 +19,6 @@ package org.apache.lucene.document;
 import java.io.IOException;
 import java.util.Objects;
 
import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.LeafReader;
 import org.apache.lucene.index.LeafReaderContext;
@@ -100,9 +99,11 @@ abstract class SortedNumericDocValuesRangeQuery extends Query {
         if (values == null) {
           return null;
         }
        final NumericDocValues singleton = DocValues.unwrapSingleton(values);
        final NumericDocValues singleton = null; // TODO: LUCENE-7649, re-consider optimization that broke SOLR-10013
        // final NumericDocValues singleton = DocValues.unwrapSingleton(values);
         final TwoPhaseIterator iterator;
         if (singleton != null) {
          assert false : "imposible code -- or: someone re-enabled singleton optinization w/o reading the whole method";
           iterator = new TwoPhaseIterator(singleton) {
             @Override
             public boolean matches() throws IOException {
diff --git a/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java b/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java
index 30af45f6a64..3bc1b9cbcb4 100644
-- a/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java
++ b/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java
@@ -19,7 +19,6 @@ package org.apache.lucene.document;
 import java.io.IOException;
 import java.util.Objects;
 
import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.LeafReader;
 import org.apache.lucene.index.LeafReaderContext;
@@ -144,9 +143,11 @@ abstract class SortedSetDocValuesRangeQuery extends Query {
           return null;
         }
 
        final SortedDocValues singleton = DocValues.unwrapSingleton(values);
        final SortedDocValues singleton = null; // TODO: LUCENE-7649, re-consider optimization that broke SOLR-10013
        // final SortedDocValues singleton = DocValues.unwrapSingleton(values);
         final TwoPhaseIterator iterator;
         if (singleton != null) {
          assert false : "imposible code -- or: someone re-enabled singleton optinization w/o reading the whole method";
           iterator = new TwoPhaseIterator(singleton) {
             @Override
             public boolean matches() throws IOException {
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java b/lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java
index 501538f426f..6cb04604730 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java
@@ -30,6 +30,7 @@ import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.NumericUtils;
 import org.apache.lucene.util.TestUtil;
 
 public class TestDocValuesQueries extends LuceneTestCase {
@@ -235,4 +236,36 @@ public class TestDocValuesQueries extends LuceneTestCase {
     reader.close();
     dir.close();
   }

  public void testSortedNumericNPE() throws IOException {
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
    double[] nums = {-1.7147449030215377E-208, -1.6887024655302576E-11, 1.534911516604164E113, 0.0,
        2.6947996404505155E-166, -2.649722021970773E306, 6.138239235731689E-198, 2.3967090122610808E111};
    for (int i = 0; i < nums.length; ++i) {
      Document doc = new Document();
      doc.add(new SortedNumericDocValuesField("dv", NumericUtils.doubleToSortableLong(nums[i])));
      iw.addDocument(doc);
    }
    iw.commit();
    final IndexReader reader = iw.getReader();
    final IndexSearcher searcher = newSearcher(reader);
    iw.close();

    final long lo = NumericUtils.doubleToSortableLong(8.701032080293731E-226);
    final long hi = NumericUtils.doubleToSortableLong(2.0801416404385346E-41);
    
    Query query = SortedNumericDocValuesField.newRangeQuery("dv", lo, hi);
    // TODO: assert expected matches
    searcher.search(query, searcher.reader.maxDoc(), Sort.INDEXORDER);

    // swap order, should still work
    query = SortedNumericDocValuesField.newRangeQuery("dv", hi, lo);
    // TODO: assert expected matches
    searcher.search(query, searcher.reader.maxDoc(), Sort.INDEXORDER);
    
    reader.close();
    dir.close();
  }
   
 }
- 
2.19.1.windows.1

