From 84bffdac2230a14dcf51acfc77315b03f4890804 Mon Sep 17 00:00:00 2001
From: Stefan Guggisberg <stefan@apache.org>
Date: Mon, 9 May 2011 14:46:34 +0000
Subject: [PATCH] JCR-2967:
 SessionItemStateManager.getIdOfRootTransientNodeState() may cause NPE

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1101046 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/state/SessionItemStateManager.java     | 7 ++++---
 1 file changed, 4 insertions(+), 3 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
index 0f8bede68..635b97545 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
@@ -462,15 +462,16 @@ public class SessionItemStateManager
 
         // the nearest common ancestor of all transient states
         // must be either
        // a) a node state with STATUS_EXISTING_MODIFIED, or
        // b) the parent node of a property state with STATUS_EXISTING_MODIFIED
        // a) a node state with STATUS_EXISTING_MODIFIED or STATUS_STALE_DESTROYED, or
        // b) the parent node of a property state with STATUS_EXISTING_MODIFIED or STATUS_STALE_DESTROYED
 
         // collect all candidates based on above criteria
         Collection<NodeId> candidateIds = new LinkedList<NodeId>();
         try {
             HierarchyManager hierMgr = getHierarchyMgr();
             for (ItemState state : transientStore.values()) {
                if (state.getStatus() == ItemState.STATUS_EXISTING_MODIFIED) {
                if (state.getStatus() == ItemState.STATUS_EXISTING_MODIFIED
                        || state.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                     NodeId nodeId;
                     if (state.isNode()) {
                         nodeId = (NodeId) state.getId();
- 
2.19.1.windows.1

