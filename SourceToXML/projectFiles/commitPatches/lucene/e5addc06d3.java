From e5addc06d3accf159434a57df8f9ac88c218117e Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Tue, 28 Jun 2011 21:39:01 +0000
Subject: [PATCH] SOLR-2626, LUCENE-2831: fix offset bug in cutover to
 AtomicReaderContext

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1140859 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solr/search/function/FileFloatSource.java    |  3 +--
 .../solr/search/function/TestFunctionQuery.java  | 16 +++++++++++++++-
 2 files changed, 16 insertions(+), 3 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/function/FileFloatSource.java b/solr/src/java/org/apache/solr/search/function/FileFloatSource.java
index 53ee329525e..8675aee526b 100755
-- a/solr/src/java/org/apache/solr/search/function/FileFloatSource.java
++ b/solr/src/java/org/apache/solr/search/function/FileFloatSource.java
@@ -76,9 +76,8 @@ public class FileFloatSource extends ValueSource {
 
   @Override
   public DocValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    int offset = 0;
    final int off = readerContext.docBase;
     ReaderContext topLevelContext = ReaderUtil.getTopLevelContext(readerContext);
    final int off = offset;
 
     final float[] arr = getCachedFloats(topLevelContext.reader);
     return new FloatDocValues(this) {
diff --git a/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java b/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
index 47f26d985d7..abdd43022b2 100755
-- a/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
++ b/solr/src/test/org/apache/solr/search/function/TestFunctionQuery.java
@@ -65,11 +65,25 @@ public class TestFunctionQuery extends SolrTestCaseJ4 {
     // lrf.args.put("version","2.0");
     for (float val : values) {
       String s = Float.toString(val);

       if (field!=null) assertU(adoc("id", s, field, s));
       else assertU(adoc("id", s));

      if (random.nextInt(100) < 20) {
        if (field!=null) assertU(adoc("id", s, field, s));
        else assertU(adoc("id", s));
      }

      if (random.nextInt(100) < 20) {
        assertU(commit());

      }


       // System.out.println("added doc for " + val);
     }
    assertU(optimize()); // squeeze out any possible deleted docs
    // assertU(optimize()); // squeeze out any possible deleted docs
    assertU(commit());
   }
 
   // replace \0 with the field name and create a parseable string 
- 
2.19.1.windows.1

