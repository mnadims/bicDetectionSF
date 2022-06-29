From f33c2917e30ccf5f564d01d35c93d0e804260b4a Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Mon, 18 Mar 2013 04:49:48 +0000
Subject: [PATCH] SOLR-4604: SolrCore is not using the UpdateHandler that is
 passed to it in SolrCore#reload.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1457640 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                                      | 3 +++
 solr/core/src/java/org/apache/solr/core/SolrCore.java | 4 +---
 2 files changed, 4 insertions(+), 3 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 9b91e4efa0d..cca59d07100 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -157,6 +157,9 @@ Bug Fixes
 * SOLR-4601: A Collection that is only partially created and then deleted will 
   leave pre allocated shard information in ZooKeeper. (Mark Miller)
 
* SOLR-4604: SolrCore is not using the UpdateHandler that is passed to it in 
  SolrCore#reload. (Mark Miller)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index c82c10035ca..82e15c978f9 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -804,9 +804,7 @@ public final class SolrCore implements SolrInfoMBean {
         this.updateHandler = createUpdateHandler(updateHandlerClass == null ? DirectUpdateHandler2.class
             .getName() : updateHandlerClass);
       } else {
        this.updateHandler = createUpdateHandler(
            updateHandlerClass == null ? DirectUpdateHandler2.class.getName()
                : updateHandlerClass, updateHandler);
        this.updateHandler = updateHandler;
       }
       infoRegistry.put("updateHandler", this.updateHandler);
       
- 
2.19.1.windows.1

