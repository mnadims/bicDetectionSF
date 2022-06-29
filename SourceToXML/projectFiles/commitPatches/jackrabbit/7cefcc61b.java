From 7cefcc61bee24677888e49fe00d5a4982a969e3b Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 27 Sep 2011 18:43:46 +0000
Subject: [PATCH] JCR-3063: NullPointerException in ItemManager

Add a few extra checks against NPEs and CMEs

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1176515 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/core/ItemSaveOperation.java | 15 ++++++++++++---
 .../core/state/SessionItemStateManager.java       |  5 +++--
 2 files changed, 15 insertions(+), 5 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemSaveOperation.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemSaveOperation.java
index a96f2bade..49a17551a 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemSaveOperation.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemSaveOperation.java
@@ -490,9 +490,18 @@ class ItemSaveOperation implements SessionWriteOperation<Object> {
                  * its primary type has changed, check its node type against the
                  * required node type in its definition
                  */
                if (nodeState.getStatus() == ItemState.STATUS_NEW
                        || !nodeState.getNodeTypeName().equals(
                            ((NodeState) nodeState.getOverlayedState()).getNodeTypeName())) {
                boolean primaryTypeChanged =
                        nodeState.getStatus() == ItemState.STATUS_NEW;
                if (!primaryTypeChanged) {
                    NodeState overlaid =
                            (NodeState) nodeState.getOverlayedState();
                    if (overlaid != null) {
                        Name newName = nodeState.getNodeTypeName();
                        Name oldName = overlaid.getNodeTypeName();
                        primaryTypeChanged = !newName.equals(oldName);
                    }
                }
                if (primaryTypeChanged) {
                     for (NodeType ntReq : nodeDef.getRequiredPrimaryTypes()) {
                         Name ntName = ((NodeTypeImpl) ntReq).getQName();
                         if (!(pnt.getQName().equals(ntName)
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
index ffbad9dec..9ff82d748 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
@@ -470,7 +470,9 @@ public class SessionItemStateManager
         Collection<NodeId> candidateIds = new LinkedList<NodeId>();
         try {
             HierarchyManager hierMgr = getHierarchyMgr();
            for (ItemState state : transientStore.values()) {
            ItemState[] states =
                    transientStore.values().toArray(new ItemState[0]);
            for (ItemState state : states) {
                 if (state.getStatus() == ItemState.STATUS_EXISTING_MODIFIED
                         || state.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                     NodeId nodeId;
@@ -567,7 +569,6 @@ public class SessionItemStateManager
      */
     public NodeState createTransientNodeState(NodeId id, Name nodeTypeName, NodeId parentId, int initialStatus)
             throws ItemStateException {

         // check map; synchronized to ensure an entry is not created twice.
         synchronized (transientStore) {
             if (transientStore.containsKey(id)) {
- 
2.19.1.windows.1

