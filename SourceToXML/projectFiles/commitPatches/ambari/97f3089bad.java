From 97f3089badbf4f16f828d92b317bb623605ee66b Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Tue, 1 Mar 2016 12:38:30 -0500
Subject: [PATCH] AMBARI-15173 - Express Upgrade Stuck At Manual Prompt Due To
 HRC Status Calculation Cache Problem (part5) (jonathanhurley)

--
 .../server/orm/dao/HostRoleCommandDAO.java    | 63 +++++++++++--------
 1 file changed, 37 insertions(+), 26 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
index 14dac797bf..b48ffa8a8c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
@@ -60,8 +60,8 @@ import org.apache.ambari.server.orm.entities.StageEntity;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.google.common.cache.Cache;
 import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
 import com.google.common.cache.LoadingCache;
 import com.google.common.collect.Lists;
 import com.google.inject.Inject;
@@ -120,8 +120,15 @@ public class HostRoleCommandDAO {
    * being read during a transaction which has updated a
    * {@link HostRoleCommandEntity}'s {@link HostRoleStatus} but has not
    * committed yet.
   * <p/>
   * This cache cannot be a {@link LoadingCache} since there is an inherent
   * problem with concurrency of reloads. Namely, if the entry has been read
   * during a load, but not yet put into the cache and another invalidation is
   * registered. The old value would eventually make it into the cache and the
   * last invalidation would not invalidate anything since the cache was empty
   * at the time.
    */
  private final LoadingCache<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> hrcStatusSummaryCache;
  private final Cache<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> hrcStatusSummaryCache;
 
   /**
    * Specifies whether caching for {@link HostRoleCommandStatusSummaryDTO} grouped by stage id for requests
@@ -252,24 +259,7 @@ public class HostRoleCommandDAO {
     hrcStatusSummaryCache = CacheBuilder.newBuilder()
       .maximumSize(hostRoleCommandStatusSummaryCacheLimit)
       .expireAfterWrite(hostRoleCommandStatusSummaryCacheExpiryDurationMins, TimeUnit.MINUTES)
      .build(new CacheLoader<Long, Map<Long, HostRoleCommandStatusSummaryDTO>>() {
        @Override
        public Map<Long, HostRoleCommandStatusSummaryDTO> load(Long requestId) throws Exception {
          LOG.debug("Cache miss for host role command status summary object for request {}, fetching from JPA", requestId);

          // ensure that we wait for any running transactions working on this cache to
          // complete
          ReadWriteLock lock = transactionLocks.getLock(LockArea.HRC_STATUS_CACHE);
          lock.readLock().lock();

          try{
            Map<Long, HostRoleCommandStatusSummaryDTO> hrcCommandStatusByStageId = loadAggregateCounts(requestId);
            return hrcCommandStatusByStageId;
          } finally {
            lock.readLock().unlock();
          }
        }
      });
      .build();
   }
 
   @RequiresSession
@@ -665,16 +655,37 @@ public class HostRoleCommandDAO {
 
 
   /**
   * Finds the counts of tasks for a request and groups them by stage id.
   * @param requestId the request id
   * Finds the counts of tasks for a request and groups them by stage id. If
   * caching is enabled, this will first consult the cache. Cache misses will
   * then defer to loading the data from the database and then caching the
   * result.
   *
   * @param requestId
   *          the request id
    * @return the map of stage-to-summary objects
    */
   public Map<Long, HostRoleCommandStatusSummaryDTO> findAggregateCounts(Long requestId) {
    if (hostRoleCommandStatusSummaryCacheEnabled) {
      return hrcStatusSummaryCache.getUnchecked(requestId);
    if (!hostRoleCommandStatusSummaryCacheEnabled) {
      return loadAggregateCounts(requestId);
     }
    else {
      return loadAggregateCounts(requestId); // if caching not enabled fall back to fetching through JPA

    Map<Long, HostRoleCommandStatusSummaryDTO> map = hrcStatusSummaryCache.getIfPresent(requestId);
    if (null != map) {
      return map;
    }

    // ensure that we wait for any running transactions working on this cache to
    // complete
    ReadWriteLock lock = transactionLocks.getLock(LockArea.HRC_STATUS_CACHE);
    lock.readLock().lock();

    try {
      map = loadAggregateCounts(requestId);
      hrcStatusSummaryCache.put(requestId, map);

      return map;
    } finally {
      lock.readLock().unlock();
     }
   }
 
- 
2.19.1.windows.1

