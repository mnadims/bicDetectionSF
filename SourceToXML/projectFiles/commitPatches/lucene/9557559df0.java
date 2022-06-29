From 9557559df07057b292ff5fd3023a8f6bcab05c1a Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Wed, 5 Jan 2011 21:04:28 +0000
Subject: [PATCH] LUCENE-2831: use int[] instead of AtomicInteger

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1055638 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/src/java/org/apache/lucene/util/ReaderUtil.java | 7 +++----
 1 file changed, 3 insertions(+), 4 deletions(-)

diff --git a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
index bc03c9a9376..701cfee2368 100644
-- a/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
++ b/lucene/src/java/org/apache/lucene/util/ReaderUtil.java
@@ -19,7 +19,6 @@ package org.apache.lucene.util;
 
 import java.util.ArrayList;
 import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
 import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
@@ -197,19 +196,19 @@ public final class ReaderUtil {
     }
     
     private int numLeaves(IndexReader reader) {
      final AtomicInteger numLeaves = new AtomicInteger();
      final int[] numLeaves = new int[1];
       try {
         new Gather(reader) {
           @Override
           protected void add(int base, IndexReader r) {
            numLeaves.incrementAndGet();
            numLeaves[0]++;
           }
         }.run();
       } catch (IOException ioe) {
         // won't happen
         throw new RuntimeException(ioe);
       }
      return numLeaves.get();
      return numLeaves[0];
     }
     
   }
- 
2.19.1.windows.1

