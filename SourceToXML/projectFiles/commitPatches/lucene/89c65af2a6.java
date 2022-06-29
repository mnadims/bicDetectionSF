From 89c65af2a6e5f1c8216c1202f65e8d670ef14385 Mon Sep 17 00:00:00 2001
From: Scott Blum <dragonsinth@apache.org>
Date: Mon, 25 Apr 2016 21:15:02 -0400
Subject: [PATCH] SOLR-9029: fix rare ZkStateReader visibility race during
 collection state format update

--
 solr/CHANGES.txt                                         | 2 ++
 .../java/org/apache/solr/common/cloud/ZkStateReader.java | 9 +++------
 2 files changed, 5 insertions(+), 6 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 999bd737fd9..77029502213 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -150,6 +150,8 @@ Bug Fixes
 
 * SOLR-8992: Restore Schema API GET method functionality removed in 6.0 (noble, Steve Rowe)
 
* SOLR-9029: fix rare ZkStateReader visibility race during collection state format update (Scott Blum, hossman)

 Optimizations
 ----------------------
 * SOLR-8722: Don't force a full ZkStateReader refresh on every Overseer operation.
diff --git a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
index 568c791af4a..1e57d7eda41 100644
-- a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
++ b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
@@ -263,9 +263,9 @@ public class ZkStateReader implements Closeable {
       }
 
       ClusterState.CollectionRef ref = clusterState.getCollectionRef(collection);
      if (ref == null) {
        // We don't know anything about this collection, maybe it's new?
        // First try to update the legacy cluster state.
      if (ref == null || legacyCollectionStates.containsKey(collection)) {
        // We either don't know anything about this collection (maybe it's new?) or it's legacy.
        // First update the legacy cluster state.
         refreshLegacyClusterState(null);
         if (!legacyCollectionStates.containsKey(collection)) {
           // No dice, see if a new collection just got created.
@@ -283,9 +283,6 @@ public class ZkStateReader implements Closeable {
         }
         // Edge case: if there's no external collection, try refreshing legacy cluster state in case it's there.
         refreshLegacyClusterState(null);
      } else if (legacyCollectionStates.containsKey(collection)) {
        // Exists, and lives in legacy cluster state, force a refresh.
        refreshLegacyClusterState(null);
       } else if (watchedCollectionStates.containsKey(collection)) {
         // Exists as a watched collection, force a refresh.
         DocCollection newState = fetchCollectionState(collection, null);
- 
2.19.1.windows.1

