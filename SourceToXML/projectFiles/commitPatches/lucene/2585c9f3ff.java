From 2585c9f3ff750b8e551f261412625aef0e7d4a4b Mon Sep 17 00:00:00 2001
From: Uwe Schindler <uschindler@apache.org>
Date: Sat, 16 Jul 2016 10:09:40 +0200
Subject: [PATCH] LUCENE-7382: Fix bug introduced by LUCENE-7355 that used the
 wrong default AttributeFactory for new Tokenizers

--
 lucene/CHANGES.txt                                            | 4 ++++
 lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java | 4 ++--
 2 files changed, 6 insertions(+), 2 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 92ee7b91158..6c62aab5235 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -65,6 +65,10 @@ Bug Fixes
 * LUCENE-7340: MemoryIndex.toString() could throw NPE; fixed. Renamed to toStringDebug().
   (Daniel Collins, David Smiley)
 
* LUCENE-7382: Fix bug introduced by LUCENE-7355 that used the
  wrong default AttributeFactory for new Tokenizers.
  (Terry Smith, Uwe Schindler)

 Improvements
 
 * LUCENE-7323: Compound file writing now verifies the incoming
diff --git a/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java b/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java
index 0d60d2495dc..aa4b42db6a0 100644
-- a/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java
++ b/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java
@@ -287,9 +287,9 @@ public abstract class Analyzer implements Closeable {
   /** Return the {@link AttributeFactory} to be used for
    *  {@link #tokenStream analysis} and
    *  {@link #normalize(String, String) normalization}. The default
   *  implementation returns {@link AttributeFactory#DEFAULT_ATTRIBUTE_FACTORY}. */
   *  implementation returns {@link TokenStream#DEFAULT_TOKEN_ATTRIBUTE_FACTORY}. */
   protected AttributeFactory attributeFactory() {
    return AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY;
    return TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY;
   }
 
   /**
- 
2.19.1.windows.1

