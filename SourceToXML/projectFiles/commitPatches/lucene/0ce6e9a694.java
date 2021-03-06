From 0ce6e9a6949fd2ffee8ac56309557433e64f5268 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Tue, 29 Oct 2013 08:37:02 +0000
Subject: [PATCH] SOLR-5311 - Avoid registering replicas which are removed ,
 SOLR-5310 -Add a collection admin command to remove a replica

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1536606 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   6 +-
 .../apache/solr/cloud/CloudDescriptor.java    |   6 +-
 .../java/org/apache/solr/cloud/Overseer.java  |  38 ++--
 .../cloud/OverseerCollectionProcessor.java    |  82 +++++++++
 .../org/apache/solr/cloud/ZkController.java   |  17 +-
 .../org/apache/solr/core/CoreDescriptor.java  |   2 +-
 .../handler/admin/CollectionsHandler.java     |  23 ++-
 .../org/apache/solr/TestRandomDVFaceting.java |   2 +-
 .../CollectionsAPIDistributedZkTest.java      |   4 +-
 .../solr/cloud/CustomCollectionTest.java      |  49 -----
 .../solr/cloud/DeleteInactiveReplicaTest.java |  91 ++++++++++
 .../apache/solr/cloud/DeleteReplicaTest.java  | 171 ++++++++++++++++++
 .../org/apache/solr/core/TestLazyCores.java   |   2 +-
 .../solr/common/cloud/ClusterState.java       |   3 +
 .../solr/common/cloud/ZkStateReader.java      |   1 +
 .../solr/common/params/CollectionParams.java  |   2 +-
 .../cloud/AbstractFullDistribZkTestBase.java  |  47 +++++
 17 files changed, 471 insertions(+), 75 deletions(-)
 create mode 100644 solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java
 create mode 100644 solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index fe6d44d5533..aec0205cc73 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -107,7 +107,11 @@ New Features
   implementations indicating that they should not be removed in later stages
   of distributed updates (usually signalled by the update.distrib parameter)
   (yonik)
  

 * SOLR-5310: Add a collection admin command to remove a replica (noble)

 * SOLR-5311: Avoid registering replicas which are removed (noble)

 
 Bug Fixes
 ----------------------
