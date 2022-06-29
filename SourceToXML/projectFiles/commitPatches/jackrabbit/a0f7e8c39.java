From a0f7e8c390bf599f67b7ce84e03de412f6bf053d Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Thu, 7 Jan 2010 17:46:51 +0000
Subject: [PATCH] JCR-2408: Mixin removal exception

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@896940 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/ItemImpl.java  |  16 +-
 .../apache/jackrabbit/core/ItemManager.java   |  13 +
 .../org/apache/jackrabbit/core/NodeImpl.java  | 229 +++++++++---------
 .../apache/jackrabbit/core/NodeImplTest.java  |  72 +++++-
 4 files changed, 208 insertions(+), 122 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
index 656060c03..da40f2b87 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
@@ -606,10 +606,18 @@ public abstract class ItemImpl implements Item {
         // walk through list of removed transient items and check REMOVE permission
         for (ItemState itemState : removed) {
             QItemDefinition def;
            if (itemState.isNode()) {
                def = itemMgr.getDefinition((NodeState) itemState).unwrap();
            } else {
                def = itemMgr.getDefinition((PropertyState) itemState).unwrap();
            try {
                if (itemState.isNode()) {
                    def = itemMgr.getDefinition((NodeState) itemState).unwrap();
                } else {
                    def = itemMgr.getDefinition((PropertyState) itemState).unwrap();
                }
            } catch (ConstraintViolationException e) {
                // since identifier of assigned definition is not stored anymore
                // with item state (see JCR-2170), correct definition cannot be
                // determined for items which have been removed due to removal
                // of a mixin (see also JCR-2130 & JCR-2408)
                continue;
             }
             if (!def.isProtected()) {
                 Path path = stateMgr.getAtticAwareHierarchyMgr().getPath(itemState.getId());
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
index 877c89918..3c6279a3e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
@@ -239,6 +239,19 @@ public class ItemManager implements Dumpable, ItemStateListener {
 
     PropertyDefinitionImpl getDefinition(PropertyState state)
             throws RepositoryException {
        // this is a bit ugly
        // there might be cases where otherwise protected items turn into
        // non-protected items because a mixin has been removed from the parent
        // node state.
        // see also: JCR-2408
        if (state.getStatus() == ItemState.STATUS_EXISTING_REMOVED
                && state.getName().equals(NameConstants.JCR_UUID)) {
            NodeTypeRegistry ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
            QPropertyDefinition def = ntReg.getEffectiveNodeType(
                    NameConstants.MIX_REFERENCEABLE).getApplicablePropertyDef(
                    state.getName(), state.getType());
            return session.getNodeTypeManager().getPropertyDefinition(def);
        }
         try {
             // retrieve parent in 2 steps in order to avoid the check for
             // read permissions on the parent which isn't required in order
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
index dd5beea73..8c57e97d4 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
@@ -24,9 +24,11 @@ import java.util.BitSet;
 import java.util.Calendar;
 import java.util.Collection;
 import java.util.Collections;
import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
import java.util.Map;
 import java.util.Set;
 
 import javax.jcr.AccessDeniedException;
@@ -76,8 +78,8 @@ import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
 import org.apache.jackrabbit.core.query.QueryManagerImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.core.state.ItemState;
 import org.apache.jackrabbit.core.state.ItemStateException;
@@ -88,27 +90,22 @@ import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
 import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
 import org.apache.jackrabbit.spi.commons.conversion.NameException;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.*;
 import org.apache.jackrabbit.spi.commons.name.PathBuilder;
 import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.util.ChildrenCollectorFilter;
 import org.apache.jackrabbit.util.ISO9075;
 import org.apache.jackrabbit.value.ValueHelper;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ISCHECKEDOUT;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_LIFECYCLE_POLICY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_CURRENT_LIFECYCLE_STATE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_LIFECYCLE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_REFERENCEABLE;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;

 /**
  * <code>NodeImpl</code> implements the <code>Node</code> interface.
  */
@@ -1016,127 +1013,143 @@ public class NodeImpl extends ItemImpl implements Node {
             throw new ConstraintViolationException(mixinName + " can not be removed: the node is locked.");
         }
 

        // modify the state of this node
         NodeState thisState = (NodeState) getOrCreateTransientItemState();
        thisState.setMixinTypeNames(remainingMixins);

        // set jcr:mixinTypes property
        setMixinTypesProperty(remainingMixins);

        // shortcut
        if (mixin.getChildNodeDefinitions().length == 0
                && mixin.getPropertyDefinitions().length == 0) {
            // the node type has neither property nor child node definitions,
            // i.e. we're done
            return;
        }
 
        // walk through properties and child nodes and remove those that aren't
        // accomodated by the resulting new effective node type (see JCR-2130)
        boolean success = false;
        // collect information about properties and nodes which require
        // further action as a result of the mixin removal;
        // we need to do this *before* actually changing the assigned the mixin types,
        // otherwise we wouldn't be able to retrieve the current definition
        // of an item.
        Map<PropertyId, PropertyDefinition> affectedProps = new HashMap<PropertyId, PropertyDefinition>();
        Map<ChildNodeEntry, NodeDefinition> affectedNodes = new HashMap<ChildNodeEntry, NodeDefinition>();
         try {
            // use temp set to avoid ConcurrentModificationException
            HashSet<Name> set = new HashSet<Name>(thisState.getPropertyNames());
            for (Name propName : set) {
                PropertyState propState = (PropertyState) stateMgr.getItemState(new PropertyId(thisState.getNodeId(), propName));
            Set<Name> names = thisState.getPropertyNames();
            for (Name propName : names) {
                PropertyId propId = new PropertyId(thisState.getNodeId(), propName);
                PropertyState propState = (PropertyState) stateMgr.getItemState(propId);
                PropertyDefinition oldDef = itemMgr.getDefinition(propState);
                 // check if property has been defined by mixin type (or one of its supertypes)
                PropertyDefinition def = itemMgr.getDefinition(propState);
                NodeTypeImpl declaringNT = (NodeTypeImpl) def.getDeclaringNodeType();
                NodeTypeImpl declaringNT = (NodeTypeImpl) oldDef.getDeclaringNodeType();
                 if (!entResulting.includesNodeType(declaringNT.getQName())) {
                     // the resulting effective node type doesn't include the
                     // node type that declared this property

                    // try to find new applicable definition first and
                    // redefine property if possible (JCR-2130)
                    try {
                        PropertyImpl prop = (PropertyImpl) itemMgr.getItem(propState.getId());
                        if (prop.getDefinition().isProtected()) {
                            // remove 'orphaned' protected properties immediately
                            removeChildProperty(propName);
                            continue;
                        }
                        PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                                propName, propState.getType(),
                                propState.isMultiValued(), false);
                        if (pdi.getRequiredType() != PropertyType.UNDEFINED
                                && pdi.getRequiredType() != propState.getType()) {
                            // value conversion required
                            if (propState.isMultiValued()) {
                                // convert value
                                Value[] values =
                                        ValueHelper.convert(
                                                prop.getValues(),
                                                pdi.getRequiredType(),
                                                session.getValueFactory());
                                // redefine property
                                prop.onRedefine(pdi.unwrap());
                                // set converted values
                                prop.setValue(values);
                            } else {
                                // convert value
                                Value value =
                                        ValueHelper.convert(
                                                prop.getValue(),
                                                pdi.getRequiredType(),
                                                session.getValueFactory());
                                // redefine property
                                prop.onRedefine(pdi.unwrap());
                                // set converted values
                                prop.setValue(value);
                            }
                        } else {
                            // redefine property
                            prop.onRedefine(pdi.unwrap());
                        }
                    } catch (ValueFormatException vfe) {
                        // value conversion failed, remove it
                        removeChildProperty(propName);
                    } catch (ConstraintViolationException cve) {
                        // no suitable definition found for this property,
                        // remove it
                        removeChildProperty(propName);
                    }
                    affectedProps.put(propId, oldDef);
                 }
             }
            // use temp array to avoid ConcurrentModificationException
            ArrayList<ChildNodeEntry> list = new ArrayList<ChildNodeEntry>(thisState.getChildNodeEntries());
            // start from tail to avoid problems with same-name siblings
            for (int i = list.size() - 1; i >= 0; i--) {
                ChildNodeEntry entry = list.get(i);

            List<ChildNodeEntry> entries = thisState.getChildNodeEntries();
            for (ChildNodeEntry entry : entries) {
                 NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                NodeDefinition def = itemMgr.getDefinition(nodeState);
                NodeDefinition oldDef = itemMgr.getDefinition(nodeState);
                 // check if node has been defined by mixin type (or one of its supertypes)
                NodeTypeImpl declaringNT = (NodeTypeImpl) def.getDeclaringNodeType();
                NodeTypeImpl declaringNT = (NodeTypeImpl) oldDef.getDeclaringNodeType();
                 if (!entResulting.includesNodeType(declaringNT.getQName())) {
                     // the resulting effective node type doesn't include the
                     // node type that declared this child node
                    affectedNodes.put(entry, oldDef);
                }
            }
        } catch (ItemStateException e) {
            throw new RepositoryException("Internal Error: Failed to determine effect of removing mixin " + session.getJCRName(mixinName), e);
        }
 
                    try {
                        NodeImpl node = (NodeImpl) itemMgr.getItem(nodeState.getId());
                        if (node.getDefinition().isProtected()) {
                            // remove 'orphaned' protected child node immediately
                            removeChildNode(entry.getName(), entry.getIndex());
                            continue;
        // modify the state of this node
        thisState.setMixinTypeNames(remainingMixins);
        // set jcr:mixinTypes property
        setMixinTypesProperty(remainingMixins);

        // process affected nodes & properties:
        // 1. try to redefine item based on the resulting
        //    new effective node type (see JCR-2130)
        // 2. remove item if 1. fails
        boolean success = false;
        try {
            for (PropertyId id : affectedProps.keySet()) {
                PropertyImpl prop = (PropertyImpl) itemMgr.getItem(id);
                PropertyDefinition oldDef = affectedProps.get(id);

                if (oldDef.isProtected()) {
                    // remove 'orphaned' protected properties immediately
                    removeChildProperty(id.getName());
                    continue;
                }
                // try to find new applicable definition first and
                // redefine property if possible (JCR-2130)
                try {
                    PropertyDefinitionImpl newDef = getApplicablePropertyDefinition(
                            id.getName(), prop.getType(),
                            oldDef.isMultiple(), false);
                    if (newDef.getRequiredType() != PropertyType.UNDEFINED
                            && newDef.getRequiredType() != prop.getType()) {
                        // value conversion required
                        if (oldDef.isMultiple()) {
                            // convert value
                            Value[] values =
                                    ValueHelper.convert(
                                            prop.getValues(),
                                            newDef.getRequiredType(),
                                            session.getValueFactory());
                            // redefine property
                            prop.onRedefine(newDef.unwrap());
                            // set converted values
                            prop.setValue(values);
                        } else {
                            // convert value
                            Value value =
                                    ValueHelper.convert(
                                            prop.getValue(),
                                            newDef.getRequiredType(),
                                            session.getValueFactory());
                            // redefine property
                            prop.onRedefine(newDef.unwrap());
                            // set converted values
                            prop.setValue(value);
                         }
                        NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                                entry.getName(),
                                nodeState.getNodeTypeName());
                        // redefine node
                        node.onRedefine(ndi.unwrap());
                    } catch (ConstraintViolationException cve) {
                        // no suitable definition found for this child node,
                        // remove it
                        removeChildNode(entry.getName(), entry.getIndex());
                    } else {
                        // redefine property
                        prop.onRedefine(newDef.unwrap());
                     }
                } catch (ValueFormatException vfe) {
                    // value conversion failed, remove it
                    removeChildProperty(id.getName());
                } catch (ConstraintViolationException cve) {
                    // no suitable definition found for this property,
                    // remove it
                    removeChildProperty(id.getName());
                }
            }

            for (ChildNodeEntry entry : affectedNodes.keySet()) {
                NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                NodeImpl node = (NodeImpl) itemMgr.getItem(entry.getId());
                NodeDefinition oldDef = affectedNodes.get(entry);

                if (oldDef.isProtected()) {
                    // remove 'orphaned' protected child node immediately
                    removeChildNode(entry.getName(), entry.getIndex());
                    continue;
                }

                // try to find new applicable definition first and
                // redefine node if possible (JCR-2130)
                try {
                    NodeDefinitionImpl newDef = getApplicableChildNodeDefinition(
                            entry.getName(),
                            nodeState.getNodeTypeName());
                    // redefine node
                    node.onRedefine(newDef.unwrap());
                } catch (ConstraintViolationException cve) {
                    // no suitable definition found for this child node,
                    // remove it
                    removeChildNode(entry.getName(), entry.getIndex());
                 }
             }
             success = true;
         } catch (ItemStateException e) {
            throw new RepositoryException("Failed to clean up child items defined by removed mixin " + session.getJCRName(mixinName));
            throw new RepositoryException("Failed to clean up child items defined by removed mixin " + session.getJCRName(mixinName), e);
         } finally {
             if (!success) {
                // TODO JCR-1914: revert changes made to jcr:mixinTypes
                // TODO JCR-1914: revert any changes made so far
             }
         }
     }
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
index 4098dceed..fc766c9cc 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
@@ -16,26 +16,27 @@
  */
 package org.apache.jackrabbit.core;
 
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Calendar;
 
 import javax.jcr.ItemExistsException;
 import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
 import javax.jcr.Property;
 import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
 import javax.jcr.nodetype.NodeType;
 import javax.jcr.security.AccessControlManager;
 import javax.jcr.security.AccessControlPolicy;
 import javax.jcr.security.AccessControlPolicyIterator;
 import javax.jcr.security.Privilege;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Calendar;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 /** <code>NodeImplTest</code>... */
 public class NodeImplTest extends AbstractJCRTest {
@@ -200,4 +201,55 @@ public class NodeImplTest extends AbstractJCRTest {
         assertEquals(PropertyType.nameFromValue(PropertyType.LONG),
                 PropertyType.nameFromValue(p.getType()));
     }

    /**
     * Test case for JCR-2130 and JCR-2408.
     *
     * @throws RepositoryException
     */
    public void testAddRemoveMixin() throws RepositoryException {
        // add mix:title to a nt:folder node and set jcr:title property
        Node n = testRootNode.addNode(nodeName1, "nt:folder");
        n.addMixin("mix:title");
        n.setProperty("jcr:title", "blah blah");
        testRootNode.getSession().save();

        // remove mix:title, jcr:title should be gone as there's no matching
        // definition in nt:folder
        n.removeMixin("mix:title");
        testRootNode.getSession().save();
        assertFalse(n.hasProperty("jcr:title"));

        // add mix:title to a nt:unstructured node and set jcr:title property
        Node n1 = testRootNode.addNode(nodeName2, ntUnstructured);
        n1.addMixin("mix:title");
        n1.setProperty("jcr:title", "blah blah");
        assertEquals(
                n1.getProperty("jcr:title").getDefinition().getDeclaringNodeType().getName(),
                "mix:title");

        // remove mix:title, jcr:title should stay since it adopts the residual
        // property definition declared in nt:unstructured
        testRootNode.getSession().save();

        n1.removeMixin("mix:title");
        testRootNode.getSession().save();
        assertTrue(n1.hasProperty("jcr:title"));

        assertEquals(
                n1.getProperty("jcr:title").getDefinition().getDeclaringNodeType().getName(),
                ntUnstructured);

        // add mix:referenceable to a nt:unstructured node, jcr:uuid is
        // automatically added
        Node n2 = testRootNode.addNode(nodeName3, ntUnstructured);
        n2.addMixin(mixReferenceable);
        testRootNode.getSession().save();

        // remove mix:referenceable, jcr:uuid should always get removed
        // since it is a protcted property
        n2.removeMixin(mixReferenceable);
        testRootNode.getSession().save();
        assertFalse(n2.hasProperty("jcr:uuid"));
    }
 }
- 
2.19.1.windows.1

