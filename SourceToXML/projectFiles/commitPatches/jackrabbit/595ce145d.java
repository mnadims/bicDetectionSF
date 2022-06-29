From 595ce145def7c72181e50dd78780540e1bb9f0d0 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Fri, 21 Oct 2011 13:49:55 +0000
Subject: [PATCH] JCR-3115: Versioning fixup leaves persistence in a state
 where the node can't be made versionable again

Modify checker to also inspect "candidate" version histories.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1187344 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/RepositoryChecker.java    | 90 ++++++++++++-------
 1 file changed, 56 insertions(+), 34 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
index 1df670ffa..3113a9ea2 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
@@ -28,6 +28,7 @@ import java.util.Calendar;
 import java.util.HashSet;
 import java.util.Set;
 
import javax.jcr.ItemNotFoundException;
 import javax.jcr.RepositoryException;
 
 import org.apache.jackrabbit.core.id.NodeId;
@@ -122,28 +123,49 @@ class RepositoryChecker {
     }
 
     private void checkVersionHistory(NodeState node) {
        if (node.hasPropertyName(JCR_VERSIONHISTORY)) {
            String message = null;
            NodeId nid = node.getNodeId();
            NodeId vhid = null;
 
            try {
                log.debug("Checking version history of node {}", nid);
        String message = null;
        NodeId nid = node.getNodeId();
        boolean isVersioned = node.hasPropertyName(JCR_VERSIONHISTORY);
 
                String intro = "Removing references to an inconsistent version history of node "
                    + nid;
        NodeId vhid = null;
 
                message = intro + " (getting the VersionInfo)";
                VersionHistoryInfo vhi = versionManager.getVersionHistoryInfoForNode(node);
                if (vhi != null) {
                    // get the version history's node ID as early as possible
                    // so we can attempt a fixup even when the next call fails
                    vhid = vhi.getVersionHistoryId();
                }
        try {
            String type = isVersioned ? "in-use" : "candidate";
            
            log.debug("Checking " + type + " version history of node {}", nid);

            String intro = "Removing references to an inconsistent " + type
                    + " version history of node " + nid;

            message = intro + " (getting the VersionInfo)";
            VersionHistoryInfo vhi = versionManager.getVersionHistoryInfoForNode(node);
            if (vhi != null) {
                // get the version history's node ID as early as possible
                // so we can attempt a fixup even when the next call fails
                vhid = vhi.getVersionHistoryId();
            }

            message = intro + " (getting the InternalVersionHistory)";
 
                message = intro + " (getting the InternalVersionHistory)";
                InternalVersionHistory vh = versionManager.getVersionHistoryOfNode(nid);
            InternalVersionHistory vh = null;
            
            try {
                vh = versionManager.getVersionHistoryOfNode(nid);
            }
            catch (ItemNotFoundException ex) {
                // it's ok if we get here if the node didn't claim to be versioned
                if (isVersioned) {
                    throw ex;
                }
            }
 
            if (vh == null) {
                if (isVersioned) {
                    message = intro + "getVersionHistoryOfNode returned null";
                    throw new InconsistentVersioningState(message);    
                }
            } else { 
                 vhid = vh.getId();
                 
                 // additional checks, see JCR-3101
@@ -162,34 +184,34 @@ class RepositoryChecker {
 
                     message = intro + "(frozen node of root version " + v.getId() + " missing)";
                     if (null == v.getFrozenNode()) {
                        throw new InconsistentVersioningState("frozen node of "
                                + v.getId() + " is missing.");
                        throw new InconsistentVersioningState(message);
                     }
                 }
 
                 if (!seenRoot) {
                     message = intro + " (root version is missing)";
                    throw new InconsistentVersioningState("root version of " + nid +" is missing.");
                    throw new InconsistentVersioningState(message);
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
        } catch (InconsistentVersioningState e) {
            log.info(message, e);
            NodeId nvhid = e.getVersionHistoryNodeId();
            if (nvhid != null) {
                if (vhid != null && !nvhid.equals(vhid)) {
                    log.error("vhrid returned with InconsistentVersioningState does not match the id we already had: "
                            + vhid + " vs " + nvhid);
                 }
                removeVersionHistoryReferences(node, vhid);
            } catch (Exception e) {
                log.info(message, e);
                removeVersionHistoryReferences(node, vhid);
                vhid = nvhid; 
             }
            removeVersionHistoryReferences(node, vhid);
        } catch (Exception e) {
            log.info(message, e);
            removeVersionHistoryReferences(node, vhid);
         }
     }
 
    private void removeVersionHistoryReferences(NodeState node, NodeId vhid) {
    // un-versions the node, and potentially moves the version history away
    private void removeVersionHistoryReferences(NodeState node,  NodeId vhid) {
         NodeState modified =
             new NodeState(node, NodeState.STATUS_EXISTING_MODIFIED, true);
 
- 
2.19.1.windows.1

