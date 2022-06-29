From ff9b8ddba04cef748d22fadcdbfa6ab9fc0ac582 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Wed, 4 Jul 2012 13:47:56 +0000
Subject: [PATCH] JCR-3345: ACL evaluation may return non-fresh results

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1357265 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/state/ItemStateReferenceCache.java           |  5 +++--
 .../jackrabbit/core/state/SharedItemStateManager.java | 11 +++++++++++
 2 files changed, 14 insertions(+), 2 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/ItemStateReferenceCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/ItemStateReferenceCache.java
index cec8bab2a..00f38449f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/ItemStateReferenceCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/ItemStateReferenceCache.java
@@ -172,10 +172,11 @@ public class ItemStateReferenceCache implements ItemStateCache {
         ItemId id = state.getId();
         Map<ItemId, ItemState> segment = getSegment(id);
         synchronized (segment) {
            if (segment.containsKey(id)) {
            ItemState s = segment.put(id, state);
            // overwriting the same instance is OK
            if (s != null && s != state) {
                 log.warn("overwriting cached entry " + id);
             }
            segment.put(id, state);
         }
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 42695aacb..d710cef22 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -787,6 +787,17 @@ public class SharedItemStateManager
 
             ISMLocking.ReadLock readLock = null;
             try {
                // make sure new item states are present/referenced in cache
                // we do this before the lock is downgraded to a read lock
                // because then other threads will be able to read from
                // this SISM again and potentially read an added item state
                // before the ones here are put into the cache (via
                // shared.persisted()). See JCR-3345
                for (ItemState state : shared.addedStates()) {
                    state.setStatus(ItemState.STATUS_EXISTING);
                    cache.cache(state);
                }

                 // downgrade to read lock
                 readLock = writeLock.downgrade();
                 writeLock = null;
- 
2.19.1.windows.1

