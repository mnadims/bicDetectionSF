From 818cb101757d45853ba53a428c4b60d72d30a03b Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 26 May 2009 10:26:13 +0000
Subject: [PATCH] JCR-134: Unreferenced VersionHistory should be deleted
 automatically
MIME-Version: 1.0
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: 8bit

Applied patch contributed by Sébastien Launay

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@778645 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/version/AbstractVersionManager.java  |  27 ++-
 .../version/InternalVersionHistoryImpl.java   |  23 ++-
 .../core/version/VersionManagerImpl.java      |  17 +-
 .../core/version/XAVersionManager.java        |  17 +-
 .../RemoveOrphanVersionHistoryTest.java       | 154 ++++++++++++++++++
 5 files changed, 220 insertions(+), 18 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/AbstractVersionManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/AbstractVersionManager.java
index f0ff6ebeb..40d9b307f 100755
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/AbstractVersionManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/AbstractVersionManager.java
@@ -40,8 +40,8 @@ import javax.jcr.version.VersionException;
 /**
  * Base implementation of the {@link VersionManager} interface.
  * <p/>
 * All read operations must aquire the read lock before reading, all write
 * operations must aquire the write lock.
 * All read operations must acquire the read lock before reading, all write
 * operations must acquire the write lock.
  */
 abstract class AbstractVersionManager implements VersionManager {
 
@@ -291,7 +291,7 @@ abstract class AbstractVersionManager implements VersionManager {
     /**
      * Returns the item with the given persistent id. Subclass responsibility.
      * <p/>
     * Please note, that the overridden method must aquire the readlock before
     * Please note, that the overridden method must acquire the readlock before
      * reading the state manager.
      *
      * @param id the id of the item
@@ -312,18 +312,31 @@ abstract class AbstractVersionManager implements VersionManager {
 
     /**
      * Checks if there are item references (from outside the version storage)
     * that reference the given version item. Subclass responsiblity.
     * that reference the given node. Subclass responsibility.
      * <p/>
     * Please note, that the overridden method must aquire the readlock before
     * Please note, that the overridden method must acquire the readlock before
      * reading the state manager.
      *
     * @param item version item
     * @param id the id of the node
      * @return <code>true</code> if there are item references from outside the
      *         version storage; <code>false</code> otherwise.
      * @throws RepositoryException if an error occurs while reading from the
      *                             repository.
      */
    protected abstract boolean hasItemReferences(InternalVersionItem item)
    protected abstract boolean hasItemReferences(NodeId id)
            throws RepositoryException;

    /**
     * Returns the node with the given persistent id. Subclass responsibility.
     * <p/>
     * Please note, that the overridden method must acquire the readlock before
     * reading the state manager.
     *
     * @param id the id of the node
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    protected abstract NodeStateEx getNodeStateEx(NodeId id)
             throws RepositoryException;
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
index 39bccec7d..65df0ea01 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
@@ -375,7 +375,7 @@ class InternalVersionHistoryImpl extends InternalVersionItemImpl
             throw new VersionException(msg);
         }
         // check if any references (from outside the version storage) exist on this version
        if (vMgr.hasItemReferences(v)) {
        if (vMgr.hasItemReferences(v.getId())) {
             throw new ReferentialIntegrityException("Unable to remove version. At least once referenced.");
         }
 
@@ -396,8 +396,25 @@ class InternalVersionHistoryImpl extends InternalVersionItemImpl
         nameCache.remove(versionName);
         vMgr.versionDestroyed(v);
 
        // store changes
        node.store();
        // Check if this was the last version in addition to the root version
        if (!vMgr.hasItemReferences(node.getNodeId())) {
            log.debug("Current version history has no references");
            NodeStateEx[] childNodes = node.getChildNodes();

            // Check if there is only root version and version labels nodes
            if (childNodes.length == 2) {
                log.debug("Removing orphan version history as it contains only two children");
                NodeStateEx parentNode = vMgr.getNodeStateEx(node.getParentId());
                // Remove version history node
                parentNode.removeNode(node.getName());
                // store changes for this node and his children
                parentNode.store();
            }
        } else {
            log.debug("Current version history has at least one reference");
            // store changes
            node.store();
        }
 
         // now also remove from labelCache
         for (int i = 0; i < labels.length; i++) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImpl.java
index 2081daacd..744f394ae 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImpl.java
@@ -411,14 +411,19 @@ public class VersionManagerImpl extends AbstractVersionManager implements ItemSt
     /**
      * {@inheritDoc}
      */
    protected boolean hasItemReferences(InternalVersionItem item)
    protected boolean hasItemReferences(NodeId id)
            throws RepositoryException {
        return stateMgr.hasNodeReferences(new NodeReferencesId(id));
    }

    /**
     * {@inheritDoc}
     */
    protected NodeStateEx getNodeStateEx(NodeId parentNodeId)
             throws RepositoryException {
         try {
            NodeReferences refs = stateMgr.getNodeReferences(
                    new NodeReferencesId(item.getId()));
            return refs.hasReferences();
        } catch (NoSuchItemStateException e) {
            return false;
            NodeState state = (NodeState) stateMgr.getItemState(parentNodeId);
            return new NodeStateEx(stateMgr, ntReg, state, null);
         } catch (ItemStateException e) {
             throw new RepositoryException(e);
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/XAVersionManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/XAVersionManager.java
index a8ab7e5b7..5519d41c5 100755
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/XAVersionManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/XAVersionManager.java
@@ -370,9 +370,22 @@ public class XAVersionManager extends AbstractVersionManager
     /**
      * {@inheritDoc}
      */
    protected boolean hasItemReferences(InternalVersionItem item)
    protected boolean hasItemReferences(NodeId id)
             throws RepositoryException {
        return session.getNodeById(item.getId()).getReferences().hasNext();
        return session.getNodeById(id).getReferences().hasNext();
    }

    /**
     * {@inheritDoc}
     */
    protected NodeStateEx getNodeStateEx(NodeId parentNodeId)
            throws RepositoryException {
        try {
            NodeState state = (NodeState) stateMgr.getItemState(parentNodeId);
            return new NodeStateEx(stateMgr, ntReg, state, null);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
     }
 
     /**
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java
new file mode 100644
index 000000000..88c741b28
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java
@@ -0,0 +1,154 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.version;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test case for JCR-134.
 */
public class RemoveOrphanVersionHistoryTest extends AbstractJCRTest {

    /**
     * Test orphan version history cleaning in a single workspace.
     * @throws RepositoryException if an error occurs.
     */
    public void testRemoveOrphanVersionHistory() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        Session session = n.getSession();
        VersionHistory vh = n.getVersionHistory();
        String vhUuid = vh.getUUID();
        assertExists(session, vhUuid);

        // First version
        Version v10 = n.checkin();
        n.checkout();

        // Second version
        Version v11 = n.checkin();
        n.checkout();

        // Remove node
        n.remove();
        testRootNode.save();
        assertExists(session, vhUuid);

        // Remove the first version
        vh.removeVersion(v10.getName());
        assertExists(session, vhUuid);

        // Remove the second and last version
        vh.removeVersion(v11.getName());

        try {
            session.getNodeByUUID(vhUuid);
            fail("Orphan version history must have been removed");
        } catch (ItemNotFoundException e) {
            // Expected
        }
    }

    /**
     * Test orphan version history cleaning in multiple workspace.
     * @throws RepositoryException if an error occurs.
     */
    public void testWorkspaceRemoveOrphanVersionHistory() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        Session session = n.getSession();
        VersionHistory vh = n.getVersionHistory();
        String vhUuid = vh.getUUID();
        assertExists(session, vhUuid);

        // First version
        Version v10 = n.checkin();
        n.checkout();

        Workspace defaultWorkspace = n.getSession().getWorkspace();
        Session otherWsSession = n.getSession().getRepository().login(new SimpleCredentials("superuser", "".toCharArray()), workspaceName);
        // Clone the node in another workspace
        otherWsSession.getWorkspace().clone(defaultWorkspace.getName(), n.getPath(), n.getPath(), false);
        Node otherWsRootNode = otherWsSession.getRootNode();
        Node clonedNode = otherWsRootNode.getNode(n.getPath().substring(1));
        // Ensure that version histories are the same
        assertEquals(vhUuid, clonedNode.getVersionHistory().getUUID());

        Version v11 = clonedNode.checkin();
        clonedNode.checkout();

        // Remove node
        n.remove();
        testRootNode.save();
        assertExists(session, vhUuid);
        assertExists(otherWsSession, vhUuid);

        // Remove the first version
        vh.removeVersion(v10.getName());
        assertExists(session, vhUuid);
        assertExists(otherWsSession, vhUuid);

        // Remove cloned node
        clonedNode.remove();
        otherWsRootNode.save();
        assertExists(session, vhUuid);
        assertExists(otherWsSession, vhUuid);

        // Remove the last version
        vh.removeVersion(v11.getName());

        try {
            session.getNodeByUUID(vhUuid);
            fail("Orphan version history must have been removed from the default workspace");
        } catch (ItemNotFoundException e) {
            // Expected
        }

        try {
            otherWsSession.getNodeByUUID(vhUuid);
            fail("Orphan version history must have been removed from the other workspace");
        } catch (ItemNotFoundException e) {
            // Expected
        }
    }

    /**
     * Assert that a node exists in a session.
     * @param session the session.
     * @param uuid the node's UUID.
     * @throws RepositoryException if an error occurs.
     */
    protected void assertExists(Session session, String uuid) throws RepositoryException
    {
        try {
            session.getNodeByUUID(uuid);
        } catch (ItemNotFoundException e) {
            fail("Unknown uuid: " + uuid);
        }
    }
}
- 
2.19.1.windows.1

