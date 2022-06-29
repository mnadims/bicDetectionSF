From 64c739f4f1277064e1bad1ab1200248a1402c6a6 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Mon, 16 Feb 2009 11:21:33 +0000
Subject: [PATCH] JCR-1979: Deadlock on concurrent read & transactional write
 operations

Moved the virtual provider accesses outside the workspace read lock. This avoids the deadlock with a transactional write.

This change in lock scope does not endanger consistency, as all the modifiable virtual providers already have their own internal locking (as evidenced by the deadlock scenario!). In fact a global virtual provider like the version store *must* have it's own locking mechanism as it can be concurrently accessed from multiple workspaces.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@744895 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/state/SharedItemStateManager.java    | 35 +++++++++----------
 1 file changed, 17 insertions(+), 18 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 506fb59eb..60d70fc69 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -313,9 +313,7 @@ public class SharedItemStateManager
      */
     public NodeReferences getNodeReferences(NodeReferencesId id)
             throws NoSuchItemStateException, ItemStateException {

         ISMLocking.ReadLock readLock = acquireReadLock(id.getTargetId());

         try {
             // check persistence manager
             try {
@@ -323,18 +321,19 @@ public class SharedItemStateManager
             } catch (NoSuchItemStateException e) {
                 // ignore
             }
            // check virtual providers
            for (int i = 0; i < virtualProviders.length; i++) {
                try {
                    return virtualProviders[i].getNodeReferences(id);
                } catch (NoSuchItemStateException e) {
                    // ignore
                }
            }
         } finally {
             readLock.release();
         }
 
        // check virtual providers
        for (int i = 0; i < virtualProviders.length; i++) {
            try {
                return virtualProviders[i].getNodeReferences(id);
            } catch (NoSuchItemStateException e) {
                // ignore
            }
        }

         // throw
         throw new NoSuchItemStateException(id.toString());
     }
@@ -343,14 +342,12 @@ public class SharedItemStateManager
      * {@inheritDoc}
      */
     public boolean hasNodeReferences(NodeReferencesId id) {

         ISMLocking.ReadLock readLock;
         try {
             readLock = acquireReadLock(id.getTargetId());
         } catch (ItemStateException e) {
             return false;
         }

         try {
             // check persistence manager
             try {
@@ -360,15 +357,17 @@ public class SharedItemStateManager
             } catch (ItemStateException e) {
                 // ignore
             }
            // check virtual providers
            for (int i = 0; i < virtualProviders.length; i++) {
                if (virtualProviders[i].hasNodeReferences(id)) {
                    return true;
                }
            }
         } finally {
             readLock.release();
         }

        // check virtual providers
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].hasNodeReferences(id)) {
                return true;
            }
        }

         return false;
     }
 
- 
2.19.1.windows.1

