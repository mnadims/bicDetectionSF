From 1cf1f0305eb4e313b61cf11b70a5697fbd4e1d8b Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 27 Sep 2011 16:52:09 +0000
Subject: [PATCH] JCR-3063: NullPointerException in ItemManager

Restore the JCR-2171 fix to avoid deadlocks.

Introduce extra synchronization and checks to prevent
the CMEs and replace the NPEs with InvalidItemStateExceptions.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1176465 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/ItemManager.java   | 14 +++++++++++++-
 .../core/state/SessionItemStateManager.java       | 15 ++++++++-------
 .../core/state/SharedItemStateManager.java        |  7 ++++---
 3 files changed, 25 insertions(+), 11 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
index 7aba54b37..03055f8af 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
@@ -159,7 +159,13 @@ public class ItemManager implements ItemStateListener {
         if (parentId == null) {
             // removed state has parentId set to null
             // get from overlayed state
            parentId = state.getOverlayedState().getParentId();
            ItemState overlaid = state.getOverlayedState();
            if (overlaid != null) {
                parentId = overlaid.getParentId();
            } else {
                throw new InvalidItemStateException(
                        "Could not find parent of node " + state.getNodeId());
            }
         }
         NodeState parentState = null;
         try {
@@ -197,6 +203,12 @@ public class ItemManager implements ItemStateListener {
 
         // get child node entry
         ChildNodeEntry cne = parentState.getChildNodeEntry(state.getNodeId());
        if (cne == null) {
            throw new InvalidItemStateException(
                    "Could not find child " + state.getNodeId()
                    + " of node " + parentState.getNodeId());
        }

         NodeTypeRegistry ntReg = sessionContext.getNodeTypeRegistry();
         try {
             EffectiveNodeType ent = ntReg.getEffectiveNodeType(
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
index 635b97545..ffbad9dec 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
@@ -65,13 +65,13 @@ public class SessionItemStateManager
      * map of those states that have been removed transiently
      */
     private final Map<ItemId, ItemState> atticStore =
        new HashMap<ItemId, ItemState>();
            Collections.synchronizedMap(new HashMap<ItemId, ItemState>());
 
     /**
      * map of new or modified transient states
      */
     private final Map<ItemId, ItemState> transientStore =
        new HashMap<ItemId, ItemState>();
            Collections.synchronizedMap(new HashMap<ItemId, ItemState>());
 
     /**
      * ItemStateManager view of the states in the attic; lazily instantiated
@@ -415,7 +415,8 @@ public class SessionItemStateManager
             // Group the descendants by reverse relative depth
             SortedMap<Integer, Collection<ItemState>> statesByReverseDepth =
                 new TreeMap<Integer, Collection<ItemState>>();
            for (ItemState state : store.values()) {
            ItemState[] states = store.values().toArray(new ItemState[0]);
            for (ItemState state : states) {
                 // determine relative depth: > 0 means it's a descendant
                 int depth = hierarchyManager.getShareRelativeDepth(
                         (NodeId) id, state.getId());
@@ -732,11 +733,12 @@ public class SessionItemStateManager
     public void disposeAllTransientItemStates() {
         // dispose item states in transient map & attic
         // (use temp collection to avoid ConcurrentModificationException)
        Collection<ItemState> tmp = new ArrayList<ItemState>(transientStore.values());
        ItemState[] tmp;
        tmp = transientStore.values().toArray(new ItemState[0]);
         for (ItemState state : tmp) {
             disposeTransientItemState(state);
         }
        tmp = new ArrayList<ItemState>(atticStore.values());
        tmp = atticStore.values().toArray(new ItemState[0]);
         for (ItemState state : tmp) {
             disposeTransientItemStateInAttic(state);
         }
@@ -841,9 +843,8 @@ public class SessionItemStateManager
                 visibleState = transientState;
             } else {
                 // check attic
                transientState = atticStore.get(destroyed.getId());
                transientState = atticStore.remove(destroyed.getId());
                 if (transientState != null) {
                    atticStore.remove(destroyed.getId());
                     transientState.onDisposed();
                 }
             }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 1260042f8..58f12b457 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -771,13 +771,14 @@ public class SharedItemStateManager
 
             ISMLocking.ReadLock readLock = null;
             try {
                // Let the shared item listeners know about the change
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

