From 093a8ce57c06f1bf2f71ddde52dcc7b40cbd6197 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Mon, 7 Mar 2016 15:03:03 +0530
Subject: [PATCH] SOLR-8745: Deprecate costly
 ZkStateReader.updateClusterState(), replace with a narrow
 forceUpdateCollection(collection)

--
 solr/CHANGES.txt                              |  3 ++
 .../hadoop/MorphlineGoLiveMiniMRTest.java     |  1 -
 .../apache/solr/cloud/ElectionContext.java    |  2 +-
 .../cloud/LeaderInitiatedRecoveryThread.java  |  6 ---
 .../OverseerCollectionMessageHandler.java     |  1 -
 .../org/apache/solr/cloud/ZkController.java   |  2 +-
 .../solr/handler/CdcrRequestHandler.java      |  2 +-
 .../solr/handler/admin/ClusterStatus.java     |  3 --
 .../handler/admin/CollectionsHandler.java     |  2 -
 .../handler/admin/CoreAdminOperation.java     |  6 +--
 .../solr/handler/admin/RebalanceLeaders.java  |  2 +-
 .../solr/cloud/BaseCdcrDistributedZkTest.java |  1 -
 .../solr/cloud/BasicDistributedZkTest.java    |  4 +-
 .../cloud/ChaosMonkeyNothingIsSafeTest.java   |  2 +-
 .../solr/cloud/ChaosMonkeyShardSplitTest.java |  2 +-
 .../solr/cloud/CollectionReloadTest.java      |  2 +-
 .../cloud/CollectionTooManyReplicasTest.java  |  6 +--
 .../CollectionsAPIDistributedZkTest.java      |  9 ++--
 .../solr/cloud/CustomCollectionTest.java      |  1 -
 .../apache/solr/cloud/DeleteShardTest.java    |  2 -
 .../apache/solr/cloud/ForceLeaderTest.java    | 11 ++---
 .../apache/solr/cloud/HttpPartitionTest.java  |  9 +---
 .../LeaderFailoverAfterPartitionTest.java     |  2 -
 .../LeaderInitiatedRecoveryOnCommitTest.java  |  4 +-
 .../solr/cloud/MigrateRouteKeyTest.java       |  4 +-
 .../org/apache/solr/cloud/OverseerTest.java   | 12 ++---
 .../solr/cloud/ReplicaPropertiesBase.java     |  3 --
 .../org/apache/solr/cloud/ShardSplitTest.java |  1 -
 .../org/apache/solr/cloud/SyncSliceTest.java  |  1 -
 .../solr/cloud/TestCloudDeleteByQuery.java    |  1 -
 .../apache/solr/cloud/TestCollectionAPI.java  |  4 +-
 .../TestLeaderInitiatedRecoveryThread.java    |  1 -
 .../solr/cloud/TestMiniSolrCloudCluster.java  | 47 +++++++++----------
 .../cloud/TestMiniSolrCloudClusterBase.java   |  3 +-
 .../cloud/TestRandomRequestDistribution.java  |  4 +-
 .../solr/cloud/TestRebalanceLeaders.java      |  1 -
 .../solr/cloud/TestReplicaProperties.java     |  1 -
 .../cloud/TestSolrCloudWithKerberosAlt.java   |  1 +
 .../solr/cloud/UnloadDistributedZkTest.java   |  4 +-
 .../apache/solr/cloud/ZkControllerTest.java   |  2 +-
 .../solr/cloud/hdfs/StressHdfsTest.java       |  3 +-
 .../cloud/overseer/ZkStateReaderTest.java     |  6 +--
 .../cloud/overseer/ZkStateWriterTest.java     | 10 ++--
 .../solr/common/cloud/ZkStateReader.java      | 46 ++++++++++++++++++
 .../solr/cloud/AbstractDistribZkTestBase.java |  4 +-
 .../cloud/AbstractFullDistribZkTestBase.java  | 10 ++--
 .../org/apache/solr/cloud/ChaosMonkey.java    | 14 +-----
 47 files changed, 129 insertions(+), 139 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 6834eb56bb2..61fcd47e233 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -299,6 +299,9 @@ Optimizations
 
 * SOLR-8720: ZkController#publishAndWaitForDownStates should use #publishNodeAsDown. (Mark Miller)
 
* SOLR-8745: Deprecate costly ZkStateReader.updateClusterState(), replace with a narrow
  forceUpdateCollection(collection) (Scott Blum via shalin)

 Other Changes
 ----------------------
 
