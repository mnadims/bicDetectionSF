From be0bb552ee26c07c27a71d2da52f36ed16185076 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Fri, 1 Nov 2013 16:30:16 +0000
Subject: [PATCH] SOLR-5311, invalid error message

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1537978 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/cloud/OverseerCollectionProcessor.java     | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

diff --git a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java
index cfa1790ba73..76a9b1a2581 100644
-- a/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java
++ b/solr/core/src/java/org/apache/solr/cloud/OverseerCollectionProcessor.java
@@ -273,7 +273,8 @@ public class OverseerCollectionProcessor implements Runnable, ClosableThread {
     }
     Replica replica = slice.getReplica(replicaName);
     if(replica == null){
      throw new SolrException(ErrorCode.BAD_REQUEST, "Invalid shard name : "+shard+" in collection : "+ collectionName);
      throw new SolrException(ErrorCode.BAD_REQUEST, "Invalid replica : " + replicaName + " in shard/collection : "
          + shard + "/"+ collectionName);
     }
 
     String baseUrl = replica.getStr(ZkStateReader.BASE_URL_PROP);
- 
2.19.1.windows.1