diff --git a/solr/core/src/java/org/apache/solr/cloud/CloudDescriptor.java b/solr/core/src/java/org/apache/solr/cloud/CloudDescriptor.java
index fbf02514e6c..e19ae117adc 100644
-- a/solr/core/src/java/org/apache/solr/cloud/CloudDescriptor.java
++ b/solr/core/src/java/org/apache/solr/cloud/CloudDescriptor.java
@@ -27,6 +27,7 @@ import java.util.Properties;
 
 public class CloudDescriptor {
 
  private final CoreDescriptor cd;
   private String shardId;
   private String collectionName;
   private SolrParams params;
@@ -48,7 +49,8 @@ public class CloudDescriptor {
   public static final String SHARD_RANGE = "shardRange";
   public static final String SHARD_PARENT = "shardParent";
 
  public CloudDescriptor(String coreName, Properties props) {
  public CloudDescriptor(String coreName, Properties props, CoreDescriptor cd) {
    this.cd = cd;
     this.shardId = props.getProperty(CoreDescriptor.CORE_SHARD, null);
     // If no collection name is specified, we default to the core name
     this.collectionName = props.getProperty(CoreDescriptor.CORE_COLLECTION, coreName);
@@ -120,6 +122,8 @@ public class CloudDescriptor {
 
   public void setCoreNodeName(String nodeName) {
     this.nodeName = nodeName;
    if(nodeName==null) cd.getPersistableStandardProperties().remove(CoreDescriptor.CORE_NODE_NAME);
    else cd.getPersistableStandardProperties().setProperty(CoreDescriptor.CORE_NODE_NAME, nodeName);
   }
 
   public String getShardRange() {
diff --git a/solr/core/src/java/org/apache/solr/cloud/Overseer.java b/solr/core/src/java/org/apache/solr/cloud/Overseer.java
index 695cd7b2861..30e5f21824b 100644
-- a/solr/core/src/java/org/apache/solr/cloud/Overseer.java
++ b/solr/core/src/java/org/apache/solr/cloud/Overseer.java
@@ -59,7 +59,7 @@ public class Overseer {
   private static Logger log = LoggerFactory.getLogger(Overseer.class);
   
   static enum LeaderStatus { DONT_KNOW, NO, YES };
  

   private class ClusterStateUpdater implements Runnable, ClosableThread {
     
     private final ZkStateReader reader;
@@ -329,6 +329,20 @@ public class Overseer {
         final String collection = message.getStr(ZkStateReader.COLLECTION_PROP);
         assert collection.length() > 0 : message;
         

        Integer numShards = message.getInt(ZkStateReader.NUM_SHARDS_PROP, null);
        log.info("Update state numShards={} message={}", numShards, message);

        List<String> shardNames  = new ArrayList<String>();

        //collection does not yet exist, create placeholders if num shards is specified
        boolean collectionExists = state.hasCollection(collection);
        if (!collectionExists && numShards!=null) {
          getShardNames(numShards, shardNames);
          state = createCollection(state, collection, shardNames, message);
        }
        String sliceName = message.getStr(ZkStateReader.SHARD_ID_PROP);

         String coreNodeName = message.getStr(ZkStateReader.CORE_NODE_NAME_PROP);
         if (coreNodeName == null) {
           coreNodeName = getAssignedCoreNodeName(state, message);
@@ -339,21 +353,18 @@ public class Overseer {
             coreNodeName = Assign.assignNode(collection, state);
           }
           message.getProperties().put(ZkStateReader.CORE_NODE_NAME_PROP, coreNodeName);
        }
        Integer numShards = message.getInt(ZkStateReader.NUM_SHARDS_PROP, null);
        log.info("Update state numShards={} message={}", numShards, message);

        List<String> shardNames  = new ArrayList<String>();
        } else {
          //probably, this core was removed explicitly
          if (sliceName !=null && collectionExists &&  !"true".equals(state.getCollection(collection).getStr("autoCreated"))) {
            Slice slice = state.getSlice(collection, sliceName);
            if (slice.getReplica(coreNodeName) == null) {
              log.info("core_deleted . Just return");
              return state;
            }
          }
 
        //collection does not yet exist, create placeholders if num shards is specified
        boolean collectionExists = state.getCollections().contains(collection);
        if (!collectionExists && numShards!=null) {
          getShardNames(numShards, shardNames);
          state = createCollection(state, collection, shardNames, message);
         }
        
         // use the provided non null shardId
        String sliceName = message.getStr(ZkStateReader.SHARD_ID_PROP);
         if (sliceName == null) {
           //get shardId from ClusterState
           sliceName = getAssignedId(state, coreNodeName, message);
@@ -541,6 +552,7 @@ public class Overseer {
         }
         collectionProps.put(DocCollection.DOC_ROUTER, routerSpec);
 
        if(message.getStr("fromApi") == null) collectionProps.put("autoCreated","true");
         DocCollection newCollection = new DocCollection(collectionName, newSlices, collectionProps, router);
 
         newCollections.put(collectionName, newCollection);
diff --git a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java
index 77246c82a74..cfa1790ba73 100644
-- a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java
++ b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java
@@ -44,6 +44,7 @@ import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.cloud.ZooKeeperException;
 import org.apache.solr.common.params.CoreAdminParams;
 import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.params.MapSolrParams;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.UpdateParams;
 import org.apache.solr.common.util.NamedList;
@@ -59,6 +60,7 @@ import org.slf4j.LoggerFactory;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
@@ -68,6 +70,7 @@ import java.util.Set;
 import static org.apache.solr.cloud.Assign.Node;
 import static org.apache.solr.cloud.Assign.getNodesForNewShard;
 import static org.apache.solr.common.cloud.ZkStateReader.COLLECTION_PROP;
import static org.apache.solr.common.cloud.ZkStateReader.REPLICA_PROP;
 import static org.apache.solr.common.cloud.ZkStateReader.SHARD_ID_PROP;
 
 
@@ -101,6 +104,8 @@ public class OverseerCollectionProcessor implements Runnable, ClosableThread {
 
   public static final String CREATESHARD = "createshard";
 
  public static final String DELETEREPLICA = "deletereplica";

   public static final String COLL_CONF = "collection.configName";
 
 
@@ -236,6 +241,8 @@ public class OverseerCollectionProcessor implements Runnable, ClosableThread {
         createShard(zkStateReader.getClusterState(), message, results);
       } else if (DELETESHARD.equals(operation)) {
         deleteShard(zkStateReader.getClusterState(), message, results);
      } else if (DELETEREPLICA.equals(operation)) {
        deleteReplica(zkStateReader.getClusterState(), message, results);
       } else {
         throw new SolrException(ErrorCode.BAD_REQUEST, "Unknown operation:"
             + operation);
@@ -254,6 +261,81 @@ public class OverseerCollectionProcessor implements Runnable, ClosableThread {
     return new OverseerSolrResponse(results);
   }
 
  private void deleteReplica(ClusterState clusterState, ZkNodeProps message, NamedList results) throws KeeperException, InterruptedException {
    checkRequired(message, COLLECTION_PROP, SHARD_ID_PROP,REPLICA_PROP);
    String collectionName = message.getStr(COLLECTION_PROP);
    String shard = message.getStr(SHARD_ID_PROP);
    String replicaName = message.getStr(REPLICA_PROP);
    DocCollection coll = clusterState.getCollection(collectionName);
    Slice slice = coll.getSlice(shard);
    if(slice==null){
      throw new SolrException(ErrorCode.BAD_REQUEST, "Invalid shard name : "+shard+" in collection : "+ collectionName);
    }
    Replica replica = slice.getReplica(replicaName);
    if(replica == null){
      throw new SolrException(ErrorCode.BAD_REQUEST, "Invalid shard name : "+shard+" in collection : "+ collectionName);
    }

    String baseUrl = replica.getStr(ZkStateReader.BASE_URL_PROP);
    String core = replica.getStr(ZkStateReader.CORE_NAME_PROP);
    //assume the core exists and try to unload it
    if (!Slice.ACTIVE.equals(replica.getStr(Slice.STATE))) {
      deleteCoreNode(collectionName, replicaName, replica, core);
      if(waitForCoreNodeGone(collectionName, shard, replicaName)) return;
    } else {
    Map m = ZkNodeProps.makeMap("qt", adminPath,
        CoreAdminParams.ACTION, CoreAdminAction.UNLOAD.toString(),
        CoreAdminParams.CORE, core) ;

      ShardRequest sreq = new ShardRequest();
      sreq.purpose = 1;
      if (baseUrl.startsWith("http://")) baseUrl = baseUrl.substring(7);
      sreq.shards = new String[]{baseUrl};
      sreq.actualShards = sreq.shards;
      sreq.params = new ModifiableSolrParams(new MapSolrParams(m) );
      try {
        shardHandler.submit(sreq, baseUrl, sreq.params);
      } catch (Exception e) {
        log.info("Exception trying to unload core "+sreq,e);
      }
      if (waitForCoreNodeGone(collectionName, shard, replicaName)) return;//check if the core unload removed the corenode zk enry
      deleteCoreNode(collectionName, replicaName, replica, core); // this could be because the core is gone but not updated in ZK yet (race condition)
      if(waitForCoreNodeGone(collectionName, shard, replicaName)) return;

    }
    throw new SolrException(ErrorCode.SERVER_ERROR, "Could not  remove replica : "+collectionName+"/"+shard+"/"+replicaName);
  }

  private boolean waitForCoreNodeGone(String collectionName, String shard, String replicaName) throws InterruptedException {
    long waitUntil = System.currentTimeMillis() + 30000;
    boolean deleted = false;
    while (System.currentTimeMillis() < waitUntil) {
      Thread.sleep(100);
      deleted = zkStateReader.getClusterState().getCollection(collectionName).getSlice(shard).getReplica(replicaName) == null;
      if (deleted) break;
    }
    return deleted;
  }

  private void deleteCoreNode(String collectionName, String replicaName, Replica replica, String core) throws KeeperException, InterruptedException {
    ZkNodeProps m = new ZkNodeProps(
        Overseer.QUEUE_OPERATION, Overseer.DELETECORE,
        ZkStateReader.CORE_NAME_PROP, core,
        ZkStateReader.NODE_NAME_PROP, replica.getStr(ZkStateReader.NODE_NAME_PROP),
        ZkStateReader.COLLECTION_PROP, collectionName,
        ZkStateReader.CORE_NODE_NAME_PROP, replicaName);
    Overseer.getInQueue(zkStateReader.getZkClient()).offer(ZkStateReader.toJSON(m));
  }

  private void checkRequired(ZkNodeProps message, String... props) {
    for (String prop : props) {
      if(message.get(prop) == null){
        throw new SolrException(ErrorCode.BAD_REQUEST, StrUtils.join(Arrays.asList(props),',') +" are required params" );
      }
    }

  }

   private void deleteCollection(ZkNodeProps message, NamedList results)
       throws KeeperException, InterruptedException {
     String collection = message.getStr("name");
diff --git a/solr/core/src/java/org/apache/solr/cloud/ZkController.java b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
index f56aa62fe02..d32f37db637 100644
-- a/solr/core/src/java/org/apache/solr/cloud/ZkController.java
++ b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
@@ -1356,7 +1356,7 @@ public final class ZkController {
   }
 
   public void preRegister(CoreDescriptor cd ) {
    

     String coreNodeName = getCoreNodeName(cd);
 
     // make sure the node name is set on the descriptor
@@ -1367,6 +1367,21 @@ public final class ZkController {
     // before becoming available, make sure we are not live and active
     // this also gets us our assigned shard id if it was not specified
     try {
      if(cd.getCloudDescriptor().getCollectionName() !=null && cd.getCloudDescriptor().getCoreNodeName() != null ) {
        //we were already registered
        if(zkStateReader.getClusterState().hasCollection(cd.getCloudDescriptor().getCollectionName())){
        DocCollection coll = zkStateReader.getClusterState().getCollection(cd.getCloudDescriptor().getCollectionName());
         if(!"true".equals(coll.getStr("autoCreated"))){
           Slice slice = coll.getSlice(cd.getCloudDescriptor().getShardId());
           if(slice != null){
             if(slice.getReplica(cd.getCloudDescriptor().getCoreNodeName()) == null) {
               log.info("core_removed This core is removed from ZK");
               throw new SolrException(ErrorCode.NOT_FOUND,coreNodeName +" is removed");
             }
           }
         }
        }
      }
       publish(cd, ZkStateReader.DOWN, false);
     } catch (KeeperException e) {
       log.error("", e);
diff --git a/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java b/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java
index b85fb78ddab..85f3d9ae378 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java
++ b/solr/core/src/java/org/apache/solr/core/CoreDescriptor.java
@@ -183,7 +183,7 @@ public class CoreDescriptor {
 
     // TODO maybe make this a CloudCoreDescriptor subclass?
     if (container.isZooKeeperAware()) {
      cloudDesc = new CloudDescriptor(name, coreProperties);
      cloudDesc = new CloudDescriptor(name, coreProperties, this);
       if (params != null) {
         cloudDesc.setParams(params);
       }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
index f8b8420bf62..843397d14eb 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
@@ -56,6 +56,7 @@ import static org.apache.solr.cloud.Overseer.QUEUE_OPERATION;
 import static org.apache.solr.cloud.OverseerCollectionProcessor.COLL_CONF;
 import static org.apache.solr.cloud.OverseerCollectionProcessor.CREATESHARD;
 import static org.apache.solr.cloud.OverseerCollectionProcessor.CREATE_NODE_SET;
import static org.apache.solr.cloud.OverseerCollectionProcessor.DELETEREPLICA;
 import static org.apache.solr.cloud.OverseerCollectionProcessor.MAX_SHARDS_PER_NODE;
 import static org.apache.solr.cloud.OverseerCollectionProcessor.NUM_SLICES;
 import static org.apache.solr.cloud.OverseerCollectionProcessor.REPLICATION_FACTOR;
@@ -164,6 +165,10 @@ public class CollectionsHandler extends RequestHandlerBase {
         this.handleCreateShard(req, rsp);
         break;
       }
      case DELETEREPLICA: {
        this.handleRemoveReplica(req, rsp);
        break;
      }
 
       default: {
           throw new RuntimeException("Unknown action: " + action);
@@ -295,10 +300,10 @@ public class CollectionsHandler extends RequestHandlerBase {
           "Collection name is required to create a new collection");
     }
     
    Map<String,Object> props = new HashMap<String,Object>();
    props.put(Overseer.QUEUE_OPERATION,
        OverseerCollectionProcessor.CREATECOLLECTION);

    Map<String,Object> props = ZkNodeProps.makeMap(
        Overseer.QUEUE_OPERATION,
        OverseerCollectionProcessor.CREATECOLLECTION,
        "fromApi","true");
     copyIfNotNull(req.getParams(),props,
         "name",
         REPLICATION_FACTOR,
@@ -314,6 +319,16 @@ public class CollectionsHandler extends RequestHandlerBase {
     handleResponse(OverseerCollectionProcessor.CREATECOLLECTION, m, rsp);
   }
 
  private void handleRemoveReplica(SolrQueryRequest req, SolrQueryResponse rsp) throws KeeperException, InterruptedException {
    log.info("Remove replica: " + req.getParamString());
    req.getParams().required().check(COLLECTION_PROP, SHARD_ID_PROP, "replica");
    Map<String, Object> map = makeMap(QUEUE_OPERATION, DELETEREPLICA);
    copyIfNotNull(req.getParams(),map,COLLECTION_PROP,SHARD_ID_PROP,"replica");
    ZkNodeProps m = new ZkNodeProps(map);
    handleResponse(DELETEREPLICA, m, rsp);
  }


   private void handleCreateShard(SolrQueryRequest req, SolrQueryResponse rsp) throws KeeperException, InterruptedException {
     log.info("Create shard: " + req.getParamString());
     req.getParams().required().check(COLLECTION_PROP, SHARD_ID_PROP);
diff --git a/solr/core/src/test/org/apache/solr/TestRandomDVFaceting.java b/solr/core/src/test/org/apache/solr/TestRandomDVFaceting.java
index 19d945b0d4f..19ea8540e35 100644
-- a/solr/core/src/test/org/apache/solr/TestRandomDVFaceting.java
++ b/solr/core/src/test/org/apache/solr/TestRandomDVFaceting.java
@@ -220,7 +220,7 @@ public class TestRandomDVFaceting extends SolrTestCaseJ4 {
       for (String method : methods) {
         if (method.equals("dv")) {
           params.set("facet.field", "{!key="+facet_field+"}"+facet_field+"_dv");
          params.set("facet.method", null);
          params.set("facet.method",(String) null);
         } else {
           params.set("facet.field", facet_field);
           params.set("facet.method", method);
diff --git a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
index 795cdc22359..b0a059dae23 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
@@ -905,7 +905,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     throw new RuntimeException("Could not find a live node for collection:" + collection);
   }
 
  private void waitForNon403or404or503(HttpSolrServer collectionClient)
/*  private void waitForNon403or404or503(HttpSolrServer collectionClient)
       throws Exception {
     SolrException exp = null;
     long timeoutAt = System.currentTimeMillis() + 30000;
@@ -929,7 +929,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     }
 
     fail("Could not find the new collection - " + exp.code() + " : " + collectionClient.getBaseURL());
  }
  }*/
   
   private void checkForMissingCollection(String collectionName)
       throws Exception {
diff --git a/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java b/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java
index 95255566fb0..d96d6f90bde 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CustomCollectionTest.java
@@ -468,55 +468,6 @@ public class CustomCollectionTest extends AbstractFullDistribZkTestBase {
   }
 
 
  public static String getUrlFromZk(ClusterState clusterState, String collection) {
    Map<String,Slice> slices = clusterState.getCollectionStates().get(collection).getSlicesMap();

    if (slices == null) {
      throw new SolrException(ErrorCode.BAD_REQUEST, "Could not find collection:" + collection);
    }

    for (Map.Entry<String,Slice> entry : slices.entrySet()) {
      Slice slice = entry.getValue();
      Map<String,Replica> shards = slice.getReplicasMap();
      Set<Map.Entry<String,Replica>> shardEntries = shards.entrySet();
      for (Map.Entry<String,Replica> shardEntry : shardEntries) {
        final ZkNodeProps node = shardEntry.getValue();
        if (clusterState.liveNodesContain(node.getStr(ZkStateReader.NODE_NAME_PROP))) {
          return ZkCoreNodeProps.getCoreUrl(node.getStr(ZkStateReader.BASE_URL_PROP), collection); //new ZkCoreNodeProps(node).getCoreUrl();
        }
      }
    }

    throw new RuntimeException("Could not find a live node for collection:" + collection);
  }

  private void waitForNon403or404or503(HttpSolrServer collectionClient)
      throws Exception {
    SolrException exp = null;
    long timeoutAt = System.currentTimeMillis() + 30000;

    while (System.currentTimeMillis() < timeoutAt) {
      boolean missing = false;

      try {
        collectionClient.query(new SolrQuery("*:*"));
      } catch (SolrException e) {
        if (!(e.code() == 403 || e.code() == 503 || e.code() == 404)) {
          throw e;
        }
        exp = e;
        missing = true;
      }
      if (!missing) {
        return;
      }
      Thread.sleep(50);
    }

    fail("Could not find the new collection - " + exp.code() + " : " + collectionClient.getBaseURL());
  }


   @Override
   protected QueryResponse queryServer(ModifiableSolrParams params) throws SolrServerException {
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java b/solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java
new file mode 100644
index 00000000000..82bcffc8ddb
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/cloud/DeleteInactiveReplicaTest.java
@@ -0,0 +1,91 @@
package org.apache.solr.cloud;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.util.NamedList;

import java.util.Map;

import static org.apache.solr.common.cloud.ZkNodeProps.makeMap;

public class DeleteInactiveReplicaTest extends DeleteReplicaTest{
  @Override
  public void doTest() throws Exception {
    deleteInactiveReplicaTest();
  }

  private void deleteInactiveReplicaTest() throws Exception{
    String COLL_NAME = "delDeadColl";
    CloudSolrServer client = createCloudClient(null);
    createCloudClient(null);
    createColl(COLL_NAME, client);
    DocCollection testcoll = getCommonCloudSolrServer().getZkStateReader().getClusterState().getCollection(COLL_NAME);
    final Slice shard1 = testcoll.getSlices().iterator().next();
    if(!shard1.getState().equals(Slice.ACTIVE)) fail("shard is not active");
    Replica replica1 = shard1.getReplicas().iterator().next();
    boolean stopped = false;
    JettySolrRunner stoppedJetty = null;
    StringBuilder sb = new StringBuilder();
    for (JettySolrRunner jetty : jettys) {
      sb.append(jetty.getBaseUrl()).append(",");
      if( jetty.getBaseUrl().toString().startsWith(replica1.getStr(ZkStateReader.BASE_URL_PROP)) ) {
        stoppedJetty = jetty;
        ChaosMonkey.stop(jetty);
        stopped = true;
        break;
      }
    }
    if(!stopped){
      fail("Could not find jetty for replica "+ replica1 + "jettys: "+sb);
    }

    long endAt = System.currentTimeMillis()+3000;
    boolean success = false;
    while(System.currentTimeMillis() < endAt){
      testcoll = getCommonCloudSolrServer().getZkStateReader().getClusterState().getCollection(COLL_NAME);
      if(!"active".equals(testcoll.getSlice(shard1.getName()).getReplica(replica1.getName()).getStr(Slice.STATE))  ){
        success=true;
      }
      if(success) break;
      Thread.sleep(100);
    }
    log.info("removed_replicas {}/{} ",shard1.getName(),replica1.getName());
    removeAndWaitForReplicaGone(COLL_NAME, client, replica1, shard1.getName());
    client.shutdown();

    ChaosMonkey.start(stoppedJetty);
    log.info("restarted jetty");


    Map m = makeMap("qt","/admin/cores",
        "action", "status");

    NamedList<Object> resp = new HttpSolrServer(replica1.getStr("base_url")).request(new QueryRequest(new MapSolrParams(m)));
    assertNull( "The core is up and running again" , ((NamedList)resp.get("status")).get(replica1.getStr("core")));

  }
}
diff --git a/solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java b/solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java
new file mode 100644
index 00000000000..a6c692392b1
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/cloud/DeleteReplicaTest.java
@@ -0,0 +1,171 @@
package org.apache.solr.cloud;

import org.apache.lucene.util.Constants;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.SolrCmdDistributor;
import org.apache.solr.util.DefaultSolrThreadFactory;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.solr.cloud.OverseerCollectionProcessor.DELETEREPLICA;
import static org.apache.solr.cloud.OverseerCollectionProcessor.MAX_SHARDS_PER_NODE;
import static org.apache.solr.cloud.OverseerCollectionProcessor.NUM_SLICES;
import static org.apache.solr.cloud.OverseerCollectionProcessor.REPLICATION_FACTOR;
import static org.apache.solr.common.cloud.ZkNodeProps.makeMap;

public class DeleteReplicaTest extends AbstractFullDistribZkTestBase {
  private static final boolean DEBUG = false;

  ThreadPoolExecutor executor = new ThreadPoolExecutor(0,
      Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
      new DefaultSolrThreadFactory("testExecutor"));

  CompletionService<Object> completionService;
  Set<Future<Object>> pending;

  @BeforeClass
  public static void beforeThisClass2() throws Exception {
    assumeFalse("FIXME: This test fails under Java 8 all the time, see SOLR-4711", Constants.JRE_IS_MINIMUM_JAVA8);
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    System.setProperty("numShards", Integer.toString(sliceCount));
    System.setProperty("solr.xml.persist", "true");
  }

  protected String getSolrXml() {
    return "solr-no-core.xml";
  }


  public DeleteReplicaTest() {
    fixShardCount = true;

    sliceCount = 2;
    shardCount = 4;
    completionService = new ExecutorCompletionService<Object>(executor);
    pending = new HashSet<Future<Object>>();
    checkCreatedVsState = false;

  }

  @Override
  protected void setDistributedParams(ModifiableSolrParams params) {

    if (r.nextBoolean()) {
      // don't set shards, let that be figured out from the cloud state
    } else {
      // use shard ids rather than physical locations
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < shardCount; i++) {
        if (i > 0)
          sb.append(',');
        sb.append("shard" + (i + 3));
      }
      params.set("shards", sb.toString());
    }
  }


  @Override
  public void doTest() throws Exception {
    deleteLiveReplicaTest();
//    deleteInactiveReplicaTest();
//    super.printLayout();
  }






  private void deleteLiveReplicaTest() throws Exception{
    String COLL_NAME = "delLiveColl";
    CloudSolrServer client = createCloudClient(null);
    createColl(COLL_NAME, client);
    DocCollection testcoll = getCommonCloudSolrServer().getZkStateReader().getClusterState().getCollection(COLL_NAME);
    final Slice shard1 = testcoll.getSlices().iterator().next();
    if(!shard1.getState().equals(Slice.ACTIVE)) fail("shard is not active");
    Replica replica = shard1.getReplicas().iterator().next();

    removeAndWaitForReplicaGone(COLL_NAME, client, replica, shard1.getName());
    client.shutdown();


  }

  protected void removeAndWaitForReplicaGone(String COLL_NAME, CloudSolrServer client, Replica replica, String shard) throws SolrServerException, IOException, InterruptedException {
    Map m = makeMap("collection", COLL_NAME,
     "action", DELETEREPLICA,
    "shard",shard,
    "replica",replica.getName());
    SolrParams params = new MapSolrParams( m);
    SolrRequest request = new QueryRequest(params);
    request.setPath("/admin/collections");
    client.request(request);
    long endAt = System.currentTimeMillis()+3000;
    boolean success = false;
    DocCollection testcoll = null;
    while(System.currentTimeMillis() < endAt){
      testcoll = getCommonCloudSolrServer().getZkStateReader().getClusterState().getCollection(COLL_NAME);
      success = testcoll.getSlice(shard).getReplica(replica.getName()) == null;
      if(success) {
        log.info("replica cleaned up {}/{} core {}",shard+"/"+replica.getName(), replica.getStr("core"));
        log.info("current state {}", testcoll);
        break;
      }
      Thread.sleep(100);
    }
    assertTrue("Replica not cleaned up", success);
  }

  protected void createColl(String COLL_NAME, CloudSolrServer client) throws Exception {
    int replicationFactor = 2;
    int numShards = 2;
    int maxShardsPerNode = ((((numShards+1) * replicationFactor) / getCommonCloudSolrServer()
        .getZkStateReader().getClusterState().getLiveNodes().size())) + 1;

    Map<String, Object> props = makeMap(
        REPLICATION_FACTOR, replicationFactor,
        MAX_SHARDS_PER_NODE, maxShardsPerNode,
        NUM_SLICES, numShards);
    Map<String,List<Integer>> collectionInfos = new HashMap<String,List<Integer>>();
    createCollection(collectionInfos, COLL_NAME, props, client);
    Set<Map.Entry<String,List<Integer>>> collectionInfosEntrySet = collectionInfos.entrySet();
    for (Map.Entry<String,List<Integer>> entry : collectionInfosEntrySet) {
      String collection = entry.getKey();
      List<Integer> list = entry.getValue();
      checkForCollection(collection, list, null);
      String url = getUrlFromZk(getCommonCloudSolrServer().getZkStateReader().getClusterState(), collection);
      HttpSolrServer collectionClient = new HttpSolrServer(url);
      // poll for a second - it can take a moment before we are ready to serve
      waitForNon403or404or503(collectionClient);
    }
  }
}
diff --git a/solr/core/src/test/org/apache/solr/core/TestLazyCores.java b/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
index 64e99c0fba9..cdb8850b49d 100644
-- a/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
++ b/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
@@ -621,7 +621,7 @@ public class TestLazyCores extends SolrTestCaseJ4 {
   private void addLazy(SolrCore core, String... fieldValues) throws IOException {
     UpdateHandler updater = core.getUpdateHandler();
     AddUpdateCommand cmd = new AddUpdateCommand(makeReq(core));
    cmd.solrDoc = sdoc(fieldValues);
    cmd.solrDoc = sdoc((Object[])fieldValues);
     updater.addDoc(cmd);
   }
 
diff --git a/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java b/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java
index 131f8c8eb7f..93fb1264d0c 100644
-- a/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java
++ b/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java
@@ -98,6 +98,9 @@ public class ClusterState implements JSONWriter.Writable {
     }
     return null;
   }
  public boolean hasCollection(String coll){
    return collectionStates.get(coll)!=null;
  }
 
 
   /**
diff --git a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
index 86483bae821..86928150c0a 100644
-- a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
++ b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
@@ -57,6 +57,7 @@ public class ZkStateReader {
   public static final String CORE_NAME_PROP = "core";
   public static final String COLLECTION_PROP = "collection";
   public static final String SHARD_ID_PROP = "shard";
  public static final String REPLICA_PROP = "replica";
   public static final String SHARD_RANGE_PROP = "shard_range";
   public static final String SHARD_STATE_PROP = "shard_state";
   public static final String SHARD_PARENT_PROP = "shard_parent";
diff --git a/solr/solrj/src/java/org/apache/solr/common/params/CollectionParams.java b/solr/solrj/src/java/org/apache/solr/common/params/CollectionParams.java
index 49165e865b7..ceda5f1243d 100644
-- a/solr/solrj/src/java/org/apache/solr/common/params/CollectionParams.java
++ b/solr/solrj/src/java/org/apache/solr/common/params/CollectionParams.java
@@ -28,7 +28,7 @@ public interface CollectionParams
 
 
   public enum CollectionAction {
    CREATE, DELETE, RELOAD, SYNCSHARD, CREATEALIAS, DELETEALIAS, SPLITSHARD, DELETESHARD, CREATESHARD;
    CREATE, DELETE, RELOAD, SYNCSHARD, CREATEALIAS, DELETEALIAS, SPLITSHARD, DELETESHARD, CREATESHARD, DELETEREPLICA;
     
     public static CollectionAction get( String p )
     {
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
index f273e2746ba..e1c8cec7d26 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
@@ -1745,5 +1745,52 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
     }
     return commondCloudSolrServer;
   }
  public static String getUrlFromZk(ClusterState clusterState, String collection) {
    Map<String,Slice> slices = clusterState.getCollectionStates().get(collection).getSlicesMap();

    if (slices == null) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Could not find collection:" + collection);
    }

    for (Map.Entry<String,Slice> entry : slices.entrySet()) {
      Slice slice = entry.getValue();
      Map<String,Replica> shards = slice.getReplicasMap();
      Set<Map.Entry<String,Replica>> shardEntries = shards.entrySet();
      for (Map.Entry<String,Replica> shardEntry : shardEntries) {
        final ZkNodeProps node = shardEntry.getValue();
        if (clusterState.liveNodesContain(node.getStr(ZkStateReader.NODE_NAME_PROP))) {
          return ZkCoreNodeProps.getCoreUrl(node.getStr(ZkStateReader.BASE_URL_PROP), collection); //new ZkCoreNodeProps(node).getCoreUrl();
        }
      }
    }

    throw new RuntimeException("Could not find a live node for collection:" + collection);
  }

 public  static void waitForNon403or404or503(HttpSolrServer collectionClient)
      throws Exception {
    SolrException exp = null;
    long timeoutAt = System.currentTimeMillis() + 30000;

    while (System.currentTimeMillis() < timeoutAt) {
      boolean missing = false;

      try {
        collectionClient.query(new SolrQuery("*:*"));
      } catch (SolrException e) {
        if (!(e.code() == 403 || e.code() == 503 || e.code() == 404)) {
          throw e;
        }
        exp = e;
        missing = true;
      }
      if (!missing) {
        return;
      }
      Thread.sleep(50);
    }

    fail("Could not find the new collection - " + exp.code() + " : " + collectionClient.getBaseURL());
  }
 
 }
- 
2.19.1.windows.1

