From 6aa1d34ba7210a35b803845288404394fe18d17b Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Wed, 8 Jul 2009 16:35:20 +0000
Subject: [PATCH] JCR-2171: Deadlock in SharedItemStateManager on session.move
 and node.save

Avoid the deadlock in CachingHierarchyManager by sending update notifications only after the SISM lock has been downgraded.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@792218 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/state/SharedItemStateManager.java      | 7 ++++---
 1 file changed, 4 insertions(+), 3 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 147edbd18..8e66d48fe 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -741,13 +741,14 @@ public class SharedItemStateManager
 
             ISMLocking.ReadLock readLock = null;
             try {
                /* Let the shared item listeners know about the change */
                shared.persisted();

                 // downgrade to read lock
                 readLock = writeLock.downgrade();
                 writeLock = null;
 
                // Let the shared item listeners know about the change
                // JCR-2171: This must happen after downgrading the lock!
                shared.persisted();

                 /* notify virtual providers about node references */
                 for (int i = 0; i < virtualNodeReferences.length; i++) {
                     ChangeLog virtualRefs = virtualNodeReferences[i];
- 
2.19.1.windows.1

