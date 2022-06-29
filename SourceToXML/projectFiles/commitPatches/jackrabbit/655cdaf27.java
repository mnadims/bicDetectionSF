From 655cdaf2733a0446ad7e0a58d4ada74d308294ab Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Mon, 21 Sep 2009 08:31:17 +0000
Subject: [PATCH] JCR-2170: Remove PropDefId and NodeDefId - fix regression and
 add test case

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@817192 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/core/ItemManager.java   | 49 ++++++++++++++++---
 .../apache/jackrabbit/core/ReplaceTest.java   | 42 ++++++++++++++++
 .../org/apache/jackrabbit/core/TestAll.java   |  1 +
 3 files changed, 84 insertions(+), 8 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplaceTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
index e04e67b8a..2908c5e93 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
@@ -29,6 +29,7 @@ import javax.jcr.NodeIterator;
 import javax.jcr.PathNotFoundException;
 import javax.jcr.PropertyIterator;
 import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
 
 import org.apache.commons.collections.map.ReferenceMap;
 import org.apache.jackrabbit.core.id.ItemId;
@@ -54,6 +55,8 @@ import org.apache.jackrabbit.spi.Path;
 import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -163,7 +166,7 @@ public class ItemManager implements Dumpable, ItemStateListener {
         shareableNodesCache.clear();
     }
 
    org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl getDefinition(NodeState state)
    NodeDefinitionImpl getDefinition(NodeState state)
             throws RepositoryException {
         if (state.getId().equals(rootNodeId)) {
             // special handling required for root node
@@ -176,18 +179,28 @@ public class ItemManager implements Dumpable, ItemStateListener {
             // get from overlayed state
             parentId = state.getOverlayedState().getParentId();
         }
        NodeState parentState;
        NodeState parentState = null;
         try {
             NodeImpl parent = (NodeImpl) getItem(parentId);
             parentState = parent.getNodeState();
             if (state.getParentId() == null) {
                 // indicates state has been removed, must use
                 // overlayed state of parent, otherwise child node entry
                // cannot be found
                parentState = (NodeState) parentState.getOverlayedState();
                // cannot be found. unless the parentState is new, which
                // means it was recreated in place of a removed node
                // that used to be the actual parent
                if (parentState.getStatus() == ItemState.STATUS_NEW) {
                    // force getting parent from attic
                    parentState = null;
                } else {
                    parentState = (NodeState) parentState.getOverlayedState();
                }
             }
         } catch (ItemNotFoundException e) {
            // parent probably removed, get it from attic
            // parent probably removed, get it from attic. see below
        }

        if (parentState == null) {
             try {
                 // use overlayed state if available
                 parentState = (NodeState) sism.getAttic().getItemState(
@@ -196,21 +209,32 @@ public class ItemManager implements Dumpable, ItemStateListener {
                 throw new RepositoryException(ex);
             }
         }

         // get child node entry
         ChildNodeEntry cne = parentState.getChildNodeEntry(state.getNodeId());
         NodeTypeRegistry ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
         try {
             EffectiveNodeType ent = ntReg.getEffectiveNodeType(
                     parentState.getNodeTypeName(), parentState.getMixinTypeNames());
            QNodeDefinition def = ent.getApplicableChildNodeDef(
            QNodeDefinition def;
            try {
                def = ent.getApplicableChildNodeDef(
                     cne.getName(), state.getNodeTypeName(), ntReg);
            } catch (ConstraintViolationException e) {
                // fallback to child node definition of a nt:unstructured
                ent = ntReg.getEffectiveNodeType(NameConstants.NT_UNSTRUCTURED);
                def = ent.getApplicableChildNodeDef(
                        cne.getName(), state.getNodeTypeName(), ntReg);
                log.warn("Fallback to nt:unstructured due to unknown child " +
                        "node definition for type '" + state.getNodeTypeName() + "'");
            }
             return session.getNodeTypeManager().getNodeDefinition(def);
         } catch (NodeTypeConflictException e) {
             throw new RepositoryException(e);
         }
     }
 
    org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl getDefinition(PropertyState state)
    PropertyDefinitionImpl getDefinition(PropertyState state)
             throws RepositoryException {
         try {
             NodeImpl parent = (NodeImpl) getItem(state.getParentId());
@@ -225,8 +249,17 @@ public class ItemManager implements Dumpable, ItemStateListener {
             NodeTypeRegistry ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
             EffectiveNodeType ent = ntReg.getEffectiveNodeType(
                     parent.getNodeTypeName(), parent.getMixinTypeNames());
            QPropertyDefinition def = ent.getApplicablePropertyDef(
            QPropertyDefinition def;
            try {
                def = ent.getApplicablePropertyDef(
                     state.getName(), state.getType(), state.isMultiValued());
            } catch (ConstraintViolationException e) {
                ent = ntReg.getEffectiveNodeType(NameConstants.NT_UNSTRUCTURED);
                def = ent.getApplicablePropertyDef(state.getName(),
                        state.getType(), state.isMultiValued());
                log.warn("Fallback to nt:unstructured due to unknown property " +
                        "definition for '" + state.getName() + "'");
            }
             return session.getNodeTypeManager().getPropertyDefinition(def);
         } catch (ItemStateException e) {
             throw new RepositoryException(e);
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplaceTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplaceTest.java
new file mode 100644
index 000000000..581489f3f
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplaceTest.java
@@ -0,0 +1,42 @@
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

import javax.jcr.RepositoryException;
import javax.jcr.Node;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>ReplaceTest</code> checks if the node definition for a removed node
 * is correctly calculated when its parent node had been replaced with a new
 * node and the uuid is still the same.
 */
public class ReplaceTest extends AbstractJCRTest {

    public void testReplace() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        n1.addMixin(mixReferenceable);
        String uuid = n1.getIdentifier();
        n1.addNode("node2");
        superuser.save();

        n1.remove();
        ((NodeImpl) testRootNode).addNodeWithUuid("node1", uuid);
        superuser.save();
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
index 85ed00bc7..4768524ca 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
@@ -44,6 +44,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(InvalidDateTest.class);
         suite.addTestSuite(SessionGarbageCollectedTest.class);
         suite.addTestSuite(ReferencesTest.class);
        suite.addTestSuite(ReplaceTest.class);
 
         // test related to NodeStateMerger
         // temporarily disabled see JCR-2272 and JCR-2295
- 
2.19.1.windows.1

