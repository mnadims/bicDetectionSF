From 7bab386783ff590c685d03089b55f5a38797106c Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Thu, 20 Oct 2011 13:46:42 +0000
Subject: [PATCH] JCR-3115: Versioning fixup leaves persistence in a state
 where the node can't be made versionable again

Extend checker so that it gets the same VersionHistoryInfo object that other code gets when versioning gets enabled again.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1186802 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/RepositoryChecker.java    | 16 +++++++++++---
 .../version/InternalVersionManagerBase.java   | 22 +++++++++++++++----
 2 files changed, 31 insertions(+), 7 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
index f0d908137..1df670ffa 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
@@ -41,6 +41,7 @@ import org.apache.jackrabbit.core.version.InconsistentVersioningState;
 import org.apache.jackrabbit.core.version.InternalVersion;
 import org.apache.jackrabbit.core.version.InternalVersionHistory;
 import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.apache.jackrabbit.core.version.VersionHistoryInfo;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.NameFactory;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
@@ -129,14 +130,23 @@ class RepositoryChecker {
             try {
                 log.debug("Checking version history of node {}", nid);
 
                message = "Removing references to a missing version history of node " + nid;
                String intro = "Removing references to an inconsistent version history of node "
                    + nid;

                message = intro + " (getting the VersionInfo)";
                VersionHistoryInfo vhi = versionManager.getVersionHistoryInfoForNode(node);
                if (vhi != null) {
                    // get the version history's node ID as early as possible
                    // so we can attempt a fixup even when the next call fails
                    vhid = vhi.getVersionHistoryId();
                }

                message = intro + " (getting the InternalVersionHistory)";
                 InternalVersionHistory vh = versionManager.getVersionHistoryOfNode(nid);
 
                 vhid = vh.getId();
                 
                 // additional checks, see JCR-3101
                String intro = "Removing references to an inconsistent version history of node "
                    + nid;
 
                 message = intro + " (getting the version names failed)";
                 Name[] versionNames = vh.getVersionNames();
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
index e4fb1045f..ece1702ba 100755
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
@@ -305,11 +305,14 @@ abstract class InternalVersionManagerBase implements InternalVersionManager {
     }
 
     /**
     * {@inheritDoc}
     * Returns information about the version history of the specified node
     * or <code>null</code> when unavailable.
     *
     * @param vNode node whose version history should be returned
     * @return identifiers of the version history and root version nodes
     * @throws RepositoryException if an error occurs
      */
    public VersionHistoryInfo getVersionHistory(Session session, NodeState node,
                                                NodeId copiedFrom)
            throws RepositoryException {
    public VersionHistoryInfo getVersionHistoryInfoForNode(NodeState node) throws RepositoryException {
         VersionHistoryInfo info = null;
 
         VersioningLock.ReadLock lock = acquireReadLock();
@@ -335,6 +338,17 @@ abstract class InternalVersionManagerBase implements InternalVersionManager {
             lock.release();
         }
 
        return info;
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistoryInfo getVersionHistory(Session session, NodeState node,
                                                NodeId copiedFrom)
            throws RepositoryException {
        VersionHistoryInfo info = getVersionHistoryInfoForNode(node);

         if (info == null) {
             info = createVersionHistory(session, node, copiedFrom);
         }
- 
2.19.1.windows.1

