From 7053a99f4939cb2780cdca68d5333cfdb09267ad Mon Sep 17 00:00:00 2001
From: Erick Erickson <erick@apache.org>
Date: Sun, 7 Feb 2016 15:30:27 -0800
Subject: [PATCH] SOLR-8651: The commitWithin parameter is not passed on for
 deleteById in UpdateRequest for distributed queries

--
 solr/CHANGES.txt                              |  3 +
 .../solr/cloud/BasicDistributedZkTest.java    | 62 ++++++++++++-------
 .../client/solrj/request/UpdateRequest.java   |  1 +
 3 files changed, 43 insertions(+), 23 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 8ad69f733ed..754976c75ba 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -456,6 +456,9 @@ Bug Fixes
 * SOLR-8575: Fix HDFSLogReader replay status numbers and a performance bug where we can reopen
   FSDataInputStream too often. (Mark Miller, Patrick Dvorack)
   
* SOLR-8651: The commitWithin parameter is not passed on for deleteById in UpdateRequest in
  distributed queries (Jessica Cheng Mallet via Erick Erickson)
  
 Optimizations
 ----------------------
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index 87604fcaa9b..f479e27bf27 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -25,13 +25,13 @@ import org.apache.solr.client.solrj.SolrClient;
 import org.apache.solr.client.solrj.SolrQuery;
 import org.apache.solr.client.solrj.SolrRequest;
 import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
 import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
 import org.apache.solr.client.solrj.request.CoreAdminRequest.Create;
 import org.apache.solr.client.solrj.request.CoreAdminRequest.Unload;
 import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
 import org.apache.solr.client.solrj.response.CollectionAdminResponse;
 import org.apache.solr.client.solrj.response.QueryResponse;
 import org.apache.solr.client.solrj.response.UpdateResponse;
@@ -49,10 +49,8 @@ import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.UpdateParams;
 import org.apache.solr.common.util.ExecutorUtil;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.update.DirectUpdateHandler2;
 import org.apache.solr.util.DefaultSolrThreadFactory;
import org.apache.solr.util.TimeOut;
import org.junit.BeforeClass;
import org.apache.solr.util.RTimer;
 import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -63,7 +61,6 @@ import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
@@ -320,7 +317,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     query(false, new Object[] {"q", "id:[1 TO 5]", CommonParams.DEBUG, CommonParams.RESULTS});
     query(false, new Object[] {"q", "id:[1 TO 5]", CommonParams.DEBUG, CommonParams.QUERY});
 
    // try commitWithin
    // try add commitWithin
     long before = cloudClient.query(new SolrQuery("*:*")).getResults().getNumFound();
     for (SolrClient client : clients) {
       assertEquals("unexpected pre-commitWithin document count on node: " + ((HttpSolrClient)client).getBaseURL(), before, client.query(new SolrQuery("*:*")).getResults().getNumFound());
@@ -328,23 +325,26 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
 
     ModifiableSolrParams params = new ModifiableSolrParams();
     params.set("commitWithin", 10);
    add(cloudClient, params, getDoc("id", 300));

    final List<SolrClient> clientsToCheck = new ArrayList<>(clients);
    TimeOut timeout = new TimeOut(45, TimeUnit.SECONDS);
    do {
      final Iterator<SolrClient> it = clientsToCheck.iterator();
      while (it.hasNext()) {
        final SolrClient sc = it.next();
        if ((before + 1) == sc.query(new SolrQuery("*:*")).getResults().getNumFound()) {
          it.remove();
        }
      }
      Thread.sleep(100);
    } while (!clientsToCheck.isEmpty() && !timeout.hasTimedOut());
    
    assertTrue("commitWithin did not work on some nodes: "+clientsToCheck, clientsToCheck.isEmpty());
    
    add(cloudClient, params , getDoc("id", 300), getDoc("id", 301));

    waitForDocCount(before + 2, 30000, "add commitWithin did not work");

    // try deleteById commitWithin
    UpdateRequest deleteByIdReq = new UpdateRequest();
    deleteByIdReq.deleteById("300");
    deleteByIdReq.setCommitWithin(10);
    deleteByIdReq.process(cloudClient);

    waitForDocCount(before + 1, 30000, "deleteById commitWithin did not work");

    // try deleteByQuery commitWithin
    UpdateRequest deleteByQueryReq = new UpdateRequest();
    deleteByQueryReq.deleteByQuery("id:301");
    deleteByQueryReq.setCommitWithin(10);
    deleteByQueryReq.process(cloudClient);

    waitForDocCount(before, 30000, "deleteByQuery commitWithin did not work");

     // TODO: This test currently fails because debug info is obtained only
     // on shards with matches.
     // query("q","matchesnothing","fl","*,score", "debugQuery", "true");
@@ -369,6 +369,22 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
       super.printLayout();
     }
   }

  private void waitForDocCount(long expectedNumFound, long waitMillis, String failureMessage)
      throws SolrServerException, IOException, InterruptedException {
    RTimer timer = new RTimer();
    long timeout = (long)timer.getTime() + waitMillis;
    while (cloudClient.query(new SolrQuery("*:*")).getResults().getNumFound() != expectedNumFound) {
      if (timeout <= (long)timer.getTime()) {
        fail(failureMessage);
      }
      Thread.sleep(100);
    }

    for (SolrClient client : clients) {
      assertEquals(failureMessage, expectedNumFound, client.query(new SolrQuery("*:*")).getResults().getNumFound());
    }
  }
   
   private void testShardParamVariations() throws Exception {
     SolrQuery query = new SolrQuery("*:*");
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java b/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java
index 0d331d25efb..d0f77599d51 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/request/UpdateRequest.java
@@ -299,6 +299,7 @@ public class UpdateRequest extends AbstractUpdateRequest {
           UpdateRequest urequest = new UpdateRequest();
           urequest.setParams(params);
           urequest.deleteById(deleteId, version);
          urequest.setCommitWithin(getCommitWithin());
           request = new LBHttpSolrClient.Req(urequest, urls);
           routes.put(leaderUrl, request);
         }
- 
2.19.1.windows.1

