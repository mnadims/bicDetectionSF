From a7f879db7682841539468f9af71f8b28423d9321 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Mon, 2 Dec 2013 07:25:58 +0000
Subject: [PATCH] SOLR-5510

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1546922 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/cloud/ZkController.java   | 25 ++++++++++---------
 1 file changed, 13 insertions(+), 12 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/cloud/ZkController.java b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
index a4aac548ddc..d291eb9ba55 100644
-- a/solr/core/src/java/org/apache/solr/cloud/ZkController.java
++ b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
@@ -1333,30 +1333,31 @@ public final class ZkController {
   public void preRegister(CoreDescriptor cd ) {
 
     String coreNodeName = getCoreNodeName(cd);

    // make sure the node name is set on the descriptor
    if (cd.getCloudDescriptor().getCoreNodeName() == null) {
      cd.getCloudDescriptor().setCoreNodeName(coreNodeName);
    }

     // before becoming available, make sure we are not live and active
     // this also gets us our assigned shard id if it was not specified
     try {
      if(cd.getCloudDescriptor().getCollectionName() !=null && cd.getCloudDescriptor().getCoreNodeName() != null ) {
      CloudDescriptor cloudDesc = cd.getCloudDescriptor();
      if(cd.getCloudDescriptor().getCollectionName() !=null && cloudDesc.getCoreNodeName() != null ) {
         //we were already registered
        if(zkStateReader.getClusterState().hasCollection(cd.getCloudDescriptor().getCollectionName())){
        DocCollection coll = zkStateReader.getClusterState().getCollection(cd.getCloudDescriptor().getCollectionName());
        if(zkStateReader.getClusterState().hasCollection(cloudDesc.getCollectionName())){
        DocCollection coll = zkStateReader.getClusterState().getCollection(cloudDesc.getCollectionName());
          if(!"true".equals(coll.getStr("autoCreated"))){
           Slice slice = coll.getSlice(cd.getCloudDescriptor().getShardId());
           Slice slice = coll.getSlice(cloudDesc.getShardId());
            if(slice != null){
             if(slice.getReplica(cd.getCloudDescriptor().getCoreNodeName()) == null) {
             if(slice.getReplica(cloudDesc.getCoreNodeName()) == null) {
                log.info("core_removed This core is removed from ZK");
               throw new SolrException(ErrorCode.NOT_FOUND,coreNodeName +" is removed");
               throw new SolrException(ErrorCode.NOT_FOUND,cloudDesc.getCoreNodeName() +" is removed");
              }
            }
          }
         }
       }

      // make sure the node name is set on the descriptor
      if (cloudDesc.getCoreNodeName() == null) {
        cloudDesc.setCoreNodeName(coreNodeName);
      }

       publish(cd, ZkStateReader.DOWN, false);
     } catch (KeeperException e) {
       log.error("", e);
- 
2.19.1.windows.1

