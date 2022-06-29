From 6475aa5219381162bfe175cb31f4092c78a21b83 Mon Sep 17 00:00:00 2001
From: Alexandru Parvulescu <alexparvulescu@apache.org>
Date: Thu, 13 Oct 2011 08:28:43 +0000
Subject: [PATCH] JCR-3098 Add hit miss statistics and logging to caches  -
 patch by Bart van der Schans

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1182713 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/cache/AbstractCache.java  | 56 ++++++++++++++++++-
 .../apache/jackrabbit/core/cache/Cache.java   | 30 +++++++++-
 .../core/cache/CacheAccessListener.java       |  5 +-
 .../jackrabbit/core/cache/CacheManager.java   | 44 ++++++++++++---
 .../core/cache/ConcurrentCache.java           | 25 +++++++--
 .../AbstractBundlePersistenceManager.java     |  2 +-
 .../core/state/MLRUItemStateCache.java        |  2 +-
 .../core/cache/ConcurrentCacheTest.java       |  2 +-
 8 files changed, 147 insertions(+), 19 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java
index c1f179e52..f3433ad60 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/AbstractCache.java
@@ -64,6 +64,22 @@ public abstract class AbstractCache implements Cache {
      */
     private final AtomicLong accessCount = new AtomicLong();
 
    /**
     * Cache access counter. Unike his counterpart {@link #accessCount}, this
     * does not get reset.
     * 
     * It is used in the cases where a cache listener needs to call
     * {@link Cache#resetAccessCount()}, but also needs a total access count. If
     * you are sure that nobody calls reset, you can just use
     * {@link #accessCount}.
     */
    private final AtomicLong totalAccessCount = new AtomicLong();

    /**
     * Cache miss counter.
     */
    private final AtomicLong missCount = new AtomicLong();

     /**
      * Cache access listener. Set in the
      * {@link #setAccessListener(CacheAccessListener)} method and accessed
@@ -102,9 +118,14 @@ public abstract class AbstractCache implements Cache {
         if (count % ACCESS_INTERVAL == 0) {
             CacheAccessListener listener = accessListener.get();
             if (listener != null) {
                listener.cacheAccessed();
                listener.cacheAccessed(count);
             }
         }
        totalAccessCount.incrementAndGet();
    }

    protected void recordCacheMiss() {
        missCount.incrementAndGet();
     }
 
     public long getAccessCount() {
@@ -114,6 +135,18 @@ public abstract class AbstractCache implements Cache {
     public void resetAccessCount() {
         accessCount.set(0);
     }
    
    public long getTotalAccessCount(){
        return totalAccessCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public void resetMissCount() {
        missCount.set(0);
    }
 
     public long getMemoryUsed() {
         return memoryUsed.get();
@@ -146,4 +179,25 @@ public abstract class AbstractCache implements Cache {
         }
     }
 
    /**
     * {@inheritDoc}
     */
    public String getCacheInfoAsString() {
        long u = getMemoryUsed() / 1024;
        long m = getMaxMemorySize() / 1024;
        StringBuilder c = new StringBuilder();
        c.append("Cache name=");
        c.append(this.toString());
        c.append(", elements=");
        c.append(getElementCount());
        c.append(", used memory=");
        c.append(u);
        c.append(", max memory=");
        c.append(m);
        c.append(", access=");
        c.append(getTotalAccessCount());
        c.append(", miss=");
        c.append(getMissCount());
        return c.toString();
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/Cache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/Cache.java
index 178367000..58d9006b7 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/Cache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/Cache.java
@@ -45,7 +45,6 @@ public interface Cache {
      * Get the number of accesses (get or set) until resetAccessCount was called.
      * @return the count
      */

     long getAccessCount();
 
     /**
@@ -53,9 +52,38 @@ public interface Cache {
      */
     void resetAccessCount();
 
    /**
     * Get the total number of cache accesses.
     * @return the number of hits
     */
    long getTotalAccessCount();

    /**
     * Get the number of cache misses.
     * 
     * @return the number of misses
     */
    long getMissCount();

    /**
     * Reset the cache miss counter.
     */
    void resetMissCount();

    /**
     * Get the number of elements/objects in the cache.
     * @return the number of elements
     */
    long getElementCount();

     /**
      * Add a listener to this cache that is informed after a number of accesses.
      */
     void setAccessListener(CacheAccessListener listener);
 
    /**
     * Gathers the stats of the cache for logging.
     */
    String getCacheInfoAsString();

 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheAccessListener.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheAccessListener.java
index 4fafe3024..8ea584ea1 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheAccessListener.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheAccessListener.java
@@ -28,9 +28,10 @@ public interface CacheAccessListener {
     int ACCESS_INTERVAL = 127;
 
     /**
     * The cache calls this method after a number of accessed.
     * The cache calls this method after a number of accessed.<br>
     * For statistical purposes, the cache access count is included
      */
    void cacheAccessed();
    void cacheAccessed(long accessCount);
 
     /**
      * Called after the cache is no longer used.
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
index 6c98cae25..f6e7dfb43 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/CacheManager.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.core.cache;
 
 import java.util.ArrayList;
import java.util.List;
 import java.util.WeakHashMap;
 
 import org.slf4j.Logger;
@@ -54,6 +55,9 @@ public class CacheManager implements CacheAccessListener {
     /** The default minimum resize interval (in ms). */
     private static final int DEFAULT_MIN_RESIZE_INTERVAL = 1000;
 
    /** The default minimum stats logging interval (in ms). */
    private static final int DEFAULT_LOG_STATS_INTERVAL = 60 * 1000;

     /** The size of a big object, to detect if a cache is full or not. */
     private static final int BIG_OBJECT_SIZE = 16 * 1024;
 
@@ -77,11 +81,21 @@ public class CacheManager implements CacheAccessListener {
             "org.apache.jackrabbit.cacheResizeInterval",
             DEFAULT_MIN_RESIZE_INTERVAL);
 
        /** The last time the caches where resized. */
    /** The minimum interval time between stats are logged */
    private long minLogStatsInterval = Long.getLong(
            "org.apache.jackrabbit.cacheLogStatsInterval",
            DEFAULT_LOG_STATS_INTERVAL);

    /** The last time the caches where resized. */
     private volatile long nextResize =
         System.currentTimeMillis() + DEFAULT_MIN_RESIZE_INTERVAL;
 
 
    /** The last time the cache stats were logged. */
    private volatile long nextLogStats =
            System.currentTimeMillis() + DEFAULT_LOG_STATS_INTERVAL;


     public long getMaxMemory() {
         return maxMemory;
     }
@@ -118,7 +132,10 @@ public class CacheManager implements CacheAccessListener {
      * After one of the caches is accessed a number of times, this method is called.
      * Resize the caches if required.
      */
    public void cacheAccessed() {
    public void cacheAccessed(long accessCount) {

        logCacheStats();

         long now = System.currentTimeMillis();
         if (now < nextResize) {
             return;
@@ -136,7 +153,22 @@ public class CacheManager implements CacheAccessListener {
     }
 
     /**
     * Re-calcualte the maximum memory for each cache, and set the new limits.
     * Log info about the caches.
     */
    private void logCacheStats() {
        if (log.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            if (now < nextLogStats) {
                return;
            }
            for (Cache cache : caches.keySet()) {
                log.debug(cache.getCacheInfoAsString());
            }
            nextLogStats = now + minLogStatsInterval;
        }
    }
    /**
     * Re-calculate the maximum memory for each cache, and set the new limits.
      */
     private void resizeAll() {
         if (log.isDebugEnabled()) {
@@ -146,11 +178,9 @@ public class CacheManager implements CacheAccessListener {
         // entries in a weak hash map may disappear any time
         // so can't use size() / keySet() directly
         // only using the iterator guarantees that we don't get null references
        ArrayList<Cache> list = new ArrayList<Cache>();
        List<Cache> list = new ArrayList<Cache>();
         synchronized (caches) {
            for (Cache c: caches.keySet()) {
                list.add(c);
            }
            list.addAll(caches.keySet());
         }
         if (list.size() == 0) {
             // nothing to do
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/ConcurrentCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/ConcurrentCache.java
index b56437a75..9931843dc 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/ConcurrentCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/cache/ConcurrentCache.java
@@ -57,10 +57,12 @@ public class ConcurrentCache<K, V> extends AbstractCache {
 
     }
 
    private final String name;
     private final Map<K, E<V>>[] segments;
 
     @SuppressWarnings({ "unchecked", "serial" })
    public ConcurrentCache(int numberOfSegments) {
    public ConcurrentCache(String name, int numberOfSegments) {
        this.name = name;
         this.segments = new Map[numberOfSegments];
         for (int i = 0; i < segments.length; i++) {
             segments[i] = new LinkedHashMap<K, E<V>>(16, 0.75f, true) {
@@ -77,8 +79,8 @@ public class ConcurrentCache<K, V> extends AbstractCache {
         }
     }
 
    public ConcurrentCache() {
        this(DEFAULT_NUMBER_OF_SEGMENTS);
    public ConcurrentCache(String name) {
        this(name, DEFAULT_NUMBER_OF_SEGMENTS);
     }
 
     /**
@@ -124,10 +126,10 @@ public class ConcurrentCache<K, V> extends AbstractCache {
             E<V> entry = segment.get(key);
             if (entry != null) {
                 return entry.value;
            } else {
                return null;
             }
         }
        recordCacheMiss();
        return null;
     }
 
     /**
@@ -252,4 +254,17 @@ public class ConcurrentCache<K, V> extends AbstractCache {
         }
     }
 
    public long getElementCount() {
        long count = 0;
        for (int i = 0; i < segments.length; i++) {
            count += segments[i].size();
        }
        return count;
    }

    @Override
    public String toString() {
        return name + "[" + getClass().getSimpleName() + "@"
                + Integer.toHexString(hashCode()) + "]";
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
index a62cc67a0..16ebe7583 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
@@ -377,7 +377,7 @@ public abstract class AbstractBundlePersistenceManager implements
     public void init(PMContext context) throws Exception {
         this.context = context;
         // init bundle cache
        bundles = new ConcurrentCache<NodeId, NodePropBundle>();
        bundles = new ConcurrentCache<NodeId, NodePropBundle>(context.getHomeDir().getName() + "BundleCache");
         bundles.setMaxMemorySize(bundleCacheSize);
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/MLRUItemStateCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/MLRUItemStateCache.java
index e3c4a8012..9f8f6abff 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/MLRUItemStateCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/MLRUItemStateCache.java
@@ -45,7 +45,7 @@ public class MLRUItemStateCache implements ItemStateCache {
     private volatile long numWrites = 0;
 
     private final ConcurrentCache<ItemId, ItemState> cache =
        new ConcurrentCache<ItemId, ItemState>();
        new ConcurrentCache<ItemId, ItemState>(MLRUItemStateCache.class.getSimpleName());
 
     public MLRUItemStateCache(CacheManager cacheMgr) {
         cache.setMaxMemorySize(DEFAULT_MAX_MEM);
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cache/ConcurrentCacheTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cache/ConcurrentCacheTest.java
index 934cf50ae..c405fee30 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cache/ConcurrentCacheTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cache/ConcurrentCacheTest.java
@@ -37,7 +37,7 @@ public class ConcurrentCacheTest extends TestCase {
         }
 
         ConcurrentCache<NodeId, NodeId> cache =
            new ConcurrentCache<NodeId, NodeId>();
            new ConcurrentCache<NodeId, NodeId>("test");
         cache.setMaxMemorySize(ids.length / 2);
 
         for (int i = 0; i < ids.length; i++) {
- 
2.19.1.windows.1

