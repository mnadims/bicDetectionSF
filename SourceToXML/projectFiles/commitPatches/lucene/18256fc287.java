From 18256fc2873f198e8e577c6eb0f337df1d1cda24 Mon Sep 17 00:00:00 2001
From: Chris Hostetter <hossman@apache.org>
Date: Tue, 31 May 2016 16:12:24 -0700
Subject: [PATCH] SOLR-8940: Fix group.sort option

--
 solr/CHANGES.txt                              |  2 ++
 .../TopGroupsResultTransformer.java           |  2 +-
 .../apache/solr/TestDistributedGrouping.java  | 34 ++++++++++++++++++-
 3 files changed, 36 insertions(+), 2 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 59d4aafb8dd..532f0b4047a 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -245,6 +245,8 @@ Bug Fixes
 * SOLR-9165: Spellcheck does not return collations if "maxCollationTries" is used with "cursorMark".
   (James Dyer)
 
* SOLR-8940: Fix group.sort option (hossman)

 Optimizations
 ----------------------
 * SOLR-8722: Don't force a full ZkStateReader refresh on every Overseer operation.
diff --git a/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java b/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java
index f65bcf24ca7..37e837e664e 100644
-- a/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java
++ b/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java
@@ -134,7 +134,7 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
 
         @SuppressWarnings("unchecked")
         List<NamedList<Object>> documents = (List<NamedList<Object>>) groupResult.get("documents");
        ScoreDoc[] scoreDocs = transformToNativeShardDoc(documents, groupSort, shard, schema);
        ScoreDoc[] scoreDocs = transformToNativeShardDoc(documents, sortWithinGroup, shard, schema);
 
         BytesRef groupValueRef = groupValue != null ? new BytesRef(groupValue) : null;
         groupDocs.add(new GroupDocs<>(Float.NaN, maxScore, totalGroupHits, scoreDocs, groupValueRef, null));
diff --git a/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java b/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java
index 5d19fd60506..af42ff419c8 100644
-- a/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java
++ b/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java
@@ -23,9 +23,11 @@ import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.SolrDocumentList;
 import org.junit.Test;
 
 import java.io.IOException;
import java.util.List;
 
 /**
  * TODO? perhaps use:
@@ -44,7 +46,7 @@ public class TestDistributedGrouping extends BaseDistributedSearchTestCase {
   String tlong = "other_tl1";
   String tdate_a = "a_n_tdt";
   String tdate_b = "b_n_tdt";
  String oddField="oddField_s";
  String oddField="oddField_s1";
 
   @Test
   public void test() throws Exception {
@@ -265,6 +267,36 @@ public class TestDistributedGrouping extends BaseDistributedSearchTestCase {
     query("q", "{!func}id", "rows", 100, "fl", "score,id," + i1, "group", "true", "group.field", i1, "group.limit", 10, "sort", "score desc, _docid_ asc, id asc");
     query("q", "{!func}id", "rows", 100, "fl", "score,id," + i1, "group", "true", "group.field", i1, "group.limit", 10);
 
    // some explicit checks of non default sorting, and sort/group.sort with diff clauses
    query("q", "{!func}id", "rows", 100, "fl", tlong + ",id," + i1, "group", "true",
          "group.field", i1, "group.limit", 10,
          "sort", tlong+" asc, id desc");
    query("q", "{!func}id", "rows", 100, "fl", tlong + ",id," + i1, "group", "true",
          "group.field", i1, "group.limit", 10,
          "sort", "id asc",
          "group.sort", tlong+" asc, id desc");
    query("q", "{!func}id", "rows", 100, "fl", tlong + ",id," + i1, "group", "true",
          "group.field", i1, "group.limit", 10,
          "sort", tlong+" asc, id desc",
          "group.sort", "id asc");
    rsp = query("q", "{!func}id", "fq", oddField+":[* TO *]",
                "rows", 100, "fl", tlong + ",id," + i1, "group", "true",
                "group.field", i1, "group.limit", 10,
                "sort", tlong+" asc",
                "group.sort", oddField+" asc");
    nl = (NamedList<?>) rsp.getResponse().get("grouped");
    nl = (NamedList<?>) nl.get(i1);
    assertEquals(rsp.toString(), 6, nl.get("matches"));
    assertEquals(rsp.toString(), 2, ((List<NamedList<?>>)nl.get("groups")).size());
    nl = ((List<NamedList<?>>)nl.get("groups")).get(0);
    assertEquals(rsp.toString(), 232, nl.get("groupValue"));
    SolrDocumentList docs = (SolrDocumentList) nl.get("doclist");
    assertEquals(docs.toString(), 5, docs.getNumFound());
    assertEquals(docs.toString(), 22, docs.get(0).getFirstValue("id"));
    assertEquals(docs.toString(), 21, docs.get(4).getFirstValue("id"));

    

     // Can't validate the response, but can check if no errors occur.
     simpleQuery("q", "*:*", "rows", 100, "fl", "id," + i1, "group", "true", "group.query", t1 + ":kings OR " + t1 + ":eggs", "group.limit", 10, "sort", i1 + " asc, id asc", CommonParams.TIME_ALLOWED, 1);
     
- 
2.19.1.windows.1

