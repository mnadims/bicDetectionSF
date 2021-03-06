From f50e4dbf5184d0ccddb86f72b7163c0ba7b7e5ad Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Tue, 21 Feb 2012 20:52:54 +0000
Subject: [PATCH] SOLR-3150: NPE when facetting using facet.prefix on an empty
 field

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1292007 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/solr/request/UnInvertedField.java  |  2 +-
 .../apache/solr/request/SimpleFacetsTest.java | 70 ++++++++++++++++++-
 2 files changed, 70 insertions(+), 2 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/request/UnInvertedField.java b/solr/core/src/java/org/apache/solr/request/UnInvertedField.java
index af126949bf5..eeba3ded5a2 100755
-- a/solr/core/src/java/org/apache/solr/request/UnInvertedField.java
++ b/solr/core/src/java/org/apache/solr/request/UnInvertedField.java
@@ -227,7 +227,7 @@ public class UnInvertedField extends DocTermOrds {
       int endTerm = numTermsInField;  // one past the end
 
       TermsEnum te = getOrdTermsEnum(searcher.getAtomicReader());
      if (prefix != null && prefix.length() > 0) {
      if (te != null && prefix != null && prefix.length() > 0) {
         final BytesRef prefixBr = new BytesRef(prefix);
         if (te.seekCeil(prefixBr, true) == TermsEnum.SeekStatus.END) {
           startTerm = numTermsInField;
diff --git a/solr/core/src/test/org/apache/solr/request/SimpleFacetsTest.java b/solr/core/src/test/org/apache/solr/request/SimpleFacetsTest.java
index 47f31f6f591..bc2747c5542 100644
-- a/solr/core/src/test/org/apache/solr/request/SimpleFacetsTest.java
++ b/solr/core/src/test/org/apache/solr/request/SimpleFacetsTest.java
@@ -17,12 +17,17 @@
 
 package org.apache.solr.request;
 
import org.apache.noggit.ObjectBuilder;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.schema.SchemaField;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.List;
import java.util.Map;
 
 
 public class SimpleFacetsTest extends SolrTestCaseJ4 {
@@ -53,7 +58,9 @@ public class SimpleFacetsTest extends SolrTestCaseJ4 {
   }
 
 
  static void createIndex() {
  static void createIndex() throws Exception {
    doEmptyFacetCounts();   // try on empty index

     indexSimpleFacetCounts();
     indexDateFacets();
     indexFacetSingleValued();
@@ -95,6 +102,67 @@ public class SimpleFacetsTest extends SolrTestCaseJ4 {
             "zerolen_s","");   
   }
 
  @Test
  public void testEmptyFacetCounts() throws Exception {
    doEmptyFacetCounts();
  }

  // static so we can try both with and without an empty index
  static void doEmptyFacetCounts() throws Exception {
    doEmptyFacetCounts("empty_t", new String[]{null, "myprefix",""});
    doEmptyFacetCounts("empty_i", new String[]{null});
    doEmptyFacetCounts("empty_f", new String[]{null});
    doEmptyFacetCounts("empty_s", new String[]{null, "myprefix",""});
    doEmptyFacetCounts("empty_d", new String[]{null});
  }
  
  static void doEmptyFacetCounts(String field, String[] prefixes) throws Exception {
    SchemaField sf = h.getCore().getSchema().getField(field);

    String response = JQ(req("q", "*:*"));
    Map rsp = (Map) ObjectBuilder.fromJSON(response);
    Long numFound  = (Long)(((Map)rsp.get("response")).get("numFound"));

    ModifiableSolrParams params = params("q","*:*", "rows","0", "facet","true", "facet.field","{!key=myalias}"+field);
    
    String[] methods = {null, "fc","enum","fcs"};
    if (sf.multiValued() || sf.getType().multiValuedFieldCache()) {
      methods = new String[]{null, "fc","enum"};
    }

    prefixes = prefixes==null ? new String[]{null} : prefixes;


    for (String method : methods) {
      if (method == null) {
        params.remove("facet.method");
      } else {
        params.set("facet.method", method);
      }
      for (String prefix : prefixes) {
        if (prefix == null) {
          params.remove("facet.prefix");
        } else {
          params.set("facet.prefix", prefix);
        }

        for (String missing : new String[] {null, "true"}) {
          if (missing == null) {
            params.remove("facet.missing");
          } else {
            params.set("facet.missing", missing);
          }
          
          String expected = missing==null ? "[]" : "[null," + numFound + "]";
          
          assertJQ(req(params),
              "/facet_counts/facet_fields/myalias==" + expected);
        }
      }
    }
  }


   @Test
   public void testSimpleFacetCounts() {
  
- 
2.19.1.windows.1

