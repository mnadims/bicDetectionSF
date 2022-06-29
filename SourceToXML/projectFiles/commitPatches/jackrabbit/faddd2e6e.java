From faddd2e6e957d1c2303381d4ec1d08b6434209ab Mon Sep 17 00:00:00 2001
From: Tobias Bocanegra <tripod@apache.org>
Date: Thu, 22 Apr 2010 09:11:50 +0000
Subject: [PATCH] JCR-2613 NoSuchItemStateException on checkin after
 removeVersion in XA Environment

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@936668 13f79535-47bb-0310-9956-ffa450edef68
--
 .../version/InternalVersionHistoryImpl.java   |   6 +-
 .../core/version/RemoveVersionTest.java       | 166 ++++++++++++++++++
 2 files changed, 171 insertions(+), 1 deletion(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveVersionTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
index d936b4661..4f0d2952f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionHistoryImpl.java
@@ -315,6 +315,8 @@ class InternalVersionHistoryImpl extends InternalVersionItemImpl
     /**
      * Returns the version from cache, or <code>null</code> if it is not
      * present.
     * @param id the id of the version
     * @return the version or <code>null</code> if not cached.
      */
     private synchronized InternalVersion getCachedVersion(NodeId id) {
         InternalVersion v = versionCache.get(id);
@@ -442,6 +444,8 @@ class InternalVersionHistoryImpl extends InternalVersionItemImpl
                 parentNode.removeNode(node.getName());
                 // store changes for this node and his children
                 parentNode.store();
            } else {
                node.store();
             }
         } else {
             log.debug("Current version history has at least one reference");
@@ -464,7 +468,7 @@ class InternalVersionHistoryImpl extends InternalVersionItemImpl
      * or <code>null</code> of the label was not moved.
      *
      * @param versionName the name of the version
     * @param label the label to assgign
     * @param label the label to assign
      * @param move  flag what to do by collisions
      * @return the version that was previously assigned by this label or <code>null</code>.
      * @throws VersionException if the version does not exist or if the label is already defined.
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveVersionTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveVersionTest.java
new file mode 100644
index 000000000..69916d880
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveVersionTest.java
@@ -0,0 +1,166 @@
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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.transaction.UserTransaction;

import org.apache.jackrabbit.core.UserTransactionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Tests if removing of versions works correctly
 */
public class RemoveVersionTest  extends AbstractJCRTest {

    /**
     * Removes a version in 1 transaction and tries to commit afterwards the
     * versionable node using a 2nd transaction.
     *
     * Tests error reported in JCR-2613
     *
     * @throws Exception if an error occurs
     */
    public void testRemoveVersionAndCheckinXA() throws Exception {
        UserTransaction tx = new UserTransactionImpl(superuser);
        tx.begin();
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        n.addMixin(mixReferenceable);
        testRootNode.save();
        String uuid = n.getUUID();
        // create two versions
        String v1 = n.checkin().getName();
        n.checkout();
        n.checkin();
        n.checkout();
        tx.commit();

        tx = new UserTransactionImpl(superuser);
        tx.begin();
        // remove one version
        n = superuser.getNodeByUUID(uuid);
        n.getVersionHistory().removeVersion(v1);
        n.save();
        tx.commit();

        // new session
        Session session = getHelper().getSuperuserSession();
        tx = new UserTransactionImpl(session);
        tx.begin();
        n = session.getNodeByUUID(uuid);
        n.checkin();
        tx.commit();
    }

    /**
     * Removes a version in 1 transaction and tries to commit afterwards the
     * versionable node using a 2nd transaction. Uses the JCR2.0 API.
     *
     * Tests error reported in JCR-2613
     * 
     * @throws Exception if an error occurs
     */
    public void testRemoveVersionAndCheckinXA_JCR2() throws Exception {
        UserTransaction tx = new UserTransactionImpl(superuser);
        tx.begin();
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        superuser.save();

        // create two versions
        String path = n.getPath();
        String v1 = superuser.getWorkspace().getVersionManager().checkpoint(path).getName();
        String v2 = superuser.getWorkspace().getVersionManager().checkpoint(path).getName();
        tx.commit();

        tx = new UserTransactionImpl(superuser);
        tx.begin();
        // remove one version
        superuser.getWorkspace().getVersionManager().getVersionHistory(path).removeVersion(v1);
        tx.commit();

        // new session
        Session session = getHelper().getSuperuserSession();
        tx = new UserTransactionImpl(session);
        tx.begin();
        session.getWorkspace().getVersionManager().checkin(path);
        tx.commit();
    }

    /**
     * Creates 3 versions and removes them afterwards. Checks if version history
     * was purged, too.
     *
     * Tests error reported in JCR-2601
     *
     * @throws Exception if an error occurs
     */
    public void testRemoveAllVersions() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        superuser.save();
        String path = n.getPath();

        // create some versions
        VersionManager mgr = superuser.getWorkspace().getVersionManager();
        mgr.checkpoint(path); // v1.0
        mgr.checkpoint(path); // v1.1
        mgr.checkpoint(path); // v1.2

        // get version history
        VersionHistory vh = mgr.getVersionHistory(path);
        String id = vh.getIdentifier();

        // remove versionable node
        n.remove();
        superuser.save();

        // get the names of the versions
        List<String> names = new LinkedList<String>();
        VersionIterator vit = vh.getAllVersions();
        while (vit.hasNext()) {
            Version v = vit.nextVersion();
            if (!v.getName().equals("jcr:rootVersion")) {
                names.add(v.getName());
            }
        }
        assertEquals("Number of versions", 3, names.size());
        
        // remove all versions
        for (String name: names) {
            vh.removeVersion(name);
        }

        // assert that version history is gone
        try {
            superuser.getNodeByIdentifier(id);
            fail("Version history not removed after last version was removed.");
        } catch (RepositoryException e) {
            // ok
        }
    }
}
\ No newline at end of file
- 
2.19.1.windows.1

