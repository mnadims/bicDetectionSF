From 5ba9ca4be2a250bd470baac69ee92a066df8921a Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Fri, 4 May 2012 23:41:12 +0000
Subject: [PATCH] LUCENE-4030: getOrdTermsEnum no longer returns null when no
 terms were uninverted (SOLR-3427)

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1334258 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/src/test/org/apache/lucene/index/TestDocTermOrds.java  | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/lucene/core/src/test/org/apache/lucene/index/TestDocTermOrds.java b/lucene/core/src/test/org/apache/lucene/index/TestDocTermOrds.java
index a4739e66755..69fb1d5ce10 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestDocTermOrds.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestDocTermOrds.java
@@ -324,7 +324,7 @@ public class TestDocTermOrds extends LuceneTestCase {
 
     //final TermsEnum te = subR.fields().terms("field").iterator();
     final TermsEnum te = dto.getOrdTermsEnum(r);
    if (te == null) {
    if (dto.numTerms() == 0) {
       if (prefixRef == null) {
         assertNull(MultiFields.getTerms(r, "field"));
       } else {
- 
2.19.1.windows.1

