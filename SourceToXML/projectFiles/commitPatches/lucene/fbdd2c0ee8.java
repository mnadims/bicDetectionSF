From fbdd2c0ee88d7e52ede525ce9cd3024dbc2fea00 Mon Sep 17 00:00:00 2001
From: Tomas Fernandez Lobbe <tflobbe@apache.org>
Date: Mon, 8 May 2017 17:11:35 -0700
Subject: [PATCH] SOLR-10639: Fix NPE in LRU/LFU/FastLRU caches toString method

--
 solr/CHANGES.txt                                            | 2 ++
 solr/core/src/java/org/apache/solr/search/FastLRUCache.java | 2 +-
 solr/core/src/java/org/apache/solr/search/LFUCache.java     | 2 +-
 solr/core/src/java/org/apache/solr/search/LRUCache.java     | 2 +-
 4 files changed, 5 insertions(+), 3 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index de3ce0bb1a5..0c4eb3342ef 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -324,6 +324,8 @@ Bug Fixes
 * SOLR-10630: HttpSolrCall.getAuthCtx().new AuthorizationContext() {...}.getParams()
   sometimes throws java.lang.NullPointerException (hu xiaodong via shalin)
 
* SOLR-10639: Fix NPE on LRU/FastLRU/LFU toString method (Tomás Fernández Löbbe)

 Other Changes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/search/FastLRUCache.java b/solr/core/src/java/org/apache/solr/search/FastLRUCache.java
index cb699b25abc..1cf4443c912 100644
-- a/solr/core/src/java/org/apache/solr/search/FastLRUCache.java
++ b/solr/core/src/java/org/apache/solr/search/FastLRUCache.java
@@ -292,7 +292,7 @@ public class FastLRUCache<K, V> extends SolrCacheBase implements SolrCache<K,V>
 
   @Override
   public String toString() {
    return name() + cacheMap != null ? cacheMap.getValue().toString() : "";
    return name() + (cacheMap != null ? cacheMap.getValue().toString() : "");
   }
 
 }
diff --git a/solr/core/src/java/org/apache/solr/search/LFUCache.java b/solr/core/src/java/org/apache/solr/search/LFUCache.java
index 82ba6d26536..f502b03600b 100644
-- a/solr/core/src/java/org/apache/solr/search/LFUCache.java
++ b/solr/core/src/java/org/apache/solr/search/LFUCache.java
@@ -308,7 +308,7 @@ public class LFUCache<K, V> implements SolrCache<K, V> {
 
   @Override
   public String toString() {
    return name + cacheMap != null ? cacheMap.getValue().toString() : "";
    return name + (cacheMap != null ? cacheMap.getValue().toString() : "");
   }
 
 }
diff --git a/solr/core/src/java/org/apache/solr/search/LRUCache.java b/solr/core/src/java/org/apache/solr/search/LRUCache.java
index ce206fe2f7e..cbd3979155d 100644
-- a/solr/core/src/java/org/apache/solr/search/LRUCache.java
++ b/solr/core/src/java/org/apache/solr/search/LRUCache.java
@@ -375,7 +375,7 @@ public class LRUCache<K,V> extends SolrCacheBase implements SolrCache<K,V>, Acco
 
   @Override
   public String toString() {
    return name() + cacheMap != null ? cacheMap.getValue().toString() : "";
    return name() + (cacheMap != null ? cacheMap.getValue().toString() : "");
   }
 
   @Override
- 
2.19.1.windows.1

