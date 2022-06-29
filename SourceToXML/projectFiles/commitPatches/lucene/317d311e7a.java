From 317d311e7a5a5d02a613b5fc593d6e299c237a8e Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Wed, 12 May 2010 13:45:44 +0000
Subject: [PATCH] LUCENE-2410: add a PhraseQuery->TermQuery rewrite when there
 is only one term

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@943493 13f79535-47bb-0310-9956-ffa450edef68
--
 .../src/java/org/apache/lucene/search/PhraseQuery.java | 10 ++++++++++
 .../test/org/apache/lucene/search/TestPhraseQuery.java |  7 +++++++
 2 files changed, 17 insertions(+)

diff --git a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
index 6d7229760bd..54c1d258144 100644
-- a/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/PhraseQuery.java
@@ -110,6 +110,16 @@ public class PhraseQuery extends Query {
       return result;
   }
 
  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (terms.size() == 1) {
      TermQuery tq = new TermQuery(terms.get(0));
      tq.setBoost(getBoost());
      return tq;
    } else
      return super.rewrite(reader);
  }

   private class PhraseWeight extends Weight {
     private final Similarity similarity;
     private float value;
diff --git a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
index fd5419cd99b..b3314a96cf6 100644
-- a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
@@ -565,4 +565,11 @@ public class TestPhraseQuery extends LuceneTestCase {
     q2.toString();
   }
   
  /* test that a single term is rewritten to a term query */
  public void testRewrite() throws IOException {
    PhraseQuery pq = new PhraseQuery();
    pq.add(new Term("foo", "bar"));
    Query rewritten = pq.rewrite(searcher.getIndexReader());
    assertTrue(rewritten instanceof TermQuery);
  }
 }
- 
2.19.1.windows.1

