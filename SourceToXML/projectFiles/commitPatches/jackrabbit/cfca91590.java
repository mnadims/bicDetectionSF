From cfca915908a449f7c79d6bd85742d3c662513d3a Mon Sep 17 00:00:00 2001
From: Stefan Guggisberg <stefan@apache.org>
Date: Fri, 4 Dec 2009 17:24:44 +0000
Subject: [PATCH] JCR-2425: Session.save() and Session.refresh(boolean) rely on
 accessibility of the root node

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@887279 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/ItemImpl.java  |   2 +-
 .../apache/jackrabbit/core/SessionImpl.java   |  15 ++-
 .../core/state/SessionItemStateManager.java   | 110 +++++++++++++++++-
 3 files changed, 116 insertions(+), 11 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
index 266106c28..656060c03 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
@@ -979,7 +979,7 @@ public abstract class ItemImpl implements Item {
              */
             Collection<ItemState> removed = getRemovedStates();
 
            // All affected item states. They keys are used to look up whether
            // All affected item states. The keys are used to look up whether
             // an item is affected, and the values are iterated through below
             Map<ItemId, ItemState> affected =
                 new HashMap<ItemId, ItemState>(dirty.size() + removed.size());
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
index de38b0c09..7a322468d 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
@@ -914,7 +914,13 @@ public class SessionImpl extends AbstractSession
         // check sanity of this session
         sanityCheck();
 
        getItemManager().getRootNode().save();
        // /JCR-2425: check whether session is allowed to read root node
        if (hasPermission("/", ACTION_READ)) {
            getItemManager().getRootNode().save();
        } else {
            NodeId id = getItemStateManager().getIdOfRootTransientNodeState();
            getItemManager().getItem(id).save();
        }
     }
 
     /**
@@ -936,11 +942,12 @@ public class SessionImpl extends AbstractSession
         }
 
         if (!keepChanges) {
            // optimization
             itemStateMgr.disposeAllTransientItemStates();
            return;
        } else {
            /** todo FIXME should reset Item#status field to STATUS_NORMAL
             * of all non-transient instances; maybe also
             * have to reset stale ItemState instances */
         }
        getItemManager().getRootNode().refresh(keepChanges);
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
index 808733fd6..918e43a9e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
@@ -22,12 +22,12 @@ import java.util.Collection;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
import java.util.LinkedList;
 
 import javax.jcr.InvalidItemStateException;
 import javax.jcr.ItemNotFoundException;
 import javax.jcr.ReferentialIntegrityException;
 import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
 
 import org.apache.commons.collections.iterators.IteratorChain;
 import org.apache.jackrabbit.core.CachingHierarchyManager;
@@ -38,10 +38,12 @@ import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.util.Dumpable;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -391,7 +393,7 @@ public class SessionItemStateManager
      * Returns an iterator over those transient item state instances that are
      * direct or indirect descendants of the item state with the given
      * <code>parentId</code>. The transient item state instance with the given
     * <code>parentId</code> itself (if there is such)                                                                            not be included.
     * <code>parentId</code> itself (if there is such) will not be included.
      * <p/>
      * The instances are returned in depth-first tree traversal order.
      *
@@ -417,9 +419,7 @@ public class SessionItemStateManager
         List[] la = new List[10];
         try {
             HierarchyManager atticAware = getAtticAwareHierarchyMgr();
            Iterator iter = transientStore.values().iterator();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
            for (ItemState state : transientStore.values()) {
                 // determine relative depth: > 0 means it's a descendant
                 int depth;
                 try {
@@ -561,6 +561,104 @@ public class SessionItemStateManager
         return resultIter;
     }
 
    /**
     * Returns the id of the root of the minimal subtree including all
     * transient states.
     *
     * @return id of nearest common ancestor of all transient states or null
     *         if there's no transient state.
     * @throws RepositoryException if an error occurs
     */
    public NodeId getIdOfRootTransientNodeState() throws RepositoryException {
        if (transientStore.isEmpty()) {
            return null;
        }

        // short cut
        if (transientStore.contains(hierMgr.getRootNodeId())) {
            return hierMgr.getRootNodeId();
        }

        // the nearest common ancestor of all transient states
        // must be either
        // a) a node state with STATUS_EXISTING_MODIFIED, or
        // b) the parent node of a property state with STATUS_EXISTING_MODIFIED 

        // collect all candidates based on above criteria
        Collection<NodeId> candidateIds = new LinkedList<NodeId>();
        try {
            HierarchyManager hierMgr = getHierarchyMgr();
            for (ItemState state : transientStore.values()) {
                if (state.getStatus() == ItemState.STATUS_EXISTING_MODIFIED) {
                    NodeId nodeId;
                    if (state.isNode()) {
                        nodeId = (NodeId) state.getId();
                    } else {
                        nodeId = state.getParentId();
                    }
                    // remove any descendant candidates
                    boolean skip = false;
                    for (NodeId id : candidateIds) {
                        if (nodeId.equals(id) || hierMgr.isAncestor(id, nodeId)) {
                            // already a candidate or a descendant thereof
                            // => skip
                            skip = true;
                            break;
                        }
                        if (hierMgr.isAncestor(nodeId, id)) {
                            // candidate is a descendant => remove
                            candidateIds.remove(id);
                        }
                    }
                    if (!skip) {
                        // add to candidates
                        candidateIds.add(nodeId);
                    }
                }
            }

            if (candidateIds.size() == 1) {
                return candidateIds.iterator().next();
            }

            // pick (any) candidate with shortest path to start with
            NodeId candidateId = null;
            for (NodeId id : candidateIds) {
                if (candidateId == null) {
                    candidateId = id;
                } else {
                    if (hierMgr.getDepth(id) < hierMgr.getDepth(candidateId)) {
                        candidateId = id;
                    }
                }
            }

            // starting with this candidate closest to root, find first parent
            // which is an ancestor of all candidates
            NodeState state = (NodeState) getItemState(candidateId);
            NodeId parentId = state.getParentId();
            boolean continueWithParent = false;
            while (parentId != null) {
                for (NodeId id : candidateIds) {
                    if (hierMgr.getRelativeDepth(parentId, id) == -1) {
                        continueWithParent = true;
                        break;
                    }
                }
                if (continueWithParent) {
                    state = (NodeState) getItemState(candidateId);
                    parentId = state.getParentId();
                    continueWithParent = false;
                } else {
                    break;
                }
            }
            return parentId;
        } catch (ItemStateException e) {
            throw new RepositoryException("failed to determine common root of transient changes", e);
        }
    }

     /**
      * Return a flag indicating whether the specified item is in the transient
      * item state manager's attic space.
- 
2.19.1.windows.1

