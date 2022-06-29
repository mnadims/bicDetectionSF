From c1ec7aa8df668cb02af45d1363d8133e65536b37 Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Mon, 13 May 2013 21:26:08 +0000
Subject: [PATCH] LUCENE-4946: Sorter.rotate is a no-op when one of the two
 adjacent slices to rotate is empty.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1482111 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/src/java/org/apache/lucene/util/Sorter.java | 12 ++++++++++--
 .../src/java/org/apache/lucene/util/TimSorter.java   |  6 +++---
 2 files changed, 13 insertions(+), 5 deletions(-)

diff --git a/lucene/core/src/java/org/apache/lucene/util/Sorter.java b/lucene/core/src/java/org/apache/lucene/util/Sorter.java
index 12c53a9b145..6ae43c8c286 100644
-- a/lucene/core/src/java/org/apache/lucene/util/Sorter.java
++ b/lucene/core/src/java/org/apache/lucene/util/Sorter.java
@@ -72,7 +72,7 @@ public abstract class Sorter {
       first_cut = upper(from, mid, second_cut);
       len11 = first_cut - from;
     }
    rotate( first_cut, mid, second_cut);
    rotate(first_cut, mid, second_cut);
     final int new_mid = first_cut + len22;
     mergeInPlace(from, first_cut, new_mid);
     mergeInPlace(new_mid, second_cut, to);
@@ -142,7 +142,15 @@ public abstract class Sorter {
     }
   }
 
  void rotate(int lo, int mid, int hi) {
  final void rotate(int lo, int mid, int hi) {
    assert lo <= mid && mid <= hi;
    if (lo == mid || mid == hi) {
      return;
    }
    doRotate(lo, mid, hi);
  }

  void doRotate(int lo, int mid, int hi) {
     if (mid - lo == hi - mid) {
       // happens rarely but saves n/2 swaps
       while (mid < hi) {
diff --git a/lucene/core/src/java/org/apache/lucene/util/TimSorter.java b/lucene/core/src/java/org/apache/lucene/util/TimSorter.java
index 57e2f8d8c18..d8b40bea77e 100644
-- a/lucene/core/src/java/org/apache/lucene/util/TimSorter.java
++ b/lucene/core/src/java/org/apache/lucene/util/TimSorter.java
@@ -205,9 +205,9 @@ public abstract class TimSorter extends Sorter {
   }
 
   @Override
  void rotate(int lo, int mid, int hi) {
    int len1 = mid - lo;
    int len2 = hi - mid;
  void doRotate(int lo, int mid, int hi) {
    final int len1 = mid - lo;
    final int len2 = hi - mid;
     if (len1 == len2) {
       while (mid < hi) {
         swap(lo++, mid++);
- 
2.19.1.windows.1

