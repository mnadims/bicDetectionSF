From cd49552b579a52ec2ce1969c0788a40e8208c824 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Wed, 30 Oct 2013 12:10:20 +0000
Subject: [PATCH] SOLR-5311 tests were failing intermittently

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1537060 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solr/cloud/DeleteInactiveReplicaTest.java | 38 +++++++++++++++++--
 1 file changed, 34 insertions(+), 4 deletions(-)

diff --git a/solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java b/solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java
index 82bcffc8ddb..f5482ed12ef 100644
-- a/solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java
@@ -28,7 +28,10 @@ import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.params.MapSolrParams;
 import org.apache.solr.common.util.NamedList;
 
import java.net.URL;
 import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
 
 import static org.apache.solr.common.cloud.ZkNodeProps.makeMap;
 
@@ -43,11 +46,38 @@ public class DeleteInactiveReplicaTest extends DeleteReplicaTest{
     CloudSolrServer client = createCloudClient(null);
     createCloudClient(null);
     createColl(COLL_NAME, client);

    boolean stopped = false;
    JettySolrRunner stoppedJetty = null;
    StringBuilder sb = new StringBuilder();
    Replica replica1=null;
    Slice shard1 = null;
     DocCollection testcoll = getCommonCloudSolrServer().getZkStateReader().getClusterState().getCollection(COLL_NAME);
    final Slice shard1 = testcoll.getSlices().iterator().next();
    for (JettySolrRunner jetty : jettys) sb.append(jetty.getBaseUrl()).append(",");

    for (Slice slice : testcoll.getActiveSlices()) {
      for (Replica replica : slice.getReplicas())
        for (JettySolrRunner jetty : jettys) {
          URL baseUrl = null;
          try {
            baseUrl = jetty.getBaseUrl();
          } catch (Exception e) {
            continue;
          }
          if (baseUrl.toString().startsWith(replica.getStr(ZkStateReader.BASE_URL_PROP))) {
            stoppedJetty = jetty;
            ChaosMonkey.stop(jetty);
            replica1 = replica;
            shard1 = slice;
            stopped = true;
            break;
          }
        }
    }

    /*final Slice shard1 = testcoll.getSlices().iterator().next();
     if(!shard1.getState().equals(Slice.ACTIVE)) fail("shard is not active");
     Replica replica1 = shard1.getReplicas().iterator().next();
    boolean stopped = false;
     JettySolrRunner stoppedJetty = null;
     StringBuilder sb = new StringBuilder();
     for (JettySolrRunner jetty : jettys) {
@@ -58,9 +88,9 @@ public class DeleteInactiveReplicaTest extends DeleteReplicaTest{
         stopped = true;
         break;
       }
    }
    }*/
     if(!stopped){
      fail("Could not find jetty for replica "+ replica1 + "jettys: "+sb);
      fail("Could not find jetty to stop in collection "+ testcoll + " jettys: "+sb);
     }
 
     long endAt = System.currentTimeMillis()+3000;
- 
2.19.1.windows.1

