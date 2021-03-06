From b5823b50db14d93b546fa898e33ad3dfea58df17 Mon Sep 17 00:00:00 2001
From: Erick Erickson <erick@apache.org>
Date: Mon, 8 Feb 2016 19:38:19 -0800
Subject: [PATCH] SOLR-8658: Fix test failure introduced in SOLR-8651

--
 .../solr/cloud/BasicDistributedZkTest.java    | 48 ++++++++++++++++---
 1 file changed, 42 insertions(+), 6 deletions(-)

diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index f479e27bf27..678c31c6798 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -39,6 +39,8 @@ import org.apache.solr.common.SolrDocument;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
 import org.apache.solr.common.cloud.Slice;
 import org.apache.solr.common.cloud.ZkCoreNodeProps;
 import org.apache.solr.common.cloud.ZkNodeProps;
@@ -370,22 +372,56 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     }
   }
 
  // Insure that total docs found is the expected number.
   private void waitForDocCount(long expectedNumFound, long waitMillis, String failureMessage)
      throws SolrServerException, IOException, InterruptedException {
      throws Exception {
     RTimer timer = new RTimer();
     long timeout = (long)timer.getTime() + waitMillis;
    while (cloudClient.query(new SolrQuery("*:*")).getResults().getNumFound() != expectedNumFound) {
      if (timeout <= (long)timer.getTime()) {
        fail(failureMessage);
    
    ClusterState clusterState = getCommonCloudSolrClient().getZkStateReader().getClusterState();
    DocCollection dColl = clusterState.getCollection(DEFAULT_COLLECTION);
    long docTotal = -1; // Could use this for 0 hits too!
    
    while (docTotal != expectedNumFound && timeout > (long) timer.getTime()) {
      docTotal = checkSlicesSameCounts(dColl);
      if (docTotal != expectedNumFound) {
        Thread.sleep(100);
       }
      Thread.sleep(100);
     }
    // We could fail here if we broke out of the above because we exceeded the time allowed.
    assertEquals(failureMessage, expectedNumFound, docTotal);
 
    // This should be redundant, but it caught a test error after all.
     for (SolrClient client : clients) {
       assertEquals(failureMessage, expectedNumFound, client.query(new SolrQuery("*:*")).getResults().getNumFound());
     }
   }
  

  // Insure that counts are the same for all replicas in each shard
  // Return the total doc count for the query.
  private long checkSlicesSameCounts(DocCollection dColl) throws SolrServerException, IOException {
    long docTotal = 0; // total number of documents found counting only one replica per slice.
    for (Slice slice : dColl.getActiveSlices()) {
      long sliceDocCount = -1;
      for (Replica rep : slice.getReplicas()) {
        HttpSolrClient one = new HttpSolrClient(rep.getCoreUrl());
        SolrQuery query = new SolrQuery("*:*");
        query.setDistrib(false);
        QueryResponse resp = one.query(query);
        long hits = resp.getResults().getNumFound();
        if (sliceDocCount == -1) {
          sliceDocCount = hits;
          docTotal += hits; 
        } else {
          if (hits != sliceDocCount) {
            return -1;
          }
        }
      }
    }
    return docTotal;
  }

   private void testShardParamVariations() throws Exception {
     SolrQuery query = new SolrQuery("*:*");
     Map<String,Long> shardCounts = new HashMap<>();
- 
2.19.1.windows.1

