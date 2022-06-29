From 1da3c12b6bc4adc8c8a704d1cf7791f3d30064cb Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Tue, 18 Oct 2011 14:38:33 +0000
Subject: [PATCH] JCR-3115: Versioning fixup leaves persistence in a state
 where the node can't be made versionable again

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1185691 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/RepositoryChecker.java    | 92 ++++++++++++++++---
 .../version/InconsistentVersioningState.java  | 24 ++++-
 .../version/InternalVersionHistoryImpl.java   |  4 +-
 .../core/version/InternalVersionImpl.java     |  2 +-
 .../version/InternalVersionManagerBase.java   | 14 ++-
 5 files changed, 110 insertions(+), 26 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
index aa28341f2..f0d908137 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
@@ -24,6 +24,7 @@ import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ROOTVERSI
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_VERSIONHISTORY;
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_VERSIONABLE;
 
import java.util.Calendar;
 import java.util.HashSet;
 import java.util.Set;
 
@@ -39,8 +40,11 @@ import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.version.InconsistentVersioningState;
 import org.apache.jackrabbit.core.version.InternalVersion;
 import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.util.ISO8601;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -62,13 +66,16 @@ class RepositoryChecker {
 
     private final ChangeLog workspaceChanges;
 
    private final InternalVersionManager versionManager;
    private final ChangeLog vworkspaceChanges;

    private final InternalVersionManagerImpl versionManager;
 
     public RepositoryChecker(
             PersistenceManager workspace,
            InternalVersionManager versionManager) {
            InternalVersionManagerImpl versionManager) {
         this.workspace = workspace;
         this.workspaceChanges = new ChangeLog();
        this.vworkspaceChanges = new ChangeLog();
         this.versionManager = versionManager;
     }
 
@@ -91,25 +98,33 @@ class RepositoryChecker {
         }
     }
 
    public void fix() throws RepositoryException {
        if (workspaceChanges.hasUpdates()) {
            log.warn("Fixing repository inconsistencies");
    private void fix(PersistenceManager pm, ChangeLog changes, String store)
            throws RepositoryException {
        if (changes.hasUpdates()) {
            log.warn("Fixing " + store + " inconsistencies");
             try {
                workspace.store(workspaceChanges);
                pm.store(changes);
             } catch (ItemStateException e) {
                e.printStackTrace();
                throw new RepositoryException(
                        "Failed to fix workspace inconsistencies", e);
                String message = "Failed to fix " + store + " inconsistencies (aborting)";
                log.error(message, e);
                throw new RepositoryException(message, e);
             }
         } else {
            log.info("No repository inconsistencies found");
            log.info("No " + store + "  inconsistencies found");
         }
     }
 
    public void fix() throws RepositoryException {
        fix(workspace, workspaceChanges, "workspace");
        fix(versionManager.getPersistenceManager(), vworkspaceChanges,
                "versioning workspace");
    }

     private void checkVersionHistory(NodeState node) {
         if (node.hasPropertyName(JCR_VERSIONHISTORY)) {
             String message = null;
             NodeId nid = node.getNodeId();
            NodeId vhid = null;
 
             try {
                 log.debug("Checking version history of node {}", nid);
@@ -117,6 +132,8 @@ class RepositoryChecker {
                 message = "Removing references to a missing version history of node " + nid;
                 InternalVersionHistory vh = versionManager.getVersionHistoryOfNode(nid);
 
                vhid = vh.getId();
                
                 // additional checks, see JCR-3101
                 String intro = "Removing references to an inconsistent version history of node "
                     + nid;
@@ -144,14 +161,25 @@ class RepositoryChecker {
                     message = intro + " (root version is missing)";
                     throw new InconsistentVersioningState("root version of " + nid +" is missing.");
                 }
            } catch (InconsistentVersioningState e) {
                log.info(message, e);
                NodeId nvhid = e.getVersionHistoryNodeId();
                if (nvhid != null) {
                    if (vhid != null && !nvhid.equals(vhid)) {
                        log.error("vhrid returned with InconsistentVersioningState does not match the id we already had: "
                                + vhid + " vs " + nvhid);
                    }
                    vhid = nvhid; 
                }
                removeVersionHistoryReferences(node, vhid);
             } catch (Exception e) {
                 log.info(message, e);
                removeVersionHistoryReferences(node);
                removeVersionHistoryReferences(node, vhid);
             }
         }
     }
 
    private void removeVersionHistoryReferences(NodeState node) {
    private void removeVersionHistoryReferences(NodeState node, NodeId vhid) {
         NodeState modified =
             new NodeState(node, NodeState.STATUS_EXISTING_MODIFIED, true);
 
@@ -166,6 +194,44 @@ class RepositoryChecker {
         removeProperty(modified, JCR_ISCHECKEDOUT);
 
         workspaceChanges.modified(modified);
        
        if (vhid != null) {
            // attempt to rename the version history, so it doesn't interfere with
            // a future attempt to put the node under version control again 
            // (see JCR-3115)
            
            log.info("trying to rename version history of node " + node.getId());

            NameFactory nf = NameFactoryImpl.getInstance();
            
            // Name of VHR in parent folder is ID of versionable node
            Name vhrname = nf.create(Name.NS_DEFAULT_URI, node.getId().toString());

            try {
                NodeState vhrState = versionManager.getPersistenceManager().load(vhid);
                NodeState vhrParentState = versionManager.getPersistenceManager().load(vhrState.getParentId());
                
                if (vhrParentState.hasChildNodeEntry(vhrname)) {
                    NodeState modifiedParent = (NodeState) vworkspaceChanges.get(vhrState.getParentId());
                    if (modifiedParent == null) {
                        modifiedParent = new NodeState(vhrParentState, NodeState.STATUS_EXISTING_MODIFIED, true);
                    }
                    
                    Calendar now = Calendar.getInstance();
                    String appendme = " (disconnected by RepositoryChecker on "
                            + ISO8601.format(now) + ")";
                    modifiedParent.renameChildNodeEntry(vhid,
                            nf.create(vhrname.getNamespaceURI(), vhrname.getLocalName() + appendme));

                    vworkspaceChanges.modified(modifiedParent);
                }
                else {
                    log.info("child node entry " + vhrname + " for version history not found inside parent folder.");
                }
            } catch (Exception ex) {
                log.error("while trying to rename the version history", ex);
            }
        }
     }
 
     private void removeProperty(NodeState node, Name name) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InconsistentVersioningState.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InconsistentVersioningState.java
index 6d0d8a2ef..74822d5d9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InconsistentVersioningState.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InconsistentVersioningState.java
@@ -16,6 +16,8 @@
  */
 package org.apache.jackrabbit.core.version;
 
import org.apache.jackrabbit.core.id.NodeId;

 /**
  * The <code>InconsistentVersionControlState</code> is used to signal
  * inconsistencies in the versioning related state of a node, such
@@ -23,6 +25,8 @@ package org.apache.jackrabbit.core.version;
  */
 public class InconsistentVersioningState extends RuntimeException {
 
    private final NodeId versionHistoryNodeId;
    
     /**
      * Constructs a new instance of this class with the specified detail
      * message.
@@ -32,18 +36,28 @@ public class InconsistentVersioningState extends RuntimeException {
      */
     public InconsistentVersioningState(String message) {
         super(message);
        this.versionHistoryNodeId = null;
     }
 
     /**
      * Constructs a new instance of this class with the specified detail
     * message and root cause.
     * message.
      *
     * @param message   the detail message. The detail message is saved for
     *                  later retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param rootCause root cause (or otherwise <code>null</code>)
     * @param versionHistoryNodeId NodeId of the version history that has problems (or otherwise <code>null</code>
      */
    public InconsistentVersioningState(String message, Throwable rootCause) {
    public InconsistentVersioningState(String message, NodeId versionHistoryNodeId, Throwable rootCause) {
         super(message, rootCause);
        this.versionHistoryNodeId = versionHistoryNodeId;
     }
 
    /**
     * @return the NodeId of the version history having problems or <code>null</code>
     * when unknown.
     */
    public NodeId getVersionHistoryNodeId() {
        return this.versionHistoryNodeId;
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
index 429b1f120..c3e42d3fb 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
@@ -213,7 +213,7 @@ class InternalVersionHistoryImpl extends InternalVersionItemImpl
             }
             return v;
         } catch (RepositoryException e) {
            throw new IllegalArgumentException("Failed to create version " + name + ".");
            throw new InconsistentVersioningState("Failed to create version " + name + " in VHR " + historyId + ".", historyId, null);
         }
     }
 
@@ -238,7 +238,7 @@ class InternalVersionHistoryImpl extends InternalVersionItemImpl
                     v = new InternalVersionImpl(this, child, child.getName());
                 }
             } catch (RepositoryException e) {
                throw new InconsistentVersioningState("Version does not have a jcr:frozenNode: " + child.getNodeId(), e);
                throw new InconsistentVersioningState("Version does not have a jcr:frozenNode: " + child.getNodeId(), historyId, e);
             }
         }
         return v;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionImpl.java
index a1c9d20a6..ec79ec0c4 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionImpl.java
@@ -121,7 +121,7 @@ class InternalVersionImpl extends InternalVersionItemImpl
         try {
             return (InternalFrozenNode) vMgr.getItem(getFrozenNodeId());
         } catch (RepositoryException e) {
            throw new InconsistentVersioningState("unable to retrieve frozen node: " + e, e);
            throw new InconsistentVersioningState("unable to retrieve frozen node: " + e, null, e);
         }
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
index da83ede94..3924c87a9 100755
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.core.version;
 
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ACTIVITY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ROOTVERSION;
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_VERSIONHISTORY;
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_VERSIONABLE;
 
@@ -31,6 +32,7 @@ import javax.jcr.version.VersionException;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.NodeIdFactory;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.LocalItemStateManager;
 import org.apache.jackrabbit.core.state.NodeReferences;
@@ -319,12 +321,14 @@ abstract class InternalVersionManagerBase implements InternalVersionManager {
             if (parent != null && parent.hasNode(name)) {
                 NodeStateEx history = parent.getNode(name, 1);
                 if (history == null) {
                    throw new InconsistentVersioningState("Unexpected failure to get child node " + name + " on parent node" + parent.getNodeId());
                    throw new InconsistentVersioningState("Unexpected failure to get child node " + name + " on parent node " + parent.getNodeId());
                }
                ChildNodeEntry rootv = history.getState().getChildNodeEntry(JCR_ROOTVERSION, 1);
                if (rootv == null) {
                    throw new InconsistentVersioningState("missing child node entry for " + JCR_ROOTVERSION + " on version history node " + history.getNodeId());
                 }
                Name root = NameConstants.JCR_ROOTVERSION;
                info = new VersionHistoryInfo(
                        history.getNodeId(),
                        history.getState().getChildNodeEntry(root, 1).getId());
                info = new VersionHistoryInfo(history.getNodeId(),
                        rootv.getId());
             }
         } finally {
             lock.release();
- 
2.19.1.windows.1

