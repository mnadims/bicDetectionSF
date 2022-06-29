From 606069f50f4f41f741e9dd94ccec9c2f1d03722b Mon Sep 17 00:00:00 2001
From: Alexandru Parvulescu <alexparvulescu@apache.org>
Date: Thu, 13 Oct 2011 13:19:38 +0000
Subject: [PATCH] JCR-3098 Add hit miss statistics and logging to caches  -
 patch by Bart van der Schans, continued

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1182835 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/cache/AbstractCache.java  |  8 ++---
 .../jackrabbit/core/cache/CacheManager.java   |  5 +--
 .../AbstractBundlePersistenceManager.java     | 35 ++++++++++++++++++-
 3 files changed, 41 insertions(+), 7 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java
index f3433ad60..a9725627f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java
@@ -114,6 +114,7 @@ public abstract class AbstractCache implements Cache {
      * interval has passed since the previous listener call.
      */
     protected void recordCacheAccess() {
        totalAccessCount.incrementAndGet();
         long count = accessCount.incrementAndGet();
         if (count % ACCESS_INTERVAL == 0) {
             CacheAccessListener listener = accessListener.get();
@@ -121,7 +122,6 @@ public abstract class AbstractCache implements Cache {
                 listener.cacheAccessed(count);
             }
         }
        totalAccessCount.incrementAndGet();
     }
 
     protected void recordCacheMiss() {
@@ -186,13 +186,13 @@ public abstract class AbstractCache implements Cache {
         long u = getMemoryUsed() / 1024;
         long m = getMaxMemorySize() / 1024;
         StringBuilder c = new StringBuilder();
        c.append("Cache name=");
        c.append("cachename=");
         c.append(this.toString());
         c.append(", elements=");
         c.append(getElementCount());
        c.append(", used memory=");
        c.append(", usedmemorykb=");
         c.append(u);
        c.append(", max memory=");
        c.append(", maxmemorykb=");
         c.append(m);
         c.append(", access=");
         c.append(getTotalAccessCount());
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
index f6e7dfb43..247d3b5ad 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
@@ -156,17 +156,18 @@ public class CacheManager implements CacheAccessListener {
      * Log info about the caches.
      */
     private void logCacheStats() {
        if (log.isDebugEnabled()) {
        if (log.isInfoEnabled()) {
             long now = System.currentTimeMillis();
             if (now < nextLogStats) {
                 return;
             }
             for (Cache cache : caches.keySet()) {
                log.debug(cache.getCacheInfoAsString());
                log.info(cache.getCacheInfoAsString());
             }
             nextLogStats = now + minLogStatsInterval;
         }
     }

     /**
      * Re-calculate the maximum memory for each cache, and set the new limits.
      */
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
index 16ebe7583..613734706 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
@@ -28,6 +28,8 @@ import javax.jcr.PropertyType;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.core.cache.Cache;
import org.apache.jackrabbit.core.cache.CacheAccessListener;
 import org.apache.jackrabbit.core.cache.ConcurrentCache;
 import org.apache.jackrabbit.core.fs.FileSystemResource;
 import org.apache.jackrabbit.core.fs.FileSystem;
@@ -82,7 +84,7 @@ import org.apache.jackrabbit.spi.Name;
  * </ul>
  */
 public abstract class AbstractBundlePersistenceManager implements
    PersistenceManager, CachingPersistenceManager, IterablePersistenceManager {
    PersistenceManager, CachingPersistenceManager, IterablePersistenceManager, CacheAccessListener {
 
     /** the default logger */
     private static Logger log = LoggerFactory.getLogger(AbstractBundlePersistenceManager.class);
@@ -112,6 +114,18 @@ public abstract class AbstractBundlePersistenceManager implements
     /** the cache of loaded bundles */
     private ConcurrentCache<NodeId, NodePropBundle> bundles;
 
    /** The default minimum stats logging interval (in ms). */
    private static final int DEFAULT_LOG_STATS_INTERVAL = 60 * 1000;

    /** The minimum interval time between stats are logged */
    private long minLogStatsInterval = Long.getLong(
            "org.apache.jackrabbit.cacheLogStatsInterval",
            DEFAULT_LOG_STATS_INTERVAL);

    /** The last time the cache stats were logged. */
    private volatile long nextLogStats =
            System.currentTimeMillis() + DEFAULT_LOG_STATS_INTERVAL;

     /** the persistence manager context */
     protected PMContext context;
 
@@ -379,6 +393,7 @@ public abstract class AbstractBundlePersistenceManager implements
         // init bundle cache
         bundles = new ConcurrentCache<NodeId, NodePropBundle>(context.getHomeDir().getName() + "BundleCache");
         bundles.setMaxMemorySize(bundleCacheSize);
        bundles.setAccessListener(this);
     }
 
     /**
@@ -709,4 +724,22 @@ public abstract class AbstractBundlePersistenceManager implements
         bundles.remove(id);
     }
 
    public void cacheAccessed(long accessCount) {
        logCacheStats();
    }

    private void logCacheStats() {
        if (log.isInfoEnabled()) {
            long now = System.currentTimeMillis();
            if (now < nextLogStats) {
                return;
            }
            log.info(bundles.getCacheInfoAsString());
            nextLogStats = now + minLogStatsInterval;
        }
    }

    public void disposeCache(Cache cache) {
        // NOOP
    }
 }
- 
2.19.1.windows.1

