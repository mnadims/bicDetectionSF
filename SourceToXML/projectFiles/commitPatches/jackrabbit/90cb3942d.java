From 90cb3942db8416fa3c3ebf13b576cc7a6ed44af6 Mon Sep 17 00:00:00 2001
From: Stefan Guggisberg <stefan@apache.org>
Date: Thu, 26 Nov 2009 11:42:18 +0000
Subject: [PATCH] JCR-2408: Mixin removal exception

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@884535 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/ItemImpl.java  |  16 +-
 .../org/apache/jackrabbit/core/NodeImpl.java  | 215 ++++++++++--------
 2 files changed, 129 insertions(+), 102 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
index 266106c28..a2e6fb26d 100644
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
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
index dd5beea73..3714fdc53 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
@@ -28,6 +28,8 @@ import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
import java.util.Map;
import java.util.HashMap;
 
 import javax.jcr.AccessDeniedException;
 import javax.jcr.Binary;
@@ -84,6 +86,7 @@ import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.NodeReferences;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
@@ -1016,127 +1019,143 @@ public class NodeImpl extends ItemImpl implements Node {
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

                if (prop.getDefinition().isProtected()) {
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
- 
2.19.1.windows.1

