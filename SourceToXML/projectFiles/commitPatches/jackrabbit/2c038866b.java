From 2c038866b1371dca26c513625d22c3c01e251bfb Mon Sep 17 00:00:00 2001
From: Stefan Guggisberg <stefan@apache.org>
Date: Tue, 3 Jan 2012 16:55:19 +0000
Subject: [PATCH] JCR-3194: ConcurrentModificationException in CacheManager.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1226863 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/cache/CacheManager.java     | 7 ++++++-
 1 file changed, 6 insertions(+), 1 deletion(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
index 4b53f505e..9073645a1 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
@@ -161,7 +161,12 @@ public class CacheManager implements CacheAccessListener {
             if (now < nextLogStats) {
                 return;
             }
            for (Cache cache : caches.keySet()) {
            // JCR-3194 avoid ConcurrentModificationException
            List<Cache> list = new ArrayList<Cache>();
            synchronized (caches) {
                list.addAll(caches.keySet());
            }
            for (Cache cache : list) {
                 log.debug(cache.getCacheInfoAsString());
             }
             nextLogStats = now + minLogStatsInterval;
- 
2.19.1.windows.1

