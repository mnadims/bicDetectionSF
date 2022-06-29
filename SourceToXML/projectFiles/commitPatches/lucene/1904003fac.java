From 1904003fac4e9810737490d3b921294b13095fc3 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Fri, 30 Jul 2010 18:27:40 +0000
Subject: [PATCH] LUCENE-2580: fix AIOOBE in MultiPhraseQuery

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@980911 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/lucene/search/MultiPhraseQuery.java     | 10 +++++-----
 1 file changed, 5 insertions(+), 5 deletions(-)

diff --git a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
index b25c3b61b42..f3be46b70f2 100644
-- a/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
++ b/lucene/src/java/org/apache/lucene/search/MultiPhraseQuery.java
@@ -175,8 +175,8 @@ public class MultiPhraseQuery extends Query {
       
       PhraseQuery.PostingsAndFreq[] postingsFreqs = new PhraseQuery.PostingsAndFreq[termArrays.size()];
 
      for (int i=0; i<postingsFreqs.length; i++) {
        Term[] terms = termArrays.get(i);
      for (int pos=0; pos<postingsFreqs.length; pos++) {
        Term[] terms = termArrays.get(pos);
 
         final DocsAndPositionsEnum postingsEnum;
         int docFreq;
@@ -187,8 +187,8 @@ public class MultiPhraseQuery extends Query {
           // coarse -- this overcounts since a given doc can
           // have more than one terms:
           docFreq = 0;
          for(int j=0;j<terms.length;j++) {
            docFreq += reader.docFreq(terms[i]);
          for(int termIdx=0;termIdx<terms.length;termIdx++) {
            docFreq += reader.docFreq(terms[termIdx]);
           }
         } else {
           final BytesRef text = new BytesRef(terms[0].text());
@@ -209,7 +209,7 @@ public class MultiPhraseQuery extends Query {
           docFreq = reader.docFreq(terms[0].field(), text);
         }
 
        postingsFreqs[i] = new PhraseQuery.PostingsAndFreq(postingsEnum, docFreq, positions.get(i).intValue());
        postingsFreqs[pos] = new PhraseQuery.PostingsAndFreq(postingsEnum, docFreq, positions.get(pos).intValue());
       }
 
       // sort by increasing docFreq order
- 
2.19.1.windows.1

