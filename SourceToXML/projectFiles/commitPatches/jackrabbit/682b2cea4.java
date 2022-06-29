From 682b2cea472aa21820b62dfa9b10d81ffa38bd32 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 26 May 2009 14:36:38 +0000
Subject: [PATCH] JCR-134: Unreferenced VersionHistory should be deleted
 automatically

Added a test case that makes sure that a version history that is still being referenced from another workspace won't get lost when the last version is removed.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@778720 13f79535-47bb-0310-9956-ffa450edef68
--
 .../RemoveOrphanVersionHistoryTest.java       | 53 +++++++++++++++++++
 1 file changed, 53 insertions(+)

diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java
index 88c741b28..9bb700bd6 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/version/RemoveOrphanVersionHistoryTest.java
@@ -18,6 +18,8 @@ package org.apache.jackrabbit.core.version;
 
 import javax.jcr.ItemNotFoundException;
 import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import javax.jcr.SimpleCredentials;
@@ -137,6 +139,57 @@ public class RemoveOrphanVersionHistoryTest extends AbstractJCRTest {
         }
     }
 
    /**
     * Test that an emptied version history that is still being referenced
     * from another workspace does not get removed.
     *
     * @throws RepositoryException if an error occurs.
     */
    public void testEmptyNonOrphanVersionHistory() throws RepositoryException {
        Session session = testRootNode.getSession();

        // Create versionable test node
        Node node = testRootNode.addNode(nodeName1);
        node.addMixin(mixVersionable);
        session.save();

        VersionHistory history = node.getVersionHistory();
        String uuid = history.getUUID();

        // Create version 1.0
        Version v10 = node.checkin();

        // Remove the test node
        node.checkout();
        node.remove();
        session.save();

        Session otherSession = helper.getReadWriteSession(workspaceName);
        try {
            // create a reference to the version history in another workspace
            Node otherRoot = otherSession.getRootNode();
            Property reference = otherRoot.setProperty(
                    "RemoveOrphanVersionTest", uuid, PropertyType.REFERENCE);
            otherSession.save();

            // Now remove the contents of the version history
            history.removeVersion(v10.getName());

            // Check that the version history still exists!
            try {
                session.getNodeByUUID(uuid);
            } catch (ItemNotFoundException e) {
                fail("Referenced empty version history must note be removed");
            }

            // Cleanup
            reference.remove();
            otherSession.save();
        } finally {
            otherSession.logout();
        }
    }

     /**
      * Assert that a node exists in a session.
      * @param session the session.
- 
2.19.1.windows.1

