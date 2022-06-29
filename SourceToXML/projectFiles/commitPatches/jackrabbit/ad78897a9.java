From ad78897a9f86674a0c162fbf9a56fee1e52e661d Mon Sep 17 00:00:00 2001
From: Thomas Mueller <thomasm@apache.org>
Date: Mon, 18 Jan 2010 16:42:10 +0000
Subject: [PATCH] JCR-2456 Repository is corrupt after concurrent changes with
 the same session

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@900453 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/ItemImpl.java  | 13 ++++++-
 .../observation/EventStateCollection.java     | 17 ++++++++-
 .../core/state/SharedItemStateManager.java    | 37 ++++++++++++++++++-
 3 files changed, 63 insertions(+), 4 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
index da40f2b87..b0ec3a9a2 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
@@ -18,6 +18,7 @@ package org.apache.jackrabbit.core;
 
 import java.util.ArrayList;
 import java.util.Collection;
import java.util.ConcurrentModificationException;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
@@ -481,7 +482,7 @@ public abstract class ItemImpl implements Item {
                     String msg = itemMgr.safeGetJCRPath(id)
                                 + ": mandatory child node " + cnd.getName()
                                 + " does not exist";
                    if (!nodeState.hasChildNodeEntry(cnd.getName())) {                      
                    if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                         log.debug(msg);
                         throw new ConstraintViolationException(msg);
                     } else {
@@ -975,7 +976,15 @@ public abstract class ItemImpl implements Item {
              * build list of transient (i.e. new & modified) states that
              * should be persisted
              */
            Collection<ItemState> dirty = getTransientStates();
            Collection<ItemState> dirty;
            try {
                dirty = getTransientStates();
            } catch (ConcurrentModificationException e) {
                String msg = "Concurrent modification; session is closed";
                log.error(msg, e);
                session.logout();
                throw e;
            }
             if (dirty.size() == 0) {
                 // no transient items, nothing to do here
                 return;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/observation/EventStateCollection.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/observation/EventStateCollection.java
index bb6956132..66a814253 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/observation/EventStateCollection.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/observation/EventStateCollection.java
@@ -404,6 +404,11 @@ public final class EventStateCollection {
                 NodeId parentId = n.getParentId();
                 // the parent of an added item is always modified or new
                 NodeState parent = (NodeState) changes.get(parentId);
                if (parent == null) {
                    String msg = "Parent " + parentId + " must be changed as well.";
                    log.error(msg);
                    throw new ItemStateException(msg);
                }
                 NodeTypeImpl nodeType = getNodeType(parent, session);
                 Set<Name> mixins = parent.getMixinTypeNames();
                 Path path = getPath(n.getNodeId(), hmgr);
@@ -420,6 +425,11 @@ public final class EventStateCollection {
             } else {
                 // property created / set
                 NodeState n = (NodeState) changes.get(state.getParentId());
                if (n == null) {
                    String msg = "Node " + state.getParentId() + " must be changed as well.";
                    log.error(msg);
                    throw new ItemStateException(msg);
                }
                 NodeTypeImpl nodeType = getNodeType(n, session);
                 Set<Name> mixins = n.getMixinTypeNames();
                 Path path = getPath(state.getId(), hmgr);
@@ -625,7 +635,12 @@ public final class EventStateCollection {
         } catch (Exception e) {
             // also catch eventual runtime exceptions here
             // should never happen actually
            String msg = "Item " + node.getNodeId() + " has unknown node type: " + node.getNodeTypeName();
            String msg;
            if (node == null) {
                msg = "Node state is null";
            } else {
                msg = "Item " + node.getNodeId() + " has unknown node type: " + node.getNodeTypeName();
            }
             log.error(msg);
             throw new ItemStateException(msg, e);
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 17a183605..5d64aa1f3 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -574,6 +574,8 @@ public class SharedItemStateManager
                     checkReferentialIntegrity();
                 }
 
                checkAddedChildNodes();

                 /**
                  * prepare the events. this needs to be after the referential
                  * integrity check, since another transaction could have modified
@@ -731,8 +733,8 @@ public class SharedItemStateManager
                 long t0 = System.currentTimeMillis();
                 persistMgr.store(shared);
                 succeeded = true;
                long t1 = System.currentTimeMillis();
                 if (log.isDebugEnabled()) {
                    long t1 = System.currentTimeMillis();
                     log.debug("persisting change log " + shared + " took " + (t1 - t0) + "ms");
                 }
             } finally {
@@ -967,6 +969,39 @@ public class SharedItemStateManager
             }
         }
 
        /**
         * Verify the added child nodes of the added or modified states exist.
         * If they don't exist, most likely the problem is that the same session
         * is used concurrently.
         */
        private void checkAddedChildNodes() throws ItemStateException {
            for (ItemState state : local.addedStates()) {
                checkAddedChildNode(state);
            }
            for (ItemState state : local.modifiedStates()) {
                checkAddedChildNode(state);
            }
        }

        private void checkAddedChildNode(ItemState state) throws ItemStateException {
            if (state.isNode()) {
                NodeState node = (NodeState) state;
                for (ChildNodeEntry child : node.getAddedChildNodeEntries()) {
                    NodeId id = child.getId();
                    if (local.get(id) == null &&
                            !id.equals(RepositoryImpl.VERSION_STORAGE_NODE_ID) &&
                            !id.equals(RepositoryImpl.ACTIVITIES_NODE_ID) &&
                            !id.equals(RepositoryImpl.NODETYPES_NODE_ID) &&
                            !cache.isCached(id) &&
                            !persistMgr.exists(id)) {
                        String msg = "Trying to add a non-existing child node: " + id;
                        log.debug(msg);
                        throw new ItemStateException(msg);
                    }
                }
            }
        }

         /**
          * Verifies that
          * <ul>
- 
2.19.1.windows.1

