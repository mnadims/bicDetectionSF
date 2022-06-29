From eee47902b7fb0f16e059c0c1c696615675f4cc2e Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Wed, 2 Feb 2011 16:22:51 +0000
Subject: [PATCH] LUCENE-2831: remove unnecessary casts

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1066515 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/src/java/org/apache/solr/search/DocSet.java          | 5 ++---
 solr/src/java/org/apache/solr/search/SortedIntDocSet.java | 3 +--
 2 files changed, 3 insertions(+), 5 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/DocSet.java b/solr/src/java/org/apache/solr/search/DocSet.java
index d188389ecd5..ddfc3cde67f 100644
-- a/solr/src/java/org/apache/solr/search/DocSet.java
++ b/solr/src/java/org/apache/solr/search/DocSet.java
@@ -248,9 +248,8 @@ abstract class DocSetBase implements DocSet {
 
     return new Filter() {
       @Override
      public DocIdSet getDocIdSet(AtomicReaderContext ctx) throws IOException {
        IndexReader.AtomicReaderContext context = (IndexReader.AtomicReaderContext)ctx;  // TODO: remove after lucene migration
        IndexReader reader = ctx.reader;
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
        IndexReader reader = context.reader;
 
         if (context.isTopLevel) {
           return bs;
diff --git a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
index 295a794bde9..ee3b9b47160 100755
-- a/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
++ b/solr/src/java/org/apache/solr/search/SortedIntDocSet.java
@@ -552,8 +552,7 @@ public class SortedIntDocSet extends DocSetBase {
       int lastEndIdx = 0;
 
       @Override
      public DocIdSet getDocIdSet(AtomicReaderContext contextX) throws IOException {
        AtomicReaderContext context = (AtomicReaderContext)contextX;  // TODO: remove after lucene migration
      public DocIdSet getDocIdSet(AtomicReaderContext context) throws IOException {
         IndexReader reader = context.reader;
 
         final int base = context.docBase;
- 
2.19.1.windows.1

