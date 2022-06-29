From 2c34cb858ec0f02c63615417a2fe044971250af0 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Tue, 9 Apr 2013 15:19:27 +0000
Subject: [PATCH] SOLR-4581: make float/double bits sort correctly for faceting

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1466078 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  4 +++
 .../apache/solr/request/NumericFacets.java    |  5 ++--
 .../org/apache/solr/request/TestFaceting.java | 27 +++++++++++++++++++
 3 files changed, 34 insertions(+), 2 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 86d07dfed34..9246f10f042 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -170,6 +170,10 @@ Bug Fixes
 * SOLR-4682: CoreAdminRequest.mergeIndexes can not merge multiple cores or indexDirs.
   (Jason.D.Cao via shalin)
 
* SOLR-4581: When faceting on numeric fields in Solr 4.2, negative values (constraints)
  were sorted incorrectly. (Alexander Buhr, shalin, yonik)


 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/request/NumericFacets.java b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
index a19356bf4c5..96796c996be 100644
-- a/solr/core/src/java/org/apache/solr/request/NumericFacets.java
++ b/solr/core/src/java/org/apache/solr/request/NumericFacets.java
@@ -39,6 +39,7 @@ import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.NumericUtils;
 import org.apache.lucene.util.PriorityQueue;
 import org.apache.lucene.util.StringHelper;
 import org.apache.solr.common.params.FacetParams;
@@ -171,7 +172,7 @@ final class NumericFacets {
             longs = new FieldCache.Longs() {
               @Override
               public long get(int docID) {
                return Float.floatToIntBits(floats.get(docID));
                return NumericUtils.floatToSortableInt(floats.get(docID));
               }
             };
             break;
@@ -180,7 +181,7 @@ final class NumericFacets {
             longs = new FieldCache.Longs() {
               @Override
               public long get(int docID) {
                return Double.doubleToLongBits(doubles.get(docID));
                return NumericUtils.doubleToSortableLong(doubles.get(docID));
               }
             };
             break;
diff --git a/solr/core/src/test/org/apache/solr/request/TestFaceting.java b/solr/core/src/test/org/apache/solr/request/TestFaceting.java
index 9cd0486b0aa..df71d9831e0 100755
-- a/solr/core/src/test/org/apache/solr/request/TestFaceting.java
++ b/solr/core/src/test/org/apache/solr/request/TestFaceting.java
@@ -294,4 +294,31 @@ public class TestFaceting extends SolrTestCaseJ4 {
     }
   }
 
  @Test
  public void testFacetSortWithMinCount() {
    assertU(adoc("id", "1.0", "f_td", "-420.126"));
    assertU(adoc("id", "2.0", "f_td", "-285.672"));
    assertU(adoc("id", "3.0", "f_td", "-1.218"));
    assertU(commit());

    /**
    assertQ(req("q", "*:*", FacetParams.FACET, "true", FacetParams.FACET_FIELD, "f_td", "f.f_td.facet.sort", FacetParams.FACET_SORT_INDEX),
        "*[count(//lst[@name='f_td']/int)=3]",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[1][@name='-420.126']",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[2][@name='-285.672']",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[3][@name='-1.218']");

    assertQ(req("q", "*:*", FacetParams.FACET, "true", FacetParams.FACET_FIELD, "f_td", "f.f_td.facet.sort", FacetParams.FACET_SORT_INDEX, FacetParams.FACET_MINCOUNT, "1", FacetParams.FACET_METHOD, FacetParams.FACET_METHOD_fc),
        "*[count(//lst[@name='f_td']/int)=3]",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[1][@name='-420.126']",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[2][@name='-285.672']",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[3][@name='-1.218']");
  **/  // nocommit
    assertQ(req("q", "*:*", FacetParams.FACET, "true", FacetParams.FACET_FIELD, "f_td", "f.f_td.facet.sort", FacetParams.FACET_SORT_INDEX, FacetParams.FACET_MINCOUNT, "1", "indent","true"),
        "*[count(//lst[@name='f_td']/int)=3]",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[1][@name='-420.126']",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[2][@name='-285.672']",
        "//lst[@name='facet_fields']/lst[@name='f_td']/int[3][@name='-1.218']");
  }

 }
\ No newline at end of file
- 
2.19.1.windows.1

