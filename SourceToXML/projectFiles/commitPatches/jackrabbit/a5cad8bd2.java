From a5cad8bd20e9069739aa70d523ee280f55b577b5 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Mon, 16 Feb 2009 13:37:19 +0000
Subject: [PATCH] JCR-1979: Deadlock on concurrent read & transactional write
 operations

We can also move the other (than getReferences) virtual provider accesses outside the workspace lock. This obsoletes the remaining parts of the "Use the JCR versioning API instead of the /jcr:system/jcr:versionStorage tree to access version information" recommendation.


git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@744911 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/state/SharedItemStateManager.java    | 53 ++++++++++---------
 1 file changed, 28 insertions(+), 25 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 60d70fc69..426fa446e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -243,29 +243,30 @@ public class SharedItemStateManager
      */
     public ItemState getItemState(ItemId id)
             throws NoSuchItemStateException, ItemStateException {
        // check the virtual root ids (needed for overlay)
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].isVirtualRoot(id)) {
                return virtualProviders[i].getItemState(id);
            }
        }
 
         ISMLocking.ReadLock readLock = acquireReadLock(id);

         try {
            // check the virtual root ids (needed for overlay)
            for (int i = 0; i < virtualProviders.length; i++) {
                if (virtualProviders[i].isVirtualRoot(id)) {
                    return virtualProviders[i].getItemState(id);
                }
            }
             // check internal first
             if (hasNonVirtualItemState(id)) {
                 return getNonVirtualItemState(id);
             }
            // check if there is a virtual state for the specified item
            for (int i = 0; i < virtualProviders.length; i++) {
                if (virtualProviders[i].hasItemState(id)) {
                    return virtualProviders[i].getItemState(id);
                }
            }
         } finally {
             readLock.release();
         }

        // check if there is a virtual state for the specified item
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].hasItemState(id)) {
                return virtualProviders[i].getItemState(id);
            }
        }

         throw new NoSuchItemStateException(id.toString());
     }
 
@@ -273,6 +274,12 @@ public class SharedItemStateManager
      * {@inheritDoc}
      */
     public boolean hasItemState(ItemId id) {
        // check the virtual root ids (needed for overlay)
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].isVirtualRoot(id)) {
                return true;
            }
        }
 
         ISMLocking.ReadLock readLock;
         try {
@@ -286,25 +293,21 @@ public class SharedItemStateManager
                 return true;
             }
 
            // check the virtual root ids (needed for overlay)
            for (int i = 0; i < virtualProviders.length; i++) {
                if (virtualProviders[i].isVirtualRoot(id)) {
                    return true;
                }
            }
             // check if this manager has the item state
             if (hasNonVirtualItemState(id)) {
                 return true;
             }
            // otherwise check virtual ones
            for (int i = 0; i < virtualProviders.length; i++) {
                if (virtualProviders[i].hasItemState(id)) {
                    return true;
                }
            }
         } finally {
             readLock.release();
         }

        // otherwise check virtual ones
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].hasItemState(id)) {
                return true;
            }
        }

         return false;
     }
 
- 
2.19.1.windows.1

