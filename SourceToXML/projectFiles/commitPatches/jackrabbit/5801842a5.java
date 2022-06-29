From 5801842a5cd9ac9f9b533ddbc97c7bfa658d4c25 Mon Sep 17 00:00:00 2001
From: Martijn Hendriks <martijnh@apache.org>
Date: Mon, 29 Jun 2009 07:46:50 +0000
Subject: [PATCH] JCR-2138 Prevent persistence of faulty back-references

* Added testcases and a fix in the SharedItemStateManager.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@789245 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/state/SharedItemStateManager.java    |  28 ++-
 .../jackrabbit/core/ReferencesTest.java       | 216 ++++++++++++++++++
 .../org/apache/jackrabbit/core/TestAll.java   |   1 +
 3 files changed, 233 insertions(+), 12 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReferencesTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index a16ff0b3a..0d32cf219 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -874,7 +874,15 @@ public class SharedItemStateManager
         private void updateReferences() throws ItemStateException {
             // process added REFERENCE properties
             for (Iterator<ItemState> i = local.addedStates(); i.hasNext();) {
                addReferences(i.next());
                ItemState state = i.next();
                if (!state.isNode()) {
                    // remove refs from the target which have been added externally (JCR-2138)
                    if (hasItemState(state.getId())) {
                        removeReferences(getItemState(state.getId()));
                    }
                    // add new references to the target
                    addReferences((PropertyState) state);
                }
             }
 
             // process modified REFERENCE properties
@@ -884,7 +892,7 @@ public class SharedItemStateManager
                     // remove old references from the target
                     removeReferences(getItemState(state.getId()));
                     // add new references to the target
                    addReferences(state);
                    addReferences((PropertyState) state);
                 }
             }
 
