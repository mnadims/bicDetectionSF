From a5402f68631768bae57d923613211128de077982 Mon Sep 17 00:00:00 2001
From: Uwe Schindler <uschindler@apache.org>
Date: Fri, 8 Sep 2017 17:39:12 +0200
Subject: [PATCH] LUCENE-7963: Remove useless getAttribute() in
 DefaultIndexingChain that causes performance drop, introduced by LUCENE-7626

--
 lucene/CHANGES.txt                                            | 4 ++++
 .../java/org/apache/lucene/index/DefaultIndexingChain.java    | 2 --
 2 files changed, 4 insertions(+), 2 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 0dced289cf3..a2fd2234fc9 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -200,6 +200,10 @@ Bug Fixes
 * LUCENE-7956: Fixed potential stack overflow error in ICUNormalizer2CharFilter.
   (Adrien Grand)
 
* LUCENE-7963: Remove useless getAttribute() in DefaultIndexingChain that
  causes performance drop, introduced by LUCENE-7626.  (Daniel Mitterdorfer
  via Uwe Schindler)

 Improvements
 
 * LUCENE-7489: Better storage of sparse doc-values fields with the default
diff --git a/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java b/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java
index f2c3de11a4c..fd241057eca 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java
++ b/lucene/core/src/java/org/apache/lucene/index/DefaultIndexingChain.java
@@ -27,7 +27,6 @@ import java.util.Map;
 import java.util.Set;
 
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.codecs.DocValuesConsumer;
 import org.apache.lucene.codecs.DocValuesFormat;
 import org.apache.lucene.codecs.NormsConsumer;
@@ -733,7 +732,6 @@ final class DefaultIndexingChain extends DocConsumer {
         stream.reset();
         invertState.setAttributeSource(stream);
         termsHashPerField.start(field, first);
        CharTermAttribute termAtt = tokenStream.getAttribute(CharTermAttribute.class);
 
         while (stream.incrementToken()) {
 
- 
2.19.1.windows.1

