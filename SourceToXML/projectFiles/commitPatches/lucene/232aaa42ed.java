From 232aaa42ed3d136672881d1bd1563aa29945fdbf Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Wed, 19 Dec 2012 16:41:18 +0000
Subject: [PATCH] SOLR-4218: SolrTestCaseJ4 throws NPE when closing the core
 (on the afterClass method)

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1423932 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solr/handler/component/HttpShardHandlerFactory.java   | 8 ++++++--
 1 file changed, 6 insertions(+), 2 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
index 32b173be2fc..9410f959a87 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
++ b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
@@ -162,12 +162,16 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements Plug
     }
     
     try {
      defaultClient.getConnectionManager().shutdown();
      if(defaultClient != null) {
        defaultClient.getConnectionManager().shutdown();
      }
     } catch (Throwable e) {
       SolrException.log(log, e);
     }
     try {
      loadbalancer.shutdown();
      if(loadbalancer != null) {
        loadbalancer.shutdown();
      }
     } catch (Throwable e) {
       SolrException.log(log, e);
     }
- 
2.19.1.windows.1

