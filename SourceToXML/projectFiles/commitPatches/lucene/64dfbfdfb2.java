From 64dfbfdfb21972444aa05c9da29d19d0351fe6f1 Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Fri, 3 May 2013 14:11:14 +0000
Subject: [PATCH] LUCENE-4946: Re-add the random-access checks that have been
 lost during refactoring.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1478801 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/src/java/org/apache/lucene/util/CollectionUtil.java  | 4 ++++
 1 file changed, 4 insertions(+)

diff --git a/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java b/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java
index c8aef094912..65d4b948868 100644
-- a/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java
++ b/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java
@@ -42,6 +42,8 @@ public final class CollectionUtil {
 
     ListIntroSorter(List<T> list, Comparator<? super T> comp) {
       super();
      if (!(list instanceof RandomAccess))
        throw new IllegalArgumentException("CollectionUtil can only sort random access lists in-place.");
       this.list = list;
       this.comp = comp;
     }
@@ -77,6 +79,8 @@ public final class CollectionUtil {
     @SuppressWarnings("unchecked")
     ListTimSorter(List<T> list, Comparator<? super T> comp, int maxTempSlots) {
       super(maxTempSlots);
      if (!(list instanceof RandomAccess))
        throw new IllegalArgumentException("CollectionUtil can only sort random access lists in-place.");
       this.list = list;
       this.comp = comp;
       if (maxTempSlots > 0) {
- 
2.19.1.windows.1

