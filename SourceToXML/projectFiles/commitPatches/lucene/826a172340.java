From 826a1723409bd70ab1f8831ebad0a98e55a9550c Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Mon, 17 Sep 2012 18:33:03 +0000
Subject: [PATCH] LUCENE-4401: don't call score() on NO_MORE_DOCS in
 DisjunctionSumScorer

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1386763 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                                  |  3 +++
 .../apache/lucene/search/ConstantScoreQuery.java    |  1 +
 .../apache/lucene/search/DisjunctionSumScorer.java  | 13 +++++++++----
 3 files changed, 13 insertions(+), 4 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 57fe36a6f8b..2f95b76cd29 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -167,6 +167,9 @@ Bug Fixes
 * LUCENE-3720: fix memory-consumption issues with BeiderMorseFilter.
   (Thomas Neidhart via Robert Muir)
 
* LUCENE-4401: Fix bug where DisjunctionSumScorer would sometimes call score()
  on a subscorer that had already returned NO_MORE_DOCS.  (Liu Chao, Robert Muir)

 Optimizations
 
 * LUCENE-4322: Decrease lucene-core JAR size. The core JAR size had increased a
diff --git a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
index 93ecefea8f3..9da1dddc3df 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
@@ -190,6 +190,7 @@ public class ConstantScoreQuery extends Query {
 
     @Override
     public float score() throws IOException {
      assert docIdSetIterator.docID() != NO_MORE_DOCS;
       return theScore;
     }
 
diff --git a/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java b/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java
index 148fd153d02..c3d32b1d483 100644
-- a/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java
@@ -68,6 +68,7 @@ class DisjunctionSumScorer extends DisjunctionScorer {
 
   @Override
   public int nextDoc() throws IOException {
    assert doc != NO_MORE_DOCS;
     while(true) {
       while (subScorers[0].docID() == doc) {
         if (subScorers[0].nextDoc() != NO_MORE_DOCS) {
@@ -91,10 +92,14 @@ class DisjunctionSumScorer extends DisjunctionScorer {
   private void afterNext() throws IOException {
     final Scorer sub = subScorers[0];
     doc = sub.docID();
    score = sub.score();
    nrMatchers = 1;
    countMatches(1);
    countMatches(2);
    if (doc == NO_MORE_DOCS) {
      nrMatchers = Integer.MAX_VALUE; // stop looping
    } else {
      score = sub.score();
      nrMatchers = 1;
      countMatches(1);
      countMatches(2);
    }
   }
   
   // TODO: this currently scores, but so did the previous impl
- 
2.19.1.windows.1

