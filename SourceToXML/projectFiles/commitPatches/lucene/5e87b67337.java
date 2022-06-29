From 5e87b67337f529546e28ddca9ab384244da96c15 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sat, 2 Nov 2013 19:05:58 +0000
Subject: [PATCH] SOLR-5311 trying to stop test failures

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1538254 13f79535-47bb-0310-9956-ffa450edef68
--
 .../test/org/apache/solr/cloud/DeleteReplicaTest.java | 11 ++++++-----
 1 file changed, 6 insertions(+), 5 deletions(-)

diff --git a/solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java b/solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java
index 1c3191da033..6824b882b64 100644
-- a/solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java
@@ -119,9 +119,6 @@ public class DeleteReplicaTest extends AbstractFullDistribZkTestBase {
 
 
 



   private void deleteLiveReplicaTest() throws Exception{
     String COLL_NAME = "delLiveColl";
     CloudSolrServer client = createCloudClient(null);
@@ -129,9 +126,13 @@ public class DeleteReplicaTest extends AbstractFullDistribZkTestBase {
     DocCollection testcoll = getCommonCloudSolrServer().getZkStateReader().getClusterState().getCollection(COLL_NAME);
     final Slice shard1 = testcoll.getSlices().iterator().next();
     if(!shard1.getState().equals(Slice.ACTIVE)) fail("shard is not active");
    Replica replica = shard1.getReplicas().iterator().next();
    boolean found = false;
    Replica replica1 = null;
    for (Replica replica : shard1.getReplicas()) if("active".equals(replica.getStr("state"))) replica1 =replica;

    if(replica1 == null) fail("no active relicas found");
 
    removeAndWaitForReplicaGone(COLL_NAME, client, replica, shard1.getName());
    removeAndWaitForReplicaGone(COLL_NAME, client, replica1, shard1.getName());
     client.shutdown();
 
 
- 
2.19.1.windows.1

