From e53b2b46eea1a1d8e74f34db5c5a9c6da8b0acfe Mon Sep 17 00:00:00 2001
From: Stefan Guggisberg <stefan@apache.org>
Date: Thu, 26 Nov 2009 14:04:02 +0000
Subject: [PATCH] JCR-2408: minor fix and added test case

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@884562 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/NodeImpl.java  |  2 +-
 .../apache/jackrabbit/core/NodeImplTest.java  | 32 +++++++++++++++++++
 2 files changed, 33 insertions(+), 1 deletion(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
index 3714fdc53..02a1c36f6 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
@@ -1074,7 +1074,7 @@ public class NodeImpl extends ItemImpl implements Node {
                 PropertyImpl prop = (PropertyImpl) itemMgr.getItem(id);
                 PropertyDefinition oldDef = affectedProps.get(id);
 
                if (prop.getDefinition().isProtected()) {
                if (oldDef.isProtected()) {
                     // remove 'orphaned' protected properties immediately
                     removeChildProperty(id.getName());
                     continue;
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
index 4098dceed..d0b3d7572 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
@@ -200,4 +200,36 @@ public class NodeImplTest extends AbstractJCRTest {
         assertEquals(PropertyType.nameFromValue(PropertyType.LONG),
                 PropertyType.nameFromValue(p.getType()));
     }

    /**
     * Test case for JCR-2130 and JCR-2408.
     *
     * @throws RepositoryException
     */
    public void testAddRemoveMixin() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, "nt:folder");
        n.addMixin("mix:title");
        n.setProperty("jcr:title", "blah blah");
        testRootNode.getSession().save();

        n.removeMixin("mix:title");
        testRootNode.getSession().save();
        assertFalse(n.hasProperty("jcr:title"));

        Node n1 = testRootNode.addNode(nodeName2, ntUnstructured);
        n1.addMixin("mix:title");
        n1.setProperty("jcr:title", "blah blah");
        assertEquals(
                n1.getProperty("jcr:title").getDefinition().getDeclaringNodeType().getName(),
                "mix:title");
        testRootNode.getSession().save();

        n1.removeMixin("mix:title");
        testRootNode.getSession().save();
        assertTrue(n1.hasProperty("jcr:title"));

        assertEquals(
                n1.getProperty("jcr:title").getDefinition().getDeclaringNodeType().getName(),
                ntUnstructured);
    }
 }
- 
2.19.1.windows.1

