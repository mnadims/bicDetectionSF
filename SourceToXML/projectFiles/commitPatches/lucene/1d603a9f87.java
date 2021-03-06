From 1d603a9f87e95e380aca776f3488b07ffa9bfeff Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Wed, 31 Jul 2013 07:51:41 +0000
Subject: [PATCH] LUCENE-5140: Fixed performance regression of span queries
 caused by LUCENE-4946.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1508757 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                              |  3 +++
 .../lucene/search/spans/NearSpansOrdered.java   | 17 ++++++++++++-----
 2 files changed, 15 insertions(+), 5 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 612dbfed9d4..5f43a7ba41c 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -153,6 +153,9 @@ Optimizations
 * LUCENE-5145: All Appending*Buffer now support bulk get.
   (Boaz Leskes via Adrien Grand)
 
* LUCENE-5140: Fixed a performance regression of span queries caused by
  LUCENE-4946. (Alan Woodward, Adrien Grand)

 Documentation
 
 * LUCENE-4894: remove facet userguide as it was outdated. Partially absorbed into
diff --git a/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java b/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java
index 1ee44a566af..95451f4586b 100644
-- a/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java
++ b/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java
@@ -22,6 +22,7 @@ import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermContext;
 import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.Bits;
import org.apache.lucene.util.InPlaceMergeSorter;
 
 import java.io.IOException;
 import java.util.ArrayList;
@@ -72,13 +73,19 @@ public class NearSpansOrdered extends Spans {
   private List<byte[]> matchPayload;
 
   private final Spans[] subSpansByDoc;
  private final Comparator<Spans> spanDocComparator = new Comparator<Spans>() {
  // Even though the array is probably almost sorted, InPlaceMergeSorter will likely
  // perform better since it has a lower overhead than TimSorter for small arrays
  private final InPlaceMergeSorter sorter = new InPlaceMergeSorter() {
     @Override
    public int compare(Spans o1, Spans o2) {
      return o1.doc() - o2.doc();
    protected void swap(int i, int j) {
      ArrayUtil.swap(subSpansByDoc, i, j);
    }
    @Override
    protected int compare(int i, int j) {
      return subSpansByDoc[i].doc() - subSpansByDoc[j].doc();
     }
   };
  

   private SpanNearQuery query;
   private boolean collectPayloads = true;
   
@@ -204,7 +211,7 @@ public class NearSpansOrdered extends Spans {
 
   /** Advance the subSpans to the same document */
   private boolean toSameDoc() throws IOException {
    ArrayUtil.timSort(subSpansByDoc, spanDocComparator);
    sorter.sort(0, subSpansByDoc.length);
     int firstIndex = 0;
     int maxDoc = subSpansByDoc[subSpansByDoc.length - 1].doc();
     while (subSpansByDoc[firstIndex].doc() != maxDoc) {
- 
2.19.1.windows.1