@@ -894,16 +902,12 @@ public class SharedItemStateManager
             }
         }
 
        private void addReferences(ItemState state)
                throws NoSuchItemStateException, ItemStateException {
            if (!state.isNode()) {
                PropertyState property = (PropertyState) state;
                if (property.getType() == PropertyType.REFERENCE) {
                    InternalValue[] values = property.getValues();
                    for (int i = 0; values != null && i < values.length; i++) {
                        addReference(
                                property.getPropertyId(), values[i].getUUID());
                    }
        private void addReferences(PropertyState property) throws NoSuchItemStateException,
                ItemStateException {
            if (property.getType() == PropertyType.REFERENCE) {
                InternalValue[] values = property.getValues();
                for (int i = 0; values != null && i < values.length; i++) {
                    addReference(property.getPropertyId(), values[i].getUUID());
                 }
             }
         }
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReferencesTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReferencesTest.java
new file mode 100644
index 000000000..28f32f8cb
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReferencesTest.java
@@ -0,0 +1,216 @@
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
package org.apache.jackrabbit.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * 
 */
public final class ReferencesTest extends AbstractJCRTest {

    private String uniquePrefix;

    private static int cnt = 0;

    /**
     * {@inheritDoc}
     */
    public void setUp() throws Exception {
        uniquePrefix = "referencesTest" + System.currentTimeMillis() + "-" + cnt;
        cnt++;
        Session session = createSession();
        Node a = getTestRootNode(session).addNode("A");
        Node b = a.addNode("B");
        a.addMixin("mix:referenceable");
        b.addMixin("mix:referenceable");
        getTestRootNode(session).addNode("C");
        saveAndlogout(session);
    }

    /**
     * Tries to create a double back-reference to "ref to B" property.
     * 
     * @throws Exception on test error
     */
    public void testDoubleBackReference() throws Exception {
        Session session1 = createSession();
        Node bses1 = getTestRootNode(session1).getNode("A").getNode("B");
        getTestRootNode(session1).getNode("C").setProperty("ref to B", bses1);

        Session session2 = createSession();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B", bses2);

        saveAndlogout(session1, session2);
        assertRemoveTestNodes();
    }

    /**
     * Tries to create a single back-reference to "ref to B" property which does not exist.
     * 
     * @throws Exception on test error
     */
    public void testBackRefToNonExistingProp() throws Exception {
        Session session2 = createSession();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B", bses2);

        Session session3 = createSession();
        getTestRootNode(session3).getNode("C").setProperty("ref to B", new Value[]{});

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Tries to create a single back-reference to "ref" property for both A and B whereas "ref" is single
     * valued and points to A.
     * 
     * @throws Exception on test error
     */
    public void testMisdirectedBackRef() throws Exception {
        Session session2 = createSession();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref", bses2);

        Session session3 = createSession();
        Node ases3 = getTestRootNode(session3).getNode("A");
        getTestRootNode(session3).getNode("C").setProperty("ref", ases3);

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Variant of {@link #testDoubleBackReference()} for mult-valued props.
     * 
     * @throws Exception on test error
     */
    public void testDoubleBackRefReferenceMultiValued() throws Exception {
        Session session2 = createSession();
        ValueFactory valFac2 = session2.getValueFactory();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B",
            new Value[]{valFac2.createValue(bses2), valFac2.createValue(bses2)});

        Session session3 = createSession();
        ValueFactory valFac3 = session3.getValueFactory();
        Node bses3 = getTestRootNode(session3).getNode("A").getNode("B");
        getTestRootNode(session3).getNode("C").setProperty("ref to B",
            new Value[]{valFac3.createValue(bses3), valFac3.createValue(bses3)});

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Variant of {@link #testMisdirectedBackRef()} for multi-valued props.
     * 
     * @throws Exception on test error
     */
    public void testMisdirectedBackRefMultiValued() throws Exception {
        Session session2 = createSession();
        ValueFactory valFac2 = session2.getValueFactory();
        Node ases2 = getTestRootNode(session2).getNode("A");
        getTestRootNode(session2).getNode("C").setProperty("ref",
            new Value[]{valFac2.createValue(ases2), valFac2.createValue(ases2)});

        Session session3 = createSession();
        ValueFactory valFac3 = session3.getValueFactory();
        Node bses3 = getTestRootNode(session3).getNode("A").getNode("B");
        getTestRootNode(session3).getNode("C").setProperty("ref", new Value[]{valFac3.createValue(bses3)});

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Regular references usage.
     * 
     * @throws Exception on test error
     */
    public void testRegularReference() throws Exception {
        Session session1 = createSession();
        Node bses1 = getTestRootNode(session1).getNode("A").getNode("B");
        getTestRootNode(session1).getNode("A").setProperty("ref to B", bses1);

        Session session2 = createSession();
        ValueFactory valFac2 = session2.getValueFactory();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B",
            new Value[]{valFac2.createValue(bses2), valFac2.createValue(bses2)});
        getTestRootNode(session2).getNode("C").setProperty("another ref to B", bses2);

        saveAndlogout(session1, session2);
        assertRemoveTestNodes();
    }

    private void assertRemoveTestNodes() throws RepositoryException {
        Session session = createSession();
        getTestRootNode(session).remove();
        assertSave(session);
        session.logout();
    }

    /**
     * @param session the session to save
     */
    private void assertSave(Session session) {
        try {
            session.save();
        } catch (RepositoryException e) {
            fail("saving session failed: " + e.getMessage());
        }
    }

    /**
     * @return a super user session
     * @throws RepositoryException on error
     */
    private Session createSession() throws RepositoryException {
        return helper.getSuperuserSession();
    }

    private void saveAndlogout(Session... sessions) throws RepositoryException {
        if (sessions != null) {
            for (Session session : sessions) {
                session.save();
                session.logout();
            }
        }
    }

    /**
     * @param session the session to use
     * @return a node which is more or less unique per testcase
     * @throws RepositoryException on error
     */
    private Node getTestRootNode(Session session) throws RepositoryException {
        if (session.getRootNode().hasNode(uniquePrefix)) {
            return session.getRootNode().getNode(uniquePrefix);
        } else {
            return session.getRootNode().addNode(uniquePrefix);
        }
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
index f503e100d..ad9d98273 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
@@ -41,6 +41,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(RetentionRegistryImplTest.class);
         suite.addTestSuite(InvalidDateTest.class);
         suite.addTestSuite(SessionGarbageCollectedTest.class);
        suite.addTestSuite(ReferencesTest.class);
 
         return suite;
     }
- 
2.19.1.windows.1

