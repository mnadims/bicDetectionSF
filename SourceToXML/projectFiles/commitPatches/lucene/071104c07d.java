From 071104c07d2db5b10b014754d0b7c585463d5e60 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Sat, 8 Jan 2011 01:37:07 +0000
Subject: [PATCH] LUCENE-2831: attempt to use the correct reader context rather
 than doing getTopReaderContext on a leaf

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1056588 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/solr/search/function/BoostedQuery.java    | 12 ++++++------
 1 file changed, 6 insertions(+), 6 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
index 9530484fe99..963a776d3a0 100755
-- a/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
++ b/solr/src/java/org/apache/solr/search/function/BoostedQuery.java
@@ -61,13 +61,13 @@ public class BoostedQuery extends Query {
   private class BoostedWeight extends Weight {
     IndexSearcher searcher;
     Weight qWeight;
    Map context;
    Map fcontext;
 
     public BoostedWeight(IndexSearcher searcher) throws IOException {
       this.searcher = searcher;
       this.qWeight = q.weight(searcher);
      this.context = boostVal.newContext(searcher);
      boostVal.createWeight(context,searcher);
      this.fcontext = boostVal.newContext(searcher);
      boostVal.createWeight(fcontext,searcher);
     }
 
     public Query getQuery() {
@@ -106,7 +106,7 @@ public class BoostedQuery extends Query {
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
       }
      DocValues vals = boostVal.getValues(context, readerContext.reader);
      DocValues vals = boostVal.getValues(fcontext, readerContext.reader);
       float sc = subQueryExpl.getValue() * vals.floatVal(doc);
       Explanation res = new ComplexExplanation(
         true, sc, BoostedQuery.this.toString() + ", product of:");
@@ -133,7 +133,7 @@ public class BoostedQuery extends Query {
       this.scorer = scorer;
       this.reader = reader;
       this.searcher = searcher; // for explain
      this.vals = vs.getValues(weight.context, reader);
      this.vals = vs.getValues(weight.fcontext, reader);
     }
 
     @Override
@@ -162,7 +162,7 @@ public class BoostedQuery extends Query {
     }
 
     public Explanation explain(int doc) throws IOException {
      Explanation subQueryExpl = weight.qWeight.explain(reader.getTopReaderContext() ,doc);
      Explanation subQueryExpl = weight.qWeight.explain(ValueSource.readerToContext(weight.fcontext,reader) ,doc);
       if (!subQueryExpl.isMatch()) {
         return subQueryExpl;
       }
- 
2.19.1.windows.1