diff --git a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java
index 1cc1723db9b..95ed9b2b17d 100644
-- a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java
++ b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java
@@ -646,7 +646,6 @@ public class MorphlineGoLiveMiniMRTest extends AbstractFullDistribZkTestBase {
       }
       
       Thread.sleep(200);
      cloudClient.getZkStateReader().updateClusterState();
     }
     
     if (TEST_NIGHTLY) {
diff --git a/solr/core/src/java/org/apache/solr/cloud/ElectionContext.java b/solr/core/src/java/org/apache/solr/cloud/ElectionContext.java
index 210787757bb..38f6083bcb6 100644
-- a/solr/core/src/java/org/apache/solr/cloud/ElectionContext.java
++ b/solr/core/src/java/org/apache/solr/cloud/ElectionContext.java
@@ -462,7 +462,7 @@ final class ShardLeaderElectionContext extends ShardLeaderElectionContextBase {
   public void publishActiveIfRegisteredAndNotActive(SolrCore core) throws KeeperException, InterruptedException {
       if (core.getCoreDescriptor().getCloudDescriptor().hasRegistered()) {
         ZkStateReader zkStateReader = zkController.getZkStateReader();
        zkStateReader.updateClusterState();
        zkStateReader.forceUpdateCollection(collection);
         ClusterState clusterState = zkStateReader.getClusterState();
         Replica rep = (clusterState == null) ? null
             : clusterState.getReplica(collection, leaderProps.getStr(ZkStateReader.CORE_NODE_NAME_PROP));
diff --git a/solr/core/src/java/org/apache/solr/cloud/LeaderInitiatedRecoveryThread.java b/solr/core/src/java/org/apache/solr/cloud/LeaderInitiatedRecoveryThread.java
index 7a72a6782ee..589ed83e833 100644
-- a/solr/core/src/java/org/apache/solr/cloud/LeaderInitiatedRecoveryThread.java
++ b/solr/core/src/java/org/apache/solr/cloud/LeaderInitiatedRecoveryThread.java
@@ -244,12 +244,6 @@ public class LeaderInitiatedRecoveryThread extends Thread {
         
         // see if the replica's node is still live, if not, no need to keep doing this loop
         ZkStateReader zkStateReader = zkController.getZkStateReader();
        try {
          zkStateReader.updateClusterState();
        } catch (Exception exc) {
          log.warn("Error when updating cluster state: "+exc);
        }        
        
         if (!zkStateReader.getClusterState().liveNodesContain(replicaNodeName)) {
           log.warn("Node "+replicaNodeName+" hosting core "+coreNeedingRecovery+
               " is no longer live. No need to keep trying to tell it to recover!");
diff --git a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java
index 6b7f6067d8b..d7d894bc69b 100644
-- a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java
++ b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionMessageHandler.java
@@ -1371,7 +1371,6 @@ public class OverseerCollectionMessageHandler implements OverseerMessageHandler
         return;
       }
       Thread.sleep(1000);
      zkStateReader.updateClusterState();
     }
     throw new SolrException(ErrorCode.SERVER_ERROR,
         "Could not find new slice " + sliceName + " in collection " + collectionName
diff --git a/solr/core/src/java/org/apache/solr/cloud/ZkController.java b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
index 7d2752a75ae..81897b717e9 100644
-- a/solr/core/src/java/org/apache/solr/cloud/ZkController.java
++ b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
@@ -883,7 +883,7 @@ public final class ZkController {
       }
       
       // make sure we have an update cluster state right away
      zkStateReader.updateClusterState();
      zkStateReader.forceUpdateCollection(collection);
       return shardId;
     } finally {
       MDCLoggingContext.clear();
diff --git a/solr/core/src/java/org/apache/solr/handler/CdcrRequestHandler.java b/solr/core/src/java/org/apache/solr/handler/CdcrRequestHandler.java
index 585c8396d23..23e4abac304 100644
-- a/solr/core/src/java/org/apache/solr/handler/CdcrRequestHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/CdcrRequestHandler.java
@@ -361,7 +361,7 @@ public class CdcrRequestHandler extends RequestHandlerBase implements SolrCoreAw
       throws IOException, SolrServerException {
     ZkController zkController = core.getCoreDescriptor().getCoreContainer().getZkController();
     try {
      zkController.getZkStateReader().updateClusterState();
      zkController.getZkStateReader().forceUpdateCollection(collection);
     } catch (Exception e) {
       log.warn("Error when updating cluster state", e);
     }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/ClusterStatus.java b/solr/core/src/java/org/apache/solr/handler/admin/ClusterStatus.java
index 667d9fa11f5..ff60adc465b 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/ClusterStatus.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/ClusterStatus.java
@@ -57,9 +57,6 @@ public class ClusterStatus {
   @SuppressWarnings("unchecked")
   public  void getClusterStatus(NamedList results)
       throws KeeperException, InterruptedException {
    zkStateReader.updateClusterState();


     // read aliases
     Aliases aliases = zkStateReader.getAliases();
     Map<String, List<String>> collectionVsAliases = new HashMap<>();
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
index de2104f4d07..593dac81bcc 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
@@ -920,8 +920,6 @@ public class CollectionsHandler extends RequestHandlerBase {
         + (checkLeaderOnly ? "leaders" : "replicas"));
     ZkStateReader zkStateReader = cc.getZkController().getZkStateReader();
     for (int i = 0; i < numRetries; i++) {

      zkStateReader.updateClusterState();
       ClusterState clusterState = zkStateReader.getClusterState();
 
       Collection<Slice> shards = clusterState.getSlices(collectionName);
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java
index 8240189cf04..e755b82ff49 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminOperation.java
@@ -461,6 +461,7 @@ enum CoreAdminOperation {
             // to accept updates
             CloudDescriptor cloudDescriptor = core.getCoreDescriptor()
                 .getCloudDescriptor();
            String collection = cloudDescriptor.getCollectionName();
 
             if (retry % 15 == 0) {
               if (retry > 0 && log.isInfoEnabled())
@@ -470,7 +471,7 @@ enum CoreAdminOperation {
                     waitForState + "; forcing ClusterState update from ZooKeeper");
 
               // force a cluster state update
              coreContainer.getZkController().getZkStateReader().updateClusterState();
              coreContainer.getZkController().getZkStateReader().forceUpdateCollection(collection);
             }
 
             if (maxTries == 0) {
@@ -483,7 +484,6 @@ enum CoreAdminOperation {
             }
 
             ClusterState clusterState = coreContainer.getZkController().getClusterState();
            String collection = cloudDescriptor.getCollectionName();
             Slice slice = clusterState.getSlice(collection, cloudDescriptor.getShardId());
             if (slice != null) {
               final Replica replica = slice.getReplicasMap().get(coreNodeName);
@@ -937,4 +937,4 @@ enum CoreAdminOperation {
     return size;
   }
 
}
\ No newline at end of file
}
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/RebalanceLeaders.java b/solr/core/src/java/org/apache/solr/handler/admin/RebalanceLeaders.java
index 4626fc92967..98e796da73d 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/RebalanceLeaders.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/RebalanceLeaders.java
@@ -79,7 +79,7 @@ class RebalanceLeaders {
       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
           String.format(Locale.ROOT, "The " + COLLECTION_PROP + " is required for the Rebalance Leaders command."));
     }
    coreContainer.getZkController().getZkStateReader().updateClusterState();
    coreContainer.getZkController().getZkStateReader().forceUpdateCollection(collectionName);
     ClusterState clusterState = coreContainer.getZkController().getClusterState();
     DocCollection dc = clusterState.getCollection(collectionName);
     if (dc == null) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java
index f1f3e9167b1..fe94309bba2 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BaseCdcrDistributedZkTest.java
@@ -635,7 +635,6 @@ public class BaseCdcrDistributedZkTest extends AbstractDistribZkTestBase {
     try {
       cloudClient.connect();
       ZkStateReader zkStateReader = cloudClient.getZkStateReader();
      zkStateReader.updateClusterState();
       ClusterState clusterState = zkStateReader.getClusterState();
       DocCollection coll = clusterState.getCollection(collection);
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index d25ce664809..8222e91677f 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -552,7 +552,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
 
     Thread.sleep(5000);
     ChaosMonkey.start(cloudJettys.get(0).jetty);
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection("multiunload2");
     try {
       cloudClient.getZkStateReader().getLeaderRetry("multiunload2", "shard1", 30000);
     } catch (SolrException e) {
@@ -830,7 +830,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     
     // we added a role of none on these creates - check for it
     ZkStateReader zkStateReader = getCommonCloudSolrClient().getZkStateReader();
    zkStateReader.updateClusterState();
    zkStateReader.forceUpdateCollection(oneInstanceCollection2);
     Map<String,Slice> slices = zkStateReader.getClusterState().getSlicesMap(oneInstanceCollection2);
     assertNotNull(slices);
     String roles = slices.get("slice1").getReplicasMap().values().iterator().next().getStr(ZkStateReader.ROLES_PROP);
diff --git a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
index 8cc80d9c843..7dceada1668 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
@@ -205,7 +205,7 @@ public class ChaosMonkeyNothingIsSafeTest extends AbstractFullDistribZkTestBase
       
       // TODO: assert we didnt kill everyone
       
      zkStateReader.updateClusterState();
      zkStateReader.updateLiveNodes();
       assertTrue(zkStateReader.getClusterState().getLiveNodes().size() > 0);
       
       
diff --git a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyShardSplitTest.java b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyShardSplitTest.java
index 7a44561d38b..190db573a50 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyShardSplitTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyShardSplitTest.java
@@ -206,7 +206,7 @@ public class ChaosMonkeyShardSplitTest extends ShardSplitTest {
     for (int i = 0; i < 30; i++) {
       Thread.sleep(3000);
       ZkStateReader zkStateReader = cloudClient.getZkStateReader();
      zkStateReader.updateClusterState();
      zkStateReader.forceUpdateCollection("collection1");
       ClusterState clusterState = zkStateReader.getClusterState();
       DocCollection collection1 = clusterState.getCollection("collection1");
       Slice slice = collection1.getSlice("shard1");
diff --git a/solr/core/src/test/org/apache/solr/cloud/CollectionReloadTest.java b/solr/core/src/test/org/apache/solr/cloud/CollectionReloadTest.java
index b6eb5e2a494..65ff78bf06a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CollectionReloadTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CollectionReloadTest.java
@@ -103,7 +103,7 @@ public class CollectionReloadTest extends AbstractFullDistribZkTestBase {
     timeout = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeoutSecs, TimeUnit.SECONDS);
     while (System.nanoTime() < timeout) {
       // state of leader should be active after session loss recovery - see SOLR-7338
      cloudClient.getZkStateReader().updateClusterState();
      cloudClient.getZkStateReader().forceUpdateCollection(testCollectionName);
       ClusterState cs = cloudClient.getZkStateReader().getClusterState();
       Slice slice = cs.getSlice(testCollectionName, shardId);
       replicaState = slice.getReplica(leader.getName()).getStr(ZkStateReader.STATE_PROP);
diff --git a/solr/core/src/test/org/apache/solr/cloud/CollectionTooManyReplicasTest.java b/solr/core/src/test/org/apache/solr/cloud/CollectionTooManyReplicasTest.java
index 92fea45e335..afc7c483fb0 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CollectionTooManyReplicasTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CollectionTooManyReplicasTest.java
@@ -97,7 +97,7 @@ public class CollectionTooManyReplicasTest extends AbstractFullDistribZkTestBase
     assertEquals(0, response.getStatus());
 
     ZkStateReader zkStateReader = getCommonCloudSolrClient().getZkStateReader();
    zkStateReader.updateClusterState();
    zkStateReader.forceUpdateCollection(collectionName);
     Slice slice = zkStateReader.getClusterState().getSlicesMap(collectionName).get("shard1");
 
     Replica rep = null;
@@ -194,7 +194,7 @@ public class CollectionTooManyReplicasTest extends AbstractFullDistribZkTestBase
     // And finally, insure that there are all the replcias we expect. We should have shards 1, 2 and 4 and each
     // should have exactly two replicas
     ZkStateReader zkStateReader = getCommonCloudSolrClient().getZkStateReader();
    zkStateReader.updateClusterState();
    zkStateReader.forceUpdateCollection(collectionName);
     Map<String, Slice> slices = zkStateReader.getClusterState().getSlicesMap(collectionName);
     assertEquals("There should be exaclty four slices", slices.size(), 4);
     assertNotNull("shardstart should exist", slices.get("shardstart"));
@@ -275,7 +275,7 @@ public class CollectionTooManyReplicasTest extends AbstractFullDistribZkTestBase
 
   private List<String> getAllNodeNames(String collectionName) throws KeeperException, InterruptedException {
     ZkStateReader zkStateReader = getCommonCloudSolrClient().getZkStateReader();
    zkStateReader.updateClusterState();
    zkStateReader.forceUpdateCollection(collectionName);
     Slice slice = zkStateReader.getClusterState().getSlicesMap(collectionName).get("shard1");
 
     List<String> nodes = new ArrayList<>();
diff --git a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
index 93f82acf20d..641dadfc236 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
@@ -368,7 +368,6 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
       }
       
       Thread.sleep(200);
      cloudClient.getZkStateReader().updateClusterState();
     }
 
     assertFalse("Still found collection that should be gone", cloudClient.getZkStateReader().getClusterState().hasCollection("halfdeletedcollection2"));
@@ -540,8 +539,6 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
   }
   
   private void testNoCollectionSpecified() throws Exception {
    
    cloudClient.getZkStateReader().updateClusterState();
     assertFalse(cloudClient.getZkStateReader().getClusterState().hasCollection("corewithnocollection"));
     assertFalse(cloudClient.getZkStateReader().getClusterState().hasCollection("corewithnocollection2"));
     
@@ -565,13 +562,13 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     makeRequest(getBaseUrl((HttpSolrClient) clients.get(1)), createCmd);
     
     // in both cases, the collection should have default to the core name
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection("corewithnocollection");
    cloudClient.getZkStateReader().forceUpdateCollection("corewithnocollection2");
     assertTrue(cloudClient.getZkStateReader().getClusterState().hasCollection("corewithnocollection"));
     assertTrue(cloudClient.getZkStateReader().getClusterState().hasCollection("corewithnocollection2"));
   }
 
   private void testNoConfigSetExist() throws Exception {
    cloudClient.getZkStateReader().updateClusterState();
     assertFalse(cloudClient.getZkStateReader().getClusterState().hasCollection("corewithnocollection3"));
 
     // try and create a SolrCore with no collection name
@@ -592,7 +589,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     assertTrue(gotExp);
     TimeUnit.MILLISECONDS.sleep(200);
     // in both cases, the collection should have default to the core name
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection("corewithnocollection3");
 
     Collection<Slice> slices = cloudClient.getZkStateReader().getClusterState().getActiveSlices("corewithnocollection3");
     int replicaCount = 0;
diff --git a/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java b/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java
index 081e96f4f08..0951b5d6fc0 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java
@@ -409,7 +409,6 @@ public class CustomCollectionTest extends AbstractFullDistribZkTestBase {
     int attempts = 0;
     while (true) {
       if (attempts > 30) fail("Not enough active replicas in the shard 'x'");
      zkStateReader.updateClusterState();
       attempts++;
       replicaCount = zkStateReader.getClusterState().getSlice(collectionName, "x").getReplicas().size();
       if (replicaCount >= 1) break;
diff --git a/solr/core/src/test/org/apache/solr/cloud/DeleteShardTest.java b/solr/core/src/test/org/apache/solr/cloud/DeleteShardTest.java
index 101bfb98c20..812fbe93218 100644
-- a/solr/core/src/test/org/apache/solr/cloud/DeleteShardTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/DeleteShardTest.java
@@ -96,7 +96,6 @@ public class DeleteShardTest extends AbstractFullDistribZkTestBase {
     ClusterState clusterState = zkStateReader.getClusterState();
     int counter = 10;
     while (counter-- > 0) {
      zkStateReader.updateClusterState();
       clusterState = zkStateReader.getClusterState();
       if (clusterState.getSlice("collection1", shard) == null) {
         break;
@@ -142,7 +141,6 @@ public class DeleteShardTest extends AbstractFullDistribZkTestBase {
     boolean transition = false;
 
     for (int counter = 10; counter > 0; counter--) {
      zkStateReader.updateClusterState();
       ClusterState clusterState = zkStateReader.getClusterState();
       State sliceState = clusterState.getSlice("collection1", slice).getState();
       if (sliceState == state) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/ForceLeaderTest.java b/solr/core/src/test/org/apache/solr/cloud/ForceLeaderTest.java
index c68fe9c3a26..a71c3e61413 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ForceLeaderTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ForceLeaderTest.java
@@ -89,7 +89,7 @@ public class ForceLeaderTest extends HttpPartitionTest {
 
       putNonLeadersIntoLIR(testCollectionName, SHARD1, zkController, leader, notLeaders);
 
      cloudClient.getZkStateReader().updateClusterState();
      cloudClient.getZkStateReader().forceUpdateCollection(testCollectionName);
       ClusterState clusterState = cloudClient.getZkStateReader().getClusterState();
       int numActiveReplicas = getNumberOfActiveReplicas(clusterState, testCollectionName, SHARD1);
       assertEquals("Expected only 0 active replica but found " + numActiveReplicas +
@@ -114,7 +114,7 @@ public class ForceLeaderTest extends HttpPartitionTest {
       // By now we have an active leader. Wait for recoveries to begin
       waitForRecoveriesToFinish(testCollectionName, cloudClient.getZkStateReader(), true);
 
      cloudClient.getZkStateReader().updateClusterState();
      cloudClient.getZkStateReader().forceUpdateCollection(testCollectionName);
       clusterState = cloudClient.getZkStateReader().getClusterState();
       log.info("After forcing leader: " + clusterState.getSlice(testCollectionName, SHARD1));
       // we have a leader
@@ -187,7 +187,7 @@ public class ForceLeaderTest extends HttpPartitionTest {
         setReplicaState(testCollectionName, SHARD1, rep, State.DOWN);
       }
 
      zkController.getZkStateReader().updateClusterState();
      zkController.getZkStateReader().forceUpdateCollection(testCollectionName);
       // Assert all replicas are down and that there is no leader
       assertEquals(0, getActiveOrRecoveringReplicas(testCollectionName, SHARD1).size());
 
@@ -224,7 +224,6 @@ public class ForceLeaderTest extends HttpPartitionTest {
     ClusterState clusterState = null;
     boolean transition = false;
     for (int counter = 10; counter > 0; counter--) {
      zkStateReader.updateClusterState();
       clusterState = zkStateReader.getClusterState();
       Replica newLeader = clusterState.getSlice(collection, slice).getLeader();
       if (newLeader == null) {
@@ -259,7 +258,6 @@ public class ForceLeaderTest extends HttpPartitionTest {
 
     Replica.State replicaState = null;
     for (int counter = 10; counter > 0; counter--) {
      zkStateReader.updateClusterState();
       ClusterState clusterState = zkStateReader.getClusterState();
       replicaState = clusterState.getSlice(collection, slice).getReplica(replica.getName()).getState();
       if (replicaState == state) {
@@ -355,7 +353,6 @@ public class ForceLeaderTest extends HttpPartitionTest {
       for (int j = 0; j < notLeaders.size(); j++)
         lirStates[j] = zkController.getLeaderInitiatedRecoveryState(collectionName, shard, notLeaders.get(j).getName());
 
      zkController.getZkStateReader().updateClusterState();
       ClusterState clusterState = zkController.getZkStateReader().getClusterState();
       boolean allDown = true;
       for (State lirState : lirStates)
@@ -391,7 +388,7 @@ public class ForceLeaderTest extends HttpPartitionTest {
     JettySolrRunner leaderJetty = getJettyOnPort(getReplicaPort(leader));
     leaderJetty.start();
     waitForRecoveriesToFinish(collection, cloudClient.getZkStateReader(), true);
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection(collection);
     ClusterState clusterState = cloudClient.getZkStateReader().getClusterState();
     log.info("After bringing back leader: " + clusterState.getSlice(collection, SHARD1));
     int numActiveReplicas = getNumberOfActiveReplicas(clusterState, collection, SHARD1);
diff --git a/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java b/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java
index 8fecc84045a..f1960aa952e 100644
-- a/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/HttpPartitionTest.java
@@ -215,7 +215,7 @@ public class HttpPartitionTest extends AbstractFullDistribZkTestBase {
 
     // Verify that the partitioned replica is DOWN
     ZkStateReader zkr = cloudClient.getZkStateReader();
    zkr.updateClusterState(); // force the state to be fresh
    zkr.forceUpdateCollection(testCollectionName);; // force the state to be fresh
     ClusterState cs = zkr.getClusterState();
     Collection<Slice> slices = cs.getActiveSlices(testCollectionName);
     Slice slice = slices.iterator().next();
@@ -645,18 +645,13 @@ public class HttpPartitionTest extends AbstractFullDistribZkTestBase {
     final RTimer timer = new RTimer();
 
     ZkStateReader zkr = cloudClient.getZkStateReader();
    zkr.updateClusterState(); // force the state to be fresh

    zkr.forceUpdateCollection(testCollectionName);
     ClusterState cs = zkr.getClusterState();
     Collection<Slice> slices = cs.getActiveSlices(testCollectionName);
     boolean allReplicasUp = false;
     long waitMs = 0L;
     long maxWaitMs = maxWaitSecs * 1000L;
     while (waitMs < maxWaitMs && !allReplicasUp) {
      // refresh state every 2 secs
      if (waitMs % 2000 == 0)
        cloudClient.getZkStateReader().updateClusterState();

       cs = cloudClient.getZkStateReader().getClusterState();
       assertNotNull(cs);
       Slice shard = cs.getSlice(testCollectionName, shardId);
diff --git a/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java b/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java
index 6fd7c534809..0436d5e874b 100644
-- a/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/LeaderFailoverAfterPartitionTest.java
@@ -159,8 +159,6 @@ public class LeaderFailoverAfterPartitionTest extends HttpPartitionTest {
     
     long timeout = System.nanoTime() + TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS);
     while (System.nanoTime() < timeout) {
      cloudClient.getZkStateReader().updateClusterState();

       List<Replica> activeReps = getActiveOrRecoveringReplicas(testCollectionName, "shard1");
       if (activeReps.size() >= 2) break;
       Thread.sleep(1000);
diff --git a/solr/core/src/test/org/apache/solr/cloud/LeaderInitiatedRecoveryOnCommitTest.java b/solr/core/src/test/org/apache/solr/cloud/LeaderInitiatedRecoveryOnCommitTest.java
index 8d2cc70c786..7d6c633f482 100644
-- a/solr/core/src/test/org/apache/solr/cloud/LeaderInitiatedRecoveryOnCommitTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/LeaderInitiatedRecoveryOnCommitTest.java
@@ -80,7 +80,7 @@ public class LeaderInitiatedRecoveryOnCommitTest extends BasicDistributedZkTest
 
     Thread.sleep(sleepMsBeforeHealPartition);
 
    cloudClient.getZkStateReader().updateClusterState(); // get the latest state
    cloudClient.getZkStateReader().forceUpdateCollection(testCollectionName); // get the latest state
     leader = cloudClient.getZkStateReader().getLeaderRetry(testCollectionName, "shard1");
     assertSame("Leader was not active", Replica.State.ACTIVE, leader.getState());
 
@@ -128,7 +128,7 @@ public class LeaderInitiatedRecoveryOnCommitTest extends BasicDistributedZkTest
     sendCommitWithRetry(replica);
     Thread.sleep(sleepMsBeforeHealPartition);
 
    cloudClient.getZkStateReader().updateClusterState(); // get the latest state
    cloudClient.getZkStateReader().forceUpdateCollection(testCollectionName); // get the latest state
     leader = cloudClient.getZkStateReader().getLeaderRetry(testCollectionName, "shard1");
     assertSame("Leader was not active", Replica.State.ACTIVE, leader.getState());
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/MigrateRouteKeyTest.java b/solr/core/src/test/org/apache/solr/cloud/MigrateRouteKeyTest.java
index f9566e30f08..c09e0d1dc6a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/MigrateRouteKeyTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/MigrateRouteKeyTest.java
@@ -72,7 +72,7 @@ public class MigrateRouteKeyTest extends BasicDistributedZkTest {
     boolean ruleRemoved = false;
     long expiryTime = finishTime + TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS);
     while (System.nanoTime() < expiryTime) {
      getCommonCloudSolrClient().getZkStateReader().updateClusterState();
      getCommonCloudSolrClient().getZkStateReader().forceUpdateCollection(AbstractDistribZkTestBase.DEFAULT_COLLECTION);
       state = getCommonCloudSolrClient().getZkStateReader().getClusterState();
       slice = state.getSlice(AbstractDistribZkTestBase.DEFAULT_COLLECTION, SHARD2);
       Map<String,RoutingRule> routingRules = slice.getRoutingRules();
@@ -186,7 +186,7 @@ public class MigrateRouteKeyTest extends BasicDistributedZkTest {
       log.info("Response from target collection: " + response);
       assertEquals("DocCount on target collection does not match", splitKeyCount[0], response.getResults().getNumFound());
 
      getCommonCloudSolrClient().getZkStateReader().updateClusterState();
      getCommonCloudSolrClient().getZkStateReader().forceUpdateCollection(AbstractDistribZkTestBase.DEFAULT_COLLECTION);
       ClusterState state = getCommonCloudSolrClient().getZkStateReader().getClusterState();
       Slice slice = state.getSlice(AbstractDistribZkTestBase.DEFAULT_COLLECTION, SHARD2);
       assertNotNull("Routing rule map is null", slice.getRoutingRules());
diff --git a/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java b/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java
index 66a214f7fcb..85a88ec3ae9 100644
-- a/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java
@@ -439,7 +439,6 @@ public class OverseerTest extends SolrTestCaseJ4 {
       int cloudStateSliceCount = 0;
       for (int i = 0; i < 40; i++) {
         cloudStateSliceCount = 0;
        reader.updateClusterState();
         ClusterState state = reader.getClusterState();
         final Map<String,Slice> slices = state.getSlicesMap(collection);
         if (slices != null) {
@@ -524,7 +523,6 @@ public class OverseerTest extends SolrTestCaseJ4 {
   private void waitForCollections(ZkStateReader stateReader, String... collections) throws InterruptedException, KeeperException {
     int maxIterations = 100;
     while (0 < maxIterations--) {
      stateReader.updateClusterState();
       final ClusterState state = stateReader.getClusterState();
       Set<String> availableCollections = state.getCollections();
       int availableCount = 0;
@@ -605,7 +603,6 @@ public class OverseerTest extends SolrTestCaseJ4 {
   private void verifyShardLeader(ZkStateReader reader, String collection, String shard, String expectedCore) throws InterruptedException, KeeperException {
     int maxIterations = 200;
     while(maxIterations-->0) {
      reader.updateClusterState(); // poll state
       ZkNodeProps props =  reader.getClusterState().getLeader(collection, shard);
       if(props!=null) {
         if(expectedCore.equals(props.getStr(ZkStateReader.CORE_NAME_PROP))) {
@@ -832,7 +829,8 @@ public class OverseerTest extends SolrTestCaseJ4 {
       killerThread = new Thread(killer);
       killerThread.start();
 
      reader = new ZkStateReader(controllerClient); //no watches, we'll poll
      reader = new ZkStateReader(controllerClient);
      reader.createClusterStateWatchersAndUpdate();
 
       for (int i = 0; i < atLeast(4); i++) {
         killCounter.incrementAndGet(); //for each round allow 1 kill
@@ -905,9 +903,10 @@ public class OverseerTest extends SolrTestCaseJ4 {
       mockController = new MockZKController(server.getZkAddress(), "node1");
       mockController.publishState(collection, "core1", "core_node1", Replica.State.RECOVERING, 1);
 
      while (version == getClusterStateVersion(controllerClient));
      while (version == reader.getClusterState().getZkClusterStateVersion()) {
        Thread.sleep(100);
      }
       
      reader.updateClusterState();
       ClusterState state = reader.getClusterState();
       
       int numFound = 0;
@@ -1048,7 +1047,6 @@ public class OverseerTest extends SolrTestCaseJ4 {
         assertTrue(overseers.size() > 0);
 
         while (true)  {
          reader.updateClusterState();
           ClusterState state = reader.getClusterState();
           if (state.hasCollection("perf_sentinel")) {
             break;
diff --git a/solr/core/src/test/org/apache/solr/cloud/ReplicaPropertiesBase.java b/solr/core/src/test/org/apache/solr/cloud/ReplicaPropertiesBase.java
index 8347af09372..fe83a8431a3 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ReplicaPropertiesBase.java
++ b/solr/core/src/test/org/apache/solr/cloud/ReplicaPropertiesBase.java
@@ -56,7 +56,6 @@ public abstract class ReplicaPropertiesBase extends AbstractFullDistribZkTestBas
     ClusterState clusterState = null;
     Replica replica = null;
     for (int idx = 0; idx < 300; ++idx) {
      client.getZkStateReader().updateClusterState();
       clusterState = client.getZkStateReader().getClusterState();
       replica = clusterState.getReplica(collectionName, replicaName);
       if (replica == null) {
@@ -82,7 +81,6 @@ public abstract class ReplicaPropertiesBase extends AbstractFullDistribZkTestBas
     ClusterState clusterState = null;
 
     for (int idx = 0; idx < 300; ++idx) { // Keep trying while Overseer writes the ZK state for up to 30 seconds.
      client.getZkStateReader().updateClusterState();
       clusterState = client.getZkStateReader().getClusterState();
       replica = clusterState.getReplica(collectionName, replicaName);
       if (replica == null) {
@@ -116,7 +114,6 @@ public abstract class ReplicaPropertiesBase extends AbstractFullDistribZkTestBas
 
     DocCollection col = null;
     for (int idx = 0; idx < 300; ++idx) {
      client.getZkStateReader().updateClusterState();
       ClusterState clusterState = client.getZkStateReader().getClusterState();
 
       col = clusterState.getCollection(collectionName);
diff --git a/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java b/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java
index 22735abdb25..6d4b9cc3b8f 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ShardSplitTest.java
@@ -416,7 +416,6 @@ public class ShardSplitTest extends BasicDistributedZkTest {
     int i = 0;
     for (i = 0; i < 10; i++) {
       ZkStateReader zkStateReader = cloudClient.getZkStateReader();
      zkStateReader.updateClusterState();
       clusterState = zkStateReader.getClusterState();
       slice1_0 = clusterState.getSlice(AbstractDistribZkTestBase.DEFAULT_COLLECTION, "shard1_0");
       slice1_1 = clusterState.getSlice(AbstractDistribZkTestBase.DEFAULT_COLLECTION, "shard1_1");
diff --git a/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java b/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java
index e753be9f459..362009e684b 100644
-- a/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java
@@ -218,7 +218,6 @@ public class SyncSliceTest extends AbstractFullDistribZkTestBase {
     for (int i = 0; i < 60; i++) { 
       Thread.sleep(3000);
       ZkStateReader zkStateReader = cloudClient.getZkStateReader();
      zkStateReader.updateClusterState();
       ClusterState clusterState = zkStateReader.getClusterState();
       DocCollection collection1 = clusterState.getCollection("collection1");
       Slice slice = collection1.getSlice("shard1");
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java b/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
index a0bb42a3ee6..f4436eb9e65 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
@@ -119,7 +119,6 @@ public class TestCloudDeleteByQuery extends SolrCloudTestCase {
       String nodeKey = jettyURL.getHost() + ":" + jettyURL.getPort() + jettyURL.getPath().replace("/","_");
       urlMap.put(nodeKey, jettyURL.toString());
     }
    zkStateReader.updateClusterState();
     ClusterState clusterState = zkStateReader.getClusterState();
     for (Slice slice : clusterState.getSlices(COLLECTION_NAME)) {
       String shardName = slice.getName();
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestCollectionAPI.java b/solr/core/src/test/org/apache/solr/cloud/TestCollectionAPI.java
index b203f02c877..45b6f733bec 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestCollectionAPI.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestCollectionAPI.java
@@ -625,7 +625,7 @@ public class TestCollectionAPI extends ReplicaPropertiesBase {
           .setCollectionName("testClusterStateMigration")
           .process(client);
 
      client.getZkStateReader().updateClusterState();
      client.getZkStateReader().forceUpdateCollection("testClusterStateMigration");
 
       assertEquals(2, client.getZkStateReader().getClusterState().getCollection("testClusterStateMigration").getStateFormat());
 
@@ -735,7 +735,7 @@ public class TestCollectionAPI extends ReplicaPropertiesBase {
   private Map<String, String> getProps(CloudSolrClient client, String collectionName, String replicaName, String... props)
       throws KeeperException, InterruptedException {
 
    client.getZkStateReader().updateClusterState();
    client.getZkStateReader().forceUpdateCollection(collectionName);
     ClusterState clusterState = client.getZkStateReader().getClusterState();
     Replica replica = clusterState.getReplica(collectionName, replicaName);
     if (replica == null) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestLeaderInitiatedRecoveryThread.java b/solr/core/src/test/org/apache/solr/cloud/TestLeaderInitiatedRecoveryThread.java
index f2c58cf808a..11858f828b7 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestLeaderInitiatedRecoveryThread.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestLeaderInitiatedRecoveryThread.java
@@ -175,7 +175,6 @@ public class TestLeaderInitiatedRecoveryThread extends AbstractFullDistribZkTest
 
     timeOut = new TimeOut(30, TimeUnit.SECONDS);
     while (!timeOut.hasTimedOut()) {
      cloudClient.getZkStateReader().updateClusterState();
       Replica r = cloudClient.getZkStateReader().getClusterState().getReplica(DEFAULT_COLLECTION, replica.getName());
       if (r.getState() == Replica.State.DOWN) {
         break;
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java
index 9be89190d42..880051b1f83 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java
@@ -176,7 +176,7 @@ public class TestMiniSolrCloudCluster extends LuceneTestCase {
       assertEquals(1, rsp.getResults().getNumFound());
 
       // remove a server not hosting any replicas
      zkStateReader.updateClusterState();
      zkStateReader.forceUpdateCollection(collectionName);
       ClusterState clusterState = zkStateReader.getClusterState();
       HashMap<String, JettySolrRunner> jettyMap = new HashMap<String, JettySolrRunner>();
       for (JettySolrRunner jetty : miniCluster.getJettySolrRunners()) {
@@ -321,7 +321,8 @@ public class TestMiniSolrCloudCluster extends LuceneTestCase {
       try (SolrZkClient zkClient = new SolrZkClient
           (miniCluster.getZkServer().getZkAddress(), AbstractZkTestCase.TIMEOUT, AbstractZkTestCase.TIMEOUT, null);
           ZkStateReader zkStateReader = new ZkStateReader(zkClient)) {
        
        zkStateReader.createClusterStateWatchersAndUpdate();

         // wait for collection to appear
         AbstractDistribZkTestBase.waitForRecoveriesToFinish(collectionName, zkStateReader, true, true, 330);
 
@@ -368,6 +369,7 @@ public class TestMiniSolrCloudCluster extends LuceneTestCase {
       try (SolrZkClient zkClient = new SolrZkClient
           (miniCluster.getZkServer().getZkAddress(), AbstractZkTestCase.TIMEOUT, AbstractZkTestCase.TIMEOUT, null);
           ZkStateReader zkStateReader = new ZkStateReader(zkClient)) {
        zkStateReader.createClusterStateWatchersAndUpdate();
         AbstractDistribZkTestBase.waitForRecoveriesToFinish(collectionName, zkStateReader, true, true, 330);
 
         // modify collection
@@ -385,7 +387,7 @@ public class TestMiniSolrCloudCluster extends LuceneTestCase {
         }
 
         // the test itself
        zkStateReader.updateClusterState();
        zkStateReader.forceUpdateCollection(collectionName);
         final ClusterState clusterState = zkStateReader.getClusterState();
 
         final HashSet<Integer> leaderIndices = new HashSet<Integer>();
@@ -444,7 +446,7 @@ public class TestMiniSolrCloudCluster extends LuceneTestCase {
         }
         AbstractDistribZkTestBase.waitForRecoveriesToFinish(collectionName, zkStateReader, true, true, 330);
 
        zkStateReader.updateClusterState();
        zkStateReader.forceUpdateCollection(collectionName);
 
         // re-query collection
         {
@@ -489,32 +491,29 @@ public class TestMiniSolrCloudCluster extends LuceneTestCase {
         }
       }
 
      try (SolrZkClient zkClient = new SolrZkClient
          (miniCluster.getZkServer().getZkAddress(), AbstractZkTestCase.TIMEOUT, 45000, null);
          ZkStateReader zkStateReader = new ZkStateReader(zkClient)) {
        AbstractDistribZkTestBase.waitForRecoveriesToFinish(collectionName, zkStateReader, true, true, 330);
      ZkStateReader zkStateReader = cloudSolrClient.getZkStateReader();
      AbstractDistribZkTestBase.waitForRecoveriesToFinish(collectionName, zkStateReader, true, true, 330);
 
        // add some documents, then optimize to get merged-sorted segments
        tstes.addDocuments(cloudSolrClient, 10, 10, true);
      // add some documents, then optimize to get merged-sorted segments
      tstes.addDocuments(cloudSolrClient, 10, 10, true);
 
        // CommonParams.SEGMENT_TERMINATE_EARLY parameter intentionally absent
        tstes.queryTimestampDescending(cloudSolrClient);
      // CommonParams.SEGMENT_TERMINATE_EARLY parameter intentionally absent
      tstes.queryTimestampDescending(cloudSolrClient);
 
        // add a few more documents, but don't optimize to have some not-merge-sorted segments
        tstes.addDocuments(cloudSolrClient, 2, 10, false);
      // add a few more documents, but don't optimize to have some not-merge-sorted segments
      tstes.addDocuments(cloudSolrClient, 2, 10, false);
 
        // CommonParams.SEGMENT_TERMINATE_EARLY parameter now present
        tstes.queryTimestampDescendingSegmentTerminateEarlyYes(cloudSolrClient);
        tstes.queryTimestampDescendingSegmentTerminateEarlyNo(cloudSolrClient);
      // CommonParams.SEGMENT_TERMINATE_EARLY parameter now present
      tstes.queryTimestampDescendingSegmentTerminateEarlyYes(cloudSolrClient);
      tstes.queryTimestampDescendingSegmentTerminateEarlyNo(cloudSolrClient);
 
        // CommonParams.SEGMENT_TERMINATE_EARLY parameter present but it won't be used
        tstes.queryTimestampDescendingSegmentTerminateEarlyYesGrouped(cloudSolrClient);
        tstes.queryTimestampAscendingSegmentTerminateEarlyYes(cloudSolrClient); // uses a sort order that is _not_ compatible with the merge sort order
      // CommonParams.SEGMENT_TERMINATE_EARLY parameter present but it won't be used
      tstes.queryTimestampDescendingSegmentTerminateEarlyYesGrouped(cloudSolrClient);
      tstes.queryTimestampAscendingSegmentTerminateEarlyYes(cloudSolrClient); // uses a sort order that is _not_ compatible with the merge sort order
 
        // delete the collection we created earlier
        miniCluster.deleteCollection(collectionName);
        AbstractDistribZkTestBase.waitForCollectionToDisappear(collectionName, zkStateReader, true, true, 330);
      }
      // delete the collection we created earlier
      miniCluster.deleteCollection(collectionName);
      AbstractDistribZkTestBase.waitForCollectionToDisappear(collectionName, zkStateReader, true, true, 330);
     }
     finally {
       miniCluster.shutdown();
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java
index 54b21dff5de..18285617d9a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudClusterBase.java
@@ -146,6 +146,7 @@ public class TestMiniSolrCloudClusterBase extends LuceneTestCase {
       try (SolrZkClient zkClient = new SolrZkClient
           (miniCluster.getZkServer().getZkAddress(), AbstractZkTestCase.TIMEOUT, AbstractZkTestCase.TIMEOUT, null);
            ZkStateReader zkStateReader = new ZkStateReader(zkClient)) {
        zkStateReader.createClusterStateWatchersAndUpdate();
         AbstractDistribZkTestBase.waitForRecoveriesToFinish(collectionName, zkStateReader, true, true, 330);
 
         // modify/query collection
@@ -160,7 +161,7 @@ public class TestMiniSolrCloudClusterBase extends LuceneTestCase {
         assertEquals(1, rsp.getResults().getNumFound());
 
         // remove a server not hosting any replicas
        zkStateReader.updateClusterState();
        zkStateReader.forceUpdateCollection(collectionName);
         ClusterState clusterState = zkStateReader.getClusterState();
         HashMap<String, JettySolrRunner> jettyMap = new HashMap<String, JettySolrRunner>();
         for (JettySolrRunner jetty : miniCluster.getJettySolrRunners()) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java b/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
index 25ffe842569..256774d08c3 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
@@ -88,7 +88,7 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
     waitForRecoveriesToFinish("a1x2", true);
     waitForRecoveriesToFinish("b1x1", true);
 
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection("b1x1");
 
     ClusterState clusterState = cloudClient.getZkStateReader().getClusterState();
     DocCollection b1x1 = clusterState.getCollection("b1x1");
@@ -137,7 +137,7 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
 
     waitForRecoveriesToFinish("football", true);
 
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection("football");
 
     Replica leader = null;
     Replica notLeader = null;
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestRebalanceLeaders.java b/solr/core/src/test/org/apache/solr/cloud/TestRebalanceLeaders.java
index 3c720bfeddf..9208229976a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestRebalanceLeaders.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestRebalanceLeaders.java
@@ -310,7 +310,6 @@ public class TestRebalanceLeaders extends AbstractFullDistribZkTestBase {
     TimeOut timeout = new TimeOut(timeoutMs, TimeUnit.MILLISECONDS);
     while (! timeout.hasTimedOut()) {
       goAgain = false;
      cloudClient.getZkStateReader().updateClusterState();
       Map<String, Slice> slices = cloudClient.getZkStateReader().getClusterState().getCollection(COLLECTION_NAME).getSlicesMap();
 
       for (Map.Entry<String, Replica> ent : expected.entrySet()) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestReplicaProperties.java b/solr/core/src/test/org/apache/solr/cloud/TestReplicaProperties.java
index 5cc15e2ba36..fc2a7e25740 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestReplicaProperties.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestReplicaProperties.java
@@ -192,7 +192,6 @@ public class TestReplicaProperties extends ReplicaPropertiesBase {
     String lastFailMsg = "";
     for (int idx = 0; idx < 300; ++idx) { // Keep trying while Overseer writes the ZK state for up to 30 seconds.
       lastFailMsg = "";
      client.getZkStateReader().updateClusterState();
       ClusterState clusterState = client.getZkStateReader().getClusterState();
       for (Slice slice : clusterState.getSlices(collectionName)) {
         Boolean foundLeader = false;
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java b/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java
index 4d3ee30ad69..f4dc97de95b 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestSolrCloudWithKerberosAlt.java
@@ -205,6 +205,7 @@ public class TestSolrCloudWithKerberosAlt extends LuceneTestCase {
       try (SolrZkClient zkClient = new SolrZkClient
           (miniCluster.getZkServer().getZkAddress(), AbstractZkTestCase.TIMEOUT, AbstractZkTestCase.TIMEOUT, null);
            ZkStateReader zkStateReader = new ZkStateReader(zkClient)) {
        zkStateReader.createClusterStateWatchersAndUpdate();
         AbstractDistribZkTestBase.waitForRecoveriesToFinish(collectionName, zkStateReader, true, true, 330);
 
         // modify/query collection
diff --git a/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java
index dd337fb8530..7d53feebf73 100644
-- a/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java
@@ -187,7 +187,7 @@ public class UnloadDistributedZkTest extends BasicDistributedZkTest {
     }
     ZkStateReader zkStateReader = getCommonCloudSolrClient().getZkStateReader();
     
    zkStateReader.updateClusterState();
    zkStateReader.forceUpdateCollection("unloadcollection");
 
     int slices = zkStateReader.getClusterState().getCollection("unloadcollection").getSlices().size();
     assertEquals(1, slices);
@@ -203,7 +203,7 @@ public class UnloadDistributedZkTest extends BasicDistributedZkTest {
       createCmd.setDataDir(getDataDir(core2dataDir));
       adminClient.request(createCmd);
     }
    zkStateReader.updateClusterState();
    zkStateReader.forceUpdateCollection("unloadcollection");
     slices = zkStateReader.getClusterState().getCollection("unloadcollection").getSlices().size();
     assertEquals(1, slices);
     
diff --git a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
index cffbb543e49..7b293ca5ea6 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
@@ -296,7 +296,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
         byte[] bytes = Utils.toJSON(state);
         zkController.getZkClient().makePath(ZkStateReader.getCollectionPath("testPublishAndWaitForDownStates"), bytes, CreateMode.PERSISTENT, true);
 
        zkController.getZkStateReader().updateClusterState();
        zkController.getZkStateReader().forceUpdateCollection("testPublishAndWaitForDownStates");
         assertTrue(zkController.getZkStateReader().getClusterState().hasCollection("testPublishAndWaitForDownStates"));
         assertNotNull(zkController.getZkStateReader().getClusterState().getCollection("testPublishAndWaitForDownStates"));
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java
index 445c4b8f615..601f4fe723a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java
@@ -154,7 +154,7 @@ public class StressHdfsTest extends BasicDistributedZkTest {
 
     waitForRecoveriesToFinish(DELETE_DATA_DIR_COLLECTION, false);
     cloudClient.setDefaultCollection(DELETE_DATA_DIR_COLLECTION);
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection(DELETE_DATA_DIR_COLLECTION);
     
     for (int i = 1; i < nShards + 1; i++) {
       cloudClient.getZkStateReader().getLeaderRetry(DELETE_DATA_DIR_COLLECTION, "shard" + i, 30000);
@@ -211,7 +211,6 @@ public class StressHdfsTest extends BasicDistributedZkTest {
       }
       
       Thread.sleep(200);
      cloudClient.getZkStateReader().updateClusterState();
     }
     
     // check that all dirs are gone
diff --git a/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java b/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java
index 69626b0828b..10cc46c5165 100644
-- a/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateReaderTest.java
@@ -94,7 +94,7 @@ public class ZkStateReaderTest extends SolrTestCaseJ4 {
         assertFalse(exists);
 
         if (explicitRefresh) {
          reader.updateClusterState();
          reader.forceUpdateCollection("c1");
         } else {
           for (int i = 0; i < 100; ++i) {
             if (reader.getClusterState().hasCollection("c1")) {
@@ -122,7 +122,7 @@ public class ZkStateReaderTest extends SolrTestCaseJ4 {
         assertTrue(exists);
 
         if (explicitRefresh) {
          reader.updateClusterState();
          reader.forceUpdateCollection("c1");
         } else {
           for (int i = 0; i < 100; ++i) {
             if (reader.getClusterState().getCollection("c1").getStateFormat() == 2) {
@@ -167,7 +167,7 @@ public class ZkStateReaderTest extends SolrTestCaseJ4 {
           new DocCollection("c1", new HashMap<String, Slice>(), new HashMap<String, Object>(), DocRouter.DEFAULT, 0, ZkStateReader.COLLECTIONS_ZKNODE + "/c1/state.json"));
       writer.enqueueUpdate(reader.getClusterState(), c1, null);
       writer.writePendingUpdates();
      reader.updateClusterState();
      reader.forceUpdateCollection("c1");
 
       assertTrue(reader.getClusterState().getCollectionRef("c1").isLazilyLoaded());
       reader.addCollectionWatch("c1");
diff --git a/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateWriterTest.java b/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateWriterTest.java
index 8e7b0098121..f5648bf148c 100644
-- a/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateWriterTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/overseer/ZkStateWriterTest.java
@@ -233,7 +233,8 @@ public class ZkStateWriterTest extends SolrTestCaseJ4 {
       writer.enqueueUpdate(reader.getClusterState(), c1, null);
       writer.writePendingUpdates();
 
      reader.updateClusterState();
      reader.forceUpdateCollection("c1");
      reader.forceUpdateCollection("c2");
       ClusterState clusterState = reader.getClusterState(); // keep a reference to the current cluster state object
       assertTrue(clusterState.hasCollection("c1"));
       assertFalse(clusterState.hasCollection("c2"));
@@ -257,7 +258,6 @@ public class ZkStateWriterTest extends SolrTestCaseJ4 {
         // expected
       }
 
      reader.updateClusterState();
       try {
         writer.enqueueUpdate(reader.getClusterState(), c2, null);
         fail("enqueueUpdate after BadVersionException should not have suceeded");
@@ -317,7 +317,7 @@ public class ZkStateWriterTest extends SolrTestCaseJ4 {
       zkClient.setData(ZkStateReader.getCollectionPath("c2"), data, true);
 
       // get the most up-to-date state
      reader.updateClusterState();
      reader.forceUpdateCollection("c2");
       state = reader.getClusterState();
       assertTrue(state.hasCollection("c2"));
       assertEquals(sharedClusterStateVersion, (int) state.getZkClusterStateVersion());
@@ -328,7 +328,7 @@ public class ZkStateWriterTest extends SolrTestCaseJ4 {
       assertTrue(writer.hasPendingUpdates());
 
       // get the most up-to-date state
      reader.updateClusterState();
      reader.forceUpdateCollection("c2");
       state = reader.getClusterState();
 
       // enqueue a stateFormat=1 collection which should cause a flush
@@ -336,7 +336,7 @@ public class ZkStateWriterTest extends SolrTestCaseJ4 {
           new DocCollection("c1", new HashMap<String, Slice>(), new HashMap<String, Object>(), DocRouter.DEFAULT, 0, ZkStateReader.CLUSTER_STATE));
 
       try {
        state = writer.enqueueUpdate(state, c1, null);
        writer.enqueueUpdate(state, c1, null);
         fail("Enqueue should not have succeeded");
       } catch (KeeperException.BadVersionException bve) {
         // expected
diff --git a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
index 3dbc6d2876d..308b3e000a5 100644
-- a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
++ b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
@@ -226,7 +226,10 @@ public class ZkStateReader implements Closeable {
 
   /**
    * Forcibly refresh cluster state from ZK. Do this only to avoid race conditions because it's expensive.
   *
   * @deprecated Don't call this, call {@link #forceUpdateCollection(String)} on a single collection if you must.
    */
  @Deprecated
   public void updateClusterState() throws KeeperException, InterruptedException {
     synchronized (getUpdateLock()) {
       if (clusterState == null) {
@@ -248,6 +251,49 @@ public class ZkStateReader implements Closeable {
     }
   }
 
  /**
   * Forcibly refresh a collection's internal state from ZK. Try to avoid having to resort to this when
   * a better design is possible.
   */
  public void forceUpdateCollection(String collection) throws KeeperException, InterruptedException {
    synchronized (getUpdateLock()) {
      if (clusterState == null) {
        return;
      }

      ClusterState.CollectionRef ref = clusterState.getCollectionRef(collection);
      if (ref == null) {
        // We don't know anything about this collection, maybe it's new?
        // First try to update the legacy cluster state.
        refreshLegacyClusterState(null);
        if (!legacyCollectionStates.containsKey(collection)) {
          // No dice, see if a new collection just got created.
          LazyCollectionRef tryLazyCollection = new LazyCollectionRef(collection);
          if (tryLazyCollection.get() == null) {
            // No dice, just give up.
            return;
          }
          // What do you know, it exists!
          lazyCollectionStates.putIfAbsent(collection, tryLazyCollection);
        }
      } else if (ref.isLazilyLoaded()) {
        if (ref.get() != null) {
          return;
        }
        // Edge case: if there's no external collection, try refreshing legacy cluster state in case it's there.
        refreshLegacyClusterState(null);
      } else if (legacyCollectionStates.containsKey(collection)) {
        // Exists, and lives in legacy cluster state, force a refresh.
        refreshLegacyClusterState(null);
      } else if (watchedCollectionStates.containsKey(collection)) {
        // Exists as a watched collection, force a refresh.
        DocCollection newState = fetchCollectionState(collection, null);
        updateWatchedCollection(collection, newState);
      }
      constructState();
    }
  }

   /** Refresh the set of live nodes. */
   public void updateLiveNodes() throws KeeperException, InterruptedException {
     refreshLiveNodes(null);
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractDistribZkTestBase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractDistribZkTestBase.java
index ff423826080..7b3617ba86c 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractDistribZkTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractDistribZkTestBase.java
@@ -145,7 +145,6 @@ public abstract class AbstractDistribZkTestBase extends BaseDistributedSearchTes
     while (cont) {
       if (verbose) System.out.println("-");
       boolean sawLiveRecovering = false;
      zkStateReader.updateClusterState();
       ClusterState clusterState = zkStateReader.getClusterState();
       Map<String,Slice> slices = clusterState.getSlicesMap(collection);
       assertNotNull("Could not find collection:" + collection, slices);
@@ -195,7 +194,6 @@ public abstract class AbstractDistribZkTestBase extends BaseDistributedSearchTes
     
     while (cont) {
       if (verbose) System.out.println("-");
      zkStateReader.updateClusterState();
       ClusterState clusterState = zkStateReader.getClusterState();
       if (!clusterState.hasCollection(collection)) break;
       if (cnt == timeoutSeconds) {
@@ -239,7 +237,7 @@ public abstract class AbstractDistribZkTestBase extends BaseDistributedSearchTes
   protected void assertAllActive(String collection,ZkStateReader zkStateReader)
       throws KeeperException, InterruptedException {
 
      zkStateReader.updateClusterState();
      zkStateReader.forceUpdateCollection(collection);
       ClusterState clusterState = zkStateReader.getClusterState();
       Map<String,Slice> slices = clusterState.getSlicesMap(collection);
       if (slices == null) {
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
index bf8f643656b..a584dbd450b 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
@@ -626,7 +626,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
   
   protected void updateMappingsFromZk(List<JettySolrRunner> jettys, List<SolrClient> clients, boolean allowOverSharding) throws Exception {
     ZkStateReader zkStateReader = cloudClient.getZkStateReader();
    zkStateReader.updateClusterState();
    zkStateReader.forceUpdateCollection(DEFAULT_COLLECTION);
     cloudJettys.clear();
     shardToJetty.clear();
 
@@ -1814,7 +1814,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
     Map<String,Replica> notLeaders = new HashMap<>();
 
     ZkStateReader zkr = cloudClient.getZkStateReader();
    zkr.updateClusterState(); // force the state to be fresh
    zkr.forceUpdateCollection(testCollectionName); // force the state to be fresh
 
     ClusterState cs = zkr.getClusterState();
     Collection<Slice> slices = cs.getActiveSlices(testCollectionName);
@@ -1824,10 +1824,6 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
     long maxWaitMs = maxWaitSecs * 1000L;
     Replica leader = null;
     while (waitMs < maxWaitMs && !allReplicasUp) {
      // refresh state every 2 secs
      if (waitMs % 2000 == 0)
        cloudClient.getZkStateReader().updateClusterState();

       cs = cloudClient.getZkStateReader().getClusterState();
       assertNotNull(cs);
       Slice shard = cs.getSlice(testCollectionName, shardId);
@@ -1879,7 +1875,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
   }
 
   protected String printClusterStateInfo(String collection) throws Exception {
    cloudClient.getZkStateReader().updateClusterState();
    cloudClient.getZkStateReader().forceUpdateCollection(collection);
     String cs = null;
     ClusterState clusterState = cloudClient.getZkStateReader().getClusterState();
     if (collection != null) {
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/ChaosMonkey.java b/solr/test-framework/src/java/org/apache/solr/cloud/ChaosMonkey.java
index d13d62f0683..511fdf34b15 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/ChaosMonkey.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/ChaosMonkey.java
@@ -425,7 +425,7 @@ public class ChaosMonkey {
     for (CloudJettyRunner cloudJetty : shardToJetty.get(slice)) {
       
       // get latest cloud state
      zkStateReader.updateClusterState();
      zkStateReader.forceUpdateCollection(collection);
       
       Slice theShards = zkStateReader.getClusterState().getSlicesMap(collection)
           .get(slice);
@@ -447,18 +447,6 @@ public class ChaosMonkey {
     return numActive;
   }
   
  public SolrClient getRandomClient(String slice) throws KeeperException, InterruptedException {
    // get latest cloud state
    zkStateReader.updateClusterState();

    // get random shard
    List<SolrClient> clients = shardToClient.get(slice);
    int index = LuceneTestCase.random().nextInt(clients.size() - 1);
    SolrClient client = clients.get(index);

    return client;
  }
  
   // synchronously starts and stops shards randomly, unless there is only one
   // active shard up for a slice or if there is one active and others recovering
   public void startTheMonkey(boolean killLeaders, final int roundPauseUpperLimit) {
- 
2.19.1.windows.1

